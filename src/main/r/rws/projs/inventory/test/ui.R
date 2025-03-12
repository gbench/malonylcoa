library(shiny)
library(DT)

# 安装包（如果尚未安装）
if (!require(ggplot2)) install.packages("ggplot2")
if (!require(plotly)) install.packages("plotly")

batch_load() # gbench 环境初始化

#' 创建App， 定义 应用的图形控件的布局与动态响应逻辑
#' @param event_handler (1st) 手动控制逻辑:通过定义与设计页面控件的事件回调（响应）函数来控制页面组件状态
#' @param render_handler (1st) 自动控制逻辑:通过将页面控件与某响应式对象(interactive组件)相绑定来动态跟踪响应式对象的状态变化
#' @param settings (1st) 基础设置，至少需要包含：事件处理器event_handler, interactive页面渲染器render_handler
#' @param side_ctrls (2nd) 侧域控件，下拉：selectInput，点选：checkboxInput，按钮：actionButton 等
#' @param main_ctrls (2nd) 主域控件，数据：renderDT，图片：renderPlot 等
createApp <- \(..., settings = list(...)){ # 1st 第一层 应用逻辑
  #' @param side_ctrls 侧域控件，下拉：selectInput，点选：checkboxInput，按钮：actionButton 等
  #' @param main_ctrls 主域控件，数据：renderDT，图片：renderPlot 等
  \(side_ctrls, main_ctrls){ # 2nd 应用界面，页面布局的UI设计
    shinyApp( # shinyApp应用对象创建，shinyApp环境与参数, 通过匿名函数的嵌套构造出结构层级与变量作用域
      # 前端页面设计
      ui = fluidPage(# 流式布局
        titlePanel = "web应用", # 页面标题
        sidebarLayout(do.call(sidebarPanel, args = side_ctrls), do.call(mainPanel, args = main_ctrls))
      ), # ui 页面设置
      # 后端处理逻辑
      server = \(input, output, session){ # server 后台处理程序
        with(settings, { # 处理页面的各种事件相应与状态渲染
          # 手动控制逻辑event_handler:通过定义与设计页面控件的事件回调（响应）函数来控制页面组件状态
          # 自动控制逻辑render_handler:通过将页面控件与某响应式对象(interactive组件)相绑定来动态跟踪响应式对象的状态变化
          lapply(c(event_handler, render_handler), \(f) f(input, output, session)) # 事件与状态渲染，注册响应式reactive对象
        }) # with
      } # server
    ) # shinyApp
  } # side_ctrls, main_ctrls
} # createApp

#' create database inventory default character set utf8mb4

#' inventory 数据库查询函数
sqlquery.inv <- partial(sqlquery, dbname = "inventory")

#' inventory 数据库执行函数
sqlexecute.inv <- partial(sqlexecute, dbname = "inventory")

#' 判断数据表名是否存在
#' @param tbl 数据表
#' @return 存在的标志
tblexists <- \(...) {
  "SELECT COUNT(*) > 0 flag FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '%s'" |>
    sprintf(c(...)) |> sqlquery.inv() |> Reduce(f = dplyr::bind_rows, x = ) |>
    (\(.) structure(if (!is.na(match("flag", names(.)))) .$flag else ., names = c(...)))()
}

#' 数据透视表
#' @param formula 透视表核算的枢轴公式（公式）
pivotTable <- \(formula=cbind(qty, times) ~ product_id + date + company_id + warehouse_id) {
  tbls <- sqlquery.inv("show tables") |> unlist() |> grep(pattern="^t_([^_]+)_([^_]+)$", value=T) # 提取数据表
  if (length(tbls) <= 0) { # 没有数据表
    data.frame() # 空图表
  } else { # 数据表
    tbls |> sprintf(fmt = "select * from %s") |> sqlquery.inv(simplify=F) |> (\(rs){ # 数据查询
        sqls <- names(rs) # 提取各个结果集相应的查询sql语句
        matches <- regexec(".*\\s+(t_([^_]+)_([^_]+))$", text = sqls) |> regmatches(sqls, m = _) |> do.call(rbind, args = _)
        lapply(seq(nrow(matches)), \(i)
          transform(rs[[i]], tbl = matches[i, 2], name = matches[i, 3], date = matches[i, 4])) |>
            Reduce(f = rbind) |> (\(ds) { # 数据统计
              data <- transform(ds, # 字段定义
                times = 1, # 次数统计
                qty = quantity * drcr, # 数量统计
                total_in = ifelse(drcr==1, quantity, 0), # 入库数量
                total_out = ifelse(drcr==-1, quantity, 0), # 出库数量
                date = strftime(create_time, format = "%Y-%m-%d") # 日期
              ) # data
              # print(data)
              aggregate(formula, data, sum) # 数据统计与透视
            })() # 数据统计
      })() # tbls
  } # if 有没有数据表
} # pivotTable

