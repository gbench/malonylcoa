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
sqlexecute.h2 <- function(sql, simplify = TRUE, get_last_id = TRUE, ...) {
  require(DBI); require(rJava)
  
  dbfun(\(con) { # 数据库练级使用函数
    tryCatch({
      dbBegin(con)
      jc <- con@jc # 提取连接指针以便.jcall使用
      res <- lapply(c(sql), \(s) { # 执行SQL语句
        S <- toupper(trimws(s)) # 转换成大写形式
        if (grepl("^(SELECT|SHOW)", S)) { # 数据查询
          list(type = "SELECT", rows = nrow(dbGetQuery(con, s)), id = NA)
        } else if (get_last_id && grepl("^INSERT", S)) { # INSERT 获取生成键
          stmt <- .jcall(jc, "Ljava/sql/PreparedStatement;", "prepareStatement", s, 
            .jfield("java/sql/Statement", "I", "RETURN_GENERATED_KEYS", TRUE))
          .jcall(stmt, "Z", "execute") # 语句执行
          rows <- .jcall(stmt, "I", "getUpdateCount") # 更新行数
          rs <- .jcall(stmt, "Ljava/sql/ResultSet;", "getGeneratedKeys")  # 获取生成键
          id <- if (!is.jnull(rs) && .jcall(rs, "Z", "next")) as.numeric(.jcall(rs, "J", "getLong", 1L)) else NA
          .jcall(rs, "V", "close"); .jcall(stmt, "V", "close") # 关闭对象
          list(type = "DML", rows = rows, id = id)
        } else { # 普通执行
          stmt <- .jcall(jc, "Ljava/sql/Statement;", "createStatement")
          has_rs <- .jcall(stmt, "Z", "execute", s)
          rows <- if (!has_rs) .jcall(stmt, "I", "getUpdateCount") else 0L
          .jcall(stmt, "V", "close")
          list(type = if (grepl("^(CREATE|DROP|ALTER|TRUNCATE)", S)) "DDL" else "DML", rows = rows, id = NA)
        } # if
      }) # res返回SQL语句的执行结果
      dbCommit(con) # 成功执行提交事务
      purrr::map_dfr(res, `[`, c("type", "rows", "id")) |> (\(df) if (simplify && nrow(df) == 1) df[1, ] else df) ()
    }, error = \(e) { dbRollback(con); stop(e) })
  }, ...)(sql) # dbfun 
}

xxxconfig <- "h2config" # 为环境命名

# 环境初始化
app_init <- \(port, dbname="mem:mybank", sqlquery.host=gettextf("jdbc:h2:tcp://localhost:%s/%s;mode=mysql;db_close_delay=-1;database_to_upper=false", port, dbname)) {
    app_uninit() # 清理环境
    
    # Underlay 图层：配置驱动和连接参数
    attach(list2env(list(sqlquery.host=sqlquery.host)), name=paste0(xxxconfig, ".underlay"), pos=match(".SqlQueryEnv", search()) + 1) |> with({
        h2.jar <- "D:/sliced/mvn_repos/com/h2database/h2/2.2.224/h2-2.2.224.jar"
        drv <- RJDBC::JDBC(driverClass="org.h2.Driver", classPath=h2.jar, identifier.quote="\"")
        localcfg <- list(sqlquery.drv=drv, sqlquery.user="root", sqlquery.password="123456", sqlquery.host=sqlquery.host)
        getOption <- \ (x, default = NULL) localcfg[[x]] %||% base::getOption(x, default)
    })

    # Overlay 图层：拦截 dbConnect 注入 URL 和认证信息
    attach(new.env(), name=paste0(xxxconfig, ".overlay")) |> with({
        shared_conn <- NULL #  共享连接
        dbConnect <- \(...) if(!is.null(shared_conn) && DBI::dbIsValid(shared_conn)) shared_conn else shared_conn <<- do.call(DBI::dbConnect, args=c(list(...), url=localcfg[["sqlquery.host"]]))
        dbDisconnect <- \(conn) if(!is.null(shared_conn)) 1 |> invisible() else DBI::dbDisconnect (conn) # 拦截状态什么都不做！（返回1）表示拒绝关闭！
    })

  message(gettextf("switch to dbname:%s, port:%s", dbname, port))
}

# qpp 取消初始化
app_uninit <- \() {
    es <- search() |> grep(pattern=xxxconfig, value=T)
    if(length(es)>0) {
      shared_conn <- get("shared_conn", envir=as.environment(paste0(xxxconfig, ".overlay")))
      if(!is.null(shared_conn) && DBI::dbIsValid(shared_conn)) DBI::dbDisconnect(shared_conn)  # 真正关闭连接！
      es |> lapply(\(e) do.call(detach, args=list(e)))
    }
}

