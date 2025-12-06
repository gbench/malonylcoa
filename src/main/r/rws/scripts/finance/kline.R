library(quantmod)
library(jsonlite)

pvo_rules <- # Json 格式的规则定义
'[
  {"PChg":1,"VChg":1,"OChg":1,"Name":"多开+","Description":"价格上涨，成交量上涨，持仓量上涨"},
  {"PChg":0,"VChg":1,"OChg":1,"Name":"双开+","Description":"价格不变，成交量上涨，持仓量上涨"},
  {"PChg":-1,"VChg":1,"OChg":1,"Name":"空开+","Description":"价格下降，成交量上涨，持仓量上涨"},
  {"PChg":1,"VChg":0,"OChg":1,"Name":"多开0","Description":"价格上涨，成交量不变，持仓量上涨"},
  {"PChg":0,"VChg":0,"OChg":1,"Name":"双开0","Description":"价格不变，成交量不变，持仓量上涨"},
  {"PChg":-1,"VChg":0,"OChg":1,"Name":"空开0","Description":"价格下降，成交量不变，持仓量上涨"},
  {"PChg":1,"VChg":-1,"OChg":1,"Name":"多开-","Description":"价格上涨，成交量下降，持仓量上涨"},
  {"PChg":0,"VChg":-1,"OChg":1,"Name":"双开-","Description":"价格不变，成交量下降，持仓量上涨"},
  {"PChg":-1,"VChg":-1,"OChg":1,"Name":"空开-","Description":"价格下降，成交量下降，持仓量上涨"},
  {"PChg":1,"VChg":1,"OChg":0,"Name":"多换+","Description":"价格上涨，成交量上涨，持仓量不变"},
  {"PChg":0,"VChg":1,"OChg":0,"Name":"双换+","Description":"价格不变，成交量上涨，持仓量不变"},
  {"PChg":-1,"VChg":1,"OChg":0,"Name":"空换+","Description":"价格下降，成交量上涨，持仓量不变，一部分空方加仓，一部分空方平仓，空方加仓导致价格下跌"},
  {"PChg":1,"VChg":0,"OChg":0,"Name":"多换0","Description":"价格上涨，成交量不变，持仓量不变，一部分多方加仓，一部分多方平仓，多方加仓导致价格上涨"},
  {"PChg":0,"VChg":0,"OChg":0,"Name":"换手0","Description":"价格不变，成交量不变，持仓量不变"},
  {"PChg":-1,"VChg":0,"OChg":0,"Name":"多换0","Description":"价格下降，成交量不变，持仓量不变"},
  {"PChg":1,"VChg":-1,"OChg":0,"Name":"多换-","Description":"价格上涨，成交量下降，持仓量不变"},
  {"PChg":0,"VChg":-1,"OChg":0,"Name":"换手-","Description":"价格不变，成交量下降，持仓量不变"},
  {"PChg":-1,"VChg":-1,"OChg":0,"Name":"多换-","Description":"价格下降，成交量下降，持仓量不变"},
  {"PChg":1,"VChg":1,"OChg":-1,"Name":"空平+","Description":"价格上涨，成交量上涨，持仓量下降"},
  {"PChg":0,"VChg":1,"OChg":-1,"Name":"双平+","Description":"价格不变，成交量上涨，持仓量下降"},
  {"PChg":-1,"VChg":1,"OChg":-1,"Name":"多平+","Description":"价格下降，成交量上涨，持仓量下降"},
  {"PChg":1,"VChg":0,"OChg":-1,"Name":"空平0","Description":"价格上涨，成交量不变，持仓量下降"},
  {"PChg":0,"VChg":0,"OChg":-1,"Name":"双平0","Description":"价格不变，成交量不变，持仓量下降"},
  {"PChg":-1,"VChg":0,"OChg":-1,"Name":"多平0","Description":"价格下降，成交量不变，持仓量下降"},
  {"PChg":1,"VChg":-1,"OChg":-1,"Name":"空平-","Description":"价格上涨，成交量下降，持仓量下降"},
  {"PChg":0,"VChg":-1,"OChg":-1,"Name":"双平-","Description":"价格不变，成交量下降，持仓量下降"},
  {"PChg":-1,"VChg":-1,"OChg":-1,"Name":"多平-","Description":"价格下降，成交量下降，持仓量下降"}
]' |> fromJSON() # 转换成data.frame

