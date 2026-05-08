# 加载必要的库
library(shiny)
library(plotly)
library(dplyr)
library(tidyr)
library(DT)
library(purrr)

setwd("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/ecodata/cn/固定资产")

# 1. 准备数据 (基于您提供的图片内容手动录入)
data <- read.csv("data.csv")

# 将数据转换为长格式，方便plotly绘图
data_long_invest <- data %>%
  select(年份, 第一产业, 第二产业, 第三产业) %>%
  pivot_longer(cols = -年份, names_to = "产业", values_to = "投资额")

data_long_growth <- data %>%
  select(年份, 第一产业增长, 第二产业增长, 第三产业增长) %>%
  pivot_longer(cols = -年份, names_to = "产业", values_to = "增长率") %>%
  mutate(产业 = gsub("增长", "", 产业))  # 清理名称

# 2. UI界面
ui <- fluidPage(
  titlePanel("中国三次产业固定资产投资分析 (2003-2024)"),
  
  sidebarLayout(
    sidebarPanel(
      radioButtons("metric", "选择展示指标:",
                   choices = list("投资额 (亿元)" = "inv", 
                                  "比上年增长 (%)" = "growth"),
                   selected = "inv"),
      
      hr(),
      
      h5("选择要显示的产业:"),
      checkboxGroupInput("industries", "产业选择",
                         choices = c("第一产业", "第二产业", "第三产业"),
                         selected = c("第一产业", "第二产业", "第三产业")),
      
      hr(),
      
      helpText("注：2003-2010年为城镇固定资产投资口径；",
               "2011-2024年为固定资产投资（不含农户）口径；",
               "增速为可比口径。"),
      
      br(),
      
      actionButton("close", "关闭应用", class = "btn-danger", 
                   icon = icon("power-off"), width = "100%")
    ),
    
    mainPanel(
      tabsetPanel(
        tabPanel("动态图表", 
                 br(),
                 plotlyOutput("trendPlot", height = "550px")),
        tabPanel("数据表格",
                 br(),
                 DTOutput("dataTable")),
        tabPanel("关于数据",
                 br(),
                 h4("数据说明"),
                 tags$ul(
                   tags$li("数据时间范围：2003-2024年"),
                   tags$li("2003-2010年为城镇固定资产投资口径"),
                   tags$li("2011-2024年为固定资产投资（不含农户）口径"),
                   tags$li("增速为可比口径"),
                   tags$li("投资额单位为：亿元"),
                   tags$li("增长率单位为：%")
                 ),
                 h4("数据结构"),
                 verbatimTextOutput("dataStructure"),
                 h4("数据摘要"),
                 verbatimTextOutput("dataSummary"))
      )
    )
  )
)

# 3. 服务器逻辑
server <- function(input, output, session) {
  
  # 根据选择的产业过滤数据
  filtered_invest_data <- reactive({
    req(input$industries)
    data_long_invest %>%
      filter(产业 %in% input$industries)
  })
  
  filtered_growth_data <- reactive({
    req(input$industries)
    data_long_growth %>%
      filter(产业 %in% input$industries)
  })
  
  # 动态生成Plotly图表
  output$trendPlot <- renderPlotly({
    req(input$industries)
    
    if(length(input$industries) == 0) {
      # 如果没有选择产业，显示提示信息
      plot_ly() %>%
        layout(title = "请至少选择一个产业",
               xaxis = list(title = "年份"),
               yaxis = list(title = ifelse(input$metric == "inv", "投资额 (亿元)", "增长率 (%)")))
    } else {
      if(input$metric == "inv") {
        # 投资额图表
        colors <- c("第一产业" = "#2ca02c", 
                    "第二产业" = "#1f77b4", 
                    "第三产业" = "#ff7f0e")
        
        p <- plot_ly()
        
        # 为每个选中的产业添加线条
        for(industry in input$industries) {
          df_industry <- filtered_invest_data() %>% filter(产业 == industry)
          p <- p %>% add_trace(data = df_industry,
                               x = ~年份, 
                               y = ~投资额, 
                               name = industry,
                               type = 'scatter', 
                               mode = 'lines+markers',
                               line = list(width = 2),
                               marker = list(size = 8),
                               color = I(colors[industry]),
                               hoverinfo = 'text',
                               text = ~paste("年份:", 年份, 
                                            "<br>产业:", 产业, 
                                            "<br>投资额:", round(投资额, 1), "亿元"))
        }
        
        p <- p %>% layout(title = "三次产业固定资产投资趋势",
                         xaxis = list(title = "年份", dtick = 2, gridcolor = '#e9e9e9'),
                         yaxis = list(title = "投资额 (亿元)", gridcolor = '#e9e9e9'),
                         legend = list(title = list(text = "产业"), 
                                      orientation = "h", 
                                      yanchor = "bottom",
                                      y = 1.02,
                                      xanchor = "right",
                                      x = 1),
                         hovermode = "closest",
                         plot_bgcolor = '#f5f5f5')
      } else {
        # 增长率图表
        colors <- c("第一产业" = "#2ca02c", 
                    "第二产业" = "#1f77b4", 
                    "第三产业" = "#ff7f0e")
        
        p <- plot_ly()
        
        # 为每个选中的产业添加线条
        for(industry in input$industries) {
          df_industry <- filtered_growth_data() %>% filter(产业 == industry)
          p <- p %>% add_trace(data = df_industry,
                               x = ~年份, 
                               y = ~增长率, 
                               name = industry,
                               type = 'scatter', 
                               mode = 'lines+markers',
                               line = list(width = 2),
                               marker = list(size = 8),
                               color = I(colors[industry]),
                               hoverinfo = 'text',
                               text = ~paste("年份:", 年份, 
                                            "<br>产业:", 产业, 
                                            "<br>增长率:", round(增长率, 1), "%"))
        }
        
        p <- p %>% layout(title = "三次产业固定资产投资比上年增长趋势",
                         xaxis = list(title = "年份", dtick = 2, gridcolor = '#e9e9e9'),
                         yaxis = list(title = "增长率 (%)", gridcolor = '#e9e9e9'),
                         legend = list(title = list(text = "产业"), 
                                      orientation = "h", 
                                      yanchor = "bottom",
                                      y = 1.02,
                                      xanchor = "right",
                                      x = 1),
                         hovermode = "closest",
                         plot_bgcolor = '#f5f5f5')
      }
    }
    p
  })
  
  # 显示原始数据表（使用DT包实现交互式表格）
  output$dataTable <- renderDT({
    datatable( data %>% mutate(across(everything(), ~replace(., is.na(.), 0))), 
              options = list( pageLength = 10, scrollX = TRUE, dom = 'Bfrtip', buttons = c('copy', 'csv', 'excel', 'pdf')),
              rownames = FALSE,
              class = 'display nowrap',
              caption = "三次产业固定资产投资完整数据表") %>% formatRound(columns = data |> compose(\(x) x[-1], seq, length) () , digits = 1)
  })
  
  # 显示数据结构
  output$dataStructure <- renderPrint({
    str(data)
  })
  
  # 显示数据摘要
  output$dataSummary <- renderPrint({
    summary(data)
  })
  
  # 关闭应用按钮的逻辑
  observeEvent(input$close, {
    showModal(modalDialog(
      title = "确认退出",
      "确定要关闭应用吗？",
      footer = tagList(
        modalButton("取消"),
        actionButton("confirmClose", "确认关闭", class = "btn-danger")
      )
    ))
  })
  
  observeEvent(input$confirmClose, {
    stopApp(returnValue = invisible())
  })
}

# 4. 运行应用
shinyApp(ui = ui, server = server)
