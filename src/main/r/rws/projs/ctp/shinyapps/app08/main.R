# ------------------------------------------------------------------------------
# 订单数据记账系统
# 基于Hitler数据库t_order表和bkp簿记系统
# ------------------------------------------------------------------------------

# 加载bkp簿记系统（基于bkp.readme.R）
source_bkp_system <- function() {
  if (!exists(".Bkp_loaded") || !.Bkp_loaded) {
    bkp <- local({
      cache <- new.env(hash=TRUE)
      .empty <- \() data.frame(ts=numeric(0), drcr=integer(0), name=character(0), 
                               amount=numeric(0), tx=character(0), stringsAsFactors=FALSE)
      .get <- \(x, s=as.character(x)) if (exists(s, envir=cache, inherits=FALSE)) 
        get(s, envir = cache) else .empty()
      .assign <- \(x, value) assign(x, value, envir=cache)
      .append <- \(ae, drcr, name, amount, tx=NA) .assign(ae, rbind(.get(ae), 
        data.frame(ts=Sys.time(), drcr=as.integer(drcr), name=as.character(name), 
                   amount=as.numeric(amount), tx=as.character(tx), stringsAsFactors=FALSE)))
      
      \(acctentity, .ae=(\(s) tryCatch(acctentity, error=\(e) s)) (as.character(substitute(acctentity))))
        list(entity=\(ae=NA) ifelse(is.na(ae), .ae, ae), entities=\() ls(cache)) |> within({
          debit <- \(name, amount, ae=NA, tx=NA) .append (entity(ae), 1L, name, amount, tx)
          credit <- \(name, amount, ae=NA, tx=NA) .append (entity(ae), -1L, name, amount, tx)
        }) |> within({
          dc <- \(dr, cr, amount, ae=NA, tx=NA) { debit(dr, amount, ae, tx); credit(cr, amount, ae, tx) }
          entries <- \(ae=NA) .get(entity(ae))
          balance <- \(name=NA, ae=NA) {
            .entries <- .get(entity(ae))
            if (nrow(.entries)<1) 0
            else {
              bal <- \(es) with(es, sum(drcr*amount))
              if(is.na(name)) .entries |> split(.entries$name) |> lapply(bal) |> 
                (\(.) data.frame(balance=unlist(.))) ()
              else .entries[.entries$name==name, ] |> bal()
            }
          }
        }) |> within({
          financial_statement <- \(ae, type = "bs") {
            entries <- entries(ae)
            if (nrow(entries) == 0) return(list())
            
            # 计算各科目余额
            balances <- entries |> split(entries$name) |> 
              lapply(\(es) with(es, sum(drcr*amount))) |>
              (\(.) data.frame(account=names(.), balance=unlist(.))) ()
            
            # 分类科目
            asset_patterns <- c("现金|银行|应收|存货|固定资产|预付")
            liability_patterns <- c("应付|借款|预收|负债")
            equity_patterns <- c("实收资本|资本公积|留存收益|权益")
            revenue_patterns <- c("收入|主营业务|其他业务")
            expense_patterns <- c("费用|成本|销售|管理|财务")
            
            classify <- \(pattern) balances[grepl(pattern, balances$account), ]
            
            list(
              assets = classify(asset_patterns),
              liabilities = classify(liability_patterns),
              equity = classify(equity_patterns),
              revenue = classify(revenue_patterns),
              expenses = classify(expense_patterns)
            )
          }
          
          ldgsave <- \(file=gettextf("%s%s.rds", entity(), strftime(Sys.time(), "%Y%m%d%H%M%S")), pattern=".") 
            saveRDS(mget(grep(pattern, ls(cache), value=T), envir=cache) |> list2env(parent=emptyenv()), file)
          
          ldgload <- \(file) cache <<- readRDS(file)
        })
    })
    
    # 全局变量
    assign("bkp", bkp, envir = .GlobalEnv)
    assign(".Bkp_loaded", TRUE, envir = .GlobalEnv)
    
    cat("BKP簿记系统加载成功!\n")
  }
}

# 加载bkp系统
source_bkp_system()

