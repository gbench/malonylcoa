package gbench.util.array;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.array.INdarray.nds;
import static gbench.util.type.Streams.intS;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import gbench.util.function.Getter;
import gbench.util.function.TriFunction;
import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;

/**
 * 
 * INdarray matriX 或者 是 INdarray eXtended 扩展化的INdarray的意思
 * 
 * @author gbench
 *
 * @param <T> 袁术类型
 */
public interface INdarrayX<T> extends INdarray<T> {

	/**
	 * 基础属性：元数据信息
	 *
	 * @return 元数据存储
	 */
	LinkedHashMap<String, Object> metas();

	/**
	 * 写入元数据: 设置参数
	 *
	 * @param key   键名
	 * @param value 键值
	 * @return INdarrayX this
	 */
	default INdarrayX<T> param(final String key, final Object value) {
		metas().put(key, value);
		return INdarrayX.this;
	}

	/**
	 * 写入元数据：读取参数
	 *
	 * @param <V> 结果类型
	 * @param key 键名
	 * @return 读取指定键值的属性值
	 */
	@SuppressWarnings("unchecked")
	default <V> V param(final String key) {
		return (V) metas().get(key);
	}

	/**
	 * 返回取模长度，这是矩阵的行长度，也是周期与进制计算的基本属性。
	 *
	 * @return 取模长度
	 */
	default int mod() {
		return (Integer) this.metas().computeIfAbsent(MetaKey.mod.toString(), e -> 1);
	}

	/**
	 * 设置行长度 返回取模长度，<br>
	 * 这是矩阵的行长度，也是周期与进制计算的基本属性。
	 *
	 * @param mod 参数取模
	 * @return nx 对象本身，便于实现链式编程
	 */
	default INdarrayX<T> mod(final int mod) {
		metas().put(MetaKey.mod.toString(), mod);
		return this;
	}

	/**
	 * get Element <br>
	 * 矩阵元素读取 <br>
	 * i行j列所对应的数据元素
	 *
	 * @param i 行索引编号,从0开始
	 * @param j 列索引编号,从0开始
	 * @return i行j列所对应的数据元素
	 */
	default T get(final int i, final int j) {
		return this.row(i).get(j);
	}

	/**
	 * set Element <br>
	 * 矩阵元素设置 <br>
	 * i行j列所对应的数据元素
	 *
	 * @param i 行索引编号,从0开始
	 * @param j 列索引编号,从0开始
	 * @param t 值对象
	 * @return INdarrayX this 对象本身，方便实现链式编程
	 */
	default INdarrayX<T> set(final int i, final int j, final T t) {
		this.row(i).set(j, t);
		return this;
	}

	/**
	 * 行数量:大于等于1的正整数
	 *
	 * @return 行数量
	 */
	default int nrow() {
		final int mod = this.mod();
		return mod < 1 ? 0 : this.length() / mod;
	}

	/**
	 * 列数量
	 *
	 * @return 列数量
	 */
	default int ncol() {
		return this.mod();
	}

