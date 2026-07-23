# ============================================================
# 《周易》卦象交互式探索 Shiny App（最终修复版）
# 修复行名访问问题
# ============================================================

library(shiny)
library(shinythemes)
library(ggplot2)
library(reshape2)
library(grid)
library(gridExtra)
library(RColorBrewer)
library(DT)
library(purrr)
library(stringr)

# ------------------- 数据加载与解析函数 -------------------

load_zhouyi_data <- function(file_path = NULL) {
  if (is.null(file_path)) {
    file_path <- "F:/slicef/ws/gitws/malonylcoa/src/test/resources/docs/texts/jingshi/zhouyi.txt"
  }
  
  lines <- readLines(file_path, encoding = "UTF-8")
  lines <- grep("(^$)|(图解)", lines, value = TRUE, invert = TRUE)
  
  ms <- grep("第[一二三四五六七八九十]+卦", lines)
  ns <- grep("上(九|六)：", lines)
  
  xs <- mapply(function(s, e) lines[seq(s, e)], ms, ns, SIMPLIFY = FALSE)
  
  gua_names <- sapply(xs, function(x) {
    name_line <- x[1]
    gsub("第[一二三四五六七八九十]+卦\\s+([^\\s]+)\\s+.*", "\\1", name_line)
  })
  
  names(xs) <- gua_names
  
  # 提取卦象结构的成分&片段 (同原始代码的 pick 函数)
  pick <- function(ys, i = 1) {
    ys |> lapply(function(y) unlist(strsplit(y[1], "\\s+"))[i])
  }
  
  # 提取卦象结构的第4列（上下卦信息）
  structure_info <- pick(xs, 4) |> unlist()
  names(structure_info) <- gua_names
  
  # 八卦编码
  trigrams <- list(
    坤 = c(0,0,0), 震 = c(0,0,1), 坎 = c(0,1,0), 兑 = c(0,1,1),
    艮 = c(1,0,0), 离 = c(1,0,1), 巽 = c(1,1,0), 乾 = c(1,1,1)
  )
  
  # 构建六爻矩阵
  ynm <- list(X1 = "初爻", X2 = "二爻", X3 = "三爻", 
              X4 = "四爻", X5 = "五爻", X6 = "上爻")
  
  # 提取太极结构
  ys <- strsplit(structure_info, "上|下") |> 
    lapply(function(i) {
      trigrams[i]
    })
  
  # 构建数据框
  zs <- lapply(ys, unlist) |> data.frame()
  attr(zs, "row.names") <- rev(names(ynm))
  
  # 转置得到爻辞结构
  yaos <- t(zs)
  # 确保行名正确
  rownames(yaos) <- gua_names
  colnames(yaos) <- c("X1", "X2", "X3", "X4", "X5", "X6")
  
  # 提取卦象的函数
  guaget <- function(gua, pattern = ".") {
    xs %>% getElement(gua) %>% grep(pattern, x = ., value = TRUE)
  }
  
  # 解析每个卦的文本内容
  parse_gua <- function(gua_content, gua_name) {
    # 卦辞（第2行）
    gua_text <- if (length(gua_content) >= 2) gua_content[2] else ""
    
    # 彖辞（以"彖"开头的行）
    tuan_text <- grep("^彖", gua_content, value = TRUE)
    tuan_text <- if (length(tuan_text) > 0) tuan_text[1] else ""
    
    # 象辞（以"象"开头的行，但要排除爻辞中的象辞）
    xiang_text <- grep("^象", gua_content, value = TRUE)
    if (length(xiang_text) > 1) {
      xiang_text <- xiang_text[1]
    }
    xiang_text <- if (length(xiang_text) > 0) xiang_text[1] else ""
    
    # 爻辞：使用正确的正则表达式匹配
    yao_lines <- tryCatch({
      guaget(gua_name, "^([初上][九六]|[九六][二三四五])")
    }, error = function(e) {
      character(0)
    })
    
    # 解析每个爻位
    yao_positions <- c("初", "二", "三", "四", "五", "上")
    yao_text <- list()
    
    for (pos in yao_positions) {
      if (pos == "初" || pos == "上") {
        pattern <- paste0("^", pos, "[九六]")
      } else {
        pattern <- paste0("^[九六]", pos)
      }
      matched <- grep(pattern, yao_lines, value = TRUE)
      
      if (length(matched) > 0) {
        content <- gsub(paste0("^", pos, "[九六]："), "", matched[1])
        content <- gsub("\\s+", " ", content)
        yao_text[[pos]] <- trimws(content)
      } else {
        yao_text[[pos]] <- ""
      }
    }
    
    names(yao_text) <- c("初爻", "二爻", "三爻", "四爻", "五爻", "上爻")
    
    return(list(
      gua_name = gua_name,
      gua_text = gua_text,
      tuan_text = tuan_text,
      xiang_text = xiang_text,
      yao_text = yao_text,
      full_text = gua_content
    ))
  }
  
  parsed_data <- list()
  for (name in gua_names) {
    parsed_data[[name]] <- parse_gua(xs[[name]], name)
  }
  
  return(list(
    xs = xs,
    parsed = parsed_data,
    yaos = yaos,
    gua_names = gua_names,
    trigrams = trigrams,
    guaget = guaget,
    structure_info = structure_info
  ))
}

