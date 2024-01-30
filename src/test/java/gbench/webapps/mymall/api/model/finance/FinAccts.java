package gbench.webapps.mymall.api.model.finance;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.acct.core.FinAcct;
import gbench.webapps.mymall.api.model.finance.acct.core.AbstractAcct.Position;
import gbench.webapps.mymall.api.model.finance.acct.invt.Inventory;

/**
 * 财务会计记账方法
 */
public class FinAccts {

	/**
	 * 会计核算运行器：财务记账器
	 * 
	 * @param jdbcApp 记账凭证数据库
	 * @param fa      FinAcct 会计对象
	 * @return 获取会计核算运行器
	 */
	public static Consumer<Integer> executor_of(final IMySQL jdbcApp, final FinAcct fa) {
		return company_id -> jdbcApp.withTransaction(sess -> {
			println("平台数据表", sess.sql2dframe("show tables")); // 数据表
			final var cpdfm = sess.sql2dframe("select * from t_company_product") // 公司产品表
					.forEachBy(h2_json_processor("attrs")).rowS(e -> e.rec("attrs").derive(e)) // 解析属性字段
					.collect(DFrame.dfmclc);
			final var bldfm = sess.sql2dframe("#getBills", "company_id", company_id) // 读取指定的记账单据凭证信息
					.forEachBy(h2_json_processor("details"));
			final var whdfm = sess.sql2dframe("select * from t_warehouse"); // 仓库信息
			final var cydfm = sess.sql2dframe("select * from t_company"); // 公司信息
			final var oddfm = sess.sql2dframe("select * from t_order"); // 订单信息

			println("--------------------------------------------------------------------------");
			println("模拟公司", cydfm.one2one("id", company_id, "cy"));
			println("--------------------------------------------------------------------------");
			println("公司cydfm", cydfm.head(5));
			println("公司产品cpdfm", cpdfm.head(5));
			println("仓库whdfm", whdfm.head(5));

			final var linedfm = bldfm.rowS().flatMap(e -> e.dfm("details/items") // 记账单据凭证行项目:产品/资产明细
					.rowS(item -> item.alias(k -> switch (k) { // 字段改名
					case "id" -> "product_id"; // 公司产品id改为产品id
					default -> k; // 其他保持默认不变
					}).derive(e.filter("id,bill_type,position,warehouse_id")))).flatMap(acct_item_adjuster(fa))
					.collect(DFrame.dfmclc); // 数据行
			final var ledger = fa.getLedger(String.format("公司账【%s】", //
					cydfm.one2one("id", company_id, "cy").str("name"))); // 会计账簿

			println("单据凭证行项目linedfm", linedfm);

			// 单据凭证行项目日记账
			linedfm.rowS().forEach(line -> { // 依次处理各个单据凭证行项目
				final var bill_id = line.i4("id"); // 单据id
				final var bill_type = line.str("bill_type"); // 单据类型
				final var position = line.str("position"); // 交易的产品头寸
				final var product_id = line.i4("product_id"); // 产品id
				final var warehouse_id = line.i4("warehouse_id"); // 仓库id
				final var amount = line.dbl("price") * line.dbl("quantity"); // 交易金额
				final var path = String.format("%s/%s", bill_type, position); // 会计策略路径：单据类型/持有头寸
				final var mykeys = "bill_id,bill_type,product_id,warehouse_id"; // 会计凭证中需要写入会计分录的自定义字段名序列
				final var vars = REC("bill_id", bill_id, "bill_type", bill_type, "product_id", product_id,
						"warehouse_id", warehouse_id, "mykeys", mykeys); // 会计凭证中需要写入会计分录的自定义内容
				// 账目誊写:是根据记账策略(特定类型的记账凭证的记账法)，把会计凭证中的内容编制成会计分录
				ledger.handle(path, amount, vars); // 写入分类账：依据path所指定的记账测录，编制会计分录
			}); // linedfm.rowS().forEach

			fa.getEntrieS().forEach(post_entry_adjuster(company_id, cpdfm, whdfm, cydfm, oddfm)); // fa.getEntrieS().forEach
		}); // jdbcApp.withTransaction
	}

