# -----------------------------------------------------------------------------------
# Support and Resistance Analysis Complete Script
# 
# Used for analyzing support and resistance levels of futures contracts
# -----------------------------------------------------------------------------------

# Load necessary packages
library(dplyr)
library(purrr)
library(ggplot2)
library(zoo)
library(TTR)
library(tidyr)
library(lubridate)

# Load sqldframe database tools
batch_load()

# Define data source: ds, host:00, database name: ctp
ds00ctp <- partial(sqldframe, dbname = "ctp", host = "127.0.0.1", port = 3371)

# Data loading function
load_data <- function(tbl, begtime, endtime) {
  rb <- record.builder("##tbl,#begtime,#endtime")
  params <- rb(tbl, begtime, endtime)
  "select * from ##tbl where UpdateTime between #begtime and #endtime" |> fill(params) |> ds00ctp()
}

# Utility function to extract instrument name
extract_instrument <- \(tbl) {
  regexpr("(?<=t_)[[:alnum:]]+(?=_[[:digit:]]{8})", tbl, perl=T) |> regmatches(x=tbl)
}

# Extract date from table name
extract_date <- \(tbl) {
  m <- regexpr("t_[[:alnum:]]+_([[:digit:]]{8})", tbl, perl = TRUE)
  regmatches(tbl, m) |> sub("t_[[:alnum:]]+_([[:digit:]]{8})", "\\1", x = _) |> ymd()
}

# Technical analysis function - Support/Resistance Identification
identify_support_resistance <- function(data, span = 2) {
  # Generic local extreme detection (high/low)
  # span: Number of bars to check before/after (default: 2)
  is_local_extreme <- function(price_series, type = "high", span = 2) {
    n <- length(price_series)
    if (n <= 2 * span) return(rep(FALSE, n))  # Insufficient data
    
    # Define comparison operator
    compare_op <- if (type == "high") `>` else `<`
    
    # Build comparison logic with Reduce (replace loop)
    expr <- Reduce(
      function(acc, i) {
        # Dynamic comparison for lag(i) and lead(i)
        bquote(
          .(acc) & 
            .(compare_op)(price_series, lag(price_series, .(i))) & 
            .(compare_op)(price_series, lead(price_series, .(i)))
        )
      },
      x = 1:span,  # Iterate over span values
      init = quote(TRUE)  # Initial value for accumulation
    )
    
    # Evaluate and clean NA values
    result <- eval(expr, envir = environment())
    result[is.na(result)] <- FALSE
    result
  }
  
  # Data preprocessing
  data_clean <- data %>%
    arrange(UpdateTime, UpdateMillisec) %>%
    mutate(
      full_timestamp = as.POSIXct(paste("2025-11-19", UpdateTime)) + UpdateMillisec/1000,
      price = LastPrice,
      volume = Volume
    ) %>%
    filter(price > 0, volume > 0)  # Filter invalid data
  
  # Calculate technical indicators
  data_tech <- data_clean %>%
    mutate(
      # Moving averages (5/10/20 periods)
      ma5 = rollmean(price, 5, fill = NA, align = "right"),
      ma10 = rollmean(price, 10, fill = NA, align = "right"),
      ma20 = rollmean(price, 20, fill = NA, align = "right"),
      
      # Bollinger Bands (20-period, 2 SD)
      bbands = BBands(price, n = 20, sd = 2),
      bb_upper = bbands[, "up"],
      bb_middle = bbands[, "mavg"],
      bb_lower = bbands[, "dn"],
      
      # RSI (14-period)
      rsi = RSI(price, n = 14),
      
      # VWAP (Volume Weighted Average Price)
      vwap = cumsum(price * (volume - lag(volume, default = 0))) / cumsum(volume - lag(volume, default = 0)),
      
      # Local highs/lows via generic function
      is_local_high = is_local_extreme(price, type = "high", span = span),
      is_local_low = is_local_extreme(price, type = "low", span = span)
    )
  
  data_tech
}

