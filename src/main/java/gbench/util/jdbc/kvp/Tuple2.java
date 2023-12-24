package gbench.util.jdbc.kvp;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 二元组(t,u)
 * 
 * @author gbench
 *
 * @param <T> 1号位置元素类型
 * @param <U> 2号位置元素类型
 */
public class Tuple2<T, U> implements Serializable {

	/**
	 * 默认构造函数
	 */
	public Tuple2() {
	}

	/**
	 * 构造函数
	 * 
	 * @param t 1号位置的元素
	 * @param u 2号位置的严肃
	 */
	public Tuple2(final T t, final U u) {
		this._1 = t;
		this._2 = u;
	}

	/**
	 * 1号位置的元素值
	 * 
	 * @return 1号位置的元素值
	 */
	public T _1() {
		return _1;
	}

	/**
	 * 对 1号位置进行变换
	 *
	 * @param <V>    返回值的类型 也就是mapper 的结果类型
	 * @param mapper 映射函数 t->v
	 * @return V 类型的数据
	 */
	public <V> V _1(final Function<T, V> mapper) {
		return mapper.apply(_1);
	}

	/**
	 * 把1号位置强制类型转换为V类型
	 *
	 * @param <V>    目标结果类型
	 * @param vclass 结果类型
	 * @return V类型的结果
	 */
	@SuppressWarnings("unchecked")
	public <V> V _1(final Class<V> vclass) {
		return (V) this._1;
	}

	/**
	 * 设置1号位置的元素的值
	 *
	 * @param t 待设置的值
	 * @return 设置后的值
	 */
	public T _1(final T t) {
		return _1 = t;
	}

	/**
	 * 2号位置的元素值
	 *
	 * @return 2号位置的元素值
	 */
	public U _2() {
		return _2;
	}

	/**
	 * 使用mapper 对值类型进行变换
	 *
	 * @param <V>    目标结果类型
	 * @param mapper u->v
	 * @return V类型的结果
	 */
	public <V> V _2(Function<U, V> mapper) {
		return mapper.apply(_2);
	}

	/**
	 * 把2号位置强制类型转换为V类型
	 *
	 * @param <V>    目标结果类型
	 * @param vclass 结果类型
	 * @return V类型的结果
	 */
	@SuppressWarnings("unchecked")
	public <V> V _2(final Class<V> vclass) {
		return (V) this._2;
	}

	/**
	 * 设置2号位置的元素的值
	 *
	 * @param u 待设置的值
	 * @return 设置后的值
	 */
	public U _2(final U u) {
		return _2 = u;
	}

	/**
	 * 交换1号与2号位置 (t,u)->(u,t) <br>
	 *
	 * @return (u, t)
	 */
	public Tuple2<U, T> swap() {
		return TUP2(this._2, this._1);
	}

