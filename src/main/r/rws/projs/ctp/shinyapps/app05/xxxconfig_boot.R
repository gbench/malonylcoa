# ------------------------------------------------------------------------------
# xxxconfig的环境启动（使用jdbc方法来拦截ignite) 类似于Photoshop的多级图层的underlay+overlay的联合图层绘图
# ------------------------------------------------------------------------------

# 安装java环境与jdbc接口包文件
# install.packages(c("rJava", "RJDBC"), repos = "https://mirrors.ustc.edu.cn/CRAN/")

batch_load() # 导入基础库，引入sqlquery系列函数
ss("rJava,RJDBC") |> setNames(nm=_) |> lapply(require, character.only=T) |> unlist() # 加载RJDBC的基础库
xxxconfig <- "igniteconfig" # 为环境命名

# 系统启动
initialize <- \() if(!gettextf("%s.underlay", xxxconfig) %in% search()) {
  # attach环境xxxconfig，加载位置pos为".SqlQueryEnv"之的直接后继，进而可以拦截base::getOption以便读取自定义的系统配置！
  attach(new.env(), name=paste0(xxxconfig, ".underlay"), pos=match(".SqlQueryEnv", search()) + 1) |> with({ # 在环境xxxconfig里定制相关的系统配置
    jars <- list.files("D:/sliced/develop/ignite/ignite3-3.1.0/ignite3-cli-3.1.0/lib", pattern = "\\.jar$", full = TRUE)
    drv <- RJDBC::JDBC(driverClass="org.apache.ignite.jdbc.IgniteJdbcDriver", classPath=paste(jars, collapse= ";"), identifier.quote="`")
    localcfg <- list(sqlquery.drv=drv, sqlquery.host="jdbc:ignite:thin://192.168.1.41:10800") # 本地环境参数设置
    getOption <- \ (x, default = NULL) localcfg[[x]] %||% base::getOption(x, default) # 联合base图层做叠化绘图的PS技法
  }) # 在环境xxxconfig里定制相关的系统配置
  attach(new.env(), name=paste0(xxxconfig, ".overlay")) |> with({ # 头前拦截，为dbConnect函数增加url参数并定制共享连接
    shared_conn <- NULL #  共享连接
    dbConnect <- \(...) if(!is.null(shared_conn) && DBI::dbIsValid(shared_conn)) shared_conn else shared_conn <<- do.call(DBI::dbConnect, args=c(list(...), url=localcfg[["sqlquery.host"]]))
    dbDisconnect <- \(conn) if(!is.null(shared_conn)) 1 |> invisible() else DBI::dbDisconnect (conn) # 拦截状态什么都不做！（返回1）表示拒绝关闭！
  }) # 头前拦截，为dbConnect函数增加url参数
}

# 移除igniteconfig的图层配置
uninitialize <- \() {
  as.environment(gettextf("%s.overlay", xxxconfig)) |> with(if(!is.null(shared_conn) && DBI::dbIsValid(shared_conn)) {dbDisconnect(shared_conn); message("数据库连接已关闭") }) # 断数据连接！
  search() |> grep(pattern=xxxconfig, value=T) |> lapply(\(e) do.call(detach, args=list(e)))  # 卸载环境
}

