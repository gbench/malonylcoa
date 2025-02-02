Haskell  语言介绍

任何 `语言` 都是一种 `记号体系`(Notational Conventions)，是一种 对 `物质决定意识` 的 `认知过程` 
加以 人为参与&处理 的 `符号记录`。具体而言, 就是 将 一种 现象存在(物质对象)  进行 `认知理解` (
认知理解 或者说 逻辑化 是指：根据/为 `现象存在/物质对象` 创建出对应的 `意识概念&关系结构` 
即 思维意识的针对性抽象) 形成 结构化 的 认知结论(全面完整的，成体系的  树状Tree式 `有条理有层次的` 
`意识概念&结果关系`)，然后 将 此认知结论 给予 记录表达(构建在某种文法符号之上的,有意义的 符号序列 
即 expression) 的 符号记录系统。

通常 将 认知理解（逻辑化）视为 一种 树形结构 即 有层次&条例 概念结构。
换句话说 就是 一种递归的分类概念体系，比如：
A
 | A1
 |   | A11
 |   | A12
 | A2
 |   | A21
 |   | A22

在 Hakell 中 `分类概念` 用 被称为 `类型`， Hakell 的 一大特色 就是它的 `类型系统`，
也就是: 它提供一种强大的 构造类型 的 方式&方法与工具。
Haskell 把类型分为：`静态数据类型 data`　与　`动态方法类型 class`

这里：我们先来说data类型:

``` r
# 颜色类型
color <- "Black,White" |> strsplit(",") |> unlist() ; color;
# 大小类型
size <- "Big,Small" |> strsplit(",") |> unlist() ; size;
# 动物类型
animal <- "Pig,Dog,Cat,Sheep,Cow,Horse" |> strsplit(",") |> unlist() ; animal;
# 数据模型：客观存在：物质模型
data <- list(animal=animal, color=color, size=size); data;
# 数据结构: 树形结构
tree <- function (hierachies)  data[match(hierachies, names(data))]  |> # 根据层级组织数据
  expand.grid() %>% {structure(., names=hierachies)} |> # 修改列名
  ftable(row.vars=hierachies); 
# 层级结构: 认知层级1
hierachies1 <- "animal,color,size" |> strsplit(",") |> unlist() ; tree(hierachies1)
# 层级结构: 认知层级2
hierachies2 <- "color,size,animal" |> strsplit(",") |> unlist() ; tree(hierachies2)
```
# 数据模型：客观存在：物质模型
# animal, color, size 为客观存在的 树形特征
> data <- list(animal=animal, color=color, size=size); data;
$animal
[1] "Pig"   "Dog"   "Cat"   "Sheep" "Cow"   "Horse"

$color
[1] "Black" "White"

$size
[1] "Big"   "Small"


# 认识发现，层级结构：hierachies1 <- "animal,color,size"，将数据特征data 按照hierachies2 进行组织&联系（物质联系）进而形成认知的意识概念体系（逻辑结构）
> hierachies1 <- "animal,color,size" |> strsplit(",") |> unlist() ; tree(hierachies1)
animal color size    
Pig    Black Big    1
             Small  1
       White Big    1
             Small  1
Dog    Black Big    1
             Small  1
       White Big    1
             Small  1
Cat    Black Big    1
             Small  1
       White Big    1
             Small  1
Sheep  Black Big    1
             Small  1
       White Big    1
             Small  1
Cow    Black Big    1
             Small  1
       White Big    1
             Small  1
Horse  Black Big    1
             Small  1
       White Big    1
             Small  1

# 认识发现，层级结构：hierachies2 <- "color,size,animal"，将数据特征 按照hierachies2 进行组织&联系（物质联系）进而形成认知的意识概念体系（逻辑结构）
> hierachies2 <- "color,size,animal" |> strsplit(",") |> unlist() ; tree(hierachies2)
color size  animal   
Black Big   Pig     1
            Dog     1
            Cat     1
            Sheep   1
            Cow     1
            Horse   1
      Small Pig     1
            Dog     1
            Cat     1
            Sheep   1
            Cow     1
            Horse   1