# ------------------- 测试函数 -------------------

test_parse <- function() {
  data <- load_zhouyi_data()
  
  cat("=== 六爻结构验证 ===\n")
  print(data$yaos)
  
  cat("\n=== 初九卦 ===\n")
  print(data$yaos[data$yaos[, "X1"] == 1, ])
  
  cat("\n=== 上九卦 ===\n")
  print(data$yaos[data$yaos[, "X6"] == 1, ])
  
  return(data)
}

# ------------------- 可视化函数 -------------------

# 单个卦象绘制
plot_single_gua <- function(gua_name, yaos_matrix, gua_data) {
  if (!gua_name %in% rownames(yaos_matrix)) {
    return(ggplot() + labs(title = "未找到该卦") + theme_void())
  }
  
  gua_values <- yaos_matrix[gua_name, ]
  
  # 根据实际阴阳确定爻位标签
  yao_labels <- c("上", "五", "四", "三", "二", "初")
  yao_type <- ifelse(gua_values == 1, "九", "六")
  yao_full_labels <- paste0(yao_labels, yao_type)
  
  yao_text <- gua_data$yao_text
  yao_text_values <- unlist(yao_text)
  names(yao_text_values) <- names(yao_text)
  
  df <- data.frame(
    yao = 6:1,
    value = as.numeric(gua_values),
    label = yao_full_labels,
    yao_name = c("上爻", "五爻", "四爻", "三爻", "二爻", "初爻")
  )
  
  df$yao_text_short <- sapply(df$yao_name, function(name) {
    if (name %in% names(yao_text_values)) {
      text <- yao_text_values[name]
      if (nchar(text) > 0) {
        if (nchar(text) > 30) paste0(substr(text, 1, 28), "...") else text
      } else {
        ""
      }
    } else {
      ""
    }
  })
  
  p <- ggplot(df, aes(x = 1, y = yao)) +
    geom_rect(aes(xmin = -1.2, xmax = 3.2, ymin = 0.5, ymax = 6.5),
              fill = NA, color = "#8B4513", linewidth = 1.5, linetype = "solid") +
    geom_tile(aes(fill = factor(value)), 
              width = 3.7, height = 0.8, color = "#2C3E50", linewidth = 1.2) +
    geom_text(aes(label = ifelse(value == 1, "—", "––")), 
              x = 1, size = 20, 
              color = ifelse(df$value == 1, "white", "#2C3E50"),
              fontface = "bold") +
    geom_text(aes(label = label), x = 2.4, hjust = 0, 
              size = 4.5, color = "#6d8688", fontface = "bold") +
    geom_text(aes(label = yao_text_short), 
              x = -0.8, hjust = 0, size = 5, color = "#7F8C8D") +
    scale_fill_manual(values = c("0" = "#F5E6D3", "1" = "#2C3E50"),
                      labels = c("阴 (––)", "阳 (—)")) +
    labs(title = paste0("「", gua_name, "」卦 · 六爻结构"),
         subtitle = paste("卦序:", which(rownames(yaos_matrix) == gua_name), "/", 
                         nrow(yaos_matrix)),
         x = NULL, y = NULL) +
    theme_minimal() +
    theme(
      axis.text = element_blank(),
      axis.ticks = element_blank(),
      panel.grid = element_blank(),
      legend.position = "bottom",
      legend.title = element_blank(),
      legend.text = element_text(size = 12),
      plot.title = element_text(size = 22, hjust = 0.5, face = "bold", 
                                color = "#2C3E50"),
      plot.subtitle = element_text(size = 14, hjust = 0.5, color = "#7F8C8D"),
      plot.background = element_rect(fill = "#FDFBF7", color = NA),
      plot.margin = margin(20, 40, 20, 40)
    ) +
    coord_fixed(ratio = 0.4) +
    scale_x_continuous(limits = c(-1.5, 3.5))
  
  return(p)
}

