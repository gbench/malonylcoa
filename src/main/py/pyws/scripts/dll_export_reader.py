import pefile
import sys
import os

# 检查是否提供了命令行参数
if len(sys.argv) != 2:
    print("用法: python dll_export_reader.py <dll_file_path>")
    sys.exit(1)

# 获取命令行参数中的 DLL 文件路径，并展开环境变量
dll_path = os.path.expandvars(sys.argv[1])

try:
    # 打开并解析 DLL 文件
    pe = pefile.PE(dll_path)

    if hasattr(pe, 'DIRECTORY_ENTRY_EXPORT'):
        print("导出的函数列表:")
        for exp in pe.DIRECTORY_ENTRY_EXPORT.symbols:
            if exp.name:
                print(exp.name.decode())
    else:
        print("该 DLL 文件没有导出函数。")

    # 关闭文件
    pe.close()

except FileNotFoundError:
    print(f"错误: 文件 {dll_path} 未找到。")
except pefile.PEFormatError:
    print(f"错误: {dll_path} 不是有效的 PE 文件。")
except Exception as e:
    print(f"发生未知错误: {e}")
    