	/**
	 * 矩阵形状
	 *
	 * @return [0:行数,1:列数]
	 */
	default int[] shape() {
		return new int[] { this.nrow(), this.ncol() };
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定行方法，可视为动态的行集合。以时间表示空间的思想
	 *
	 * @return 行数据流
	 */
	default Stream<INdarray<T>> rowS() {
		return this.rowS(mod());
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定行方法，可视为动态的行集合。以时间表示空间的思想
	 *
	 * @return (i:行号索引,从0开始i)->row
	 */
	default Getter<Integer, INdarray<T>> rows() {
		return this.rows(mod());
	}

	/**
	 * 矩阵的第i行数据
	 *
	 * @param i 行索引编号,从0开始
	 * @return 第i行数据
	 */
	default INdarray<T> row(final int i) {
		return this.rows(mod()).get(i);
	}

	/**
	 * 将ndarray视为一个行长为mod的二维矩阵，给予提取指定行方法，可视为动态的行集合。以时间表示空间的思想
	 *
	 * @return 行数据流
	 */
	default Stream<INdarray<IPoint<T>>> colS() {
		return this.colS(mod());
	}

	/**
	 * (i:行号索引,从0开始i)->row
	 *
	 * @return (i:列号索引,从0开始i)->col
	 */
	default Getter<Integer, INdarray<IPoint<T>>> cols() {
		return this.cols(mod());
	}

	/**
	 * 矩阵的第i列数据
	 *
	 * @param j 列索引编号,从0开始
	 * @return 第i列数据
	 */
	default INdarray<IPoint<T>> col(final int j) {
		return this.col(mod(), j);
	}

	/**
	 * functor 来源于 haskell 的思想
	 * 
	 * @param <U> 目标类型
	 * @param tf  (i:行索引从0开始,j:列索引从0开始,t:元素)->u
	 * @return U类型的数据
	 */
	default <U> INdarrayX<U> fmap(final TriFunction<Integer, Integer, T, U> tf) {
		final int n = this.ncol();
		return this.fmap((index, t) -> {
			final int i = index / n;
			final int j = index % n;
			return tf.apply(i, j, t);
		}).nx(mod());
	}

	/**
	 * 转换成 nx 对象
	 *
	 * @param <ND> INdarray 类型
	 * @param <U>  元数据类型
	 * @param nd   元数据矩阵
	 * @param mod  行宽度，列数
	 * @return INdarrayX
	 */
	default <U, ND extends INdarray<U>> INdarrayX<U> asNx(final ND nd, final Integer mod) {
		return nd.nx(MetaKey.mod, Optional.ofNullable(mod).orElse(this.mod()));
	}

	/**
	 * 转换成 nx 对象
	 *
	 * @param <ND> INdarray 类型
	 * @param <U>  元数据类型
	 * @param nd   元数据矩阵
	 * @return INdarrayX
	 */
	default <U, ND extends INdarray<U>> INdarrayX<U> asNx(final ND nd) {
		return this.asNx(nd, null);
	}

	/**
	 * 复制 INdarray
	 *
	 * @return 复制品,进行data复制,只复制[start,end) 之间的数据空间。
	 */
	@Override
	default INdarrayX<T> dupdata() {
		final INdarray<T> nd = this.arrayOf(data -> this.create(data, 0, this.length()));
		return this.asNx(nd);
	}

	/**
	 * 转换成 数据矩阵
	 * 
	 * @return 数据矩阵
	 */
	default INdarrayX<Double> dblx() {
		return this.asNx(this.dbls(), null);
	}

	/**
	 * 矩阵转置
	 *
	 * @return 转置后的数据矩阵：保持对源数矩阵的引用
	 */
	default Stream<IPoint<T>> transposeS() {
		return this.colS(mod()).flatMap(INdarray::stream);
	}

	/**
	 * 矩阵转置
	 *
	 * @return 转置后的数据矩阵：保持对源数矩阵的引用
	 */
	default INdarrayX<IPoint<T>> transpose() {
		return this.transposeS().collect(INdarray.nxclc(this.nrow()));
	}

	/**
	 * 矩阵转置
	 *
	 * @return 转置后的数据矩阵：解除对源数矩阵的引用
	 */
	default INdarrayX<T> transpose2() {
		return this.transposeS().map(INdarray::get).collect(INdarray.nxclc(this.nrow()));
	}

	/**
	 * 交换两行
	 *
	 * @param i 行索引 从0开始
	 * @param j 行索引 从0开始
	 * @return i 与 j 交换后的矩阵
	 */
	default INdarrayX<T> swap(final int i, final int j) {
		final T[] data = this.data();
		final int ncol = this.ncol();
		final int istart = i * ncol;
		final int jstart = j * ncol;
		final T[] idata = Arrays.copyOfRange(data, istart, istart + ncol); // 保存原来的数据

		// i<->j两行相互交换
		System.arraycopy(data, jstart, data, istart, ncol);
		System.arraycopy(idata, 0, data, jstart, ncol);

		return this;
	}

	/**
	 * 交换两列
	 *
	 * @param i 列索引 从0开始
	 * @param j 列索引 从0开始
	 * @return i 与 j 交换后的矩阵
	 */
	default INdarray<T> swap2(final int i, final int j) {
		final INdarray<T> c = this.col(i).fflat();
		this.col(i).assign(nds(this.col(j).fflat()));
		this.col(j).assign(nds(c));
		return this;
	}

	/**
	 * 矩阵乘法
	 *
	 * @param <E>  元素类型
	 * @param <ND> 另一个ndarray 对象的类型
	 * @param nd   另一个ndarray 对象的类型
	 * @return 结果矩阵 Double类型的数字矩阵
	 */
	default <E, ND extends INdarray<E>> INdarrayX<Double> mmult(final ND nd) {
		final INdarray<Double> ndd = this.mmult(mod(), nd);
		return ndd.nx(nd.length() / mod());
	}

	/**
	 * 矩阵的QR分解 会改变：会改变当前 矩阵元素 <br>
	 * 
	 * @return PA=LU分解结果,(P:排序矩阵,(L:下三角矩阵,U:上三角矩阵))
	 */
	default Tuple2<INdarrayX<Double>, Tuple2<INdarrayX<Double>, INdarrayX<Double>>> lu() {
		final INdarrayX<Double> mx = this.dblx();
		final int n = Math.min(nrow(), ncol()); // 矩阵尺寸
		final INdarrayX<Double> px = INdarrayX.eyex(n);

		for (int k = 0; k < n - 1; k++) { // 依次获取第k列最大值的行索引,按照 主对角线 方向,从1直到倒数第二行,最后一行没有必要处理。对角线索引
			final Tuple2<Double, Integer> pivot = mx.col(k).stream(IPoint::get) //
					.map(Tuple2.snb2(0)).skip(k) // 剔除前k项，对剩余部分进行检索最大值作为pivot
					.reduce(Tuple2::max).orElse(null);
			assert Objects.nonNull(pivot) : String.format("pivot 出现空值！@ %d", k);
			px.swap(k, pivot._2); // 调整行，记录pivot行位于k
			mx.swap(k, pivot._2); // 调整行，k行为pivot行
			if (Objects.equals(mx.get(k, k), 0d)) { // continue是因为mx[k,k]天然为0就不必再继续调整了同时又保证mx[k,k]不为0的除法有意义
				continue;
			} // m[k,k]非零检测与保证
			for (int i = k + 1; i < n; i++) { // 逐行处理, 从 k+1 开始, mik 元素为pivot元素。
				mx.set(i, k, mx.get(i, k) / mx.get(k, k)); // 乘数 mik,k 行就是 pivot 行
				for (int j = k + 1; j < n; j++) { // 书写列
					mx.set(i, j, mx.get(i, j) - mx.get(i, k) * mx.get(k, j)); // 从i行 mij 减去 mik 倍的 pivot行即 mkj
				} // for j 列索引
			} // i 行索引
		} // k 对角线索引

		final INdarrayX<Double> lx = mx.fmap((i, j, e) -> i > j ? e : Objects.equals(i, j) ? 1d : 0d);
		final INdarrayX<Double> ux = mx.fmap((i, j, e) -> i > j ? 0d : e);

		return Tuple2.of(px, Tuple2.of(lx, ux));
	}

	/**
	 * 矩阵的QR分解
	 *
	 * @return QR分解的结果, (Q:正交矩阵,R:投影矩阵)
	 */
	default Tuple2<INdarrayX<Double>, INdarrayX<Double>> qr() {
		final int m = this.nrow(); // 行数
		final int n = this.ncol(); // 列数
		final INdarray<Double> data = this.dbls();
		final INdarray<Double> qs = INdarray.nd(i -> 0, m * n).dbls(); // 表示正交方向:最多n个由data列向量定义上限个数
		final INdarray<Double> rs = INdarray.nd(i -> 0, n * n).dbls(); // r 表示data列项量(n个)在q方向上(最多n个)的投影,因此是一个 nxn 矩阵
		final Getter<Integer, INdarray<IPoint<Double>>> qq = qs.cols(n);
		final Getter<Integer, INdarray<IPoint<Double>>> rr = rs.cols(n);
		final Getter<Integer, INdarray<IPoint<Double>>> aa = data.cols(n);

		// q0,r0 的设置
		qq.get(0).assign(nds(aa.get(0).normalize())); // 使用第一条向量初始化
		rr.get(0).get(0).set(aa.get(0).norm()); // 初始化r0,0的一个元素

		intS(1, n).forEach(i -> { // 当前执行位置:i表示正在计算的向量
			final INdarray<Double> ai = aa.get(i).dbls();
			final INdarray<IPoint<Double>> ri = rr.get(i);
			final INdarray<Integer> js = nats(i); // 已经完成的向量索引j的集合[0,i)
			// j表示已经计算完成的向量。
			js.forEach(j -> ri.get(j).set(ai.proj(qq.get(j))));
			// _qi 剔除 投影向量 后 剩余的部分, 这是还没有归一化的向量，注意：保存至qq的时候需要进行归一化。
			final INdarray<Double> _qi = ai.subacc(qq.lis((j, _q) -> _q.mul(ri.get(j)), js));
			// qi向量 与 ri投影的设置
			qq.get(i).assign(nds(_qi.normalize())); // 归一化向量:q向量生成
			ri.get(i).set(_qi.norm()); // 记录ri在_qi上的投影,r元素生成
		}); // forEach

		return Tuple2.of(qs.nx(n), rs.nx(n));
	}

	/**
	 * Ax=b的PA=LU方法求解 会改变alphas矩阵元素 <br>
	 *
	 * @param <B>  b 矩阵元素类型
	 * @param <ND> b 矩阵类型
	 * @param b    值向量
	 * @return 方程组的解向量
	 */
	default <B, ND extends INdarray<B>> INdarrayX<Double> solve(final ND b) {
		final INdarrayX<Double> alphas = this.dblx();
		final INdarrayX<Double> beta = this.asNx(b.dbls(), 1);
		return INdarrayX.solve(alphas, beta);
	}

	/**
	 * 矩阵行列式(使用det2的palu分解方法求行列式)
	 * 
	 * @return 矩阵行列式
	 */
	default Double det() {
		return INdarrayX.det2(this.dblx());
	}

	/**
	 * 矩阵行的伴随矩阵
	 * 
	 * @return 矩阵行的伴随矩阵
	 */
	default INdarrayX<Double> adj() {
		return INdarrayX.adjoints(this);
	}

	/**
	 * 矩阵的逆矩阵
	 * 
	 * @return 矩阵行列式
	 */
	default INdarrayX<Double> inv() {
		return this.asNx(this.adj().div(this.det()));
	}

	/**
	 * 提取对角线上的元素
	 * 
	 * @return 提取对角线上的元素:保持对源数据的引用
	 */
	default Stream<T> diagonalS() {
		final int mod1 = this.mod() + 1;
		final int n = this.length();
		return Stream.iterate(0, i -> i < n, i -> i + mod1).map(this::get);
	}

	/**
	 * 提取对角线上的元素
	 * 
	 * @return 提取对角线上的元素，解除对源数据的引用
	 */
	default INdarray<T> diagonal2() {
		return this.diagonalS().collect(INdarray.ndclc(this.ncol(), this.dtype()));
	}

	/**
	 * 提取对角线上的元素
	 * 
	 * @return 提取对角线上的元素:保持对源数据的引用
	 */
	default INdarray<IPoint<T>> diagonal() {
		return this.col(mod() + 1, 0);
	}

	/**
	 * 对角阵
	 *
	 * @param <T> 元素类型
	 * @param ts  对角阵的迹
	 * @return 对角阵
	 */
	@SafeVarargs
	static <T> INdarrayX<T> diagx(final T... ts) {
		return Optional.ofNullable(INdarray.diag(ts)) //
				.map(e -> e.nx(ts.length)) //
				.orElse(null);
	}

	/**
	 * 长度为n的单位矩阵
	 *
	 * @param n 矩阵的维度
	 * @return 对角阵
	 */
	static INdarrayX<Double> eyex(final int n) {
		final Double[] data = n < 1 ? new Double[] {} : INdarray.nd(i -> 1d, n).data();
		return diagx(data);
	}

	/**
	 * Ax=b的PA=LU方法求解 会改变alphas矩阵元素 <br>
	 *
	 * @param alphas 系数矩阵
	 * @param beta   值向量
	 * @return 方程组的解向量
	 */
	static INdarrayX<Double> solve(final INdarrayX<Double> alphas, final INdarrayX<Double> beta) {
		final Tuple2<INdarrayX<Double>, Tuple2<INdarrayX<Double>, INdarrayX<Double>>> palu = alphas.lu();
		final INdarrayX<Double> p = palu._1;
		final INdarrayX<Double> l = palu._2._1;
		final INdarrayX<Double> u = palu._2._2;
		final INdarrayX<Double> pb = p.mmult(beta);
		final INdarrayX<Double> c = pb;
		final int n = c.length(); // 结果向量维度

		// 一次规约 Lc=Pb 获得 c 向量
		Stream.iterate(1, i -> i < n, i -> i + 1).forEach(i -> { // 从1开始
			c.set(i, c.get(i) - INdarray.dot(l.row(i).head(i), c.head(i)));
		});

		// 二次规约 Ux = c
		Stream.iterate(n - 1, i -> i >= 0, i -> i - 1).forEach(i -> { // 从n-1开始，最后一行
			final int j = (n - 1) - i; // 与最后一行之间的距离,与对角线上元素之间的距离。即 已经计算完成的x值的个数。
			final INdarray<Double> irow = u.row(i);
			final Double coef = irow.get(i); // 对角线上的元素
			c.set(i, (c.get(i) - INdarray.dot(irow.tail(j), c.tail(j))) / coef);
		});

		return c;
	}

	/**
	 * 求一个矩阵的行列式：按照行展开方法(代数余子式)
	 * 
	 * @param nx 数据矩阵
	 * @return 矩阵行列式
	 */
	static Double det0(final INdarrayX<Double> nx) {
		final int mod = nx.mod();
		assert Objects.equals(nx.nrow(), nx.ncol()) : String.format("nx必须为方阵,nrow:%s,ncol:%s", nx.nrow(), nx.ncol());
		if (mod == 1)
			return nx.get();
		else {
			final BiFunction<Integer, Integer, INdarrayX<Double>> mij = minorNxFn(nx); // 余子式函数
			final Optional<Tuple2<Long, Integer>> zeromost = nx.rowS(mod) //
					.map(row -> -1 * row.filter(((Double) 0d)::equals).count()) //
					.map(Tuple2.snb2(0)).sorted().findFirst(); // 优化挑选
			final int i = zeromost.get()._2; // 寻找0最多的行
			return Stream.iterate(0, j -> j + 1).limit(mod).parallel() //
					.map(j -> ((i + j) % 2 == 0 ? 1 : -1) * Optional.of(nx.get(i, j)) //
							.map(e -> ((Double) 0d).equals(e) ? 0d : e * det0(mij.apply(i, j))).get()) //
					.reduce(0d, Double::sum);
		} // if
	}

	/**
	 * 求一个矩阵的行列式，逆序数方法:全排列
	 * 
	 * @param nx 数据矩阵
	 * @return 矩阵行列式
	 */
	static Double det1(final INdarrayX<Double> nx) {
		final int n = nx.ncol();
		return Lisp.permute(nats(n).data()).map(e -> {
			final double d = DoubleStream.iterate(0, i -> i + 1).limit(e.length) //
					.map(i -> nx.row((int) i).get(e[(int) i])).reduce(1d, (a, b) -> a * b);
			final double sign = Optional.of(nd(e).tau()).map(s -> s % 2 == 0 ? 1d : -1d).get();
			return sign * d;
		}).reduce(0d, Double::sum);
	}

	/**
	 * 求一个矩阵的行列式，palu方式
	 * 
	 * @param nx 数据矩阵
	 * @return 矩阵行列式
	 */
	static Double det2(final INdarrayX<Double> nx) {
		if (nx.length() < 1) { // 空矩阵
			return null;
		} else {
			final Tuple2<INdarrayX<Double>, Tuple2<INdarrayX<Double>, INdarrayX<Double>>> palu = nx.lu();
			final INdarrayX<Double> p = palu._1;
			final INdarrayX<Double> u = palu._2._2;
			final INdarray<Integer> pnd = p.rowS().map(INdarrayX::ohDecode)
					.collect(INdarray.ndclc(p.ncol(), Integer.class));
			final int ptau = pnd.tau();
			final double sign = ptau % 2 == 0 ? 1d : -1d;
			final Iterator<Double> itr = u.diagonalS().iterator(); // 对角线迭代器
			double d = sign; // 初始值符号

			while (itr.hasNext() && !Objects.equals(0d, d)) { // 累计惊醒对角线上的元素乘积,直至结束呼叫欧式结果为0
				d = d * itr.next();
			} // while

			return d;
		}
	}

	/**
	 * <a href=
	 * "https://handwiki.org/wiki/Minor_(linear_algebra)#cite_note-1">minor</a> <br>
	 * 余子式 生成函数 fisrtMinor
	 * <p>
	 * minor 矩阵 生成函数
	 *
	 * @param <T> 元素类型
	 * @param nx  矩阵
	 * @return 余子式生成函数(i, j)->余子式
	 */
	static <T> BiFunction<Integer, Integer, INdarrayX<T>> minorNxFn(final INdarrayX<T> nx) {
		final int mod = nx.mod();
		return (i, j) -> nx.stream(Tuple2.modulob(mod)).filter(e -> (i >= 0 && j >= 0) //
				&& (i < nx.nrow() && j < nx.ncol()) //
				&& (e._1._1 != i && e._1._2 != j) //
		).map(e -> e._2).collect(INdarray.nxclc(mod - 1));
	}

	/**
	 * 求一个矩阵的余子式
	 * 
	 * @param nx 数据矩阵
	 * @return 求一个矩阵的余子式 (i:行索引从0开始,j:列索引从0开始)->minor
	 */
	static BiFunction<Integer, Integer, Double> minorFn(final INdarrayX<Double> nx) {
		final int mod = nx.mod();
		assert Objects.equals(nx.nrow(), nx.ncol()) : String.format("nx必须为方阵,nrow:%s,ncol:%s", nx.nrow(), nx.ncol());
		return (i, j) -> (mod == 1) ? nx.get() : det0(minorNxFn(nx).apply(i, j));
	}

	/**
	 * 求一个矩阵的代数余子式函数
	 * 
	 * @param nx 数据矩阵
	 * @return 矩阵的代数余子式函数:(i:行索引从0开始,j:列索引从0开始)->cofactor
	 */
	static BiFunction<Integer, Integer, Double> cofactorFn(final INdarrayX<Double> nx) {
		return (i, j) -> ((i + j) % 2 == 0 ? 1 : -1) * minorFn(nx).apply(i, j);
	}

	/**
	 * 矩阵行的伴随矩阵，adjoints 伴随矩阵是对cofactors的引用
	 * 
	 * @param <T> 矩阵元素
	 * @param nx  矩阵
	 * @return 矩阵行的伴随矩阵
	 */
	static <T> INdarrayX<Double> adjoints(final INdarrayX<T> nx) {
		return cofactors(nx).transpose2();
	}

	/**
	 * 代数余子式
	 * 
	 * @param <T> 矩阵元素类型
	 * @param nx  矩阵
	 * @return 代数余子式
	 */
	static <T> INdarrayX<Double> cofactors(final INdarrayX<T> nx) {
		final BiFunction<Integer, Integer, Double> fn = INdarrayX.cofactorFn(nx.dblx());
		return nx.fmap((i, j, e) -> fn.apply(i, j));
	}

	/**
	 * 独热编码 <br>
	 * 
	 * Converts a class vector (integers) to binary class matrix. <br>
	 * 参考 keras to_categorical api
	 * 
	 * @param <T>         数字类型
	 * @param origind     编码向量,[0,1,2,3,...,之类的自然数],T会转换成相应的整数(按照num_classes进行取模)进行编码
	 * @param num_classes 编码长度
	 * @return 独热编码向量
	 */
	static <T extends Number> INdarrayX<Byte> ohEncode(final INdarray<T> origind, final int num_classes) {
		final INdarrayX<Byte> onehots = diagx(INdarray.nd(i -> (byte) 1, num_classes).data());
		return origind.map(i -> i.intValue() % num_classes).map(onehots::row) //
				.flatMap(e -> e.stream()) //
				.collect(INdarray.nxclc(num_classes));
	}

	/**
	 * 独热编码 <br>
	 * 
	 * Converts a class vector (integers) to binary class matrix.
	 * 
	 * @param <T>     数字类型
	 * @param origins 源义向量
	 * @return 独热编码向量
	 */
	static <T extends Number> INdarrayX<Byte> ohEncode(final Iterable<T> origins) {
		final INdarray<T> origind = INdarray.nd(origins);
		return INdarrayX.ohEncode(origind, origind.length());
	}

	/**
	 * 独热编码解析 <br>
	 * 
	 * Converts a class vector (integers) to binary class matrix.
	 * 
	 * @param <T>    数字类型
	 * @param onehot 独热编码向量
	 * @return 独热编码解析
	 */
	static <T extends Number> Integer ohDecode(final INdarray<T> onehot) {
		return INdarrayX.ohDecode(onehot, (Iterable<Integer>) null);
	}

	/**
	 * 独热编码解析 <br>
	 * 
	 * Converts a class vector (integers) to binary class matrix.
	 * 
	 * @param <T>     数字类型
	 * @param <U>     结果类型
	 * @param onehot  独热编码向量
	 * @param origins 独热编码解析字典
	 * @return 独热编码解析
	 */
	static <T extends Number, U> U ohDecode(final INdarray<T> onehot, final Iterable<U> origins) {
		final int n = onehot.length();

		@SuppressWarnings("unchecked")
		final Iterator<U> itr = Optional.ofNullable(origins).orElse(() -> {
			return (Iterator<U>) nats(n).iterator();
		}).iterator();

		for (int i = 0; i < n; i++) {
			if (itr.hasNext()) {
				final U u = itr.next();
				if (Objects.equals(onehot.get(i).intValue(), 1)) {
					return u;
				} // if
			} else { // origin 访问尽了仍未找到
				return null;
			} //
		} // if

		return null; // onehot 中全都是0没有1,这是一个错误的onehot 编码
	}

	/**
	 * metas 元数据的，参数键名
	 * 
	 * @author gbench
	 *
	 */
	enum MetaKey {
		/**
		 * 取模长度,矩阵的行长度即列的数据:ncol与mod函数返回mod参数对应的值（合法有效值为一个大于0正整数)
		 */
		mod // 取模长度,矩阵的行长度即列的数据:ncol与mod函数返回mod参数对应的值（合法有效值为一个大于0正整数)
	}
}