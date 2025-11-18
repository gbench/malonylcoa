#' Recursively Collect Environment Closures
#'
#' Traverses up the environment hierarchy starting from a given environment
#' and collects all environments in the chain until the empty environment
#' is reached. This function is useful for inspecting the environment
#' structure of R objects and closures.
#'
#' @param x Starting environment object. Default is the calling environment
#'   (`sys.frame(sys.nframe())`).
#' @param ret Accumulator for the result. Used internally for recursion.
#'   Users should not modify this parameter.
#'
#' @return A list of environments in order from the starting environment
#'   up to the empty environment. The list represents the environment chain.
#'
#' @export
#'
#' @examples
#' # Get the environment chain from current environment
#' env_chain <- envclos()
#' str(env_chain)
#'
#' # Get environment chain from a specific environment
#' my_env <- new.env()
#' parent.env(my_env) <- globalenv()
#' env_chain <- envclos(my_env)
#' str(env_chain)
#'
#' # Inspect function closure environments
#' f <- function(x) {
#'   y <- 10
#'   function(z) x + y + z
#' }
#' closure_fn <- f(5)
#' env_chain <- envclos(environment(closure_fn))
#' str(env_chain)
#'
#' @seealso \code{\link{environment}} for getting function environments,
#'   \code{\link{parent.env}} for accessing parent environments,
#'   \code{\link{sys.frame}} for call frame environments,
#'   \code{\link{emptyenv}} for the empty environment
#'
#' @keywords utilities environment
envclos <- function(x = sys.frame(sys.nframe()), ret = list()) {
  if (identical(emptyenv(), x)) {
    ret
  } else {
    parent.env(x) |> envclos(append(list(x), ret))
  }
}

#' 查看所有变量符号所在的环境名称，当which='path'的时候就是查看符号所在的库文件路径，可以通过R.home() 查看R_HOME即R的的根安装位置目录
# envclos() |> (\(xs, nms=lapply(xs, partial(attr, which='name'))) structure(lapply(xs, names), names=nms)) ()

#' Cartesian Product for Data Points
#'
#' Generates the Cartesian product (all possible combinations) of a set of points
#' grouped by a specified dimension. This is useful for creating grid-like
#' structures from a set of values.
#'
#' @param ps A vector of points (values) to combine. Default is 1:3.
#' @param gn Group number specifying the dimension of the Cartesian product.
#'   Default is `length(ps)`.
#'
#' @return A data frame where each row represents one combination from the
#'   Cartesian product. The columns are named "x1", "x2", ..., "xgn" indicating
#'   the dimension of each coordinate.
#'
#' @export
#'
#' @examples
#' # Default usage: 3 points in 3 dimensions
#' pgn()
#'
#' # 2 points in 2 dimensions
#' pgn(ps = 1:2, gn = 2)
#'
#' # Character points in 2 dimensions
#' pgn(ps = c("A", "B"), gn = 2)
#'
#' # 4 points in 2 dimensions
#' pgn(ps = 1:4, gn = 2)
#'
#' # Create a 3D grid with custom points
#' pgn(ps = c(0, 0.5, 1), gn = 3)
#'
#' @seealso \code{\link{expand.grid}} for the base R function that creates
#'   Cartesian products, \code{\link{rep}} for repeating vectors,
#'   \code{\link{split}} for splitting vectors into groups
#'
#' @keywords utilities combinatorics
pgn <- function(ps = 1:3, gn = length(ps)) {
  rep(ps, gn) |>
    split(rep(paste0("x", seq(gn)), rep(length(ps), gn))) |>
    expand.grid()
}

#' Create a Named List from Key-Value Pairs
#'
#' Builds a named list from alternating key-value arguments. This function
#' evaluates the value expressions in the parent frame and creates a list
#' where keys are taken from the odd-position arguments and values from
#' the even-position arguments.
#'
#' @param ... Alternating key-value pairs. Keys should be unquoted names,
#'   values can be any R expressions. The function expects an even number
#'   of arguments in the pattern: key1, value1, key2, value2, ...
#'
#' @return A named list where names are the key arguments and values are
#'   the evaluated value arguments. If evaluation of a value fails,
#'   the function returns the unevaluated expression as a character string.
#'
#' @export
#'
#' @examples
#' # Basic usage with simple values
#' REC(a = 1, b = 2, c = 3)
#'
#' # With expressions that need evaluation
#' REC(x = 1 + 1, y = mean(1:10), z = paste("hello", "world"))
#'
#' # With undefined variables (falls back to character)
#' REC(name = undefined_var, value = "safe_string")
#'
#' # Mixed types
#' REC(number = 42, text = "hello", flag = TRUE, vector = 1:5)
#'
#' @seealso \code{\link{list}} for creating simple lists,
#'   \code{\link{setNames}} for naming list elements,
#'   \code{\link{eval}} for expression evaluation
#'
#' @keywords utilities list
REC <- function(...) {
  dots <- match.call(expand.dots = FALSE)$...
  n <- length(dots)
  
  if (n %% 2 != 0) stop("Number of arguments must be even")
  
  Reduce(\(acc, i) {
    key <- as.character(dots[[2*i-1]])
    value <- tryCatch(
      eval(dots[[2*i]], parent.frame()),
      error = function(e) as.character(dots[[2*i]])
    )
    acc[[key]] <- value
    acc
  }, x = seq_len(n/2), init = list())
}



