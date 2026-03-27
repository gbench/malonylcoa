ctpd_async <- function(host="192.168.1.41", port=9898,  envir=with(new.env(), {e<-environment(); attr(e, "name") <- "TickDataEnv"; e})) {
  
  c("later", "jsonlite", "dplyr") |> sapply(\(x) tryCatch({ #尝试加载&安装x包
      if(!require(x, character.only=TRUE)) install.packages(x);
      (if(flag) require else library) (x, character.only=TRUE)
    }, error=\(e) e))
  
  # 初始化环境
  envir$instruments <- new.env()
  envir$instrument_ids <- character()
  envir$total_ticks <- 0
  envir$last_update <- Sys.time()
  envir$.running <- FALSE
  
  # arraylist 创建函数
  make_arraylist <- function(capacity=1000) {
    env <- new.env()
    env$data <- vector("list", capacity)
    env$offset <- 0
    env$.capacity <- capacity
    env$add <- function(e, i=NA) {
      j <- if(is.na(i)) env$offset + 1 else i
      n <- length(env$data)
      if(j > n) {
        length(env$data) <<- env$.capacity * (1 + length(env$data) %/% env$.capacity)
      }
      env$data[[j]] <- e
      env$offset <<- max(env$offset, j)
    }
    env$size <- function() env$offset
    env$aslist <- function() if(env$offset > 0) head(env$data, env$offset) else list()
    env$unlist <- function() base::unlist(env$aslist())
    env
  }
  
  # 添加 tick 函数
  envir$add_tick <- function(tick_data) {
    inst_id <- tick_data$InstrumentID
    if(is.null(envir$instruments[[inst_id]])) {
      envir$instruments[[inst_id]] <- make_arraylist(1000)
      envir$instrument_ids <- unique(c(envir$instrument_ids, inst_id))
    }
    envir$instruments[[inst_id]]$add(tick_data)
    envir$total_ticks <- envir$total_ticks + 1
    envir$last_update <- Sys.time()
  }
  
  # 创建非阻塞连接
  envir$.conn <- tryCatch({
    socketConnection(host=host, port=port, blocking=FALSE, open="r+", timeout=2)
  }, error=function(e) NULL)
  
  if(is.null(envir$.conn)) {
    stop("无法连接到 CTP 服务器")
  }
  
  # 握手
  Sys.sleep(0.3)
  welcome <- readLines(envir$.conn)
  writeLines("hi", envir$.conn)
  Sys.sleep(0.3)
  loginfo <- readLines(envir$.conn)
  envir$.session_fd <- sub(".*my friend\\[([0-9]+)\\].*", "\\1", loginfo, perl=TRUE)
  writeLines("dump -1", envir$.conn)
  
  message("CTPD 已连接，session_fd: ", envir$.session_fd)
  
  # 异步读取回调
  envir$.read_callback <- function() {
    if(!envir$.running) return()
    
    # 批量读取所有可用数据
    n_read <- 0
    repeat {
      line <- tryCatch(readLines(envir$.conn, n=1), error=function(e) NULL)
      if(is.null(line) || length(line) == 0) break
      
      n_read <- n_read + 1
      if(grepl("^\\{", line)) {
        tryCatch({
          tick <- jsonlite::fromJSON(line)
          envir$add_tick(tick)
        }, error=function(e) {})
      }
    }
    
    # 安排下一次回调（10ms后，可根据需要调整）
    later::later(envir$.read_callback, delay=0.01)
  }
  
  # 启动
  envir$.running <- TRUE
  later::later(envir$.read_callback, delay=0.1)
  
  # 停止函数
  envir$stop <- function() {
    envir$.running <- FALSE
    if(!is.null(envir$.conn)) {
      tryCatch({
        writeLines(sprintf("undump %s", envir$.session_fd), envir$.conn)
        close(envir$.conn)
      }, error=function(e) {})
      envir$.conn <- NULL
    }
    message("CTPD 已停止")
  }
  
  # 查询函数
  envir$get_ticks <- function(instrument_id) {
    if(is.null(envir$instruments[[instrument_id]])) return(list())
    envir$instruments[[instrument_id]]$aslist() |> do.call(bind_rows, args=_) |> as_tibble()
  }
  
  envir$get_stats <- function(instrument_id=NULL) {
    if(is.null(instrument_id)) {
      stats <- list()
      for(id in envir$instrument_ids) {
        ticks <- envir$get_ticks(id)
        if(length(ticks) > 0) {
          last_prices <- ticks$LastPrice
          n <- length(ticks)
          stats[[id]] <- list(
            n = length(ticks),
            mean = mean(last_prices),
            sd = sd(last_prices),
            min = min(last_prices),
            max = max(last_prices),
            last = utils::tail(last_prices, 1),
            update_time = ticks$UpdateTime[n]
          )
        }
      }
      return(stats)
    } else {
      ticks <- envir$get_ticks(instrument_id)
      if(length(ticks) == 0) return(NULL)
      last_prices <- ticks$LastPrice
      n <- length(last_prices)
      list(
        n = n,
        mean = mean(last_prices),
        sd = sd(last_prices),
        min = min(last_prices),
        max = max(last_prices),
        last = utils::tail(last_prices, 1),
        update_time = ticks$UpdateTime[n]
      )
    }
  }
  
  envir$print_status <- function() {
    cat("\n========== Tick Data Status ==========\n")
    cat(sprintf("总Tick数: %d\n", envir$total_ticks))
    cat(sprintf("合约数: %d\n", length(envir$instrument_ids)))
    cat(sprintf("最后更新: %s\n", envir$last_update))
    cat("\n合约统计:\n")
    stats <- envir$get_stats()
    for(id in names(stats)) {
      s <- stats[[id]]
      cat(sprintf("  %s: n=%d, last=%.2f, mean=%.2f, sd=%.2f\n", id, s$n, s$last, s$mean, s$sd))
    }
    cat("=======================================\n")
  }
  
  # 清理
  reg.finalizer(envir, function(e) if(!is.null(e$stop)) e$stop(), onexit=TRUE)
  
  message("CTPD 异步守护进程已启动")
  envir
}

if(F) {
  env <- ctpd_async()
  env$print_status()
  env$get_stats("ao2609")
  env$get_ticks("ao2609")
  env$stop()
}