# Fixed key price level identification function
find_key_levels_simple <- function(data_tech) {
  # Use price clustering to identify support/resistance levels
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
  
  # Support levels: price rebounds at this level
  support_candidates <- data_tech %>%
    mutate(
      price_change = lead(price) - price,
      is_bounce = price_change > 0 & lag(price_change, 1) < 0  # From decline to rise
    ) %>%
    filter(is_bounce == TRUE) %>%
    group_by(price_rounded = round(price)) %>%
    reframe(
      bounce_count = n(),
      avg_bounce_strength = mean(price_change, na.rm = TRUE)
    ) %>%
    filter(bounce_count >= 2) %>%
    mutate(level_type = "Support")
  
  # Resistance levels: price rejected at this level
  resistance_candidates <- data_tech %>%
    mutate(
      price_change = lead(price) - price,
      is_rejection = price_change < 0 & lag(price_change, 1) > 0  # From rise to decline
    ) %>%
    filter(is_rejection == TRUE) %>%
    group_by(price_rounded = round(price)) %>%
    reframe(
      rejection_count = n(),
      avg_rejection_strength = mean(abs(price_change), na.rm = TRUE)
    ) %>%
    filter(rejection_count >= 2) %>%
    mutate(level_type = "Resistance")
  
  return(list(
    price_levels = price_levels,
    support_levels = support_candidates,
    resistance_levels = resistance_candidates
  ))
}

# Fibonacci retracement analysis
fibonacci_retracement <- function(data_tech) {
  # Calculate daily high and low
  day_high <- max(data_tech$price, na.rm = TRUE)
  day_low <- min(data_tech$price, na.rm = TRUE)
  day_range <- day_high - day_low
  
  fib_levels <- c(0.236, 0.382, 0.5, 0.618, 0.786)
  fib_prices <- day_high - day_range * fib_levels
  
  fib_data <- data.frame(
    level = c("0.0%", "23.6%", "38.2%", "50.0%", "61.8%", "78.6%", "100.0%"),
    price = c(day_high, fib_prices, day_low),
    type = "Fibonacci Retracement"
  )
  
  return(fib_data)
}

# Volume cluster analysis
analyze_volume_clusters <- function(data_tech) {
  # Analyze volume distribution by price range
  price_range <- range(data_tech$price, na.rm = TRUE)
  price_breaks <- seq(floor(price_range[1]), ceiling(price_range[2]), by = 2)  # 2 units per bin
  
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
      level_type = "Volume Cluster",
      cluster_strength = total_volume / max(total_volume)  # Normalized strength
    )
  
  return(volume_clusters)
}

# Moving average support/resistance analysis
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
        ma_type == "ma5" ~ "MA5 Support/Resistance",
        ma_type == "ma10" ~ "MA10 Support/Resistance", 
        ma_type == "ma20" ~ "MA20 Support/Resistance"
      )
    ) %>%
    filter(!is.na(current_value))
  
  return(ma_levels)
}

