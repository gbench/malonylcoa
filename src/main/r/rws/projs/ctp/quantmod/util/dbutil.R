# 获得期货合约
get_instruments <- \(n=100) {
  try({
    dfm <- env_adhoc$sqlquery("show tables") # 查询数据表
    i <- dfm |> unlist() |> grep(pat="^t_[[:alnum:]]+_\\d{8}$") # 提取数据表索引
    if(length(i)>0) {
      datatbls <- dfm[i, 1, drop=T] # 数据表名称
      instruments <- gsub(pattern="(^t_)|(_\\d{8}$)", "", x=datatbls) # 整理合约名称
      instruments |> unique() |> tail(n)
      structure(instruments, names=paste0("合约", instruments))
    }else {
      c("none"="-") # 空数据表
    }
  })
}

# 获得期货合约
get_tbls <- \(instrument, n=100) {
  try({
    dfm <- env_adhoc$sqlquery("show tables") # 查询数据表
    pat=sprintf(fmt="^t_%s+_\\d{8}$", instrument)
    i <- dfm |> unlist() |> grep(pat=pat) # 提取数据表索引
    if(length(i)>0) {
      datatbls <- dfm[i, 1, drop=T] # 数据表名称
      names(datatbls) <- paste0("合约", gsub(pattern="(^t_)", "", x=datatbls))# 整理合约名称
      datatbls |> tail(n)
    }else {
      c("none"="-") # 空数据表
    }
  })
}

# 获得期货合约
get_dbs <- \() {
  try({
    dfm <- env_adhoc$sqlquery("show databases") # 查询数据表
    dbs <- dfm[, 1, drop=T] # 数据表名称
    dbs
  })
}