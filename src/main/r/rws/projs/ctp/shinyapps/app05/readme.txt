# 加载文件
# source(file.choose())
source("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app05/xxxconfig_boot.R")

# 加载文件
initialize()

# 指定时区
Sys.setenv(TZ="Asia/Shanghai")

# 持仓量时间序列
klines.xts() |> tail(30) |> with(plot(OINT1))

# K线绘图
local({
    xs1min <- klines.xts() |> with(cbind(OPEN,HIGH,LOW,CLOSE,VOLUME,TIMES,OINT1)) |> # 提取分钟K线
        setNames(nm=ss("Open,High,Low,Close,Volume,n,OpenInterest")) %>% .[.$n>1] # 过滤非法值
    xs5min <- to.minutes(xs, 5) |>setNames(nm=ss("Open,High,Low,Close,Volume"))#  项目聚合
    xs5min |> chartSeries(theme="white") # 聚合绘图
    
    # 收益率分布
    xs1min |> as_tibble()|> mutate(Return=(Close-Open)/Open) |> with(hist(Return)) # 1分钟收益率分布
    xs5min |> as_tibble()|> mutate(Return=(Close-Open)/Open) |> with(hist(Return)) # 5分钟收益率分布
})

# 提取单价成交量(多少成交量可以拉动一个价格挡位，价格拥堵情况，越是拥堵越价格越是难以波动）
klines()|>tail(20)|>with(ifelse(OPEN==CLOSE,0,VOLUME/abs(OPEN-CLOSE))|>setNames(nm=str_sub(TS,-4,-1)))|>(\(x){barplot(x,horiz=T,las=1);abline(v=mean(x)+(-1:1)*2*sd(x),lty=c(1,2,1))})()

# 持仓增量变化
klines()|>tail(200)|>filter(TIMES>1)|>with({x<-OINT;plot(x,xaxt="n");axis(side=1,at=seq_along(x),labels=str_sub(TS,-4,-1));abline(h=mean(x)+(-1:1)*2*sd(x),lty=c(1,2,1))})

# 成交量变化
klines()|>tail(200)|>filter(TIMES>1)|>with({x<-VOLUME;plot(x,xaxt="n");axis(side=1,at=seq_along(x),labels=str_sub(TS,-4,-1));abline(h=mean(x)+(-1:1)*2*sd(x),lty=c(1,2,1))})

# 本地绘图
local({
    origin <- par(mfrow=c(3, 1)); on.exit(par(origin)) # 绘图设计
    data <- klines()|>tail(200) |> filter(TIMES>1) # 过滤掉非法值
    data$RETURN<-with(data, (CLOSE-OPEN)/OPEN) # 收益率
    TS <- data$TS
    kplot <- \(x, lbls=str_sub(TS,-4,-1)) {
        key = as.character(substitute(x)[[3]])
        plot(x,xaxt="n", main=gettextf("Chart Of %s", key), ylab=key, xlab="TS")
        axis(side=1,at=seq_along(x), labels=lbls)
        abline(h=mean(x)+(-1:1)*2*sd(x), lty=c(1,2,1)) 
    }
    kplot(data$VOLUME);
    kplot(data$OINT)
    kplot(data$RETURN)
})
