#  数据准备
# 在MYSQL服务器上创建数据库
create database inventory default character set utf8mb4;

# 进入目录 F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/data
cd /d F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/data
# 运行脚本：数据导入
R -e "source(\"F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/data/import.R\")"

# 程序文件的启动
# 进入目录 F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/
cd /d F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory
# 运行脚本：主程序执行
R -e "source(\"F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/inventory/main.R\")"

# 前端页面访问
http://127.0.0.1:7070