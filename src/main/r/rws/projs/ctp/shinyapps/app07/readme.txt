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