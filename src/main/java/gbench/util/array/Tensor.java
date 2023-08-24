package gbench.util.array;

import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Types;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 张量：多维数组<br>
 * 多维数组采用 树形结构，索引向量 访问的数据模型。<br>
 * 原始数据元素数组,tensor用一维物理存储，表示多维逻辑数组。
 *
 * @param <T> 元素类型
 * @author gbench
 */
public class Tensor<T> implements Comparable<Tensor<T>>, IStream<T> {

	/**
	 * 张量构造函数
	 *
	 * @param data 源数据
	 * @param dims 维度信息, 空间的维度结构
	 */
	public Tensor(final T[] data, final int... dims) {
		this.data = data;
		this.dims = dims;
		final int n = this.dims.length; // 空间维度
		this.strides = Stream.iterate(1, i -> i + 1).limit(n - 1)
				.mapToInt(i -> IntStream.iterate(i, j -> j + 1).limit(n - i) // 流程
						.map(j -> dims[j]).reduce(1, (a, b) -> a * b))
				.toArray(); // 计算数组跨度
		this.index_reader = index -> at(this.data, offset(index));
		this.index_writer = index -> t -> {
			final int i = this.offset(index);
			this.data[i] = t;
		};
	}

	/**
	 * Tensor
	 * 
	 * @param data 矩阵数据，行顺序
	 * @param dims 维度向量
	 */
	public Tensor(final Iterable<T> data, final int... dims) {
		this(Types.itr2array(data), dims);
	}

	/**
	 * 原始数据元素数组,tensor用一维物理存储，表示多维逻辑数组。
	 *
	 * @return 张量数据数组
	 */
	public T[] data() {
		return data;
	}

	/**
	 * 原始数据元素流
	 */
	public Stream<T> stream() {
		return Arrays.stream(this.data);
	}

	/**
	 * 生成索引键值对儿 <br>
	 * <p>
	 * 注意index为共享式，如需要持久保存请进行复制，例如：Arrays.copyOf,Arrays.asList等 <br>
	 *
	 * @return [(index, t)] 的 流数据
	 */
	public Stream<Tuple2<Integer[], T>> tupleS() {
		final Integer[][] ts = Arrays.stream(this.dims)
				.mapToObj(d -> Stream.iterate(0, i -> i + 1).limit(d).toArray(Integer[]::new))
				.toArray(Integer[][]::new);
		return cph(ts).map(index -> Tuple2.of(index, this.at(index)));
	}

	/**
	 * 生成索引键值对儿
	 *
	 * @param <K>   键名类型
	 * @param keyer 索引变换函数
	 *              index-&gt;key,注意index为共享式，如需要持久保存请进行复制，例如：Arrays.copyOf,Arrays.asList等
	 * @return [(k, t)] 的 流数据
	 */
	public <K> Stream<Tuple2<K, T>> tupleS(final Function<Integer[], K> keyer) {
		return this.tupleS().map(e -> e.fmap1(keyer));
	}

	/**
	 * 数组维度，返回一个数。
	 *
	 * @return 张量维度信息
	 */
	public int ndim() {
		return this.dims.length;
	}

	/**
	 * 各个维度大小的数组，一维数组:一个数,二维数组:两个数,三维数组:3个数,...
	 *
	 * @return 各个维度大小的数组
	 */
	public int[] dims() {
		return this.dims;
	}

	/**
	 * dims() 的别名,参照 numpy的命名 <br>
	 * 各个维度大小的数组，一维数组:一个数,二维数组:两个数,三维数组:3个数,...
	 * 
	 * @return 张量维度信息
	 */
	public int[] shape() {
		return this.dims;
	}

	/**
	 * dims() 的别名,参照 numpy的命名 <br>
	 * 各个维度大小的数组，一维数组:一个数,二维数组:两个数,三维数组:3个数,...
	 * 
	 * @return 张量维度信息
	 */
	public INdarray<Integer> shape_nd() {
		return INdarray.nd(this.dims);
	}

