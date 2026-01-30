# 库文件加载
attach(NULL, name=".Bkp") |> sys.source("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app07/bkp.R", envir=_)

# 会计主体
uae <- bkp("uae") # 阿联酋石油公司
barclays <- bkp("barclays") # 巴克莱银行
indian <- bkp("indian") # 印度石油公司
hsbc <- bkp("hsbc") # 汇丰银行

# 记录交易
tx0 <- newtx("阿联酋卖油给印度（赊销）")
tx0(uae) |> with({ tdc("应收账款-印度", "主营业务收入-石油", 1000) })
tx0(indian) |> with({ tdc("库存商品-石油", "应付账款-UAE", 1000) })

tx1 <- newtx("巴克莱创造货币信用（短期贷款[3M]，核心！）")
tx1(barclays) |> with({ tdc("短期贷款[3M]-印度", "客户存款-印度", 1000) })
tx1(indian) |> with({ tdc("银行存款-巴克莱", "短期借款[3M]-巴克莱", 1000) })

tx2 <- newtx("印度直接转账给UAE（行间划转，无现金）")
tx2(indian) |> with({ tdc("应付账款-UAE", "银行存款-巴克莱", 1000) })
tx2(uae) |> with({ tdc("银行存款-巴克莱", "应收账款-印度", 1000) })
tx2(barclays) |> with({ tdc("库存现金", "客户存款-UAE", 1000) })

tx3 <- newtx("阿联酋转至汇丰（银行间头寸转移，涉及代理行账户）")
tx3(uae) |> with({ tdc("银行存款-汇丰", "银行存款-巴克莱", 1000) })
tx3(hsbc) |> with({ tdc("存放同业-巴克莱", "客户存款-UAE", 1000) }) # 对巴克莱的债权
tx3(barclays) |> with({ tdc("客户存款-UAE", "同业存放-汇丰", 1000) }) #  欠汇丰的债务(把债主从UAE换成汇丰）

# 会计分录列表
environment(bkp) |> with(ls(cache) |> setNames(nm=_) |> lapply(\(x) get(x, envir=cache)))

# 会计分录汇总
environment(bkp) |> with(ls(cache) |> setNames(nm=_) |> lapply(\(x) get(x, envir=cache)) |> 
  lapply(\(xs) split(xs, xs$name) |> lapply(\(es) with(es, sum(drcr*amount)))) |> 
    lapply(\(bs) data.frame(balance=unlist(bs))) )

# 科目余额
barclays |> with(balance())

# 数据持久化 : 保存所有数据
barclays |> with(ldgsave("allinone.rds"))

# ---------------------------------------

file <- "F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app07/data/journal20260130.txt"
xs <- readLines(file) # 数据行
.ts <- grep("#[[:blank:]]*TX", xs) # 交易标记所在的行
ts <- if(max(.ts) < length(xs)) c(.ts, length(xs)) else .ts
txs <- cbind(ts[-length(ts)], ts[-1]) |> apply(1, \(p) grep("^[[:blank:]]*$", xs[p[1]:p[2]-1], invert=T, value=T)) # 提取交易信息行
txs |> lapply(\(tx) {
   ii <- grep("#", tx) # 注释所在行索引
   ks <- rep(ii, c(diff(ii), 1+length(tx)-max(ii))) # 距离自己最近行的注释作为会计主体所在行位置
   data.frame(k=tx[ks][-ii], v=tx[-ii])
})

