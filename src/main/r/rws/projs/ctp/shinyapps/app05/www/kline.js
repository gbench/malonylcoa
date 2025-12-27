// www/kline.js
(function(){
  const chart = klinecharts.init('chart');

  /* 1. 副图成交量 */
  chart.createIndicator('VOL');

  /* 2. 注册 5 条 EMA 覆盖指标（主图） */
  function addEMA(period, color){
    chart.createIndicator(
      {
        name: 'MA',
        calcParams: [period],
        regenerateFigures: () => [
          {
            key: 'ma',
            title: 'EMA' + period,
            type: 'line',
            baseValue: 0,
            styles: { line: { color: color, size: 1 } }
          }
        ],
        calc: (klineDataList) =>
          klineDataList.map(k => ({ ma: k['ema' + period] }))
      },
      true,                       // overlay
      { id: 'candle_pane' }       // 主图 pane
    );
  }

  addEMA(1,  '#FF6B6B');
  addEMA(5,  '#F9C74F');
  addEMA(15, '#90BE6D');
  addEMA(30, '#43AA8B');
  addEMA(60, '#277DA1');

  /* 3. 监听 Shiny 推送 */
  Shiny.addCustomMessageHandler('push', j => chart.applyNewData(j));
})();