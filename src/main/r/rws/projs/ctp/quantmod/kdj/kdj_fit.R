# -----------------------------------------------------------------------------------------------------
# 对于tickdata 交易数据，数据框式结构(data.frame,xts, tibble等类型)
# 各列变量结构, ActionDay:交易日期, UpdateTime为交易时间(分钟),UpdateMillisec:交易时间(毫秒);
#               LastPrice:最新成交价格, Volume 累计成交量
# > head(tickdata,2) |> toJSON()
# [{"Id":1,"ActionDay":"20250428","AskPrice1":3145,"AskPrice2":0,"AskPrice3":0,"AskPrice4":0,"AskPrice5":0,"AskVolume1":1019,"AskVolume2":0,"AskVolume3":0,"AskVolume4":0,"AskVolume5":0,"AveragePrice":31424.8336,"BidPrice1":3144,"BidPrice2":0,"BidPrice3":0,"BidPrice4":0,"BidPrice5":0,"BidVolume1":2069,"BidVolume2":0,"BidVolume3":0,"BidVolume4":0,"BidVolume5":0,"ClosePrice":0,"CurrDelta":0,"CxxCtpCreateTime":"2025-04-28 09:12:48","ExchangeID":"","ExchangeInstID":"","HighestPrice":3164,"InstrumentID":"rb2510","LastPrice":3145,"LowerLimitPrice":2954,"LowestPrice":3102,"OpenInterest":2066536,"OpenPrice":3106,"PreClosePrice":3101,"PreDelta":0,"PreOpenInterest":1986742,"PreSettlementPrice":3110,"SettlementPrice":0,"TradingDay":"20250428","Turnover":66586771220,"UpdateMillisec":0,"UpdateTime":"09:12:47","UpperLimitPrice":3265,"Volume":2118922},{"Id":2,"ActionDay":"20250428","AskPrice1":3145,"AskPrice2":0,"AskPrice3":0,"AskPrice4":0,"AskPrice5":0,"AskVolume1":1019,"AskVolume2":0,"AskVolume3":0,"AskVolume4":0,"AskVolume5":0,"AveragePrice":31424.8336,"BidPrice1":3144,"BidPrice2":0,"BidPrice3":0,"BidPrice4":0,"BidPrice5":0,"BidVolume1":2066,"BidVolume2":0,"BidVolume3":0,"BidVolume4":0,"BidVolume5":0,"ClosePrice":0,"CurrDelta":0,"CxxCtpCreateTime":"2025-04-28 09:12:48","ExchangeID":"","ExchangeInstID":"","HighestPrice":3164,"InstrumentID":"rb2510","LastPrice":3145,"LowerLimitPrice":2954,"LowestPrice":3102,"OpenInterest":2066538,"OpenPrice":3106,"PreClosePrice":3101,"PreDelta":0,"PreOpenInterest":1986742,"PreSettlementPrice":3110,"SettlementPrice":0,"TradingDay":"20250428","Turnover":66586865560,"UpdateMillisec":500,"UpdateTime":"09:12:47","UpperLimitPrice":3265,"Volume":2118925}] 
#> 
# 编写程序(R语言）：
# 1） ohlcv（K线）计算：计算指定周期(分钟级别）级别，如(1, 3, 5, 15, 60) ohlcv的值,
# 2） kdj 指标计算：计算ohlcv (data.frame ,tibble, xts 等格式) 在指定参数默认为（9，3，3）时kdj指标。
# 3） kdj 与 ohlcv 适配度计算: 识别kd交叉点(金叉死叉), ohlcv的K线价格拐点(顶部拐点,底部拐点) 
# 通过kd交叉点K线拐点的匹配情况来评估kdj与ohlcv的匹配情况，即对特定的ohlcv数据K线, 
# 调试出最佳的kdj参数(rsv周期数n, k线平滑m1,d线平滑m2)以精准描述ohlcv即K线的价格拐点， 
# 验证标准为：KDJ的金叉与K线底部拐点，KDJ 死叉与K线顶部拐点　即KD交叉与拐点之间的长度的平方和最小!
# argmin(n, m1, m2) sum((kd交叉点与ohlcv价格拐点之间的距离)^2)：
# 交叉点与价格拐点以K线索引描述，距离为索引之间的差值描述
# 4) 测试&验证：对c(1,3,5) 周期的K线进行 结果验证
#   绘制K线图, 绘制KDJ曲线图（优化后的参数）, 标记出K线与KDJ线参数
# -----------------------------------------------------------------------------------------------------                        

library(dplyr)
library(lubridate)
library(RcppRoll)
library(TTR)

