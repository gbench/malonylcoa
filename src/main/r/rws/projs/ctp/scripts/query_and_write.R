library(data.table)
library(tidyverse)

#' 创建目录
#' @param x 目录集合
mkdir <- \(x) x |> strsplit("[,/\\]+") |> structure(names=x) |> lapply( \(e) Reduce(x=e, \(acc, a) paste(acc, a, sep="/"), accumulate=T) |> lapply(dir.create) )

# ------------------------------------------------------
# 查询数据并写入文件
# ------------------------------------------------------

data.home <- "data/h10ctp2/"

#' 创建数据文件根目录
mkdir(data.home)

#'  读取二月份rb2505的数据
sqlquery.h10ctp2("show tables") |> unlist() |> # 从h10ctp2中读取数据
  grep(pat="rb2505_202502\\d{2}", value=T) |> # 读取指定名称模式的数据文件
  sprintf(fmt="select * from %s") |> 
  sqlquery.h10ctp2() %>%
  structure(., names=gsub(pat=".*\\st_(\\w+)$", "\\1", x=names(.))) |> ( \( xs, ns=names(xs) )  # 改名并存储
    lapply(ns, \(n) fwrite(getElement(xs, n), paste0(data.home, n, ".csv"))) )() # 存储到指定的data.home数据目录

# 查看data.home目录下的所有数据文件（不包括目录）
data.files <- list.files(path=data.home, all.files=T, recursive=T, include.dirs=F); data.files

# 当前工作区中的数据文件
data.files <- list.files(path=".", all.files=T, recursive=T, include.dirs=F); data.files

# 读取指定数据:data.files 的最后一项
data <- fread(last(data.files))

# ------------------------------------------------------
# 数据查看
# ------------------------------------------------------

#' 使用指定分隔符delim 将指定字符串line个分割处数据片段向量
#' @param line 文件字符串
#' @delim 分隔符
#' @retun 分割后的数据向量
partition <- \(line, delim=",") line |> strsplit(delim) |> unlist() # 切分

#' 一个将输入字符串内容转换成POSIXct日期格式的函数
as.datetime <- partial(as.POSIXct, format="%H:%M:%S") # 时间分析

# 间隔区间
interval <- "15 min" # 时间间隔
# 标准分割点
std.breaks <- "09:00:00,10:15:00;10:30:00,11:30:00;13:30:00,15:00:00;20:00:00,23:00:00" |> # 坐标刻度，标准时间刻度
  partition(delim=";") |> # 解析交易时段
  lapply(\(line, ps=compose(as.datetime, partition)(line), .interval=interval) seq(ps[1], ps[2], by=.interval)) |> # 提取交易标记点
  Reduce(c, init=as.POSIXct(character(0)), x=_) # Reduce 需要为init指定由初始类型，如果提供默认c()则会返回long型数据
# K线数据
klinedata <- data |> compute_kline()
# 指定范围的K线数据
kdata <- klinedata["T09:00/T23:00"] |> (\(d, idx=index(d)) { # 剔除指定时间端的数据
  d[ ! (idx>as.datetime("15:00:00") & idx<as.datetime("21:00:00")) ]
})() #  数据范围

# 时间轴绘图
ggplot(kdata, aes(seq_along(index(kdata)))) + # 基础数据
  geom_line(aes(y=Close)) + # 收盘价
  scale_x_continuous( # 自定义时间轴
    breaks=\(x) { # 需要保持与ggplot的基础映射x的相同的数据类型
      print(sprintf("breaks:%s(%d)", x, length(x)))
      tp <- c("10:15:00", "11:30:00", "15:00:00") |> as.datetime() # 连接时间点，交易时段的结尾时刻
      breaks <- std.breaks[-match(tp, std.breaks)] # 剔除掉省略掉的时间
      print(sprintf("breaks --> %s(%d)", breaks, length(breaks)))
      match(breaks, index(kdata)) |> na.omit() # 剔除NA值
    }, labels=\(x) { # 坐标id
      print(sprintf("labels:%s(%d)", x, length(x)))
      points <- index(kdata)[x] # 提取时间点数据的时间索引
      # 定义关键时间点
      p1030 <- as.datetime("10:30:00")
      p1330 <- as.datetime("13:30:00")
      p2100 <- as.datetime("21:00:00")
      lbls <- dplyr::case_when( # 时间刻度
         points == p1030 ~ "10:15/30", # 上午盘的时间交接点
         points == p1330 ~ "11/13:30:00", # 下午盘的时间交接点
         minute(points) == 0 & points == p2100 ~ "15/21:00", # 晚盘的时间交接点
         minute(points) == 0 ~ format(points, "%H:00"), # 默认整点时刻
         TRUE ~ sprintf("%02d", minute(points)) # 默认非整点时刻
      ) # 时间刻度
      print(sprintf("labels --> %s(%d)", lbls, length(lbls)))
      lbls
    }, limits=\(x) { # 刻度区间
      print(sprintf("limits:%s(%d)", x, length(x)))
      x
    }) + # 自定义时间轴
  labs(x = "时间", y = "收盘价格", title = "价格线图") # 坐标轴名称

