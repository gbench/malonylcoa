# install.packages(c( "shiny", "shinythemes", "ggplot2", "reshape2","gridExtra", "RColorBrewer", "DT", "purrr", "igraph"))

# 添加 GitHub 作为第二个远程仓库
git remote add github https://github.com/gbench/malonylcoa.git

# 确认远程仓库列表
git remote -v

# 同步
git pull github develop --allow-unrelated-histories

# 推送本地 master 到 github 的 main 分支，并设置上游跟踪
git push -u github develop:develop