#' 标识辨析
#' @param PChg 价格变动
#' @param VChg 交易量变动
#' @param OChg 持仓量变动
#' @param pvo_rules 变动解释规则
#' @return 变动的的意义解释
identify <- function(PChg, VChg, OChg, pvo_rules=pvo_rules) {
  if(c(PChg,VChg,OChg) |> sapply(is.na) |> any()){ # 存在无效数据的情况
    list(PChg=PChg, VChg=VChg, OChg=VChg, Name="无效", Description="无效") # 起始数据无效
  } else {# 数据有效
    pvo_rules[ pvo_rules$PChg==sign(PChg) & pvo_rules$VChg==sign(VChg) & pvo_rules$OChg==sign(OChg), ,drop=T ] # 检索相应位置条目
  }
} # identify

#' 交易解析
#' @param tdfm xts 结构的 Open,High,Low,Close,Volume
#' @return 交易解析
tanalyze <- function(tdfm) {
  tdfm |> mutate (
    PChg = c(NA, diff(LastPrice)),
    VChg = c(NA, diff(Volume)),
    OChg = c(NA, diff(OpenInterest)),
    Name = pmap_chr(list(PChg, VChg, OChg), ~ identify (..1, ..2, ..3, pvo_rules)$Name),
    Description = pmap_chr(list(PChg, VChg, OChg), ~ identify (..1, ..2, ..3, pvo_rules)$Description)
  ) |> select(Id, UpdateTime, LastPrice, PChg, VChg, OChg, Name, Description) |> arrange(-Id)
}

#' 计算K线
#' 将tickdata的dataframe  即 tdfm 转换成分钟线 
#' @param tdfm tickdata 的数据
#' @param tdate tickdata 所在的交易日期
#' @return xts 结构的 Open,High,Low,Close,Volume
compute_kline <- function(tdfm, tdate=Sys.Date()) { # 计算K线
  # Step 1: 创建 TickData  (依据更新时间进行数据排序）
  tickdata <- tdfm |> mutate(Period=substr(UpdateTime, 1, 5)) |> arrange(UpdateTime, UpdateMillisec) # 交易数据
  # Step 2: 创建 KlineData 
  pattern <- "%Y-%m-%d %H:%M" # 时间模式
  klinedata <- tickdata |> group_by(Period) |> mutate( # OHLC 数据生成
    Period=strptime(sprintf('%s %s', tdate, Period), pattern), # 日期时间
    Open=first(LastPrice), High=max(LastPrice), Low=min(LastPrice), Close=last(LastPrice), 
    ranking=row_number(desc(Id))  # 确保最后成交的tickdata排序为1，以便作为分钟线进行提取
  ) |> ungroup() |> dplyr::filter(ranking==1)  # 选择每个周期的最后一行作为kline数据数据线
  # Step 3: 计算成交量和开仓量的变化 
  kd <- klinedata |> mutate( # 计算分钟成交量和分钟持仓量变化
    Vol1m=Volume-dplyr::lag(Volume, default=Volume[1]), # 成交量-分钟增量
    OpenInt1m=OpenInterest-dplyr::lag(OpenInterest, default=OpenInterest[1]) # 持仓量-分钟增量
  ) # 带有OHLCV数据klinedata 
  # Step4: 包装成Period,OHLCV,OI的格式
  kd |> select(Period, Open, High, Low, Close, Volume=Vol1m, OpenInt1m, AccVol=Volume, AccOpenInt=OpenInterest) |> # 返回K线结果
    with(xts(order.by=Period, data.frame(Open,High,Low,Close,Volume)))
} # compute_kline 

