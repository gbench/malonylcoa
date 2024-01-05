package gbench.sandbox.jdbc.finance.acct;

import static java.util.stream.Collectors.summarizingDouble;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.tree.Node;

/**
 * Financial Accounting
 */
public class FinAcct {
	/**
	 * 账册就是就是一个特定的核算对象
	 * 
	 * @param name 账册名称
	 * @param coa  科目表
	 * @param coa  记账策略
	 */
	public FinAcct(final DFrame coa, final IRecord policies) {
		this.coa = coa.forEachBy(e -> { // 将账号改为long格式
			e.set("acctnum", e.lng("acctnum"));
		});
		this.policies = policies;
	}

	/**
	 * 获取账户信息
	 * 
	 * @param ledgerId 分类账
	 * @param account  账户
	 * @return 账户信息
	 */
	public IRecord getAccount(final String ledgerId, final Object account) {
		if (account instanceof Number acctnum) {
			final var acct = coa.one2one("acctnum", acctnum.longValue(), "coa_num");
			return acct.derive("balance", this.getBalance(ledgerId, acct.lng("acctnum")));
		} else if (account instanceof String name) { // 账户名称
			final var acct = coa.one2one("account", name, "coa_name");
			return acct.derive("balance", this.getBalance(ledgerId, acct.lng("acctnum")));
		} else {
			return null;
		}
	}

	/**
	 * 获取指定分类账下面额分录账户余额
	 * 
	 * @param acctnum 账户编码
	 * @return 账户余额
	 */
	public double getBalance(final String ledgerId, final long acctnum) {
		final var balance = this.getEntries(ledgerId).stream() // 获取科目分类账
				.filter(e -> Objects.equals(acctnum, e.lng("acctnum"))) //
				.collect(summarizingDouble(e -> e.dbl("drcr") * e.dbl("amount"))).getSum();
		return balance;
	}

	/**
	 * 分类账
	 * 
	 * @param ledgerId 分类账
	 * @return 分类账
	 */
	public Ledger getLedger(final String ledgerId) {
		return new Ledger(ledgerId, this);
	}

	/**
	 * 账户数据
	 * 
	 * @param ledger 分类账
	 * @return 账户分录数据
	 */
	public List<IRecord> getEntries(final String ledger) {
		return this.entries.stream()//
				.filter(e -> Objects.equals(ledger, e.str("ledger"))) //
				.toList();
	}

	/**
	 * 存储分类账行记录
	 * 
	 * @param items 行记录
	 */
	public void store(final List<IRecord> items) {
		this.entries.addAll(items);
	}

	/**
	 * 查看指定分类账试算平衡
	 * 
	 * @param ledgerId 分类账id
	 * @return 分类账的根节点
	 */
	public Node<String> trialBalance(final String ledgerId) {
		return IJournalSession.trialBalance(this.getEntries(ledgerId));
	}

	/**
	 * 
	 * @return
	 */
	public IRecord getPolicies() {
		return policies;
	}

	private final DFrame coa; // 账户科目表
	private final IRecord policies; // 科技策略
	private List<IRecord> entries = new LinkedList<IRecord>(); // 账户分录
}
