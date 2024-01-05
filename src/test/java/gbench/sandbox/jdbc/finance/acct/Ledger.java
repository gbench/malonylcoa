package gbench.sandbox.jdbc.finance.acct;

import static gbench.sandbox.jdbc.finance.acct.IJournalSession.evaluateBalance;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.tree.Node;

/**
 * 分类账
 */
public class Ledger {

	/**
	 * 账册就是就是一个特定的核算对象
	 * 
	 * @param id  账册id
	 * @param coa 科目表
	 * @param coa 记账策略
	 */
	public Ledger(final String id, final FinAcct accounting) {
		fa = accounting;
		this.id = id;
	}

	/**
	 * 初始化
	 */
	public void initialize() {
		// do nothing
	}

	/**
	 * 生成一个drcr分录科目指令的账户处理处理器
	 * 
	 * @param drcr 分录科目指令
	 * @param acct 分录指定的操作账户
	 * @return sess-&gt;rec
	 */
	final Function<IJournalSession, IRecord> drcr_handler(final String drcr, final IRecord acct) {
		return sess -> {
			final var acctnum = acct.lng("acctnum"); // 提取账号编码
			final var amount = sess.evaluate(acctnum); // 计算账户金额
			final Function<Double, IRecord> balance_adjust = balance -> { // 余额调整分录
				return balance > 0 // 余额方向,大于0借方余额,小于0贷方余额
						? sess.credit(acctnum, balance) // 借方余额贷方配平
						: sess.debit(acctnum, -balance);// 贷方余额借方配平
			};
			final IRecord rec = switch (drcr.toUpperCase()) { // 分录科目指令的处理
			case "DR" -> sess.debit(acctnum, amount); // 借方科目
			case "CR" -> sess.credit(acctnum, amount); // 贷方科目
			case "CL" -> balance_adjust.apply(sess.getBalance(acctnum)); // 余额调整:清零科目
			case "BL" -> balance_adjust.apply(sess.getJournalBalance()); // 余额调整:倒轧日记账的分类科目
			default -> null; // 非法分录科目指令
			}; // 日记账分录

			if (rec == null) {
				System.err.println(String.format("非法分录科目指令:%s", drcr)); // 其他科目
			}

			return rec; // 返回会计分录
		};
	}

	/**
	 * 会计记账
	 * 
	 * @param path     策略路径
	 * @param amount   金额
	 * @param restings 其余参数
	 * @throws Exception
	 * @return 分类账的试算平衡表
	 */
	public Node<String> handle(final String path, final double amount, Object... restings) throws Exception {
		return this.handle(REC("path", path, "amount", amount).derive(restings));
	}

	/**
	 * 会计记账
	 * 
	 * @param variables 变量列表
	 * @throws Exception
	 * @return 分类账的试算平衡表
	 */
	public Node<String> handle(final IRecord variables) throws Exception {
		this.withTransaction(variables, sess -> {
			final var policy = sess.getPolicy(); // 记账策略
			final var drcrs = policy.keys().stream() // key即为分录科目指令,根据DRCR_RANKS的优先级给予排序
					.sorted((a, b) -> DRCR_RANKS.i4(a) - DRCR_RANKS.i4(b)) // 根据规定次序进行比较
					.toList(); // 倒轧指令BL的计算位置最为靠后，且在一组日记账中只能有一个倒轧指令
			final var handlers = new ArrayList<Function<IJournalSession, IRecord>>(drcrs.size());

			for (final var drcr : drcrs) { // 窥基策略的处理
				final var acctdfm = policy.dfm(drcr, sess::getAccount); // 获取记账在分录科目指令drcr下的会计账户
				for (final var acct : acctdfm.rows()) { // 依次处理各个记账
					handlers.add(drcr_handler(drcr, acct)); // 科目指令&账户处理器
				} //
			} // for

			// 执行科目指令
			handlers.forEach(h -> h.apply(sess));

		}); // acct

		// 计算试算平衡
		final var node = fa.trialBalance(id);
		// 查看试算平衡表
		this.dump(node);

		return node;
	}

	/**
	 * 查看数据内容
	 * 
	 * @param root 根节点
	 */
	public void dump(final Node<String> root) {
		System.out.println(String.format("\n-------------[NODE:%s]-----------------", root));

		root.forEach(node -> {
			final Integer level = node.getLevel();
			final var name = switch (level) {
			case 3 -> fa.getAccount(id, Long.parseLong(node.getName())).str("account");
			case 4 -> switch (Integer.parseInt(node.getName())) {
			case 1 -> "DR";
			case -1 -> "CR";
			default -> node.getName();
			};
			default -> node.getName();
			};
			final var line = String.format("%s%s ---> %s", // 模板字符串
					" | ".repeat(level - 1), // 阶层显示
					name, // 科目名称
					evaluateBalance(node));
			// 数据行输出
			System.out.println(line);
		});
	}

	/**
	 * 
	 * @return
	 */
	public DFrame getEntries() {
		return fa.getEntries(id).stream().collect(DFrame.dfmclc);
	}

	/**
	 * 开启一个日记账会话
	 * 
	 * @param variables 变量集合
	 * @param action    会话处理过程
	 * @return 会话结果
	 * @throws Exception
	 */
	public List<IRecord> withTransaction(final IRecord variables, final Consumer<IJournalSession> action)
			throws Exception {

		final var path = variables.str("path"); // 策略路径
		final var policy = fa.getPolicies().path2rec(path);
		final var txid = UUID.randomUUID().toString(); // 日记账会话的交易id
		final var journalItems = new LinkedList<IRecord>();

		// 交易会话
		action.accept(new IJournalSession() {
			public String getId() {
				return txid;
			}

			public String getLedgerId() {
				return Ledger.this.id;
			}

			public IRecord getPolicy() {
				return policy;
			}

			public IRecord getAccount(final Object acctnum) {
				return fa.getAccount(id, acctnum);
			}

			public List<IRecord> getJournalItems() {
				return journalItems;
			}

			public double getBalance(final long acctnum) {
				return fa.getBalance(id, acctnum);
			}
			
			public IRecord getVariables() {
				return variables;
			}

			public List<IRecord> getEntries() {
				return fa.getEntries(id);
			}

			/**
			 * 提取参数金额 获取数据参数
			 * 
			 * @param variable
			 */
			public double evaluate(final Object variable) {
				final Function<String, Double> evaluator = name -> { // 尝试在variables进行按名提取
					final var varopt = variables //
							.aoks2rec(s -> s.replaceAll("[-]+", "-")) // 变换多连结符为单连接符
							.opt(name);
					return varopt.map(IRecord.obj2dbl()).orElseGet(() -> {
						return variables.dbl("amount");
					});
				};

				if (variable instanceof String key) { // 根据键名获取键值
					return evaluator.apply(key);
				} else if (variable instanceof Number acctnum) { // 对于账号类型尝试翻译
					final var acct = this.getAccount(acctnum.longValue()); // 提取账号
					return Optional.of(acct).map(e -> e.str("account")).map(evaluator).orElse(0d);
				} else {
					return 0d;
				}
			}
			
		});
		final var items = journalItems.stream() //
				.sorted((a, b) -> b.i4("drcr") - a.i4("drcr")) //
				.toList();
		fa.store(items);
		
		return items;
	}

	/**
	 * 科目指令的计算顺序
	 */
	final static IRecord DRCR_RANKS = REC("DR", 0, "CR", 1, "CL", 2, "BL", 3); // 序号

	private final String id;
	private final FinAcct fa;

}