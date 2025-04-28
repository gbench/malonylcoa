library(dplyr)
library(lubridate)

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

# 2. 计算KDJ指标
ema <- function(x, n) {
  if (n <= 1 || length(x) < n) return(rep(NA, length(x)))
  ema <- numeric(length(x))
  ema[n] <- mean(x[1:n], na.rm = TRUE)
  multiplier <- 2 / (n + 1)
  for (i in (n+1):length(x)) {
    ema[i] <- ifelse(is.na(ema[i-1]), NA, 
                     (x[i] * multiplier) + (ema[i-1] * (1 - multiplier)))
  }
  ema[1:(n-1)] <- NA
  ema
}

calculate_kdj <- function(ohlcv, n = 9, m1 = 3, m2 = 3) {
  high <- ohlcv$High
  low <- ohlcv$Low
  close <- ohlcv$Close
  rsv <- numeric(length(close))
  
  for (i in 1:length(close)) {
    if (i < n) {
      rsv[i] <- NA
    } else {
      hh <- max(high[(i-n+1):i], na.rm = TRUE)
      ll <- min(low[(i-n+1):i], na.rm = TRUE)
      if (hh == ll) {
        rsv[i] <- 50
      } else {
        rsv[i] <- (close[i] - ll) / (hh - ll) * 100
      }
    }
  }
  
  K <- ema(rsv, m1)
  D <- ema(K, m2)
  J <- 3*K - 2*D
  
  data.frame(K, D, J)
}

# 3. 寻找KDJ交叉点
find_kdj_cross <- function(kdj, confirm_bars) {
  K <- kdj$K
  D <- kdj$D
  n <- nrow(kdj)
  golden <- logical(n)
  dead <- logical(n)
  
  for (i in (confirm_bars + 1):(n - confirm_bars)) {
    if (any(is.na(K[(i-confirm_bars):i]), is.na(D[(i-confirm_bars):i]))) next
    
    # 金叉确认：前confirm_bars次K<=D，当前K>D
    if (all(K[(i-confirm_bars):i] > D[(i-confirm_bars):i]) &&
        any(K[(i-confirm_bars-1)] <= D[(i-confirm_bars-1)], na.rm = TRUE)) {
      golden[i] <- TRUE
    }
    # 死叉确认：前confirm_bars次K>=D，当前K<D
    if (all(K[(i-confirm_bars):i] < D[(i-confirm_bars):i]) &&
        any(K[(i-confirm_bars-1)] >= D[(i-confirm_bars-1)], na.rm = TRUE)) {
      dead[i] <- TRUE
    }
  }
  list(golden = which(golden), dead = which(dead))
}

# 4. 寻找价格拐点
find_peaks <- function(series, window_size) {
  n <- length(series)
  peaks <- logical(n)
  for (i in 1:n) {
    left <- max(1, i - window_size)
    right <- min(n, i + window_size)
    current_window <- series[left:right]
    if (!any(is.na(current_window)) && series[i] == max(current_window)) {
      peaks[i] <- TRUE
    }
  }
  which(peaks)
}

find_valleys <- function(series, window_size) {
  n <- length(series)
  valleys <- logical(n)
  for (i in 1:n) {
    left <- max(1, i - window_size)
    right <- min(n, i + window_size)
    current_window <- series[left:right]
    if (!any(is.na(current_window)) && series[i] == min(current_window)) {
      valleys[i] <- TRUE
    }
  }
  which(valleys)
}

# 5. 计算适应度
calculate_fitness <- function(cross, peaks, valleys) {
  golden_cross <- cross$golden
  dead_cross <- cross$dead
  
  fitness <- 0
  # 处理金叉和底部拐点
  for (gc in golden_cross) {
    if (length(valleys) == 0) next
    distances <- abs(valleys - gc)
    min_dist <- min(distances)
    fitness <- fitness + min_dist^2
  }
  # 处理死叉和顶部拐点
  for (dc in dead_cross) {
    if (length(peaks) == 0) next
    distances <- abs(peaks - dc)
    min_dist <- min(distances)
    fitness <- fitness + min_dist^2
  }
  fitness
}

# 6. 参数优化主函数
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