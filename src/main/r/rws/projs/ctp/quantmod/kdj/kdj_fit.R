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