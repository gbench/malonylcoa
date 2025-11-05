package gbench.sandbox.jdbc.finance.acct;

import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.tree.Node;

/**
 * 分类账
 */
public class Ledger {

	/**
	 * 账册就是就是一个特定的核算对象
	 * 
	 * @param id 账册id
	 * @param fa 财务会计
	 */
	public Ledger(final String id, final FinAcct fa) {
		this.fa = fa;
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
			case "CL" -> balance_adjust.apply(sess.getAcctBalance(acctnum)); // 余额调整:清零科目
			case "BL" -> balance_adjust.apply(sess.getJournalBalance()); // 余额调整:倒轧科目余额依据日记账累计余额
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
	 * @return 分类账的试算平衡表
	 */
	public Node<String> handle(final String path, final double amount, Object... restings) {
		return this.handle(REC("path", path, "amount", amount).derive(restings));
	}

	/**
	 * 会计记账
	 * 
	 * @param variables 变量列表，variables 必须包含,path和amount字段,其余字段根据单据类型自行设置。<br>
	 *                  path:是会计策略中的单据类型和会计主体的头寸位置,是由t_acct_policy的bill_type与position连个字段拼接成的字符串<br>
	 *                  比如：交易性金融资产-初始确认/LONG <br>
	 *                  amount:默认金额，也就是借贷记账时候写入账户的金额,如果variables没有没有明确给出但再会计策略中又又要求提供的时候，所使用的金额。<br>
	 *                  variables的其余变量，则是 根据会计策略的记账要求，需要给与专门提供的金额。比如：<br>
	 *                  交易性金融资产-初始确认/LONG 里面的 应收股利，投资收益(即交易费用),银行存款等。<br>
	 *                  变量名需要使用中文名称进行标记，
	 * @return 分类账的试算平衡表
	 */
	public Node<String> handle(final IRecord variables) {
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
		final var node = fa.trialBalance(id, null);

		return node;
	}

	/**
	 * 
	 * @return
	 */
	public DFrame getEntries() {
		return fa.getEntries(id).stream().collect(DFrame.dfmclc);
	}

	/**
	 * 开启一个日记账会话<br>
	 * 每个日记账会话，创建并维护一个独立的日记账簿:JournalEntries
	 * 
	 * @param variables 变量集合
	 * @param action    会话处理过程
	 * @return 日记账分录
	 */
	public synchronized List<IRecord> withTransaction(final IRecord variables, final Consumer<IJournalSession> action) {
		final var path = variables.str("path"); // 策略路径
		final var policy = fa.getPolicies().path2rec(path);
		final var journalId = UUID.randomUUID().toString(); // 日记账会话的交易id，或者说是 独立日记账簿ID
		final var journalEntries = new LinkedList<IRecord>(); // 独立日记账簿
		final Function<Tuple2<String, ?>, Stream<Tuple2<?, ?>>> translator = p -> { // key:键名,value:键值
			@SuppressWarnings("unchecked")
			final var empty = (Stream<Tuple2<?, ?>>) (Object) Stream.empty();

			if (p._1() instanceof String line) {
				if (line.matches("^\\d+$")) {// 仅处理数字类型的字段名
					return Optional.ofNullable(line).map(IRecord.obj2dbl()).map(Number::longValue) //
							.flatMap(fa::getAcctOpt).map(account -> { // 账户内容
								final Tuple2<?, ?> p1 = Tuple2.of(account.str("account"), p._2()); // 字符串键名
								final Tuple2<?, ?> p2 = Tuple2.of(account.lng("acctnum"), p._2()); // 数值键名
								return Stream.of(p1, p2);
							}).orElse(empty);
				} else { // 其他数值类型的
					final var p1 = p.fmap1(s -> s.replaceAll("[-]+", "-"));
					return Stream.of(p1, p);
				}
			}
			return empty;
		};

		final var _variables = new HashMap<Object, Object>();
		_variables.putAll(variables.toMap()); // 调整后的变量列表
		variables.tupleS().flatMap(translator).forEach(e -> {
			_variables.computeIfAbsent(e._1(), k -> e._2()); // 仅当值不存在的时候才给予添加
		});

		// 交易会话
		action.accept(new IJournalSession() {
			public String getId() {
				return journalId;
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

			public List<IRecord> getJournalEntries() {
				return journalEntries;
			}

			public double getAcctBalance(final long acctnum) {
				return fa.getBalance(id, acctnum);
			}

			public Map<Object, Object> getVariables() {
				return _variables;
			}

			public List<IRecord> getEntries() {
				return fa.getEntries(id);
			}

		});

		final var sortedjes = journalEntries.stream() //
				.sorted((a, b) -> { // 先借后贷的顺序,即: 1 -> -1
					return b.i4("drcr") - a.i4("drcr");
				}) //
				.toList(); // 给予借贷排序后的日记账分录
		fa.store(sortedjes);

		return sortedjes;
	}

	/**
	 * 科目指令的计算顺序
	 */
	final static IRecord DRCR_RANKS = REC("DR", 0, "CR", 1, "CL", 2, "BL", 3); // 序号

	private final String id;
	private final FinAcct fa;

}