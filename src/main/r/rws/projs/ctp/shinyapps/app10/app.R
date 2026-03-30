library(shiny)
library(shinydashboard)
library(dplyr)
library(xts)
library(plotly)
library(jsonlite)
library(later)
library(lubridate)

# 加载必要的包
required_packages <- c("later", "jsonlite", "tibble", "dplyr", "xts", "plotly", "shiny", "shinydashboard", "lubridate")
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
  dashboardHeader(title = "CTP 行情监控"),
  dashboardSidebar(
    sidebarMenu(
      menuItem("K线图", tabName = "kline", icon = icon("chart-line")),
      menuItem("状态", tabName = "status", icon = icon("info-circle"))
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
      "))
    ),
    tabItems(
      tabItem(tabName = "kline",
        fluidRow(
          box(width = 4, title = "连接配置", status = "primary", solidHeader = TRUE,
            textInput("host", "服务器地址", value = "localhost"),
            numericInput("port", "端口", value = 9892, min = 1, max = 65535),
            actionButton("connect_btn", "启动连接", class = "btn-primary", icon = icon("play")),
            actionButton("disconnect_btn", "停止并清空", class = "btn-danger", icon = icon("stop")),
            hr(),
            selectInput("instrument", "选择合约", choices = NULL),
            numericInput("k_period", "K线周期(分钟)", value = 1, min = 1, step = 1),
            actionButton("refresh_k", "刷新K线", icon = icon("sync"))
          ),
          box(width = 8, title = "K线图", status = "success", solidHeader = TRUE,
            plotlyOutput("kline_plot", height = "600px")
          )
        )
      ),
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
  # 使用普通变量存储定时器ID
  timer_id <- NULL
  # 连接状态标志
  is_connected <- reactiveVal(FALSE)
  
  # 辅助函数: 清空环境 (重新初始化)
  reset_env <- function() {
    client <- ctpclient()
    if(!is.null(client)) {
      # 停止并清理连接
      tryCatch({
        client$stop()
      }, error = function(e) {
        message("停止连接时出错: ", e$message)
      })
    }
    # 将ctpclient设为NULL
    ctpclient(NULL)
    available_instruments(character())
    is_connected(FALSE)
    message("环境已重置")
  }
  
  # 停止定时器
  stop_timer <- function() {
    if(!is.null(timer_id)) {
      timer_id <<- NULL
    }
  }
  
  # 安全显示通知的函数
  safe_notification <- function(message, type = NULL, duration = 3) {
    tryCatch({
      if(is.null(type)) {
        showNotification(message, duration = duration)
      } else {
        # 只允许有效的type值
        valid_types <- c("default", "message", "warning", "error")
        if(type %in% valid_types) {
          showNotification(message, type = type, duration = duration)
        } else {
          showNotification(message, duration = duration)
        }
      }
    }, error = function(e) {
      # 如果通知失败，只打印到控制台
      message("通知失败: ", message)
    })
  }
  
  # 启动连接
  observeEvent(input$connect_btn, {
    # 如果已有连接，先停止并清空
    if(!is.null(ctpclient())) {
      showModal(modalDialog(
        title = "提示",
        "当前已有连接，将先停止并清空现有环境后再启动新连接。",
        easyClose = TRUE,
        footer = NULL
      ))
      reset_env()
    }
    
    # 停止旧的定时器
    stop_timer()
    
    # 显示连接中提示
    safe_notification("正在连接CTP服务器...")
    
    # 尝试连接
    tryCatch({
      # 确保 ctpd_async 函数存在
      if(!exists("ctpd_async")) {
        stop("ctpd_async 函数未定义，请确保已加载 ctpd.R 文件")
      }
      
      client <- ctpd_async(host = input$host, port = input$port)
      ctpclient(client)
      is_connected(TRUE)
      
      # 显示成功通知
      safe_notification("连接成功", type = "success", duration = 3)
      
      # 启动定时更新合约列表
      update_instruments_async()
      
    }, error = function(e) {
      showModal(modalDialog(
        title = "连接失败",
        paste("无法连接到服务器:", e$message),
        easyClose = TRUE,
        footer = modalButton("关闭")
      ))
    })
  })
  
  # 异步更新合约列表
  update_instruments_async <- function() {
    # 检查是否还有活动的连接
    client <- isolate(ctpclient())
    if(is.null(client)) {
      return()
    }
    
    # 从环境中获取合约ID列表
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
      # 在shiny会话中更新UI
      updateSelectInput(session, "instrument", choices = instr, selected = instr[1])
    }
    
    # 继续定时更新
    if(!is.null(isolate(ctpclient()))) {
      timer_id <<- later::later(update_instruments_async, 2)
    }
  }
  
  # 停止并清空
  observeEvent(input$disconnect_btn, {
    if(!is.null(ctpclient())) {
      stop_timer()
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
  
  # 生成K线数据
  kline_data <- reactive({
    req(input$instrument)
    client <- ctpclient()
    if(is.null(client)) return(NULL)
    
    # 刷新按钮或周期变化时重新获取
    input$refresh_k
    k_period <- input$k_period
    
    isolate({
      tryCatch({
        # 先获取tick数据，然后手动计算K线
        if(!is.null(client$ticks)) {
          ticks_df <- client$ticks(input$instrument)
          if(is.null(ticks_df) || nrow(ticks_df) == 0) {
            return(NULL)
          }
          
          # 手动计算K线
          minute_k <- compute_kline(ticks_df)
          if(is.null(minute_k) || nrow(minute_k) == 0) {
            return(NULL)
          }
          
          # 根据周期聚合
          if(k_period > 1) {
            result <- to.minutes(minute_k, k_period)
          } else {
            result <- minute_k
          }
          
          return(result)
        } else {
          NULL
        }
      }, error = function(e) {
        message("获取K线数据错误: ", e$message)
        NULL
      })
    })
  })
  
  # 自动刷新K线数据（每2秒）
  auto_refresh <- reactiveTimer(2000)
  observe({
    auto_refresh()
    # 触发K线数据重新计算
    kline_data()
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
    
    # 准备OHLC数据
    p <- plot_ly(df, x = ~Index, type = "candlestick",
                 open = ~Open, close = ~Close,
                 high = ~High, low = ~Low,
                 name = input$instrument,
                 showlegend = TRUE,
                 increasing = list(line = list(color = "red")),
                 decreasing = list(line = list(color = "green"))) %>%
      layout(title = paste(input$instrument, "K线图 (", input$k_period, "分钟)"),
             xaxis = list(title = "时间", rangeslider = list(visible = FALSE)),
             yaxis = list(title = "价格"),
             hovermode = "x unified")
    
    # 添加成交量条形图
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
    
    tryCatch({
      cat("========== CTP客户端状态 ==========\n")
      cat(sprintf("连接状态: %s\n", ifelse(is_connected(), "已连接", "未连接")))
      
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
      
      cat("\n--- 合约详细统计 ---\n")
      if(!is.null(client$stats)) {
        stats <- client$stats()
        if(length(stats) > 0) {
          for(id in names(stats)) {
            s <- stats[[id]]
            if(!is.null(s) && is.list(s)) {
              cat(sprintf("%s: 最新价=%.2f, 数量=%d, 均价=%.2f, 最高=%.2f, 最低=%.2f\n", 
                         id, 
                         ifelse(!is.null(s$last), s$last, NA), 
                         ifelse(!is.null(s$n), s$n, 0), 
                         ifelse(!is.null(s$mean), s$mean, NA),
                         ifelse(!is.null(s$max), s$max, NA),
                         ifelse(!is.null(s$min), s$min, NA)))
            }
          }
        } else {
          cat("暂无合约数据\n")
        }
      }
      cat("====================================\n")
    }, error = function(e) {
      cat("获取状态失败:", e$message, "\n")
    })
  })
  
  # 应用退出时清理
  session$onSessionEnded(function() {
    stop_timer()
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
