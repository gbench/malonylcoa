#' 异步调用 接受ctp daemon2 的数据推送，并把结果保存到本地数据环境envir
#' @param host ctp daemon2服务器/中间件IP地址 
#' @param port ctp daemon2的监听端口 
#' @param envir 本地数据环境 
#' @param delay 下次回调时间
ctpd_async <- function(host="192.168.1.41", port=9898, envir=with(new.env(), {e<-environment(); attr(e, "name") <- "TickDataEnv"; e}), delay=0.1) {
  
  strsplit("later,jsonlite,dplyr", "[,]+") |> unlist() |> sapply(\(x) tryCatch({ # 尝试加载&安装x包
      if(!require(x, character.only=TRUE)) install.packages(x);
      (if(flag) require else library) (x, character.only=TRUE)
    }, error=\(e) e))
  
  # 初始化环境
  envir$instruments <- new.env()
  envir$instrument_ids <- character()
  envir$total_ticks <- 0
  envir$last_update <- Sys.time()
  envir$.running <- FALSE
  
  # arraylist 私有创建函数
  .arraylist <- function(capacity=1000, parent=envir) {
    arrenv <- new.env(T, parent) # 实质
    arrenv$data <- vector("list", capacity)
    arrenv$offset <- 0
    arrenv$.capacity <- capacity
    arrenv$add <- function(e, i=NA) {
      j <- if(is.na(i)) arrenv$offset + 1 else i
      n <- length(arrenv$data)
      if(j > n) {
        length(arrenv$data) <<- arrenv$.capacity * (1 + length(arrenv$data) %/% arrenv$.capacity)
      }
      arrenv$data[[j]] <- e
      arrenv$offset <<- max(arrenv$offset, j)
    }
    arrenv$size <- function() arrenv$offset
    arrenv$aslist <- function() if(arrenv$offset > 0) head(arrenv$data, arrenv$offset) else list()
    arrenv$unlist <- function() base::unlist(arrenv$aslist())
    arrenv
  }
  
  # 添加 tick 函数
  envir$addtick <- function(tickdata) {
    id <- tickdata$InstrumentID # 依据InstrumentID进行分组 
    if(is.null(envir$instruments[[id]])) {
      envir$instruments[[id]] <- .arraylist(1e3)
      envir$instrument_ids <- unique(c(envir$instrument_ids, id))
    }
    envir$instruments[[id]]$add(tickdata) # 记录数据
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

  # 过滤左侧提示符号
  ltrim <- \(x, prompt="^[\\s>]+") if(is.null(prompt)) x else gsub(prompt, "", x=x, perl=T) 

  # 握手
  Sys.sleep(0.3)
  welcome <- readLines(envir$.conn) |> ltrim()
  writeLines("hi", envir$.conn)
  Sys.sleep(0.3)
  loginfo <- readLines(envir$.conn) |> ltrim()
  envir$.session_fd <- sub(".*my friend\\[([0-9]+)\\].*", "\\1", loginfo, perl=TRUE)
  writeLines("dump -1", envir$.conn)
  
  message("CTPD 已连接，session_fd: ", envir$.session_fd)
  
  # 异步读取回调
  envir$.read_callback <- function() {
    if(!envir$.running) return()
    
    # 批量读取所有可用数据
    n_read <- 0
    repeat {
      line <- tryCatch( readLines(envir$.conn, n=1) |> ltrim(), error=function(e) { message(as.character(e)); NULL } )
      if(is.null(line) || length(line) < 1) break # 一直读到出错或是没有数据为止
      
      n_read <- n_read + 1 # 记录循环次数
      if(grepl("^\\{", line)) { # json 标记
        tryCatch(envir$addtick(fromJSON(line)), error=function(e) {})
      }
    } # repeat
    
    # 安排下一次回调
    later::later(envir$.read_callback, delay=delay)
  }
  
  # 启动
  envir$.running <- TRUE
  later::later(envir$.read_callback, delay=delay)
  
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