# K线刷新函数（可以把klines理解为一个简单的浏览器，可以访问动态主页sym上的数据）
# 
# 无参调用 ≈ 心跳 + 增量同步
# 有参调用 ≈ 纯缓存查询
# 
# 这种“零参即心跳”的设计，让同一套接口同时承担了三种职责，而调用方根本不用额外记忆任何动词：
# 1.启动/恢复时——klines("rb")
#   空缓存 → 拉全量 → 本地建立基准。
# 2.盘中实时——循环 klines("rb")
#   每次只拉回“上次基准至今”的增量，自动拼到缓存尾部；
#   调用间隔就是心跳频率，想快就快、想慢就慢，无需另开线程。
# 3.回测/绘图——klines("rb", "2025-12-01", "2025-12-02")
#   直接切片，不走网络；
#   且因为返回的是同一套列名、同一类对象，后续代码无需分支判断。
# 换句话说，作者用“startime参数是否为NA”这一个比特信息，把数据同步逻辑与数据使用逻辑彻底解耦：
#   上层策略代码只管“我要哪段”，
#   底层缓存代码默默决定“该查多少、该怎么拼”。
# 一行代码就完成“心跳、补数、落盘、查询”四件事！
klines <- local({
  cache <- new.env(hash=T) # K线缓存
  .get <- \(x) get0(x, envir=cache, ifnotfound=data.frame(TS=character())) # 缓存读写
  .assign <- \(x, value) assign(x, value, envir=cache) # 环境赋值

  #' @param startime NA表示增量同步，从ignite读取数据到本地，非NA，表示从缓存中查询的起始时间！这是klines的模式标志！
  #' @param endtime 截止时间
  \(sym='KL_RB2605', startime=NA, endtime=NA) { # 开始时间为NA时候表示获取所有之前数据！
    .k <- substitute(sym) # 提取参数符号
    k <- tryCatch(sym, error=\(e) as.character(.k)) # 如果求值失败则把参数符号名作为合约代码（缓存key)
    lc <- .get(k) # 本地拷贝LocalCopy默认为空列表(0行带有TS列,防止lc$TS返回NULL)
    updt <- if(nrow(lc)) max(lc$TS) else 0 # 上一次的更新时间

    if (!is.na(startime) && !is.na(endtime) && nrow(lc)>0 && min(lc$TS)<=startime && max(lc$TS)>=endtime) { # 查询范围完全在缓存内直接返回
      lc[lc$TS>=startime & lc$TS<=endtime, , drop = FALSE]
    } else { # 缓存未命中
      flag <- is.na(startime) # NA心跳模式&更新缓存
      .startime <- if (flag) updt else startime # 开始时间
      rng <- \(s, e, op=c("TS>=", "TS<=")) paste0(op, "'", c(s, e), "'") |> (\(x) x[!endsWith(x, "'NA'")]) () |> 
        paste(collapse = " AND ") |> (\(s) if(!nzchar(s)) "" else gettextf("WHERE %s", s)) () # 时间范围
      sql <- sprintf("SELECT * FROM %s %s ORDER BY TS", k, rng(.startime, endtime)) # 读取SQL的拼装
      cat(sql, "\n") # 打印查询sql
      ds <- sqlquery(sql) # 使用SQL查询结果集(dataset), 原始结果集，只查一次
      # 1. 增量过滤：只保留 TS >= 缓存尾部的数据，剔除迟到旧记录
      nu <- ds[ds$TS>=updt, ] # 只拿 >= 缓存尾部的数据
      # 2. “同名 TS”是唯一信号：仅当 nu 带回同名 TS 才砍掉旧尾，否则原封不动
      mu <- if (sum(nu$TS==updt)>0) lc[lc$TS<updt, ] else lc # 若带回同名 TS 才砍掉旧尾 否则 原样保留
      # 3. 心跳模式（startime 为 NA）才把 mu 与 nu 拼成完整缓存；区间查询直接返回 nu，不污染缓存
      if(flag) .assign(k, rbind(mu, nu)) else ds # NA心跳模式才会更新缓存(删尾拼新)，心跳模式拼新缓存，区间查询原样返回 ds，绝不回写
    } # if 
  } # 匿名函数
})

# klinechart的抓取K线数据
fetch_json <- local({
  cache <- new.env() # 时间状态缓存
  
  \(sym="KL_RB2605", startime=NA) {
    .startime <- if(is.na(startime)) get0(sym, envir=cache, ifnotfound=NA) else startime
    x <- klines(sym, startime=.startime) # 读取K线数据
    with(last(x), cat("sym", sym, "nrow", nrow(x), "startime", startime, "IDX", IDX, 
      "VOLUME", VOLUME,  "CLOSE", CLOSE, "\n -- \n")) # 获得的最新数据
    assign(sym, max(x$TS), envir=cache) # 更新时间缓存
    if (!nrow(x)) return(NULL)
    with(x, list(instrument = sym, ds = purrr::transpose(list( # 纯列表数组，字段名严格对应 KlineCharts 要求, transpose 转成行向量
      timestamp = as.numeric(as.POSIXct(TS, format="%Y%m%d%H%M")) * 1000, # 毫秒级的时间戳
      open = OPEN, high = HIGH, low = LOW, close = CLOSE, volume = VOLUME, oint = OINT1,  idx = IDX
    )))) |> toJSON(auto_unbox = TRUE)
  } # 匿名函数
})

