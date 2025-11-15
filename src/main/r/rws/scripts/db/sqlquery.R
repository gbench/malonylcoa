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

pkgs <- "RMySQL,tidyverse" |> # 程序包名称列表
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
  dbcfg <- list(...) # 数据库连接参数配置
  defaultcfg <- list(drv=MySQL(), host="localhost", user="root", password="123456", port=3371, dbname="ctp2") # 默认连接参数
  readcfg <- \(key) dbcfg[[key]] %||% defaultcfg[[key]] #  带有默认值的配置参数key的值读取 

  #' 执行SQL语句
  #' @param sql  SQL语句
  function (sql) {
    # 数据库连接
    tryCatch({
      keys <- "drv,host,user,password,port,dbname" |> strsplit(",") |> unlist() # 配置参数keys向量
      con <- structure(keys, names=keys) |> lapply(readcfg)  |> do.call(dbConnect, args=_ ) # 读取配置并获得数据库连接
      on.exit(dbDisconnect(con), add=TRUE) # 注册函数运行结束时关闭调该数据库连接con
      f(con) # 执行sql
    }, error=\(err) {
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
#' @return 返回结果数据集
sqlquery <- function(sql, simplify=T, n=-1, ...) {
  # 连接使用函数
  dbfun(\ (con) { # 使用数据库连接进行查询结果数据集
    dataset <- c(list(), sql) |> lapply(compose(tibble, partial(dbGetQuery, con=con, n=n))) |> structure(names=sql)# 执行数据查询
    if( simplify & length(dataset) == 1 )  dataset[[1]]  else  dataset  # 返回结果数据集
  }, ...)(sql) # 连接使用函数
}

#' 执行SQL语句（dbExecute模式)
#' @param sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
#' @param simplify 是否对查询结果做简化处理后再返回，
#'   T:  简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#'   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
#' @return 返回结果是 affected_rows, last_insert_id 两列的数据框，对于
#'    insert into t_user(name) values('name_1'),('name_2'),...,('name_n') 一条语句插入多条数据
#'    的情况affected_rows返回实际插入的数量，last_insert_id返回插入的第一条数据的id
#'    其余id请根据last_insert_id,affected_rows依次计算，比如name_1的id为x，那么name_2就是x+1,...，name_n为x+n-1
#'    返回实际插入数据的id为: seq(from=last_insert_id,lengout.out=affected_rows)
sqlexecute <- function(sql, simplify=T, ...) {
    # 连接使用函数
    dbfun(\ (con) { # 使用数据库连接进行查询结果数据集
      tryCatch({ # try 运行结果
        dbBegin(con) # 开启事务
        dataset <- c(list(), sql) |> lapply(\(.sql){
          affected_rows <- dbExecute(con, .sql); # 影响数据行数
          last_insert_id <-  dbGetQuery(con, "SELECT LAST_INSERT_ID()") |> unlist() # 获取插入的Id
          list(affected_rows=affected_rows, last_insert_id=last_insert_id) # 返回结果
        }) |> do.call(rbind, args=_) |> tibble() # 执行数据查询
        dbCommit(con) # 提交事务
        if( simplify & length(dataset) == 1 )  dataset[[1]]  else  dataset  # 返回结果数据集
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
ctsql <- function( dfm, tbl ) {
  tbl <- if(missing(tbl)) deparse( substitute( dfm ) )  else tbl #  提取数据表名
  dfm |> lapply(\(e, t=typeof(e), cls=class(e), # 基础类型与class包含高级类型list
      n=as.integer(Reduce(\(acc, a) max(acc, max(acc, nchar(a))), x=as.character(e), init=0) * 1.5), # 列数据宽度
      default_type=sprintf('varchar(%s)', n) # 默认类型
    ) switch(t, # 类型判断
        `logical`='bool', # 布尔类型
        `integer`=if(cls=='factor') default_type else 'integer', # 列表类型
        `double`=if(any(grepl(pattern="Date|POSIXct|POSIXt", x=cls))) "datetime" else 'double', # 列表类型
        `list`='json', # 列表类型
        default_type # 默认类型 
    )) |> (\(x) # 获取字段定义
      sprintf("create table %s (\n  %s \n)\n", tbl, paste(names(x), x, collapse=",\n  ")) # 数据表创建语句 
    ) () # SQL 创建表语句
}

#' 表数据插入SQL
#' @param dfm 数据框数据
#' @param tbl  数据表名
#' @return 表数据插入SQL
insql <- function( dfm, tbl ) {
  tbl <- if(missing(tbl)) deparse( substitute( dfm ) )  else tbl #  提取数据表名
  keys <- names( dfm ) |> paste(collapse=", ") # 列名列表
  values <- dfm |> lapply(\(e, t=typeof(e), cls=class(e)) # 记录值列表的各个字段值处理：
    switch(t, # 元素类型判断，决定是否用单引号把数值括起来，数值与逻辑值不用，list 转换成列表
      `logical`=e, # 逻辑类型，保持原值不变
      `integer`=if(cls=='factor') sprintf("'%s'", e) else e, # 整数类型，保持原值不变
      `double`=if(any(grepl(pattern="Date|POSIXct|POSIXt", x=cls))) sprintf("'%s'", e) else e, # 双精度，保持原值不变
      `list`=sprintf("'%s'", gsub("'", "''", toJSON(e))), # list类型，转换成JSON, 并对单引号进行转义
      sprintf("'%s'", gsub("'", "''", e)) # 默认类型，使用单引号'给括起来, 并对单引号进行转义
    )) |> do.call(\(...) mapply(\(...) paste(..., sep=', ', collapse=','), ...), args=_) |> # 行映射,此处\(...)有层级差异,内为字段外为数据行是两个不同变量
    sprintf(fmt='( %s )') |> paste(collapse=',\n  ') # 值列表
  sprintf( "insert into %s (%s) values \n  %s\n", tbl, keys, values ) # SQL 插入记录行数据（多行）语句
}

#' 表数据更新SQL
#' @param dfm 数据框数据
#' @param tbl 数据表名
#' @param pk 数据主键
upsql <- \(dfm, tbl, pk="id") { # 数据更新
    nms <- names(dfm) # 提取各个数据列名
    stopifnot(!is.na(match(pk, nms))) # dfm的名称中必须包含主键名pk
    flds <- sapply(nms, \(i) sprintf("%s='%s'", i, dfm[, i, drop=T] |> gsub("'", "''",x=_))) |> 
      apply(1, \(line) paste(line[-match(pk, nms)], collapse=",\n  ")) # 字段拼接
    sprintf("update %s set\n  %s \nwhere %s='%s'\n", tbl, flds, pk, dfm[, pk]) # 数据更新的SQL语句
} # upsql

# 自定义主机与数据库
sqlquery.h10ctp2 <- partial(sqlquery, host="192.168.1.10", dbname="ctp2")
sqlquery.ctp <- partial(sqlquery, host="192.168.1.10", dbname="ctp")
