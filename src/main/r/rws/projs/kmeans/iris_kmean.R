# 加载 iris 数据集
data(iris)
# 查看数据集的基本信息
str(iris)
# 查看数据集行数和列数
rows <- nrow(iris)
cols <- ncol(iris)
# 查看数据集行数和列数
print(paste("数据集中有", rows, "行", cols, "列"))
# 查看数据集行数和列数
head(iris)

# 去除数据集中的种类标签列
iris_features <- iris[, -5]
# 设置随机种子，保证结果可复现
set.seed(42)
# 进行 kmeans 聚类，设定聚类数为 3
kmeans_result <- kmeans(iris_features, centers = 3)
# 查看聚类结果
kmeans_result

# 安装并加载 fpc 包
if (!require(fpc)) {
    install.packages("fpc")
    library(fpc)
}
# 计算聚类结果的统计信息，包括轮廓系数
cluster_stats <- cluster.stats(dist(iris_features), kmeans_result$cluster)
# 提取轮廓系数
silhouette_avg <- cluster_stats$avg.silwidth
# 输出轮廓系数
print(paste("轮廓系数：", silhouette_avg))


# 安装并加载 ggplot2 包
if (!require(ggplot2)) {
    install.packages("ggplot2")
    library(ggplot2)
}
# 将聚类结果添加到原始数据中
iris$Cluster <- as.factor(kmeans_result$cluster)
# 创建散点图展示聚类结果
ggplot(iris, aes(x = Sepal.Length, y = Petal.Length, color = Cluster)) +
    geom_point() +
    labs(title = "K-Means 聚类结果",
         x = "花萼长度",
         y = "花瓣长度")
