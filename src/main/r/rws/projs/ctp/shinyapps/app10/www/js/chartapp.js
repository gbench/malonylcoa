
// www/js/chartapp.js
(function () {
  /* ========== 全局状态 ========== */
  const chart = klinecharts.init("chart");
  let currentInstrument = null;
  let updateTimer = null;

  // 持仓量指标
  klinecharts.registerIndicator({
    name: "OINT",
    shortName: "OINT",
    calcParams: [],
    shouldFormatBigNumber: true,
    figures: [
      { key: "oint", title: "持仓量: ", type: "line" },
      { key: "preoint", title: "前仓量: ", type: "line" }
    ],
    calc: (kLineDataList, _) => {
      return kLineDataList.map((k, i, ks) => ({
        oint: k.oint || 0,
        preoint: i < 1 ? (k.oint || 0) : (ks[i - 1].oint || 0)
      }));
    }
  });

  // 设置图表样式
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

  // 创建指标
  chart.createIndicator("VOL");
  chart.createIndicator("MA", true, { id: "candle_pane" });
  chart.createIndicator("MACD");

  /* ========== 监听 Shiny 推送 ========== */
  Shiny.addCustomMessageHandler("push", function(data) {
    const { instrument, ds } = data;
    
    if (!ds || ds.length === 0) {
      console.log("数据无效！");
      return;
    }
    
    if (instrument !== currentInstrument) {
      // 新品种：清空并加载全量数据
      currentInstrument = instrument;
      chart.clearData();
      chart.applyNewData(ds);
      console.log("切换到新品种:", instrument, "数据量:", ds.length);
    } else {
      // 同一品种：增量更新
      const currentData = chart.getDataList();
      if (!currentData || currentData.length === 0) {
        chart.applyNewData(ds);
      } else {
        ds.forEach(function(bar) {
          chart.updateData(bar);
        });
      }
    }
  });

  // 切换合约
  Shiny.addCustomMessageHandler("switchInstrument", function(instrument) {
    if (instrument !== currentInstrument) {
      currentInstrument = instrument;
      chart.clearData();
      console.log("手动切换合约:", instrument);
    }
  });

  // 页面关闭时清理
  window.addEventListener("beforeunload", function() {
    if (updateTimer) clearInterval(updateTimer);
  });

  console.log("K线图表初始化完成");
})();