# 产品列表
products <- c("苹果" = "apple001", "香蕉" = "banana001", "草莓" = "strawberry001") # 产品列表
# 公司列表
companies <- c("中国第一出口公司" = "001", "华北进出口公司" = "002") # 公司
# 仓库列表
warehouses <- c("北京仓库" = "001", "上海仓库" = "002") # 产品列表
# 默认统计公式
default_path <- "cbind(total_in, total_out, qty, times) ~ name + date + company_id + warehouse_id"

# 侧边面板控件
side_ctrls = list( # 侧边面板控件
  selectInput("product_id", "产品:", products),
  selectInput("company_id", "公司:", companies),
  selectInput("warehouse_id", "仓库:", warehouses),
  checkboxInput("direction", "是否出库,T:出库,F:入库", F), # 出库入库
  numericInput("quantity", "数量", value = 10, min = 0, max = 100),
  textInput("bill_id", "单据编码", value = "--"),
  textInput("timestamp", "时间戳", value = Sys.time()), # 用户响应式编程的刷星页面图表
  textInput("pivot_path", "数据透视路径", value = default_path),
  actionButton("submit", "提交") # 数据提交
)
# 主面板控件
main_ctrls = list( # 主面板控件
  dataTableOutput("dt"), # 数据表
  plotlyOutput("bcplotly") # 数据图
)

#‘ 表单id生成
#’ @param direction 出入库标志 T出库, F 入库
bill_id_of <- \(direction) sprintf("%s%s", ifelse(direction, "OUT", "IN"), strftime(Sys.time(), "%Y%m%d%H%M%OS"))

#’ 增加主键字段
#' @param xu 数据表定义
#' @param id 主键字段名称，默认为 id
add_pk <- \(x, pk="id") sub(pattern = "\\(\n", replacement = sprintf("(\n  %s int primary key auto_increment,\n", pk), x = x)

#' 添加数据
#’ @param items 数据项目
#’ @param tbl 数据表
add_items <- \(items, tbl) {
  # 数据库插入
  if (!tblexists(tbl)) ctsql(items, tbl) |> add_pk() |> print() |> sqlexecute.inv() # 创世数据表
  insql(items, tbl) |> print() |>  sqlexecute.inv() # 数据插入
}

# 事件处理器
event_handler <- \(input, output, session) {
  
  # 变更出入库标志
  observeEvent(input$direction, {
    updateTextInput(session, "bill_id", value = bill_id_of(input$direction))
  })
  
  # 变更产品选择
  observeEvent(input$product_id, {
    # print(input$product_id)
  })
  
  # 按钮提交
  observeEvent(input$submit, {
    
    drcr <- ifelse(input$direction, -1, +1) # 出库-1, 入库+1
    product_id <- input$product_id # 产品id
    ps <- regexec("([[:alpha:]]+)(\\d+)", product_id) |> regmatches(product_id, m = _) |> unlist() # 产品id分析
    name <- ps[2] # 提取名称
    cttm <- Sys.time() # 系统时间
    
    items <- tribble( # 数据项目
      ~bill_id, ~name, ~quantity, ~drcr, ~product_id, ~company_id, ~warehouse_id, ~create_time, # 字段名称
      input$bill_id, name, input$quantity, drcr, input$product_id, input$company_id, input$warehouse_id, cttm # 数据行
    ) # items
    # print(items)
    
    tbl <- sprintf("t_%s_%s", name, strftime(cttm, format = "%Y%m%d")) # 确定数据的插入数据表名
    add_items(items, tbl) # 插入数据
    
    output$dt <- renderDT(pivotTable(as.formula(input$pivot_path))) # 数据刷新
    updateTextInput(session, "bill_id", value = bill_id_of(input$direction)) # 更新表单id
    updateTextInput(session, "timestamp", value = Sys.time()) # 更新timestamp
    
  }) # input$submit
  
} # event_handler

# 渲染处理器
render_handler <- \(input, output, session) { # 初始图像绘制
  
  # 数据图表
  output$dt <- renderDT(pivotTable(as.formula(input$pivot_path)))
  
  bchart <- reactive({ # 数据绘图
    par(mar = c(0, 0, 0, 0) + 0.1) 
    print(sprintf("刷新透视图:%s",input$timestamp)) # 刷新数据表
    p <- pivotTable(as.formula(input$pivot_path)) |> 
      pivot_longer(cols=c(total_in, total_out, qty), names_to="type") |>
      ggplot(aes(name, y=value, fill=type)) + # 数据绘图
      geom_bar(position = "dodge", stat="identity") # 绘制条形图 
    ggplotly(p, tooltip = c("x", "y")) # 动态绘图
  }) # 数据图表
  output$bcplotly <- renderPlotly(bchart()) # 动态图表
  
} # render_handler

# 创建并启动app
createApp(event_handler, render_handler)(side_ctrls, main_ctrls) |> runApp(port = 7070)