# 清空数据缓存: 把时间戳TS早于startime的过期数据给予砍除；返回startime(包含)以后的数据，若startime为NA则清除所有数据！
invalidate_kline_caches <- \(startime=NA) {
  (  if (!is.na(startime)) # with环境要继承于当前函数的执行环境而不可with(environment(klines), ... ), # 即必须手动创建with环境，以通过继承链来发现startime！
       list(cache=environment(klines)$cache) |> with(lapply(ls(cache) |> setNames(nm=_), 
         \(k, x=get(k, envir=cache)) assign(k, x[x$TS>=startime, , drop=FALSE], envir=cache))) # 砍掉过期数据
     else c(klines, fetch_json) |> lapply(\(e) get("cache", envir=environment(e))) |>  # 提取缓存对象
       lapply(\(e) rm(list=ls(e), envir=e)) # 清空所有缓存对象
  ) |> invisible() # 隐藏执行结果
}

# kline缓存导出, home 导出文件存放位置
dump_kline_cache <- \(home="data") {
  handle.csv <- \(x, name, dir=home) { if(!dir.exists(dir)) dir.create(dir, recursive=T); write.csv(x, "./%s/%s.csv" |> gettextf(dir, name)); nrow(x) } # csv 文件的写入本地
  environment(klines) |> with(ls(envir=cache) |> setNames(nm=_) |> lapply(\(nm) get(nm, envir=cache))) %>% mapply(FUN=handle.csv, ., names(.))
}

# ------------------------------------------------------------------------------------------------------------------------------------
# 接口使用示例
# ------------------------------------------------------------------------------------------------------------------------------------
#  初始化前环境结构
# >  search()
#  [1] ".GlobalEnv"        "package:stats"     "package:graphics" 
#  [4] "package:grDevices" "package:utils"     "package:datasets" 
#  [7] ".SqlQueryEnv"      "package:methods"   "Autoloads"        
# [10] "package:base"     

# 数据 环境初始化，search 里增加了igniteconfig.overlay；igniteconfig.underlay 两个拦截层环境！
# 这样sqlquery就直接连接到了ignite了
# initialize();  search()
# > 	
# i> search()
# i [1] ".GlobalEnv"            "igniteconfig.overlay"  "package:RJDBC"        
# i [4] "package:rJava"         "package:RPostgres"     "package:janitor"      
# i [7] "package:data.table"    "package:quantmod"      "package:TTR"          
# i[10] "package:xts"           "package:zoo"           "package:jsonlite"     
# i[13] "package:lubridate"     "package:forcats"       "package:stringr"      
# i[16] "package:dplyr"         "package:purrr"         "package:readr"        
# i[19] "package:tidyr"         "package:tibble"        "package:ggplot2"      
# i[22] "package:tidyverse"     "package:RMySQL"        "package:DBI"          
# i[25] "package:stats"         "package:graphics"      "package:grDevices"    
# i[28] "package:utils"         "package:datasets"      ".SqlQueryEnv"         
# i[31] "igniteconfig.underlay" "package:methods"       "Autoloads"            
# i[34] "package:base" 
    
#  我们可以查看当前ignite里各种实时更新的缓存表：KL_XXX是K线表，TK_XXX是TK_XXX是tickdta数据！
#  > sqlquery("select table_name from system.tables")
#  [1] "TK_RB2605C3100" "PERSON"         "TK_RB2603"      "TK_MA601"      
#  [5] "T_MTCARS"       "KL_MA601"       "KL_RB2605"      "KL_RB2603"     
#  [9] "KL_RB2601"      "KL_EG2601"      "KL_RB2605P3100" "KL_RB2605P3150"
#  [13] "KL_RB2605C3150" "KL_RB2605C3100" "TK_EG2601"      "TK_RB2605"     
#  [17] "KL_IF2601"      "TK_AO2601"      "TK_RB2605P3100" "KL_AO2601"     
#  [21] "T_IRIS"         "TK_RB2601"     
 
