# app.R - 完整版：包含K线图表 + 模拟交易功能

# 加载必要的包
required_packages <- c("later", "shiny", "shinydashboard", "dplyr", "jsonlite", "lubridate", "data.table", "purrr", "R6", "svSocket")
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
  
  if (nrow(dt) == 0) return(data.table::data.table())
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
  
  kline[volume < 0, volume := 0]
  data.table::setorder(kline, timestamp)
  kline[, .(timestamp, open, high, low, close, volume, oint)]
}

# 合并K线数据框
merge_kline_dt <- function(base_dt, new_dt) {
  if (is.null(base_dt) || nrow(base_dt) == 0) return(new_dt)
  if (is.null(new_dt) || nrow(new_dt) == 0) return(base_dt)
  
  if (!data.table::is.data.table(base_dt)) base_dt <- data.table::as.data.table(base_dt)
  if (!data.table::is.data.table(new_dt)) new_dt <- data.table::as.data.table(new_dt)
  
  base_dt[new_dt, on = "timestamp", `:=`(
    high   = pmax(high, i.high),
    low    = pmin(low, i.low),
    close  = i.close,
    volume = volume + i.volume,
    oint   = i.oint
  )]
  
  new_rows <- new_dt[!base_dt, on = "timestamp"]
  if (nrow(new_rows) > 0) {
    base_dt <- rbind(base_dt, new_rows)
    data.table::setorder(base_dt, timestamp)
  }
  
  return(base_dt)
}

# 批量递推更新统计量
update_stats_batch <- function(prev_stats, new_prices) {
  if (length(new_prices) == 0) return(prev_stats)
  
  prev_n <- prev_stats$n
  prev_mean <- prev_stats$mean
  prev_M2 <- prev_stats$M2
  prev_min <- prev_stats$min
  prev_max <- prev_stats$max
  
  k <- length(new_prices)
  new_n <- prev_n + k
  
  sum_new <- sum(new_prices)
  new_mean <- (prev_n * prev_mean + sum_new) / new_n
  
  sum_sq_new <- sum((new_prices - new_mean)^2)
  new_M2 <- prev_M2 + sum_sq_new + prev_n * (prev_mean - new_mean)^2
  
  new_min <- min(prev_min, min(new_prices))
  new_max <- max(prev_max, max(new_prices))
  
  list(
    n = new_n,
    mean = new_mean,
    M2 = new_M2,
    sd = if (new_n > 1) sqrt(new_M2 / (new_n - 1)) else 0,
    min = new_min,
    max = new_max,
    last = new_prices[k]
  )
}

# 随机分配可用端口号
rand_port <- function(min_port = 1024, max_port = 65535, max_tries = 100) {
  if (min_port < 1 || max_port > 65535 || min_port > max_port) {
    stop("端口范围无效！请设置 1 ≤ 最小端口 ≤ 最大端口 ≤ 65535")
  }
  if (max_tries < 1) stop("重试次数必须大于0")
  
  for (i in seq_len(max_tries)) {
    random_port <- sample(min_port:max_port, 1)
    
    port_available <- tryCatch({
      con <- suppressWarnings(socketConnection(
        host = "localhost",
        port = random_port,
        server = FALSE,
        blocking = FALSE,
        timeout = 0.1
      ))
      close(con)
      FALSE
    }, error = function(e) {
      TRUE
    })
    
    if (port_available) {
      return(random_port)
    }
  }
  
  warning(sprintf("尝试%d次后未找到可用端口，请扩大端口范围或重试", max_tries))
  return(NULL)
}

# ============ 模拟交易模块 - 期货账户类 ============

