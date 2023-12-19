package gbench.util.jdbc.kvp;

import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个列数据 其实就是 键值对儿,差别就是这个键值是一个列表数据。
 * 
 * @author gbench
 *
 * @param <V> 列中值的类型
 */
public class Column<V> extends KVPair<String, List<V>> implements IColumn<V> {
	private static final long serialVersionUID = 5783906325810918364L;
	final static int INIT_LIST_SIZE = 100;

	public Column(String name) {
		super(name, new ArrayList<>(INIT_LIST_SIZE));
		this.clazz = null;
	}

	public Column(String name, Class<V> clazz) {
		super(name, new ArrayList<>(INIT_LIST_SIZE));
		this.clazz = clazz;
	}

	public V get(int i) {
		return this.value().get(i);
	}

	public void add(V v) {
		this.value().add(v);
	}

	public Class<V> getType() {
		return this.clazz;
	}

	@SuppressWarnings("unchecked")
	public void setType(Class<?> cls) {
		clazz = (Class<V>) cls;
	}

	/**
	 * 添加一個元素
	 * 
	 * @param v 值对象
	 */
	@SuppressWarnings("unchecked")
	public void addObject(final Object v) {
		this.add((V) v);
	}

	/**
	 * 格式化數據
	 */
	public String toString() {
		String type = this.getType() == null ? "unknown" : this.getType().getSimpleName();// 類型名
		return this.key() + "(" + type + ")" + " -> " + this.value();
	}

	@SuppressWarnings("unchecked")
	public V[] toArray() {
		List<V> vv = this.value();
		if (vv == null)
			return null;
		return (V[]) vv.toArray();
	}

	private Class<V> clazz = null; // 初始類型
}