# 六十四卦热图
plot_gua_heatmap <- function(yaos_matrix, cluster = FALSE) {
  data_matrix <- t(yaos_matrix)
  colnames(data_matrix) <- rownames(yaos_matrix)
  rownames(data_matrix) <- c("初爻", "二爻", "三爻", "四爻", "五爻", "上爻")
  
  if (cluster) {
    hc <- hclust(dist(t(data_matrix)))
    data_matrix <- data_matrix[, hc$order]
  }
  
  df <- melt(data_matrix)
  colnames(df) <- c("爻位", "卦名", "阴阳")
  df$阴阳 <- factor(df$阴阳, levels = c(0, 1), labels = c("阴", "阳"))
  df$爻位 <- factor(df$爻位, levels = c("上爻", "五爻", "四爻", "三爻", "二爻", "初爻"))
  
  p <- ggplot(df, aes(x = 卦名, y = 爻位, fill = 阴阳)) +
    geom_tile(color = "white", linewidth = 0.3) +
    scale_fill_manual(values = c("阴" = "#F5E6D3", "阳" = "#2C3E50")) +
    labs(title = "六十四卦爻位全景图",
         subtitle = if(cluster) "按相似度聚类排序" else "按卦序排列",
         x = NULL, y = NULL) +
    theme_minimal() +
    theme(
      axis.text.x = element_text(angle = 90, hjust = 1, vjust = 0.5, size = 7),
      axis.text.y = element_text(size = 11, face = "bold"),
      panel.grid = element_blank(),
      plot.title = element_text(size = 16, hjust = 0.5, face = "bold", color = "#2C3E50"),
      plot.subtitle = element_text(size = 12, hjust = 0.5, color = "#7F8C8D"),
      legend.position = "bottom",
      legend.title = element_blank()
    )
  
  return(p)
}

# 爻位分布统计
plot_yao_distribution <- function(yaos_matrix) {
  yang_count <- colSums(yaos_matrix == 1)
  yin_count <- colSums(yaos_matrix == 0)
  
  df <- data.frame(
    爻位 = factor(c("初爻", "二爻", "三爻", "四爻", "五爻", "上爻"),
                 levels = c("初爻", "二爻", "三爻", "四爻", "五爻", "上爻")),
    阳爻 = yang_count,
    阴爻 = yin_count
  )
  
  df_long <- melt(df, id.vars = "爻位", variable.name = "阴阳", value.name = "数量")
  df_long$阴阳 <- factor(df_long$阴阳, levels = c("阴爻", "阳爻"))
  
  p <- ggplot(df_long, aes(x = 爻位, y = 数量, fill = 阴阳)) +
    geom_bar(stat = "identity", position = "dodge", width = 0.7) +
    geom_text(aes(label = 数量), position = position_dodge(width = 0.7), 
              vjust = -0.3, size = 4) +
    scale_fill_manual(values = c("阳爻" = "#E74C3C", "阴爻" = "#3498DB")) +
    labs(title = "各爻位阴阳分布统计",
         subtitle = paste("共", nrow(yaos_matrix), "卦 · 总阳爻:", sum(yaos_matrix == 1), 
                         " · 总阴爻:", sum(yaos_matrix == 0)),
         x = NULL, y = "卦数") +
    theme_minimal() +
    theme(
      plot.title = element_text(size = 16, hjust = 0.5, face = "bold", color = "#2C3E50"),
      plot.subtitle = element_text(size = 12, hjust = 0.5, color = "#7F8C8D"),
      legend.position = "bottom",
      legend.title = element_blank(),
      axis.text = element_text(size = 11, color = "#34495E")
    )
  
  return(p)
}

# 关键词分布
plot_keyword_distribution <- function(keyword, parsed_data) {
  matches <- sapply(parsed_data, function(x) {
    grepl(keyword, x$gua_text) || grepl(keyword, x$tuan_text) || 
      grepl(keyword, x$xiang_text)
  })
  
  if (sum(matches) == 0) {
    return(ggplot() + labs(title = paste("未找到包含关键词 '", keyword, "' 的卦", sep = "")) + 
             theme_void())
  }
  
  df <- data.frame(
    卦名 = names(matches),
    包含 = ifelse(matches, "是", "否")
  )
  
  df <- df[order(df$卦名, decreasing = TRUE), ]
  df$order <- 1:nrow(df)
  
  p <- ggplot(df, aes(x = 1, y = reorder(卦名, order), fill = 包含)) +
    geom_tile(color = "white", linewidth = 0.5, width = 0.8, height = 0.8) +
    scale_fill_manual(values = c("是" = "#D62728", "否" = "#E8E8E8")) +
    geom_text(aes(label = 卦名), hjust = 0.5, size = 3) +
    labs(title = paste("关键词 '", keyword, "' 分布", sep = ""),
         subtitle = paste("出现于", sum(matches), "个卦中"),
         x = NULL, y = NULL) +
    theme_minimal() +
    theme(
      axis.text = element_blank(),
      axis.ticks = element_blank(),
      panel.grid = element_blank(),
      plot.title = element_text(size = 14, hjust = 0.5, face = "bold"),
      plot.subtitle = element_text(size = 11, hjust = 0.5),
      legend.position = "bottom",
      legend.title = element_blank()
    )
  
  return(p)
}

