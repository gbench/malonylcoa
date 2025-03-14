# 安装依赖程序
set mypyver=3.11
set myenv=PY3_11
@REM conda create -n %myenv% python=%mypyver%
conda activate %myenv%
@REM conda install -y pefile

# 进入脚本路径
cd /d F:\slicef\ws\gitws\malonylcoa\src\main\py\pyws\scripts
# 查看R.dll
python dll_export_reader.py %R_HOME%\bin\x64\R.dll
# 查看Rblas
python dll_export_reader.py %R_HOME%\bin\x64\Rblas.dll
