Haskell  语言介绍

任何语言都是一种记号体系，是一种对 物质决定意识 的 的认知 过程 加以 符号记录
具体而言就是 将 一种 现象存在 （物质对象） 进行 认知理解（逻辑化） 形成
意识概念&结果关系 的 认知结论 然后 将 此认知结论 给予 记录表达 （expression）
的符号记录系统。

通常 将 认知理解（逻辑化）视为 一种 树形结构 即 有层次&条例 概念结构。
换句话说 就是 一种递归的分类概念体系，比如：
A
 | A1
 |   | A11
 |   | A12
 | A2
 |   | A21
 |   | A22

在 Hakell 中 分类概念 用 被成为类型， Hakell 的 一大特色 就是他的 类型系统，
也就是 它提供一种强大的 构造类型的 方法与工具。
Haskell 把类型分为　静态数据（data）类型　与　动态方法类型（class）
我们先来说data类型，定义data类型的语法是：
-- ----------------------------------------------------------------------------------------------------------
-- 数据类型的定义
-- ----------------------------------------------------------------------------------------------------------
:{ 
-- 颜色：不带有类型参数
data Color = Black | White deriving (Eq, Show, Enum)
-- 大小：带有类型参数
data Size = Big | Small deriving (Eq, Show, Enum)
--  动物：　带有类型参数
data Animal = 
 Pig {c::Color , s::Size} -- 猪
 | Dog {c::Color , s::Size} -- 狗
 | Cat {c::Color , s::Size} -- 猫
 | Sheep {c::Color , s::Size} -- 羊
 | Cow {c::Color , s::Size} --  牛
 | Horse {c::Color , s::Size} -- 马
 deriving (Eq, Show)
:}
--  找出狗对象
let as=[ cons c s | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse], c <- [Black, White], s <- [Big, Small] ] in [x | x<-as, case x of (Dog _ _) -> True; _ -> False ]

-- ----------------------------------------------------------------------------------------------------------
-- 黑狗函数的定义
-- ----------------------------------------------------------------------------------------------------------
:{
--　是否是一致黑狗对象
isBlackDog ::  Animal -> Bool
isBlackDog a = case a of (Dog c s) -> c==Black; _ -> False
:}

--  是否是一只黑狗
isBlackDog $ Dog Black Small

-- 获取对象颜色
c $ Dog Black Small

-- 获取对象颜色
let as=[ cons c s | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse], c <- [Black, White], s <- [Big, Small] ] in map c as