	/**
	 * Functor 函数
	 *
	 * @param <X>    目标结果第一位置 类型
	 * @param <Y>    目标结果第二位置 类型
	 * @param mapper 变换函数 (t,u)->(x,y)
	 * @return (x, y)二元组
	 */
	public <X, Y> Tuple2<X, Y> fmap(final Function<Tuple2<T, U>, Tuple2<X, Y>> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象
	 *
	 * @param <X> 结果的1号 位置类型
	 * @param <Y> 结果的2号位置类型
	 * @param t2x 1号位置的变换函数
	 * @param u2y 2号位置的变换函数
	 * @return TUP2(X, Y) 类型的对象。
	 */
	public <X, Y> Tuple2<X, Y> fmap(final Function<T, X> t2x, final Function<U, Y> u2y) {
		return TUP2(t2x.apply(_1), u2y.apply(_2));
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象
	 *
	 * @param <X> 结果的1号 位置类型
	 * @param t2x 1号位置的变换函数
	 * @return TUP2(X, U) 类型的对象。
	 */
	public <X> Tuple2<X, U> fmap1(final Function<T, X> t2x) {
		return TUP2(t2x.apply(_1), _2);
	}

	/**
	 * 使用指定变换函数 去 变换各个位置上的 元素内容，并生成一个新的 对象
	 *
	 * @param <Y> 结果的2号位置类型
	 * @param u2y 2号位置的变换函数
	 * @return TUP2(T, Y) 类型的对象。
	 */
	public <Y> Tuple2<T, Y> fmap2(final Function<U, Y> u2y) {
		return TUP2(_1, u2y.apply(_2));
	}

	/**
	 * 批量设置1号位置
	 *
	 * @param <V> 1号位置元素
	 * @param vv  1号位置元素集合
	 * @return {(v,u)}
	 */
	public <V> List<Tuple2<V, U>> multiSet1(Collection<V> vv) {
		return zip(vv, Collections.singletonList(this._2), true);
	}

	/**
	 * 批量设置1号位置
	 *
	 * @param <V> 1号位置元素
	 * @param vv  1号位置元素集合
	 * @return {(v,u)}
	 */
	public <V> List<Tuple2<V, U>> multiSet1(final V[] vv) {
		return zip(Arrays.asList(vv), Collections.singletonList(this._2), true);
	}

	/**
	 * 批量设置2号位置
	 *
	 * @param <V> 二号位置元素
	 * @param vv  二号位置元素集合
	 * @return {(t,v)}
	 */
	public <V> List<Tuple2<T, V>> multiSet2(final Collection<V> vv) {
		return zip(Collections.singletonList(this._1), vv, true);
	}

	/**
	 * 批量设置2号位置
	 *
	 * @param <V> 二号位置元素
	 * @param vv  二号位置元素集合
	 * @return {(t,v)}
	 */
	public <V> List<Tuple2<T, V>> multiSet2(final V[] vv) {
		return zip(Collections.singletonList(this._1), Arrays.asList(vv), true);
	}

	/**
	 * TUP2(2,1).cons(3) <br>
	 * 对 2,1,追加一个 新元素 形成 3-->(2-->1) <br>
	 * 模仿 LISP的cons函数 用v来做新的链表的表头 <br>
	 *
	 * @param <V> 新追加的元素的类型
	 * @param v   新追加的元素
	 * @return 0-->(1-->2)
	 */
	public <V> Tuple2<V, Tuple2<T, U>> cons(final V v) {
		return TUP2(v, TUP2(this._1, this._2));
	}

	/**
	 * 对 1-->2,追加一个 新元素 形成 1-->(2-->3) <br>
	 * 需要注意当：U为Tuple2结构的 结果可会造成 非预期之感觉: <br>
	 * (1,2).append(3).append(4) 的结果是：(1,((2,3),4)), 当 flatMap的时候，会出现 1,4,2,3 <br>
	 *
	 * @param <V> 新追加的元素的类型
	 * @param v   新追加的元素
	 * @return 1-->(2-->3)
	 */
	public <V> Tuple2<T, Tuple2<U, V>> append(final V v) {
		return TUP2(this._1, TUP2(this._2, v));
	}

	/**
	 * 强制转换成T类型的数组元素 把1号二号元素封装成T类型的列表
	 *
	 * @return T类型的列表元素[t, u],
	 *         1号元素对应list的第0个元素，2号元素对应list的第1个元素，列表元素可能包含空元素，代表这一条边(把元组视为边)没有后继或是前驱元素。
	 */
	@SuppressWarnings({ "unchecked" })
	public List<T> tt() {
		return Arrays.asList((T) this._1, (T) this._2);
	}

	/**
	 * 把1号二号元素封装成Object类型的列表
	 *
	 * @return Object的列表元素[t, u], 1号元素对应list的第0个元素，2号元素对应list的第1个元素，
	 */
	public List<Object> oo() {
		return Arrays.asList(this._1, this._2);
	}

	/**
	 * 把1号二号元素封装成Object类型的数组
	 *
	 * @return Object类型的数组:[t,u],1号元素对应list的第0个元素，2号元素对应list的第1个元素，
	 */
	public Object[] toArray() {
		return this.toArray(Object[]::new);
	}

	/**
	 * 把1号二号元素封装成X类型的数组
	 *
	 * @param <X>       数组的元素类型
	 * @param generator 数组生成器
	 * @return X类型的数组:[t,u],1号元素对应list的第0个元素，2号元素对应list的第1个元素，
	 */
	public <X> X[] toArray(final IntFunction<X[]> generator) {
		return Arrays.asList(this._1, this._2).toArray(generator);
	}

	/**
	 * 把1号二号元素封装成Object类型的流
	 *
	 * @return Object类型的流:[t,u],1号元素对应list的第0个元素，2号元素对应list的第1个元素，
	 */
	public Stream<Object> stream() {
		return Stream.of(this._1, this._2);
	}

	/**
	 * 把1号二号元素封装成X类型的流
	 *
	 * @param <X>   元素类型
	 * @param clazz 元素类型类型
	 * @return X类型的流:[t,u],1号元素对应list的第0个元素，2号元素对应list的第1个元素，
	 */
	@SuppressWarnings("unchecked")
	public <X> Stream<X> stream(final Class<X> clazz) {
		return Arrays.stream(this.toArray(n -> (X[]) Array.newInstance(clazz, 2)));
	}

	/**
	 * 把一个二元组转换成一个 目标类型
	 *
	 * @param <O>    目标类型
	 * @param mapper 二元组转换器器(t,u)->o
	 * @return O 类型的结果
	 */
	public <O> O map(final BiFunction<T, U, O> mapper) {
		return mapper.apply(this._1, this._2);
	}

	/**
	 * 对列表的元素进行平铺展开,用于 Tuple2中俄元素类型为 也是Tuple2类型的时候有用。<br>
	 * 比如:<br>
	 * TUP2(TUP2(1,2),TUP2(3,4)).flatMap(e->e) 将 返回:[3, 4, 1, 2] 即 对于 非 Tuple2元素 按照
	 * _1,_2的顺序 而 Tuple2类型的元素按照 _1,_2 的顺序（实现<br>
	 * 采用了堆栈进行层次递进)。进行按展开. <br>
	 *
	 * @param mapper 结果变换函数:对 _1,_2的值进行变换的函数。
	 * @return 0-->(1-->2)
	 */
	public <V> List<V> flatMap(final Function<Object, V> mapper) {
		final var tt = new LinkedList<V>();
		final var stack = new Stack<Tuple2<?, ?>>();

		stack.push(this);
		while (!stack.empty()) {
			final var tup = stack.pop();
			Stream.of(tup._1, tup._2).forEach(e -> {
				if (e instanceof Tuple2) {
					stack.push((Tuple2<?, ?>) e);
				} else {
					tt.add(mapper.apply(e));
				} // if
			});
		} // while

		return tt;
	}

	/**
	 * 使用自身作为1位元素进行 zip
	 *
	 * @param <V> 新2号位置的元素的类型
	 * @param vv  新2号位置元素
	 * @return {((T,U),V)} 结构的数组
	 */
	public <V> List<Tuple2<Tuple2<T, U>, V>> lzip(final Collection<V> vv) {
		return zip(Collections.singletonList(this), vv, true);
	}

	/**
	 * 2号位zip 使用自身作为1位元素进行 zip
	 *
	 * @param <V> 新2号位置的元素的类型
	 * @param vv  新2号位置元素
	 * @return {(T,(U,V))} 结构的数组
	 */
	public <V> List<Tuple2<T, Tuple2<U, V>>> lzip0(final Collection<V> vv) {
		final var aa = zip(Collections.singletonList(this._2), vv, true);
		return zip(Collections.singletonList(this._1), aa, true);
	}

	/**
	 * 使用自身作为2位元素进行 zip
	 *
	 * @param <V> 新1位的元素的类型
	 * @param vv  新1位元素集合
	 * @return {(V,(T,U))} 结构的数组
	 */
	public <V> List<Tuple2<V, Tuple2<T, U>>> rzip(final Collection<V> vv) {
		return zip(vv, Collections.singletonList(this), true);
	}

	/**
	 * 2号位zip 使用自身作为1位元素进行 zip
	 *
	 * @param <V> 新2号位置的元素的类型
	 * @param vv  新2号位置元素
	 * @return {(U,(T,V)} 结构的数组
	 */
	public <V> List<Tuple2<U, Tuple2<T, V>>> rzip0(final Collection<V> vv) {
		final var m = this.swap().lzip0(vv);
		return m;
	}

	/**
	 * hashCode
	 * 
	 * @return hashCode
	 */
	public int hashCode() {
		return _1 == null ? Integer.valueOf(0).hashCode() : this._1().hashCode();
	}

	/**
	 * @param obj 数据对象
	 * @return 判断两个对象相等
	 */
	public boolean equals(final Object obj) {
		if (obj instanceof Tuple2) {// 同类比较
			final var tup2 = (Tuple2<?, ?>) obj;
			if (_1 == null && tup2._1 == null) {
				return _2 == null && tup2._2 == null;
			}
			if (_1 == null)
				return false;
			if (_2 == null)
				return false;
			return _1.equals(tup2._1) && _2.equals(tup2._2);
		}
		return _1 != null && _1().equals(obj);
	}

	/**
	 * 格式化输出
	 * 
	 * @return 格式化输出
	 */
	public String toString() {
		return _1() + " --> " + _2();
	}

	/**
	 * 创建一个二元组
	 * 
	 * @param <M> 1号位置的元素类型
	 * @param <N> 2号位置的元素类型
	 * @param m   1号位置的元素的值
	 * @param n   2号位置的元素的值
	 * @return (m,n) 的二元组
	 */
	public static <M, N> Tuple2<M, N> TUP2(final M m, final N n) {
		return Tuple2.of(m, n);
	}

	/**
	 * 创建一个二元组
	 * 
	 * @param <M> 1号位置的元素类型
	 * @param <N> 2号位置的元素类型
	 * @param m   1号位置的元素的值
	 * @param n   2号位置的元素的值
	 * @return (m,n) 的二元组
	 */
	public static <M, N> Tuple2<M, N> of(final M m, final N n) {
		return new Tuple2<>(m, n);
	}

	/**
	 * 创建一个二元组
	 * 
	 * @param <M> 1号位置的元素类型
	 * @param <N> 2号位置的元素类型
	 * @param m   1号位置的元素的值
	 * @param n   2号位置的元素的值
	 * @return (m,n) 的二元组
	 */
	public static <M, N> Tuple2<M, N> P(final M m, final N n) {
		return Tuple2.of(m, n);
	}

	/**
	 * 创建一个二元组
	 *
	 * @param <T> 元素类型
	 * @param tt  键值序列
	 * @return 元组(tt[0], tt[1])
	 */
	public static <T> Tuple2<T, T> TUP2(final T[] tt) {
		if (tt == null || tt.length < 1) {
			return null;
		}
		final var n = tt.length;
		return TUP2(tt[0 % n], tt[1 % n]);
	}

	/**
	 * 把一个字符串str,视作一个由separator分隔出来的列表。把第一项提取在tuple第一项,其余放在tuple第二项 比如 abc/efg/hij
	 * 分解成 abc --> efg/hij
	 * 
	 * @param str       检测的字符串
	 * @param separator 分割符号
	 * @return
	 */
	public static Tuple2<String, String> TUPLE2(final String str, final String separator) {
		int p = str.indexOf(separator);
		Tuple2<String, String> tup = new Tuple2<>();
		if (p < 0) {
			tup._1(str);
		} else {
			tup._1(str.substring(0, p));
			if (p + 1 < str.length())
				tup._2(str.substring(p + 1));
		} // if
		return tup;
	}

	/**
	 * 拼接成一个键值对(默认循环补位）
	 *
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合
	 * @param vv  2号位置元素结合
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> List<Tuple2<K, V>> zip(final Collection<K> kk, final Collection<V> vv) {
		return zip(kk, vv, true);
	}

	/**
	 * 拼接成一个键值对
	 * 
	 * @param <K>     1号位置元素类型
	 * @param <V>     2号位置元素类型
	 * @param kk      1号位置元素集合
	 * @param vv      2号位置元素结合
	 * @param recycle 是否进行循环补位
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> List<Tuple2<K, V>> zip(final Collection<K> kk, final Collection<V> vv, final boolean recycle) {
		final var tups = new LinkedList<Tuple2<K, V>>();
		final var kitr = kk.iterator();
		final var vitr = vv.iterator();
		K pre_k = null;
		V pre_v = null;
		final var k_size = kk.size();
		final var v_size = vv.size();
		int i = 0;// 统计计数
		while (kitr.hasNext() || vitr.hasNext()) {
			var k = kitr.hasNext() ? kitr.next() : null;
			var v = vitr.hasNext() ? vitr.next() : null;
			if (recycle && k == null) {
				k = pre_k;
				k = tups.get(i % k_size)._1();// 覆盖掉pre_k的值，这是为了去除pre_k unused的警告
			}
			if (recycle && v == null) {
				v = pre_v;
				v = tups.get(i % v_size)._2();// 覆盖掉pre_v的值，这是为了去除pre_v unused的警告
			}
			tups.add(TUP2(k, v));
			pre_k = k;
			pre_v = v;
			i++;
		} // while

		return tups;
	}

	/**
	 * 拼接成一个键值对
	 * 
	 * @param <K>     1号位置元素类型
	 * @param <V>     2号位置元素类型
	 * @param kk      1号位置元素集合数组
	 * @param vv      2号位置元素结合数组
	 * @param recycle 是否进行循环补位
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> List<Tuple2<K, V>> zip(final K[] kk, final V[] vv, final boolean recycle) {
		return zip(Arrays.asList(kk), Arrays.asList(vv), recycle);
	}

	/**
	 * 拼接成一个键值对
	 * 
	 * @param <K>     1号位置元素类型
	 * @param <V>     2号位置元素类型
	 * @param <U>     返回的结果类型
	 * @param kk      1号位置元素集合数组
	 * @param vv      2号位置元素结合数组
	 * @param recycle 是否进行循环补位
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V, U> U zip(final K[] kk, final V[] vv, final boolean recycle, U identity,
			final BiFunction<U, Tuple2<K, V>, U> accumulator, BinaryOperator<U> combiner) {
		return zip(Arrays.asList(kk), Arrays.asList(vv), recycle).stream().reduce(identity, accumulator, combiner);
	}

	/**
	 * 拼接成一个键值对
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param <U> 返回的结果类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V, U> U zip(final Collection<K> kk, final Collection<V> vv, final boolean recycle, U identity,
			final BiFunction<U, Tuple2<K, V>, U> accumulator, BinaryOperator<U> combiner) {
		return zip(kk, vv, recycle).stream().reduce(identity, accumulator, combiner);
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K>         1号位置元素类型
	 * @param <V>         2号位置元素类型
	 * @param <U>         返回的结果类型
	 * @param kk          1号位置元素集合数组
	 * @param vv          2号位置元素结合数组
	 * @param identity    起始点
	 * @param accumulator 累加器
	 * @param combiner    中间值合并器
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V, U> U zip(final Collection<K> kk, final Collection<V> vv, final U identity,
			final BiFunction<U, Tuple2<K, V>, U> accumulator, final BinaryOperator<U> combiner) {
		return zip(kk, vv, true).stream().reduce(identity, accumulator, combiner);
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K>         1号位置元素类型
	 * @param <V>         2号位置元素类型
	 * @param <U>         返回的结果类型
	 * @param kk          1号位置元素集合数组
	 * @param vv          2号位置元素结合数组 recycle 是否进行循环补位
	 * @param identity    起始点
	 * @param accumulator 累加器
	 * @param combiner    中间值合并器
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V, U> U zip(final K[] kk, final V[] vv, final U identity,
			final BiFunction<U, Tuple2<K, V>, U> accumulator, final BinaryOperator<U> combiner) {
		return zip(Arrays.asList(kk), Arrays.asList(vv), true).stream().reduce(identity, accumulator, combiner);
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> List<Tuple2<K, V>> zip(final K[] kk, final V[] vv) {
		return zip(Arrays.asList(kk), Arrays.asList(vv), true);
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2tups(final K[] kk, final V[] vv) {
		return zip2stream(kk, vv, true);
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2tups(final Stream<K> kk, final Stream<V> vv) {
		return zip2stream(kk, vv, true);
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2tups(final Collection<K> kk, final Collection<V> vv) {
		return zip2stream(kk, vv);
	}

	/**
	 * 拼接成一个键值对的流 recycle 默认循环补位
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2stream(final Collection<K> kk, final Collection<V> vv) {
		return zip(kk, vv, true).stream();
	}

	/**
	 * 拼接成一个键值对
	 * 
	 * @param <K>     1号位置元素类型
	 * @param <V>     2号位置元素类型
	 * @param kk      1号位置元素集合数组
	 * @param vv      2号位置元素结合数组
	 * @param recycle 是否进行循环补位
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2stream(final K[] kk, final V[] vv, final boolean recycle) {
		return zip(Arrays.asList(kk), Arrays.asList(vv), recycle).stream();
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K> 1号位置元素类型
	 * @param <V> 2号位置元素类型
	 * @param kk  1号位置元素集合数组
	 * @param vv  2号位置元素结合数组
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2stream(final K[] kk, final V[] vv) {
		return zip(Arrays.asList(kk), Arrays.asList(vv), true).stream();
	}

	/**
	 * 拼接成一个键值对 recycle 默认循环补位
	 * 
	 * @param <K>     1号位置元素类型
	 * @param <V>     2号位置元素类型
	 * @param kk      1号位置元素集合数组
	 * @param vv      2号位置元素结合数组
	 * @param recycle 是否进行循环补位
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2stream(final Stream<K> kk, final Stream<V> vv,
			final boolean recycle) {
		return zip(kk.collect(Collectors.toList()), vv.collect(Collectors.toList()), true).stream();
	}

	/**
	 * 拼接成一个键值对的流 recycle 默认循环补位
	 * 
	 * @param <K>     1号位置元素类型
	 * @param <V>     2号位置元素类型
	 * @param kk      1号位置元素集合数组
	 * @param vv      2号位置元素结合数组
	 * @param recycle 是否进行循环补位
	 * @return [[k1,v1],[k2,v2],...] 的二元组集合。
	 */
	public static <K, V> Stream<Tuple2<K, V>> zip2stream(final Collection<K> kk, final Collection<V> vv,
			final boolean recycle) {
		return zip(kk, vv, true).stream();
	}

	/**
	 * tup collector, mapper 的数据类型为 List
	 *
	 * @param <P>    Tuple2类型的数据类型
	 * @param <K>    键名
	 * @param <V>    值
	 * @param <U>    结果类型
	 * @param mapper 映射对象
	 * @return U类型的结果
	 */
	public static <P extends Tuple2<K, V>, K, V, U> Collector<P, ?, U> tupclc(
			final Function<List<Tuple2<K, V>>, U> mapper) {
		return Collector.of(LinkedList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, mapper);
	}

	/**
	 * tup collector , mapper 的数据类型为 Stream
	 *
	 * @param <P>    Tuple2类型的数据类型
	 * @param <K>    键名
	 * @param <V>    值
	 * @param <U>    结果类型
	 * @param mapper 映射对象
	 * @return U类型的结果
	 */
	public static <P extends Tuple2<K, V>, K, V, U> Collector<P, ?, U> tupSclc(
			final Function<Stream<Tuple2<K, V>>, U> mapper) {
		return tupclc(ll -> mapper.apply(ll.stream()));
	}

	/**
	 * 把一个列表拆分成 (head,tail) 结构. 只有一个元素的列表 [a] 返回 (a,[]) <br>
	 *
	 * @param <V>  元素类型
	 * @param list 列表元素
	 * @return (v, [v])
	 */
	public static <V> Tuple2<V, List<V>> head2tail(final List<V> list) {
		if (list == null)
			return null;
		if (list.size() < 2)
			return TUP2(list.size() < 1 ? null : list.get(0), Collections.emptyList());

		final var x = split(list, 0);
		return TUP2(x._1.get(0), x._2());
	}

	/**
	 * 把一个列表拆分成 (initial,last) 结构 <br>
	 * 只有一个元素的列表 [a] 返回 ([a],null) <br>
	 *
	 * @param <V>  元素类型
	 * @param list 列表元素
	 * @return ([v], v)
	 */
	public static <V> Tuple2<List<V>, V> initial2last(final List<V> list) {
		if (list == null)
			return null;
		if (list.size() < 2)
			return TUP2(list, null);
		final var x = split(list, list.size() - 2);
		return TUP2(x._1(), x._2().get(0));
	}

	/**
	 * 把一个列表拆分成 一个kvpair
	 *
	 * @param <V>  元素类型
	 * @param list 列表元素
	 * @param n    把 索引(从0开始)大于等于n 的元素放置到 list2
	 * @return (list1, list2)
	 */
	public static <V> Tuple2<List<V>, List<V>> split(final List<V> list, int n) {
		return split(list, (idx, v) -> idx <= n);
	}

	/**
	 * 把一个列表拆分成 一个kvpair
	 *
	 * @param <V>       元素类型
	 * @param list      列表元素
	 * @param predicate (index,v)->boolean, true 表示 list1,false 表示 list2 ,索引从0开始
	 * @return (list1, list2)
	 */
	public static <V> Tuple2<List<V>, List<V>> split(final List<V> list, final BiPredicate<Integer, V> predicate) {
		final var itr = list.iterator();
		final var list1 = new LinkedList<V>();
		final var list2 = new LinkedList<V>();
		var i = 0;
		while (itr.hasNext()) {
			final var elem = itr.next();
			(predicate.test(i++, elem) ? list1 : list2).add(elem);
		}
		return TUP2(list1, list2);
	}

	/**
	 * make list <br>
	 * 使用pattern 把一个字符串分解成 列表
	 *
	 * @param pattern 分解模式,默认的 分隔标识为 "[\\s,/\\.\\\\]+"
	 * @return 使用pattern分解list
	 */
	public static Function<String, List<String>> mkll(final String pattern) {
		final var defaultPattern = "[\\s,/\\.\\\\]+";
		return line -> Arrays.asList(line.split(pattern == null ? defaultPattern : pattern));
	}

	private static final long serialVersionUID = -9103479443503056191L;
	protected T _1; // 1号位置元素
	protected U _2; // 2号位置元素
}