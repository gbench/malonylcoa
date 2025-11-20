# -----------------------------------------------------------------------------------
# 支撑压力位分析完整脚本
# 
# 用于分析期货合约的支撑位和压力位
# -----------------------------------------------------------------------------------

# 加载必要的包
library(dplyr)
library(purrr)
library(ggplot2)
library(zoo)
library(TTR)
library(tidyr)
library(lubridate)

batch_load()

# 定义数据源
ds10ctp <- partial(sqldframe, dbname = "ctp", host = "127.0.0.1", port = 3371)

# 数据读取函数
load_data <- function(tbl, span="UpdateTime<'12:00' and UpdateTime>'09:00'") {
  ds10ctp(gettextf("select * from %s %s", tbl, ifelse(!is.na(paste0("where ", span)),"")))
}

# 工具函数
instrument <- \(tbl) {
  regexpr("(?<=t_)[[:alnum:]]+(?=_[[:digit:]]{8})", tbl, perl=T) |> regmatches(x=tbl)
}

date_of <- \(tbl) {
  m <- regexpr("t_[[:alnum:]]+_([[:digit:]]{8})", tbl, perl = TRUE)
  regmatches(tbl, m) |> sub("t_[[:alnum:]]+_([[:digit:]]{8})", "\\1", x = _) |> ymd()
}

# 技术分析函数 - 支撑压力位识别
identify_support_resistance <- function(data) {
  # 数据预处理
  data_clean <- data %>%
    arrange(UpdateTime, UpdateMillisec) %>%
    mutate(
      full_timestamp = as.POSIXct(paste("2025-11-19", UpdateTime)) + UpdateMillisec/1000,
      price = LastPrice,
      volume = Volume
    ) %>%
    filter(price > 0, volume > 0)  # 过滤无效数据
  
  # 计算技术指标
  data_tech <- data_clean %>%
    mutate(
      # 移动平均
      ma5 = rollmean(price, 5, fill = NA, align = "right"),
      ma10 = rollmean(price, 10, fill = NA, align = "right"),
      ma20 = rollmean(price, 20, fill = NA, align = "right"),
      
      # 布林带
      bbands = BBands(price, n = 20, sd = 2),
      bb_upper = bbands[, "up"],
      bb_middle = bbands[, "mavg"],
      bb_lower = bbands[, "dn"],
      
      # RSI
      rsi = RSI(price, n = 14),
      
      # 成交量加权平均价
      vwap = cumsum(price * (volume - lag(volume, default = 0))) / cumsum(volume - lag(volume, default = 0)),
      
      # 寻找局部高点和低点
      is_local_high = price > lag(price, 1) & price > lag(price, 2) & 
        price > lead(price, 1) & price > lead(price, 2),
      is_local_low = price < lag(price, 1) & price < lag(price, 2) & 
        price < lead(price, 1) & price < lead(price, 2)
    )
  
  return(data_tech)
}

# 修复后的关键价格水平识别函数
find_key_levels_simple <- function(data_tech) {
  # 使用价格聚类方法识别支撑阻力位
  price_levels <- data_tech %>%
    mutate(price_rounded = round(price)) %>%
    group_by(price_rounded) %>%
    reframe(
      touch_count = n(),
      total_volume = sum(volume - lag(volume, default = first(volume)), na.rm = TRUE),
      avg_price = mean(price),
      price_volatility = sd(price, na.rm = TRUE)
    ) %>%
    arrange(desc(touch_count))
  
  # 基于价格行为分类支撑阻力
  # 支撑位：价格在该水平反弹上涨
  support_candidates <- data_tech %>%
    mutate(
      price_change = lead(price) - price,
      is_bounce = price_change > 0 & lag(price_change, 1) < 0  # 从下跌转为上涨
    ) %>%
    filter(is_bounce == TRUE) %>%
    group_by(price_rounded = round(price)) %>%
    reframe(
      bounce_count = n(),
      avg_bounce_strength = mean(price_change, na.rm = TRUE)
    ) %>%
    filter(bounce_count >= 2) %>%
    mutate(level_type = "支撑位")
  
  # 阻力位：价格在该水平受阻下跌
  resistance_candidates <- data_tech %>%
    mutate(
      price_change = lead(price) - price,
      is_rejection = price_change < 0 & lag(price_change, 1) > 0  # 从上涨转为下跌
    ) %>%
    filter(is_rejection == TRUE) %>%
    group_by(price_rounded = round(price)) %>%
    reframe(
      rejection_count = n(),
      avg_rejection_strength = mean(abs(price_change), na.rm = TRUE)
    ) %>%
    filter(rejection_count >= 2) %>%
    mutate(level_type = "阻力位")
  
  return(list(
    价格水平 = price_levels,
    支撑位 = support_candidates,
    阻力位 = resistance_candidates
  ))
}

