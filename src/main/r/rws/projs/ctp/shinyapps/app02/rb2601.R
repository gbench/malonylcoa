# 将以下脚本改写成单文件的 Shiny App
#
# batch_load 是用于导入 sqldframe（模板 SQL 执行）、record.builder（模板参数容器构建器） 
# 等基础函数的运算环境初始化指令。
#
# 界面输入控件要求：
# - 将 ##tbl、#startime、#endtime 作为界面输入控件
# - #endtime 默认为当前时间
# - #startime 默认为 #endtime - 2h
#
# 时间控制功能：
# - 增加时长滑块，调整 #endtime 与 #startime 之间的间隔时长
#   - 单位：分钟
#   - 最小单位：5分钟
#   - 最大范围：600分钟
# - 为 #startime、#endtime 增加选择选项，标识是否选中（焦点时点所在位置）
# - 增加重置时间按钮，将焦点时点重置为当前时间
#   - 重置后可通过时间滑块调整对应的 #startime 或 #endtime
#
# 连接设置功能：
# - host 输入控件有两种模式：
#   - 文本框
#   - select 下拉列表（可通过复选框切换模式）
#   - select 选项：
#     - "10服务器" = "192.168.1.10" 
#     - "04服务器" = "192.168.1.4" 
#     - "本地主机" = "127.0.0.1" 【默认选择】
# - port 输入控件，默认值：3371
#
# 刷新功能：
# - 手动刷新与自动刷新选择（单选按钮）
# - 默认：手动刷新
# - 自动刷新设置：
#   - 点选自动刷新后，显示刷新间隔设置输入框
#   - 单位：秒，默认值：5秒
#   - 功能：在指定时间间隔内，不改变当前界面输入的情况下，刷新数据
#     （重新向数据源请求数据并绘制图表）
#   - 手动刷新：立即执行刷新行为
#
# 响应式特性：
# - 时间调节是响应式（reactive）的，会触发重新绘制图表动作
#
# 内容标签页：
# - 加权平均
# - K线图
# - K线数据
#
# K线图特性：
# - 添加移动平均值（3, 5, 10, 15, 30, 60）
# - 可通过多选框进行选择
#
# 页面标题：CTP 数据可视化分析平台
# 输入控件按照功能分布进行tab分组，美化界面，简洁大方!


# 导入基础函数
batch_load()

# 定义本地数据源
ds10ctp <- partial(sqldframe,dbname = "ctp", host = "192.168.1.10", port=3371) # ds数据源10主机ctp数据库
rb <- record.builder("##tbl,#startime,#endtime") # 表格-时间构建器
f <- \() ds10ctp("select * from t_rb2601_20251119 where UpdateTime<'09:16'") # 读取测试函数

# 读取实验
f()|> mutate(Wt=diff(Volume)|>append(0,0)) |> select(LastPrice,Wt) |> with(weighted.mean(LastPrice, Wt))

# 使用表格-时间参数填充本地文件夹"./sql" 中SQL模板"tickdata.1min.kline"并获取查询结果数据
rb('t_ma601_20251119','09:00','09:16') |> ds10ctp("tickdata.1min.kline", params=_, files="./sql")

# 使用表格-时间参数填充本地文件夹"./sql" 中SQL模板"1min.kline.weighted"并进行绘图
rb('t_ma601_20251119','10:15','10:41') |> 
  ds10ctp(params=_, "1min.kline.weighted", files="./sql") |>
  ggplot(aes(MinuteTime|>substr(4,5))) +  # 原始x轴仍用MinuteTime（用于线条和标签）
  geom_line(aes(y = ClosePrice, group = 1)) +
  geom_line(aes(y = WeightedAvgPrice, group = 2), color = "red") +
  geom_line(aes(y = HighPrice, group = 1),color="purple") +
  geom_line(aes(y = LowPrice, group = 1),color="navy") +
  # 在geom_smooth内部转换x为POSIXct（带日期）
  geom_smooth(aes(seq(MinuteTime),WeightedAvgPrice),color="blue")
