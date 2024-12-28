package gbench.util.data.xls;

import static gbench.util.data.xls.DataMatrix.xlsn;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;

import gbench.util.data.xls.SimpleExcel.BorderName;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
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
	 * @param shape  (1:nrows,2:ncols), 如果 nrows, ncols 小于0, 实际的 ltCell
	 *               将沿着逆方向（向上，相左）进行对应位置的调整。比如 <br>
	 *               1) C3,(-1,-2) 等价于 A2,(1,2); <br>
	 *               2) C3,(1,-2) 等价于 A2,(1,2); <br>
	 * @param excel  excel对象
	 */
	public AffectedArea(final SimpleExcel excel, final Cell ltCell, final Tuple2<Integer, Integer> shape,
			final Pack<CellStyle> pcs) {
		this.excel = excel; // excel
		final var nrows = shape._1; // 高度
		final var ncols = shape._2; // 宽度
		this.ltCell = this.adjustLtCell(ltCell, nrows, ncols); // 节点对象
		this.shape = Tuple2.of(Math.abs(nrows), Math.abs(ncols)); // 对象尺寸:nrows,ncols
		this.pcs = pcs; // 式样信息
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
		this(excel, excel.cell(rdf.sheetName(), rdf.x0() + startx, rdf.y0() + starty),
				Tuple2.of(rdf.nrows(), rdf.ncols()));
	}

	/**
	 * 
	 * 根据nrows,ncols的正负情况,调整LtCell(左上角Cell)
	 * 
	 * @param cell  待调整的ltCell
	 * @param nrows 行偏移
	 * @param ncols 列偏移
	 * @return
	 */
	public Cell adjustLtCell(final Cell cell, final int nrows, final int ncols) {
		if (null == cell) { // 无效cell返回null
			return null;
		} else { // cell 不为空
			final var origin_irow = cell.getRowIndex(); // 原来行索引
			final var origin_icol = cell.getColumnIndex(); // 原来列索引
			var irow = origin_irow; // 调整后行索引：初始化origin_irow
			var icol = origin_icol; // 调整后的列索引：初始化为 origin_icol

			if (nrows < 0) { // 行数量小于0:调整移动行索引
				irow = Math.max(irow + nrows, 0); // 更新行索引(向上运动）
			} else {
				// do nothing
			}

			if (ncols < 0) { // 列数量小于0:调整移动列索引
				icol = Math.max(icol + ncols, 0); // 更新列索引（向左运动
			} else {
				// do nothing
			}

			final var ltcell = excel.getOrCreateCell(cell.getSheet(), //
					Math.min(origin_irow, irow), // 选择左上端的行索引
					Math.min(origin_icol, icol) // 选择左边的列索引
			); // 移动后的ltCell

			return ltcell;
		} // if
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
	 * getSheetOpt 区域所在表单对象
	 * 
	 * @return
	 */
	public Optional<Sheet> getSheetOpt() {
		return this.originOpt().map(e -> e.getSheet());
	}

	/**
	 * getSheet 区域所在表单对象
	 * 
	 * @return
	 */
	public Sheet getSheet() {
		return this.getSheetOpt().orElse(null);
	}

	/**
	 * getSheetName 区域所在表单对象名称
	 * 
	 * @return
	 */
	public String getSheetName() {
		return this.getSheetOpt().map(e -> e.getSheetName()).orElse(null);
	}

	/**
	 * 获取区域名称（例如：A1:B2）
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
	 * @return 区域名称类似于EXCEL的RANGE名称,ltCell为null时返回null
	 */
	public String getName(final boolean flag) {
		return Optional.ofNullable(this.ltCell).map(cell -> { // cell 有效
			final var sheetName = cell.getSheet().getSheetName(); // 表单名称
			final var x0 = cell.getRowIndex(); // LT行索引
			final var y0 = cell.getColumnIndex(); // LT列索引
			final var x1 = x0 + (this.nrows() - 1); // RB 行索引
			final var y1 = y0 + (this.ncols() - 1); // RB 列索引
			final var prefix = Optional.ofNullable(flag ? sheetName : null).map("%s!"::formatted).orElse(""); // 前缀
			return "%s%s%s:%s%s".formatted(prefix, xlsn(y0), x0 + 1, xlsn(y1), x1 + 1); // x0+1, x1+1是把0based索引号转换成行号
		}).orElse(null);
	}

	/**
	 * 行索引左上（INDEX OF TOP ）
	 * 
	 * @return
	 */
	public Integer itop() {
		return Optional.ofNullable(this.ltCell).map(cell -> cell.getRowIndex()).orElse(null);
	}

	/**
	 * 行索引左上（INDEX OF LEFT ）
	 * 
	 * @return
	 */
	public Integer ileft() {
		return Optional.ofNullable(this.ltCell).map(cell -> cell.getColumnIndex()).orElse(null);
	}

	/**
	 * 行索引左上（INDEX OF BOTTOM ）
	 * 
	 * @return
	 */
	public Integer ibottom() {
		return Optional.ofNullable(this.ltCell).map(cell -> cell.getRowIndex() + this.nrows() - 1).orElse(null);
	}

	/**
	 * 行索引左上（INDEX OF RIGHT ）
	 * 
	 * @return
	 */
	public Integer iright() {
		return Optional.ofNullable(this.ltCell).map(cell -> cell.getColumnIndex() + this.ncols() - 1).orElse(null);
	}

	/**
	 * 相对向定位<br>
	 * 
	 * this.cell(0,0)表示AA的基点单元格(ltCell) <br>
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
	 * 把起始点转换为影响区
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea ltAa() {
		return create(this.ltCell);
	}

	/**
	 * 基点单元格
	 * 
	 * @return Cell
	 */
	public Cell ltCell() {
		return this.ltCell;
	}

	/**
	 * Optional ltCell
	 * 
	 * @return Optional ltCell
	 */
	public Optional<Cell> ltOpt() {
		return Optional.ofNullable(this.ltCell);
	}

	/**
	 * 基点位置
	 * 
	 * @return Cell
	 */
	public Cell origin() {
		return this.ltCell;
	}

	/**
	 * 把起始点转换为影响区(ltAa的别名)
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea originAa() {
		return this.ltAa();
	}

	/**
	 * Optional ltCell
	 * 
	 * @return Optional ltCell
	 */
	public Optional<Cell> originOpt() {
		return this.ltOpt();
	}

	/**
	 * originAddress
	 * 
	 * @return originAddress
	 */
	public String originAddress() {
		return Optional.of(this.origin()).map(e -> e.getAddress()).map(String::valueOf).orElse(null);
	}

	/**
	 * 把起始点转换为影响区
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea rbAa() {
		return create(this.rbCell());
	}

	/**
	 * 右下角单元格
	 * 
	 * @return Cell 单元格
	 */
	public Cell rbCell() {
		return this.rbCellOpt().orElse(null);
	}

	/**
	 * 右下角单元格
	 * 
	 * @return Optional Cell
	 */
	public Optional<Cell> rbCellOpt() {
		return Optional.ofNullable(this.cell(this.nrows() - 1, this.ncols() - 1));
	}

	/**
	 * 起点(区域起始位置）
	 * 
	 * @return
	 */
	public AffectedArea startPoint() {
		return this.create(this.origin());
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
	 * 写入点
	 * 
	 * @return
	 */
	public AffectedArea writePoint() {
		return this.create(this.activeCell());
	}

	/**
	 * 右下角(RB)的 偏移位置。 <br>
	 * offset(0,0) 为 区域的右下角的cell
	 * 
	 * @param nrows 行偏移数量大于等于0的整数
	 * @param ncols 列偏移数量大于等于0的整数
	 * @return 右下角RB的偏移位置
	 */
	public Optional<Cell> offsetOpt(final int nrows, final int ncols) {
		return this.rbCellOpt().map(cell -> {
			final var irow = cell.getRowIndex() + nrows; // 目标行索引
			final var icol = cell.getColumnIndex() + ncols; // 目标列索引
			final var sheet = cell.getSheet(); // 单元格所在sheet
			return this.excel.getOrCreateCell(sheet, irow, icol); // 提取单元格
		}); // Optional
	}

	/**
	 * 右下角(RB)的 偏移位置
	 * 
	 * @param offset_x 行偏移,从0开始
	 * @param offset_y 列偏移,从0开始
	 * @return 右下角的偏移位置
	 */
	public Cell offset(final int offset_x, final int offset_y) {
		return this.offsetOpt(offset_x, offset_y).orElse(null);
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
		return this.offsetOpt(1, -this.width() + 1).orElse(null); // ltCell的直接正下（下一行相同列）。
	}

	/**
	 * AffectedArea被视为一次书写完成以后得数据数据,于是下一个可以书写的Cell就被称为ActiveCell <br>
	 * 当前活动Cell(比邻选区正下方的第一个cell)的地址
	 * 
	 * @return 当前活动Cell的地址
	 */
	public String activeAddress() {
		return Optional.ofNullable(this.activeCell()).map(AffectedArea::fullAddress).orElse("Sheet1!A1"); // 默认地址为A1
	}

	/**
	 * 单点(ltCell:1x1的AffectedArea)
	 * 
	 * @param ltCell 基点,左上角的单元格的位置
	 * @return AffectedArea
	 */
	public AffectedArea create(final Cell ltCell) {
		return create(ltCell, 1, 1);
	}

	/**
	 * 创建一个 AffectedArea
	 * 
	 * @param ltCell 基点,左上角的单元格的位置
	 * @param nrows  行数量即高度大于等于1的正数
	 * @param ncols  列数量即宽度大于等于1的正数
	 * @return AffectedArea
	 */
	public AffectedArea create(final Cell ltCell, int nrows, int ncols) {
		return new AffectedArea(this.excel, ltCell, nrows, ncols);
	}

	/**
	 * 以当前AA为基准进行区域AffectedArea构建
	 * 
	 * @param nameline EXCEL的Range的描述字符串如 A1:B2 <br>
	 *                 如果 nameline 只含有一个cell没有":"给予补充A1作为左上cell. <br>
	 *                 即nameline如果只有B2将会补充A1作为头前ltCell
	 * @return AffectedArea
	 */
	public AffectedArea relativeCreate(final String nameline) {
		return Optional.ofNullable(nameline) //
				.map(e -> e.matches("\s*") ? "A1" : e) // 空白视为A1
				.map(e -> e.indexOf("!") < 0 ? "%s!%s".formatted(this.getSheetName(), e) : e) // 补充表单名
				.map(e -> e.indexOf(":") < 0 // nameline补充，是否缺少ltCell
						? e.replace("!", "!A1:") // 补充A1作为ltCell
						: e // 不缺少保持不变
				).map(DataMatrix::name2rdf) // 解析成RangeDef
				.flatMap(rdf -> this.cellOpt(rdf.x0(), rdf.y0()) // 解析成ltCell
						.map(ltCell -> this.create(ltCell, rdf.nrows(), rdf.ncols())))
				.orElse(null);
	}

	/**
	 * 以当前AA为基准进行区域AffectedArea构建
	 * 
	 * @param nameline EXCEL的Range的描述字符串，如:"A1:C1",<br>
	 *                 注意单个cell比如‘C1’等价于‘A1:C1’它会自动补充头前缺失的A1,<br>
	 *                 所以如果只想标注一个Cell请请使用 "A1:A1"这样的完整语法
	 * @return AffectedArea
	 */
	public AffectedArea rangeAa(final String nameline) {
		return this.relativeCreate(nameline);
	}

	/**
	 * ncols别名
	 * 
	 * @return 宽度
	 */
	public int width() {
		return this.ncols();
	}

	/**
	 * nrows 别名
	 * 
	 * @return 高度
	 */
	public int height() {
		return this.nrows();
	}

	/**
	 * 行数量(shape的1号位置)
	 * 
	 * @return 高度
	 */
	public int nrows() {
		return this.shape._1;
	}

	/**
	 * 返回一个高度为nrows的宽度不变的AffectedArea
	 * 
	 * @param nrows 缩放后的行数量
	 * @return AffectedArea
	 */
	public AffectedArea nrows(final int nrows) {
		return this.reshape(nrows, null);
	}

	/**
	 * 列数量(shape的2号位置)
	 * 
	 * @return 宽度
	 */
	public int ncols() {
		return this.shape._2;
	}

	/**
	 * 返回一个宽度为ncols的高度不变的AffectedArea
	 * 
	 * @param ncols 缩放后的列数量
	 * @return AffectedArea
	 */
	public AffectedArea ncols(final int ncols) {
		return reshape(null, ncols);
	}

	/**
	 * 区间拉伸（增长） <br>
	 * nrows,ncols的绝对值表示拉伸程度，正负号表示拉伸方向 <br>
	 * 也就是 无论 nrows，ncols 是正是负，返回的AffectedArea的尺寸shape都至少是原来的尺寸，<br>
	 * 也就是 不肯能出现 extend 以后 shape变小的情况是不存在的。
	 * 
	 * @param nrows: 大于0 表示向右扩展,小于0表示向左扩展
	 * @param ncols: 大于0 表示向下扩展,小于0表示向上扩展
	 * @return AffectedArea
	 */
	public AffectedArea extend(int nrows, int ncols) {
		final var cell = this.adjustLtCell(this.origin(), nrows, ncols);
		return this.create(cell, Math.abs(nrows) + this.nrows(), Math.abs(ncols) + this.ncols());
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
	 * 垂直平移，然后垂直扩展
	 * 
	 * @param s_nrows 平移行数
	 * @param e_nrows 扩展行数
	 * @return
	 */
	public AffectedArea vshe(int s_nrows, int e_ncols) {
		return this.vshift(s_nrows).hextend(e_ncols);
	}

	/**
	 * 垂直平移，然后垂直扩展
	 * 
	 * @param s_cols 平移行数
	 * @param e_rows 扩展行数
	 * @return AffectedArea
	 */
	public AffectedArea hsve(int s_cols, int e_rows) {
		return this.hshift(s_cols).vextend(e_rows);
	}

	/**
	 * 垂直平移，然后垂直扩展
	 * 
	 * @param s_ncols 平移行数
	 * @param e_ncols 扩展行数
	 * @return AffectedArea
	 */
	public AffectedArea hshe(int s_ncols, int e_ncols) {
		return this.hshift(s_ncols).hextend(e_ncols);
	}

	/**
	 * 水平平移(rows正数下移动,负数上移动)
	 * 
	 * @param nrows 垂直平移行数量:小于0表示向上移动,大于0表示向下移动，等于0不移动
	 * @return AffectedArea
	 */
	public AffectedArea vshift(int nrows) {
		return this.shift(nrows, 0);
	}

	/**
	 * 水平平移（ncols正数右移动,负数左移动）
	 * 
	 * @param ncols 水平平移列数量:小于0表示向左移动,大于0表示向右移动，等于0不移动
	 * @return AffectedArea
	 */
	public AffectedArea hshift(int ncols) {
		return this.shift(0, ncols);
	}

	/**
	 * 平移（nrows正数下移动,负数上移动;ncols正数右移动,负数左移动）
	 * 
	 * @param nrows 垂直平移行数量:小于0表示向上移动,大于0表示向下移动，等于0不移动
	 * @param ncols 水平平移列数量:小于0表示向左移动,大于0表示向右移动，等于0不移动
	 * @return AffectedArea
	 */
	public AffectedArea shift(int nrows, int ncols) {
		final var origin_irow = this.ltCell.getRowIndex();
		final var origin_icol = this.ltCell.getColumnIndex();
		final var new_irow = Math.max(origin_irow + nrows, 0);
		final var new_icol = Math.max(origin_icol + ncols, 0);
		final var newcell = excel.getOrCreateCell(this.ltCell.getSheet(), new_irow, new_icol);
		return create(newcell, this.nrows(), this.ncols());
	}

	/**
	 * 数据书写（选区内数据写入）
	 * 
	 * @param <T> 数据类型
	 * @param ts  吸入的数据行
	 * @return AffectedArea
	 */
	@SuppressWarnings("unchecked")
	public <T> AffectedArea set(final T... ts) {
		return this.update(ts);
	}

	/**
	 * 公式写入书写（选区内数据写入）
	 * 
	 * @param <T>      数据类型
	 * @param formulas 单元格公式
	 * @return AffectedArea
	 */
	public <T> AffectedArea setCellFormula(final String... formulas) {
		Optional.ofNullable(formulas).ifPresent(fs -> {
			var i = 0;
			final var n = fs.length;
			for (final var cell : this) {
				final var formula = fs[i % n];
				cell.setCellFormula(formula);
			} // for
		}); // ifPresent

		return this;
	}

	/**
	 * 数据书写（选区内数据写入）
	 * 
	 * @param <T> 数据类型
	 * @param ts  吸入的数据行
	 * @return AffectedArea
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
	 * @return AffectedArea
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
	 * @return AffectedArea
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
	 * @return AffectedArea
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
	 * 书写一行 <br>
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>  元素类型
	 * @param line 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	@SafeVarargs
	public final <T> AffectedArea writeLine(final T... line) {
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
	public <T> AffectedArea writeLines(final T lines[][]) {
		return this.writeLines(this.activeAddress(), lines);
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>   元素类型
	 * @param lines 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeLines(final Iterable<? extends Iterable<?>> lines) {
		return Optional.ofNullable(lines).map(lns -> StreamSupport.stream(lns.spliterator(), false)
				.map(line -> Optional.ofNullable(line)
						.map(ln -> StreamSupport.stream(ln.spliterator(), false).toArray()).orElse(new Object[0]))
				.toArray(Object[][]::new)).map(lns -> this.writeLines(this.activeAddress(), lns)).orElse(null);

	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param linesAa 数据内容行
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writeLines(final AffectedArea linesAa) {
		return Optional.ofNullable(linesAa).map(aa -> this.writeLines(aa.rows())).orElse(null);
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
	public final <T> AffectedArea writeColumn(final T... line) {
		final Object[][] lines = DataMatrix.of(Stream.of(line).collect(Collectors.toList())).transpose().data(); // 转置
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
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址
	 * @param datas   数据内容
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	public <T> AffectedArea writeTable(final String address, final DFrame datas) {
		this.excel.write(address, datas);
		return this.excel.getAffectedArea();
	}

	/**
	 * 写在lt位置<br>
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>   元素类型
	 * @param datas 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writelt(final DFrame dfm) {
		return this.writeTable(fullAddress(ltCell), dfm);
	}

	/**
	 * 写在lt位置<br>
	 * 写入excel (有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * @param <T>   元素类型
	 * @param datas 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> AffectedArea writelt(final DataMatrix<?> dmx) {
		return this.writeTable(fullAddress(ltCell), dmx);
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
	public <T> AffectedArea writeLine(final String address, Iterable<T> line) {
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
	public <T> AffectedArea writeLine(final BiConsumer<AffectedArea, Integer> action, final int n) {
		final var point = this.writePoint();
		for (var i = 0; i < n; i++) {
			final var p = point.hshift(i); // 水平平移
			action.accept(p, i);
		}
		return this.writePoint().extend(0, n - 1); // n-1 表示包含写入点在内的n个点
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>    元素类型
	 * @param addrAa 起始位置地址(全SHEET绝对地址:默认为originAddress)
	 * @return AffectedArea
	 */
	public <T> AffectedArea writeLines(final AffectedArea addrAa, final T lines[][]) {
		return this.writeLines(addrAa.originAddress(), lines);
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>    元素类型
	 * @param addrAa 起始位置地址(全SHEET绝对地址:默认为originAddress)
	 * @return AffectedArea
	 */
	public <T> AffectedArea writeLines(final AffectedArea addrAa, final AffectedArea lines) {
		return this.writeLines(addrAa.originAddress(), lines.toArray2());
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。
	 * 
	 * @param <T>     元素类型
	 * @param address 起始位置地址(全SHEET绝对地址)
	 * @param lines   数据内容
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	@SafeVarargs
	public final <T> AffectedArea writeColumn(final String address, final T... line) {
		final T[][] lines = DataMatrix.of(line).transpose().data();
		return this.writeLines(address, lines);
	}

	/**
	 * 保持origin(ltCell)不变，重新设置区域的行数(height)与列数(width)
	 * 
	 * @param nrows 缩放后的行数量
	 * @param ncols 缩放后的列数量
	 * @return AffectedArea
	 */
	public AffectedArea reshape(final Integer nrows, final Integer ncols) {
		final var new_nrows = Optional.ofNullable(nrows == null || nrows < 1 ? null : nrows).orElse(this.nrows());
		final var new_ncols = Optional.ofNullable(ncols == null || ncols < 1 ? null : ncols).orElse(this.ncols());
		return this.create(this.origin(), new_nrows, new_ncols);
	}

	/**
	 * 数据列
	 * 
	 * @return AffectedArea
	 */
	public Stream<AffectedArea> colS() {
		return Stream.iterate(0, i -> i + 1).limit(this.width())
				.map(i -> new AffectedArea(this.excel, this.cell(0, i), this.height(), 1)); // 列宽度为1
	}

	/**
	 * 数据列
	 * 
	 * @return AffectedArea
	 */
	public List<AffectedArea> cols() {
		return this.colS().toList();
	}

	/**
	 * 第n列
	 * 
	 * @param n 列索引从0开始
	 * @return AffectedArea
	 */
	public Optional<AffectedArea> colOpt(final int n) {
		return Optional.ofNullable(0 <= n && n < this.ncols() ? n : null)
				.map(i -> new AffectedArea(this.excel, this.cell(0, i), this.height(), 1));
	}

	/**
	 * 第n列
	 * 
	 * @param n 列索引从0开始
	 * @return AffectedArea
	 */
	public AffectedArea col(final int n) {
		return this.colOpt(n).orElse(null);
	}

	/**
	 * 区域最后一行（顶部的别名）
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea firstRow() {
		return this.row(0);
	}

	/**
	 * 第一列
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea firstCol() {
		return this.col(0);
	}

	/**
	 * 最后一列
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea lastCol() {
		return this.col(this.ncols() - 1);
	}

	/**
	 * 区域最后n项目
	 * 
	 * @param n 区域长度(列数量）
	 * @return AffectedArea
	 */
	public AffectedArea lastCols(final int n) {
		final var i = Math.max(0, this.nrows() - 1 - n);
		return this.create(this.cell(i, 0), n, this.ncols());
	}

	/**
	 * 区域最后n项目
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea last() {
		return lastRows(1);
	}

	/**
	 * 区域最后一行（底部的别名）
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea lastRow() {
		return this.row(this.nrows() - 1);
	}

	/**
	 * 区域最后n项目
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea lastRows(final int n) {
		final var i = Math.max(0, this.nrows() - 1 - n);
		return this.create(this.cell(i, 0), n, this.ncols());
	}

	/**
	 * 区域顶部
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea top() {
		return this.row(0);
	}

	/**
	 * 区域前n项目长度
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea top(final int n) {
		return this.head(n);
	}

	/**
	 * 区域最后n项目
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea bottom(final int n) {
		return this.lastRows(n);
	}

	/**
	 * 区域底部
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea bottom() {
		return this.row(this.nrows() - 1);
	}

	/**
	 * 区域左侧
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea left() {
		return this.col(0);
	}

	/**
	 * 区域最后n项目
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea left(final int n) {
		return this.ncols(n);
	}

	/**
	 * 区域右侧
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea right() {
		return this.col(this.ncols() - 1);
	}

	/**
	 * 区域最后n项目
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea right(final int n) {
		final var i = Math.max(0, this.ncols() - 1 - n);
		return this.create(this.cell(0, i), this.nrows(), n);
	}

	/**
	 * 区域前n项目长度
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea head() {
		return this.head(1);
	}

	/**
	 * 区域前n项目长度
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea head(final int n) {
		return this.headRows(n);
	}

	/**
	 * 头前n行
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea headRows(final int n) {
		return this.nrows(n);
	}

	/**
	 * 头前的n列
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea headCols(final int n) {
		return this.ncols(n);
	}

	/**
	 * 去除第一行（head)的剩余部分
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea tail() {
		return this.tailRows(1);
	}

	/**
	 * 剔除头前n行（头部）的剩余部分
	 * 
	 * @param 剔除
	 * @return AffectedArea
	 */
	public AffectedArea tailRows(final int n) {
		return this.skipRows(n);
	}

	/**
	 * 剔除头前的n列的剩余
	 * 
	 * @param n 区域长度
	 * @return AffectedArea
	 */
	public AffectedArea tailCols(final int n) {
		return this.skipCols(n);
	}

	/**
	 * 第n行
	 * 
	 * @param n 行索引从0开始
	 * @return AffectedArea
	 */
	public AffectedArea row(final int n) {
		return this.rowOpt(n).orElse(null);
	}

	/**
	 * 第n行
	 * 
	 * @param n 行索引从0开始
	 * @return 数据行AffectedArea
	 */
	public Optional<AffectedArea> rowOpt(final int n) {
		return Optional.ofNullable(0 <= n && n < this.nrows() ? n : null)
				.map(i -> this.create(this.cell(i, 0), 1, this.ncols()));
	}

	/**
	 * 数据行
	 * 
	 * @return 数据行列表
	 */
	public List<AffectedArea> rows() {
		return this.rowS().toList();
	}

	/**
	 * 数据行
	 * 
	 * @return 数据行流
	 */
	public Stream<AffectedArea> rowS() {
		return Stream.iterate(0, i -> i + 1).limit(this.height()).map(this::row); // 行高度为1
	}

	/**
	 * 数据行(rowS的别名)
	 * 
	 * @return 数据行流
	 */
	public Stream<AffectedArea> lineS() {
		return this.rowS();
	}

	/**
	 * 过滤掉指定头前的行数与列数<br>
	 * 注意 skip(0,0) equals this 对象
	 * 
	 * @param m 过滤的头前行数，大于等于0的整数
	 * @param n 过滤的头前列数，大于等于0的整数
	 * @return AffectedArea
	 */
	public AffectedArea rest(final int m, final int n) {
		return this.skip(m, n);
	}

	/**
	 * 过滤掉指定头前的行数与列数<br>
	 * 注意 skip(0,0) equals this 对象
	 * 
	 * @param m 过滤的头前行数，大于等于0的整数
	 * @param n 过滤的头前列数，大于等于0的整数
	 * @return AffectedArea
	 */
	public AffectedArea skip(final int m, final int n) {
		return this.create(this.cell(m, n), this.nrows() - m, this.ncols() - n);
	}

	/**
	 * 过滤掉指定头前的行数<br>
	 * 注意 skipRows(0) equals this 对象
	 * 
	 * @param m 过滤的头前行数，大于等于0的整数
	 * @return AffectedArea
	 */
	public AffectedArea skipRows(final int m) {
		return this.skip(m, 0);
	}

	/**
	 * 过滤掉指定头前的列数<br>
	 * 注意 skipCols(0) equals this 对象
	 * 
	 * @param n 过滤的头前列数，大于等于0的整数
	 * @return AffectedArea
	 */
	public AffectedArea skipCols(final int n) {
		return this.skip(0, n);
	}

	/**
	 * 空间切割(数据矩阵)
	 * 
	 * 切分点：points 是一个整数序列, p0,p1,p2, ... <br>
	 * 比如 [0,1,2,3,4,5,6],在[2,4]的points下会被分成[0,1],[2,3],[4,5,6]
	 * 
	 * @param ps 行切分点
	 * @param qs 列切分点
	 * @return
	 */
	public DataMatrix<AffectedArea> splitX(final Iterable<Integer> ps, final Iterable<Integer> qs) {
		return this.splitX(Types.itr2array(ps), Types.itr2array(qs));
	}

	/**
	 * 空间切割(数据矩阵)
	 * 
	 * 切分点：points 是一个整数序列, p0,p1,p2, ... <br>
	 * 比如 [0,1,2,3,4,5,6],在[2,4]的points下会被分成[0,1],[2,3],[4,5,6]
	 * 
	 * @param ps 行切分点
	 * @param qs 列切分点
	 * @return
	 */
	public DataMatrix<AffectedArea> splitX(final Integer[] ps, final Integer[] qs) {
		final var aas = this.splitS(true, ps).map(row -> row.splits(false, qs).toArray(AffectedArea[]::new))
				.toArray(AffectedArea[][]::new);
		return DataMatrix.of(aas);
	}

	/**
	 * 空间切割
	 * 
	 * @param flag   切分方向，true：按照行方向进行切分, false:按照列方向进行切分
	 * @param points 切分点, p0,p1,p2, ... <br>
	 *               比如 [0,1,2,3,4,5,6],在[2,4]的points下会被分成[0,1],[2,3],[4,5,6]
	 * @return
	 */
	public Stream<AffectedArea> splitS(final Boolean flag, final Iterable<Integer> points) {
		return this.splits(flag, Types.itr2array(points)).stream();
	}

	/**
	 * 空间切割
	 * 
	 * @param flag   切分方向，true：按照行方向进行切分, false:按照列方向进行切分
	 * @param points 切分点, p0,p1,p2, ... <br>
	 *               比如 [0,1,2,3,4,5,6],在[2,4]的points下会被分成[0,1],[2,3],[4,5,6]
	 * @return
	 */
	public Stream<AffectedArea> splitS(final Boolean flag, final Integer... points) {
		return this.splits(flag, points).stream();
	}

	/**
	 * 空间切割
	 * 
	 * @param flag   切分方向，true：按照行方向进行切分, false:按照列方向进行切分
	 * @param points 切分点, p0,p1,p2, ... <br>
	 *               比如 [0,1,2,3,4,5,6],在[2,4]的points下会被分成[0,1],[2,3],[4,5,6]
	 * @return
	 */
	public List<AffectedArea> splits(final Boolean flag, final Integer... points) {
		if (points.length < 1) {
			return Arrays.asList(this);
		} else {
			final var n = flag ? this.nrows() : this.ncols();
			final var ps = Stream.of(points).filter(e -> e <= n).distinct().sorted().collect(Collectors.toList());

			if (ps.get(0) != 0) { // 加入首项0
				ps.addFirst(0);
			}

			if (ps.getLast() != n) { // 加入尾项n
				ps.addLast(n);
			}

			final List<AffectedArea> list = new LinkedList<AffectedArea>();
			for (int i = 0; i < ps.size() - 1; i++) { // 数据分割
				final var seg = this.segment(ps.get(i), ps.get(i + 1), flag);
				if (seg == null) {
					break;
				}
				list.add(seg);
			}

			return list;
		}
	}

	/**
	 * 判断索引位置是否谓语区域内部
	 * 
	 * @param i 行索引从0开始
	 * @param j 列索引从0开始
	 * @return
	 */
	public boolean contains(final int i, final int j) {
		return i < this.nrows() && j < this.ncols();
	}

	/**
	 * 提取分段（行方向）
	 * 
	 * @param start 开始行索引包含,inclusive,0 based
	 * @param end   结束行索引不包含,exclusive,0 based
	 * @return 片段
	 */
	public AffectedArea segment(final Integer start) {
		return this.segment(start, this.nrows(), true);
	}

	/**
	 * 提取分段（行方向）
	 * 
	 * @param start 开始行索引包含,inclusive,0 based
	 * @param end   结束行索引不包含,exclusive,0 based
	 * @return 片段
	 */
	public AffectedArea segment(final Integer start, final Integer end) {
		return this.segment(start, end, true);
	}

	/**
	 * 提取分段
	 * 
	 * @param start 开始行索引包含,inclusive,0 based
	 * @param end   结束行索引不包含,exclusive,0 based
	 * @param flag  切分方向，true 行方向,false 列方向
	 * @return 片段
	 */
	public AffectedArea segment(final Integer start, final Integer end, final Boolean flag) {
		if (null == start || null == end) {
			return null;
		}

		final var nrows = this.nrows(); // 行数
		final var ncols = this.ncols(); // 列数
		final var i = Math.min(start, end); // 开始索引
		final var j = Math.max(start, end); // 结束索引
		var size = j - i; // 片段尺寸

		if (i < 0 || j < 0 || size <= 0) { // 起止索引无效
			return null;
		}

		if (Optional.ofNullable(flag).orElse(false)) { // 按照行切分

			if (i >= nrows) { // 行索引无效
				return null;
			}

			if (i + size > nrows) {
				size = nrows - i;
			}

			return this.create(this.cell(i, 0), size, ncols);
		} else { // 按照列进行分割

			if (i >= ncols) { // 列索引无效
				return null;
			}

			if (i + size > ncols) {
				size = ncols - i;
			}

			return this.create(this.cell(0, i), nrows, size);
		} // if
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
	 * 数据矩阵(进行单元格计算后的数据）
	 * 
	 * @return 数据值
	 */
	public DataMatrix<Object> evalmx() {
		return this.dmx(this::evaluate);
	}

	/**
	 * 数据矩阵
	 * 
	 * @return 数据值
	 */
	public DataMatrix<Cell> dmx() {
		return this.stream(e -> e).collect(DataMatrix.dmxclc(this.ncols()));
	}

	/**
	 * 数据矩阵
	 * 
	 * @param <T>    元素类型
	 * @param mapper 值变换函数 cell-&gt;
	 * @return 数据值
	 */
	public <T> DataMatrix<T> dmx(final Function<? super Cell, T> mapper) {
		return this.stream(mapper).collect(DataMatrix.dmxclc(this.ncols()));
	}

	/**
	 * 数据矩阵
	 * 
	 * @param <T> 元素类型
	 * @return 数据值
	 */
	public <T> DFrame dfm() {
		return this.dfm(null, null);
	}

	/**
	 * 数据矩阵
	 * 
	 * @param <T>   元素类型
	 * @param rowrb 值变换函数 cell-&gt;
	 * @return 数据值
	 */
	@SuppressWarnings("unchecked")
	public <T> DFrame dfm(final Function<Cell[], IRecord> rowrb, final Function<Cell, Object> mapper) {
		final Function<Cell[], IRecord> rb = Optional.ofNullable(rowrb)
				.orElse((Function<Cell[], IRecord>) IRecord.rb(DataMatrix.xlsns(this.ncols()))::get);
		final var dfm = this.rowS().map(e -> e.toArray()).map(rb).collect(DFrame.dfmclc);
		return Optional.ofNullable(mapper).map(e -> dfm.fmap(r -> r.fmap(Function.class.cast(e)))).orElse(dfm);

	}

	/**
	 * 单元格流序列（行顺序）
	 * 
	 * @return AffectedArea
	 */
	public Stream<Cell> cellS() {
		return Stream.iterate(0, i -> i + 1).limit(this.nrows())
				.flatMap(i -> Stream.iterate(0, j -> j + 1).limit(this.ncols()).map(j -> this.cell(i, j)));

	}

	/**
	 * 单元格流序列（行顺序）
	 * 
	 * @return Cell 列表
	 */
	public List<Cell> cells() {
		return this.cellS().toList();
	}

	/**
	 * 行顺序的一维数组 <br>
	 * 
	 * @return Cell一维数组
	 */
	public Cell[] toArray() {
		return this.cellS().toArray(Cell[]::new);
	}

	/**
	 * 行顺序的二维数组<br>
	 * 
	 * @return Cell二维数组
	 */
	public Cell[][] toArray2() {
		return this.rowS().map(AffectedArea::toArray).toArray(Cell[][]::new);

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
	 * 获取 Pack CellStyle
	 * 
	 * @param cs 式样如处理器
	 * @return Pack CellStyle
	 */
	public Pack<CellStyle> pcs(final Consumer<CellStyle> cs) {
		return this.pcs().wrap(cs);
	}

	/**
	 * 属性清空
	 * 
	 * @return AffectedArea
	 */
	public synchronized AffectedArea clear() {
		this.pcs = null;
		return this;
	}

	/**
	 * 设置选区
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea focus() {
		this.excel.setAffectedArea(this);
		return this;
	}

	/**
	 * 计算单元格的值
	 * 
	 * @param cell 单元格
	 * @return 计算值
	 */
	public Optional<Object> evaluateOpt(final Cell cell) {
		return Optional.ofNullable(cell).map(excel::evaluate);
	}

	/**
	 * 计算单元格的值
	 * 
	 * @param cell 单元格
	 * @return 计算值
	 */
	public Object evaluate(final Cell cell) {
		return this.evaluateOpt(cell).orElse(null);
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

	@Override
	public Iterator<Cell> iterator() {
		return this.cellS().iterator();
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
		if (obj == this) {
			return true;
		}

		if (obj instanceof AffectedArea aa) {
			return Objects.equals(this.pcs, aa.pcs) && Objects.equals(this.pcs, aa.pcs)
					&& Objects.equals(this.shape, aa.shape) && Objects.equals(this.excel, aa.excel);
		}

		return false;
	}

	/**
	 * 采用与excel的range相同的命名方式，以保证的可以直接用aa与字符串连接去生成EXCEL公式 <br>
	 * 比如:"sum(%s)".format(aa) 就可以生成对整个aa中的所有cell进行求和的公式
	 */
	@Override
	public String toString() {
		return this.getName(true);
	}

	/**
	 * 返回excel对象
	 * 
	 * @return SimpleExcel
	 */
	public SimpleExcel excel() {
		return this.excel;
	}

	/**
	 * 数据保存（excel.save)的别名
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea save() {
		this.excel.save();
		return this;
	}

	/**
	 * 数据保存（excel.save)的别名
	 * 
	 * @param append 是否是追加模式, null 通过判断 文件是否存在作为依据,存在追加,否则不追加
	 * @return AffectedArea
	 */
	public AffectedArea save(final Boolean append) {
		this.excel.save(append);
		return this;
	}

	/**
	 * 数据保存（excel.save(true))的别名
	 * 
	 * @param append 是否是追加模式, null 通过判断 文件是否存在作为依据,存在追加,否则不追加
	 * @return AffectedArea
	 */
	public AffectedArea apdsave() {
		return this.save(true);
	}

	/**
	 * 数据保存（excel.close)的别名
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea close() {
		this.excel.close();
		return this;
	}

	/**
	 * this.excel.save().close() 的简写
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea saveAndClose() {
		this.excel.save().close();
		return this;
	}

	/**
	 * this.excel.save().close() 的简写
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea sclose() {
		return this.saveAndClose();
	}

	/**
	 * 格式喷涂
	 * 
	 * @param pstyle 单元格式样
	 * @return AffectedArea
	 */
	public AffectedArea paint() {
		return this.paint(this.pcs);
	}

	/**
	 * 格式喷涂
	 * 
	 * @param newstyle 单元格式样
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	public AffectedArea paint(final Pack<CellStyle> pcs) {
		Optional.ofNullable(pcs).ifPresent(p -> p.evaluate(this::paint));
		return this;
	}

	/**
	 * 设置区域喷涂
	 * 
	 * @param prepare 喷涂式样转呗
	 * @return AffectedArea
	 */
	public AffectedArea paint(final Consumer<CellStyle> prepare) {
		return this.paint(excel.packWKCellStyle(this.origin()).evaluate(prepare));
	}

	/**
	 * 设置字体颜色
	 * 
	 * @param prepare 喷涂式样转呗
	 * @return AffectedArea
	 */
	public AffectedArea pcolor(final IndexedColors color) {
		excel.packWKFont(null).wrap(font -> font.setColor(color.getIndex()))
				.flatMap(font -> excel.packWKCellStyle(ltCell) //
						.wrap(s -> s.setFont(font)))
				.evaluate(this::paint);
		return this;
	}

	/**
	 * 喷绘制标题 <br>
	 * 
	 * @param color 边框颜色
	 * @return AffectedArea
	 */
	public AffectedArea ptitle(final IndexedColors color) {
		return this.paintTitle(color);
	}

	/**
	 * 喷绘制标题 <br>
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea ptitle() {
		return this.paintTitle(IndexedColors.BLACK);
	}

	/**
	 * 喷绘制标题 <br>
	 * 
	 * @param color 边框颜色
	 * @return AffectedArea
	 */
	public AffectedArea paintTitle(final IndexedColors color) {
		final var top = this.top();
		final var cell = top.origin();
		excel.packWKCellStyle(cell).valueOpt().map(s -> {
			s.setBorderTop(BorderStyle.THIN);
			s.setTopBorderColor(color.getIndex());
			s.setFont(excel.packWKFont(null).evaluate(font -> {
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
	 * @param color 边框颜色
	 * @return AffectedArea
	 */
	public AffectedArea ptop(final IndexedColors color) {
		return this.paintTop(color, null);
	}

	/**
	 * 喷绘制顶部 <br>
	 * 
	 * @param color 边框颜色
	 * @param bs    边框式样
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	public AffectedArea pbottom(final IndexedColors color) {
		return this.paintBottom(color, null);
	}

	/**
	 * 喷绘制顶部 <br>
	 * 
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	public AffectedArea pleft(final IndexedColors color) {
		return this.paintLeft(color, null);
	}

	/**
	 * 喷绘制左侧 <br>
	 * 
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	public AffectedArea pright(final IndexedColors color) {
		return this.paintRight(color, null);
	}

	/**
	 * 喷绘制右侧 <br>
	 * 
	 * @return AffectedArea
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
	 * @return AffectedArea
	 */
	public AffectedArea poutline(final IndexedColors color) {
		return this.paintOutline(color, null);
	}

	/**
	 * 喷涂边框 <br>
	 * 
	 * @return AffectedArea
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
	 * @param color 颜色
	 * @return AffectedArea
	 */
	public AffectedArea background(final IndexedColors color) {
		this.pcs(SimpleExcel.background_color.apply(color)).evaluate(this::paint);
		return this;
	}

	/**
	 * 设置背景色
	 * 
	 * @param color 颜色
	 * @return AffectedArea
	 */
	public AffectedArea bottomThick(final IndexedColors color) {
		return this.border(BorderName.BOTTOM, BorderStyle.THICK,
				Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param color 颜色
	 * @return AffectedArea
	 */
	public AffectedArea bottomThin(final IndexedColors color) {
		return this.border(BorderName.BOTTOM, BorderStyle.THIN, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param color 颜色
	 * @return AffectedArea
	 */
	public AffectedArea bottomDouble(final IndexedColors color) {
		return this.border(BorderName.BOTTOM, BorderStyle.DOUBLE,
				Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param color 颜色
	 * @return AffectedArea
	 */
	public AffectedArea topThick(final IndexedColors color) {
		return this.border(BorderName.TOP, BorderStyle.THICK, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param color 颜色
	 * @return AffectedArea
	 */
	public AffectedArea topThin(final IndexedColors color) {
		return this.border(BorderName.TOP, BorderStyle.THIN, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @param color 杨色
	 * @return AffectedArea
	 */
	public AffectedArea topDouble(final IndexedColors color) {
		return this.border(BorderName.TOP, BorderStyle.DOUBLE, Optional.ofNullable(color).orElse(IndexedColors.BLACK));
	}

	/**
	 * 设置背景色
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea border() {
		return this.border(null, null, null);
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName 边框名
	 * @param color      颜色
	 * @return AffectedArea
	 */
	public AffectedArea border(final BorderName borderName, final IndexedColors color) {
		return this.border(borderName, null, color);
	}

	/**
	 * 设置背景色
	 * 
	 * @param borderName  边框名
	 * @param borderStyle 边框式样
	 * @param color       杨色
	 * @return AffectedArea
	 */
	public AffectedArea border(final BorderName borderName, BorderStyle borderStyle, final IndexedColors color) {
		final var bn = Optional.ofNullable(borderName).orElse(BorderName.ALL);
		final var bs = Optional.ofNullable(borderStyle).orElse(BorderStyle.THIN);
		final var clr = Optional.ofNullable(color).orElse(IndexedColors.BLACK);
		this.pcs(SimpleExcel.border_color.apply(bs).apply(bn, clr)).evaluate(this::paint);

		return this;
	}

	/**
	 * 设置或是取消字体加黑
	 * 
	 * @param bold 是否字体加黑
	 * @return AffectedArea
	 */
	public AffectedArea bold(final boolean bold) {
		excel.packWKFont(this.origin()).wrap(font -> font.setBold(bold)).flatMap(font -> this.pcs(e -> e.setFont(font)))
				.evaluate(this::paint);
		return this;
	}

	/**
	 * 字体加黑
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea bold() {
		return bold(true);
	}

	/**
	 * 设置或是取消字体倾斜
	 * 
	 * @param italic 是否字体倾斜
	 * @return AffectedArea
	 */
	public AffectedArea italic(final boolean italic) {
		excel.packWKFont(null).wrap(font -> font.setItalic(italic)).flatMap(font -> this.pcs(e -> e.setFont(font)))
				.wrap(this::paint);
		return this;
	}

	/**
	 * 设置或是取消字体倾斜
	 * 
	 * @return AffectedArea
	 */
	public AffectedArea italic() {
		return this.italic(true);
	}

	/**
	 * 获得一个单元格的绝地地址带有Sheet名称的那种(eg,Sheet1!A1)
	 * 
	 * @param cell 单元格对象
	 * @return 单元格的绝地地址
	 */
	public static String fullAddress(final Cell cell) {
		return Optional.ofNullable(cell)
				.map(c -> "%s!%s".formatted(c.getSheet().getSheetName(), c.getAddress().formatAsString())).orElse(null);
	}

	/**
	 * 当前缓存的单元格式样
	 */
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