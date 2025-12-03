# --------------------------------------------------------------------------------------------------
# @REM 设置环境变量R_HOME 
# set R_HOME=D:\sliced\develop\r\R-4.4.2 
# set RSCRIPT_HOME=D:\sliced\develop\r\scripts\db
#
# # Rprofile.site R环境初始设置
# # 文件参考:https://gitee.com/gbench/malonylcoa/tree/master/src/main/r/rws/etc/Rprofile.site
# # 编辑 %R_HOME%/etc/Rprofile.site 在文件末尾加入以下代码
#
# # 设置R的工作空间
# rws_home <- "F:/slicef/ws/rws"
# dailywork_home <- sprintf("%s/%s", rws_home, Sys.Date()) 
# if ( !dir.exists(dailywork_home) )  dir.create(dailywork_home)
# setwd(dailywork_home)
#
# # 自定义的预先加载的文件列表
# # 文件参考:https://gitee.com/gbench/malonylcoa/tree/master/src/main/r/rws
# myfiles <- "db/sqlquery.R;utils/envs.R" |> strsplit("[,;]+") |> unlist()
# scriptlog <- sprintf(fmt="%s/%s", Sys.getenv("RSCRIPT_HOME"), myfiles) |> lapply(source) 
# --------------------------------------------------------------------------------------------------

pkgs <- "RMySQL,RPostgres,tidyverse,janitor" |> # 程序包名称列表
  strsplit(",") |> unlist() # 程序包列表的拆解
flags <- sapply(pkgs, \(p) substitute(require(p), list(p=p)) |> eval()) # 生成程序包是否业已加载标志
  
if(!all(flags)) { # 存在没有安装的程序包
  pkgs[!flags] |> install.packages() # 安装没有安装的程序包
  sapply(pkgs, \(p) substitute(require(p), list(p=p)) |> eval()) # 重新加载
} # if

#' 数据库函数
#' @param f 连接执行函数
#' @param 执行SQL语句 的函数
dbfun <- function(f, ...) {
  dbcfg <- match.call(expand.dots=F)$... |> lapply(\(x) tryCatch(eval(x), error=\(e) deparse(x))) # 数据库连接参数配置(尝试求值，失败则解释符号名为字符串)
  defaultcfg <- list( # 从 global options 中提取默认连接参数
    drv = getOption("sqlquery.drv", MySQL()), 
    host = getOption("sqlquery.host", "localhost"), 
    user = getOption("sqlquery.user", "root"), 
    password = getOption("sqlquery.password", "123456"), 
    port = getOption("sqlquery.port", 3371), 
    dbname = getOption("sqlquery.dbname", "ctp2")) # 默认连接参数
  readcfg <- \(key) dbcfg[[key]] %||% defaultcfg[[key]] #  带有默认值的配置参数key的值读取 

  #' 执行SQL语句（生成数据库的执行函数）
  #' @param sql  SQL语句
  function (sql, ...) {
    # 数据库连接
    tryCatch({
      keys <- "drv,host,user,password,port,dbname" |> strsplit(",") |> unlist() # 配置参数keys向量
      con <- structure(keys, names=keys) |> lapply(readcfg)  |> do.call(dbConnect, args=_ ) # 读取配置并获得数据库连接
      on.exit(dbDisconnect(con), add=TRUE) # 注册函数运行结束时关闭调该数据库连接con
      f(con, ...) # 执行sql
    }, error = \ (err) {
      print(sprintf("sql:%s", sql)) # 打印错误sql
      stop(err) # 重新抛出错误
    }) # 数据库连接
  } # function (sql)

} # dbfun

