aggregate_ohlcv <- function(ohlcv_data, period = "5 mins", 
                            volume_column = "Volume", 
                            return_xts = TRUE) {
  # 加载必要包
  if (!requireNamespace("xts", quietly = TRUE)) install.packages("xts")
  if (!requireNamespace("dplyr", quietly = TRUE)) install.packages("dplyr")
  if (!requireNamespace("lubridate", quietly = TRUE)) install.packages("lubridate")
  library(xts)
  library(dplyr)
  library(lubridate)
  
  # 检查输入数据类型
  is_xts <- is.xts(ohlcv_data)
  
  # 转换为统一的数据框格式处理
  if (is_xts) {
    df <- data.frame(DateTime = index(ohlcv_data), coredata(ohlcv_data))
  } else {
    df <- ohlcv_data
  }
  
  # 检查必要列
  required_cols <- c("DateTime", "Open", "High", "Low", "Close", volume_column)
  if (!all(required_cols %in% colnames(df))) {
    stop(paste("缺少必要列:", paste(setdiff(required_cols, colnames(df)), collapse = ", ")))
  }
  
  # 确保DateTime是POSIXct类型
  if (!inherits(df$DateTime, "POSIXct")) {
    df$DateTime <- as.POSIXct(df$DateTime)
  }
  
  # 核心聚合操作
  aggregated <- df %>%
    mutate(TimeGroup = floor_date(DateTime, unit = period)) %>%
    group_by(TimeGroup) %>%
    summarise(
      Open = first(Open),
      High = max(High),
      Low = min(Low),
      Close = last(Close),
      Volume = sum(.data[[volume_column]]),
      .groups = "drop"
    ) %>%
    rename(DateTime = TimeGroup) %>%
    arrange(DateTime)
  
  # 处理可能的空周期导致的NA值
  aggregated <- na.omit(aggregated)
  
  # 按需返回xts或data.frame
  if (return_xts) {
    xts_obj <- xts(aggregated[, -1], order.by = aggregated$DateTime)
    return(xts_obj)
  } else {
    return(as.data.frame(aggregated))
  }
}