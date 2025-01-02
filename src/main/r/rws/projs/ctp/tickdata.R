# ------------------------------------------------------------------------------------------------------------
# 简单的交易数据的shinyApp的应用演示示例
#
# 加载说明:
# 1）环境变量设置:export RWS_URL=https://gitee.com/gbench/malonylcoa/tree/master/src/main/r/rws;
# 2）程序启动需要加载:$RWS_URL/scripts之下的函数到.GlobalEnv; 
# 3）可采用:$RWS_URL/etc/Rprofile.site 作为启动R的环境配置。
# ------------------------------------------------------------------------------------------------------------

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
        lapply(c(event_handler, render_handler), \(f) f(input, output, session)) # 事件与状态渲染，注册reactive 
      })}) # shinyApp
    ) ( # shinyApp应用对象定制
         side_ctrls=list( # 侧边面板控件
           selectizeInput("symbol", "选择合约代码", choices=settings$symbols), # 期货合约列表
           selectizeInput("date", "选择数据日期", choices=settings$dates), # 期货合约列表
           sliderInput.fetch_size(settings$symbols[1], settings$dates[1]), # 读取数据数量
           actionButton("update_time", "刷新时间"),textOutput("time")
         ), main_ctrls=list( # 主面板控件
           plotOutput("tickdata_chart"), # 交易数据图
           dataTableOutput("tickdata_ds")) # 交易数据源头
      ) # shinyApp 应用对象创建
  ) (# 系统设置，需要注意这里是一个实际参数的初始列表，也就是各个参数是独立的，
     # 后面的参数是不能引用前面的参数内容，比如dates与symbols就是互而不见
     # ns:tickdata 空间-函数的定义
     if(searchpaths()[2] != 'ns:tickdata') { # ns:tickdata 安置位置为2#searchpath
       attach(NULL, name="ns:tickdata", pos=2) # 创建一个临时函数空间 tickdata
     }, # ns:tickdata
     if(is.null(tryCatch(get('extract_part', pos=2), error=\(e) NULL))) { # extract_part函数的位置注入
       # 提取合约表名成的结构部件：i 提取组件部分，1：合约名，2：合约日期，symbol 合约编码, NULL 提取所有合约
       assign('extract_part', \(i, symbol=NULL) { #  extract_part
         pattern <- sprintf("t_(%s)_(\\d+)$", if(is.null(symbol)) "\\w+\\d+" else symbol) # 表名式样
         sqlquery('show tables') |> unlist() |> grep(pattern, value=T, x=_) |> sub(pattern, sprintf("\\%s", i), x=_) |> unique() # 去重
       }, pos=2) # extract_part
       
       # 提取指定合约的日期列表，当symbol返回所有所有合约的各个日期集合
       assign('extract_dates', \(symbol=NULL) { #  extract_dates
         extract_part(2, symbol) |> sub("(\\d{4})(\\d{2})(\\d{2})", "\\1-\\2-\\3", x=_) # 日期格式
       }, pos=2) # extract_dates
       
       # symbol 合约代码, date 日期 , session 会话对象
       assign('sliderInput.fetch_size', \(symbol, date, session=NULL) { # fetch_size_ctrl
         sql <- sprintf("select count(*) cnt from t_%s_%s", symbol, gsub("-", "", date))
         size <- sqlquery(sql) |> unlist() |> getElement(1)
         if (is.null(session))
           sliderInput("fetch_size", "读取数据数量", min=0, max=size, value=100) # 读取数据数量
         else 
           updateSliderInput(session, "fetch_size", min=0, max=size, value=100)  # 更新数据数量
       } , pos=2)
    }, # extract_part函数的位置注入
     
     # 常量定义
     symbols=extract_part(1), # 合约代码
     dates=extract_dates(), # 合约日期
     
     # -----------------------------------------------------------------------------
     # 主要后台回调函数
     # -----------------------------------------------------------------------------
     event_handler=\(input, output, session) { # 事件处理
       observeEvent(input$symbol, { # 监听合约内容变化
         symbol <- input$symbol # 合约编码
         dates <- extract_dates(symbol) # 提取指定合约的日期
         if(length(dates) > 0) { # 更新日期选项
           date <- dates[1]
           updateSelectizeInput( session, "date", choices=dates, selected=date )
           sliderInput.fetch_size(symbol, date, session) # 更新fetch_size组件
         } # if
       }) # observeEvent symbol
       output$time <- renderText({ format(Sys.time(), "%a %b %d %X %Y") }) |> bindEvent(input$update_time)
     }, # event_handler
     render_handler=\(input, output, session) { # 页面渲染处理
       output$tickdata_chart <- renderPlot({
         symbol <- as.name(input$symbol) # 转换成符号确保没有引号
         date <- input$date # 转换成符号确保没有引号
         ds <- substitute(tickdata.lhctp2(symbol, date=date, n=input$fetch_size)) |> eval() |> tanalyze() # 交易分析
         output$tickdata_ds <- renderDT(ds) # 交易数据源
         ds |> with(substr(Name,1,2)) |> table() |> pie() }) 
     } # render_handler
  ), port=7070) # runApp
