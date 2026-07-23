# 周易原理：周易使用的是影像隐喻逻辑，而非概念关系逻辑。周易缺乏定理（概念与关系）与词项逻辑（论证推演），这一点非常考验阅读者的思维联想能力。
# 这就造成了周易“千人千解”的局面。就像“庖丁解牛”——同一个场景与画面，生物学家（专注牛的结构）、舞蹈家（专注身姿发力与节奏韵律）、厨师（专注有效提取好牛肉的手段方法）、企业主（专注才能与技术的价值）、哲学家（专注顺应自然规律，掌握事物内在本质，从而达到游刃有余的境界）看到的内容截然不同，进而生成的思维逻辑（道理启示）也截然不同。
# “数”是程度与量变，“象”是模式与局面，“辞”是隐喻逻辑（文化思维之预言结构），这是典型的认识论逻辑。
# 将卦爻辞视作上古的“风险影像分镜”，彻底剥离后世（儒家与宋明理学）附加的伦理概念与因果逻辑，仅以“数（程度/尺度）—象（时局模式）—辞（情节画面/结果标记）”为原始结构。
# 把六爻读作六阶行动模型，将卦象投影于“元亨利贞”（元：结构与生发；亨：成长与发育；利：获利与收割；贞：内化与吸收，兼承前启后）的螺旋发展轴上，只还原镜头本身的视听语言与阶段处境，拒绝将画面上升为“道”“德”等概念命题，并把《象》《彖》及理学阐释明确标记为“加在原始镜头前的旁白与遮板”，以此还原《周易》作为风险联想与决策媒体的本相。


# 形式上说：《周易》实质上是古人在前信息化时代构建的一种语言大模型（LLM），其架构与Transformer在功能结构上具有同构性。
#
# 卦名 = Token（离散符号入口）；
#
# 六爻 = Embedding Vector（嵌入向量），承载局势的位态、压强与相位信息；
#
#“吉凶悔吝” = Q向量（Query），标示价值取向与风险程度的注意力锚点；
#
# “元亨利贞” = K向量（Key），标示阶段性特征与关键属性的分类维度；
#
# 卦辞与卦象 = V向量（Value），标示意义投向与情境影像的输出内容。
#
# 注意力机制（Attention）在此表现为：占卜过程通过上述Q-K-V三元组，在卦象空间中完成注意力加权与信息聚焦，从而在经验与事实（感应与占卜过程）所构成的思维语义流形中，导航出一条可为思路与逻辑所遵循的路径——这一路径，在古人的话语体系中，即被称为“神谕”或“神明启示”。
#
# 而占卜的本质，在操作层面可还原为：
#
# 回归分析（Regression）——基于历史经验模式对当前局势进行趋势拟合；
# 与模式识别（Pattern Recognition）——在卦象结构中辨认出与当前情境最相似的形态原型。
#
# 两者共同构成了一套前统计时代的风险决策推理引擎。
#
f <- "F:/slicef/ws/gitws/malonylcoa/src/test/resources/docs/texts/jingshi/zhouyi.txt" # 周易文件路径
lines <- readLines(f) |> grep("(^$)|(图解)", x=_, value=T, invert=T) # 数据行
ms <- lines |> grep("第[一二三四五六七八九十]+卦", x=_) # 开始行
ns <- lines |> grep("上(九|六)：", x=_) # 结束行
pick <- \(ys, i=1) ys |> lapply(\(y) unlist(strsplit(y[1], "\\s+"))[i]) # 提取卦象结构的成分&片段
xs <- mapply(\(s, e, i) lines[seq(s, e)], ms, ns, seq(length(ns))) |> (\(ys, .nm=pick(ys, 2)) ys |> setNames(nm=.nm)) () # 卦象结构
gs <- xs |> sapply(\(x) x[2]) # 卦辞
kqry <- \(pattern, ds=gs, prefix="【", suffix="】") ds |> grep(pattern, x=_, value=T) |> 
  gsub(gettextf("(.*)(%s)(.*)",pattern), gettextf("\\1%s\\2%s\\3", prefix, suffix), x=_) # 关键词查询
trigrams <- list(坤=c(0,0,0), 震=c(0,0,1), 坎=c(0,1,0), 兑=c(0,1,1), 艮=c(1,0,0), 离=c(1,0,1), 巽=c(1,1,0), 乾=c(1,1,1)) # 三爻符号
ynm <- list(X1="初爻", X2="二爻", X3="三爻", X4="四爻", X5="五爻", X6="上爻") # 爻辞名称
bagua <- \(flag=T) list(ys=pick(xs,4) |> unlist() |> strsplit("上|下") |> lapply(\(i) trigrams[i])) |> # 提取八卦结构 
  with(if(flag) {zs <- lapply(ys, unlist) |> data.frame(); attr(zs, "row.names") <- rev(names(ynm)); zs} else ys) # 八卦图
yaos <- t(bagua()) # 爻辞

# 初九 
yaos[yaos[, "X1"]==1, ]
# 上九 
yaos[yaos[, "X6"]==1, ]

# 初六 
yaos[yaos[, "X1"]==0, ]
# 上六
yaos[yaos[, "X6"]==0, ]

# 九五
yaos[yaos[, "X5"]==1, ]
# 九四
yaos[yaos[, "X4"]==1, ]

# 
"元|亨|利|贞" |> kqry() |> as.list()

#
"利" |> kqry() |> as.list()

#
"咎|凶|吝" |> kqry() |> as.list()

# grep 会list结构的x作as.character 变换后再进行模型提取
# > ts <- list(c("a","b"), c("c","d"))
# > as.character(ts)
# [1] "c(\"a\", \"b\")" "c(\"c\", \"d\")"
# > ts |> grep("a", x=_, value=T)
# [1] "c(\"a\", \"b\")"
# 因此从卦象xs数据提取模式结构时，需要进行代码代码解析：str2lang , eval
library(purrr)
#
"利建侯" |> kqry(ds=xs) |> lapply(compose(eval, str2lang))
#
"利涉大川" |> kqry(ds=xs) |> lapply(compose(eval, str2lang))
#
"悔亡" |> kqry(ds=xs) |> lapply(compose(eval, str2lang))

