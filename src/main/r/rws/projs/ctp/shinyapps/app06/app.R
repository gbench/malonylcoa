# app.R  ---- 移动端极简版 ----
library(shiny); library(quantmod); library(xts); library(jsonlite); library(shinyjs)
source("xxxconfig_boot.R")  # 保证 klines() 可用
initialize() # 环境初始化

ui <- fluidPage(
  useShinyjs(),
  tags$head(
    tags$meta(name = "viewport", content = "width=device-width, initial-scale=1.0"),
    tags$link(rel = "stylesheet", type = "text/css", href = "css/style.css"),
    tags$link(rel = "stylesheet", type = "text/css", href = "css/mobile.css")
  ),
  
  # 桌面版布局（移动端隐藏）
  div(class = "desktop-only",
    div(class = "title-panel", titlePanel("Ignite K-line")),
    sidebarLayout(
      sidebarPanel(
        width = 2,
        class = "sidebar-panel",
        div(class = "form-group", tags$label(class = "control-label", "合约"), 
            selectInput("sym", label = NULL, choices = NULL)),
        div(class = "form-group", tags$label(class = "control-label", "刷新间隔(秒)"), 
            numericInput("intv", label = NULL, value = 1, min = 0.1, step = 0.1)),
        actionButton("refresh", "手动刷新", class = "btn btn-primary", width = "100%"),
        hr(),
        actionButton("exit", "退出", icon = icon("power-off"), class = "btn btn-danger", width = "100%")
      ),
      mainPanel(
        width = 10,
        class = "main-panel",
        tabsetPanel(
          id = "tabs",
          selected = "js",
          tabPanel("js", tags$div(id = "chart", style = "width:100%;height:540px;border:none;"), 
                   tags$script(src = "js/klinecharts.min.js"), 
                   tags$script(src = "js/chartapp.js")),
          tabPanel("R", div(class = "shiny-plot-output", plotOutput("kline", height = "540px")))
        )
      )
    )
  ),
  
  # 移动端布局（桌面端隐藏）- 极简设计
  div(class = "mobile-only",
    # 图表区域 - 占大部分屏幕
    div(class = "mobile-chart-container",
        div(id = "chart_mobile", class = "mobile-chart"),
        tags$script(src = "js/klinecharts.min.js"),
        tags$script(src = "js/chartapp_mobile.js")
    ),
    
    # 极小控制面板
    div(class = "mobile-controls-mini",
        div(style = "display: flex; align-items: center; gap: 8px;",
            selectInput("sym_mobile", label = NULL, choices = list("RB2605" = "KL_RB2605"), 
                       width = "50%", selectize = FALSE),
            actionButton("refresh_mobile", "", icon = icon("sync"),
                        class = "btn btn-sm btn-outline-primary", 
                        style = "padding: 4px 8px;"),
            actionButton("exit_mobile", "", icon = icon("power-off"),
                        class = "btn btn-sm btn-outline-danger", 
                        style = "padding: 4px 8px;"),
            numericInput("intv_mobile", label = NULL, value = 1, 
                        min = 0.1, step = 0.1, width = "20%")
        )
    )
  )
)

