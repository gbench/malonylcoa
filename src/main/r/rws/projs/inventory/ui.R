library(shiny)
library(DT)

# 创建App
createApp <- \(..., settings = list(...)){
  \(side_ctrls, main_ctrls) {
    shinyApp( # shinyApp应用对象创建
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
  }
} # createApp

#' create database inventory default character set utf8mb4
# inventory 数据库查询函数
sqlquery.inv <- partial(sqlquery, dbname = "inventory")

#' inventory 数据库执行函数
sqlexecute.inv <- partial(sqlexecute, dbname = "inventory")

# 数据刷新
refresh <- \()
sqlquery.inv("show tables") |>
  unlist() |>
  sprintf(fmt = "select * from %s") |>
  sqlquery.inv() |>
  (\(.){
    nms <- names(.)
    matches <- regexec(pattern = ".*\\s+(t_([^_]+)_([^_]+))$", text = nms) |>
      regmatches(nms, m = _) |>
      do.call(rbind, args = _)
    lapply(seq(nrow(matches)), \(i) transform(.[[i]], tbl = matches[i, 2], name = matches[i, 3], date = matches[i, 4])) |>
      Reduce(f = rbind) |>
      (\(.) { # 数据统计
        data <- transform(., qty = quantity * drcr, date = strftime(create_time, format = "%Y-%m-%d"), times = 1)
        print(data)
        aggregate(cbind(qty, times) ~ product_id + date + company_id + warehouse_id, data, sum) # 数据统计与透视
      })()
  })()

app <- createApp(
  event_handler = \(input, output, session) {
    observeEvent(input$variable, { # 监听合约内容变化
      print(input$variable)
    }) # observeEvent symbol
  },
  render_handler = \(input, output, session) {
    output$dt <- renderDT(refresh())
  }
)(side_ctrls = list( # 侧边面板控件
  selectInput("variable", "Variable:", c("Cylinders" = "cyl", "Transmission" = "am", "Gears" = "gear"))
), main_ctrls = list( # 主面板控件
  dataTableOutput("dt") # 数据表
))

# 启动app
runApp(app, port = 7070)