# 1. 生成多周期OHLCV数据
generate_ohlcv_for_periods <- function(tickdata, periods) {
  ohlcv_list <- list()
  
  # 合并日期和时间
  tickdata$datetime <- as.POSIXct(
    paste0(tickdata$ActionDay, " ", tickdata$UpdateTime),
    format = "%Y%m%d %H:%M:%S"
  )
  
  # 按时间排序
  tickdata <- tickdata[order(tickdata$datetime), ]
  
  for (period in periods) {
    # 切割时间到period分钟
    tickdata$time_group <- floor_date(tickdata$datetime, paste(period, "min"))
    
    # 计算OHLCV
    ohlcv <- tickdata %>%
      group_by(time_group) %>%
      summarize(
        Open = first(LastPrice),
        High = max(LastPrice),
        Low = min(LastPrice),
        Close = last(LastPrice),
        Volume = sum(Volume),
        .groups = 'drop'
      ) %>%
      rename(datetime = time_group) %>%
      na.omit()
    
    ohlcv_list[[as.character(period)]] <- ohlcv
  }
  return(ohlcv_list)
}

# 2. 计算ohlcv的KDJ 
calculate_kdj <- function(ohlcv, n = 9, m1 = 3, m2 = 3) {
  high <- ohlcv$High
  low <- ohlcv$Low
  close <- ohlcv$Close
  
  # 向量化计算滚动极值
  hh <- roll_max(high, n, fill = NA, align = "right", na.rm = TRUE)
  ll <- roll_min(low, n, fill = NA, align = "right", na.rm = TRUE)
  
  # 计算RSV（向量化操作）
  rsv <- (close - ll) / (hh - ll) * 100
  rsv[is.na(hh) | is.na(ll) | hh == ll] <- NA
  
  # 使用优化后的EMA计算K/D值
  K <- EMA(rsv, m1)
  D <- EMA(K, m2)
  J <- 3*K - 2*D
  
  data.frame(K, D, J)
}

# 3. 寻找KDJ交叉点
find_kdj_cross <- function(kdj, confirm_bars) {
  K <- kdj$K
  D <- kdj$D
  n <- length(K)
  
  # 生成逻辑向量并处理NA
  k_above_d <- K > D
  k_below_d <- K < D
  valid <- !is.na(K) & !is.na(D)
  
  # 计算有效窗口（confirm_bars长度内无NA）
  valid_window <- roll_sum(valid, confirm_bars, align = "right", fill = NA) == confirm_bars
  
  # 金叉条件计算 -------------------------------------------------
  # 条件1：连续confirm_bars个K>D
  cond_golden1 <- roll_sum(k_above_d, confirm_bars, align = "right", fill = NA) == confirm_bars
  # 条件2：前一个位置K<=D（使用滞后操作）
  cond_golden2 <- c(rep(NA, confirm_bars), (K <= D)[1:(n - confirm_bars)])
  golden <- cond_golden1 & cond_golden2 & valid_window
  
  # 死叉条件计算 -------------------------------------------------
  # 条件1：连续confirm_bars个K<D
  cond_dead1 <- roll_sum(k_below_d, confirm_bars, align = "right", fill = NA) == confirm_bars
  # 条件2：前一个位置K>=D（使用滞后操作）
  cond_dead2 <- c(rep(NA, confirm_bars), (K >= D)[1:(n - confirm_bars)])
  dead <- cond_dead1 & cond_dead2 & valid_window
  
  # 处理边界NA值
  golden[is.na(golden)] <- FALSE
  dead[is.na(dead)] <- FALSE
  
  list(golden = which(golden), dead = which(dead))
}

# 4. 寻找价格拐点-低点
find_peaks <- function(series, window_size) {
  n <- length(series)
  indices <- seq_len(n)
  lefts <- pmax(1L, indices - window_size)
  rights <- pmin(n, indices + window_size)
  
  # 向量化计算每个窗口是否包含NA
  valid_windows <- mapply(function(l, r) !any(is.na(series[l:r])), lefts, rights, SIMPLIFY = TRUE)
  
  # 计算每个窗口的最大值（仅处理有效窗口）
  max_values <- mapply(function(l, r) if (valid_windows[l]) max(series[l:r]) else NA, lefts, rights, SIMPLIFY = TRUE)
  
  # 确定峰值位置
  which(valid_windows & (series == max_values))
}

# 4. 寻找价格拐点-高点
find_valleys <- function(series, window_size) {
  n <- length(series)
  indices <- seq_len(n)
  lefts <- pmax(1L, indices - window_size)
  rights <- pmin(n, indices + window_size)
  
  # 向量化计算每个窗口是否包含NA
  valid_windows <- mapply(function(l, r) !any(is.na(series[l:r])), lefts, rights, SIMPLIFY = TRUE)
  
  # 计算每个窗口的最小值（仅处理有效窗口）
  min_values <- mapply(function(l, r) if (valid_windows[l]) min(series[l:r]) else NA, lefts, rights, SIMPLIFY = TRUE)
  
  # 确定谷值位置
  which(valid_windows & (series == min_values))
}

