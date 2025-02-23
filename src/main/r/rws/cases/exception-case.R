# 定义一个函数来模拟自定义运算
myop <- function(x, y, call. = F) {
  result <- x + y
  if (result == 0) {
    # 当结果为 0 时，抛出异常, 此时的 stop相当于 java 的 throw
    stop("运算结果为零: ", x, " + ", y, "  = 0", call. = call.)
  }
  return(result)
}

# 定义一个包装函数来处理异常
try_myop <- function(x, y) {
  tryCatch(
    {
      # 尝试执行 myop 函数
      res <- myop(x, y)
      return(res)
    },
    error = function(e) {
      # 捕获异常并输出错误信息
      message("捕获到异常: ", conditionMessage(e))
      return(NA)
    }
  )
}

# 测试正常情况
res1 <- try_myop(1, 2)
if (!is.na(res1)) {
  cat("运算结果: ", res1, "\n")
}

# 测试异常情况
res2 <- try_myop(-1, 1)
if (!is.na(res2)) {
  cat("运算结果: ", res2, "\n")
}

# 显示错误堆栈信息: 'myop(1, -1, T)' 当myop被一种更高级的函数比如try_myop_xxx进行分装时候，
# 显示出Error in myop(1, -1, T) 可能会让它们感觉莫名奇妙，特别是封装的时候 参数进行了变换
# 的情形, 比如: try_myop_xxx <- \(x, y) myop(2*x, y/3, T); try_myop_xxx(1, -6)
# 'Error in myop(2 * x, y/3, T) : 运算结果为零: 2 + -2  = 0' 就很人困惑.
# 因此在在发布版本的时候，call.标识是默认为F而仅当在调试版本的时候才予以设置为T
myop(- 1, 1, T)
# Error in myop(1, -1, T) : 运算结果为零: 1 + -1  = 0

# 不显示错误堆栈信息: 错误信息没有了'myop(1, -1, T)' 
myop(- 1, 1, F)
# Error: 运算结果为零: 1 + -1  = 0