#' 交易数据母函数
#' @param symbol 证券代码
#' @param date 日期
#' @param tbl 数据表名
#' @param n 返回数量数量
tickdata.genfun <- function(symbol=ma505, date=Sys.Date(), tbl=paste0('t_', symbol, '_', strftime(date, '%Y%m%d')), n=20){
  symbol <- substitute(symbol) |> deparse()
  sql <- sprintf('select * from %s where Id > (select max(Id)-%s from %s )', tbl, n, tbl)
  \(f) f( sql )
}

#' 远程交易数据,192.168.1.10
tickdata.h10ctp2 <- function(...) tickdata.genfun(...) (sqlquery.h10ctp2)

#' 本地交易数据:localhost
tickdata.lhctp2 <- function(...) tickdata.genfun(...) (sqlquery)

#' 获取ohlc数据
#' @param tbl 数据表
#' @param startime 开始时间
#' @param endtime 结束时间
#' @param keys 提取键名（负数索引表示剔除的索引）, 0, NA, NULL 表示提取所有！
ohlc <- \(tbl=NA, startime=NA, endtime=NA, keys=-c(1, 2, 3, 8)) {
  .tbl_expr <- substitute(tbl) # tbl表达式,必须在promise进行eval(tbl)求值之前进行获取，否则promise计算之后，表达式就获取不到了！
  .tbl <- tryCatch(eval(tbl), error=\(e) .tbl_expr)
  
  .startime_expr <- substitute(startime)
  .startime <- tryCatch(eval(startime), error=\(e) .startime_expr)

  .endtime_expr <- substitute(endtime)
  .endtime <- tryCatch(eval(endtime), error=\(e) .endtime_expr)

  .keys <- \(.) if(is.null(keys) ||  all(is.na(keys)) || (length(keys)==1 && keys==0)) seq(ncol(.)) else keys # 过滤key名列表
  params <- do.call(rbx.tse, args=list(.tbl, .startime, .endtime)) # 片姐参数
  sqldframe(OHLCV1M, params) %>% with(xts(.[, .keys(.)], order.by=as.POSIXct(paste(Date, Time)))) 
}

