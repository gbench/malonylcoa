-- ------------------------------------------------------------------------------
-- 公司日常经济活动的各种记账凭证：是会计记账的基础与依据
-- 核算原理:根据指定的company_id即持有者id，通过其在经济业务中与订单,收发单,付款单之间的相互关系来确定会计科目的持有头寸
-- 由于会计策略的结构就是一个由策略名/科目持有头寸的路径结构，所以通过设计SQL查询语句的position字段标识就可以定位会计策略了。
-- position即持有者的科目持有头寸，或是记账凭证(单据)持有头寸是记账逻辑的关键字段,基本原则是实物产品立场,即从产品的立场/角度，
-- 持有产品(支付货币)为多头,交付产品(获得货币)为空头; 获得货币(交付产品)为空头,支付货币(获得产品)为多头。
-- 具体而言，各个单据的科目头寸定义如下：
-- 空头short:单据签发人(发票卖方partb签发,买方parta接收), 订单卖方(partb,订单乙方), 付款单收款人也就是订单卖方
-- 多头long:单据接收人(收据买方parta签发,卖方partb接收), 订单买方(parta,订单甲方), 付款单付款人也就是订单买方
-- 计算计算出产品粒度的会计科目(资产负债权益)分录的持有方式
-- # getBills
-- #company_id 公司id，即记账凭证(单据)的持有者
-- ------------------------------------------------------------------------------
select
*
from (
	select -- 订单的处理
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
	select -- 收发单的处理，其实单据的签发人issuer_id是函数依赖于bill_type的，是个冗余字段，是为了数据查阅方便才给予保留的。这就是为何此处没有issuer_id的原因。
		b1.bill_type,b1.position,b1.id,b1.details, -- 把内层产品收发凭证的字段信息暴露出来
		case b1.bill_type -- 根据收发货单据类型进行分别处理
		when 'invoice' then -- 发货单
			casewhen(b1.position='long' and f.id is not NULL, -- 收货方持有发货单且货运单有效
				f.shipping_to, -- 当多头持有invoice并且货运单有效,使用货运单的寄送地址作为warehouse_id
				b1.warehouse_id -- 默认为单据中的warehouse_id
			) --  发货单
		when 'receipt' then -- 收货单，进入收货环节，货运单必定有效
			casewhen(b1.position='long', -- 发货方持有收货单
				b1.warehouse_id, -- 发货方依旧保持原有的发货仓库不变
				f.shipping_to -- 默认为货运单中的
		) -- 收货单
		else b1.warehouse_id end warehouse_id -- 精准的仓库id
	from ( select -- 内层产品收发凭证
			b.bill_type, -- 凭证类型
			case bill_type -- 主体(issuer_id)持有凭证的头寸，依据bill_type进行分情形讨论
			when 'invoice' then casewhen(##company_id=o.partb_id, -- 主体(company_id)的持有发票的头寸，依据其是否是发货人而不同
				'short', -- 对于发货单，单据的空方issuer_id就是订单的partb_id，即乙方，发货人，卖方
				'long') -- 单据主体的issuer_id是订单乙方即发货人 签出发票 是空头，否则是多头
			when 'receipt' then casewhen(##company_id=o.parta_id, -- 主体(company_id)的持有收票的头寸，依据其是否是收货人而不同
				'short', -- 对于收货单，单据的空方issuer_id就是订单的parta_id，即甲方，收货人，买方
				'long') -- 单据主体的issuer_id订单甲方即收货人 签出收票 是空头，否则是多头
			else '-' end position, -- 头寸
			b.id, -- 收发单据id
			b.details, -- 产品明细
			b.warehouse_id, -- 仓库id,
			b.freight_order_id -- 暴露出货运单id
			from ( -- 单据类型是 收货单 或是 发货单
				select * from t_billof_product where bill_type in ('invoice', 'receipt')
			) b left join ( -- 单据主体的company_id 是 位于 是订单的甲方parta_id或是乙方partb_id
				select * from t_order where ##company_id in (parta_id, partb_id)
			) o on b.order_id=o.id 
	) b1 left join t_freight_order f -- 尝试通过货运单获取获取精确的仓库id
		on b1.freight_order_id = f.id -- 收发凭证关联货运单以便提取对应的精准warehouse_id
	union
	select -- 支付单的处理
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
