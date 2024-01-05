package gbench.sandbox.jdbc.finance.acct;

import static gbench.util.jdbc.kvp.IRecord.REC;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.tree.Node;

/**
 * 日记账会话
 */
public interface IJournalSession {

	/**
	 * 会话ID
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 分类账ID
	 * 
	 * @return
	 */
	String getLedgerId();

	/**
	 * 会计策略(各种不同order_type的单据记账方法)
	 * 
	 * @return
	 */
	IRecord getPolicy();

	/**
	 * 
	 * @param acct 账户编码或者账户名称:long 或者 String
	 * @return 账户信息
	 */
	IRecord getAccount(final Object acct);

	/**
	 * 获取账户余额
	 * 
	 * @param acctnum 账户编码
	 * @return
	 */
	double getAcctBalance(final long acctnum);

	/**
	 * 获取会话变量集合
	 * 
	 * @return 会话变量集合
	 */
	Map<Object, Object> getVariables();

	/**
	 * 日记账的分录集合
	 * 
	 * @return
	 */
	List<IRecord> getJournalEntries();

	/**
	 * 分类账的科目集合
	 * 
	 * @return
	 */
	List<IRecord> getEntries();

	/**
	 * 当前会话的分录的科目累计余额
	 * 
	 * @return 会话的分录的科目余额
	 */
	default double getJournalBalance() {
		return evaluateBalance(trialBalance(this.getJournalEntries()));
	}

	/**
	 * 计算会话语境下的变量名称
	 * 
	 * @param variable 变量名称<br>
	 *                 1)若name是数值类型,则从coa中检索对应科目名称后再从会话上下文的变量注册表variables中检索
	 *                 2)name是字符串名称,直接从会话上下文的变量注册表variables中检索变量的值
	 * @return name 所标记的值
	 */
	default double evaluate(final Object variable) {
		return this.evaluate(this.getVariables(), variable);
	}

	/**
	 * 计算会话语境下的变量名称 <br>
	 * 若是名字不存在则使用默认值amount <br>
	 * 若是默认值也不存在则返回0 <br>
	 * 
	 * @param context  上下文<br>
	 * @param variable 变量名称<br>
	 *                 1)若name是数值类型,则从coa中检索对应科目名称后再从会话上下文的变量注册表context中检索
	 *                 2)name是字符串名称,直接从会话上下文的变量注册表context中检索变量的值
	 * @return name 所标记的值
	 */
	default double evaluate(final Map<Object, Object> context, final Object variable) {
		if (context.get(variable) instanceof Double value) { // 尝试直接读取
			return value;
		} else { // 直接读取失败
			final Function<String, Double> evaluator = name -> {
				final var d = Optional.ofNullable(context.get(name)) // 尝试按照名检索
						.orElseGet(() -> context.get("amount")); // 若是名字不存在则使用默认值amount
				return Optional.ofNullable(d).map(IRecord.obj2dbl()).orElse(0d); // 若是默认值也不存在则返回0
			}; // 变量计算器

			if (variable instanceof String key) { // 根据键名获取键值
				return evaluator.apply(key);
			} else if (variable instanceof Number acctnum) { // 对于账号类型尝试翻译
				final var acct = this.getAccount(acctnum.longValue()); // 提取账号
				return Optional.of(acct).map(e -> e.str("account")).map(evaluator).orElse(0d);
			} else {
				return 0d;
			} // if 根据键名获取键值
		} // if 尝试直接读取
	}

	/**
	 * 书写一个借贷分录
	 * 
	 * @param drcr   借贷方向
	 * @param name   账户编码
	 * @param amount 金额
	 * @return 借贷分录
	 */
	default IRecord write(final int drcr, final Object name, final double amount) {
		final var _drcr = amount > 0 ? drcr : -drcr;
		final var ledger_id = this.getLedgerId(); // 记账对象
		final var now = LocalDateTime.now(); // 当前时间
		final var rec = REC("id", this.getId(), "drcr", _drcr, //
				"name", name, "amount", Math.abs(amount), "ledger_id", ledger_id);
		final var acct = Optional.ofNullable(this.getAccount(name)) //
				.orElse(REC("account", name, "acctnum", name)); // 账户名称
		final var line = rec.derive("name", acct.str("account"), "acctnum", acct.lng("acctnum"));
		this.getJournalEntries().add(line.derive("now", now)); // 写入journalItem

		return line;
	};

	/**
	 * 借方分录
	 * 
	 * @param name   账户
	 * @param amount 金额
	 * @return 借贷账户
	 */
	default IRecord debit(final Object name, final double amount) {
		return this.write(1, name, amount);
	}

	/**
	 * 贷方分录
	 * 
	 * @param name   账户
	 * @param amount 金额
	 * @return 借贷账户
	 */
	default IRecord credit(final Object name, final double amount) {
		return this.write(-1, name, amount);
	}

	/**
	 * 获取指定分类账的试算平衡表(不包裹当前会话的日记账分录)
	 * 
	 * @param ledgerId 客户id
	 * @return 指定分类账的试算平衡表
	 */
	default Node<String> trialBalance(final String ledgerId) {
		return trialBalance(this.getEntries());
	}

	/**
	 * 试算平衡表
	 * 
	 * @param entries 分录数据
	 * @return trialBalance
	 */
	static Node<String> trialBalance(final List<IRecord> entries) {
		return trialBalance(entries, null);
	}

	/**
	 * 试算平衡表
	 * 
	 * @param entries 分录集合
	 * @param keys    键名序列,默认为 "ledger_id,acctnum,drcr"
	 * @return 试算平衡表
	 */
	static Node<String> trialBalance(final List<IRecord> entries, final String keys) {
		final var _keys = Optional.ofNullable(keys).orElse("ledger_id,acctnum,drcr");// 键名序列
		final var balance = entries.stream().collect(DFrame.dfmclc).pivotTable(_keys,
				ss -> ss.collect(Collectors.summarizingDouble(e -> e.dbl("amount"))).getSum());
		final var root = balance.treeNode();
		final var kk = String.format("root,%s", _keys).split(","); // 补充根节点键名
		root.forEach(node -> { // 设置分层key
			node.attrSet("key", kk[node.getLevel() - 1]);
		});
		evaluateBalance(root); // 计算余额
		return root;
	}

	/**
	 * 节点结算
	 * 
	 * @param node 根节点
	 * @return 计算节点的余额
	 */
	static double evaluateBalance(final Node<String> node) {
		final var opt = node.attrvalOpt();
		if (opt.isPresent()) { // 存在数值属性,叶子节点
			final var value = opt.map(IRecord.obj2dbl()).get();
			final var sgn = Objects.equals(node.getName(), "-1") ? -1 : 1;
			return sgn * value;
		} else { // 不存在数值属性,非叶子节点
			double total = 0d; // 总和
			for (final var c : node.childrenL()) {
				final var value = evaluateBalance(c);
				total += value;
				c.attrSet("value", value); // 设置属性值
			} // for
			return total;
		} // if
	}

}