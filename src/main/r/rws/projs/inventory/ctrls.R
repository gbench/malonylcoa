#' 侧边面板控件
side_ctrls <- \() list( # 侧边面板控件
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

#' 主面板控件
main_ctrls <- \() list( # 主面板控件
  dataTableOutput("dt"), # 数据表
  plotlyOutput("bcplotly") # 数据图
)