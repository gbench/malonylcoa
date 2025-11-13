# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-主数据对象
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

#' 该函数依赖于sqlquery.inv,故需要在引入了sqlquery.inv 的环境中运行,比如：source(file.path(home, "app.R"))
#' 生成下拉选项集合函数
#' @param tbls 数据表名向量
#' @param n 最大返回数据行数
#' @param simplify 对于只有一行数据的情况，是否进行简化处理，默认给予简化，返回向量而不是列表
opts <- \(tbls, n=-1, simplify=T) "select * from %s %s" |> sprintf(tbls, ifelse(n<0, "", paste("limit", n))) |>
  sqlquery.inv(simplify=F) |> lapply((\(.) structure(.$code, names=.$name))) |> # 拼接一条以code为值,name为名称的向量
  (\(.) structure(., names=names(.) |> sub(".*from\\s+(\\S+)(\\s*.*)", "\\1", x=_))) () |> # 提取表名
  (\(.) if(simplify & length(.)<2) .[[1]] else .) () # 对于只有一行数据的情况，进行简化处理

#' 带有默认值的opts2
#' 该函数依赖于sqlquery.inv,故需要在引入了sqlquery.inv 的环境中运行,比如：source(file.path(home, "app.R"))
#' 生成下拉选项集合函数
#' @param tbls 数据表名向量
#' @param default 最大返回数据行数
#' @param n 最大返回数据行数
opts2 <- \(tbls, default, n=-1) match(tbls, sqlquery.inv("show tables") |> unlist()) |> is.na() |> 
  (\(bs) seq(bs) |> lapply(\(i, b=bs[i]) if(b) default else opts(tbls[i], n, T))) () |> # 若数据表不存在返回默认值
  (\(.) if(length(.)<2) .[[1]] else .) () # 对于只有一行数据的情况，进行简化处理

# 产品列表
products <- opts2("t_product", c("苹果" = "apple001", "香蕉" = "banana001", "草莓" = "strawberry001"), 10)
# 公司列表
companies <- opts2("t_company", c("沃尔玛" = "CMPN001", "亚马逊" = "CMPN002"), 10)
# 仓库列表
warehouses <- opts2("t_warehouse", c("北京京邦达贸易有限公司" = "WRHS001", "顺丰控股股份有限公司" = "WRHS002"), 10)

# 默认统计公式
default_path <- "cbind(total_in, total_out, qty, times) ~ name + date + company_id + warehouse_id"