#' 执行SQL语句查询数据结果（dbGetQuery模式)
#' @param sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
#' @param simplify 是否对查询结果做简化处理后再返回，
#'   T:  简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#'   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
#' @param n 查询结果的返回最大数量
#' @param pretty 结果美化函数，可以做一些数据R层面的后处理, 类型转换或是结果与当前执行环境的融合合并,do.call(x)等:
#'   sqlquery("show tables") |> head() %>% sprintf(fmt="select '%s' tbl, count(*) n from %s t limit 2", ., .) |> sqlquery(prettify=bind_rows)
#' @return 返回结果数据集
sqlquery <- function(sql, simplify = T, n = -1, prettify=\(x) if(1 == length(x)) x[[1]] else x, ...) {
  # 连接使用函数
  dbfun(\ (con, ...) { # 使用数据库连接进行查询结果数据集
    dataset <- c(list(), sql) |> lapply(compose(tibble, partial(dbGetQuery, con=con, n=n))) |> structure(names=sql) # 执行数据查询
    prettify(if(simplify & length(dataset) == 1) dataset[[1]] else dataset)  # 返回结果数据集
  }, ...)(sql) # 连接使用函数
}

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
sqlexecute <- function(sql, simplify = T, ...) {
    # 连接使用函数
    dbfun(\ (con, ...) { # 使用数据库连接进行查询结果数据集
      tryCatch({ # try 运行结果
        dbBegin(con) # 开启事务
        dataset <- c(sql) |> lapply(\(.sql){
          affected_rows <- dbExecute(con, .sql); # 影响数据行数
          last_insert_id <-  dbGetQuery(con, "SELECT LAST_INSERT_ID()") |> unlist() # 获取插入的Id
          list(affected_rows=affected_rows, last_insert_id=last_insert_id) # 返回结果
        }) |> do.call(rbind, args=_) |> tibble() # 执行数据查询
        dbCommit(con) # 提交事务
        if(simplify & length(dataset) == 1)  dataset[[1]]  else  dataset  # 返回结果数据集
      }, error=\(err) { # 错误处理
        dbRollback(con) # 回滚错误
        stop(err) # 重新抛出错误
      }) # try 运行结果
    }, ...)(sql) # 连接使用函数
}

#' 创建数据表SQL
#' @param dfm 数据框数据
#' @param tbl  数据表名
#' @return 创建数据表SQL
ctsql <- function(dfm, tbl) {
  # 获取原始参数表达式
  .dfm <- substitute(dfm)
  .tbl <- if(missing(tbl)) deparse(substitute(dfm)) else substitute(tbl)
  # 根据驱动类型选择合适的SQL建表函数，并计算
  getOption("sqlquery.drv") |> (\ (x) # 根据驱动类型选择合适的SQL建表函数，并创建表格
    (if(is.null(x) || (is.atomic(x) && is.na(x))) ctsql.mysql # 非法统一指向，默认是 MySQL
      else switch(x |> class() |> attr("package"), # 根据驱动的爆属性决定当前连接的是什么数据库
      "RPostgres" = ctsql.pg, # PostgreSQL
      "RMySQL" = ctsql.mysql, # MySQL
      ctsql.mysql # 默认
    )) |> do.call(args=list(dfm=.dfm, tbl=.tbl))) () # x 根据驱动类型选择合适的SQL建表函数，并创建表格
}

#' 创建数据表SQL
#' @param dfm 数据框数据
#' @param tbl  数据表名
#' @return 创建数据表SQL
ctsql.mysql <- function(dfm, tbl) {
  .tbl <- if(missing(tbl)) deparse(substitute(dfm)) else deparse(substitute(tbl)) |> gsub(pattern = "^['\"]|['\"]$", replacement = "")  #  提取数据表名
  dfm |> lapply(\(e, t=typeof(e), cls=class(e), # 基础类型与class包含高级类型list
      n=as.integer(Reduce(\(acc, a) max(acc, max(acc, nchar(a))), x=as.character(e), init=0) * 1.5), # 列数据宽度
      default_type=sprintf('varchar(%s)', n) # 默认类型
    ) switch(t, # 类型判断
      `logical`='bool', # 布尔类型
      `integer`=if(cls=='factor') default_type else 'integer', # 列表类型
      `double`=if(any(grepl(pattern="Date|POSIXct|POSIXt", x=cls))) "datetime" else 'double', # 列表类型
      `list`='json', # 列表类型
      default_type # 默认类型
    )) |> (\ (x) # 获取字段定义
      sprintf("create table %s (\n  %s \n)\n", .tbl, paste(names(x), x, collapse=",\n  ")) # 数据表创建语句 
    ) () # SQL 创建表语句
}

