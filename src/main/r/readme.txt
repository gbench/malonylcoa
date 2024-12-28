# 描述: 用 分解与组合 的方式记录去 表达(结构化) 事物。
# 1）分解 就是 把目标的对象的细节或者说特征 以 测量与记录（measurement）的形式进行组织罗列。
# 2）组合 就是 在对象的细节，获得的 测量与记录 中 发现 
#   2.1) 新的观点与关系,变化的 趋势/动力,或是
#   2.2) 形成 对未来/下一步的发展状况,状态,可行性的空间分布,属性特征等的看法与预测。
# 
# 联系：事物是普遍联系association，这种联系可以作为一种关联形成一种可以相互映射的反映。A->B, A<-B, A<->B.
# 记录：是一种association，它采用一种单向的关联反映：object->notation。
#   即采用一种notation，即symbol的结构.或者说把symbol组织成expression的形式去 复述/复制/表达object.
# 测量: 是一种特殊的关联记录:object->notation(unit)，他所采用的notation是一种被我们称为度量衡的unit标准单元的数量结构。
#   具体来说是 object->quantity*unit。 这种quantity*unit的记录语法是将object视为一种由unit作为基本构造单元，
#   通过quantity次的不断地 积累&重复 而构造出一种与object形成的等效关系。近似于或是无法在现实意义上进行有效区分。
#   即quantity倍的unit等价于object。或者是 object/unit=quantity. 也就是quantity是作为量化比率ratio来记录object。
#   一般而言我们就直接说object的measurement量化数值就是quantity。更一步我们将这种可以被measure的object成为variable.
# 变量: 可以测量的对象/属性。可以建立 object/variable->notation(unit) 关系的对象/属性。
#   属性被对象拥有/决定的可以被分辨与观测的特征。在采用模型化描述的时候，一般用 某个/些variables代表去代表整个object。
#   即对象事物是一种variable集合。或者说 对象以是一种属性variables的向量(var1,var2,...,varn) 来进行表达记录组织。
# ----------------------------------------------------------------------------------------------------------------------------
# 数据集: tabular data,tidy的表格数据是一种对象集合: 这是一种标准的理想的数据形式。
#   也可以理解我们就是需要如此的去准备数据记录：输入数据的样例。即tidyverse都是按照如此的结构去理解数据的
#   换句话说要是需要使用tidyverse去处理数据，就必须私用这样的语法与记录规则去准备数据。
#   用于表示对事物的个体，通过一定观测计量手段进行记录后的结果。
#   {obj1,var2,...} <=> 
#              	var1	var2	...	varn
#   object1	v11	v12	...	vn1
#   object2	v21	v22	...	v2n
#    ...		...	..	...	...
#    objectm	vm1	vm2	...	vm2n
# ----------------------------------------------------------------------------------------------------------------------------
# ref: https://r4ds.hadley.nz/data-visualize
# 1) A variable is a quantity, quality, or property that you can measure.
# 2) A value is the state of a variable when you measure it. The value of a variable may change from measurement to measurement.
# 3) An observation is a set of measurements made under similar conditions (you usually make all of the measurements in 
#   an observation at the same time and on the same object). An observation will contain several values, each associated 
#   with a different variable. We’ll sometimes refer to an observation as a data point.
# 4) Tabular data is a set of values, each associated with a variable and an observation. Tabular data is tidy if each value is 
#   placed in its own“cell”, each variable in its own column, and each observation in its own row.
# ----------------------------------------------------------------------------------------------------------------------------
# object1行 又被称为一次observation1。列var_i (var_1i,var_2i,...,var_mi) 构造事物的属性向量（跨越了多个观测），所以可以
# 近似var_i是事物即总体的属性var_i的观测记录，也可以理解为事物的var_i属性的本质。注意：本质是对类型而言的而不是个体。
#
# 模型：模型其实一种特殊的测量，object(y)->notation(VARs),VARs是一种变量向量(var1,var2,....,varn) 。简单的说就是
#  y~model(x1,x2,...,xn). 用测量的话来说就是我们通过组织（x1,x2,...,xn）形成一种新的unit去测量y. 特别当模型表示成:Y,X是矩阵
#  Y=X*alpha。即alpha一种Quantity向量，即作为一种 Q=Y/A的比例系数。（quantity1,quantity2,....,quantityn）.
#  此时X*Quantity就成称为线性回归模型。也就是用构造出一种x1,x2,...,xn）的计量单元去测量Y。
#
# 组合: 就是用逻辑关系（原因结论：因为所以，动机目的：为了/需要，空间分布/前后左右，生长发育的过程，
#   开始发展结束，包含&构成）将 variable/measurement 进行组织成 观点和结论。
#
# 逻辑：因果关系，目的动因关系，空间分布关系，发展过程关系。
#
# 语言是一种思想的表达，思想是一种概念的的结构,有层次（有结构）的逻辑细节就是组织。这种方法在计算机中被叫做通用表:也就是树形结构。
#
# ----------------------------------------------------------
require(lobstr)
ast(expr(a+b+c))
idea<-expr(a+b+c)
ast(!!idea)
ast(!!idea+!!idea)

# 语法树
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
# R的数据显示方法：索引从1开始，从上到下，从左到右
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
# 生成数据框:
data.frame(structure(rep(1:6,6),dim=c(6,6)))->dd

