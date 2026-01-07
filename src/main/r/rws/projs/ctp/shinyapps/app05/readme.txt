# 加载文件
# source(file.choose())
source("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app05/xxxconfig_boot.R")

# 加载文件
initialize()

# 提取单价成交量(多少成交量可以拉动一个价格挡位，价格拥堵情况，越是拥堵越价格越是难以波动）
klines()|>tail(20)|>with(ifelse(OPEN==CLOSE,0,VOLUME/abs(OPEN-CLOSE))|>setNames(nm=str_sub(TS,-4,-1)))|>(\(x){barplot(x,horiz=T,las=1);abline(v=mean(x)+(-1:1)*2*sd(x),lty=c(1,2,1))})()

# 持仓增量变化
klines()|>tail(20)|>filter(TIMES>1)|>with({x<-setNames(OINT,nm=str_sub(TS,-4,-1));plot(names(x),x);abline(h=mean(x)+(-1:1)*2*sd(x),lty=c(1,2,1))})
