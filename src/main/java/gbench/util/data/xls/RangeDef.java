package gbench.util.data.xls;

import java.util.Objects;
import java.util.Optional;

import gbench.util.jdbc.kvp.Tuple2;

/**
 * Sheet1!C2:D3表示为<br>
 * Sheet1表单里 <br>
 * 1) 左上: 第2行第3列(C)，x0->1,y0->2; 左上: 第2行第3三列(C)，x0->1,y0->2; <br>
 * 2) 右下: 第4行第5列(C)，x0->1,y0->2; 左上: 第2行第3三列(C)，x0->3,y0->4; <br>
 * (x0:1,y0:2,x1:3,y1:4) <br>
 * <br>
 * 表示一个平面的额区的矩形区域(x:行号,y:列号) <br>
 * 
 * LT 即 Left Top <br>
 * x0,y0 →------→y→------→ x0,y1 <br>
 * ↓ &nbsp;&nbsp;&nbsp;&nbsp;↓ <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * ↓ &nbsp;&nbsp;&nbsp;&nbsp;↓ <br>
 * x &nbsp;&nbsp;&nbsp;&nbsp;x <br>
 * ↓ &nbsp;&nbsp;&nbsp;&nbsp;↓ <br>
 * | &nbsp;&nbsp;&nbsp;&nbsp;| <br>
 * ↓ &nbsp;&nbsp;&nbsp;&nbsp;↓ <br>
 * x1,y0 →------→y→------→ x1,y1 RB 即 Right Bottom；<br>
 * L<R；T<B；LT<RB；x0<x1；y0<y1 <br>
 * <br>
 * 依据excel的布局特点:行号从0开始,从上向下依次递增，Top上<Bottom下 <br>
 * 依据excel的布局特点:列号从0开始,从左向右依次递增,Left左<Right右 <br>
 * 
 * 需要注意：与数学的坐标系（x表示水平轴,y表表示垂直轴）不同，<br>
 * RangeDef 中 x是垂直轴（列方向：height）,y 是水平轴(width行方向）<br>
 * 这是因为 RangeDef 是去描述 EXCEL 某个 sheet里面的一块矩形的数据区域 <br>
 * 需要把LT（左上）RB（右下）进行坐标描述，按照先行x后列y的坐标结构(行号,列号)进行<br>
 * 一个矩形：((x0:左上行号,y0:左上列号),(x0:右下行号,y0:右下列号)) 。 <br>
 * EXCEL的选区：A1:E5 对应着 ((x0->1,y0->A),(x1->5,y1->E)) <br>
 * 通常x0<x1，y0<y1是因为 EXCEL的单元格的编号就是从上向下，从左向右的的属性进行的。 <br>
 * <br>
 * x0 x0 左上行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增） <br>
 * y0 y0 左上列偏移（EXCEL表格水平方向，y从左向右依次水平递增） <br>
 * x1 x1 右下行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增） <br>
 * y1 y1 右下行偏移（EXCEL表格垂直方向，y从左向右依次水平递增） <br>
 * 
 * @author gbench
 *
 */
public class RangeDef {

