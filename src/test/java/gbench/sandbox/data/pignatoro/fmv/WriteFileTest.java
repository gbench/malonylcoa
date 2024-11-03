package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.RPTA;
import static org.apache.poi.ss.usermodel.IndexedColors.RED;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.junit.jupiter.api.Test;

import gbench.util.lisp.DFrame;
import gbench.util.data.xls.AffectedArea;
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
		println("书写完毕：%s".formatted(outfile));
	}

	@Test
	public void qux() {
		try (final var excel = SimpleExcel.of(outfile)) {
			excel.select("B4:E4") // 设置响应区域
					.update("A,B,C,D".split(",")).paint(style -> { // 表头:首行元素使用update
						style.setBorderTop(BorderStyle.THIN);
						style.setBorderBottom(BorderStyle.THICK);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).writeLine(1, 2, 3, 4) // 区域行使用writeLine
					.writeLine(4, 5, 6, 7).paint(style -> { // 绘制结尾 式样
						style.setBorderBottom(BorderStyle.THIN);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).writeColumn(1, 2, 3, 4).paint(style -> { // 书写一列元素
						style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
						style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
					}).writeLines(RPTA("".split(""), 5)) // 吸入5个空行
					.writeTable(nats(25).cuts(5).map(d -> IRecord.rb("ABCDE".split("")).get(d)).collect(DFrame.dfmclc))
					.paint(style -> { // 绘制数据表式样
						style.setBorderBottom(BorderStyle.DASH_DOT);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).withTransaction(aa -> {
						aa.firstRow().ptop(IndexedColors.RED);
						aa.lastRow().pbottom(IndexedColors.RED);
					}).save();
		}
		println("书写完毕：%s".formatted(outfile));
	}

	@Test
	public void quz() {
		AffectedArea.debug = true; // 开启调试标记
		try (final var excel = SimpleExcel.of(outfile)) {
			excel.select("Sheet2!B1") // (响应区域)落笔点设置作为书写位置的前一行(实际写入在B2)
					.writeTable(nats(25).cuts(5).stream() // 切割成长度为5的均匀片段
							.map(IRecord.rb("ABCDE".split(""))::get).collect(DFrame.dfmclc))
					.paint(style -> { // 绘制数据表式样
						style.setBorderBottom(BorderStyle.DASH_DOT);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).withTransaction(aa -> aa.ptitle(RED).pbottom(RED)) // 式样绘制
					.writeLine((p, i) -> { // 公式写入
						final var formula = "sum(%s)".formatted(p.vsve(-5, 4));
						p.setCellFormula(formula);
					}, 10).writeLine((p, i) -> { // 写入区域名称
						p.set("=%s".formatted(p.vsve(-6, 4)));
					}, 10).save();
		}
		println("书写完毕：%s".formatted(outfile));
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