# 上下卦组合热图
plot_gua_network <- function(parsed_data, yaos_matrix, structure_info) {
  get_structure <- function(info) {
    parts <- strsplit(info, "上|下")[[1]]
    if (length(parts) >= 2) {
      return(c(parts[2], parts[1]))
    }
    return(c(info, info))
  }
  
  structures <- sapply(structure_info, get_structure)
  df <- data.frame(
    下卦 = structures[1, ],
    上卦 = structures[2, ],
    卦名 = names(parsed_data)
  )
  
  p <- ggplot(df, aes(x = 上卦, y = 下卦)) +
    geom_bin2d() +
    scale_fill_gradient(low = "#ECF0F1", high = "#E74C3C", guide = "colorbar") +
    geom_text(aes(label = after_stat(count)), stat = "bin2d", 
              size = 3, color = "#2C3E50") +
    labs(title = "上下卦组合热图",
         subtitle = "颜色越深表示组合越常见",
         x = "上卦", y = "下卦") +
    theme_minimal() +
    theme(
      plot.title = element_text(size = 16, hjust = 0.5, face = "bold", color = "#2C3E50"),
      plot.subtitle = element_text(size = 12, hjust = 0.5, color = "#7F8C8D"),
      axis.text = element_text(size = 11, color = "#34495E"),
      legend.position = "bottom",
      legend.title = element_blank()
    )
  
  return(p)
}

# ------------------- UI 设计 -------------------

