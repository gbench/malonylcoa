# 安装并加载必要的包
if(!require(boot)) install.packages("boot")
library(boot)
batch_load()

# ------------------------------------------------------------------------

# 参数构建器
rb <- record.builder("##tbl,#startime,#endtime")
# 计算期货合约的收盘价的分布
ohlc <- \(symbol="rb2605", startime="09:00", endtime="12:00", date=strftime(Sys.Date(), "%Y%m%d"), keys=4:8) 
    `OHLCV1M` |> sqldframe(rb(gettextf("t_%s_%s", symbol, date), startime, endtime)) %>% # 填充合约K线sql模板
    with(xts(.[, keys], as.POSIXct(paste(Date, Time)))) # 分钟K线函数

system.time({
    # 直接使用元编程生成计算表达式expression:采用bootstrap方式来计算相应的指标统计量
    if(!require(boot)) install.packages("boot"); library(boot) # 加载bootstrap包，以便采用自助法进行有放回的重复抽样
    ohlcs <- \(pattern="rb2605_2025121", startime="09:00", endtime="23:00", keys=4:8, flag=T) {
        rb <- record.builder("##tbl,#startime,#endtime") # 参数构建器
        ohlc <- \(tbl) `OHLCV1M` |> sqldframe(rb(tbl, startime, endtime)) %>% with(xts(.[, keys], as.POSIXct(paste(Date, Time)))) # 分钟K线函数
        sqlquery("show tables") |> sort() |> grep(pattern, value=T, x=_) |> setNames(nm=_) |> # 提取指定表名模式的tickdata交易数据表
            lapply(ohlc) |> (\(.) if(flag) do.call(rbind, args=.) else .) () # 根据flag标记进行多日K线数据的合并
    } # 多日表K线的求值函数
    qux <- \(key, fn, probs = seq(0, 1, 0.25)) expr(xs |> # 生成四分位数表达式(采用bootstrap抽样生成5000个样本)
        with(boot(!!ensym(key), compose(!!ensym(fn), `[`), 5000)) |> with(quantile(t, !!probs))) # qu:quantile, x:统计量
    indgen <- \(fn) list(p=\(x) expr(qux(!!ensym(x), !!ensym(fn))) |> eval()) |> with(\(...) # 指标生成器
        ensyms(...) |> setNames(nm=_) |> lapply(\(x) expr(!!p(!!x))) %>% (\(.) expr(cbind(!!!.))) ()) # 指标生成器IndicatorGenerator
    xs <- ohlcs("rb2605_2025121")

    # 指标统计
    cat("mean:\n"); indgen(mean)(Open, High, Low, Close,  Volume) |> eval() |> print() # 使用指标生成器(均值统计)来计算指标分布形态
    cat("sd:\n"); indgen(sd)(Open, High, Low, Close, Volume) |> eval() |> print()  # 使用指标生成器(标准差统计)来计算指标分布形态
    cat("mad:\n"); indgen(mad)(Open, High, Low, Close, Volume) |> eval() |> print()  # 使用指标生成器(标准差统计)来计算指标分布形态
})

system.time({
    # 直接使用元编程生成计算表达式expression:采用bootstrap方式来计算相应的指标统计量
    if(!require(boot)) install.packages("boot"); library(boot) # 加载bootstrap包，以便采用自助法进行有放回的重复抽样
    ohlcs <- \(pattern="rb2605_2025121", startime="09:00", endtime="23:00", keys=4:8, flag=T) {
        rb <- record.builder("##tbl,#startime,#endtime") # 参数构建器
        ohlc <- \(tbl) `OHLCV1M` |> sqldframe(rb(tbl, startime, endtime)) %>% with(xts(.[, keys], as.POSIXct(paste(Date, Time)))) # 分钟K线函数
        sqlquery("show tables") |> sort() |> grep(pattern, value=T, x=_) |> setNames(nm=_) |> # 提取指定表名模式的tickdata交易数据表
            lapply(ohlc) |> (\(.) if(flag) do.call(rbind, args=.) else .) () # 根据flag标记进行多日K线数据的合并
    } # 多日表K线的求值函数
    btgen <- \(key, fn1) \(fn2, ...) expr(boot(!!ensym(key), compose(!!ensym(fn1), as.numeric, `[`), 5000) |> # bootstrap自助法计算fn1统计量并予以fn2分析的生成器函数：fn1统计量函数, fn2 统计量分析函数
        with(eval(call(!!as.character(ensym(fn2)), t, !!!match.call(expand.dots=F)$...)))) # 自助法生成器BootstrapGenerator
    btgen2 <- \(key, fn1) \(fn2, ...) expr(boot(!!ensym(key), compose(!!ensym(fn1), as.numeric, `[`), 5000) |> # bootstrap自助法计算fn1统计量并予以fn2分析的生成器函数：fn1统计量函数, fn2 统计量分析函数
        with(match.fun(!!ensym(fn2))(t, !!!match.call(expand.dots=F)$...))) # 自助法生成器BootstrapGenerator
    indgen <- \(fn) list(p=\(x) expr(btgen2(!!ensym(x), !!ensym(fn)) (quantile, probs=seq(0, 1, .25)))) |> with(\(...) # 指标生成器
        ensyms(...) |> setNames(nm=_) |> lapply(\(x) eval(p(!!ensym(x)))) %>% (\(.) expr(with(xs, cbind(!!!.)))) ()) # 指标生成器IndicatorGenerator
    xs <- ohlcs("rb2605_2025121")
    
    # 指标统计
    cat("mean:\n"); indgen(mean)(Open, High, Low, Close,  Volume) |> eval() |> print() # 使用指标生成器(均值统计)来计算指标分布形态
    cat("sd:\n"); indgen(sd)(Open, High, Low, Close, Volume) |> eval() |> print()  # 使用指标生成器(标准差统计)来计算指标分布形态
    cat("mad:\n"); indgen(mad)(Open, High, Low, Close, Volume) |> eval() |> print()  # 使用指标生成器(标准差统计)来计算指标分布形态
})


