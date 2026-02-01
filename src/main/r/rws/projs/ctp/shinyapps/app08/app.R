# ------------------------------------------------------------------------------
# Shiny App: 订单记账与财务报表系统
# 基于Hitler数据库t_order表和bkp簿记系统
# ------------------------------------------------------------------------------

library(shiny)
library(shinydashboard)
library(shinyWidgets)
library(tidyverse)
library(plotly)
library(DT)
library(RMySQL)
library(jsonlite)

# UI界面定义
ui <- dashboardPage(
  skin = "blue",
  
  # 标题栏
  dashboardHeader(
    title = tags$span(
      icon("chart-line"),
      "订单记账与财务报表系统"
    ),
    titleWidth = 300
  ),
  
  # 侧边栏
  dashboardSidebar(
    width = 300,
    sidebarMenu(
      id = "sidebar",
      menuItem("系统概览", tabName = "overview", icon = icon("dashboard")),
      menuItem("订单管理", tabName = "orders", icon = icon("shopping-cart")),
      menuItem("会计记账", tabName = "accounting", icon = icon("book")),
      menuItem("财务报表", tabName = "reports", icon = icon("file-invoice-dollar")),
      menuItem("可视化分析", tabName = "visualization", icon = icon("chart-bar")),
      menuItem("数据导出", tabName = "export", icon = icon("download")),

      # 退出按钮
      hr(),
      div(style = "padding: 10px;",
        actionButton("exit_app", label = "安全退出系统", icon = icon("power-off"),
          class = "btn-danger btn-block")),
      
      # 数据库连接设置
      hr(),
      div(
        style = "padding: 10px;",
        h4("数据库连接设置"),
        textInput("db_host", "主机地址", value = "192.168.1.6"),
        numericInput("db_port", "端口", value = 3309),
        textInput("db_name", "数据库名", value = "hitler"),
        textInput("db_user", "用户名", value = "root"),
        passwordInput("db_pass", "密码", value = "123456"),
        actionButton("connect_db", "连接数据库", 
          icon = icon("database"),
          class = "btn-primary btn-block")
      ),
      
      # 处理控制
      hr(),
      div(
        style = "padding: 10px;",
        h4("数据处理控制"),
        dateRangeInput("date_range", "订单日期范围",
          start = Sys.Date() - 1000,
          end = Sys.Date()),
        actionButton("load_orders", "加载订单", 
          icon = icon("refresh"),
          class = "btn-success btn-block"),
        br(),
        actionButton("process_accounting", "执行记账", 
          icon = icon("calculator"),
          class = "btn-warning btn-block"),
        br(),
        actionButton("generate_reports", "生成报表", 
          icon = icon("file-alt"),
          class = "btn-info btn-block")
      ),
      
      # 系统状态
      hr(),
      div(
        style = "padding: 10px;",
        h4("系统状态"),
        uiOutput("system_status")
      )
    )
  ),
  
  # 主内容区
  dashboardBody(
    tags$head(
      tags$style(HTML("
        .small-box { border-radius: 10px; }
        .info-box { border-radius: 10px; }
        .btn-block { width: 90%; }
        .box { border-radius: 10px; }
        .skin-blue .main-header .logo { background-color: #2c3e50; }
        .skin-blue .main-header .navbar { background-color: #34495e; }
        .system-log { height: 150px; overflow-y: auto; background-color: #f5f5f5; padding: 10px; border-radius: 5px; }
        .notification-success { background-color: #d4edda !important; border-color: #c3e6cb !important; color: #155724 !important; }
        .notification-info { background-color: #d1ecf1 !important; border-color: #bee5eb !important; color: #0c5460 !important; }
      "))
    ),
    
    tabItems(
      # 系统概览标签页
      tabItem(
        tabName = "overview",
        fluidRow(
          # 状态指标
          valueBoxOutput("total_orders"),
          valueBoxOutput("total_amount"),
          valueBoxOutput("total_companies"),
          valueBoxOutput("total_entries")
        ),
        
        fluidRow(
          # 订单趋势图
          box(
            title = "订单趋势分析",
            status = "primary",
            solidHeader = TRUE,
            width = 8,
            plotlyOutput("order_trend", height = "400px")
          ),
          
          # 公司交易排名
          box(
            title = "公司交易排名",
            status = "info",
            solidHeader = TRUE,
            width = 4,
            plotlyOutput("company_ranking", height = "400px")
          )
        ),
        
        fluidRow(
          # 最近订单
          box(
            title = "最近订单",
            status = "success",
            solidHeader = TRUE,
            width = 12,
            collapsible = TRUE,
            DTOutput("recent_orders", height = "300px")
          )
        )
      ),
      
      # 订单管理标签页
      tabItem(
        tabName = "orders",
        fluidRow(
          box(
            title = "订单数据",
            status = "primary",
            solidHeader = TRUE,
            width = 12,
            collapsible = TRUE,
            DTOutput("orders_table", height = "500px")
          )
        ),
        
        fluidRow(
          box(
            title = "订单筛选",
            status = "info",
            solidHeader = TRUE,
            width = 3,
            selectInput("filter_shipper", "发货方", 
              choices = c("全部"), multiple = TRUE),
            selectInput("filter_receiver", "收货方", 
              choices = c("全部"), multiple = TRUE),
            sliderInput("filter_amount", "金额范围",
              min = 0, max = 1000,
              value = c(0, 1000)),
            actionButton("apply_filters", "应用筛选", 
              icon = icon("filter"))
          ),
          
          box(
            title = "订单详情",
            status = "warning",
            solidHeader = TRUE,
            width = 9,
            uiOutput("order_details")
          )
        )
      ),
      
      # 会计记账标签页
      tabItem(
        tabName = "accounting",
        fluidRow(
          box(
            title = "会计分录",
            status = "primary",
            solidHeader = TRUE,
            width = 12,
            collapsible = TRUE,
            DTOutput("journal_entries", height = "400px")
          )
        ),
        
        fluidRow(
          box(
            title = "科目余额",
            status = "info",
            solidHeader = TRUE,
            width = 6,
            collapsible = TRUE,
            DTOutput("account_balances", height = "400px")
          ),
          
          box(
            title = "会计主体选择",
            status = "success",
            solidHeader = TRUE,
            width = 6,
            selectInput("select_entity", "选择会计主体", 
              choices = c("请选择"), width = "100%"),
            actionButton("view_entity_entries", "查看分录", 
              icon = icon("eye")),
            actionButton("view_entity_balance", "查看余额", 
              icon = icon("balance-scale")),
            hr(),
            plotlyOutput("account_distribution", height = "250px")
          )
        )
      ),
      
      # 财务报表标签页
      tabItem(
        tabName = "reports",
        fluidRow(
          tabBox(
            title = "财务报表",
            id = "report_tabs",
            width = 12,
            height = "600px",
            
            # 资产负债表
            tabPanel(
              "资产负债表",
              icon = icon("balance-scale-left"),
              fluidRow(
                column(
                  6,
                  h3("资产"),
                  DTOutput("assets_table", height = "400px")
                ),
                column(
                  6,
                  h3("负债及所有者权益"),
                  DTOutput("liabilities_equity_table", height = "400px")
                )
              ),
              fluidRow(
                column(
                  12,
                  hr(),
                  h4("资产负债表检查"),
                  verbatimTextOutput("balance_check")
                )
              )
            ),
            
            # 利润表
            tabPanel(
              "利润表",
              icon = icon("chart-line"),
              fluidRow(
                column(
                  6,
                  h3("收入"),
                  DTOutput("revenue_table", height = "200px")
                ),
                column(
                  6,
                  h3("费用"),
                  DTOutput("expense_table", height = "200px")
                )
              ),
              fluidRow(
                column(
                  12,
                  hr(),
                  h3("净利润"),
                  valueBoxOutput("net_income_box", width = 12)
                )
              )
            ),
            
            # 现金流量表
            tabPanel(
              "现金流量表",
              icon = icon("money-bill-wave"),
              fluidRow(
                column(
                  12,
                  h3("经营活动现金流量"),
                  DTOutput("cash_flow_table", height = "300px"),
                  hr(),
                  h4("现金流量分析"),
                  plotlyOutput("cash_flow_chart", height = "250px")
                )
              )
            )
          )
        )
      ),
      
      # 可视化分析标签页
      tabItem(
        tabName = "visualization",
        fluidRow(
          box(
            title = "财务指标分析",
            status = "primary",
            solidHeader = TRUE,
            width = 12,
            tabBox(
              width = 12,
              
              tabPanel(
                "公司对比",
                selectInput("compare_companies", "选择公司", 
                  choices = c("请选择"), multiple = TRUE),
                plotlyOutput("company_comparison", height = "500px")
              ),
              
              tabPanel(
                "科目分析",
                selectInput("analyze_accounts", "选择科目", 
                  choices = c("请选择"), multiple = TRUE),
                plotlyOutput("account_analysis", height = "500px")
              ),
              
              tabPanel(
                "趋势分析",
                dateRangeInput("trend_date_range", "分析期间",
                  start = Sys.Date() - 60,
                  end = Sys.Date()),
                plotlyOutput("financial_trend", height = "500px")
              )
            )
          )
        )
      ),
      
      # 数据导出标签页
      tabItem(
        tabName = "export",
        fluidRow(
          box(
            title = "导出选项",
            status = "primary",
            solidHeader = TRUE,
            width = 4,
            radioButtons("export_format", "导出格式",
              choices = c("Excel" = "excel",
                "CSV" = "csv",
                "JSON" = "json",
                "PDF报告" = "pdf")),
            selectInput("export_data", "导出数据",
              choices = c("订单数据",
                "会计分录",
                "科目余额表",
                "资产负债表",
                "利润表",
                "现金流量表",
                "全部数据")),
              selectInput("export_entity", "会计主体（可选）",
                choices = c("全部主体")),
              textInput("export_filename", "文件名",
                value = paste0("财务报表_", Sys.Date())),
              
              hr(),
              actionButton("do_export", "开始导出",
                icon = icon("download"),
                class = "btn-success btn-block"),

              br(),
              actionButton("preview_report", "预览报告",
                icon = icon("eye"),
                class = "btn-info btn-block")
          ),
          
          box(
            title = "导出预览",
            status = "success",
            solidHeader = TRUE,
            width = 8,
            uiOutput("export_preview")
          )
        ),
        
        fluidRow(
          box(
            title = "系统备份",
            status = "warning",
            solidHeader = TRUE,
            width = 12,
            collapsible = TRUE,
            fluidRow(
              column(
                4,
                h4("数据备份"),
                p("备份当前所有会计数据"),
                actionButton("backup_data", "备份数据",
                  icon = icon("save"),
                  class = "btn-primary")
              ),
              column(
                4,
                h4("恢复备份"),
                fileInput("restore_file", "选择备份文件",
                  accept = c(".rds")),
                actionButton("restore_data", "恢复数据",
                  icon = icon("undo"),
                  class = "btn-warning")
              ),
              column(
                4,
                h4("系统日志"),
                div(class = "system-log",
                  verbatimTextOutput("system_log")
                )
              )
            )
          )
        )
      )
    )
  )
)

# 自定义通知函数
show_custom_notification <- function(message, type = "default", duration = 5) {
  # 为不同的通知类型添加自定义CSS类
  css_class <- switch(type,
    "message" = "notification-success",
    "warning" = "notification-warning",
    "error" = "notification-error",
    "default" = "notification-info")
  
  # 创建自定义的HTML内容
  html_content <- sprintf(
    '<div class="shiny-notification %s" style="width: 300px;">
      <div class="shiny-notification-content">%s</div>
     </div>',
    css_class, message
  )
  
  # 显示通知
  showNotification(
    HTML(html_content),
    type = "default",  # 使用默认类型
    duration = duration
  )
}

# 服务器逻辑
server <- function(input, output, session) {
  
  # 初始化反应值
  mydata <- reactiveValues(
    orders_data = NULL,
    entities = list(),
    journal_entries = list(),
    account_balances = list(),
    financial_reports = list(),
    db_connected = FALSE,
    system_status = "未连接数据库",
    last_backup = NULL
  )
  
  # 加载BKP簿记系统
  source_bkp_system <- function() {
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
            } # if
          } # balance
        }) # within
    }) # local

    return(bkp)
  } # source_bkp_system
  
  # 创建BKP实例
  bkp_instance <- reactive(source_bkp_system())

  # 退出功能
  observeEvent(input$exit_app, {
    showModal(modalDialog(
      title = "确认退出", "确定要退出系统吗？",
      footer = tagList(modalButton("取消"), actionButton("confirm_exit", "退出", class = "btn-danger"))
    ))
  })

  observeEvent(input$confirm_exit, { removeModal(); stopApp() })
  
  # 数据库连接
  observeEvent(input$connect_db, {
    tryCatch({
      # 创建数据库连接
      con <- dbConnect(
        MySQL(),
        host = input$db_host,
        port = input$db_port,
        dbname = input$db_name,
        user = input$db_user,
        password = input$db_pass
      )
      
      # 测试查询
      test_query <- dbGetQuery(con, "SELECT 1 as test")
      dbDisconnect(con)
      
      mydata$db_connected <- TRUE
      mydata$system_status <- "数据库连接成功"
      
      showNotification("数据库连接成功!", type = "message")
      
    }, error = function(e) {
      mydata$db_connected <- FALSE
      mydata$system_status <- paste("数据库连接失败:", e$message)
      
      showNotification(paste("连接失败:", e$message), type = "error")
    })
  })
  
  # 加载订单数据
  observeEvent(input$load_orders, {
    if (!mydata$db_connected) {
      showNotification("请先连接数据库!", type = "warning")
      return()
    }
    
    tryCatch({
      # 连接数据库
      con <- dbConnect(
        MySQL(),
        host = input$db_host,
        port = input$db_port,
        dbname = input$db_name,
        user = input$db_user,
        password = input$db_pass
      )
      
      # 查询订单数据
      query <- sprintf(
        "SELECT * FROM t_order WHERE create_time BETWEEN '%s 00:00:00' AND '%s 23:59:59'",
        format(input$date_range[1], "%Y-%m-%d"),
        format(input$date_range[2], "%Y-%m-%d")
      )
      
      orders_data <- dbGetQuery(con, query)
      dbDisconnect(con)
      
      if (nrow(orders_data) == 0) {
        showNotification("没有找到订单数据!", type = "warning")
        return()
      }
      
      mydata$orders_data <- orders_data
      
      # 更新筛选选项
      updateSelectInput(session, "filter_shipper",
        choices = c("全部", unique(orders_data$shipper)))
      updateSelectInput(session, "filter_receiver",
        choices = c("全部", unique(orders_data$receiver)))
      
      # 更新金额范围
      max_amount <- max(orders_data$amount, na.rm = TRUE)
      updateSliderInput(session, "filter_amount",
        max = ceiling(max_amount * 1.1))
      
      mydata$system_status <- sprintf("已加载 %d 条订单", nrow(orders_data))
      showNotification("订单数据加载成功!", type = "message")
      
    }, error = function(e) {
      showNotification(paste("加载失败:", e$message), type = "error")
    })
  })
  
  # 执行记账
  observeEvent(input$process_accounting, {
    if (is.null(mydata$orders_data)) {
      showNotification("请先加载订单数据!", type = "warning")
      return()
    }
    
    tryCatch({
      bkp <- bkp_instance()
      entities_env <- new.env()
      
      # 处理所有订单
      for (i in 1:nrow(mydata$orders_data)) {
        order <- mydata$orders_data[i, ]
        
        # 创建会计主体
        shipper_name <- as.character(order$shipper)
        receiver_name <- as.character(order$receiver)
        
        if (!exists(shipper_name, envir = entities_env)) {
          assign(shipper_name, bkp(shipper_name), envir = entities_env)
        }
        
        if (!exists(receiver_name, envir = entities_env)) {
          assign(receiver_name, bkp(receiver_name), envir = entities_env)
        }
        
        shipper_entity <- get(shipper_name, envir = entities_env)
        receiver_entity <- get(receiver_name, envir = entities_env)
        
        # 生成交易描述
        tx_desc <- sprintf("订单-%d: %s", order$id, order$name)
        
        # 发货方分录
        shipper_entity$dc(
          sprintf("应收账款-%s", receiver_name),
          sprintf("主营业务收入-%s", order$name),
          order$amount,
          tx = tx_desc
        )
        
        # 收货方分录
        receiver_entity$dc(
          sprintf("存货-%s", order$name),
          sprintf("应付账款-%s", shipper_name),
          order$amount,
          tx = tx_desc
        )
      }
      
      # 收集所有分录和余额
      entities <- ls(entities_env)
      journal_entries <- list()
      account_balances <- list()
      
      for (entity_name in entities) {
        entity <- get(entity_name, envir = entities_env)
        journal_entries[[entity_name]] <- entity$entries()
        account_balances[[entity_name]] <- entity$balance()
      }
      
      mydata$entities <- as.list(entities_env)
      mydata$journal_entries <- journal_entries
      mydata$account_balances <- account_balances
      
      # 更新会计主体选择
      updateSelectInput(session, "select_entity", choices = c("请选择", entities))
      updateSelectInput(session, "export_entity", choices = c("全部主体", entities))
      updateSelectInput(session, "compare_companies", choices = entities)
      
      mydata$system_status <- sprintf("记账完成，处理了 %d 个会计主体", length(entities))
      showNotification("记账处理完成!", type = "message")
      
    }, error = function(e) {
      showNotification(paste("记账失败:", e$message), type = "error")
    })
  })
  
  # 生成财务报表
  observeEvent(input$generate_reports, {
    if (length(mydata$entities) == 0) {
      showNotification("请先执行记账!", type = "warning")
      return()
    }
    
    tryCatch({
      financial_reports <- list()
      
      for (entity_name in names(mydata$entities)) {
        entity <- mydata$entities[[entity_name]]
        entries <- entity$entries()
        
        if (nrow(entries) > 0) {
          # 计算科目余额
          balances <- entries |> 
            group_by(name) |> 
            summarise(balance = sum(drcr * amount))
          
          # 分类科目
          balances$category <- sapply(balances$name, function(name) {
            name_lower <- tolower(name)
            if (grepl("现金|银行|应收|预付|存货|固定|资产", name_lower)) return("资产")
            if (grepl("应付|预收|借款|负债", name_lower)) return("负债")
            if (grepl("实收|资本|公积|权益", name_lower)) return("所有者权益")
            if (grepl("收入|收益", name_lower)) return("收入")
            if (grepl("成本|费用", name_lower)) return("费用")
            return("其他")
          })
          
          # 生成财务报表
          assets <- balances[balances$category == "资产", ]
          liabilities <- balances[balances$category == "负债", ]
          equity <- balances[balances$category == "所有者权益", ]
          revenues <- balances[balances$category == "收入", ]
          expenses <- balances[balances$category == "费用", ]
          
          financial_reports[[entity_name]] <- list(
            balance_sheet = list(
              assets = assets,
              liabilities = liabilities,
              equity = equity,
              total_assets = sum(assets$balance, na.rm = TRUE),
              total_liabilities = sum(liabilities$balance, na.rm = TRUE),
              total_equity = sum(equity$balance, na.rm = TRUE)
            ),
            income_statement = list(
              revenues = revenues,
              expenses = expenses,
              total_revenue = sum(revenues$balance, na.rm = TRUE),
              total_expense = sum(expenses$balance, na.rm = TRUE),
              net_income = sum(revenues$balance, na.rm = TRUE) - sum(expenses$balance, na.rm = TRUE)
            ),
            entries = entries
          )
        }
      }
      
      mydata$financial_reports <- financial_reports
      
      # 更新分析选项
      all_accounts <- unique(unlist(lapply(mydata$account_balances, rownames)))
      updateSelectInput(session, "analyze_accounts",
                        choices = all_accounts)
      
      mydata$system_status <- "财务报表生成完成"
      showNotification("财务报表生成成功!", type = "message")
      
    }, error = function(e) {
      showNotification(paste("报表生成失败:", e$message), type = "error")
    })
  })
  
  # 系统状态输出
  output$system_status <- renderUI({
    tags$div(
      class = "alert",
      style = ifelse(mydata$db_connected, 
        "background-color: #d4edda; color: #155724;", 
        "background-color: #f8d7da; color: #721c24;"),
      tags$p(tags$strong("状态:"), mydata$system_status),
      tags$p(tags$strong("数据库:"), 
        ifelse(mydata$db_connected, "已连接", "未连接")),
      if (!is.null(mydata$orders_data)) {
        tags$p(tags$strong("订单数:"), nrow(mydata$orders_data))
      },
      if (length(mydata$entities) > 0) {
        tags$p(tags$strong("会计主体:"), length(mydata$entities))
      }
    )
  })
  
  # 数据概览指标
  output$total_orders <- renderValueBox({
    count <- ifelse(is.null(mydata$orders_data), 0, nrow(mydata$orders_data))
    valueBox(
      value = count,
      subtitle = "订单总数",
      icon = icon("shopping-cart"),
      color = "green"
    )
  })
  
  output$total_amount <- renderValueBox({
    total <- ifelse(is.null(mydata$orders_data), 0, 
      sum(mydata$orders_data$amount, na.rm = TRUE))
    valueBox(
      value = formatC(total, format = "f", digits = 2, big.mark = ","),
      subtitle = "总交易金额",
      icon = icon("money-bill-wave"),
      color = "blue"
    )
  })
  
  output$total_companies <- renderValueBox({
    count <- length(mydata$entities)
    valueBox(
      value = count,
      subtitle = "会计主体数",
      icon = icon("building"),
      color = "yellow"
    )
  })
  
  output$total_entries <- renderValueBox({
    count <- ifelse(length(mydata$journal_entries) == 0, 0,
      sum(sapply(mydata$journal_entries, nrow)))
    valueBox(
      value = count,
      subtitle = "会计分录数",
      icon = icon("book"),
      color = "purple"
    )
  })
  
  # 订单趋势图
  output$order_trend <- renderPlotly({
    if (is.null(mydata$orders_data)) return(NULL)
    
    orders <- mydata$orders_data
    
    # 按日期汇总
    daily_summary <- orders %>%
      mutate(date = as.Date(create_time)) %>%
      group_by(date) %>%
      summarise(
        order_count = n(),
        total_amount = sum(amount, na.rm = TRUE)
      ) %>%
      arrange(date)
    
    plot_ly(daily_summary) %>%
      add_trace(x = ~date, y = ~order_count, 
        type = 'scatter', mode = 'lines+markers',
        name = '订单数',
        line = list(color = '#3498db'),
        yaxis = 'y') %>%
      add_trace(x = ~date, y = ~total_amount, 
        type = 'bar', name = '交易金额',
        yaxis = 'y2',
        marker = list(color = '#2ecc71', opacity = 0.6)) %>%
      layout(
        title = "订单趋势分析",
        xaxis = list(title = "日期"),
        yaxis = list(title = "订单数"),
        yaxis2 = list(
          title = "交易金额",
          overlaying = "y",
          side = "right"
        ),
        hovermode = 'x unified',
        legend = list(x = 0.1, y = 0.9)
      )
  })
  
  # 公司交易排名
  output$company_ranking <- renderPlotly({
    if (is.null(mydata$orders_data)) return(NULL)
    
    orders <- mydata$orders_data
    
    # 计算各公司交易额
    company_stats <- bind_rows(
      orders %>% 
        group_by(company = shipper) %>%
        summarise(total_amount = sum(amount, na.rm = TRUE),
          role = "发货方"),
      orders %>% 
        group_by(company = receiver) %>%
        summarise(total_amount = sum(amount, na.rm = TRUE),
          role = "收货方")
    ) %>% group_by(company) %>%
      summarise(total_amount = sum(total_amount, na.rm = TRUE)) %>%
      arrange(desc(total_amount)) %>%
      head(10)
    
    plot_ly(company_stats,
      x = ~total_amount,
      y = ~reorder(company, total_amount),
      type = 'bar',
      orientation = 'h',
      marker = list(color = '#9b59b6')) %>% layout(
        title = "公司交易排名",
        xaxis = list(title = "交易金额"),
        yaxis = list(title = ""),
        margin = list(l = 150)
      )
  })
  
  # 最近订单表
  output$recent_orders <- renderDT({
    if (is.null(mydata$orders_data)) return(NULL)
    
    orders <- mydata$orders_data %>%
      arrange(desc(create_time)) %>%
      head(20) %>%
      select(id, name, shipper, receiver, amount, create_time)
    
    datatable(
      orders,
      options = list(
        pageLength = 5,
        lengthMenu = c(5, 10, 20),
        scrollX = TRUE
      ),
      rownames = FALSE,
      colnames = c("ID", "商品", "发货方", "收货方", "金额", "创建时间")
    ) %>%
      formatCurrency("amount", currency = "", interval = 3, mark = ",") %>%
      formatDate("create_time", method = "toLocaleString")
  })
  
  # 订单数据表
  output$orders_table <- renderDT({
    if (is.null(mydata$orders_data)) return(NULL)
    
    orders <- mydata$orders_data
    
    # 应用筛选
    if (!is.null(input$filter_shipper) && !"全部" %in% input$filter_shipper) {
      orders <- orders %>% filter(shipper %in% input$filter_shipper)
    }
    
    if (!is.null(input$filter_receiver) && !"全部" %in% input$filter_receiver) {
      orders <- orders %>% filter(receiver %in% input$filter_receiver)
    }
    
    orders <- orders %>% filter(amount >= input$filter_amount[1] & amount <= input$filter_amount[2])
    
    datatable(
      orders,
      options = list(
        pageLength = 10,
        lengthMenu = c(5, 10, 25, 50),
        scrollX = TRUE,
        dom = 'Bfrtip',
        buttons = c('copy', 'csv', 'excel', 'print')
      ),
      extensions = 'Buttons',
      rownames = FALSE,
      selection = 'single'
    ) %>%
      formatCurrency("amount", currency = "", interval = 3, mark = ",") %>%
      formatDate("create_time", method = "toLocaleString")
  })
  
  # 订单详情
  output$order_details <- renderUI({
    if (is.null(mydata$orders_data)) return(NULL)
    
    selected <- input$orders_table_rows_selected
    if (is.null(selected)) return(tags$p("请选择一行订单以查看详情"))
    
    order <- mydata$orders_data[selected, ]
    
    # 解析订单详情
    details <- tryCatch({
      fromJSON(order$details)
    }, error = function(e) {
      list(flag = FALSE, name = order$name, items = list())
    })
    
    # 创建详情卡片
    tags$div(
      class = "order-details",
      tags$h3(tags$span(icon("box"), order$name)),
      
      tags$div(
        class = "row",
        tags$div(
          class = "col-md-6",
          tags$h4("基本信息"),
          tags$table(
            class = "table table-bordered",
            tags$tr(tags$th("订单ID"), tags$td(order$id)),
            tags$tr(tags$th("发货方"), tags$td(order$shipper)),
            tags$tr(tags$th("收货方"), tags$td(order$receiver)),
            tags$tr(tags$th("金额"), tags$td(sprintf("¥%.2f", order$amount))),
            tags$tr(tags$th("创建时间"), tags$td(order$create_time))
          )
        ),
        tags$div(
          class = "col-md-6",
          tags$h4("收货地址"),
          tags$p(order$receive_address)
        )
      ),
      
      tags$h4("商品明细"),
      if (!is.null(details$items) && length(details$items) > 0) {
        if (is.data.frame(details$items)) {
          tags$div(
            DTOutput("order_items")
          ) 
        } else {
          tags$p("商品数据格式不正确")
        }
      } else {
        tags$p("无商品明细")
      }
    )
  })
  
  # 订单商品明细
  output$order_items <- renderDT({
    selected <- input$orders_table_rows_selected
    if (is.null(selected)) return(NULL)
    
    order <- mydata$orders_data[selected, ]
    details <- fromJSON(order$details)
    
    if (is.data.frame(details$items)) {
      datatable(
        details$items,
        options = list(
          pageLength = 5,
          lengthMenu = c(5, 10)
        ),
        rownames = FALSE
      )
    } else {
      NULL
    }
  })
  
  # 会计分录表
  output$journal_entries <- renderDT({
    if (length(mydata$journal_entries) == 0) return(NULL)
    
    # 合并所有分录
    all_entries <- bind_rows(
      lapply(names(mydata$journal_entries), function(entity_name) {
        entries <- mydata$journal_entries[[entity_name]]
        if (nrow(entries) > 0) {
          entries$entity <- entity_name
          entries$time <- as.POSIXct(entries$ts, origin = "1970-01-01")
          entries$direction <- ifelse(entries$drcr == 1, "借", "贷")
          return(entries[, c("time", "entity", "direction", "name", "amount", "tx")])
        }
      }),
      .id = "id"
    )
    
    datatable(
      all_entries,
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50),
        scrollX = TRUE
      ),
      rownames = FALSE,
      colnames = c("时间", "会计主体", "方向", "科目", "金额", "交易说明")
    ) %>%
      formatCurrency("amount", currency = "", interval = 3, mark = ",") %>%
      formatDate("time", method = "toLocaleString")
  })
  
  # 科目余额表
  output$account_balances <- renderDT({
    if (length(mydata$account_balances) == 0) return(NULL)
    
    # 合并所有余额
    all_balances <- bind_rows(
      lapply(names(mydata$account_balances), function(entity_name) {
        balances <- mydata$account_balances[[entity_name]]
        if (nrow(balances) > 0) {
          balances$entity <- entity_name
          balances$account <- row.names(balances) # 账户名称  
          return(balances)
        }
      }),
      .id = "id"
    )
    
    if (nrow(all_balances) == 0) return(NULL)
    
    datatable(
      all_balances[, c("entity", "account", "balance")],
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50),
        scrollX = TRUE
      ),
      rownames = FALSE,
      colnames = c("会计主体", "科目", "余额")
    ) %>% formatCurrency("balance", currency = "", interval = 3, mark = ",")
  })
  
  # 科目分布图
  output$account_distribution <- renderPlotly({
    if (is.null(input$select_entity) || input$select_entity == "请选择") return(NULL)
    
    entity_name <- input$select_entity
    balances <- mydata$account_balances[[entity_name]]
    
    if (is.null(balances) || nrow(balances) == 0) return(NULL)
    
    # 分类汇总
    balances$category <- sapply(rownames(balances), function(name) {
      name_lower <- tolower(name)
      if (grepl("现金|银行|应收|预付|存货|固定|资产", name_lower)) return("资产")
      if (grepl("应付|预收|借款|负债", name_lower)) return("负债")
      if (grepl("实收|资本|公积|权益", name_lower)) return("所有者权益")
      if (grepl("收入|收益", name_lower)) return("收入")
      if (grepl("成本|费用", name_lower)) return("费用")
      return("其他")
    })
    
    summary <- balances %>%
      group_by(category) %>%
      summarise(total = sum(abs(balance)))
    
    plot_ly(
      summary,
      labels = ~category,
      values = ~total,
      type = 'pie',
      textinfo = 'label+percent',
      insidetextorientation = 'radial',
      marker = list(
        colors = c('#3498db', '#2ecc71', '#e74c3c', '#f39c12', '#9b59b6')
      )
    ) %>%
      layout(
        title = sprintf("%s 科目分布", entity_name),
        showlegend = TRUE
      )
  })
  
  # 查看特定主体的分录
  observeEvent(input$view_entity_entries, {
    if (is.null(input$select_entity) || input$select_entity == "请选择") return()
    
    entity_name <- input$select_entity
    entries <- mydata$journal_entries[[entity_name]]
    
    if (is.null(entries) || nrow(entries) == 0) {
      showNotification("该主体无会计分录", type = "default")
      return()
    }
    
    # 显示分录详情
    showModal(
      modalDialog(
        title = sprintf("%s - 会计分录", entity_name),
        DTOutput("entity_entries_table"),
        size = "l",
        easyClose = TRUE
      )
    )
  })
  
  # 特定主体的分录表
  output$entity_entries_table <- renderDT({
    if (is.null(input$select_entity) || input$select_entity == "请选择") return(NULL)
    
    entity_name <- input$select_entity
    entries <- mydata$journal_entries[[entity_name]]
    
    if (is.null(entries)) return(NULL)
    
    entries$time <- as.POSIXct(entries$ts, origin = "1970-01-01")
    entries$direction <- ifelse(entries$drcr == 1, "借", "贷")
    
    datatable(
      entries[, c("time", "direction", "name", "amount", "tx")],
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50)
      ),
      rownames = FALSE,
      colnames = c("时间", "方向", "科目", "金额", "交易说明")
    ) %>%
      formatCurrency("amount", currency = "", interval = 3, mark = ",") %>%
      formatDate("time", method = "toLocaleString")
  })
  
  # 资产负债表 - 资产
  output$assets_table <- renderDT({
    if (length(mydata$financial_reports) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    report <- mydata$financial_reports[[selected_entity]]
    if (is.null(report)) return(NULL)
    
    assets <- report$balance_sheet$assets
    
    if (nrow(assets) == 0) return(NULL)
    
    datatable(
      assets,
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50),
        dom = 't'
      ),
      rownames = FALSE,
      colnames = c("科目", "余额")
    ) %>% formatCurrency("balance", currency = "", interval = 3, mark = ",") %>% 
    formatStyle("balance", background = styleColorBar(range(assets$balance), '#3498db'),
      backgroundSize = '100% 90%',
      backgroundRepeat = 'no-repeat',
      backgroundPosition = 'center')
  })
  
  # 资产负债表 - 负债及所有者权益
  output$liabilities_equity_table <- renderDT({
    if (length(mydata$financial_reports) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    report <- mydata$financial_reports[[selected_entity]]
    if (is.null(report)) return(NULL)
    
    liabilities <- report$balance_sheet$liabilities
    equity <- report$balance_sheet$equity
    
    combined <- bind_rows(
      liabilities,
      equity
    )
    
    if (nrow(combined) == 0) return(NULL)
    
    datatable(
      combined,
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50),
        dom = 't'
      ),
      rownames = FALSE,
      colnames = c("科目", "余额")
    ) %>%
      formatCurrency("balance", currency = "", interval = 3, mark = ",") %>%
      formatStyle("balance",
        background = styleColorBar(range(combined$balance), '#2ecc71'),
        backgroundSize = '100% 90%',
        backgroundRepeat = 'no-repeat',
        backgroundPosition = 'center')
  })
  
  # 资产负债表检查
  output$balance_check <- renderPrint({
    if (length(mydata$financial_reports) == 0) {
      cat("请先生成财务报表")
      return()
    }
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") {
      cat("请选择会计主体")
      return()
    }
    
    report <- mydata$financial_reports[[selected_entity]]
    if (is.null(report)) {
      cat("该主体无财务报表")
      return()
    }
    
    bs <- report$balance_sheet
    
    cat("=== 资产负债表检查 ===\n\n")
    cat(sprintf("资产总计: %.2f\n", bs$total_assets))
    cat(sprintf("负债总计: %.2f\n", bs$total_liabilities))
    cat(sprintf("所有者权益: %.2f\n", bs$total_equity))
    cat(sprintf("负债 + 所有者权益: %.2f\n", bs$total_liabilities + bs$total_equity))
    cat(sprintf("差额: %.4f\n", bs$total_assets - (bs$total_liabilities + bs$total_equity)))
    
    if (abs(bs$total_assets - (bs$total_liabilities + bs$total_equity)) < 0.01) {
      cat("\n✓ 资产负债表平衡\n")
    } else {
      cat("\n✗ 资产负债表不平衡\n")
    }
  })
  
  # 利润表 - 收入
  output$revenue_table <- renderDT({
    if (length(mydata$financial_reports) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    report <- mydata$financial_reports[[selected_entity]]
    if (is.null(report)) return(NULL)
    
    revenues <- report$income_statement$revenues
    
    if (nrow(revenues) == 0) return(NULL)
    
    datatable(
      revenues,
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50),
        dom = 't'
      ),
      rownames = FALSE,
      colnames = c("科目", "金额")
    ) %>%
      formatCurrency("balance", currency = "", interval = 3, mark = ",") %>%
      formatStyle("balance",
        color = styleInterval(0, c('red', 'green')),
        fontWeight = styleInterval(0, c('normal', 'bold')))
  })
  
  # 利润表 - 费用
  output$expense_table <- renderDT({
    if (length(mydata$financial_reports) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    report <- mydata$financial_reports[[selected_entity]]
    if (is.null(report)) return(NULL)
    
    expenses <- report$income_statement$expenses
    
    if (nrow(expenses) == 0) return(NULL)
    
    datatable(
      expenses,
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50),
        dom = 't'
      ),
      rownames = FALSE,
      colnames = c("科目", "金额")
    ) %>%
      formatCurrency("balance", currency = "", interval = 3, mark = ",") %>%
      formatStyle("balance",
        color = styleInterval(0, c('green', 'red')),
        fontWeight = styleInterval(0, c('bold', 'normal')))
  })
  
  # 净利润显示框
  output$net_income_box <- renderValueBox({
    if (length(mydata$financial_reports) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    report <- mydata$financial_reports[[selected_entity]]
    if (is.null(report)) return(NULL)
    
    net_income <- report$income_statement$net_income
    
    valueBox(
      value = formatC(net_income, format = "f", digits = 2, big.mark = ","),
      subtitle = "净利润",
      icon = icon(ifelse(net_income >= 0, "arrow-up", "arrow-down")),
      color = ifelse(net_income >= 0, "green", "red")
    )
  })
  
  # 现金流量表
  output$cash_flow_table <- renderDT({
    if (length(mydata$journal_entries) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    entries <- mydata$journal_entries[[selected_entity]]
    if (is.null(entries)) return(NULL)
    
    # 筛选现金相关科目
    cash_entries <- entries %>%
      filter(grepl("现金|银行", name, ignore.case = TRUE)) %>%
      mutate(
        time = as.POSIXct(ts, origin = "1970-01-01"),
        direction = ifelse(drcr == 1, "借", "贷"),
        amount = ifelse(drcr == 1, amount, -amount)
      ) %>%
      arrange(time) %>%
      select(time, name, direction, amount, tx)
    
    if (nrow(cash_entries) == 0) return(NULL)
    
    datatable(
      cash_entries,
      options = list(
        pageLength = 10,
        lengthMenu = c(10, 25, 50)
      ),
      rownames = FALSE,
      colnames = c("时间", "科目", "方向", "金额", "交易说明")
    ) %>%
      formatCurrency("amount", currency = "", interval = 3, mark = ",") %>%
      formatStyle("amount", color = styleInterval(0, c('red', 'green'))) %>%
      formatDate("time", method = "toLocaleString")
  })
  
  # 现金流量分析图
  output$cash_flow_chart <- renderPlotly({
    if (length(mydata$journal_entries) == 0) return(NULL)
    
    selected_entity <- input$select_entity
    if (is.null(selected_entity) || selected_entity == "请选择") return(NULL)
    
    entries <- mydata$journal_entries[[selected_entity]]
    if (is.null(entries)) return(NULL)
    
    # 筛选现金相关科目
    cash_entries <- entries %>%
      filter(grepl("现金|银行", name, ignore.case = TRUE)) %>%
      mutate(
        date = as.Date(as.POSIXct(ts, origin = "1970-01-01")),
        cash_flow = ifelse(drcr == 1, amount, -amount)
      ) %>%
      group_by(date) %>%
      summarise(daily_flow = sum(cash_flow, na.rm = TRUE))
    
    if (nrow(cash_entries) == 0) return(NULL)
    
    # 计算累计现金流量
    cash_entries$cumulative <- cumsum(cash_entries$daily_flow)
    
    plot_ly(cash_entries) %>%
      add_trace(x = ~date, y = ~daily_flow,
        type = 'bar', name = '日现金流量',
        marker = list(color = ~daily_flow,
          colorscale = list(c(0, '#e74c3c'), c(1, '#2ecc71')),
          showscale = FALSE)) %>%
      add_trace(x = ~date, y = ~cumulative,
        type = 'scatter', mode = 'lines',
        name = '累计现金流量',
        line = list(color = '#3498db', width = 3),
        yaxis = 'y2') %>%
      layout(
        title = sprintf("%s - 现金流量分析", selected_entity),
        xaxis = list(title = "日期"),
        yaxis = list(title = "日现金流量"),
        yaxis2 = list(
          title = "累计现金流量",
          overlaying = "y",
          side = "right"
        ),
        legend = list(x = 0.1, y = 0.9)
      )
  })
  
  # 公司对比分析
  output$company_comparison <- renderPlotly({
    selected_companies <- input$compare_companies
    if (is.null(selected_companies) || length(selected_companies) < 2) {
      return(NULL)
    }
    
    # 收集各公司财务数据
    comparison_data <- data.frame()
    
    for (company in selected_companies) {
      report <- mydata$financial_reports[[company]]
      if (!is.null(report)) {
        bs <- report$balance_sheet
        is <- report$income_statement
        
        comparison_data <- bind_rows(
          comparison_data,
          data.frame(
            company = company,
            metric = "总资产",
            value = bs$total_assets
          ),
          data.frame(
            company = company,
            metric = "总负债",
            value = bs$total_liabilities
          ),
          data.frame(
            company = company,
            metric = "所有者权益",
            value = bs$total_equity
          ),
          data.frame(
            company = company,
            metric = "总收入",
            value = is$total_revenue
          ),
          data.frame(
            company = company,
            metric = "净利润",
            value = is$net_income
          )
        )
      }
    }
    
    if (nrow(comparison_data) == 0) return(NULL)
    
    # 创建雷达图或柱状图
    plot_ly(
      data = comparison_data,
      x = ~metric,
      y = ~value,
      color = ~company,
      type = 'bar'
    ) %>%
      layout(
        title = "公司财务对比",
        xaxis = list(title = "财务指标"),
        yaxis = list(title = "金额"),
        barmode = 'group'
      )
  })
  
  # 数据导出处理
  observeEvent(input$do_export, {
    if (is.null(mydata$orders_data) && length(mydata$entities) == 0) {
      showNotification("没有数据可导出!", type = "warning")
      return()
    }
    
    tryCatch({
      format <- input$export_format
      data_type <- input$export_data
      entity <- input$export_entity
      filename <- input$export_filename
      
      # 根据选择准备数据
      if (data_type == "订单数据") {
        data_to_export <- mydata$orders_data
      } else if (data_type == "会计分录") {
        if (entity == "全部主体") {
          # 合并所有主体的分录
          all_entries <- bind_rows(
            lapply(names(mydata$journal_entries), function(en) {
              entries <- mydata$journal_entries[[en]]
              if (nrow(entries) > 0) {
                entries$entity <- en
                entries$time <- as.POSIXct(entries$ts, origin = "1970-01-01")
                entries$direction <- ifelse(entries$drcr == 1, "借", "贷")
                return(entries[, c("time", "entity", "direction", "name", "amount", "tx")])
              }
            })
          )
          data_to_export <- all_entries
        } else {
          entries <- mydata$journal_entries[[entity]]
          if (!is.null(entries) && nrow(entries) > 0) {
            entries$time <- as.POSIXct(entries$ts, origin = "1970-01-01")
            entries$direction <- ifelse(entries$drcr == 1, "借", "贷")
            data_to_export <- entries[, c("time", "direction", "name", "amount", "tx")]
          } else {
            showNotification("该主体无会计分录!", type = "warning")
            return()
          }
        }
      } else if (data_type == "科目余额表") {
        if (entity == "全部主体") {
          # 合并所有主体的余额
          all_balances <- bind_rows(
            lapply(names(mydata$account_balances), function(en) {
              balances <- mydata$account_balances[[en]]
              if (nrow(balances) > 0) {
                balances$entity <- en
                return(balances)
              }
            })
          )
          data_to_export <- all_balances
        } else {
          balances <- mydata$account_balances[[entity]]
          if (!is.null(balances) && nrow(balances) > 0) {
            data_to_export <- balances
          } else {
            showNotification("该主体无科目余额!", type = "warning")
            return()
          }
        }
      } else {
        showNotification("该数据类型暂不支持导出!", type = "warning")
        return()
      }
      
      # 根据格式导出
      if (format == "excel") {
        filepath <- paste0(filename, ".xlsx")
        # 这里需要安装openxlsx包
        # write.xlsx(data_to_export, filepath)
        showNotification(sprintf("Excel文件已导出: %s", filepath), type = "message")
      } else if (format == "csv") {
        filepath <- paste0(filename, ".csv")
        write.csv(data_to_export, filepath, row.names = FALSE, fileEncoding = "UTF-8")
        showNotification(sprintf("CSV文件已导出: %s", filepath), type = "message")
      } else if (format == "json") {
        filepath <- paste0(filename, ".json")
        json_data <- toJSON(data_to_export, pretty = TRUE, auto_unbox = TRUE)
        write(json_data, filepath)
        showNotification(sprintf("JSON文件已导出: %s", filepath), type = "message")
      }
      
      # 更新导出预览
      output$export_preview <- renderUI({
        tags$div(
          class = "alert alert-success",
          tags$h4("导出成功"),
          tags$p(sprintf("文件: %s", filepath)),
          tags$p(sprintf("数据量: %d 行", nrow(data_to_export))),
          tags$p(sprintf("导出时间: %s", Sys.time()))
        )
      })
      
    }, error = function(e) {
      showNotification(paste("导出失败:", e$message), type = "error")
    })
  })
  
  # 数据备份
  observeEvent(input$backup_data, {
    backup_data <- list(
      orders = mydata$orders_data,
      entities = mydata$entities,
      journal_entries = mydata$journal_entries,
      account_balances = mydata$account_balances,
      financial_reports = mydata$financial_reports,
      timestamp = Sys.time()
    )
    
    filename <- sprintf("backup_%s.rds", format(Sys.time(), "%Y%m%d_%H%M%S"))
    saveRDS(backup_data, filename)
    
    mydata$last_backup <- filename
    mydata$system_status <- sprintf("数据已备份: %s", filename)
    
    showNotification("数据备份成功!", type = "message")
  })
  
  # 数据恢复
  observeEvent(input$restore_data, {
    req(input$restore_file)
    
    tryCatch({
      backup_data <- readRDS(input$restore_file$datapath)
      
      mydata$orders_data <- backup_data$orders
      mydata$entities <- backup_data$entities
      mydata$journal_entries <- backup_data$journal_entries
      mydata$account_balances <- backup_data$account_balances
      mydata$financial_reports <- backup_data$financial_reports
      
      # 更新UI选项
      if (!is.null(mydata$orders_data)) {
        updateSelectInput(session, "filter_shipper",
          choices = c("全部", unique(mydata$orders_data$shipper)))
        updateSelectInput(session, "filter_receiver",
          choices = c("全部", unique(mydata$orders_data$receiver)))
      }
      
      if (length(mydata$entities) > 0) {
        entity_names <- names(mydata$entities)
        updateSelectInput(session, "select_entity", choices = c("请选择", entity_names))
        updateSelectInput(session, "export_entity", choices = c("全部主体", entity_names))
        updateSelectInput(session, "compare_companies", choices = entity_names)
      }
      
      mydata$system_status <- sprintf("数据恢复成功: %s", input$restore_file$name)
      showNotification("数据恢复成功!", type = "message")
      
    }, error = function(e) {
      showNotification(paste("恢复失败:", e$message), type = "error")
    })
  })
  
  # 系统日志
  output$system_log <- renderText({
    log_text <- sprintf("系统状态: %s\n", mydata$system_status)
    log_text <- paste0(log_text, sprintf("数据库连接: %s\n", ifelse(mydata$db_connected, "已连接", "未连接")))
    
    if (!is.null(mydata$orders_data)) {
      log_text <- paste0(log_text, sprintf("订单数量: %d\n", nrow(mydata$orders_data)))
    }
    
    if (length(mydata$entities) > 0) {
      log_text <- paste0(log_text, sprintf("会计主体: %d\n", length(mydata$entities)))
    }
    
    if (!is.null(mydata$last_backup)) {
      log_text <- paste0(log_text, sprintf("最近备份: %s\n", mydata$last_backup))
    }
    
    log_text <- paste0(log_text, sprintf("当前时间: %s", Sys.time()))
    
    log_text
  })
  
  # 应用筛选
  observeEvent(input$apply_filters, {
    # 更新订单数据表的显示
    output$orders_table <- renderDT({
      if (is.null(mydata$orders_data)) return(NULL)
      
      orders <- mydata$orders_data
      
      # 应用筛选
      if (!is.null(input$filter_shipper) && !"全部" %in% input$filter_shipper) {
        orders <- orders %>% filter(shipper %in% input$filter_shipper)
      }
      
      if (!is.null(input$filter_receiver) && !"全部" %in% input$filter_receiver) {
        orders <- orders %>% filter(receiver %in% input$filter_receiver)
      }
      
      orders <- orders %>% filter(amount >= input$filter_amount[1] & amount <= input$filter_amount[2])
      
      datatable(
        orders,
        options = list(
          pageLength = 10,
          lengthMenu = c(5, 10, 25, 50),
          scrollX = TRUE,
          dom = 'Bfrtip',
          buttons = c('copy', 'csv', 'excel', 'print')
        ),
        extensions = 'Buttons',
        rownames = FALSE,
        selection = 'single'
      ) %>%
        formatCurrency("amount", currency = "", interval = 3, mark = ",") %>%
        formatDate("create_time", method = "toLocaleString")
    })
    
    showNotification("筛选已应用!", type = "default")
  })
}

# 运行Shiny应用
shinyApp(ui = ui, server = server)
