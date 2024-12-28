package gbench.util.jdbc.kvp;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import gbench.util.jdbc.Jdbcs;

/**
 * 理论上 KVPair对应了与 Tuple2是一一对应的，那为何 还要单独设计 一个 KVPair结构呢。<br>
 * KVPair 的特点 就是 为Tuple2 制定了 一种 默认的 操作位置即2号位置，我们把这个位置 value 位置<br>
 * 一些函数 比如 val2rec,corece,map等都是默认对value进行操作的。 <br>
 * 所以KVPair 是一种带有默认 位置的 Tuple2 <br>
 * 
 * 键值对儿：可以把Key-Value 视为一个值的包装器，或者是一个 单值的容器。为一个值贴上了衣蛾key的标签。 <br>
 * Key-Value 是最简单的一个 值Value容器:单值容器。二IRecord是一个多值多键容器。 <br>
 * 需要好注意 当 key为一条代表层级的路径 path 的时候, KVPair就可以用来表达 一条具有层级性质的信息 Path2Value<br>
 * 一组Path2Value的 就可以表达一个属性结构比如:<br>
 * L( KVP("/国务院/卫生部",REC("部长","张三")), KVP("/国务院/工业部",REC("部长","李四") ) 就表示了 <br>
 * 国务院 <br>
 * &nbsp; &nbsp; 卫生部 --- 部长:张三 <br>
 * &nbsp; &nbsp; 工业部 --- 部长:李四 <br>
 * 特别是当 <br>
 * 
 * @author gbench
 *
 * @param <K> 键名类型
 * @param <V> 值类型
 */
public class KVPair<K, V> extends Tuple2<K, V> {

	/**
	 * 键值对儿
	 * 
	 * @param tup 二元组对象
	 */
	public KVPair(Tuple2<K, V> tup) {
		super(tup._1, tup._2);
	}

	/**
	 * 键值对儿
	 * 
	 * @param key   键名
	 * @param value 键值
	 */
	public KVPair(K key, V value) {
		super(key, value);
	}

	/**
	 * 获取键名
	 * 
	 * @return 键名
	 */
	public K key() {
		return this._1();
	}

	/**
	 * 设置键名
	 * 
	 * @param k 键名的值
	 * @return 设置后的键名的值
	 */
	public K key(K k) {
		return this._1(k);
	}

	/**
	 * 获取当前的值
	 * 
	 * @return
	 */
	public V value() {
		return this._2();
	}

	/**
	 * 交换1号与2号位置 (k,v)->(v,k) <br>
	 * 
	 * @return (v,k)
	 */
	public KVPair<V, K> swap() {
		return KVP(this._2(), this._1());
	};

	/**
	 * 设置当前的值为v
	 * 
	 * @param v 值
	 * @return 设置后的值
	 */
	public V value(final V v) {
		return this._2(v);
	}

	/**
	 * 设置当前的值为v
	 * 
	 * @param <U>    结果类型
	 * @param mapper 映射函数
	 * @return 设置后的值
	 */
	public <U> U key(final Function<K, U> mapper) {
		return this._1(mapper);
	}

	/**
	 * 把 key 强制类型转换成U类型对象:这当key是Object对象的时候有用
	 * 
	 * @param <U>    uclass
	 * @param uclass 结果类型
	 * @return U 类型的对象
	 */
	public <U> U key(Class<U> uclass) {
		return this._1(uclass);
	}

	/**
	 * 
	 * 把 key 视为X类型 给予 应用mapper 函数进行变换，当 vlalue 为 Object的时候很有用
	 * 
	 * @param <X>    mapper的参数类型
	 * @param <U>    mapper的结果
	 * @param mapper 变换函数
	 * @return U 类型的对象
	 */
	@SuppressWarnings("unchecked")
	public <X, U> U key2(final Function<X, U> mapper) {
		return mapper.apply((X) this._1());
	}

	/**
	 * 把value值根据指定方法 mapper 进行变换
	 * 
	 * @param <U>    mapper的结果
	 * @param mapper 变换函数
	 * @return U 类型的对象
	 */
	public <U> U map(final Function<V, U> mapper) {
		return this._2(mapper);
	}

	/**
	 * 把 value 视为X类型 给予 应用mapper 函数进行变换，当 vlalue 为 Object的时候 很有用.这是 value2的别名
	 * 
	 * @param <U>    mapper的结果
	 * @param mapper 变换函数
	 * @return U 类型的对象
	 */
	public <X, U> U map2(final Function<X, U> mapper) {
		return this.value2(mapper);
	}

	/**
	 * map的别名 把value值根据指定方法 mapper 进行变换
	 * 
	 * @param <U>    mapper的结果
	 * @param mapper 变换函数
	 * @return U 类型的对象
	 */
	public <U> U value(final Function<V, U> mapper) {
		return map(mapper);
	}

