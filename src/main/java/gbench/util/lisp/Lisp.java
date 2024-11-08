package gbench.util.lisp;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

/**
 * 列表函数
 * 
 * @author gbench
 *
 */
public class Lisp {

	/**
	 * 默认构造函数
	 */
	private Lisp() {

	}

	/**
	 * 起始序列
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 起始序列
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T> Tuple2<T[], T> initalLast(final T... ts) {
		if (ts.length < 1) {
			return Tuple2.of((T[]) Array.newInstance(ts.getClass().getComponentType(), 0), null);
		} else if (1 == ts.length) {
			return Tuple2.of(Arrays.copyOf(ts, 1), null);
		} else {
			return Tuple2.of(Arrays.copyOfRange(ts, 0, ts.length - 1), ts[ts.length - 1]);
		}
	}

	/**
	 * 头尾
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 头尾
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T> Tuple2<T, T[]> headTail(final T... ts) {
		if (ts.length < 1) {
			return Tuple2.of(null, (T[]) Array.newInstance(ts.getClass().getComponentType(), 0));
		} else if (1 == ts.length) {
			return Tuple2.of(null, Arrays.copyOf(ts, 1));
		} else {
			return Tuple2.of(ts[0], Arrays.copyOfRange(ts, 1, ts.length));
		}
	}

	/**
	 * 头部元素
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 头部元素
	 */
	@SafeVarargs
	public static <T> T head(final T... ts) {
		return 1 > ts.length ? null : ts[0];
	}

	/**
	 * 尾部蓄力
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 尾部序列
	 */
	@SafeVarargs
	public static <T> T[] tail(final T... ts) {
		return 1 > ts.length ? null : Arrays.copyOfRange(ts, 1, ts.length);
	}

	/**
	 * 起始序列
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 起始序列
	 */
	@SafeVarargs
	public static <T> T[] initial(final T... ts) {
		return 1 > ts.length ? null : Arrays.copyOfRange(ts, 0, ts.length - 1);
	}

	/**
	 * 最后一个元素
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 最后一个元素
	 */
	@SafeVarargs
	public static <T> T last(final T... ts) {
		return 1 > ts.length ? null : ts[ts.length - 1];
	}

	/**
	 * Y组合子：一种令 lambda表达式提供递归 调用技巧<br>
	 * 使用方法:
	 * 
	 * Function<Integer,Integer> factorial = <br>
	 * yCombinator(f->n->if(n<1) n*1d else f.apply(n-1)*n) <br>
	 * factorial.apply(10)
	 * 
	 * @param <T> 参数类型
	 * @param <R> 结果类型
	 * @param g   二级lambda表达式: 是一个 f->n->...的结构。 注意这里用f封装了n->...的lambda包装 <br>
	 *            其中 n->...就是我们 需要进行递归的lambda表达式: 目标lamda。<br>
	 *            通过 yCombinator的 目标lamda 会被赋值给f，即f就代表了目标lamda，具体使用实例如下：<br>
	 *            f->n->if(n<1) n*1d else f.apply(n-1)*n <br>
	 *            f 就是 “n->if(n<1) n*1d else f.apply(n-1)*n” 的命名名引用：<br>
	 *            那么f 是 如何 被复制的呢？答案就是参数调用 即g.apply(目标lamda)<br>
	 *            那么 目标lamda 又如何提取的方法就是构造一个与 目标lamda
	 *            一样功能的匿名函数。然后在这个匿名函数之中使用this进行引用。<br>
	 *            需要明白：yCombinator 的功能 构造一个 目标lamda 具有一模一样功能的函数 这个函数《br>
	 *            用匿名类的this医用; 于是 g.apply(this)(需要明白这里的apply(this)就是把this复值给了f） ,其实
	 *            就是返回了目标lamda。然后 通过 g.apply(this).apply(t) 就是模拟了 目标lambda功能。
	 *            就是返回了目标lamda 这里的鹅巧妙之处就在于 g.apply(this).apply(t); 的这一句：
	 *            g.apply(this) 是一个新编写的功能
	 * @return 生成函数
	 */
	public static <T, R> Function<T, R> yCombinator(final Function<Function<T, R>, Function<T, R>> g) {
		return new Function<T, R>() {
			@Override
			public R apply(final T t) {
				return g.apply(this).apply(t);
			}
		};
	}

