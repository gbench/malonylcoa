package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import gbench.util.data.xls.SimpleExcel;

/**
 * 
 */
public class DataTest {

	@Test
	public void foo() {
		final var datafile = "f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";
		try (final var excel = SimpleExcel.of(datafile)) {
			final var mx = excel.autoDetect(1);
			println(mx);
		}
	}

}
