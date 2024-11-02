package gbench.util.data.xls;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;

import gbench.util.data.xls.SimpleExcel.BorderName;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Pack;

/**
 * AffectedArea被设计为记录一次书写完成以后的数据区域（受影响区域），但实际的使用却未必如此，他可以用于选区和改写
 * 影响范围(AffectedArea AA)
 * 
 * @author xuqinghua
 *
 */
public class AffectedArea implements Iterable<Cell> {

	/**
	 * 影响范围
	 * 
	 * @param ltCell 左上单元格
	 * @param shape  (nrows,ncols)
	 * @param excel  excel对象
	 */
	public AffectedArea(final SimpleExcel excel, final Cell ltCell, final Tuple2<Integer, Integer> shape,
			final Pack<CellStyle> pcs) {
		this.excel = excel;
		this.ltCell = ltCell;
		this.shape = shape;
		this.pcs = pcs;
	}

	/**
	 * 影响范围
	 * 
	 * @param ltCell 左上单元格
	 * @param shape  (nrows,ncols)
	 * @param excel  excel对象
	 */
	public AffectedArea(final SimpleExcel excel, final Cell ltCell, final Tuple2<Integer, Integer> shape) {
		this(excel, ltCell, shape, null);
	}

	/**
	 * 
	 * @param excel
	 * @param ltCell
	 * @param nrows
	 * @param ncols
	 */
	public AffectedArea(final SimpleExcel excel, final Cell ltCell, final int nrows, final int ncols) {
		this(excel, ltCell, Tuple2.of(nrows, ncols));
	}

	/**
	 * 影响范围
	 * 
	 * @param rgname 相对区域名称
	 * @param excel  excel对象
	 * @param startx 开始行偏移从0开始
	 * @param starty 开始列偏移从0开始
	 */
	public AffectedArea(final SimpleExcel excel, final String rgname) {
		this(excel, rgname, 0, 0);
	}

	/**
	 * 影响范围
	 * 
	 * @param rgname 相对区域名称
	 * @param startx 开始行偏移从0开始
	 * @param starty 开始列偏移从0开始
	 * @param excel  excel对象
	 */
	public AffectedArea(final SimpleExcel excel, final String rgname, final int startx, int starty) {
		this(excel, DataMatrix.name2rdf(rgname), startx, starty);

	}

	/**
	 * 影响范围
	 * 
	 * @param excel  excel对象
	 * 
	 * @param rgname 相对区域名称
	 * @param startx 开始行偏移从0开始
	 * @param starty 开始列偏移从0开始
	 */
	public AffectedArea(final SimpleExcel excel, final RangeDef rdf) {
		this(excel, rdf, 0, 0);
	}

	/**
	 * 影响范围
	 * 
	 * @param rgname 相对区域名称
	 * @param startx 开始行偏移从0开始
	 * @param starty 开始列偏移从0开始
	 * @param excel  excel对象
	 */
	public AffectedArea(final SimpleExcel excel, final RangeDef rdf, final int startx, int starty) {
		this.excel = excel;
		final var ltCell = this.excel.cell(rdf.x0() + startx, rdf.y0() + starty);
		final var shape = Tuple2.of(rdf.width(), rdf.height());
		this.ltCell = ltCell;
		this.shape = shape;
	}

	/**
	 * 
	 * @return
	 */
	public Cell getLtCell() {
		return ltCell;
	}

	/**
	 * 
	 * @param ltCell
	 */
	public void setLtCell(final Cell ltCell) {
		this.ltCell = ltCell;
	}

	/**
	 * 返回区域行列数量
	 * 
	 * @return shape (_1:nrows行数量,_2:ncols列数量)
	 */
	public Tuple2<Integer, Integer> getShape() {
		return shape;
	}

	/**
	 * 设置区域行数量与列数据量
	 * 
	 * @param shape (_1:nrows行数量,_2:ncols列数量)
	 */
	public void setShape(final Tuple2<Integer, Integer> shape) {
		this.shape = shape;
	}

