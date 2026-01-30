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

#' 构建簿记系统(new transaction )
#' @param text 交易描述信息的文本字符串
#' @return 为ae会计主体增加复式记账逻辑函数tdc
newtx <- \(text) { 
  tx <- gettextf("%s[%s]", strftime(Sys.time(), "%Y%m%d%H%M%S"), text)
  \(ae) ae |> within({ tdc <- \(dr, cr, amount) dc(dr, cr, amount, NA, tx) })
}

#'  解释日记账
#' 
#' 日记账格式示例：
#' # TX0 : 阿联酋卖油给印度（赊销）
#' 
#' # uae:
#' dr 应收账款-印度 1000
#' cr 主营业务收入-石油 1000
#' ...
#' @param path 日记账文件的路径或是记录日记账的文本行向量
#' @return journal 结构的数据框,列向量为 : data.frame(tx_id, entity, direction, account, amount)
parse_journal <- \(path) {
  xs <- if(file.exists(path)) readLines(path) else path # 如果文件存在则尝试读取文件，否则把path当成字符串向量！
  .ts <- grep("#[[:blank:]]*TX", xs) # 交易标记所在的行
  ts <- if(max(.ts) < length(xs)) c(.ts, length(xs)) else .ts
  txs <- cbind(ts[-length(ts)], ts[-1]) |> apply(1, \(p) grep("^[[:blank:]]*$", xs[p[1]:p[2]-1], invert=T, value=T)) # 提取交易信息行
  tx_parser <- \(tx) { # 交易解释器
    tx_id <- sub(".*TX\\s*([A-Za-z0-9]+).*", "\\1", tx[grepl("TX", tx)][1]) # 提取交易ID (如 "TX0")
    js <- grep("^#[[:blank:]]+[a-z]+", tx) # 会计主体所在行索引 (# uae: 等)
    if(length(js) == 0) return(NULL)

    entry_lines <- grepl("^[[:blank:]]*(dr|cr)[[:blank:]]+", tx, ignore.case=TRUE)  # 分录所在行索引 (dr/cr 开头)
    groups <- cut(which(entry_lines), c(js, Inf), labels=FALSE, right=FALSE) # 建立映射：每个分录行属于哪个会计主体
    if(any(is.na(groups))) groups[is.na(groups)] <- length(js)

    entities <- gsub("^#[[:blank:]]*|:$", "", tx[js])[groups] # 提取会计主体名称
    entries <- tx[entry_lines]  # 解析分录行
    matches <- regmatches(entries, regexec("^(dr|cr)[[:blank:]]+(.+?)[[:blank:]]+(\\d+(?:\\.\\d+)?)$", entries, ignore.case = TRUE)) 
    data.frame( tx_id=tx_id, entity=entities, direction=sapply(matches, `[`, 2), account=sapply(matches, `[`, 3),  # 构建数据框
      amount=as.numeric(sapply(matches, `[`, 4)), stringsAsFactors=FALSE)
  }

  txs |> lapply(tx_parser) |> do.call(what=rbind)
}

