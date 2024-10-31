package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.data.xls.SimpleExcel;
import gbench.util.lisp.DFrame;

/**
 * 
 */
public class FinModValTest {

	@Test
	public void foo() {
		println(datafile);
		final var excel = SimpleExcel.of(datafile);
		final var rnh = "INCOME_STATEMENT3!B15:E15";
		final var rnd = "INCOME_STATEMENT3!B15:E41";
		final var hdfm = excel.range(rnh, new String[] {}).collect(DFrame.dfmclc2);
		final var keys = hdfm.row(0).set(0, "item").values().toArray(String[]::new);
		println(hdfm.row(0));
		println("-".repeat(100));
		final var dfm = excel.range(rnd, keys).collect(DFrame.dfmclc2);

		println(dfm);
	}

	final String datafile = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files/amazon-2021-10k.xls"
			.formatted(WS_HOME);
}
