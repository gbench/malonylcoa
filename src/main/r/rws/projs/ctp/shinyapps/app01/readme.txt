金融数据分析仪表板应用

启动程序办法：
进入目录：F:\slicef\ws\gitws\malonylcoa\src\main\r\rws\projs\ctp\shinyapp\app01
打开命令行：运行app.R，把程序至于端口10000
@rem 加载library(shiny)库,启动shinyApp应用
R -e "library(shiny);runApp('%cd:\=/%', port=10000)"