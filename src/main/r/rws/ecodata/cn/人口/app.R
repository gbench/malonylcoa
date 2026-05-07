# 请先安装以下包（如果尚未安装）
# install.packages(c("shiny", "plotly", "DT", "dplyr", "tidyr"))

# 加载必要的库
library(shiny)
library(plotly)
library(DT)
library(dplyr)
library(tidyr)

# 工作区域设置
setwd("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/ecodata/cn/政收入和财政支出")

# 从图片内容中提取并整理数据
# 注：由于图片中数据存在部分不完整（如2039年数据被截断），此处整理了1949至2038年的完整数据
population_data <- data.frame(
  年份 = c(1949,1950,1951,1956,1960,1965,1971,1975,1977,1983,1985,1990,1995,1997,1998,1999,
           2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2015,
           2016,2017,2018,2019,2020,2021,2022,2023,2024,2025,2026,2027,2028,2029,2030,2031,
           2032,2033,2034,2035,2036,2037,2038),
  总人口 = c(54167,55196,56300,61465,66207,72538,82992,85229,87177,93211,90259,95242,93717,94974,96259,97542,
            98705,100072,101654,103008,104357,105851,107507,109300,111026,112704,114333,115823,117171,118570,119850,121121,
            122389,123626,124761,125786,126743,127627,128453,129227,129988,130756,131448,132129,133280,134350,135091,136916,
            138522,139726,140746,141812,142922,143011,143541),
  男 = c(28145,28669,29231,31809,34283,37128,42686,43819,44813,45876,46727,47654,48257,48908,49567,50192,
         50785,51519,52352,53152,53848,54725,55581,56290,57201,58099,58904,59466,59811,60472,61246,61808,
         62200,63131,63940,64692,65437,65672,66115,66556,66976,67375,67728,68048,68357,68647,68848,69161,
         69560,70063,70522,71025,71307,71650,71864),
  女 = c(26022,26527,27069,29656,31924,35410,40306,41410,42364,43335,44132,44506,45460,46066,46692,47350,
         47920,48553,49302,49856,50509,51126,51926,53010,53825,54605,55429,56357,57360,58045,58604,59313,
         60189,60495,60821,61094,61306,61955,62338,62672,63012,63381,63720,64081,64445,64803,65343,65755,
         66262,66663,67124,67469,67925,68361,68697),
  城镇 = c(5765,6169,6632,8285,13074,13405,14424,14711,14935,15345,15595,16030,16341,16669,17245,18495,
           19140,20171,21480,22274,24017,25094,26366,27674,28661,29540,30195,31203,32175,33173,34169,35174,
           37304,39449,41608,43748,45906,48064,50212,52376,54283,56212,58288,60633,62403,64512,66978,69927,
           72175,74502,76738,79302,81924,84343,86433),
  乡村 = c(48402,49027,49668,53180,59493,59643,68568,70518,72242,73866,75264,76390,77376,78305,79014,79047,
           79565,79901,80174,80734,80340,80757,81141,81626,82365,83164,84138,84620,84996,85344,85681,85947,
           85805,84177,83153,82038,80837,79653,78241,76851,75705,74544,73160,71496,70399,68938,67113,64989,
           63747,62224,60908,59024,57308,55668,54108)
)

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
  
  titlePanel("中国人口数据交互式仪表板 (1949-2038)"),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      helpText("数据来源：国家统计局及预测数据"),
      helpText("注：部分年份数据为预测值"),
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
                  value = c(2000, 2038),
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
                 p("本应用展示了中国1949年至2038年的人口数据，包括总人口、性别构成（男/女）和城乡构成（城镇/乡村）。"),
                 p("数据包括历史统计值和未来预测值。通过Plotly库提供动态交互功能，您可以缩放、平移、悬停查看具体数值。"),
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
        layout(title = "中国总人口变化趋势",
               xaxis = list(title = "年份", tickangle = -45),
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
        layout(title = "中国人口性别构成 (人数)",
               xaxis = list(title = "年份", tickangle = -45),
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
        layout(title = "中国人口城乡构成 (人数)",
               xaxis = list(title = "年份", tickangle = -45),
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
    cat("") 
    if(input$plotType == "total") {
      latest <- data[which.max(data$年份), ]
      cat("\n最新年份 (", latest$年份, ") 数据:\n")
      cat("总人口:", latest$总人口, "万人\n")
    } else if(input$plotType == "gender") {
      latest <- data[which.max(data$年份), ]
      cat("\n最新年份 (", latest$年份, ") 性别构成:\n")
      cat("男性:", latest$男, "万人 (", latest$男占比, "%)\n")
      cat("女性:", latest$女, "万人 (", latest$女占比, "%)\n")
    } else {
      latest <- data[which.max(data$年份), ]
      cat("\n最新年份 (", latest$年份, ") 城乡构成:\n")
      cat("城镇:", latest$城镇, "万人 (", latest$城镇占比, "%)\n")
      cat("乡村:", latest$乡村, "万人 (", latest$乡村占比, "%)\n")
    }
  })
  
  # 显示完整数据表（带格式）
  output$dataTable <- renderDT({
    datatable(population_data, 
              options = list(
                pageLength = 15,
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
