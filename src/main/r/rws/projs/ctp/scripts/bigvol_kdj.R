# ----------------------------------------------------------------------
# 提取 交易数据中的 大成交量 项目（包含时刻）
# ----------------------------------------------------------------------

library(tidyverse)
library(data.table)
library(xts)

#' 一个将输入字符串内容转换成POSIXct日期格式的函数
as.datetime <- partial(as.POSIXct, format = "%H:%M:%S") # 时间分析

# K线数据的提取
kdata <- (if (T) { # 使用文件数据还是数据库数据
    # 当前工作区中的数据文件（以保证返回的文件路径都是相对于当前工作区的路径，而不是一个`不可直接访问`的简单字符串）
    data.files <- list.files(path = ".", pattern="*.csv", all.files = T, recursive = T, include.dirs = F)
    fread(last(data.files)) # 数据文件读取(最后一个数据文件,时间最新)
  } else { # 读取数据库数据
    "t_rb2505_20250225" |> sprintf(fmt = "select * from %s") |> sqlquery.h10ctp2()
  }) |> compute_kline() |> (\(x, kd = x["T09:00/T23:00"], ix = index(kd)) # 提取指定时间段内的数据&并清除噪音数据
    kd[ # 小时跨度的时间跨度过滤
      !(ix > as.datetime("15:00:00") & ix < as.datetime("21:00:00")) |
      !(ix > as.datetime("11:30:00") & ix < as.datetime("13:30:00")) |
      !(ix > as.datetime("10:15:00") & ix < as.datetime("10:30:00"))
    ])()
kdata # 精选时段数据

# ----------------------------------------------------------------------------------------------------------------------------------

# 显示K线图
ggplot(kdata, aes(seq(Close), Close)) +
  geom_line() +
  scale_x_continuous( n.breaks = 10, labels = partial(sapply, FUN = \(i) index(kdata)[i] |> strftime("%H:%M")))

# 提取交易数据（xts格式)
vol <- kdata$Volume # 获取成交量数据
x <- as.numeric(vol) # 转换成向量

# 计算经验累积分布函数
empirical_cdf <- x |> ecdf() # 结算经验分布函数

# 大单交易
bigvols <- vol[empirical_cdf(as.numeric(x)) > 0.95] # 获得大单交易量
# 显示大成交量
dbl <- as.numeric # 转换成字符串数值向量的简写
# 对于每一笔的大单成交量，一般来说都会对应着一次价格的大幅波动，因为，这往往意味着多空双方至少有一方
# 在其报价盘口的头部档位中，其买卖数量的增幅，较其先前出了现大幅增加，当此种增幅 形成了某种单边趋势时，
# 就必然会吞噬掉对方在其报价盘口中足够多的报价数量，若该吞噬能够彻底的将对方盘口挡位中头部价格的报价数量
# 完全吃掉，成交价格的波动即涨跌幅就出现了，且势头越强，涨跌幅也就也越大。然而，这也不是尽然，不可一概而论，
# 因为, 这里还会有双边趋势的时候，也就是双方同时都加大了投入力度，造成了旗鼓相当的局面，即当市场分歧较大时，
# 多头与空头可能同时抛单，结果就造成了，尽管成交量放大了，价格却变化不大的局面。
# 如果收盘价接近最低C_LO,说明 空头趋势 -1
# 如果收盘价接近最高C_HI,说明 多头趋势 1
kdata[index(bigvols)] |> # 依据bigvols时间索引提取kdata数据
  transform(C_LO = dbl(Close - Low), C_HI = dbl(Close - High)) |> # xts 需要用dbl转换成数字,否则C_LO，C_HI命名无效
  transform(Desc = dbl(ifelse(abs(C_LO) <= abs(C_HI), -1, 1))) # 另外，这里必须使用transform, 而mutate是不可以的，因为它不能用于xts结构

bigvols2 <- vol[x > quantile(x, 0.95)]
# 百分位数与经验分布计算的数据时一样
bigvols2 == bigvols

# 查看大成交量项出现的时间间隔
index(bigvols) |> diff()

# -------------------------------------------------------
# KDJ 绘图
# -------------------------------------------------------

# 滑动窗口计算
#' @param x 原始数据
#' @param n 窗口大小
#' @param  f 窗口应用函数
sliding <- \(x, n, f) sapply(seq_along(x), \(i, j = (i - n + 1):i) if (i < n) NA else do.call(f, list(i = i, n = n, j = j, data = x[j])))

#' 一阶指数平滑生成函数(Exponential Smoothing，ES，Generator GEN, 序列x[t]的平滑公式为：s[t]=a*s[t-1]+(1-a)*x[t]且 s[1]=init
#' @param n 周期长度(ES权重系数a的定义参数)，大于1的整数, 权重系数向量的结构为c(前期:n-1,当前期:1/n)
#' @param init 指标初始值，默认为50
#' @return 按照 c(n-1, 1)/n %*% c(pre：前期, cur：当前期) 的指数平滑
esgen <- \(n, init = 50) \(pre, cur) if (is.na(cur) || n < 2) NA else c(if (is.na(pre)) init else pre, cur) %*% c(n - 1, 1) / n

# 基础数据
kdata <- na.omit(kdata) # 清除NA值，kdata 是一个xts类型的OHLCV数据
x <- kdata$Close |> as.numeric() # 收盘价
hi <- kdata$High |> as.numeric() # 最高值
lo <- kdata$Low |> as.numeric() # 最低值
# 指标计算, i:当前坐标索引, n:窗口宽度, j:窗口索引范围, data:当前窗口数据, llv:窗口数据最低价, hhv:窗口数据最高价, amp:振幅大小
rsv <- sliding(x, 9, \(i, n, j, data, llv = min(lo[j]), hhv = max(hi[j]), amp = hhv - llv) if (i < n || amp == 0) NA else 100 * (x[i] - llv) / amp) # 未成熟随机值
k <- Reduce(f = esgen(3), x = rsv, accumulate = T) # K值
d <- Reduce(f = esgen(3), x = k, accumulate = T) # D值
data <- xts(cbind(x = seq_along(rsv), rsv = rsv, k = k, d = d, j = 3 * k - 2 * d), order.by = index(kdata))# 生成xts对象以便进行时间索引过滤
labels <- partial(sapply, FUN = \(i) index(kdata)[i] |> strftime("%H:%M")) # 注意index(kdata)使用kdata而非data以保证labels与data$x的同步,都使用kdata的绝对时间索引

# 绘图
ggplot(data["T09:00/T15:30"] |> apply(2, \(p) ifelse(is.na(p), 50, p)), aes(x)) +
  geom_line(aes(y = rsv), color = "grey70", alpha = 0.6, linetype = "dashed") +
  geom_line(aes(y = k), color = "red", linewidth = 0.8) +
  geom_line(aes(y = d), color = "blue", linewidth = 0.8) +
  geom_line(aes(y = j), color = "orchid3") +
  scale_x_continuous(n.breaks = 10, labels = labels) +
  labs(title = "专业KDJ指标分析", subtitle = format(start(data), "%Y-%m-%d"), x = "交易时段", y = "指标值") +
  theme(plot.title = element_text(face = "bold", hjust = 0.5), plot.subtitle = element_text(hjust = 0.5), panel.grid.minor = element_blank())
