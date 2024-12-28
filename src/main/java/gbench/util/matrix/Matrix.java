package gbench.util.matrix;

import gbench.util.array.INdarray;
import gbench.util.array.StandardNdarray;
import gbench.util.array.Tensor;
import gbench.util.function.TriFunction;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Types;
import gbench.util.lisp.IRecord;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 矩阵设置(行序优先）
 *
 * @param <T> 元素类型
 * @author xuqinghua
 */
public class Matrix<T> extends Tensor<T> {

	/**
	 * Matrix
	 * 
	 * @param data 矩阵数据，行顺序
	 * @param nrow 行长度
	 * @param ncol 列宽度
	 */
	public Matrix(final Iterable<T> data, final int nrow, final int ncol) {
		super(Types.itr2array(data), nrow, ncol);
	}

	/**
	 * Matrix
	 * 
	 * @param data   矩阵数据，行顺序
	 * @param tclass 矩阵元素类型
	 * @param nrow   行长度
	 * @param ncol   列宽度
	 */
	@SuppressWarnings("unchecked")
	public Matrix(final Stream<T> data, final Class<T> tclass, final int nrow, final int ncol) {
		super(data.toArray(n -> (T[]) Array.newInstance(tclass, n)), nrow, ncol);
	}

	/**
	 * Matrix
	 * 
	 * @param data 矩阵数据，行顺序
	 * @param nrow 行数
	 * @param ncol 列宽度
	 */
	public Matrix(final T[] data, final int nrow, final int ncol) {
		super(data, nrow, ncol);
	}

	/**
	 * Matrix
	 * 
	 * @param data 矩阵数据，行顺序
	 */
	public Matrix(final T[] data) {
		this(data, data.length, 1);
	}

	/**
	 * Matrix
	 * 
	 * @param tensor 矩阵数据，行顺序
	 */
	public Matrix(final Tensor<T> tensor) {
		super(tensor.data(), tensor.dims());
	}

	/**
	 * tclass
	 * 
	 * @return 元素类型
	 */
	@SuppressWarnings("unchecked")
	public Class<T> tclass() {
		return (Class<T>) this.data.getClass().getComponentType();
	}

	/**
	 * ncol
	 * 
	 * @return 列数
	 */
	public int ncol() {
		return this.dims[1];
	}

	/**
	 * nrow
	 * 
	 * @return 行数
	 */
	public int nrow() {
		return this.dims[0];
	}

	/**
	 * row
	 * 
	 * @param i 行索引从0开始
	 * @return row
	 */
	public T[] row(final int i) {
		return Arrays.copyOfRange(this.data, this.ncol() * i, this.ncol() * (i + 1));
	}

	/**
	 * 行向量 1 x ncol
	 *
	 * @param i 行向量
	 * @return Matrix
	 */
	public Matrix<T> vrow(final int i) {
		return Matrix.of(this.row(i), 1, this.ncol());
	}

	/**
	 * INdarray 数据类型
	 * 
	 * @param i 行索引 从0开始
	 * @return INdarray
	 */

	public INdarray<T> ndrow(final int i) {
		final int n = this.ncol();
		final int start = i * n;
		final int end = start + n;
		final INdarray<T> ndrow = new StandardNdarray<>(this.data, start, end, null);
		return ndrow;
	}

	/**
	 * 行列表
	 * 
	 * @return 行数据 [ [a11,a12,...], [a21,a22,...], ...,]
	 */
	public INdarray<INdarray<T>> ndrows() {
		return Stream.iterate(0, i -> i + 1).limit(this.nrow()) //
				.map(i -> new StandardNdarray<>(this.data, i * this.ncol(), i * this.ncol() + this.ncol(), null)) //
				.collect(INdarray.ndclc());
	}

	/**
	 * rowS
	 * 
	 * @return rowS
	 */
	public Stream<T[]> rowS() {
		return Stream.iterate(0, i -> i + 1).limit(this.nrow()).map(i -> this.row(i));
	}

	/**
	 * rows
	 * 
	 * @return rows
	 */
	@SuppressWarnings("unchecked")
	public T[][] rows() {
		return this.rowS().toArray(n -> (T[][]) Array.newInstance(this.data.getClass(), n));
	}

	/**
	 * 转置
	 *
	 * @return transpose
	 */
	public Matrix<T> transpose() {
		final int ncol = this.ncol();
		final int nrow = this.nrow();
		final T[] dd = this.newArray(nrow * ncol);

		Stream.iterate(0, i -> i + 1).limit(ncol * nrow).forEach(i -> {
			final int p = i / ncol; // 行号
			final int q = i % ncol; // 列号
			dd[q * nrow + p] = data[i]; // 转置后的元素
		});
		return Matrix.of(dd, ncol, nrow);
	}

	/**
	 * columnS
	 * 
	 * @return columnS
	 */
	public Stream<T[]> columnS() {
		return Stream.iterate(0, i -> i + 1).limit(this.ncol()).map(i -> this.column(i));
	}

	/**
	 * columns
	 * 
	 * @return columns
	 */
	public List<T[]> columns() {
		return this.columnS().collect(Collectors.toList());
	}

	/**
	 * 列向量 nrow x 1
	 *
	 * @param j 列索引从0开始
	 * @return 列向量
	 */
	public Matrix<T> vcol(final int j) {
		return Matrix.of(this.column(j));
	}

	/**
	 * INdarray 数据类型
	 * 
	 * @param j 行索引 从0开始
	 * @return INdarray
	 */

	public INdarray<T> ndcol(final int j) {
		final INdarray<T> ndcol = this.vcol(j).nd();
		this.add_nd_interceptor_method_set((StandardNdarray<T>) ndcol, j);

		return ndcol;
	}

	/**
	 * 列数据:每个列元素是一个长度为1的INdarray，一个列是一个mx1的矩阵
	 * 
	 * @return 列数据。[ [[a11],[a21],...], [[a12],[a22],...], ,..., ]
	 */
	public INdarray<INdarray<INdarray<T>>> ndcols() {
		return Stream.iterate(0, k -> k + 1).limit(this.ncol()) //
				.map(j -> Stream.iterate(0, i -> i + 1).limit(this.nrow()) // 每一个列都包含由nrow个元素
						.map(i -> i * this.nrow() + j) // 行首位置索引
						.map(i -> (INdarray<T>) new StandardNdarray<>(this.data(), i, i + 1, null)) //
						.collect(INdarray.ndclc()))
				.collect(INdarray.ndclc());
	}

	/**
	 * 增加ndarray的拦截器
	 * 
	 * @param target nd 对象
	 * @param index  向量偏移位置,从0开始
	 * @return Matrix
	 */
	@SuppressWarnings("unchecked")
	public Matrix<T> add_nd_interceptor_method_set(final Object target, final int index) {
		try {
			if (target instanceof StandardNdarray) {
				final StandardNdarray<T> ndarray = (StandardNdarray<T>) target;
				ndarray.add_method_interceptor("set", (nd, args) -> { // set 方法拦截
					final Integer i = (Integer) args[0];
					final T value = (T) args[1];
					this.cell(i, index, value);
				}); // add_method_interceptor
			} // if
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return this;
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果流元素类型
	 * @param i      列索引从0开始
	 * @param mapper t-&gt;u 元素变换
	 * @return U类型的流
	 */
	public <U> Stream<U> colS(final int i, final Function<T, U> mapper) {
		return Stream.of(this.column(i)).map(mapper);
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果的流的元素类型
	 * @param i      行索引从0开始
	 * @param mapper t-&gt;u 列变换函数
	 * @return U类型的流
	 */
	public <U> Stream<U> rowS(final int i, final Function<T, U> mapper) {
		return Stream.of(this.row(i)).map(mapper);
	}

	/**
	 * cbind
	 * 
	 * @param m 另一个矩阵
	 * @return Matrix
	 */
	public Matrix<T> cbind(final Matrix<T> m) {
		return Stream.concat(this.columnS(), m.columnS()).collect(cmxclc());
	}

	/**
	 * rbind
	 * 
	 * @param m 另一个矩阵
	 * @return Matrix
	 */
	public Matrix<T> rbind(final Matrix<T> m) {
		return Stream.concat(this.rowS(), m.rowS()).collect(rmxclc());
	}

	/**
	 * 列向量
	 *
	 * @param j 列索引从0开始
	 * @return 列向量
	 */
	public T[] column(final int j) {
		return Stream.iterate(0, i -> i + 1).limit(this.nrow()).map(i -> this.data[i * this.ncol() + j])
				.toArray(this::newArray);
	}

	/**
	 * 计算 占比(ai/total)
	 *
	 * @param <N> 数值转换函数
	 * @return 频率矩阵
	 */
	public <N extends Number> Matrix<Double> ratio() {
		return this.ratio(null);
	}

	/**
	 * 计算 占比(ai/total)
	 *
	 * @param <U>    结果矩阵的元素类型
	 * @param mapper 变换器 (u,t)-&gt;u
	 * @param u      起始规约值
	 * @return 频率矩阵
	 */
	public <U> Matrix<U> scanl(final BiFunction<U, T, U> mapper, final U u) {
		final Iterator<T> titr = this.iterator();
		final List<U> data = Stream.iterate(u, _u -> titr.hasNext(), _u -> mapper.apply(_u, titr.next()))
				.collect(Collectors.toList());

		return Matrix.of(data);
	}

	/**
	 * 计算 占比(ai/total)
	 *
	 * @param <U>    mapper 结果类型
	 * @param mapper 变换函数
	 * @return 频率矩阵
	 */
	public <U> Matrix<U> scanl(final Function<Matrix<T>, U> mapper) {
		final ArrayList<T> tt = new ArrayList<T>();
		return this.stream().map(e -> {
			tt.add(e);
			return Matrix.of(tt);
		}).map(mapper).collect(Matrix.mxclc());
	}

	/**
	 * 计算 占比(ai/total)
	 *
	 * @param <N>   数值转换函数
	 * @param tonum 转换成数字
	 * @return 频率矩阵
	 */
	public <N extends Number> Matrix<Double> ratio(final Function<T, N> tonum) {
		final double total = this.sum(tonum);
		return this.fmap(e -> {
			final double d = (tonum == null ? ((Number) e).doubleValue() : tonum.apply(e).doubleValue()) / total;
			return d;
		});
	}

	/**
	 * 生成数值矩阵
	 *
	 * @param <N>    数字元素类型
	 * @param mapper 数值变换函数
	 * @return 数字矩阵
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> Matrix<N> numx(final Function<T, N> mapper) {
		return this.fmap(e -> null == mapper ? (N) e : mapper.apply(e));
	}

	/**
	 * 生成数值矩阵
	 *
	 * @return 双精度矩阵
	 */
	public Matrix<Double> dblx() {
		return this.numx(e -> ((Number) e).doubleValue());
	}

	/**
	 * mmult
	 * 
	 * @param <U> U
	 * @param mu  mu
	 * @return Matrix
	 */
	public <U> Matrix<List<Tuple2<T, U>>> mmult(final Matrix<U> mu) {
		return this.mmult(mu, Tuple2::new, Collectors.toList());
	}

	/**
	 * 矩阵计算
	 *
	 * @param <U>   U
	 * @param <N>   N
	 * @param tonum 转换成数值
	 * @param mu    右侧矩阵
	 * @return Matrix
	 */
	@SuppressWarnings("unchecked")
	public <U extends Number, N extends Number> Matrix<Double> mmult(final Function<T, N> tonum, final Matrix<U> mu) {
		return (Matrix<Double>) this.fmap(e -> tonum == null ? (N) e : tonum.apply(e)).mmult(mu, mul_op(), plus_clc());
	}

	/**
	 * mmult
	 * 
	 * @param mu        右侧矩阵
	 * @param biop      二元操作
	 * @param collector 轨迹其
	 * @param <U>       右侧矩阵类型
	 * @param <X>       X 类型
	 * @param <Y>       Y类型
	 * @return Y类型的矩阵
	 */
	@SuppressWarnings("unchecked")
	public <U, X, Y> Matrix<Y> mmult(final Matrix<U> mu, BiFunction<T, U, X> biop, Collector<X, ?, Y> collector) {
		final Optional<Class<Y>> opt = (Optional<Class<Y>>) (Object) this.stream().filter(Objects::nonNull).findFirst()
				.flatMap(t -> mu.stream().filter(Objects::nonNull).findFirst().map(u -> biop.apply(t, u)))
				.map(e -> Stream.of(e).collect(collector)).map(Object::getClass);
		return this.mmult(mu, biop, collector, opt.orElse(null));
	}

	/**
	 * 矩阵乘法
	 *
	 * @param mu        u matrix
	 * @param biop      (t,u)->元素归集
	 * @param collector [x]->y 结果类归集器
	 * @param yclass    结果类名
	 * @param <U>       U
	 * @param <X>       X
	 * @param <Y>       Y
	 * @return Matrix
	 */
	public <U, X, Y> Matrix<Y> mmult(final Matrix<U> mu, BiFunction<T, U, X> biop, Collector<X, ?, Y> collector,
			final Class<Y> yclass) {
		final int nrow = this.nrow();
		final int depth = this.ncol();
		final int ncol = mu.ncol();
		final T[] tt = this.data;
		final U[] uu = mu.data;
		final Stream<Y> yyS = Stream.iterate(0, i -> i + 1).limit(nrow)// i
				.flatMap(i -> Stream.iterate(0, j -> j + 1).limit(ncol) // j
						.map(j -> Stream.iterate(0, k -> k + 1).limit(depth) // j
								.map(k -> biop.apply(tt[i * depth + k], uu[k * ncol + j])).collect(collector)));

		return Optional.ofNullable(yclass).map(ycls -> new Matrix<>(yyS.toArray(n -> newArray(ycls, n)), nrow, ncol))
				.orElseGet(() -> new Matrix<>(yyS.collect(Collectors.toList()), nrow, ncol));
	}

	/**
	 * 数据点乘积
	 *
	 * @param <U>  U
	 * @param <X>  X
	 * @param mu   右侧矩阵
	 * @param biop 数据城府 (t,u)-&gt;x
	 * @return 数据矩阵
	 */
	public <U, X, Y> Y dot(final Matrix<U> mu, BiFunction<T, U, X> biop, Collector<X, ?, Y> collector) {
		final Y y = this.stream().map(Tuple2.zipper2(mu.data)).map(e -> biop.apply(e._1, e._2)).collect(collector);
		return y;
	}

	/**
	 * 数据点乘积
	 *
	 * @param <U> U
	 * @param mu  右侧矩阵
	 * @return 数据矩阵
	 */
	public <U> List<Tuple2<T, U>> dot(final Matrix<U> mu) {
		return this.dot(mu, Tuple2::new, Collectors.toList());
	}

	/**
	 * 数据点乘积
	 *
	 * @param <U> U
	 * @param <N> N
	 * @param mu  右侧矩阵
	 * @return 数据矩阵
	 */
	@SuppressWarnings("unchecked")
	public <U extends Number, N extends Number> Double dot(final Function<T, N> tonum, final Matrix<U> mu) {
		return this.fmap(e -> tonum == null ? (N) e : tonum.apply(e)).dot(mu, mul_op(), plus_clc());
	}

	/**
	 * 矩阵二元运算 cellwise 运算
	 *
	 * @param <U>         U
	 * @param <X>         X
	 * @param mu          另一个矩阵
	 * @param pair_mapper (t,u)-&gt;x
	 * @return Matrix
	 */
	public <U, X> Matrix<X> fmap(final Matrix<U> mu, BiFunction<T, U, X> pair_mapper) {
		return this.fmap(mu.pairf((pair_mapper)));
	}

	/**
	 * 矩阵二元运算 cellwise 运算
	 *
	 * @param <U> U
	 * @param <X> X
	 * @param mu  另一个矩阵
	 * @return Matrix
	 */
	public <U, X> Matrix<Tuple2<T, U>> fmap(final Matrix<U> mu) {
		return this.fmap(mu.pairf((Tuple2::of)));
	}

	/**
	 * 数据变换
	 *
	 * @param <U>    元素类型
	 * @param mapper 元素映射 (i,j),t -&gt; u
	 * @return U类型数据矩阵
	 */
	public <U> Matrix<U> fmap(final BiFunction<Tuple2<Integer, Integer>, T, U> mapper) {
		final int ncol = this.ncol();
		final AtomicReference<Class<U>> tclass = new AtomicReference<>();
		@SuppressWarnings("unchecked")
		final U[] uu = this.stream().map(Tuple2.snb(0)).map(t -> {
			final int i = t._1 / ncol;
			final int j = t._1 % ncol;
			final U u = mapper.apply(Tuple2.of(i, j), t._2);
			return u;
		}).toArray(n -> newArray(Optional.ofNullable(tclass.get()).orElse((Class<U>) (Object) Object.class), n));

		return new Matrix<U>(uu, this.nrow(), this.ncol());
	}

	/**
	 * fmap
	 * 
	 * @param <U>    U
	 * @param mapper 元素映射 t-&gt;u
	 * @return Matrix
	 */
	public <U> Matrix<U> fmap(final Function<T, U> mapper) {
		return this.fmap((index, t) -> mapper.apply(t));
	}

	/**
	 * 元素流
	 *
	 * @return 元素流
	 */
	public Stream<T> stream() {
		return Stream.of(this.data);
	}

	/**
	 * 元素流
	 *
	 * @param <U> 流元素
	 * @return 元素流
	 */
	public <U> Stream<U> stream(final Function<T, U> mapper) {
		return Stream.of(this.data).map(mapper);
	}

	/**
	 * newArray
	 * 
	 * @param n n
	 * @return [t]
	 */
	public T[] newArray(final int n) {
		return newArray(this.tclass(), n);
	}

	/**
	 * newArray
	 * 
	 * @param m m
	 * @param n n
	 * @return [[t]]
	 */
	@SuppressWarnings("unchecked")
	public T[][] newArray(final int m, final int n) {
		return (T[][]) Array.newInstance(this.tclass(), m, n);
	}

	/**
	 * 矩阵变形
	 *
	 * @param ncol 列数据
	 * @return [[t]]
	 */
	public Matrix<T> reshape(final int ncol) {
		final int size = this.ncol() * this.nrow();
		return Matrix.of(Arrays.copyOf(data, size), ncol, size / ncol);
	}

	/**
	 * 垂直化 成 一条列向量 [1 2;3;4] -> [1,3,2,4]
	 *
	 * @return [[t]]
	 */
	public Matrix<T> veriticalize() {
		return this.transpose().reshape(1);
	}

	/**
	 * 水平化车一条行行向量 <br>
	 * [1 2;3;4] -&gt; t([1,2,3,4])
	 *
	 * @return [[t]]
	 */
	public Matrix<T> horizontalize() {
		return this.reshape(this.size());
	}

	/**
	 * (i,j) 的索引的单元格的值
	 *
	 * @param i 行索引从0开始
	 * @param j 列索引从0开始
	 * @return (i, j) 的索引的单元格的值
	 */
	public T cell(final int i, final int j) {
		return this.data[i * this.ncol() + j];
	}

	/**
	 * 设置数值
	 *
	 * @param i 行索引 从0开始
	 * @param j 列所索引 从0开始
	 * @param t 之元素
	 * @return Matrix
	 */
	public Matrix<T> cell(final int i, final int j, final T t) {
		this.data[i * this.ncol() + j] = t;
		return this;
	}

	/**
	 * 向量操作函数<br>
	 * <p>
	 * cell(i,0) 的别名
	 *
	 * @param i 元素索引
	 * @return T类型的元素
	 */
	public T get(final int i) {
		return this.cell(i, 0);
	}

	/**
	 * 向量操作函数<br>
	 * cell(i,0,t) 的别名
	 *
	 * @param i 元素索引
	 * @param t 元素t
	 * @return 矩阵对象本身
	 */
	public Matrix<T> set(final int i, final T t) {
		return this.cell(i, 0, t);
	}

	/**
	 * 单元格数组 (rows)的别名: nrow x ncol 的二维数组
	 *
	 * @return 矩阵数组二维结构形式
	 */
	public T[][] cells() {
		return this.rows();
	}

	/**
	 * 复制函数
	 *
	 * @return 复制品
	 */
	public Matrix<T> duplicate() {
		final T[] dd = Arrays.copyOf(this.data, this.data.length);
		return new Matrix<>(dd, this.nrow(), this.ncol());
	}

	/**
	 * 复制函数
	 *
	 * @return 复制品
	 */
	@Override
	public Matrix<T> clone() {
		return this.duplicate();
	}

	/**
	 * 数据尺寸大小 应该与 ncol*nrow一样大小
	 *
	 * @return 数据尺寸大小
	 */
	public int size() {
		return this.data.length;
	}

	/**
	 * 矩阵按位加法
	 * 
	 * @param <N>     数值形元素类型
	 * @param another 另一个矩阵
	 * @return [[db]] 矩阵
	 */
	public <N extends Number> Matrix<Double> plus(final Matrix<N> another) {
		return this.dblx().fmap(another, (a, b) -> Optional.ofNullable(a)
				.flatMap(_a -> Optional.of(b).map(_b -> _a.doubleValue() + _b.doubleValue())).orElse(null));
	}

	/**
	 * mul
	 * 
	 * @param <N>     N
	 * @param another 另一个矩阵
	 * @return Matrix
	 */
	public <N extends Number> Matrix<Double> mul(final Matrix<N> another) {
		return this.dblx().fmap(another, (a, b) -> Optional.ofNullable(a)
				.flatMap(_a -> Optional.of(b).map(_b -> _a.doubleValue() * _b.doubleValue())).orElse(null));
	}

	/**
	 * div
	 * 
	 * @param <N>     N
	 * @param another 另一个矩阵
	 * @return Matrix
	 */
	public <N extends Number> Matrix<Double> div(final Matrix<N> another) {
		return this.dblx().fmap(another, (a, b) -> Optional.ofNullable(a)
				.flatMap(_a -> Optional.of(b).map(_b -> _a.doubleValue() / _b.doubleValue())).orElse(null));
	}

	/**
	 * @param <N>     N
	 * @param another 另一个矩阵
	 * @return Matrix
	 */
	public <N extends Number> Matrix<Double> minus(final Matrix<N> another) {
		return this.dblx().fmap(another, (a, b) -> Optional.ofNullable(a)
				.flatMap(_a -> Optional.of(b).map(_b -> _a.doubleValue() - _b.doubleValue())).orElse(null));
	}

	/**
	 * 配对映射 mu.fmap(this.pairf((u,t)-> u+t))
	 *
	 * @param <U>         外围函数类型
	 * @param <X>         结果映射类型
	 * @param pair_mapper (u,t)->x
	 * @return (( i, j), u)->x
	 */
	public <U, X> BiFunction<Tuple2<Integer, Integer>, U, X> pairf(final BiFunction<U, T, X> pair_mapper) {
		return (index, u) -> pair_mapper.apply(u, this.cell(index._1, index._2));
	}

	/**
	 * sum
	 * 
	 * @return 求和
	 */
	public Double sum() {
		return this.sum(null);
	}

	/**
	 * 规约
	 *
	 * @param <N>   节点值的变换类型
	 * @param tonum 数值变换
	 * @return Double
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> Double sum(final Function<T, N> tonum) {
		return this.stream().map(e -> tonum != null ? tonum.apply(e) : (N) e).map(e -> e.doubleValue())
				.reduce((a, b) -> a + b).orElse(null);
	}

	/**
	 * 乘积
	 * 
	 * @return 乘积
	 */
	public Double product() {
		return this.product(null);
	}

	/**
	 * 规约
	 *
	 * @param <N>   节点值的变换类型
	 * @param tonum 数值变换
	 * @return 乘积
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> Double product(final Function<T, N> tonum) {
		return this.stream().map(e -> tonum != null ? tonum.apply(e) : (N) e).map(e -> e.doubleValue())
				.reduce((a, b) -> a * b).orElse(null);
	}

	/**
	 * 规约
	 *
	 * @param <N>  节点值的变换类型
	 * @param biop 数值变换
	 * @return T 类型的 Optional
	 */
	public <N extends Number> Optional<T> reduceOpt(final BinaryOperator<T> biop) {
		return this.stream().reduce(biop);
	}

	/**
	 * 算术运算 count 计数函数, freq 频率函数,sin,cos,tan,cot,exp
	 *
	 * @param expr r1*r2 表示 第1行 * 第2行; c1 + c2 表示 第2列+第三列
	 * @return 算术计算
	 */
	@SuppressWarnings("unchecked")
	public <X> Matrix<X> eval(final String expr) {
		final Pattern pattern = Pattern.compile("\\s*(([rc])([\\d]+))\\s*([\\+\\-\\*\\/])\\s*(([rc])([\\d]+))\\s*",
				Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(expr);
		if (matcher.find()) { // 四则混合运算
			// final String left = matcher.group(1); // 左模式
			final String op = matcher.group(4); // 算符
			// final String right = matcher.group(5); // 右模式
			final String left_mode = matcher.group(2).toLowerCase();
			final String right_mode = matcher.group(6).toLowerCase();
			final Integer i = Integer.parseInt(matcher.group(3));
			final Integer j = Integer.parseInt(matcher.group(7));

			if (left_mode.equals(right_mode)) { // 行
				if (left_mode.equals("r")) {
					// System.out.println("r");
					final Matrix<Double> r_i = this.vrow(i).dblx();
					final Matrix<Double> r_j = this.vrow(j).dblx();
					if (op.equals("+")) {
						return (Matrix<X>) r_i.plus(r_j);
					} else if (op.equals("-")) {
						return (Matrix<X>) r_i.minus(r_j);
					} else if (op.equals("*")) {
						return (Matrix<X>) r_i.mul(r_j);
					} else if (op.equals("/")) {
						return (Matrix<X>) r_i.div(r_j);
					}
				} else if (left_mode.equals("c")) {
					// System.out.println("c");
					final Matrix<Double> c_i = this.vcol(i).dblx();
					final Matrix<Double> c_j = this.vcol(j).dblx();
					if (op.equals("+")) {
						return (Matrix<X>) c_i.plus(c_j);
					} else if (op.equals("-")) {
						return (Matrix<X>) c_i.minus(c_j);
					} else if (op.equals("*")) {
						return (Matrix<X>) c_i.mul(c_j);
					} else if (op.equals("/")) {
						return (Matrix<X>) c_i.div(c_j);
					}
				}
			} else {
				return null;
			}
			return null;
		} else {
			final Pattern fpattern = Pattern.compile("\\s*([a-z_]+)\\s*\\(\\s*([rc])([\\d]+)\\s*\\)\\s*",
					Pattern.CASE_INSENSITIVE);
			final Matcher fmatcher = fpattern.matcher(expr);
			if (fmatcher.find()) {
				final String fname = fmatcher.group(1).toLowerCase();
				final String mode = fmatcher.group(2).toLowerCase();
				final Integer i = Integer.parseInt(fmatcher.group(3));
				final Matrix<T> mm = (mode.equals("c") ? this.vcol(i) : this.vrow(i));

				if (fname.equals("get")) { // 频率函数
					return (Matrix<X>) mm;
				} else { //
					final Matrix<Double> dmm = mm.dblx();
					if (fname.equals("ratio")) { // 频率函数
						return (Matrix<X>) dmm.ratio();
					} else if (fname.equals("freq")) { // 频率函数
						final double total = dmm.size();
						final Matrix<Object> _dmm = mm.stream().collect(Collectors.groupingBy(e -> e)).entrySet()
								.stream().map(e -> new Object[] { e.getKey(), e.getValue().size() / total })
								.collect(rmxclc());
						return (Matrix<X>) _dmm;
					} else if (fname.equals("sin")) { // 频率函数
						return (Matrix<X>) dmm.fmap(Math::sin);
					} else if (fname.equals("cos")) { // 频率函数
						return (Matrix<X>) dmm.fmap(Math::cos);
					} else if (fname.equals("tan")) { // 频率函数
						return (Matrix<X>) dmm.fmap(Math::tan);
					} else if (fname.equals("exp")) { // 频率函数
						return (Matrix<X>) dmm.fmap(Math::exp);
					} else if (fname.equals("count")) { // 频率函数
						return (Matrix<X>) mm.stream().collect(Collectors.groupingBy(e -> e)).entrySet().stream()
								.map(e -> new Object[] { e.getKey(), e.getValue().size() * 1d }).collect(rmxclc());
					} else {
						return null;
					} // if
				} //
			} // if

			return null;
		}
	}

	/**
	 * 交换 i 与 j 行
	 *
	 * @param i 行索引 从0开始
	 * @param j 行索引 从0开始
	 * @return i 与 j 交换后的矩阵
	 */
	public Matrix<T> swap(final int i, final int j) {
		return this.swap(i, j, false);
	}

	/**
	 * 交换 i 与 j 行
	 *
	 * @param i    行索引 从0开始
	 * @param j    行索引 从0开始
	 * @param flag 是否修改矩阵本身,true 修改本身,false 保持源矩阵不变，返回交换行后的复制品矩阵
	 * @return i 与 j 交换后的矩阵
	 */
	public Matrix<T> swap(final int i, final int j, final boolean flag) {

		if (Math.min(i, j) >= this.nrow())
			return null;
		final int ncol = this.ncol();
		if (flag) {
			final T[] i_row = Arrays.copyOfRange(this.data, ncol * i, ncol * i + ncol);
			System.arraycopy(this.data, ncol * j, this.data, ncol * i, ncol);
			System.arraycopy(i_row, 0, this.data, ncol * j, ncol);

			return this;
		} else {
			final T[] _data = Arrays.copyOf(this.data, this.data.length);
			System.arraycopy(this.data, ncol * j, _data, ncol * i, ncol);
			System.arraycopy(this.data, ncol * i, _data, ncol * j, ncol);

			return Matrix.of(_data, this.ncol(), this.nrow());
		}
	}

	/**
	 * 交换 i 与 j 列
	 *
	 * @param i 行索引 从0开始
	 * @param j 行索引 从0开始
	 * @return i 与 j 交换后的矩阵
	 */
	public Matrix<T> swap2(final int i, final int j) {
		return this.swap2(i, j, false);
	}

	/**
	 * 交换 i 与 j 列
	 *
	 * @param i    行索引 从0开始
	 * @param j    行索引 从0开始
	 * @param flag 是否修改矩阵本身,true 修改本身,false 保持源矩阵不变，返回交换行后的复制品矩阵
	 * @return i 与 j 交换后的矩阵
	 */
	public Matrix<T> swap2(final int i, final int j, final boolean flag) {
		if (Math.min(i, j) >= this.ncol())
			return null;
		final int ncol = this.ncol();
		final T[] _data = flag ? null : Arrays.copyOf(this.data, this.data.length);
		final Consumer<Integer> swap_action = flag ? (base) -> { // 修改本身
			final T t = this.data[base + i];
			this.data[base + i] = this.data[base + j];
			this.data[base + j] = t;
		} : (base) -> { // 保持源矩阵不变,返回交换行后的复制品矩阵
			_data[base + i] = this.data[base + j];
			_data[base + j] = this.data[base + i];
		};

		Stream.iterate(0, k -> k + 1).limit(this.nrow()) // 逐行处理
				.map(k -> k * ncol).forEach(swap_action);

		return flag ? this : Matrix.of(_data, this.ncol(), this.nrow());
	}

	/**
	 * 按照行进行排序
	 * 
	 * @return Matrix
	 */
	public Matrix<T> sorted() {
		return this.sorted(INdarray::compareTo);
	}

	/**
	 * 按照行进行排序
	 * 
	 * @param comparator 行比较器
	 * @return Matrix
	 */
	public Matrix<T> sorted(final Comparator<T[]> comparator) {
		return this.rowS() //
				.sorted(comparator) //
				.flatMap(e -> INdarray.of(e).stream()).collect(Matrix.mxclc(this.nrow(), this.ncol()));
	}

	/**
	 * Matrices
	 *
	 * @return Matrices
	 */
	public Matrices<T> toMatrices() {
		return new Matrices<>(this);
	}

	/**
	 * 格式化
	 *
	 * @return
	 */
	public String toString() {
		return this.toString(e -> "" + e);
	}

	/**
	 * 格式化
	 *
	 * @return
	 */
	public String toString(final Function<Object, String> format) {
		final int ncol = this.ncol();
		final int nrow = this.nrow();
		final String line = Stream.iterate(0, i -> i + 1).limit(nrow)
				.map(i -> Stream.iterate(i * ncol, j -> j + 1).limit(ncol).map(j -> data[j]))
				.map(dd -> dd.map(format).collect(Collectors.joining("\t"))).collect(Collectors.joining("\n"));
		return line;
	}

	/**
	 * @param <X>
	 * @param <Y>
	 * @return
	 */
	public static <X extends Number, Y extends Number> BiFunction<X, Y, ? extends Double> mul_op() {
		return (X x, Y y) -> x.doubleValue() * y.doubleValue();
	}

	/**
	 * @param <X>
	 * @return
	 */
	public static <X extends Number> Collector<X, ?, ? extends Double> plus_clc() {
		return Collectors.summingDouble(e -> Optional.ofNullable(e).map(Number::doubleValue).orElse(0d));
	}

	/**
	 * 矩阵生成
	 *
	 * @param data 矩阵数据
	 * @param nrow 行长度
	 * @param ncol 列宽度
	 * @param <T>  矩阵元素
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> of(final Iterable<T> data, final int nrow, final int ncol) {
		return new Matrix<>(data, nrow, ncol);
	}

	/**
	 * 矩阵生成
	 *
	 * @param data 数据矩阵
	 * @param ncol 列宽度
	 * @param nrow 行长度
	 * @param <T>  矩阵元素
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> of(final T[] data, final int nrow, final int ncol) {
		return Matrix.of(Arrays.asList(data), nrow, ncol);
	}

	/**
	 * 矩阵生成
	 *
	 * @param data 数据矩阵
	 * @param nrow 行长度
	 * @param <T>  矩阵元素
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> of(final T[] data, final int nrow) {
		return Matrix.of(Arrays.asList(data), data.length / nrow, nrow);
	}

	/**
	 * 矩阵生成
	 *
	 * @param cellgen ( i:行序号,j:列序号)->t
	 * @param nrow    行数
	 * @param ncol    列数
	 * @param <T>     元素类型
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> of(final BiFunction<Integer, Integer, T> cellgen, final int nrow, final int ncol) {
		final List<T> data = IRecord.cph(intA(nrow), intA(ncol)).map(r -> cellgen.apply(r[0], r[1]))
				.collect(Collectors.toList());
		return new Matrix<T>(data, nrow, ncol);
	}

	/**
	 * 矩阵生成
	 *
	 * @param cellgen ( i:行序号)->t
	 * @param nrow    行数
	 * @param <T>     元素类型
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> of(final Function<Integer, T> cellgen, final int nrow) {
		return Matrix.of((i, j) -> cellgen.apply(i), nrow, 1);
	}

	/**
	 * @param data 矩阵数据，行顺序
	 */
	public static <T> Matrix<T> of(final T[] data) {
		return new Matrix<T>(data);
	}

	/**
	 * @param data 矩阵数据，行顺序
	 */
	public static <T> Matrix<T> of(final Iterable<T> data) {
		final List<T> listdata = StreamSupport.stream(data.spliterator(), false).collect(Collectors.toList());
		return Matrix.of(listdata, listdata.size(), 1);
	}

	/**
	 * 生成列向量
	 *
	 * @param cellgen ( i:行序号)->t
	 * @param nrow    行数
	 * @param <T>     元素类型
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> V(final Function<Integer, T> cellgen, final int nrow) {
		return Matrix.of(cellgen, nrow);
	}

	/**
	 * 生成列向量
	 *
	 * @param nrow 行数
	 * @param <T>  元素类型
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> V(final T t, final int nrow) {
		return Matrix.of(i -> t, nrow);
	}

	/**
	 * 生成列向量:[0,1,...]
	 *
	 * @param nrow 行数
	 * @return 数据矩阵
	 */
	public static Matrix<Integer> V(final int nrow) {
		return Matrix.of(i -> i, nrow);
	}

	/**
	 * 生成列向量
	 *
	 * @param ts 向量元素
	 * @return 数据矩阵
	 */
	@SafeVarargs
	public static <T> Matrix<T> V(final T... ts) {
		return Matrix.of(ts);
	}

	/**
	 * 生成行向量,行向量
	 *
	 * @param ts 向量元素
	 * @return 数据矩阵
	 */
	@SafeVarargs
	public static <T> Matrix<T> VT(final T... ts) {
		return V(ts).transpose();
	}

	/**
	 * 生成行向量
	 *
	 * @param n 向量长度
	 * @return 数据矩阵
	 */
	public static <T> Matrix<Integer> VT(final int n) {
		return V(n).transpose();
	}

	/**
	 * 矩阵生成
	 *
	 * @param cellgen ( i:行序号,j:列序号)->t
	 * @param ncol    列数
	 * @param nrow    行数
	 * @param <T>     元素类型
	 * @return 数据矩阵
	 */
	public static <T> Matrix<T> M(final BiFunction<Integer, Integer, T> cellgen, final int nrow, final int ncol) {
		return Matrix.of(cellgen, nrow, ncol);
	}

	/**
	 * 整型流
	 * 
	 * @param n 矩阵数据流
	 * @return 整型流
	 */
	public static Stream<Integer> intS(final int n) {
		return Stream.iterate(0, i -> i + 1).limit(n);
	}

	/**
	 * intA
	 * 
	 * @param n 长度
	 * @return 整型数组
	 */
	public static Integer[] intA(final int n) {
		return intS(n).toArray(Integer[]::new);
	}

	/**
	 * 创建指定长度的数组
	 * 
	 * @param tclass 类型类
	 * @param n      数组长度
	 * @param <T>    元素类型
	 * @return T类型的数组
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(final Class<T> tclass, final int n) {
		return (T[]) Array.newInstance(tclass, n);
	}

	/**
	 * 矩阵归集器
	 * 
	 * @param <T>  元素类型
	 * @param ncol 列数,大于0的整数
	 * @param nrow 行数木,大于0的整数
	 * @return 矩阵归集器
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collector<T, ?, Matrix<T>> mxclc(final int nrow, final int ncol) {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, ee -> {
			return new Matrix<T>((List<T>) ee, nrow, ncol);
		});
	}

	/**
	 * rmxclc
	 * 
	 * @param <T> 元素类型
	 * @return Collector
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collector<T[], ?, Matrix<T>> rmxclc() {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, ee -> {
			final Integer ncol = ((T[]) ee.get(0)).length;
			final Integer nrow = ee.size();
			final List<T> data = (List<T>) ee.stream().flatMap(e -> Arrays.stream((T[]) e))
					.collect(Collectors.toList());
			return new Matrix<T>(data, nrow, ncol);
		});
	}

	/**
	 * 行归集器
	 *
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collector<Iterable<T>, ?, Matrix<T>> rmxclc2() {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, ee -> {
			final Iterable<T> tt = (Iterable<T>) (Object) ee.get(0);
			final int ncol = tt instanceof Collection ? ((Collection<T>) tt).size()
					: StreamSupport.stream(tt.spliterator(), false).collect(Collectors.toList()).size();
			return new Matrix<T>(
					(List<T>) ee.stream().flatMap(e -> StreamSupport.stream(((Iterable<T>) e).spliterator(), false))
							.collect(Collectors.toList()),
					ee.size(), ncol);
		});
	}

	/**
	 * 列归集器
	 *
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collector<T[], ?, Matrix<T>> cmxclc() {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, ee -> {
			final Integer ncol = ((T[]) ee.get(0)).length;
			final List<T> data = (List<T>) ee.stream().flatMap(e -> Arrays.stream((T[]) e))
					.collect(Collectors.toList());
			return new Matrix<T>(data, ee.size(), ncol).transpose();
		});
	}

	/**
	 * 列归集器
	 *
	 * @param <T> 元素类型
	 * @return 矩阵归集器
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collector<Iterable<T>, ?, Matrix<T>> cmxclc2() {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, ee -> {
			final Iterable<T> tt = (Iterable<T>) (Object) ee.get(0);
			final int ncol = tt instanceof Collection ? ((Collection<T>) tt).size()
					: StreamSupport.stream(tt.spliterator(), false).collect(Collectors.toList()).size();
			return new Matrix<T>(
					(List<T>) ee.stream().flatMap(e -> StreamSupport.stream(((Iterable<T>) e).spliterator(), false))
							.collect(Collectors.toList()),
					ee.size(), ncol).transpose();
		});
	}

	/**
	 * 列归集器
	 * 
	 * @param <T> 元素类型
	 * @return 矩阵归集器
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collector<T, ?, Matrix<T>> mxclc() {
		return Collector.of(ArrayList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, data -> Matrix.of((List<T>) data));
	}

	/**
	 * 矩阵二元运算
	 *
	 * @param <X>  1号矩阵
	 * @param <Y>  2号矩阵
	 * @param <Z>  结果矩阵
	 * @param biop 二元运算 (x,y)->z
	 * @return (mx, my)->mz
	 */
	public static <X, Y, Z> BiFunction<Matrix<X>, Matrix<Y>, Matrix<Z>> binaryFun(final BiFunction<X, Y, Z> biop) {
		return (mx, my) -> {
			final int ncol = Math.max(mx.ncol(), my.ncol());
			final int nrow = Math.max(mx.nrow(), my.nrow());
			return new Matrix<>(Tensor.bifun(biop).apply(mx, my).data(), nrow, ncol);
		};
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 Optional.empty
	 *
	 * @param <T> 函数的参数类型
	 * @param t   参数值
	 * @return Optional T
	 */
	public static <T> Optional<Double> obj2dblOpt(final T t) {
		return Optional.ofNullable(obj2dbl(null).apply(t));
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 null
	 *
	 * @param <T> 函数的参数类型
	 * @return t-&gt;dbl
	 */
	public static <T> Function<T, Double> obj2dbl() {
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
	public static <T> Function<T, Double> obj2dbl(final Number defaultValue) {
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
	 * @return t-&gt;dbl
	 */
	public static <T> Function<T, Double> obj2dbl(final Number defaultValue, final boolean timeflag) {
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
				}
			} // try

			return dbl;
		};
	}

	/**
	 * @param <T> 元素类型
	 * @param ts  元素列表
	 * @return defaultValue-&gt;diag
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <T> Function<T, Matrix<T>> diagonal(final T... ts) {
		if (ts == null)
			return null;
		final int n = ts.length;
		final Class<T> tclass = (Class<T>) ts.getClass().getComponentType();
		final T[] tt = (T[]) Array.newInstance(tclass, n * n);
		return t -> {
			Arrays.fill(tt, t);
			Stream.iterate(0, i -> i + 1).limit(n) //
					.forEach(i -> tt[i * (n + 1)] = ts[i]);
			return Matrix.of(tt, n, n);
		};
	}

	/**
	 * 对角矩阵
	 * 
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return defaultValue->diag
	 */
	@SafeVarargs
	public static <T> Matrix<T> diag(final T... ts) {
		return diagonal(ts).apply(null);
	}

	/**
	 * I 矩阵
	 *
	 * @param n 方阵维度
	 * @return I 矩阵
	 */
	public static Matrix<Double> eye(final int n) {
		return diagonal(Stream.iterate(1, i -> i + 1).limit(n).map(e -> 1d).toArray(Double[]::new)).apply(0d);
	}

	/**
	 * 设置 变换矩阵
	 *
	 * @param n 矩阵维度
	 * @return Lij 矩阵
	 */
	public static TriFunction<Number, Number, Number, Matrix<Double>> Lij(final int n) {
		return (i, j, number) -> eye(n).cell(i.intValue(), j.intValue(), number.doubleValue());
	}

	/**
	 * 一维数组二维化访问
	 *
	 * @param <T>  元素类型
	 * @param ts   一维数组
	 * @param ncol 列宽度
	 * @return bireader
	 */
	public static <T> BiFunction<Integer, Integer, T> bireader(final T[] ts, final int ncol) {
		return (i, j) -> ts[i * ncol + j];
	}

	/**
	 * 一维索引转二维索引
	 *
	 * @param ncol 列宽度
	 * @return (index / ncol, index % ncol)
	 */
	public static Function<Integer, Tuple2<Integer, Integer>> mod(int ncol) {
		return (index) -> Tuple2.of(index / ncol, index % ncol);
	}
}
