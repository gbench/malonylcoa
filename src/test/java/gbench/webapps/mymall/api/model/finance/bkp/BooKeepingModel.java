package gbench.webapps.mymall.api.model.finance.bkp;

import static gbench.util.jdbc.kvp.DFrame.dfmclc;
import static gbench.util.jdbc.kvp.IRecord.REC;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.jdbc.kvp.IRecord;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 账簿业务模块
 * 
 * @author Administrator
 *
 */
public class BooKeepingModel extends AbstractBkp {

	/**
	 * BooKeepingModel
	 * 
	 * @param sqlfile
	 * @param datafile
	 * @param h2_flag
	 */
	public BooKeepingModel(final String sqlfile, final String datafile, final boolean h2_flag) {
		super(sqlfile, datafile, h2_flag);
	}

	/**
	 * 根据 datafile 文件生成对应的记账分录
	 * 
	 * @param datafile     模板数据文件
	 * @param transactions 数据表单名,默认为transactions
	 * @param policies     数据表单名,默认为policies
	 */
	public List<IRecord> journalize(final String datafile, final String transactions, final String policies) {
		final var excel = SimpleExcel.of(datafile);
		final Function<String, DFrame> readfm = name -> excel.autoDetect(name).collect(dfmclc(IRecord::REC));

		return this.journalize(readfm.apply(Optional.ofNullable(transactions).orElse("transactions")),
				readfm.apply(Optional.ofNullable(policies).orElse("policies")));
	}

	/**
	 * 根据 datafile 文件生成对应的记账分录
	 * 
	 * @param linedfm   交易数据行列表 行字段结构为:<br>
	 *                  id path amounts date comment)
	 * @param policydfm 会计策略行列表 行字段结构为:<br>
	 *                  drcr path/account date/amount comment
	 */
	public List<IRecord> journalize(final DFrame linedfm, final DFrame policydfm) {
		final var policies = new HashMap<String, DFrame>(); // 记账策略的分析&分解path->记账策略的分析&分解结构(journal:日记账,entries:交易事项的科目分解结构)即lpdfm

		// 策略分析
		this.handle(policydfm.rows(), plines -> { // 策略行 policylines
			final var dfm = DFrame.of(plines); // 策略集合
			final var head = dfm.head(); // 首行为journal 结构
			head.stropt("path/account").ifPresent(path -> { //
				policies.put(path, dfm);
			}); // ifPresent
		}); // handle

		return linedfm.rowS(line -> { // 一次处理各个交易数据
			final var path = line.str("path"); // 交易的记账策略的分析&分解path
			final var lpdfm = policies.get(path); // line_policy 记账策略的分析&分解结构(journal:日记账,entries:交易事项的科目分解结构)
			if (lpdfm != null) {// 急症
				final var amounts = line.stropt("amounts").map(e -> e.split("[;]+")).orElse(new String[] { "0" }); // 交易金额
				final var date = line.stropt("date").orElse(now().format(ofPattern("yyyy-MM-dd HH:mm:ss")));// 交易时间
				final var comment = line.stropt("comment").orElse("-"); // 交易注释
				final var journal = REC("path", path, "date", date, "comment", comment); // 交易事项的日记账记录对象
				final var ai = new AtomicInteger(); // 行号索引偏移从0开始
				final var entries = lpdfm.tail().rowS().map(e -> REC( //
						"drcr", e.stropt("drcr").orElse("-"), // 借贷方向
						"account", e.stropt("path/account").orElse("-"), // 账户名称
						"amount", amounts[ai.getAndIncrement() % amounts.length], // 账户金额
						"comment", e.stropt("comment").orElse("-"))).toList(); // 会计分录集合
				return REC("journal", journal, "entries", entries);
			} else {
				return null;
			}
		}).filter(Objects::nonNull).toList();
	}
}
