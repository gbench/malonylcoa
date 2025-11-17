#' 计算起始环境对象x的各个环境闭包
#' @param x 起始的环境对象
#' @param ret 返回值，上一阶调用的返回值
#' @return 起始环境对象x的各个环境闭包
envclos <- \(x=sys.frame(sys.nframe()), ret=list()) if(identical(emptyenv(), x)) ret else parent.env(x) |> envclos(append(x, ret));

#' 查看所有变量符号所在的环境名称，当which='path'的时候就是查看符号所在的库文件路径，可以通过R.home() 查看R_HOME即R的的根安装位置目录
# envclos() |> (\(xs, nms=lapply(xs, partial(attr, which='name'))) structure(lapply(xs, names), names=nms)) ()

#' 数据descartes product
#' @param ps: points点集合 
#' @param gn: group numer 分组阶数 
pgn <- function(ps=1:3, gn=length(ps)) rep(ps, gn) |> split( rep(paste0("x", seq(gn)), rep(length(ps), gn)) ) |> expand.grid()

#' 创建键值对列表
#'
#' 该函数接受不定数量的参数，以键值对的形式创建命名列表。
#' 这是一个简单可靠的实现，要求所有键名都必须带引号。
#'
#' @param ... 键值对参数，格式为"key1", value1, "key2", value2, ...
#'            键名必须是带引号的字符串，值可以是任意R对象。
#'
#' @return 一个命名列表，其中键作为列表元素的名称，对应的值作为元素内容。
#'         返回的列表结构与`list(key1 = value1, key2 = value2)`相同。
#'
#' @examples
#' # 基本用法
#' REC("name", "zhangsan", "age", 13, "address", "shanghai changning")
#'
#' # 包含各种数据类型的值
#' REC(
#'   "name", "lisi", 
#'   "score", 95.5, 
#'   "passed", TRUE, 
#'   "courses", c("math", "english")
#' )
#'
#' # 访问生成的列表
#' my_list <- REC("x", 1:5, "y", letters[1:3])
#' my_list$x
#' my_list$y
#'
#' @seealso 
#' \code{\link{list}} 基础列表构造函数
#' \code{\link{setNames}} 设置对象名称的函数
#' 
#' @export
REC <- function(...) {
  # 捕获表达式而不是值
  dots <- match.call(expand.dots = FALSE)$...
  n <- length(dots)
  
  if (n %% 2 != 0) stop("参数个数必须为偶数")
  
  keys <- character(n/2)
  values <- vector("list", n/2)
  
  for (i in seq_len(n/2)) {
    # 键：直接转换为字符
    key_expr <- dots[[2*i - 1]]
    keys[i] <- as.character(key_expr)
    
    # 值：尝试评估，如果失败则使用原表达式
    value_expr <- dots[[2*i]]
    values[[i]] <- tryCatch(
      eval(value_expr, parent.frame()),
      error = function(e) as.character(value_expr)
    )
  }
  
  setNames(values, keys)
}

