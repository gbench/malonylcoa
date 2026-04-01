# app.R - 修复版v3.1：CSS分离，布局紧凑

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

# ============ UI模块 - 引用外部CSS ============

ui <- fluidPage(
  # 引用外部CSS文件
  tags$head(
    tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css")
  ),

  div(class = "title-panel", 
      titlePanel("CTP 实时K线图 - v3.1")),

  sidebarLayout(
    sidebarPanel(
      width = 3,
      class = "sidebar-panel",

      # 连接状态 - 紧凑显示
      div(class = "status-bar",
        uiOutput("connection_status_ui", inline = TRUE),
        textOutput("active_instrument_display", inline = TRUE)
      ),

      # 实时行情 - 包含盘口档位
      div(class = "market-data",
        div(class = "section-title", "实时行情"),
        verbatimTextOutput("price_info", placeholder = TRUE),
        # 盘口档位
        div(class = "order-book",
          div(class = "order-book-section",
            div(class = "order-book-title", "卖盘 Ask"),
            uiOutput("ask_levels")
          ),
          div(class = "order-book-section",
            div(class = "order-book-title", "买盘 Bid"),
            uiOutput("bid_levels")
          )
        )
      ),

      # 服务器配置 - 紧凑布局，主机端口一行
      div(class = "control-group compact",
        div(class = "section-title", "服务器"),
        div(class = "host-port-row",
          div(class = "host-input",
            tags$label("主机"),
            textInput("host", NULL, value = "192.168.1.41", placeholder = "IP地址")
          ),
          div(class = "port-input",
            tags$label("端口"),
            numericInput("port", NULL, value = 9898, min = 1, max = 65535)
          )
        ),
        div(class = "btn-row",
          actionButton("connect_btn", "启动", class = "btn-primary btn-small"),
          actionButton("disconnect_btn", "停止", class = "btn-danger btn-small")
        )
      ),

      # 推送周期 - 紧凑
      div(class = "control-group compact",
        div(class = "section-title", "推送周期"),
        sliderInput("push_interval", NULL, min = 0.1, max = 5, value = 1, step = 0.1),
        div(class = "help-text", "默认1秒，越小越频繁")
      ),

      # 合约选择 - 紧凑
      div(class = "control-group compact",
        div(class = "section-title", "合约"),
        selectInput("instrument", NULL, choices = NULL),
        actionButton("refresh_btn", "刷新", icon = icon("sync"), class = "btn-primary btn-small btn-full")
      ),

      # K线周期 - 紧凑
      div(class = "control-group compact",
        div(class = "section-title", "K线周期"),
        div(class = "period-row",
          numericInput("kline_period", NULL, value = 1, min = 1, max = 1440, step = 1),
          span(class = "unit", "分钟")
        )
      ),

      # 调试日志
      div(class = "debug-panel",
        div(class = "debug-header", 
          span("调试日志"),
          actionButton("clear_log", "清除", class = "btn-tiny")
        ),
        uiOutput("debug_info")
      ),

      actionButton("exit", "退出", icon = icon("power-off"), class = "btn-danger btn-full")
    ),

    mainPanel(
      width = 9,
      class = "main-panel",
      div(id = "chart", class = "chart-container"),
      tags$script(src = "js/klinecharts.min.js"),
      tags$script(src = "js/chartapp.js")
    )
  )
)

# ============ 服务器模块 ============

