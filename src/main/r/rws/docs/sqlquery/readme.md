# SQLQuery R Package 设计文档

## 1. 架构概述

### 1.1 设计理念
SQLQuery 是一个基于 R 语言的**四层架构数据访问平台**，采用**函数式编程**和**环境继承**模式实现数据库操作的统一接口。其核心设计理念是：

- **接口统一化**：为不同数据源提供一致的函数接口
- **实现插件化**：通过后缀命名约定支持多数据源扩展
- **拦截器模式**：支持方法拦截和AOP编程
- **环境驱动**：通过全局配置动态切换数据源行为

### 1.2 四层架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                   第四层：统一接口层                         │
│    sqlquery(), sqlexecute(), ctsql(), insql(), upsql()      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   第三层：平台实现层                         │
│    sqlquery.mysql(), sqlquery.postgresql(), sqlquery.xls()  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   第二层：连接管理器层                       │
│      dbfun.mysql(), dbfun.postgresql(), dbfun.xls()         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   第一层：通用连接管理器                     │
│                        dbfun()                              │
└─────────────────────────────────────────────────────────────┘
```

## 2. 核心组件设计

### 2.1 第一层：通用连接管理器 (`dbfun.R`)

**职责**：提供统一的数据库连接生命周期管理

```r
dbfun <- function(f, ..., .interceptors = list()) {
  # 配置解析
  dbcfg <- match.call(expand.dots=F)$... |> 
    lapply(\(x) tryCatch(eval(x), error=\(e) deparse(x)))
  
  function(sql, ...) {
    # 前置拦截器执行
    if (!is.null(.interceptors$before)) {
      sql <- .interceptors$before(sql, ...)
    }
    
    tryCatch({
      # 连接建立与执行
      con <- establish_connection(dbcfg)
      on.exit(dbDisconnect(con), add=TRUE)
      result <- f(con, ...)
      
      # 后置拦截器执行  
      if (!is.null(.interceptors$after)) {
        result <- .interceptors$after(result, sql, ...)
      }
      result
    }, error=\(err) {
      # 错误拦截器执行
      if (!is.null(.interceptors$error)) .interceptors$error(err, sql, ...)
      else stop(err)
    })
  }
}
```

**关键特性**：
- 统一的连接生命周期管理
- 拦截器链支持（前置、后置、错误）
- 配置参数的动态解析
- 异常统一处理

### 2.2 第二层：具体连接管理器 (`dbfun.xxx.R`)

**设计模式**：函数式继承模式

```r
dbfun.postgresql <- \(f, ...) {
  with(list(...), dbfun(\ (con, ...) {
    # PostgreSQL 特定配置：search_path 设置
    .log <- \ (x) { if(verbose) cat(" -- ", x, "\n"); x }
    if(!is.na(search_path)) search_path else getOption("sqlquery.schema", "public,economics") |> 
      strsplit("[,[:blank:]]+") |> unlist() |> sprintf(fmt="'%s'") |> paste0(collapse=", ") |> 
      sprintf(fmt="SET search_path to %s") |> .log() |> dbExecute(con, statement=_)
    
    # 函数继承：注入 .log 到子函数
    f(con, .log=.log)
  }, ...))
}
```

**关键特性**：
- 通过 `with(list(...), ...)` 实现参数继承
- 数据库特定配置初始化
- 工具函数注入（如 `.log`）
- 透明的父函数调用

### 2.3 第三层：平台实现层 (`sqlquery.xxx.R`)

**设计模式**：策略模式

```r
sqlquery.mysql <- \(sql, simplify=T, n=-1, ...) {
  dbfun.mysql(\ (con, ...) {
    dataset <- c(list(), sql) |> 
      lapply(compose(tibble, partial(dbGetQuery, con=con, n=n))) |> 
      structure(names=sql)
    if(simplify & length(dataset)==1) dataset[[1]] else dataset
  }, ...)(sql)
}

