## 将一下文本改写成单文件的 shinyApp
## batch_load: 用于导入sqldframe，record.builder 等基础函数
## 请把##tbl,#startime,#endtime作为界面输入控件以便方便输入
## 其中#endtime 默认位当前时间，#startime 默认为#endtime - 2h
## 增加一个时长滑杆用于调整#endtime与#endtime之间的间隔时长，最小单位时5min, 最大范围是600
## 为#startime,#endtime增加一个选择option用户表示当前是否选中。即焦点时点。
## 增加一个重置时间按钮，用于将焦点时点重置为当前时间，一旦重置时间后，根据时间滑杆去调整对应的#startime,#endtime
## 增加host,port的input控件，host 有两种模式 文本框和select下拉列表，可以通过checkbox进行切换，
##  select选项有 {10服务器:192.168.1.10;04服务器:192.168.1.4;本地主机:127.0.01},port 默认值3371
## 增加手动刷新与自动刷新选择radio，默认是手动刷新，点选自动刷新后，调节时间是reactive的重新绘制图表
## 增加内容tab：k线图和K线数据，k线图 添加移动平均值(3,5,10,15,30,60)可以通过多选框进行复选
## 内容tab默认就是下图示例加权平均
## 增加刷新间隔设置输入框（单位秒），默认为5S 

# 导入基础函数
batch_load()

ds10ctp <- partial(sqldframe,dbname = "ctp", host = "192.168.1.10", port=3371)

f<-\() ds10ctp("select * from t_rb2601_20251119 where UpdateTime<'09:16'")

f()|> mutate(W=diff(Volume)|>append(0,0)) |> select(LastPrice,W) |> with(weighted.mean(LastPrice,W))

rb <- record.builder("##tbl,#startime,#endtime")


# 绘图
rb('t_ma601_20251119','10:15','10:41') |> 
  ds10ctp(params=_, "1min.keline.weighted", files="sql") |>
  ggplot(aes(MinuteTime|>substr(4,5))) +  # 原始x轴仍用MinuteTime（用于线条和标签）
  geom_line(aes(y = ClosePrice, group = 1)) +
  geom_line(aes(y = WeightedAvgPrice, group = 2), color = "red") +
  geom_line(aes(y = HighPrice, group = 1),color="purple") +
  geom_line(aes(y = LowPrice, group = 1),color="navy") +
  # 在geom_smooth内部转换x为POSIXct（带日期）
  geom_smooth(aes(seq(MinuteTime),WeightedAvgPrice),color="blue")
