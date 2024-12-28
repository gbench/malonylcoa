package gbench.util.math.algebra.tuple;

import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 数据记录对象
 * 
 * @author gbench
 *
 */
public interface IRecord // 记录结构
		extends IterableOps<Tuple2<String, Object>, IRecord>, // Iterable的基本运算
		Comparable<IRecord> { // 可比较

	/**
	 * 根据键名进行取值
	 * 
	 * @param key 键名
	 * @return 键key的值
	 */
	Object get(final String key);

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
	 * 返回值数据流
	 * 
	 * @return 值数据流
	 * 
	 */
	default Stream<Object> valueS() {
		return this.keys().stream().map(k -> this.get(k));
	}

	/**
	 * 返回值列表
	 * 
	 * @return 值列表
	 * 
	 */
	default List<Object> values() {
		return this.valueS().collect(Collectors.toList());
	}

	/**
	 * 更新式添加<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 * 
	 * @param key   新的 键名
	 * @param value 新的 键值
	 * @return 对象本身
	 */
	default IRecord add(final String key, final Object value) {
		this.set(key, value);
		return this;
	}

	/**
	 * 更新式添加<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 * 
	 * @param kvs 键名键值序列
	 * @return 对象本身
	 */
	default IRecord add(final Object... kvs) {
		IRecord.slidingS(kvs, 2, 2, true).forEach(wnd -> { // 窗口遍历
			this.add(wnd.get(0) + "", wnd.get(1));
		});
		return this;
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
	String toString(final Function<Object, ?> preprocessor, final String e_delimiter, final String e_prefix,
			final String e_suffix, final String ee_delimiter, final String ee_prefix, final String ee_suffix,
			final String kvp_delimiter, final String key_prefix, final String key_suffix,
			final Function<Object, String> value_formatter);

	/**
	 * 更新式添加,即改变自身的内容<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 * 
	 * @param tup 待添加的键值对
	 * @return 对象本身
	 */
	default IRecord add(final Tuple2<String, ?> tup) {
		return this.add(tup._1, tup._2);
	}

	/**
	 * 更新式添加,即改变自身的内容<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 * 
	 * @param rec 待添加的键值对集合
	 * @return 对象本身
	 */
	default IRecord add(final IRecord rec) {
		rec.forEach(tup -> this.add(tup));
		return this;
	}

	/**
	 * 更新式添加,即改变自身的内容<br>
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 * 
	 * @param map 待添加的键值对集合
	 * @return 对象本身
	 */

	default IRecord add(final Map<String, ?> map) {
		map.forEach(this::add);
		return this;
	}

	/**
	 * 返回 key 所对应的 值
	 * 
	 * @param <T>          目标类型
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return T 类型的数值
	 */
	default <T> T get(final String key, final T defaultValue) {
		return this.get(key, _k -> defaultValue);
	}

	/**
	 * 返回 key 所对应的 值
	 * 
	 * @param <T>    目标类型
	 * @param key    键名
	 * @param tclass key 所标定的 值 的 类型
	 * @return T 类型的数值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(final String key, final Class<T> tclass) {
		return (T) this.get(key);
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
	default <T> T get(final String key, Function<String, T> defaultEvaluator) {
		@SuppressWarnings("unchecked")
		final var t = (T) this.get(key);
		return Optional.ofNullable(t).orElse(defaultEvaluator.apply(key));
	}

	/**
	 * 返回 键名索引 所对应的 值
	 * 
	 * @param idx 键名索引 从0开始
	 * @return Object 类型的数值
	 */
	default <T> Object get(final int idx) {
		return this.get(idx, (Object) null);
	}

	/**
	 * 返回 键名索引 所对应的 值
	 * 
	 * @param <T>          目标类型
	 * @param idx          键名索引 从0开始
	 * @param defaultValue 默认值
	 * @return T 类型的数值
	 */
	default <T> T get(final int idx, final T defaultValue) {
		return this.get(idx, _idx -> defaultValue);
	}

	/**
	 * 返回 键名索引 所对应的 值
	 * 
	 * @param <T>    目标类型
	 * @param idx    键名索引从0开始
	 * @param tclass key 所标定的 值 的 类型
	 * @return T 类型的数值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(final int idx, final Class<T> tclass) {
		final var key = this.keys().get(idx);
		return (T) this.get(key);
	}

	/**
	 * 带有缺省值计算的值获取函数，<br>
	 * defaultEvaluator 计算的结果并不给予更新到this当中。这是与computeIfAbsent不同的。
	 * 
	 * @param <T>              返回值类型
	 * @param idx              键名索引从0开始
	 * @param defaultEvaluator 缺省值计算函数(key)->obj
	 * @return 缺值计算的值
	 */
	default <T> T get(final int idx, Function<Integer, T> defaultEvaluator) {
		final var key = this.keys().get(idx);
		return this.get(key, k -> defaultEvaluator.apply(idx));
	}

	/**
	 * 提取指定键值索引的值
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param idx    键名索引从0开始
	 * @param mapper 映射函数 x->t
	 * @param xclass 类型参照类的类名
	 * @return 变换后的值
	 */
	default <X, T> T get(final int idx, final Function<X, T> mapper, final Class<X> xclass) {
		return this.get(this.keyOf(idx), mapper, xclass);
	}

	/**
	 * 使用指定类型函数做类型变换来做转换,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * 
	 * @param <X>      原值类型
	 * @param <T>      新值类型
	 * @param idx      键名索引从0开始
	 * @param mapper   映射函数 x-&gt;t
	 * @param defaultX 默认值
	 * @return 变换后的值
	 */
	default <X, T> T get(final Integer idx, final Function<X, T> mapper, final X defaultX) {
		return this.get(this.keyOf(idx), mapper, defaultX);
	}

	/**
	 * 使用指定类型函数做类型变换来做转换,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * 
	 * @param <X>      原值类型
	 * @param <T>      新值类型
	 * @param key      键名
	 * @param mapper   映射函数 x-&gt;t
	 * @param defaultX 默认值
	 * @return 变换后的值
	 */
	@SuppressWarnings("unchecked")
	default <X, T> T get(final String key, final Function<X, T> mapper, final X defaultX) {
		final var xclass = (Class<X>) (defaultX != null ? defaultX.getClass() : Object.class);
		return this.get(key, mapper, xclass, defaultX);
	}

	/**
	 * 使用指定类型函数做类型变换来做转换
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param key    键名
	 * @param mapper 映射函数 x->t
	 * @param xclass 类型参照类的类名
	 * @return 变换后的值
	 */
	default <X, T> T get(final String key, final Function<X, T> mapper, final Class<X> xclass) {
		return this.get(key, mapper, xclass, null);
	}

	/**
	 * 使用指定类型函数做类型变换来做转换
	 * 
	 * @param <X>      原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>      新值类型
	 * @param idx      键名索引从0开始
	 * @param mapper   映射函数 x-&gt;t
	 * @param xclass   类型参照类的类名
	 * @param defaultX 默认值
	 * @return 变换后的值
	 */
	default <X, T> T get(final Integer idx, final Function<X, T> mapper, final Class<X> xclass, final X defaultX) {
		return this.get(this.keyOf(idx), mapper, xclass, defaultX);
	}

	/**
	 * 使用指定类型函数做类型变换来做转换
	 * 
	 * @param <X>      原值类型，包括:Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>      新值类型
	 * @param key      键名
	 * @param mapper   映射函数 x-&gt;t
	 * @param xclass   类型参照类的类名
	 * @param defaultX 默认值
	 * @return 变换后的值
	 */
	@SuppressWarnings("unchecked")
	default <X, T> T get(final String key, final Function<X, T> mapper, final Class<X> xclass, final X defaultX) {
		final var x = (X) Optional.of(xclass).map(v -> {
			if (xclass == Boolean.class || xclass == boolean.class) {
				return (X) this.bool(key);
			} else if (xclass == Integer.class || xclass == int.class) {
				return (X) this.i4(key);
			} else if (xclass == Long.class || xclass == long.class) {
				return (X) this.lng(key);
			} else if (xclass == Double.class || xclass == int.class) {
				return (X) this.dbl(key);
			} else if (xclass == Short.class || xclass == short.class) {
				return (X) (Short) this.i4(key).shortValue();
			} else if (xclass == LocalDate.class) {
				return this.ld(key);
			} else if (xclass == LocalDateTime.class) {
				return (X) this.ldt(key);
			} else if (xclass == LocalTime.class) {
				return (X) this.lt(key);
			} else if (xclass == String.class) {
				return (X) this.lt(key);
			} else if (xclass == IRecord.class) {
				return (X) this.rec(key);
			} else {
				return (X) this.get(key);
			}
		}).orElse(defaultX);

		final var value = (T) mapper.apply(x);
		return value;
	}

	/**
	 * 提取可选值
	 * 
	 * @param key 键名
	 * @return Optional
	 */
	default Optional<Object> opt(final String key) {
		return Optional.ofNullable(this.get(key));
	}

	/**
	 * 提取可选值
	 * 
	 * @param idx 键名索引 从0开始
	 * @return Optional
	 */
	default Optional<Object> opt(final int idx) {
		return this.opt(this.keyOf(idx));
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
	 * 把key列转换成逻辑值
	 * 
	 * @param key 键名
	 * @return 布尔类型
	 */
	default Boolean bool(final String key) {
		return this.get(key, o -> Boolean.parseBoolean(o + ""));
	};

	/**
	 * 把key列转换成逻辑值
	 * 
	 * @param key           键名
	 * @param default_value 默认值
	 * @return 布尔类型
	 */
	default Boolean bool(final String key, final Boolean default_value) {
		final var b = this.bool(key);
		return b == null ? default_value : b;
	};

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx 键名索引从0开始
	 * @return Boolean 类型
	 */
	default Boolean bool(final int idx) {
		String key = this.keyOf(idx);
		return key == null ? null : bool(key);
	};

	/**
	 * 把key列转换成逻辑值
	 * 
	 * @param index         键名索引从0开始
	 * @param default_value 默认值
	 * @return 布尔类型
	 */
	default Boolean bool(final int index, final Boolean default_value) {
		final var b = this.bool(index);
		return b == null ? default_value : b;
	};

	/**
	 * 返回 建明索引 所对应的 键值, Double 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值
	 */
	default Double dbl(int idx) {
		return this.dbl(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Double dbl(String key) {
		return IRecord.obj2dbl().apply(this.get(key));
	}

	/**
	 * 返回 键名索引 所对应的 键值, Double 类型
	 * 
	 * @param idx          键名索引 从 0开始
	 * @param defaultValue 默认值
	 * @return idx 所标定的 值
	 */
	default Double dbl(final int idx, final Double defaultValue) {
		return this.dbl(this.keyOf(idx), defaultValue);
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 * 
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return key 所标定的 值
	 */
	default Double dbl(final String key, final Double defaultValue) {
		return IRecord.obj2dbl(defaultValue).apply(this.get(key));
	}

	/**
	 * 返回 键名索引 所对应的 键值, Integer 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值
	 */
	default Integer i4(final Integer idx) {
		return this.i4(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Integer i4(final String key) {
		return this.dbl(key).intValue();
	}

	/**
	 * 返回 键名索引 所对应的 键值, Integer 类型
	 * 
	 * @param idx          键名索引从0开始
	 * @param defaultValue 默认的值
	 * @return idx 所标定的 值
	 */
	default Integer i4(final Integer idx, final Integer defaultValue) {
		return this.i4(this.keyOf(idx), defaultValue);
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 * 
	 * @param key          键名
	 * @param defaultValue 默认的值
	 * @return key 所标定的 值
	 */
	default Integer i4(final String key, final Integer defaultValue) {
		return IRecord.obj2dbl(defaultValue).apply(this.get(key)).intValue();
	}

	/**
	 * 返回 键名索引 所对应的 键值, Long 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值
	 */
	default Long lng(final int idx) {
		return this.lng(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Long 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Long lng(final String key) {
		return this.dbl(key).longValue();
	}

	/**
	 * 返回 键名索引 所对应的 键值, Long 类型
	 * 
	 * @param idx          键名索引 从0开始
	 * @param defaultValue 默认的值
	 * @return idx 所标定的 值
	 */
	default Long lng(final int idx, final Integer defaultValue) {
		return this.lng(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Long 类型
	 * 
	 * @param key          键名
	 * @param defaultValue 默认的值
	 * @return key 所标定的 值
	 */
	default Long lng(final String key, final Integer defaultValue) {
		return IRecord.obj2dbl(defaultValue).apply(this.get(key)).longValue();
	}

	/**
	 * 返回 建明索引 所对应的 键值, String 类型
	 * 
	 * @param idx 键名索引
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
		final var obj = this.get(key);

		if (obj == null) {
			return null;
		}

		return obj instanceof String ? (String) obj : obj + "";
	}

	/**
	 * 返回 键名索引 所对应的 键值, String 类型
	 * 
	 * @param idx          键名索引 从0开始
	 * @param defaultValue 默认的值
	 * @return idx 所标定的 值
	 */
	default String str(final int idx, final String defaultValue) {
		return this.str(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, String 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default String str(final String key, final String defaultValue) {
		final var obj = this.get(key);

		if (obj == null) {
			return defaultValue;
		}

		return obj instanceof String ? (String) obj : obj + "";
	}

	/**
	 * 返回 idx 键名索引 所对应的 键值, LocalTime 类型
	 * 
	 * @param idx 键名索引 从 0开始
	 * @return idx 键名索引 所标定的 值
	 */
	default LocalTime lt(final int idx) {
		return this.lt(this.keyOf(idx));
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
		final var ldt = this.ldt(key, null);
		return Optional.ofNullable(ldt).map(LocalDateTime::toLocalTime).orElse(defaultValue);
	}

	/**
	 * 返回 idx 键名索引 所对应的 键值, LocalDate 类型
	 * 
	 * @param idx 键名索引 从 0开始
	 * @return idx 键名索引 所标定的 值
	 */
	default LocalDate ld(final int idx) {
		return this.ld(this.keyOf(idx));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDate 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default LocalDate ld(final String key) {
		return this.ld(key, null);
	}

	/**
	 * 返回 idx 键名索引 所对应的 键值, LocalDate 类型
	 * 
	 * @param idx          键名索引 从0开始
	 * @param defaultValue 默认值
	 * @return idx 键名索引 所标定的 值
	 */
	default LocalDate ld(final int idx, LocalDate defaultValue) {
		return this.ld(this.keyOf(idx), defaultValue);
	}

	/**
	 * 返回 key 所对应的 键值, LocalDate 类型
	 * 
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return key 所标定的 值
	 */
	default LocalDate ld(final String key, LocalDate defaultValue) {
		final var ldt = this.ldt(key);
		return Optional.ofNullable(ldt).map(LocalDateTime::toLocalDate).orElse(defaultValue);
	}

	/**
	 * 返回 idx 键名索引 所对应的 键值, LocalDateTime 类型
	 * 
	 * @param idx 键名索引 从 0开始
	 * @return idx 键名索引 所标定的 值
	 */
	default LocalDateTime ldt(final int idx) {
		return this.ldt(idx, null);
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
	 * 返回 idx 键名索引 所对应的 键值, LocalDateTime 类型
	 * 
	 * @param idx          键名索引 从 0开始
	 * @param defaultValue 默认值
	 * @return idx 键名索引 所标定的 值
	 */
	default LocalDateTime ldt(final int idx, final LocalDateTime defaultValue) {
		return this.ldt(this.keyOf(idx), defaultValue);
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 * 
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return key 所标定的 值
	 */
	default LocalDateTime ldt(final String key, final LocalDateTime defaultValue) {
		final var value = this.get(key);

		if (value == null) { // 空结构
			return defaultValue;
		} else { // 非空值
			final var ldt = IRecord.asLocalDateTime(value);
			return Optional.ofNullable(ldt).orElse(defaultValue);
		} // if
	}

	/**
	 * 返回 建明索引 所对应的 键值, Integer 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Integer> i4Opt(final int idx) {
		return Optional.of(this.i4(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Integer> i4Opt(final String key) {
		return Optional.of(this.i4(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, Long 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Long> lngOpt(final int idx) {
		return Optional.of(this.lng(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Long 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Long> lngOpt(final String key) {
		return Optional.of(this.lng(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, Double 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<Double> dblOpt(final int idx) {
		return Optional.of(this.dbl(idx));
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<Double> dblOpt(final String key) {
		return Optional.of(this.dbl(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, LocalDateTime 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<LocalDateTime> ldtOpt(final int idx) {
		return Optional.of(this.ldt(idx));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDateTime 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<LocalDateTime> ldtOpt(final String key) {
		return Optional.of(this.ldt(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, LocalDate 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<LocalDate> ldOpt(final int idx) {
		return Optional.of(this.ld(idx));
	}

	/**
	 * 返回 key 所对应的 键值, LocalDate 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<LocalDate> ldOpt(final String key) {
		return Optional.of(this.ld(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, LocalTime 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<LocalDate> ltOpt(final int idx) {
		return Optional.of(this.ld(idx));
	}

	/**
	 * 返回 key 所对应的 键值, LocalTime 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值 Optional
	 */
	default Optional<LocalDate> ltOpt(final String key) {
		return Optional.of(this.ld(key));
	}

	/**
	 * 返回 建明索引 所对应的 键值, String 类型
	 * 
	 * @param idx 键名索引 从0开始
	 * @return idx 所标定的 值 Optional
	 */
	default Optional<String> strOpt(final int idx) {
		return this.opt(idx).map(Object::toString);
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
	 * 提取record类型的结果 <br>
	 * 
	 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
	 * 
	 * @param idx 键名索引 从 0开始
	 * @return IRecord类型的值
	 */
	default IRecord rec(final int idx) {
		return this.rec(this.keyOf(idx));
	}

	/**
	 * 提取record类型的结果
	 * 
	 * @param key 键名
	 * @return IRecord类型的值
	 */
	default IRecord rec(final String key) {
		return this.rec(key, (k, e) -> e);
	}

	/**
	 * 提取record类型的结果 <br>
	 * 
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
		final var value = preprocessor.apply(key, (X) this.get(key));
		final Function<Collection<?>, IRecord> clcn2rec = tt -> {
			int i = 0;
			final var rec = this.build(); // 创建一个空IRecord
			for (final var t : tt) {
				rec.add("" + (i++), t);
			} // for
			return rec;
		}; // clc2rec

		if (value instanceof IRecord) {
			return (IRecord) value;
		} else if (value instanceof Map) {
			return this.build((Map<?, ?>) value);
		} else if (value instanceof Collection || (value != null && value.getClass().isArray())) {
			final var tt = value instanceof Collection // 值类型判断
					? (Collection<Object>) value // 集合类型
					: (Collection<Object>) Arrays.asList((Object[]) value); // 数组类型
			return clcn2rec.apply(tt);
		} else if (value instanceof Iterable) {
			return clcn2rec.apply(IRecord.itr2list((Iterable<?>) value));
		} else {
			return null;
		}
	}

	/**
	 * 提取record类型的结果 <br>
	 * 
	 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
	 * 
	 * @param <X>          源数据类型
	 * @param <Y>          目标数据类型,即 IRecord,Map,Collection,Array 其中之一
	 * @param idx          键名索引 从0开始
	 * @param preprocessor 预处理器
	 * @return IRecord类型的值
	 */
	default <X, Y> IRecord rec(final int idx, final BiFunction<String, X, Y> preprocessor) {
		return this.rec(this.keyOf(idx), preprocessor);
	}

	/**
	 * 提取record类型的结果 <br>
	 * 
	 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
	 * 
	 * @param <X>          源数据类型
	 * @param <Y>          目标数据类型,即 IRecord,Map,Collection,Array 其中之一
	 * @param key          键名
	 * @param preprocessor 预处理器
	 * @return IRecord类型的值
	 */
	@SuppressWarnings("unchecked")
	default <X, Y> IRecord rec(final String key, final Function<X, Y> preprocessor) {
		return this.rec(key, (k, v) -> preprocessor.apply((X) v));
	}

	/**
	 * 提取record类型的结果 <br>
	 * 
	 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
	 * 
	 * @param <X>          源数据类型
	 * @param <Y>          目标数据类型,即 IRecord,Map,Collection,Array 其中之一
	 * @param idx          键名索引 从0开始
	 * @param preprocessor 预处理器
	 * @return IRecord类型的值
	 */
	@SuppressWarnings("unchecked")
	default <X, Y> IRecord rec(final int idx, final Function<X, Y> preprocessor) {
		return this.rec(this.keyOf(idx), (k, v) -> preprocessor.apply((X) v));
	}

	/**
	 * 把 键名元素的值转换为 列表结构 <br>
	 * lla 是 LinkedList apply 的含义，取意为 应用方法 获得 链表
	 * 
	 * @param <T>    源数据类型
	 * @param <U>    目标列表元素的数据类型
	 * @param key    键名
	 * @param lister 列表构建器 t->[u]
	 * 
	 * @return 列表结构的数据,U 类型的列表 [u]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> List<U> lla(final String key, final Function<T, List<U>> lister) {
		final var value = this.get(key); // 提取数据值

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
	 * 
	 * @return 列表结构的数据,U 类型的列表 [u]
	 */
	default <T, U> List<Object> lla(final int idx, final Function<T, List<U>> lister) {
		return this.lla(this.keyOf(idx));
	}

	/**
	 * 把 键名元素的值转换为 列表结构 <br>
	 * lla 是 LinkedList apply 的含义，取意为 应用方法 获得 链表
	 * 
	 * @param key 键名
	 * 
	 * @return 列表结构的数据
	 */
	default List<Object> lla(final String key) {
		return this.lla(key, IRecord::asList);
	}

	/**
	 * 把 键名索引 元素的值转换为 列表结构
	 * 
	 * @param idx 键名索引
	 * 
	 * @return 列表结构的数据
	 */
	default List<Object> lla(final int idx) {
		return this.lla(this.keyOf(idx));
	}

	/**
	 * 把 键名元素的值转换为 元素对象流 <br>
	 * llS 是 LinkedList Stream 的 缩写 根据 lla 的变体,S 表示这是一个 返回 Stream类型的函数
	 * 
	 * @param <T>      源数据类型
	 * @param <U>      目标列表元素的数据类型
	 * @param key      键名
	 * @param streamer 元素对象流构建器 t->[u]
	 * 
	 * @return U 类型的元素对象流 [u]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> llS(final String key, final Function<T, Stream<U>> streamer) {
		final var value = this.get(key); // 提取数据值

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
	 * 
	 * @return U 类型的元素对象流 [u]
	 */
	default <T, U> Stream<U> llS(final int idx, final Function<T, Stream<U>> streamer) {
		return this.llS(this.keyOf(idx), streamer);
	}

	/**
	 * 把 键名 元素的值转换为 元素对象流
	 * 
	 * llS 是 LinkedList Stream 的 缩写 根据 lla 的变体,S 表示这是一个 返回 Stream类型的函数
	 * 
	 * @param key 键名
	 * 
	 * @return 元素对象流
	 */
	default Stream<Object> llS(final String key) {
		return this.llS(key, e -> Optional.ofNullable(e).map(IRecord::asList).map(List::stream).orElse(null));
	}

	/**
	 * 把 键名索引 元素的值转换为 元素对象流
	 * 
	 * @param idx 键名索引
	 * 
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
	 * 根据路径提取数据
	 * 
	 * @param <T>    键值变换函数的源类型
	 * @param <U>    键值变换函数的目标类型
	 * @param path   键名路径 如 a/b/c
	 * @param lister 列表构建器 t->[u]
	 * @return U类型的元素对象流
	 */
	default <T, U> List<U> pathlla(final String path, final Function<T, List<U>> lister) {
		return this.pathget(path, lister);
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return 对象列表
	 */
	default List<Object> pathlla(final String path) {
		return this.pathget(path, IRecord::asList);
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
	 * @param path 键名路径 如 a/b/c
	 * @return 元素对象流,路径不存在或是值为null的时候返回null值
	 */
	default Stream<Object> pathllS(final String path) {
		return this.pathllS(path, e -> Optional.ofNullable(e).map(IRecord::asList).map(List::stream).orElse(null));
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return Boolean 类型的值, 0 被视为 false,非零被视为 true, null视为 false, 非 null 被视为 true
	 */
	default Boolean pathbool(final String path) {
		final var bool = this.pathget(path.split("[/,]+"), e -> {
			if (e instanceof Boolean) {
				return (Boolean) e;
			} else if (e instanceof Number) {
				return ((Number) e).intValue() == 0;
			} else {
				return e == null;
			}
		});
		return Optional.ofNullable(bool).orElse(null);
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return LocalDateTime 类型的值
	 */
	default LocalDateTime pathldt(final String path) {

		final var ldt = this.pathget(path.split("[/,]+"), IRecord::asLocalDateTime);

		return ldt;
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return int 类型的值
	 */
	default Integer pathint(final String path) {
		final var dbl = this.pathget(path.split("[/,]+"), IRecord.obj2dbl());
		return Optional.ofNullable(dbl).map(Number::intValue).orElse(null);
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return long 类型的值
	 */
	default Long pathlng(final String path) {
		final var dbl = this.pathget(path.split("[/,]+"), IRecord.obj2dbl());
		return Optional.ofNullable(dbl).map(Number::longValue).orElse(null);
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return double 类型的值
	 */
	default Double pathdbl(final String path) {
		return this.pathget(path.split("[/,]+"), IRecord.obj2dbl());
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param path 键名路径 如 a/b/c
	 * @return double 字符串类型
	 */
	default String pathstr(final String path) {
		return this.pathget(path.split("[/,]+"), e -> e + "");
	}

	/**
	 * 根据路径提取数据
	 * 
	 * @param <T>  元素类型
	 * @param <U>  结果类型
	 * @param path 键名路径 如 a/b/c
	 * @return U类型的值,强制类型转化，强制转换成U类型
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U pathget(final String path) {
		return this.pathget(path, (k, e) -> e, e -> (U) e);
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
	 * 根据路径提取数据 <br>
	 * 
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
	 * @param path   键名路径
	 * @param mapper 值变换函数 t->u
	 * @return U类型的值
	 */
	default <T, U> U pathget(final String[] path, final Function<T, U> mapper) {
		return this.pathget(path, (k, e) -> e, mapper);
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
		final var key = path[0].strip();
		final var len = path.length;

		if (len > 1) {
			final var _rec = this.rec(key, preprocessor);
			final var _path = Arrays.copyOfRange(path, 1, len);
			return _rec != null ? _rec.pathget(_path, preprocessor, mapper) : null;
		} else {
			return mapper.apply((T) this.get(key));
		} // if
	}

	/**
	 * 缺值计算
	 * 
	 * @param <T>    值类型
	 * @param key    键名
	 * @param mapper 缺省值计算函数 (key)->t
	 * @return 缺值计算的值，并把缺少值补充到当前的IRecord之中
	 */
	@SuppressWarnings("unchecked")
	default <T> T computeIfAbsent(final String key, final Function<String, T> mapper) {
		final var v = this.get(key);
		if (v == null) {
			final var t = mapper.apply(key);
			this.set(key, t); // 设置当前的值
			return t;
		} else {
			return (T) v;
		}
	}

	/**
	 * 键值计算，若是 键值不存在 则 用defaultValue更新
	 * 
	 * @param <T>          键值类型
	 * @param key          键名
	 * @param defaultValue 默认值
	 * @return T 类型的键值
	 */
	default <T> T computeIfAbsent(final String key, final T defaultValue) {
		return this.computeIfAbsent(key, (k) -> defaultValue);
	}

	/**
	 * 键值索引计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param idx    键名索引从0开始
	 * @param mapper 值计算函数 (key,x)->t
	 * @return 计算后的新值
	 */
	default <X, T> T compute(final Integer idx, final BiFunction<String, X, T> mapper) {
		return this.compute(this.keyOf(idx), mapper);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号','分隔,最后一个键名用于最后接收计算值的键,比如x,y表示y是x的函数,其值由x决定。 <br>
	 *               rec.compute("x,y",mapper) 相当于
	 *               rec.set("y",mapper.apply(rec.get("x")))
	 * @param mapper 值计算函数 (key,x)->t
	 * @return 计算后的新值，当keys的元素个数小于1的时候，返回 null,<br>
	 *         等于0表示用rec.set("x",mapper.apply(rec.get("x"))),即 自己更新自己的update
	 */
	@SuppressWarnings("unchecked")
	default <X, T> T compute(final String keys, final BiFunction<String, X, T> mapper) {
		final var kk = keys.matches("^\\s*[,]+\\s*$") ? new String[] { "," }
				: Arrays.stream(keys.split("[,]+")).map(String::strip).toArray(String[]::new);
		final var n = kk.length; // 健名数组长度
		if (n < 1) { // 健名数量不足
			return null;
		} else { // 健名数量有效
			final var dest_key = kk[n - 1]; // 目标键
			final var v = (X) this.get(kk[0]); // 提取键值
			final var source_value = mapper.apply(kk[0], v); // 键值映射,原值
			this.set(dest_key, source_value); // 把源值绑定到目标值
			return (T) source_value;
		}
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示 y是x的函数,其值由x决定。,
	 * @param mapper 值计算函数 (x)->t
	 * @return 计算后的新值
	 */
	@SuppressWarnings("unchecked")
	default <X, T> T compute(final String keys, final Function<X, T> mapper) {
		final var kk = Arrays.stream(keys.split("[,]+")).map(String::strip).toArray(String[]::new);
		final var _key = kk[kk.length - 1];
		final var v = (X) this.get(kk[0]); // 提取键值
		final var _t = mapper.apply(v); // 键值映射
		this.set(_key, _t);
		return (T) _t;
	}

	/**
	 * 建索引计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param idx    键名索引 从 0开始
	 * @param mapper 值计算函数 (x)->t
	 * @param xclass 类型参照类的类名
	 * @return 计算后的新值
	 */
	default <X, T> IRecord compute(final Integer idx, final Function<X, T> mapper, final Class<X> xclass) {
		return this.compute(this.keyOf(idx), mapper, xclass);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示 y是x的函数,其值由x决定。,
	 * @param mapper 值计算函数 (x)->t
	 * @param xclass 类型参照类的类名
	 * @return 计算后的新值
	 */
	default <X, T> IRecord compute(final String keys, final Function<X, T> mapper, final Class<X> xclass) {
		final var kk = Arrays.stream(keys.split("[,]+")).map(String::strip).toArray(String[]::new);
		final var _key = kk[kk.length - 1];
		this.set(_key, this.get(kk[0], mapper, xclass));
		return this;
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <X>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <Y>    原值类型,Boolean,Integer,Long,Double,Short,LocalDate,LocalTime,LocalDateTime,String,IRecord,Object
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @param xclass 类型参照类的类名
	 * @return 计算后的新值
	 */
	default <X, Y, T> IRecord compute(final String keys, final BiFunction<X, Y, T> mapper, final Class<X> xclass,
			final Class<Y> yclass) {
		final var kk = Arrays.stream(keys.split("[,]+")).map(String::strip).toArray(String[]::new);
		if (kk.length >= 2) {
			final var x = this.get(kk[0], (Function<X, X>) e -> e, xclass);
			final var y = this.get(kk[1], (Function<Y, Y>) e -> e, yclass);
			final var t = mapper.apply(x, y);
			final var key = kk.length > 2 ? kk[2] : kk[1];
			this.set(key, t);
		}
		return this;
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示 y是x的函数,其值由x决定。,
	 * @param mapper 值计算函数 (x)->t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLt(final String keys, final Function<LocalTime, T> mapper) {
		return this.compute(keys, mapper, LocalTime.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLts(final String keys, final BiFunction<LocalTime, LocalTime, T> mapper) {
		return this.compute(keys, mapper, LocalTime.class, LocalTime.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示 y是x的函数,其值由x决定。,
	 * @param mapper 值计算函数 (x)->t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLd(final String keys, final Function<LocalDate, T> mapper) {
		return this.compute(keys, mapper, LocalDate.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLds(final String keys, final BiFunction<LocalDate, LocalDate, T> mapper) {
		return this.compute(keys, mapper, LocalDate.class, LocalDate.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示 y是x的函数,其值由x决定。
	 * @param mapper 值计算函数 (x)->t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLdt(final String keys, final Function<LocalDateTime, T> mapper) {
		return this.compute(keys, mapper, LocalDateTime.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLdts(final String keys, final BiFunction<LocalDateTime, LocalDateTime, T> mapper) {
		return this.compute(keys, mapper, LocalDateTime.class, LocalDateTime.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示 y是x的函数,其值由x决定。
	 * @param mapper 值计算函数 (x)->t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeInt(final String keys, final Function<Integer, T> mapper) {
		return this.compute(keys, mapper, Integer.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeInts(final String keys, final BiFunction<Integer, Integer, T> mapper) {
		return this.compute(keys, mapper, Integer.class, Integer.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名
	 * @param mapper 值计算函数 (x)->t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLng(final String keys, final Function<Long, T> mapper) {
		return this.compute(keys, mapper, Long.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeLngs(final String keys, final BiFunction<Long, Long, T> mapper) {
		return this.compute(keys, mapper, Long.class, Long.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeDbl(final String keys, final Function<Double, T> mapper) {
		return this.compute(keys, mapper, Double.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeDbls(final String keys, final BiFunction<Double, Double, T> mapper) {
		return this.compute(keys, mapper, Double.class, Double.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeStr(final String keys, final Function<String, T> mapper) {
		return this.compute(keys, mapper, String.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeStrs(final String keys, final BiFunction<String, String, T> mapper) {
		return this.compute(keys, mapper, String.class, String.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeIntDbl(final String keys, final BiFunction<Integer, Double, T> mapper) {
		return this.compute(keys, mapper, Integer.class, Double.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeIntLt(final String keys, final BiFunction<Integer, LocalTime, T> mapper) {
		return this.compute(keys, mapper, Integer.class, LocalTime.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeIntLd(final String keys, final BiFunction<Integer, LocalDate, T> mapper) {
		return this.compute(keys, mapper, Integer.class, LocalDate.class);
	}

	/**
	 * 键值计算，并用计算结果更新 key的值 <br>
	 * 如果键不存在 则用 新值给予 设置 否则 给予 替换。
	 * 
	 * @param <T>    新值类型
	 * @param keys   键名,键名列表用逗号分隔,最后一个键名用于最后接收计算值的键,，比如x,y 表示
	 *               y是x的函数,其值由x决定。,又比如x,y,z 相当于 set("z",t) ,其中 t 是 x,y 的计算结果,t =
	 *               mapper.apply(x,y);
	 * @param mapper 值计算函数 (x,y)-&gt;t
	 * @return 计算后的新值
	 */
	default <T> IRecord computeIntLdt(final String keys, final BiFunction<Integer, LocalDateTime, T> mapper) {
		return this.compute(keys, mapper, Integer.class, LocalDateTime.class);
	}

	/**
	 * 转换成 tuple2 的 列表
	 * 
	 * @return tuple的流结构 [(k,v)]
	 */
	default List<Tuple2<String, Object>> tuples() {
		return this.dataS().collect(Collectors.toList());
	}

	/**
	 * 转换成 tuple2 的 流式结构
	 * 
	 * @return tuple的流结构 [(k,v)]
	 */
	default Stream<Tuple2<String, Object>> tupleS() {
		return this.dataS();
	}

	/**
	 * 转换成 tuple2 的 流式结构
	 * 
	 * @param <T>          value_mapper 的参数类型
	 * @param <U>          value_mapper 的结果类型
	 * @param value_mapper 值变换函数 t->u
	 * @return tuple的流结构 [(k,t)]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<Tuple2<String, U>> tupleS(final Function<T, U> value_mapper) {
		return this.stream(tup -> P(tup._1, value_mapper.apply((T) tup._2)));
	}

	/**
	 * 转换成流式结构
	 * 
	 * @param <T>         tup2_mapper 的二号元素类型
	 * @param <U>         tup2_mapper 的结果类型
	 * @param tup2_mapper tuple2变换函数 t->u
	 * @return U 类型的数据流 [(k,t)]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> stream(final BiFunction<String, T, U> tup2_mapper) {
		return this.stream(tup -> tup2_mapper.apply(tup._1(), (T) tup._2()));
	}

	/**
	 * 转换成流式结构
	 * 
	 * @param <T>         tup2_mapper 的二号元素类型
	 * @param <U>         tup2_mapper 的结果类型
	 * @param tup2_mapper tuple2变换函数 (k:String,t:T)->u:U
	 * @return V 类型的数据流 [(k,t)]
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> stream(Function<Tuple2<String, T>, U> tup2_mapper) {
		return this.dataS().map(tup -> tup2_mapper.apply((Tuple2<String, T>) tup));
	}

	/**
	 * Tests whether a predicate holds for all elements of this sequence.
	 * 
	 * 
	 * @param <T>
	 * @param p   the predicate used to test elements.
	 * @return true if this sequence is empty or the given predicate p holds for all
	 *         elements of this sequence, otherwise false.
	 */
	@SuppressWarnings("unchecked")
	default <T> boolean forall(final BiPredicate<String, T> p) {
		boolean ret = false;
		try {
			ret = this.tupleS().allMatch(tup -> p.test(tup._1, (T) tup._2));
		} catch (Exception e) {
			// do nothing
		}
		return ret;
	}

	/**
	 * IRecord 数据的 结构 变换 <br>
	 * 
	 * 把 IRecord 转换 另一种结构的 数据 <br>
	 * 
	 * @param <T>    目标类型结构
	 * @param mapper 结构变换函数 rec-&gt;T
	 * @return T 类型的结构
	 */
	default <T> T mutate(final Function<IRecord, T> mapper) {
		return mapper.apply(this);
	}

	/**
	 * IRecord 数据的 结构 变换 <br>
	 * 
	 * 把 IRecord 转换 另一种结构的 数据 <br>
	 * 
	 * @param <T>    目标类型结构
	 * @param mapper 结构变换函数 {(key,value):toMap递归处理后的结果}-&gt;T
	 * @return T 类型的结构
	 */
	default <T> T mutate2(final Function<? super Map<String, Object>, T> mapper) {
		final var map = this.toMap(true);// 递归处理
		return mapper.apply(map);
	}

	/**
	 * 根据指定的键表对IRecord的数据进行拆分
	 * 
	 * @param keysList 键表序列,比如: "a,b,c;d,e,g",键表中的键名用逗号","分隔,键表之间 用 分号";"分隔
	 * @return 拆分出的子IRecord 流
	 */
	default Stream<IRecord> splitS(final String keysList) {
		return this.splitS(keysList.split("[;]+"));
	}

	/**
	 * 根据指定的键表对IRecord的数据进行拆分
	 * 
	 * @param keysList 键表序列,比如: ["a,b,c","d,e,g"],键表中的键名用逗号分隔
	 * @return 拆分出的子IRecord 流
	 */
	default Stream<IRecord> splitS(final String... keysList) {
		return Stream.of(keysList).map(this::filter);
	}

	/**
	 * 根据指定的键表对IRecord的数据进行拆分
	 * 
	 * @param keysList 键表序列, 比如:[["a","b","c"],["d","e","f"]]
	 * @return 拆分出的子IRecord 流
	 */
	default Stream<IRecord> splitS(final String[]... keysList) {
		return Stream.of(keysList).map(this::filter);
	}

	/**
	 * 根据指定的键表对IRecord的数据进行拆分
	 * 
	 * @param keysList 键表序列,比如: "a,b,c;d,e,g",键表中的键名用逗号","分隔,键表之间 用 分号";"分隔
	 * @return 拆分出的子IRecord 列表
	 */
	default List<IRecord> splits(final String keysList) {
		return this.splits(keysList.split("[;]+"));
	}

	/**
	 * 根据指定的键表对IRecord的数据进行拆分
	 * 
	 * @param keysList 键表序列,比如: ["a,b,c","d,e,g"],键表中的键名用逗号分隔
	 * @return 拆分出的子IRecord 列表
	 */
	default List<IRecord> splits(final String... keysList) {
		return this.splitS(keysList).collect(Collectors.toList());
	}

	/**
	 * 根据指定的键表对IRecord的数据进行拆分
	 * 
	 * @param keysList 键表序列, 比如:[["a","b","c"],["d","e","f"]]
	 * @return 拆分出的子IRecord 列表
	 */
	default List<IRecord> splits(final String[]... keysList) {
		return this.splitS(keysList).collect(Collectors.toList());
	}

	/**
	 * 数据滑动
	 * 
	 * @param size 窗口大小
	 * @param step 步长
	 * @param flag 是否返回等长记录 true 数据等长 剔除 尾部的 不齐整（小于 size） 的元素,false 包含不齐整
	 * @return 子IRecord的流 [rec]
	 */
	@Override
	default Stream<IRecord> slidingS(final int size, final int step, final boolean flag) {
		return IRecord.slidingS(this.tupleS().collect(Collectors.toList()), size, step, flag).map(tups -> {
			return tups.stream().collect(IRecord.recclc(e -> e));
		});
	}

	/**
	 * 数据滑动(齐整模式)
	 * 
	 * @param size 窗口大小
	 * @param step 步长
	 * @return 子IRecord的流 [rec]
	 */
	default Stream<IRecord> slidingS(final int size, final int step) {
		return this.slidingS(size, step, true);
	}

	/**
	 * 数据滑动
	 * 
	 * @param size 窗口大小
	 * @param step 步长
	 * @param flag 是否返回等长记录 true 数据等长 剔除 尾部的 不齐整（小于 size） 的元素,false 包含不齐整
	 * @return 子IRecord的列表 [rec]
	 */
	default List<IRecord> slidings(final int size, final int step, final boolean flag) {
		return this.slidingS(size, step, flag).collect(Collectors.toList());
	}

	/**
	 * 数据滑动(齐整模式)
	 * 
	 * @param size 窗口大小
	 * @param step 步长
	 * @return 子IRecord的列表 [rec]
	 */
	@Override
	default List<IRecord> slidings(final int size, final int step) {
		return this.slidings(size, step, true);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
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
		final var oo = this.toArray();
		U u = null;
		try {
			u = mapper.apply((T[]) oo);
		} catch (Exception e) {
			// do nothing
		} // try

		return u;
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * 使用示例：<br>
	 * IRecord.rb("name,birth,marry") // 档案结构 <br>
	 * .get("zhangsan,19810713,20011023".split(",")) // 构建张三的数据记录 <br>
	 * .arrayOf("birth,marry",IRecord::asLocalDate, // 把 出生日期和结婚日期转换为日期类型 <br>
	 * ldts->ldts[0].until(ldts[1]).getYears()); // 计算张三的结婚年龄 <br>
	 * 
	 * @param <X>         x2t_mapper 的原数据类型
	 * @param <T>         tt2u_mapper 的原数据类型
	 * @param <U>         tt2u_mapper 目标元素的类型
	 * @param x2t_mapper  x->t 元素类型变换函数
	 * @param tt2u_mapper [t]->u 数组变换函数
	 * @return U类型结果
	 */
	default <X, T, U> U arrayOf(final Function<X, T> x2t_mapper, final Function<T[], U> tt2u_mapper) {
		final var tt = this.toArray(x2t_mapper);
		return tt2u_mapper.apply(tt);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * 使用示例：<br>
	 * IRecord.rb("name,birth,marry") // 档案结构 <br>
	 * .get("zhangsan,19810713,20011023".split(",")) // 构建张三的数据记录 <br>
	 * .arrayOf("birth,marry",IRecord::asLocalDate, // 把 出生日期和结婚日期转换为日期类型 <br>
	 * ldts->ldts[0].until(ldts[1]).getYears()); // 计算张三的结婚年龄 <br>
	 * 
	 * @param <X>         x2t_mapper 的原数据类型
	 * @param <T>         tt2u_mapper 的原数据类型
	 * @param <U>         tt2u_mapper 目标元素的类型
	 * @param keys        提取的键名列表，键名之间使用逗号，半角","或全角"，"都可以。
	 * @param x2t_mapper  x->t 元素类型变换函数
	 * @param tt2u_mapper [t]->u 数组变换函数
	 * @return U类型结果
	 */
	default <X, T, U> U arrayOf(final String keys, final Function<X, T> x2t_mapper,
			final Function<T[], U> tt2u_mapper) {
		return this.arrayOf(keys.split("[,，]+"), x2t_mapper, tt2u_mapper);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * 使用示例：<br>
	 * IRecord.rb("name,birth,marry") // 档案结构 <br>
	 * .get("zhangsan,19810713,20011023".split(",")) // 构建张三的数据记录 <br>
	 * .arrayOf("birth,marry",IRecord::asLocalDate, // 把 出生日期和结婚日期转换为日期类型 <br>
	 * ldts->ldts[0].until(ldts[1]).getYears()); // 计算张三的结婚年龄 <br>
	 * 
	 * @param <X>         x2t_mapper 的原数据类型
	 * @param <T>         tt2u_mapper 的原数据类型
	 * @param <U>         tt2u_mapper 目标元素的类型
	 * @param keys        提取的键名列表
	 * @param x2t_mapper  x->t 元素类型变换函数
	 * @param tt2u_mapper [t]->u 数组变换函数
	 * @return U类型结果
	 */
	default <X, T, U> U arrayOf(final String[] keys, final Function<X, T> x2t_mapper,
			final Function<T[], U> tt2u_mapper) {
		final var tt = this.filter(keys).toArray(x2t_mapper);
		return tt2u_mapper.apply(tt);
	}

	/**
	 * 把 IRecord 值转换成 Object 一维数组
	 * 
	 * @return Object 一维数组
	 */
	default Object[] toArray() {
		return this.toArray(e -> e);
	}

	/**
	 * 把 IRecord 值转换成 数组结构
	 * 
	 * @param <X>    mapper 参数类型
	 * @param <T>    mapper 值类型
	 * @param mapper 值的变换函数 x->t
	 * @return T类型的一维数组
	 */
	@SuppressWarnings("unchecked")
	default <X, T> T[] toArray(final Function<X, T> mapper) {
		final var classes = this.tupleS().map(x -> mapper.apply((X) x._2)).filter(Objects::nonNull)
				.map(e -> (Class<Object>) e.getClass()).distinct().collect(Collectors.toList());
		final var clazz = classes.size() > 1 // 类型不唯一
				? classes.stream().allMatch(e -> Number.class.isAssignableFrom(e)) // 数字类型
						? Number.class // 数字类型
						: Object.class // 节点类型
				: classes.get(0); // 类型唯一
		return this.tupleS().map(x -> mapper.apply((X) x._2)).toArray(n -> (T[]) Array.newInstance(clazz, n));
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
		final var rec = this.duplicate(); // 复制品
		IRecord.slidingS(Arrays.asList(tups), 2, 2, true).forEach(aa -> {
			rec.add(aa.get(0).toString(), aa.get(1));
		}); // forEach
		return rec;
	}

	/**
	 * 是否key的键名的值有效
	 * 
	 * @param key 键名
	 */
	default boolean contains(final String key) {
		return this.get(key) != null;
	}

	/**
	 * 肯定过滤
	 * 
	 * @param idices 保留的键名索引序列，键名索引从0开始
	 * @return 新生的IRecord
	 */
	default IRecord filter(final Integer... idices) {
		final var kk = this.keys();
		final int n = kk.size();
		final var rec = this.build();

		Arrays.stream(idices).filter(i -> i >= 0 && i < n).map(kk::get).forEach(key -> {
			rec.add(key, this.get(key));
		});

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
		final var rec = this.build();

		keys.forEach(key -> {
			final var _key = key.strip();
			final var value = this.get(_key);
			if (value != null) {
				rec.add(key, value);
			}
		});

		return rec;
	}

	/**
	 * 否定过滤
	 * 
	 * @param idices 剔除的键名索引序列，键名索引从0开始
	 * @return 新生的IRecord
	 */
	default IRecord filterNot(final Integer... idices) {
		final var n = this.size();
		final var ids = Arrays.asList(idices);
		final var _indices = Stream.iterate(0, i -> i < n, i -> i + 1).filter(i -> !ids.contains(i))
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
	 * 键名过滤
	 * 
	 * @param bipredicate 过滤函数 (key,value)->bool, true 值保留, false 剔除
	 * @return 新生的IRecord
	 */
	default IRecord filter(final BiPredicate<String, Object> bipredicate) {
		final var rec = this.build(); // 空IRecord
		this.tupleS().filter(tup -> bipredicate.test(tup._1, tup._2)).forEach(rec::add);
		return rec;
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
	 *         {@code false} otherwise.
	 * @see java.util.HashMap
	 */
	default boolean equals(final IRecord rec) {
		return this == rec || this.compareTo(rec) == 0;
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
			final var hashSet = new LinkedHashSet<>(this.keys());
			hashSet.addAll(o.keys());// 归并
			final var kk = hashSet.stream().sorted().toArray(String[]::new);
			System.err.println(
					"比较的两个键名序列(" + this.keys() + "," + o.keys() + ")不相同,采用归并键名序列进行比较:" + Arrays.deepToString(kk));
			return IRecord.cmp(kk, true).compare(this, o);
		} // if
	}

	/**
	 * 把idx转key
	 * 
	 * @param idx 键名索引
	 * @return 索引转键名
	 */
	default String keyOf(final int idx) {

		final var kk = this.keys();
		return idx < kk.size() ? kk.get(idx) : null;
	}

	/**
	 * 键名索引
	 * 
	 * @param key 键名
	 * @return 键名索引 从0开始
	 */
	default Integer indexOf(final String key) {

		final var kk = this.keys();

		for (int i = 0; i < kk.size(); i++) {
			final var _key = kk.get(i);
			if (key.equals(_key))
				return i;
		}

		return null;
	}

	/**
	 * 转化成字符串
	 * 
	 * 值的格式化方法为：尝试转换为数字，若成功数字格式 否则 字符串格式,字符串格式 用 引号 " 括起来,数字格式 直接显示
	 * 
	 * @param preprocessor  值的预处理器，若返回 null，则在最终结果中给予过滤。
	 * @param e_delimiter   元素间分隔符
	 * @param e_prefix      元素前缀
	 * @param e_suffix      元素结尾
	 * @param ee_delimiter  集合元素间分隔符
	 * @param ee_prefix     集合元素前缀
	 * @param ee_suffix     集合元素后缀
	 * @param kvp_delimiter key-value之间的分隔
	 * @param key_prefix    key 前缀
	 * @param key_suffix    key 后缀
	 * @return 文本化描述
	 */
	default String toString(final Function<Object, ?> preprocessor, final String e_delimiter, final String e_prefix,
			final String e_suffix, final String ee_delimiter, final String ee_prefix, final String ee_suffix,
			final String kvp_delimiter, final String key_prefix, final String key_suffix) {

		return this.toString(preprocessor, e_delimiter, e_prefix, e_suffix, ee_delimiter, ee_prefix, ee_suffix,
				kvp_delimiter, key_prefix, key_suffix, value -> { // 默认的值的格式化

					final var _v = IRecord.obj2dbl(null, false).apply(value); // 关闭掉时间转换成数字的能力
					return FT(_v == null ? "\"$0\"" : "$0", value);
				});
	}

	/**
	 * 转换成json 字符串
	 * 
	 * @return json 字符串
	 */
	default String json() {
		return this.json(null); // json 字符串
	}

	/**
	 * 转换成json 字符串
	 * 
	 * @param mapper 值变换函数,当mapper为null 默认把 Map 结构给予IRecord化.
	 * @return json 字符串
	 */
	default String json(final Function<Object, Object> mapper) {
		final Function<Object, Object> _mapper = mapper == null // 没有提供预处理函数 于是 按照默认规则来进行处理
				? e -> e instanceof Map // map 类型的结构
						? this.build(e) // 转黄IRecord
						: e instanceof String // 字符串类型的值
								? e.toString().replace("\"", "\\\"") // 把双引号进行转义
								: e // 保持不变
				: mapper; // 使用默认的值变换函数
		return this.toString(_mapper, ",", "{", "}", ",", "[", "]", ":", "\"", "\"");
	}

	/**
	 * 返回 当前keys 结构构造的器
	 * 
	 * @return IRecord.Builder
	 */
	default IRecord.Builder builder() {
		return IRecord.rb(this.keys());
	}

	/**
	 * 构建IRecord <br>
	 * Record Builder Get 的简称
	 * 
	 * @param <T>  元素类型
	 * @param objs 值元素列表
	 * @return IRecord
	 */
	@SuppressWarnings("unchecked")
	default <T> IRecord rbget(final T... objs) {
		return this.builder().get(objs);
	}

	/**
	 * 加法
	 * 
	 * @param other 另一个 IRecord
	 * @return 和 IRecord
	 */
	default IRecord plus(final IRecord other) {
		return IRecord.plus().apply(this, other);
	}

	/**
	 * 加法
	 * 
	 * @param number 另一个 加数
	 * @return 和 IRecord
	 */
	default IRecord plus(final Number number) {
		return this.plus(this.rbget(number));
	}

	/**
	 * 减法
	 * 
	 * @param other 另一个 IRecord
	 * @return 差 IRecord
	 */
	default IRecord subtract(final IRecord other) {
		return IRecord.subtract().apply(this, other);
	}

	/**
	 * 减法
	 * 
	 * @param number 减数
	 * @return 差 IRecord
	 */
	default IRecord subtract(final Number number) {
		return this.subtract(this.rbget(number));
	}

	/**
	 * 乘法
	 * 
	 * @param other 另一个 IRecord
	 * @return 积 IRecord
	 */
	default IRecord multiply(final IRecord other) {
		return IRecord.multiply().apply(this, other);
	}

	/**
	 * 乘法
	 * 
	 * @param number 另外一个乘数
	 * @return 积 IRecord
	 */
	default IRecord multiply(final Number number) {
		return this.multiply(this.rbget(number));
	}

	/**
	 * 除法
	 * 
	 * @param other 另一个 IRecord
	 * @return 商 IRecord
	 */
	default IRecord divide(final IRecord other) {
		return IRecord.divide().apply(this, other);
	}

	/**
	 * 除法
	 * 
	 * @param number 除数
	 * @return 商 IRecord
	 */
	default IRecord divide(final Number number) {
		return this.divide(this.rbget(number));
	}

	/**
	 * 树形遍历
	 * 
	 * @param action 回调函数( level:层级从0开始时且0标识根节点层级,node:(path:从根节点开始,value:节点值))
	 *               -&gt;{}
	 */
	default void forEach(final BiConsumer<Integer, Tuple2<String, Object>> action) {
		IRecord.forEach(this, action);
	}

	/**
	 * Converts this iterable collection of pairs into two collections of the first
	 * and second half of each pair.
	 * 
	 * @return a pair of iterable collections, containing the first, respectively
	 *         second half of each element pair of this iterable collection.
	 */
	default Tuple2<List<String>, List<Object>> unzip() {
		return this.unzip(e -> e);
	}

	/**
	 * 对键值对儿进行排序序列进行排序(升顺序,先比较第一元素键名,然后比较键值)
	 * 
	 * @return 排序后的减值对儿组成的IRecord
	 */
	default IRecord sorted() {
		return this.sorted(Tuple2.defaultComparator());
	}

	/**
	 * 返回一个 Map 结构
	 * 
	 * @param flag 是否进行递归处理,true 递归处理 , false 非递归处理
	 * @return 一个 键值 对儿 的 列表 [(key,map)]
	 */
	default Map<String, Object> toMap(final boolean flag) {
		final Function<Object, Object> deep_mapper = obj -> {
			final var o = IRecord.tidy(obj,
					e -> e instanceof IRecord || e instanceof Map ? IRecord.REC(e).toMap(true) : e);
			return o;
		}; // deep_mapper 深度数据变换

		return !flag // 是否递归处理
				? this.toMap() // 非递归处理
				: this.map(e -> e.fmap2(deep_mapper)).collect(IRecord.recclc()) // 转换成IReord
						.mutate(e -> e.toMap()); // 转换成 Map
	}

	/**
	 * 数据归集
	 * 
	 * @param <T>       归集的结果类型
	 * @param collector 归集器
	 * @return T类型的结果
	 */
	default <T> T collect(final Collector<? super Tuple2<String, Object>, ?, T> collector) {
		return this.tupleS().collect(collector);
	}

	/**
	 * 视rootnode(this)为一个树形结构的根节点,即每个 键值元组(K,V) 的 值V可以是一个 递归 嵌套的IRecord <br>
	 * 对rootnode(this)做深度遍历,并把遍历结果 写入 到一个 List&lt;U&gt; 结果之中 <br>
	 * 
	 * @param <T>    元素类型
	 * @param <U>    目标结果类型
	 * @param mapper 值变换函数 节点变换函数 (path:路径,node:T类型的节点值)-&gt;u:U 类型的变换结果
	 * @return 深度遍历的结果序列
	 */
	default <T, U> List<U> hierachizes(final BiFunction<List<String>, T, U> mapper) {
		return IRecord.hierachizeS(this, mapper).collect(Collectors.toList());
	}

	/**
	 * 视rootnode(this)为一个树形结构的根节点,即每个 键值元组(K,V) 的 值V可以是一个 递归 嵌套的IRecord <br>
	 * 对rootnode(this)做深度遍历,并把遍历结果 写入 到一个 List&lt;U&gt; 结果之中 <br>
	 * 
	 * @param <T>       元素类型
	 * @param <U>       目标结果类型
	 * @param mapper    值变换函数 节点变换函数 (path:路径,node:T类型的节点值)-&gt;u:U 类型的变换结果
	 * @param predicate 节点过滤函数, u -&gt; bool
	 * @return 深度遍历的结果序列
	 */
	default <T, U> List<U> hierachizes(final BiFunction<List<String>, T, U> mapper, final Predicate<U> predicate) {
		return this.hierachizeS(mapper, predicate).collect(Collectors.toList());
	}

	/**
	 * 视rootnode(this)为一个树形结构的根节点,即每个 键值元组(K,V) 的 值V可以是一个 递归 嵌套的IRecord <br>
	 * 对rootnode(this)做深度遍历,并把遍历结果 写入 到一个 List&lt;U&gt; 结果之中 <br>
	 * 
	 * @return 深度遍历的结果序列 [([string],obj)]
	 */
	default Stream<Tuple2<List<String>, Object>> hierachizeS() {
		return this.hierachizeS(Tuple2::new, e -> true);
	}

	/**
	 * 视rootnode(this)为一个树形结构的根节点,即每个 键值元组(K,V) 的 值V可以是一个 递归 嵌套的IRecord <br>
	 * 对rootnode(this)做深度遍历,并把遍历结果 写入 到一个 List&lt;U&gt; 结果之中 <br>
	 * 
	 * @param <T>    元素类型
	 * @param <U>    目标结果类型
	 * @param mapper 值变换函数 节点变换函数 (path:路径,node:T类型的节点值)-&gt;u:U 类型的变换结果
	 * @return 深度遍历的结果序列 [u]
	 */
	default <T, U> Stream<U> hierachizeS(final BiFunction<List<String>, T, U> mapper) {
		return this.hierachizeS(mapper, e -> true);
	}

	/**
	 * 视rootnode(this)为一个树形结构的根节点,即每个 键值元组(K,V) 的 值V可以是一个 递归 嵌套的IRecord <br>
	 * 对rootnode(this)做深度遍历,并把遍历结果 写入 到一个 List&lt;U&gt; 结果之中 <br>
	 * 
	 * @param <T>       元素类型
	 * @param <U>       目标结果类型
	 * @param mapper    值变换函数 节点变换函数 (path:路径,node:T类型的节点值)-&gt;u:U 类型的变换结果
	 * @param predicate 节点过滤函数, u -&gt; bool
	 * @return 深度遍历的结果序列 [u]
	 */
	default <T, U> Stream<U> hierachizeS(final BiFunction<List<String>, T, U> mapper, final Predicate<U> predicate) {
		return IRecord.hierachizeS(this, mapper).filter(predicate);
	}

	/**
	 * 对 IRecord 进行重命名
	 * 
	 * @param names 新的名称序列,如果 names 只有一个元素 则视为 names[0]为 键名描述序列,不同键名之间 使用 半角',' 给予分割
	 * @return 把一个IRecord进行重命名的函数
	 */
	default IRecord rename(final String... names) {
		return IRecord.namector(names).apply(this);
	}

	/**
	 * 键值对儿序列顺序调转<br>
	 * reverses an array in place. The first array element becomes the last, <br>
	 * and the last array element becomes the first. <br>
	 * {a:1,b:2,c:3} 转变成 {c:3,b:2,a:1}
	 * 
	 * @return 键值对儿序列顺序调转
	 */
	default IRecord reverse() {
		final var aa = new LinkedList<Tuple2<String, Object>>();
		this.tupleS().forEach(aa::addFirst);
		return aa.stream().collect(IRecord.recclc());
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 术语来源 pandas<br>
	 * 返回矩阵形状<br>
	 * 
	 * @return (height,width)的二维矩阵
	 */
	default Tuple2<Integer, Integer> shape() {
		final var width = this.keys().size();
		final var height = this.tupleS().map(kvp -> {
			final var vv = this.lla(kvp._1());
			if (vv == null)
				return 0;
			else
				return vv.size();
		}).collect(Collectors.summarizingInt(e -> e)).getMax();
		return new Tuple2<>(Math.max(height, 0), width);
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm <br>
	 * 
	 * 返回:<br>
	 * A:a B:1 C:2 D:3 E:31 <br>
	 * A:b B:2 C:4 D:6 E:61 <br>
	 * A:c B:3 C:6 D:9 E:91 <br>
	 * A:a B:1 C:10 D:3 E:31 <br>
	 * 
	 * @param <T>    结果类型 Target
	 * @param <V>    源数据的值类型 Value
	 * @param mapper 元素类型格式化函数,类型为， (key:String,value:Object)->new_value
	 * @param hh     列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * @return 返回以hh值为列名的行列表
	 */
	default <T, V> Stream<IRecord> rowS(final BiFunction<String, V, T> mapper, final List<String> hh) {
		return this.rows(mapper, hh).stream();
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm <br>
	 * 
	 * 返回:<br>
	 * A:a B:1 C:2 D:3 E:31 <br>
	 * A:b B:2 C:4 D:6 E:61 <br>
	 * A:c B:3 C:6 D:9 E:91 <br>
	 * A:a B:1 C:10 D:3 E:31 <br>
	 * 
	 * @return 返回以key值为列名的行列表
	 */
	default List<IRecord> rows() {
		return this.rows((key, value) -> value, this.keys());
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm <br>
	 * 
	 * 返回:<br>
	 * A:a B:1 C:2 D:3 E:31 <br>
	 * A:b B:2 C:4 D:6 E:61 <br>
	 * A:c B:3 C:6 D:9 E:91 <br>
	 * A:a B:1 C:10 D:3 E:31 <br>
	 * 
	 * @return 返回以key值为列名的行列表
	 */
	default Stream<IRecord> rowS() {
		return this.rows((key, value) -> value, this.keys()).stream();
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm <br>
	 * 
	 * 返回:<br>
	 * A:a B:1 C:2 D:3 E:31 <br>
	 * A:b B:2 C:4 D:6 E:61 <br>
	 * A:c B:3 C:6 D:9 E:91 <br>
	 * A:a B:1 C:10 D:3 E:31 <br>
	 * 
	 * @param <T>    结果类型 Target
	 * @param <V>    源数据的值类型 Value
	 * @param mapper 元素类型格式化函数,类型为， (key:String,value:Object)->new_value
	 * @param hh     列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * @return 返回以hh值为列名的行列表
	 */
	@SuppressWarnings("unchecked")
	default <T, V> List<IRecord> rows(final BiFunction<String, V, T> mapper, final List<String> hh) {
		final var shape = this.shape();// 提取图形结构
		final var rows = new ArrayList<IRecord>(shape._1());// 提取行数记录行数
		final List<String> final_hh = hh == null
				? Stream.iterate(0, i -> i + 1).limit(shape._2()).map(IRecord::excelname).collect(Collectors.toList())// 生成excel列名
				: hh;
		if (hh != null)
			Stream.iterate(hh.size(), i -> i < shape._2(), i -> i + 1).forEach(i -> final_hh.add(excelname(i)));
		final var keys = this.keys().toArray(String[]::new);
		for (int j = 0; j < shape._2(); j++) {// 列号
			final var col = this.lla(keys[j]);// 提取name列
			if (col == null)
				continue;
			final var size = col.size();// 列大小
			for (int i = 0; i < shape._1(); i++) { // 列名索引
				if (rows.size() <= i)
					rows.add(REC());
				final var row = rows.get(i);// 提取i 行的数据记录。
				final var key = final_hh.get(j);
				row.add(key, mapper.apply(keys[j], (V) col.get(i % size)));
			} // for i
		} // keys
		return rows;
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm
	 * 
	 * 返回:<br>
	 * A B C D E <br>
	 * a 1 2 3 31 <br>
	 * b 2 4 6 61 <br>
	 * c 3 6 9 91 <br>
	 * a 1 10 3 31 <br>
	 * 
	 * 按照列进行展示 对DataFrame进行初始化
	 * 
	 * @param key_formatter  键名内容初始化
	 * @param cell_formatter 键值元素内容初始化
	 * @return 格式化字符串
	 */
	default String toString2(final Function<Object, String> key_formatter,
			final Function<Object, String> cell_formatter) {

		final var builder = new StringBuilder();
		final var final_cell_formatter = cell_formatter != null ? cell_formatter : frt(2);
		final var final_key_formatter = key_formatter != null ? key_formatter : frt(2);
		builder.append(this.keys().stream().map(final_key_formatter).collect(Collectors.joining("\t"))).append("\n");
		this.rows().forEach(rec -> {
			builder.append(rec.values().stream().map(final_cell_formatter).collect(Collectors.joining("\t")));
			builder.append("\n");
		});// forEach
		return builder.toString();
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm
	 * 
	 * 返回:<br>
	 * A B C D E <br>
	 * a 1 2 3 31 <br>
	 * b 2 4 6 61 <br>
	 * c 3 6 9 91 <br>
	 * a 1 10 3 31 <br>
	 * 
	 * 按照列进行展示 对DataFrame进行初始化
	 * 
	 * @param cell_formatter 元素内容初始化
	 * @return 格式化字符串
	 */
	default String toString2(final Function<Object, String> cell_formatter) {

		return this.toString2(null, cell_formatter);
	}

	/**
	 * DataFrame 类型的数据方法,所谓DataFrame 是指键值对儿中的值为List的IRecord(键值对儿集合)<br>
	 * 返回行列表:<br>
	 * final var dfm = REC( <br>
	 * "A",L("a","b","c"), // 第一列 <br>
	 * "B",L(1,2,3), // 第二列 <br>
	 * "C",A(2,4,6,10), // 第三列 <br>
	 * "D",REC(0,3,1,6,2,9), // 第四列,需要注意这是一个
	 * (0,3),(1,6),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * "E",REC(0,31,1,61,2,91).toMap() // 第五列，需要注意这是一个
	 * (0,31),(1,61),...,这样的(key,value)序列，而不是单纯的值 序列 <br>
	 * );// dfm
	 * 
	 * 返回:<br>
	 * A B C D E <br>
	 * a 1 2 3 31 <br>
	 * b 2 4 6 61 <br>
	 * c 3 6 9 91 <br>
	 * a 1 10 3 31 <br>
	 * 
	 * 按照列进行展示 对DataFrame进行初始化
	 * 
	 * @return 格式化字符串
	 */
	default String toString2() {

		return toString2(null);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>       第一参数类型
	 * @param keys      参数名列表，保留的键名序列，键名之间采用半角逗号分隔
	 * @param predicate 参数测试函数 t->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T> Predicate<IRecord> test(final String keys, final Predicate<T> predicate) {
		return IRecord.test(keys.split("[,]"), predicate);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>       第一参数类型
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 t->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	@SuppressWarnings("unchecked")
	public static <T> Predicate<IRecord> test(final String[] keys, final Predicate<T> predicate) {
		return rec -> {
			final var tt = rec.filter(keys).toArray();
			final var t = Optional.ofNullable(tt.length > 0 ? (T) tt[0] : null).orElse(null);
			return predicate.test(t);
		};
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         第一参数类型
	 * @param <U>         第二参数类型
	 * @param keys        参数名列表，保留的键名序列，键名之间采用半角逗号分隔
	 * @param bipredicate 参数测试函数 (t,u)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> test(final String keys, final BiPredicate<T, U> bipredicate) {
		return IRecord.test(keys.split("[,]+"), bipredicate);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         第一参数类型
	 * @param <U>         第二参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (t,u)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Predicate<IRecord> test(final String keys[], final BiPredicate<T, U> bipredicate) {
		return rec -> {
			final var tt = rec.filter(keys).toArray();
			final var t = Optional.ofNullable(tt.length > 0 ? (T) tt[0] : null).orElse(null);
			final var u = Optional.ofNullable(tt.length > 1 ? (U) tt[1] : null).orElse(null);
			return bipredicate.test(t, u);
		};
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (u,u)->bool
	 * @param mapper      参数值变换函数 (t)->u
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Predicate<IRecord> test(final String keys[], final BiPredicate<U, U> bipredicate,
			final Function<T, U> mapper) {
		return rec -> {
			final var uu = rec.filter(keys).toArray();
			final var u0 = Optional.ofNullable(uu.length > 0 ? (T) uu[0] : null).map(mapper).orElse(null);
			final var u1 = Optional.ofNullable(uu.length > 1 ? (T) uu[1] : null).map(mapper).orElse(null);
			return bipredicate.test(u0, u1);
		};
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <U>       目标参数类型
	 * @param keys      参数名列表，保留的键名序列，键名之间采用半角逗号分隔
	 * @param predicate 参数测试函数 (u)->bool
	 * @param mapper    参数值变换函数 [o]->u
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <U> Predicate<IRecord> test(final String keys, final Predicate<U> predicate,
			final Function<Object[], U> mapper) {
		return IRecord.test(keys.split("[,]+"), predicate, mapper);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>       源参数类型
	 * @param <U>       目标参数类型
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (u)->bool
	 * @param mapper    参数值变换函数 [o]->u
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> test(final String keys[], final Predicate<U> predicate,
			final Function<Object[], U> mapper) {
		return rec -> {
			final var oo = rec.filter(keys).toArray();
			return predicate.test(mapper.apply(oo));
		};
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表，保留的键名序列，键名之间采用半角逗号分隔
	 * @param predicate 参数测试函数 [o]->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> testa(final String keys, final Predicate<Object[]> predicate) {
		return IRecord.test(keys.split("[,]+"), predicate, e -> e);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 [o]->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> testa(final String keys[], final Predicate<Object[]> predicate) {
		return rec -> {
			final var oo = rec.filter(keys).toArray();
			return predicate.test(oo);
		};
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (int)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> int_test(final String keys[], final Predicate<Integer> predicate) {
		return IRecord.test(keys, (x, y) -> predicate.test(x), a -> IRecord.obj2dbl().apply(a).intValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (int)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> int_test(final String keys, final Predicate<Integer> predicate) {
		return IRecord.test(keys.split("[,]+"), (x, y) -> predicate.test(x),
				a -> IRecord.obj2dbl().apply(a).intValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (int,int)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> int_test(final String keys,
			final BiPredicate<Integer, Integer> bipredicate) {
		return IRecord.test(keys.split("[,]+"), bipredicate, a -> IRecord.obj2dbl().apply(a).intValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (t,u)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> int_test(final String keys[],
			final BiPredicate<Integer, Integer> bipredicate) {
		return IRecord.test(keys, bipredicate, a -> IRecord.obj2dbl().apply(a).intValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (lng)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> lng_test(final String keys, final Predicate<Long> predicate) {
		return IRecord.test(keys.split("[,]+"), (x, y) -> predicate.test(x),
				a -> IRecord.obj2dbl().apply(a).longValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (lng)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> lng_test(final String keys[], final Predicate<Long> predicate) {
		return IRecord.test(keys, (x, y) -> predicate.test(x), a -> IRecord.obj2dbl().apply(a).longValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (lng,lng)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> lng_test(final String keys, final BiPredicate<Long, Long> bipredicate) {
		return IRecord.test(keys.split("[,]+"), bipredicate, a -> IRecord.obj2dbl().apply(a).longValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (dbl,dbl)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> lng_test(final String keys[], final BiPredicate<Long, Long> bipredicate) {
		return IRecord.test(keys, bipredicate, a -> IRecord.obj2dbl().apply(a).longValue());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (dbl)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> dbl_test(final String keys, final Predicate<Double> predicate) {
		return IRecord.test(keys.split("[,]+"), (x, y) -> predicate.test(x), IRecord.obj2dbl());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (dbl)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> dbl_test(final String keys[], final Predicate<Double> predicate) {
		return IRecord.test(keys, (x, y) -> predicate.test(x), a -> IRecord.obj2dbl().apply(a));
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (dbl,dbl)->bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> dbl_test(final String keys[],
			final BiPredicate<Double, Double> bipredicate) {
		return IRecord.test(keys, bipredicate, IRecord.obj2dbl());
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> ldt_test(final String keys, final Predicate<LocalDateTime> predicate) {
		return IRecord.test(keys.split("[,]+"), (x, y) -> predicate.test(x), IRecord::asLocalDateTime);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> ldt_test(final String keys[], final Predicate<LocalDateTime> predicate) {
		return IRecord.test(keys, (x, y) -> predicate.test(x), a -> IRecord.asLocalDateTime(a));
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (ldt,ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> ldt_test(final String keys,
			final BiPredicate<LocalDateTime, LocalDateTime> bipredicate) {
		return IRecord.test(keys.split("[,]+"), bipredicate, IRecord::asLocalDateTime);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (ldt,ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> ldt_test(final String keys[],
			final BiPredicate<LocalDateTime, LocalDateTime> bipredicate) {
		return IRecord.test(keys, bipredicate, IRecord::asLocalDateTime);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> ld_test(final String keys, final Predicate<LocalDate> predicate) {
		return IRecord.test(keys.split("[,]+"), (x, y) -> predicate.test(x), IRecord::asLocalDate);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> ld_test(final String keys[], final Predicate<LocalDate> predicate) {
		return IRecord.test(keys, (x, y) -> predicate.test(x), a -> IRecord.asLocalDate(a));
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (ldt,ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> ld_test(final String keys,
			final BiPredicate<LocalDate, LocalDate> bipredicate) {
		return IRecord.test(keys.split("[,]+"), bipredicate, IRecord::asLocalDate);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (ldt,ldt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> ld_test(final String keys[],
			final BiPredicate<LocalDate, LocalDate> bipredicate) {
		return IRecord.test(keys, bipredicate, IRecord::asLocalDate);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (lt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> lt_test(final String keys, final Predicate<LocalTime> predicate) {
		return IRecord.test(keys.split("[,]+"), (x, y) -> predicate.test(x), IRecord::asLocalTime);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param keys      参数名列表
	 * @param predicate 参数测试函数 (lt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static Predicate<IRecord> lt_test(final String keys[], final Predicate<LocalTime> predicate) {
		return IRecord.test(keys, (x, y) -> predicate.test(x), a -> IRecord.asLocalTime(a));
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (lt,lt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> lt_test(final String keys,
			final BiPredicate<LocalTime, LocalTime> bipredicate) {
		return IRecord.test(keys.split("[,]+"), bipredicate, IRecord::asLocalTime);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>         源参数类型
	 * @param <U>         目标参数类型
	 * @param keys        参数名列表
	 * @param bipredicate 参数测试函数 (lt,lt)-&gt;bool
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> lt_test(final String keys[],
			final BiPredicate<LocalTime, LocalTime> bipredicate) {
		return IRecord.test(keys, bipredicate, IRecord::asLocalTime);
	}

	/**
	 * 结果值的测试
	 * 
	 * @param <T>  第一参数类型
	 * @param <U>  第二参数类型
	 * @param keys 参数名列表
	 * @return IRecord 的谓词测试函数 rec-&gt;bool
	 */
	public static <T, U> Predicate<IRecord> eql(final String keys) {
		return IRecord.test(keys, (a, b) -> {
			if (a == null) {
				return a == b;
			} else {
				return a.equals(b);
			}
		});
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final List<String> keys) {
		return IRecord.cmp(keys.toArray(String[]::new), true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable <br>
	 * 升序排序器 <br>
	 * 
	 * @param keys 键名序列，键名之间用半角','分隔
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final String keys) {
		return cmp(keys, true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列，键名之间用半角','分隔
	 * @param asc  是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final String keys, final boolean asc) {
		return cmp(keys.split(","), null, asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param <T>    元素类型
	 * @param <U>    具有比较能力的类型
	 * @param keys   keys 键名序列
	 * @param mapper (key:键名,t:键值)-&gt;u 比较能力变换器
	 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	public static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final List<String> keys,
			final BiFunction<String, T, U> mapper, final boolean asc) {
		return IRecord.cmp(keys.toArray(String[]::new), mapper, asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final String keys[]) {
		return IRecord.cmp(keys, true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列
	 * @param asc  是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final String keys[], final boolean asc) {
		return cmp(keys, null, asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param <T>    元素类型
	 * @param <U>    具有比较能力的类型
	 * @param keys   键名序列
	 * @param mapper (key:键名,t:键值)-&gt;u 比较能力变换器
	 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	@SuppressWarnings("unchecked")
	public static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final String keys[],
			final BiFunction<String, T, U> mapper, final boolean asc) {

		final BiFunction<String, T, U> final_mapper = mapper == null
				? (String i, T o) -> o instanceof Comparable ? (U) o : (U) (o + "")
				: mapper;

		return (a, b) -> {
			final var queue = new LinkedList<String>();
			Arrays.stream(keys).map(String::trim).forEach(queue::offer); // 压入队列

			while (!queue.isEmpty()) {
				final var key = queue.poll(); // 提取队首元素
				final var ta = (Comparable<Object>) a.invoke(key, (T t) -> final_mapper.apply(key, t));
				final var tb = (Comparable<Object>) b.invoke(key, (T t) -> final_mapper.apply(key, t));

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
						final var aa = Stream.of(ta, tb).map(o -> o != null ? o.getClass().getName() + o : "null")
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
	 * 树形遍历
	 * 
	 * @param root   根节点
	 * @param action 回调函数( level:层级从0开始时且0标识根节点层级,node:(path:从根节点开始,value:节点值))
	 *               -&gt;{}
	 */
	public static void forEach(final IRecord root, final BiConsumer<Integer, Tuple2<String, Object>> action) {
		final var parents = new TreeMap<Integer, Tuple2<String, Object>>(); // 层级注册表
		final var rootNode = P("", (Object) root); // 根节点的名称为空

		parents.put(0, rootNode); // 根节点
		action.accept(0, rootNode); // 遍历根节点

		forEach(root, action, 1, parents);
	}

	/**
	 * 树形遍历
	 * 
	 * @param root   根节点
	 * @param action 回调函数( level:层级从0开始时且0标识根节点层级,node:(path:从根节点开始,value:节点值))
	 *               -&gt;{}
	 * @param level  阶层位置,从 1 开始，因为根节点已经预先装入
	 */
	static void forEach(final IRecord root, final BiConsumer<Integer, Tuple2<String, Object>> action, final int level,
			final Map<Integer, Tuple2<String, Object>> parents) {

		for (final var node : root) {
			parents.put(level, node); // 记录当前层级信息

			final var base = parents.keySet().stream().limit(level) // 获取当前有效层级前驱
					.map(e -> parents.get(e)._1) // 提取阶层标识key
					.collect(Collectors.joining("/")); // 汇总阶层标识key连接成阶层前缀
			final var value = node._2(); // 层级节点的值
			final var path = base + "/" + node._1; // 当前节点路径
			final var valueNode = P(path, value); // 当前的值节点

			action.accept(level, valueNode); // 处理当前节点

			if (value instanceof IRecord) { // 对于IRecord类型的节点则深入遍历
				forEach((IRecord) value, action, level + 1, parents);
			} // if
		} // for
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 null <br>
	 * 
	 * 默认会尝试把时间类型也解释为数字,即 '1970-01-01 08:00:01' <br>
	 * 也会被转换成一个 0时区 的 从1970年1月1 即 epoch time 以来的毫秒数<br>
	 * 对于 中国 而言 位于+8时区, '1970-01-01 08:00:01' 会被解析为1000
	 * 
	 * @return t->dbl
	 */
	static ToDoubleFunction<?> todbl(final Number default_value) {
		return e -> IRecord.obj2dbl(default_value).apply(e);
	};

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 defaultValue <br>
	 * 
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
	 * 
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
					final var ldt = IRecord.REC("key", obj).ldt("key"); // 尝试把时间转换成数字
					if (ldt != null) {
						final var systemZone = ZoneId.systemDefault(); // 默认时区
						final var offset = systemZone.getRules().getOffset(ldt); // 时区 offset
						dbl = ((Number) ldt.toInstant(offset).toEpochMilli()).doubleValue(); // 转换成 epoch time 以来的毫秒数
					} // if
				}
			} // try

			return dbl;
		};
	}

	/**
	 * 生成 构建器
	 * 
	 * @param keys 键名序列, 用半角逗号 “,” 分隔
	 * @return keys 为格式的 构建器
	 */
	public static Builder rb(final String keys) {

		return rb(keys.split(","));
	}

	/**
	 * 生成 构建器
	 * 
	 * @param keys 键名序列
	 * @return keys 为格式的 构建器
	 */
	public static Builder rb(final String[] keys) {

		return rb(Arrays.asList(keys));
	}

	/**
	 * 生成 构建器
	 * 
	 * @param keys 键名序列
	 * @return keys 为格式的 构建器
	 */
	public static <T> Builder rb(final Iterable<T> keys) {

		return new Builder(keys);
	}

	/**
	 * 生成 构建器
	 * 
	 * @param n     键数量,正整数
	 * @param keyer 键名生成器 (i:从0开始)-&gt;key
	 * @return IRecord 构造器
	 */
	public static Builder rb(final int n, Function<Integer, ?> keyer) {

		final var keys = Stream.iterate(0, i -> i < n, i -> i + 1).map(keyer).collect(Collectors.toList());
		return new Builder(keys);
	}

	/**
	 * 生成 构建器
	 * 
	 * @param keys 键名序列
	 * @return keys 为格式的 构建器
	 */
	@SafeVarargs
	public static <T> Builder rb(final T... keys) {

		final var _keys = Arrays.asList(keys).stream().map(e -> e + "").collect(Collectors.toList());
		return new Builder(_keys);
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
	public static <T> Stream<List<T>> slidingS(final T[] aa, final int size, final int step, final boolean flag) {

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
	public static <T> Stream<List<T>> slidingS(final Collection<T> collection, final int size, final int step,
			final boolean flag) {

		final var n = collection.size();
		final var arrayList = collection instanceof ArrayList // 类型检测
				? (ArrayList<T>) collection // 数组列表类型
				: new ArrayList<T>(collection); // 其他类型

		// 当flag 为true 的时候 i的取值范围是: [0,n-size] <==> [0,n+1-size)
		return Stream.iterate(0, i -> i < (flag ? n + 1 - size : n), i -> i + step) // 序列生成
				.map(i -> arrayList.subList(i, (i + size) > n ? n : (i + size)));
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
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
	 * @param z_finisher 最终结果包装函数 {(k,v)}-&gt;z
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
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
	 * @param valuerer   键值函数 x->value
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
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
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
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
	 * @param valueer    键值创建函数 x-&gt;value
	 * @param u_finisher 键值元素集合包装函数 vv-&gt;u
	 * @param z_finisher 最终结果的生成函数 {(k,u)}-&gt;z
	 * @return U 结果类型
	 */
	public static <X, K, V, U, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer, final Function<X, V> valueer,
			final Function<List<V>, U> u_finisher, final Function<Map<K, U>, Z> z_finisher) {

		final var clc = Collector.of( // 创建轨迹器
				(Supplier<LinkedHashMap<K, List<V>>>) () -> new LinkedHashMap<K, List<V>>(),
				(LinkedHashMap<K, List<V>> lhm, X x) -> {
					final var k = keyer.apply(x);
					final var v = valueer.apply(x);
					lhm.computeIfAbsent(k, _k -> new ArrayList<V>()).add(v);
				}, (tuples1, tuples2) -> {
					tuples1.putAll(tuples2);
					return tuples1;
				}, lhm -> { // 最终类型包装器
					final var lhm1 = new LinkedHashMap<K, U>();

					lhm.entrySet().stream().forEach(entry -> {
						final var key = entry.getKey(); // 键名
						final var u = u_finisher.apply(entry.getValue()); // 值类型归集
						lhm1.put(key, u); // 值类型
					});

					final var z = z_finisher.apply(lhm1);

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
	 * @param evaluator 路径集合计算函数 ([r]:IRecord列表)-&gt;t
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
	 * @param evaluator 路径集合计算函数 ([r]:IRecord列表)-&gt;t
	 * @param keys      归集的键名序列,keys 长度为0则统一归key类别之下
	 * @return 数据透视表
	 */
	public static <T> Collector<IRecord, ?, IRecord> pvtclc(final Function<List<IRecord>, T> evaluator,
			final String... keys) {
		if (keys.length > 1 && keys[0] != null) { // 键名序列长度大于0,递归归集成嵌套的IRecord
			final var restings = Arrays.copyOfRange(keys, 1, keys.length); // 剩余的键名
			return IRecord.grpclc((IRecord r) -> r.str(keys[0].strip()), e -> e, pvtclc(evaluator, restings),
					IRecord::REC); // 递归归集
		} else if (keys.length > 0 && keys[0] != null) { // 键名序列长度为1,键值归集成列表
			return IRecord.grpclc((IRecord r) -> r.str(keys[0].strip()), e -> e, evaluator, IRecord::REC);
		} else { // 没有键名序列,统一归key类别之下
			return IRecord.grpclc((IRecord r) -> "key", e -> e, evaluator, IRecord::REC);
		} // if
	}

	/**
	 * 二元算术运算符号 除法<br>
	 * 
	 * 非数字 则 返回第一个值
	 * 
	 * @return (record0,record1)-&gt;record2
	 */
	public static BinaryOperator<IRecord> divide() {

		return IRecord.binaryOp((a, b) -> a / b);
	}

	/**
	 * 二元算术运算符号 乘法 <br>
	 * 
	 * 非数字 则 返回第一个值
	 * 
	 * @return (record0,record1)-&gt;record2
	 */
	public static BinaryOperator<IRecord> multiply() {

		return IRecord.binaryOp((a, b) -> a * b);
	}

	/**
	 * 二元算术运算符号 减法 <br>
	 * 
	 * 非数字 则 返回第一个值
	 * 
	 * @return (record0,record1)-&gt;record2
	 */
	public static BinaryOperator<IRecord> subtract() {

		return IRecord.binaryOp((a, b) -> a - b);
	}

	/**
	 * 二元算术运算符号 加法 <br>
	 * 
	 * 非数字 则 返回第一个值
	 * 
	 * @return (record0,record1)-&gt;record2
	 */
	public static BinaryOperator<IRecord> plus() {

		return IRecord.binaryOp((a, b) -> a + b);
	}

	/**
	 * 生成一个接收右侧值一元运算符(左侧值采用leftAtom提供) <br>
	 * 
	 * @param <L>          左参数类型
	 * @param <R>          右参数类型
	 * @param <V>          结果值类型
	 * @param leftSupplier 左侧值生成器
	 * @param op           归并器 (l,r)->v
	 * @return r-> op.apply(l,r);
	 */
	public static <L, R, V> Function<IRecord, IRecord> rightUnaryOp(final Supplier<IRecord> leftSupplier,
			final BiFunction<L, R, V> op) {

		return right -> IRecord.combine(op).apply(leftSupplier.get(), right);
	}

	/**
	 * 生成一个左侧一元运算符(右侧值采用rightAtom提供) <br>
	 * 
	 * @param <L>           左参数类型
	 * @param <R>           右参数类型
	 * @param <V>           结果值类型
	 * @param rightSupplier 右侧值生成器
	 * @param op            归并器 (l,r)-&gt;v
	 * @return l -&gt; op.apply(l,r)
	 */
	public static <L, R, V> Function<IRecord, IRecord> leftUnaryOp(final Supplier<IRecord> rightSupplier,
			final BiFunction<L, R, V> op) {

		return left -> IRecord.combine(op).apply(left, rightSupplier.get());
	}

	/**
	 * 返回一个左加函数
	 * 
	 * 非数字 则 返回第一个值
	 * 
	 * @param biop 归并器 (d,d)-&gt;d
	 * @return (record0,record1)-&gt;record2
	 */
	public static BinaryOperator<IRecord> binaryOp(final BinaryOperator<Double> biop) {

		return IRecord.combine2((key, tup) -> {
			final var aa = Stream.of(tup._1, tup._2).map(IRecord.obj2dbl()).toArray(Double[]::new);
			if (aa[0] != null && null != aa[1]) {
				return biop.apply(aa[0], aa[1]);
			} else { // 非数字 则 返回第一个值
				return tup._1 == null || tup._1.toString().matches("^\\s*$") ? tup._2() : tup._1;
			} // if
		});
	}

	/**
	 * 二元算术运算符号 <br>
	 * 
	 * 非数字 则 返回第一个值
	 * 
	 * @param <T>    归并元素类型
	 * @param biop   归并器 (t,t)->t
	 * @param tclass 占位符的元素类型
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	public static <T> BinaryOperator<IRecord> binaryOp(final BinaryOperator<T> biop, final Class<T> tclass) {

		return IRecord.combine2((key, tup) -> {
			final var aa = Stream.of(tup._1, tup._2).map(IRecord.obj2dbl()).toArray(Object[]::new);
			if (aa[0] != null && null != aa[1]) {
				return biop.apply((T) aa[0], (T) aa[1]);
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
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T> BinaryOperator<IRecord> max(final Function<T, Number> quantizer) {

		return IRecord.combine2((k, tup) -> {
			final var a = quantizer.apply((T) tup._1).doubleValue();
			final var b = quantizer.apply((T) tup._2).doubleValue();
			return a > b ? tup._1 : tup._2;
		});
	}

	/**
	 * 生成一个 IRecord的二元运算法。最小值
	 * 
	 * @param <T>       度量器的数据类型
	 * @param quantizer 度量器 t-&gt;number
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T> BinaryOperator<IRecord> min(final Function<T, Number> quantizer) {

		return IRecord.combine2((k, tup) -> {
			final var a = quantizer.apply((T) tup._1).doubleValue();
			final var b = quantizer.apply((T) tup._2).doubleValue();
			return a < b ? tup._1 : tup._2;
		});
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 * 
	 * @param <T> 第一参数类型
	 * @param <U> 第二参数类型
	 * @param <V> 结果类型
	 * @param op  归并器 (t,u)-&gt;v
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine(final BiFunction<T, U, V> op) {

		return IRecord.combine2((k, tup) -> op.apply((T) tup._1, (U) tup._2));
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 * 
	 * @param <T> 第一参数类型
	 * @param <U> 第二参数类型
	 * @param <V> 结果类型
	 * @param op  归并器 (k:键名,(t:左侧元素,u:右侧元素))-&gt;v
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine2(final BiFunction<String, Tuple2<T, U>, V> op) {

		return IRecord.combine4((tup1, tup2) -> op.apply(tup1._2, (Tuple2<T, U>) tup2));
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 * 
	 * @param <T> 第一参数类型
	 * @param <U> 第二参数类型
	 * @param <V> 结果类型
	 * @param op  归并器 (i:键名索引,(t:左侧元素,u:右侧元素))->v
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine3(final BiFunction<Integer, Tuple2<T, U>, V> op) {

		return IRecord.combine4((tup1, tup2) -> op.apply(tup1._1, (Tuple2<T, U>) tup2));
	}

	/**
	 * 生成一个 IRecord的二元运算法。
	 * 
	 * @param <T>    第一参数类型
	 * @param <U>    第二参数类型
	 * @param <V>    结果类型
	 * @param bifunc 归并器 ((i:键名索引,k:键名),(t:左侧元素,u:右侧元素))-&gt;v
	 * @return (record0,record1)-&gt;record2
	 */
	@SuppressWarnings("unchecked")
	static <T, U, V> BinaryOperator<IRecord> combine4(
			final BiFunction<Tuple2<Integer, String>, Tuple2<T, U>, V> bifunc) {

		return (record_left, record_right) -> {
			final var keys_left = record_left.keys();
			final var keys_right = record_right.keys();
			final var keys = Stream.concat(keys_left.stream(), keys_right.stream()).distinct()
					.collect(Collectors.toList());
			final var rb = IRecord.rb(keys); // 返回结果的构建器
			final var ai = new AtomicInteger();
			final var values = keys.stream().map(k -> {
				Object value = null;
				try {
					final var i = ai.getAndIncrement(); // 记录键名索引
					final var left = (T) record_left.get(k);
					final var right = (U) record_right.get(k);
					value = bifunc.apply(P(i, k), P(left, right)); // 返回结果
				} catch (Exception e) {
					e.printStackTrace();
				}
				return value;
			}).toArray(); // 计算结果值

			return rb.get(values);
		}; // BinaryOperator
	}

	/**
	 * 可枚举 转 列表
	 * 
	 * @param <T>      元素类型
	 * @param iterable 可枚举类
	 * @param maxSize  最大的元素数量
	 * @return 元素列表
	 */
	public static <T> List<T> itr2list(final Iterable<T> iterable, final int maxSize) {

		final var stream = StreamSupport.stream(iterable.spliterator(), false).limit(maxSize);
		return stream.collect(Collectors.toList());
	}

	/**
	 * 可枚举 转 列表
	 * 
	 * @param <T>      元素类型
	 * @param iterable 可枚举类
	 * @return 元素列表
	 */
	public static <T> List<T> itr2list(final Iterable<T> iterable) {

		return IRecord.itr2list(iterable, MAX_SIZE);
	}

	/**
	 * 构造 Map 结构
	 * 
	 * @param <T>  元素类型
	 * @param objs 值序列
	 * @return IRecord
	 */
	@SuppressWarnings("unchecked")
	static <T> Map<Object, Object> asMap(final T... objs) {

		final int n = objs.length;
		final var data = new LinkedHashMap<Object, Object>();

		for (int i = 0; i < n - 1; i += 2) {
			final var key = objs[i];
			final var value = objs[i + 1];
			data.put(key, value);
		} // for

		return data;
	}

	/**
	 * 把值对象转化成列表结构
	 * 
	 * @param value 值对象
	 * @return 列表结构
	 */
	@SuppressWarnings("unchecked")
	public static List<Object> asList(final Object value) {

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
			final var aa = new ArrayList<Object>();

			if (value != null) {
				aa.add(value);
			}

			return aa;
		} // if
	}

	/**
	 * 把一个值对象转换成LocalTime
	 * 
	 * @param value 值对象
	 * @return LocalTime
	 */
	static LocalTime asLocalTime(final Object value) {
		return Optional.ofNullable(IRecord.asLocalDateTime(value)).map(e -> e.toLocalTime()).orElse(null);
	}

	/**
	 * 把一个值对象转换成LocalDate
	 * 
	 * @param value 值对象
	 * @return LocalDate
	 */
	static LocalDate asLocalDate(final Object value) {
		return Optional.ofNullable(IRecord.asLocalDateTime(value)).map(e -> e.toLocalDate()).orElse(null);
	}

	/**
	 * 把一个值对象转换成LocalDateTime
	 * 
	 * @param value 值对象
	 * @return LocalDateTime
	 */
	static LocalDateTime asLocalDateTime(final Object value) {
		final Function<LocalDate, LocalDateTime> ld2ldt = ld -> LocalDateTime.of(ld, LocalTime.of(0, 0));
		final Function<LocalTime, LocalDateTime> lt2ldt = lt -> LocalDateTime.of(LocalDate.of(0, 1, 1), lt);
		final Function<Long, LocalDateTime> lng2ldt = lng -> {
			final var timestamp = lng;
			final var instant = Instant.ofEpochMilli(timestamp);
			final var zoneId = ZoneId.systemDefault();
			return LocalDateTime.ofInstant(instant, zoneId);
		};

		final Function<Timestamp, LocalDateTime> timestamp2ldt = timestamp -> {
			return lng2ldt.apply(timestamp.getTime());
		};

		final Function<Date, LocalDateTime> dt2ldt = dt -> {
			return lng2ldt.apply(dt.getTime());
		};

		final Function<String, LocalTime> str2lt = line -> {
			LocalTime lt = null;
			for (var format : "HH:mm:ss,HH:mm,HHmmss,HHmm,HH".split("[,]+")) {
				try {
					lt = LocalTime.parse(line, DateTimeFormatter.ofPattern(format));
				} catch (Exception ex) {
					// do nothing
				}
				if (lt != null)
					break;
			}
			return lt;
		};

		final Function<String, LocalDate> str2ld = line -> {
			LocalDate ld = null;
			for (var format : "yyyy-MM-dd,yyyy-M-d,yyyy/MM/dd,yyyy/M/d,yyyyMMdd".split("[,]+")) {
				try {
					ld = LocalDate.parse(line, DateTimeFormatter.ofPattern(format));
				} catch (Exception ex) {
					// do nothing
				}
				if (ld != null)
					break;
			}

			return ld;
		};

		final Function<String, LocalDateTime> str2ldt = line -> {
			LocalDateTime ldt = null;
			final var patterns = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS," //
					+ "yyyy-MM-dd'T'HH:mm:ss.SSSSSS," //
					+ "yyyy-MM-dd'T'HH:mm:ss.SSS," //
					+ "yyyy-MM-dd HH:mm:ss," //
					+ "yyyy-MM-dd HH:mm," //
					+ "yyyy-MM-dd HH," //
					+ "yyyy-M-d H:m:s," //
					+ "yyyy-M-d H:m," //
					+ "yyyy-M-d H," //
					+ "yyyy/MM/dd HH:mm:ss," //
					+ "yyyy/MM/dd HH:mm," //
					+ "yyyy/MM/dd HH," //
					+ "yyyy/M/d H:m:s," //
					+ "yyyy/M/d H:m," //
					+ "yyyy/M/d H," //
					+ "yyyyMMddHHmmss," //
					+ "yyyyMMddHHmm," //
					+ "yyyyMMddHH"//
			; // patterns 时间的格式字符串

			for (var format : patterns.split("[,]+")) {
				try {
					ldt = LocalDateTime.parse(line, DateTimeFormatter.ofPattern(format));
				} catch (Exception ex) {
					// do nothing
				}
				if (ldt != null)
					break;
			}

			return ldt;
		};

		if (value instanceof LocalDateTime) {
			return (LocalDateTime) value;
		} else if (value instanceof LocalDate) {
			return ld2ldt.apply((LocalDate) value);
		} else if (value instanceof LocalTime) {
			return lt2ldt.apply((LocalTime) value);
		} else if (value instanceof Number) {
			return lng2ldt.apply(((Number) value).longValue());
		} else if (value instanceof Timestamp) {
			return timestamp2ldt.apply(((Timestamp) value));
		} else if (value instanceof Date) {
			return dt2ldt.apply(((Date) value));
		} else if (value instanceof String) {
			final var line = (String) value;
			final var _ldt = str2ldt.apply(line);
			if (Objects.nonNull(_ldt)) {
				return _ldt;
			}
			final var _ld = str2ld.apply(line);
			if (Objects.nonNull(_ld)) {
				return ld2ldt.apply(_ld);
			}
			final var _lt = str2lt.apply(line);
			if (Objects.nonNull(_lt)) {
				return lt2ldt.apply(_lt);
			}
			return null;
		} else {
			return null;
		}
	}

	/**
	 * fill_template 的别名<br>
	 * Term.FT("$0+$1=$2",1,2,3) 替换后的结果为 1+2=3
	 * 
	 * @param template 模版字符串，占位符$0,$1,$2,...
	 * @param tt       模版参数序列
	 * @return template 被模版参数替换后的字符串
	 */
	public static String FT(final String template, final Object... tt) {
		final var rec = IRecord.rb(tt.length, i -> "$" + i).get(tt);
		return fill_template(template, rec);
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
	public static String FT(final String template, final IRecord placeholder2values) {
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
	public static String fill_template(final String template, final IRecord placeholder2values) {

		if (placeholder2values == null) { // 空值判断
			return template;
		}

		final var len = template.length(); // 模版的长度
		final StringBuilder buffer = new StringBuilder();// 工作缓存
		final var keys = placeholder2values.keys().stream().sorted((a, b) -> -(a.length() - b.length())) // 按照从长到短的顺序进行排序。以保证keys的匹配算法采用的是贪婪算法。
				.collect(Collectors.toList());// 键名 也就是template中的占位符号。
		final var firstCharMap = keys.stream().collect(Collectors.groupingBy(key -> key.charAt(0)));// keys的首字符集合，这是为了加快
		// 读取的步进速度
		int i = 0;// 当前读取的模版字符位置,从0开始。

		while (i < len) {// 从前向后[0,len)的依次读取模板字符串的各个字符
			// 注意:substring(0,0) 或是 substring(x,x) 返回的是一个长度为0的字符串 "",
			// 特别是,当 x大于等于字符串length会抛异常:StringIndexOutOfBoundsException
			final var line = template.substring(0, i);// 业已读过的模版字符串内容[0,i),当前所在位置为i等于line.length
			String placeholder = null;// 占位符号 的内容
			final var kk = firstCharMap.get(template.charAt(i));// 以 i为首字符开头的keys
			if (kk != null) {// 使用firstCharMap加速步进速度读取模版字符串
				for (final var key : kk) {// 寻找 可以被替换的key
					final var endIndex = line.length() + key.length(); // 终点索引 exclusive
					final var b = endIndex > len // 是否匹配到placeholder
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
				final var value = placeholder2values.get(placeholder); // 提取占位符内容
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
	 * @param <T> 键值序列的元素类型
	 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
	 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
	 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
	 * @return 新生成的IRecord
	 */
	@SafeVarargs
	public static <T> IRecord REC(final T... kvs) {
		return MyRecord.REC(kvs); // 采用 MyRecord 来作为IRecord的默认实现函数
	}

	/**
	 * 滑动窗口的T元素归集器<br>
	 * 返回齐次窗口长度,齐次窗口是一个二元组<br>
	 * [1,2,3] -> [(1,2),(2,3)] <br>
	 * 
	 * @param <T> 流元素类型
	 * @return 滑动窗口的T元素归集器
	 */
	public static <T> Collector<T, ?, Stream<Tuple2<T, T>>> tup2slidingclcS() {
		return IRecord.tup2slidingclcS(true);
	}

	/**
	 * 使用指定的 名称序列 为 指定的 记录结构命名
	 * 
	 * @param names 新的名称序列,如果 names 只有一个元素 则视为 names[0]为 键名描述序列,不同键名之间 使用 半角',' 给予分割
	 * @return 把一个IRecord进行重命名的函数
	 */
	public static Function<IRecord, IRecord> namector(final String... names) {
		String[] _names = names.length == 1 ? names[0].split("[,]+") : names;
		return rec -> IRecord.rb(_names).get(rec.toArray());
	}

	/**
	 * 非更新式添加,即在一个复制品上添加新的键值数据<br>
	 * 添加键值对数据。 在 指定的记录对象 r 之后 将 tups 之中的数据给予 追加，<br>
	 * 若是 tups为单个 元素 且为 Iterable 类型[aa] <br>
	 * 则在 aa 中的 每个元素之前 追加 r 记录作为前缀。 <br>
	 * 比如 (address:shanghai) concat [(name:zhangsan),(name:lisi)] 生成的结果 是 <br>
	 * [(address:shanghai,name:zhangsan),(address:shanghai,name:lisi)] <br>
	 * (address:shanghai) concat ( name:zhangsan) 生成的 结果 <br>
	 * [(address:shanghai,name:zhangsan)]
	 * 
	 * @param tups 键值对集合
	 * @return 追加了 新键值对儿的变换函数: r->r1
	 */
	@SuppressWarnings("unchecked")
	public static Function<IRecord, Stream<IRecord>> concatS(final Object... tups) {
		return r -> {
			final var rec = r.duplicate(); // 复制品
			if (tups.length == 1) {
				if (tups[0] != null && tups[0] instanceof IRecord || tups[0] instanceof Map) {
					Optional.ofNullable(tups[0] instanceof Map ? tups[0] : null).map(IRecord::REC)
							.orElse((IRecord) tups[0]).forEach(p -> rec.add(p));
				} else if (tups[0] != null && tups[0] instanceof Tuple2) {// if
					rec.add(((Tuple2<Object, Object>) tups[0]).fmap1(Object::toString));
				} else if (tups[0] != null && tups[0] instanceof Iterable) {// if 集合类的 数据连接
					return StreamSupport.stream(((Iterable<Object>) tups[0]).spliterator(), true)
							.map(e -> e instanceof IRecord ? (IRecord) e : IRecord.REC(e)) //
							.map(rec::derive); //
				} // if
			} else {
				IRecord.slidingS(Arrays.asList(tups), 2, 2, true).forEach(aa -> {
					rec.add(aa.get(0).toString(), aa.get(1));
				}); // forEach
			} // if
			return Stream.of(rec);
		};
	}

	/**
	 * 滑动窗口的T元素归集器<br>
	 * 返回非齐次窗口长度<br>
	 * flag:true,[1,2,3] -> [(1,2),(2,3)] <br>
	 * flag:false,[1,2,3] -> [(1,2),(2,3),(3,null)] <br>
	 * 
	 * @param <T>  流元素类型
	 * @param flag 是否返回齐次窗口, true:齐次窗口,false:非齐次窗口
	 * @return 滑动窗口的T元素归集器
	 */
	public static <T> Collector<T, ?, Stream<Tuple2<T, T>>> tup2slidingclcS(final boolean flag) {
		return IRecord.slidingclc(2, 1, flag, List::stream, Tuple2::P);
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
	public static <T> Collector<T, ?, List<List<T>>> slidingclc(final int size, final int step) {
		return IRecord.slidingclc(size, step, false);
	}

	/**
	 * 滑动窗口的T元素归集器 <br>
	 * 返回非齐次窗口长度<br>
	 * 
	 * @param <T>      流元素类型
	 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt;&gt;变换
	 *                 List&lt;DFrame&gt; 这样的类型
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
	public static <T> Collector<T, ?, List<List<T>>> slidingclc(final int size, final int step, final boolean flag) {
		return IRecord.slidingclc(size, step, flag, e -> e);
	}

	/**
	 * 滑动窗口的T元素归集器
	 * 
	 * @param <T>      流元素类型
	 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt;&gt;变换
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
	 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt;&gt;变换
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
		final var ai = new AtomicInteger(0);
		return Collector.of(() -> new ArrayList<List<T>>(), (wnds, a) -> {
			if (ai.getAndIncrement() % step == 0) { // 达到步长位置则添加一个数据窗口
				wnds.add(new ArrayList<>()); // 添加一个数据窗口
			}
			final var n = wnds.size(); // 结果集合的长度
			for (var i = n - 1; i >= 0; i--) { // 从后前向依次遍历 追加数据元素，直到长度达到 窗口长度 size 位置
				final var wnd = wnds.get(i); // 窗口数据
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
					var i = aa.size() - 1; // 末尾元素的下标索引
					while (i > 0) {
						if (aa.get(i).size() >= size) { // 直到现在子列表长度等于size的的最大下标索引
							break;
						} else { // 长度小于窗口长度size 的继续向前移动
							i--;
						} // if
					} // while

					final var homo_aa = i == aa.size() - 1 ? aa : aa.subList(0, i + 1); // 提取齐整的列表集合
					return homo_aa; // 齐整列表
				} // if
			}).map(ll -> {
				final var frames = ll.stream().map(as_frame).collect(Collectors.toList());
				return finisher.apply(frames);
			}).get(); // of
		}); // Collector.of
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static List<Tuple2<String, List<Object>>> rows2tuples(final List<IRecord> recs) {
		return IRecord.rows2tupleS(recs).collect(Collectors.toList());
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	@SuppressWarnings("unchecked")
	static Stream<Tuple2<String, List<Object>>> rows2tupleS(final List<IRecord> recs) {
		return IRecord.ROWS2COLS(recs).map(e -> e.fmap2(x -> (List<Object>) x));
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord ROWS2COLS(final List<IRecord> recs) {
		return IRecord.ROWS2COLS(recs, null);
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @param keys 字段键名序列，键名间用逗号分隔,如果为null, 采用recs 的一个元素的键名集合。
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord ROWS2COLS(final List<IRecord> recs, final List<String> keys) {
		if (recs == null || recs.size() < 1) {
			return REC();
		}
		final var first = recs.stream().filter(Objects::nonNull).findFirst().orElse(null);
		if (first == null) {
			return REC();
		}
		final var kk = (keys == null || keys.size() < 1 ? first.keys() : keys).toArray(String[]::new);
		final var mm = new LinkedHashMap<String, List<Object>>();
		recs.forEach(line -> {
			for (var k : kk) {
				mm.compute(k, (_k, v) -> {
					if (v == null)
						v = new LinkedList<>();
					v.add(line.get(_k));
					return v;
				});
			}
			;// for
		});

		return REC(mm);
	}

	/**
	 * Record类型的T元素归集器
	 * 
	 * @param <X> Tuple2 的第二元素类型
	 * @param <T> 元素类型
	 * @param <U> 元组的1#位置占位符元素类型
	 * @return IRecord类型的T元素归集器
	 */
	public static <X, T extends Tuple2<String, X>, U> Collector<T, ?, IRecord> recclc() {
		return IRecord.recclc(e -> e);
	}

	/**
	 * Record类型的T元素归集器
	 * 
	 * @param <T>    元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param mapper Tuple2 类型的元素生成器 t-&gt;(str,u)
	 * @return IRecord类型的T元素归集器
	 */
	public static <T, U> Collector<T, ?, IRecord> recclc(final Function<T, Tuple2<String, U>> mapper) {
		return Collector.of((Supplier<List<T>>) ArrayList::new, List::add, (left, right) -> {
			left.addAll(right);
			return left;
		}, (ll) -> { // finisher
			final var rec = REC(); // 空对象
			ll.stream().map(mapper).forEach(rec::add);
			return rec;
		}); // Collector.of
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T>    归集的元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param <X>    元组的2#位置占位符元素元素类型
	 * @param mapper Tuple2 类型的元素生成器 t-&gt;(u,x)
	 * @return IRecord类型的T元素归集器
	 */
	public static <T, U, X> Collector<T, ?, IRecord> rclc(final Function<T, Tuple2<U, X>> mapper) {
		return IRecord.recclc(e -> mapper.apply(e).fmap1(o -> o + ""));
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T>   归集的元素类型
	 * @param start 序列号的开始编码
	 * @return IRecord类型的T元素归集器,序列号编码的元素键名，序列号从 start 开始
	 */
	public static <T> Collector<T, ?, IRecord> rclc(int start) {
		return IRecord.rclc(Tuple2.snbuilder(start));
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T> 归集的元素类型
	 * @return IRecord类型的T元素归集器,序列号编码的元素键名,序列号从0开始
	 */
	public static <T> Collector<T, ?, IRecord> rclc() {
		return IRecord.rclc(0);
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T> 归集的元素类型
	 * @return IRecord类型的T元素归集器,元素的名称编码采用excel的列名命名法
	 */
	public static <T> Collector<T, ?, IRecord> rclc2() {
		return IRecord.rclc2(IRecord::excelname);
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T>  归集的元素类型
	 * @param keys 键名列表,用逗号分隔
	 * @return IRecord类型的T元素归集器,序列号编码的元素键名,序列号从0开始
	 */
	public static <T> Collector<T, ?, IRecord> rclc2(final String keys) {
		final var kk = keys.split(",");
		return IRecord.rclc2(kk);
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T>  归集的元素类型
	 * @param keys 键名列表,用逗号分隔
	 * @return IRecord类型的T元素归集器,序列号编码的元素键名,序列号从0开始
	 */
	public static <T> Collector<T, ?, IRecord> rclc2(final String... keys) {
		final var n = keys.length;
		return IRecord.rclc2(i -> keys[i % n]);
	}

	/**
	 * Record类型的T元素归集器 简化版本
	 * 
	 * @param <T>      归集的元素类型
	 * @param idx2name 索引键名函数,把索引序号转换成文字名称 idx:从0开始->name字符串名称
	 * @return IRecord类型的T元素归集器,序列号编码的元素键名,序列号从0开始
	 */
	public static <T> Collector<T, ?, IRecord> rclc2(final Function<Integer, String> idx2name) {
		final var ai = new AtomicInteger();

		final Function<T, Tuple2<String, Object>> mapper = t -> {
			final var i = ai.getAndIncrement();
			return P(idx2name.apply(i), t);
		};

		return IRecord.recclc(mapper);
	}

	/**
	 * Map类型的T元素归集器
	 * 
	 * @param <K>    键类型
	 * @param <T>    元素类型
	 * @param <U>    元组的1#位置占位符元素类型
	 * @param mapper Tuple2 类型的元素生成器 t-&gt;(k,u)
	 * @return IRecord类型的T元素归集器
	 */
	public static <T, K, U> Collector<T, ?, Map<K, List<U>>> mapclc(final Function<T, Tuple2<K, U>> mapper) {
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
	 * @param mapper Tuple2 类型的元素生成器 t-&gt;(k,u)
	 * @param biop   二元运算算子 (u,u)->u
	 * @return IRecord类型的T元素归集器 (LinkedHashMap结构,以保持健名顺序)
	 */
	public static <T, K, U> Collector<T, ?, Map<K, U>> mapclc2(final Function<T, Tuple2<K, U>> mapper,
			final BinaryOperator<U> biop) {
		return Collector.of((Supplier<Map<K, List<U>>>) LinkedHashMap::new, (tt, t) -> {
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
	 * @param mapper Tuple2 类型的元素生成器 t-&gt;(k,u)
	 * @return IRecord类型的T元素归集器
	 */
	public static <T, K, U> Collector<T, ?, Map<K, U>> mapclc2(final Function<T, Tuple2<K, U>> mapper) {
		return IRecord.mapclc2(mapper, (a, b) -> b); // 使用新值覆盖老值。
	}

	/**
	 * Map类型的T元素归集器
	 * 
	 * @param <K> 键类型
	 * @param <T> 元素类型
	 * @return IRecord类型的T元素归集器
	 */
	public static <K, T> Collector<? super Tuple2<K, T>, ?, Map<K, T>> mapclc2() {
		return IRecord.mapclc2(e -> e, (a, b) -> b); // 使用新值覆盖老值。
	}

	/**
	 * 把一个IRecord 函数 转换成一个 键值对
	 * 
	 * @param <K>   键名类型
	 * @param <T>   值类型
	 * @param keyer 键名生成函数,t-&gt;k
	 * @return record-&gt;(k,t)
	 */
	public static <T, K> Function<T, Tuple2<K, T>> keyit(final Function<T, K> keyer) {
		return r -> Tuple2.of(keyer.apply(r), r);
	}

	/**
	 * 列表顺序调转
	 * 
	 * @param <T>  列表元素类型
	 * @param list 列表数据
	 * @return 收尾颠倒后的数据列表
	 */
	public static <T> List<T> reverse(final List<T> list) {
		Collections.reverse(list);
		return list;
	}

	/**
	 * 视rootnode为一个树形结构的根节点,即每个 键值元组(K,V) 的 值V可以是一个 递归 嵌套的IRecord <br>
	 * 对rootnode做深度遍历,并把遍历结果 写入 到一个 List&lt;U&gt; 结果之中 <br>
	 * 
	 * @param <T>      元素类型
	 * @param <U>      目标结果类型
	 * @param rootnode 待遍历的 IRecord
	 * @param mapper   值变换函数 节点变换函数 (path:路径,node:T类型的节点值)-&gt;u:U 类型的变换结果
	 * @return 深度遍历的结果序列 [u]
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Stream<U> hierachizeS(final IRecord rootnode, final BiFunction<List<String>, T, U> mapper) {
		final var stack = new Stack<Tuple2<String, Object>>();
		final var level_atom = new AtomicInteger(0); // 层级
		final Tuple2<String, Object> LEVEL_END = Tuple2.of(null, null); // 层级结尾标记
		final Predicate<Tuple2<String, ?>> level_end_predicate = e -> e.equals(LEVEL_END); // 判断是否读到了层级的结束节点。
		final Consumer<List<? extends Tuple2<String, Object>>> stack_put = tups -> {
			stack.push(LEVEL_END); // 加入 层级间隔标记
			stack.addAll(tups);
			level_atom.incrementAndGet(); // 增加层级
		};
		final var path = new ArrayList<String>(); // 层级路径的缓冲
		final Function<String, List<String>> path_update = s -> {
			final var n = level_atom.get();
			if (path.size() < n) { // 按照层级从根节点向子节点进行增长递增。
				path.add(s);
			} else {
				path.set(n - 1, s);
			} //
			final var _path = ((List<String>) path.clone()).subList(0, path.size() < n ? path.size() : n);
			return _path;
		}; // path_update
		final List<String> rootpath = new ArrayList<>(); // 根节点路径

		stack_put.accept(IRecord.reverse(rootnode.tuples())); // 把堆栈数据访问顺序调节为自然序
		return Stream.iterate(Tuple2.of(rootpath, (Object) rootnode), prev -> !stack.empty(), e -> {
			var p = stack.pop(); // 读取元素
			List<String> _path = path_update.apply(p._1); // 更新节点路径
			while (!stack.empty() && level_end_predicate.test(p)) {
				level_atom.decrementAndGet(); // 读到了层级的结尾标记,减小层级
				p = stack.pop();
				_path = path_update.apply(p._1); // 更新节点路径
			}
			final var node = p._2 instanceof Map ? REC((Map<?, ?>) p._2) : p._2; // 把Map转化成 IRecord 来进行处理
			if (node instanceof IRecord) { // 子节点继续遍历
				stack_put.accept(IRecord.reverse(((IRecord) node).tuples())); // 把堆栈数据访问顺序调节为自然序
			} // if
			return Tuple2.of(_path, node);
		}).map(p -> mapper.apply(p._1, (T) p._2));
	}

	/**
	 * 列名称： 从0开始 <br>
	 * 0->A,1->B;2->C;....,25->Z,26->AA
	 * 
	 * @param n 数字
	 * @return 类似于EXCEL的列名称
	 */
	public static String excelname(final int n) {
		// 字母表
		String alphabetics[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
		return nomenclature(n, alphabetics);
	}

	/**
	 * 把 一个数字 n转换成一个字母表中的数值(术语）<br>
	 * 
	 * 在alphabetics中:ABCDEFGHIJKLMNOPQRSTUVWXYZ <br>
	 * 比如:0->A,1-B,25-Z,26-AA 等等
	 * 
	 * @param n      数字 从0开始
	 * @param alphas 字母表
	 * @return 生成exel式样的名称
	 */
	public static String nomenclature(final Integer n, final String[] alphas) {
		final int model = alphas.length;// 字母表尺寸
		final List<Integer> dd = new LinkedList<>();
		Integer num = n;

		do {
			dd.add(num % model);
			num /= model;// 进入下回一个轮回
		} while (num-- > 0); // num-- 使得每次都是从A开始，即Z的下一个是AA而不是BA

		// 就是这个简单算法我想了一夜,我就是不知道如何让10位每次都从0开始。
		Collections.reverse(dd);

		return dd.stream().map(e -> alphas[e]).collect(Collectors.joining(""));
	}

	/**
	 * 数据清理 <br>
	 * 对于 非集合类型: 不做变换 <br>
	 * 对于 集合类型：数组类型，Collection,Iterable 类型 则逐个元素进行处理 进行递归处理,并 给予 拼装成 ArrayList 结构
	 * 
	 * @param obj 元素类型
	 * @return 处理后的元素类型
	 */
	public static Object tidy(final Object obj) {
		return IRecord.tidy(obj, e -> e);
	}

	/**
	 * 数据清理 <br>
	 * 对于 非集合类型: 调用 mapper 进行 变换， <br>
	 * 对于 集合类型：数组类型，Collection,Iterable 类型 则逐个元素进行处理 进行递归处理,并 给予 拼装成 ArrayList 结构
	 * 
	 * @param obj         元素数值
	 * @param deep_mapper 深度深度变换函数,元素值的变换函数 {obj:非集合类型}-&gt;new_value
	 * @return 扁平化处理后数据
	 */
	public static Object tidy(final Object obj, final Function<Object, Object> deep_mapper) {
		if (obj == null) { // 空值类型
			return null;
		} else if (obj instanceof Map) { // 数组类型
			return deep_mapper.apply(obj);
		} else if (obj instanceof IRecord) { // 数组类型
			return deep_mapper.apply(obj);
		} else if (obj instanceof Collection || obj.getClass().isArray() || obj instanceof Iterable) { // 集合类型的处理
			final var ll = new ArrayList<Object>();
			final Collection<?> cc = obj.getClass().isArray() // 是否数组类型
					? Arrays.asList((Object[]) obj) // 转换列表
					: obj instanceof Iterable // 可遍历类型
							? MyRecord.iterable2list((Iterable<?>) obj, IRecord.MAX_SIZE) // 转换成列表
							: (Collection<?>) obj; // 直接返回原来类型

			for (final var c : cc) {
				ll.add(tidy(c, deep_mapper)); // 元素清理
			}

			return ll; // 返回列表类型
		} else { // 其他值类型的处理
			return deep_mapper.apply(obj);
		} // if
	}

	/**
	 * 前缀函数
	 * 
	 * @param prefix 前缀
	 * @return 为指定名称添加 前缀
	 */
	public static Function<String, String> prefix(final String prefix) {
		return name -> IRecord.FT("$0$1", prefix, name);
	}

	/**
	 * 后缀函数
	 * 
	 * @param suffix 前缀
	 * @return 为指定名称添加 后缀
	 */
	public static Function<String, String> suffix(final String suffix) {
		return name -> IRecord.FT("$0$1", name, suffix);
	}

	/**
	 * 为指定的字符串添加一个前缀与后缀
	 * 
	 * @param prefix 前缀
	 * @param suffix 后缀
	 * @return 为指定名称添加 前缀 与 后缀
	 */
	public static Function<String, String> decorate(final String prefix, final String suffix) {
		return name -> IRecord.FT("$0$1$2", prefix, name, suffix);
	}

	/**
	 * 位数精度:fraction的别名
	 * 
	 * @param n 位数的长度
	 * @return 小数位置的精度
	 */
	public static Function<Object, String> frt(final int n) {
		return fraction(n);
	}

	/**
	 * 位数精度
	 * 
	 * @param n 位数的长度
	 * @return 小数位置的精度
	 */
	public static Function<Object, String> fraction(final int n) {
		return v -> {
			if (v == null)
				return "(null)";
			var line = "{0}";// 数据格式化
			if (v instanceof Date) {
				line = "{0,Date,yyyy-MM-dd HH:mm:ss}"; // 时间格式化
			} else if (v instanceof Number) {
				line = "{0,number," + ("#." + "#".repeat(n)) + "}"; // 数字格式化
			} // if
			return MessageFormat.format(line, v);
		};// cell_formatter
	}

	/**
	 * 最大数字数量,默认为10000
	 */
	public static int MAX_SIZE = 10000; // 最大的数量

	/**
	 * IRecord 构建器
	 * 
	 * @author gbench
	 *
	 */
	class Builder {

		/**
		 * 构造IRecord构造器
		 * 
		 * @param keys 键名列表的迭代器
		 */
		public <T> Builder(final Iterable<T> keys) {
			this.keys = StreamSupport.stream(keys.spliterator(), false).limit(10000).map(e -> e + "")
					.collect(Collectors.toList());
		}

		/**
		 * 构造IRecord构造器
		 * 
		 * @param keys 键名列表
		 */
		public Builder(final List<String> keys) {
			this.keys = new ArrayList<>(keys);
		}

		/**
		 * 构造 IRecord <br>
		 * 按照构建器的 键名序列表，依次把objs中的元素与其适配以生成 IRecord <br>
		 * {key0:objs[0],key1:objs[1],key2:objs[2],...}
		 * 
		 * @param <T>  元素类型
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

			final int n = objs.length;
			final var size = this.keys.size();
			final var data = new LinkedHashMap<String, Object>();

			for (int i = 0; n > 0 && i < size; i++) {
				final var key = keys.get(i);
				final var value = objs[i % n];
				data.put(key, value);
			} // for

			return this.build(data);
		}

		/**
		 * 创建IRecord
		 * 
		 * @param map 键名 键值 序列对儿
		 * @return IRecord
		 */
		final public IRecord build(final Map<?, ?> map) {
			return IRecord.REC(map);
		}

		/**
		 * 键名序列 的 变换函数
		 * 
		 * @param <U>    结果类型
		 * @param mapper 键名序列的变换函数 [k]:键名列表->u
		 * @return U 键名序列的某种变换
		 */
		public <U> U keysOf(final Function<List<String>, U> mapper) {
			return mapper.apply(keys);
		}

		private List<String> keys = new ArrayList<>();
	} // Builders

} // IRecord