	/**
	 * 张量T类型的元素个数，dims的乘积。
	 *
	 * @return 元素个数
	 */
	public int length() {
		return Tensor.length(this.dims);
	}

	/**
	 * 读取指定索引位置的元素
	 *
	 * @param index 索引向量,元素值从0开始
	 * @return 数据读取器
	 */
	public T at(final int... index) {
		return this.index_reader.apply(index);
	}

	/**
	 * 读取指定索引位置的元素怒
	 *
	 * @param index 索引向量,元素值从0开始
	 * @return 数据读取器
	 */
	public T at(final Integer[] index) {
		return this.at(Stream.of(index).mapToInt(e -> e).toArray());
	}

	/**
	 * 读取指定索引位置的元素
	 *
	 * @param index 索引向量,元素值从0开始
	 * @return 数据读取器
	 */
	public Tensor<T> get(final int... index) {
		return this.getItem(index)._1;
	}

	/**
	 * 读取指定索引位置的元素
	 *
	 * @param index 索引向量,元素值从0开始
	 * @param t     T类型的元素
	 * @return 对象本身用于实现链式编程
	 */
	public Tensor<T> set(final int[] index, final T t) {
		this.index_writer.apply(index).accept(t);
		return this;
	}

	/**
	 * 读取指定索引位置的元素
	 *
	 * @param index 索引向量,元素值从0开始
	 * @return 数据读取器(tensor:index前ndim-1所对应的向量, t:index最后一维对应元素)
	 */
	public Tuple2<Tensor<T>, T> getItem(final int... index) {
		return this.getItem(null, index);
	}

	/**
	 * 读取指定索引位置的元素
	 *
	 * @param path  递进的搜素路径
	 * @param index 索引向量,元素值从0开始
	 * @return 数据读取器(tensor:index前ndim-1所对应的向量, t:index最后一维对应元素)
	 */
	public Tuple2<Tensor<T>, T> getItem(final List<Tensor<T>> path, final int... index) {
		final int i = index[0];
		final int stride = this.strides.length > 0 //
				? this.strides[0] // 获取第一索引的跨度信息
				: 1; // 终端节点 跨度为1
		final int j = i * stride;
		final T[] _data = Arrays.copyOfRange(this.data, j, j + stride);
		final int[] _dims = this.dims.length <= 1 // 是否为多为向量
				? null // 一维度向量的子元素
				: Arrays.copyOfRange(this.dims, 1, this.dims.length);

		if (_dims == null) { // 进入到了元素级别,叶子元素节点
			return Tuple2.of(null, _data[0]);
		} else { // 子项目类型
			final Tensor<T> _tensor = new Tensor<>(_data, _dims); // 子项目
			if (path != null) { // 路径有效
				path.add(_tensor);
			}
			if (index.length <= 1) { // 末端索引
				return Tuple2.of(_tensor, null); // 末端
			} else { // 中间索引
				final int[] _index = Arrays.copyOfRange(index, 1, index.length); // 递进子索引
				return _tensor.getItem(path, _index); // 中间
			} // if
		} // if
	}

	/**
	 * 获取所有子项目：遍历第一阶层
	 *
	 * @return 子项目流，对于 1维度向量，子项目为空流
	 */
	public Stream<Tensor<T>> itemS() {
		if (this.dims.length > 1) {
			return Stream.iterate(0, i -> i + 1).limit(this.dims[0]).map(this::get);
		} else {
			return Stream.empty();
		}
	}

	/**
	 * 获取子项目：遍历指定阶层
	 *
	 * @param index 索引向量，索引匀元素从0开始
	 * @return 子项目流，对于 1维度向量，子项目为空流
	 */
	public Stream<Tensor<T>> itemS(final int... index) {
		return Optional.ofNullable(this.get(index)).map(Tensor::itemS).orElse(Stream.empty());
	}

