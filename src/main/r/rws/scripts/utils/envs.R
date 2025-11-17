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
