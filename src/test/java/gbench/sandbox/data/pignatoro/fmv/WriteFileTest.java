package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.junit.jupiter.api.Test;

import gbench.util.lisp.DFrame;
import gbench.util.data.xls.DataMatrix;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.SimpleExcel.BorderName;
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
			final var ai = new AtomicInteger();

			excel.select("C3:H8").rowS().forEach(line -> {
				line.background(IndexedColors.YELLOW).border(BorderName.BOTTOM, IndexedColors.BLUE)
						.forEach(cell -> cell.setCellValue(ai.getAndIncrement()));
				println(line.dump());
			});
			Optional.of(excel.select("B!C3:H8")).ifPresent(aa -> {
				aa.row(0).border(BorderName.BOTTOM, BorderStyle.THICK, IndexedColors.RED);
				aa.row(2).color(IndexedColors.BLUE).bold(true).italic(true)
						.forEach(e -> e.setCellValue(ai.getAndIncrement()));
			});
			excel.write("C!D2", dfm).save();
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
