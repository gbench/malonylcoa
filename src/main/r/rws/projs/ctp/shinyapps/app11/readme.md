markdown
# CTP实时K线图系统架构设计文档

## 1. 系统概述

本系统是一个基于R Shiny的实时期货行情监控平台，通过异步TCP连接接收CTP行情数据，实时聚合K线并提供可视化图表展示。

### 1.1 核心功能

- 实时行情数据接收与存储
- 多周期K线聚合（1-1440分钟）
- 实时价格统计（均值、极值、标准差）
- 盘口行情展示（五档买卖盘）
- 多合约切换管理
- 技术指标展示（MA、VOL、KDJ、MACD、OINT）

---

## 2. 系统架构图

```mermaid
flowchart TB
    subgraph A["☁️ 外部数据源"]
        CTP["🏦 CTP期货服务器<br/>192.168.1.41:9898"]
    end

    subgraph B["📦 R Shiny应用层"]
        direction TB
        
        subgraph B1["📡 通信层"]
            CTPClient["🔄 CTPD异步客户端<br/>ctpd_async2"]
            SocketServer["🔌 Socket控制台<br/>端口随机分配"]
        end
        
        subgraph B2["⚙️ 业务逻辑层"]
            StateManager["📊 状态管理器<br/>InstrumentStateManager"]
            KlineAgg["📈 K线聚合引擎<br/>aggregate_kline"]
            StatsCalc["📐 统计计算引擎<br/>update_stats_batch"]
            PricePoll["🔄 价格轮询<br/>reactivePoll"]
        end
        
        subgraph B3["💾 数据存储层"]
            TickCache["📝 Tick数据<br/>原子向量存储"]
            KlineCache["🗂️ K线缓存<br/>data.table"]
            AttrStore["🏷️ 属性存储<br/>new.env hash"]
        end
        
        subgraph B4["🎨 表现层"]
            UI["🖥️ Shiny UI<br/>侧边栏+主图区"]
            ChartJS["📊 klinecharts.js<br/>图表渲染"]
            Console["💻 CTP R控制台<br/>命令行管理"]
        end
    end

    subgraph C["🌐 客户端"]
        Browser["🧑‍💻 Web浏览器<br/>localhost:随机端口"]
    end

    CTP -->|🔌 TCP Socket| CTPClient
    CTPClient -->|📨 行情推送| TickCache
    TickCache -->|📤 增量提取| KlineAgg
    KlineAgg -->|📊 K线数据| KlineCache
    KlineCache -->|📡 WebSocket消息| ChartJS
    PricePoll -->|📈 统计更新| UI
    UI -->|🖱️ 用户操作| StateManager
    
    SocketServer -->|🔧 运维管理| Console
    Console -->|👁️ 查看/操作| StateManager
    Console -->|🔍 查询| TickCache
    
    ChartJS -->|🎨 渲染| Browser
    UI -->|📄 HTML/CSS/JS| Browser
3. 核心模块设计
3.1 CTPD异步客户端 (ctpd_async2)
职责：管理与CTP服务器的TCP连接，异步接收行情数据

数据结构：

r
# 合约存储结构（每个合约独立环境）
instruments[[instrument_id]] = {
    # 原子向量（预分配容量20000，线性扩容）
    LastPrice: numeric[capacity]
    Volume: integer[capacity]
    OpenInterest: integer[capacity]
    DateTime: numeric[capacity]
    
    # 五档盘口
    AskPrice1-5: numeric[capacity]
    AskVolume1-5: integer[capacity]
    BidPrice1-5: numeric[capacity]
    BidVolume1-5: integer[capacity]
    
    # 方法
    add(tick)        # 追加新tick
    size()           # 当前数据量
    last_tick()      # 获取最新tick
    ticks_dt(idx, period_seconds)
}
工作流程：

3.2 状态管理器 (InstrumentStateManager)
r
InstrumentStateManager <- R6::R6Class(
  "InstrumentStateManager",
  public = list(
    kline_cache = list(),
    tick_counts = list(),
    attrs = new.env(hash = TRUE),
    current_instrument_id = NULL,
    current_period = 1,
    
    get_kline_dt = function(instrument_id),
    set_kline_dt = function(instrument_id, dt),
    clear_instrument = function(instrument_id),
    clear_all = function(),
    set_current = function(instrument_id, period = NULL)
  )
)
3.3 K线聚合引擎
聚合算法：

r
aggregate_kline <- function(ticks_df) {
    dt <- as.data.table(ticks_df)
    
    kline <- dt[, .(
        timestamp = as.numeric(first(Period)) * 1000,
        open = first(LastPrice),
        high = max(LastPrice),
        low = min(LastPrice),
        close = last(LastPrice),
        volume = last(Volume) - first(Volume),
        oint = last(OpenInterest)
    ), by = Period]
    
    kline[volume < 0, volume := 0]
    setorder(kline, timestamp)
    kline[, .(timestamp, open, high, low, close, volume, oint)]
}
增量合并策略：

r
merge_kline_dt <- function(base_dt, new_dt) {
    base_dt[new_dt, on = "timestamp", `:=`(
        high = pmax(high, i.high),
        low = pmin(low, i.low),
        close = i.close,
        volume = volume + i.volume,
        oint = i.oint
    )]
    
    new_rows <- new_dt[!base_dt, on = "timestamp"]
    if (nrow(new_rows) > 0) {
        base_dt <- rbind(base_dt, new_rows)
        setorder(base_dt, timestamp)
    }
    
    return(base_dt)
}
3.4 统计计算引擎（Welford在线算法）
r
update_stats_batch <- function(prev_stats, new_prices) {
    prev_n <- prev_stats$n
    prev_mean <- prev_stats$mean
    prev_M2 <- prev_stats$M2
    
    k <- length(new_prices)
    new_n <- prev_n + k
    
    sum_new <- sum(new_prices)
    new_mean <- (prev_n * prev_mean + sum_new) / new_n
    
    sum_sq_new <- sum((new_prices - new_mean)^2)
    new_M2 <- prev_M2 + sum_sq_new + prev_n * (prev_mean - new_mean)^2
    
    new_min <- min(prev_min, min(new_prices))
    new_max <- max(prev_max, max(new_prices))
    
    return(list(
        n = new_n,
        mean = new_mean,
        M2 = new_M2,
        sd = if (new_n > 1) sqrt(new_M2 / (new_n - 1)) else 0,
        min = new_min,
        max = new_max,
        last = new_prices[k]
    ))
}
优势：O(1)时间复杂度，无需存储历史价格，数值稳定性高。

4. 数据流设计
4.1 实时数据流
4.2 K线更新策略















5. 前端架构
5.1 图表库集成
javascript
// chartapp.js 核心架构
(function() {
    let chart = null;
    let currentInstrument = null;
    
    function registerIndicators() {
        klinecharts.registerIndicator({
            name: "OINT",
            shortName: "OINT",
            figures: [
                { key: "oint", title: "持仓量: ", type: "line" }
            ],
            calc: function(kLineDataList) {
                return kLineDataList.map(function(k) {
                    return { oint: k.oint || 0 };
                });
            }
        });
    }
    
    function initChart() {
        chart = klinecharts.init("chart");
        chart.setStyles({
            grid: { 
                horizontal: { color: "#2d2d3f", size: 1 }, 
                vertical: { color: "#2d2d3f", size: 1 } 
            },
            candle: { 
                candle: { 
                    upColor: "#ef5350", 
                    downColor: "#26a69a" 
                } 
            }
        });
        
        chart.createIndicator("VOL", false);
        chart.createIndicator("MA", true, { id: "candle_pane" });
        chart.createIndicator("OINT", false);
        chart.createIndicator("KDJ", false);
        chart.createIndicator("MACD", false);
    }
    
    function updateKline(data) {
        if (data.type === "full") {
            chart.applyNewData(data.ds);
        } else {
            data.ds.forEach(k => chart.updateData(k));
        }
    }
})();
5.2 Shiny消息处理器
javascript
Shiny.addCustomMessageHandler("updateKline", function(data) {
    if (data.instrument === currentInstrument) updateKline(data);
});

Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
    switchInstrument(msg.instrument);
});

Shiny.addCustomMessageHandler("clearChart", function(msg) {
    if (!msg.instrument || msg.instrument === currentInstrument) clearChart();
});

Shiny.addCustomMessageHandler("resetAll", resetAll);
5.3 实时行情面板
text
┌─────────────────────────────────────────────────┐
│ 状态栏 ● 已连接 | MA605                          │
├─────────────────────────────────────────────────┤
│ 实时行情                                         │
│ MA605                                            │
│ 最新: 3852.00  均: 3848.50                      │
│ 最高: 3855.00  低: 3840.00                      │
│ Ticks: 12450  时间: 14:35:22.123                │
├─────────────────────────────────────────────────┤
│ 卖盘 Ask                    买盘 Bid            │
│ 3853.00  100               3852.00  50         │
│ 3854.00  200               3851.00  80         │
│ 3855.00  150               3850.00 120         │
└─────────────────────────────────────────────────┘
6. 运维管理控制台
6.1 Socket控制台架构














6.2 常用运维命令
r
# 查看所有应用实例
apps |> ls()

# 查看特定合约统计
apps$APP00001$state$attrs$MA605 |> unlist()

# 导出Tick数据
inst <- apps$APP00001$ctpclient$instruments$MA605
data.table(
    DateTime = as.POSIXct(inst$DateTime[1:inst$size()]),
    LastPrice = inst$LastPrice[1:inst$size()]
) |> write.csv("ma605_ticks.csv")
7. 性能优化设计
优化策略	实现方式	效果
预分配向量	numeric(capacity)	减少动态扩容
线性扩容	容量不足时增长	平衡内存与性能
原子向量存储	基础R类型	降低开销
data.table存储	列式存储	高效聚合
环境哈希	new.env(hash = TRUE)	O(1)属性访问
8. 容错设计















9. 部署架构
9.1 单机部署
bash
# 安装依赖
Rscript -e "install.packages(c('shiny', 'shinydashboard', 'dplyr', 'jsonlite', 
    'lubridate', 'data.table', 'purrr', 'R6', 'svSocket', 'later'))"

# 启动应用
Rscript klc2.R

# 运维控制台
nc localhost 随机端口号
9.2 启动流程







10. 技术栈总结
层次	技术选型	用途
后端框架	Shiny	Web应用框架
数据存储	data.table	K线数据存储与聚合
异步通信	later + svSocket	定时任务与Socket服务
对象编程	R6	状态管理类
前端图表	klinecharts.js	K线图渲染
网络协议	TCP Socket + JSON	行情数据传输
11. 目录结构
text
项目根目录/
├── klc2.R                    # 主应用脚本
├── README.md                 # 本文档
├── www/
│   ├── css/
│   │   └── style.css         # 深色主题样式
│   └── js/
│       ├── klinecharts.min.js
│       └── chartapp.js
12. 系统亮点
序号	亮点	说明
1	增量聚合算法	避免全量重算，降低CPU消耗
2	递推统计	在线计算均值/方差，内存恒定
3	原子向量存储	Tick数据紧凑存储，线性扩容
4	运维控制台	Socket接口动态管理
5	多实例隔离	每个会话独立状态
6	可视化丰富	集成5种技术指标
7	容错机制	断线重连、异常捕获
13. 扩展建议
策略回测引擎集成

多周期联动显示

价差套利监控

Docker容器化部署

Redis缓存历史K线

14. 版本历史
版本	日期	变更内容
v4.2	当前	恢复完整指标，修复K线合并逻辑
v4.1	-	增加运维控制台，随机端口分配
v4.0	-	重构为R6状态管理
文档更新日期：2026-04-04