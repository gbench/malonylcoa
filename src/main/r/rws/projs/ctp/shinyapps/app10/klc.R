# app.R - 优化的K线图应用，支持增量K线计算

# 加载必要的包
required_packages <- c("later", "shiny", "shinydashboard", "dplyr", "jsonlite", "lubridate", "data.table", "purrr")
for(pkg in required_packages) {
  if(!require(pkg, character.only = TRUE)) {
    install.packages(pkg)
    library(pkg, character.only = TRUE)
  }
}

# 向量化的floor_date实现
floor_date_vectorized <- function(datetime, period_seconds) {
  # 将POSIXct转换为数值并执行向量化地板除法
  timestamp <- as.numeric(datetime)
  floor_timestamp <- floor(timestamp / period_seconds) * period_seconds
  as.POSIXct(floor_timestamp, origin = "1970-01-01")
}

# data.table聚合函数
aggregate_with_dt <- function(ticks_df) {
  dt <- data.table::as.data.table(ticks_df)
  
  # 使用data.table的向量化聚合
  kline <- dt[, .(
    open = first(LastPrice),
    high = max(LastPrice),
    low = min(LastPrice),
    close = last(LastPrice),
    volume = last(Volume) - first(Volume),
    oint = last(OpenInterest)
  ), by = Period]
  
  # 排序（data.table默认按分组排序）
  data.table::setorder(kline, Period)
  
  return(as.data.frame(kline))
}

# dplyr聚合函数（优化版）
aggregate_with_dplyr <- function(ticks_df) {
  # 使用dplyr但避免重复计算
  ticks_df %>%
    dplyr::group_by(Period) %>%
    dplyr::summarise(
      open = dplyr::first(LastPrice),
      high = max(LastPrice),
      low = min(LastPrice),
      close = dplyr::last(LastPrice),
      volume = dplyr::last(Volume) - dplyr::first(Volume),
      oint = dplyr::last(OpenInterest),
      .groups = "drop"
    ) %>%
    dplyr::arrange(Period)
}

# 函数式合并K线数据
merge_kline_data <- function(base, new) {
  # 使用purrr进行函数式组合（如果可用）
  if (requireNamespace("purrr", quietly = TRUE)) {
    merge_fields <- function(field) {
      c(base[[field]], new[[field]])
    }
    
    all_fields <- purrr::map(names(base), merge_fields)
    names(all_fields) <- names(base)
    
    # 去重（保留新的）
    unique_indices <- !duplicated(all_fields$timestamp, fromLast = TRUE)
    
    purrr::map(all_fields, ~ .[unique_indices])
  } else {
    # 基础R实现
    all_timestamps <- c(base$timestamp, new$timestamp)
    unique_indices <- !duplicated(all_timestamps, fromLast = TRUE)
    
    list(
      timestamp = all_timestamps[unique_indices],
      open = c(base$open, new$open)[unique_indices],
      high = c(base$high, new$high)[unique_indices],
      low = c(base$low, new$low)[unique_indices],
      close = c(base$close, new$close)[unique_indices],
      volume = c(base$volume, new$volume)[unique_indices],
      oint = c(base$oint, new$oint)[unique_indices]
    )
  }
}

