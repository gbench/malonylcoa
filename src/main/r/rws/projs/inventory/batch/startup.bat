@echo off
set "current_dir=%~dp0"
set "parent_dir=%current_dir:~0,-1%"

@rem 提取上级目录  
set "parent_dir=%current_dir:~0,-1%"  
for %%A in ("%parent_dir%") do set "parent_dir=%%~dpA"

@rem 目录显示  
echo Current Directory: %current_dir%
echo Parent Directory: %parent_dir%

@rem 
set cmd="source(\"%parent_dir:\=/%main.R\")"
echo %cmd%
R -e %cmd%