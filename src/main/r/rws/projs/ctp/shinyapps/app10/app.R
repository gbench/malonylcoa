library(shiny)
library(shinydashboard)
library(dplyr)
library(xts)
library(plotly)
library(jsonlite)
library(later)
library(lubridate)
library(DT)

# 加载必要的包
required_packages <- c("later", "jsonlite", "tibble", "dplyr", "xts", "plotly", "shiny", "shinydashboard", "lubridate", "DT")
for(pkg in required_packages) {
  if(!require(pkg, character.only = TRUE)) {
    install.packages(pkg)
    library(pkg, character.only = TRUE)
  }
}

# 修复 compute_kline 函数
compute_kline <- function(dfm) {
  if(nrow(dfm) == 0) return(data.frame())
  
  # 确保有必要的列
  if(!"LastPrice" %in% colnames(dfm) || !"Volume" %in% colnames(dfm)) {
    return(data.frame())
  }
  
  # 创建时间列
  if("ActionDay" %in% colnames(dfm) && "UpdateTime" %in% colnames(dfm)) {
    # 创建完整的时间戳
    dfm$DateTime <- as.POSIXct(paste0(dfm$ActionDay, " ", dfm$UpdateTime), format="%Y%m%d %H:%M:%S")
    # 添加毫秒（如果有的话）
    if("UpdateMillisec" %in% colnames(dfm)) {
      dfm$DateTime <- dfm$DateTime + dfm$UpdateMillisec / 1000
    }
    
    dfm <- dfm[!is.na(dfm$DateTime), ]
    
    if(nrow(dfm) > 0) {
      result <- dfm %>%
        mutate(Minute = lubridate::floor_date(DateTime, "minute")) %>%
        group_by(Minute) %>%
        summarise(
          Open = first(LastPrice),
          High = max(LastPrice),
          Low = min(LastPrice),
          Close = last(LastPrice),
          Volume = sum(Volume, na.rm = TRUE),
          .groups = 'drop'
        ) %>%
        arrange(Minute) %>%
        mutate(Index = Minute)
      
      return(result)
    }
  }
  return(data.frame())
}

# 定义 to.minutes 函数
to.minutes <- function(df, k) {
  if(nrow(df) == 0) return(df)
  if(!"Minute" %in% colnames(df)) return(df)
  
  df$Group <- (seq_len(nrow(df)) - 1) %/% k
  result <- df %>%
    group_by(Group) %>%
    summarise(
      Minute = first(Minute),
      Open = first(Open),
      High = max(High),
      Low = min(Low),
      Close = last(Close),
      Volume = sum(Volume),
      .groups = 'drop'
    ) %>%
    mutate(Index = Minute)
  
  return(result)
}

