# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-应用框架
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

library(shiny)
library(DT)

# 安装包（如果尚未安装）
"ggplot2,plotly,htmlwidgets" |> strsplit(",") |> unlist() |> lapply(\(p) {
    if (!require(p, character.only = T)) install.packages(p)
    library(p, character.only = T)
  })

# batch_load函数，来自R的启动配置 https://gitee.com/gbench/malonylcoa/blob/master/src/main/r/rws/etc/Rprofile.site
# 本app程序需要按照Rprofile.site的的基础配置来进行运行 (sqlquery, sqlexecute 也是配置在该环境之中的)
batch_load() # gbench 环境初始化

#' 创建App： 定义 应用 之 图形控件 的 布局与动态响应 逻辑
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
          lapply(c(event_handler, render_handler), \(f) f(input, output, session)) # 事件&状态渲染，注册 响应式reactive对象
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

#' 增加主键字段
#' @param xu 数据表定义
#' @param id 主键字段名称，默认为 id
add_pk <- \(x, pk="id") sub(pattern = "\\(\n", replacement = sprintf("(\n  %s int primary key auto_increment,\n", pk), x = x)

#' 添加数据
#' @param items 数据项目（数据框对象）
#' @param tbl 数据表
add_items <- \(items, tbl) {
  # 数据库插入
  if (!tblexists(tbl)) ctsql(na.omit(items), tbl) |> add_pk() |> print() |> sqlexecute.inv() #  创建数据表
  insql(items, tbl) |> print() |>  sqlexecute.inv() # 数据插入
}

