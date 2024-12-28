package gbench.util.lisp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.json.MyJson;
import gbench.util.type.Types;

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
				final Object[] vv = values == null ? new Object[n + 1] : Arrays.copyOf(values, n + 1);
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
	 * 应用mapper作用于值数组
	 * 
	 * @param mapper 元素变换器 [t]-&gt;u
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
	 * 键名列表
	 * 
	 * @return 键名列表
	 */
	public String[] getKeys() {
		return keys;
	}

	/**
	 * 设置键名列表
	 * 
	 * @param keys 键名列表
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
	public ArrayRecord dresscln(final String[] keys) {
		return this.dressup(keys).clone();
	}

	/**
	 * dressup and clone
	 * 
	 * @param keys 键名序列
	 * @return ra 本身
	 */
	public ArrayRecord dresscln(final String keys) {
		return this.dresscln(keys.split(delims));
	}

	/**
	 * dressup and clone
	 * 
	 * @param keys 键名序列
	 * @return ra 本身
	 */
	public IRecord dressdup(final String[] keys) {
		return this.dressup(keys).duplicate();
	}

	/**
	 * 设置键名列表
	 * 
	 * @param keys 键名列表
	 * @return ra 本身
	 */
	public ArrayRecord setKeys(final Iterable<String> keys) {
		this.keys = StreamSupport.stream(keys.spliterator(), false).toArray(String[]::new);
		return this;
	}

	/**
	 * 获取值列表
	 * 
	 * @return 值列表
	 */
	public Object[] getValues() {
		return values;
	}

	/**
	 * 设置值列表
	 * 
	 * @param values 值列表
	 * @return ra 本身
	 */
	public ArrayRecord setValues(final Object[] values) {
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
	 * 数据共享是附加：提供一套keys用于访问数据，<br>
	 * 即 提供IRecord的键值对儿算法来使用数据values <br>
	 * setValues 别名： 为 一个数组values 指定键名索引. 参见与wrap方法的区别。
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
	public ArrayRecord attachcln(final Object[] values) {
		return this.attach(values).clone();
	}

	/**
	 * attach and duplicate
	 * 
	 * @param values 值数据
	 * @return ra 本身
	 */
	public IRecord attachdup(final Object[] values) {
		return this.attach(values).duplicate();
	}

	/**
	 * setValues 别名： 为 一个数组values 指定键名索引.<br>
	 * wrap 和 attach 的区别就是 attach 参数是 对象数组 Object[] <br>
	 * 数据类型的参数Object[]而wrap是Iterabl类型的参数。<br>
	 * attach强调共享共享数据，用 IRecord的接口来操作数据。 <br>
	 * 在IRecord上的修在可以在另外的以values为成员的对象比如DataMatrix上反应 <br>
	 * 或者DataMatrix上的操作可以在IRecord上反应。 <br>
	 * 而wrap强调在vlaues之上再包裹一层IRecord接口。存粹是为了为底层数据提供一个IRecord数据访问而已。 没有数据共享的思想。
	 * 
	 * @param values 值数据
	 * @return ra 本身
	 */
	public ArrayRecord wrap(final Iterable<?> values) {
		return this.setValues(Types.itr2array(values));
	}

	/**
	 * 把一个键名序列转换成索引序列
	 * 
	 * @param keys 键名序列
	 * @return 索引序列
	 */
	public Stream<Integer> indexOfS(final String... keys) {
		return null == keys ? null : Stream.of(keys).map(this::indexOf);
	}

	/**
	 * 把一个索引序列转换成键序列
	 * 
	 * @param indices 索引序列
	 * @return 键名序列
	 */
	public Stream<String> keyOfS(final Integer... indices) {
		return null == keys ? null : Stream.of(indices).map(this::keyOf);
	}

	/**
	 * 生成一个索引访问器序列
	 * 
	 * @param <T>    参数类型
	 * @param <U>    结果类型
	 * @param keys   键名序列
	 * @param mapper 索引变换函数 (t,idx)-&gt;U
	 * @return 索引序列
	 */
	public <T, U> Stream<Function<T, U>> generateS(final String[] keys, final BiFunction<T, Integer, U> mapper) {
		return this.indexOfS(null == keys ? this.keys : keys).map(i -> (Function<T, U>) t -> mapper.apply(t, i));
	}

	/**
	 * 生成一个索引访问器序列
	 * 
	 * @param <T>      参数类型
	 * @param <U>      结果类型
	 * @param keys     键名序列,逗号[,;/\\]进行分割
	 * @param accessor 索引访问器 (t,i)-&gt;u
	 * @return 索引序列
	 */
	public <T, U> Stream<Function<T, U>> generateS(final String keys, final BiFunction<T, Integer, U> accessor) {
		return this.generateS(keys.split(delims), accessor);
	}

	/**
	 * 生成一个索引访问器序列
	 * 
	 * @param <T>      参数类型
	 * @param <U>      结果类型
	 * @param indices  键名序列
	 * @param accessor 索引访问器 (t,i)-&gt;u
	 * @return 索引访问器序列
	 */
	public <T, U> Stream<Function<T, U>> generateS(final Integer[] indices, final BiFunction<T, Integer, U> accessor) {
		return (indices == null ? Stream.iterate(0, i -> i + 1).limit(this.size()) : Stream.of(indices))
				.map(i -> (Function<T, U>) t -> accessor.apply(t, i));
	}

	/**
	 * 生成一个索引访问器序列
	 * 
	 * @param <T>      参数类型
	 * @param <U>      结果类型
	 * @param accessor 索引访问器 (t,i)-&gt;u
	 * @return 索引访问器序列
	 */
	public <T, U> Stream<Function<T, U>> generateS(final BiFunction<T, Integer, U> accessor) {
		return this.generateS((Integer[]) null, accessor);
	}

	/**
	 * 更新式添加<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值 <br>
	 * 若 kvs 长度为 1 <br>
	 * 1) IRecord 或 Map 类型 根据 (键,值) 序列给予 元素添加 <br>
	 * 2) Iterable 类型 索引序号（从0开始）为 键名, 元素值 进行 (键,值)序列添加
	 *
	 * @param kvs 键名键值序列
	 * @return 对象本身
	 */
	@Override
	public IRecord add(final Object... kvs) {
		if (kvs.length == 1) {
			final Object obj = kvs[0];
			if (obj instanceof IRecord) { // 记录类型
				// 可迭代类型
				((IRecord) obj).forEach((k, v) -> this.add(k, v));
			} else if (obj instanceof Map) { // Map类型
				return ((Map<?, ?>) obj).entrySet().stream().reduce((IRecord) this,
						(acc, a) -> acc.add(a.getKey(), a.getValue()), IRecord::add);
			} else if (obj instanceof Iterable<?> itr) { // 可迭代类型
				return Types.itr2stream(itr).map(Tuple2.snb(0)).map(e -> e.fmap1(k -> k + "")).reduce((IRecord) this,
						(acc, a) -> acc.add(a._1 + "", a._2), IRecord::add);
			} else {
				// do nothing
			}
		} else {
			return IRecord.slidingS(kvs, 2, 2, true).map(wnd -> Tuple2.of(wnd.get(0) + "", wnd.get(1))) // // 窗口遍历
					.reduce((IRecord) this, (acc, a) -> acc.add(a._1, a._2), IRecord::add);
		}
		return this;
	}

	/**
	 * 前置添加<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值 <br>
	 * 若 kvs 长度为 1 <br>
	 * 1) IRecord 或 Map 类型 根据 (键,值) 序列给予 元素添加 <br>
	 * 2) Iterable 类型 索引序号（从0开始）为 键名, 元素值 进行 (键,值)序列添加
	 *
	 * @param kvs 键名键值序列
	 * @return 新创建的数据。
	 */
	@Override
	public IRecord prepend(final Object... kvs) {
		return REC(kvs).add(this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { ArrayRecord.class, this.keys, this.values });
	}

	@Override
	public boolean equals(final Object obj) {
		return obj == this || (obj instanceof ArrayRecord ra
				? Objects.equals(ra.keys, this.keys) && Objects.equals(ra.values, this.values)
				: false);
	}

	/**
	 * 构建一个键名键值序列 指定的 IRecord
	 *
	 * @param <T> 元素类型
	 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return 新生成的IRecord
	 */
	@SafeVarargs
	public static <T> IRecord REC(final T... kvs) {
		return ra2(kvs);
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
		return ArrayRecord.of(keys.split(delims));
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
	 * 构建一个键名键值序列 指定的 IRecord
	 *
	 * @param <T> 元素类型
	 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return 新生成的IRecord
	 */
	@SafeVarargs
	public static <T> ArrayRecord ra2(final T... kvs) {
		final int len = kvs.length; // 输入数据长度
		final Object[] data = len % 2 == 0 ? kvs : Arrays.copyOf(kvs, len + 1);
		final int size = data.length; // 新数据长度
		final int n = size / 2; // 新IRecord长度
		final String[] keys = new String[n];
		final Object[] values = new Object[n];

		for (int i = 0; i < n; i++) {
			final Object key = data[2 * i];
			keys[i] = key instanceof String k ? k : key + "";
			values[i] = data[2 * i + 1];
		} // for

		return new ArrayRecord(keys, values);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys 键名序列,逗号[,;/\\]进行分割
	 * @return ArrayRecord
	 */
	public static ArrayRecord ra(final String keys) {
		return ArrayRecord.of(keys);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys 键名序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord ra(final String... keys) {
		return ArrayRecord.of(keys);
	}

	/**
	 * ArrayRecord
	 * 
	 * @param keys 键名序列
	 * @return ArrayRecord
	 */
	public static ArrayRecord ra(final Iterable<String> keys) {
		return ArrayRecord.of(Types.itr2array(keys));
	}

	/**
	 * 序列号
	 */
	private static final long serialVersionUID = -4990916390989613873L;
	private static String delims = "[,;/\\\\]+";
	private String[] keys; // 键名
	private Object[] values; // 键值
}
