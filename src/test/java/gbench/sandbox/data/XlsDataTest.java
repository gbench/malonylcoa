package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;

import gbench.global.Globals;
import gbench.util.data.xls.SimpleExcel;

/**
 * 
 */
public class XlsDataTest {

	@Test
	public void foo() {
		final var datafile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";
		try (final var excel = SimpleExcel.of(datafile)) {
			final var mx = excel.autoDetect(1);
			println(mx);
		}
	}

}
