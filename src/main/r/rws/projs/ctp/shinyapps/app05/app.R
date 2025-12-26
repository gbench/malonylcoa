# app.R
library(shiny)
library(quantmod)
library(xts)

# ---------- 0. 导入环境配置 ----------
source("xxxconfig_boot.R") 

# 提取指定期货合约的OHLCV数据（转换成xts)
kl <- function(symbol = "kl_rb2605") {
  sql <- sprintf("select * from %s", symbol)
  raw <- sqlquery(sql)
  raw |> transform(TS = as.POSIXct(TS, format = "%Y%m%d%H%M")) |> arrange(TS) |>
    with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME, VOL0, VOL1), order.by = TS)) |> tail(30)
}

# ---------- 1. UI ----------
ui <- fluidPage(
  titlePanel("Ignite K-line Real-time"),
  sidebarLayout(
    sidebarPanel(
      # textInput("symbol", "Symbol:", value = "kl_rb2605"),
      selectInput("symbol", "选择服务器", 
          choices = c("螺纹钢2605" = "kl_rb2605", 
              "螺纹钢2603" = "kl_rb2603", 
              "螺纹钢2601" = "kl_rb2601", 
              "甲醇2601" = "kl_ma601"
          ), selected = "kl_rb2601"),
      numericInput("interval", "Refresh interval (s):", value = 1, min = 1, step = 1),
      actionButton("refresh", "Manual Refresh"),
      hr(),
      actionButton("exit", "退出程序", icon = icon("power-off"), 
                   class = "btn-danger", width = "100%")
    ),
    mainPanel(plotOutput("kline", height = "600px"))
  )
)

# ---------- 2. Server ----------
server <- function(input, output, session) {
  initialize()
  ## 1. 动态定时器：interval 变化时重新创建
  timer <- reactiveVal(reactiveTimer(1000))   # 初始 500 ms
  observe({ # 当 interval 变化时，重新生成定时器
    timer(reactiveTimer(input$interval * 1000))
  }) |> bindEvent(input$interval)
  
  # 结束运行
  observeEvent(input$exit, {
    uninitialize()
    stopApp()   # 优雅终止 shiny
  })
  
  ## 2. 数据获取：依赖定时器 + 手动按钮
  data <- reactive({
    timer()()        # 依赖定时器
    input$refresh    # 手动按钮也触发
    kl(input$symbol)
  }) |> bindEvent(timer()(), input$refresh)
  
  ## 3. 绘图
  output$kline <- renderPlot({
    x <- data()
    if (is.null(x) || NROW(x) == 0) return(NULL)   # 没数据直接停
    chartSeries(x, name = toupper(input$symbol), theme = chartTheme("white"))
  })
}

# ---------- 3. 启动 ----------
shinyApp(ui, server)