#' KDJ指标计算（对标TTR包风格）
#' 
#' 计算随机指标KDJ（随机摆动指标），兼容xts/zoo对象，输出K、D、J三线，
#' 逻辑对齐传统通达信/同花顺算法，参数可配置。
#' 
#' @param x 价格序列：可以是xts/zoo对象（需包含High/Low/Close列），或数值矩阵/数据框（第1列=最高价，第2列=最低价，第3列=收盘价）
#' @param n KDJ周期（通常9），对应RSV的计算周期
#' @param nK K值平滑周期（通常3），EMA平滑窗口
#' @param nD D值平滑周期（通常3），EMA平滑窗口
#' @param maType 平滑方法（默认EMA，兼容TTR包的maType参数风格）
#' @param ... 传递给maType的额外参数
#' @return 与输入同类型的对象（xts/zoo/矩阵/数据框），包含K、D、J三列
#' @examples
#' # 示例1：用TTR包的样例数据测试
#' library(TTR)
#' data(ttrc)
#' kdj_result <- KDJ(ttrc[,c("High","Low","Close")], n=9, nK=3, nD=3)
#' head(kdj_result)
#' 
#' # 示例2：纯数值向量测试
#' high <- c(10,12,11,13,14,15)
#' low <- c(8,9,8,10,11,12)
#' close <- c(9,11,10,12,13,14)
#' price_df <- data.frame(High=high, Low=low, Close=close)
#' kdj_result2 <- KDJ(price_df)
#' 
#' # 示例3：纯数值向量测试
#' ohlc(rb2605) |> KDJ(9,3,3) |>  plot()
#' @export
KDJ <- function(x, n = 9, nK = 3, nD = 3, maType, ...) {
  # ------------- 第一步：参数校验（对标TTR包的严谨性） -------------
  if (missing(x)) stop("必须提供价格序列x（High/Low/Close）")
  # 统一输入格式：提取High/Low/Close
  if (inherits(x, c("xts", "zoo"))) {
    # 兼容xts/zoo对象（TTR包核心数据类型）
    high <- x[, grep("High", colnames(x), ignore.case = TRUE)]
    low <- x[, grep("Low", colnames(x), ignore.case = TRUE)]
    close <- x[, grep("Close", colnames(x), ignore.case = TRUE)]
  } else if (is.matrix(x) || is.data.frame(x)) {
    # 矩阵/数据框：按列顺序取High(1)、Low(2)、Close(3)
    if (ncol(x) < 3) stop("矩阵/数据框需至少3列：High/Low/Close")
    high <- x[, 1]
    low <- x[, 2]
    close <- x[, 3]
  } else {
    stop("x必须是xts/zoo/矩阵/数据框类型")
  }
  # 转换为数值向量（避免类型错误）
  high <- as.vector(high)
  low <- as.vector(low)
  close <- as.vector(close)
  
  # ------------- 第二步：核心计算（传统KDJ算法） -------------
  # 1. 计算未成熟随机值RSV (Raw Stochastic Value)
  # RSV = (当前收盘价 - n周期内最低价) / (n周期内最高价 - n周期内最低价) * 100
  low_n <- TTR::runMin(low, n = n)  # n周期最低价（TTR包滑动最小值）
  high_n <- TTR::runMax(high, n = n) # n周期最高价（TTR包滑动最大值）
  rsv <- (close - low_n) / (high_n - low_n) * 100
  # 处理分母为0的情况（避免Inf/NaN）
  rsv[is.na(rsv) | is.infinite(rsv)] <- 50
  
  # 2. 计算K值（RSV的EMA平滑）
  if (missing(maType)) {
    # 默认用EMA平滑（对标TTR包默认风格）
    maType <- TTR::EMA
  } else {
    # 兼容自定义平滑函数（TTR包核心特性）
    maType <- match.fun(maType)
  }
  K <- maType(rsv, n = nK, ...)
  
  # 3. 计算D值（K值的EMA平滑）
  D <- maType(K, n = nD, ...)
  
  # 4. 计算J值（J = 3*K - 2*D，传统公式）
  J <- 3 * K - 2 * D
  
  # ------------- 第三步：结果整理（对标TTR包输出风格） -------------
  result <- cbind(K, D, J)
  colnames(result) <- c("K", "D", "J")
  
  # 还原输入类型（xts/zoo保持原有索引）
  if (inherits(x, c("xts", "zoo"))) {
    result <- xts::xts(result, order.by = index(x))
  } else if (is.data.frame(x)) {
    result <- as.data.frame(result)
  }
  
  return(result)
}

#' 计算交易时段验证的生成器, "trading breaks" : tbs
#' @param tbs 交易分点trading breaks序列
#' @return 交易时段验证函数
is.trading.gen <- \(tbs="09:00,10:15;10:30,11:30;13:30,15:00;21:00,23:00") {
    breaks <- tbs |> strsplit("[,;]+") |> unlist() |> lubridate::hm() # 生成时段分点
    #' 时间整理函数(剔除秒数,注意23:60==24:00，它们都是表示24个小时，这就像0.9999...==1是一个道理，不同记法而已！
    #' 对于时长(Timespan)，这里使用period而非duration是因为period更接近日常时间的时分秒现象，而非duration的累计单位时长的容量的属性！
    tidy <- \(x) sapply(as.character(x), USE.NAMES = FALSE,
        \(t) regexpr("(\\d{1,2}:\\d{1,2})(?=:\\d{1,2})?", t, perl = TRUE) |> (\(m) if (is.na(m) || m == -1) NA_character_ else regmatches(t, m)) ())
    
    #' 判断时点x是否位于交易时段之内
    #' 注意，时点x这里采用的是时长period结构来描述，period是特定时刻是与基准时刻"00:00"之间时长跨度
    #' 如此，记法上"23:60==24:00"是时长等价的，同理hm("23:60")==hms("23:59:60")、hms("23:59:60")==ddays(1)！
    #' @param x 时点
    \(x) {
        # 统一转成 period（时长）
        .x <-  if (lubridate::is.period(x)) x
            else if (inherits(x, c("POSIXct", "POSIXlt"))) lubridate::hm(strftime(x, "%H:%M"))
            else lubridate::hm(tidy(as.character(x)))

        # 向量化判断
        idx <- findInterval(.x, breaks)

        # 返回结果
        (idx %% 2 != 0) & !is.na(.x)
    }
}

