package gbench.util.array;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.array.INdarrayX.MetaKey;
import gbench.util.function.Getter;
import gbench.util.function.TriConsumer;
import gbench.util.function.TriFunction;
import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.tree.TrieNode;
import gbench.util.type.Strings;
import gbench.util.type.Types;

import static java.util.Arrays.asList;
import static gbench.util.function.Functions.identity;

/**
 * 相对访问 <br>
 * ***需要注意长度为1的INdarray就是一个对数组元素的引用: <br>
 * 这是一个与C语言pointer等同的该概念,这里称为IPoint*** <br>
 * <br>
 * <p>
 * 为了这点醋才包的这顿饺子 <br>
 * <p>
 * 为了项目索引访问而创建的类 <br>
 * <p>
 * 多维数据数组（用于展示数组的一部分，视图式封装），受到一定的numpy启发<br>
 * (data:连续区域的数据块,start:开始位置索引inclusive,end:结束位置索引exlusive) 去操作数据 <br>
 * <p>
 * !!!主要不要在INdarray直接操作data数组。请使用get和set函数处理数据。以保证INdarray可以正确设置 <br>
 * !!!必要的拦截和回调函数。
 *
 * @param <T> 元素类型
 * @author gbench
 */
public interface INdarray<T> extends Comparable<INdarray<T>>, Iterable<T>, IStream<T> {

	/**
	 * 源数据 <br>
	 * !!!主要不要在INdarray直接操作data数组。请使用get和set函数处理数据。以保证INdarray可以正确设置 <br>
	 * !!!必要的拦截和回调函数。 <br>
	 *
	 * @return 数据数组
	 */
	T[] data();

	/**
	 * 开始索引 inclusive
	 *
	 * @return 开始索引 inclusive
	 */
	int start();

	/**
	 * 结束索引 exclusive
	 *
	 * @return 结束索引 exclusive
	 */
	int end();

	/**
	 * 分区索引跨度
	 *
	 * @return 分区索引跨度
	 */
	LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides();

	/**
	 * 创建多维数组
	 *
	 * @param data    元数据
	 * @param start   data的绝对索引,开始位置索引,从0开始
	 * @param end     data的绝对索引,结束位置索引 exclusive
	 * @param strides 分区索引跨度
	 * @return ndarray
	 */
	INdarray<T> create(final T[] data, final int start, final int end,
			final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides);

	/**
	 * 当前视图内的数据[start,end)之间的数据
	 *
	 * @return 数据数组,[start,end)之间的数据
	 */
	default T[] mydata() {
		return this.arrayOf(identity());
	}

	/**
	 * 创建多维数组
	 *
	 * @param data  元数据
	 * @param start data的绝对索引,开始位置索引
	 * @param end   data的绝对索引,结束位置索引 exclusive
	 * @return ndarray
	 */
	default INdarray<T> create(final T[] data, final int start, final int end) {
		return this.create(data, start, end, this.strides());
	}

	/**
	 * 创建分区
	 *
	 * @param partitioner 索引分区器
	 * @return ndarray
	 */
	default INdarray<T> create(final Partitioner partitioner) {
		return this.create(partitioner.strides());
	}

	/**
	 * 创建分区：都区间进行区分 <br>
	 * 可以通过Tuple2.bifun(nd::create)转换成元组构造子
	 *
	 * @param start data的绝对索引,位置索引起点，从0开始 inclusive
	 * @param end   data的绝对索引,位置索引终点 exclusive
	 * @return ndarray
	 */
	default INdarray<T> create(final int start, final int end) {
		return this.create(this.data(), start, end, this.strides());
	}

	/**
	 * 创建分区
	 *
	 * @param start data的绝对索引,start 位置索引起点,从0开始 inclusive, end 默认为当前分段的结束位置
	 * @return ndarray
	 */
	default INdarray<T> create(final int start) {
		return this.create(this.data(), start, this.end(), this.strides());
	}

	/**
	 * @param strides 分区索引跨度
	 * @return ndarray
	 */
	default INdarray<T> create(final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		return this.create(this.data(), this.start(), this.end(), strides);
	}

	/**
	 * 复制 INdarray shallow copy
	 *
	 * @return 复制品, data不进行复制，仅复制start与end索引位置，浅拷贝
	 */
	default INdarray<T> duplicate() {
		return this.create(this.start());
	}

	/**
	 * 有效数据复制 INdarray deep
	 *
	 * @return 复制品,进行data的有效范围复制,即只复制[start,end) 之间的数据空间。 深拷贝
	 */
	default INdarray<T> dupdata() {
		return this.arrayOf(data -> this.create(data, 0, this.length()));
	}

	/**
	 * 完全数据复制 INdarray deep
	 *
	 * @return 复制品,进行整个data的完全复制,深拷贝
	 */
	default INdarray<T> dupdata2() {
		final T[] data = this.data();
		return this.create(Arrays.copyOf(data, data.length), this.start(), this.end(), this.strides());
	}

	/**
	 * 索引是否有效
	 *
	 * @param i 位置索引从0开始
	 * @return 索引是否有效 true 有效,false 无效
	 */
	default boolean checkIndex(final int i) {
		final int index = this.start() + i;
		return index >= this.start() && index < this.end();
	}

	/**
	 * subarray 的别名 <br>
	 * 创建分区：<br>
	 * 首部位置元素为:data[i+start],<br>
	 * 尾部元素为:data[Math.min(i + length, this.length())+start]
	 *
	 * <br>
	 * final var data = nd(i -> i, 5); <br>
	 * nd(i -> data.build1(i, 3), data.length()) <br>
	 * [[0,1,2],[1,2,3],[2,3,4],[3,4],[4]]<br>
	 *
	 * @param start 开始索引,相对于this.start的偏移,从0开始,inclusive
	 * @param size  生成的nd长度
	 * @return ndarray
	 */
	default INdarray<T> build1(final int start, final int size) {
		return this.subarray(start, Math.min(start + size, this.length()));
	}

	/**
	 * subarray 的别名 <br>
	 * 创建分区：<br>
	 * 首部位置元素为:data[i+start],<br>
	 * 尾部元素为:data[Math.min(i + length, this.length())+start]<br>
	 * <br>
	 * final var data = nd(i -> i, 5); <br>
	 * nd(i -> data.build2(i, 3), data.length()) <br>
	 * [[],[0],[0,1],[0,1,2],[1,2,3]]<br>
	 *
	 * @param end  结尾索引,相对于this.start的偏移,从0开始,exclusive
	 * @param size 生成的nd长度
	 * @return ndarray
	 */
	default INdarray<T> build2(final int end, final int size) {
		return this.subarray(Math.max(0, end - size), Math.min(end, this.length()));
	}

	/**
	 * subarray 的别名 <br>
	 * 创建分区：首部位置元素为data[i+start],尾部元素为:data[start+j-1]
	 *
	 * @param i 开始索引,相对于this.start的偏移,从0开始,inclusive
	 * @param j 结束索引,相对于this.start的偏移,从0开始,exclusive
	 * @return ndarray
	 */
	default INdarray<T> build(final int i, final int j) {
		return this.subarray(i, j);
	}

	/**
	 * subarray 的别名 <br>
	 * 创建分区：首部位置元素为data[i+start],尾部元素为:data[start+j-1]
	 *
	 * @param i 开始索引,相对于this.start的偏移,从0开始,inclusive
	 * @return ndarray
	 */
	default INdarray<T> build(final int i) {
		return this.build(i, this.end());
	}

	/**
	 * 构建一个点元素
	 *
	 * @param i 开始索引,相对于this.start的偏移,从0开始,inclusive
	 * @return point
	 */
	default IPoint<T> point(final int i) {
		return INdarray.point(this.data(), this.start() + i);
	}

	/**
	 * 构建一个点元素 首位元素的点元素
	 *
	 * @return point head
	 */
	default IPoint<T> point() {
		return this.point(0);
	}

	/**
	 * 创建分区：首部位置元素为data[i+start],尾部元素为:data[start+j-1]
	 *
	 * @param i 开始索引,相对于this.start的偏移,从0开始,inclusive
	 * @param j 结束索引,相对于this.start的偏移,从0开始,exclusive
	 * @return ndarray
	 */
	default INdarray<T> subarray(final int i, final int j) {
		final int _start = this.start() + i;
		final int n = this.length();
		final int _end = this.start() + Math.min(j, n);
		return this.create(this.data(), _start, _end, this.strides());
	}

	/**
	 * 尝试保持开始位置不变拉伸至size为n
	 *
	 * @param n 拉伸的nd长度,若超过了data的长度则给予截取。<br>
	 *          n>0 向右侧拉长,以 datalen为限度<br>
	 *          n<0 向左侧拉长,以 0为限度
	 * @return ndarray
	 */
	default INdarray<T> resize(final int n) {
		final int s = this.start();
		return n > 0 // 正数右侧，负数左侧
				? this.create(s, Math.min(n + s, this.datalen())) // 右侧拉伸
				: this.create(Math.max(0, this.end() + n), this.end()); // 左侧拉伸
	}

	/**
	 * 左右平移
	 *
	 * @param offset 左右平移动,保持length不变, 负数左移，正数右移动
	 * @return ndarray
	 */
	default INdarray<T> shift(final int offset) {
		final int s = this.start() + offset;
		final int e = this.end() + offset;

		return this.create(this.data(), Math.max(0, s), Math.min(e, this.datalen()));
	}

	/**
	 * 创建分区：首部位置元素为data[i]
	 *
	 * @param i 开始索引,相对于this.start的偏移,从0开始,inclusive,<br>
	 *          this.legnth(); 默认为 j 结束索引,相对于this.start的偏移,从0开始,exclusive
	 * @return ndarray
	 */
	default INdarray<T> subarray(final int i) {
		return this.subarray(i, this.length());
	}

	/**
	 * 生成长度为1的INdarray序列:等价于subset() <br>
	 * 生成一个[[nd1],[nd2],...]长度为1的数组序列
	 *
	 * @return 提取子集合（生成了新的nd,数据data不再是引用了)
	 */
	default INdarray<INdarray<T>> entries() {
		return this.entrieS().collect(ndndclc(this.length()));
	}

