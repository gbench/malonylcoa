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
      .well-panel{padding:10px;margin-bottom:10px;background:#f8f9fa;border:1px solid #dee2e6;border-radius:6px;}
      .sql-display{padding:8px;margin:8px 0;background:#e9ecef;border:1px solid #ced4da;border-radius:4px;font-family:monospace;}
      .stat-card{padding:8px;margin:6px 0;background:white;border:1px solid #dee2e6;border-radius:4px;}
      .compact-row{margin-bottom:8px;}
      .compact-row .form-group{margin-bottom:8px;}
      .inline-group{display:flex;gap:10px;align-items:flex-end;}
      .inline-group .form-group{flex:1;margin-bottom:0;}
    "))
  ),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      div(class = "well-panel",
          actionButton("update", "更新数据", class = "btn-primary", width = "100%"),
          br(), actionButton("close", "关闭应用", class = "btn-danger", width = "100%")
      ),
      
      div(class = "well-panel",
          h4("📊 数据源"),
          div(class = "compact-row",
              textInput("tbl", "数据表", value = "t_rb2601_20251117", width = "100%")
          ),
          div(class = "inline-group",
              textInput("db", "数据库", value = "ctp", width = "100%"),
              numericInput("port", "端口", value = 3371, width = "100%"),
              textInput("host", "主机", value = "127.0.0.1", width = "100%")
          )
      ),
      
      div(class = "well-panel",
          h4("📈 价格参数"),
          div(class = "inline-group",
              numericInput("n", "时间点数", value = 10, min = 5, max = 50, width = "100%"),
              numericInput("span", "时长(分)", value = 30, min = 1, max = 120, width = "100%")
          )
      ),
      
      div(class = "well-panel",
          h4("📊 成交量参数"),
          div(class = "compact-row",
              numericInput("nlev", "分组数", value = 3, min = 2, max = 10, width = "100%")
          )
      )
    ),
    
    mainPanel(
      width = 9,
      tabsetPanel(
        tabPanel("📈 价格趋势", 
                 plotOutput("pricePlot", height = "500px"),
                 h4("SQL查询:"),
                 verbatimTextOutput("priceSql")
        ),
        
        tabPanel("📊 成交量分析",
                 plotOutput("volPlot", height = "400px"),
                 fluidRow(
                   column(6, tableOutput("volTable")),
                   column(6, verbatimTextOutput("volSummary"))
                 ),
                 tableOutput("volTable2")
        ),
        
        tabPanel("📋 数据概览",
                 verbatimTextOutput("dataSummary"),
                 h5("数据预览 (前10行)"),
                 tableOutput("dataPreview")
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
  
  priceData <- eventReactive(input$update, {
    req(input$tbl, input$span)
    
    sql <- sprintf("select * from %s where UpdateTime>'%s'", 
                   input$tbl, 
                   format(now() - minutes(input$span), "%H:%M:%S"))
    
    sqlText(sql)
    
    tryCatch({
      ds()(sql)
    }, error = function(e) {
      sqlText(paste("SQL错误:", e$message))
      NULL
    })
  })
  
  volData <- eventReactive(input$update, {
    req(input$tbl)
    
    sql <- sprintf(
      "select Id, LastPrice, Volume, Volume - lag(Volume) over(order by Id) as Vol, UpdateTime from %s", 
      input$tbl
    )
    
    tryCatch({ ds()(sql) }, error = function(e) { NULL })
  })
  
  output$priceSql <- renderText({ sqlText() })
  
  output$pricePlot <- renderPlot({
    df <- priceData()
    req(df, nrow(df) > 0)
    
    tryCatch({
      with(df, {
        p <- ggplot(df, aes(Id, LastPrice)) + 
          geom_point(alpha = 0.01, color = "#3498db") + 
          geom_smooth(color = "#e74c3c", se = TRUE) +
          geom_hline(yintercept = mean(LastPrice) + c(-2, 0, 2) * sd(LastPrice),
                     linewidth = c(1.5, 1, 1.5), color = c("#c0392b", "#95a5a6", "#e74c3c")) +
          geom_abline(slope = coef(lm(LastPrice ~ Id))[2], 
                      intercept = coef(lm(LastPrice ~ Id))[1], 
                      linewidth = 1.5, color = "#9b59b6") +
          scale_x_continuous(
            breaks = seq(min(Id), max(Id), length.out = input$n), 
            labels = function(x) UpdateTime[x - min(Id) + 1]
          ) +
          labs(title = "价格趋势分析", x = "时间", y = "价格") +
          theme_minimal()
        print(p)
      })
    }, error = function(e) {
      plot(1, 1, type = "n", main = "绘图错误")
      text(1, 1, paste("错误:", e$message), col = "red")
    })
  })
  
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
        geom_col(position = "stack", alpha = 0.8) +
        scale_fill_brewer(palette = "Set3") +
        labs(title = "价格档位 vs 成交量分布", x = "价格", y = "频率") +
        theme_minimal() +
        theme(axis.text.x = element_text(angle = 45, hjust = 1))
      
    }, error = function(e) {
      plot(1, 1, type = "n", main = "分析错误")
      text(1, 1, paste("错误:", e$message), col = "red")
    })
  })
  
  output$volTable <- renderTable({
    df <- volData()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    data.frame(
      统计量 = c("均值", "最大值", "最小值", "标准差"),
      数值 = round(c(mean(df_clean$Vol), max(df_clean$Vol), 
                   min(df_clean$Vol), sd(df_clean$Vol)), 2)
    )
  }, bordered = TRUE)
  
  output$volSummary <- renderPrint({
    df <- volData()
    req(df, nrow(df) > 0)
    
    df_clean <- df[!is.na(df$Vol), ]
    
    cat("分布统计:\n")
    cat("价格档位:", length(unique(df_clean$LastPrice)), "\n")
    cat("成交量组:", input$nlev, "\n")
    cat("观测数:", nrow(df_clean), "\n")
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
      data.frame(Error = paste("错误:", e$message))
    })
  }, bordered = TRUE, digits = 3)
  
  output$dataSummary <- renderPrint({
    df <- priceData()
    req(df)
    
    cat("数据概览\n")
    cat("记录数:", nrow(df), "\n")
    cat("时间:", format(min(df$UpdateTime)), "至", format(max(df$UpdateTime)), "\n")
    cat("价格: 均值", round(mean(df$LastPrice), 2), 
        "标准差", round(sd(df$LastPrice), 2), "\n")
    
    vol_df <- volData()
    if(!is.null(vol_df) && nrow(vol_df) > 0 && "Vol" %in% names(vol_df)) {
      vol_clean <- vol_df[!is.na(vol_df$Vol), ]
      cat("成交量: 均值", round(mean(vol_clean$Vol), 2), 
          "标准差", round(sd(vol_clean$Vol), 2), "\n")
    }
  })
  
  # 修复数据预览
  output$dataPreview <- renderTable({
    df <- priceData()
    req(df)
    
    # 安全地选择列并处理数据
    result <- head(df, 10)
    
    # 只选择数值和字符列，避免复杂对象
    simple_cols <- names(result)[sapply(result, function(x) is.numeric(x) | is.character(x))]
    result <- result[simple_cols]
    
    # 安全格式化时间列
    if("UpdateTime" %in% names(result)) {
      if(inherits(result$UpdateTime, "POSIXt")) {
        result$UpdateTime <- format(result$UpdateTime, "%H:%M:%S")
      } else if(is.character(result$UpdateTime)) {
        # 如果是字符，提取时间部分
        result$UpdateTime <- substr(result$UpdateTime, 1, 8)
      }
    }
    
    result
  }, bordered = TRUE)
  
  observeEvent(input$close, {
    showModal(modalDialog(
      "确定关闭应用吗？",
      footer = tagList(
        actionButton("confirm_close", "确定", class = "btn-danger"),
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