	/**
	 * itemS 的别名
	 *
	 * @return 子张量流
	 */
	public Stream<Tensor<T>> childrenS() {
		return this.itemS();
	}

	/**
	 * itemS 的别名
	 *
	 * @return 子张量列表
	 */
	public List<Tensor<T>> children() {
		return this.itemS().collect(Collectors.toList());
	}

	/**
	 * 读取指定索引位置的元素
	 *
	 * @param index 索引向量,元素值从0开始
	 * @return 对象本身用于实现链式编程
	 */
	public Consumer<T> writer(final int... index) {
		return this.index_writer.apply(index);
	}

	/**
	 * 转印转偏移
	 *
	 * @param index 索引向量
	 * @return 所以转跨度
	 */
	public int offset(final int[] index) {
		return Tensor.index2offset(index, this.strides);
	}

	/**
	 * 更改矩阵形状
	 *
	 * @param dims 新的矩阵尺寸
	 * @return 新的矩阵的形状
	 */
	public Tensor<T> reshape(final int... dims) {
		final int old_len = this.length();
		final int new_len = Tensor.length(dims);
		final T[] data = Arrays.copyOf(this.data, new_len);
		for (int i = old_len; i < new_len; i++) { // 循环填充
			data[i] = this.data[i % old_len];
		}
		return new Tensor<>(data, dims);
	}

	/**
	 * U类型张量,保留原有shape
	 *
	 * @param mapper 元素变换器 t-&gt;u
	 * @param <U>    元素类型
	 * @return U类型的元素流 [u]
	 */
	public <U> Tensor<U> fmap(final Function<T, U> mapper) {
		return new Tensor<>(stream().map(mapper).collect(Types.arrayclc()), this.dims);
	}

	/**
	 * 判断是否为一阶向量
	 *
	 * @return dims 长度&lt;2 为1级向量,否则为非叶子节点。
	 */
	public boolean isLeaf() {
		return this.dims.length < 2;
	}

	/**
	 * 二元运行
	 *
	 * @param <U>    参数类型
	 * @param <V>    结果元素类型
	 * @param tu     二号参数
	 * @param mapper 二元运行 (t,u)-&gt;v
	 * @return tv
	 */
	public <U, V> Tensor<V> op(final Tensor<U> tu, final BiFunction<T, U, V> mapper) {
		return Tensor.bifun(mapper).apply(this, tu);
	}

	/**
	 * 复制一个 tensor
	 *
	 * @return tensor 的复制品
	 */
	public Tensor<T> duplicate() {
		final T[] _data = Arrays.copyOf(this.data, this.data.length);
		return new Tensor<>(_data, this.dims.clone());
	}

	@Override
	public Tensor<T> clone() {
		return this.duplicate();
	}

	@Override
	public int compareTo(final Tensor<T> t) {
		return Tuple2.of(this.data, this.dims).compareTo(Tuple2.of(t.data, t.dims));
	}

	/**
	 * 扁平化输出
	 *
	 * @param mapper 元素变换器
	 * @param <U>    结果元素类型
	 * @return U类型的数据流
	 */
	public <U> Stream<U> flatMapS(final Function<Tensor<T>, U> mapper) {
		final Stack<Tensor<T>> stack = new Stack<>();
		stack.push(this);
		final AtomicInteger ai = new AtomicInteger(0);
		return Stream.iterate(this, e -> ai.get() > -1, e -> {
			if (!stack.isEmpty()) {
				final Tensor<T> p = stack.pop();
				p.itemS().collect(Lisp.llclc(true, stack::addAll));
				return p;
			} else { // 空堆栈，表明进入最有一条数据了，标记ai,用于结束
				ai.decrementAndGet();
				return e;
			} // if
		}).map(mapper).skip(1); // 移除引子
	}

