package gbench.sandbox.jdbc.finance.acct;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.tree.Node;

/**
 * 记账会话
 */
public interface ILedgerSession {

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
	 * 分类账的科目集合
	 * 
	 * @return
	 */
	List<IRecord> getEntries();

	/**
	 * 日记账的分录集合
	 * 
	 * @return
	 */
	List<IRecord> getJournalItems();

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
	double getBalance(final long acctnum);

	/**
	 * 计算会话语境下的变量名称
	 * 
	 * @param variable 变量名称<br>
	 *                 1)若name是数值类型,则从coa中检索对应科目名称后再从会话上下文的变量注册表variables中检索
	 *                 2)name是字符串名称,直接从会话上下文的变量注册表variables中检索变量的值
	 * @return name 所标记的值
	 */
	double evaluate(final Object variable);

	/**
	 * 书写一个借贷分录
	 * 
	 * @param drcr   借贷方向
	 * @param name   账户编码
	 * @param amount 金额
	 * @return 借贷分录
	 */
	IRecord write(final int drcr, final Object name, final double amount);

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
	 * 当前会话的分录的科目余额
	 * 
	 * @return 会话的分录的科目余额
	 */
	default double getJournalBalance() {
		return evaluateBalance(trialBalance(this.getJournalItems()));
	}

	/**
	 * 试算平衡表
	 * 
	 * @return
	 */
	static Node<String> trialBalance(final List<IRecord> entries) {
		final var balance = entries.stream().collect(DFrame.dfmclc).pivotTable("ledger,acctnum,drcr", //
				ss -> ss.collect(Collectors.summarizingDouble(e -> e.dbl("amount"))).getSum());
		final var root = balance.treeNode();
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