#' 创建REC函数构建器
#'
#' 该函数返回一个专门用于生成特定键名REC调用的函数。
#' 主要用于创建具有固定键名的数据记录构造函数。
#'
#' @param keys 字符向量或逗号分隔的字符串，指定REC函数的键名
#' @param .func_name 可选参数，指定生成的函数名称（用于错误消息）
#'
#' @return 返回一个函数，该函数接受与键名数量相等的值参数，
#'         并返回对应的REC函数调用
#'
#' @examples
#' \dontrun{
#' # 创建具有固定键名的REC构建器
#' rb <- rec.rb("a,b,c")
#' rb(1, 2, 3)  # 返回 REC(a, 1, b, 2, c, 3)
#'
#' # 使用字符向量
#' rb2 <- rec.rb(c("name", "age", "city"))
#' rb2("John", 25, "New York")
#'
#' # 生成的函数可以重复使用
#' person_rb <- rec.rb("name,age,gender")
#' person1 <- person_rb("Alice", 30, "F")
#' person2 <- person_rb("Bob", 25, "M")
#' }
#'
#' @seealso \code{\link{REC}} 基础REC函数
#' @export
rec.rb <- function(keys, .func_name = NULL) {
  # 参数验证和处理
  if (is.character(keys) && length(keys) == 1) {
    # 如果是逗号分隔的字符串，分割为字符向量
    keys <- strsplit(keys, ",")[[1]]
    keys <- trimws(keys)  # 去除前后空格
  }
  
  if (!is.character(keys) || length(keys) == 0) {
    stop("keys参数必须是字符向量或逗号分隔的字符串")
  }
  
  # 移除空字符串
  keys <- keys[keys != ""]
  
  if (length(keys) == 0) {
    stop("keys参数不能为空")
  }
  
  # 检查键名有效性
  invalid_keys <- keys[!grepl("^[#a-zA-Z_][#a-zA-Z0-9_]*$", keys)]
  if (length(invalid_keys) > 0) {
    stop("无效的键名: ", paste(invalid_keys, collapse = ", "), 
         "。键名必须以#、字母或下划线开头，只能包含#、字母、数字和下划线。")
  }
  
  func_name <- .func_name %||% paste("REC", paste(keys, collapse = "_"), sep = "_")
  
  # 创建并返回构建器函数
  function(...) {
    values <- list(...)
    
    # 验证参数数量
    if (length(values) != length(keys)) {
      stop("函数 ", func_name, " 需要 ", length(keys), " 个参数，但提供了 ", length(values), " 个")
    }
    
    # 构建REC调用表达式
    args <- vector("list", length(keys) * 2)
    
    # 交替放置键和值
    for (i in seq_along(keys)) {
      args[[2 * i - 1]] <- as.symbol(keys[i])
      args[[2 * i]] <- values[[i]]
    }
    
    # 创建REC调用
    rec_call <- as.call(c(list(as.symbol("REC")), args))
    
    # 评估并返回结果
    eval(rec_call, envir = parent.frame())
  }
}

#' 便捷的REC构建器创建函数
#'
#' rec.rb的别名，提供更简洁的命名
#'
#' @inheritParams rec.rb
#' @return 返回一个REC构建器函数
#'
#' @examples
#' \dontrun{
#' # 使用record_builder创建构建器
#' rb <- record_builder("x,y,z")
#' rb(10, 20, 30)
#' }
#'
#' @export
record_builder <- rec.rb

#' 创建具有类型检查的REC构建器
#'
#' 增强版本的REC构建器，支持参数类型验证
#'
#' @param keys 字符向量或逗号分隔的字符串，指定键名
#' @param types 可选的类型规范，用于参数类型检查
#' @param .func_name 可选参数，指定生成的函数名称
#'
#' @return 返回一个具有类型检查的REC构建器函数
#'
#' @examples
#' \dontrun{
#' # 创建具有类型检查的构建器
#' typed_rb <- rec.rb.typed("name,age,active", c("character", "numeric", "logical"))
#' typed_rb("John", 25, TRUE)  # 正确
#' typed_rb("John", "25", TRUE)  # 错误：age应该是numeric
#' }
#'
#' @export
rec.rb.typed <- function(keys, types = NULL, .func_name = NULL) {
  # 处理键名（与rec.rb相同）
  if (is.character(keys) && length(keys) == 1) {
    keys <- strsplit(keys, ",")[[1]]
    keys <- trimws(keys)
  }
  
  if (!is.character(keys) || length(keys) == 0) {
    stop("keys参数必须是字符向量或逗号分隔的字符串")
  }
  
  keys <- keys[keys != ""]
  
  # 类型检查逻辑
  if (!is.null(types)) {
    if (length(types) != length(keys)) {
      stop("types参数的长度必须与keys参数相同")
    }
    
    # 类型验证函数
    validate_type <- function(value, type, key) {
      type_ok <- switch(
        type,
        "character" = is.character(value),
        "numeric" = is.numeric(value),
        "integer" = is.integer(value),
        "logical" = is.logical(value),
        "factor" = is.factor(value),
        "list" = is.list(value),
        "any" = TRUE,
        TRUE  # 默认情况下不进行严格检查
      )
      
      if (!type_ok) {
        stop("参数 '", key, "' 应该是 ", type, " 类型，但得到的是 ", class(value)[1])
      }
    }
  }
  
  # 创建基础构建器
  base_builder <- rec.rb(keys, .func_name)
  
  # 返回增强的构建器函数
  function(...) {
    values <- list(...)
    
    # 类型检查
    if (!is.null(types)) {
      for (i in seq_along(values)) {
        validate_type(values[[i]], types[i], keys[i])
      }
    }
    
    # 调用基础构建器
    base_builder(...)
  }
}
