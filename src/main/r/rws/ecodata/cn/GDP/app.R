# 保存为 app.R
# 运行方式：在RStudio中打开此文件，点击“Run App”按钮，或执行 shiny::runApp()

# 加载必要的库
library(shiny)
library(plotly)
library(dplyr)
library(tidyr)

setwd("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/ecodata/cn/GDP")

# 数据 - 从图片内容整理
data <- read.csv("gdp_2016-2025.csv")
names(data ) <- names(data) |> gsub("^X", "", x=_)
rng_year <- names(data)[-1] |> range() |> as.numeric() |> setNames(c("min_year","max_year"))

# 转换为长格式（面板数据）
data_long <- data %>%
  pivot_longer(cols = -地区, names_to = "年份", values_to = "GDP") %>%
  mutate(年份 = as.numeric(年份))

# 计算每个地区各年份的增长率（用于构建误差线）
# 这里使用增长率的标准差作为误差线的度量，展示经济波动
data_with_error <- data_long %>%
  group_by(地区) %>%
  arrange(年份) %>%
  mutate(
   增长率 = (GDP - lag(GDP)) / lag(GDP) * 100,
   # 使用移动窗口标准差作为误差（前3年的增长率标准差）
   error = zoo::rollapply(增长率, width = 3, FUN = function(x) sd(x, na.rm = TRUE), 
                          fill = NA, align = "right", partial = TRUE)
  ) %>%
  ungroup() %>%
  # 将误差转换为GDP的绝对误差（用于可视化）
  mutate(
    error_abs = ifelse(is.na(error), GDP * 0.02, GDP * (error / 100)),  # 基于增长率的波动
    # 确保误差线不为负值
    error_lower = pmax(GDP - error_abs, GDP * 0.95),
    error_upper = GDP + error_abs
  )

# 获取地区列表用于checkbox
regions <- sort(unique(data_long$地区))

# 辅助函数：计算趋势线（线性回归）
get_trend <- function(df) {
  if(nrow(df) < 2) return(NULL)
  model <- lm(GDP ~ 年份, data = df)
  trend <- predict(model, newdata = data.frame(年份 = df$年份))
  return(trend)
}