#' 表数据插入SQL
#' @param dfm 数据框数据
#' @param tbl  数据表名
#' @return 表数据插入SQL
insql <- function(dfm, tbl) {
  .tbl <- if(missing(tbl)) deparse(substitute(dfm)) else deparse(substitute(tbl)) |> gsub(pattern = "^['\"]|['\"]$", replacement = "") #  提取数据表名
  keys <- names(dfm) |> paste(collapse=", ") # 列名列表
  values <- dfm |> lapply(\(e, t=typeof(e), cls=class(e)) # 记录值列表的各个字段值处理：
      switch(t, # 元素类型判断，决定是否用单引号把数值括起来，数值与逻辑值不用，list 转换成列表
        `logical`=e, # 逻辑类型，保持原值不变
        `integer`=if(cls=='factor') sprintf("'%s'", e) else e, # 整数类型，保持原值不变
        `double`=if(any(grepl(pattern="Date|POSIXct|POSIXt", x=cls))) sprintf("'%s'", e) else e, # 双精度，保持原值不变
        `list`=sprintf("'%s'", gsub("'", "''", toJSON(e))), # list类型，转换成JSON, 并对单引号进行转义
        sprintf("'%s'", gsub("'", "''", e)) # 默认类型，使用单引号'给括起来, 并对单引号进行转义
      ) |> (\(x) ifelse(is.na(x), "NULL", x))() # NA值转NULL值
    ) |> do.call(\(...) mapply(\(...) paste(..., sep=', ', collapse=','), ...), args=_) |> # 行映射，此处\(...)有层级差异,内为字段外为数据行是两个不同变量
    sprintf(fmt='( %s )') |> paste(collapse=',\n  ') # 值列表
  sprintf( "insert into %s (%s) values \n  %s\n", .tbl, keys, values ) # SQL 插入记录行数据（多行）语句
}

#' 表数据更新SQL
#' @param dfm 数据框数据
#' @param tbl 数据表名
#' @param pk 数据主键
upsql <- \(dfm, tbl, pk = "id") { # 数据更新
  .tbl <- if(missing(tbl)) deparse(substitute(dfm)) else tbl #  提取数据表名
  nms <- names(dfm) # 提取各个数据列名
  .pk <- deparse(substitute(pk)) |> gsub(pattern = "^['\"]|['\"]$", replacement = "")
  idx <- match(.pk, nms) #  主键pk在名称nms中的索引位置
  stopifnot("dfm的名称nms中必须包含主键名pk" = !is.na(idx)) # pk名字的有效性检测
  flds <- sapply(nms, \(i) sprintf("%s='%s'", i, dfm[, i, drop=T] |> gsub("'", "''", x=_))) |> 
    apply(1, \(line) paste(line[-idx], collapse=",\n  ")) # 字段拼接
  sprintf("update %s set\n  %s \nwhere %s='%s'\n", .tbl, flds, .pk, dfm[, .pk]) # 数据更新的SQL语句
}

