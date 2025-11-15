library(shiny)
library(ggplot2)
library(dplyr)
library(purrr)
library(magrittr)
library(lubridate)

# 加载必要的函数
batch_load()

# UI部分
ui <- fluidPage(
  titlePanel("金融数据分析仪表板"),
  tags$head(
    tags$script(HTML("
      // 关闭窗口函数
      function closeWindow() {
        window.close();
      }
    "))
  ),
  sidebarLayout(
    sidebarPanel(
      actionButton("update", "更新数据"),
      # 关闭程序按钮
      actionButton("close", "关闭应用", class = "btn-danger"),

      # -------------------------------------------------------------

      # 数据源配置
      h4("数据源配置"),
      textInput("dbname", "数据库名", value = "ctp"),
      numericInput("port", "端口", value = 3371),
      textInput("host", "主机地址", value = "127.0.0.1"),
      textInput("curtbl", "数据表名", value = "t_rb2601_20251117"),
      
      # 价格绘图参数
      h4("价格绘图参数"),
      numericInput("n", "时间分割点数量", value = 10, min = 5, max = 50),
      numericInput("span", "分析时长(分钟)", value = 30, min = 1, max = 120),
      
      # 成交量分组参数
      h4("成交量分析参数"),
      numericInput("nlevel", "成交量区间分组数量", value = 3, min = 2, max = 10),

      # -------------------------------------------------------------
            
      width = 3
    ),
    
    mainPanel(
      tabsetPanel(
        tabPanel("价格趋势分析", 
                 plotOutput("pricePlot", height = "600px")),
        tabPanel("成交量分析",
                 plotOutput("volumePlot", height = "600px"),
                 tableOutput("volumeTable"),
		 tableOutput("volumeTable2")),
        tabPanel("数据概览",
                 verbatimTextOutput("dataSummary"),
                 tableOutput("dataPreview"))
      )
    )
  )
)

# Server部分
server <- function(input, output, session) {
  
  # 反应式数据源函数
  ds00ctp <- reactive({
    partial(sqlquery, dbname = input$dbname, port = input$port, host = input$host)
  })
  
  # 获取价格数据
  priceData <- eventReactive(input$update, {
    req(input$curtbl, input$span)
    
    # 构建SQL查询 - 价格数据
    sql <- "select * from %s where UpdateTime>'%s'" |> 
      sprintf(input$curtbl, (now()-dminutes(input$span)) |> strftime("%H:%M:%S"))
    
    # 执行查询
    ds00ctp()(sql)
  })
  
  # 获取成交量数据
  volumeData <- eventReactive(input$update, {
    req(input$curtbl)
    
    # 构建SQL查询 - 成交量数据
    sql <- "select Id, LastPrice, Volume, Volume - lag(Volume) over(order by Id) Vol, UpdateTime from %s" |> 
      sprintf(input$curtbl)
    
    # 执行查询
    ds00ctp()(sql)
  })
  
  # 价格趋势图
  output$pricePlot <- renderPlot({
    df <- priceData()
    req(df, nrow(df) > 0)
    
    with(df, # 打开成交数据&暴露元素以方便直接引用：{Id:序列索引，LastPrice:成交价格}
      ggplot(df, aes(Id, LastPrice)) + geom_point(alpha=0.01) + geom_smooth() +
        geom_hline(yintercept=2*(1:3-2)*sd(LastPrice)+mean(LastPrice), # 均线与2倍数标准差
          linewidth=c(1.5, 1, 1.5), color=c("darkred", "gray", "red")) + with(lm(LastPrice~Id), # 趋势回归的成交价格vs序列Id索引模型
            geom_abline(slope=coefficients[2], intercept=coefficients[1], linewidth=1.5, color="purple")) + # 绘制趋势线
	scale_x_continuous(breaks=\(xs) seq(min(Id), max(Id), length.out=input$n), labels=\(xs) UpdateTime[xs-min(Id)+1]) + # 索引刻度翻译成时间
        labs( title = "价格趋势分析", x = "时间", y = "最新成交价格") +
        theme_minimal()
    )
  })
  
  # 成交量分析图
  output$volumePlot <- renderPlot({
    df <- volumeData()
    req(df, nrow(df) > 0, input$nlevel > 0)
    
    # 移除NA值
    df_clean <- df[!is.na(df$Vol), ]
    
    xs <- df_clean |> mutate(Level=cut(Vol, input$nlevel)) |> 
        select(LastPrice, Level) |> # 价格与成交量关系数据
	table() |> prop.table() |> addmargins() |> # 计算百分比并添加margin统计
	(\(x) {
	  attr(x, "dimnames") <- attr(x, "dimnames") |> 
	  within(Level <- gsub("e\\+03", "K", Level)); 
	  x
        }) () |> round(3) # 标题美化并保留3位小数
     output$volumeTable2 <- renderTable(xs)
     plot(row.names(xs), xs[,"Sum"], xlab="price", ylab="density")
  })
  
  # 成交量统计表
  output$volumeTable <- renderTable({
    df <- volumeData()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    data.frame(
      统计量 = c("平均成交量变化", "最大成交量变化", "最小成交量变化", "成交量变化标准差"),
      数值 = c(
        round(mean(df_clean$Vol, na.rm = TRUE), 2),
        round(max(df_clean$Vol, na.rm = TRUE), 2),
        round(min(df_clean$Vol, na.rm = TRUE), 2),
        round(sd(df_clean$Vol, na.rm = TRUE), 2)
      )
    )
  })
  
  # 数据概览
  output$dataSummary <- renderPrint({
    df <- priceData()
    req(df)
    
    cat("价格数据概览:\n")
    cat("记录数:", nrow(df), "\n")
    cat("时间范围:", format(min(df$UpdateTime)), "至", format(max(df$UpdateTime)), "\n")
    cat("价格统计 - 均值:", round(mean(df$LastPrice), 2), "标准差:", round(sd(df$LastPrice), 2), "\n")
    
    vol_df <- volumeData()
    cat("\n成交量数据概览:\n")
    cat("记录数:", nrow(vol_df), "\n")
    if ("Vol" %in% names(vol_df)) {
      vol_clean <- vol_df[!is.na(vol_df$Vol), ]
      cat("成交量变化统计 - 均值:", round(mean(vol_clean$Vol), 2), "标准差:", round(sd(vol_clean$Vol), 2), "\n")
    }
  })
  
  # 数据预览
  output$dataPreview <- renderTable({
    df <- priceData()
    req(df)
    head(df, 10)
  })
  
  # 关闭应用逻辑
  observeEvent(input$close, {
    # 确认对话框
    showModal(modalDialog(
      title = "确认关闭",
      "确定要关闭应用吗？",
      footer = tagList(
        actionButton("confirm_close", "确定", class = "btn-danger"), modalButton("取消")
      )
    ))
  })
  
  # 确认关闭
  observeEvent(input$confirm_close, {
    # 移除确认对话框
    removeModal()
    
    # 停止Shiny应用
    stopApp()
    
    # 尝试关闭浏览器窗口（如果适用）
    session$sendCustomMessage(type = "closeWindow", list())
  })
}

# 运行应用
shinyApp(ui, server)
