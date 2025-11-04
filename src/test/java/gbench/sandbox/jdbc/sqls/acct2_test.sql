-- ------------------------------------------------------------------------------
-- 公司日常经济活动的各种记账凭证：是会计记账的基础与依据
--
-- 核算原理:根据指定的company_id即当前的核算主体id亦即会计凭证的持有主体，通过其在经济业务中与订单,收发单,付款单之间
-- 的相互关系来确定会计科目的持有头寸。
-- 会计策略是一个由'单据类型/科目持有头寸'的名称路径，可通过编制select语句的position字段来实现会计策略与记账凭证的对应
-- position即核算主体的科目持有头寸是记账逻辑的主控字段,基本编制原则是以实物产品为立场,即是否获得产品的角度:交付空,持有多
-- 持有产品(支付货币)为多头,交付产品(获得货币)为空头; 获得货币(交付产品)为空头,支付货币(获得产品)为多头。
-- 具体而言，核算主体对待各个单据或者说记账凭证的持有科目头寸定义如下：
-- 空头short:发票的签发人partb,收据的接收人partb,订单卖方(partb,订单乙方), 付款单收款人也就是订单卖方partb
-- 多头long:发票的接收人parta,收据的签发人parta,订单买方(parta,订单甲方), 付款单付款人也就是订单买方parta
-- 简而言之,就是核算主体id若是订单的partb即卖方空头持有会计科目头寸,若是买方parta则多持有会计科目头寸。
--
-- 计算出产品粒度的会计科目(资产负债权益)分录的持有方式
-- 总账凭证 General Journal Vouchers
-- # GJVs
-- #company_id 核算主体id，本质是一个公司id这也是围城叫他为company_id的原因
-- ------------------------------------------------------------------------------
select
*
from (
	select -- 订单的处理
		't_order' bill_type, -- 凭证类型
		case ##company_id -- 核算主体(company_id)持有凭证的头寸，依据company_id的位置进行分情形讨论
		when partb_id then 'short' -- 核算主体是订单乙方 short空头持有
		when parta_id then 'long' -- 核算主体是订单甲方 long多头持有
		else '-' end position, -- 核算主体持有的会计科目头寸
		id, -- 订单id
		details, -- 产品明细
		-1 warehouse_id -- 仓库id,-1代表无效仓库
		from t_order where parta_id=##company_id or partb_id=##company_id
	union
	select -- 收发单的处理，其实单据的签发人issuer_id是函数依赖于bill_type的，是个冗余字段，是为了数据查阅方便才给予保留的。这就是为何此处没有issuer_id的原因。
		b1.bill_type,b1.position,b1.id,b1.details, -- 把内层产品收发凭证的字段信息暴露出来
		case b1.bill_type -- 根据收发货单据类型进行分别处理
		when 'invoice' then -- 发货单
			casewhen(b1.position='long' and f.id is not NULL, -- 核算主体为收货方
				f.shipping_to, -- 当多头持有invoice并且货运单有效,使用货运单的寄送地址作为warehouse_id,即收货仓库位置
				b1.warehouse_id -- 默认为单据中的warehouse_id,即发货仓库位置
			) --  发货单
		when 'receipt' then -- 收货单，进入收货环节，货运单必定有效
			casewhen(b1.position='short', -- 核算主体为发货方
				b1.warehouse_id, -- 发货方依旧保持原有的发货仓库不变,即发货仓库位置
				f.shipping_to -- 默认为货运单中的仓库id,即收货仓库位置
		) -- 收货单
		else b1.warehouse_id end warehouse_id -- 精准的仓库id
	from ( select -- 内层产品收发凭证
			b.bill_type, -- 凭证类型
			case bill_type -- 核算主体持有的会计记科目头寸，依据bill_type进行分情形讨论
			when 'invoice' then casewhen(##company_id=o.partb_id, -- 核算主体持有发票的会计科目的头寸，依据其是否是发货人而不同
				'short', -- 核算主体是订单乙方，发货人，卖方，空头持有会计科目头寸
				'long') -- 核算主体是订单甲方，收货人，买方，多头持有会计科目头寸
			when 'receipt' then casewhen(##company_id=o.parta_id, -- 核算主体持有的收据的会计科目的头寸，依据其是否是收货人而不同
				'long', -- 核算主体是订单甲方，收货人，买方，多头持有会计科目头寸
				'short') -- 核算主体是订单乙方，发货人，卖方，空头持有会计科目头寸
			else '-' end position, -- 核算主体持有的会计科目头寸
			b.id, -- 收发单据id
			b.details, -- 产品明细
			b.warehouse_id, -- 仓库id,
			b.freight_order_id -- 暴露出货运单id
			from ( -- 单据类型是 收货单 或是 发货单
				select * from t_billof_product where bill_type in ('invoice', 'receipt')
			) b right join ( -- 依据核算主体来筛选相应订单,确保核算主体要么是订单的甲方parta_id，要么是订单的乙方partb_id
				select * from t_order where ##company_id in (parta_id, partb_id)
			) o on b.order_id=o.id 
	) b1 left join t_freight_order f -- 尝试通过货运单获取获取精确的仓库id
		on b1.freight_order_id = f.id -- 收发凭证关联货运单以便提取对应的精准warehouse_id
	union
	select -- 支付单的处理
		't_payment' bill_type, -- 凭证类型
		case ##company_id -- 核算主体持有的会计科目头寸，依据核算主体(company_id)的位置来进行分情形讨论
		when payee_id then 'short' -- 核算主体是收款方 short空头持有
		when payer_id then 'long' --  核算主体是付款方 long多头持有
		else '-' end position, -- 核算主体持有的会计科目头寸
		id, -- 付款凭证id
		details, -- 产品明细
		-1 warehouse_id -- 仓库id,-1代表无效仓库
		from t_payment where payer_id=##company_id or payee_id=##company_id
) tbls where position in ('short', 'long') order by id
