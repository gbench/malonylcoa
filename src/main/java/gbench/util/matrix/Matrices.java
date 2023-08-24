package gbench.util.matrix;

import static gbench.util.array.INdarray.dot;
import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nds;
import static gbench.util.type.Streams.intS;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.array.IPoint;
import gbench.util.function.Getter;
import gbench.util.function.TriFunction;
import gbench.util.lisp.IRecord;
import gbench.util.lisp.Tuple2;

/**
 * 矩阵主要用于数值计算
 *
 * @param <T> 元素类型
 */
public class Matrices<T> {

	/**
	 * Matrices
	 * 
	 * @param matrix 矩阵
	 */
	public Matrices(final Matrix<T> matrix) {
		this.matrix = matrix;
	}

	/**
	 * mmult
	 * 
	 * @param mu  右边乘矩阵
	 * @param <U> 矩阵元素类型
	 * @return Matrices
	 */
	public <U> Matrices<Double> mmult(final Matrix<U> mu) {
		return Matrices.mmult(this.matrix, mu);
	}

	/**
	 * mmult
	 * 
	 * @param matrices 右乘元素
	 * @param <U>      院所类型
	 * @return Matrices
	 */
	public <U> Matrices<Double> mmult(final Matrices<U> matrices) {
		return Matrices.mmult(this.matrix, matrices.matrix);
	}

	/**
	 * 交换 i,j 两行数据
	 *
	 * @param i 行索引从0开始
	 * @param j 列索引从0开始
	 * @return 交换后的矩阵
	 */
	public Matrices<T> swap(final int i, final int j) {
		return this.matrix.swap(i, j).toMatrices();
	}

	/**
	 * 交换 i 与 j 行
	 *
	 * @param i    行索引 从0开始
	 * @param j    行索引 从0开始
	 * @param flag 是否修改矩阵本身,true 修改本身,false 保持源矩阵不变，返回交换行后的复制品矩阵
	 * @return i 与 j 交换后的矩阵
	 */
	public Matrices<T> swap(final int i, final int j, final boolean flag) {
		return this.matrix.swap(i, j, flag).toMatrices();
	}

	/**
	 * 交换 i,j 两列
	 *
	 * @param i 列索引从0开始
	 * @param j 列索引从0开始
	 * @return 交换后的矩阵
	 */
	public Matrices<T> swap2(final int i, final int j) {
		return this.matrix.swap2(i, j).toMatrices();
	}

	/**
	 * 交换 i 与 j 列
	 *
	 * @param i    行索引 从0开始
	 * @param j    行索引 从0开始
	 * @param flag 是否修改矩阵本身,true 修改本身,false 保持源矩阵不变，返回交换行后的复制品矩阵
	 * @return i 与 j 交换后的矩阵
	 */
	public Matrices<T> swap2(final int i, final int j, final boolean flag) {
		return this.matrix.swap2(i, j, flag).toMatrices();
	}

	/**
	 * 复制矩阵
	 *
	 * @return 复制矩阵
	 */
	public Matrices<T> duplicate() {
		return this.duplicatex().toMatrices();
	}

