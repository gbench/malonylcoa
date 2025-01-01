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
  ) (# 系统设置，需要注意这里是一个实际参数的初始列表，也就是各个参数是独立的，后面的参数是不能引用前面的参数内容，比如dates与symbols就是互而不见
     # ns:tickdata 空间-函数的定义
     if(searchpaths()[2] != 'ns:tickdata') { # ns:tickdata
       attach(NULL, name="ns:tickdata", pos=2) # 创建一个临时函数空间 tickdata
     }, # ns:tickdata
     if(is.null(get("extract_part",pos=2))) { # extract_part
       # 提取数据部件：i 提取组件部分，symbol 合约编码, NULL 表格所有合约
       assign('extract_part', \(i, symbol=NULL) { 
         pattern <- sprintf("t_(%s)_(\\d+)$", if(is.null(symbol)) "\\w+\\d+" else symbol) # 表名式样
         sqlquery('show tables') |> unlist() |> grep(pattern, value=T, x=_) |> sub(pattern, sprintf("\\%s", i), x=_) |> unique() # 去重
       }, pos=2) # assign
    }, # ns:tickdata 空间-函数的定义
     
     # 常量定义
     symbols=extract_part(1), # 合约代码
     dates=extract_part(2) |> sub("(\\d{4})(\\d{2})(\\d{2})", "\\1-\\2-\\3", x=_), # 合约日期
     
     # -----------------------------------------------------------------------------
     # 主要后台回调函数
     # -----------------------------------------------------------------------------
     event_handler=\(input, output, session) { # 事件处理
       observeEvent(input$symbol, { # 监听合约内容变化
         symbol <- input$symbol # 合约编码
         dates <- extract_part(2,symbol) |> sub("(\\d{4})(\\d{2})(\\d{2})", "\\1-\\2-\\3", x=_) # 日期格式
	       if(length(dates) > 0) { # 更新日期选项
	         updateSelectizeInput(session, "date", choices=dates, selected=dates[1] )
	        } # if
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
