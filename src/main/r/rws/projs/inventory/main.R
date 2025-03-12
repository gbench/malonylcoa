# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

home <- dirname(sys.frame(1)$ofile) # 文件基准路径

source(file.path(home, "app.R")) # creatApp 与 数据库函数
source(file.path(home, "ctrls.R")) # 页面控件

# 产品列表
products <- c("苹果" = "apple001", "香蕉" = "banana001", "草莓" = "strawberry001") # 产品列表
# 公司列表
companies <- c("中国第一出口公司" = "001", "华北进出口公司" = "002") # 公司
# 仓库列表
warehouses <- c("北京仓库" = "001", "上海仓库" = "002") # 产品列表
# 默认统计公式
default_path <- "cbind(total_in, total_out, qty, times) ~ name + date + company_id + warehouse_id"

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

# 事件处理器
event_handler <- \(input, output, session) {
  
  #‘ 表单id生成
  #’ @param direction 出入库标志 T出库, F 入库
  bill_id_of <- \(direction) sprintf("%s%s", ifelse(direction, "OUT", "IN"), strftime(Sys.time(), "%Y%m%d%H%M%OS"))
  
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
    par(mar = c(0, 0, 0, 0) + 0.1) # 设置图形
    print(sprintf("刷新透视图:%s",input$timestamp)) # 刷新数据表
    p <- pivotTable(as.formula(input$pivot_path)) |> 
      pivot_longer(cols=c(total_in, total_out, qty), names_to="type") |>
      ggplot(aes(name, y=value, fill=type)) + # 数据绘图
      geom_bar(position = "dodge", stat="identity") # 绘制条形图 
    ggplotly(p, tooltip = c("x", "y")) # 动态绘图
  }) # 数据图表
  output$bcplotly <- renderPlotly(bchart()) # 动态图表
  
} # render_handler

# ------------------------------------------------------------------------------------
# 创建并启动app, 指定运行端口号为 7070
# ------------------------------------------------------------------------------------
createApp(event_handler, render_handler)(side_ctrls(), main_ctrls()) |> runApp(port = 7070)