# 斐波那契回撤分析
fibonacci_retracement <- function(data_tech) {
  # 计算当日最高价和最低价
  day_high <- max(data_tech$price, na.rm = TRUE)
  day_low <- min(data_tech$price, na.rm = TRUE)
  day_range <- day_high - day_low
  
  fib_levels <- c(0.236, 0.382, 0.5, 0.618, 0.786)
  fib_prices <- day_high - day_range * fib_levels
  
  fib_data <- data.frame(
    level = c("0.0%", "23.6%", "38.2%", "50.0%", "61.8%", "78.6%", "100.0%"),
    price = c(day_high, fib_prices, day_low),
    type = "斐波那契回撤"
  )
  
  return(fib_data)
}

# 成交量密集区分析
analyze_volume_clusters <- function(data_tech) {
  # 按价格区间分析成交量分布
  price_range <- range(data_tech$price, na.rm = TRUE)
  price_breaks <- seq(floor(price_range[1]), ceiling(price_range[2]), by = 2)  # 每2元一个区间
  
  volume_clusters <- data_tech %>%
    mutate(price_bin = cut(price, breaks = price_breaks)) %>%
    group_by(price_bin) %>%
    reframe(
      total_volume = sum(volume - lag(volume, default = first(volume)), na.rm = TRUE),
      avg_price = mean(price),
      tick_count = n()
    ) %>%
    arrange(desc(total_volume)) %>%
    filter(!is.na(price_bin)) %>%
    mutate(
      level_type = "成交量密集区",
      cluster_strength = total_volume / max(total_volume)  # 标准化强度
    )
  
  return(volume_clusters)
}

# 移动平均线支撑压力分析
analyze_ma_support_resistance <- function(data_tech) {
  ma_levels <- data_tech %>%
    select(full_timestamp, ma5, ma10, ma20) %>%
    tidyr::pivot_longer(cols = c(ma5, ma10, ma20), 
                        names_to = "ma_type", 
                        values_to = "ma_value") %>%
    group_by(ma_type) %>%
    reframe(
      current_value = last(ma_value),
      level_type = case_when(
        ma_type == "ma5" ~ "MA5支撑压力",
        ma_type == "ma10" ~ "MA10支撑压力", 
        ma_type == "ma20" ~ "MA20支撑压力"
      )
    ) %>%
    filter(!is.na(current_value))
  
  return(ma_levels)
}

