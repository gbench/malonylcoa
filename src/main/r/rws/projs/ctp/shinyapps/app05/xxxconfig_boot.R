# ------------------------------------------------------------------------------
# xxxconfig的环境启动（使用jdbc方法来拦截ignite) 类似于Photoshop的多级图层的underlay+overlay的联合图层绘图
# ------------------------------------------------------------------------------
# install.packages(c("rJava", "RJDBC"), repos = "https://mirrors.ustc.edu.cn/CRAN/")

batch_load() # 导入基础库，引入sqlquery系列函数
ss("rJava,RJDBC") |> setNames(nm=_) |> lapply(require, character.only=T) |> unlist() # 加载RJDBC的基础库
xxxconfig <- "igniteconfig" # 为环境命名

# 系统启动
initialize <- \() if(!"igniteconfig.underlay" %in% search()) {
  # attach环境xxxconfig，加载位置pos为".SqlQueryEnv"之的直接后继，进而可以拦截base::getOption以便读取自定义的系统配置！
  attach(new.env(), name=paste0(xxxconfig, ".underlay"), pos=match(".SqlQueryEnv", search()) + 1) |> with({ # 在环境xxxconfig里定制相关的系统配置
    jars <- list.files("D:/sliced/develop/ignite/ignite3-3.1.0/ignite3-cli-3.1.0/lib", pattern = "\\.jar$", full = TRUE)
    drv <- RJDBC::JDBC(driverClass="org.apache.ignite.jdbc.IgniteJdbcDriver", classPath=paste(jars, collapse= ";"), identifier.quote="`")
    localcfg <- list(sqlquery.drv=drv, sqlquery.host="jdbc:ignite:thin://192.168.1.41:10800") # 本地环境参数设置
    getOption <- \ (x, default = NULL) localcfg[[x]] %||% base::getOption(x, default) # 联合base图层做叠化绘图的PS技法
  }) # 在环境xxxconfig里定制相关的系统配置
  attach(new.env(), name=paste0(xxxconfig, ".overlay")) |> with({ # 头前拦截，为dbConnect函数增加url参数
    dbConnect <- \(...) do.call(DBI::dbConnect, args=c(list(...), url=localcfg[["sqlquery.host"]]))
  }) # 头前拦截，为dbConnect函数增加url参数
}

# 移除igniteconfig的图层配置
uninitialize <- \() search() |> grep(pattern=xxxconfig, value=T) |> lapply(\(e) do.call(detach, args=list(e)))  # 卸载环境

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

  #' @param startime NA表示增量同步，从ignite读取数据到本地，非NA，表示从缓存中查询的起始时间！这是klines的模式标志！
  #' @param endtime 截止时间
  \(sym='kl_rb2605', startime=NA, endtime=NA) { # 开始时间为NA时候表示获取所有之前数据！
    k <- paste(sym) # 缓存key
    lc <- get0(k, envir=cache, ifnotfound=data.frame(TS=character())) # 本地拷贝LocalCopy默认为空列表(0行带有TS列,防止lc$TS返回NULL)
    last_uptime <- if(nrow(lc)) max(lc$TS) else 0 # 上一次的更新时间
    # 查询范围完全在缓存内直接返回
    if (!is.na(startime) && !is.na(endtime) && min(lc$TS)<=startime && max(lc$TS)>=endtime) 
      return(lc[lc$TS>=startime & lc$TS<=endtime,])
    sql <- sprintf("SELECT * FROM %s WHERE TS>='%s'%s ORDER BY TS", sym, last_uptime, 
        paste0(if (!all(is.na(c(startime, endtime))))
        sprintf(" AND %s", paste(c(if(!is.na(startime)) sprintf("TS>='%s'", startime),
          if(!is.na(endtime)) sprintf("TS<='%s'", endtime)), collapse=" AND ")), ""))
    cat(sql, "\n") # 打印查询sql

    ds <- sqlquery(sql) # 使用SQL查询结果集(dataset), 原始结果集，只查一次
    # 1. 增量过滤：只保留 TS >= 缓存尾部的数据，剔除迟到旧记录
    nu <- ds %>% with(.[TS>=last_uptime, ]) # # 只拿 >= 缓存尾部的数据
    # 2. “同名 TS”是唯一信号：仅当 nu 带回同名 TS 才砍掉旧尾，否则原封不动
    mu <- if(sum(nu$TS==last_uptime)>0) lc[lc$TS!=last_uptime, ] else lc # 若带回同名 TS 才砍掉旧尾 否则 原样保留
    # 3. 心跳模式（startime 为 NA）才把 mu 与 nu 拼成完整缓存；区间查询直接返回 nu，不污染缓存
    res <- if(is.na(startime)) rbind(mu, nu) else ds  # 删尾拼新

    if(is.na(startime)) assign(k, res, envir=cache) else nu # NA心跳模式才会更新缓存，心跳模式拼新缓存，区间查询原样返回 ds，绝不回写
  }
})

# klinechart的抓取K线数据
fetch_json <- local({
  cache <- new.env() # 时间状态缓存
  
  \(sym="kl_rb2605") {
    startime <- get0(sym, envir=cache, ifnotfound=NA)
    x <- klines(sym, startime=startime) # 读取K线数据
    with(last(x), cat("nrow", nrow(x), "startime", startime, "IDX", IDX, "VOLUME", VOLUME, "\n -- \n")) # 获得的最新数据
    assign(sym, max(x$TS), envir=cache) # 更新时间缓存
    if (!NROW(x)) return(NULL)
    with(x, purrr::transpose(list( # 纯列表数组，字段名严格对应 KlineCharts 要求, transpose 转成行向量
      timestamp = as.numeric(as.POSIXct(TS, format="%Y%m%d%H%M")) * 1000, # 毫秒级的时间戳
      open = OPEN, high = HIGH, low = LOW, close = CLOSE, volume = VOLUME, idx= IDX
    ))) |> toJSON(auto_unbox = TRUE)
  }
})

# 清空数据缓存
invalidate_cache <- \(){
  c(klines, fetch_json) |> lapply(\(e) get("cache", envir=environment(e))) |>  # 提取缓存对象
    lapply(\(e) rm(list=ls(e), envir=e))
}

# # 1. 查询kl_rb2605所有数据（首次查询，缓存未命中）
# cat("1. 查询kl_rb2605所有数据（首次查询，缓存未命中）\n")
# data_all <- klines("kl_rb2605")

# cat("2. 再次查询相同范围（缓存命中，直接返回）\n")
# data_all_cached <- klines("kl_rb2605")

# cat("3. 查询指定时间范围（202512242200 至 202512242255）\n")
# data_range <- klines("kl_rb2605", start = "202512242200", end = "202512242255")

# cat("4. 清空缓存并全量查询\n")
# invalidate_cache()
# data_refresh <- klines("kl_rb2605")

# cat("5. 1分钟后再次查询（自动刷新当前分钟数据，历史数据复用缓存）\n")
# # Sys.sleep(60)
# # data_refresh_current <- klines("kl_rb2605")

# cat(" 4. 清空缓存并全量查询\n")
# sqlquery("select * from kl_rb2605 order by TS") |> tail(5)
# klines() |> tail()
