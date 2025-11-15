library(shiny)
library(ggplot2)
library(dplyr)
library(purrr)
library(magrittr)
library(lubridate)
library(tidyr)

batch_load()

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
    "))
  ),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      style = "padding: 8px;",
      
      div(class = "well-panel",
          actionButton("update", "更新数据", class = "btn-primary btn-sm", width = "100%"),
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
  
  ds <- reactive({
    partial(sqlquery, dbname = input$db, port = input$port, host = input$host)
  })
  
  sqlText <- reactiveVal("点击更新数据查看SQL")
  
  # 解析时间字符串
  parseTime <- function(time_str) {
    if (grepl("^\\d{1,2}:\\d{2}:\\d{2}$", time_str)) {
      today <- as.Date(now())
      return(ymd_hms(paste(today, time_str)))
    }
    NULL
  }
  
  # 滑动条调整时间
  observeEvent(input$time_range, {
    start_time <- parseTime(input$start_time)
    end_time <- parseTime(input$end_time)
    
    if (!is.null(start_time) && !is.null(end_time)) {
      if (input$lock_type == "start") {
        # 锁定开始时间，调整结束时间
        new_end_time <- start_time + minutes(input$time_range)
        updateTextInput(session, "end_time", value = format(new_end_time, "%H:%M:%S"))
      } else {
        # 锁定结束时间，调整开始时间
        new_start_time <- end_time - minutes(input$time_range)
        updateTextInput(session, "start_time", value = format(new_start_time, "%H:%M:%S"))
      }
    }
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
  
  # 显示时间范围文本
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
  
  # 交易时间段设置
  observeEvent(input$set_morning, {
    updateTextInput(session, "start_time", value = "09:00:00")
    updateTextInput(session, "end_time", value = "12:00:00")
    updateSliderInput(session, "time_range", value = 180)
  })
  
  observeEvent(input$set_afternoon, {
    updateTextInput(session, "start_time", value = "13:00:00")
    updateTextInput(session, "end_time", value = "15:00:00")
    updateSliderInput(session, "time_range", value = 120)
  })
  
  observeEvent(input$set_night, {
    updateTextInput(session, "start_time", value = "21:00:00")
    updateTextInput(session, "end_time", value = "23:00:00")
    updateSliderInput(session, "time_range", value = 120)
  })
  
  observeEvent(input$set_full_day, {
    updateTextInput(session, "start_time", value = "09:00:00")
    updateTextInput(session, "end_time", value = "15:00:00")
    updateSliderInput(session, "time_range", value = 360)
  })
  
  # 快速设置
  observeEvent(input$set_1h, { updateSliderInput(session, "time_range", value = 60) })
  observeEvent(input$set_2h, { updateSliderInput(session, "time_range", value = 120) })
  observeEvent(input$set_3h, { updateSliderInput(session, "time_range", value = 180) })
  
  # 数据查询
  priceData <- eventReactive(input$update, {
    req(input$tbl, input$start_time, input$end_time)
    
    sql <- sprintf("select * from %s where UpdateTime between '%s' and '%s'", 
                   input$tbl, input$start_time, input$end_time)
    
    sqlText(sql)
    
    tryCatch({
      result <- ds()(sql)
      if (is.null(result) || nrow(result) == 0) {
        sqlText(paste(sql, "\n\n查询结果为空"))
        return(NULL)
      }
      result
    }, error = function(e) {
      sqlText(paste("SQL错误:", e$message))
      NULL
    })
  })
  
  volData <- eventReactive(input$update, {
    req(input$tbl, input$start_time, input$end_time)
    
    sql <- sprintf(
      "select Id, LastPrice, Volume, Volume - lag(Volume) over(order by Id) as Vol, UpdateTime from %s where UpdateTime between '%s' and '%s'", 
      input$tbl, input$start_time, input$end_time
    )
    
    tryCatch({ ds()(sql) }, error = function(e) { NULL })
  })
  
  output$priceSql <- renderText({ sqlText() })
  
  # 价格趋势图 - 优化计算效率
  output$pricePlot <- renderPlot({
    df <- priceData()
    req(df, nrow(df) > 0)
    
    tryCatch({
      # 预先计算回归模型
      lm_model <- lm(LastPrice ~ Id, data = df)
      lm_coef <- coef(lm_model)
      
      with(df, {
        ggplot(df, aes(Id, LastPrice)) + 
          geom_point(alpha = 0.1, color = "#3498db", size = 0.5) + 
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
    df <- volData()
    req(df, nrow(df) > 0, input$nlev > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    tryCatch({
      xs <- df_clean |> 
        mutate(Level = cut(Vol, input$nlev)) |> 
        select(LastPrice, Level) |> 
        table() |> 
        prop.table() |> 
        addmargins() 
      
      attr(xs, "dimnames")$Level <- gsub("e\\+03", "K", attr(xs, "dimnames")$Level)
      xs <- round(xs, 3)
      
      plot_data <- as.data.frame(xs) %>%
        filter(Level != "Sum" & LastPrice != "Sum") %>%
        mutate(LastPrice = as.numeric(as.character(LastPrice)),
               Frequency = ifelse(Freq > 0, Freq, NA))
      
      ggplot(plot_data, aes(x = LastPrice, y = Frequency, fill = Level)) +
        geom_col(position = "stack", alpha = 0.8, width = 0.8) +
        scale_fill_brewer(palette = "Set3") +
        labs(title = NULL, x = "价格", y = "频率") +
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
    df <- volData()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    data.frame(
      统计项 = c("均值", "最大值", "最小值", "标准差"),
      数值 = round(c(mean(df_clean$Vol), max(df_clean$Vol), 
                   min(df_clean$Vol), sd(df_clean$Vol)), 2)
    )
  }, bordered = TRUE, align = 'c', width = "100%")
  
  output$volSummary <- renderPrint({
    df <- volData()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    cat("价格档位:", length(unique(df_clean$LastPrice)), "\n")
    cat("成交量组:", input$nlev, "\n")
    cat("观测数:", nrow(df_clean))
  })
  
  output$volTable2 <- renderTable({
    df <- volData()
    req(df, nrow(df) > 0, input$nlev > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    tryCatch({
      xs <- df_clean |> 
        mutate(Level = cut(Vol, input$nlev)) |> 
        select(LastPrice, Level) |> 
        table() |> 
        prop.table() |> 
        addmargins() 
      
      attr(xs, "dimnames")$Level <- gsub("e\\+03", "K", attr(xs, "dimnames")$Level)
      xs <- round(xs, 3)
      
      as.data.frame(xs) %>%
        pivot_wider(names_from = Level, values_from = Freq) %>%
        arrange(LastPrice)
      
    }, error = function(e) {
      data.frame(错误 = paste("表格错误:", e$message))
    })
  }, bordered = TRUE, digits = 3, width = "100%")
  
  output$dataSummary <- renderPrint({
    df <- priceData()
    req(df)
    
    cat("数据概览\n")
    cat("记录数:", nrow(df), "\n")
    cat("选择时间:", input$start_time, "至", input$end_time, "\n")
    cat("实际时间:", format(min(df$UpdateTime)), "至", format(max(df$UpdateTime)), "\n")
    cat("价格: 均值", round(mean(df$LastPrice), 2), 
        "标准差", round(sd(df$LastPrice), 2))
    
    vol_df <- volData()
    if(!is.null(vol_df) && nrow(vol_df) > 0 && "Vol" %in% names(vol_df)) {
      vol_clean <- vol_df[!is.na(vol_df$Vol), ]
      cat("\n成交量: 均值", round(mean(vol_clean$Vol), 2), 
          "标准差", round(sd(vol_clean$Vol), 2))
    }
  })
  
  output$dataPreview <- renderTable({
    df <- priceData()
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
