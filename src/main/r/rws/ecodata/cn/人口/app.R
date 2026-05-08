# 请先安装以下包（如果尚未安装）
# install.packages(c("shiny", "plotly", "DT", "dplyr", "tidyr"))

# 加载必要的库
library(shiny)
library(plotly)
library(DT)
library(dplyr)
library(tidyr)

setwd("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/ecodata/cn/人口")

# 根据图片内容提取数据（仅到2024年）
population_data <- read.csv("data.csv")

# 计算性别占比（百分比）
population_data <- population_data %>%
  mutate(
    男占比 = round(男 / 总人口 * 100, 2),
    女占比 = round(女 / 总人口 * 100, 2),
    城镇占比 = round(城镇 / 总人口 * 100, 2),
    乡村占比 = round(乡村 / 总人口 * 100, 2)
  )

# 定义UI
ui <- fluidPage(
  # 添加关闭按钮的样式
  tags$head(
    tags$style(HTML("
      .stop-button {
        position: fixed;
        bottom: 20px;
        right: 20px;
        z-index: 1000;
        background-color: #d9534f;
        color: white;
        border: none;
        padding: 10px 20px;
        border-radius: 5px;
        font-size: 16px;
        cursor: pointer;
        box-shadow: 2px 2px 5px rgba(0,0,0,0.3);
      }
      .stop-button:hover {
        background-color: #c9302c;
      }
    "))
  ),
  
  titlePanel("中国人口数据交互式仪表板 (1949-2024)"),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      helpText("数据来源：国家统计局"),
      helpText("注：数据为1949-2024年实际统计值"),
      br(),
      
      # 选择图表类型
      selectInput("plotType", 
                  label = "选择图表类型",
                  choices = c("人口总数趋势" = "total",
                              "性别构成趋势" = "gender",
                              "城乡构成趋势" = "urban_rural"),
                  selected = "total"),
      
      # 年份范围滑块
      sliderInput("yearRange",
                  label = "选择年份范围",
                  min = min(population_data$年份),
                  max = max(population_data$年份),
                  value = c(2000, 2024),
                  step = 1,
                  sep = ""),
      
      # 在总人口图中额外显示数据点选项
      conditionalPanel(
        condition = "input.plotType == 'total'",
        checkboxInput("showPoints", "显示数据点", value = TRUE)
      ),
      
      br(),
      helpText("提示：可以缩放、平移或悬停查看详细数据")
    ),
    
    mainPanel(
      width = 9,
      tabsetPanel(
        tabPanel("交互图表", 
                 plotlyOutput("dynamicPlot", height = "500px"),
                 br(),
                 h4("数据摘要"),
                 verbatimTextOutput("summary")
        ),
        tabPanel("数据表格", 
                 DTOutput("dataTable")
        ),
        tabPanel("关于",
                 h4("关于本应用"),
                 p("本应用展示了中国1949年至2024年的人口数据，包括总人口、性别构成（男/女）和城乡构成（城镇/乡村）。"),
                 p("数据来源于国家统计局官方统计值。通过Plotly库提供动态交互功能，您可以缩放、平移、悬停查看具体数值。"),
                 p("点击右下角的「关闭应用」按钮可安全退出程序。"),
                 p("应用开发：Shiny + Plotly + DT")
        )
      )
    )
  ),
  
  # 关闭按钮 - 使用actionButton
  div(
    style = "position: fixed; bottom: 20px; right: 20px; z-index: 1000;",
    actionButton("stopApp", "关闭应用", 
                 icon = icon("power-off"),
                 style = "background-color: #d9534f; color: white; border: none; 
                          padding: 10px 20px; border-radius: 5px; font-size: 16px;
                          box-shadow: 2px 2px 5px rgba(0,0,0,0.3);")
  )
)

# 定义Server逻辑
server <- function(input, output, session) {
  
  # 根据年份范围筛选数据
  filtered_data <- reactive({
    population_data %>%
      filter(年份 >= input$yearRange[1], 年份 <= input$yearRange[2])
  })
  
  # 生成动态图表
  output$dynamicPlot <- renderPlotly({
    data <- filtered_data()
    
    # 根据选择的图表类型创建不同的plotly对象
    if(input$plotType == "total") {
      # 总人口趋势图
      p <- plot_ly(data, x = ~年份, y = ~总人口, 
                   type = 'scatter', mode = ifelse(input$showPoints, 'lines+markers', 'lines'),
                   name = '总人口',
                   line = list(color = '#1f77b4', width = 2),
                   marker = list(size = 4),
                   hovertemplate = paste('年份: %{x}<br>总人口: %{y:.0f} 万人<br><extra></extra>')) %>%
        layout(title = "中国总人口变化趋势 (1949-2024)",
               xaxis = list(title = "年份", tickangle = -45, dtick = 5),
               yaxis = list(title = "人口数 (万人)", 
                           hoverformat = '.0f'),
               hovermode = "x unified")
      
    } else if(input$plotType == "gender") {
      # 性别构成图 - 人口数
      p <- plot_ly(data, x = ~年份, y = ~男, 
                   name = '男性人口', type = 'scatter', mode = 'lines+markers',
                   line = list(color = '#2ca02c', width = 2),
                   marker = list(size = 3),
                   hovertemplate = paste('年份: %{x}<br>男性: %{y:.0f} 万人<br><extra></extra>')) %>%
        add_trace(y = ~女, name = '女性人口', mode = 'lines+markers',
                  line = list(color = '#d62728', width = 2),
                  marker = list(size = 3),
                  hovertemplate = paste('年份: %{x}<br>女性: %{y:.0f} 万人<br><extra></extra>')) %>%
        layout(title = "中国人口性别构成 (1949-2024)",
               xaxis = list(title = "年份", tickangle = -45, dtick = 5),
               yaxis = list(title = "人口数 (万人)"),
               hovermode = "x unified")
      
    } else { # urban_rural
      # 城乡构成图 - 人口数
      p <- plot_ly(data, x = ~年份, y = ~城镇, 
                   name = '城镇人口', type = 'scatter', mode = 'lines+markers',
                   line = list(color = '#ff7f0e', width = 2),
                   marker = list(size = 3),
                   hovertemplate = paste('年份: %{x}<br>城镇人口: %{y:.0f} 万人<br><extra></extra>')) %>%
        add_trace(y = ~乡村, name = '乡村人口', mode = 'lines+markers',
                  line = list(color = '#9467bd', width = 2),
                  marker = list(size = 3),
                  hovertemplate = paste('年份: %{x}<br>乡村人口: %{y:.0f} 万人<br><extra></extra>')) %>%
        layout(title = "中国人口城乡构成 (1949-2024)",
               xaxis = list(title = "年份", tickangle = -45, dtick = 5),
               yaxis = list(title = "人口数 (万人)"),
               hovermode = "x unified")
    }
    
    # 添加范围滑块和通用设置
    p %>% config(displayModeBar = TRUE, 
                 modeBarButtonsToRemove = c("lasso2d", "select2d"),
                 displaylogo = FALSE) %>%
      layout(legend = list(orientation = "h", yanchor = "bottom", y = 1.02, xanchor = "right", x = 1))
  })
  
  # 显示数据摘要
  output$summary <- renderPrint({
    data <- filtered_data()
    cat("当前筛选数据范围:", "\n")
    cat("年份区间:", min(data$年份), "至", max(data$年份), "\n")
    cat("总人口范围:", min(data$总人口), "至", max(data$总人口), "万人\n")
    cat("\n") 
    if(input$plotType == "total") {
      latest <- data[which.max(data$年份), ]
      cat("最新年份 (", latest$年份, ") 数据:\n")
      cat("总人口:", latest$总人口, "万人\n")
    } else if(input$plotType == "gender") {
      latest <- data[which.max(data$年份), ]
      cat("最新年份 (", latest$年份, ") 性别构成:\n")
      cat("男性:", latest$男, "万人 (", latest$男占比, "%)\n")
      cat("女性:", latest$女, "万人 (", latest$女占比, "%)\n")
      cat("性别比:", round(latest$男/latest$女*100, 2), " (男/女*100)\n")
    } else {
      latest <- data[which.max(data$年份), ]
      cat("最新年份 (", latest$年份, ") 城乡构成:\n")
      cat("城镇:", latest$城镇, "万人 (", latest$城镇占比, "%)\n")
      cat("乡村:", latest$乡村, "万人 (", latest$乡村占比, "%)\n")
      cat("城镇化率:", latest$城镇占比, "%\n")
    }
  })
  
  # 显示完整数据表（带格式）
  output$dataTable <- renderDT({
    datatable(population_data, 
              options = list(
                pageLength = 20,
                scrollX = TRUE,
                dom = 'Bfrtip',
                buttons = c('copy', 'csv', 'excel')
              ),
              rownames = FALSE,
              colnames = c("年份", "总人口(万人)", "男(万人)", "女(万人)", 
                          "城镇(万人)", "乡村(万人)", "男占比(%)", "女占比(%)",
                          "城镇占比(%)", "乡村占比(%)")
    ) %>%
      formatRound(columns = c("总人口", "男", "女", "城镇", "乡村"), digits = 0) %>%
      formatPercentage(columns = c("男占比", "女占比", "城镇占比", "乡村占比"), digits = 2)
  })
  
  # 关闭应用按钮的响应事件
  observeEvent(input$stopApp, {
    # 显示确认对话框
    showModal(modalDialog(
      title = "确认退出",
      "确定要关闭应用吗？",
      footer = tagList(
        modalButton("取消"),
        actionButton("confirmStop", "确定", class = "btn-danger")
      ),
      easyClose = TRUE
    ))
  })
  
  # 确认关闭后停止应用
  observeEvent(input$confirmStop, {
    stopApp(returnValue = invisible())
  })
}

# 运行Shiny应用
shinyApp(ui = ui, server = server)