#' 带有动态参数计算能力：sqlexecute2(sql, dbname=test), 即dbname=test相当于dbname="test"，合法标识符名可以不打引号而直接获得符号名!
#' 但是"eval.parent(substitute(... ...))"与partial的固定参数机制相冲突：
#' 这种自定义数据库的定义SQL查询方式是不可以的：sqlquery.test <- partial(sqlquery2, dbname="test") 
#' 需要写成确定参数形式：sqlquery.test <- \(sql) sqlquery2(sql, dbname="test")
#' 对于静态SQL这两种范式没有区别，但是对于动态生成的sql语句的dynamic_sql() 就差别很大了！
#' 即partial方式定义的sqlquery.test不能用于这种形式：dynamic_sql() |> sqlquery.test()， dynamic_sql() 会生成一场！
#' 
#' 执行SQL语句查询数据结果（dbGetQuery模式)
#' @param sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
#' @param simplify 是否对查询结果做简化处理后再返回，
#'   T: 简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#'   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
#' @param n 查询结果的返回最大数量
#' @param pretty 结果美化函数，可以做一些数据R层面的后处理, 类型转换或是结果与当前执行环境的融合合并,do.call(x)等:
#'   sqlquery("show tables") |> head() %>% sprintf(fmt="select '%s' tbl, count(*) n from %s t limit 2", ., .) |> sqlquery(prettify=bind_rows)
#' @return 返回结果数据集
sqlquery2 <- function(sql, simplify = T, n = -1, prettify=\(x) if(1 == length(x)) x[[1]] else x, ...) {
  # 连接使用函数:使用"eval.parent(substitute(... ...)) 去封装dbfun调用，可以避免R把实际"..."给改写"..1, ..2"的形式：
  # 否则，sqlquery(sql, dbname=ctp) 会被翻译成 sqlexecute(sql, ..1=ctp) 而改掉了参数名
  eval.parent(substitute(dbfun(\ (con, ...) { # 使用数据库连接进行查询结果数据集
    dataset <- c(sql) |> lapply(compose(tibble, partial(dbGetQuery, con=con, n=n))) |> structure(names=sql) # 执行数据查询
    prettify(if(simplify & length(dataset) == 1) dataset[[1]] else dataset) # 返回结果数据集
  }, ...)(sql))) # 连接使用函数
}

#' 带有动态参数计算能力：sqlexecute2(sql, dbname=test), 即dbname=test相当于dbname="test"，合法标识符名可以不打引号而直接获得符号名!
#' 但是"eval.parent(substitute(... ...))"与partial的固定参数机制相冲突：
#' 这这种自定义数据库的定义SQL执行方式是不可以的：sqlexecute.test <- partial(sqlexecute2, dbname=test) # 
#' 需要写成确定参数形式：sqlexecute.test <- \(sql) sqlexecute2(sql, dbname=test)
#' 对于静态SQL这两种范式没有区别，但是对于动态生成的sql语句的dynamic_sql() 就差别很大了！
#' 即partial方式定义的sqlquery.test不能用于这种形式：dynamic_sql() |> sqlexecute.test()， dynamic_sql() 会生成一场！
#' 
#' 执行SQL语句（dbExecute模式)
#' @param sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
#' @param simplify 是否对查询结果做简化处理后再返回，
#'   T: 简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#'   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
#' @return 返回结果是 affected_rows, last_insert_id 两列的数据框，对于
#'   insert into t_user(name) values('name_1'),('name_2'),...,('name_n') 一条语句插入多条数据
#'   的情况affected_rows返回实际插入的数量，last_insert_id返回插入的第一条数据的id
#'   其余id请根据last_insert_id,affected_rows依次计算，比如name_1的id为x，那么name_2就是x+1,...，name_n为x+n-1
#'   返回实际插入数据的id为: seq(from=last_insert_id,lengout.out=affected_rows)
sqlexecute2 <- function(sql, simplify = T, ...) {
    # 连接使用函数:使用"eval.parent(substitute(... ...)) 去封装dbfun调用，可以避免R把实际"..."给改写"..1, ..2"的形式：
    # 否则，sqlexecute(sql, dbname=ctp) 会被翻译成 sqlexecute(sql, ..1=ctp) 而改掉了参数名
    eval.parent(substitute(dbfun(\ (con, ...) { # 使用数据库连接进行查询结果数据集
      tryCatch({ # try 运行结果
        dbBegin(con) # 开启事务
        dataset <- c(sql) |> lapply(\(.sql){
          affected_rows <- dbExecute(con, .sql); # 影响数据行数
          last_insert_id <-  dbGetQuery(con, "SELECT LAST_INSERT_ID()") |> unlist() # 获取插入的Id
          list(affected_rows=affected_rows, last_insert_id=last_insert_id) # 返回结果
        }) |> do.call(rbind, args=_) |> tibble() # 执行数据查询
        dbCommit(con) # 提交事务
        if(simplify & length(dataset) == 1)  dataset[[1]] else dataset  # 返回结果数据集
      }, error = \ (err) { # 错误处理
        dbRollback(con) # 回滚错误
        stop(err) # 重新抛出错误
      }) # try 运行结果
    }, ...)(sql))) # 连接使用函数
}

