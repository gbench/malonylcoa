package gbench.util.data.xls;

import java.util.Optional;

import gbench.util.jdbc.kvp.Tuple2;

/**
 * 表示一个平面的额区的矩形区域(x:行号,y:列号) <br>
 * 
 * x0,y0 ---------------- x0,y1 <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * x1,y0 ---------------- x1,y1 <br>
 * 
 * @author gbench
 *
 */
public class RangeDef {

	/**
	 * 构造函数
	 */
	public RangeDef() {
		super();
	}

	/**
	 * 构造函数
	 * 
	 * @param x0 x0
	 * @param y0 y0
	 * @param x1 x1
	 * @param y1 y1
	 */
	public RangeDef(final Integer x0, final Integer y0, final Integer x1, final Integer y1) {
		this(null, x0, y0, x1, y1);
	}

	/**
	 * 构造函数
	 * 
	 * @param sheetName sheetName
	 * @param x0        x0
	 * @param y0        y0
	 * @param x1        x1
	 * @param y1        y1
	 */
	public RangeDef(final String sheetName, final Integer x0, final Integer y0, final Integer x1, final Integer y1) {
		super();
		this.sheetName = sheetName;
		this._x0 = x0;
		this._y0 = y0;
		this._x1 = x1;
		this._y1 = y1;
	}

	/**
	 * RangeDef
	 * 
	 * @param x0 x0
	 * @param y0 y0
	 * @param x1 x1
	 * @param y1 y1
	 */
	public RangeDef(final int x0, final int y0, final int x1, final int y1) {
		super();
		this._x0 = x0;
		this._y0 = y0;
		this._x1 = x1;
		this._y1 = y1;
	}

	public String sheetName() {
		return this.sheetName;
	}

	/**
	 * x0 左上行号
	 * 
	 * @return x0
	 */
	public int x0() {
		return _x0;
	}

	/**
	 * x0 左上行号
	 * 
	 * @param x0 x0
	 */
	public void x0(final int x0) {
		this._x0 = x0;
	}

	/**
	 * y0 左上列号
	 * 
	 * @return y0
	 */
	public int y0() {
		return _y0;
	}

	/**
	 * y0 左上列号
	 * 
	 * @param y0 y0
	 */
	public void y0(final int y0) {
		this._y0 = y0;
	}

	/**
	 * x1 右下行号
	 * 
	 * @return x1
	 */
	public int x1() {
		return _x1;
	}

	/**
	 * x1 右下行号
	 * 
	 * @param x1 x1
	 */
	public void x1(final int x1) {
		this._x1 = x1;
	}

	/**
	 * y1 右下列号
	 * 
	 * @return y1
	 */
	public int y1() {
		return _y1;
	}

	/**
	 * y1 右下列号
	 * 
	 * @param y1 y1
	 */
	public void y1(final int y1) {
		this._y1 = y1;
	}

	/**
	 * Left Top
	 * 
	 * @return (_1:左上行号,_2:左上列号)
	 */
	public Tuple2<Integer, Integer> lt() {
		return Tuple2.of(this.x0(), this.y0());
	}

	/**
	 * Right Bottom
	 * 
	 * @return (_1:右下行号,_2:右下列号)
	 */
	public Tuple2<Integer, Integer> rb() {
		return Tuple2.of(this.x1(), this.y1());
	}

	/**
	 * 高度
	 * 
	 * @return
	 */
	public int height() {
		return Math.abs(this._y1 - this._y0) + 1;
	}

	/**
	 * 宽度
	 * 
	 * @return
	 */
	public int width() {
		return Math.abs(this._x1 - this._x0) + 1;
	}

	/**
	 * sheet名称是否是空白
	 * 
	 * @return
	 */
	public boolean isBlankName() {
		return Optional.ofNullable(this.sheetName).map(e -> e.matches("\s*")).orElse(true);
	}

	@Override
	public String toString() {
		return "Range [x0=" + _x0 + ", y0=" + _y0 + ", x1=" + _x1 + ", y1=" + _y1 + "]";
	}

	private String sheetName; // 表单名称
	/**
	 * 左上角第一个单元格水平坐标:左上行号
	 */
	private Integer _x0;
	/**
	 * 左上角第一个单元格垂直坐标:左上列号
	 */
	private Integer _y0;
	/**
	 * 右下角第一个单元格垂直坐标:右下行号
	 */
	private Integer _x1;

	/**
	 * 右下角第一个单元格垂直坐标:右下列号
	 */
	private Integer _y1;
}