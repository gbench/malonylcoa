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

在 Hakell 中 `分类概念` 用 被称为 `类型`， Hakell 的 一大特色 就是它的 `类型系统`，
也就是: 它提供一种强大的 构造类型 的 方式&方法与工具。
Haskell 把类型分为：`静态数据类型 data`　与　`动态方法类型 class`

这里：我们先来说data类型，定义data类型的语法是：
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
 Pig {c::Color , s::Size} -- 猪
 | Dog {c::Color , s::Size} -- 狗
 | Cat {c::Color , s::Size} -- 猫
 | Sheep {c::Color , s::Size} -- 羊
 | Cow {c::Color , s::Size} --  牛
 | Horse {c::Color , s::Size} -- 马
 deriving (Eq, Show)
:}
--  找出狗对象
let anms=[ cons c s | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse], c <- [Black, White], s <- [Big, Small] ] in [ x | x<-anms, case x of (Dog _ _) -> True; _ -> False ]

-- ----------------------------------------------------------------------------------------------------------
-- 黑狗函数的定义
-- ----------------------------------------------------------------------------------------------------------
:{
--  是否是一只黑狗对象
isBlackDog :: forall {c :: Color} {s :: Size}. Animal c s -> Bool
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
c $ Dog Black Small

--  获取对象颜色
let anms=[ cons c s | cons <- [Pig, Dog, Cat, Sheep, Cow, Horse], c <- [Black, White], s <- [Big, Small] ] in map c anms

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

-- 实现转字符串格式化函数
instance CString Color where toString = show
instance CString Size
:}

toString $ Small
toString $ Black