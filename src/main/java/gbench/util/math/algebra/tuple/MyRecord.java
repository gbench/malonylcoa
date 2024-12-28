package gbench.util.math.algebra.tuple;

import static gbench.util.math.algebra.tuple.IRecord.FT;
import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * 数据记录对象的实现
 * 
 * @author gbench
 *
 */
public class MyRecord implements IRecord {

	/**
	 * 构造数据记录对象
	 * 
	 * @param data IRecord数据
	 */
	public MyRecord(final Map<?, ?> data) {
		data.forEach((k, v) -> {
			this.data.put(k instanceof String ? (String) k : k + "", v);
		}); // forEach
	}

	/**
	 * 构造空数据记录
	 * 
	 */
	public MyRecord() {
		this(new LinkedHashMap<String, Object>());
	}

	@Override
	public IRecord set(final String key, final Object value) {
		this.data.put(key, value);
		return this;
	}

	@Override
	public IRecord remove(String key) {
		this.data.remove(key);
		return this;
	}

	@Override
	public IRecord build(final Object... kvs) {
		return MyRecord.REC(kvs);
	}

	@Override
	public Map<String, Object> toMap() {
		return this.data;
	}

	@Override
	public List<String> keys() {
		return new ArrayList<>(this.data.keySet());
	}

	/**
	 * 
	 */
	@Override
	public Object get(final String key) {
		return this.data.get(key);
	}

	@Override
	public IRecord duplicate() {
		final var _rec = new MyRecord();
		_rec.data.putAll(this.data);
		return _rec;
	}

	@Override
	public Iterator<Tuple2<String, Object>> iterator() {
		return this.data.entrySet().stream().map(e -> P(e.getKey(), e.getValue())).iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple2<String, Object>[] data() {
		return (Tuple2<String, Object>[]) this.data.entrySet().stream().map(e -> P(e.getKey(), e.getValue()))
				.toArray(Tuple2[]::new);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.toMap());
	}

	@Override
	public String toString() {
		return this.data.toString();
	}

	/**
	 * 转化成字符串
	 * 
	 * @param preprocessor    值的预处理器，若返回 null，则在最终结果中给予过滤。
	 * @param e_delimiter     元素间分隔符
	 * @param e_prefix        元素前缀
	 * @param e_suffix        元素结尾
	 * @param ee_delimiter    集合元素间分隔符
	 * @param ee_prefix       集合元素前缀
	 * @param ee_suffix       集合元素后缀
	 * @param kvp_delimiter   key-value之间的分隔
	 * @param key_prefix      key 前缀
	 * @param key_suffix      key 后缀
	 * @param value_formatter 值的格式化函数 vlaue->string
	 * @return 文本化描述
	 */
	public String toString(final Function<Object, ?> preprocessor, final String e_delimiter, final String e_prefix,
			final String e_suffix, final String ee_delimiter, final String ee_prefix, final String ee_suffix,
			final String kvp_delimiter, final String key_prefix, final String key_suffix,
			final Function<Object, String> value_formatter) {

		final var joiner = new StringJoiner(e_delimiter, e_prefix, e_suffix); // 连接器
		this.forEach(tup -> {
			final var key = tup._1;
			final Function<Object, ?> _preprocessor = preprocessor == null // 是否存在预处理函数
					? o -> IRecord.tidy(o, e -> {
						if (e instanceof IRecord) { // IRecord
							return (IRecord) e;
						} else if (e instanceof Map<?, ?>) { // Map
							return new MyRecord((Map<?, ?>) e);
						} else { //
							return e == null ? "null" : e;
						} // if
					}) // 默认与处理
					: preprocessor; // 指定预处理
			final var value = _preprocessor.apply(tup._2); // 预处理器
			final var _v = MyRecord.kvpfmt(key, value, _preprocessor, e_delimiter, e_prefix, e_suffix, ee_delimiter,
					ee_prefix, ee_suffix, kvp_delimiter, key_prefix, key_suffix, value_formatter);
			joiner.add(_v);
		}); // forEach

		return joiner.toString();
	}

	/**
	 * 
	 * @param value
	 * @param preprocessor    值的预处理器，若返回 null，则在最终结果中给予过滤。
	 * @param e_delimiter     元素间分隔符
	 * @param e_prefix        元素前缀
	 * @param e_suffix        元素结尾
	 * @param ee_delimiter    集合元素间分隔符
	 * @param ee_prefix       集合元素前缀
	 * @param ee_suffix       集合元素后缀
	 * @param kvp_delimiter   key-value之间的分隔
	 * @param key_prefix      key 前缀
	 * @param key_suffix      key 后缀
	 * @param value_formatter 值的格式化函数
	 * @return 文本化描述
	 */
	private static String kvpfmt(final String key, final Object value, final Function<Object, ?> preprocessor,
			final String e_delimiter, final String e_prefix, final String e_suffix, final String ee_delimiter,
			final String ee_prefix, final String ee_suffix, final String kvp_delimeter, final String key_prefix,
			final String key_suffix, final Function<Object, String> value_formatter) {

		final Object _value = preprocessor.apply(value); // 预处理器
		if (_value == null) { // 过滤空值结果
			return null;
		} else {
			final var _v = valuefmt(value, preprocessor, e_delimiter, e_prefix, e_suffix, ee_delimiter, ee_prefix,
					ee_suffix, kvp_delimeter, key_prefix, key_suffix, value_formatter);
			return FT("$0$1$2$3$4", key_prefix, key, key_suffix, kvp_delimeter, _v);
		}
	}

