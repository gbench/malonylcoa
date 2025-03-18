# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-主程序
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

source(file.path(home, "app.R")) # 应用框架：creatApp 与 数据库函数
source(file.path(home, "mastdata.R")) # 应用主数据
source(file.path(home, "ctrls.R")) # 页面控件

# ************************************************************************************
# 事件处理器（手动控制逻辑：各种控件的事件响应逻辑)
# 通过定义与设计页面控件的事件回调（响应）函数来控制页面组件状态
# ************************************************************************************
event_handler <- \(input, output, session) {

  #' 关闭shiny应用程序
  observeEvent(input$stopApp, {
    stopApp()
  }) # input$stopApp
  
}  

# ************************************************************************************
# 渲染处理器（自动控制逻辑：自维护控件的状态刷新逻辑)
# 通过将页面控件与某响应式对象(interactive组件)相绑定来动态跟踪响应式对象的状态变化
# ************************************************************************************
render_handler <- \(input, output, session) { # 初始图像绘制
  as.time <- \(time) as.POSIXct(time, format="%H:%M") # 时间格式
  data <- reactive({
      fetch <- \(symbol, date) "select * from t_%s_%s" |> 
        sprintf(symbol, strftime(as.Date(date), "%Y%m%d")) |> sqlquery() |>
        group_by(time=substr(UpdateTime, 1, 5) |> as.time()) |> 
        summarize(y=mean(LastPrice))
      # 读取数据
      fetch(input$symbol, input$date) |> filter(as.time(input$start_time) < time & time<as.time(input$end_time))
  }) # data
    
  output$dt <- renderDT(datatable(data(), options=list(pageLength=5))) # renderDT
  output$pt <- renderPlot({ data() %>% mutate(yhat=lm(y~seq_along(time), data=.) |> predict()) %>% 
    ggplot(aes(time, y)) + geom_point() + geom_line(aes(y=yhat), col="red")
  }) # renderPlot
} # render_handler

# ************************************************************************************
# 创建并启动app, 指定运行端口号为 7070
# ************************************************************************************
createApp(event_handler, render_handler)(side_ctrls(), main_ctrls()) |> runApp(port = 7070)
