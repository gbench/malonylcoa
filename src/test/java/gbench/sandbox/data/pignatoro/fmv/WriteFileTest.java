package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static org.apache.poi.ss.usermodel.IndexedColors.RED;

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
				aa.row(0).border(BorderName.BOTTOM, BorderStyle.THICK, IndexedColors.RED)
						.background(IndexedColors.BLUE_GREY).color(IndexedColors.WHITE)
						.forEach(e -> e.setCellValue(ai.getAndIncrement()));
				aa.row(1).update(1, 2, 4).offset(0, 0);
				aa.row(1).span("A1:C6").background(IndexedColors.RED);
				aa.row(2).color(IndexedColors.BLUE).bold(true).italic(true)
						.forEach(e -> e.setCellValue(ai.getAndIncrement()));
			});
			excel.write("C!D2", dfm).save();
		}
		println("completed:%s".formatted(this.outfile));
	}

	@Test
	public void bar() {
		try (final var excel = SimpleExcel.of(outfile)) {
			excel.select("B4:E4") // 设置响应区域
					.update("A,B,C,D".split(",")) // 表头:首行元素使用update
					.italic().bold().topThin(RED).bottomThick(RED) //
					.writeLine(1, 2, 3, 4) // 区域行使用writeLine
					.writeLine(4, 5, 6, 7).bottomThin(RED) // 区域行使用writeLine
					.save();
		}
		println("书写完毕");
	}

	@Test
	public void qux() {
		try (final var excel = SimpleExcel.of(outfile)) {
			excel.select("B4:E4") // 设置响应区域
					.update("A,B,C,D".split(",")) // 表头:首行元素使用update
					.paint(style -> {
						style.setBorderTop(BorderStyle.THIN);
						style.setBorderBottom(BorderStyle.THICK);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).writeLine(1, 2, 3, 4) // 区域行使用writeLine
					.writeLine(4, 5, 6, 7) //
					.paint(style -> {
						style.setBorderBottom(BorderStyle.THIN);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).save();
		}
		println("书写完毕");
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