#' Create a Record Builder Function
#'
#' Generates a specialized constructor function that creates named lists
#' with predefined keys and optional type checking. Supports both traditional
#' and NSE (Non-Standard Evaluation) syntax.
#'
#' @param ... Key-type pairs using NSE syntax: key = type (without quotes),
#'   or traditional character keys
#' @param types Optional character vector specifying types for each key
#'   (for traditional syntax)
#'
#' @return A function that takes values as arguments and returns a named list
#'
#' @export
#'
#' @examples
#' # Traditional style - string keys
#' person_builder <- record.builder("name,age,score")
#' person <- person_builder("Alice", 25, 95.5)
#' print(person)
#'
#' # NSE style - no quotes needed for types
#' person_builder2 <- record.builder(name = character, age = integer, score = numeric)
#' person2 <- person_builder2("Alice", 25L, 95.5)
#' print(person2)
#'
#' # Mixed usage
#' builder <- record.builder(name = character, sex = logical)("zhangsan", TRUE)
#' print(builder)
record.builder <- function(..., types = NULL) {
  dots <- match.call(expand.dots = FALSE)$...
  
  # 检测输入模式：如果所有参数都是命名参数且值是类型符号，就是NSE模式
  if (is_nse_mode(dots)) {
    config <- process_nse_input(dots)
  } else {
    config <- process_traditional_input(dots, types)
  }
  
  # Create the builder function
  create_builder_function(config$keys, config$types)
}

#' Detect if input is in NSE mode
#' @keywords internal
is_nse_mode <- function(dots) {
  if (length(dots) == 0) return(FALSE)
  
  # 检查是否有命名参数
  if (is.null(names(dots)) || any(names(dots) == "")) {
    return(FALSE)
  }
  
  # 检查值是否是类型符号（character, integer, logical等）
  is_type_symbol <- function(expr) {
    if (!is.symbol(expr)) return(FALSE)
    type_name <- as.character(expr)
    type_name %in% get_valid_types()
  }
  
  all(sapply(dots, is_type_symbol))
}

#' Process NSE input
#' @keywords internal
process_nse_input <- function(dots) {
  keys <- names(dots)
  types <- extract_types_from_dots(dots)
  
  list(keys = keys, types = types)
}

#' Process traditional input
#' @keywords internal
process_traditional_input <- function(dots, types) {
  # 传统模式下，dots 包含的是键名字符串
  if (length(dots) == 1 && is.character(dots[[1]])) {
    # 单个字符串："a,b,c"
    keys <- parse_keys_string(dots[[1]])
  } else {
    # 多个参数：c("a", "b", "c") 或 "a", "b", "c"
    keys <- sapply(dots, as.character)
  }
  
  # Validate types if provided
  if (!is.null(types)) {
    if (length(types) != length(keys)) {
      stop("Length of types (", length(types), ") must match length of keys (", 
           length(keys), ")")
    }
    validate_types(types)
  }
  
  list(keys = keys, types = types)
}

#' Parse keys from string input
#' @keywords internal
parse_keys_string <- function(keys_str) {
  strsplit(keys_str, "[,[:blank:]]+")[[1]] |> 
    Filter(\(x) x != "", x = _)
}

#' Extract types from NSE dots
#' @keywords internal
extract_types_from_dots <- function(dots) {
  sapply(dots, as.character)
}

#' Validate types against allowed types
#' @keywords internal
validate_types <- function(types) {
  valid_types <- get_valid_types()
  invalid_types <- setdiff(types, valid_types)
  
  if (length(invalid_types) > 0) {
    stop("Invalid types: ", paste(invalid_types, collapse = ", "), 
         ". Valid types are: ", paste(valid_types, collapse = ", "))
  }
}

#' Get list of valid type names
#' @keywords internal
get_valid_types <- function() {
  c(
    "logical", "integer", "numeric", "double", "complex", "character", "raw",
    "list", "vector", "matrix", "array", "data.frame", "factor",
    "function", "closure", "builtin", "special",
    "environment", "expression", "call", "name", "symbol", "language",
    "null", "na", "any"
  )
}

#' Create the final builder function
#' @keywords internal
create_builder_function <- function(keys, types) {
  function(...) {
    values <- list(...)
    validate_argument_count(values, keys)
    
    result <- setNames(values, keys)
    
    if (!is.null(types)) {
      result <- apply_type_validation(result, types, keys)
    }
    
    result
  }
}

#' Validate argument count matches key count
#' @keywords internal
validate_argument_count <- function(values, keys) {
  if (length(values) != length(keys)) {
    stop("Number of arguments (", length(values), ") doesn't match number of keys (", 
         length(keys), ")")
  }
}