FuturesAccount <- R6::R6Class(
  "FuturesAccount",
  
  public = list(
    account_id = NULL,
    bank_balance = 0,
    available = 0,
    equity = 0,
    margin = 0,
    positions = list(),
    transactions = list(),
    position_id_counter = 1,
    
    initialize = function() {
      self$account_id <- sprintf("%06d", sample(0:999999, 1))
      self$bank_balance <- 1000000
      self$available <- 0
      self$equity <- 0
      self$margin <- 0
      self$positions <- list()
      self$transactions <- list()
      self$position_id_counter <- 1
    },
    
    bank_to_futures = function(amount) {
      if (amount <= 0) return(list(success = FALSE, msg = "金额必须大于0"))
      if (amount > self$bank_balance) return(list(success = FALSE, msg = "银行卡余额不足"))
      
      self$bank_balance <- self$bank_balance - amount
      self$available <- self$available + amount
      self$equity <- self$equity + amount
      
      self$add_transaction("银期转账", "银行转期货", amount, self$available)
      
      return(list(success = TRUE, msg = sprintf("成功转入 %.2f 元", amount)))
    },
    
    futures_to_bank = function(amount) {
      if (amount <= 0) return(list(success = FALSE, msg = "金额必须大于0"))
      if (amount > self$available) return(list(success = FALSE, msg = "可用资金不足"))
      
      self$available <- self$available - amount
      self$equity <- self$equity - amount
      self$bank_balance <- self$bank_balance + amount
      
      self$add_transaction("银期转账", "期货转银行", amount, self$available)
      
      return(list(success = TRUE, msg = sprintf("成功转出 %.2f 元", amount)))
    },
    
    open_position = function(instrument_id, side, price, volume, margin_rate = 0.1) {
      if (volume <= 0) return(list(success = FALSE, msg = "手数必须大于0"))
      if (price <= 0) return(list(success = FALSE, msg = "价格无效"))
      
      required_margin <- price * volume * margin_rate
      
      if (required_margin > self$available) {
        return(list(success = FALSE, msg = sprintf("可用资金不足，需要保证金 %.2f", required_margin)))
      }
      
      self$available <- self$available - required_margin
      self$margin <- self$margin + required_margin
      
      position_id <- as.character(self$position_id_counter)
      self$position_id_counter <- self$position_id_counter + 1
      
      position <- list(
        id = position_id,
        instrument = instrument_id,
        side = side,
        volume = volume,
        open_price = price,
        current_price = price,
        margin = required_margin,
        open_time = Sys.time(),
        pnl = 0
      )
      
      self$positions[[position_id]] <- position
      
      self$add_transaction(
        "开仓", 
        sprintf("%s %s", instrument_id, if(side == "long") "买多" else "卖空"),
        volume, 
        self$available,
        price = price,
        margin = required_margin
      )
      
      return(list(
        success = TRUE, 
        msg = sprintf("成功开仓 %s %d手 @ %.2f", 
                      if(side == "long") "买多" else "卖空", 
                      volume, price),
        position_id = position_id
      ))
    },
    
    close_position = function(position_id, price, volume = NULL) {
      if (is.null(self$positions[[position_id]])) {
        return(list(success = FALSE, msg = "持仓不存在"))
      }
      
      position <- self$positions[[position_id]]
      close_volume <- if (is.null(volume)) position$volume else min(volume, position$volume)
      
      if (close_volume <= 0) {
        return(list(success = FALSE, msg = "平仓手数无效"))
      }
      
      if (position$side == "long") {
        pnl <- (price - position$open_price) * close_volume
      } else {
        pnl <- (position$open_price - price) * close_volume
      }
      
      released_margin <- position$margin * (close_volume / position$volume)
      
      self$available <- self$available + released_margin + pnl
      self$equity <- self$equity + pnl
      self$margin <- self$margin - released_margin
      
      if (close_volume >= position$volume) {
        self$positions[[position_id]] <- NULL
      } else {
        position$volume <- position$volume - close_volume
        position$margin <- position$margin - released_margin
        position$pnl <- 0
        self$positions[[position_id]] <- position
      }
      
      self$add_transaction(
        "平仓",
        sprintf("%s %s", position$instrument, if(position$side == "long") "多头平仓" else "空头平仓"),
        close_volume,
        self$available,
        price = price,
        pnl = pnl
      )
      
      return(list(
        success = TRUE,
        msg = sprintf("成功平仓 %d手，盈亏: %.2f", close_volume, pnl),
        pnl = pnl
      ))
    },
    
    update_position_price = function(instrument_id, current_price) {
      total_pnl <- 0
      for (pos_id in names(self$positions)) {
        pos <- self$positions[[pos_id]]
        if (pos$instrument == instrument_id) {
          if (pos$side == "long") {
            pos$pnl <- (current_price - pos$open_price) * pos$volume
          } else {
            pos$pnl <- (pos$open_price - current_price) * pos$volume
          }
          total_pnl <- total_pnl + pos$pnl
          self$positions[[pos_id]] <- pos
        }
      }
      
      self$equity <- self$available + self$margin + total_pnl
      return(total_pnl)
    },
    
    get_margin_usage = function() {
      if (self$equity == 0) return(0)
      return(self$margin / self$equity * 100)
    },
    
    get_total_pnl = function() {
      total <- 0
      for (pos in self$positions) {
        total <- total + (pos$pnl %||% 0)
      }
      return(total)
    },
    
    add_transaction = function(type, detail, amount, available_after, price = NULL, margin = NULL, pnl = NULL) {
      record <- list(
        time = Sys.time(),
        type = type,
        detail = detail,
        amount = amount,
        price = price,
        margin = margin,
        pnl = pnl,
        available_after = available_after
      )
      self$transactions <- c(list(record), self$transactions)[1:100]
    },
    
    get_positions_summary = function() {
      if (length(self$positions) == 0) return(data.frame())
      
      df <- data.frame()
      for (pos in self$positions) {
        side_text <- if(pos$side == "long") "多头" else "空头"
        df <- rbind(df, data.frame(
          instrument = pos$instrument, # 合约
          side = side_text, # 方向
          volume = pos$volume, # 手数
          open_price = round(pos$open_price, 2), # 开仓价
          current_price = round(pos$current_price, 2), # 现价
          pnl = round(pos$pnl, 2), # 浮动盈亏
          margin = round(pos$margin, 2), # 保证金
          id = pos$id, # 持仓ID
          stringsAsFactors = FALSE
        ))
      }
      return(df)
    },
    
    get_summary = function() {
      list(
        account_id = self$account_id,
        bank_balance = round(self$bank_balance, 2),
        available = round(self$available, 2),
        equity = round(self$equity, 2),
        margin = round(self$margin, 2),
        margin_usage = round(self$get_margin_usage(), 2),
        total_pnl = round(self$get_total_pnl(), 2),
        position_count = length(self$positions)
      )
    }
  )
)

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

