# 加载必要的包
library(shiny)
library(ggplot2)
library(dplyr)
library(plotly)
library(DT)
library(tidyr)

# 工作区域设置
setwd("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/ecodata/cn/政收入和财政支出")

# 数据
data <- read.csv("data.csv")

min_year <- min(data$年份)
max_year <- max(data$年份)

# UI
ui <- fluidPage(
  titlePanel(gettextf("中国一般公共预算收支增长情况 (%s-%s)", min_year, max_year)),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      
      # 年份范围选择
      sliderInput("年份范围", 
                  "选择年份范围:",
                  min = min_year, 
                  max = max_year, 
                  value = c(min_year, max_year),
                  step = 1,
                  sep = ""),
      
      hr(),
      
      # 指标选择
      selectInput("指标类型", 
                  "选择图表类型:",
                  choices = c("收支金额对比" = "amount",
                              "收支增速对比" = "growth",
                              "占GDP比重对比" = "ratio")),
      
      # 图表样式选项
      conditionalPanel(
        condition = "input.指标类型 == 'amount'",
        checkboxInput("显示差异区域", "显示收支差异区域（填充）", value = TRUE)
      ),
      
      # 显示数据点选项
      checkboxInput("显示数据点", "显示数据点标记", value = TRUE),
      
      # 平滑曲线选项
      checkboxInput("平滑曲线", "显示平滑趋势线", value = FALSE),
      
      hr(),
      
      # 显示数据摘要
      h4("数据摘要"),
      verbatimTextOutput("data_summary"),
      
      hr(),
      
      # 数据来源说明
      p(em("数据来源：《中国财政年鉴》《中国统计年鉴》")),
      p(em("注：财政收入中不包括国内外债务收入；从2000年起，财政支出中包括国内外债务付息支出。"))
    ),
    
    mainPanel(
      width = 9,
      
      tabsetPanel(
        tabPanel("动态趋势图", 
                 br(),
                 plotlyOutput("trend_plot", height = "550px"),
                 br(),
                 hr(),
                 h4("关键发现"),
                 verbatimTextOutput("insights")),
        
        tabPanel("3D 可视化", 
                 br(),
                 plotlyOutput("scatter3d", height = "550px")),
        
        tabPanel("数据表", 
                 br(),
                 DTOutput("data_table")),
        
        tabPanel("年度对比", 
                 br(),
                 fluidRow(
                   column(6, 
                          selectInput("year1", "选择基准年份:", 
                                      choices = min_year:max_year, selected = 2000)),
                   column(6, 
                          selectInput("year2", "选择对比年份:", 
                                      choices = min_year:max_year, selected = max_year))
                 ),
                 br(),
                 h4("对比结果"),
                 verbatimTextOutput("comparison"),
                 br(),
                 plotlyOutput("compare_plot", height = "400px"))
      )
    )
  )
)

