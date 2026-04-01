# app.R - 重构版：修复合约切换 A->B->A 数据不一致问题

# 加载必要的包
required_packages <- c("later", "shiny", "shinydashboard", "dplyr", "jsonlite", "lubridate", "data.table", "purrr", "R6")
for(pkg in required_packages) {
  if(!require(pkg, character.only = TRUE)) {
    install.packages(pkg)
    library(pkg, character.only = TRUE)
  }
}

# ============ 工具函数模块 ============

#' 向量化时间戳计算
floor_date_vectorized <- function(datetime, period_seconds) {
  if (length(datetime) == 0) return(datetime)
  timestamp <- as.numeric(datetime)
  floor_timestamp <- floor(timestamp / period_seconds) * period_seconds
  as.POSIXct(floor_timestamp, origin = "1970-01-01")
}

#' 使用 data.table 高性能聚合K线
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

#' 使用 dplyr 聚合K线（备用）
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

#' 合并K线数据（函数式）
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

  # 去重保留最新的
  unique_indices <- !duplicated(all_fields$timestamp, fromLast = TRUE)
  lapply(all_fields, function(x) x[unique_indices])
}

#' 将K线列表转换为前端格式
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

#' 优化的 ticks_to_kline 函数 - 支持增量更新
ticks_to_kline <- function(ticks_df, period_minutes = 1, base_kline = NULL) {
  if (is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)

  required_cols <- c("ActionDay", "UpdateTime", "LastPrice", "Volume")
  if (!all(required_cols %in% colnames(ticks_df))) return(NULL)

  # 向量化时间戳创建
  datetime_str <- paste(ticks_df$ActionDay, ticks_df$UpdateTime)
  ticks_df$DateTime <- as.POSIXct(datetime_str, format = "%Y%m%d %H:%M:%S")

  if ("UpdateMillisec" %in% colnames(ticks_df)) {
    ticks_df$DateTime <- ticks_df$DateTime + ticks_df$UpdateMillisec / 1000
  }

  period_seconds <- period_minutes * 60
  ticks_df$Period <- floor_date_vectorized(ticks_df$DateTime, period_seconds)

  # 过滤已处理的数据（关键优化：避免重复计算）
  if (!is.null(base_kline) && length(base_kline$timestamp) > 0) {
    last_ts <- max(base_kline$timestamp) / 1000
    last_period <- floor_date_vectorized(
      as.POSIXct(last_ts, origin = "1970-01-01"), 
      period_seconds
    )

    # 只保留 >= 最后周期的数据（包括需要更新的最后一根K线）
    keep_indices <- ticks_df$Period >= last_period
    ticks_df <- ticks_df[keep_indices, ]

    if (nrow(ticks_df) == 0) return(NULL)
  }

  # 高性能聚合
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

#' R6类：合约状态管理器（解决多合约数据隔离问题）
InstrumentStateManager <- R6::R6Class(
  "InstrumentStateManager",

  public = list(
    # 每个合约的完整K线缓存
    kline_cache = list(),
    # 每个合约的tick计数（用于检测变化）
    tick_counts = list(),
    # 每个合约的最后更新时间
    last_update_times = list(),
    # 当前激活的合约
    current_instrument = NULL,
    # 当前周期
    current_period = 1,

    #' 获取合约缓存
    get_cache = function(instrument_id) {
      if (is.null(instrument_id)) return(NULL)
      cache <- self$kline_cache[[instrument_id]]
      if (is.null(cache)) return(NULL)

      # 转换为kline格式
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

    #' 更新缓存（支持全量/增量/更新三种模式）
    update_cache = function(instrument_id, result) {
      if (is.null(instrument_id)) return()

      if (result$type == "full") {
        # 全量替换
        self$kline_cache[[instrument_id]] <- convert_to_frontend_format(result$data)

      } else if (result$type == "incremental") {
        # 增量添加
        new_data <- convert_to_frontend_format(result$data)
        existing <- self$kline_cache[[instrument_id]]
        if (is.null(existing)) existing <- list()
        self$kline_cache[[instrument_id]] <- c(existing, new_data)

      } else if (result$type == "update") {
        # 更新最后一根
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
        # 更新最后一根 + 新增
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

      # 更新最后更新时间
      self$last_update_times[[instrument_id]] <- Sys.time()
    },

    #' 清除特定合约缓存
    clear_instrument_cache = function(instrument_id) {
      if (!is.null(instrument_id)) {
        self$kline_cache[[instrument_id]] <- NULL
        self$tick_counts[[instrument_id]] <- 0
        self$last_update_times[[instrument_id]] <- NULL
      }
    },

    #' 设置当前合约
    set_current = function(instrument_id, period = NULL) {
      self$current_instrument <- instrument_id
      if (!is.null(period)) {
        self$current_period <- period
      }
    },

    #' 获取当前合约缓存
    get_current_cache = function() {
      self$get_cache(self$current_instrument)
    },

    #' 检查是否需要刷新（缓存过期检测）
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
      titlePanel("CTP 实时K线图 - 重构版 v2.0")),

  sidebarLayout(
    sidebarPanel(
      width = 3,
      class = "sidebar-panel",

      # 连接状态显示
      div(class = "info-box",
        h4("连接状态"),
        div(
          style = "display: flex; align-items: center;",
          uiOutput("connection_status"),
          textOutput("active_instrument_display")
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

  # 使用R6状态管理器替代原始的环境变量
  state <- InstrumentStateManager$new()

  # 基础状态
  ctp_client <- NULL
  is_connected <- FALSE
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
  output$connection_status <- renderUI({
    if (is_connected) {
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

  output$active_instrument_display <- renderText({
    if (!is.null(state$current_instrument)) {
      paste0(" | 当前: ", state$current_instrument)
    } else {
      ""
    }
  })

  # 清理环境
  reset_env <- function() {
    if (!is.null(ctp_client)) {
      tryCatch({
        undump_cmd <- gettextf("undump %s", ctp_client$.sessionfd)
        writeLines(undump_cmd, ctp_client$.conn)
        ctp_client$stop()
        add_debug("CTP客户端已停止")
      }, error = function(e) {
        message("停止连接时出错: ", e$message)
      })
    }
    ctp_client <<- NULL
    is_connected <<- FALSE
    available_instruments <<- character()
    state$kline_cache <- list()
    state$tick_counts <- list()
    state$current_instrument <- NULL
    add_debug("环境已完全重置")
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

  #' 计算增量K线数据（关键修复：正确处理基准数据）
  get_incremental_kline <- function(ticks_df, period_minutes, instrument_id) {
    if (is.null(ticks_df) || nrow(ticks_df) == 0) {
      return(NULL)
    }

    # 获取当前合约的缓存作为基准
    base_kline <- state$get_cache(instrument_id)

    # 计算完整K线（包含历史+新增）
    full_kline <- ticks_to_kline(ticks_df, period_minutes, base_kline)

    if (is.null(full_kline) || length(full_kline$timestamp) == 0) {
      return(NULL)
    }

    # 首次加载：返回全量
    if (is.null(base_kline) || length(base_kline$timestamp) == 0) {
      return(list(
        type = "full",
        instrument = instrument_id,
        data = full_kline,
        last_timestamp = max(full_kline$timestamp)
      ))
    }

    # 比较新旧数据
    cached_last_ts <- max(base_kline$timestamp)
    update_indices <- which(full_kline$timestamp >= cached_last_ts)

    if (length(update_indices) == 0) {
      return(NULL)  # 无变化
    }

    # 检查第一根需要更新的K线是否与缓存最后时间戳相同
    if (full_kline$timestamp[update_indices[1]] == cached_last_ts) {
      # 需要更新最后一根K线
      new_last <- list(
        timestamp = full_kline$timestamp[update_indices[1]],
        open = full_kline$open[update_indices[1]],
        high = full_kline$high[update_indices[1]],
        low = full_kline$low[update_indices[1]],
        close = full_kline$close[update_indices[1]],
        volume = full_kline$volume[update_indices[1]],
        oint = full_kline$oint[update_indices[1]]
      )

      # 检查是否有更多新K线
      if (length(update_indices) > 1) {
        new_indices <- update_indices[-1]  # 排除第一个（已更新的）
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
      # 只有新增K线
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

  #' 发送K线数据到前端（关键修复：确保数据同步）
  send_kline_to_frontend <- function(result) {
    if (is.null(result)) return()

    instrument <- result$instrument
    type <- result$type

    # 构造发送数据
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
      # 更新服务器端缓存
      state$update_cache(instrument, result)

      # 发送到前端（只有当前合约才发送）
      if (instrument == state$current_instrument) {
        session$sendCustomMessage("push", send_data)
        add_debug(paste0("发送[", type, "]到前端: ", instrument, ", K线数: ", 
                        if(type == "update") "1" else length(send_data$ds)))
      }
    }
  }

  # ============ 定时器回调 ============

  #' K线更新定时器
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
          # 检测tick数量变化
          current_count <- nrow(ticks_df)
          last_count <- state$tick_counts[[current]] %||% 0

          if (current_count != last_count) {
            state$tick_counts[[current]] <- current_count

            # 计算并发送增量数据
            result <- get_incremental_kline(ticks_df, period, current)
            send_kline_to_frontend(result)
          }
        }
      }, error = function(e) {
        add_debug(paste0("K线更新错误: ", e$message))
      })

      if (running && !is.null(ctp_client)) {
        timers$kline <<- later::later(update_kline, 1)
      }
    }

    add_debug("K线更新器已启动")
    update_kline()
  }

  #' 价格监控定时器
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
        # 静默处理价格监控错误
      })

      if (running && !is.null(ctp_client)) {
        timers$price <<- later::later(monitor_prices, 1)
      }
    }

    monitor_prices()
  }

  #' 合约列表更新定时器
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

          # 保持当前选择，如果可用
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

  # 合约选择变化（关键修复：正确处理切换）
  observeEvent(input$instrument, {
    new_instrument <- input$instrument
    if (is.null(new_instrument) || new_instrument == "") return()

    old_instrument <- state$current_instrument

    # 如果切换到不同合约
    if (!identical(new_instrument, old_instrument)) {
      add_debug(paste0("合约切换: ", old_instrument, " -> ", new_instrument))

      # 更新当前合约
      state$set_current(new_instrument, input$kline_period)

      # 清除该合约的tick计数，强制刷新
      state$tick_counts[[new_instrument]] <- 0

      # 发送切换消息到前端（前端会处理缓存恢复）
      session$sendCustomMessage("switchInstrument", list(
        instrument = new_instrument,
        from_instrument = old_instrument,
        clearCache = FALSE  # 前端不清除，尝试从缓存恢复
      ))

      show_notification(paste0("已切换到: ", new_instrument), "message", 2)
    }
  }, ignoreNULL = TRUE, ignoreInit = FALSE)

  # K线周期变化
  observeEvent(input$kline_period, {
    new_period <- input$kline_period
    if (!is.null(new_period) && !identical(new_period, state$current_period)) {
      add_debug(paste0("周期变更: ", state$current_period, " -> ", new_period, " 分钟"))
      state$current_period <- new_period

      # 清除当前合约缓存，强制重新计算
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
      is_connected <<- TRUE

      add_debug("CTP连接成功建立")
      show_notification("连接成功", "message", 3)

      # 启动所有定时器
      start_instrument_updater()
      start_kline_updater()
      start_price_monitor()

    }, error = function(e) {
      add_debug(paste0("连接失败: ", e$message))
      showModal(modalDialog(
        title = "连接失败",
        paste("无法连接到服务器:", e$message),
        easyClose = TRUE,
        footer = modalButton("关闭")
      ))
    })
  }

  # 停止连接
  observeEvent(input$disconnect_btn, {
    if (!is.null(ctp_client)) {
      add_debug("正在停止CTP连接...")
      running <<- FALSE
      stop_timers()
      reset_env()

      output$price_info <- renderPrint({
        cat("CTP客户端未启动\n")
        cat("请点击'启动'按钮连接\n")
      })

      session$sendCustomMessage("clearAll", list())
      show_notification("连接已停止", "message", 3)
    } else {
      show_notification("当前没有活动连接", "warning", 3)
    }
  })

  # 强制刷新按钮
  observeEvent(input$refresh_btn, {
    current <- state$current_instrument
    if (!is.null(current) && !is.null(ctp_client)) {
      add_debug(paste0("强制刷新合约: ", current))

      # 清除服务器端缓存
      state$clear_instrument_cache(current)
      state$tick_counts[[current]] <- 0

      # 通知前端清除并重新加载
      session$sendCustomMessage("clearChartForInstrument", list(
        instrument = current,
        reason = "manual_refresh"
      ))

      show_notification("正在刷新数据...", "message", 2)
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

  # 会话结束清理
  session$onSessionEnded(function() {
    running <<- FALSE
    stop_timers()
    if (!is.null(ctp_client)) {
      tryCatch({
        ctp_client$stop()
      }, error = function(e) NULL)
    }
  })

  # 初始化价格信息显示
  output$price_info <- renderPrint({
    cat("等待连接...\n")
    cat("请点击'启动'按钮\n")
  })
}

# ============ 创建前端文件 ============

create_frontend_files <- function() {
  # 创建目录
  if (!dir.exists("www")) dir.create("www")
  if (!dir.exists("www/js")) dir.create("www/js")
  if (!dir.exists("www/css")) dir.create("www/css")

  # CSS文件
  writeLines('
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
.shiny-notification { position: fixed; top: 20px; right: 20px; z-index: 9999; }
', "www/css/style.css")

  # 重构后的 chartapp.js（关键修复：正确处理合约切换）
  writeLines('
// chartapp.js - 重构版：修复合约切换 A->B->A 数据不一致问题
(function () {
  "use strict";

  // ============ 配置与状态 ============
  const CONFIG = {
    debug: true,
    maxCacheAge: 300000,  // 缓存最大年龄：5分钟
    chartHeight: 800
  };

  // 状态管理
  const State = {
    chart: null,
    currentInstrument: null,
    previousInstrument: null,
    // 多合约数据缓存：{ instrumentId -> { data: [], lastUpdate: timestamp, version: number } }
    instrumentCache: new Map(),
    // 数据版本号（用于检测变更）
    dataVersion: 0,
    // 是否正在切换合约（防止并发操作）
    isSwitching: false,
    // 待处理的数据更新（切换期间暂存）
    pendingUpdates: new Map()
  };

  // ============ 工具函数 ============

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

  // 深拷贝数据（防止引用污染）
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

  // 验证K线数据格式
  function validateKlineData(data) {
    if (!data || !Array.isArray(data)) return false;
    if (data.length === 0) return true;

    const required = ["timestamp", "open", "high", "low", "close"];
    const first = data[0];
    return required.every(field => field in first && typeof first[field] === "number");
  }

  // ============ 缓存管理 ============

  const CacheManager = {
    // 获取缓存
    get(instrumentId) {
      return State.instrumentCache.get(instrumentId);
    },

    // 设置缓存（带版本控制）
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
        // 合并增量数据
        newData = [...existing.data, ...deepClone(data)];
        // 按时间戳去重（保留后出现的）
        const seen = new Map();
        newData.forEach(k => seen.set(k.timestamp, k));
        newData = Array.from(seen.values()).sort((a, b) => a.timestamp - b.timestamp);
      } else if (type === "update") {
        // 更新最后一根
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

      debugLog(`缓存[${type}]更新: ${instrumentId}, K线数: ${newData.length}, 版本: ${State.instrumentCache.get(instrumentId).version}`);
    },

    // 清除特定合约缓存
    clear(instrumentId) {
      if (State.instrumentCache.has(instrumentId)) {
        State.instrumentCache.delete(instrumentId);
        debugLog(`清除缓存: ${instrumentId}`);
      }
    },

    // 清除所有缓存
    clearAll() {
      State.instrumentCache.clear();
      debugLog("清除所有缓存");
    },

    // 获取缓存统计
    getStats() {
      const stats = [];
      State.instrumentCache.forEach((value, key) => {
        stats.push(`${key}: ${value.data.length}条(v${value.version})`);
      });
      return stats.join(", ");
    }
  };

  // ============ 图表操作 ============

  const ChartController = {
    // 初始化图表
    init() {
      if (State.chart) {
        debugLog("图表已存在，跳过初始化");
        return;
      }

      try {
        State.chart = klinecharts.init("chart");

        // 设置样式
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

        // 注册并创建指标
        this.registerIndicators();
        this.createIndicators();

        debugLog("图表初始化完成");
      } catch (e) {
        errorLog("图表初始化失败", e);
      }
    },

    // 注册自定义指标
    registerIndicators() {
      // 持仓量指标
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

    // 创建指标面板
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

    // 加载数据（关键修复：正确处理全量/增量）
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
            // 全量替换：使用 applyNewData（自动清空旧数据）
            State.chart.applyNewData(data);
            debugLog("全量数据加载完成", { count: data.length });
            break;

          case "incremental":
            // 增量添加：逐个使用 updateData（klinecharts 会自动追加）
            data.forEach(bar => State.chart.updateData(bar));
            debugLog("增量数据添加完成", { count: data.length });
            break;

          case "update":
            // 更新最后一根
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

    // 清空图表
    clear() {
      if (!State.chart) return;
      try {
        // klinecharts v9: clearData 只清除数据，不触发重绘
        // applyNewData([], true) 是更好的清空方式
        State.chart.applyNewData([], true);
        debugLog("图表已清空");
      } catch (e) {
        errorLog("清空图表失败", e);
      }
    },

    // 获取当前数据
    getCurrentData() {
      if (!State.chart) return [];
      try {
        return State.chart.getDataList() || [];
      } catch (e) {
        return [];
      }
    }
  };

  // ============ 合约切换逻辑（核心修复） ============

  const InstrumentSwitcher = {
    // 执行合约切换
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
        // 1. 保存当前合约数据到缓存（如果存在）
        if (State.previousInstrument) {
          const currentData = ChartController.getCurrentData();
          if (currentData && currentData.length > 0) {
            CacheManager.set(State.previousInstrument, currentData, "full");
            debugLog(`保存当前合约数据到缓存: ${State.previousInstrument}, ${currentData.length}条`);
          }
        }

        // 2. 清空图表（关键：确保旧数据不残留）
        ChartController.clear();

        // 3. 尝试从缓存恢复目标合约数据
        const cached = CacheManager.get(newInstrument);
        if (cached && cached.data.length > 0) {
          debugLog(`从缓存恢复数据: ${newInstrument}, ${cached.data.length}条, 版本${cached.version}`);

          // 验证缓存数据完整性
          const isValid = validateKlineData(cached.data);
          if (isValid) {
            const success = ChartController.loadData(cached.data, "full");
            if (success) {
              debugLog("缓存数据恢复成功");
            } else {
              errorLog("缓存数据恢复失败，等待服务器推送");
            }
          } else {
            errorLog("缓存数据无效，清除并等待服务器推送");
            CacheManager.clear(newInstrument);
          }
        } else {
          debugLog(`无缓存数据，等待服务器推送: ${newInstrument}`);
        }

        // 4. 处理切换期间暂存的更新
        this.applyPendingUpdates(newInstrument);

      } catch (e) {
        errorLog("合约切换失败", e);
      } finally {
        State.isSwitching = false;
        debugLog("合约切换完成");
      }
    },

    // 应用暂存的更新
    applyPendingUpdates(instrumentId) {
      const pending = State.pendingUpdates.get(instrumentId);
      if (!pending || pending.length === 0) return;

      debugLog(`应用暂存的更新: ${instrumentId}, ${pending.length}条`);

      pending.forEach(update => {
        this.handleDataUpdate(update, true);
      });

      State.pendingUpdates.delete(instrumentId);
    },

    // 处理数据更新（支持暂存机制）
    handleDataUpdate(data, isPending = false) {
      const instrument = data.instrument;
      const type = data.type;

      if (!instrument || !type) {
        errorLog("数据更新格式无效", data);
        return;
      }

      // 如果不是当前合约，更新缓存但不更新图表
      if (instrument !== State.currentInstrument) {
        debugLog(`非当前合约更新，仅缓存: ${instrument}`);

        if (type === "full" && data.ds) {
          CacheManager.set(instrument, data.ds, "full");
        } else if (type === "incremental" && data.ds) {
          CacheManager.set(instrument, data.ds, "incremental");
        } else if (type === "update" && data.ds) {
          CacheManager.set(instrument, [data.ds], "update");
        }
        return;
      }

      // 如果正在切换，暂存更新
      if (State.isSwitching && !isPending) {
        debugLog(`切换中，暂存更新: ${instrument}`);
        if (!State.pendingUpdates.has(instrument)) {
          State.pendingUpdates.set(instrument, []);
        }
        State.pendingUpdates.get(instrument).push(data);
        return;
      }

      // 更新图表和缓存
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
            // 先更新，再添加增量
            if (data.update_data) {
              ChartController.loadData(data.update_data, "update");
            }
            if (data.new_data && ChartController.loadData(data.new_data, "incremental")) {
              // 合并到缓存
              const existing = CacheManager.get(instrument);
              if (existing) {
                const combined = [...existing.data];
                if (combined.length > 0) combined.pop(); // 移除旧的最后一根
                combined.push(data.update_data); // 添加更新后的
                combined.push(...data.new_data); // 添加新增的
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

  // ============ Shiny 消息处理器 ============

  function initShinyHandlers() {
    if (typeof Shiny === "undefined") {
      console.error("Shiny 未加载");
      return;
    }

    // 数据推送
    Shiny.addCustomMessageHandler("push", function(data) {
      debugLog("收到推送", { instrument: data.instrument, type: data.type });
      InstrumentSwitcher.handleDataUpdate(data);
    });

    // 合约切换指令
    Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
      debugLog("收到切换指令", msg);
      InstrumentSwitcher.switchTo(msg.instrument, msg.from_instrument);
    });

    // 清空特定合约
    Shiny.addCustomMessageHandler("clearChartForInstrument", function(msg) {
      debugLog("收到清空指令", msg);
      if (msg.instrument === State.currentInstrument) {
        ChartController.clear();
        CacheManager.clear(msg.instrument);
      }
    });

    // 清空所有
    Shiny.addCustomMessageHandler("clearAll", function() {
      debugLog("收到清空所有指令");
      ChartController.clear();
      CacheManager.clearAll();
      State.currentInstrument = null;
      State.previousInstrument = null;
    });

    debugLog("Shiny消息处理器已注册");
  }

  // ============ 初始化 ============

  function init() {
    debugLog("应用初始化开始");

    // 等待DOM和klinecharts加载
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", init);
      return;
    }

    if (typeof klinecharts === "undefined") {
      console.error("klinecharts 库未加载");
      setTimeout(init, 100);
      return;
    }

    // 初始化图表
    ChartController.init();

    // 注册Shiny处理器
    initShinyHandlers();

    // 页面卸载时清理
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

  // 启动
  init();

})();
', "www/js/chartapp.js")

  message("前端文件创建完成")
  message("请确保 klinecharts.min.js 已放置在 www/js/ 目录")
}

# 执行文件创建
create_frontend_files()

# 启动应用
shinyApp(ui, server)