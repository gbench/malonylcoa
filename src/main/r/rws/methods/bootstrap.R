# 安装并加载必要的包
install.packages("boot")
library(boot)
batch_load()

# ------------------------------------------------------------------------

# 参数构建器
rb <- record.builder("##tbl,#startime,#endtime")
# 计算期货合约的收盘价的分布
ohlc <- \(symbol="rb2605", startime="09:00", endtime="12:00", date=strftime(Sys.Date(), "%Y%m%d"), keys=4:8) 
  `OHLCV1M` |> sqldframe(rb(gettextf("t_%s_%s", symbol, date), startime, endtime)) %>% # 填充合约K线sql模板
  with(xts(.[, keys], as.POSIXct(paste(Date, Time)))) # 分钟K线函数

xs <- ohlc("rb2605", startime='21:00', endtime="23:00", date='20251215', keys=4:8) 
btrs.close <- xs |> with(boot(Close[1:30], \(x, indices) mean(x[indices]), R = 1000))

# 查看结果:前10分钟的收盘价均值分布
print(btrs.close)
summary(btrs.close)

# 计算Bootstrap置信区间
boot_ci <- boot.ci(btrs.close, type = "perc")
print(boot_ci)

# 或者使用百分位数法
with(btrs.close, {
    alpha <- 0.05
    lower <- quantile(t, alpha/2)
    upper <- quantile(t, 1 - alpha/2)
    cat("95% Bootstrap置信区间: [", lower, ",", upper, "]\n")
})

# 数据绘图
with(btrs.close, {
    # 可视化Bootstrap结果
    hist(t, main="Bootstrap均值分布", xlab="收盘价均值", col="lightblue", border="white")
    # 添加参考线
    abline(v=mean(t), col="red", lwd=2, lty=2)
    abline(v=quantile(t, c(0.025, 0.975)), col="blue", lwd=2, lty=3)
})

# ------------------------------------------------------------------------

# 提取数据
ohlc("rb2605", startime='21:00', endtime="23:00", date='20251215', keys=4:8) |> with({
    # 假设检验：单样本均值检验（检验均值是高于特定值）
    close.expected=3050 # 收盘价是否能站上3050
    
    # 根据前30min交易判断收盘价是否能站上close.expected
    xs <- Close[1:30]
    observed_diff <- mean(xs) - close.expected
    btest.close <- boot(xs, \(x, indices, mu0) mean(x[indices]) - mu0, mu0 = close.expected, R = 2000)
    
    # 计算置信区间（百分位数法）
    ci_lower <- quantile(btest.close$t, 0.05)
    ci_upper <- quantile(btest.close$t, 0.95)
    
    # p值计算（单尾检验）
    p_value <- mean(btest.close$t >= observed_diff)
    
    # 结果输出
    cat("=== Bootstrap单样本均值检验结果 ===\n")
    cat("样本均值:", mean(xs), "\n")
    cat("检验值:", close.expected, "\n")
    cat("均值差值:", observed_diff, "\n")
    cat("90%置信区间:[", ci_lower, ",", ci_upper, "]\n")
    cat("单尾检验p值:", p_value, "\n")
   
    # 效应量计算
    effect_size <- observed_diff / sd(xs)
    cat("效应量(Cohen's d):", effect_size, "\n")

    # 决策
    if (p_value < 0.05) {
        cat("结果: 拒绝H0，均值显著高于", close.expected, "\n")
    } else {
        cat("结果: 不能拒绝H0，没有足够证据表明均值高于", close.expected, "\n")
    }
})