	/**
	 * 右下角的 偏移位置。 <br>
	 * offset(0,0) 为 区域的右下角的cell
	 * 
	 * @param offset_x 行偏移
	 * @param offset_y 列偏移
	 * @return 右下角的 偏移位置
	 */
	public Optional<Cell> offsetOpt(final int offset_x, final int offset_y) {
		return Optional.ofNullable(this.ltCell).map(cell -> {
			final int origin_x = cell.getRowIndex(); // 行索引
			final int origin_y = cell.getColumnIndex(); // 列索引
			final int _x = origin_x + this.shape._1 - 1 + offset_x;
			final int _y = origin_y + this.shape._2 - 1 + offset_y;
			return this.excel.getOrCreateCell(cell.getSheet(), _x, _y); //
		}); // Optional
	}

	/**
	 * 相对向定位<br>
	 * 
	 * this.cell(0,0)表示AA的基点单元格 <br>
	 * this.cell(0,1)表示AA的基点单元格的右侧第一个单元格 <br>
	 * this.cell(1,0)表示AA的基点单元格的下侧第一个单元格 <br>
	 * this.cell(1,1)表示AA的基点单元格的右下第一个单元格 <br>
	 * 
	 * @param i AA区域单元格的行索引,从0开始,
	 * @param j AA区域单元格的列索引,从0开始,
	 * @return
	 */
	public Cell cell(final int i, final int j) {
		return this.cellOpt(i, j).orElse(null);
	}

	/**
	 * 相对向定位<br>
	 * 
	 * this.cell(0,0)表示AA的基点单元格 <br>
	 * this.cell(0,1)表示AA的基点单元格的右侧第一个单元格 <br>
	 * this.cell(1,0)表示AA的基点单元格的下侧第一个单元格 <br>
	 * this.cell(1,1)表示AA的基点单元格的右下第一个单元格 <br>
	 * 
	 * @param i AA区域单元格的行索引,从0开始,
	 * @param j AA区域单元格的列索引,从0开始,
	 * @return
	 */
	public Optional<Cell> cellOpt(final int i, final int j) {
		return Optional.ofNullable(this.ltCell).map(cell -> {
			final int origin_x = cell.getRowIndex(); // 行索引
			final int origin_y = cell.getColumnIndex(); // 列索引
			final int _x = origin_x + i;
			final int _y = origin_y + j;
			return this.excel.getOrCreateCell(cell.getSheet(), _x, _y); //
		});
	}

	/**
	 * 下一个区域
	 * 
	 * @param width
	 * @param height
	 */
	public AffectedArea next(final int width, final int height) {
		return new AffectedArea(this.excel, this.activeCell(), width, height);
	}

	/**
	 * 下一个区域
	 * 
	 * @param width
	 * @param height
	 */
	public AffectedArea next() {
		return this.next(1, 1);
	}

	/**
	 * 基点位置
	 * 
	 * @return
	 */
	public Cell origin() {
		return this.ltCell;
	}

	/**
	 * nrows 别名
	 * 
	 * @return
	 */
	public int height() {
		return this.nrows();
	}

	/**
	 * 行数量(shape的1号位置)
	 * 
	 * @return
	 */
	public int nrows() {
		return this.shape._1;
	}

	/**
	 * ncols别名
	 * 
	 * @return
	 */
	public int width() {
		return this.ncols();
	}

	/**
	 * 列数量(shape的2号位置)
	 * 
	 * @return
	 */
	public int ncols() {
		return this.shape._2;
	}

	/**
	 * 右下角的 偏移位置
	 * 
	 * @param offset_x 行偏移
	 * @param offset_y 列偏移
	 * @return 右下角的 偏移位置
	 */
	public Cell offset(final int offset_x, final int offset_y) {
		return this.offsetOpt(offset_x, offset_y).orElse(null);
	}

	/**
	 * 生成新的长度空间
	 * 
	 * @param i      行偏移
	 * @param j      列偏移
	 * @param width  行长度
	 * @param height 列长度
	 * @return
	 */
	public AffectedArea span(final int i, final int j, final int width, final int height) {
		return new AffectedArea(this.excel, this.cell(i, j), width, height);
	}

