library(shiny)
library(DT)

# 启动web应用
runApp((\(..., settings=list(...)) (\(side_ctrls, main_ctrls) # shinyApp环境与参数, 通过匿名函数的嵌套构造出结构层级与变量作用域
 shinyApp( # shinyApp 应用对象创建
    #前端页面设计
    ui=fluidPage( titlePanel="web应用", # 页面标题
      sidebarLayout( do.call(sidebarPanel, args=side_ctrls), do.call(mainPanel, args=main_ctrls)) ), # 页面设置 
      # 后端处理逻辑
      server=\(input, output, session) { # server 后台处理程序
        with(settings, { # 处理页面的各种事件相应与状态渲染
          event_handler(input, output, session)	# 式样响应
          render_handler(input, output, session)  # 状态渲染
       })})
   ) ( # shinyApp 的业务定制
      side_ctrls=list( # 侧边面板控件
        selectizeInput("symbol", "选择期货合约", choices=settings$symbols)), # 期货合约列表
      main_ctrls=list( # 主面板控件
        plotOutput("tickdata_chart"), # 交易数据图
        dataTableOutput("tickdata_ds")) # 交易数据源头
     ) # shinyApp 应用对象创建
 ) ( # 系统设置
    symbols=sqlquery('show tables') |> unlist() |> sub(pattern="t_(\\w+\\d+)_(\\d+)$", "\\1", x=_) |> unique(), # 合约代码
    event_handler=\(input, output, sesssion){ # 事件处理
      observeEvent(input$symbol, { # 监听合约内容变化
        print(input$symbol)
      }) # observeEvent symbol
    }, # event_handler
    render_handler=\(input, output, session){ # 页面渲染处理
     output$tickdata_chart <- renderPlot({
      symbol <- as.name(input$symbol) # 转换成符号确保没有引号
      ds <- substitute(tickdata.lhctp2(symbol, date='2024-12-19', n=100)) |> eval() |> tanalyze() # 交易分析
      output$tickdata_ds <- renderDT(ds) # 交易数据源
      ds |> with(substr(Name,1,2)) |> table() |> pie() }) 
    } # render_handler
 ), port=7070) # runApp