# # 1. 查询KL_RB2605所有数据（首次查询，缓存未命中）
# cat("1. 查询KL_RB2605所有数据（首次查询，缓存未命中）\n")
# data_all <- klines("KL_RB2605"); data_all |> tail()
# >
# SELECT * FROM KL_RB2605 WHERE TS>='0' ORDER BY TS 
# # A tibble: 6 × 11
#   TS            OPEN  HIGH   LOW CLOSE VOLUME   VOL0   VOL1 IDX    TIMES UPTIME                 
#   <chr>        <dbl> <dbl> <dbl> <dbl>  <dbl>  <dbl>  <dbl> <chr>  <dbl> <chr>                  
# 1 202512261058  3101  3102  3100  3101   2390 712842 715232 866306   120 2025-12-26 10:58:59.500
# 2 202512261059  3102  3105  3101  3103   7906 715234 723140 866852   120 2025-12-26 10:59:59.500
# 3 202512261100  3104  3105  3102  3102   3818 723152 726970 867366   120 2025-12-26 11:00:59.500
# 4 202512261101  3103  3103  3102  3103   2019 726973 728992 867854   120 2025-12-26 11:01:59.500
# 5 202512261102  3102  3105  3102  3104   2641 728993 731634 868336   120 2025-12-26 11:02:59.500
# 6 202512261103  3105  3105  3103  3104   2022 731635 733657 868647    66 2025-12-26 11:03:32.500
# >

# cat("2. 再次查询相同范围（缓存命中部分，直接返回，更新部分增量读取, 202512261103的VOLUME,IDX等进行了更新）\n")
# data_all_cached <- klines("KL_RB2605"); data_all_cached |> tail(1)
# > 
# SELECT * FROM KL_RB2605 WHERE TS>='202512261103' ORDER BY TS 
# # A tibble: 6 × 11
#   TS            OPEN  HIGH   LOW CLOSE VOLUME   VOL0   VOL1 IDX    TIMES UPTIME                 
#   <chr>        <dbl> <dbl> <dbl> <dbl>  <dbl>  <dbl>  <dbl> <chr>  <dbl> <chr>                  
# 1 202512261058  3101  3102  3100  3101   2390 712842 715232 866306   120 2025-12-26 10:58:59.500
# 2 202512261059  3102  3105  3101  3103   7906 715234 723140 866852   120 2025-12-26 10:59:59.500
# 3 202512261100  3104  3105  3102  3102   3818 723152 726970 867366   120 2025-12-26 11:00:59.500
# 4 202512261101  3103  3103  3102  3103   2019 726973 728992 867854   120 2025-12-26 11:01:59.500
# 5 202512261102  3102  3105  3102  3104   2641 728993 731634 868336   120 2025-12-26 11:02:59.500
# 6 202512261103  3105  3105  3103  3104   2097 731635 733732 868694    76 2025-12-26 11:03:37.500
# >

# 直接读取缓存数据
# environment(klines) |> with(with(cache, KL_RB2605))
# A tibble: 1,305 × 11
#    TS            OPEN  HIGH   LOW CLOSE VOLUME   VOL0   VOL1 IDX    TIMES UPTIME                 
#    <chr>        <dbl> <dbl> <dbl> <dbl>  <dbl>  <dbl>  <dbl> <chr>  <dbl> <chr>                  
#  1 202512221330  3122  3124  3122  3123    413 724908 725321 141555    30 2025-12-22 13:30:59.500
#  2 202512221331  3124  3124  3122  3124   1929 725342 727271 142149   120 2025-12-22 13:31:59.500
#  3 202512221332  3123  3125  3123  3125   1251 727272 728523 142714   120 2025-12-22 13:32:59.500
#  4 202512221333  3124  3124  3121  3121   2510 728561 731071 143297   120 2025-12-22 13:33:59.500
#  5 202512221334  3121  3124  3121  3124   1842 731071 732913 143852   120 2025-12-22 13:34:59.500
#  6 202512221335  3124  3124  3122  3123   1013 733023 734036 144383   118 2025-12-22 13:35:59.500
#  7 202512221336  3122  3125  3122  3124   2069 734038 736107 144969   120 2025-12-22 13:36:59.500
#  8 202512221337  3124  3124  3123  3124    431 736109 736540 145507   119 2025-12-22 13:37:59.500
#  9 202512221338  3124  3124  3121  3122   1794 736541 738335 146079   120 2025-12-22 13:38:59.500
# 10 202512221339  3122  3123  3121  3122    948 738335 739283 146659   119 2025-12-22 13:39:59.500
# # ℹ 1,295 more rows
# # ℹ Use `print(n = ...)` to see more rows
# >

