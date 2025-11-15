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
                         textInput("tbl", "数据表", value = "t_rb2601_20251113", width = "100%")
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
                 plotOutput("pricePlot", height = "450px"),
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
  
  # ========== 核心状态管理 ==========
  manual_trigger <- reactiveVal(0)
  auto_trigger <- reactiveVal(0)
  sql_display <- reactiveVal("设置时间范围后点击手动刷新查看数据")
  
  # ========== 配置常量 ==========
  trading_sessions <- list(
    set_morning = list(start = "09:00:00", end = "12:00:00", range = 180),
    set_afternoon = list(start = "13:00:00", end = "15:00:00", range = 120),
    set_night = list(start = "21:00:00", end = "23:00:00", range = 120),
    set_full_day = list(start = "09:00:00", end = "15:00:00", range = 360)
  )
  
  quick_settings <- list(set_1h = 60, set_2h = 120, set_3h = 180)
  
  # ========== 工具函数 ==========
  ds <- reactive(partial(sqlquery, dbname = input$db, port = input$port, host = input$host))
  
  generate_sql <- function() {
    req(input$tbl, input$start_time, input$end_time)
    sprintf("select * from %s where UpdateTime between '%s' and '%s'", 
            input$tbl, input$start_time, input$end_time)
  }
  
  parse_time <- function(time_str) {
    if (grepl("^\\d{1,2}:\\d{2}:\\d{2}$", time_str)) {
      today <- as.Date(now())
      return(ymd_hms(paste(today, time_str)))
    }
    NULL
  }
  
  update_sql_and_trigger <- function() {
    sql_display(generate_sql())
    if (input$auto_refresh) auto_trigger(auto_trigger() + 1)
  }
  
  # 在 server 函数中修改 create_levels 函数
  create_levels <- function(df, nlev, mode) {
    req(df, nlev > 0)
    
    if (mode == "ntile") {
      df <- df %>% mutate(Level_ntile = ntile(Vol, nlev))
      level_ranges <- df %>%
        group_by(Level_ntile) %>%
        summarise(min_vol = min(Vol), max_vol = max(Vol), .groups = 'drop') %>%
        arrange(min_vol) %>%  # 按最小值排序
        mutate(Level = sprintf("(%.1f, %.1f]", min_vol, max_vol))
      
      df <- df %>% 
        left_join(level_ranges %>% select(Level_ntile, Level), by = "Level_ntile") %>%
        select(-Level_ntile)
      
      # 设置因子水平以保持排序
      df$Level <- factor(df$Level, levels = level_ranges$Level)
    } else {
      breaks <- seq(min(df$Vol), max(df$Vol), length.out = nlev + 1)
      labels <- sprintf("(%.1f, %.1f]", head(breaks, -1), tail(breaks, -1))
      labels <- gsub("e\\+03", "K", labels)
      
      # 创建有序因子
      df <- df %>% mutate(Level = cut(Vol, breaks = breaks, labels = labels, include.lowest = TRUE))
      df$Level <- factor(df$Level, levels = labels)
    }
    return(df)
  }
  
  # ========== 统一观察者设置 ==========
  # SQL更新触发器
  sql_update_inputs <- c("tbl", "db", "port", "host", "start_time", "end_time")
  lapply(sql_update_inputs, function(input_id) {
    observeEvent(input[[input_id]], {
      if (input_id %in% c("tbl", "start_time", "end_time") && input$auto_refresh) {
        update_sql_and_trigger()
      } else {
        sql_display(generate_sql())
      }
    })
  })
  
  # 时间范围控制
  observeEvent(input$time_range, {
    start_time <- parse_time(input$start_time)
    end_time <- parse_time(input$end_time)
    
    if (!is.null(start_time) && !is.null(end_time)) {
      new_time <- if (input$lock_type == "start") {
        list(end_time = format(start_time + minutes(input$time_range), "%H:%M:%S"))
      } else {
        list(start_time = format(end_time - minutes(input$time_range), "%H:%M:%S"))
      }
      updateTextInput(session, names(new_time), value = new_time[[1]])
    }
    update_sql_and_trigger()
  })
  
  # 按钮组设置
  lapply(names(trading_sessions), function(btn_id) {
    observeEvent(input[[btn_id]], {
      config <- trading_sessions[[btn_id]]
      updateTextInput(session, "start_time", value = config$start)
      updateTextInput(session, "end_time", value = config$end)
      updateSliderInput(session, "time_range", value = config$range)
      update_sql_and_trigger()
    })
  })
  
  lapply(names(quick_settings), function(btn_id) {
    observeEvent(input[[btn_id]], {
      updateSliderInput(session, "time_range", value = quick_settings[[btn_id]])
    })
  })
  
  # ========== 事件处理 ==========
  observeEvent(input$reset_time, {
    current_time <- format(now(), "%H:%M:%S")
    updateTextInput(session, ifelse(input$lock_type == "start", "start_time", "end_time"), 
                    value = current_time)
  })
  
  observeEvent(input$update, {
    manual_trigger(manual_trigger() + 1)
    sql_display(generate_sql())
  })
  
  # ========== 数据查询 ==========
  query_trigger <- reactive(list(manual = manual_trigger(), auto = auto_trigger()))
  
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
  
  volume_data <- eventReactive(query_trigger(), {
    req(input$tbl, input$start_time, input$end_time)
    sql <- sprintf(
      "select Id, LastPrice, Volume, Volume - lag(Volume) over(order by Id) as Vol, UpdateTime from %s where UpdateTime between '%s' and '%s'", 
      input$tbl, input$start_time, input$end_time
    )
    tryCatch({ ds()(sql) }, error = function(e) { NULL })
  })
  
  # ========== 输出渲染 ==========
  output$time_range_text <- renderText({
    hours <- floor(input$time_range / 60)
    minutes <- input$time_range %% 60
    
    if (hours == 0) paste(input$time_range, "分钟")
    else if (minutes == 0) paste(hours, "小时")
    else paste(hours, "小时", minutes, "分钟")
  })
  
  output$priceSql <- renderText(sql_display())
  
  # 价格趋势图
  output$pricePlot <- renderPlot({
    df <- price_data()
    req(df, nrow(df) > 0)
    
    tryCatch({
      lm_model <- lm(LastPrice ~ Id, data = df)
      ggplot(df, aes(Id, LastPrice)) + 
        geom_point(alpha = 0.1, color = "#3498db", size = 0.5) + 
        geom_smooth(color = "#e74c3c", se = TRUE, linewidth = 0.8) +
        geom_hline(yintercept = mean(df$LastPrice) + c(-2, 0, 2) * sd(df$LastPrice),
                   linewidth = c(1, 0.5, 1), color = c("#c0392b", "#95a5a6", "#e74c3c")) +
        geom_abline(slope = coef(lm_model)[2], intercept = coef(lm_model)[1], 
                    linewidth = 1, color = "#9b59b6") +
        scale_x_continuous(
          breaks = seq(min(df$Id), max(df$Id), length.out = input$n), 
          labels = function(x) df$UpdateTime[x - min(df$Id) + 1]
        ) +
        labs(x = "时间", y = "价格") +
        theme_minimal() +
        theme(text = element_text(size = 10), axis.text = element_text(size = 8))
    }, error = function(e) {
      plot(1, 1, type = "n", main = "绘图错误")
      text(1, 1, paste("错误:", e$message), col = "red", cex = 0.7)
    })
  })
  
  # 成交量分析
  render_volume_analysis <- function() {
    df <- volume_data()
    req(df, nrow(df) > 0, input$nlev > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    tryCatch({
      df_clean <- create_levels(df_clean, input$nlev, input$group_mode)
      xs <- df_clean |> select(LastPrice, Level) |> table() |> prop.table() |> addmargins()
      
      plot_data <- as.data.frame(xs) %>%
        filter(Level != "Sum" & LastPrice != "Sum") %>%
        mutate(LastPrice = as.numeric(as.character(LastPrice)),
               Frequency = ifelse(Freq > 0, Freq, NA))
      
      ggplot(plot_data, aes(x = LastPrice, y = Frequency, fill = Level)) +
        geom_col(position = "stack", alpha = 0.8, width = 0.8) +
        scale_fill_brewer(palette = "Set3") +
        labs(x = "价格", y = "频率", fill = "成交量分组") +
        theme_minimal() +
        theme(axis.text.x = element_text(angle = 45, hjust = 1), legend.position = "bottom")
    }, error = function(e) {
      plot(1, 1, type = "n", main = "分析错误")
      text(1, 1, paste("错误:", e$message), col = "red", cex = 0.7)
    })
  }
  
  output$volPlot <- renderPlot(render_volume_analysis())
  
  output$volTable <- renderTable({
    df <- volume_data()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    stats <- c(mean(df_clean$Vol), max(df_clean$Vol), min(df_clean$Vol), sd(df_clean$Vol))
    data.frame(
      统计项 = c("均值", "最大值", "最小值", "标准差"),
      数值 = round(stats, 2)
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
  
  # 修改 volTable2 的输出部分
  output$volTable2 <- renderTable({
    df <- volume_data()
    req(df, nrow(df) > 0, input$nlev > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    tryCatch({
      df_clean <- create_levels(df_clean, input$nlev, input$group_mode)
      xs <- df_clean |> select(LastPrice, Level) |> table() |> prop.table() |> addmargins()
      
      result_table <- as.data.frame(xs) %>%
        pivot_wider(names_from = Level, values_from = Freq) %>%
        arrange(LastPrice)
      
      # 确保列按正确的数值顺序排列
      level_cols <- setdiff(names(result_table), c("LastPrice", "Sum"))
      
      # 提取最小值用于排序
      get_min_value <- function(col_name) {
        as.numeric(gsub(".*\\(([0-9.-]+).*", "\\1", col_name))
      }
      
      sorted_cols <- level_cols[order(sapply(level_cols, get_min_value))]
      
      # 重新排列列顺序
      result_table <- result_table %>% select(LastPrice, all_of(sorted_cols), Sum)
      
      colnames(result_table) <- gsub("e\\+03", "K", colnames(result_table))
      result_table
    }, error = function(e) {
      data.frame(错误 = paste("表格错误:", e$message))
    })
  }, bordered = TRUE, digits = 3, width = "100%")
  
  output$dataSummary <- renderPrint({
    df <- price_data()
    req(df)
    
    cat("数据概览\n记录数:", nrow(df), "\n")
    cat("选择时间:", input$start_time, "至", input$end_time, "\n")
    cat("实际时间:", format(min(df$UpdateTime)), "至", format(max(df$UpdateTime)), "\n")
    cat("价格: 均值", round(mean(df$LastPrice), 2), "标准差", round(sd(df$LastPrice), 2))
    
    vol_df <- volume_data()
    if(!is.null(vol_df) && nrow(vol_df) > 0 && "Vol" %in% names(vol_df)) {
      vol_clean <- vol_df[!is.na(vol_df$Vol), ]
      cat("\n成交量: 均值", round(mean(vol_clean$Vol), 2), "标准差", round(sd(vol_clean$Vol), 2))
    }
  })
  
  output$dataPreview <- renderTable({
    df <- price_data()
    req(df)
    
    result <- head(df, 10)
    simple_cols <- names(result)[sapply(result, function(x) is.numeric(x) | is.character(x))]
    result <- result[simple_cols]
    
    if("UpdateTime" %in% names(result)) {
      result$UpdateTime <- if(inherits(result$UpdateTime, "POSIXt")) {
        format(result$UpdateTime, "%H:%M:%S")
      } else {
        substr(result$UpdateTime, 1, 8)
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