	/**
	 * 值的格式化
	 * 
	 * @param value           值对象
	 * @param preprocessor    值的预处理器，若返回 null，则在最终结果中给予过滤。
	 * @param e_delimiter     元素间分隔符
	 * @param e_prefix        元素前缀
	 * @param e_suffix        元素结尾
	 * @param ee_delimiter    集合元素间分隔符
	 * @param ee_prefix       集合元素前缀
	 * @param ee_suffix       集合元素后缀
	 * @param kv_delim        key-value之间的分隔
	 * @param key_prefix      key 前缀
	 * @param key_suffix      key 后缀
	 * @param value_formatter 值的格式化函数 value->string
	 * 
	 */
	public static String valuefmt(final Object value, final Function<Object, ?> preprocessor, final String e_delimiter,
			final String e_prefix, final String e_suffix, final String ee_delimiter, final String ee_prefix,
			final String ee_suffix, String kv_delim, final String key_prefix, final String key_suffix,
			final Function<Object, String> value_formatter) {

		final var flag = value != null && value.getClass().isArray(); // 值是否是数组类型
		if (flag || value instanceof Collection) { // 集合类型的处理
			final var cc_joiner = new StringJoiner(ee_delimiter, ee_prefix, ee_suffix);
			final var ee = flag ? Arrays.asList((Object[]) value) : (Collection<?>) value; // 如果是数组转换成列表结构
			for (final var e : ee) { // 集合类型
				final var _v = MyRecord.valuefmt(e, preprocessor, e_delimiter, e_prefix, e_suffix, ee_delimiter,
						ee_prefix, ee_suffix, kv_delim, key_prefix, key_suffix, value_formatter);
				cc_joiner.add(_v);
			} // for
			return cc_joiner.toString();
		} else if (value instanceof Map || value instanceof IRecord) { // IRecord 类型的处理
			final var myrec = value instanceof Map ? MyRecord.REC((Map<?, ?>) value) : ((IRecord) value);
			final var _v = myrec.toString(preprocessor, e_delimiter, e_prefix, e_suffix, ee_delimiter, ee_prefix,
					ee_suffix, kv_delim, key_prefix, key_suffix);
			return _v;
		} else { // 普通纸类型的处理
			return value_formatter.apply(value);
		} // if
	}

	/**
	 * Iterable 转 List
	 * 
	 * @param <T>      元素类型
	 * @param iterable 可迭代类型
	 * @param maxSize  最大元素长度
	 * @return 元素类型
	 */
	public static <T> List<T> iterable2list(final Iterable<T> iterable, final long maxSize) {
		final var aa = new ArrayList<T>();
		StreamSupport.stream(iterable.spliterator(), false).limit(maxSize).forEach(aa::add);
		return aa;
	}

	/**
	 * 标准版的记录生成器, map 生成的是LinkedRecord
	 * 
	 * @param <T> 类型占位符
	 * @param kvs 键,值序列:key0,value0,key1,value1,.... <br>
	 *            Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return IRecord对象
	 */
	@SafeVarargs
	public static <T> IRecord REC(final T... kvs) {
		final var n = kvs.length;
		final var data = new LinkedHashMap<String, Object>();

		if (n == 1) { // 单一参数情况
			final var obj = kvs[0];
			if (obj instanceof Map) { // Map情况的数据处理
				((Map<?, ?>) obj).forEach((k, v) -> { // 键值的处理
					data.put(k + "", v);
				}); // forEach
			} else if (obj instanceof IRecord) {// IRecord 对象类型 复制对象数据
				data.putAll(((IRecord) obj).toMap());
			} // if
		} else { // 键名减值序列
			for (int i = 0; i < n - 1; i += 2) {
				data.put(kvs[i].toString(), kvs[i + 1]);
			}
		} // if

		return new MyRecord(data);
	}

	@Override
	public final IRecord CC(final List<Tuple2<String, Object>> as) {
		final var _data = new LinkedHashMap<String, Object>();
		for (var a : as) {
			_data.put(a._1(), a._2());
		}
		return new MyRecord(_data);
	}

	private LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
}