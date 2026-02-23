#' 执行SQL语句（dbExecute模式)
#' @param sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
#' @param simplify 是否对查询结果做简化处理后再返回，
#'   T:  简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#'   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
#' @return 返回结果是 affected_rows, last_insert_id 两列的数据框，对于
#'   insert into t_user(name) values('name_1'),('name_2'),...,('name_n') 一条语句插入多条数据
#'   的情况affected_rows返回实际插入的数量，last_insert_id返回插入的第一条数据的id
#'   其余id请根据last_insert_id,affected_rows依次计算，比如name_1的id为x，那么name_2就是x+1,...，name_n为x+n-1
#'   返回实际插入数据的id为: seq(from=last_insert_id,lengout.out=affected_rows)
sqlexecute.h2 <- function(sql, simplify = T, get_last_id = FALSE, ...) {
  require(DBI)
  require(rJava)
  
  dbfun(\ (con, ...) {
    tryCatch({
      dbBegin(con)
      
      dataset <- lapply(c(sql), function(.sql) {
        sql_upper <- trimws(toupper(.sql))
        is_ddl <- grepl("^(CREATE|DROP|ALTER|TRUNCATE)", sql_upper)
        is_insert <- grepl("^INSERT", sql_upper)
        is_select <- grepl("^SELECT", sql_upper)
        
        # 获取底层 JDBC 连接
        jc <- con@jc
        
        if (is_select) {
          result <- dbGetQuery(con, .sql)
          return(list(
            type = "SELECT",
            affected_rows = nrow(result),
            last_insert_id = NA
          ))
        }
        
        # 创建 Statement，指定返回生成键
        if (get_last_id && is_insert) {
          # 使用 prepareStatement 获取生成键能力
          stmt <- rJava::.jcall(jc, "Ljava/sql/PreparedStatement;", "prepareStatement", .sql, 
            rJava::.jfield("java/sql/Statement", "RETURN_GENERATED_KEYS"))
          rJava::.jcall(stmt, "Z", "execute")
          affected_rows <- rJava::.jcall(stmt, "I", "getUpdateCount")
          
          # 获取生成键
          last_id <- NA
          gen_keys <- rJava::.jcall(stmt, "Ljava/sql/ResultSet;", "getGeneratedKeys")
          if (!rJava::is.jnull(gen_keys) && rJava::.jcall(gen_keys, "Z", "next")) {
            last_id <- rJava::.jcall(gen_keys, "J", "getLong", 1L)
            rJava::.jcall(gen_keys, "V", "close")
          }
          rJava::.jcall(stmt, "V", "close")
          
        } else {
          # 普通执行（DDL 或其他 DML）
          stmt <- rJava::.jcall(jc, "Ljava/sql/Statement;", "createStatement")
          is_rs <- rJava::.jcall(stmt, "Z", "execute", .sql)
          affected_rows <- if (!is_rs) rJava::.jcall(stmt, "I", "getUpdateCount") else 0L
          rJava::.jcall(stmt, "V", "close")
          last_id <- NA
        }
        
        list(
          type = ifelse(is_ddl, "DDL", "DML"),
          affected_rows = affected_rows,
          last_insert_id = if (!is.na(last_id)) as.numeric(last_id) else NA
        )
      })
      
      result_df <- do.call(rbind, lapply(dataset, function(x) {
        data.frame(
          type = x$type,
          affected_rows = x$affected_rows,
          last_insert_id = x$last_insert_id,
          stringsAsFactors = FALSE
        )
      })) |> tibble::as_tibble()
      
      dbCommit(con)
      
      if (simplify && nrow(result_df) == 1) result_df[1, ] else result_df
      
    }, error = function(err) {
      dbRollback(con)
      stop(err)
    })
  }, ...)(sql)
}

xxxconfig <- "h2config" # 为环境命名

# 环境初始化
app_init <- \(port) {
    # Underlay 图层：配置驱动和连接参数
    attach(list2env(list(port=port)), name=paste0(xxxconfig, ".underlay"), pos=match(".SqlQueryEnv", search()) + 1) |> with({
        h2.jar <- "D:/sliced/mvn_repos/com/h2database/h2/2.2.224/h2-2.2.224.jar"
        drv <- RJDBC::JDBC(driverClass="org.h2.Driver", classPath=h2.jar, identifier.quote="\"")
        localcfg <- list(sqlquery.drv=drv, sqlquery.user="root", sqlquery.password="123456", sqlquery.host=gettextf("jdbc:h2:tcp://localhost:%s/mem:mybank;mode=mysql;db_close_delay=-1;database_to_upper=false", port))
        getOption <- \ (x, default = NULL) localcfg[[x]] %||% base::getOption(x, default)
    })

    # Overlay 图层：拦截 dbConnect 注入 URL 和认证信息
    attach(new.env(), name=paste0(xxxconfig, ".overlay")) |> with({
        shared_conn <- NULL #  共享连接
        dbConnect <- \(...) if(!is.null(shared_conn) && DBI::dbIsValid(shared_conn)) shared_conn else shared_conn <<- do.call(DBI::dbConnect, args=c(list(...), url=localcfg[["sqlquery.host"]]))
        dbDisconnect <- \(conn) if(!is.null(shared_conn)) 1 |> invisible() else DBI::dbDisconnect (conn) # 拦截状态什么都不做！（返回1）表示拒绝关闭！
        })
}

# qpp 取消初始化
app_uninit <- \() {
    search() |> grep(pattern=xxxconfig, value=T) |> lapply(\(e) do.call(detach, args=list(e)))
}

