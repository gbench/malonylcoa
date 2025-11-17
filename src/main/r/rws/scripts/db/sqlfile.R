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
#' sql_list <- sqlfile("path/to/sql_scripts.sql")
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
#' sql_list <- sqlfile(sql_content)
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


#' Get Package SQL Template Files
#'
#' Internal function to get the path to SQL template files included in the package.
#'
#' @return Character vector of file paths to package SQL templates
#' @keywords internal
.get_package_sql_files <- function() {
  package_dir <- system.file("sql", package = "malonylcoa")
  if (nchar(package_dir) > 0 && dir.exists(package_dir)) {
    list.files(package_dir, pattern = "\\.sql$", full.names = TRUE)
  } else {
    character(0)
  }
}


#' Execute Parameterized SQL Query and Return Data Frame
#'
#' This function searches for SQL templates in workspace files, parses them, 
#' replaces parameters, and executes the query returning results as a data frame.
#' When `name` is a SQL statement (starting with SQL keywords), it directly 
#' processes the SQL without file lookup.
#'
#' @param name Character string specifying either:
#'   - Name of SQL statement in template files, or
#'   - Actual SQL statement (if it starts with SQL keywords like SELECT, INSERT, etc.)
#' @param params Named list of parameters for template substitution (optional for direct SQL)
#' @param files SQL template file scope, can be file list or regex pattern (default: "*.sql")
#' @param ... Additional arguments passed to `sqlquery` function
#'
#' @return A data frame containing query results
#'
#' @details
#' The function automatically detects if `name` is a SQL statement by checking 
#' if it starts with common SQL keywords using the pattern:
#' ^\\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|WITH|MERGE|
#' BEGIN|COMMIT|ROLLBACK|GRANT|REVOKE|EXPLAIN|DESC|SHOW)
#'
#' When using direct SQL mode (when `name` is a SQL statement), the `params` 
#' argument is optional. If no parameters are provided, the SQL is executed as-is.
#'
#' The function searches for SQL templates in the following order:
#' 1. Current working directory (files matching the pattern)
#' 2. Package-installed SQL template files
#'
#' @examples
#' \dontrun{
#' # Using SQL statement name from template files
#' result <- sqldframe("User Query", list("#status" = "active"), "*.sql")
#'
#' # Using direct SQL statement without parameters
#' result <- sqldframe("SHOW TABLES")
#'
#' # Using direct SQL statement with parameters
#' result <- sqldframe("SELECT * FROM users WHERE status = #status",
#'                    list("#status" = "active"))
#'
#' # Using INSERT statement directly
#' result <- sqldframe("INSERT INTO logs (message) VALUES (#msg)",
#'                    list("#msg" = "test message"))
#'
#' # Real-world example: query 1-minute kline data with specific time range
#' result <- sqldframe("1min.kline", 
#'                    list("##tbl" = "t_rb2601_20251117", 
#'                         "#startime" = "09:00", 
#'                         "#endtime" = "09:30"), 
#'                    dbname = "ctp")
#' }
#'
#' @importFrom purrr map flatten pluck
#' @export
sqldframe <- function(name, params = list(), files = "*.sql", ...) {
  # Enhanced SQL detection pattern covering all major SQL commands
  sql_pattern <- paste0(
    "^\\s*(", 
    "SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|",
    "WITH|MERGE|BEGIN|COMMIT|ROLLBACK|GRANT|REVOKE|",
    "EXPLAIN|DESC|SHOW|CALL|EXECUTE|DECLARE|SET",
    ")\\s"
  )
  
  is_sql <- grepl(sql_pattern, name, ignore.case = TRUE)
  
  if (is_sql) {
    # Direct SQL processing
    sql <- name
  } else {
    # Template-based SQL processing - search in multiple locations
    sql_files <- if (is.character(files) && length(files) == 1 && grepl("\\*", files)) {
      # Search in working directory first
      wd_files <- list.files(pattern = files, full.names = TRUE)
      # Then search in package SQL files
      pkg_files <- .get_package_sql_files()
      c(wd_files, pkg_files)
    } else {
      # Use provided file list
      files
    }
    
    # Remove duplicates and non-existent files
    sql_files <- unique(sql_files)
    sql_files <- sql_files[file.exists(sql_files)]
    
    if (length(sql_files) == 0) {
      stop("No matching SQL files found: ", files)
    }
    
    # Extract SQL from templates using purrr
    all_sql_templates <- sql_files |>
      purrr::map(sqlfile) |>
      purrr::flatten()
    
    sql <- purrr::pluck(all_sql_templates, name)
    
    if (is.null(sql)) {
      available_templates <- names(all_sql_templates)
      if (length(available_templates) > 0) {
        stop("SQL statement '", name, "' not found in specified files. ",
             "Available templates: ", paste(available_templates, collapse = ", "))
      } else {
        stop("SQL statement '", name, "' not found and no templates available")
      }
    }
  }
  
  # Parameter substitution and query execution
  if (length(params) > 0) {
    sql <- fill(sql, params)
  }
  
  sqlquery(sql, ...)
}


#' List Available SQL Templates
#'
#' Lists all available SQL templates in the current working directory and package.
#'
#' @param files SQL template file scope, can be file list or regex pattern (default: "*.sql")
#'
#' @return A named list of available SQL templates
#'
#' @examples
#' \dontrun{
#' # List all available SQL templates
#' templates <- list_sql_templates()
#' print(names(templates))
#' }
#'
#' @export
list_sql_templates <- function(files = "*.sql") {
  sql_files <- if (is.character(files) && length(files) == 1 && grepl("\\*", files)) {
    wd_files <- list.files(pattern = files, full.names = TRUE)
    pkg_files <- .get_package_sql_files()
    c(wd_files, pkg_files)
  } else {
    files
  }
  
  sql_files <- unique(sql_files)
  sql_files <- sql_files[file.exists(sql_files)]
  
  if (length(sql_files) == 0) {
    return(list())
  }
  
  sql_files |>
    purrr::map(sqlfile) |>
    purrr::flatten()
}