	/**
	 * 
	 * 把 value 视为X类型 给予 应用mapper 函数进行变换，当 vlalue 为 Object的时候很有用
	 * 
	 * @param <X>    mapper的参数类型
	 * @param <U>    mapper的结果
	 * @param mapper 变换函数
	 * @return U 类型的对象
	 */
	@SuppressWarnings("unchecked")
	public <X, U> U value2(final Function<X, U> mapper) {
		return mapper.apply((X) this._2());
	}

	/**
	 * 把值转换成IRecord：当值是IRecord类型直接返回,是Map类型是采用REC包装返回，其他类型 返回 null.<br>
	 * 
	 * @return IReord类型的值对象。
	 */
	public IRecord val2rec() {
		if (this.value() instanceof IRecord)
			return (IRecord) this.value();
		else if (this.value() instanceof Map)
			return IRecord.REC((Map<?, ?>) this.value());
		else
			return null;
	}

	/**
	 * 把value 强制类型转换成U类型对象:这当value 是 Object对象的时候有用
	 * 
	 * @param <U>    结果类型类型
	 * @param uclass 结果类型类型类
	 * @return U 类型的对象
	 */
	public <U> U value(final Class<U> uclass) {
		return this._2(uclass);
	}

	/**
	 * 把value 强制类型转换成U类型对象:这当value 是 Object对象的时候有用
	 * 
	 * @param <U>    结果类型
	 * @param uclass 结果类型
	 * @return U 类型的对象
	 */
	public <U> U corece(Class<U> uclass) {
		return value(uclass);
	}

	/**
	 * 实现 mutator.apply(this)
	 * 
	 * @param mutator 映射函数
	 */
	public <U> U mutate(final Function<KVPair<K, V>, U> mutator) {
		return mutator.apply(this);
	}

	/**
	 * 把 key 视为 一个 Function&lt;V,U&gt; 函数，调用 用 value 来调用 key行数 获取执行结果。<br>
	 * 即 实现 一个 key(value) 形式的 数据调用 <br>
	 * U类型 需要在 语句外部指定例如 final var Integer = kvp.evaluate(); <br>
	 * 
	 * @param <T> key 的函数类型是一个 K-&gt;U 类型函数，这是 一个辅助类型，不需要传递的。
	 * @param <U> 返回的结果类型
	 * @return U 类型的数据对象
	 */
	@SuppressWarnings("unchecked")
	public <T extends Function<V, U>, U> U evaluate() {
		return ((T) this.key()).apply(this.value());
	}

	/**
	 * 把 key 视为 一个 Function&lt;V,U&gt; 函数，调用 用 value 来调用 key行数 获取执行结果。<br>
	 * 
	 * @param <T>    key 的函数类型
	 * @param <U>    结果类型
	 * @param uclazz 结果类的类型 class
	 * @return U 类型的数据对象
	 */
	public <T extends Function<V, U>, U> U evaluate(final Class<U> uclazz) {
		return this.evaluate();
	}

