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
    select(DateTime, K, D, cross_type)
  
  # 添加交叉时的价格信息（如果原始数据包含Close）
  if ("Close" %in% colnames(kdj_data)) {
    crosses <- crosses %>%
      left_join(
        kdj_data %>% select(DateTime, Close),
        by = "DateTime"
      )
  }
  
  # 按需返回xts或data.frame
  if (return_xts && requireNamespace("xts", quietly = TRUE)) {
    xts_obj <- xts::xts(crosses[, -1], order.by = crosses$DateTime)
    return(xts_obj)
  } else {
    return(as.data.frame(crosses))
  }
}