# 可视化函数生成器
plot_support_resistance_generator <- function(tbl) {
  function(data_tech, key_levels, fib_levels, volume_clusters, ma_levels) {
    # 基础价格图表
    p_base <- ggplot(data_tech, aes(x = full_timestamp, y = price)) +
      geom_line(color = "blue", alpha = 0.7, linewidth = 0.8) +
      labs(title = gettextf("%s 支撑压力位分析 - %s", instrument(tbl), date_of(tbl)),
           x = "时间", y = "价格") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold"))
    
    # 添加移动平均线
    p_ma <- p_base +
      geom_line(aes(y = ma5), color = "red", linetype = "dashed", linewidth = 0.6, alpha = 0.8) +
      geom_line(aes(y = ma10), color = "orange", linetype = "dashed", linewidth = 0.6, alpha = 0.8) +
      geom_line(aes(y = ma20), color = "green", linetype = "dashed", linewidth = 0.6, alpha = 0.8) +
      annotate("text", x = min(data_tech$full_timestamp), y = max(data_tech$price), 
               label = "红色: MA5\n橙色: MA10\n绿色: MA20", 
               hjust = 0, vjust = 1, size = 3, color = "black")
    
    # 添加支撑阻力位
    support_lines <- key_levels$支撑位 %>% 
      filter(bounce_count >= 2) %>%
      arrange(desc(bounce_count)) %>%
      head(5)  # 取前5个最强支撑
    
    resistance_lines <- key_levels$阻力位 %>% 
      filter(rejection_count >= 2) %>%
      arrange(desc(rejection_count)) %>%
      head(5)  # 取前5个最强阻力
    
    p_levels <- p_ma
    
    if(nrow(support_lines) > 0) {
      p_levels <- p_levels +
        geom_hline(data = support_lines, 
                   aes(yintercept = price_rounded, alpha = bounce_count/5), 
                   color = "green", linetype = "solid", linewidth = 0.8) +
        geom_text(data = support_lines, 
                  aes(x = max(data_tech$full_timestamp), 
                      y = price_rounded, 
                      label = paste("支撑", price_rounded, "\n(", bounce_count, "次)")),
                  hjust = 1, vjust = -0.2, color = "darkgreen", size = 2.8)
    }
    
    if(nrow(resistance_lines) > 0) {
      p_levels <- p_levels +
        geom_hline(data = resistance_lines, 
                   aes(yintercept = price_rounded, alpha = rejection_count/5), 
                   color = "red", linetype = "solid", linewidth = 0.8) +
        geom_text(data = resistance_lines, 
                  aes(x = max(data_tech$full_timestamp), 
                      y = price_rounded, 
                      label = paste("阻力", price_rounded, "\n(", rejection_count, "次)")),
                  hjust = 1, vjust = 1.2, color = "darkred", size = 2.8)
    }
    
    # 添加斐波那契水平
    p_fib <- p_levels +
      geom_hline(data = fib_levels, 
                 aes(yintercept = price, linetype = type), 
                 color = "purple", alpha = 0.6, linewidth = 0.5) +
      geom_text(data = fib_levels, 
                aes(x = min(data_tech$full_timestamp), 
                    y = price, 
                    label = paste(level, round(price, 1))),
                hjust = 0, vjust = -0.5, color = "purple", size = 2.5)
    
    # 添加成交量密集区标注
    if(nrow(volume_clusters) > 0) {
      top_clusters <- volume_clusters %>% head(3)  # 取前3个成交量最大的区域
      
      # 从data_tech中获取时间范围
      time_min <- min(data_tech$full_timestamp)
      time_max <- max(data_tech$full_timestamp)
      
      p_final <- p_fib +
        # 使用annotate而不是geom_rect，避免数据框问题
        annotate("rect", 
                 xmin = time_min, xmax = time_max,
                 ymin = as.numeric(sub("\\((.+),.*", "\\1", top_clusters$price_bin[1])),
                 ymax = as.numeric(sub("[^,]*,([^]]*)\\]", "\\1", top_clusters$price_bin[1])),
                 alpha = 0.1, fill = "yellow") +
        geom_point(data = top_clusters,
                   aes(x = time_min + (time_max - time_min) * 0.5,  # 放在时间轴中间
                       y = avg_price,
                       size = cluster_strength),
                   color = "brown", alpha = 0.7) +
        geom_text(data = top_clusters,
                  aes(x = time_min + (time_max - time_min) * 0.5,
                      y = avg_price,
                      label = paste("成交量密集区\n", round(avg_price, 1))),
                  color = "brown", size = 2.5, alpha = 0.7, vjust = -1)
    } else {
      p_final <- p_fib
    }
    
    return(p_final)
  }
}

