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

# 读取 js 代码文件：plotly　的前端页面交互程序，bar 被点击时候的事件处理逻辑
on_bar_click <- file.path(home, "js", "on_bar_click.js") |> ( \(f) 
  if (file.exists(f)) readLines(f) |> paste(collapse = "\n") # 文件存在,则返回文件文本
  else NULL # 文件不存在返回NULL
) ()

#' 数据透视表
#' @param formula 透视表核算的枢轴公式（公式）
#' @return 数据透视表
pivotTable <- \(formula=cbind(total_in, total_out, qty, times) ~ name + date + company_id + warehouse_id) {
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

# ************************************************************************************
# 事件处理器（手动控制逻辑：各种控件的事件响应逻辑)
# 通过定义与设计页面控件的事件回调（响应）函数来控制页面组件状态
# ************************************************************************************
event_handler <- \(input, output, session) {
  
  #‘ 表单id生成
  #’ @param direction 出入库标志 T出库, F 入库
  bill_id_of <- \(direction) sprintf("%s%s", ifelse(direction, "OUT", "IN"), strftime(Sys.time(), "%Y%m%d%H%M%OS"))
  
  #' 变更出入库标志
  observeEvent(input$direction, {
    updateTextInput(session, "bill_id", value = bill_id_of(input$direction))
  })
  
  #' 变更产品选择
  observeEvent(input$product_id, {
    # print(input$product_id)
  })
  
  #' 按钮提交
  observeEvent(input$submit, {
    cttm <- Sys.time() # 系统时间
    drcr <- ifelse(input$direction, -1, +1) # 出库-1, 入库+1
    product_id <- input$product_id # 产品id
    ps <- regexec("([_[:alpha:]]+)(\\d+)", product_id) |> regmatches(product_id, m = _) |> unlist() # 产品id分析
    name <- ps[2] # 提取名称
    description <- (\(keys) sprintf("%s-%s", keys[match(drcr, c(1, -1))], product_id) |> toupper()) (c("IN", "OUT"))
    items <- tribble( # 数据项目
      ~bill_id, ~name, ~quantity, ~drcr, ~product_id, ~company_id, ~warehouse_id, ~create_time, ~description, # 字段名称
      input$bill_id, name, input$quantity, drcr, input$product_id, input$company_id, input$warehouse_id, cttm, description, # 数据行
    ) # items
    # print(items)
    
    tbl <- sprintf("t_%s_%s", name, strftime(cttm, format = "%Y%m%d")) # 确定数据的插入数据表名
    add_items(items, tbl) # 插入数据项目
    
    updateTextInput(session, "bill_id", value = bill_id_of(input$direction)) # 更新 出入库单id
    updateTextInput(session, "timestamp", value = Sys.time()) # 更新timestamp,通知响应式对象(由render_handler定义维护)作状态同步&刷新
  }) # input$submit

  #' 关闭shiny应用程序
  observeEvent(input$stopApp, {
    stopApp()
  }) # input$stopApp
  
} # event_handler

# ************************************************************************************
# 渲染处理器（自动控制逻辑：自维护控件的状态刷新逻辑)
# 通过将页面控件与某响应式对象(interactive组件)相绑定来动态跟踪响应式对象的状态变化
# ************************************************************************************
render_handler <- \(input, output, session) { # 初始图像绘制
  
  pvtdata <- reactive({ # 创建一个响应式-二维表对象，跟踪：input的timestamp, pivot_path成员变量
    print(sprintf("刷新透视图:%s",input$timestamp)) # 刷新数据表
    pivotTable(as.formula(input$pivot_path)) # 数据透视表
  }) # 数据透视表数据
  bchart <- reactive({ # 创建一个响应式-柱形图对象 - 跟踪：响应式对象 pvtdata 的 数据状态
    p <- pvtdata() |> (\(data) { # 根据透视表返回的结果状态创造响应的绘图对象
      if ( nrow(data) < 1 ) ggplot() # 数据表没有数据
      else data |> # 数据含有数据,提取透视表数据
        pivot_longer(cols=c(total_in, total_out, qty), names_to="state", values_to="volume") |> # 长格式变换
        transform( # 字段值变换
          place=paste0(company_id, warehouse_id) |> toupper() |> # 拼接 场所位置为 C{公司ID}W{仓库ID}
            gsub((\(s)sprintf(fmt="%s%s",s,s))("([[:alpha:]])[[:alpha:]]+([[:digit:]]+)"), "\\1\\2\\3\\4", x=_) # 格式简化
        ) |> ggplot(aes(name, y=volume, fill=state, color=place, alpha=date)) + # ggplot数据绘图
        geom_bar(position = "dodge", stat="identity") + # 绘制条形图 
        theme( # 设置主题
          text = element_text(family = "Helvetica", size = 12), # 设置整体字体
          plot.title = element_text(size = 16, face = "bold", hjust = 0.5), # 调整绘图区域标题样式
          axis.title = element_text(size = 12, face = "bold"), # 调整坐标轴标题样式
          axis.text = element_text(size = 12), # 调整坐标轴文本样式
        ) + scale_fill_manual( # 填充 特征的绘图配置
          values = c("total_in"="#0073C2FF", "total_out"="#EFC000FF", "qty"="#d65555FF"), # 设置各个值的颜色映射
          labels = c("total_in"="入库量", "total_out"="出库量", "qty"="余量") # 文本值
        ) + labs(title = "INVENTORY存货分布状况", x = "NAME产品", y = "VOLUME数量", fill = "库存状态", color="仓库位置", alpha="日期时间") # 绘图p对象
    })() # p plot 绘图结构
    ggplotly(p, tooltip = c("x", "y", "fill", "color", "alpha")) # 动态绘图
  }) # 响应式对象-数据图表
  
  # 绘制二维表
  output$dt <- renderDT(pvtdata()) # 数据图表 - 跟踪 pvtdata 数据状态
  # 绘制柱形图，增加页面组件bar点击事件
  output$bcplotly <- renderPlotly(bchart() |> (\(p)if (is.null(on_bar_click)) p else p |> onRender(on_bar_click))())

} # render_handler

# ************************************************************************************
# 创建并启动app, 指定运行端口号为 7070
# ************************************************************************************
createApp(event_handler, render_handler)(side_ctrls(), main_ctrls()) |> runApp(port = 7070)
