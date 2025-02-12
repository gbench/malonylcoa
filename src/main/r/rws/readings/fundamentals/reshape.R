# 把 long与wide格式进行相互转化的的示例：reshape 函数很综合的体现R语言的主要编程思想，因此很有必要仔细的研读一下
#
# 需要重点关注以下几点：
# 1） 基本的数据数据结构
# 2） 向量化数据操作的编码技巧（数据索引，数据批量复制 与 创建）
# 3） 函数编程的思想
# 4） 大型程序（函数）的函数化分解 等。
# 
# 标准模式：varying：(x1,y1,x2,y2,x3,y3), times: (1,2,3,4), v.names(x,y), 简单的说
# 是采用 varying <- split(varying, rep(v.names, ntimes)) 对 varying时间格式分组的。
#
# varying 是一个复合结构列：可以理解为一个 v.names X times 的矩阵：
# -------------------------------
# | \time : 1   2   3   ..  时间
# | v |---:----------------------
# | n | x : x1  x2  x3  ..  ..
# | a | y : y1  y2  y3  ..  ..
# | m | z : z1  z2  z3  ..  ..
# | e | . : ..  ..  ..  ..  ..
# | 名| . : ..  ..  ..  ..  ..
# | 称| . : ..  ..  ..  ..  ..
# -------------------------------
# 在R中矩阵是列顺序优先的，由此上面的矩阵的atomic向量模式就是：x1,y1,z1, ... , x2,y2,z2, ..., x3,y3,z3, ...
# 或者 list(`1`=c("x1","y1","z1", ...), `2`=c("x2","y2","z2", ...), `3`=c("x3","y3","z3", ...))：
# （需要知道：list(`1`=c("x1","y1","z1"), `2`=c("x2","y2","z2"), `3`=c("x3","y3","z3"))[1] |> unlist() == c("x1","y1","z1")）
# 参见代码 reshapeLong：varying.i的使用部分
# do.call(rbind, lapply(seq_along(times), function(i) { # 遍历时刻向量,也就是varying矩阵的列
#   d[, timevar] <- times[i] # 读取第i个时刻的时间名称
#   varying.i <- vapply(varying, `[`, i, FUN.VALUE = character(1L)) # 提取第i个时刻所对应的varying中的复合结构列集合
#   d[, v.names] <- data[, varying.i] # 把复合结构以var.names的形式追加到结果times[i]的长格式区段中
#   ...
# }) # do.call
#
#' @param data 待处理的数据,数据框，wide 或者 long 格式
#' @param varying # wide 中的复合结构列的变量名称序列比如[x1,y1,..., x2,y2, ...], 是一种[({v.names}.{times})]的结构序列
#'                本质是一个[{1 -> c(x,y)},{2 -> c(x,y)}]的时间数据值v.names的映射关系.
#' @param v.names long 中的数据值列名称
#' @param timevar long 中的时间值times值所在的列名
#' @param idvar wide的行记录主键列，即观测值(obs)的主键所在列名
#' @param ids 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
#' @param times  wide格式中varying列名集合中蕴含的long格式的时间值times序列
#' @param drop 需要从data中剔除掉的列名集合 
#' @param direction 变换目标的格式名称,long 长格式, wide 宽格式
#' @param new.row.names 当由wide转成long的时候，新生成的新的行的名称集合。
#' @param sep varying中v.names与times之间的分隔标记
#' @param split 分解varying的正则表达式，即varying的构成结构模式{v.names}{sep}{times}：例如x1
function( # === 参数列表 ===
    data, # 待处理的数据
    varying = NULL, # wide 中的复合结构列变量名称序列比如[x1,y1,...,x2,y2,...], 是一种[({v.names}.{times})]的结构序列
    v.names = NULL, # long 中的数据值列名称
    timevar = "time", # long 中的时间值times值所在的列名
    idvar = "id", # wide的行记录，即观测值的主键所在列名
    ids = 1L:NROW(data), # 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
    times = seq_along(varying[[1L]]), # wide格式中varying列名集合中蕴含的long格式的时间值times序列
    drop = NULL, # 需要从data中剔除掉的列名集合 
    direction, # 变换目标的格式名称,long 长格式, wide 宽格式
    new.row.names = NULL, # 当由wide转成long的时候，新生成的新的行的名称集合。
    sep = ".", # varying中v.names与times之间的分隔标记
    split = if (sep == "") { # v.names与times之间没有，使用正则表达式方式
      list(regexp = "[A-Za-z][0-9]", include = TRUE) # 字母与数字比如x1,x2这样的模式
    } else { # 存在有效标记，”regexp = sep" 分隔符号
      list(regexp = sep, include = FALSE, fixed = TRUE)
    } # split
) { #  === 算法正文 ===

  if (!is.character(sep) || length(sep) != 1L) { # 确保sep为单个字符串的向量，也就是sep不能是向量化多值,只是一个普通的字符串
    stop(gettextf("'%s' must be a character string", "sep"), domain = NA) # 打印告警并退出 
  } # if

  # 返回与索引值ix所对应的data的name
  #' @param ix 索引值
  #' @return 返回与索引值ix所对应的data的name
  ix2names <- function(ix) {
    if (is.character(ix)) { # ix 是字符串
      ix # 字符串原值返回
    } else {# 非字符串：数值或是逻辑值
      names(data)[ix] # 索引data的names向量中的对应元素
    } # if
  } # ix2name

  # 通过varying即名称nms去猜测v.names值名称(long格式的variable names)
  #' @param nms wide 格式中varying中的名称： 结构为[x1,y1,...,x2,y2,...]
  #' @param re 分解nms的正则表达式，即nms的构成结构模式{v.names}{re}{times},比如:x1 
  #' @param drop 分割符号sep 是否删除掉，T 删除掉, F 不予删除保留, 需要注意这里的drop与reshape中的drop是不同含义的，guess重新定义drop的意义
  #' @param fixed 这是传递给strsplit的参数，T表示纯字符串, F表示正则表达式模式，默认为 F
  #' @return 值变量名称
  guess <- function(nms, re = split$regexp, drop = !split$include, fixed = split$fixed %||% FALSE) {
    # nn 是指 对 nms 进行re模式成分分解之后获得的nms的各个构成部件
    # 比如 nms=(x1,y1,x2,y2) 会被分解成矩阵:[["x","1"],["y","1"],["x","2"],["y","2"]]
    # nn 的期望结构是{变量名称：name}{分隔模式：re}{时点名称:time}的模式
    if (drop) { # 模式构件re于结果里删除
      nn <- do.call("rbind", strsplit(nms, re, fixed = fixed))
    } else {# 模式构件re于结果里保留,regexpr(re, nms) 获取re的起始点index
      nn <- cbind( # 从后半部分的’regexpr(re, nms) + 1L‘来看，re 的长度应该指示一个字符。否则re就会被拆分的。
       substr(nms, 1L, regexpr(re, nms)), # 前半部分,例如：就var123来说，此部分就是var
       substr(nms, regexpr(re, nms) + 1L, 10000L) # 后半部分,就var123来说，此部分就是123
      ) # nn
    } ## if

    if (ncol(nn) != 2L) { # 确保
      stop("failed to guess time-varying variables from their names")
    } # if

    vn <- unique(nn[, 1]) # nms 结构的 第一部分，变量名称
    v.names <- split(nms, factor(nn[, 1L], levels = vn)) # 把第一部分作为作为值名称
    times <- unique(nn[, 2L]) # 把第二部分，时点时刻
    attr(v.names, "v.names") <- vn # v.names 变量中写入 v.names 属性, 写到返回值的属性的做事是一种用返回单个值的形式返回多个值的技术手段,要注意掌握和理解

    tt <- tryCatch(as.numeric(times), warning = function(w) times) # 尝试把times转换成数值，非数值情况则原来不变
    attr(v.names, "times") <- tt # # v.names 变量中写入 times 属性,, 写到返回值的属性的做事是一种用返回单个值的形式返回多个值的技术手段

    v.names # 返回长格式的值变量名称
  } # guess

  # 把宽格式转为长格式
  #' @param data 待处理的数据,数据框，wide 或者 long 格式
  #' @param varying # wide 中的复合结构列变量名称序列比如[x1,y1,..., x2,y2, ...], 是一种[({v.names}.{times})]的结构序列
  #'        本质是一个[{1 -> c(x,y)},{2 -> c(x,y)}]的时间数据值v.names的映射关系.
  #' @param v.names long 中的数据值列名称
  #' @param timevar long 中的时间值times值所在的列名
  #' @param idvar wide的行记录主键列，即观测值(obs)的主键所在列名
  #' @param ids 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
  #' @param times  wide格式中varying列名集合中蕴含的long格式的时间值times序列
  #' @param drop 需要从data中剔除掉的列名集合 
  #' @param new.row.names 当由wide转成long的时候，新生成的新的行的名称集合。
  reshapeLong <- function(data, varying, v.names = NULL, timevar, idvar, ids = 1L:NROW(data), times, drop = NULL, new.row.names = NULL) {

    # === 算法正文 ===
    ll <- unlist(lapply(varying, length)) # 提取varying中各个成员的数据长度

    # 由于R语言是非严格类型的语言，非严格类型非常方便进行脚本，或是实时环境中执行, 脚本/命令(这对
    # 于调试与知识探索类应用很重要， 因为一个想法不是很成熟的时候，方向性的思路，要点逻辑远比周密
    # 细节，更能提供工作效率，R是面向统计学家的语言，所以它天生就是用于高度凝练的概括性的工作环境
    # 所谓行大事不拘小节，在特定的研究领域，环境是与信息一般都是极为确定和明确与转义，去掉繁文缛节，
    # 简单的直接的直奔主题，要比层层安检，处处提防的小心谨慎更有效率，这就是严格类型语言的意义特点）
    # 但对于模块化的函数就不是友好，因为它为函数入口给出了一个非常宽松的准入条件。没有对非法参数
    # 给予强制性限定，由此为了确保函数的正确运行，入口就需要对参数类型做一定的检查核对。一下就是
    # 对参数的校验。所以非严格类型语言，再写大型程序或是类库时候，即应对各种复杂的使用场景就不如严格
    # 类型语言上方便，因为他需要自己手段的进行类型检查

    if (any(ll != ll[1L])) { # 确保各个元素长度必须一致
      stop("'varying' arguments must be the same length")
    } # if

    if (ll[1L] != length(times)) { # 时间值 必须 与 varying 保持一致
      stop("'lengths(varying)' must all match 'length(times)'")
    } # if

    if (!is.null(drop)) { # 用户指定了删除列向量
      if (is.character(drop)) { # 当drop 是字符串向量的时候
        drop <- names(data) %in% drop # 在data的names向量中标记要删除的列名, 转名称索引drop为逻辑值索引
      } # if
      data <- data[, if (is.logical(drop)) { !drop } else { -drop }, drop = FALSE] # 剔除掉drop中指定的各个列（注意这里是复制品的删除）
    } # 从data 中删除的掉的由drop指定的列 

    # 逆操作基础信息
    undoInfo <- list(
      varying = varying, v.names = v.names,
      idvar = idvar, timevar = timevar
    ) # undoInfo

    if (is.null(new.row.names)) { # 没有提供新生成行名向量则采用默认的行名
      if (length(idvar) > 1L) {# 多个主键列
    	# interaction(1:3,1:3)则返回值为1.1,2.2,3.3的factor,带有笛卡尔全排列的levels
        # Levels: 1.1 2.1 3.1 1.2 2.2 3.2 1.3 2.3 3.3
        ids <- interaction(data[, idvar], drop = TRUE) # 生成主键列数据
      } else if (idvar %in% names(data)) { # 单个主键列
        ids <- data[, idvar] # 提取主键列数据
      } # if

      if (anyDuplicated(ids)) { # ids 值是否有重复， 注意本文duplicated的应用，也很很巧妙的
        stop("'idvar' must uniquely identify records")
      } # if
    } # new.row.names

    d <- data # 复制data数据,作为中间时间片的计算结果模具：单位初始模具
    # varying:{x:[x1,x2,y3],...;y:[y1,y2,y3]}, 相当于list("x"=c("x1","x2"),"y"=c("y1","y2")) |> unlist() 的 "x1" "x2" "y1" "y2" 
    all.varying <- unlist(varying) # 读取待展开的复合结构的列名集合，并给予扁平化成一维结构的向量, 只有扁平化以后才能狗进行向量化索引读取数据
    d <- d[, !(names(data) %in% all.varying), drop = FALSE] # 提取除了all.varying中的各个列去初始化d，单位初始模具

    if (is.null(v.names)) { # 用户没有提供long格式的数据值列名集合
      v.names <- vapply(varying, `[`, 1L, FUN.VALUE = character(1L)) # 将varying作为v.names
    } # if

    # 从wide格式逐个剥离出varing.i列然后将相应的值贴附始到模具d的timevar列之上，剪一条varying.粘到d$timevar然后把各个d串联起来，就形成了long
    rval <- do.call(rbind, lapply(seq_along(times), function(i) { # 每次冲times提取一个时间片然后将varying列上的数据
      d[, timevar] <- times[i] # 提取指定时刻点与varying.i相匹配，注意这里修改的是d在function(i)中的复制品当i运行完毕执行i+1时候d将恢复到初模具状态
      # varying:{x:[x1,x2,y3],...;y:[y1,y2,y3]}, 从 varying提取对应于时刻i的列名称：varying.i = [xi,yi,....]
      # 注意:vapply的结果varying.i是名称向量对应一组名称而不是一个.也就是一个时刻值对应多个v.names
      varying.i <- vapply(varying, `[`, i, FUN.VALUE = character(1L)) # 注意varying.i与times[i]相对应
      # 注意:这是向量化的批量操作的写法，R的向量化操作，对于批量化的数据的处理太方便了，说是神奇都不为过呀
      d[, v.names] <- data[, varying.i] # 为中间结果追加数据值列v.names, 其实就是从wide格式截取一条(varying.i)贴到d的最后一列之后
      
      if (is.null(new.row.names)) { # 用户没有提供新生成数行的的名称
        attr(d, "row.names") <- paste(ids, times[i], sep = ".") # 把times[i]作为该单位模具的行名后缀
      } else { # 用户提供了行名称，当前偏移位置为(i - 1L) * NROW(d)，注意这里每次书写了ROW(d)，向量化的批处理太简洁了，不在需要for循环了
        row.names(d) <- new.row.names[(i - 1L) * NROW(d) + 1L:NROW(d)] # 使用用户指定的new.row.names去初始化该新生成行的名称
      } # if
      d # 返回该时间片的单位片段。
    })) # rval

    if (length(idvar) == 1L && !(idvar %in% names(data))) { # 用户指定主键在宽格式的data中不存在
      rval[, idvar] <- ids # 指定的ids作为作为记录标识
    } # if

    attr(rval, "reshapeLong") <- undoInfo # 追加逆操作信息

    return(rval) # 返回结果值long格式
  } # reshapeLong 

  # 长格式转换为宽格式
  #' @param data 待处理的数据,数据框，wide 或者 long 格式
  #' @param timevar long 中的时间值times值所在的列名
  #' @param idvar wide的行记录主键列，即观测值(obs)的主键所在列名
  #' @param varying # wide 中的复合结构列变量名称序列比如[x1,y1,..., x2,y2, ...], 是一种[({v.names}.{times})]的结构序列
  #'        本质是一个[{1 -> c(x,y)},{2 -> c(x,y)}]的时间数据值v.names的映射关系.
  #' @param v.names long 中的数据值列名称
  #' @param drop 需要从data中剔除掉的列名集合 
  #' @param ids 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
  #' @param new.row.names 当由wide转成long的时候，新生成的新的行的名称集合。
  reshapeWide <- function(data, timevar, idvar, varying = NULL, v.names = NULL, drop = NULL, new.row.names = NULL) {

    # === 算法正文 ===
    if (!is.null(drop)) {  # 用户指定了删除列向量
      if (is.character(drop)) { # 当drop 是字符串向量的时候
        drop <- names(data) %in% drop # 在data的names向量中标记要删除的列名, 转名称索引drop为逻辑值索引
      } # if
      data <- data[, if (is.logical(drop)) { !drop } else { -drop }, drop = FALSE] # 剔除掉drop中指定的各个列（注意这里是复制品的删除）
    } #if
    
    # 逆运算操作参数信息详情
    undoInfo <- list(
      v.names = v.names, timevar = timevar,
      idvar = idvar
    ) # undo

    orig.idvar <- idvar # 保留原来的主键索引列 
    if (length(idvar) > 1L) { # 主键索引列中存在数据
      repeat ({ # 创建一个临时id名称列
        tempidname <- basename(tempfile("tempID")) # 生成一个随机字符串，类似于"tempID2c14328b3319"的名称
        if (!(tempidname %in% names(data))) { # 确保生成的字符串不与data数据框的变量名称相同
          break
        } # if
      }) # 直到，生成的字符串不与data数据框的变量名称相同为止

      # 注意，此时包括了idvar是向量复合主键列的情况, R的向量化运算很强大，特别是当以参数去进行索引读取数据的请示，就需要考虑该参数是多值向量的情况
      data[, tempidname] <- interaction(data[, idvar], drop = TRUE) # 临时主键索引初始化为原来的主键索引列
      idvar <- tempidname # 将主键索引列名更新为临时主键索引
      drop.idvar <- TRUE # 标记主键所以已经被更新替换掉了（删除）
    } else { # 原来的主键索引列不存在（没有数据），直接使用用户指定的主键索引列名
      drop.idvar <- FALSE # 标记主键索引列没有被替换
    } # if

    times <- unique(data[, timevar]) # 提取时间列中的数据的唯一值样本

    if (anyNA(times)) { # 确保时间列中的数据没有NA值
      # warning 是一种在函数内部输出日志信息的很有用的方式，可以理解为他是一种带重点提示的print
      warning("there are records with missing times, which will be dropped.")
    } # if

    undoInfo$times <- times # 把时间唯一值样本记录到逆操作参数信息之中

    if (is.null(v.names)) { # 用户没有提供值变量列名，尝试从 data列名与用户提供的timevar,idvar进行推导
      v.names <- names(data)[!(names(data) %in% c(timevar, idvar, orig.idvar))] # 将 除掉timevar,idvar列明依赖的变量列名都视为v.names
    } # if

    if (is.null(varying)) { # 用户没有指定varying
      # 示例：outer(c("a","b"),1:2,paste,sep=".")生成： [["a.1","b.1"],["a.2" "b.2"]] 的 {v.names}{sep}{times}的名称*时间的复合接结构列名矩阵
      varying <- outer(v.names, times, paste, sep = sep) # 使用v.names与times的外连接矩阵作为varying
    } else if (is.list(varying)) { # 用户指定的varying是一个列表结构，列表的每以行都是一个v.names项目
      varying <- do.call("rbind", varying) # 使用rbind将拼装成一个名称*时间的复合结构列名称矩阵
    } else if (is.vector(varying)) { # 向量模式
      varying <- matrix(varying, nrow = length(v.names)) # 将向量按照
    } # if

    undoInfo$varying <- varying # varying复合结构列信息（v.names*times）记录到逆操作参数信息之中

    keep <- !(names(data) %in% c(timevar, v.names, idvar, orig.idvar)) # 需要从长格式保留（复制到）宽格式中的剩余其他的列名
    if (any(keep)) { # 存在保留列集合
      rval <- data[keep] # 提取保留列数据
      tmp <- data[, idvar] # 以主键列作为分类标签
      really.constant <- unlist( lapply(rval, function(a) { # 判断keep列名集合里的各个列向量a是否是只有单一值的常量列（向量）
        all(tapply(a, as.vector(tmp), function(b) { # 判断列b是常量const的逻辑:tmp是按照主键分组，即这些值相对于特定主键是不变的,与主键是双射关系
          length(unique(b)) == 1L # 指定列b只有一个值的情况就是常量
        })) # all
      })) # really.const -- unlist 

      if (!all(really.constant)) { # 不都是常量, keep列中的数据应该都是与idvar中各个主键列一一对应的
        warning( # 打印告警信息，存在非常常量信息告警
          gettextf( # 格式化文本输出
            "some constant variables (%s) are really varying",
            paste(names(rval)[!really.constant], collapse = ",")
          ), domain = NA
        ) # warning
      } # if
    } # if keep

    # 初始结构就是每行都是唯一的idvar主键，且除掉timevar与v.names的其他各个数据列
    rval <- data[!duplicated(data[, idvar]), !(names(data) %in% c(timevar, v.names)), drop = FALSE] # 初始化一个返回值的核心结构
    for (i in seq_along(times)) { # 遍历时间值
      thistime <- data[data[, timevar] %in% times[i], ] # 提取时间段落数据
      tab <- table(thistime[, idvar]) # 统计idvar主键索引列的各个键值频数数据表

      if (any(tab > 1L)) { # 唯一索引中出现相同的名称的行，也就存在唯一索引不唯一情况
        warning(sprintf( "multiple rows match for %s=%s: first taken", timevar, times[i]), domain = NA) # 打印告警信息
      } # if

      # long 转 wide 的 核心代码：match(rval[, idvar]； 选定 idvar 所表标记数据行，依据 idvar 中的 主键索引进行批量复制&写入
      # 提取v.names中的数据列，写入wide里 varying[, i]，i即times索引去索引varying中宽格式的数据列名， 这一端甚为经典， 一行代码完成很复杂的功能
      rval[, varying[, i]] <- thistime[match(rval[, idvar], thistime[, idvar]), v.names] # 从long中提取idvar段落的行添加wide的相应列上。行转列
    } # for

    if (!is.null(new.row.names)) { # 用户指定了行名
      row.names(rval) <- new.row.names
    } # if

    if (drop.idvar) { # 如果id.var已被更改（使用算法生成的临时列）
      rval[, idvar] <- NULL # 伤处
    } # if

    attr(rval, "reshapeWide") <- undoInfo # 将你操作参数信息写入返回值的reshapeWide属性之中

    rval
  } # reshapeWide 
  
  # ------------------------------------------------------------------------------------- 
  # reshape真正开始执行的位置
  # ------------------------------------------------------------------------------------- 
  if (missing(direction)) { # 用户没有指定操作方向direction
    undo <- c("wide", "long")[c("reshapeLong", "reshapeWide") %in% names(attributes(data))] # 尝试从data中读取逆操作标识
    if (length(undo) == 1L) { # 存在逆操作参数信息
      direction <- undo # 使用你操作方向作为当前的操作方向
    } # if
  } # if

  direction <- match.arg(direction, c("wide", "long")) # 提取变换方向
  switch(direction, # 根据direction指令方向选择特定的执行子程序

    wide = { # 宽格式转换
      back <- attr(data, "reshapeLong") # 尝试获取你操作信息
      if (missing(timevar) && missing(idvar) && !is.null(back)) { # 没有逆操作信息 没有指定指定 timevar列 与 id主键列
        reshapeWide(data,
          idvar = back$idvar, timevar = back$timevar,
          varying = back$varying, v.names = back$v.names,
          new.row.names = new.row.names
        ) # reshapeWide
      } else { # 包括逆操作信息
        reshapeWide(data,
          idvar = idvar, timevar = timevar,
          varying = varying, v.names = v.names, drop = drop,
          new.row.names = new.row.names
        ) # reshapeWide
      } # if
    }, # wide 

    long = { # 长格式转换
      if (missing(varying)) { # 用户没有指定复合结构列的varying
        back <- attr(data, "reshapeWide") # 尝试从数据中读取逆操作信息
        # 可把stop 理解为print & exit的复合操作 而 warning则是printerr之类的的告警输出上的但因信息, 需要注意领会该内容
        if (is.null(back)) stop("no 'reshapeWide' attribute, must specify 'varying'") # 没有你操作信息且确实varying提示并退出。

        varying <- back$varying
        idvar <- back$idvar
        timevar <- back$timevar
        v.names <- back$v.names
        times <- back$times
      } # if

      if (is.matrix(varying)) { # 对于 varying 是矩阵的情况(v.names*times) 结构
        # row 函数是返回一个与varing 相同结构的矩阵，但是矩阵的每个元素都是该元素所在的行号索引
        # 例如：一个json格式的矩阵m:[["a.1","a.2"],["b.1","b.2"]], row(m): [[1,1],[2,2]] 
        # split(m,row(m))|>toJSON() 将返回按照行号进行分组的结构：{"1":["a.1","a.2"],"2":["b.1","b.2"]} ，
        # 即split的结构list结构即每个元素都一个数据值变量v.name,而该数据值变量的资源则是在时间维度上的展开：
        # 示例就是a:[a.1,a.2]这样的树形结构
        varying <- split(c(varying), row(varying)) # 对矩阵按照行进行分组
      } # if

      if (is.null(varying)) stop("'varying' must be nonempty list or vector") # 用户没有给出varying告警并退出

      if (is.atomic(varying)) { # varying为简单类型向量
        varying <- ix2names(varying) # 将索引转换成名称

        if (missing(v.names)) { # 没有给出长格式的数据值列名集合
          varying <- guess(varying) # 根据varying尝试猜测出v.names，v.names 与 times信息 将写在返回值的属性值之中
        } else { # 给出了长格式数据值列名集合
          if (length(varying) %% length(v.names)) stop("length of 'v.names' does not evenly divide length of 'varying'")
          ntimes <- length(varying) %/% length(v.names) # 根据varying与v.names长度推测times长度

          if (missing(times)) { # 没有给出时点名称序列
            times <- seq_len(ntimes) # 时点长度
          } else if (length(times) != ntimes) { # varying长度可以被v.names长度整除 
            stop("length of 'varying' must be the product of length of 'v.names' and length of 'times'")
          } # if
          # varying可以理解为:v.names X ntimes 的结构矩阵形式
          # wide 中的复合结构列变量名称序列比如[x1,y1,..., x2,y2, ...], 是一种[({v.names}.{times})]的结构序列
          # 本质是一个[{1 -> c(x,y)},{2 -> c(x,y)}]的时间数据值v.names的映射关系.
          varying <- split(varying, rep(v.names, ntimes)) # 按照 names 进行分组，每个name下包含ntimes个时点的数据
          attr(varying, "v.names") <- v.names
          attr(varying, "times") <- times
        } # if
      } else { # varying非简单类型向量 
        varying <- lapply(varying, ix2names) # 将索引转为名称
      } # if atomic

      if (missing(v.names) && !is.null(attr(varying, "v.names"))) {
        v.names <- attr(varying, "v.names") # 从属性值里读取数据，通过数据属性可以实现函数如何返回多个值的问题
        times <- attr(varying, "times") # 从属性值里读取数据，通过数据属性可以实现函数如何返回多个值的问题
      } # if

      # 把宽格式数据转换成长格式数据
      reshapeLong(data,
        idvar = idvar, timevar = timevar, varying = varying,
        v.names = v.names, drop = drop, times = times, ids = ids,
        new.row.names = new.row.names
      ) # reshapeLong
    } # long
  ) # switch
}