# 生成交易建议
generate_trading_recommendations <- function(key_levels, current_price, day_high, day_low) {
  recommendations <- list()
  
  # 寻找最近的支撑和阻力
  nearby_support <- key_levels$支撑位 %>% 
    filter(price_rounded < current_price) %>%
    arrange(desc(price_rounded)) %>%
    head(3)
  
  nearby_resistance <- key_levels$阻力位 %>% 
    filter(price_rounded > current_price) %>%
    arrange(price_rounded) %>%
    head(3)
  
  # 支撑位分析
  if(nrow(nearby_support) > 0) {
    strongest_support <- nearby_support %>% slice(1)
    recommendations$支撑位分析 <- paste(
      "最近支撑位:", strongest_support$price_rounded, 
      "(反弹次数:", strongest_support$bounce_count, ")"
    )
    
    # 次要支撑位
    if(nrow(nearby_support) > 1) {
      secondary_support <- nearby_support %>% slice(2)
      recommendations$次要支撑位 <- paste(
        "次要支撑:", secondary_support$price_rounded,
        "(反弹次数:", secondary_support$bounce_count, ")"
      )
    }
  } else {
    recommendations$支撑位分析 <- paste("下方支撑参考当日最低点:", day_low)
  }
  
  # 阻力位分析
  if(nrow(nearby_resistance) > 0) {
    strongest_resistance <- nearby_resistance %>% slice(1)
    recommendations$阻力位分析 <- paste(
      "最近阻力位:", strongest_resistance$price_rounded, 
      "(受阻次数:", strongest_resistance$rejection_count, ")"
    )
    
    # 次要阻力位
    if(nrow(nearby_resistance) > 1) {
      secondary_resistance <- nearby_resistance %>% slice(2)
      recommendations$次要阻力位 <- paste(
        "次要阻力:", secondary_resistance$price_rounded,
        "(受阻次数:", secondary_resistance$rejection_count, ")"
      )
    }
  } else {
    recommendations$阻力位分析 <- paste("上方阻力参考当日最高点:", day_high)
  }
  
  # 交易区间和建议
  if(nrow(nearby_support) > 0 && nrow(nearby_resistance) > 0) {
    trading_range <- strongest_resistance$price_rounded - strongest_support$price_rounded
    recommendations$交易区间 <- paste(
      "主要交易区间:", strongest_support$price_rounded, "-", strongest_resistance$price_rounded,
      "(幅度:", trading_range, "点)"
    )
    
    # 基于当前位置的建议
    position_in_range <- (current_price - strongest_support$price_rounded) / trading_range
    
    if(position_in_range < 0.3) {
      recommendations$操作建议 <- "价格接近强支撑，可考虑逢低做多，止损设于支撑下方"
      recommendations$风险等级 <- "低风险"
    } else if(position_in_range > 0.7) {
      recommendations$操作建议 <- "价格接近强阻力，可考虑逢高做空，止损设于阻力上方"
      recommendations$风险等级 <- "低风险"
    } else if(position_in_range >= 0.3 && position_in_range <= 0.7) {
      recommendations$操作建议 <- "价格处于区间中部，建议观望或进行区间震荡操作"
      recommendations$风险等级 <- "中等风险"
    }
    
    # 突破策略
    recommendations$突破策略 <- paste(
      "上破", strongest_resistance$price_rounded, "可追多，",
      "下破", strongest_support$price_rounded, "可追空"
    )
  }
  
  return(recommendations)
}

