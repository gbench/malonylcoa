# bookkeeping 金融记账系统
# 金融（核算价值利益的组织安排）的本质 就是 跨越会计实体的 对经济交易 给予 记录&核算（实现货币的计量、流通、支付职能的信息管理系统）：多主体会计
bkp <- local({
  cache <- new.env(hash=TRUE)
  .empty <- \() data.frame(ts=numeric(0), drcr=integer(0), name=character(0), amount=numeric(0), tx=character(0), stringsAsFactors=FALSE)
  .get <- \(x) if (exists(x, envir=cache, inherits=FALSE))  get(x, envir = cache) else .empty()
  .assign <- \(x, value) assign(x, value, envir=cache)
  .append <- \(ae, drcr, name, amount, tx=NA) .assign(ae, rbind(.get(ae), 
    data.frame(ts=Sys.time(), drcr=as.integer(drcr), name=as.character(name), amount=as.numeric(amount), tx=as.character(tx), stringsAsFactors=FALSE)))
  
  #' @param acctentity 会计主体
  \(acctentity) list(entity=\(ae=NA) ifelse(is.na(ae), acctentity, ae)) |> within({
    debit <- \(name, amount, ae=NA, tx=NA) .append (entity(), 1L, name, amount, tx) # 借入
    credit <- \(name, amount, ae=NA, tx=NA) .append (entity(), -1L, name, amount, tx) # 贷出
  }) |> within({
    # 1. 复式记账
    dc <- \(dr, cr, amount, ae=NA, tx=NA) { debit(dr, amount, ae, tx); credit(cr, amount, ae, tx) } # 借贷分录
    # 2. 会计分录
    entries <- \(ae=NA) .get(entity()) # 科目分录
    # 3. 科目余额
    balance <- \(name=NA, ae=NA) { # 科目余额
      .entries <- .get(entity()) # 主体分录
      if (nrow(.entries)<1) 0 # 没有数据
      else { # 数据非空
         bal <- \(es) with(es, sum(drcr*amount)) # 余额计算函数
         if(is.na(name)) .entries |> split(.entries$name) |> lapply(bal) |> (\(.) data.frame(balance=unlist(.))) ()
         else .entries[.entries$name==name, ] |> bal()
      } # if
    } # balance
  }) |> within({
    # 4. 会计报表报表生成
    financial_statement <- \(ae, type = "bs") {
      # 资产负债表或利润表
    }
    # 5. 数据持久化
    ldgsave <- \(file=gettextf("%s%s.rds", entity(), strftime(Sys.time(), "%Y%m%d%H%M%S")), pattern=".") 
      saveRDS(mget(grep(pattern, ls(cache), value=T), envir=cache) |> list2env(parent=emptyenv()), file)
    # 6. 数据读取
    ldgload <- \(file) cache <<- readRDS(file)
  }) # within
}) # bkp

# 构建簿记系统
newtx <- \(text) { 
  tx <- gettextf("%s[%s]", strftime(Sys.time(), "%Y%m%d%H%M%S"), text)
  \(ae) ae |> within({ tdc <- \(dr, cr, amount) dc(dr, cr, amount, NA, tx) })
}
