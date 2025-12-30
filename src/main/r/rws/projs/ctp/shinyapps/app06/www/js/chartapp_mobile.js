// 移动端白色主题K线图 - 显示VOL成交量
(function () {
  const chart = klinecharts.init("chart_mobile", {
    styles: {
      candle: {
        bar: {
          upColor: '#26A69A',
          downColor: '#EF5350',
          noChangeColor: '#78909C'
        },
        priceMark: {
          high: {
            color: '#26A69A'
          },
          low: {
            color: '#EF5350'
          },
          last: {
            upColor: '#26A69A',
            downColor: '#EF5350'
          }
        },
        tooltip: {
          text: {
            size: 12
          }
        }
      },
      indicator: {
        legend: {
          fontSize: 11
        },
        tag: {
          fontSize: 11
        }
      },
      crosshair: {
        horizontal: {
          line: {
            color: '#2962FF'
          }
        },
        vertical: {
          line: {
            color: '#2962FF'
          }
        }
      }
    }
  });
  
  let currentInstrument = null;
  
  // 创建VOL成交量指标（副图）
 
  chart.createIndicator("VOL"); // 副图
  chart.createIndicator("KDJ"); // 副图
  chart.createIndicator("MACD"); // 副图
  chart.createIndicator("MA", true, { id: "candle_pane" });
  
  // 可选：添加简单移动平均线（主图）
  chart.createIndicator("MA", true, { 
    id: 'candle_pane',
    calcParams: [5, 10, 20],  // 5,10,20日均线
    styles: {
      line: [
        { color: '#FF6B6B', size: 1 },  // 5日线
        { color: '#4ECDC4', size: 1 },  // 10日线
        { color: '#FFD166', size: 1 }   // 20日线
      ]
    }
  });
  
  Shiny.addCustomMessageHandler("push_mobile", ({ instrument, ds }) => {
    if (instrument !== currentInstrument && !!ds && ds.length>0) {
      currentInstrument = instrument;
      chart.clearData();
      chart.applyNewData(ds);
      return;
    }
    
    const dls = chart.getDataList()
    if (!!dls && dls.length>0) {
      ds.forEach((bar) => chart.updateData(bar));
    } else if(!!ds && ds.length>0) {
      chart.applyNewData(ds);
    }
  });
})();