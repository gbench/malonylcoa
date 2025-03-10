library(data.table)

# 数据表: id, quantity, drcr,  company_id, warehouse_id, create_time
data0 <- tribble (
  ~id,  ~name, ~quantity,  ~drcr,  ~company_id,  ~warehouse_id,  ~create_time,
  1,     'apple',  1,                1,        1,                     1,                        as.Date('2023-01-21'),
  2,     'orange',  2,              -1,       1,                     1,                        as.Date('2023-01-22'),
  3,     'peach', 3,                1,         1,                     1,                        as.Date('2023-01-23')
)
#
data1 <- tribble (
  ~id,  ~name, ~quantity,  ~drcr,  ~company_id,  ~warehouse_id,  ~create_time,
  1,     'apple', 1,                1,        1,                     1,                        as.POSIXct('2023-01-25 14:23:45'),
  2,     'orange', 2,              -1,       1,                     1,                        as.POSIXct('2023-01-26 14:23:45'),
  3,     'peach', 3,                1,         1,                    1,                        as.POSIXct('2023-01-27 14:23:45')
)
#
data2 <- tribble (
  ~id,  ~name, ~quantity,  ~drcr,  ~company_id,  ~warehouse_id,  ~create_time,
  1,     'apple', 1,                1,        1,                     1,                        as.POSIXct('2023-01-26 14:23:45'),
  2,     'orange', 2,              -1,       1,                     1,                        as.POSIXct('2023-01-27 14:23:45'),
  3,     'peach', 3,                1,         1,                    1,                        as.POSIXct('2023-01-28 14:23:45')
) 

#' create database inventory default character set utf8mb4
# inventory 数据库查询函数
sqlquery.inv <- partial(sqlquery, dbname="inventory")
  
#' inventory 数据库执行函数
sqlexecute.inv <- partial(sqlexecute, dbname="inventory")

#' 数据表是否存在
#' @param tbl 数据表
#' @return 存在的标志
tblexists <- \(...) {
  "SELECT COUNT(*) >0 flag FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '%s'" |>
  sprintf(c(...)) |> sqlquery.inv() |> Reduce(f=dplyr::bind_rows , x=) |> (\(.)structure(.$flag, names=c(...)))()
}

#'  批量添加数据
#' @param ... 数据表
batch_appends <- \(...) list(...) |> Reduce(f=dplyr::bind_rows , x=) |> 
  transform(tbl=sprintf("t_%s_%s", name, strftime(create_time, format="%Y%m%d"))) |> (\(data) {
    xs <- split(data, data$tbl) # 分片数据
    nms <- names(xs)  # 数据名称
    flags <- tblexists(nms) # 数据表是否存在的标志
    lapply(seq(xs), \(i) { # 遍历数据表
      x <- xs[[i]] |> (\(.) .[, -match(c("id", "tbl"), names(.))]) () #  提取分表数据，去除掉id与表名
      if(!flags[i]) ctsql(x, nms[i]) |> sub("\\(\n", "(\n  id int primary key auto_increment,\n", x=_) |> print() |> sqlexecute.inv() # 创建数据表
      insql(x, nms[i]) |> sqlexecute.inv() # 插入表数据
    })
  }) ()

# 插入表数据
batch_appends(data0, data1, data2)

# 读取数据
sqlquery.inv("show tables") |> unlist() |> sprintf(fmt="select * from %s")|> sqlquery.inv() |> (\(.){
  xs <- names(.) 
  matches <- regexec(pattern=".*\\s+(t_([^_]+)_([^_]+))$", text=xs) |> regmatches(xs, m=_) |> do.call(rbind, args=_) 
  lapply(seq(nrow(matches)), \(i) transform(.[[i]], tbl=matches[i, 2], name=matches[i, 3], date=matches[i, 4])) |> Reduce(f=rbind)
}) () 