# UI
ui <- dashboardPage(
  dashboardHeader(title = "CTP 行情监控 - 实时动态刷新"),
  dashboardSidebar(
    sidebarMenu(
      menuItem("实时行情", tabName = "realtime", icon = icon("chart-line")),
      menuItem("K线图", tabName = "kline", icon = icon("candlestick-chart")),
      menuItem("合约状态", tabName = "status", icon = icon("info-circle"))
    )
  ),
  dashboardBody(
    tags$head(
      tags$style(HTML("
        .shiny-notification {
          height: 50px;
          width: 300px;
          position: fixed;
          top: calc(50% - 25px);
          left: calc(50% - 150px);
        }
        .price-up {
          color: red;
          font-weight: bold;
        }
        .price-down {
          color: green;
          font-weight: bold;
        }
        .price-box {
          padding: 15px;
          margin: 10px;
          border-radius: 5px;
          background-color: #f8f9fa;
          text-align: center;
        }
        .latest-price {
          font-size: 24px;
          font-weight: bold;
        }
      "))
    ),
    tabItems(
      # 实时行情标签页
      tabItem(tabName = "realtime",
        fluidRow(
          box(width = 4, title = "连接配置", status = "primary", solidHeader = TRUE,
            textInput("host", "服务器地址", value = "localhost"),
            numericInput("port", "端口", value = 9892, min = 1, max = 65535),
            actionButton("connect_btn", "启动连接", class = "btn-primary", icon = icon("play")),
            actionButton("disconnect_btn", "停止并清空", class = "btn-danger", icon = icon("stop")),
            hr(),
            selectInput("instrument_realtime", "选择合约", choices = NULL),
            hr(),
            h4("最新价格监控"),
            verbatimTextOutput("latest_price_info")
          ),
          box(width = 8, title = "实时价格走势", status = "success", solidHeader = TRUE,
            plotlyOutput("realtime_price_chart", height = "400px"),
            hr(),
            h4("最近20笔Tick数据"),
            DTOutput("recent_ticks")
          )
        )
      ),
      # K线图标签页
      tabItem(tabName = "kline",
        fluidRow(
          box(width = 3, title = "K线设置", status = "primary", solidHeader = TRUE,
            selectInput("instrument_kline", "选择合约", choices = NULL),
            numericInput("k_period", "K线周期(分钟)", value = 1, min = 1, step = 1),
            actionButton("refresh_k", "刷新K线", icon = icon("sync")),
            hr(),
            h4("价格统计"),
            verbatimTextOutput("price_stats")
          ),
          box(width = 9, title = "K线图", status = "success", solidHeader = TRUE,
            plotlyOutput("kline_plot", height = "600px")
          )
        )
      ),
      # 合约状态标签页
      tabItem(tabName = "status",
        fluidRow(
          box(width = 12, title = "运行状态", status = "info", solidHeader = TRUE,
            verbatimTextOutput("status_text")
          )
        )
      )
    )
  )
)

server <- function(input, output, session) {
  
  # 全局变量存储 ctpclient 对象
  ctpclient <- reactiveVal(NULL)
  # 存储当前选择的合约列表
  available_instruments <- reactiveVal(character())
  # 存储最新价格数据
  latest_prices <- reactiveVal(list())
  # 存储价格变化方向
  price_direction <- reactiveVal(list())
  # 使用普通变量存储定时器ID
  timer_id <- NULL
  price_timer_id <- NULL
  # 连接状态标志
  is_connected <- reactiveVal(FALSE)
  
  # 辅助函数: 清空环境 (重新初始化)
  reset_env <- function() {
    client <- ctpclient()
    if(!is.null(client)) {
      tryCatch({
        client$stop()
      }, error = function(e) {
        message("停止连接时出错: ", e$message)
      })
    }
    ctpclient(NULL)
    available_instruments(character())
    latest_prices(list())
    price_direction(list())
    is_connected(FALSE)
    message("环境已重置")
  }
  
  # 停止定时器
  stop_timers <- function() {
    if(!is.null(timer_id)) {
      timer_id <<- NULL
    }
    if(!is.null(price_timer_id)) {
      price_timer_id <<- NULL
    }
  }
  
  # 安全显示通知的函数
  safe_notification <- function(message, type = NULL, duration = 3) {
    tryCatch({
      if(is.null(type)) {
        showNotification(message, duration = duration)
      } else {
        valid_types <- c("default", "message", "warning", "error")
        if(type %in% valid_types) {
          showNotification(message, type = type, duration = duration)
        } else {
          showNotification(message, duration = duration)
        }
      }
    }, error = function(e) {
      message("通知失败: ", message)
    })
  }
  
  # 启动连接
  observeEvent(input$connect_btn, {
    if(!is.null(ctpclient())) {
      showModal(modalDialog(
        title = "提示",
        "当前已有连接，将先停止并清空现有环境后再启动新连接。",
        easyClose = TRUE,
        footer = NULL
      ))
      reset_env()
    }
    
    stop_timers()
    safe_notification("正在连接CTP服务器...")
    
    tryCatch({
      if(!exists("ctpd_async")) {
        stop("ctpd_async 函数未定义，请确保已加载 ctpd.R 文件")
      }
      
      client <- ctpd_async(host = input$host, port = input$port)
      ctpclient(client)
      is_connected(TRUE)
      
      safe_notification("连接成功", type = "success", duration = 3)
      
      # 启动定时更新
      update_instruments_async()
      start_price_monitor()
      
    }, error = function(e) {
      showModal(modalDialog(
        title = "连接失败",
        paste("无法连接到服务器:", e$message),
        easyClose = TRUE,
        footer = modalButton("关闭")
      ))
    })
  })
  
  # 启动价格监控
  start_price_monitor <- function() {
    monitor_prices <- function() {
      client <- isolate(ctpclient())
      if(is.null(client)) {
        return()
      }
      
      # 获取所有合约的最新价格
      tryCatch({
        stats <- client$stats()
        if(length(stats) > 0) {
          current_prices <- list()
          directions <- list()
          
          for(id in names(stats)) {
            s <- stats[[id]]
            if(!is.null(s) && !is.null(s$last)) {
              old_price <- isolate(latest_prices())[[id]]
              current_prices[[id]] <- s$last
              
              # 判断价格方向
              if(!is.null(old_price)) {
                if(s$last > old_price) {
                  directions[[id]] <- "up"
                } else if(s$last < old_price) {
                  directions[[id]] <- "down"
                } else {
                  directions[[id]] <- "same"
                }
              } else {
                directions[[id]] <- "same"
              }
            }
          }
          
          isolate(latest_prices(current_prices))
          isolate(price_direction(directions))
        }
      }, error = function(e) {
        message("监控价格错误: ", e$message)
      })
      
      # 继续监控 - 每0.3秒更新一次，确保实时性
      if(!is.null(isolate(ctpclient()))) {
        price_timer_id <<- later::later(monitor_prices, 0.3)
      }
    }
    
    monitor_prices()
  }
  
  # 异步更新合约列表
  update_instruments_async <- function() {
    client <- isolate(ctpclient())
    if(is.null(client)) return()
    
    instr <- tryCatch({
      if(!is.null(client$instrumentids)) {
        client$instrumentids
      } else {
        character()
      }
    }, error = function(e) {
      message("获取合约列表错误: ", e$message)
      character()
    })
    
    if(length(instr) > 0) {
      isolate(available_instruments(instr))
      updateSelectInput(session, "instrument_realtime", choices = instr, selected = instr[1])
      updateSelectInput(session, "instrument_kline", choices = instr, selected = instr[1])
    }
    
    if(!is.null(isolate(ctpclient()))) {
      timer_id <<- later::later(update_instruments_async, 2)
    }
  }
  
  # 停止并清空
  observeEvent(input$disconnect_btn, {
    if(!is.null(ctpclient())) {
      stop_timers()
      reset_env()
      showModal(modalDialog(
        title = "已停止",
        "CTP连接已停止，TickDataEnv已清空。",
        easyClose = TRUE,
        footer = modalButton("确定")
      ))
    } else {
      safe_notification("当前没有活动的连接")
    }
  })
  
  # 实时价格走势图数据
  realtime_price_data <- reactive({
    req(input$instrument_realtime)
    client <- ctpclient()
    if(is.null(client)) return(NULL)
    
    # 每0.3秒刷新一次
    invalidateLater(300)
    
    tryCatch({
      ticks_df <- client$ticks(input$instrument_realtime)
      if(is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)
      
      # 获取最近100个tick用于走势图
      recent_ticks <- tail(ticks_df, 100)
      
      # 创建时间序列
      if("ActionDay" %in% colnames(recent_ticks) && "UpdateTime" %in% colnames(recent_ticks)) {
        recent_ticks$DateTime <- as.POSIXct(paste0(recent_ticks$ActionDay, " ", recent_ticks$UpdateTime), 
                                           format="%Y%m%d %H:%M:%S")
        if("UpdateMillisec" %in% colnames(recent_ticks)) {
          recent_ticks$DateTime <- recent_ticks$DateTime + recent_ticks$UpdateMillisec / 1000
        }
        
        return(recent_ticks %>% select(DateTime, LastPrice, Volume, UpdateTime))
      }
      return(NULL)
    }, error = function(e) {
      message("获取实时数据错误: ", e$message)
      NULL
    })
  })
  
  # 最新价格信息显示
  output$latest_price_info <- renderPrint({
    req(input$instrument_realtime)
    client <- ctpclient()
    if(is.null(client)) {
      cat("未连接")
      return()
    }
    
    # 每0.3秒刷新
    invalidateLater(300)
    
    tryCatch({
      stats <- client$stats(input$instrument_realtime)
      if(!is.null(stats) && is.list(stats)) {
        direction <- isolate(price_direction())[[input$instrument_realtime]]
        arrow <- switch(direction,
                       "up" = " ↑↑↑",
                       "down" = " ↓↓↓",
                       "same" = " →")
        
        cat("合约: ", input$instrument_realtime, "\n")
        cat("最新价: ", sprintf("%.2f", stats$last), arrow, "\n")
        cat("均价: ", sprintf("%.2f", stats$mean), "\n")
        cat("最高价: ", sprintf("%.2f", stats$max), "\n")
        cat("最低价: ", sprintf("%.2f", stats$min), "\n")
        cat("Tick数: ", stats$n, "\n")
        cat("更新时间: ", stats$updatetime, "\n")
      } else {
        cat("暂无数据")
      }
    }, error = function(e) {
      cat("获取数据失败: ", e$message)
    })
  })
  
  # 实时价格走势图
  output$realtime_price_chart <- renderPlotly({
    df <- realtime_price_data()
    if(is.null(df) || nrow(df) == 0) {
      return(plot_ly() %>% layout(title = "等待数据..."))
    }
    
    p <- plot_ly(df, x = ~DateTime, y = ~LastPrice, 
                 type = 'scatter', mode = 'lines+markers',
                 name = input$instrument_realtime,
                 line = list(color = 'blue', width = 2),
                 marker = list(size = 4)) %>%
      layout(title = paste(input$instrument_realtime, "实时价格走势"),
             xaxis = list(title = "时间", rangeslider = list(visible = TRUE)),
             yaxis = list(title = "价格"),
             hovermode = "x unified")
    
    p
  })
  
  # 最近Tick数据表 - 修复列名问题
  output$recent_ticks <- renderDT({
    df <- realtime_price_data()
    if(is.null(df) || nrow(df) == 0) {
      return(datatable(data.frame(消息 = "暂无数据")))
    }
    
    # 获取最近20个tick
    recent <- tail(df, 20) %>%
      select(DateTime, LastPrice, Volume) %>%
      arrange(desc(DateTime))
    
    # 使用英文列名避免编码问题
    datatable(recent, 
              options = list(
                pageLength = 10, 
                autoWidth = TRUE,
                language = list(
                  url = '//cdn.datatables.net/plug-ins/1.10.11/i18n/Chinese.json'
                )
              ),
              colnames = c("时间", "最新价", "成交量")) %>%
      formatRound(columns = "LastPrice", digits = 2) %>%
      formatRound(columns = "Volume", digits = 0)
  })
  
  # K线数据
  kline_data <- reactive({
    req(input$instrument_kline)
    client <- ctpclient()
    if(is.null(client)) return(NULL)
    
    input$refresh_k
    k_period <- input$k_period
    
    # 每1秒刷新K线数据
    invalidateLater(1000)
    
    isolate({
      tryCatch({
        ticks_df <- client$ticks(input$instrument_kline)
        if(is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)
        
        minute_k <- compute_kline(ticks_df)
        if(is.null(minute_k) || nrow(minute_k) == 0) return(NULL)
        
        if(k_period > 1) {
          result <- to.minutes(minute_k, k_period)
        } else {
          result <- minute_k
        }
        
        return(result)
      }, error = function(e) {
        message("获取K线数据错误: ", e$message)
        NULL
      })
    })
  })
  
  # 价格统计
  output$price_stats <- renderPrint({
    req(input$instrument_kline)
    client <- ctpclient()
    if(is.null(client)) {
      cat("未连接")
      return()
    }
    
    # 每0.5秒刷新
    invalidateLater(500)
    
    tryCatch({
      stats <- client$stats(input$instrument_kline)
      if(!is.null(stats) && is.list(stats)) {
        cat("========== 价格统计 ==========\n")
        cat(sprintf("最新价: %.2f\n", stats$last))
        cat(sprintf("均价: %.2f\n", stats$mean))
        cat(sprintf("标准差: %.2f\n", stats$sd))
        cat(sprintf("最高价: %.2f\n", stats$max))
        cat(sprintf("最低价: %.2f\n", stats$min))
        cat(sprintf("Tick数量: %d\n", stats$n))
        cat(sprintf("最后更新: %s\n", stats$updatetime))
      } else {
        cat("暂无统计数据")
      }
    }, error = function(e) {
      cat("获取统计失败: ", e$message)
    })
  })
  
  # 绘制K线图
  output$kline_plot <- renderPlotly({
    df <- kline_data()
    if(is.null(df) || nrow(df) == 0) {
      return(plot_ly() %>% 
               layout(title = "暂无数据，请等待Tick数据",
                      xaxis = list(title = "时间"),
                      yaxis = list(title = "价格")))
    }
    
    p <- plot_ly(df, x = ~Index, type = "candlestick",
                 open = ~Open, close = ~Close,
                 high = ~High, low = ~Low,
                 name = input$instrument_kline,
                 showlegend = TRUE,
                 increasing = list(line = list(color = "red")),
                 decreasing = list(line = list(color = "green"))) %>%
      layout(title = paste(input$instrument_kline, "K线图 (", input$k_period, "分钟)"),
             xaxis = list(title = "时间", rangeslider = list(visible = FALSE)),
             yaxis = list(title = "价格"),
             hovermode = "x unified")
    
    if("Volume" %in% names(df) && nrow(df) > 0 && sum(df$Volume, na.rm = TRUE) > 0) {
      p <- p %>% add_trace(x = ~Index, y = ~Volume, type = "bar", 
                          name = "成交量", yaxis = "y2",
                          marker = list(color = "lightblue")) %>%
        layout(yaxis2 = list(overlaying = "y", side = "right", 
                            title = "成交量", showgrid = FALSE))
    }
    
    p
  })
  
  # 状态信息
  output$status_text <- renderPrint({
    client <- ctpclient()
    if(is.null(client)) {
      cat("CTP客户端未启动\n")
      cat("请点击'启动连接'按钮连接CTP服务器\n")
      return()
    }
    
    # 每0.5秒刷新
    invalidateLater(500)
    
    tryCatch({
      cat("========== CTP客户端状态 ==========\n")
      cat(sprintf("连接状态: %s\n", ifelse(is_connected(), "✓ 已连接", "✗ 未连接")))
      
      if(!is.null(client$totalticks)) {
        cat(sprintf("总Tick数: %d\n", client$totalticks))
      }
      if(!is.null(client$instrumentids) && length(client$instrumentids) > 0) {
        cat(sprintf("合约数: %d\n", length(client$instrumentids)))
        cat(sprintf("合约列表: %s\n", paste(client$instrumentids, collapse = ", ")))
      }
      if(!is.null(client$lastupdate)) {
        cat(sprintf("最后更新: %s\n", as.character(client$lastupdate)))
      }
      
      cat("\n--- 最新价格快照 ---\n")
      prices <- isolate(latest_prices())
      if(length(prices) > 0) {
        for(id in names(prices)) {
          direction <- isolate(price_direction())[[id]]
          arrow <- switch(direction,
                         "up" = "↑",
                         "down" = "↓",
                         "same" = "→")
          cat(sprintf("%s: %.2f %s\n", id, prices[[id]], arrow))
        }
      } else {
        cat("等待价格数据...\n")
      }
      cat("====================================\n")
    }, error = function(e) {
      cat("获取状态失败:", e$message, "\n")
    })
  })
  
  # 应用退出时清理
  session$onSessionEnded(function() {
    stop_timers()
    client <- isolate(ctpclient())
    if(!is.null(client)) {
      tryCatch({
        client$stop()
      }, error = function(e) {
        message("退出时清理出错: ", e$message)
      })
    }
  })
}

# 检查 ctpd_async 函数是否存在
if(!exists("ctpd_async")) {
  warning("ctpd_async 函数未定义！请确保已加载 ctpd.R 文件")
}

# 运行应用
shinyApp(ui, server)
