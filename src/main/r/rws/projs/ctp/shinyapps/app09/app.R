# 安装必需包（第一次运行时执行，之后可注释）
# install.packages(c("shiny", "ssh", "dplyr", "future", "future.apply"))

# 加载包（关键：必须加载 future.apply）
library(shiny)
library(ssh)
library(future)
library(future.apply) # 核心修复：加载 future_lapply 所在包

# 配置并发（可选，指定并发数）
plan(multisession, workers = 2) # 两台服务器，设2个并发足够

# 你的服务器清单
hosts <- c("192.168.1.10", "192.168.1.41")
user <- "root"
pwd  <- "123456"

# ------------------------------
# UI：网页界面
# ------------------------------
ui <- fluidPage(
  titlePanel("R Ansible · 批量 SSH 控制台"),
  sidebarLayout(
    sidebarPanel(
      textInput("cmd", "要执行的命令", "hostname && uptime"),
      actionButton("run", "批量执行", class = "btn-primary"),
      hr(),
      h4("目标服务器"),
      verbatimTextOutput("hostlist")
    ),
    mainPanel(
      h3("执行结果"),
      verbatimTextOutput("result")
    )
  )
)

# ------------------------------
# Server：后台逻辑（R 版 Ansible 核心）
# ------------------------------
server <- function(input, output) {
  # 显示服务器列表
  output$hostlist <- renderPrint({
    cat(paste0(hosts, collapse = "\n"))
  })
  
  # 点击执行按钮触发批量SSH
  observeEvent(input$run, {
    cmd <- input$cmd
    if (cmd == "") {
      output$result <- renderPrint(cat("请输入要执行的命令！"))
      return()
    }
    
    # 批量并发执行（修复：用 future.apply::future_lapply 显式调用）
    res <- future.apply::future_lapply(hosts, function(host) {
      tryCatch({
        # 建立SSH连接
        session <- ssh_connect(
          host = paste0(user, "@", host),
          passwd = pwd  # 注意：这里是 passwd，不是 password
        )
        # 执行命令并获取输出
        exec_res <- ssh_exec_internal(session, cmd)
        stdout <- rawToChar(exec_res$stdout)
        stderr <- rawToChar(exec_res$stderr)
        ssh_disconnect(session)
        
        # 拼接结果（区分标准输出/错误）
        out <- paste0("[", host, "] 执行成功：\n", stdout)
        if (stderr != "") out <- paste0(out, "\n[错误输出]：\n", stderr)
        out
      }, error = function(e) {
        # 捕获连接/执行错误
        paste0("[", host, "] 执行失败：\n", e$message)
      })
    })
    
    # 渲染结果
    output$result <- renderPrint({
      cat(paste(unlist(res), collapse = "\n\n--- 分割线 ---\n\n"))
    })
  })
}

# 启动Shiny App
shinyApp(ui, server)