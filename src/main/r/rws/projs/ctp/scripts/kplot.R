# ------------------------------------------------------
# 数据准备&绘图
# ------------------------------------------------------

library(data.table) # 导入高效率的适合大数据读写的fread与fwrite函数

# ****************************************
#  工具函数定义
# ****************************************

#' 使用指定分隔符delim 将指定字符串line个分割处数据片段向量
#' @param line 文件字符串
#' @param delim 分隔符
#' @retun 分割后的数据向量
partition <- \(line, delim=",") line |> strsplit(delim) |> unlist() # 切分

#' 一个将输入字符串内容转换成POSIXct日期格式的函数
as.datetime <- partial(as.POSIXct, format="%H:%M:%S") # 时间分析

#' 绘制单日的kdata
#' @param kdata K线数据, xts 的数据格式, 包括 Open,High,Low,Close,Volume的数据
#' @param interval 时间间隔字符串， 默认为 "15min"
#' @param sessions 交易时段字符串, 默认为 "09:00:00,10:15:00;10:30:00,11:30:00;13:30:00,15:00:00;20:00:00,23:00:00"
#’ @return ggplot的绘图对象
kplot <- function (# 绘制K线图
    kdata, # K线数据
    interval="15 min", # 时间间隔
    sessions="09:00:00,10:15:00;10:30:00,11:30:00;13:30:00,15:00:00;21:00:00,23:00:00", # 交易时段字符串
    sessmx=strsplit(sessions, ";") |> sapply(strsplit, split=",") |> sapply(c), # 交易时段矩阵
    std.breaks=apply(sessmx, 2, \(sess, ps=as.datetime(sess)) seq(ps[1], ps[2], by=interval)) |> # 提取交易标记点
      Reduce(c, init=as.POSIXct(character(0)), x=_) # Reduce 需要为init指定由初始类型，如果提供默认c()则会返回long型数据
  ) { # 函数正文
  # 时间轴绘图
  ggplot( kdata |> as.data.frame() |> transform ( # 值列变换
      Id=seq_along(index(kdata)), # 追加时间索引
      Color=ifelse(Close > Open, "red", "green") # 数据颜色
    ), aes(Id) ) + # 基础数据
    # 图层添加：收盘价-linerange
    # geom_linerange(aes(ymin=Low, ymax=High, y=Open)) + # 收盘价
    # 图层添加：收盘价-boxplot KLINE图
    geom_boxplot( aes ( # K线参数设置
      group=Id, # boxplot 每个Id位置均为独立分组
      lower=pmin(Open, Close), # 最低价
      upper=pmax(Open, Close), # 最高价
      middle=(Open + Close) / 2, # 中位价
      ymin=Low, # 最低价
      ymax=High, # 最高价
      fill=Color # 颜色
    ), stat = "identity") + # K线图
    # 图层添加：收盘价-line
    geom_line(aes(y=Close), color="pink") + # 收盘
    geom_point(aes(y=Close), color="purple") + # 收盘
    scale_fill_manual(name="颜色", labels=c(red="上涨", green="下降"), 
      values=c(red="red2", green="green2")) + # 颜色设置
    scale_x_continuous( # 自定义时间轴
      breaks=\(x) { # 需要保持与ggplot的基础映射x的相同的数据类型
        print(sprintf("breaks:%s(%d)", x, length(x)))
        .p <- c("10:15:00", "11:30:00", "15:00:00") |> as.datetime() # 交易时段的结束时刻,由于连续交易,本时段的结束就是下一时段开始,故称其为时段连接点
        breaks <- std.breaks[-match(.p, std.breaks)] # 考虑持续交易,每个时段都存在有后继时段,为避免重复绘制需要把上一个时段的尾部时点给剔除掉。
        print(sprintf("breaks --> %s(%d)", breaks, length(breaks)))
        match(breaks, index(kdata)) |> na.omit() # 剔除NA值后的交易时点
      }, labels=\(x) { # 坐标id
        print(sprintf("labels:%s(%d)", x, length(x)))
        points <- index(kdata)[x] # 提取时间点数据的时间索引
        # 定义时段连接点:join points, （前一结束，后一开始的）交易时段的连接点
        # 把[a,b,c,d,e] 分组成[(b,c),(d,e)]的仅包含前一时段结束时间与后一时段的结束时间的连接点
        # 是连接点把物理不连续的时间片段给拼凑成一条逻辑连续的交易序列
        jps <- split(sessmx, floor(seq_along(sessmx)/2)) |> Filter(f=\(x) length(x)>1) # 构造连接点
        jp.labels <- jps |> sapply((\(x, ps=strsplit(x,":"), p1=ps[[1]], p2=ps[[2]])  
          ifelse(p1[1]==p2[1], sprintf("%s:%s/%s", p1[1], p1[2], p2[2]),   # 相同前缀
            ifelse(p1[[2]]==p2[[2]], sprintf("%s/%s:%s",p1[1], p2[1], p1[2]), # 相同中后缀
              F)))) # 连接点的格式文本
        is.jp <-\(points) points %in% sapply(jps,`[`,2) # 判断是否是连接点
        ix.jp <-\(points) match(points, sapply(jps,`[`,2)) # points在jps中的索引
        ps <- strftime(points, format="%H:%M:%S") # 时间点的格式化字符串
        lbls <- dplyr::case_when( # 时间刻度
          is.jp(ps) ~ jp.labels[ix.jp(ps) |> na.omit()], # 时段交界点
          minute(points) == 0 ~ format(points, "%H:00"), # 默认整点时刻
          TRUE ~ sprintf("%02d", minute(points)) # 默认非整点时刻
        ) # 时间刻度
	    print(sprintf("labels --> %s(%d)", lbls, length(lbls)))
        lbls # 时点标签
      }, limits=\(x) { # 刻度区间
        print(sprintf("limits:%s(%d)", x, length(x)))
        x # 坐标轴的刻度区间范围，目前采用默认不予调整
    }) + # 自定义时间轴
    labs(x="时间", y="收盘价格", title="商品K线价格图") + # 坐标轴名称
    theme_minimal() # 精简风格
} # kplot

# ****************************************
# 数据准备
# ****************************************

# 当前工作区中的数据文件（以保证返回的文件路径都是相对于当前工作区的路径，而不是一个不可直接访问的简单字符串）
data.files <- list.files(path=".", all.files=T, recursive=T, include.dirs=F); data.files # 数据文件集合
# 读取指定数据:data.files 的最后一项
data <- fread(last(data.files)) # 数据文件读取
# K线数据
klinedata <- data |> compute_kline() # 解析数据为ohlcv结构的K线数据
# 指定范围的K线数据（精选时段）
kdata <- klinedata["T21:00/T23:00"] |> (\(kd, ix=index(kd))  # 剔除指定时间段后的数据
  kd[ !( ix>as.datetime("15:00:00") & ix<as.datetime("21:00:00") ) ])(); kdata # 精选时段数据 

# ****************************************
# 数据绘图
# ****************************************

# 绘图
kdata |> kplot()
