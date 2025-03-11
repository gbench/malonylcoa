library(shiny)
library(DT)
batch_load() # gbench 环境初始化

# 创建App
createApp <- \(..., settings = list(...)){
  \(side_ctrls, main_ctrls){
    shinyApp( # shinyApp应用对象创建，shinyApp环境与参数, 通过匿名函数的嵌套构造出结构层级与变量作用域
      # 前端页面设计
      ui = fluidPage(
        titlePanel = "web应用", # 页面标题
        sidebarLayout(do.call(sidebarPanel, args = side_ctrls), do.call(mainPanel, args = main_ctrls))
      ), # 页面设置
      # 后端处理逻辑
      server = \(input, output, session){ # server 后台处理程序
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

#' 数据表是否存在
#' @param tbl 数据表
#' @return 存在的标志
tblexists <- \(...) {
  "SELECT COUNT(*) > 0 flag FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '%s'" |>
    sprintf(c(...)) |>
    sqlquery.inv() |>
    Reduce(f = dplyr::bind_rows, x = ) |>
    (\(.) structure(if (!is.na(match("flag", names(.)))) .$flag else ., names = c(...)))()
}

# 数据透视表
pivotTable <- \() {
  tbls <- sqlquery.inv("show tables") |> unlist() |> grep(pattern="^t_([^_]+)_([^_]+)$", value=T) |> unlist() # 提取数据表
  if (length(tbls) <= 0) { # 没有数据表
    data.frame() # 空图表
  } else { # 数据表
    tbls |> sprintf(fmt = "select * from %s") |> sqlquery.inv(simplify=F) |> (\(.){
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
  } # if
}

# id 主键
idpk <- \(x) sub(pattern = "\\(\n", replacement = "(\n  id int primary key auto_increment,\n", x = x)

# 产品列表
products <- c("苹果" = "apple001", "香蕉" = "banana001", "草莓" = "strawberry001") # 产品列表
# 公司列表
companies <- c("中国第一出口公司" = "001", "华北进出口公司" = "002") # 公司
# 仓库列表
warehouses <- c("北京仓库" = "001", "上海仓库" = "002") # 产品列表

# 侧边面板控件
side_ctrls = list( # 侧边面板控件
  selectInput("product_id", "产品:", products),
  selectInput("company_id", "公司:", companies),
  selectInput("warehouse_id", "仓库:", warehouses),
  checkboxInput("direction", "是否出库,T:出库,F:入库", F), # 出库入库
  numericInput("quantity", "数量", value = 10, min = 0, max = 100),
  textInput("bill_id", "单据编码", value = "--"),
  actionButton("submit", "提交") # 数据提交
)
# 主面板控件
main_ctrls = list( # 主面板控件
  dataTableOutput("dt") # 数据表
)

#‘ 表单id生成
#’ @param direction 出入库标志 T出库, F 入库
billid <- \(direction) sprintf("%s%s", ifelse(direction, "OUT", "IN"), strftime(Sys.time(), "%Y%m%d%H%M%OS"))

# 事件处理器
event_handler <- \(input, output, session) {
  # 变更出入库标志
  observeEvent(input$direction, {
    updateTextInput(session, "bill_id", value = billid(input$direction))
  })
  # 变更产品选择
  observeEvent(input$product_id, {
    print(input$product_id)
  })
  # 按钮提交
  observeEvent(input$submit, {
    drcr <- ifelse(input$direction, -1, +1) # 出库-1, 入库+1
    product_id <- input$product_id
    ps <- regexec(pattern = "([[:alpha:]]+)(\\d+)", text = product_id) |> regmatches(product_id, m = _) |> unlist()
    name <- ps[2]
    cttm <- Sys.time()
    data <- tribble( #
      ~bill_id, ~name, ~quantity, ~drcr, ~product_id, ~company_id, ~warehouse_id, ~create_time,
      input$bill_id, name, input$quantity, drcr, input$product_id, input$company_id, input$warehouse_id, cttm
    ) # data
    print(data)
    tbl <- sprintf("t_%s_%s", name, strftime(cttm, format = "%Y%m%d")) # 船舰数据表名
    if (!tblexists(tbl))  ctsql(data, tbl) |> idpk() |> print() |> sqlexecute.inv() # 创世数据表
    insql(data, tbl) |> print() |>  sqlexecute.inv()
    output$dt <- renderDT(pivotTable()) # 数据刷新
    updateTextInput(session, "bill_id", value = billid(input$direction)) # 更新表单id
  })
} # event_handler

# 渲染处理器
render_handler <- \(input, output, session) { # 初始图像绘制
  output$dt <- renderDT(pivotTable())
} # render_handler

# 数据应用
app <- createApp(event_handler, render_handler)(side_ctrls, main_ctrls)

# 启动app
runApp(app, port = 7070)