server <- function(input, output, session) {

  state <- InstrumentStateManager$new()
  ctp_client <- NULL
  is_connected <- reactiveVal(FALSE)
  available_instruments <- character()
  running <- TRUE
  debug_messages <- character()
  current_tick <- reactiveVal(NULL)

  timers <- list(instrument = NULL, price = NULL, kline = NULL)

  add_debug <- function(msg) {
    timestamp <- format(Sys.time(), "%H:%M:%S.%OS3")
    debug_msg <- paste0("<div class='debug-entry'><span class='debug-timestamp'>[", timestamp, "]</span> ", msg, "</div>")
    debug_messages <<- c(debug_msg, debug_messages)[1:50]
    output$debug_info <- renderUI({
      HTML(paste(rev(debug_messages), collapse = ""))
    })
    message(paste0("[", timestamp, "] ", msg))
  }

  # 清除日志
  observeEvent(input$clear_log, {
    debug_messages <<- character()
    output$debug_info <- renderUI({ HTML("") })
  })

  show_notification <- function(message, type = "default", duration = 3) {
    tryCatch({
      showNotification(message, type = type, duration = duration)
    }, error = function(e) {
      message("通知失败: ", message)
    })
  }

  output$connection_status_ui <- renderUI({
    if (is_connected()) {
      HTML("<span class='status-indicator status-connected'></span><span class='status-text connected'>已连接</span>")
    } else {
      HTML("<span class='status-indicator status-disconnected'></span><span class='status-text disconnected'>未连接</span>")
    }
  })

  output$active_instrument_display <- renderText({
    req(state$current_instrument)
    paste0(" | ", state$current_instrument)
  })

  # 盘口显示
  output$ask_levels <- renderUI({
    tick <- current_tick()
    if (is.null(tick)) {
      return(HTML("<div class='waiting'>等待数据...</div>"))
    }

    rows <- lapply(5:1, function(i) {
      price <- tick[[paste0("AskPrice", i)]] %||% 0
      vol <- tick[[paste0("AskVolume", i)]] %||% 0
      if (price > 0) {
        div(class = "order-row",
          span(class = "ask-price", sprintf("%.2f", price)),
          span(class = "order-volume", vol)
        )
      } else {
        div(class = "order-row empty", HTML("—"))
      }
    })
    do.call(tagList, rows)
  })

  output$bid_levels <- renderUI({
    tick <- current_tick()
    if (is.null(tick)) {
      return(HTML("<div class='waiting'>等待数据...</div>"))
    }

    rows <- lapply(1:5, function(i) {
      price <- tick[[paste0("BidPrice", i)]] %||% 0
      vol <- tick[[paste0("BidVolume", i)]] %||% 0
      if (price > 0) {
        div(class = "order-row",
          span(class = "bid-price", sprintf("%.2f", price)),
          span(class = "order-volume", vol)
        )
      } else {
        div(class = "order-row empty", HTML("—"))
      }
    })
    do.call(tagList, rows)
  })

  # 清理函数
  send_undump_command <- function() {
    if (is.null(ctp_client)) return(FALSE)
    tryCatch({
      if (!is.null(ctp_client$.sessionfd) && !is.null(ctp_client$.conn)) {
        undump_cmd <- sprintf("undump %s", ctp_client$.sessionfd)
        add_debug(paste0("发送 undump: ", undump_cmd))
        writeLines(undump_cmd, ctp_client$.conn)
        response <- base::readLines(ctp_client$.conn, n = 1, warn = FALSE)
        Sys.sleep(0.1)
        return(TRUE)
      }
    }, error = function(e) {
      add_debug(paste0("undump 失败: ", e$message))
      return(FALSE)
    })
    return(FALSE)
  }

  stop_client <- function() {
    if (is.null(ctp_client)) return()
    tryCatch({
      ctp_client$stop()
      add_debug("客户端已停止")
    }, error = function(e) {
      add_debug(paste0("停止客户端出错: ", e$message))
    })
  }

  reset_env <- function(full_reset = TRUE) {
    add_debug("开始清理...")
    if (!is.null(ctp_client)) {
      send_undump_command()
      stop_client()
    }
    ctp_client <<- NULL
    is_connected(FALSE)
    available_instruments <<- character()
    current_tick(NULL)
    if (full_reset) {
      state$clear_all_cache()
    }
    add_debug("清理完成")
  }

  stop_timers <- function() {
    for (name in names(timers)) {
      if (!is.null(timers[[name]])) timers[[name]] <- NULL
    }
  }

  # K线更新
  get_incremental_kline <- function(ticks_df, period_minutes, instrument_id) {
    if (is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)

    base_kline <- state$get_cache(instrument_id)
    full_kline <- ticks_to_kline(ticks_df, period_minutes, base_kline)
    if (is.null(full_kline) || length(full_kline$timestamp) == 0) return(NULL)

    if (is.null(base_kline) || length(base_kline$timestamp) == 0) {
      return(list(type = "full", instrument = instrument_id, data = full_kline, last_timestamp = max(full_kline$timestamp)))
    }

    cached_last_ts <- max(base_kline$timestamp)
    update_indices <- which(full_kline$timestamp >= cached_last_ts)
    if (length(update_indices) == 0) return(NULL)

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
        return(list(type = "update_and_incremental", instrument = instrument_id, update_data = new_last, new_data = new_kline, last_timestamp = max(full_kline$timestamp)))
      }
      return(list(type = "update", instrument = instrument_id, data = new_last, last_timestamp = max(full_kline$timestamp)))
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
      return(list(type = "incremental", instrument = instrument_id, data = new_kline, last_timestamp = max(full_kline$timestamp)))
    }
  }

  send_kline_to_frontend <- function(result) {
    if (is.null(result)) return()
    instrument <- result$instrument
    type <- result$type

    send_data <- switch(type,
      "full" = list(instrument = instrument, type = "full", ds = convert_to_frontend_format(result$data)),
      "incremental" = list(instrument = instrument, type = "incremental", ds = convert_to_frontend_format(result$data)),
      "update" = list(instrument = instrument, type = "update", ds = list(timestamp = result$data$timestamp, open = result$data$open, high = result$data$high, low = result$data$low, close = result$data$close, volume = result$data$volume, oint = result$data$oint)),
      "update_and_incremental" = list(instrument = instrument, type = "update_and_incremental", update_data = list(timestamp = result$update_data$timestamp, open = result$update_data$open, high = result$update_data$high, low = result$update_data$low, close = result$update_data$close, volume = result$update_data$volume, oint = result$update_data$oint), new_data = convert_to_frontend_format(result$new_data)),
      NULL
    )

    if (!is.null(send_data)) {
      state$update_cache(instrument, result)
      if (instrument == state$current_instrument) {
        session$sendCustomMessage("push", send_data)
        add_debug(paste0("发送[", type, "]: ", instrument))
      }
    }
  }

  # 定时器
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
        add_debug(paste0("K线错误: ", e$message))
      })
      push_interval <- isolate(input$push_interval) %||% 1
      if (running && !is.null(ctp_client)) {
        timers$kline <<- later::later(update_kline, push_interval)
      }
    }
    add_debug(paste0("K线更新器: ", input$push_interval, "s"))
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
            cat(current, "\n")
            cat("最新: ", sprintf("%.2f", stats$last), " 均: ", sprintf("%.2f", stats$mean), "\n")
            cat("最高: ", sprintf("%.2f", stats$max), " 低: ", sprintf("%.2f", stats$min), "\n")
            cat("Ticks:", stats$n, " 时间: ", stats$updatetime, "\n")
          })
        }
        ticks_df <- ctp_client$ticks(current)
        if (!is.null(ticks_df) && nrow(ticks_df) > 0) {
          current_tick(as.list(ticks_df[nrow(ticks_df), ]))
        }
      }, error = function(e) {})
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
        instr <- if (!is.null(ctp_client$instrumentids)) ctp_client$instrumentids else character()
        if (length(instr) > 0 && !identical(sort(instr), sort(available_instruments))) {
          available_instruments <<- instr
          current_selection <- isolate(input$instrument)
          new_selection <- if (!is.null(current_selection) && current_selection %in% instr) current_selection else instr[1]
          updateSelectInput(session, "instrument", choices = instr, selected = new_selection)
          add_debug(paste0("合约: ", length(instr), "个"))
        }
      }, error = function(e) {
        add_debug(paste0("合约列表错误: ", e$message))
      })
      if (running && !is.null(ctp_client)) {
        timers$instrument <<- later::later(update_instruments, 5)
      }
    }
    update_instruments()
  }

  # 事件处理
  observeEvent(input$instrument, {
    new_instrument <- input$instrument
    if (is.null(new_instrument) || new_instrument == "") return()
    old_instrument <- state$current_instrument
    if (!identical(new_instrument, old_instrument)) {
      add_debug(paste0("切换: ", old_instrument, " -> ", new_instrument))
      state$set_current(new_instrument, input$kline_period)
      state$tick_counts[[new_instrument]] <- 0
      current_tick(NULL)
      session$sendCustomMessage("switchInstrument", list(instrument = new_instrument, from_instrument = old_instrument, clearCache = FALSE))
      show_notification(paste0("已切换: ", new_instrument), "message", 2)
    }
  }, ignoreNULL = TRUE, ignoreInit = FALSE)

  observeEvent(input$kline_period, {
    new_period <- input$kline_period
    if (!is.null(new_period) && !identical(new_period, state$current_period)) {
      add_debug(paste0("周期: ", state$current_period, " -> ", new_period, "m"))
      state$current_period <- new_period
      current <- state$current_instrument
      if (!is.null(current)) {
        state$clear_instrument_cache(current)
        state$tick_counts[[current]] <- 0
        session$sendCustomMessage("clearChartForInstrument", list(instrument = current, reason = "period_change"))
      }
      show_notification(paste0("周期: ", new_period, "分钟"), "message", 2)
    }
  })

  observeEvent(input$push_interval, {
    add_debug(paste0("推送: ", input$push_interval, "s"))
  })

  observeEvent(input$connect_btn, {
    add_debug("启动连接...")
    if (!is.null(ctp_client)) {
      add_debug("清理旧连接...")
      reset_env(full_reset = TRUE)
      Sys.sleep(0.5)
    }
    do_connect()
  })

  do_connect <- function() {
    stop_timers()
    running <<- TRUE
    state$clear_all_cache()
    current_tick(NULL)
    tryCatch({
      if (!exists("ctpd_async")) stop("ctpd_async 未定义")
      client <- ctpd_async(host = input$host, port = input$port)
      ctp_client <<- client
      is_connected(TRUE)
      add_debug("已连接")
      show_notification("连接成功", "message", 3)
      start_instrument_updater()
      start_kline_updater()
      start_price_monitor()
    }, error = function(e) {
      is_connected(FALSE)
      add_debug(paste0("连接失败: ", e$message))
      showModal(modalDialog(title = "连接失败", paste("错误:", e$message), easyClose = TRUE, footer = modalButton("关闭")))
    })
  }

  observeEvent(input$disconnect_btn, {
    if (!is.null(ctp_client)) {
      add_debug("停止连接...")
      running <<- FALSE
      stop_timers()
      session$sendCustomMessage("resetAll", list(reason = "disconnect", timestamp = as.numeric(Sys.time())))
      reset_env(full_reset = TRUE)
      output$price_info <- renderPrint({ cat("未连接\n点击启动\n") })
      show_notification("已停止", "message", 3)
    } else {
      show_notification("无连接", "warning", 3)
    }
  })

  observeEvent(input$refresh_btn, {
    current <- state$current_instrument
    if (!is.null(current) && !is.null(ctp_client)) {
      add_debug(paste0("刷新: ", current))
      state$clear_instrument_cache(current)
      state$tick_counts[[current]] <- 0
      current_tick(NULL)
      session$sendCustomMessage("clearChartForInstrument", list(instrument = current, reason = "manual_refresh"))
      show_notification("刷新中...", "message", 2)
    }
  })

  observeEvent(input$exit, {
    showModal(modalDialog(title = "确认退出", "确定退出?", easyClose = TRUE, footer = tagList(modalButton("取消"), actionButton("confirm_exit", "确定", class = "btn-danger"))))
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
    if (!is.null(ctp_client)) {
      send_undump_command()
      stop_client()
    }
  })

  output$price_info <- renderPrint({ cat("等待连接...\n") })
}

