# 把 long与wide格式进行相互转化的的示例
# 
#' @param data 待处理的数据,数据框，wide 或者 long 格式
#' @param varying # wide 中的复合结构列变量名称序列比如[x1,x2,y1,y2,...], 是一种[({v.names}{times})]的结构序列
#' @param v.names long 中的数据值列名称
#' @param timevar long 中的时间值times值所在的列名
#' @param idvar wide的行记录主键列，即观测值(obs)的主键所在列名
#' @param ids 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
#' @param times  wide格式中varying列名集合中蕴含的long格式的时间值times序列
#' @param drop 是从varying中删除sep
#' @param direction 变换目标的格式名称,long 长格式, wide 宽格式
#' @param new.row.names 当由wide转成long的时候，新生成的新的行的名称集合。
#' @param sep varying中v.names与times之间的分隔标记
#' @param split 分解varying的正则表达式，即varying的构成结构模式{v.names}{sep}{times}
function( # === 参数列表 ===
    data, # 待处理的数据
    varying = NULL, # wide 中的复合结构列变量名称序列比如[x1,x2,y1,y2,...], 是一种[({v.names}{times})]的结构序列
    v.names = NULL, # long 中的数据值列名称
    timevar = "time", # long 中的时间值times值所在的列名
    idvar = "id", # wide的行记录，即观测值的主键所在列名
    ids = 1L:NROW(data), # 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
    times = seq_along(varying[[1L]]), # wide格式中varying列名集合中蕴含的long格式的时间值times序列
    drop = NULL, # 是从varying中删除sep
    direction, # 变换目标的格式名称,long 长格式, wide 宽格式
    new.row.names = NULL, # 当由wide转成long的时候，新生成的新的行的名称集合。
    sep = ".", # varying中v.names与times之间的分隔标记
    split = if (sep == "") { # v.names与times之间没有，使用正则表达式方式
      list(regexp = "[A-Za-z][0-9]", include = TRUE) # 字母与数字比如x1,x2这样的模式
    } else { # 存在有效标记，”regexp = sep" 分隔符号
      list(regexp = sep, include = FALSE, fixed = TRUE)
    } # split
) { #  === 算法正文 ===
  if (!is.character(sep) || length(sep) != 1L) { # 确保sep 只能为单个字符的字符串
    stop(gettextf("'%s' must be a character string", "sep"),
      domain = NA
    )
  }
  # 返回与索引值ix所对应的data的name
  #' @param ix 索引值
  #' @return 返回与索引值ix所对应的data的name
  ix2names <- function(ix) {
    if (is.character(ix)) {
      ix
    } else {
      names(data)[ix]
    }
  }
  # 通过varying即名称去nms猜测v.names值名称(long格式的variable names)
  #' @param nms wide 格式中varying中的名称
  #' @param re 分解nms的正则表达式，即nms的构成结构模式{v.names}{re}{times}
  #' @param drop 分割符号sep 是否删除掉，T 删除掉, F 不予删除保留
  #' @param fixed 这是传递给strsplit的参数，T表示纯字符串, F表示正则表达式模式，默认为 F
  #' @return 值变量名称
  guess <- function(nms, re = split$regexp, drop = !split$include, fixed = split$fixed %||% FALSE) {
    # nn 是指 对 nms 进行re模式成分分解之后获得的nms的各个构成部件
    # 比如 nms=c("a.1","a.2","b.1","b.2") 会被分解成矩阵:[["a","1"],["a","2"],["b","1"],["b","2"]]
    # nn 的期望结构是{变量名称：name}{分隔模式：re}{时点名称:time}的模式
    if (drop) { # 模式构件re于结果里删除
      nn <- do.call("rbind", strsplit(nms, re, fixed = fixed))
    } else {# 模式构件re于结果里保留,regexpr(re, nms) 获取re的起始点index
      nn <- cbind( # 从后半部分的’regexpr(re, nms) + 1L‘来看，re 的长度应该指示一个字符。否则re就会被拆分的。
          substr(nms, 1L, regexpr(re, nms)), # 前半部分
          substr(nms, regexpr(re, nms) + 1L, 10000L) # 后半部分
      ) # nn
    } ## if
    if (ncol(nn) != 2L) { # 确保
      stop("failed to guess time-varying variables from their names")
    }
    vn <- unique(nn[, 1]) # nms 结构的 第一部分，变量名称
    v.names <- split(nms, factor(nn[, 1L], levels = vn)) # 把第一部分作为作为值名称
    times <- unique(nn[, 2L]) # 把第二部分，时点时刻
    attr(v.names, "v.names") <- vn # v.names 变量中写入 v.names 属性
    tt <- tryCatch(as.numeric(times), warning = function(w) times) # 尝试把times转换成数值，非数值情况则原来不变
    attr(v.names, "times") <- tt # # v.names 变量中写入 times 属性
    v.names # 返回长格式的值值变量名称
  }

  # 把宽格式转为长格式
  #' @param data 待处理的数据,数据框，wide 或者 long 格式
  #' @param varying # wide 中的复合结构列变量名称序列比如[x1,x2,y1,y2,...], 是一种[({v.names}{times})]的结构序列
  #' @param v.names long 中的数据值列名称
  #' @param timevar long 中的时间值times值所在的列名
  #' @param idvar wide的行记录主键列，即观测值(obs)的主键所在列名
  #' @param ids 默认的观测值的主键，如果不提供idvar，则采用ids作为wide行记录的主键。
  #' @param times  wide格式中varying列名集合中蕴含的long格式的时间值times序列
  #' @param drop 需要从data中剔除掉的列名集合 
  #' @param new.row.names 当由wide转成long的时候，新生成的新的行的名称集合。
  reshapeLong <- function(data, varying, v.names = NULL, timevar, idvar, 
    ids = 1L:NROW(data), times, drop = NULL, new.row.names = NULL) {
    # === 算法正文 ===
    ll <- unlist(lapply(varying, length)) # 提取varying中各个成员的数据长度
    if (any(ll != ll[1L])) { # 确保各个元素长度必须一致
      stop("'varying' arguments must be the same length")
    }
    if (ll[1L] != length(times)) { # 时间值 必须 与 varying 保持一致
      stop("'lengths(varying)' must all match 'length(times)'")
    }
    if (!is.null(drop)) { # 用户指定了删除列向量
      if (is.character(drop)) { # 当drop 是字符串向量的时候
        drop <- names(data) %in% drop # 在data的names向量中标记要删除的列名
      }
      data <- data[, if (is.logical(drop)) { !drop } else { -drop }, drop = FALSE] # 剔除掉drop中指定的各个列
    } # 从data 中删除的掉的由drop指定的列 
    # 逆操作基础信息
    undoInfo <- list(
      varying = varying, v.names = v.names,
      idvar = idvar, timevar = timevar
    )
    if (is.null(new.row.names)) {
      if (length(idvar) > 1L) {
        ids <- interaction(data[, idvar], drop = TRUE)
      } else if (idvar %in% names(data)) {
        ids <- data[, idvar]
      }
      if (anyDuplicated(ids)) {
        stop("'idvar' must uniquely identify records")
      }
    }
    d <- data
    all.varying <- unlist(varying)
    d <- d[, !(names(data) %in% all.varying), drop = FALSE]
    if (is.null(v.names)) {
      v.names <- vapply(varying, `[`, 1L, FUN.VALUE = character(1L))
    }
    rval <- do.call(rbind, lapply(seq_along(times), function(i) {
      d[, timevar] <- times[i] # 提取指定时刻点与varying.i相匹配
      varying.i <- vapply(varying, `[`, i, FUN.VALUE = character(1L)) # 注意varying.i与times[i]相对应
      d[, v.names] <- data[, varying.i]
      if (is.null(new.row.names)) {
        attr(d, "row.names") <- paste(ids, times[i],
          sep = "."
        )
      } else {
        row.names(d) <- new.row.names[(i - 1L) * NROW(d) +
          1L:NROW(d)]
      }
      d
    }))
    if (length(idvar) == 1L && !(idvar %in% names(data))) {
      rval[, idvar] <- ids
    }
    attr(rval, "reshapeLong") <- undoInfo
    return(rval)
  }
  reshapeWide <- function(data, timevar, idvar, varying = NULL,
                          v.names = NULL, drop = NULL, new.row.names = NULL) {
    if (!is.null(drop)) {
      if (is.character(drop)) {
        drop <- names(data) %in% drop
      }
      data <- data[, if (is.logical(drop)) {
        !drop
      } else {
        -drop
      }, drop = FALSE]
    }
    undoInfo <- list(
      v.names = v.names, timevar = timevar,
      idvar = idvar
    )
    orig.idvar <- idvar
    if (length(idvar) > 1L) {
      repeat ({
        tempidname <- basename(tempfile("tempID"))
        if (!(tempidname %in% names(data))) {
          break
        }
      })
      data[, tempidname] <- interaction(data[, idvar],
        drop = TRUE
      )
      idvar <- tempidname
      drop.idvar <- TRUE
    } else {
      drop.idvar <- FALSE
    }
    times <- unique(data[, timevar])
    if (anyNA(times)) {
      warning("there are records with missing times, which will be dropped.")
    }
    undoInfo$times <- times
    if (is.null(v.names)) {
      v.names <- names(data)[!(names(data) %in% c(
        timevar,
        idvar, orig.idvar
      ))]
    }
    if (is.null(varying)) {
      varying <- outer(v.names, times, paste, sep = sep)
    } else if (is.list(varying)) {
      varying <- do.call("rbind", varying)
    } else if (is.vector(varying)) {
      varying <- matrix(varying, nrow = length(v.names))
    }
    undoInfo$varying <- varying
    keep <- !(names(data) %in% c(
      timevar, v.names, idvar,
      orig.idvar
    ))
    if (any(keep)) {
      rval <- data[keep]
      tmp <- data[, idvar]
      really.constant <- unlist(lapply(rval, function(a) {
        all(tapply(
          a,
          as.vector(tmp), function(b) {
            length(unique(b)) ==
              1L
          }
        ))
      }))
      if (!all(really.constant)) {
        warning(
          gettextf(
            "some constant variables (%s) are really varying",
            paste(names(rval)[!really.constant], collapse = ",")
          ),
          domain = NA
        )
      }
    }
    rval <- data[!duplicated(data[, idvar]), !(names(data) %in%
      c(timevar, v.names)), drop = FALSE]
    for (i in seq_along(times)) {
      thistime <- data[data[, timevar] %in% times[i], ]
      tab <- table(thistime[, idvar])
      if (any(tab > 1L)) {
        warning(sprintf(
          "multiple rows match for %s=%s: first taken",
          timevar, times[i]
        ), domain = NA)
      }
      rval[, varying[, i]] <- thistime[match(
        rval[, idvar],
        thistime[, idvar]
      ), v.names]
    }
    if (!is.null(new.row.names)) {
      row.names(rval) <- new.row.names
    }
    if (drop.idvar) {
      rval[, idvar] <- NULL
    }
    attr(rval, "reshapeWide") <- undoInfo
    rval
  }
  if (missing(direction)) {
    undo <- c("wide", "long")[c("reshapeLong", "reshapeWide") %in%
      names(attributes(data))]
    if (length(undo) == 1L) {
      direction <- undo
    }
  }
  direction <- match.arg(direction, c("wide", "long"))
  switch(direction,
    wide = {
      back <- attr(data, "reshapeLong")
      if (missing(timevar) && missing(idvar) && !is.null(back)) {
        reshapeWide(data,
          idvar = back$idvar, timevar = back$timevar,
          varying = back$varying, v.names = back$v.names,
          new.row.names = new.row.names
        )
      } else {
        reshapeWide(data,
          idvar = idvar, timevar = timevar,
          varying = varying, v.names = v.names, drop = drop,
          new.row.names = new.row.names
        )
      }
    },
    long = {
      if (missing(varying)) {
        back <- attr(data, "reshapeWide")
        if (is.null(back)) stop("no 'reshapeWide' attribute, must specify 'varying'")
        varying <- back$varying
        idvar <- back$idvar
        timevar <- back$timevar
        v.names <- back$v.names
        times <- back$times
      }
      if (is.matrix(varying)) {
        varying <- split(c(varying), row(varying))
      }
      if (is.null(varying)) stop("'varying' must be nonempty list or vector")
      if (is.atomic(varying)) {
        varying <- ix2names(varying)
        if (missing(v.names)) {
          varying <- guess(varying)
        } else {
          if (length(varying) %% length(v.names)) stop("length of 'v.names' does not evenly divide length of 'varying'")
          ntimes <- length(varying) %/% length(v.names)
          if (missing(times)) {
            times <- seq_len(ntimes)
          } else if (length(times) !=
            ntimes) {
            stop("length of 'varying' must be the product of length of 'v.names' and length of 'times'")
          }
          varying <- split(varying, rep(v.names, ntimes))
          attr(varying, "v.names") <- v.names
          attr(varying, "times") <- times
        }
      } else {
        varying <- lapply(varying, ix2names)
      }
      if (missing(v.names) && !is.null(attr(varying, "v.names"))) {
        v.names <- attr(varying, "v.names")
        times <- attr(varying, "times")
      }
      reshapeLong(data,
        idvar = idvar, timevar = timevar, varying = varying,
        v.names = v.names, drop = drop, times = times, ids = ids,
        new.row.names = new.row.names
      )
    }
  )
}
