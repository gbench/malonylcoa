f <- "F:/slicef/ws/gitws/malonylcoa/src/test/resources/docs/texts/jingshi/zhouyi.txt" # 周易文件路径
lines <- readLines(f) |> grep("(^$)|(图解)", x=_, value=T, invert=T) # 数据行
ms <- lines |> grep("第[一二三四五六七八九十]+卦", x=_) # 开始行
ns <- lines |> grep("上(九|六)：", x=_) # 结束行
xs <- mapply(\(s, e, i) lines[seq(s, e)], ms, ns, seq(length(ns))) # 卦象结构
guas <- xs |> sapply(\(e) e[2]) # 卦辞
guas |> grep("元|亨|利|贞", x=_, value=T)