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
runApp((\(..., settings = list(...)) (\(side_ctrls, main_ctrls) shinyApp( # shinyApp应用对象创建
  # shinyApp环境与参数, 通过匿名函数的嵌套构造出结构层级与变量作用域
  # 前端页面设计
  ui = fluidPage(
    titlePanel = "web应用", # 页面标题
    sidebarLayout(do.call(sidebarPanel, args = side_ctrls), do.call(mainPanel, args = main_ctrls))
  ), # 页面设置
  # 后端处理逻辑
  server = \(input, output, session) { # server 后台处理程序
    with(settings, { # 处理页面的各种事件相应与状态渲染
      lapply(c(event_handler, render_handler), \(f) f(input, output, session)) # 事件与状态渲染，注册reactive
    })
  }
) # shinyApp
)( # shinyApp应用对象定制
  side_ctrls = list( # 侧边面板控件
    selectizeInput("symbol", "选择合约代码", choices = settings$symbols), # 期货合约列表
    selectizeInput("date", "选择数据日期", choices = settings$dates), # 期货合约列表
    sliderInput.fetch_size(settings$symbols[1], settings$dates[1]), # 读取数据数量
    textInput("cmdline", "输入行:"), # 命令行
    checkboxInput("direction", "是否递增", T), # 递增方向
    actionButton("update_time", "刷新时间"), # 时间按钮
    verbatimTextOutput("time", placeholder = T) # 文本输出
  ), main_ctrls = list( # 主面板控件
    plotOutput("tickdata_chart"), # 交易数据图
    dataTableOutput("tickdata_dt")
  ) # 交易数据表
) # shinyApp 应用对象创建
)( # 系统设置，需要注意这里是一个实际参数的初始列表，也就是各个参数是独立的，
  # 后面的参数是不能引用前面的参数内容，比如dates与symbols就是互而不见
  # ns:tickdata 空间-函数的定义
  if (searchpaths()[2] != "ns:tickdata") { # ns:tickdata 安置位置为2#searchpath
    attach(NULL, name = "ns:tickdata", pos = 2) # 创建一个临时函数空间 tickdata
  }, # ns:tickdata
  if (is.null(tryCatch(get("extract_part", pos = 2), error = \(e) NULL))) { # extract_part函数的位置注入
    # 提取合约表名成的结构部件：i 提取组件部分，1：合约名，2：合约日期，symbol 合约编码, NULL 提取所有合约
    assign("extract_part", \(i, symbol = NULL) { #  extract_part
      pattern <- sprintf("t_(%s)_(\\d+)$", if (is.null(symbol)) "\\w+\\d+" else symbol) # 表名式样
      sqlquery("show tables") |>
        unlist() |>
        grep(pattern, value = T, x = _) |>
        sub(pattern, sprintf("\\%s", i), x = _) |>
        unique() # 去重
    }, pos = 2) # extract_part

    # 提取指定合约的日期列表，当symbol返回所有所有合约的各个日期集合
    assign("extract_dates", \(symbol = NULL) { #  extract_dates
      extract_part(2, symbol) |> sub("(\\d{4})(\\d{2})(\\d{2})", "\\1-\\2-\\3", x = _) # 日期格式
    }, pos = 2) # extract_dates

    # symbol 合约代码, date 日期 , session 会话对象
    assign("sliderInput.fetch_size", \(symbol, date, session = NULL) { # sliderInput.fetch_size
      sql <- sprintf("select count(*) cnt from t_%s_%s", symbol, gsub("-", "", date))
      fz_max <- sqlquery(sql) |>
        unlist() |>
        getElement(1) # 最大值
      fz_min <- min(0, fz_max) # fetch_size 的最小值
      value <- min(100, fz_max) # fetch_size 的值
      if (is.null(session)) { # 初始化
        sliderInput("fetch_size", "读取数据数量", min = fz_min, max = fz_max, value = value)
      } # 读取数据数量
      else { # 数据更新
        updateSliderInput(session, "fetch_size", min = fz_min, max = fz_max, value = value)
      } # 更新数据数量
    }, pos = 2)
  }, # extract_part函数的位置注入

  # 常量定义
  symbols = extract_part(1), # 合约代码
  dates = extract_dates(), # 合约日期

  # -----------------------------------------------------------------------------
  # 主要后台回调函数
  # -----------------------------------------------------------------------------
  event_handler = \(input, output, session) { # 事件处理
    observeEvent(input$symbol, { # 监听合约内容变化
      dates <- extract_dates(input$symbol) # 提取指定合约的日期
      if (length(dates) > 0) { # 更新日期选项
        date <- dates[1] # 日期
        updateSelectizeInput(session, "date", choices = dates, selected = date) # 更新日期
        sliderInput.fetch_size(input$symbol, input$date, session) # 更新sliderInput.fetch_size组件
      } # if
    }) # observeEvent symbol
    observeEvent(input$date, { # 监听合约内容变化
      sliderInput.fetch_size(input$symbol, input$date, session) # 更新sliderInput.fetch_size组件
    }) # observeEvent symbol
    output$time <- renderText({ # 刷新时间
      value <- input$fetch_size + 100 * (if (input$direction) 1 else -1) # fetch_size 的处理
      updateSliderInput(session, "fetch_size", value = max(1, value)) # 更新数据数量, 每一批增加100
      format(Sys.time(), "%a %b %d %X %Y")
    }) |> bindEvent(input$update_time)
  }, # event_handler
  render_handler = \(input, output, session) { # 页面渲染处理
    tickdata_pie <- reactive({ # tickdata_pie
      try(print(input$cmdline)) # 命令行
      symbol <- as.name(input$symbol) # 转换成符号确保没有引号
      date <- input$date # 转换成符号确保没有引号
      dt <- substitute(tickdata.lhctp2(symbol, date = date, n = input$fetch_size)) |>
        eval() |>
        tanalyze() # 交易分析
      output$tickdata_dt <- renderDT(dt) # 交易数据源
      dt |>
        with(substr(Name, 1, 2)) |>
        table() |>
        graphics::pie() # 绘制饼图
    }) # tickdata_pie
    output$tickdata_chart <- renderPlot(tickdata_pie())
    # output$tickdata_chart <- tdpie() |> bindEvent(input$update_time) # render_handler
  }), port = 7070) # runApp
