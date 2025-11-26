-- ---------------------------------------------------------------
-- 根据指定表名提取交易源数据
-- # tickdata
-- ##tbl 表名
-- ---------------------------------------------------------------
SELECT
    Id id, -- 主键索引
    CAST(unix_timestamp(time(UpdateTime)) as UNSIGNED) x, -- 时间索引
    LastPrice y, -- 价格
    COALESCE(Volume-LAG(VOLUME) OVER(), 0)  vol, -- 成交量
    STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  date, -- 日期
    UpdateTime time, -- 更新日期
    REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)' ) symbol -- 合约代码
FROM ##tbl

