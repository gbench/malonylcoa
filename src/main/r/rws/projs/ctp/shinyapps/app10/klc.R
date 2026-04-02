# app.R - 修复版v4.2：恢复完整指标，保持布局不变

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

# 使用data.table聚合K线
aggregate_kline <- function(ticks_df) {
  dt <- data.table::as.data.table(ticks_df)
  
  # 添加调试信息
  if (nrow(dt) == 0) return(data.table::data.table())
  
  # 检查Period列
  if (length(unique(dt$Period)) == 0) return(data.table::data.table())
  
  kline <- dt[, .(
    timestamp = as.numeric(first(Period)) * 1000,
    open = first(LastPrice),
    high = max(LastPrice),
    low = min(LastPrice),
    close = last(LastPrice),
    volume = last(Volume) - first(Volume),
    oint = last(OpenInterest)
  ), by = Period]
  
  # 确保volume不为负数
  kline[volume < 0, volume := 0]
  
  data.table::setorder(kline, timestamp)
  kline[, .(timestamp, open, high, low, close, volume, oint)]
}

# 合并K线数据框 - 同一周期合并OHLCV数据
merge_kline_dt <- function(base_dt, new_dt) {
  # 直接使用data.table操作，避免频繁转换
  if (is.null(base_dt) || nrow(base_dt) == 0) return(new_dt)
  if (is.null(new_dt) || nrow(new_dt) == 0) return(base_dt)
  
  # 确保是data.table
  if (!data.table::is.data.table(base_dt)) base_dt <- data.table::as.data.table(base_dt)
  if (!data.table::is.data.table(new_dt)) new_dt <- data.table::as.data.table(new_dt)
  
  # 找出需要合并的键
  setkey(base_dt, timestamp)
  setkey(new_dt, timestamp)
  
  # 使用data.table的merge
  merged <- merge(base_dt, new_dt, by = "timestamp", all = TRUE, suffixes = c("", "_new"))
  
  # 处理合并后的数据
  merged[, `:=`(
    open = ifelse(is.na(open), open_new, open),
    high = ifelse(is.na(high), high_new, pmax(high, high_new, na.rm = TRUE)),
    low = ifelse(is.na(low), low_new, pmin(low, low_new, na.rm = TRUE)),
    close = ifelse(is.na(close), close_new, close_new),
    volume = ifelse(is.na(volume), volume_new, volume + volume_new),
    oint = ifelse(is.na(oint), oint_new, oint_new)
  )]
  
  # 删除临时列
  merged[, c("open_new", "high_new", "low_new", "close_new", "volume_new", "oint_new") := NULL]
  
  setorder(merged, timestamp)
  merged
}

# 批量递推更新统计量的函数
update_stats_batch <- function(prev_stats, new_prices) {
  if (length(new_prices) == 0) return(prev_stats)
  
  prev_n <- prev_stats$n
  prev_mean <- prev_stats$mean
  prev_M2 <- prev_stats$M2
  prev_min <- prev_stats$min
  prev_max <- prev_stats$max
  
  k <- length(new_prices)
  new_n <- prev_n + k
  
  # 批量更新均值和 M2
  sum_new <- sum(new_prices)
  new_mean <- (prev_n * prev_mean + sum_new) / new_n
  
  # 更新 M2（离差平方和）
  sum_sq_new <- sum((new_prices - new_mean)^2)
  new_M2 <- prev_M2 + sum_sq_new + prev_n * (prev_mean - new_mean)^2
  
  # 更新最小最大值
  new_min <- min(prev_min, min(new_prices))
  new_max <- max(prev_max, max(new_prices))
  
  list(
    n = new_n,
    mean = new_mean,
    M2 = new_M2,
    sd = if (new_n > 1) sqrt(new_M2 / (new_n - 1)) else 0,
    min = new_min,
    max = new_max,
    last = new_prices[k]  # 保存最新价格
  )
}

# ============ 状态管理类 ============

