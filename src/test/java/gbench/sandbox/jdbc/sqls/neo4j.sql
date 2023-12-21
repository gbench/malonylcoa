-- --------------------------
-- 参与电影#name中的演员
-- # actIn
-- #name: 影片名称
-- --------------------------
match (a)-[:ACTED_IN]->(m:Movie {title:#name}) return a {.born,.name}

-- --------------------------
-- 创建订单
-- # purchase
-- #customer: 用户名称
-- #product: 产品名称
-- #transcode: 交易代码
-- #price: 产品单价
-- #shop: 店铺名称
-- #quantity: 购买数量
-- #unit: 产品单位
-- #createtime: 发生时间
-- --------------------------
create 
(##customer :Customer {name:#customer,vlblk:#customer}),
(##product :Product {name:#product,vlblk:#product,price:##price}),
(##customer)-[e##transcode :Purchase {from:#customer,to:#product,elblk:"Purchase",
    transcode:#transcode,shop:#shop,quantity:##quantity,unit:#unit,price:##price,createtime:#createtime}]->(##product)

-- --------------------------
-- 销量统计
-- # summary
-- --------------------------
MATCH (a:Customer)-[e]->(b) 
WITH e.to as pct,e.quantity as quantity,e.price as price,e.quantity*e.price as amount 
RETURN pct,sum(amount)

-- --------------------------
-- 创建一个带有交易代码的关系
-- # createLine
-- #a: 节点a
-- #b: 节点b
-- #transcode: 交易代码
-- --------------------------
create 
(##a :Vertex {name:#a,vlblk:#a}),
(##b :Vertex {name:#b,vlblk:#b}),
(##a)-[e##transcode :Edge {from:#a,to:#b,elblk:"##a-##b"}]->(##b)