	/**
	 * list ComPreHend
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
			final int i = ai.getAndIncrement(); // level阶层位置，注意一定要用非对象类型:int而非Integer
			return parentS.flatMap(parent -> Stream.of(tt).map(t -> buffer[i] = t)); // 反复刷新buffer缓存,用父子关系发散出t流
		}, (a, b) -> a).map(e -> buffer); // 输出缓存状态
	}

	/**
	 * 全排列
	 *
	 * @param <T> 排列元素 类型
	 * @param tt  排列元素数组
	 * @param len 排列长度. len大于等于1的正整数,<br>
	 *            最长不能超过tt的length即位于区间[1,tt.length], 超出会视同tt.length
	 * @return 排列结果的流
	 */
	public static <T> Stream<T[]> permute(final T[] tt, final Integer len) {
		final int n = (len == null || len > tt.length ? tt.length : len);
		@SuppressWarnings("unchecked")
		final T[] buffer = (T[]) Array.newInstance(tt.getClass().getComponentType(), tt.length);

		return n == 0 // 排列长度
				? Stream.empty() // 空数据
				// 生成树的展开，注意这里把reduce作为一个阶层的 过程结构 来使用, <br>
				// 流的lazy compute 使得map, flatMap 这样的运算变成了 一种层级的结构的构造运算，于是就可以利用这个特性来做 展开树的操作。
				// reduce 的反复 flatMap 就有点类似于道生一，一生二，二生三，三生万物的意思。
				// 所以 reduce+flatMap 的本质就是一种过程的从一生多的结构（树形结构），这是一个有趣的思想，请注意领悟,并在实践中 给予丰富加强
				// 并采用元素数据外置到buffer之中的共享内存的方式对流进行操作,reduce
				: Stream.iterate(0, i -> i < n, i -> ++i).reduce(Stream.of(tt), // 生成树的初生代元素
						(oneS, i) -> { // ones 当前需要写入的元素, 也就是道生一的那个一，这个一的功能就是用来flatMap去生成更多的新的 ones,也及时一生二，生万物的的意思，
							return oneS.map(one -> buffer[i] = one) // 写入元素数据。
									.flatMap(one -> { // 使用flatMap做生成树的展开，flatMap 的特长就是善于从1点出发开始发散，于是就极为方便的用来做生成树。
										final Predicate<T> ancestor_not_contains = t -> { // 已经写入的祖先数据,是否写入过t
											for (int j = 0; j <= i; j++) {
												if (Objects.equals(buffer[j], t)) {
													return false;
												} // if
											} // for
											return true;
										}; // ancestor_not_contains
										final Stream<T> _oneS = Stream.of(tt).filter(ancestor_not_contains); // 尚未排序的其他元素加入新的ones投递到下一阶层。
										// 投递到下一阶层执行。
										return i == n - 1 // 已经写入完成。
												? Stream.of((T) null) // 执行结束
												: _oneS; // 新的ones
									}); // flatMap 在parent的基础之上给予继续展开
						}, (a, b) -> a).map(e -> Arrays.copyOfRange(buffer, 0, n)); // 把buffer 中数据给予拷贝输出
	}

