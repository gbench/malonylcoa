#' MalonylCoA Database Utilities
#'
#' A collection of database utility functions for MySQL operations including
#' query execution, table creation, data insertion and updates.
#'
#' @name malonylcoa
#' @docType package
NULL

#' @import RMySQL
#' @importFrom DBI dbConnect dbDisconnect dbSendQuery dbFetch dbClearResult
#' @importFrom DBI dbGetQuery dbExecute dbBegin dbCommit dbRollback
#' @importFrom dplyr %>% mutate filter select arrange group_by ungroup
#' @importFrom tibble as_tibble tibble
#' @importFrom jsonlite toJSON
#' @importFrom purrr partial compose
#' @importFrom rlang %||%
#' @importFrom utils install.packages
NULL

# Package installation helper
.install_packages_if_missing <- function(pkgs) {
  flags <- sapply(pkgs, function(p) {
    eval(substitute(require(p, character.only = TRUE, quietly = TRUE), list(p = p)))
  })
  
  if (!all(flags)) {
    utils::install.packages(pkgs[!flags])
    sapply(pkgs[!flags], function(p) {
      eval(substitute(require(p, character.only = TRUE), list(p = p)))
    })
  }
}

# Install required packages on load
.pkgs <- c("RMySQL", "DBI", "dplyr", "tibble", "jsonlite", "purrr", "rlang")
.install_packages_if_missing(.pkgs)

#' Database Connection Function Factory
#'
#' Creates a database function with specified connection parameters.
#'
#' @param f Function to execute with database connection
#' @param ... Database connection parameters (host, user, password, port, dbname)
#' @return A function that executes SQL with the configured connection
#' @export
#' @examples
#' \dontrun{
#' query_func <- dbfun(function(con) DBI::dbGetQuery(con, "SELECT 1"))
#' query_func("SELECT 1")
#' }
dbfun <- function(f, ...) {
  dbcfg <- list(...)
  defaultcfg <- list(
    drv = RMySQL::MySQL(),
    host = "localhost",
    user = "root",
    password = "123456",
    port = 3371,
    dbname = "ctp2"
  )
  
  readcfg <- function(key) dbcfg[[key]] %||% defaultcfg[[key]]
  
  function(sql) {
    tryCatch({
      keys <- c("drv", "host", "user", "password", "port", "dbname")
      con_args <- structure(keys, names = keys) |> lapply(readcfg)
      con <- do.call(DBI::dbConnect, args = con_args)
      on.exit(DBI::dbDisconnect(con), add = TRUE)
      f(con)
    }, error = function(err) {
      print(sprintf("SQL Error - SQL: %s", sql))
      stop(err)
    })
  }
}

#' Execute SQL Query
#'
#' Executes SQL query statements and returns results as tibble(s).
#'
#' @param sql SQL statement(s) as character vector
#' @param simplify Whether to simplify single-element results (default: TRUE)
#' @param n Maximum number of records to return (default: -1 for all)
#' @param ... Additional database connection parameters
#' @return Tibble or list of tibbles containing query results
#' @export
#' @examples
#' \dontrun{
#' # Single query
#' result <- sqlquery("SELECT * FROM table LIMIT 10")
#'
#' # Multiple queries
#' results <- sqlquery(c("SELECT 1", "SELECT 2"), simplify = FALSE)
#' }
sqlquery <- function(sql, simplify = TRUE, n = -1, ...) {
  query_func <- dbfun(function(con) {
    dataset <- sql |>
      lapply(function(s) {
        result <- DBI::dbGetQuery(con, s, n = n)
        tibble::as_tibble(result)
      }) |>
      structure(names = sql)
    
    if (simplify && length(dataset) == 1) dataset[[1]] else dataset
  }, ...)
  
  query_func(sql)
}

#' Execute SQL Statements
#'
#' Executes SQL DML/DDL statements (INSERT, UPDATE, DELETE, etc.).
#'
#' @param sql SQL statement(s) as character vector
#' @param simplify Whether to simplify single-element results (default: TRUE)
#' @param ... Additional database connection parameters
#' @return Tibble with affected_rows and last_insert_id columns
#' @export
#' @examples
#' \dontrun{
#' # Insert data
#' result <- sqlexecute("INSERT INTO table (col) VALUES ('value')")
#'
#' # Multiple statements
#' results <- sqlexecute(c("DELETE FROM table", "UPDATE table SET col = 1"))
#' }
sqlexecute <- function(sql, simplify = TRUE, ...) {
  execute_func <- dbfun(function(con) {
    tryCatch({
      DBI::dbBegin(con)
      dataset <- sql |>
        lapply(function(s) {
          affected_rows <- DBI::dbExecute(con, s)
          last_insert_id <- DBI::dbGetQuery(con, "SELECT LAST_INSERT_ID()") |> unlist()
          list(affected_rows = affected_rows, last_insert_id = last_insert_id)
        }) |>
        do.call(rbind, args = _) |>
        tibble::as_tibble()
      
      DBI::dbCommit(con)
      if (simplify && length(dataset) == 1) dataset[[1]] else dataset
    }, error = function(err) {
      DBI::dbRollback(con)
      stop(err)
    })
  }, ...)
  
  execute_func(sql)
}

