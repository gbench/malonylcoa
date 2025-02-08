# 尝试加载 purrr 包，如果失败则安装
if (!require(purrr, quietly = TRUE)) {
  install.packages("purrr")
  library(purrr)
}

# 数据分析的本质就是将一组数据给予相应的分类id,也就是层级化出一系列的结构。每个分类就是一种结构
# 分类的结果就是把data结构化成{class1:[d1,d2],class2:[d3,d4],...}，即分析就是Tree式的结构化数据
# 使之成为有条理：序列化罗列，有层级，拥有父子关系的概念&对象集合。注意，若分类算法是可以递归的
# 进行的算法，于是任何的只有一层的结构分类算法，一旦开始进行在分类的分类上再次运行就可以构造出任何
# 复杂的层级结构了，这也就是为何说分类就是数据分析&构造结构化概念的本质与核心了。
#
# 求取聚类中心点，即对data数据进行聚类分类：核心原理。
# 聚类分析的关键思想就是用聚类中心作为区分各个点属于何种的类别的分类依据
# @param data 数据点集合:数据框或是矩阵,行为点所列，列为该点在各个维度轴向上的索引坐标
# @param k 指定的分类即聚类中心的数量。注意,这个需要根据观测数据分布特点而后给出，也就是KMean
#          不能自行发现出分类数，需要人为的给出相应提示，有KMean自行确定聚类中心的细节（具体坐标）
# @param eps 收敛验证的误差限度，最大容许偏差，大于0的实数
# @return list列表{centeroids:聚类中心点集合(一行代表一个点集合, 列为数据点在各个维度轴向的索引坐标),
#         cluster_ids: 聚类中心id, 即数据点的分类编码, cluster_id就是未给予正式的命名的概念或者说
#         是机器分析出的概念，一旦为cluster_id即分类id进行了命名,给出了带有人类文化&经验色彩的名称,
#         cluster_id或者说分类id就就变成了我们通常使用的概念了，它的意义将由该分类的下的数据元素的
#         共性所承载}
km <- function(data, k, eps = 0.01) {
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
  n <- nrow(data) # 数据长度

  # 计算 data 与 特定 点之间的距离,p:点的各个维度坐标的向量
  d <- function(p) {
    apply(data, 1, compose(sqrt, sum, \(x) (x - p)^2))
  }

  # cluster_ids的本质是一个分类器(classifier), 即将拥有着共同的聚类中心的点视为同一个类别
  # @param centeroids k个聚类中心的索引坐标 (行:点索引编号从1到k, 列:各个维度坐标)
  # @return 获取聚类中点id索引号：分类标记
  cluster_ids <- function(centeroids) {
    apply(centeroids, 1, d) |> # 按行统计 ,计算各个点的距离, 结果为矩阵：(行data点编号,列centeroids点编号）
      apply(1, which.min) # 按照行统计， 找出各数据data点与centeroids各点距离中最小的centeriods索引点编号作为cluster_id
  }

  # K-Means++ 选择初始中心点
  # @param data 数据点集合
  # @param k 聚类中心数量
  kmeans_plus_plus <- function(data, k) {
    n <- nrow(data)
    centers <- matrix(0, nrow = k, ncol = ncol(data))
    # 随机选择第一个中心点
    centers[1, ] <- as.numeric(data[sample(1:n, 1), ])
    # 记录已经被选为中心点的数据点的索引
    selected_indices <- integer(k)
    selected_indices[1] <- which(apply(data, 1, function(x) all(x == centers[1, ])))

    for (i in 2:k) {
      # 计算每个数据点到已选中心点的距离
      distances <- apply(centers[1:(i - 1), , drop = FALSE], 1, function(p) {
        dist_matrix <- sweep(data, 2, p, "-")
        sqrt(rowSums(dist_matrix^2))
      })
      # 排除已经被选为中心点的数据点
      non_selected_indices <- setdiff(1:n, selected_indices[1:(i - 1)])
      non_selected_distances <- distances[non_selected_indices, , drop = FALSE]
      # 找到每个未被选数据点到已选中心点的最小距离
      min_distances <- apply(non_selected_distances, 1, min)
      # 计算每个未被选数据点被选为下一个中心点的概率：距离越近概率越小
      probabilities <- min_distances / sum(min_distances)
      # 根据概率从未被选的数据点中选择下一个中心点的索引
      next_center_index_in_non_selected <- sample(seq_along(non_selected_indices), 1, prob = probabilities)
      next_center_index <- non_selected_indices[next_center_index_in_non_selected]
      # 更新中心点和已选索引
      centers[i, ] <- as.numeric(data[next_center_index, ])
      selected_indices[i] <- next_center_index
    }

    return(centers)
  }

  # 随机生成中心点
  # @param data 数据点集合
  # @param k 聚类中心数量
  rand_centeroids <- function(data, k) {
    data[sample(1:n, k), ] # 随机选择k个中心点
  }

  # 聚类中心点生成函数
  centeroids_gen <- function(b = F) {
    if (b) rand_centeroids(data, k) else kmeans_plus_plus(data, k)
  }

  flag <- F # 聚类中心点的生成方式，是否使用随机生成的中心点， True 随机方式, False非随机方式
  centeroids <- centeroids_gen(flag)

  repeat { # 一直计算到中心点收敛到指定的误差范围之内eps：当前点.centeroids与先前点centeroids之间的各个维度坐标的差绝对值小于eps
    # 求出各个分类的样本的均值:作为准聚类中心点，更新聚类中心点
    .centeroids <- split(data, cluster_ids(centeroids)) |> # 计算各个数据点的聚类id
      lapply(\(x) apply(x, 2, mean)) |> # 介个每个分组的各个维度的坐标平均值
      Reduce(x = _, f = rbind, init = data.frame()) # 结果合并为数据框,组合成当前中心点.centeroids,即准中心点

    # 当两次计算的聚簇中线点的坐标键的误差小于规定误差限度时终止算法，
    if (all(abs(centeroids - .centeroids) < eps)) { # 误差小于 eps, 获得中心点
      break # 中心点收敛，算法结束
    } else if (nrow(.centeroids) < k) { # 检查准聚类中心点是否结构完整
      centeroids <- centeroids_gen(flag) # 结构不完整, 重新选择新的聚类中心点
    } else { # 进入下一轮循环
      centeroids <- .centeroids # 准聚类中心点正式作为聚类中心点
    }
  } # repeat

  classifier <- cluster_ids # 将cluster_ids 作为数据点的分类器
  # 返回结果，{centeroids: 聚类中心点, cluster_ids: 聚类中心点ids, 其实聚类中心id就是数据点的分类编码}
  list(centeroids = structure(centeroids, names = names(data)), cluster_ids = classifier(centeroids))
}

