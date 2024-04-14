#描述用分解与组合，分解及时把目标的对象细节或者说特征进行测量与记录（measurment），
组合就是用逻辑关系（原因结论：因为所以，动机目的：为了&需要，空间分布前后左右，生长发育的过程，开始发展结束，包含&构成）将measuremeng进行组织成。观点和结论。
逻辑：因果关系，目的动因关系，空间分布关系，发展过程关系。

#语言是一种思想的表达，思想是一种概念的的结构,有层次（有结构）的逻辑细节就是组织。这种方法在计算机中被叫做通用表:也就是树形结构。
# ----------------------------------------------------------
require(lobstr)
ast(expr(a+b+c))
idea<-expr(a+b+c)
ast(!!idea)
ast(!!idea+!!idea)

> ast(expr(a+b+c))
█─expr 
└─█─`+` 
  ├─█─`+` 
  │ ├─a 
  │ └─b 
  └─c 
> idea<-expr(a+b+c)
> ast(!!idea)
█─`+` 
├─█─`+` 
│ ├─a 
│ └─b 
└─c 
> ast(!!idea+!!idea)
█─`+` 
├─█─`+` 
│ ├─█─`+` 
│ │ ├─a 
│ │ └─b 
│ └─c 
└─█─`+` 
  ├─█─`+` 
  │ ├─a 
  │ └─b 
  └─c 

# ----------------------------------------------------------
# 发展就是给一个方向（下一个步/元素的位置），然后下一步作为目标的从当前进行执行：
# 从当前到下一步，从下一步再到下下一步，依此类推直到无穷。
# 记录就是在平面上（下一步的位置要么在正右，要么在正下） 一行一行的从左向右，从上到下的 把 发展的过程 用符号进行 标记
#R的数据显示方法：索引从1开始，从上到下，从左到右
structure(1:36,dim=c(6,6))
--------------------------------------------------------
     [,1] [,2] [,3] [,4] [,5] [,6]
[1,]    1    7   13   19   25   31
[2,]    2    8   14   20   26   32
[3,]    3    9   15   21   27   33
[4,]    4   10   16   22   28   34
[5,]    5   11   17   23   29   35
[6,]    6   12   18   24   30   36

# 交流，就是 inqury 与 应答的过程：我问你答，你问我答。

#形式参数
formals(rnorm)

# 基本按住,rep(a,n) 重复a,n 次
rep(1:6,6) 
# structure, 为一个对象赋予制定属性,dim是一个独享的维度属性。
structure(rep(1:6,6),dim=c(6,6))

# attr 提取一定对象的属性。
attr(structure(rep(1:6,6),dim=c(6,6),name='zhangsan'),'name')
#生成数据框:
data.frame(structure(rep(1:6,6),dim=c(6,6)))->dd

#展开
expand.grid(dd)

# 对角相乘
outer(1:6,1:6)
# 加法
outer(1:7,1:9,FUN='+')

#联加法
sum(1:6)

#联合乘法
prod(1:6)
#6^6
prod(rep(6,6))

#循环
sapply(1:5,\(i) i^i)

#显示并绘图
sapply(1:9,\(i) i^i) |>barplot(main="X^X",xlab="x")

#组合方法
data.frame(x=1:3,y=c('a','b','c')) |> expand.grid() -> dd
#提取维度信息
dim(dd)
#class 属性,data.frame 其实就是一个list
data.frame(mode=mode(dd),typeof=typeof(dd),class=class(dd))
#class属性
class(list(i=1:6,name='a'))

#list 转换合格data.frame,data.frame 是一种特殊的list
list(i=1:6,name='a')|>as.data.frame()

#rlang::ensym 读取符号
x<-1
#ensym 定义一种符号计算的方式，expr(!!x+1) 构造了一种符号结构，用于表单特定概念。
add<-function(var) {x<-ensym(var);expr(!!x+1)}
eval(add(x))

#lobstr: 符号结构
library(lobstr)
#对象的结构
x <- 1:1e6
y <- list(x, x, x)
#
str(y)
#
ref(y)


# 环境的结构
e <- rlang::env()
e$self <- e
ref(e)

obj_size(x)
obj_size(y)
f <- function(x) g(x)
g <- function(x) h(x)
h <- function(x) x
# 显示调用的堆栈结构
f(cst())

#按照行遍历数据框
expand.grid(rep(1:6,6)|>matrix(ncol=6)|>data.frame())|>head(10)

#purrr
https://purrr.tidyverse.org/reference/map.html

#形式参数
formals(rnorm)

# defused 分解中断 （http://127.0.0.1:21349/library/rlang/html/topic-data-mask.html）
# R的代码可以被中断执行，也就是R的代码，在运行过程中可以进行动态编辑，即他可以被分解成：
# 1） defusal interrupted 动态结构编译（动态生成代码语法树）：expr
# 程序启动了，程序还没有写好好，一边干一边修改程序。类似于 开工了，图纸还没设计好，
# 需要临时停下来修改一下图纸参数（具体数据的结构细节&变量的组织关系等）。
# 例如：code<-expr(mean(cyl + am)) ，此时cyl与am还有数据框与之对应。我们先编码然后暂停interrupt执行。
# 可以视为把cyl与am的属主数据框给数据掩盖掉：Data-masking了。
# 2）resume to evaluate 恢复执行 : eval
# 找到mtcars数据后，即先前Data-masking的数据给暴露出来，在恢复执行code, eval(code,mtcars);
# 使用with暴露数据去除Data-masking：with(mtcars, mean(cyl + am))，
# 即用mtcars 加油/驱动 mean(cyl + am) 命令的执行。

# quosiquotation 注入injection !!var与 嵌入 {{var}}. 
# !! 其实就是R语言类似于C语言的宏替换，比如：
idea<-expr(a+b+c)
# 注入
# 错误，!! 是无法嵌入的，它只能在expr里左类似于 expr(!!idea+!!idea) 的宏替换
!!idea
# 单变量
expr(!!idea)
# 表达式
expr(!!idea+!!idea)
# 嵌入
# 错误
{{idea}}+{{idea}} 
# 展开
expr({{idea}}+{{idea}})
# 当使用在函数中使用 summarise这样的自动参数给defussal的函数
# 比如
my_mean <- function(data, var1, var2) dplyr::summarise(data, mean(var1 + var2))
my_mean(mtcars,cyl,am）
# 试图设置var1=cyl,var2=am,期望可以计算mean(cyl + am) 是不行的。
# 因为mean(var1 + var2)被defuse成与语法树的形式给冻结了，所以我们就需要把冻结的mean(var1 + var2)
# 给化开一点把var1,var2 分别用 cyl 与 am 用实际参数给嵌进去： dplyr::summarise(data, mean({{ var1 }} + {{ var2 }}))
# {{var1}},就是把var1的实际参数值给嵌入到mean(var1 + var2)语法数的意思，也就是：{{镶嵌位置}} 就是嵌入的语法。
# 注意这里并不是整个mean(var1 + var2)语法树给解冻，把var1給var2替换而已。所以并不会嵌入完成后立即执行mean(cyl+am)
# 而是依旧保持interrupted直到被dplyr::summarise结合了具体的data数据比如mtcars 后才恢复执行。
# 比如：
my_mean2 <- function(data, var1, var2) dplyr::summarise(data, mean({{var1}}+{{var2}}))
my_mean2(mtcars,cyl,am)
# !! 是无法嵌入的，它只能在expr里左类似于 expr(!!idea+!!idea) 的宏替换
my_mean3 <- function(data, var1, var2) dplyr::summarise(data, mean(!!var1+!!var2))
my_mean3(mtcars,cyl,am)