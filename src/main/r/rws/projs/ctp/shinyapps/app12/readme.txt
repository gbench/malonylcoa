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
# 添加一个名为 all 的远程，指向两个仓库
# ----------------------------------------------------------------------------------
# 先添加一个名为 all 的远程（fetch URL 可以随便填一个）
git remote add all https://gitee.com/gbench/malonylcoa.git
# 指定 push 位置
git remote set-url --add --push all https://gitee.com/gbench/malonylcoa.git
git remote set-url --add --push all https://github.com/gbench/malonylcoa.git
git push all develop

# ----------------------------------------------------------------------------------

Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git remote show origin
* remote origin
  Fetch URL: https://gitee.com/gbench/malonylcoa.git
  Push  URL: https://gitee.com/gbench/malonylcoa.git
  HEAD branch: master
  Remote branches:
    develop tracked
    feature tracked
    master  tracked
  Local branch configured for 'git pull':
    master merges with remote master
  Local refs configured for 'git push':
    develop pushes to develop (up to date)
    master  pushes to master  (up to date)

Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git remote show github
* remote github
  Fetch URL: https://github.com/gbench/malonylcoa.git
  Push  URL: https://github.com/gbench/malonylcoa.git
  HEAD branch: develop
  Remote branches:
    develop tracked
    main    new (next fetch will store in remotes/github)
  Local branch configured for 'git pull':
    develop merges with remote develop
  Local ref configured for 'git push':
    develop pushes to develop (up to date)

# 注意 本地 develop 跟踪的是github/develop； 本地 master 跟踪的是origin/master
Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git branch -vv
* develop 2fe095ee [github/develop] 删除文件
  master  14abe37f [origin/master] 增加图片

# 将 develop 分支的上游改回 origin/develop
git branch --set-upstream-to=origin/develop

Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git branch --set-upstream-to=origin/develop
branch 'develop' set up to track 'origin/develop'.

Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git branch -vv
* develop 9895e276 [origin/develop] 增加跟踪位置查看说明
  master  14abe37f [origin/master] 增加图片

# 增加修改内容
Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git add .

# 提交修改
Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git commit -m "修改默认跟踪远程未origin"
[develop 1f245ee8] 修改默认跟踪远程未origin
 1 file changed, 3 insertions(+)

# 默认推送
Administrator@XQH-THINKPAD-230518 MINGW64 /f/slicef/ws/gitws/malonylcoa (develop)
$ git push
Enumerating objects: 21, done.
Counting objects: 100% (21/21), done.
Delta compression using up to 4 threads
Compressing objects: 100% (11/11), done.
Writing objects: 100% (11/11), 963 bytes | 240.00 KiB/s, done.
Total 11 (delta 8), reused 0 (delta 0), pack-reused 0
remote: Powered by GITEE.COM [1.1.23]
remote: Set trace flag a040d19f
To https://gitee.com/gbench/malonylcoa.git
   9895e276..1f245ee8  develop -> develop



