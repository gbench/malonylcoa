package gbench.util.math.algebra.tuple;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 二维元组 (t,u) 仅含有两个元素的数据结构，形式简单 但是 内涵极为丰富
 *
 * @param <T> 第一位置 元素类型
 * @param <U> 第而位置 元素类型
 */
public class Tuple2<T, U> {

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
	 * @param mapper 元素变化函数 t-x
	 * @return 变换后的 元素 (x,u)
	 */
	public <X> Tuple2<X, U> fmap1(final Function<T, X> mapper) {
		return P(mapper.apply(this._1), this._2);
	}

	/**
	 * 2#位置 元素变换
	 * 
	 * @param <X>    mapper 的结果类型
	 * @param mapper 元素变化函数 u-x
	 * @return 变换后的 元素 (t,x)
	 */
	public <X> Tuple2<T, X> fmap2(final Function<U, X> mapper) {
		return P(this._1, mapper.apply(this._2));
	}

	/**
	 * 1#位置 元素变换
	 * 
	 * @param <X> mapper 的结果类型
	 * @param x   默认值，类型强制转换失败后的默认值
	 * @return 变换后的 元素 (t,x)
	 */
	@SuppressWarnings({ "unchecked" })
	public <X> Tuple2<X, U> fmap1(final X x) {
		X _x = x;
		try {
			_x = (X) this._1;
		} catch (Exception e) {
			// do nothing
		}
		return P(_x, this._2);
	}

	/**
	 * 2#位置 元素变换
	 * 
	 * @param <X> mapper 的结果类型
	 * @param x   默认值，类型强制转换失败后的默认值
	 * @return 变换后的 元素 (t,x)
	 */
	@SuppressWarnings({ "unchecked" })
	public <X> Tuple2<T, X> fmap2(final X x) {
		X _x = x;
		try {
			_x = (X) this._2;
		} catch (Exception e) {
			// do nothing
		}
		return P(this._1, _x);
	}

	/**
	 * 对象复制
	 * 
	 * @return 复制的对象
	 */
	public Tuple2<T, U> duplicate() {
		return P(this._1, this._2);
	}

	/**
	 * 元素位置互换
	 * 
	 * @return (u,t)
	 */
	public Tuple2<U, T> swap() {
		return P(this._2, this._1);
	}

	/**
	 * 元素位置互换
	 * 
	 * @param <X>    元素类型
	 * @param mapper 元祖变换函数 (u,t)->X
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
	 * @param mapper [o]->x 数组变换函数
	 * @return X类型结果
	 */
	public <X> X arrayOf(final Function<Object[], X> mapper) {
		return mapper.apply(new Object[] { this._1, this._2 });
	}

	/**
	 * 转成列表结构
	 * 
	 * @return 列表结构
	 */
	@SuppressWarnings("unchecked")
	public <X> List<X> toList(final Class<X> clazz) {
		return Arrays.asList((X) this._1, (X) this._2);
	}

	/**
	 * 提取 指定索引的值，并强制转换成 clazz 类型
	 * 
	 * @param <X>   占位符类名
	 * @param idx   值索引,0 返回 第一个值,1:返回第二个值
	 * @param clazz 占位符类名，用于 标识 X的类型
	 * @return X 类型的值
	 */
	@SuppressWarnings("unchecked")
	public <X> X get(final int idx, final Class<X> clazz) {
		return (X) (idx == 0 ? this._1 : this._2);
	}

	/**
	 * 提取1#值，并强制转换成 clazz 类型
	 * 
	 * @param <X>   占位符类名
	 * @param clazz 占位符类名，用于 标识 X的类型
	 * @return X 类型的值
	 */
	public <X> X get1(final Class<X> clazz) {
		return this.get(0, clazz);
	}

	/**
	 * 提取2#值，并强制转换成 clazz 类型
	 * 
	 * @param <X>   占位符类名
	 * @param clazz 占位符类名，用于 标识 X的类型
	 * @return X 类型的值
	 */
	public <X> X get2(final Class<X> clazz) {
		return this.get(1, clazz);
	}

	/**
	 * 生成第一元素谓词函数
	 * 
	 * @param <X>       第一元素
	 * @param <Y>       第二元素
	 * @param <X1>      新第一元素
	 * @param predicate X->X1 第一元素谓词函数
	 * @return (x,y) -> (x1,y)
	 */
	public static <X, Y, X1> Predicate<Tuple2<X, Y>> predicateOf1(final Predicate<X> predicate) {
		return tup -> predicate.test(tup._1);
	}

	/**
	 * 生成第二元素谓词函数
	 * 
	 * @param <X>       第一元素
	 * @param <Y>       第二元素
	 * @param <X1>      新第一元素
	 * @param predicate Y-&gt;b 第二元素谓词函数
	 * @return (x,y) -&gt; (x1,y)
	 */
	public static <X, Y, X1> Predicate<Tuple2<X, Y>> predicateOf2(final Predicate<Y> predicate) {
		return tup -> predicate.test(tup._2);
	}

