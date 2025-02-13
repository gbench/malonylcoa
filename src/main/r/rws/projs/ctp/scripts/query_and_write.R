library(data.table)
library(tidyverse)

#' 创建目录
#' @param x 目录集合
mkdir <- \(x) x |> strsplit("[,/\\]+") |> structure(names=x) |> lapply( \(e) Reduce(x=e, \(acc, a) paste(acc, a, sep="/"), accumulate=T) |> lapply(dir.create) )

# ------------------------------------------------------
# 查询数据并写入文件
# ------------------------------------------------------

data.home <- "data/h10ctp2/"

#' 创建数据文件根目录
mkdir(data.home)

#'  读取二月份rb2505的数据
sqlquery.h10ctp2("show tables") |> unlist() |> # 从h10ctp2中读取数据
  grep(pat="rb2505_202502\\d{2}", value=T) |> # 读取指定名称模式的数据文件
  sprintf(fmt="select * from %s") |> 
  sqlquery.h10ctp2() %>%
  structure(., names=gsub(pat=".*\\st_(\\w+)$", "\\1", x=names(.))) |> ( \( xs, ns=names(xs) )  # 改名并存储
    lapply(ns, \(n) fwrite(getElement(xs, n), paste0(data.home, n, ".csv"))) )() # 存储到指定的data.home数据目录

# 查看data.home目录下的所有数据文件（不包括目录）
data.files <- list.files(path=data.home, all.files=T, recursive=T, include.dirs=F); data.files