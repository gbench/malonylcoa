package gbench.util.math.algebra.tuple;

import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据框对象的实现,列式的组织方式
 * 
 * @author gbench
 *
 */
public class DFrame implements IterableOps<IRecord, DFrame> {

	/**
	 * 构造函数
	 * 
	 * @param data 源数据
	 */
	public DFrame(final List<IRecord> data) {
		this.rowsData = data.toArray(IRecord[]::new);
	}

	/**
	 * 构造函数
	 * 
	 * @param data 源数据
	 */
	public DFrame(final IRecord[] data) {
		this.rowsData = data;
	}

	/**
	 * 转换成列数据数组
	 * 
	 * @return
	 */
	public LinkedHashMap<String, ArrayList<Object>> initialize() {

		final var map = new LinkedHashMap<String, ArrayList<Object>>();

		Arrays.stream(this.rowsData).forEach(e -> {
			e.keys().forEach(k -> {
				map.compute(k, (_key, _value) -> _value == null ? new ArrayList<Object>() : _value).add(e.get(k));
			});
		});

		return map;

	}

	/**
	 * 
	 * @return
	 */
	public List<String> keys() {
		return new ArrayList<>(this.cols().keySet());
	}

	/**
	 * 
	 * @return
	 */
	public String keyOf(final int idx) {

		final var kk = this.keys();
		return idx < kk.size() ? kk.get(idx) : null;
	}

	/**
	 * 键名索引
	 * 
	 * @param key 键名
	 * @return 键名索引 从0开始
	 */
	Integer indexOf(final String key) {

		final var kk = this.keys();

		for (int i = 0; i < kk.size(); i++) {
			final var _key = kk.get(i);
			if (key.equals(_key))
				return i;
		}

		return null;
	}

	/**
	 * 指定列名的记录
	 * 
	 * @param name 列名
	 * @return 列数据
	 */
	public Optional<List<Object>> colOpt(final String name) {
		return Optional.ofNullable(this.cols().get(name));
	}

	/**
	 * 指定列名的记录
	 * 
	 * @param name 列名
	 * @return 列数据
	 */
	public List<Object> col(final String name) {
		return this.colOpt(name).orElse(null);
	}

	/**
	 * 指定列索引的记录
	 * 
	 * @param idx 列名索引从0开始
	 * @return 列数据
	 */
	public Optional<List<Object>> colOpt(final int idx) {
		return this.colOpt(this.keyOf(idx));
	}

	/**
	 * 列数据
	 * 
	 * @param idx 列名索引从0开始
	 * @return 列数据
	 */
	public List<Object> col(final int idx) {
		return this.colOpt(idx).orElse(null);
	}

	/**
	 * 列数据
	 * 
	 * @param name 列名
	 * @return 列数据的流
	 */
	@SuppressWarnings("unchecked")
	public <T, A, R> R col(final String name, Collector<T, A, R> collector) {
		return this.col(name).stream().map(e -> (T) e).collect(collector);
	}

	/**
	 * 列数据
	 * 
	 * @param idx 列名索引从0开始
	 * @return 列数据的流
	 */
	@SuppressWarnings("unchecked")
	public <T, A, R> R col(final int idx, Collector<T, A, R> collector) {
		return this.col(this.keyOf(idx)).stream().map(e -> (T) e).collect(collector);
	}

