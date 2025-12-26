# app.R  ---- 函数式精简版，JS 默认首页 ----
library(shiny); library(quantmod); library(xts); library(jsonlite)
source("xxxconfig_boot.R")  # 保证 kl() 可用

# 常规函数：拉数据 -> xts -> 截取30条 -> JSON
# 替换 fetch_json 即可
fetch_json <- function(sym) {
  x <- kl(sym) # 读取K线数据
  if (!NROW(x)) return(NULL)
  # 纯列表数组，字段名严格对应 KlineCharts 要求
  purrr::transpose(list(
    timestamp = as.numeric(index(x)) * 1000,
    open      = x$OPEN,
    high      = x$HIGH,
    low       = x$LOW,
    close     = x$CLOSE,
    volume    = x$VOLUME
  )) |> toJSON(auto_unbox = TRUE)
}

ui <- fluidPage(
  tags$head(
    tags$script(src = "https://cdn.jsdelivr.net/npm/klinecharts/dist/klinecharts.min.js")
  ),
  titlePanel("Ignite K-line"), # 面板标题
  sidebarLayout(
    sidebarPanel(
      selectInput("sym", "合约",
        c("螺纹钢2605" = "kl_rb2605",
          "螺纹钢2603" = "kl_rb2603",
          "螺纹钢2601" = "kl_rb2601",
          "甲醇2601"   = "kl_ma601"),
        selected = "kl_rb2601"), # selectInput
      numericInput("intv", "刷新(s):", 1, min = 1, step = 1), # 刷新频率
      actionButton("refresh", "手动刷新"), # 刷新按钮
      hr(),
      actionButton("exit", "退出", icon = icon("power-off"), class = "btn-danger", width = "100%")
    ), # sidebarPanel
    mainPanel(
      tabsetPanel(id = "tabs", selected = "js",
        tabPanel("js",
          tags$div(id = "chart", style = "width:100%;height:400px;border:1px solid #ccc;"),
          tags$script(HTML("
            const chart = klinecharts.init('chart');
            Shiny.addCustomMessageHandler('push', j => chart.applyNewData(j));
          "))), # js panel
        tabPanel("R", plotOutput("kline", height = "600px")) # R
      ) # tabsetPanel
    ) # mainPanel
  ) # sidebarLayout
) # fluidPage

server <- function(input, output, session) {
  initialize()

  tick <- reactiveTimer(1000)

  data <- reactive({
    tick(); input$refresh
    kl(input$sym, 30)
  }) |> bindEvent(tick(), input$refresh)
  
  output$kline <- renderPlot({
    d <- data()
    if (is.null(d) || !NROW(d)) return(NULL)
    chartSeries(d, name = toupper(input$sym), theme = chartTheme("white"))
  })
  
  observe({
    tick(); invalidateLater(input$intv * 1000, session)
    json <- fetch_json(input$sym)
    if (!is.null(json)) session$sendCustomMessage("push", json)
  }) |> bindEvent(tick(), input$intv)
  
  observeEvent(input$exit, {uninitialize(); stopApp()})
}

shinyApp(ui, server)