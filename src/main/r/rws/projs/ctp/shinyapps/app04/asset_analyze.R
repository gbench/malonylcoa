# 我们来一起分析一种观点:
# 资产的本质是一种收益率载体（收益率容器），它在特定时空里应该是一种确定的恒定值: r = p[t]/p[t-1]，
# r就是资产总体的期望，它不会轻易改变,因为人们是有计划的去开发和控制&利用资产！
# 所以，资产收益率应该是符合正态分布的（有意识目的确定&平衡某种最优）。资产价格其实是收益率的计算工具！
# 价格波动是形式，而表象收益率才是本质。因此，当我们按照特定的收益率核算周期来采样价格，测量出的收益率，
# 应该是符合正态分布的。
#
# 一种带有固定的收益率的资产，其资产价格vs时间（以相应的收益率周期为单位的数值度量）的在曲线上的表示就是一条趋势线！
# 于是这种趋势一旦确定，就可以理解为，它是资产稳定的表现。
# 由此，资产稳定有三种状态，正收益率（上行：多头资产），负收益率（下跌：空头资产），0收益率（震荡）。
# 所以，证券投资的趋势的核心本质就是确定收益率的符号模式。进而确认收益率的分布参数。
# 以及，选择相应的显著程度区间来承担风险！

# 我们把它实现出来（用特定细节化来应用它）
# 首先我们需要定义一个收益率周期（比如1分钟，5min,1h, 1d,1month) 等。
period<- "1min"

# ------------------------------------------------------------------------------
# 定义&获取的价格数据
# 建立资产行为模型
# ------------------------------------------------------------------------------

# 批量加载工具函数（包括sqlquery的期货交易数据的读取函数）
batch_load()

# 定义&获取的价格数据
#' 获取分组数据
#' 
#' @param ratio 分组比率
#' @param instrument 合约工具
#' @param dbname 数据库名称
#' @param host 数据库主机
#' @param begtime 开始时间
#' @param endtime 结束时间
tickdata <- \(ratio=c(0.3, 0.5), instrument="t_rb2601_20251124", dbname="ctp", 
              host="127.0.0.1", begtime="09:00", endtime="12:00") {
  
  .ratio <- ratio |> sort() |> (\(x) ifelse(x>=1 | x<=0, NA, x))() |> na.omit()    # 占比排序
  # 数据分组的标签生成
  lbls <- .ratio |> append(values=_, x=c(0, 1), after=1) |> (\(xt, x0=lag(xt)) cbind(x0, xt)[-1, ])()|> 
    apply(1, paste, collapse=", ") |> sprintf(fmt="(%s]") # 绘制区间式样
  
  # 获取价格数据，Id:数据主键，LastPrice：最新成交价格，Volume：当日累计成交量，Vol:期间成交量，UpdateTime：hms解构数据更新时间
  "select Id, LastPrice, Volume, Volume-lag(Volume) over() Vol, UpdateTime from %s where UpdateTime between '%s' and '%s'" |>
    sprintf(fmt=_, instrument, begtime, endtime) |> # 凭借成有效的SQL
    sqlquery(dbname=dbname, host=host) |> with({ # 依据Id(数据主键)进行分组处理
    Id |> (\(x) range(x) |> append(quantile(x, .ratio), 1))() |> 
      cut(Id, breaks=_, include.lowest=T, labels=lbls) |> 
      split(price_data, f=_) # 区间分组
  });
  
}; 

# 数据分组, 一半用于分析：一半用于测试，
parts <- tickdata(c(0.5));

# 训练数据
training_data <- parts[[1]]

# 测试数据
test_data <- parts[[2]]