# cat("3. 查询指定时间范围（202512221331 至 202512221331）\n")
# data_range <- klines("KL_RB2605", startime = "202512221331", endtime = "202512221335"); data_range
# > 
# # A tibble: 5 × 11
#   TS            OPEN  HIGH   LOW CLOSE VOLUME   VOL0   VOL1 IDX    TIMES UPTIME                 
#   <chr>        <dbl> <dbl> <dbl> <dbl>  <dbl>  <dbl>  <dbl> <chr>  <dbl> <chr>                  
# 1 202512221331  3124  3124  3122  3124   1929 725342 727271 142149   120 2025-12-22 13:31:59.500
# 2 202512221332  3123  3125  3123  3125   1251 727272 728523 142714   120 2025-12-22 13:32:59.500
# 3 202512221333  3124  3124  3121  3121   2510 728561 731071 143297   120 2025-12-22 13:33:59.500
# 4 202512221334  3121  3124  3121  3124   1842 731071 732913 143852   120 2025-12-22 13:34:59.500
# 5 202512221335  3124  3124  3122  3123   1013 733023 734036 144383   118 2025-12-22 13:35:59.500
# >

# cat("4. 清空缓存并全量查询\n")
# invalidate_kline_caches()
# data_refresh <- klines("KL_RB2605"); data_refresh
# > 
# SELECT * FROM KL_RB2605 WHERE TS>='0' ORDER BY TS 
# A tibble: 1,317 × 11
#    TS            OPEN  HIGH   LOW CLOSE VOLUME   VOL0   VOL1 IDX    TIMES UPTIME                 
#    <chr>        <dbl> <dbl> <dbl> <dbl>  <dbl>  <dbl>  <dbl> <chr>  <dbl> <chr>                  
#  1 202512221330  3122  3124  3122  3123    413 724908 725321 141555    30 2025-12-22 13:30:59.500
#  2 202512221331  3124  3124  3122  3124   1929 725342 727271 142149   120 2025-12-22 13:31:59.500
#  3 202512221332  3123  3125  3123  3125   1251 727272 728523 142714   120 2025-12-22 13:32:59.500
#  4 202512221333  3124  3124  3121  3121   2510 728561 731071 143297   120 2025-12-22 13:33:59.500
#  5 202512221334  3121  3124  3121  3124   1842 731071 732913 143852   120 2025-12-22 13:34:59.500
#  6 202512221335  3124  3124  3122  3123   1013 733023 734036 144383   118 2025-12-22 13:35:59.500
#  7 202512221336  3122  3125  3122  3124   2069 734038 736107 144969   120 2025-12-22 13:36:59.500
#  8 202512221337  3124  3124  3123  3124    431 736109 736540 145507   119 2025-12-22 13:37:59.500
#  9 202512221338  3124  3124  3121  3122   1794 736541 738335 146079   120 2025-12-22 13:38:59.500
# 10 202512221339  3122  3123  3121  3122    948 738335 739283 146659   119 2025-12-22 13:39:59.500
# # ℹ 1,307 more rows
# # ℹ Use `print(n = ...)` to see more rows
# >

# cat(" 5. 使用SQL查询: 根据查询时机，二者会存在差异\n")
# a <- sqlquery("select * from KL_RB2605 order by TS") ; b <- klines(); (a$VOLUME-b$VOLUME ) |> sum()
# >
# > a <- sqlquery("select * from KL_RB2605 order by TS") ; b <- klines(); (a$VOLUME-b$VOLUME ) |> sum()
# SELECT * FROM KL_RB2605 WHERE TS>='202512261126' ORDER BY TS 
# [1] 0
# > a <- sqlquery("select * from KL_RB2605 order by TS") ; b <- klines(); (a$VOLUME-b$VOLUME ) |> sum()
# SELECT * FROM KL_RB2605 WHERE TS>='202512261126' ORDER BY TS 
# [1] -28
# > 

# 使用完毕后unitialize，我们移除ingnite拦截层
# >
#  uninitialize()
#  [[1]]
#  <environment: 0x000001f93a729178>
#  attr(,"name")
#  [1] "igniteconfig.overlay"
#  
#  [[2]]
#  <environment: 0x000001f93a41e928>
#  attr(,"name")
#  [1] "igniteconfig.underlay"

