-- --------------------------
-- # getAllTables
-- --------------------------
show tables;

-- --------------------------
-- # getUsers
-- #cnt
-- --------------------------
select * from t_user limit ##cnt

-- --------------------------
-- # updateUserById
-- #name
-- #id
-- --------------------------
update t_user set name=#name where id=##id

-- --------------------------
-- # removeUserById
-- #id
-- --------------------------
delete from t_user where id=##id

-- --------------------------
-- # getTablesOf
-- #schema : 细胞名称
-- --------------------------
select * from TABLES where TABLE_SCHEMA = #schema

-- --------------------------
-- # getTablesCount2
-- #schema : 细胞名称
-- --------------------------
select count(*) from TABLES where TABLE_SCHEMA = #schema

-- --------------------------
-- 订单的入账SQL,分录数据源
-- # getOrdersByCID
-- #bksys_id : 账册号码
-- #company_id : 公司号码
-- --------------------------
select * from -- 提取尚未入账的订单项目:是否入账由t_journal的记账对象objects标记 
    ( select objects from t_journal where bksys_id = ##bksys_id) j1 -- 提取账簿的记账对象信息
        right join ( select -- 概念映射,将订单概念转换为记账概念,这其实是业务场景变换即销售转会计
            a.id journal_id,  -- 订单id 映射为 记账对象id
            a.shipper partb,  -- 订单发货方 映射为 乙方
            a.receiver parta, -- 订单收货方 映射为 甲方 
            a.receive_address store, -- 订单收货地址 映射为 产品仓库 
            b.*, -- 订单行项目 保持 不变: id,name,quantity,price 
            a.create_time   -- 订单创建时间 映射为 交易发生时间
        from t_order a, json_table(a.details,'$.items[*]' columns ( -- 订单行项目,拆解json:details.items
            id int PATH '$.id', -- 产品id
            name varchar(256) PATH '$.name', -- 产品名称
            quantity double PATH '$.quantity', -- 产品数量
            price double PATH '$.price') -- 产品价格
        ) b where shipper = ##company_id or receiver = ##company_id ) a1 -- 提取范围:会计主体参与的订单
    on j1.objects = a1.journal_id where isnull(j1.objects) -- objects 为空表示尚未入账 

-- --------------------------
-- 计算的指定账册的试算平衡表
-- # trialBalance
-- #bksys_id : 账册号码
-- --------------------------
select c.account, bs.*  -- 账户(科目) 余额信息 
	from ( select acctnum, sum(dr) dr, sum(cr) cr, sum(balance) balance -- 账簿的科目余额信息
	    from ( select a.title, a.acctnum, -- 科目标题&科目编号
	            if(a.drcr = 1,amount,0) dr, -- 生成借方余额: 核算的本质就是用衍生的方式创造概念
	            if(a.drcr = -1,amount,0) cr, -- 生成贷方余额: 基础概念与衍生概念的关系就是核算的结构 
	            a.drcr*amount balance -- drcr标记,令借方余额为正,贷方余额为负.
	        from t_accts a inner join ( select * from t_journal 
	            where bksys_id = #bksys_id ) j on a.journal_id = j.id -- 分录关联账簿
	    ) t0 group by t0.acctnum ) -- 余额信息表
	bs left join t_coa c on substr(bs.acctnum,1,4) = c.acctnum -- 翻译科目编码
	order by bs.acctnum,c.account  -- 依据科目编号和账户名称进行排序
            
-- --------------------------
-- 计算的指定账册的试算平衡表(H2版本)
-- 因为h2没有if函数,故将其替换成casewhen
-- # trialBalanceForH2
-- #bksys_id : 账册号码
-- --------------------------
select c.account, bs.*  -- 账户(科目) 余额信息 
	from ( select acctnum, sum(dr) dr, sum(cr) cr, sum(balance) balance -- 账簿的科目余额信息
	    from ( select a.title, a.acctnum, -- 科目标题&科目编号
	            casewhen(a.drcr = 1,amount,0) dr, -- 生成借方余额: 核算的本质就是用衍生的方式创造概念
	            casewhen(a.drcr = -1,amount,0) cr, -- 生成贷方余额: 基础概念与衍生概念的关系就是核算的结构 
	            a.drcr*amount balance -- drcr标记,令借方余额为正,贷方余额为负.
	        from t_accts a inner join ( select * from t_journal 
	            where bksys_id = #bksys_id ) j on a.journal_id = j.id -- 分录关联账簿
	    ) t0 group by t0.acctnum ) -- 余额信息表
	bs left join t_coa c on substr(to_char(bs.acctnum),1,4) = c.acctnum -- 翻译科目编码
	order by bs.acctnum,c.account  -- 依据科目编号和账户名称进行排序

-- --------------------------
-- 插入订单数据
-- # addOrder
-- #name : 订单名称
-- #shipper : 乙方
-- #receiver : 甲方
-- --------------------------	
insert into t_order(name,shipper,receiver,receive_address,amount,details,create_time)
values (#name,#shipper,#receiver,#receive_address,#amount,#details,#create_time)