	/**
	 * 提交后的分录项目调整
	 * 
	 * @param acct_entity_id 记账主体id
	 * @param cpdfm
	 * @param whdfm
	 * @param cydfm
	 * @param oddfm
	 * @return 提交后的分录项目调整方法
	 */
	private static Consumer<? super IRecord> post_entry_adjuster(final int acct_entity_id, final DFrame cpdfm,
			final DFrame whdfm, final DFrame cydfm, final DFrame oddfm) {
		return entry -> { // 提交后调整
			final var bill_id = entry.i4("bill_id"); // 记账凭证id
			final var bill_type = entry.str("bill_type"); // 记账凭证类型
			final var product_id = entry.i4("product_id"); // 公司产品id
			final var warehouse_id = entry.i4("warehouse_id"); // 库房id
			final var cpopt = cpdfm.one2opt("id", product_id, "cp"); // 公司产品opt
			final var pcy_id = cpopt.map(e -> e.i4("company_id")).orElse(-1); // 产品公司id
			final var product = cpopt.map(e -> e.str("name")).orElse("-"); // 公司产品
			final var warehouse = whdfm.one2opt("id", warehouse_id, "wh").map(e -> e.str("name")).orElse("总库"); // 库房
			final var pcy = cydfm.one2opt("id", pcy_id, "cy2").map(e -> e.str("name")).orElse("无"); // 产品公司
			final int counterpart_id; // 对手方类型
			final Position position; // 单据头寸，从订单方向查看
			switch (bill_type) { // 根据记账凭证进行对应的分录字段补充
			case "t_order": { // t_order类型的交易单的字段处理
				position = oddfm.one2opt("id", bill_id, "od").map(od -> { // 订单对手方判断
					final var parta_id = od.i4("parta_id"); // 订单的甲方id:多头交易方,买方
					final var partb_id = od.i4("partb_id"); // 订单的乙方id:空头交易方,卖方
					if (Objects.equals(acct_entity_id, parta_id)) {
						return Position.LONG; // 交易头寸:多头
					} else if (Objects.equals(acct_entity_id, partb_id)) {
						return Position.SHORT; // 交易头寸:空头
					} else { // 非交易头寸
						return Position.NONE; // 非交易头寸
					} // 头寸判断
				}).orElse(Position.NONE); // 对手方id
				counterpart_id = oddfm.one2opt("id", bill_id, "od") //
						.map(od -> switch (position) { // 根据交易头寸判断交易对手方
						case LONG -> od.i4("partb_id"); // 多头的对手方为卖方
						case SHORT -> od.i4("parta_id"); // 空头的对手方为买
						default -> -1; // 非交易对手方
						}).orElse(-1); // 非交易对手方
				break;
			} // t_order类型的交易单的字段处理
			default: { // 各个字段的默认值
				counterpart_id = -1; // 非交易对手方
				position = Position.NONE; // 非交易头寸
			} // 默认处理
			} // 根据记账凭证进行对应的分录字段补充
			final var counterpart = cydfm.one2opt("id", counterpart_id, "cy2").map(e -> e.str("name")).orElse("无"); // 对手方

			entry.add("bill_id", bill_id, "bill_type", bill_type, "pcy_id", pcy_id, "pcy", pcy, "product", product,
					"warehouse", warehouse, "counterpart_id", counterpart_id, "counterpart", counterpart, "position",
					position); // 补充字段
		};
	}

	/**
	 * 凭证项目的记账调整
	 * 
	 * @param fa 会计对象
	 */
	private static Function<IRecord, Stream<IRecord>> acct_item_adjuster(final FinAcct fa) {
		return item -> { // 产品调整,根据需要会把一单分拆成多单
			final var position = item.str("position"); // 交易的产品头寸
			final var product_id = item.i4("product_id"); // 产品id
			switch (item.str("bill_type")) {
			case "invoice": { // 发货单中的产品项目的的处理
				switch (position) { // 交易头寸
				case "short": { // 空头持有发货凭证,按照货物的入库成本法进行数量核算
					final var rcps = fa.getEntrieS().filter(e -> e.i4("acctnum").equals(1406) // 库存商品
							&& e.i4("drcr").equals(1) // 入库产品
							&& e.i4("product_id").equals(product_id)).sorted(IRecord.cmp("time")).toList(); // 入库单
					final var ivcs = fa.getEntrieS().filter(e -> e.i4("acctnum").equals(1406) // 库存商品
							&& e.i4("drcr").equals(1) // 出库产品
							&& e.i4("product_id").equals(product_id)).sorted(IRecord.cmp("time")).toList(); // 入库单
					if (rcps.size() > 0) { // 发现库存产品
						final var receipts = rcps.stream().mapToDouble(e -> e.dbl("quantity")).toArray(); // 入库单
						final var invoices = Stream.concat(ivcs.stream().map(e -> e.dbl("quantity")), //
								Stream.of(item.dbl("quantity"))).mapToDouble(e -> e).toArray(); // 出库单
						final var lines = Inventory.correspondfm(receipts, invoices); // 生成发货方案
						println(String.format("发货方案,receipts:%s,invoices:%s,lines:%s", receipts, invoices, lines)); // 发货方案
						println("product_id#", product_id, rcps);
						return lines.rowS().flatMap(line -> { // 每个line代表一个发货单的发货方案
							// final var i_ivc = line.i4("id"); // 发票索引
							final var provides = line.llS("provides", IRecord::REC);
							final var lackS = line.i4opt("lacks").map(e -> Stream.of(item)).orElse(Stream.empty()); // 缺货
							final var provideS = provides.equals(Stream.empty()) // 缺货判定
									? Stream.of(item) //// provides 为空带表缺货
									: line.llS("provides", IRecord::REC).map(e -> {// i_ivc发货单需要拆分曾provides个给予核算
										final var i_rcp = e.i4("index"); // 收据索引
										// final var quantity = e.dbl("quantity"); // 对应的收货单数量
										final var receipt = rcps.get(i_rcp); // 提取对应额收货单
										return item.derive("price", receipt.dbl("price")); // 使用receipt中的产品介个
									}); // 对应于存货的发货方案
							return Stream.concat(lackS, provideS); // 缺货记录与库存计价进行一并处理
						}); // flatMap
					} // 发现库存产品
				} // short
				default: {
					// do nothing
				}
				} // switch position
			} // 发货成本的处理
			default: {
				// do nothing
			}
			} // switch bill_type

			return Stream.of(item);
		}; // item
	}

}