# Server
server <- function(input, output, session) {
  
  # 筛选数据
  filtered_data <- reactive({
    data %>%
      filter(年份 >= input$年份范围[1], 年份 <= input$年份范围[2])
  })
  
  # 动态趋势图（增强版）
  output$trend_plot <- renderPlotly({
    df <- filtered_data()
    
    if(input$指标类型 == "amount") {
      # 收支金额对比图
      
      # 创建文本提示
      df$收入提示 <- paste0(
        "年份: ", df$年份, "<br>",
        "财政收入: ", format(round(df$财政收入, 0), big.mark = ","), "亿元<br>",
        "财政支出: ", format(round(df$财政支出, 0), big.mark = ","), "亿元<br>",
        "收支差额: ", format(round(df$财政支出 - df$财政收入, 0), big.mark = ","), "亿元<br>",
        "占GDP比重(收入): ", round(df$财政收入占比, 1), "%<br>",
        "占GDP比重(支出): ", round(df$财政支出占比, 1), "%"
      )
      
      p <- plot_ly() %>%
        add_trace(
          x = ~df$年份, y = ~df$财政收入,
          name = "财政收入",
          type = 'scatter', mode = if(input$显示数据点) 'lines+markers' else 'lines',
          line = list(color = '#2E86C1', width = 3),
          marker = list(size = 8, color = '#2E86C1', 
                        line = list(color = '#1B4F72', width = 1)),
          text = ~df$收入提示, hoverinfo = 'text',
          hovertemplate = paste('<b>%{text}</b><extra></extra>')
        ) %>%
        add_trace(
          x = ~df$年份, y = ~df$财政支出,
          name = "财政支出",
          type = 'scatter', mode = if(input$显示数据点) 'lines+markers' else 'lines',
          line = list(color = '#E74C3C', width = 3),
          marker = list(size = 8, color = '#E74C3C',
                        line = list(color = '#7B241C', width = 1)),
          text = ~df$收入提示, hoverinfo = 'text',
          hovertemplate = paste('<b>%{text}</b><extra></extra>')
        )
      
      # 添加差异区域填充
      if(input$显示差异区域) {
        df$差額 <- df$财政支出 - df$财政收入
        df$填充_收入 <- df$财政收入
        df$填充_支出 <- df$财政支出
        
        # 添加填充区域（支出大于收入的区域）
        fill_data <- df[df$差額 > 0, ]
        if(nrow(fill_data) > 0) {
          p <- p %>% add_trace(
            x = ~fill_data$年份,
            y = ~fill_data$财政支出,
            name = "赤字区域",
            type = 'scatter', mode = 'none',
            fill = 'tozeroy',
            fillcolor = 'rgba(231, 76, 60, 0.2)',
            line = list(color = 'rgba(0,0,0,0)'),
            showlegend = TRUE
          )
        }
      }
      
      # 添加平滑趋势线
      if(input$平滑曲线) {
        # 财政收入平滑线
        loess_revenue <- loess(财政收入 ~ 年份, data = df)
        df$平滑收入 <- predict(loess_revenue)
        
        # 财政支出平滑线
        loess_expense <- loess(财政支出 ~ 年份, data = df)
        df$平滑支出 <- predict(loess_expense)
        
        p <- p %>%
          add_trace(
            x = ~df$年份, y = ~df$平滑收入,
            name = "财政收入趋势",
            type = 'scatter', mode = 'lines',
            line = list(color = '#2E86C1', width = 1.5, dash = 'dash'),
            showlegend = TRUE,
            opacity = 0.6
          ) %>%
          add_trace(
            x = ~df$年份, y = ~df$平滑支出,
            name = "财政支出趋势",
            type = 'scatter', mode = 'lines',
            line = list(color = '#E74C3C', width = 1.5, dash = 'dash'),
            showlegend = TRUE,
            opacity = 0.6
          )
      }
      
      p <- p %>% layout(
        title = list(text = "一般公共预算收支金额趋势", font = list(size = 16)),
        xaxis = list(title = "年份", gridcolor = '#E5E5E5', showgrid = TRUE),
        yaxis = list(title = "金额（亿元）", gridcolor = '#E5E5E5', showgrid = TRUE,
                     tickformat = ',.0f'),
        hovermode = 'closest',
        legend = list(orientation = 'h', yanchor = 'bottom', y = 1.02,
                      xanchor = 'right', x = 1),
        plot_bgcolor = '#F8F9FA',
        paper_bgcolor = '#FFFFFF'
      )
      
    } else if(input$指标类型 == "growth") {
      # 收支增速对比图
      df$增速提示 <- paste0(
        "年份: ", df$年份, "<br>",
        "财政收入增速: ", round(df$财政收入增速, 1), "%<br>",
        "财政支出增速: ", round(df$财政支出增速, 1), "%<br>",
        "增速差: ", round(df$财政支出增速 - df$财政收入增速, 1), "%"
      )
      
      p <- plot_ly() %>%
        add_trace(
          x = ~df$年份, y = ~df$财政收入增速,
          name = "财政收入增速",
          type = 'scatter', mode = if(input$显示数据点) 'lines+markers' else 'lines',
          line = list(color = '#2E86C1', width = 3),
          marker = list(size = 8, color = '#2E86C1'),
          text = ~df$增速提示, hoverinfo = 'text',
          hovertemplate = paste('<b>%{text}</b><extra></extra>')
        ) %>%
        add_trace(
          x = ~df$年份, y = ~df$财政支出增速,
          name = "财政支出增速",
          type = 'scatter', mode = if(input$显示数据点) 'lines+markers' else 'lines',
          line = list(color = '#E74C3C', width = 3),
          marker = list(size = 8, color = '#E74C3C'),
          text = ~df$增速提示, hoverinfo = 'text',
          hovertemplate = paste('<b>%{text}</b><extra></extra>')
        ) %>%
        add_trace(
          x = ~df$年份, y = rep(0, nrow(df)),
          name = "零增长线",
          type = 'scatter', mode = 'lines',
          line = list(color = 'gray', width = 1, dash = 'dash'),
          showlegend = TRUE
        ) %>%
        layout(
          title = list(text = "一般公共预算收支增速趋势", font = list(size = 16)),
          xaxis = list(title = "年份", gridcolor = '#E5E5E5'),
          yaxis = list(title = "增长率（%）", gridcolor = '#E5E5E5',
                       zeroline = FALSE),
          hovermode = 'closest',
          legend = list(orientation = 'h', yanchor = 'bottom', y = 1.02,
                        xanchor = 'right', x = 1),
          plot_bgcolor = '#F8F9FA',
          paper_bgcolor = '#FFFFFF'
        )
      
    } else {
      # 占GDP比重对比图
      df$占比提示 <- paste0(
        "年份: ", df$年份, "<br>",
        "财政收入占GDP: ", round(df$财政收入占比, 1), "%<br>",
        "财政支出占GDP: ", round(df$财政支出占比, 1), "%<br>",
        "占比差: ", round(df$财政支出占比 - df$财政收入占比, 1), "%"
      )
      
      p <- plot_ly() %>%
        add_trace(
          x = ~df$年份, y = ~df$财政收入占比,
          name = "财政收入/GDP",
          type = 'scatter', mode = if(input$显示数据点) 'lines+markers' else 'lines',
          line = list(color = '#2E86C1', width = 3),
          marker = list(size = 8, color = '#2E86C1'),
          text = ~df$占比提示, hoverinfo = 'text',
          hovertemplate = paste('<b>%{text}</b><extra></extra>')
        ) %>%
        add_trace(
          x = ~df$年份, y = ~df$财政支出占比,
          name = "财政支出/GDP",
          type = 'scatter', mode = if(input$显示数据点) 'lines+markers' else 'lines',
          line = list(color = '#E74C3C', width = 3),
          marker = list(size = 8, color = '#E74C3C'),
          text = ~df$占比提示, hoverinfo = 'text',
          hovertemplate = paste('<b>%{text}</b><extra></extra>')
        ) %>%
        layout(
          title = list(text = "一般公共预算收支占GDP比重趋势", font = list(size = 16)),
          xaxis = list(title = "年份", gridcolor = '#E5E5E5'),
          yaxis = list(title = "占GDP比重（%）", gridcolor = '#E5E5E5',
                       range = c(0, max(df$财政支出占比) + 5)),
          hovermode = 'closest',
          legend = list(orientation = 'h', yanchor = 'bottom', y = 1.02,
                        xanchor = 'right', x = 1),
          plot_bgcolor = '#F8F9FA',
          paper_bgcolor = '#FFFFFF'
        )
    }
    
    # 添加范围滑块
    p <- p %>% layout(
      xaxis = list(rangeslider = list(visible = TRUE, thickness = 0.05)),
      annotations = list(
        list(
          x = 0.02, y = 0.98,
          text = "← 拖动选择年份范围",
          showarrow = FALSE,
          xref = 'paper', yref = 'paper',
          font = list(size = 10, color = 'gray')
        )
      )
    )
    
    p
  })
  
  # 3D 可视化
  output$scatter3d <- renderPlotly({
    df <- filtered_data()
    
    plot_ly(df, x = ~财政收入, y = ~财政支出, z = ~年份,
            type = 'scatter3d', mode = 'markers',
            marker = list(size = 8, 
                          color = ~财政收入增速, 
                          colorscale = 'Viridis',
                          showscale = TRUE,
                          colorbar = list(title = "收入增速(%)")),
            text = ~paste("年份:", 年份, "<br>",
                          "收入:", format(round(财政收入, 0), big.mark = ","), "亿<br>",
                          "支出:", format(round(财政支出, 0), big.mark = ","), "亿<br>",
                          "收入增速:", round(财政收入增速, 1), "%<br>",
                          "支出增速:", round(财政支出增速, 1), "%"),
            hoverinfo = 'text') %>%
      layout(
        title = "财政收入-支出-年份 3D 关系图",
        scene = list(
          xaxis = list(title = "财政收入（亿元）", tickformat = ',.0f'),
          yaxis = list(title = "财政支出（亿元）", tickformat = ',.0f'),
          zaxis = list(title = "年份"),
          camera = list(eye = list(x = 1.5, y = 1.5, z = 0.8))
        )
      )
  })
  
  # 数据摘要
  output$data_summary <- renderPrint({
    df <- filtered_data()
    cat("📊 数据概览\n")
    cat(rep("─", 30), "\n")
    cat("年份范围：", min(df$年份), "-", max(df$年份), "\n")
    cat("财政收入均值：", format(round(mean(df$财政收入), 0), big.mark = ","), "亿元\n")
    cat("财政支出均值：", format(round(mean(df$财政支出), 0), big.mark = ","), "亿元\n")
    cat("赤字总额：", format(round(sum(df$财政支出 - df$财政收入), 0), big.mark = ","), "亿元\n")
    cat("\n📈 平均增速\n")
    cat("财政收入：", round(mean(df$财政收入增速), 1), "%\n")
    cat("财政支出：", round(mean(df$财政支出增速), 1), "%\n")
  })
  
  # 关键发现
  output$insights <- renderPrint({
    df <- data %>%
      filter(年份 >= input$年份范围[1], 年份 <= input$年份范围[2])
    
    cat("🔍 关键发现\n")
    cat(rep("═", 40), "\n\n")
    
    # 最高最低年份
    max_revenue_year <- df$年份[which.max(df$财政收入)]
    max_expense_year <- df$年份[which.max(df$财政支出)]
    
    cat("💰 财政收入\n")
    cat("  最高年份：", max_revenue_year, "年 (", 
        format(round(max(df$财政收入), 0), big.mark = ","), "亿元)\n")
    cat("  最低年份：", df$年份[which.min(df$财政收入)], "年 (", 
        format(round(min(df$财政收入), 0), big.mark = ","), "亿元)\n\n")
    
    cat("💸 财政支出\n")
    cat("  最高年份：", max_expense_year, "年 (", 
        format(round(max(df$财政支出), 0), big.mark = ","), "亿元)\n")
    cat("  最低年份：", df$年份[which.min(df$财政支出)], "年 (", 
        format(round(min(df$财政支出), 0), big.mark = ","), "亿元)\n\n")
    
    # 增长情况
    first_year <- df[1, ]
    last_year <- df[nrow(df), ]
    revenue_growth <- (last_year$财政收入 - first_year$财政收入) / first_year$财政收入 * 100
    expense_growth <- (last_year$财政支出 - first_year$财政支出) / first_year$财政支出 * 100
    
    cat("📈 累计增长（", first_year$年份, "→", last_year$年份, "）\n")
    cat("  财政收入增长：", format(round(revenue_growth, 1), big.mark = ","), "%\n")
    cat("  财政支出增长：", format(round(expense_growth, 1), big.mark = ","), "%\n\n")
    
    # 赤字情况
    deficit_years <- df %>% filter(财政支出 > 财政收入)
    surplus_years <- df %>% filter(财政收入 >= 财政支出)
    
    cat("⚖️ 财政平衡\n")
    cat("  赤字年份：", nrow(deficit_years), "年\n")
    cat("  盈余年份：", nrow(surplus_years), "年\n")
    cat("  最大赤字额：", format(round(max(df$财政支出 - df$财政收入), 0), big.mark = ","), "亿元\n\n")
    
    # 特殊情况
    if(2020 %in% df$年份) {
      covid_data <- df %>% filter(年份 == 2020)
      cat("🦠 特殊情况：2020年（新冠疫情）\n")
      cat("  财政收入增速：", covid_data$财政收入增速, "%\n")
      cat("  财政支出增速：", covid_data$财政支出增速, "%\n")
      cat("  财政赤字：", format(round(covid_data$财政支出 - covid_data$财政收入, 0), big.mark = ","), "亿元\n")
    }
    
    # 占比变化
    cat("\n📊 占GDP比重变化\n")
    cat("  财政收入占比从", first_year$财政收入占比, "% →", 
        last_year$财政收入占比, "%\n")
    cat("  财政支出占比从", first_year$财政支出占比, "% →", 
        last_year$财政支出占比, "%\n")
  })
  
  # 数据表
  output$data_table <- renderDT({
    df <- filtered_data() %>%
      mutate(
        财政收入 = format(round(财政收入, 1), big.mark = ","),
        财政支出 = format(round(财政支出, 1), big.mark = ","),
        财政收入增速 = paste0(round(财政收入增速, 1), "%"),
        财政支出增速 = paste0(round(财政支出增速, 1), "%"),
        财政收入占比 = paste0(round(财政收入占比, 1), "%"),
        财政支出占比 = paste0(round(财政支出占比, 1), "%")
      )
    
    datatable(df, 
              options = list(
                pageLength = 20,
                scrollX = TRUE,
                dom = 'Bfrtip',
                buttons = c('copy', 'csv', 'excel', 'pdf'),
                columnDefs = list(list(className = 'dt-center', targets = '_all'))
              ),
              rownames = FALSE,
              caption = htmltools::tags$caption(
                style = 'caption-side: bottom; text-align: center;',
                '表1：中国一般公共预算收支数据表（1978-2022）'
              ))
  })
  
  # 年度对比
  output$comparison <- renderPrint({
    year1_data <- data %>% filter(年份 == input$year1)
    year2_data <- data %>% filter(年份 == input$year2)
    
    if(nrow(year1_data) == 0 | nrow(year2_data) == 0) {
      cat("请选择有效的年份")
      return()
    }
    
    cat("📊 对比分析：", input$year1, "年 vs", input$year2, "\n")
    cat(rep("═", 50), "\n\n")
    
    # 财政收入对比
    revenue_change <- (year2_data$财政收入 - year1_data$财政收入) / year1_data$财政收入 * 100
    cat("💰 财政收入\n")
    cat("  ", format(round(year1_data$财政收入, 0), big.mark = ","), "亿元 → ",
        format(round(year2_data$财政收入, 0), big.mark = ","), "亿元\n")
    cat("  变化：", ifelse(revenue_change >= 0, "+", ""), round(revenue_change, 1), "%\n\n")
    
    # 财政支出对比
    expense_change <- (year2_data$财政支出 - year1_data$财政支出) / year1_data$财政支出 * 100
    cat("💸 财政支出\n")
    cat("  ", format(round(year1_data$财政支出, 0), big.mark = ","), "亿元 → ",
        format(round(year2_data$财政支出, 0), big.mark = ","), "亿元\n")
    cat("  变化：", ifelse(expense_change >= 0, "+", ""), round(expense_change, 1), "%\n\n")
    
    # 赤字变化
    deficit1 <- year1_data$财政支出 - year1_data$财政收入
    deficit2 <- year2_data$财政支出 - year2_data$财政收入
    cat("⚖️ 财政赤字\n")
    cat("  ", format(round(deficit1, 0), big.mark = ","), "亿元 → ",
        format(round(deficit2, 0), big.mark = ","), "亿元\n")
    cat("  变化：", ifelse(deficit2 - deficit1 >= 0, "+", ""), 
        format(round(deficit2 - deficit1, 0), big.mark = ","), "亿元\n\n")
    
    # 增速对比
    cat("📈 增速对比\n")
    cat("  财政收入增速：", year1_data$财政收入增速, "% → ", 
        year2_data$财政收入增速, "%\n")
    cat("  财政支出增速：", year1_data$财政支出增速, "% → ", 
        year2_data$财政支出增速, "%\n")
  })
  
  # 对比图
  output$compare_plot <- renderPlotly({
    df <- data %>%
      filter(年份 %in% c(input$year1, input$year2))
    
    df_long <- df %>%
      pivot_longer(cols = c(财政收入, 财政支出),
                   names_to = "指标",
                   values_to = "金额")
    
    p <- plot_ly(df_long, 
                 x = ~指标, 
                 y = ~金额, 
                 color = ~as.factor(年份),
                 type = 'bar',
                 colors = c('#2E86C1', '#E74C3C'),
                 text = ~paste(年份, "年<br>", 指标, ":", format(round(金额, 0), big.mark = ","), "亿元"),
                 hoverinfo = 'text',
                 textposition = 'auto') %>%
      layout(
        title = paste(input$year1, "年与", input$year2, "年收支对比"),
        xaxis = list(title = "", tickangle = 0),
        yaxis = list(title = "金额（亿元）", tickformat = ',.0f'),
        barmode = 'group',
        legend = list(title = list(text = "年份"), orientation = 'h', 
                      yanchor = 'bottom', y = 1.02, xanchor = 'center', x = 0.5),
        hovermode = 'closest'
      )
    
    p
  })
}

# 运行应用
shinyApp(ui = ui, server = server)