# Visualization function generator
plot_support_resistance_generator <- function(tbl) {
  
  function(data_tech, key_levels, fib_levels, volume_clusters, ma_levels) {
    # Base price chart
    p_base <- ggplot(data_tech, aes(x = full_timestamp, y = price)) +
      geom_line(color = "blue", alpha = 0.7, linewidth = 0.8) +
      labs(title = gettextf("%s Support/Resistance Analysis - %s", extract_instrument(tbl), extract_date(tbl)),
           x = "Time", y = "Price") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold"))
    
    # Add moving averages
    p_ma <- p_base +
      geom_line(aes(y = ma5), color = "red", linetype = "dashed", linewidth = 0.6, alpha = 0.8) +
      geom_line(aes(y = ma10), color = "orange", linetype = "dashed", linewidth = 0.6, alpha = 0.8) +
      geom_line(aes(y = ma20), color = "green", linetype = "dashed", linewidth = 0.6, alpha = 0.8) +
      annotate("text", x = min(data_tech$full_timestamp), y = max(data_tech$price), 
               label = "Red: MA5\nOrange: MA10\nGreen: MA20", 
               hjust = 0, vjust = 1, size = 3, color = "black")
    
    # Add support/resistance levels
    support_lines <- key_levels$support_levels %>% 
      filter(bounce_count >= 2) %>%
      arrange(desc(bounce_count)) %>%
      head(5)  # Top 5 strongest supports
    
    resistance_lines <- key_levels$resistance_levels %>% 
      filter(rejection_count >= 2) %>%
      arrange(desc(rejection_count)) %>%
      head(5)  # Top 5 strongest resistances
    
    p_levels <- p_ma
    
    if(nrow(support_lines) > 0) {
      p_levels <- p_levels +
        geom_hline(data = support_lines, 
                   aes(yintercept = price_rounded, alpha = bounce_count/5), 
                   color = "green", linetype = "solid", linewidth = 0.8) +
        geom_text(data = support_lines, 
                  aes(x = max(data_tech$full_timestamp), 
                      y = price_rounded, 
                      label = paste("Support", price_rounded, "\n(", bounce_count, "times)")),
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
                      label = paste("Resistance", price_rounded, "\n(", rejection_count, "times)")),
                  hjust = 1, vjust = 1.2, color = "darkred", size = 2.8)
    }
    
    # Add Fibonacci levels
    p_fib <- p_levels +
      geom_hline(data = fib_levels, 
                 aes(yintercept = price, linetype = type), 
                 color = "purple", alpha = 0.6, linewidth = 0.5) +
      geom_text(data = fib_levels, 
                aes(x = min(data_tech$full_timestamp), 
                    y = price, 
                    label = paste(level, round(price, 1))),
                hjust = 0, vjust = -0.5, color = "purple", size = 2.5)
    
    # Add volume cluster annotations
    if(nrow(volume_clusters) > 0) {
      top_clusters <- volume_clusters %>% head(3)  # Top 3 volume clusters
      
      # Get time range from data_tech
      time_min <- min(data_tech$full_timestamp)
      time_max <- max(data_tech$full_timestamp)
      
      p_final <- p_fib +
        annotate("rect", 
                 xmin = time_min, xmax = time_max,
                 ymin = as.numeric(sub("\\((.+),.*", "\\1", top_clusters$price_bin[1])),
                 ymax = as.numeric(sub("[^,]*,([^]]*)\\]", "\\1", top_clusters$price_bin[1])),
                 alpha = 0.1, fill = "yellow") +
        geom_point(data = top_clusters,
                   aes(x = time_min + (time_max - time_min) * 0.5,  # Place in middle of time axis
                       y = avg_price,
                       size = cluster_strength),
                   color = "brown", alpha = 0.7) +
        geom_text(data = top_clusters,
                  aes(x = time_min + (time_max - time_min) * 0.5,
                      y = avg_price,
                      label = paste("Volume Cluster\n", round(avg_price, 1))),
                  color = "brown", size = 2.5, alpha = 0.7, vjust = -1)
    } else {
      p_final <- p_fib
    }
    
    return(p_final)
  }
}

