library(shiny)
library(ggplot2)
library(dplyr)
library(purrr)
library(magrittr)
library(lubridate)
library(tidyr)

batch_load()

# UI部分保持不变
ui <- fluidPage(
  titlePanel("金融数据分析仪表板"),
  tags$head(
    tags$script(HTML("function closeWindow(){window.close();}")),
    tags$style(HTML("
      .well-panel{padding:8px;margin-bottom:8px;background:#f8f9fa;border:1px solid #dee2e6;border-radius:4px;}
      .sql-display{padding:6px;margin:6px 0;background:#e9ecef;border:1px solid #ced4da;border-radius:3px;font-family:monospace;font-size:12px;}
      .stat-card{padding:6px;margin:4px 0;background:white;border:1px solid #dee2e6;border-radius:3px;font-size:12px;}
      .compact-row{margin-bottom:6px;}
      .compact-row .form-group{margin-bottom:6px;}
      .inline-group{display:flex;gap:6px;align-items:flex-end;}
      .inline-group .form-group{flex:1;margin-bottom:0;}
      .sidebar-tabs .tab-content{padding:8px;}
      .nav-tabs>li>a{padding:8px 12px;font-size:12px;}
      .small-table{font-size:11px;}
      .small-plot{font-size:11px;}
      .trading-session-btn{font-size:10px; padding:4px 6px;}
      .slider-value{font-size:10px; color:#666; text-align:center; margin-top:-5px;}
      .time-input-group{display:flex;align-items:center;gap:4px;}
      .time-input-group .form-group{flex:1;margin-bottom:0;}
      .radio-group{display:flex;gap:8px;margin-bottom:4px;}
      .auto-refresh{background-color: #e8f5e8 !important;}
    "))
  ),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      style = "padding: 8px;",
      
      div(class = "well-panel",
          actionButton("update", "手动刷新", class = "btn-primary btn-sm", width = "100%"),
          br(), 
          actionButton("close", "关闭应用", class = "btn-danger btn-sm", width = "100%")
      ),
      
      div(class = "sidebar-tabs",
          tabsetPanel(
            tabPanel("数据源",
                     div(class = "compact-row",
                         textInput("tbl", "数据表", value = "t_rb2601_20251117", width = "100%")
                     ),
                     div(class = "inline-group",
                         textInput("db", "数据库", value = "ctp", width = "100%"),
                         numericInput("port", "端口", value = 3371, width = "100%")
                     ),
                     div(class = "compact-row",
                         textInput("host", "主机", value = "127.0.0.1", width = "100%")
                     )
            ),
            
            tabPanel("时间范围",
                     div(class = "compact-row",
                         h5("时间范围选择", style = "margin: 0 0 8px 0; font-size: 12px;")
                     ),
                     
                     div(class = "time-input-group",
                         textInput("start_time", "开始时间", 
                                   value = format(now() - hours(2), "%H:%M:%S"),
                                   width = "100%")
                     ),
                     div(class = "time-input-group",
                         textInput("end_time", "结束时间", 
                                   value = format(now(), "%H:%M:%S"),
                                   width = "100%")
                     ),
                     
                     div(class = "radio-group",
                         radioButtons("lock_type", "锁定", 
                                      choices = c("开始时间" = "start", "结束时间" = "end"),
                                      selected = "end", inline = TRUE)
                     ),
                     
                     div(class = "compact-row",
                         actionButton("reset_time", "重置当前时间", class = "btn-warning btn-sm", width = "100%")
                     ),
                     
                     div(class = "compact-row",
                         sliderInput("time_range", "时长(分钟)", 
                                     min = 5, max = 480, value = 120, step = 5,
                                     width = "100%")
                     ),
                     div(class = "slider-value",
                         textOutput("time_range_text")
                     ),
                     
                     div(class = "compact-row",
                         checkboxInput("auto_refresh", "自动刷新图表", value = FALSE) |>
                           tagAppendAttributes(class = "auto-refresh")
                     ),
                     
                     div(class = "compact-row",
                         h5("交易时间段", style = "margin: 8px 0 4px 0; font-size: 11px; color: #666;")
                     ),
                     div(class = "inline-group",
                         actionButton("set_morning", "早盘", class = "trading-session-btn btn-warning", width = "100%"),
                         actionButton("set_afternoon", "午盘", class = "trading-session-btn btn-warning", width = "100%")
                     ),
                     div(class = "inline-group",
                         actionButton("set_night", "夜盘", class = "trading-session-btn btn-info", width = "100%"),
                         actionButton("set_full_day", "全天", class = "trading-session-btn btn-success", width = "100%")
                     ),
                     
                     div(class = "compact-row",
                         h5("快速设置", style = "margin: 8px 0 4px 0; font-size: 11px; color: #666;")
                     ),
                     div(class = "inline-group",
                         actionButton("set_1h", "1小时", class = "btn-info btn-sm", width = "100%"),
                         actionButton("set_2h", "2小时", class = "btn-info btn-sm", width = "100%"),
                         actionButton("set_3h", "3小时", class = "btn-info btn-sm", width = "100%")
                     )
            ),
            
            tabPanel("价格",
                     div(class = "compact-row",
                         numericInput("n", "时间点数", value = 10, min = 5, max = 50, width = "100%")
                     )
            ),
            
            tabPanel("成交量",
                     div(class = "compact-row",
                         numericInput("nlev", "分组数", value = 3, min = 2, max = 10, width = "100%")
                     ),
                     div(class = "compact-row",
                         radioButtons("group_mode", "分组模式",
                                      choices = c("等频分组(ntile)" = "ntile", 
                                                  "等距分组(cut)" = "cut"),
                                      selected = "ntile")
                     )
            )
          )
      )
    ),
    
    mainPanel(
      width = 9,
      style = "padding: 8px;",
      tabsetPanel(
        tabPanel("价格趋势", 
                 plotOutput("pricePlot", height = "300px"),
                 div(class = "sql-display",
                     verbatimTextOutput("priceSql")
                 )
        ),
        
        tabPanel("成交量分析",
                 fluidRow(
                   column(12, plotOutput("volPlot", height = "250px"))
                 ),
                 fluidRow(
                   column(6, 
                          div(class = "stat-card",
                              h5("成交量统计"),
                              tableOutput("volTable")
                          )
                   ),
                   column(6,
                          div(class = "stat-card",
                              h5("分布信息"),
                              verbatimTextOutput("volSummary")
                          )
                   )
                 ),
                 fluidRow(
                   column(12,
                          div(class = "stat-card",
                              h5("价格-成交量分布表"),
                              div(style = "max-height: 200px; overflow-y: auto;",
                                  tableOutput("volTable2")
                              )
                          )
                   )
                 )
        ),
        
        tabPanel("数据概览",
                 div(class = "stat-card",
                     verbatimTextOutput("dataSummary")
                 ),
                 div(class = "stat-card",
                     h5("数据预览 (前10行)"),
                     div(style = "max-height: 200px; overflow-y: auto;",
                         tableOutput("dataPreview")
                     )
                 )
        )
      )
    )
  )
)

server <- function(input, output, session) {
  
  # 数据库连接函数
  ds <- reactive({
    partial(sqlquery, dbname = input$db, port = input$port, host = input$host)
  })
  
  # ========== 核心状态管理 ==========
  
  # 1. 手动刷新触发器
  manual_trigger <- reactiveVal(0)
  
  # 2. 自动刷新触发器
  auto_trigger <- reactiveVal(0)
  
  # 3. SQL显示状态
  sql_display <- reactiveVal("设置时间范围后点击手动刷新查看数据")
  
  # ========== 工具函数 ==========
  
  # 生成SQL语句
  generate_sql <- function() {
    req(input$tbl, input$start_time, input$end_time)
    sprintf("select * from %s where UpdateTime between '%s' and '%s'", 
            input$tbl, input$start_time, input$end_time)
  }
  
  # 解析时间字符串
  parse_time <- function(time_str) {
    if (grepl("^\\d{1,2}:\\d{2}:\\d{2}$", time_str)) {
      today <- as.Date(now())
      return(ymd_hms(paste(today, time_str)))
    }
    NULL
  }
  
  # 更新时间显示文本
  output$time_range_text <- renderText({
    if (input$time_range < 60) {
      paste(input$time_range, "分钟")
    } else if (input$time_range == 60) {
      "1小时"
    } else if (input$time_range < 120) {
      paste("1小时", input$time_range - 60, "分钟")
    } else if (input$time_range == 120) {
      "2小时"
    } else if (input$time_range < 180) {
      paste("2小时", input$time_range - 120, "分钟")
    } else if (input$time_range == 180) {
      "3小时"
    } else {
      paste(round(input$time_range / 60, 1), "小时")
    }
  })
  
  # ========== 时间控制逻辑 ==========
  
  # 滑动条调整时间
  observeEvent(input$time_range, {
    start_time <- parse_time(input$start_time)
    end_time <- parse_time(input$end_time)
    
    if (!is.null(start_time) && !is.null(end_time)) {
      if (input$lock_type == "start") {
        new_end_time <- start_time + minutes(input$time_range)
        updateTextInput(session, "end_time", value = format(new_end_time, "%H:%M:%S"))
      } else {
        new_start_time <- end_time - minutes(input$time_range)
        updateTextInput(session, "start_time", value = format(new_start_time, "%H:%M:%S"))
      }
    }
    
    # 更新SQL显示
    sql_display(generate_sql())
    
    # 如果自动刷新启用，触发自动刷新
    if (input$auto_refresh) {
      auto_trigger(auto_trigger() + 1)
    }
  })
  
  # 手动输入时间时更新SQL
  observeEvent(input$start_time, {
    sql_display(generate_sql())
    if (input$auto_refresh) auto_trigger(auto_trigger() + 1)
  })
  
  observeEvent(input$end_time, {
    sql_display(generate_sql())
    if (input$auto_refresh) auto_trigger(auto_trigger() + 1)
  })
  
  # 重置当前时间
  observeEvent(input$reset_time, {
    current_time <- format(now(), "%H:%M:%S")
    if (input$lock_type == "start") {
      updateTextInput(session, "start_time", value = current_time)
    } else {
      updateTextInput(session, "end_time", value = current_time)
    }
  })
  
  # 交易时间段设置
  trading_session_handlers <- list(
    set_morning = list(start = "09:00:00", end = "12:00:00", range = 180),
    set_afternoon = list(start = "13:00:00", end = "15:00:00", range = 120),
    set_night = list(start = "21:00:00", end = "23:00:00", range = 120),
    set_full_day = list(start = "09:00:00", end = "15:00:00", range = 360)
  )
  
  # 为每个交易时间段按钮创建观察者
  lapply(names(trading_session_handlers), function(btn_id) {
    observeEvent(input[[btn_id]], {
      config <- trading_session_handlers[[btn_id]]
      updateTextInput(session, "start_time", value = config$start)
      updateTextInput(session, "end_time", value = config$end)
      updateSliderInput(session, "time_range", value = config$range)
      sql_display(generate_sql())
      if (input$auto_refresh) auto_trigger(auto_trigger() + 1)
    })
  })
  
  # 快速设置按钮
  quick_set_handlers <- list(
    set_1h = 60,
    set_2h = 120,
    set_3h = 180
  )
  
  lapply(names(quick_set_handlers), function(btn_id) {
    observeEvent(input[[btn_id]], {
      updateSliderInput(session, "time_range", value = quick_set_handlers[[btn_id]])
    })
  })
  
  # ========== 数据查询逻辑 ==========
  
  # 统一的查询触发器
  query_trigger <- reactive({
    list(
      manual = manual_trigger(),
      auto = auto_trigger()
    )
  })
  
  # 价格数据查询
  price_data <- eventReactive(query_trigger(), {
    sql <- sql_display()
    
    tryCatch({
      result <- ds()(sql)
      if (is.null(result) || nrow(result) == 0) {
        sql_display(paste(sql, "\n\n查询结果为空"))
        return(NULL)
      }
      result
    }, error = function(e) {
      sql_display(paste("SQL错误:", e$message))
      NULL
    })
  })
  
  # 成交量数据查询
  volume_data <- eventReactive(query_trigger(), {
    req(input$tbl, input$start_time, input$end_time)
    
    sql <- sprintf(
      "select Id, LastPrice, Volume, Volume - lag(Volume) over(order by Id) as Vol, UpdateTime from %s where UpdateTime between '%s' and '%s'", 
      input$tbl, input$start_time, input$end_time
    )
    
    tryCatch({ 
      result <- ds()(sql)
      result
    }, error = function(e) { 
      NULL 
    })
  })
  
  # ========== 用户操作处理 ==========
  
  # 手动刷新按钮
  observeEvent(input$update, {
    manual_trigger(manual_trigger() + 1)
    sql_display(generate_sql())
  })
  
  # ========== 分组处理函数 ==========
  
  # 创建分组标签
  create_levels <- function(df, nlev, mode) {
    req(df, nlev > 0)
    
    if (mode == "ntile") {
      # 等频分组
      df <- df %>%
        mutate(Level = as.factor(ntile(Vol, nlev)))
    } else {
      # 等距分组
      breaks <- seq(min(df$Vol), max(df$Vol), length.out = nlev + 1)
      labels <- sprintf("(%.1f, %.1f]", head(breaks, -1), tail(breaks, -1))
      
      # 美化标签：将科学计数法转换为K格式
      labels <- gsub("e\\+03", "K", labels)
      
      df <- df %>%
        mutate(Level = cut(Vol, breaks = breaks, labels = labels, include.lowest = TRUE))
    }
    
    return(df)
  }
  
  # ========== 输出渲染 ==========
  
  # SQL显示
  output$priceSql <- renderText({ sql_display() })
  
  # 价格趋势图
  output$pricePlot <- renderPlot({
    df <- price_data()
    req(df, nrow(df) > 0)
    
    tryCatch({
      lm_model <- lm(LastPrice ~ Id, data = df)
      lm_coef <- coef(lm_model)
      
      with(df, {
        ggplot(df, aes(Id, LastPrice)) + 
          geom_point(alpha = 0.01, color = "#3498db", size = 0.5) + 
          geom_smooth(color = "#e74c3c", se = TRUE, size = 0.8) +
          geom_hline(yintercept = mean(LastPrice) + c(-2, 0, 2) * sd(LastPrice),
                     linewidth = c(1, 0.5, 1), color = c("#c0392b", "#95a5a6", "#e74c3c")) +
          geom_abline(slope = lm_coef[2], intercept = lm_coef[1], 
                      linewidth = 1, color = "#9b59b6") +
          scale_x_continuous(
            breaks = seq(min(Id), max(Id), length.out = input$n), 
            labels = function(x) UpdateTime[x - min(Id) + 1]
          ) +
          labs(title = NULL, x = "时间", y = "价格") +
          theme_minimal() +
          theme(text = element_text(size = 10),
                axis.text = element_text(size = 8))
      })
    }, error = function(e) {
      plot(1, 1, type = "n", main = "绘图错误")
      text(1, 1, paste("错误:", e$message), col = "red", cex = 0.7)
    })
  })
  
  # 成交量分析图
  output$volPlot <- renderPlot({
    df <- volume_data()
    req(df, nrow(df) > 0, input$nlev > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    tryCatch({
      # 使用选择的分组模式
      df_clean <- create_levels(df_clean, input$nlev, input$group_mode)
      
      xs <- df_clean |> 
        select(LastPrice, Level) |> 
        table() |> 
        prop.table() |> 
        addmargins() 
      
      plot_data <- as.data.frame(xs) %>%
        filter(Level != "Sum" & LastPrice != "Sum") %>%
        mutate(LastPrice = as.numeric(as.character(LastPrice)),
               Frequency = ifelse(Freq > 0, Freq, NA))
      
      ggplot(plot_data, aes(x = LastPrice, y = Frequency, fill = Level)) +
        geom_col(position = "stack", alpha = 0.8, width = 0.8) +
        scale_fill_brewer(palette = "Set3") +
        labs(title = NULL, x = "价格", y = "频率", fill = "成交量分组") +
        theme_minimal() +
        theme(axis.text.x = element_text(angle = 45, hjust = 1),
              legend.position = "bottom")
      
    }, error = function(e) {
      plot(1, 1, type = "n", main = "分析错误")
      text(1, 1, paste("错误:", e$message), col = "red", cex = 0.7)
    })
  })
  
  # 统计表格
  output$volTable <- renderTable({
    df <- volume_data()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    data.frame(
      统计项 = c("均值", "最大值", "最小值", "标准差"),
      数值 = round(c(mean(df_clean$Vol), max(df_clean$Vol), 
                   min(df_clean$Vol), sd(df_clean$Vol)), 2)
    )
  }, bordered = TRUE, align = 'c', width = "100%")
  
  output$volSummary <- renderPrint({
    df <- volume_data()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    cat("价格档位:", length(unique(df_clean$LastPrice)), "\n")
    cat("成交量组:", input$nlev, "\n")
    cat("分组模式:", ifelse(input$group_mode == "ntile", "等频分组(ntile)", "等距分组(cut)"), "\n")
    cat("观测数:", nrow(df_clean))
  })
  
  # 价格-成交量分布表
  output$volTable2 <- renderTable({
    df <- volume_data()
    req(df, nrow(df) > 0, input$nlev > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    tryCatch({
      df_clean <- create_levels(df_clean, input$nlev, input$group_mode)
      
      xs <- df_clean |> 
        select(LastPrice, Level) |> 
        table() |> 
        prop.table() |> 
        addmargins() 
      
      result_table <- as.data.frame(xs) %>%
        pivot_wider(names_from = Level, values_from = Freq) %>%
        arrange(LastPrice)
      
      # 美化列名：将科学计数法转换为K格式
      colnames(result_table) <- gsub("e\\+03", "K", colnames(result_table))
      
      result_table
      
    }, error = function(e) {
      data.frame(错误 = paste("表格错误:", e$message))
    })
  }, bordered = TRUE, digits = 3, width = "100%")
  
  output$dataSummary <- renderPrint({
    df <- price_data()
    req(df)
    
    cat("数据概览\n")
    cat("记录数:", nrow(df), "\n")
    cat("选择时间:", input$start_time, "至", input$end_time, "\n")
    cat("实际时间:", format(min(df$UpdateTime)), "至", format(max(df$UpdateTime)), "\n")
    cat("价格: 均值", round(mean(df$LastPrice), 2), 
        "标准差", round(sd(df$LastPrice), 2))
    
    vol_df <- volume_data()
    if(!is.null(vol_df) && nrow(vol_df) > 0 && "Vol" %in% names(vol_df)) {
      vol_clean <- vol_df[!is.na(vol_df$Vol), ]
      cat("\n成交量: 均值", round(mean(vol_clean$Vol), 2), 
          "标准差", round(sd(vol_clean$Vol), 2))
    }
  })
  
  output$dataPreview <- renderTable({
    df <- price_data()
    req(df)
    
    result <- head(df, 10)
    simple_cols <- names(result)[sapply(result, function(x) is.numeric(x) | is.character(x))]
    result <- result[simple_cols]
    
    if("UpdateTime" %in% names(result)) {
      if(inherits(result$UpdateTime, "POSIXt")) {
        result$UpdateTime <- format(result$UpdateTime, "%H:%M:%S")
      } else if(is.character(result$UpdateTime)) {
        result$UpdateTime <- substr(result$UpdateTime, 1, 8)
      }
    }
    
    result
  }, bordered = TRUE, width = "100%")
  
  # 关闭应用
  observeEvent(input$close, {
    showModal(modalDialog(
      "确定关闭应用吗？",
      footer = tagList(
        actionButton("confirm_close", "确定", class = "btn-danger btn-sm"),
        modalButton("取消")
      )
    ))
  })
  
  observeEvent(input$confirm_close, {
    removeModal()
    stopApp()
  })
}

shinyApp(ui, server)
