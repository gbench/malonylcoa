#' JAVA 共享内存数据读取
#' ref https://gitee.com/gbench/malonylcoa/blob/master/src/main/java/gbench/util/array/SharedMem.java
#' ref https://gitee.com/gbench/malonylcoa/blob/master/src/test/java/gbench/sandbox/matrix/partitioner/SharedMemTest.java
#' read.shm("E:/slicee/temp/malonylcoa/array/mpg")
#' @param name 文件名
#' @param dir 路径
read.shm <- function(name, dir = NULL) {
  path <- if (!is.null(dir)) {
    file.path(dir, basename(name))
  } else if (grepl("[/\\\\]", name) || file.exists(name)) {
    name
  } else {
    file.path(tempdir(), paste0("shm_", name))
  }

  if (!file.exists(path)) stop("File not found: ", path)

  read.strings <- function(con, type, n) {
    widths <- c(STRING16 = 16, STRING32 = 32, STRING64 = 64, STRING128 = 128,
                STRING256 = 256, STRING512 = 512, STRING1024 = 1024, STRING2048 = 2048)
    bytes <- widths[[type]] * 2
    raw <- readBin(con, "raw", bytes * n)
    
    vapply(seq_len(n), function(i) {
      chunk <- raw[((i-1)*bytes + 1):(i*bytes)]
      len <- 0
      while (len < bytes - 1) {
        if (chunk[len+1] == 0 && chunk[len+2] == 0) break
        len <- len + 2
      }
      if (len == 0) return("")
      iconv(list(chunk[1:len]), from = "UTF-16LE", to = "UTF-8")
    }, "")
  }

  read.column <- function(con, type, n) {
    switch(type,
      BYTE    = readBin(con, "raw",    n),
      INT8    = readBin(con, "integer", n, 1, signed = TRUE),
      INT16   = readBin(con, "integer", n, 2, signed = TRUE,  endian = "big"),
      INT32   = readBin(con, "integer", n, 4, signed = TRUE,  endian = "big"),
      INT64   = readBin(con, "double",  n, 8, endian = "big"),
      FLOAT32 = readBin(con, "double",  n, 4, endian = "big"),
      FLOAT64 = readBin(con, "double",  n, 8, endian = "big"),
      read.strings(con, type, n)
    )
  }

  con <- file(path, "rb")
  on.exit(close(con))

  meta.len <- readBin(con, "integer", 1, 4, endian = "big")
  if (meta.len <= 0 || meta.len > 1e8) stop("Invalid meta length: ", meta.len)

  meta.raw <- readBin(con, "raw", meta.len)
  meta <- jsonlite::fromJSON(rawToChar(meta.raw), simplifyDataFrame = FALSE)

  offset <- 4 + meta.len

  cols <- lapply(meta$slots, function(s) {
    name  <- s[["x"]]           # 列名
    type  <- s[["t"]]           # 类型
    start <- s[["s"]]           # 起始偏移
    count <- s[["n"]]           # 行数
    
    seek(con, offset + as.integer(start))
    read.column(con, type, as.integer(count))
  })

  names(cols) <- sapply(meta$slots, `[[`, "x")
  as.data.frame(cols, stringsAsFactors = FALSE)
}
