package gbench.webapps.mymall.api.model.finance;

import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.Objects;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.acct.core.Ledger;
import gbench.webapps.mymall.api.model.finance.acct.core.AbstractAcct.Position;
import gbench.webapps.mymall.api.model.finance.acct.invt.Inventory;

/**
 * 财务会计记账方法
 */
public class FinAccts {

	/**
	 * 会计核算运行器：财务记账器
	 * 
	 * @param jdbcApp 业务数据库
	 * @param ledger  分类账对象
	 * @param jvs     记账凭证数据源 journal voucher sources,
	 *                通过指定不同的记账凭证源，就可以实现不同业务范围的账目核算了。
	 * @return 获取会计核算运行器
	 */
	public static Consumer<Long> executor_of(final IMySQL jdbcApp, final Ledger ledger, final String jvs) {
		return company_id -> jdbcApp.withTransaction(sess -> {
			println("平台数据表", sess.sql2dframe("show tables")); // 数据表
			final var cpdfm = sess.sql2dframe("select * from t_company_product") // 公司产品表
					.forEachBy(h2_json_processor("attrs")).rowS(e -> e.rec("attrs").derive(e)) // 解析属性字段
					.collect(DFrame.dfmclc);
			final var bldfm = sess.sql2dframe(jvs, "company_id", company_id) // 依据凭证源头，加载指定的记账凭证信息
					.forEachBy(h2_json_processor("details")); // 记账凭证&单据信息
			final var whdfm = sess.sql2dframe("select * from t_warehouse"); // 仓库信息
			final var cydfm = sess.sql2dframe("select * from t_company"); // 公司信息
			final var ord_sql = String.format("select * from t_order where %s in (parta_id, partb_id)", company_id); // 订单sql
			final var ordfm = sess.sql2dframe(ord_sql); // 订单信息

			println("--------------------------------------------------------------------------");
			println("财务记账主体", cydfm.one2one("id", company_id, "cy"));
			println("--------------------------------------------------------------------------");
			println("公司cydfm", cydfm.head(5));
			println("公司产品cpdfm", cpdfm.head(5));
			println("仓库whdfm", whdfm.head(5));
			println("订单ordfm", ordfm.head(5));

			final var linedfm = bldfm.rowS().flatMap(e -> e.dfm("details/items") // 记账凭证行项目:产品/资产明细
					.rowS(item -> item.alias(k -> switch (k) { // 字段改名
					case "id" -> "item_id"; // id改为item_id即公司产品id
					default -> k; // 其他保持默认不变
					}).derive(e.filter("id,bill_type,position,warehouse_id")))).collect(DFrame.dfmclc); // 数据行

			println("记账凭证行项目linedfm", linedfm);

			// 记账凭证行项目的按照账策略进行调整
			linedfm.rowS().map(line -> {// 为凭证航行项目补充product_id字段以便为记账凭证行项目调整器acct_item_adjuster处理
				final var item_id = line.i4("item_id"); // 公司产品id
				final var product_id = cpdfm.one2one("id", item_id, "cp").i4("product_id"); // 产品id
				return line.add("product_id", product_id); // 增加产品id字段
			}).flatMap(acct_item_adjuster(ledger)).forEach(line -> { // 依次处理各个记账凭证行项目
				final var bill_id = line.i4("id"); // 记账凭证&单据id
				final var bill_type = line.str("bill_type"); // 记账凭证&单据类型
				final var position = line.str("position"); // 交易的产品头寸
				final var item_id = line.i4("item_id"); // 公司产品id
				final var product_id = line.i4("product_id"); // 公司产品id
				final var warehouse_id = line.i4("warehouse_id"); // 仓库id
				final var quantity = line.dbl("quantity"); // item数量
				final var price = line.dbl("price"); // item单价
				final var amount = price * quantity; // *交易金额* 重点关注，这是科目金额的默认值
				final var path = String.format("%s/%s", bill_type, position); // 会计策略路径：记账凭证类型/持有头寸
				final var mykeys = "bill_id,bill_type,item_id,product_id,warehouse_id,price,quantity"; // 会计凭证中需要写入会计分录的自定义字段名序列
				final var vars = REC("bill_id", bill_id, "bill_type", bill_type, "item_id", item_id, "product_id",
						product_id, "warehouse_id", warehouse_id, "price", price, "quantity", quantity, "mykeys",
						mykeys); // 会计凭证中需要写入会计分录的自定义内容
				// 账目誊写:是根据记账策略(特定类型的记账凭证的记账法)，把会计凭证中的内容编制成会计分录
				ledger.handle(path, amount, vars); // 写入分类账：依据path所指定的记账测录，编制会计分录
			}); // linedfm.rowS().forEach

			ledger.getEntrieS().forEach(post_entry_adjuster(company_id, cpdfm, whdfm, cydfm, ordfm)); // 分类帐分录的最后补充&调整
		}); // jdbcApp.withTransaction
	}

