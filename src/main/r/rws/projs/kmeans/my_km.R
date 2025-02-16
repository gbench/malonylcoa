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
# @return list列表{centeroids::矩阵类型的聚类中心点集合(一行代表一个点集合, 列为数据点在各个维度轴向的索引坐标),
#         cluster_ids: 聚类中心id, 即数据点的分类编码, cluster_id就是未给予正式的命名的概念或者说
#         是机器分析出的概念，一旦为cluster_id即分类id进行了命名,给出了带有人类文化&经验色彩的名称,
#         cluster_id或者说分类id就就变成了我们通常使用的概念了，它的意义将由该分类的下的数据元素的
#         共性所承载,classifier:数据分类器}
km <- function (data, k, eps = 0.01) {
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
  # @param data 数据点集合
  # @param p 指定数据点
  dist <- function (data, p) {
    apply(data, 1, compose(sqrt, sum, \(x) (x - p)^2))
  }

  # cluster_ids的本质是一个分类器(classifier), 即将拥有着共同的聚类中心的点视为同一个类别
  # @param data 数据点集合
  # @param cs k个聚类中心的索引坐标 (行:点索引编号从1到k, 列:各个维度坐标)
  # @return 获取聚类中点id索引号：分类标记
  cluster_ids <- function (data, cs) {
    apply(cs, 1, \(p) dist(data, p)) |> # 按行统计 ,计算各个点的距离, 结果为矩阵：(行data点编号,列cs点编号）
      apply(1, which.min) # 按照行统计， 找出各数据data点与cs各点距离中最小的cs索引点编号作为cluster_id
  }

  # K-Means++ 选择初始中心点
  # @param data 数据点集合, data.frame 或是 矩阵类型
  # @param k 聚类中心数量 整数类型
  # @return 初始中点集合: 矩阵类型
  kmeans_plus_plus <- function (data, k) {
    data <- if(!is.matrix(data)) as.matrix(data) else data # 转成矩阵以避免data.frame的data在按行索引取行值返回data.frame而非行向量
    if (k == 1) { # 唯一中心随机选择一项
      data[sample(nrow(data), 1), , drop = F] # 随机选择一个数据, drop=F 确保返回结果为矩阵
    } else if(k > 1) { # 多余一个中心点
      centers <- kmeans_plus_plus(data, k - 1) # 计算前k-1级别中心点
      dists <- apply(centers, 1, \(x) rowSums(sweep(data, 2, x)^2));# 计算各数据点与中心点的距离[data X centers的矩阵]
      wts <- apply(dists, 1, min) # 以数据点与中心点间最短的距离作为此后，继续参加备选中心点概率权重，距离越近概率越低
      wts[apply(data, 1, \(x) any(apply(centers, 1, \(y) all(x == y))))] <- 0; # 调整概率权重，已选则不再在参选，即权重为0
      rbind(centers, data[sample(nrow(data), 1, prob = wts / sum(wts)), ]) # 以新权重选出新中心点&追加,开始下一轮
    } else {
      stop("k 必须大于等于1")
    } #if
  }

  # 随机生成中心点
  # @param data 数据点集合
  # @param k 聚类中心数量
  # @return 初始中心点集合: 矩阵类型
  rand_cs <- function (data, k) {
    data[sample(1:n, k), , drop = F] |> as.matrix() # 随机选择k个中心点
  }
  
  #' 创建中心点初始化
  #' @param flag 聚类中心点的生成方式，是否使用随机生成的中心点， True 随机方式, False非随机方式
  # @return 初始中心点集合: 矩阵类型
  init_cs <- function (flag = F) if (flag) rand_cs(data, k) else kmeans_plus_plus(data, k) # 创建中心点
  
  #' 一直计算到中心点收敛到指定的误差范围之内eps：当前点.cs与先前点cs之间的各个维度坐标的差的绝对值小于eps
  #' @param cs 假定的中心点集合:矩阵类型
  #' @return 调整后的中心点集合:矩阵类型 
  loop <- function (cs = init_cs()) { 
    # 求出各个分类的样本的均值:作为准聚类中心点，更新聚类中心点
    .cs <- split(data, cluster_ids(data, cs)) |> # 计算各个数据点的聚类id
      lapply(\(x) apply(x, 2, mean)) |> # 计算每个分组的在各个维度上的坐标平均值
      Reduce(x = _, f = rbind) # 结果合并为数据框,组合成当前中心点.cs,即准中心点
    # 依据两次中心点cs,.cs之间的相对关系决定下一步操作：终止结束还是继续进行
    if (all(abs(cs - .cs) < eps)) # 当两次计算的聚簇中线点的坐标间的误差小于规定误差限度时终止算法，
         .cs # 中心点收敛，算法结束
    else loop ( if (nrow(.cs) < k) init_cs() # 中心点结构不完整，重新生成假定中心点已被再次重试 
                else .cs ) # 不收敛则再次循环, 继续调整
  } # loop

  centeroids <- loop() # 计算出最优的中心点位置

  # 分类器
  classifier <- function(data) cluster_ids(data, centeroids) # 将cluster_ids 作为数据点的分类器
  # 返回结果，{centeroids: :矩阵类型的聚类中心点, cluster_ids: 聚类中心点ids, 其实聚类中心id就是数据点的分类编码}
  list(centeroids = structure(centeroids, dimnames=list(1:nrow(centeroids), names(data))), cluster_ids = classifier(data), classifier = classifier)
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
# 使用分类器进行分类
print(km.res$classifier(iris[, -5]))

# ---------------------------------------------------------------------
# 数据绘图
# ---------------------------------------------------------------------

library(ggplot2)
library(scales) # hue_pal 函数所在的库

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
species_est <- append(as.factor(km.res$cluster_ids), as.factor(rep("centeroids", 3)))
species_act <- append(iris$Species, as.factor(rep("centeroids", 3)))
data2 <- with(rbind(iris[, -5], km.res$centeroids), rbind( # 分组 组织数据
  # -- 估计值
  data.frame(x = Sepal.Length, y = Petal.Length, species = species_est, label = "Length.Estimate"),
  data.frame(x = Sepal.Width, y = Petal.Width, species = species_est, label = "Width.Estimate"),
  # -- 实际值
  data.frame(x = Sepal.Length, y = Petal.Length, species = species_act, label = "Length.Actual"),
  data.frame(x = Sepal.Width, y = Petal.Width, species = species_act, label = "Width.Actual")
)) # with

# 获取除 centeroids 外的唯一类别
other_species <- with(data2, unique(species)[unique(species) != "centeroids"])
# 生成除 centeroids 外其他类别使用 scale_color_hue 生成的颜色
hue_colors <- hue_pal()(length(other_species))
names(hue_colors) <- other_species
# 构建包含 centeroids 为红色以及其他类别对应颜色的颜色向量
color_vector <- c("centeroids" = "darkred", hue_colors)
size <- ifelse(data2$species == "centeroids", 2, 1)

# 数据绘图
ggplot(data2, aes(x = x, y = y, color = species)) +
  facet_wrap(. ~ label) + # 属于同一聚类中心的点用同一颜色绘制
  geom_point(aes(size = size)) + # 散点描绘&涂色
  labs(title = "K-Means 聚类结果", x = "花萼", y = "花瓣") +
  scale_color_manual(values = color_vector) +
  scale_size_continuous(name = "size", range = range(size), breaks = seq(min(size), max(size), length.out = 2))
