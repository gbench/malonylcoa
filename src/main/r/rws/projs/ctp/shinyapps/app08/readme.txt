# AI 项目，通过 materials中的数据内容，让AI学习，然后 集合数据库编码

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

# 你学会了作者的极致简洁的编码风格了吗？
# hitler 数据库的表结构学会了吗？
# 基于hitler编码程序，为各个会计主体(公司)生成财务报表(资产负债，利润，现金流量)