	/**
	 * 构造函数
	 * 
	 * @param sheetName sheetName sheet名称(可以为null)
	 * @param x0        x0 左上行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增）
	 * @param y0        y0 左上列偏移（EXCEL表格水平方向，y从左向右依次水平递增）
	 * @param x1        x1 右下行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增）
	 * @param y1        y1 右下行偏移（EXCEL表格垂直方向，y从左向右依次水平递增）
	 */
	public RangeDef(final String sheetName, final Integer x0, final Integer y0, final Integer x1, final Integer y1) {
		this.sheetName = sheetName;
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	/**
	 * 构造函数
	 * 
	 * @param x0 x0 左上行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增）
	 * @param y0 y0 左上列偏移（EXCEL表格水平方向，y从左向右依次水平递增）
	 * @param x1 x1 右下行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增）
	 * @param y1 y1 右下行偏移（EXCEL表格垂直方向，y从左向右依次水平递增）
	 */
	public RangeDef(final Integer x0, final Integer y0, final Integer x1, final Integer y1) {
		this(null, x0, y0, x1, y1);
	}

	/**
	 * RangeDef
	 * 
	 * @param x0 x0 左上行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增）
	 * @param y0 y0 左上列偏移（EXCEL表格水平方向，y从左向右依次水平递增）
	 * @param x1 x1 右下行偏移（EXCEL表格垂直方向，x从上向下依次垂直递增）
	 * @param y1 y1 右下行偏移（EXCEL表格垂直方向，y从左向右依次水平递增）
	 */
	public RangeDef(final int x0, final int y0, final int x1, final int y1) {
		this(null, x0, y0, x1, y1);
	}

	/**
	 * 构造函数
	 */
	public RangeDef() {
		this(null, null, null, null, null);
	}

	/**
	 * 区域所在的sheet名称
	 * 
	 * @return
	 */
	public String sheetName() {
		return this.sheetName;
	}

	/**
	 * x0 左上行号
	 * 
	 * @return x0
	 */
	public int x0() {
		return x0;
	}

	/**
	 * 读取：x0 左上行号，依据excel的布局特点:行号从0开始,从上向下依次递增
	 * 
	 * @param x0 x0
	 */
	public void x0(final int x0) {
		this.x0 = x0;
	}

	/**
	 * y0 左上列号
	 * 
	 * @return y0
	 */
	public int y0() {
		return y0;
	}

	/**
	 * 设置：y0 左上列号
	 * 
	 * @param y0 y0
	 */
	public void y0(final int y0) {
		this.y0 = y0;
	}

	/**
	 * 读取：x1 右下行号，依据excel的布局特点:行号从0开始,从上向下依次递增
	 * 
	 * @return x1
	 */
	public int x1() {
		return x1;
	}

	/**
	 * 设置x1 右下行号，依据excel的布局特点:行号从0开始,从上向下依次递增
	 * 
	 * @param x1 x1
	 */
	public void x1(final int x1) {
		this.x1 = x1;
	}

	/**
	 * y1 右下列号，依据excel的布局特点:列号从0开始,从左向右依次递增
	 * 
	 * @return y1
	 */
	public int y1() {
		return y1;
	}

	/**
	 * y1 右下列号，依据excel的布局特点:列号从0开始,从左向右依次递增
	 * 
	 * @param y1 y1
	 */
	public void y1(final int y1) {
		this.y1 = y1;
	}

	/**
	 * Left Top <br>
	 * 依据excel的布局特点:行号从0开始,从上向下依次递增，Top上<Bottom下 <br>
	 * 依据excel的布局特点:列号从0开始,从左向右依次递增,Left左<Right右 <br>
	 * 
	 * @return (_1:左上行号,_2:左上列号)
	 */
	public Tuple2<Integer, Integer> lt() {
		return Tuple2.of(this.x0(), this.y0());
	}

	/**
	 * Right Bottom <br>
	 * 依据excel的布局特点:行号从0开始,从上向下依次递增，Top上<Bottom下 <br>
	 * 依据excel的布局特点:列号从0开始,从左向右依次递增,Left左<Right右 <br>
	 * 
	 * @return (_1:右下行号,_2:右下列号)
	 */
	public Tuple2<Integer, Integer> rb() {
		return Tuple2.of(this.x1(), this.y1());
	}

	/**
	 * 行数量
	 * 
	 * @return
	 */
	public int nrows() {
		return Math.abs(this.x1 - this.x0) + 1;
	}

	/**
	 * 高度（nrows的别名）
	 * 
	 * @return
	 */
	public int height() {
		return this.nrows();
	}

	/**
	 * 列数量
	 * 
	 * @return
	 */
	public int ncols() {
		return Math.abs(this.y1 - this.y0) + 1;
	}

	/**
	 * 宽度（ncols的别名）
	 * 
	 * @return
	 */
	public int width() {
		return this.ncols();
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
		return "Range [sheetName=%s,x0=%s, y0=%s, x1=%s, y1=%s, nrows:%s, ncols:%s]" //
				.formatted(sheetName, x0, y0, x1, y1, this.nrows(), this.ncols());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.sheetName, x0, y0, x1, y1);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof RangeDef def) {
			return Objects.equals(this.sheetName, def.sheetName) //
					&& Objects.equals(this.x0, def.x0) //
					&& Objects.equals(this.y0, def.y0) //
					&& Objects.equals(this.x1, def.x1) //
					&& Objects.equals(this.y1, def.y1);
		} else {
			return false;
		}
	}

	/**
	 * 表单名称
	 */
	private String sheetName;

	/**
	 * 左上角第一个单元格水平坐标:左上行号
	 */
	private Integer x0;

	/**
	 * 左上角第一个单元格垂直坐标:左上列号
	 */
	private Integer y0;

	/**
	 * 右下角第一个单元格垂直坐标:右下行号
	 */
	private Integer x1;

	/**
	 * 右下角第一个单元格垂直坐标:右下列号
	 */
	private Integer y1;
}