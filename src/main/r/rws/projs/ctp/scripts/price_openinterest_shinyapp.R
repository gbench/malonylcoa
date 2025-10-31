library(shiny)
library(ggplot2)
library(xts)

batch_load()

# 定义数据库函数
sqlquery.adhoc <- function(...) {
  sqlquery(..., host="127.0.0.1", dbname = "ctp", port = 3372)
}

getdata <- function(tbl) {
  tbl |> sprintf(fmt = "
    SELECT
      concat(TradingDay, ' ' ,UpdateTime, '.', UpdateMillisec) Time,
      LastPrice,
      AveragePrice,
      Volume,
      OpenInterest,
      PreOpenInterest
    FROM %s ORDER BY Id DESC
  ") |> sqlquery.adhoc() |> transform(Time = as.POSIXct(Time, format = "%Y%m%d %H:%M:%OS")) %>% 
    with(xts(.[, -1], order.by = Time))
}

contracts <- sqlquery.adhoc("show tables") |> unlist()|> 
  gsub(pattern="t_(\\w+)_\\d{8}", replace="\\1") |> unique()

# UI定义
ui <- fluidPage(
  titlePanel("期货合约分时图分析"),
  sidebarLayout(
    sidebarPanel(
      dateInput("curdate", "日期", value = Sys.Date()),
      selectInput("contract", "期货合约", choices = contracts, selected = "rb2510"),
      selectInput("times", "交易时段", choices = c("09:00-11:30", "13:30-15:00", "21:00-23:00"),
        selected = "09:00-11:30"),
      numericInput("k", "时间间隔数量", value = 30, min = 1),
      selectInput("on", "时间单位",
        choices = c("微秒" = "us", "毫秒" = "ms", "秒" = "secs", "分钟" = "mins", 
          "小时" = "hours", "天" = "days", "周" = "weeks", "月" = "months", 
          "季" = "quarters", "年" = "years"),
        selected = "mins"),
      actionButton("refresh", "刷新数据", icon = icon("sync"))
    ),
    mainPanel(
      tabsetPanel(
        tabPanel("价格与持仓量", plotOutput("priceOiPlot")),
        tabPanel("开平仓统计", plotOutput("oiBarPlot")),
        tabPanel("交易量统计", plotOutput("volBarPlot")))
    ) # mainPanel
  ) # sidebarLayout
) # fluidPage

# Server逻辑
server <- function(input, output) {
  
  # 创建响应式值用于触发刷新
  refreshTrigger <- reactiveVal(0)
  
  # 当点击刷新按钮时增加refreshTrigger的值
  observeEvent(input$refresh, {
    refreshTrigger(refreshTrigger() + 1)
  })
  
  # 响应式数据加载
  processedData <- reactive({
    # 依赖所有输入参数和refreshTrigger
    input$refresh
    req(input$contract, input$curdate)
    
    tickdata <- paste0("t_", input$contract, "_", gsub("-", "", input$curdate)) |> 
      getdata() |> transform(volume = diff(Volume) |> as.vector())
    
    period <- paste(input$curdate, unlist(strsplit(input$times, "-")), collapse = "/")
    tickdata <- tickdata[period, ]
    
    breaks <- endpoints(tickdata, on = input$on, input$k) |> (\(x) c(1, x[-1]))()
    labels <- index(tickdata)[breaks] |> strftime(format = "%H:%M")
    
    scale_factor <- with(tickdata, max(LastPrice) / max(OpenInterest))
    
    list(
      tickdata = tickdata,
      breaks = breaks,
      labels = labels,
      scale_factor = scale_factor
    )
  })
  
  # 双坐标轴价格持仓量分时图
  output$priceOiPlot <- renderPlot({
    data <- processedData()
    req(data$tickdata)
    
    ggplot(data$tickdata, aes(x = 1:nrow(data$tickdata))) +
      geom_line(aes(y = LastPrice, color = "LastPrice"), linewidth = 1) +
      geom_line(aes(y = OpenInterest * data$scale_factor, color = "OpenInterest"), linewidth = 1) +
      scale_y_continuous(
        name = "Price",
        sec.axis = sec_axis(~ ./data$scale_factor, name = "OpenInterest")
      ) +
      scale_x_continuous(breaks = data$breaks, labels = data$labels) +
      scale_color_manual(values = c(LastPrice = "red", OpenInterest = "blue")) +
      labs(x = "时间", title = "价格与持仓量走势") +
      theme_minimal() +
      theme(legend.position = "top")
  })
  
  # 开平仓数量统计图
  output$oiBarPlot <- renderPlot({
    data <- processedData()
    req(data$tickdata)
    
    oi_changes <- data$tickdata |> with(OpenInterest |> as.numeric() |> diff())
    oi_data <- tapply(oi_changes, sign(oi_changes), sum)
    
    bp <- barplot(oi_data, main = "开平仓数量统计",
      xlab = "方向 (1=开仓,0=不变, -1=平仓)", ylab = "数量", 
      col = c("#FF6B6B", "#4E67C4", "#4ECDC4"), ylim=range(oi_data)*1.5)
    text(x = bp, y = oi_data, labels = round(oi_data, 2), pos=3) # 显示持仓量
  })
  
  # 交易量统计图
  output$volBarPlot <- renderPlot({
    data <- processedData()
    req(data$tickdata)
    
    tickdata <- data$tickdata
    breaks <- data$breaks
    volume <- tickdata$Volume
    vol <- volume[breaks] |> diff()
    plot.xts(vol)
  })
}

# 运行应用
shinyApp(ui = ui, server = server)