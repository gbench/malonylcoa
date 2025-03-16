#' create database inventory default character set utf8mb4
# inventory 数据库查询函数
sqlquery.inv <- partial(sqlquery, dbname = "inventory")

#' inventory 数据库执行函数
sqlexecute.inv <- partial(sqlexecute, dbname = "inventory")

#' 数据表是否存在
#' @param tbl 数据表
#' @return 存在的标志
tblexists <- \(...) {
  "SELECT COUNT(*) > 0 flag FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '%s'" |>
    sprintf(c(...)) |>
    sqlquery.inv() |>
    Reduce(f = dplyr::bind_rows, x = ) |>
    (\(.) structure(if (!is.na(match("flag", names(.)))) .$flag else ., names = c(...)))()
}

# 判断中间表
if (!tblexists("table_metadata")) {
  table_metadata <- tribble(
    ~product_id, ~company_id, ~warehouse_id, ~date_range_start, ~date_range_end, ~total_in, ~total_out, ~create_time,
    "PRODUCT001", 0, 0, Sys.time(), Sys.time(), 0, 0, Sys.time()
  )
  ctsql(table_metadata) |>
    sub(pattern = "\\(\n", replacement = "(\n  id int primary key auto_increment,\n", x = _) |> # 增加数据库主键
    sqlexecute.inv()
}

sqlquery.inv("show tables") |>
  unlist() |> (\(nms) {
    sqls <- sprintf(fmt = "select * from %s", nms) 
    nms
  })()
  

# 读取数据
sqlquery.inv("show tables") |>
  unlist() |>
  sprintf(fmt = "select * from %s") |>
  sqlquery.inv() |>
  (\(.){
    nms <- names(.)
    matches <- regexec(pattern = ".*\\s+(t_([^_]+)_([^_]+))$", text = nms) |>
      regmatches(nms, m = _) |>
      do.call(rbind, args = _)
    lapply(seq(nrow(matches)), \(i) transform(.[[i]], tbl = matches[i, 2], name = matches[i, 3], date = matches[i, 4])) |>
      Reduce(f = rbind) |>
      (\(.) { # 数据统计
        data <- transform(., # 出入库数量数据整理
          total_in = ifelse(drcr == 1, quantity, 0), # 期间入库数量
          total_out = ifelse(drcr == -1, quantity, 0) # 期间出库数量
        ) # data
        formula <- cbind(total_in, total_out) ~ product_id + company_id + warehouse_id # 统计公式
        data <- aggregate(formula, data, sum) # 数据统计与透视
        data$date_range_start <- Sys.time() + c(-1, 0, 0) %*% c(3600, 60, 1) |> rep(nrow(data)) # 批次开始时间
        data$date_range_end <- Sys.time() # 批次结束时间
        data$create_time <- Sys.time() # 数据写入时间
        insql(data, "table_metadata") |>
          print() |>
          sqlexecute.inv() # 插入分批汇总
        data
      })()
  })()

# 分批汇总
sqlquery.inv("select * from table_metadata")
