# 尝试加载 purrr 包，如果失败则安装
if (!require(purrr, quietly = TRUE)) {
  install.packages('purrr')
  library(purrr)
}

# 求取聚类中心点
km <- function(data, k, eps= 0.01) {
  # -------------------------------------------------------
  # 数据类型验证
  # -------------------------------------------------------
  if (!is.matrix(data) && !is.data.frame(data)) {
    stop("输入 data 必须是矩阵或数据框。")
  }

  if (!is.numeric(k) || k <= 0 || k != round(k)) {
    stop("输入 k 必须是正整数。")
  }

  # -------------------------------------------------------
  # 算法正文
  # -------------------------------------------------------
  n <- nrow(data)  # 数据长度
  
  # 计算 data 与 特定 点之间的距离,p:点的各个维度坐标的向量
  d <- function(p) {
    apply(data, 1, compose(sqrt, sum, \(x) (x - p)^2))
  }
  
  # 获取聚类中点id索引号，centeroids(行：点索引编号,列:各个维度坐标）
  cluster_ids <- function (centeroids) {
    apply(centeroids, 1, d) |>  # 计算各个点的距离, 结果为矩阵：(行data点编号,列centeroids点编号）
    apply(1, which.min) # 找出距离最短距离作为分组编号 
  }
  
  centeroids <- data[sample(1:n, k), ] # 随机选择k个中心点
  
  repeat { # 一直计算到中心点收敛到指定的误差范围之内eps：当前点.centeroids与先前点centeroids之间的各个维度坐标的差绝对值小于eps
    # 求出各个分类的样本的均值:作为准聚类中心点，更新聚类中心点
    .centeroids <- split(data, cluster_ids(centeroids)) |> # 计算各个数据点的聚类id
      lapply(\(x) apply(x, 2, mean)) |> # 介个每个分组的各个维度的坐标平均值 
      Reduce(x = _, f = rbind, init = data.frame()) # 结果合并为数据框,组合成当前中心点.centeroids,即准中心点

    # 当两次计算的聚簇中线点的坐标键的误差小于规定误差限度时终止算法， 
    if (all(abs(centeroids - .centeroids) < eps)) { # 误差小于 eps, 获得中心点
      break # 中心点收敛，算法结束 
    } else if (nrow(.centeroids) < k) { # 检查准聚类中心点是否结构完整
      centeroids <- data[sample(1:n, k), ] # 结构不完整, 重新开始，随机选择新的聚类中心点
    } else { # 进入下一轮循环
      centeroids <- .centeroids # 准聚类中心点正式作为聚类中心点
    }
  } # repeat

  # 返回结果， centeroids: 聚类中心点, cluster_ids: 聚类中心点ids
  list(centeroids=structure(centeroids, names=names(data)), cluster_ids=cluster_ids(centeroids))
}

# ---------------------------------------------------------------------
# 数据绘图
# ---------------------------------------------------------------------
# 调用函数进行聚类
data <- iris[,-5] # 剔除Species 
km.res<- km(data, 3) # 聚类分析
print(km.res) # 打印结果

# 将聚类结果添加到原始数据中
data$Cluster <- as.factor(km.res$cluster_ids) # 加入聚类中心点

# ---------------------------------------------------------------------
# 长度
# ---------------------------------------------------------------------
# 创建散点图展示聚类结果
ggplot(data, aes(x=Sepal.Length, y=Petal.Length, color=Cluster)) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title="K-Means 聚类结果", x="花萼长度", y="花瓣长度")

# ---------------------------------------------------------------------
# 宽度
# ---------------------------------------------------------------------
# 创建散点图展示聚类结果
ggplot(data, aes(x=Sepal.Width, y=Petal.Width, color=Cluster)) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x="花萼宽度", y="花瓣宽度")

    
# ---------------------------------------------------------------------
# 合并成一个绘图中进行显示
# ---------------------------------------------------------------------
data2 <- with(iris,rbind(# 分组组织数据
  data.frame(x=Sepal.Length, y=Petal.Length, species=as.factor(km.res$cluster_ids), name="Length"),
  data.frame(x=Sepal.Width, y=Petal.Width, species=as.factor(km.res$cluster_ids), name="Width")
)) # with

# 数据绘图
ggplot(data2, aes(x=x, y=y, color=species)) + facet_wrap(.~name) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x="花萼", y="花瓣")