#' 判断时点x是否位于交易时段之内
#' 注意，时点x这里采用的是时长period结构来描述，period是特定时刻是与基准时刻"00:00"之间时长跨度
#' 如此，记法上"23:60==24:00"是时长等价的，同理hm("23:60")==hms("23:59:60")、hms("23:59:60")==ddays(1)！
#' 测试示例:
#' c("08:60", "09:10", "10:20", "12:00", "21:30", "23:60", "25:00", NA) |> is.trading()
#' (Sys.time()+(1:5)*60*60) |> is.trading() # 当前时间的结构判断
#' @param x 时点 
is.trading <- is.trading.gen()

# 使用符号变量 
# symbol <- "rb2605"; ohlc(symbol, 2100, 2300, keys=0)
# 提取ohlc数据：使用符号
# ohlc(rb2601, 2100, 2300, keys=1:8)
# K线图 1min
# library(quantmod);ohlc(rb2601, 2100, 2300, ss("Open,High,Low,Close,Volume")) |> chartSeries()
# K线图 10min
# library(quantmod);ohlc(rb2601, 2100, 2300, ss("Open,High,Low,Close,Volume")) |> to.minutes10() |> chartSeries()
# K线图 11min(自定义)
# library(quantmod);ohlc(rb2601, 2100, 2300, ss("Open,High,Low,Close,Volume")) |> to.minutes(11) |> chartSeries()
# 使用with+environment()结构构造的运算执行环境来打包生成时间序列：难受的就是 x不支持直接的环境参数需要：as.list() |> as.data.frame()过渡一下！
# ohlcv1m |> sqldframe(rbx.tse(rb2601, .0900, .1200)) |> with(environment() |> as.list() |> as.data.frame() |> xts(order.by=as.POSIXct(paste(date,time))))
# 添加移动均线的绘图
# ohlc(rb2601, startime=.0900, endtime=.1200, keys=4:8) |> chartSeries(theme="white"); addSMA(c(1, 3, 5, 10, 10, 20, 30, 60))
# 指定合约&添加移动均线的绘图(在options中设定合约代码，ohlc中就可以省略合约代码，这样的定默认合约代码是可以跨越ohlc的!)
# options("sqlquery.rb.instrument"="rb2601");ohlc(startime=.0900, endtime=.1200, keys=4:8) |> chartSeries(theme="white"); addSMA(c(1, 3, 5, 10, 10, 20, 30, 60))
# 默认合约&添加移动均线的绘图(从options中清除合约代码)
# options("sqlquery.rb.instrument"=NULL);ohlc(startime=.0900, endtime=.1200, keys=4:8) |> chartSeries(theme="white"); addSMA(c(1, 3, 5, 10, 10, 20, 30, 60))
# 单合约的多周期式金融数据的处理
# ohlc(rb2605,.00,.23) |> #读取指定时间范围的金融合约交易的OHLC数据
#   with(xts(cbind(Open, High, Low, Close), index(Close))) |> # 提取ohlc数据并组装成xts格式数据
#   list(periods=c(1, 5, 10), handler=\(i, xs) to.minutes(xs, i) |> KDJ(), xs=_) |> # 组织成本地计算的资源环境包
#   with(lapply(periods, handler, xs=xs) |> setNames(paste0(periods, "min"))) # 开始多周期计算
