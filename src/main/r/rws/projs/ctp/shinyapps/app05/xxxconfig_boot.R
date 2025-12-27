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

kl <- local({
  kl_cache <- new.env(hash=T)
  \(sym='kl_rb2605',startime=NA, endtime=NA) { # 开始时间为NA时候表示获取所有之前数据！
    k <- paste(sym) # 缓存key
    old <- get0(k,envir=kl_cache, ifnotfound=data.frame(TS=character()))
    last_uptime <- if (nrow(old)) max(old$TS) else 0 # 上一次的更新时间
    # 查询范围完全在缓存内直接返回
    if (!is.na(startime) && !is.na(endtime) && min(old$TS)<=startime && max(old$TS)>=endtime) 
      return(old[old$TS>=startime & old$TS<=endtime,])
    sql <- sprintf("SELECT * FROM %s WHERE TS>='%s'%s ORDER BY TS", sym, last_uptime, 
        paste0(if (!all(is.na(c(startime,endtime))))
        sprintf(" AND %s",paste(c(if(!is.na(startime)) sprintf("TS>='%s'",startime),
            if(!is.na(endtime)) sprintf("TS<='%s'",endtime)), collapse=" AND ")), ""))
    cat(sql, "\n") # 打印查询sql
    nu <- sqlquery(sql) # 数据查询
    mu = old[old$TS!=last_uptime, ] # 去除了last_uptime的本地数据
    flag <- is.na(startime) && sum(nu$TS==last_uptime)>0
    res <- if(flag) rbind(mu, nu) else nu  # 删尾拼新
    if(flag) assign(k, res, envir=kl_cache) else nu # 仅当合并拼接的时候才更新缓存
  }
})

# klinechart的抓取K线数据
fetch_json <- local({
  st_cache <- new.env() # 时间状态缓存
  
  function(sym="kl_rb2605") {
    startime <- get0(sym, envir=st_cache, ifnotfound=NA)
    x <- kl(sym, startime=startime) # 读取K线数据
    cat("nrow", nrow(x), "startime", startime, "IDX",last(x)$IDX, "VOLUME",last(x)$VOLUME, "\n -- \n") # 获得的最新数据
    assign(sym, max(x$TS), envir=st_cache) # 更新时间缓存
    if (!NROW(x)) return(NULL)
    with(x, purrr::transpose(list( # 纯列表数组，字段名严格对应 KlineCharts 要求, transpose 转成行向量
      timestamp = as.numeric(as.POSIXct(TS, format="%Y%m%d%H%M")) * 1000, # 毫秒级的时间戳
      open = OPEN, high = HIGH, low = LOW, close = CLOSE, volume = VOLUME, idx= IDX
    ))) |> toJSON(auto_unbox = TRUE)
  }
})

# 清空数据缓存
invalidate_cache <- \(){
  c(environment(kl)$kl_cache, environment(fetch_json)$st_cache) |>  # 提取缓存对象
    lapply(\(e) rm(list=ls(e), envir=e))
}

# 1. 查询kl_rb2605所有数据（首次查询，缓存未命中）
# data_all <- kl("kl_rb2605")

# 2. 再次查询相同范围（缓存命中，直接返回）
# data_all_cached <- kl("kl_rb2605")

# 3. 查询指定时间范围（202512242200 至 202512242255）
# data_range <- kl("kl_rb2605", start = "202512242200", end = "202512242255")

# 4. 清空缓存并全量查询
# data_refresh <- kl("kl_rb2605", flag = TRUE)

# 5. 1分钟后再次查询（自动刷新当前分钟数据，历史数据复用缓存）
# Sys.sleep(60)
# data_refresh_current <- kl("kl_rb2605")

# sqlquery("select * from kl_rb2605 order by TS") |> tail(5)
# kl() |> tail()
