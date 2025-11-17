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
  lines <- if (length(input)==1 && file.exists(input)) readLines(input, warn = FALSE) else
    if (is.character(input)) input else stop("Input must be a valid file path or character vector")
  
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


#' Fill SQL Template with Parameters
#'
#' Replaces parameter placeholders in SQL templates with actual values.
#' Supports different formatting based on parameter prefix:
#' - `##param` parameters are inserted without quotes (for table/column names)
#' - `#param` parameters are wrapped in quotes (for string values)
#'
#' @param template Character string containing the SQL template with parameter placeholders
#' @param params Named list of parameters where names are parameter placeholders and values are replacements
#'
#' @return Character string with parameters substituted
#'
#' @examples
#' \dontrun{
#' # SQL template with parameters
#' sql_template <- "SELECT * FROM ##table WHERE date >= #start_date AND status = #status"
#' 
#' # Parameters
#' params <- list(
#'   "##table" = "users",
#'   "#start_date" = "2024-01-01", 
#'   "#status" = "active"
#' )
#' 
#' # Fill template
#' filled_sql <- fill(sql_template, params)
#' cat(filled_sql)
#' # Output: SELECT * FROM users WHERE date >= '2024-01-01' AND status = 'active'
#' }
#'
#' @export
fill <- function(template, params) {
  if (length(params) == 0) {
    return(template)
  }
  
  Reduce(
    f = function(acc, kv) {
      pattern <- kv[1]
      replacement <- kv[2]
      
      # Determine formatting based on parameter prefix
      format_spec <- switch(
        sub("^\\s*(#+)[^#]+", "\\1", pattern),
        "##" = "%s",  # No quotes for table/column names
        "'%s'"        # Quotes for string values
      )
      
      gsub(pattern, sprintf(format_spec, replacement), acc, fixed = TRUE)
    },
    x = mapply(c, names(params), params, SIMPLIFY = FALSE),
    init = template
  )
}