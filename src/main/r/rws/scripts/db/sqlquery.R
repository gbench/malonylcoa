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

# 数据库函数
dbfun <- function(f, ...) {
  dbcfg <- list(...) # 数据库连接参数配置
  defaultcfg <- list(drv=MySQL(), host="localhost", user="root", password="123456", port=3371, dbname="ctp2") # 默认连接参数
  readcfg <- \(key) dbcfg[[key]] %||% defaultcfg[[key]] #  带有默认值的配置参数key的值读取 

  # 执行SQL语句 
  function (sql) {
    # 数据库连接
    try({
      keys <- "drv,host,user,password,port,dbname" |> strsplit(",") |> unlist() # 配置参数keys向量
      con <- structure(keys, names=keys) |> lapply(readcfg)  |> do.call(dbConnect, args=_ ) # 读取配置并获得数据库连接
      on.exit(dbDisconnect(con), add=TRUE) # 注册函数运行结束时关闭调该数据库连接con
      f(con) # 执行sql
    }) # 数据库连接
  } # function (sql)
} # dbfun

# 执行SQL语句查询数据结果（dbGetQuery模式)
# sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
# simplify 是否对查询结果做简化处理后再返回，
#   T:  简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
# n 查询结果的返回最大数量
sqlquery <- function(sql, simplify=T, n=-1, ...) {
  # 连接使用函数
  dbfun(\ (con) { # 使用数据库连接进行查询结果数据集
    dataset <- c(list(), sql) |> lapply(compose(tibble, partial(dbGetQuery, con=con, n=n))) |> structure(names=sql)# 执行数据查询
    if( simplify & length(dataset) == 1 )  dataset[[1]]  else  dataset  # 返回结果数据集
  }, ...)(sql) # 连接使用函数
}

# 执行SQL语句（dbExecute模式)
# sql 语句是向量化的，当 sql长度大于1，返回一个 tibble 列表，否则 返回一个 tibble 对象
# simplify 是否对查询结果做简化处理后再返回，
#   T:  简化处理，即 对于只有单个元素的查询结果（单元素列表），直接返回该元素；
#   F：不做简化处理，直接返回 查询结果（列表），不论该结果是否为单元素列表
# 返回结果是 affected_rows, last_insert_id 两列的数据框，对于
# insert into t_user(name) values('name_1'),('name_2'),...,('name_n') 一条语句插入多条数据
# 的情况affected_rows返回实际插入的数量，last_insert_id返回插入的第一条数据的id
# 其余id请根据last_insert_id,affected_rows依次计算，比如name_1的id为x，那么name_2就是x+1,...，name_n为x+n-1
# 返回实际插入数据的id为: seq(from=last_insert_id,lengout.out=affected_rows)
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

# 自定义主机与数据库
sqlquery.h10ctp2 <- partial(sqlquery, host="192.168.1.10", dbname="ctp2")
