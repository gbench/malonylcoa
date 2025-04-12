# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-页面控件对象
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

#' 侧边面板控件
side_ctrls <- \() list( # 侧边面板控件
  dateInput("date", "日期", value=current_date),
  selectInput("symbol", "合约:", symbols),
  timeInput("start_time", "开始时间", value = startime),
  timeInput("end_time", "结束时间", value = endtime),
  actionButton("stopApp", "关闭") # 关闭应用
)

#' 主面板控件
main_ctrls <- \() list( # 主面板控件
  dataTableOutput("dt"), # 数据表
  plotOutput("pt") # 数据图
)
