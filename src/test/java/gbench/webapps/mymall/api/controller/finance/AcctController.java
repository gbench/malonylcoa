package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.Json.objM;
import static gbench.webapps.mymall.api.model.finance.FinAccts.executor_of;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.webapps.mymall.api.config.param.Param;
import gbench.webapps.mymall.api.model.finance.acct.core.FinAcctBuilder;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.type.Types;
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
	 * @return IRecord {code:结果状态标记0表示成功,data:分录列表数组}
	 */
	@RequestMapping("entries")
	public Mono<IRecord> entries(final @Param Long[] company_ids) {
		final var fa = fabuilder.add("policy", "policy0001").build(); // 创建会计类型
		final var ledger = fa.getLedger(this.glid(company_ids)); // 总账
		Stream.of(Optional.ofNullable(company_ids).orElseGet(() -> new Long[] { 1l, 2l }))
				.forEach(executor_of(jdbcApp, ledger, "#GJVs")); // 模拟各个公司的运行

		return Mono.just(REC("code", "0", "data", fa.getEntrieS().toList()));
	}

	/**
	 * 数据演示
	 * <p>
	 * http://localhost:6010/finance/acct/trial_balance?company_ids=1&keys=ledger_id,name,warehouse,item,drcr
	 * 
	 * @param company_ids 公司id集合
	 * @param keys        透视表的键值路径，阶层的组织顺序，可选字段为:<br>
	 *                    id:会计分录id,drcr:分录借贷标记,name:科目名称,amount:科目金额即单价与数量的乘积,ledger_id:分类账id,acctnum:科目编码
	 *                    ,bill_id:单据id,bill_type:单据类型,product_id:产品id,item_id:公司产品id,item:公司产品名称,warehouse_id:库房id
	 *                    ,warehouse:库房名称,pcy_id:产品公司id,pcy:产品公司,counterart_id:对手方id,counterpart:对手方名称,position:交易单据持有头寸
	 *                    ,price:公司产品单价,quantity:公司产品数量,time:记账时间
	 * @return IRecord {code:结果状态标记0表示成功,data:json结构根节点即透视表数据,
	 *         keys:当前结果所使用的keys,allkeys:可供设计透视表的键值列表}
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("trial_balance")
	public Mono<IRecord> trial_balance(final @Param Long[] company_ids, final @Param String keys) {
		final var fa = fabuilder.add("policy", "policy0001").build(); // 创建会计类型
		final var ledger = fa.getLedger(glid(company_ids));
		Stream.of(Optional.ofNullable(company_ids).orElseGet(() -> new Long[] { 1l, 2l }))
				.forEach(executor_of(jdbcApp, ledger, "#GJVs")); // 模拟各个公司的运行
		final var _keys = Optional.ofNullable(keys).orElse("ledger_id,acctnum,warehouse,item,drcr").split(","); // 透视表键值列表
		final var json = fa.trialBalance(_keys).json( // 生成son
				(sb, node) -> {
					final var value = node.attrvalOpt().map(Types.obj2dbl(0d)).orElse(0d); // 提取数值性的节点值属性
					final String key = node.attr("key"); // 透视表索引路径
					final var nodename = node.getName(); // 节点名
					final var name = switch (key) { // name 翻译
					case "drcr" -> switch (nodename) { // drcr 借贷标记 替换
					case "1" -> "DR"; // 借方
					case "-1" -> "CR"; // 贷方
					default -> "NONE"; // 非借贷标记
					}; // 借贷标记字段的枢轴字段值的翻译
					default -> nodename;
					}; // 根据阶层键名key名进行对应的枢轴字段值的翻译
					return String.format("{\"name\":\"%s : %.2f\", \"value\":%f, \"children\":[", name, value, value);
				}, //
				(sb, node) -> "]}"); // 生成json
		final var root = new HashMap<Object, Object>(); // 解析json生成根节点
		try {
			root.putAll(objM.readValue(json, Map.class)); // 保存解析数据
		} catch (Exception e) {
			e.printStackTrace();
		}
		final var allkeys = fa.getEntrieS().findFirst().map(IRecord::keys).orElse(new LinkedList<>()); // 可供设计透视表的键值列表
		return Mono.just(REC("code", "0", "data", root, "keys", keys, "allkeys", allkeys));
	}

	/**
	 * 根据公司id生成总账id,General Ledger Id
	 * 
	 * @param company_ids 公司id列表
	 * @return 总账id
	 */
	private String glid(final Long... company_ids) {
		if (company_ids == null || company_ids.length < 1) { // 默认加时间戳
			return String.format("GL[NONE%s]", now());
		} else {
			final var sql = String.format("select name from t_company where id in ( %s ) ",
					Stream.of(company_ids).map(Object::toString).collect(joining(",")));
			final var names = jdbcApp.sqlqueryS(sql).map(e -> e.str(0)).collect(joining(","));
			return String.format("GL[%s]", names);
		} // if
	}

	@Autowired
	private IMySQL jdbcApp;
	@Autowired
	private FinAcctBuilder fabuilder;

}
