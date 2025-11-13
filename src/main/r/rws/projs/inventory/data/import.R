# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-主数据导入程序
#
# author:gbench@sina.com
# date: 2025-03-13
# ------------------------------------------------------------------------------------

# 安装并加载程序包
"RMySQL,tibble,dplyr,purrr,readxl" |> strsplit(",") |> unlist() |> lapply(\(p) {
  if (!require(p, character.only = T)) {
    install.packages(p)
    library(p, character.only = T)
  } # if
}) # lapply


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

#' inventory 数据库查询函数
sqlquery.inv <- partial(sqlquery, dbname = "inventory")

#' inventory 数据库执行函数
sqlexecute.inv <- partial(sqlexecute, dbname = "inventory")

#' 数据更新
#' @param data 更新数据源
#' @param tbl 数据表名
#' @param pk 数据主键
update <- \(data, tbl, pk="id") { # 数据更新
    nms <- names(data) # 提取各个数据列
    flds <- sapply(nms, \(i) sprintf("%s='%s'", i, data[, i, drop=T] |> gsub("'", "''",x=_))) |> 
      apply(1, \(line) paste(line[-match(pk, nms)], collapse=",\n  ")) 
    sprintf("update %s set\n  %s \nwhere id=%s", tbl, flds, data$id) |> print() |> sqlexecute.inv() # 执行并更
} # update

# 文件基准路径
home <- "F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/data" # 文件基准路径
files <- list.files(home, pattern = "\\.xlsx$") # 读取excel文件
regexec("(.+)\\.xlsx", files) |> regmatches(files, m=_) |> Reduce(rbind, x=_) |> (\(ps) { #文件解析 ps:parts
  tbls = sprintf("t_%s", ps[, 2]) # 表名集合
  seq(tbls) |> lapply(\(i){ # 遍历表名
    tbl <- tbls[i] # 数据表名
    file <- file.path(home, ps[i, 1]) # 绝对文件路径
    data <-  read_excel(file)[-1, ] |> transform(# 去除字段说明行（第一行）, 然后进行表数据结构变换,内容/类型转换等工作以便add_items函数处理 
      code=sprintf("%s%03d", toupper(abbreviate(ps[i, 2])), as.numeric(code)) # 生成公司编码
    ) # 表数据 
    if (!tblexists(tbl)) { # 数据表已经存在
      add_items(data, tbl) # 插入数据
    } else { # 数据表不存在
      print(sprintf("%s 业已存在", tbl))
    } # if
    sqlquery.inv(sprintf("select * from %s", tbl)) |> print() # 查询&并打印数据结果
  }) # lapply
})() # ps

# 修改products数据表的 code 字段
change_product_code <- \() {
  sqlquery.inv("alter table t_product modify column code varchar(100)") # 列字段长度扩增
  sqlquery.inv("select * from t_product") |> 
    mutate(code = sprintf(fmt="%s%03d", gsub("\\s", "_", tolower(ename)), 1)) |> # 将 code 设置为{ename}001的版本样式
    update("t_product") # 批量更新
  sqlquery.inv("select * from t_product")
}

# 修改code
change_product_code()
