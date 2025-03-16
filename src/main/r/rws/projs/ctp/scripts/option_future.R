#' 函数应用
#' @param f \(i：索引，name:字段名称, p:数据项, x: 源数据)
fapply <- \(x, f) lapply(seq_along(x), \(i) do.call(f, list(i = i, name = names(x)[i], p = x[i], x = x)))

# 期权期货数据
optfuts <- sqlquery("show tables") |> unlist() |> # 查询所有数据库表
  grep(pat = "rb(2504(p|c)\\d{4}|2505)_20250228", value = T) |> # 提取指定品种与日期的期权与期货合约
  sprintf(fmt = "select * from %s") |> sqlquery() |>
  (\(.) structure(., names = sub(".* t_(.+)$", "\\1", x = names(.))))()
optfuts |> sapply(nrow) |> sort()

# 提取期权期货数据的OHLCV数据
rb2504c3350_20250228 <- optfuts[["rb2504c3350_20250228"]] |> compute_kline()
rb2504c3400_20250228 <- optfuts[["rb2504c3400_20250228"]] |> compute_kline()
rb2505_20250228 <- optfuts[["rb2505_20250228"]] |> compute_kline()

# 绘制期货期权数据
xs <- (\(...) {
  data <- list(...) |> lapply(\(p) p$Close) |> Reduce(f=merge) |> na.omit()
  names(data) <- substitute(c(...)) |> # 此处...括起来的原因是保证可以提取所有参数，否则只能捕获一个
    deparse() |> gsub("^c\\((.+)\\)$", "\\1", x = _) |> strsplit(", ") |> unlist()
  plot.zoo(data)
  data
})(rb2504c3350_20250228, rb2504c3400_20250228, rb2505_20250228)

# 查看向量与相关系数区域&分布统计
list(cor=cor, cov=cov, summary=\(x) apply(x,2,summary)) |> lapply(\(f) do.call(f, list(xs)))
