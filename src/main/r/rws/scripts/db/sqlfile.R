#' 解析SQL脚本文件为命名SQL语句列表
#'
#' 该函数读取SQL脚本文件，将其解析为命名SQL语句的列表。SQL脚本使用特殊注释格式来标识不同的SQL语句块。
#'
#' @param input 字符向量，可以是文件路径或包含SQL脚本内容的字符向量
#'
#' @return 一个命名列表，其中名称是SQL语句的标题，值是相应的SQL语句内容
#'
#' @details
#' SQL脚本格式说明：
#' - 使用 `-- # 标题名` 格式来标识SQL语句块的开始
#' - `-- #参数名` 或 `-- ##参数名` 格式被视为参数注释，不会被识别为标题
#' - 支持行尾注释（使用 `--` 或 `//`）
#' - 自动过滤空行和纯注释行
#'
#' @examples
#' \dontrun{
#' # 从文件读取
#' sql_list <- script_file("path/to/sql_scripts.sql")
#'
#' # 从字符向量读取
#' sql_content <- c(
#'   "-- # 用户查询",
#'   "SELECT * FROM users",
#'   "WHERE active = TRUE",
#'   "",
#'   "-- # 产品统计",
#'   "SELECT category, COUNT(*) as count",
#'   "FROM products",
#'   "GROUP BY category"
#' )
#' sql_list <- script_file(sql_content)
#'
#' # 使用解析后的SQL语句
#' users_sql <- sql_list[["用户查询"]]
#' products_sql <- sql_list[["产品统计"]]
#' }
#'
#' @export
sqlfile <- function(input) {
  lines <- if (file.exists(input)) readLines(input, warn = FALSE) else
    if (is.character(input)) input else stop("无效输入")
  
  result <- Reduce(\(acc, line) {
    if (grepl("^\\s*--\\s*#\\s+([^#\\s].*?)\\s*$", line)) {
      if (!is.null(acc$title)) acc$stmts[[acc$title]] <- paste(acc$buffer, collapse = "\n")
      list(stmts = acc$stmts, title = sub("^\\s*--\\s*#\\s+([^#\\s].*?)\\s*$", "\\1", line), buffer = character())
    } else if (!grepl("^\\s*--", line) && nchar(trimws(line)) > 0) {
      list(stmts = acc$stmts, title = acc$title, buffer = c(acc$buffer, sub("\\s*((--)|(//)).*$", "", line)))
    } else acc
  }, lines, init = list(stmts = list(), title = NULL, buffer = character()))
  
  if (!is.null(result$title)) result$stmts[[result$title]] <- paste(result$buffer, collapse = "\n")
  result$stmts
}
