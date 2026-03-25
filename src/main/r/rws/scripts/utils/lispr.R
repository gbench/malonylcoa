# 定义 LISP 风格的 AST 操作符
car <- function(x) if (is.call(x) || is.pairlist(x) || is.list(x)) x[[1]] else NULL
cdr <- function(x) if (length(x) > 1) x[-1] else NULL
cadr <- function(x) car(cdr(x))
caddr <- function(x) car(cdr(cdr(x)))
caar <- function(x) car(car(x))
cdar <- function(x) cdr(car(x))

# 安全版本（处理 NULL）
scar <- function(x) if (is.null(x)) NULL else car(x)
scdr <- function(x) if (is.null(x)) NULL else cdr(x)