ctsql.mysql <- \(dfm, tbl, ...) {
  .tbl <- if(missing(tbl)) deparse(substitute(dfm)) else deparse(substitute(tbl)) |> 
    gsub(pattern="^['\"]|['\"]$", replacement="")
  
  # 类型推断与SQL生成
  dfm |> lapply(\(e, t=typeof(e), cls=class(e),
    n=as.integer(Reduce(\(acc, a) max(acc, max(acc, nchar(a))), x=as.character(e), init=0)*1.5),
    default_type=sprintf('varchar(%s)', n)
  ) switch(t,
    `logical`='bool',
    `integer`=if(cls=='factor') default_type else 'integer',
    `double`=if(any(grepl(pattern="Date|POSIXct|POSIXt", x=cls))) "datetime" else 'double',
    `list`='json',
    default_type
  )) |> (\(x) 
    sprintf("create table %s (\n  %s \n)\n", .tbl, paste(names(x), x, collapse=",\n  "))
  )()
}
```

**关键特性**：
- 数据库特定的SQL方言实现
- 统一的函数签名
- 智能类型映射系统
- 符号名自动捕获（`deparse(substitute(...))`）

### 2.4 第四层：统一接口层 (`sqlquery.R`)

**设计模式**：门面模式

```r
sqlquery <- \(..., driver=getOption("sqlquery.driver", "mysql")) {
  impl_func <- get0(paste0("sqlquery.", driver), mode="function")
  if (is.null(impl_func)) stop("未找到驱动实现: ", driver)
  impl_func(...)
}

sqlexecute <- \(..., driver=getOption("sqlquery.driver", "mysql")) {
  impl_func <- get0(paste0("sqlexecute.", driver), mode="function")
  if (is.null(impl_func)) stop("未找到驱动实现: ", driver)
  impl_func(...)
}

set_sql_driver <- \(driver) {
  options(sqlquery.driver=driver)
  message("数据库驱动已设置为: ", driver)
}
```

**关键特性**：
- 统一的函数接口
- 运行时驱动切换
- 错误驱动的实现发现
- 透明的实现委派

## 3. 扩展机制

### 3.1 数据源扩展协议

要添加新的数据源，需要实现以下协议：

```r
# 1. 连接管理器 (dbfun.newdb.R)
dbfun.newdb <- \(f, ...) {
  with(list(...), dbfun(\ (con, ...) {
    # 新数据库特定配置
    f(con, ...)
  }, ...))
}

# 2. 平台实现 (sqlquery.newdb.R)
sqlquery.newdb <- \(sql, ...) {
  dbfun.newdb(\ (con, ...) {
    # 新数据库查询实现
  }, ...)(sql)
}

sqlexecute.newdb <- \(sql, ...) {
  dbfun.newdb(\ (con, ...) {
    # 新数据库执行实现
  }, ...)(sql)
}

ctsql.newdb <- \(dfm, tbl, ...) {
  # 新数据库DDL生成实现
}
```

### 3.2 拦截器扩展

```r
# 创建拦截器链
create_interceptor_chain <- function(interceptors = list()) {
  list(
    before = \(sql, ...) {
      for(interceptor in interceptors$before) sql <- interceptor(sql, ...)
      sql
    },
    after = \(result, sql, ...) {
      for(interceptor in interceptors$after) result <- interceptor(result, sql, ...)
      result
    }
  )
}

# 预置拦截器
query_rewrite_interceptor <- \(sql, ...) {
  # SQL重写逻辑
  sql
}