# 主分析函数 - 统一入口
analyze <- function(tbl, home=".") {
  # 创建对应的instrument文件夹
  inst <- instrument(tbl)
  date_str <- format(date_of(tbl), "%Y%m%d")
  dir_name <- gettextf("%s/%s/%s", home, inst, date_of(tbl))
  
  if (!dir.exists(dir_name)) {
    dir.create(dir_name, recursive = T)
    cat(gettextf("创建目录: %s\n", dir_name))
  }
  
  cat(gettextf("正在加载%s %s日数据...\n", inst, date_of(tbl)))
  data <- load_data(tbl)
  
  cat("进行技术指标计算...\n")
  data_tech <- identify_support_resistance(data)
  
  cat("识别关键价格水平...\n")
  key_levels <- find_key_levels_simple(data_tech)
  
  cat("斐波那契回撤分析...\n")
  fib_levels <- fibonacci_retracement(data_tech)
  
  cat("成交量密集区分析...\n")
  volume_clusters <- analyze_volume_clusters(data_tech)
  
  cat("移动平均线分析...\n")
  ma_levels <- analyze_ma_support_resistance(data_tech)
  
  cat("生成可视化...\n")
  plot_support_resistance <- plot_support_resistance_generator(tbl)
  plot_result <- plot_support_resistance(data_tech, key_levels, fib_levels, volume_clusters, ma_levels)
  
  # 当前价格和极值
  current_price <- tail(data_tech$price, 1)
  day_high <- max(data_tech$price, na.rm = TRUE)
  day_low <- min(data_tech$price, na.rm = TRUE)
  
  cat("生成交易建议...\n")
  recommendations <- generate_trading_recommendations(key_levels, current_price, day_high, day_low)
  
  # 输出分析结果
  cat("\n")
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  cat(gettextf("%s 支撑压力位分析报告\n", inst))
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  cat(gettextf("分析日期: %s\n", date_of(tbl)))
  cat("当前价格:", current_price, "\n")
  cat("当日最高:", day_high, "\n")
  cat("当日最低:", day_low, "\n")
  cat("价格区间:", round(day_low), "-", round(day_high), 
      "(幅度:", round(day_high - day_low, 1), "点)\n\n")
  
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("关键支撑位（前5）\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  if(nrow(key_levels$支撑位) > 0) {
    print(key_levels$支撑位 %>% 
            select(price_rounded, bounce_count, avg_bounce_strength) %>%
            head(5))
  } else {
    cat("未发现明显的支撑位\n")
  }
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("关键阻力位（前5）\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  if(nrow(key_levels$阻力位) > 0) {
    print(key_levels$阻力位 %>% 
            select(price_rounded, rejection_count, avg_rejection_strength) %>%
            head(5))
  } else {
    cat("未发现明显的阻力位\n")
  }
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("斐波那契关键水平\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  print(fib_levels)
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("移动平均线当前值\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  print(ma_levels)
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("成交量密集区（前3）\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  if(nrow(volume_clusters) > 0) {
    print(volume_clusters %>% 
            select(price_bin, avg_price, total_volume, cluster_strength) %>%
            head(3))
  } else {
    cat("未发现明显的成交量密集区\n")
  }
  
  cat("\n")
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  cat("交易建议\n")
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  walk2(names(recommendations), recommendations, function(name, value) {
    cat(name, ":", value, "\n")
  })
  
  # 保存分析结果到CSV文件
  cat("\n保存分析结果到CSV文件...\n")
  
  # 使用统一命名格式：xxxx_instrument_date.csv
  support_file <- file.path(dir_name, gettextf("support_%s_%s.csv", inst, date_str))
  resistance_file <- file.path(dir_name, gettextf("resistance_%s_%s.csv", inst, date_str))
  fibonacci_file <- file.path(dir_name, gettextf("fibonacci_%s_%s.csv", inst, date_str))
  volume_file <- file.path(dir_name, gettextf("volume_%s_%s.csv", inst, date_str))
  ma_file <- file.path(dir_name, gettextf("ma_%s_%s.csv", inst, date_str))
  plot_file <- file.path(dir_name, gettextf("chart_%s_%s.png", inst, date_str))
  
  write.csv(key_levels$支撑位, support_file, row.names = FALSE)
  write.csv(key_levels$阻力位, resistance_file, row.names = FALSE)
  write.csv(fib_levels, fibonacci_file, row.names = FALSE)
  write.csv(volume_clusters, volume_file, row.names = FALSE)
  write.csv(ma_levels, ma_file, row.names = FALSE)
  
  # 保存图表
  ggsave(plot_file, plot = plot_result, width = 12, height = 8, dpi = 300)
  
  cat("\n分析完成！结果已保存到文件夹:", dir_name, "\n")
  cat("支撑位:", support_file, "\n")
  cat("阻力位:", resistance_file, "\n")
  cat("斐波那契:", fibonacci_file, "\n")
  cat("成交量密集区:", volume_file, "\n")
  cat("移动平均线:", ma_file, "\n")
  cat("分析图表:", plot_file, "\n")
  
  # 输出总结
  cat("\n")
  cat(paste0(rep("*", 60), collapse = ""), "\n")
  cat("分析总结\n")
  cat(paste0(rep("*", 60), collapse = ""), "\n")
  cat("1. 基于价格行为识别支撑阻力位\n")
  cat("2. 结合斐波那契回撤提供关键水平参考\n")
  cat("3. 通过成交量验证支撑阻力强度\n")
  cat("4. 移动平均线提供动态支撑压力参考\n")
  cat("5. 生成具体交易建议和风险管理策略\n")
  cat(paste0(rep("*", 60), collapse = ""), "\n")
  
  # 显示图表
  cat("\n生成分析图表...\n")
  print(plot_result)
  
  return(list(
    技术数据 = data_tech,
    关键水平 = key_levels,
    斐波那契 = fib_levels,
    成交量密集区 = volume_clusters,
    移动平均线 = ma_levels,
    图表 = plot_result,
    建议 = recommendations
  ))
}

# 使用示例
# analysis_result <- analyze("t_rb2601_20251120")

# 批量生成报告
# sqldframe("show tables",dbname="ctp") |> unlist() |> grep(pattern="20251120", value=T) |> lapply(partial(analyze, home="reports"))