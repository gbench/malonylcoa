function(el, x) {
  console.log('Plotly chart rendered');
  el.on('plotly_click', function (data) {
    console.log('Click event triggered', data);
    const point = data.points[0];
    if (point) {
      console.log('Point found');
      const trace = point.data;
      const xaxis = point.xaxis
      const yaxis = point.yaxis
      const x = point.x;
      const y = point.y;
      const selectedBar = trace.x.indexOf(x);
      console.log({ xaxis, yaxis, x, y, selectedBar });
      const newLine = { color: 'rgba(255, 0, 0, 1)', width: 2 };
      trace.marker.line = newLine;
      Plotly.redraw(el);
    } // if
  });
}