	/**
	 * 复制矩阵
	 *
	 * @return 复制矩阵
	 */
	public Matrix<T> duplicatex() {
		return this.matrix.duplicate();
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param i      列索引从0开始
	 * @param mapper t->u 元素变换
	 * @return U类型的列表
	 */
	public <U> List<U> cols(final int i, final Function<T, U> mapper) {
		return this.matrix.colS(i, mapper).collect(Collectors.toList());
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param i      列索引从0开始
	 * @param mapper t->u 元素变换
	 * @return U类型的流
	 */
	public <U> Stream<U> colS(final int i, final Function<T, U> mapper) {
		return this.matrix.colS(i, mapper);
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param mapper t->u 列变换函数
	 * @return U类型的列表
	 */
	public <U> Stream<U> colS(final Function<T[], U> mapper) {
		return this.matrix.columnS().map(mapper);
	}

	/**
	 * 行向量
	 *
	 * @param <U> 结果向量元素类型
	 * @return U类型的列表
	 */
	public <U> Stream<List<T>> colS() {
		return this.matrix.columnS().map(Arrays::asList);
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param mapper t->u 列变换函数
	 * @return U类型的列表
	 */
	public <U> List<U> cols(final Function<T[], U> mapper) {
		return this.matrix.columnS().map(mapper).collect(Collectors.toList());
	}

	/**
	 * Matrices 类型
	 *
	 * @param i 行索引 从0开始
	 * @return Matrix
	 */
	public Matrices<T> vrow(final int i) {
		return this.vrowx(i).toMatrices();
	}

	/**
	 * Matrices 类型
	 *
	 * @param i 行索引 从0开始
	 * @return Matrix
	 */
	public Matrices<T> vcol(final int i) {
		return this.vcolx(i).toMatrices();
	}

	/**
	 * Matrix 类型
	 *
	 * @param i 行索引 从0开始
	 * @return Matrix
	 */
	public Matrix<T> vrowx(final int i) {
		return this.matrix.vrow(i);
	}

	/**
	 * INdarray 数据行
	 *
	 * @param i 行索引 从0开始
	 * @return Matrix
	 */
	public INdarray<T> ndrow(final int i) {
		return this.matrix.ndrow(i);
	}

	/**
	 * vcolx
	 * 
	 * @param i 行索引 从0开始
	 * @return Matrix
	 */
	public Matrix<T> vcolx(final int i) {
		return this.matrix.vcol(i);
	}

	/**
	 * INdarray 数据列
	 *
	 * @param j 列索引 从0开始
	 * @return INdarray
	 */
	public INdarray<T> ndcol(final int j) {
		return this.matrix.ndcol(j);
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param i      行索引从0开始
	 * @param mapper t->u 列变换函数
	 * @return U类型的列表
	 */
	public <U> List<U> rows(final int i, final Function<T, U> mapper) {
		return this.matrix.rowS(i, mapper).collect(Collectors.toList());
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param mapper t->u 列变换函数
	 * @return U类型的列表
	 */
	public <U> List<U> rows(final Function<T[], U> mapper) {
		return this.matrix.rowS().map(mapper).collect(Collectors.toList());
	}

	/**
	 * 列向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param i      行索引从0开始
	 * @param mapper t->u 列变换函数
	 * @return U类型的流
	 */
	public <U> Stream<U> rowS(final int i, final Function<T, U> mapper) {
		return this.matrix.rowS(i, mapper);
	}

	/**
	 * 行向量
	 *
	 * @param <U>    结果向量元素类型
	 * @param mapper t->u 列变换函数
	 * @return U类型的列表
	 */
	public <U> Stream<U> rowS(final Function<T[], U> mapper) {
		return this.matrix.rowS().map(mapper);
	}

	/**
	 * 行向量
	 *
	 * @return U类型的列表
	 */
	public Stream<List<T>> rowS() {
		return this.matrix.rowS().map(Arrays::asList);
	}

	/**
	 * 矩阵元素类型变换
	 *
	 * @param <U>    结果元素类型
	 * @param mapper 映射函数
	 * @return U矩阵
	 */
	public <U> Matrices<U> fmap(final BiFunction<Tuple2<Integer, Integer>, T, U> mapper) {
		return this.matrix.fmap(mapper).toMatrices();
	}

	/**
	 * 矩阵数据
	 *
	 * @return 矩阵数据
	 */
	public T[] data() {
		return this.matrix.data();
	}

	/**
	 * 提取矩阵元素值
	 *
	 * @param i 行索引从0开始
	 * @param j 列索引从0开始
	 * @return 矩阵元素值
	 */
	public T get(final int i, final int j) {
		return this.matrix.cell(i, j);
	}

	/**
	 * 设置元素值
	 *
	 * @param i 行索引
	 * @param j 列索引
	 * @param t 设置元素
	 * @return Matrices
	 */
	public Matrices<T> set(final int i, final int j, final T t) {
		return new Matrices<>(matrix.cell(i, j, t));
	}

	/**
	 * 矩阵形状
	 *
	 * @return 行数, 列数
	 */
	public int[] shape() {
		return this.matrix.shape();
	}

	/**
	 * 矩阵数据长度
	 * 
	 * @return 矩阵数据长度
	 */
	public int length() {
		return this.toMatrix().length();
	}

	/**
	 * 矩阵元素列表
	 *
	 * @return (( 行索引 ： 从0开始, 列索引从0开始), 矩阵元素)
	 */
	public Stream<Tuple2<Tuple2<Integer, Integer>, T>> entrieS() {
		final int ncol = this.matrix.ncol();
		return this.matrix.stream(Tuple2.snb(0)).map(e -> e.fmap1(Matrix.mod(ncol)));
	}

	/**
	 * 矩阵元素列表
	 *
	 * @param <U>    流类型
	 * @param mapper 元素预处理函数 ((行索引：从0开始,列索引从0开始),矩阵元素) -> u
	 * @return (( 行索引 ： 从0开始, 列索引从0开始), 矩阵元素)
	 */
	public <U> Stream<U> entrieS(final Function<Tuple2<Tuple2<Integer, Integer>, T>, U> mapper) {
		final int ncol = this.matrix.ncol();
		return this.matrix.stream(Tuple2.snb(0)).map(e -> e.fmap1(Matrix.mod(ncol))).map(mapper);
	}

	/**
	 * INdarray
	 * 
	 * @return INdarray
	 */
	public INdarray<T> nd() {
		return this.matrix.nd();
	}

	/**
	 * 矩阵转置
	 *
	 * @return Matrices
	 */
	public Matrices<T> transpose() {
		return this.matrix.transpose().toMatrices();
	}

	/**
	 * matrix
	 *
	 * @return matrix
	 */
	public Matrix<T> toMatrix() {
		return this.matrix;
	}

	/**
	 * @return 格式化字符串
	 */
	public String toString() {
		final Pattern number_pattern = Pattern.compile("^\\s*((\\+|-)?(\\d+))\\s*(\\.\\d+)?\\s*$");
		return this.matrix.toString(o -> {
			final String s = String.valueOf(o);
			final Matcher number_matcher = number_pattern.matcher(s);
			if (number_matcher.matches()) { // 数字格式
				if (Optional.ofNullable(number_matcher.group(4)) //
						.map(e -> e.matches("^\\.\\s*0+\\s*$")) //
						.orElse(false)) {
					return number_matcher.group(1);
				} else if (Optional.ofNullable(number_matcher.group(4)).map(e -> e.length() > 3).orElse(false)) {
					return String.format("%s%s", number_matcher.group(1), number_matcher.group(4).substring(0, 4));
				} // if
			} // if
			return s;
		});
	}

	/**
	 * 矩阵乘法
	 *
	 * @param mt  右侧矩阵
	 * @param mu  右侧矩阵
	 * @param <T> 左侧矩阵的类型
	 * @param <U> 右侧矩阵的类型
	 * @return Matrices
	 */
	public static <T, U> Matrices<Double> MMULT(final Matrices<T> mt, final Matrices<U> mu) {
		return mt.mmult(mu);
	}

	/**
	 * 矩阵乘法
	 *
	 * @param mt  右侧矩阵
	 * @param mu  右侧矩阵
	 * @param <T> 左侧矩阵的类型
	 * @param <U> 右侧矩阵的类型
	 * @return Matrices
	 */
	public static <T, U> Matrices<Double> mmult(final Matrix<T> mt, final Matrix<U> mu) {
		final Matrix<Double> m = mt.mmult(mu, (x, y) -> Matrix.obj2dblOpt(x) //
				.flatMap(_x -> Matrix.obj2dblOpt(y).map(_y -> _y * _x)) //
				.orElse(0d), //
				Matrix.plus_clc());
		return m.toMatrices();
	}

	/**
	 * 单位矩阵
	 *
	 * @param n 方阵长度
	 * @return 单位矩阵
	 */
	public static Matrices<Double> eye(final int n) {
		return Matrix.eye(n).toMatrices();
	}

	/**
	 * 单位矩阵
	 * 
	 * @param <T> 矩阵元素类型
	 * @param ts  方阵长度
	 * @return 单位矩阵
	 */
	@SafeVarargs
	public static <T> Matrices<T> diag(final T... ts) {
		return Matrix.diag(ts).toMatrices();
	}

	/**
	 * PA=LU分解
	 *
	 * @param mx 源矩阵:{L,U,P}
	 * @return {L,U,P}
	 */
	public static IRecord lu(final Matrix<? extends Number> mx) {
		return lu(mx.toMatrices());
	}

	/**
	 * PA=LU分解
	 *
	 * @param ms 源矩阵:{L,U,P}
	 * @return {L,U,P}
	 */
	public static IRecord lu(final Matrices<? extends Number> ms) {
		final int[] shape = ms.shape(); // 矩阵尺寸
		final int n = Math.min(shape[0], shape[1]); // 矩阵尺寸
		final Matrices<Double> p = Matrices.eye(n); // p 矩阵
		final Matrices<Double> m = ms.fmap((index, t) -> Optional.of(t) //
				.map(Number::doubleValue).orElse(0d)); // 源矩阵

		for (int k = 0; k < n - 1; k++) { // 依次获取第k列最大值的行索引,按照 主对角线 方向,从1直到倒数第二行,最后一行没有必要处理。对角线索引
			final Tuple2<Double, Integer> pivot = m.colS(k, Tuple2.snb2(0)).skip(k).reduce(Tuple2::max).get();
			p.swap(k, pivot._2, true); // 调整行，记录pivot行位于k
			m.swap(k, pivot._2, true); // 调整行，k行为pivot行
			for (int i = k + 1; i < n; i++) { // 逐行处理, 从 k+1 开始, mik 元素为pivot元素。
				m.set(i, k, m.get(i, k) / m.get(k, k)); // 乘数 mik,k 行就是 pivot 行
				for (int j = k + 1; j < n; j++) { // 书写列
					m.set(i, j, m.get(i, j) - m.get(i, k) * m.get(k, j)); // 从i行 mij 减去 mik 倍的 pivot行即 mkj
				} // for j 列索引
			} // i 行索引
		} // k 对角线索引

		final Matrices<Double> l = m
				.fmap((index, v) -> index._1 > index._2 ? v : Objects.equals(index._1, index._2) ? 1 : 0d);
		final Matrices<Double> u = m.fmap((index, v) -> index._1 <= index._2 ? v : 0);

		return IRecord.REC("L", l, "U", u, "P", p);
	}

	/**
	 * Ax=b的PA=LU方法求解
	 *
	 * @param A 系数矩阵
	 * @param b 值向量
	 * @return 方程组的根向量
	 */
	public static INdarray<Double> solve(final Matrices<? extends Number> A, final Matrices<? extends Number> b) {
		final IRecord palu = Matrices.lu(A);
		final Matrices<Double> p = palu.get(dblx("P"));
		final Matrices<Double> l = palu.get(dblx(0));
		final Matrices<Double> u = palu.get(dblx("U"));
		final Matrices<Double> pb = MMULT(p, b); // Pb 相乘
		final INdarray<Double> c = pb.duplicatex().nd();
		final int n = c.length(); // 结果向量维度

		// 一次规约 Lc=Pb 获得 c 向量
		Stream.iterate(1, i -> i < n, i -> i + 1).forEach(i -> { // 从1开始
			c.set(i, c.get(i) - dot(l.ndrow(i).head(i), c.head(i)));
		});
		// 二次规约 Ux = c
		Stream.iterate(n - 1, i -> i >= 0, i -> i - 1).forEach(i -> { // 从n-1开始，最后一行
			final int j = (n - 1) - i; // 与最后一行之间的距离,与对角线上元素之间的距离。即 已经计算完成的x值的个数。
			final INdarray<Double> irow = u.ndrow(i);
			final Double coef = irow.get(i); // 对角线上的元素
			c.set(i, (c.get(i) - dot(irow.tail(j), c.tail(j))) / coef);
		});
		return c;
	}

	/**
	 * PA=LU分解
	 *
	 * @param ms 源矩阵
	 * @return PA=LU分解:{L,U,P}
	 */
	public static IRecord qr(final Matrices<Double> ms) {
		return qr(ms.toMatrix());
	}

	/**
	 * QR 分解
	 *
	 * @param ms 数据矩阵
	 * @return QR 分解
	 */
	public static IRecord qr(final Matrix<Double> ms) {
		final int m = ms.nrow();
		final int n = ms.ncol();
		final int mod = n; // 列数量

		final INdarray<Double> data = ms.nd();
		final INdarray<Double> q = INdarray.nd(i -> 0, m * n).dbls(); // 表示正交方向:最多n个由data列向量定义上限个数
		final INdarray<Double> r = INdarray.nd(i -> 0, n * n).dbls(); // r 表示data列项量(n个)在q方向上(最多n个)的投影,因此是一个 nxn 矩阵
		final Getter<Integer, INdarray<IPoint<Double>>> qq = q.cols(mod);
		final Getter<Integer, INdarray<IPoint<Double>>> rr = r.cols(mod);
		final Getter<Integer, INdarray<IPoint<Double>>> aa = data.cols(mod);

		// q0,r0 的设置
		qq.get(0).assign(nds(aa.get(0).normalize())); // 使用第一条向量初始化
		rr.get(0).get(0).set(aa.get(0).norm()); // 初始化r0,0的一个元素

		intS(1, n).forEach(i -> { // 当前执行位置:i表示正在计算的向量
			final INdarray<Double> ai = aa.get(i).dbls();
			final INdarray<IPoint<Double>> ri = rr.get(i);
			final INdarray<Integer> js = nats(i); // 已经完成的向量索引j的集合[0,i)
			// j表示已经计算完成的向量。
			js.forEach(j -> ri.get(j).set(ai.proj(qq.get(j))));
			// _qi 剔除 投影向量 后 剩余的部分, 这是还没有归一化
			final INdarray<Double> _qi = ai.subacc(qq.lis((j, _q) -> _q.mul(ri.get(j)), js));
			// qi向量 与 ri投影的设置
			qq.get(i).assign(nds(_qi.normalize())); // 归一化向量:q向量生成
			ri.get(i).set(_qi.norm()); // 记录ri在_qi上的投影,r元素生成
		}); // forEach

		return IRecord.REC("Q", Matrix.of(q, m, n).toMatrices(), //
				"R", Matrix.of(r, m, n).toMatrices());
	}

	/**
	 * 设置 变换矩阵
	 *
	 * @param n 矩阵维度
	 * @return Lij 矩阵
	 */
	public static TriFunction<Number, Number, Number, Matrices<Double>> Lij(final int n) {
		return (i, j, number) -> Matrix.Lij(n).apply(i, j, number).toMatrices();
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
	public static <X, Y, Z> BiFunction<Matrices<X>, Matrices<Y>, Matrices<Z>> binaryFun(
			final BiFunction<X, Y, Z> biop) {
		return (mx, my) -> Matrix.binaryFun(biop).apply(mx.matrix, my.matrix).toMatrices();
	}

	/**
	 * 矩阵二元运算
	 *
	 * @param <X>  1号矩阵
	 * @param biop 二元运算 (x,y)->z
	 * @return (mx, my)->mz
	 */
	public static <X> BinaryOperator<Matrices<X>> binaryOp(final BinaryOperator<X> biop) {
		return (mx, my) -> Matrices.binaryFun(biop).apply(mx, my);
	}

	/**
	 * 加法 采用R模式的索引循环访问，获取最大的矩阵尺寸
	 *
	 * @param <X> 参数类型
	 * @return 加法函数
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Number> BinaryOperator<Matrices<X>> add() {
		return binaryOp((x, y) -> (X) (Number) ((x == null ? 0 : x.doubleValue()) + (y == null ? 0 : y.doubleValue())));
	}

	/**
	 * 减法 采用R模式的索引循环访问，获取最大的矩阵尺寸
	 *
	 * @param <X> 元素类型
	 * @return 减法函数
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Number> BinaryOperator<Matrices<X>> subtract() {
		return binaryOp((x, y) -> (X) (Number) ((x == null ? 0 : x.doubleValue()) - (y == null ? 0 : y.doubleValue())));
	}

	/**
	 * 乘法 采用R模式的索引循环访问，获取最大的矩阵尺寸
	 *
	 * @param <X> 参数类型
	 * @return 乘法函数
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Number> BinaryOperator<Matrices<X>> multiply() {
		return binaryOp((x, y) -> (X) (Number) ((x == null ? 0 : x.doubleValue()) * (y == null ? 0 : y.doubleValue())));
	}

	/**
	 * 除法 采用R模式的索引循环访问，获取最大的矩阵尺寸
	 *
	 * @param <X> 参数类型
	 * @return 除法函数
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Number> BinaryOperator<Matrices<X>> divide() {
		return binaryOp((x, y) -> (X) (Number) ((x == null ? 0 : x.doubleValue()) / (y == null ? 0 : y.doubleValue())));
	}

	/**
	 * 除法 采用R模式的索引循环访问，获取最大的矩阵尺寸
	 *
	 * @param <X> 参数类型
	 * @return 矩阵函数
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Number> BinaryOperator<Matrices<X>> mmult() {
		return (mx, my) -> (Matrices<X>) mx.mmult(my);
	}

	/**
	 * 生成列向量:[0,1,...]
	 *
	 * @param nrow 行数
	 * @return 数据矩阵
	 */
	public static Matrices<Integer> V(final int nrow) {
		return Matrix.V(nrow).toMatrices();
	}

	/**
	 * 生成列向量
	 *
	 * @param <T> 元素类型
	 * @param ts 行数
	 * @return 数据矩阵
	 */
	@SafeVarargs
	public static <T> Matrices<T> V(final T... ts) {
		return Matrix.V(ts).toMatrices();
	}

	/**
	 * 生成行向量:[0,1,...]
	 *
	 * @param nrow 行数
	 * @return 数据矩阵
	 */
	public static Matrices<Integer> VT(final int nrow) {
		return Matrix.VT(nrow).toMatrices();
	}

	/**
	 * 生成行向量:[0,1,...]
	 *
	 * @param <T> 元素类型
	 * @param ts  行数
	 * @return 数据矩阵
	 */
	@SafeVarargs
	public static <T> Matrices<T> VT(final T... ts) {
		return Matrix.VT(ts).toMatrices();
	}

	/**
	 * 创建矩阵
	 * 
	 * @param <T>  元素类型
	 * @param data 矩阵数据
	 * @param nrow 行数
	 * @param ncol 列数
	 * @return 矩阵
	 */
	public static <T> Matrices<T> of(final T[] data, final int nrow, final int ncol) {
		return Matrix.of(data, nrow, ncol).toMatrices();
	}

	/**
	 * 矩阵生成函数
	 * 
	 * @param <T>  元素类型
	 * @param data 矩阵元素
	 * @return 矩阵对象
	 */
	public static <T> Matrices<T> of(final T[] data) {
		return Matrix.of(data).toMatrices();
	}

	/**
	 * double matrix
	 *
	 * @param key 键名
	 * @return rec->dblx
	 */
	@SuppressWarnings("unchecked")
	public static Function<IRecord, Matrices<Double>> dblx(final String key) {
		return rec -> (Matrices<Double>) rec.get(key);
	}

	/**
	 * double matrix
	 *
	 * @param index 键名索引从0开始
	 * @return rec-&gt;dblx
	 */
	@SuppressWarnings("unchecked")
	public static Function<IRecord, Matrices<Double>> dblx(final int index) {
		return rec -> (Matrices<Double>) rec.get(index);
	}

	private final Matrix<T> matrix;
}