server <- function(input, output, session) {
  tick <- reactiveTimer(500)
  
  # 一行获取合约列表
  syms <- reactive({ input$refresh; input$refresh_mobile; 
    sqlquery("select table_name nm from system.tables order by nm") |> 
      grep("KL", x=_, value=T) %>% setNames(nm=sub("^KL_", "", x=.)) 
  }) 
  
  # 一行更新选择输入
  observe({
    availables <- syms()
    s <- "KL_RB2605"
    selected_sym <- if(s %in% availables) s else if(length(availables) > 0) availables[1] else "KL_RB2605"
    updateSelectInput(session, "sym", choices = availables, selected = selected_sym)
    updateSelectInput(session, "sym_mobile", choices = availables, selected = selected_sym)
  }) |> bindEvent(syms())
  
  # 一行数据获取
  data <- reactive({ 
    klines(input$sym) |> with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME), 
      order.by=as.POSIXct(TS, format="%Y%m%d%H%M"))) |> tail(100)
  }) |> bindEvent(input$sym, input$refresh)
  
  data_mobile <- reactive({ 
    klines(input$sym_mobile) |> with(as.xts(cbind(OPEN, HIGH, LOW, CLOSE, VOLUME), 
      order.by=as.POSIXct(TS, format="%Y%m%d%H%M"))) |> tail(80)
  }) |> bindEvent(input$sym_mobile, input$refresh_mobile)
  
  # 一行消息推送
  update_klinecharts <- \(data) session$sendCustomMessage("push", data)
  update_klinecharts_mobile <- \(data) session$sendCustomMessage("push_mobile", data)
  
  # 安全的safefetch函数
  safefetch <- function(sym, startime=NA) {
    if(is.null(sym) || length(sym) == 0 || sym == "") return(toJSON(list()))
    sym_list <- syms()
    if(is.null(sym_list) || length(sym_list) == 0) return(toJSON(list()))
    if(!sym %in% sym_list) return(toJSON(list()))
    fetch_json(sym, startime)
  }
  
  # 一行定时刷新
  observe({
    data <- safefetch(input$sym)
    if(!is.null(data)) update_klinecharts(data)
  }) |> bindEvent(tick())
  
  observe({
    data <- safefetch(input$sym_mobile)
    if(!is.null(data)) update_klinecharts_mobile(data)
  }) |> bindEvent(tick())
  
  # 一行切换合约
  observe({
    data <- safefetch(input$sym, startime=0)
    if(!is.null(data)) update_klinecharts(data)
  }) |> bindEvent(input$sym)
  
  observe({
    data <- safefetch(input$sym_mobile, startime=0)
    if(!is.null(data)) update_klinecharts_mobile(data)
  }) |> bindEvent(input$sym_mobile)
  
  # 一行手动刷新
  observe({
    invalidate_kline_caches()
    data <- safefetch(input$sym, startime=0)
    if(!is.null(data)) update_klinecharts(data)
  }) |> bindEvent(input$refresh)
  
  observe({
    invalidate_kline_caches()
    data <- safefetch(input$sym_mobile, startime=0)
    if(!is.null(data)) update_klinecharts_mobile(data)
  }) |> bindEvent(input$refresh_mobile)
  
  # 一行同步输入
  observe({
    if(!is.null(input$sym) && input$sym != "")
      updateSelectInput(session, "sym_mobile", selected = input$sym)
  }) |> bindEvent(input$sym)
  
  observe({
    if(!is.null(input$sym_mobile) && input$sym_mobile != "")
      updateSelectInput(session, "sym", selected = input$sym_mobile)
  }) |> bindEvent(input$sym_mobile)
  
  observe({
    if(!is.null(input$intv))
      updateNumericInput(session, "intv_mobile", value = input$intv)
  }) |> bindEvent(input$intv)
  
  observe({
    if(!is.null(input$intv_mobile))
      updateNumericInput(session, "intv", value = input$intv_mobile)
  }) |> bindEvent(input$intv_mobile)
  
  # 初始化时自动绘图
  observe({
    runjs("
      // 窗口大小变化时重绘图表
      $(window).on('resize', function() {
        if(window.mobileChart) {
          setTimeout(function() { window.mobileChart.resize(); }, 100);
        }
      });
      
      // 延迟触发数据更新
      setTimeout(function() {
        if(Shiny.setInputValue) {
          Shiny.setInputValue('refresh_mobile', Math.random());
        }
      }, 800);
    ")
  })
  
  # 一行退出事件
  observeEvent(input$exit, showModal(modalDialog("确认退出吗？", 
    footer = tagList(modalButton("取消"), actionButton("confirm_exit", "确定", class = "btn-danger")))))
  observeEvent(input$exit_mobile, showModal(modalDialog("确认退出吗？", 
    footer = tagList(modalButton("取消"), actionButton("confirm_exit_mobile", "确定", class = "btn-danger")))))
  observeEvent(input$confirm_exit, { uninitialize(); stopApp() })
  observeEvent(input$confirm_exit_mobile, { uninitialize(); stopApp() })
  
  # 一行静态绘图
  output$kline <- renderPlot(data() |> chartSeries(name=toupper(input$sym), theme=chartTheme("white")))
  output$kline_mobile <- renderPlot(data_mobile() |> chartSeries(name=toupper(input$sym_mobile), theme=chartTheme("white")))
}

shinyApp(ui, server)
