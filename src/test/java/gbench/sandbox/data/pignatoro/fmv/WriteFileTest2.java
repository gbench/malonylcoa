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
			final var rname = "%s!F1:B10".formatted("foo");
			final var aa = excel.select(rname).poutline(IndexedColors.BLACK);
			println(aa.segment(2, 5).background(IndexedColors.YELLOW));
			excel.save();
		}
		println("completed:%s".formatted(file));
	}

	/**
	 * 文件书写测试
	 */
	@Test
	public void bar() {
		final var file = outhome.formatted("bar.xlsx");

		try (final var excel = SimpleExcel.of(file)) {
			final var aa = excel.select("BAR!B1:F7");
			int i = 0;
			final var cs = new IndexedColors[] { IndexedColors.GREEN, IndexedColors.RED, IndexedColors.BLUE,
					IndexedColors.YELLOW, IndexedColors.CORAL };

			for (var s : aa.splits(true, 2, 4)) { // 相当于0,2,4,7
				s.set(i).background(cs[i]);
				i++;
			}

			excel.save();
		}
		println("completed:%s".formatted(file));
	}

	/**
	 * quz
	 */
	@Test
	public void quz() {
		final var excel = SimpleExcel.of(datafile);
		println(excel.select("INCOME_STATEMENT3!B14:E41").dmx());
		println("---------------------------------------------------------");
		println(excel.select("INCOME_STATEMENT3!B14:E41").dfm(null, e -> "'%s:%s'".formatted(e.getCellType(), e)));
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
