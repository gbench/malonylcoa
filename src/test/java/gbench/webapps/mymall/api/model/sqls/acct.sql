-- --------------------------
-- 公司日常经济活动的各种记账凭证
-- # getBills
-- #company_id 公司id
-- --------------------------
select
*
from (
	select 
		't_order' bill_type,
		case ##company_id
		when partb_id then 'short' -- 乙方是short
		when parta_id then 'long' -- 甲方是long
		else '-'
		end position, -- 头寸
		id, -- 订单id
		details, -- 产品明细
		-1 warehouse_id -- 仓库id
		from t_order where parta_id=##company_id or partb_id=##company_id
	union
	select
		't_billof_product' bill_type,
		case bill_type
		when 'invoice' then 'short' -- 发货方是short
		when 'receipt' then 'long' -- 收货方是long
		else '-'
		end position, -- 头寸
		id, -- 收发单据id
		details, -- 产品明细
		warehouse_id -- 仓库id
		from t_billof_product  where company_id=##company_id
	union
	select
		't_payment' bill_type,
		case ##company_id
		when payee_id then 'short' -- 收款方 short
		when payer_id then 'long' -- 付款方 long
		else '-'
		end position, -- 头寸
		id, -- 付款凭证id
		details, -- 产品明细
		-1 warehouse_id, -- 仓库id
		from t_payment where payer_id=##company_id or payee_id=##company_id
) tbls where position in ('short', 'long')

