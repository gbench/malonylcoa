# 设定家目录
home <- "F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/quantmod/kdj/"
setwd(home)
list.files("indicators",full.names = T) |> lapply(source) # 加载指标文件

# 批量加载
batch_load()

# kdj 的绘图
plot_kdj<- \(data) ggplot(data, aes(x=1:length(DateTime))) +
  geom_line(aes(y=K), color="red") +
  geom_line(aes(y=D), color="green") +
  geom_line(aes(y=J), color="blue")

# 即兴sqlquery
sqlquery.adhoc <- \(...) sqlquery(host = "localhost", dbname = "ctp4", ...)
# 查看数据表
sqlquery.adhoc("show tables")

# 指定数据表
tbl <- "t_rb2510_20250425" # 交易数据表
tickdata <- tbl |> sprintf(fmt="select * from %s") |> sqlquery.adhoc() # 交易数据
range_data <- tickdata$UpdateTime|>range(); range_data

ohlcv1m <- calculate_ohlcv(tickdata);ohlcv1m # 1m K线
kdj1m <-calculate_kdj(ohlcv1m);kdj1m # 计算1m kdj

ohlcv5m <- aggregate_ohlcv(ohlcv1m, "5 mins");ohlcv5m # 5m k线
kdj5m <-calculate_kdj(ohlcv5m);kdj5m # 计算5m kdj

ohlcv15m <- aggregate_ohlcv(ohlcv1m, "15 mins");ohlcv15m # 5m k线
kdj15m <-calculate_kdj(ohlcv15m);kdj15m # 计算15m kdj

# kdj 绘图
plot_kdj(kdj1m)
plot_kdj(kdj5m)
plot_kdj(kdj15m)

# 识别kdj金叉与死叉
kdj_data <- identify_kdj_cross(kdj15m);kdj_data

# ----------------------------------------------------------
# 增加过滤条件：
# 只识别超卖区的金叉（K/D < 20）
identify_kdj_cross(kdj_data) %>% filter(cross_type == "Golden" & K < 20)

# 只识别成交量放大的金叉
kdj_data %>% identify_kdj_cross() %>% 
  left_join(kdj_data[,c("Volume", "DateTime")], by = "DateTime") %>%  
  filter(Volume > SMA(Volume, 10))

# 多周期确认：
identify_kdj_cross(kdj_data, confirm_bars = 2)