#' PostgreSQL API 
#' 数据库函数（对PostgreSQL的dbfun封装）
#' @param f 连接执行函数
#' @param 执行SQL语句 的函数
dbfun.pg <- \(f, ...) {
  with(list(...), dbfun(\ (con, ...) { # 连接配置, 通过with(list(...), xxx xxx)从dbfun.pg参数中提取search_path
    .log <- \ (x) { if(verbose) cat(" -- ", x, "\n"); x } # 日志输出
    if(!is.na(search_path)) search_path else getOption("sqlquery.schema", "public,economics") |> 
      strsplit("[,[:blank:]]+") |> unlist() |> sprintf(fmt="'%s'") |> paste0(collapse=", ") |> 
      sprintf(fmt="SET search_path to %s") |> .log() |> dbExecute(con, statement=_) # search_path 的设置
    f(con, .log=.log) # 把.log注入到f函数
  }))
}

#' PostgreSQL API 
#' 自定义查询函数 （手动设定shema）
#' @param sql 查询语句
#' @param search_path 检索路径，默认值NA，表示尝试读取getOption("sqlquery.schema")
#' @param simplify 是否简化模式，，默认T
#' @param pretty 结果美化函数，可以做一些数据R层面的后处理, 类型转换或是结果与当前执行环境的融合合并,do.call(x)等:
#'   sqlquery.pg("select table_name from information_schema.tables where table_schema='economics'") |> head() %>% sprintf(fmt="select '%s' tbl, count(*) n from %s t limit 2", ., .) |> sqlquery.pg(prettify=bind_rows)
#' @param verbose 息详情模式，， 默认F
sqlquery.pg <- \(sql, search_path = NA, simplify = T, prettify=\(x) if(1 == length(x)) x[[1]] else x, verbose = F, ...) {
  dbfun.pg(\ (con, ...) { # 连接配置（dbfun.pg 的参数f)
    with(list(...), ( # 执行查询结果, 通过with(list(...), xxx xxx)从dbfun.pg的f函数的参数中提取.log函数
      \ (.sqls = c(sql)) .sqls |> .log() |> lapply(compose(tibble, dbGetQuery), conn = con) |> (\ (res) # res 结果集简化
      prettify(if(simplify && 1 == length(res)) res[[1]] else structure(res, names=.sqls))) ()
    ) ()) # with 从dbfun.pg中提取参数.log并执行
  }, search_path = search_path, simplify = simplify, verbose = verbose, ...) (sql) 
}

