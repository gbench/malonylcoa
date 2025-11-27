-- ---------------------------------------------------------------
-- 根据指定表名提取交易原数据
-- # tickdata
-- ##tbl 表名
-- ---------------------------------------------------------------
SELECT
    Id id, -- 主键索引
    -- 计算毫秒级时间戳：秒级时间戳*1000 + 毫秒数
    UNIX_TIMESTAMP(STR_TO_DATE(CONCAT(TradingDay, " ", UpdateTime), '%Y%m%d %H:%i:%s')) * 1000 + UpdateMillisec AS x, -- 毫秒级时间戳
    LastPrice y, -- 价格
    COALESCE(Volume-LAG(VOLUME) OVER(), 0)  vol, -- 成交量
    STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  date, -- 日期
    UpdateTime time, -- 更新日期
    REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)') symbol -- 合约代码
FROM ##tbl

-- ---------------------------------------------------------------
-- 根据指定表名提取交易原数据
-- # tickdata2
-- ##tbl 表名
-- ##startime  开始时间
-- ##endtime 结束时间
-- ---------------------------------------------------------------
SELECT
    Id id, -- 主键索引
    -- 计算毫秒级时间戳：秒级时间戳*1000 + 毫秒数
    UNIX_TIMESTAMP(STR_TO_DATE(CONCAT(TradingDay, " ", UpdateTime), '%Y%m%d %H:%i:%s')) * 1000 + UpdateMillisec AS x, -- 毫秒级时间戳
    LastPrice y, -- 价格
    COALESCE(Volume-LAG(VOLUME) OVER(), 0)  vol, -- 成交量
    STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  date, -- 日期
    UpdateTime time, -- 更新日期
    REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)') symbol -- 合约代码
FROM ##tbl WHERE UpdateTime BETWEEN #startime AND #endtime