	/**
	 * 扁平化输出
	 *
	 * @return U类型的数据流
	 */
	public Stream<Tensor<T>> flatMapS() {
		return this.flatMapS(e -> e);
	}

	/**
	 * nd
	 *
	 * @param data    元数据
	 * @param start   开始索引从0喀什
	 * @param end     结束所以从0开始
	 * @param strides 分区索引跨度 (index,(start,end))
	 * @return nd
	 */
	public INdarray<T> nd(final T[] data, final int start, final int end,
			final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		return new StandardNdarray<>(data, start, end, strides);
	}

	/**
	 * 创建数据试图
	 *
	 * @param start 开始位置从0开始 inclusive
	 * @param end   结束位置 exclusive
	 * @return Indarray
	 */
	public INdarray<T> nd(final int start, final int end) {
		return this.nd(data, start, end, null);
	}

	/**
	 * nd
	 *
	 * @param start 开始位置从0开始
	 * @return nd
	 */
	public INdarray<T> nd(final int start) {
		return this.nd(this.data, start, this.data.length, null);
	}

	/**
	 * 多维数组
	 *
	 * @param partitioner 拾取器
	 * @return nd
	 */
	public INdarray<T> nd(final Partitioner partitioner) {
		return this.nd(data, 0, this.length(), partitioner.strides());
	}

	/**
	 * 多维数组
	 *
	 * @param strides 分区索引跨度
	 * @return nd
	 */
	public INdarray<T> nd(final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		return this.nd(data, 0, this.length(), strides);
	}

	/**
	 * nd 多维数组
	 *
	 * @return nd
	 */
	public INdarray<T> nd() {
		return this.nd(data, 0, this.length(), null);
	}

	/**
	 * 转换成 nd
	 *
	 * @param partitioner 分区器
	 * @return nd 数据流
	 */
	public Stream<INdarray<T>> partitionS(final Partitioner partitioner) {
		return this.nd(partitioner).partitionS();
	}

	/**
	 * 转换成 nd
	 *
	 * @param strides 分区索引跨度
	 * @return nd 数据流
	 */
	public Stream<INdarray<T>> partitionS(final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		return this.nd(strides).partitionS();
	}

	/**
	 * 转换成 nd
	 *
	 * @param breaks 分割点
	 * @return nd 数据流
	 */
	public Stream<INdarray<T>> partitionS(final int... breaks) {
		return this.nd().partitionS(breaks);
	}

	/**
	 * 转换成 nd
	 *
	 * @param partitioner 拾取器
	 * @return nd map
	 */
	public Map<List<Integer>, INdarray<T>> partitions(final Partitioner partitioner) {
		return this.nd(partitioner).partitions();
	}

	/**
	 * 转换成 nd
	 *
	 * @param strides 分区索引跨度
	 * @return nd Map
	 */
	public Map<List<Integer>, INdarray<T>> partitions(
			final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		return this.nd(strides).partitions();
	}

	/**
	 * 转换成 nd
	 *
	 * @param breaks 分割点
	 * @return nd Map
	 */
	public List<INdarray<T>> partitions(final int... breaks) {
		return this.partitionS(breaks).collect(Collectors.toList());
	}

	@Override
	public Iterator<T> iterator() {
		return Arrays.asList(this.data).iterator();
	}

