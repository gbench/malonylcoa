# app.R  ---- 函数式精简版，JS 默认首页 ----
library(shiny); library(quantmod); library(xts); library(jsonlite); 
source("xxxconfig_boot.R")  # 保证 kl() 可用
initialize()

ui <- fluidPage(
  titlePanel("Ignite K-line"),
  sidebarLayout(
    sidebarPanel(width = 2,
      selectInput("sym", "合约", choices=sqlquery("select table_name from system.tables") |> 
        grep("KL", x=_, value=T) %>% setNames(nm=sub("", "", x=.)), selected = "KL_RB2605"),
      numericInput("intv", "刷新(s):", 1, min = .1, step = .1),
      actionButton("refresh", "手动刷新"),
      hr(),
      actionButton("exit", "退出", icon = icon("power-off"), class = "btn-danger", width = "100%")
    ),
    mainPanel(width = 10,
      tabsetPanel(id = "tabs", selected = "js",
        tabPanel("js",
          tags$div(id = "chart", style = "width:100%;height:550px;border:1px solid #ccc;"),
          # 关键：把 JS 放在这个 tab 里，确保 DOM 已存在
          tags$script(src = "klinecharts.min.js"),
          tags$script(src = "kline.js")
        ),
        tabPanel("R", plotOutput("kline", height = "550px"))
      )
    )
  )
)

server <- function(input, output, session) {
  # 定时器
  tick <- reactiveTimer(1000)
  # K线数据
  data <- reactive({
    klines(input$sym) |> with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME), 
      order.by=as.POSIXct(TS, format="%Y%m%d%H%M"))) |> tail(100)
  }) |> bindEvent(input$sym, input$refresh)
  # quantmod 近期绘图
  output$kline <- renderPlot(data() |> chartSeries(name=toupper(input$sym), theme=chartTheme("white")))
  # 定时刷新klinechart
  observe(fetch_json(input$sym) %>% session$sendCustomMessage("push", .)) |> bindEvent(tick(), input$intv)
  # 手动刷新
  observe(invalidate_kline_caches()) |> bindEvent(input$refresh)
  # 退出事件
  observeEvent(input$exit, {uninitialize(); stopApp()})
}

shinyApp(ui, server)