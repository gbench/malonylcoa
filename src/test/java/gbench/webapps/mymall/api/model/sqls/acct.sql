-- --------------------------
-- 公司日常经济活动的各种记账凭证
-- # getBills
-- #company_id 公司id
-- --------------------------
select
*
from (
	select 
		't_order' bill_type, -- 凭证类型
		case ##company_id -- 主体(company_id)持有凭证的头寸，依据company_id的位置进行分情形讨论
		when partb_id then 'short' -- 乙方是short
		when parta_id then 'long' -- 甲方是long
		else '-' end position, -- 头寸
		id, -- 订单id
		details, -- 产品明细
		-1 warehouse_id -- 仓库id
		from t_order where parta_id=##company_id or partb_id=##company_id
	union
	select
		b.bill_type, -- 凭证类型
		case bill_type -- 主体(company_id)持有凭证的头寸，依据bill_type进行分情形讨论
		when 'invoice' then casewhen(##company_id=o.partb_id, -- 主体(company_id)的持有发票的头寸，依据其是否是发货人而不同
			'short', 'long') -- 单据主体的company_id是订单乙方即发货人 签出发票 是空头，否则是多头
		when 'receipt' then casewhen(##company_id=o.parta_id, -- 主体(company_id)的持有收票的头寸，依据其是否是收货人而不同
			'short', 'long') -- 单据主体的company_id订单甲方即收货人 签出收票 是空头，否则是多头
		else '-' end position, -- 头寸
		b.id, -- 收发单据id
		b.details, -- 产品明细
		b.warehouse_id -- 仓库id
		from ( -- 单据类型是 收货单 或是 发货单
			select * from t_billof_product where bill_type in ('invoice', 'receipt')
		) b left join ( -- 单据主体的company_id 是 位于 是订单的甲方parta_id或是乙方partb_id
			select * from t_order where ##company_id in (parta_id, partb_id)
		) o on b.order_id=o.id 
	union
	select
		't_payment' bill_type, -- 凭证类型
		case ##company_id -- 主体(company_id)持有凭证的头寸，，依据company_id的位置进行分情形讨论
		when payee_id then 'short' -- 收款方 short
		when payer_id then 'long' -- 付款方 long
		else '-' end position, -- 头寸
		id, -- 付款凭证id
		details, -- 产品明细
		-1 warehouse_id, -- 仓库id
		from t_payment where payer_id=##company_id or payee_id=##company_id
) tbls where position in ('short', 'long') order by id

