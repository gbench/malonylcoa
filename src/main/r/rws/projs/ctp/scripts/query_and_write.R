library(data.table)
library(tidyverse)

#' 创建目录
#' @param x 目录集合
mkdir <- \(x) x |> strsplit("[,/\\]+") |> structure(names=x) |> lapply( \(e) Reduce(x=e, \(acc, a) paste(acc, a, sep="/"), accumulate=T) |> lapply(dir.create) )

# ------------------------------------------------------
# 查询数据并写入文件
# ------------------------------------------------------

# 数据文件根目录
data.home <- "data/h10ctp2/"

#' 创建数据文件根目录
mkdir(data.home)

#' 读取二月份rb2505的数据
sqlquery.h10ctp2("show tables") |> unlist() |> # 从h10ctp2中读取数据
  grep(pat="rb2505_202502\\d{2}", value=T) |> # 读取指定名称模式的数据文件
  sprintf(fmt="select * from %s") |> 
  sqlquery.h10ctp2() %>%
  structure(., names=gsub(pat=".*\\st_(\\w+)$", "\\1", x=names(.))) |> ( \( xs, ns=names(xs) )  # 改名并存储
    lapply(ns, \(n) fwrite(getElement(xs, n), paste0(data.home, n, ".csv"))) )() # 存储到指定的data.home数据目录

# 查看data.home目录下的所有数据文件（不包括目录）
data.files <- list.files(path=data.home, all.files=T, recursive=T, include.dirs=F); data.files

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
kplot <- function(kdata) {
  # 时间轴绘图
  ggplot(kdata, aes(seq_along(index(kdata)))) + # 基础数据
    geom_line(aes(y=Close)) + # 收盘价
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
	lbls # 时点标签
      }, limits=\(x) { # 刻度区间
	print(sprintf("limits:%s(%d)", x, length(x)))
	x # 坐标轴的刻度区间范围，目前采用默认不予调整
      }) + # 自定义时间轴
    labs(x = "时间", y = "收盘价格", title = "价格线图") # 坐标轴名称
} # kplot

# ****************************************
# 数据准备
# ****************************************

# 间隔区间
interval <- "15 min" # 时间间隔
# 标准分割点
std.breaks <- "09:00:00,10:15:00;10:30:00,11:30:00;13:30:00,15:00:00;20:00:00,23:00:00" |> # 坐标刻度，标准时间刻度
  partition(delim=";") |> # 解析交易时段
  lapply(\(line, ps=compose(as.datetime, partition)(line), intv=interval) seq(ps[1], ps[2], by=intv)) |> # 提取交易标记点
  Reduce(c, init=as.POSIXct(character(0)), x=_) # Reduce 需要为init指定由初始类型，如果提供默认c()则会返回long型数据
# 当前工作区中的数据文件（以保证返回的文件路径都是相对于当前工作区的路径，而不是一个不可直接访问的简单字符串）
data.files <- list.files(path=".", all.files=T, recursive=T, include.dirs=F); data.files # 数据文件集合
# 读取指定数据:data.files 的最后一项
data <- fread(last(data.files)) # 数据文件读取
# K线数据
klinedata <- data |> compute_kline() # 解析数据为ohlcv结构的K线数据
# 指定范围的K线数据（精选时段）
kdata <- klinedata["T09:00/T23:00"] |> (\(kd, ix=index(kd))  # 剔除指定时间段后的数据
  kd[ !( ix>as.datetime("15:00:00") & ix<as.datetime("21:00:00") ) ])(); kdata # 精选时段数据 

# ****************************************
# 数据绘图
# ****************************************

# 绘图
kdata |> kplot()
