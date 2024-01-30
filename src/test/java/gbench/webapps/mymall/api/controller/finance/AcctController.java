package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.Json.objM;
import static gbench.webapps.mymall.api.model.finance.FinAccts.executor_of;
import static java.time.LocalDateTime.now;

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
	public Mono<IRecord> entries(final @Param Integer[] company_ids) {
		final var fa = fabuilder.add("policy", "policy0001").build(); // 创建会计类型
		Stream.of(Optional.ofNullable(company_ids).orElseGet(() -> new Integer[] { 1, 2 }))
				.forEach(executor_of(jdbcApp, fa)); // 模拟各个公司的运行

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
	 *                    ,bill_id:单据id,bill_type:单据类型,product_id:产品id,item_id:公司产品id,product:产品名称,warehouse_id:库房id
	 *                    ,warehouse:库房名称,pcy_id:产品公司id,pcy:产品公司,countart_id:对手方id,counterpart:对手方名称,position:交易单据持有头寸
	 *                    ,time:记账时间
	 * @return IRecord {code:结果状态标记0表示成功,data:json结构根节点即透视表数据,keys:可供设计透视表的键值列表}
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("trial_balance")
	public Mono<IRecord> trial_balance(final @Param Integer[] company_ids, final @Param String keys) {
		final var fa = fabuilder.add("policy", "policy0001").build(); // 创建会计类型
		Stream.of(Optional.ofNullable(company_ids).orElseGet(() -> new Integer[] { 1, 2 }))
				.forEach(executor_of(jdbcApp, fa)); // 模拟各个公司的运行
		final var _keys = Optional.ofNullable(keys).orElse("ledger_id,acctnum,warehouse,product,drcr").split(","); // 透视表键值列表
		final var json = fa.trialBalance(_keys).json( // 生成josn
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
		final var kk = fa.getEntrieS().findFirst().map(IRecord::keys).orElse(new LinkedList<>()); // 可供设计透视表的键值列表
		return Mono.just(REC("code", "0", "data", root, "keys", kk));
	}

	@Autowired
	private IMySQL jdbcApp;
	@Autowired
	private FinAcctBuilder fabuilder;

}
