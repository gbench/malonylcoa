# app.R - 修复版v2.2：修复停止时未发送undump导致的资源泄漏

# 加载必要的包
required_packages <- c("later", "shiny", "shinydashboard", "dplyr", "jsonlite", "lubridate", "data.table", "purrr", "R6")
for(pkg in required_packages) {
  if(!require(pkg, character.only = TRUE)) {
    install.packages(pkg)
    library(pkg, character.only = TRUE)
  }
}

# ============ 工具函数模块 ============

floor_date_vectorized <- function(datetime, period_seconds) {
  if (length(datetime) == 0) return(datetime)
  timestamp <- as.numeric(datetime)
  floor_timestamp <- floor(timestamp / period_seconds) * period_seconds
  as.POSIXct(floor_timestamp, origin = "1970-01-01")
}

aggregate_with_dt <- function(ticks_df) {
  dt <- data.table::as.data.table(ticks_df)
  kline <- dt[, .(
    open = first(LastPrice),
    high = max(LastPrice),
    low = min(LastPrice),
    close = last(LastPrice),
    volume = last(Volume) - first(Volume),
    oint = last(OpenInterest)
  ), by = Period]
  data.table::setorder(kline, Period)
  as.data.frame(kline)
}

aggregate_with_dplyr <- function(ticks_df) {
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

merge_kline_data <- function(base, new) {
  if (is.null(base) || length(base$timestamp) == 0) return(new)
  if (is.null(new) || length(new$timestamp) == 0) return(base)

  all_fields <- list(
    timestamp = c(base$timestamp, new$timestamp),
    open = c(base$open, new$open),
    high = c(base$high, new$high),
    low = c(base$low, new$low),
    close = c(base$close, new$close),
    volume = c(base$volume, new$volume),
    oint = c(base$oint, new$oint)
  )

  unique_indices <- !duplicated(all_fields$timestamp, fromLast = TRUE)
  lapply(all_fields, function(x) x[unique_indices])
}

convert_to_frontend_format <- function(kline_list) {
  if (is.null(kline_list) || length(kline_list$timestamp) == 0) return(list())

  lapply(seq_along(kline_list$timestamp), function(i) {
    list(
      timestamp = kline_list$timestamp[i],
      open = kline_list$open[i],
      high = kline_list$high[i],
      low = kline_list$low[i],
      close = kline_list$close[i],
      volume = kline_list$volume[i],
      oint = kline_list$oint[i]
    )
  })
}

# ============ K线计算模块 ============

ticks_to_kline <- function(ticks_df, period_minutes = 1, base_kline = NULL) {
  if (is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)

  required_cols <- c("ActionDay", "UpdateTime", "LastPrice", "Volume")
  if (!all(required_cols %in% colnames(ticks_df))) return(NULL)

  datetime_str <- paste(ticks_df$ActionDay, ticks_df$UpdateTime)
  ticks_df$DateTime <- as.POSIXct(datetime_str, format = "%Y%m%d %H:%M:%S")

  if ("UpdateMillisec" %in% colnames(ticks_df)) {
    ticks_df$DateTime <- ticks_df$DateTime + ticks_df$UpdateMillisec / 1000
  }

  period_seconds <- period_minutes * 60
  ticks_df$Period <- floor_date_vectorized(ticks_df$DateTime, period_seconds)

  if (!is.null(base_kline) && length(base_kline$timestamp) > 0) {
    last_ts <- max(base_kline$timestamp) / 1000
    last_period <- floor_date_vectorized(
      as.POSIXct(last_ts, origin = "1970-01-01"), 
      period_seconds
    )

    keep_indices <- ticks_df$Period >= last_period
    ticks_df <- ticks_df[keep_indices, ]

    if (nrow(ticks_df) == 0) return(NULL)
  }

  kline <- if (requireNamespace("data.table", quietly = TRUE)) {
    aggregate_with_dt(ticks_df)
  } else {
    aggregate_with_dplyr(ticks_df)
  }

  if (nrow(kline) == 0) return(NULL)

  result <- list(
    timestamp = as.numeric(kline$Period) * 1000,
    open = kline$open,
    high = kline$high,
    low = kline$low,
    close = kline$close,
    volume = kline$volume,
    oint = kline$oint
  )

  if (!is.null(base_kline) && length(base_kline$timestamp) > 0) {
    result <- merge_kline_data(base_kline, result)
  }

  return(result)
}

# ============ 状态管理类 ============

InstrumentStateManager <- R6::R6Class(
  "InstrumentStateManager",

  public = list(
    kline_cache = list(),
    tick_counts = list(),
    last_update_times = list(),
    current_instrument = NULL,
    current_period = 1,

    get_cache = function(instrument_id) {
      if (is.null(instrument_id)) return(NULL)
      cache <- self$kline_cache[[instrument_id]]
      if (is.null(cache)) return(NULL)

      if (length(cache) > 0) {
        list(
          timestamp = sapply(cache, function(x) x$timestamp),
          open = sapply(cache, function(x) x$open),
          high = sapply(cache, function(x) x$high),
          low = sapply(cache, function(x) x$low),
          close = sapply(cache, function(x) x$close),
          volume = sapply(cache, function(x) x$volume),
          oint = sapply(cache, function(x) x$oint)
        )
      } else {
        NULL
      }
    },

    update_cache = function(instrument_id, result) {
      if (is.null(instrument_id)) return()

      if (result$type == "full") {
        self$kline_cache[[instrument_id]] <- convert_to_frontend_format(result$data)
      } else if (result$type == "incremental") {
        new_data <- convert_to_frontend_format(result$data)
        existing <- self$kline_cache[[instrument_id]]
        if (is.null(existing)) existing <- list()
        self$kline_cache[[instrument_id]] <- c(existing, new_data)
      } else if (result$type == "update") {
        cache <- self$kline_cache[[instrument_id]]
        if (!is.null(cache) && length(cache) > 0) {
          cache[[length(cache)]] <- list(
            timestamp = result$data$timestamp,
            open = result$data$open,
            high = result$data$high,
            low = result$data$low,
            close = result$data$close,
            volume = result$data$volume,
            oint = result$data$oint
          )
          self$kline_cache[[instrument_id]] <- cache
        }
      } else if (result$type == "update_and_incremental") {
        cache <- self$kline_cache[[instrument_id]]
        if (!is.null(cache) && length(cache) > 0) {
          cache[[length(cache)]] <- list(
            timestamp = result$update_data$timestamp,
            open = result$update_data$open,
            high = result$update_data$high,
            low = result$update_data$low,
            close = result$update_data$close,
            volume = result$update_data$volume,
            oint = result$update_data$oint
          )
        }
        new_data <- convert_to_frontend_format(result$new_data)
        self$kline_cache[[instrument_id]] <- c(cache, new_data)
      }

      self$last_update_times[[instrument_id]] <- Sys.time()
    },

    clear_instrument_cache = function(instrument_id) {
      if (!is.null(instrument_id)) {
        self$kline_cache[[instrument_id]] <- NULL
        self$tick_counts[[instrument_id]] <- 0
        self$last_update_times[[instrument_id]] <- NULL
      }
    },

    clear_all_cache = function() {
      self$kline_cache <- list()
      self$tick_counts <- list()
      self$last_update_times <- list()
      self$current_instrument <- NULL
    },

    set_current = function(instrument_id, period = NULL) {
      self$current_instrument <- instrument_id
      if (!is.null(period)) {
        self$current_period <- period
      }
    },

    get_current_cache = function() {
      self$get_cache(self$current_instrument)
    },

    needs_refresh = function(instrument_id, max_age_seconds = 30) {
      last_update <- self$last_update_times[[instrument_id]]
      if (is.null(last_update)) return(TRUE)
      as.numeric(Sys.time() - last_update, units = "secs") > max_age_seconds
    }
  )
)

# ============ UI模块 ============

ui <- fluidPage(
  tags$head(
    tags$style(HTML("
      body {
        background-color: #1e1e2f;
        color: #e0e0e0;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }
      .title-panel {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 15px;
        border-radius: 8px;
        margin-bottom: 20px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.3);
      }
      .title-panel h2 {
        margin: 0;
        color: white;
        text-align: center;
        font-weight: 600;
      }
      .sidebar-panel {
        background-color: #2d2d3f;
        border-radius: 8px;
        padding: 20px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.2);
      }
      .control-group {
        margin-bottom: 20px;
      }
      .control-group label {
        color: #a0aec0;
        font-weight: 600;
        font-size: 12px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        margin-bottom: 8px;
        display: block;
      }
      .btn-primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;
        font-weight: 600;
        transition: all 0.3s ease;
      }
      .btn-primary:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
      }
      .btn-danger {
        background: linear-gradient(135deg, #fc8181 0%, #e53e3e 100%);
        border: none;
        font-weight: 600;
      }
      .info-box {
        background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
        border-radius: 8px;
        padding: 15px;
        margin-bottom: 20px;
        border: 1px solid #4a5568;
      }
      .info-box h4 {
        margin: 0 0 12px 0;
        color: #667eea;
        font-size: 14px;
        font-weight: 600;
      }
      .status-indicator {
        display: inline-block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 8px;
      }
      .status-connected { background-color: #48bb78; box-shadow: 0 0 8px #48bb78; }
      .status-disconnected { background-color: #fc8181; }
      .debug-panel {
        background-color: #1a1a2e;
        border: 1px solid #4a5568;
        border-radius: 8px;
        padding: 12px;
        font-family: 'Fira Code', 'Consolas', monospace;
        font-size: 11px;
        max-height: 250px;
        overflow-y: auto;
        color: #a0aec0;
      }
      .debug-entry {
        margin-bottom: 4px;
        padding: 2px 0;
        border-bottom: 1px solid #2d3748;
      }
      .debug-timestamp { color: #667eea; }
      .shiny-input-container { margin-bottom: 0; }
    "))
  ),

  div(class = "title-panel", 
      titlePanel("CTP 实时K线图 - 修复版 v2.2")),

  sidebarLayout(
    sidebarPanel(
      width = 3,
      class = "sidebar-panel",

      # 连接状态显示
      div(class = "info-box",
        h4("连接状态"),
        div(
          style = "display: flex; align-items: center; justify-content: space-between;",
          uiOutput("connection_status_ui", inline = TRUE),
          textOutput("active_instrument_display", inline = TRUE)
        )
      ),

      # 实时价格信息
      div(class = "info-box",
        h4("实时行情"),
        verbatimTextOutput("price_info", placeholder = TRUE)
      ),

      div(class = "control-group",
        tags$label("服务器配置"),
        textInput("host", "服务器地址", value = "192.168.1.41"),
        numericInput("port", "端口", value = 9898, min = 1, max = 65535),
        div(style = "display: grid; grid-template-columns: 1fr 1fr; gap: 8px;",
          actionButton("connect_btn", "启动", class = "btn-primary", icon = icon("play")),
          actionButton("disconnect_btn", "停止", class = "btn-danger", icon = icon("stop"))
        )
      ),

      div(class = "control-group",
        tags$label("推送设置"),
        sliderInput("push_interval", "推送周期(秒)", 
                   min = 0.1, max = 5, value = 1, step = 0.1),
        helpText("默认1秒，数值越小刷新越频繁", style = "color: #718096; font-size: 11px;")
      ),

      div(class = "control-group",
        tags$label("合约选择"),
        selectInput("instrument", "合约代码", choices = NULL),
        div(style = "margin-top: 8px;",
          actionButton("refresh_btn", "强制刷新", icon = icon("sync"), 
                      class = "btn-primary", style = "width: 100%;")
        )
      ),

      div(class = "control-group",
        tags$label("K线设置"),
        numericInput("kline_period", "周期(分钟)", value = 1, min = 1, max = 1440, step = 1)
      ),

      div(class = "debug-panel",
        h4(style = "margin-top: 0; color: #667eea; font-size: 12px;", "调试日志"),
        uiOutput("debug_info")
      ),

      actionButton("exit", "退出应用", icon = icon("power-off"), 
                  class = "btn-danger", style = "width: 100%; margin-top: 15px;")
    ),

    mainPanel(
      width = 9,
      div(id = "chart", style = "width:100%; height:800px; background-color: #1e1e2f; border: 1px solid #4a5568; border-radius: 8px; box-shadow: 0 8px 16px rgba(0,0,0,0.3);"),
      tags$script(src = "js/klinecharts.min.js"),
      tags$script(src = "js/chartapp.js")
    )
  )
)

# ============ 服务器模块 ============

server <- function(input, output, session) {

  # 使用R6状态管理器
  state <- InstrumentStateManager$new()

  # 基础状态 - 使用reactiveVal确保UI更新
  ctp_client <- NULL
  is_connected <- reactiveVal(FALSE)
  available_instruments <- character()
  running <- TRUE
  debug_messages <- character()

  # 定时器句柄
  timers <- list(instrument = NULL, price = NULL, kline = NULL)

  # 调试日志函数
  add_debug <- function(msg) {
    timestamp <- format(Sys.time(), "%H:%M:%S.%OS3")
    debug_msg <- paste0("<div class='debug-entry'><span class='debug-timestamp'>[", timestamp, "]</span> ", msg, "</div>")
    debug_messages <<- c(debug_msg, debug_messages)[1:50]
    output$debug_info <- renderUI({
      HTML(paste(rev(debug_messages), collapse = ""))
    })
    message(paste0("[", timestamp, "] ", msg))
  }

  # 安全通知
  show_notification <- function(message, type = "default", duration = 3) {
    tryCatch({
      showNotification(message, type = type, duration = duration)
    }, error = function(e) {
      message("通知失败: ", message)
    })
  }

  # 连接状态显示
  output$connection_status_ui <- renderUI({
    if (is_connected()) {
      HTML(paste0(
        "<span class='status-indicator status-connected'></span>",
        "<span style='color: #48bb78; font-weight: 600;'>已连接</span>"
      ))
    } else {
      HTML(paste0(
        "<span class='status-indicator status-disconnected'></span>",
        "<span style='color: #fc8181;'>未连接</span>"
      ))
    }
  })

  # 当前合约显示
  output$active_instrument_display <- renderText({
    req(state$current_instrument)
    paste0("当前: ", state$current_instrument)
  })

  # ============ 关键修复：正确的清理顺序 ============

  # 步骤1：发送 undump 命令到服务器（必须在 stop 之前）
  send_undump_command <- function() {
    if (is.null(ctp_client)) return(FALSE)

    tryCatch({
      if (!is.null(ctp_client$.sessionfd) && !is.null(ctp_client$.conn)) {
        undump_cmd <- sprintf("undump %s", ctp_client$.sessionfd)
        add_debug(paste0("发送 undump 命令: ", undump_cmd))

        writeLines(undump_cmd, ctp_client$.conn)

        # 读取并丢弃返回数据（等待服务器确认）
        response <- base::readLines(ctp_client$.conn, n = 1, warn = FALSE)
        add_debug(paste0("undump 响应: ", paste(response, collapse = ", ")))

        Sys.sleep(0.1)  # 给服务器一点时间处理
        return(TRUE)
      }
    }, error = function(e) {
      add_debug(paste0("发送 undump 失败: ", e$message))
      return(FALSE)
    })
    return(FALSE)
  }

  # 步骤2：停止客户端连接
  stop_client <- function() {
    if (is.null(ctp_client)) return()

    tryCatch({
      ctp_client$stop()
      add_debug("CTP客户端已停止")
    }, error = function(e) {
      add_debug(paste0("停止客户端出错: ", e$message))
    })
  }

  # 完整的清理流程
  reset_env <- function(full_reset = TRUE) {
    add_debug("开始完整清理流程...")

    # 关键顺序：1.发送undump -> 2.停止客户端 -> 3.清理变量
    if (!is.null(ctp_client)) {
      send_undump_command()  # 先通知服务器停止写入
      stop_client()          # 再停止本地连接
    }

    ctp_client <<- NULL
    is_connected(FALSE)  # 更新UI状态
    available_instruments <<- character()

    if (full_reset) {
      state$clear_all_cache()
      add_debug("所有缓存已清除")
    }

    add_debug("环境清理完成")
  }

  # 停止所有定时器
  stop_timers <- function() {
    for (name in names(timers)) {
      if (!is.null(timers[[name]])) {
        timers[[name]] <- NULL
      }
    }
    add_debug("所有定时器已停止")
  }

  # ============ K线更新核心逻辑 ============

  get_incremental_kline <- function(ticks_df, period_minutes, instrument_id) {
    if (is.null(ticks_df) || nrow(ticks_df) == 0) {
      return(NULL)
    }

    base_kline <- state$get_cache(instrument_id)
    full_kline <- ticks_to_kline(ticks_df, period_minutes, base_kline)

    if (is.null(full_kline) || length(full_kline$timestamp) == 0) {
      return(NULL)
    }

    if (is.null(base_kline) || length(base_kline$timestamp) == 0) {
      return(list(
        type = "full",
        instrument = instrument_id,
        data = full_kline,
        last_timestamp = max(full_kline$timestamp)
      ))
    }

    cached_last_ts <- max(base_kline$timestamp)
    update_indices <- which(full_kline$timestamp >= cached_last_ts)

    if (length(update_indices) == 0) {
      return(NULL)
    }

    if (full_kline$timestamp[update_indices[1]] == cached_last_ts) {
      new_last <- list(
        timestamp = full_kline$timestamp[update_indices[1]],
        open = full_kline$open[update_indices[1]],
        high = full_kline$high[update_indices[1]],
        low = full_kline$low[update_indices[1]],
        close = full_kline$close[update_indices[1]],
        volume = full_kline$volume[update_indices[1]],
        oint = full_kline$oint[update_indices[1]]
      )

      if (length(update_indices) > 1) {
        new_indices <- update_indices[-1]
        new_kline <- list(
          timestamp = full_kline$timestamp[new_indices],
          open = full_kline$open[new_indices],
          high = full_kline$high[new_indices],
          low = full_kline$low[new_indices],
          close = full_kline$close[new_indices],
          volume = full_kline$volume[new_indices],
          oint = full_kline$oint[new_indices]
        )

        return(list(
          type = "update_and_incremental",
          instrument = instrument_id,
          update_data = new_last,
          new_data = new_kline,
          last_timestamp = max(full_kline$timestamp)
        ))
      }

      return(list(
        type = "update",
        instrument = instrument_id,
        data = new_last,
        last_timestamp = max(full_kline$timestamp)
      ))

    } else {
      new_indices <- update_indices
      new_kline <- list(
        timestamp = full_kline$timestamp[new_indices],
        open = full_kline$open[new_indices],
        high = full_kline$high[new_indices],
        low = full_kline$low[new_indices],
        close = full_kline$close[new_indices],
        volume = full_kline$volume[new_indices],
        oint = full_kline$oint[new_indices]
      )

      return(list(
        type = "incremental",
        instrument = instrument_id,
        data = new_kline,
        last_timestamp = max(full_kline$timestamp)
      ))
    }
  }

  send_kline_to_frontend <- function(result) {
    if (is.null(result)) return()

    instrument <- result$instrument
    type <- result$type

    send_data <- switch(type,
      "full" = list(
        instrument = instrument,
        type = "full",
        ds = convert_to_frontend_format(result$data)
      ),
      "incremental" = list(
        instrument = instrument,
        type = "incremental", 
        ds = convert_to_frontend_format(result$data)
      ),
      "update" = list(
        instrument = instrument,
        type = "update",
        ds = list(
          timestamp = result$data$timestamp,
          open = result$data$open,
          high = result$data$high,
          low = result$data$low,
          close = result$data$close,
          volume = result$data$volume,
          oint = result$data$oint
        )
      ),
      "update_and_incremental" = list(
        instrument = instrument,
        type = "update_and_incremental",
        update_data = list(
          timestamp = result$update_data$timestamp,
          open = result$update_data$open,
          high = result$update_data$high,
          low = result$update_data$low,
          close = result$update_data$close,
          volume = result$update_data$volume,
          oint = result$update_data$oint
        ),
        new_data = convert_to_frontend_format(result$new_data)
      ),
      NULL
    )

    if (!is.null(send_data)) {
      state$update_cache(instrument, result)

      if (instrument == state$current_instrument) {
        session$sendCustomMessage("push", send_data)
        add_debug(paste0("发送[", type, "]到前端: ", instrument, ", K线数: ", 
                        if(type == "update") "1" else length(send_data$ds)))
      }
    }
  }

  # ============ 定时器回调 ============

  start_kline_updater <- function() {
    update_kline <- function() {
      if (!running || is.null(ctp_client)) return()

      current <- state$current_instrument
      period <- state$current_period

      if (is.null(current) || current == "") {
        timers$kline <<- later::later(update_kline, 1)
        return()
      }

      tryCatch({
        ticks_df <- ctp_client$ticks(current)

        if (!is.null(ticks_df) && nrow(ticks_df) > 0) {
          current_count <- nrow(ticks_df)
          last_count <- state$tick_counts[[current]] %||% 0

          if (current_count != last_count) {
            state$tick_counts[[current]] <- current_count

            result <- get_incremental_kline(ticks_df, period, current)
            send_kline_to_frontend(result)
          }
        }
      }, error = function(e) {
        add_debug(paste0("K线更新错误: ", e$message))
      })

      push_interval <- isolate(input$push_interval) %||% 1
      if (running && !is.null(ctp_client)) {
        timers$kline <<- later::later(update_kline, push_interval)
      }
    }

    add_debug(paste0("K线更新器已启动，推送周期: ", input$push_interval, "秒"))
    update_kline()
  }

  start_price_monitor <- function() {
    monitor_prices <- function() {
      if (!running || is.null(ctp_client)) return()

      current <- state$current_instrument
      if (is.null(current) || current == "") {
        timers$price <<- later::later(monitor_prices, 1)
        return()
      }

      tryCatch({
        stats <- ctp_client$stats(current)
        if (!is.null(stats) && is.list(stats)) {
          output$price_info <- renderPrint({
            cat("合约: ", current, "\n")
            cat("最新价: ", sprintf("%.2f", stats$last), "\n")
            cat("均价:   ", sprintf("%.2f", stats$mean), "\n")
            cat("最高:   ", sprintf("%.2f", stats$max), "\n")
            cat("最低:   ", sprintf("%.2f", stats$min), "\n")
            cat("Tick数: ", stats$n, "\n")
            cat("时间:   ", stats$updatetime, "\n")
          })
        }
      }, error = function(e) {
        # 静默处理
      })

      if (running && !is.null(ctp_client)) {
        timers$price <<- later::later(monitor_prices, 1)
      }
    }

    monitor_prices()
  }

  start_instrument_updater <- function() {
    update_instruments <- function() {
      if (!running || is.null(ctp_client)) return()

      tryCatch({
        instr <- if (!is.null(ctp_client$instrumentids)) {
          ctp_client$instrumentids
        } else {
          character()
        }

        if (length(instr) > 0 && !identical(sort(instr), sort(available_instruments))) {
          available_instruments <<- instr

          current_selection <- isolate(input$instrument)
          new_selection <- if (!is.null(current_selection) && current_selection %in% instr) {
            current_selection
          } else {
            instr[1]
          }

          updateSelectInput(session, "instrument", choices = instr, selected = new_selection)
          add_debug(paste0("合约列表更新: ", length(instr), " 个合约"))
        }
      }, error = function(e) {
        add_debug(paste0("获取合约列表错误: ", e$message))
      })

      if (running && !is.null(ctp_client)) {
        timers$instrument <<- later::later(update_instruments, 5)
      }
    }

    update_instruments()
  }

  # ============ 事件处理器 ============

  observeEvent(input$instrument, {
    new_instrument <- input$instrument
    if (is.null(new_instrument) || new_instrument == "") return()

    old_instrument <- state$current_instrument

    if (!identical(new_instrument, old_instrument)) {
      add_debug(paste0("合约切换: ", old_instrument, " -> ", new_instrument))

      state$set_current(new_instrument, input$kline_period)
      state$tick_counts[[new_instrument]] <- 0

      session$sendCustomMessage("switchInstrument", list(
        instrument = new_instrument,
        from_instrument = old_instrument,
        clearCache = FALSE
      ))

      show_notification(paste0("已切换到: ", new_instrument), "message", 2)
    }
  }, ignoreNULL = TRUE, ignoreInit = FALSE)

  observeEvent(input$kline_period, {
    new_period <- input$kline_period
    if (!is.null(new_period) && !identical(new_period, state$current_period)) {
      add_debug(paste0("周期变更: ", state$current_period, " -> ", new_period, " 分钟"))
      state$current_period <- new_period

      current <- state$current_instrument
      if (!is.null(current)) {
        state$clear_instrument_cache(current)
        state$tick_counts[[current]] <- 0

        session$sendCustomMessage("clearChartForInstrument", list(
          instrument = current,
          reason = "period_change"
        ))
      }

      show_notification(paste0("K线周期已更改为: ", new_period, " 分钟"), "message", 2)
    }
  })

  observeEvent(input$push_interval, {
    add_debug(paste0("推送周期已设置为: ", input$push_interval, " 秒"))
  })

  # 启动连接
  observeEvent(input$connect_btn, {
    add_debug("正在启动CTP连接...")

    if (!is.null(ctp_client)) {
      showModal(modalDialog(
        title = "确认重连",
        "当前已有连接，是否重新启动？",
        easyClose = FALSE,
        footer = tagList(
          modalButton("取消"),
          actionButton("confirm_reconnect", "确认", class = "btn-primary")
        )
      ))
      return()
    }

    do_connect()
  })

  observeEvent(input$confirm_reconnect, {
    removeModal()
    do_connect()
  })

  do_connect <- function() {
    stop_timers()
    running <<- TRUE

    tryCatch({
      if (!exists("ctpd_async")) {
        stop("ctpd_async 函数未定义，请确保已加载 ctpd.R 文件")
      }

      client <- ctpd_async(host = input$host, port = input$port)
      ctp_client <<- client
      is_connected(TRUE)

      add_debug("CTP连接成功建立")
      show_notification("连接成功", "message", 3)

      start_instrument_updater()
      start_kline_updater()
      start_price_monitor()

    }, error = function(e) {
      is_connected(FALSE)
      add_debug(paste0("连接失败: ", e$message))
      showModal(modalDialog(
        title = "连接失败",
        paste("无法连接到服务器:", e$message),
        easyClose = TRUE,
        footer = modalButton("关闭")
      ))
    })
  }

  # ============ 关键修复：正确的停止顺序 ============
  observeEvent(input$disconnect_btn, {
    if (!is.null(ctp_client)) {
      add_debug("正在停止CTP连接...")
      running <<- FALSE
      stop_timers()

      # 关键：先通知前端重置，再执行清理
      session$sendCustomMessage("resetAll", list(
        reason = "disconnect",
        timestamp = as.numeric(Sys.time())
      ))

      # 关键：正确的清理顺序
      reset_env(full_reset = TRUE)

      output$price_info <- renderPrint({
        cat("CTP客户端未启动\n")
        cat("请点击'启动'按钮连接\n")
      })

      show_notification("连接已停止，资源已释放", "message", 3)
    } else {
      show_notification("当前没有活动连接", "warning", 3)
    }
  })

  observeEvent(input$refresh_btn, {
    current <- state$current_instrument
    if (!is.null(current) && !is.null(ctp_client)) {
      add_debug(paste0("强制刷新合约: ", current))

      state$clear_instrument_cache(current)
      state$tick_counts[[current]] <- 0

      session$sendCustomMessage("clearChartForInstrument", list(
        instrument = current,
        reason = "manual_refresh"
      ))

      show_notification("正在刷新数据...", "message", 2)
    }
  })

  observeEvent(input$exit, {
    showModal(modalDialog(
      title = "确认退出",
      "确定要退出应用程序吗？",
      easyClose = TRUE,
      footer = tagList(
        modalButton("取消"),
        actionButton("confirm_exit", "确定退出", class = "btn-danger")
      )
    ))
  })

  observeEvent(input$confirm_exit, {
    running <<- FALSE
    stop_timers()
    reset_env()
    stopApp()
  })

  session$onSessionEnded(function() {
    running <<- FALSE
    stop_timers()
    # 会话结束时也要正确清理
    if (!is.null(ctp_client)) {
      send_undump_command()
      stop_client()
    }
  })

  # 初始化
  output$price_info <- renderPrint({
    cat("等待连接...\n")
    cat("请点击'启动'按钮\n")
  })
}

# ============ 创建前端文件 ============

create_frontend_files <- function() {
  if (!dir.exists("www")) dir.create("www")
  if (!dir.exists("www/js")) dir.create("www/js")
  if (!dir.exists("www/css")) dir.create("www/css")

  writeLines('
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
.shiny-notification { position: fixed; top: 20px; right: 20px; z-index: 9999; }
', "www/css/style.css")

  writeLines('
// chartapp.js - 修复版v2.2
(function () {
  "use strict";

  const CONFIG = {
    debug: true,
    maxCacheAge: 300000,
    chartHeight: 800
  };

  const State = {
    chart: null,
    currentInstrument: null,
    previousInstrument: null,
    instrumentCache: new Map(),
    dataVersion: 0,
    isSwitching: false,
    pendingUpdates: new Map(),
    isRunning: false
  };

  function debugLog(msg, data) {
    if (!CONFIG.debug) return;
    const timestamp = new Date().toLocaleTimeString();
    const prefix = `[${timestamp}] [ChartApp]`;
    if (data !== undefined) {
      console.log(prefix, msg, data);
    } else {
      console.log(prefix, msg);
    }
  }

  function errorLog(msg, error) {
    const timestamp = new Date().toLocaleTimeString();
    console.error(`[${timestamp}] [ChartApp] ERROR:`, msg, error);
  }

  function deepClone(obj) {
    if (obj === null || typeof obj !== "object") return obj;
    if (obj instanceof Date) return new Date(obj.getTime());
    if (Array.isArray(obj)) return obj.map(deepClone);
    const cloned = {};
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        cloned[key] = deepClone(obj[key]);
      }
    }
    return cloned;
  }

  function validateKlineData(data) {
    if (!data || !Array.isArray(data)) return false;
    if (data.length === 0) return true;

    const required = ["timestamp", "open", "high", "low", "close"];
    const first = data[0];
    return required.every(field => field in first && typeof first[field] === "number");
  }

  const CacheManager = {
    get(instrumentId) {
      return State.instrumentCache.get(instrumentId);
    },

    set(instrumentId, data, type = "full") {
      if (!validateKlineData(data)) {
        errorLog("尝试缓存无效数据", { instrumentId, data });
        return;
      }

      const existing = State.instrumentCache.get(instrumentId);
      let newData;

      if (type === "full" || !existing) {
        newData = deepClone(data);
      } else if (type === "incremental") {
        newData = [...existing.data, ...deepClone(data)];
        const seen = new Map();
        newData.forEach(k => seen.set(k.timestamp, k));
        newData = Array.from(seen.values()).sort((a, b) => a.timestamp - b.timestamp);
      } else if (type === "update") {
        newData = [...existing.data];
        if (newData.length > 0) {
          const lastIdx = newData.length - 1;
          if (newData[lastIdx].timestamp === data.timestamp) {
            newData[lastIdx] = deepClone(data);
          }
        }
      }

      State.instrumentCache.set(instrumentId, {
        data: newData,
        lastUpdate: Date.now(),
        version: (existing?.version || 0) + 1
      });

      debugLog(`缓存[${type}]更新: ${instrumentId}, K线数: ${newData.length}`);
    },

    clear(instrumentId) {
      if (State.instrumentCache.has(instrumentId)) {
        State.instrumentCache.delete(instrumentId);
        debugLog(`清除缓存: ${instrumentId}`);
      }
    },

    clearAll() {
      State.instrumentCache.clear();
      debugLog("清除所有缓存");
    },

    getStats() {
      const stats = [];
      State.instrumentCache.forEach((value, key) => {
        stats.push(`${key}: ${value.data.length}条(v${value.version})`);
      });
      return stats.join(", ");
    }
  };

  const ChartController = {
    init() {
      if (State.chart) {
        debugLog("图表已存在，跳过初始化");
        return;
      }

      try {
        State.chart = klinecharts.init("chart");

        State.chart.setStyles({
          grid: {
            horizontal: { color: "#2d2d3f", size: 1 },
            vertical: { color: "#2d2d3f", size: 1 }
          },
          candle: {
            candle: {
              bar: { 
                upColor: "#ef5350", 
                downColor: "#26a69a", 
                noChangeColor: "#888888" 
              }
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

        this.registerIndicators();
        this.createIndicators();

        debugLog("图表初始化完成");
      } catch (e) {
        errorLog("图表初始化失败", e);
      }
    },

    registerIndicators() {
      klinecharts.registerIndicator({
        name: "OINT",
        shortName: "OINT",
        calcParams: [],
        shouldFormatBigNumber: true,
        figures: [
          { key: "oint", title: "持仓量: ", type: "line" },
          { key: "preoint", title: "前仓量: ", type: "line" }
        ],
        calc: function(kLineDataList) {
          return kLineDataList.map(function(k, i, ks) {
            return {
              oint: k.oint || 0,
              preoint: i < 1 ? (k.oint || 0) : (ks[i - 1].oint || 0)
            };
          });
        }
      });
    },

    createIndicators() {
      if (!State.chart) return;

      try {
        State.chart.createIndicator("VOL", false);
        State.chart.createIndicator("MA", true, { id: "candle_pane" });
        State.chart.createIndicator("OINT", false);
        State.chart.createIndicator("KDJ", false);
        State.chart.createIndicator("MACD", false);
        debugLog("指标创建完成");
      } catch (e) {
        errorLog("创建指标失败", e);
      }
    },

    loadData(data, type) {
      if (!State.chart || !data) {
        errorLog("无法加载数据：图表或数据无效");
        return false;
      }

      if (!validateKlineData(type === "update" ? [data] : data)) {
        errorLog("数据格式无效", data);
        return false;
      }

      try {
        switch (type) {
          case "full":
            State.chart.applyNewData(data);
            debugLog("全量数据加载完成", { count: data.length });
            break;

          case "incremental":
            data.forEach(bar => State.chart.updateData(bar));
            debugLog("增量数据添加完成", { count: data.length });
            break;

          case "update":
            State.chart.updateData(data);
            debugLog("最后一根K线已更新", { timestamp: data.timestamp });
            break;

          default:
            errorLog("未知数据类型", type);
            return false;
        }
        return true;
      } catch (e) {
        errorLog(`加载数据失败[${type}]`, e);
        return false;
      }
    },

    clear() {
      if (!State.chart) return;
      try {
        State.chart.applyNewData([], true);
        debugLog("图表已清空");
      } catch (e) {
        errorLog("清空图表失败", e);
      }
    },

    getCurrentData() {
      if (!State.chart) return [];
      try {
        return State.chart.getDataList() || [];
      } catch (e) {
        return [];
      }
    },

    destroy() {
      if (!State.chart) return;
      try {
        klinecharts.dispose("chart");
        State.chart = null;
        debugLog("图表已销毁");
      } catch (e) {
        errorLog("销毁图表失败", e);
      }
    }
  };

  const InstrumentSwitcher = {
    switchTo(newInstrument, fromInstrument) {
      if (State.isSwitching) {
        debugLog("切换进行中，忽略重复请求");
        return;
      }

      if (!newInstrument) {
        errorLog("目标合约无效");
        return;
      }

      if (newInstrument === State.currentInstrument) {
        debugLog("合约未变化，跳过切换");
        return;
      }

      State.isSwitching = true;
      State.previousInstrument = fromInstrument || State.currentInstrument;
      State.currentInstrument = newInstrument;

      debugLog(`开始切换合约: ${State.previousInstrument} -> ${newInstrument}`);

      try {
        if (State.previousInstrument) {
          const currentData = ChartController.getCurrentData();
          if (currentData && currentData.length > 0) {
            CacheManager.set(State.previousInstrument, currentData, "full");
            debugLog(`保存当前合约数据: ${State.previousInstrument}, ${currentData.length}条`);
          }
        }

        ChartController.clear();

        const cached = CacheManager.get(newInstrument);
        if (cached && cached.data.length > 0) {
          debugLog(`从缓存恢复: ${newInstrument}, ${cached.data.length}条`);

          if (validateKlineData(cached.data)) {
            const success = ChartController.loadData(cached.data, "full");
            if (success) {
              debugLog("缓存恢复成功");
            } else {
              errorLog("缓存恢复失败");
            }
          } else {
            errorLog("缓存数据无效");
            CacheManager.clear(newInstrument);
          }
        } else {
          debugLog(`无缓存，等待推送: ${newInstrument}`);
        }

        this.applyPendingUpdates(newInstrument);

      } catch (e) {
        errorLog("合约切换失败", e);
      } finally {
        State.isSwitching = false;
        debugLog("合约切换完成");
      }
    },

    applyPendingUpdates(instrumentId) {
      const pending = State.pendingUpdates.get(instrumentId);
      if (!pending || pending.length === 0) return;

      debugLog(`应用暂存更新: ${instrumentId}, ${pending.length}条`);

      pending.forEach(update => {
        this.handleDataUpdate(update, true);
      });

      State.pendingUpdates.delete(instrumentId);
    },

    handleDataUpdate(data, isPending = false) {
      const instrument = data.instrument;
      const type = data.type;

      if (!instrument || !type) {
        errorLog("数据更新格式无效", data);
        return;
      }

      if (instrument !== State.currentInstrument) {
        debugLog(`非当前合约，仅缓存: ${instrument}`);

        if (type === "full" && data.ds) {
          CacheManager.set(instrument, data.ds, "full");
        } else if (type === "incremental" && data.ds) {
          CacheManager.set(instrument, data.ds, "incremental");
        } else if (type === "update" && data.ds) {
          CacheManager.set(instrument, [data.ds], "update");
        }
        return;
      }

      if (State.isSwitching && !isPending) {
        debugLog(`切换中，暂存: ${instrument}`);
        if (!State.pendingUpdates.has(instrument)) {
          State.pendingUpdates.set(instrument, []);
        }
        State.pendingUpdates.get(instrument).push(data);
        return;
      }

      try {
        switch (type) {
          case "full":
            if (data.ds && ChartController.loadData(data.ds, "full")) {
              CacheManager.set(instrument, data.ds, "full");
            }
            break;

          case "incremental":
            if (data.ds && ChartController.loadData(data.ds, "incremental")) {
              CacheManager.set(instrument, data.ds, "incremental");
            }
            break;

          case "update":
            if (data.ds && ChartController.loadData(data.ds, "update")) {
              CacheManager.set(instrument, [data.ds], "update");
            }
            break;

          case "update_and_incremental":
            if (data.update_data) {
              ChartController.loadData(data.update_data, "update");
            }
            if (data.new_data && ChartController.loadData(data.new_data, "incremental")) {
              const existing = CacheManager.get(instrument);
              if (existing) {
                const combined = [...existing.data];
                if (combined.length > 0) combined.pop();
                combined.push(data.update_data);
                combined.push(...data.new_data);
                CacheManager.set(instrument, combined, "full");
              }
            }
            break;
        }
      } catch (e) {
        errorLog("处理数据更新失败", e);
      }
    }
  };

  function resetAllState(reason) {
    debugLog(`执行完全重置，原因: ${reason}`);

    ChartController.destroy();
    CacheManager.clearAll();

    State.currentInstrument = null;
    State.previousInstrument = null;
    State.isSwitching = false;
    State.pendingUpdates.clear();
    State.dataVersion = 0;
    State.isRunning = false;

    setTimeout(() => {
      ChartController.init();
      debugLog("状态已完全重置，图表重新初始化");
    }, 100);
  }

  function initShinyHandlers() {
    if (typeof Shiny === "undefined") {
      console.error("Shiny 未加载");
      return;
    }

    Shiny.addCustomMessageHandler("push", function(data) {
      debugLog("收到推送", { instrument: data.instrument, type: data.type });
      InstrumentSwitcher.handleDataUpdate(data);
    });

    Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
      debugLog("收到切换指令", msg);
      InstrumentSwitcher.switchTo(msg.instrument, msg.from_instrument);
    });

    Shiny.addCustomMessageHandler("clearChartForInstrument", function(msg) {
      debugLog("收到清空指令", msg);
      if (msg.instrument === State.currentInstrument) {
        ChartController.clear();
        CacheManager.clear(msg.instrument);
      }
    });

    Shiny.addCustomMessageHandler("clearAll", function() {
      debugLog("收到清空所有指令");
      ChartController.clear();
      CacheManager.clearAll();
      State.currentInstrument = null;
      State.previousInstrument = null;
    });

    Shiny.addCustomMessageHandler("resetAll", function(msg) {
      debugLog("收到完全重置指令", msg);
      resetAllState(msg.reason || "unknown");
    });

    debugLog("Shiny消息处理器已注册");
  }

  function init() {
    debugLog("应用初始化开始");

    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", init);
      return;
    }

    if (typeof klinecharts === "undefined") {
      console.error("klinecharts 库未加载");
      setTimeout(init, 100);
      return;
    }

    ChartController.init();
    initShinyHandlers();
    State.isRunning = true;

    window.addEventListener("beforeunload", function() {
      CacheManager.clearAll();
      if (State.chart) {
        try {
          klinecharts.dispose("chart");
        } catch (e) {}
      }
    });

    debugLog("应用初始化完成");
  }

  init();

})();
', "www/js/chartapp.js")

  message("前端文件创建完成")
}

create_frontend_files()

shinyApp(ui, server)