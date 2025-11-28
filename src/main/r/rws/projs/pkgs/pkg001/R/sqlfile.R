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
#' filled_sql <- sqlfill(sql_template, params)
#' cat(filled_sql)
#' # Output: SELECT * FROM users WHERE date >= '2024-01-01' AND status = 'active'
#' }
#'
#' @export
sqlfill <- function(template, params) {
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
#' @param recursive logical. Should the listing recurse into directories?
#' @return Character vector of file paths to package SQL templates
#' @keywords internal
.get_package_sql_files <- function(recursive = FALSE) {
  package_dir <- system.file("sql", package = "malonylcoa")
  
  if (nchar(package_dir) > 0L && dir.exists(package_dir)) {
    list.files(
      package_dir,
      pattern = "\\.sql$",
      full.names = TRUE,
      recursive = recursive,
      ignore.case = TRUE
    )
  } else {
    character(0L)
  }
}

#' Find SQL Files in Working Directory
#'
#' Internal function to find SQL files in working directory with flexible input handling.
#'
#' @param files SQL template file scope
#' @param recursive logical. Should the listing recurse into directories?
#' @return Character vector of SQL file paths from working directory
#' @keywords internal
.find_local_sql_files <- function(files = "\\.sql$", recursive = FALSE) {
  # Handle different input types for local files
  local_files <- if (is.character(files) && length(files) == 1L) {
    if (file.exists(files) && !dir.exists(files)) {
      # Direct file path
      files
    } else if (dir.exists(files)) {
      # Directory - find all SQL files within
      list.files(
        path = files,
        pattern = "\\.sql$",
        full.names = TRUE,
        recursive = recursive,
        ignore.case = TRUE
      )
    } else {
      # Regex pattern - search in working directory
      list.files(
        pattern = files,
        full.names = TRUE,
        recursive = recursive,
        ignore.case = TRUE
      )
    }
  } else {
    # Multiple files provided
    files
  }
  
  # Filter valid SQL files
  local_files |>
    as.character() |>
    purrr::keep(file.exists) |>
    purrr::keep(~ !dir.exists(.x)) |>
    purrr::keep(~ grepl("\\.sql$", .x, ignore.case = TRUE))
}

#' Unified SQL Files Finder
#'
#' Internal function to find all SQL files with correct override priority.
#'
#' @param files SQL template file scope
#' @param recursive logical. Should the listing recurse into directories?
#' @return Character vector of SQL file paths with local files first
#' @keywords internal
.find_sql_files <- function(files = "\\.sql$", recursive = FALSE) {
  # Get files from both sources
  local_files <- .find_local_sql_files(files, recursive)
  pkg_files <- .get_package_sql_files(recursive)
  
  # Combine with local files first for override priority
  unique(c(local_files, pkg_files))
}

#' Load SQL Templates from Single File
#'
#' Internal function to safely load templates from a single SQL file.
#'
#' @param file_path Path to SQL file
#' @return List with templates and source info, or NULL on error
#' @keywords internal
.load_sql_file_templates <- function(file_path) {
  tryCatch({
    if (file.exists(file_path) && file.info(file_path)$size > 0L) {
      templates <- sqlfile(file_path)
      if (length(templates) > 0L) {
        list(
          templates = templates,
          source = if (grepl(system.file(package = "malonylcoa"), file_path, fixed = TRUE)) "package" else "local",
          file = file_path
        )
      } else {
        NULL
      }
    } else {
      NULL
    }
  }, error = function(e) {
    warning("Failed to load SQL file '", basename(file_path), "': ", e$message)
    NULL
  })
}

#' Merge Templates with Override Logic
#'
#' Internal function to merge templates ensuring local overrides package.
#'
#' @param acc Accumulator list with templates and sources
#' @param file_info File info list with templates and source
#' @return Updated accumulator
#' @keywords internal
.merge_templates <- function(acc, file_info) {
  file_info$templates |>
    purrr::imap(function(template_sql, template_name) {
      # Local files override package files
      # Package files only added if no local version exists
      current_source <- acc$sources[[template_name]]
      if (is.null(current_source) || file_info$source == "local") {
        acc$templates[[template_name]] <<- template_sql
        acc$sources[[template_name]] <<- file_info$source
      }
    })
  acc
}

#' Load All SQL Templates with Override
#'
#' Internal function to load all SQL templates with correct override logic.
#'
#' @param sql_files Character vector of SQL file paths
#' @return Named list of SQL templates
#' @keywords internal
.load_sql_templates <- function(sql_files) {
  # Load templates from all files
  all_file_info <- sql_files |>
    purrr::map(.load_sql_file_templates) |>
    purrr::compact()
  
  if (length(all_file_info) == 0L) {
    warning("No SQL templates loaded from provided files")
    return(list())
  }
  
  # Merge templates with override logic
  result <- all_file_info |>
    purrr::reduce(.merge_templates, .init = list(templates = list(), sources = list()))
  
  result$templates
}

#' List Available SQL Templates
#'
#' Lists all available SQL templates in the current working directory and package.
#' Local templates override package templates with the same name.
#'
#' @param files SQL template file scope, can be file list, directory, or regex pattern (default: "\\.sql$")
#' @param recursive logical. Should the listing recurse into directories? (default: FALSE)
#' @param verbose logical. Should override information be shown? (default: TRUE)
#'
#' @return A named list of available SQL templates with source information as attribute
#'
#' @examples
#' \dontrun{
#' # List all available SQL templates
#' templates <- list_sql_templates()
#' 
#' # List from specific directory
#' templates <- list_sql_templates(files = "sql", recursive = TRUE)
#' 
#' # List with detailed information
#' templates <- list_sql_templates(verbose = TRUE)
#' }
#'
#' @export
list_sql_templates <- function(files = "\\.sql$", recursive = FALSE, verbose = TRUE) {
  # Input validation
  if (!is.character(files)) {
    stop("Files must be a character vector")
  }
  
  if (!is.logical(recursive) || length(recursive) != 1L) {
    stop("Recursive must be a single logical value")
  }
  
  if (!is.logical(verbose) || length(verbose) != 1L) {
    stop("Verbose must be a single logical value")
  }
  
  # Find all SQL files
  sql_files <- .find_sql_files(files, recursive)
  
  if (length(sql_files) == 0L) {
    if (verbose) message("No SQL files found")
    return(list())
  }
  
  if (verbose) {
    message("Found ", length(sql_files), " SQL files")
  }
  
  # Load all file info for detailed reporting
  all_file_info <- sql_files |>
    purrr::map(.load_sql_file_templates) |>
    purrr::compact()
  
  if (length(all_file_info) == 0L) {
    if (verbose) message("No SQL templates loaded from files")
    return(list())
  }
  
  # Merge templates
  result <- all_file_info |>
    purrr::reduce(.merge_templates, .init = list(templates = list(), sources = list()))
  
  # Add sources as attribute
  final_templates <- result$templates
  attr(final_templates, "sources") <- result$sources
  
  # Verbose output
  if (verbose) {
    template_sources <- result$sources
    local_count <- sum(template_sources == "local")
    package_count <- sum(template_sources == "package")
    
    message("Loaded ", length(final_templates), " SQL templates:")
    message("  - Local: ", local_count, " (overrides package templates)")
    message("  - Package: ", package_count)
    
    if (length(final_templates) > 0L) {
      local_names <- names(template_sources)[template_sources == "local"]
      package_names <- names(template_sources)[template_sources == "package"]
      
      if (length(local_names) > 0L) {
        message("Local templates (", length(local_names), "): ", 
                paste(utils::head(local_names, 10), collapse = ", "),
                if (length(local_names) > 10L) paste0("... (", length(local_names), " total)") else "")
      }
      
      if (length(package_names) > 0L) {
        message("Package templates (", length(package_names), "): ", 
                paste(utils::head(package_names, 10), collapse = ", "),
                if (length(package_names) > 10L) paste0("... (", length(package_names), " total)") else "")
      }
    }
  }
  
  final_templates
}

#' Execute Parameterized SQL Query and Return Data Frame
#'
#' This function searches for SQL templates and executes queries with parameter substitution.
#' Local SQL templates override package templates with the same name.
#'
#' @param x Character string specifying either SQL template name or direct SQL statement
#' @param params Named list of parameters for template substitution
#' @param files SQL template file scope
#' @param recursive logical. Should the listing recurse into directories?
#' @param ... Additional arguments passed to `sqlquery` function
#'
#' @return A data frame containing query results
#'
#' @export
sqldframe <- function(x, params = list(), files = "\\.sql$", recursive = FALSE, ...) {
  # Capture original expression of 'x' → convert to string → remove leading/trailing double/single quotes (retain pure name only)
  name <- deparse(substitute(x)) |> gsub(pattern = "^['\"]|['\"]$", replacement = "")
  
  # Input validation
  if (!is.character(name) || length(name) != 1L || nchar(trimws(name)) == 0L) {
    stop("Parameter 'name' must be a non-empty character string")
  }
  
  if (!is.list(params)) {
    stop("Parameter 'params' must be a list")
  }
  
  name <- trimws(name)
  
  # Check if input is direct SQL statement
  if (.is_sql_statement(name)) {
    sql <- name
  } else {
    # Template-based SQL processing
    sql_files <- .find_sql_files(files, recursive)
    
    if (length(sql_files) == 0L) {
      stop("No SQL files found matching: ", files)
    }
    
    templates <- .load_sql_templates(sql_files)
    sql <- purrr::pluck(templates, name)
    
    if (is.null(sql)) {
      available <- names(templates)
      available_msg <- if (length(available) > 0L) {
        paste("Available templates: ", paste(utils::head(available, 10), collapse = ", "),
              if (length(available) > 10L) paste0("... (", length(available), " total)") else "")
      } else {
        "No templates available"
      }
      stop("SQL template '", name, "' not found. ", available_msg)
    }
  }
  
  # Parameter substitution and execution
  sql_with_params <- if (length(params) > 0L) {
    tryCatch(sqlfill(sql, params), error = function(e) stop("Parameter substitution failed: ", e$message))
  } else {
    sql
  }
  
  tryCatch(sqlquery(sql_with_params, ...), error = function(e) stop("SQL query execution failed: ", e$message))
}

#' Check if String is a SQL Statement
#'
#' Internal function to detect if a string is a direct SQL statement.
#'
#' @param text Character string to check
#' @return Logical indicating if text is a SQL statement
#' @keywords internal
.is_sql_statement <- function(text) {
  sql_keywords <- c(
    "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", 
    "TRUNCATE", "WITH", "MERGE", "BEGIN", "COMMIT", "ROLLBACK", "GRANT", 
    "REVOKE", "EXPLAIN", "DESC", "SHOW", "CALL", "EXECUTE", "DECLARE", "SET"
  )
  pattern <- paste0("^\\s*(", paste(sql_keywords, collapse = "|"), ")\\s")
  grepl(pattern, text, ignore.case = TRUE)
}
