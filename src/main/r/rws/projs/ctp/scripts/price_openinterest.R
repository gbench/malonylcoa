batch_load()

# 定义数据库函数
sqlquery.adhoc <- \(...) sqlquery(..., dbname = "ctp", port = 3372) # 自定义函数
getdata <- \(tbl) tbl |> sprintf(fmt = " -- 数据查询的SQL语句
    SELECT
      concat(TradingDay, ' ' ,UpdateTime, '.', UpdateMillisec) Time, -- 拼接成交易时间
      LastPrice, -- 成交价格
      AveragePrice, -- 平均价格
      Volume, -- 累计成交量
      OpenInterest, -- 开仓量
      PreOpenInterest -- 前日开仓量
    FROM %s ORDER BY Id DESC -- 按照插入数据库的序号进行倒序排列，最新数据位于前头
  ") |> sqlquery.adhoc() |> transform(Time = as.POSIXct(Time, format = "%Y%m%d %H:%M:%OS")) %>% 
  with(xts(.[, -1], order.by = Time))

# 自定义参数
curdate <- Sys.Date() # 当前时间.例如：2025-05-23
contract <- "rb2510" # 期货合约, 候选项目：ma509,v2509,i2509,c2507
times <- c("13:30", "15:00") # 时间范围: 09:00/11:30;13:30/15:00;21:00/23:00, selected=13:30/15:00
k <- 30 # 时间间隔数量，number of periods each endpoint should cover.
on <- "mins" # 候选项目 us,ms,secs,mins,hours,days,weeks,months,quarters,years

# 获取&转换交易数据
tickdata <- paste0("t_", contract, "_", curdate |> gsub(pattern = "-", replace = "")) |> 
  getdata() |> transform(volume=diff(Volume) |> as.vector()) # 提取交易数据并计算分笔成交量
period <- paste(curdate, times, collapse = "/") # 指定交易时段的时间范围
tickdata <- tickdata[period, ] # 提取指定交易时段范围中的数据

# 统计开平仓数量
tickdata |> with(OpenInterest |> as.numeric() |> diff() %>% tapply(sign(.), sum)) |> barplot()
breaks <- endpoints(tickdata, on = "mins", k) |> (\(x) c(1, x[-1]))() # 提取min级别的分隔点
labels <- index(tickdata)[breaks] |> strftime(format="%H:%M") # 时间格式化
scale_factor <- with(tickdata, max(LastPrice) / max(OpenInterest)) # 计算 OpenInterest 的缩放比例（根据需要调整）

# 分时图
ggplot(tickdata, aes(x = 1:nrow(tickdata))) +
  geom_line(aes(y = LastPrice, color = "LastPrice"), linewidth = 1) +
  geom_line(aes(y = OpenInterest * scale_factor, color = "OpenInterest"), linewidth = 1) +
  scale_y_continuous(name = "Price", sec.axis = sec_axis(~ ./scale_factor, name = "OpenInterest")) + # 右侧坐标轴
  scale_x_continuous(breaks = breaks, labels = labels) + # 自定义标签
  scale_color_manual(values = c(LastPrice = "red", OpenInterest = "blue")) +
  labs(x = "时间", y = "收盘价格", title = "价格持仓量") + # 坐标轴名称
  theme_minimal()
