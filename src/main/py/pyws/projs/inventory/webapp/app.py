from flask import Flask, render_template, request, jsonify
import mysql.connector
import re
from datetime import datetime

app = Flask(__name__)

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3371,
    'user': 'root',
    'password': '123456',
    'database': 'inventory'
}


@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        data = request.get_json()
        ioentry = {
            "product_id": data.get('product_id'),
            "quantity": int(data.get('quantity')),
            "drcr": int(data.get('drcr')),
            "company_id": data.get('company_id'),
            "warehouse_id": data.get('warehouse_id'),
            "bill_id": data.get('bill_id'),
            "create_time": data.get('create_time'),
            "description": data.get('description')
        }

        # 验证表单字段
        if not (1 <= ioentry["quantity"] <= 10):
            return jsonify({"error": "数量必须在 1 到 10 之间"}), 400
        if not re.match(r'(IN|OUT|CONTRACT)\d{12}', ioentry["bill_id"]):
            return jsonify({"error": "单据编号格式不正确"}), 400

        result = create_inv_order(ioentry)
        if result.get('error'):
            return jsonify(result), 400
        return jsonify(result)

    # 产品选项映射
    product_options = {
        "apple": "apple001",
        "banana": "banana001",
        "strawberry": "strawberry001"
    }
    # 公司选项映射
    company_options = {
        "沃尔玛": "CMPN001",
        "亚马逊": "CMPN002"
    }
    # 仓库选项映射
    warehouse_options = {
        "北京京邦达贸易有限公司": "WRHS001",
        "顺丰控股股份有限公司": "WRHS002"
    }
    # 默认下单时间
    default_create_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    kwargs = {"product_options":product_options, 
        "company_options":company_options, 
        "warehouse_options":warehouse_options, 
        "default_create_time":default_create_time}
   
    return render_template('index.html', **kwargs)


def create_inv_order(ioentry):
    product_name = re.findall(r'[a-zA-Z]+', ioentry["product_id"])[0]
    date = ioentry["create_time"].replace("-", "")[:8]
    table_name = f't_{product_name}_{date}'

    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 创建表（如果不存在）
        create_table_query = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            product_id VARCHAR(255) NOT NULL,
            quantity INT NOT NULL,
            drcr INT NOT NULL,
            company_id VARCHAR(255) NOT NULL,
            warehouse_id VARCHAR(255) NOT NULL,
            bill_id VARCHAR(255) NOT NULL,
            create_time DATETIME NOT NULL,
            description VARCHAR(255) NOT NULL
        )
        """
        cursor.execute(create_table_query)

        # 插入数据
        insert_query = f"""
        INSERT INTO {table_name} (name, product_id, quantity, drcr, company_id, warehouse_id, bill_id, create_time, description)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        values = (
            re.sub(r'\d+$', '', ioentry["product_id"]),
            ioentry["product_id"],
            ioentry["quantity"],
            ioentry["drcr"],
            ioentry["company_id"],
            ioentry["warehouse_id"],
            ioentry["bill_id"],
            ioentry["create_time"],
            ioentry["description"]
        )
        cursor.execute(insert_query, values)
        conn.commit()

        # 查询最新数据
        select_query = f"SELECT * FROM {table_name}"
        cursor.execute(select_query)
        columns = [col[0] for col in cursor.description]
        data = [dict(zip(columns, row)) for row in cursor.fetchall()]

        cursor.close()
        conn.close()

        return {"data": data}

    except mysql.connector.Error as err:
        return {"error": str(err)}


if __name__ == '__main__':
    app.run(debug=True)
    