# UI
ui <- fluidPage(
  # 添加关闭按钮的JavaScript
  tags$head(
    tags$script(HTML('
      Shiny.addCustomMessageHandler("close_window", function(m) {
        if (window.close) {
          window.close();
        } else {
          alert("无法自动关闭窗口，请手动关闭浏览器标签页。");
        }
      });
    ')),
    tags$style(HTML('
      .dataTables_wrapper {
        font-size: 12px;
      }
      .plotly-container {
        margin-bottom: 20px;
      }
    '))
  ),
  
  titlePanel(gettextf("中国各省（区、市）地区生产总值展示（%s-%s）", rng_year[1], rng_year[2])),
  
  sidebarLayout(
    sidebarPanel(
      h4("选择要显示的省份/地区"),
      p("勾选下方复选框以选择经济个体，支持多选："),
      checkboxGroupInput(
        inputId = "selected_regions",
        label = "地区选择",
        choices = regions,
        selected = sample(data$地区, 5)  # 默认选择几个代表性省份
      ),
      hr(),
      h5("图表设置"),
      checkboxInput("show_trend", "显示趋势线（线性回归）", value = TRUE),
      checkboxInput("show_errorbars", "显示误差线（基于增长率波动）", value = TRUE),
      hr(),
      helpText(gettextf("数据来源：国家统计局数据发布库。%s年数据为初步核算数。", rng_year[2])),
      helpText("误差线说明：基于3年滚动增长率标准差计算的GDP波动范围，反映经济稳定性。"),
      br(),
      # 关闭按钮
      actionButton("close_btn", "关闭应用", icon = icon("power-off"), 
                   style = "color: white; background-color: #d9534f; border-color: #d43f3a; width: 100%;")
    ),
    
    mainPanel(
      tabsetPanel(
        tabPanel("主图",
                 br(),
                 plotlyOutput("time_series_plot", height = "650px"),
                 br(),
                 div(style = "background-color: #f9f9f9; padding: 10px; border-radius: 5px;",
                     p("📊 图表说明："),
                     p("• 实线表示实际GDP值，点标记为数据点。"),
                     p("• 趋势线（如果启用）为线性回归拟合线，展示长期增长趋势。"),
                     p("• 误差线（如果启用）基于3年滚动增长率标准差，反映该地区经济的波动程度。")
                 )
        ),
        tabPanel("数据",
                 br(),
                 DT::dataTableOutput("data_table")
        ),
        tabPanel("数据说明",
                 br(),
                 h4("📋 数据来源与说明"),
                 h5("基础数据"),
                 p("• 数据来源：国家统计局数据发布库 (data.stats.gov.cn)"),
                 p("• 统计指标：地区生产总值 (GDP)，单位为亿元。"),
                 p(gettextf("• 时间范围：%s年至%s年，共%s个年份。", rng_year[1], rng_year[2], rng_year[2] - rng_year[1])),
                 p("• 地区范围：全国31个省、自治区、直辖市（不含港澳台）。"),
                 p(gettextf("• %s年数据为初步核算数，后续可能修订。", rng_year[2])),
                 
                 h5("📈 误差线计算方法"),
                 p("误差线基于各地区的年度增长率波动构建："),
                 p("1. 计算每个地区各年份的GDP年增长率（与前一年相比）"),
                 p("2. 使用3年滚动窗口计算增长率的标准差"),
                 p("3. 将标准差转化为GDP的绝对误差值：误差绝对值 = GDP × (滚动标准差 / 100)"),
                 p("4. 误差线范围 = GDP ± 误差绝对值"),
                 p("• 误差线越宽表示该地区近年来经济波动越大，稳定性相对较低"),
                 p("• 误差线越窄表示经济增长相对稳定"),
                 
                 h5("📉 趋势线说明"),
                 p("趋势线使用最小二乘法线性回归拟合，反映该地区2016-2022年的整体增长趋势。"),
                 p("• 斜率正值表示经济增长，负值表示经济收缩"),
                 p("• 趋势线可以帮助识别长期增长模式，排除短期波动干扰"),
                 
                 h5("💡 使用指南"),
                 p("1. 左侧面板可勾选想要查看的地区（最多支持所有31个地区）"),
                 p("2. 主图支持缩放、平移、悬停查看详细数值"),
                 p("3. 图例可点击筛选单条曲线，双击恢复全部显示"),
                 p("4. 可通过复选框控制趋势线和误差线的显示"),
                 p("5. 数据页可查看完整面板数据，支持搜索、排序和导出"),
                 p("6. 点击下方红色按钮可关闭应用窗口")
        )
      )
    )
  )
)

# Server
server <- function(input, output, session) {
  
  # 根据选择过滤数据，并添加趋势线和误差线
  filtered_data <- reactive({
    req(input$selected_regions)
    df <- data_with_error %>%
      filter(地区 %in% input$selected_regions)
    
    # 为每个地区添加趋势线值
    df <- df %>%
      group_by(地区) %>%
      mutate(trend_value = if(input$show_trend) {
        get_trend(data.frame(年份 = 年份, GDP = GDP))
      } else {
        NA_real_
      }) %>%
      ungroup()
    
    return(df)
  })
  
  # 动态交互主图（带误差线和趋势线）
  output$time_series_plot <- renderPlotly({
    df <- filtered_data()
    req(nrow(df) > 0)
    
    # 获取唯一的地区列表
    regions_selected <- unique(df$地区)
    
    # 创建plotly对象
    p <- plot_ly()
    
    # 为每个地区添加迹（包括主曲线、趋势线、误差线）
    for(region in regions_selected) {
      df_region <- df %>% filter(地区 == region) %>% arrange(年份)
      
      # 主曲线：带标记的线图
      p <- p %>% add_trace(
        data = df_region,
        x = ~年份, y = ~GDP,
        name = region,
        type = 'scatter', mode = 'lines+markers',
        line = list(width = 2.5),
        marker = list(size = 8, symbol = 'circle'),
        text = ~paste(
          "地区:", 地区,
          "<br>年份:", 年份,
          "<br>GDP:", round(GDP, 1), "亿元",
          "<br>年增长率:", ifelse(is.na(增长率), "N/A", paste0(round(增长率, 2), "%")),
          "<br>波动误差:", ifelse(is.na(error), "N/A", paste0(round(error, 2), "%"))
        ),
        hoverinfo = 'text',
        legendgroup = region,
        showlegend = TRUE
      )
      
      # 添加误差线（如果启用）
      if(input$show_errorbars) {
        p <- p %>% add_trace(
          data = df_region,
          x = ~年份, y = ~GDP,
          name = paste(region, "(误差范围)"),
          type = 'scatter', mode = 'markers',
          marker = list(size = 0, opacity = 0),
          error_y = list(
            type = 'data',
            symmetric = TRUE,
            array = ~error_abs,
            arrayminus = ~error_abs,
            color = 'rgba(100,100,100,0.5)',
            thickness = 1.5,
            width = 8
          ),
          hoverinfo = 'none',
          legendgroup = region,
          showlegend = FALSE
        )
      }
      
      # 添加趋势线（如果启用）
      if(input$show_trend && any(!is.na(df_region$trend_value))) {
        p <- p %>% add_trace(
          data = df_region,
          x = ~年份, y = ~trend_value,
          name = paste(region, "(趋势)"),
          type = 'scatter', mode = 'lines',
          line = list(dash = 'dash', width = 1.5, color = 'rgba(0,0,0,0.5)'),
          marker = list(size = 0),
          text = ~paste(
            "地区:", 地区,
            "<br>趋势值:", round(trend_value, 1), "亿元"
          ),
          hoverinfo = 'text',
          legendgroup = region,
          showlegend = FALSE
        )
      }
    }
    
    # 布局设置
    p <- p %>% layout(
      title = list(
        text = "各省（区、市）地区生产总值时间序列（带趋势线与误差线）",
        x = 0.05,
        font = list(size = 16)
      ),
      xaxis = list(
        title = "年份",
        dtick = 1,
        tickangle = 0,
        gridcolor = '#e9e9e9',
        zerolinecolor = '#cccccc'
      ),
      yaxis = list(
        title = "GDP (亿元)",
        hoverformat = '.1f',
        rangemode = "nonnegative",
        gridcolor = '#e9e9e9',
        zerolinecolor = '#cccccc'
      ),
      legend = list(
        orientation = "v", 
        x = 1.02, 
        y = 1, 
        bgcolor = 'rgba(255,255,255,0.9)',
        bordercolor = 'rgba(0,0,0,0.2)',
        borderwidth = 1,
        font = list(size = 11)
      ),
      hovermode = "closest",
      margin = list(r = 150, l = 60, t = 60, b = 50),
      plot_bgcolor = '#f8f9fa',
      paper_bgcolor = '#ffffff'
    ) %>%
      config(
        displayModeBar = TRUE, 
        modeBarButtonsToRemove = c("lasso2d", "select2d"),
        displaylogo = FALSE,
        toImageButtonOptions = list(format = "png", width = 1200, height = 800)
      )
    
    p
  })
  
  # 数据表格显示（面板数据格式）
  output$data_table <- DT::renderDataTable({
    df <- filtered_data()
    req(nrow(df) > 0)
    
    # 选择要显示的列
    df_display <- df %>%
      select(地区, 年份, GDP, 增长率, error, error_abs) %>%
      mutate(
        增长率 = round(增长率, 2),
        波动率_标准差 = round(error, 2),
        误差绝对值 = round(error_abs, 1)
      ) %>%
      rename(
        地区 = 地区,
        年份 = 年份,
        GDP_亿元 = GDP,
        年增长率_百分比 = 增长率,
        滚动波动率_百分比 = 波动率_标准差,
        误差范围_亿元 = 误差绝对值
      )
    
    DT::datatable(
      df_display,
      options = list(
        pageLength = 31,
        scrollX = TRUE,
        dom = 'Bfrtip',
        buttons = c('copy', 'csv', 'excel', 'print'),
        language = list(url = '//cdn.datatables.net/plug-ins/1.10.11/i18n/Chinese.json')
      ),
      rownames = FALSE,
      caption = htmltools::tags$caption(
        style = 'caption-side: bottom; text-align: center;',
        '单位：GDP为亿元；增长率、波动率为百分比；误差线基于3年滚动增长率标准差'
      )
    ) %>%
      DT::formatRound(columns = c("GDP_亿元", "误差范围_亿元"), digits = 1) %>%
      DT::formatRound(columns = c("年增长率_百分比", "滚动波动率_百分比"), digits = 2)
  })
  
  # 关闭应用按钮逻辑
  observeEvent(input$close_btn, {
    showModal(modalDialog(
      title = tagList(icon("question-circle"), "确认退出"),
      "确定要关闭应用吗？",
      easyClose = FALSE,
      footer = tagList(
        modalButton("取消"),
        actionButton("confirm_close", "确认关闭", class = "btn-danger", icon = icon("power-off"))
      )
    ))
  })
  
  observeEvent(input$confirm_close, {
    removeModal()
    # 尝试关闭窗口
    session$sendCustomMessage("close_window", list())
    # 停止应用（作为后备方案）
    stopApp()
  })
  
  # 在会话结束时也尝试停止，确保资源释放
  session$onSessionEnded(function() {
    stopApp()
  })
}

# 运行应用
shinyApp(ui = ui, server = server)