	/**
	 * 数组应用
	 * 
	 * @param <SELF>   本身类型
	 * @param <TARGET> 目标结果类型
	 * @param mapper   结果变换类型 this-&gt;u, this 为当前ISeq对象
	 * @return U类型的结果
	 */
	@SuppressWarnings("unchecked")
	public <SELF extends Tensor<T>, TARGET> TARGET mutate(final Function<SELF, TARGET> mapper) {
		return mapper.apply((SELF) this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { this.data, this.dims });
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Tensor) {
			return 0 == ((Tensor<Object>) this).compareTo((Tensor<Object>) obj);
		} else {
			return false;
		} // if
	}

	/**
	 * 格式化
	 */
	public String toString() {
		return format(this.data, e -> Optional.ofNullable(e) //
				.map(Object::toString).orElse("-"), this.dims);
	}

	/**
	 * 张量T类型的元素个数，dims的乘积。
	 *
	 * @param dims 维度序列
	 * @return 元素个数
	 */
	public static int length(final int... dims) {
		final int size = IntStream.of(dims).reduce(1, (a, b) -> a * b);
		return size;
	}

	/**
	 * 格式化
	 *
	 * @param <T>       元素类型
	 * @param ts        数据数组
	 * @param formatter 元素格式化
	 * @param dims      空间维度
	 * @return 格式化输出
	 */
	public static <T> String format(final T[] ts, final Function<Object, String> formatter, final int... dims) {
		return Optional.ofNullable(ts).map(data -> {
			if (dims.length < 2) { // 1维
				return Stream.of(data).map(formatter).collect(Collectors.joining(","));
			} else { // 高维
				final int[] _dims = Arrays.copyOfRange(dims, 1, dims.length); // 剩余维度
				final int _size = Tensor.length(_dims); // 剩余的维度的尺寸
				final String gap = Stream.iterate(0, i -> i + 1).map(i -> "\n") //
						.limit(_dims.length).collect(Collectors.joining()); // 区间间隔
				return Stream.iterate(0, i -> i + 1).limit(dims[0]).map(i -> {
					final int _offset = i * _size;
					final T[] _ts = Arrays.copyOfRange(data, _offset, _offset + _size);
					return Tensor.of(_ts, _dims);
				}).map(Object::toString).collect(Collectors.joining("," + gap));
			}
		}).map(line -> String.format("[%s]", line)).orElse("null");
	}

	/**
	 * 点乘
	 *
	 * @param <T> 元素类型
	 * @param ts1 1 号元素
	 * @param ts2 2 号元素
	 * @return 点积结果
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T dot(final T[] ts1, final T[] ts2) {
		return (T) Stream.iterate(0, i -> i + 1).limit(Math.max(ts1.length, ts2.length))
				.map(i -> optAt(ts1, i).map(t1 -> optAt(ts2, i)//
						.map(t2 -> t1.doubleValue() * t2.doubleValue()).orElse(0d)).orElse(0d))
				.reduce((a, b) -> a * b).orElse(0d);
	}

	/**
	 * ts 中的第offset_index位置上的元素
	 * 
	 * @param <T>          元素类型
	 * @param ts           数据源
	 * @param offset_index 偏移索引 从0开始
	 * @return 依据偏移索引读取数据
	 */
	public static <T> T at(final T[] ts, final int offset_index) {
		return ts[offset_index % ts.length];
	}

	/**
	 * ts 中的第offset_index位置上的元素 Optional类型
	 * 
	 * @param <T>          元素类型
	 * @param ts           数据源
	 * @param offset_index 从0开始
	 * @return 依据偏移索引读取数据
	 */
	public static <T> Optional<T> optAt(final T[] ts, final int offset_index) {
		return Optional.ofNullable(at(ts, offset_index));
	}

	/**
	 * 数据
	 *
	 * @param <T>  元素类型
	 * @param data 源数据
	 * @param dims 维度信息
	 * @return n 维度 张量
	 */
	public static <T> Tensor<T> of(final T[] data, final int... dims) {
		return new Tensor<>(data, dims);
	}

	/**
	 * 数据
	 *
	 * @param <T>  元素类型
	 * @param data 源数据
	 * @return 张量 1维度
	 */
	public static <T> Tensor<T> of(final T[] data) {
		return of(data, data.length);
	}

	/**
	 * 数据
	 *
	 * @param <T>     元素类型
	 * @param cellgen 元素生成器
	 * @param n       向量维度
	 * @return 张量 1维度
	 */
	public static <T> Tensor<T> of(final Function<Integer, T> cellgen, final int n) {
		final T[] data = Stream.iterate(0, i -> i + 1).map(cellgen).limit(n).collect(Types.arrayclc());
		return of(data);
	}

	/**
	 * 分区器具构造
	 *
	 * @param <T>     元素类型
	 * @param cellgen 元素生成器
	 * @param p       分区器
	 * @return INdarray
	 */
	public static <T> INdarray<T> nd(final Function<Integer, T> cellgen, final Partitioner p) {
		return Tensor.of(cellgen, p.length()).nd(p);
	}

	/**
	 * 分区器具构造
	 *
	 * @param <T> 元素类型
	 * @param ts  元数据长度
	 * @param p   分区器
	 * @return INdarray
	 */
	public static <T> INdarray<T> nd(final T[] ts, final Partitioner p) {
		assert ts.length == p.length() : String.format("分区器长度与数据长度不等,ts:%s,p:%s", ts.length, p.length());
		return Tensor.of(ts).nd(p);
	}

	/**
	 * 索引转偏位位置 index[n]+index[0:n)*stride[0:n) <br>
	 * 正常应该是 index 长度比strides多1，最后一项index表示偏移,若是index长度小于strides,则默认偏移为0 <br>
	 *
	 * @param index   索引向量,index长度比strides长度多1
	 * @param strides 分区索引跨度
	 * @return 索引标记的偏移位置
	 */
	public static int index2offset(final int[] index, final int[] strides) {
		final int n = Math.min(index.length, strides.length); // 获取最短长度
		// 正常应该是 index 长度比strides多1，最后一项index表示偏移,若是index长度小于strides,则默认偏移为0
		final int offset = index.length < strides.length ? 0 : index[n];
		return Stream.iterate(0, i -> i + 1).limit(n) //
				.map(i -> index[i] * strides[i]).reduce(offset, Integer::sum);
	}

	/**
	 * 张量二元运算
	 *
	 * @param <X>  1号矩阵
	 * @param <Y>  2号矩阵
	 * @param <Z>  结果矩阵
	 * @param biop 二元运算 (x,y)->z
	 * @return (mx, my)->mz
	 */
	public static <X, Y, Z> BiFunction<Tensor<X>, Tensor<Y>, Tensor<Z>> bifun(final BiFunction<X, Y, Z> biop) {
		return (tx, ty) -> {
			final int[] x_dims = tx.dims;
			final int[] y_dims = ty.dims;
			final int n = Math.max(x_dims.length, y_dims.length); // 向量空间的维度
			final int[] new_dims = x_dims == y_dims ? x_dims
					: Stream.iterate(0, i -> i + 1).limit(n)
							.mapToInt(i -> Math.max(x_dims[i % x_dims.length], y_dims[i % y_dims.length])).toArray();
			final X[] x = Arrays.equals(new_dims, x_dims) ? tx.data : tx.reshape(new_dims).data;
			final Y[] y = Arrays.equals(new_dims, y_dims) ? ty.data : ty.reshape(new_dims).data;
			final Z[] z = Stream.iterate(0, i -> i + 1).limit(Tensor.length(new_dims)) //
					.map(i -> biop.apply(x[i], y[i])).collect(Types.arrayclc());
			return Tensor.of(z, new_dims == x_dims ? new_dims.clone() : new_dims);
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
			final int i = ai.getAndIncrement(); // level 阶层位置，注意一定要用非对象类型:int而非Integer
			return parentS.flatMap(parent -> Stream.of(tt).map(t -> buffer[i] = t)); // 反腐刷新工作缓存输出流
		}, (a, b) -> a).map(e -> buffer); // 输出缓存状态
	}

	protected final T[] data; // 原始数据元素数组
	protected final int[] dims; // 维度数据
	protected final int[] strides; // 分区索引跨度

	private final Function<int[], T> index_reader; // 索引读取器
	private final Function<int[], Consumer<T>> index_writer; // 索引书写器

}
