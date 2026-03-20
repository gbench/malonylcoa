# MySQL 原生协议 R 实现 - 适用于 MySQL 8.0/9.0+
# 修正：认证切换时的序列号问题

if (!require("digest", quietly = TRUE)) {
  stop("请先安装 digest 包: install.packages('digest')")
}

# ==== 协议工具函数 ====

# 字节转整数（小端）
bytes_to_int <- \(bytes, start = 1, n = 4) {
  if (length(bytes) < start + n - 1) 0 else sum(as.integer(bytes[start:(start + n - 1)]) * 256^(0:(n - 1)))
}

# 打包整数为小端字节
pack_int <- \(x, n = 4) {
  x <- as.integer(x)
  as.raw(bitwAnd(bitwShiftR(rep(x, each = n), 8 * (0:(n - 1))), 255))
}

#' 通用 Length-Encoded Integer/String 解析
#' 
#' 
#' | 首字节值                    | 含义          | 实际长度              |
#' | ----------------------- | ----------- | ----------------- |
#' | `0x00` - `0xfa` (0-250) | 直接表示长度      | 0-250 字节          |
#' | `0xfb` (251)            | **NULL**    | 无后续数据             |
#' | `0xfc` (252)            | 后续 2 字节表示长度 | 251-65535 字节      |
#' | `0xfd` (253)            | 后续 3 字节表示长度 | 65536-16777215 字节 |
#' | `0xfe` (254)            | 后续 8 字节表示长度 | 大字段               |
#' | `0xff` (255)            | 保留/错误       | -                 |
#' 
#' @param bytes 字节数组
#' @param pos 位置索引
#' @param eval_bs value值的字节数组计算函数(bs:字节数组, start:开始索引, end：结束索引inclusive)
read_lenenc <- \(bytes, pos, eval_bs=\(bs, start, end) if (start > end) "" else rawToChar(bs[start:end])) {
  if (pos > length(bytes)) return(NULL)
  
  first <- as.integer(bytes[pos])
  
  # 长度信息表：c(额外字节数, 长度读取字节数)
  info <- switch(as.character(first),
    "251" = list(extra = 0, len = 0, null = TRUE),           # NULL (0xfb)
    "252" = list(extra = 2, len = 2, null = FALSE),          # 2字节长度 (0xfc)
    "253" = list(extra = 3, len = 3, null = FALSE),          # 3字节长度 (0xfd)
    "254" = list(extra = 8, len = 8, null = FALSE),          # 8字节长度 (0xfe)
    "255" = NULL,                                            # 错误
    list(extra = 0, len = first, null = FALSE)               # 0-250 直接编码
  )
  
  if (is.null(info)) list(value = NULL, next_pos = pos + 1, is_null = FALSE, is_error = TRUE)
  else if (info$null) list(value = NA, next_pos = pos + 1, is_null = TRUE, is_error = FALSE)
  else { 
    data_len <- ifelse(info$extra == 0, info$len, bytes_to_int(bytes, pos + 1, info$len)) 
    data_start <- pos + 1 + max(0, info$extra - 1) # max(0, info$extra - 1) 等价于 ifelse(info$extra == 0, 0, info$extra - 1)
    data_end <- data_start + data_len - 1
  
    if (data_end > length(bytes)) list(value = NULL, next_pos = pos + 1 + info$extra, is_null = FALSE, is_error = TRUE)
    else list(value = eval_bs(bytes, data_start, data_end), next_pos = data_end + 1, is_null = FALSE, is_error = FALSE)
  } # if
}