#' 5. 计算适应度
#’
#' 计算kd交叉点cross与价格拐点(peaks,valleys) 之间距离的差值的平方和fitness
#’ 用以表示kd交叉点（金叉/死叉） 标记 价格拐点的适配度：
#' fitness越小cross预测价格趋势就越高, 由此该模式kdj描述价格趋势能力就越强：
#‘
#' @param cross kdj的k线与d线的交叉位置索引
#' @param peaks 价格顶部拐点
#' @param valleys 价格底部拐点
calculate_fitness <- function(cross, peaks, valleys) {
  
  # 定义局部函数计算单个交叉类型到拐点的适应度
  compute_fitness <- function(cross_points, turns) {
    if (length(cross_points) == 0 || length(turns) == 0) return(0)
    outer(cross_points, turns, \(a, b) abs(a - b)) |> apply(1, min) |> (\(x) x^2)() |> sum()
  }
  
  # 计算金叉和死叉的适应度并求和
  with(cross, compute_fitness(golden, valleys) + compute_fitness(dead, peaks))
}

#‘ 6. 参数优化主函数
#’ @param tickdata 交易数据数据框: 各个列变量
#’          ActionDay 为交易日期, UpdateTime为交易时间(分钟）
#'          UpdateMillisec为交易时间(毫秒), LastPrice:最新成交价格
#’ @param periods K线周期向量如：c(1,3,5) 表示1,3,5 分钟的K线
#’ @param periods K线周期向量如：c(1,3,5) 表示1,3,5 分钟的K线
#’ @param n_range kdj的周期（计算RSV采用数据窗口大小）范围：如 c(5, 10)
#’ @param m1_range kdj的K线平滑参数的数据范围：如 c(2, 5)
#’ @param m2_range kdj的D线平滑参数的数据范围：如 c(2, 5)
#’ @param confirm_bars 计算kdj的k线与d线交叉时进行交叉点验证的ohlcv柱子的K线数量
#’ @param window_size 计算价格拐点采用的数据窗口大小：如5
optimize_kdj_strategy <- function(tickdata, periods, n_range, m1_range, m2_range, 
                                  confirm_bars, window_size) {
  ohlcv_list <- generate_ohlcv_for_periods(tickdata, periods)
  results <- list()
  
  for (period in periods) {
    period_str <- as.character(period)
    ohlcv <- ohlcv_list[[period_str]]
    if (nrow(ohlcv) < max(n_range) + max(m1_range, m2_range)) next
    
    best_fitness <- Inf
    best_params <- NULL
    
    param_grid <- expand.grid(
      n = seq(n_range[1], n_range[2]),
      m1 = seq(m1_range[1], m1_range[2]),
      m2 = seq(m2_range[1], m2_range[2])
    )
    
    for (i in 1:nrow(param_grid)) {
      params <- param_grid[i, ]
      n <- params$n
      m1 <- params$m1
      m2 <- params$m2
      
      kdj <- tryCatch({
        calculate_kdj(ohlcv, n, m1, m2)
      }, error = function(e) NULL)
      if (is.null(kdj)) next
      
      cross <- find_kdj_cross(kdj, confirm_bars)
      peaks <- find_peaks(ohlcv$High, window_size)
      valleys <- find_valleys(ohlcv$Low, window_size)
      
      fitness <- calculate_fitness(cross, peaks, valleys)
      
      if (fitness < best_fitness) {
        best_fitness <- fitness
        best_params <- params
      }
    }
    
    if (!is.null(best_params)) {
      results[[period_str]] <- list(
        period = period_str,
        best_params = best_params,
        best_fitness = best_fitness
      )
    }
  }
  results
}

# 示例测试
sqlquery.adhoc <- \(...) sqlquery(host = "localhost", dbname = "ctp", ...) # 基础数据库函数
tickdata <- sqlquery.adhoc("select * from t_rb2510_20250428")

results <- optimize_kdj_strategy(
  tickdata,
  periods = c(1, 3, 5, 15, 30, 60),
  n_range = c(5, 10),
  m1_range = c(2, 5),
  m2_range = c(2, 5),
  confirm_bars = 2,
  window_size = 5
)

for (result in results) {
  cat(sprintf("周期: %s, 最佳参数: n=%d, m1=%d, m2=%d, 适应度: %.2f\n",
              result$period,
              result$best_params$n,
              result$best_params$m1,
              result$best_params$m2,
              result$best_fitness))
}
