function(el, x) {
    // 初始化持久化存储
    if (!el.dataset.originalColors) {
        el.dataset.originalColors = JSON.stringify([]);
        el.dataset.currentSelected = 'null';
    }

    el.on('plotly_click', function (data) {
        const point = data.points[0];
        if (!point) return;

        const traceIndex = point.curveNumber;
        const pointIndex = point.pointNumber;
        const trace = el.data[traceIndex];

        // 获取持久化存储
        let originalColors = JSON.parse(el.dataset.originalColors);
        let currentSelected = JSON.parse(el.dataset.currentSelected);

        // 首次运行时初始化原始颜色
        if (originalColors.length === 0) {
            originalColors = Array.isArray(trace.marker.line.color) ?
                [...trace.marker.line.color] :
                new Array(trace.x.length).fill(trace.marker.line.color || 'rgba(0,0,0,0)');
            el.dataset.originalColors = JSON.stringify(originalColors);
        }

        // 创建可修改的颜色副本
        let newColors = [...originalColors];

        // 恢复上一次选中的颜色
        if (currentSelected !== null && currentSelected < newColors.length) {
            newColors[currentSelected] = originalColors[currentSelected];
        }

        // 判断当前点击行为
        if (currentSelected === pointIndex) {
            // 重复点击相同柱子：取消选择
            newColors[pointIndex] = originalColors[pointIndex];
            currentSelected = null;
        } else {
            // 新点击：设置红色边框
            newColors[pointIndex] = 'rgba(255,0,0,1)';
            currentSelected = pointIndex;
        }

        // 更新图表
        Plotly.restyle(
            el,
            { 'marker.line.color': [newColors] },
            [traceIndex]
        );

        // 持久化存储当前状态
        el.dataset.currentSelected = JSON.stringify(currentSelected);
    });
}
