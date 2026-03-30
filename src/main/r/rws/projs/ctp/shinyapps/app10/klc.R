# app.R - 完整的K线图应用，使用klinecharts.js
library(shiny)
library(shinydashboard)
library(dplyr)
library(jsonlite)
library(later)
library(lubridate)

# 加载必要的包
required_packages <- c("later", "jsonlite", "dplyr", "shiny", "shinydashboard", "lubridate")
for(pkg in required_packages) {
  if(!require(pkg, character.only = TRUE)) {
    install.packages(pkg)
    library(pkg, character.only = TRUE)
  }
}

# 确保 ctpd_async 函数存在
if(!exists("ctpd_async")) {
  warning("ctpd_async 函数未定义！请确保已加载 ctpd.R 文件")
}

# 将tick数据转换为K线数据
ticks_to_kline <- function(ticks_df, period_minutes = 1) {
  if(is.null(ticks_df) || nrow(ticks_df) == 0) return(NULL)
  
  # 创建时间戳
  if("ActionDay" %in% colnames(ticks_df) && "UpdateTime" %in% colnames(ticks_df)) {
    ticks_df$DateTime <- as.POSIXct(paste0(ticks_df$ActionDay, " ", ticks_df$UpdateTime), 
                                    format="%Y%m%d %H:%M:%S")
    if("UpdateMillisec" %in% colnames(ticks_df)) {
      ticks_df$DateTime <- ticks_df$DateTime + ticks_df$UpdateMillisec / 1000
    }
    
    # 按周期聚合
    ticks_df$Period <- lubridate::floor_date(ticks_df$DateTime, paste0(period_minutes, " mins"))
    
    kline <- ticks_df %>%
      group_by(Period) %>%
      summarise(
        open = first(LastPrice),
        high = max(LastPrice),
        low = min(LastPrice),
        close = last(LastPrice),
        volume = sum(Volume, na.rm = TRUE),
        .groups = 'drop'
      ) %>%
      arrange(Period)
    
    # 转换为klinecharts需要的格式
    result <- list(
      timestamp = as.numeric(kline$Period) * 1000,  # 毫秒时间戳
      open = kline$open,
      high = kline$high,
      low = kline$low,
      close = kline$close,
      volume = kline$volume,
      instrument = attr(ticks_df, "instrument")
    )
    
    return(result)
  }
  return(NULL)
}

# UI
ui <- fluidPage(
  tags$head(
    tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css"),
    tags$style(HTML("
      body {
        background-color: #1e1e2f;
        color: #e0e0e0;
      }
      .title-panel {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 15px;
        border-radius: 5px;
        margin-bottom: 20px;
      }
      .title-panel h2 {
        margin: 0;
        color: white;
        text-align: center;
      }
      .sidebar-panel {
        background-color: #2d2d3f;
        border-radius: 5px;
        padding: 15px;
      }
      .main-panel {
        background-color: #1e1e2f;
      }
      .control-group {
        margin-bottom: 15px;
      }
      .control-group label {
        color: #e0e0e0;
        font-weight: bold;
      }
      .btn-primary {
        background-color: #667eea;
        border: none;
      }
      .btn-primary:hover {
        background-color: #5a67d8;
      }
      .btn-danger {
        background-color: #e53e3e;
        border: none;
      }
      .btn-danger:hover {
        background-color: #c53030;
      }
      .info-box {
        background-color: #2d2d3f;
        border-radius: 5px;
        padding: 10px;
        margin-top: 10px;
      }
      .info-box h4 {
        margin: 0 0 10px 0;
        color: #667eea;
      }
    "))
  ),
  
  div(class = "title-panel", 
      titlePanel("CTP 实时K线图 - 动态刷新")),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      class = "sidebar-panel",
      
      div(class = "control-group",
        tags$label("服务器配置"),
        textInput("host", "服务器地址", value = "localhost"),
        numericInput("port", "端口", value = 9892, min = 1, max = 65535),
        actionButton("connect_btn", "启动连接", class = "btn-primary", icon = icon("play"), width = "100%"),
        actionButton("disconnect_btn", "停止连接", class = "btn-danger", icon = icon("stop"), width = "100%"),
        hr()
      ),
      
      div(class = "control-group",
        tags$label("合约选择"),
        selectInput("instrument", "合约代码", choices = NULL),
        hr()
      ),
      
      div(class = "control-group",
        tags$label("K线设置"),
        numericInput("kline_period", "K线周期(分钟)", value = 1, min = 1, max = 60, step = 1),
        actionButton("refresh_btn", "刷新K线", icon = icon("sync"), width = "100%"),
        hr()
      ),
      
      div(class = "info-box",
        h4("实时信息"),
        verbatimTextOutput("price_info", placeholder = TRUE)
      ),
      
      actionButton("exit", "退出应用", icon = icon("power-off"), class = "btn-danger", width = "100%")
    ),
    
    mainPanel(
      width = 9,
      class = "main-panel",
      div(id = "chart", style = "width:100%; height:600px; background-color: #1e1e2f; border: 1px solid #4a5568; border-radius: 5px;"),
      tags$script(src = "js/klinecharts.min.js"),
      tags$script(src = "js/chartapp.js"),
      tags$script(src = "js/exit.js")
    )
  )
)

