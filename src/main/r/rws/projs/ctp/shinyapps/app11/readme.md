# CTP实时K线图系统架构设计文档

## 1. 系统概述

本系统是一个基于R Shiny的实时期货行情监控平台，通过异步TCP连接接收CTP（中国期货市场监控中心）行情数据，实时聚合K线并提供可视化图表展示。

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
graph TB
    subgraph "外部数据源"
        CTP[CTP期货服务器<br/>192.168.1.41:9898]
    end

    subgraph "R Shiny应用层"
        direction TB
        
        subgraph "通信层"
            CTPClient[CTPD异步客户端<br/>ctpd_async2]
            SocketServer[Socket控制台<br/>端口随机分配]
        end
        
        subgraph "业务逻辑层"
            StateManager[状态管理器<br/>InstrumentStateManager]
            KlineAgg[K线聚合引擎<br/>aggregate_kline]
            StatsCalc[统计计算引擎<br/>update_stats_batch]
            PricePoll[价格轮询<br/>reactivePoll]
        end
        
        subgraph "数据存储层"
            TickCache[Tick数据<br/>原子向量存储]
            KlineCache[K线缓存<br/>data.table]
            AttrStore[属性存储<br/>new.env hash]
        end
        
        subgraph "表现层"
            UI[Shiny UI<br/>侧边栏+主图区]
            ChartJS[klinecharts.js<br/>图表渲染]
            Console[CTP R控制台<br/>命令行管理]
        end
    end

    subgraph "客户端"
        Browser[Web浏览器<br/>localhost:随机端口]
    end

    CTP -->|TCP Socket| CTPClient
    CTPClient -->|行情推送| TickCache
    TickCache -->|增量提取| KlineAgg
    KlineAgg -->|K线数据| KlineCache
    KlineCache -->|WebSocket消息| ChartJS
    PricePoll -->|统计更新| UI
    UI -->|用户操作| StateManager
    
    SocketServer -->|运维管理| Console
    Console -->|查看/操作| StateManager
    Console -->|查询| TickCache
    
    ChartJS -->|渲染| Browser
    UI -->|HTML/CSS/JS| Browser

    style CTPClient fill:#667eea,stroke:#333,stroke-width:2px,color:#fff
    style StateManager fill:#48bb78,stroke:#333,stroke-width:2px,color:#fff
    style KlineAgg fill:#ef5350,stroke:#333,stroke-width:2px,color:#fff
    style ChartJS fill:#ffa500,stroke:#333,stroke-width:2px,color:#fff
    style Console fill:#9b59b6,stroke:#333,stroke-width:2px,color:#fff
```

---

## 3. 核心模块设计

### 3.1 CTPD异步客户端 (`ctpd_async2`)

**职责**：管理与CTP服务器的TCP连接，异步接收行情数据

**数据结构**：
```r
# 合约存储结构（每个合约独立环境）
instruments[[instrument_id]] = {
    # 原子向量（预分配容量20000）
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
    ticks_dt(idx)    # 转换为data.table
}
```

**工作流程**：
```mermaid
sequenceDiagram
    participant CTP as CTP Server
    participant Client as ctpd_async2
    participant Cache as TickCache
    participant App as Shiny App
    
    Client->>CTP: 连接请求
    CTP-->>Client: 建立连接
    Client->>CTP: 发送"dump -1"
    
    loop 异步轮询
        Client->>CTP: 读取行情行
        CTP-->>Client: JSON格式Tick
        Client->>Cache: add(tick)
        Cache->>App: 更新通知
    end
    
    App->>Client: stop()
    Client->>CTP: undump [sessionfd]
```

### 3.2 状态管理器 (`InstrumentStateManager`)

**职责**：管理多合约的K线缓存、tick计数、统计属性

```r
class InstrumentStateManager {
    # 核心存储
    kline_cache: list[instrument_id] = data.table  # K线数据
    tick_counts: list[instrument_id] = integer      # 已处理tick数
    attrs: environment[hash]                        # 统计属性
    current_instrument_id: string                   # 当前合约
    current_period: integer                         # 当前周期（分钟）
    
    # 方法
    get_kline_dt(instrument_id)
    set_kline_dt(instrument_id, dt)
    clear_instrument(instrument_id)
    clear_all()
    set_current(instrument_id, period)
}
```

### 3.3 K线聚合引擎

**核心算法**：

```mermaid
flowchart LR
    subgraph Tick数据
        T1[LastPrice: 3850<br/>Volume: 1000]
        T2[LastPrice: 3852<br/>Volume: 1002]
        T3[LastPrice: 3851<br/>Volume: 1005]
        T4[LastPrice: 3853<br/>Volume: 1008]
    end
    
    subgraph 周期分组
        P1[Period: T0<br/>tick 1-4]
    end
    
    subgraph K线生成
        K1[timestamp: T0*1000<br/>open: 3850<br/>high: 3853<br/>low: 3850<br/>close: 3853<br/>volume: 8<br/>oint: ...]
    end
    
    T1 & T2 & T3 & T4 -->|按Period分组| P1
    P1 -->|聚合计算| K1
