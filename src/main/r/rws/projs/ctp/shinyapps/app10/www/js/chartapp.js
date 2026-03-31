
// www/js/chartapp.js
(function () {
  const chart = klinecharts.init("chart");
  let currentInstrument = null;
  let instrumentDataCache = new Map();
  
  function debugLog(msg) {
    console.log("[" + new Date().toLocaleTimeString() + "] " + msg);
  }
  
  debugLog("K线图表初始化开始");
  
  klinecharts.registerIndicator({
    name: "OINT",
    shortName: "OINT",
    calcParams: [],
    shouldFormatBigNumber: true,
    figures: [
      { key: "oint", title: "持仓量: ", type: "line" },
      { key: "preoint", title: "前仓量: ", type: "line" }
    ],
    calc: function(kLineDataList, _) {
      return kLineDataList.map(function(k, i, ks) {
        return {
          oint: k.oint || 0,
          preoint: i < 1 ? (k.oint || 0) : (ks[i - 1].oint || 0)
        };
      });
    }
  });

  chart.setStyles({
    grid: {
      horizontal: { color: "#2d2d3f", size: 1 },
      vertical: { color: "#2d2d3f", size: 1 }
    },
    candle: {
      candle: {
        bar: { upColor: "#ef5350", downColor: "#26a69a", noChangeColor: "#888888" }
      }
    },
    xAxis: {
      line: { color: "#4a5568" },
      tick: { color: "#e0e0e0" }
    },
    yAxis: {
      line: { color: "#4a5568" },
      tick: { color: "#e0e0e0" }
    }
  });

  chart.createIndicator("VOL");
  chart.createIndicator("MA", true, { id: "candle_pane" });
  chart.createIndicator("MACD");

  function loadInstrumentData(instrument, data, type) {
    debugLog("loadInstrumentData: " + instrument + ", type: " + type);
    
    if (!instrument || !data) {
      debugLog("Invalid data");
      return;
    }
    
    if (type === "full") {
      instrumentDataCache.set(instrument, data);
      debugLog("缓存全量数据: " + data.length + "根K线");
    } else if (type === "incremental") {
      let cached = instrumentDataCache.get(instrument) || [];
      instrumentDataCache.set(instrument, cached.concat(data));
      debugLog("缓存增量数据: " + data.length + "根新K线");
    } else if (type === "update") {
      debugLog("更新最后一根K线");
    }
    
    if (instrument === currentInstrument) {
      if (type === "full") {
        chart.clearData();
        chart.applyNewData(data);
        debugLog("全量数据已加载到图表");
      } else if (type === "incremental") {
        const currentData = chart.getDataList();
        if (!currentData || currentData.length === 0) {
          chart.applyNewData(data);
        } else {
          data.forEach(function(bar) {
            chart.updateData(bar);
          });
        }
        debugLog("增量数据已更新到图表");
      } else if (type === "update") {
        chart.updateData(data);
        debugLog("最后一根K线已更新");
      }
    }
  }
  
  function switchToInstrument(instrument) {
    debugLog("switchToInstrument: " + instrument);
    
    if (instrument === currentInstrument) {
      debugLog("合约相同，无需切换");
      return;
    }
    
    currentInstrument = instrument;
    const cachedData = instrumentDataCache.get(instrument);
    
    if (cachedData && cachedData.length > 0) {
      chart.clearData();
      chart.applyNewData(cachedData);
      debugLog("从缓存恢复数据: " + cachedData.length + "根K线");
    } else {
      chart.clearData();
      debugLog("等待数据加载");
    }
  }
  
  function clearChart() {
    debugLog("清空图表");
    chart.clearData();
  }

  Shiny.addCustomMessageHandler("push", function(data) {
    debugLog("收到推送消息, 类型: " + data.type);
    
    if (!data || !data.ds) {
      debugLog("消息无效");
      return;
    }
    
    if (data.type === "full") {
      loadInstrumentData(data.instrument, data.ds, "full");
    } else if (data.type === "incremental") {
      loadInstrumentData(data.instrument, data.ds, "incremental");
    } else if (data.type === "update") {
      loadInstrumentData(data.instrument, data.ds, "update");
    }
  });

  Shiny.addCustomMessageHandler("switchInstrument", function(msg) {
    debugLog("收到切换合约消息");
    if (msg && msg.instrument) {
      switchToInstrument(msg.instrument);
    }
  });
  
  Shiny.addCustomMessageHandler("clearChart", function() {
    debugLog("收到清空图表消息");
    clearChart();
  });
  
  Shiny.addCustomMessageHandler("clearChartForInstrument", function(instrument) {
    debugLog("收到清空合约图表消息: " + instrument);
    if (instrument === currentInstrument) {
      chart.clearData();
      instrumentDataCache.delete(instrument);
      debugLog("强制刷新合约");
    }
  });

  window.addEventListener("beforeunload", function() {
    instrumentDataCache.clear();
  });

  debugLog("K线图表初始化完成");
})();

