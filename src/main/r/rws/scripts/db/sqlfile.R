#' Parse SQL Script File into Named SQL Statements List
#'
#' This function reads a SQL script file and parses it into a named list of SQL statements.
#' SQL scripts use special comment formats to identify different SQL statement blocks.
#'
#' @param input Character vector, either a file path or a character vector containing SQL script content
#'
#' @return A named list where names are SQL statement titles and values are corresponding SQL statement content
#'
#' @details
#' SQL script format specification:
#' - Use `-- # Title Name` format to identify the start of SQL statement blocks
#' - `-- #param` or `-- ##param` formats are treated as parameter comments and not recognized as titles
#' - Supports inline comments (using `--` or `//`)
#' - Automatically filters empty lines and pure comment lines
#'
#' The function uses functional programming with `Reduce` for efficient processing and state management.
#'
#' @examples
#' \dontrun{
#' # Read from file
#' sql_list <- script_file("path/to/sql_scripts.sql")
#'
#' # Read from character vector
#' sql_content <- c(
#'   "-- # User Query",
#'   "SELECT * FROM users",
#'   "WHERE active = TRUE",
#'   "",
#'   "-- # Product Stats",
#'   "SELECT category, COUNT(*) as count",
#'   "FROM products",
#'   "GROUP BY category"
#' )
#' sql_list <- script_file(sql_content)
#'
#' # Use parsed SQL statements
#' users_sql <- sql_list[["User Query"]]
#' products_sql <- sql_list[["Product Stats"]]
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
