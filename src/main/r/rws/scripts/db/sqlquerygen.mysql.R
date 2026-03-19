# MySQL 原生协议 R 实现 - 适用于 MySQL 8.0/9.0+
# 修正：认证切换时的序列号问题

if (!require("digest", quietly = TRUE)) {
  stop("请先安装 digest 包: install.packages('digest')")
}

# ==== 协议工具函数 ====

# 字节转整数（小端）
bytes_to_int <- function(bytes, start = 1, n = 4) {
  if (length(bytes) < start + n - 1) return(0)
  sum(as.integer(bytes[start:(start + n - 1)]) * 256^(0:(n - 1)))
}

# 打包整数为小端字节
pack_int <- function(x, n = 4) {
  x <- as.integer(x)
  as.raw(bitwAnd(bitwShiftR(rep(x, each = n), 8 * (0:(n - 1))), 255))
}

# 通用 Length-Encoded Integer/String 解析
read_lenenc <- \(bytes, pos, eval_bs=\(bs, start, end) if (start > end) "" else rawToChar(bs[start:end])) {
  if (pos > length(bytes)) return(NULL)
  
  first <- as.integer(bytes[pos])
  
  # 长度信息表：c(额外字节数, 长度读取字节数)
  info <- switch(as.character(first),
    "251" = list(extra = 0, len = 0, null = TRUE),           # NULL (0xfb)
    "252" = list(extra = 2, len = 2, null = FALSE),          # 2字节长度 (0xfc)
    "253" = list(extra = 3, len = 3, null = FALSE),          # 3字节长度 (0xfd)
    "254" = list(extra = 8, len = 8, null = FALSE),          # 8字节长度 (0xfe)
    "255" = NULL,                                             # 错误
    list(extra = 0, len = first, null = FALSE)               # 0-250 直接编码
  )
  
  if (is.null(info)) return(NULL)
  if (info$null) return(list(val = NA, next_pos = pos + 1))
  
  # 用 bytes_to_int 读取长度（如果 extra > 0）
  data_len <- if (info$extra == 0) info$len else bytes_to_int(pkt, pos + 1, info$len)
  data_start <- pos + 1 + info$extra
  data_end <- data_start + data_len - 1
  
  if (data_end > length(bytes)) list(error = TRUE, next_pos = pos + 1 + info$extra)
  else list(
    value = eval_bs(bytes, data_start, data_end),
    next_pos = data_end + 1,
    is_null = FALSE
  )
}

# MySQLConnection 数据库连接对象
# 
# | 首字节值                    | 含义          | 实际长度              |
# | ----------------------- | ----------- | ----------------- |
# | `0x00` - `0xfa` (0-250) | 直接表示长度      | 0-250 字节          |
# | `0xfb` (251)            | **NULL**    | 无后续数据             |
# | `0xfc` (252)            | 后续 2 字节表示长度 | 251-65535 字节      |
# | `0xfd` (253)            | 后续 3 字节表示长度 | 65536-16777215 字节 |
# | `0xfe` (254)            | 后续 8 字节表示长度 | 大字段               |
# | `0xff` (255)            | 保留/错误       | -                 |