InstrumentStateManager <- R6::R6Class(
  "InstrumentStateManager",
  
  public = list(
    kline_cache = list(),
    tick_counts = list(),
    last_update_times = list(),
    attrs = new.env(hash = TRUE),
    current_instrument_id = NULL,
    current_period = 1,
    
    get_kline_dt = function(instrument_id) {
      if (is.null(instrument_id)) return(NULL)
      self$kline_cache[[instrument_id]]
    },
    
    set_kline_dt = function(instrument_id, dt) {
      if (is.null(instrument_id)) return()
      self$kline_cache[[instrument_id]] <- dt
      self$last_update_times[[instrument_id]] <- Sys.time()
    },
    
    clear_instrument = function(instrument_id) {
      if (!is.null(instrument_id)) {
        self$kline_cache[[instrument_id]] <- NULL
        self$tick_counts[[instrument_id]] <- 0
        self$last_update_times[[instrument_id]] <- NULL
        self$attrs[[instrument_id]] <- NULL
      }
    },
    
    clear_all = function() {
      self$kline_cache <- list()
      self$tick_counts <- list()
      self$last_update_times <- list()
      self$attrs <- new.env(hash = TRUE)
      self$current_instrument_id <- NULL
    },
    
    set_current = function(instrument_id, period = NULL) {
      self$current_instrument_id <- instrument_id
      if (!is.null(period)) {
        self$current_period <- period
      }
    }
  )
)

# ============ UI模块 ============

