package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
			final BiConsumer<String, Integer> cs = (name, i) -> {
				final var rname = "%s!F1:B10".formatted(name);
				final var aa = excel.select(rname).border();
				println(aa.segment(2, 5).background(IndexedColors.YELLOW));
			};

			cs.accept("A", 1);
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
