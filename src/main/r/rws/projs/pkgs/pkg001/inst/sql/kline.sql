-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # 1min.kline
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT
        REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)') Symbol,
        STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  Date,
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') as MinuteTime,
        FIRST_VALUE(LastPrice) OVER w as OpenPrice,
        MAX(LastPrice) OVER w as HighPrice,
        MIN(LastPrice) OVER w as LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) as ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w as MinuteVolume,
        COUNT(*) OVER w as TradeCount
    FROM ##tbl
    WHERE Volume > 0 AND UpdateTime BETWEEN #startime AND #endtime
    WINDOW w AS (PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i'))
)
SELECT DISTINCT *
FROM MinuteKLine
ORDER BY MinuteTime

-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # 1min_maxcnt.kline
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- #maxcnt 最大返回数量
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT
        REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)') Symbol,
        STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  Date,
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') as MinuteTime,
        FIRST_VALUE(LastPrice) OVER w as OpenPrice,
        MAX(LastPrice) OVER w as HighPrice,
        MIN(LastPrice) OVER w as LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) as ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w as MinuteVolume,
        COUNT(*) OVER w as TradeCount
    FROM ##tbl
    WHERE Volume > 0 AND UpdateTime BETWEEN #startime AND #endtime
    WINDOW w AS (PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i'))
)
SELECT DISTINCT *
FROM MinuteKLine
ORDER BY MinuteTime
LIMIT ##maxcnt

-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # OHLCV1M
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT
        REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)') Symbol,
        STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  Date,
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') as MinuteTime,
        FIRST_VALUE(LastPrice) OVER w as OpenPrice,
        MAX(LastPrice) OVER w as HighPrice,
        MIN(LastPrice) OVER w as LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) as ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w as MinuteVolume,
        COUNT(*) OVER w as TradeCount
    FROM ##tbl
    WHERE Volume > 0 AND UpdateTime BETWEEN #startime AND #endtime
    WINDOW w AS (PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i'))
), MinuteKLine2 AS (
    SELECT DISTINCT * FROM MinuteKLine ORDER BY MinuteTime
)
SELECT
    Symbol Symbol,
    Date Date,
    MinuteTime Time,
    OpenPrice Open,
    HighPrice High,
    LowPrice Low,
    ClosePrice Close,
    MinuteVolume Volume
FROM MinuteKLine2

-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # ohlcv1m
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT
        REGEXP_SUBSTR('##tbl', '(?<=t_).*?(?=_\\d{8}$)') Symbol,
        STR_TO_DATE(REGEXP_SUBSTR('##tbl', '\\d{8}$'), '%Y%m%d')  Date,
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') as MinuteTime,
        FIRST_VALUE(LastPrice) OVER w as OpenPrice,
        MAX(LastPrice) OVER w as HighPrice,
        MIN(LastPrice) OVER w as LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) as ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w as MinuteVolume,
        COUNT(*) OVER w as TradeCount
    FROM ##tbl
    WHERE Volume > 0 AND UpdateTime BETWEEN #startime AND #endtime
    WINDOW w AS (PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i'))
), MinuteKLine2 AS (
    SELECT DISTINCT * FROM MinuteKLine ORDER BY MinuteTime
)
SELECT
    Symbol symbol,
    Date date,
    MinuteTime time,
    OpenPrice open,
    HighPrice high,
    LowPrice low,
    ClosePrice close,
    MinuteVolume volume
FROM MinuteKLine2

