package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import com.alibaba.nacos.shaded.com.google.common.base.Objects;

import static gbench.util.io.Output.println;

import gbench.global.Globals;
import gbench.util.data.xls.DataMatrix;
import gbench.util.data.xls.RangeDef;
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

	@Test
	void range() {
		final var rgname = "Sheet1!C2:D3";
		final var rdf0 = new RangeDef("Sheet1", 1, 2, 2, 3);
		final var rdf = DataMatrix.name2rdf(rgname);
		println("rgname", rdf);
		println("Objects.equal(rdf0, rdf)", Objects.equal(rdf0, rdf));
		println("C2", DataMatrix.name2rdf("D1"));
		println("C2:D3", DataMatrix.name2rdf("C2:D3"));
		println("ABC D", DataMatrix.name2rdf("ABC D"));
		println("ABC D1", DataMatrix.name2rdf("ABC D1"));
		println("D1", DataMatrix.name2rdf("D1"));
	}

}
