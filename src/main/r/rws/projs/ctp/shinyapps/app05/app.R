# app.R  ---- 函数式精简版，JS 默认首页 ----
library(shiny); library(quantmod); library(xts); library(jsonlite); 
source("xxxconfig_boot.R")  # 保证 kl() 可用
initialize()

ui <- fluidPage(
  titlePanel("Ignite K-line"),
  sidebarLayout(
    sidebarPanel(width = 2,
      selectInput("sym","合约",choices=NULL),
      numericInput("intv", "刷新(s):", 1, min = .1, step = .1),
      actionButton("refresh", "手动刷新"),
      hr(),
      actionButton("exit", "退出", icon = icon("power-off"), class = "btn-danger", width = "100%")
    ),
    mainPanel(width = 10,
      tabsetPanel(id = "tabs", selected = "js",
        tabPanel("js",
          tags$div(id = "chart", style = "width:100%;height:650px;border:1px solid #ccc;"),
          # 关键：把 JS 放在这个 tab 里，确保 DOM 已存在
          tags$script(src = "klinecharts.min.js"),
          tags$script(src = "kline.js")
        ),
        tabPanel("R", plotOutput("kline", height = "650px"))
      )
    )
  )
)

server <- function(input, output, session) {
  tick <- reactiveTimer(500) # 定时计时周期，因为CTP推送聚合tickdata就是一秒两次的500毫秒的推送周期！
  update_klinechart <- \(data) session$sendCustomMessage("push", data) # 更新k线
  safefech <- \(...) if(input$sym %in% syms()) fetch_json(...) else toJSON(list())
  syms <- reactive({ input$refresh; sqlquery("select table_name nm from system.tables order by nm") |> 
    grep("KL", x=_, value=T) %>% setNames(nm=sub("", "", x=.)) }) 
  data <- reactive({  # K线数据
    klines(input$sym) |> with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME), 
      order.by=as.POSIXct(TS, format="%Y%m%d%H%M"))) |> tail(100)
  }) |> bindEvent(input$sym, input$refresh)
  
  # quantmod 近期绘图
  output$kline <- renderPlot(data() |> chartSeries(name=toupper(input$sym), theme=chartTheme("white")))
  
  # 动态合约注入
  observe(updateSelectInput(session, "sym", choices=syms(), selected="KL_RB2605")) |> bindEvent(syms()) # ③ 一行注入
  # 定时刷新klinechart（增量更新数据）
  observe(update_klinechart(safefech(input$sym))) |> bindEvent(tick())
  # 切换合约（使用全量更新数据）
  observe(update_klinechart(safefech(input$sym, startime=0))) |> bindEvent(input$sym)
  # 手动刷新
  observe(invalidate_kline_caches()) |> bindEvent(input$refresh)
  # 退出事件
  observeEvent(input$exit, {uninitialize(); stopApp()})
}

shinyApp(ui, server)