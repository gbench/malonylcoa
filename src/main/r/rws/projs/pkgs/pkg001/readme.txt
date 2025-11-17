# -----------------------------------------------------------------------------------
# 环境准备
# -----------------------------------------------------------------------------------

# 启动R程序，选择sqlquery.R等资料文件所在位置(这个目录不能包含DESCRIPTION文件
# 否则devtools会提示在其他项目内项目
> file.choose() |> dirname() |> setwd()
> getwd()
[1] "F:/slicef/ws/rws/dwk/2025-11-16/0942"

# 2025-11-16/0942 标志表示这是2025-11-16 09:42:00时候创建的临时工作目录

# -----------------------------------------------------------------------------------
# 打包过程
# -----------------------------------------------------------------------------------

# 包开发工具
library(devtools)

# 包名设置
pkgname <- 'malonylcoa' # 设置包名

# 创建R包项目工程
create_package(path=pkgname) 

# 创建一个README.md文件
use_readme_rmd() 

# 创建License（default MIT）和License.md文件
use_mit_license() 

# 源码与资料编辑准备
# 修改sqlquery.R文件（把./R/sqlquery.R文件移动到malonylcoa/R目录）
# 修改sqlfile.R文件（把./R/sqlfile.R文件移动到malonylcoa/R目录）
# 添加sql模板的实例文件 (把./inst/sql/kline.sql移动到malonylcoa/根目录)
# 修改DESCRIPTION文件(把./DESCRIPTION文件移动到malonylcoa/根目录）

# 编写测试文档
"dbfun,sqlquery,sqlexecute,ctsql,insql,upsql,sqlquery_h10ctp2,sqlquery_ctp,sqlfile,fill" |> 
  strsplit(",") |> unlist() |> lapply(\(x) use_test(x, F))

# 测试文件编写示例
cat >> F:/slicef/ws/rws/dwk/2025-11-16/0942/malonylcoa/tests/testthat/test-fill.R << EOF
test_that("fill works", {
  expect_equal(
    fill("select * from ##tbl where status=#status", 
      list("##tbl"="t_user", "#status"="active")), 
    "select * from t_user where status='active'")
})
EOF

# 执行测试&检验，直至完全通过检查
test()

# ℹ Testing malonylcoa
# ✔ | F W  S  OK | Context                                                                         
# ✔ |          1 | ctsql                                                                                      
# ✔ |          1 | dbfun
# ✔ |          1 | fill
# ✔ |          1 | insql
# ✔ |          1 | sqlexecute
# ✔ |          1 | sqlfile
# ✔ |          1 | sqlquery
# ✔ |          1 | sqlquery_ctp
# ✔ |          1 | sqlquery_h10ctp2
# ══ Results ══════════════════
# [ FAIL 0 | WARN 0 | SKIP 0 | PASS 10 ]

# 生产&更新 帮助文档与NAMSPACE文件
document()

# 再次检查(test之后的portable检查）
check()

# 打包生成
build()

# -----------------------------------------------------------------------------------
# 打包过程
# -----------------------------------------------------------------------------------

# 重启启动R & 安装包并使用

# 清空工作空间
rm(list=ls())

# 确保空间内容为空
# > ls()
# character(0)
# > 

# 移除程序包
"malonylcoa" |> strsplit(",") |> unlist() |> lapply(\(pkg) tryCatch(remove.packages(pkg, lib=.libPaths()), error=\(e) e))

# 确认没有包文件
# > library(malonylcoa)
# Error in library(malonylcoa) : there is no package called ‘malonylcoa’

# 安装包文件
install.packages("0942/malonylcoa_1.0.0.tar.gz", repos=NULL, type="source")

# > install.packages("0942/malonylcoa_1.0.0.tar.gz", repos=NULL, type="source")
# * installing *source* package 'malonylcoa' ...
# ** this is package 'malonylcoa' version '1.0.0'
# ** using staged installation
# ** R
# ** byte-compile and prepare package for lazy loading
# ** help
# *** installing help indices
# ** building package indices
# ** testing if installed package can be loaded from temporary location
# ** testing if installed package can be loaded from final location
# ** testing if installed package keeps a record of temporary installation path
# * DONE (malonylcoa)

# 加载包文件
library(malonylcoa)

# 库函数 查询
?sqlquery

# 方法测试
# > sqlquery("select database()")
# # A tibble: 1 × 1
#  `database()`
#  <chr>       
# 1 ctp2        
# >

# 查询数据表
# > sqlquery("show tables")
# # A tibble: 202 × 1
#    Tables_in_ctp2   
#    <chr>            
#  1 data             
#  2 t_ao2501_20241217
#  3 t_ao2501_20241218
#  4 t_ao2501_20241219
#  5 t_ao2501_20241220
#  6 t_ao2505_20241217
#  7 t_ao2505_20241218
#  8 t_ao2505_20241219
#  9 t_ao2505_20241220
# 10 t_ao2601_20251112
# # ℹ 192 more rows
# # ℹ Use `print(n = ...)` to see more rows
# > 

# 数据表删除
# > sqlexecute("drop table data")
# # A tibble: 1 × 2
#   affected_rows last_insert_id
#   <list>        <list>        
# 1 <int [1]>     <dbl [1]>     
# > 