#' Generate CREATE TABLE SQL
#'
#' Generates CREATE TABLE SQL statement from data frame structure.
#'
#' @param dfm Data frame to use as template
#' @param tbl Table name (optional, defaults to data frame name)
#' @return CREATE TABLE SQL statement
#' @export
#' @examples
#' \dontrun{
#' df <- data.frame(id = 1:3, name = c("A", "B", "C"))
#' create_sql <- ctsql(df, "my_table")
#' print(create_sql)
#' }
ctsql <- function(dfm, tbl) {
  if (missing(tbl)) {
    tbl <- deparse(substitute(dfm))
  }
  
  col_defs <- dfm |> lapply(function(col) {
    col_type <- typeof(col)
    col_class <- class(col)
    max_width <- as.integer(max(nchar(as.character(col)), na.rm = TRUE) * 1.5)
    default_type <- sprintf("varchar(%d)", max_width)
    
    switch(col_type,
      logical = "bool",
      integer = if ("factor" %in% col_class) default_type else "integer",
      double = if (any(grepl("Date|POSIXct|POSIXt", col_class))) "datetime" else "double",
      list = "json",
      default_type
    )
  })
  
  col_specs <- paste(names(col_defs), col_defs, collapse = ",\n  ")
  sprintf("CREATE TABLE %s (\n  %s\n)", tbl, col_specs)
}

#' Generate INSERT SQL
#'
#' Generates INSERT SQL statement from data frame.
#'
#' @param dfm Data frame containing data to insert
#' @param tbl Table name (optional, defaults to data frame name)
#' @return INSERT SQL statement
#' @export
#' @examples
#' \dontrun{
#' df <- data.frame(id = 1:3, name = c("A", "B", "C"))
#' insert_sql <- insql(df, "my_table")
#' print(insert_sql)
#' }
insql <- function(dfm, tbl) {
  if (missing(tbl)) {
    tbl <- deparse(substitute(dfm))
  }
  
  keys <- names(dfm) |> paste(collapse = ", ")
  
  values <- dfm |> lapply(function(col) {
    col_type <- typeof(col)
    col_class <- class(col)
    
    switch(col_type,
      logical = as.character(col),
      integer = if ("factor" %in% col_class) sprintf("'%s'", col) else as.character(col),
      double = if (any(grepl("Date|POSIXct|POSIXt", col_class))) {
        sprintf("'%s'", col)
      } else {
        as.character(col)
      },
      list = sapply(col, function(x) {
        sprintf("'%s'", gsub("'", "''", jsonlite::toJSON(x)))
      }),
      sprintf("'%s'", gsub("'", "''", as.character(col)))
    )
  }) |>
    do.call(function(...) mapply(paste, ..., sep = ", "), args = _) |>
    sapply(function(x) sprintf("(%s)", x)) |>
    paste(collapse = ",\n  ")
  
  sprintf("INSERT INTO %s (%s) VALUES\n  %s", tbl, keys, values)
}

#' Generate UPDATE SQL
#'
#' Generates UPDATE SQL statement from data frame.
#'
#' @param dfm Data frame containing data to update
#' @param tbl Table name
#' @param pk Primary key column name (default: "id")
#' @return UPDATE SQL statement(s)
#' @export
#' @examples
#' \dontrun{
#' df <- data.frame(id = 1:3, name = c("A", "B", "C"))
#' update_sql <- upsql(df, "my_table", "id")
#' print(update_sql)
#' }
upsql <- function(dfm, tbl, pk = "id") {
  nms <- names(dfm)
  idx <- match(pk, nms)
  stopifnot("Data frame must contain primary key column" = !is.na(idx))
  
  flds <- sapply(nms, function(col) {
    sprintf("%s = '%s'", col, gsub("'", "''", as.character(dfm[[col]])))
  }) |>
    apply(1, function(row) paste(row[-idx], collapse = ",\n  "))
  
  sprintf("UPDATE %s SET\n  %s\nWHERE %s = '%s'", tbl, flds, pk, dfm[[pk]])
}

#' Pre-configured SQL Query Function for CTP2 Database
#'
#' A pre-configured version of sqlquery for connecting to a specific
#' CTP2 database instance.
#'
#' @param sql SQL statement(s) as character vector
#' @param simplify Whether to simplify single-element results (default: TRUE)
#' @param n Maximum number of records to return (default: -1 for all)
#' @return Query results as tibble or list of tibbles
#' @export
#' @examples
#' \dontrun{
#' # Query CTP2 database
#' result <- sqlquery_h10ctp2("SELECT * FROM some_table LIMIT 10")
#' }
sqlquery_h10ctp2 <- function(sql, simplify = TRUE, n = -1) {
  sqlquery(sql = sql, simplify = simplify, n = n, 
           host = "192.168.1.10", dbname = "ctp2")
}

#' Pre-configured SQL Query Function for CTP Database
#'
#' A pre-configured version of sqlquery for connecting to a specific
#' CTP database instance.
#'
#' @param sql SQL statement(s) as character vector
#' @param simplify Whether to simplify single-element results (default: TRUE)
#' @param n Maximum number of records to return (default: -1 for all)
#' @return Query results as tibble or list of tibbles
#' @export
#' @examples
#' \dontrun{
#' # Query CTP database
#' result <- sqlquery_ctp("SELECT * FROM some_table LIMIT 10")
#' }
sqlquery_ctp <- function(sql, simplify = TRUE, n = -1) {
  sqlquery(sql = sql, simplify = simplify, n = n,
           host = "192.168.1.10", dbname = "ctp")
}
