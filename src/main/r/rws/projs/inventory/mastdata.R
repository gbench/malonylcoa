# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-主数据对象
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

# 产品列表
products <- c("苹果" = "apple001", "香蕉" = "banana001", "草莓" = "strawberry001") # 产品列表
# 公司列表
companies <- c("沃尔玛" = "CMPN001", "亚马逊" = "CMPN002") # 公司
# 仓库列表
warehouses <- c("北京京邦达贸易有限公司" = "WRHS001", "顺丰控股股份有限公司" = "WRHS002") # 产品列表

# 默认统计公式
default_path <- "cbind(total_in, total_out, qty, times) ~ name + date + company_id + warehouse_id"
