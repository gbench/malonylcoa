package gbench.util.lisp;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.type.Types;

/**
 * 二维元组 (t,u) 仅含有两个元素的数据结构，形式简单 但是 内涵极为丰富
 *
 * @param <T> 第一位置 元素类型
 * @param <U> 第而位置 元素类型
 */
public class Tuple2<T, U> implements Iterable<Object>, Comparable<Tuple2<T, U>> {

	/**
	 * @param _1 第一位置元素
	 * @param _2 第二位置元素
	 */
	public Tuple2(final T _1, final U _2) {
		this._1 = _1;
		this._2 = _2;
	}

	/**
	 * 返回第一元素
	 *
	 * @return 第一元素
	 */
	public T _1() {
		return this._1;
	}

	/**
	 * 返回 第二元素
	 *
	 * @return 第二元素
	 */
	public U _2() {
		return this._2;
	}

	/**
	 * 1#位置 元素变换
	 *
	 * @param <X>    mapper 的结果类型
	 * @param mapper 元素变化函数 t-&gt;x
	 * @return 变换后的 元素 (x,u)
	 */
	public <X> Tuple2<X, U> fmap1(final Function<T, X> mapper) {
		return TUP2(mapper.apply(this._1), this._2);
	}

	/**
	 * 2#位置 元素变换
	 *
	 * @param <X>    mapper 的结果类型
	 * @param mapper 元素变化函数 u-&gt;x
	 * @return 变换后的 元素 (t,x)
	 */
	public <X> Tuple2<T, X> fmap2(final Function<U, X> mapper) {
		return TUP2(this._1, mapper.apply(this._2));
	}

	/**
	 * 对象复制
	 *
	 * @return 复制的对象
	 */
	public Tuple2<T, U> duplicate() {
		return TUP2(this._1, this._2);
	}

	/**
	 * 元素位置互换
	 *
	 * @return (u, t)
	 */
	public Tuple2<U, T> swap() {
		return TUP2(this._2, this._1);
	}

	/**
	 * 元素位置互换
	 *
	 * @param <X>    元素类型
	 * @param mapper 元祖变换函数 (u,t)-&gt;X
	 * @return X 类型结果
	 */
	public <X> X swap(final Function<Tuple2<U, T>, X> mapper) {
		return mapper.apply(this.swap());
	}

	/**
	 * 智能版的数组转换 <br>
	 * 视 Tuple2 为一个Object的二元数组[_1,_2],然后调用mapper 给予变换<br>
	 *
	 * @param <X>    mapper 结果的类型
	 * @param mapper [o]-&gt;x 数组变换函数
	 * @return X类型结果
	 */
	public <X> X arrayOf(final Function<Object[], X> mapper) {
		return mapper.apply(this.toArray());
	}

	/**
	 * 转成列表结构
	 * <p>
	 * 使用示例:toList((Integer)null);
	 *
	 * @param typepholder 类型占位符
	 * @return 列表结构
	 */
	@SuppressWarnings("unchecked")
	public <X> List<X> toList(final T typepholder) {
		return Arrays.asList((X) this._1, (X) this._2);
	}

	/**
	 * 转成列表结构
	 *
	 * @return 列表结构
	 */
	public List<Object> toList() {
		return Arrays.asList(this._1, this._2);
	}

	/**
	 * 生成数组 生成数组 [_1,_2]
	 *
	 * @return [_1, _2]
	 */
	public Object[] toArray() {
		final Class<?>[] classes = Stream.of(this._1, this._2).filter(Objects::nonNull).map(Object::getClass)
				.toArray(Class[]::new);
		final List<Class<?>> shared = Types.getSharedSuperClasses(classes);
		final Class<?> clazz = (null != shared && shared.size() > 0) ? shared.get(0) : Object.class;
		return this.toArray(Types.aagen(clazz));
	}

	/**
	 * 生成数组 [_1,_2]
	 *
	 * @param generator 数组生成器 n-&gt;[t]
	 * @param <V>       元素类型
	 * @return [_1, _2]
	 */
	public <V> V[] toArray(final IntFunction<V[]> generator) {
		return Stream.of(this._1, this._2).toArray(generator);
	}

