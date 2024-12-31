library(shiny)
library(DT)

# 启动web应用
runApp(shinyApp( # 应用程序
  ui = fluidPage ( # ui 流式页面
    titlePanel="web应用", # 页面标题
    sidebarLayout ( # 边栏布局
      sidebarPanel ( # 侧边
        selectizeInput("symbol", "选择期货合约", choices="ma505,rb2505,ao2505,cu2505,eb2505,fg505,hc2505,i2505,j2505,jm2505,l2501" |> 
          strsplit("[,;]+") |> unlist())
      ), mainPanel ( # 内容
        plotOutput("tickdata_chart"), # 交易数据图
        dataTableOutput("tickdata_ds") # 交易数据源头
      ) # mainPanel
    ) # sidebarLayout
  ), server = \(input, output, session) { # server 后台处理程序
      observeEvent(input$symbol, { # 监听合约内容变化
        print(input$symbol)
      }) # observeEvent symbol
      output$tickdata_chart <- renderPlot({
        symbol <- as.name(input$symbol) # 转换成符号确保没有引号
        ds <- substitute(tickdata.h10ctp2(symbol, n=100)) |> eval() |> tanalyze() # 交易分析
        output$tickdata_ds <- renderDT(ds) # 交易数据源
        ds|> with(substr(Name,1,2)) |> table() |> pie() # 绘制饼图
      }) # tickdat_chart
}), port=7070) # runApp
