# 尝试加载 purrr 包，如果失败则安装
if (!require(purrr, quietly = TRUE)) {
  install.packages('purrr')
  library(purrr)
}

# 求取聚类中心点，即对data数据进行聚类分类
# 聚类分析的关键思想就是用聚类中心作为区分各个点属于何种的类别的分类依据
# @param data 数据点集合:数据框或是矩阵,行为点所列，列为该点在各个维度轴向上的索引坐标
# @param k 指定的分类即聚类中心的数量。注意,这个需要根据观测数据分布特点而后给出，也就是KMean
#           不能自行发现出分类数，需要人为的给出相应提示，有KMean自行确定聚类中心的细节（具体坐标）
# @return [centeroids:聚类中心点集合(一行代表一个点集合, 列为数据点在各个维度轴向的索引坐标), 
#          cluster_ids: 聚类中心id, 即数据点的分类编码]
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
 
  # cluster_ids的本质是一个分类器(classifier), 即将拥有着共同的聚类中心的点视为同一个类别 
  # @param centeroids k个聚类中心的索引坐标 (行:点索引编号从1到k, 列:各个维度坐标)
  # @return 获取聚类中点id索引号：分类标记
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

  classifier = cluster_ids # 将cluster_ids 作为数据点的分类器
  # 返回结果， centeroids: 聚类中心点, cluster_ids: 聚类中心点ids,  聚类中心id, 即数据点的分类编码
  list(centeroids=structure(centeroids, names=names(data)), cluster_ids=classifier(centeroids))
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
data2 <- with(iris, rbind(# 分组 组织数据
  # -- 估计值
  data.frame(x=Sepal.Length, y=Petal.Length, species=as.factor(km.res$cluster_ids), label="Length.Estimate"),
  data.frame(x=Sepal.Width, y=Petal.Width, species=as.factor(km.res$cluster_ids), label="Width.Estimate"),
  # -- 实际值
  data.frame(x=Sepal.Length, y=Petal.Length, species=iris$Species, label="Length.Actual"),
  data.frame(x=Sepal.Width, y=Petal.Width, species=iris$Species, label="Width.Actual")
)) # with

# 数据绘图
ggplot(data2, aes(x=x, y=y, color=species)) + facet_wrap(.~label) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x="花萼", y="花瓣")
