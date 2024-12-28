package gbench.util.lisp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.type.Times;

/**
 * 数据记录对象
 *
 * @author xuqinghua
 */
public interface IRecord extends Iterable<Tuple2<String, Object>>, Comparable<IRecord> {

	/**
	 * 根据键名进行取值
	 *
	 * @param <T> 返回值类型
	 * @param key 键名
	 * @return 键key的值
	 */
	<T> T get(final String key);

	/**
	 * 设置键，若 key 与 老的 键 相同则 覆盖 老的值
	 *
	 * @param key   新的 键名
	 * @param value 键值
	 * @return 对象本身
	 */
	IRecord set(String key, Object value);

	/**
	 * 除掉键 key 的值
	 *
	 * @param key 新的 键名
	 * @return 对象本身(移除了key)
	 */
	IRecord remove(String key);

	/**
	 * 转换成json 字符串
	 *
	 * @return json 字符串
	 */
	String json();

	/**
	 * 数据复制
	 *
	 * @return 当前对象的拷贝
	 */
	IRecord duplicate();

	/**
	 * 构建一个键名键值序列 指定的 IRecord
	 *
	 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return 新生成的IRecord
	 */
	IRecord build(final Object... kvs);

	/**
	 * 键名序列
	 *
	 * @return 键名列表
	 */
	List<String> keys();

	/**
	 * 返回一个 Map 结构<br>
	 * 非递归进行变换
	 *
	 * @return 一个 键值 对儿 的 列表 [(key,map)]
	 */
	Map<String, Object> toMap();

	/**
	 * 根据键名索引进行取值
	 *
	 * @param <T> 返回值类型
	 * @param idx 键名索引从0开始
	 * @return 键名索引idx的值
	 */
	default <T> T get(final int idx) {
		return this.get(this.keyOf(idx));
	}

	/**
	 * IRecord 变换
	 * 
	 * @param <T>    结果类型
	 * @param mapper rec->t
	 * @return T 类型结果
	 */
	default <T> T get(final Function<IRecord, T> mapper) {
		return this.mutate(mapper);
	}

	/**
	 * 带有缺省值计算的值获取函数，<br>
	 * defaultEvaluator 计算的结果并不给予更新到this当中。这是与computeIfAbsent不同的。
	 *
	 * @param <T>              返回值类型
	 * @param key              键名
	 * @param defaultEvaluator 缺省值计算函数(key)->obj
	 * @return 缺值计算的值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(final String key, Function<String, T> defaultEvaluator) {
		return this.opt(key).map(e -> (T) e).orElse(defaultEvaluator.apply(key));
	}

	/**
	 * 构造 IRecord <br>
	 * 按照构建器的 键名序列表，依次把objs中的元素与其适配以生成 IRecord <br>
	 * {key0:objs[0],key1:objs[1],key2:objs[2],...}
	 *
	 * @param objs 值序列, 若 objs 为 null 则返回null, <br>
	 *             若 objs 长度不足以匹配 keys 将采用 循环补位的仿制给予填充 <br>
	 *             若 objs 长度为0则返回一个空对象{},注意是没有元素且不是null的对象
	 * @return IRecord 对象 若 objs 为 null 则返回null
	 */
	default IRecord get(final Object... objs) {
		if (objs == null) { // 空值判断
			return null;
		}

		final int n = objs.length;
		final List<String> keys = this.keys();
		final int size = this.keys().size();
		final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();

		for (int i = 0; n > 0 && i < size; i++) {
			final String key = keys.get(i);
			final Object value = objs[i % n];
			data.put(key, value);
		} // for

		return this.build(data);
	}

	/**
	 * 根据键名提取数据(pathgetS单层路径别名) <br>
	 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
	 *
	 * @param <T>    元素类型
	 * @param <U>    结果（流）：元素类型
	 * @param key    键名
	 * @param mapper 元素值变换函数 t->u
	 * @return U类型的流
	 */
	default <T, U> Stream<U> getS(final String key, final Function<T, U> mapper) {
		return this.pathgetS(key, mapper);
	}

	/**
	 * 根据键名索引提取数据 <br>
	 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
	 *
	 * @param <T>    元素类型
	 * @param <U>    结果（流）：元素类型
	 * @param idx    键名索引,从0开始
	 * @param mapper 元素值变换函数 t->u
	 * @return U类型的流
	 */
	default <T, U> Stream<U> getS(final int idx, final Function<T, U> mapper) {
		return this.getS(this.keyOf(idx), mapper);
	}

	/**
	 * 根据键名提取数据(pathgetS单层路径别名) <br>
	 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
	 *
	 * @param <T>    元素类型
	 * @param <U>    结果（流）：元素类型
	 * @param key    键名
	 * @param mapper 元素值变换函数 t->u
	 * @return U类型的列表
	 */
	default <T, U> List<U> gets(final String key, final Function<T, U> mapper) {
		return this.getS(key, mapper).toList();
	}

	/**
	 * 根据键名索引提取数据 <br>
	 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
	 *
	 * @param <T>    元素类型
	 * @param <U>    结果（流）：元素类型
	 * @param idx    键名索引,从0开始
	 * @param mapper 元素值变换函数 t->u
	 * @return U类型的列表
	 */
	default <T, U> List<U> gets(final int idx, final Function<T, U> mapper) {
		return this.gets(this.keyOf(idx), mapper);
	}

	/**
	 * 键名序列
	 *
	 * @return 键名列表
	 */
	default Stream<String> keyS() {
		return this.keys().stream();
	}

	/**
	 * 返回一个 Map 结构,递归遍历<br>
	 *
	 * @return 一个 键值 对儿 的 列表 [(key,map)]
	 */
	default Map<String, Object> toMap2() {
		final Map<String, Object> data = new LinkedHashMap<String, Object>();
		this.forEach((key, value) -> {
			final boolean is_record = value instanceof IRecord; // 检测是否是IRecord
			if (is_record) { // 优先检测 is_record
				final IRecord rec = (IRecord) value;
				data.put(key, rec.toMap2());
			} else if (value instanceof Iterable /* && !is_record */) { // 因为IRecord 即是Iterable也是IRecord 所以需要区分
				final List<?> value_ll = StreamSupport.stream(((Iterable<?>) value).spliterator(), false)
						.map(e -> e instanceof IRecord ? ((IRecord) e).toMap2() : e).collect(Collectors.toList());
				data.put(key, value_ll);
			} else if (value instanceof Stream) {
				@SuppressWarnings("unchecked")
				final List<?> value_ll = ((Stream<Object>) value) //
						.map(e -> e instanceof IRecord ? ((IRecord) e).toMap2() : e) //
						.collect(Collectors.toList());
				data.put(key, value_ll);
			} else {
				data.put(key, value);
			}
		});
		return data;
	}

	/**
	 * 数据元素便利访问
	 *
	 * @param action 遍历的回调函数 (key,value)->{}
	 * @return this 对象本身。
	 */
	default IRecord forEach(BiConsumer<String, Object> action) {
		this.toMap().forEach(action);
		return this;
	}

	/**
	 * 设置键，若 idx 与 老的 键 相同则 覆盖 老的值
	 *
	 * @param idx   键名索引，从0开始
	 * @param value 数值
	 * @return this 对象本身
	 */
	default IRecord set(final Integer idx, final Object value) {
		final String key = this.keyOf(idx);
		this.add(key, value);
		return this;
	}

	/**
	 * 尝试强转数据类型为U
	 *
	 * @param key    键名
	 * @param mapper 值变换函数 t->u
	 * @param <T>    源数据类型
	 * @param <U>    返回值类型
	 * @return U 类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U as(final String key, Function<T, U> mapper) {
		final Object v = this.get(key);
		final U u = mapper == null ? (U) v : mapper.apply((T) v);
		return u;
	}

	/**
	 * 尝试强转数据类型为U
	 *
	 * @param index  键名索引从0开始
	 * @param mapper 值变换函数 t->u
	 * @param <T>    源数据类型
	 * @param <U>    返回值类型
	 * @return U 类型的结果
	 */
	default <T, U> U as(final int index, Function<T, U> mapper) {
		return this.as(this.keyOf(index), mapper);
	}

	/**
	 * 尝试强转数据类型为T
	 *
	 * @param key         键名
	 * @param placeholder 类型,占位符 用于表示 返回值类型
	 * @param <T>         返回值类型
	 * @return t 类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <T> T as(final String key, final T placeholder) {
		final T _t = (T) this.get(key);
		return _t;
	}

	/**
	 * 尝试强转数据类型为T
	 *
	 * @param idx         键名索引,从0开始
	 * @param placeholder 类型,占位符 用于表示 返回值类型
	 * @param <T>         返回值类型
	 * @return t 类型的结果
	 */
	default <T> T as(final int idx, T placeholder) {
		return this.as(this.keyOf(idx), placeholder);
	}

	/**
	 * 除掉键 idx 的值
	 *
	 * @param idx 键名索引,从0开始
	 * @return 对象本身(移除了key)
	 */
	default IRecord remove(final Integer idx) {
		return this.remove(this.keyOf(idx));
	}

