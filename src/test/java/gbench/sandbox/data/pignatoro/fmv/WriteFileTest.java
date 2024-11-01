package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.junit.jupiter.api.Test;

import gbench.util.lisp.DFrame;
import gbench.util.data.xls.DataMatrix;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.lisp.IRecord;

/**
 * 文件书写测试
 */
public class WriteFileTest {

	/**
	 * 文件书写测试
	 */
	@Test
	public void foo() {
		try (final var excel = SimpleExcel.of(outfile)) {
			final var rb = nats(10).fmap(e -> DataMatrix.xlsn(e)).arrayOf(IRecord::rb);
			final var dfm = nats(100).cuts(10).map(e -> rb.get(e)).collect(DFrame.dfmclc);

			excel.select("X!C3:H8").rowS().forEach(line -> {
				line.bgclr(IndexedColors.YELLOW);
				line.bdclr();
				println(line.dump());
			});
			excel.write(dfm).save();
		}
		println("completed:%s".formatted(this.outfile));
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
