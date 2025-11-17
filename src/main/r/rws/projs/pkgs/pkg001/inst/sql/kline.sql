-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # 1min.kline
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT 
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') as MinuteTime,
        FIRST_VALUE(LastPrice) OVER w as OpenPrice,
        MAX(LastPrice) OVER w as HighPrice,
        MIN(LastPrice) OVER w as LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) as ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w as MinuteVolume,
        COUNT(*) OVER w as TradeCount
    FROM ##tbl
    WHERE Volume > 0 AND UpdateTime > #startime AND UpdateTime < #endtime
    WINDOW w AS (PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i'))
)
SELECT DISTINCT *
FROM MinuteKLine
ORDER BY MinuteTime; 

-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # 1min_maxcnt.kline
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT 
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') as MinuteTime,
        FIRST_VALUE(LastPrice) OVER w as OpenPrice,
        MAX(LastPrice) OVER w as HighPrice,
        MIN(LastPrice) OVER w as LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) as ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w as MinuteVolume,
        COUNT(*) OVER w as TradeCount
    FROM ##tbl
    WHERE Volume > 0 AND UpdateTime > #startime AND UpdateTime < #endtime
    WINDOW w AS (PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i'))
)
SELECT DISTINCT *
FROM MinuteKLine
ORDER BY MinuteTime
LIMIT ##maxcnt; 
