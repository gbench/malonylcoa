package gbench.sandbox.data.pignatoro.fmv;

import org.junit.jupiter.api.Test;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.lisp.Lisp.CONS;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;

/**
 * 财务估值模型
 */
public class FinModValTest {

	@Test
	public void foo() {
		println("datafile", datafile);

		final var excel = SimpleExcel.of(datafile); // 读入财务数据
		final var hdname = "INCOME_STATEMENT3!B15:E15"; // 表头区域:head name
		final var bdname = "INCOME_STATEMENT3!B16:E41"; // 表数据区域: body name
		final Function<StrMatrix, DFrame> dfmapper = strmx -> strmx.collect(DFrame.dfmclc2); // 转换成DFrame
		// new String[0]表示使用默认的自定义表头,不可用null代替,null表示把区域RANGE第一行作为表头
		final var stmtdfm = excel.rangeOpt(hdname, new String[0], dfmapper) // 强制使用默认表头_A,_B,_C,...进行StrMatrix字段命名
				.flatMap(strmx -> strmx.headOpt().map(rec -> rec.set(0, "item").toArray(String.class::cast))) // 第一列改名为item
				.flatMap(keys -> excel.rangeOpt(bdname, keys, dfmapper)).orElse(null);
		println("INCOME STATEMENT", stmtdfm); // 利润表
		final var lines = stmtdfm.rowS().collect(Collectors.toMap(e -> e.str(0), e -> e.filterNot(0), (a, b) -> b)); // 数据行,同名覆盖
		println("-- ".repeat(100));
		final var NPS = "Net product sales"; // 产品销售收入
		final var NSS = "Net service sales"; // 服务销售收入
		println(NPS, lines.get(NPS));
		println(NSS, lines.get(NSS));

		// 数据行提取操作
		final Function<String[], Stream<IRecord>> gets = keys -> Stream.of(keys).map(lines::get); // 提取指定keys中的数据行
		final Function<String[], IRecord> add = gets.andThen(lns -> lns.reduce(IRecord.plus()).get()); // 加法
		final Function<String[], IRecord> sub = gets.andThen(lns -> lns.reduce(IRecord.subtract()).get()); // 减法
		final Function<String[], IRecord> div = gets.andThen(lns -> lns.reduce(IRecord.divide()).get()); // 除法
		final Function<String[], IRecord> mul = gets.andThen(lns -> lns.reduce(IRecord.multiply()).get()); // 乘法

		// 计算总收入
		final var REVENUE = "REVENUE"; // 总收入（交税收入）
		final var revenue = lines.computeIfAbsent(REVENUE, k -> add.apply(A(NPS, NSS))); // 计算总销售收入
		println(REVENUE, revenue);

		// 计算同比增长率
		final var stmtproto = stmtdfm.head(); // 表头
		final Function<String, IRecord> growth_eval = key -> Optional.ofNullable(lines.get(key)) // 提取指定key的数据行
				.map(e -> e.valueS().collect(IRecord.slidingclc(IRecord.rb("previous,current")::get, 2, 1, true)) // 齐次的宽度2步长1的连续窗口滑动
						.map(entry -> entry.dbl("current") / entry.dbl("previous") - 1).toArray(Double[]::new)) // 计算增长率
				.map(e -> CONS(null, CONS(null, e))).map(IRecord.rb(stmtproto.keys())::get).orElse(null); // item,首项的增长率为null
		final var REVENUE_GROWTH = "REVENUE_GROWTH"; // 收入增长率
		final var revenue_growth = lines.computeIfAbsent(REVENUE_GROWTH, k -> growth_eval.apply(REVENUE)); // 计算收入增长率
		println(REVENUE_GROWTH, revenue_growth);

		// 毛利润
		final var COGS = "Cost of Goods Sold"; // 销售成本
		final var GPROFIT = "Gross Profit"; // 毛利润
		lines.put(COGS, lines.get("Cost of sales")); // 写入COGS， 把 ‘Cost of sales’ 改名为 ‘Cost of Goods Sold’
		final var gprofit = lines.computeIfAbsent(GPROFIT, k -> sub.apply(A(REVENUE, COGS))); // 毛利润
		println(GPROFIT, gprofit);
		final var NEG_ONE = "NEG_ONE"; // 负1向量
		final var NEG_COGS = "NEG_COGS"; // 销售成本*-1
		final var lnrb = lines.entrySet().iterator().next().getValue().rb(); // 数据行计算器
		final var neg_one = lines.computeIfAbsent(NEG_ONE, k -> lnrb.get(-1)); // -1向量值
		final var neg_cogs = lines.computeIfAbsent(NEG_COGS, k -> mul.apply(A(COGS, NEG_ONE))); // 负的销货成本
		println("%s(2)".formatted(GPROFIT), add.apply(A(REVENUE, NEG_COGS)), neg_one, neg_cogs);

		// 毛利率
		final var GMARGIN = "Gross Margin"; // 毛利率
		final var gmargin = lines.computeIfAbsent(GMARGIN, k -> div.apply(A(GPROFIT, REVENUE))); // 毛利率
		println(GMARGIN, gmargin);
	}

	/**
	 * 数据源文件
	 */
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件
}
