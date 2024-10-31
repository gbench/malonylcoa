package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.lisp.DFrame;

/**
 * 
 */
public class FinModValTest {

	@Test
	public void foo() {
		println(datafile);
		final var excel = SimpleExcel.of(datafile);
		final var hdname = "INCOME_STATEMENT3!B15:E15"; // 表头区域:head name
		final var bdname = "INCOME_STATEMENT3!B16:E41"; // 表数据区域: body name
		final Function<StrMatrix, DFrame> dfmapper = strmx -> strmx.collect(DFrame.dfmclc2); // 转换成DFrame
		final var dfm = excel.rangeOpt(hdname, new String[] {}, dfmapper)
				.flatMap(strmx -> strmx.headOpt().map(rec -> rec.set(0, "item").toArray(String.class::cast)))
				.flatMap(keys -> excel.rangeOpt(bdname, keys, dfmapper)).orElse(null);

		println(dfm);
	}

	final String datafile = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files/amazon-2021-10k.xls"
			.formatted(WS_HOME);
}
