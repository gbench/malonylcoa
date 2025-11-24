library(shiny)
library(dplyr)
library(ggplot2)
library(purrr)
library(lubridate)
library(jsonlite)
library(zoo)

# UI 部分
ui <- fluidPage(
  # 添加自定义CSS美化界面
  tags$head(
    tags$title("CTP 数据可视化分析平台"),
    tags$style(HTML("
      .well {
        background-color: #f8f9fa;
        border: 1px solid #e9ecef;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }
      .nav-tabs > li > a {
        color: #495057;
        font-weight: 500;
      }
      .nav-tabs > li.active > a {
        color: #007bff;
        border-bottom: 2px solid #007bff;
      }
      .btn-primary {
        background-color: #007bff;
        border-color: #007bff;
      }
      .btn-primary:hover {
        background-color: #0056b3;
        border-color: #0056b3;
      }
      h4 {
        color: #2c3e50;
        border-bottom: 2px solid #3498db;
        padding-bottom: 8px;
        margin-top: 20px;
      }
      .shiny-input-container {
        margin-bottom: 12px;
      }
    "))
  ),
  
  titlePanel(
    div(
      style = "text-align: center;",
      h1("CTP 数据可视化分析平台", style = "color: #2c3e50; margin-bottom: 5px;"),
      h4("实时价格监控与趋势分析", style = "color: #7f8c8d; font-weight: normal;")
    )
  ),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      style = "background-color: #f8f9fa; border-radius: 8px; padding: 20px;",
      
      # 输入控件使用tab分组
      tabsetPanel(
        id = "input_tabs",
        type = "tabs",
        
        # 数据库设置标签页
        tabPanel(
          "数据库设置",
          icon = icon("database"),
          wellPanel(
            style = "background-color: white;",
            # Host 输入模式切换
            checkboxInput("host_mode", "使用预设服务器", value = TRUE),
            
            # 条件面板：根据模式显示不同的 host 输入控件
            conditionalPanel(
              condition = "input.host_mode == true",
              selectInput("host_select", "选择服务器", 
                          choices = c("10服务器" = "192.168.1.10", 
                                      "04服务器" = "192.168.1.4", 
                                      "本地主机" = "127.0.0.1"),
                          selected = "127.0.0.1")
            ),
            conditionalPanel(
              condition = "input.host_mode == false",
              textInput("host_text", "自定义主机地址", value = "192.168.1.10")
            ),
            
            numericInput("port", "数据库端口", value = 3371, min = 1, max = 65535),
            textInput("tbl", "数据表名", value = "t_ma601_20251119",
                      placeholder = "输入数据表名称")
          )
        ),
        
        # 时间设置标签页
        tabPanel(
          "时间设置",
          icon = icon("clock"),
          wellPanel(
            style = "background-color: white;",
            # 互斥的焦点时间选择
            radioButtons("focus_time", "焦点时点", 
                         choices = c("开始时间" = "start", "结束时间" = "end"),
                         selected = "end", inline = TRUE),
            
            uiOutput("time_controls"),
            sliderInput("time_range", "时间间隔 (分钟)", 
                        min = 5, max = 600, value = 120, step = 5),
            actionButton("reset_time", "重置时间为当前时间", 
                         icon = icon("sync"), class = "btn-info")
          )
        ),
        
        # 图表设置标签页
        tabPanel(
          "图表设置",
          icon = icon("chart-line"),
          wellPanel(
            style = "background-color: white;",
            h5("移动平均线设置"),
            checkboxGroupInput("ma_periods", "选择移动平均周期",
                               choices = c("MA3" = 3, "MA5" = 5, "MA10" = 10, 
                                           "MA15" = 15, "MA30" = 30, "MA60" = 60),
                               selected = c(5, 10, 30),
                               inline = TRUE),
            
            h5("刷新设置"),
            radioButtons("refresh_mode", "刷新模式",
                         choices = c("手动刷新" = "manual", "自动刷新" = "auto"),
                         selected = "manual", inline = TRUE),
            
            # 刷新间隔设置
            conditionalPanel(
              condition = "input.refresh_mode == 'auto'",
              numericInput("refresh_interval", "刷新间隔 (秒)", 
                           value = 5, min = 1, max = 60, step = 1)
            ),
            
            conditionalPanel(
              condition = "input.refresh_mode == 'manual'",
              actionButton("refresh_btn", "手动刷新数据", 
                           icon = icon("download"), 
                           class = "btn-primary btn-block")
            ),
            conditionalPanel(
              condition = "input.refresh_mode == 'auto'",
              div(style = "text-align: center; color: #28a745; font-weight: bold;",
                  icon("sync"), "自动刷新中...")
            )
          )
        )
      )
    ),
    
    mainPanel(
      width = 9,
      # 内容标签页 - 默认显示加权平均图
      tabsetPanel(
        id = "main_tabs",
        type = "tabs",
        
        # 加权平均图标签页（默认）
        tabPanel(
          "加权平均图",
          icon = icon("chart-line"),
          wellPanel(
            style = "background-color: white; min-height: 600px;",
            plotOutput("price_plot", height = "550px")
          )
        ),
        
        # K线图标签页
        tabPanel(
          "K线图",
          icon = icon("chart-line"),
          wellPanel(
            style = "background-color: white; min-height: 600px;",
            plotOutput("kline_plot", height = "550px")
          )
        ),
        
        # K线数据标签页
        tabPanel(
          "K线数据",
          icon = icon("table"),
          wellPanel(
            style = "background-color: white; min-height: 600px;",
            div(style = "margin-bottom: 15px;",
                downloadButton("download_data", "下载数据", class = "btn-success")
            ),
            DT::dataTableOutput("kline_table")
          )
        )
      ),
      
      # 状态信息栏
      fluidRow(
        column(6,
               wellPanel(
                 style = "background-color: #e8f4f8; text-align: center;",
                 h5(icon("server"), "当前连接"),
                 textOutput("current_connection")
               )
        ),
        column(6,
               wellPanel(
                 style = "background-color: #e8f6f3; text-align: center;",
                 h5(icon("chart-line"), "数据状态"),
                 textOutput("data_status")
               )
        )
      )
    )
  )
)

# Server 部分
server <- function(input, output, session) {
  
  # 导入基础函数
  batch_load()
  
  # 响应式获取 host 值
  current_host <- reactive({
    if (input$host_mode) {
      # 使用下拉列表选择的值
      input$host_select
    } else {
      # 使用文本框输入的值
      input$host_text
    }
  })
  
  # 响应式创建 ds10ctp 函数，根据输入的 host 和 port
  ds10ctp <- reactive({
    req(current_host(), input$port)
    partial(sqldframe, dbname = "ctp", host = current_host(), port = input$port)
  })
  
  # 初始化时间值 - 设置初始值
  start_time <- reactiveVal({
    # 默认开始时间：当前时间前2小时
    format(Sys.time() - hours(2), "%H:%M")
  })
  
  end_time <- reactiveVal({
    # 默认结束时间：当前时间
    format(Sys.time(), "%H:%M")
  })
  
  # 响应式时间控制UI
  output$time_controls <- renderUI({
    tagList(
      fluidRow(
        column(6, 
               textInput("startime", "开始时间 (HH:MM)", 
                         value = start_time())
        ),
        column(6,
               textInput("endtime", "结束时间 (HH:MM)", 
                         value = end_time())
        )
      )
    )
  })
  
  # 解析时间字符串为分钟数（用于计算）
  parse_time_to_minutes <- function(time_str) {
    tryCatch({
      parts <- strsplit(time_str, ":")[[1]]
      if (length(parts) == 2) {
        as.numeric(parts[1]) * 60 + as.numeric(parts[2])
      } else {
        NULL
      }
    }, error = function(e) {
      NULL
    })
  }
  
  # 格式化分钟数为时间字符串
  format_minutes_to_time <- function(total_minutes) {
    hours <- floor(total_minutes / 60)
    minutes <- total_minutes %% 60
    sprintf("%02d:%02d", hours, minutes)
  }
  
  # 获取当前焦点时间（分钟数）
  get_focus_time_minutes <- reactive({
    req(input$focus_time, input$startime, input$endtime)
    
    if (input$focus_time == "start") {
      parse_time_to_minutes(input$startime)
    } else {
      parse_time_to_minutes(input$endtime)
    }
  })
  
  # 观察焦点时间输入变化，自动调整另一个时间
  observeEvent(input$startime, {
    if (input$focus_time == "start") {
      # 当焦点是开始时间且开始时间变化时，自动更新结束时间
      start_minutes <- parse_time_to_minutes(input$startime)
      if (!is.null(start_minutes)) {
        new_end_minutes <- start_minutes + input$time_range
        if (new_end_minutes >= 24 * 60) new_end_minutes <- 24 * 60 - 1
        new_end_time <- format_minutes_to_time(new_end_minutes)
        updateTextInput(session, "endtime", value = new_end_time)
        end_time(new_end_time)
      }
    }
  })
  
  observeEvent(input$endtime, {
    if (input$focus_time == "end") {
      # 当焦点是结束时间且结束时间变化时，自动更新开始时间
      end_minutes <- parse_time_to_minutes(input$endtime)
      if (!is.null(end_minutes)) {
        new_start_minutes <- end_minutes - input$time_range
        if (new_start_minutes < 0) new_start_minutes <- 0
        new_start_time <- format_minutes_to_time(new_start_minutes)
        updateTextInput(session, "startime", value = new_start_time)
        start_time(new_start_time)
      }
    }
  })
  
  # 更新时间范围 - 当时间间隔变化时调整非焦点时间
  observeEvent(input$time_range, {
    focus_minutes <- get_focus_time_minutes()
    if (!is.null(focus_minutes)) {
      minutes_diff <- input$time_range
      
      if (input$focus_time == "start") {
        # 焦点是开始时间，调整结束时间
        new_end_minutes <- focus_minutes + minutes_diff
        # 处理跨天情况
        if (new_end_minutes >= 24 * 60) new_end_minutes <- 24 * 60 - 1
        new_end_time <- format_minutes_to_time(new_end_minutes)
        updateTextInput(session, "endtime", value = new_end_time)
        end_time(new_end_time)
      } else {
        # 焦点是结束时间，调整开始时间
        new_start_minutes <- focus_minutes - minutes_diff
        # 处理跨天情况
        if (new_start_minutes < 0) new_start_minutes <- 0
        new_start_time <- format_minutes_to_time(new_start_minutes)
        updateTextInput(session, "startime", value = new_start_time)
        start_time(new_start_time)
      }
    }
  })
  
  # 当焦点时间改变时，自动调整非焦点时间
  observeEvent(input$focus_time, {
    focus_minutes <- get_focus_time_minutes()
    if (!is.null(focus_minutes)) {
      minutes_diff <- input$time_range
      
      if (input$focus_time == "start") {
        # 切换到开始时间作为焦点，调整结束时间
        new_end_minutes <- focus_minutes + minutes_diff
        if (new_end_minutes >= 24 * 60) new_end_minutes <- 24 * 60 - 1
        new_end_time <- format_minutes_to_time(new_end_minutes)
        updateTextInput(session, "endtime", value = new_end_time)
        end_time(new_end_time)
      } else {
        # 切换到结束时间作为焦点，调整开始时间
        new_start_minutes <- focus_minutes - minutes_diff
        if (new_start_minutes < 0) new_start_minutes <- 0
        new_start_time <- format_minutes_to_time(new_start_minutes)
        updateTextInput(session, "startime", value = new_start_time)
        start_time(new_start_time)
      }
    }
  })
  
  # 重置时间按钮
  observeEvent(input$reset_time, {
    current_time <- format(Sys.time(), "%H:%M")
    current_minutes <- parse_time_to_minutes(current_time)
    
    if (input$focus_time == "end") {
      # 焦点是结束时间，设置结束时间为当前时间，开始时间自动调整
      new_start_minutes <- current_minutes - input$time_range
      if (new_start_minutes < 0) new_start_minutes <- 0
      
      new_start_time <- format_minutes_to_time(new_start_minutes)
      updateTextInput(session, "startime", value = new_start_time)
      updateTextInput(session, "endtime", value = current_time)
      
      start_time(new_start_time)
      end_time(current_time)
    } else {
      # 焦点是开始时间，设置开始时间为当前时间，结束时间自动调整
      new_end_minutes <- current_minutes + input$time_range
      if (new_end_minutes >= 24 * 60) new_end_minutes <- 24 * 60 - 1
      
      new_end_time <- format_minutes_to_time(new_end_minutes)
      updateTextInput(session, "startime", value = current_time)
      updateTextInput(session, "endtime", value = new_end_time)
      
      start_time(current_time)
      end_time(new_end_time)
    }
  })
  
  # 获取数据
  get_data <- reactive({
    # 自动刷新模式下的依赖
    if (input$refresh_mode == "auto") {
      # 使用用户设置的刷新间隔
      refresh_interval_ms <- input$refresh_interval * 1000
      invalidateLater(refresh_interval_ms)
    } else {
      input$refresh_btn  # 手动刷新依赖
    }
    
    # 验证输入
    req(input$tbl, input$startime, input$endtime, current_host(), input$port)
    
    # 使用 record.builder 的正确方式构建参数（使用占位符）
    tryCatch({
      # 使用 NSE 风格构建参数列表，使用占位符
      rb <- record.builder("##tbl" = character, "#startime" = character, "#endtime" = character)
      params <- rb(input$tbl, input$startime, input$endtime)
      
      # 使用响应式的 ds10ctp 函数执行查询
      data <- ds10ctp()(params = params, "1min.kline.weighted", files = "sql")
      
      # 数据验证和清理
      if (!is.null(data) && nrow(data) > 0) {
        # 移除可能包含空值或NA的行
        data <- data %>%
          filter(!is.na(MinuteTime) & MinuteTime != "" &
                   !is.na(ClosePrice) & !is.na(OpenPrice) &
                   !is.na(HighPrice) & !is.na(LowPrice)) %>%
          arrange(MinuteTime) %>%
          mutate(
            TimeOrder = row_number()
          )
        
        # 动态计算选中的移动平均线
        if (nrow(data) > 0) {
          ma_periods <- as.numeric(input$ma_periods)
          for (period in ma_periods) {
            if (nrow(data) >= period) {
              ma_col_name <- paste0("MA", period)
              # 使用更安全的移动平均计算
              data[[ma_col_name]] <- tryCatch({
                rollmean(data$ClosePrice, period, fill = NA, align = "right")
              }, error = function(e) {
                rep(NA, nrow(data))
              })
            }
          }
        }
      }
      
      data
      
    }, error = function(e) {
      showNotification(paste("数据查询错误:", e$message), type = "error")
      return(NULL)
    })
  })
  
  # 当前连接状态
  output$current_connection <- renderText({
    paste(current_host(), ":", input$port)
  })
  
  # 数据状态
  output$data_status <- renderText({
    data <- get_data()
    if (is.null(data)) {
      "等待数据加载..."
    } else if (nrow(data) == 0) {
      "无有效数据"
    } else {
      paste("已加载", nrow(data), "条有效记录")
    }
  })
  
  # 加权平均图（原来的图表，作为默认显示）
  output$price_plot <- renderPlot({
    data <- get_data()
    
    if (is.null(data) || nrow(data) == 0) {
      return(
        ggplot() + 
          geom_text(aes(x = 0.5, y = 0.5, label = "无数据或查询失败"), 
                    size = 8, color = "gray50") +
          labs(title = "数据加载中...") +
          theme_void() +
          theme(plot.title = element_text(hjust = 0.5, size = 16, color = "gray50"))
      )
    }
    
    # 数据处理 - 确保数据有效
    data_processed <- data |>
      filter(!is.na(TimeOrder) & !is.na(ClosePrice)) |>
      mutate(
        TimeLabel = MinuteTime
      )
    
    # 绘制图表 - 使用顺序编号作为x轴，但显示时间标签
    p <- ggplot(data_processed, aes(x = TimeOrder)) +
      geom_line(aes(y = ClosePrice, group = 1), color = "#2c3e50", linewidth = 0.8) +
      geom_line(aes(y = WeightedAvgPrice, group = 1), color = "#e74c3c", linewidth = 1) +
      geom_line(aes(y = HighPrice, group = 1), color = "#9b59b6", alpha = 0.7) +
      geom_line(aes(y = LowPrice, group = 1), color = "#3498db", alpha = 0.7) +
      geom_smooth(aes(y = WeightedAvgPrice), color = "#2980b9", 
                  method = "loess", se = FALSE, linewidth = 1.2) +
      labs(
        title = paste("价格走势分析 -", input$tbl),
        subtitle = paste("数据时间范围:", input$startime, "-", input$endtime),
        x = "时间",
        y = "价格",
        color = "价格类型",
        caption = paste("服务器:", current_host(), "端口:", input$port,
                        if(input$refresh_mode == "auto") paste("| 刷新间隔:", input$refresh_interval, "秒"))
      ) +
      theme_minimal() +
      theme(
        plot.title = element_text(hjust = 0.5, size = 18, face = "bold", color = "#2c3e50"),
        plot.subtitle = element_text(hjust = 0.5, size = 12, color = "#7f8c8d"),
        plot.caption = element_text(hjust = 1, size = 10, color = "#95a5a6"),
        axis.text.x = element_text(angle = 45, hjust = 1, color = "#2c3e50"),
        axis.text.y = element_text(color = "#2c3e50"),
        axis.title = element_text(color = "#2c3e50"),
        panel.grid.major = element_line(color = "#ecf0f1"),
        panel.grid.minor = element_blank(),
        plot.background = element_rect(fill = "white", color = NA)
      )
    
    # 添加移动平均线到加权平均图
    ma_periods <- as.numeric(input$ma_periods)
    ma_colors <- c("#3498db", "#9b59b6", "#e74c3c", "#f39c12", "#1abc9c", "#34495e")
    
    for (i in seq_along(ma_periods)) {
      period <- ma_periods[i]
      ma_col_name <- paste0("MA", period)
      if (ma_col_name %in% names(data_processed)) {
        # 确保移动平均线数据有效
        valid_ma_data <- data_processed[!is.na(data_processed[[ma_col_name]]), ]
        if (nrow(valid_ma_data) > 0) {
          p <- p + geom_line(data = valid_ma_data, aes_string(y = ma_col_name), 
                             color = ma_colors[i], 
                             linewidth = 0.8, 
                             alpha = 0.8,
                             linetype = "solid")
        }
      }
    }
    
    # 设置x轴刻度
    if (nrow(data_processed) > 0) {
      p <- p + scale_x_continuous(
        breaks = function(x) {
          seq(1, nrow(data_processed), by = max(1, floor(nrow(data_processed) / 10)))
        },
        labels = function(x) {
          idx <- round(x)
          ifelse(idx >= 1 & idx <= nrow(data_processed), data_processed$TimeLabel[idx], "")
        }
      )
    }
    
    p
  })
  
  # K线图
  output$kline_plot <- renderPlot({
    data <- get_data()
    
    if (is.null(data) || nrow(data) == 0) {
      return(
        ggplot() + 
          geom_text(aes(x = 0.5, y = 0.5, label = "无数据或查询失败"), 
                    size = 8, color = "gray50") +
          labs(title = "数据加载中...") +
          theme_void() +
          theme(plot.title = element_text(hjust = 0.5, size = 16, color = "gray50"))
      )
    }
    
    # 数据验证
    valid_data <- data %>%
      filter(!is.na(TimeOrder) & !is.na(OpenPrice) & !is.na(ClosePrice) &
               !is.na(HighPrice) & !is.na(LowPrice))
    
    if (nrow(valid_data) == 0) {
      return(
        ggplot() + 
          geom_text(aes(x = 0.5, y = 0.5, label = "无有效K线数据"), 
                    size = 8, color = "gray50") +
          theme_void()
      )
    }
    
    # 创建K线图
    p <- ggplot(valid_data, aes(x = TimeOrder)) +
      # K线主体
      geom_segment(aes(xend = TimeOrder, y = LowPrice, yend = HighPrice), 
                   color = "#2c3e50", linewidth = 0.8) +
      # K线实体（上涨为绿色，下跌为红色）
      geom_rect(aes(xmin = TimeOrder - 0.3, xmax = TimeOrder + 0.3,
                    ymin = pmin(OpenPrice, ClosePrice), 
                    ymax = pmax(OpenPrice, ClosePrice),
                    fill = ClosePrice >= OpenPrice)) +
      scale_fill_manual(values = c("TRUE" = "#2ecc71", "FALSE" = "#e74c3c"), 
                        guide = "none") +
      # 加权平均价线
      geom_line(aes(y = WeightedAvgPrice), color = "#f39c12", linewidth = 1, alpha = 0.8) +
      labs(
        title = paste("K线图 -", input$tbl),
        subtitle = paste("数据时间范围:", input$startime, "-", input$endtime),
        x = "时间",
        y = "价格",
        caption = paste("服务器:", current_host(), "端口:", input$port,
                        if(input$refresh_mode == "auto") paste("| 刷新间隔:", input$refresh_interval, "秒"))
      ) +
      theme_minimal() +
      theme(
        plot.title = element_text(hjust = 0.5, size = 18, face = "bold", color = "#2c3e50"),
        plot.subtitle = element_text(hjust = 0.5, size = 12, color = "#7f8c8d"),
        plot.caption = element_text(hjust = 1, size = 10, color = "#95a5a6"),
        axis.text.x = element_text(angle = 45, hjust = 1, color = "#2c3e50"),
        axis.text.y = element_text(color = "#2c3e50"),
        axis.title = element_text(color = "#2c3e50"),
        panel.grid.major = element_line(color = "#ecf0f1"),
        panel.grid.minor = element_blank(),
        plot.background = element_rect(fill = "white", color = NA)
      )
    
    # 添加移动平均线到K线图
    ma_periods <- as.numeric(input$ma_periods)
    ma_colors <- c("#3498db", "#9b59b6", "#e74c3c", "#f39c12", "#1abc9c", "#34495e")
    
    for (i in seq_along(ma_periods)) {
      period <- ma_periods[i]
      ma_col_name <- paste0("MA", period)
      if (ma_col_name %in% names(valid_data)) {
        # 确保移动平均线数据有效
        valid_ma_data <- valid_data[!is.na(valid_data[[ma_col_name]]), ]
        if (nrow(valid_ma_data) > 0) {
          p <- p + geom_line(data = valid_ma_data, aes_string(y = ma_col_name), 
                             color = ma_colors[i], 
                             linewidth = 0.8, 
                             alpha = 0.8,
                             linetype = "solid")
        }
      }
    }
    
    # 设置x轴刻度
    if (nrow(valid_data) > 0) {
      p <- p + scale_x_continuous(
        breaks = function(x) {
          seq(1, nrow(valid_data), by = max(1, floor(nrow(valid_data) / 10)))
        },
        labels = function(x) {
          idx <- round(x)
          ifelse(idx >= 1 & idx <= nrow(valid_data), valid_data$MinuteTime[idx], "")
        }
      )
    }
    
    p
  })
  
  # K线数据表格
  output$kline_table <- DT::renderDataTable({
    data <- get_data()
    if (is.null(data) || nrow(data) == 0) {
      return(DT::datatable(data.frame(Message = "无数据")))
    }
    
    # 选择要显示的列
    display_data <- data %>%
      select(MinuteTime, OpenPrice, HighPrice, LowPrice, ClosePrice, 
             WeightedAvgPrice, MinuteVolume, TradeCount)
    
    DT::datatable(
      display_data,
      options = list(
        pageLength = 10,
        scrollX = TRUE,
        autoWidth = TRUE,
        dom = 'Bfrtip',
        buttons = c('copy', 'csv', 'excel')
      ),
      rownames = FALSE,
      filter = 'top'
    )
  })
  
  # 下载数据
  output$download_data <- downloadHandler(
    filename = function() {
      paste0("kline_data_", input$tbl, "_", Sys.Date(), ".csv")
    },
    content = function(file) {
      data <- get_data()
      if (!is.null(data) && nrow(data) > 0) {
        write.csv(data, file, row.names = FALSE)
      }
    }
  )
}

# 运行应用
shinyApp(ui, server)
