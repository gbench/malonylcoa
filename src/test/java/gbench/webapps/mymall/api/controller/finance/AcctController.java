package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static java.time.LocalDateTime.now;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.webapps.mymall.api.config.param.Param;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Json;
import gbench.webapps.mymall.api.model.finance.acct.FinAcct;
import gbench.webapps.mymall.api.model.finance.acct.FinAcctBuilder;
import gbench.util.jdbc.IMySQL;
import reactor.core.publisher.Mono;

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
	 * 数据演示
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
			root.putAll(Json.objM.readValue(json, Map.class)); // 保存解析数据
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Mono.just(REC("code", "0", "data", root));
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
				final var bill_type = line.str("bill_type"); // 订单类型
				final var position = line.str("position"); // 交易的产品头寸
				final var product_id = line.i4("product_id"); // 产品id
				final var warehouse_id = line.i4("warehouse_id"); // 仓库id
				final var amount = line.dbl("price") * line.dbl("quantity"); // 交易金额
				final var path = String.format("%s/%s", bill_type, position); // 会计测录路径
				final var mykeys = "bill_type,product_id,warehouse_id"; // 自定义属性的键名序列
				final var vars = REC("bill_type", bill_type, "product_id", product_id, //
						"warehouse_id", warehouse_id, "mykeys", mykeys);
				// 账目誊写
				ledger.handle(path, amount, vars); // 写入分类账
			}); // forEach

			fa.getEntrieS().forEach(entry -> { // 增加id转名字
				final var product_id = entry.i4("product_id");
				final var warehouse_id = entry.i4("warehouse_id");
				final var product = cpdfm.one2opt("id", product_id, "cp").map(e -> e.str("name")).orElse("-");
				final var warehouse = whdfm.one2opt("id", warehouse_id, "wh").map(e -> e.str("name")).orElse("总库");
				entry.add("product", product, "warehouse", warehouse);
			}); // forEach
		});
	}

	@Autowired
	private IMySQL jdbcApp;

	@Autowired
	private FinAcctBuilder fabuilder;

}