	/**
	 * 生成第一元素映射函数
	 * 
	 * @param <X>    第一元素
	 * @param <Y>    第二元素
	 * @param <X1>   新第一元素
	 * @param mapper X->X1 第一元素变换函数
	 * @return (x,y) -> (x1,y)
	 */
	public static <X, Y, X1> Function<Tuple2<X, Y>, Tuple2<X1, Y>> mapperOf1(final Function<X, X1> mapper) {
		return tup -> tup.fmap1(mapper);
	}

	/**
	 * 生成第二元素映射函数
	 * 
	 * @param <X>    第一元素
	 * @param <Y>    第二元素
	 * @param <Y1>   新第二元素
	 * @param mapper Y->Y1 第二元素变换函数
	 * @return (x,y) -> (x,y1)
	 */
	public static <X, Y, Y1> Function<Tuple2<X, Y>, Tuple2<X, Y1>> mapperOf2(final Function<Y, Y1> mapper) {
		return tup -> tup.fmap2(mapper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this._1, this._2);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { // 对象本身
			return true;
		} else if (obj instanceof Tuple2) { // 同类类型
			@SuppressWarnings("unchecked")
			final var another = (Tuple2<Object, Object>) obj;
			return Objects.equals(this._1, another._1) && Objects.equals(this._2, another._2);
		} else {
			return false;
		} // if
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
	 * 返回数组对象
	 * 
	 * @return 只有两个元素的数组 [_1:1#元素,_2:2号元素]
	 */
	public Object[] toArray() {
		return this.arrayOf(e -> e);
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
				var ret = -1; // 默认返回值
				try {
					if (a instanceof String || b instanceof String) {
						throw new Exception("字符串比较,设计的跳转异常，并非错误，这是一条类似于goto 的跳转语句写法");
					} else {
						ret = ((Comparable<Object>) a).compareTo(b);
					} // if
				} catch (final Exception e) {
					final var _a = a.toString();
					final var _b = b.toString();
					try { // 尝试做数字解析
						ret = ((Double) Double.parseDouble(_a)).compareTo(Double.parseDouble(_b));
					} catch (Exception p) {
						ret = _a.compareTo(_b);
					} // try
				} // try

				return ret; // 返回比较结果
			} // if
		}; // cmp 比较函数

		return (tup1, tup2) -> {
			final var a = cmp.apply(tup1._1, tup2._1);
			if (a == 0) {
				return cmp.apply(tup1._2, tup2._2);
			} else {
				return a;
			} // if
		};
	}

	/**
	 * 
	 * @param tup
	 * @return
	 */
	public static Stream<Object> flatS(final Tuple2<?, ?> tup) {
		return flat(tup).stream();
	}

	/**
	 * 
	 * @param tup
	 * @return
	 */
	public static List<Object> flat(final Tuple2<?, ?> tup) {
		final var ll = new LinkedList<Object>();
		final var stack = new Stack<Object>();

		stack.push(tup);
		while (!stack.isEmpty()) {
			final var p = stack.pop();
			if (p == null)
				continue;

			if (p instanceof Tuple2) {
				@SuppressWarnings("unchecked")
				final var _tup = (Tuple2<Object, Object>) p;
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
		return new Tuple2<>(t, u);
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
	public static <T, U> Tuple2<T, U> P(final T _1, final U _2) {
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
	public static <T> Tuple2<T, T> P(final T[] tt) {
		if (tt == null || tt.length < 1) {
			return null;
		}

		final var _1 = tt.length >= 1 ? tt[0] : null;
		final var _2 = tt.length >= 2 ? tt[1] : null;

		return new Tuple2<>(_1, _2);
	}

	/**
	 * 构造一个二元组 <br>
	 * 提取tt前两个元素组成 Tuple2
	 * 
	 * @param tt  列表元素,提取tt前两个元素组成 Tuple2, (tt[0],tt[1])
	 * @param <T> 第一元素类型,第二元素类型
	 * @return 二元组对象的构造
	 */
	public static <T> Tuple2<T, T> P(final List<T> tt) {
		final var _1 = tt.size() >= 1 ? tt.get(0) : null;
		final var _2 = tt.size() >= 2 ? tt.get(1) : null;

		return Tuple2.of(_1, _2);
	}

	/**
	 * 序列号生成器 <br>
	 * Serial Number Builder
	 * 
	 * @param <T>   元素
	 * @param start 开始号码
	 * @return t->(int,t) 的标记函数
	 */
	public static <T> Function<T, Tuple2<Integer, T>> snbuilder(final int start) {
		final var sn = new AtomicInteger(start);
		return t -> P(sn.getAndIncrement(), t);
	}

	/**
	 * 键名，键值 生成器 <br>
	 * 开始号码为为0
	 * 
	 * @param <T> 元素类型
	 * @return t->(int,t) 的标记函数
	 */
	public static <T> Function<T, Tuple2<Integer, T>> snbuilder() {
		return snbuilder(0);
	}

	public final T _1; // 第一位置元素
	public final U _2; // 第二位置元素

}