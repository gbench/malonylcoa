
// www/js/exit.js
document.addEventListener("DOMContentLoaded", function() {
  setTimeout(function() {
    var exitBtn = document.getElementById("exit");
    if (exitBtn) {
      exitBtn.onclick = null;
      exitBtn.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (confirm("确定要退出应用程序吗？")) {
          Shiny.setInputValue("exit", "yes", {priority: "event"});
          setTimeout(function() {
            window.close();
          }, 100);
        }
      });
    }
  }, 500);
});