	/**
	 * 提交后的分录项目调整
	 * 
	 * @param acct_entity_id 记账主体id
	 * @param cpdfm          公司产品信息
	 * @param whdfm          仓库信息
	 * @param cydfm          公司信息
	 * @param oddfm          订单信息
	 * @return 提交后的分录项目调整方法
	 */
	public static Consumer<? super IRecord> post_entry_adjuster(final long acct_entity_id, final DFrame cpdfm,
			final DFrame whdfm, final DFrame cydfm, final DFrame oddfm) {
		return entry -> { // 提交后调整
			final var bill_id = entry.i4("bill_id"); // 记账凭证id
			final var bill_type = entry.str("bill_type"); // 记账凭证类型
			final var item_id = entry.i4("item_id"); // 公司产品id,记账凭证行项目的核算对象
			final var itopt = cpdfm.one2opt("id", item_id, "it"); // 公司产品opt,记账凭证行项目的核算对象opt
			final var item = itopt.map(e -> e.str("name")).orElse("-"); // 公司产品名称,记账凭证行项目的核算对象名称，是自定义公司产品名称,一般或是默认采用通用的产品名称
			final var pcy_id = itopt.map(e -> e.i4("company_id")).orElse(-1); // 产品公司id
			final var pcy = cydfm.one2opt("id", pcy_id, "cy2").map(e -> e.str("name")).orElse("无"); // 产品公司
			final var product_id = itopt.map(e -> e.i4("product_id")).orElse(-1); // 公司产品id
			final var warehouse_id = entry.i4("warehouse_id"); // 库房id
			final var warehouse = whdfm.one2opt("id", warehouse_id, "wh").map(e -> e.str("name")).orElse("总库"); // 库房名称

			final int counterpart_id; // 对手方类型
			final Position position; // 记账凭证&单据头寸，从订单方向查看
			switch (bill_type) { // 根据记账凭证进行对应的分录字段补充
			case "t_order": { // t_order类型的交易单的字段处理
				position = oddfm.one2opt("id", bill_id, "od").map(od -> { // 订单对手方判断
					final var parta_id = od.lng("parta_id"); // 订单的甲方id:多头交易方,买方
					final var partb_id = od.lng("partb_id"); // 订单的乙方id:空头交易方,卖方
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

			entry.add("bill_id", bill_id, "bill_type", bill_type, "pcy_id", pcy_id, "pcy", pcy, "product_id",
					product_id, "item", item, "warehouse", warehouse, "counterpart_id", counterpart_id, "counterpart",
					counterpart, "position", position); // 补充字段
		}; // entry
	}

	/**
	 * 记账凭证行项目即产品行的记账调整（以符合会计策略的编制要求）
	 * 
	 * @param ledger 分类账对象
	 */
	public static Function<IRecord, Stream<IRecord>> acct_item_adjuster(final Ledger ledger) {
		return item -> { // 产品调整,根据需要会把一单分拆成多单
			final var position = item.str("position"); // 交易的产品头寸
			final var bill_type = item.str("bill_type"); // 记账凭证&单据类型

			return switch (bill_type) { // bill_type/position勾构成了会计策略的定位路径
			// 收发货单的处理
			case "invoice", "receipt" -> switch (position) { // 交易头寸
			// 空头方的交易头寸的处理，对于空头持有的该种记账凭证(invoice,receipt)的会计主体acct_entity而言，需要采用历史成本策略来调整记账凭证行项目item的price和quantity,
			// 这里涉及记账凭证行项目item由于数量巨大需要多个入库存单checkin来进行合并才能满足发货要求,以及一个入库单checkin数量巨大,可以满足多个出库单checkout的记账凭证行项目的情况。
			// 但具体对checkout而则只需要考虑是否合并多个checkin的问题(checkin的拆分与合并问题可交由存货算法的来匹配chekout)，所以这里这里的checkout需要与至少一个checkin进行关联。
			case "short" -> bill_short_fifo_handler(ledger, item);
			default -> Stream.of(item); // 默认处理
			}; // switch position

			// 发货成本的处理
			default -> Stream.of(item); // 默认处理
			}; // switch bill_type
		}; // items
	}

	/**
	 * 出入库票据的空头方的price和quantity调整(先入先出算法) <br>
	 * 凭证项目调整:以便适配会计策略的算法 <br>
	 * 空头持有发货凭证,按照货物的入库成本法进行数量核算:原理就是分析item成一组新的items,每个item具有更为精确的quantiy和price
	 * 
	 * @param ledger 分类账对象
	 * @param item   发货单 凭证项目
	 * @return 调整后的记账凭证行项目(在一次在发货checkout中,根据成本核算模式要求(fifo),将与发货单chekout即记账凭证行项目相对应的一系列库存单拆分成多组具有不同成本结构的记账凭证行项目）
	 */
	public static Stream<IRecord> bill_short_fifo_handler(final Ledger ledger, final IRecord item) {
		return bill_short_handler(ledger, item, IRecord.cmp("time", true));
	}

	/**
	 * 出入库票据的空头方的price和quantity调整(后入先出算法) <br>
	 * 凭证项目调整:以便适配会计策略的算法 <br>
	 * 空头持有发货凭证,按照货物的入库成本法进行数量核算:原理就是分析item成一组新的items,每个item具有更为精确的quantiy和price
	 * 
	 * @param ledger 分类账对象
	 * @param item   发货单 凭证项目
	 * @return 调整后的记账凭证行项目(在一次在发货checkout中,根据成本核算模式要求(lifo),将与发货单chekout即记账凭证行项目相对应的一系列库存单拆分成多组具有不同成本结构的记账凭证行项目）
	 */
	public static Stream<IRecord> bill_short_lifo_handler(final Ledger ledger, final IRecord item) {
		return bill_short_handler(ledger, item, IRecord.cmp("time", false));
	}

	/**
	 * 出入库票据的空头方的price和quantity调整(先入先出算法) <br>
	 * 凭证项目调整:以便适配会计策略的算法 <br>
	 * 空头持有发货凭证,按照货物的入库成本法进行数量核算:原理就是分析item成一组新的items,每个item具有更为精确的quantiy和price
	 * 
	 * @param ledger      分类账对象
	 * @param item        发货单 凭证项目
	 * @param cost_method 成本计算方法:比如：<br>
	 *                    IRecord.cmp("time", true)为lifo模式, <br>
	 *                    IRecord.cmp("time", false)为lifo模式，<br>
	 *                    当然还可自行定义会计分录的排序方法来进行成本核算方法的定制.
	 * @return 调整后的记账凭证行项目(在一次在发货checkout中,根据成本核算模式要求(cost_method),将与发货单chekout即记账凭证行项目相对应的一系列库存单拆分成多组具有不同成本结构的记账凭证行项目）
	 */
	public static Stream<IRecord> bill_short_handler(final Ledger ledger, final IRecord item,
			final Comparator<? super IRecord> cost_method) {
		final var bill_type = item.str("bill_type").toUpperCase(); // 记账凭证&单据类型
		final var product_id = item.i4("product_id"); // 产品id,注意这里使用的是产品id而不是公司产品id
		final var acctnum = switch (bill_type) { // 根据记账凭证&单据类型选择具体的核算科目
		case "INVOICE" -> 1406L;// 库存商品
		case "RECEIPT" -> 1407L; // 发出商品
		default -> null;
		};

		println("====================================================================");
		println(String.format("SHORT FOR %s:%s#%s [ %s ]", bill_type, acctnum, ACCTS.get(acctnum + ""), item));
		println("====================================================================");

		final var entrydfm = ledger.getEntrieS() // 财务记账的会计分录
				.filter(e -> e.i4("product_id").equals(product_id)) // *指定产品的会计分录*,注意这里使用的通用产品product_id,而不是item_id,以保证不同公司产品的checkin可以混合发货。
				.sorted(cost_method) // 根据成本核算方法所执行的顺序进行编排会计分录
				.collect(DFrame.dfmclc); // 满足记账策略算法编制要求的会计分录表,即本次会计凭证行项目的涉及分录范围(借方分录集checkins与贷方分录集checkouts),以便后续根据借贷标记将其进行分解
		println(String.format("entrydfm#\n%s", entrydfm));
		final var checkindfm = entrydfm.rowS().filter(e -> e.lng("acctnum").equals(acctnum) && e.i4("drcr").equals(1)) // 核算科目账户的借方余额表示流入
				.collect(DFrame.dfmclc); // 分解出checkins,核算科目借方余额-流入
		println(String.format("checkindfm#\n%s", checkindfm));
		final var checkoutdfm = entrydfm.rowS().filter(e -> e.lng("acctnum").equals(acctnum) && e.i4("drcr").equals(-1)) // 核算科目账户的贷方余额表示流出
				.collect(DFrame.dfmclc); // 分解出checkouts,核算科目贷方余额-流出
		println(String.format("checkoutdfm#\n%s", checkoutdfm));

		if (checkindfm.length() > 0) { // 发现库存产品,根据库存产品进行发货凭证中的quantity与price调整。即调整记账凭证行项目。
			final Function<DFrame, double[]> todbls = dfm -> dfm.rowS().mapToDouble(e -> e.dbl("quantity")).toArray(); // 转换成double[]
			final var checkins = todbls.apply(checkindfm); // 流入向checkin数量单
			final var checkouts = Arrays.copyOf(todbls.apply(checkoutdfm), checkoutdfm.length() + 1); // 流出向checkout数量单,加1是为当前的凭证行项目item做空间预留。
			checkouts[checkouts.length - 1] = item.dbl("quantity"); // 最后一项使用当前凭证项目的数量,这就是逐次累计的实现逻辑，始终在最后追加。
			println(String.format("checkins:%s\ncheckouts:%s", nd(checkins), nd(checkouts)));

			final var linedfm = Inventory.correspondfm(checkins, checkouts); // 根据checkins为checkouts生成发货方案
			final var checkout_index = checkouts.length - 1; // 记账凭证行项目item对应的发货单的发货计划,也就是checkouts的最后一项的偏移/索引位置
			final var items_adjusted = linedfm.rowS().filter(e -> e.i4("index").equals(checkout_index)) // 提取记账凭证行项目item对应发货方案，然后计算其发货成本(quantiti&price)
					.flatMap(line -> { // 每个line代表一个发货单的发货方案,每个记账项目可以由一组缺货数量lacks_items与可供应数量provides_items来进行表示
						final var provides = line.llS("provides", IRecord::REC); // 可供应数量
						final var lacks_items = line.dblopt("lacks") // 缺货项目数量
								.map(lacks -> Arrays.asList(item.derive("quantity", lacks))) // 创建一条与当前行项目即price为市场item价格且quantity数量为缺货lacks大小的调整调整出库行项目
								.orElseGet(LinkedList::new); // 缺货项目按照订单价格与缺货数量进行发货
						final var provides_items = provides.equals(Stream.empty()) // 缺货判定
								? Arrays.asList(item) // provides为空代表缺货
								: line.llS("provides", IRecord::REC).map(e -> {// 发货单需要拆分曾provides个给予核算
									final var checkin_index = e.i4("index"); // 收据索引
									final var checkin = checkindfm.row(checkin_index); // 提取对应的收货单
									final var quantity = e.dbl("quantity"); // 根据发货方案安排下的对应的收货单中的发货数量
									final var price = checkin.dbl("price"); // *入库的历史成本价格*重点关注这就是可以逐个产品项的按照历史成本进行计价的原理。因为将每个出库项目对应&定位到具体的入库单
									return item.derive("quantity", quantity, "price", price); // 使用存货产品数量进行发货
								}).toList(); // 对应于可供库存发货的发货方案
						final var plan_items = new LinkedList<IRecord>(); // 最终的整体发货方案

						plan_items.addAll(lacks_items); // 缺失部分：转单或是市价购入等，具体业务如何实现这里不关注，这里仅仅是给予核算
						plan_items.addAll(provides_items); // 可供应部分
						println(String.format("----\n发货方案#%s\n%s", checkout_index, item));
						REC("lacks_items", lacks_items, "provides_items", provides_items).forEach(p -> { // 发出方案的打印
							@SuppressWarnings("unchecked")
							final var rows = (List<IRecord>) p._2();
							if (rows.size() > 0) { // 仅打印含有内容部分
								println(String.format("%s:\n%s", p._1(), DFrame.of(rows)));
							} // if
						}); // forEach

						return plan_items.stream(); // 转换成数据流的形式予以返回。
					}); // flatMap

			return items_adjusted; // 调整后的记账凭证行项目
		} else { // 没有相应的库存产品,则保持原理的记账凭证不变,即不做任何调整
			return Stream.of(item);
		} // if 发现库存产品，调整记账凭证行项目。
	}

	final static IRecord ACCTS = REC(1406, "库存商品", 1407L, "发出商品"); // 分录编号字典

}
