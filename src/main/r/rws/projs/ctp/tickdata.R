library(shiny)
library(DT)

# 启动web应用
runApp( (\(side_ctrls, main_ctrls) shinyApp( # 应用程序
  ui=fluidPage( titlePanel="web应用", # 页面标题
    sidebarLayout( do.call(sidebarPanel, args=side_ctrls), do.call(mainPanel, args=main_ctrls))), # 页面设置 
	server=\(input, output, session) { # server 后台处理程序
    observeEvent(input$symbol, { # 监听合约内容变化
		  print(input$symbol)
		}) # observeEvent symbol
		output$tickdata_chart <- renderPlot ({
      symbol <- as.name(input$symbol) # 转换成符号确保没有引号
      ds <- substitute(tickdata.lhctp2(symbol, date='2024-12-19', n=100)) |> eval() |> tanalyze() # 交易分析
      output$tickdata_ds <- renderDT(ds) # 交易数据源
      ds|> with(substr(Name,1,2)) |> table() |> pie() # 绘制饼图
    }) # tickdat_chart
  })) ( list( # 侧边面板控件
		  selectizeInput("symbol", "选择期货合约", choices=sqlquery("show tables") |> unlist() |> # 期货合约列表
		    sub(pattern="t_(\\w+\\d+)_(\\d+)$", "\\1", x=_) |> unique() ) # 提取505合约
	  ), list( # 主面板控件
			plotOutput("tickdata_chart"), # 交易数据图
      dataTableOutput("tickdata_ds") # 交易数据源头
	)), port=7070) # runApp