cache_interceptor <- \(result, sql, ...) {
  # 缓存逻辑
  result
}
```

## 4. 高级特性

### 4.1 分布式计算支持

```r
sqlquery.distributed <- \(sql, clusters=getOption("sqlquery.clusters"), ...) {
  # 并行执行到多个集群
  results <- parallel::mclapply(clusters, function(cluster) {
    do.call(sqlquery, c(list(sql), cluster))
  })
  
  # 智能结果合并
  federated_merge(results)
}
```

### 4.2 SQL分析与优化

```r
optimization_interceptor <- \(sql, ...) {
  sql |> 
    optimize_join_conditions() |>
    optimize_subqueries() |>
    add_query_hints()
}
```

### 4.3 联邦查询

```r
sqlquery.federated <- \(sql, data_sources=list(...), ...) {
  results <- list()
  for (name in names(data_sources)) {
    set_sql_driver(data_sources[[name]]$driver)
    results[[name]] <- do.call(sqlquery, c(list(sql), data_sources[[name]]))
  }
  federated_merge(results)
}
```

## 5. 配置系统

### 5.1 全局配置选项

```r
# 数据库驱动配置
options(
  sqlquery.driver = "mysql",           # 默认驱动
  sqlquery.drv = RMySQL::MySQL(),      # 默认数据库驱动
  sqlquery.host = "localhost",         # 默认主机
  sqlquery.user = "root",              # 默认用户
  sqlquery.password = "",              # 默认密码
  sqlquery.dbname = "test",            # 默认数据库
  sqlquery.schema = "public",          # PostgreSQL模式
  sqlquery.clusters = list(            # 分布式集群配置
    list(host="db1", dbname="shard1"),
    list(host="db2", dbname="shard2")
  )
)
```

### 5.2 环境特定配置

```r
# 开发环境
setup_development <- function() {
  options(
    sqlquery.driver = "mysql",
    sqlquery.host = "localhost",
    sqlquery.dbname = "dev_db"
  )
}

# 生产环境  
setup_production <- function() {
  options(
    sqlquery.driver = "postgresql", 
    sqlquery.host = "prod-db.example.com",
    sqlquery.dbname = "prod_db"
  )
}
```

## 6. 使用示例

### 6.1 基础使用

```r
# 设置MySQL驱动
set_sql_driver("mysql")

# 执行查询
users <- sqlquery("SELECT * FROM users WHERE active=1", 
                  host="localhost", dbname="myapp")

# 执行更新
result <- sqlexecute("UPDATE users SET last_login=NOW() WHERE id=1")

# 生成建表语句
sql <- ctsql(iris, "iris_table")
```

### 6.2 高级使用

```r
# 分布式查询
set_sql_driver("distributed")
result <- sqlquery("SELECT COUNT(*) FROM events")

# 联邦查询  
set_sql_driver("federated")
result <- sqlquery("SELECT * FROM sales_data")

# 带拦截器的自定义查询
custom_dbfun <- dbfun_with_interceptors(list(
  before = \(sql, ...) { cat("Executing:", sql, "\n"); sql },
  after = \(result, sql, ...) { cat("Rows:", nrow(result), "\n"); result }
))

custom_sqlquery <- \(sql, ...) {
  custom_dbfun(\(con, ...) dbGetQuery(con, sql) |> tibble(), ...)(sql)
}
```

## 7. 文件结构规范

```
sqlquery/
├── core/                    # 核心层
│   ├── dbfun.R             # 通用连接管理器
│   └── sqlquery.R          # 统一接口层
├── drivers/                # 驱动层
│   ├── dbfun.mysql.R       # MySQL连接管理器
│   ├── dbfun.postgresql.R  # PostgreSQL连接管理器
│   ├── sqlquery.mysql.R    # MySQL平台实现
│   └── sqlquery.postgresql.R # PostgreSQL平台实现
├── interceptors/           # 拦截器层
│   ├── distributed.R       # 分布式拦截器
│   ├── cache.R            # 缓存拦截器
│   └── optimizer.R        # 优化拦截器
├── extensions/            # 扩展层
│   ├── sqlquery.distributed.R # 分布式查询
│   ├── sqlquery.federated.R   # 联邦查询
│   └── sqlquery.parallel.R    # 并行查询
└── utils/
    ├── config.R           # 配置工具
    └── types.R            # 类型映射工具
```

## 8. 设计优势总结

1. **架构清晰**：四层分离，职责单一
2. **扩展性强**：插件式架构，易于添加新数据源
3. **拦截灵活**：支持方法拦截和AOP编程
4. **配置驱动**：环境敏感的配置管理
5. **统一接口**：多数据源统一操作体验
6. **分布式就绪**：内置分布式计算支持
7. **类型安全**：智能类型推断和映射
8. **生产就绪**：完善的错误处理和事务管理

这种设计使得 SQLQuery 不仅是一个数据库操作工具，更是一个完整的数据访问平台，能够适应从简单查询到复杂分布式计算的各种场景。