// 封装获取表单数据的函数
function getFormData() {
    const formData = new FormData(document.getElementById('inventoryForm'));
    const ioentry = {};
    formData.forEach((value, key) => {
        ioentry[key] = value;
    });
    return ioentry;
}

// 封装获取当前时间戳的函数
function formatCurrentTime() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    return `${year}${month}${day}${hours}${minutes}${seconds}`;
}

// 封装获取选中 radio 对应标签的函数
function getDrcrRadioLabel() {
    const selectedRadio = document.querySelector('input[name="drcr"]:checked');
    return selectedRadio ? { "1": "IN", "-1": "OUT", "0": "CONTRACT" }[selectedRadio.value] : '';
}

// 封装生成备注说明的函数
function generateDescription() {
    const ioentry = getFormData();
    return `${getDrcrRadioLabel()}-${ioentry.product_id}`.toUpperCase();
}

// 封装更新 bill_id 输入框值的函数
function updateBillIdInput() {
    const billIdInput = document.getElementById('bill_id');
    if (billIdInput.dataset.userInput === 'true') return;
    const timestamp = formatCurrentTime();
    billIdInput.value = getDrcrRadioLabel() + timestamp;
}

// 封装更新 description 文本框值的函数
function updateDescriptionTextarea() {
    const descriptionTextarea = document.getElementById('description');
    descriptionTextarea.value = generateDescription();
}

// 表单提交事件处理
document.getElementById('inventoryForm').addEventListener('submit', function (e) {
    e.preventDefault();
    const ioentry = getFormData();
    const drcrText = getDrcrRadioLabel();
    const productText = document.getElementById('product_id').options[document.getElementById('product_id').selectedIndex].text;

    fetch('/', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ioentry)
    })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert(data.error);
            } else {
                const table = document.createElement('table');
                const thead = document.createElement('thead');
                const tbody = document.createElement('tbody');

                const headers = Object.keys(data.data[0]);
                const headerRow = document.createElement('tr');
                headers.forEach(header => {
                    const th = document.createElement('th');
                    th.textContent = header;
                    headerRow.appendChild(th);
                });
                thead.appendChild(headerRow);
                table.appendChild(thead);

                data.data.forEach(rowData => {
                    const row = document.createElement('tr');
                    headers.forEach(header => {
                        const td = document.createElement('td');
                        td.textContent = rowData[header];
                        row.appendChild(td);
                    });
                    tbody.appendChild(row);
                });
                table.appendChild(tbody);

                document.getElementById('resultTable').innerHTML = '';
                document.getElementById('resultTable').appendChild(table);
            }
        });
});

// 页面加载完成后的初始化操作
document.addEventListener('DOMContentLoaded', function () {
    const productIdSelect = document.getElementById('product_id');
    const quantityInput = document.getElementById('quantity');
    const billIdInput = document.getElementById('bill_id');

    // 初始化 description 文本框的值
    updateDescriptionTextarea();

    // 初始化 bill_id 输入框的值
    updateBillIdInput();

    // 监听用户在 bill_id 输入框的输入
    billIdInput.addEventListener('input', function () {
        this.dataset.userInput = 'true';
    });

    // 监听 select 和 input 的变化事件
    productIdSelect.addEventListener('change', updateDescriptionTextarea);
    quantityInput.addEventListener('input', updateDescriptionTextarea);

    // 监听 radio 的 change 事件
    const radios = document.querySelectorAll('input[name="drcr"]');
    radios.forEach(function (radio) {
        radio.addEventListener('change', () => {
            updateDescriptionTextarea();
            updateBillIdInput();
        });
    });
});