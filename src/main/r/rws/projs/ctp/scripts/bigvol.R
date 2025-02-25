# ----------------------------------------------------------------------
# 提取 交易数据中的 大成交量 项目（包含时刻）
# ----------------------------------------------------------------------
#' 一个将输入字符串内容转换成POSIXct日期格式的函数
as.datetime <- partial(as.POSIXct, format="%H:%M:%S") # 时间分析
# K线数据的提取
kdata <- if (F) { # 使用文件数据还是数据库数据
      # 当前工作区中的数据文件（以保证返回的文件路径都是相对于当前工作区的路径，而不是一个`不可直接访问`的简单字符串）
      data.files <- list.files(path=".", all.files=T, recursive=T, include.dirs=F); data.files # 数据文件集合
      # 读取指定数据(data.files的最后一项即最新的数据文件)
      fread(last(data.files)) # 数据文件读取
    } else { # 读取数据库数据
      "t_rb2505_20250225" |> sprintf(fmt="select * from %s")|> sqlquery.h10ctp2()   
  } |> compute_kline() |> (\(x, kd=x['T21:00/T23:00'], ix=index(kd))  # 提取指定时间段内的数据&并清除噪音数据
      kd[ # 小时跨度的时间跨度过滤 
          !( ix>as.datetime("15:00:00") & ix<as.datetime("21:00:00")) |
          !( ix>as.datetime("11:30:00") & ix<as.datetime("13:30:00")) |
          !( ix>as.datetime("10:15:00") & ix<as.datetime("10:30:00"))
      ]) (); kdata# 精选时段数据 

# ----------------------------------------------------------------------------------------------------------------------------------

# 提取交易数据（xts格式) 
vol <- kdata$Volume # 获取成交量数据
x <- as.numeric(vol) # 转换成向量

# 计算经验累积分布函数
empirical_cdf <-  x |> ecdf() # 结算经验分布函数

# 大单交易
bigvols <- vol[empirical_cdf(as.numeric(x)) > 0.95] # 获得大单交易量
print(bigvols) # 显示大胆交易交易数量

bigvols2 <- vol[x > quantile(x, 0.95)]
# 百分位数与经验分布计算的数据时一样
bigvols2 == bigvols

# 查看大成交量项出现的时间间隔
index(bigvols) |> diff()
