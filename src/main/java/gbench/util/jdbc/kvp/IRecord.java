package gbench.util.jdbc.kvp;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import gbench.util.jdbc.Jdbcs;
import gbench.util.type.Times;
import gbench.util.tree.Node;
import gbench.util.jdbc.sql.SQL;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Properties;
import java.util.Date;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LongSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Stream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.StreamSupport;

import static gbench.util.jdbc.Jdbcs.*;

/**
 * IRecord 记录结构(键值集合) <br>
 * IRecord 是键值对儿(Key Value Pairs KVPs)的集合 (也可以理解键-值集合 KeyValues kvs)。<br>
 * 其基本单元为一个键值对儿KVP,有时候简写成 pair或p。KVP 是一个(String,Object) <br>
 * 的二元组，需要对于一个kvp的value他是可以为任何类型的。因此也是可以为IRecord的类型， <br>
 * 所以IRecord是一个可以进行递归定义的数据结构。由于IRecord的设计思想来源于LISP的列表。<br>
 * 因此他具有丰富的表达能力。可以表达 列表,树,图 等基础结构。因此可以很容易拼装成其他数据结构。<br>
 * 是一种既具有原子性 又具有集合性的 数据结构。非常适合表达动态定义的概念,同时对静态概念也有很好 <br>
 * 的变换方法 ,比如: mutate,toTarget(有损变换,部分转换),cast(整体转换), <br>
 * OBJINIT,OBJ2REC,OBJ2KVS,P(点数据)等方法，可以很方便的在动态概念与静态概念的之间<br>
 * 的转换。静态概念：结构固定的数据结构，即一旦概念确定，结构的数据成员就拥有了确定类型和数量的概念。<br>
 * 与静态概念相对应 成员的类型与数量不确定的数据的结构就是动态概念。<br>
 * <br>
 * 由于record 就是模仿 LISP语言中的列表（增强了key-value的树形特征）。所以 IRecord <br>
 * 理论上是可以标识任何数据结构的。用一句话来形容 IRecord:IRecord 就是结构化的数据的意思。<br>
 * IRecord 需要具有递归的含义，也就是IRecord的元素也可以是IRecord. <br>
 * IRecord 不仅表现出结构化，还要表现出类似于LISP 列表计算。也就是可以依附于符号(key)<br>
 * 的绑定类型,而表现出值的计算的能力。比如:<br>
 * public default &lt;T,U&gt; U get(String key,T t,Function&lt;T,U&gt; t2u)，<br>
 * 就把一个key,绑定到一个Function&lt;T,U&gt; t2u 然后对vlaue 进行计算。<br>
 * 另外key->value 中的value 不仅可以是数值,还可以是函数。<br>
 * 这样key:value 就不是一个静态意义，而是一个动态的概念了。比如：<br>
 * &nbsp;interface FOO{ void foo();};<br>
 * &nbsp;REC("foo",(FOO)()->{});<br>
 * 
 * @author gbench
 *
 */
@JsonSerialize(using = IRecordSerializer.class)
@JsonDeserialize(using = IRecordDeserializer.class)
public interface IRecord extends Serializable, Comparable<IRecord>, Iterable<KVPair<String, Object>> {

	/**
	 * 添加一个新的键值对儿(key,value).根据具体的IRecord的不同 对于已经存在的key：若允许同名字段，则添加新的键值对儿，<br>
	 * 若不允许同名key 则修改key的值位value
	 * 
	 * @param key   字段名: key 会调用 toString作为进行键名转换
	 * @param value 字段值 任意对象 可以为null
	 * @return 当前的IRecord 以保证可以链式编程。
	 */
	IRecord add(final Object key, final Object value);

	/**
	 * 设置键名key的值为vlaue,如果key不存在则添加，否则修改键名key的值为value
	 * 
	 * @param key   键名
	 * @param value 键值 可以为null
	 * @return 当前的IRecord 以保证可以链式编程。
	 */
	IRecord set(final String key, final Object value);

	/**
	 * 提取字段key 所对应的值
	 * 
	 * @param key 键名
	 * @return 键名字段key 所对应的值
	 */
	Object get(final String key);

	/**
	 * 字段key 所对应的值列表，如果存在多个同名的key 则把这些key的值合并成一个列表。
	 * 
	 * @param key 字段名
	 * @return 字段key 所对应的值列表，如果存在多个同名的key 则把这些key的值合并成一个列表。
	 */
	List<Object> gets(final String key);

	/**
	 * 键值序{(k0,v0),(k1,v1),...}
	 * 
	 * @return 键值序{(k0,v0),(k1,v1),...}
	 */
	Stream<KVPair<String, Object>> tupleS();

	/**
	 * 键值序{(k0,v0),(k1,v1),...}
	 * 
	 * @return 键值序{(k0,v0),(k1,v1),...}
	 */
	Map<String, Object> toMap();

	/**
	 * 复制当前IRecord，即做一个当前实例的拷贝:这是一个浅拷贝。仅当对字段值进行拷贝，不对字段值的属性进行进一步拷贝。
	 */
	IRecord duplicate();// 复制克隆

	/////////////////////////////////////////////////////////////////////
	// 以下是IRecord 的默认方法区域
	/////////////////////////////////////////////////////////////////////

	/**
	 * 添加一个新的键值对儿(key,value).根据具体的IRecord的不同 对于已经存在的key：若允许同名字段，<br>
	 * 则添加新的键值对儿，若不允许同名key则修改key的值位value <br>
	 * 
	 * @param p 键值对儿对象
	 * @return 当前的IRecord 以保证可以链式编程。
	 */
	default <P extends Tuple2<String, ?>> IRecord add(final P p) {
		return this.add(p._1(), p._2());
	}

	/*
	 * 增加键值列表
	 * 
	 * @param kvs 键,值序列:key0,value0,key1,value1,....
	 * 
	 * @return 当前的IRecord 以保证可以链式编程。
	 */
	default IRecord add(final Object... kvs) {
		for (int i = 0; i < kvs.length - 1; i += 2) {
			final var key = kvs[i];
			final var value = kvs[i + 1];
			this.add(key, value);
		} // for
		return this;
	}

	/**
	 * 把键值序列 变换成一个T类型的数据流。
	 * 
	 * @param <T>    结果的数据类型
	 * @param mapper kvp 的变换函数
	 * @return T 类型的数据流
	 */
	default <T> Stream<T> tupleS(final Function<KVPair<String, Object>, T> mapper) {
		@SuppressWarnings("unchecked")
		final Function<KVPair<String, Object>, T> fmapper = mapper == null ? e -> (T) e : mapper;// 变换函数
		return this.tupleS().map(fmapper);
	}

	/**
	 * 键值序{(k0,v0),(k1,v1),...}
	 * 
	 * @return 键值序{(k0,v0),(k1,v1),...}
	 */
	default List<KVPair<String, Object>> kvs() {
		return this.tupleS().toList();
	}

	/**
	 * 分割成键值对的序列
	 * 
	 * @param key   键名
	 * @param value 值名
	 * @return [[key:key0,value:value0],[key:key1,value:value1],...]
	 */
	default List<IRecord> kvs2(final String key, final String value) {
		return this.tupleS().map(e -> rb("key,value").get(e._1(), e._2())).toList();
	}

	/**
	 * 分割成键值对的序列
	 * 
	 * @param names 键与值名序列,笔记间用英文逗号','或';'分割,如:key,value
	 * @return [{key:key0,value:value0},{key:key1,value:value1},...]
	 */
	default List<IRecord> kvs2(final String names) {
		final var ss = names.split(",");
		return ss.length < 2 ? kvs2() : kvs2(ss[0], ss[1]);
	}

	/**
	 * 分割成键值对的序列
	 * 
	 * @return [{key:key0,value:value0},{key:key1,value:value1},...]
	 */
	default List<IRecord> kvs2() {
		return this.kvs2("key", "value");
	}

	/**
	 * 分割成键值对的序列,键与值分别用列表进行组织
	 * 
	 * @return {keys:[key0,key1,...],values:[value0,value1,...]}
	 */
	default IRecord kvs3() {
		return kvs3("keys", "values");
	}

	/**
	 * 分割成键值对的序列,键与值分别用列表进行组织
	 * 
	 * @return [[key:key0,value:value0],[key:key1,value:value1],...]
	 */
	default IRecord kvs3(final String keys, final String values) {
		return REC(keys, this.keys(), values, this.values());
	}

	/**
	 * 分割成键值对的序列
	 * 
	 * @param names 键与值名序列,笔记间用英文逗号','或';'分割,如:keys,values
	 * @return [{key:key0,value:value0},{key:key1,value:value1},...]
	 */
	default IRecord kvs3(final String names) {
		final var ss = names.split("[,;]");
		return ss.length < 2 ? kvs3() : kvs3(ss[0], ss[1]);
	}

	/**
	 * 键值对儿的个数
	 * 
	 * @return 键值对儿的个数
	 */
	default int size() {
		return this.keys().size();
	}

	/**
	 * 是否含有key字段
	 * 
	 * @param key 字段名称
	 * @return boolean 包含true:不包含false
	 */
	default boolean has(final String key) {
		if (key == null || this.keys().size() < 1)
			return false;
		return this.keys().contains(key);
	}

	/**
	 * 判断是否为一个空记录：IRecord.如果没有一个key就是一个空对象。
	 * 
	 * @return 空记录标记。
	 */
	default boolean isEmpty() {
		return this.keys().size() <= 0;
	}

	/////////////////////////////////////////////////////////////////////
	// 以下是IRecord 的默认方法区域
	/////////////////////////////////////////////////////////////////////

	/**
	 * 提取第一个键名元素数据
	 * 
	 * @param <T>           默认值类型
	 * @param default_value 默认值
	 * @return T类型值
	 */
	default <T> T car(T default_value) {
		@SuppressWarnings("unchecked")
		final var obj = (T) this.get(0);
		return obj == null ? default_value : obj;
	}

	/**
	 * 提取第一个键名元素数据
	 * 
	 * @return 提取第一个键名元素数据
	 */
	default Object car() {
		return this.car(null);
	}

	/**
	 * 提取除掉第一元素之后的内容
	 * 
	 * @return IRecord
	 */
	default IRecord cdr() {
		return IRecord.KVS2REC(this.tupleS().skip(1));
	}

	/**
	 * 提取字段key 所对应的值
	 * 
	 * @param <T>           值类型
	 * @param key           键名
	 * @param default_value 默认值
	 * @return 键名字段key 所对应的值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(String key, T default_value) {
		final var obj = (T) this.get(key);
		return obj == null ? default_value : obj;
	}

	/**
	 * 提取指定類型的字段<br>
	 * 提取字段並把字段指定轉換到targetType
	 * 
	 * @param <T>        目標類型
	 * @param key        字段名，鍵名
	 * @param targetType 目標類型
	 * @return 键名字段key 所对应的值
	 */
	default <T> T getT(final String key, final Class<T> targetType) {
		final var targetValue = REC(0, this.get(key)).toTarget(targetType);
		return targetValue;
	}

	/**
	 * 提取指定類型的字段 提取字段並把字段指定轉換到targetType <br>
	 * 
	 * @param <T>        目標類型
	 * @param idx        字段名，鍵名 索引 從0開始
	 * @param targetType 目標類型
	 * @return 键名字段索引 idx 所对应的值
	 */
	default <T> T getT(Integer idx, final Class<T> targetType) {
		final var targetValue = REC(0, this.get(idx)).toTarget(targetType);
		return targetValue;
	}

	/**
	 * 提取字段idx索引 所对应的值
	 * 
	 * @param <T>           结果类型
	 * @param idx           键名索引从0开始
	 * @param default_value 默认值
	 * @return 键名字段key 所对应的值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(int idx, T default_value) {
		final var obj = (T) this.get(this.keyOfIndex(idx));
		return obj == null ? default_value : obj;
	}

	/**
	 * 按照 索引进行字段 取值
	 * 
	 * @param idx 从0开始
	 * @return idx 所标识的字段的值,非法索引(超出范围) 返回null
	 */
	default Object get(final Integer idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : get(key);
	}

	/**
	 * 使用bb 筛选键值对儿: 比如 提取水果的所有子集 <br>
	 * final var rec = STRING2REC("苹果,西瓜,草莓,哈密瓜,鸭梨");// 水果列表 <br>
	 * cph2(RPT(rec.size(),L(true,false))).map(e->rec.gets(e.bools())).map(e->MFT("{0}",e.values()))
	 * <br>
	 * .forEach(System.out::println); <br>
	 * 
	 * @param bb 下标选择器,当 bb 为 null或 长度小于等于0返回空记录，即 REC()
	 * @return bb 所筛选出来的对应字段
	 */
	default IRecord gets(final Boolean... bb) {
		final IRecord rec = REC();
		if (bb != null) {
			final var n = bb.length;
			final var ai = new AtomicInteger(0);
			if (n <= 0) {
				return rec;
			} else {
				this.kvs().forEach(p -> {
					final var i = ai.getAndIncrement() % n;
					if (bb[i]) {
						rec.add(p._1(), p._2());
					}
				});// forEach
			}
		} // if
		return rec;
	}

	/**
	 * 这是按照indexes 所指定的键值进行数据过滤。默认过滤空值字段（该字段的值value为null,或者非法的索引位置)
	 * 
	 * @param indexes 提取的字段键名索引序列,非负整数从0开始
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord gets(final Number... indexes) {
		final var kk = Arrays.stream(indexes).map(i -> this.keyOfIndex(i.intValue())).filter(Objects::nonNull)
				.toArray(String[]::new);
		final var rec = new SimpleRecord();
		for (String k : kk) {
			final var v = this.get(k);
			if (v != null)
				rec.add(k, v);
		}

		return rec;
	}

	/**
	 * 把key字段的值强制装换成 T 类型。类型强转失败时返回null
	 * 
	 * @param <T>         参照对象的类的类型
	 * @param key         字段名称
	 * @param targetClass 参照对象的类
	 * @return T 类型数据
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(final String key, final Class<T> targetClass) {
		return map(key, e -> {
			T t1 = null;
			try {
				t1 = (T) e;
			} catch (Exception ignored) {
			}
			return t1;
		});
	}

	/**
	 * 把index 字段索引位置的值强制装换成 T 类型。类型强转失败时返回null
	 * 
	 * @param <T>         结果类型
	 * @param idx         字段索引从0开始
	 * @param targetClass 参照对象
	 * @return idx 所对应的值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(final int idx, final Class<T> targetClass) {
		return map(idx, e -> {
			T t1 = null;
			try {
				t1 = (T) e;
			} catch (Exception ignored) {
			}
			return t1;
		});
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1,2,3)).aa("numbers",Integer.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param <T>         参照类的类型
	 * @param key         字段名称
	 * @param targetClass 类型参照类
	 * @return idx 所对应的值
	 */
	@SuppressWarnings("unchecked")
	default <T> T[] aa(final String key, final Class<T> targetClass) {
		final var tc = Optional.ofNullable(targetClass).orElse((Class<T>) Object.class); // 目标类型
		final var isnum_t = Number.class.isAssignableFrom(tc); // 目标类型是否是数值类型
		final Function<Number, Object> cast_number = num -> { // 数值类型转换
			final var tcname = tc.getSimpleName(); // 目标类型名
			final var _num = switch (tcname) { // 目的类型选择
			case "int", "Integer" -> num.intValue();
			case "double", "Double" -> num.doubleValue();
			case "long", "Long" -> num.longValue();
			case "short", "Short" -> num.shortValue();
			case "byte", "Byte" -> num.byteValue();
			case "float", "Float" -> num.floatValue();
			default -> num;
			};
			return _num;
		}; // cast_number 数值类型转换
		final Function<Object, Object> cast_any = o -> { // 通用类型转换
			if (o != null) { // 非空元素
				final var oc = o.getClass(); // 源类型
				if (!Objects.equals(oc, tc)) { // 目的类型
					if (isnum_t && o instanceof Number num) { // 源类型和目的类型都是数值
						return cast_number.apply(num);
					} else if (isnum_t && o instanceof String s) { // 源类是字符串 目的类型是 数值
						final var num = IRecord.obj2dbl().apply(s);
						return cast_number.apply(num);
					} else if (Objects.equals(tc, String.class)) { // 目标类型市字符串类型
						return String.valueOf(o);
					} else { // 其他类型
						return o;
					} // if
				} else { // 原类型与目标类型相同
					return o;
				} // if
			} else { // 空元素 o is null
				return o;
			} // if
		}; // cast_any 通用类型转换

		return map(key, e -> {
			T[] tt = null;
			if (null != e) {
				final var clazz = e.getClass();
				try {
					if (tc.isAssignableFrom(clazz)) {
						tt = (T[]) e;
					} else {
						final Stream<Object> dataS; // 数据流
						if (clazz.isArray()) { // 数组类型
							dataS = Arrays.stream((Object[]) e);
						} else if (e instanceof Iterable<?> itr) { // 可遍历类型
							dataS = (Stream<Object>) StreamSupport.stream(itr.spliterator(), false);
						} else { // 其他类型
							dataS = Stream.of(e);
						}
						tt = dataS.map(cast_any).toArray(n -> (T[]) Array.newInstance(tc, n));
					} // if
				} catch (Exception ignored) {
					// do nothing
				} // try
			} // if
			return tt;
		}); // map
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1,2,3)).aa("numbers",Integer.class); 可以获取得到
	 * 数组类型。<br>
	 * 把index 字段索引位置的值强制装换成 T 类型。类型强转失败时返回null <br>
	 * 
	 * @param <T>         参照类的类型
	 * @param idx         字段索引序号 从0开始
	 * @param targetClass 类型参照类
	 * @return idx 所对应的值
	 */
	default <T> T[] aa(final int idx, final Class<T> targetClass) {
		return this.aa(keyOfIndex(idx), targetClass);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1d,2d,3d)).aa("numbers",Double.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param key 字段名称 targetClass 类型参照类 默认为 Double.class
	 * @return idx 所对应的值
	 */
	default Double[] dbls(final String key) {
		return this.aa(key, Double.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1d,2d,3d)).aa("numbers",Double.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param idx 字段索引从0开始 targetClass 类型参照类 默认为 Double.class
	 * @return idx 所对应的值
	 */
	default Double[] dbls(final int idx) {
		return this.aa(idx, Double.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1l,2l,3l)).aa("numbers",Long.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param key 字段名称 targetClass 类型参照类 默认为 Long.class
	 * @return idx 所对应的值
	 */
	default Long[] lngs(final String key) {
		return this.aa(key, Long.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1l,2l,3l)).aa("numbers",Long.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param idx 字段索引从0开始 targetClass 类型参照类 默认为 Long.class
	 * @return idx 所对应的值
	 */
	default Long[] lngs(final int idx) {
		return this.aa(idx, Long.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1,2,3)).aa("numbers",Integer.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param key 字段名称 targetClass 类型参照类 默认为 Integer.class
	 * @return idx 所对应的值
	 */
	default Integer[] ints(final String key) {
		return this.aa(key, Integer.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A(1,2,3)).aa("numbers",Integer.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param idx 字段索引从0开始 targetClass 类型参照类 默认为 Integer.class
	 * @return idx 所对应的值
	 */
	default Integer[] ints(final int idx) {
		return this.aa(idx, Integer.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A("1","2","3")).aa("numbers",String.class); 可以获取得到
	 * 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param key 字段名称 targetClass 类型参照类 默认为 String.class
	 * @return idx 所对应的值
	 */
	default String[] strs(final String key) {
		return this.aa(key, String.class);
	}

	/**
	 * 比如 对于 一个 REC("numbers",A("1","2","3")).aa(0,String.class); 可以获取得到 数组类型。<br>
	 * 把key字段值强制装换成 T 类型。类型强转失败时返回null<br>
	 * 
	 * @param idx 字段索引从0开始 targetClass 类型参照类 默认为 String.class
	 * @return idx 所对应的值
	 */
	default String[] strs(final int idx) {
		return this.aa(idx, String.class);
	}

	/**
	 * 所谓叶子节点是值：元素类型不是IRecord 或是Map之类的节点。 注意：contact,address 不是叶子节点 比如；<br>
	 * final var rec = REC( <br>
	 * "name","zhangsan", <br>
	 * "sex","man", <br>
	 * "contact",REC("mobile","13120751773","phone","0411833802234","email","gbench@sina.com"),
	 * <br>
	 * "address",REC("provice","liaoning","city","dalian","district","pulandian"));<br>
	 * System.out.println(rec.leafs());<br>
	 * 提取所有叶子节点:注意当叶子节点是多元素类型：如List,Set 之类的Collection的时候，只返回Collection中的第一个元素
	 * 
	 * @return 叶子节点集合：
	 */
	default List<IRecord> leafs() {
		// 单节点遍历
		return this.dfs_eval_forone(IRecord::REC);
	}

	/**
	 * 提取第一个叶子节点 <br>
	 * 所谓叶子节点是值：元素类型不是IRecord 或是Map之类的节点。 注意：contact,address 不是叶子节点 比如：<br>
	 * final var rec = REC( <br>
	 * "name","zhangsan", <br>
	 * "sex","man", <br>
	 * "contact",REC("mobile","13120751773","phone","0411833802234","email","gbench@sina.com"),
	 * <br>
	 * "address",REC("provice","liaoning","city","dalian","district","pulandian"));
	 * <br>
	 * System.out.println(rec.leaf()); // "/name:zhangsan" <br>
	 * System.out.println(rec.leaf(3)); // "/contact/phone:0411833802234" <br>
	 * <br>
	 * 
	 * @param n 叶子节点的序号 从0开始,默认为0，如果不输入的化，只返回一个叶子，节点输入多个序号只提取第一个元素
	 * @return 第一个叶子姐弟啊
	 */
	default IRecord leaf(final Number... n) {

		final var final_n = (n == null || n.length < 1) ? 0 : n[0].intValue();
		// 单节点遍历
		return this.dfs_eval_forone(IRecord::REC).get(final_n);
	}

	/**
	 * 按照索引 idx 设置字段的值
	 * 
	 * @param idx   键值索引序号 从0开始
	 * @param value 字段值
	 * @return 当前的IRecord对象 以保证可以链式编程。
	 */
	default IRecord set(final int idx, final Object value) {
		final var key = this.keyOfIndex(idx);
		return this.set(key, value);
	}

	/**
	 * 按照索引 idx 设置字段的值
	 * 
	 * @param <T>    mapper 的源数据类型
	 * @param <U>    mapper 的目标数据类型
	 * @param idx    键值索引序号 从0开始
	 * @param mapper t->u 键值变换函数
	 * 
	 * @return 当前的IRecord对象 以保证可以链式编程。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> IRecord set(final int idx, final Function<T, U> mapper) {
		final var value = mapper.apply((T) this.get(idx));
		return this.set(idx, value);
	}

	/**
	 * 按照键名 key 设置字段的值
	 * 
	 * @param <T>    mapper 的源数据类型
	 * @param <U>    mapper 的目标数据类型
	 * @param key    键名
	 * @param mapper t->u 键值变换函数
	 * @return 当前的IRecord对象 以保证可以链式编程。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> IRecord set(final String key, final Function<T, U> mapper) {
		if (mapper == null) { // mapper 不存在，返回对象本身
			return this;
		} else { // 调用mapper 进行数据砖混
			final var value = mapper.apply((T) this.get(key));
			return this.set(key, value);
		} // if
	}

	/**
	 * 按照键名 更新键值
	 * 
	 * @param <T>    mapper 的源数据类型
	 * @param <U>    mapper 的目标数据类型
	 * @param mapper (k:键名,t:元素对象值)->u 键值变换函数
	 * @return 当前的IRecord对象 以保证可以链式编程。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> IRecord update(final BiFunction<String, T, U> mapper) {
		for (final var key : this.keys()) { // 键值便利
			final var t = (T) this.get(key);
			final var value = mapper.apply(key, t); // 提取键值
			this.set(key, value);
		} // for
		return this;
	}

	/**
	 * 按照索引号 更新键值
	 * 
	 * @param <T>    mapper 的源数据类型
	 * @param <U>    mapper 的目标数据类型
	 * @param mapper (i:键名索引序号从0开始,t:元素对象值)->u 键值变换函数
	 * @return 当前的IRecord对象 以保证可以链式编程。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> IRecord update2(final BiFunction<Integer, T, U> mapper) {
		final var n = this.size(); // 键值对儿的个数
		for (var i = 0; i < n; i++) { // 键值便利
			final var t = (T) this.get(i);
			final var value = mapper.apply(i, t); // 提取键值
			this.set(i, value);
		} // for
		return this;
	}

	/**
	 * 把key 所代表的值(value) 变换成 U类型的数据
	 * 
	 * @param <T> index 所代表的值的类型
	 * @param <U> 返回结果的类型
	 * 
	 * @param key 字段名
	 * @param t2u 字段值的变换函数
	 * @return U类型数据
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U get(final String key, final Function<T, U> t2u) {
		return map(key, e -> {
			T t1 = null;
			try {
				t1 = (T) e;
			} catch (Exception ignored) {
			}
			return t2u.apply(t1);
		});
	}

	/**
	 * 把index 所代表的值(value) 变换成 U类型的数据
	 * 
	 * @param <T> index 所代表的值的类型
	 * @param <U> 返回结果的类型
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @param t2u 字段值的变换函数
	 * @return U 类型数据
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U get(final int idx, final Function<T, U> t2u) {
		return map(idx, e -> {
			T t1 = null;
			try {
				t1 = (T) e;
			} catch (Exception ignored) {
			}
			return t2u.apply(t1);
		});
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<Object> opt(final String key) {
		return this.get(key, Optional::ofNullable);
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<Object> opt(final int idx) {
		return this.get(idx, Optional::ofNullable);
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<String> stropt(final String key) {
		return Optional.ofNullable(this.str(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<String> stropt(final int idx) {
		return this.stropt(this.keyOfIndex(idx));
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<Integer> i4opt(final String key) {
		return Optional.ofNullable(this.i4(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<Integer> i4opt(final int idx) {
		return this.i4opt(this.keyOfIndex(idx));
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<Long> lngopt(final String key) {
		return Optional.ofNullable(this.lng(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<Long> lngopt(final int idx) {
		return this.lngopt(this.keyOfIndex(idx));
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<LocalDateTime> ldtopt(final String key) {
		return Optional.ofNullable(this.ldt(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<LocalDateTime> ldtopt(final int idx) {
		return this.ldtopt(this.keyOfIndex(idx));
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<LocalDate> ldopt(final String key) {
		return Optional.ofNullable(this.ld(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<LocalDate> ldopt(final int idx) {
		return this.ldopt(this.keyOfIndex(idx));
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<LocalTime> ltopt(final String key) {
		return Optional.ofNullable(this.lt(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<LocalTime> ltopt(final int idx) {
		return this.ltopt(this.keyOfIndex(idx));
	}

	/**
	 * 提取指定键名的可选值
	 * 
	 * @param key 字段键名
	 * @return Optional
	 */
	default Optional<Double> dblopt(final String key) {
		return Optional.ofNullable(this.dbl(key));
	}

	/**
	 * 提取指定键名索引的可选值
	 * 
	 * @param idx 字段编号索引,从0开始
	 * @return Optional
	 */
	default Optional<Double> dblopt(final int idx) {
		return this.dblopt(this.keyOfIndex(idx));
	}

	/**
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param <T>  节点的数据类型
	 * @param <U>  转换结果的数据类型
	 * @param keys 键名序列：键名额层级结构
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Optional<U> pgetopt(final List<String> keys, final Function<T, U> t2u) {
		final var kk = keys.stream().filter(e -> !e.matches("[\\s/\\\\]*")).collect(Collectors.toList());//
		final var size = kk.size();
		if (size < 1) {
			return Optional.ofNullable(t2u.apply((T) this));//
		}
		final var obj = this.get(kk.get(0));
		if (kk.size() == 1) {
			return Optional.ofNullable(t2u.apply((T) obj));
		}

		IRecord node = null;// 中间节点数据
		try {
			if (obj instanceof IRecord) {// IRecord 直接转换
				node = (IRecord) obj;
			} else if (obj instanceof Map) {// 对Map类型 需要通过IRecord 给予简介转换。
				final var mm = (Map<String, Object>) obj;
				node = REC(mm);
			} else {
				return Optional.empty();
			}
		} catch (Exception e) {// 类型转换出现了异常
			e.printStackTrace();
			return Optional.empty();
		}

		// 步进一级继续按路径检索数据。
		return node.pgetopt(kk.subList(1, size), t2u);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	default <T, U> U pathget(final String path, final Function<T, U> t2u) {
		return pathget(Arrays.asList(path.split("[/]+")), t2u);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path        键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param targetClass 类型参照类的对象
	 * @return U类型数据值。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U pathget(final String path, final Class<U> targetClass) {
		return pathget(path, e -> (U) e);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2","/",identity) 返回 value 数值 <br>
	 * 
	 * @param path 键名序列
	 * @param sep  键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	default <T, U> U pathget(final String path, String sep, final Function<T, U> t2u) {
		return pathget(Arrays.asList(path.split(sep)), t2u);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2") 返回 value 数值 <br>
	 * 
	 * @param path 键名序列, 分隔符：sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return U类型数据值。
	 */
	default Object pathget(final String path) {
		return pathget(Arrays.asList(path.split("[/]+")), identity);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法<br>
	 * ,即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2","/") 返回 value 数值 <br>
	 * 
	 * @param path 键名序列
	 * @param sep  键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return U类型数据值。
	 */
	default Object pathget(final String path, String sep) {
		return pathget(Arrays.asList(path.split(sep)), identity);
	}

	/**
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2]) 返回 value 数值
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param keys 键名序列
	 */
	default Object pathget(List<String> keys) {
		return this.pathget(keys, identity);
	}

	/**
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param <T>  节点的数据类型
	 * @param <U>  转换结果的数据类型
	 * @param keys 键名序列：键名额层级结构
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	default <T, U> U pathget(final List<String> keys, final Function<T, U> t2u) {
		return this.pgetopt(keys, t2u).orElse(null);
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param <T>  节点的数据类型
	 * @param <U>  转换结果的数据类型
	 * @param keys 键名序列：键名额层级结构
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	default <T, U> Optional<U> path2opt(final List<String> keys, final Function<T, U> t2u) {
		return this.pgetopt(keys, t2u);
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param <T>  节点的数据类型
	 * @param <U>  转换结果的数据类型
	 * @param keys 键名序列：键名额层级结构
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	default <T, U> Optional<U> path2opt(final String[] keys, final Function<T, U> t2u) {
		return this.pgetopt(Arrays.asList(keys), t2u);
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param <T>  节点的数据类型
	 * @param <U>  转换结果的数据类型
	 * @param path 键名序列：键名额层级结构 , 键名分隔符的 regex "[/]+"
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U类型数据值。
	 */
	default <T, U> Optional<U> path2opt(final String path, final Function<T, U> t2u) {
		final var regex = "[/]+"; //
		return this.pgetopt(Arrays.asList(path.split(regex)), t2u);
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param <U>  转换结果的数据类型
	 * @param path 键名序列：键名额层级结构 , 键名分隔符的 regex "[/]+"
	 * @return U类型数据值。
	 */
	@SuppressWarnings("unchecked")
	default <U> Optional<U> path2opt(final String path) {
		return this.path2opt(path, e -> (U) e);
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param path 键名序列：键名额层级结构 , 键名分隔符的 regex "[/]+"
	 * @return U类型数据值。
	 */
	default Optional<String> path2optstr(final String path) {
		return this.path2opt(path, o -> o + "");
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param path 键名序列：键名额层级结构 , 键名分隔符的 regex "[/]+"
	 * @return U类型数据值。
	 */
	default Optional<Number> path2optnum(final String path) {
		return this.path2opt(path, IRecord.obj2num);
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param path 键名序列：键名额层级结构 , 键名分隔符的 regex "[/]+"
	 * @return U类型数据值。
	 */
	default Optional<Integer> path2optint(final String path) {
		return this.path2opt(path, IRecord.obj2num).map(e -> e.intValue());
	}

	/**
	 * pathgetOptional 的别名 <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget([k0,k1,k2],identity) 返回 value 数值 <br>
	 * 
	 * 根据路径获取Record 数据值。<br>
	 * 依据keys:k0/k1/k2/... 按层次访问元素数据。<br>
	 * 
	 * @param path 键名序列：键名额层级结构 , 键名分隔符的 regex "[/]+"
	 * @return U类型数据值。
	 */
	default Optional<Double> path2optdbl(final String path) {
		return this.path2opt(path, IRecord.obj2num).map(e -> e.doubleValue());
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param b    对于单值类型的数据v是否给予封装成 REC(0,v),<br>
	 *             例如: 当 v 为3的时候 <br>
	 *             如果b为true,3会被封装成 REC(0,3) 给予返回,<br>
	 *             否则b为false，则返回 Optional.empty
	 * @return IRecord 类型的结果
	 */
	default Optional<IRecord> path2optrec(final String path, final boolean b) {
		return path2opt(path, o -> {
			if (o == null) {
				return null;
			} else if (o instanceof IRecord) {
				return (IRecord) o;
			} else if (o instanceof Map) {
				return REC((Map<?, ?>) o);
			} else {
				var rec = b ? REC(0, o) : null;// 单值
				final var jsn = o instanceof String ? o.toString() : Json.obj2json(o);
				final var mode = Json.getJsonMode(jsn);
				if (mode.equals(Json.JsonMode.MAP)) {
					rec = REC("0", jsn).rec("0");// 类型转换
				} // if
				return rec;
			} // if
		});// if
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param t2u  对 record 结果进行转换的函数
	 * @return U 类型数据值。
	 */
	default <T, U> U path2target(final String path, final Function<T, U> t2u) {
		return this.pathget(path, t2u);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 * 
	 * @param <U>         类型参照类的类型
	 * @param path        键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param targetClass 类型参照类
	 * @return U 类型的数据对象。
	 */
	@SuppressWarnings("unchecked")
	default <U> U path2target(final String path, Class<U> targetClass) {
		return this.pathget(path, e -> ((U) e));
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * <br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return 整型数组
	 */
	default Integer[] path2ints(final String path) {
		return pathget(path, Integer[].class);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return 长整型数组
	 */
	default Long[] path2lngs(final String path) {
		return pathget(path, Long[].class);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return 浮点数类型的数组。
	 */
	default Double[] path2dbls(final String path) {
		return pathget(path, Double[].class);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return 字符串类型的数组。
	 */
	default String[] path2strs(final String path) {
		return pathget(path, String[].class);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Object 类型的结果
	 */
	default Object path2obj(final String path) {
		return pathget(path, Object.class);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Integer 类型的结果,非法值 返回 null
	 */
	default String path2str(final String path) {
		return this.path2optstr(path).orElse(null);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Integer 类型的结果,非法值 返回 null
	 */
	default Integer path2int(final String path) {
		return this.path2optint(path).orElse(null);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Double 类型的结果,非法值 返回 null
	 */
	default Double path2dbl(final String path) {
		return this.path2optdbl(path).orElse(null);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Object 类型的列表
	 */
	default List<Object> path2lls(final String path) {
		return this.path2llS(path).toList();
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <T>    源数据元素类型
	 * @param <U>    目标数据元素类型
	 * @param path   键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。<br>
	 *               当path不存在的时候,返回一个 长度为0的流。
	 * @param mapper 元素变换函数 t-&gt;u
	 * @return U类型的列表
	 */
	default <T, U> List<U> path2lls(final String path, final Function<T, U> mapper) {
		return this.path2llS(path, mapper).toList();
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Object 类型的流
	 */
	@SuppressWarnings("unchecked")
	default Stream<Object> path2llS(final String path) {
		return (Stream<Object>) pathget(path, o -> {
			if (o instanceof final Collection<?> coll) {
				return coll.stream();
			} else if (o instanceof final Stream<?> stream) {
				return stream;
			} else if (Objects.nonNull(o) && o.getClass().isArray()) {
				return Arrays.stream((Object[]) o);
			} else if (o instanceof final Iterable<?> itr) {
				return StreamSupport.stream(itr.spliterator(), false);
			} else if (o instanceof Object) {
				return Stream.of(o);
			} else {
				return null;
			}
		});
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 <br>
	 * [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <T>    源数据元素类型
	 * @param <U>    目标数据元素类型
	 * @param path   键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。<br>
	 *               当path不存在的时候,返回一个 长度为0的流。
	 * @param mapper 元素变换函数 t-&gt;u
	 * @return U类型 的流
	 */
	default <T, U> Stream<U> path2llS(final String path, final Function<T, U> mapper) {
		@SuppressWarnings("unchecked")
		final Function<Object, U> final_mapper = mapper == null ? e -> (U) e : (Function<Object, U>) mapper;
		final var vv = pathget(path);// 提取路径值
		return REC("0", vv == null ? new ArrayList<>() : vv).lla("0", Object.class).stream().map(final_mapper); // 构造一个IRecord然后用IRecord的lla计算流
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <U>          目标数据元素类型
	 * @param path         键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param defaultValue 元素的默认值
	 * @return U 类型 的流
	 */
	@SuppressWarnings("unchecked")
	default <U> Stream<U> path2llS(final String path, final U defaultValue) {
		final var uclass = defaultValue == null ? Object.class : defaultValue.getClass();
		return this.path2llS(path, (Object e) -> {
			if (e != null) { // 元素非空
				if (uclass.isAssignableFrom(e.getClass())) { // 元素为U类型
					var u = defaultValue;
					try {
						u = (U) e;
					} catch (Exception ex) {
						ex.printStackTrace();
					} // try
					return u;
				} else {
					return defaultValue;
				} // if
			} else { // 元素为空
				return defaultValue;
			} // if
		});// path2llS
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return IRecord 类型的结果
	 */
	default IRecord path2rec(final String path) {
		return this.path2rec(path, false);
	}

	/**
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param b    对于单值类型的数据v是否给予封装成 REC(0,v),<br>
	 *             例如: 当 v 为3的时候 <br>
	 *             如果b为true,3会被封装成 REC(0,3) 给予返回,<br>
	 *             否则b为false，则返回 null
	 * @return IRecord 类型的结果, 非法值返回null
	 */
	default IRecord path2rec(final String path, final boolean b) {
		return this.path2optrec(path, b).orElse(null);
	}

	/**
	 * pathclc 的别名 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <T>       归集器元素类型
	 * @param <U>       归集器结果类型
	 * @param path      键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param collector 归集器 [t]->u
	 * @return U 类型的结果
	 */
	default <T, U> U pathclc(final String path, final Collector<T, ?, U> collector) {
		return this.pathclc(path, (T) null, collector);
	}

	/**
	 * pathgetS 的归集器版本 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <T>          归集器元素类型
	 * @param <U>          归集器结果类型
	 * @param path         键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param defaultValue 默认值
	 * @param collector    归集器 [t]->u
	 * @return U 类型的结果
	 */
	default <T, U> U pathclc(final String path, final T defaultValue, final Collector<T, ?, U> collector) {
		return this.pathgetS(path, defaultValue).collect(collector);
	}

	/**
	 * pathgetS 的别名 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Object 类型的流
	 */
	default Stream<Object> pathgetS(final String path) {
		return this.pathgetS(path, null);
	}

	/**
	 * path2llS 的别名 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <U>          目标数据元素类型
	 * @param path         键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param defaultValue 元素的默认值
	 * @return U 类型的流
	 */
	default <U> Stream<U> pathgetS(final String path, final U defaultValue) {
		return this.path2llS(path, defaultValue);
	}

	/**
	 * path2llS 的别名 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <T>    源数据元素类型
	 * @param <U>    目标数据元素类型
	 * @param path   键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param mapper 元素变换函数 t-&gt;u
	 * @return U类型 的流
	 */
	default <T, U> Stream<U> pathgetS(final String path, final Function<T, U> mapper) {
		return this.path2llS(path, mapper);
	}

	/**
	 * pathgets 的别名 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Object 类型的列表
	 */
	default List<Object> pathgets(final String path) {
		return this.pathgets(path, null);
	}

	/**
	 * path2llS 的列表形式衍生函数 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <U>          目标数据元素类型
	 * @param path         键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param defaultValue 元素的默认值
	 * @return U 类型的 列表
	 */
	default <U> List<U> pathgets(final String path, final U defaultValue) {
		return this.path2llS(path, defaultValue).collect(Collectors.toList());
	}

	/**
	 * pathgeta 的别名 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param path 键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @return Object 类型的 数组
	 */
	default Object[] pathgeta(final String path) {
		return this.pathgeta(path, null);
	}

	/**
	 * path2llS 的数组形式衍生函数 <br>
	 * 根据路径获取Record 数据值。<br>
	 * 这是对 递归结构(层级式)的 IRecord 按照 路径键名序列path 进行访问的算法,<br>
	 * 即 IRecord的字段元素仍然是 IRecord的形式 <br>
	 * 类似于如下的形式 [k0:[ <br>
	 * &nbsp; &nbsp; k1:[ <br>
	 * &nbsp;&nbsp; &nbsp;&nbsp; k2:value]]] <br>
	 * pathget("k0/k1/k2",identity) 返回 value 数值 <br>
	 *
	 * @param <U>          目标数据元素类型
	 * @param path         键名序列,分隔符sep 默认为："[/]+" 键名序列 的分割符号，这样就可以从path中构造出层级关系。
	 * @param defaultValue 元素的默认值
	 * @return U 类型的 数组
	 */
	default <U> U[] pathgeta(final String path, final U defaultValue) {
		return this.path2llS(path, defaultValue).collect(IRecord.aaclc(e -> e));
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 * 如果value是一个List类型,Collection 这直接使用 序列转化，对于单元素的类型，这使用Arrays.asList 对其进行包装<br>
	 * 给予扩展乘一个只有一个元素的List<br>
	 * 不存在的key,不予处理返回null<br>
	 * 
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param key 列表类型的字段名
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构,List 的复制品，注意不是元列表引用
	 */
	@SuppressWarnings("unchecked")
	default <T, U> List<U> llapply(final String key, final Function<T, U> t2u) {
		List<U> uu = null;
		try {
			List<T> tt = null; // T 类型的元素列表
			final Object o = get(key); // 提取key的值
			if (o == null) {
				return null;// 不存在的value值不予处理返回null
			}

			if (o instanceof List) {// List 类型
				tt = new ArrayList<>((List<T>) o); // 复制一个李彪
			} else if (o instanceof ArrayNode) {// Jackson的节点元素类型
				tt = new LinkedList<>(); // 复制列表
				final ArrayNode anode = (ArrayNode) o;
				final var itr = anode.iterator();
				final var splitr = Spliterators.spliteratorUnknownSize(itr, Spliterator.ORDERED);
				try {
					StreamSupport.stream(splitr, false).map(e -> (T) e).forEach(tt::add);
				} catch (Exception e) {
					e.printStackTrace();
				} // 忽略异常
			} else if (o instanceof Collection) {// Collection 类型
				tt = new LinkedList<>(); // 复制列表
				for (var t : ((Collection<?>) o))
					tt.add((T) t);
			} else if (o.getClass().isArray()) {// 数组类型
				final var componetClazz = o.getClass().getComponentType();
				if (componetClazz.isPrimitive()) { // 基础类型的处理
					if (componetClazz == byte.class) {
						final var aa = (byte[]) o;
						final var n = aa.length;
						final var bb = new Byte[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == char.class) {
						final var aa = (char[]) o;
						final var n = aa.length;
						final var bb = new Character[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == boolean.class) {
						final var aa = (boolean[]) o;
						final var n = aa.length;
						final var bb = new Boolean[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == short.class) {
						final var aa = (short[]) o;
						final var n = aa.length;
						final var bb = new Short[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == int.class) {
						final var aa = (int[]) o;
						final var n = aa.length;
						final var bb = new Integer[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == long.class) {
						final var aa = (long[]) o;
						final var n = aa.length;
						final var bb = new Long[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == float.class) {
						final var aa = (float[]) o;
						final var n = aa.length;
						final var bb = new Float[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else if (componetClazz == double.class) {
						final var aa = (double[]) o;
						final var n = aa.length;
						final var bb = new Double[n];
						for (int i = 0; i < n; i++) {
							bb[i] = aa[i];
						}
						tt = (List<T>) Arrays.asList(bb);
					} else { // 非法的基础类型
						System.err.println("未知的基础类型:" + componetClazz);
					}
				} else { // 非基础类型
					tt = (List<T>) Arrays.asList((Object[]) o); // 复制列表
				} // if
			} else if (o instanceof IRecord) {// IRecord 类型
				tt = (List<T>) ((IRecord) o).values(); // 复制列表
			} else if (o instanceof Map) {// IRecord 类型
				tt = (List<T>) new ArrayList<>(((Map<Object, Object>) o).values()); // 复制列表
			} else if (o instanceof Iterable) {// Iterable 类型
				tt = new LinkedList<>(); // 复制列表
				for (var t : ((Iterable<?>) o)) {
					tt.add((T) t);
				}
			} else {// 其他 给予包装成一个List类型
				tt = (List<T>) Collections.singletonList(o); // 复制列表
			} // 键值得类型判断与列表化构造

			if (tt != null) {// 尝试进行类型转换
				uu = tt.stream().map(t2u).collect(Collectors.toList()); // 复制列表
			} // if
		} catch (Exception e) {
			e.printStackTrace();
		}

		return uu;
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <U>    字段key所对应的列表数据的元素的类型
	 * @param key    列表类型的字段名
	 * @param uclass 列表元素类型, 当 uclass 为 IRecord.class 且 值为 Sttring 的时候 会采用
	 *               Json.json2list(value, IRecord.class); 进行处理
	 * @return 以U类型为元素类型的列表结构
	 */
	@SuppressWarnings("unchecked")
	default <U> List<U> lla(final String key, final Class<U> uclass) {
		final var value = this.get(key);
		final var _uclass = uclass == null ? (Class<U>) Object.class : uclass;

		if (_uclass == IRecord.class) { // IRecord 类型的特殊处理
			if (value != null && value instanceof String) { // 字符串的 特殊处理
				return (List<U>) Json.json2list(value, IRecord.class);
			} else { // 非字符串 类型处理
				return llapply(key, o -> { // 键名值处理
					if (o instanceof Map)
						return (U) REC((Map<?, ?>) o);
					else if (o instanceof IRecord)
						return (U) (IRecord) o;
					else if (o instanceof String)
						return (U) Json.json2rec(o);
					else
						return null;
				}); // llapply
			} // if
		} else { // 其他类型
			return llapply(key, u -> (U) u);
		} // if
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <U>    字段key所对应的列表数据的元素的类型
	 * @param idx    列表类型的字段的索引号,从0开始
	 * @param uclass 列表元素类型
	 * @return 以U数元素类型的列表结构
	 */
	@SuppressWarnings("unchecked")
	default <U> List<U> lla(final int idx, final Class<U> uclass) {
		return lls(idx, u -> (U) u);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param key 列表类型的字段名
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T, U> List<U> lla(final String key, final Function<T, U> t2u) {
		return llapply(key, t2u);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param idx 列表类型的字段的索引号从0开始
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T, U> List<U> lla(final int idx, final Function<T, U> t2u) {
		return lls(idx, t2u);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <U> 字段key所对应的列表数据的元素的类型
	 * @param key 列表类型的字段的名称
	 * @return 以U类型为元素类型的列表结构
	 */
	@SuppressWarnings("unchecked")
	default <U> List<U> lls(final String key) {
		return llapply(key, e -> (U) e);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param idx 列表类型的字段的索引号从0开始
	 * @return 以U类型为元素类型的列表结构
	 */
	default <U> List<U> lls(final int idx) {
		return lls(keyOfIndex(idx));
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param key 列表类型的字段的名称
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T, U> List<U> lls(final String key, final Function<T, U> t2u) {
		return llapply(key, t2u);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param idx 列表类型的字段的索引号从0开始
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T, U> List<U> lls(final int idx, final Function<T, U> t2u) {
		return llapply(keyOfIndex(idx), t2u);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param key 列表类型的字段的名称
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T, U> Stream<U> llS(final String key, final Function<T, U> t2u) {
		return this.lls(key, t2u).stream();
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param idx 列表类型的字段的索引号从0开始
	 * @param t2u 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T, U> Stream<U> llS(final int idx, final Function<T, U> t2u) {
		return this.llS(this.keyOfIndex(idx), t2u);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <U> 字段key所对应的列表数据的元素的类型
	 * @param key 列表类型的字段的名称
	 * @return 以U类型为元素类型的流结构
	 */
	@SuppressWarnings("unchecked")
	default <U> Stream<U> llS(final String key) {
		return llapply(key, e -> (U) e).stream();
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <U> 字段key所对应的列表数据的元素的类型T进行变换的结果
	 * @param idx 列表类型的字段的索引号从0开始
	 * @return 以U类型为元素类型的流结构
	 */
	default <U> Stream<U> llS(final int idx) {
		return llS(keyOfIndex(idx));
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T>    字段key所对应的列表数据的元素的类型
	 * @param key    列表类型的字段的名称
	 * @param mapper 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T> DFrame dfm(final String key, final Function<T, IRecord> mapper) {
		return this.path2llS(key, mapper).collect(DFrame.dfmclc);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T>    字段key所对应的列表数据的元素的类型
	 * @param idx    列表类型的字段的索引号从0开始
	 * @param mapper 对key字段的元素数据进行变换的结构
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T> DFrame dfm(final int idx, final Function<T, IRecord> mapper) {
		return this.dfm(this.keyOfIndex(idx), mapper);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param key 列表类型的字段的名称
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T> DFrame dfm(final String key) {
		return this.path2llS(key, IRecord::REC).collect(DFrame.dfmclc);
	}

	/**
	 * 这里key 是一个集合对象List，用t2u的对集合List中的元素进行处理：<br>
	 * 把一个列表改变成另一个列表，也就是在不改变聚合方式的情况下改变元素内容，这就是所谓的换药不换瓶 <br>
	 *
	 * @param <T> 字段key所对应的列表数据的元素的类型
	 * @param idx 列表类型的字段的索引号从0开始
	 * @return 以U类型为元素类型的列表结构
	 */
	default <T> DFrame dfm(final int idx) {
		return this.dfm(this.keyOfIndex(idx));
	}

	/**
	 * 是否含有指定索引的字段名．
	 * 
	 * @param idx 索引
	 * @return 是否含有idx标号的索引
	 */
	default boolean has(final int idx) {
		return this.get(keyOfIndex(idx)) == null;
	}

	/**
	 * 字段的索引下标转字段名称
	 * 
	 * @param idx 索引序号从0开始
	 * @return 索引对应的键名，非法索引(不存在的列号) 返回 null
	 */
	default String keyOfIndex(final Integer idx) {

		if (idx >= this.keys().size() || idx < 0) {
			return null;
		}
		final String key = this.keys().get(idx);

		return key;
	}

	/**
	 * 获取 键名字段 的 索引号
	 * 
	 * @param key 键名
	 * @return 键名索引, 从 0 开始 , 非法的 键名字段 （比如不存在的 键名字段) 返回 null
	 */
	default Integer indexOfKey(final String key) {

		final var kk = this.keys().toArray(String[]::new);
		for (int i = 0; i < kk.length; i++) {
			if (kk[i].equals(key)) {
				return i;
			}
		}

		return null;
	}

	/**
	 * 当且仅当key值不存在的才给予:compute,否则直接返回键值，属于名称来源于 Java Map <br>
	 * compute 就是获取并变换设置。相当于 get&amp;set: <br>
	 * final var value = this.get(idx);<br>
	 * if(value!=null)return (U)value; <br>
	 * final var u = mapper.apply(idx); this.set(key, u); <br>
	 * 
	 * @param <U>    变换的t2u的目标结果类型
	 * @param idx    键名索引 从0开始
	 * @param mapper key->U 类型的函数
	 * @return 值变换后的数据。 U类型
	 */
	@SuppressWarnings("unchecked")
	default <U> U computeIfAbsent(final Integer idx, final Function<Integer, U> mapper) {
		final var value = this.get(idx);
		if (value != null)
			return (U) value;
		final var u = mapper.apply(idx);
		this.set(idx, u);
		return u;
	}

	/**
	 * 当且仅当key值不存在的才给予:compute,否则直接返回键值，属于名称来源于 Java Map compute 就是获取并变换设置。相当于
	 * get&amp;set: <br>
	 * final var value = this.get(idx);<br>
	 * if(value!=null)return (U)value; <br>
	 * final var u = mapper.apply(key); <br>
	 * set(key,u); <br>
	 * 
	 * @param <U>    变换的t2u的目标结果类型
	 * @param key    字段键名
	 * @param mapper key->U 类型的函数
	 * @return 值变换后的数据。 U类型
	 */
	@SuppressWarnings("unchecked")
	default <U> U computeIfAbsent(final String key, final Function<String, U> mapper) {
		final var value = this.get(key);
		if (value != null)
			return (U) value;
		final var u = mapper.apply(key);
		this.set(key, u);
		return u;
	}

	/**
	 * 当且仅当key值不存在的才给予:compute,否则直接返回键值，属于名称来源于 Java Map <br>
	 * compute 就是获取并变换设置。相当于 get&amp;set: <br>
	 * U u = mapper.apply(idx,get(key)) <br>
	 * set(key,u); <br>
	 * 
	 * @param <T>    变换的t2u源数据类型
	 * @param <U>    变换的t2u的目标结果类型
	 * @param idx    键名索引 从0开始
	 * @param mapper 二元函数(key,value)-&gt;u
	 * @return 值变换后的数据。 U类型
	 */
	default <T, U> U computeIfAbsent(final Integer idx, final BiFunction<Integer, T, U> mapper) {
		@SuppressWarnings("unchecked")
		final var o = (U) this.get(this.keyOfIndex(idx));
		if (o != null)
			return o;
		@SuppressWarnings("unchecked")
		final U u = mapper.apply(idx, (T) o);
		this.set(idx, u);
		return u;
	}

	/**
	 * 当且仅当key值不存在的才给予:compute,否则直接返回键值，属于名称来源于 Java Map <br>
	 * compute 就是获取并变换设置。相当于 get&amp;set: <br>
	 * U u = mapper.apply(key,get(key,t2u)) <br>
	 * set(key,u); <br>
	 * 
	 * @param <T>    变换的t2u源数据类型
	 * @param <U>    变换的t2u的目标结果类型
	 * @param key    键名
	 * @param mapper 键值变换函数(key,value)->u
	 * @return 值变换后的数据。 U类型
	 */
	default <T, U> U computeIfAbsent(final String key, final BiFunction<String, T, U> mapper) {
		@SuppressWarnings("unchecked")
		final var o = (U) this.get(key);
		if (o != null)
			return o;
		@SuppressWarnings("unchecked")
		final U u = mapper.apply(key, (T) (Object) o);
		this.set(key, u);
		return u;
	}

	/**
	 * 获取并变换设置。相当于 相当于 get&amp;set: <br>
	 * U u = get(key,t2u) <br>
	 * set(key,u); <br>
	 * 
	 * @param <T> 变换的t2u源数据类型
	 * @param <U> 变换的t2u的目标结果类型
	 * @param idx 键名索引 从0开始
	 * @param t2u 值变换函数 value0->value1
	 * @return 值变换后的数据。
	 */
	default <T, U> U compute(final Integer idx, final Function<T, U> t2u) {
		final U u = this.get(idx, t2u);
		final var key = this.keyOfIndex(idx);
		this.set(key, u);
		return u;
	}

	/**
	 * 获取并变换设置。相当于 get&amp;set: <br>
	 * U u = get(key,t2u) <br>
	 * set(key,u); <br>
	 * 
	 * @param <T> 变换的t2u源数据类型
	 * @param <U> 变换的t2u的目标结果类型
	 * @param key 键名
	 * @param t2u 键值变换函数 value0->value1
	 * @return 值变换后的数据。 U类型
	 */
	default <T, U> U compute(final String key, final Function<T, U> t2u) {
		final U u = this.get(key, t2u);
		this.set(key, u);
		return u;
	}

	/**
	 * 获取并变换设置。相当于 相当于 get&amp;set: <br>
	 * U u = mapper.apply(key, (T)get(key)); <br>
	 * set(key,u); <br>
	 * 
	 * @param <T>    变换的t2u源数据类型
	 * @param <U>    变换的t2u的目标结果类型
	 * @param idx    键名索引 从0开始
	 * @param mapper 值变换函数 value0->value1
	 * @return 值变换后的数据。
	 */
	default <T, U> U compute(final Integer idx, final BiFunction<Integer, T, U> mapper) {
		@SuppressWarnings("unchecked")
		final T t = (T) this.get(this.keyOfIndex(idx));
		final U u = mapper.apply(idx, t);
		this.set(idx, u);
		return u;
	}

	/**
	 * 当且晋档 key对应的值存在的时候才给予更新<br>
	 * 相当于 调用 t2u 修改key所对应的值
	 * 
	 * @param <T> 变换的t2u源数据类型
	 * @param <U> 变换的t2u的目标结果类型
	 * @param key 键名
	 * @param t2u 键值变换函数 value0->value1
	 * @return 值变换后的数据。 U类型
	 */
	default <T, U> U computeIfPresent(final String key, final Function<T, U> t2u) {
		if (!this.has(key))
			return null;
		final U u = this.get(key, t2u);
		this.set(key, u);
		return u;
	}

	/**
	 * 当且晋档 key对应的值存在的时候才给予更新<br>
	 * 相当于 调用 t2u 修改索引idx所对应的值
	 * 
	 * @param <T> 变换的t2u源数据类型
	 * @param <U> 变换的t2u的目标结果类型
	 * @param idx 键名索引 从0开始
	 * @param t2u 值变换函数 t->u
	 * @return 值变换后的数据。
	 */
	default <T, U> U computeIfPresent(final Integer idx, final Function<T, U> t2u) {
		final var key = this.keyOfIndex(idx);
		return computeIfPresent(key, t2u);
	}

	/**
	 * k-&gt;t-&gt;u <br>
	 * 获取并变换设置。相当于 相当于 get&amp;set: <br>
	 * U u = mapper.apply(key, (T)get(key)); <br>
	 * set(key,u); <br>
	 * 
	 * @param <T>    变换的mapper源数据类型
	 * @param <U>    变换的mapper额目标结果类型
	 * @param key    键名
	 * @param mapper 二元值变换函数 (key,value0)-&gt;value1
	 * @return 值变换后的数据。
	 */
	default <T, U> U compute(final String key, final BiFunction<String, T, U> mapper) {
		@SuppressWarnings("unchecked")
		final T t = (T) this.get(key);
		final U u = mapper.apply(key, t);
		this.set(key, u);
		return u;
	}

	/**
	 * 把自身 与 rec的kvs对象合并成一个对象
	 * 
	 * @param rec    IRecord类型的 键值对儿集合：记录结构
	 * @param append 是否追加到自身
	 * @return 新合并后的对象 包含有 this,与 rec 的所有属性。
	 */
	default IRecord union(final IRecord rec, boolean append) {
		return union(this, rec, append);
	}

	/**
	 * 把rec中的数据值合并到自身。
	 * 
	 * @param rec     等待添加的record
	 * @param overlap 同名键值是否用 rec来覆盖自身的值
	 * @return 自身对象
	 */
	default IRecord merge(final IRecord rec, final boolean overlap) {
		if (overlap)
			return union(this, rec, true);
		final var keys = new HashSet<>(this.keys());
		final var rec2 = rec.filter(e -> !keys.contains(e.key()));
		if (rec2.size() < 1)
			return this;
		return union(this, rec2, true);
	}

	/**
	 * 把rec中的数据值合并到自身：不进行覆盖
	 * 
	 * @param rec 等待添加的record
	 * @return 自身对象
	 */
	default IRecord merge(final IRecord rec) {
		return this.merge(rec, false);
	}

	/**
	 * 把rec的所有在kvs值添加自身的kvs 之中，采用的是rec的add 方法。<br>
	 * 等效为：union(this, rec, true);
	 * 
	 * @param rec 等待添加的record
	 * @return 自身对象
	 */
	default IRecord add(final IRecord rec) {
		return union(this, rec, true);
	}

	/**
	 * 表头:所有字段名集合
	 * 
	 * @return 所有字段名集合列表
	 */
	default List<String> keys() {
		return this.tupleS().map(Tuple2::_1).collect(Collectors.toList());
	}

	/**
	 * 表头: 所有字段名集合 keys 的别名
	 * 
	 * @return 所有字段名集合列表
	 */
	default List<String> ks() {
		return this.keys();
	}

	/**
	 * 值元素集合的流
	 * 
	 * @param <V>    值类型
	 * @param <T>    目标类型
	 * @param mapper 值映射对象方法
	 * @return 值元素集合的流
	 */
	@SuppressWarnings("unchecked")
	default <V, T> Stream<T> valueS(final Function<V, T> mapper) {
		return this.tupleS().map(e -> mapper.apply((V) e._2()));
	}

	/**
	 * 值的流
	 * 
	 * @param <T>        值元素类型
	 * @param typeholder 类型占位符用于 指定 T的具体类型
	 * @return 值集合列表
	 */
	@SuppressWarnings("unchecked")
	default <T> Stream<T> valueS(final T typeholder) {
		return this.valueS(e -> (T) e);
	}

	/**
	 * 值集合
	 * 
	 * @param <T>    值类型
	 * @param <U>    值类型
	 * @param mapper 值映射对象方法
	 * @return 所有值集合列表
	 */
	default <T, U> List<U> values(final Function<T, U> mapper) {
		return this.valueS(mapper).toList();
	}

	/**
	 * 值集合
	 * 
	 * @param <T>    值元素类型
	 * @param tclazz 类型占位符用于 指定 T的具体类型
	 * @return 值集合列表
	 */
	@SuppressWarnings("unchecked")
	default <T> List<T> values(final Class<T> tclazz) {
		return this.values(e -> (T) e);
	}

	/**
	 * 值集合
	 * 
	 * @return 值集合列表
	 */
	default List<Object> values() {
		return this.values(identity);
	}

	/**
	 * 值集合 (values 的别名)
	 * 
	 * @param <T>    值类型
	 * @param <U>    值类型
	 * @param mapper 值映射目标对象方法
	 * @return 所有值集合列表
	 */
	default <T, U> List<U> vs(final Function<T, U> mapper) {
		return this.values(mapper);
	}

	/**
	 * 值元素集合的流
	 * 
	 * @param <T>        值元素类型
	 * @param typeholder 类型占位符用于 指定 T的具体类型
	 * @return 值元素集合的列表
	 */
	default <T> List<T> vs(final T typeholder) {
		return this.vS(typeholder).toList();
	}

	/**
	 * 值元素集合的流
	 * 
	 * @param <T> 值元素类型
	 * @return 值元素集合的流
	 */
	@SuppressWarnings("unchecked")
	default <T> List<T> vs() {
		return this.values(e -> (T) e);
	}

	/**
	 * 值集合 (values 的别名)
	 * 
	 * @param <T>    值类型
	 * @param mapper 值映射目标对象方法
	 * @return 所有值集合列表
	 */
	default <T> Stream<T> vS(final Function<Object, T> mapper) {
		return this.valueS(mapper);
	}

	/**
	 * 值元素集合的流
	 * 
	 * @param <T>        值元素类型
	 * @param typeholder 类型占位符用于 指定 T的具体类型
	 * @return 值元素集合的流
	 */
	@SuppressWarnings("unchecked")
	default <T> Stream<T> vS(final T typeholder) {
		return this.valueS(e -> (T) e);
	}

	/**
	 * 值元素集合的流
	 * 
	 * @param <T> 值元素类型
	 * @return 值元素集合的流
	 */
	@SuppressWarnings("unchecked")
	default <T> Stream<T> vS() {
		return this.valueS(e -> (T) e);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * 使用示例：<br>
	 * IRecord.STRING2REC("1,2,3,4").arrayOf((String
	 * e)->Integer.parseInt(e),NVec::of).sum();
	 * 
	 * @param <X>         x2t_mapper 的原数据类型
	 * @param <T>         tt2u_mapper 的原数据类型
	 * @param <U>         tt2u_mapper 目标元素的类型
	 * @param x2t_mapper  x->t 元素类型变换函数
	 * @param tt2u_mapper [t]-&gt;u 的数组变换函数
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
	 * @param <U>    mapper 目标元素的类型
	 * @param mapper [o]->u 的数组变换函数
	 * @return U类型结果
	 */
	default <U> U arrayOf(final Function<Object[], U> mapper) {
		final var oo = this.toArray();
		return mapper.apply(oo);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * @param <T>    mapper 的原数据类型
	 * @param <U>    mapper 目标元素的类型
	 * @param mapper [t]-&gt;u 的数组变换函数
	 * @return U类型结果
	 */
	default <T, U> U arrayOf(final Class<T> tclass, final Function<T[], U> mapper) {
		final var tt = this.toArray(tclass);
		return mapper.apply(tt);
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
	 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * @param <U>    目标元素的类型
	 * @param uclass 结果的类型类，这是一个占位符类型，用于提示辅助编译。
	 * @return U类型的一维数组
	 */
	default <U> U[] arrayOf(final Class<U> uclass) {
		return this.toArray(IRecord.coerce(uclass));
	}

	/**
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 Integer,Long,Double <br>
	 * 等，把当前集合中的值集合转换成 一维数组 转换成布尔值数组
	 * 
	 * @return 布尔类型的一维数组
	 */
	default Boolean[] bools() {
		return this.toArray(Boolean.class);
	}

	/**
	 * 把IRecord 视为一个 key-value的序列 <br>
	 * 提取指定键值序列 拼装成一个键值列表
	 * 
	 * @param keys 用逗号分割的键名序列
	 * @return 返回的 IRecord 结构为 key:kkk,value:vvv
	 */
	default List<IRecord> kvs(final String keys) {
		return LIST(kvS(keys));
	}

	/**
	 * 把IRecord 视为一个 key-value的序列 <br>
	 * 提取指定键值序列 拼装成一个键值列表
	 * 
	 * @param keys 用逗号分割的键名序列
	 * @return 返回的IRecord 结构为 key:kkk,value:vvv
	 */
	default List<IRecord> kvs(final String[] keys) {
		return LIST(kvS(keys));
	}

	/**
	 * 把IRecord 视为一个(key,value) 键值对kvp而的序列, <br>
	 * 为 每个kvp构造一个IRecord(key:kkk,value:vvv)<br>
	 * 然后再把这这种IRecord化的kvp给予流序列化。之所以采用IRecord来 转换KVP是为了利用IRecord缩影提供的方法。<br>
	 * 着对于key,value是复杂数据类型的时候非常有用，例如：当key是一个路径的层级信息,value是一个操作语义的数据对象。kvS 后<br>
	 * 就会得到一个 (key:路径,value:对于集合) 的record. ("张三 /苹果",REC("name","卖","quality","5"))
	 * 就得到了<br>
	 * 一个 张三够吗5个苹果的 数据指令。进而可以通过IRecord来提取这些数据内容。比如:<br>
	 * REC("张三 /苹果",REC("name","卖","quality","5")).applyOnKvs( IRecord::STRING2REC,
	 * e->(IRecord)e )<br>
	 * 就可以得到一个 (IRecord,IRecord) 的数据Map，可以分方便的进行 数据分解。<br>
	 * <br>
	 * 
	 * @return 返回的 IRecord 结构为 key:kkk,value:vvv，即一个 IRecord 格式的kv键值对儿记录流
	 */
	default Stream<IRecord> kvS() {
		return kvS((String) null);
	}

	/**
	 * 把IRecord 视为一个 key-value的序列 <br>
	 * 提取指定键值序列 拼装成一个键值列表
	 * 
	 * @param keys 用逗号分割的键名序列:用于过滤返回的key,keys 为null不进行任何过滤，返回全部
	 * @return 返回的 IRecord 结构为 key:kkk,value:vvv
	 */
	default Stream<IRecord> kvS(final String keys) {
		final String kk[] = keys == null ? null : keys.split("[\\s,]+");
		return this.filter(kk).tupleS().map(g -> SimpleRecord.REC2("key", g._1(), "value", g._2()));
	}

	/**
	 * 把IRecord 视为一个 key-value的序列 <br>
	 * 提取指定键值序列 拼装成一个键值列表
	 * 
	 * @param keys 用逗号分割的键名序列:用于过滤返回的key,keys 为null不进行任何过滤，返回全部
	 * @return 返回的 IRecord 结构为 key:kkk,value:vvv
	 */
	default Stream<IRecord> kvS(final String keys[]) {
		return this.filter(keys).tupleS().map(g -> SimpleRecord.REC2("key", g._1(), "value", g._2()));
	}

	/**
	 * 生成一个 记录对象，不会对原来的对象进行更改操作
	 * 
	 * @param key 需要移除的键值名
	 * @return 移除了指定key的字段序列
	 */
	default IRecord remove(final String key) {
		return this.filter(kvp -> !kvp.key().equals(key));
	}

	/**
	 * 生成一个 记录对象，不会对原来的对象进行更改操作
	 * 
	 * @param idx 字段索引编号从0开始
	 * @return 移除了指定idx的字段序列
	 */
	default IRecord remove(final int idx) {
		return this.remove(this.keyOfIndex(idx));
	}

	/**
	 * 使用map 函数变换指定列的数据,以key 所在数值在作为参数调用mapper所指定的函数.
	 * 
	 * @param <T>    目标结果的类型
	 * @param key    键名
	 * @param mapper 键值映射函数
	 * @return T类型的目标结果
	 */
	default <T> T map(final String key, final Function<Object, T> mapper) {
		return mapper.apply(this.get(key));
	}

	/**
	 * 使用map 函数变换指定列的数据,以key 所在数值在作为参数调用mapper所指定的函数.
	 * 
	 * @param <T> t2u定义域类型
	 * @param <U> t2u的值域类型
	 * @param key 键名
	 * @param t2u 键值映射函数
	 * @return 以key 所在数值在作为参数调用mapper所指定的函数.
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U map2(final String key, final Function<T, U> t2u) {
		T t = null;
		try {
			t = (T) this.get(key);
		} catch (Exception ignored) {
		}
		if (t == null) {
			return null;
		}
		return t2u.apply(t);
	}

	/**
	 * 使用map 函数变换指定列的数据,以key 所在数值在作为参数调用mapper所指定的函数.
	 *
	 * @param <T> t2u定义域类型
	 * @param <U> t2u的值域类型
	 * @param idx 键名的索引序号从0开始
	 * @param t2u 键值映射函数
	 * @return 以key 所在数值在作为参数调用mapper所指定的函数.
	 */
	default <T, U> U map2(final int idx, final Function<T, U> t2u) {
		return this.map2(this.keyOfIndex(idx), t2u);
	}

	/**
	 * 强制类型转换，把key 作为T 类型数据
	 * 
	 * @param <T>元素类型
	 * @param id         字段序号，从0开始
	 * @param typeholder 类型占位符
	 * @return T 类型数据
	 */
	@SuppressWarnings("unchecked")
	default <T> T as(final int id, final T typeholder) {
		return (T) (this.get(id));
	}

	/**
	 * 强制类型转换，把key 作为iT 类型数据
	 * 
	 * @param <T>元素类型
	 * @param key        字段名
	 * @param typeholder 类型占位符
	 * @return T类型数据
	 */
	@SuppressWarnings("unchecked")
	default <T> T as(final String key, final T typeholder) {
		return (T) (this.get(key));
	}

	/**
	 * 把键key的视作一个 Function&lt;IRecord,IRecord &gt;
	 * 
	 * @param key 字段名
	 * @return Function&lt;IRecord,IRecord &gt;
	 */
	@SuppressWarnings("unchecked")
	default Function<IRecord, IRecord> fx(final String key) {
		try {
			final var fx = (Function<IRecord, IRecord>) (this.get(key));
			return fx;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 把键key的视作一个 BiFunction &lt;IRecord,IRecord,IRecord &gt;
	 * 
	 * @param key 字段名
	 * @return BiFunction &lt;IRecord,IRecord,IRecord &gt;
	 */
	@SuppressWarnings("unchecked")
	default BiFunction<IRecord, IRecord, IRecord> fx2(final String key) {
		try {
			final var fx2 = (BiFunction<IRecord, IRecord, IRecord>) (this.get(key));
			return fx2;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 把键key的视作一个 Function &lt;List&lt;IRecord&gt;,IRecord &gt; 的函数
	 * 
	 * @param key 字段名
	 * @return Function &lt;List&lt;IRecord&gt;,IRecord &gt;
	 */
	@SuppressWarnings("unchecked")
	default Function<List<IRecord>, IRecord> fxx(final String key) {
		try {
			final var fxx = (Function<List<IRecord>, IRecord>) (this.get(key));
			return fxx;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 把键key的视作一个 BiFunction &lt;List &lt; IRecord &gt;, List &lt; IRecord
	 * &gt;,IRecord &gt;,IRecord &gt;
	 * 
	 * @param key 字段名
	 * @return BiFunction &lt;List &lt; IRecord &gt;,&lt; IRecord &gt;,IRecord &gt;
	 */
	@SuppressWarnings("unchecked")
	default BiFunction<List<IRecord>, List<IRecord>, IRecord> fxx2(final String key) {
		try {
			final var fxx2 = (BiFunction<List<IRecord>, List<IRecord>, IRecord>) (this.get(key));
			return fxx2;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 需要注意：xx 的元素中的每个元素 都是一个独立的列向量，代表一个单独的属性维度。这一点非常重要。<br>
	 * 二元函数的应用 <br>
	 * 这是模仿tensorflow 的session.run 把当前IRecord 视作一个 ops
	 * 集合{key0,op0,key1:op1,key2:op2,...}<br>
	 * 进行一个这样的运算:{key0:op0.apply(x,y),key1:op1.apply(x,y),key2:op2.apply(x,y)} <br>
	 * 将fx视为一个二元函数列表：为每个二元函数 apply 参数 x,y并求值 <br>
	 * 
	 * @param x 输入参数 x
	 * @param y 输入参数 y
	 * @return IRecord
	 */
	default IRecord fapply(IRecord x, IRecord y) {
		return aov2rec((BiFunction<IRecord, IRecord, IRecord> f) -> f.apply(x, y));
	}

	/**
	 * 需要注意：xx 的元素中的每个元素 都是一个独立的列向量，代表一个单独的属性维度。这一点非常重要。<br>
	 * 多元函数的应用 <br>
	 * 将本实例元素视为 一个 多元函数 fxx:[x0,x1,x2,...]->y <br>
	 * List &lt; IRecord &gt; 是列向量构成的数据矩阵 xx:{col0,col2,col2,...} . 把 xx 视做一个
	 * [r0,r1,r2,...] 的行向量的实参集合。 <br>
	 * 把每个行向量应用到 fxx: fxx.apply(r0) ->y0, fxx.apply(r0) ->y1, fxx.apply(r0) ->y2,
	 * ... <br>
	 * 并最终 将 (y0,y1,y2,...) 拼接成一个列向量 result:REC(0,y0,1,y1,2,y2). 并将 result 返回。 <br>
	 * 
	 * @param xx 输入多参数向量
	 * @return fxx 应用之后的 列向量IRecord
	 */
	default IRecord fapply(List<IRecord> xx) {
		return aov2rec((Function<List<IRecord>, IRecord> f) -> f.apply(xx));
	}

	/**
	 * 需要注意：xx 的元素中的每个元素 都是一个独立的列向量，代表一个单独的属性维度。这一点非常重要。<br>
	 * 多元函数的应用 <br>
	 * 将本实例元素视为 一个 多元函数 fxx:[x0,x1,x2,...]->y <br>
	 * List &lt; IRecord &gt; 是列向量构成的数据矩阵 xx:{col0,col2,col2,...} . 把 xx 视做一个
	 * [r0,r1,r2,...] 的行向量的实参集合。 <br>
	 * 把每个行向量应用到 fxx: fxx.apply(r0) ->y0, fxx.apply(r0) ->y1, fxx.apply(r0) ->y2,
	 * ... <br>
	 * 并最终 将 (y0,y1,y2,...) 拼接成一个列向量 result:REC(0,y0,1,y1,2,y2). 并将 result 返回。 <br>
	 * 
	 * @param xx 参数1
	 * @param yy 参数2
	 * @return fxx 应用之后的 列向量IRecord
	 */
	default IRecord fapply(final List<IRecord> xx, final List<IRecord> yy) {
		return aov2rec((BiFunction<List<IRecord>, List<IRecord>, IRecord> f) -> f.apply(xx, yy));
	}

	/**
	 * 视IRecord为函数集:<br>
	 * 用key0的函数 f 去 组合 (compose ) key1的函数 g,获得一个 f.compose(g)的函数 即：<br>
	 * this.fx(key0).compose(this.fx(key1));<br>
	 * 
	 * @param key0 字段名0
	 * @param key1 字段名1
	 * @return Function&lt;IRecord,IRecord &gt;
	 */
	default Function<IRecord, IRecord> compose(final String key0, String key1) {
		return this.fx(key0).compose(this.fx(key1));
	}

	/**
	 * 视IRecord为函数集:<br>
	 * 每个key标识一个函数fx:(rec0,rec1)->rec <br>
	 * 从键名序列key0,key1提取出函数对儿 (fx0,fx1),对于 函数对儿(fx0,fx1)应用输入对儿(rec00,rec01）<br>
	 * 获得输出值对儿(rec10,rec11),fx0(rec01,rec02)对应rec10,fx1(rec01,rec02)对应rec11,
	 * 合在一起就构成了(rec10,rec11),然后对输出值对儿(rec10,rec11)应用gx获取[rec20,rec21]
	 * 即生成一个:(rec00,rec01）->[fx:key1,key2] ->gx->[rec20,rec21]的函数 <br>
	 * 
	 * @param gx   复合函数
	 * @param key0 字段名0
	 * @param key1 字段名1
	 * @return Function&lt;IRecord,IRecord &gt;
	 */
	default BiFunction<IRecord, IRecord, IRecord> compose(final BiFunction<IRecord, IRecord, IRecord> gx,
			final String key0, final String key1) {
		return (x, y) -> gx.apply(this.fx2(key0).apply(x, y), this.fx2(key1).apply(x, y));
	}

	/**
	 * 视IRecord为函数集:<br>
	 * 每个key标识一个函数fx:[rec0]->rec1 <br>
	 * 从键名序列keys 提取出函数列表[fx],对于[fx] 应用输入列表[rec0] 获得输出列表[rec1],<br>
	 * 一个fx对应一个rec1,合在一起就构成了[rec1]<br>
	 * 然后对[rec1]应用gx.<br>
	 * 即生成一个:[rec0]->[fx:keys]->gx->[rec1] 的函数 <br>
	 * 
	 * @param gx   复合函数
	 * @param keys 字段名称序列
	 * @return Function&lt;IRecord,IRecord &gt;
	 */
	default Function<List<IRecord>, IRecord> compose(final Function<List<IRecord>, IRecord> gx, final String... keys) {

		return (xx) -> gx.apply(Stream.of(keys).map(this::fxx).filter(Objects::nonNull).map(f -> f.apply(xx)).toList());
	}

	/**
	 * 检索满足条件所有键值对儿
	 * 
	 * @param predicate 字段名
	 * @return 满足 predicate 条件的键值对儿
	 */
	default List<KVPair<String, Object>> find(final Predicate<KVPair<String, Object>> predicate) {
		if (predicate == null)
			return null;
		return this.kvs().stream().filter(predicate).collect(Collectors.toList());
	}

	/**
	 * 检索满足条件的第一个键值对儿
	 * 
	 * @param predicate 字段名
	 * @return 满足 predicate 条件的键值对儿
	 */
	default Optional<KVPair<String, Object>> findOne(final Predicate<KVPair<String, Object>> predicate) {
		if (predicate == null)
			return Optional.empty();
		return this.kvs().stream().filter(predicate).findFirst();
	}

	/**
	 * 检索值类型为clazz 类型的对象。检索成功返回该值，不成功 null
	 * 
	 * @param <T>   检索的目标对象的类型
	 * @param clazz 目标值的类型
	 * @return 检索成功返回该值，不成功 null
	 */
	@SuppressWarnings("unchecked")
	default <T> T findOne(Class<T> clazz) {
		final var opt = this.kvs().stream()
				.filter(e -> e.value() != null && clazz.isAssignableFrom(e.value().getClass())).findFirst();
		return opt.map(stringObjectKVPair -> (T) stringObjectKVPair.value()).orElse(null);
	}

	/**
	 * 检索值类型为clazz 类型的对象。检索成功返回该值，不成功 null, 并对 T类型的结果调用 mapper给予变换 <br>
	 * 
	 * @param <T>    检索的目标对象的类型
	 * @param <U>    变换结果
	 * @param clazz  目标值的类型
	 * @param mapper T-&gt;U 的变换函数
	 * @return 检索成功返回该值，不成功 null
	 */
	default <T, U> U findOne2(Class<T> clazz, Function<T, U> mapper) {
		final T t = this.findOne(clazz);
		return mapper.apply(t);
	}

	/**
	 * 强制类型转换，把key 作为T 类型数据,转换成函数,一般用于lambda表达式提取
	 * 
	 * @param key 字段名
	 * @return Function类型的对象
	 */
	@SuppressWarnings("rawtypes")
	default Function func(final String key) {
		return as(key, (Function) null);
	}

	/**
	 * 普通函数求值<br>
	 * 强制类型转换，把key 作为T 类型数据,转换成函数,一般用于lambda表达式提取<br>
	 * 
	 * @param <T> 函数的参数类型
	 * @param <U> 函数返回值类型
	 * @param key 键名
	 * @param arg 字段名, 提交各key:t2u 函数的 参数对象
	 * @return 调用与key绑定的t2u.apply(arg)
	 */
	default <T, U> U eval(final String key, final T arg) {
		final var foo = as(key, (Function<T, U>) null);
		if (foo == null)
			return null;
		U u = foo.apply(arg);
		return u;
	}

	/**
	 * 强制类型转换，把key 作为T 类型数据,转换成函数,一般用于lambda表达式提取
	 * 
	 * @param <FUNC>       函数类型
	 * @param <U>          目标结果类型
	 * @param key          键名
	 * @param functorClass 函数类
	 * @param args         函数的实际参数
	 * @return 调用与key绑定的t2u.apply(arg)
	 */
	@SuppressWarnings("unchecked")
	default <FUNC, U> U eval2(final String key, final Class<FUNC> functorClass, final Object... args) {
		FUNC func = as(key, (FUNC) null);
		if (ANNO(functorClass, FunctionalInterface.class) == null) {
			System.out.println(functorClass + " 需要时一个函数接口，必须强制用@FunctionalInterface标记！");
			return null;
		} // if
		final var method = functorClass.getMethods()[0];
		U obj = null;// 返回值
		try {
			obj = (U) method.invoke(func, args);
		} catch (Exception e) {
			e.printStackTrace();
		} // try
		return obj;
	}

	/**
	 * 强制类型转换，把key 作为iT 类型数据,转换成函数,一般用于lambda表达式提取
	 * 
	 * @param key  字段名
	 * @param args 回调函数的参数
	 */
	default <T> void callback(final String key, final T args) {
		final var foo = as(key, (Consumer<T>) null);
		if (foo == null)
			return;
		foo.accept(args);
	}

	/**
	 * 强制类型转换，<br>
	 * 一般用于lambda表达式类型的值<br>
	 * 把key 作为T-&gt;U类型函数:t2u<br>
	 * 返回一个 应用了 arg:T的结果u:U类型 ,t2u.apply(arg)<br>
	 * <br>
	 * 
	 * @param <T>        参数类型
	 * @param <U>        结果类型
	 * @param arg        函数（value)的参数
	 * @param typeholder 类型占位符,这是一个类型占位符,用于向编译器提供类型信息，并无实际运算意义。
	 * @return U类型数据
	 */
	default <T, U> U evaluate(final int idx, final T arg, final U typeholder) {
		return this.evaluate(this.keyOfIndex(idx), arg, typeholder);
	}

	/**
	 * 强制类型转换，<br>
	 * 一般用于lambda表达式类型的值<br>
	 * 把key 作为T-&gt;U类型函数:t2u<br>
	 * 返回一个 应用了 arg:T的结果u:U类型 ,t2u.apply(arg)<br>
	 * <br>
	 * 
	 * @param <T>        t2u即key值 的参数的类型
	 * @param <U>        t2u即key值 的结果类型
	 * @param key        字段名即t2u的函数的键名
	 * @param arg        t2u的具体参数
	 * @param typeholder 类型占位符
	 * @return U类型数据
	 */
	default <T, U> U evaluate(final String key, final T arg, final U typeholder) {
		return this.as(key, (Function<T, U>) null).apply(arg);
	}

	/**
	 * 强制类型转换，把key 作为T 类型数据,转换成函数,一般用于lambda表达式提取
	 * 
	 * @param key 字段名
	 * @return IRecord 结果的数据
	 */
	default <T, U> IRecord bind(final String key, final Function<T, U> transform, final String newKey) {
		final U obj = this.eval(key, transform);
		return REC(newKey == null ? key : newKey, obj);
	}

	/**
	 * 强制类型转换，把key 作为 T 类型数据,转换成函数,一般用于lambda表达式提取
	 * 
	 * @param transform 变换函数
	 * @return bind 结果的数据
	 */
	@SuppressWarnings("unchecked")
	default <T, U> IRecord bind(final Function<T, U> transform) {
		final IRecord rec = REC();
		final Function<T, U> foo = t -> {
			U u = null;
			try {
				u = transform.apply(t);
			} catch (Exception ex) {
				// ex.printStackTrace();
			} // try
			return u;
		};

		this.kvs().stream().map(e -> new Tuple2<>(e._1(), foo.apply((T) get(e._1())))).forEach(kv -> {
			if (kv._2 != null)
				rec.add(kv._1, kv._2);
		});
		return rec;
	}

	/**
	 * Record 归集器:这个方法是为了放置编译器抱怨ambiguous错误
	 * 
	 * @param <R>         规约结果的类型
	 * @param supplier    容器:()->r0
	 * @param accumulator 累加:(r0,kv)->r1
	 * @param combiner    和兵器:(r1,r2)->r3
	 * @return 规约的结果 R
	 */
	default <R> R collect0(final Supplier<R> supplier, final BiConsumer<R, KVPair<String, Object>> accumulator,
			final BinaryOperator<R> combiner) {
		final var collector = Collector.of(supplier, accumulator, combiner);
		return this.tupleS().collect(collector);
	};

	/**
	 * Record 归集器:这个方法是为了放置编译器抱怨ambiguous错误<br>
	 * 默认 combiner 为:(a,b)->a
	 * 
	 * @param <R>         规约结果的类型
	 * @param supplier    容器:()->r0
	 * @param accumulator 累加:可以带有一个返回值，这个返回值会被忽略掉:(r0,kv)->r1
	 * @return 规约的结果 R
	 */
	default <R> R collect(final Supplier<R> supplier, final BiConsumer<R, KVPair<String, Object>> accumulator) {
		return this.collect0(supplier, accumulator, (a, b) -> a);
	};

	/**
	 * Record 归集器
	 * 
	 * @param <R>         规约结果的类型
	 * @param supplier    容器:()->r0
	 * @param accumulator 累加:(r0,kv)->r1
	 * @param combiner    合并器:(r1,r2)-&gt;r3
	 * @return 规约的结果 R
	 */
	default <R> R collect(final Supplier<R> supplier, final BiConsumer<R, KVPair<String, Object>> accumulator,
			final BinaryOperator<R> combiner) {
		return this.collect0(supplier, accumulator, combiner);
	};

	/**
	 * Record 归集器
	 * 
	 * @param <R>         结果的类型
	 * @param initial     初始值:r0
	 * @param accumulator 累加器:(r0,kv)-&gt;r1
	 * @param combiner    合并器:(r1,r2)-&gt;r3
	 * @return 规约的结果 R
	 */
	default <R> R collect(final R initial, final BiConsumer<AtomicReference<R>, KVPair<String, Object>> accumulator,
			final BinaryOperator<R> combiner) {

		final var collector = Collector.of(() -> new AtomicReference<>(initial), accumulator,
				(aa, bb) -> new AtomicReference<>(combiner.apply(aa.get(), bb.get())));
		return this.tupleS().collect(collector).get();
	};

	/**
	 * Record 归集器
	 * 
	 * @param <A>       累加器的元素 类型 中间结果类型,用于暂时存放 累加元素的中间结果的集合。
	 * @param <R>       返回结果类型
	 * @param collector 搜集 KVPair&lt;String,Object&gt;类型的 归集器
	 * @return 规约的结果 R
	 */
	default <A, R> R collect(final Collector<KVPair<String, Object>, A, R> collector) {
		return this.tupleS().collect(collector);
	};

	/**
	 * DFrame 函数 <br>
	 * Row collect <br>
	 * 把IRecord视为一个DFrame dfm,而后 把dfm转换成个一个行流，最后使用 collector进行归集。<br>
	 * 
	 * @param <R>       结果类型
	 * @param collector 行归集器
	 * @return R类型的结果
	 */
	default <R> R rcollect(final Collector<IRecord, List<IRecord>, R> collector) {
		return IRecord.ROWSCLC(collector).apply(this);
	}

	/**
	 * DFrame 函数 (rcollect2 的后缀2表示这是一个队KVPair 进行归集的版本） <br>
	 * Row KVPair 的 collect <br>
	 * 把IRecord视为一个DFrame dfm,而后 把dfm转换成个一个行流KVPair用key_idx,value_idx标定键名与键值，最后使用
	 * collector进行归集。<br>
	 * 
	 * @param key_idx   key 索引 从0开始
	 * @param value_idx value 索引从0开始
	 * @param collector 行归集器
	 * @return R类型的结果
	 */
	default <R> R rcollect2(final int key_idx, final int value_idx,
			final Collector<KVPair<String, ?>, List<KVPair<String, Object>>, R> collector) {
		return this.rowS().map(e -> KVPair.KVP(e.str(key_idx), e.get(value_idx))).collect(collector);
	}

	/**
	 * DFrame 函数 (rcollect2 的后缀2表示这是一个队KVPair 进行归集的版本） <br>
	 * Row KVPair 的 collect <br>
	 * 把IRecord视为一个DFrame dfm,而后 把dfm转换成个一个行流KVPair用key_name,value_name标定键名与键值，最后使用
	 * collector进行归集。<br>
	 * 
	 * @param key_name   key 键名
	 * @param value_name value 键名
	 * @param collector  行归集器
	 * @return R类型的结果
	 */
	default <R> R rcollect2(final String key_name, final String value_name,
			final Collector<KVPair<String, ?>, List<KVPair<String, Object>>, R> collector) {
		return this.rowS().map(e -> KVPair.KVP(e.str(key_name), e.get(value_name))).collect(collector);
	}

	/**
	 * DFrame 函数 (rcollect2 的后缀2表示这是一个队KVPair 进行归集的版本）<br>
	 * Row KVPair 的 collect <br>
	 * 把把IRecord视为一个DFrame dfm,而后 把dfm转换成个一个行流KVPair用0,1标定键名与键值，最后使用
	 * collector进行归集。<br>
	 * rcollect（0,1,collector)的别名函数
	 * 
	 * @param collector 行归集器
	 * @return R类型的结果
	 */
	default <R> R rcollect2(final Collector<KVPair<String, ?>, List<KVPair<String, Object>>, R> collector) {
		return this.rcollect2(0, 1, collector);
	}

	/**
	 * 把KV值规约到一个数值R
	 * 
	 * @param <R>     规约结果的类型
	 * @param kv2r    KV值转换成R类型数据。kv->r
	 * @param initial 初始值:r0
	 * @param reducer 规约算法:(r0,r1)-&gt;r2
	 * @return 规约的结果 R
	 */
	default <R> R reduce(final Function<KVPair<String, ?>, R> kv2r, final R initial, final BinaryOperator<R> reducer) {
		final Collector<KVPair<String, Object>, AtomicReference<R>, AtomicReference<R>> collector = Collector.of(
				() -> new AtomicReference<>(initial), (aa, b) -> aa.set(reducer.apply(aa.get(), kv2r.apply(b))),
				(aa, bb) -> new AtomicReference<>(reducer.apply(aa.get(), bb.get())));
		return this.tupleS().collect(collector).get();
	};

	/**
	 * 左折叠
	 * 
	 * @param <R>     规约结果的类型
	 * @param initial 初始值:r0
	 * @param op      折叠函数 (r0,kv)->r1
	 * @return R 的累计结果
	 */
	default <R> R foldLeft(final R initial, final BiFunction<R, KVPair<String, ?>, R> op) {
		var ar = new AtomicReference<>(initial);
		this.tupleS().forEach(kv -> ar.set(op.apply(ar.get(), kv)));
		return ar.get();
	};

	/**
	 * 把键值对儿中的值数据 用 delim 进行串联： [0:1 1:2 2:3 3:4] 的返回结果是: 1/2/3/4
	 * 
	 * @param delim 分隔符
	 * @return delim 分割的values序列
	 */
	default String vvjoin(String delim) {
		return this.foldLeft((String) null,
				(r, kv) -> Jdbcs.format("{0}{1}{2}", r == null ? "" : r, r == null ? "" : delim, kv._2()));
	};

	/**
	 * 把键值对儿中的值数据 用 delim 进行串联： [0:1 1:2 2:3 3:4] 的返回结果是: 1/2/3/4 delim 分隔符 默认为 ：“/”
	 * 
	 * @return delim 分割的values序列
	 */
	default String vvjoin() {
		final var delim = "/";
		return this.foldLeft((String) null,
				(r, kv) -> Jdbcs.format("{0}{1}{2}", r == null ? "" : r, r == null ? "" : delim, kv._2()));
	};

	/**
	 * 右折叠
	 * 
	 * @param <R>     规约结果的类型
	 * @param initial 初始值:r0
	 * @param op      折叠函数 ：(kv,r0)->r1
	 * @return 右折叠的累计结果
	 */
	default <R> R foldRight(R initial, final BiFunction<KVPair<String, ?>, R, R> op) {
		final var ar = new AtomicReference<>(initial);
		final var kvs = this.kvs();
		final var reverse_litr = kvs.listIterator(kvs.size());// 移动到列表末尾
		while (reverse_litr.hasPrevious())
			ar.set(op.apply(reverse_litr.previous(), ar.get()));
		return ar.get();
	};

	/**
	 * 把KV值规约到一个整型
	 *
	 * @param initial 初始值:r0
	 * @param reducer 规约算法:(r0,r1)-&gt;r2
	 * @return 规约的结果
	 */
	default Integer reduce(final Integer initial, BinaryOperator<Integer> reducer) {
		return this.reduce(kv2int, initial, reducer);
	}

	/**
	 * 把KV值规约到一个数值:Double 类型
	 *
	 * @param initial 初始值:r0
	 * @param reducer 规约算法:(r0,r1)-&gt;r2
	 * @return 规约的结果
	 */
	default Double reduce(final Double initial, final BinaryOperator<Double> reducer) {
		return this.reduce(kv2dbl, initial, reducer);
	}

	/**
	 * 把KV值规约到一个数值R:Long 类型
	 *
	 * @param initial 初始值:r0
	 * @param reducer 规约算法:(r0,r1)-&gt;r2
	 * @return 规约的结果
	 */
	default Long reduce(final Long initial, final BinaryOperator<Long> reducer) {
		return this.reduce(kv2lng, initial, reducer);
	}

	/**
	 * 数据窗口滑动：step 每次移动的步长为1<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param size 滑动的窗口大小
	 * @return 滑动窗口的列表。
	 */
	default List<List<KVPair<String, Object>>> sliding(final int size) {
		return sliding(this.kvs(), size, 1);
	}

	/**
	 * slidingStream 数据窗口滑动：step 每次移动的步长为1<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param size 滑动的窗口大小
	 * @return 滑动窗口的Stream&lt;List&lt;KVPair&lt;String,Object&gt;&gt;&gt;
	 */
	default Stream<List<KVPair<String, Object>>> sliding2(final int size) {
		return sliding(this.kvs(), size, 1).stream();
	}

	/**
	 * sliding2 分解成一个窗口长度为2的收尾向量的线段:线段空间。 数据窗口滑动：step 每次移动的步长为1<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param discardSinglePoint 是否把单点元素给剔除, 比如对于 [ [1,2], [2,3], [3,4], [4] ] <br>
	 *                           true: 只返回 [ [1,2], [2,3], [3,4]] <br>
	 *                           false: 将返回[ [1,2], [2,3], [3,4],[4,null]] <br>
	 * @return 滑动窗口的滑动窗口的Stream&lt;Tuple2&lt;KVPair&lt;String,Object&gt;,KVPair&lt;String,Object&gt;&gt;&gt;
	 */
	default Stream<Tuple2<KVPair<String, Object>, KVPair<String, Object>>> sliding2(boolean discardSinglePoint) {
		return sliding(this.kvs(), 2, 1).stream().filter(e -> !discardSinglePoint || e.size() == 2)
				.map(e -> Tuple2.TUP2(e.get(0), e.size() > 1 ? e.get(1) : null));
	}

	/**
	 * sliding2 分解成一个窗口长度为2的收尾向量的线段:线段空间。 数据窗口滑动：step 每次移动的步长为1<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4] ] 对结果进行截取没有 [4,null]节点 <br>
	 * 
	 * @return 滑动窗口的Stream&lt;Tuple2&lt;KVPair&lt;String,Object&gt;,KVPair&lt;String,Object&gt;&gt;&gt;
	 */
	default Stream<Tuple2<KVPair<String, Object>, KVPair<String, Object>>> sliding2() {
		return this.sliding2(true);
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,5按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ U1, U2, U3, U4 ]<br>
	 * mapper.apply([1,2])==U1 mapper.apply([2,3])==U2
	 * 
	 * @param <U>    元素类型
	 * @param size   滑动的窗口大小
	 * @param step   每次移动的步长
	 * @param mapper 窗口变换函数
	 * @return 滑动窗口的列表。
	 */
	default <U> Stream<U> sliding2(final int size, final int step,
			final Function<List<KVPair<String, Object>>, U> mapper) {
		return sliding(this.kvs(), size, step).stream().map(mapper);
	}

	/**
	 * slidingStream 数据窗口滑动：step 每次移动的步长为1<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param size 滑动的窗口大小
	 * @return 滑动窗口的Stream&lt;List&lt;KVPair&lt;String,Object&gt;&gt;&gt;
	 */
	default Stream<IRecord> slidingS(final int size) {
		return this.slidingS(size, false);
	}

	/**
	 * slidingStream 数据窗口滑动：step 每次移动的步长为1<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param size               滑动的窗口大小
	 * @param discardSinglePoint 是否把单点元素给剔除, 比如对于 [ [1,2], [2,3], [3,4], [4] ] <br>
	 *                           true: 只返回 [ [1,2], [2,3], [3,4]] <br>
	 *                           false: 将返回[ [1,2], [2,3], [3,4],[4,null]] <br>
	 * @return 滑动窗口的Stream&lt;List&lt;KVPair&lt;String,Object&gt;&gt;&gt;
	 */
	default Stream<IRecord> slidingS(final int size, final boolean discardSinglePoint) {
		return sliding(this.kvs(), size, 1).stream().filter(e -> !discardSinglePoint ? true : e.size() == size)
				.map(IRecord::KVS2REC);
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param size 滑动的窗口大小
	 * @param step 每次移动的步长
	 * @return 滑动窗口的列表。
	 */
	default List<List<KVPair<String, Object>>> sliding(final int size, final int step) {
		return sliding(this.kvs(), size, step);
	}

	/**
	 * 根据键名列表集合 对IRecord 进行拆分
	 * 
	 * @param keys 键名列表集合 ["k11,k12,k13","k11,k12,k13",...,] 单项键名间用逗号分隔
	 * @return 使用键名序列拆分的IRecord 流
	 */
	default Stream<IRecord> splitS(final String... keys) {
		return this.flatMap(selects(keys));
	}

	/**
	 * 根据键名列表集合 对IRecord 进行拆分
	 * 
	 * @param keys 键名列表集合 [[k11,k12,k13],[k11,k12,k13],....,]
	 * @return 使用键名序列拆分的IRecord 流
	 */
	default Stream<IRecord> splitS(final String[]... keys) {
		return this.flatMap(selects(keys));
	}

	/**
	 * 根据键名列表集合 对IRecord 进行拆分
	 * 
	 * @param idxes 索引列表,索引号从0开始
	 * @return 使用键名序列拆分的IRecord
	 */
	default Stream<IRecord> splitS(final Integer[]... idxes) {
		return this.flatMap(selects(idxes));
	}

	/**
	 * 根据键名列表集合 对IRecord 进行拆分
	 * 
	 * @param keys 键名列表集合 ["k11,k12,k13","k11,k12,k13",...,] 单项键名间用逗号分隔
	 * @return 使用键名序列拆分的IRecord 列表
	 */
	default List<IRecord> split(final String... keys) {
		return this.splitS(keys).collect(Collectors.toList());
	}

	/**
	 * 根据键名列表集合 对IRecord 进行拆分
	 * 
	 * @param keys 键名列表集合 [[k11,k12,k13],[k11,k12,k13],....,]
	 * @return 使用键名序列拆分的IRecord 列表
	 */
	default List<IRecord> split(final String[]... keys) {
		return this.splitS(keys).collect(Collectors.toList());
	}

	/**
	 * 根据键名列表集合 对IRecord 进行拆分
	 * 
	 * @param idxes 索引列表,索引号从0开始
	 * @return 使用键名序列拆分的IRecord 列表
	 */
	default List<IRecord> split(final Integer[]... idxes) {
		return this.splitS(idxes).collect(Collectors.toList());
	}

	/**
	 * 使用map 函数变换指定列的数据
	 * 
	 * @param idx    键名索引 从0开始
	 * @param mapper 变换函数
	 * @param <T>    mapper 的转换类型
	 * @return map 函数变换指定列的数据
	 */
	default <T> T map(final int idx, final Function<Object, T> mapper) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : map(key, mapper);
	}

	/**
	 * 把key列转换成字符串
	 * 
	 * @param key 键名
	 * @return 字符串数据
	 */
	default String str(final String key) {
		return map(key, o -> o == null ? null : o.toString());
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param key 键名
	 * @return 字符串类型
	 */
	default String str(final String key, final String default_value) {
		final var ret = this.str(key);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成字符串
	 * 
	 * @param idx 从0开始
	 * @return 字符串
	 */
	default String str(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : str(key);
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param idx 索引编号 从0开始
	 * @return 字符串类型
	 */
	default String str(final int idx, final String default_value) {
		final var ret = this.str(idx);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成字符串
	 * 
	 * @param key 键名
	 * @return 短整型
	 */
	default Short i2(final String key) {
		final var num = this.num(key);
		return num == null ? null : num.shortValue();
	}

	/**
	 * 把key列转换成字符串
	 * 
	 * @param key 键名
	 * @return 短整型
	 */
	default Short i2(final String key, final short default_value) {
		final var num = this.num(key);
		return num == null ? default_value : num.shortValue();
	}

	/**
	 * 把key列转换成字符串
	 * 
	 * @param index 从0开始
	 * @return 短整型
	 */
	default Short i2(final int index) {
		final var num = this.num(index);
		return num == null ? null : num.shortValue();
	}

	/**
	 * 把key列转换成字符串
	 * 
	 * @param index 从0开始
	 * @return 短整型
	 */
	default Short i2(final int index, final short default_value) {
		final var num = this.num(index);
		return num == null ? default_value : num.shortValue();
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param key 键名
	 * @return Double 双精度浮点数
	 */
	default Double dbl(final String key) {
		final var num = this.num(key);
		return num == null ? null : num.doubleValue();
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param key 键名
	 * @return Double 双精度浮点数
	 */
	default Double dbl(final String key, final Double default_value) {
		final var ret = this.dbl(key);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param idx 键名索引从0开始
	 * @return 双精度浮点数
	 */
	default Double dbl(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : dbl(key);
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param idx           键名索引 从0开始
	 * @param default_value 默认值
	 * @return Double 双精度浮点数
	 */
	default Double dbl(final int idx, final Double default_value) {
		final var ret = this.dbl(idx);
		return ret == null ? default_value : ret;
	}

	/**
	 * 用法示例： System.out.println(REC("0","1.1234").i4(0)); --> 1 <br>
	 * System.out.println(REC("0",".1234").i4(0)); -->0 <br>
	 * System.out.println(REC("0","1.1234").dbl(0)); -->1.1234 <br>
	 * System.out.println(REC("0","00.1234").dbl(0)); -->0.1234 <br>
	 * System.out.println(REC("0","0.0.1234").dbl(0)); -->null <br>
	 * 
	 * 把key列的值转换成数字，除了数字以外其中： 字符串："true","fasle" 会被分别被转换成 1和0； bool型： true,fasle
	 * <br>
	 * 会被分别被转换成 1和0 <br>
	 * 
	 * @param key 键名
	 * @return 整数值
	 */
	default Integer i4(final String key) {
		final var num = this.num(key);// 转换成数字
		return num == null ? null : num.intValue();
	}

	/**
	 * 把key列的值转换成数字，除了数字以外其中： 字符串："true","fasle" 会被分别被转换成 1和0； bool型： true,fasle
	 * 会被分别被转换成 1和0
	 * 
	 * @param key 键名
	 * @return Integer 整数值
	 */
	default Integer i4(final String key, Integer default_value) {
		final var ret = this.i4(key);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param idx 键名索引 从0开始
	 * @return Integer 整数值
	 */
	default Integer i4(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : i4(key);
	};

	/**
	 * 把key列的值转换成数字，除了数字以外其中： 字符串："true","fasle" 会被分别被转换成 1和0； bool型： true,fasle
	 * 会被分别被转换成 1和0
	 * 
	 * @param idx 键名 的索引号,从0开始
	 * @return Integer 整数值
	 */
	default Integer i4(final int idx, Integer default_value) {
		final var ret = this.i4(idx);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成数字
	 * 
	 * @param key 键名
	 * @return 长整型
	 */
	default Long lng(final String key) {
		return map(key, o -> {
			Long lng = null; // 返回结果
			if (o instanceof Number)
				return ((Number) o).longValue();
			try {
				lng = Long.parseLong(o + "");
			} catch (Exception e) {
				try {
					lng = ((Number) Double.parseDouble(o + "")).longValue();
				} catch (Exception ignored) {
				} // try
			} // try
			return lng;
		});
	};

	/**
	 * 把key列转换成数字
	 * 
	 * @param key           键名
	 * @param default_value 默认值
	 * @return 长整型
	 */
	default Long lng(final String key, Long default_value) {
		final var value = this.lng(key);
		return value == null ? default_value : value;
	}

	/**
	 * 把key列转换成浮点数
	 * 
	 * @param idx 键名索引 从0开始
	 * @return 长整型
	 */
	default Long lng(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : lng(key);
	};

	/**
	 * 把idx列索引所标识的键名列值转换成数字
	 * 
	 * @param idx           键名索引 从0开始
	 * @param default_value 默认值
	 * @return 长整型
	 */
	default Long lng(final int idx, Long default_value) {
		final var value = this.lng(idx);
		return value == null ? default_value : value;
	}

	/**
	 * 把key字段转换成数字<br>
	 * 布尔类型的值:false 转换成 0, true 转换成1
	 * 
	 * @param key 键名
	 * @return 数值类型
	 */
	default Number num(final String key) {
		return map(key, o -> {

			if (o instanceof Number)
				return ((Number) o);// 数字值提取
			if (o instanceof Date)
				return (Number) ((Date) o).getTime();// 日期转数字,java.sql.timestamp 是 Date的子类
			if (o instanceof LocalDate)
				return (Number) (Times.ld2dt((LocalDate) o)).getTime();// 日期转数字
			if (o instanceof LocalDateTime)
				return (Number) (Times.ldt2dt((LocalDateTime) o)).getTime();// 日期转数字
			if (o instanceof LocalTime)
				return (Number) (Times.lt2dt((LocalTime) o)).getTime();// 日期转数字
			if (o instanceof Boolean)
				return ((Boolean) o) ? 1 : 0;

			Double dbl = null;
			try {
				dbl = Double.parseDouble(o + "");
			} catch (Exception ignored) {
			} // 默认使用Double进行数据解析

			return dbl;
		});// map
	};

	/**
	 * 把key字段转换成数字
	 * 
	 * @param key           键名
	 * @param default_value 默认值
	 * @return Number
	 */
	default Number num(final String key, Number default_value) {
		final var ret = this.num(key);
		return ret == null ? default_value : ret;
	};

	/**
	 * 把key字段转换成数字
	 * 
	 * @param idx 键名索引 从0开始
	 * @return 可能返回空值
	 */
	default Number num(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : num(key);
	};

	/**
	 * 把key字段转换成数字
	 * 
	 * @param idx           键名索引，从0开始
	 * @param default_value 默认值
	 * @return Number
	 */
	default Number num(final int idx, Number default_value) {
		final var ret = this.num(idx);
		return ret == null ? default_value : ret;
	};

	/**
	 * 把key字段转换成 异常对象
	 * 
	 * @param key 键名
	 * @return 例外异常
	 */
	default Exception except(final String key) {
		return map(key, o -> {
			if (o instanceof Exception)
				return ((Exception) o);// 数字值提取
			else
				return null;
		});
	}

	/**
	 * 把key字段转换成 异常对象
	 * 
	 * @param idx 键名索引 从0开始
	 * @return 例外异常
	 */
	default Exception except(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : except(key);
	};

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key 键名
	 * @return Timestamp 时间错
	 */
	default Timestamp timestamp(final String key) {
		return map(key, o -> {
			if (o == null)
				return null;
			if (o instanceof Timestamp)
				return (Timestamp) o;
			final var time = this.date(key);
			if (time == null)
				return null;
			return new Timestamp(time.getTime());
		});
	};

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key           键名
	 * @param default_value 默认值
	 * @return Timestamp
	 */
	default Timestamp timestamp(final String key, Timestamp default_value) {
		return key == null ? default_value : timestamp(key);
	};

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx 键名索引 从0开始
	 * @return Timestamp
	 */
	default Timestamp timestamp(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : timestamp(key);
	};

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx           键名索引 从0开始
	 * @param default_value 默认值
	 * @return Timestamp
	 */
	default Timestamp timestamp(final int idx, Timestamp default_value) {
		final String key = this.keyOfIndex(idx);
		return key == null ? default_value : timestamp(key);
	};

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key 键名
	 * @return LocalDateTime
	 */
	default LocalDateTime ldt(final String key) {
		Object obj = this.get(key);
		if (obj == null)
			return null;
		if (obj instanceof LocalDateTime)
			return (LocalDateTime) obj;
		return Times.date2localDateTime(this.date(key));
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key           键名
	 * @param default_value 默认值,当 值为null时候返回
	 * @return LocalDateTime
	 */
	default LocalDateTime ldt(final String key, LocalDateTime default_value) {
		final var ret = this.ldt(key);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx 从0开始
	 * @return LocalDateTime
	 */
	default LocalDateTime ldt(final int idx) {
		return Times.dt2ldt(this.date(keyOfIndex(idx)));
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx           从0开始
	 * @param default_value 默认值,当 值为null时候返回
	 * @return LocalDateTime
	 */
	default LocalDateTime ldt(final int idx, LocalDateTime default_value) {
		final var ret = this.ldt(idx);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key 键名
	 * @return LocalDate
	 */
	default LocalDate ld(final String key) {
		final Object obj = this.get(key);
		if (obj == null)
			return null;
		if (obj instanceof LocalDate)
			return (LocalDate) obj;
		return Times.dt2ld(this.date(key));
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key           键名
	 * @param default_value 默认值,当 值为null时候返回
	 * @return LocalDate
	 */
	default LocalDate ld(final String key, LocalDate default_value) {
		final var ret = ld(key);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx 从0开始
	 * @return LocalDate
	 */
	default LocalDate ld(final int idx) {
		final Object obj = this.get(idx);
		if (obj == null)
			return null;
		if (obj instanceof LocalDate)
			return (LocalDate) obj;
		return Times.dt2ld(this.date(idx));
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx           从0开始
	 * @param default_value 默认值,当 值为null时候返回
	 * @return LocalDate
	 */
	default LocalDate ld(final int idx, LocalDate default_value) {
		final var ret = ld(idx);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key 键名
	 * @return LocalTime
	 */
	default LocalTime lt(final String key) {
		final Object obj = this.get(key);
		if (obj == null)
			return null;
		if (obj instanceof LocalTime)
			return (LocalTime) obj;
		return Times.dt2lt(this.date(key));
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param key           键名
	 * @param default_value 默认值,当 值为null时候返回
	 * @return LocalTime
	 */
	default LocalTime lt(final String key, LocalTime default_value) {
		final var ret = lt(key);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx 索引需要从0开始
	 * @return LocalTime
	 */
	default LocalTime lt(final int idx) {
		final Object obj = this.get(idx);
		if (obj == null)
			return null;
		if (obj instanceof LocalTime)
			return (LocalTime) obj;
		return Times.dt2lt(this.date(idx));
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx           索引需要从0开始
	 * @param default_value 默认值,当 值为null时候返回
	 * @return LocalTime
	 */
	default LocalTime lt(final int idx, LocalTime default_value) {
		final var ret = lt(idx);
		return ret == null ? default_value : ret;
	}

	/**
	 * 把key列转换成时间值,时间值转换成long
	 * 
	 * @param key 键名
	 * @return long 时间值
	 */
	default Long lngdate(final String key) {
		final Date date = date(key);
		if (date == null)
			return null;
		return date.getTime();
	}

	/**
	 * 时间值<br>
	 * 把key列转换成时间值<br>
	 * 对于 数字类型的字符串，将给予解析成长整形后转换成日期<br>
	 * 
	 * @param key 键名
	 * @return Date 日期类型
	 */
	default Date date(final String key) {
		return map(key, o -> {
			Date date = null;
			if (o instanceof Date)
				return (Date) o;
			if (o instanceof LocalDateTime)
				return Times.ldt2dt((LocalDateTime) o);
			if (o instanceof LocalDate)
				return Times.ld2dt((LocalDate) o);
			if (o instanceof Timestamp)
				date = new Date(((Timestamp) o).getTime());
			else if (o instanceof Number || o instanceof LongNode) {
				final var lng = o instanceof LongNode ? ((LongNode) o).asLong() : (Long) (((Number) o).longValue());
				date = new Date(lng);
			} else if (o instanceof String || o instanceof TextNode) {
				var value = o instanceof TextNode ? ((TextNode) o).asText() : o.toString();
				if (value.matches("[\\.\\d]+")) {// 数字类型的字符串
					Number num = null;// 数字类型
					try {
						num = Double.parseDouble(value);// 尝试给予解析成数字
					} catch (Exception e) {
						// do nothing
					}
					if (num != null)
						return new Date(num.longValue());
				}

				// 需要注意顺序很重要"yyyy-MM-dd"，yyMMdd" 很重要,不能把yyMMdd放在首位
				if (value.endsWith("Z")) {// 解析带有时区的字符串．这个很丑陋，但是我也没有更好的办法,专门为mongo日期准备的解析
					final var s = "yyyy-MM-dd'T'HH:mm:ss.SSS Z";
					value = value.replace("Z", " UTC");
					try {
						date = new SimpleDateFormat(s).parse(value);// 时间解析
						return date;
					} catch (Exception xx) {
						// do nothing
					} // try
				} // if

				final var ss = new String[] { "yyMMdd", "yyyyMMdd", "yy-MM-dd", "yyyy-MM-dd", "yyyy-MM-dd HH",
						"yyyy/MM/dd HH", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyyMMdd HH:mm:ss",
						"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", };// 常用的时间格式
				final var dates = new LinkedList<Date>();
				for (String s : ss) {
					try {
						date = new SimpleDateFormat(s).parse(value);
					} catch (Exception ignored) {
					}
					if (date != null) {
						dates.add(date);
					} else {
						// do nothing
					} // if if(date!=null)
				} // for s:ss
				if (dates.size() > 0)
					date = dates.getLast();// 挑选出一个时间格式
			} // if

			return date;
		});// map
	};

	/**
	 * 时间值<br>
	 * 把key列转换成时间值<br>
	 * 对于 数字类型的字符串，将给予解析成长整形后转换成日期<br>
	 * 
	 * @param key           键名
	 * @param default_value 默认值
	 * @return Date 日期类型
	 */
	default Date date(final String key, final Date default_value) {
		final var dt = this.date(key);
		return dt == null ? default_value : dt;
	}

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param idx 索引序号从０开始
	 * @return 日期类型
	 */
	default Date date(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : date(key);
	};

	/**
	 * 把key列转换成逻是时间值<br>
	 * 对于 数字类型的字符串，将给予解析成长整形后转换成日期<br>
	 * 
	 * @param idx           索引序号从０开始
	 * @param default_value 默认值
	 * @return 日期类型
	 */
	default Date date(final int idx, final Date default_value) {
		final var dt = this.date(idx);
		return dt == null ? default_value : dt;
	};

	/**
	 * 把key列转换成逻辑值
	 * 
	 * @param key 键名
	 * @return 布尔类型
	 */
	default Boolean bool(final String key) {
		return map(key, o -> Boolean.parseBoolean(o + ""));
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
		final String key = this.keyOfIndex(idx);
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
	 * 把列转换成指定的 类型，这里只是强制转换，没有完成值结构的变换。
	 * 
	 * @param <T> 目标类型
	 * @param key 键/字段名称
	 * @param cls 指定类型
	 * @return T类型的返回值
	 */
	@SuppressWarnings("unchecked")
	default <T> T val(final String key, final Class<T> cls) {
		return (T) this.get(key);
	};// 获取接口值

	/**
	 * 把key列转换成逻是时间值
	 * 
	 * @param <T> 目标类型
	 * @param idx 从0开始
	 * @return T类型
	 */
	default <T> T val(final int idx, final Class<T> cls) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : val(key, cls);
	};

	/**
	 * 转换成一个json对象
	 * 
	 * @return 字符串形式的json对象
	 */
	default String json() {
		return Json.obj2json(this);
	}

	/**
	 * 整体转换<br>
	 * 把REC 转传成 tclass类型的对象,采用OBJINIT初始化对象<br>
	 * 注意与toTaget(部分转换)进行区分(toTarget一般用于提取元素的第一个键值元素。只有当第一个元素不满足要求，才尝试采用cast的方式进行整体转换。)
	 * 
	 * @param <T>    目标对象类型
	 * @param tclass 目标类型
	 * @return T 类型的对象
	 */
	default <T> T cast(final Class<T> tclass) {
		return cast(tclass, null);
	}

	/**
	 * 整体转换<br>
	 * 把REC 转传成 tclass类型的对象,采用OBJINIT初始化对象<br>
	 * 注意与toTaget(部分转换)进行区分(toTarget
	 * 一般用于提取元素的第一个键值元素。只有当第一个元素不满足要求，才尝试采用cast的方式进行整体转换。)
	 * 
	 * @param <T>    目标对象类型
	 * @param tclass 目标类型
	 * @param init   对象的初始值
	 * @return T 类型的对象
	 */
	default <T> T cast(final Class<T> tclass, final T init) {
		T t = null;

		if (tclass == null) {
			return null;
		}

		try {
			t = tclass.getDeclaredConstructor((Class<?>[]) null).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		final var _init = t != null ? t : init;

		return OBJINIT(_init, this.toMap());
	}

	/**
	 * 修改列名：根据具体实现类的不同 同名的key 可能会被覆盖。如果需要用重名的key请tags序列
	 * 
	 * @param keymapper 改名
	 * @return IRecord
	 */
	default IRecord keymap(final Function<String, String> keymapper) {
		return REC(toMap2(keymapper, identity));
	};

	/**
	 * 依据索引值对kvs 进行标记
	 * 
	 * @param key2tag 改名函数：把键名改成指定的标号
	 * @return SimpleRecord 的Record 可以存在多个同名的键名。即tag
	 */
	default IRecord tags(final Function<String, String> key2tag) {
		final var rec = new SimpleRecord();

		this.kvs().forEach(kv -> {
			final var key = key2tag.apply(kv.key());
			final var value = kv.value();
			rec.add(key, value);
		});

		return rec;
	};

	/**
	 * 依据索引值对kvs 进行标记
	 * 
	 * @param idx2tag 改名函数
	 * @return SimpleRecord
	 */
	default IRecord tagsi(final Function<Integer, String> idx2tag) {
		final var rec = new SimpleRecord();
		final var ai = new AtomicInteger(0);

		this.kvs().forEach(kv -> {
			final var key = idx2tag.apply(ai.getAndIncrement());
			rec.add(key, kv.value());
		});

		return rec;
	};

	/**
	 * 修改列名:keymap 的别名，调整映射位置 project
	 * 
	 * @param keyname_mapper 改名函数
	 * @return 修改了键名之后的IRecord
	 */
	default IRecord proj(final Function<String, String> keyname_mapper) {
		return keymap(keyname_mapper);
	};

	/**
	 * 分别对keys 和 values 进行变换。 toMap2 函数的别名。
	 * 
	 * @param <T>         key 的类型
	 * @param <U>         value 的类型
	 * @param keymapper   key 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @param valuemapper value 的变换函数:把原来的Object类型的Value,转换成U类型的值。
	 * @return {(t, u)} 的Map
	 */
	default <T, U> Map<T, U> applyOnKvs(final Function<String, T> keymapper, Function<Object, U> valuemapper) {
		return this.toMap2(keymapper, valuemapper);
	}

	/**
	 * 分别对keys 和 values 进行变换。 toMap2 函数的别名。
	 * 
	 * @param <T>         key 的类型
	 * @param <U>         value 的类型
	 * @param keymapper   key 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @param valuemapper value 的变换函数:把原来的Object类型的Value,转换成U类型的值。
	 * @param finisher    map-&gt;V
	 * @return {(t, u)} 的Map
	 */
	default <T, U, V> V applyOnKvs(final Function<String, T> keymapper, Function<Object, U> valuemapper,
			final Function<Map<T, U>, V> finisher) {
		return finisher.apply(this.applyOnKvs(keymapper, valuemapper));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> Map<T, Object> applyOnkeys(final Function<String, T> keymapper) {
		return toMap2(keymapper, identity);
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> Map<T, Object> applyOnkeys2(final Function<Integer, T> keymapper) {
		return toMap2(key -> keymapper.apply(this.indexOfKey(key)), identity);
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> Map<T, Object> aoks(final Function<String, T> keymapper) {
		return this.applyOnkeys(keymapper);
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> IRecord aoks2rec(final Function<String, T> keymapper) {
		return REC(this.applyOnkeys(keymapper));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 键名类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return IRecord
	 */
	default <T> Map<T, Object> aoks2(final Function<Integer, T> keymapper) {
		return this.applyOnkeys(k -> keymapper.apply(this.indexOfKey(k)));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 键名类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return IRecord
	 */
	default <T> IRecord aoks2rec2(final Function<Integer, T> keymapper) {
		return REC(this.applyOnkeys(k -> keymapper.apply(this.indexOfKey(k))));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T> key 键名类型
	 * @param kk  用kk替换键名 列表,当 kk 的长度 小于 当前 IRecord的键名列表的长度是时候 不予 进行替换 返回一个自身的克隆
	 * @return IRecord
	 */
	default <T> IRecord aoks2rec2(final String kk[]) {
		if (kk == null || kk.length < this.keys().size())
			return this.duplicate();
		return REC(this.applyOnkeys(k -> kk[this.indexOfKey(k) % kk.length]));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>  key 键名类型
	 * @param keys 用kk替换键名 列表,用 “,” “/” “\” “空白” 给予元素分隔
	 * @return IRecord
	 */
	default <T> IRecord aoks2rec2(final String keys) {
		final var kk = keys.split("[,/\\\\\\s]+");
		return aoks2rec2(kk);
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T> key 的类型
	 * @param kk  用kk替换键名
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	@SuppressWarnings("unchecked")
	default <T> Map<T, Object> aoks2(final T kk[]) {
		if (kk == null)
			return (Map<T, Object>) this.toMap();
		return this.aoks2(i -> i < kk.length ? kk[i] : null);// aoks2
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> IRecord keys(final Function<String, T> keymapper) {
		return REC(this.applyOnkeys(keymapper));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param kk 用kk替换键名数组,对于 i>=kk.length的时候,键名的时候 的返回键名 为null, 当kk 为null 的时候返回本身。
	 * @return 变换键名后的IRecord
	 */
	default IRecord keys(final String kk[]) {
		return REC(this.aoks2(kk));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param kk 用kk替换键名数组,对于 i>=kk.length的时候,键名的时候 的返回键名 为null, 当kk 为null 的时候返回本身。
	 * @return 变换键名后的IRecord
	 */
	default IRecord keys(final List<String> kk) {
		return REC(this.aoks2(kk.toArray(String[]::new)));
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param keys 用keys替换键名数组,采用"[\\|/,\\\\]+"对键名进行分隔获得键名数组kk,对于
	 *             i>=kk.length的时候,键名的时候 的返回键名 为null, 当kk 为null 的时候返回本身。
	 * @return 变换键名后的IRecord
	 */
	default IRecord keys(final String keys) {
		if (keys == null)
			return this;
		return REC(this.aoks2(keys.trim().split("[\\|/,\\\\]+")));
	}

	/**
	 * aoks2rec 的别名 <br>
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> IRecord alias(final Function<String, T> keymapper) {
		return REC(this.applyOnkeys(keymapper));
	}

	/**
	 * aoks2rec 的别名 <br>
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param <T>       key 键名类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return IRecord
	 */
	default <T> Map<T, Object> aliasi(final Function<Integer, T> keymapper) {
		return this.applyOnkeys(k -> keymapper.apply(this.indexOfKey(k)));
	}

	/**
	 * keys 的 别名 <br>
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param kk 用kk替换键名数组,对于 i>=kk.length的时候,键名的时候 的返回键名 为null, 当kk 为null 的时候返回本身。
	 * @return 变换键名后的IRecord
	 */
	default IRecord alias(final String kk[]) {
		return REC(this.aoks2(kk));
	}

	/**
	 * keys 的 别名 <br>
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param kk 用kk替换键名数组,对于 i>=kk.length的时候,键名的时候 的返回键名 为null, 当kk 为null 的时候返回本身。
	 * @return 变换键名后的IRecord
	 */
	default IRecord alias(final List<String> kk) {
		return keys(kk);
	}

	/**
	 * keys 的 别名 <br>
	 * 对键名集合进行Map,不对value 进行变换:applyOnkeys 的简写
	 * 
	 * @param keys 用keys替换键名数组,采用"[\\|/,\\\\]+"对键名进行分隔获得键名数组kk,对于
	 *             i>=kk.length的时候,键名的时候 的返回键名 为null, 当kk 为null 的时候返回本身。
	 * @return 变换键名后的IRecord
	 */
	default IRecord alias(final String keys) {
		return keys(keys);
	}

	/**
	 * 对值集合进行Map,不对key 进行变换<br>
	 * 把函数mapper 应用到 values 对象:Object->U 对象。然后使用目标结果呈递器finisher给呈递给最终结果。<br>
	 * 先转换成一个 String -> U 的Map lhm 然后把这个lhm 给转换成目标对象 O <br>
	 * 
	 * @param <T>         Value 的值类型
	 * @param <U>         valuemapper 对值的变换结果：中间结果
	 * @param <O>         目标结果呈递器的呈递的最终结果
	 * @param valuemapper 值变换函数
	 * @param finisher    目标结果呈递器O
	 * @return {(String,U)} 结构的Map
	 */
	@SuppressWarnings("unchecked")
	default <T, U, O> O applyOnValues(final Function<T, U> valuemapper,
			final Function<LinkedHashMap<String, U>, O> finisher) {
		return finisher.apply(toMap(t -> valuemapper.apply((T) t)));
	}

	/**
	 * 对值集合进行Map,不对key 进行变换 把函数mapper 应用到 values 对象:Object->U 对象。 转换成一个 String -> U
	 * 的Map
	 * 
	 * @param <T>         Value 的值类型
	 * @param <U>         valuemapper 对值的变换结果
	 * @param valuemapper 值变换函数
	 * @return {(String,U)} 结构的Map
	 */
	default <T, U> LinkedHashMap<String, U> applyOnValues(final Function<T, U> valuemapper) {
		return applyOnValues(valuemapper, e -> e);
	}

	/**
	 * applyOnValues:的简写 对值集合进行Map,不对key 进行变换 把函数mapper 应用到 values 对象:Object->U
	 * 对象。<br>
	 * 转换成一个 String -&gt; U 的Map
	 * 
	 * @param <T>         Value 的值类型
	 * @param <U>         valuemapper 对值的变换结果
	 * @param valuemapper 值变换函数
	 * @param finisher    目标结果呈递器O
	 * @return {(String,U)} 结构的Map
	 */
	default <T, U> LinkedHashMap<String, U> aovs(final Function<T, U> valuemapper,
			final LinkedHashMap<String, U> finisher) {
		return applyOnValues(valuemapper, e -> e);
	}

	/**
	 * applyOnValues:的简写 <br>
	 * 对值集合进行Map,不对key 进行变换<br>
	 * 把函数mapper 应用到 values 对象:Object-&gt;U 对象。<br>
	 * 转换成一个 String -&gt; U 的Map<br>
	 * 
	 * @param <T>         Value 的值类型
	 * @param <U>         valuemapper 对值的变换结果
	 * @param valuemapper 值变换函数
	 * @return {(String,U)} 结构的Map
	 */
	default <T, U> LinkedHashMap<String, U> aovs(final Function<T, U> valuemapper) {
		return applyOnValues(valuemapper, e -> e);
	}

	/**
	 * applyOnValue to Record:的简写 <br>
	 * 对值集合进行Map,不对key 进行变换<br>
	 * 把函数mapper 应用到 values 对象:Object->U 对象。然后使用目标结果呈递器finisher给呈递给最终结果。<br>
	 * 先转换成一个 String -> U 的Map lhm 然后把这个lhm 给转换成目标对象 O <br>
	 * 转换成一个 String -&gt; U 的Map<br>
	 * 
	 * @param <T>         Value 的值类型
	 * @param <U>         valuemapper 对值的变换结果
	 * @param valuemapper 值变换函数
	 * @return {(String,U)} 结构的Map
	 */
	default <T, U> IRecord aov2rec(final Function<T, U> valuemapper) {
		return applyOnValues(valuemapper, IRecord::REC);
	}

	/**
	 * 把函数mapper 应用到 键值列表进行变换，返回一个新的IRecourd对象。
	 * 
	 * @param <T>    mapper 的数据源类型
	 * @param <U>    mapper 的结果类型
	 * @param mapper 对键值对儿进行数值变换的结果。
	 * @return 与当前的键名相同的record 但是键值使用mapper进行变换了后的结果。
	 */
	@SuppressWarnings("unchecked")
	default <T, U> IRecord apply(final Function<KVPair<String, T>, U> mapper) {
		final List<KVPair<String, U>> kvs = LIST(
				this.kvs().stream().map(p -> KVPair.KVP(p._1(), mapper.apply((KVPair<String, T>) p))));
		return KVS2REC(kvs);
	}

	/**
	 * 把value 视作一个IRecord或是可以转换成record的数据，并给予转换成IRecord
	 * 
	 * @param key 需要进行分解的字段名：一般为json结构的列
	 * @return key 所对应对应的值得IRecord数据。
	 */
	default IRecord rec(final String key) {
		final Object value = this.get(key);// 提取值数据
		if (value == null) {
			return null;
		}
		if (value instanceof IRecord) {
			return (IRecord) value;
		}
		return new LinkedRecord(this.asMap(key));
	}

	/**
	 * 把value 视作一个IRecord或是可以转换成record的数据，并给予转换成IRecord
	 * 
	 * @param idx 键名索引从0开始
	 * @return idx 所对应对应的值得IRecord数据。
	 */
	default IRecord rec(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : rec(key);
	}

	/**
	 * json 格式的字段转换
	 * 
	 * @param key 需要进行分解的字段名：一般为json结构的列
	 * @return IRecord 的列表
	 */
	default List<IRecord> recs(final String key) {
		return this.lla(key, IRecord.class);
	}

	/**
	 * 把key列转换成IRecord的列表：[rec]
	 * 
	 * @param idx 索引列表 从0开始
	 * @return IRecord 的列表
	 */
	default List<IRecord> recs(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : recs(key);
	}

	/**
	 * 把key列转换成IRecord的列表 ：[rec] <br>
	 * recs(别名) <br>
	 * 
	 * @param key 列名
	 * @return IRecord 的流 [rec]
	 */
	default Stream<IRecord> recS(final String key) {
		final var ll = recs(key);
		return ll == null ? null : ll.stream();
	}

	/**
	 * 把index列转换成逻是recs的列表 ：[rec] <br>
	 * recs(别名);
	 * 
	 * @param idx 列索引 从0开始
	 * @return IRecord 的流 [rec]
	 */
	default Stream<IRecord> recS(final int idx) {
		return this.recS(this.keyOfIndex(idx));
	}

	/**
	 * 保留键名的真前缀扫描 <br>
	 * 对键值儿进行扫描:所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真后缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * <br>
	 * [[1], [1, 2], [1, 2, 3], [1, 2, 3, 4], [1, 2, 3, 4, 5]] <br>
	 * fx 就是作用 真后缀的集合的元素如[1],[4,5]的函数(不包括空集合),把每个元素后缀转换成目标类型T的对象 <br>
	 * 
	 * @param <T> fx 目标结果类型
	 * @param <X> fx 函数的源数据类型
	 * @param fx  真后缀的变换函数: [(k,x)]-&gt;t
	 * @return List&lt;T&gt; fx真前缀处理结果的集合
	 */
	@SuppressWarnings("unchecked")
	default <X, T> List<T> scanl(final Function<List<KVPair<String, X>>, T> fx) {
		return IRecord.scan(this.kvs().stream().map(p -> KVPair.KVP(p._1(), (X) p._2())), fx);
	}

	/**
	 * 对键值儿进行扫描:所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真后缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * <br>
	 * [[1], [1, 2], [1, 2, 3], [1, 2, 3, 4], [1, 2, 3, 4, 5]] <br>
	 * fx 就是作用 真后缀的集合的元素如[1],[4,5]的函数(不包括空集合),把每个元素后缀转换成目标类型T的对象 <br>
	 * 
	 * @param <T> fx 目标结果类型
	 * @param <X> fx 函数的源数据类型
	 * @param fx  真前缀的变换函数:xx-&gt;t
	 * @return List&lt;T&gt; fx真前缀处理结果的集合
	 */
	default <X, T> List<T> scanl2(final Function<List<X>, T> fx) {
		return scanl2(fx, e -> e);
	}

	/**
	 * 对键值儿进行扫描:所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 <br>
	 * [[1],[1,2],[1,2,3],[1,2,3,4]] 这样的真前缀的集合。 <br>
	 * 
	 * @param <T>      目标结果类型
	 * @param <X>      fx 函数的源数据类型
	 * @param <U>      目标结果：finisher 的返回类型
	 * @param fx       真前缀的变换函数:xx-&gt;t
	 * @param finisher 结果呈递函数:对fx作用后的真后缀集tt，进行再加工 递呈 最终结果 U: tt->u
	 * @return U finisher 呈递 的目标结果
	 */
	@SuppressWarnings("unchecked")
	default <X, T, U> U scanl2(final Function<List<X>, T> fx, final Function<List<T>, U> finisher) {
		var tt = IRecord.scan(this.kvs().stream().map(e -> (X) e._2()), fx);
		return finisher.apply(tt);
	}

	/**
	 * 保留键名的真后缀扫描 <br>
	 * 对键值儿进行扫描:所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真后缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * <br>
	 * [[5], [4, 5], [3, 4, 5], [2, 3, 4, 5], [1, 2, 3, 4, 5]] <br>
	 * fx 就是作用 真后缀的集合的元素如[1],[4,5]的函数(不包括空集合),把每个元素后缀转换成目标类型T的对象 <br>
	 * 
	 * @param <T> fx 目标结果类型
	 * @param <X> fx 函数的源数据类型
	 * @param fx  真后缀的变换函数: [(k,x)]-&gt;t
	 * @return List&lt;T&gt; fx真后缀处理结果的集合
	 */
	@SuppressWarnings("unchecked")
	default <X, T> List<T> scanr(final Function<List<KVPair<String, X>>, T> fx) {
		return IRecord.scan(this.kvs().stream().map(p -> KVPair.KVP(p._1(), (X) p._2())), fx, true);
	}

	/**
	 * 对键值儿进行扫描:所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真后缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * [[5], [4, 5], [3, 4, 5], [2, 3, 4, 5], [1, 2, 3, 4, 5]] fx 就是作用
	 * 真后缀的集合的元素如[1],[4,5]的函数(不包括空集合),把每个元素后缀转换成目标类型T的对象
	 * 
	 * @param <T> fx 目标结果类型
	 * @param <X> fx 函数的源数据类型
	 * @param fx  真后缀的变换函数: xx-&gt;t
	 * @return List&lt;T&gt; fx真后缀处理结果的集合
	 */
	default <X, T> List<T> scanr2(final Function<List<X>, T> fx) {
		return this.scanr2(fx, e -> e);
	}

	/**
	 * 对键值儿进行扫描:所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真后缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * [[5], [4, 5], [3, 4, 5], [2, 3, 4, 5], [1, 2, 3, 4, 5]] fx 就是作用
	 * 真后缀的集合的元素如[1],[4,5]的函数(不包括空集合),把每个元素后缀转换成目标类型T的对象
	 * 
	 * @param <T>      fx 目标结果类型
	 * @param <X>      fx 函数的源数据类型
	 * @param <U>      目标结果：finisher 的返回类型
	 * @param fx       真后缀的变换函数: xx-&gt;t
	 * @param finisher 结果呈递函数:对fx作用后的真后缀集tt，进行再加工 递呈 最终结果 U: tt->u
	 * @return U finisher呈递的目标结果
	 */
	default <X, T, U> U scanr2(final Function<List<X>, T> fx, final Function<List<T>, U> finisher) {
		final var kvs = LIST(this.kvs());
		Collections.reverse(kvs);
		@SuppressWarnings("unchecked")
		final var tt = IRecord.scan(kvs.stream().map(e -> (X) e._2()), fx, true);

		return finisher.apply(tt);
	}

	/**
	 * gets 的别名 <br>
	 * 使用bb 筛选键值对儿: 比如 提取水果的所有子集 <br>
	 * final var rec = STRING2REC("苹果,西瓜,草莓,哈密瓜,鸭梨");// 水果列表 <br>
	 * cph2(RPT(rec.size(),L(true,false))).map(e->rec.filter(e.bools())).map(e->MFT("{0}",e.values()))
	 * <br>
	 * .forEach(System.out::println); <br>
	 * 
	 * @param bb 下标选择器
	 * @return bb 所筛选出来的对应字段
	 */
	default IRecord filter(final Boolean[] bb) {
		return this.gets(bb);
	}

	/**
	 * 这是按照indexes 所指定的键值进行数据过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 
	 * @param indexes 提取的字段键名索引序列,非负整数从0开始
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filter(final Integer... indexes) {
		final var n = this.size();
		if (n <= 0) {
			return REC();
		}

		return this.filter(Arrays.stream(indexes).filter(Objects::nonNull).map(Math::abs).map(this::keyOfIndex)
				.toArray(String[]::new));
	}

	/**
	 * 这是按照flds 所指定的键名进行字段过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 
	 * @param flds 提取的字段集合用逗号分割,flds为null 表示不进行过滤。 注意分隔符号 之间不能留有空格
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filter(final String flds) {
		return this.filter(flds, true);
	}

	/**
	 * 这是按照kk 所指定的键名进行字段过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param kk 提取的字段的键值名称数据,kk 为null 表示不进行过滤。
	 * @return 一个LinkedRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filter(final String kk[]) {
		return filter(kk, true);
	}

	/**
	 * 这是按照kk 所指定的键名进行字段过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 当 discard_null_field 为false 的时候，会保证返回返回的IRecord记录中包含一个完整的kk序列字段结构。
	 * 
	 * @param kk                 提取的字段的键值名称数据,kk 为null 表示不进行过滤。
	 * @param discard_null_field 空值字段是否过滤。但相同的key只会保留一个,比如:filter(["id","id"]),只会返回一个{id:xxx}
	 * @return 一个LinkedRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filter(final String kk[], final boolean discard_null_field) {
		final var rec = REC();
		final String[] ss = kk == null ? this.tupleS().map(Tuple2::_1).toArray(String[]::new) : kk;
		for (final String s : ss) {
			final var v = this.get(s);
			if (discard_null_field && v == null)
				continue;// 过滤掉空值字段。
			rec.add(s, this.get(s));
		} // for

		return rec;
	}

	/**
	 * 这是按照pfilter 所指定的键值进行数据 肯定过滤。
	 * 
	 * @param pfilter 字段过滤检测器,null 不进行过滤。
	 * @return 一个LinkedRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filter(final Predicate<KVPair<String, Object>> pfilter) {
		final var rec = REC();
		Predicate<KVPair<String, Object>> _pfilter = pfilter == null ? e -> true : pfilter;
		this.tupleS().filter(_pfilter).forEach(s -> rec.add(s.key(), s.value()));
		return rec;
	}

	/**
	 * 这是按照flds 所指定的键名进行字段过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 
	 * @param flds               提取的字段集合用逗号分割,flds为null 表示不进行过滤。<br>
	 *                           当 discard_null_field 为false
	 *                           的时候，会保证返回返回的IRecord记录中包含一个完整的kk序列字段结构。<br>
	 * @param discard_null_field 空值字段是否过滤。
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filter(final String flds, final boolean discard_null_field) {
		String kk[] = flds == null ? null : flds.split("[,、，]+");
		return this.filter(kk, discard_null_field);
	}

	/**
	 * 这是按照flds 所指定的键值进行数据否定过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param flds 否定提取的字段集合用逗号分割,flds为null 表示不进行过滤。
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filterNot(final String flds) {
		if (flds == null)
			return this.duplicate();
		return this.filterNot(flds, true);
	}

	/**
	 * 这是按照flds 所指定的键名进行字否定过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param flds               否定提取的字段集合用逗号分割,flds为null 表示不进行过滤。<br>
	 *                           当 discard_null_field 为false
	 *                           的时候，会保证返回返回的IRecord记录中包含一个完整的kk序列字段结构。<br>
	 * @param discard_null_field 空值字段是否过滤。
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filterNot(final String flds, final boolean discard_null_field) {
		if (flds == null) {
			return this.duplicate();
		}
		final String kk[] = flds == null ? null : flds.split("[,、，]+");
		return this.filterNot(kk, discard_null_field);
	}

	/**
	 * 这是按照flds 所指定的键名进行字段否定过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 
	 * @param flds               否定提取的字段集合用逗号分割,flds为null 表示不进行过滤。 当
	 *                           discard_null_field 为false
	 *                           的时候，会保证返回返回的IRecord记录中包含一个完整的kk序列字段结构。
	 * @param discard_null_field 空值字段是否过滤。
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filterNot(final String flds[], final boolean discard_null_field) {
		if (flds == null) {
			return this.duplicate();
		}
		final var aa = Arrays.asList(flds);
		return this.filterNot(e -> aa.contains(e._1())).filter(kvp -> !discard_null_field || kvp._2() != null);
	}

	/**
	 * 这是按照flds 所指定的键名进行字段否定过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 
	 * @param flds 提取的字段集合用逗号分割,flds为null 表示不进行过滤。 当 discard_null_field 为false
	 *             的时候，会保证返回返回的IRecord记录中包含一个完整的kk序列字段结构。 discard_null_field
	 *             空值字段是否过滤。默认为true
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filterNot(final String flds[]) {
		return filterNot(flds, true);
	}

	/**
	 * 这是按照indices 所指定的键名进行字段否定过滤。默认过滤空值字段（该字段的值value为null)<br>
	 * 
	 * @param indices 键名索引号列表,索引号从0开始 当 discard_null_field 为false的时候，
	 *                会保证返回返回的IRecord记录中包含一个完整的kk序列字段结构。 discard_null_field
	 *                空值字段是否过滤。默认为true
	 * @return 一个SimpleRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filterNot(final Integer... indices) {
		final var n = this.size();
		final var flds = Arrays.stream(indices).map(i -> this.keyOfIndex(i % n)).toArray(String[]::new);
		return filterNot(flds, true);
	}

	/**
	 * 这是按照pfilter 所指定的键值进行数据 否定过滤。<br>
	 * 
	 * @param pfilter 字段否定过滤检测器,null 不进行过滤。true:去除掉,false:给保留
	 * @return 一个LinkedRecord 以保证空值字段也可以保持顺序。
	 */
	default IRecord filterNot(final Predicate<KVPair<String, Object>> pfilter) {
		return this.filter(t -> !pfilter.test(t));
	}

	/**
	 * 视IRecord 为一个Key-Value的数据流，把他们展开成一个U类型的数据流。
	 * 
	 * @param <T>    本IRecord的元素值类型
	 * @param <U>    目标结果的元素类型
	 * @param mapper 二元函数,把kvp展开成一个U类型的数据流
	 * @return U类型数据流
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> flatMap(BiFunction<String, T, Stream<U>> mapper) {
		return this.tupleS().flatMap(kvp -> mapper.apply(kvp._1(), (T) kvp._2()));
	}

	/**
	 * 视IRecord 为一个Key-Value的数据流，把他们展开成一个U类型的数据流。
	 * 
	 * @param <T>    本IRecord的元素值类型
	 * @param <U>    目标结果的元素类型
	 * @param mapper 一元函数,把kvp展开成一个U类型的数据流
	 * @return U类型数据流
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> flatMap(Function<T, Stream<U>> mapper) {
		return this.tupleS().flatMap(kvp -> mapper.apply((T) kvp._2()));
	}

	/**
	 * 使用一个变换集合 mappers 分别变换 IRecord
	 * 
	 * @param mappers 变换集合
	 * @return IRecord流
	 */
	@SuppressWarnings("unchecked")
	default Stream<IRecord> flatMap(final Function<IRecord, IRecord>... mappers) {
		return this.flatMap(Arrays.asList(mappers));
	}

	/**
	 * 使用一个变换集合 mappers 分别变换 IRecord
	 * 
	 * @param map_iterable 变换集合
	 * @return IRecord流
	 */
	default Stream<IRecord> flatMap(final Iterable<Function<IRecord, IRecord>> map_iterable) {
		return StreamSupport.stream(map_iterable.spliterator(), false).map(e -> e.apply(this));
	}

	/**
	 * 使用一个变换集合 mappers 分别变换 IRecord
	 * 
	 * @param <U>          结果类型
	 * @param map_iterable 变换集合 [rec->rec,rec->rec,...]
	 * @param ret_mapper   结果变换集合 [rec]->u
	 * @return IRecord流
	 */
	default <U> U flatMap(final Iterable<Function<IRecord, IRecord>> map_iterable,
			final Function<Iterable<IRecord>, U> ret_mapper) {
		final var list = this.flatMap(map_iterable).collect(Collectors.toList());
		return ret_mapper.apply(list);
	}

	/**
	 * mutate 强调 高级结构变换, toTarget 强调底层数值变换，即muate是结构,toTarget是数据,对于简单类型二者有重合，可以互换
	 * <br>
	 * 
	 * 汽车人变形，看我七十二变！<br>
	 * 把 当前IRecord 视为一个 targetClass 对象的 的属性的 候选值 集合.类似于 把 IRecord 作为一个 Spring
	 * 的Context <br>
	 * 用过 按名（按照IRecord的键) 去注入到targetClass 的对象实例 之中。 <br>
	 * 看我72变化：Record的变化比72般变化还要多，哈哈 <br>
	 * IRecord的变身：就像孙悟空一样变成 林林种种的 其他物件。<br>
	 * 孙悟空.mutate(白骨精）：就是孙悟空变成一个白骨精。 <br>
	 * targetClass 需要擁有一個 默認的 構造函數<br>
	 * 
	 * @param <T>     mutator 所要变成的目标对象的类型
	 * @param mutator 变换函数:变身逻辑
	 * @return mutator 变换后的目标对象。
	 */
	default <T> T mutate(final Function<IRecord, T> mutator) {
		return mutator.apply(this);
	}

	/**
	 * mutate 强调 高级结构变换, toTarget 强调底层数值变换，即muate是结构,toTarget是数据,对于简单类型二者有重合，可以互换
	 * <br>
	 * 
	 * 汽车人变形，看我七十二变！<br>
	 * 把 当前IRecord 视为一个 targetClass 对象的 的属性的 候选值 集合.类似于 把 IRecord 作为一个 Spring
	 * 的Context <br>
	 * 用过 按名（按照IRecord的键) 去注入(@Resource)到targetClass 的对象实例 之中。 <br>
	 * 字段变身:不带有类型变换功能，对于属性字段只是强制类型转换，转换不成功则设置为null <br>
	 * targetClass 需要擁有一個 默認的 構造函數( 对于 Primitive mutate 则依靠 toTarget来完成,<br>
	 * mutate强调采用targetClass构造默认函数生成空实例object然后用IRecord(this)去实例化object的各个属性) <br>
	 * 
	 * @param <T>         目标结果类型
	 * @param targetClass 目标类对象的类<br>
	 *                    <br>
	 *                    param enable 是否开启对 字段属性的 的默认构造, 即
	 *                    对于没有出现在在当前record键名列表中的targetClass字段是否给予设置。<br>
	 *                    true: 表示 对于 没有在 当前keys列表中出现的 属性的 调用 其字段类型的 默认构造函数 给予 初始化。
	 *                    <br>
	 *                    false: 表示给予丢弃。 <br>
	 *                    mutate的enable默认为false即不予设置默认属性对象
	 * @return 目标对象T类型
	 */
	default <T> T mutate(final Class<T> targetClass) {
		return this.mutate(targetClass, false);
	}

	/**
	 * mutate 强调 高级结构变换, toTarget 强调底层数值变换，即muate是结构,toTarget是数据,对于简单类型二者有重合，可以互换
	 * <br>
	 * 
	 * 汽车人变形，看我七十二变！<br>
	 * 把 当前IRecord 视为一个 targetClass 对象的 的属性的 候选值 集合.类似于 把 IRecord 作为一个 Spring
	 * 的Context <br>
	 * 用过 按名（按照IRecord的键) 去注入(@Resouce)到targetClass 的对象实例 之中。 <br>
	 * 字段变身:不带有类型变换功能，对于属性字段只是强制类型转换，转换不成功则设置为null <br>
	 * targetClass 需要擁有一個 默認的 構造函數( 对于 Primitive mutate 则依靠 toTarget来完成,<br>
	 * mutate强调采用targetClass构造默认函数生成空实例object然后用IRecord(this)去实例化object的各个属性) <br>
	 * 
	 * @param <T>         目标结果类型
	 * @param targetClass 目标类对象的类
	 * @param enable      说是否开启对 字段属性的 的默认构造, 即
	 *                    对于没有出现在在当前record键名列表中的targetClass字段是否给予设置。<br>
	 *                    true: 表示 对于 没有在 当前keys列表中出现的 属性的 调用 其字段类型的 默认构造函数 给予 初始化。
	 *                    <br>
	 *                    false: 表示给予丢弃。 <br>
	 * @return 目标对象T类型
	 */
	@SuppressWarnings("unchecked")
	default <T> T mutate(final Class<T> targetClass, final boolean enable) {
		T bean = null; // 返回值

		if (targetClass == null)
			return bean;// 目标targetClass对象

		if (targetClass.isAssignableFrom(this.getClass()))
			return (T) this;// 如果 是cls的子类，则直接强制转换并返回。

		if (targetClass.isPrimitive() // 基础类型委托给 toTarget
				|| targetClass.isAssignableFrom(Number.class) || targetClass.isAssignableFrom(Short.class)
				|| targetClass.isAssignableFrom(Integer.class) || targetClass.isAssignableFrom(Long.class)
				|| targetClass.isAssignableFrom(Float.class) || targetClass.isAssignableFrom(Double.class)
				|| targetClass.isAssignableFrom(Character.class) || targetClass.isAssignableFrom(Boolean.class)
				|| targetClass.isAssignableFrom(Void.class) // 这个类型我没有做特殊处理,只是写在这里而已
				|| targetClass.isAssignableFrom(Byte.class) || targetClass.isAssignableFrom(String.class))
			return this.toTarget(targetClass);

		try {
			final var ctor = targetClass.getDeclaredConstructor();// 获取构造函数
			ctor.setAccessible(true);// 设置为可访问
			final T target = ctor.newInstance(); // 创建载体对象（承载Record的各个属性的值)

			// 遍历targetClass的各个字段
			Stream.of(targetClass.getDeclaredFields())
					// 对字段进行分组选取每个分组的第一个字段作为属性名(对于SimpleRecord 是可以存在同名的field的),这也是分组的原因。
					.collect(Collectors.groupingBy(Field::getName, // 键为字段名称：
							Collector.of(HashSet<Field>::new, HashSet::add, (l, r) -> {
								l.addAll(r);
								return l;
							}, f -> f.iterator().next())) // 敛集字段信息
					).forEach((name, fld) -> { // 字段遍历
						if (!enable && !this.has(name))
							return;// 如果没有开启默认设置则 对于 没有 候选值 的属性 给予放弃设置。
						fld.setAccessible(true);/* 使得字段变得可编辑 */
						final var targetType = fld.getType(); // 获取字段类型
						final var fldval = this.get(name); // 获取字段值
						final Object value = (fldval != null && (fldval instanceof IRecord))
								? ((IRecord) fldval).mutate(targetType, enable) // rec 类型的数据给予递归处理
								: this.getT(name, targetType);// 提取指定類型的字段
						if (value != null) {
							try {
								fld.set(target, value); // 設置字段值
							} catch (Exception ex) {
								ex.printStackTrace();
							} /* 根据键值名设置字段值,从record拷贝到Class */
						} // value != null
					});/* foreach */

			// 实现结果组装
			bean = target;// 成功完成对象转换
		} catch (Exception e) {
			e.printStackTrace();
		}
		;

		return bean;

	}// mutate

	/**
	 * 字段变身 带有简单的类型转换比如时间转换。字符串类型转换成时间对象 <br>
	 * 既然mutate2比mutate 更为智能为何保留mutate呢，原因在指定targetClass的具体类型的时候，mutate比较快。 <br>
	 * 另外對字段類型當目標類型與字段類型不匹配的時候mutate會採用getT來進行字段類型轉換。<br>
	 * targetClass 需要擁有一個 默認的 構造函數 <br>
	 * 
	 * @param <T>         目标类类型。
	 * @param targetClass 目标类对象的类
	 * @return T类型的对象
	 */
	default <T> T mutate2(final Class<T> targetClass) {
		return OBJINIT(targetClass, this);
	}

	/**
	 * 字段浅遍历
	 * 
	 * @param <V> 值类型
	 * @param cs  回调函数：Consumer:(k,v)->{}
	 */
	@SuppressWarnings("unchecked")
	default <V> void foreach(final BiConsumer<String, V> cs) {
		this.tupleS().forEach(kv -> cs.accept(kv.key(), (V) kv.value()));
	}

	/**
	 * iterator
	 * 
	 * @return [kvp] iterator
	 */
	default Iterator<KVPair<String, Object>> iterator() {
		return this.tupleS().iterator();
	}

	/**
	 * 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param cs 路径结构回调函数 (路径path,path所对应的数据流)->{}, path 以 "/"开头
	 */
	default void dfs(final BiConsumer<String, Stream<Object>> cs) {
		dfs(this, cs, null, null);
	}

	/**
	 * 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <T> 元素类型
	 * @param cs  路径结构回调函数 (路径path,path所对应的值列表)->{}, path 以 "/"开头
	 */
	@SuppressWarnings("unchecked")
	default <T> void dfs_forall(final BiConsumer<String, List<T>> cs) {
		dfs(this, (path, stream) -> cs.accept(path, (List<T>) stream.toList()), null, null);
	}

	/**
	 * 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <T> 元素类型
	 * @param cs  路径结构回调函数 (路径path,path所对应的值列表)->{}, path 以 "/"开头
	 */
	@SuppressWarnings("unchecked")
	default <T> void dfs_forall2(final Consumer<Tuple2<String, List<T>>> cs) {
		this.dfs_forall((p, v) -> cs.accept(Tuple2.TUP2(p, (List<T>) v)));
	}

	/**
	 * 遍历IRecord 深度遍历(Deep First Search),并把遍历的结果 采用 归集器 clc 进行归集 到一个中间结果IRecord <br>
	 * 最后把IRecord 给映射成U类型的结果
	 * 
	 * @param <T>    clc的元素类型
	 * @param <O>    clc 的归集结果类型
	 * @param <U>    结果类型
	 * @param clc    每个 path 对应的 T类型数据序列的归集器
	 * @param mapper IRecord的最终结果映射器。 类型为 {path:String,value:O} ->U
	 * @return U类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <T, O, U> U dfs_clc(final Collector<T, ?, O> clc, final Function<IRecord, U> mapper) {
		final var rec = REC();
		final BiConsumer<String, O> callback = rec::add;// dfs_forall 的回调函数
		dfs(this, (path, stream) -> callback.accept(path, ((Stream<T>) stream).collect(clc)), null, null);
		return mapper.apply(rec);
	}

	/**
	 * 遍历IRecord 深度遍历(Deep First Search),并把遍历的结果 采用 归集器 clc 进行归集 到一个中间结果IRecord <br>
	 * 
	 * @param <T> clc的元素类型
	 * @param <O> clc 的归集结果类型
	 * @param clc 每个 path 对应的 T类型数据序列的归集器
	 * 
	 * @return IRecourd {path:String,value:O}
	 */
	default <T, O> IRecord dfs_clc(final Collector<T, ?, O> clc) {
		return dfs_clc(clc, e -> e);
	}

	/**
	 * 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <T> 元素类型
	 * @param cs  路径结构回调函数 (路径path,path所对应的数据的单值若为多值则取第一个元素)->{} , path 以 "/"开头
	 */
	@SuppressWarnings("unchecked")
	default <T> void dfs_forone(final BiConsumer<String, T> cs) {
		dfs(this, (path, stream) -> {
			// 之所以采用 Optional.ofNullable 为为了保证 stream 可以容纳 null 值
			final var opt = stream.map(Optional::ofNullable).findFirst().orElse(Optional.empty());
			cs.accept(path, (T) (opt.isPresent() ? opt.get() : null));
		}, null, null);
	}

	/**
	 * 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <T> 元素类型
	 * @param cs  路径结构回调函数 (路径path,path所对应的数据的单值若为多值则取第一个元素)->{} , path 以 "/"开头
	 */
	@SuppressWarnings("unchecked")
	default <T> void dfs_forone2(final Consumer<Tuple2<String, T>> cs) {
		this.dfs_forone((p, v) -> cs.accept(Tuple2.TUP2(p, (T) v)));
	}

	/**
	 * 单值式深度优先遍历计算 <br>
	 * 遍历IRecord 深度遍历: Deep First Search <br>
	 * <br>
	 * 使用示例:<br>
	 * final var userinfo =
	 * rec.splitS("name,sex,birth","province,city,district,street").collect(IRecord.rbclc("profile,address"));
	 * <br>
	 * userinfo.dfs_eval_forone((path,value)->{ <br>
	 * println(path,value); <br>
	 * return value; <br>
	 * }); <br>
	 *
	 * @param <U>       结果的参数类型
	 * @param <T>       元素的参数类型
	 * @param evaluator 路径结构计算函数 (路径path,path所对应的数据的单值若为多值则取第一个元素)->U , path 以 "/"开头
	 * 
	 * @return U 的列表
	 */
	@SuppressWarnings("unchecked")
	default <T, U> List<U> dfs_eval_forone(final BiFunction<String, T, U> evaluator) {
		final var uu = new LinkedList<U>();
		dfs(this, (path, stream) -> {
			var opt = stream.findFirst();
			var u = evaluator.apply(path, (T) (opt.isPresent() ? opt.get() : null));
			uu.add(u);
		}, null, null);
		return uu;
	}

	/**
	 * 单值式深度优先遍历计算 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <U>       结果的参数类型
	 * @param <T>       元素的参数类型
	 * @param evaluator 路径结构计算函数 (路径path,path所对应的数据的单值若为多值则取第一个元素)->U , path 以 "/"开头
	 * 
	 * @return U 的流
	 */
	default <T, U> Stream<U> dfs_eval_forone2(final BiFunction<String, T, U> evaluator) {
		return this.dfs_eval_forone(evaluator).stream();
	}

	/**
	 * 多值式深度优先遍历计算 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <U>       结果的参数类型
	 * @param <T>       元素的参数类型
	 * @param evaluator 路径结构计算函数 (路径path,path所对应的数据流)->U , path 以 "/"开头
	 * 
	 * @return U的列表
	 */
	@SuppressWarnings("unchecked")
	default <T, U> List<U> dfs_eval_forall(final BiFunction<String, Stream<T>, U> evaluator) {
		final var uu = new LinkedList<U>();
		dfs(this, (path, stream) -> {
			final var u = evaluator.apply(path, (Stream<T>) stream);
			uu.add(u);
		}, null, null);
		return uu;
	}

	/**
	 * 多值式深度优先遍历计算 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <U>       结果的参数类型
	 * @param <T>       元素的参数类型
	 * @param evaluator 路径结构计算函数 (路径path,path所对应的数据流)->U , path 以 "/"开头
	 * @return U 的流程
	 */
	default <T, U> Stream<U> dfs_eval_forall2(final BiFunction<String, Stream<T>, U> evaluator) {
		return this.dfs_eval_forall(evaluator).stream();
	}

	/**
	 * 结果生成：一个Key-Value的列表。 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <U>    值类型:Record的值的类型，但是这个类型被隐藏了
	 * @param mapper 值函数 key,key 所对应的值的集合的第一个元素。
	 * 
	 * @return IRecord
	 */
	default <U> IRecord dfs2rec(final BiFunction<String, Stream<Object>, U> mapper) {
		final var rec = REC();
		dfs(this, (path, stream) -> rec.add(path, mapper.apply(path, stream)), null, null);
		return rec;
	}

	/**
	 * 结果生成：一个Key-Value的列表。 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param <U>    值类型
	 * @param mapper 值函数 key,提取 key 所对应的值的集合的第一个元素 进行变换后的结果
	 * @return mapper 值函数 key,提取 key 所对应的值的集合的第一个元素 进行变换后的结果
	 */
	default <U> Map<String, U> dfs2kvs(final BiFunction<String, Stream<Object>, U> mapper) {
		final var kvs = new LinkedHashMap<String, U>();
		dfs(this, (path, stream) -> kvs.put(path, mapper.apply(path, stream)), null, null);
		return kvs;
	}

	/**
	 * 遍历IRecord 深度遍历: Deep First Search
	 * 
	 * @param cs 路径结构计算函数 (路径path,path所对应的数据的单值若为多值则取第一个元素)->U , path 以 "/"开头
	 */
	default void dfs2(final BiConsumer<String, Object> cs) {
		dfs(this, (k, v) -> cs.accept(k, v.findFirst().get()), null, null);
	}

	/**
	 * 类似于数据的库的表连接,通过 key 把 自己与另外一个 rec 进行连接 <br>
	 * a = REC("1","A","2","B","4","D"); <br>
	 * b = a.join(REC("1","a","2","b","3","c")) <br>
	 * 则b 就是： 1:A --> a 2:B --> b 4:D --> null 3:null --> c
	 * 
	 * @param rec 另外的一个连接对象
	 * @return {key-&gt;TUP2&lt;Object,Object&gt;}的 Map
	 */
	default IRecord join(final IRecord rec) {
		return REC(join(this, rec));
	}

	/**
	 * 连接成列表 <br>
	 * 类似于数据的库的表连接,通过 key 把 自己与另外一个 rec 进行连接 <br>
	 * a = REC("1","A","2","B","4","D"); <br>
	 * b = a.join(REC("1","a","2","b","3","c")) <br>
	 * 则b 就是： 1:A --> a 2:B --> b 4:D --> null 3:null --> c <br>
	 * 
	 * var x = porec.join2ll(sorec,"path,po,so".split(",")); <br>
	 * 类似于这样的集合：<br>
	 * path po so <br>
	 * /煤焦油/计提损耗 37180.8 (null) <br>
	 * /煤焦油/徐州市龙山制焦有限公司 1.60138826E7 (null) <br>
	 * 
	 * @param rec  另外的一个连接对象
	 * @param keys 新生记录的键值名称
	 * @return {key-&gt;TUP2&lt;Object,Object&gt;}的 Map
	 */
	default List<IRecord> join2rr(IRecord rec, String[] keys) {
		final var rr = new LinkedList<IRecord>();
		join(this, rec).forEach((k, tup) -> rr.add(REC(keys[0], k, keys[1], tup._1, keys[2], tup._2)));
		return rr;
	}

	/**
	 * 连接成列表 <br>
	 * 类似于数据的库的表连接,通过 key 把 自己与另外一个 rec 进行连接 <br>
	 * a = REC("1","A","2","B","4","D"); <br>
	 * b = a.join(REC("1","a","2","b","3","c")) <br>
	 * 则b 就是： 1:A --> a 2:B --> b 4:D --> null 3:null --> c <br>
	 * 
	 * var x = porec.join2ll(sorec,"path,po,so".split(",")); <br>
	 * 类似于这样的集合： <br>
	 * path po so <br>
	 * /煤焦油/计提损耗 37180.8 (null) <br>
	 * /煤焦油/徐州市龙山制焦有限公司 1.60138826E7 (null) <br>
	 * 
	 * @param rec  另外的一个连接对象
	 * @param keys 新生记录的键值名称 ,用逗号分割
	 * @return {key-&gt;TUP2&lt;Object,Object&gt;}的 Map
	 */
	default List<IRecord> join2rr(final IRecord rec, final String keys) {
		return join2rr(rec, (keys == null ? "key,v1,v2" : keys).split("[,]+"));
	}

	/**
	 * 连接成列表 <br>
	 * 类似于数据的库的表连接,通过 key 把 自己与另外一个 rec 进行连接 <br>
	 * a = REC("1","A","2","B","4","D"); <br>
	 * b = a.join(REC("1","a","2","b","3","c")) <br>
	 * 则b 就是： 1:A --> a 2:B --> b 4:D --> null 3:null --> c <br>
	 * 
	 * var x = porec.join2ll(sorec,"path,po,so".split(",")); <br>
	 * 类似于这样的集合：<br>
	 * path po so /煤焦油/计提损耗 37180.8 (null) <br>
	 * /煤焦油/徐州市龙山制焦有限公司 1.60138826E7 (null) <br>
	 * 
	 * @param rec 另外的一个连接对象 keys 新生记录的键值名称:默认为[key,v1,v2]
	 * @return {key-&gt;TUP2&lt;Object,Object&gt;}的 Map
	 */
	default List<IRecord> join2rr(final IRecord rec) {
		return join2rr(rec, (String) null);
	}

	/**
	 * 对于IRecord 进行排序
	 * 
	 * @param comparator 排序比较器
	 * @return 新的IRecord 排序后
	 */
	default IRecord sorted(final Comparator<? super KVPair<String, Object>> comparator) {
		final var rec = REC();
		this.tupleS().sorted(comparator).forEach(kv -> rec.add(kv._1(), kv._2()));
		return rec;
	}

	/**
	 * 对于IRecord 的字段进行倒排
	 * 
	 * @return 新的IRecord 倒排后
	 */
	default IRecord reverse() {
		return this.foldRight(REC(), (kv, rec) -> rec.add(kv._1(), kv._2()));
	}

	/**
	 * 二维矩阵<br>
	 * 这个方法一般用于生成一个 列向量矩阵：比如REC("variable",new String[]{"a","b","c"}).toArray2();
	 * 生成二维数组：视每个元素为单独二维数组的单独的列。
	 * 
	 * @return String[][] String类型的二维数组
	 */
	default String[][] strmx() {
		return this.toArray2(String.class);
	}

	/**
	 * 二维矩阵<br>
	 * 这个方法一般用于生成一个 列向量矩阵：比如REC("variable",new Integer[]{1,2,3}).toArray2();
	 * 生成二维数组：视每个元素为单独二维数组的单独的列。
	 * 
	 * @return Integer[][] Integer类型的二维数组
	 */
	default Integer[][] intmx() {
		return this.toArray2(Integer.class);
	}

	/**
	 * 二维矩阵<br>
	 * 这个方法一般用于生成一个 列向量矩阵：比如REC("variable",new Double[]{1d,2d,3d}).toArray2();
	 * 生成二维数组：视每个元素为单独二维数组的单独的列。
	 * 
	 * @return Double[][] Double类型的二维数组
	 */
	default Double[][] dblmx() {
		return this.toArray2(Double.class);
	}

	/////////////////////////////////////////////////////////////////////
	// 以下是IRecord DFrame 类型的方法区域:所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)
	/////////////////////////////////////////////////////////////////////

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
	 * 术语来源 pandas<br>
	 * 返回矩阵形状<br>
	 * 
	 * @return (height,width)的二维矩阵
	 */
	default Tuple2<Integer, Integer> shape() {
		final var width = this.keys().size();
		final var height = this.tupleS().map(kvp -> {
			final var vv = this.lla(kvp._1(), e -> e);
			if (vv == null)
				return 0;
			else
				return vv.size();
		}).collect(Collectors.summarizingInt(e -> e)).getMax();

		return new Tuple2<>(Math.max(height, 0), width);
	}

	/**
	 * 第rowid 所在的行记录
	 * 
	 * @param rowid 行号索引：从0开始
	 * @return rowid所标记行记录
	 */
	default IRecord row(final int rowid) {
		final var rows = this.rows((name, e) -> e, this.keys());
		if (rows == null || rows.size() < 1 || rows.size() <= rowid)
			return null;
		return rows.get(rowid);
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * @return 返回以key值为列名的行 的流
	 */
	default Stream<IRecord> rowS() {
		return this.rows().stream();
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * @param <U>    结果类型
	 * @param mapper 变换函数 rec-&gt;u
	 * @return 行变换的的结果u类型的流
	 */
	default <U> Stream<U> rowS(final Function<IRecord, U> mapper) {
		return this.rowS().map(mapper);
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * @param <T>    转换mapper的 结果类型
	 * @param <V>    源数据的值类型 Value
	 * @param mapper 元素类型格式化函数,类型为， (key:String,value:Object)-&gt;new_value
	 *               hh列名序列,默认为keys
	 * @return 返回以key值为列名的行列表
	 */
	default <T, V> List<IRecord> rows(final BiFunction<String, V, T> mapper) {
		return rows(mapper, this.keys());
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * @param mapper 元素类型格式化函数,类型为， (key:String,value:Object)-&gt;new_value
	 * @param hh     列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * @return 返回以hh值为列名的行列表
	 */
	@SuppressWarnings("unchecked")
	default <T, V> List<IRecord> rows(final BiFunction<String, V, T> mapper, final List<String> hh) {
		final var shape = this.shape();// 提取图形结构
		final var rows = new ArrayList<IRecord>(shape._1());// 提取行数记录行数
		final List<String> final_hh = hh == null
				? LIST(Stream.iterate(0, i -> i + 1).limit(shape._2()).map(Jdbcs::excelname))// 生成excel列名
				: hh;
		if (hh != null) {
			Stream.iterate(hh.size(), i -> i < shape._2(), i -> i + 1).forEach(i -> final_hh.add(excelname(i)));
		}
		final var keys = this.keys().toArray(String[]::new);
		for (int j = 0; j < shape._2(); j++) {// 列号
			final var col = this.lla(keys[j], e -> e);// 提取name列
			if (col == null) {
				continue;
			}
			final var size = col.size();// 列大小
			for (int i = 0; i < shape._1(); i++) { // 列名索引
				if (rows.size() <= i) {
					rows.add(REC());
				}
				final var row = rows.get(i);// 提取i 行的数据记录。
				final var key = final_hh.get(j);
				row.add(key, mapper.apply(keys[j], (V) col.get(i % size)));
			} // for i
		} // keys

		return rows;
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
	 * 行化操作：数据分析类 需要与DataMatrix 相结合生成 data.frame类型的 转换函数<br>
	 * 
	 * row:Map 行记录 <br>
	 * L:结果类型为List <br>
	 * 主要用途就是 完成 IRecord 向 DataMatrix的转换，但是为了保证DataMatrix 与IRecord 的独立。而设置这个函数。比如
	 * <br>
	 * var dm = new DataMatrix&lt;&gt; (rec.rowS(),Integer.class); 就构造了一个 DataMatrix
	 * 对象。<br>
	 * 
	 * @param <T>   值类型
	 * @param clazz 值类型class
	 * @return 生成一个hashmap 的集合
	 */
	@SuppressWarnings("unchecked")
	default <T> List<Map<String, T>> lines(final Class<T> clazz) {
		return (List<Map<String, T>>) (Object) Collections.singletonList(this.toMap());
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
	 * 行化操作：数据分析类 需要与DataMatrix 相结合生成 data.frame类型的 转换函数<br>
	 * 
	 * row:Map 行记录<br>
	 * L:结果类型为List<br>
	 * 主要用途就是 完成 IRecord 向 DataMatrix的转换，但是为了保证DataMatrix 与IRecord 的独立。而设置这个函数。比如
	 * <br>
	 * var dm = new DataMatrix&lt;&gt; (rec.rowL(),Integer.class); 就构造了一个 DataMatrix
	 * 对象。<br>
	 * 
	 * @return 生成一个hashmap的集合<br>
	 */
	default List<Map<String, Object>> lines() {
		return Collections.singletonList(this.toMap());
	}

	/**
	 * 返回idx 列索引位置的列数据:列表
	 * 
	 * @param <T> 列的元数据类型
	 * @param <U> 返回值变换后的列的类型
	 * @param idx 列名索引，从0开始
	 * @param t2u 列值转换函数:t-&gt;u
	 * @return idx 所标识的列(key)的经过t2u变换后的元素集合
	 */
	default <T, U> List<U> column(final Integer idx, final Function<T, U> t2u) {
		return this.lla(idx, t2u);
	}

	/**
	 * 
	 * 返回idx 位置的列元素集合：：强制转换为targetClass 的类型
	 * 
	 * 类型采用强制转换，因此可能会出现不同列之间的类型不一致的风险，使用时候需要注意。这一部分需要在编程中给注意与防范。<br>
	 * 类库设置不予考虑。<br>
	 * 
	 * @param <T>         列的元数据类型
	 * @param idx         列名索引，从0开始
	 * @param targetClass 列的值类型类
	 */
	@SuppressWarnings("unchecked")
	default <T> List<T> column(final Integer idx, final Class<T> targetClass) {
		return this.lla(idx, e -> (T) e);
	}

	/**
	 * 返回idx 位置的列元素集合
	 * 
	 * @param idx 列名索引，从0开始
	 * @return idx 所标识的列(key)的元素集合
	 */
	default List<Object> column(final Integer idx) {
		return this.lla(idx, e -> e);
	}

	/**
	 * 提取columnName 所在的列数据列表
	 * 
	 * @param <T> 列的元数据类型
	 * @param <U> 返回值变换后的列的类型
	 * @param key 列名：这是对lla的别名
	 * @param t2u 列值转换函数 t-&gt;u
	 * @return key 所标识的列(key)的经过t2u变换后的元素集合
	 */
	default <T, U> List<U> column(final String key, final Function<T, U> t2u) {
		return this.lla(key, t2u);
	}

	/**
	 * 返回key的列元素集合：强制转换为targetClass 的类型 <br>
	 * 
	 * 类型采用强制转换，因此可能会出现不同列之间的类型不一致的风险，使用时候需要注意。这一部分需要在编程中给注意与防范。<br>
	 * 类库设置不予考虑。<br>
	 * 
	 * @param <T>         列的元数据类型
	 * @param key         列名
	 * @param targetClass 列的值类型类
	 * @return idx 所标识的列(key)的元素集合(强制姐转换为T类型)
	 */
	@SuppressWarnings("unchecked")
	default <T> List<T> column(final String key, final Class<T> targetClass) {
		return this.lla(key, e -> (T) e);
	}

	/**
	 * 返回key位置的列元素集合
	 * 
	 * @param key 列名：这是对lla的别名
	 * @return key 所标识的列(key)的元素集合
	 */
	default List<Object> column(final String key) {
		return this.lla(key, e -> e);
	}

	/**
	 * 提取所有的列数据列表
	 * 
	 * @param <T> 列的元数据类型
	 * @param <U> 返回值变换后的列的类型
	 * @param t2u 列值转换函数:t-&gt;u
	 * @return 列集合每个列族使一个U类型的列表
	 */
	default <T, U> List<List<U>> columns(final Function<T, U> t2u) {
		return this.keys().stream().map(name -> this.lla(name, t2u)).collect(Collectors.toList());
	}

	/**
	 * 返回所有的列的数据的列表 <br>
	 * 
	 * 类型采用强制转换，因此可能会出现不同列之间的类型不一致的风险，使用时候需要注意。这一部分需要在编程中给注意与防范。<br>
	 * 类库设置不予考虑。<br>
	 * 
	 * @param <T> 列的元数据类型
	 * @return 列集合每个列族使一个U类型的列表
	 */
	@SuppressWarnings("unchecked")
	default <T> List<List<T>> columns(final Class<T> targetClass) {
		return this.keys().stream().map(name -> this.lla(name, e -> (T) e)).collect(Collectors.toList());
	}

	/**
	 * 提取列值集合：通通返回List &lt;Object&gt;
	 * 
	 * @return 列集合每个列族使一个U类型的列表
	 */
	default List<List<Object>> columns() {
		return this.keys().stream().map(name -> this.lla(name, e -> e)).collect(Collectors.toList());
	}

	/**
	 * 提取列值集合：把列转换成U类型的对象。
	 *
	 * @param <T>  源数据列表的元素类型
	 * @param <U>  目标列的结果类型
	 * @param tt2u 列值转换函数:tt-&gt;u
	 * @return 列集合每个列族使一个IRecord类型的列表
	 */
	default <T, U> List<U> cols(final Function<List<T>, U> tt2u) {
		return this.colS(tt2u).collect(Collectors.toList());
	}

	/**
	 * 提取列值集合：把列转换成IRecord
	 * 
	 * @return 列集合每个列族使一个IRecord类型的列表
	 */
	default List<IRecord> cols() {
		return this.cols(IRecord::L2REC);
	}

	/**
	 * 提取列值集合：把列转换成U类型的对象。
	 *
	 * @param <T>  源数据列表的元素类型
	 * @param <U>  目标列的结果类型
	 * @param tt2u 列值转换函数:tt-&gt;u
	 * @return 列集合每个列族使一个IRecord类型的流
	 */
	@SuppressWarnings("unchecked")
	default <T, U> Stream<U> colS(final Function<List<T>, U> tt2u) {
		return this.keys().stream().map(name -> (tt2u.apply((List<T>) this.lla(name, t -> (T) t))));
	}

	/**
	 * 提取列值集合：把列转换成IRecord
	 *
	 * @return 列集合每个列族使一个IRecord类型的列表
	 */
	default Stream<IRecord> colS() {
		return this.colS(IRecord::L2REC);
	}

	/**
	 * 返回key列名的列数据:列表
	 * 
	 * @param <T>  列的元数据类型
	 * @param <U>  返回值变换后的列的类型
	 * @param key  列名索引，从0开始
	 * @param tt2u 列值转换函数:t-&gt;u
	 * @return idx 所标识的列(key)的经过t2u变换后的元素集合
	 */
	default <T, U> U col(final String key, final Function<? super List<T>, U> tt2u) {
		final List<T> tt = this.lla(key, (T e) -> e);
		return tt2u.apply(tt);
	}

	/**
	 * 返回idx列索引位置的列数据:列表
	 * 
	 * @param <T>  列的元数据类型
	 * @param <U>  返回值变换后的列的类型
	 * @param idx  列名索引，从0开始
	 * @param tt2u 列值转换函数:t-&gt;u
	 * @return idx 所标识的列(key)的经过t2u变换后的元素集合
	 */
	default <T, U> U col(final Integer idx, final Function<? super List<T>, U> tt2u) {
		return this.col(this.keyOfIndex(idx), tt2u);
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个 记录集合的列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * 待分类的数据集合(rr)。即源数据,采用默认的this.rows()<br>
	 * 指标计算器evaluator 列指标：分类结果的计算器，采用LittleTree::LIST<br>
	 * <br>
	 * 
	 * @param keys 分类的key列表，分类依据字段列表。或者说 分类层级的序列
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	default IRecord pivotTable(final Object... keys) {
		return this.rows().stream().collect(pvtclc(keys));
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个 记录集合的列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * 待分类的数据集合(rr)。即源数据,采用默认的this.rows()<br>
	 * 指标计算器evaluator 列指标：分类结果的计算器，采用LittleTree::LIST<br>
	 * <br>
	 * 
	 * @param keys 分类 的层级key列表，分类依据字段列表。或者说 分类层级的序列，采用',','\'和'/' 进行分隔, null
	 *             或者空白字符，默认为record的keys()作为分类层级。
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	default IRecord pivotTable(final String keys) {
		final var kk = keys != null && !keys.matches("\\s*") // 输入参数的有效性检查
				? Arrays.stream(keys.split("[,\\\\/]+")).map(String::strip).toArray() // 转换成Object[]
				: null; // 分类 的层级key列表
		return this.rows().stream().collect(pvtclc(kk));
	}

	/**
	 * Unpivot a DFrame from wide to long format, optionally leaving identifiers set
	 * <br>
	 * 
	 * - mapper 元素类型格式化函数,类型为， (key:String,value:Object)-&gt;new_value <br>
	 * - hh
	 * 列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * <br>
	 * 
	 * @param id_vars    Column(s) to use as identifier variables.
	 * @param value_vars Column(s) to unpivot. If not specified, uses all columns
	 *                   that are not set as id_vars.
	 * @param var_name   scalarName to use for the ‘variable’ column. If null use
	 *                   ‘variable’.
	 * @param value_name Name to use for the ‘value’ column.
	 * @return Unpivoted DFrame.
	 */
	default IRecord melt(final List<String> id_vars, final List<String> value_vars, final String var_name,
			final String value_name) {
		return _melt((s, e) -> e, null, id_vars, value_vars, var_name, value_name);
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
	 * 术语来源于：R reshape, 接口 原型来源于 pandas Unpivot a DFrame from wide to long format,
	 * optionally leaving identifiers set <br>
	 * 
	 * - mapper 元素类型格式化函数,类型为， (key:String,value:Object)-&gt;new_value <br>
	 * - hh
	 * 列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * <br>
	 * 
	 * @param id_vars    Column(s) to use as identifier variables.
	 * @param value_vars Column(s) to unpivot. If not specified, uses all columns
	 *                   that are not set as id_vars. <br>
	 *                   - var_names scalarName to use for the ‘variable’ column. If
	 *                   null use ‘variable’. <br>
	 *                   - value_name Name to use for the ‘value’ column. <br>
	 * @return Unpivoted DFrame.
	 */
	default IRecord melt(final List<String> id_vars, final List<String> value_vars) {

		return _melt((s, e) -> e, null, id_vars, value_vars, "variable", "value");
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
	 * 术语来源于：R reshape, 接口 原型来源于 pandas Unpivot a DFrame from wide to long format,
	 * optionally leaving identifiers set <br>
	 * 
	 * @param <T>        中间结果类型: 由 mapper: (k:String,o:Object)->t:T 生成。
	 * @param mapper     元素类型格式化函数,类型为， (key:String,value:Object)-&gt;new_value:T
	 * @param hh         列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * 
	 * @param id_vars    标识变量集合 Column(s) to use as identifier variables.
	 * @param value_vars 值变量集合 Column(s) to unpivot. If not specified, uses all
	 *                   columns that are not set as id_vars.
	 * @param var_name   窄长scalarName to use for the ‘variable’ column. If null use
	 *                   ‘variable’.
	 * @param value_name Name to use for the ‘value’ column.
	 * @return Unpivoted DFrame. 窄长型数据
	 */
	default <T> List<IRecord> melt2recs(final BiFunction<String, Object, T> mapper, final List<String> hh,
			final List<String> id_vars, final List<String> value_vars, final String var_name, final String value_name) {

		final var final_id_vars = id_vars;// identifier variables names,id标识变量名称集合
		final var final_value_vars = value_vars;// variables names,即值变量名称集合
		final var final_var_name = var_name;// 新生成的值变量字段名称:分组名称
		final var final_value_name = value_name;// 新生成的值变量字段名称
		final var idvars = final_id_vars.toArray(new String[0]);// 标识字段名称。

		/**
		 * 对于宽结构的数据矩阵：我们一般把它视为一个列向量集合。(v1,v2,v3,...). 由于 每个向量vi
		 * 都表示数据(总体)的一种特定属性，拥有一定数据范围 有时又把它称为变量variable，即数据表格中的列在数据分析与操作中被视为变量variable
		 * 
		 * 在具体应用中每个变量vi 的意义是不相同的,它们又可以分为两类 {idvars:id标识向量集合} 和 {value_vars:value向量集合}
		 * 来表达层级结构，因此可以 根据idvars 和value_vars 来构造出层级结构。比如： 一条数据记录：(id11:idvar,
		 * id12:idvar2 ,value11:value_var1, value12:value_var2,value13:value_var3)
		 * 就可以表示为 以下的树形结构（层级结构) (id11:idvar1,id12:idvar2) 张:姓氏, 三:名字 |---- value11:
		 * value_var1 |---- 18601690610 电话 |---- vlaue12: value_var2 |----
		 * gbench@sina.com 邮箱 |---- value13: value_var3 |---- 上海徐家汇法华镇路 地址 分析上述结构，就会发现
		 * 这其实是一种融合操作：value_vars 集合的变量的名称给汇集成一个新的变量/列var_name, 并把相对的值汇聚成 value_name变量/列
		 * idvar1 idvar2 var_name value_name 姓 名 属性 属性值 value11 vlaue12 value_var1
		 * value11 张 三 电话 18601690610 value11 vlaue12 value_var2 vlaue12 张 三 邮箱
		 * gbench@sina.com value11 vlaue12 value_var3 vlaue13 张 三 地址 上海徐家汇法华镇路
		 * 这是把短宽型1x5的数据 给 转换成 窄长型3x4的数据的 变换方式。
		 */

		final var items = this.rows(mapper, hh).stream().flatMap( // DFrame 的行记录名称
				wide_rec -> {// 待进行分解的短宽型数据记录
					// 采用原型法来构架窄记录，
					final var proto = wide_rec.filter(idvars);// 制作原型数据, 即长窄型数据的前导部分的 idvars部分的数据：前导记录
					final var value_vars_left = new AtomicInteger(value_vars.size());// 剩余尾槌的value_vars数量
					return final_value_vars.stream().map(value_var -> {// 依次把属性字段信息 value_vars 加入到 protod额后半段中
						final var narrow_item = value_vars_left.getAndDecrement() != 1 // 当剩下有多个时候用副本否则直接使用原型，避免浪费
								? proto.duplicate() // 窄长属性记录的前半段：标识变量部分。采用对proto的副本进行追加的方式来构造
								: proto;// 最后一条记录使用原型
						narrow_item.add(final_var_name, value_var);// 把值属性名value_var增加到final_var_name 列名下
						narrow_item.add(final_value_name, wide_rec.get(value_var));// 把值属性value_var的值增加到final_value_name
																					// 列名下
						return narrow_item;// 返回新生成窄记录
					});// return 依据wide_rec新生成窄记录 narrow_item 集合：这是一对多的变换
				}).collect(Collectors.toList());// flatMap

		return items;// 窄长数据的集合
	}

	/**
	 * 一般用于内部调用 DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
	 * 术语来源于：R reshape, 接口 原型来源于 pandas
	 * 
	 * Unpivot a DFrame from wide to long format, optionally leaving identifiers set
	 * <br>
	 * 
	 * @param <T>        中间结果类型: 由 mapper: (k:String,o:Object)->t:T 生成。
	 * @param mapper     元素类型格式化函数,类型为， (key:String,value:Object)-&gt;new_value:T
	 * @param hh         列名序列,若为空则采用EXCEL格式的列名称(0:A,1:B,...),如果列表不够也采用excelname给予填充区别只不过添加了一个前缀"_"
	 * 
	 * @param id_vars    Column(s) to use as identifier variables.
	 * @param value_vars Column(s) to unpivot. If not specified, uses all columns
	 *                   that are not set as id_vars.
	 * @param var_name   scalarName to use for the ‘variable’ column. If null use
	 *                   ‘variable’.
	 * @param value_name Name to use for the ‘value’ column.
	 * @return Unpivoted DFrame.
	 */
	default <T> IRecord _melt(final BiFunction<String, Object, T> mapper, final List<String> hh,
			final List<String> id_vars, final List<String> value_vars, final String var_name, final String value_name) {
		final var dfm = REC();// 生成一个DFrame
		this.melt2recs(mapper, hh, id_vars, value_vars, var_name, value_name).forEach(item -> {
			item.kvs().forEach(p -> dfm.computeIfAbsent(p.key(), _k -> new LinkedList<>()).add(p.value()));// item.kvs()
		});// forEach

		return dfm;
	}

	/**
	 * 判断键值是否都满足表达式
	 * 
	 * @param <T>       元素类型
	 * @param predicate 兼职的判断函数:(key,value)->boolean
	 * @return 是否所有的值都匹配表达式 predicate
	 */
	@SuppressWarnings("unchecked")
	default <T> boolean allMatch(BiPredicate<String, T> predicate) {
		return this.kvs().stream().allMatch(e -> predicate.test(e.key(), (T) e.value()));
	}

	/**
	 * 判断键值是否都满足表达式
	 * 
	 * @param <T>       元素类型
	 * @param predicate 兼职的判断函数
	 * @return 是否所有的值都匹配表达式 predicate
	 */
	@SuppressWarnings("unchecked")
	default <T> boolean allMatch(Predicate<T> predicate) {
		return this.values().stream().map(e -> (T) e).allMatch(predicate);
	}

	/**
	 * 判断键值是否有满足predicate的元素
	 * 
	 * @param <T>       元素类型
	 * @param predicate 兼职的判断函数
	 * @return 判断键值是否有满足predicate的元素
	 */
	@SuppressWarnings("unchecked")
	default <T> boolean anyMatch(Predicate<T> predicate) {
		return this.values().stream().map(e -> (T) e).anyMatch(predicate);
	}

	/**
	 * 判断键值是否有满足predicate的元素
	 * 
	 * @param <T>       元素类型
	 * @param predicate 兼职的判断函数:(key,value)->boolean
	 * @return 判断键值是否有满足predicate的元素
	 */
	@SuppressWarnings("unchecked")
	default <T> boolean anyMatch(BiPredicate<String, T> predicate) {
		return this.kvs().stream().anyMatch(e -> predicate.test(e.key(), (T) e.value()));
	}

	/**
	 * 判断键值元素是否都为typeClass的T的类型
	 * 
	 * @param <T>       元素类型
	 * @param typeClass 结果类类型
	 * @return 判断键值是否有满足predicate的元素
	 */
	default <T> boolean typeOf(Class<T> typeClass) {
		return this.allMatch(e -> typeClass.isAssignableFrom(e.getClass()));
	}

	/**
	 * 原型法构造对象：对象衍生化，对象产生自:proto.derive(props) <br>
	 * 只是duplicate 与add 混合。 <br>
	 * 相当于 this.duplicate().add(_REC(props)); 的操作用户采用原型的方式进行 创建数据对象。 <br>
	 * 标准版的记录生成器, map 生成的是LinkedRecord <br>
	 * 
	 * @param props 自定义属性的 键,值序列:key0,value0,key1,value1,....
	 * @return 衍生于 proto(this)的IRecord对象
	 */
	default IRecord derive(final Object... props) {
		return this.duplicate().add(_REC(props));
	}

	/**
	 * 原型法构造对象：对象衍生化，对象产生自:proto.derive(props) <br>
	 * 只是duplicate 与set 混合。 <br>
	 * 相当于 this.duplicate().set(idx, value); 的操作用户采用原型的方式进行 创建数据对象。 <br>
	 * 标准版的记录生成器, map 生成的是LinkedRecord <br>
	 * 
	 * @param idx   索引号从0开始
	 * @param value 值对象
	 * @return 衍生于 proto(this)的IRecord对象
	 */
	default IRecord derive(final int idx, final Object value) {
		return this.duplicate().set(idx, value);
	}

	/**
	 * 原型法构造对象：对象衍生化，对象产生自:proto.derive(props) <br>
	 * 只是duplicate 与add 混合。<br>
	 * 相当于 this.duplicate().add(rec); 的操作用户采用原型的方式进行 创建数据对象。 <br>
	 * 标准版的记录生成器, map 生成的是LinkedRecord <br>
	 * 
	 * @param rec IRecord对象
	 * @return 衍生于 proto(this)的IRecord对象
	 */
	default IRecord derive(final IRecord rec) {
		return this.duplicate().add(rec);
	}

	/**
	 * 键名改名并扁平化 ,相当于 this.aoks2rec2(aliases).flat(null);<br>
	 * 把一个 IRecord 给予扁平化:非递归式即只扁平化一层 把 一个 order: <br>
	 * REC("id",1,"product","苹果","user",REC("name","张三","sex","男"),"address",REC("city","上海","district","长宁区"));
	 * <br>
	 * 转换成 <br>
	 * REC("id",1,"product","苹果","user.name","张三","address.city","上海","address.district","长宁区");<br>
	 * 
	 * @param aliases 别名 键名的别名 <br>
	 *                sep 层级间的 分隔符号 默认为 "." <br>
	 * @return 扁平化的 数据记录
	 */
	default IRecord flat2(final String aliases) {
		return this.flat2(aliases.split("[,\\\\/\\s]+"), null);
	}

	/**
	 * 键名改名并扁平化 ,相当于 this.aoks2rec2(aliases).flat(sep);<br>
	 * 把一个 IRecord 给予扁平化:非递归式即只扁平化一层<br>
	 * 把 一个 order: <br>
	 * REC("id",1,"product","苹果","user",REC("name","张三","sex","男"),"address",REC("city","上海","district","长宁区"));
	 * <br>
	 * 转换成 <br>
	 * REC("id",1,"product","苹果","user.name","张三","address.city","上海","address.district","长宁区");<br>
	 * 
	 * @param aliases 别名 键名的别名 <br>
	 * @param sep     层级间的 分隔符号 默认为 "." <br>
	 * @return 扁平化的 数据记录
	 */
	default IRecord flat2(final String aliases, final String sep) {
		return this.flat2(aliases.split("[,\\\\/\\s]+"), sep);
	}

	/**
	 * 键名改名并扁平化 ,相当于 this.aoks2rec2(aliases).flat(null);<br>
	 * 把一个 IRecord 给予扁平化:非递归式即只扁平化一层 把 一个 order: <br>
	 * REC("id",1,"product","苹果","user",REC("name","张三","sex","男"),"address",REC("city","上海","district","长宁区"));
	 * <br>
	 * 转换成 <br>
	 * REC("id",1,"product","苹果","user.name","张三","address.city","上海","address.district","长宁区");<br>
	 * 
	 * @param aliases 别名 键名的别名 <br>
	 *                sep 层级间的 分隔符号 默认为 "." <br>
	 * @return 扁平化的 数据记录
	 */
	default IRecord flat2(final String aliases[]) {
		return this.flat2(aliases, null);
	}

	/**
	 * 键名改名并扁平化 ,相当于 this.aoks2rec2(aliases).flat(sep);<br>
	 * 把一个 IRecord 给予扁平化:非递归式即只扁平化一层 把 一个 order: <br>
	 * REC("id",1,"product","苹果","user",REC("name","张三","sex","男"),"address",REC("city","上海","district","长宁区"));
	 * <br>
	 * 转换成 <br>
	 * REC("id",1,"product","苹果","user.name","张三","address.city","上海","address.district","长宁区");<br>
	 * 
	 * @param aliases 别名 键名的别名 <br>
	 * @param sep     层级间的 分隔符号 默认为 "." <br>
	 * @return 扁平化的 数据记录
	 */
	default IRecord flat2(final String aliases[], final String sep) {
		return this.aoks2rec2(aliases).flat(sep);
	}

	/**
	 * 把一个 IRecord 给予扁平化:非递归式即只扁平化一层 把 一个 order: <br>
	 * REC("id",1,"product","苹果","user",REC("name","张三","sex","男"),"address",REC("city","上海","district","长宁区"));
	 * <br>
	 * 转换成 <br>
	 * REC("id",1,"product","苹果","user.name","张三","address.city","上海","address.district","长宁区");<br>
	 * sep 层级间的 分隔符号 默认为 "." <br>
	 * 
	 * @return 扁平化的 数据记录
	 */
	default IRecord flat() {
		return flat(null);
	}

	/**
	 * 把一个 IRecord 给予扁平化:非递归式即只扁平化一层 把 一个 order: <br>
	 * REC("id",1,"product","苹果","user",REC("name","张三","sex","男"),"address",REC("city","上海","district","长宁区"));
	 * <br>
	 * 转换成 <br>
	 * REC("id",1,"product","苹果","user.name","张三","address.city","上海","address.district","长宁区");<br>
	 * 
	 * @param sep 层级间的 分隔符号
	 * @return 扁平化的 数据记录
	 */
	default IRecord flat(final String sep) {
		final var separator = (sep == null) ? "." : sep; // 默认分隔符为 "."
		final var rec = this
				.tupleS().reduce(
						REC(), (r,
								kvp) -> kvp.value() instanceof IRecord || kvp.value() instanceof Map
										? r.derive(kvp.value() instanceof Map
												? kvp.value2((Map<String, Object> p) -> REC(p)
														.aoks2rec(k -> kvp.key() + separator + k))
												: kvp.value2((IRecord p) -> p.aoks2rec(k -> kvp.key() + separator + k))) // 使用
																															// kvp.key
																															// 作为前缀
										: r.derive(kvp.key(), kvp.value()), // 直接加入
						IRecord::derive);
		return rec;
	}

	/**
	 * 把 一个IRecord节点 更换成 Node &lt; String &gt; <br>
	 * rootName 根节点名称,默认为"root" <br>
	 * valueKey KVPair的value 在 Node props中的键值名,默认为 :"value"<br>
	 * 
	 * @return 根节点 Node &lt; String &gt;
	 */
	default Node<String> treeNode() {
		return this.treeNode(null, null);
	}

	/**
	 * 把 一个IRecord节点 更换成 Node &lt; String &gt;
	 * 
	 * @param rootName 根节点名称，默认为:"root" <br>
	 * @param valueKey KVPair的value 在 Node props中的键值名 ，默认为:"value"<br>
	 * @return 根节点 Node &lt; String &gt;
	 */
	default Node<String> treeNode(final String rootName, final String valueKey) {
		return TREENODE(this, rootName, valueKey);
	}

	/**
	 * IRececord 之间的比较大小,比较的keys选择当前对象的keys,当 null 小于任何值,即null值会排在牵头
	 */
	@Override
	default int compareTo(final IRecord o) {
		if (o == null)
			return 1;
		if (this.keys().equals(o.keys())) {
			return IRecord.cmp(this.keys()).compare(this, o);
		} else {
			final var set = new LinkedHashSet<>(this.keys());
			set.addAll(o.keys());// 归并
			final var kk = set.stream().sorted().toArray(String[]::new);
			System.err.println(
					"比较的两个个键名序列(" + this.keys() + "," + o.keys() + ")不相同,采用归并键名序列进行比较:" + Arrays.deepToString(kk));
			return IRecord.cmp(kk, true).compare(this, o);
		} // if
	}

	/**
	 * 返回hash代码
	 * 
	 * @return hash代码
	 */
	default int hashint() {
		if (Thread.currentThread().getStackTrace().length > MAX_STACK_TRACE_SIZE) {
			return Objects.hash(this.tupleS().flatMap(p -> Stream.of(p._1(), p._2())).toArray());
		} else {
			return Objects.hash(this.keys().toArray());
		}
	}

	/////////////////////////////////////////////////////////////////////
	// 以下是IRecord DFrame 类型的方法区域:所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)
	/////////////////////////////////////////////////////////////////////

	/**
	 * 把key列转换成Map结构 <br>
	 * 当且仅当 key 所代表的数据是一个Map&lt;String,Object&gt; 的实例返回一个 key 的value对象。<br>
	 * 否则 生成一个 Map的复制品(clone) <br>
	 *
	 * @param key 需要进行分解的字段名：一般为json结构的列
	 * @return Map&lt;String, Object&gt;
	 */
	@SuppressWarnings("unchecked")
	default Map<String, Object> asMap(final String key) {
		final Object obj = this.get(key);
		if (obj == null) {
			return null;
		} else if (obj instanceof Map) { // Map类型
			final var mm = (Map<Object, Object>) obj;
			if (mm.keySet().iterator().next() instanceof String) { // key 为String类型
				return (Map<String, Object>) obj;// Map 的key是不含null值的
			} else { // key 为非String类型
				final var mss = new LinkedHashMap<String, Object>();// 复制品
				mm.forEach((k, v) -> mss.put(k.toString(), v));// 进行数据复制。
				return mss;
			} // if
		} else if (obj instanceof IRecord) { // IRecord 结构
			return ((IRecord) obj).toMap();
		} else { // 其他类型视为json 字符串给予转换映射
			return map(key, e -> Json.json2obj(e, Map.class));
		} // if
	}

	/**
	 * 把idx列转换成Map结构
	 *
	 * @param idx 列索引从0开始
	 * @return Map&lt;String, Object&gt;
	 */
	default Map<String, Object> asMap(final int idx) {
		final String key = this.keyOfIndex(idx);
		return key == null ? null : asMap(key);
	}

	/**
	 * 对值集合进行Map,不对key 进行变换 <br>
	 * 把函数mapper 应用到 values 对象:Object-&gt;U 对象。<br>
	 * 转换成一个 String -&gt; U 的Map <br>
	 * 
	 * @param <U>         valuemapper 对值的变换结果
	 * @param valuemapper 值变换函数
	 * @return {(String,U)} 结构的Map
	 */
	default <U> LinkedHashMap<String, U> toMap(final Function<Object, U> valuemapper) {
		final LinkedHashMap<String, U> mm = new LinkedHashMap<>();
		this.toMap().forEach((k, v) -> mm.put(k, valuemapper.apply(v)));
		return mm;
	}

	/**
	 * 对键名集合进行Map,不对value 进行变换
	 * 
	 * @param <T>       key 的类型
	 * @param keymapper 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @return 键名变换后键值对儿({T ,Object)}
	 */
	default <T> LinkedHashMap<T, Object> toMap2(final Function<String, T> keymapper) {
		return toMap2(keymapper, identity);
	}

	/**
	 * 分别对keys 和 values 进行变换。
	 * 
	 * @param <T>         key 的类型
	 * @param <U>         value 的类型
	 * @param keymapper   key 键名变换函数:把原来的字符类型的key,转换成T类型的键名。
	 * @param valuemapper value 的变换函数:把原来的Object类型的Value,转换成U类型的值。
	 * @return {(t, u)} 的Map
	 */
	default <T, U> LinkedHashMap<T, U> toMap2(final Function<String, T> keymapper, Function<Object, U> valuemapper) {
		final LinkedHashMap<T, U> mm = new LinkedHashMap<>();
		this.foreach((k, v) -> mm.put(keymapper.apply(k), valuemapper.apply(v)));
		return mm;
	}

	/**
	 * 返回一个 LinkedHashMap&lt;String,T&gt;
	 * 
	 * @param <T>    LinkedHashMap 中的值得类型
	 * @param tclass 值类型 :null 表示Object.class
	 * @return LinkedHashMap&lt;String,T&gt;
	 */
	@SuppressWarnings("unchecked")
	default <T> LinkedHashMap<String, T> toLhm(final Class<T> tclass) {
		if (this instanceof LinkedHashMap)
			return (LinkedHashMap<String, T>) this;
		final LinkedHashMap<String, T> mm = new LinkedHashMap<>();
		this.foreach(mm::put);
		return mm;
	}

	/**
	 * 返回一个 LinkedHashMap&lt;String,T&gt;
	 * 
	 * @return LinkedHashMap&lt;String,T&gt;
	 */
	default LinkedHashMap<String, Object> toLhm() {
		return toLhm(null);
	}

	/**
	 * 转换成一个 转义映射 '\' 被转译成 \\ "'" 被转译成 \'
	 * 
	 * @return 转移后的Map
	 */
	default Map<String, Object> toEscapedMap() {
		final var m = this.toMap();
		m.forEach((k, v) -> {// 依次对每个MAP元素进行转义
			Object obj = v;
			if (v != null) {
				if (v instanceof String) {
					obj = v.toString().replace("\"", "\\\\\"").replace("'", "\\'"); // 字符串转义
				} else if (v instanceof IRecord) { // 递归进行 转换
					final var mm = this.toMap(); // 转换成数组形式避免 进入 自我包含 即 v == this 形成 的死循环
					obj = REC((Map<?, ?>) mm).toEscapedMap();
				} else if (v instanceof Map) {
					obj = REC((Map<?, ?>) obj).toEscapedMap();
				} else {
					obj = v;
				} // if
			} // if
			m.put(k, obj);// 重新放入map结构
		});// forEach

		return m;
	}

	/**
	 * 转换成一个 String -> String 的Map
	 * 
	 * @return String -> String 的Map
	 */
	default Map<String, String> toStrMap() {
		return this.toMap(e -> e + "");
	}

	/**
	 * 转换成一个 Properties
	 * 
	 * @return Properties
	 */
	default Properties toProps() {
		final Properties props = new Properties();
		this.toStrMap().forEach(props::put);
		return props;
	}

	/**
	 * 转换成一维数组
	 */
	default String[] toStrArray() {
		return this.tupleS().map(String::valueOf).toArray(String[]::new);
	}

	/**
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]), <br>
	 * 比如 Integer,Long,Double 等，把当前集合中的值集合转换成 一维数组 <br>
	 * 转换成一个一维数组
	 * 
	 * @return Object 类型的一维数组
	 */
	default Object[] toArray() {
		return this.tupleS().map(g -> g._2() == null ? "" : g._2()).toArray(Object[]::new);
	}

	/**
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]), <br>
	 * 比如 Integer,Long,Double 等，把当前集合中的值集合转换成 一维数组<br>
	 * 转换成一维数组
	 * 
	 * @param <T>    源元素的类型
	 * @param <U>    目标元素的类型
	 * @param mapper 元素变换函数 t-&gt;u
	 * @param uclass 目标结果类型：如果uclass 为null则懂mapper的取值结果中提取uclass 类型
	 * @return U类型的一维数组
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U[] toArray(final Function<T, U> mapper, final Class<U> uclass) {
		final Object[] oo = this.tupleS().map(e -> (T) e._2()).map(mapper).toArray();
		Class<U> _uclass = uclass;
		if (uclass == null)
			for (Object o : oo) {
				if (o != null) {
					_uclass = (Class<U>) o.getClass();
					break;
				}
			} // if
		if (_uclass == null)
			_uclass = (Class<U>) Object.class;
		final U[] uu = (U[]) Array.newInstance(_uclass, oo.length);
		for (int i = 0; i < oo.length; i++) {
			try {
				uu[i] = (U) oo[i];
			} catch (Exception ignored) {
				// do nothing
			} // try
		} // for
		return uu;
	}

	/**
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]), <br>
	 * 比如 Integer,Long,Double 等，把当前集合中的值集合转换成 一维数组 <br>
	 * 转换成一维数组
	 * 
	 * @param <T>    源元素的类型
	 * @param <U>    目标元素的类型
	 * @param mapper 元素变换函数 t-&gt;u
	 * @return U类型的一维数组
	 */
	default <T, U> U[] toArray(final Function<T, U> mapper) {
		return toArray(mapper, (Class<U>) null);
	}

	/**
	 * 为了保证执行效率 uclass 没有没有使用 智能转换 <br>
	 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]), <br>
	 * 比如 Integer,Long,Double 等，把当前集合中的值集合转换成 一维数组<br>
	 * 
	 * @param <U>    目标元素的类型
	 * @param uclass 结果的类型类，这是一个占位符类型，用于提示辅助编译。
	 * @return U类型的一维数组
	 */
	@SuppressWarnings("unchecked")
	default <U> U[] toArray(final Class<U> uclass) {
		final Object[] oo = this.toArray();
		final Class<U> final_uclass = uclass == null ? (Class<U>) Object.class : uclass;
		final U[] uu = (U[]) Array.newInstance(final_uclass, oo.length);

		for (int i = 0; i < oo.length; i++) {
			try {
				uu[i] = (U) oo[i];
			} catch (Exception ignored) {
				// do nothing
			} // try
		} // for

		return uu;
	}

	/**
	 * 生成一个二维数组矩阵：数组元素采用 Object 类型.
	 * 
	 * @return Object[][] U类型的二维数组
	 */
	default Object[][] toArray2() {
		return toArray2(e -> e, Object.class);
	}

	/**
	 * 这个方法一般用于生成一个 列向量矩阵：比如REC("variable",new Double[]{1d,2d,3d}).toArray2();
	 * 生成二维数组：视每个元素为单独二维数组的单独的列。
	 * 
	 * @param <U>    目标类型的数据结果
	 * @param uclass 目标类型的class
	 * @return U[][] U类型的二维数组
	 */
	@SuppressWarnings("unchecked")
	default <U> U[][] toArray2(final Class<U> uclass) {
		final var final_uclass = uclass != null ? uclass : (Class<U>) Object.class;
		if (uclass == Object.class)
			return (U[][]) (Object) this.toArray2();
		else
			return this.toArray2(t -> IRecord.rec2obj(REC(0, t), final_uclass));
	}

	/**
	 * 生成一个二维数组矩阵,生成数组的类型,采用t2u对于第一列元素进行 Apply,提取第一个非空元素的类型作为U类型，对于 <br>
	 * 全为null的情况采用Object.class作为默认值。<br>
	 * 第一列的值不存在则返回null<br>
	 * 
	 * @param <T> t2u的源类型，即Record 中List的中的元素类型 一般为Object,除非明确知道IRecord中的具体的数据结构
	 * @param <U> t2u的目标结果的类型
	 * @param t2u 值变换函数 t->u
	 * @return U[][] U类型的二维数组
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U[][] toArray2(final Function<T, U> t2u) {
		final var ll = this.lla(0, t2u);// 尝试应用到第一列t2u，提取Class<U>的类型信息。
		if (ll == null)
			return null;// 列值不村子直接返回
		final var cellClass = ll.stream().filter(Objects::nonNull).map(e -> (Class<U>) e.getClass()).findFirst()
				.orElse((Class<U>) Object.class);
		return this.toArray2(t2u, cellClass);
	}

	/**
	 * 生成一个二维数组矩阵,生成数组的类型,采用t2u对于第一列元素进行 Apply,提取第一个非空元素的类型作为U类型，对于 <br>
	 * 全为null的情况采用Object.class作为默认值。<br>
	 * 
	 * @param <T>       t2u的源类型，即Record 中List的中的元素类型
	 *                  一般为Object,除非明确知道IRecord中的具体的数据结构
	 * @param <U>       t2u的目标结果的类型
	 * @param t2u       值变换函数 t->u
	 * @param cellClass 结果容器的数据类型,cellClass 为null 视作 Object.class
	 * @return U[][] U类型的二维数组
	 */
	@SuppressWarnings("unchecked")
	default <T, U> U[][] toArray2(final Function<T, U> t2u, final Class<U> cellClass) {
		final var shape = this.shape();
		final var final_cellClass = cellClass == null ? (Class<U>) Object.class : cellClass;
		final var ooo = this.rows().stream().map(row -> row.toArray(t2u, cellClass)).toArray(n -> {
			U[][] uu = null;
			uu = (U[][]) Array.newInstance(final_cellClass, shape._1(), shape._2());
			return uu;
		});// ooo
		return ooo;
	}

	/**
	 * 部分转换<br>
	 * 把Record（一般为单元素记录：即只有一个键的Record） 转换成 目标类型对象,采用 rec2obj 转换对象,<br>
	 * toTarget 一般用于提取元素的第一个键值元素。只有当第一个元素不满足要求，才尝试采用cast的方式进行整体转换。
	 * 
	 * @param <T>          目标类型
	 * @param defaultValue 默认值,转换失败或是结果为null的时候返回默认值
	 * @return T 结构的对象
	 */
	@SuppressWarnings("unchecked")
	default <T> T toTarget(final T defaultValue) {
		return this.toTarget(defaultValue != null ? (Class<T>) defaultValue.getClass() : null, defaultValue);
	}

	/**
	 * 部分转换<br>
	 * 把Record（一般为单元素记录：即只有一个键的Record） 转换成 目标类型对象,采用 rec2obj 转换对象,<br>
	 * toTarget 一般用于提取元素的第一个键值元素。只有当第一个元素不满足要求，才尝试采用cast的方式进行整体转换。
	 * 
	 * @param <T>         目标类型
	 * @param targetClass 目标类型类：null 则返回IRecord本身
	 * @return T 结构的对象
	 */
	default <T> T toTarget(final Class<T> targetClass) {
		return this.toTarget(targetClass, null);
	}

	/**
	 * 部分转换<br>
	 * 把Record（一般为单元素记录：即只有一个键的Record） 转换成 目标类型对象,采用 rec2obj 转换对象,<br>
	 * toTarget 一般用于提取元素的第一个键值元素。只有当第一个元素不满足要求，才尝试采用cast的方式进行整体转换。
	 * 
	 * @param <T>          目标类型
	 * @param targetClass  目标类型类：null 则返回IRecord本身
	 * @param defaultValue 默认值,转换失败或是结果为null的时候返回默认值
	 * @return T 结构的对象
	 */
	default <T> T toTarget(final Class<T> targetClass, final T defaultValue) {
		if (targetClass == null)
			return ((T) defaultValue);
		final var t = IRecord.rec2obj(this, targetClass);
		return t == null ? defaultValue : t;
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * 按照列进行展示 对DFrame进行初始化
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
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * 按照列进行展示 对DFrame进行初始化
	 * 
	 * @param cell_formatter 元素内容初始化
	 * @return 格式化字符串
	 */
	default String toString2(final Function<Object, String> cell_formatter) {
		return this.toString2(null, cell_formatter);
	}

	/**
	 * DFrame 类型的数据方法,所谓DFrame 是指键值对儿中的值为List的IRecord(kvs)<br>
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
	 * 按照列进行展示 对DFrame进行初始化
	 * 
	 * @return 格式化字符串
	 */
	default String toString2() {
		return toString2(null);
	}

	/**
	 * 字符串格式化
	 * 
	 * @param cell_formatter 键值得格式化算法
	 * @return IRecord 的字符串形式
	 */
	default String toString(final Function<Object, String> cell_formatter) {
		final var builder = new StringBuilder();
		final Function<Object, String> final_cell_formatter = cell_formatter != null ? cell_formatter : v -> {
			if (v == null)
				return "(null)";
			var line = "{0}";// 数据格式化
			if (v instanceof Date) {
				line = "{0,Date,yyyy-MM-dd HH:mm:ss}"; // 时间格式化
			} else if (v instanceof Number) {
				line = "{0,Number,#}"; // 数字格式化
			} // if

			return MessageFormat.format(line, v);
		};// cell_formatter

		this.kvs().forEach(
				p -> builder.append(p._1()).append(":").append(final_cell_formatter.apply(p._2())).append("\t"));

		return builder.toString().trim();
	}

	/////////////////////////////////////////////////////////////////////
	// 以下是IRecord 的静态方法区域
	/////////////////////////////////////////////////////////////////////

	/**
	 * 全连接连个rec1,rec2:返回一个 类型为{(key,(v1,v2)}的LinkedHashMap mm <br>
	 * mm 中的key 与 rec1,rec2 中的key 相一致,可以这样理解mm是对rec1,rec2中的键值对儿按照key 进行分组，<br>
	 * 每个分组是一个二元组,1号位置是rec1 中的元素 <br>
	 * 2号位置是rec1中的元素，过rec1,rec2中没有对应与key的值,则对应key的值设置为null <br>
	 * 
	 * 全连接连个rec1,rec2 ,比如:<br>
	 * var r1 = REC("1","A","2","B","4","D");<br>
	 * var r2 = REC("1","a","2","b","3","c");<br>
	 * var r3 = join(r1,r2);<br>
	 * 返回：<br>
	 * key tuple2 1 A --> a <br>
	 * 2 B --> b <br>
	 * 4 D --> null <br>
	 * 3 null --> c <br>
	 * 
	 * @param rec1 左边的记录(键值对儿集合)
	 * @param rec2 右边的记录(键值对儿集合)
	 * @return 连接后的连个字 {(key,(v1,v2)} 的集合，这是一个LinkedHashMap,是一个有序序列
	 */
	static Map<String, Tuple2<Object, Object>> join(final IRecord rec1, final IRecord rec2) {
		// mm 中的key 与 rec1,rec2 中的key 相一致,可以这样理解mm是对rec1,rec2中的键值对儿按照key
		// 进行分组，每个分组是一个二元组,1号位置是rec1 中的元素
		// 2号位置是rec1中的元素，过rec1,rec2中没有对应与key的值,则对应key的值设置为null
		final var mm = new LinkedHashMap<String, Tuple2<Object, Object>>();// 返回值:{(key,(v1,v2)}

		if (rec1 != null)
			rec1.foreach((k, v) -> {// 遍历第一个record:k 是键名,v是record中与k对应的值
				// tuple_value 是一个用于存放字段连接结果的 二元组,1号位置rec1中的数据,2号位置rec2中对应的数据。遍历rec1号时候2号位置不予设置
				mm.compute(k, (key, tuple_value) -> {// key 与 k 是同一个值,
					if (tuple_value == null)
						tuple_value = Tuple2.TUP2(v, null); // 初次创建,2号位置不予设置
					else
						tuple_value._1(v);// 该键名位置窜在
					return tuple_value; // 返回当前的值
				}); // 遍历MAP
			});// rec1.foreach

		if (rec2 != null)
			rec2.foreach((k, v) -> {// 遍历第一个record:k 是键名,v是record中与k对应的值
				// tuple_value 是一个用于存放字段连接结果的 二元组,1号位置rec1中的数据,2号位置rec2中对应的数据。遍历rec2号时候1号位置不予设置
				mm.compute(k, (key, tuple_value) -> {// key 与 k 是同一个值,
					if (tuple_value == null)
						tuple_value = Tuple2.TUP2(null, v); // 初次创建,1号位置不予设置
					else
						tuple_value._2(v); // 已经存在则修改原有的值
					return tuple_value; // 返回当前的值
				});// 遍历MAP
			});// rec2.foreach

		return mm;
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param <T>  元素类型
	 * @param ll   待操作的列表
	 * @param size 滑动的窗口大小
	 * @param step 每次移动的步长
	 * @return 滑动窗口的列表。
	 */
	static <T> List<List<T>> sliding(final List<T> ll, final int size, final int step) {
		final int n = ll.size();
		final var aa = new ArrayList<>(ll);// 转换成数组类型
		final var res = new LinkedList<List<T>>();// 返回结果

		for (int i = 0; i < n; i += step) {
			final var from = i;// 开始位置
			final var to = i + size;// 结束位置
			final var sl = aa.subList(from, Math.min(to, n));// sublist
			res.add(sl);
		} // for

		return res;
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param <T>  元素类型
	 * @param ll   待操作的列表
	 * @param size 滑动的窗口大小
	 * @param step 每次移动的步长
	 * @param b    是否过滤掉窗口大小不等于size的窗口,false 不过滤 ,true过滤,即 只返回
	 *             [[1,2],[2,3],[3,4]],把后面的残缺窗口 [4]舍弃
	 * @return 滑动窗口的列表。
	 */
	@SuppressWarnings("unchecked")
	static <T> List<T[]> sliding(final List<T> ll, final int size, final int step, final boolean b) {
		final int n = ll.size();
		final var aa = new ArrayList<>(ll);// 转换成数组类型
		final var res = new LinkedList<T[]>();// 返回结果
		final var componentType = ll.stream().filter(Objects::nonNull).findFirst().map(e -> (Class<T>) e.getClass())
				.orElse((Class<T>) Object.class);

		for (int i = 0; i < n; i += step) {
			final var from = i;// 开始位置
			final var to = i + size;// 结束位置
			final var sl = aa.subList(from, Math.min(to, n));// sublist
			if (b && sl.size() != size)
				break;
			res.add(sl.toArray(x -> (T[]) Array.newInstance(componentType, x)));
		} // for

		return res;
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param <T>  元素类型
	 * @param ll   待操作的列表
	 * @param size 滑动的窗口大小
	 * @param step 每次移动的步长
	 * @return 滑动窗口的流
	 */
	static <T> Stream<List<T>> slidingS(final List<T> ll, final int size, final int step) {
		return IRecord.sliding(ll, size, step).stream();
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param <T>  元素类型
	 * @param ll   待操作的列表
	 * @param size 滑动的窗口大小
	 * @param step 每次移动的步长
	 * @param b    是否过滤掉窗口大小不等于size的窗口,false 不过滤 ,true过滤,即 只返回
	 *             [[1,2],[2,3],[3,4]],把后面的残缺窗口 [4]舍弃
	 * @return 滑动窗口的流
	 */
	static <T> Stream<T[]> slidingS(final List<T> ll, final int size, final int step, final boolean b) {
		return IRecord.sliding(ll, size, step, b).stream();
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param <T>  元素类型
	 * @param tt   待操作的数据数组
	 * @param size 滑动的窗口大小
	 * @param step 每次移动的步长
	 * @param b    是否过滤掉窗口大小不等于size的窗口,false 不过滤 ,true过滤,即 只返回
	 *             [[1,2],[2,3],[3,4]],把后面的残缺窗口 [4]舍弃
	 * @return 滑动窗口的流
	 */
	static <T> Stream<T[]> slidingS(final T[] tt, final int size, final int step, final boolean b) {
		return IRecord.slidingS(Arrays.asList(tt), size, step, b);
	}

	/**
	 * 数据窗口滑动<br>
	 * 对一个:1 2 3 4,按照 size:为2,step为1的参数进行滑动的结果。<br>
	 * 
	 * | size | 每个窗口大小为 size,每次移动的步长为step<br>
	 * [1 2]<br>
	 * step0:[2 3]<br>
	 * - step1:[3 4]<br>
	 * - - step2:[4]<br>
	 * 返回:[ [1,2], [2,3], [3,4], [4] ]<br>
	 * 
	 * @param <T>    元素类型
	 * @param stream 待操作的数据流
	 * @param size   滑动的窗口大小
	 * @param step   每次移动的步长
	 * @param b      是否过滤掉窗口大小不等于size的窗口,false 不过滤 ,true过滤,即 只返回
	 *               [[1,2],[2,3],[3,4]],把后面的残缺窗口 [4]舍弃
	 * @return 滑动窗口的流
	 */
	static <T> Stream<T[]> slidingS(final Stream<T> stream, final int size, final int step, final boolean b) {
		return IRecord.slidingS(stream.collect(Collectors.toList()), size, step, b);
	}

	/**
	 * 把一个信息路径：path分解成各个组分结构的IRecord:kvp 定义为(路径位点key,路径信息元)：例如 :<br>
	 * (path:中国/辽宁/大连, keys:["country","province","city"]),给变换成:
	 * REC("country","中国","province","辽宁","city","大连") <br>
	 * <br>
	 * 
	 * @param path  信息路劲,即具有层级的信息结构,是一个路径信息元（信息单元）的序列,比如：中国/辽宁/大连
	 * @param delim path的分隔符
	 * @param keys  路径层级的名称：每个阶层应该如何称谓，比如对于path:中国/辽宁/大连，
	 *              第一层是国家，第二层是省份，第三层是城市，层级称谓keys就是[国家,省份,城市]
	 * @return IRecord
	 */
	static IRecord path2kvs(String path, String delim, String keys[]) {
		final var rec = REC();
		final var pp = path.split(delim);
		final var n = Math.min(keys.length, pp.length);
		Stream.iterate(0, i -> i < n, i -> i + 1).forEach(i -> rec.add(keys[i], pp[i + 1]));
		return rec;
	}

	/**
	 * 把一个信息路径：path分解成各个组分结构的IRecord:kvp 定义为(路径位点key,路径信息元)：例如 :<br>
	 * (path:中国/辽宁/大连, keys:["country","province","city"]),给变换成:
	 * REC("country","中国","province","辽宁","city","大连") <br>
	 * <br>
	 * 
	 * @param path 信息路劲,即具有层级的信息结构,是一个路径信息元（信息单元）的序列,比如：中国/辽宁/大连 信息单元的分隔符： delim 默认为
	 *             "[,/\\]+" <br>
	 * @param keys 路径层级的名称：每个阶层应该如何称谓，比如对于path:中国/辽宁/大连，
	 *             第一层是国家，第二层是省份，第三层是城市，层级称谓keys就是[国家,省份,城市]
	 * @return IRecord
	 */
	static IRecord path2kvs(String path, String keys[]) {
		final var rec = REC();
		final var delim = "[,/\\\\]+";
		final var pp = path.split(delim);
		final var n = Math.min(keys.length, pp.length);
		Stream.iterate(0, i -> i < n, i -> i + 1).forEach(i -> rec.add(keys[i], pp[i + 1]));
		return rec;
	}

	/**
	 * 这是专门为pv2rec_eval 函数设计的值变换函数：所以采用采用value2kvs 而不是 obj2rec这样的名称。<br>
	 * 把一个对象转换一个IRecord:kvs 是IRecord的别名与IRecord可以互换 <br>
	 * 
	 * 用指定的key 去构建一个Record,确保返回的结果中含有一个 名为key 键。<br>
	 * 
	 * @param value 值对象
	 * @param key   值名称
	 * @return IRecord IRecord 即键值对 kvs
	 */
	static IRecord value2kvs(Object value, String key) {
		if (value instanceof IRecord)
			return (IRecord) value;
		if (value instanceof Map)
			return REC((IRecord) value);
		return REC(key, value);
	}

	/**
	 * 这是专门为pv2rec_eval 函数设计的值变换函数：所以采用采用value2kvs 而不是 obj2rec这样的名称。<br>
	 * 把一个对象转换陈一个IRecord:kvs 是IRecord的别名与IRecord可以互换<br>
	 * 
	 * @param value 值对象
	 * @return IRecord 即键值对 kvs
	 */
	static IRecord value2kvs(Object value) {
		return value2kvs(value, "value");
	}

	/**
	 * 层级信息的维护合并 path,value 计算 即 (path,value) 的变换为IRecord（键值对集合)<br>
	 * 把 path 通过 pathkeys 进行变换<br>
	 * 把value 通过value_key 进行变换<br>
	 * 比如对于 (path:iphone6/苹果公司,value:6800) 进行pv2rec_eval("产品名/生产企业","价格") 计算 即:<br>
	 * pv2rec_eval("产品名/生产企业","价格").apply("iphone6/苹果公司",6800);<br>
	 * 就会返回：(产品名:iphone6,生产企业:苹果公司,价格:6800) 的一个IRecord 即kvs<br>
	 * 
	 * 使用示例：<br>
	 * var r =
	 * res.dfs_eval_forone(pv2rec_eval("pct,vendor".split(","),"value"));<br>
	 * path,value 转record 的 计算器 <br>
	 * 
	 * @param pathkeys  路径 keys, 使用"[,/\\\\]+"进行分割
	 * @param value_key 值 的key名
	 * @return BiFunction&lt;String,Object,IRecord&gt;
	 */
	static BiFunction<String, Object, IRecord> pv2rec_eval(final String pathkeys, String value_key) {
		return pv2rec_eval(pathkeys.split("[,/\\\\]+"), value_key);
	}

	/**
	 * 层级信息的维护合并 path,value 计算 即 (path,value) 的变换为IRecord（键值对集合)<br>
	 * 把 path 通过 pathkeys 进行变换<br>
	 * 把value 通过value_key 进行变换<br>
	 * 比如对于 (path:iphone6/苹果公司,value:6800) 进行pv2rec_eval("产品名/生产企业","价格") 计算 即:<br>
	 * pv2rec_eval("产品名/生产企业","价格").apply("iphone6/苹果公司",6800);<br>
	 * 就会返回：(产品名:iphone6,生产企业:苹果公司,价格:6800) 的一个IRecord 即kvs<br>
	 * 
	 * 使用示例：<br>
	 * var r =
	 * res.dfs_eval_forone(pv2rec_eval("pct,vendor".split(","),"value"));<br>
	 * path,value 转record 的 计算器 <br>
	 * 
	 * @param pathkeys  路径 keys 对层级信息(path)进行分解：维度分解的键名序列
	 * @param value_key 值 key 值键名
	 * @return BiFunction&lt;String,Object,IRecord&gt; 一个 path,value 转record 的 计算器
	 */
	static BiFunction<String, Object, IRecord> pv2rec_eval(final String[] pathkeys, String value_key) {
		return (path, value) -> {
			var rec = REC();
			rec.add(path2kvs(path, pathkeys)); // 路径信息转换成 IRecord 键值对儿
			rec.add(value2kvs(value, value_key)); // value 信息转换成键值对儿
			return rec;
		};
	}

	/**
	 * 遍历IRecord 深度遍历 <br>
	 * 
	 * @param rec    遍历的对象
	 * @param cs     biCONSumer 回调函数, 即KV值的处理函数( key,values), 需要注意cons是一个二元函数。 key
	 *               是指节点的路径信息; values 是指与key所对应的值的*集合*：这个集合是Stream 类型的；
	 *               之所以采用Stream来封装值数据，是为统一单值与集合(Collection)类型的值数据。stream
	 *               读一下只返回一个值。单值与多值是一样的。
	 * @param prefix 键值的前缀,null表示"/"，即根节点
	 * @param delim  层级分隔符号,null表示"/"
	 */
	@SuppressWarnings("unchecked")
	static void dfs(final IRecord rec, final BiConsumer<String, Stream<Object>> cs, final String prefix,
			final String delim) {
		final var default_prefix = "/";// 默认的根前缀（第0基层的前缀）
		final var default_delim = "/";// 默认的阶层分隔符号
		final var final_prefix = (prefix == null ? default_prefix : prefix);// 生成可用的不可变对象
		final var final_delim = (delim == null ? default_delim : delim);// 生成可用的不可变对象
		final Function<String, String> new_prefix = (k) -> {// 更具节点名称(key)生成下一阶层的名称前缀。
			var d = ((default_prefix.equals(final_prefix)) ? "" : final_delim);
			return final_prefix + d + k; // 新的阶层前缀。
		};

		rec.foreach((k, v) -> {
			Stream<Object> stream = null;// 流是对单值对象与Collection的统一描述。这是dfs所规定的默认数据）数值的访问方法。
			if (v instanceof Collection) {// 集合类型给予展开
				stream = ((Collection<Object>) v).stream();
			} else if (v instanceof IRecord) {
				dfs((IRecord) v, cs, new_prefix.apply(k), null);// 修改前缀，递归进入更深一层
			} else if (v instanceof Map) {
				var r = REC();// 把map 转换成 IRecord
				((Map<Object, Object>) v).forEach((key, value) -> r.add(key.toString(), value));
				if (r.size() > 0)
					dfs(r, cs, new_prefix.apply(k), delim);// 修改前缀，递归进入更深一层
				else
					stream = new LinkedList<>().stream(); // 空值列表
			} else {// 默认对象绑定。
				stream = v instanceof Stream ? (Stream<Object>) v : Stream.of(v);// 生成一个单值的流
			} // if

			// 回调函数处理
			if (stream != null)
				cs.accept(new_prefix.apply(k), stream);
		});// record 对象遍历
	}

	/**
	 * SUPplier&lt;LinkedList&gt;的一个简写：这是一个容器生运算符 <br>
	 * 生成一个 列表：使用示例 <br>
	 * e.collect(supll(Object.class), (List&lt;Object&gt;aa,
	 * KVPair&lt;String,Object&gt;a)-&gt;{aa.add( MFT( "{0}[{1}]", a.key(),a.value()
	 * ) );}, <br>
	 * cbll(Object.class) <br>
	 * ).stream().map(f-&lt;f+"").collect(Collectors.joining("*")), <br>
	 * 
	 * @param <R>   容器中的元素的类型
	 * @param clazz 容器中的元素的类型的class
	 * @return 新建列表
	 */
	static <R> Supplier<List<R>> supll(Class<R> clazz) {
		return LinkedList::new;
	}

	/**
	 * ComBiner&lt;LinkedList&gt; 的一个简写：这是一个操作运算符 e.collect(supll(Object.class),
	 * (List&lt;Object&gt;aa, KVPair&lt;String,Object&gt;a)-&gt;{aa.add( MFT(
	 * "{0}[{1}]", a.key(),a.value() ) );}, cbll(Object.class)
	 * ).stream().map(f-&lt;f+"").collect(Collectors.joining("*")),
	 * 
	 * @param <R>   容器中的元素的类型
	 * @param clazz 容器中的元素的类型的class
	 * @return 新列表
	 */
	static <R> BinaryOperator<List<R>> cbll(Class<R> clazz) {
		return (List<R> aa, List<R> bb) -> {
			aa.addAll(bb);
			return aa;
		};
	}

	/**
	 * 把一个键值对的数据转换成一个R的类型
	 * 
	 * @param <R>   容器中的元素的类型
	 * @param obj2r 把键值对儿kv 转换成 目标对象R的函数
	 * @return R类型的数据对象
	 */
	static <R> Function<KVPair<String, Object>, R> kv2r(final Function<Object, R> obj2r) {
		return (kv) -> obj2r.apply(kv.value());
	}

	/**
	 * 合并两个 Record：即这个一个按照键名对连个 键值对儿集合进行 的 并集操作。
	 * 
	 * @param rec1               IRecord 记录1
	 * @param rec2               IRecord 记录2
	 * @param rec2_appendto_rec1 是否把 rec2 追加到rec1, 是 追加,否 合并 生成了一个新的对象。
	 * @return 合并了rec1 和 rec2 两个记录内容键值儿 后的Record
	 */
	static IRecord union(final IRecord rec1, final IRecord rec2, final boolean rec2_appendto_rec1) {
		var rec = rec2_appendto_rec1 ? rec1 : REC();
		if (rec1 != null && rec != rec1)
			rec1.foreach(rec::add);
		if (rec2 != null)
			rec2.foreach(rec::add);
		return rec;
	}

	/**
	 * 格式化记录列表
	 * 
	 * @param recs 记录列表
	 * @return 格式化记录列表
	 */
	static String format(final List<IRecord> recs) {
		return format(recs, "\t");
	}

	/**
	 * 格式化记录列表
	 * 
	 * @param recs 记录列表
	 * @param sep  分隔符
	 * @return 格式化记录列表
	 */
	static String format(final List<IRecord> recs, final String sep) {

		final StringBuilder buffer = new StringBuilder();
		@SuppressWarnings("unused")
		final var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (recs == null)
			return "";
		final int n = recs.size();
		List<String> hh = new ArrayList<>(n);// 表头字段顺序
		for (int i = 0; i < n; i++) {
			if (i == 0) {
				IRecord r = recs.get(0);
				buffer.append(recs.get(i).tupleS().map(kv -> kv.key() + "").collect(Collectors.joining(sep)))
						.append("\n");
				hh = MAP(r.kvs(), KVPair::key);// 只提取第一个记录的结构字段
			} // if

			final StringBuilder line = new StringBuilder();
			// 之所以使用 hh,而不是kvs collect joining 是为了保持异质的record 集合。即里那个中不同结构的record混合list
			for (var h : hh) {
				var obj = recs.get(i).get(h);
				if (obj == null)
					obj = "(null)";// 空数值变换
				line.append(obj).append(sep);
			}
			buffer.append(line.toString().trim()).append("\n");
		} // for

		return buffer.toString();
	}

	/**
	 * java反射bean的get方法
	 * 
	 * @param clazz     类名
	 * @param fieldName 属性名
	 * @return getter 方法
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Method getter(final Class clazz, final String fieldName) {
		StringBuilder sb = new StringBuilder();
		sb.append("get").append(fieldName.substring(0, 1).toUpperCase()).append(fieldName.substring(1));
		try {
			Class[] types = new Class[] {};
			return clazz.getMethod(sb.toString(), types);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return null;
	}

	/**
	 * java反射bean的set方法
	 * 
	 * @param clazz     类名
	 * @param fieldName 字段名
	 * @return setter 方法
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Method setter(final Class clazz, final String fieldName) {
		try {
			Class[] parameterTypes = new Class[1];
			Field field = clazz.getDeclaredField(fieldName);
			parameterTypes[0] = field.getType();// 返回参数类型
			StringBuilder sb = new StringBuilder();
			sb.append("set").append(fieldName.substring(0, 1).toUpperCase()).append(fieldName.substring(1));
			Method method = clazz.getMethod(sb.toString(), parameterTypes);
			return method;
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return null;
	}

	/**
	 * 格式化数据对象
	 * 
	 * @param recs IRecord 列表
	 * @return 格式化数据对象
	 */
	static String FMT(final List<IRecord> recs) {
		return format(recs);
	}

	/**
	 * 格式化数据对象,带有行号的格式化
	 * 
	 * @param recs IRecord 列表
	 * @return 带有行号的格式化
	 */
	static String FMT2(final List<IRecord> recs) {
		final var line = format(recs);
		final var seed = new AtomicLong(0L);// 行号生成器的种子
		final Supplier<String> rownum = () -> {// 行号生成器
			final var n = seed.getAndIncrement();
			return n == 0 ? "#" : n + "";// 行号生成器
		};
		return JOIN(Arrays.stream(line.split("\n")).map(e -> Jdbcs.format("{0}\t{1}", rownum.get(), e)), // 行格式化
				"\n");// JOIN 行连接
	}

	/**
	 * 格式化数据对象
	 * 
	 * @param recs IRecord 列表
	 * @param sep  行内分隔符
	 * @return 格式化数据对象
	 */
	static String FMT2(final List<IRecord> recs, final String sep) {
		return format(recs, sep);
	}

	/**
	 * 获取指定Class的字段列表
	 * 
	 * @param clsu 类对象
	 * @return clsu的字段列表
	 */
	static <U> Map<String, Field> FIELDS(final Class<U> clsu) {
		Map<String, Field> map = new LinkedHashMap<>();
		Arrays.stream(clsu.getDeclaredFields()).forEach(e -> map.put(e.getName(), e));
		return map;
	}

	/**
	 * 把一个java object的值类型 转换成 获取对应SQL类型
	 * 
	 * @return obj对应的SQL数据类型。
	 */
	static String OBJ2SQLTYPE(final Object obj) {
		return OBJ2SQLTYPE(obj, null);
	}

	/**
	 * 把对象转换成key->value对儿
	 * 
	 * @param obj 数据对象
	 * @return key->value对儿集合的IRecord
	 */
	static IRecord OBJ2REC(final Object obj) {
		return OBJ2REC(obj, (Predicate<Field>) null);
	}

	/**
	 * 把对象转换成key->value对儿
	 * 
	 * @param obj     数据对象
	 * @param pfilter 字段过滤,只返回返回结果为true的结果字段.
	 * @return key->value对儿
	 */
	static IRecord OBJ2REC(final Object obj, final Predicate<Field> pfilter) {
		return REC(OBJ2KVS(obj, pfilter == null ? (t) -> true : pfilter));
	}

	/**
	 * 把对象转换成key->value对儿
	 * 
	 * @param obj  数据对象
	 * @param keys 字段过滤数组,只返keys中字段.
	 * @return key->value对儿
	 */
	static IRecord OBJ2REC(final Object obj, String keys[]) {
		final var kk = Arrays.asList(keys);
		return REC(OBJ2KVS(obj, e -> kk.contains(e.getName())));
	}

	/**
	 * 把对象转换成key->value对儿
	 * 
	 * @param obj  数据对象
	 * @param keys 字段过滤,只返keys中字段. keys 中采用"[,\\s，/\\\\]+"的分隔符进行分隔。
	 * @return key->value对儿
	 */
	static IRecord OBJ2REC(final Object obj, String keys) {
		final var kk = Arrays.asList(keys.split("[,\\s，/\\\\]+"));
		return REC(OBJ2KVS(obj, e -> kk.contains(e.getName())));
	}

	/**
	 * 把对象转换成key->value对儿<br>
	 * 分解一个对象obj成为键值对儿集合即IRecord 记录。
	 * 
	 * @param obj     待分解的数据对象
	 * @param pfilter 字段过滤,只返回返回结果为true的结果字段. 当pfilter 为null 不对字段进行过滤。
	 * @return 键值对儿集合
	 */
	static Map<String, Object> OBJ2KVS(final Object obj, final Predicate<Field> pfilter) {
		final var mm = new LinkedHashMap<String, Object>();
		final Predicate<Field> _pfilter = pfilter == null ? e -> true : pfilter;
		final var clazz = obj.getClass();

		if (obj != null) {
			Arrays.stream(clazz.getDeclaredFields()).filter(_pfilter).forEach(fld -> {
				final var getter = getter(clazz, fld.getName()); // 提取getter方法
				Object v = null;
				if (getter != null) {// 优先尝试使用getter方法进行数据读取
					try {
						v = getter.invoke(obj, (Object[]) null);
					} catch (Exception ignored) {
					}
				} else { // 读取不到getter方法而后才进行字段直接读取
					fld.setAccessible(true); // 加入字段直接访问方法
					try {
						v = fld.get(obj);
					} catch (Exception ignored) {
					}
				} // if
				mm.put(fld.getName(), v);
			}); // forEach
		} // obj!=null

		return mm;
	}

	/**
	 * 用inits初始化对象obj 数据初始化,使用 inits 来初始化对象obj,用inits中的key的值设置obj中的对应的字段
	 * 
	 * @param objClass 待初始对象的类型class
	 * @param inits    初始源数据
	 * @return T 类型的结果
	 */
	static <T> T OBJINIT(final Class<T> objClass, final IRecord inits) {
		if (objClass == null || inits == null)
			return null;
		return OBJINIT(objClass, inits.toMap());
	}

	/**
	 * 用inits初始化obj 数据初始化,使用 inits 来初始化对象obj,用inits中的key的值设置obj中的对应的字段
	 * 
	 * @param objClass 待初始对象的类型class
	 * @param inits    初始源数据
	 * @return T 类型的结果
	 */
	static <T> T OBJINIT(final Class<T> objClass, final Map<String, Object> inits) {
		if (inits == null)
			return null;
		T obj = null;
		try {
			obj = objClass.getConstructor((Class[]) null).newInstance((Object[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (obj == null)
			return null;
		return OBJINIT(obj, inits);
	}

	/**
	 * 用inits初始化对象obj 数据初始化,使用 inits 来初始化对象obj,用inits中的key的值设置obj中的对应的字段
	 * 
	 * @param obj   待初始对象
	 * @param inits 初始源数据
	 * @return T 类型的结果
	 */
	static <T> T OBJINIT(final T obj, final IRecord inits) {
		if (inits == null || obj == null)
			return null;
		return OBJINIT(obj, inits.toMap());
	}

	/**
	 * 数据初始化,使用 inits 来初始化对象obj,用inits中的key的值设置obj中的对应的字段:带有简单的类型转换功能
	 * 
	 * @param obj   待初始对象
	 * @param inits 初始源数据
	 * @param <T>   待初始化的对象的类型
	 * @return T 类型的结果
	 */
	static <T> T OBJINIT(final T obj, final Map<String, ?> inits) {

		Arrays.stream(obj.getClass().getDeclaredFields()) // 依次处目标对象结构的各个字段属性
				.filter(e -> inits.containsKey(e.getName())).forEach(fld -> {// 字段数值的设置
					try {
						fld.setAccessible(true);
						final Object value = inits.get(fld.getName());
						if (value == null)
							return;

						/* 基本类型与包裹类之间的转换 */ {
							// 这里关于类型的转换没有一个好的实现办法，目前来看只能一个的写出来。
							final Class<?> src = value.getClass();// 源类型
							final Class<?> target = fld.getType();// 目标类型
							// System.out.println(fld.getName()+"("+value+")["+target+"<-"+src+"]");
							if ((target == int.class || target == Integer.class)
									&& (src == Long.class || src == long.class)) { // 长整类型换 整型
								fld.set(obj, ((Long) value).intValue());
								return;
							}
							if ((target == long.class || target == Long.class)
									&& (src == int.class || src == Integer.class)) { // 长整类型换 整型
								fld.set(obj, ((Number) value).longValue());
								return;
							}

							// 从timestamp 转换为其他类型
							if (src == Timestamp.class && target == Date.class) {// Timestamp -> Date
								final Date d = ((Timestamp) value);
								fld.set(obj, d);
								return;
							} else if (src == Timestamp.class && target == LocalDate.class) {// Timestamp ->
																								// LocalDate
								final Date d = ((Timestamp) value);
								fld.set(obj, Times.dt2ld(d));
								return;
							} else if (src == Timestamp.class && target == LocalDateTime.class) {// Timestamp ->
																									// LocalDateTime
								final Date d = ((Timestamp) value);
								fld.set(obj, Times.dt2ldt(d));
								return;
							} else if (src == Timestamp.class && target == LocalTime.class) {// Timestamp ->
																								// LocalTime
								final Date d = ((Timestamp) value);
								fld.set(obj, Times.dt2lt(d));
								return;
							}

							if (src == Date.class && target == Timestamp.class) {// Date->Timestamp
								final Timestamp d = new Timestamp(((Date) value).getTime());
								fld.set(obj, d);
								return;
							} else if (src == LocalDateTime.class && target == Timestamp.class) {// LocalDateTime->Timestamp
								final Timestamp d = new Timestamp(Times.ldt2dt((LocalDateTime) value).getTime());
								fld.set(obj, d);
								return;
							} else if (src == LocalDate.class && target == Timestamp.class) {// LocalDateTime->Timestamp
								final Timestamp d = new Timestamp(Times.ld2dt((LocalDate) value).getTime());
								fld.set(obj, d);
								return;
							} else if (src == LocalTime.class && target == Timestamp.class) {// LocalDateTime->Timestamp
								final Timestamp d = new Timestamp(Times.lt2dt((LocalTime) value).getTime());
								fld.set(obj, d);
								return;
							}
						} // /*基本类型与包裹类之间的转换*/

						if (fld.getType() == value.getClass()) {
							fld.set(obj, value);
						} else if (value instanceof String) {// 对字符串类型尝试做类型转换
							if (fld.getType() == Character.class || fld.getType() == char.class) {// 数字
								fld.set(obj, (value.toString().charAt(0)));
							} else if (fld.getType() == Integer.class || fld.getType() == int.class) {// 数字
								fld.set(obj, ((Double) Double.parseDouble(value.toString())).intValue());
							} else if (fld.getType() == Double.class || fld.getType() == double.class) {// 数字
								fld.set(obj, Double.parseDouble(value.toString()));
							} else if (fld.getType() == Float.class || fld.getType() == float.class) {// 数字
								fld.set(obj, Float.parseFloat(value.toString()));
							} else if (fld.getType() == Short.class || fld.getType() == short.class) {// 数字
								fld.set(obj, Short.parseShort(value.toString()));
							} else if (fld.getType() == Boolean.class || fld.getType() == boolean.class) {// 数字
								fld.set(obj, Boolean.parseBoolean(value.toString()));
							} else if (fld.getType() == Long.class || fld.getType() == long.class) {// 数字
								fld.set(obj, ((Number) Double.parseDouble(value.toString())).longValue());
								// System.out.println(obj+"===>"+value);
							} else if (fld.getType() == Date.class || fld.getType() == LocalDate.class
									|| fld.getType() == LocalDateTime.class || fld.getType() == LocalTime.class) {// 时间类型的处理。

								Date date = null;// 日期对象
								if (value instanceof Number) {
									final long time = ((Number) value).longValue();
									date = new Date(time);
								} else {
									final String ss[] = "yyyy-MM-dd HH:mm:ss,yyyy-MM-dd HH:mm,yyyy-MM-dd HH,yyyy-MM-dd,yyyy-MM,yyyy-MM,yyyy"
											.split("[,]+");
									for (String s : ss) {
										try {
											date = new SimpleDateFormat(s).parse(value.toString());
										} catch (Exception ignored) {
										}
										;
										if (date != null)
											break;
									} // for
								} // if( value instanceof Number
									// 设置时间字段

								if (fld.getType() == LocalDate.class) {// LocalDate
									fld.set(obj, Times.dt2ld(date));
								} else if (fld.getType() == LocalDateTime.class) {// LocalDateTime
									fld.set(obj, Times.dt2ldt(date));
								} else if (fld.getType() == LocalTime.class) {// LocalTime
									fld.set(obj, Times.dt2lt(date));
								} else {
									fld.set(obj, date);
								} // if(fld.getType() == LocalDate.class)

							} // else if(fld.getType() == Date.class)

						} // if(fld.getType() == value.getClass())
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});// forEach(fld

		return obj;
	}// OBJINIT

	/**
	 * 把一个java object的值类型 转换成 获取对应SQL类型
	 * 
	 * @param size 类型的尺寸大小
	 * @return 把一个java object的值类型 转换成 获取对应SQL类型
	 */
	static String OBJ2SQLTYPE(final Object obj, final Integer size) {
		if (obj == null)
			return null;
		Class<?> cls = null;
		cls = (obj instanceof Class<?>) ? (Class<?>) obj : obj.getClass();
		final var mm = new HashMap<Object, String>();
		mm.put(byte.class, "char(1)");
		mm.put(Byte.class, "char(1)");
		mm.put(char.class, "int");
		mm.put(Character.class, "int");
		mm.put(int.class, "int");
		mm.put(Integer.class, "int");
		mm.put(long.class, "long");
		mm.put(Long.class, "long");
		mm.put(short.class, "int");
		mm.put(Short.class, "int");
		mm.put(double.class, "double");
		mm.put(Double.class, "double");
		mm.put(float.class, "double");
		mm.put(float.class, "double");
		mm.put(boolean.class, "tinyint(1)");
		mm.put(boolean.class, "tinyint(1)");
		if (size != null && cls == String.class)
			mm.put(String.class, "varchar(" + size + ")");
		else
			mm.put(String.class, "TEXT");
		mm.put(StringBuffer.class, "TEXT");
		mm.put(Date.class, "timestamp");

		return mm.get(cls);
	}

	/**
	 * 具体了的REC函数的实现：为了保证REC的美观性与不限定参数的性质。 标准版的记录生成器, map 生成的是LinkedRecord
	 * 
	 * @param kvs 键值序列: key1,value1,key2,value2,....
	 * @return IRecord对象
	 */
	static IRecord _REC(final Object[] kvs) {
		final LinkedRecord rec = new LinkedRecord();

		if (kvs == null) { // 空值
			return rec;
		} else if (kvs.length == 1) {// 单个元素
			final Object o = kvs[0];
			if (o instanceof Map<?, ?> m) { // (key,value) 序列
				return LinkedRecord.of(m);
			} else if (o instanceof IRecord r) { // (key,value) 序列
				return r;
			} else {
				return rec;
			}
		} // if

		for (int i = 0; i < kvs.length; i += 2) {
			String key = kvs[i] == null ? "null" : kvs[i].toString();// 键值名
			Object value = (i + 1) < kvs.length ? kvs[i + 1] : null;// 如果最后一位是key则它的value为null
			rec.data.put(key, value);
		} // for

		return rec;
	}

	/**
	 * 标准版的记录生成器, map 生成的是LinkedRecord
	 * 
	 * @param kvs 键,值序列:key0,value0,key1,value1,....
	 * @return IRecord对象
	 */
	static IRecord REC(final Object... kvs) {
		if (kvs.length < 2) { // 单项目元素
			if (kvs.length > 0) {
				final var o = kvs[0];
				if (o instanceof IRecord rec) {
					return rec;
				} else if (o instanceof Map<?, ?> m) {
					return REC(m);
				} else if (o instanceof Iterable<?> aa) {
					final var rec = REC();
					for (var a : aa) {
						if (a instanceof Tuple2<?, ?> t) {
							rec.add(t._1, t._2);
						} // if
					} // for
					return rec;
				} else if (o instanceof String s && Json.json2map(s) instanceof Map<?, ?> m) {
					return REC(m);
				} else { // 空项目
					return REC();
				} // if
			} else {
				return REC();
			} // if
		} else { // 多项目元素
			return _REC(kvs);
		} // if
	}

	/**
	 * 标准版的记录生成器, map 生成的是LinkedRecord
	 * 
	 * @param map 键值映射,键名会调用掉map键的toString 来给予生成。
	 * @return IRecord对象
	 */
	static IRecord REC(final Map<?, ?> map) {
		final LinkedRecord rec = new LinkedRecord();
		map.forEach((k, v) -> rec.data.put(k == null ? "null" : k.toString(), v));
		return rec;
	}

	/**
	 * 把一个键值对儿
	 * 
	 * @param kvps 键值对儿序列 [(key0,value0),(key1,value1),...]
	 * @return IRecord 结果记录
	 */
	@SafeVarargs
	static IRecord REC(final KVPair<String, Object>... kvps) {
		final var rec = new LinkedRecord();
		Stream.of(kvps).forEach(p -> rec.add(p.key(), p.value()));
		return rec;
	}

	/**
	 * 把一个键值对儿
	 * 
	 * @param kvps 键值对儿序列 [(key0,value0),(key1,value1),...]
	 * @return IRecord 结果记录
	 */
	static IRecord REC(final List<KVPair<String, Object>> kvps) {
		final var rec = new LinkedRecord();
		kvps.forEach(p -> rec.add(p.key(), p.value()));
		return rec;
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 
	 * 示例:<br>
	 * REC3(REC("a/b/c",123,"a/b/d",456),REC("e/f",7,"g",8)).dfs_forone((p,v)->{<br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。 <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 示例代码: <br>
	 * final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98)); <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * <br>
	 * 
	 * 生成IRecord记录
	 * 
	 * @param recs IRecord记录集合 <br>
	 *             initrec 初始化 record 默认为 REC
	 * @return IRecord
	 */
	static IRecord REC3(final IRecord... recs) {
		return REC3(Arrays.stream(recs), REC());
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 示例:<br>
	 * REC3(L(REC("a/b/c",123,"a/b/d",456),REC("e/f",7,"g",8))).dfs_forone((p,v)->{
	 * <br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。<br>
	 * 
	 * <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 示例代码: <br>
	 * final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98)); <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * <br>
	 * 
	 * @param recs IRecord记录 集合 <br>
	 *             initrec 初始化 record 默认为 REC
	 * @return IRecord
	 */
	static IRecord REC3(final List<IRecord> recs) {
		return REC3(recs.stream(), REC());
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 示例:<br>
	 * var rec = REC("a/b/c",123,"a/b/d",456,"e/f",7,"g",8); <br>
	 * REC3(rec,REC()).dfs_forone((p,v)-&gt;{ <br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * 
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。<br>
	 * 
	 * <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 示例代码: <br>
	 * final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98)); <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * <br>
	 * 
	 * 生成IRecord记录
	 * 
	 * @param recs    IRecord记录集合流
	 * @param initrec 初始化 record
	 * @return IRecord
	 */
	static IRecord REC3(final List<IRecord> recs, final IRecord initrec) {
		return REC3(recs.stream(), initrec);
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 示例:<br>
	 * var rec = REC("a/b/c",123,"a/b/d",456,"e/f",7,"g",8); <br>
	 * REC3(rec,REC()).dfs_forone((p,v)-&gt;{ <br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * 
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。<br>
	 * 
	 * <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 示例代码: <br>
	 * final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98)); <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * <br>
	 * 
	 * 生成IRecord记录
	 * 
	 * @param recsStream IRecord记录
	 * @return 层级结构的IRecord
	 */
	static IRecord REC3(final Stream<IRecord> recsStream) {
		return REC3(recsStream, REC(), null);
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 示例:<br>
	 * var rec = REC("a/b/c",123,"a/b/d",456,"e/f",7,"g",8); <br>
	 * REC3(rec,REC()).dfs_forone((p,v)-&gt;{ <br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * 
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。<br>
	 * 
	 * <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 示例代码: <br>
	 * final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98)); <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * <br>
	 * 
	 * 生成IRecord记录
	 * 
	 * @param recsStream IRecord记录
	 * @param initrec    初始化 record
	 * @return 层级结构的IRecord
	 */
	static IRecord REC3(final Stream<IRecord> recsStream, final IRecord initrec) {
		return REC3(recsStream, initrec, null);
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 示例:<br>
	 * var rec = REC("a/b/c",123,"a/b/d",456,"e/f",7,"g",8); <br>
	 * REC3(rec,REC()).dfs_forone((p,v)-&gt;{ <br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * 
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * System.out.println(p + "---&gt;" + v); <br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。<br>
	 * 
	 * <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 
	 * 示例代码: <br>
	 * final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98)); <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * <br>
	 * 
	 * 生成IRecord记录
	 * 
	 * @param recsStream IRecord记录
	 * @param initrec    初始化 record
	 * @param pattern    key的路径分解的 分隔符的模式pattern,默认(空值null)的 分隔模式pattern标识为
	 *                   "[\\s,/\\.\\\\]+"
	 * @return 层级结构的IRecord
	 */
	static IRecord REC3(final Stream<IRecord> recsStream, final IRecord initrec, final String pattern) {
		return HIERACHIES(recsStream, initrec, pattern == null ? "/" : pattern);
	}

	/**
	 * 把一个扁平结构的 recs 转换成一个层级结构的IRecord <br>
	 * 示例:<br>
	 * var rec = REC("a/b/c",123,"a/b/d",456,"e/f",7,"g",8); <br>
	 * REC3(rec,REC()).dfs_forone((p,v)-&gt;{ <br>
	 * System.out.println(p+"---&gt;"+v); <br>
	 * });<br>
	 * 结构如下:<br>
	 * /g---&gt;8 <br>
	 * /a/b/c---&gt;123 <br>
	 * /a/b/d---&gt;456 <br>
	 * /e/f---&gt;7 <br>
	 * 
	 * <br>
	 * 注意:<br>
	 * 不同Record 之间的同名键名 会彼此覆盖:比如 REC3(REC("a",123,"a/b/c",234)). <br>
	 * REC3(REC("a",123,"a/b/c",234)).dfs_forone((p, v) -&gt; { <br>
	 * System.out.println(p + "---&gt;" + v); <br>
	 * }); <br>
	 * 会只生成 /a/b/c---&gt;234 一条记录。原因是 "a"的123被"a/b/c" 给覆盖掉了。<br>
	 * a/b/c 表示 a是一个复合结构，因此 原来的 单值 123,就被替换换。<br>
	 * 即 一个层级节点：只能要么是 复合节点，要么是单值节点 不能二者兼具。<br>
	 * 
	 * <br>
	 * REC3(REC("a",1),REC("a",2)) 会把 同名的key 合并在一起:值别拼接到一个数组中。即 a:[1,2] <br>
	 * 示例代码: final var s = S(REC("a/b/c",123,"a",98),REC("a/b/c",123,"a/x",98));
	 * <br>
	 * System.out.println(HIERACHIES(s,REC(),"/")); <br>
	 * 输出:a:x:98 b:c:[123, 123] <br>
	 * 
	 * 生成IRecord记录
	 * 
	 * @param recsStream IRecord记录
	 * @param initrec    初始化 record
	 * @param pattern    路径元素的分隔符
	 * @return 层级结构的IRecord
	 */
	static IRecord HIERACHIES(final Stream<IRecord> recsStream, final IRecord initrec, final String pattern) {

		final var path2recs = new HashMap<String, Stream<IRecord>>(); // 路径 -> IRecord 集合
		// 把 同名key 进行 进行分组,用于 IRecord 的compute 计算。
		final Function<KVPair<String, Object>, BiFunction<String, Object, Object>> kvp_remapper = (kvp) -> {
			return (k, v) -> {
				if (v == null)
					return kvp.value();
				@SuppressWarnings("unchecked")
				final List<Object> vv = v instanceof List ? (List<Object>) v // 把单元素的值变成列表形式的值
						: Stream.of(v).collect(Collectors.toList()); // 转换成列表形式的值
				vv.add(kvp.value()); // 加入新的kvp元素
				return vv;// 返回合并之后的结果
			}; // (k,v)->{}
		};// kvp_remapper

		recsStream.flatMap(IRecord::tupleS)// 把recsStream分解成 KVPair 序列流。
				// 把键key字段分解成2部分键名序列
				.map(e -> KVPair.split(e.key(KVPair
						// 第一部分只有一个元素的集合,第二部分为剩余的元素的集合
						.mkll(pattern)), 0).map((kk1, kk2) -> KVPair.KVP(JOIN(kk1, "/"), // 第一部分键名 连接成路径
								KVPair.KVP(JOIN(kk2, "/"), // 第二部分键名连接成路径
										e.value()) // 键值
				)) // mkll
				).collect(Collectors.groupingBy(KVPair::key)) // 按照第一部分键名进行分组:KVPair 进行阶层分组。
				.forEach((pathKey, pathValues) -> {// key, 键值对集合
					final var kvps = pathValues.stream().map(pathValue -> {
						final var kvp = pathValue.value();
						if (kvp.key().matches("\\s*")) {// key 单值节点
							initrec.compute(pathKey, kvp_remapper.apply(kvp));// 和多个键值合并到一块。
							return null; // 单值节点不予进行递归处理。
						} else {// key 为复合节点
							return pathValue;
						} // if
					}).filter(Objects::nonNull) // 滤除掉 空值
							.map(e -> KVPair.KVP(e._2()._1(), e._2()._2()));

					final var rec = REC();// 新的节点
					kvps.forEach(kvp -> {// 合并同名键
						rec.compute(kvp.key(), kvp_remapper.apply(kvp));// 和多个键值合并到一块。
					}); // forEach

					// 对于非空rec给予继续分解调用
					// 把具有共同的 path路径的字阶层IRecord(KVPair集合) 归拢起来,合并入path2recs
					if (!rec.isEmpty())
						path2recs.merge(pathKey, Stream.of(rec), Stream::concat); // 合并具相同路径阶层路径path的IRecord归并到一处。
				});// forEach

		path2recs.forEach((path, rr) -> {
			initrec.add(path, REC());// 为键名k,添加一个占位符对象，空的IRecord,通过递归进行更深层次的键值设置
			HIERACHIES(rr, (IRecord) initrec.get(path), "/");// 递归步进
		});// forEach

		return initrec;// 返回设置结果
	}

	/**
	 * 把 一个 KVPair集合进行层次合并。注意当T为Character类型的时候。返回的结构就构成了一个Trie结构。
	 * 
	 * @param <T>        路径键名序列
	 * @param kvplStream 路径键-值 对儿 集合 的 列表 的 流 [[([t],u)]]
	 * @return KVPair集合进行层次合并
	 */
	static <T> Map<T, Object> KVPSMERGE(final Stream<List<KVPair<List<T>, Object>>> kvplStream) {
		return KVPSMERGE(kvplStream, (T) null);
	}

	/**
	 * 把 一个 KVPair集合进行层次合并，注意当T为Character类型的时候。返回的结构就构成了一个Trie结构。
	 * 
	 * @param <T>  路径（键名) 元素的类型
	 * @param <U>  路径（键名）所对应的 键值 类型
	 * @param kvps 路径键-值 对儿 集合 的 流 [([t],u)]
	 * @return KVPair集合进行层次合并
	 */
	static <T, U> Map<T, Object> KVPSMERGE2(final Stream<KVPair<List<T>, U>> kvps) {
		@SuppressWarnings("unchecked")
		final List<KVPair<List<T>, Object>> kvpl = (List<KVPair<List<T>, Object>>) (Object) kvps
				.collect(Collectors.toList());
		return KVPSMERGE2(kvpl);
	}

	/**
	 * 把 一个 KVPair集合进行层次合并，注意当T为Character类型的时候。返回的结构就构成了一个Trie结构。
	 * 
	 * @param <T>   路径（键名) 元素的类型
	 * @param <U>   路径（键名）所对应的 键值 类型
	 * @param kvpll 路径键-值 对儿 集合 的 列表 的 列表 [[([t],u)]]
	 * @return KVPair集合进行层次合并
	 */
	static <T, U> Map<T, Object> KVPSMERGE(final Iterable<List<KVPair<List<T>, U>>> kvpll) {
		@SuppressWarnings("unchecked")
		final var stream = ((Stream<List<KVPair<List<T>, Object>>>) (Object) StreamSupport.stream(kvpll.spliterator(),
				false));
		return KVPSMERGE(stream, (T) null);
	}

	/**
	 * 把 一个 KVPair集合进行层次合并，注意当T为Character类型的时候。返回的结构就构成了一个Trie结构。
	 * 
	 * @param <T>  路径（键名) 元素的类型
	 * @param <U>  路径（键名）所对应的 键值 类型
	 * @param kvpl 路径键-值 对儿 集合 的 列表 [([t],u)]
	 * @return KVPair集合进行层次合并
	 */
	static <T, U> Map<T, Object> KVPSMERGE2(final Iterable<KVPair<List<T>, U>> kvpl) {
		@SuppressWarnings("unchecked")
		final var stream = (Stream<List<KVPair<List<T>, Object>>>) (Object) StreamSupport.stream(kvpl.spliterator(),
				false);
		return KVPSMERGE(stream, (T) null);
	}

	/**
	 * 把 一个 KVPair集合进行层次合并，注意当T为Character类型的时候。返回的结构就构成了一个Trie结构。
	 * 
	 * @param <T>        路径（键名) 元素的类型
	 * @param kvplStream 键值对儿 集合 流
	 * @param nullkey    空键值名的 默认值
	 * @return KVPair集合进行层次合并
	 */
	static <T> Map<T, Object> KVPSMERGE(final Stream<List<KVPair<List<T>, Object>>> kvplStream, final T nullkey) {
		final var ret = new HashMap<T, Object>();// 返回值

		kvplStream.flatMap(Collection::stream).collect(Collectors.groupingBy(e -> e.key().size() < 1 ? nullkey // 默认空值对应的
																												// key
				: e.key().get(0))) // 根据第一层元素进行分组
				.forEach((key, kvps) -> {
					kvps.stream().map(e -> // 即path2vlaue的层级结构进行 阶层分析。
					KVPair.KVP( // 阶层结构分解 提取 子阶层
							e.key().size() < 1 ? e.key() // 空列表
									: e.key().subList(1, e.key().size()), // 次级阶层结构的路径
							e.value() // 次级阶层 结构的值
					)// KVP
					).collect(Collectors.groupingBy(e -> e.key().size())) // 次级阶层结构分组
							.forEach((length, vv) -> { // 次级阶层 结构处理
								if (length == 0) { // 单值元素,次级阶层长度为0
									final var ll = vv.stream().map(Tuple2::_2).collect(Collectors.toList());
									ret.put(key, ll.size() == 1 ? ll.get(0) : ll);
								} else {// 次级阶层长度不为0
									ret.put(key, KVPSMERGE(Stream.of(vv)));
								} // if length
							});// forEach length,vv
				});// forEach key,kvps

		return ret;
	}

	/**
	 * 把 一个 KVPair集合进行层次合并
	 * 
	 * @param kvplStream 键值对儿 集合 流
	 * @return KVPair集合进行层次合并
	 */
	static Map<String, Object> RECMERGE(final Stream<IRecord> kvplStream) {
		return RECMERGE(kvplStream, null);
	}

	/**
	 * 把 一个 KVPair集合进行层次合并
	 * 
	 * @param kvplStream 键值对儿 集合 流
	 * @param pattern    路径分隔符号，默认(空值)为 "/",注意 pattern 需要采用正则表达式的形式 对于
	 *                   $,|,^等符号需要给予进行转义。
	 * @return KVPair集合进行层次合并
	 */
	static Map<String, Object> RECMERGE(final Stream<IRecord> kvplStream, final String pattern) {
		final Function<IRecord, List<KVPair<List<String>, Object>>> mapper = rec -> rec.tupleS()
				.map(p -> KVPair.KVP(p.key(line -> {
					final var regex = pattern == null ? "/" : pattern; // 路径元素分隔方式。
					return Arrays.asList(line.split(regex));
				}), // 阶层路径元素 列表
						p.value() // 阶层
				)).collect(Collectors.toList());
		return KVPSMERGE(kvplStream.map(mapper));
	}

	/**
	 * 把 一个 KVPair集合进行层次合并
	 * 
	 * @param records 键值对儿 集合 流
	 * @return KVPair集合进行层次合并
	 */
	static Map<String, Object> RECMERGE(final IRecord... records) {
		return RECMERGE(Stream.of(records));
	}

	/**
	 * 把 一个 KVPair集合进行层次合并
	 * 
	 * @param map 待转换的键值列表
	 * @return KVPair集合进行层次合并
	 */
	@SuppressWarnings("unchecked")
	static IRecord MAP2REC(final Map<String, Object> map) {
		final var rec = REC(map);
		map.keySet().forEach(
				key -> rec.computeIfPresent(key, v -> v instanceof Map ? MAP2REC((Map<String, Object>) v) : v));
		return rec;
	}

	/**
	 * 把 一个 KVPair集合进行层次合并 REC3的另一种实现方式<br>
	 * 
	 * eg:<br>
	 * final var rec = RECMERGE2(REC("a", 4, "a/b/c", 234), REC("a/b/d", 567),
	 * REC("a/b/d", 567)); <br>
	 * 返回:<br>
	 * a:b:c:234 d:[567, 567]<br>
	 * 
	 * @param records 键值对儿 集合 流
	 * @return KVPair集合进行层次合并
	 */
	static IRecord RECMERGE2(final IRecord... records) {
		return MAP2REC(RECMERGE(Stream.of(records)));
	}

	/**
	 * 把 一个 KVPair集合进行层次合并 REC3的另一种实现方式
	 * 
	 * eg:<br>
	 * final var rec = RECMERGE2(REC("a", 4, "a/b/c", 234), REC("a/b/d", 567),
	 * REC("a/b/d", 567)); <br>
	 * 返回:<br>
	 * a:b:c:234 d:[567, 567]<br>
	 * 
	 * 数据透视表功能演示: <br>
	 * final var digits = "0123456789".split(""); <br>
	 * final var ai = new AtomicInteger(0); <br>
	 * Elem.cph2(asList(digits,digits,digits,digits,digits)).map(IRecord::L2REC)
	 * <br>
	 * .map(path-&gt;REC(JOIN(path.ss().subList(0, 4)), ai.getAndIncrement() )) //
	 * 生成一个 path2value结构,这是一种阶层结构的方法方法 <br>
	 * .collect(llclc(IRecord::RECMERGE2)) // 阶层化 Record 集合 <br>
	 * .dfs_forall2(System.out::println); <br>
	 * 
	 * @param records 键值对儿 集合 流
	 * @return KVPair集合进行层次合并
	 */
	static IRecord RECMERGE2(final Iterable<IRecord> records) {
		return MAP2REC(RECMERGE(StreamSupport.stream(records.spliterator(), false)));
	}

	/**
	 * 把 一个 KVPair集合进行层次合并 REC3的另一种实现方式,其实 一个 RECMERGE2 结合 llclc 可以实现 数据透视表的功能。
	 * 
	 * eg:<br>
	 * final var rec = RECMERGE2(REC("a", 4, "a/b/c", 234), REC("a/b/d", 567),
	 * REC("a/b/d", 567)); <br>
	 * 返回:<br>
	 * a:b:c:234 d:[567, 567]<br>
	 * 
	 * 数据透视表功能演示: <br>
	 * final var digits = "0123456789".split(""); <br>
	 * final var ai = new AtomicInteger(0); <br>
	 * Elem.cph2(asList(digits,digits,digits,digits,digits)).map(IRecord::L2REC)
	 * <br>
	 * .map(path->REC(JOIN(path.ss().subList(0, 4)), ai.getAndIncrement() )) // 生成一个
	 * path2value结构,这是一种阶层结构的方法方法 <br>
	 * .collect(llclc(IRecord::RECMERGE2)) // 阶层化 Record 集合 <br>
	 * .dfs_forall2(System.out::println);<br>
	 * 
	 * @param records 键值对儿 集合 流
	 * @param pattern 路径分隔符号，默认(空值)为 "/"
	 * @return KVPair集合进行层次合并
	 */
	static IRecord RECMERGE2(final Iterable<IRecord> records, String pattern) {
		return MAP2REC(RECMERGE(StreamSupport.stream(records.spliterator(), false), pattern));
	}

	/**
	 * 把 一个 KVPair集合进行层次合并 REC3的另一种实现方式
	 * 
	 * eg:<br>
	 * final var rec = RECMERGE2(REC("a", 4, "a/b/c", 234), REC("a/b/d", 567),
	 * REC("a/b/d", 567)); <br>
	 * 返回:<br>
	 * a:b:c:234 d:[567, 567]<br>
	 * 
	 * @param records 键值对儿 集合 流
	 * @return KVPair集合进行层次合并
	 */
	static IRecord RECMERGE2(final Stream<IRecord> records) {
		return MAP2REC(RECMERGE(records));
	}

	/**
	 * 把一个数组元素转换成一个IRecord对象 键值对序列为[key_i,tt[i]]
	 * 
	 * @param <T>   数组元素类型
	 * @param tt    数组元素对象
	 * @param t2key 键名变换函数：(i:索引序号从0开始,t:元素对象)->key 元素转换成key,<br>
	 *              把一个tt[i]->key_i,进而成为一个键值对儿[key_i,tt[i]]
	 * @return IRecord对象
	 */
	static <T> IRecord A2REC(final T[] tt, final BiFunction<Integer, T, String> t2key) {
		final var ai = new AtomicInteger(0);
		return REC(Stream.of(tt).flatMap(e -> Stream.of(t2key.apply(ai.getAndIncrement(), e), e)).toArray());
	}

	/**
	 * 把一个数组元素转换成一个IRecord对象 键值对序列为[key_i,tt[i]]
	 * 
	 * @param <T>   数组元素类型
	 * @param tt    数组元素对象
	 * @param t2key 元素转换成key,把一个tt[i]->key_i,进而成为一个键值对儿[key_i,tt[i]]
	 * @return IRecord对象
	 */
	static <T> IRecord A2REC(final T[] tt, final Function<T, String> t2key) {
		return REC(Stream.of(tt).flatMap(e -> Stream.of(t2key.apply(e), e)).toArray());
	}

	/**
	 * 把一个数组元素转换成一个IRecord对象 键值对序列为[i,tt[i]],i从0开始
	 * 
	 * @param <T> 数组元素类型
	 * @param tt  数组元素对象
	 * @return IRecord对象
	 */
	static <T> IRecord A2REC(final T[] tt) {
		final var aint = new AtomicInteger(0);
		final Function<Object, String> t2key = o -> aint.getAndIncrement() + "";
		return REC(Stream.of(tt).flatMap(e -> Stream.of(t2key.apply(e), e)).toArray());
	}

	/**
	 * A2REC 的别名 把一个数组元素转换成一个IRecord对象 键值对序列为[i,tt[i]],i从0开始
	 * 
	 * @param <T> 数组元素类型
	 * @param tt  数组元素的序列
	 * @return IRecord对象
	 */
	@SafeVarargs
	static <T> IRecord A2R(final T... tt) {
		return A2REC(tt);
	}

	/**
	 * A2REC 的别名: example, 用来做 制作一个 概念示例。<br>
	 * 比如 EG(1,"张三","上海",198).keys("id,name,address") 就构建了一个 用户概念。<br>
	 * 把一个数组元素转换成一个IRecord对象 键值对序列为[i,tt[i]],i从0开始
	 * 
	 * @param <T> 数组元素类型
	 * @param tt  数组元素的序列
	 * @return IRecord对象
	 */
	@SafeVarargs
	static <T> IRecord EG(final T... tt) {
		return A2REC(tt);
	}

	/**
	 * 用一个IRecord 来表示一个点数据Point，键名为"0" 把一个对象转换成Object: Pair:{0,Object}
	 * 
	 * @param obj 数据对象
	 * @return 键名为"0" 的IRecord
	 */
	static IRecord P(Object obj) {
		return REC("0", obj);// 生成默认键名为0
	}

	/**
	 * 解析一个字符串数据，把它拆分成字符串数组 然后把该数组元素转换成一个IRecord对象 键值对序列为[i,tt[i]],i从0开始
	 * 
	 * 例如：STRING2REC("1,2,3,4","[,/\\\\]+") 生成 {0:1,1:2,2:3,3:4}
	 * 
	 * @param line  带解析的数据行
	 * @param delim 分隔符,分隔符好的正则表达式。比如 "[,/\\\\]+" 表示 使用 “,” “/” “\” 作为分隔符号。
	 * @return 解析行的line而得到的IRecord
	 */
	static IRecord STRING2REC(final String line, final String delim) {
		return A2REC(line.split(delim));
	}

	/**
	 * 解析一个字符串数据，把它拆分成字符串数组<br>
	 * 然后把该数组元素转换成一个IRecord对象<br>
	 * 键值对序列为[i,tt[i]],i从0开始<br>
	 * 
	 * 例如：STRING2REC("1,2,3,4","[,/\\\\]+") 生成 {0:1,1:2,2:3,3:4}<br>
	 * 
	 * 默认的delim "[,/\\\\]+"<br>
	 * 
	 * @param line 带解析的数据行,line 会被 String.valueOf 强制转为 字符串
	 * @return 解析行的line而得到的IRecord
	 */
	static IRecord STRING2REC(final Object line) {
		return A2REC(String.valueOf(line).split("[,/\\\\]+"));
	}

	/**
	 * 把一个数组元素转换成一个IRecord对象<br>
	 * 键值对序列为[key_i,tt[i]] <br>
	 * 
	 * @param <T>   数组元素类型
	 * @param ss    数组元素对象
	 * @param s2key 元素转换成key,把一个tt[i]->key_i,进而成为一个键值对儿[key_i,tt[i]]
	 * @return IRecord对象
	 */
	static <T> IRecord STREAM2REC(final Stream<T> ss, final Function<T, String> s2key) {
		return REC(ss.flatMap(e -> Stream.of(s2key.apply(e), e)).toArray());
	}

	/**
	 * 把一个元素流转换成一个IRecord对象 键值对序列为[i,tt[i]],i从0开始
	 * 
	 * @param <T> 数组元素类型
	 * @param ss  数组元素对象
	 * @return IRecord对象
	 */
	static <T> IRecord STREAM2REC(final Stream<T> ss) {
		final var aint = new AtomicInteger(0);
		final Function<Object, String> t2key = o -> aint.getAndIncrement() + "";
		return REC(ss.flatMap(e -> Stream.of(t2key.apply(e), e)).toArray());
	}

	/**
	 * 把一个列表转换成一个IRecord对象 键值对序列为[key_i,tt[i]]
	 * 
	 * @param <T>   数组元素类型
	 * @param ll    列表元素对象
	 * @param t2key 元素转换成key,把一个tt[i]->key_i,进而成为一个键值对儿[key_i,tt[i]]
	 * @return IRecord对象
	 */
	static <T> IRecord L2REC(final List<T> ll, final Function<T, String> t2key) {
		return REC(ll.stream().flatMap(e -> Stream.of(t2key.apply(e), e)).toArray());
	}

	/**
	 * 把一个列表转换成一个IRecord对象, 键值对序列为[i,tt[i]],i从0开始
	 * 
	 * @param <T> 数组元素类型
	 * @param ll  数组元素对象:
	 * @return IRecord对象
	 */
	static <T> IRecord L2REC(final List<T> ll) {
		final var aint = new AtomicInteger(0);
		final Function<Object, String> t2key = o -> aint.getAndIncrement() + "";
		return REC(ll.stream().flatMap(e -> Stream.of(t2key.apply(e), e)).toArray());
	}

	/**
	 * 把一个KVS列表转换成一个IRecord对象,
	 * 
	 * @param kvs 键值列表
	 * @return IRecord对象
	 */
	static <T> IRecord KVS2REC(final Iterable<? extends KVPair<String, T>> kvs) {
		return KVS2REC(StreamSupport.stream(kvs.spliterator(), false));
	}

	/**
	 * 把一个KVS列表转换成一个IRecord对象,
	 * 
	 * @param kvsStream 键值列表的流
	 * @return IRecord对象
	 */
	static <T> IRecord KVS2REC(final Stream<? extends KVPair<String, T>> kvsStream) {
		final IRecord rec = REC();
		if (kvsStream != null)
			kvsStream.forEach(kvp -> rec.add(kvp.key(), kvp.value()));
		return rec;
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @param keys 字段键名 如果为null, 采用recs 的一个元素的键名集合。
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord ROWS2COLS(final List<IRecord> recs, final List<String> keys) {
		if (recs == null || recs.size() < 1)
			return REC();
		final var first = recs.stream().filter(Objects::nonNull).findFirst().orElse(null);
		if (first == null)
			return REC();
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
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record流
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord ROWS2COLS(List<IRecord> recs) {
		return ROWS2COLS(recs, null);
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @param keys 字段键名 如果为null, 采用recs 的一个元素的键名集合。
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord ROWS2COLS(final Stream<IRecord> recs, final List<String> keys) {
		return ROWS2COLS(recs.collect(Collectors.toList()), null);
	}

	/**
	 * 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record流
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord ROWS2COLS(Stream<IRecord> recs) {
		return ROWS2COLS(recs, null);
	}

	/**
	 * ROWS CoLleCt 行归集器 函数<br>
	 * 把IRecord视为一个DFrame dfm,而后 把dfm转换成个一个行流，最后使用 collector进行归集。
	 * 
	 * @param <R>       结果类型
	 * @param collector 行归集器
	 * @return 一个 rec->R 的函数
	 */
	static <R> Function<IRecord, R> ROWSCLC(final Collector<IRecord, List<IRecord>, R> collector) {
		return rec -> rec.rowS().collect(collector);
	}

	/**
	 * ROWS2COLS 的别名 行形式转换成列形式。即每一元素为列向量的IRecord
	 * 
	 * @param recs 行形式的Record集合
	 * @return IRecord 即每一元素为列向量的IRecord
	 */
	static IRecord R2C(final List<IRecord> recs) {
		return ROWS2COLS(recs, null);
	}

	/**
	 * 标准版的记录生成器, map 生成的是LinkedRecord
	 * 
	 * @param values:key1,value1,key2,value2,....
	 * @return IRecord对象
	 */
	static IRecord R(final Object... values) {
		return REC(values);
	}

	/**
	 * 制作一个列表
	 * 
	 * @param <T> tt 的元素类型
	 * @param tt  列表元素
	 * @return List&lt;T&gt;
	 */
	@SafeVarargs
	static <T> List<T> L(final T... tt) {
		return Arrays.asList(tt);
	}

	/**
	 * 制作一个流
	 * 
	 * @param <T> tt 的元素类型
	 * @param tt  列表元素
	 * @return Stream&lt;T&gt;
	 */
	@SafeVarargs
	static <T> Stream<T> S(final T... tt) {
		return Stream.of(tt);
	}

	/**
	 * 制作一个数组
	 * 
	 * @param <T> tt的元素类型
	 * @param tt  列表元素：当TT中的元素异质接类型的时候，比如A(1,1d,1l) T被视作Object类型。
	 * @return T类型数组
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] A(final T... tt) {
		final var classes = Stream.of(tt).collect(Collectors.groupingBy(e -> e == null ? Object.class : e.getClass()))
				.keySet();// 提取所有类别
		final Function<Collection<Class<?>>, Class<?>> num_predicate = cc -> {
			if (cc.size() > 1) {
				var b = cc.stream().allMatch(Number.class::isAssignableFrom);// 判断是否是数值类型
				if (b)
					return Number.class;// 数值类型
				b = cc.stream().allMatch(Serializable.class::isAssignableFrom);// 判断是否是数值类型
				if (b)
					return Serializable.class;// 其他类型采用Serializable
				return Object.class;
			} else if (cc.size() == 1) {
				return cc.iterator().next();
			} // if

			return Object.class;
		};// num_predicate

		final var componentType = classes.size() == 1 ? (Class<T>) (Object) classes.iterator().next()
				: (Class<T>) (Object) num_predicate.apply(classes);
		return Arrays.asList(tt).toArray(n -> (T[]) Array.newInstance(componentType, n));
	}

	/**
	 * 制作一个数组:在 tt 前面插入一个元素t
	 * 
	 * @param <T> tt的元素类型
	 * @param t   列表元素
	 * @param tt  列表元素
	 * @return T[]
	 */
	static <T> T[] A(T t, T[] tt) {
		return CONS(t, tt);
	}

	/**
	 * 制作一个数组:在tt 后面插入一个元素t
	 * 
	 * @param <T> tt的元素类型
	 * @param tt  列表元素
	 * @param t   列表元素
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] A(T[] tt, T t) {
		final var dd = new LinkedList<T>();
		dd.add(t);
		dd.addAll(Arrays.asList(tt));
		Class<T> componentType = dd.stream().filter(Objects::nonNull).map(e -> (Class<T>) e.getClass()).findAny()
				.orElse((Class<T>) Object.class);
		return dd.toArray(n -> (T[]) Array.newInstance(componentType, n));
	}

	/**
	 * 制作一个数组:在 tt 前面插入一个元素t
	 * 
	 * @param <T> tt1,tt2 的 元素类型
	 * @param tt1 列表元素数组1
	 * @param tt2 列表元素数组2
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] A(T[] tt1, T[] tt2) {
		final var dd = new LinkedList<T>();
		if (tt1 != null)
			dd.addAll(Arrays.asList(tt1));
		if (tt2 != null)
			dd.addAll(Arrays.asList(tt2));
		Class<T> componentType = dd.stream().filter(Objects::nonNull).map(e -> (Class<T>) e.getClass()).findAny()
				.orElse((Class<T>) Object.class);
		return dd.toArray(n -> (T[]) Array.newInstance(componentType, n));
	}

	/**
	 * FixedArray 制作一个数组:定长数组:size 为数组长度,数组不足采用zeros进行补充,多余数据给予删除
	 * 
	 * @param <T>   元素类型
	 * @param tt    列表元素数组
	 * @param zeros 空值序列:会会采用循环补填的方式进行数据填充。
	 * @param size  数组长度
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] FA(final T[] tt, final T[] zeros, final int size) {
		final var dd = new LinkedList<T>();
		if (tt != null)
			dd.addAll(Arrays.asList(tt));
		if (dd.size() > size) {
			while (dd.size() > size)
				dd.removeLast(); // 去除掉尾部的多余数据
		} else if (dd.size() < size) {
			// 使用zeros 进行数据补填
			for (int i = tt == null ? 0 : tt.length; i < size; i++)
				dd.add(zeros == null ? null : zeros[i % zeros.length]);
		} // if

		final Class<T> componentType = dd.stream().filter(Objects::nonNull).map(e -> (Class<T>) e.getClass()).findAny()
				.orElse((Class<T>) Object.class);

		return dd.toArray(n -> (T[]) Array.newInstance(componentType, n));
	}

	/**
	 * FixedArray 制作一个数组:定长数组:size 为数组长度,数组不足采用zeros进行补充,多余数据给予删除
	 * 
	 * @param <T>   元素类型
	 * @param tt    列表元素数组
	 * @param zeros 空值序列:会会采用循环补填的方式进行数据填充。
	 * @param size  数组长度
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] FA(final T[] tt, final T zeros, final int size) {
		return FA(tt, A(zeros), size);
	}

	/**
	 * 提取数组的第一个元素,术语来自于 LISP
	 * 
	 * @param <T> 数组元素的类型
	 * @param tt  数组
	 * @return T 数组元素
	 */
	static <T> T CAR(final T[] tt) {
		return (tt == null || tt.length < 1) ? null : tt[0];
	}

	/**
	 * 提取数组除了第一个元素之外的其他元素,术语来自于 LISP
	 * 
	 * @param <T> 数组元素的类型
	 * @param tt  数组元素
	 * @return T[] 子数组
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] CDR(final T[] tt) {
		if (tt.length < 2)
			return null;
		var ll = Arrays.asList(tt);
		var tclazz = ll.stream().filter(Objects::nonNull).map(e -> (Class<T>) e.getClass()).findAny().orElse(null);
		if (tclazz == null)
			return null;
		return ll.subList(1, tt.length).toArray(n -> (T[]) Array.newInstance(tclazz, n));
	}

	/**
	 * 制作一个数组:在 tt 前面插入一个元素t,术语来源于 lisp
	 * 
	 * @param <T> 数组元素类型
	 * @param t   列表元素
	 * @param tt  元素列表
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	static <T> T[] CONS(T t, T[] tt) {

		if (t != null && tt == null) {
			T[] aa = (T[]) Array.newInstance(t.getClass(), 1);
			aa[0] = t;
			return aa;
		} else {
			if (tt == null && t == null) {
				return null;
			} else {
				if (tt == null || tt.length < 1)
					return Collections.singletonList(t).toArray(n -> (T[]) Array.newInstance(t.getClass(), n));// 生成一个单元素数组
				final var dd = Stream.of(Stream.of(t), Arrays.stream(tt)).flatMap(e -> e).collect(Collectors.toList());
				Class<T> componentType = dd.stream().filter(Objects::nonNull).map(e -> (Class<T>) e.getClass())
						.findAny().orElse((Class<T>) Object.class);
				return dd.toArray(n -> (T[]) Array.newInstance(componentType, n));
			}
		} // if
	}

	/**
	 * 数组进行倒转。
	 * 
	 * @param <T> 数据元素类型
	 * @param tt  列表元素
	 * @return T[]
	 */
	static <T> T[] REV(T[] tt) {
		if (tt == null)
			return null;
		return Jdbcs.reverse(tt);
	}

	/**
	 * 制作一个Map&lt;String,Object&gt;
	 * 
	 * @param tt 列表元素
	 * @return Map&lt;String,Object&gt;
	 */
	static Map<String, Object> M(final Object... tt) {
		return _REC(tt).toMap();
	}

	/**
	 * 等价函数
	 */
	@SuppressWarnings("unchecked")
	static <T> Function<Object, T> identity(final Class<T> tcls) {
		return (Object o) -> (T) o;
	}

	/**
	 * 把一个 IRecord节点 转换成 一个 树形结构 Node &lt; String &gt;
	 * 
	 * @param rec      IRecord 对象
	 * @param rootName 根节点名称 , 默认值(null) 为 "root"
	 * @param valuekey IRecord KVPair的 value 值在 Node 的props 中的键值名,默认值 为 "value"
	 * @return Node &lt; String &gt;
	 */
	static Node<String> TREENODE(final IRecord rec, final String rootName, final String valuekey) {

		final var finalvk = valuekey == null ? "value" : valuekey;
		final var finalrn = rootName == null ? "root" : rootName;

		// BiConsumer
		final class BCS implements BiConsumer<String, Object> {

			// 构造函数
			public BCS(Node<String> parent) {
				this.parent2value = KVPair.KVP(parent, null);
			}

			@Override
			public void accept(final String name, final Object value) {
				final var node = new Node<>(name);// 创建当前节点
				this.parent2value.key().addChild(node);
				this.parent2value.value(value);

				if (value instanceof IRecord)
					((IRecord) value).foreach(new BCS(node)); // 节点关系处理
				else
					node.attrSet(finalvk, value);
			}

			private final KVPair<Node<String>, Object> parent2value;// 上级节点 与 当前节点
		} // BCS

		final var root = new Node<>(finalrn);
		rec.foreach(new BCS(root));

		return root;
	}

	/**
	 * 行化操作：数据分析类 需要与DataMatrix 相结合生成 data.frame类型的 转换函数
	 * 
	 * rr:Collection&lt;IRecord&gt; recs 记录集合 row:Map 行记录 S:结果类型为Stream 主要用途就是 完成
	 * IRecord 向 DataMatrix的转换，但是为了保证DataMatrix 与IRecord 的独立。而设置这个函数。比如 var dm = new
	 * DataMatrix&lt;&gt; (rr2rowS(recs),Integer.class); 就构造了一个 DataMatrix 对象。
	 * 
	 * @param recs record 集合
	 * @return 生成一个hashmap 的集合
	 */

	static <T> Stream<LinkedHashMap<String, T>> rr2rowS(final Iterable<IRecord> recs) {
		return rr2rowS(StreamSupport.stream(recs.spliterator(), false));
	}

	/**
	 * 行化操作：数据分析类 需要与DataMatrix 相结合生成 data.frame类型的 转换函数
	 * 
	 * rr:Collection&lt;IRecord&gt; recs 记录集合 row:Map 行记录 S:结果类型为Stream 主要用途就是 完成
	 * IRecord 向 DataMatrix的转换，但是为了保证DataMatrix 与IRecord 的独立。而设置这个函数。比如 var dm = new
	 * DataMatrix&lt;&gt; (rr2rowS(recs),Integer.class); 就构造了一个 DataMatrix 对象。
	 * 
	 * @param stream records 的集合
	 * @return 生成一个hashmap 的集合
	 */
	@SuppressWarnings("unchecked")
	static <T> Stream<LinkedHashMap<String, T>> rr2rowS(final Stream<IRecord> stream) {
		return (Stream<LinkedHashMap<String, T>>) (Object) stream.map(IRecord::toLhm);
	}

	/**
	 * 行化操作：数据分析类 需要与DataMatrix 相结合生成 data.frame类型的 转换函数 r:record 单条记录 row:Map 行记录
	 * s:结果类型为Stream 主要用途就是 完成 IRecord 向 DataMatrix的转换，但是为了保证DataMatrix 与IRecord
	 * 的独立。而设置这个函数。比如 var dm = new DataMatrix&lt;&gt; (rr2rowS(recs),Integer.class);
	 * 就构造了一个 DataMatrix 对象。
	 * 
	 * @param rec 的单元素：但也把他视为只有一个元素的集合。
	 * @return 生成一个hashmap 的集合
	 */
	@SuppressWarnings("unchecked")
	static Stream<Map<String, ?>> r2rowS(final IRecord rec) {
		return (Stream<Map<String, ?>>) (Object) Stream.of(rec.toEscapedMap());
	}

	/**
	 * 一般用法就是对于一个 Records List,通过某种某种classifier:根分组成一个map 这是一个 List 转Map的方法。 需要注意
	 * classifier 需要保证不会把由多个元素给到相同组。所以classifier 一般就是Records 记录的候选主键：比如,id,name 之类
	 * 可以唯一标识 record的字段属性。
	 * 
	 * 比如: final var rr = jdbc.sql2records(sql); final var map =
	 * rr.stream().collect(Collectors.groupingBy(e-&gt;e.str("name"),// name 唯一标识e
	 * atomic_collector(e-&gt;e.get("cnt"))));// rr-&gt;map
	 * 
	 * 但值容器：即只保存一个值。 把一个T类型的元素给归约成一个U类型的单个元素。
	 * 
	 * @param t2u 把T类型转换成U类型的变换杉树
	 * @param <T> Stream 元素类型
	 * @param <U> t2u 生成的Value值
	 * @return U类型的目标值。
	 */
	static <T, U> Collector<T, AtomicReference<U>, U> atomic_collector(final Function<T, U> t2u) {
		return Collector.of(AtomicReference::new, (atomic, t) -> atomic.set(t2u.apply(t)), // 规约容器之
				(atomic_a, atomic_b) -> atomic_b, // 合并容器之
				AtomicReference::get);// 提取容器值
	}

	/**
	 * 用法示例：
	 * 
	 * final var rr = jdbc.sql2records(sql); <br>
	 * final var map =
	 * rr.stream().collect(groupby(e-&gt;e.str("name"),e-&gt;e.get("cnt"))); <br>
	 * 
	 * 制作一个分组器 把一个 Stream&lt;U&gt; 给份组成也成一个Map&lt;K,U&gt;
	 * 
	 * @param classifier T 类型的分类器，classifier 会申城一个键类型。
	 * @param t2u        把T类型给转换成U类型
	 * 
	 * @param <T>        Stream 元素类型
	 * @param <K>        classifier 生的K的类型
	 * @param <U>        t2u 生成的Value值。
	 * 
	 * @return Map&lt;K,U&gt;
	 */
	static <T, K, U> Collector<T, ?, Map<K, U>> groupby(final Function<T, K> classifier, final Function<T, U> t2u) {
		return Collectors.groupingBy(classifier, atomic_collector(t2u));
	}

	/**
	 * 对Stream&lt;T&gt; 中的元素进行 classifier 分组。
	 * 
	 * @param classifier 分组函数
	 * 
	 * @param <T>        Stream 元素类型
	 * @param <K>        classifier 生的K的类型
	 * 
	 * @return Map&lt;K,List&gt;K&gt;&gt;
	 */
	static <T, K> Collector<T, ?, Map<K, List<T>>> groupby(final Function<T, K> classifier) {
		return Collectors.groupingBy(classifier);
	}

	/**
	 * 使用示例 var s = ll.stream().map(e-&gt;e.toString()).collect(join("\n")); 把String
	 * 通过 delimiter 连接骑起来
	 * 
	 * @param delimiter 分隔符
	 * @return 用delimiter 分割的Stream&lt;String&gt;
	 */
	static Collector<CharSequence, ?, String> join(final CharSequence delimiter) {
		return Collectors.joining(delimiter);
	}

	/**
	 * 对 rr 按照key的值进行分类：即key 就是分类依据。
	 * 
	 * @param rr  一个records 列表
	 * @param key 列名：分类依据
	 * @return 对rr进行分类的映射:Map&lt;String,List&lt;IRecord&gt;&gt;
	 */
	static Map<String, List<IRecord>> classify(final Iterable<IRecord> rr, final String key) {
		var mm = StreamSupport.stream(rr.spliterator(), true).filter(Objects::nonNull).collect(groupby(e -> {
			var k = e.str(key);
			return k == null ? "unknown " : k;
		}));
		return mm;
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * @param <U>       分类结果的计算器 结果类型
	 * @param rr        待分类的数据集合。即源数据,null 或者长度为0返回一个空IRecord
	 * @param keys      分类的key列表：分类依据字段列表。或者说 分类层级的序列
	 * @param level     当前的处理节点的层级，从0开始。
	 * @param parent    分类的结果的保存位置
	 * @param evaluator 列指标：分类结果的计算器
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	static <U> IRecord pivotTable(final List<IRecord> rr, final String keys[], final int level, final IRecord parent,
			final Function<List<IRecord>, U> evaluator) {

		if (rr == null || rr.size() < 1)
			return REC();//
		final var final_keys = (keys == null || keys.length < 1)// 分类层级的序列无效,默认为全部分类
				? rr.get(0).keys().toArray(String[]::new) // 提取第一个元素的键值作为层级分类依据。
				: keys;// 分类层级依据
		final var key = final_keys[level];// 进行分类的 分析依据字段名。
		if (level < final_keys.length) {
			// 创建新节点
			classify(rr, key).forEach((k, sub_rr) -> {// 把rr 进行分类：分类型若个子分类
				if (level != keys.length - 1) {// 只要不是最后一个分类项就需要继续分类。
					final var node = REC();
					parent.add(k, node);// 生成一个中间分类节点。注意不记录分组数据。
					pivotTable(sub_rr, final_keys, level + 1, node, evaluator);// 对分类节点继续分类。
				} else {// 到达了分类层级的末端 ,即当前是最后一个分类的 key
					parent.add(k, evaluator.apply(sub_rr));// 最后一层记录分组数据
				} // if level
			});// classify 对 rr 按照key 的值进行分类：即key 就是分类依据。
		} // 阶层有效。

		return parent;// 返回计算计算
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * @param <U>       分类结果的计算器 结果类型
	 * @param rr        待分类的数据集合。即源数据
	 * @param keys      分类的key列表，分类依据字段列表。或者说 分类层级的序列
	 * @param evaluator 列指标：分类结果的计算器
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	static <U> IRecord pivotTable(final List<IRecord> rr, final String keys[],
			final Function<List<IRecord>, U> evaluator) {

		return pivotTable(rr, keys, 0, REC(), evaluator);
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个 记录集合的列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * @param <U>       分类结果的计算器 结果类型
	 * @param rr        待分类的数据集合。即源数据
	 * @param keys      分类的key列表，分类依据字段列表。或者说 分类层级的序列
	 * @param evaluator 列指标：分类结果的计算器
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	static <U> IRecord pivotTable2(final List<IRecord> rr, final String keys[],
			final Function<Stream<IRecord>, U> evaluator) {
		return pivotTable(rr, keys, 0, REC(), ee -> evaluator.apply(ee.stream()));
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个 记录集合的列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * @param <U>       分类结果的计算器 结果类型
	 * @param rr        待分类的数据集合。即源数据
	 * @param keys      分类的key列表，分类依据字段列表。或者说 分类层级的序列
	 * @param delim     分隔符
	 * @param evaluator 列指标：分类结果的计算器
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	static <U> IRecord pivotTable(final List<IRecord> rr, final String keys, String delim,
			final Function<Stream<IRecord>, U> evaluator) {
		return pivotTable(rr, keys.split(delim), 0, REC(), ee -> evaluator.apply(ee.stream()));
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个列表 rr：<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即
	 * 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * <br>
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * <br>
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * @param <U>       分类结果的计算器 结果类型
	 * @param rr        待分类的数据集合。即源数据
	 * @param keys      分类的key列表，分类依据字段列表。或者说 分类层级的序列
	 * @param evaluator 列指标：分类结果的计算器
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	static <U> IRecord pivotTable(final List<IRecord> rr, final String keys,
			final Function<Stream<IRecord>, U> evaluator) {
		return pivotTable(rr, keys.split(","), 0, REC(), ee -> evaluator.apply(ee.stream()));
	}

	/**
	 * 生成一个 数据透视表:参照Excel的实现。<br>
	 * 简单说说就是把 一个列表 rr:<br>
	 * a b c d <br>
	 * .. .. .. ..<br>
	 * .. .. .. ..<br>
	 * 分类成成 a/b/c [(a,b,c,d)],即如下图所示的 分组的层次结构，并调用函数 evaluator 计算终端数据集合：<br>
	 * [[a0 b0 c0 di],[a0 b0 c0 dj],...]的指标结果 U <br>
	 * 
	 * 可以所所谓透视就是对一个列表 rr进行分组再分组的过程，亦即 递归分组。<br>
	 * a0 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d0],[a0 b0 c0 d1],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d2],[a0 b0 c0 d3],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a0 b0 c0 d4],[a0 b0 c0 d5],...] <br>
	 * - - c1 <br>
	 * - - - [[a0 b0 c0 d6],[a0 b0 c0 d7'],...] <br>
	 * a1 <br>
	 * - b0 <br>
	 * - - c0 <br>
	 * - - - [[a1 b0 c0 d7],[a1 b0 c0 d9],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b0 c1 d10],[a1 b0 c1 d11],...] <br>
	 * - b1 <br>
	 * - - c0 <br>
	 * - - - [[a1 b1 c0 d12],[a1 b1 c0 d12],...] <br>
	 * - - c1 <br>
	 * - - - [[a1 b1 c1 d14],[a1 b1 c1 d15],...] <br>
	 * 
	 * 这样的层级结构。然后再对每个分组计算：调用函数 evaluator 结算集合的指标结果 U<br>
	 * 
	 * 例如:rr 需要包含：sale_name,goods_name 的key,以及 number 的值字段。<br>
	 * var result = IRecord.pivotTable( rr, <br>
	 * "sale_name,goods_name".split(","), <br>
	 * ee->ee.stream().collect(Collectors.summarizingDouble(e->e.dbl("number"))).getSum()
	 * <br>
	 * ); <br>
	 * 
	 * @param <U>       分类结果的计算器 结果类型
	 * @param stream    待分类的数据集合流的形式。即源数据
	 * @param keys      分类的key的数组，分类依据字段列表。或者说 分类层级的序列
	 * @param evaluator 列指标：分类结果的计算器
	 * @return 一个包含由层级关系 IRecord. 中间节点是IRecord类型，叶子节点是 U 类型。
	 */
	static <U> IRecord pivotTable(final Stream<IRecord> stream, final String keys[],
			final Function<List<IRecord>, U> evaluator) {
		return pivotTable(stream.collect(Collectors.toList()), keys, 0, REC(), evaluator);
	}

	/**
	 * Pivot Table 的归集器：使用示例
	 * cph(RPTS(3,L("A","B"))).stream().collect(pvtclc("0,1"));
	 * 
	 * @param keys 键名序列,用逗号分隔
	 * @return pivotTable 的 归集器
	 */
	static Collector<IRecord, List<IRecord>, IRecord> pvtclc(final String keys) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> IRecord.pivotTable(aa, keys, Jdbcs::LIST));
	}

	/**
	 * Pivot Table 的归集器：（指标变换批量evaluator） <br>
	 * 使用示例 <br>
	 * cph(RPTS(3,L("A","B"))).stream().collect(pvtclc("0,1"));
	 * 
	 * @param <U>       结果类型
	 * @param keys      键名序列,用逗号分隔
	 * @param evaluator 列指标：分类结果的计算器
	 * @return pivotTable 的 归集器
	 */
	static <U> Collector<IRecord, List<IRecord>, IRecord> pvtclc(final String keys,
			final Function<Stream<IRecord>, U> evaluator) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> IRecord.pivotTable(aa, keys, evaluator));
	}

	/**
	 * Pivot Table 的归集器：(（指标变换逐行mapper）) <br>
	 * 使用示例 <br>
	 * dfm.rowS().collect(IRecord.pvtclc1("name,order_type,position,drcr",
	 * getter("acctnum",0)))
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 列指标（列表）元素变换器：分类结果的计算器
	 * @return pivotTable 的 归集器
	 */
	static <U> Collector<IRecord, List<IRecord>, IRecord> pvtclc1(final String keys,
			final Function<IRecord, U> mapper) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> IRecord.pivotTable(aa, keys, lines -> lines.map(mapper).toList()));
	}

	/**
	 * Pivot Table 的归集器：(（指标变换逐行mapper）) <br>
	 * dfm.rowS().collect(IRecord.pvtclc1("name,order_type,position,drcr",
	 * getter("acctnum",0)))
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列
	 * @param mapper 列指标（列表）元素变换器：分类结果的计算器
	 * @return pivotTable 的 归集器
	 */
	static <U> Collector<IRecord, List<IRecord>, IRecord> pvtclc1(final String keys[],
			final Function<IRecord, U> mapper) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> IRecord.pivotTable(aa, keys, lines -> lines.stream().map(mapper).toList()));
	}

	/**
	 * Pivot Table 的归集器：搜集并变换 <br>
	 * 根据 keys 做数据 透视，并把中间结果List&lt;IRecord&gt;类型, <br>
	 * 存入IRecord(以keys为路径维度标记)，最后 使用 mapper 对IRecord进行变换 <br>
	 * 
	 * @param <T>    表换结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 结果变换函数 rec->t
	 * @return pivotTable 的 归集器
	 */
	static <T> Collector<IRecord, List<IRecord>, T> pvtclcL(final String keys, final Function<IRecord, T> mapper) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> mapper.apply(IRecord.pivotTable(aa, keys, Jdbcs::LIST)));
	}

	/**
	 * Pivot Table 的归集器：搜集并变换 <br>
	 * 根据 keys 做数据 透视，并把中间结果Stream&lt;IRecord&gt;类型, <br>
	 * 存入IRecord(以keys为路径维度标记)，最后 使用 mapper 对IRecord进行变换 <br>
	 * 
	 * @param <T>    表换结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 结果变换函数 rec->t
	 * @return pivotTable 的 归集器
	 */
	static <T> Collector<IRecord, List<IRecord>, T> pvtclcS(final String keys, final Function<IRecord, T> mapper) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> mapper.apply(IRecord.pivotTable(aa, keys, stream -> stream)));
	}

	/**
	 * Pivot Table 的归集器：搜集并变换 <br>
	 * 根据 keys 做数据 透视，并把中间结果IRecord[]类型, <br>
	 * 存入IRecord(以keys为路径维度标记)，最后 使用 mapper 对IRecord进行变换 <br>
	 * 
	 * @param <T>    表换结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 结果变换函数 rec->t
	 * @return pivotTable 的 归集器
	 */
	static <T> Collector<IRecord, List<IRecord>, T> pvtclcA(final String keys, final Function<IRecord, T> mapper) {
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> mapper.apply(IRecord.pivotTable(aa, keys, stream -> stream.toArray(IRecord[]::new))));
	}

	/**
	 * Pivot Table 的归集器：使用示例 cph(RPTS(3,L("A","B"))).stream().collect(pvtclc(0,1));
	 * 
	 * @param keys 键名序列,分类层级序列 ， ,键名非String类型的对象，会调用toString给予完成转换。
	 * @return pivotTable 的 归集器
	 */
	static Collector<IRecord, List<IRecord>, IRecord> pvtclc(final Object... keys) {

		final var kk = keys == null ? new String[] {} // 空的分类层级序列
				: Arrays.stream(keys).map(Object::toString).toArray(String[]::new);// 透视表的层级分类依据
		return Collector.of(LinkedList::new, List::add, (a, b) -> {
			a.addAll(b);
			return a;
		}, aa -> IRecord.pivotTable(aa, kk, Jdbcs::LIST));
	}

	/**
	 * 分组归集器
	 * 
	 * @param <X>     元素数据类型
	 * @param <K>     键名类型
	 * @param <V>     键值类型
	 * @param keyer   键名创建函数 x->k
	 * @param valueer 键值创建函数 x->v
	 * @return U 结果类型
	 */
	static <X, K, V, U> Collector<X, ?, Map<K, List<V>>> grpclc(final Function<X, K> keyer,
			final Function<X, V> valueer) {
		return grpclc(keyer, valueer, e -> e);
	}

	/**
	 * 分组归集器
	 * 
	 * @param <X>      元素数据类型
	 * @param <K>      键名类型
	 * @param <V>      键值类型
	 * @param <U>      结果类型
	 * @param keyer    键名创建函数 x->k
	 * @param valueer  键值创建函数 x->v
	 * @param finisher vv->u 最终结果的生成函数
	 * @return U 结果类型
	 */
	static <X, K, V, U> Collector<X, ?, Map<K, U>> grpclc(final Function<X, K> keyer, final Function<X, V> valueer,
			final Function<List<V>, U> finisher) {
		final Collector<X, List<V>, U> tlist = Collector.of((Supplier<List<V>>) () -> new ArrayList<V>(),
				(List<V> left, X right) -> {
					final var t = valueer.apply(right);
					left.add(t);
				}, (left, right) -> {
					return left;
				}, finisher);
		return Collectors.groupingBy(keyer, tlist);
	}

	/**
	 * 分组归集器
	 * 
	 * @param <X>      元素数据类型
	 * @param <K>      键名类型
	 * @param <V>      键值类型
	 * @param <U>      结果类型
	 * @param keyer    键名创建函数 x->k
	 * @param valueer  键值创建函数 x->v
	 * @param finisher vv->u 最终结果的生成函数
	 * @return U 结果类型
	 */
	static <X, K, V, U> Collector<X, ?, Map<K, U>> grpclc(final Function<X, K> keyer, final Function<X, V> valueer,
			final Collector<V, ?, U> finisher) {
		final Collector<X, List<V>, U> tlist = Collector.of((Supplier<List<V>>) () -> new ArrayList<V>(),
				(List<V> left, X right) -> {
					final var t = valueer.apply(right);
					left.add(t);
				}, (left, right) -> {
					return left;
				}, (aa) -> aa.stream().collect(finisher));
		return Collectors.groupingBy(keyer, tlist);
	}

	/**
	 * 转换成一个二维数组：把IRecord视为一个values的LinkedList,将其转换成一维数组，然后再把 rr 集成起来拼装成二维数组。
	 * 
	 * @param rr IRecord的集合
	 * @return 二维数组
	 */
	static String[][] toStringArray(final List<IRecord> rr) {
		return rr.stream().map(IRecord::toStrArray).toArray(String[][]::new);
	}

	/**
	 * 转换成一个二维数组
	 */
	static Object[][] toObjArray(final List<IRecord> rr) {
		return rr.stream().map(IRecord::toArray).toArray(Object[][]::new);
	}

	/**
	 * 把一个函数t2u 应用到一个 List&lt;T&gt;类型的容器类型
	 * 
	 * @param <T> 列表的元素的类型
	 * @param <U> 函数t2u的返回结果
	 * @param t2u 应用到列表上的函数
	 * @return 把一个函数应用到 一个LIST
	 */
	static <T, U> Function<List<T>, List<U>> applicative(final Function<T, U> t2u) {
		return ll -> ll.stream().map(t2u).collect(Collectors.toList());
	}

	/**
	 * line 待分解的字符序列 分解成 所有的字串名称。 一般用于数据钻取的分层处理。累加性运算的Key-Value 表达具有普遍意义。
	 * 
	 * 把 /a/b/c 解析成 /a,/a/b,/a/b/c 的需求
	 * 
	 * @param line 待分解的字符序列 delim 分隔符默认为 "/"
	 * @return 字符串的所有前缀的集合。
	 */
	static List<String> split2prefixes(final String line) {
		return split2prefixes(line, "/");
	}

	/**
	 * line 待分解的字符序列 分解成 所有的字串名称。 一般用于数据钻取的分层处理。累加性运算的Key-Value 表达具有普遍意义。
	 * 
	 * 把 /a/b/c 解析成 /a,/a/b,/a/b/c 的需求
	 * 
	 * @param line  待分解的字符序列
	 * @param delim 分隔符
	 * @return 字符串的所有前缀的集合。
	 */
	static List<String> split2prefixes(final String line, final String delim) {
		final var ss = line.split(delim);
		return split2prefixes(ss, delim);
	}

	/**
	 * 遮住scanl具有相似意义，差别是split2prefixes 返回的是List&lt;String&gt; 二者在原理上一致 line 待分解的字符序列
	 * 分解成 所有的字串名称。 一般用于数据钻取的分层处理。累加性运算的Key-Value 表达具有普遍意义。
	 * 
	 * 把 [a,b,c] 解析成 /a,/a/b,/a/b/c 的序列，这个对于累加数据的钻取，累加性运算的Key-Value 表达具有普遍意义。
	 * 
	 * @param ss    字符串序列，拼接成前缀字符串
	 * @param delim 分隔符,作根节点的分隔符
	 * @return 字符串的所有前缀的集合。
	 */
	static List<String> split2prefixes(final String[] ss, final String delim) {
		final var ll = new LinkedList<String>();
		ll.add(delim);// 加入根节点。
		final var buffer = new StringBuffer();
		Arrays.stream(ss).filter(e -> !e.matches("\\s*"))// 去除掉空白的key
				.forEach(s -> {
					buffer.append(delim).append(s);
					ll.add(buffer.toString());
				});// 前缀累加
		return ll;
	}

	/**
	 * 对键值儿进行扫描: 所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真前缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * [[1], [1, 2], [1, 2, 3], [1, 2, 3, 4], [1, 2, 3, 4, 5]]
	 * 
	 * @param <T>           元素类型
	 * @param <U>           变换结果类型
	 * @param stream        元素的流数据
	 * @param fx            真前缀的变换函数:tt-&gt;u
	 * @param reverse       是否对数据进行倒转
	 * @param include_empty 是否包含空列表。true 表示包含空集合
	 * @return List&lt;U&gt;
	 */
	@SuppressWarnings("unchecked")
	static <T, U> List<U> scan(final Stream<T> stream, final Function<List<T>, U> fx, boolean reverse,
			boolean include_empty) {
		final var uu = new LinkedList<U>();
		if (stream == null)
			return uu;
		final Function<List<T>, U> final_fx = fx == null ? (Function<List<T>, U>) (e -> (U) e) : fx;
		final var prev = new LinkedList<T>();
		if (include_empty)
			uu.add(final_fx.apply(LIST(prev)));// 包含空列表
		stream.forEach(t -> {
			prev.add(t);
			final var pp = LIST(prev);
			if (reverse)
				Collections.reverse(pp);
			final var u = final_fx.apply(pp);
			uu.add(u);
		});

		return uu;
	}

	/**
	 * 对键值儿进行扫描: 所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真前缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * [[1], [1, 2], [1, 2, 3], [1, 2, 3, 4], [1, 2, 3, 4, 5]]
	 * 
	 * @param <T>     元素类型
	 * @param <U>     fx:变换结果类型
	 * @param stream  元素的流数据
	 * @param fx      真前缀的变换函数:tt-&gt;u
	 * @param reverse 是否对数据进行倒转
	 * @return List&lt;U&gt; U的集合
	 */
	static <T, U> List<U> scan(final Stream<T> stream, final Function<List<T>, U> fx, boolean reverse) {
		return scan(stream, fx, reverse, false);
	}

	/**
	 * 对键值儿进行扫描: 所谓扫描是指 对于 [1,2,3,4] 这样的序列生成 如下的真前缀的集合(不包括空集合)：术语scan 来源于 haskell 语言
	 * [[1], [1, 2], [1, 2, 3], [1, 2, 3, 4], [1, 2, 3, 4, 5]]
	 * 
	 * @param <T>    元素类型
	 * @param <U>    变换结果类型
	 * @param stream 元素的流数据
	 * @param fx     真前缀的变换函数:tt-&gt;u
	 * @return List&lt;U&gt; U 的集合
	 */
	static <T, U> List<U> scan(final Stream<T> stream, final Function<List<T>, U> fx) {
		return scan(stream, fx, false);
	}

	/**
	 * 强制类型转换函数 <br>
	 * 把一个Object 转换目标类型：动用rec2obj的方式进行转换。 <br>
	 * 创立一个 key为0的值为为obj的record 然后使用IRecord的类型转换方法进行类型转换。<br>
	 * 是 IRecord.rec2obj(REC(0,obj),targetClass); 的简写方式 <br>
	 * 由于record 就是模仿 LISP语言中的列表（增强了key-value的树形特征）。<br>
	 * 所以 IRecord 理论上是可以标识任何数据结构的(类型)。<br>
	 * 所以rec 转换成任意一个对象结构也是可行的。<br>
	 * 
	 * @param <T>         目标类型数据的class
	 * @param targetClass 目标类型
	 * @return obj-&gt;T 的实例函数
	 */
	static <T> Function<Object, T> coerce(final Class<T> targetClass) {
		return obj -> IRecord.obj2target(obj, targetClass);// 创立一个 key为0的值为为obj的recourd 然后使用IRecord的类型转换方法进行类型转换。
	}

	/**
	 * 把一个Object 转换目标类型：动用rec2obj的方式进行转换。 <br>
	 * 创立一个 key为0的值为为obj的record 然后使用IRecord的类型转换方法进行类型转换。<br>
	 * 是 IRecord.rec2obj(REC(0,obj),targetClass); 的简写方式 <br>
	 * 由于record 就是模仿 LISP语言中的列表（增强了key-value的树形特征）。<br>
	 * 所以 IRecord 理论上是可以标识任何数据结构的(类型)。<br>
	 * 所以rec 转换成任意一个对象结构也是可行的。<br>
	 * 
	 * @param <T>         目标类型数据的class
	 * @param obj         源数据类型
	 * @param targetClass 目标类型
	 * @return T类型数据结果
	 */
	static <T> T obj2target(final Object obj, final Class<T> targetClass) {
		return IRecord.rec2obj(REC(0, obj), targetClass);// 创立一个 key为0的值为为obj的recourd 然后使用IRecord的类型转换方法进行类型转换。
	}

	/**
	 * 把一个record 转换目标类型：由于record 就是模仿 LISP语言中的列表（增强了key-value的树形特征）。<br>
	 * 所以 IRecord 理论上是可以标识任何数据结构的。<br>
	 * 所以rec 转换成任意一个对象结构也是可行的。<br>
	 * 
	 * 对于集合类型,如:数组, Collections,Stream,Iterable
	 * 只从键值列表中检索是否存在对应的类型给kvp数据，如存在返回对应的value，否则返回null<br>
	 * 并不做更为高级的加工处理 <br>
	 * 
	 * <br>
	 * 可以这样理解obj2target<br>
	 * 首先 我们把 rec 视为一个 targetClass 的候选值的 集合(KVPair kvp 的值)。<br>
	 * 尝试在 用targetClass与kvp中的值进行匹配<br>
	 * 如果没有合适类型。我们就把 obj2target 视为 targetClass 的构造函数的参数集合 参数名称就是 kvp的key,<br>
	 * 参数值 就是 kvp的value <br>
	 * 调用targetClass的默认构造函数使用参数 rec给予初始化进而生成对象。 <br>
	 * 
	 * @param <T>         目标类型数据的class
	 * @param rec         IRecord 结构的数据
	 * @param targetClass 目标类型,当targetClass 为Object.class 返回rec本省。
	 * @return targetClass 的一个实例数据。
	 */
	@SuppressWarnings("unchecked")
	static <T> T rec2obj(final IRecord rec, final Class<T> targetClass) {
		// IRecord 作为特殊存在，给予直接返回。
		if (targetClass.isAssignableFrom(IRecord.class))
			return (T) rec;// IRecord 直接返回

		if (rec != null) {// 首先从record 内进行类型检索
			// 元素类型提取
			final var opt = rec.tupleS().map(KVPair::value).filter(Objects::nonNull)
					.filter((e -> targetClass.isAssignableFrom(e.getClass()))).findFirst(); // 寻找一个满足要求类型的字段
			if (opt.isPresent())
				return (T) opt.get();
		} // rec

		////////////////////////////////////////////////////////////////////
		// java 基本类型的处理
		////////////////////////////////////////////////////////////////////

		// 布尔类型的处理
		if (targetClass == Boolean.class || targetClass == boolean.class) {// 布尔类型的转换
			// 这是规定。比如 判断表是否存在 "show tables like ''{0}''",不存在的就是返回null,因此作为false;
			if (rec == null)
				return (T) (Object) false;// 值null 是为false.
			final var value = rec.get(0);// 提取字段的第一项 这个对于 boolean 比较特殊 仅通过 一项 就可以判断出来，就不用 遍历所有字段了。
			if (value == null) {// null 被视为 false
				return (T) (Object) false;
			} else {// 非空值
				if (value instanceof String) {// 字符串类型 "false" 视为false,其他视为true
					final var str = ((String) value).strip().toLowerCase(); // string value
					try { // 尝试转换成数字然后0是做false,其他视作true
						final Number number = Double.parseDouble(str);
						return (T) (Object) (number.intValue() != 0);
					} catch (NumberFormatException e) {
						// do nothing
					} // try
					return (T) (Object) (!str.equals("false"));
				} else if (Number.class.isAssignableFrom(value.getClass())) {// 数值类型非0视为true;
					final var number = (Number) value; // number value
					return (T) (Object) (number.intValue() != 0);// 非零均视为true
				} else {// 其他类型均视为true
					return (T) (Object) true;
				} // if
			} // if(v==null)
		} // targetClass== Boolean.class

		// 把Boolean类型前置再rec==null 就是为了处理 null作为false的理解的情况。
		if (rec == null)
			return null;// 无效则直接返回。

		// 遍历字段 处理基本类型
		for (int i = 0; i < rec.size(); i++) { // 依次遍历各个字段
			T target = null; // 目标值
			// java 的基本类型的处理
			if (targetClass == Byte.class || targetClass == byte.class) {// 字节类型
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number.byteValue());
			} else if (targetClass == Integer.class || targetClass == int.class) {// 整型
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number.intValue());
			} else if (targetClass == Long.class || targetClass == long.class) {// 长整型
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number.longValue());
			} else if (targetClass == Double.class || targetClass == double.class) {// 双精度
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number.doubleValue());
			} else if (targetClass == Float.class || targetClass == float.class) {// 单精度
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number.floatValue());
			} else if (targetClass == Short.class || targetClass == short.class) {// 短整型
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number.shortValue());
			} else if (targetClass == Character.class || targetClass == char.class) {// 字符型:这里由精度损失。仅用于ascii码
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) ((char) number.byteValue());
			} else if (targetClass == Number.class) {// 数字
				final var number = rec.num(i); // 提取数字类型的值
				if (number != null)
					target = (T) (Object) (number);
			} else if (targetClass == Date.class) {// 日期类型
				target = (T) (Object) (rec.date(i));
			} else if (targetClass == LocalDate.class) {// 本地日期
				target = (T) (Object) (rec.ld(i));
			} else if (targetClass == LocalTime.class) {// 本地时间
				target = (T) (Object) (rec.lt(i));
			} else if (targetClass == LocalDateTime.class) {// 本事日期使劲。
				target = (T) (Object) (rec.ldt(i));
			} else if (targetClass == Timestamp.class) {// 时间戳类型
				target = (T) (Object) (rec.timestamp(i));
			} else if (targetClass == String.class) {// 字符串类型
				target = (T) (Object) (rec.str(i));
			} else if (targetClass == Object.class) {// 对象类型返回本身
				target = (T) (Object) rec;
			} else if ((targetClass.isAssignableFrom(Collection.class) // 集合类型的判断
					|| targetClass.equals(Stream.class) // 流类型
					|| targetClass.isArray() // 数组
					|| targetClass.isAssignableFrom(Iterable.class) // 迭代类型
			) && rec.get(i) != null) {// 对象类型返回本身

				final var tclass = (Class<T>) rec.get(i).getClass();
				if (targetClass.isAssignableFrom(tclass))
					target = (T) rec.get(i);
			} // if

			if (target != null)
				return target;
		} // for 字段遍历

		////////////////////////////////////////////////////////////////////
		// JavaBean 对象 的 处理 ： 当 字段逐个遍历之后仍旧找不到 合适的类型
		// 则 认定 targetClass 是一个复合类型,需要采用构造函数的方式来进行创建
		////////////////////////////////////////////////////////////////////
		Object javaBean = null;// 目标类型
		try {
			final var ctor = targetClass.getDeclaredConstructor((Class<?>[]) null); // 获取默认构造函数
			ctor.setAccessible(true);// 增强构造函数的访问能力
			javaBean = ctor.newInstance((Object[]) null);// 提取默认构造函数
			IRecord.OBJINIT(javaBean, rec);// 对Javabean 进行实例化(字段初始化)
		} catch (Exception e) {
			if (debug)
				e.printStackTrace();
		} // try

		// 返回结果值
		return (T) javaBean;// 以bean的实例作为对象返回值。
	}// rec2obj

	/////////////////////////////////////////////////////////////////////
	// 以下是IRecord 的默认静态函数区域
	/////////////////////////////////////////////////////////////////////

	/**
	 * 构建一个 IRecord的构建器
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder builder(final String keys) {
		return new Builder(keys);
	}

	/**
	 * 构建一个 IRecord的构建器
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder builder(final String[] keys) {
		return new Builder(keys);
	}

	/**
	 * 构建一个 IRecord的构建器
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder builder(final List<String> keys) {
		return new Builder(keys);
	}

	/**
	 * 构建一个 IRecord的构建器
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder builder(final Stream<String> keys) {
		return new Builder(keys.toArray(String[]::new));
	}

	/**
	 * 构建一个 IRecord的构建器
	 * 
	 * @param recs 键名集合：只提取 recs中的keys,会把 recs 合并成一个 Record 然后提取keys,空值返回一个 空的REC().
	 * @return Builder
	 */
	static Builder builder(final IRecord... recs) {
		final var rec = REC();
		if (recs != null)
			Arrays.stream(recs).forEach(rec::add);
		return new Builder(rec.keys().stream().map(SQL::parseFieldName).map(e -> e.str("name")).toList());
	}

	/**
	 * 构建一个 IRecord的构建器
	 * 
	 * @param sql named sql 对象，提取其中的 sqlCtx作为键名列表
	 * @return Builder
	 */
	static Builder builder(final SQL sql) {
		return IRecord.builder(sql.getSqlCtx2());
	}

	/**
	 * 构建一个 IRecord的构建器 (builder的别名)
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder rb(final String keys) {
		return new Builder(keys);
	}

	/**
	 * 构建一个 IRecord的构建器 (builder的别名)
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder rb(final String[] keys) {
		return IRecord.builder(keys);
	}

	/**
	 * 构建一个 IRecord的构建器 (builder的别名)
	 * 
	 * @param recs 键名集合：只提取 recs中的keys,会把 recs 合并成一个 Record 然后提取keys,空值返回一个 空的REC().
	 * @return Builder
	 */
	static Builder rb(final IRecord... recs) {
		return builder(recs);
	}

	/**
	 * 构建一个 IRecord的构建器 (builder的别名)
	 * 
	 * @param sql named sql 对象，提取其中的 sqlCtx作为键名列表
	 * @return Builder
	 */
	static Builder rb(final SQL sql) {
		return IRecord.builder(sql);
	}

	/**
	 * 构建一个 IRecord的构建器 (builder的别名)
	 * 
	 * @param keys 键名序列
	 * @return Builder
	 */
	static Builder rb(final List<String> keys) {
		return IRecord.builder(keys);
	}

	/**
	 * 采用SQL::parseFieldName 进行字段名处理<br>
	 * 构建一个 IRecord的构建器 (builder的别名)
	 * 
	 * @param keys 键名序列,不同键之间用逗号分隔
	 * @return Builder
	 */
	static Builder rb2(final String keys) {
		return rb2(keys.split("[,]+"));
	}

	/**
	 * 采用SQL::parseFieldName 进行字段名处理<br>
	 * 构建一个 IRecord的构建器 (builder的别名) <br>
	 * 
	 * @param keys 键名序列,不同键之间用逗号分隔
	 * @return Builder
	 */
	static Builder rb2(final String[] keys) {
		return IRecord.builder(Arrays.stream(keys).map(SQL::parseFieldName).map(e -> e.str("name")));
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 构建IRecord的归集器 <br>
	 * 
	 * <br>
	 * 例如: Stream.of(1,2,3,4).collect(IRecord.rbclc("A,B,C,D"))<br>
	 * 返回: A:1 B:2 C:3 D:4 <br>
	 * <br>
	 * Stream.of(1,2,3,4).collect(IRecord.rbclc("A,B,C,D,E,F")) <br>
	 * 返回: A:1 B:2 C:3 D:4 E:1 F:2 <br>
	 * <br>
	 * Stream.of(1,2,3,4).collect(IRecord.rbclc("A,B")) <br>
	 * 返回: A:1 B:2
	 * 
	 * @param <T>  归集的元素类型
	 * @param keys 键名序列,键名之间用逗号分隔,当keys 为 null 采取 excelname 方式进行 字段命名，即依次命名为 :
	 *             A,B,C,...
	 * @return 构建IRecord的归集器
	 */
	static <T> Collector<T, ?, IRecord> rbclc(final String keys) {
		return new Builder(keys).collector();
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 采取 excelname 方式进行 字段命名，即依次命名为 : A,B,C,... <br>
	 * <br>
	 * 例如：Stream.of(1,2,3,4,5).collect(IRecord.rbclc()) <br>
	 * 返回：A:1 B:2 C:3 D:4 E:5 构建IRecord的归集器 <br>
	 */
	static <T> Collector<T, ?, IRecord> rbclc() {
		return new Builder().collector();
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 构建IRecord的归集器 <br>
	 * 
	 * 示例：Stream.of(1,2,3,4,5).collect(IRecord.rbclc("a,b,c,d,e", e->"-".repeat(e)))
	 * <br>
	 * 返回：a:- b:-- c:--- d:---- e:----- <br>
	 * 
	 * @param <T>        源流的元素类型
	 * @param <U>        目的流的元素类型
	 * @param keys       键名序列,键名之间用逗号分隔
	 * @param t2u_mapper 预处理器值变换函数 t->u, 把 流中的元素t变换成u类型之后再传递给IRecord.Builder
	 * @return IRecord Builder 的归集器
	 */
	static <T, U> Collector<T, ?, IRecord> rbclc(final String keys, final Function<T, U> t2u_mapper) {
		return new Builder(keys).collector(t2u_mapper);
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 构建IRecord的归集器 <br>
	 * 
	 * 示例：Stream.of(1,2,3,4,5).collect(IRecord.rbclc((i,v)->LittleTree.excelname(i)))
	 * <br>
	 * 返回：a:- b:-- c:--- d:---- e:----- <br>
	 * 
	 * @param <T>        源流的元素类型
	 * @param <U>        目的流的元素类型
	 * @param key_mapper 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
	 * @return IRecord Builder 的归集器
	 */
	static <T, U> Collector<T, ?, IRecord> rbclc(final BiFunction<Integer, Object, String> key_mapper) {
		return new Builder(key_mapper).collector();
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 构建IRecord的归集器 <br>
	 * 
	 * 示例：Stream.of(1,2,3,4,5).collect(IRecord.rbclc((i,v)->LittleTree.excelname(i),
	 * e->"-".repeat(e))) <br>
	 * 返回：A:- B:-- C:--- D:---- E:-----
	 * 
	 * @param <T>        源流的元素类型
	 * @param <U>        目的流的元素类型
	 * @param key_mapper 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
	 * @param t2u_mapper 预处理器值变换函数 t->u, 把 流中的元素t变换成u类型之后再传递给IRecord.Builder
	 * @return IRecord Builder 的归集器
	 */
	static <T, U> Collector<T, ?, IRecord> rbclc(final BiFunction<Integer, Object, String> key_mapper,
			final Function<T, U> t2u_mapper) {
		return new Builder(key_mapper).collector(t2u_mapper);
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 构建IRecord的归集器 <br>
	 * <br>
	 * excelname 样式的键名命名规则 <br>
	 * 
	 * 示例：Stream.of(1,2,3,4,5).collect(IRecord.rbclc(e->"-".repeat(e))) <br>
	 * 返回：A:- B:-- C:--- D:---- E:----- <br>
	 * 
	 * @param <T>    源流的元素类型
	 * @param <U>    目的流的元素类型
	 * @param mapper 预处理器值变换函数 t->u, 把 流中的元素t变换成u类型之后再传递给IRecord.Builder
	 * @return IRecord Builder 的归集器
	 */
	static <T, U> Collector<T, ?, IRecord> rbclc(final Function<T, U> mapper) {
		return new Builder().collector(mapper);
	}

	/**
	 * IRecord.Builder Collector <br>
	 * 构建IRecord的归集器 <br>
	 * 
	 * 示例：Stream.of(1,2,3,4,5).collect(IRecord.rbclc(A(1,2,3,4,5),
	 * e->"-".repeat(e))) <br>
	 * 返回：1:- 2:-- 3:--- 4:---- 5:----- <br>
	 * 
	 * @param <T>    源流的元素类型
	 * @param <U>    目的流的元素类型
	 * @param keys   键名序列数组
	 * @param mapper 预处理器值变换函数 t->u, 把 流中的元素t变换成u类型之后再传递给IRecord.Builder
	 * @return IRecord Builder 的归集器
	 */
	static <T, U> Collector<T, ?, IRecord> rbclc(final Object[] keys, final Function<T, U> mapper) {
		final var kk = Stream.of(keys).map(String::valueOf).toArray(String[]::new);// 键名列表
		return new Builder(kk).collector(mapper);
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 null
	 * 
	 * @param <T> 函数的参数类型
	 * @return t-&gt;dbl
	 */
	static <T> Function<T, Double> obj2dbl() {
		return IRecord.obj2dbl(null);
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 defaultValue
	 * 
	 * @param <T>          函数的参数类型
	 * @param defaultValue 非法的数字类型 返回 的默认值
	 * @return t-&gt;dbl
	 */
	static <T> Function<T, Double> obj2dbl(final Number defaultValue) {
		return (T obj) -> {
			if (obj instanceof Number)
				return ((Number) obj).doubleValue();
			Double dbl = defaultValue == null ? null : defaultValue.doubleValue();
			try {
				dbl = Double.parseDouble(obj.toString());
			} catch (Exception e) {
				// e.printStackTrace();
			}
			return dbl;
		};
	}

	/**
	 * 把一个数据对象转换为num
	 */
	Function<Object, Number> obj2num = obj -> {
		if (obj instanceof Number)
			return (Number) obj;
		Number num = (Number) Double.parseDouble(obj.toString());
		return num;
	};

	/**
	 * 把一个键值对的数据转换成一个数字的类型
	 */
	Function<KVPair<String, ?>, Number> kv2num = (kv) -> obj2num.apply(kv.value());

	/**
	 * 把一个键值对的数据转换成一个数字的类型
	 */
	Function<KVPair<String, Object>, Number> kv2num2 = (kv) -> obj2num.apply(kv.value());

	/**
	 * 把一个键值对的数据转换成一个R的类型
	 */
	Function<KVPair<String, ?>, Integer> kv2int = (kv) -> obj2num.apply(kv.value()).intValue();

	/**
	 * 把一个键值对的数据转换成一个整形的类型
	 */
	Function<KVPair<String, Object>, Integer> kv2int2 = (kv) -> obj2num.apply(kv.value()).intValue();

	/**
	 * 把一个键值对的数据转换成一个R的类型
	 */
	Function<KVPair<String, ?>, Long> kv2lng = (kv) -> obj2num.apply(kv.value()).longValue();

	/**
	 * 把一个键值对的数据转换成一个长整形的类型
	 */
	Function<KVPair<String, Object>, Long> kv2lng2 = (kv) -> obj2num.apply(kv.value()).longValue();

	/**
	 * 把一个键值对的数据转换成一个R的类型
	 */
	Function<KVPair<String, ?>, Double> kv2dbl = (kv) -> obj2num.apply(kv.value()).doubleValue();

	/**
	 * 把一个键值对的数据转换成一个双精度浮点数的类型
	 */
	Function<KVPair<String, Object>, Double> kv2dbl2 = (kv) -> obj2num.apply(kv.value()).doubleValue();

	/**
	 * 把一个键值对的数据转换成一个浮点数的类型
	 */
	Function<KVPair<String, ?>, Float> kv2float = (kv) -> obj2num.apply(kv.value()).floatValue();

	/**
	 * 把一个键值对的数据转换成一个浮点数的类型
	 */
	Function<KVPair<String, Object>, Float> kv2float2 = (kv) -> obj2num.apply(kv.value()).floatValue();

	/**
	 * 把一个键值对的数据转换成一个段整形的类型
	 */
	Function<KVPair<String, ?>, Short> kv2short = (kv) -> obj2num.apply(kv.value()).shortValue();

	/**
	 * 把一个键值对的数据转换成一个短整形的类型
	 */
	Function<KVPair<String, Object>, Short> kv2short2 = (kv) -> obj2num.apply(kv.value()).shortValue();

	/**
	 * 把一个键值对的数据转换成一个字节的类型
	 */
	Function<KVPair<String, ?>, Byte> kv2byte = (kv) -> obj2num.apply(kv.value()).byteValue();

	/**
	 * 把一个键值对的数据转换成一个字节的类型
	 */
	Function<KVPair<String, Object>, Byte> kv2byte2 = (kv) -> obj2num.apply(kv.value()).byteValue();

	/**
	 * 把一个键值对的数据转换成一个布尔的类型
	 */
	Function<KVPair<String, ?>, Boolean> kv2bool = (kv) -> Boolean.valueOf(kv.value() + "");

	/**
	 * 把一个键值对的数据转换成一个布尔的类型
	 */
	Function<KVPair<String, Object>, Boolean> kv2bool2 = (kv) -> Boolean.valueOf(kv.value() + "");

	/**
	 * 把一个键值对的数据转换成一个字符的类型
	 */
	Function<KVPair<String, Object>, Character> kv2char = (kv) -> (kv.value() + "").charAt(0);

	/**
	 * 把一个键值对的数据转换成一个字符的类型
	 */
	Function<KVPair<String, Object>, Character> kv2char2 = (kv) -> (kv.value() + "").charAt(0);

	/**
	 * 等价函数
	 */
	Function<Object, Object> identity = (Object o) -> o;

	/**
	 * 统计搜集工具
	 * 
	 * @param name 字段名称
	 * @return
	 */
	static Collector<? super IRecord, ?, DoubleSummaryStatistics> dbl_stats(String name) {
		return dbl_stats(e -> e.dbl(name));
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param t2dbl 类型变换函数 把 一个T类型的对象转换成浮点数
	 * @return 浮点数的统计器
	 */
	static <T> Collector<? super T, ?, DoubleSummaryStatistics> dbl_stats(ToDoubleFunction<T> t2dbl) {
		return Collectors.summarizingDouble(t2dbl);
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param t2dbl 类型变换函数 把 一个KVP类型的对象转换成浮点数
	 * @return 浮点数的统计器
	 */
	static Collector<KVPair<String, Object>, ?, DoubleSummaryStatistics> dbl_stats2(
			Function<KVPair<String, Object>, Double> t2dbl) {
		return Collectors.summarizingDouble(kv2dbl::apply);
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param name 字段名称
	 * @return 整型统计对象
	 */
	static Collector<? super IRecord, ?, IntSummaryStatistics> int_stats(String name) {
		return Collectors.summarizingInt(e -> e.i4(name));
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param <T>   源数据对象的类型
	 * @param t2int 对象转换成整型的函数
	 * @return 整型统计对象
	 */
	static <T> Collector<? super T, ?, IntSummaryStatistics> int_stats(ToIntFunction<T> t2int) {
		return Collectors.summarizingInt(t2int);
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param t2int kvp对象转换成整型的函数
	 * @return 整型统计对象
	 */
	static Collector<KVPair<String, Object>, ?, IntSummaryStatistics> int_stats2(
			Function<KVPair<String, Object>, Integer> t2int) {
		return Collectors.summarizingInt(t2int::apply);
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param name 字段名称
	 * @return 长整型统计对象
	 */
	static Collector<? super IRecord, ?, LongSummaryStatistics> lng_stats(String name) {
		return lng_stats(e -> e.i4(name));
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param t2lng 字段名称
	 * @param <T>   列表数据元素的类型
	 * @return 长整型统计对象
	 */
	static <T> Collector<? super T, ?, LongSummaryStatistics> lng_stats(ToLongFunction<T> t2lng) {
		return Collectors.summarizingLong(t2lng);
	}

	/**
	 * 统计搜集工具
	 * 
	 * @param t2lng 元素变换函数 kvp-&gt;long
	 * @return 长整型统计对象
	 */
	static Collector<KVPair<String, Object>, ?, LongSummaryStatistics> lng_stats2(
			Function<KVPair<String, Object>, Integer> t2lng) {
		return Collectors.summarizingLong(t2lng::apply);
	}

	/**
	 * List Collector : 对一个 List&lt;T&gt; 进行归纳: ll表示 mapper 是一个流List->U
	 * 
	 * @param <T>    归纳元素类型
	 * @param <U>    返回结果类型
	 * @param mapper 列表变换函数 records -> U
	 * @return U 类型的对象
	 */
	static <T, U> Collector<T, List<T>, U> llclc(final Function<List<T>, U> mapper) {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, mapper);// Collector
	}

	/**
	 * List Collector : 对一个 List&lt;IRecord&gt; 进行归纳: 不进行数据变换
	 * 
	 * @param <T> 归纳元素类型
	 * @return List&lt;T&gt; T 类型的列表
	 */
	static <T> Collector<T, List<T>, List<T>> llclc() {
		return llclc(e -> e);
	}

	/**
	 * Stream Collector : 对一个 List&lt;T&gt; 进行归纳: ss 表示 mapper 是一个流Stream-&gt;U
	 * 
	 * @param <T>    归纳元素类型
	 * @param <U>    返回结果类型
	 * @param mapper 流变换函数 records -&gt; U
	 * @return U 类型的对象
	 */
	static <T, U> Collector<T, List<T>, U> ssclc(final Function<Stream<T>, U> mapper) {
		final Collector<T, List<T>, List<T>> clc = llclc(); // 获取链表归集器
		return Collector.of(clc.supplier(), clc.accumulator(), clc.combiner(), ll -> mapper.apply(ll.stream()));// Collector
	}

	/**
	 * Array Collector : 对一个 List&lt;T&gt; 进行归纳: ss 表示 mapper 是一个流Stream-&gt;U
	 * 
	 * @param <T>    归纳元素类型
	 * @param <U>    返回结果类型
	 * @param mapper 流变换函数 tt-> U
	 * @return U 类型的对象
	 */
	@SuppressWarnings("unchecked")
	static <T, U> Collector<T, List<T>, U> aaclc(final Function<T[], U> mapper) {
		final Collector<T, List<T>, List<T>> clc = llclc(); // 获取链表归集器
		return Collector.of(clc.supplier(), clc.accumulator(), clc.combiner(), ll -> mapper.apply(ll.toArray(n -> {
			final var componentType = ll.stream().filter(Objects::nonNull).findFirst().map(e -> (Class<T>) e.getClass())
					.orElse((Class<T>) Object.class);
			return (T[]) Array.newInstance(componentType, n);
		})));// Collector
	}

	/**
	 * 把IRecord 集合 [rec] 规约成一个 IRecord 向量 r,然后 再 调用r2u把r变换成u类型的对象
	 *
	 * @param <U> 结果类型
	 * @param r2u 映射 rec->u
	 * @return r2u 类型的归集器
	 */
	static <U> Collector<IRecord, List<IRecord>, U> r2uclc(final Function<IRecord, U> r2u) {
		@SuppressWarnings("unchecked")
		final Function<IRecord, U> mapper = r2u == null ? e -> (U) e : r2u;
		final Collector<IRecord, List<IRecord>, List<IRecord>> clc = llclc(); // 获取链表归集器
		return Collector.of(clc.supplier(), clc.accumulator(), clc.combiner(), compose_f(mapper, IRecord::ROWS2COLS));
	}

	/**
	 * 把IRecord 集合 [rec] 规约成一个 IRecord 向量 r,然后 再 调用r2u把r变换成u类型的对象
	 *
	 * @param <U> 结果类型
	 * @param r2u 映射 rec->u
	 * @return r2u 类型的归集器
	 */
	static <U> Collector<KVPair<String, ?>, List<KVPair<String, Object>>, U> r2uclc2(final Function<IRecord, U> r2u) {
		return t2uclc(e -> e, r2u);
	}

	/**
	 * 把t 集合 [t] 规约成一个 IRecord 向量 r
	 *
	 * @param <T>   规约元素类型
	 * @param t2kvp 映射 t->kvp
	 * @return t2r 类型的归集器
	 */
	static <T> Collector<T, List<KVPair<String, Object>>, IRecord> t2rclc(final Function<T, KVPair<String, ?>> t2kvp) {
		return IRecord.t2uclc(t2kvp, e -> e);
	}

	/**
	 * 把t 集合 [t] 规约成一个 IRecord 向量 r,然后 再 调用r2u把r变换成u类型的对象
	 *
	 * @param <T>   规约元素类型
	 * @param <U>   结果类型
	 * @param t2kvp 映射 t->kvp
	 * @param r2u   映射 rec->u
	 * @return t2u 类型的归集器
	 */
	@SuppressWarnings("unchecked")
	static <T, U> Collector<T, List<KVPair<String, Object>>, U> t2uclc(final Function<T, KVPair<String, ?>> t2kvp,
			final Function<IRecord, U> r2u) {

		final Function<IRecord, U> mapper = r2u == null ? e -> (U) e : r2u;
		final Function<List<KVPair<String, Object>>, U> kvp2u = compose_f(mapper, IRecord::KVS2REC);

		return Collector.of(ArrayList::new, (aa, a) -> aa.add((KVPair<String, Object>) t2kvp.apply(a)), (a, b) -> {
			a.addAll(b);
			return a;
		}, kvp2u);
	}

	/**
	 * 累加行结构的IRecord并将其转换成列结构的IRecord<br>
	 * Collector 变换器。可以把一个IRecord流序列转换成一个IRecord对象<br>
	 * 方法示意: cph2(RPTA(3,A(1,2,3))).collect(rclc) 即生成一个数据框/columns结构的IRecord<br>
	 */
	Collector<IRecord, List<IRecord>, IRecord> rclc = r2uclc(e -> e);

	/**
	 * rclc2 ( rclc2 的后缀2 表示这是一个队KVPair 进行归集的版本 ) <br>
	 * 对KVPair&lt;String,?&gt;进行累积并组合成一个IRecord <br>
	 * 方法示意: REC("key",
	 * A("name","sex","birth"),"value",A("姓名","性别","出生日期")).rcollect2(rclc2); <br>
	 * 生成一个 字典对象 { name:姓名 sex:性别 birth:出生日期 }
	 */
	Collector<KVPair<String, ?>, List<KVPair<String, Object>>, IRecord> rclc2 = r2uclc2(e -> e);

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列，键名之间用 逗号,/,\号进行分隔
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final String keys) {
		return IRecord.cmp(keys, true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列，键名之间用 逗号,/,\号进行分隔
	 * @param asc  是否升序,true 升序从小打到大,fasle 降序从大到小
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final String keys, final boolean asc) {
		return IRecord.cmp(keys.split("[,/\\\\]+"), asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param <T>    元素类型
	 * @param <U>    具有比较能力的类型
	 * @param keys   keys 键名序列，键名之间用 逗号,/,\号进行分隔
	 * @param mapper (key:键名,t:键值)->u 比较能力变换器
	 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	public static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final String keys,
			final BiFunction<String, T, U> mapper, final boolean asc) {
		return IRecord.cmp(keys.split("[,/\\\\]+"), mapper, asc);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final List<String> keys) {
		return IRecord.cmp(keys, true);
	}

	/**
	 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
	 * 
	 * @param keys 键名序列
	 * @return keys 序列的比较器
	 */
	public static Comparator<IRecord> cmp(final List<String> keys, final boolean asc) {
		return IRecord.cmp(keys.toArray(String[]::new), asc);
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
	 * @param mapper (key:键名,t:键值)->u 比较能力变换器
	 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return keys 序列的比较器
	 */
	@SuppressWarnings("unchecked")
	public static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final String keys[],
			final BiFunction<String, T, U> mapper, final boolean asc) {
		final BiFunction<String, T, U> final_mapper = mapper == null
				? (String i, T o) -> o instanceof Comparable ? (U) o // 返回 可比较类型
						: (o == null // o 的空值判断，是否是空值
								? null // 空值保持不变
								: (U) (o + "") // 非空值 转换成字符串
						) // o 的空值判断
				: mapper; // 使用指定的 值变换函数

		return (a, b) -> { // Comparator<IRecord> 比较器
			final var queue = new LinkedList<String>();
			for (var k : keys)
				queue.offer(k);// 压入队列
			while (!queue.isEmpty()) {
				final var key = queue.poll(); // 提取队首元素
				final var ta = (Comparable<Object>) a.get(key, (T t) -> final_mapper.apply(key, t));
				final var tb = (Comparable<Object>) b.get(key, (T t) -> final_mapper.apply(key, t));
				if (ta == null && tb == null)
					return 0;
				else if (ta == null && tb != null) {
					return -1;
				} else if (ta != null && tb == null) {
					return 1;
				} else {
					int ret = 0; // 默认的比较结果
					try {
						ret = ta.compareTo(tb);// 进行元素比较
					} catch (Exception e) {
						ret = (ta + "").compareTo(tb + "");// 进行元素比较
					}
					if (ret != 0) {
						return (asc ? 1 : -1) * ret;
					} // 返回比较结果,如果不相等直接返回,相等则继续比计较
				} // if
			} // while
			return 0;// 所有key都比较完毕,则认为两个元素相等
		}; // Comparator<IRecord> 比较器
	}

	/**
	 * 判断键名key的值是否位于values值之中
	 * 
	 * @param <T>    备选值的类型
	 * @param key    键名
	 * @param values 值序列
	 * @return 判断谓词
	 */
	@SafeVarargs
	public static <T> Predicate<IRecord> in(final String key, final T... values) {
		return rec -> {
			final var v0 = rec.get(key);
			for (var v : values)
				if (v.equals(v0))
					return true;
			return false;
		};
	}

	/**
	 * 判断键名索引idx的值是否位于values值之中
	 * 
	 * @param <T>    备选值的类型
	 * @param idx    键名索引从0开始
	 * @param values 值序列
	 * @return 判断谓词
	 */
	@SafeVarargs
	public static <T> Predicate<IRecord> in(final Number idx, final T... values) {
		final var i = idx.intValue();
		return rec -> {
			final var v0 = rec.get(i);
			for (var v : values)
				if (v.equals(v0))
					return true;
			return false;
		};
	}

	/**
	 * 判断键名key的值是否不位于values值之中
	 * 
	 * @param <T>    备选值的类型
	 * @param key    键名
	 * @param values 值序列
	 * @return 判断谓词
	 */
	@SafeVarargs
	public static <T> Predicate<IRecord> notin(final String key, T... values) {
		return rec -> !in(key, values).test(rec);
	}

	/**
	 * 判断键名索引idx的值是否不位于values值之中
	 * 
	 * @param <T>    备选值的类型
	 * @param idx    键名索引从0开始
	 * @param values 值序列
	 * @return 判断谓词
	 */
	@SafeVarargs
	public static <T> Predicate<IRecord> notin(final Number idx, T... values) {
		return rec -> !in(idx, values).test(rec);
	}

	/**
	 * 字段名判断谓词
	 * 
	 * @param idx    键名索引从0开始
	 * @param tester 键值测试器
	 * @param <T>    键值类型
	 * @return rec->boolean
	 */
	static <T> Predicate<IRecord> predicate(final int idx, final Predicate<T> tester) {
		return rec -> rec.get(idx, tester::test);
	};

	/**
	 * 字段名判断谓词
	 * 
	 * @param key    键名
	 * @param tester 键值测试器
	 * @param <T>    键值类型
	 * @return rec->boolean
	 */
	static <T> Predicate<IRecord> predicate(final String key, final Predicate<T> tester) {
		return rec -> rec.get(key, tester::test);
	};

	/**
	 * 字段变换器 mapper
	 * 
	 * @param key    键名
	 * @param mapper 键值变换器 t->u
	 * @param <T>    键值类型
	 * @param <U>    结果类型
	 * @return rec->boolean
	 */
	static <T, U> Function<IRecord, U> mapper(final String key, final Function<T, U> mapper) {
		return rec -> rec.get(key, mapper);
	};

	/**
	 * 字段变换器 mapper
	 * 
	 * @param idx    键名索引从0开始
	 * @param mapper 键值变换器 t->u
	 * @param <T>    键值类型
	 * @param <U>    结果类型
	 * @return rec->boolean
	 */
	static <T, U> Function<IRecord, U> mapper(int idx, Function<T, U> mapper) {
		return rec -> rec.get(idx, mapper);
	};

	/**
	 * 判断键名key的值是否满足于predicate
	 * 
	 * @param <T>       predicate 的参数类型
	 * @param key       键名
	 * @param predicate 判断谓词
	 * @return 判断谓词
	 */
	public static <T> Predicate<IRecord> test(final String key, final Predicate<T> predicate) {
		return IRecord.predicate(key, predicate);
	}

	/**
	 * 判断键名key的值是否不满足于predicate
	 * 
	 * @param <T>       predicate 的参数类型
	 * @param key       键名
	 * @param predicate 判断谓词
	 * @return 判断谓词
	 */
	@SuppressWarnings("unchecked")
	static <T> Predicate<IRecord> test2(final String key, final Predicate<T> predicate) {
		return rec -> !predicate.test((T) rec.get(key));
	}

	/**
	 * 行数据的子集函数<br>
	 * 视rec为一个[rec]的行集合然后用predicate进行过滤 <br>
	 * 
	 * @param predicate 行数据过滤条件
	 * @return rec->[rec]
	 */
	static Function<IRecord, Stream<IRecord>> subset(final Predicate<IRecord> predicate) {
		return rec -> rec.rowS().filter(predicate);
	}

	/**
	 * flagbools 在一个bool向量中 进行标记。 <br>
	 * 标志向量设置函数在一个 长度为n的全为b值的向量数组中[b,b,b,...,b]的 index 位置进行取反, 比如：<br>
	 * flagbools(false,2,3).apply(5) 返回[false, false, true, true, false] <br>
	 * flagbools(true,2,3).apply(5) 返回[true, true, false, false, true] <br>
	 *
	 * @param b       标志向量的默认值:true,false
	 * @param indexes 标记的索引位置: 从0开始
	 * @return 标记向量生成函数 ,(n:标志向量长度)->[b]
	 */
	static Function<Integer, Boolean[]> flagbools(Boolean b, Integer... indexes) {
		return n -> {
			if (n <= 0) {
				return new Boolean[0];
			}
			final var notb = !b;
			final var bb = new Boolean[n];
			Arrays.fill(bb, b);
			for (final var idx : indexes) {
				bb[Math.abs(idx) % n] = notb;
			}
			return bb;
		};
	}

	/**
	 * 键名过滤
	 * 
	 * @param keys 提取键名列表,键名间使用','进行分隔
	 * @return 提取idxes所在的数据
	 */
	static Function<IRecord, IRecord> select(final String keys) {
		return select(keys.split(","));
	}

	/**
	 * 键名过滤
	 * 
	 * @param keys 提取键名列表所在的数据
	 * @return 提取idxes所在的数据
	 */
	static Function<IRecord, IRecord> select(final String[] keys) {
		return r -> r.filter(keys);
	}

	/**
	 * 索引号过滤
	 * 
	 * @param idxes 索引列表,索引号从0开始
	 * @return 提取idxes所在的数据
	 */
	static Function<IRecord, IRecord> select(final Integer[] idxes) {
		return r -> r.filter(idxes);
	}

	/**
	 * 索引号过滤
	 * 
	 * @param idxes 索引列表,索引号从0开始
	 * @return 提取idxes所在的数据
	 */
	static Function<IRecord, IRecord> select2(final Integer[] idxes) {
		return r -> r.filterNot(idxes);
	}

	/**
	 * 索引号过滤
	 * 
	 * @param idxes 索引列表,索引号从0开始
	 * @return 提取idxes所在的数据
	 */
	static List<Function<IRecord, IRecord>> selects(final Integer[]... idxes) {
		return Arrays.stream(idxes).map(i -> select(i)).collect(Collectors.toList());
	}

	/**
	 * 键名过滤
	 * 
	 * @param keys 键名列表集合 [[k11,k12,k13],[k11,k12,k13],....,]
	 * @return 提取idxes所在的数据
	 */
	static List<Function<IRecord, IRecord>> selects(final String[]... keys) {
		return Arrays.stream(keys).map(k -> select(k)).collect(Collectors.toList());
	}

	/**
	 * 键名过滤
	 * 
	 * @param keys 键名列表集合 ["k11,k12,k13","k11,k12,k13",...,] 单项键名间用逗号分隔
	 * @return 提取idxes所在的数据
	 */
	static List<Function<IRecord, IRecord>> selects(final String... keys) {
		return Arrays.stream(keys).map(k -> select(k)).collect(Collectors.toList());
	}

	/**
	 * 元素提取器
	 * 
	 * @param <U>          结果类型
	 * @param name         元素名
	 * @param defaultValue 默认值
	 * @return 元素提取器
	 */
	static <U> Function<IRecord, U> getter(final String name, final U defaultValue) {
		return rec -> (U) rec.get(name, defaultValue);
	}

	/**
	 * 元素提取器
	 * 
	 * @param <U>  结果类型
	 * @param name 元素名
	 * @return 元素提取器
	 */
	static <U> Function<IRecord, U> getter(final String name) {
		return getter(name, null);
	}

	/**
	 * IRecord 的内部构建器 <br>
	 * 创建一个IRecord对象 一共分两步:<br>
	 * 1、先指定键名序列 <br>
	 * 2、然后再指定值序列 <br>
	 * 
	 * @author gbench
	 *
	 */
	class Builder {
		/**
		 * 构造函数
		 * 
		 * @param keys 字段键名 列表 , 当 keys 为 null 值的时候, 采取 excelname 方式进行 字段命名，即依次命名为 :
		 *             A,B,C,...
		 */
		public Builder(final String[] keys) {
			this.keys = new Keys(keys);
		}

		/**
		 * 构造函数 <br>
		 * 
		 * @param mapper 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
		 */
		public Builder(final BiFunction<Integer, Object, String> mapper) {
			this.keys = new Keys(mapper);
		}

		/**
		 * 构造函数
		 * 
		 * @param keys 字段键名 列表,当 keys 为 null 值的时候, 采取 excelname 方式进行 字段命名，即依次命名为 :
		 *             A,B,C,...
		 */
		public Builder(final Iterable<String> keys) {
			this(keys == null ? null : StreamSupport.stream(keys.spliterator(), false).toArray(String[]::new));
		}

		/**
		 * 构造函数 <br>
		 * 
		 * @param mapper 索引键名映射函数 (i:键名索引从0开始) -> string
		 */
		public Builder(final Function<Integer, String> mapper) {
			this((i, v) -> mapper.apply(i));
		}

		/**
		 * 构造函数
		 * 
		 * @param keys 字段键名 列表, 键名之间采用逗号分隔, 当 keys 为 null 值的时候, 采取 excelname 方式进行
		 *             字段命名，即依次命名为 : A,B,C,...
		 */
		public Builder(final String keys) {
			this(keys == null ? null : keys.split("[,]+"));
		}

		/**
		 * 构造函数 <br>
		 * 
		 * 采取 excelname 方式进行 字段命名，即依次命名为 : A,B,C,...
		 */
		public Builder() {
			this(Jdbcs::excelname);
		}

		/**
		 * 根据值序列建立一个IRecord记录结构
		 * 
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		@SafeVarargs
		public final <T> IRecord get(final T... objects) {
			return this.internal_get((Object[]) objects);
		}

		/**
		 * 根据值序列建立一个IRecord记录结构
		 *
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		public final <T> IRecord get(final Stream<T> objects) {
			return this.internal_get((Object[]) objects.toArray());
		}

		/**
		 * 根据值序列建立一个IRecord记录结构
		 *
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		public final <T> IRecord get(final Iterable<T> objects) {
			final var oo = StreamSupport.stream(objects.spliterator(), false).toArray();
			return this.internal_get(oo);
		}

		/**
		 * 采用SQL::parseFieldName 进行字段名处理<br>
		 * 根据值序列建立一个IRecord记录结构
		 * 
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		@SafeVarargs
		public final <T> IRecord get2(final T... objects) {
			return this.internal_get2((Object[]) objects);
		}

		/**
		 * 采用SQL::parseFieldName 进行字段名处理<br>
		 * 根据值序列建立一个IRecord记录结构
		 *
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		public final <T> IRecord get2(final Stream<T> objects) {
			return this.internal_get2((Object[]) objects.toArray());
		}

		/**
		 * 采用SQL::parseFieldName 进行字段名处理<br>
		 * 根据值序列建立一个IRecord记录结构
		 *
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		public final <T> IRecord get2(final Iterable<T> objects) {
			final var oo = StreamSupport.stream(objects.spliterator(), false).toArray();
			return this.internal_get2(oo);
		}

		/**
		 * 根据值序列建立一个IRecord记录结构 (get的 别名函数),非多态只有一个build,以方便使用lambda
		 *
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		@SafeVarargs
		public final <T> IRecord build(final T... objects) {
			return this.get(objects);
		}

		/**
		 * 采用SQL::parseFieldName 进行字段名处理<br>
		 * 根据值序列建立一个IRecord记录结构 (get2的 别名函数),非多态只有一个build,以方便使用lambda
		 *
		 * @param <T>     参数数组的 元素类型
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		@SafeVarargs
		public final <T> IRecord build2(final T... objects) {
			return this.get2(objects);
		}

//            /**
//             * 绘制构建器中的键名序列
//             *
//             * @return 键名序列
//             */
//            public String[] getKeys() {
//                return this.keys;
//            }

		/**
		 * IRecord Builder 的归集器
		 * 
		 * @param <T> 元素类型
		 * @return IRecord Builder 的归集器
		 */
		public final <T> Collector<T, ?, IRecord> collector() {
			return IRecord.aaclc(this::build);
		}

		/**
		 * IRecord Builder 的归集器
		 * 
		 * @param <T>        源流的元素类型
		 * @param <U>        目的流的元素类型
		 * @param t2u_mapper 预处理器值变换函数 t->u, 把 流中的元素t变换成u类型之后再传递给IRecord.Builder
		 * @return IRecord Builder 的归集器
		 */
		public final <T, U> Collector<T, ?, IRecord> collector(final Function<T, U> t2u_mapper) {
			return IRecord.ssclc(ss -> this.get(ss.map(t2u_mapper)));
		}

		/**
		 * 根据值序列建立一个IRecord记录结构
		 * 
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		private IRecord internal_get(final Object[] objects) {
			return internal_build(keys, objects);
		}

		/**
		 * 根据值序列建立一个IRecord记录结构
		 *
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		private IRecord internal_get2(final Object[] objects) {

			final var n = objects.length;
			final var _keys = new Keys(this.keys.size, i -> {
				final var key = this.keys.mapper.apply(i, objects[i % n]);
				return SQL.parseFieldName(key).str("name");
			});

			return internal_build(_keys, objects);
		}

		/**
		 * 根据值序列建立一个IRecord记录结构
		 *
		 * @param keys    键名序列, 但 kk 为 null使用才有 excelname 方式对值进行字段命名,即命名为： A,B,C,...
		 * @param objects 值序列，与键名按次序一一对应,对于 不足keys长度的值采用类似于R语言的循环遍历objects的方式进行重复利用
		 * @return IRecord 记录对象
		 */
		private static IRecord internal_build(final Keys keys, final Object[] objects) {

			final var rec = REC(); // 返回值
			if (objects == null || objects.length < 1) {
				return rec;
			}

			final var maxSize = keys.isSized() // 生成的IRecord 的记录长度的 确定逻辑
					// ? Math.min(keys.size, objects.length)
					? keys.size // 当键名序列有限长度的时候，生成记录采用 键名列表长度。
					: objects.length; // 当键名序列无限长度，生成记录采用 数据长度
			final var n = objects.length;
			Stream.iterate(0, i -> i + 1).limit(maxSize).forEach(i -> {
				final var value = objects[i % n];
				final var key = keys.mapper.apply(i, value);
				rec.add(key, value);
			});

			return rec;
		}

		/**
		 * 键名序列结构<br>
		 * 用于表示 一个键名列表 类似于 [k0,k1,k2,....] 这样的 一种数据结构：
		 */
		public class Keys {

			/**
			 * 固定长度
			 * 
			 * @param keys 键名列表
			 */
			public Keys(final String[] keys) {
				this.size = keys == null ? -1 : keys.length;
				final Function<Integer, String> _mapper = keys == null ? Jdbcs::excelname : i -> keys[i % this.size];
				this.mapper = (i, value) -> _mapper.apply(i);
			}

			/**
			 * 索引映射函数
			 * 
			 * @param mapper 索引键名映射函数 (i:键名索引从0开始) -> string
			 */
			public Keys(final Function<Integer, String> mapper) {
				this((i, value) -> mapper.apply(i));
			}

			/**
			 * 索引映射函数
			 * 
			 * @param mapper 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
			 */
			public Keys(final BiFunction<Integer, Object, String> mapper) {
				this.size = -1;
				this.mapper = mapper;
			}

			/**
			 * 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
			 * 
			 * @param size   键名列表长度, 小于1的长度表示 这是一位 无限size的 Keys 键名序列
			 * @param mapper 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
			 */
			public Keys(final int size, final Function<Integer, String> mapper) {
				this.size = size;
				this.mapper = (i, value) -> mapper.apply(i);
			}

			/**
			 * keys 是否是固定长度
			 * 
			 * @return 是否是固定长度,true 固定长度,false 无限长度
			 */
			public boolean isSized() {
				return size > 0;
			}

			/**
			 * keys 的长度
			 */
			private final int size;

			/**
			 * keys 的生成函数 <br>
			 * 索引键名映射函数 ( i : 键名索引从0开始, v : value 值对象 ) -> 键名:string
			 */
			private final BiFunction<Integer, Object, String> mapper;
		} // Keys

		/**
		 * 键名序列
		 */
		private final Keys keys;
	} // Builder

	String SHARP_VARIABLE_PATTERN = "#([a-zA-Z_][a-zA-Z0-9_]*)";// sharp 变量的此法模式
	Long MAX_STACK_TRACE_SIZE = 200L; // 最大的堆栈高度

}
