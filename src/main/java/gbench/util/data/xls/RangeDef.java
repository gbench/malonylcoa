package gbench.util.data.xls;

/**
 * 表示一个平面的额区的矩形区域 <br>
 * 
 * x0,y0 ---------------- x0,y1 <br>
 * | | <br>
 * | | <br>
 * | | <br>
 * | | <br>
 * | | <br>
 * | | <br>
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
		super();
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

	/**
	 * x0
	 * 
	 * @return x0
	 */
	public int x0() {
		return _x0;
	}

	/**
	 * x0
	 * 
	 * @param x0 x0
	 */
	public void x0(final int x0) {
		this._x0 = x0;
	}

	/**
	 * y0
	 * 
	 * @return y0
	 */
	public int y0() {
		return _y0;
	}

	/**
	 * y0
	 * 
	 * @param y0 y0
	 */
	public void y0(final int y0) {
		this._y0 = y0;
	}

	/**
	 * x1
	 * 
	 * @return x1
	 */
	public int x1() {
		return _x1;
	}

	/**
	 * x1
	 * 
	 * @param x1 x1
	 */
	public void x1(final int x1) {
		this._x1 = x1;
	}

	/**
	 * y1
	 * 
	 * @return y1
	 */
	public int y1() {
		return _y1;
	}

	/**
	 * y1
	 * 
	 * @param y1 y1
	 */
	public void y1(final int y1) {
		this._y1 = y1;
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

	@Override
	public String toString() {
		return "Range [x0=" + _x0 + ", y0=" + _y0 + ", x1=" + _x1 + ", y1=" + _y1 + "]";
	}

	private Integer _x0;// 左上角第一个单元格水平坐标
	private Integer _y0;// 右上角第一个单元格垂直坐标
	private Integer _x1;// 右下角第一个单元格垂直坐标
	private Integer _y1;// 右下角第一个单元格垂直坐标
}