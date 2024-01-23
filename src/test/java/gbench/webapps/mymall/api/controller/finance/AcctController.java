package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.Json.objM;
import static java.time.LocalDateTime.now;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.webapps.mymall.api.config.param.Param;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.acct.AbstractAcct.Position;
import gbench.webapps.mymall.api.model.finance.acct.FinAcct;
import gbench.webapps.mymall.api.model.finance.acct.FinAcctBuilder;
import gbench.util.jdbc.IMySQL;
import reactor.core.publisher.Mono;

/**
 * AcctController 会计核算
 */
@RestController
@RequestMapping("finance/acct")
public class AcctController {

	/**
	 * 模块信息 <br>
	 * <p>
	 * http://localhost:6010/finance/acct/info
	 *
	 * @return IRecord
	 */
	@RequestMapping("info")
	public Mono<IRecord> info() {
		return Mono.just(REC("code", "0", "data", //
				REC("db", jdbcApp.getDbName(), "time", now())));
	}

	/**
	 * 会计分录
	 * <p>
	 * http://localhost:6010/finance/acct/entries?company_ids=1
	 * 
	 * @return IRecord
	 */
	@RequestMapping("entries")
	public Mono<IRecord> entries(final @Param Integer[] company_ids) {
		final var fa = fabuilder.build("policy0001"); // 创建会计类型
		Stream.of(Optional.ofNullable(company_ids).orElseGet(() -> new Integer[] { 1, 2 })).forEach(executor_of(fa)); // 模拟各个公司的运行

		return Mono.just(REC("code", "0", "data", fa.getEntrieS().toList()));
	}

	/**
	 * 数据演示
	 * <p>
	 * http://localhost:6010/finance/acct/trial_balance?company_ids=1&keys=ledger_id,name,warehouse,product,drcr
	 * 
	 * @param company_ids 公司id集合
	 * @param keys        透视表的键值路径，阶层的组织顺序，可选字段为:<br>
	 *                    id:会计分录id,drcr:分录借贷标记,name:科目名称,amount:科目金额,ledger_id:分类账id,acctnum:科目编码
	 *                    ,bill_id:单据id,bill_type:单据类型,product_id:产品id,product:产品名称,warehouse_id:库房id,warehouse:库房名称
	 *                    ,pcy_id:产品公司id,pcy:产品公司,time:记账时间
	 * @return IRecord
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("trial_balance")
	public Mono<IRecord> trial_balance(final @Param Integer[] company_ids, final @Param String keys) {
		final var fa = fabuilder.build("policy0001"); // 创建会计类型
		Stream.of(Optional.ofNullable(company_ids).orElseGet(() -> new Integer[] { 1, 2 })).forEach(executor_of(fa)); // 模拟各个公司的运行
		final var _keys = Optional.ofNullable(keys).orElse("ledger_id,acctnum,warehouse,product,drcr").split(","); // 透视表键值列表
		final var json = fa.trialBalance(_keys).json( // 生成josn
				(sb, node) -> {
					final var d = node.attrvalOpt().orElse(0d);
					final String key = node.attr("key"); // 透视表索引路径
					final var name = node.getName();
					final var _name = switch (key) { // name 翻译
					case "drcr" -> switch (name) { // drcr 借贷标记 替换
					case "1" -> "DR";
					case "-1" -> "CR";
					default -> "";
					};
					default -> name;
					};
					return String.format("{\"name\":\"%s : %.2f\", \"value\":%f, \"children\":[", _name, d, d);
				}, //
				(sb, node) -> "]}"); // 生成json
		final var root = new HashMap<Object, Object>(); // 解析json生成根节点
		try {
			root.putAll(objM.readValue(json, Map.class)); // 保存解析数据
		} catch (Exception e) {
			e.printStackTrace();
		}
		final var all_keys = fa.getEntrieS().findFirst().map(IRecord::keys).orElse(new LinkedList<>());
		return Mono.just(REC("code", "0", "data", root, "all_keys", all_keys));
	}

	/**
	 * 会计核算运行器
	 * 
	 * @param fa FinAcct 会计对象
	 * @return 获取会计核算运行器
	 */
	private Consumer<Integer> executor_of(final FinAcct fa) {
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
					}).derive(e.filter("id,bill_type,position,warehouse_id")))).collect(DFrame.dfmclc); // 数据行
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
				final var path = String.format("%s/%s", bill_type, position); // 会计测录路径
				final var mykeys = "bill_id,bill_type,product_id,warehouse_id"; // 自定义属性的键名序列
				final var vars = REC("bill_id", bill_id, "bill_type", bill_type, "product_id", product_id,
						"warehouse_id", warehouse_id, "mykeys", mykeys);
				// 账目誊写
				ledger.handle(path, amount, vars); // 写入分类账
			}); // forEach

			fa.getEntrieS().forEach(entry -> { // 增加id转名字
				final var bill_id = entry.i4("bill_id"); // 记账凭证id
				final var bill_type = entry.str("bill_type"); // 记账凭证类型
				final var product_id = entry.i4("product_id"); // 公司产品id
				final var warehouse_id = entry.i4("warehouse_id"); // 库房id
				final var pcy_id = cpdfm.one2opt("id", product_id, "cp").map(e -> e.i4("company_id")).orElse(-1); // 产品公司id
				final var product = cpdfm.one2opt("id", product_id, "cp").map(e -> e.str("name")).orElse("-"); // 公司产品
				final var warehouse = whdfm.one2opt("id", warehouse_id, "wh").map(e -> e.str("name")).orElse("总库"); // 库房
				final var pcy = cydfm.one2opt("id", pcy_id, "cy2").map(e -> e.str("name")).orElse("无"); // 产品公司
				final int counterpart_id; // 对手方类型
				final Position position; // 单据头寸
				switch (bill_type) { // 根据记账凭证进行对应的分录字段补充
				case "t_order": { // t_order类型的交易单的字段处理
					position = oddfm.one2opt("id", bill_id, "od").map(od -> { // 订单对手方判断
						final var parta_id = od.i4("parta_id"); // 订单的甲方id:多头交易方,买方
						final var partb_id = od.i4("partb_id"); // 订单的乙方id:空头交易方,卖方
						if (Objects.equals(company_id, parta_id)) {
							return Position.LONG; // 交易头寸:多头
						} else if (Objects.equals(company_id, partb_id)) {
							return Position.SHORT; // 交易头寸:空头
						} else {
							return Position.NONE; // 非交易头寸
						}
					}).orElse(Position.NONE); // 对手方id
					counterpart_id = oddfm.one2opt("id", bill_id, "od") //
							.map(od -> switch (position) { // 根据交易头寸判断交易对手方
							case Position.LONG -> od.i4("partb_id"); // 多头的对手方为卖方
							case Position.SHORT -> od.i4("parta_id"); // 空头的对手方为买
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
						"warehouse", warehouse, "counterpart_id", counterpart_id, "counterpart", counterpart); // 补充字段
			}); // forEach
		});
	}

	@Autowired
	private IMySQL jdbcApp;

	@Autowired
	private FinAcctBuilder fabuilder;

}
