# app.R - 优化的K线图应用，支持增量K线计算
library(shiny)
library(shinydashboard)
library(dplyr)
library(jsonlite)
library(later)
library(lubridate)

# 加载必要的包
required_packages <- c("later", "jsonlite", "dplyr", "shiny", "shinydashboard", "lubridate")
for(pkg in required_packages) {
  if(!require(pkg, character.only = TRUE)) {
    install.packages(pkg)
    library(pkg, character.only = TRUE)
  }
}

# 优化的ticks_to_kline函数 - 支持增量更新
ticks_to_kline <- function(ticks_df, period_minutes = 1, base_kline = NULL) {
  if(is.null(ticks_df) || nrow(ticks_df) == 0) {
    return(NULL)
  }
  
  # 创建时间戳
  if("ActionDay" %in% colnames(ticks_df) && "UpdateTime" %in% colnames(ticks_df)) {
    ticks_df$DateTime <- as.POSIXct(paste0(ticks_df$ActionDay, " ", ticks_df$UpdateTime), format="%Y%m%d %H:%M:%S")
    if("UpdateMillisec" %in% colnames(ticks_df)) {
      ticks_df$DateTime <- ticks_df$DateTime + ticks_df$UpdateMillisec / 1000
    }
    
    # 按周期聚合
    ticks_df$Period <- lubridate::floor_date(ticks_df$DateTime, paste0(period_minutes, " mins"))
    
    # 如果有基准K线数据，只计算新增的部分
    if(!is.null(base_kline) && length(base_kline$timestamp) > 0) {
      # 获取基准数据的最后时间戳
      last_timestamp <- max(base_kline$timestamp)
      last_period <- as.POSIXct(last_timestamp / 1000, origin = "1970-01-01")
      
      # 只保留大于等于最后时间戳的数据（包括最后一根K线，因为可能更新）
      ticks_df <- ticks_df %>% filter(Period >= last_period)
      
      if(nrow(ticks_df) == 0) {
        # 没有新数据，返回NULL
        return(NULL)
      }
    }
    
    # 计算K线
    kline <- ticks_df %>%
      group_by(Period) %>%
      summarise(
        open = first(LastPrice),
        high = max(LastPrice),
        low = min(LastPrice),
        close = last(LastPrice),
        volume = if(n() > 0) max(Volume, na.rm = TRUE) - min(Volume, na.rm = TRUE) else 0,
        .groups = 'drop'
      ) %>%
      arrange(Period)
    
    if(nrow(kline) == 0) {
      return(NULL)
    }
    
    # 转换为klinecharts需要的格式
    result <- list(
      timestamp = as.numeric(kline$Period) * 1000,
      open = as.numeric(kline$open),
      high = as.numeric(kline$high),
      low = as.numeric(kline$low),
      close = as.numeric(kline$close),
      volume = as.numeric(kline$volume)
    )
    
    # 如果有基准数据且不是全量计算，需要合并
    if(!is.null(base_kline) && length(base_kline$timestamp) > 0) {
      # 合并基准数据和新增数据
      all_timestamps <- c(base_kline$timestamp, result$timestamp)
      all_open <- c(base_kline$open, result$open)
      all_high <- c(base_kline$high, result$high)
      all_low <- c(base_kline$low, result$low)
      all_close <- c(base_kline$close, result$close)
      all_volume <- c(base_kline$volume, result$volume)
      
      # 去重（如果时间戳重复，保留新的）
      unique_indices <- !duplicated(all_timestamps, fromLast = TRUE)
      
      result <- list(
        timestamp = all_timestamps[unique_indices],
        open = all_open[unique_indices],
        high = all_high[unique_indices],
        low = all_low[unique_indices],
        close = all_close[unique_indices],
        volume = all_volume[unique_indices]
      )
    }
    
    return(result)
  }
  return(NULL)
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
      div(id = "chart", style = "width:100%; height:600px; background-color: #1e1e2f; border: 1px solid #4a5568; border-radius: 5px;"),
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
    if(is.null(ticks_df) || nrow(ticks_df) == 0) {
      return(NULL)
    }
    
    # 获取缓存的K线数据
    cached_kline <- NULL
    if(!is.null(cache[[instrument_id]]) && !is.null(cache[[instrument_id]]$data)) {
      # 将缓存的data转换为kline格式
      cached_data <- cache[[instrument_id]]$data
      if(length(cached_data) > 0) {
        cached_kline <- list(
          timestamp = sapply(cached_data, function(x) x$timestamp),
          open = sapply(cached_data, function(x) x$open),
          high = sapply(cached_data, function(x) x$high),
          low = sapply(cached_data, function(x) x$low),
          close = sapply(cached_data, function(x) x$close),
          volume = sapply(cached_data, function(x) x$volume)
        )
      }
    }
    
    # 使用优化的ticks_to_kline，传入基准K线数据
    full_kline <- ticks_to_kline(ticks_df, period_minutes, cached_kline)
    
    if(is.null(full_kline) || length(full_kline$timestamp) == 0) {
      return(NULL)
    }
    
    # 如果没有缓存，返回全量数据
    if(is.null(cached_kline) || length(cached_kline$timestamp) == 0) {
      add_debug(paste("全量加载:", instrument_id, "K线数量:", length(full_kline$timestamp)))
      if(length(full_kline$timestamp) > 0) {
        add_debug(paste("K线时间范围:", 
          as.POSIXct(full_kline$timestamp[1]/1000, origin="1970-01-01"),
          "到",
          as.POSIXct(full_kline$timestamp[length(full_kline$timestamp)]/1000, origin="1970-01-01")))
      }
      return(list(
        type = "full",
        instrument = instrument_id,
        data = full_kline,
        last_timestamp = max(full_kline$timestamp)
      ))
    }
    
    # 比较新旧数据，找出变化
    cached_last_ts <- max(cached_kline$timestamp)
    
    # 找出新增或更新的K线索引
    update_indices <- which(full_kline$timestamp >= cached_last_ts)
    
    if(length(update_indices) == 0) {
      add_debug(paste("无新K线:", instrument_id))
      return(NULL)
    }
    
    # 检查第一根需要更新的K线是否与缓存最后时间戳相同
    if(full_kline$timestamp[update_indices[1]] == cached_last_ts) {
      # 更新最后一根K线
      add_debug(paste("更新最后一根K线，时间戳:", full_kline$timestamp[update_indices[1]]))
      
      new_last <- list(
        timestamp = full_kline$timestamp[update_indices[1]],
        open = full_kline$open[update_indices[1]],
        high = full_kline$high[update_indices[1]],
        low = full_kline$low[update_indices[1]],
        close = full_kline$close[update_indices[1]],
        volume = full_kline$volume[update_indices[1]]
      )
      
      # 如果有更多新K线（时间戳大于缓存最后时间戳）
      if(length(update_indices) > 1) {
        new_indices <- update_indices[update_indices > which(full_kline$timestamp == cached_last_ts)]
        if(length(new_indices) > 0) {
          new_kline <- list(
            timestamp = full_kline$timestamp[new_indices],
            open = full_kline$open[new_indices],
            high = full_kline$high[new_indices],
            low = full_kline$low[new_indices],
            close = full_kline$close[new_indices],
            volume = full_kline$volume[new_indices]
          )
          add_debug(paste("同时有新增K线:", length(new_indices)))
          return(list(
            type = "update_and_incremental",
            instrument = instrument_id,
            update_data = new_last,
            new_data = new_kline,
            last_timestamp = max(full_kline$timestamp)
          ))
        }
      }
      
      return(list(
        type = "update",
        instrument = instrument_id,
        data = new_last,
        last_timestamp = max(full_kline$timestamp)
      ))
    } else {
      # 只有新增K线（时间戳大于缓存最后时间戳）
      new_kline <- list(
        timestamp = full_kline$timestamp[update_indices],
        open = full_kline$open[update_indices],
        high = full_kline$high[update_indices],
        low = full_kline$low[update_indices],
        close = full_kline$close[update_indices],
        volume = full_kline$volume[update_indices]
      )
      
      add_debug(paste("增量更新:", instrument_id, "新增K线:", length(update_indices)))
      
      return(list(
        type = "incremental",
        instrument = instrument_id,
        data = new_kline,
        last_timestamp = max(full_kline$timestamp)
      ))
    }
  }
  
  # 更新缓存
  update_cache <- function(instrument_id, result) {
    # 获取或创建缓存
    if(is.null(.state$kline_cache[[instrument_id]])) {
      .state$kline_cache[[instrument_id]] <- list(data = list())
    }
    
    # 根据类型更新缓存
    if(result$type == "full") {
      # 全量替换
      cache_data <- list()
      for(i in seq_along(result$data$timestamp)) {
        cache_data[[i]] <- list(
          timestamp = result$data$timestamp[i],
          open = result$data$open[i],
          high = result$data$high[i],
          low = result$data$low[i],
          close = result$data$close[i],
          volume = result$data$volume[i]
        )
      }
      .state$kline_cache[[instrument_id]]$data <- cache_data
      add_debug(paste("全量缓存更新:", instrument_id, "缓存K线数量:", length(cache_data)))
      
    } else if(result$type == "incremental") {
      # 增量添加
      for(i in seq_along(result$data$timestamp)) {
        .state$kline_cache[[instrument_id]]$data[[length(.state$kline_cache[[instrument_id]]$data) + 1]] <- list(
          timestamp = result$data$timestamp[i],
          open = result$data$open[i],
          high = result$data$high[i],
          low = result$data$low[i],
          close = result$data$close[i],
          volume = result$data$volume[i]
        )
      }
      add_debug(paste("增量缓存更新:", instrument_id, "新增K线数量:", length(result$data$timestamp)))
      
    } else if(result$type == "update") {
      # 更新最后一根K线
      if(length(.state$kline_cache[[instrument_id]]$data) > 0) {
        last_idx <- length(.state$kline_cache[[instrument_id]]$data)
        .state$kline_cache[[instrument_id]]$data[[last_idx]] <- list(
          timestamp = result$data$timestamp,
          open = result$data$open,
          high = result$data$high,
          low = result$data$low,
          close = result$data$close,
          volume = result$data$volume
        )
        add_debug(paste("更新缓存最后一根K线:", instrument_id))
      }
    } else if(result$type == "update_and_incremental") {
      # 更新最后一根并添加新K线
      if(length(.state$kline_cache[[instrument_id]]$data) > 0) {
        last_idx <- length(.state$kline_cache[[instrument_id]]$data)
        .state$kline_cache[[instrument_id]]$data[[last_idx]] <- list(
          timestamp = result$update_data$timestamp,
          open = result$update_data$open,
          high = result$update_data$high,
          low = result$update_data$low,
          close = result$update_data$close,
          volume = result$update_data$volume
        )
      }
      # 添加新K线
      for(i in seq_along(result$new_data$timestamp)) {
        .state$kline_cache[[instrument_id]]$data[[length(.state$kline_cache[[instrument_id]]$data) + 1]] <- list(
          timestamp = result$new_data$timestamp[i],
          open = result$new_data$open[i],
          high = result$new_data$high[i],
          low = result$new_data$low[i],
          close = result$new_data$close[i],
          volume = result$new_data$volume[i]
        )
      }
      add_debug(paste("更新缓存: 更新最后一根 + 新增", length(result$new_data$timestamp), "根K线"))
    }
  }
  
  # 启动K线更新器
  start_kline_updater <- function() {
    update_kline <- function() {
      if(!.state$running) return()
      
      client_val <- .state$ctpclient
      req_instrument_val <- .state$current_instrument
      period_val <- .state$current_period
      
      if(is.null(client_val) || req_instrument_val == "") {
        if(.state$running) {
          timers$kline_timer <- later::later(update_kline, 1)
        }
        return()
      }
      
      tryCatch({
        ticks_df <- client_val$ticks(req_instrument_val)
        
        if(!is.null(ticks_df) && nrow(ticks_df) > 0) {
          current_tick_count <- nrow(ticks_df)
          if(current_tick_count != .state$last_tick_count) {
            add_debug(paste("Tick更新:", req_instrument_val, "数量:", current_tick_count))
            .state$last_tick_count <- current_tick_count
          }
          
          result <- get_incremental_kline(ticks_df, period_val, req_instrument_val, .state$kline_cache)
          
          if(!is.null(result)) {
            # 准备发送到前端的数据
            if(result$type == "full") {
              ds_list <- vector("list", length(result$data$timestamp)) 
              for(i in seq_along(result$data$timestamp)) {
                ds_list[[i]] <- list(
                  timestamp = result$data$timestamp[i],
                  open = result$data$open[i],
                  high = result$data$high[i],
                  low = result$data$low[i],
                  close = result$data$close[i],
                  volume = result$data$volume[i]
                )
              }
              send_data <- list(instrument = req_instrument_val, type = "full", ds = ds_list)
              update_cache(req_instrument_val, result)
              add_debug(paste("发送全量数据:", length(ds_list), "根K线"))
              session$sendCustomMessage("push", send_data)
              
            } else if(result$type == "incremental") {
              ds_list <- vector("list", length(result$data$timestamp)) 
              for(i in seq_along(result$data$timestamp)) {
                ds_list[[i]] <- list(
                  timestamp = result$data$timestamp[i],
                  open = result$data$open[i],
                  high = result$data$high[i],
                  low = result$data$low[i],
                  close = result$data$close[i],
                  volume = result$data$volume[i]
                )
              }
              send_data <- list(instrument = req_instrument_val, type = "incremental", ds = ds_list)
              update_cache(req_instrument_val, result)
              add_debug(paste("发送增量数据:", length(ds_list), "根新K线"))
              session$sendCustomMessage("push", send_data)
              
            } else if(result$type == "update") {
              send_data <- list(
                instrument = req_instrument_val,
                type = "update",
                ds = list(
                  timestamp = result$data$timestamp,
                  open = result$data$open,
                  high = result$data$high,
                  low = result$data$low,
                  close = result$data$close,
                  volume = result$data$volume
                )
              )
              update_cache(req_instrument_val, result)
              add_debug(paste("发送更新数据: 更新最后一根K线"))
              session$sendCustomMessage("push", send_data)
              
            } else if(result$type == "update_and_incremental") {
              # 先发送更新
              update_data <- list(
                instrument = req_instrument_val,
                type = "update",
                ds = list(
                  timestamp = result$update_data$timestamp,
                  open = result$update_data$open,
                  high = result$update_data$high,
                  low = result$update_data$low,
                  close = result$update_data$close,
                  volume = result$update_data$volume
                )
              )
              session$sendCustomMessage("push", update_data)
              
              # 再发送新增
              ds_list <- list()
              for(i in seq_along(result$new_data$timestamp)) {
                ds_list[[i]] <- list(
                  timestamp = result$new_data$timestamp[i],
                  open = result$new_data$open[i],
                  high = result$new_data$high[i],
                  low = result$new_data$low[i],
                  close = result$new_data$close[i],
                  volume = result$new_data$volume[i]
                )
              }
              new_data <- list(instrument = req_instrument_val, type = "incremental", ds = ds_list)
              session$sendCustomMessage("push", new_data)
              
              update_cache(req_instrument_val, result)
              add_debug(paste("发送更新+增量数据"))
            }
          }
        }
      }, error = function(e) {
        add_debug(paste("更新K线错误:", e$message))
      })
      
      if(.state$running && !is.null(.state$ctpclient)) {
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
      
      client_val <- .state$ctpclient
      req_instrument_val <- .state$current_instrument
      
      if(is.null(client_val)) return()
      if(req_instrument_val != "") {
        tryCatch({
          stats <- client_val$stats(req_instrument_val)
          if(!is.null(stats) && is.list(stats)) {
            output$price_info <- renderPrint({
              cat("======== 实时行情 ========\n")
              cat(sprintf("合约: %s\n", req_instrument_val))
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
    
    client_val <- .state$ctpclient
    
    if(is.null(client_val)) {
      if(.state$running && !is.null(.state$ctpclient)) {
        timers$instrument_timer <- later::later(update_instruments_async, 3)
      }
      return()
    }
    
    instr <- tryCatch({
      if(!is.null(client_val$instrumentids)) {
        client_val$instrumentids
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

  chart.createIndicator("VOL");
  chart.createIndicator("MA", true, { id: "candle_pane" });
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