	/**
	 * Functor 函数
	 * 
	 * @param <X>    目标结果第一位置 类型
	 * @param <Y>    目标结果第二位置 类型
	 * @param mapper 变换函数 (t,u)-&gt;(x,y)
	 * @return (x,y)二元组
	 */
	public <X, Y> KVPair<X, Y> fmapK(final Function<KVPair<K, V>, KVPair<X, Y>> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象
	 * 
	 * @param <X> 结果的1号 位置类型
	 * @param <Y> 结果的2号位置类型
	 * @param k2x 1号位置的变换函数
	 * @param v2y 2号位置的变换函数
	 * @return KVP(X,Y) 类型的对象。
	 */
	public <X, Y> KVPair<X, Y> fmap(final Function<K, X> k2x, final Function<V, Y> v2y) {
		return KVP(super.fmap(k2x, v2y));
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象
	 * 
	 * @param <X> 结果的1号 位置类型
	 * @param k2x 1号位置的变换函数
	 * @return KVP(X,U) 类型的对象。
	 */
	public <X> KVPair<X, V> fmap1(final Function<K, X> k2x) {
		return KVP(super.fmap1(k2x));
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象：对于 Object类型的数据很有用。比如 IRecord的kvps
	 * 
	 * @param <X>   结果的1号 位置类型
	 * @param clazz X 的数据类型
	 * @return KVP(X,U) 类型的对象。
	 */
	public <X> KVPair<X, V> fmap1(Class<X> clazz) {
		return this.fmap1(Jdbcs.FC(clazz));
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象
	 * 
	 * @param <Y> 结果的2号位置类型
	 * @param v2y 2号位置的变换函数
	 * @return KVP(T,Y) 类型的对象。
	 */
	public <Y> KVPair<K, Y> fmap2(final Function<V, Y> v2y) {
		return KVP(super.fmap2(v2y));
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象:对于 Object类型的数据很有用。比如 IRecord的kvps
	 * 
	 * @param <Y>   结果的2号位置类型
	 * @param clazz 目标类型的结果
	 * @return KVP(T,Y) 类型的对象。
	 */
	public <Y> KVPair<K, Y> fmap2(final Class<Y> clazz) {
		return this.fmap2(Jdbcs.FC(clazz));
	}

	/**
	 * 格式化输出
	 */
	public String toString() {
		return Jdbcs.format("({0}:{1})", _1(), _2());
	}

	/**
	 * 把一个列表拆分成 (head,tail) 结构. 只有一个元素的列表 [a] 返回 (a,[]) <br>
	 * 
	 * @param <V>  元素类型
	 * @param list 列表元素
	 * @return (v,[v])
	 */
	public static <V> KVPair<V, List<V>> head2tail(final List<V> list) {
		return new KVPair<>(Tuple2.head2tail(list));
	}

	/**
	 * 把一个列表拆分成 (initial,last) 结构 <br>
	 * 只有一个元素的列表 [a] 返回 ([a],null) <br>
	 * 
	 * @param <V>  元素类型
	 * @param list 列表元素
	 * @return ([v],v)
	 */
	public static <V> KVPair<List<V>, V> initial2last(final List<V> list) {
		return new KVPair<>(Tuple2.initial2last(list));
	}

	/**
	 * 把一个列表拆分成 一个tuple
	 * 
	 * @param <V>  元素类型
	 * @param list 列表元素
	 * @param n    把 索引(从0开始)大于等于n 的元素放置到 list2
	 * @return (list1,list2)
	 */
	public static <V> Tuple2<List<V>, List<V>> split(final List<V> list, int n) {
		return new KVPair<>(Tuple2.split(list, n));
	}

	/**
	 * 把一个列表拆分成 一个tuple
	 * 
	 * @param <V>       元素类型
	 * @param list      列表元素
	 * @param predicate (index,v)->boolean, true 表示 list1,false 表示 list2 ,索引从0开始
	 * @return (list1,list2)
	 */
	public static <V> KVPair<List<V>, List<V>> split(final List<V> list, final BiPredicate<Integer, V> predicate) {
		return new KVPair<>(Tuple2.split(list, predicate));
	}

	/**
	 * 创建一个键值对儿
	 * 
	 * @param <K1> 键类型
	 * @param <V1> 值类型
	 * @param k1   键名
	 * @param v1   键值
	 * @return 键值对儿(k1,v1)
	 */
	public static <K1, V1> KVPair<K1, V1> KVP(final K1 k1, final V1 v1) {
		return new KVPair<>(k1, v1);
	}

	/**
	 * 创建一个键值对儿
	 * 
	 * @param <K1>   键类型
	 * @param <V1>   值类型
	 * @param tuple2 2元组类型
	 * @return 键值对儿(k1,v1)
	 */
	public static <K1, V1> KVPair<K1, V1> KVP(final Tuple2<K1, V1> tuple2) {
		return new KVPair<>(tuple2);
	}

	/**
	 * 创建一个键值对儿
	 *
	 * @param <T> 元素类型
	 * @param tt  键值序列
	 * @return 键值对儿(tt[0], tt[1])
	 */
	public static <T> KVPair<T, T> KVP(final T[] tt) {
		if (tt == null || tt.length < 1)
			return null;
		final var n = tt.length;
		return new KVPair<>(tt[0 % n], tt[1 % n]);
	}

	/**
	 * kvp collector , List 类型的 mapper
	 *
	 * @param <P>    KVPair 类型的数据类型
	 * @param <K>    键名
	 * @param <V>    值
	 * @param <U>    结果类型
	 * @param mapper 映射对象
	 * @return U类型的结果
	 */
	public static <P extends KVPair<K, V>, K, V, U> Collector<P, ?, U> kvpclc(
			final Function<List<KVPair<K, V>>, U> mapper) {

		return Collector.of(LinkedList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, mapper);
	}

	/**
	 * kvp collector , Stream 类型的 mapper
	 *
	 * @param <P>    KVPair 类型的数据类型
	 * @param <K>    键名
	 * @param <V>    值
	 * @param <U>    结果类型
	 * @param mapper 映射对象
	 * @return U类型的结果
	 */
	public static <P extends KVPair<K, V>, K, V, U> Collector<KVPair<K, V>, ?, U> kvpSclc(
			final Function<Stream<KVPair<K, V>>, U> mapper) {

		return kvpclc(ll -> mapper.apply(ll.stream()));
	}

	private static final long serialVersionUID = 1110300882502209203L;
}