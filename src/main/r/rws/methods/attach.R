# 基础库加载
batch_load()

# ------------------------------------------------------------------------------
# app01的环境启动
# ------------------------------------------------------------------------------

# 取消掉拦截方法
detach("app01") # 移除掉最近attach到的app01，并不会移除所有的名称为app01的环境
# 插入.SqlQueryEnv以拦截mask base的 getOption 方法。
attach(new.env(), "app01", pos=match(".SqlQueryEnv", search()) + 1) # 将环境app01设置为.SqlQueryEnv的父亲环境, 重复执行会添加多个重名app01的环境
identical(parent.env(as.environment(".SqlQueryEnv")), as.environment("app01")) # 验证父环境

# 定制应用配置
with(as.environment("app01"), { # 在应用app01内部设置该应用相关的环境配置
    localcfg <- list( # sqlquery的数据库参数预先设置
        #  sqlquery.drv=MySQL(), # 数据库驱动
        sqlquery.host="127.0.0.1", # 数据库主机
        sqlquery.user="root", # 数据库用户
        sqlquery.password="123456", # 数据库密码
        sqlquery.port=3371, # 数据库端口
        sqlquery.dbname="ctp2", # 数据库名
        sqlquery.schema="public,economics", # sqlquery.schema一般为PostgreSQL的参数配置，而MySQL通常不予使用
        #  sqlquery.rb.timestamp=(\(times=0, period=24*3600) (\() {on.exit(times<<-times+1); Sys.time()+times*period})) (), # 带状态时间戳生成函数
        sqlquery.rb.keys="##tbl,#startime,#endtime", # 结构记录构建器默认键名序列
        sqlquery.rb.interval=3, # 结构记录构建器默认时间时间跨度
        sqlquery.rb.instrument="rb2603", # 结构记录构建器默认合约
        sqlquery.env.pkgs = "RMySQL,tidyverse,jsonlite,quantmod,data.table,janitor,RPostgres,boot" # 环境依赖包！
    ) # 本地环境参数设置

    # 拦截getOption
    getOption <- \ (x, default = NULL) localcfg[[x]] %||% base::getOption(x, default) # 其余配置项目使用默认！
})

getOption("sqlquery.dbname") # 读取配置
sqlquery("select database()") # 执行SQL语句

# ------------------------------------------------------------------------------
# xxxconfig的环境启动
# ------------------------------------------------------------------------------
# attach环境xxxconfig，加载位置pos为".SqlQueryEnv"之的直接后继，进而可以拦截base::getOption以便读取自定义的系统配置！
attach(new.env(), "xxxconfig", pos=match(".SqlQueryEnv", search()) + 1) |> with({ # 在环境xxxconfig里定制相关的系统配置
    localcfg <- list(sqlquery.drv=RPostgres::Postgres(),sqlquery.dbname="latinus", sqlquery.host="127.0.0.1", sqlquery.port=5432, 
        sqlquery.user="postgres", sqlquery.password="123456", sqlquery.schema="economics") # 本地环境参数设置
    getOption <- \ (x, default = NULL) localcfg[[x]] %||% base::getOption(x, default) # 联合base图层做叠化绘图的PS技法
}) # 在环境xxxconfig里定制相关的系统配置

# 检测当前的数据库schema
sqlquery.pg("select current_schema") # 当前schema是economics

# mysql 数据库查询
sqlquery.ms <- partial(sqlquery, drv=MySQL(), host="127.0.0.1", port=3371, dbname="ctp", user="root", password="123456", 
    prettify = \(x) if (1 == length(x)) x[[1]] else bind_rows(x)) # 合并多张表为一张表

# 使用框架处理程序来执行dbplyr数据操作！
rb2605 <- (\(con, .log) { # 使用通用数据库连接管理框架dbfun.pg来托管数据库连接来进行dbply方式的合约处理
      .rb2605 <- sqlquery.ms("show tables") |> grep(pattern="rb2605", value=T, x=_) |> # 读取数据
          tail(2) |> sprintf(fmt="select * from %s limit 10") |> sqlquery.ms() # 从mysql里读取数据合约
      copy_to(con, .rb2605) # 把从mysql获得数据数据上传到postgresql数据库
      mtcars2 <- tbl(con, ".rb2605") |> collect() # 数据查询
  }) |> dbfun.pg(f=_, search_path = getOption("sqlquery.schema"), verbose=F) # 合约处理

# 数据执行数据
rb2605() |> print()

detach("xxxconfig") # 卸载环境

# ------------------------------------------------------------------------------
# 工作区的配置
# ------------------------------------------------------------------------------

# 添加会话图层专门处理螺纹钢数据
attach(new.env(), name="rb2605") |> with({ # 螺纹钢环境信息绘制
    ohlcs <- \(pattern="rb2605_2025121", startime="09:00", endtime="23:00", keys=4:8, flag=T) {
        rb <- record.builder("##tbl,#startime,#endtime") # 参数构建器
        ohlc <- \(tbl) `OHLCV1M` |> sqldframe(rb(tbl, startime, endtime)) %>% with(xts(.[, keys], as.POSIXct(paste(Date, Time)))) # 分钟K线函数
        sqlquery("show tables") |> sort() |> grep(pattern, value=T, x=_) |> setNames(nm=_) |> # 提取指定表名模式的tickdata交易数据表
            lapply(ohlc) |> (\(.) if(flag) do.call(rbind, args=.) else .) () # 根据flag标记进行多日K线数据的合并
    } # 多日表K线的求值函数
    btgen <- \(key, fn1) \(fn2, ...) expr(boot(!!ensym(key), compose(!!ensym(fn1), as.numeric, `[`), 500) |> # bootstrap自助法计算fn1统计量并予以fn2分析的生成器函数：fn1统计量函数, fn2 统计量分析函数
        with(eval(call(!!as.character(ensym(fn2)), t, !!!match.call(expand.dots=F)$...)))) # 自助法生成器BootstrapGenerator
    btgen2 <- \(key, fn1) \(fn2, ...) expr(boot(!!ensym(key), compose(!!ensym(fn1), as.numeric, `[`), 500) |> # bootstrap自助法计算fn1统计量并予以fn2分析的生成器函数：fn1统计量函数, fn2 统计量分析函数
        with(match.fun(!!ensym(fn2))(t, !!!match.call(expand.dots=F)$...))) # 自助法生成器BootstrapGenerator
    indgen <- \(fn) list(p=\(x) expr(btgen2(!!ensym(x), !!ensym(fn)) (quantile, probs=seq(0, 1, .25)))) |> with(\(...) # 指标生成器
        ensyms(...) |> setNames(nm=_) |> lapply(\(x) eval(p(!!ensym(x)))) |> (\(.) expr(with(xs, cbind(!!!.)))) ()) # 指标生成器IndicatorGenerator
    xs <- ohlcs("rb2605_2025121") # 生成图层主数据

    # 指标统计
    cat("mean:\n"); indgen(mean)(Open, High, Low, Close,  Volume) |> eval() |> print() # 使用指标生成器(均值统计)来计算指标分布形态
    cat("sd:\n"); indgen(sd)(Open, High, Low, Close, Volume) |> eval() |> print()  # 使用指标生成器(标准差统计)来计算指标分布形态
    cat("mad:\n"); indgen(mad)(Open, High, Low, Close, Volume) |> eval() |> print()  # 使用指标生成器(标准差统计)来计算指标分布形态
})