# 优化的ticks_to_kline函数 - 支持增量更新
ticks_to_kline <- function(ticks_df, period_minutes = 1, base_kline = NULL) {
  # 早期返回检查
  if (is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)
  
  # 检查必要列
  required_cols <- c("ActionDay", "UpdateTime", "LastPrice", "Volume")
  if (!all(required_cols %in% colnames(ticks_df))) return(NULL)
  
  # 使用向量化操作创建时间戳
  datetime_str <- paste(ticks_df$ActionDay, ticks_df$UpdateTime)
  ticks_df$DateTime <- as.POSIXct(datetime_str, format = "%Y%m%d %H:%M:%S")
  
  # 向量化添加毫秒
  if ("UpdateMillisec" %in% colnames(ticks_df)) {
    ticks_df$DateTime <- ticks_df$DateTime + ticks_df$UpdateMillisec / 1000
  }
  
  # 向量化计算周期（避免lubridate的逐行操作开销）
  period_seconds <- period_minutes * 60
  ticks_df$Period <- floor_date_vectorized(ticks_df$DateTime, period_seconds)
  
  # 处理基准数据过滤 - 向量化比较
  if (!is.null(base_kline) && length(base_kline$timestamp) > 0) {
    last_period <- floor_date_vectorized(as.POSIXct(max(base_kline$timestamp) / 1000, origin = "1970-01-01"), period_seconds)
    
    # 向量化过滤
    keep_indices <- ticks_df$Period >= last_period
    ticks_df <- ticks_df[keep_indices, ]
    
    if (nrow(ticks_df) == 0) return(NULL)
  }
  
  # 使用data.table进行高性能聚合（如果可用）
  if (requireNamespace("data.table", quietly = TRUE)) {
    kline <- aggregate_with_dt(ticks_df)
  } else {
    kline <- aggregate_with_dplyr(ticks_df)
  }
  
  if (nrow(kline) == 0) return(NULL)
  
  # 构建结果列表
  result <- list(
    timestamp = as.numeric(kline$Period) * 1000,
    open = kline$open,
    high = kline$high,
    low = kline$low,
    close = kline$close,
    volume = kline$volume,
    oint = kline$oint
  )
  
  # 合并基准数据（函数式合并）
  if (!is.null(base_kline) && length(base_kline$timestamp) > 0) {
    result <- merge_kline_data(base_kline, result)
  }
  
  return(result)
}

# UI
ui <- fluidPage(
  tags$head(
    tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css"),
    tags$style(HTML("
      body {
        background-color: #1e1e2f;
        color: #e0e0e0;
      }
      .title-panel {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 15px;
        border-radius: 5px;
        margin-bottom: 20px;
      }
      .title-panel h2 {
        margin: 0;
        color: white;
        text-align: center;
      }
      .sidebar-panel {
        background-color: #2d2d3f;
        border-radius: 5px;
        padding: 15px;
      }
      .main-panel {
        background-color: #1e1e2f;
      }
      .control-group {
        margin-bottom: 15px;
      }
      .control-group label {
        color: #e0e0e0;
        font-weight: bold;
      }
      .btn-primary {
        background-color: #667eea;
        border: none;
      }
      .btn-primary:hover {
        background-color: #5a67d8;
      }
      .btn-danger {
        background-color: #e53e3e;
        border: none;
      }
      .btn-danger:hover {
        background-color: #c53030;
      }
      .info-box {
        background-color: #2d2d3f;
        border-radius: 5px;
        padding: 10px;
        margin-top: 10px;
      }
      .info-box h4 {
        margin: 0 0 10px 0;
        color: #667eea;
      }
      .debug-panel {
        background-color: #1a1a2e;
        border: 1px solid #4a5568;
        border-radius: 5px;
        padding: 10px;
        margin-top: 20px;
        font-family: monospace;
        font-size: 12px;
        max-height: 200px;
        overflow-y: auto;
      }
    "))
  ),
  
  div(class = "title-panel", 
      titlePanel("CTP 实时K线图 - 动态刷新")),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      class = "sidebar-panel",

      div(class = "info-box",
        h4("实时信息"),
        verbatimTextOutput("price_info", placeholder = TRUE),
        hr()
      ),
      
      div(class = "control-group",
        tags$label("服务器配置"),
        textInput("host", "服务器地址", value = "192.168.1.41"),
        numericInput("port", "端口", value = 9898, min = 1, max = 65535),
        actionButton("connect_btn", "启动连接", class = "btn-primary", icon = icon("play"), width = "100%"),
        actionButton("disconnect_btn", "停止连接", class = "btn-danger", icon = icon("stop"), width = "100%"),
        hr()
      ),
      
      div(class = "control-group",
        tags$label("合约选择"),
        selectInput("instrument", "合约代码", choices = NULL),
        hr()
      ),
      
      div(class = "control-group",
        tags$label("K线设置"),
        numericInput("kline_period", "K线周期(分钟)", value = 1, min = 1, max = 60, step = 1),
        actionButton("refresh_btn", "刷新K线", icon = icon("sync"), width = "100%"),
        hr()
      ),
      
      div(class = "debug-panel",
        h4("调试信息"),
        verbatimTextOutput("debug_info", placeholder = TRUE)
      ),
      
      actionButton("exit", "退出应用", icon = icon("power-off"), class = "btn-danger", width = "100%")
    ),
    
    mainPanel(
      width = 9,
      class = "main-panel",
      div(id = "chart", style = "width:100%; height:800px; background-color: #1e1e2f; border: 1px solid #4a5568; border-radius: 5px;"),
      tags$script(src = "js/klinecharts.min.js"),
      tags$script(src = "js/chartapp.js"),
      tags$script(src = "js/exit.js")
    )
  )
)

