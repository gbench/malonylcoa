# app.R  ---- 函数式精简版，JS 默认首页 ----
library(shiny); library(quantmod); library(xts); library(jsonlite); 
source("xxxconfig_boot.R")  # 保证 klines() 可用
initialize() # 环境初始化

ui <- fluidPage(
  tags$head(tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css")),
  div(class = "title-panel", titlePanel("Ignite K-line")),
  sidebarLayout(
    sidebarPanel(
      width = 2,
      class = "sidebar-panel",
      div(class = "form-group", tags$label(class = "control-label", "合约"), selectInput("sym", label = NULL, choices = NULL)),
      div(class = "form-group", tags$label(class = "control-label", "刷新间隔(秒)"), numericInput("intv", label = NULL, value = 1, min = 0.1, step = 0.1)),
      actionButton("refresh", "手动刷新", class = "btn btn-primary", width = "100%"),
      hr(),
      actionButton("exit", "退出", icon = icon("power-off"), class = "btn btn-danger", width = "100%")
    ),
    mainPanel( width = 10, class = "main-panel",
      tabsetPanel( id = "tabs", selected = "js",
        tabPanel("js", tags$div(id = "chart", style = "width:100%;height:540px;border:none;"), 
          tags$script(src = "js/klinecharts.min.js"), tags$script(src = "js/chartapp.js"), tags$script(src = "js/exit.js")),
        tabPanel("R", div(class = "shiny-plot-output", plotOutput("kline", height = "540px")))
      )
    )
  )
)

server <- function(input, output, session) {
  tick <- reactiveTimer(500) # 定时计时周期，因为CTP推送聚合tickdata就是一秒两次的500毫秒的推送周期！
  update_klinecharts <- \(data) session$sendCustomMessage("push", data) # 更新k线
  safefetch <- \(...) if(input$sym %in% syms()) fetch_json(...) else toJSON(list())
  syms <- reactive({ input$refresh; sqlquery("select table_name nm from system.tables order by nm") |> 
    grep("KL", x=_, value=T) %>% setNames(nm=sub("^KL_", "", x=.)) }) 
  data <- reactive({ # K线数据
    klines(input$sym) |> with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME), 
      order.by=as.POSIXct(TS, format="%Y%m%d%H%M"))) |> tail(100)
  }) |> bindEvent(input$sym, input$refresh)
  
  # 事件的监听
  observe(updateSelectInput(session, "sym", choices=syms(), selected={ # 选择项目
      availables <- syms(); s <- "KL_RB2605"; if(s %in% availables) s else availables[1] })) |> bindEvent(syms()) # # 动态合约注入
  observe(update_klinecharts(safefetch(input$sym))) |> bindEvent(tick()) # 定时刷新klinechart（增量更新数据）
  observe(update_klinecharts(safefetch(input$sym, startime=0))) |> bindEvent(input$sym) # 切换合约（使用全量更新数据）
  observe(invalidate_kline_caches()) |> bindEvent(input$refresh) # 手动刷新
  # 退出事件
  observeEvent(input$confirm_exit, { if (input$confirm_exit == "yes") { 
    uninitialize(); stopApp(); session$sendCustomMessage(type = "closeWindow", message = list()) }})
  
   # 静态绘图
   output$kline <- renderPlot(data() |> chartSeries(name=toupper(input$sym), theme=chartTheme("white"))) # quantmod 近期绘图
}

shinyApp(ui, server)