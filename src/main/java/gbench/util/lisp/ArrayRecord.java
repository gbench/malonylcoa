package gbench.util.lisp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
	 * 构造函数
	 * 
	 * @param keys   键名序列
	 * @param values 键值序列
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
	public IRecord set(final String key, final Object value) {
		final Integer idx = this.indexOf(key);
		if (idx != null && idx >= 0) {
			this.set(idx, value);
			return this;
		} else { //
			final int n = this.size();
			if (n < 1) {
				return new ArrayRecord(new String[] { key }, new Object[] { value });
			} else {
				final String[] kk = Arrays.copyOf(keys, n + 1);
				final Object[] vv = Arrays.copyOf(values, n + 1);
				kk[n] = key;
				vv[n] = value;
				return new ArrayRecord(kk, vv);
			}
		}
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
		return new ArrayRecord( // 元素拷贝，深度拷贝
				keys == null //
						? null //
						: Arrays.copyOf(keys, keys.length),
				values == null //
						? null //
						: Arrays.copyOf(values, values.length) //
		);
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
	// 非必须接口实现: 特色方法的实现。
	// -----------------------------------------------------------------

	/**
	 * 格式化
	 */
	@Override
	public String toString() {
		return this.toMap().toString();
	}

	/**
	 * 删除指定索引的数据
	 * 
	 * @return 删除数据后的复制品
	 */
	@Override
	public IRecord remove(final Integer idx) {
		final Integer n = this.size();
		if (idx != null && idx < this.size()) { // 确保索引位置有效
			final Object[] data = new Object[2 * (n - 1)];
			for (int i = 0; i < n; i++) {
				if (i == idx) { // 越过索引位置
					continue;
				} else { // 拷贝其他索引位置
					final int j = i < idx ? i : i - 1; // 低于idx保持不变高于idx则向前移动一位
					data[2 * j] = this.keys[i];
					data[2 * j + 1] = this.values[i];
				} // for
			} // for
			return this.build(data);
		} else { // 索引位置无效返回复制品
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

	/**
	 * 当前元素类型
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <X, T> T[] toArray(final Function<X, T> mapper) {
		return mapper == null //
				? (T[]) this.values
				: this.valueS().map(e -> mapper.apply((X) e)) //
						.collect(Lisp.aaclc(this.size(), null, e -> e));
	}

	/**
	 * @return 直接返回值数组
	 */
	@Override
	public Object[] toArray() {
		return this.values;
	}

	/**
	 * @应用mapper作用于值数组
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T, U> U arrayOf(final Function<T[], U> mapper) {
		return mapper.apply((T[]) this.values);
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
		if (this.values == null) {
			return null;
		} else if (idx < this.values.length) { // 确保索引位置有效
			return (T) this.values[idx];
		} else { // 索引无效返回 null
			return null;
		}
	}

	@Override
	public IRecord set(final Integer idx, final Object value) {
		if (this.values != null && idx < this.values.length) { // 确保索引有效
			this.values[idx] = value;
		} else {
			// System.err.println(String.format("状态非法,values空标志:%s,参数索引:(%d)", this.values
			// == null, idx));
		}
		return this;
	}

	@Override
	public String keyOf(final int idx) {
		if (this.keys == null || idx >= this.keys.length) { // 索引位置无效
			return null;
		} else { // 索引有效
			return this.keys[idx];
		} // if
	}

	@Override
	public Integer indexOf(final String key) {
		return null == key ? null : Arrays.asList(this.keys).indexOf(key);
	}

	/**
	 * 依据键名长度为基准
	 */
	@Override
	public int size() {
		return this.keys == null ? 0 : this.keys.length;
	}

	@Override
	public Stream<Tuple2<String, Object>> tupleS() {
		final int n = this.size();
		return Stream.iterate(0, i -> i < n, i -> i + 1) //
				.map(i -> Tuple2.of( // 键值对儿
						keys == null || i >= keys.length ? null : keys[i], //
						values == null || i >= values.length ? null : values[i] //
				)); // map
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
	public ArrayRecord setKeys(final String[] keys) {
		if (keys != null && keys.length == this.size()) {
			this.keys = keys;
		}
		return this;
	}

	/**
	 * setKeys 别名: 为 一个数组values 指定键名索引
	 * 
	 * @param keys 键名序列
	 * @return ra 本身
	 */
	public ArrayRecord dressup(final String[] keys) {
		return this.setKeys(keys);
	}

	/**
	 * dressup and clone
	 * 
	 * @param keys 键名序列,逗号[,;/\\]进行分割
	 * @return ra 本身
	 */
	public ArrayRecord dressupClone(final String[] keys) {
		return this.dressup(keys).clone();
	}

	/**
	 * dressup and clone
	 * 
	 * @param keys 键名序列
	 * @return ra 本身
	 */
	public ArrayRecord dressupClone(final String keys) {
		return this.dressupClone(keys.split("[,;/\\\\]+"));
	}

	/**
	 * dressup and clone
	 * 
	 * @param keys 键名序列
	 * @return ra 本身
	 */
	public IRecord dressupDuplicate(final String[] keys) {
		return this.dressup(keys).duplicate();
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
	 * 浅拷贝:注意clone 与 duplicate的区别是: <br>
	 * 1)clone返回实际类型而duplicate返回IRecord <br>
	 * 2)clone是浅拷贝,而duplicate是深拷贝 <br>
	 */
	@Override
	public ArrayRecord clone() {
		return new ArrayRecord(this.keys, this.values);
	}

	/**
	 * setValues 别名： 为 一个数组values 指定键名索引
	 * 
	 * @param values 值数据
	 * @return ra 本身
	 */
	public ArrayRecord attach(final Object[] values) {
		return this.setValues(values);
	}

	/**
	 * attach and clone
	 * 
	 * @param values 值数据
	 * @return ra 本身
	 */
	public ArrayRecord attachClone(final Object[] values) {
		return this.attach(values).clone();
	}

	/**
	 * attach and duplicate
	 * 
	 * @param values 值数据
	 * @return ra 本身
	 */
	public IRecord attachDuplicate(final Object[] values) {
		return this.attach(values).duplicate();
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
	 * @param keys 键名序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord of(final String... keys) {
		return new ArrayRecord(keys, null);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys 键名序列,逗号[,;/\\]进行分割
	 * @return ArrayRecord
	 */
	public static ArrayRecord of(final String keys) {
		return ArrayRecord.of(keys.split("[,;/\\\\]+"));
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