MySQLConnection <- R6::R6Class("MySQLConnection",
  public = list(
    # 连接参数
    host = NULL,
    port = NULL,
    user = NULL,
    password = NULL,
    database = NULL,
    
    # 连接状态
    sock = NULL,
    seq_id = 0,
    expected_seq_id = 0,
    
    # 服务器信息
    protocol_version = NULL,
    server_version = NULL,
    thread_id = NULL,
    charset = NULL,
    server_status = NULL,
    capabilities = NULL,
    server_auth_plugin = NULL,
    
    # 认证数据
    salt = NULL,
    salt1 = NULL,
    salt2 = NULL,
    
    # 认证状态
    auth_phase = 0,
    
    initialize = function(host, port = 3306, user, password, database) {
      self$host <- host
      self$port <- port
      self$user <- user
      self$password <- password
      self$database <- database
    },
    
    connect = function() {
      self$sock <- socketConnection(host = self$host, port = self$port, blocking = TRUE, open = "r+b", timeout = 30)
      
      # 读取握手包
      self$read_handshake()
      
      # 发送认证包
      self$seq_id <- 1
      self$send_auth()
      
      # 处理认证响应
      self$handle_auth_response()
      
      invisible(self)
    },
    
    read_handshake = function() {
      pkt <- self$read_packet()
      if (length(pkt) < 5) stop("握手包太短")
      
      pos <- 1
      self$protocol_version <- as.integer(pkt[pos])
      pos <- pos + 1
      
      # 服务器版本
      version_bytes <- pkt[pos:length(pkt)]
      null_pos <- which(version_bytes == 0)[1]
      if (is.na(null_pos)) stop("握手包格式错误")
      self$server_version <- rawToChar(version_bytes[1:(null_pos-1)])
      pos <- pos + null_pos
      
      # 线程 ID
      self$thread_id <- bytes_to_int(pkt, 4)
      pos <- pos + 4
      
      # Salt part 1
      self$salt1 <- pkt[pos:(pos+7)]
      pos <- pos + 8
      
      pos <- pos + 1  # filler
      
      # 能力标志（低16位）
      self$capabilities <- as.integer(pkt[pos]) + bitwShiftL(as.integer(pkt[pos+1]), 8)
      pos <- pos + 2
      
      # 字符集
      self$charset <- as.integer(pkt[pos])
      pos <- pos + 1
      
      # 服务器状态
      self$server_status <- as.integer(pkt[pos]) + bitwShiftL(as.integer(pkt[pos+1]), 8)
      pos <- pos + 2
      
      # 能力标志（高16位）
      high_caps <- as.integer(pkt[pos]) + bitwShiftL(as.integer(pkt[pos+1]), 8)
      self$capabilities <- self$capabilities + bitwShiftL(high_caps, 16)
      pos <- pos + 2
      
      # Auth plugin 长度
      auth_plugin_len <- as.integer(pkt[pos])
      pos <- pos + 1
      
      # 保留字节
      pos <- pos + 10
      
      # Salt part 2
      if (pos + 11 <= length(pkt)) {
        self$salt2 <- pkt[pos:(pos+11)]
        pos <- pos + 12
      } else {
        self$salt2 <- raw(0)
      }
      
      # 认证插件名
      if (pos < length(pkt)) {
        plugin_bytes <- pkt[pos:length(pkt)]
        null_idx <- which(plugin_bytes == 0)[1]
        if (!is.na(null_idx) && null_idx > 1) {
          self$server_auth_plugin <- rawToChar(plugin_bytes[1:(null_idx-1)])
        } else if (is.na(null_idx) && length(plugin_bytes) > 0) {
          self$server_auth_plugin <- rawToChar(plugin_bytes)
        }
      }
      
      # 默认认证插件
      if (is.null(self$server_auth_plugin) || self$server_auth_plugin == "") {
        self$server_auth_plugin <- "mysql_native_password"
      }
      
      # 合并 salt
      self$salt <- c(self$salt1, self$salt2[1:min(12, length(self$salt2))])
      if (length(self$salt) < 20) {
        self$salt <- c(self$salt, raw(20 - length(self$salt)))
      }
      
      message(sprintf("连接到 MySQL %s", self$server_version))
    },
    
    calculate_auth = function(plugin, salt) {
      if (is.null(self$password) || nchar(self$password) == 0) return(raw(0))
      
      if (plugin == "mysql_native_password") {
        stage1 <- digest::digest(self$password, "sha1", serialize = FALSE, raw = TRUE)
        stage2 <- digest::digest(stage1, "sha1", serialize = FALSE, raw = TRUE)
        sha_salt_stage2 <- digest::digest(c(salt[1:20], stage2), "sha1", 
                                         serialize = FALSE, raw = TRUE)
        auth_response <- raw(20)
        for (i in 1:20) {
          auth_response[i] <- as.raw(bitwXor(as.integer(stage1[i]), 
                                            as.integer(sha_salt_stage2[i])))
        }
        return(auth_response)
      } else if (plugin == "caching_sha2_password") {
        hash1 <- digest::digest(self$password, "sha256", serialize = FALSE, raw = TRUE)
        hash2 <- digest::digest(hash1, "sha256", serialize = FALSE, raw = TRUE)
        hash3 <- digest::digest(c(hash2, salt[1:20]), "sha256", serialize = FALSE, raw = TRUE)
        auth_response <- raw(32)
        for (i in 1:32) {
          auth_response[i] <- as.raw(bitwXor(as.integer(hash1[i]), as.integer(hash3[i])))
        }
        return(auth_response)
      }
      return(raw(0))
    },
    
    send_auth = function() {
      # 客户端能力标志
      CLIENT_LONG_PASSWORD <- 1
      CLIENT_PROTOCOL_41 <- 512
      CLIENT_SECURE_CONNECTION <- 32768
      CLIENT_PLUGIN_AUTH <- 524288
      CLIENT_CONNECT_WITH_DB <- 8
      CLIENT_PLUGIN_AUTH_LENENC_DATA <- 131072
      
      client_flag <- CLIENT_LONG_PASSWORD
      client_flag <- bitwOr(client_flag, CLIENT_PROTOCOL_41)
      client_flag <- bitwOr(client_flag, CLIENT_SECURE_CONNECTION)
      client_flag <- bitwOr(client_flag, CLIENT_PLUGIN_AUTH)
      client_flag <- bitwOr(client_flag, CLIENT_CONNECT_WITH_DB)
      client_flag <- bitwOr(client_flag, CLIENT_PLUGIN_AUTH_LENENC_DATA)
      
      # 使用服务器要求的插件计算认证响应
      auth_response <- self$calculate_auth(self$server_auth_plugin, self$salt)
      
      # 构建认证包
      pkt <- c(
        self$pack_int32(client_flag),
        self$pack_int32(16777216),  # max packet size
        as.raw(33),                  # charset utf8
        raw(23),                     # reserved
        charToRaw(self$user), as.raw(0)
      )
      
      # 认证响应
      if (length(auth_response) > 0) {
        pkt <- c(pkt, as.raw(length(auth_response)), auth_response)
      } else {
        pkt <- c(pkt, as.raw(0))
      }
      
      # 数据库名
      if (!is.null(self$database) && nchar(self$database) > 0) {
        pkt <- c(pkt, charToRaw(self$database), as.raw(0))
      } else {
        pkt <- c(pkt, as.raw(0))
      }
      
      # 认证插件名
      pkt <- c(pkt, charToRaw(self$server_auth_plugin), as.raw(0))
      
      self$write_packet(pkt)
      message(sprintf("发送认证包，使用插件: %s", self$server_auth_plugin))
    },
    
    handle_auth_response = function() {
      pkt <- self$read_packet()
      if (length(pkt) == 0) stop("连接失败：无响应")
      
      msg_type <- as.integer(pkt[1])
      
      if (msg_type == 0x00) {
        # OK 包 - 认证成功
        message("认证成功")
        return(TRUE)
      } else if (msg_type == 0xff) {
        # 错误包
        err_code <- as.integer(pkt[2]) + bitwShiftL(as.integer(pkt[3]), 8)
        err_msg <- rawToChar(pkt[4:length(pkt)])
        stop(sprintf("MySQL错误 [%d]: %s", err_code, err_msg))
      } else if (msg_type == 0xfe) {
        # Auth Switch Request
        self$handle_auth_switch(pkt)
      } else if (msg_type == 0x01) {
        # caching_sha2_password 额外认证
        self$handle_caching_sha2_auth(pkt)
      } else {
        stop(sprintf("未知响应类型: %d", msg_type))
      }
    },
    
    handle_auth_switch = function(pkt) {
      if (length(pkt) < 2) stop("Auth switch 包太短")
      
      # 解析插件名
      pos <- 2
      plugin_bytes <- pkt[pos:length(pkt)]
      null_idx <- which(plugin_bytes == 0)[1]
      
      if (is.na(null_idx)) {
        stop("Auth switch 包格式错误")
      }
      
      new_plugin <- rawToChar(plugin_bytes[1:(null_idx-1)])
      pos <- pos + null_idx
      
      # 获取新的 salt
      if (pos <= length(pkt)) {
        new_salt <- pkt[pos:length(pkt)]
        # 移除末尾的 null
        if (length(new_salt) > 0 && new_salt[length(new_salt)] == 0) {
          new_salt <- new_salt[-length(new_salt)]
        }
      } else {
        new_salt <- self$salt
      }
      
      message(sprintf("服务器要求切换认证插件: %s", new_plugin))
      
      # 确保 salt 长度足够
      if (length(new_salt) < 20) {
        new_salt <- c(new_salt, raw(20 - length(new_salt)))
      }
      
      # 计算新的认证响应
      auth_response <- self$calculate_auth(new_plugin, new_salt)
      
      # 重要：认证切换时，序列号应该使用当前值，不需要增加
      # 直接发送响应
      self$write_packet(auth_response)
      
      # 继续处理认证
      self$handle_auth_response()
    },
    
    handle_caching_sha2_auth = function(pkt) {
      if (length(pkt) < 2) {
        self$handle_auth_response()
        return()
      }
      
      auth_status <- as.integer(pkt[2])
      
      if (auth_status == 0x03) {
        # 快速认证成功
        message("caching_sha2_password 快速认证成功")
        
        # 读取后续的 OK 包
        next_pkt <- self$read_packet()
        if (length(next_pkt) > 0 && as.integer(next_pkt[1]) == 0x00) {
          return(TRUE)
        }
        return(TRUE)
      } else if (auth_status == 0x04) {
        # 需要完整认证
        message("caching_sha2_password 需要完整认证，发送密码...")
        
        # 发送密码（这里简化处理，实际应该用 RSA 加密）
        pwd_pkt <- c(charToRaw(self$password), as.raw(0))
        self$write_packet(pwd_pkt)
        
        # 读取认证结果
        result_pkt <- self$read_packet()
        if (length(result_pkt) > 0) {
          if (as.integer(result_pkt[1]) == 0x00) {
            message("密码认证成功")
            return(TRUE)
          } else if (as.integer(result_pkt[1]) == 0xff) {
            err_code <- as.integer(result_pkt[2]) + bitwShiftL(as.integer(result_pkt[3]), 8)
            err_msg <- rawToChar(result_pkt[4:length(result_pkt)])
            stop(sprintf("密码认证失败 [%d]: %s", err_code, err_msg))
          }
        }
      }
      
      self$handle_auth_response()
    },
    
    query = function(sql) {
      # 重置序列号
      self$seq_id <- 0
      
      # 发送查询命令
      self$write_packet(c(as.raw(0x03), charToRaw(sql))) # 通过 COM_QUERY (0x03) 发送 SQL, 结果集返回的数据类型都是字符串
      
      # 读取结果
      self$read_result()
    },
    
    read_result = function() {
      pkt <- self$read_packet()
      if (length(pkt) == 0) return(data.frame())
      
      msg_type <- as.integer(pkt[1])
      
      if (msg_type == 0xff) {
        err_code <- as.integer(pkt[2]) + bitwShiftL(as.integer(pkt[3]), 8)
        err_msg <- rawToChar(pkt[4:length(pkt)])
        stop(sprintf("查询错误 [%d]: %s", err_code, err_msg))
      }
      
      # OK 包
      if (msg_type == 0x00) {
        pos <- 2
        affected <- self$read_lenenc_int(pkt[pos:length(pkt)])
        return(data.frame(affected_rows = affected))
      }
      
      # 结果集
      col_count <- as.integer(pkt[1])
      columns <- seq(col_count) |> Reduce(\(acc, i) { col_pkt <- self$read_packet(); acc[[i]] <- self$parse_column(col_pkt); acc }, x=_, init=list())
      
      # EOF 包
      self$read_packet()
      # 读取行数据
      rows <- callCC(\(exit) {
        (\(f, acc = list()) { # f 表示当前函数本身
          row_pkt <- self$read_packet()
          if (length(row_pkt) == 0 || as.integer(row_pkt[1]) == 0xfe) exit(acc)
          f(f, append(acc, list(self$parse_row(row_pkt, columns))))
        }) |> (\(g) g(g)) () # 把 lambda 表达式命名为f，进而模拟递归
      })
      row_count <- length(rows)
      
      # 转换为数据框
      if (row_count == 0) {
        df <- as.data.frame(matrix(ncol = col_count, nrow = 0))
        colnames(df) <- sapply(columns, function(x) x$name)
        return(df)
      }
      
      df <- data.frame(matrix(nrow = row_count, ncol = col_count))
      for (i in 1:col_count) {
        df[, i] <- sapply(rows, function(row) row[[i]])
      }
      colnames(df) <- sapply(columns, function(x) x$name)
      
      df
    },
    
    parse_column = function(pkt) {
      pos <- 1
      
      # 跳过 catalog
      catalog <- self$read_lenenc_str(pkt, pos)
      pos <- attr(catalog, "next_pos")
      
      # 跳过 schema
      schema <- self$read_lenenc_str(pkt, pos)
      pos <- attr(schema, "next_pos")
      
      # 跳过 table
      table <- self$read_lenenc_str(pkt, pos)
      pos <- attr(table, "next_pos")
      
      # 跳过 org_table
      org_table <- self$read_lenenc_str(pkt, pos)
      pos <- attr(org_table, "next_pos")
      
      # 读取 name
      name_info <- self$read_lenenc_str(pkt, pos)
      pos <- attr(name_info, "next_pos")
      name <- ifelse(is.null(name_info$value), "unknown", name_info$value)
      
      # 跳过 org_name
      org_name <- self$read_lenenc_str(pkt, pos)
      pos <- attr(org_name, "next_pos")
      
      list(name = name)
    },

    # 读取长度编码整数
    read_lenenc_int = function(bytes) {
      if (length(bytes) == 0) return(0)
      first <- as.integer(bytes[1])
      
      if (first < 0xfb) first
      else if (first == 0xfc && length(bytes) >= 3) as.integer(bytes[2]) + bitwShiftL(as.integer(bytes[3]), 8)
      else if (first == 0xfd && length(bytes) >= 4) as.integer(bytes[2]) + bitwShiftL(as.integer(bytes[3]), 8) + bitwShiftL(as.integer(bytes[4]), 16)
      else 0
    },
    
    # 读取长度编码字符串
    read_lenenc_str = function(bytes, start_pos) {
      # 执行解析
      result <- read_lenenc(bytes, start_pos)

      # 统一返回格式（保持与原函数兼容的 list + attr 风格）
      out <- list(value = result$value)
      attr(out, "next_pos") <- result$next_pos
      attr(out, "is_null") <- result$is_null %||% FALSE
      attr(out, "error") <- result$error %||% FALSE

      out
    },
    
    parse_row = function(pkt, columns) {
      
      callCC(\(exit) {
        (\(f, pos = 1, row = list(), cols_left = columns) {
          if (length(cols_left) == 0) exit(row)
          if (pos > length(pkt)) exit(c(row, rep(list(NA), length(cols_left))))
          
          res <- read_lenenc(pkt, pos)
          
          if (is.null(res)) {
            f(f, pos + 1, c(row, list(NA)), cols_left[-1])
          } else {
            f(f, res$next_pos, c(row, list(res$val)), cols_left[-1])
          }
        }) |> (\(g) g(g))()
      })
    },
    
    read_packet = function() {
      # 读取包头
      header <- readBin(self$sock, "raw", 4)
      if (length(header) < 4) return(raw(0))
      
      # 解析包长度
      pkt_len <- as.integer(header[1]) + 
        bitwShiftL(as.integer(header[2]), 8) + 
        bitwShiftL(as.integer(header[3]), 16)
      
      # 获取序列号
      seq <- as.integer(header[4])
      
      # 检查序列号
      if (seq != self$seq_id) {
        # 在认证切换时，序列号可能会重置，这里只记录不报错
        if (self$auth_phase > 0) {
          # 认证阶段，接受序列号变化
          self$seq_id <- seq + 1
        } else {
          warning(sprintf("数据包序列号异常: 期望 %d, 收到 %d", self$seq_id, seq))
          self$seq_id <- seq + 1
        }
      } else {
        self$seq_id <- seq + 1
      }
      
      if (pkt_len == 0) return(raw(0))
      
      # 读取包数据
      pkt <- readBin(self$sock, "raw", pkt_len)
      while (length(pkt) < pkt_len) {
        more <- readBin(self$sock, "raw", pkt_len - length(pkt))
        if (length(more) == 0) break
        pkt <- c(pkt, more)
      }
      
      pkt
    },
    
    write_packet = function(data) {
      pkt_len <- length(data)
      
      header <- c(
        as.raw(bitwAnd(pkt_len, 255)),
        as.raw(bitwAnd(bitwShiftR(pkt_len, 8), 255)),
        as.raw(bitwAnd(bitwShiftR(pkt_len, 16), 255)),
        as.raw(self$seq_id)
      )
      
      writeBin(header, self$sock)
      if (pkt_len > 0) {
        writeBin(data, self$sock)
      }
      flush(self$sock)
      
      self$seq_id <- self$seq_id + 1
      self$auth_phase <- self$auth_phase + 1
    },
    
    pack_int32 = pack_int,
    
    close = function() {
      if (!is.null(self$sock)) {
        tryCatch({
          if (isOpen(self$sock)) {
            try({
              self$seq_id <- 0
              self$write_packet(as.raw(0x01))
            }, silent = TRUE)
            close(self$sock)
          }
        }, error = function(e) NULL)
        self$sock <- NULL
        message("连接已关闭")
      }
    }
  )
)

# 用户接口
sqlquerygen.mysql <- function(host, port = 3306, user, password, database) {
  conn <- MySQLConnection$new(host, port, user, password, database)
  conn$connect()
  
  query_fn <- function(sql) {
    tryCatch({
      conn$query(sql)
    }, error = function(e) {
      try(conn$close(), silent = TRUE)
      stop(e)
    })
  }
  
  attr(query_fn, "close") <- function() conn$close()
  query_fn
}

# 使用示例
if (T) {
  library(tibble); library(purrr)
  sqlquery <- sqlquerygen.mysql(host="localhost", port=3371, user="root", password="123456", database="ctp")
  tbls <- sqlquery("SHOW TABLES") |> print()
  tbls |> (\(., x=unlist(head(.))) sprintf(fmt="select * from %s limit 5", x=x) |> setNames(nm=_)) () |> lapply(compose(as_tibble, sqlquery)) |> print()
  attr(sqlquery, "close") ()
}