	/**
	 * 全排列
	 *
	 * @param <T> 排列元素 类型
	 * @param tt  排列元素数组
	 * @return 排列结果的流
	 */
	public static <T> Stream<T[]> permute(final T[] tt) {
		return Lisp.permute(tt, null);
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
	 * 列表构造器,参见 Lisp 语言的cons <br>
	 * 在数组头前添加一个元素 t <bar> 样例： CONS(0,[1,2]) 返回 [0,1,2] <br>
	 * CONS(1,null) [1] <br>
	 * CONS(null, null) [null] <br>
	 * CONS(1, A(2, 3)) [1, 2, 3] <br>
	 * CONS(null, A(2, 3)) [null, 2, 3] <br>
	 *
	 * @param <T> 元素类型
	 * @param t   头前元素 t0
	 * @param ts  元素序列,[t1,t2,...]
	 * @return t[t0,t1,t2]
	 */
	public static <T> T[] CONS(final T t, final T[] ts) {
		@SuppressWarnings("unchecked")
		final Class<T> tclazz = Optional.ofNullable(ts)
				.map(e -> Optional.ofNullable(e.getClass()).map(p -> (Class<T>) p.getComponentType())) //
				.orElseGet(() -> Optional.ofNullable(t).map(e -> (Class<T>) e.getClass()))
				.orElseGet(() -> (Class<T>) Object.class);
		final var n0 = Optional.ofNullable(t).map(e -> 1).orElse(1);
		final var n1 = Optional.ofNullable(ts).map(e -> e.length).orElse(0);
		final var aa = Stream
				.concat(Stream.of(t), Optional.ofNullable(ts).map(Stream::of).orElseGet(() -> Stream.empty()))
				.collect(aaclc(n0 + n1, tclazz, e -> e));

		return aa;
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
		for (int i = 0; i < n; i++) {
			tt[i] = t;
		}
		return tt;
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>      归集器元素类型
	 * @param <U>      结果类型
	 * @param size     每次增扩的空间大小
	 * @param tclazz   元素类型,tclazz 为null时候，默认为获取的第一个非空元素的类型作为数组元素的类型。
	 * @param finisher 完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Collector<T, ?, U> aaclc(final int size, final Class<T> tclazz,
			final Function<T[], U> finisher) {
		return Collector.of(() -> Tuple2.of(new AtomicInteger(), new AtomicReference<T[]>(null)), // (i,ts)
				(p, t) -> {
					final int i = p._1.getAndIncrement(); // 生成当前写入下标索引
					if (Objects.nonNull(t)) {
						Optional.ofNullable(p._2.get()).map(ts -> { // 使用 Optional 实现变量改名
							if (i >= ts.length) { // 数据空间不足 增加 size 大小
								p._2.set(Arrays.copyOf(ts, size + ts.length));
							}
							return p;
						}).orElseGet(() -> { // 初次空间生成
							final int n = size * ((i + 1) / size + ((i + 1) % size == 0 ? 0 : 1)); // 向上取整
							p._2.set((T[]) Array.newInstance(Optional.ofNullable(tclazz) //
									.orElse((Class<T>) t.getClass()), n)); // 生成指定大小的数组空间
							return p;
						})._2.get()[i] = t;// 归集数据写入
					} // if nonNull
				}, (p1, p2) -> { // 合并
					return Optional.ofNullable(p1._2.get()).flatMap(ts1 -> Optional.ofNullable(p2._2.get()).map(ts2 -> { // ts1,ts2均非空
						final int free1 = p1._2.get().length - p1._1.get(); // 1#空闲空间
						final int free2 = p2._2.get().length - p2._1.get(); // 2#空闲空间
						return Optional.of(free1 > free2 // 使用 Optional 实现变量改名
								? Tuple2.of(p1, p2) // 1#大于2#
								: Tuple2.of(p2, p1) // 1#小于2#
						).map(p -> { // (p._1:max,p._2:min)
							final int total = p._1._1.get() + p._2._1.get(); // 合并后的大小
							if (Math.max(free1, free2) > p._2._1.get()) { // 最大的剩余空间要比最小的有效空间要大,把小的数据拷贝到大的剩余空间里面去
								System.arraycopy(p._2._2.get(), 0, p._1._2.get(), p._1._1.get(), p._2._1.get());
								if (p1._2.get() != p._1._2.get()) { // 确保ar1指向合并后的结果p。_1._2
									p1._2.set(p._1._2.get()); // 更新新的数据空间
								} // if
							} else { // 空间不能重用，则重新开创空间
								final T[] ts = Arrays.copyOf(p._1._2.get(), total); // 扩展成新的合并大小
								System.arraycopy(p._2._2.get(), 0, ts, p._1._1.get(), p._2._1.get());
								p1._2.set(ts); // 更新新的数据空间
							} // if
							p1._1.set(total);
							return p1;
						}); // Optional
					}).orElseGet(() -> { // ts1非空,ts2为空
						final int free1 = p1._2.get().length - p1._1.get(); // 1#空闲空间
						final int total = p1._1.get() + p2._1.get(); // 合并后的大小
						if (free1 > p2._1.get()) { // free1空间可以容纳p2
							// do nothing
						} else { // free1空间不可以容纳p2
							final T[] ts = Arrays.copyOf(p1._2.get(), total); // 扩展成新的合并大小
							p1._2.set(ts);
						}
						p1._1.set(total);
						return Optional.of(p1);
					})).orElseGet(() -> { // ts1为空
						final int total = p1._1.get() + p2._1.get(); // 合并后的大小
						if (null != p2._2.get()) { // ts1为空,ts2非空
							final int free2 = p2._2.get().length - p2._1.get(); // 2#空闲空间
							if (free2 > p1._1.get()) { // free2 空间可以容纳p1
								p1._2.set(p2._2.get());
							} else { // free2 空间不可以容纳p1
								final T[] ts = Arrays.copyOf(p2._2.get(), total); // 扩展成新的合并大小
								p1._2.set(ts);
							} // if free2
						} else { // ts1, ts2 都为空
							// do nothing
						} // if ts1 & ts2 空值判断
						p1._1.set(total); // 合并后的大小
						return p1;
					});
				}, p -> Optional.ofNullable(p._2.get()).flatMap(ts -> Optional.of(p._1.get()) // 使用optional 改名
						.map(n -> Objects.equals(n, ts.length) ? ts : Arrays.copyOf(ts, n))) // 去除多余空间
						.map(finisher).orElseGet(() -> finisher.apply((T[]) new Object[p._1.get()])));
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>      归集器元素类型
	 * @param <U>      结果类型
	 * @param tclazz   t类型类
	 * @param finisher 完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	public static <T, U> Collector<T, ?, U> aaclc(final Class<T> tclazz, final Function<T[], U> finisher) {
		return Lisp.aaclc(10, tclazz, finisher);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>             归集器元素类型
	 * @param <U>             结果类型
	 * @param initialCapacity the initial capacity of the list
	 * @param finisher        完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	public static <T, U> Collector<T, ?, U> aaclc(final int initialCapacity, final Function<ArrayList<T>, U> finisher) {
		return Lisp.aaclc(initialCapacity, false, finisher);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>          归集器元素类型
	 * @param <U>          结果类型
	 * @param reverse_flag 归集标志,true 调转, false 正序
	 * @param finisher     完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	public static <T, U> Collector<T, ?, U> aaclc(final boolean reverse_flag,
			final Function<ArrayList<T>, U> finisher) {
		return Lisp.aaclc(10, reverse_flag, finisher);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>             归集器元素类型
	 * @param <U>             结果类型
	 * @param initialCapacity the initial capacity of the list
	 * @param reverse_flag    归集标志,true 调转, false 正序
	 * @param finisher        完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	public static <T, U> Collector<T, ?, U> aaclc(final int initialCapacity, final boolean reverse_flag,
			final Function<ArrayList<T>, U> finisher) {
		return Collector.of(() -> new ArrayList<T>(initialCapacity), ArrayList::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, (ArrayList<T> aa) -> {
			if (reverse_flag) {
				Collections.reverse(aa);
			}
			return finisher.apply(aa);
		});
	}

	/**
	 * 链表 元素归集器具
	 * <p>
	 * reverse_flag 采用默认 false 正序
	 *
	 * @param <T>      归集器元素类型
	 * @param <U>      结果类型
	 * @param finisher 完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	public static <T, U> Collector<T, ?, U> aaclc(final Function<ArrayList<T>, U> finisher) {
		return Lisp.aaclc(false, finisher);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>          归集器元素类型
	 * @param <U>          结果类型
	 * @param reverse_flag 归集标志,true 调转, false 正序
	 * @param finisher     完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	public static <T, U> Collector<T, ?, U> llclc(final boolean reverse_flag,
			final Function<LinkedList<T>, U> finisher) {
		return Collector.of(LinkedList::new, reverse_flag ? LinkedList::addFirst : LinkedList::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, finisher);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param reverse_flag 归集标志,true 调转, false 正序
	 * @param <T>          归集器元素类型
	 * @return [t] 链表结构
	 */
	public static <T> Collector<T, ?, LinkedList<T>> llclc(final boolean reverse_flag) {
		return Lisp.llclc(reverse_flag, e -> e);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T> 归集器元素类型
	 * @return [t] 链表结构
	 */
	public static <T> Collector<T, ?, LinkedList<T>> llclc() {
		return Lisp.llclc(false, e -> e);
	}

	/**
	 * 链表 元素归集器具
	 *
	 * @param <T>          归集器元素类型
	 * @param <U>          结果类型
	 * @param reverse_flag 归集标志,true 调转, false 正序
	 * @param finisher     完成归集器 ll->u
	 * @return [t]->u 链表归集器
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Collector<T, ?, U> llclcS(final boolean reverse_flag, final Function<Stream<T>, U> finisher) {
		return Collector.of(LinkedList::new, reverse_flag ? LinkedList::addFirst : LinkedList::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, e -> finisher.apply((Stream<T>) e.stream()));
	}

	/**
	 * 将所有元素串连字符串的归集器[t]->s
	 * 
	 * @param <T>    归集元素类型
	 * @param params 串联参数设置为null表示采用默认"",
	 *               prefix:前缀,delim:分片内元素分隔,suffix:后缀,lndelim:分片间隔
	 * @return 将所有元素串连字符串的归集器[t]->s
	 */
	public static <T> Collector<T, ?, String> joinclc(final String... params) {
		final var _params = null == params ? new String[4] : Arrays.copyOf(params, 4); // 转换成长度为4的数组形式
		return Lisp.joinclc(_params[0], _params[1], _params[2], _params[3]);
	}

	/**
	 * 将所有元素串连字符串的归集器[t]->s
	 * 
	 * @param <T>     归集元素类型
	 * @param prefix  前缀
	 * @param delim   同一一个执行分片之间的元素间分隔符
	 * @param suffix  后缀
	 * @param lndelim 并发计算的时候 分片之间的间隔,line delimeter.注意对于非并发流采用单分片机制不会用到lndelim
	 * @return 将所有元素串连字符串的归集器[t]->s
	 */
	public static <T> Collector<T, ?, String> joinclc(final String prefix, final String delim, final String suffix,
			final String lndelim) {
		final var _prefix = Optional.ofNullable(prefix).orElse("");
		final var _delim = Optional.ofNullable(delim).orElse("");
		final var _suffix = Optional.ofNullable(suffix).orElse("");
		final var _lndelim = Optional.ofNullable(lndelim).orElse("");
		final var _empty = String.format("%s%s", _prefix, _suffix);
		return Collector.of(() -> new StringJoiner(_delim, _prefix, _suffix), (acc, a) -> acc.add(a + ""), (a, b) -> {
			final var _b = b.toString();
			return _empty.equals(_b) ? a : // b是一个空串直接返回a
					a.add(_lndelim + _b.substring(_prefix.length(), _b.length() - _suffix.length()));
		}, e -> Optional.ofNullable(e.toString()).map(p -> p.endsWith(_suffix) ? p : p + _suffix).get());
	}

}