```

**增量合并策略**：
```r
merge_kline_dt = function(base_dt, new_dt) {
    # 1. 更新连接：合并已存在的K线
    base_dt[new_dt, on = "timestamp", `:=`(
        high = pmax(high, i.high),
        low = pmin(low, i.low),
        close = i.close,
        volume = volume + i.volume,
        oint = i.oint
    )]
    
    # 2. 追加新K线
    new_rows = new_dt[!base_dt, on = "timestamp"]
    base_dt = rbind(base_dt, new_rows)
    
    return(base_dt)
}
```

### 3.4 统计计算引擎

**递推更新算法**（Welford在线算法）：

```r
update_stats_batch = function(prev_stats, new_prices) {
    # 输入：历史统计 + 新价格数组
    # 输出：更新后的统计量
    
    new_n = prev_n + k
    new_mean = (prev_n * prev_mean + sum(new_prices)) / new_n
    
    # M2更新（离差平方和）
    sum_sq_new = sum((new_prices - new_mean)^2)
    new_M2 = prev_M2 + sum_sq_new + prev_n * (prev_mean - new_mean)^2
    
    # 极值更新
    new_min = min(prev_min, min(new_prices))
    new_max = max(prev_max, max(new_prices))
    
    return(list(
        n = new_n,
        mean = new_mean,
        sd = sqrt(new_M2 / (new_n - 1)),
        min = new_min,
        max = new_max,
        last = new_prices[k]
    ))
}
```

**优势**：
- O(1)时间复杂度
- 无需存储历史价格
- 数值稳定性高

---

## 4. 数据流设计

### 4.1 实时数据流

```mermaid
sequenceDiagram
    participant CTP as CTP服务器
    participant Client as ctpd_async2
    participant Poll as price_data<br/>reactivePoll
    participant Stats as 统计计算
    participant UI as 界面渲染
    participant Chart as klinecharts
    
    loop 每500ms
        CTP->>Client: JSON Tick
        Client->>Client: add(tick)
        Client->>Poll: 更新lastupdate
    end
    
    loop 每500ms轮询
        Poll->>Client: 检查lastupdate
        Client-->>Poll: 时间戳变化
        Poll->>Client: 获取增量数据
        Client-->>Poll: 新价格数组
        Poll->>Stats: update_stats_batch
        Stats-->>Poll: 更新统计量
        Poll->>UI: 刷新价格显示
    end
    
    loop 每1秒（可配置）
        Chart->>Client: 请求K线数据
        Client->>Client: aggregate_kline
        Client->>Chart: WebSocket消息
        Chart->>Chart: 更新图表
    end
```

### 4.2 K线更新策略

```mermaid
stateDiagram-v2
    [*] --> 定时触发
    定时触发 --> 检查连接
    
    检查连接 --> 无连接: NULL
    无连接 --> [*]
    
    检查连接 --> 获取增量: 有连接
    获取增量 --> 无新数据: current == last
    无新数据 --> [*]
    
    获取增量 --> 全量聚合: last == 0
    全量聚合 --> 合并K线
    
    获取增量 --> 增量聚合: last > 0
    增量聚合 --> 合并K线
    
    合并K线 --> 更新缓存
    更新缓存 --> 发送前端: 当前合约
    发送前端 --> [*]
```

---

## 5. 前端架构

### 5.1 图表库集成

```javascript
// klinecharts.js 集成架构
{
    // 1. 指标注册
    indicators: [
        "MA", "VOL", "MACD", "KDJ", "OINT"
    ],
    
    // 2. 消息处理器
    handlers: {
        updateKline: (data) => {
            if (data.type === "full") {
                chart.applyNewData(data.ds);
            } else {
                data.ds.forEach(k => chart.updateData(k));
            }
        },
        switchInstrument: (msg) => {
            clearChart();
            currentInstrument = msg.instrument;
        }
    },
    
    // 3. 样式配置
    styles: {
        candle: { upColor: "#ef5350", downColor: "#26a69a" },
        grid: { color: "#2d2d3f" }
    }
}
```

### 5.2 实时行情面板

```
┌─────────────────────────────────┐
│ 状态栏 ●已连接 | MA605          │
├─────────────────────────────────┤
│ 实时行情                        │
│ MA605                           │
│ 最新: 3852.00  均: 3848.50     │
│ 最高: 3855.00  低: 3840.00     │
│ Ticks: 12450  时间: 14:35:22   │
├─────────────────────────────────┤
│ 卖盘 Ask        买盘 Bid        │
│ 3853.00  100    3852.00  50    │
│ 3854.00  200    3851.00  80    │
│ 3855.00  150    3850.00 120    │
└─────────────────────────────────┘
```

---

## 6. 运维管理控制台

### 6.1 Socket控制台架构

```mermaid
graph LR
    subgraph "运维终端"
        NC[nc localhost PORT]
        TCL[tclsh]
    end
    
    subgraph "R环境"
        SocketServer[svSocket Server<br/>端口:随机]
        AppPool[apps环境<br/>全局注册表]
    end
    
    subgraph "Shiny实例"
        APP1[APP00001<br/>MA605监控]
        APP2[APP00002<br/>RB2510监控]
        APP3[APP00003<br/>价差套利]
    end
    
    NC -->|TCP连接| SocketServer
    TCL -->|命令| SocketServer
    SocketServer -->|eval| AppPool
    AppPool -->|管理| APP1
    AppPool -->|管理| APP2
    AppPool -->|管理| APP3
