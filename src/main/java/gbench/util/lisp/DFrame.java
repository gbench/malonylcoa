package gbench.util.lisp;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 数据框
 * 
 * @author xuqinghua
 *
 */
public class DFrame implements Iterable<IRecord> {

	/**
	 * 构造函数
	 * 
	 * @param data 源数据
	 */
	public DFrame(final Iterable<IRecord> data) {
		this(StreamSupport.stream(data.spliterator(), false).toArray(IRecord[]::new));
	}

	/**
	 * 基础构造函数
	 * 
	 * @param data 源数据
	 */
	public DFrame(final IRecord[] data) {
		this.rowsData = Optional.ofNullable(data).orElse(new IRecord[] {});
	}

	/**
	 * 健名列表
	 * 
	 * @return 健名列表
	 */
	public List<String> keys() {
		return new ArrayList<>(this.cols().keySet());
	}

	/**
	 * 根据索引 提取键名
	 * 
	 * @param idx 键名索引从0开始
	 * @return 键名
	 */
	public String keyOf(final int idx) {

		final List<String> kk = this.keys();
		return idx < kk.size() ? kk.get(idx) : null;
	}

	/**
	 * 根据键名 提取 键名索引
	 * 
	 * @param key 键名
	 * @return 键名索引
	 */
	public int indexOf(final String key) {

		int i = -1;
		for (String _key : this.keys()) {
			i++;
			if (_key.equals(key))
				break;
		}
		return i;
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
	 * 转换成列数据数组
	 * 
	 * @return LinkedHashMap
	 */
	public LinkedHashMap<String, ArrayList<Object>> initialize() {

		final LinkedHashMap<String, ArrayList<Object>> map = new LinkedHashMap<String, ArrayList<Object>>();
		Arrays.stream(this.rowsData).forEach(e -> {
			e.keys().forEach(k -> {
				map.compute(k, (_key, _value) -> _value == null ? new ArrayList<Object>() : _value).add(e.get(k));
			});
		});

		return map;

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
	 * @param idx    列名索引从0开始
	 * @param mapper 列元素的数据变换 t->u
	 * @return 列数据的流
	 */
	public <T, U> Stream<U> colS(final int idx, final Function<T, U> mapper) {
		return this.colS(this.keyOf(idx), mapper);
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
	public <T, U> List<U> cols(final String name, final Function<T, U> mapper) {
		return this.col(name).stream().map(e -> (T) e).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 列数据
	 * 
	 * @param <T>    原数据类型
	 * @param <U>    目标数据类型
	 * @param idx    列名索引从0开始
	 * @param mapper 列元素的数据变换 t->u
	 * @return 列数据的列表
	 */
	public <T, U> List<U> cols(final int idx, final Function<T, U> mapper) {
		return this.cols(this.keyOf(idx), mapper);
	}

	/**
	 * 行数据流
	 * 
	 * @return 新对象 行数据流(LinkedList结构的，方便插入）
	 */
	public LinkedList<IRecord> rows() {
		return Stream.of(this.rowsData).collect(llclc());
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
	 * 行数据流
	 * 
	 * @param <U>    变换器的结果类型
	 * @param mapper 行记录便器 rec->u
	 * @return 行数据流 [u]
	 */
	public <U> Stream<U> rowS(final Function<IRecord, U> mapper) {

		return this.rowS().map(mapper);
	}

	/**
	 * 指定行索引的记录
	 * 
	 * @param idx 行名索引从0开始
	 * @return 行数据流
	 */
	public Optional<IRecord> rowOpt(final int idx) {
		return Optional.ofNullable(this.rowsData == null || this.rowsData.length < 1 || idx >= this.rowsData.length //
				? null // 空值
				: this.rowsData[idx]);
	}

	/**
	 * 指定行索引的记录
	 * 
	 * @param<T> 元素类型
	 * @param idx    行名索引从0开始
	 * @param mapper 值变换函数 o->t
	 * @return 行数据流
	 */
	public <T> Optional<List<T>> rowOpt(final int idx, final Function<Tuple2<String, Object>, T> mapper) {
		return this.rowOpt(idx).map(e -> e.tupleS().map(mapper).collect(Collectors.toList()));
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
	 * 指定行索引的记录
	 * 
	 * @param<T> 元素类型
	 * @param idx    行名索引从0开始
	 * @param mapper 值变换函数 o->t
	 * @return 行数据流
	 */
	public <T> List<T> row(final int idx, final Function<Tuple2<String, Object>, T> mapper) {
		return this.rowOpt(idx, mapper).orElse(null);
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
	 * 数据框变换
	 * 
	 * @param <T>    结果类型
	 * @param mapper dfm->t
	 * @return T 类型对象
	 */
	public <T> T mutate(final Function<DFrame, T> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 数组应用 暴露源数据, arrayOf的别名
	 * 
	 * @param <TARGET> 目标结果类型
	 * @param mapper   结果变换类型 data->u
	 * @return U类型的结果
	 */
	public <TARGET> TARGET dataOf(final Function<Object[][], TARGET> mapper) {
		return this.arrayOf(mapper);
	}

	/**
	 * 数据数组
	 * 
	 * @param <T>    mapper 结果类型
	 * @param mapper obj->t
	 * @return 数据数组
	 */
	@SuppressWarnings("unchecked")
	public <T> T[][] data(final Function<Object, T> mapper) {
		final Object[][] oo = this.rowS().map(e -> e.arrayOf(p -> p)).toArray(Object[][]::new);
		final int width = Stream.of(oo).collect(Collectors.summarizingInt(e -> e.length)).getMax();
		final Optional<Class<T>> optCls = (Optional<Class<T>>) (Object) Stream.of(oo).flatMap(Stream::of).map(mapper)
				.filter(Objects::nonNull).map(Object::getClass).findFirst();
		final T[][] tt = optCls.map(clazz -> {
			final IntFunction<T[]> f1 = n -> (T[]) Array.newInstance(clazz, n);
			final IntFunction<T[][]> f2 = n -> (T[][]) Array.newInstance(clazz, n, width);
			return Stream.of(oo).map(e -> Stream.of(e).toArray(f1)).toArray(f2);
		}).orElse((T[][]) oo);

		return tt;
	}

	/**
	 * 数据数组
	 * 
	 * @return 数据数组
	 */
	public Object[][] data() {
		return this.data(e -> e);
	}

	/**
	 * 数组类型变换
	 * 
	 * @param <T>    元素类型
	 * @param <U>    结果类型
	 * @param mapper 变换函数 obj-&gt;t
	 * @param gen    生成函数 (kk,tt)-&gt;u
	 * @return U数据类型
	 */
	public <T, U> U arrayOf(final Function<Object, T> mapper,
			final BiFunction<? super Iterable<String>, T[][], U> gen) {
		return gen.apply(this.keys(), this.data(mapper));
	}

	/**
	 * 数组类型变换
	 * 
	 * @param <T>    元素类型
	 * @param <U>    结果类型
	 * @param mapper 变换函数 obj-&gt;t
	 * @param gen    生成函数 tt-&gt;u
	 * @return U数据类型
	 */
	public <T, U> U arrayOf(final Function<Object, T> mapper, final Function<T[][], U> gen) {
		return this.arrayOf(mapper, (kk, tt) -> gen.apply(tt));
	}

	/**
	 * 数组类型变换
	 * 
	 * @param <U> 结果类型
	 * @param gen 生成函数 tt-&gt;u
	 * @return U数据类型
	 */
	public <U> U arrayOf(final BiFunction<? super Iterable<String>, Object[][], U> gen) {
		return this.arrayOf(e -> e, gen);
	}

	/**
	 * 数组类型变换
	 * 
	 * @param <U> 结果类型
	 * @param gen 生成函数 tt-&gt;u
	 * @return U数据类型
	 */
	public <U> U arrayOf(final Function<Object[][], U> gen) {
		return this.arrayOf(e -> e, (kk, tt) -> gen.apply(tt));
	}

	/**
	 * 否定过滤
	 *
	 * @param keys 剔除的键名序列，键名之间采用半角逗号分隔
	 * @return DFrame
	 */
	public DFrame filterNot(final String keys) {
		return this.rowS().map(e -> e.filterNot(keys)).collect(DFrame.dfmclc);
	}

	/**
	 * 肯定过滤
	 *
	 * @param keys 保留的键名序列，键名之间采用半角逗号分隔
	 * @return DFrame
	 */
	public DFrame filter(final String keys) {
		return this.rowS().map(e -> e.filter(keys)).collect(DFrame.dfmclc);
	}

	/**
	 * 肯定过滤
	 *
	 * @param keys 保留的键名序列，键名之间采用半角逗号分隔
	 * @return DFrame
	 */
	public DFrame filter(final List<String> keys) {
		return this.rowS().map(e -> e.filter(keys)).collect(DFrame.dfmclc);
	}

	/**
	 * 返回 数据尺寸(行数,列数)
	 * 
	 * @return 数据的尺寸 (行数,列数)
	 */
	public Tuple2<Integer, Integer> shape() {
		return Tuple2.of(this.nrows(), this.ncols());
	}

	/**
	 * 行数
	 * 
	 * @return
	 */
	public int nrows() {
		final int m = this.rowsData.length;
		return m;
	}

	/**
	 * 行数(nrows)别名
	 * 
	 * @return
	 */
	public int height() {
		return this.nrows();
	}

	/**
	 * 列数量
	 * 
	 * @return
	 */
	public int ncols() {
		final int n = this.cols().size();
		return n;
	}

	/**
	 * 列数量(ncols别名)
	 * 
	 * @return
	 */
	public int width() {
		return this.ncols();
	}

	/**
	 * IRecord的归集器
	 * 
	 * @param <U>       结果类型
	 * @param collector [r] -> U
	 * @return U
	 */
	public <U> U collect(Collector<? super IRecord, ?, U> collector) {
		return this.rowS().collect(collector);
	}

	/**
	 * 根据长度拆分数据框
	 * 
	 * @param n 长度
	 * @return （car,cdr)
	 */
	public Tuple2<DFrame, DFrame> split(final int n) {

		if (this.rowsData.length > n) {
			final IRecord[] dd1 = (IRecord[]) Arrays.copyOfRange(this.rowsData, 0, n);
			final IRecord[] dd2 = (IRecord[]) Arrays.copyOfRange(this.rowsData, n, this.rowsData.length);
			return Tuple2.of(DFrame.of(Arrays.asList(dd1)), DFrame.of(Arrays.asList(dd2)));
		} else {
			return Tuple2.of(DFrame.of(Arrays.asList(this.rowsData)), null);
		}
	}

	/**
	 * 列拆分
	 * 
	 * @param n 键名索引位置,从0开始(df1 exclusiv,df2 inclusive)
	 * @return (df1,df2)
	 */
	public Tuple2<DFrame, DFrame> split2(final int n) {
		final int size = this.keys().size();
		final List<String> keys = this.keys().subList(0, n > size ? size : n);

		return this.split2(keys);

	}

	/**
	 * 列拆分
	 * 
	 * @param keys 键值列表
	 * @return (df1,df2)
	 */
	public Tuple2<DFrame, DFrame> split2(final String keys[]) {
		return this.split2(Arrays.asList(keys));
	}

	/**
	 * 列拆分
	 * 
	 * @param keys 键值列表
	 * @return (df1,df2)
	 */
	public Tuple2<DFrame, DFrame> split2(final List<String> keys) {

		if (this.keys().size() > keys.size()) {
			final Map<Object, List<String>> kk = this.keys().stream()
					.collect(Collectors.groupingBy(e -> keys.contains(e)));
			final List<String> k1 = kk.get(true);
			final List<String> k2 = kk.get(false);
			final DFrame df1 = this.filter(k1);
			final DFrame df2 = this.filter(k2);
			return Tuple2.of(df1, df2);
		} else {
			return Tuple2.of(DFrame.of(Arrays.asList(this.rowsData)), null);
		}

	}

	/**
	 * IRecord的归集器
	 * 
	 * @return 对象数组
	 */
	public Object[][] toArray() {
		return this.arrayOf((hh, oo) -> oo);
	}

	/**
	 * 头部元素
	 * 
	 * @param n 头数据长度
	 * @return 头数据的元素的流
	 */
	public Optional<IRecord> headOpt() {
		return Optional.ofNullable(this.head());
	}

	/**
	 * 头部元素
	 * 
	 * @return IRecord
	 */
	public IRecord head() {
		final IRecord[] dd = this.rowsData;
		return dd.length > 0 ? dd[0] : null;
	}

	/**
	 * 头部元素
	 * 
	 * @param n 头数据长度
	 * @return 头数据的元素的流
	 */
	public Stream<IRecord> headS(final int n) {
		final IRecord[] dd = this.rowsData;
		return dd.length > 0 ? Stream.of(Arrays.copyOfRange(this.rowsData, 0, n > dd.length ? dd.length : n)) : null;
	}

	/**
	 * 尾部元素
	 * 
	 * @return 尾部元素
	 */
	public List<IRecord> tail() {
		return this.tailS(this.rowsData.length - 1).collect(Collectors.toList());
	}

	/**
	 * 尾部元素
	 * 
	 * @param n 尾数据长度
	 * @return 尾部元素
	 */
	public Stream<IRecord> tailS(final int n) {
		final IRecord[] dd = this.rowsData;
		final int sz = dd.length;
		return Optional.ofNullable(dd.length > 1 ? Arrays.copyOfRange(dd, sz - n, dd.length) : null).map(lines -> {
			final CopyOnWriteArrayList<IRecord> aa = new CopyOnWriteArrayList<IRecord>();
			for (final IRecord line : lines) {
				aa.add(line);
			}
			return aa.stream();
		}).orElse(Stream.empty());
	}

	/**
	 * 最后一个元素
	 * 
	 * @param n 头数据长度
	 * @return 最后一个元素
	 */
	public IRecord last() {
		final IRecord[] dd = this.rowsData;
		return dd.length > 0 ? dd[dd.length - 1] : null;
	}

	/**
	 * 最后一个元素
	 * 
	 * @return 最后一个元素
	 */
	public Optional<IRecord> lastOpt() {
		return Optional.ofNullable(this.last());
	}

	/**
	 * Returns a view of the portion of this list between the specified fromIndex,
	 * inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the
	 * returned list isempty.) The returned list is backed by this list, so
	 * non-structuralchanges in the returned list are reflected in this list, and
	 * vice-versa.The returned list supports all of the optional list operations
	 * supportedby this list.
	 * 
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @return 字列表
	 */
	public List<IRecord> subList(final int fromIndex) {
		return this.subList(fromIndex, null);
	}

	/**
	 * Returns a view of the portion of this list between the specified fromIndex,
	 * inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the
	 * returned list isempty.) The returned list is backed by this list, so
	 * non-structuralchanges in the returned list are reflected in this list, and
	 * vice-versa.The returned list supports all of the optional list operations
	 * supportedby this list.
	 * 
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @param toIndex   high endpoint (exclusive) of the subList
	 * @return 字列表
	 */
	public List<IRecord> subList(final Integer fromIndex, final Integer toIndex) {
		return this.subListS(fromIndex, toIndex).collect(Collectors.toList());
	}

	/**
	 * Returns a view of the portion of this list between the specified fromIndex,
	 * inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the
	 * returned list isempty.) The returned list is backed by this list, so
	 * non-structuralchanges in the returned list are reflected in this list, and
	 * vice-versa.The returned list supports all of the optional list operations
	 * supportedby this list.
	 * 
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @param toIndex   high endpoint (exclusive) of the subList
	 * @return 字列表
	 */
	public Stream<IRecord> subListS(final Integer fromIndex, final Integer toIndex) {
		final int n = this.rowsData.length;
		final IRecord[] rr = Arrays.copyOfRange(this.rowsData, fromIndex == null ? 0 : fromIndex,
				null == toIndex || toIndex >= n ? n : toIndex);

		return Stream.of(rr);
	}

	/**
	 * 行顺序调转
	 * 
	 * @return 新的DFrame
	 */
	public DFrame reverse() {
		final List<IRecord> rows = this.rows();
		Collections.reverse(rows);
		return DFrame.of(rows);
	}

	/**
	 * 列表排序
	 * 
	 * @return 新的DFrame
	 */
	public DFrame sorted() {
		return this.rowS().collect(DFrame.dfmclc);
	}

	/**
	 * 列表排序
	 * 
	 * @param comparator (a,b)->int
	 * @return 新的DFrame
	 */
	public DFrame sorted(final Comparator<? super IRecord> comparator) {
		return this.rowS().sorted(comparator).collect(DFrame.dfmclc);
	}

	/**
	 * Functor 变换
	 * 
	 * @param mapper 数据映射 rec->rec1
	 * @return 新的DFrame
	 */
	public DFrame fmap(final Function<IRecord, IRecord> mapper) {
		return this.rowS(mapper).collect(DFrame.dfmclc);
	}

	/**
	 * Inserts the specified element at the specified position in this DFrame.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 *
	 * @param index   index at which the specified element is to be inserted
	 * @param element element to be inserted
	 * @return DFrame
	 */
	public DFrame insert(final int index, IRecord element) {
		final LinkedList<IRecord> ll = this.rows();
		ll.add(index, element);
		return new DFrame(ll);
	}

	/**
	 * Inserts the specified element at the specified position in this DFrame.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 *
	 * @param index    index at which the specified element is to be inserted
	 * @param elements collection containing elements to be added to this DFrame
	 * @return 新对象 DFrame
	 */
	public DFrame insert(final int index, Collection<IRecord> elements) {
		final LinkedList<IRecord> ll = this.rows();
		ll.addAll(index, elements);
		return new DFrame(ll);
	}

	/**
	 * Inserts the specified element at the specified position in this DFrame.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 *
	 * @param index    index at which the specified element is to be inserted
	 * @param elements elements to be inserted
	 * @return 新对象 DFrame
	 */
	public DFrame insert(final int index, IRecord... elements) {
		return insert(index, Arrays.asList(elements));
	}

	/**
	 * 数据分组
	 * 
	 * @param <K>        the type of the keys
	 * @param <A>        the intermediate accumulation type of the downstream
	 *                   collector
	 * @param <M>        the type of the resulting Map
	 * @param <D>        the result type of the downstream reduction
	 * @param classifier a classifier function mapping input elements to keys
	 * @param mapFactory a supplier providing a new empty Mapinto which the results
	 *                   will be inserted
	 * @param downstream a Collector implementing the downstream reduction
	 * @return M
	 */
	public <K, A, M extends Map<K, D>, D> M groupBy(Function<? super IRecord, ? extends K> classifier,
			final Supplier<M> mapFactory, final Collector<? super IRecord, A, D> downstream) {
		return this.collect(Collectors.groupingBy(classifier, mapFactory, downstream));
	}

	/**
	 * 
	 * 数据分组
	 * 
	 * @param <K>        the type of the keys
	 * @param classifier a classifier function mapping input elements to keys
	 * @param mapFactory a supplier providing a new empty Mapinto which the results
	 *                   will be inserted
	 * @param downstream a Collector implementing the downstream reduction
	 * @return LinkedHashMap
	 */
	public <K> LinkedHashMap<K, List<IRecord>> groupBy(Function<? super IRecord, ? extends K> classifier) {
		return this.groupBy(classifier, LinkedHashMap::new, Collectors.toList());
	}

	/**
	 * 转换成映射 结构
	 * 
	 * @param <K>           the type of the keys
	 * @param <V>           the output type of the value mapping function
	 * @param <M>           the type of the resulting Map
	 * @param keyMapper     a mapping function to produce keys
	 * @param valueMapper   a mapping function to produce values
	 * @param mergeFunction a merge function, used to resolve collisions
	 *                      betweenvalues associated with the same key, as
	 *                      suppliedto Map.merge(Object, Object, BiFunction)
	 * @param mapFactory    a supplier providing a new empty Mapinto which the
	 *                      results will be inserted
	 * @return M
	 */
	public <K, V, M extends Map<K, V>> M toMap(final Function<? super IRecord, K> keyMapper,
			final Function<? super IRecord, V> valueMapper, final BinaryOperator<V> mergeFunction,
			Supplier<M> mapFactory) {
		return this.collect(Collectors.toMap(keyMapper, valueMapper, mergeFunction, mapFactory));
	}

	/**
	 * 转换成映射 结构
	 * 
	 * @param <K>         the type of the keys
	 * @param <V>         the output type of the value mapping function
	 * @param keyMapper   a mapping function to produce keys
	 * @param valueMapper a mapping function to produce values
	 * @return LinkedHashMap
	 */
	public <K, V> LinkedHashMap<K, V> toMap(final Function<? super IRecord, K> keyMapper,
			final Function<? super IRecord, V> valueMapper) {
		return this.collect(Collectors.toMap(keyMapper, valueMapper, (a, b) -> b, LinkedHashMap::new));
	}

	/**
	 * 转换成映射 结构
	 * 
	 * @param <K>         the type of the keys
	 * @param <V>         the output type of the value mapping function
	 * @param keyMapper   a mapping function to produce keys
	 * @param valueMapper a mapping function to produce values
	 * @return
	 */
	public <K, V> LinkedHashMap<K, IRecord> toMap(final Function<? super IRecord, K> keyMapper) {
		return this.collect(Collectors.toMap(keyMapper, e -> e, (a, b) -> b, LinkedHashMap::new));
	}

	/**
	 * 转换成映射 结构(依据指定键名）
	 * 
	 * @param <K> 键名类型
	 * @param key 键名
	 * @return
	 */
	public <K> LinkedHashMap<K, IRecord> toMap(final String key) {
		return this.toMap(e -> e.get(key));
	}

	/**
	 * 转换成映射 结构（根据指定键名索引）
	 *
	 * @param <K> 键名类型
	 * @param idx 键名索引从开始
	 * @return
	 */
	public <K> LinkedHashMap<K, IRecord> toMap(final Integer idx) {
		return this.toMap(e -> e.get(idx));
	}

	/**
	 * 转换成映射 结构（根据第一列）
	 * 
	 * @param <K> 键名类型
	 * @return
	 */
	public <K> LinkedHashMap<K, IRecord> toMap() {
		return this.toMap(e -> e.get(0));
	}

	/**
	 * iterator
	 */
	@Override
	public Iterator<IRecord> iterator() {
		return this.rows().iterator();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { DFrame.class, this.rowsData });
	}

	@Override
	public boolean equals(final Object obj) {
		return obj == this
				|| (obj instanceof DFrame dfm ? this.rowsData.equals(dfm.rowsData) && this.colsData.equals(dfm.colsData)
						: false);
	}

	/**
	 * 格式化
	 */
	@Override
	public String toString() {
		return fmt(Arrays.asList(this.rowsData));
	}

	/**
	 * 创建一个 DFrame 对象
	 * 
	 * @param data 源数据
	 * @return DFrame
	 */
	public static DFrame of(final List<IRecord> data) {
		return new DFrame(data);
	}

	/**
	 * DFrame 數據的格式化
	 * 
	 * @param records 源数据
	 * @return 數據的格式化
	 */
	public static String fmt(final List<IRecord> records) {
		if (records.size() > 0) {
			final StringBuffer buffer = new StringBuffer();
			final IRecord first = records.get(0);
			final List<String> keys = first.keys();
			final String header = keys.stream().collect(Collectors.joining("\t"));
			final String body = records.stream() //
					.map(e -> keys.stream().map(e::str).collect(Collectors.joining("\t"))) // 行數據格式化
					.collect(Collectors.joining("\n"));
			buffer.append(header + "\n");
			buffer.append(body);
			return buffer.toString();
		} else {
			return "DFrame(empty)";
		}

	}

	/**
	 * LinkedList clc
	 * 
	 * @param <T> 归集元素类型
	 * @return LinkedList clc
	 */
	public static <T> Collector<T, ?, LinkedList<T>> llclc() {
		return Collector.of(LinkedList::new, LinkedList::add, (aa, bb) -> {
			return aa;
		});
	}

	/**
	 * DFrame 归集器 (IRecord 类型)
	 * 
	 * @param <T>    归集的元素类型
	 * @param mapper t-&gt;rec
	 * @return [o]-&gt;dfm
	 */
	public static <T> Collector<T, ?, DFrame> dfmclc(final Function<T, IRecord> mapper) {
		return Collector.of((Supplier<List<IRecord>>) ArrayList::new, (acc, t) -> acc.add(mapper.apply(t)),
				(left, right) -> {
					left.addAll(right);
					return left;
				}, e -> {
					return new DFrame(e);
				});
	}

	/**
	 * DFrame 归集器 ( Map 类型 )
	 * 
	 * @param <T>    归集的元素类型
	 * @param mapper t-&gt;map
	 * @return [o]-&gt;dfm
	 */
	public static <T> Collector<T, ?, DFrame> dfmclc2(final Function<T, Map<?, ?>> mapper) {

		return Collector.of((Supplier<List<IRecord>>) ArrayList::new, (acc, t) -> acc.add(IRecord.REC(mapper.apply(t))),
				(left, right) -> {
					left.addAll(right);
					return left;
				}, e -> {
					return new DFrame(e);
				});
	}

	/**
	 * DFrame 归集器
	 */
	public static Collector<IRecord, ?, DFrame> dfmclc = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
			List::add, (left, right) -> {
				left.addAll(right);
				return left;
			}, e -> {
				return new DFrame(e);
			});

	/**
	 * DFrame 归集器
	 */
	public static Collector<Map<?, ?>, ?, DFrame> dfmclc2 = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
			(aa, a) -> aa.add(new MyRecord(a)), (left, right) -> {
				left.addAll(right);
				return left;
			}, e -> {
				return new DFrame(e);
			});

	/**
	 * 列数据
	 */
	protected transient LinkedHashMap<String, ArrayList<Object>> colsData = null; // 列数据
	/**
	 * 行数据
	 */
	protected final IRecord[] rowsData; // 行数据

}
