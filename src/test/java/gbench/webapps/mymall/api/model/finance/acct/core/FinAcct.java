package gbench.webapps.mymall.api.model.finance.acct.core;

import static gbench.webapps.mymall.api.model.finance.acct.core.IJournalSession.evaluateBalance;
import static java.util.stream.Collectors.summarizingDouble;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.tree.Node;

/**
 * 会计记账的原理就是根据记账策略(特定类型的记账凭证的记账法)，把会计凭证中的内容编制成会计分录 <br>
 * Financial Accounting
 */
public class FinAcct extends AbstractAcct<FinAcct> {

	/**
	 * 账册就是就是一个特定的核算对象
	 * 
	 * @param name     账册名称
	 * @param coa      科目表,请确保acctnum列为long格式
	 * @param policies 记账策略
	 */
	public FinAcct(final IMySQL jdbcApp, final DFrame coa, final IRecord policies) {
		super(jdbcApp);
		this.coa = coa;
		this.policies = policies;
	}

	/**
	 * 获取账户信息
	 * 
	 * @param account 账户
	 * @return 账户信息
	 */
	public Optional<IRecord> getAcctOpt(final Object account) {
		if (account instanceof Number acctnum) {
			return coa.one2opt("acctnum", acctnum.longValue(), "coa_num");
		} else if (account instanceof String name) { // 账户名称
			return coa.one2opt("account", name, "coa_name");
		} else {
			return Optional.empty();
		}
	}

	/**
	 * 获取账户信息
	 * 
	 * @param ledgerId 分类账
	 * @param account  账户
	 * @return 账户信息
	 */
	public IRecord getAccount(final String ledgerId, final Object account) {
		return getAcctOpt(account).map(acct -> acct.derive("balance", this.getBalance(ledgerId, acct.lng("acctnum"))))
				.orElse(null);

	}

	/**
	 * 获取指定分类账下面额分录账户余额
	 * 
	 * @param acctnum 账户编码
	 * @return 账户余额
	 */
	public double getBalance(final String ledgerId, final long acctnum) {
		final var balance = this.getEntrieS(ledgerId) // 获取科目分类账
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
	 * @return 账户分录数据
	 */
	public List<IRecord> getEntries() {
		return this.entries;
	}

	/**
	 * 账户数据
	 * 
	 * @return 账户分录数据
	 */
	public Stream<IRecord> getEntrieS() {
		return this.entries.stream();
	}

	/**
	 * 账户数据
	 * 
	 * @param ledgerId 分类账
	 * @return 账户分录数据
	 */
	public Stream<IRecord> getEntrieS(final String ledgerId) {
		return this.getEntries(ledgerId).stream();
	}

	/**
	 * 账户数据
	 * 
	 * @param ledgerId 分类账id
	 * @return 账户分录数据
	 */
	public List<IRecord> getEntries(final String ledgerId) {
		if (ledgerId == null) {
			return this.getEntries();
		} else {
			return this.getEntrieS() //
					.filter(e -> Objects.equals(ledgerId, e.str("ledger_id"))) //
					.toList();
		}
	}

	/**
	 * 会计策略
	 * 
	 * @return 会计策略
	 */
	public IRecord getPolicies() {
		return policies;
	}

	/**
	 * 查看指定分类账试算平衡
	 * 
	 * @param ledgerId 分类账id
	 * @return 分类账的根节点
	 */
	public Node<String> trialBalance() {
		return IJournalSession.trialBalance(this.getEntries());
	}

	/**
	 * 查看指定分类账试算平衡
	 * 
	 * @param ledgerId 分类账id
	 * @return 分类账的根节点
	 */
	public Node<String> trialBalance(final String ledgerId) {
		return this.trialBalance(ledgerId, (String) null);
	}

	/**
	 * 查看指定分类账试算平衡
	 * 
	 * @param keys 阶层key名序列
	 * @return 分类账的根节点
	 */
	public Node<String> trialBalance(final String keys[]) {
		return this.trialBalance(null, keys == null ? null : Arrays.stream(keys).collect(Collectors.joining(",")));
	}

	/**
	 * 查看指定分类账试算平衡
	 * 
	 * @param ledgerId 分类账id
	 * @param keys     阶层key名序列
	 * @return 分类账的根节点
	 */
	public Node<String> trialBalance(final String ledgerId, final String keys) {
		return IJournalSession.trialBalance(this.getEntries(ledgerId), keys);
	}

	/**
	 * 存储分类账行记录:一般为
	 * 
	 * @param journalItems 日记账分录集合
	 * @return FinAcct(this)对象本身,以便实现链式编程
	 */
	public FinAcct store(final List<IRecord> journalItems) {
		/**
		 * 写入journalItems数据， <br>
		 * 可以在此加入持久化到数据库或是消息队列的算法，以便可以将日记账内容分发到相应的存贮或是分析部件
		 */
		this.entries.addAll(journalItems);

		// 返回 FinAcct 对象本身
		return this;
	}

	/**
	 * 查看数据内容
	 * 
	 * @param root 根节点
	 */
	public String dump(final Node<String> root) {
		final Function<Node<String>, String> nameit = node -> //
		this.getAcctOpt(Long.parseLong(node.getName()))//
				.map(e -> e.str("account")).orElse(null);
		return this.dump(nameit, root);
	}

	/**
	 * 查看数据内容
	 * 
	 * @param root 根节点
	 */
	public String dump(Function<Node<String>, String> nameit, final Node<String> root) {
		final var builder = new StringBuilder();
		builder.append(String.format("\n-------------[NODE:%s]-----------------\n", root));

		root.forEach(node -> {
			final Integer level = node.getLevel();
			final var key = node.attr("key", "-"); // 提取层级key
			final var _name = node.getName(); // 源内容
			final var name = switch (key) {
			case "acctnum" -> this.getAcctOpt(Integer.parseInt(_name)) //
					.map(e -> e.str("account")).orElse(_name); // 翻译成账户名称
			case "drcr" -> switch (Integer.parseInt(_name)) { // 借贷名称的翻译
			case 1 -> "DR"; // 借方
			case -1 -> "CR"; // 贷方
			default -> _name; // 其他
			};
			default -> _name;
			};
			final var line = String.format("%s%s ---> %s\n", // 模板字符串
					" | ".repeat(level - 1), // 阶层显示
					name, // 科目名称
					evaluateBalance(node));
			// 数据行输出
			builder.append(line);
		});
		return builder.toString();
	}

	private final DFrame coa; // 账户科目表
	private final IRecord policies; // 科技策略
	private final List<IRecord> entries = new LinkedList<IRecord>(); // 账户分录
}
