#' JAVA 共享内存数据读取
#' ref https://gitee.com/gbench/malonylcoa/blob/master/src/main/java/gbench/util/array/SharedMem.java
#' ref https://gitee.com/gbench/malonylcoa/blob/master/src/test/java/gbench/sandbox/matrix/partitioner/SharedMemTest.java
#' read.shm("E:/slicee/temp/malonylcoa/array/mpg")
#' @param name 文件名
#' @param dir 路径
read.shm <- function(name, dir = NULL, show_progress = FALSE, encoding = "UTF-8") {
  path <- if (!is.null(dir)) {
    file.path(dir, basename(name))
  } else if (grepl("[/\\\\]", name) || file.exists(name)) {
    name
  } else {
    file.path(tempdir(), paste0("shm_", name))
  }

  if (!file.exists(path)) stop("File not found: ", path)
  file_size <- file.info(path)$size
  if (file_size == 0) stop("Shared memory file is empty: ", path)
  if (show_progress) message("Reading SHM file: ", path, " (", file_size, " bytes)")

  # 预定义常量
  STRING_WIDTHS <- c(STRING16 = 16, STRING32 = 32, STRING64 = 64, STRING128 = 128,
    STRING256 = 256, STRING512 = 512, STRING1024 = 1024, STRING2048 = 2048)
  
  read.strings <- function(con, type, n) {
    bytes <- STRING_WIDTHS[[type]] * 2
    raw <- readBin(con, "raw", bytes * n)

    # 批量处理字符串
    result <- character(n)
    for (i in seq_len(n)) {
      start_pos <- (i-1) * bytes + 1
      end_pos <- start_pos + bytes - 1

      # 找到字符串结束位置
      str_end <- start_pos
      while (str_end < end_pos) {
        if (raw[str_end] == 0 && raw[str_end + 1] == 0) break
        str_end <- str_end + 2
      }

      if (str_end == start_pos) {
        result[i] <- ""
      } else {
        result[i] <- iconv(list(raw[start_pos:(str_end-1)]), from = "UTF-16LE", to = encoding)
      }
    }
    result
  }

  read.date <- function(con, n) {
    epoch_days <- readBin(con, "integer", n, 8, signed = TRUE, endian = "big")
    as.Date(as.numeric(epoch_days), origin = "1970-01-01")
  }

  read.datetime <- function(con, n) {
    # 预分配
    result <- numeric(n)
    for (i in seq_len(n)) {
      epoch_seconds <- readBin(con, "integer", 1, 8, signed = TRUE, endian = "big")
      nano <- readBin(con, "integer", 1, 4, signed = TRUE, endian = "big")
      
      if (epoch_seconds == 0 && nano == 0) {
        result[i] <- NA_real_
      } else {
        result[i] <- as.numeric(epoch_seconds) + as.numeric(nano) / 1e9
      }
    }
    as.POSIXct(result, origin = "1970-01-01", tz = "UTC")
  }

  read.column <- function(con, type, n) {
    switch(type,
      BYTE    = readBin(con, "raw",      n),
      INT8    = readBin(con, "integer",  n, 1, signed = TRUE),
      INT16   = readBin(con, "integer",  n, 2, signed = TRUE,  endian = "big"),
      INT32   = readBin(con, "integer",  n, 4, signed = TRUE,  endian = "big"),
      INT64   = readBin(con, "integer",  n, 8, signed = TRUE, endian = "big"),
      FLOAT32 = readBin(con, "double",   n, 4, endian = "big"),
      FLOAT64 = readBin(con, "double",   n, 8, endian = "big"),
      DATE    = read.date(con, n),
      DATETIME = read.datetime(con, n),
      read.strings(con, type, n)  # 字符串类型
    )
  }

  con <- file(path, "rb")
  on.exit(close(con))

  meta.len <- readBin(con, "integer", 1, 4, endian = "big") # 读取元数据
  if (meta.len <= 0 || meta.len > 1e8) stop("Invalid meta length: ", meta.len)

  if (show_progress) message("Metadata size: ", meta.len, " bytes")

  meta.raw <- readBin(con, "raw", meta.len)
  meta <- jsonlite::fromJSON(rawToChar(meta.raw), simplifyDataFrame = FALSE)

  if (show_progress) message("Columns: ", length(meta$slots))

  offset <- 4 + meta.len
  total_rows <- if(length(meta$slots) > 0) meta$slots[[1]]$n else 0

  if (show_progress) message("Total rows: ", total_rows)

  cols <- vector("list", length(meta$slots)) # 读取所有列
  for (i in seq_along(meta$slots)) {
    s <- meta$slots[[i]]

    if (show_progress) {
      message(sprintf("  [%d/%d] %s (%s, %d rows)", i, length(meta$slots), s$x, s$t, s$n))
    }

    seek(con, offset + as.integer(s$s))
    cols[[i]] <- read.column(con, s$t, as.integer(s$n))
  }

  names(cols) <- sapply(meta$slots, `[[`, "x")
  result <- as.data.frame(cols, stringsAsFactors = FALSE) # 转换为data.frame

  if (show_progress) message("Done! Loaded ", nrow(result), " rows × ", ncol(result), " cols")

  result
}