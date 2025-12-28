# app.R  ---- 函数式精简版，JS 默认首页 ----
library(shiny); library(quantmod); library(xts); library(jsonlite); 
source("xxxconfig_boot.R")  # 保证 kl() 可用

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
          tags$div(id = "chart", style = "width:100%;height:500px;border:1px solid #ccc;"),
          # 关键：把 JS 放在这个 tab 里，确保 DOM 已存在
          tags$script(src = "klinecharts.min.js"),
          tags$script(src = "kline.js")
        ),
        tabPanel("R", plotOutput("kline", height = "500px"))
      )
    )
  )
)

server <- function(input, output, session) {
  initialize()

  tick <- reactiveTimer(1000)

  data <- reactive({
    klines(input$sym)
  }) |> bindEvent(tick(), input$refresh)
  
  output$kline <- renderPlot({
    d <- data() |> with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME), order.by=as.POSIXct(TS, format="%Y%m%d%H%M")))
    if (is.null(d) || !NROW(d)) NULL else chartSeries(d, name = toupper(input$sym), theme = chartTheme("white"))
  })
  
  # 刷新klinechart
  observe(fetch_json(input$sym) %>% session$sendCustomMessage("push", .)) |> bindEvent(tick(), input$intv)
  # 退出事件
  observeEvent(input$exit, {uninitialize(); stopApp()})
}

shinyApp(ui, server)