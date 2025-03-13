# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-数据导入数据库程序
#
# author:gbench@sina.com
# date: 2025-03-13
# ------------------------------------------------------------------------------------

# 安装并加载 readxl 包
if (!require(readxl)) {
  install.packages("readxl")
  library(readxl)
}

# 数据导入
batch_load()

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
#' @param items 数据项目
#' @param tbl 数据表
add_items <- \(items, tbl) {
  # 数据库插入
  if (!tblexists(tbl)) ctsql(items, tbl) |> add_pk() |> print() |> sqlexecute.inv() #  创建数据表
  insql(items, tbl) |> print() |>  sqlexecute.inv() # 数据插入
}

#' inventory 数据库查询函数
sqlquery.inv <- partial(sqlquery, dbname = "inventory")

#' inventory 数据库执行函数
sqlexecute.inv <- partial(sqlexecute, dbname = "inventory")

# 文件基准路径
home <- "F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/data" # 文件基准路径
files <- list.files(home, pattern = "\\.xlsx$") # 读取excel 文件
regexec("(.+)\\.xlsx",files) |> regmatches(files, m=_) |> Reduce(rbind, x=_) |> (\(p) {
  tbls = sprintf("t_%s", p[, 2])
  seq(tbls) |> lapply(\(i){
    file <- file.path(home, p[i, 1]) # 提取文件名
    data <-  read_excel(file)[-1,] %>% mutate(code=sprintf("%03d", as.numeric(code)))
    tbl <- tbls[i]
    if (!tblexists(tbl)) {
      add_items(data, tbl)
    } else {
      print(sprintf("%s 业已存在", tbl))
      sqlquery.inv(sprintf("select * from %s", tbl)) |> print()
    } # if
  }) # lapply
})()