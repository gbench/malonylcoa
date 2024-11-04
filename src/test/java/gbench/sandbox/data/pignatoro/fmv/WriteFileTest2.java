package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.junit.jupiter.api.Test;

import gbench.util.data.xls.SimpleExcel;

/**
 * 文件书写测试
 */
public class WriteFileTest2 {

	/**
	 * 文件书写测试
	 */
	@Test
	public void foo() {
		final var file = outhome.formatted("foo.xlsx");
		try (final var excel = SimpleExcel.of(file)) {
			final var aa = excel.select("B1:F10");
			aa.poutline(IndexedColors.RED);
//			final var a = excel.select("B1:F100").split(3);
//			a._1.background(IndexedColors.RED);
//			a._2.background(IndexedColors.GREEN);
			excel.save();
		}
		println("completed:%s".formatted(file));
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