	/**
	 * 生成长度为1的INdarray序列: 等价于subset().stream()<br>
	 * 生成一个[[nd1],[nd2],...]长度为1的数组序列
	 *
	 * @return 提取子集合（生成了新的nd,数据data不再是引用了)
	 */
	default Stream<INdarray<T>> entrieS() {
		final int len = this.length();
		return Stream.iterate(0, i -> i + 1).limit(len).map(i -> this.build(i, Math.min(i + 1, len)));
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想) <br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始,t:数据元素)->bool
	 * @param builder   i ：索引从0开始->u 索引构造函数
	 * @return 提取子集合(生成了新的nd, 外部数据data不再是引用了源data, 内部data依旧保持引用源data),
	 *         保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default <U> Stream<U> subsetS(final BiPredicate<Integer, T> predicate, Function<Integer, U> builder) {
		return Stream.iterate(0, i -> i + 1).limit(this.length()).filter(i -> predicate.test(i, this.get(i)))
				.map(builder);
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想) <br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始,t:数据元素)->bool
	 * @param n         选取的nd的长度
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default Stream<INdarray<T>> subsetS(final BiPredicate<Integer, T> predicate, final int n) {
		return this.subsetS(predicate, i -> this.build(i, Math.min(i + n, this.length())));
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想)<br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始)->bool
	 * @param n         选取的nd的长度
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default Stream<INdarray<T>> subsetS(final Predicate<Integer> predicate, final int n) {
		return this.subsetS((i, t) -> predicate.test(i), n);
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想)<br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始)->bool
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default Stream<IPoint<T>> subsetS(final Predicate<Integer> predicate) {
		return this.subsetS((i, t) -> predicate.test(i), this::point);
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想) <br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始,t:数据元素)->bool
	 * @param n         选取的nd的长度
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default INdarray<INdarray<T>> subset(final BiPredicate<Integer, T> predicate, final int n) {
		return this.subsetS(predicate, n).collect(ndndclc(this.length()));
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想)<br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始)->bool
	 * @param n         选取的nd的长度
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default INdarray<INdarray<T>> subset(final Predicate<Integer> predicate, final int n) {
		return this.subsetS(predicate, n).collect(ndndclc(this.length()));
	}

	/**
	 * 这是一个把非连续转换为连续空间的关键方法(思想)<br>
	 * 提取指定predicate位置：这个一种把非连续变为连续的思想。用间接来构造连续比邻。
	 *
	 * @param predicate (i:索引从0开始)->bool
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default INdarray<IPoint<T>> subset(final Predicate<Integer> predicate) {
		return this.subsetS((i, t) -> predicate.test(i), this::point).collect(ndptclc(this.length()));
	}

	/**
	 * <br>
	 * 提取指定.这是一个把非连续转换为连续空间的关键方法(思想)
	 *
	 * @param indices 切分索引，切分长度为1的INdarray, 保留indices顺序，保留重复索引值
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序
	 */
	default INdarray<INdarray<T>> subset2(final int... indices) {
		return IntStream.of(indices).mapToObj(this::point).collect(ndndclc(this.length()));
	}

	/**
	 * <br>
	 * 提取指定.这是一个把非连续转换为连续空间的关键方法(思想)
	 *
	 * @param indices 切分索引，切分长度为1的INdarray
	 * @return 提取子集合(生成了新的nd,外部数据data不再是引用了源data,内部data依旧保持引用源data),保留引用顺序，忽略重复值，重复索引按照最后出现的索引循序。
	 */
	default INdarray<IPoint<T>> subset(final int... indices) {
		final TreeMap<Integer, Integer> m1 = new TreeMap<>();
		final Map<Integer, Integer> m2 = new TreeMap<>();
		final List<Integer> _indices = IntStream.of(indices).boxed().collect(Collectors.toList());

		_indices.stream().map(Tuple2.snb(0)).forEach(e -> {
			m1.put(e._2, e._1); // key,offset1
		}); // index 在 indices参数中的位置
		m1.entrySet().stream().map(Tuple2.snb(0)).forEach(e -> { // 结果排序对应的参数排序的位置。
			m2.put(e._1, e._2.getValue()); // offset2->offfset1
		});

		return this.subsetS(_indices::contains) //
				.map(Tuple2.snb(0)) //
				.sorted(Comparator.comparingInt(k -> m2.get(k._1))) //
				.map(e -> e._2).collect(ndptclc(this.length()));
	}

	/**
	 * <br>
	 * 生成一个[[nd1],[nd2],...]长度为1的数组序列
	 *
	 * @return 提取子集合（生成了新的nd,数据data不再是引用了)
	 */
	default INdarray<IPoint<T>> subset() {
		return this.subset(t -> true);
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]]
	 *
	 * @param step 大于等一1的整数，滑动步长
	 * @param size 窗口大小
	 * @param flag 窗口大小是否齐次,true:大小都是size，去除小窗口,false:保留尾部的费不满足大小的小窗口。
	 * @return 滑动窗口元素流
	 */
	default Stream<INdarray<T>> slidingS(final int step, final int size, final boolean flag) {
		final Stream<INdarray<T>> ndS = size < 1 ? Stream.empty() : this.subsetS(i -> i % step == 0, size);
		return flag ? ndS.filter(e -> e.length() == size) : ndS;
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]] <br>
	 * 窗口非齐次即保留小窗口
	 *
	 * @param step 大于等一1的整数，滑动步长
	 * @param size 窗口大小
	 * @return 滑动窗口元素流
	 */
	default Stream<INdarray<T>> slidingS(final int step, final int size) {
		return this.slidingS(step, size, false);
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]] <br>
	 * <br>
	 * <p>
	 * step 滑动步长 默认为1
	 *
	 * @param size 窗口大小
	 * @return 滑动窗口元素流
	 */
	default Stream<INdarray<T>> slidingS(final int size) {
		return this.slidingS(1, size);
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]]
	 *
	 * @param step 大于等一1的整数，滑动步长
	 * @param size 窗口大小
	 * @param flag 窗口大小是否齐次,true:大小都是size，去除小窗口,false:保留尾部的费不满足大小的小窗口。
	 * @return 滑动窗口集合
	 */
	default INdarray<INdarray<T>> slidings(final int step, final int size, final boolean flag) {
		return this.slidingS(step, size, flag).collect(ndndclc(this.length()));
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]] <br>
	 * 窗口非齐次即保留小窗口
	 *
	 * @param step 大于等一1的整数，滑动步长
	 * @param size 窗口大小
	 * @return 滑动窗口集合
	 */
	default INdarray<INdarray<T>> slidings(final int step, final int size) {
		return this.slidingS(step, size, false).collect(ndclc());
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]] <br>
	 * <br>
	 * <p>
	 * step 滑动步长 默认为1
	 *
	 * @param size 窗口大小
	 * @return 滑动窗口集合
	 */
	default INdarray<INdarray<T>> slidings(final int size) {
		return this.slidingS(1, size).collect(ndndclc(this.length()));
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]] <br>
	 * <br>
	 * <p>
	 * step 滑动步长 默认为1
	 *
	 * @param n    移动步长 与 窗口大小 , 大于等于1的整数
	 * @param flag 窗口大小是否齐次,true:大小都是size，去除小窗口,false:保留尾部的费不满足大小的小窗口。
	 * @return 滑动窗口集合
	 */
	default INdarray<INdarray<T>> cuts(final int n, final boolean flag) {
		return this.slidings(n, n, flag);
	}

	/**
	 * 滑动窗口 <br>
	 * nd=[1,2,3,4,5]的 <br>
	 * nd.slidings(1,2) -> [0,1],[1,2],[2,3],[3,4],[4,5]] <br>
	 * nd.slidings(2, 3) -> [[0,1,2],[2,3,4],[4]] <br>
	 * <br>
	 *
	 * @param n 移动步长 与 窗口大小 , 大于等于1的整数
	 * @return 滑动窗口集合
	 */
	default INdarray<INdarray<T>> cuts(final int n) {
		return this.slidings(n, n, false);
	}

	/**
	 * 相对索引访问 <br>
	 * 检查所索引并运行
	 *
	 * @param i         位置(相对)索引从0开始,大于等于0,小于length,<br>
	 *                  否则会抛出 ArrayIndexOutOfBoundsException
	 * @param evaluator 计算器
	 * @throws ArrayIndexOutOfBoundsException i:相对位置索引,index:绝对索引,length:ndarray的长度
	 */
	default <U> U checkStatus(final int i, final Function<Integer, U> evaluator) {
		final int index = this.start() + i;

		if (index >= this.start() && index < this.end()) {
			return evaluator.apply(index);
		} else {
			final int start = this.start();
			final int end = this.end();
			final String line = String.format( //
					"i:%s,index:%s,{start:%s,end:%s,nd-length:%s,data-length:%s}", //
					i, index, start, end, this.length(), this.data() == null ? null : this.length());
			throw new ArrayIndexOutOfBoundsException(line);
		} // if
	}

	/**
	 * 采用stride针对data进行分区 <br>
	 * <p>
	 * 返回多维数组：对data 进行重新划分,不再以nd的start,end为界限
	 *
	 * @return ndarry map
	 */
	default Map<List<Integer>, INdarray<T>> partitions() {
		final Map<List<Integer>, INdarray<T>> ndarrays = new LinkedHashMap<>();
		if (this.strides() != null) {
			this.strides().forEach((index, range) -> ndarrays.put(index, this.create(range._1, range._2)));
		} // if
		return ndarrays;
	}

	/**
	 * 采用stride针对data进行分区 <br>
	 * <p>
	 * 返回多维数组: 对data 进行重新划分,不再以nd的s tart,end为界限
	 *
	 * @return ndarray 数据流
	 */
	default Stream<INdarray<T>> partitionS() {
		return this.strides().values().stream().map(Tuple2.bifun(this::create));
	}

	/**
	 * 根据索引进行区段切分 (splits 别名)
	 *
	 * @param breaks 相对索引， 分割点不包含0,与length [...,i,i+n,...,]
	 * @return 分区点
	 */
	default List<INdarray<T>> partitions(final int... breaks) {
		return this.splits(breaks);
	}

	/**
	 * 根据索引进行区段切分(splitS 别名)
	 *
	 * @param breaks 相对索引，分割点不包含0,与length [...,i,i+n,...,]
	 * @return 分区点
	 */
	default Stream<INdarray<T>> partitionS(final int... breaks) {
		return this.splitS(breaks);
	}

	/**
	 * split 别名 <br>
	 * 从i索引处拆分,([0,i),[i+1,size)) 即 1#元素长度为i,2#元素长度为length-i
	 *
	 * @param i 相对索引，拆分索引大于0小于length,i位于 第二即 _2位置的第一个元素。
	 * @return ([0, i)长度为i, [i+1,size)长度为length-i)
	 */
	default Tuple2<INdarray<T>, INdarray<T>> partition(final int i) {
		return this.split(i);
	}

	/**
	 * 分割点
	 *
	 * @param breaks 相对索引，索引分割点从0开始,不包含0与length [...,i,i+n,...,]
	 * @return 区间切分点[(0, i), (i, .), ..., (., length)]
	 */
	default Stream<Tuple2<Integer, Integer>> breakS(final int[] breaks) {
		final int start = this.start();
		final IntStream s = IntStream.of(breaks).filter(e -> e != 0 && checkIndex(e)).distinct().sorted();
		final int[] _breaks = Stream.of(IntStream.of(0), s, IntStream.of(this.length())) //
				.flatMapToInt(Objects::requireNonNull).toArray();
		final int n = _breaks.length;
		return Stream.iterate(1, i -> i + 1).limit(n - 1)
				.map(i -> Tuple2.of(_breaks[i - 1] + start, _breaks[i] + start));
	}

	/**
	 * 从i索引处拆分,([0,i),[i+1,size)) 即 1#元素长度为i,2#元素长度为length-i
	 *
	 * @param i 拆分索引大于0小于length,i位于 第二即 _2位置的第一个元素。
	 * @return ([0, i)长度为i, [i+1,size)长度为length-i)
	 */
	default Tuple2<INdarray<T>, INdarray<T>> split(final int i) {
		return Tuple2.of(this.subarray(0, i), this.subarray(i));
	}

	/**
	 * 从i索引处拆分,([0,i),[i+1,length)) 即 1#元素长度为i,2#元素长度为length-i
	 *
	 * @param breaks 相对索引，索引分割点从0开始,不包含0与length [...,i,i+n,...,]
	 * @return ([0, i)长度为i, [i+1,size)长度为length-i)
	 */
	default List<INdarray<T>> splits(final int... breaks) {
		return this.splitS(breaks).collect(Collectors.toList());
	}

	/**
	 * 从i索引处拆分,([0,i),[i+1,length)) 即 1#元素长度为i,2#元素长度为length-i
	 *
	 * @param breaks 相对索引，索引分割点从0开始,不包含0与length [...,i,i+n,...,]
	 * @return ([0, i)长度为i, [i+1,size)长度为length-i)
	 */
	default Stream<INdarray<T>> splitS(final int... breaks) {
		return this.breakS(breaks).filter(e -> e._1 < this.end()) //
				.map(t -> t.fmap2(_2 -> Math.min(_2, this.end()))) //
				.map(Tuple2.bifun(this::create));
	}

	/**
	 * 数据透视表 & 递归分组计算的 指标框架
	 * 
	 * @param <K>         键名索引类型
	 * @param <INDICATOR> 核算指标
	 * @param evaluator   核算器[t]->indicator,
	 *                    核算器并发执行可以依据枢轴的分类层级实现对源数据进行多级划分进而实现分组/批次计算能力:例如分库分表，惨见H2Test.qux
	 * @param classifiers 枢轴：分类函数序列 [cf1,cf2,...], 分类函数cf,把一组t元素映射成键名索引k:[t]->k
	 * @return 数据透视表,依据分类函数序列classifiers指定枢轴。
	 */
	default <K extends Comparable<K>, INDICATOR> Map<K, Object> pivotTable(
			final Function<INdarray<T>, INDICATOR> evaluator, final Iterable<Function<T, K>> classifiers) {
		return this.pivotTable(evaluator, Types.itr2array(classifiers));
	}

	/**
	 * 数据透视表 & 递归分组计算的 指标框架
	 * 
	 * @param <K>         键名索引类型
	 * @param <INDICATOR> 核算指标
	 * @param evaluator   核算器[t]->indicator,
	 *                    核算器并发执行可以依据枢轴的分类层级实现对源数据进行多级划分进而实现分组/批次计算能力:例如分库分表，惨见H2Test.qux
	 * @param classifiers 枢轴：分类函数序列 [cf1,cf2,...], 分类函数cf,把一组t元素映射成键名索引k:[t]->k
	 * @return 数据透视表,依据分类函数序列classifiers指定枢轴。
	 */
	default <K extends Comparable<K>, INDICATOR> Map<K, Object> pivotTable(
			final Function<INdarray<T>, INDICATOR> evaluator, final Stream<Function<T, K>> classifiers) {
		return this.pivotTable(evaluator, Types.stream2array(classifiers));
	}

	/**
	 * 数据透视表 & 递归分组计算的 指标框架
	 * 
	 * @param <K>         键名索引类型
	 * @param <INDICATOR> 核算指标类型
	 * @param <CF>        分类函数类型
	 * @param evaluator   核算器[t]->indicator,
	 *                    核算器并发执行可以依据枢轴的分类层级实现对源数据进行多级划分进而实现分组/批次计算能力:例如分库分表，惨见H2Test.qux
	 * @param classifiers 枢轴：分类函数序列 [cf1,cf2,...], 分类函数cf,把一组t元素映射成键名索引k:[t]->k
	 * @return 数据透视表,依据分类函数序列classifiers指定枢轴。
	 */
	default <K extends Comparable<K>, INDICATOR, CF extends Function<T, K>> Map<K, Object> pivotTable(
			final Function<INdarray<T>, INDICATOR> evaluator, final CF[] classifiers) {
		final Map<K, Object> pvtdatas = new ConcurrentHashMap<>(); // 透视表结果，用于结果返回。

		if (null != classifiers && classifiers.length > 0) { // 分类函数非空,
			final Consumer<Function<INdarray<T>, ?>> cs = f -> this.groupBy(classifiers[0]) // 提取枢轴上的首位分类函数进行分组计算
					.entrySet().stream().parallel() // 为每个键建立一个指标计算线程 进行 并发计算。
					.forEach(e -> pvtdatas.put(e.getKey(), null == f ? e.getValue() : f.apply(e.getValue()))); // 分类指标核算
			cs.accept(classifiers.length == 1 // 枢轴(分类函数序列)长度评估:是否抵达枢轴末端即枢轴长度递减至1
					? evaluator // 抵达枢轴末端则执行指标计算
					: nd -> nd.pivotTable(evaluator, Arrays.copyOfRange(classifiers, 1, classifiers.length))); // 未抵达枢轴末尾,继续沿着枢轴计算,递归进入下一阶层做分类统计。
		} // if classifiers

		return pvtdatas;
	}

	/**
	 * 分组 &排序
	 * 
	 * @param <K>        分类键类型
	 * @param classifier 分类器 把T转换成分组键名
	 * @return {(k,nd)}
	 */
	default <K extends Comparable<K>> Map<K, INdarray<T>> groupBy(final Function<T, K> classifier) {
		final Map<K, INdarray<T>> groups = new LinkedHashMap<>(); // 分组结果
		final INdarray<T> ndata = this.dupdata(); // 备份当前数据区域已作为后续的索引排序的数据源。
		final Iterator<Tuple2<K, Integer>> key_itr = this.map(classifier) // 计算键值（索引排序的键索引）
				.map(Tuple2.snb2(0)).sorted().iterator(); // 计算键值并排序

		int i = 0; // 当前读取位置
		int start = i; // 开始位置
		K key = null; // 先前key:当前的分组键名
		while (key_itr.hasNext()) {
			final Tuple2<K, Integer> e = key_itr.next(); // 提取
			if (key != null && key != e._1) { // key 发生变化，将先前的数据合并一个区域分组。
				groups.put(key, this.build(start, i)); // 注意要使用相对位置构造nd,即build不能create,以保证动态性
				start = i; // 更新开始位置
			} //
			key = e._1; // 记录键值
			this.set(i++, ndata.get(e._2)); // 对当前数据区域的数据进行排序
		} // while
		if (key != null && start < i) { // 记录索引结果
			groups.put(key, this.build(start, i)); // 注意要使用相对位置构造nd,即build不能create,以保证动态性
		} // if

		return groups; // 返回分组
	}

	/**
	 * <b>关键的类:相对访问的终点实现</br>
	 * this.get(0)的简写，方便对长度为1的INdarray进行处; 读取位置索引的元素值 <br>
	 * 非法索引会抛出 ArrayIndexOutOfBoundsException <br>
	 * <p>
	 * 默认位置索引为0
	 *
	 * @return 位置索引的元素值
	 */
	default T get() {
		return this.get(0);
	}

	/**
	 * <b>关键的类:相对访问的终点实现</br>
	 * 读取位置索引的元素值 <br>
	 * 非法索引会抛出 ArrayIndexOutOfBoundsException
	 *
	 * @param i 位置索引从0开始
	 * @return 位置索引的元素值
	 */
	default T get(final int i) {
		return this.checkStatus(i, _i -> this.data()[_i]);
	}

	/**
	 * 读取位置索引的元素值
	 *
	 * @param index 位置索引向量[_index:nd索引,i:元素偏移]
	 * @return 位置索引的元素值
	 */
	default T get(final int... index) {
		if (index.length < 1) {
			return null;
		} else if (index.length < 2) {
			return this.get(index[0]);
		} else if (index.length >= 2 && this.strides() != null && this.strides().size() > 0) {
			final int n = index.length;
			final int[] _index = Arrays.copyOfRange(index, 0, n - 2);
			final int i = index[n - 1];
			return Optional.ofNullable(this.getnd(_index)).map(nd -> nd.get(i)).orElse(null);
		} else {
			return null;
		}
	}

	/**
	 * mutate 函数别名
	 *
	 * @param mapper 数据映射
	 * @return U 类型结果
	 */
	default <U> U get(final Function<INdarray<T>, U> mapper) {
		return this.mutate(mapper);
	}

	/**
	 * 读取位置索引的元素值
	 *
	 * @param i 位置索引从0开始
	 * @return 位置索引的元素值
	 */
	default Optional<T> getopt(final int i) {
		return Optional.ofNullable(this.get(i));
	}

	/**
	 * 区间索引
	 *
	 * @param index 区间索引
	 * @return INdarray
	 */
	default INdarray<T> getnd(final int... index) {
		final List<Integer> _index = IntStream.of(index).boxed().collect(Collectors.toList());
		return this.getnd(_index);
	}

	/**
	 * 区间索引
	 *
	 * @param index 区间索引
	 * @return INdarray
	 */
	default INdarray<T> getnd(final Integer[] index) {
		return this.getnd(Arrays.asList(index));
	}

	/**
	 * 区间索引
	 *
	 * @param index 区间索引
	 * @return INdarray
	 */
	default INdarray<T> getnd(final List<Integer> index) {
		final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides = this.strides();
		return Optional.ofNullable(strides) //
				.map(_strides -> _strides.get(index)).map(Tuple2.bifun(this::create)).orElse(null);
	}

	/**
	 * 区间索引 getnd 的别名用于lambda标记
	 *
	 * @param index 区间索引
	 * @return INdarray
	 */
	default INdarray<T> getndi(final int[] index) {
		return this.getnd(index);
	}

	/**
	 * 区间索引 getnd 的别名用于lambda标记
	 *
	 * @param index 区间索引
	 * @return INdarray
	 */
	default INdarray<T> getndI(final Integer[] index) {
		return this.getnd(index);
	}

	/**
	 * 区间索引 getnd 的别名用于lambda标记
	 *
	 * @param index 区间索引
	 * @return INdarray
	 */
	default INdarray<T> getndl(final List<Integer> index) {
		return this.getnd(index);
	}

	/**
	 * 设置指定索引位置的元素值. <br>
	 * this.set(0,t)的简写，方便对长度为1的INdarray进行处理。
	 *
	 * @param t 元素值， 索引位置默认为0
	 * @return 区段对象
	 */
	default INdarray<T> set(final T t) {
		return this.set(0, t);
	}

	/**
	 * 设置指定索引位置的元素值
	 *
	 * @param i 位置索引从0开始
	 * @param t 元素值
	 * @return 区段对象
	 */
	default INdarray<T> set(final int i, final T t) {
		return this.checkStatus(i, index -> {
			this.data()[index] = t;
			return this;
		});
	}

	/**
	 * 修改INdarray元素的内容<br>
	 * <p>
	 * assign 可以使用 subset 对INdarray 进行过滤选择，而后通过TriConsumer的setter进行修改数据 <br>
	 * <code>
	 * nd.subset(i -> i % 2 == 0).assign((n, i, t) -> t.set(t.get() * 10));
	 * </code>
	 *
	 * @param <ND>   ND衍生类型
	 * @param setter 设置方法:(nd:原先的ndarray,i:索引,t:原先的值)->{}
	 * @return ndarray this对象本身
	 */
	@SuppressWarnings("unchecked")
	default <ND extends INdarray<T>> INdarray<T> assign(final TriConsumer<ND, Integer, T> setter) {
		for (int i = 0; i < this.length(); i++) {
			setter.accept((ND) this, i, this.get(i));
		}
		return this;
	}

	/**
	 * 修改INdarray元素的内容<br>
	 * <p>
	 * assign 可以使用 subset 对INdarray 进行过滤选择，而后通过TriConsumer的setter进行修改数据 <br>
	 * <code>
	 * nd.subset(i -> i % 2 == 0).assign((n, i, t) -> t.set(t.get() * 10));
	 * </code>
	 *
	 * @param setter 设置方法:(i:索引,t:原先的值)->{}
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final BiConsumer<Integer, T> setter) {
		for (int i = 0; i < this.length(); i++) {
			setter.accept(i, this.get(i));
		}
		return this;
	}

	/**
	 * 修改INdarray元素的内容<br>
	 * <p>
	 * assign 可以使用 subset 对INdarray 进行过滤选择，而后通过TriConsumer的setter进行修改数据 <br>
	 * <code>
	 * nd.subset(i -> i % 2 == 0).assign((n, i, t) -> t.set(t.get() * 10));
	 * </code>
	 *
	 * @param setter 设置方法:(t:原先的值)->{}
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final Consumer<T> setter) {
		for (int i = 0; i < this.length(); i++) {
			setter.accept(this.get(i));
		}
		return this;
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param data   数据源
	 * @param offset starting position in this data. 从0开始
	 * @param setter 根据索引设置值的函数,(i,t)->this.set(i,t);
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final Iterable<T> data, final int offset, final BiConsumer<Integer, T> setter) {
		if (data != null) {
			final Iterator<T> itr = data.iterator();
			for (int i = 0; i < this.length(); i++) {
				if (!itr.hasNext()) {
					break;
				} else {
					if (i >= offset) { // 从offset处开始数据写入
						Optional.ofNullable(setter).orElse(this::set).accept(i, itr.next());
					} // if
				} // if
			} // for
		} // if

		return this;
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param data   数据源
	 * @param offset starting position in this data. 从0开始
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final Iterable<T> data, final int offset) {
		return this.assign(data, offset, null);
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param data   数据源
	 * @param setter 根据索引设置值的函数,(i,t)->this.set(i,t);
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final Iterable<T> data, final BiConsumer<Integer, T> setter) {
		return this.assign(data, 0, setter);
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param data 数据源
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final Iterable<T> data) {
		return this.assign(data, 0);
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param data   数据源
	 * @param offset starting position in this data. 从0开始
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final T[] data, final int offset) {
		this.assign(Arrays.asList(data), offset);
		return this;
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param data 数据源
	 * @return ndarray this对象本身
	 */
	default INdarray<T> assign(final T[] data) {
		return this.assign(data, 0);
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param <U>    us 对象类型
	 * @param us     参数类型
	 * @param offset 偏移位置从0开始
	 * @param biop   数据操作 (t,u)->u
	 * @return ndarray this对象本身
	 */
	default <U> INdarray<T> assign2(final Iterable<U> us, final int offset, final BiFunction<T, U, T> biop) {
		final INdarray<U> _us = us instanceof INdarray ? (INdarray<U>) us
				: StreamSupport.stream(us.spliterator(), false).collect(INdarray.ndclc(this.length()));
		this.subarray(offset).subset().assign((i, t) -> {
			final T _t = biop.apply(t.get(), _us.get(i % _us.length()));
			t.set(_t);
		});
		return this;
	}

	/**
	 * 把data中的数据拷贝到当前nd的数据区data
	 *
	 * @param <U>  us 对象类型
	 * @param us   参数类型
	 * @param biop 数据操作 (t,u)->u
	 * @return ndarray this对象本身
	 */
	default <U> INdarray<T> assign2(final Iterable<U> us, final BiFunction<T, U, T> biop) {
		return this.assign2(us, 0, biop);
	}

	/**
	 * car cdr
	 *
	 * @return (head, tail)
	 */
	default Tuple2<T, INdarray<T>> carcdr() {
		return Tuple2.of(this.head(), this.tail());
	}

	/**
	 * 头部元素
	 *
	 * @return head
	 */
	default T head() {
		return this.length() > 0 ? this.get(0) : null;
	}

	/**
	 * 头部元素
	 *
	 * @param n 长度
	 * @return head
	 */
	default INdarray<T> head(final int n) {
		final int _n = Math.min(this.length(), n);
		return this.create(this.start(), this.start() + _n);
	}

	/**
	 * 头部元素
	 *
	 * @return headS
	 */
	default Stream<INdarray<T>> headS() {
		return Stream.iterate(1, i -> i <= this.length(), i -> i + 1).map(this::head);
	}

	/**
	 * 头部元素 <br>
	 * nd(1,2,3,4).heads() <br>
	 * [[1],[1,2],[1,2,3],[1,2,3,4]] <br>
	 *
	 * @return heads
	 */
	default INdarray<INdarray<T>> heads() {
		return this.headS().collect(ndndclc(this.length()));
	}

	/**
	 * 尾部元素
	 *
	 * @return tail
	 */
	default INdarray<T> tail() {
		if (this.start() + 1 < this.end())
			return this.create(this.start() + 1);
		else
			return null;
	}

	/**
	 * 尾部元素
	 *
	 * @param n 长度
	 * @return tail
	 */
	default INdarray<T> tail(final int n) {
		final int s = this.start() + (this.length() > n ? (this.length() - n) : 0);
		return this.create(s, this.end());
	}

	/**
	 * 尾部元素
	 *
	 * @return tailS
	 */
	default Stream<INdarray<T>> tailS() {
		return Stream.iterate(1, i -> i <= this.length(), i -> i + 1).map(this::tail);
	}

	/**
	 * 尾部元素 <br>
	 *
	 * nd(1,2,3,4).tails() <br>
	 * [[4],[3,4],[2,3,4],[1,2,3,4]] <br>
	 *
	 * @return tails
	 */
	default INdarray<INdarray<T>> tails() {
		return this.tailS().collect(ndndclc(this.length()));
	}

	/**
	 * 最后以元素
	 *
	 * @return last
	 */
	default T last() {
		return this.get(this.end() - 1);
	}

	/**
	 * 最后n个元素 tail(n) 的别名
	 *
	 * @param n 最后末尾的n个元素
	 * @return ndarray
	 */
	default INdarray<T> last(final int n) {
		return this.tail(n);
	}

	/**
	 * 前部元素
	 *
	 * @return initial 除掉最后一个元素所剩余的元素
	 */
	default INdarray<T> initial() {
		if (this.end() - 1 > this.start())
			return this.create(this.start(), this.end() - 1);
		else
			return null;
	}

	/**
	 * 前部元素
	 *
	 * @param n 前部的哥哥元素
	 * @return initial 除掉最后一个元素所剩余的元素
	 */
	default INdarray<T> intial(final int n) {
		if (this.end() - n > this.start())
			return this.create(this.start(), this.start() + n);
		else
			return null;
	}

	/**
	 * 首尾顺序调转(start位于最后,end-1位于第一)
	 *
	 * @return 首尾顺序调转(start位于最后, end - 1位于第一)
	 */
	default INdarray<T> reverse() {
		if (this.start() >= this.end()) {
			// do nothing
		} else {
			for (int i = 0, j = this.length() - 1; i < j; i++, j--) {
				final T t = this.get(i);
				this.set(i, this.get(j));
				this.set(j, t);
			} // for
		}
		return this;
	}

	/**
	 * 数据流
	 *
	 * @return 数据流
	 */
	default Stream<T> stream() {
		return Stream.iterate(0, i -> i + 1).limit(this.length()).map(this::get);
	}

	/**
	 * functor 来源于 haskell 的思想
	 *
	 * @param <U>    结果元素类型
	 * @param mapper t->u
	 * @return ndarray
	 */
	default <U> INdarray<U> fmap(final Function<T, U> mapper) {
		return this.map(mapper).collect(INdarray.ndclc(this.length()));
	}

	/**
	 * functor 来源于 haskell 的思想 <br>
	 * eg: <br>
	 * 提取最近3天数据 <br>
	 * data.fmap((i, p) -> data.build(Math.max(i - 3, 0), i)) <br>
	 * [[],[1],[1,2],[1,2,3],[2,3,4],[3,4,5],...]
	 *
	 * @param <U>    结果元素类型
	 * @param mapper (i:索引从0开始,t:元素内容)->u
	 * @return ndarray
	 */
	default <U> INdarray<U> fmap(final BiFunction<Integer, T, U> mapper) {
		return this.map(mapper).collect(INdarray.ndclc(this.length()));
	}

	/**
	 * 扁平化
	 *
	 * @param <U>      结果元素类型
	 * @param <X>      U的元素类型
	 * @param streamer t->[x]
	 * @return ndarray,返回对rawdata复制
	 */
	default <X, U extends Iterable<X>> INdarray<X> fflat(final Function<T, U> streamer) {
		final X[] us = this.flatMap(e -> StreamSupport.stream(streamer.apply(e).spliterator(), false))
				.collect(Types.arrayclc());
		return of(us);
	}

	/**
	 * 扁平化
	 *
	 * @param <X> U的元素类型
	 * @return ndarray,返回对rawdata复制
	 */
	@SuppressWarnings("unchecked")
	default <X> INdarray<X> fflat() {
		return this.fflat(e -> (Iterable<X>) ((e instanceof Iterable) ? e : Arrays.asList(e)));
	}

	/**
	 * 扁平化
	 *
	 * @param <U>      结果元素类型
	 * @param <X>      U的元素类型
	 * @param streamer t->[x]
	 * @return ndarray
	 */
	default <X, U extends Stream<X>> INdarray<X> fflatMap(final Function<T, U> streamer) {
		final X[] us = this.flatMap(e -> StreamSupport.stream(streamer.apply(e).spliterator(), false))
				.collect(Types.arrayclc());
		return of(us);
	}

	/**
	 * 把t与u拉链到一起
	 *
	 * @param <U> 第二元素类型
	 * @param us  第二元素
	 * @return 拉链后的ndarray
	 */
	default <U> INdarray<Tuple2<T, U>> zip(final INdarray<U> us) {
		return INdarray.zip(this, us);
	}

	/**
	 * 左侧扫描
	 *
	 * @param <U>    结果元素类型
	 * @param mapper nd->u
	 * @return 左侧扫描
	 */
	default <U> Stream<U> scanlS(final Function<INdarray<T>, U> mapper) {
		final int n = this.length();
		return Stream.iterate(0, i -> i < n, i -> i + 1).map(i -> this.head(i + 1)).map(mapper);
	}

	/**
	 * 左侧扫描
	 *
	 * @param <U>    结果元素类型
	 * @param mapper nd->u
	 * @return 左侧扫描
	 */
	default <U> INdarray<U> scanls(final Function<INdarray<T>, U> mapper) {
		return this.scanlS(mapper).collect(ndclc());
	}

	/**
	 * 右侧扫描
	 *
	 * @param <U>    结果元素类型
	 * @param mapper nd->u
	 * @return 右侧扫描
	 */
	default <U> Stream<U> scanrS(final Function<INdarray<T>, U> mapper) {
		final int n = this.length();
		return Stream.iterate(0, i -> i < n, i -> i + 1).map(i -> this.tail(i + 1)).map(mapper);
	}

	/**
	 * 右侧扫描
	 *
	 * @param <U>    结果元素类型
	 * @param mapper nd->u
	 * @return 右侧扫描
	 */
	default <U> INdarray<U> scanrs(final Function<INdarray<T>, U> mapper) {
		return this.scanrS(mapper).collect(ndclc());
	}

	/**
	 * 数字向量
	 *
	 * @param <N>    数据类型
	 * @param mapper 数字类型转换
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default <N extends Number> INdarray<N> nums(final Function<T, N> mapper) {
		final INdarray<N> nums = this.fmap(mapper);
		return Arrays.equals(nums.data(), this.data()) ? (INdarray<N>) this : nums;
	}

	/**
	 * 数字向量
	 *
	 * @param <N>         数据类型
	 * @param placeholder 类型占位符用于指示编译器进行
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default <N extends Number> INdarray<N> nums(final N placeholder) {
		return Number.class.isAssignableFrom(this.dtype()) //
				? (INdarray<N>) this
				: null;
	}

	/**
	 * 数字向量(强转)
	 *
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default INdarray<Integer> ints() {
		if (Objects.equals(this.dtype(), Integer.class))
			return (INdarray<Integer>) this;
		else
			return this.nums((Function<T, Integer>) e -> //
			(e instanceof Integer ? (Integer) e : ((Number) e).intValue()));
	}

	/**
	 * 数字向量(强转)
	 *
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default INdarray<Long> lngs() {
		if (Objects.equals(this.dtype(), Long.class))
			return (INdarray<Long>) this;
		else
			return this.nums((Function<T, Long>) e -> //
			(e instanceof Long ? (Long) e : ((Number) e).longValue()));
	}

	/**
	 * 数字向量(强转)
	 *
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default INdarray<Double> dbls() {
		if (this.dtype() == Double.class) {
			return (INdarray<Double>) this;
		} else {
			return this.nums((Function<T, Double>) o -> //
			Optional.of(o).map(p -> p instanceof INdarray ? ((INdarray<Object>) p).fflat().get() : p)
					.map(e -> (e instanceof Double //
							? (Double) e
							: Optional.ofNullable(Types.obj2dbl(null).apply(e)).orElse(null)))
					.orElse(null));
		} // if
	}

	/**
	 * 数字向量(强转)
	 *
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default INdarray<Float> floats() {
		if (Objects.equals(this.dtype(), Float.class))
			return (INdarray<Float>) this;
		else
			return this.nums((Function<T, Float>) e -> //
			(e instanceof Float ? (Float) e : ((Number) e).floatValue()));
	}

	/**
	 * 数字向量(强转)
	 *
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default INdarray<Short> shorts() {
		if (Objects.equals(this.dtype(), Short.class))
			return (INdarray<Short>) this;
		else
			return this.nums((Function<T, Short>) e -> //
			(e instanceof Short ? (Short) e : ((Number) e).shortValue()));
	}

	/**
	 * 数字向量(强转)
	 *
	 * @return 数字向量
	 */
	@SuppressWarnings("unchecked")
	default INdarray<Byte> bytes() {
		if (Objects.equals(this.dtype(), Byte.class))
			return (INdarray<Byte>) this;
		else
			return this.nums((Function<T, Byte>) e -> //
			(e instanceof Byte ? (Byte) e : ((Number) e).byteValue()));
	}

	/**
	 * 数据元素类型
	 *
	 * @return 数据元素类型
	 */
	@SuppressWarnings("unchecked")
	default Class<T> dtype() {
		return (Class<T>) this.data().getClass().getComponentType();
	}

	/**
	 * 是否为空
	 */
	default boolean empty() {
		return this.start() >= this.end();
	}

	/**
	 * 数据长度 end-start
	 *
	 * @return 数据长度:首尾之间的元素个数
	 */
	default int length() {
		return this.end() - this.start();
	}

	/**
	 * 数据数组的长度:this.data().length
	 *
	 * @return 数据数组的长度
	 */
	default int datalen() {
		return this.data().length;
	}

	/**
	 * 逆序数 <br>
	 * 在一个排列中，如果一对数的前后位置与大小顺序相反，即前面的数大于后面的数，那么它们就称为一个逆序。 一个排列中逆序的总数就称为这个排列的逆序数
	 * 简单的来说逆序数就是前面的数比这个数大，有几个比他大的数逆序数就是几。
	 * 
	 * @return 逆序数
	 */
	default int tau() {
		return INdarray.tau(this.dbls());
	}

	/**
	 * 数据累计和
	 *
	 * @return T 类型数字，非数字返回null
	 */
	default T sum() {
		final Double dbl = Optional.ofNullable(this.dbls()) //
				.flatMap(e -> e.filter(Objects::nonNull).reduce(Double::sum)) //
				.orElse(null);
		return this.asNum(dbl);
	}

	/**
	 * 数据乘积
	 *
	 * @return T 类型数字，非数字返回null
	 */
	default T product() {
		final Double dbl = Optional.ofNullable(this.dbls()) //
				.flatMap(e -> e.filter(Objects::nonNull).reduce((a, b) -> a * b)) //
				.orElse(null);
		return this.asNum(dbl);
	}

	/**
	 * 转换成数字类型
	 *
	 * @param obj 数据
	 * @return 类型数字，非数字返回null
	 */
	@SuppressWarnings("unchecked")
	default T asNum(final Object obj) {
		final Object value = Optional.ofNullable(obj).map(_value -> {
			if (_value instanceof String) {
				try {
					return Double.parseDouble(_value.toString());
				} catch (Exception e) {
					e.printStackTrace();
				} // try
				return null;
			} else if (!(_value instanceof Number)) {
				return null;
			} else {
				return _value;
			} // if
		}).orElse(null);

		final Class<?> dtype = (Number.class.isAssignableFrom(this.dtype())) //
				? this.dtype() //
				: Types.getSharedSuperClasses(Arrays.asList(this.mydata())).stream() // dtype 不是number类型尝试讯在元素类型
						.filter(Number.class::isAssignableFrom).findFirst() //
						.orElse(Object.class);
		final Number num = (Number) value;

		if (Number.class.isAssignableFrom(dtype)) { // 尝试根据dtype 提取争取元素类型
			if (Integer.class == dtype) {
				return (T) (Integer) num.intValue();
			} else if (Long.class == dtype) {
				return (T) (Long) num.longValue();
			} else if (Double.class == dtype) {
				return (T) (Double) num.doubleValue();
			} else if (Float.class == dtype) {
				return (T) (Float) num.floatValue();
			} else if (Short.class == dtype) {
				return (T) (Short) num.shortValue();
			} else if (Byte.class == dtype) {
				return (T) (Byte) num.byteValue();
			} else { // Number 类型
				return (T) num;
			}
		} else {
			return (T) num;
		} // if
	}

	/**
	 * 数据信息排序
	 * <p>
	 * 使用T的自带比较方法进行排序(升序）
	 * 
	 * @param <K>   排序键类型
	 * @param keyer t->k 排序键转换器,排序算法
	 * @return 排序后的数据
	 */
	default <K extends Comparable<K>> INdarray<T> sortBy(final Function<T, K> keyer) {
		return this.sortBy(keyer, true);
	}

	/**
	 * 数据信息排序
	 * <p>
	 * 使用T的自带比较方法进行排序
	 * 
	 * @param <K>   排序键类型
	 * @param keyer t->k 排序键转换器
	 * @param flag  排序标记,true:升序,false:降序
	 * @return 排序后的数据
	 */
	default <K extends Comparable<K>> INdarray<T> sortBy(final Function<T, K> keyer, final boolean flag) {
		return flag ? this.sorted((a, b) -> keyer.apply(a).compareTo(keyer.apply(b)))
				: this.sorted((a, b) -> keyer.apply(b).compareTo(keyer.apply(a)));
	}

	/**
	 * 数据信息排序
	 * <p>
	 * 使用T的自带比较方法进行排序
	 *
	 * @return 排序后的数据
	 */
	@SuppressWarnings("unchecked")
	default INdarray<T> sorted() {
		return this.sorted((a, b) -> ((Comparable<T>) a).compareTo(b));
	}

	/**
	 * 数据信息排序
	 *
	 * @param comparator Compares its two arguments for order.
	 * @return 排序后的数据
	 */
	default INdarray<T> sorted(final Comparator<T> comparator) {
		final int n = this.length();
		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 1; j < n; j++) {
				if (comparator.compare(this.get(i), this.get(j)) > 0) { // 前面的大于后面的，掉转
					final T t = this.get(i);
					this.set(i, this.get(j));
					this.set(j, t);
				} // if
			} // for
		} // for
		return this;
	}

	/**
	 *
	 */
	@Override
	default int compareTo(final INdarray<T> o) {
		return INdarray.compareTo(this.data(), o.data());
	}

	/**
	 * 生成遍历器
	 *
	 * @param start 开始位置
	 * @param end   结束位置
	 * @return iterator
	 */
	default Iterator<T> iterator(final int start, final int end) {

		return new Iterator<T>() {
			private int i = start;

			@Override
			public boolean hasNext() {
				return i < end;
			}

			@Override
			public T next() {
				return data()[i++];
			}
		};
	}

	/**
	 * 迭代器
	 */
	@Override
	default Iterator<T> iterator() {
		return this.iterator(this.start(), this.end());
	}

	/**
	 * 数组应用
	 *
	 * @param <U>    结果类型
	 * @param mapper 结果变换类型 [t]->u, [t] 为 复制数组
	 * @return U类型的结果
	 */
	default <U> U arrayOf(final Function<T[], U> mapper) {
		final T[] ts = Arrays.copyOfRange(this.data(), this.start(), this.end());
		return mapper.apply(ts);
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 * <p>
	 *
	 * @param <U>    映射结果
	 * @param mapper 节点映射函数 trienode->u
	 * @return 生成TrieTree,根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default <U> Stream<U> trieNodeS(final Function<TrieNode<Integer>, U> mapper) {
		return this.trieTree().nodeS(mapper);
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 * <p>
	 *
	 * @param <U>    映射结果
	 * @param mapper 节点映射函数 trienode->u
	 * @return 生成TrieTree,根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default <U> INdarray<U> trieNodes(final Function<TrieNode<Integer>, U> mapper) {
		return this.trieNodeS(mapper).collect(ndclc());
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 * <p>
	 *
	 * @return 生成TrieTree,根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default INdarray<INdarray<T>> trieNodes() {
		return this.trieNodes(e -> e.attrval(Types.cast((INdarray<T>) null)));
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 * <p>
	 *
	 * @return 生成TrieTree,根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default TrieNode<Integer> trieTree() {
		return this.trieTree((LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>>) null);
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 *
	 * @param strides 分区索引跨度 [(index,(start,end))]
	 * @return 生成TrieTree, 根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default TrieNode<Integer> trieTree(LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		final Collector<? super Tuple2<List<Integer>, INdarray<T>>, ?, TrieNode<Integer>> collector = Partitioner
				.trieclc();
		return Optional.ofNullable(strides) //
				.orElse(Optional.ofNullable(this.strides()).orElse(new LinkedHashMap<>())) //
				.entrySet().stream() //
				.map(e -> Tuple2.of(e.getKey(), e.getValue())) // index->(start,end)
				.map(e -> e.fmap2(Tuple2.bifun(this::create))) // index->ndarray
				.collect(collector); // trienode
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 *
	 * @param partitioner 分区器
	 * @return 生成TrieTree, 根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default TrieNode<Integer> trieTree(final Partitioner partitioner) {
		return partitioner.trieTree(this::create);
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 *
	 * @param xs vs 分区序列,1,P(2,3),...
	 * @return 生成TrieTree, 根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default TrieNode<Integer> trieTree(final Object... xs) {
		return this.trieTree(Partitioner.P(xs));
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 *
	 * @param xs 元素路径 分区序列 分区定义，key1,value1,key2,value2,...
	 * @return 生成TrieTree, 根节点为null, (多维键名单值-键值对儿) 列表 即 [([int],ndarray]) 亦即
	 *         TrieNode&lt;Integer&gt; <br>
	 *         分区结果作为 TrieNode (多维键名单值-键值对儿) 的属性 attrs 给予存放,结构示例如下:<br>
	 *         {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} <br>
	 *         value:数据分区ndarray,index:分区索引Interger[],level:u所在层级:从0开始,isleaf:
	 *         是否是叶子节点。
	 */
	default TrieNode<Integer> trieTree2(final Object... xs) {
		return this.trieTree(Partitioner.P2(xs));
	}

	/**
	 * 划分成结构体
	 *
	 * @param p 结构划分
	 * @return key->ndarray
	 */
	default Getter<String, INdarray<T>> struct(final Partitioner p) {
		return key -> with_key(p, this).apply(key).orElse(null);
	}

	/**
	 * 划分成结构体
	 *
	 * @param kvs 划分的键名，键值列表
	 * @return key->ndarray
	 */
	default Getter<String, INdarray<T>> struct(final Object... kvs) {
		return this.struct(Partitioner.P2(kvs));
	}

	/**
	 * 数组应用
	 * 
	 * @param <TARGET> 目标结果类型
	 * @param mapper   结果变换类型 this->u, this 为当前ISeq对象
	 * @return U类型的结果
	 */
	@SuppressWarnings("unchecked")
	default <SELF extends INdarray<T>, TARGET> TARGET mutate(final Function<SELF, TARGET> mapper) {
		return mapper.apply((SELF) this);
	}

	/**
	 * 数组应用 暴露源数据
	 * 
	 * @param <TARGET> 目标结果类型
	 * @param mapper   结果变换类型 this->u, this 为当前ISeq对象
	 * @return U类型的结果
	 */
	default <TARGET> TARGET dataOf(final Function<T[], TARGET> mapper) {
		return mapper.apply(this.data());
	}

	/**
	 * 格式化输出
	 *
	 * @param fmt 格式化输出
	 * @return 格式化输出
	 */
	default String toString(final Function<T, String> fmt) {
		return String.format("[ %s ]", this.stream().map(fmt).collect(Collectors.joining(", ")));
	}

	/**
	 * 取模格式化: <br>
	 * final var data = INdarray.nd(i -> i, 32); <br>
	 * data.toString(16,8,4) 表示第一级单行16个元素,第二级8个元素,第三级4个㢝 <br>
	 *
	 * @param mods 模长度,行长度, 16,8,4
	 * @return 取模格式化 一般用于矩阵类数据结构
	 */
	default String toString(final int... mods) {
		return this.mutate(INdarray.modfmt(mods));
	}

	/**
	 * 以mod为提取周期,生成数据提取器,提取第i个mod周期开始后的size长度的数据。<br>
	 * 可视为动态的行集合。以时间表示空间的思想
	 *
	 * @param mod  取模长度
	 * @param size 提取数据的长度
	 * @return i:提取周期，从0开始、行号索引 -> 提取第i个mod周期开始后的size长度的数据。(row)
	 */
	default Getter<Integer, INdarray<T>> modgets(final int mod, final int size) {
		final INdarray<INdarray<T>> rows = this.subset(is_modrem(mod, 0), size);
		return i -> rows.get(i);
	}

	/**
	 * 提取数据行<br>
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定行方法，可视为动态的行集合。以时间表示空间的思想
	 *
	 * @param mod 行长度
	 * @return 行数据流
	 */
	default Stream<INdarray<T>> rowS(final int mod) {
		return this.subsetS(is_modrem(mod, 0), mod);
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定行方法，可视为动态的行集合。以时间表示空间的思想
	 *
	 * @param mod 取模长度
	 * @return (i:行号索引,从0开始i)->row
	 */
	default Getter<Integer, INdarray<T>> rows(final int mod) {
		return this.modgets(mod, mod);
	}

	/**
	 * 提取数据列<br>
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法，可视为动态的列集合。以时间表示空间的思想
	 *
	 * @param mod 行长度
	 * @return 列数据流
	 */
	default Stream<INdarray<IPoint<T>>> colS(final int mod) {
		final int n = this.length();
		return Stream.iterate(0, i -> i + 1).limit(mod) //
				.map(i -> Stream.iterate(0, j -> j + i < n, j -> j + mod)//
						.map(j -> j + i).map(this::point).collect(ndclc()));
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法:可视为动态的列集合。以时间表示空间的思想 <br>
	 * INdarray&ltIPoint&ltT&gt;&gt; 把不连续的列数据IPoint&lt;T&gt;构造曾INdarray的连续区间。<br>
	 * 也就是说外层的INdarray此时相当于一个 算法结果的缓存:<br>
	 * i->i*mod,并将 [0*mod,1*mod,2*mod,...,] <br>
	 * 以缓存的方式,保存起来,于是 动态的算法这里给予静态的数据结构化了，<br>
	 * 这就是时空变换的本质。缓存是时空变换的关键结构.<br>
	 * 表示缓存的结构, 就是算法空间结构。<br>
	 *
	 * @param mod 取模长度,行长度
	 * @return i:列号索引，从0开始->col，返回列cell引用向量，每个cell为一个长度为1的ndarray,即entry et
	 */
	default Getter<Integer, INdarray<IPoint<T>>> cols(final int mod) {
		return i -> this.subset(is_modrem(mod, i));
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法，可视为动态的列集合。以时间表示空间的思想
	 *
	 * @param mod 取模长度，行长度
	 * @return i:列号索引，从0开始->col,返回列数据向量
	 */
	default Getter<Integer, INdarray<T>> cols2(final int mod) {
		int n = this.length() / mod;
		return i -> this.subsetS(is_modrem(mod, i)).map(e -> e.get()).collect(INdarray.ndclc(n));
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定行方法
	 *
	 * @param mod 取模长度
	 * @param i   行号索引从0开始
	 * @return i:行号索引，从0开始->row
	 */
	default INdarray<T> row(final int mod, final int i) {
		return this.rows(mod).get(i);
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法
	 *
	 * @param mod 取模长度
	 * @param i   列号索引从0开始
	 * @return 返回列cell引用向量，每个cell为一个长度为1的ndarray,即entry et，引用原始data数据
	 */
	default INdarray<IPoint<T>> col(final int mod, final int i) {
		return this.cols(mod).get(i);
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法
	 *
	 * @param mod 取模长度
	 * @param i   列号索引从0开始
	 * @return 返回列数据向量,不再引用原始data数据
	 */
	default INdarray<T> col2(final int mod, final int i) {
		return this.cols2(mod).get(i);
	}

	/**
	 * 按照树形结构的方式进行数据遍历
	 *
	 * @param <U>          子节点类型
	 * @param get_children 求取子节点方法
	 * @return 子节点的数据流 [(path,nd)]
	 */
	default <U extends Iterable<INdarray<T>>> Stream<Tuple2<INdarray<Integer>, INdarray<T>>> traverseS(
			final Function<INdarray<T>, U> get_children) {

		final Tuple2<INdarray<Integer>, INdarray<T>> root = Tuple2.of(INdarray.from(0), this); // 根节点
		final Stack<Tuple2<INdarray<Integer>, INdarray<T>>> stack = new Stack<>();
		stack.push(root);

		return Stream.iterate(root, e -> !stack.empty(), e -> {
			final Tuple2<INdarray<Integer>, INdarray<T>> p = stack.pop();

			if (p._2 != null) { // 尚未结束
				final Iterable<INdarray<T>> children = get_children.apply(p._2);
				final INdarray<Integer> path = p._1;
				if (children != null) { // 子节点有效
					Types.itr2stream(children).map(Tuple2.snb(0)).collect(Lisp.llclc(true)).forEach(tup -> {
						stack.push(Tuple2.of(path.concat(tup._1), tup._2));
					}); // forEach
				} // if
				if (stack.isEmpty()) {// 压入结束标记
					stack.push(Tuple2.of(null, null)); // 空值标记位置
				} // if
				return p;
			} else { // 遭遇空值代表结束了
				return e;
			} // if

		}).skip(1);
	}

	/**
	 * 按照树形结构的方式进行数据遍历
	 *
	 * @param <U>          子节点类型
	 * @param get_children 求取子节点方法
	 * @return 子节点的nd [(path,nd)]
	 */
	default <U extends Iterable<INdarray<T>>> INdarray<Tuple2<INdarray<Integer>, INdarray<T>>> traverses(
			final Function<INdarray<T>, U> get_children) {
		return this.traverseS(get_children).collect(ndclc());
	}

	/**
	 * 连接两个 ndarray <br>
	 * 将可视区间中的数据进行连接 [this.mydata,nd.mydata]
	 *
	 * @param nd 另一个ndarray
	 * @return 连接以后的ndarray
	 */
	default INdarray<T> concat(final Iterable<T> nd) {
		return INdarray.of(INdarray.concat(this.mydata(), INdarray.of(nd).mydata()));
	}

	/**
	 * 连接两个 ndarray <br>
	 * 将可视区间中的数据进行连接 [this.mydata,nd.mydata]
	 *
	 * @param ts 另一个ndarray
	 * @return 连接以后的ndarray,新生成的nd对象
	 */
	@SuppressWarnings("unchecked")
	default INdarray<T> concat(final T... ts) {
		return INdarray.of(INdarray.concat(this.mydata(), ts));
	}

	/**
	 * 把us累计的添加到当前向量中
	 *
	 * @param <U>   另一个向量序列
	 * @param <NU>  另一个向量序列
	 * @param tuacc 累加函数 tu accumulaor
	 * @param nus   另一个向量序列序列
	 * @return ndarray 保持this形状的累加值
	 */
	default <U, NU extends INdarray<U>> INdarray<T> accum(final BiFunction<T, U, T> tuacc, final List<NU> nus) {
		final INdarray<T> nt = this.dupdata();
		final int tlen = nt.length();

		for (final NU nu : nus) {
			final int ulen = nu.length();
			for (int i = 0; i < tlen; i++) {
				final T t = nt.get(i);
				final U u = nu.get(i % ulen);
				final T _t = tuacc.apply(t, u);
				nt.set(i, _t);
			} // i
		} // nu

		return nt;
	}

	/**
	 * 把us累计的添加到当前向量中
	 *
	 * @param <U>   另一个向量序列
	 * @param <NU>  另一个向量序列类型
	 * @param tuacc 累加函数 tu accumulaor
	 * @param nu    另一个向量
	 * @return ndarray ndarray 保持this形状的累加值
	 */
	default <U, NU extends INdarray<U>> INdarray<T> accum(final BiFunction<T, U, T> tuacc, final NU nu) {
		return this.accum(tuacc, Arrays.asList(nu));
	}

	/***********************************************************
	 * 简单的线性代数函数
	 ***********************************************************/

	/**
	 * 把us累计的添加到当前向量中：加法
	 *
	 * @param <U>  另一个向量序列
	 * @param <NU> 另一个向量序列
	 * @param nus  另一个向量序列序列
	 * @return ndarray 保持this形状的累加值
	 */
	default <U, NU extends INdarray<U>> INdarray<Double> addacc(final List<NU> nus) {
		final List<INdarray<Double>> nds = nus.stream().map(INdarray::dbls) //
				.collect(Collectors.toList());
		return this.dbls() //
				.accum((a, b) -> a + b, nds);
	}

	/**
	 * 把us累计的添加到当前向量中：减法
	 *
	 * @param <U>  另一个向量序列
	 * @param <NU> 另一个向量序列
	 * @param nus  另一个向量序列序列
	 * @return ndarray 保持this形状的累加值
	 */
	default <U, NU extends INdarray<U>> INdarray<Double> subacc(final List<NU> nus) {
		final List<INdarray<Double>> nds = nus.stream().map(INdarray::dbls) //
				.collect(Collectors.toList());
		return this.dbls() //
				.accum((a, b) -> a - b, nds);
	}

	/**
	 * 把us累计的添加到当前向量中：乘法
	 *
	 * @param <U>  另一个向量序列
	 * @param <NU> 另一个向量序列
	 * @param nus  另一个向量序列序列
	 * @return ndarray 保持this形状的累加值
	 */
	default <U, NU extends INdarray<U>> INdarray<Double> mulacc(final List<NU> nus) {
		final List<INdarray<Double>> nds = nus.stream().map(INdarray::dbls) //
				.collect(Collectors.toList());
		return this.dbls() //
				.accum((a, b) -> a * b, nds);
	}

	/**
	 * 把us累计的添加到当前向量中：除法
	 *
	 * @param <U>  另一个向量序列
	 * @param <NU> 另一个向量序列
	 * @param nus  另一个向量序列序列
	 * @return ndarray 保持this形状的累加值
	 */
	default <U, NU extends INdarray<U>> INdarray<Double> divacc(final List<NU> nus) {
		final List<INdarray<Double>> nds = nus.stream().map(INdarray::dbls) //
				.collect(Collectors.toList());
		return this.dbls() //
				.accum((a, b) -> a / b, nds);
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法
	 *
	 * @param <U> 另一个向量类型
	 * @param nd  另一个ndarray
	 * @return i:列号索引，从0开始->col,返回列数据向量
	 */
	default <U> T dot(final INdarray<U> nd) {
		return this.asNum(INdarray.dot(this.dbls(), nd.dbls()));
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法
	 *
	 * @param nd 另一个ndarray
	 * @return i:列号索引，从0开始->col,返回列数据向量
	 */
	default Number dotNum(final INdarray<T> nd) {
		return this.dotDbl(nd);
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法
	 *
	 * @param nd 另一个ndarray
	 * @return i:列号索引，从0开始->col,返回列数据向量
	 */
	default double dotDbl(final INdarray<T> nd) {
		return INdarray.dot(this.dbls(), nd.dbls());
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定列方法
	 *
	 * @param nd 另一个ndarray
	 * @return i:列号索引，从0开始->col,返回列数据向量
	 */
	default int dotInt(final INdarray<T> nd) {
		return this.dotNum(nd).intValue();
	}

	/**
	 * 矩阵乘法 : this: m x mod , us: mod x n 生成 m x n 矩阵
	 *
	 * @param <U> 矩阵元素
	 * @param mod 当前矩阵的列数
	 * @param nu  另一个矩阵: mod x n
	 * @return ndarray
	 */
	default <U> INdarray<Double> mmult(final int mod, INdarray<U> nu) {
		final int nrow = this.length() / mod;
		final int ncol = nu.length() / mod;
		final Double[] ts = this.dbls().data();
		final double[] us = Optional.of(nu.dbls().data())
				.map(udata -> IntStream.iterate(0, i -> i < udata.length, i -> i + 1)
						.mapToDouble(i -> udata[(i % ncol) * mod + i / ncol]).toArray())
				.get();
		final Double[] data = new Double[nrow * ncol]; // 结果数据
		final Function<IntStream, IntStream> parallelize = s -> data.length > 1000 ? s.parallel() : s;

		Optional.of(IntStream.iterate(0, i -> i < ts.length, i -> i + mod)).map(parallelize).get().forEach(i -> {
			final int offset = i / mod * ncol;
			Optional.of(IntStream.iterate(0, j -> j < us.length, j -> j + mod)).map(parallelize).get().forEach(j -> {
				data[offset + j / mod] = IntStream.iterate(0, k -> k < mod, k -> k + 1) //
						.mapToDouble(k -> ts[i + k] * us[j + k]).reduce(0d, Double::sum); //
			}); // j
		}); // i

		return INdarray.nd(data);
	}

	/**
	 * 依据取模长度进行维度生成
	 *
	 * nats(10).shape(5): 返回 [2,5] 表示行数与列数。
	 *
	 * @param mods 模蓄力,8,4,2这样的递减序列，每个维度代表一阶长度。 <br>
	 *             对于一个length为32的nd,shape(8,2) 返回,2,2,8;
	 *             表示这是三维矩阵地一个维度：有两个元素,每个元素16个元素,第二个维度，有两个维度每个维度8个元素。
	 * @return dims 维度信息:
	 */
	default int[] shape(final int... mods) {
		final INdarray<Integer> ndims = nd(mods);
		final int d = this.length() / ndims.product();
		return ndims.concat(d).mutate((INdarray<Integer> e) -> Optional.ofNullable(e) //
				.map(INdarray::reverse).map(INdarray::data).map(Types.Ints2ints)).orElse(null);
	}

	/**
	 * 变量的模，2-范数，Euclidean Distance
	 *
	 * @return 向量的长度
	 */
	default double norm() {
		return Math.sqrt(this.dotDbl(this));
	}

	/**
	 * 向量单位化
	 *
	 * @return 单位话向量
	 */
	default INdarray<Double> normalize() {
		return this.dbls().accum((a, b) -> a / b, from(this.norm()));
	}

	/**
	 * 当前向量在另一个向量us方向上的投影(us被标准化)
	 *
	 * @param <U> 向量元素类型
	 * @param us  另一个向量
	 * @return 投影长度
	 */
	default <U> double proj(final INdarray<U> us) {
		return dot(this.dbls(), us.dbls()) / us.norm();
	}

	/**
	 * 向量算术运算-加法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param num 向量与整数求和
	 * @return 投影长度
	 */
	default <U> INdarray<Double> add(final Number num) {
		return this.add(point(num.doubleValue()));
	}

	/**
	 * 向量算术运算-加法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param nu  另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> add(final INdarray<U> nu) {
		final List<INdarray<Double>> pts = asList(nu.dbls());
		return this.dbls().accum((a, b) -> a + b, pts);
	}

	/**
	 * 向量算术运算-减法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param num 另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> sub(final Number num) {
		return this.sub(point(num));
	}

	/**
	 * 向量算术运算-减法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param nu  另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> sub(final INdarray<U> nu) {
		final List<INdarray<Double>> pts = asList(nu.dbls());
		return this.dbls().accum((a, b) -> a - b, pts);
	}

	/**
	 * 向量算术运算-乘法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param num 另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> mul(final Number num) {
		return this.mul(point(num.doubleValue()));
	}

	/**
	 * 向量算术运算-乘法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param nu  另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> mul(final INdarray<U> nu) {
		final List<INdarray<Double>> pts = asList(nu.dbls());
		return this.dbls().accum((a, b) -> a * b, pts);
	}

	/**
	 * 向量算术运算-除法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param num 另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> div(final Number num) {
		return this.div(point(num));
	}

	/**
	 * 向量算术运算-除法 <br>
	 *
	 * @param <U> 向量元素类型
	 * @param nu  另一个向量
	 * @return 投影长度
	 */
	default <U> INdarray<Double> div(final INdarray<U> nu) {
		final List<INdarray<Double>> pts = asList(nu.dbls());
		return this.dbls().accum((a, b) -> a / b, pts);
	}

	/**
	 * 转换成 nx 对象
	 *
	 * @param kvs nx 元数据的 key,value 序列,比如: mod,1,表示构建1维列向量
	 * @return nx 对象
	 */
	default INdarrayX<T> nx(final Object... kvs) {
		return this.withNX(e -> e, kvs);
	}

	/**
	 * 转换成 nx 对象
	 *
	 * @param mod 指定mod行宽列数量,大于等于1的正整数。
	 * @return nx 对象
	 */
	default INdarrayX<T> nx(final int mod) {
		return this.nx(MetaKey.mod, mod);
	}

	/**
	 * MatriX Session(nxfun) 会话操作
	 *
	 * @param <U>   结果类型
	 * @param nxfun 矩阵函数操作类 nx->u
	 * @param kvs   nxfun 的各种属性,比如 mod 取模长度等。
	 * @return nxfun 的返回值
	 */
	default <U> U withNX(final Function<INdarrayX<T>, U> nxfun, Object... kvs) {
		final INdarray<T> self = this; // 定义别名，方便NdarrayX引用
		// 私有类 矩阵化扩展的 Ndarray 的 实现：通过增加元数据metas来扩展,增加mod引入矩阵列数(行长度)来完成矩阵格式定义
		class NdarrayX extends AbstractNdarray<T> implements INdarrayX<T> {
			// 构造函数
			public NdarrayX() {
				super(self.data(), Math.min(self.start(), self.datalen()), Math.min(self.end(), self.datalen()),
						self.strides());
				if (Objects.nonNull(kvs) && kvs.length > 0) { // 设置属性
					for (int i = 0; i < kvs.length - 1; i += 2) {
						metas.put(kvs[i] + "", kvs[i + 1]);
					} // for
				} // if
			} // NdarrayX

			@Override
			public String toString() {
				return INdarray.this.toString(mod());
			}

			@Override
			public LinkedHashMap<String, Object> metas() {
				return metas;
			}

			private LinkedHashMap<String, Object> metas = new LinkedHashMap<>();
		}

		return nxfun.apply(new NdarrayX());
	}

	/**
	 * 把数组ts1 和 ts2 连接到一起
	 *
	 * @param <T>
	 * @param ts1 数组1
	 * @param ts2 数组2
	 * @return 连接后的数组
	 */
	static <T> T[] concat(final T[] ts1, final T[] ts2) {
		final T[] ts = Arrays.copyOf(ts1, ts1.length + ts2.length);
		System.arraycopy(ts2, 0, ts, ts1.length, ts2.length);
		return ts;
	}

	/**
	 * 比较两个数组 <br>
	 * <p>
	 * Compares this object with the specified object for order. Returns anegative
	 * integer, zero, or a positive integer as this object is lessthan, equal to, or
	 * greater than the specified object.
	 * <p>
	 * The implementor must ensure sgn(x.compareTo(y)) ==-sgn(y.compareTo(x)) for
	 * all x and y. (Thisimplies that x.compareTo(y) must throw an exception iff
	 * y.compareTo(x) throws an exception.)
	 * <p>
	 * The implementor must also ensure that the relation is transitive:
	 * (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
	 * <p>
	 * Finally, the implementor must ensure that x.compareTo(y)==0implies that
	 * sgn(x.compareTo(z)) == sgn(y.compareTo(z)), forall z.
	 * <p>
	 * It is strongly recommended, but not strictly required that
	 * (x.compareTo(y)==0) == (x.equals(y)). Generally speaking, anyclass that
	 * implements the Comparable interface and violatesthis condition should clearly
	 * indicate this fact. The recommendedlanguage is "Note: this class has a
	 * natural ordering that isinconsistent with equals."
	 * <p>
	 * In the foregoing description, the notation sgn(expression) designates the
	 * mathematical signum function, which is defined to return one of -1, 0, or 1
	 * according to whether the value of expression is negative, zero or positive.
	 * <p>
	 * Specified by: compareTo(...) in Comparable Parameters:o the object to be
	 * compared.Returns:a negative integer, zero, or a positive integer as this
	 * objectis less than, equal to, or greater than the specified object.
	 *
	 * @param <T> 第一数组元素类型
	 * @param <U> 第二数组元素类型
	 * @param ts  T类型的数组
	 * @param us  U类型的数组
	 * @return a negative integer, zero, or a positive integer as this objectis less
	 *         than, equal to, or greater than the specified object.
	 */
	static <T, U> int compareTo(final T[] ts, final U[] us) {
		if (ts == us) {
			return 0;
		} else {
			final int n = Math.max(ts.length, us.length);
			@SuppressWarnings("unchecked")
			final Optional<Integer> optional = Stream.iterate(0, i -> i + 1).limit(n).map(i -> {
				final T t1 = i < ts.length ? ts[i] : null;
				final U t2 = i < us.length ? us[i] : null;
				int flag = 1; // 默认值类型不匹配的情况
				try {
					flag = (int) Optional.ofNullable(t1) //
							.map(_t1 -> Optional.ofNullable(t2) //
									.map(_t2 -> ((Comparable<Object>) _t1).compareTo(_t2)).orElse(1))
							.orElse(t1 == t2 ? 0 : -1); // t1 为 null,
				} catch (Exception e) {
					e.printStackTrace();
				} // try
				return flag; // 默认值类型不匹配的情况
			}).filter(e -> e != 0).findFirst();

			return optional.orElse(0);
		} // if
	}

	/**
	 * 参数列表可以被视为一个元组
	 *
	 * @param <T> 元素类型
	 * @param p   分区器
	 * @param nd  多维数组
	 * @return 键名访问器 key->nd
	 */
	static <T> Getter<String, Optional<INdarray<T>>> with_key(final Partitioner p, final INdarray<T> nd) {
		return key -> p.evaluate(key, nd::create);
	}

	/**
	 * 参数列表可以被视为一个元组
	 *
	 * @param <T> 元素类型
	 * @param p   分区器
	 * @param nd  多维数组
	 * @return 索引访问器 index->nd
	 */
	static <T> Getter<Integer[], Optional<INdarray<T>>> with_index(final Partitioner p, final INdarray<T> nd) {
		return key -> p.evaluate(key, nd::create);
	}

	/**
	 * 拉链函数
	 *
	 * @param <T> 第一元素类型
	 * @param <U> 第二元素类型
	 * @param a   T 类型的ndarray
	 * @param b   U 类型的ndarray
	 * @return 拉链以后二元组元素流 [(t,u)]
	 */
	static <T, U> Stream<Tuple2<T, U>> zipS(final INdarray<T> a, final INdarray<U> b) {
		final int alen = a.length();
		final int blen = b.length();
		final int n = Math.max(alen, blen);
		if (n == 0) {
			return Stream.empty();
		}
		return Stream.iterate(0, i -> i < n, i -> i + 1) //
				.map(i -> Tuple2.of( // 数据元组
						alen == 0 ? null : a.get(i % alen), //
						blen == 0 ? null : b.get(i % blen) //
				)); // map
	}

	/**
	 * 拉链函数
	 *
	 * @param <T> 第一元素类型
	 * @param <U> 第二元素类型
	 * @param a   T 类型的ndarray
	 * @param b   U 类型的ndarray
	 * @return 拉链以后二元组元素流 [(t,u)]
	 */
	@SuppressWarnings("unchecked")
	static <T, U> INdarray<Tuple2<T, U>> zip(final INdarray<T> a, final INdarray<U> b) {
		return INdarray.of(INdarray.zipS(a, b).toArray(Tuple2[]::new));
	}

	/**
	 * ND BiFunctioN <br>
	 * 把 两个 ts 和 us 进行按位置的合并 生成一个 vs
	 *
	 * @param <T> 第一向量元素类型
	 * @param <U> 第二向量元素类型
	 * @param <V> 结果向量元素类型
	 * @param f   变换函数 (t,u)->v
	 * @return (ts,us)->vs
	 */
	static <T, U, V> BiFunction<INdarray<T>, INdarray<U>, INdarray<V>> ndbfn(final BiFunction<T, U, V> f) {
		return (ts, us) -> INdarray.zipS(ts, us).map(p -> f.apply(p._1, p._2)).collect(ndclc());
	}

	/**
	 * ND BinaryOPerator <br>
	 * 把 两个 ts 和 us 进行按位置的合并 生成一个 vs
	 *
	 * @param <T> 第一向量元素类型
	 * @param f   变换函数 (t,u)->v
	 * @return (ts,us)->vs
	 */
	static <T> BinaryOperator<INdarray<T>> ndbop(final BiFunction<T, T, T> f) {
		return (t1, t2) -> ndbfn(f).apply(t1, t2);
	}

	/**
	 * 点积:当 a,b 有一个为null或是长度为0的时候,返回-0d
	 *
	 * @param <T> 第一元素类型
	 * @param <U> 第二元素类型
	 * @param a   T 类型的ndarray
	 * @param b   U 类型的ndarray
	 * @return 拉链以后二元组元素流 [(t,u)],注意长度为0的向量的点积为0
	 */
	static <T extends Number, U extends Number> Double dot(final INdarray<T> a, final INdarray<U> b) {
		if (a == null || b == null || a.length() < 1 || b.length() < 1) {
			return -0d;
		}

		final int alen = a.length();
		final int blen = b.length();
		final int n = Math.max(alen, blen);

		return Stream.iterate(0, i -> i + 1).limit(n) //
				.map(i -> Optional.ofNullable(a.get(i % alen)) //
						.map(a1 -> Optional.ofNullable(b.get(i % blen)) //
								.map(b1 -> b1.doubleValue() * a1.doubleValue()) //
								.orElse(null))
						.orElse(null))
				.filter(Objects::nonNull) //
				.collect(Collectors.summingDouble(e -> e));
	}

	/**
	 * 逆序数 <br>
	 * 在一个排列中，如果一对数的前后位置与大小顺序相反，即前面的数大于后面的数，那么它们就称为一个逆序。 一个排列中逆序的总数就称为这个排列的逆序数
	 * 简单的来说逆序数就是前面的数比这个数大，有几个比他大的数逆序数就是几。
	 * 
	 * @param <E>  可以比较的元素类型
	 * @param <ND> 元素课比较向量
	 * @param nd   元素课比较向量
	 * @return 逆序数
	 */
	static <E extends Comparable<E>, ND extends INdarray<E>> int tau(final ND nd) {
		if (nd.length() <= 1) { // 序列长度小于等于1，逆序数为0
			return 0;
		} else { // 序列长度大于等于2进行分分解&递归求解
			final E h = nd.head();
			final INdarray<E> tail = nd.tail();
			final int _tau = tail.stream().map(e -> e.compareTo(h) == -1 ? 1 : 0).reduce(0, Integer::sum);
			return tau(tail) + _tau;
		} // if
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param <T>  元素类型
	 * @param data 源数据
	 */
	@SafeVarargs
	static <T> INdarray<T> from(final T... data) {
		return INdarray.of(data, 0, data.length, null);
	}

	/**
	 * 创建多维数组 ( 基础函数 ) *** 基础函数 ***
	 *
	 * @param <T>     元素类型
	 * @param data    元数据
	 * @param start   data的绝对索引,开始位置索引,从0开始
	 * @param end     data的绝对索引,结束位置索引 exclusive
	 * @param strides 分区索引跨度
	 * @return ndarray
	 */
	static <T> INdarray<T> of(final T[] data, final int start, final int end,
			final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		return new StandardNdarray<>(data, start, end, strides);
	}

	/**
	 * 创建多维数组
	 *
	 * @param <T>   元素类型
	 * @param data  元数据
	 * @param start data的绝对索引,开始位置索引,从0开始
	 * @param end   data的绝对索引,结束位置索引 exclusive
	 * @return ndarray
	 */
	static <T> INdarray<T> of(final T[] data, final int start, final int end) {
		return INdarray.of(data, start, end, null);
	}

	/**
	 * 创建多维数组
	 *
	 * @param <T>   元素类型
	 * @param data  元数据
	 * @param start data的绝对索引,开始位置索引,从0开始
	 * @return ndarray
	 */
	static <T> INdarray<T> of(final T[] data, final int start) {
		return INdarray.of(data, start, data.length);
	}

	/**
	 * 创建多维数组
	 *
	 * @param <T> 元素类型
	 * @param gen i->t
	 * @param n   数据维度
	 * @return ndarray
	 */
	static <T> INdarray<T> of(Function<Integer, T> gen, final int n) {
		return Stream.iterate(0, i -> i + 1).limit(n).map(gen).collect(INdarray.ndclc(n));
	}

	/**
	 * 创建多维数组
	 *
	 * @param <T>  元素类型
	 * @param data 元数据
	 * @return ndarray
	 */
	static <T> INdarray<T> of(final T[] data) {
		return INdarray.of(data, 0);
	}

	/**
	 * 创建多维数组: INdarray.of的别名
	 *
	 * @param <T>  元素类型
	 * @param data 元数据
	 * @return ndarray
	 */
	static <T> INdarray<T> of(final Iterable<T> data) {
		if (data instanceof INdarray) {
			return ((INdarray<T>) data).duplicate();
		}
		return INdarray.of(Types.itr2array(data));
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Character> nd(final char... data) {
		return INdarray.of(Types.chars2Chars.apply(data), 0, data.length, null);
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Byte> nd(final byte... data) {
		return INdarray.of(Types.bytes2Bytes.apply(data), 0, data.length, null);
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Integer> nd(final int... data) {
		return INdarray.of(Types.ints2Ints.apply(data), 0, data.length, null);
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Short> nd(final short... data) {
		return INdarray.of(Types.shorts2Shorts.apply(data), 0, data.length, null);
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Long> nd(final long... data) {
		return INdarray.of(Types.lngs2Lngs.apply(data), 0, data.length, null);
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Float> nd(final float... data) {
		return INdarray.of(Types.floats2Floats.apply(data), 0, data.length, null);
	}

	/**
	 * INdarray 生成函数
	 *
	 * @param data 源数据
	 */
	static INdarray<Double> nd(final double... data) {
		return INdarray.of(Types.dbls2Dbls.apply(data), 0, data.length, null);
	}

	/**
	 * 创建多维数组:INdarray.of的别名
	 *
	 * @param <T> 元素类型
	 * @param gen i->t
	 * @param n   数据维度
	 * @return ndarray
	 */
	static <T> INdarray<T> nd(Function<Integer, T> gen, final int n) {
		return INdarray.of(gen, n);
	}

	/**
	 * 创建多维数组:INdarray.of的别名
	 *
	 * @param <T>     元素类型
	 * @param gen     i->t
	 * @param dims_nd 维度向量
	 * @return ndarray
	 */
	static <T> INdarray<T> nd(Function<Integer, T> gen, final INdarray<? extends Number> dims_nd) {
		return INdarray.of(gen, dims_nd.fmap(Number::intValue).product());
	}

	/**
	 * 创建多维数组: INdarray.of的别名
	 *
	 * @param <T>   元素类型
	 * @param data  元数据
	 * @param start data的绝对索引,开始位置索引,从0开始,inclusive
	 * @param end   data的绝对索引,结束位置索引 exclusive
	 * @return ndarray
	 */
	static <T> INdarray<T> nd(final T[] data, final int start, final int end) {
		return INdarray.of(data, start, end);
	}

	/**
	 * 创建多维数组: INdarray.of的别名
	 *
	 * @param <T>   元素类型
	 * @param data  元数据
	 * @param start data的绝对索引,开始位置索引,从0开始
	 * @return ndarray
	 */
	static <T> INdarray<T> nd(final T[] data, final int start) {
		return INdarray.of(data, start);
	}

	/**
	 * 创建多维数组: INdarray.of的别名
	 *
	 * @param <T>  元素类型
	 * @param data 元数据
	 * @return ndarray
	 */
	static <T> INdarray<T> nd(final T[] data) {
		return INdarray.of(data);
	}

	/**
	 * 创建多维数组: INdarray.of的别名
	 *
	 * @param <T>  元素类型
	 * @param data 元数据
	 * @return ndarray
	 */
	static <T> INdarray<T> nd(final Iterable<T> data) {
		return INdarray.of(data);
	}

	/**
	 * 点对象
	 *
	 * @param <T>
	 * @param ts  点值
	 * @return 长度为1的点对象
	 */
	@SafeVarargs
	static <T> IPoint<T> point(final T... ts) {
		return point(ts, 0);
	}

	/**
	 * 点对象:用于表示一个没有内部结构的基础对象
	 *
	 * @param <T>   点类型
	 * @param data  原始数据
	 * @param start 开始位置
	 * @return point
	 */
	static <T> IPoint<T> point(final T[] data, final int start) {
		// 私有类
		class Point extends AbstractNdarray<T> implements IPoint<T> {
			/**
			 * 构造函数
			 *
			 * @param data  开始位置
			 * @param start 起始位置偏移
			 */
			public Point(final T[] data, final int start) {
				super(data, Math.min(start, data.length), Math.min(start + 1, data.length), null);
			}

			/**
			 * 信息格式化
			 */
			public String toString() {
				return "" + this.get();
			}
		}
		return new Point(data, start);
	}

	/**
	 * 点对象:用于表示一个没有内部结构的基础对象
	 *
	 * @param <T> 点类型
	 * @param nd  提取nd的首位元素生成point对象
	 * @return point
	 */
	static <T> IPoint<T> point(final INdarray<T> nd) {
		return (IPoint<T>) point(nd.data(), nd.start());
	}

	/**
	 * 对角阵
	 *
	 * @param <T> 元素类型
	 * @param ts  对角阵的迹
	 * @return 对角阵
	 */
	@SafeVarargs
	static <T> INdarray<T> diag(final T... ts) {
		if (ts.length < 1) {
			return null;
		} else {
			final int n = ts.length;
			@SuppressWarnings("unchecked")
			final T dft = Optional.ofNullable((Class<T>) ts.getClass().getComponentType()) //
					.map(Types::defaultValue).orElse(null);
			final INdarray<T> nd = Stream.iterate(0, i -> i + 1).limit(n * n) // ;
					.map(i -> i % (n + 1) == 0 ? ts[i / (n + 1)] : dft).collect(ndclc());
			return nd;
		}
	}

	/**
	 * point bifunction 映射函数
	 *
	 * @param <T>  第一参数类型
	 * @param <U>  第二参数了ing
	 * @param <V>  函数结果类型
	 * @param bifn 二元函数 (t,u)->v
	 * @return (pt,pu)->pv 的 包装函数
	 */
	static <T, U, V> BiFunction<IPoint<T>, IPoint<U>, IPoint<V>> ptbfn(final BiFunction<T, U, V> bifn) {
		return (pt, pu) -> point(bifn.apply(pt.get(), pu.get()));
	}

	/**
	 * 自然数序列,Natural number
	 *
	 * @param n 序列大小
	 * @return [0,1,...,n-1]
	 */
	static INdarray<Integer> nats(final int n) {
		return nd(Stream.iterate(0, i -> i + 1).limit(n).toArray(Integer[]::new));
	}

	/**
	 * ndarray 归集器
	 *
	 * @param <T>  元素类型
	 * @param size 每次增扩的空间大小
	 * @return INdarray 归集器 [t]->nd
	 */
	static <T> Collector<T, ?, INdarray<T>> ndclc(final int size) {
		return Lisp.aaclc(size, null, INdarray::nd);
	}

	/**
	 * ndarray 归集器
	 *
	 * @param <T>    元素类型
	 * @param size   空间扩增大小
	 * @param tclazz 元素类型
	 * @return INdarray 归集器 [t]->nd
	 */
	static <T> Collector<T, ?, INdarray<T>> ndclc(final int size, final Class<T> tclazz) {
		return Lisp.aaclc(size, tclazz, INdarray::nd);
	}

	/**
	 * ndarray 归集器
	 *
	 * @param <T>    元素类型
	 * @param tclazz 元素类型
	 * @return INdarray 归集器 [t]->nd
	 */
	static <T> Collector<T, ?, INdarray<T>> ndclc(final Class<T> tclazz) {
		return Lisp.aaclc(10, tclazz, INdarray::nd);
	}

	/**
	 * ndarray 归集器
	 *
	 * @param <T> 元素类型
	 * @return INdarray 归集器 [t]->nd
	 */
	static <T> Collector<T, ?, INdarray<T>> ndclc() {
		return Lisp.aaclc(false, INdarray::nd);
	}

	/**
	 * ndndclc nd nd collector
	 * 
	 * @param <T>  元素类型
	 * @param size 扩增长度
	 * @return ndndclc
	 */
	static <T> Collector<INdarray<T>, ?, INdarray<INdarray<T>>> ndndclc(final int size) {
		@SuppressWarnings("unchecked")
		Class<INdarray<T>> ndclazz = (Class<INdarray<T>>) (Object) INdarray.class;
		return INdarray.ndclc(size, ndclazz);
	}

	/**
	 * ndptclc nd point collector
	 * 
	 * @param <T>  元素类型
	 * @param size 扩增长度
	 * @return ndndclc
	 */
	static <T> Collector<IPoint<T>, ?, INdarray<IPoint<T>>> ndptclc(final int size) {
		@SuppressWarnings("unchecked")
		Class<IPoint<T>> ndclazz = (Class<IPoint<T>>) (Object) INdarray.class;
		return INdarray.ndclc(size, ndclazz);
	}

	/**
	 * ndarray 归集器
	 *
	 * @param <T> 元素类型
	 * @param kvs nx 元数据的 key,value 序列,比如: mod,1,表示构建1维列向量
	 * @return INdarray 归集器 [t]->nd
	 */
	static <T> Collector<T, ?, INdarrayX<T>> nxclc(final Object... kvs) {
		return Lisp.aaclc(aa -> {
			return nd(aa).nx(kvs);
		});
	}

	/**
	 * ndarray 归集器
	 *
	 * @param <T> 元素类型
	 * @param mod 指定mod行宽列数量,大于等于1的正整数。
	 * @return INdarray 归集器 [t]->nd
	 */
	static <T> Collector<T, ?, INdarrayX<T>> nxclc(final int mod) {
		return Lisp.aaclc(aa -> {
			return nd(aa).nx(mod);
		});
	}

	/**
	 * 判断函数是否在modulo模下余数为rem
	 *
	 * @param modulo    模数
	 * @param remainder 余数
	 * @return 判断函数是否在modulo模下余数为rem x -> x % modulo == rem;
	 */
	static Predicate<Integer> is_modrem(final Integer modulo, final Integer remainder) {
		return x -> x % modulo == remainder;
	}

	/**
	 * point setter 元素设置函数 <br>
	 * Entry 快捷设置函数，语法糖 <br>
	 *
	 * data.subset(i -> i % 10 == 0).assign(setter(100))
	 *
	 * @param <T>   元素类型
	 * @param <ND>  INdarray 的衍生类
	 * @param value 元素常量值
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> Consumer<ND> pts(final T value) {
		return nd -> nd.set(value);
	}

	/**
	 * point setter 元素设置函数 <br>
	 * Entry 快捷设置函数，语法糖 <br>
	 *
	 * data.subset(i -> i % 10 == 0).assign(setter(t -> t * 3))
	 *
	 * @param <T>  元素类型
	 * @param <ND> INdarray 的衍生类
	 * @param uf   一元变换函数 t->t
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> Consumer<ND> pts(final UnaryOperator<T> uf) {
		return nd -> nd.set(uf.apply(nd.get()));
	}

	/**
	 * entry setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.subset(i -> i % 10 == 0).assign(setter((i, t) -> i * t))
	 *
	 * @param <T>  元素类型
	 * @param <ND> INdarray 的衍生类
	 * @param bf   (i:索引从0开始,t:元素值)->t
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> BiConsumer<Integer, ND> ets(final BiFunction<Integer, T, T> bf) {
		return (i, nd) -> nd.set(bf.apply(i, nd.get()));
	}

	/**
	 * value setter <br>
	 * entry setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.rows(10).get(3).assign(vls(10000));
	 *
	 * @param <T>   元素类型
	 * @param <ND>  INdarray 的衍生类
	 * @param value 常量值
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> TriConsumer<ND, Integer, T> vls(final T value) {
		return (nd, i, t) -> nd.set(i, value);
	}

	/**
	 * value setter <br>
	 * entry setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.rows(10).get(3).assign(vls(10000));
	 *
	 * @param <T>   元素类型
	 * @param <ND>  INdarray 的衍生类
	 * @param valnd 常量向量 value ndarray,索引循环利用以匹配this长度
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> TriConsumer<ND, Integer, T> vls(final ND valnd) {
		final int len = valnd == null ? 0 : valnd.length();
		return (nd, i, t) -> nd.set(i, len < 1 ? t : valnd.get(i % len));
	}

	/**
	 * unary t setter <br>
	 * entry setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.rows(10).get(0).assign(ufs((t) -> 10 * t));
	 *
	 * @param <T>  元素类型
	 * @param <ND> INdarray 的衍生类
	 * @param uf   (t:元素值)->t
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> TriConsumer<ND, Integer, T> ufs(final UnaryOperator<T> uf) {
		return (nd, i, t) -> nd.set(i, uf.apply(t));
	}

	/**
	 * bifunction settern <br>
	 * entry setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.rows(10).get(1).assign(bfs((i, t) -> 100 * t));
	 *
	 * @param <T>  元素类型
	 * @param <ND> INdarray 的衍生类
	 * @param bf   (i:索引从0开始,t:元素值)->t
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> TriConsumer<ND, Integer, T> bfs(final BiFunction<Integer, T, T> bf) {
		return (nd, i, t) -> nd.set(i, bf.apply(i, t));
	}

	/**
	 * trifunction setter <br>
	 * entry setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.rows(10).get(2).assign(tfs((nd, i, t) -> 1000 * t));
	 *
	 * @param <T>  元素类型
	 * @param <ND> INdarray 的衍生类
	 * @param tf   (nd:待设置ndarray ,i:索引从0开始,t:元素值)->t
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> TriConsumer<ND, Integer, T> tfs(final TriFunction<ND, Integer, T, T> tf) {
		return (nd, i, t) -> nd.set(i, tf.apply(nd, i, t));
	}

	/**
	 * nd setter 元素设置函数 <br>
	 * 快捷设置函数，语法糖 <br>
	 *
	 * data.subset(i -> i % 10 == 0).assign(setter(nd))
	 *
	 * @param <T>  元素类型
	 * @param <ND> INdarray 的衍生类
	 * @param nd   元素向量
	 * @return 设置entry 元素INdarary长度为1的
	 */
	static <T, ND extends INdarray<T>> BiConsumer<Integer, ND> nds(final INdarray<T> nd) {
		final int n = nd.length();
		return (i, _nd) -> _nd.set(nd.get(i % n));
	}

	/**
	 * 取模格式化
	 *
	 * @param <T>  元素
	 * @param mods 模长度序列, 如: 16,8,4
	 * @return np->string
	 */
	static <T> Function<INdarray<T>, String> modfmt(final int... mods) {
		final INdarray<Integer> _mods = INdarray.nd(mods);
		final Function<INdarray<T>, String> asline = mods.length > 1
				? e -> e.toString(_mods.tail().arrayOf(Types.Ints2ints))
				: e -> e.toString();

		final String delimiter = String.format("%s,", Strings.repeat("\n", mods.length));
		return nd -> String.format("[%s]", nd.cuts(mods[0]) //
				.map(asline).collect(Collectors.joining(delimiter)));

	}

	/**
	 * 枢轴分类路径。
	 * 
	 * @param <CF>        分类函数类型 t->u
	 * @param <T>         分类对象类型
	 * @param <U>         分类路径的元素类型
	 * @param classifiers 枢轴分类器序列，分类策略。比如按照：[国家,性别,年代,研究方向】的枢轴分类，<br>
	 *                    牛顿的 枢轴分类路径是 英国/男/现代/物理学家, 鲁迅:中国/男/现代/文学家, 李清照中国/女/南宋/诗人。
	 * @return 分类结果
	 */
	static <CF extends Function<T, U>, T, U> INdarray<U> pivotPath(final Iterable<CF> classifiers, final T object) {
		return INdarray.of(classifiers).fmap(f -> f.apply(object));
	}

	Function<?, INdarray<Integer>> ndint = Types.cast((INdarray<Integer>) null);
	Function<?, INdarray<Double>> nddbl = Types.cast((INdarray<Double>) null);
	Function<?, INdarray<Float>> ndfloat = Types.cast((INdarray<Float>) null);
	Function<?, INdarray<Long>> ndlng = Types.cast((INdarray<Long>) null);
	Function<?, INdarray<Number>> ndnum = Types.cast((INdarray<Number>) null);
	Function<?, INdarray<Byte>> ndbyte = Types.cast((INdarray<Byte>) null);
	Function<?, INdarray<Boolean>> ndbool = Types.cast((INdarray<Boolean>) null);
	Function<?, INdarray<Character>> ndchar = Types.cast((INdarray<Character>) null);
	Function<?, INdarray<String>> ndstr = Types.cast((INdarray<String>) null);
	Function<?, INdarray<Short>> ndshort = Types.cast((INdarray<Short>) null);

}
