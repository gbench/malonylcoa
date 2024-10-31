package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;

import org.junit.jupiter.api.Test;

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

		final var excel = SimpleExcel.of(datafile);
		final var hdname = "INCOME_STATEMENT3!B15:E15"; // 表头区域:head name
		final var bdname = "INCOME_STATEMENT3!B16:E41"; // 表数据区域: body name
		final Function<StrMatrix, DFrame> dfmapper = strmx -> strmx.collect(DFrame.dfmclc2); // 转换成DFrame
		final var stmtdfm = excel.rangeOpt(hdname, new String[] {}, dfmapper)
				.flatMap(strmx -> strmx.headOpt().map(rec -> rec.set(0, "item").toArray(String.class::cast)))
				.flatMap(keys -> excel.rangeOpt(bdname, keys, dfmapper)).orElse(null);
		println("INCOME STATEMENT", stmtdfm); // 利润表
		final var lines = stmtdfm.rowS().collect(Collectors.toMap(e -> e.str(0), e -> e.filterNot(0), (a, b) -> b));
		println("-- ".repeat(100));
		final var NPS = "Net product sales"; //
		final var NSS = "Net service sales"; //
		println(NPS, lines.get(NPS));
		println(NSS, lines.get(NSS));

		// 数据行提取操作
		final Function<String[], Stream<IRecord>> aslineS = keys -> Stream.of(keys).map(lines::get);
		final Function<String[], IRecord> add = aslineS.andThen(lns -> lns.reduce(IRecord.plus()).get());
		final Function<String[], IRecord> sub = aslineS.andThen(lns -> lns.reduce(IRecord.subtract()).get());
		final Function<String[], IRecord> div = aslineS.andThen(lns -> lns.reduce(IRecord.divide()).get());
		@SuppressWarnings("unused")
		final Function<String[], IRecord> mul = aslineS.andThen(lns -> lns.reduce(IRecord.multiply()).get());

		// 计算总收入
		final var total = add.apply(A(NPS, NSS));
		final var TOTAL = "TOTAL"; // 总利润
		lines.put(TOTAL, total); // 加入总利润
		println(TOTAL, total);

		// 毛利润
		final var COGS = "Cost of Goods Sold"; // 销售成本
		final var GPROFIT = "Gross Profit"; // 毛利润
		lines.put(COGS, lines.get("Cost of sales")); // 改名
		final var gprofit = sub.apply(A(TOTAL, COGS)); // 计算毛利润
		lines.put(GPROFIT, gprofit); // 毛利润
		println(GPROFIT, gprofit);

		// 毛利率
		final var GMARGIN = "Gross Margin"; // 毛利率
		final var gmargin = div.apply(A(GPROFIT, TOTAL)); // 计算毛利率
		lines.put(GMARGIN, gmargin);
		println(GMARGIN, gmargin);
	}

	final String datafile = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files/amazon-2021-10k.xls"
			.formatted(WS_HOME);
}