	/**
	 * 生成新的长度空间
	 * 
	 * @param i      行偏移
	 * @param j      列偏移
	 * @param width  行长度
	 * @param height 列长度
	 * @return
	 */
	public AffectedArea span(final String rngName) {
		final var rng = DataMatrix.name2rdf(rngName);
		final var lt = rng.lt();
		return this.span(lt._1(), lt._2(), rng.width(), rng.height());
	}

	/**
	 * 右下角的 偏移位置的地址 (0,0) 右下角元素
	 * 
	 * @param offset_x 行偏移 从0开始
	 * @param offset_y 列偏移 从0开始
	 * @return 右下角的 偏移位置
	 */
	public String address(final int offset_x, final int offset_y) {
		return this.offsetOpt(offset_x, offset_y).map(e -> e.getAddress().toString()).orElse(null);
	}

	/**
	 * 书写位点:write操作的默认书写位置<br>
	 * AffectedArea被设计为记录一次书写完成以后的数据区域（受影响区域）,于是下一个可以书写的Cell就被称为ActiveCell <br>
	 * 当前活动Cell(比邻选区正下方的第一个cell:可以用写入数据的位置)
	 * 
	 * @return 当前活动Cell
	 */
	public Cell activeCell() {
		// 下一行 相同列。
		return this.offsetOpt(1, -this.width() + 1).orElse(null);
	}

	/**
	 * AffectedArea被视为一次书写完成以后得数据数据,于是下一个可以书写的Cell就被称为ActiveCell <br>
	 * 当前活动Cell(比邻选区正下方的第一个cell)的地址
	 * 
	 * @return 当前活动Cell的地址
	 */
	public String activeAddress() {
		return Optional.ofNullable(this.activeCell()).map(e -> e.getAddress().toString()) //
				.orElse("A1"); // 默认地址为A1
	}

	/**
	 * 书写一行 <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>  元素类型
	 * @param line 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	@SafeVarargs
	public final <T> AffectedArea writeLine(T... line) {
		return this.writeLine(this.activeAddress(), line);
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>   元素类型
	 * @param lines 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeLines(T lines[][]) {
		return this.writeLines(this.activeAddress(), lines);
	}

	/**
	 * 书写1列 <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>  元素类型
	 * @param line 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	@SafeVarargs
	public final <T> AffectedArea writeColumn(T... line) {
		final String[][] lines = DataMatrix
				.of(Stream.of(line).map(e -> e instanceof String ? (String) e : e + "").collect(Collectors.toList()))
				.transpose().data(); // 转置
		return this.writeLines(this.activeAddress(), lines);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>   元素类型
	 * @param datas 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeTable(final DataMatrix<T> datas) {
		return this.writeTable(this.activeAddress(), datas);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>   元素类型
	 * @param datas 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeTable(final DFrame datas) {
		final var origin = this.origin();
		final var origin_address = origin.getAddress();
		final var active_address = this.activeAddress();
		if (AffectedArea.debug) {
			System.err.println("writeTable:origin:%s,active:%s(write at)".formatted(origin_address, active_address));
		}
		return this.writeTable(active_address, datas);
	}

	/**
	 * 书写一行(writeLine的别名) <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址)
	 * @param line    数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	@SafeVarargs
	public final <T> AffectedArea writeRow(final String address, T... line) {
		return this.writeLine(line);
	}

	/**
	 * 书写一行 <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址)
	 * @param line    数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	@SafeVarargs
	public final <T> AffectedArea writeLine(final String address, T... line) {
		this.excel.write(address, line);
		return this.excel.getAffectedArea();
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址)
	 * @param lines   数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeLines(final String address, final T lines[][]) {
		this.excel.write(address, lines);
		return this.excel.getAffectedArea();
	}

	/**
	 * 书写1列 <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址)
	 * @param line    数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	@SafeVarargs
	public final <T> AffectedArea writeColumn(final String address, final T... line) {
		final T[][] lines = DataMatrix.of(line).transpose().data();
		return this.writeLines(address, lines);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址
	 * @param datas   数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeTable(final String address, final DataMatrix<T> datas) {
		this.excel.write(address, datas);
		return this.excel.getAffectedArea();
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址
	 * @param datas   数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeTable(final String address, final DFrame datas) {
		this.excel.write(address, datas);
		return this.excel.getAffectedArea();
	}

	/**
	 * 格式喷涂
	 * 
	 * @param style 单元格式样
	 * @return
	 */
	public AffectedArea paint(final CellStyle style) {
		this.forEach(cell -> cell.setCellStyle(style));
		return this;
	}