server <- function(input, output, session) {
  
  # 全局变量
  ctpclient <- reactiveVal(NULL)
  available_instruments <- reactiveVal(character())
  is_connected <- reactiveVal(FALSE)
  timer_id <- NULL
  price_timer_id <- NULL
  
  # 辅助函数: 清空环境
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
    is_connected(FALSE)
    message("环境已重置")
  }
  
  # 停止定时器
  stop_timers <- function() {
    if(!is.null(timer_id)) timer_id <<- NULL
    if(!is.null(price_timer_id)) price_timer_id <<- NULL
  }
  
  # 安全显示通知
  safe_notification <- function(message, type = NULL, duration = 3) {
    tryCatch({
      if(is.null(type)) {
        showNotification(message, duration = duration)
      } else {
        showNotification(message, type = type, duration = duration)
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
      start_kline_updater()
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
  
  # 启动K线更新器
  start_kline_updater <- function() {
    update_kline <- function() {
      client <- isolate(ctpclient())
      if(is.null(client)) return()
      
      req_instrument <- isolate(input$instrument)
      if(is.null(req_instrument) || req_instrument == "") return()
      
      tryCatch({
        # 获取tick数据
        ticks_df <- client$ticks(req_instrument)
        if(!is.null(ticks_df) && nrow(ticks_df) > 0) {
          # 转换为K线数据
          period <- isolate(input$kline_period)
          kline_data <- ticks_to_kline(ticks_df, period)
          
          if(!is.null(kline_data) && length(kline_data$timestamp) > 0) {
            # 构建发送数据
            send_data <- list(
              instrument = req_instrument,
              ds = lapply(seq_along(kline_data$timestamp), function(i) {
                list(
                  timestamp = kline_data$timestamp[i],
                  open = kline_data$open[i],
                  high = kline_data$high[i],
                  low = kline_data$low[i],
                  close = kline_data$close[i],
                  volume = kline_data$volume[i]
                )
              })
            )
            
            # 发送到前端
            session$sendCustomMessage("push", send_data)
          }
        }
      }, error = function(e) {
        message("更新K线错误: ", e$message)
      })
      
      # 继续定时更新
      if(!is.null(isolate(ctpclient()))) {
        timer_id <<- later::later(update_kline, 1)  # 每秒更新一次
      }
    }
    
    update_kline()
  }
  
  # 启动价格监控
  start_price_monitor <- function() {
    monitor_prices <- function() {
      client <- isolate(ctpclient())
      if(is.null(client)) return()
      
      req_instrument <- isolate(input$instrument)
      if(!is.null(req_instrument) && req_instrument != "") {
        tryCatch({
          stats <- client$stats(req_instrument)
          if(!is.null(stats) && is.list(stats)) {
            # 更新价格信息显示
            isolate({
              output$price_info <- renderPrint({
                cat("========== 实时行情 ==========\n")
                cat(sprintf("合约: %s\n", req_instrument))
                cat(sprintf("最新价: %.2f\n", stats$last))
                cat(sprintf("均价: %.2f\n", stats$mean))
                cat(sprintf("最高价: %.2f\n", stats$max))
                cat(sprintf("最低价: %.2f\n", stats$min))
                cat(sprintf("Tick数: %d\n", stats$n))
                cat(sprintf("更新时间: %s\n", stats$updatetime))
                cat("===============================\n")
              })
            })
          }
        }, error = function(e) {
          message("监控价格错误: ", e$message)
        })
      }
      
      # 继续监控
      if(!is.null(isolate(ctpclient()))) {
        price_timer_id <<- later::later(monitor_prices, 0.5)
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
      updateSelectInput(session, "instrument", choices = instr, selected = instr[1])
    }
    
    if(!is.null(isolate(ctpclient()))) {
      timer_id <<- later::later(update_instruments_async, 2)
    }
  }
  
  # 停止连接
  observeEvent(input$disconnect_btn, {
    if(!is.null(ctpclient())) {
      stop_timers()
      reset_env()
      showModal(modalDialog(
        title = "已停止",
        "CTP连接已停止，数据已清空。",
        easyClose = TRUE,
        footer = modalButton("确定")
      ))
      # 清空价格显示
      output$price_info <- renderPrint({
        cat("CTP客户端未启动\n")
        cat("请点击'启动连接'按钮连接CTP服务器\n")
      })
    } else {
      safe_notification("当前没有活动的连接")
    }
  })
  
  # 手动刷新
  observeEvent(input$refresh_btn, {
    if(!is.null(ctpclient())) {
      safe_notification("正在刷新K线数据...")
      # 触发一次更新
      start_kline_updater()
    }
  })
  
  # 切换合约时重新加载
  observeEvent(input$instrument, {
    if(!is.null(ctpclient()) && input$instrument != "") {
      # 清空现有数据，让前端重新加载
      session$sendCustomMessage("switchInstrument", input$instrument)
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
        actionButton("confirm_exit", "确定", class = "btn-danger")
      )
    ))
  })
  
  observeEvent(input$confirm_exit, {
    stop_timers()
    reset_env()
    stopApp()
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

# 创建必要的目录和文件
if(!dir.exists("www")) dir.create("www")
if(!dir.exists("www/js")) dir.create("www/js")
if(!dir.exists("www/css")) dir.create("www/css")

# 创建CSS文件
writeLines('
/* www/css/style.css */
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.shiny-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
}

.chart-container {
  background-color: #1e1e2f;
}

.klinecharts-pro {
  background-color: #1e1e2f;
}
', "www/css/style.css")

# 创建 chartapp.js
writeLines('
// www/js/chartapp.js
(function () {
  /* ========== 全局状态 ========== */
  const chart = klinecharts.init("chart");
  let currentInstrument = null;
  let updateTimer = null;

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
    calc: (kLineDataList, _) => {
      return kLineDataList.map((k, i, ks) => ({
        oint: k.oint || 0,
        preoint: i < 1 ? (k.oint || 0) : (ks[i - 1].oint || 0)
      }));
    }
  });

  // 设置图表样式
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

  // 创建指标
  chart.createIndicator("VOL");
  chart.createIndicator("MA", true, { id: "candle_pane" });
  chart.createIndicator("MACD");

  /* ========== 监听 Shiny 推送 ========== */
  Shiny.addCustomMessageHandler("push", function(data) {
    const { instrument, ds } = data;
    
    if (!ds || ds.length === 0) {
      console.log("数据无效！");
      return;
    }
    
    if (instrument !== currentInstrument) {
      // 新品种：清空并加载全量数据
      currentInstrument = instrument;
      chart.clearData();
      chart.applyNewData(ds);
      console.log("切换到新品种:", instrument, "数据量:", ds.length);
    } else {
      // 同一品种：增量更新
      const currentData = chart.getDataList();
      if (!currentData || currentData.length === 0) {
        chart.applyNewData(ds);
      } else {
        ds.forEach(function(bar) {
          chart.updateData(bar);
        });
      }
    }
  });

  // 切换合约
  Shiny.addCustomMessageHandler("switchInstrument", function(instrument) {
    if (instrument !== currentInstrument) {
      currentInstrument = instrument;
      chart.clearData();
      console.log("手动切换合约:", instrument);
    }
  });

  // 页面关闭时清理
  window.addEventListener("beforeunload", function() {
    if (updateTimer) clearInterval(updateTimer);
  });

  console.log("K线图表初始化完成");
})();
', "www/js/chartapp.js")

# 创建 exit.js
writeLines('
// www/js/exit.js
document.addEventListener("DOMContentLoaded", function() {
  setTimeout(function() {
    var exitBtn = document.getElementById("exit");
    if (exitBtn) {
      exitBtn.onclick = null;
      exitBtn.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (confirm("确定要退出应用程序吗？")) {
          Shiny.setInputValue("exit", "yes", {priority: "event"});
          setTimeout(function() {
            window.close();
          }, 100);
        }
      });
    }
  }, 500);
});
', "www/js/exit.js")

message("请确保 klinecharts.min.js 文件已放置在 www/js/ 目录下")
message("可以从 https://cdn.jsdelivr.net/npm/klinecharts@latest/dist/klinecharts.min.js 下载")

# 运行应用
shinyApp(ui, server)