#展开
expand.grid(dd)

# 对角相乘
outer(1:6,1:6)
# 加法
outer(1:7,1:9,FUN='+')

# 联加法
sum(1:6)

# 联合乘法
prod(1:6)
# 6^6
prod(rep(6,6))

# 循环
sapply(1:5,\(i) i^i)

# 显示并绘图
sapply(1:9,\(i) i^i) |>barplot(main="X^X",xlab="x")

# 组合方法
data.frame(x=1:3,y=c('a','b','c')) |> expand.grid() -> dd
# 提取维度信息
dim(dd)
# class 属性,data.frame 其实就是一个list
data.frame(mode=mode(dd),typeof=typeof(dd),class=class(dd))
# class属性
class(list(i=1:6,name='a'))

# list 转换合格data.frame,data.frame 是一种特殊的list
list(i=1:6,name='a')|>as.data.frame()

# rlang::ensym 读取符号
x<-1
# ensym 定义一种符号计算的方式，expr(!!x+1) 构造了一种符号结构，用于表单特定概念。
add<-function(var) {x<-ensym(var);expr(!!x+1)}
eval(add(x))

# lobstr: 符号结构
library(lobstr)
# 对象的结构
x <- 1:1e6
y <- list(x, x, x)
# 结构
> str(y)
List of 3
 $ : int [1:1000000] 1 2 3 4 5 6 7 8 9 10 ...
 $ : int [1:1000000] 1 2 3 4 5 6 7 8 9 10 ...
 $ : int [1:1000000] 1 2 3 4 5 6 7 8 9 10 ...
> # 引用参考
> ref(y)
█ [1:0x1ba7030de68] <list> 
├─[2:0x1ba6edc9e68] <int> 
├─[2:0x1ba6edc9e68] 
└─[2:0x1ba6edc9e68] 

# 环境的结构
e <- rlang::env()
e$self <- e
> ref(e)
█ [1:0x1ba7191c718] <env> 
└─self = [1:0x1ba7191c718] 

> # 对象大小
> obj_size(x)
680 B
> obj_size(y)
760 B
> f <- function(x) g(x)
> g <- function(x) h(x)
> h <- function(x) x
> # 显示调用的堆栈结构
> f(cst())
    ▆
 1. ├─global f(cst())
 2. │ └─global g(x)
 3. │   └─global h(x)
 4. └─lobstr::cst()
> 

# 按照行遍历数据框
> expand.grid(rep(1:6,6)|>matrix(ncol=6)|>data.frame())|>head(10)
   X1 X2 X3 X4 X5 X6
1   1  1  1  1  1  1
2   2  1  1  1  1  1
3   3  1  1  1  1  1
4   4  1  1  1  1  1
5   5  1  1  1  1  1
6   6  1  1  1  1  1
7   1  2  1  1  1  1
8   2  2  1  1  1  1
9   3  2  1  1  1  1
10  4  2  1  1  1  1


# R的函数编程 purrr
https://purrr.tidyverse.org/reference/map.html

# 形式参数
formals(rnorm)
> formals(rnorm)
$n

$mean
[1] 0

$sd
[1] 1

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

# -------------------------------------------------------------------------------------------------------------------------------------------------
# 计算的原理就是：SUBSTITUTE(formula:形式结构,data:具体数据)  成
# => expression数据表达式 即，形如： op( op1(a, b, ... ), op2(x, y, ... ), ... )  的 formula 分解&恒等变形形式。
# 最后根据运算规则与操作数类型进行计算求解，这也是为何把数据分析与处理称为计算的原因。
#
# 第一、设计出形式结构formula和运算规则op；
# 第二、就是为形式结构formula填充数据内容，也就是把formula里面的变量符号给替换成具体数值，形成 实值填充的ParseTree即expression
# 第三、表达式求解，数值计算。
#
# 1） substitute 替换（变量替代）：适合于formula 结构复杂的单替换内容简单的情形。
# 2） bquote  反引用：适合于formula 结构简单单替换内容复杂的情形。
# -------------------------------------------------------------------------------------------------------------------------------------------------
# R的原生方法替换，substitute 的特点就是 他需要为替换的元素预先指定一个 符号,比如 x。对于复杂结构substitute有优势。
substitute(x+1, list(x=rep(1:3, 2)))

> 
c(1L, 2L, 3L, 1L, 2L, 3L) + 1

# 但是反引用的bquote则不需要可以直接使用.(expr:表达式)形式进行实际运算。所以对于简单结构bquote更有优势。要是结构复杂的化
# substitue的那种符号做占位符的formula结构更容易让人理解。反之bquote的写法就更为简洁，因为比较短。
bquote(.(rep(1:3, 2))+1)

>
c(1L, 2L, 3L, 1L, 2L, 3L) + 1

# 此外 bquote 还可以把使用 ..(exprs:表达式集合) 的方式进行语句的拼接编织铰接，这就是语句块的嵌入功能，对内容复杂时的替换就很有优势了。
bquote(function() {..(exprs)}, splice = TRUE, where=list(exprs=expression(x <- 1, y <- 2, x + y)))

>
function() {
    x <- 1
    y <- 2
    x + y
}
