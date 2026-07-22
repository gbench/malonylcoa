f <- "F:/slicef/ws/gitws/malonylcoa/src/test/resources/docs/texts/jingshi/zhouyi.txt" # 周易文件路径
lines <- readLines(f) |> grep("(^$)|(图解)", x=_, value=T, invert=T) # 数据行
ms <- lines |> grep("第[一二三四五六七八九十]+卦", x=_) # 开始行
ns <- lines |> grep("上(九|六)：", x=_) # 结束行
xs <- mapply(\(s, e, i) lines[seq(s, e)], ms, ns, seq(length(ns))) # 卦象结构
guas <- xs |> sapply(\(e) e[2]) # 卦辞
kqry <- \(pattern, ds=guas) ds |> grep(pattern, x=_, value=T) # 关键词查询

# 
"元|亨|利|贞" |> kqry()

#
"利" |> kqry()

#
"咎|凶|吝" |> kqry()

# grep 会list结构的x作as.character 变换后再进行模型提取
# > ts <- list(c("a","b"), c("c","d"))
# > as.character(ts)
# [1] "c(\"a\", \"b\")" "c(\"c\", \"d\")"
# > ts |> grep("a", x=_, value=T)
# [1] "c(\"a\", \"b\")"
# 因此从卦象xs数据提取模式结构时，需要进行代码代码解析：str2lang , eval
library(purrr)
"利建侯" |> kqry(ds=xs) |> lapply(compose(eval, str2lang))

