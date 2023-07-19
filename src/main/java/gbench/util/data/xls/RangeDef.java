package gbench.util.data.xls;

/**
 * 表示一个平面的额区的矩形区域 <br>
 * 
 * x0,y0 ---------------- x0,y1 <br>
 * |                          | <br>
 * |                          | <br>
 * |                          | <br>
 * |                          | <br>
 * |                          | <br>
 * |                          | <br>
 * x1,y0 ---------------- x1,y1 <br>
 * 
 * @author xuqinghua
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
    public RangeDef(Integer x0, Integer y0, Integer x1, Integer y1) {
        super();
        this._x0 = x0;
        this._y0 = y0;
        this._x1 = x1;
        this._y1 = y1;
    }

    public RangeDef(int x0, int y0, int x1, int y1) {
        super();
        this._x0 = x0;
        this._y0 = y0;
        this._x1 = x1;
        this._y1 = y1;
    }

    public int x0() {
        return _x0;
    }

    public void x0(int x0) {
        this._x0 = x0;
    }

    public int y0() {
        return _y0;
    }

    public void y0(int y0) {
        this._y0 = y0;
    }

    public int x1() {
        return _x1;
    }

    public void x1(int x1) {
        this._x1 = x1;
    }

    public int y1() {
        return _y1;
    }

    public void y1(int y1) {
        this._y1 = y1;
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