# ---------------------------------------------------------------------
# 数据绘图
# ---------------------------------------------------------------------
# 调用函数进行聚类
data <- iris[, -5] # 剔除Species
km.res <- km(data, 3) # 聚类分析
print(km.res) # 打印结果

# 将聚类结果添加到原始数据中
data$Cluster <- as.factor(km.res$cluster_ids) # 加入聚类中心点

# ---------------------------------------------------------------------
# 长度
# ---------------------------------------------------------------------
# 创建散点图展示聚类结果
ggplot(data, aes(x = Sepal.Length, y = Petal.Length, color = Cluster)) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x = "花萼长度", y = "花瓣长度")

# ---------------------------------------------------------------------
# 宽度
# ---------------------------------------------------------------------
# 创建散点图展示聚类结果
ggplot(data, aes(x = Sepal.Width, y = Petal.Width, color = Cluster)) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x = "花萼宽度", y = "花瓣宽度")

# ---------------------------------------------------------------------
# 合并成一个绘图中进行显示
# ---------------------------------------------------------------------
data2 <- with(iris, rbind( # 分组 组织数据
  # -- 估计值
  data.frame(x = Sepal.Length, y = Petal.Length, species = as.factor(km.res$cluster_ids), label = "Length.Estimate"),
  data.frame(x = Sepal.Width, y = Petal.Width, species = as.factor(km.res$cluster_ids), label = "Width.Estimate"),
  # -- 实际值
  data.frame(x = Sepal.Length, y = Petal.Length, species = iris$Species, label = "Length.Actual"),
  data.frame(x = Sepal.Width, y = Petal.Width, species = iris$Species, label = "Width.Actual")
)) # with

# 数据绘图
ggplot(data2, aes(x = x, y = y, color = species)) +
  facet_wrap(. ~ label) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point() + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x = "花萼", y = "花瓣")
