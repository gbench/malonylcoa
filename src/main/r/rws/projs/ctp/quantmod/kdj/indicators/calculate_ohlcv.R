calculate_ohlcv <- function(tick_data, minutes = 1) {
  # 确保必要的包已加载
  if (!requireNamespace("dplyr", quietly = TRUE)) {
    install.packages("dplyr")
  }
  if (!requireNamespace("lubridate", quietly = TRUE)) {
    install.packages("lubridate")
  }
  library(dplyr)
  library(lubridate)
  
  # 检查必要列是否存在
  required_cols <- c("InstrumentID", "ActionDay", "UpdateTime", "LastPrice", "Volume")
  if (!all(required_cols %in% colnames(tick_data))) {
    stop(paste("缺少必要列:", paste(setdiff(required_cols, colnames(tick_data)), collapse = ", ")))
  }
  
  # 将时间转换为POSIXct格式
  tick_data <- tick_data %>%
    mutate(
      datetime = as.POSIXct(paste(ActionDay, UpdateTime), format = "%Y%m%d %H:%M:%S"),
      # 处理毫秒部分（如果有）
      datetime = if ("UpdateMillisec" %in% colnames(tick_data)) {
        datetime + as.numeric(UpdateMillisec)/1000
      } else {
        datetime
      }
    )
  
  # 创建时间分组变量
  tick_data <- tick_data %>%
    mutate(
      time_group = floor_date(datetime, unit = paste(minutes, "min"))
    )
  
  # 计算OHLCV
  ohlcv_data <- tick_data %>%
    group_by(InstrumentID, ActionDay, time_group) %>%
    summarise(
      Open = first(LastPrice),
      High = max(LastPrice),
      Low = min(LastPrice),
      Close = last(LastPrice),
      Volume = max(Volume) - min(Volume),  # 计算该分钟内的成交量变化
      .groups = "drop"
    ) %>%
    arrange(InstrumentID, time_group)
  
  # 重命名列以符合常规
  ohlcv_data <- ohlcv_data %>%
    rename(
      DateTime = time_group,
      Symbol = InstrumentID,
      Date = ActionDay
    )
  
  return(ohlcv_data)
}