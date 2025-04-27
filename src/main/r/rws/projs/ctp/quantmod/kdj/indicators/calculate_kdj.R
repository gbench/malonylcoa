calculate_kdj <- function(ohlcv_data, 
                          n = 9, 
                          k_smooth = 3, 
                          d_smooth = 3,
                          fill_na = TRUE) {
  # 加载必要包
  if (!requireNamespace("zoo", quietly = TRUE)) install.packages("zoo")
  if (!requireNamespace("dplyr", quietly = TRUE)) install.packages("dplyr")
  library(zoo)
  library(dplyr)
  
  # 参数验证（支持data.frame/tibble/xts对象）
  if (!(is.data.frame(ohlcv_data) || is.xts(ohlcv_data))) {
    stop("输入数据必须是data.frame、tibble或xts对象")
  }
  
  # 如果是xts对象转换为data.frame
  if (is.xts(ohlcv_data)) {
    ohlcv_data <- data.frame(DateTime = index(ohlcv_data), coredata(ohlcv_data))
  }
  
  # 检查必要列
  required_cols <- c("DateTime", "High", "Low", "Close")
  missing_cols <- setdiff(required_cols, colnames(ohlcv_data))
  if (length(missing_cols) > 0) {
    stop(paste("缺少必要列:", paste(missing_cols, collapse = ", ")))
  }
  
  # 确保数据按时间排序
  ohlcv_data <- ohlcv_data %>% 
    arrange(DateTime) %>% 
    as.data.frame()  # 确保是标准data.frame
  
  # 检查数据长度
  if (nrow(ohlcv_data) < n) {
    warning("数据长度不足，将使用可用数据计算")
    n <- min(n, nrow(ohlcv_data))
  }
  
  # 计算周期内极值（处理可能的NA值）
  ohlcv_data <- ohlcv_data %>%
    mutate(
      highest_high = zoo::rollapply(High, width = n, FUN = function(x) {
        if (all(is.na(x))) NA else max(x, na.rm = TRUE)
      }, align = "right", fill = NA),
      
      lowest_low = zoo::rollapply(Low, width = n, FUN = function(x) {
        if (all(is.na(x))) NA else min(x, na.rm = TRUE)
      }, align = "right", fill = NA)
    )
  
  # 计算RSV（处理除零情况）
  ohlcv_data <- ohlcv_data %>%
    mutate(
      rsv = ifelse(
        !is.na(highest_high) & !is.na(lowest_low) & (highest_high != lowest_low),
        100 * (Close - lowest_low) / (highest_high - lowest_low),
        NA  # 如果highest_high == lowest_low，设为NA
      )
    )
  
  # 初始化KD值
  ohlcv_data$K <- NA
  ohlcv_data$D <- NA
  
  # 设置初始值（找到第一个有效RSV位置）
  first_valid <- which(!is.na(ohlcv_data$rsv))[1]
  if (!is.na(first_valid)) {
    ohlcv_data$K[first_valid] <- 50
    ohlcv_data$D[first_valid] <- 50
    
    # 递归计算KD值（仅在有RSV值时更新）
    for (i in (first_valid+1):nrow(ohlcv_data)) {
      if (!is.na(ohlcv_data$rsv[i])) {
        ohlcv_data$K[i] <- (1 - 1/k_smooth) * ohlcv_data$K[i-1] + (1/k_smooth) * ohlcv_data$rsv[i]
        ohlcv_data$D[i] <- (1 - 1/d_smooth) * ohlcv_data$D[i-1] + (1/d_smooth) * ohlcv_data$K[i]
      }
    }
  }
  
  # 计算J值
  ohlcv_data$J <- 3 * ohlcv_data$K - 2 * ohlcv_data$D
  
  # 清理中间列
  ohlcv_data <- ohlcv_data %>%
    select(-highest_high, -lowest_low, -rsv)
  
  # 处理缺失值
  if (fill_na) {
    ohlcv_data <- ohlcv_data %>%
      mutate(
        K = zoo::na.locf(K, na.rm = FALSE),
        D = zoo::na.locf(D, na.rm = FALSE),
        J = zoo::na.locf(J, na.rm = FALSE)
      )
  }
  
  return(ohlcv_data)
}