# MySQLConnection 数据库连接对象
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
    
    initialize = \(host, port = 3306, user, password, database) {
      self$host <- host
      self$port <- port
      self$user <- user
      self$password <- password
      self$database <- database
    },
    
    connect = \() {
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
    
    read_handshake = \() {
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
    
    calculate_auth = \(plugin, salt) {
      if (is.null(self$password) || nchar(self$password) == 0) return(raw(0))
      
      if (plugin == "mysql_native_password") {
        stage1 <- digest::digest(self$password, "sha1", serialize = FALSE, raw = TRUE)
        stage2 <- digest::digest(stage1, "sha1", serialize = FALSE, raw = TRUE)
        sha_salt_stage2 <- digest::digest(c(salt[1:20], stage2), "sha1", serialize = FALSE, raw = TRUE)
        auth_response <- raw(20)
        for (i in 1:20) {
          auth_response[i] <- as.raw(bitwXor(as.integer(stage1[i]), as.integer(sha_salt_stage2[i])))
        }
        auth_response
      } else if (plugin == "caching_sha2_password") {
        hash1 <- digest::digest(self$password, "sha256", serialize = FALSE, raw = TRUE)
        hash2 <- digest::digest(hash1, "sha256", serialize = FALSE, raw = TRUE)
        hash3 <- digest::digest(c(hash2, salt[1:20]), "sha256", serialize = FALSE, raw = TRUE)
        auth_response <- raw(32)
        for (i in 1:32) {
          auth_response[i] <- as.raw(bitwXor(as.integer(hash1[i]), as.integer(hash3[i])))
        }
        auth_response
      } else {
        raw(0)
      } # if
    },
    
    send_auth = \() {
      # 客户端能力标志
      CLIENT_LONG_PASSWORD <- 2^0
      CLIENT_PROTOCOL_41 <- 2^9
      CLIENT_SECURE_CONNECTION <- 2^15
      CLIENT_PLUGIN_AUTH <- 2^19
      CLIENT_CONNECT_WITH_DB <- 2^3
      CLIENT_PLUGIN_AUTH_LENENC_DATA <- 2^17
      
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
      ) # pkt
      
      # 认证响应
      if (length(auth_response) > 0) {
        pkt <- c(pkt, as.raw(length(auth_response)), auth_response)
      } else {
        pkt <- c(pkt, as.raw(0))
      } # if
      
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
   
    # 处理认证响应
    handle_auth_response = \() {
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
    
    # 处理认证切换 
    handle_auth_switch = \(pkt) {
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
   
    # 处理 caching_sha2_auth 
    handle_caching_sha2_auth = \(pkt) {
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
          } # if as.integer
        } # if length
      } # if auth_status
      
      self$handle_auth_response()
    },
    
    query = \(sql) {
      # 重置序列号
      self$seq_id <- 0
      
      # 发送查询命令
      self$write_packet(c(as.raw(0x03), charToRaw(sql))) # 通过 COM_QUERY (0x03) 发送 SQL, 结果集返回的数据类型都是字符串
      
      # 读取结果
      self$read_result()
    },
    
    read_result = \() {
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
        (\(f, acc = list()) { # f 表示当前函数本身, acc:累计行集
          row_pkt <- self$read_packet()
          if (length(row_pkt) == 0 || as.integer(row_pkt[1]) == 0xfe) exit(acc)
          row <- list(self$parse_row(row_pkt, columns)) # 把行向量封装成list对象,确保append到acc成为独立行元素,否则append向量会扁平化，造成行无法区分！
          f(f, append(acc, row)) # 递归追加数据行
        }) |> (\(g) g(g)) () # 把 lambda 表达式命名为g，进而模拟递归
      }) # rows
      row_count <- length(rows)
      
      # 转换为数据框
      if (row_count == 0) {
        df <- as.data.frame(matrix(ncol = col_count, nrow = 0))
        colnames(df) <- sapply(columns, \(x) x$name)
        df
      } else {
        df <- seq(col_count) |> lapply(\(j) sapply(rows, getElement, name=j) |> self$convert_column(columns[[j]])) |> as.data.frame()
        colnames(df) <- sapply(columns, \(x) x$name)
        df
      }
    },
    
    # 添加列转换函数 - 修复版本
    convert_column = \(values, col_info) {
      type_code <- col_info$column_type
      type_name <- ifelse(is.null(col_info$type_name), self$get_type_name(type_code), col_info$type_name) 

      # 处理 NULL 值
      values <- lapply(values, \(v) if (is.null(v) || (is.raw(v) && length(v) == 1 && as.integer(v) == 0xfb)) NA else if (is.raw(v)) rawToChar(v) else v)
      
      # 根据类型进行转换 - 使用 if-else 替代 switch 避免 NULL 问题
      result <- if (type_name %in% c("TINY", "SHORT", "LONG", "LONGLONG", "INT24", "YEAR")) {
        as.integer(unlist(values))
      } else if (type_name %in% c("DECIMAL", "NEWDECIMAL")) {
        as.numeric(unlist(values))
      } else if (type_name %in% c("FLOAT", "DOUBLE")) {
        as.numeric(unlist(values))
      } else if (type_name == "BIT") {
        sapply(values, \(v) {
          if (is.na(v)) NA
	        else if (is.raw(v)) sum(as.integer(v) * 256^(rev(seq_along(v)-1)))
          else as.integer(v)
        })
      } else if (type_name %in% c("DATE", "DATETIME", "TIMESTAMP")) {
        lapply(values, \(v) {
          if (is.na(v)) NA
	        else if (grepl("^\\d{4}-\\d{2}-\\d{2}$", v)) as.Date(v)
          else if (grepl("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", v)) as.POSIXct(v, format = "%Y-%m-%d %H:%M:%S")
          else v
        }) |> do.call(c, args=_) # 使用do.call来避免unlist自动抹除时间类型的class属性:"POSIXct","POSIXt"
      } else if (type_name == "TIME") {
        unlist(values)
      } else if (type_name %in% c("TINY_BLOB", "MEDIUM_BLOB", "LONG_BLOB", "BLOB", "GEOMETRY")) {
        if (all(sapply(values, is.raw))) I(values)
        else unlist(values)
      } else if (type_name == "JSON") {
	      if(!require(jsonlite)) stop("请先安装 jsonlite 包: install.packages('jsonlite')")
        sapply(values, \(v) if (is.na(v)) NA else tryCatch(jsonlite::fromJSON(v), error = \(e) v), simplify = FALSE)
      } else if (type_name %in% c("ENUM", "SET")) {
        factor(unlist(values))
      } else { # 默认作为字符类型
        unlist(values)
      } # result
      
      if (length(result) != length(values)) values else result # 确保结果长度一致
    },
    
    # 列信息分析
    parse_column = \(pkt) {
      pos <- 1
      
      # 读取 catalog (总是 "def")
      catalog <- self$read_lenenc_str(pkt, pos)
      pos <- attr(catalog, "next_pos")
      
      # 读取 schema (数据库名)
      schema <- self$read_lenenc_str(pkt, pos)
      pos <- attr(schema, "next_pos")
      
      # 读取 table (表别名)
      table <- self$read_lenenc_str(pkt, pos)
      pos <- attr(table, "next_pos")
      
      # 读取 org_table (原始表名)
      org_table <- self$read_lenenc_str(pkt, pos)
      pos <- attr(org_table, "next_pos")
      
      # 读取 name (列别名)
      name_info <- self$read_lenenc_str(pkt, pos)
      pos <- attr(name_info, "next_pos")
      name <- ifelse(is.null(name_info), "unknown", name_info)
      
      # 读取 org_name (原始列名)
      org_name <- self$read_lenenc_str(pkt, pos)
      pos <- attr(org_name, "next_pos")
      
      # 【关键修复】直接读取 1 字节作为 fixed_len (总是 0x0c)
      fixed_len <- bytes_to_int(pkt, pos, 1)
      pos <- pos + 1
      
      # 读取字符集编号 (2字节)
      charset <- as.integer(pkt[pos]) + bitwShiftL(as.integer(pkt[pos + 1]), 8)
      pos <- pos + 2
      
      # 读取列长度 (4字节)
      column_length <- bytes_to_int(pkt, pos, 4)
      pos <- pos + 4
      
      # 读取列类型 (1字节) - 现在位置正确了
      column_type <- as.integer(pkt[pos])
      pos <- pos + 1
      
      # 读取 flags (2字节)
      flags <- as.integer(pkt[pos]) + bitwShiftL(as.integer(pkt[pos + 1]), 8)
      pos <- pos + 2
      
      # 读取 decimals (1字节)
      decimals <- as.integer(pkt[pos])
      pos <- pos + 1
      
      # 跳过 filler (2字节)
      pos <- pos + 2
      
      list(
        name = name,
        org_name = org_name,
        table = table,
        org_table = org_table,
        schema = schema,
        charset = charset,
        column_length = column_length,
        column_type = column_type,
        flags = flags,
        decimals = decimals,
        type_name = self$get_type_name(column_type)
      )
    },

    # 添加类型名称映射函数
    get_type_name = \(type_code) {
      type_map <- c(
        "0" = "DECIMAL",
        "1" = "TINY",
        "2" = "SHORT",
        "3" = "LONG",
        "4" = "FLOAT",
        "5" = "DOUBLE",
        "6" = "NULL",
        "7" = "TIMESTAMP",
        "8" = "LONGLONG",
        "9" = "INT24",
        "10" = "DATE",
        "11" = "TIME",
        "12" = "DATETIME",
        "13" = "YEAR",
        "14" = "NEWDATE",
        "15" = "VARCHAR",
        "16" = "BIT",
        "17" = "TIMESTAMP2",
        "18" = "DATETIME2",
        "19" = "TIME2",
        "20" = "TYPED_ARRAY",  # MySQL 8.0.17+
        "245" = "JSON",
        "246" = "NEWDECIMAL",
        "247" = "ENUM",
        "248" = "SET",
        "249" = "TINY_BLOB",
        "250" = "MEDIUM_BLOB",
        "251" = "LONG_BLOB",
        "252" = "BLOB",
        "253" = "VAR_STRING",
        "254" = "STRING",
        "255" = "GEOMETRY"
      )
      
      if (as.character(type_code) %in% names(type_map)) type_map[as.character(type_code)]
      else sprintf("UNKNOWN(%d)", type_code)
    },
    
    # 读取长度编码字符串
    read_lenenc_str = \(bytes, start_pos) {
      read_lenenc(bytes, start_pos) |> with({
        attr(value, "next_pos") <- next_pos
        attr(value, "is_null") <- is_null %||% FALSE
        attr(value, "is_error") <- is_error %||% FALSE
        value
      }) # with
    },
    
    # 行数据, 返回一个字符向量
    parse_row = \(bytes, columns) {
      callCC(\(exit) {
        (\(f, pos = 1, row = character(0), cols_left = columns) { # f 表示当前函数本身,row = character(0), 行初始为空字符串向量
          if (length(cols_left) == 0) exit(row)
          if (pos > length(bytes)) exit(c(row, rep(NA, length(cols_left)))) # pos超出数据范围，把剩余字段填写为NA
          read_lenenc(bytes, pos) |> with(f(f, next_pos, c(row, value), cols_left[-1])) # 递归读取剩余列
        }) |> (\(g) g(g)) () # 把 lambda 表达式命名为g，进而模拟递归
      }) # callCC
    },
   
    # 读取报数据 
    read_packet = \() {
      header <- readBin(self$sock, "raw", 4) # 读取包头
      if (length(header) < 4) return(raw(0))
      
      pkt_len <- bytes_to_int(header, n=3) # 解析包长度
      seq <- as.integer(header[4]) # 获取序列号

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
      } # while
      
      pkt
    },
   
    # 发送包数据 
    write_packet = \(data) {
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
   
    # 打包一个整数默认4字节 
    pack_int32 = pack_int,
    
    # 连接关闭 
    close = \() {
      if (!is.null(self$sock)) {
        tryCatch({
          if (isOpen(self$sock)) {
            try({
              self$seq_id <- 0
              self$write_packet(as.raw(0x01))
            }, silent = TRUE)
            close(self$sock)
          }
        }, error = \(e) NULL)
        self$sock <- NULL
        message("连接已关闭")
      }
    }
  )
)

# 用户接口
sqlquerygen.mysql <- \(host, port = 3306, user, password, database) {
  conn <- MySQLConnection$new(host, port, user, password, database)
  conn$connect()
  query_fn <- \(sql) tryCatch(conn$query(sql), error = print)
  attr(query_fn, "close") <- \() conn$close()
  query_fn
}

# 使用示例
if (F) {
  library(tibble); library(purrr)
  sqlquery <- sqlquerygen.mysql(host="localhost", port=3371, user="root", password="123456", database="ctp")
  tbls <- sqlquery("SHOW TABLES") |> print()
  tbls |> (\(., x=unlist(head(.))) sprintf(fmt="select * from %s limit 5", x=x) |> setNames(nm=_)) () |> lapply(compose(as_tibble, sqlquery)) |> print()
  attr(sqlquery, "close") ()
}
