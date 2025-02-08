# 尝试加载 purrr 包，如果失败则安装
if (!require(purrr, quietly = TRUE)) {
  install.packages('purrr')
  library(purrr)
}

# 求取聚类中心点
km <- function(data, k, eps = 0.5) {
  n <- nrow(data)  # 数据长度
  
  # 计算 data 与 特定 点之间的距离 
  d <- function(p) {
    apply(data, 1, compose(sqrt, sum, \(x) (x - p)^2))
  }
  
  # 获取聚类中线点 id 索引号
  cluster_ids <- function (ps) apply(ps, 1, d) |>  # 计算各个点的距离
    as.data.frame() |> # 转换成数据框
    apply(1, which.min) # 找出距离最短距离作为分组编号 
  
  ps <- data[sample(1:n, k), ] # 随机选择 k 个中心点
  
  repeat {
    # 求出各个分类的样本的均值:作为准聚类中心点
    .ps <- split(data, cluster_ids(ps)) |> lapply(\(x) apply(x, 2, mean)) |> Reduce(x = _, f = rbind, init = data.frame())
    if (nrow(.ps) < k) { # 检查准聚类中心点是否结构完整
      ps <- data[sample(1:n, k), ] # 结构不完整, 重新开始，随机选择新的聚类中心点
    } else if (all(abs(ps - .ps) < eps)) { # 误差小于 eps, 获得中心点
      break
    } else { # 进入下一轮循环
      ps <- .ps # 准聚类中心点正式作为聚类中心点
    }
  } # repeat
  ps
}

# 调用函数进行聚类
result <- km(iris[, -5], 3)
print(result)