ui <- fluidPage(
  theme = shinytheme("cosmo"),
  
  tags$head(
    tags$style(HTML("
      @import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;700&display=swap');
      
      body {
        font-family: 'Noto Serif SC', serif;
        background-color: #FDFBF7;
      }
      
      .main-header {
        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
        color: #FDFBF7;
        padding: 25px 30px;
        border-radius: 8px;
        margin-bottom: 25px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.3);
        border-left: 6px solid #E8C87A;
        position: relative;
      }
      .main-header h1 {
        margin: 0;
        font-weight: 700;
        font-size: 32px;
        letter-spacing: 4px;
      }
      .main-header h1 small {
        font-size: 16px;
        opacity: 0.8;
        margin-left: 15px;
        font-weight: 400;
      }
      .main-header p {
        margin: 8px 0 0 0;
        opacity: 0.8;
        font-size: 14px;
        letter-spacing: 2px;
      }
      .exit-btn {
        position: absolute;
        right: 20px;
        top: 50%;
        transform: translateY(-50%);
        background-color: rgba(255,255,255,0.15);
        color: white;
        border: 1px solid rgba(255,255,255,0.3);
        padding: 8px 20px;
        border-radius: 20px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.3s;
      }
      .exit-btn:hover {
        background-color: rgba(255,255,255,0.3);
        border-color: rgba(255,255,255,0.6);
      }
      
      .well {
        background-color: #FFFFFF;
        border: 1px solid #E8E8E8;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.06);
        padding: 20px;
      }
      
      .info-box {
        background: #F8F6F0;
        border-left: 4px solid #E8C87A;
        padding: 15px 20px;
        margin: 10px 0;
        border-radius: 4px;
        max-height: 150px;
        overflow-y: auto;
      }
      .info-box pre {
        white-space: pre-wrap;
        word-wrap: break-word;
        font-size: 13px;
        margin: 0;
        background: transparent;
        border: none;
        padding: 0;
      }
      
      .stat-box {
        background: linear-gradient(135deg, #FFFFFF 0%, #F8F6F0 100%);
        padding: 15px 20px;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.06);
        text-align: center;
        border: 1px solid #EDE8DC;
        margin: 5px 0;
      }
      .stat-box .number {
        font-size: 30px;
        font-weight: 700;
        color: #1a1a2e;
      }
      .stat-box .label {
        color: #7F8C8D;
        font-size: 13px;
        letter-spacing: 1px;
      }
      
      .btn-primary {
        background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%);
        border: none;
        font-weight: 600;
        letter-spacing: 1px;
      }
      .btn-primary:hover {
        background: linear-gradient(135deg, #2c2c4e 0%, #1a4a7a 100%);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
      }
      
      .btn-success {
        background: linear-gradient(135deg, #2C3E50 0%, #3498DB 100%);
        border: none;
        font-weight: 600;
      }
      
      .btn-danger {
        background: linear-gradient(135deg, #c0392b 0%, #e74c3c 100%);
        border: none;
        font-weight: 600;
      }
      .btn-danger:hover {
        background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
        transform: translateY(-1px);
      }
      
      .nav-tabs > li > a {
        font-weight: 600;
        color: #2C3E50;
        font-size: 15px;
      }
      .nav-tabs > li.active > a {
        color: #1a1a2e;
        border-bottom: 3px solid #E8C87A;
      }
      
      .shiny-output-error {
        background: #FFF5F5;
        border: 1px solid #FFE0E0;
        border-radius: 4px;
        padding: 15px;
      }
    "))
  ),
  
  div(class = "main-header",
      h1("《周易》", tags$small("I Ching · 卦象探索系统")),
      p("基于「数 · 象 · 辞」结构的交互式可视化平台 · 真实文本解析"),
      actionButton("exit_app", "🚪 退出", class = "exit-btn")
  ),
  
  navbarPage(
    title = NULL,
    id = "main_nav",
    selected = "卦象浏览",
    collapsible = TRUE,
    
    # ==================== 标签1: 卦象浏览 ====================
    tabPanel(
      "卦象浏览",
      icon = icon("book"),
      fluidRow(
        column(5,
          wellPanel(
            h4("📖 选择卦象", style = "color: #1a1a2e; font-weight: 700;"),
            selectInput("gua_select", NULL,
                        choices = NULL,
                        selected = NULL,
                        width = "100%"),
            hr(),
            div(class = "info-box",
                h5("📜 卦辞", style = "font-weight: 600; color: #2C3E50;"),
                verbatimTextOutput("gua_text_display", placeholder = TRUE)
            ),
            hr(),
            div(class = "info-box",
                h5("📝 彖辞", style = "font-weight: 600; color: #2C3E50;"),
                verbatimTextOutput("tuan_text_display", placeholder = TRUE)
            ),
            hr(),
            div(class = "info-box",
                h5("🌿 象辞", style = "font-weight: 600; color: #2C3E50;"),
                verbatimTextOutput("xiang_text_display", placeholder = TRUE)
            ),
            hr(),
            div(class = "info-box",
                h5("🔮 爻辞", style = "font-weight: 600; color: #2C3E50;"),
                verbatimTextOutput("yao_text_display", placeholder = TRUE)
            ),
            hr(),
            actionButton("random_gua", "🎲 随机一卦", 
                        class = "btn-primary btn-block")
          )
        ),
        column(7,
          fluidRow(
            column(4,
              div(class = "stat-box",
                  div(class = "number", textOutput("gua_index")),
                  div(class = "label", "卦序")
              )
            ),
            column(4,
              div(class = "stat-box",
                  div(class = "number", textOutput("gua_structure")),
                  div(class = "label", "上下卦结构")
              )
            ),
            column(4,
              div(class = "stat-box",
                  div(class = "number", textOutput("gua_yao_count")),
                  div(class = "label", "阳爻 / 阴爻")
              )
            )
          ),
          fluidRow(
            column(12,
              plotOutput("gua_plot", height = "600px")
            )
          )
        )
      )
    ),
    
    # ==================== 标签2: 全景分析 ====================
    tabPanel(
      "全景分析",
      icon = icon("th"),
      fluidRow(
        column(12,
          wellPanel(
            h4("🔍 六十四卦全景视图", style = "color: #1a1a2e; font-weight: 700;"),
            p("展示所有卦的爻位分布 · 白色 = 阴爻 · 黑色 = 阳爻", 
              style = "color: #7F8C8D;"),
            checkboxInput("cluster_heatmap", "按相似度聚类排序", value = FALSE),
            plotOutput("heatmap_plot", height = "600px")
          )
        )
      ),
      fluidRow(
        column(6,
          wellPanel(
            h4("📊 爻位统计", style = "color: #1a1a2e; font-weight: 700;"),
            plotOutput("distribution_plot", height = "400px")
          )
        ),
        column(6,
          wellPanel(
            h4("🔎 关键词搜索", style = "color: #1a1a2e; font-weight: 700;"),
            fluidRow(
              column(8, textInput("keyword_input", NULL, value = "元", 
                                  placeholder = "输入关键词")),
              column(4, actionButton("search_keyword", "搜索", 
                                     class = "btn-success", style = "margin-top: 25px;"))
            ),
            hr(),
            plotOutput("keyword_plot", height = "400px")
          )
        )
      )
    ),
    
    # ==================== 标签3: 网络分析 ====================
    tabPanel(
      "网络分析",
      icon = icon("project-diagram"),
      fluidRow(
        column(12,
          wellPanel(
            h4("🌐 上下卦组合网络", style = "color: #1a1a2e; font-weight: 700;"),
            p("展示八卦之间的组合关系 · 颜色深度表示组合频率", 
              style = "color: #7F8C8D;"),
            plotOutput("network_plot", height = "600px")
          )
        )
      ),
      fluidRow(
        column(6,
          wellPanel(
            h4("📋 卦象列表", style = "color: #1a1a2e; font-weight: 700;"),
            DT::dataTableOutput("gua_table")
          )
        ),
        column(6,
          wellPanel(
            h4("📈 组合统计", style = "color: #1a1a2e; font-weight: 700;"),
            verbatimTextOutput("network_stats")
          )
        )
      )
    ),
    
    # ==================== 标签4: 关键词分析 ====================
    tabPanel(
      "关键词分析",
      icon = icon("chart-bar"),
      fluidRow(
        column(12,
          wellPanel(
            h4("📝 高频词分析", style = "color: #1a1a2e; font-weight: 700;"),
            p("分析卦辞、彖辞、象辞中的高频词汇", style = "color: #7F8C8D;"),
            actionButton("analyze_freq", "运行词频分析", class = "btn-primary"),
            hr(),
            plotOutput("wordcloud_plot", height = "400px"),
            DT::dataTableOutput("freq_table")
          )
        )
      ),
      fluidRow(
        column(12,
          wellPanel(
            h4("☯ 元亨利贞分布", style = "color: #1a1a2e; font-weight: 700;"),
            fluidRow(
              lapply(c("元", "亨", "利", "贞"), function(kw) {
                column(3,
                  div(class = "stat-box",
                    div(class = "number", textOutput(paste0("count_", kw))),
                    div(class = "label", paste0("'", kw, "' 出现次数"))
                  )
                )
              })
            ),
            plotOutput("yhli_distribution", height = "300px")
          )
        )
      )
    ),
    
    # ==================== 标签5: 关于 ====================
    tabPanel(
      "关于",
      icon = icon("info-circle"),
      fluidRow(
        column(8, offset = 2,
          wellPanel(
            h3("📚 关于本系统", style = "color: #1a1a2e; font-weight: 700;"),
            hr(),
            h4("设计理念", style = "color: #2C3E50;"),
            p("本系统将《周易》视为一种前信息化时代的'语言模型'，"),
            p("以「数 · 象 · 辞」结构为基础，剥离后世伦理阐释，"),
            p("还原其作为风险联想与决策媒体的本相。"),
            br(),
            h4("数据结构", style = "color: #2C3E50;"),
            tags$ul(
              tags$li("📌 卦名 = Token（离散符号入口）"),
              tags$li("📊 六爻 = Embedding Vector（嵌入向量）"),
              tags$li("📜 卦辞 = 主要文本内容"),
              tags$li("📝 彖辞 = 彖曰注释"),
              tags$li("🌿 象辞 = 象曰注释（不含爻辞中的象）"),
              tags$li("🔮 爻辞 = 各爻位解释（含象曰）")
            ),
            br(),
            h4("数据来源", style = "color: #2C3E50;"),
            p("基于《经史/周易》文本解析生成"),
            p("文件路径: F:/slicef/ws/gitws/malonylcoa/src/test/resources/docs/texts/jingshi/zhouyi.txt"),
            br(),
            h4("技术栈", style = "color: #2C3E50;"),
            tags$ul(
              tags$li("R语言 + Shiny"),
              tags$li("ggplot2 / DT / reshape2")
            ),
            br(),
            h4("版本信息", style = "color: #2C3E50;"),
            p("版本 1.0 | 2026年"),
            p("🎯 探索《周易》的数字化之道")
          )
        )
      )
    )
  )
)

# ------------------- Server 逻辑 -------------------

server <- function(input, output, session) {
  
  # 退出应用
  observeEvent(input$exit_app, {
    showModal(modalDialog(
      title = "确认退出",
      "确定要退出《周易》探索系统吗？",
      footer = tagList(
        actionButton("confirm_exit", "确定退出", class = "btn-danger"),
        modalButton("取消")
      )
    ))
  })
  
  observeEvent(input$confirm_exit, {
    removeModal()
    stopApp("用户主动退出")
  })
  
  data <- reactive({
    withProgress(message = "加载《周易》数据...", {
      load_zhouyi_data()
    })
  })
  
  observe({
    gua_names <- data()$gua_names
    updateSelectInput(session, "gua_select", 
                      choices = gua_names,
                      selected = gua_names[1])
  })
  
  current_gua <- reactive({
    input$gua_select
  })
  
  observeEvent(input$random_gua, {
    gua_names <- data()$gua_names
    selected <- sample(gua_names, 1)
    updateSelectInput(session, "gua_select", selected = selected)
  })
  
  # ---------- 卦象浏览 ----------
  
  output$gua_plot <- renderPlot({
    req(current_gua())
    gua_data <- data()$parsed[[current_gua()]]
    plot_single_gua(current_gua(), data()$yaos, gua_data)
  }, width = 800, height = 600)
  
  output$gua_text_display <- renderText({
    req(current_gua())
    text <- data()$parsed[[current_gua()]]$gua_text
    if (is.null(text) || text == "" || length(text) == 0) "暂无卦辞" else text
  })
  
  output$tuan_text_display <- renderText({
    req(current_gua())
    text <- data()$parsed[[current_gua()]]$tuan_text
    if (is.null(text) || length(text) == 0 || text == "") "暂无彖辞" else text
  })
  
  output$xiang_text_display <- renderText({
    req(current_gua())
    text <- data()$parsed[[current_gua()]]$xiang_text
    if (is.null(text) || length(text) == 0 || text == "") "暂无象辞" else text
  })
  
  output$yao_text_display <- renderText({
    req(current_gua())
    yao_text <- data()$parsed[[current_gua()]]$yao_text
    if (is.null(yao_text) || length(yao_text) == 0) {
      "暂无爻辞"
    } else {
      result <- ""
      for (name in names(yao_text)) {
        content <- yao_text[[name]]
        if (nchar(content) > 0) {
          result <- paste0(result, name, ": ", content, "\n")
        } else {
          result <- paste0(result, name, ": (无)\n")
        }
      }
      trimws(result)
    }
  })
  
  output$gua_index <- renderText({
    req(current_gua())
    idx <- which(data()$gua_names == current_gua())
    if (length(idx) > 0) {
      paste(idx, "/", nrow(data()$yaos))
    } else {
      "未知"
    }
  })
  
  output$gua_structure <- renderText({
    req(current_gua())
    idx <- which(data()$gua_names == current_gua())
    if (length(idx) > 0) {
      info <- data()$structure_info[idx]
      parts <- strsplit(info, "上|下")[[1]]
      if (length(parts) >= 2) {
        paste(parts[1], "→", parts[2])
      } else {
        "未知结构"
      }
    } else {
      "未知"
    }
  })
  
  output$gua_yao_count <- renderText({
    req(current_gua())
    # 使用 match 确保正确获取行索引
    idx <- match(current_gua(), rownames(data()$yaos))
    if (!is.na(idx)) {
      yao <- data()$yaos[idx, ]
      yang <- sum(yao == 1, na.rm = TRUE)
      yin <- sum(yao == 0, na.rm = TRUE)
      paste(yang, "/", yin)
    } else {
      "0 / 0"
    }
  })
  
  # ---------- 全景分析 ----------
  
  output$heatmap_plot <- renderPlot({
    plot_gua_heatmap(data()$yaos, input$cluster_heatmap)
  }, width = 1000, height = 600)
  
  output$distribution_plot <- renderPlot({
    plot_yao_distribution(data()$yaos)
  }, width = 600, height = 400)
  
  observeEvent(input$search_keyword, {
    output$keyword_plot <- renderPlot({
      req(input$keyword_input)
      plot_keyword_distribution(input$keyword_input, data()$parsed)
    }, width = 400, height = 500)
  })
  
  # ---------- 网络分析 ----------
  
  output$network_plot <- renderPlot({
    plot_gua_network(data()$parsed, data()$yaos, data()$structure_info)
  }, width = 700, height = 600)
  
  output$gua_table <- DT::renderDataTable({
    df <- data.frame(
      序号 = 1:length(data()$gua_names),
      卦名 = data()$gua_names,
      卦辞 = sapply(data()$parsed, function(x) x$gua_text),
      彖辞 = sapply(data()$parsed, function(x) {
        if (nchar(x$tuan_text) > 0) substr(x$tuan_text, 1, 30) else ""
      })
    )
    DT::datatable(df, options = list(
      pageLength = 20,
      scrollY = "300px"
    ), rownames = FALSE)
  })
  
  output$network_stats <- renderPrint({
    get_structure <- function(info) {
      parts <- strsplit(info, "上|下")[[1]]
      if (length(parts) >= 2) {
        return(c(parts[2], parts[1]))
      }
      return(c(info, info))
    }
    
    structures <- sapply(data()$structure_info, get_structure)
    comb_str <- paste(structures[1, ], structures[2, ], sep = "→")
    freq <- sort(table(comb_str), decreasing = TRUE)
    
    cat("═══ 上下卦组合统计 ═══\n\n")
    cat("📊 总卦数:", nrow(data()$yaos), "\n")
    cat("🔢 八卦种类: 8\n\n")
    
    cat("🏆 最常见的5个组合:\n")
    print(head(freq, 5))
    
    cat("\n📈 各卦出现频率（上卦）:\n")
    print(sort(table(structures[2, ]), decreasing = TRUE))
    
    cat("\n📈 各卦出现频率（下卦）:\n")
    print(sort(table(structures[1, ]), decreasing = TRUE))
  })
  
  # ---------- 关键词分析 ----------
  
  observeEvent(input$analyze_freq, {
    all_text <- ""
    for (gua in data()$parsed) {
      all_text <- paste(all_text, gua$gua_text, gua$tuan_text, 
                        gua$xiang_text, 
                        paste(unlist(gua$yao_text), collapse = ""))
    }
    
    chars <- strsplit(all_text, "")[[1]]
    chars <- chars[nchar(chars) > 0 & chars != "：" & chars != "。" & 
                     chars != "，" & chars != "、" & chars != " "]
    
    freq_table <- sort(table(chars), decreasing = TRUE)
    freq_df <- data.frame(
      字符 = names(freq_table),
      频次 = as.numeric(freq_table)
    )
    
    output$freq_table <- DT::renderDataTable({
      DT::datatable(freq_df[1:30, ], 
                    options = list(pageLength = 30),
                    rownames = FALSE)
    })
    
    output$wordcloud_plot <- renderPlot({
      top_n <- min(25, nrow(freq_df))
      df_plot <- freq_df[1:top_n, ]
      df_plot$字符 <- factor(df_plot$字符, levels = rev(df_plot$字符))
      
      ggplot(df_plot, aes(x = 字符, y = 频次)) +
        geom_bar(stat = "identity", fill = "#E8C87A", color = "#B89B5E", 
                 width = 0.7) +
        geom_text(aes(label = 频次), hjust = -0.2, size = 3.5, color = "#2C3E50") +
        coord_flip() +
        labs(title = "卦辞高频词 TOP 25", 
             x = NULL, y = "频次") +
        theme_minimal() +
        theme(
          plot.title = element_text(size = 16, hjust = 0.5, face = "bold", 
                                   color = "#1a1a2e"),
          axis.text = element_text(size = 11, color = "#34495E"),
          panel.grid.major.y = element_blank(),
          panel.grid.minor = element_blank()
        )
    }, width = 600, height = 400)
  })
  
  # 元亨利贞统计
  observe({
    all_text <- ""
    for (gua in data()$parsed) {
      all_text <- paste(all_text, gua$gua_text, gua$tuan_text, gua$xiang_text)
    }
    
    for (kw in c("元", "亨", "利", "贞")) {
      count <- str_count(all_text, kw)
      output[[paste0("count_", kw)]] <- renderText({ count })
    }
  })
  
  output$yhli_distribution <- renderPlot({
    all_text <- ""
    for (gua in data()$parsed) {
      all_text <- paste(all_text, gua$gua_text, gua$tuan_text, gua$xiang_text)
    }
    
    df <- data.frame(
      关键词 = c("元", "亨", "利", "贞"),
      频次 = c(
        str_count(all_text, "元"),
        str_count(all_text, "亨"),
        str_count(all_text, "利"),
        str_count(all_text, "贞")
      )
    )
    
    colors <- c("#E8C87A", "#2C3E50", "#E74C3C", "#3498DB")
    
    ggplot(df, aes(x = 关键词, y = 频次, fill = 关键词)) +
      geom_bar(stat = "identity", width = 0.6) +
      geom_text(aes(label =频次), vjust = -0.8, size = 6, fontface = "bold") +
      scale_fill_manual(values = colors, guide = "none") +
      labs(title = "「元亨利贞」在卦辞中的出现频次",
           x = NULL, y = "频次") +
      theme_minimal() +
      theme(
        plot.title = element_text(size = 16, hjust = 0.5, face = "bold", 
                                 color = "#1a1a2e"),
        axis.text = element_text(size = 13, color = "#34495E", face = "bold"),
        panel.grid.major.x = element_blank()
      )
  }, width = 500, height = 300)
}

# ------------------- 启动应用 -------------------

shinyApp(ui = ui, server = server)
