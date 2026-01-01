// js/exit.js 或添加到 kline.js 末尾
document.addEventListener('DOMContentLoaded', function() {
  // 等待页面完全加载
  setTimeout(function() {
    // 找到退出按钮
    var exitBtn = document.getElementById('exit');
    
    if (exitBtn) {
      // 移除之前的点击事件（如果有）
      exitBtn.onclick = null;
      
      // 添加新的点击事件
      exitBtn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        // 显示确认对话框
        if (confirm('确定要退出应用程序吗？')) {
          // 发送退出信号到Shiny
          Shiny.setInputValue('confirm_exit', 'yes', {priority: 'event'});
          
          // 可选：延迟停止应用
          setTimeout(function() {
            // 如果需要关闭窗口
            // window.close();
          }, 100);
        }
      });
    }
  }, 500); // 延迟确保按钮已渲染
});