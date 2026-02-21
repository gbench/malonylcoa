# 安装必需包（第一次运行）
# install.packages(c("shiny", "ssh", "future.apply", "httpuv"))

library(shiny)
library(ssh)
library(future.apply)
library(httpuv)

# 配置并发
plan(multisession, workers = 2)

# 服务器配置（可扩展）
servers <- list(
  "192.168.1.10" = list(user = "gbench", pwd = "123456"),
  "192.168.1.41" = list(user = "gbench", pwd = "123456")
)
hosts <- names(servers)

# ------------------------------
# UI：最终版（布局+输入完美）
# ------------------------------
ui <- fluidPage(
  titlePanel("R Ansible · 金融云作战室"),
  tags$head(
    # 引入Xterm.js和样式
    tags$link(href = "https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.min.css", rel = "stylesheet"),
    tags$script(src = "https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.min.js"),
    tags$script(src = "https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.min.js"),
    # CSS样式
    tags$style(HTML("
      #terminal-container {
        height: 500px;
        width: 100%;
        background-color: #1e1e1e;
        border-radius: 8px;
        padding: 10px;
        overflow: hidden;
        position: relative;
        cursor: text;
      }
      #terminal {
        height: 100% !important;
        width: 100% !important;
      }
      .sidebar-panel { background-color: #f8f9fa; padding: 20px; border-radius: 8px; }
      .btn { margin-right: 10px; }
      .tab-content { margin-top: 15px; }
    "))
  ),
  
  sidebarLayout(
    sidebarPanel(
      width = 3,
      h4("📌 服务器管理"),
      selectInput("selected_host", "选择服务器", choices = c("批量执行" = "batch", hosts), selected = "batch"),
      hr(),
      h4("⚡ 批量命令"),
      textInput("cmd", "输入命令", value = "hostname && uptime && free -h"),
      actionButton("run_batch", "批量执行", class = "btn-primary"),
      hr(),
      h4("📊 执行日志"),
      verbatimTextOutput("batch_result", placeholder = TRUE)
    ),
    
    mainPanel(
      tabsetPanel(
        tabPanel("交互式终端", 
                 tags$div(id = "terminal-container",
                          tags$div(id = "terminal")
                 ),
                 actionButton("focus_terminal", "点击聚焦终端", class = "btn-sm btn-secondary mt-2"),
                 verbatimTextOutput("terminal_status")
        ),
        tabPanel("服务器状态", 
                 h4("所有服务器负载"),
                 verbatimTextOutput("server_status")
        )
      )
    )
  ),
  
  # Xterm.js初始化脚本（修复特殊字符）
  tags$script(HTML('
    let term = null;
    let fitAddon = null;
    
    function initTerminal() {
      if (term) return;
      
      term = new Terminal({
        fontSize: 14,
        theme: {
          background: "#1e1e1e",
          foreground: "#ffffff",
          cursor: "#ffffff",
          selection: "#444444"
        },
        scrollback: 1000,
        cursorBlink: true,
        cols: 100,
        rows: 24,
        allowProposedApi: true
      });
      
      fitAddon = new FitAddon.FitAddon();
      term.loadAddon(fitAddon);
      
      const terminalEl = document.getElementById("terminal");
      term.open(terminalEl);
      fitAddon.fit();
      
      // 点击聚焦
      document.getElementById("terminal-container").addEventListener("click", function() {
        term.focus();
      });
      
      // 输入事件（仅传普通字符，避免特殊编码）
      term.onData(function(data) {
        Shiny.setInputValue("terminal_input", data, {priority: "event"});
      });
      
      setTimeout(() => term.focus(), 500);
    }

    document.addEventListener("DOMContentLoaded", function() {
      initTerminal();
    });
    
    window.addEventListener("resize", () => {
      if (fitAddon) fitAddon.fit();
    });
    
    Shiny.addCustomMessageHandler("terminal_output", function(data) {
      if (term) term.write(data);
    });
    
    Shiny.addCustomMessageHandler("terminal_clear", function() {
      if (term) term.clear();
    });
    
    Shiny.addCustomMessageHandler("terminal_focus", function() {
      if (term) term.focus();
    });
  '))
)

# ------------------------------
# Server：核心逻辑（修复所有报错）
# ------------------------------
server <- function(input, output, session) {
  # 响应式变量（统一管理状态）
  ssh_sessions <- reactiveValues(
    current = NULL,
    cmd = "",
    cwd = "~"
  )
  
  # 1. 批量命令执行
  observeEvent(input$run_batch, {
    exec_cmd <- input$cmd
    if (exec_cmd == "") {
      output$batch_result <- renderPrint(cat("⚠️ 请输入要执行的命令！"))
      return()
    }
    
    output$batch_result <- renderPrint(cat("🚀 正在批量执行命令...\n"))
    
    res <- future.apply::future_lapply(hosts, function(host) {
      cfg <- servers[[host]]
      tryCatch({
        session <- ssh_connect(
          host = paste0(cfg$user, "@", host),
          passwd = cfg$pwd
        )
        exec_res <- ssh_exec_internal(session, exec_cmd)
        ssh_disconnect(session)
        
        stdout <- rawToChar(exec_res$stdout)
        stderr <- rawToChar(exec_res$stderr)
        
        out <- paste0("✅ [", host, "] 执行成功：\n", stdout)
        if (stderr != "") out <- paste0(out, "\n❌ 错误输出：\n", stderr)
        out
      }, error = function(e) {
        paste0("❌ [", host, "] 执行失败：\n", e$message)
      })
    })
    
    output$batch_result <- renderPrint({
      cat(paste(unlist(res), collapse = "\n\n--- 分割线 ---\n\n"))
    })
    output$server_status <- renderPrint({
      cat(paste(unlist(res), collapse = "\n\n--- 分割线 ---\n\n"))
    })
  })
  
  # 2. 服务器选择+SSH连接（确保在响应式上下文内访问）
  observeEvent(input$selected_host, {
    # 断开旧会话（响应式上下文内操作）
    if (!is.null(ssh_sessions$current)) {
      try(ssh_disconnect(ssh_sessions$current), silent = TRUE)
      ssh_sessions$current <- NULL
    }
    
    host <- input$selected_host
    if (host == "batch") {
      output$terminal_status <- renderPrint(cat("ℹ️ 请选择具体服务器打开终端"))
      session$sendCustomMessage("terminal_clear", NULL)
      session$sendCustomMessage("terminal_output", "\r\nℹ️ 请选择具体服务器打开终端\r\n")
      return()
    }
    
    # 连接新服务器
    cfg <- servers[[host]]
    tryCatch({
      session$sendCustomMessage("terminal_clear", NULL)
      session$sendCustomMessage("terminal_output", paste0("\r\n🔌 正在连接 ", host, "...\r\n"))
      
      # 同步建立连接（响应式上下文内）
      ssh_sessions$current <- ssh_connect(
        host = paste0(cfg$user, "@", host),
        passwd = cfg$pwd
      )
      
      output$terminal_status <- renderPrint(cat(paste0("✅ 已连接 ", host, " (", cfg$user, ")")))
      session$sendCustomMessage("terminal_output", paste0("✅ 连接成功！", host, " ~ $ "))
      session$sendCustomMessage("terminal_focus", NULL)
      ssh_sessions$cmd <- ""
    }, error = function(e) {
      output$terminal_status <- renderPrint(cat(paste0("❌ 连接失败：", e$message)))
      session$sendCustomMessage("terminal_output", paste0("\r\n❌ 连接失败：", e$message, "\r\n"))
    })
  })
  
  # 3. 终端输入处理（核心修复：去掉特殊字符解析）
  observeEvent(input$terminal_input, {
    # 必须在响应式上下文内访问ssh_sessions$current
    req(ssh_sessions) # 确保变量存在
    
    input_data <- input$terminal_input
    # 无连接时提示
    if (is.null(ssh_sessions$current)) {
      session$sendCustomMessage("terminal_output", "\r\n❌ 请先选择并连接服务器！\r\n")
      return()
    }
    
    # 回车执行命令
    if (input_data == "\r") {
      cmd <- trimws(ssh_sessions$cmd)
      if (cmd == "") {
        session$sendCustomMessage("terminal_output", paste0("\r\n", ssh_sessions$cwd, " ~ $ "))
        ssh_sessions$cmd <- ""
        return()
      }
      
      # 执行命令
      tryCatch({
        exec_res <- ssh_exec_internal(ssh_sessions$current, cmd)
        stdout <- rawToChar(exec_res$stdout)
        stderr <- rawToChar(exec_res$stderr)
        
        session$sendCustomMessage("terminal_output", paste0("\r\n", stdout))
        if (stderr != "") session$sendCustomMessage("terminal_output", paste0("\r\n❌ ", stderr))
      }, error = function(e) {
        session$sendCustomMessage("terminal_output", paste0("\r\n❌ 执行失败：", e$message))
      })
      
      # 重置命令行
      session$sendCustomMessage("terminal_output", paste0("\r\n", ssh_sessions$cwd, " ~ $ "))
      ssh_sessions$cmd <- ""
    } 
    # 退格键（仅处理\b，去掉\x7f避免解析错误）
    else if (input_data == "\b") {
      if (nchar(ssh_sessions$cmd) > 0) {
        ssh_sessions$cmd <- substr(ssh_sessions$cmd, 1, nchar(ssh_sessions$cmd)-1)
        session$sendCustomMessage("terminal_output", "\b \b")
      }
    } 
    # 普通字符输入
    else {
      ssh_sessions$cmd <- paste0(ssh_sessions$cmd, input_data)
      session$sendCustomMessage("terminal_output", input_data)
    }
  })
  
  # 4. 手动聚焦终端
  observeEvent(input$focus_terminal, {
    session$sendCustomMessage("terminal_focus", NULL)
    output$terminal_status <- renderPrint(cat("ℹ️ 终端已聚焦，可输入命令"))
  })
  
  # 5. 初始化+关闭清理
  observe({
    session$sendCustomMessage("terminal_output", "📟 R Ansible 交互式终端\r\nℹ️ 请选择左侧服务器连接\r\n")
  })
  
  onStop(function() {
    # 确保在响应式上下文内清理
    if (!is.null(ssh_sessions$current)) {
      try(ssh_disconnect(ssh_sessions$current), silent = TRUE)
    }
  })
}

# 启动App
shinyApp(ui, server)
