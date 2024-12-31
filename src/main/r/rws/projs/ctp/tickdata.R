library(shiny)
library(DT)

# 启动web应用
runApp( (\(..., settings=list(...)) (\(side_ctrls, main_ctrls) shinyApp( # shinyApp应用对象创建
    # shinyApp环境与参数, 通过匿名函数的嵌套构造出结构层级与变量作用域
    # 前端页面设计
    ui=fluidPage( titlePanel="web应用", # 页面标题
      sidebarLayout( do.call(sidebarPanel, args=side_ctrls), do.call(mainPanel, args=main_ctrls)) ), # 页面设置 
    # 后端处理逻辑
    server=\(input, output, session) { # server 后台处理程序
      with(settings, { # 处理页面的各种事件相应与状态渲染
        event_handler(input, output, session)	# 式样响应
        render_handler(input, output, session)  # 状态渲染
      })}) # shinyApp
    ) ( # shinyApp应用对象定制
         side_ctrls=list( # 侧边面板控件
           selectizeInput("symbol", "选择合约代码", choices=settings$symbols), # 期货合约列表
           selectizeInput("date", "选择数据日期", choices=settings$dates) # 期货合约列表
         ), main_ctrls=list( # 主面板控件
           plotOutput("tickdata_chart"), # 交易数据图
           dataTableOutput("tickdata_ds")) # 交易数据源头
      ) # shinyApp 应用对象创建
  ) ( # 系统设置
     symbols=sqlquery('show tables') |> unlist() |> sub(pattern="t_(\\w+\\d+)_(\\d+)$", "\\1", x=_) |> unique(), # 合约代码
     dates=sqlquery('show tables') |> unlist() |> sub(pattern="t_(\\w+\\d+)_(\\d+)$", "\\2", x=_) |> unique() |>  
       gsub("(\\d{4})(\\d{2})(\\d{2})", "\\1-\\2-\\3", x=_), # 日期 
     event_handler=\(input, output, sesssion) { # 事件处理
       observeEvent(input$symbol, { # 监听合约内容变化
         print(input$symbol)
       }) # observeEvent symbol
     }, # event_handler
     render_handler=\(input, output, session) { # 页面渲染处理
       output$tickdata_chart <- renderPlot({
         symbol <- as.name(input$symbol) # 转换成符号确保没有引号
         date <- input$date # 转换成符号确保没有引号
         ds <- substitute(tickdata.lhctp2(symbol, date=date, n=100)) |> eval() |> tanalyze() # 交易分析
         output$tickdata_ds <- renderDT(ds) # 交易数据源
         ds |> with(substr(Name,1,2)) |> table() |> pie() }) 
     } # render_handler
  ), port=7070) # runApp