# ============ 创建前端文件 ============

create_frontend_files <- function() {
  if (!dir.exists("www")) dir.create("www")
  if (!dir.exists("www/js")) dir.create("www/js")
  if (!dir.exists("www/css")) dir.create("www/css")

  # 完整的CSS文件
  css_content <- '
/* CTP K线图应用样式 - 紧凑布局 */

* { box-sizing: border-box; }

body {
  background-color: #1e1e2f;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  margin: 0;
  padding: 0;
  font-size: 13px;
}

/* 标题栏 */
.title-panel {
  display: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 10px 10px;
  border-radius: 6px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.title-panel h2 {
  margin: 0;
  color: white;
  text-align: center;
  font-weight: 600;
  font-size: 18px;
}

/* 侧边栏 */
.sidebar-panel {
  background-color: #2d2d3f;
  border-radius: 6px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  height: calc(110vh - 70px);
  overflow-y: auto;
}

/* 状态栏 - 紧凑 */
.status-bar {
  background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  border: 1px solid #4a5568;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.status-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-connected { background-color: #48bb78; box-shadow: 0 0 6px #48bb78; }
.status-disconnected { background-color: #fc8181; }

.status-text { font-weight: 600; }
.status-text.connected { color: #48bb78; }
.status-text.disconnected { color: #fc8181; }

/* 行情数据区 */
.market-data {
  background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #4a5568;
}

.section-title {
  font-size: 11px;
  color: #667eea;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

/* 价格信息 */
pre.price-info {
  background: transparent !important;
  border: none !important;
  color: #e0e0e0 !important;
  font-family: "Fira Code", Consolas, monospace !important;
  font-size: 11px !important;
  padding: 0 !important;
  margin: 0 0 10px 0 !important;
  line-height: 1.5 !important;
}

/* 盘口档位 */
.order-book {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.order-book-section {
  background: rgba(0,0,0,0.2);
  border-radius: 4px;
  padding: 6px;
}

.order-book-title {
  font-size: 10px;
  color: #a0aec0;
  text-align: center;
  margin-bottom: 4px;
  font-weight: 600;
}

.order-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  padding: 1px 0;
  font-family: "Fira Code", monospace;
}

.order-row.empty {
  color: #4a5568;
  justify-content: center;
}

.waiting {
  color: #718096;
  text-align: center;
  font-size: 11px;
  padding: 10px 0;
}

.ask-price { color: #ef5350; font-weight: 600; }
.bid-price { color: #26a69a; font-weight: 600; }
.order-volume { color: #e0e0e0; }

/* 控制组 - 紧凑 */
.control-group {
  background: rgba(255,255,255,0.03);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #3d3d5c;
}

.control-group.compact { padding: 8px; }

.control-group label {
  color: #a0aec0;
  font-weight: 600;
  font-size: 10px;
  text-transform: uppercase;
  margin-bottom: 4px;
  display: block;
}

/* 主机端口一行 */
.host-port-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.host-input { flex: 2; }
.port-input { flex: 1; }

.host-input label,
.port-input label {
  font-size: 9px;
  margin-bottom: 2px;
}

/* 输入框样式 */
.form-control {
  background-color: #1e1e2f !important;
  border: 1px solid #4a5568 !important;
  color: #e0e0e0 !important;
  border-radius: 4px !important;
  padding: 4px 8px !important;
  font-size: 12px !important;
  height: 28px !important;
}

.form-control:focus {
  border-color: #667eea !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

select.form-control { padding-right: 20px !important; }

/* 按钮行 */
.btn-row {
  display: flex;
  gap: 8px;
}

/* 按钮样式 */
.btn {
  border: none;
  border-radius: 4px;
  font-weight: 600;
  font-size: 12px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.btn-danger {
  background: linear-gradient(135deg, #fc8181 0%, #e53e3e 100%);
  color: white;
}

.btn-small { padding: 4px 10px; font-size: 11px; flex: 1; }
.btn-full { width: 100%; margin-top: 6px; }
.btn-tiny { 
  padding: 2px 8px; 
  font-size: 9px; 
  background: #4a5568;
  color: white;
  float: right;
}

/* 周期行 */
.period-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.period-row .form-control { width: 60px !important; }

.unit {
  color: #718096;
  font-size: 11px;
}

/* 滑块 */
.irs-line {
  background: #4a5568 !important;
  border-radius: 2px;
  height: 4px !important;
}

.irs-bar {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%) !important;
  height: 4px !important;
}

.irs-handle {
  background: #667eea !important;
  border: 2px solid white !important;
  width: 14px !important;
  height: 14px !important;
  top: 19px !important;
}

.irs-single {
  background: #667eea !important;
  font-size: 10px !important;
  padding: 2px 6px !important;
}

.irs-min, .irs-max {
  color: #718096 !important;
  font-size: 9px !important;
}

/* 帮助文字 */
.help-text {
  color: #718096;
  font-size: 10px;
  margin-top: 4px;
}

/* 调试面板 */
.debug-panel {
  background-color: #1a1a2e;
  border: 1px solid #3d3d5c;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 10px;
  font-family: "Fira Code", Consolas, monospace;
  font-size: 10px;
  max-height: 150px;
  overflow-y: auto;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  color: #667eea;
  font-weight: 600;
  font-size: 11px;
}

.debug-entry {
  margin-bottom: 2px;
  padding: 1px 0;
  border-bottom: 1px solid #2d2d3f;
  color: #a0aec0;
}

.debug-timestamp { color: #667eea; }

/* 主面板 */
.main-panel {
  padding: 0;
}

.chart-container {
  width: 100%;
  height: calc(110vh - 70px);
  background-color: #1e1e2f;
  border: 1px solid #3d3d5c;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}

/* 通知 */
.shiny-notification {
  position: fixed !important;
  top: 15px !important;
  right: 15px !important;
  z-index: 9999;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white !important;
  border: none !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3) !important;
  padding: 10px 15px !important;
  font-size: 12px !important;
}

/* 模态框 */
.modal-content {
  background-color: #2d2d3f !important;
  color: #e0e0e0 !important;
  border: 1px solid #4a5568 !important;
  border-radius: 8px !important;
}

.modal-header {
  border-bottom: 1px solid #3d3d5c !important;
  padding: 12px 15px !important;
}

.modal-title { font-size: 14px !important; font-weight: 600 !important; }

.modal-body { padding: 15px !important; font-size: 13px !important; }

.modal-footer {
  border-top: 1px solid #3d3d5c !important;
  padding: 10px 15px !important;
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #1e1e2f;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #4a5568;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #667eea;
}
'

  writeLines(css_content, "www/css/style.css")

  # 前端JS
  js_content <- '
// chartapp.js - v3.1
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
    isRunning: false,
    isInitialized: false
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
        errorLog("无效数据", { instrumentId });
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

      debugLog(`缓存[${type}]: ${instrumentId}, ${newData.length}条`);
    },

    clear(instrumentId) {
      if (State.instrumentCache.has(instrumentId)) {
        State.instrumentCache.delete(instrumentId);
        debugLog(`清除: ${instrumentId}`);
      }
    },

    clearAll() {
      State.instrumentCache.clear();
      debugLog("清除所有缓存");
    }
  };

  const ChartController = {
    init() {
      if (State.chart) {
        debugLog("销毁旧图表");
        this.destroy();
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

        this.registerIndicators();
        this.createIndicators();

        State.isInitialized = true;
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
        errorLog("无效数据");
        return false;
      }

      if (!validateKlineData(type === "update" ? [data] : data)) {
        errorLog("格式错误");
        return false;
      }

      try {
        switch (type) {
          case "full":
            State.chart.applyNewData(data);
            debugLog("全量", data.length);
            break;
          case "incremental":
            data.forEach(bar => State.chart.updateData(bar));
            debugLog("增量", data.length);
            break;
          case "update":
            State.chart.updateData(data);
            debugLog("更新");
            break;
          default:
            return false;
        }
        return true;
      } catch (e) {
        errorLog("加载失败", e);
        return false;
      }
    },

    clear() {
      if (!State.chart) return;
      try {
        State.chart.applyNewData([], true);
        debugLog("清空");
      } catch (e) {
        errorLog("清空失败", e);
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
        State.isInitialized = false;
        debugLog("图表销毁");
      } catch (e) {
        errorLog("销毁失败", e);
      }
    }
  };

  const InstrumentSwitcher = {
    switchTo(newInstrument, fromInstrument) {
      if (State.isSwitching) {
        debugLog("切换中...");
        return;
      }

      if (!newInstrument || newInstrument === State.currentInstrument) {
        return;
      }

      State.isSwitching = true;
      State.previousInstrument = fromInstrument || State.currentInstrument;
      State.currentInstrument = newInstrument;

      debugLog(`切换: ${State.previousInstrument} -> ${newInstrument}`);

      try {
        if (State.previousInstrument) {
          const currentData = ChartController.getCurrentData();
          if (currentData && currentData.length > 0) {
            CacheManager.set(State.previousInstrument, currentData, "full");
          }
        }

        ChartController.clear();

        const cached = CacheManager.get(newInstrument);
        if (cached && cached.data.length > 0) {
          debugLog(`恢复: ${newInstrument}, ${cached.data.length}条`);
          if (validateKlineData(cached.data)) {
            ChartController.loadData(cached.data, "full");
          } else {
            CacheManager.clear(newInstrument);
          }
        }

        this.applyPendingUpdates(newInstrument);
      } catch (e) {
        errorLog("切换失败", e);
      } finally {
        State.isSwitching = false;
      }
    },

    applyPendingUpdates(instrumentId) {
      const pending = State.pendingUpdates.get(instrumentId);
      if (!pending) return;
      pending.forEach(update => this.handleDataUpdate(update, true));
      State.pendingUpdates.delete(instrumentId);
    },

    handleDataUpdate(data, isPending = false) {
      const instrument = data.instrument;
      const type = data.type;

      if (!instrument || !type) return;

      if (instrument !== State.currentInstrument) {
        if (type === "full" && data.ds) CacheManager.set(instrument, data.ds, "full");
        else if (type === "incremental" && data.ds) CacheManager.set(instrument, data.ds, "incremental");
        else if (type === "update" && data.ds) CacheManager.set(instrument, [data.ds], "update");
        return;
      }

      if (State.isSwitching && !isPending) {
        if (!State.pendingUpdates.has(instrument)) State.pendingUpdates.set(instrument, []);
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
            if (data.update_data) ChartController.loadData(data.update_data, "update");
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
        errorLog("更新失败", e);
      }
    }
  };

  function resetAllState(reason) {
    debugLog(`重置: ${reason}`);
    ChartController.destroy();
    CacheManager.clearAll();
    State.currentInstrument = null;
    State.previousInstrument = null;
    State.isSwitching = false;
    State.pendingUpdates.clear();
    State.dataVersion = 0;
    State.isRunning = false;
    State.isInitialized = false;

    setTimeout(() => {
      ChartController.init();
      debugLog("重置完成");
    }, 100);
  }

  function initShinyHandlers() {
    if (typeof Shiny === "undefined") {
      console.error("Shiny 未加载");
      return;
    }

    Shiny.addCustomMessageHandler("push", function(data) {
      debugLog("推送", { instrument: data.instrument, type: data.type });
      InstrumentSwitcher.handleDataUpdate(data);
    });

    Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
      debugLog("切换指令", msg);
      InstrumentSwitcher.switchTo(msg.instrument, msg.from_instrument);
    });

    Shiny.addCustomMessageHandler("clearChartForInstrument", function(msg) {
      debugLog("清空", msg);
      if (msg.instrument === State.currentInstrument) {
        ChartController.clear();
        CacheManager.clear(msg.instrument);
      }
    });

    Shiny.addCustomMessageHandler("clearAll", function() {
      debugLog("清空所有");
      ChartController.clear();
      CacheManager.clearAll();
      State.currentInstrument = null;
      State.previousInstrument = null;
    });

    Shiny.addCustomMessageHandler("resetAll", function(msg) {
      debugLog("完全重置", msg);
      resetAllState(msg.reason || "unknown");
    });

    debugLog("处理器已注册");
  }

  function init() {
    debugLog("初始化开始");

    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", init);
      return;
    }

    if (typeof klinecharts === "undefined") {
      console.error("klinecharts 未加载");
      setTimeout(init, 100);
      return;
    }

    ChartController.init();
    initShinyHandlers();
    State.isRunning = true;

    window.addEventListener("beforeunload", function() {
      CacheManager.clearAll();
      if (State.chart) {
        try { klinecharts.dispose("chart"); } catch (e) {}
      }
    });

    debugLog("初始化完成");
  }

  init();

})();
'

  writeLines(js_content, "www/js/chartapp.js")

  message("前端文件创建完成")
}

create_frontend_files()

shinyApp(ui, server)