#' 解析订单详情JSON
#' @param details JSON字符串
parse_order_details <- function(details) {
  tryCatch({
    parsed <- fromJSON(details)
    
    # 处理items字段
    if (!is.null(parsed$items) && length(parsed$items) > 0) {
      if (is.list(parsed$items) && !is.data.frame(parsed$items)) {
        # 转换为数据框
        items_df <- do.call(rbind, lapply(parsed$items, as.data.frame))
        parsed$items <- items_df
      }
    }
    
    parsed
  }, error = function(e) {
    list(
      flag = FALSE,
      name = "解析失败",
      items = data.frame(id = NA, name = "解析失败", quantity = 0, price = 0)
    )
  })
}

#' 为订单创建会计主体
#' @param order_row 订单数据行
#' @param entities_env 会计主体环境
create_entities_for_order <- function(order_row, entities_env) {
  # 发货方会计主体
  shipper_name <- as.character(order_row$shipper)
  if (!exists(shipper_name, envir = entities_env)) {
    assign(shipper_name, bkp(shipper_name), envir = entities_env)
    cat(sprintf("创建发货方会计主体: %s\n", shipper_name))
  }
  
  # 收货方会计主体
  receiver_name <- as.character(order_row$receiver)
  if (!exists(receiver_name, envir = entities_env)) {
    assign(receiver_name, bkp(receiver_name), envir = entities_env)
    cat(sprintf("创建收货方会计主体: %s\n", receiver_name))
  }
  
  list(
    shipper = get(shipper_name, envir = entities_env),
    receiver = get(receiver_name, envir = entities_env)
  )
}

#' 根据订单生成会计分录
#' @param order_row 订单数据行
#' @param entities 会计主体列表
#' @param tx_id 交易ID
generate_journal_entries <- function(order_row, entities, tx_id) {
  amount <- as.numeric(order_row$amount)
  order_name <- as.character(order_row$name)
  order_id <- as.integer(order_row$id)
  
  # 解析订单详情
  details <- parse_order_details(as.character(order_row$details))
  
  # 生成交易描述
  tx_desc <- sprintf("订单-%d: %s (发货方: %s, 收货方: %s)", 
                     order_id, order_name, 
                     as.character(order_row$shipper),
                     as.character(order_row$receiver))
  
  # 创建交易函数
  tx <- \(ae) ae |> within({ 
    tdc <- \(dr, cr, amount) dc(dr, cr, amount, NA, tx_desc)
  })
  
  # 发货方会计分录 (销售商品)
  # 借: 应收账款-{收货方}
  # 贷: 主营业务收入-{商品}
  shipper_ae <- tx(entities$shipper)
  shipper_ae$tdc(
    sprintf("应收账款-%s", as.character(order_row$receiver)),
    sprintf("主营业务收入-%s", order_name),
    amount
  )
  
  # 如果有销售成本信息
  if (!is.null(details$items) && is.data.frame(details$items)) {
    total_cost <- sum(details$items$quantity * details$items$price * 0.7, na.rm = TRUE) # 假设成本是售价的70%
    if (total_cost > 0) {
      shipper_ae$tdc(
        sprintf("主营业务成本-%s", order_name),
        sprintf("存货-%s", order_name),
        total_cost
      )
    }
  }
  
  # 收货方会计分录 (采购商品)
  # 借: 存货-{商品}
  # 贷: 应付账款-{发货方}
  receiver_ae <- tx(entities$receiver)
  receiver_ae$tdc(
    sprintf("存货-%s", order_name),
    sprintf("应付账款-%s", as.character(order_row$shipper)),
    amount
  )
  
  list(
    shipper_entries = entities$shipper$entries(),
    receiver_entries = entities$receiver$entries(),
    tx_desc = tx_desc
  )
}

