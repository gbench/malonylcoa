batch_load()

# 定义数据库函数
sqlquery.adhoc <- \(...) sqlquery(..., dbname = "ctp", port = 3372) # 自定义函数
getdata <- \(tbl) tbl |> sprintf(fmt = " -- 数据查询的SQL语句
    SELECT
      concat(TradingDay, ' ' ,UpdateTime, '.', UpdateMillisec) Time, -- 拼接成交易时间
      LastPrice, -- 成交价格
      AveragePrice, -- 平均价格
      OpenInterest, -- 开仓量
      PreOpenInterest -- 前日开仓量
    FROM %s ORDER BY Id DESC -- 按照插入数据库的序号进行倒序排列，最新数据位于前头
  ") |> sqlquery.adhoc() |> transform(Time = as.POSIXct(Time, format = "%Y%m%d %H:%M:%OS")) %>% 
  with(xts(.[, -1], order.by = Time))

# 指定时间范围
curdate <- Sys.Date()
period <- paste(curdate, c("09:00", "15:00"), collapse = "/") # 指定数据区间范围
tickdata <- paste0("t_ma509_", curdate |> gsub(pat = "-", rep = "")) |> getdata()
tickdata <- tickdata[period, ]
volume <- tickdata[, "OpenInterest"] |> diff() # 提取交易量
tickdata <- tickdata |> cbind(Volume = as.numeric(volume)) # 指定Volume列名, 注意需要as.numberic转成向量

# 统计开平仓数量
volume |> tapply(sign(volume), sum) 
plot(volume)

# 计算 OpenInterest 的缩放比例（根据需要调整）
scale_factor <- with(tickdata, max(LastPrice) / max(OpenInterest))
n <- 30 # 分钟间隔
breaks <- endpoints(tickdata, on = "mins", n) |> (\(x) c(1, x[-1]))() # 提取n min分隔点
labels <- index(tickdata)[breaks] |> strftime(format="%H:%M")
ggplot(tickdata, aes(x = 1:nrow(tickdata))) + 
  geom_line(aes(y = LastPrice, color = "LastPrice"), linewidth = 1) +
  geom_line(aes(y = OpenInterest * scale_factor, color = "OpenInterest"), linewidth = 1) + 
  scale_y_continuous(name = "Price", sec.axis = sec_axis(~ ./scale_factor, name = "OpenInterest")) + # 右侧坐标轴
  scale_x_continuous(breaks = breaks, labels = labels) + # 自定义标签
  scale_color_manual(values = c(LastPrice = "red", OpenInterest = "blue")) + 
  labs(x = "时间", y = "收盘价格", title = "价格持仓量") +  # 坐标轴名称
  theme_minimal()
