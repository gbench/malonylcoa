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
  attach(new.env(),  name=paste0(xxxconfig, ".underlay"), pos=match(".SqlQueryEnv", search()) + 1) |> with({ # 在环境xxxconfig里定制相关的系统配置
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

# 提取指定期货合约的OHLCV数据（转换成xts)
kl <- function(symbol = "kl_rb2605", n = -1) {
  sql <- sprintf("select * from %s %s", symbol, ifelse(n>0, paste("limit", n), ""))
  sqlquery(sql) |> transform(TS = as.POSIXct(TS, format = "%Y%m%d%H%M")) |> arrange(TS) |>
    with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME, VOL0, VOL1), order.by = TS))
} # kl

  