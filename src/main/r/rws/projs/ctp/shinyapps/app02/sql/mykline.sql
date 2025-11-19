-- -----------------------------------------------------------------------------------------
-- # 1min.keline.weighted
-- -----------------------------------------------------------------------------------------
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