	/**
	 * 格式喷涂
	 * 
	 * @param pcs 单元格式样
	 * @return
	 */
	public AffectedArea paint(final Pack<CellStyle> pcs) {
		Optional.ofNullable(pcs).ifPresent(p -> p.peek(this::paint));
		return this;
	}

	/**
	 * 格式喷涂
	 * 
	 * @param pstyle 单元格式样
	 * @return
	 */
	public AffectedArea paint() {
		return this.paint(this.pcs);
	}

	/**
	 * 设置区域喷涂
	 * 
	 * @param prepare 喷涂式样转呗
	 * @return
	 */
	public AffectedArea paint(final Consumer<CellStyle> prepare) {
		return this.paint(excel.packCellStyle().peek(prepare));
	}

	/**
	 * 头部行 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintHead(final IndexedColors color) {
		this.paint(style -> {
			style.setBorderTop(BorderStyle.THIN);
			style.setBottomBorderColor(color.getIndex());
			style.setBorderBottom(BorderStyle.THICK);
			style.setBottomBorderColor(color.getIndex());
		});
		return this;
	}

	/**
	 * 尾部行 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintLast(final IndexedColors color) {
		this.paint(style -> {
			style.setBorderBottom(BorderStyle.THIN);
			style.setBottomBorderColor(color.getIndex());
		});
		return this;
	}

	/**
	 * 设置背景色
	 * 
	 * @param color
	 * @return
	 */
	public AffectedArea background(final IndexedColors color) {
		this.pcs().peek(SimpleExcel.background_color.apply(color)).peek(this::paint);
		return this;
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea border() {
		return this.border(null, null, null);
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea border(final BorderName borderName, final IndexedColors color) {
		return this.border(borderName, null, color);
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea bottomThick(final IndexedColors color) {
		return this.border(BorderName.BOTTOM, BorderStyle.THICK,
				Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea bottomThin(final IndexedColors color) {
		return this.border(BorderName.BOTTOM, BorderStyle.THIN, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea bottomDouble(final IndexedColors color) {
		return this.border(BorderName.BOTTOM, BorderStyle.DOUBLE,
				Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea topThick(final IndexedColors color) {
		return this.border(BorderName.TOP, BorderStyle.THICK, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea topThin(final IndexedColors color) {
		return this.border(BorderName.TOP, BorderStyle.THIN, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea topDouble(final IndexedColors color) {
		return this.border(BorderName.TOP, BorderStyle.DOUBLE, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea border(final BorderName borderName, BorderStyle borderStyle, final IndexedColors color) {
		final var bn = Optional.ofNullable(borderName).orElse(BorderName.ALL);
		final var bs = Optional.ofNullable(borderStyle).orElse(BorderStyle.THIN);
		final var clr = Optional.ofNullable(color).orElse(IndexedColors.BLACK);
		this.pcs().peek(SimpleExcel.border_color.apply(bs).apply(bn, clr)).peek(this::paint);
		return this;
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName
	 * @param color
	 * @return
	 */
	public AffectedArea color(final IndexedColors color) {
		excel.packFont().peek(font -> font.setColor(color.getIndex()))
				.flatMap(font -> this.pcs().peek(e -> e.setFont(font))).peek(this::paint);
		return this;
	}

	/**
	 * 设置或是取消字体加黑
	 * 
	 * @param bold 是否字体加黑
	 * @return
	 */
	public AffectedArea bold(final boolean bold) {
		excel.packFont().peek(font -> font.setBold(bold)).flatMap(font -> this.pcs().peek(e -> e.setFont(font)))
				.peek(this::paint);
		return this;
	}

	/**
	 * 字体加黑
	 * 
	 * @return
	 */
	public AffectedArea bold() {
		return bold(true);
	}

	/**
	 * 设置或是取消字体倾斜
	 * 
	 * @param italic 是否字体倾斜
	 * @return
	 */
	public AffectedArea italic(final boolean italic) {
		excel.packFont().peek(font -> font.setItalic(italic)).flatMap(font -> this.pcs().peek(e -> e.setFont(font)))
				.peek(this::paint);
		return this;
	}

	/**
	 * 设置或是取消字体倾斜
	 * 
	 * @return
	 */
	public AffectedArea italic() {
		return this.italic(true);
	}

	/**
	 * 数据行(rowS的别名)
	 * 
	 * @return
	 */
	public Stream<AffectedArea> lineS() {
		return this.rowS();
	}

	/**
	 * 数据行
	 * 
	 * @return
	 */
	public Stream<AffectedArea> rowS() {
		return Stream.iterate(0, i -> i + 1).limit(this.height())
				.map(i -> new AffectedArea(this.excel, this.cell(i, 0), 1, this.width())); // 行高度为1
	}

	/**
	 * 数据行
	 * 
	 * @return
	 */
	public List<AffectedArea> rows() {
		return this.rowS().toList();
	}

	/**
	 * 
	 * @param n 航索引
	 * @return
	 */
	public Optional<AffectedArea> rowOpt(final int n) {
		return Optional.ofNullable(0 <= n && n < this.nrows() ? n : null)
				.map(i -> new AffectedArea(this.excel, this.cell(i, 0), 1, this.width()));
	}

	/**
	 * 
	 * @param n 航索引
	 * @return
	 */
	public AffectedArea row(final int n) {
		return this.rowOpt(n).orElse(null);
	}

	/**
	 * 
	 * @return
	 */
	public AffectedArea firstRow() {
		return this.row(0);
	}

	/**
	 * 
	 * @return
	 */
	public AffectedArea lastRow() {
		return this.row(this.nrows() - 1);
	}

	/**
	 * 数据列
	 * 
	 * @return
	 */
	public Stream<AffectedArea> colS() {
		return Stream.iterate(0, i -> i + 1).limit(this.width())
				.map(i -> new AffectedArea(this.excel, this.cell(0, i), this.height(), 1)); // 列宽度为1
	}

	/**
	 * 数据列
	 * 
	 * @return
	 */
	public List<AffectedArea> cols() {
		return this.colS().toList();
	}

	/**
	 * 
	 * @param n 航索引
	 * @return
	 */
	public Optional<AffectedArea> colOpt(final int n) {
		return Optional.ofNullable(0 <= n && n < this.nrows() ? n : null)
				.map(i -> new AffectedArea(this.excel, this.cell(0, i), this.height(), 1));
	}

	/**
	 * 
	 * @param n 航索引
	 * @return
	 */
	public AffectedArea col(final int n) {
		return this.colOpt(n).orElse(null);
	}

	/**
	 * 
	 * @return
	 */
	public AffectedArea firstCol() {
		return this.col(0);
	}

	/**
	 * 
	 * @return
	 */
	public AffectedArea lastCol() {
		return this.col(this.ncols() - 1);
	}

	/**
	 * 设置选区
	 * 
	 * @return
	 */
	public AffectedArea focus() {
		this.excel.setAffectedArea(this);
		return this;
	}

	/**
	 * 数据书写（选区内数据写入）
	 * 
	 * @param <T> 数据类型
	 * @param ts  吸入的数据行
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> AffectedArea update(final T... ts) {
		return this.updateLine(true, ts);
	}

	/**
	 * 数据书写（选区内数据写入）
	 * 
	 * @param <T>  数据类型
	 * @param flag ts 书写完毕后是否结束写入, true:结束写入,false:继续写入,采用ts的循环写入
	 * @param ts   书写的数据内容
	 * @return
	 */
	public <T> AffectedArea updateLine(final boolean flag, final T[] ts) {
		Optional.ofNullable(ts).map(e -> e.length > 0 ? e : null).ifPresent(data -> {
			var i = 0; // 写入索引
			final var n = ts.length; // 数据长度

			for (final var e : this.cells()) {
				if (flag && i >= n) { // 信息书写完毕
					break;
				}
				final var value = data[(i++) % n];
				if (value instanceof Number num) {
					e.setCellValue(num.doubleValue());
				} else if (value instanceof Date dt) {
					e.setCellValue(dt);
				} else if (value instanceof Boolean bool) {
					e.setCellValue(bool);
				} else if (value instanceof LocalDate ldt) {
					e.setCellValue(ldt);
				} else if (value instanceof RichTextString rts) {
					e.setCellValue(rts);
				} else if (value instanceof Calendar calendar) {
					e.setCellValue(calendar);
				} else { //
					e.setCellValue(String.valueOf(value));
				}

			}
		});
		return this;
	}

	/**
	 * 提取数据流
	 * 
	 * @param <T>
	 * @param mapper 单元格变换对象 cell-&gt;T
	 * @return 流对象
	 */
	public <T> Stream<T> stream(final Function<? super Cell, T> mapper) {
		return this.cellS().map(mapper);
	}

	/**
	 * 单元格流序列（行顺序）
	 * 
	 * @return
	 */
	public Stream<Cell> cellS() {
		return Stream.iterate(0, i -> i + 1).limit(this.nrows())
				.flatMap(i -> Stream.iterate(0, j -> j + 1).limit(this.ncols()).map(j -> this.cell(i, j)));

	}

	/**
	 * 单元格流序列（行顺序）
	 * 
	 * @return
	 */
	public List<Cell> cells() {
		return this.cellS().toList();
	}

	/**
	 * 
	 * @return
	 */
	public AffectedArea save() {
		this.excel.save();
		return this;
	}

	@Override
	public Iterator<Cell> iterator() {
		return this.cellS().iterator();
	}

	@Override
	public String toString() {
		return "AffectedArea [startCell=%s(%d,%d), shape=(rows:%s,cols:%s)]".formatted(ltCell, ltCell.getRowIndex(),
				ltCell.getColumnIndex(), this.nrows(), this.ncols());
	}

	/**
	 * 详情罗列
	 * 
	 * @return 详情罗列
	 */
	public String dump() {
		return "{%s:%s}".formatted(this.toString(), this.rowS().map(e -> "[%s]".formatted(e.cellS().map(String::valueOf) //
				.collect(Collectors.joining(",")))).collect(Collectors.joining("\n")));
	}

	/**
	 * 单位会话处理
	 * 
	 * @param action 单位会话
	 * @return
	 */
	public AffectedArea withTransaction(final Consumer<AffectedArea> action) {
		action.accept(this);
		return this;
	}

	/**
	 * 属性清空
	 * 
	 * @return
	 */
	public synchronized AffectedArea clear() {
		this.pcs = null;
		return this;
	}

	/**
	 * 获取 Pack CellStyle
	 * 
	 * @param cs 式样如处理器
	 * @return Pack CellStyle
	 */
	public Pack<CellStyle> pcs(final Consumer<CellStyle> cs) {
		return this.pcs().peek(cs);
	}

	/**
	 * 获取 Pack CellStyle
	 * 
	 * @return Pack CellStyle
	 */
	public synchronized Pack<CellStyle> pcs() {
		return this.pcs == null ? this.pcs = excel.packCellStyle() : this.pcs;
	}

	private Pack<CellStyle> pcs;

	/**
	 * 左上角的单元格
	 */
	private Cell ltCell;

	/**
	 * 行数量(shape的1号位置) <br>
	 * 列数量(shape的2号位置)
	 */
	private Tuple2<Integer, Integer> shape;

	/**
	 * excel
	 */
	public final SimpleExcel excel;

	/**
	 * 调试标记
	 */
	public static boolean debug = false;

}