	/**
	 * 提取record类型的结果 <br>
	 * <p>
	 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
	 *
	 * @param <X>          源数据类型
	 * @param <Y>          目标数据类型,即 IRecord,Map,Collection,Array 其中之一
	 * @param key          键名
	 * @param preprocessor 预处理器
	 * @return IRecord类型的值
	 */
	@SuppressWarnings("unchecked")
	default <X, Y> IRecord rec(final String key, final BiFunction<String, X, Y> preprocessor) {
		final Y value = preprocessor.apply(key, (X) this.get(key));
		final Function<Collection<?>, IRecord> clcn2rec = tt -> {
			int i = 0;
			final IRecord rec = this.build(); // 创建一个空IRecord
			for (final Object t : tt) {
				rec.add("" + (i++), t);
			} // for
			return rec;
		}; // clc2rec

		if (value instanceof IRecord) {
			return (IRecord) value;
		} else if (value instanceof Map) {
			return this.build((Map<?, ?>) value);
		} else if (value instanceof Collection || (value != null && value.getClass().isArray())) {
			final Collection<Object> tt = value instanceof Collection // 值类型判断
					? (Collection<Object>) value // 集合类型
					: (Collection<Object>) Arrays.asList((Object[]) value); // 数组类型
			return clcn2rec.apply(tt);
		} else if (value instanceof Iterable) {
			return clcn2rec.apply(IRecord.itr2list((Iterable<?>) value));
		} else if (value instanceof String) { // json 结构的数据处理
			return IRecord.REC(value);
		} else {
			return null;
		}
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param <X>          元素类型
	 * @param <Y>          元素类型，需要为IRecord,Map,Collection,Array其中Collection和Array任一
	 * @param <T>          元素类型
	 * @param <U>          结果类型
	 * @param path         键名路径
	 * @param preprocessor 预处理器 x->y
	 * @param mapper       值变换函数 t->u
	 * @return U类型的值
	 */
	@SuppressWarnings("unchecked")
	default <X, Y, T, U> U pathget(final String[] path, final BiFunction<String, X, Y> preprocessor,
			final Function<T, U> mapper) {
		final String key = path[0].trim();
		final int len = path.length;

		if (len > 1) {
			final IRecord _rec = this.rec(key, preprocessor);
			final String[] _path = Arrays.copyOfRange(path, 1, len);
			return _rec != null ? _rec.pathget(_path, preprocessor, mapper) : null;
		} else {
			return mapper.apply((T) this.get(key));
		} // if
	}

	/**
	 * 根据路径提取数据 <br>
	 * <p>
	 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
	 *
	 * @param <X>          元素类型
	 * @param <Y>          元素类型Y 需要为
	 *                     IRecord,Map,Collection,Array其中Collection和Array任一
	 * @param <T>          元素类型
	 * @param <U>          结果类型
	 * @param path         键名路径
	 * @param preprocessor 预处理器 x->y
	 * @param mapper       值变换函数 t->u
	 * @return U类型的值
	 */
	default <X, Y, T, U> U pathget(final String path, final BiFunction<String, X, Y> preprocessor,
			final Function<T, U> mapper) {
		return this.pathget(path.split("[/,]+"), preprocessor, mapper);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param <T>    元素类型
	 * @param <U>    结果类型
	 * @param path   键名路径 如 a/b/c
	 * @param mapper 值变换函数 t->u
	 * @return U类型的值
	 */
	default <T, U> U pathget(final String path, final Function<T, U> mapper) {
		return this.pathget(path, (k, e) -> e, mapper);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default String pathstr(final String path) {
		return this.pathget(path, e -> Optional.ofNullable(e).map(Object::toString).orElse(null));
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default Boolean pathbool(final String path) {
		return this.pathget(path, e -> REC("0", e).bool(0));
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default Integer pathi4(final String path) {
		return this.pathget(path, IRecord.obj2int());
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default Long pathlng(final String path) {
		return this.pathget(path, e -> REC("0", e).lng(0));
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default LocalTime pathlt(final String path) {
		return this.pathget(path, e -> REC("0", e).lt(0));
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default Double pathdbl(final String path) {
		return this.pathget(path, IRecord.obj2dbl());
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default LocalDateTime pathldt(final String path) {
		return this.pathget(path, Times::asLocalDateTime);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default LocalDate pathld(final String path) {
		return this.pathget(path, Times::asLocalDate);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default Timestamp pathts(final String path) {
		return this.pathget(path, e -> REC("0", e).timestamp(0));
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值
	 */
	default Date pathdate(final String path) {
		return this.pathts(path);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param <T>      键值变换函数的源类型
	 * @param <U>      键值变换函数的目标类型
	 * @param path     键名路径 如 a/b/c
	 * @param streamer 元素对象流构建器 t->[u]
	 * @return U类型的元素对象流
	 */
	default <T, U> Stream<U> pathllS(final String path, final Function<T, Stream<U>> streamer) {
		return this.pathget(path, streamer);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param path   键名路径 如 a/b/c
	 * @param mapper 元素对象变换器 t->u
	 * @return U类型的元素对象流
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> pathllS2(final String path, final Function<T, U> mapper) {
		return this.pathllS(path).map(e -> mapper.apply((T) e));
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param path 键名路径 如 a/b/c
	 * @return 元素对象流, 路径不存在或是值为null的时候返回null值
	 */
	default Stream<Object> pathllS(final String path) {
		return this.pathgetS(path, e -> e);
	}

	/**
	 * 根据路径提取数据
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param path   键名路径 如 a/b/c
	 * @param mapper 元素对象变换器 t->u
	 * @return U类型的元素对象流
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> pathgetS(final String path, final Function<T, U> mapper) {
		return this.pathget(path, (e) -> {
			return Optional.ofNullable(e).map(_e -> {
				final Class<?> tclass = _e.getClass(); // 提取元素类型
				if (tclass.isArray()) { // 数组结构
					return Stream.of((T[]) _e);
				} else if (e instanceof Collection) { // Collection 结构
					return ((Collection<T>) _e).stream();
				} else if (e instanceof Stream) {// Stream 结构
					return (Stream<T>) _e;
				} else if (e instanceof Map) { // map结构
					return (Stream<T>) ((Map<Object, Object>) _e).entrySet().stream();
				} else { // 其他结构
					return Stream.of((T) _e);
				} // if
			}).map(stream -> stream.map(mapper)).orElse(Stream.empty());
		}); // pathget
	}

	/**
	 * 把idx转key
	 *
	 * @param idx 键名索引 从0开始
	 * @return 索引转键名
	 */
	default String keyOf(final int idx) {

		final List<String> kk = this.keys();
		return idx < kk.size() ? kk.get(idx) : null;
	}

	/**
	 * 键名索引
	 *
	 * @param key 键名
	 * @return 键名索引 从0开始, key 为null 时返回null
	 */
	default Integer indexOf(final String key) {

		if (key == null) {
			return null;
		}

		final List<String> kk = this.keys();

		for (int i = 0; i < kk.size(); i++) {
			final String _key = kk.get(i);
			if (key.equals(_key)) {
				return i;
			}
		}

		return null;
	}

	/**
	 * 返回值数据流
	 *
	 * @return 值数据流
	 */
	default Stream<Object> valueS() {
		return this.keys().stream().map(this::get);
	}

	/**
	 * 返回值列表
	 *
	 * @return 值列表
	 */
	default List<Object> values() {
		return this.valueS().collect(Collectors.toList());
	}

	/**
	 * 提取键名所标定的值
	 *
	 * @param key 键名
	 * @return Optional 的键值
	 */
	default Optional<Object> opt(final String key) {
		return this.get(key) == null ? Optional.empty() : Optional.of(this.get(key));
	}

	/**
	 * 提取键名索引所标定的值
	 *
	 * @param idx 键名索引，从0开始
	 * @return Optional 的键值
	 */
	default Optional<Object> opt(final Integer idx) {
		return this.opt(this.keyOf(idx));
	}

	/**
	 * 把key列转换成逻辑值
	 *
	 * @param idx 键名索引,从0开始
	 * @return 布尔类型
	 */
	default Boolean bool(final Integer idx) {
		return this.bool(this.keyOf(idx));
	}

	/**
	 * 把key列转换成逻辑值<br>
	 * 对于数字或是数字类型的字符串会转换为整数而后与0进行比较，非0为true，0为false
	 *
	 * @param key 键名
	 * @return 布尔类型
	 */
	default Boolean bool(final String key) {
		final Object o = this.get(key);
		boolean b = false;

		if (o instanceof Number) {
			b = ((Number) o).intValue() != 0;
		} else if (o != null) {
			b = Optional.ofNullable(o) //
					.map(IRecord.obj2int()) //
					.map(e -> !e.equals(0)) //
					.orElse(false);
			if (!b) {
				try {
					b = Boolean.parseBoolean(String.valueOf(o));
				} catch (Exception e) {
					// do nothing
				} // try
			} // if
		} else { // o is null
			b = false;
		}

		return b;
	}

	/**
	 * 把key列转换成逻辑值
	 *
	 * @param key           键名
	 * @param default_value 默认值
	 * @return 布尔类型
	 */
	default Boolean bool(final String key, final Boolean default_value) {
		final Boolean b = this.bool(key);
		return b == null ? default_value : b;
	}

	/**
	 * 返回 建明索引 所对应的 键值, Boolean 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Boolean> boolOpt(final int idx) {
		return this.boolOpt(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Boolean 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Boolean> boolOpt(final String key) {
		return this.opt(key).map(e -> this.bool(key));
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Integer i4(final String key) {
		return Optional.ofNullable(IRecord.obj2dbl(null).apply(this.get(key))).map(e -> e.intValue()).orElse(null);
	}

	/**
	 * 返回 键名索引 所对应的 键值, Integer 类型
	 *
	 * @param idx 键名索引 从0开始 从0开始
	 * @return idx 所标定的 值
	 */
	default Integer i4(final Integer idx) {
		return this.i4(this.keyOf(idx));
	}

	/**
	 * 返回 建明索引 所对应的 键值, Integer 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Integer> i4Opt(final int idx) {
		return this.i4Opt(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Integer> i4Opt(final String key) {
		return this.opt(key).map(e -> this.i4(key));
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Long lng(final String key) {
		final Object o = this.get(key);
		if (o instanceof Long) {
			return (Long) o;
		} else if (o instanceof Number) {
			return ((Number) o).longValue();
		} else if ((o + "").matches("^\\d+$")) { // 字符串转换
			return Long.parseLong(o + "");
		} else {
			return this.opt(key).map(IRecord.obj2dbl()).map(e -> e.longValue()).orElse(null);
		}
	}

	/**
	 * 返回 键名索引 所对应的 键值, Integer 类型
	 *
	 * @param idx 键名索引 从0开始 从0开始
	 * @return idx 所标定的 值
	 */
	default Long lng(final Integer idx) {
		return this.lng(this.keyOf(idx));
	}

	/**
	 * 返回 建明索引 所对应的 键值, Long 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Long> lngOpt(final int idx) {
		return this.lngOpt(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Long 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Long> lngOpt(final String key) {
		return this.opt(key).map(e -> this.lng(key));
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Double dbl(final String key) {
		return IRecord.obj2dbl().apply(this.get(key));
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Double dbl(final int key) {
		return this.dbl(this.keyOf(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, Double 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Double> dblOpt(final int idx) {
		return this.dblOpt(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Double> dblOpt(final String key) {
		return Optional.ofNullable(this.dbl(key));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default LocalDateTime ldt(final String key) {
		return this.ldt(key, null);
	}

	/**
	 * 返回 建明索引 所对应的 键值, LocalDateTime 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值
	 */
	default LocalDateTime ldt(final int idx) {
		return this.ldt(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return key 所标定的 值
	 */
	default LocalDateTime ldt(final String key, final LocalDateTime defaultValue) {
		final Object value = this.get(key);

		if (value == null) { // 空结构
			return defaultValue;
		} else { // 非空值
			final LocalDateTime ldt = Times.asLocalDateTime(value);
			return Optional.ofNullable(ldt).orElse(defaultValue);
		} // if
	}

	/**
	 * 返回 建明索引 所对应的 键值, LocalDateTime 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<LocalDateTime> ldtOpt(final int idx) {
		return Optional.ofNullable(this.ldt(idx));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<LocalDateTime> ldtOpt(final String key) {
		return Optional.ofNullable(this.ldt(key));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param idx          键名索引
	 * @param defaultValue 默认值
	 * @return key 所标定的 值
	 */
	default LocalDateTime ldt(final int idx, final LocalDateTime defaultValue) {
		return this.ldt(this.keyOf(idx), defaultValue);
	}

	/**
	 * 返回 key 所对应的 键值, LocalDate 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default LocalDate ld(final String key) {
		return this.ldtOpt(key).map(e -> e.toLocalDate()).orElse(null);
	}

	/**
	 * 返回 建明索引 所对应的 键值, LocalDate 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值
	 */
	default LocalDate ld(final int idx) {
		return this.ldtOpt(idx).map(e -> e.toLocalDate()).orElse(null);
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default LocalTime lt(final String key) {
		return this.lt(key, null);
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param idx 键名索引从0开始
	 * @return key 所标定的 值
	 */
	default LocalTime lt(final Integer idx) {
		return this.lt(this.keyOf(idx));
	}

	/**
	 * 返回 idx 键名索引 所对应的 键值, LocalDateTime 类型
	 *
	 * @param idx          键名索引 从0开始
	 * @param defaultValue 默认值
	 * @return idx 键名索引 所标定的 值
	 */
	default LocalTime lt(final int idx, final LocalTime defaultValue) {
		return this.lt(this.keyOf(idx), defaultValue);
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 *
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return key 所标定的 值
	 */
	default LocalTime lt(final String key, final LocalTime defaultValue) {
		final LocalDateTime ldt = this.ldt(key, null);
		return Optional.ofNullable(ldt).map(LocalDateTime::toLocalTime).orElse(defaultValue);
	}

	/**
	 * 把key键名转换成逻是日期值
	 *
	 * @param key 键名
	 * @return Date 值
	 */
	default Date date(final String key) {
		final LocalDateTime localDateTime = this.ldt(key);
		if (localDateTime == null)
			return null;
		final ZoneId zoneId = ZoneId.systemDefault();
		final ZonedDateTime zdt = localDateTime.atZone(zoneId);
		return Date.from(zdt.toInstant());
	}

	/**
	 * 把idx键名索引转换成逻是日期值
	 *
	 * @param idx 键名索引
	 * @return Date 值
	 */
	default Date date(final int idx) {
		return this.date(this.keyOf(idx));
	}

	/**
	 * 把key列转换成逻是时间戳
	 *
	 * @param key 键名
	 * @return Timestamp 时间错
	 */
	default Timestamp timestamp(final String key) {
		final Object o = this.get(key);
		if (o == null)
			return null;
		if (o instanceof Timestamp)
			return (Timestamp) o;
		final Date time = this.date(key);
		if (time == null)
			return null;
		return new Timestamp(time.getTime());
	}

	/**
	 * 把idx键名索引转换成逻是时间戳
	 *
	 * @param idx 键名索引从0开始
	 * @return Timestamp 时间错
	 */
	default Timestamp timestamp(final int idx) {
		return this.timestamp(this.keyOf(idx));
	}

	/**
	 * 返回 建明索引 所对应的 键值, String 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值
	 */
	default String str(final int idx) {
		return this.str(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, String 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default String str(final String key) {
		final Object obj = this.get(key);

		if (obj == null) {
			return null;
		}

		return obj instanceof String ? (String) obj : obj + "";
	}

	/**
	 * 返回 建明索引 所对应的 键值, String 类型
	 *
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<String> strOpt(final int idx) {
		return this.strOpt(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, String 类型
	 *
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<String> strOpt(final String key) {
		return this.opt(key).map(Object::toString);
	}

	/**
	 * 把 键名元素的值转换为 列表结构 <br>
	 * lla 是 LinkedList apply 的含义，取意为 应用方法 获得 链表
	 *
	 * @param <T>    源数据类型
	 * @param <U>    目标列表元素的数据类型
	 * @param key    键名
	 * @param lister 列表构建器 t->[u]
	 * @return 列表结构的数据, U 类型的列表 [u]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> List<U> lla(final String key, final Function<T, List<U>> lister) {
		final Object value = this.get(key); // 提取数据值

		return lister.apply((T) value);
	}

	/**
	 * 把 键名索引 元素的值转换为 列表结构<br>
	 * lla 是 LinkedList apply 的含义，取意为 应用方法 获得 链表
	 *
	 * @param <T>    源数据类型
	 * @param <U>    目标列表元素的数据类型
	 * @param idx    键名索引 从0开始
	 * @param lister 列表构建器 t->[u]
	 * @return 列表结构的数据, U 类型的列表 [u]
	 */
	default <T, U> List<U> lla(final int idx, final Function<T, List<U>> lister) {
		return this.lla(this.keyOf(idx), lister);
	}

	/**
	 * 把 键名元素的值转换为 列表结构 <br>
	 * lla 是 LinkedList apply 的含义，取意为 应用方法 获得 链表
	 *
	 * @param key 键名
	 * @return 列表结构的数据
	 */
	default List<Object> lla(final String key) {
		return this.lla(key, IRecord::asList);
	}

	/**
	 * 把 键名索引 元素的值转换为 列表结构
	 *
	 * @param idx 键名索引 从0开始
	 * @return 列表结构的数据
	 */
	default List<Object> lla(final int idx) {
		return this.lla(this.keyOf(idx));
	}

	/**
	 * 把 键名索引 元素的值转换为 元素列表
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param key    键名
	 * @param mapper 键值变换函数 t->u
	 * @return U类型的 元素列表
	 */
	default <T, U> List<U> lla2(final String key, final Function<T, U> mapper) {
		return this.llS2(key, mapper).collect(Collectors.toList());
	}

	/**
	 * 把 键名索引 元素的值转换为 元素列表
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param idx    键名索引从0开始
	 * @param mapper 键值变换函数 t->u
	 * @return U类型的 元素列表
	 */
	default <T, U> List<U> lla2(final int idx, final Function<T, U> mapper) {
		return this.lla2(this.keyOf(idx), mapper);
	}

	/**
	 * 把 键名元素的值转换为 元素对象流 <br>
	 * llS 是 LinkedList Stream 的 缩写 根据 lla 的变体,S 表示这是一个 返回 Stream类型的函数
	 *
	 * @param <T>      源数据类型
	 * @param <U>      目标列表元素的数据类型
	 * @param key      键名
	 * @param streamer 元素对象流构建器 t->[u]
	 * @return U 类型的元素对象流 [u]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> llS(final String key, final Function<T, Stream<U>> streamer) {
		final Object value = this.get(key); // 提取数据值

		return streamer.apply((T) value);
	}

	/**
	 * 把 键名索引 元素的值转换为 元素对象流 <br>
	 * llS 是 LinkedList Stream 的 缩写 根据 lla 的变体,S 表示这是一个 返回 Stream类型的函数
	 *
	 * @param <T>      源数据类型
	 * @param <U>      目标列表元素的数据类型
	 * @param idx      键名索引 从0开始
	 * @param streamer 元素对象流构建器 t->[u]
	 * @return U 类型的元素对象流 [u]
	 */
	default <T, U> Stream<U> llS(final int idx, final Function<T, Stream<U>> streamer) {
		return this.llS(this.keyOf(idx), streamer);
	}

	/**
	 * 把 键名 元素的值转换为 元素对象流
	 * <p>
	 * llS 是 LinkedList Stream 的 缩写 根据 lla 的变体,S 表示这是一个 返回 Stream类型的函数
	 *
	 * @param key 键名
	 * @return 元素对象流
	 */
	default Stream<Object> llS(final String key) {
		return this.llS(key, e -> Optional.ofNullable(e).map(IRecord::asList).map(List::stream).orElse(null));
	}

	/**
	 * 把 键名索引 元素的值转换为 元素对象流
	 *
	 * @param idx 键名索引 从0开始
	 * @return 元素对象流
	 */
	default Stream<Object> llS(final int idx) {
		return this.llS(this.keyOf(idx));
	}

	/**
	 * 把 键名 元素的值转换为 元素对象流
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param key    键名
	 * @param mapper 键值变换函数 t->u
	 * @return U类型的元素对象流
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> llS2(final String key, final Function<T, U> mapper) {
		return this.llS(key).map(e -> mapper.apply((T) e));
	}

	/**
	 * 把 键名索引 元素的值转换为 元素对象流
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param idx    键名索引 从0开始
	 * @param mapper 键值变换函数 t->u
	 * @return U类型的元素对象流
	 */
	default <T, U> Stream<U> llS2(final int idx, final Function<T, U> mapper) {
		return this.llS2(this.keyOf(idx), mapper);
	}

	/**
	 * 把 键名 元素的值转换为 元素对象流
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param key    键名
	 * @param mapper 键值变换函数 t->u
	 * @return U类型的元素对象流 的 Optional
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Optional<Stream<U>> llOptS2(final String key, final Function<T, U> mapper) {
		return Objects.nonNull(this.get(key)) //
				? Optional.ofNullable(this.llS(key).map(e -> mapper.apply((T) e)))
				: Optional.empty();
	}

	/**
	 * 把 键名索引 元素的值转换为 元素对象流
	 *
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param idx    键名索引 从0开始
	 * @param mapper 键值变换函数 t->u
	 * @return U类型的元素对象流 的 Optional
	 */
	default <T, U> Optional<Stream<U>> llOptS2(final int idx, final Function<T, U> mapper) {
		return this.llOptS2(this.keyOf(idx), mapper);
	}

	/**
	 * 更新式添加<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 *
	 * @param key   新的 键名
	 * @param value 新的 键值
	 * @return 添加了新元素后的对象,可能为this(MyRecord),也可能为新生成的对象(ArrayRecord)等。
	 */
	default IRecord add(final String key, final Object value) {
		final IRecord r = this.set(key, value);
		if (r == this) {
			return this;
		} else {
			// System.err.println(String.format("IRecord.set(key,value)生成了新的对象:%s",
			// this.getClass()));
			return r;
		} // if
	}

	/**
	 * 更新式添加,即改变自身的内容<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 *
	 * @param tup 待添加的键值对
	 * @return 对象本身(根据具体实现的不同,也可能是新生成的数据对象)
	 */
	default IRecord add(final Tuple2<String, ?> tup) {
		return this.add(tup._1, tup._2);
	}

	/**
	 * 更新式添加,即改变自身的内容<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 *
	 * @param tuples 待添加的键值对序列
	 * @return 对象本身(根据具体实现的不同,也可能是新生成的数据对象)
	 */
	default IRecord add(final Iterable<Tuple2<String, Object>> tuples) {
		return StreamSupport.stream(tuples.spliterator(), false).reduce(this, IRecord::add, IRecord::add);
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
	default IRecord add(final Object... kvs) {
		if (kvs.length == 1) {
			final Object obj = kvs[0];
			if (obj instanceof IRecord) { // 记录类型
				// 可迭代类型
				((IRecord) obj).forEach((k, v) -> this.add(k, v));
			} else if (obj instanceof Map) { // Map类型
				((Map<?, ?>) obj).forEach((k, v) -> this.add(k + "", v));
			} else if (obj instanceof Iterable) { // 可迭代类型
				int i = 0; //
				for (final Object x : (Iterable<?>) obj) {
					if (i > 10000) {
						break;
					} else {
						this.add(String.valueOf(i++), x);
					} // if
				} // for
			} else {
				// do nothing
			}
		} else {
			IRecord.slidingS(kvs, 2, 2, true).forEach(wnd -> { // 窗口遍历
				this.add(wnd.get(0) + "", wnd.get(1));
			});
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
	default IRecord prepend(final Object... kvs) {
		return REC(kvs).add(this);
	}

	/**
	 * 对键 key 应用函数 mapper
	 *
	 * @param <T>    键值类型
	 * @param <U>    结果类型
	 * @param key    键名
	 * @param mapper 键值变换函数 t->u
	 * @return 对键 key 应用函数 mapper
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U invoke(final String key, Function<T, U> mapper) {
		return mapper.apply((T) this.get(key));
	}

	/**
	 * 对键索引 idx 应用函数 mapper
	 *
	 * @param <T>    键值类型
	 * @param <U>    结果类型
	 * @param idx    键名
	 * @param mapper 键值变换函数 t->u
	 * @return 对键 key 应用函数 mapper
	 */
	default <T, U> U invoke(final int idx, Function<T, U> mapper) {
		return this.invoke(this.keyOf(idx), mapper);
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation on non-null
	 * object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value {@code x},
	 * {@code x.equals(x)} should return {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values {@code x} and
	 * {@code y}, {@code x.equals(y)} should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values {@code x},
	 * {@code y}, and {@code z}, if {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then {@code x.equals(z)} should
	 * return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values {@code x} and
	 * {@code y}, multiple invocations of {@code x.equals(y)} consistently return
	 * {@code true} or consistently return {@code false}, provided no information
	 * used in {@code equals} comparisons on the objects is modified.
	 * <li>For any non-null reference value {@code x}, {@code x.equals(null)} should
	 * return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements the most
	 * discriminating possible equivalence relation on objects; that is, for any
	 * non-null reference values {@code x} and {@code y}, this method returns
	 * {@code true} if and only if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode} method
	 * whenever this method is overridden, so as to maintain the general contract
	 * for the {@code hashCode} method, which states that equal objects must have
	 * equal hash codes.
	 *
	 * @param rec the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj argument;
	 *         {@code false} otherwise. see hashCode()
	 * @see java.util.HashMap
	 */
	default boolean equals(final IRecord rec) {
		return this == rec || this.compareTo(rec) == 0;
	}

	/**
	 * 元素个数
	 *
	 * @return 元素个数
	 */
	default int size() {
		return this.toMap().size();
	}

	/**
	 * IRececord 之间的比较大小,比较的keys选择当前对象的keys,当 null 小于任何值,即null值会排在牵头
	 */
	@Override
	default int compareTo(final IRecord o) {

		if (o == null) {
			return 1;
		} else if (this.keys().equals(o.keys())) {
			return IRecord.cmp(this.keys()).compare(this, o);
		} else {
			final Set<String> hashSet = new LinkedHashSet<String>(this.keys());
			hashSet.addAll(o.keys());// 归并
			final String[] kk = hashSet.stream().sorted().toArray(String[]::new);
			System.err.println(
					"比较的两个键名序列(" + this.keys() + "," + o.keys() + ")不相同,采用归并键名序列进行比较:" + Arrays.deepToString(kk));
			return IRecord.cmp(kk, true).compare(this, o);
		} // if
	}

	/**
	 * 肯定过滤
	 *
	 * @param predicate 谓词判断函数 tuple2->boolean
	 * @return 新生的IRecord
	 */
	default IRecord filter(Predicate<Tuple2<String, Object>> predicate) {
		return this.tupleS().filter(predicate).collect(IRecord.recclc(e -> e));
	}

	/**
	 * 肯定过滤
	 *
	 * @param idices 保留的键名索引序列，键名索引从0开始
	 * @return 新生的IRecord
	 */
	default IRecord filter(final Integer... idices) {
		final List<String> kk = this.keys();
		final int n = kk.size();
		final IRecord rec = this.build();

		Arrays.stream(idices).filter(i -> i >= 0 && i < n).map(kk::get).forEach(key -> rec.add(key, this.get(key)));

		return rec;
	}

	/**
	 * 肯定过滤
	 *
	 * @param keys 保留的键名序列，键名之间采用半角逗号分隔
	 * @return 新生的IRecord
	 */
	default IRecord filter(final String keys) {
		return this.filter(keys.split("[,]+"));
	}

	/**
	 * 肯定过滤
	 *
	 * @param keys 保留的键名序列
	 * @return 新生的IRecord
	 */
	default IRecord filter(final String[] keys) {
		return this.filter(Arrays.asList(keys));
	}

	/**
	 * 肯定过滤
	 *
	 * @param keys 保留的键名序列
	 * @return 新生的IRecord
	 */
	default IRecord filter(final List<String> keys) {
		return keys.stream().reduce(this.build(), (acc, key) -> {
			final String _key = key.trim();
			final Object value = this.get(_key);
			return value != null ? acc.add(key, value) : acc;
		}, IRecord::add);
	}

	/**
	 * 否定过滤
	 *
	 * @param idices 剔除的键名索引序列，键名索引从0开始
	 * @return 新生的IRecord
	 */
	default IRecord filterNot(final Integer... idices) {
		final int n = this.size();
		final List<Integer> ids = Arrays.asList(idices);
		final Integer[] _indices = Stream.iterate(0, i -> i + 1).limit(n).filter(i -> !ids.contains(i))
				.toArray(Integer[]::new);

		return this.filter(_indices);
	}

	/**
	 * 否定过滤
	 *
	 * @param keys 剔除的键名序列，键名之间采用半角逗号分隔
	 * @return 新生的IRecord
	 */
	default IRecord filterNot(final String keys) {
		return this.filterNot(keys.split("[,]+"));
	}

	/**
	 * 否定过滤
	 *
	 * @param keys 剔除的键名序列
	 * @return 新生的IRecord
	 */
	default IRecord filterNot(final String[] keys) {
		return this.filterNot(Arrays.asList(keys));
	}

	/**
	 * 否定过滤
	 *
	 * @param keys 剔除的键名序列
	 * @return 新生的IRecord
	 */
	default IRecord filterNot(final List<String> keys) {
		final BiPredicate<String, Object> bipredicate = (key, v) -> !keys.contains(key);
		return this.filter(bipredicate);
	}

	/**
	 * 转换成 tuple2 的 列表结构
	 *
	 * @return tuple的 列表结构 [(k,v)]
	 */
	default List<Tuple2<String, Object>> tuples() {
		return this.tupleS().collect(Collectors.toList());
	}

	/**
	 * 转换成 tuple2 的 流式结构
	 *
	 * @return tuple的流结构 [(k,v)]
	 */
	default Stream<Tuple2<String, Object>> tupleS() {
		return this.toMap().entrySet().stream().map(e -> Tuple2.of(e.getKey(), e.getValue()));
	}

	/**
	 * 键名过滤
	 *
	 * @param bipredicate 过滤函数 (key,value)->bool, true 值保留, false 剔除
	 * @return 新生的IRecord
	 */
	default IRecord filter(final BiPredicate<String, Object> bipredicate) {
		final IRecord rec = this.build(); // 空IRecord
		this.toMap().entrySet().stream().filter(tup -> bipredicate.test(tup.getKey(), tup.getValue()))
				.forEach(e -> rec.add(e.getKey(), e.getValue()));
		return rec;
	}

	/**
	 * 默认构造器
	 *
	 * @return 默认记录构造器
	 */
	default IRecord.Builder rb() {
		return IRecord.rb(this.keys());
	}

	/**
	 * 把IRecord的值系列即values转换成一维的Object对象数组
	 *
	 * @return Object 一维数组
	 */
	default Object[] toArray() {
		return this.toArray(e -> e);
	}

	/**
	 * 把IRecord的值系列即values转换成数组结构
	 *
	 * @param <X>    mapper 参数类型
	 * @param <T>    mapper 值类型
	 * @param mapper 值的变换函数 x->t
	 * @return T类型的一维数组
	 */
	@SuppressWarnings("unchecked")
	default <X, T> T[] toArray(final Function<X, T> mapper) {
		final List<Class<?>> classes = this.tupleS().map(x -> mapper.apply((X) x._2)).filter(Objects::nonNull)
				.map(e -> (Class<Object>) e.getClass()).distinct().collect(Collectors.toList());
		final Class<?> clazz = classes.size() > 1 // 类型不唯一
				? classes.stream().allMatch(Number.class::isAssignableFrom) // 数字类型
						? Number.class // 数字类型
						: Object.class // 节点类型
				: classes.get(0); // 类型唯一
		return this.tupleS().map(x -> mapper.apply((X) x._2)).toArray(n -> (T[]) Array.newInstance(clazz, n));
	}

	/**
	 * 带有键值对儿间隔数组 [key1,value1,key2,value2]
	 *
	 * @param generator 数组生成器
	 * @param <T>       数组元素类型
	 * @return 数组
	 */
	default <T> T[] toArray2(final IntFunction<T[]> generator) {
		return this.tupleS().flatMap(e -> Arrays.stream(e.toArray(Object[]::new))).toArray(generator);
	}

	/**
	 * 带有键值对儿间隔数组 [key1,value1,key2,value2]
	 *
	 * @return 数组
	 */
	default Object[] toArray2() {
		return this.toArray2(Object[]::new);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * <p>
	 * 使用示例：<br>
	 * IRecord.rb("name,birth,marry") // 档案结构 <br>
	 * .get("zhangsan,19810713,20011023".split(",")) // 构建张三的数据记录 <br>
	 * .arrayOf("birth,marry",IRecord::asLocalDate, // 把 出生日期和结婚日期转换为日期类型 <br>
	 * ldts->ldts[0].until(ldts[1]).getYears()); // 计算张三的结婚年龄 <br>
	 *
	 * @param <T>    数组的元素类型
	 * @param <U>    mapper 目标元素的类型
	 * @param mapper [t]->u 数组变换函数
	 * @return U类型结果
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U arrayOf(final Function<T[], U> mapper) {
		final Object[] oo = this.toArray();
		U u = null;
		try {
			u = mapper.apply((T[]) oo);
		} catch (Exception e) {
			// do nothing
		} // try

		return u;
	}

	/**
	 * 非更新式添加,即在一个复制品上添加新的键值数据<br>
	 * 添加键值对数据
	 *
	 * @param rec 键值对集合
	 * @return 添加了新的减值数据的复制品
	 */
	default IRecord derive(final IRecord rec) {
		return this.duplicate().add(rec);
	}

	/**
	 * 非更新式添加,即在一个复制品上添加新的键值数据<br>
	 * 添加键值对数据
	 *
	 * @param tups 键值对集合
	 * @return 添加了新的减值数据的复制品
	 */
	default IRecord derive(final Object... tups) {
		final IRecord rec = this.duplicate(); // 复制品
		IRecord.slidingS(Arrays.asList(tups), 2, 2, true).forEach(aa -> rec.add(aa.get(0).toString(), aa.get(1))); // forEach
		return rec;
	}

	/**
	 * 重新计算(不管是否存在)
	 *
	 * @param <T>    值类型1
	 * @param <V>    值类型2
	 * @param key    健名
	 * @param mapper 健名映射 t->v
	 * @return T 类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <T, V> V compute(final String key, final Function<T, V> mapper) {
		final T t = (T) this.get(key);
		final V v = mapper.apply(t);
		this.set(key, v);
		return v;
	}

	/**
	 * 重新计算(不管是否存在)
	 *
	 * @param <T>    值类型1
	 * @param <V>    值类型2
	 * @param index  健名
	 * @param mapper 健名映射 t->v
	 * @return T 类型的结果
	 */
	default <T, V> V compute(final int index, final Function<T, V> mapper) {
		return this.compute(this.keyOf(index), mapper);
	}

	/**
	 * 重新计算(不管是否存在)
	 *
	 * @param <T>    值类型1
	 * @param <V>    值类型2
	 * @param key    健名
	 * @param mapper 健名映射 (s,t)->v
	 * @return T 类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <T, V> V compute(final String key, final BiFunction<String, T, V> mapper) {
		final T t = (T) this.get(key);
		final V v = mapper.apply(key, t);
		this.set(key, v);
		return v;
	}

	/**
	 * 重新计算
	 *
	 * @param <T>    值类型1
	 * @param <V>    值类型2
	 * @param index  健名索引
	 * @param mapper 健名映射 (s,t)->v
	 * @return T 类型的结果
	 */
	default <T, V> V compute(final int index, final BiFunction<String, T, V> mapper) {
		return this.compute(index, mapper);
	}

	/**
	 * 不存在则计算
	 *
	 * @param <T>    值类型
	 * @param key    健名
	 * @param mapper 健名映射 k->t
	 * @return T 类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <T> T computeIfAbsent(final String key, final Function<String, T> mapper) {
		return this.opt(key).map(e -> (T) e).orElseGet(() -> {
			final T value = mapper.apply(key);
			this.add(key, value);
			return value;
		});
	}

	/**
	 * 不存在则计算
	 *
	 * @param <T>    值类型
	 * @param idx    键名索引，从0开始
	 * @param mapper 健名映射 k->t
	 * @return T 类型的结果
	 */
	default <T> T computeIfAbsent(final Integer idx, final Function<String, T> mapper) {
		return this.computeIfAbsent(this.keyOf(idx), mapper);
	}

	/**
	 * 不存在则计算
	 *
	 * @param <T>    mapper 结果类型
	 * @param key    健名
	 * @param mapper 健名映射 k->t
	 * @return T 类型的结果
	 */
	default <T> T computeIfPresent(final String key, final Function<String, T> mapper) {
		return this.opt(key).map(v -> {
			final T t = mapper.apply(key);
			this.add(key, t);
			return t;
		}).orElse(null);
	}

	/**
	 * 不存在则计算
	 *
	 * @param <T>    mapper 结果类型
	 * @param idx    键名索引，从0开始
	 * @param mapper 健名映射 k->t
	 * @return T 类型的结果
	 */
	default <T> T computeIfPresent(final Integer idx, final Function<String, T> mapper) {
		return this.computeIfPresent(this.keyOf(idx), mapper);
	}

	/**
	 * Haskell Functor （健名版）
	 *
	 * @param mapper (key,value)->new_value
	 * @return 变换后者IRecord
	 */
	default IRecord fmap(final BiFunction<String, Object, Object> mapper) {
		return this.tupleS().map(e -> Tuple2.of(e._1, mapper.apply(e._1, e._2))).collect(IRecord.recclc());
	}

	/**
	 * Haskell Functor （索引序号版）
	 *
	 * @param mapper (idx,value)->new_value
	 * @return 变换后者IRecord
	 */
	default IRecord fmap2(final BiFunction<Integer, Object, Object> mapper) {
		final AtomicInteger ai = new AtomicInteger(0); // 遍历索引
		return this.tupleS().map(e -> Tuple2.of(e._1, mapper.apply(ai.getAndIncrement(), e._2)))
				.collect(IRecord.recclc());
	}

	/**
	 * Haskell Functor
	 *
	 * @param mapper value->new_value
	 * @return 变换后者IRecord
	 */
	default IRecord fmap(final Function<Object, Object> mapper) {
		return this.tupleS().map(e -> Tuple2.of(e._1, mapper.apply(e._2))).collect(IRecord.recclc());
	}

	/**
	 * 子集别名:生成一个 同结构的对象,健名使用新值 <br>
	 * 例如：对象 rec={a:1,b:2,c:3,d:4} <br>
	 * rec.alias("a,A,c,C"); 返回 {A:1,C:3} <br>
	 * rec.alias("a,A,c,C,e,E"); 返回 {A:1,C:3}
	 *
	 * @param <T>        占位符类型：name1类型，
	 * @param <U>        占位符类型：alias_name1 类型
	 * @param name_pairs [(name1,alias_name1),(name2,alias_name2)]
	 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
	 */
	default <T, U> IRecord alias(final Iterable<Tuple2<T, U>> name_pairs) {
		return StreamSupport.stream(name_pairs.spliterator(), false) //
				.map(e -> Optional.ofNullable(this.get(e._1 + "")) //
						.map(p -> Tuple2.of(e._2 + "", p)).orElse(null))
				.filter(Objects::nonNull).collect(IRecord.recclc());
	}

	/**
	 * 子集别名:生成一个 同结构的对象,健名使用新值 <br>
	 * 例如：对象 rec={a:1,b:2,c:3,d:4} <br>
	 * rec.alias("a,A,c,C"); 返回 {A:1,C:3}
	 * 
	 * @param kvps 健名映射序列, name1,alias_name1,name2,alias_name2 ...
	 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
	 */
	default IRecord alias(final Object... kvps) {
		return this.alias(REC(kvps));
	}

	/**
	 * 子集别名:生成一个 同结构的对象,健名使用新值 <br>
	 * 例如：对象 rec={a:1,b:2,c:3,d:4} <br>
	 * rec.alias("a,A,c,C"); 返回 {A:1,C:3} <br>
	 *
	 * @param kvps 健名映射序列, name1,alias_name1,name2,alias_name2 ... , 键名间采用英文 逗号',',
	 *             分号';'分隔
	 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
	 */
	default IRecord alias(final String kvps) {
		return this.alias((Object[]) kvps.split("[,;]+"));
	}

	/**
	 * 全集别名-kvp键名顺序:保持kvps记录键名顺序的全序列键名的别名构建 <br>
	 * 例如：对象 rec={a:1,b:2,c:3,d:4} <br>
	 * rec.alias("a,A,c,C"); 返回 {A:1,C:3,b:2,d:4} <br>
	 * rec.alias("a,A,c,C,e,E"); 返回 {A:1,b:2,C:3,d:4} <br>
	 *
	 * @param kvps 健名映射序列, name1,alias_name1,name2,alias_name2 ... , 键名间采用英文 逗号',',
	 *             分号';'分隔
	 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
	 */
	default IRecord alias2(final String kvps) {
		final var clone = this.duplicate(); // 克隆体
		final var rec = REC(); // 返回值新纪录
		final var ps = kvps.split("[,;]+");

		for (int i = 0; i < ps.length; i += 2) {
			final var k = ps[i];
			final var _k = ps[i + 1];
			if (clone.has(k)) { //
				rec.add(_k, clone.get(k)); // 在新记录中增加改名后的变量值
				clone.remove(k); // 从克隆体 剔除原来的k
			}
		}

		return rec.add(clone);
	}

	/**
	 * 全集别名-源键名顺序:保持源记录键名顺序的全序列键名的别名构建 <br>
	 * 例如：对象 rec={a:1,b:2,c:3,d:4} <br>
	 * rec.alias("a,A,c,C"); 返回 {A:1,b:2,C:3,d:4} <br>
	 * rec.alias("a,A,c,C,e,E"); 返回 {A:1,b:2,C:3,d:4} <br>
	 *
	 * @param kvps 健名映射序列, name1,alias_name1,name2,alias_name2 ... , 键名间采用英文 逗号',',
	 *             分号';'分隔
	 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
	 */
	default IRecord alias3(final String kvps) {
		final var ks = new LinkedHashMap<String, String>();
		final var ps = kvps.split("[,;]+");
		this.keys().forEach(k -> ks.put(k, k));
		for (int i = 0; i < ps.length; i += 2) {
			final var k = ps[i]; // 旧名称
			final var _k = ps[i + 1]; // 新名称
			if (this.has(k)) {
				ks.put(k, _k);
			}
		}
		final var _ps = ks.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray();
		return this.alias(_ps);
	}

	/**
	 * IRecord 变换
	 * 
	 * @param <T>    结果类型
	 * @param mapper rec->t , 若 mapper 为 null 则返回 this
	 * @return T 类型结果
	 */
	@SuppressWarnings("unchecked")
	default <T> T mutate(final Function<IRecord, T> mapper) {
		return mapper == null ? (T) this : mapper.apply(this);
	}

	/**
	 * 是否 包含收键名 k
	 * 
	 * @param k 键名
	 * @return 是否 包含收键名 k
	 */
	default boolean has(final String k) {
		return this.keys().contains(k);
	}

	/**
	 * 是否包含键名索引 idx
	 * 
	 * @param idx 键名索引从0开始
	 * @return 是否包含键名索引 idx
	 */
	default boolean has(final int idx) {
		return this.keys().size() > idx;
	}

	/**
	 * IRecord 变换
	 * 
	 * @param <T>    结果类型
	 * @param mapper [(k,v)]->t , 若 mapper 为 null 则返回 this
	 * @return T 类型结果
	 */
	@SuppressWarnings("unchecked")
	default <T> T mutate2(final Function<? super Map<?, ?>, T> mapper) {
		return mapper == null ? (T) this : mapper.apply(this.toMap());
	}

	/**
	 * Record 搜集器
	 * 
	 * @param <A>       累加器的元素 类型 中间结果类型,用于暂时存放 累加元素的中间结果的集合。
	 * @param <R>       返回结果类型
	 * @param collector 搜集 KVPair&lt;String,Object&gt;类型的 搜集器
	 * @return 规约的结果 R
	 */
	default <A, R> R collect(final Collector<Tuple2<String, Object>, A, R> collector) {
		return this.tupleS().collect(collector);
	}

	/**
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param accumulator (result,e)-&gt;result
	 * @param recs        归集集合
	 * @return 规约结果
	 */
	default IRecord reduce(final BinaryOperator<IRecord> accumulator, final IRecord... recs) {
		final BinaryOperator<IRecord> acc = (result, e) -> result; // 返回第一个元素(result)保持运算结果不变
		return Optional.ofNullable(recs)
				.map(rs -> Stream.of(rs).reduce(this, Optional.ofNullable(accumulator).orElse(acc))).orElse(this);
	}

	/**
	 * 加法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param nums 归集集合
	 * @return 规约结果
	 */
	default <T> IRecord max(final Number... nums) {
		return this.max(null, nums);
	}

	/**
	 * 加法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param recs 归集集合
	 * @return 规约结果
	 */
	default <T> IRecord max(final IRecord... recs) {
		return this.max(null, recs);
	}

	/**
	 * 提取最大的值<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param quantizer 量化器: t-&gt;num
	 * @param nums      数值集合
	 * @return 规约结果
	 */
	default <T> IRecord max(final Function<T, Number> quantizer, final Number... nums) {
		return this.max(quantizer, Stream.of(nums).map(this.rb()::get).toArray(IRecord[]::new));
	}

	/**
	 * 加法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param quantizer 量化器: t-&gt;num
	 * @param recs      归集集合
	 * @return 规约结果
	 */
	default <T> IRecord max(final Function<T, Number> quantizer, final IRecord... recs) {
		return this.reduce(IRecord.max(quantizer), recs);
	}

	/**
	 * 提取最小的值<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param nums 数值集合
	 * @return 规约结果
	 */
	default <T> IRecord min(final Number... nums) {
		return this.min(null, nums);
	}

	/**
	 * 提取最小的值<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param nums 数值集合
	 * @return 规约结果
	 */
	default <T> IRecord min(final IRecord... recs) {
		return this.min(null, recs);
	}

	/**
	 * 提取最小的值<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param quantizer 量化器: t-&gt;num
	 * @param nums      数值集合
	 * @return 规约结果
	 */
	default <T> IRecord min(final Function<T, Number> quantizer, final Number... nums) {
		return this.min(quantizer, Stream.of(nums).map(this.rb()::get).toArray(IRecord[]::new));
	}

	/**
	 * 提取最小的值<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param<T> 元素类型
	 * @param quantizer 量化器: t-&gt;num
	 * @param recs      归集集合
	 * @return 规约结果
	 */
	default <T> IRecord min(final Function<T, Number> quantizer, final IRecord... recs) {
		return this.reduce(IRecord.min(quantizer), recs);
	}

	/**
	 * 加法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param nums 加数集合
	 * @return 规约结果
	 */
	default IRecord plus(final Number... nums) {
		return this.plus(Stream.of(nums).map(this.rb()::get).toArray(IRecord[]::new));
	}

	/**
	 * 加法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param accumulator (result,e)-&gt;result
	 * @param recs        归集集合
	 * @return 规约结果
	 */
	default IRecord plus(final IRecord... recs) {
		return this.reduce(IRecord.plus(), recs);
	}

	/**
	 * 减法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param nums 减数集合
	 * @return 规约结果
	 */
	default IRecord subtract(final Number... nums) {
		return this.subtract(Stream.of(nums).map(this.rb()::get).toArray(IRecord[]::new));
	}

	/**
	 * 减法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param recs 归集集合
	 * @return 规约结果
	 */
	default IRecord subtract(final IRecord... recs) {
		return this.reduce(IRecord.subtract(), recs);
	}

	/**
	 * 乘法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param nums 乘数集合
	 * @return 规约结果
	 */
	default IRecord multiply(final Number... nums) {
		return this.multiply(Stream.of(nums).map(this.rb()::get).toArray(IRecord[]::new));
	}

	/**
	 * 乘法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param recs 归集集合
	 * @return 规约结果
	 */
	default IRecord multiply(final IRecord... recs) {
		return this.reduce(IRecord.multiply(), recs);
	}

	/**
	 * 除法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param nums 除数集合
	 * @return 规约结果
	 */
	default IRecord divide(final Number... nums) {
		return this.divide(Stream.of(nums).map(this.rb()::get).toArray(IRecord[]::new));
	}

	/**
	 * 除法<br>
	 * 使用this作为初始规约值result进行iterate的迭代规约
	 * 
	 * @param recs 归集集合
	 * @return 规约结果
	 */
	default IRecord divide(final IRecord... recs) {
		return this.reduce(IRecord.divide(), recs);
	}

	/**
	 * 生成 构建器
	 *
	 * @param n     键数量,正整数
	 * @param keyer 键名生成器 (i:从0开始)->key
	 * @return IRecord 构造器
	 */
	static Builder rb(final int n, Function<Integer, ?> keyer) {

		final List<?> keys = Stream.iterate(0, i -> i + 1).limit(n).map(keyer).collect(Collectors.toList());
		return new Builder(keys);
	}

	/**
	 * 生成 构建器
	 *
	 * @param <T>  元素类型
	 * @param keys 键名序列
	 * @return keys 为格式的 构建器
	 */
	@SafeVarargs
	static <T> Builder rb(final T... keys) {

		final List<String> _keys = Arrays.asList(keys).stream().map(e -> e + "").collect(Collectors.toList());
		return new Builder(_keys);
	}

	/**
	 * 生成 构建器
	 *
	 * @param keys 键名序列, 用半角逗号 “,” 分隔
	 * @return keys 为格式的 构建器
	 */
	static Builder rb(final String keys) {

		return rb(keys.split(","));
	}

	/**
	 * 生成 构建器
	 *
	 * @param keys 键名序列
	 * @return keys 为格式的 构建器
	 */
	static Builder rb(final String[] keys) {

		return rb(Arrays.asList(keys));
	}

	/**
	 * 生成 构建器
	 *
	 * @param <T>  keys 元素类型
	 * @param keys 键名序列
	 * @return keys 为格式的 构建器
	 */
	static <T> Builder rb(final Iterable<T> keys) {

		return new Builder(keys);
	}

	/**
	 * 括号版的替换,括号与数字键不能出现空格, 即 ${ 0 } 是非法的, 只有 ${0} 才是合法的 <br>
	 * fill_template 的别名<br>
	 * IRecord.FT2("${0}+${1}=${2}",1,2,3) 替换后的结果为 1+2=3 <br>
	 * 健名使用${} 括起来避免对于模式串里的 "$11"出现混淆 是 $1后跟1 还是 就是$11的情况 <br>
	 *
	 * @param <T>      参数列表元素类型
	 * @param template 模版字符串，占位符${0},${1},${2},...
	 * @param tt       模版参数序列
	 * @return template 被模版参数替换后的字符串
	 */
	public static <T> String FT2(final String template, final Object... tt) {

		final IRecord rec = IRecord.rb(tt.length, i -> "${" + i + "}").get(tt);
		return fill_template(template, rec);
	}

	/**
	 * fill_template 的别名<br>
	 * Term.FT("$0+$1=$2",1,2,3) 替换后的结果为 1+2=3
	 *
	 * @param <T>      参数列表元素类型
	 * @param template 模版字符串，占位符$0,$1,$2,...
	 * @param tt       模版参数序列
	 * @return template 被模版参数替换后的字符串
	 */
	@SafeVarargs
	static <T> String FT(final String template, final T... tt) {
		final Matcher matcher = Pattern.compile("(\\$\\d+)").matcher(template);
		int n = 0; // 占位符数量
		while (matcher.find()) {
			n++;
		}
		Object[] oo = tt; // 参数值
		if (n > 1 && tt.length == 1) { // 单一值的情况
			if (tt[0] instanceof Collection) {
				oo = ((Collection<?>) tt[0]).stream().toArray();
			} else if (tt[0] instanceof Stream) {
				oo = ((Stream<?>) tt[0]).toArray();
			} else {
				// do nothing
			} // if
		} // if
		final IRecord rec = IRecord.rb(oo.length, i -> "$" + i).get(oo);
		return fill_template(template, rec);
	}

	/**
	 * fill_template 的别名<br>
	 *
	 * @param line     模板字符串
	 * @param pattern  字段模式
	 * @param mappings 键值映射
	 * @return 填充后的字符串
	 */
	static String FT(final String line, final String pattern, IRecord mappings) {
		return FT(line, pattern, matcher -> mappings.strOpt(matcher.group()).orElse(matcher.group()));
	}

	/**
	 * fill_template 的别名<br>
	 *
	 * @param line     模板字符串
	 * @param pattern  字段模式
	 * @param replacer 变换函数 The function to be applied to the match result of this
	 *                 matcher that returns a replacement string.
	 * @return 填充后的字符串
	 */
	static String FT(final String line, final String pattern, final Function<MatchResult, String> replacer) {
		return IRecord.FT(line, Pattern.compile(pattern), replacer);
	}

	/**
	 * fill_template 的别名<br>
	 *
	 * @param line     模板字符串
	 * @param pattern  字段模式
	 * @param replacer 变换函数 The function to be applied to the match result of this
	 *                 matcher that returns a replacement string.
	 * @return 填充后的字符串
	 */
	static String FT(final String line, final Pattern pattern, final Function<MatchResult, String> replacer) {

		final StringBuilder sb = new StringBuilder();
		try {
			final Matcher matcher = pattern.matcher(line);
			final List<MatchResult> results = new ArrayList<>();
			while (matcher.find()) {
				results.add(matcher.toMatchResult());
			}

			int i = 0;
			for (final MatchResult result : results) {
				while (i < result.start()) {
					sb.append(line.charAt(i++));
				}
				final String e = replacer.apply(result);
				sb.append(e);
				i = result.end();
			} //

			while (i < line.length()) {
				sb.append(line.charAt(i++));
			} //
		} catch (Exception e) {
			// e.printStackTrace();
			sb.append(line);
		}

		return sb.toString();
	}

	/**
	 * fill_template 的别名<br>
	 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
	 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来。<br>
	 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
	 * 例如 : FT("insert into tblname$ (name,sex) values
	 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
	 * 返回: insert into user (name,sex) values ('张三','男') <br>
	 *
	 * @param template           模版字符串
	 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
	 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
	 */
	static String FT(final String template, final IRecord placeholder2values) {
		return fill_template(template, placeholder2values);
	}

	/**
	 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
	 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来,<br>
	 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
	 * 例如 : fill_template("insert into tblname$ (name,sex) values
	 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
	 * 返回: insert into user (name,sex) values ('张三','男') <br>
	 *
	 * @param template           模版字符串
	 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
	 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
	 */
	static String fill_template(final String template, final IRecord placeholder2values) {

		if (placeholder2values == null) { // 空值判断
			return template;
		}

		final int len = template.length(); // 模版的长度
		final StringBuilder buffer = new StringBuilder();// 工作缓存
		final List<String> keys = placeholder2values.keys().stream().sorted((a, b) -> -(a.length() - b.length())) // 按照从长到短的顺序进行排序。以保证keys的匹配算法采用的是贪婪算法。
				.collect(Collectors.toList());// 键名 也就是template中的占位符号。
		final Map<Object, List<String>> firstCharMap = keys.stream()
				.collect(Collectors.groupingBy(key -> key.charAt(0)));// keys的首字符集合，这是为了加快
		// 读取的步进速度
		int i = 0;// 当前读取的模版字符位置,从0开始。

		while (i < len) {// 从前向后[0,len)的依次读取模板字符串的各个字符
			// 注意:substring(0,0) 或是 substring(x,x) 返回的是一个长度为0的字符串 "",
			// 特别是,当 x大于等于字符串length会抛异常:StringIndexOutOfBoundsException
			final String line = template.substring(0, i);// 业已读过的模版字符串内容[0,i),当前所在位置为i等于line.length
			String placeholder = null;// 占位符号 的内容
			final List<String> kk = firstCharMap.get(template.charAt(i));// 以 i为首字符开头的keys
			if (kk != null) {// 使用firstCharMap加速步进速度读取模版字符串
				for (final String key : kk) {// 寻找 可以被替换的key
					final int endIndex = line.length() + key.length(); // 终点索引 exclusive
					final boolean b = endIndex > len // 是否匹配到placeholder
							? false // 拼装的组合串(line+key) 超长（超过模板串的长度)
							: template.substring(line.length(), endIndex).equals(key); // 当前位置之后刚好存在一个key模样的字符串
					if (b) { // 定位到一个完整的占位符：长度合适（endIndex<=len) && 内容匹配 equals
						placeholder = key; // 提取key作为占位符
						break; // 确定了占位符 ,跳出本次i位置的kk循环。
					} // if b 定位到一个完整的占位符
				} // for key:kk
			} // if kk!=null

			if (placeholder != null) {// 已经发现了占位符号，用rec中的值给予替换
				boolean isnumberic = false;// 默认不是数字格式
				final Object value = placeholder2values.get(placeholder); // 提取占位符内容
				if (placeholder.startsWith("$") || placeholder.endsWith("$"))
					isnumberic = true; // value 是否是 强制 使用 数字格式，即用$作为前/后缀。
				if (value instanceof Number)
					isnumberic = true; // 如果值是数字类型就一定会不加引号的。即采用数字格式

				buffer.append((isnumberic || value == null) ? value + "" : "'" + value + "'"); // 为字符串添加引号，数字格式或是null不加引号
				i += placeholder.length();// 步进placeholder的长度，跳着前进
			} else {// 发现了占位符号则附加当前的位置的字符
				buffer.append(template.charAt(i));
				i++;// 后移一步
			} // if placeholder
		} // while

		return buffer.toString(); // 返回替换后的结果值
	}

	/**
	 * 构建一个键名键值序列 指定的 IRecord
	 *
	 * @param <T> Map结构的参数列表元素类型
	 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return 新生成的IRecord
	 */
	@SafeVarargs
	static <T> IRecord REC(final T... kvs) {
		return MyRecord.REC(kvs); // 采用 MyRecord 来作为IRecord的默认实现函数
	}

	/**
	 * 把一个数据对象转换为整数<br>
	 * 对于 非法的数字类型 返回 null
	 *
	 * @param <T> 函数的参数类型
	 * @return t->dbl
	 */
	static <T> Function<T, Integer> obj2int() {
		return t -> {
			final Double d = IRecord.obj2dbl(null).apply(t);
			return d == null ? null : d.intValue();
		};
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 null
	 *
	 * @param <T> 函数的参数类型
	 * @return t->dbl
	 */
	static <T> Function<T, Double> obj2dbl() {
		return IRecord.obj2dbl(null);
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 defaultValue <br>
	 * <p>
	 * 默认会尝试把时间类型也解释为数字,即 '1970-01-01 08:00:01' <br>
	 * 也会被转换成一个 0时区 的 从1970年1月1 即 epoch time 以来的毫秒数<br>
	 * 对于 中国 而言 位于+8时区, '1970-01-01 08:00:01' 会被解析为1000
	 *
	 * @param <T>          函数的参数类型
	 * @param defaultValue 非法的数字类型 返回 的默认值
	 * @return t->dbl
	 */
	static <T> Function<T, Double> obj2dbl(final Number defaultValue) {
		return obj2dbl(defaultValue, true);
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 defaultValue
	 * <p>
	 * 默认会尝试把时间类型也解释为数字,即 '1970-01-01 08:00:01' <br>
	 * 也会被转换成一个 0时区 的 从1970年1月1 即 epoch time 以来的毫秒数<br>
	 * 对于 中国 而言 位于+8时区, '1970-01-01 08:00:01' 会被解析为1000
	 *
	 * @param <T>          函数的参数类型
	 * @param defaultValue 非法的数字类型 返回 的默认值
	 * @param timeflag     是否对时间类型数据进行转换, true 表示 开启,'1970-01-01 08:00:01'将会被解析为
	 *                     1000,false 不开启 时间类型将会返回defaultValue
	 * @return t->dbl
	 */
	static <T> Function<T, Double> obj2dbl(final Number defaultValue, final boolean timeflag) {
		return (T obj) -> {
			if (obj instanceof Number) {
				return ((Number) obj).doubleValue();
			}

			Double dbl = defaultValue == null ? null : defaultValue.doubleValue();
			try {
				dbl = Double.parseDouble(obj.toString());
			} catch (Exception e) { //
				if (timeflag) { // 开启了时间解析功能，则尝试 进行事件的数字化转换
					final LocalDateTime ldt = IRecord.REC("key", obj).ldt("key"); // 尝试把时间转换成数字
					if (ldt != null) {
						final ZoneId systemZone = ZoneId.systemDefault(); // 默认时区
						final ZoneOffset offset = systemZone.getRules().getOffset(ldt); // 时区 offset
						dbl = ((Number) ldt.toInstant(offset).toEpochMilli()).doubleValue(); // 转换成 epoch time 以来的毫秒数
					} // if
				} // if
			} // try

			return dbl;
		};
	}

	/**
	 * 把值对象转化成列表结构
	 *
	 * @param value 值对象
	 * @return 列表结构
	 */
	@SuppressWarnings("unchecked")
	static List<Object> asList(final Object value) {

		if (value instanceof List) {
			return ((List<Object>) value);
		} else if (value instanceof Collection) {
			return new ArrayList<>((Collection<?>) value);
		} else if (value instanceof Iterable) {
			return IRecord.itr2list((Iterable<Object>) value);
		} else if (value instanceof Stream) {
			return ((Stream<Object>) value).collect(Collectors.toList());
		} else if (Objects.nonNull(value) && value.getClass().isArray()) {
			return Arrays.asList((Object[]) value);
		} else {
			final List<Object> aa = new ArrayList<Object>();

			if (value != null) {
				aa.add(value);
			}

			return aa;
		} // if
	}

	/**
	 * 数据滑动<br>
	 * 例如 [1,2,3,4,5] 按照 width=2, step=1 进行滑动 <br>
	 * flag false: [1, 2][2, 3][3, 4][4, 5][5] 如果 flag = true 则 返回 <br>
	 * flag true: [1, 2][2, 3][3, 4][4, 5] <br>
	 * 按照 width=2, step=2 进行滑动 <br>
	 * flag false: [1, 2][3, 4] <br>
	 * flag true: [1, 2][3, 4] <br>
	 *
	 * @param <T>  数据元素类型
	 * @param aa   数据集合
	 * @param size 窗口大小
	 * @param step 步长
	 * @param flag 是否返回等长记录,true 数据等长 剔除 尾部的 不齐整（小于 size） 的元素,false 包含不齐整
	 * @return 滑动窗口列表
	 */
	static <T> Stream<List<T>> slidingS(final T[] aa, final int size, final int step, final boolean flag) {
		return IRecord.slidingS(Arrays.asList(aa), size, step, flag);
	}

	/**
	 * 数据滑动<br>
	 * 例如 [1,2,3,4,5] 按照 width=2, step=1 进行滑动 <br>
	 * flag false: [1, 2][2, 3][3, 4][4, 5][5] 如果 flag = true 则 返回 <br>
	 * flag true: [1, 2][2, 3][3, 4][4, 5] <br>
	 * 按照 width=2, step=2 进行滑动 <br>
	 * flag false: [1, 2][3, 4] <br>
	 * flag true: [1, 2][3, 4] <br>
	 *
	 * @param <T>        数据元素类型
	 * @param collection 数据集合
	 * @param size       窗口大小
	 * @param step       步长
	 * @param flag       是否返回等长记录,true 数据等长 剔除 尾部的 不齐整（小于 size） 的元素,false 包含不齐整
	 * @return 滑动窗口列表
	 */
	static <T> Stream<List<T>> slidingS(final Collection<T> collection, final int size, final int step,
			final boolean flag) {
		final int n = collection.size();
		final ArrayList<T> arrayList = collection instanceof ArrayList // 类型检测
				? (ArrayList<T>) collection // 数组列表类型
				: new ArrayList<T>(collection); // 其他类型

		// 当flag 为true 的时候 i的取值范围是: [0,n-size] <==> [0,n+1-size)
		return IRecord.iterate(0, i -> i < (flag ? n + 1 - size : n), i -> i + step) // 序列生成
				.map(i -> arrayList.subList(i, Math.min((i + size), n)));
	}

	/**
	 * Returns a sequential ordered {@code Stream} produced by iterative application
	 * of the given {@code next} function to an initial element, conditioned on
	 * satisfying the given {@code hasNext} predicate. The stream terminates as soon
	 * as the {@code hasNext} predicate returns false.
	 *
	 * <p>
	 * {@code Stream.iterate} should produce the same sequence of elements as
	 * produced by the corresponding for-loop:
	 *
	 * <pre>
	 * {@code
	 *     for (T index=seed; hasNext.test(index); index = next.apply(index)) {
	 *         ...
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>
	 * The resulting sequence may be empty if the {@code hasNext} predicate does not
	 * hold on the seed value. Otherwise the first element will be the supplied
	 * {@code seed} value, the next element (if present) will be the result of
	 * applying the {@code next} function to the {@code seed} value, and so on
	 * iteratively until the {@code hasNext} predicate indicates that the stream
	 * should terminate.
	 *
	 * <p>
	 * The action of applying the {@code hasNext} predicate to an element <a href=
	 * "../concurrent/package-summary.html#MemoryVisibility"><i>happens-before</i></a>
	 * the action of applying the {@code next} function to that element. The action
	 * of applying the {@code next} function for one element <i>happens-before</i>
	 * the action of applying the {@code hasNext} predicate for subsequent elements.
	 * For any given element an action may be performed in whatever thread the
	 * library chooses.
	 *
	 * @param <T>     the type of stream elements
	 * @param seed    the initial element
	 * @param hasNext a predicate to apply to elements to determine when the stream
	 *                must terminate.
	 * @param next    a function to be applied to the previous element to produce a
	 *                new element
	 * @return a new sequential {@code Stream}
	 */
	static <T> Stream<T> iterate(final T seed, final Predicate<? super T> hasNext, final UnaryOperator<T> next) {
		Objects.requireNonNull(next);
		Objects.requireNonNull(hasNext);
		final Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE,
				Spliterator.ORDERED | Spliterator.IMMUTABLE) {
			T prev;
			boolean started, finished;

			@Override
			public boolean tryAdvance(final Consumer<? super T> action) {
				Objects.requireNonNull(action);
				if (finished)
					return false;
				T t;
				if (started)
					t = next.apply(prev);
				else {
					t = seed;
					started = true;
				}
				if (!hasNext.test(t)) {
					prev = null;
					finished = true;
					return false;
				}
				action.accept(prev = t);
				return true;
			}

			@Override
			public void forEachRemaining(final Consumer<? super T> action) {
				Objects.requireNonNull(action);
				if (finished)
					return;
				finished = true;
				T t = started ? next.apply(prev) : seed;
				prev = null;
				while (hasNext.test(t)) {
					action.accept(t);
					t = next.apply(t);
				}
			}
		};

		return StreamSupport.stream(spliterator, false);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 *
	 * @param keys 键名序列
	 * @return keys 序列的比较器
	 */
	static Comparator<IRecord> cmp(final List<String> keys) {
		return IRecord.cmp(keys.toArray(new String[0]), true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 *
	 * @param <T>    元素类型
	 * @param <U>    具有比较能力的类型
	 * @param keys   keys 键名序列
	 * @param mapper (key:键名,t:键值)->u 比较能力变换器
	 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final List<String> keys,
			final BiFunction<String, T, U> mapper, final boolean asc) {
		return IRecord.cmp(keys.toArray(new String[0]), mapper, asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 *
	 * @param keys 键名序列
	 * @return keys 序列的比较器
	 */
	static Comparator<IRecord> cmp(final String[] keys) {
		return IRecord.cmp(keys, true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 *
	 * @param keys 键名序列
	 * @param asc  是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	static Comparator<IRecord> cmp(final String[] keys, final boolean asc) {
		return cmp(keys, null, asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 *
	 * @param <T>    元素类型
	 * @param <U>    具有比较能力的类型
	 * @param keys   键名序列
	 * @param mapper (key:键名,t:键值)->u 比较能力变换器
	 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	@SuppressWarnings("unchecked")
	static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final String[] keys,
			final BiFunction<String, T, U> mapper, final boolean asc) {

		final BiFunction<String, T, U> final_mapper = mapper == null
				? (String i, T o) -> o instanceof Comparable ? (U) o : (U) (o + "")
				: mapper;

		return (a, b) -> {
			final Queue<String> queue = new LinkedList<String>();
			for (String k : keys)
				queue.offer(k);// 压入队列
			while (!queue.isEmpty()) {
				final String key = queue.poll(); // 提取队首元素
				final Comparable<Object> ta = (Comparable<Object>) a.invoke(key, (T t) -> final_mapper.apply(key, t));
				final Comparable<Object> tb = (Comparable<Object>) b.invoke(key, (T t) -> final_mapper.apply(key, t));

				if (ta == null && tb == null)
					return 0;
				else if (ta == null)
					return -1;
				else if (tb == null)
					return 1;
				else {
					int ret = 0;

					try {
						ret = ta.compareTo(tb);// 进行元素比较
					} catch (Exception e) {
						final String[] aa = Stream.of(ta, tb).map(o -> o != null ? o.getClass().getName() + o : "null")
								.toArray(String[]::new);
						ret = aa[0].compareTo(aa[1]);// 进行元素比较
					} // try

					if (ret != 0) {
						return (asc ? 1 : -1) * ret; // 返回比较结果,如果不相等直接返回,相等则继续比计较
					} // if
				} // if
			} // while

			return 0;// 所有key都比较完毕,则认为两个元素相等
		};
	}

	/**
	 * 二元算术运算符号 除法<br>
	 * <p>
	 * 非数字 则 返回第一个值
	 *
	 * @return (record0, record1)->record2
	 */
	static BinaryOperator<IRecord> divide() {
		return IRecord.binaryOp((a, b) -> a / b);
	}

	/**
	 * 二元算术运算符号 乘法 <br>
	 * <p>
	 * 非数字 则 返回第一个值
	 *
	 * @return (record0, record1)->record2
	 */
	static BinaryOperator<IRecord> multiply() {
		return IRecord.binaryOp((a, b) -> a * b);
	}

	/**
	 * 二元算术运算符号 减法 <br>
	 * <p>
	 * 非数字 则 返回第一个值
	 *
	 * @return (record0, record1)->record2
	 */
	static BinaryOperator<IRecord> subtract() {
		return IRecord.binaryOp((a, b) -> a - b);
	}

	/**
	 * 二元算术运算符号 加法 <br>
	 * <p>
	 * 非数字 则 返回第一个值
	 *
	 * @return (record0, record1)->record2
	 */
	static BinaryOperator<IRecord> plus() {
		return IRecord.binaryOp(Double::sum);
	}

	/**
	 * 二元算术运算符号 <br>
	 * <p>
	 * 非数字 则 返回第一个值
	 *
	 * @param biop 归并器 (t,u)->v
	 * @return (record0, record1)->record2
	 */
	static BinaryOperator<IRecord> binaryOp(final BinaryOperator<Double> biop) {
		return IRecord.combine2((key, tup) -> {
			final Double[] aa = Stream.of(tup._1, tup._2).map(IRecord.obj2dbl()).toArray(Double[]::new);
			if (aa[0] != null && null != aa[1]) {
				return biop.apply(aa[0], aa[1]);
			} else { // 非数字 则 返回第一个值
				return tup._1 == null || tup._1.toString().matches("^\\s*$") ? tup._2() : tup._1;
			} // if
		});
	}

	/**
	 * 生成一个 IRecord的二元运算法。 最大值
	 *
	 * @param <T>       度量器的数据类型
	 * @param quantizer 度量器 t-&gt;number
	 * @return (record0, record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T> BinaryOperator<IRecord> max(final Function<T, Number> quantizer) {
		final Function<T, Number> quant = Optional.ofNullable(quantizer)
				.orElseGet(() -> x -> IRecord.REC("x", x).dblOpt("x").orElse(0d));

		return IRecord.combine2((k, tup) -> {
			final double a = Optional.ofNullable(tup._1).map(e -> (T) e).map(quant).map(Number::doubleValue).orElse(0d);
			final double b = Optional.ofNullable(tup._2).map(e -> (T) e).map(quant).map(Number::doubleValue).orElse(0d);
			return a > b ? tup._1 : tup._2;
		});
	}

	/**
	 * 生成一个 IRecord的二元运算法。最小值
	 *
	 * @param <T>       度量器的数据类型
	 * @param quantizer 度量器 t->number
	 * @return (record0, record1)->record2
	 */
	@SuppressWarnings("unchecked")
	static <T> BinaryOperator<IRecord> min(final Function<T, Number> quantizer) {
		final Function<T, Number> quant = Optional.ofNullable(quantizer)
				.orElseGet(() -> x -> IRecord.REC("x", x).dblOpt("x").orElse(0d));

		return IRecord.combine2((k, tup) -> {
			final double a = quant.apply((T) tup._1).doubleValue();
			final double b = quant.apply((T) tup._2).doubleValue();
			return a < b ? tup._1 : tup._2;
		});
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 *
	 * @param <T>    第一参数类型
	 * @param <U>    第二参数类型
	 * @param <V>    结果类型
	 * @param bifunc 归并器 (t,u)->v
	 * @return (record0, record1)->record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine(final BiFunction<T, U, V> bifunc) {

		return IRecord.combine2((k, tup) -> bifunc.apply((T) tup._1, (U) tup._2));
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 *
	 * @param <T>    第一参数类型
	 * @param <U>    第二参数类型
	 * @param <V>    结果类型
	 * @param bifunc 归并器 (k:键名,(t:左侧元素,u:右侧元素))->v
	 * @return (record0, record1)->record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine2(final BiFunction<String, Tuple2<T, U>, V> bifunc) {

		return IRecord.combine4((tup1, tup2) -> bifunc.apply(tup1._2, (Tuple2<T, U>) tup2));
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 *
	 * @param <T>    第一参数类型
	 * @param <U>    第二参数类型
	 * @param <V>    结果类型
	 * @param bifunc 归并器 (i:键名索引,(t:左侧元素,u:右侧元素))->v
	 * @return (record0, record1)->record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine3(final BiFunction<Integer, Tuple2<T, U>, V> bifunc) {

		return IRecord.combine4((tup1, tup2) -> bifunc.apply(tup1._1, (Tuple2<T, U>) tup2));
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 *
	 * @param <T>    第一参数类型
	 * @param <U>    第二参数类型
	 * @param <V>    结果类型
	 * @param bifunc 归并器 ((i:键名索引,k:键名),(t:左侧元素,u:右侧元素))->v
	 * @return (record0, record1)->record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine4(
			final BiFunction<Tuple2<Integer, String>, Tuple2<T, U>, V> bifunc) {
		return (record_left, record_right) -> {
			final List<String> empty = Arrays.asList();
			final List<String> keys_left = Optional.ofNullable(record_left).map(IRecord::keys).orElse(empty);
			final List<String> keys_right = Optional.ofNullable(record_right).map(IRecord::keys).orElse(empty);
			final List<String> keys = Stream.concat(keys_left.stream(), keys_right.stream()).distinct()
					.collect(Collectors.toList());
			final Builder rb = IRecord.rb(keys); // 返回结果的构建器
			final AtomicInteger ai = new AtomicInteger();
			final Object[] values = keys.stream().map(k -> {
				Object value = null;
				try {
					final int i = ai.getAndIncrement(); // 记录键名索引
					final T left = (T) record_left.get(k);
					final U right = (U) record_right.get(k);
					value = bifunc.apply(Tuple2.TUP2(i, k), Tuple2.TUP2(left, right)); // 返回结果
				} catch (Exception e) {
					e.printStackTrace();
				}
				return value;
			}).toArray(); // 计算结果值

			return rb.get(values);
		}; // BinaryOperator
	}

	/**
	 * 把 一个 Iterable 对象 转换成 flat List 结构
	 *
	 * @param itr    可遍历结构
	 * @param mapper 元素变换器 o->o
	 * @return ArrayList结构的数据
	 */
	static List<Object> itr2flatlist(final Iterable<?> itr, final Function<Object, Object> mapper) {
		final List<Object> list = new ArrayList<>();
		itr.forEach(e -> {
			if (e instanceof Iterable) {
				Iterable<?> e_itr = (Iterable<?>) e;
				final List<Object> ll = IRecord.itr2flatlist(e_itr, mapper);
				list.add(ll);
			} else {
				list.add(mapper.apply(e));
			}
		});
		return list;
	}

	/**
	 * 可枚举 转 列表
	 *
	 * @param <T>      元素类型
	 * @param iterable 可枚举类
	 * @param maxSize  最大的元素数量
	 * @return 元素列表
	 */
	static <T> List<T> itr2list(final Iterable<T> iterable, final int maxSize) {
		final Stream<T> stream = StreamSupport.stream(iterable.spliterator(), false).limit(maxSize);
		return stream.collect(Collectors.toList());
	}

	/**
	 * 可枚举 转 列表
	 *
	 * @param <T>      元素类型
	 * @param iterable 可枚举类
	 * @return 元素列表
	 */
	static <T> List<T> itr2list(final Iterable<T> iterable) {
		return IRecord.itr2list(iterable, MAX_SIZE);
	}

	/**
	 * Record类型的T元素归集器
	 *
	 * @param <T> 元组值类型
	 * @return IRecord类型的T元素归集器
	 */
	static <T> Collector<Tuple2<String, T>, ?, IRecord> recclc() {
		return IRecord.recclc(e -> e);
	}

	/**
	 * Record类型的T元素归集器
	 *
	 * @param <T>    元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param mapper Tuple2 类型的元素生成器 t->(str,u)
	 * @return IRecord类型的T元素归集器
	 */
	static <T, U> Collector<T, ?, IRecord> recclc(final Function<T, Tuple2<String, U>> mapper) {
		return Collector.of((Supplier<List<T>>) ArrayList::new, List::add, (left, right) -> {
			left.addAll(right);
			return left;
		}, (ll) -> { // finisher
			final IRecord rec = REC(); // 空对象
			ll.stream().map(mapper).forEach(rec::add);
			return rec;
		}); // Collector.of
	}

	/**
	 * Map类型的T元素归集器
	 *
	 * @param <K>    键类型
	 * @param <T>    元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param mapper Tuple2 类型的元素生成器 t->(k,u)
	 * @return IRecord类型的T元素归集器
	 */
	static <T, K, U> Collector<T, ?, Map<K, List<U>>> mapclc(final Function<T, Tuple2<K, U>> mapper) {
		return Collector.of((Supplier<Map<K, List<U>>>) HashMap::new, (tt, t) -> {
			final Tuple2<K, U> tup = mapper.apply(t);
			tt.computeIfAbsent(tup._1, _k -> new ArrayList<>()).add(tup._2);
		}, (left, right) -> {
			left.putAll(right);
			return left;
		}, (tt) -> { // finisher
			return tt;
		}); // Collector.of
	}

	/**
	 * Map类型的T元素归集器
	 *
	 * @param <K>    键类型
	 * @param <T>    元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param mapper Tuple2 类型的元素生成器 t->(k,u)
	 * @param biop   二元运算算子 (u,u)->u
	 * @return IRecord类型的T元素归集器
	 */
	static <T, K, U> Collector<T, ?, Map<K, U>> mapclc2(final Function<T, Tuple2<K, U>> mapper,
			final BinaryOperator<U> biop) {
		return Collector.of((Supplier<Map<K, List<U>>>) HashMap::new, (tt, t) -> {
			final Tuple2<K, U> tup = mapper.apply(t);
			tt.computeIfAbsent(tup._1, _k -> new ArrayList<>()).add(tup._2);
		}, (left, right) -> {
			left.putAll(right);
			return left;
		}, (tt) -> { // finisher
			Map<K, U> map = new LinkedHashMap<K, U>();
			tt.forEach((k, uu) -> {
				final U u = uu.stream().reduce(biop).orElse(null);
				map.put(k, u);
			});
			return map;
		}); // Collector.of
	}

	/**
	 * Map类型的T元素归集器
	 *
	 * @param <K>    键类型
	 * @param <T>    元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param mapper Tuple2 类型的元素生成器 t->(k,u)
	 * @return IRecord类型的T元素归集器
	 */
	static <T, K, U> Collector<T, ?, Map<K, U>> mapclc2(final Function<T, Tuple2<K, U>> mapper) {
		return IRecord.mapclc2(mapper, (a, b) -> b); // 使用新值覆盖老值。
	}

	/**
	 * Map类型的T元素归集器
	 *
	 * @param <K> 键类型
	 * @param <T> 元素类型
	 * @return IRecord类型的T元素归集器
	 */
	static <K, T> Collector<? super Tuple2<K, T>, ?, Map<K, T>> mapclc2() {
		return IRecord.mapclc2(e -> e, (a, b) -> b); // 使用新值覆盖老值。
	}

	/**
	 * 固定长度切割
	 *
	 * @param <T> 元素类型
	 * @param n   切割的长度
	 * @return 切割归集器
	 */
	static <T> Collector<T, ?, Stream<List<T>>> splitclcS(final int n) {
		return splitclcS((i, t) -> i > 0 && i % n == 0);
	}

	/**
	 * 指定条件切割
	 *
	 * @param <T>       元素类型
	 * @param predicate (i:元素索引编号从0开始,(line,t)：行数据缓存,当前字符)-> boolean,
	 *                  行分割函数，对于流中第一个元素 执行一下测试函数： predicate.test(0,(null,t))
	 * @return 切割归集器
	 */
	static <T> Collector<T, ?, Stream<List<T>>> splitclcS(final BiPredicate<Integer, Tuple2<List<T>, T>> predicate) {
		return splitclc(predicate, List::stream);
	}

	/**
	 * 固定长度切割
	 *
	 * @param <T>    元素类型
	 * @param <U>    结果类型
	 * @param n      切割的长度
	 * @param mapper [[t]]->u 结果变换函数
	 * @return 切割归集器
	 */
	static <T, U> Collector<T, ?, U> splitclc(final int n, final Function<List<List<T>>, U> mapper) {
		return splitclc((i, t) -> i > 0 && i % n == 0, mapper);
	}

	/**
	 * 指定条件切割
	 *
	 * @param <T>       元素类型
	 * @param <U>       结果类型
	 * @param predicate (i:元素索引编号从0开始,(line,t)：行数据缓存,当前字符)-> boolean,
	 *                  行分割函数，对于流中第一个元素 执行一下测试函数： predicate.test(0,(null,t))
	 * @param mapper    结果变换函数 [[t]] -> u
	 * @return 切割归集器
	 */
	static <T, U> Collector<T, ?, U> splitclc(final BiPredicate<Integer, Tuple2<List<T>, T>> predicate,
			final Function<List<List<T>>, U> mapper) {
		return splitclc(predicate, mapper, true); // 同步版本
	}

	/**
	 * 指定条件切割(同步版本)
	 *
	 * @param <T>       元素类型
	 * @param <U>       结果类型
	 * @param predicate (i:元素索引编号从0开始,(line,t)：行数据缓存,当前字符)-> boolean,
	 *                  行分割函数，对于流中第一个元素 执行一下测试函数： predicate.test(0,(null,t))
	 * @param mapper    结果变换函数 [[t]] -> u
	 * @param flag      同步异步标记,true 同步，false 异步。
	 * @return 切割归集器
	 */
	static <T, U> Collector<T, ?, U> splitclc(final BiPredicate<Integer, Tuple2<List<T>, T>> predicate,
			final Function<List<List<T>>, U> mapper, final boolean flag) {
		final Function<LinkedList<List<T>>, BiConsumer<Integer, T>> reader_builder = lines -> (i, t) -> {
			if (i == 0 || predicate.test(i, Tuple2.of(i == 0 ? null : lines.peekLast(), t))) { // 首元素或是满足切断条件新列表追加
				lines.add(new ArrayList<>()); // 添加新行
			} // if
		};

		if (flag) { // 同步版本
			final AtomicInteger ai = new AtomicInteger(); // 计数器
			final LinkedList<List<T>> _lines = new LinkedList<List<T>>();
			final BiConsumer<Integer, T> reader = reader_builder.apply(_lines);
			return Collector.of(() -> _lines, (lines, t) -> {
				final int i = ai.getAndIncrement(); // 构建器
				reader.accept(i, t);
				lines.peekLast().add(t);
			}, (aa, bb) -> {
				aa.addAll(bb);
				return aa;
			}, lines -> mapper.apply(lines)); // of
		} else { // 异步版本
			return Collector.of(() -> new ArrayList<T>(), (aa, a) -> aa.add(a), (aa, bb) -> {
				aa.addAll(bb);
				return aa;
			}, (tt) -> {
				final LinkedList<List<T>> lines = new LinkedList<List<T>>(); // 行序列
				final BiConsumer<Integer, T> reader = reader_builder.apply(lines);
				for (int i = 0; i < tt.size(); i++) {
					final T t = tt.get(i); // 当前数据元素
					reader.accept(i, t);
					lines.peekLast().add(t);
				} // for
				return mapper.apply(lines);
			}); // of
		}

	}

	/**
	 * 滑动窗口的T元素归集器<br>
	 * 返回非齐次窗口长度<br>
	 *
	 * @param <T>  流元素类型
	 * @param size 窗口长度 大于0的整数
	 * @param step 移动步长 大于0的整数
	 * @return 滑动窗口的T元素归集器
	 */
	public static <T> Collector<T, ?, Stream<List<T>>> slidingclc(final int size, final int step) {
		return IRecord.slidingclc(size, step, false);
	}

	/**
	 * 滑动窗口的T元素归集器 <br>
	 * 返回非齐次窗口长度<br>
	 *
	 * @param <T>      流元素类型
	 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt;&gt; 变换
	 *                 List&lt;DFrame;&gt; 这样的类型
	 * @param size     窗口长度 大于0的整数
	 * @param step     移动步长 大于0的整数
	 * @param finisher 最终结果处理器,比如 把 List&lt;List&lt;T&gt;&gt; 变换 List&lt;DFrame&gt;
	 *                 这样的类型函数
	 * @return 滑动窗口的T元素归集器，归集成U类型的窗口集合
	 */
	public static <T, U> Collector<T, ?, U> slidingclc(final int size, final int step,
			final Function<List<List<T>>, U> finisher) {
		return IRecord.slidingclc(size, step, true, finisher);
	}

	/**
	 * 滑动窗口的T元素归集器
	 *
	 * @param <T>  流元素类型
	 * @param size 窗口长度 大于0的整数
	 * @param step 移动步长 大于0的整数
	 * @param flag 是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
	 * @return 滑动窗口的T元素归集器
	 */
	public static <T> Collector<T, ?, Stream<List<T>>> slidingclc(final int size, final int step, final boolean flag) {
		return IRecord.slidingclc(size, step, flag, e -> e.stream());
	}

	/**
	 * 滑动窗口的T元素归集器
	 *
	 * @param <T>     流元素类型
	 * @param winctor 窗口构建函数:[t]-&gt;u
	 * @param size    窗口长度 大于0的整数
	 * @param step    移动步长 大于0的整数
	 * @param flag    是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
	 * @param flag    是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
	 * @return 滑动窗口的T元素归集器
	 */
	public static <T, U> Collector<T, ?, Stream<U>> slidingclc(final Function<List<T>, U> winctor, final int size,
			final int step, final boolean flag) {
		@SuppressWarnings("unchecked")
		final Function<List<T>, U> identity = e -> (U) e;
		return IRecord.slidingclc(size, step, flag, e -> e.stream().map(Optional.ofNullable(winctor).orElse(identity)));
	}

	/**
	 * 滑动窗口的T元素归集器
	 *
	 * @param <T>      流元素类型
	 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt;&gt; 变换
	 *                 List&lt;DFrame&gt; 这样的类型
	 * @param size     窗口长度 大于0的整数
	 * @param step     移动步长 大于0的整数
	 * @param flag     是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
	 * @param finisher 最终结果处理器,比如 把 List&lt;List&lt;T&gt;&gt; 变换 List&lt;DFrame&gt;
	 *                 这样的类型函数
	 * @return 滑动窗口的T元素归集器，归集成U类型的窗口集合
	 */
	public static <T, U> Collector<T, ?, U> slidingclc(final int size, final int step, final boolean flag,
			final Function<List<List<T>>, U> finisher) {
		return IRecord.slidingclc(size, step, flag, finisher, e -> e);
	}

	/**
	 * 滑动窗口的T元素归集器
	 *
	 * @param <F>      帧框类型
	 * @param <T>      流元素类型
	 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt;&gt; 变换
	 *                 List&lt;DFrame&gt; 这样的类型
	 * @param size     窗口长度 大于0的整数
	 * @param step     移动步长 大于0的整数
	 * @param flag     是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
	 * @param finisher 最终结果处理器,比如 把 List&lt;List&lt;T&gt;&gt; 变换成 List&lt;F&gt;
	 *                 这样的类型函数
	 * @param as_frame 帧框变换函数 把 List&lt;T&gt;&gt; 变换成 F 类型的 帧框
	 * @return 滑动窗口的T元素归集器，归集成U类型的窗口集合
	 */
	public static <F, T, U> Collector<T, ?, U> slidingclc(final int size, final int step, final boolean flag,
			final Function<List<F>, U> finisher, final Function<List<T>, F> as_frame) {
		final AtomicInteger ai = new AtomicInteger(0);
		return Collector.of(() -> new ArrayList<List<T>>(), (wnds, a) -> {
			if (ai.getAndIncrement() % step == 0) { // 达到步长位置则添加一个数据窗口
				wnds.add(new ArrayList<>()); // 添加一个数据窗口
			}
			final int n = wnds.size(); // 结果集合的长度
			for (int i = n - 1; i >= 0; i--) { // 从后前向依次遍历 追加数据元素，直到长度达到 窗口长度 size 位置
				final List<T> wnd = wnds.get(i); // 窗口数据
				// 由于是从后向前，所以一旦遇到长度等于要求长度的窗口,则停止倒退(向前滑行)，因为前面也都是size 长度的
				if (wnd.size() == size) { // 窗口长度达到要求，停止倒退，结束返回。
					break;
				} else { //
					wnd.add(a);
				} // if
			} // for
		}, (aa, bb) -> {
			try {
				throw new Exception("slidingclc 不支持并发 的流处理模式！");
			} catch (Exception e) {
				e.printStackTrace();
			}
			aa.addAll(bb);
			return aa;
		}, aa -> {
			return Optional.of(flag).map(e -> { // flag 的判断
				if (!e) { // 非齐整模式
					return aa;
				} else { // 齐整模式
					int i = aa.size() - 1; // 末尾元素的下标索引
					while (i > 0) {
						if (aa.get(i).size() >= size) { // 直到现在子列表长度等于size的的最大下标索引
							break;
						} else { // 长度小于窗口长度size 的继续向前移动
							i--;
						} // if
					} // while

					final List<List<T>> homo_aa = i == aa.size() - 1 ? aa : aa.subList(0, i + 1); // 提取齐整的列表集合
					return homo_aa; // 齐整列表
				} // if
			}).map(ll -> {
				final List<F> frames = ll.stream().map(as_frame).collect(Collectors.toList());
				return finisher.apply(frames);
			}).get(); // of
		}); // Collector.of
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>   元素数据类型
	 * @param <K>   键名类型
	 * @param keyer 键名函数,分类规则&amp;依据 x-&gt;key
	 * @return Map:{(K,U)}
	 */
	public static <X, K> Collector<X, ?, Map<K, List<X>>> grpclc2(final Function<X, K> keyer) {
		return grpclc(keyer, e -> e, e -> e, e -> e);
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>     元素数据类型
	 * @param <K>     键名类型
	 * @param <V>     键值类型
	 * @param <U>     中间结果类型
	 * @param keyer   键名函数,分类规则&amp;依据 x-&gt;key
	 * @param valueer 键值创建函数 x-&gt;value
	 * @param uclc    键值元素集合包装函数 vv-&gt;u
	 * @return Map:{(K,U)}
	 */
	public static <X, K, V, U> Collector<X, ?, Map<K, U>> grpclc2(final Function<X, K> keyer,
			final Function<X, V> valueer, final Collector<V, ?, U> uclc) {
		return IRecord.grpclc(keyer, valueer, uclc, e -> e);
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>        元素数据类型
	 * @param <K>        键名类型
	 * @param <V>        键值类型
	 * @param <U>        中间结果类型
	 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
	 * @param valueer    键值创建函数 x-&gt;value
	 * @param u_finisher 键值元素集合包装函数 vv-&gt;u
	 * @return Map:{(K,U)}
	 */
	public static <X, K, V, U> Collector<X, ?, Map<K, U>> grpclc2(final Function<X, K> keyer,
			final Function<X, V> valueer, final Function<List<V>, U> u_finisher) {
		return IRecord.grpclc(keyer, valueer, u_finisher, e -> e);
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>        元素数据类型
	 * @param <K>        键名类型
	 * @param <Z>        最终结果类型
	 * @param keyer      键名函数,分类规则&amp;依据 x->key
	 * @param z_finisher 最终结果包装函数 {(k,v)}->z
	 * @return Z类型的结果
	 */
	public static <X, K, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer,
			final Function<Map<K, List<X>>, Z> z_finisher) {
		return grpclc(keyer, e -> e, e -> e, z_finisher);
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>        元素数据类型
	 * @param <K>        键名类型
	 * @param <V>        键名值
	 * @param <Z>        最终结果类型
	 * @param keyer      键名函数,分类规则&amp;依据 x->key
	 * @param valuerer   键值函数 x-&gt;value
	 * @param z_finisher 最终结果包装函数 {(k,v)}-&gt;z
	 * @return Z类型的结果
	 */
	public static <X, K, V, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer, final Function<X, V> valuerer,
			final Function<Map<K, List<V>>, Z> z_finisher) {
		return grpclc(keyer, valuerer, e -> e, z_finisher);
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>        元素数据类型
	 * @param <K>        键名类型
	 * @param <V>        键值类型
	 * @param <U>        中间结果类型
	 * @param <Z>        结果类型
	 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
	 * @param valueer    键值创建函数 x-&gt;value
	 * @param uclc       键值元素集合归集器 vv-&gt;u
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
	 * @return U 结果类型
	 */
	public static <X, K, V, U, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer, final Function<X, V> valueer,
			final Collector<V, ?, U> uclc, final Function<Map<K, U>, Z> z_finisher) {
		return IRecord.grpclc(keyer, valueer, ll -> ll.stream().collect(uclc), z_finisher);
	}

	/**
	 * 分组归集器
	 *
	 * @param <X>        元素数据类型
	 * @param <K>        键名类型
	 * @param <V>        键值类型
	 * @param <U>        中间结果类型
	 * @param <Z>        结果类型
	 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
	 * @param valueer    键值创建函数 x-&gt;value
	 * @param u_finisher 键值元素集合包装函数 vv-&gt;u
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
	 * @return U 结果类型
	 */
	public static <X, K, V, U, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer, final Function<X, V> valueer,
			final Function<List<V>, U> u_finisher, final Function<Map<K, U>, Z> z_finisher) {
		final Collector<X, LinkedHashMap<K, List<V>>, Z> clc = Collector.of( // 创建轨迹器
				(Supplier<LinkedHashMap<K, List<V>>>) () -> new LinkedHashMap<K, List<V>>(),
				(LinkedHashMap<K, List<V>> lhm, X x) -> {
					final K k = keyer.apply(x);
					final V v = valueer.apply(x);
					lhm.computeIfAbsent(k, _k -> new ArrayList<V>()).add(v);
				}, (tuples1, tuples2) -> {
					tuples1.putAll(tuples2);
					return tuples1;
				}, lhm -> { // 最终类型包装器
					final LinkedHashMap<K, U> lhm1 = new LinkedHashMap<K, U>();

					lhm.entrySet().stream().forEach(entry -> {
						final K key = entry.getKey(); // 键名
						final U u = u_finisher.apply(entry.getValue()); // 值类型归集
						lhm1.put(key, u); // 值类型
					});

					final Z z = z_finisher.apply(lhm1);

					return z;
				}); // Collector.of

		return clc;
	}

	/**
	 * 数据透视表的归集结构
	 *
	 * @param keys 归集的键名序列,键名之间采用半全角的逗号活分号即"[,，;；]+" 进行分隔。
	 * @return 数据透视表
	 */
	public static Collector<IRecord, ?, IRecord> pvtclc(final String keys) {
		return IRecord.pvtclc(e -> e, keys.split("[,，;；]+"));
	}

	/**
	 * 数据透视表的归集结构
	 *
	 * @param keys 归集的键名序列,keys 长度为0则统一归key类别之下
	 * @return 数据透视表
	 */
	public static Collector<IRecord, ?, IRecord> pvtclc(final String... keys) {
		return IRecord.pvtclc(e -> e, keys);
	}

	/**
	 * 数据透视表的归集结构 <br>
	 * 把 [IRecord] 集合数据 归集到 由 keys 所标识的 路径集合中，并 调用evaluator 做集合数据额演算。
	 *
	 * @param <T>       路径集合的函数的计算值类型
	 * @param evaluator 路径集合计算函数 ([r]:IRecord列表)->t
	 * @param keys      归集的键名序列,键名间采用英文半角','分隔,keys 长度为0则统一归key类别之下
	 * @return 数据透视表
	 */
	public static <T> Collector<IRecord, ?, IRecord> pvtclc(final Function<List<IRecord>, T> evaluator,
			final String keys) {
		return IRecord.pvtclc(evaluator, keys == null ? new String[0] : keys.split("[,]+"));
	}

	/**
	 * 数据透视表的归集结构 <br>
	 * 把 [IRecord] 集合数据 归集到 由 keys 所标识的 路径集合中，并 调用evaluator 做集合数据额演算。
	 *
	 * @param <T>       路径集合的函数的计算值类型
	 * @param evaluator 路径集合计算函数 ([r]:IRecord列表)->t
	 * @param keys      归集的键名序列,keys 长度为0则统一归key类别之下
	 * @return 数据透视表
	 */
	public static <T> Collector<IRecord, ?, IRecord> pvtclc(final Function<List<IRecord>, T> evaluator,
			final String... keys) {
		if (keys.length > 1 && keys[0] != null) { // 键名序列长度大于0,递归归集成嵌套的IRecord
			final String[] restings = Arrays.copyOfRange(keys, 1, keys.length); // 剩余的键名
			return IRecord.grpclc((IRecord r) -> r.str(keys[0].trim()), e -> e, pvtclc(evaluator, restings),
					IRecord::REC); // 递归归集
		} else if (keys.length > 0 && keys[0] != null) { // 键名序列长度为1,键值归集成列表
			return IRecord.grpclc((IRecord r) -> r.str(keys[0].trim()), e -> e, evaluator, IRecord::REC);
		} else { // 没有键名序列,统一归key类别之下
			return IRecord.grpclc((IRecord r) -> "key", e -> e, evaluator, IRecord::REC);
		} // if
	}

	/**
	 * 集合并集(c1包含 或者 c2包含)
	 *
	 * @param <T> 元素类型
	 * @param c1  集合1
	 * @param c2  集合2
	 * @return 集合并集
	 */
	@SuppressWarnings("unchecked")
	public static <T> LinkedHashSet<T> union(final Collection<T> c1, final Collection<T> c2) {
		return (LinkedHashSet<T>) Stream.concat(c1.stream(), c2.stream()).distinct()
				.collect(Collector.of(LinkedHashSet::new, LinkedHashSet::add, (aa, bb) -> {
					aa.add(bb);
					return aa;
				}, aa -> aa));
	}

	/**
	 * 集合交集(c1 包含 并且 c2也包含)
	 *
	 * @param <T> 元素类型
	 * @param c1  集合1
	 * @param c2  集合2
	 * @return 集合交集
	 */
	@SuppressWarnings("unchecked")
	public static <T> LinkedHashSet<T> intersect(final Collection<T> c1, final Collection<T> c2) {
		return (LinkedHashSet<T>) c1.stream().filter(c2::contains)
				.collect(Collector.of(LinkedHashSet::new, LinkedHashSet::add, (aa, bb) -> {
					aa.add(bb);
					return aa;
				}, aa -> aa));
	}

	/**
	 * 集合差集 (c1 包含 并且 c2不包含)
	 *
	 * @param <T> 元素类型
	 * @param c1  集合1
	 * @param c2  集合2
	 * @return 集合差集
	 */
	@SuppressWarnings("unchecked")
	public static <T> LinkedHashSet<T> diff(final Collection<T> c1, final Collection<T> c2) {
		return (LinkedHashSet<T>) c1.stream().filter(e -> !c2.contains(e))
				.collect(Collector.of(LinkedHashSet::new, LinkedHashSet::add, (aa, bb) -> {
					aa.add(bb);
					return aa;
				}, aa -> aa));
	}

	/**
	 * list comprehend
	 *
	 * @param <T> 元素类型
	 * @param tts 批次层级数据,[tt1,tt2,...,tt_i,...] 每个批次 tt_i 都是一个 [t]
	 * @return 流引用，元素为缓存引用
	 */
	@SuppressWarnings("unchecked")
	public static <T> Stream<T[]> cph(final T[]... tts) {
		final AtomicInteger ai = new AtomicInteger(0); // 阶层偏移
		final Class<?> tclass = tts.getClass().getComponentType().getComponentType(); // tt 的元素的类型
		final T[] buffer = (T[]) Array.newInstance(tclass, tts.length); // 工作缓存,从根节点向叶子节点逐层递进
		return Stream.of(tts).reduce(Stream.of((T) null), (parentS, tt) -> {
			final int i = ai.getAndIncrement(); // level 阶层位置，注意一定要用非对象类型:int而非Integer
			return parentS.flatMap(parent -> Stream.of(tt).map(t -> buffer[i] = t)); // 反腐刷新工作缓存输出流
		}, (a, b) -> a).map(e -> buffer); // 输出缓存状态
	}

	/**
	 * list comprehend
	 *
	 * @param <T> 元素类型
	 * @param tts 批次层级数据,[tt1,tt2,...,tt_i,...] 每个批次 tt_i 都是一个 [t]
	 * @return 流引用，元素为缓存引用
	 */
	@SuppressWarnings("unchecked")
	public static <T> Stream<T[]> cph(final Iterable<T[]> tts) {
		final T[] sample = tts.iterator().next();
		final T[][] _tts = StreamSupport.stream(tts.spliterator(), false)
				.toArray(n -> (T[][]) Array.newInstance(sample.getClass(), n));
		return cph(_tts);
	}

	/**
	 * 强制转换函数
	 *
	 * @param <T>   源类型
	 * @param <U>   目标类型
	 * @param unull U类型的占位符
	 * @return 把t类型转换成X类型的函数
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Function<T, U> nullas(final U unull) {
		return t -> (U) t;
	}

	/**
	 * 数组元素
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return T[]
	 */
	@SafeVarargs
	public static <T> T[] A(final T... ts) {
		return ts;
	}

	/**
	 * 列表元素
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return T[]
	 */
	@SafeVarargs
	public static <T> List<T> L(final T... ts) {
		return Arrays.asList(ts);
	}

	/**
	 * repeat array <br>
	 * 复制元素t,生成数组 n 个元素的数组
	 * 
	 * @param <T> 数组元素类型
	 * @param t   数组元素
	 * @param n   负责次数
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] RPTA(final T t, final int n) {
		final Class<T> clazz = (Class<T>) t.getClass();
		final T[] tt = (T[]) Array.newInstance(clazz, n);
		for (int i = 0; i < n; i++)
			tt[i] = t;
		return tt;
	}

	/**
	 * 随机生成的数据元素
	 *
	 * @param <T> 元素类型
	 * @param ts  元素列表
	 * @return 随机生成的数据元素
	 */
	@SafeVarargs
	public static <T> T rndget(T... ts) {
		return ts[new Random().nextInt(ts.length)];
	}

	/**
	 * 把一个对象转换成键值对儿(javabean分解)
	 * 
	 * @param obj 目标对象
	 * @return 键值对儿列表
	 */
	public static LinkedHashMap<String, Object> obj2lhm(final Object obj) {
		final LinkedHashMap<String, Object> lhm = new LinkedHashMap<String, Object>();
		for (final Field fld : obj.getClass().getDeclaredFields()) {
			fld.setAccessible(true);
			final String key = fld.getName();
			try {
				final Object value = fld.get(obj);
				lhm.put(key, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // for

		return lhm;
	}

	/**
	 * 最大数字数量,默认为10000
	 */
	int MAX_SIZE = 10000; // 最大的数量

	/**
	 * IRecord 构建器
	 *
	 * @author gbench
	 */
	class Builder {

		/**
		 * 构造IRecord构造器
		 * 
		 * @param <T>  参数元素类型
		 * @param keys 键名列表的迭代器
		 */
		public <T> Builder(final Iterable<T> keys) {
			this.keys = new ArrayList<>(StreamSupport.stream(keys.spliterator(), false).limit(10000).map(e -> e + "")
					.collect(Collectors.toList()));
		}

		/**
		 * build
		 * 
		 * @param <T> 参数列表元素类型
		 * @param kvs 键值序列 key1,value1,key2,value2,...
		 * @return IRecord 对象
		 */
		@SafeVarargs
		final public <T> IRecord build(final T... kvs) {
			return MyRecord.REC(kvs);
		}

		/**
		 * 构造 IRecord <br>
		 * 按照构建器的 键名序列表，依次把objs中的元素与其适配以生成 IRecord <br>
		 * {key0:objs[0],key1:objs[1],key2:objs[2],...}
		 *
		 * @param <T>  参数列表元素类型
		 * @param objs 值序列, 若 objs 为 null 则返回null, <br>
		 *             若 objs 长度不足以匹配 keys 将采用 循环补位的仿制给予填充 <br>
		 *             若 objs 长度为0则返回一个空对象{},注意是没有元素且不是null的对象
		 * @return IRecord 对象 若 objs 为 null 则返回null
		 */
		@SafeVarargs
		final public <T> IRecord get(final T... objs) {
			if (objs == null) { // 空值判断
				return null;
			}

			final int size = this.keys.size();
			final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();

			Object[] oo = objs; // 参数值
			if (objs.length == 1) { // 单一值的情况
				if (objs[0] instanceof Iterable<?> itr) {
					if (itr instanceof Collection<?> coll) {
						oo = coll.toArray();
					} else {
						oo = StreamSupport.stream(itr.spliterator(), false).limit(MAX_SIZE).toArray();
					} // if
				} else if (objs[0] instanceof Stream) {
					oo = ((Stream<?>) objs[0]).toArray();
				} else {
					// do nothing
				} // if
			} // if

			final int n = oo.length;
			for (int i = 0; n > 0 && i < size; i++) {
				final String key = keys.get(i);
				final Object value = oo[i % n];
				data.put(key, value == null ? "" : value); // key 默认为 ""
			} // for

			return this.build(data);
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public List<String> keys() {
			return this.keys;
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public Stream<String> keyS() {
			return this.keys().stream();
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public String[] keyA() {
			return this.toArray();
		}

		/**
		 * 注意这是一个修改Builder的方法。<br>
		 * Inserts the specified element at the specified position in thislist. Shifts
		 * the element currently at that position (if any) andany subsequent elements to
		 * the right (adds one to their indices).
		 * 
		 * @param index at which the specified element is to be inserted
		 * @param key   element to be inserted
		 * @return Builder 对象本身
		 */
		public Builder insert(final int index, final String key) {
			this.keys.add(index, key);
			return this;
		}

		/**
		 * 在头部添加key
		 * 
		 * @param keys 键名序列
		 * @return Builder 对象复制品
		 */
		public Builder prepend(final String... keys) {
			final int this_size = this.keys.size();
			final String[] kk = Arrays.copyOf(keys, keys.length + this_size);
			final int start = keys.length;
			for (int i = 0; i < this_size; i++) {
				kk[i + start] = this.keys.get(i);
			}
			return new Builder(Arrays.asList(kk));
		}

		/**
		 * 尾部追加键名
		 * 
		 * @param keys 键名序列
		 * @return 对象复制品
		 */
		public Builder append(final String... keys) {
			final Builder rb = this.clone();
			rb.keys.addAll(Arrays.asList(keys));
			return rb;
		}

		/**
		 * 构造复制品
		 * 
		 * @return Builder
		 */
		public Builder duplicate() {
			return this.clone();
		}

		/**
		 * 复制品
		 */
		@Override
		public Builder clone() {
			return new Builder(this.keys);
		}

		/**
		 * 键列表keys长度
		 * 
		 * @return 键列表keys长度
		 */
		public int size() {
			return this.keys.size();
		}

		/**
		 * 对keys进行变换
		 * 
		 * @param <T>    映射的结果类型
		 * @param mapper keys:[k] -> T 变换函数
		 * @return T 类型的结果
		 */
		public <T> T arrayOf(final Function<String[], T> mapper) {
			return mapper.apply(keys.toArray(String[]::new));
		}

		/**
		 * 键名列表
		 * 
		 * @return keys 的数组结果
		 */
		public String[] toArray() {
			return this.arrayOf(e -> e);
		}

		private ArrayList<String> keys = new ArrayList<>();

	}

}