# 确定区间分布
ohlc("rb2605", startime='21:00', endtime="23:00", date='20251215', keys=4:8) |> 
    with(boot(Close, compose(mean, `[`), R=1000)) |> with(quantile(t, c(0.1, 0.9)))

xs <- ohlc("rb2605", startime='21:00', endtime="23:00", date='20251215', keys=4:8) 
btrs.close <- xs |> with(boot(Close[1:30], \(.xs, i) mean(.xs[i]), R = 1000))

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
    close.expected <- 3050
    xs <- Close[1:30]
    
    cat("样本大小:", length(xs), "\n")
    cat("样本均值:", mean(xs), "\n")
    
    # 1. 计算原始数据的bootstrap置信区间
    boot_orig <- boot(xs, compose(mean, `[`), R = 2000)
    ci <- quantile(boot_orig$t, c(0.05, 0.95))
    
    # 2. 假设检验：创建H₀分布(μ=3050)
    shifted_xs <- xs - mean(xs) + close.expected
    boot_h0 <- boot(shifted_xs, compose(mean, `[`), R = 2000)
    
    # 3. P值计算（右侧检验）
    p_value <- mean(boot_h0$t >= mean(xs)) # 使用H₀均值与实际试验值的有效差异度来计算差异度频率
    
    # 4. 可视化（可选）
    hist(boot_h0$t, main="H₀下的Bootstrap分布(μ=3050)", xlab="样本均值", col="lightblue")
    abline(v=mean(xs), col="red", lwd=2, lty=2)
    abline(v=close.expected, col="blue", lwd=2)
    legend("topright", legend=c(paste("观测均值 =", round(mean(xs), 2)), paste("H₀均值 =", close.expected)), col=c("red", "blue"), lty=2:1)
    
    # 5. 结果输出
    cat("\n=== Bootstrap单样本均值检验结果 ===\n")
    cat("检验假设: H₀: μ ≤", close.expected, " vs H₁: μ >", close.expected, "\n")
    cat("样本均值:", round(mean(xs), 2), "\n")
    cat("90%置信区间:[", round(ci[1], 2), ",", round(ci[2], 2), "]\n")
    cat("单尾检验p值:", round(p_value, 4), "\n")
    
    if (p_value < 0.05) {
        cat("结论: 拒绝H₀，均值显著高于", close.expected, "(p =", round(p_value, 4), ")\n")
    } else {
        cat("结论: 不能拒绝H₀，没有足够证据表明均值高于", close.expected, "(p =", round(p_value, 4), ")\n")
    }
    
    # 6. 附加决策建议
    if (mean(xs) > close.expected && p_value < 0.1) {
        cat("\n[交易建议] 收盘价站上3050的可能性较大\n")
    }
})

# 数据分层
system.time({
    x<-1:10 # 源数据
    strata <- x%%2 # 分层标记
    # 按层提取
    split(x, strata) |> lapply(\(x) boot(x, `[`, 5)$t) |> do.call(rbind, args=_) |> print()
    # 按层提取
    split(x, strata) |> lapply(\(x) boot(x, `[`, 5)$t) |> do.call(rbind, args=_) |> apply(2, mean) |> cat("\n")
    # 原生
    boot(x, compose(mean, `[`), 5, strata=strata) |> with(base::t(t)) |> cat("\n")
})
