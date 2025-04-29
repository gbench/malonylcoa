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

# 时间格式化函数
as.time <- \(time) as.POSIXct(time, format="%H:%M") # 时间格式
# kdj 的绘图
plot_kdj<- \(data) {
  if(is.null(data) || nrow(data) < 1) ggplot()
  else {
    cross_data <- identify_kdj_cross(data) # 金叉数据
    cross_points <- cross_data$DateTime |> sapply(strftime, format="%H:%M") # 金叉数据时刻
    points <- data$DateTime |> sapply(\(x) if(is.na(x) || is.null(x)) "" else strftime(x, format="%H:%M")) # 交叉时刻
    cross_index <- match(cross_points, points) # 时点索引
    
    ggplot(data, aes(x=1:length(DateTime))) +
      geom_line(aes(y=K), color="red") +
      geom_line(aes(y=D), color="green") +
      geom_line(aes(y=J), color="blue") + 
      scale_x_continuous(breaks=\(x) cross_index, labels=\(x) points[cross_index]) + 
      labs(x="时间", y="kdj")
  } # if
}

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
    line = paste0("dbhost:", input$dbhost, ", dbname:", input$dbname, ", database：", env_adhoc$sqlquery("select database()"))
    output$debug = renderText(line)
    updateSelectInput(session, "dbname", choices = get_dbs()) # 更新数据表名
  }) # observeEvent symbol

  observeEvent(input$dbname, { # 监听数据库名称内容变化
    update.adhoc(input$dbhost, input$dbname) # 环境更新
    line = paste0("dbhost:", input$dbhost, ", dbname:", input$dbname, ", database：", env_adhoc$sqlquery("select database()"))
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
  
  lm_data <- reactive ({
      fetch <- \(symbol) # 根据合约代码提取数据
        if(anyNA(symbol) || regexec(pat="^[\\s-]*$", symbol)>=0) 
          NULL # 非法表名 
        else "select * from %s" |> sprintf(symbol) |> env_adhoc$sqlquery() |>
          group_by(time=substr(UpdateTime, 1, 5) |> as.time()) |> 
            summarize(y=mean(LastPrice))
      # 读取数据表
      if(anyNA(input$datatbl)) NULL else {
        fetch(input$datatbl) |> (\(dfm) if(anyNA(dfm)) NULL else dfm |> filter( 
          as.time(input$start_time) < time & time<as.time(input$end_time)) |>
            (\(x) if(anyNA(x) || nrow(x) < 1) NULL else x) ()
        ) () # if
      } # if
  }) # data
  
  # kdj数据
  kdj_data <- reactive({ # 计算kdj数据
    tickdata <- input$datatbl |> (\(x) if(is.null(x) || regexec(pat="^[\\s-]*$", x)>=0) NULL 
      else sprintf(fmt="select * from %s", x) |> env_adhoc$sqlquery()) ()
    dfm <- if(is.null(tickdata) || nrow(tickdata)<1) NULL else calculate_ohlcv(tickdata) |> 
      aggregate_ohlcv(paste(input$timeframe, "mins")) |> calculate_kdj() # kdj 数据
    dfm |> na.omit()
  }) # kdj_data
  
  # -----------------------------------------------------------------------------------
  # 数据渲染
  # -----------------------------------------------------------------------------------
  
  output$dt <- renderDT({ # 绘制数据
    {if (input$plotmode=="kdj") # kdj 模式
      kdj_data() |> (\(x) if (is.null(x)) data.frame() else x |> identify_kdj_cross()) () |> 
        (\(x) { if(nrow(x)<1) data.frame() else x |> transform ( # 数据格式变换
            DateTime=strftime(DateTime, "%H:%M"), # 日期格式化
            K=sprintf("%.2f",K), # K值
            D=sprintf("%.2f",D), # D值
            J=sprintf("%.2f",J) # J值
        )}) () 
    else lm_data() # 默认模式
    } |> datatable(options=list(pageLength=5)) |> na.omit()
  }) # renderDT
  
  output$pt <- renderPlot({ # 数据绘图
    if (input$plotmode == "kdj") # kdj 的金叉死叉模式
      kdj_data() |> plot_kdj()
    else { # 默认模式
      lm_data() |> (\(x) if(is.null(x) || nrow(x) <1) ggplot() else {
        x %>% mutate(yhat=lm(y~seq_along(time), data=.) |> predict()) %>% 
        ggplot(aes(time, y)) + geom_point() + geom_line(aes(y=yhat), col="red")
      }) ()
    } # if
  }) # renderPlot
  
} # render_handler

# ************************************************************************************
# 创建并启动app, 指定运行端口号为 7070
# ************************************************************************************
createApp(event_handler, render_handler)(side_ctrls(), main_ctrls()) |> runApp(port = 7070)
