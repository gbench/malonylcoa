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
	 * 会计分录
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 会计对象
	 * 
	 * @return
	 */
	String getLedgerId();

	/**
	 * 会计分录
	 * 
	 * @return
	 */
	List<IRecord> getEntries();

	/**
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
	 * 日记账项目
	 * 
	 * @return
	 */
	List<IRecord> getJournalItems();

	/**
	 * 
	 * @param acctnum
	 * @return
	 */
	double getBalance(final long acctnum);

	/**
	 * 提取指定参数
	 * 
	 * @param name 名称检索
	 * @return
	 */
	double evaluate(final Object name);

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
	 * 借方
	 * 
	 * @param name   账户
	 * @param amount 金额
	 * @return 借贷账户
	 */
	default IRecord debit(final Object name, final double amount) {
		return this.write(1, name, amount);
	}

	/**
	 * 贷方
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
		if (opt.isPresent()) {
			final var d = opt.map(IRecord.obj2dbl()).get();
			final var sgn = Objects.equals(node.getName(), "-1") ? -1 : 1;
			return sgn * d;
		} else {
			double total = 0d;
			for (var c : node.childrenL()) {
				final var d = evaluateBalance(c);
				total += d;
				c.attrSet("value", d);
			} // for
			return total;
		} // if
	}
}