#' PostgreSQL API  
#' 自定义执行函数 （手动设定shema）
#' @param sql 查询语句向量
#' @param search_path 检索路径，默认值NA，表示尝试读取getOption("sqlquery.schema")
#' @param simplify 是否简化模式， 默认T
#' @param verbose 信息详情模式， 默认F
#' @param flag 事务开启标记, 需要注意PostgreSQL对于DML语句会自动进行提交，如果sql向量中同时包含有create,insert时
#'   后面的语句就会跳出事务，语句执行了但是没有事务内结果且并不会报错。即，你不能在一个事务中先创建后插入，必须在非事务
#'   环境中执行创建表与插入表的混合操作！
sqlexecute.pg <- \ (sql, search_path = NA, simplify = TRUE, verbose = F, flag = F, ...) {
  dbfun.pg(\ (con, ...) {
    with(list(...), tryCatch({ # 执行SQL执行过程（dbfun.pg 的参数f), 通过with(list(...), xxx xxx)从dbfun.pg的f函数的参数中提取.log函数
      if(flag) dbBegin(con) # 开启事务
      rs <- lapply(c(sql), function(s) {
        .sql <- .log(s) |> gsub(pattern="^\\s*|\\s*$", replacement="", x=_)
        is.ddl <- grepl("^CREATE\\s+TABLE|^ALTER|^DROP", .sql, ignore.case=T)
        is.insert <- grepl("^INSERT", .sql, ignore.case=T)
        affected_rows <- ifelse(is.ddl, # 
          dbSendStatement(con, .sql) |> (\(res) {n=dbGetRowsAffected(res); dbClearResult(res); n}) (),  # ddl 
          dbExecute(con, .sql)) # not ddl(dml)
        last_insert_id <- if(is.insert) tryCatch({dbGetQuery(con, "SELECT LASTVAL()") |> unlist()}, error=\(e) NA) else NA
        list(affected_rows=affected_rows, last_insert_id=last_insert_id)
      }) |> do.call(rbind, args=_) 
      if(flag) dbCommit(con)  # 开启事务
      if(simplify && nrow(rs)==1) as.list(rs[1, ]) else rs
    }, error=\(err) {if(flag) dbRollback(con); stop(err)})) # with 从dbfun.pg中提取参数.log并执行
  }, search_path = search_path, simplify = simplify, verbose = verbose, ...) (sql)
}

#' PostgreSQL API  
#' PostgreSQL 自增长主键（serial）并不会你手动设置主键之后进行同步，需要你手动给与同步设定，这与MySQL有很大不同，提请注意一下！
#' 数据表创造语句: postgresql 版本
#' @param dfm 数据框数据
#' @param tbl  数据表名
#' @return 创建数据表SQL
ctsql.pg <- function(dfm, tbl) {
  .tbl <- if(missing(tbl)) deparse(substitute(dfm)) else deparse(substitute(tbl)) |> gsub(pattern = "^['\"]|['\"]$", replacement = "")  #  提取数据表名
  dfm |> lapply(\(e, t=typeof(e), cls=class(e), # 基础类型与class包含高级类型list
    n=as.integer(Reduce(\(acc, a) max(acc, max(acc, nchar(a))), x=as.character(e), init=0) * 1.5), # 列数据宽度
    default_type=sprintf('varchar(%s)', n) # 默认类型
    ) switch(t, # 类型判断
      `logical`='boolean', # PostgreSQL布尔类型
      `integer`=if(cls=='factor') default_type else 'integer', # 整数类型
      `double`=if(any(grepl(pattern="Date", x=cls))) "date" else if(any(grepl(pattern="POSIXct|POSIXt", x=cls))) "timestamp with time zone" else 'double precision', # PostgreSQL浮点类型
      `character`=default_type, # 字符类型
      `list`='jsonb', # PostgreSQL JSONB类型
      `raw`='bytea', # 二进制类型
      default_type # 默认类型 
    )) |> (\ (x) { # 获取字段定义
      if('id' %in% names(x)) x['id'] <- if(grepl('integer', x['id'])) 'serial primary key' else x['id']; # 主键处理
      sprintf("create table %s (\n  %s \n);\n", .tbl, paste(names(x), x, collapse=",\n  ")) # 数据表创建语句 
    }) () # SQL 创建表语句
}

# 自定义主机与数据库
sqlquery.h10ctp2 <- partial(sqlquery, host="192.168.1.10", dbname="ctp2")
sqlquery.ctp <- partial(sqlquery, host="192.168.1.10", dbname="ctp")