# sqlquery 恢复默认模式的mysql环境了（我们把缓存数据持久化）
#
# 配置数据库到测试环境
# options("sqlquery.dbname"="test") # 连接大测试库
# > "select version() version; select database() db;" |> ss(";") |> sqlquery() |> lapply(unlist) |> cbind();
#                          [,1]   
# select version() version "9.1.0"
#  select database() db    "test"  
# >
#
# 连接到测试库 & 并 给于缓存数据持久化
# local({ # 缓存数据持久化
#   handle.csv <- \(x, name) { # 本地CSV文件写入
#       if(!dir.exists("data")) dir.create("data"); write.csv(x, "./data/%s.csv" |> gettextf(name))
#       nrow(x)
#   } # handle.csv
#   handle.ms <- \(x, name) { # MYSQL数据文件写入
#      params <- list(dfm=x, tbl=name) # 表格定义参数
#      ctsql <- do.call(ctsql, params); insql <- do.call(insql, params) # DDL 语句
#      sqlexecute("drop table if exists %s" |> gettextf(name)) # 删除前期数据表（我们全量更新）
#      sqlexecute(ctsql); sqlexecute(insql) # 创建&插入数据
#      sqlquery("select count(*) from %s" |> gettextf(name)) # 显示插入数据行
#   } # handle.ms
#   environment(klines) |> with(ls(envir=cache) |> setNames(nm=_) |> lapply(\(nm) get(nm, envir=cache))) %>% mapply(FUN=handle.ms, ., names(.))
# }) # local
# >
#
# 导出所有缓存数据
# source(file.choose()) # 加载xxxconfig_boot.R
# initialize() # 环境初始化
# sqlquery("select table_name from system.tables") |> grep(pattern="^KL", value=T) |> lapply(klines) |> system.time() # 导出数据并计时统计（初次数据量，二次较小）
# 
# > sqlquery("select table_name from system.tables") |> grep(pattern="^KL", value=T) |> lapply(klines) |> system.time() # 导出数据并计时统计（初次数据量，二次较小）
# SELECT * FROM KL_MA601 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2605 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2603 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2601 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_EG2601 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2605P3100 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2605P3150 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2605C3150 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_RB2605C3100 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_IF2601 WHERE TS>='0' ORDER BY TS 
# SELECT * FROM KL_AO2601 WHERE TS>='0' ORDER BY TS 
#    user  system elapsed 
#   10.23    1.02    7.37 
# 
# > sqlquery("select table_name from system.tables") |> grep(pattern="^KL", value=T) |> lapply(klines) |> system.time() # 导出数据并计时统计（初次数据量，二次较小）
# SELECT * FROM KL_MA601 WHERE TS>='202512262259' ORDER BY TS 
# SELECT * FROM KL_RB2605 WHERE TS>='202512262300' ORDER BY TS 
# SELECT * FROM KL_RB2603 WHERE TS>='202512262300' ORDER BY TS 
# SELECT * FROM KL_RB2601 WHERE TS>='202512262300' ORDER BY TS 
# SELECT * FROM KL_EG2601 WHERE TS>='202512262259' ORDER BY TS 
# SELECT * FROM KL_RB2605P3100 WHERE TS>='202512260907' ORDER BY TS 
# SELECT * FROM KL_RB2605P3150 WHERE TS>='202512260907' ORDER BY TS 
# SELECT * FROM KL_RB2605C3150 WHERE TS>='202512260907' ORDER BY TS 
# SELECT * FROM KL_RB2605C3100 WHERE TS>='202512260907' ORDER BY TS 
# SELECT * FROM KL_IF2601 WHERE TS>='202512260733' ORDER BY TS 
# SELECT * FROM KL_AO2601 WHERE TS>='202512270100' ORDER BY TS 
#    user  system elapsed 
#    1.36    0.25    2.92 
# > 
#
# dump_kline_cache() |> compose(t, t)() %>% structure(dimnames=list(row.names(.), "cnt")) # 转置一下好看一些！
#                 cnt
# KL_AO2601      1907
# KL_EG2601      1511
# KL_IF2601       424
# KL_MA601       1500
# KL_RB2601      1515
# KL_RB2603      1515
# KL_RB2605      1515
# KL_RB2605C3100  595
# KL_RB2605C3150  602
# KL_RB2605P3100  603
# KL_RB2605P3150  586
# 
# 数据文件已经导出
# > list.files("./data")
#  [1] "KL_AO2601.csv"      "KL_EG2601.csv"      "KL_IF2601.csv"      "KL_MA601.csv"       "KL_RB2601.csv"      "KL_RB2603.csv"      "KL_RB2605.csv"     
#  [8] "KL_RB2605C3100.csv" "KL_RB2605C3150.csv" "KL_RB2605P3100.csv" "KL_RB2605P3150.csv"

