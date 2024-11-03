package gbench.util.data.xls;

import static gbench.util.data.xls.DataMatrix.xlsn;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;

import gbench.util.data.xls.SimpleExcel.BorderName;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Pack;
import gbench.util.type.Types;

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
	 * 获取区域名称
	 * 
	 * @return
	 */
	public String getName() {
		return this.getName(false);
	}

	/**
	 * 获取区域名称（比如:Sheet1!A1:B2)
	 *
	 * @param flag 是否显示sheet名称,true:显示,false:不显示
	 * @return
	 */
	public String getName(final boolean flag) {
		return Optional.ofNullable(this.ltCell).map(cell -> {
			final var sheetName = cell.getSheet().getSheetName();
			final var x0 = cell.getRowIndex();
			final var y0 = cell.getColumnIndex();
			final var x1 = x0 + (this.nrows() - 1);
			final var y1 = y0 + (this.ncols() - 1);

			return "%s%s%s:%s%s".formatted(
					Optional.ofNullable(flag ? sheetName : null).map("%s!"::formatted).orElse(""), // sheet前缀
					xlsn(y0), x0 + 1, xlsn(y1), x1 + 1); // x0+1, x1+1是吧0based索引号转换成行号
		}).orElse(null);
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
			final var line = "清洁区的writeTable:origin:%s(affected:内容/感染区),active:%s(write at:清洁区)" //
					.formatted(origin_address, active_address);
			System.err.println(line);
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
	public final <T> AffectedArea writeLine(final String address, Iterable<T> line) {
		return this.writeLine(address, Types.itr2array(line));
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
	 * 书写一行 <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param action 书写回调,(point:AffectedArea)->{}
	 * @param n      书写行长度
	 * @return AffectedArea
	 */
	public final <T> AffectedArea writeLine(final BiConsumer<AffectedArea, Integer> action, final int n) {
		final var point = this.writePoint();
		for (var i = 0; i < n; i++) {
			final var p = point.hshift(i); // 水平平移
			action.accept(p, i);
		}
		return this.writePoint().extend(0, n);
	}

	/**
	 * 区间拉伸
	 * 
	 * @param nrows
	 * @param ncols
	 * @return
	 */
	public AffectedArea extend(int nrows, int ncols) {
		return this.create(this.origin(), nrows + this.nrows(), ncols + this.ncols());
	}

	/**
	 * 区间拉伸
	 * 
	 * @param ncols 水平拉伸列宽
	 * @return
	 */
	public AffectedArea hextend(int ncols) {
		return this.extend(0, ncols);
	}

	/**
	 * 区间拉伸
	 * 
	 * @param ncols 垂直拉伸行高
	 * @return
	 */
	public AffectedArea vextend(int nrows) {
		return this.extend(nrows, 0);
	}

	/**
	 * 起点(区域起始位置）
	 * 
	 * @return
	 */
	public AffectedArea startPoint() {
		return this.singleton(this.origin());
	}

	/**
	 * 写入点
	 * 
	 * @return
	 */
	public AffectedArea writePoint() {
		return this.singleton(this.activeCell());
	}

	/**
	 * 单点
	 * 
	 * @param cell
	 * @return
	 */
	public AffectedArea singleton(final Cell cell) {
		return create(cell, 1, 1);
	}

	/**
	 * 垂直平移，然后垂直扩展
	 * 
	 * @param s_nrows 平移行数
	 * @param e_nrows 扩展行数
	 * @return
	 */
	public AffectedArea vsve(int s_nrows, int e_nrows) {
		return this.vshift(s_nrows).vextend(e_nrows);
	}

	/**
	 * 水平平移
	 * 
	 * @param nrows
	 * @param ncols
	 * @return
	 */
	public AffectedArea vshift(int nrows) {
		return this.shift(nrows, 0);
	}

	/**
	 * 水平平移
	 * 
	 * @param ncols 水平平移列数量
	 * @return
	 */
	public AffectedArea hshift(int ncols) {
		return this.shift(0, ncols);
	}

	/**
	 * 平移
	 * 
	 * @param nrows 垂直平移行数量
	 * @param ncols 水平平移列数量
	 * @return
	 */
	public AffectedArea shift(int nrows, int ncols) {
		final var x0 = this.ltCell.getRowIndex();
		final var y0 = this.ltCell.getColumnIndex();
		final var newcell = excel.getOrCreateCell(x0 + nrows, y0 + ncols);
		return create(newcell, this.nrows(), this.ncols());
	}

	/**
	 * 创建一个 AffectedArea
	 * 
	 * @param cell   基点位置
	 * @param height 高度
	 * @param width  宽度
	 * @return
	 */
	public AffectedArea create(final Cell cell, int height, int width) {
		return new AffectedArea(this.excel, cell, height, width);
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
	 * @param newstyle 单元格式样
	 * @return
	 */
	public AffectedArea paint(final CellStyle newstyle) {
		excel.packWKCellStyle(null).valueOpt().ifPresent(s -> { // 创建一个新的式样
			s.cloneStyleFrom(newstyle);
			this.forEach(cell -> {
				cell.setCellStyle(s);
			});
		});

		return this;
	}

	/**
	 * 格式喷涂
	 * 
	 * @param pcs 单元格式样
	 * @return
	 */
	public AffectedArea paint(final Pack<CellStyle> pcs) {
		Optional.ofNullable(pcs).ifPresent(p -> p.evaluate(this::paint));
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
		return this.paint(excel.packWKCellStyle(this.origin()).evaluate(prepare));
	}

	/**
	 * 喷绘制标题 <br>
	 * 
	 * @return
	 */
	public AffectedArea ptitle(final IndexedColors color) {
		return this.paintTitle(color);
	}

	/**
	 * 喷绘制标题 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintTitle(final IndexedColors color) {
		final var top = this.top();
		final var cell = top.origin();
		excel.packWKCellStyle(cell).valueOpt().map(s -> {
			s.setBorderTop(BorderStyle.THIN);
			s.setTopBorderColor(color.getIndex());
			s.setFont(excel.packWKFont(cell).evaluate(font -> {
				font.setBold(true);
			}));
			s.setBorderBottom(BorderStyle.THICK);
			s.setBottomBorderColor(color.getIndex());
			s.setAlignment(HorizontalAlignment.CENTER);
			return top().paint(s);
		}).orElse(null);
		return this;
	}

	/**
	 * 喷绘制顶部 <br>
	 * 
	 * @return
	 */
	public AffectedArea ptop(final IndexedColors color) {
		return this.paintTop(color, null);
	}

	/**
	 * 喷绘制顶部 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintTop(final IndexedColors color, final BorderStyle bs) {
		final var top = this.top();
		excel.packWKCellStyle(top.origin()).valueOpt().map(s -> {
			s.setBorderTop(Optional.ofNullable(bs).orElse(BorderStyle.THIN));
			s.setTopBorderColor(color.getIndex());
			return top.paint(s);
		}).orElse(null);
		return this;
	}

	/**
	 * 喷绘制顶部 <br>
	 * 
	 * @return
	 */
	public AffectedArea pbottom(final IndexedColors color) {
		return this.paintBottom(color, null);
	}

	/**
	 * 喷绘制顶部 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintBottom(final IndexedColors color, final BorderStyle bs) {
		final var bottom = this.bottom();
		excel.packWKCellStyle(this.bottom().origin()).valueOpt().map(s -> {
			s.setBorderBottom(Optional.ofNullable(bs).orElse(BorderStyle.THIN));
			s.setBottomBorderColor(color.getIndex());
			return bottom.paint(s);
		}).orElse(null);
		return this;
	}

	/**
	 * 喷绘制左侧 <br>
	 * 
	 * @return
	 */
	public AffectedArea pleft(final IndexedColors color) {
		return this.paintLeft(color, null);
	}

	/**
	 * 喷绘制左侧 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintLeft(final IndexedColors color, final BorderStyle bs) {
		this.left().forEach(cell -> {
			excel.packWKCellStyle(cell).wrap(style -> {
				style.setBorderLeft(Optional.ofNullable(bs).orElse(BorderStyle.THIN));
				style.setLeftBorderColor(color.getIndex());
			}).evaluate(cell::setCellStyle);
		});
		return this;
	}

	/**
	 * 喷绘制左侧 <br>
	 * 
	 * @return
	 */
	public AffectedArea pright(final IndexedColors color) {
		return this.paintRight(color, null);
	}

	/**
	 * 喷绘制右侧 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintRight(final IndexedColors color, final BorderStyle bs) {
		this.right().forEach(cell -> {
			excel.packWKCellStyle(cell).wrap(style -> {
				style.setBorderRight(Optional.ofNullable(bs).orElse(BorderStyle.THIN));
				style.setRightBorderColor(color.getIndex());
			}).evaluate(cell::setCellStyle);
		});
		return this;
	}

	/**
	 * 喷涂边框 <br>
	 * 
	 * @return
	 */
	public AffectedArea poutline(final IndexedColors color) {
		return this.paintOutline(color, null);
	}

	/**
	 * 喷涂边框 <br>
	 * 
	 * @return
	 */
	public AffectedArea paintOutline(final IndexedColors color, final BorderStyle bs) {
		this.paintTop(color, null);
		this.paintBottom(color, null);
		this.paintLeft(color, null);
		this.paintRight(color, null);
		return this;
	}

	/**
	 * 设置背景色
	 * 
	 * @param color
	 * @return
	 */
	public AffectedArea background(final IndexedColors color) {
		this.pcs(SimpleExcel.background_color.apply(color)).evaluate(this::paint);
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
		this.pcs(SimpleExcel.border_color.apply(bs).apply(bn, clr)).evaluate(this::paint);

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
		excel.packWKFont(this.ltCell).wrap(font -> font.setColor(color.getIndex()))
				.flatMap(font -> this.pcs(e -> e.setFont(font))).evaluate(this::paint);
		return this;
	}

	/**
	 * 设置或是取消字体加黑
	 * 
	 * @param bold 是否字体加黑
	 * @return
	 */
	public AffectedArea bold(final boolean bold) {
		excel.packWKFont(this.origin()).wrap(font -> font.setBold(bold)).flatMap(font -> this.pcs(e -> e.setFont(font)))
				.evaluate(this::paint);
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
		excel.packWKFont(this.origin()).wrap(font -> font.setItalic(italic))
				.flatMap(font -> this.pcs(e -> e.setFont(font))).wrap(this::paint);
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
	 * 区域最后一行（顶部的别名）
	 * 
	 * @return
	 */
	public AffectedArea firstRow() {
		return this.row(0);
	}

	/**
	 * 区域最后一行（底部的别名）
	 * 
	 * @return
	 */
	public AffectedArea lastRow() {
		return this.row(this.nrows() - 1);
	}

	/**
	 * 区域顶部
	 * 
	 * @return
	 */
	public AffectedArea top() {
		return this.row(0);
	}

	/**
	 * 区域底部
	 * 
	 * @return
	 */
	public AffectedArea bottom() {
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
	 * 最后一列
	 * 
	 * @return
	 */
	public AffectedArea lastCol() {
		return this.col(this.ncols() - 1);
	}

	/**
	 * 区域左侧
	 * 
	 * @return
	 */
	public AffectedArea left() {
		return this.col(0);
	}

	/**
	 * 区域右侧
	 * 
	 * @return
	 */
	public AffectedArea right() {
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
	 * 公式写入书写（选区内数据写入）
	 * 
	 * @param <T>      数据类型
	 * @param formulas 单元格公式
	 * @return
	 */
	public <T> AffectedArea setCellFormula(final String... formulas) {
		Optional.ofNullable(formulas).ifPresent(fs -> {
			var i = 0;
			final var n = fs.length;
			for (final var cell : this) {
				final var formula = fs[i % n];
				cell.setCellFormula(formula);
			}
		});

		return null;
	}

	/**
	 * 数据书写（选区内数据写入）
	 * 
	 * @param <T> 数据类型
	 * @param ts  吸入的数据行
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> AffectedArea set(final T... ts) {
		return this.update(ts);
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
	 * @param <T> 数据类型
	 * @param ts  吸入的数据行
	 * @return
	 */
	public <T> AffectedArea update(final Iterable<T> ts) {
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
	public <T> AffectedArea updateLine(final boolean flag, final Iterable<T> ts) {
		return this.updateLine(flag, Types.itr2array(ts));
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
		return this.pcs().wrap(cs);
	}

	/**
	 * 获取 Pack CellStyle
	 * 
	 * @return Pack CellStyle
	 */
	public synchronized Pack<CellStyle> pcs() {
		return this.pcs == null ? this.pcs = excel.packWKCellStyle(this.origin()) : this.pcs;
	}

	/**
	 * 
	 */
	@Override
	public int hashCode() {
		return Objects.hash(pcs, ltCell, shape, excel);
	}

	/**
	 * 
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof AffectedArea aa) {
			return Objects.equals(this.pcs, aa.pcs) && Objects.equals(this.pcs, aa.pcs)
					&& Objects.equals(this.shape, aa.shape) && Objects.equals(this.excel, aa.excel);
		}

		return false;
	}

	@Override
	public String toString() {
		return this.getName(true);
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