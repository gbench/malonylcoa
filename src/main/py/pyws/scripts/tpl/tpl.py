import datetime
import time

def filtpl(template_file, mappings):
    """
    Args:
        template_file 模板文件
        mapping 模板映射
    """
    try:
        with open(template_file, 'r', encoding='utf-8') as file:
            template = file.read()
        for var, value in mappings.items():
            placeholder = '{' + var + '}'
            template = template.replace(placeholder, str(value))
        return template
    except FileNotFoundError:
        print(f"错误: 文件 {template_file} 未找到。")
    except Exception as e:
        print(f"错误: 发生了未知错误: {e}")
    return None    

# 假设 mappings 是包含变量替换值的字典
mappings = {'product_name': 'apple', 'date': time.strftime("%Y%m%d", time.localtime())}
# 读取 pct_ctsql.sql 文件并替换变量
result = filtpl('./pct_ctsql.sql', mappings)
if result is not None:
    print(result)