server <- function(input, output, session) {
  
  # 使用普通变量存储状态
  .state <- new.env()
  .state$ctpclient <- NULL
  .state$available_instruments <- character()
  .state$is_connected <- FALSE
  .state$kline_cache <- list()  # 存储每个合约的完整K线数据
  .state$last_update_time <- 0
  .state$current_instrument <- ""
  .state$current_period <- 1
  .state$running <- TRUE
  .state$debug_messages <- character()
  .state$last_tick_count <- 0
  
  # 添加调试信息
  add_debug <- function(msg) {
    timestamp <- format(Sys.time(), "%H:%M:%S")
    debug_msg <- paste0("[", timestamp, "] ", msg)
    .state$debug_messages <- c(debug_msg, .state$debug_messages)[1:30]
    output$debug_info <- renderPrint({
      cat(paste(rev(.state$debug_messages), collapse="\n"))
    })
    cat(debug_msg, "\n")
  }
  
  # 定时器 ID
  timers <- list(
    instrument_timer = NULL,
    price_timer = NULL,
    kline_timer = NULL
  )
  
  # 辅助函数: 清空环境
  reset_env <- function() {
    if(!is.null(.state$ctpclient)) {
      tryCatch({
        undump_cmd <- gettextf("undump %s", .state$ctpclient$.sessionfd)
        add_debug(gettextf("尝试UNDUMP：'%s'", undump_cmd))
        writeLines(undump_cmd, .state$ctpclient$.conn)
        add_debug(gettextf("UNDUMP后返回数据被丢弃：[%s]", base::paste0(base::readLines(.state$ctpclient$.conn), collapse=",")))
        .state$ctpclient$stop()
      }, error = function(e) {
        message("停止连接时出错: ", e$message)
      })
    }
    .state$ctpclient <- NULL
    .state$available_instruments <- character()
    .state$is_connected <- FALSE
    .state$kline_cache <- list()
    .state$last_update_time <- 0
    .state$last_tick_count <- 0
    add_debug("环境已重置")
  }
  
  # 停止定时器
  stop_timers <- function() {
    if(!is.null(timers$instrument_timer)) timers$instrument_timer <- NULL
    if(!is.null(timers$price_timer)) timers$price_timer <- NULL
    if(!is.null(timers$kline_timer)) timers$kline_timer <- NULL
    add_debug("定时器已停止")
  }
  
  # 安全显示通知
  show_notification <- function(message, type = "default", duration = 3) {
    tryCatch({
      valid_types <- c("default", "message", "warning", "error")
      if(!type %in% valid_types) type <- "default"
      showNotification(message, type = type, duration = duration)
    }, error = function(e) {
      message("通知失败: ", message)
    })
  }
  
  # 优化的get_incremental_kline函数
  get_incremental_kline <- function(ticks_df, period_minutes, instrument_id, cache) {
    # 卫语句：早期返回
    if (is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)
    
    # 辅助函数：从缓存数据提取K线列表（向量化替代多个sapply）
    extract_cached_kline <- function(cached_data) {
      if (length(cached_data) == 0) return(NULL)
      fields <- c("timestamp", "open", "high", "low", "close", "volume", "oint")
      setNames(
        lapply(fields, function(f) vapply(cached_data, `[[`, numeric(1), f)),
        fields
      )
    }
    
    # 辅助函数：提取K线索引子集（函数式提取）
    subset_kline <- function(kline, indices) {
      if (length(indices) == 0) return(NULL)
      lapply(kline, `[`, indices)
    }
    
    # 辅助函数：格式化时间戳调试信息
    format_ts_range <- function(timestamps) {
      fmt <- function(ts) format(as.POSIXct(ts / 1000, origin = "1970-01-01"))
      sprintf("K线时间范围: %s 到 %s", fmt(timestamps[1]), fmt(tail(timestamps, 1)))
    }
    
    # 获取缓存数据（简化嵌套判断）
    cached_entry <- cache[[instrument_id]]
    cached_data <- if (!is.null(cached_entry)) cached_entry$data else NULL
    cached_kline <- extract_cached_kline(cached_data)
    
    # 生成完整K线
    full_kline <- ticks_to_kline(ticks_df, period_minutes, cached_kline)
    if (is.null(full_kline) || length(full_kline$timestamp) == 0) return(NULL)
    
    last_ts <- max(full_kline$timestamp)
    
    # 无缓存：返回全量
    if (is.null(cached_kline) || length(cached_kline$timestamp) == 0) {
      # 修复：%d -> %.0f（时间戳可能是numeric）
      add_debug(sprintf("全量加载: %s K线数量: %.0f", instrument_id, length(full_kline$timestamp)))
      add_debug(format_ts_range(full_kline$timestamp))
      return(list(type = "full", instrument = instrument_id, data = full_kline, last_timestamp = last_ts))
    }
    
    cached_last_ts <- max(cached_kline$timestamp)
    update_indices <- which(full_kline$timestamp >= cached_last_ts)
    
    if (length(update_indices) == 0) {
      add_debug(sprintf("无新K线: %s", instrument_id))
      return(NULL)
    }
    
    # 提取最后一根K线
    last_kline <- subset_kline(full_kline, update_indices[1])
    
    # 检查是否有新增K线（时间戳严格大于缓存最后时间戳）
    new_indices <- update_indices[full_kline$timestamp[update_indices] > cached_last_ts]
    new_kline <- subset_kline(full_kline, new_indices)
    
    has_update <- full_kline$timestamp[update_indices[1]] == cached_last_ts
    has_new <- length(new_indices) > 0
    
    # 使用switch处理不同返回类型（替代深层if嵌套）
    switch(
      paste0(has_update, "_", has_new),
      "TRUE_TRUE" = {
        add_debug(sprintf("更新最后一根K线，时间戳: %.0f", last_kline$timestamp))
        add_debug(sprintf("同时有新增K线: %.0f", length(new_indices)))
        list(type = "update_and_incremental", instrument = instrument_id,
            update_data = last_kline, new_data = new_kline, last_timestamp = last_ts)
      },
      "TRUE_FALSE" = {
        add_debug(sprintf("更新最后一根K线，时间戳: %.0f", last_kline$timestamp))
        list(type = "update", instrument = instrument_id, data = last_kline, last_timestamp = last_ts)
      },
      "FALSE_TRUE" = {
        add_debug(sprintf("增量更新: %s 新增K线: %.0f", instrument_id, length(new_indices)))
        list(type = "incremental", instrument = instrument_id, data = new_kline, last_timestamp = last_ts)
      },
      NULL
    )
  }
  
  # 更新缓存
  update_cache <- function(instrument_id, result) {
    cache <- .state$kline_cache
    if (is.null(cache[[instrument_id]])) {
      cache[[instrument_id]] <- list(data = list())
      .state$kline_cache <- cache
    }
    
    # 向量化：将kline list转换为list of records
    kline_to_records <- function(kline) {
      if (is.null(kline) || length(kline$timestamp) == 0) return(list())
      fields <- c("timestamp", "open", "high", "low", "close", "volume", "oint")
      n <- length(kline$timestamp)
      lapply(seq_len(n), function(i) {
        setNames(lapply(fields, function(f) kline[[f]][i]), fields)
      })
    }
    
    # 获取当前缓存数据
    cache_data <- cache[[instrument_id]]$data
    
    # 统一处理四种类型
    switch(result$type,
      "full" = {
        cache_data <- kline_to_records(result$data)
        msg <- sprintf("全量缓存更新: %s 缓存K线数量: %d", instrument_id, length(cache_data))
      },
      "incremental" = {
        new_records <- kline_to_records(result$data)
        cache_data <- c(cache_data, new_records)
        msg <- sprintf("增量缓存更新: %s 新增K线数量: %d", instrument_id, length(new_records))
      },
      "update" = {
        if (length(cache_data) > 0) {
          cache_data[[length(cache_data)]] <- setNames(
            lapply(c("timestamp", "open", "high", "low", "close", "volume", "oint"), 
                  function(f) result$data[[f]]),
            c("timestamp", "open", "high", "low", "close", "volume", "oint")
          )
        }
        msg <- sprintf("更新缓存最后一根K线: %s", instrument_id)
      },
      "update_and_incremental" = {
        if (length(cache_data) > 0) {
          cache_data[[length(cache_data)]] <- setNames(
            lapply(c("timestamp", "open", "high", "low", "close", "volume", "oint"), 
                  function(f) result$update_data[[f]]),
            c("timestamp", "open", "high", "low", "close", "volume", "oint")
          )
        }
        new_records <- kline_to_records(result$new_data)
        cache_data <- c(cache_data, new_records)
        msg <- sprintf("更新缓存: 更新最后一根 + 新增 %d 根K线", length(new_records))
      }
    )
    
    .state$kline_cache[[instrument_id]]$data <- cache_data
    add_debug(msg)
  }

  start_kline_updater <- function() {
    # 辅助函数：构建发送数据（向量化）
    build_send_data <- function(instrument, type, kline) {
      fields <- c("timestamp", "open", "high", "low", "close", "volume", "oint")
      ds <- if (type == "update" && !is.list(kline$timestamp)) {
        # 单条记录（timestamp是标量）
        setNames(lapply(fields, function(f) kline[[f]]), fields)
      } else {
        # 多条记录
        n <- length(kline$timestamp)
        lapply(seq_len(n), function(i) {
          setNames(lapply(fields, function(f) kline[[f]][i]), fields)
        })
      }
      list(instrument = instrument, type = type, ds = ds)
    }
    
    update_kline <- function() {
      if (!.state$running) return()
      
      req_instrument <- .state$current_instrument
      req_period <- .state$current_period
      
      # 辅助函数：发送并缓存（定义在内部以捕获req_instrument）
      send_and_cache <- function(send_data, result, debug_msg) {
        update_cache(req_instrument, result)
        add_debug(debug_msg)
        session$sendCustomMessage("push", send_data)
      }
      
      # 早期返回条件
      if (is.null(.state$ctpclient) || req_instrument == "") {
        schedule_next()
        return()
      }
      
      tryCatch({
        ticks_df <- .state$ctpclient$ticks(req_instrument)
        
        if (!is.null(ticks_df) && nrow(ticks_df) > 0) {
          # 更新tick计数
          current_tick_count <- nrow(ticks_df)
          if (current_tick_count != .state$last_tick_count) {
            add_debug(sprintf("Tick更新: %s 数量: %d", req_instrument, current_tick_count))
            .state$last_tick_count <- current_tick_count
          }
          
          result <- get_incremental_kline(ticks_df, req_period, req_instrument, .state$kline_cache)
          if (is.null(result)) {
            schedule_next()
            return()
          }
          
          # 统一处理所有类型
          switch(result$type,
            "full" = {
              send_data <- build_send_data(req_instrument, "full", result$data)
              send_and_cache(send_data, result, 
                            sprintf("发送全量数据: %d 根K线", length(result$data$timestamp)))
            },
            "incremental" = {
              send_data <- build_send_data(req_instrument, "incremental", result$data)
              send_and_cache(send_data, result,
                            sprintf("发送增量数据: %d 根新K线", length(result$data$timestamp)))
            },
            "update" = {
              send_data <- build_send_data(req_instrument, "update", result$data)
              send_and_cache(send_data, result, "发送更新数据: 更新最后一根K线")
            },
            "update_and_incremental" = {
              # 先发送更新
              update_data <- build_send_data(req_instrument, "update", result$update_data)
              session$sendCustomMessage("push", update_data)
              # 再发送新增
              new_data <- build_send_data(req_instrument, "incremental", result$new_data)
              session$sendCustomMessage("push", new_data)
              # 更新缓存
              update_cache(req_instrument, result)
              add_debug("发送更新+增量数据")
            }
          )
        }
      }, error = function(e) {
        add_debug(sprintf("更新K线错误: %s", e$message))
      })
      
      schedule_next()
    }
    
    schedule_next <- function() {
      if (.state$running && !is.null(.state$ctpclient)) {
        timers$kline_timer <- later::later(update_kline, 1)
      }
    }
    
    add_debug("启动K线更新器")
    update_kline()
  }
  
  # 启动价格监控
  start_price_monitor <- function() {
    monitor_prices <- function() {
      if(!.state$running) return()
      
      req_instrument <- .state$current_instrument
      
      if(is.null(.state$ctpclient)) return()
      if(req_instrument != "") {
        tryCatch({
          stats <- .state$ctpclient$stats(req_instrument)
          if(!is.null(stats) && is.list(stats)) {
            output$price_info <- renderPrint({
              cat("======== 实时行情 ========\n")
              cat(sprintf("合约: %s\n", req_instrument))
              cat(sprintf("最新价: %.2f\n", stats$last))
              cat(sprintf("均价: %.2f\n", stats$mean))
              cat(sprintf("最高价: %.2f\n", stats$max))
              cat(sprintf("最低价: %.2f\n", stats$min))
              cat(sprintf("Tick数: %d\n", stats$n))
              cat(sprintf("更新时间: %s\n", stats$updatetime))
              cat("===========================\n")
            })
          }
        }, error = function(e) {
          add_debug(paste("监控价格错误:", e$message))
        })
      }
      
      if(.state$running && !is.null(.state$ctpclient)) {
        timers$price_timer <- later::later(monitor_prices, 1)
      }
    }
    
    monitor_prices()
  }
  
  # 异步更新合约列表
  update_instruments_async <- function() {
    if(!.state$running) return()
    
    if(is.null(.state$ctpclient)) {
      if(.state$running && !is.null(.state$ctpclient)) {
        timers$instrument_timer <- later::later(update_instruments_async, 3)
      }
      return()
    }
    
    instr <- tryCatch({
      if(!is.null(.state$ctpclient$instrumentids)) {
        .state$ctpclient$instrumentids
      } else {
        character()
      }
    }, error = function(e) {
      add_debug(paste("获取合约列表错误:", e$message))
      character()
    })
    
    if(length(instr) > 0) {
      if(!identical(sort(instr), sort(.state$available_instruments))) {
        .state$available_instruments <- instr
        updateSelectInput(session, "instrument", choices = instr, selected = instr[1])
        add_debug(paste("合约列表已更新，共", length(instr), "个合约"))
      }
    }
    
    if(.state$running && !is.null(.state$ctpclient)) {
      timers$instrument_timer <- later::later(update_instruments_async, 5)
    }
  }
  
  # 监听合约选择变化
  observeEvent(input$instrument, {
    if(!is.null(input$instrument) && input$instrument != "") {
      .state$current_instrument <- input$instrument
      .state$last_tick_count <- 0
      add_debug(paste("切换合约到:", input$instrument))
      show_notification(paste("切换到合约:", input$instrument), "message", duration = 2)
      session$sendCustomMessage("switchInstrument", list(
        instrument = input$instrument,
        clearCache = TRUE
      ))
    }
  })
  
  # 监听K线周期变化
  observeEvent(input$kline_period, {
    if(!is.null(input$kline_period)) {
      .state$current_period <- input$kline_period
      add_debug(paste("K线周期更改为:", input$kline_period, "分钟"))
      show_notification(paste("K线周期已更改为:", input$kline_period, "分钟"), "message", duration = 2)
    }
  })
  
  # 启动连接
  observeEvent(input$connect_btn, {
    add_debug("开始连接CTP服务器...")
    
    if(!is.null(.state$ctpclient)) {
      showModal(modalDialog(
        title = "提示",
        "当前已有连接，将先停止并清空现有环境后再启动新连接。",
        easyClose = TRUE,
        footer = NULL
      ))
      reset_env()
    }
    
    stop_timers()
    .state$running <- TRUE
    show_notification("正在连接CTP服务器...", "message")
    
    tryCatch({
      if(!exists("ctpd_async")) {
        stop("ctpd_async 函数未定义，请确保已加载 ctpd.R 文件")
      }
      
      client <- ctpd_async(host = input$host, port = input$port)
      .state$ctpclient <- client
      .state$is_connected <- TRUE
      
      add_debug("CTP连接成功")
      show_notification("连接成功", "message", duration = 3)
      
      update_instruments_async()
      start_kline_updater()
      start_price_monitor()
      
    }, error = function(e) {
      add_debug(paste("连接失败:", e$message))
      showModal(modalDialog(
        title = "连接失败",
        paste("无法连接到服务器:", e$message),
        easyClose = TRUE,
        footer = modalButton("关闭")
      ))
    })
  })
  
  # 停止连接
  observeEvent(input$disconnect_btn, {
    if(!is.null(.state$ctpclient)) {
      add_debug("停止CTP连接")
      .state$running <- FALSE
      stop_timers()
      reset_env()
      showModal(modalDialog(
        title = "已停止",
        "CTP连接已停止，数据已清空。",
        easyClose = TRUE,
        footer = modalButton("确定")
      ))
      output$price_info <- renderPrint({
        cat("CTP客户端未启动\n")
        cat("请点击'启动连接'按钮连接CTP服务器\n")
      })
      session$sendCustomMessage("clearChart", list())
    } else {
      show_notification("当前没有活动的连接", "warning", duration = 3)
    }
  })
  
  # 手动刷新
  observeEvent(input$refresh_btn, {
    if(!is.null(.state$ctpclient)) {
      req_instrument <- .state$current_instrument
      if(req_instrument != "") {
        .state$kline_cache[[req_instrument]] <- NULL
        .state$last_tick_count <- 0
        add_debug(paste("手动刷新合约:", req_instrument))
        show_notification("正在刷新K线数据...", "message", duration = 2)
        session$sendCustomMessage("clearChartForInstrument", req_instrument)
      }
    }
  })
  
  # 退出应用
  observeEvent(input$exit, {
    showModal(modalDialog(
      title = "确认退出",
      "确定要退出应用程序吗？",
      easyClose = TRUE,
      footer = tagList(
        modalButton("取消"),
        actionButton("confirm_exit", "确定", class = "btn-danger")
      )
    ))
  })
  
  observeEvent(input$confirm_exit, {
    .state$running <- FALSE
    stop_timers()
    reset_env()
    stopApp()
  })
  
  session$onSessionEnded(function() {
    .state$running <- FALSE
    stop_timers()
    if(!is.null(.state$ctpclient)) {
      tryCatch({
        .state$ctpclient$stop()
      }, error = function(e) {
        message("退出时清理出错: ", e$message)
      })
    }
  })
}

