@rem 加载library(shiny)库,启动shinyApp应用
R -e "library(shiny);runApp('%cd:\=/%', host='0.0.0.0', port=10001)"