	/**
	 * 列数据
	 * 
	 * @param <T>    原数据类型
	 * @param <U>    目标数据类型
	 * @param name   列名
	 * @param mapper 列元素的数据变换 t->u
	 * @return 列数据的列表
	 */
	@SuppressWarnings("unchecked")
	public <T, U> List<U> col(final String name, final Function<T, U> mapper) {
		return this.col(name).stream().map(e -> (T) e).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 列数据
	 * 
	 * @param <T>    原数据类型
	 * @param <U>    目标数据类型
	 * @param idx    列名索引 从0开始
	 * @param mapper 列元素的数据变换 t->u
	 * @return 列数据的列表
	 */
	public <T, U> List<U> col(final int idx, final Function<T, U> mapper) {
		return this.col(this.keyOf(idx), mapper);
	}

	/**
	 * 列数据
	 * 
	 * @param <T>    原数据类型
	 * @param <U>    目标数据类型
	 * @param name   列名
	 * @param mapper 列元素的数据变换 t->u
	 * @return 列数据的流
	 */
	@SuppressWarnings("unchecked")
	public <T, U> Stream<U> colS(final String name, final Function<T, U> mapper) {
		return this.col(name).stream().map(e -> (T) e).map(mapper);
	}

	/**
	 * 列数据
	 * 
	 * @param <T>    原数据类型
	 * @param <U>    目标数据类型
	 * @param idx    列名索引 从0开始
	 * @param mapper 列元素的数据变换 t->u
	 * @return 列数据的流
	 */
	public <T, U> Stream<U> colS(final int idx, final Function<T, U> mapper) {
		return this.colS(this.keyOf(idx), mapper);
	}

	/**
	 * 列数据
	 * 
	 * @param name 列名
	 * @return 列数据的流
	 */
	public Stream<Object> colS(final String name) {
		return this.col(name).stream();
	}

	/**
	 * 列数据
	 * 
	 * @param idx 列名索引从0开始
	 * @return 列数据的流
	 */
	public Stream<Object> colS(final int idx) {
		return this.col(this.keyOf(idx)).stream();
	}

	/**
	 * 列集数据的流
	 * 
	 * @return 列集数据的流
	 */
	public synchronized Stream<Tuple2<String, ArrayList<Object>>> colS() {
		return this.cols().entrySet().stream().map(e -> P(e.getKey(), e.getValue()));
	}

	/**
	 * 列数据
	 * 
	 * @return 行数据流
	 */
	public synchronized LinkedHashMap<String, ArrayList<Object>> cols() {
		return this.colsData == null ? this.colsData = this.initialize() : this.colsData;
	}

	/**
	 * 行数据流
	 * 
	 * @return 行数据流
	 */
	public Stream<IRecord> rowS() {
		return Arrays.stream(this.rowsData);
	}

	/**
	 * 指定行索引的记录
	 * 
	 * @param idx 行名索引从0开始
	 * @return 行数据流
	 */
	public Optional<IRecord> rowOpt(final int idx) {
		return Optional.of(this.rowsData[idx]);
	}

	/**
	 * 指定行索引的记录
	 * 
	 * @param idx 行名索引从0开始
	 * @return 行数据流
	 */
	public IRecord row(final int idx) {
		return this.rowOpt(idx).orElse(null);
	}

	/**
	 * 行数据流
	 * 
	 * @return 行数据流
	 */
	public List<IRecord> rows() {
		return Arrays.asList(this.rowsData);
	}

	/**
	 * 列数据流
	 * 
	 * @return 列数据流
	 */
	public List<Tuple2<String, List<Object>>> tuples() {
		return IRecord.rows2tuples(this.rows());
	}

	/**
	 * 列数据流
	 * 
	 * @return 列数据流
	 */
	public Stream<Tuple2<String, List<Object>>> tupleS() {
		return IRecord.rows2tupleS(this.rows());
	}

	/**
	 * 列变换,数据变形
	 * 
	 * @param <U>    结果类型
	 * @param mapper 映射函数 {(name,col)} -> u
	 * @return U类型的结果
	 */
	public <U> U mapCols(final Function<LinkedHashMap<String, ArrayList<Object>>, U> mapper) {
		return mapper.apply(this.cols());
	}

	/**
	 * 使用指定的 名称序列 来为数据框 进行命名。
	 * 
	 * @param names 新的名称序列,如果 names 只有一个元素 则视为 names[0]为 键名描述序列,不同键名之间 使用 半角',' 给予分割
	 * @return 重新命名后的DFrame
	 */
	public DFrame rename(final String... names) {
		return this.rowS().map(IRecord.namector(names)).collect(DFrame.dfmclc);
	}

	/**
	 * 使用指定的 名称序列 来为数据框 进行命名。
	 * 
	 * @param names 新的名称序列,如果 names 只有一个元素 则视为 names[0]为 键名描述序列,不同键名之间 使用 半角',' 给予分割
	 * @return 重新命名后的DFrame
	 */
	public DFrame rename(final List<?> names) {
		final var _names = names.stream().filter(Objects::nonNull).map(Object::toString).toArray(String[]::new);
		return this.rename(_names);
	}

	/**
	 * 使用指定的 名称序列 来为数据框 进行命名。
	 * 
	 * @param namector 命名函数(key:键名) -&gt;key1:新的键名
	 * @return 重新命名后的DFrame
	 */
	public DFrame rename(final Function<String, String> namector) {
		return this.rename((i, k) -> namector.apply(k));
	}

	/**
	 * 使用指定的 名称序列 来为数据框 进行命名。
	 * 
	 * @param namector 命名函数(idx:键名索引从0开始,key:键名) -&gt;key1:新的键名
	 * @return 重新命名后的DFrame
	 */
	public DFrame rename(final BiFunction<Integer, String, String> namector) {
		final var idx_atom = new AtomicInteger();
		final var names = this.keys().stream().map(k -> namector.apply(idx_atom.incrementAndGet(), k))
				.toArray(String[]::new);
		return this.rename(names);
	}

	/**
	 * 列求和
	 * 
	 * @param keys 排序列名列表, 键名之间用半角','分隔
	 * @return DFrame 排序后的 数据框
	 */
	public DFrame sorted(final String keys) {
		return this.sorted(keys, true);
	}

	/**
	 * 列求和
	 * 
	 * @param keys 排序列名列表, 键名之间用半角','分隔
	 * @param asc  是否升序,true 表示升序,小值在前,false 表示降序,大值在前
	 * @return DFrame 排序后的 数据框
	 */
	public DFrame sorted(final String keys, final boolean asc) {
		return this.sorted(IRecord.cmp(keys, asc));
	}

	/**
	 * 列求和
	 * 
	 * @param name 列名
	 * @return 求和
	 */
	public double sum(final String name) {
		return this.col(name, Collectors.summarizingDouble(IRecord.todbl(0d))).getSum();
	}

	/**
	 * 获取各个列的累计和
	 * 
	 * @return IRecord 获取各个列的累计和
	 */
	public IRecord sum() {
		return this.sum(IRecord.plus());
	}

	/**
	 * 获取各个列的累计和
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord 进行求和 (record0,record1)->record
	 * @return 获取各个列的累计和
	 */
	public IRecord sum(final BinaryOperator<IRecord> biop) {
		return this.sumOpt(biop).orElse(null);
	}

	/**
	 * 获取各个列的累计和
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord 进行求和 (record0,record1)->record
	 * @return 获取各个列的累计和
	 */
	public Optional<IRecord> sumOpt(final BinaryOperator<IRecord> biop) {
		return this.rowS().reduce(biop);
	}

	/**
	 * 获取指定列的平均值
	 * 
	 * @param name 列名
	 * @return 指定列的平均值
	 */
	public double mean(final String name) {
		return this.col(name, Collectors.summarizingDouble(IRecord.todbl(0d))).getAverage();
	}

	/**
	 * 获取各个列的平均值
	 * 
	 * @return 获取各个列的平均值
	 */
	public IRecord mean() {
		return this.mean(IRecord.plus());
	}

	/**
	 * 获取各个列的平均值
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord 进行求和 (record0,record1)->record
	 * @return 获取各个列的平均值
	 */
	public IRecord mean(final BinaryOperator<IRecord> biop) {
		return this.meanOpt(biop).orElse(null);
	}

	/**
	 * 获取各个列的平均值
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord 进行求和 (record0,record1)->record
	 * @return 获取各个列的平均值
	 */
	public Optional<IRecord> meanOpt(final BinaryOperator<IRecord> biop) {
		final var cnt = this.shape()._1; // 行数
		return Optional.ofNullable(cnt > 1 ? this : null)
				.map(e -> e.rowS().reduce(biop).map(r -> r.divide(cnt)).orElse(null));
	}

	/**
	 * 列计数
	 * 
	 * @param name 列名
	 * @return 列计数
	 */
	public long count(final String name) {
		return this.col(name, Collectors.summarizingDouble(IRecord.todbl(0d))).getCount();
	}

	/**
	 * 获取最大值（各个列的最大值，或是 最大行记录, 最大值的规则 需要 使用 biop 来指定 )
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord较大的求取出来 (record0,record1)->record <br>
	 *             record 可以是 record0,record1 之一, 也可以是 重新生成的一个IRecord，具体的要根据业务规则来实现。
	 * @return 获取最大值（各个列的最大值，或是 最大行记录, 最大值的规则 需要 使用 biop 来指定 )
	 */
	public Optional<IRecord> maxOpt(final BinaryOperator<IRecord> biop) {
		return this.rowS().reduce(biop);
	}

	/**
	 * 获取最大值（各个列的最大值，或是 最大行记录, 最大值的规则 需要 使用 biop 来指定 )
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord较大的求取出来 (record0,record1)->record <br>
	 *             record 可以是 record0,record1 之一, 也可以是 重新生成的一个IRecord，具体的要根据业务规则来实现。
	 * @return 获取最大值（各个列的最大值，或是 最大行记录, 最大值的规则 需要 使用 biop 来指定 )
	 */
	public IRecord max(final BinaryOperator<IRecord> biop) {
		return this.maxOpt(biop).orElse(null);
	}

	/**
	 * 获取最小值（各个列的最小值，或是 最大行记录, 最小值的规则 需要 使用 biop 来指定 )
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord较小的求取出来 (record0,record1)->record <br>
	 *             record 可以是 record0,record1 之一, 也可以是 重新生成的一个IRecord，具体的要根据业务规则来实现。
	 * @return 获取最大值（各个列的最小值，或是 最大行记录, 最小值的规则 需要 使用 biop 来指定 )
	 */
	public Optional<IRecord> minOpt(final BinaryOperator<IRecord> biop) {
		return this.rowS().reduce(biop);
	}

	/**
	 * 获取最小值（各个列的最小值，或是 最大行记录, 最小值的规则 需要 使用 biop 来指定 )
	 * 
	 * @param biop IRecord的二元运算法 把 两个IRecord较小的求取出来 (record0,record1)->record <br>
	 *             record 可以是 record0,record1 之一, 也可以是 重新生成的一个IRecord，具体的要根据业务规则来实现。
	 * @return 获取最大值（各个列的最小值，或是 最大行记录, 最小值的规则 需要 使用 biop 来指定 )
	 */
	public IRecord min(final BinaryOperator<IRecord> biop) {
		return this.minOpt(biop).orElse(null);
	}

	/**
	 * 数据框的尺寸
	 * 
	 * @return (行的数量,列的数量)
	 */
	public Tuple2<Integer, Integer> shape() {
		return P(this.rowsData.length, this.cols().size());
	}

	@Override
	public DFrame CC(final List<IRecord> as) {
		return new DFrame(as);
	}

	@Override
	public IRecord[] data() {
		return this.rowsData;
	}

	/**
	 * 数据刷新,清空列集合数据给予冲洗计算
	 * 
	 * @return DFrame 本身，以实现链式编程
	 */
	public synchronized DFrame refresh() {
		this.colsData = null;
		return this;
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * => 1)(_ + _) Note: Even when applied to a view or a lazy collection it will
	 * always force the elements.
	 * 
	 * @param <K> the type of keys returned by the discriminator function.
	 * @param key the discriminator function
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x => f(x) == k)
	 */
	public <K> Map<K, DFrame> groupMapReduce(final Function<IRecord, K> key) {
		return this.groupMapReduce(key, DFrame::new);
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * => 1)(_ + _) Note: Even when applied to a view or a lazy collection it will
	 * always force the elements.
	 * 
	 * @param <K> the type of keys returned by the discriminator function.
	 * @param key the discriminator function
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x => f(x) == k)
	 */
	public <K> IRecord groupBy2(final Function<IRecord, K> key) {
		return IRecord.REC(this.groupMapReduce(key));
	}

	/**
	 * 笛卡尔连接
	 * 
	 * @param rights 右侧的 记录集合
	 * @return 笛卡尔连接 之后生成的数据集合
	 */
	public Stream<IRecord> crossjoinS(final Iterable<IRecord> rights) {
		return this.rowS().flatMap(IRecord.concatS(rights));
	}

	/**
	 * 转换成 IRecord
	 * 
	 * @return IRecord 结构
	 */
	public IRecord toRecord() {
		return this.colS().collect(IRecord.rclc(e -> e));
	}

	/**
	 * 笛卡尔连接
	 * 
	 * @param rights 右侧的 记录集合
	 * @return 笛卡尔连接 之后生成的数据集合
	 */
	public DFrame crossjoin(final Iterable<IRecord> rights) {
		return this.crossjoinS(rights).collect(DFrame.dfmclc);
	}

	/**
	 * 按照列值过滤
	 * 
	 * @param flds 保留的键名序列，键名之间采用半角逗号分隔
	 * @return DFrame
	 */
	public DFrame filter2(final String flds) {
		return this.fmap(e -> e.filter(flds));
	}

	/**
	 * 按照列值过滤（否定过滤）
	 * 
	 * @param flds 剔除的键名序列，键名之间采用半角逗号分隔
	 * @return DFrame
	 */
	public DFrame filterNot2(final String flds) {
		return this.fmap(e -> e.filterNot(flds));
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { DFrame.class, this.rowsData });
	}

	@Override
	public boolean equals(final Object obj) {
		return obj == this || (obj instanceof DFrame dfm
				? Objects.equals(this.rowsData, dfm.rowsData) && Objects.equals(this.colsData, dfm.colsData)
				: false);
	}

	/**
	 * 数据内容格式化
	 */
	public String toString() {
		if (this.rowsData.length > 0) {
			final var buffer = new StringBuffer();
			final var first = this.rowsData[0];
			final var keys = first.keys();
			final var header = keys.stream().collect(Collectors.joining("\t"));
			final var body = this.rowS().map(e -> keys.stream().map(e::str).collect(Collectors.joining("\t")))
					.collect(Collectors.joining("\n"));
			buffer.append(header + "\n");
			buffer.append(body);
			return buffer.toString();
		} else {
			return "DFrame(empty)";
		}
	}

	/**
	 * 以数据列的方式构建数据框
	 * 
	 * @param cols 列序列
	 * @return DFrame
	 */
	public static DFrame of(final List<?>... cols) {
		return Arrays.stream(cols).collect(IRecord.rclc2()).rowS().collect(DFrame.dfmclc);
	}

	/**
	 * IRecord 类型的归集器 [rec:行]->dfm,行法归集
	 */
	public static Collector<IRecord, ?, DFrame> dfmclc = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
			List::add, (left, right) -> {
				left.addAll(right);
				return left;
			}, e -> {
				return new DFrame(e);
			});

	/**
	 * Map 类型的归集器 [map:行]->dfm,行法归集
	 */
	public static Collector<Map<?, ?>, ?, DFrame> dfmclc2 = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
			(aa, a) -> aa.add(new MyRecord(a)), (left, right) -> {
				left.addAll(right);
				return left;
			}, e -> {
				return new DFrame(e);
			});

	/**
	 * (String,Object) 类型Tuple2的归集器 [(key:列名,obj:列数据)]->dfm,列法归集
	 */
	public static Collector<Tuple2<String, ?>, ?, DFrame> dfmclc3 = Collector
			.of((Supplier<ArrayList<Tuple2<String, ?>>>) ArrayList::new, List::add, (left, right) -> {
				left.addAll(right);
				return left;
			}, e -> {
				final var cols = e.stream().map(Tuple2::_2).map(IRecord::asList).toArray(List[]::new);
				final var names = e.stream().map(Tuple2::_1).collect(Collectors.toList());
				return DFrame.of(cols).rename(names);
			});

	protected transient LinkedHashMap<String, ArrayList<Object>> colsData = null; // 列数据
	protected final IRecord[] rowsData; // 行数据

}