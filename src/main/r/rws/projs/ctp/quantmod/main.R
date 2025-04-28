# ------------------------------------------------------------------------------------
# QUANtMOD-量化金融-寻找买点与卖点-主程序
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

home <- dirname(sys.frame(1)$ofile) # 文件基准路径

# ************************************************************************************
# 注意：R语言的文件引用包含采用:library 与 source 两种方式
# 1）library 相当于C的 标准位置 包含 即 include <>
# 2）source  相当于C的 本地内容 包含 即 include ""
# ************************************************************************************
# 导入文件
import_files <- \(dir, pattern=".R$") list.files(recursive = T, path = file.path(home, dir), full.names = T) |> lapply(source)
import_files("apl") # 应用程序框架
import_files("kdj/indicators") # 页面指标
import_files("util") # 辅助函数, get_xxx 系列(dbs, tbls, instruments)

# ************************************************************************************
# 基础函数（手动控制逻辑：各种控件的事件响应逻辑)
# 数据操作函数
# ************************************************************************************

# ************************************************************************************
# 事件处理器（手动控制逻辑：各种控件的事件响应逻辑)
# 通过定义与设计页面控件的事件回调（响应）函数来控制页面组件状态
# ************************************************************************************
event_handler <- \(input, output, session) {

  #' 关闭shiny应用程序
  observeEvent(input$stopApp, {
    stopApp()
  }) # input$stopApp

  observeEvent(input$dbhost, { # 监听数据库ip内容变化
    update.adhoc(input$dbhost, input$dbname) # 环境更新
    line = paste0("dbhost:", input$dbhost, ", dbname:", input$dbname, ", tables：", env_adhoc$sqlquery("select database()"))
    output$debug = renderText(line)
    updateSelectInput(session, "dbname", choices = get_dbs()) # 更新数据表名
  }) # observeEvent symbol

  observeEvent(input$dbname, { # 监听数据库名称内容变化
    update.adhoc(input$dbhost, input$dbname) # 环境更新
    line = paste0("dbhost:", input$dbhost, ", dbname:", input$dbname, ", tables：", env_adhoc$sqlquery("select database()"))
    output$debug = renderText(line)
    updateSelectInput(session, "instrument", choices = get_instruments()) # 更新数据表名
  }) # observeEvent symbol
  
  observeEvent(input$instrument, { # 监听数据库名称内容变化
    updateSelectInput(session, "datatbl", choices = get_tbls(input$instrument)) # 更新数据表名
  }) # observeEvent symbol
  
}  

# ************************************************************************************
# 渲染处理器（自动控制逻辑：自维护控件的状态刷新逻辑)
# 通过将页面控件与某响应式对象(interactive组件)相绑定来动态跟踪响应式对象的状态变化
# ************************************************************************************
render_handler <- \(input, output, session) { # 初始图像绘制
  as.time <- \(time) as.POSIXct(time, format="%H:%M") # 时间格式
  data <- reactive({
      fetch <- \(symbol) # 根据合约代码提取数据
        if(anyNA(symbol) || regexec(pat="^[\\s-]*$", symbol)>=0) 
          NA # 非法表名 
        else "select * from %s" |> sprintf(symbol) |> env_adhoc$sqlquery() |>
          group_by(time=substr(UpdateTime, 1, 5) |> as.time()) |> 
            summarize(y=mean(LastPrice))
      # 读取数据表
      if(anyNA(input$datatbl)) NA else {
        fetch(input$datatbl) |> (\(dfm)
        if(anyNA(dfm)) NA else dfm |> filter( 
          as.time(input$start_time) < time & time<as.time(input$end_time)) |>
            (\(x) if(anyNA(x) || nrow(x) < 1) NA else x ) ()
        ) () # if
      } # if
  }) # data
    
  output$dt <- renderDT({
    data() |> (\(x) if(anyNA(x) || nrow(x) <1) data.frame() else 
      datatable(x, options=list(pageLength=5)))()
  }) # renderDT
  output$pt <- renderPlot({
    data() |> (\(x) if(anyNA(x) || nrow(x) <1) ggplot() else {
      print(x)
      x %>% mutate(yhat=lm(y~seq_along(time), data=.) |> predict()) %>% 
      ggplot(aes(time, y)) + geom_point() + geom_line(aes(y=yhat), col="red")
    })()
  }) # renderPlot
} # render_handler

# ************************************************************************************
# 创建并启动app, 指定运行端口号为 7070
# ************************************************************************************
createApp(event_handler, render_handler)(side_ctrls(), main_ctrls()) |> runApp(port = 7070)
