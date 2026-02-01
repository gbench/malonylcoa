-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据
-- # OHLCV1M.PG
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT
        SUBSTRING('##tbl' FROM 't_(.*?)_\d{8}$') AS Symbol,
        TO_DATE(SUBSTRING('##tbl' FROM '\d{8}$'), 'YYYYMMDD') AS Date,
        TO_CHAR(TO_TIMESTAMP(UpdateTime, 'HH24:MI:SS'), 'HH24:MI') AS MinuteTime,
        FIRST_VALUE(LastPrice) OVER w AS OpenPrice,
        MAX(LastPrice) OVER w AS HighPrice,
        MIN(LastPrice) OVER w AS LowPrice,
        FIRST_VALUE(LastPrice) OVER (PARTITION BY TO_CHAR(TO_TIMESTAMP(UpdateTime, 'HH24:MI:SS'), 'HH24:MI')
                                     ORDER BY UpdateTime DESC) AS ClosePrice,
        MAX(Volume) OVER w - MIN(Volume) OVER w AS MinuteVolume,
        COUNT(*) OVER w AS TradeCount
    FROM   ##tbl
    WHERE  Volume > 0
      AND  UpdateTime BETWEEN #startime AND #endtime
    WINDOW w AS (PARTITION BY TO_CHAR(TO_TIMESTAMP(UpdateTime, 'HH24:MI:SS'), 'HH24:MI'))
),
MinuteKLine2 AS (
    SELECT DISTINCT *
    FROM   MinuteKLine
    ORDER  BY MinuteTime
)
SELECT Symbol,
       Date,
       MinuteTime AS Time,
       OpenPrice  AS Open,
       HighPrice  AS High,
       LowPrice   AS Low,
       ClosePrice AS Close,
       MinuteVolume AS Volume
FROM   MinuteKLine2;