#' 处理所有订单数据
#' @param orders_df 订单数据框
process_all_orders <- function(orders_df) {
  # 创建会计主体环境
  entities_env <- new.env()
  
  # 按订单ID排序
  orders_df <- orders_df[order(orders_df$id), ]
  
  results <- list()
  
  for (i in 1:nrow(orders_df)) {
    order_row <- orders_df[i, ]
    order_id <- as.integer(order_row$id)
    
    cat(sprintf("处理订单 %d/%d: ID=%d, 金额=%.2f\n", 
                i, nrow(orders_df), order_id, order_row$amount))
    
    tryCatch({
      # 创建会计主体
      entities <- create_entities_for_order(order_row, entities_env)
      
      # 生成会计分录
      journal_entries <- generate_journal_entries(order_row, entities, order_id)
      
      results[[as.character(order_id)]] <- list(
        order_id = order_id,
        shipper = as.character(order_row$shipper),
        receiver = as.character(order_row$receiver),
        amount = order_row$amount,
        tx_desc = journal_entries$tx_desc,
        success = TRUE
      )
      
    }, error = function(e) {
      results[[as.character(order_id)]] <- list(
        order_id = order_id,
        shipper = as.character(order_row$shipper),
        receiver = as.character(order_row$receiver),
        amount = order_row$amount,
        error = e$message,
        success = FALSE
      )
      cat(sprintf("  订单 %d 处理失败: %s\n", order_id, e$message))
    })
  }
  
  # 获取所有会计主体
  entity_names <- ls(entities_env)
  
  list(
    entities_env = entities_env,
    entity_names = entity_names,
    results = results,
    total_orders = nrow(orders_df),
    successful_orders = sum(sapply(results, \(x) x$success))
  )
}

#' 生成财务报表
#' @param entities_env 会计主体环境
#' @param entity_name 会计主体名称
generate_financial_statements <- function(entities_env, entity_name) {
  if (!exists(entity_name, envir = entities_env)) {
    stop(sprintf("会计主体 %s 不存在", entity_name))
  }
  
  entity <- get(entity_name, envir = entities_env)
  
  # 获取所有分录
  entries <- entity$entries()
  
  if (nrow(entries) == 0) {
    return(list(
      entity = entity_name,
      balance_sheet = NULL,
      income_statement = NULL,
      message = "无交易记录"
    ))
  }
  
  # 计算科目余额
  account_balances <- entries |> 
    group_by(name) |> 
    summarise(balance = sum(drcr * amount)) |>
    arrange(desc(abs(balance)))
  
  # 分类科目
  classify_account <- function(account_name) {
    account_lower <- tolower(account_name)
    
    if (grepl("现金|银行|存款|应收|预付|存货|固定|资产", account_lower)) {
      return("资产")
    } else if (grepl("应付|预收|借款|负债|贷款", account_lower)) {
      return("负债")
    } else if (grepl("实收|资本|公积|留存|权益|所有者", account_lower)) {
      return("所有者权益")
    } else if (grepl("收入|收益|主营|其他业务", account_lower)) {
      return("收入")
    } else if (grepl("成本|费用|销售|管理|财务|税金", account_lower)) {
      return("费用")
    } else {
      return("其他")
    }
  }
  
  account_balances$category <- sapply(account_balances$name, classify_account)
  
  # 生成资产负债表
  assets <- account_balances[account_balances$category == "资产", ]
  liabilities <- account_balances[account_balances$category == "负债", ]
  equity <- account_balances[account_balances$category == "所有者权益", ]
  
  total_assets <- sum(assets$balance, na.rm = TRUE)
  total_liabilities <- sum(liabilities$balance, na.rm = TRUE)
  total_equity <- sum(equity$balance, na.rm = TRUE)
  
  # 生成利润表
  revenues <- account_balances[account_balances$category == "收入", ]
  expenses <- account_balances[account_balances$category == "费用", ]
  
  total_revenue <- sum(revenues$balance, na.rm = TRUE)
  total_expense <- sum(expenses$balance, na.rm = TRUE)
  net_income <- total_revenue - total_expense
  
  list(
    entity = entity_name,
    balance_sheet = list(
      assets = assets,
      liabilities = liabilities,
      equity = equity,
      total_assets = total_assets,
      total_liabilities = total_liabilities,
      total_equity = total_equity,
      balance_check = abs(total_assets - (total_liabilities + total_equity)) < 0.01
    ),
    income_statement = list(
      revenues = revenues,
      expenses = expenses,
      total_revenue = total_revenue,
      total_expense = total_expense,
      net_income = net_income
    ),
    all_accounts = account_balances,
    entries = entries
  )
}

