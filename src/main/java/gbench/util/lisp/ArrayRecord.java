package gbench.util.lisp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.json.MyJson;

import java.util.LinkedHashMap;

/**
 * 数据结构的IRecord 通常用于为一个值values指定某种键名序列
 * 
 * @author gbench
 *
 */
public class ArrayRecord implements IRecord, Serializable {

	/**
	 * 
	 * @param keys
	 * @param values
	 */
	public ArrayRecord(final String[] keys, final Object[] values) {
		super();
		this.keys = keys;
		this.values = values;
	}

	@Override
	public Iterator<Tuple2<String, Object>> iterator() {
		return this.tupleS().iterator();
	}

	@Override
	public <T> T get(final String key) {
		final Integer idx = this.indexOf(key);
		return idx == null ? null : this.get(idx);
	}

	@Override
	public IRecord set(final String key, Object value) {
		final Integer idx = this.indexOf(key);
		if (idx != null) {
			this.set(idx, value);
		}
		return this;
	}

	@Override
	public IRecord remove(final String key) {
		return this.remove(this.indexOf(key));
	}

	@Override
	public String json() {
		return MyJson.toJson(this.toMap());
	}

	@Override
	public IRecord duplicate() {
		return new ArrayRecord(keys == null ? null : Arrays.copyOf(keys, keys.length),
				values == null ? null : Arrays.copyOf(values, values.length));
	}

	@Override
	public IRecord build(final Object... kvs) {
		return REC(kvs);
	}

	@Override
	public List<String> keys() {
		return keys == null ? null : Arrays.asList(keys);
	}

	@Override
	public Map<String, Object> toMap() {
		final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		for (int i = 0; i < keys.length; i++) {
			final Object value = values == null || i >= values.length ? null : values[i];
			data.put(keys[i], value);
		}
		return data;
	}

	// -----------------------------------------------------------------
	// 非必须接口实现
	// -----------------------------------------------------------------

	/**
	 * 
	 */
	@Override
	public String toString() {
		return this.toMap().toString();
	}

	@Override
	public IRecord remove(final Integer idx) {
		final Integer n = this.size();
		if (idx != null && idx < this.size()) {
			final Object[] oo = new Object[2 * (n - 1)];
			for (int i = 0; i < n; i++) {
				if (i == idx)
					continue;
				else {
					final int j = i < idx ? i : i - 1;
					oo[2 * j] = this.keys[i];
					oo[2 * j + 1] = this.values[i];
				}
			}
			return this.build(oo);
		} else {
			return this.duplicate();
		} // if
	}

	/**
	 * 
	 */
	@Override
	public Stream<String> keyS() {
		return keys == null ? null : Arrays.stream(keys);
	}

	@Override
	public List<Object> values() {
		return values == null ? null : Arrays.asList(values);
	}

	/**
	 * 
	 */
	@Override
	public Stream<Object> valueS() {
		return values == null ? null : Arrays.stream(values);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(final int idx) {
		if (this.values == null)
			return null;
		else if (idx < this.values.length) {
			return (T) this.values[idx];
		} else {
			return null;
		}
	}

	@Override
	public IRecord set(final Integer idx, final Object value) {
		this.values[idx] = value;
		return this;
	}

	@Override
	public String keyOf(final int idx) {
		return this.keys[idx];
	}

	@Override
	public Integer indexOf(final String key) {
		return Arrays.asList(this.keys).indexOf(key);
	}

	@Override
	public int size() {
		return this.keys.length;
	}

	@Override
	public Stream<Tuple2<String, Object>> tupleS() {
		final int n = this.size();
		return Stream.iterate(0, i -> i < n, i -> i + 1).map(i -> Tuple2.of(keys[i], values[i]));
	}

	/**
	 * 
	 * @return
	 */
	public String[] getKeys() {
		return keys;
	}

	/**
	 * 
	 * @param keys
	 * @return ra 本身
	 */
	public ArrayRecord setKeys(String[] keys) {
		this.keys = keys;
		return this;
	}

	/**
	 * 
	 * @param keys
	 * @return ra 本身
	 */
	public ArrayRecord setKeys(final Iterable<String> keys) {
		this.keys = StreamSupport.stream(keys.spliterator(), false).toArray(String[]::new);
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public Object[] getValues() {
		return values;
	}

	/**
	 * 
	 * @param values
	 * @return ra 本身
	 */
	public ArrayRecord setValues(Object[] values) {
		this.values = values;
		return this;
	}

	/**
	 * 浅拷贝
	 */
	@Override
	public ArrayRecord clone() {
		return new ArrayRecord(this.keys, this.values);
	}

	/**
	 * setValues 别名： 为 一个数组values 指定键名索引
	 * 
	 * @param values
	 * @return ra 本身
	 */
	public ArrayRecord attach(final Object[] values) {
		return this.setValues(values);
	}

	/**
	 * setValues 别名: 为 一个数组values 指定键名索引
	 * 
	 * @param values
	 * @return ra 本身
	 */
	public ArrayRecord wrap(final Object[] values) {
		return this.setValues(values);
	}

	/**
	 * 构建一个键名键值序列 指定的 IRecord
	 *
	 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return 新生成的IRecord
	 */
	@SafeVarargs
	public static <T> IRecord REC(final T... kvs) {
		final int len = kvs.length;
		final Object[] oo = len % 2 == 0 ? kvs : Arrays.copyOf(kvs, len + 1);
		final int n = oo.length;
		final String[] keys = new String[n / 2];
		final Object[] values = new Object[n / 2];
		for (int i = 0; i < n / 2; i++) {
			keys[i] = oo[2 * i] + "";
			values[i] = oo[2 * i + 1];
		}
		return new ArrayRecord(keys, values);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys   键名序列
	 * @param values 键值序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord of(final String[] keys, final Object[] values) {
		return new ArrayRecord(keys, values);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys   键名序列
	 * @param values 键值序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord of(final String... keys) {
		return new ArrayRecord(keys, null);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys   键名序列
	 * @param values 键值序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord of(final Iterable<String> keys, final Iterable<Object> values) {
		final String[] _keys = keys == null ? null
				: StreamSupport.stream(keys.spliterator(), false).toArray(String[]::new);
		final Object[] _values = values == null ? null
				: StreamSupport.stream(values.spliterator(), false).toArray(Object[]::new);
		return new ArrayRecord(_keys, _values);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys 键名序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord of(final Iterable<String> keys) {
		return ArrayRecord.of(keys, null);
	}

	/**
	 * 序列号
	 */
	private static final long serialVersionUID = -4990916390989613873L;
	private String[] keys; // 键名
	private Object[] values; // 键值
}
