identify_kdj_cross <- function(kdj_data, 
                               confirm_bars = 1,
                               return_xts = FALSE) {
  # 加载必要包
  if (!requireNamespace("dplyr", quietly = TRUE)) install.packages("dplyr")
  library(dplyr)
  
  # 检查输入数据
  required_cols <- c("DateTime", "K", "D")
  missing_cols <- setdiff(required_cols, colnames(kdj_data))
  if (length(missing_cols) > 0) {
    stop(paste("缺少必要列:", paste(missing_cols, collapse = ", ")))
  }
  
  # 确保数据按时间排序
  kdj_data <- kdj_data %>% arrange(DateTime)
  
  # 计算金叉和死叉
  crosses <- kdj_data %>%
    mutate(
      # 计算K与D的关系
      k_above_d = K > D,
      k_below_d = K < D,
      
      # 识别交叉点（考虑确认柱）
      golden_cross = k_above_d & lag(k_below_d, confirm_bars),
      dead_cross = k_below_d & lag(k_above_d, confirm_bars),
      
      # 标记交叉类型
      cross_type = case_when(
        golden_cross ~ "Golden",
        dead_cross ~ "Dead",
        TRUE ~ NA_character_
      )
    ) %>%
    filter(!is.na(cross_type)) %>%
    select(DateTime, K, D, J, cross_type, Open, High, Low, Close, Volume)
  
  # 确保所有要求的列都存在，如果不存在则设为NA
  required_return_cols <- c("DateTime", "K", "D", "J", "cross_type", 
                            "Open", "High", "Low", "Close", "Volume")
  
  for (col in required_return_cols) {
    if (!col %in% colnames(crosses)) {
      crosses[[col]] <- NA
    }
  }
  
  # 按需返回xts或data.frame
  if (return_xts) {
    if (!requireNamespace("xts", quietly = TRUE)) {
      install.packages("xts")
    }
    library(xts)
    xts_obj <- xts(crosses[, -which(names(crosses) == "DateTime")], 
                   order.by = crosses$DateTime)
    return(xts_obj)
  } else {
    return(as.data.frame(crosses))
  }
}