#' 导出会计分录到CSV
#' @param entities_env 会计主体环境
#' @param entity_name 会计主体名称
#' @param output_dir 输出目录
export_journal_to_csv <- function(entities_env, entity_name, output_dir = "./accounting_reports") {
  if (!dir.exists(output_dir)) {
    dir.create(output_dir, recursive = TRUE)
  }
  
  entity <- get(entity_name, envir = entities_env)
  entries <- entity$entries()
  
  if (nrow(entries) > 0) {
    # 格式化时间戳
    entries$ts_formatted <- as.POSIXct(entries$ts, origin = "1970-01-01")
    
    # 添加借贷方向文字
    entries$direction <- ifelse(entries$drcr == 1, "借", "贷")
    
    # 重新排列列
    entries <- entries[, c("ts_formatted", "direction", "name", "amount", "tx")]
    
    # 写入CSV
    filename <- sprintf("%s/%s_会计分录_%s.csv", 
                       output_dir, 
                       entity_name,
                       format(Sys.time(), "%Y%m%d_%H%M%S"))
    
    write.csv(entries, filename, row.names = FALSE, fileEncoding = "UTF-8")
    cat(sprintf("会计分录已导出到: %s\n", filename))
    
    return(filename)
  }
  
  NULL
}

#' 生成所有公司的财务报表
#' @param entities_env 会计主体环境
generate_all_financial_reports <- function(entities_env) {
  entity_names <- ls(entities_env)
  
  reports <- list()
  
  for (entity_name in entity_names) {
    cat(sprintf("生成财务报表: %s\n", entity_name))
    
    report <- generate_financial_statements(entities_env, entity_name)
    reports[[entity_name]] <- report
    
    # 导出会计分录
    csv_file <- export_journal_to_csv(entities_env, entity_name)
    
    if (!is.null(csv_file)) {
      reports[[entity_name]]$csv_file <- csv_file
    }
  }
  
  reports
}

#' 主函数：从Hitler数据库处理订单并生成财务报表
main_accounting_system <- function() {
  cat("=== 订单记账系统启动 ===\n")
  
  # 1. 加载数据库连接
  batch_load()
  sqlquery.hitler <- partial(sqlquery, host="192.168.1.6", port=3309, dbname="hitler")
  
  # 2. 读取订单数据
  cat("读取订单数据...\n")
  orders_df <- sqlquery.hitler("SELECT * FROM t_order ORDER BY id")
  
  if (nrow(orders_df) == 0) {
    cat("未找到订单数据\n")
    return(NULL)
  }
  
  cat(sprintf("找到 %d 条订单记录\n", nrow(orders_df)))
  
  # 3. 处理订单并记账
  cat("处理订单并生成会计分录...\n")
  processing_result <- process_all_orders(orders_df)
  
  cat(sprintf("订单处理完成: %d/%d 成功\n", 
              processing_result$successful_orders, 
              processing_result$total_orders))
  
  # 4. 生成财务报表
  cat("生成财务报表...\n")
  financial_reports <- generate_all_financial_reports(processing_result$entities_env)
  
  # 5. 输出摘要
  cat("\n=== 财务报表摘要 ===\n")
  for (entity_name in names(financial_reports)) {
    report <- financial_reports[[entity_name]]
    
    if (!is.null(report$balance_sheet)) {
      bs <- report$balance_sheet
      is <- report$income_statement
      
      cat(sprintf("\n公司: %s\n", entity_name))
      cat(sprintf("  资产总计: %.2f\n", bs$total_assets))
      cat(sprintf("  负债总计: %.2f\n", bs$total_liabilities))
      cat(sprintf("  所有者权益: %.2f\n", bs$total_equity))
      cat(sprintf("  收入总计: %.2f\n", is$total_revenue))
      cat(sprintf("  费用总计: %.2f\n", is$total_expense))
      cat(sprintf("  净利润: %.2f\n", is$net_income))
      cat(sprintf("  资产负债表平衡: %s\n", 
                  ifelse(bs$balance_check, "是", "否")))
    }
  }
  
  # 6. 保存所有数据
  cat("\n保存会计数据...\n")
  save_dir <- "./accounting_data"
  if (!dir.exists(save_dir)) {
    dir.create(save_dir, recursive = TRUE)
  }
  
  save_file <- sprintf("%s/accounting_data_%s.rds", 
                       save_dir, 
                       format(Sys.time(), "%Y%m%d_%H%M%S"))
  
  saveRDS(list(
    orders = orders_df,
    processing_result = processing_result,
    financial_reports = financial_reports,
    timestamp = Sys.time()
  ), save_file)
  
  cat(sprintf("数据已保存到: %s\n", save_file))
  
  # 7. 生成HTML报告
  generate_html_accounting_report(financial_reports, orders_df)
  
  return(list(
    orders = orders_df,
    processing_result = processing_result,
    financial_reports = financial_reports
  ))
}