# 创建必要的目录和文件
if(!dir.exists("www")) dir.create("www")
if(!dir.exists("www/js")) dir.create("www/js")
if(!dir.exists("www/css")) dir.create("www/css")

# 创建CSS文件
writeLines('
/* www/css/style.css */
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.shiny-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
}

.chart-container {
  background-color: #1e1e2f;
}

.klinecharts-pro {
  background-color: #1e1e2f;
}
', "www/css/style.css")

# 创建 chartapp.js
writeLines('
// www/js/chartapp.js
(function () {
  const chart = klinecharts.init("chart");
  let currentInstrument = null;
  let instrumentDataCache = new Map();
  
  function debugLog(msg) {
    console.log("[" + new Date().toLocaleTimeString() + "] " + msg);
  }
  
  debugLog("K线图表初始化开始");
  
  klinecharts.registerIndicator({
    name: "OINT",
    shortName: "OINT",
    calcParams: [],
    shouldFormatBigNumber: true,
    figures: [
      { key: "oint", title: "持仓量: ", type: "line" },
      { key: "preoint", title: "前仓量: ", type: "line" }
    ],
    calc: function(kLineDataList, _) {
      return kLineDataList.map(function(k, i, ks) {
        return {
          oint: k.oint || 0,
          preoint: i < 1 ? (k.oint || 0) : (ks[i - 1].oint || 0)
        };
      });
    }
  });

  chart.setStyles({
    grid: {
      horizontal: { color: "#2d2d3f", size: 1 },
      vertical: { color: "#2d2d3f", size: 1 }
    },
    candle: {
      candle: {
        bar: { upColor: "#ef5350", downColor: "#26a69a", noChangeColor: "#888888" }
      }
    },
    xAxis: {
      line: { color: "#4a5568" },
      tick: { color: "#e0e0e0" }
    },
    yAxis: {
      line: { color: "#4a5568" },
      tick: { color: "#e0e0e0" }
    }
  });

  // 持仓量
  klinecharts.registerIndicator({
    name: "OINT", // 指标名，之后用这个名字挂载
    shortName: "OINT", // 左上角缩写
    calcParams: [], // 本例不需要参数
    shouldFormatBigNumber: true, // 格式化参数
    figures: [
      {
        key: "oint",
        title: "持仓量: ",
        type: "line",
      },
      {
        key: "preoint",
        title: "前仓量: ",
        type: "line",
      },
    ],
    calc: (kLineDataList, _) => {
      // 核心：把 K 线数据里的 openInterest 抽出来返回
      return kLineDataList.map((k, i, ks) => ({
        oint: k.oint,
        preoint: i < 1 ? k.oint : ks[i - 1].oint,
      }));
    },
  });

  chart.createIndicator("VOL");
  chart.createIndicator("MA", true, { id: "candle_pane" });
   chart.createIndicator("OINT");
  chart.createIndicator("KDJ");
  chart.createIndicator("MACD");

  function loadInstrumentData(instrument, data, type) {
    debugLog("loadInstrumentData: " + instrument + ", type: " + type);
    
    if (!instrument || !data) {
      debugLog("Invalid data");
      return;
    }
    
    if (type === "full") {
      instrumentDataCache.set(instrument, data);
      debugLog("缓存全量数据: " + data.length + "根K线");
    } else if (type === "incremental") {
      let cached = instrumentDataCache.get(instrument) || [];
      instrumentDataCache.set(instrument, cached.concat(data));
      debugLog("缓存增量数据: " + data.length + "根新K线");
    } else if (type === "update") {
      debugLog("更新最后一根K线");
    }
    
    if (instrument === currentInstrument) {
      if (type === "full") {
        chart.clearData();
        chart.applyNewData(data);
        debugLog("全量数据已加载到图表");
      } else if (type === "incremental") {
        const currentData = chart.getDataList();
        if (!currentData || currentData.length === 0) {
          chart.applyNewData(data);
        } else {
          data.forEach(function(bar) {
            chart.updateData(bar);
          });
        }
        debugLog("增量数据已更新到图表");
      } else if (type === "update") {
        chart.updateData(data);
        debugLog("最后一根K线已更新");
      }
    }
  }
  
  function switchToInstrument(instrument) {
    debugLog("switchToInstrument: " + instrument);
    
    if (instrument === currentInstrument) {
      debugLog("合约相同，无需切换");
      return;
    }
    
    currentInstrument = instrument;
    const cachedData = instrumentDataCache.get(instrument);
    
    if (cachedData && cachedData.length > 0) {
      chart.clearData();
      chart.applyNewData(cachedData);
      debugLog("从缓存恢复数据: " + cachedData.length + "根K线");
    } else {
      chart.clearData();
      debugLog("等待数据加载");
    }
  }
  
  function clearChart() {
    debugLog("清空图表");
    chart.clearData();
  }

  Shiny.addCustomMessageHandler("push", function(data) {
    debugLog("收到推送消息, 类型: " + data.type);
    
    if (!data || !data.ds) {
      debugLog("消息无效");
      return;
    }
    
    if (data.type === "full") {
      loadInstrumentData(data.instrument, data.ds, "full");
    } else if (data.type === "incremental") {
      loadInstrumentData(data.instrument, data.ds, "incremental");
    } else if (data.type === "update") {
      loadInstrumentData(data.instrument, data.ds, "update");
    }
  });

  Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
    debugLog("收到切换合约消息");
    if (msg && msg.instrument) {
      switchToInstrument(msg.instrument);
    }
  });
  
  Shiny.addCustomMessageHandler("clearChart", function() {
    debugLog("收到清空图表消息");
    clearChart();
  });
  
  Shiny.addCustomMessageHandler("clearChartForInstrument", function(instrument) {
    debugLog("收到清空合约图表消息: " + instrument);
    if (instrument === currentInstrument) {
      chart.clearData();
      instrumentDataCache.delete(instrument);
      debugLog("强制刷新合约");
    }
  });

  window.addEventListener("beforeunload", function() {
    instrumentDataCache.clear();
  });

  debugLog("K线图表初始化完成");
})();
', "www/js/chartapp.js")

# 创建 exit.js
writeLines('
// www/js/exit.js
document.addEventListener("DOMContentLoaded", function() {
  setTimeout(function() {
    var exitBtn = document.getElementById("exit");
    if (exitBtn) {
      exitBtn.onclick = null;
      exitBtn.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (confirm("确定要退出应用程序吗？")) {
          Shiny.setInputValue("exit", "yes", {priority: "event"});
          setTimeout(function() {
            window.close();
          }, 100);
        }
      });
    }
  }, 500);
});
', "www/js/exit.js")

message("请确保 klinecharts.min.js 文件已放置在 www/js/ 目录下")
message("可以从 https://cdn.jsdelivr.net/npm/klinecharts@latest/dist/klinecharts.min.js 下载")

# 运行应用
shinyApp(ui, server)