White Big   Pig     1
            Dog     1
            Cat     1
            Sheep   1
            Cow     1
            Horse   1
      Small Pig     1
            Dog     1
            Cat     1
            Sheep   1
            Cow     1
            Horse   1

# 现在根据上述的 客观现象(data 数据模型)进行 概念化 ：
# 所谓概念化 即 创建 ·意识概念&关系结构· 
#
# 创建树形结构（以Haskell  的形式进行 逻辑化组织 即 认知发现）
# 按照hierachies1 <- "animal,color,size"进行 关联（Associate）、组织(Organize) 
# 以 形成 认识（recognition) 的 意识概念逻辑(Knowledge) 即 树形结构化 的 分类层级 (Haskell的分类系统)

定义data类型的语法是：
-- ----------------------------------------------------------------------------------------------------------
-- 静态数据类型 data 的定义
-- ----------------------------------------------------------------------------------------------------------
:{ 
-- 颜色：不带有类型参数
data Color = Black | White deriving (Eq, Show, Enum)
-- 大小：带有类型参数
data Size = Big | Small deriving (Eq, Show, Enum)
--  动物：　带有类型参数
data Animal c s= -- 动物
 Pig {color::c , size::s} -- 猪
 | Dog {color::c , size::s} -- 狗
 | Cat {color::c , size::s} -- 猫
 | Sheep {color::c , size::s} -- 羊
 | Cow {color::c , size::s} --  牛
 | Horse {color::c , size::s} -- 马
 deriving (Eq, Show)
:}
--  找出狗对象
let anms=[ cons c s | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse], c <- [Black, White], s <- [Big, Small] ] in [ x | x<-anms, case x of (Dog _ _) -> True; _ -> False ]

-- ----------------------------------------------------------------------------------------------------------
-- 黑狗函数的定义
-- ----------------------------------------------------------------------------------------------------------
:{
--  是否是一只黑狗对象
isBlackDog :: Animal Color size -> Bool
isBlackDog a = case a of (Dog c s) -> c==Black; _ -> False
:}

--  是否是一只黑狗
isBlackDog (Dog (Black) (Small))
--  注意此处  相当于：let b=Black; s=Small in isBlackDog (Dog b s)
isBlackDog (Dog Black Small)
--  用$代替括号
let b=Black; s=Small in isBlackDog $ Dog b s
--  用$代替括号
isBlackDog $ Dog Black Small

--  获取对象颜色
color $ Dog Black Small

--  获取对象颜色
let anms=[ cons c s | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse], c <- [Black, White], s <- [Big, Small] ] in map color anms

有了数据类型，接下来我们再来说一下 动态方法类型 class 的语法结构， 
-- ----------------------------------------------------------------------------------------------------------
-- 动态方法类型 class 的定义
-- ----------------------------------------------------------------------------------------------------------
:{
--  a 类型的数据格式化
class (Show a) => CString a where
  -- 数据格式化
  toString :: a -> String
  toString = toLowerString . show where
      -- 将字符串转换为小写的函数
      toLowerString :: String -> String
      toLowerString s = [toLowerChar c | c <- s]
      -- 将单个字符转换为小写的函数
      toLowerChar :: Char -> Char
      toLowerChar c
        | c >= 'A' && c <= 'Z' = toEnum (fromEnum c + 32) -- 大写字符形式
        | otherwise = c -- 其他类型保持不变

-- Color 的 CString 方法格式化的自定义实现
instance CString Color where toString = show
-- Size 的 CString 方法格式化的默认实现
instance CString Size
-- 动物的格式化
instance CString (Animal Color Size) where
  toString anm = (concat $ [ f anm | f <- [show . color, show . size] ]) ++ 
    case anm of -- 分类执行
      (Pig _ _) -> "Pig"
      (Dog _ _) -> "Dog"
      (Cat _ _) -> "Cat"
      (Sheep _ _) -> "Sheep"
      (Cow _ _) -> "Cow"
      -- (Horse _ _) -> "Horse" -- 取消掉
      _ -> "Horse" -- 其他类型
:}

toString $ Small
toString $ Black
-- 对于 animal 需要进行 强制类型转换 Animal Color Size
toString $ (Pig Black Big)
-- 打印 Animal Color Size 格式
[toString $ (cons Black Big) | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse]]