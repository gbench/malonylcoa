package gbench.sandbox.data.pignatoro.fmv;

import static gbench.util.data.DataApp.DFrame.dfmclc2;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.data.xls.SimpleExcel;

public class ReadFileTest1 {

	/**
	 * 文件书写测试
	 */
	@Test
	public void foo() {
		final var file = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/myfuture/api/model/data/datafile.xlsx";
		try (final var excel = SimpleExcel.of(file)) {
			final var dfm = excel.autoDetectDMX("t_match_order").collect(dfmclc2);
			println(dfm);
		}
	}

}