```

### 6.2 常用运维命令

```r
# 查看所有应用实例
apps |> ls()

# 查看特定合约统计
apps$APP00001$state$attrs$MA605 |> unlist()

# 导出Tick数据
insts <- ctpclient$instruments
insts$MA605$aslist() |> 
    data.table::rbindlist() |> 
    write.csv("ma605.csv")

# 实时价格分布
insts$MA605$aslist() |> 
    data.table::rbindlist() |> 
    with(hist(LastPrice, main="MA605价格分布"))
```

---

## 7. 性能优化设计

### 7.1 内存优化

| 优化策略 | 实现方式 | 效果 |
|---------|---------|------|
| 预分配向量 | `numeric(capacity)` | 减少动态扩容 |
| 线性扩容 | 容量不足时翻倍 | 平衡内存与性能 |
| 原子向量存储 | 基础R类型 | 降低开销 |
| data.table存储 | 列式存储K线 | 高效聚合 |

### 7.2 计算优化

```r
# 1. 批量处理
update_stats_batch(prev, new_prices)  # O(1) vs O(n)

# 2. 增量聚合
idx <- (last_count + 1):current_count  # 仅处理新增tick

# 3. data.table原地更新
base_dt[new_dt, on = "timestamp", `:=`(high = pmax(high, i.high))]
```

### 7.3 网络优化

- 异步非阻塞I/O（`later`包）
- 可配置推送间隔（0.1-5秒）
- 增量K线传输（减少带宽）

---

## 8. 容错设计

```mermaid
flowchart TD
    Start[系统运行] --> Check1{连接正常?}
    
    Check1 -->|是| Process[处理行情]
    Check1 -->|否| Reconnect[重连机制]
    Reconnect -->|3次失败| Notify[用户通知]
    
    Process --> Check2{K线聚合?}
    Check2 -->|异常| LogError[记录错误]
    LogError --> Continue[跳过本次]
    Continue --> Process
    
    Process --> Check3{内存溢出?}
    Check3 -->|是| GC[垃圾回收]
    GC --> Process
    
    Process --> Check4{前端断开?}
    Check4 -->|是| Buffer[缓存K线]
    Buffer -->|恢复后| Sync[全量同步]
```

---

## 9. 部署架构

### 9.1 单机部署

```bash
# 启动应用
Rscript klc2.R

# 访问地址
http://localhost:随机端口

# 运维控制台
nc localhost 随机端口
```

### 9.2 端口分配策略

```r
rand_port(min_port=1024, max_port=65535) {
    1. 随机生成端口
    2. 尝试socketConnection
    3. 连接失败=端口可用
    4. 重试最多100次
}
```

---

## 10. 技术栈总结

| 层次 | 技术选型 | 用途 |
|-----|---------|------|
| 后端框架 | Shiny | Web应用框架 |
| 数据存储 | data.table | K线数据存储与聚合 |
| 异步通信 | later + svSocket | 定时任务与Socket服务 |
| 对象编程 | R6 | 状态管理类 |
| 前端图表 | klinecharts.js | K线图渲染 |
| 网络协议 | TCP Socket + JSON | 行情数据传输 |
| 并发模型 | 事件驱动 + 轮询 | 非阻塞数据处理 |

---

## 11. 系统亮点

1. **增量聚合算法**：避免全量重算，降低CPU消耗
2. **递推统计**：在线计算均值/方差，内存占用恒定
3. **原子向量存储**：Tick数据紧凑存储，支持线性扩容
4. **运维控制台**：Socket接口支持动态管理
5. **多实例隔离**：每个浏览器会话独立状态
6. **可视化丰富**：集成5种技术指标
7. **容错机制**：断线重连、异常捕获、日志记录

---

## 12. 扩展建议

### 12.1 功能扩展
- 策略回测引擎集成
- 多周期联动显示
- 价差套利监控
- 自动交易接口

### 12.2 性能扩展
- 使用Rcpp重写核心聚合算法
- Redis缓存历史K线
- WebSocket替代轮询

### 12.3 部署扩展
- Docker容器化
- 多进程负载均衡
- 数据库持久化