#' 生成HTML格式的会计报告
generate_html_accounting_report <- function(financial_reports, orders_df) {
  output_dir <- "./accounting_reports"
  if (!dir.exists(output_dir)) {
    dir.create(output_dir, recursive = TRUE)
  }
  
  timestamp <- format(Sys.time(), "%Y%m%d_%H%M%S")
  
  # 创建HTML内容
  html_content <- sprintf('
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>会计报告 - %s</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; margin-bottom: 30px; }
        .section { margin-bottom: 30px; border: 1px solid #ddd; padding: 15px; }
        .section-title { background-color: #f5f5f5; padding: 10px; font-weight: bold; }
        table { width: 100%%; border-collapse: collapse; margin-top: 10px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .total-row { font-weight: bold; background-color: #e6f7ff; }
        .positive { color: green; }
        .negative { color: red; }
        .company-card { border: 1px solid #ccc; margin: 10px; padding: 15px; }
        .flex-container { display: flex; flex-wrap: wrap; }
    </style>
</head>
<body>
    <div class="header">
        <h1>会计报告</h1>
        <p>生成时间: %s</p>
        <p>订单总数: %d</p>
    </div>
    
    <div class="section">
        <div class="section-title">订单概览</div>
        <table>
            <tr>
                <th>ID</th>
                <th>商品</th>
                <th>发货方</th>
                <th>收货方</th>
                <th>金额</th>
                <th>创建时间</th>
            </tr>
            %s
        </table>
    </div>
    
    <div class="section">
        <div class="section-title">各公司财务报表</div>
        <div class="flex-container">
            %s
        </div>
    </div>
</body>
</html>',
    format(Sys.time(), "%Y-%m-%d"),
    format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
    nrow(orders_df),
    paste(apply(orders_df, 1, function(row) {
      sprintf('<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%.2f</td><td>%s</td></tr>',
              row['id'], row['name'], row['shipper'], row['receiver'], 
              as.numeric(row['amount']), row['create_time'])
    }), collapse = "\n"),
    paste(lapply(names(financial_reports), function(entity_name) {
      report <- financial_reports[[entity_name]]
      
      if (is.null(report$balance_sheet)) {
        return(sprintf('
            <div class="company-card">
                <h3>%s</h3>
                <p>无交易记录</p>
            </div>', entity_name))
      }
      
      bs <- report$balance_sheet
      is <- report$income_statement
      
      sprintf('
            <div class="company-card">
                <h3>%s</h3>
                <p><strong>资产总计:</strong> %.2f</p>
                <p><strong>负债总计:</strong> %.2f</p>
                <p><strong>所有者权益:</strong> %.2f</p>
                <p><strong>收入总计:</strong> %.2f</p>
                <p><strong>费用总计:</strong> %.2f</p>
                <p><strong>净利润:</strong> <span class="%s">%.2f</span></p>
                <p><strong>资产负债表平衡:</strong> %s</p>
            </div>',
              entity_name,
              bs$total_assets,
              bs$total_liabilities,
              bs$total_equity,
              is$total_revenue,
              is$total_expense,
              ifelse(is$net_income >= 0, "positive", "negative"),
              is$net_income,
              ifelse(bs$balance_check, "✓", "✗")
      )
    }), collapse = "\n")
  )
  
  filename <- sprintf("%s/会计报告_%s.html", output_dir, timestamp)
  write(html_content, filename, fileEncoding = "UTF-8")
  cat(sprintf("HTML报告已生成: %s\n", filename))
  
  filename
}

# ------------------------------------------------------------------------------
# 执行主程序
# ------------------------------------------------------------------------------

# 检查是否在交互模式
if (interactive()) {
  cat("正在启动订单记账系统...\n")
  
  # 执行主程序
  accounting_results <- main_accounting_system()
  
  # 提供交互式查询功能
  cat("\n=== 交互式查询 ===\n")
  cat("可用的命令:\n")
  cat("1. view_entries('公司名称') - 查看公司会计分录\n")
  cat("2. view_balance('公司名称') - 查看公司科目余额\n")
  cat("3. view_report('公司名称') - 查看公司财务报表\n")
  cat("4. export_all() - 导出所有数据\n")
  
  # 定义交互函数
  view_entries <- function(company_name) {
    if (exists("accounting_results")) {
      entities_env <- accounting_results$processing_result$entities_env
      if (exists(company_name, envir = entities_env)) {
        entity <- get(company_name, envir = entities_env)
        entries <- entity$entries()
        
        if (nrow(entries) > 0) {
          entries$time <- as.POSIXct(entries$ts, origin = "1970-01-01")
          entries$direction <- ifelse(entries$drcr == 1, "借", "贷")
          
          return(entries[, c("time", "direction", "name", "amount", "tx")])
        } else {
          cat("无会计分录\n")
        }
      } else {
        cat(sprintf("公司 %s 不存在\n", company_name))
      }
    } else {
      cat("请先运行主程序\n")
    }
  }
  
  view_balance <- function(company_name) {
    if (exists("accounting_results")) {
      entities_env <- accounting_results$processing_result$entities_env
      if (exists(company_name, envir = entities_env)) {
        entity <- get(company_name, envir = entities_env)
        balance <- entity$balance()
        
        if (nrow(balance) > 0) {
          return(balance[order(abs(balance$balance), decreasing = TRUE), ])
        } else {
          cat("无科目余额\n")
        }
      } else {
        cat(sprintf("公司 %s 不存在\n", company_name))
      }
    } else {
      cat("请先运行主程序\n")
    }
  }
  
  view_report <- function(company_name) {
    if (exists("accounting_results")) {
      report <- accounting_results$financial_reports[[company_name]]
      if (!is.null(report)) {
        return(report)
      } else {
        cat(sprintf("公司 %s 的报表不存在\n", company_name))
      }
    } else {
      cat("请先运行主程序\n")
    }
  }
  
  export_all <- function() {
    if (exists("accounting_results")) {
      timestamp <- format(Sys.time(), "%Y%m%d_%H%M%S")
      save_file <- sprintf("./accounting_data/full_export_%s.rds", timestamp)
      saveRDS(accounting_results, save_file)
      cat(sprintf("数据已导出到: %s\n", save_file))
      return(save_file)
    } else {
      cat("请先运行主程序\n")
    }
  }
  
  # 将函数导出到全局环境
  assign("view_entries", view_entries, envir = .GlobalEnv)
  assign("view_balance", view_balance, envir = .GlobalEnv)
  assign("view_report", view_report, envir = .GlobalEnv)
  assign("export_all", export_all, envir = .GlobalEnv)
  
  cat("\n交互函数已加载，可以使用以下命令:\n")
  cat('view_entries("WALMART")\n')
  cat('view_balance("SINOPEC GROUP")\n')
  cat('view_report("CHINA NATIONAL PETROLEUM")\n')
}

# 直接运行（非交互模式）
if (!interactive()) {
  main_accounting_system()
}
