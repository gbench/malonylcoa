// www/kline.js
(function(){
  const chart = klinecharts.init('chart');
  /* 1. 副图成交量 */
  chart.createIndicator('VOL');
  /* https://klinecharts.com/guide/indicator#%E5%86%85%E7%BD%AE%E6%8A%80%E6%9C%AF%E6%8C%87%E6%A0%87 */
  chart.createIndicator('MA', true, { id:'candle_pane' }) 
  chart.createIndicator('BOL', true, { id:'candle_pane' }) 
  /* 3. 监听 Shiny 推送 */
  Shiny.addCustomMessageHandler('push', j => chart.applyNewData(j));
})();