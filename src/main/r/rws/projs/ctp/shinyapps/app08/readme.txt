# 请学习以下文件：API接口用法 以及 作者编码风格
# attach.R  bkp.readme.R  bkp.readme.txt  sqlquery.readme.txt

# 数据加载
batch_load() # 导入库函数sqlquery
sqlquery.hitler <- partial(sqlquery, host="192.168.1.6", port=3309, dbname="hitler")
tbldef.sql<- \(tbl, dbname="hitler") gettextf("
  SELECT -- 查看列定义详情
      COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE,
      COLUMN_DEFAULT, COLUMN_COMMENT
  FROM information_schema.COLUMNS 
  WHERE TABLE_SCHEMA = '%s' 
    AND TABLE_NAME = '%s'
  ORDER BY ORDINAL_POSITION",dbname, tbl)
topn.sql <- \(tbl, n=5,dbname="hitler") gettextf("select * from %s.%s limit %s", dbname, tbl, n)
hitler.tables <- sqlquery.hitler("show tables") |> setNames(nm=_) # hitler数据库中的所有数据表

# 表定义
hitler.tables |> lapply(\(x) sqlquery.hitler(tbldef.sql(x)))

# 数据示例
hitler.tables |> lapply(\(x) sqlquery.hitler(topn.sql(x)))

# 编写程序编制多会计主体的财务报表