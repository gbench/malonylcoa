#' 异步调用 接受ctp daemon2 的数据推送，并把结果保存到本地数据环境envir
#' @param host ctp daemon2服务器/中间件IP地址 
#' @param port ctp daemon2的监听端口 
#' @param envir 本地数据环境 
#' @param delay 下次回调时间
ctpd_async <- function(host="192.168.1.41", port=9898, envir=with(new.env(), { e <- environment(); attr(e, "name") <- "TickDataEnv"; e }), delay=0.3) {
  
  strsplit("later,jsonlite,tibble,dplyr,xts", "[,]+") |> unlist() |> sapply(\(x) tryCatch({ # 尝试加载&安装x包
      if(!require(x, character.only=TRUE)) install.packages(x);
      flag <- T # require library 的选择标志 
      (if(flag) require else library) (x, character.only=TRUE)
    }, error=\(e) e))
  
  # 初始化环境
  envir$instruments <- new.env()
  envir$instrumentids <- character()
  envir$totalticks <- 0
  envir$lastupdate <- Sys.time()
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
        length(arrenv$data) <<- arrenv$.capacity * (1 + n %/% arrenv$.capacity)
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
    if(is.null(envir$instruments[[id]])) { # 新合约数据
      envir$instruments[[id]] <- .arraylist(1e3)
      envir$instrumentids <- unique(c(envir$instrumentids, id))
    }
    envir$instruments[[id]]$add(tickdata) # 记录数据
    envir$totalticks <- envir$totalticks + 1
    envir$lastupdate <- Sys.time()
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
  flush <- \() flush.connection(envir$.conn) 

  # 握手
  Sys.sleep(0.3)
  welcome <- readLines(envir$.conn) |> ltrim()
  writeLines("hi", envir$.conn); flush()
  Sys.sleep(0.3)
  loginfo <- readLines(envir$.conn) |> ltrim()
  envir$.sessionfd <- sub(".*my friend\\[([0-9]+)\\].*", "\\1", loginfo, perl=TRUE)
  writeLines("dump -1", envir$.conn); flush()
  
  message("CTPD 已连接，sessionfd: ", envir$.sessionfd)
  
  # 异步读取回调 read callback
  envir$.rdcb <- function() {
    if(!envir$.running) return()
    
    # 批量读取所有可用数据
    nread <- 0
    repeat {
      line <- tryCatch( readLines(envir$.conn, n=1) |> ltrim(), error=function(e) { message(as.character(e)); NULL } )
      if(is.null(line) || length(line) < 1) break # 一直读到出错或是没有数据为止
      
      nread <- nread + 1 # 记录循环次数
      if(grepl("^\\{", line)) { # json 标记
        tryCatch(envir$addtick(jsonlite::fromJSON(line)), error=function(e) {})
      }
    } # repeat
    
    # 安排下一次递归回调
    later::later(envir$.rdcb, delay=delay)
  } # .rdcb
  
  # 启动
  envir$.running <- TRUE
  later::later(envir$.rdcb, delay=delay)
  
  # 停止函数
  envir$stop <- function() {
    envir$.running <- FALSE
    if(!is.null(envir$.conn)) {
      tryCatch({
        writeLines(sprintf("undump %s", envir$.sessionfd), envir$.conn); flush()
        close(envir$.conn)
      }, error=function(e) {})
      envir$.conn <- NULL
    }
    message("CTPD 已停止")
  }
  
  # 查询函数
  envir$ticks <- function(instrumentid) {
    if(is.null(envir$instruments[[instrumentid]])) return(tibble::tibble())
    envir$instruments[[instrumentid]]$aslist() |> do.call(dplyr::bind_rows, args=_) |> tibble::as_tibble()
  }

  # 时间序列
  envir$tickdata <- function(instrumentid) {
    dfm <- envir$ticks(instrumentid)
    ind <- as.POSIXct(paste0(dfm$ActionDay, " ", dfm$UpdateTime, ".", dfm$UpdateMillisec), format="%Y%m%d %H:%M:%S:%OS")
    dfm |> mutate(Index=ind)
  }

  # 时间序列
  envir$ts <- function(instrumentid) envir$tickdata(instrumentid) |> with(as.xts(data.frame(LastPrice=LastPrice,Volume=Volume), order.by=Index))

  # K线
  envir$ohlc <- function(instrumentid, k=1) {
    dfm <- envir$ticks(instrumentid)
    kline1m <- compute_kline(dfm |> mutate(Id=seq(nrow(dfm)))) 
    if(k<1) kline1m else to.minutes(kline1m, k)
  }

  # 价格统计 
  envir$stats <- function(instrumentid=NULL) {
    do_stat <- \(id) { # 根据id进行统计
      ticks <- envir$ticks(id)
      if (!inherits(ticks, "data.frame") || nrow(ticks) == 0) return(NULL)
      if (!"LastPrice" %in% colnames(ticks)) return(NULL)
      lastprices <- ticks$LastPrice
      n <- length(lastprices)
      list(
        n = n,
        mean = mean(lastprices),
        sd = sd(lastprices),
        min = min(lastprices),
        max = max(lastprices),
        last = utils::tail(lastprices, 1),
        updatetime = ticks$UpdateTime[n]
      ) # list
    } # do_stat

    if(is.null(instrumentid)) envir$instrumentids |> setNames(nm=_) |> lapply(do_stat)
    else do_stat(instrumentid)
  }
  
  envir$status <- function() {
    cat("\n========== Tick Data Status ==========\n")
    cat(sprintf("总Tick数: %d\n", envir$totalticks))
    cat(sprintf("合约数: %d\n", length(envir$instrumentids)))
    cat(sprintf("最后更新: %s\n", envir$lastupdate))
    cat("\n合约统计:\n")
    stats <- envir$stats()
    for(id in names(stats)) {
      s <- stats[[id]]
      if(!is.null(s)) cat(sprintf("  %s: n=%d, last=%.2f, mean=%.2f, sd=%.2f\n", id, s$n, s$last, s$mean, s$sd))
    }
    cat("=======================================\n")
  }
  
  # 清理
  reg.finalizer(envir, function(e) if(!is.null(e$stop)) e$stop(), onexit=TRUE)
  
  message("CTPD 异步守护进程已启动")
  envir
}

if(F) {
  ctpclient <- ctpd_async()

  ctpclient |> with({ #  状态查看
    status()
    stats("ao2609")
    ticks("ao2609")
  })

  ctpclient$stop()
}