# 数据表已经删除
# > sqlquery("show tables")
# # A tibble: 201 × 1
#    Tables_in_ctp2   
#    <chr>            
#  1 t_ao2501_20241217
#  2 t_ao2501_20241218
#  3 t_ao2501_20241219
#  4 t_ao2501_20241220

# 数据查询 : 
# 注意 ifnull(Volume - lag(Volume) over(), 0) Vol <==> ifnull(Volume - lag(Volume) over(order by Id), 0) 
# 因为 Id 是主键，所以 order by Id 与 数据库的默认结果顺序是一样的！
# 窗口函数的语法 expression over(XXX) 表示 在结果集合XXX上执行子执行表达式，XXX 通常式 把  对 select直接结果进行变换的 操作指令，
# 如 排序 order by, 分组 partition by 等操作, 框架 rows(索引范围)/range(值范围) 等：
#“rows between 9 preceding and current row ”近期10条数据（包含自己），即范围 {x[t-9], x[t-8], ..., x[t-1], x[t]}
#“range between 9 preceding and current row ”前面的x[i]与当前自己x[t]差差值小于9的范围区间：即 |x[t] - x[i]| ≤ 9
# Volume - lag(Volume) over() 表示在 查询结果上 直接运行 表达式 “Volume - lag(Volume)”
# Volume - lag(Volume) over(order by Id) 表示在 查询结果上 再去构建一个“order by Id”的子排序结果集，并在此子排序结果上 执行表达式“Volume - lag(Volume)”
#
sqlquery("select Id, LastPrice, Volume,  coalesce(Volume - lag(Volume) over(), 0) Vol , UpdateTime from t_ao2501_20241217")

# # A tibble: 11,521 × 5
#       Id LastPrice Volume   Vol UpdateTime
#    <int>     <dbl>  <int> <dbl> <chr>     
#  1     1      5179      0     0 19:55:32  
#  2     2      5179    105   105 20:59:01  
#  3     3      5172    127    22 21:00:01  
#  4     4      5174    147    20 21:00:01  
#  5     5      5177    163    16 21:00:02  
#  6     6      5177    166     3 21:00:02  
#  7     7      5177    192    26 21:00:03  
#  8     8      5178    204    12 21:00:03  
#  9     9      5178    234    30 21:00:04  
# 10    10      5178    251    17 21:00:04  
# # ℹ 11,511 more rows
# # ℹ Use `print(n = ...)` to see more rows
# >

# 近期10项的移动过平均
# SQL 移动平均查询，使用窗口函数计算价格的平均值。
# 对于每一行数据，计算当前行及其前面9行的LastPrice平均值
# 这是一个10期简单移动平均(SMA)
# 业务意义：
# 趋势识别 - 平滑价格波动，显示主要趋势方向
# 买卖信号 - 价格上穿/下穿均线可能产生交易信号
# 支撑阻力 - 移动平均线常作为动态支撑阻力位
# 噪声过滤 - 过滤短期价格波动，关注中长期走势
#
sqlquery("select Id, LastPrice, Volume,  avg(LastPrice) over(rows between 9 preceding and current row) avg , UpdateTime from t_ao2501_20241217")

# A tibble: 11,521 × 5
#       Id LastPrice Volume   avg UpdateTime
#    <int>     <dbl>  <int> <dbl> <chr>     
#  1     1      5179      0 5179  19:55:32  
#  2     2      5179    105 5179  20:59:01  
#  3     3      5172    127 5177. 21:00:01  
#  4     4      5174    147 5176  21:00:01  
#  5     5      5177    163 5176. 21:00:02  
#  6     6      5177    166 5176. 21:00:02  
#  7     7      5177    192 5176. 21:00:03  
#  8     8      5178    204 5177. 21:00:03  
#  9     9      5178    234 5177. 21:00:04  
# 10    10      5178    251 5177. 21:00:04  
# # ℹ 11,511 more rows
# # ℹ Use `print(n = ...)` to see more rows


# 对于每一行数据，计算在当前价格±9的价格区间内有多少条记录
# LastPrice: 100, 102, 105, 108, 110, 115, 120
# 对于 LastPrice = 110 的记录：计算价格在 [101, 119] 区间内（110±9）的记录数量
# 业务意义：
# 识别价格密集区 - Num值大的区域表示特定价格区间的交易活跃
# 支撑阻力位分析 - 高Num值可能表示特定价格区间的重要的价格水平
# 市场流动性分析 - 观察在不同价格区间的交易集中程度
#
sqlquery("select Id, LastPrice, Volume,  count(LastPrice) over(order by LastPrice range between 9 preceding and current row) Num , 
  UpdateTime from t_ao2501_20241217") -> dfm

# # A tibble: 11,521 × 5
#       Id LastPrice Volume   Num UpdateTime
#    <int>     <dbl>  <int> <dbl> <chr>     
#  1     3      5172    127     1 21:00:01  
#  2     4      5174    147     2 21:00:01  
#  3     5      5177    163     5 21:00:02  
#  4     6      5177    166     5 21:00:02  
#  5     7      5177    192     5 21:00:03  
#  6     8      5178    204    14 21:00:03  
#  7     9      5178    234    14 21:00:04  
#  8    10      5178    251    14 21:00:04  
#  9    11      5178    268    14 21:00:05  
# 10    13      5178    274    14 21:00:06  
# # ℹ 11,511 more rows
# # ℹ Use `print(n = ...)` to see more rows
# > 

# 绘图价格强度
with(dfm, plot(LastPrice, Num))
