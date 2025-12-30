// www/kline.js
(function () {
  /* ========== 全局状态 ========== */
  const chart = klinecharts.init("chart");
  let currentInstrument = null; // 当前品种

  /* ========== 1. 指标只建一次 ========== */
  (function initOnce() {
    chart.createIndicator("VOL"); // 副图
    chart.createIndicator("KDJ"); // 副图
    chart.createIndicator("MACD"); // 副图
    chart.createIndicator("MA", true, { id: "candle_pane" });
    // chart.createIndicator("BOLL", true, { id: "candle_pane" });
  })();

  /* ========== 2. 外部手动切换（可选） ========== */
  window.switchInstrument = function (instrument) {
    if (instrument === currentInstrument) return;
    currentInstrument = instrument;
    chart.clearData(); // 只清数据，不动指标
    // 可选：通知后端
    Shiny.setInputValue("switchInstrument", instrument);
  };

  /* ========== 3. 监听 Shiny 推送 ========== */
  Shiny.addCustomMessageHandler("push", ({ instrument, ds }) => {
    if (instrument !== currentInstrument && !!ds && ds.length>0) {
      // 后端推送了新品种
      currentInstrument = instrument;
      chart.clearData(); // 清旧 K 线
      chart.applyNewData(ds); // 直接整包新数据
      return;
    }

    // 同一品种：增量更新
    const dls = chart.getDataList()
    if ( !!dls && dls.length>0) {
      ds.forEach((bar) => chart.updateData(bar));
    } else if(!!ds) {
      chart.applyNewData(ds);
    } else {
      // do nothing
    } // if
  }); // Shiny.addCustomMessageHandler

})();
