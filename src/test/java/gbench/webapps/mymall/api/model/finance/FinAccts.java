package gbench.webapps.mymall.api.model.finance;

import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.Objects;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
					case "id" -> "item_id"; // id改为item_id即公司产品id
					default -> k; // 其他保持默认不变
					}).derive(e.filter("id,bill_type,position,warehouse_id")))).collect(DFrame.dfmclc); // 数据行
			final var ledger = fa.getLedger(String.format("公司账【%s】", //
					cydfm.one2one("id", company_id, "cy").str("name"))); // 会计账簿

			println("单据凭证行项目linedfm", linedfm);

			// 单据凭证行项目日记账
			linedfm.rowS().map(line -> {
				final var item_id = line.i4("item_id"); // 公司产品id
				final var product_id = cpdfm.one2one("id", item_id, "cp").i4("product_id"); // 产品id
				return line.add("product_id", product_id); // 增加产品id字段
			}).flatMap(acct_item_adjuster(fa)).forEach(line -> { // 依次处理各个单据凭证行项目
				final var bill_id = line.i4("id"); // 单据id
				final var bill_type = line.str("bill_type"); // 单据类型
				final var position = line.str("position"); // 交易的产品头寸
				final var item_id = line.i4("item_id"); // 公司产品id
				final var product_id = line.i4("product_id"); // 公司产品id
				final var warehouse_id = line.i4("warehouse_id"); // 仓库id
				final var quantity = line.dbl("quantity"); // 数量
				final var price = line.dbl("price"); // 单价
				final var amount = price * quantity; // 交易金额
				final var path = String.format("%s/%s", bill_type, position); // 会计策略路径：单据类型/持有头寸
				final var mykeys = "bill_id,bill_type,item_id,product_id,warehouse_id,price,quantity"; // 会计凭证中需要写入会计分录的自定义字段名序列
				final var vars = REC("bill_id", bill_id, "bill_type", bill_type, "item_id", item_id, "product_id",
						product_id, "warehouse_id", warehouse_id, "price", price, "quantity", quantity, "mykeys",
						mykeys); // 会计凭证中需要写入会计分录的自定义内容
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
	 * @param cpdfm          公司产品信息
	 * @param whdfm          仓库信息
	 * @param cydfm          公司信息
	 * @param oddfm          订单信息
	 * @return 提交后的分录项目调整方法
	 */
	public static Consumer<? super IRecord> post_entry_adjuster(final int acct_entity_id, final DFrame cpdfm,
			final DFrame whdfm, final DFrame cydfm, final DFrame oddfm) {
		return entry -> { // 提交后调整
			final var bill_id = entry.i4("bill_id"); // 记账凭证id
			final var bill_type = entry.str("bill_type"); // 记账凭证类型
			final var item_id = entry.i4("item_id"); // 公司产品id
			final var warehouse_id = entry.i4("warehouse_id"); // 库房id
			final var cpopt = cpdfm.one2opt("id", item_id, "cp1"); // 公司产品opt
			final var pcy_id = cpopt.map(e -> e.i4("company_id")).orElse(-1); // 产品公司id
			final var product = cpopt.map(e -> e.str("name")).orElse("-"); // 公司产品
			final var product_id = cpopt.map(e -> e.i4("product_id")).orElse(-1); // 公司产品id
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

			entry.add("bill_id", bill_id, "bill_type", bill_type, "pcy_id", pcy_id, "pcy", pcy, "product_id",
					product_id, "product", product, "warehouse", warehouse, "counterpart_id", counterpart_id,
					"counterpart", counterpart, "position", position); // 补充字段
		};
	}

	/**
	 * 凭证项目的记账调整
	 * 
	 * @param fa 会计对象
	 */
	public static Function<IRecord, Stream<IRecord>> acct_item_adjuster(final FinAcct fa) {
		return item -> { // 产品调整,根据需要会把一单分拆成多单
			final var position = item.str("position"); // 交易的产品头寸
			final var bill_type = item.str("bill_type"); // 单据类型

			return switch (bill_type) { // bill_type/position勾构成了会计策略的定位路径
			// 收发货单的处理
			case "invoice", "receipt" -> switch (position) { // 交易头寸
			// 空头方的交易头寸的处理
			case "short" -> bill_short_fifo_handler(fa, item);
			default -> Stream.of(item); // 默认处理
			}; // switch position

			// 发货成本的处理
			default -> Stream.of(item); // 默认处理
			};
		};
	}

	/**
	 * 出入库票据的空头方的price和quantity调整(先入先出算法) <br>
	 * 凭证项目调整:以便适配会计策略的算法 <br>
	 * 空头持有发货凭证,按照货物的入库成本法进行数量核算:原理就是分析item成一组新的items,每个item具有更为精确的quantiy和price
	 * 
	 * @param fa   会计对象
	 * @param item 发货单 凭证项目
	 * @return 调整以后记账凭证项目(往往是一次发货或会根据成本核算模式要求拆分成多组不同成本构成的记账凭证项目）
	 */
	public static Stream<IRecord> bill_short_fifo_handler(final FinAcct fa, IRecord item) {
		return bill_short_handler(fa, item, true);
	}

	/**
	 * 出入库票据的空头方的price和quantity调整(先入先出算法) <br>
	 * 凭证项目调整:以便适配会计策略的算法 <br>
	 * 空头持有发货凭证,按照货物的入库成本法进行数量核算:原理就是分析item成一组新的items,每个item具有更为精确的quantiy和price
	 * 
	 * @param fa   会计对象
	 * @param item 发货单 凭证项目
	 * @return 调整以后记账凭证项目(往往是一次发货或会根据成本核算模式要求拆分成多组不同成本构成的记账凭证项目）
	 */
	public static Stream<IRecord> bill_short_lifo_handler(final FinAcct fa, IRecord item) {
		return bill_short_handler(fa, item, false);
	}

	/**
	 * 出入库票据的空头方的price和quantity调整(先入先出算法) <br>
	 * 凭证项目调整:以便适配会计策略的算法 <br>
	 * 空头持有发货凭证,按照货物的入库成本法进行数量核算:原理就是分析item成一组新的items,每个item具有更为精确的quantiy和price
	 * 
	 * @param fa   会计对象
	 * @param item 发货单 凭证项目
	 * @param flag 是否采用fifo模式来核算成本,true:filo模式,false:lifo模式
	 * @return 调整以后记账凭证项目(往往是一次发货或会根据成本核算模式要求拆分成多组不同成本构成的记账凭证项目）
	 */
	public static Stream<IRecord> bill_short_handler(final FinAcct fa, IRecord item, final boolean flag) {
		final var bill_type = item.str("bill_type").toUpperCase(); // 单据类型
		final var product_id = item.i4("product_id"); // 产品id,注意这里使用的是产品id而不是公司产品id
		final var acctnum = switch (bill_type) { // 根据单据类型选择具体的核算科目
		case "INVOICE" -> 1406L;// 库存商品
		case "RECEIPT" -> 1407L; // 发出商品
		default -> null;
		};

		println("====================================================================");
		println(String.format("SHORT FOR %s:%s#%s [ %s ]", bill_type, acctnum, ACCTS.get(acctnum + ""), item));
		println("====================================================================");

		final var entrydfm = fa.getEntrieS() // 会计科目
				.filter(e -> e.i4("product_id").equals(product_id)).sorted(IRecord.cmp("time", flag)) // 入库时间进行排序
				.collect(DFrame.dfmclc);
		println(String.format("entrydfm#\n%s", entrydfm));
		final var checkindfm = entrydfm.rowS() // 库存商品借方分录
				.filter(e -> e.lng("acctnum").equals(acctnum) && e.i4("drcr").equals(1)).collect(DFrame.dfmclc); // 借方余额
		println(String.format("entrydfm#\n%s", checkindfm));
		final var checkoutdfm = entrydfm.rowS() // 库存商品贷方分录
				.filter(e -> e.lng("acctnum").equals(acctnum) && e.i4("drcr").equals(-1)).collect(DFrame.dfmclc); // 贷方余额
		println(String.format("crdfm#\n%s", checkoutdfm));

		if (checkindfm.size() > 0) { // 发现库存产品
			final Function<DFrame, double[]> todbls = dfm -> dfm.rowS().mapToDouble(e -> e.dbl("quantity")).toArray();
			final var checkins = todbls.apply(checkindfm); // 入库单:checkin方向
			final var checkouts = Arrays.copyOf(todbls.apply(checkoutdfm), checkoutdfm.height() + 1); // 出库单:checkout方向
			checkouts[checkouts.length - 1] = item.dbl("quantity"); // 最后一项使用当前凭证项目的数量,这就是逐次累计的实现逻辑，始终在最后追加
			println(String.format("checkins:%s\ncheckouts:%s", nd(checkins), nd(checkouts)));

			final var lines = Inventory.correspondfm(checkins, checkouts); // 生成发货方案
			final var checkout_index = checkouts.length - 1; // item 对应的发货单的发货计划
			final var items_adjusted = lines.rowS().filter(e -> e.i4("index").equals(checkout_index)).flatMap(line -> { // 每个line代表一个发货单的发货方案
				final var provides = line.llS("provides", IRecord::REC); // 可供应数量
				final var lacks_items = line.i4opt("lacks") // 缺货项目数量
						.map(lacks -> Arrays.asList(item.derive("quantity", lacks))) //
						.orElseGet(LinkedList::new); // 缺货项目按照订单价格与缺货数量进行发货
				final var provides_items = provides.equals(Stream.empty()) // 缺货判定
						? Arrays.asList(item) // provides 为空带表缺货
						: line.llS("provides", IRecord::REC).map(e -> {// i_ivc发货单需要拆分曾provides个给予核算
							final var checkin_index = e.i4("index"); // 收据索引
							final var quantity = e.dbl("quantity"); // 对应的收货单数量
							final var checkin = checkindfm.row(checkin_index); // 提取对应额收货单
							return item.derive("quantity", quantity, "price", checkin.dbl("price")); // 使用存货产品数量进行发货
						}).toList(); // 对应于存货的发货方案
				final var plan_items = new LinkedList<IRecord>(); // 发货方案

				plan_items.addAll(provides_items); // 可提供部分
				plan_items.addAll(lacks_items); // 缺失部分
				println(String.format("----\n发货方案#%s\n%s", checkout_index, item));
				REC("lacks_items", lacks_items, "provides_items", provides_items).forEach(p -> { // 发出方案的打印
					@SuppressWarnings("unchecked")
					final var rows = (List<IRecord>) p._2();
					if (rows.size() > 0) {
						println(String.format("%s:\n%s", p._1(), DFrame.of(rows)));
					} // if
				}); // forEach

				return plan_items.stream();
			}); // flatMap

			return items_adjusted; // 调整后的凭证项目
		} else { // if 发现库存产品
			return Stream.of(item);
		}
	}

	final static IRecord ACCTS = REC(1406, "库存商品", 1407L, "发出商品"); // 分录编号字典

}