# Generate trading recommendations
generate_trading_recommendations <- function(key_levels, current_price, day_high, day_low) {
  recommendations <- list()
  
  # Find nearest support and resistance
  nearby_support <- key_levels$support_levels %>% 
    filter(price_rounded < current_price) %>%
    arrange(desc(price_rounded)) %>%
    head(3)
  
  nearby_resistance <- key_levels$resistance_levels %>% 
    filter(price_rounded > current_price) %>%
    arrange(price_rounded) %>%
    head(3)
  
  # Support level analysis
  if(nrow(nearby_support) > 0) {
    strongest_support <- nearby_support %>% slice(1)
    recommendations$support_analysis <- paste(
      "Nearest support:", strongest_support$price_rounded, 
      "(Bounce count:", strongest_support$bounce_count, ")"
    )
    
    # Secondary support
    if(nrow(nearby_support) > 1) {
      secondary_support <- nearby_support %>% slice(2)
      recommendations$secondary_support <- paste(
        "Secondary support:", secondary_support$price_rounded,
        "(Bounce count:", secondary_support$bounce_count, ")"
      )
    }
  } else {
    recommendations$support_analysis <- paste("No support below, refer to daily low:", day_low)
  }
  
  # Resistance level analysis
  if(nrow(nearby_resistance) > 0) {
    strongest_resistance <- nearby_resistance %>% slice(1)
    recommendations$resistance_analysis <- paste(
      "Nearest resistance:", strongest_resistance$price_rounded, 
      "(Rejection count:", strongest_resistance$rejection_count, ")"
    )
    
    # Secondary resistance
    if(nrow(nearby_resistance) > 1) {
      secondary_resistance <- nearby_resistance %>% slice(2)
      recommendations$secondary_resistance <- paste(
        "Secondary resistance:", secondary_resistance$price_rounded,
        "(Rejection count:", secondary_resistance$rejection_count, ")"
      )
    }
  } else {
    recommendations$resistance_analysis <- paste("No resistance above, refer to daily high:", day_high)
  }
  
  # Trading range and recommendations
  if(nrow(nearby_support) > 0 && nrow(nearby_resistance) > 0) {
    trading_range <- strongest_resistance$price_rounded - strongest_support$price_rounded
    recommendations$trading_range <- paste(
      "Main trading range:", strongest_support$price_rounded, "-", strongest_resistance$price_rounded,
      "(Range:", trading_range, "points)"
    )
    
    # Position-based recommendations
    position_in_range <- (current_price - strongest_support$price_rounded) / trading_range
    
    if(position_in_range < 0.3) {
      recommendations$action_recommendation <- "Price near strong support, consider buying on dips with stop below support"
      recommendations$risk_level <- "Low risk"
    } else if(position_in_range > 0.7) {
      recommendations$action_recommendation <- "Price near strong resistance, consider selling on rallies with stop above resistance"
      recommendations$risk_level <- "Low risk"
    } else if(position_in_range >= 0.3 && position_in_range <= 0.7) {
      recommendations$action_recommendation <- "Price in middle of range, consider wait&see or conduct range-bound range trading"
      recommendations$risk_level <- "Medium risk"
    }
    
    # Breakout strategy
    recommendations$breakout_strategy <- paste(
      "Break above", strongest_resistance$price_rounded, "to go long, ",
      "Break below", strongest_support$price_rounded, "to go short"
    )
  }
  
  return(recommendations)
}

