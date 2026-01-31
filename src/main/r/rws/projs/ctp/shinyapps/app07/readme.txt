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

# 数据持久化 : 多主体私有保存
barclays |> with(entities() |> setNames(nm=_) |> lapply(\(ae, file=gettextf("%s%s.rds", ae, strftime(Sys.time(),"%Y%m%d%H%M%S"))) ldgsave(file, ae)))

# ----------------------------------------------------------------------------------------------------------------------------------------------------------

# 移除库文件
if(!is.na(match(".Bkp", search()))) detach(".Bkp")

# 库文件加载
attach(NULL, name=".Bkp") |> sys.source("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app07/bkp.R", envir=_)

strsplit("error,barclays,hsbc,uae,indian", "[[:blank:],]+") |> unlist() |> setNames(nm=_) |> lapply(\(ae) bkp(ae) |> #  创建匿名会计主体ae
  with(callCC(\(k) { # 会计主体ae不会绑定到具体变量，而是通过返回值传递到with，进而暴露展开其内部的私有函数与细节属性：ldgload, entities, balance 等。
    backups <- list.files(pattern=sprintf("^%s.*\\.rds$", entity())) # 加载备份文件
    if(length(backups)<1) { message(gettextf("没找到会计主体[%s]的RDS备份文件!", entity())); k(list()) } # 没有发现主体ae的数据备份文件则kill本次执行，返回空列表！
    list(backup=tail(backups, 1)) |> with(list(ldgload=ldgload(tail(backups, 1)) |> ls()) |> #  加载数据文件(尾部最新)并罗列会计主体
      append(list(entity=entity(), backup=backup, mtime=file.mtime(backup), entries=entries(), balance=balance()))) # 私有方法
  }))) # strsplit

# ----------------------------------------------------------------------------------------------------------------------------------------------------------

# 移除库文件
if(!is.na(match(".Bkp", search()))) detach(".Bkp")

# 库文件加载
attach(NULL, name=".Bkp") |> sys.source("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app07/bkp.R", envir=_)
path <- "F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app07/data/journal20260130.txt"
xs <- readLines(path) # 数据行
.ts <- grep("#[[:blank:]]*TX", xs) # 交易标记所在的行
ts <- if(max(.ts) < length(xs)) c(.ts, length(xs)) else .ts
# ts[-1]-(seq(m)<m) 表示除了ts的最后一个元素，其余元素都向前移动一位，这样我们就可以把区间[a,b,c]打造成开闭区间链,并且每个元素都互斥[[a,b), [b,c]]
txs <- (\(n=length(ts), m=n-1) cbind(ts[-n], ts[-1]-(seq(m)<m))) () |> apply(1, \(p) grep("^[[:blank:]]*$", xs[p[1]:p[2]], invert=T, value=T)) # 提取交易信息行

# 分组示例
# js:注释所在行索引,  ks:距离自己最近行的注释作为会计主体所在行位置, tx[-js]: 分录所在的数据行， tx[ks][-js] |> sub("#([^#:]+):", "\\1", x=_) 分录所属于的会计主体
txs |> setNames(nm=rep_len(xs[.ts], length(txs))) |> lapply(\(tx) list(js=grep("^[[:blank:]]*#", tx)) |> within( ks <- rep(js, c(diff(js), 1+length(tx)-max(js) )) ) |> 
  with({ data.frame(k=tx[ks][-js] |> sub("#([^#:]+):", "\\1", x=_), v=tx[-js])} ))

# 提取日记账
journal <- parse_journal(path)
print(journal)

# 创建会计主体容器环境
entities <- new.env()

# 自动执行 journal 到账簿系统
journal |> apply(1, \(row) { # 分录誊写
  tx_id <- row["tx_id"]
  entity_name <- trimws(row["entity"])
  direction <- tolower(trimws(row["direction"]))
  account <- trimws(row["account"])
  amount <- as.numeric(row["amount"])
 
  if (!exists(entity_name, envir = entities, inherits = FALSE)) { # 获取或创建会计主体（懒加载模式）
    assign(entity_name, bkp(entity_name), envir = entities)
    message(sprintf("创建会计主体: %s", entity_name))
  }

  get(entity_name, envir = entities) |> with({  # 根据借贷方向记账（自动携带交易号）
     if(direction == "dr") { # 借方
       debit(account, amount, tx = tx_id)
      } else if(direction == "cr") { # 贷方
       credit(account, amount, tx = tx_id)
     } else { # 其他
       warning(sprintf("未知方向 '%s' 在交易 %s", direction, tx_id))
     } # if
  }) # get
}) |> invisible() # journal

# 验证：查看所有主体的科目余额
cat("\n=== 各会计主体科目余额 ===\n")
ls(entities) |> setNames(nm=_) |> lapply(\(name) list(entity=name, balance=get(name, envir = entities)$balance()))

# 查看具体主体的分录（例如 uae）
get("uae", envir = entities) |> with({ entries() })