ui <- fluidPage(
  tags$head(
    tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css")
  ),
  
  div(class = "title-panel", 
      titlePanel("CTP 实时K线图 - v4.2")),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      class = "sidebar-panel",
      
      div(class = "status-bar",
          uiOutput("connection_status_ui", inline = TRUE),
          textOutput("active_instrument_display", inline = TRUE)
      ),
      
      div(class = "market-data",
          div(class = "section-title", "实时行情"),
          uiOutput("price_info"),
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
      
      div(class = "control-group compact",
          div(class = "section-title", "推送周期"),
          sliderInput("push_interval", NULL, min = 0.1, max = 5, value = 1, step = 0.1),
          div(class = "help-text", "默认1秒，越小越频繁")
      ),
      
      div(class = "control-group compact",
          div(class = "section-title", "合约"),
          selectInput("instrument", NULL, choices = NULL),
          actionButton("refresh_btn", "刷新", icon = icon("sync"), class = "btn-primary btn-small btn-full")
      ),
      
      div(class = "control-group compact",
          div(class = "section-title", "K线周期"),
          div(class = "period-row",
              numericInput("kline_period", NULL, value = 1, min = 1, max = 1440, step = 1),
              span(class = "unit", "分钟")
          )
      ),
      
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
  
  last_price_data <- reactiveValues(
    last = NULL, mean = NULL, max = NULL, min = NULL, 
    n = NULL, updatetime = NULL, instrument = NULL
  )
  
  timers <- list(instrument = NULL, kline = NULL)
  
  add_debug <- function(msg) {
    timestamp <- format(Sys.time(), "%H:%M:%S.%OS3")
    debug_msg <- paste0("<div class='debug-entry'><span class='debug-timestamp'>[", timestamp, "]</span> ", msg, "</div>")
    debug_messages <<- c(debug_msg, debug_messages)[1:50]
    output$debug_info <- renderUI({
      HTML(paste(rev(debug_messages), collapse = ""))
    })
    message(paste0("[", timestamp, "] ", msg))
  }
  
  show_notification <- function(message, type = "default", duration = 3) {
    tryCatch({
      showNotification(message, type = type, duration = duration)
    }, error = function(e) {
      add_debug(paste0("通知失败: ", message))
    })
  }
  
  observeEvent(input$clear_log, {
    debug_messages <<- character()
    output$debug_info <- renderUI({ HTML("") })
  })
  
  output$connection_status_ui <- renderUI({
    if (is_connected()) {
      HTML("<span class='status-indicator status-connected'></span><span class='status-text connected'>已连接</span>")
    } else {
      HTML("<span class='status-indicator status-disconnected'></span><span class='status-text disconnected'>未连接</span>")
    }
  })
  
  output$active_instrument_display <- renderText({
    req(state$current_instrument_id)
    paste0(" | ", state$current_instrument_id)
  })
  
  # 价格数据轮询
  price_data <- reactivePoll(
    intervalMillis = 500, session = session,
    checkFunc = function() {
      if (is.null(ctp_client) || is.null(state$current_instrument_id)) return(NULL) 
      tryCatch(ctp_client$lastupdate, error = function(e) NULL)
    },
    valueFunc = function() {
      inst_id <- state$current_instrument_id
      if (is.null(ctp_client) || is.null(inst_id)) return(NULL) 
      
      inst_entity <- ctp_client$instruments[[inst_id]]
      current_n <- inst_entity$offset
      if (current_n < 1) return(NULL)
      
      # 获取之前保存的状态
      prev_stats <- state$attrs[[inst_id]]
      
      if (is.null(prev_stats) || prev_stats$n == 0) {
        # 首次：提取所有价格并计算完整统计
        all_prices <- sapply(seq(current_n), \(i) inst_entity$data[[i]]$LastPrice)
        stats <- list(
          n = current_n,
          mean = mean(all_prices),
          M2 = sum((all_prices - mean(all_prices))^2),
          sd = if (current_n > 1) sd(all_prices) else 0,
          min = min(all_prices),
          max = max(all_prices),
          last = all_prices[current_n]
        )
        # 保存首次提取的价格向量？不需要，只需要统计量
      } else if (current_n > prev_stats$n) {
        # 有新数据：只获取增量部分
        new_prices <- sapply(seq(prev_stats$n + 1, current_n), \(i) inst_entity$data[[i]]$LastPrice)
        stats <- update_stats_batch(prev_stats, new_prices)
      } else {
        # 无新数据，保持原样
        stats <- prev_stats
      }
      
      # 保存到 state$attrs
      state$attrs[[inst_id]] <- stats
      
      # 获取最新 tick 数据
      tick <- inst_entity$data[[current_n]]
      
      # 返回结果
      list(
        stats = list(
          n = stats$n,
          mean = stats$mean,
          sd = stats$sd,
          min = stats$min,
          max = stats$max,
          last = stats$last,
          updatetime = tick$UpdateTime
        ),
        tick = tick
      )
    }
  ) # price_data

  # 更新价格显示
  observe({
    data <- price_data()
    if (is.null(data) || is.null(data$stats)) return()
    
    stats <- data$stats
    current_instr <- state$current_instrument_id
    
    has_changed <- FALSE
    if (is.null(last_price_data$last) || !identical(stats$last, last_price_data$last)) {
      last_price_data$last <- stats$last
      has_changed <- TRUE
    }
    if (is.null(last_price_data$mean) || !identical(stats$mean, last_price_data$mean)) {
      last_price_data$mean <- stats$mean
      has_changed <- TRUE
    }
    if (is.null(last_price_data$max) || !identical(stats$max, last_price_data$max)) {
      last_price_data$max <- stats$max
      has_changed <- TRUE
    }
    if (is.null(last_price_data$min) || !identical(stats$min, last_price_data$min)) {
      last_price_data$min <- stats$min
      has_changed <- TRUE
    }
    if (is.null(last_price_data$n) || !identical(stats$n, last_price_data$n)) {
      last_price_data$n <- stats$n
      has_changed <- TRUE
    }
    if (is.null(last_price_data$updatetime) || !identical(stats$updatetime, last_price_data$updatetime)) {
      last_price_data$updatetime <- stats$updatetime
      has_changed <- TRUE
    }
    
    if (has_changed) {
      output$price_info <- renderUI({
        tags$div(class = "price-info",
                 tags$div(class = "price-current", current_instr),
                 tags$div(class = "price-stats-row",
                          tags$span(class = "price-label", "最新: "),
                          tags$span(class = "price-value", sprintf("%.2f", stats$last)),
                          tags$span(class = "price-label", " 均: "),
                          tags$span(class = "price-value", sprintf("%.2f", stats$mean))
                 ),
                 tags$div(class = "price-stats-row",
                          tags$span(class = "price-label", "最高: "),
                          tags$span(class = "price-value", sprintf("%.2f", stats$max)),
                          tags$span(class = "price-label", " 低: "),
                          tags$span(class = "price-value", sprintf("%.2f", stats$min))
                 ),
                 tags$div(class = "price-meta",
                          tags$span("Ticks: ", tags$strong(stats$n)),
                          tags$span(" 时间: ", stats$updatetime)
                 )
        )
      })
    }
    
    if (!is.null(data$tick)) current_tick(data$tick)
  })
  
  # 盘口显示
  output$ask_levels <- renderUI({
    tick <- current_tick()
    if (is.null(tick)) {
      return(HTML("<div class='waiting'>等待数据...</div>"))
    }
    
    rows <- lapply(1:5, function(i) {
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
    
    last_price_data$last <- NULL
    last_price_data$mean <- NULL
    last_price_data$max <- NULL
    last_price_data$min <- NULL
    last_price_data$n <- NULL
    last_price_data$updatetime <- NULL
    
    if (full_reset) {
      state$clear_all()
    }
    add_debug("清理完成")
  }
  
  stop_timers <- function() {
    for (name in names(timers)) {
      if (!is.null(timers[[name]])) timers[[name]] <- NULL
    }
  }
  
  # K线更新：flag 用于标记是否强制执行全量更新，默认为FALSE，表示系统自行判断！
  update_and_send_kline <- function(instrument_id, period, flag = FALSE) {
    if (is.null(ctp_client)) return(NULL)

    begtime <- Sys.time() # 开始时间锚点
    
    tryCatch({
      # 直接访问内部数据结构
      inst_entity <- ctp_client$instruments[[instrument_id]]
      if (is.null(inst_entity)) return(NULL)
      
      current_count <- inst_entity$size()
      last_count <- state$tick_counts[[instrument_id]] %||% 0
      
      # 没有新数据
      if (current_count == last_count) return(NULL)
      
      # 直接获取新增的tick数据（list of lists格式）
      new_ticks_list <- inst_entity$data[seq(last_count + 1, current_count)]
      
      # 更新计数
      state$tick_counts[[instrument_id]] <- current_count
      add_debug(paste0("新增tick: ", length(new_ticks_list), "条"))
      
      # 获取现有K线
      base_kline <- state$get_kline_dt(instrument_id)
      
      # 添加DateTime和Period（需要先转换为data.table）
      new_ticks_dt <- data.table::rbindlist(new_ticks_list) 
      datetime_str <- paste(new_ticks_dt$ActionDay, new_ticks_dt$UpdateTime)
      new_ticks_dt$DateTime <- as.POSIXct(datetime_str, format = "%Y%m%d %H:%M:%S") + new_ticks_dt$UpdateMillisec / 1000
      period_seconds <- period * 60 # 转换成秒数
      new_ticks_dt$Period <- floor_date_vectorized(new_ticks_dt$DateTime, period_seconds)
      new_kline_segment <- aggregate_kline(new_ticks_dt) # 新数据聚合
      if (is.null(new_kline_segment) || nrow(new_kline_segment) == 0) return(NULL)
      
      # 合并K线
      if (is.null(base_kline) || nrow(base_kline) == 0) {
        final_kline <- new_kline_segment
      } else {
        final_kline <- merge_kline_dt(base_kline, new_kline_segment) # 新老合并
      }
      
      state$set_kline_dt(instrument_id, final_kline)
      
      # 发送到前端
      if (instrument_id == state$current_instrument_id) {
        is_full <- flag || is.null(base_kline) || nrow(base_kline) == 0
        ds <- purrr::transpose(if(is_full) final_kline else final_kline[final_kline$timestamp >= min(new_kline_segment$timestamp), ]) 
        send_data <- list(instrument = instrument_id, type = if(is_full) "full" else "incremental", ds = ds)
        session$sendCustomMessage("updateKline", send_data)
        add_debug(paste0("发送[", if(is_full) "全量" else "增量", "]: ", instrument_id, " | ", length(ds), "条"))
      }
      
    }, error = function(e) {
      add_debug(paste0("K线错误: ", e$message))
    })
    
    elapsed <- as.numeric(Sys.time() - begtime)
    if (elapsed > 0.1) {
      add_debug(paste0("性能: K线更新耗时 ", round(elapsed, 3), "秒"))
    }
  }
  
  # K线更新定时器
  start_kline_updater <- function() {
    update_kline <- function() {
      if (!running || is.null(ctp_client)) return()
      inst_id <- state$current_instrument_id
      period <- state$current_period
      if (!is.null(inst_id) && inst_id != "") {
        update_and_send_kline(inst_id, period)
      }
      push_interval <- isolate(input$push_interval) %||% 1
      if (running && !is.null(ctp_client)) {
        timers$kline <<- later::later(update_kline, push_interval)
      }
    }
    add_debug(paste0("K线更新器启动: 周期=", state$current_period, "分钟"))
    update_kline()
  }
  
  # 合约列表更新
  start_instrument_updater <- function() {
    update_instruments <- function() {
      if (!running || is.null(ctp_client)) return()
      tryCatch({
        instrument_ids <- if (!is.null(ctp_client$instrumentids)) ctp_client$instrumentids else character()
        if (length(instrument_ids) > 0 && !identical(sort(instrument_ids), sort(available_instruments))) {
          available_instruments <<- instrument_ids
          current_selection <- isolate(input$instrument)
          new_selection <- if (!is.null(current_selection) && current_selection %in% instrument_ids) current_selection else instrument_ids[1]
          updateSelectInput(session, "instrument", choices = instrument_ids, selected = new_selection)
          add_debug(paste0("合约: ", length(instrument_ids), "个"))
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
    new_instrument_id <- input$instrument
    if (is.null(new_instrument_id) || new_instrument_id == "") return()
    old_instrument_id <- state$current_instrument_id
    if (!identical(new_instrument_id, old_instrument_id)) {
      add_debug(paste0("切换: ", old_instrument_id, " -> ", new_instrument_id))
      state$set_current(new_instrument_id, input$kline_period)
      state$tick_counts[[new_instrument_id]] <- 0
      current_tick(NULL)
      session$sendCustomMessage("switchInstrument", list(instrument = new_instrument_id))
      show_notification(paste0("已切换: ", new_instrument_id), "message", 2)
      update_and_send_kline(new_instrument_id, input$kline_period, TRUE) # 强制对新合约执行K线数据的全量更新操作！
    }
  }, ignoreNULL = TRUE, ignoreInit = FALSE)
  
  observeEvent(input$kline_period, {
    new_period <- input$kline_period
    if (!is.null(new_period) && !identical(new_period, state$current_period)) {
      add_debug(paste0("周期: ", state$current_period, " -> ", new_period, "m"))
      state$current_period <- new_period
      inst_id <- state$current_instrument_id
      if (!is.null(inst_id)) {
        state$clear_instrument(inst_id)
        state$tick_counts[[inst_id]] <- 0
        session$sendCustomMessage("clearChart", list(instrument = inst_id))
      }
      show_notification(paste0("周期: ", new_period, "分钟"), "message", 2)
    }
  })
  
  observeEvent(input$push_interval, {
    add_debug(paste0("推送间隔: ", input$push_interval, "s"))
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
    state$clear_all()
    current_tick(NULL)
    
    last_price_data$last <- NULL
    last_price_data$mean <- NULL
    last_price_data$max <- NULL
    last_price_data$min <- NULL
    last_price_data$n <- NULL
    last_price_data$updatetime <- NULL
    
    tryCatch({
      if (!exists("ctpd_async")) stop("ctpd_async 未定义")
      client <- ctpd_async(host = input$host, port = input$port)
      ctp_client <<- client
      is_connected(TRUE)
      add_debug("连接成功")
      show_notification("连接成功", "message", 3)
      start_instrument_updater()
      start_kline_updater()
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
      session$sendCustomMessage("resetAll", list(reason = "disconnect"))
      reset_env(full_reset = TRUE)
      output$price_info <- renderUI({
        tags$div(class = "price-info", tags$div(class = "price-current", "未连接"), tags$div("点击启动"))
      })
      show_notification("已停止", "message", 3)
    } else {
      show_notification("无连接", "warning", 3)
    }
  })
  
  observeEvent(input$refresh_btn, {
    instrument_id <- state$current_instrument_id
    if (!is.null(instrument_id) && !is.null(ctp_client)) {
      add_debug(paste0("刷新: ", instrument_id))
      state$clear_instrument(instrument_id)
      state$tick_counts[[instrument_id]] <- 0
      current_tick(NULL)
      session$sendCustomMessage("clearChart", list(instrument = instrument_id))
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
  
  output$price_info <- renderUI({
    tags$div(class = "price-info", tags$div(class = "price-current", "等待连接..."))
  })
}

# ============ 创建前端文件 ============

create_frontend_files <- function() {
  if (!dir.exists("www")) dir.create("www")
  if (!dir.exists("www/js")) dir.create("www/js")
  if (!dir.exists("www/css")) dir.create("www/css")
  
  # CSS保持原样
  css_content <- '
* { box-sizing: border-box; }

body {
  background-color: #1e1e2f;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  margin: 0;
  padding: 0;
  font-size: 13px;
}

.title-panel {
  display: none;
}

.sidebar-panel {
  background-color: #2d2d3f;
  border-radius: 6px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  height: calc(110vh - 70px);
  overflow-y: auto;
}

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

.price-info {
  background: rgba(0,0,0,0.2);
  border-radius: 4px;
  padding: 8px;
  margin: 0 0 10px 0;
}

.price-current {
  font-size: 14px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid #4a5568;
}

.price-stats-row {
  font-family: "Fira Code", Consolas, monospace;
  font-size: 12px;
  margin: 4px 0;
  line-height: 1.4;
}

.price-label {
  color: #a0aec0;
}

.price-value {
  color: #e0e0e0;
  font-weight: 600;
  margin-right: 8px;
}

.price-meta {
  font-size: 10px;
  color: #718096;
  margin-top: 6px;
  padding-top: 4px;
  border-top: 1px solid #4a5568;
}

.price-meta span {
  margin-right: 12px;
}

.price-meta strong {
  color: #e0e0e0;
}

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

.host-port-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.host-input { flex: 2; }
.port-input { flex: 1; }

.form-control {
  background-color: #1e1e2f !important;
  border: 1px solid #4a5568 !important;
  color: #e0e0e0 !important;
  border-radius: 4px !important;
  padding: 4px 8px !important;
  font-size: 12px !important;
  height: 28px !important;
}

.btn-row {
  display: flex;
  gap: 8px;
}

.btn {
  border: none;
  border-radius: 4px;
  font-weight: 600;
  font-size: 12px;
  padding: 6px 12px;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-danger {
  background: linear-gradient(135deg, #fc8181 0%, #e53e3e 100%);
  color: white;
}

.btn-small { padding: 4px 10px; font-size: 11px; flex: 1; }
.btn-full { width: 100%; margin-top: 6px; }
.btn-tiny { padding: 2px 8px; font-size: 9px; background: #4a5568; color: white; float: right; }

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

.main-panel {
  padding: 0;
}

.chart-container {
  width: 100%;
  height: calc(110vh - 70px);
  background-color: #1e1e2f;
  border: 1px solid #3d3d5c;
  border-radius: 6px;
}

.shiny-notification {
  position: fixed !important;
  top: 15px !important;
  right: 15px !important;
  z-index: 9999;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white !important;
}
'
writeLines(css_content, "www/css/style.css")

# JS文件 - 恢复完整指标
js_content <- '
// chartapp.js - v4.2 恢复完整指标
(function () {
  "use strict";
  
  let chart = null;
  let currentInstrument = null;
  
  function debugLog(msg) {
    const timestamp = new Date().toLocaleTimeString();
    console.log(`[${timestamp}] [Chart]`, msg);
  }
  
  // 注册自定义指标
  function registerIndicators() {
    if (typeof klinecharts === "undefined") return;
    
    try {
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
      debugLog("指标注册完成");
    } catch(e) {
      console.error("指标注册失败", e);
    }
  }
  
  // 初始化图表
  function initChart() {
    if (chart) {
      try { 
        klinecharts.dispose("chart"); 
      } catch(e) {}
      chart = null;
    }
    
    try {
      chart = klinecharts.init("chart");
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
      
      // 创建所有指标
      chart.createIndicator("VOL", false);
      chart.createIndicator("MA", true, { id: "candle_pane" });
      chart.createIndicator("OINT", false);
      chart.createIndicator("KDJ", false);
      chart.createIndicator("MACD", false);
      
      debugLog("图表初始化完成，指标已加载");
    } catch(e) {
      console.error("图表初始化失败", e);
    }
  }
  
  // 更新K线数据
  function updateKline(data) {
    if (!chart) {
      debugLog("图表未初始化");
      return;
    }
    
    if (!data || !data.ds) {
      return;
    }
    
    const klineData = data.ds;
    if (!Array.isArray(klineData) || klineData.length === 0) {
      debugLog(`klineData数据格式错误!
        数据需要采用行记录数组格式: [{timestamp,open,high,low,close,volume,oint}] !
        当前数据: ${JSON.stringify(klineData).substr(0, 100)} ...`);
      return;
    }
    
    try {
      if (data.type === "full") {
        chart.applyNewData(klineData);
        debugLog(`全量更新: ${klineData.length}条K线`);
      } else {
        for (let i = 0; i < klineData.length; i++) {
          chart.updateData(klineData[i]);
        }
        debugLog(`增量更新: ${klineData.length}条K线`);
      }
    } catch(e) {
      console.error("更新K线失败", e);
    }
  }
  
  // 清空图表
  function clearChart() {
    if (!chart) return;
    try {
      chart.applyNewData([], true);
      debugLog("清空图表");
    } catch(e) {}
  }
  
  // 重置
  function resetAll() {
    clearChart();
    currentInstrument = null;
    debugLog("重置完成");
  }
  
  // 切换合约
  function switchInstrument(instrument) {
    if (!instrument) return;
    currentInstrument = instrument;
    clearChart();
    debugLog(`切换合约: ${instrument}`);
  }
  
  // 注册Shiny消息处理
  function initShinyHandlers() {
    if (typeof Shiny === "undefined") {
      console.error("Shiny未加载");
      return;
    }
    
    Shiny.addCustomMessageHandler("updateKline", function(data) {
      if (data.instrument === currentInstrument) {
        updateKline(data);
      }
    });
    
    Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
      switchInstrument(msg.instrument);
    });
    
    Shiny.addCustomMessageHandler("clearChart", function(msg) {
      if (!msg.instrument || msg.instrument === currentInstrument) {
        clearChart();
      }
    });
    
    Shiny.addCustomMessageHandler("resetAll", function() {
      resetAll();
    });
    
    debugLog("消息处理器注册完成");
  }
  
  // 启动
  function start() {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", function() {
        registerIndicators();
        initChart();
        initShinyHandlers();
        debugLog("应用启动完成");
      });
    } else {
      registerIndicators();
      initChart();
      initShinyHandlers();
      debugLog("应用启动完成");
    }
  }
  
  start();
})();
'
writeLines(js_content, "www/js/chartapp.js")

message("前端文件创建完成")
}

create_frontend_files()

shinyApp(ui, server)
