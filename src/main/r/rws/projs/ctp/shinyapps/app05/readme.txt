# 加载文件
# source(file.choose())
source("F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app05/xxxconfig_boot.R")
# 加载文件
initialize()
# 提取单位
klines()|>tail(20)|>with(ifelse(OPEN==CLOSE,0,VOLUME/abs(OPEN-CLOSE))|>setNames(nm=str_sub(TS,-4,-1)))|>(\(x){barplot(x);abline(h=mean(x),lty=2)})()
