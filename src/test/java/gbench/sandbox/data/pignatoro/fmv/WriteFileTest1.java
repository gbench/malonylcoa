package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.array.INdarray.nats;
import static gbench.util.data.xls.SimpleExcel.background_color;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.RPTA;
import static org.apache.poi.ss.usermodel.IndexedColors.BLUE;
import static org.apache.poi.ss.usermodel.IndexedColors.RED;
import static org.apache.poi.ss.usermodel.IndexedColors.YELLOW;

import java.time.LocalDateTime;
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
public class WriteFileTest1 {

	/**
	 * 文件书写测试
	 */
	@Test
	public void foo() {
		final var file = outhome.formatted("foo.xlsx");
		try (final var excel = SimpleExcel.of(file)) {
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
						.background(IndexedColors.BLUE_GREY).pcolor(IndexedColors.WHITE)
						.forEach(e -> e.setCellValue(ai.getAndIncrement()));
				aa.row(1).update(1, 2, 4).offset(0, 0);
				aa.row(2).pcolor(IndexedColors.BLUE).bold(true).italic(true)
						.forEach(e -> e.setCellValue(ai.getAndIncrement()));
			});
			excel.write("C!D2", dfm).save();
		}
		println("completed:%s".formatted(file));
	}

	@Test
	public void bar() {
		final var file = outhome.formatted("bar.xlsx");
		try (final var excel = SimpleExcel.of(file)) {
			excel.select("B4:E4") // 设置响应区域
					.update("A,B,C,D".split(",")) // 表头:首行元素使用update
					.italic().bold().topThin(RED).bottomThick(RED) //
					.writeLine(1, 2, 3, 4) // 区域行使用writeLine
					.writeLine(4, 5, 6, 7).bottomThin(RED) // 区域行使用writeLine
					.save();
		}
		println("书写完毕：%s".formatted(file));
	}

	@Test
	public void qux() {
		final var file = outhome.formatted("qux.xlsx");
		try (final var excel = SimpleExcel.of(file)) {
			excel.select("B4:E4") // 设置响应区域
					.update("A,B,C,D".split(",")).paint(style -> { // 表头:首行元素使用update
						style.setBorderTop(BorderStyle.THIN);
						style.setTopBorderColor(IndexedColors.RED.getIndex());
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
		println("书写完毕：%s".formatted(file));
	}

	/**
	 * 典型的EXCEL数据表格写入： <br>
	 * AffectedArea简称aa,是一种数据选取的工具类。<br>
	 * AffectedArea是一个由origin基点cell即ltCell左上角单元格. <br>
	 * height:nrows行数量,width:ncols列数量的excel表单的数据区域的对象。<br>
	 * 提供一系列操作excel表单单元格的批量处理的工具方法 <br>
	 * 保证write操作不会写入其中的数据: <br>
	 * write写入的是aa区域的正对于origin单元格ltCell的同列但是正下间隔为aa.height的行中（ltCell同列），<br>
	 * 要修改其中的内容使用set或updateLine<br>
	 * 1）数据表写入与格式化 <br>
	 * 2）加入margin累计公式 <br>
	 * 3）指定位置的多行数据写入（带有平移hshift,拉伸hextend操作 <br>
	 * 3）hsve:是 hshift与vextend的缩写, h表示horizonal,v表示vertical <br>
	 * 于是有: hsve:水平平移垂直拉伸,hshe：水平平移水平拉伸, vshe：垂直平移水平拉伸,vsve：垂直平移垂直拉伸 <br>
	 * 4) 对于平移：shift,拉伸：extend,与变形/缩放：reshape的说明：<br>
	 * 4.1)shift:表示将AffectedArea的基点origin即左上角的单元格ltCell，做水平或是上下移动,<br>
	 * 高度与宽度保持不变:aa 面积不变,origin坐标改变 <br>
	 * 4.2)extend: 表示将 AffectedArea左右边界进行向外扩展,正数表示向下或向右，负数表示向上或向左。
	 * 改变aa面积,负向extend可以会改变origin<br>
	 * 4.3)reshape:
	 * 表示保持origin不变,更改aa的height即nrows,或是width即ncols列数量。改变的是面积而不是origin坐标 <br>
	 */
	@Test
	public void quz() {
		AffectedArea.debug = true; // 开启调试标记
		final var file = outhome.formatted("quz.xlsx");
		try (final var excel = SimpleExcel.of(file)) {
			excel.select("Sheet2!B1") // (响应区域)落笔点设置作为书写位置的前一行(实际写入在B2)
					.writeTable(nats(25).cuts(5).stream() // 切割成长度为5的均匀片段
							.map(IRecord.rb("ABCDE".split(""))::get).collect(DFrame.dfmclc))
					.paint(style -> { // 绘制数据表式样
						style.setBorderBottom(BorderStyle.DASH_DOT);
						style.setBottomBorderColor(IndexedColors.RED.getIndex());
					}).ptitle(RED).pbottom(RED).withTransaction(aa -> { // 右侧margin公式写入,aa:AffectedArea的缩写
						final var rightAa = aa.right(); // 提取右侧

						rightAa.originAa().hshift(1).set("H-SUBTOTAL").ptitle(RED); // 写入表头h-subtotal
						// 表体数据:subtotal的公式写入:剔除第一行的表头（其余涂成黄色）
						rightAa.hshift(1).skipRows(1).background(YELLOW).rowS().forEach(subtotal -> { // 水平方向上的小计
							final var line = subtotal.hshe(-5, 4); // 水平行(向左移动5个位置,扩展4个位置)
							final var formula = "sum(%s)".formatted(line); // 水平的求和公式
							subtotal.setCellFormula(formula).paintBottom(RED, BorderStyle.DASH_DOT).pleft(RED);
							println(subtotal, subtotal.hshe(-5, 4), formula);
						}); // forEach

						// 演示一个较为复杂的数据选区的操作：right为第五列E列：（此处行列坐标采用的是AffectedArea内部的坐标定位）而非绝对的sheet中的坐标。
						// 选区的水平复制:right（列）E：hshift(-2) 变为：C列(-2步即向左经过(E,D)->C) right:变为C列
						// hextend(-2):变为A列（-2步即向左经过(C,B)->A)），right:变为从A到C列的aa,该aa的base为A2宽度为3
						// reshape把aa变形为成3行2列（A1:B3）最后形成最终的cliplines.
						final var dataAa = rightAa.hshift(-2).hextend(-2).reshape(3, 2); // 一块选区:剪切出一段作为数据行
						final var pointAa = rightAa.hshift(3); // 右侧水平平移3列：I2:书写点

						// 相对nameline的使用（A4:B4是相对于addrAa的，实际为I5:J5）, 采用相对区间选区rangeAa的小计公式写入:下侧margin
						pointAa.rangeAa("A4:B4").pbottom(RED).background(YELLOW).colS().forEach(subtotal -> { // 垂直
							subtotal.setCellFormula("sum(%s)".formatted(subtotal.vsve(-3, 2)));
						});
						// 左侧margin
						pointAa.rangeAa("C1").ptitle(RED); // 以pointAa为基准选择头前的1x3区域作为表头,注意这里的‘C1’等价于‘A1:C1’它会自动补充头前确实的A1
						final var subtotalAa = pointAa.rangeAa("C1:C4"); // 右侧的margin区域
						subtotalAa.head().set("H-SUBTOTAL"); // 写入表头信息-水平(Horizontal)的小计
						subtotalAa.tail().background(YELLOW).pbottom(RED).rowS().forEach(subtotal -> { // 水平
							subtotal.setCellFormula("sum(%s)".formatted(subtotal.hshe(-3, 2)));
						});

						// 打印数据位置
						println("书写点（address)", pointAa, "剪切(clip)数据行区域", dataAa); // 打印操作信息
						// 把该块选区写入右侧第三个位置,并用红框标注
						aa.writeLines(pointAa, dataAa).poutline(RED); // 书写数据并绘制边框
					}) // 式样绘制
					.writeLine((p, i) -> { // 下侧margin公式写入
						final var formula = "sum(%s)".formatted(p.vsve(-5, 4));
						p.setCellFormula(formula);
					}, 6).paint(background_color.apply(IndexedColors.YELLOW)).pbottom(RED) // 汇总和
					.writeLine((p, i) -> { // 写入区域名称
						p.set("%s".formatted(p.vsve(-6, 4)));
					}, 5).pcolor(BLUE) // 蓝色字体
					.writeLine("author:%s,time:%s".formatted("gbench", LocalDateTime.now()).split(",")) // 数据签名
					.pcolor(IndexedColors.PINK) // 粉红字体
					.save();
		}
		println("书写完毕：%s".formatted(file));
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
