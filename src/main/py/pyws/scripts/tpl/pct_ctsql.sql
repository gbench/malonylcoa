CREATE TABLE t_{product_name}_{date} (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    drcr INT NOT NULL,
    company_id VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(255) NOT NULL,
    bill_id VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    description VARCHAR(512)
);