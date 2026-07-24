# install.packages(c( "shiny", "shinythemes", "ggplot2", "reshape2","gridExtra", "RColorBrewer", "DT", "purrr", "igraph"))

# ----------------------------------------------------------------------------------
# 同步本地与
# ----------------------------------------------------------------------------------

# 添加 GitHub 作为第二个远程仓库
git remote add github https://github.com/gbench/malonylcoa.git

# 确认远程仓库列表
git remote -v

# 同步：拉取 GitHub 的 develop 分支并合并
git pull github develop --allow-unrelated-histories

# 修改本地
git add .
git commit -m "Merge remote develop branch"

# 推送本地 master 到 github 的 main 分支，并设置上游跟踪
# git push -u github master:main

# 实际使用 develop 跟踪：git push -u ${remote_repo} ${local_branch}:${remote_branch}
git push -u github develop:develop

# 然后推送: git push ${remote_repo} ${remote_branch}
git push github develop

# ----------------------------------------------------------------------------------