package gbench.util.jdbc.kvp;

/**
 *
 * @author gbench
 *
 * @param <V> 元素类型
 */
public interface IColumn<V> {
	/**
	 * 獲得第個元素
	 * 
	 * @param i 元素編號，從0開始
	 * @return 元素内容
	 */
	V get(final int i);

	/**
	 * 添加一個元素
	 * 
	 * @param v 值对象
	 */
	void add(final V v);

	/**
	 * 添加一個元素
	 * 
	 * @param v 元素对象
	 */
	void addObject(final Object v);

	/**
	 * 獲得初始類型
	 * 
	 * @return 类元素类型
	 */
	Class<V> getType();
}