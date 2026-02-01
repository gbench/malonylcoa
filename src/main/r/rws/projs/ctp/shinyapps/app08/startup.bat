@rem 加载library(shiny)库,启动shinyApp应用
R -e "library(shiny);runApp('%cd:\=/%', host='127.0.0.1', port=10000)"