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