# ctpd_async2 异步调用客户端
ctpd_async2 <- function(host = "192.168.1.41", port = 9898, delay = 0.3) {
    for (pkg in c("later", "jsonlite", "data.table")) {
        if (!require(pkg, character.only = TRUE)) install.packages(pkg)
        library(pkg, character.only = TRUE)
    }
    
    envir <- new.env()
    envir$instruments <- new.env()
    envir$instrumentids <- character()
    envir$totalticks <- 0
    envir$lastupdate <- Sys.time()
    envir$.running <- FALSE
    
    .create_instrument <- function(capacity = 20000) {
        inst <- new.env()
        inst$.capacity <- capacity
        inst$.offset <- 0
        
        inst$LastPrice <- numeric(capacity)
        inst$Volume <- integer(capacity)
        inst$OpenInterest <- integer(capacity)
        inst$DateTime <- numeric(capacity)
        
        for (i in 1:5) {
            inst[[paste0("AskPrice", i)]] <- numeric(capacity)
            inst[[paste0("AskVolume", i)]] <- integer(capacity)
            inst[[paste0("BidPrice", i)]] <- numeric(capacity)
            inst[[paste0("BidVolume", i)]] <- integer(capacity)
        }
        
        inst$add <- function(tick) {
            j <- inst$.offset + 1
            n <- length(inst$LastPrice)
            
            if (j > n) {
                new_cap <- inst$.capacity * (1 + n %/% inst$.capacity)
                length(inst$LastPrice) <- new_cap
                length(inst$Volume) <- new_cap
                length(inst$OpenInterest) <- new_cap
                length(inst$DateTime) <- new_cap
                
                for (i in 1:5) {
                    length(inst[[paste0("AskPrice", i)]]) <- new_cap
                    length(inst[[paste0("AskVolume", i)]]) <- new_cap
                    length(inst[[paste0("BidPrice", i)]]) <- new_cap
                    length(inst[[paste0("BidVolume", i)]]) <- new_cap
                }
            }
            
            inst$LastPrice[j] <- tick$LastPrice
            inst$Volume[j] <- tick$Volume
            inst$OpenInterest[j] <- tick$OpenInterest %||% 0
            
            for (i in 1:5) {
                inst[[paste0("AskPrice", i)]][j] <- tick[[paste0("AskPrice", i)]] %||% 0
                inst[[paste0("AskVolume", i)]][j] <- tick[[paste0("AskVolume", i)]] %||% 0
                inst[[paste0("BidPrice", i)]][j] <- tick[[paste0("BidPrice", i)]] %||% 0
                inst[[paste0("BidVolume", i)]][j] <- tick[[paste0("BidVolume", i)]] %||% 0
            }
            
            dt_str <- paste(tick$ActionDay, tick$UpdateTime)
            inst$DateTime[j] <- as.numeric(as.POSIXct(dt_str, format = "%Y%m%d %H:%M:%S") + tick$UpdateMillisec / 1000)
            
            inst$.offset <- j
        }
        
        inst$size <- function() inst$.offset
        
        inst$last_tick <- function() {
            if (inst$.offset == 0) return(NULL)
            j <- inst$.offset
            tick <- list(
                LastPrice = inst$LastPrice[j],
                Volume = inst$Volume[j],
                OpenInterest = inst$OpenInterest[j]
            )
            for (i in 1:5) {
                tick[[paste0("AskPrice", i)]] <- inst[[paste0("AskPrice", i)]][j]
                tick[[paste0("AskVolume", i)]] <- inst[[paste0("AskVolume", i)]][j]
                tick[[paste0("BidPrice", i)]] <- inst[[paste0("BidPrice", i)]][j]
                tick[[paste0("BidVolume", i)]] <- inst[[paste0("BidVolume", i)]][j]
            }
            tick
        }
        
        inst$ticks_dt <- function(idx, period_seconds = NULL) {
            dt <- data.table::data.table(
                LastPrice = inst$LastPrice[idx],
                Volume = inst$Volume[idx],
                OpenInterest = inst$OpenInterest[idx]
            )
            
            if (!is.null(period_seconds)) {
                dt$Period <- floor(inst$DateTime[idx] / period_seconds) * period_seconds
            }
            
            dt
        }
        
        inst
    }
    
    envir$addtick <- function(tickdata) {
        id <- tickdata$InstrumentID
        if (is.null(envir$instruments[[id]])) {
            envir$instruments[[id]] <- .create_instrument()
            envir$instrumentids <- unique(c(envir$instrumentids, id))
        }
        envir$instruments[[id]]$add(tickdata)
        envir$totalticks <- envir$totalticks + 1
        envir$lastupdate <- Sys.time()
    }
    
    envir$last_tick <- function(instrument_id) {
        if (is.null(envir$instruments[[instrument_id]])) return(NULL)
        envir$instruments[[instrument_id]]$last_tick()
    }
    
    envir$.conn <- tryCatch({
        socketConnection(host = host, port = port, blocking = FALSE, open = "r+", timeout = 2)
    }, error = function(e) NULL)
    
    if (is.null(envir$.conn)) stop("无法连接到 CTP 服务器")
    
    ltrim <- function(x) gsub("^[\\s>]+", "", x, perl = TRUE)
    flush <- function() flush.connection(envir$.conn)
    
    Sys.sleep(0.3)
    readLines(envir$.conn)
    writeLines("hi", envir$.conn)
    flush()
    Sys.sleep(0.3)
    loginfo <- ltrim(readLines(envir$.conn))
    envir$.sessionfd <- sub(".*my friend\\[([0-9]+)\\].*", "\\1", loginfo, perl = TRUE)
    writeLines("dump -1", envir$.conn)
    flush()
    
    message("CTPD 已连接, sessionfd: ", envir$.sessionfd)
    
    envir$.rdcb <- function() {
        if (!envir$.running) return()
        repeat {
            line <- tryCatch(ltrim(readLines(envir$.conn, n = 1)), error = function(e) NULL)
            if (is.null(line) || length(line) < 1) break
            if (grepl("^\\{", line)) {
                tryCatch(envir$addtick(jsonlite::fromJSON(line)), error = function(e) {})
            }
        }
        later::later(envir$.rdcb, delay = delay)
    }
    
    envir$.running <- TRUE
    later::later(envir$.rdcb, delay = delay)
    
    envir$stop <- function() {
        envir$.running <- FALSE
        if (!is.null(envir$.conn)) {
            tryCatch({
                writeLines(sprintf("undump %s", envir$.sessionfd), envir$.conn)
                flush()
                close(envir$.conn)
            }, error = function(e) {})
        }
        message("CTPD 已停止")
    }
    
    reg.finalizer(envir, function(e) if (!is.null(e$stop)) e$stop(), onexit = TRUE)
    message("CTPD 异步守护进程已启动")
    
    envir
}

