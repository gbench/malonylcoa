-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据(带有tickdata源数据)
-- # 1min.kline.weighted
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH TickData AS (
    -- 第一步：计算单次成交量Vol（当前累计Volume - 上一笔累计Volume）
    SELECT 
        UpdateTime,
        LastPrice,
        Volume,  -- 累计成交量
        -- 计算单次成交量：当前Volume减去上一笔Volume（首笔交易Vol为自身Volume）
        Volume - COALESCE(LAG(Volume) OVER (ORDER BY UpdateTime), 0) AS Vol
    FROM ##tbl
    WHERE 
        Volume > 0
        AND UpdateTime > #startime
        AND UpdateTime < #endtime
),
MinuteKLine AS (
    -- 第二步：按分钟聚合K线数据（使用计算出的Vol作为权重）
    SELECT 
        DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i') AS MinuteTime,
        FIRST_VALUE(LastPrice) OVER w AS OpenPrice,
        MAX(LastPrice) OVER w AS HighPrice,
        MIN(LastPrice) OVER w AS LowPrice,
        FIRST_VALUE(LastPrice) OVER (w ORDER BY UpdateTime DESC) AS ClosePrice,
        -- 按单次成交量Vol加权的平均价
        SUM(LastPrice * Vol) OVER w / SUM(Vol) OVER w AS WeightedAvgPrice,
        -- 分钟总成交量（累加单次Vol）
        SUM(Vol) OVER w AS MinuteVolume,
        -- 分钟内交易次数
        COUNT(*) OVER w AS TradeCount
    FROM TickData
    -- 过滤有效单次成交量（避免负数，通常不会出现，但做防御性处理）
    WHERE Vol > 0
    WINDOW w AS (
        PARTITION BY DATE_FORMAT(STR_TO_DATE(UpdateTime, '%H:%i:%s'), '%H:%i')
    )
)
-- 按分钟去重，保留唯一记录
SELECT DISTINCT *
FROM MinuteKLine
ORDER BY MinuteTime;

-- ---------------------------------------------------------------
-- 根据指定表名生成K线数据(带有tickdata源数据)
-- # tickdata.1min.kline
-- ##tbl 表名
-- #startime 开始时间
-- #endtime 结束时间
-- ---------------------------------------------------------------
WITH MinuteKLine AS (
    SELECT 
        Id,
        LastPrice,
        UpdateTime,
        Volume,
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
SELECT 
    Id,
    LastPrice,
    UpdateTime,
    Volume,
    MinuteTime,
    OpenPrice,
    HighPrice,
    LowPrice,
    ClosePrice,
    MinuteVolume,
    TradeCount
FROM MinuteKLine
ORDER BY UpdateTime, Id;



