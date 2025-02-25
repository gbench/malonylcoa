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
  data.files <- list.files(path = ".", all.files = T, recursive = T, include.dirs = F)
  data.files # 数据文件集合
  # 读取指定数据(data.files的最后一项即最新的数据文件)
  fread(last(data.files)) # 数据文件读取
} else { # 读取数据库数据
  "t_rb2505_20250225" |>
    sprintf(fmt = "select * from %s") |>
    sqlquery.h10ctp2()
}) |>
  compute_kline() |>
  (\(x, kd = x["T09:00/T23:00"], ix = index(kd)) # 提取指定时间段内的数据&并清除噪音数据
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
  scale_x_continuous(
    n.breaks = 10,
    labels = partial(sapply, FUN = \(i) index(kdata)[i] |> strftime("%H:%M"))
  )

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
kdata[index(bigvols)] |> #
  transform(C_LO = dbl(Close - Low), C_HI = dbl(Close - High)) |> # xts 需要用dbl转换成数字,否则C_LO，C_HI命名无效
  transform(Desc = ifelse(abs(C_LO) <= abs(C_HI), -1, 1)) # 另外 这里必须使用transform mutate 不可以

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
sliding <- \(x, n, f) sapply(seq(x), \(i) if (i < n) NA else do.call(f, list(i = i, n = n, data = x[(i - n + 1):i])))

#' 周期为3的指数平均
#' @param init
ema3_init <- \(init = 50) \(acc, a) if (is.na(a)) NA else 2 / 3 * (if (is.na(acc)) init else acc) + a / 3

# 基础数据
kdata <- na.omit(kdata) # 清除NA值， kdata 时一个 OHLCV 的数据xts结构的数据
x <- kdata$Close |> as.numeric()
hi <- kdata$High |> as.numeric()
lo <- kdata$Low |> as.numeric()
# 指标计算
rsv <- sliding(x, 9, \(i, n, data, j = (i - n + 1):i, llv = min(lo[j]), hhv = max(hi[j]), amp = hhv - llv)
if (i < n || amp == 0) 50 else 100 * (x[i] - llv) / amp) # 未成熟随机值
rsv[is.na(rsv) | is.infinite(rsv)] <- 50 # 设置无效值为50
k <- Reduce(f = ema3_init(), x = rsv, accumulate = T) # K 值
d <- Reduce(f = ema3_init(), x = k, accumulate = T) # J值
# 生成xts 对象
data <- xts(cbind(rsv = rsv, k = k, d = d, j = 3 * k - 2 * d), order.by = index(kdata))["T21:00/T23:30"]

# 绘图
ggplot(data, aes(x = 1:nrow(data))) +
  geom_line(aes(y = rsv), col = rgb(.7, .7, .7)) +
  geom_line(aes(y = k), col = "red") +
  geom_line(aes(y = d), col = "blue") +
  geom_line(aes(y = j), col = rgb(.5, .1, .1)) +
  scale_x_continuous(
    n.breaks = 10,
    labels = partial(sapply, FUN = \(i) index(data)[i] |> strftime("%H:%M"))
  ) +
  labs(
    title = "专业KDJ指标分析",
    subtitle = format(start(data), "%Y-%m-%d"),
    x = "交易时段",
    y = "指标值"
  ) +
  theme(
    plot.title = element_text(face = "bold", hjust = 0.5),
    plot.subtitle = element_text(hjust = 0.5),
    panel.grid.minor = element_blank()
  )