# ============ UI模块 ============

ui <- fluidPage(
  tags$head(
    tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css")
  ),
  
  div(class = "title-panel", 
      titlePanel("CTP 实时K线图 - 含模拟交易")),
  
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
      
      # ========== 模拟交易面板 - 优化布局 ==========
      div(class = "control-group trading-panel",
          div(class = "section-title", "📈 模拟交易"),
          
          # 账户摘要 - 网格布局
          div(class = "account-info",
              uiOutput("account_summary_ui")
          ),
          
          # 银期转账 - 紧凑布局
          div(class = "transfer-section",
              div(class = "subsection-title", "银期转账"),
              div(class = "transfer-row",
                  div(class = "transfer-input-group", 
                      numericInput("transfer_amount", "金额", value = 10000, min = 0, step = 1000)
                  ),
                  div(class = "transfer-buttons",
                      actionButton("bank_to_futures_btn", "银行→期货", class = "btn-primary btn-transfer"),
                      actionButton("futures_to_bank_btn", "期货→银行", class = "btn-primary btn-transfer")
                  )
              )
          ),
          
          # 开仓 - 左右布局
          div(class = "trade-section",
              div(class = "subsection-title", "开仓"),
              div(class = "trade-row",
                  div(class = "trade-inputs",
                      div(class = "input-group-small",
                          numericInput("trade_volume", "手数", value = 1, min = 1, step = 1)
                      ),
                      div(class = "input-group-small",
                          numericInput("trade_price", "价格", value = 0, min = 0, step = 1)
                      )
                  ),
                  div(class = "trade-buttons",
                      actionButton("open_long_btn", "买多", class = "btn-long"),
                      actionButton("open_short_btn", "卖空", class = "btn-short")
                  )
              )
          ),
          
          # 平仓 - 左右布局
          div(class = "trade-section",
              div(class = "subsection-title", "平仓"),
              div(class = "trade-row",
                  div(class = "trade-inputs",
                      div(class = "input-group-select",
                          selectInput("close_position_id", "持仓", choices = NULL)
                      ),
                      div(class = "input-group-small",
                          numericInput("close_volume", "手数", value = 1, min = 1, step = 1)
                      )
                  ),
                  div(class = "trade-buttons",
                      actionButton("close_position_btn", "平仓", class = "btn-close")
                  )
              )
          ),
          
          # 持仓列表 - 滚动区域
          div(class = "positions-section",
              div(class = "subsection-title", "📋 当前持仓"),
              div(class = "positions-list",
                  uiOutput("positions_ui")
              )
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
  ctpclient <- NULL
  account <- reactiveVal(NULL)
  is_connected <- reactiveVal(FALSE)
  available_instruments <- character()
  running <- TRUE
  debug_messages <- character()
  current_tick <- reactiveVal(NULL)
  
  # 强制刷新计数器
  refresh_counter <- reactiveVal(0)
  
  last_price_data <- reactiveValues(
    last = NULL, mean = NULL, max = NULL, min = NULL, 
    n = NULL, updatetime = NULL, instrument = NULL
  )
  
  timers <- list(instrument = NULL, kline = NULL)
  
  add_debug <- function(msg) {
    timestamp <- format(Sys.time(), "%H:%M:%OS3")
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
  
  # 强制刷新UI
  force_ui_update <- function() {
    refresh_counter(refresh_counter() + 1)
  }
  
  # 账户摘要数据
  account_summary_data <- reactive({
    refresh_counter()
    acc <- account()
    if (is.null(acc)) return(NULL)
    acc$get_summary()
  })
  
  # 持仓数据
  positions_data <- reactive({
    refresh_counter()
    acc <- account()
    if (is.null(acc)) return(data.frame())
    acc$get_positions_summary()
  })
  
  # 价格数据轮询
  price_data <- reactivePoll(
    intervalMillis = 500, session = session,
    checkFunc = function() {
      if (is.null(ctpclient) || is.null(state$current_instrument_id)) return(NULL) 
      tryCatch(ctpclient$lastupdate, error = function(e) NULL)
    },
    valueFunc = function() {
      inst_id <- state$current_instrument_id
      if (is.null(ctpclient) || is.null(inst_id)) return(NULL) 
      
      inst_entity <- ctpclient$instruments[[inst_id]]
      current_n <- inst_entity$size()
      if (current_n < 1) return(NULL)
      
      prev_stats <- state$attrs[[inst_id]]
      
      if (is.null(prev_stats) || prev_stats$n == 0) {
        all_prices <- inst_entity$LastPrice[1:current_n]
        stats <- list(
          n = current_n,
          mean = mean(all_prices),
          M2 = sum((all_prices - mean(all_prices))^2),
          sd = if (current_n > 1) sd(all_prices) else 0,
          min = min(all_prices),
          max = max(all_prices),
          last = all_prices[current_n]
        )
      } else if (current_n > prev_stats$n) {
        new_prices <- inst_entity$LastPrice[seq(prev_stats$n + 1, current_n)]
        stats <- update_stats_batch(prev_stats, new_prices)
        update_and_send_kline(inst_id, state$current_period)
      } else {
        stats <- prev_stats
      }
      
      state$attrs[[inst_id]] <- stats
      tick <- inst_entity$last_tick()
      
      list(
        stats = list(
          n = stats$n,
          mean = stats$mean,
          sd = stats$sd,
          min = stats$min,
          max = stats$max,
          last = stats$last,
          updatetime = as.POSIXct(inst_entity$DateTime[current_n])
        ),
        tick = tick
      )
    }
  )
  
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
  
  # ========== 模拟交易相关Server逻辑 ==========
  
  # 账户摘要显示 - 网格布局
  output$account_summary_ui <- renderUI({
    summary <- account_summary_data()
    if (is.null(summary)) {
      return(div(class = "account-summary", 
                 style = "text-align: center; color: #718096; padding: 10px;",
                 "等待连接并初始化账户..."))
    }
    
    tags$div(
      class = "account-summary-grid",
      div(class = "summary-item", 
          span(class = "summary-label", "账户"), 
          span(class = "summary-value", summary$account_id)),
      div(class = "summary-item", 
          span(class = "summary-label", "银行卡"), 
          span(class = "summary-value", sprintf("%.0f", summary$bank_balance))),
      div(class = "summary-item", 
          span(class = "summary-label", "可用"), 
          span(class = "summary-value", sprintf("%.0f", summary$available))),
      div(class = "summary-item", 
          span(class = "summary-label", "权益"), 
          span(class = "summary-value", sprintf("%.0f", summary$equity))),
      div(class = "summary-item", 
          span(class = "summary-label", "保证金"), 
          span(class = "summary-value", sprintf("%.0f", summary$margin))),
      div(class = "summary-item", 
          span(class = "summary-label", "使用率"), 
          span(class = "summary-value", 
               style = if(summary$margin_usage > 80) "color: #ef5350;" else "color: #48bb78;",
               sprintf("%.1f%%", summary$margin_usage))),
      div(class = "summary-item", 
          span(class = "summary-label", "盈亏"), 
          span(class = "summary-value", 
               style = if(summary$total_pnl >= 0) "color: #48bb78;" else "color: #ef5350;",
               sprintf("%.0f", summary$total_pnl))),
      div(class = "summary-item", 
          span(class = "summary-label", "持仓"), 
          span(class = "summary-value", paste0(summary$position_count, "手")))
    )
  })
  
  # 持仓列表显示
  last_choices <- reactiveVal(NULL)
  output$positions_ui <- renderUI({
    positions_df <- positions_data()
    
    if (is.null(positions_df) || nrow(positions_df) == 0) {
      updateSelectInput(session, "close_position_id", choices = c("无持仓" = ""))
      return(div(style = "text-align: center; color: #718096; padding: 10px;", "暂无持仓"))
    }
    
    choices <- setNames(positions_df$id, paste0(positions_df$id, "#", positions_df$instrument, " ", positions_df$side, " ", positions_df$volume, "手"))
    # 只在 choices 变化时更新
    if (!identical(choices, last_choices())) {
      # 保持当前选中的值（如果还存在）
      current_selected <- input$close_position_id
      new_selected <- if (current_selected %in% names(choices)) current_selected else NULL
      updateSelectInput(session, "close_position_id", choices = choices, selected = new_selected)
      last_choices(choices)
    }
    
    rows <- lapply(1:nrow(positions_df), function(i) {
      pos <- positions_df[i, ]
      pnl_class <- if(pos$pnl >= 0) "pnl-positive" else "pnl-negative"
      div(
        class = "position-item",
        div(class = "position-header",
            span(class = "position-instrument", pos$instrument),
            span(class = pnl_class, sprintf("%+.0f", pos$pnl))
        ),
        div(class = "position-details",
            span(sprintf("%s#%s %d手 @%.1f", pos$id, pos$side, pos$volume, pos$open_price)),
            span(sprintf("保证金: %.0f", pos$margin))
        )
      )
    })
    
    do.call(tagList, rows)
  })
  
  # 更新交易价格
  observe({
    tick <- current_tick()
    if (!is.null(tick) && !is.null(tick$LastPrice)) {
      isolate({
        updateNumericInput(session, "trade_price", value = round(tick$LastPrice, 2))
      })
    }
  })
  
  # 银期转账 - 银行转期货
  observeEvent(input$bank_to_futures_btn, {
    acc <- account()
    if (is.null(acc)) {
      show_notification("请先连接服务器并初始化账户", "warning")
      return()
    }
    
    amount <- input$transfer_amount
    if (is.na(amount) || amount <= 0) {
      show_notification("请输入有效的转账金额", "warning")
      return()
    }
    
    result <- acc$bank_to_futures(amount)
    
    if (result$success) {
      show_notification(result$msg, "message")
      account(acc)
      force_ui_update()
    } else {
      show_notification(result$msg, "error")
    }
  })
  
  # 银期转账 - 期货转银行
  observeEvent(input$futures_to_bank_btn, {
    acc <- account()
    if (is.null(acc)) {
      show_notification("请先连接服务器并初始化账户", "warning")
      return()
    }
    
    amount <- input$transfer_amount
    if (is.na(amount) || amount <= 0) {
      show_notification("请输入有效的转账金额", "warning")
      return()
    }
    
    result <- acc$futures_to_bank(amount)
    
    if (result$success) {
      show_notification(result$msg, "message")
      account(acc)
      force_ui_update()
    } else {
      show_notification(result$msg, "error")
    }
  })
  
  # 开仓 - 买多
  observeEvent(input$open_long_btn, {
    if (!is_connected()) {
      show_notification("请先连接服务器", "warning")
      return()
    }
    
    acc <- account()
    if (is.null(acc)) {
      show_notification("账户未初始化，请重新连接", "warning")
      return()
    }
    
    instrument_id <- state$current_instrument_id
    if (is.null(instrument_id)) {
      show_notification("请先选择合约", "warning")
      return()
    }
    
    price <- input$trade_price
    volume <- input$trade_volume
    
    if (price <= 0) {
      show_notification("请输入有效的价格", "warning")
      return()
    }
    
    if (volume <= 0) {
      show_notification("请输入有效的手数", "warning")
      return()
    }
    
    result <- acc$open_position(instrument_id, "long", price, volume, margin_rate = 0.1)
    
    if (result$success) {
      show_notification(result$msg, "message")
      account(acc)
      force_ui_update()
    } else {
      show_notification(result$msg, "error")
    }
  })
  
  # 开仓 - 卖空
  observeEvent(input$open_short_btn, {
    if (!is_connected()) {
      show_notification("请先连接服务器", "warning")
      return()
    }
    
    acc <- account()
    if (is.null(acc)) {
      show_notification("账户未初始化，请重新连接", "warning")
      return()
    }
    
    instrument_id <- state$current_instrument_id
    if (is.null(instrument_id)) {
      show_notification("请先选择合约", "warning")
      return()
    }
    
    price <- input$trade_price
    volume <- input$trade_volume
    
    if (price <= 0) {
      show_notification("请输入有效的价格", "warning")
      return()
    }
    
    if (volume <= 0) {
      show_notification("请输入有效的手数", "warning")
      return()
    }
    
    result <- acc$open_position(instrument_id, "short", price, volume, margin_rate = 0.1)
    
    if (result$success) {
      show_notification(result$msg, "message")
      account(acc)
      force_ui_update()
    } else {
      show_notification(result$msg, "error")
    }
  })
  
  # 平仓
  observeEvent(input$close_position_btn, {
    if (!is_connected()) {
      show_notification("请先连接服务器", "warning")
      return()
    }
    
    acc <- account()
    if (is.null(acc)) {
      show_notification("账户未初始化，请重新连接", "warning")
      return()
    }
    
    position_id <- input$close_position_id
    if (is.null(position_id) || position_id == "") {
      show_notification("请选择要平仓的持仓", "warning")
      return()
    }
    
    price <- input$trade_price
    volume <- input$close_volume
    
    if (price <= 0) {
      show_notification("请输入有效的价格", "warning")
      return()
    }
    
    if (volume <= 0) {
      show_notification("请输入有效的手数", "warning")
      return()
    }
    
    result <- acc$close_position(position_id, price, volume)
    
    if (result$success) {
      show_notification(result$msg, "message")
      account(acc)
      force_ui_update()
    } else {
      show_notification(result$msg, "error")
    }
  })
  
  # 定时更新持仓价格
  observe({
    invalidateLater(500, session)
    
    acc <- account()
    if (!is.null(acc) && is_connected() && !is.null(state$current_instrument_id) && !is.null(ctpclient)) {
      inst_id <- state$current_instrument_id
      inst_entity <- ctpclient$instruments[[inst_id]]
      if (!is.null(inst_entity) && inst_entity$size() > 0) {
        current_price <- inst_entity$LastPrice[inst_entity$size()]
        if (!is.na(current_price) && current_price > 0) {
          old_equity <- acc$equity
          acc$update_position_price(inst_id, current_price)
          if (old_equity != acc$equity) {
            account(acc)
            force_ui_update()
          }
        }
      }
    }
  })
  
  # ========== 清理函数 ==========
  send_undump_command <- function() {
    if (is.null(ctpclient)) return(FALSE)
    tryCatch({
      if (!is.null(ctpclient$.sessionfd) && !is.null(ctpclient$.conn)) {
        undump_cmd <- sprintf("undump %s", ctpclient$.sessionfd)
        add_debug(paste0("发送 undump: ", undump_cmd))
        writeLines(undump_cmd, ctpclient$.conn)
        response <- base::readLines(ctpclient$.conn, n = 1, warn = FALSE)
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
    if (is.null(ctpclient)) return()
    tryCatch({
      ctpclient$stop()
      add_debug("客户端已停止")
    }, error = function(e) {
      add_debug(paste0("停止客户端出错: ", e$message))
    })
  }
  
  reset_env <- function(full_reset = TRUE) {
    add_debug("开始清理...")
    if (!is.null(ctpclient)) {
      send_undump_command()
      stop_client()
    }
    ctpclient <<- NULL
    is_connected(FALSE)
    available_instruments <<- character()
    current_tick(NULL)
    account(NULL)
    
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
  
  # K线更新
  update_and_send_kline <- function(instrument_id, period, flag = FALSE) {
    if (is.null(ctpclient)) return(NULL)
    
    begtime <- Sys.time()
    
    tryCatch({
      inst_entity <- ctpclient$instruments[[instrument_id]]
      if (is.null(inst_entity)) return(NULL)
      
      current_count <- inst_entity$size()
      last_count <- state$tick_counts[[instrument_id]] %||% 0
      
      if (!flag && current_count == last_count) return(NULL)
      
      if (flag || last_count == 0) {
          idx <- 1:current_count
          add_debug(paste0("全量tick: ", current_count, "条"))
      } else {
          idx <- (last_count + 1):current_count
          add_debug(paste0("新增tick: ", current_count - last_count, "条"))
      }
      
      new_kline_segment <- aggregate_kline(inst_entity$ticks_dt(idx, period * 60))
      
      if (is.null(new_kline_segment) || nrow(new_kline_segment) == 0) return(NULL)
      
      base_kline <- state$get_kline_dt(instrument_id)
      if (is.null(base_kline) || nrow(base_kline) == 0) {
          final_kline <- new_kline_segment
      } else {
          final_kline <- merge_kline_dt(base_kline, new_kline_segment)
      }
      
      state$set_kline_dt(instrument_id, final_kline)
      state$tick_counts[[instrument_id]] <- current_count
      
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
  
  # 合约列表更新
  start_instrument_updater <- function() {
    update_instruments <- function() {
      if (!running || is.null(ctpclient)) return()
      tryCatch({
        instrument_ids <- if (!is.null(ctpclient$instrumentids)) ctpclient$instrumentids else character()
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
      if (running && !is.null(ctpclient)) {
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
      update_and_send_kline(new_instrument_id, input$kline_period, TRUE)
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
    if (!is.null(ctpclient)) {
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
    
    # 初始化期货账户
    new_account <- FuturesAccount$new()
    account(new_account)
    add_debug(paste0("期货账户已创建: ", new_account$account_id))
    add_debug(paste0("初始银行卡余额: ", new_account$bank_balance))
    show_notification(paste0("期货账户: ", new_account$account_id), "message", 3)
    
    last_price_data$last <- NULL
    last_price_data$mean <- NULL
    last_price_data$max <- NULL
    last_price_data$min <- NULL
    last_price_data$n <- NULL
    last_price_data$updatetime <- NULL

    registerapp <- \(app, envir = .GlobalEnv) {
      if (is.null(.GlobalEnv$apps)) envir$apps <- new.env(hash = TRUE)
      appkey <- gettextf("APP%05d", length(envir$apps) + 1)
      assign(appkey, app, envir = envir$apps)
    }
    
    tryCatch({
      if (!exists("ctpd_async2")) stop("ctpd_async2 未定义")
      ctpclient <<- ctpd_async2(host = input$host, port = input$port)
      registerapp(list(ctpclient = ctpclient, state = state, account = new_account))
      is_connected(TRUE)
      add_debug("连接成功")
      show_notification("连接成功", "message", 3)
      start_instrument_updater()
      force_ui_update()
    }, error = function(e) {
      is_connected(FALSE)
      account(NULL)
      add_debug(paste0("连接失败: ", e$message))
      showModal(modalDialog(title = "连接失败", paste("错误:", e$message), easyClose = TRUE, footer = modalButton("关闭")))
    })
  }
  
  observeEvent(input$disconnect_btn, {
    if (!is.null(ctpclient)) {
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
    if (!is.null(instrument_id) && !is.null(ctpclient)) {
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
    if (!is.null(ctpclient)) {
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
  
  # CSS文件 - 优化后的样式
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

.subsection-title {
  font-size: 10px;
  color: #a0aec0;
  font-weight: 500;
  margin-bottom: 6px;
  padding-bottom: 2px;
  border-bottom: 1px solid #3d3d5c;
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

/* 交易面板专用按钮样式 */
.btn-long {
  background: linear-gradient(135deg, #26a69a 0%, #00897b 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  font-size: 12px;
  padding: 6px 16px;
  cursor: pointer;
  flex: 1;
  text-align: center;
}

.btn-long:hover {
  background: linear-gradient(135deg, #2bbbad 0%, #009688 100%);
}

.btn-short {
  background: linear-gradient(135deg, #ef5350 0%, #e53935 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  font-size: 12px;
  padding: 6px 16px;
  cursor: pointer;
  flex: 1;
  text-align: center;
}

.btn-short:hover {
  background: linear-gradient(135deg, #ff6659 0%, #ff3d3d 100%);
}

.btn-close {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  font-size: 12px;
  padding: 6px 16px;
  cursor: pointer;
  width: 100%;
  text-align: center;
}

.btn-transfer {
  flex: 1;
  text-align: center;
  padding: 4px 8px;
  font-size: 11px;
}

/* 账户摘要网格布局 */
.account-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  background: rgba(0,0,0,0.3);
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 10px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10px;
}

.summary-label {
  color: #a0aec0;
}

.summary-value {
  color: #e0e0e0;
  font-weight: 600;
  font-family: "Fira Code", Consolas, monospace;
}

/* 转账区域 */
.transfer-section {
  margin-bottom: 12px;
}

.transfer-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transfer-input-group {
  width: 100%;
}

.transfer-input-group .form-control {
  width: 100%;
}

.transfer-buttons {
  display: flex;
  gap: 8px;
}

/* 交易区域 */
.trade-section {
  margin-bottom: 12px;
}

.trade-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trade-inputs {
  display: flex;
  gap: 8px;
}

.input-group-small {
  flex: 1;
}

.input-group-small .form-control {
  width: 100%;
}

.input-group-select {
  flex: 3;
}

.input-group-select .form-control {
  width: 100%;
}

.trade-buttons {
  display: flex;
  gap: 8px;
}

/* 持仓区域 */
.positions-section {
  margin-top: 8px;
}

.positions-list {
  max-height: 140px;
  overflow-y: auto;
  background: rgba(0,0,0,0.2);
  border-radius: 4px;
  padding: 6px;
}

.position-item {
  border-bottom: 1px solid #3d3d5c;
  padding: 6px 0;
}

.position-item:last-child {
  border-bottom: none;
}

.position-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;
}

.position-instrument {
  font-weight: bold;
  font-size: 11px;
  color: #667eea;
}

.pnl-positive {
  color: #48bb78;
  font-size: 10px;
  font-weight: 600;
}

.pnl-negative {
  color: #ef5350;
  font-size: 10px;
  font-weight: 600;
}

.position-details {
  display: flex;
  justify-content: space-between;
  font-size: 9px;
  color: #a0aec0;
}

/* 滚动条样式 */
.positions-list::-webkit-scrollbar,
.debug-panel::-webkit-scrollbar {
  width: 4px;
}

.positions-list::-webkit-scrollbar-track,
.debug-panel::-webkit-scrollbar-track {
  background: #2d2d3f;
}

.positions-list::-webkit-scrollbar-thumb,
.debug-panel::-webkit-scrollbar-thumb {
  background: #667eea;
  border-radius: 2px;
}

/* 其他组件样式 */
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

/* 响应式调整 */
@media (min-width: 1281px) {
  .transfer-row {
    flex-direction: row;
    align-items: center;
  }
  
  .transfer-input-group {
    flex: 2;
  }
  
  .transfer-buttons {
    flex: 3;
  }
  
  .trade-row {
    flex-direction: row;
    align-items: center;
  }
  
  .trade-inputs {
    flex: 4;
  }
  
  .trade-buttons {
    flex: 1;
  }
  
  .account-summary-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
'
  writeLines(css_content, "www/css/style.css")
  
  # JS文件
  js_content <- '
// chartapp.js - v4.2 完整指标 + 模拟交易
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
      debugLog(`klineData数据格式错误!`);
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

# ========== CTP R控制台 ==========
later::later(\() {
  svskt_port <- rand_port()
  if (is.null(svskt_port)) {
    message(gettextf("无法分配有效端口号，放弃启动CTP R CONSOLE！"))
    return (NULL)
  }
  prompt_str <- "R > "
  welcomed <- new.env(parent = emptyenv())
  welcome_msg <- paste0("CTP R CONSOLE V1.0\n", Sys.time())

  try(svSocket::stopSocketServer(port = svskt_port), silent = TRUE)
  
  svSocket::startSocketServer(
    port = svskt_port,
    procfun = \(msg, socket, server_port, ...) {
      key <- paste0(server_port, "_", socket)
      if (!exists(key, envir = welcomed)) {
        assign(key, TRUE, envir = welcomed)
        svSocket::par_socket_server( client = socket, server_port = server_port, bare = FALSE, prompt = prompt_str, continue = "+ ")
      }
      res <- svSocket::process_socket_server(msg, socket, server_port, ...)
      send_socket_clients(prompt_str, sockets = socket, server_port = server_port)
      res
    },
    local = TRUE
  )
  
  tcl_cmd <- sprintf('
    rename sock_accept_%1$d __orig_sock_accept_%1$d
    proc sock_accept_%1$d {sock addr port} {
      __orig_sock_accept_%1$d $sock $addr $port
      catch {
        puts $sock "%2$s"
        puts -nonewline $sock "%3$s"
        flush $sock
      }
    }
  ', svskt_port, welcome_msg, prompt_str)
  try(tcltk::tcl("eval", tcl_cmd), silent = TRUE)
  
  message("CTP R CONSOLE Listening on ", svskt_port, "\n\n")
}, delay = 3)

shinyApp(ui, server)
