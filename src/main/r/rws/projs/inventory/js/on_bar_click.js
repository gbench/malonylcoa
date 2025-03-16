function(el, x) {
    // 初始化持久化存储
    if (!el.dataset.originalColors) {
        // 存储每个曲线所有点的原始边框颜色
        el.dataset.originalColors = JSON.stringify([]);
        // 存储当前选中的 [曲线索引, 点索引]
        el.dataset.currentSelected = JSON.stringify(null);
    }

    el.on('plotly_click', function (data) {
        const point = data.points[0];
        if (!point) return;

        const traceIndex = point.curveNumber;
        const pointIndex = point.pointNumber;
        const trace = el.data[traceIndex];

        // 获取持久化存储的数据
        let originalColors = JSON.parse(el.dataset.originalColors);
        let currentSelected = JSON.parse(el.dataset.currentSelected);

        // 首次运行时初始化所有曲线点的原始颜色
        if (originalColors.length === 0) {
            originalColors = el.data.map(t => {
                return Array.isArray(t.marker.line.color) ?
                    [...t.marker.line.color] :
                    new Array(t.x.length).fill(t.marker.line.color || 'rgba(0,0,0,0)');
            });
            el.dataset.originalColors = JSON.stringify(originalColors);
        }

        // 创建可修改的颜色副本
        let newColors = originalColors.map(arr => [...arr]);

        // 恢复上一次选中点的颜色
        if (currentSelected!== null) {
            const [prevTraceIndex, prevPointIndex] = currentSelected;
            newColors[prevTraceIndex][prevPointIndex] = originalColors[prevTraceIndex][prevPointIndex];
        }

        // 判断当前点击情况
        if (currentSelected!== null && currentSelected[0] === traceIndex && currentSelected[1] === pointIndex) {
            // 重复点击同一曲线的同一点：取消选择
            newColors[traceIndex][pointIndex] = originalColors[traceIndex][pointIndex];
            currentSelected = null;
        } else {
            // 新点击或切换曲线：设置新点击点为红色边框
            newColors[traceIndex][pointIndex] = 'rgba(255,0,0,1)';
            currentSelected = [traceIndex, pointIndex];
        }

        // 更新所有曲线的点边框颜色
        for (let i = 0; i < el.data.length; i++) {
            Plotly.restyle(
                el,
                { 'marker.line.color': [newColors[i]] },
                [i]
            );
        }

        // 持久化存储当前选中状态
        el.dataset.currentSelected = JSON.stringify(currentSelected);
    });
}
