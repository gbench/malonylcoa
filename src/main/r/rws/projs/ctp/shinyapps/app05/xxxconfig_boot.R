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
  e <- new.env(hash=T)
  \(sym='kl_rb2605',start=NA,end=NA,flush=F){
    if (flush) {rm(list=ls(e),envir=e); return()}
    k <- paste(sym,start,end,sep="_")
    old <- get0(k,envir=e, ifnotfound=data.frame(TS=character()))
    maxT <- if (nrow(old)) max(old$TS) else "1970-01-01 00:00:00"
    # 查询范围完全在缓存内直接返回
    if (!is.na(start) && !is.na(end) && min(old$TS)<=start && max(old$TS)>=end) 
      return(old[old$TS>=start & old$TS<=end,])
    new <- sqlquery(sprintf("SELECT * FROM %s WHERE TS>='%s'%s ORDER BY TS",
      sym, maxT, paste0(if (!all(is.na(c(start,end))))
        sprintf(" AND %s",paste(c(if(!is.na(start))sprintf("TS>='%s'",start),
            if(!is.na(end))  sprintf("TS<='%s'",end)),
          collapse=" AND ")),"")))
    # 删尾拼新
    res <- rbind(old[old$TS!=maxT,], new)
    assign(k,res,envir=e); res
  }
})

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
# kl() |> tail(5)