	/**
	 * 扁平化
	 *
	 * @param <X>    元素类型
	 * @param mapper 值变换函数 o-&gt;x
	 * @return X类型额数据流
	 */
	public <X> Stream<X> flatS(final Function<Object, X> mapper) {
		return Tuple2.flatS(this).map(mapper);
	}

	/**
	 * 转成列表结构
	 *
	 * @param <V>    元素类型
	 * @param mapper 值变换函数 o-&gt;x
	 * @return v list
	 */
	public <V> List<V> toFlatList(final Function<Object, V> mapper) {
		return this.flatS(mapper).collect(Collectors.toList());
	}

	/**
	 * 转成列表结构
	 *
	 * @return obj 列表
	 */
	public List<Object> toFlatList() {
		return this.toFlatList(e -> e);
	}

	/**
	 * 数值变换
	 *
	 * @param <V>    元素类型
	 * @param mapper 数值变换函数 tup-&gt;x
	 * @return V类型的数据
	 */
	public <V> V mutate(final Function<Tuple2<T, U>, V> mapper) {
		return mapper.apply(this);
	}

	@Override
	public Iterator<Object> iterator() {
		return this.toList().iterator();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this._1, this._2);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Tuple2) {
			@SuppressWarnings("unchecked")
			final Tuple2<Object, Object> another = (Tuple2<Object, Object>) obj;
			return eql.test(this._1, another._1) && eql.test(this._2, another._2);
		} else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(final Tuple2<T, U> o) {
		if (o == null)
			return 1;
		final Optional<Comparable<T>> l1 = asCP(this._1);
		final Optional<Comparable<T>> r1 = asCP(o._1);
		final Optional<Comparable<U>> l2 = asCP(this._2);
		final Optional<Comparable<U>> r2 = asCP(o._2);

		final int t = l1.map(t1 -> r1.map(t2 -> t1.compareTo((T) t2)).orElse(1)).orElse(r1.isPresent() ? 0 : -1);
		if (t == 0) {
			final int u = l2.map(u1 -> r2.map(u2 -> u1.compareTo((U) u2)).orElse(1)).orElse(r2.isPresent() ? 0 : -1);
			return u;
		} else {
			return t;
		}
	}

	/**
	 * 数据格式化
	 *
	 * @return 数据格式化
	 */
	public String toString() {
		return "(" + this._1 + "," + this._2 + ")";
	}

	/**
	 * 1 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param itr 第一元素遍历器
	 * @return t->(t,u)
	 */
	public static <T, U> Function<U, Tuple2<T, U>> zipper1(final Iterator<T> itr) {
		final List<T> data = new ArrayList<T>();
		final AtomicInteger ai = new AtomicInteger();
		final Supplier<T> tgetter = () -> {
			final int i = ai.getAndIncrement();
			if (itr.hasNext()) {
				data.add(itr.next());
			}
			return data.get(i % data.size());
		};
		return u -> Tuple2.of(tgetter.get(), u);
	}

	/**
	 * 1 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param tt  第一元素遍历器
	 * @return t->(t,u)
	 */
	public static <T, U> Function<U, Tuple2<T, U>> zipper1(final Iterable<T> tt) {
		return zipper1(tt.iterator());
	}

	/**
	 * 1 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param tt  第一元素遍历器
	 * @return t-&gt;(t,u)
	 */
	public static <T, U> Function<U, Tuple2<T, U>> zipper1(final T[] tt) {
		return zipper1(Arrays.asList(tt));
	}

	/**
	 * 1 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param stm 第一元素流
	 * @return t-&gt;(t,u)
	 */
	public static <T, U> Function<U, Tuple2<T, U>> zipper1(final Stream<T> stm) {
		return zipper1(stm.iterator());
	}

	/**
	 * 2 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param itr 第二元素遍历器
	 * @return t-&gt;(t,u)
	 */
	public static <T, U> Function<T, Tuple2<T, U>> zipper2(final Iterator<U> itr) {
		final List<U> data = new ArrayList<U>();
		final AtomicInteger ai = new AtomicInteger();
		final Supplier<U> ugetter = () -> {
			final int i = ai.getAndIncrement();
			if (itr.hasNext()) {
				data.add(itr.next());
			}
			return data.get(i % data.size());
		};
		return t -> Tuple2.of(t, ugetter.get());
	}

	/**
	 * 2 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param uu  第二元素遍历器
	 * @return t-&gt;(t,u)
	 */
	public static <T, U> Function<T, Tuple2<T, U>> zipper2(final Iterable<U> uu) {
		return zipper2(uu.iterator());
	}

	/**
	 * 2 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param uu  第二元素遍历器
	 * @return t-&gt;(t,u)
	 */
	public static <T, U> Function<T, Tuple2<T, U>> zipper2(final U[] uu) {
		return zipper2(Arrays.asList(uu));
	}

	/**
	 * 2 位 zipper
	 *
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param stm 第二元素流
	 * @return t-&gt;(t,u)
	 */
	public static <T, U> Function<T, Tuple2<T, U>> zipper2(final Stream<U> stm) {
		return zipper2(stm.iterator());
	}

	/**
	 * 生成一个 (x,y) 类型的 comparator <br>
	 * (升顺序,先比较第一元素键名,然后比较键值)
	 *
	 * @param <X> 第一元素类型
	 * @param <Y> 第二元素类型
	 * @return 生成一个 comparator
	 */
	public static <X, Y> Comparator<Tuple2<X, Y>> defaultComparator() {
		@SuppressWarnings("unchecked")
		final BiFunction<Object, Object, Integer> cmp = (a, b) -> { // 生成比较函数
			if (a == null && b == null)
				return 0;
			else if (a == null) {
				return -1;
			} else if (b == null) {
				return 1;
			} else { // a b 均为 非空
				int ret = -1; // 默认返回值
				try {
					if (a instanceof String || b instanceof String) {
						throw new Exception("字符串比较,设计的跳转异常，并非错误，这是一条类似于goto 的跳转语句写法");
					} else {
						ret = ((Comparable<Object>) a).compareTo(b);
					} // if
				} catch (final Exception e) {
					final String _a = a.toString();
					final String _b = b.toString();
					try { // 尝试做数字解析
						ret = Double.compare(Double.parseDouble(_a), Double.parseDouble(_b));
					} catch (Exception p) {
						ret = _a.compareTo(_b);
					} // try
				} // try

				return ret; // 返回比较结果
			} // if
		}; // cmp 比较函数

		return (tup1, tup2) -> {
			final int a = cmp.apply(tup1._1, tup2._1);
			if (a == 0) {
				return cmp.apply(tup1._2, tup2._2);
			} else {
				return a;
			} // if
		};
	}

	/**
	 * @param tup 二元组
	 * @return [_1,_2]
	 */
	public static Stream<Object> flatS(final Tuple2<?, ?> tup) {
		return flat(tup).stream();
	}

	/**
	 * @param tup 二元组
	 * @return [_1,_2]
	 */
	public static List<Object> flat(final Tuple2<?, ?> tup) {
		final List<Object> ll = new LinkedList<Object>();
		final Stack<Object> stack = new Stack<Object>();

		stack.push(tup);
		while (!stack.isEmpty()) {
			final Object p = stack.pop();
			if (p == null)
				continue;

			if (p instanceof Tuple2) {
				@SuppressWarnings("unchecked")
				final Tuple2<Object, Object> _tup = (Tuple2<Object, Object>) p;
				Stream.of(_tup._2(), _tup._1()).forEach(stack::push);
			} else {
				ll.add(p);
			}
		}

		return ll;
	}

	/**
	 * 构造一个二元组
	 *
	 * @param t   第一元素
	 * @param u   第二元素
	 * @param <T> 第一元素类型
	 * @param <U> 第二元素类型
	 * @return 二元组对象的构造
	 */
	public static <T, U> Tuple2<T, U> of(final T t, final U u) {
		return new Tuple2<T, U>(t, u);
	}

	/**
	 * 构造一个二元组
	 *
	 * @param iterable 可遍历对象
	 * @param <T>      iterable的元素类型
	 * @return 二元组对象的构造(提取可便利对象的前面两个元素)
	 */
	public static <T> Tuple2<T, T> from(final Iterable<T> iterable) {
		final Iterator<T> itr = iterable.iterator();
		final T t1 = itr.hasNext() ? itr.next() : null;
		final T t2 = itr.hasNext() ? itr.next() : null;
		return new Tuple2<T, T>(t1, t2);
	}

	/**
	 * 构造一个二元组
	 *
	 * @param _1  第一元素
	 * @param _2  第二元素
	 * @param <T> 第一元素类型
	 * @param <U> 第二元素类型
	 * @return 二元组对象的构造
	 */
	public static <T, U> Tuple2<T, U> TUP2(final T _1, final U _2) {
		return Tuple2.of(_1, _2);
	}

	/**
	 * 构造一个二元组 <br>
	 * 提取tt前两个元素组成 Tuple2
	 *
	 * @param tt  数组元素,提取tt前两个元素组成 Tuple2, (tt[0],tt[1])
	 * @param <T> 第一元素类型,第二元素类型
	 * @return 二元组对象的构造
	 */
	public static <T> Tuple2<T, T> TUP2(final T[] tt) {
		if (tt == null || tt.length < 1) {
			return null;
		}

		final T _1 = tt.length >= 1 ? tt[0] : null;
		final T _2 = tt.length >= 2 ? tt[0] : null;

		return new Tuple2<>(_1, _2);
	}

	/**
	 * 健名，键值 序列
	 *
	 * @param tt 健名，键值 序列
	 * @return 健名，键值 序列
	 */
	@SafeVarargs
	public static <T> Stream<Tuple2<T, T>> tupleS(final T... tt) {
		return Stream.iterate(0, i -> i < tt.length, i -> i + 2)
				.map(i -> i + 1 >= tt.length ? null : Tuple2.of(tt[i], tt[i + 1])).filter(Objects::nonNull);
	}

	/**
	 * 健名，键值 序列
	 *
	 * @param line    健名，键值序列
	 * @param pattern 分隔符模式
	 * @return 健名，键值 序列
	 */
	public static Stream<Tuple2<String, String>> tupleS(final String line, final String pattern) {
		return Tuple2.tupleS(line.split(pattern));
	}

	/**
	 * 健名，键值 序列 (默认采用空格 逗号 /进行间隔)
	 *
	 * @param line 健名，键值 序列
	 * @return 健名，键值 序列
	 */
	public static Stream<Tuple2<String, String>> tupleS(final String line) {
		return Tuple2.tupleS(line, "[\\s,;/]+");
	}

	/**
	 * snbuilder 的简写 <br>
	 * 键名，键值 生成器 <br>
	 * 开始号码为0
	 *
	 * @param <T>   元素
	 * @param start 开始号码
	 * @return t-&gt;(int,t) 的标记函数
	 */
	public static <T> Function<T, Tuple2<Integer, T>> snb(final Integer start) {
		return snbuilder(start);
	}

	/**
	 * snb 的 键名，键值的调转形式 键名，键值 生成器 <br>
	 * 开始号码为0
	 *
	 * @param <T>   元素
	 * @param start 开始号码
	 * @return t-&gt;(t,integer) 的标记函数
	 */
	public static <T> Function<T, Tuple2<T, Integer>> snb2(final Integer start) {
		final AtomicInteger sn = new AtomicInteger(start);
		return t -> TUP2(t, sn.getAndIncrement());
	}

	/**
	 * 序列号生成器 <br>
	 * Serial Number Builder
	 *
	 * @param <T>   元素
	 * @param start 开始号码
	 * @return t-&gt;(int,t) 的标记函数
	 */
	public static <T> Function<T, Tuple2<Integer, T>> snbuilder(final int start) {
		final AtomicInteger sn = new AtomicInteger(start);
		return t -> TUP2(sn.getAndIncrement(), t);
	}

	/**
	 * 键名，键值 生成器 <br>
	 * 开始号码为0
	 *
	 * @param <T> 元素类型
	 * @return t-&gt;(int,t) 的标记函数
	 */
	public static <T> Function<T, Tuple2<Integer, T>> snbuilder() {
		return snbuilder(0);
	}

	/**
	 * modulo builder <br>
	 * 一维索引二维化
	 * 
	 * @param <T>     元素类型
	 * @param divisor 除数
	 * @param start   开始数值
	 * @return t-&gt;((i,j),t)
	 */
	public static <T> Function<T, Tuple2<Tuple2<Integer, Integer>, T>> modulob(final int divisor, final int start) {
		final AtomicInteger sn = new AtomicInteger(start);
		return t -> TUP2(Optional.of(sn.getAndIncrement()).map(i -> Tuple2.of(i / divisor, i % divisor)).get(), t);
	}

	/**
	 * modulo builder
	 * 
	 * @param <T>     元素类型
	 * @param divisor 除数
	 * @return t-&gt;((i,j),t)
	 */
	public static <T> Function<T, Tuple2<Tuple2<Integer, Integer>, T>> modulob(final int divisor) {
		return Tuple2.modulob(divisor, 0);
	}

	/**
	 * 取模运算
	 * 
	 * @param divisor 除数 取模运算
	 * @return (quotient,remainder)
	 */
	public static Function<Integer, Tuple2<Integer, Integer>> modulo(final Integer divisor) {
		return dividend -> {
			final int quotient = dividend / divisor;
			final int remainder = dividend % divisor;
			return Tuple2.of(quotient, remainder);
		};
	}

	/**
	 * 强制转换
	 *
	 * @param t   值对象
	 * @param <T> 元素类型
	 * @return Optional Comparable T
	 */
	@SuppressWarnings("unchecked")
	public static <T> Optional<Comparable<T>> asCP(final T t) {
		return Optional.ofNullable(t instanceof Comparable ? (Comparable<T>) t : null);
	}

	/**
	 * 元组比较 次序为 1号元素比较，较大的返回，若是 1号元素相等才比较二号元素。。
	 * 
	 * @param tup1 第一元组
	 * @param tup2 第二元组
	 * @param <T>  1号位置元素
	 * @param <U>  2号位置元素
	 * @return 最大的元组
	 */
	public static <T, U> Tuple2<T, U> max(final Tuple2<T, U> tup1, final Tuple2<T, U> tup2) {
		final Optional<Tuple2<T, U>> opt1 = Optional.ofNullable(tup1);
		final Optional<Tuple2<T, U>> opt2 = Optional.ofNullable(tup2);
		return opt1.map(t1 -> opt2.filter(t2 -> t1.compareTo(t2) <= 0).orElse(t1))
				.orElse(opt2.isPresent() ? tup1 : tup2);
	}

	/**
	 * 元组比较 次序为 1号元素比较，较小的返回，若是 1号元素相等才比较二号元素。
	 * 
	 * @param tup1 第一元组
	 * @param tup2 第二元组
	 * @param <T>  1号位置元素
	 * @param <U>  2号位置元素
	 * @return 最小的元组
	 */
	public static <T, U> Tuple2<T, U> min(final Tuple2<T, U> tup1, final Tuple2<T, U> tup2) {
		final Optional<Tuple2<T, U>> opt1 = Optional.ofNullable(tup1);
		final Optional<Tuple2<T, U>> opt2 = Optional.ofNullable(tup2);
		return opt1.map(t1 -> opt2.filter(t2 -> t1.compareTo(t2) >= 0).orElse(t1))
				.orElse(opt2.isPresent() ? tup1 : tup2);
	}

	/**
	 * 二元函数
	 * 
	 * @param <X>    1号元素类型
	 * @param <Y>    2号元素类型
	 * @param <Z>    结果类型
	 * @param mapper (x,y)-&gt;z
	 * @return (x,y)-&gt;z
	 */
	public static <X, Y, Z> Function<Tuple2<X, Y>, Z> bifun(final BiFunction<X, Y, Z> mapper) {
		return tup -> mapper.apply(tup._1, tup._2);
	}

	/**
	 * 判断两个对象是否等价
	 */
	public final static BiPredicate<Object, Object> eql = (a, b) -> a != null ? a.equals(b) : b == null;

	public final T _1; // 第一位置元素
	public final U _2; // 第二位置元素

}