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
#' @return 分割后的数据向量
partition <- \(line, delim=",") line |> strsplit(delim) |> unlist() # 切分

#' 一个将输入字符串内容转换成POSIXct日期格式的函数
as.datetime <- partial(as.POSIXct, format="%H:%M:%S") # 时间分析

#' 绘制单日的kdata
#' @param kdata K线数据, xts 的数据格式, 包括由Open,High,Low,Close,Volume成员属性(variable)
#' @param interval 时间间隔字符串， 默认为 "15min"
#' @param sessions 交易时段字符串,时间段之间用';'分隔,时段内时点用','分隔。
#'        默认为 "09:00:00,10:15:00;10:30:00,11:30:00;13:30:00,15:00:00;20:00:00,23:00:00"
#' @return ggplot的绘图对象
kplot <- function (# 绘制K线图
    kdata, # K线数据
    interval="15 min", # 时间间隔
    sessions="09:00:00,10:15:00;10:30:00,11:30:00;13:30:00,15:00:00;21:00:00,23:00:00", # 交易时段字符串
    # -------------------------------------------------------------------------------------------------------------------------
    # Promise 变量定义区域。注意：以下参数是不需要传递的，之所以写在这里（形式参数位置），仅是为了Lazy Evaluation的编码技巧而已
    # -------------------------------------------------------------------------------------------------------------------------
    sessmx=strsplit(sessions, ";") |> sapply(strsplit, split=",") |> sapply(c), # 交易时段矩阵
    std.breaks=apply(sessmx, 2, \(sess, ps=as.datetime(sess)) seq(ps[1], ps[2], by=interval)) |> # 提取交易标记点
      Reduce(c, init=as.POSIXct(character(0)), x=_), # Reduce 需要为init指定由初始类型，如果提供默认c()则会返回long型数据。即避免类型檫除
    # 主要概念：
    # 交易时段：交易时段[a,b)是指从a（包含）时刻开始到b时刻（不包含）结束的一段交易过程。
    # 交易过程：由于交易存在中场休息或是停盘交易的情况，金融交易在实际请款中分段执行的，比如：[a,b), [c,d), [e, f), ... ,
    #           为了书写方便将该过程，简写为[a,b, c,d, e,f, ...)
    # 连接点(join points)：在交易过程中,`连接点`是指那些在相邻的时段之间充当着的承上启下的关键`时点`，它包括两个元素:
    #         前段交易的结束时刻previous_end, 后段交易的开始时刻current_start, 简记为(previous_end, current_start)
    #         物理意义上的previous_end与current_start一般是不同的，但是在逻辑上它们却是同一个交易时点。由此，就有了
    #         连接点的前端previous_end,与后端的current_start的说法。
    #         eg.对于一个多段交易过程（时点序列）：[a,b,c,d,e,f)，它的连接点集即jps的向量就可以写为：[(b,c), (d,e)]
    #         前一结束与后一开始在逻辑上是同一个时间的不同名称而已。逻辑上 b<=>c, d<==>e， 符号 <==> 表示等价
    #         是连接点把物理不连续的时间片段给拼凑成一条逻辑连续的交易序列.
    jps=split(sessmx, floor(seq_along(sessmx)/2)) |> Filter(f=\(x) length(x)>1), # 构造连接点:将彼此相邻的交易时段的前结尾与后开始的时点分成一组后给予提取
    jps.labels=jps |> sapply((\(jp, ps=strsplit(jp, ":"), p1=ps[[1]], p2=ps[[2]]) # jp连接点ps为其':'的模式分解,p1,p2为相应的前后端,其成员为时间字段:时分秒
      ifelse(p1[1]==p2[1], sprintf("%s:%s/%s", p1[1], p1[2], p2[2]),   # 相同前缀的情形
        ifelse(p1[[2]]==p2[[2]], sprintf("%s/%s:%s", p1[1], p2[1], p1[2]), # 相同中后缀情形
          NA)))), # 连接点的格式文本
    is.jp=\(points) points %in% sapply(jps, `[`, 2), # 以连接点的后端作为判断依据，返回点集合points各元素是否是连接点(由jps包含)的标志向量
    ix.jp=\(points) match(points, sapply(jps, `[`, 2)) # points在连接点向量jps中的索引
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
        pre.jps <- sapply(jps,`[`, 1) |> as.datetime() # 连接点的前端集合
        # 当考虑连续交易时，每个时段都存在&有其相应的后继时段,即`连接点`我们只能绘制一端，否则就会出现
        # 既要绘制上个交易的结束时刻又要绘制本次交易的开始时刻，而连续的就意味着前一结束就本次之开始。
        # 为避免两次绘制所导致的在视觉上的连续性破坏,本算法将采用，以连接点的后端合并前端的方式来编制
        # 刻度分位点集合的breaks，由此，这里就需要把pre.jps的前端（点）从std.breaks里给予剔除掉
        breaks <- std.breaks[-match(pre.jps, std.breaks)] # 从标准分点std.breaks中剔除掉pre.jps
        print(sprintf("breaks --> %s(%d)", breaks, length(breaks)))
        match(breaks, index(kdata)) |> na.omit() # 剔除NA值后的交易时点
      }, labels=\(x) { # 坐标id
        print(sprintf("labels:%s(%d)", x, length(x)))
        points <- index(kdata)[x] # 提取时间点数据的时间索引
        ps <- strftime(points, format="%H:%M:%S") # 时间点的格式化字符串
        lbls <- dplyr::case_when( # 时间刻度
          is.jp(ps) ~ jps.labels[ix.jp(ps)], # 时段交界点
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

# 当前工作区中的数据文件（以保证返回的文件路径都是相对于当前工作区的路径，而不是一个`不可直接访问`的简单字符串）
data.files <- list.files(path=".", all.files=T, recursive=T, include.dirs=F); data.files # 数据文件集合
# 读取指定数据(data.files的最后一项即最新的数据文件)
data <- fread(last(data.files)) # 数据文件读取
# K线数据
klinedata <- data |> compute_kline() # 解析数据为`ohlcv结构`的K线数据
# 指定范围的K线数据（精选时段）
kdata <- klinedata["T09:00/T23:00"] |> (\(kd, ix=index(kd))  # 剔除指定时间段后的数据
  kd[ !( ix>as.datetime("15:00:00") & ix<as.datetime("21:00:00") ) ])(); kdata # 精选时段数据, 剔除掉非交易时间出现的噪音数据 

# ****************************************
# 数据绘图
# ****************************************

# 绘图
kdata |> kplot()