# Main analysis function - Unified entry
analyze <- function(tbl, begtime="09:00", endtime="12:00", home=".") {
  # Create corresponding instrument folder
  inst <- extract_instrument(tbl)
  date_str <- format(extract_date(tbl), "%Y%m%d")
  dir_name <- gettextf("%s/%s/%s", home, inst, date_str)
  
  if (!dir.exists(dir_name)) {
    dir.create(dir_name, recursive = T)
    cat(gettextf("Created directory: %s\n", dir_name))
  }
  
  cat(gettextf("Loading %s %s data...\n", inst, date_str))
  data <- load_data(tbl, begtime, endtime)
  
  cat("Calculating technical indicators...\n")
  data_tech <- identify_support_resistance(data)
  
  cat("Identifying key price levels...\n")
  key_levels <- find_key_levels_simple(data_tech)
  
  cat("Fibonacci retracement analysis...\n")
  fib_levels <- fibonacci_retracement(data_tech)
  
  cat("Volume cluster analysis...\n")
  volume_clusters <- analyze_volume_clusters(data_tech)
  
  cat("Moving average analysis...\n")
  ma_levels <- analyze_ma_support_resistance(data_tech)
  
  cat("Generating visualization...\n")
  plot_support_resistance <- plot_support_resistance_generator(tbl)
  plot_result <- plot_support_resistance(data_tech, key_levels, fib_levels, volume_clusters, ma_levels)
  
  # Current price and extremes
  current_price <- tail(data_tech$price, 1)
  day_high <- max(data_tech$price, na.rm = TRUE)
  day_low <- min(data_tech$price, na.rm = TRUE)
  
  cat("Generating trading recommendations...\n")
  recommendations <- generate_trading_recommendations(key_levels, current_price, day_high, day_low)
  
  # Output analysis results
  cat("\n")
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  cat(gettextf("%s Support/Resistance Analysis Report\n", inst))
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  cat(gettextf("Analysis Date: %s\n", date_str))
  cat("Current Price:", current_price, "\n")
  cat("Daily High:", day_high, "\n")
  cat("Daily Low:", day_low, "\n")
  cat("Price Range:", round(day_low), "-", round(day_high), 
      "(Range:", round(day_high - day_low, 1), "points)\n\n")
  
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("Key Support Levels (Top 5)\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  if(nrow(key_levels$support_levels) > 0) {
    print(key_levels$support_levels %>% 
            select(price_rounded, bounce_count, avg_bounce_strength) %>%
            head(5))
  } else {
    cat("No significant support levels found\n")
  }
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("Key Resistance Levels (Top 5)\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  if(nrow(key_levels$resistance_levels) > 0) {
    print(key_levels$resistance_levels %>% 
            select(price_rounded, rejection_count, avg_rejection_strength) %>%
            head(5))
  } else {
    cat("No significant resistance levels found\n")
  }
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("Fibonacci Key Levels\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  print(fib_levels)
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("Moving Average Current Values\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  print(ma_levels)
  
  cat("\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  cat("Volume Clusters (Top 3)\n")
  cat(paste0(rep("-", 40), collapse = ""), "\n")
  if(nrow(volume_clusters) > 0) {
    print(volume_clusters %>% 
            select(price_bin, avg_price, total_volume, cluster_strength) %>%
            head(3))
  } else {
    cat("No significant volume clusters found\n")
  }
  
  cat("\n")
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  cat("Trading Recommendations\n")
  cat(paste0(rep("=", 60), collapse = ""), "\n")
  walk2(names(recommendations), recommendations, function(name, value) {
    cat(name, ":", value, "\n")
  })
  
  # Save analysis results to CSV files
  cat("\nSaving analysis results to CSV files...\n")
  
  # Use unified naming format: prefix_instrument_date.suffix
  f <- \(prefix, suffix="csv") file.path(dir_name, gettextf("%s_%s_%s.%s", prefix, inst, date_str, suffix))
  support_file <- f("support")
  resistance_file <- f("resistance")
  fibonacci_file <- f("fibonacci")
  volume_file <- f("volume")
  ma_file <- f("ma")
  plot_file <- f("chart","png")
  
  write.csv(key_levels$support_levels, support_file, row.names = FALSE)
  write.csv(key_levels$resistance_levels, resistance_file, row.names = FALSE)
  write.csv(fib_levels, fibonacci_file, row.names = FALSE)
  write.csv(volume_clusters, volume_file, row.names = FALSE)
  write.csv(ma_levels, ma_file, row.names = FALSE)
  
  # Save chart
  ggsave(plot_file, plot = plot_result, width = 12, height = 8, dpi = 300)
  
  cat("\nAnalysis complete! Results saved to folder:", dir_name, "\n")
  cat("Support levels:", support_file, "\n")
  cat("Resistance levels:", resistance_file, "\n")
  cat("Fibonacci levels:", fibonacci_file, "\n")
  cat("Volume clusters:", volume_file, "\n")
  cat("Moving averages:", ma_file, "\n")
  cat("Analysis chart:", plot_file, "\n")
  
  # Output summary
  cat("\n")
  cat(paste0(rep("*", 60), collapse = ""), "\n")
  cat("Analysis Summary\n")
  cat(paste0(rep("*", 60), collapse = ""), "\n")
  cat("1. Identify support/resistance based on price action\n")
  cat("2. Provide key levels reference with Fibonacci retracement\n")
  cat("3. Verify support/resistance strength with volume\n")
  cat("4. Moving averages provide dynamic support/resistance reference\n")
  cat("5. Generate specific trading recommendations and risk management strategies\n")
  cat(paste0(rep("*", 60), collapse = ""), "\n")
  
  # Display chart
  cat("\nGenerating analysis chart...\n")
  print(plot_result)
  
  return(list(
    technical_data = data_tech,
    key_levels = key_levels,
    fibonacci = fib_levels,
    volume_clusters = volume_clusters,
    moving_averages = ma_levels,
    chart = plot_result,
    recommendations = recommendations
  ))
}

# Example usage with default path
analysis_result <- analyze("t_rb2601_20251120", "13:30", "15:00")

# Batch generate reports for all tables in ctp database
# sqldframe("show tables", dbname="ctp") |> unlist() |> grep(pattern="20251120", value=T) |>
#   lapply(partial(analyze, begtime="09:00", endtime="12:00", home="reports"))