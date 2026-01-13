// www/kline.js
(function () {
  /* ========== 全局状态 ========== */
  const chart = klinecharts.init("chart");
  let currentInstrument = null; // 当前品种

  // 持仓量
  klinecharts.registerIndicator({
    name: "OINT", // 指标名，之后用这个名字挂载
    shortName: "OINT", // 左上角缩写
    calcParams: [], // 本例不需要参数
    shouldFormatBigNumber: true, // 格式化参数
    figures: [
      {
        key: "oint",
        title: "持仓量: ",
        type: "line",
      },
      {
        key: "preoint",
        title: "前仓量: ",
        type: "line",
      },
    ],
    calc: (kLineDataList, _) => {
      // 核心：把 K 线数据里的 openInterest 抽出来返回
      return kLineDataList.map((k, i, ks) => ({
        oint: k.oint,
        preoint: i < 1 ? k.oint : ks[i - 1].oint,
      }));
    },
  });

  // 剩余时长
  klinecharts.registerOverlay({
    name: "timeTick",
    totalStep: 1,
    needDefaultPointFigure: false,
    needDefaultXAxisFigure: false,
    needDefaultYAxisFigure: false,

    createPointFigures: () => {
      const dataList = chart.getDataList();
      if (!dataList.length) return [];

      const last = dataList[dataList.length - 1];
      const leftSec = (((120 - last.times) / 120) * 60).toFixed(0); //  剩余更新次数
      const { x, y } = chart.convertToPixel(
        { timestamp: last.timestamp, value: last.low },
        { paneId: "candle_pane", absolute: false }
      );

      return [
        {
          type: "text",
          attrs: {
            x,
            y,
            text: `${leftSec.padStart(2, "0")}S`,
          }, // 标记剩余时间
          styles: {
            color: "white",
            size: 14,
            backgroundColor: "rgba(33, 150, 243, 0.2)",
          },
        },
      ];
    },
  });

  /* ========== 1. 指标只建一次 ========== */
  (function initOnce() {
    chart.createIndicator("OINT"); // 副图
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

  window.ttid = chart.createOverlay({
    name: "timeTick",
    points: [{}], // 必须指定points, 否则挂载不上
  }); //  全局的timeTick的id

  /* ========== 3. 监听 Shiny 推送 ========== */
  Shiny.addCustomMessageHandler("push", ({ instrument, ds }) => {
    const flag = !!ds && ds.length > 0;

    if (!flag) {
      // 数据无效
      console.log("数据无效！");
      return;
    } else if (instrument !== currentInstrument) {
      // 后端推送了新品种
      currentInstrument = instrument;
      chart.clearData(); // 清旧 K 线
      chart.applyNewData(ds); // 直接整包新数据
      return;
    }

    // 同一品种：增量更新
    const dls = chart.getDataList();
    if (!dls || dls.length < 1) {
      chart.applyNewData(ds, { period: 60 });
    } else {
      // 数据更新
      ds.forEach((bar) => chart.updateData(bar));
    } // if
  }); // Shiny.addCustomMessageHandler
})();