#' Apply type validation to all values
#' @keywords internal
apply_type_validation <- function(values, types, keys) {
  Map(validate_and_convert_type, values, types, keys) |> 
    setNames(keys)
}

#' Core type validation and conversion function
#' @keywords internal
validate_and_convert_type <- function(value, type, key_name) {
  # Handle special types
  if (type %in% c("any", "na", "null")) {
    return(handle_special_types(value, type, key_name))
  }
  
  # Dispatch to appropriate type handler
  type_handlers <- list(
    logical = \(v) convert_atomic(v, is.logical, as.logical, "logical"),
    integer = \(v) convert_atomic(v, is.integer, as.integer, "integer"),
    numeric = \(v) convert_atomic(v, is.numeric, as.numeric, "numeric"),
    double = \(v) convert_atomic(v, is.double, as.double, "double"),
    complex = \(v) convert_atomic(v, is.complex, as.complex, "complex"),
    character = \(v) convert_atomic(v, is.character, as.character, "character"),
    raw = \(v) convert_atomic(v, is.raw, as.raw, "raw"),
    list = \(v) if (!is.list(v)) as.list(v) else v,
    vector = \(v) if (!is.vector(v)) as.vector(v) else v,
    matrix = \(v) convert_structure(v, is.matrix, as.matrix, "matrix"),
    array = \(v) convert_structure(v, is.array, as.array, "array"),
    data.frame = \(v) convert_structure(v, is.data.frame, as.data.frame, "data.frame"),
    factor = \(v) convert_structure(v, is.factor, as.factor, "factor"),
    "function" = validate_function,
    closure = validate_function,
    builtin = validate_function,
    special = validate_function,
    environment = validate_environment,
    expression = \(v) convert_structure(v, is.expression, as.expression, "expression"),
    call = validate_language,
    language = validate_language,
    name = validate_symbol,
    symbol = validate_symbol
  )
  
  handler <- type_handlers[[type]]
  if (is.null(handler)) return(value)
  
  tryCatch(
    handler(value),
    error = function(e) stop("Value for '", key_name, "' - ", e$message)
  )
}

#' Handle special types (any, na, null)
#' @keywords internal
handle_special_types <- function(value, type, key_name) {
  switch(type,
    any = value,
    na = if (any(is.na(value))) value else stop("must be NA"),
    null = if (is.null(value)) value else stop("must be NULL")
  )
}

#' Helper for atomic type conversion
#' @keywords internal
convert_atomic <- function(value, check_fun, convert_fun, type_name) {
  if (check_fun(value)) value else convert_fun(value)
}

#' Helper for structure type conversion
#' @keywords internal
convert_structure <- function(value, check_fun, convert_fun, type_name) {
  if (check_fun(value)) value else convert_fun(value)
}

#' Validate function type
#' @keywords internal
validate_function <- function(value) {
  if (!is.function(value)) stop("must be a function")
  value
}

#' Validate environment type
#' @keywords internal
validate_environment <- function(value) {
  if (!is.environment(value)) stop("must be an environment")
  value
}

#' Validate language type
#' @keywords internal
validate_language <- function(value) {
  if (!is.language(value)) stop("must be a language object")
  value
}

#' Validate symbol type
#' @keywords internal
validate_symbol <- function(value) {
  if (!is.name(value) && !is.symbol(value)) stop("must be a name/symbol")
  value
}

# # 传统风格 - 单个字符串 {"name":["zhangsan"],"sex":[true]} 
#  record.builder("name,sex")("zhangsan", TRUE) |> jsonlite::toJSON()
# # 传统风格 - 多个参数  {"name":["lisi"],"sex":[false]}
#  record.builder("name", "sex")("lisi", FALSE) |> jsonlite::toJSON()
# # 传统风格 - 带类型检查 {"name":["lisi"],"sex":[false]} 
# record.builder("name,sex", types = c("character", "logical"))("wangwu", TRUE) |> jsonlite::toJSON()
# # NSE风格 {"character":["zhaoliu"],"logical":[false]} 
# record.builder(name = character, sex = logical)("zhaoliu", FALSE) |> jsonlite::toJSON()
# # NSE风格 - 使用别名  {"name":["lisi"],"sex":[false]}
# record.builder(name = char, sex = bool)("lisi", FALSE) |> jsonlite::toJSON()
# # 链式调用 {"character":["zhangsan"],"logical":[true]}
# record.builder(name = character, sex = logical)("zhangsan", TRUE) |> jsonlite::toJSON()
# # 键名定义采用符号名 {"b":[1],"d":[2]} 
# record.builder(a = b, c = d)(1, 2) |> jsonlite::toJSON()
# # character键名 {"xxx":[1]} 
# record.builder(name = xxx)(1) |> jsonlite::toJSON()
# # character键名 {"character":[1]} 
# record.builder(name = character)(1) |> jsonlite::toJSON()
# # char 为 类型约束 {"name":["1"]} 
# record.builder(name = char)(1) |> jsonlite::toJSON()
