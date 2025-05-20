# ------------------------------------------------------------------------------------
# INVENTORY 库存&存货统计程序-主数据对象
#
# author:gbench@sina.com
# date: 2025-03-12
# ------------------------------------------------------------------------------------

startime <- "08:30" 
endtime <- "23:30"
current_date <- "2025-02-28"
dbhosts <- c("localhost"="localhost", "192.168.1.10"="192.168.1.10")
dbnames <- c("ctp"="ctp", "ctp2"="ctp2", "ctp4"="ctp4")
dbtbls <- c() # 数据表
instruments <- c() # 合约列表
plotmodes <- c("金叉死叉"="kdj", "回归趋势"="lm") # 绘图模式
timeframe <- 1 # 时间周期

# 随兴&自定义环境
env_adhoc <- new.env() # 函数环境
update.adhoc <- \(dbhost, dbname) { # 环境更新
  env_adhoc$sqlquery <- \(...) sqlquery(host = dbhost, dbname = dbname, ...) # 数据库查询函数
}
update.adhoc(dbhosts[1], dnames[1]) # 随兴&自定义环境