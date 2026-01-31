# bookkeeping_v3.R - 修正作用域bug
library(data.table)

create_ledger_system <- function(base_currency = "CNY") {
  private <- new.env(parent = emptyenv())
  private$entities <- new.env(hash = TRUE, parent = emptyenv())
  private$tx_counter <- 0
  
  create_entity <- function(code, name, base = base_currency) {
    if (exists(code, envir = private$entities)) stop("会计主体已存在: ", code)
    
    # 每个主体是一个独立环境
    entity <- new.env(parent = emptyenv())
    entity$meta <- list(code = code, name = name, currency = base, created = Sys.time())
    entity$entries <- data.table(
      ts = as.POSIXct(character()), no = integer(), 
      dr_acct = character(), cr_acct = character(),
      amount = numeric(), memo = character(), tx_id = character()
    )
    entity$balances <- data.table(
      account = character(), debit = numeric(), credit = numeric(), net = numeric()
    )
    
    assign(code, entity, envir = private$entities)
    message("创建成功: ", code, " - ", name)
    invisible(entity)
  }
  
  # BUGFIX: 使用普通赋值 <- 而非 <<-, 因为 entity 是环境，具有引用语义
  post <- function(entity_code, dr_acct, cr_acct, amount, memo = "", tx_id = NULL) {
    if (amount <= 0) stop("金额必须为正数")
    if (!exists(entity_code, envir = private$entities)) stop("主体不存在: ", entity_code)
    
    entity <- get(entity_code, envir = private$entities)
    private$tx_counter <- private$tx_counter + 1
    
    if (is.null(tx_id)) {
      tx_id <- sprintf("TX%s%06d", format(Sys.time(), "%Y%m%d"), private$tx_counter)
    }
    
    # 新增分录
    new_entry <- data.table(
      ts = Sys.time(), no = nrow(entity$entries) + 1L,
      dr_acct = dr_acct, cr_acct = cr_acct, amount = amount,
      memo = memo, tx_id = tx_id
    )
    # BUGFIX: 使用 <- 即可，entity 是环境，修改其 binding 会持久化
    entity$entries <- rbind(entity$entries, new_entry, fill = TRUE)
    
    # 更新余额 - 直接使用 <- 修改环境内容
    update_bal <- function(acct, amt, type) {
      b <- entity$balances
      if (!acct %in% b$account) {
        entity$balances <- rbind(b, data.table(
          account = acct, debit = 0, credit = 0, net = 0
        ), fill = TRUE)
      }
      row_idx <- which(entity$balances$account == acct)
      if (type == "dr") {
        entity$balances$debit[row_idx] <- entity$balances$debit[row_idx] + amt
      } else {
        entity$balances$credit[row_idx] <- entity$balances$credit[row_idx] + amt
      }
      entity$balances$net[row_idx] <- entity$balances$debit[row_idx] - entity$balances$credit[row_idx]
    }
    
    update_bal(dr_acct, amount, "dr")
    update_bal(cr_acct, amount, "cr")
    
    invisible(list(tx_id = tx_id, entry = new_entry))
  }
  
  trial_balance <- function(entity_code = NULL) {
    if (!is.null(entity_code)) {
      if (!exists(entity_code, envir = private$entities)) return(NULL)
      entity <- get(entity_code, envir = private$entities)
      return(copy(entity$balances))
    }
    all_balances <- lapply(ls(private$entities), function(code) {
      e <- get(code, envir = private$entities)
      if (nrow(e$balances) > 0) cbind(entity = code, e$balances) else NULL
    })
    rbindlist(all_balances, use.names = TRUE)
  }
  
  get_entries <- function(entity_code) {
    if (!exists(entity_code, envir = private$entities)) return(NULL)
    copy(get(entity_code, envir = private$entities)$entries)
  }
  
  financial_statements <- function(entity_code) {
    if (!exists(entity_code, envir = private$entities)) stop("主体不存在: ", entity_code)
    
    entity <- get(entity_code, envir = private$entities)
    b <- copy(entity$balances)
    
    if (nrow(b) == 0) return(list(message = "该主体无交易记录"))
    
    classify <- function(acct) {
      if (grepl("资产|现金|存款|应收|存货|固定资产|银行|库存", acct)) return("asset")
      if (grepl("负债|应付|借款|债券|往来", acct)) return("liability")
      if (grepl("收入|收益|主营|其他业务", acct)) return("revenue")
      if (grepl("成本|费用|支出|折旧", acct)) return("expense")
      if (grepl("资本|股本|盈余|未分配|权益", acct)) return("equity")
      "other"
    }
    b[, type := sapply(account, classify)]
    
    bs <- list(
      assets = b[type == "asset", .(account, amount = net)],
      liabilities = b[type == "liability", .(account, amount = -net)],
      equity = b[type == "equity", .(account, amount = -net)]
    )
    bs$total_assets <- sum(bs$assets$amount, na.rm = TRUE)
    bs$total_liab_eq <- sum(bs$liabilities$amount, bs$equity$amount, na.rm = TRUE)
    bs$balanced <- abs(bs$total_assets - bs$total_liab_eq) < 0.01
    
    pl <- list(
      revenue = b[type == "revenue", .(account, amount = -net)],
      expenses = b[type == "expense", .(account, amount = net)]
    )
    pl$net_profit <- sum(pl$revenue$amount, na.rm = TRUE) - sum(pl$expenses$amount, na.rm = TRUE)
    
    list(entity = entity$meta, balance_sheet = bs, income_statement = pl, timestamp = Sys.time())
  }
  
  transfer <- function(from_entity, to_entity, amount, memo = "跨主体转账") {
    if (!exists(from_entity, envir = private$entities)) stop("转出主体不存在")
    if (!exists(to_entity, envir = private$entities)) stop("转入主体不存在")
    
    tx <- sprintf("TF%s", format(Sys.time(), "%Y%m%d%H%M%S"))
    post(from_entity, "内部往来-应付", "银行存款", amount, memo, tx)
    post(to_entity, "银行存款", "内部往来-应收", amount, memo, tx)
    invisible(tx)
  }
  
  save_ledger <- function(filepath = "ledger.rds") {
    save_list <- list(
      entities = as.list(private$entities),
      tx_counter = private$tx_counter,
      base_currency = base_currency
    )
    saveRDS(save_list, filepath)
    message("账簿已保存至: ", filepath)
  }
  
  list_entities <- function() ls(private$entities)
  
  structure(list(
    create_entity = create_entity,
    post = post,
    transfer = transfer,
    trial_balance = trial_balance,
    financial_statements = financial_statements,
    list_entities = list_entities,
    get_entries = get_entries,
    save = save_ledger
  ), class = "bookkeeping_system")
}

# ==================== 验证测试 ====================

ledger <- create_ledger_system("USD")

ledger$create_entity("UAE_OIL", "阿联酋石油公司")
ledger$create_entity("IND_REF", "印度炼油集团")

cat("\n1. 记录赊销交易:\n")
ledger$post("UAE_OIL", "应收账款-IND", "主营业务收入", 1000000, "销售原油")
ledger$post("IND_REF", "库存商品-原油", "应付账款-UAE", 1000000, "采购原油")

cat("\n2. UAE_OIL 分录:\n")
print(ledger$get_entries("UAE_OIL"))

cat("\n3. UAE_OIL 试算平衡:\n")
print(ledger$trial_balance("UAE_OIL"))

cat("\n4. 财务报表:\n")
fs <- ledger$financial_statements("UAE_OIL")
cat("资产:", fs$balance_sheet$total_assets, "\n")
cat("利润:", fs$income_statement$net_profit, "\n")
