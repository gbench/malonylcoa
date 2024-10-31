package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.array.INdarray.nats;
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
		final var is_header = excel.range("INCOME_STATEMENT3!B15:E15", nats(4).fmap(e -> e + "").data())
				.collect(DFrame.dfmclc2);
		println(is_header);
		println("-".repeat(100));
		final var is_data = excel.range("INCOME_STATEMENT3!B16:E41").collect(DFrame.dfmclc2);

		println(is_data);
	}

	final String datafile = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files/amazon-2021-10k.xls"
			.formatted(WS_HOME);
}
