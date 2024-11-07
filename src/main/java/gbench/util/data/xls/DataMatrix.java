package gbench.util.data.xls;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * DataMatrix(数据矩阵/二维对象数组)
 * 
 * @author gbench
 *
 * @param <T> 矩阵元素类型
 */
public class DataMatrix<T> implements Iterable<T[]> {

	/**
	 * 数据矩阵 <br>
	 * 首行数据为表头 <br>
	 *
	 * @param cells 数据
	 */
	public DataMatrix(final T[][] cells) {
		this(cells, null);
	}

	/**
	 * 数据矩阵
	 *
	 * @param cells 数据
	 * @param keys  表头定义，null 则首行为表头
	 */
	public DataMatrix(final T[][] cells, final Iterable<String> keys) {
		this.initialize(cells, keys);
	}

	/**
	 * 数据矩阵:使用cells的数据初始化一个数据矩阵,注意当keys为null的时候采用cells的第一行作为数据标题。
	 *
	 * @param cells 数据:
	 * @param keys  表头定义，null 则首行为表头,对于keys短与width的时候, 使用编号的excel名称,名称用“_”作为开头。
	 * @return DataMatrix&lt;T&gt; 对象自身
	 */
	public DataMatrix<T> initialize(final T[][] cells, final Iterable<String> keys) {
		final List<String> final_keys = new LinkedList<>();
		this.cells = cells;
		if (keys == null) {// 采用数据的第一行作为表头
			final_keys.addAll(Arrays.stream(cells[0]).map(e -> e + "").collect(Collectors.toList()));
			this.cells = removeFirstLine(cells);
		} else {
			for (final String key : keys) {
				final_keys.add(key);
			} // for
		}

		final int n = this.width();// 矩阵的列宽,每行的数据长度
		final ListIterator<String> itr = final_keys.listIterator();
		for (int i = 0; i < n; i++) {// 诸列检查
			if (!itr.hasNext()) {// 使用excelname 来进行补充列表的补填。
				itr.add("_" + index_to_excel_name(i));// 使用默认的excel名称加入一个下划线前缀
			} else {
				itr.next();// 步进到下一个位置
			} // if !itr.hasNext()
		} // for i

		// 设置键名列表
		this.setKeys(final_keys.subList(0, n));// 设置表头，表头与键名列表同义

		return this;
	}

	/**
	 * MatrixBuilder 纠正构建器
	 * 
	 * @param 数据
	 * @return
	 */
	public DataMatrix<T> mb(final T[][] data) {
		return DataMatrix.of(data, this.keys());
	}

	/**
	 * MatrixBuilder 纠正构建器
	 * 
	 * @param 数据
	 * @return
	 */
	public DataMatrix<T> mb(final T[] data) {
		return Optional.ofNullable(data).map(d -> {
			@SuppressWarnings("unchecked")
			final var dd = (T[][]) Array.newInstance(data.getClass().getComponentType(), 1, d.length);
			return DataMatrix.of(dd, this.keys());
		}).orElse(null);
	}

	/**
	 * 初始化一个空矩阵（元素内容均为null)
	 *
	 * @param m         行数
	 * @param n         列数
	 * @param cellClass 元素类型：null 默认为Object.class
	 * @param keys      列名序列,null 使用excelname 进行默认构造
	 * @return DataMatrix&lt;T&gt; 对象自身
	 */
	@SuppressWarnings("unchecked")
	public DataMatrix<T> initialize(final int m, final int n, final Class<T> cellClass, final List<String> keys) {
		final Class<T> finalCellClass = cellClass != null ? cellClass : (Class<T>) Object.class;// 元素类型默认为Object类型
		this.cells = (T[][]) Array.newInstance(finalCellClass, m, n);
		final List<String> final_keys = keys == null ? Stream.iterate(0, i -> i + 1)
				.map(DataMatrix::index_to_excel_name).limit(n).collect(Collectors.toList()) : keys;

		this.setKeys(final_keys);// 设置表头

		return this;
	}

	/**
	 * 数值
	 * 
	 * @param i 行索引从0开始
	 * @param j 列索引从0开始
	 * @return T值
	 */
	public T get(final int i, final int j) {
		return cells[i][j];
	}

	/**
	 * 数值
	 * 
	 * @param i     行索引从0开始
	 * @param j     列索引从0开始
	 * @param value 数值对象
	 * @return T值
	 */
	public DataMatrix<T> set(final int i, final int j, final T value) {
		cells[i][j] = value;
		return this;
	}

	/**
	 * 矩阵的宽度 ，列数
	 *
	 * @return 矩阵的宽度 ，列数
	 */
	public int width() {
		return shape(this.cells)._2;
	}

	/**
	 * 矩阵高度：行数
	 *
	 * @return 行数
	 */
	public int height() {
		return shape(this.cells)._1;
	}

	/**
	 * 表头，列名字段序列(keys别名）
	 *
	 * @return 返回 数据 的表头，列名序列
	 */
	public List<String> title() {
		return this.keys();
	}

	/**
	 * 表头，列名字段序列(keys别名）
	 *
	 * @param keys 列名字段序列，用逗号分隔
	 * @return 当前对象this
	 */
	public DataMatrix<T> title(final String keys) {
		return this.keys(keys);
	}

	/**
	 * 表头，列名字段序列(keys别名）
	 *
	 * @param keys 列名字段序列
	 * @return 当前对象this
	 */
	public DataMatrix<T> title(final String[] keys) {
		return this.keys(keys);
	}

	/**
	 * 表头，列名字段序列(keys别名）
	 *
	 * @param keys 列名字段序列
	 * @return 当前对象this
	 */
	public DataMatrix<T> title(final Iterable<String> keys) {
		return this.keys(keys);
	}

	/**
	 * 表头，列名字段序列:
	 *
	 * @return 返回 数据 的表头，列名序列
	 */
	public Stream<String> keyS() {
		return this.keymetas.entrySet().stream().sorted(Comparator.comparingInt(Entry::getValue)).map(Entry::getKey);
	}

	/**
	 * 表头，列名字段序列:
	 *
	 * @return 返回 数据 的表头，列名序列
	 */
	public List<String> keys() {
		return this.keyS().collect(Collectors.toList());
	}

	/**
	 * 获取并设置健名（列名）索引
	 *
	 * @param keys 列名字段序列，用逗号分隔
	 * @return 当前对象this
	 */
	public DataMatrix<T> keys(final String keys) {
		return this.setKeys(keys);
	}

	/**
	 * 获取并设置健名（列名）索引(setKeys别名）
	 *
	 * @param keys 列名字段序列，用逗号分隔
	 * @return 当前对象this
	 */
	public DataMatrix<T> keys(final String[] keys) {
		return this.setKeys(keys);
	}

	/**
	 * 获取并设置健名（列名）索引(setKeys别名）
	 *
	 * @param keys 列名字段序列，用逗号分隔
	 * @return 当前对象this
	 */
	public DataMatrix<T> keys(final Iterable<String> keys) {
		return this.setKeys(keys);
	}

	/**
	 * 获取并设置健名（列名）索引
	 *
	 * @param keys 列名字段序列，用逗号分隔
	 * @return 当前对象this
	 */
	public DataMatrix<T> setKeys(final String keys) {
		return this.setKeys(keys.split("[,，]+"));
	}

	/**
	 * 获取并设置健名（列名）索引
	 *
	 * @param keys 列名字段序列
	 * @return 当前对象this
	 */
	public DataMatrix<T> setKeys(final String[] keys) {
		return this.setKeys(Arrays.asList(keys));
	}

	/**
	 * 获取并设置健名（列名）索引
	 *
	 * @param keys 列名字段序列
	 * @return 当前对象this
	 */
	public DataMatrix<T> setKeys(final Iterable<?> keys) {
		final Map<String, Integer> keysMap = new HashMap<>();
		final var n = this.width(); // 列数量
		if (keys == null) {
			final List<String> _keys = this.keys();
			for (int i = 0; i < Math.min(_keys.size(), n); i++) {
				keysMap.put(_keys.get(i), i);
			} // for
		} else {
			int i = 0;
			for (final Object key : keys) {
				if (i >= n) { // 剔除掉多余的键名
					break;
				} else { // 加入键名
					keysMap.put(String.valueOf(key), i++);
				} // if
			} // for
			if (keysMap.size() < n) { // 补充长度
				for (; i < n; i++) {
					keysMap.put("_" + DataMatrix.xlsn(i), i);
				}
			}
			this.setKeymetas(keysMap);
		} // if

		return this;
	}

	/**
	 * map
	 * 
	 * @param <U>    U
	 * @param mapper mapper [t]-&gt;u
	 * @return U 类型的类型的流
	 */
	public <U> Stream<U> map(final Function<T[], U> mapper) {
		return Arrays.stream(this.data()).map(mapper);
	}

	/**
	 * 去掉最后一行元素的剩余部分
	 * 
	 * @return
	 */
	public T[][] initial() {
		return this.initialOpt().orElse(null);
	}

	/**
	 * 去掉最后一行元素的剩余部分
	 * 
	 * @return
	 */
	public DataMatrix<T> initialmx() {
		return this.initialOpt().map(this::mb).orElse(null);
	}

	/**
	 * 去掉最后一行元素的剩余部分
	 * 
	 * @return
	 */
	public Optional<T[][]> initialOpt() {
		return Optional.ofNullable(this.cells)
				.map(cs -> cs.length > 1 ? Arrays.copyOfRange(this.cells, 0, this.cells.length - 1) : null);
	}

	/**
	 * 去掉首行元素的剩余部分
	 * 
	 * @return
	 */
	public T[][] tail() {
		return this.tailOpt().orElse(null);
	}

	/**
	 * 去掉首行元素的剩余部分
	 * 
	 * @return
	 */
	public DataMatrix<T> tailmx() {
		return this.tailOpt().map(this::mb).orElse(null);
	}

	/**
	 * 去掉首行元素的剩余部分
	 * 
	 * @return
	 */
	public Optional<T[][]> tailOpt() {
		return Optional.ofNullable(this.cells)
				.map(cs -> cs.length > 1 ? Arrays.copyOfRange(this.cells, 1, this.cells.length) : null);
	}

	/**
	 * 首行元素
	 * 
	 * @return
	 */
	public T[] head() {
		return this.headOpt().orElse(null);
	}

	/**
	 * 首行元素
	 * 
	 * @return
	 */
	public DataMatrix<T> headmx() {
		return this.headOpt().map(this::mb).orElse(null);
	}

	/**
	 * 首行数据元素
	 * 
	 * @return
	 */
	public Optional<T[]> headOpt() {
		return Optional.ofNullable(this.cells).map(cs -> cs.length > 0 ? cs[0] : null);
	}

	/**
	 * 尾行数据元素
	 * 
	 * @return
	 */
	public T[] last() {
		return this.lastOpt().orElse(null);
	}

	/**
	 * 尾行数据元素
	 * 
	 * @return
	 */
	public DataMatrix<T> lastmx() {
		return this.lastOpt().map(this::mb).orElse(null);
	}

	/**
	 * 尾行元素
	 * 
	 * @return
	 */
	public Optional<T[]> lastOpt() {
		return Optional.ofNullable(this.cells).map(cs -> cs.length > 0 ? cs[cs.length - 1] : null);
	}

	/**
	 * 行列表
	 *
	 * @param<U> 变换的结果类型
	 * @param mapper 行元素变换器: [t]->{}
	 * @return 行列表 集合
	 */
	public <U> List<U> rows(final Function<T[], U> mapper) {
		return Arrays.stream(this.cells).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 行列表
	 *
	 * @return 行列表 集合
	 */
	public List<List<T>> rows() {
		return rows(Arrays::asList);
	}

	/**
	 * 行列表
	 *
	 * @param i 行索引 从0开始
	 * @return 行元素
	 */
	public Optional<List<T>> rowOpt(final int i) {
		return Optional.ofNullable(i >= this.height() ? null : this.rows().get(i));
	}

	/**
	 * 行数据
	 *
	 * @param i 行索引 从0开始
	 * @return 行元素
	 */
	public List<T> row(final int i) {
		return this.rowOpt(i).orElse(null);
	}

	/**
	 * 行列表
	 *
	 * @param i 行索引 从0开始
	 * @return 行元素
	 */
	public <U> U row(final int i, final Function<? super Iterable<T>, U> mapper) {
		return this.rowOpt(i).map(mapper).orElse(null);
	}

	/**
	 * 行列表
	 *
	 * @param i 行索引 从0开始
	 * @return 行元素
	 */
	public Stream<T> rowDataS(final int i) {
		return this.rowOpt(i).map(e -> e.stream()).orElse(null);
	}

	/**
	 * 按照行进行映射
	 *
	 * @param <U>    目标行记录{(String,T)} 所变换成的结果类型
	 * @param mapper 行数据映射 LinkedHashMap&lt;String,T&gt;的结构, key-&gt;value
	 * @return U 类型的流
	 */
	public <U> Stream<U> rowS(final Function<LinkedHashMap<String, T>, U> mapper) {
		final String[] keys = this.keys().toArray(new String[0]);
		final int hn = keys.length;// 表头长度
		@SuppressWarnings("unchecked")
		Function<LinkedHashMap<String, T>, U> final_mapper = mapper == null ? e -> (U) e : mapper;
		return this.rows().stream().map(row -> {
			final int n = row.size();
			final LinkedHashMap<String, T> mm = new LinkedHashMap<>();
			for (int i = 0; i < n; i++)
				mm.put(keys[i % hn], row.get(i));
			return final_mapper.apply(mm);
		});// lrows
	}

	/**
	 * 行列表
	 *
	 * @param <U>    结果流的元素类型
	 * @param mapper 行元素变换器: [t]-&gt;u
	 * @return 行列表 流
	 */
	public <U> Stream<U> row2S(final Function<T[], U> mapper) {
		return Arrays.stream(this.cells).map(mapper);
	}

	/**
	 * 行列表
	 *
	 * @return 行列表 流
	 */
	public Stream<T[]> row2S() {
		return Arrays.stream(this.cells).map(e -> e);
	}

	/**
	 * 按照行进行映射
	 *
	 * @param <U>    结果
	 * @param mapper 列变换函数 (k,tt)->u
	 * @return U 类型的流
	 */
	public <U> Stream<U> colS(final Function<Tuple2<String, T[]>, U> mapper) {
		final T[][] tt = DataMatrix.transpose(this.data());
		return this.keyS().map(Tuple2.snb(0)).map(e -> Tuple2.of(e._2, tt[e._1])).map(mapper);
	}

	/**
	 * 按照行进行映射
	 *
	 * @return U 类型的流
	 */
	public Stream<Tuple2<String, T[]>> colS() {
		return this.colS(e -> e);
	}

	/**
	 * 按照行进行映射
	 *
	 * @return U 类型的流
	 */
	public List<Tuple2<String, T[]>> cols() {
		return this.colS().collect(Collectors.toList());
	}

	/**
	 * 行列表
	 *
	 * @param j 列索引 从0开始
	 * @return 行元素
	 */
	public List<T> col(final int j) {
		return this.colOpt(j).orElse(null);
	}

	/**
	 * 列数据
	 *
	 * @param j 列索引 从0开始
	 * @return 行元素
	 */
	public <U> U col(final int j, final Function<? super Iterable<T>, U> mapper) {
		return this.colOpt(j).map(mapper).orElse(null);
	}

	/**
	 * 列列表
	 *
	 * @param j 列索引 从0开始
	 * @return 列列表
	 */
	public Optional<List<T>> colOpt(final int j) {
		return Optional.ofNullable(j >= this.width() ? null
				: this.colS().map(e -> Arrays.asList(e._2)).collect(Collectors.toList()).get(j));
	}

	/**
	 * 列列表
	 *
	 * @param key 列名
	 * @return 列列表
	 */
	public Optional<List<T>> colOpt(final String key) {
		return this.colOpt(this.indexOf(key));
	}

	/**
	 * 行列表
	 *
	 * @param key 列索引 从0开始
	 * @return 行元素
	 */
	public List<T> col(final String key) {
		return this.col(this.indexOf(key));
	}

	/**
	 * 列元素流
	 *
	 * @param j 列索引 从0开始
	 * @return 列元素流
	 */
	public Stream<T> colDataS(final int j) {
		return this.colOpt(j).map(e -> e.stream()).orElse(null);
	}

	/**
	 * 列元素流
	 *
	 * @param key 列名
	 * @return 列元素流
	 */
	public Stream<T> colDataS(final String key) {
		return this.colDataS(this.indexOf(key));
	}

	/**
	 * 按照行进行映射
	 *
	 * @return U 类型的流
	 */
	public Stream<T[]> col2S() {
		return this.colS(e -> e._2);
	}

	/**
	 * 按照行进行映射
	 *
	 * @param <U>    值类型
	 * @param mapper [t]->u 列变换函数
	 * @return U 类型的流
	 */
	public <U> Stream<U> col2S(final Function<T[], U> mapper) {
		return this.col2S().map(mapper);
	}

	/**
	 * 获取列名索引
	 *
	 * @param key 列名
	 * @return 列名索引
	 */
	public int indexOf(final String key) {
		return this.keymetas.get(key);
	}

	/**
	 * 获取列名
	 *
	 * @param idx 列名索引从0开始
	 * @return 列名
	 */
	public String keyOf(final int idx) {
		return this.keymetas.entrySet().stream().filter(e -> e.getValue() == idx).map(e -> e.getKey()).findFirst()
				.orElse(null);
	}

	/**
	 * 设置单元格
	 *
	 * @param i     行索引
	 * @param j     列索引
	 * @param value 数据值
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> setCell(final int i, final int j, final T value) {
		final Tuple2<Integer, Integer> shape = this.shape();
		if (i < shape._1 && j < shape._2) {
			this.cells[i][j] = value;
		}
		return this;
	}

	/**
	 * 设置行
	 *
	 * @param idx 行索引 从 0开始
	 * @param row 行数据
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> setRow(final int idx, final Iterable<T> row) {
		final AtomicInteger ai = new AtomicInteger();
		if (idx < this.height()) {
			final T[] tt = this.cells[idx];
			final int n = tt.length;
			if (n > 0) { //
				for (final T t : row) {
					final int i = ai.getAndIncrement();
					if (i < n) { // 索引有效
						tt[i] = t;
					} else { // 索引无效
						break;
					} // if
				} // for
			} // if
		} // if
		return this;
	}

	/**
	 * 行操作
	 *
	 * @param i      行索引从 0开始
	 * @param mapper 行操作 ([i:元素索引从0开始,t])->[t]
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> withRowS(final int i, final Function<Stream<Tuple2<Integer, T>>, Stream<T>> mapper) {
		this.rowOpt(i).ifPresent(e -> {
			final List<T> _e = mapper.apply(e.stream().map(Tuple2.snb(0))).collect(Collectors.toList());
			this.setRow(i, _e);
		});
		return this;
	}

	/**
	 * 行操作
	 *
	 * @param i      行索引从 0开始
	 * @param action 行操作 [t]->{}
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> withRow(final int i, final Consumer<List<T>> action) {
		this.rowOpt(i).ifPresent(e -> {
			action.accept(e);
			this.setRow(i, e);
		});
		return this;
	}

	/**
	 * 设置列
	 *
	 * @param idx 列索引 从 0开始
	 * @param col 列数据
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> setColumn(final int idx, final Iterable<T> col) {
		final AtomicInteger ai = new AtomicInteger();
		final int height = this.height();
		if (idx < this.width()) {
			for (final T t : col) {
				final int i = ai.getAndIncrement();
				if (i < height) {
					final T[] tt = this.cells[i];
					if (idx < tt.length) {
						tt[idx] = t;
					}
				} else {
					break;
				}
			}
		}
		return this;
	}

	/**
	 * 设置列
	 *
	 * @param key 列名
	 * @param col 列数据
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> setColumn(final String key, final Iterable<T> col) {
		return this.setColumn(this.indexOf(key), col);
	}

	/**
	 * 列操作
	 *
	 * @param j      列索引从 0开始
	 * @param mapper 列操作 ([i:元素索引从0开始,t])->[t]
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> withColumnS(final int j, final Function<Stream<Tuple2<Integer, T>>, Stream<T>> mapper) {
		this.colOpt(j).ifPresent(e -> {
			final List<T> _e = mapper.apply(e.stream().map(Tuple2.snb(0))).collect(Collectors.toList());
			this.setColumn(j, _e);
		});
		return this;
	}

	/**
	 * 列操作
	 *
	 * @param key    列名
	 * @param mapper 列操作 ([i:元素索引从0开始,t])->[t]
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> withColumnS(final String key, final Function<Stream<Tuple2<Integer, T>>, Stream<T>> mapper) {
		return this.withColumnS(this.indexOf(key), mapper);
	}

	/**
	 * 列操作
	 *
	 * @param j      列索引从 0开始
	 * @param action 列操作 [t]->{}
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> withColumn(final int j, final Consumer<List<T>> action) {
		this.colOpt(j).ifPresent(e -> {
			action.accept(e);
			this.setColumn(j, e);
		});
		return this;
	}

	/**
	 * 列操作
	 *
	 * @param key    列名
	 * @param action 列操作 [t]->{}
	 * @return 矩阵对象本身
	 */
	public DataMatrix<T> withColumn(final String key, final Consumer<List<T>> action) {
		return this.withColumn(this.indexOf(key), action);
	}

	/**
	 * 设置矩阵的表头字段序列
	 *
	 * @param keymetas {(name,id:从0开始)}的字段序列
	 * @return 设置成功的 表头字段序列：{(name,id)}的字段序列
	 */
	public Map<String, Integer> setKeymetas(final Map<String, Integer> keymetas) {
		this.keymetas = keymetas;
		return keymetas;
	}

	/**
	 * 矩阵转置
	 *
	 * @return 矩阵转置
	 */
	public DataMatrix<T> tp() {
		return this.transpose();
	}

	/**
	 * 矩阵转置
	 *
	 * @return 矩阵转置
	 */
	public DataMatrix<T> transpose() {
		final T[][] dd = DataMatrix.transpose(this.data());
		final Integer n = shape(dd)._2;
		final List<String> keys = intS(n).map(DataMatrix::index_to_excel_name).collect(Collectors.toList());
		return new DataMatrix<T>(dd, keys);
	}

	/**
	 * 类型装换
	 *
	 * @param <U>     目标结果的类型
	 * @param corecer 类型变换函数
	 * @return 类型变换
	 */
	@SuppressWarnings("unchecked")
	public <U> DataMatrix<U> corece(final Function<T, U> corecer) {
		final T[][] cc = this.cells;

		try {
			final int m = this.height();
			final int n = this.width();
			final List<U> ulist = Arrays.stream(cc).flatMap(Arrays::stream).map(corecer).collect(Collectors.toList());// 找到一个非空的数据类型
			final Optional<Class<U>> opt = ulist.stream().filter(Objects::nonNull).findFirst()
					.map(e -> (Class<U>) e.getClass()); // 提取费控的类型
			final Class<U> cls = opt.orElseGet(() -> (Class<U>) (Object) Object.class);
			final U[][] uu = (U[][]) Array.newInstance(cls, m, n);
			final Iterator<U> itr = ulist.iterator();

			for (int i = 0; i < m; i++) {
				for (int j = 0; j < n; j++) {
					uu[i][j] = itr.hasNext() ? itr.next() : null;
				}
			}

			return new DataMatrix<>(uu, this.keys());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} // try
	}

	/**
	 * 增加数据预处理函数，只改变数据内容并改变数据形状shape:比如 无效非法值，缺失值，数字格式化等功能。
	 *
	 * @param handler 行数据映射 LinkedHashMap&lt;String,T&gt;的结构, key-&gt;value
	 * @return DataMatrix
	 */
	public DataMatrix<T> preProcess(final Consumer<T[]> handler) {
		for (int i = 0; i < this.height(); i++)
			handler.accept(cells[i]);
		return this;
	}

	/**
	 * 加法
	 *
	 * @param <U> 右侧矩阵类型
	 * @param uu  右侧矩阵
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> add(final DataMatrix<U> uu) {
		return DataMatrix.binaryOp(this.corece(DataMatrix::todbl), uu.corece(DataMatrix::todbl), (a, b) -> Optional
				.ofNullable(a).map(_a -> Optional.ofNullable(b).map(_b -> _a + _b).orElse(null)).orElse(null));
	}

	/**
	 * 加法
	 *
	 * @param <U> 右侧元素类型
	 * @param u   右侧元素
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> add(final U u) {
		return this.add(DataMatrix.oneXone(u));
	}

	/**
	 * 减法
	 *
	 * @param <U> 右侧矩阵类型
	 * @param uu  右侧矩阵
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> sub(final DataMatrix<U> uu) {
		return DataMatrix.binaryOp(this.corece(DataMatrix::todbl), uu.corece(DataMatrix::todbl), (a, b) -> Optional
				.ofNullable(a).map(_a -> Optional.ofNullable(b).map(_b -> _a - _b).orElse(null)).orElse(null));
	}

	/**
	 * 减法
	 *
	 * @param <U> 右侧元素类型
	 * @param u   右侧元素
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> sub(final U u) {
		return this.sub(DataMatrix.oneXone(u));
	}

	/**
	 * 乘法
	 *
	 * @param <U> 右侧矩阵类型
	 * @param uu  右侧矩阵
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> mul(final DataMatrix<U> uu) {
		return DataMatrix.binaryOp(this.corece(DataMatrix::todbl), uu.corece(DataMatrix::todbl), (a, b) -> Optional
				.ofNullable(a).map(_a -> Optional.ofNullable(b).map(_b -> _a * _b).orElse(null)).orElse(null));
	}

	/**
	 * 乘法
	 *
	 * @param <U> 右侧元素类型
	 * @param u   右侧元素
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> mul(final U u) {
		return this.mul(DataMatrix.oneXone(u));
	}

	/**
	 * 除法
	 *
	 * @param <U> 右侧矩阵类型
	 * @param uu  右侧矩阵
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> div(final DataMatrix<U> uu) {

		return DataMatrix.binaryOp(this.corece(DataMatrix::todbl), uu.corece(DataMatrix::todbl), (a, b) -> Optional
				.ofNullable(a).map(_a -> Optional.ofNullable(b).map(_b -> _a / _b).orElse(null)).orElse(null));
	}

	/**
	 * 除法
	 *
	 * @param <U> 右侧元素类型
	 * @param u   右侧元素
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> div(final U u) {
		return this.div(DataMatrix.oneXone(u));
	}

	/**
	 * 维度尺寸
	 *
	 * @return (行数, 列数)
	 */
	public Tuple2<Integer, Integer> shape() {
		return DataMatrix.shape(this.data());
	}

	/**
	 * 数据矩阵变换
	 *
	 * @param <U>    结果类型
	 * @param mapper dmx->u
	 * @return U类型
	 */
	public <U> U mutate(final Function<? super DataMatrix<T>, U> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 矩阵乘法
	 *
	 * @param <U> 右矩阵元素类型
	 * @param uu  右矩阵类型
	 * @return [[dbl]]
	 */
	public <U> DataMatrix<Double> mmult(final DataMatrix<U> uu) {
		return DataMatrix.mmult(this.corece(DataMatrix::todbl), uu.corece(DataMatrix::todbl),
				(Double a, Double b) -> Optional.ofNullable(a)
						.map(_a -> Optional.ofNullable(b).map(_b -> _a * _b).orElse(null)).orElse(null),
				Collectors.summingDouble(e -> e));
	}

	/**
	 * 复制当前对象
	 *
	 * @param <U>    结果类型
	 * @param mapper ((i:行索引从0开始,j:列索引从0开始),t:元素) -> u
	 * @return 矩阵对象[[u]]
	 */
	@SuppressWarnings("unchecked")
	public <U> DataMatrix<U> fmap(final BiFunction<Tuple2<Integer, Integer>, T, U> mapper) {
		final Tuple2<Integer, Integer> shape = this.shape();
		U[][] datas = null;
		for (int i = 0; i < shape._1; i++) {
			final int n = cells[i].length;
			for (int j = 0; j < n && j < shape._2; j++) {
				final U u = mapper.apply(Tuple2.of(i, j), cells[i][j]);
				if (u == null) {
					continue;
				} else if (datas == null) {
					datas = (U[][]) Array.newInstance(u.getClass(), shape._1, shape._2);
				} // if
				datas[i][j] = u;
			} // for
		} // for

		return new DataMatrix<U>(datas, this.keys());
	}

	/**
	 * 复制当前对象
	 *
	 * @param <U>    结果类型
	 * @param mapper t -> u
	 * @return 矩阵对象[[u]]
	 */
	public <U> DataMatrix<U> fmap(final Function<T, U> mapper) {
		return this.fmap((ij, t) -> mapper.apply(t));
	}

	/**
	 * 复制当前对象
	 *
	 * @return 矩阵对象[[t]]
	 */
	public DataMatrix<T> duplicate() {
		return this.fmap(e -> e);
	}

	/**
	 * submx
	 * 
	 * @param rangeName 区域名称,比如, A1:C10
	 * @return DataMatrix 对象
	 */
	public DataMatrix<T> submx(final String rangeName) {
		final RangeDef rangeDef = DataMatrix.name2rdf(rangeName);
		return this.submx(rangeDef.x0(), rangeDef.y0(), rangeDef.x1(), rangeDef.y1());
	}

	/**
	 * 创建子矩阵
	 * 
	 * @param <U>   cells数组中的元素的数据类型。
	 * @param cells 对象二维数组
	 * @param x0    起点行索引 从0开始 行坐标,x0超过或等于最大值数量height，返回null
	 * @param y0    起点列索引 从0开始 列坐标,x0超过或等于最大值数量height，返回null
	 * @param x1    终点行索引 包含，超过最大行数量采用最大行数量
	 * @param y1    终点列索引 包含，超过最大列数量采用最大列数量
	 * @return 子矩阵
	 */
	public DataMatrix<T> submx(final int x0, int y0, int x1, int y1) {
		final var h = this.height();
		final var w = this.width();

		if (h < 1 || w < 1 || x0 >= h || y0 >= w || x0 < 0 || y0 < 0) { // 非法索引
			return null;
		} else {
			final var _x0 = x0;
			final var _y0 = y0;
			final var _x1 = Math.min(h - 1, x1);
			final var _y1 = Math.min(w - 1, y1);

			final T[][] tt = DataMatrix.submx(this.cells, _x0, _y0, _x1, _y1);
			return DataMatrix.of((Iterable<String>) null, tt);
		}
	}

	/**
	 * 获得一个mxn的子矩阵
	 * 
	 * @param rangeName 区域名称,比如, A1:C10
	 * @return 返回mxn的子矩阵（对于m,n超出矩阵索引范围，元素位置为null）
	 */
	public DataMatrix<T> submxn(final String nameline) {
		final RangeDef rangeDef = DataMatrix.name2rdf(nameline);
		return this.submxn(rangeDef.x0(), rangeDef.y0(), rangeDef.nrows(), rangeDef.ncols());
	}

	/**
	 * 获得一个mxn的子矩阵
	 * 
	 * @param i 行开始坐标从0开始
	 * @param j 列开始坐标从0开始
	 * @param m 子矩阵的行长度
	 * @param n 子矩阵的列长度
	 * @return 返回mxn的子矩阵（对于m,n超出矩阵索引范围，元素位置为null）
	 */
	public DataMatrix<T> submxn(final int i, final int j, final int m, final int n) {
		final var h = this.height(); // 矩阵行数
		final var w = this.width(); // 矩阵列数
		@SuppressWarnings("unchecked")
		final T[][] data = (T[][]) Array.newInstance(DataMatrix.getGenericClass(this.cells), m, n);

		for (int r = 0, p = i; p < h; p++, r++) {
			for (int c = 0, q = j; q < w; q++, c++) {
				if (r < m && c < n) {
					data[r][c] = this.cells[p][q];
				} // if
			} // q
		} // p

		return DataMatrix.of(data, this.keys());
	}

	/**
	 * 数据矩阵变换
	 *
	 * @param <U>    结果类型
	 * @param mapper (kk,tt)->u
	 * @return U类型
	 */
	public <U> U arrayOf(final BiFunction<? super Iterable<String>, T[][], U> mapper) {
		return mapper.apply(this.keys(), this.cells);
	}

	/**
	 * 数据矩阵变换
	 *
	 * @param <U>    结果类型
	 * @param mapper tt->u
	 * @return U类型
	 */
	public <U> U arrayOf(final Function<T[][], U> mapper) {
		return this.arrayOf((kk, tt) -> mapper.apply(tt));
	}

	/**
	 * 数组
	 *
	 * @return T[][] 数组
	 */
	public T[][] toArray() {
		return this.data();
	}

	/**
	 * 转换成字符串矩阵
	 *
	 * @return 字符串矩阵
	 */
	public DataMatrix<String> strmx() {
		return this.fmap(e -> e instanceof String ? (String) e : e + "");
	}

	/**
	 * 数据行叠加
	 *
	 * @param dmx 数据矩阵
	 * @return 行叠加后的数据矩阵
	 */
	public DataMatrix<T> rbind(final DataMatrix<T> dmx) {
		return DataMatrix.rbind(this, dmx);
	}

	/**
	 * 数据行叠加
	 *
	 * @param row 数据矩阵
	 * @return 行叠加后的数据矩阵
	 */
	public DataMatrix<T> rbind(final Iterable<T> row) {
		return DataMatrix.rbind(this, DataMatrix.of(row));
	}

	/**
	 * 数据行叠加
	 *
	 * @param cells 行值序列
	 * @return 行叠加后的数据矩阵
	 */
	@SuppressWarnings("unchecked")
	public DataMatrix<T> rbind(final T... cells) {
		return this.rbind(Arrays.asList(cells));
	}

	/**
	 * 数据列叠加
	 *
	 * @param dmx 数据矩阵
	 * @return 列叠加后的数据矩阵
	 */
	public DataMatrix<T> cbind(final DataMatrix<T> dmx) {
		return DataMatrix.cbind(this, dmx);
	}

	/**
	 * 数据列叠加
	 *
	 * @param key 列名
	 * @param col 列数据
	 * @return 列叠加后的数据矩阵
	 */
	public DataMatrix<T> cbind(final String key, final Iterable<T> col) {
		return DataMatrix.cbind(this, DataMatrix.of(Tuple2.of(key, col)));
	}

	/**
	 * 数据列叠加
	 * 
	 * @param <V> 烈性量类型
	 * @param tup (String,V) 列向量数据
	 * @return 列叠加后的数据矩阵
	 */
	public <V extends Iterable<T>> DataMatrix<T> cbind(final Tuple2<String, V> tup) {
		return DataMatrix.cbind(this, DataMatrix.of(tup));
	}

	/**
	 * 数据列叠加
	 *
	 * @param col 数据矩阵
	 * @return 行叠加后的数据矩阵
	 */
	public DataMatrix<T> cbind(final Iterable<T> col) {
		return DataMatrix.cbind(this, DataMatrix.of(col).transpose());
	}

	/**
	 * 数据列叠加
	 *
	 * @param cells 列值序列
	 * @return 行叠加后的数据矩阵
	 */
	@SuppressWarnings("unchecked")
	public DataMatrix<T> cbind(final T... cells) {
		return this.cbind(Arrays.asList(cells));
	}

	/**
	 * 行数据归集器
	 *
	 * @param <U>       归集器结果对象类型
	 * @param collector [lhm] -> u
	 * @return U 类型的对象
	 */
	public <U> U collect(Collector<? super HashMap<String, T>, ?, U> collector) {
		return this.rowS(e -> e).collect(collector);
	}

	/**
	 * 列数据归集器
	 *
	 * @param <U>       归集器结果对象类型
	 * @param collector [(s,[t])] -> u
	 * @return U 类型的对象
	 */
	public <U> U collect2(Collector<? super Tuple2<String, T[]>, ?, U> collector) {
		return this.colS(e -> e).collect(collector);
	}

	/**
	 * 复制当前对象
	 *
	 * @return 矩阵对象[[t]]
	 */
	@Override
	public DataMatrix<T> clone() {
		return this.duplicate();
	}

	@Override
	public Iterator<T[]> iterator() {
		return Arrays.asList(this.cells).iterator();
	}

	/**
	 * 一个维度的数据流化(行顺序）
	 * 
	 * @return T 类型的数据流
	 */
	public Stream<T> cellS() {
		return this.stream(e -> e);
	}

	/**
	 * 矩阵数据的二维数组
	 *
	 * @return 矩阵数据的二维数组
	 */
	public T[][] data() {
		return this.cells;
	}

	/**
	 * 矩阵数据的二维数组, 一般用于向外暴露cells数据。
	 *
	 * @param <U>    结果类型
	 * @param mapper 变换函数 data->u
	 * @return 矩阵数据的二维数组
	 */
	public <U> U dataOf(final Function<T[][], U> mapper) {
		return mapper.apply(this.cells);
	}

	/**
	 * 一个维度的数据流化
	 * 
	 * @param <U>    结果类型
	 * @param mapper t-&gt;u
	 * @return U 类型的数据流
	 */
	public <U> Stream<U> stream(final Function<T, U> mapper) {
		return StreamSupport.stream(this.spliterator(), false).flatMap(Arrays::stream).map(mapper);
	}

	/**
	 * 格式化输出<br>
	 * <br>
	 * 垂直方向:第一维度 i 增长方向 从上到下 <br>
	 * 水平方向:第二维度 j 增长方向 从左到右 <br>
	 *
	 * @return 格式化输出
	 */
	public String toString() {
		return toString("\t", "\n", null);
	}

	/**
	 * 格式化输出<br>
	 * <br>
	 * 垂直方向:第一维度 i 增长方向 从上到下 <br>
	 * 水平方向:第二维度 j 增长方向 从左到右 <br>
	 * 
	 * @param cell_formatter 单元格格式化
	 * @return 格式化输出
	 */
	public String toString(final Function<Object, String> cell_formatter) {
		return toString("\t", "\n", cell_formatter);
	}

	/**
	 * 矩阵格式化 <br>
	 * <br>
	 * 垂直方向:第一维度 i 增长方向 从上到下 <br>
	 * 水平方向:第二维度 j 增长方向 从左到右 <br>
	 *
	 * @param ident          行内间隔
	 * @param ln             行间间隔
	 * @param cell_formatter 元素格式化
	 * @return 格式化输出的字符串
	 */
	public String toString(final String ident, final String ln, final Function<Object, String> cell_formatter) {

		final Function<Object, String> final_cell_formatter = cell_formatter == null ? e -> {
			String line = "{0}";// 数据格式字符串
			if (e instanceof Number)
				line = (e instanceof Integer || e instanceof Long) ? "{0,number,#}" : "{0,number,0.##}";
			else if (e instanceof Date) {
				line = "{0,Date,yyy-MM-dd HH:mm:ss}";
			}
			return MessageFormat.format(line, e);
		}// 默认的格式化
				: cell_formatter;

		if (cells == null || cells.length < 1 || cells[0] == null || cells[0].length < 1)
			return "";
		final StringBuilder buffer = new StringBuilder();
		final String headline = String.join(ident, this.keys());
		if (!headline.matches("\\s*"))
			buffer.append(headline).append(ln);

		// 按照维度自然顺序（从小打到):i->j给与展开格式化
		for (T[] cell : cells) { // 第一维度
			final int n = (cell != null && cell.length > 0) ? cell.length : 0;
			for (int j = 0; j < n; j++) { // 第二维度
				buffer.append(final_cell_formatter.apply(cell[j])).append(ident);
			} // for j 水平方向
			buffer.append(ln);
		} // for i 垂直方向

		return buffer.toString(); // 返回格式化数据
	}

	/**
	 * 二元组
	 * 
	 * @param <T>
	 * @param <U>
	 */
	public static class Tuple2<T, U> {

		/**
		 * 
		 * @param t
		 * @param u
		 */
		public Tuple2(final T t, final U u) {
			this._1 = t;
			this._2 = u;
		}

		/**
		 * 元素位置互换
		 *
		 * @return (u, t)
		 */
		public Tuple2<U, T> swap() {
			return Tuple2.of(this._2, this._1);
		}

		/**
		 * 1#位置 元素变换
		 *
		 * @param <X>    mapper 的结果类型
		 * @param mapper 元素变化函数 t-&gt;x
		 * @return 变换后的 元素 (x,u)
		 */
		public <X> Tuple2<X, U> fmap1(final Function<T, X> mapper) {
			return Tuple2.of(mapper.apply(this._1), this._2);
		}

		/**
		 * 2#位置 元素变换
		 *
		 * @param <X>    mapper 的结果类型
		 * @param mapper 元素变化函数 u-&gt;x
		 * @return 变换后的 元素 (t,x)
		 */
		public <X> Tuple2<T, X> fmap2(final Function<U, X> mapper) {
			return Tuple2.of(this._1, mapper.apply(this._2));
		}

		/**
		 * 数据格式化
		 */
		@Override
		public String toString() {
			return "(%s, %s)".formatted(this._1, this._2);
		}

		/**
		 * 
		 * @param <T>
		 * @param <U>
		 * @param t
		 * @param u
		 * @return
		 */
		public static <T, U> Tuple2<T, U> of(final T t, final U u) {
			return new Tuple2<>(t, u);
		}

		/**
		 * 序列号生成器 <br>
		 * Serial Number Builder
		 *
		 * @param <T>   元素
		 * @param start 开始号码
		 * @return t-&gt;(int,t) 的标记函数
		 */
		public static <T> Function<T, Tuple2<Integer, T>> snb(final int start) {
			final AtomicInteger sn = new AtomicInteger(start);
			return t -> Tuple2.of(sn.getAndIncrement(), t);
		}

		final T _1;
		final U _2;
	}

	/**
	 * 转换成 浮点数类型
	 *
	 * @param <T> 元素类型
	 * @param t   元素
	 * @return 浮点数
	 */
	@SuppressWarnings("unchecked")
	public final static <T> DataMatrix<T> oneXone(final T t) {
		T[][] tt = null;
		Class<T> tclass = null;
		if (t == null) {
			tclass = (Class<T>) Object.class;
		} else {
			tclass = (Class<T>) t.getClass();
		}
		tt = (T[][]) Array.newInstance(tclass, 1, 1);
		tt[0][0] = t;
		return new DataMatrix<T>(tt, Arrays.asList("A"));
	}

	/**
	 * 转换成 浮点数类型
	 *
	 * @param <T> 元素类型
	 * @param t   元素
	 * @return 浮点数
	 */
	final static <T> Double todbl(final T t) { //
		if (t instanceof Number) {
			return ((Number) t).doubleValue();
		} else {
			final String s = t + "";
			Double d = null;
			try {
				d = Double.parseDouble(s);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return d;
		}
	}

	; // 数值转换函数

	/**
	 * 两个矩阵进行行叠加
	 *
	 * @param <T>  矩阵的元素类型
	 * @param dmx1 第一个矩阵
	 * @param dmx2 第二个矩阵
	 * @return DataMatrix
	 */
	public static <T> DataMatrix<T> rbind(DataMatrix<T> dmx1, DataMatrix<T> dmx2) {
		return Stream.concat(dmx1.row2S(Arrays::asList), dmx2.row2S(Arrays::asList)).collect(dmxclc());
	}

	/**
	 * 两个矩阵进行列叠加
	 *
	 * @param <T>  矩阵的元素类型
	 * @param dmx1 第一个矩阵
	 * @param dmx2 第二个矩阵
	 * @return DataMatrix
	 */
	public static <T> DataMatrix<T> cbind(DataMatrix<T> dmx1, DataMatrix<T> dmx2) {
		final List<String> keys = Stream.concat(dmx1.keyS(), dmx2.keyS()).distinct().collect(Collectors.toList());
		return rbind(dmx1.transpose(), dmx2.transpose()).transpose().setKeys(keys);
	}

	/**
	 * 矩阵乘法
	 *
	 * @param <T> 左矩阵元素
	 * @param <U> 右矩阵元素
	 * @param <V> mul 结果类型
	 * @param <X> 归集类型
	 * @param tt  左矩阵
	 * @param uu  右矩阵
	 * @param bop 元素乘法 (t,u)->v
	 * @return DataMatrix
	 */
	@SuppressWarnings("unchecked")
	public static <T, U, V, X> DataMatrix<V> binaryOp(final DataMatrix<T> tt, final DataMatrix<U> uu,
			final BiFunction<T, U, V> bop) {
		if (tt != null && uu != null) {
			final Tuple2<Integer, Integer> tshape = tt.shape();
			final Tuple2<Integer, Integer> ushape = uu.shape();
			final Tuple2<Integer, Integer> vshape = Tuple2.of(Math.max(tshape._1, ushape._1),
					Math.max(tshape._2, ushape._2));
			V[][] vv = null;
			final T[][] _tt = tt.cells;
			final U[][] _uu = uu.cells;
			for (int i = 0; i < vshape._1; i++) {
				for (int j = 0; j < vshape._2; j++) {
					final T t = _tt[i % tshape._1][j % tshape._2];
					final U u = _uu[i % ushape._1][j % ushape._2];
					final V v = bop.apply(t, u);
					if (v == null) {
						continue;
					} else if (vv == null) {
						vv = (V[][]) Array.newInstance(v.getClass(), vshape._1, vshape._2);
					}
					vv[i][j] = v;
				}
			}

			return DataMatrix.of((Iterable<String>) null, vv);
		} else {
			return null;
		}

	}

	/**
	 * 矩阵乘法
	 *
	 * @param <T>      左矩阵元素
	 * @param <U>      右矩阵元素
	 * @param <V>      mul 结果类型
	 * @param <X>      归集类型
	 * @param tt       左矩阵
	 * @param uu       右矩阵
	 * @param mul      元素乘法 (t,u)->v
	 * @param sigmaclc 求和归集器
	 * @return DataMatrix
	 */
	@SuppressWarnings("unchecked")
	public static <T, U, V, X> DataMatrix<X> mmult(final DataMatrix<T> tt, final DataMatrix<U> uu,
			final BiFunction<T, U, V> mul, Collector<V, ?, X> sigmaclc) {

		X[][] xx = null; // 结果元素类型
		if (uu != null && tt.width() == uu.height()) {
			final T[][] _tt = tt.cells;
			final U[][] _uu = uu.transpose().cells;
			final int m = _tt.length;
			final int n = _uu.length;

			for (int i = 0; i < m; i++) {
				for (int j = 0; j < n; j++) {
					final int _i = i;
					final int _j = j;
					final X x = Stream.iterate(0, k -> k + 1).limit(tt.width())
							.map(k -> mul.apply(_tt[_i][k], _uu[_j][k])).collect(sigmaclc);
					if (x == null) {
						continue;
					} else if (xx == null) {
						xx = (X[][]) Array.newInstance(x.getClass(), m, n);
					} // if

					xx[i][j] = x;
				} // j
			} // i
			return DataMatrix.of((Iterable<String>) null, xx);
		} else { // 左右矩阵的行列不匹配
			if (tt != null && uu != null) {
				System.err.println(String.format("左右矩阵的行列不匹配:%s,%s", tt.shape(), uu.shape()));
			}
			return null;
		} // if

	}

	/**
	 * 矩阵转置
	 *
	 * @param <U>   元素类型
	 * @param cells 数据矩阵
	 * @return U[][]
	 */
	public static <U> U[][] transpose(final U[][] cells) {
		return transpose(cells, 1);
	}

	/**
	 * 矩阵转置
	 *
	 * @param <U>   元素类型
	 * @param cells 数据矩阵
	 * @param mode  转置模式:0 for,1 stream
	 * @return U[][] 转置后的矩阵
	 */
	@SuppressWarnings("unchecked")
	public static <U> U[][] transpose(final U[][] cells, final long mode) {
		if (null == cells) {
			return null;
		}

		final Class<U> cellClazz = (Class<U>) cells.getClass().getComponentType().getComponentType();
		if (mode == 0) {// 0 for
			if (cells[0] == null) {
				return null;
			} // 通过第一个元素获取二维数组的维度
			final int m = cells.length;
			final int n = cells[0].length;
			final U[][] cc = (U[][]) Array.newInstance(cellClazz, n, m);
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[0].length; j++) {
					cc[j][i] = cells[i][j];
				} // j
			} // i
			return cc;
		} else {// mode 1 stream
			final Class<U[]> rowClazz = (Class<U[]>) cells.getClass().getComponentType();
			final Tuple2<Integer, Integer> shape = shape(cells);// 获取矩阵类型
			return intS(shape._2).map(i -> intS(shape._1).map(j -> {
				final int n = cells[j].length;
				return i >= n ? cells[j][i % n] : cells[j][i];
			}).toArray(n -> (U[]) Array.newInstance(cellClazz, n)))// 行数据数组
					.toArray(m -> (U[][]) Array.newInstance(rowClazz, m));// 返回数组元素
		} // if
	}

	/**
	 * 生成一个从0开始的无限流
	 *
	 * @return 生成一个从0开始的无限流
	 */
	public static Stream<Integer> intS() {
		return Stream.iterate(0, i -> i + 1);
	}

	/**
	 * 生成一个从0开始的长度为n的流
	 *
	 * @param n 流程的数据长度
	 * @return 生成一个从0开始的长度为n的流
	 */
	public static Stream<Integer> intS(final Number n) {
		return intS().limit(n.intValue());
	}

	/**
	 * 删除数组的第一行
	 *
	 * @param <U>   U 元素类型
	 * @param cells 数据矩阵
	 * @return U类型的数据矩阵
	 */
	@SuppressWarnings("unchecked")
	public static <U> U[][] removeFirstLine(final U[][] cells) {
		final int m = cells.length;
		final int n = cells[0].length;
		if (m < 1) {
			return null;
		}

		final U[][] cc = (U[][]) Array.newInstance(getGenericClass(cells), m - 1, n);
		System.arraycopy(cells, 1, cc, 0, (m - 1));

		return cc;
	}

	/**
	 * 获得cells数组中的元素的数据类型。 如果cells中的所有元素都是null,返回Object.class;
	 *
	 * @param <U>          cells数组中的元素的数据类型。
	 * @param cells        数据矩阵
	 * @param defaultClass 当cells 全为空返回的默认类型。
	 * @return cells 元素类型
	 */
	@SuppressWarnings("unchecked")
	public static <U> Class<U> getGenericClass(final U[][] cells, final Class<U> defaultClass) {
		Class<U> uclass = (Class<U>) defaultClass;
		if (cells == null)
			return uclass;
		else {
			uclass = (Class<U>) cells.getClass().getComponentType().getComponentType();
		}

		if (uclass == null) { // 这段代码被封存了
			uclass = DataMatrix.getGenericClass(Arrays.stream(cells).flatMap(Arrays::stream), defaultClass);
		} // if

		return uclass;
	}

	/**
	 * 获得cells数组中的元素的数据类型。 如果cells中的所有元素都是null,返回Object.class;
	 *
	 * @param <U>          cells数组中的元素的数据类型。
	 * @param us           数据矩阵
	 * @param defaultClass 当cells 全为空返回的默认类型。
	 * @return cells 元素类型
	 */
	public static <U> Class<U> getGenericClass(final Iterable<U> us, final Class<U> defaultClass) {
		return DataMatrix.getGenericClass(StreamSupport.stream(us.spliterator(), false), defaultClass);
	}

	/**
	 * 获得cells数组中的元素的数据类型。 如果cells中的所有元素都是null,返回Object.class;
	 *
	 * @param <U>          cells数组中的元素的数据类型。
	 * @param cells        数据矩阵
	 * @param defaultClass 当cells 全为空返回的默认类型。
	 * @return cells 元素类型
	 */
	@SuppressWarnings("unchecked")
	public static <U> Class<U> getGenericClass(final Stream<U> us, final Class<U> defaultClass) {
		final Class<U> uclass;
		final List<Class<?>> ll = us.filter(Objects::nonNull).map((Function<U, ? extends Class<?>>) U::getClass)
				.distinct().collect(Collectors.toList());
		if (ll.size() == 1) {
			uclass = (Class<U>) ll.get(0);
		} else {
			uclass = (Class<U>) defaultClass;
		} // if
		return uclass;
	}

	/**
	 * 获得cells数组中的元素的数据类型。 如果cells中的所有元素都是null,返回Object.class;
	 *
	 * @param <U>   cells数组中的元素的数据类型。
	 * @param cells 数据矩阵<br>
	 *              defaultClass 当cells 全为空返回的默认类型:Object.class
	 * @return cells 元素类型
	 */
	@SuppressWarnings("unchecked")
	public static <U> Class<U> getGenericClass(final U[][] cells) {
		return getGenericClass(cells, (Class<U>) Object.class);
	}

	/**
	 * 创建子矩阵
	 * 
	 * @param <U>   cells数组中的元素的数据类型。
	 * @param cells 对象二维数组
	 * @param i0    起点行索引 从0开始 行坐标
	 * @param j0    起点行索引 从0开始 列坐标
	 * @param i1    终点行索引 包含 对于超过最大范文的边界节点采用 循环取模的办法给与填充
	 * @param j1    终点行索引 包含 对于超过最大范文的边界节点采用 循环取模的办法给与填充
	 * @return 子矩阵
	 */
	public static <U> U[][] submx(final U[][] cells, final int i0, final int j0, final int i1, final int j1) {
		final int h = i1 - i0 + 1;
		final int w = j1 - j0 + 1;
		final Tuple2<Integer, Integer> shape = DataMatrix.shape(cells);
		@SuppressWarnings("unchecked")
		final U[][] cc = (U[][]) Array.newInstance(getGenericClass(cells), h, w);
		for (int i = i0; i <= i1; i++) {
			final var line = cells[i % shape._1]; // 读取数据行
			if (line == null) {
				continue;
			} else {
				final var lh = line.length;
				for (int j = j0; j <= Math.min(lh - 1, j1); j++) {
					cc[i - i0][j - j0] = line[j % shape._2];
				} // for j
			}
		} // for i
		return cc;
	}

	/**
	 * 返回矩阵的高度与宽度即行数与列数
	 *
	 * @param <U> aa的元素类型
	 * @param aa  待检测的矩阵：过矩阵为null返回一个(0,0)的二元组。
	 * @return (height : 行数, width : 列数)
	 */
	public static <U> Tuple2<Integer, Integer> shape(final U[][] aa) {
		if (aa == null || aa.length < 1) {
			return new Tuple2<>(0, 0);
		}

		final int height = aa.length;
		final int width = Stream.of(aa).collect(Collectors.summarizingInt(e -> e.length)).getMax(); // 最长的宽度

		return new Tuple2<>(height, width);
	}

	/**
	 * 把 一个数字 n转换成一个字母表中的数值(术语） 在alphabetics中:ABCDEFGHIJKLMNOPQRSTUVWXYZ
	 * 比如:0->A,1-B,25-Z,26-AA 等等
	 *
	 * @param n      数字
	 * @param alphas 字母表
	 * @return 生成exel式样的名称
	 */
	public static String nomenclature(final Integer n, final String[] alphas) {
		final int model = alphas.length;// 字母表尺寸
		final List<Integer> numbers = new LinkedList<>();
		Integer num = n;
		do {
			numbers.add(num % model);
			num /= model;// 进入下回一个轮回
		} while (num-- > 0); // num-- 使得每次都是从A开始，即Z的下一个是AA而不是BA
		// 就是这个简答但算法我想了一夜,我就是不知道如何让10位每次都从0开始。
		Collections.reverse(numbers);

		return numbers.stream().map(e -> alphas[e]).collect(Collectors.joining(""));
	}

	/**
	 * 把excel名称转换成位置坐标(RangeDef)
	 * 
	 * @param nameline 名称行‘Sheet1!C2:D3’ 或是 ‘C2:D3’ 这样的字符串;<br>
	 *                 需要注意对于nameline为:'A1'这样的名称长会被视为'A1:A1'的单个单元格进行处理
	 * @return 名称转rdf, 回结果会保证 LT，小于 RB； F1:B10, 会被解释为 B1:F10。
	 */
	public static RangeDef name2rdf(final String nameline) {
		final String pattern = "(([A-Z]+)\\s*(\\d+))(\\s*:\\s*(([A-Z]+)\\s*(\\d+)))?";
		if (nameline == null) {
			return null;
		} else { // nameline 有效
			final int i = nameline.indexOf("!");
			final String name = nameline.toUpperCase(); // 转换成大写形式
			final String line = (i < 0 ? name : name.substring(i + 1)).strip();
			final String sheetName = i >= 0 ? nameline.substring(0, i).strip() : null;
			final Matcher matcher = Pattern.compile(pattern).matcher(line);
			final Function<Integer, String> reader = id -> Optional.ofNullable(matcher.group(id)) //
					.map(String::strip).orElse(null);
			final RangeDef rdf;// 数值区域

			if (matcher.find()) {
				final String y0 = reader.apply(2); // 左上列号
				final String x0 = reader.apply(3); // 左上行号
				final String y1 = Optional.ofNullable(reader.apply(6)).orElse(y0); // 右下列号:默认为y0
				final String x1 = Optional.ofNullable(reader.apply(7)).orElse(x0); // 右下行号:默认为x0
				final Integer ix0 = DataMatrix.xlsn2id(x0);
				final Integer iy0 = DataMatrix.xlsn2id(y0);
				final Integer ix1 = DataMatrix.xlsn2id(x1);
				final Integer iy1 = DataMatrix.xlsn2id(y1);

				rdf = new RangeDef(sheetName, // 表单名称
						Math.min(ix0, ix1), Math.min(iy0, iy1), // LT 的索引值较小
						Math.max(ix0, ix1), Math.max(iy0, iy1) // RB 的索引较大
				); // RangeDef
			} else { // if
				rdf = null;
			} // if

			return rdf; // 数据区域内容
		} // if
	}

	/**
	 * index_to_excel_name 的 别名 <br>
	 * 列名称： 从0开始 0->A,1->B;2->C;....,25->Z,26->AA
	 *
	 * @param n 数字 从0开始映射
	 * @return 类似于EXCEL的列名称
	 */
	public static String label(final int n) {
		return index_to_excel_name(n);
	}

	/**
	 * index_to_excel_name 的 别名 <br>
	 * 列名称： 从0开始 0->A,1->B;2->C;....,25->Z,26->AA
	 *
	 * @param n 数字 从0开始映射
	 * @return 类似于EXCEL的列名称
	 */
	public static String xlsn(final int n) {
		return index_to_excel_name(n);
	}

	/**
	 * 生成一个默认的键名蓄力
	 * 
	 * @param n 键名序列长度，大于等于1的正数,1:[A],2:[A,B];3:[A,B,C]
	 * @return 指定长度的XLS的命名序列
	 */
	public static String[] xlsns(final int n) {
		if (n < 0) { // 无效长度
			return null;
		} else { // 键名序列长度
			final var keys = Stream.iterate(0, i -> i + 1).limit(n) // 提取指定长度序列
					.map(DataMatrix::xlsn).toArray(String[]::new);
			return keys;
		} // if
	}

	/**
	 * 列名称： 从0开始 0->A,1->B;2->C;....,25->Z,26->AA
	 *
	 * @param index 数字 从0开始映射
	 * @return 类似于EXCEL的列名称
	 */
	public static String index_to_excel_name(final int index) {
		// 字母表
		final String[] alphabetics = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");

		return nomenclature(index, alphabetics);
	}

	/**
	 * 把A转换成0,b转换成1,aa 转换成26 如果line 本身就是一个数字则直接返回 "A1:B2" -> x0=0, y0=0, x1=1, y1=1
	 *
	 * @param line 字符串: 字符类别,A:转换成0,B 转换成1,AAA 转换成 702 数字类别 ,1:转换哼0,2 转换成 1;
	 * @return 数据解析
	 */
	public static Integer excel_name_to_index(final String line) {
		if (line == null)
			return null;
		final String final_line = line.toUpperCase().trim();// 转换成大写形式。
		Matcher matcher = Pattern.compile("\\d+").matcher(final_line);
		if (matcher.matches()) {// 数字格式
			return Integer.parseInt(final_line) - 1;
		}

		matcher = Pattern.compile("[A-Z]+", Pattern.CASE_INSENSITIVE).matcher(final_line);
		if (!matcher.matches())
			return null;
		final int N_SIZE = 26;// 进制
		final int len = final_line.length();
		int num = 0;// 数量内容
		for (int i = 0; i < len; i++) {
			final char c = final_line.charAt(i);
			final int n = (Character.toUpperCase(c) - 'A') + (i == len - 1 ? 0 : 1);// A在line尾端为0，否则A对应为1
			num = num * N_SIZE + n;
		} // for

		final int index = num;

		return index;
	}

	/**
	 * 把A转换成0,b转换成1,aa 转换成26 如果line 本身就是一个数字则直接返回 "A1:B2" -> x0=0, y0=0, x1=1, y1=1
	 *
	 * @param name 字符串: 字符类别,A:转换成0,B 转换成1,AAA 转换成 702 数字类别 ,1:转换哼0,2 转换成 1;
	 * @return 数据解析
	 */
	public static Integer xlsn2id(final String name) {
		return DataMatrix.excel_name_to_index(name);
	}

	/**
	 * excel 地址转偏移地址
	 *
	 * @param address 单元格地址 比如: A5 -> (4,0)
	 * @return (行索引 ， 列索引 ）
	 */
	public static Tuple2<Integer, Integer> addr2offset(final String address) {
		final Matcher matcher = Pattern.compile("([A-Z]+)([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(address);
		if (matcher.find()) {
			final String p1 = matcher.group(1);
			final String p2 = matcher.group(2);
			final Integer[] dd = Stream.of(p1, p2).map(DataMatrix::excel_name_to_index).toArray(Integer[]::new);

			return Tuple2.of(dd[0], dd[1]).swap(); //
		} else {
			return null;
		}

	}

	/**
	 * excel 地址偏移转换地址
	 *
	 * @param offset 单元格偏移 比如: (4,0)->A5
	 * @return 单元格地址
	 */
	public static String offset2addr(final Tuple2<Integer, Integer> offset) {
		return DataMatrix.index_to_excel_name(offset._2) + (offset._1 + 1);

	}

	/**
	 * 公式偏移调整(相对偏移调整）
	 *
	 * @param formula       公式
	 * @param origin_offset 相对于起点的偏移
	 * @return 公式调整
	 */
	public static String adjust_formula(final String formula, final Tuple2<Integer, Integer> origin_offset) {

		// JAVA 9 的API这里不适用
		// final Matcher matcher = Pattern.compile("([A-Z]+)([0-9]+)",
		// Pattern.CASE_INSENSITIVE).matcher(formula);
		// final String _line = matcher.replaceAll(e -> {
		// final var term = e.group();
		// final var offset = DataMatrix.addr2offset(term) //
		// .map1(p -> p + origin_offset._1) //
		// .map2(p -> p + origin_offset._2); //
		// return DataMatrix.offset2addr(offset);
		// });
		// return _line;

		final String pattern = "([A-Z]+)([0-9]+)";
		final String line = pattern_replace_all(formula, pattern, term -> {
			final Tuple2<Integer, Integer> offset = DataMatrix.addr2offset(term) //
					.fmap1(p -> p + origin_offset._1) //
					.fmap2(p -> p + origin_offset._2); //
			return DataMatrix.offset2addr(offset);
		});

		return line;
	}

	/**
	 * pattern_replace_all
	 * 
	 * @param line     数据行
	 * @param pattern  模板式样
	 * @param replacer 变换函数 The function to be applied to the match result of this
	 *                 matcher that returns a replacement string.
	 * @return 替换
	 */
	public static String pattern_replace_all(final String line, final String pattern,
			final Function<String, String> replacer) {

		final Matcher matcher = Pattern.compile("([A-Z]+)([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(line);
		final StringBuilder sb = new StringBuilder();
		final List<MatchResult> results = new ArrayList<>();
		while (matcher.find()) {
			results.add(matcher.toMatchResult());
		}

		int i = 0;
		for (final MatchResult result : results) {
			while (i < result.start()) {
				sb.append(line.charAt(i++));
			}
			final String e = replacer.apply(result.group());
			sb.append(e);
			i = result.end();
		} //

		while (i < line.length()) {
			sb.append(line.charAt(i++));
		} //

		return sb.toString();
	}

	/**
	 * 公式偏移调整
	 *
	 * @param formula            公式
	 * @param origin_offset_name 相对于起点的偏移
	 * @return 公式调整
	 */
	public static String adjust_formula(final String formula, final String origin_offset_name) {
		final Tuple2<Integer, Integer> origin_offset = DataMatrix.addr2offset(origin_offset_name);
		return DataMatrix.adjust_formula(formula, origin_offset);
	}

	/**
	 * 生成函数
	 *
	 * @param <T>   元素类型
	 * @param keys  表头
	 * @param datas 数据数组
	 * @return 数据矩阵
	 */
	public static <T> DataMatrix<T> build(final Iterable<String> keys, final T[][] datas) {
		final DataMatrix<T> dm = new DataMatrix<T>(datas,
				keys == null
						? Stream.iterate(0, i -> i + 1).limit(datas[0].length).map(DataMatrix::index_to_excel_name)
								.collect(Collectors.toList())
						: StreamSupport.stream(keys.spliterator(), false).collect(Collectors.toList()));
		return dm;
	}

	/**
	 * 生成函数（默认的excel风格表头）
	 *
	 * @param <T>   元素类型
	 * @param datas 数据数组
	 * @return 数据矩阵
	 */
	public static <T> DataMatrix<T> of(final T[][] datas) {
		return DataMatrix.of(datas, (Iterable<String>) null);
	}

	/**
	 * 生成函数（默认的excel风格表头）
	 *
	 * @param <T>   元素类型
	 * @param keys  表头数据
	 * @param datas 数据数组
	 * @return 数据矩阵
	 */
	public static <T> DataMatrix<T> of(final T[][] datas, final Iterable<String> keys) {
		final var title = Optional.ofNullable(keys)
				.orElseGet(() -> Optional.ofNullable(datas)
						.map(e -> Stream.of(e).map(p -> p.length)
								.collect(Collectors.maxBy(Comparator.comparing(x -> x))).orElse(null))
						.map(DataMatrix::xlsns).map(Arrays::asList).orElse(null));
		return Optional.ofNullable(datas).map(e -> new DataMatrix<T>(datas, title)).orElse(null);
	}

	/**
	 * 生成函数
	 *
	 * @param <T>   元素类型
	 * @param keys  表头
	 * @param datas 数据数组
	 * @return 数据矩阵
	 */
	public static <T> DataMatrix<T> of(final Iterable<String> keys, final T[][] datas) {
		return DataMatrix.build(keys, datas);
	}

	/**
	 * 生成函数
	 *
	 * @param <T>   元素类型
	 * @param keys  表头
	 * @param datas 数据数组
	 * @return 数据矩阵
	 */
	public static <T> DataMatrix<T> of(final String[] keys, final T[][] datas) {
		return DataMatrix.of(keys == null ? null : Arrays.asList(keys), datas);
	}

	/**
	 * 生成函数
	 *
	 * @param keys  表头
	 * @param datas 数据数组
	 * @return 数据矩阵
	 */
	public static DataMatrix<String> of(final String keys, final String datas) {
		if (datas == null) {
			return null;
		}

		final String[][] s_datas = Stream.of(datas.split("[;\n]+")) // 行分隔符
				.map(e -> e.split("[,\\s]+")) // 列分隔符
				.toArray(String[][]::new);

		return DataMatrix.of((Iterable<String>) null, s_datas);
	}

	/**
	 * 生成函数
	 * <p>
	 * names 表头表头默认为 EXCEL NAME 的命名
	 *
	 * @param datas 数据数组,行顺序:行分隔符';'行内分隔符','
	 * @return 数据矩阵
	 */
	public static DataMatrix<String> of(final String datas) {
		return DataMatrix.of(null, datas);
	}

	/**
	 * 生成一条行向量
	 *
	 * @param <T>      矩阵元素
	 * @param dataline 行向量
	 * @return 行向量
	 */
	public static <T> DataMatrix<T> of(final Iterable<T> dataline) {
		final List<T> line = StreamSupport.stream(dataline.spliterator(), false).collect(Collectors.toList());
		return Stream.of(line).collect(dmxclc());
	}

	/**
	 * 生成一条行向量
	 *
	 * @param <T>  矩阵元素
	 * @param line 行向量
	 * @return 行向量
	 */
	public static <T> DataMatrix<T> of(final T[] line) {
		return of(Arrays.asList(line));
	}

	/**
	 * 生成一条列向量
	 *
	 * @param <T>  矩阵元素
	 * @param keys 列名序列
	 * @param line 列向量
	 * @return 列向量
	 */
	public static <T> DataMatrix<T> of(final String keys, final T[] line) {
		return of(Arrays.asList(line)).transpose().setKeys(keys);
	}

	/**
	 * 生成一条列向量
	 *
	 * @param <T> 矩阵元素
	 * @param <V> 第二元素类型
	 * @param tup (name,列向量数据)
	 * @return 列向量
	 */
	public static <T, V extends Iterable<T>> DataMatrix<T> of(final Tuple2<String, V> tup) {
		return of(tup._2).transpose().setKeys(tup._1);
	}

	/**
	 * 数据矩阵(行向量),ROW matriX
	 *
	 * @param <T>   矩阵元素
	 * @param cells 数据行元素
	 * @return 数据矩阵
	 */
	@SafeVarargs
	public static <T> DataMatrix<T> ROWX(final T... cells) {
		return of(cells);
	}

	/**
	 * 数据矩阵(列向量),COLumn matriX
	 *
	 * @param <T>   元素类型
	 * @param cells 数据行元素
	 * @return 数据矩阵
	 */
	@SafeVarargs
	public static <T> DataMatrix<T> COLX(final T... cells) {
		return ROWX(cells).transpose();
	}

	/**
	 * 行转数组
	 * 
	 * @param <T> 元素类型
	 * @param row 行数据
	 * @return 行转数组
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] row2aa(final Iterable<T> row) {
		final T t = StreamSupport.stream(row.spliterator(), false).filter(Objects::nonNull).findFirst().orElse(null);
		final Class<T> tclass = (Class<T>) t.getClass();
		return row2aa(tclass, row);
	}

	/**
	 * 行转数组
	 * 
	 * @param <T>    元素类型
	 * @param tclass 数据元素
	 * @param row    行数据
	 * @return 行转数组
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] row2aa(final Class<T> tclass, final Iterable<T> row) {
		return StreamSupport.stream(row.spliterator(), false).toArray(n -> (T[]) Array.newInstance(tclass, n));
	}

	/**
	 * 行数据转数组
	 * 
	 * @param <T>  行元素
	 * @param <R>  行类型
	 * @param rows 行数据
	 * @return T类型的数组
	 */
	public static <T, R extends Iterable<T>> T[][] rows2aa(final Iterable<R> rows) {
		final T t = StreamSupport.stream(rows.spliterator(), false)
				.flatMap(e -> StreamSupport.stream(e.spliterator(), false)).filter(Objects::nonNull).findFirst()
				.orElse(null);
		@SuppressWarnings("unchecked")
		final Class<T> tclass = (Class<T>) t.getClass();
		return rows2aa(tclass, rows);
	}

	/**
	 * 行数据转数组
	 * 
	 * @param <T>    行元素
	 * @param <H>    行类型
	 * @param tclass 元素类型
	 * @param rows   行数据
	 * @return T类型的数组
	 */
	public static <T, H extends Iterable<T>> T[][] rows2aa(final Class<T> tclass, final Iterable<H> rows) {

		final AtomicInteger ai = new AtomicInteger(0); // 数组长度
		@SuppressWarnings("unchecked")
		final T[][] tt = StreamSupport.stream(rows.spliterator(), false).filter(Objects::nonNull)
				.map(row -> DataMatrix.row2aa(tclass, row)).peek(e -> { // 计算元素(数组)长度
					if (e.length > ai.get()) { // 保持长度为最长元素（数组）的长度
						ai.set(e.length); // 更新数组长度
					} // if
				}).toArray(n -> (T[][]) Array.newInstance(tclass, n, ai.get()));
		return tt;
	}

	/**
	 * 生成函数
	 * <p>
	 * names 表头表头默认为 EXCEL NAME 的命名
	 *
	 * @param datas 数据数组,行顺序:行分隔符';'行内分隔符','
	 * @return 数据矩阵
	 */
	public static DataMatrix<Integer> intof(final String datas) {
		return DataMatrix.of(null, datas).corece(e -> {
			Integer i = null;
			try {
				i = Integer.parseInt(e);
			} catch (Exception ex) {
				// ex.printStackTrace();
			}
			return i;
		}); //
	}

	/**
	 * 生成函数
	 * <p>
	 * names 表头表头默认为 EXCEL NAME 的命名
	 *
	 * @param datas 数据数组,行顺序:行分隔符';'行内分隔符','
	 * @return 数据矩阵
	 */
	public static DataMatrix<Double> dblof(final String datas) {
		return DataMatrix.of(null, datas).corece(e -> {
			Double d = null;
			try {
				d = Double.parseDouble(e);
			} catch (Exception ex) {
				// ex.printStackTrace();
			}
			return d;
		}); //
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <T>  元素类型
	 * @param <LN> 行类型
	 * @return 矩阵归集器
	 */
	public static <T, LN extends Iterable<T>> Collector<LN, ?, DataMatrix<T>> dmxclc() {
		return dmxclc((List<String>) null);
	}

	/**
	 * 矩阵归集器 列方向
	 * 
	 * @param <T> 元素
	 * @param <V> 列元素
	 * @return 矩阵归集器
	 */
	public static <T, V extends Iterable<T>> Collector<Tuple2<String, V>, ?, DataMatrix<T>> dmxclc2() {
		return DataMatrix.dmxclc2(null);
	}

	/**
	 * 矩阵归集器 列方向
	 * 
	 * @param <T>    元素
	 * @param <V>    列元素
	 * @param tclass 元素类型类
	 * @return 矩阵归集器
	 */
	public static <T, V extends Iterable<T>> Collector<Tuple2<String, V>, ?, DataMatrix<T>> dmxclc2(
			final Class<T> tclass) {
		return Collector.of(() -> new LinkedHashMap<String, V>(), (aa, a) -> aa.put(a._1, a._2), (aa, bb) -> {
			aa.putAll(bb);
			return aa;
		}, e -> {
			final List<V> rows = e.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
			final T[][] _data = tclass == null ? DataMatrix.rows2aa(rows) : DataMatrix.rows2aa(tclass, rows);
			final T[][] data = DataMatrix.transpose(_data);
			return new DataMatrix<T>(data, e.keySet());
		});
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <T>  元素类型
	 * @param <LN> 行类型
	 * @param keys 键名列表
	 * @return 矩阵归集器
	 */
	public static <T, LN extends Iterable<T>> Collector<LN, ?, DataMatrix<T>> dmxclc(final Iterable<String> keys) {
		return dmxclc(e -> e, keys);
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <LN>       行类型
	 * @param <T>        元素类型
	 * @param linemapper 行转换器
	 * @param keys       键名列表
	 * @return 矩阵归集器
	 */
	@SuppressWarnings("unchecked")
	public static <LN, T> Collector<LN, ?, DataMatrix<T>> dmxclc(final Function<LN, Iterable<T>> linemapper,
			final Iterable<String> keys) {
		return Collector.of(() -> new ArrayList<Iterable<T>>(), (aa, a) -> aa.add(linemapper.apply(a)), (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, tt -> {
			final Class<T> tclass = (Class<T>) DataMatrix.getGenericClass(
					tt.stream().flatMap(e -> StreamSupport.stream(e.spliterator(), false)), Object.class);
			final AtomicInteger ai = new AtomicInteger(0); // 数组长度
			final T[][] _tt = tt.stream().filter(Objects::nonNull).map(e -> StreamSupport.stream(e.spliterator(), false) //
					.toArray(n -> (T[]) Array.newInstance(tclass, n))).peek(e -> { // 计算元素(数组)长度
						if (e.length > ai.get()) { // 保持长度为最长元素（数组）的长度
							ai.set(e.length); // 更新数组长度
						} // if
					}).toArray(n -> (T[][]) Array.newInstance(tclass, n, ai.get()));
			return DataMatrix.of(keys, _tt);

		}); // Collector.of
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <LN>       行类型
	 * @param <T>        元素类型
	 * @param linemapper 行转换器
	 * @return 矩阵归集器
	 */
	public static <LN, T> Collector<LN, ?, DataMatrix<T>> dmxclc(final Function<LN, Iterable<T>> linemapper) {
		return dmxclc(linemapper, null);
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <T>  元素类型
	 * @param n    行长度
	 * @param keys 健名序列
	 * @return 矩阵归集器
	 */
	public static <T> Collector<T, ?, DataMatrix<T>> dmxclc(final int n, String keys) {
		return dmxclc(n, keys.split("[，,]+"));
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <T>  元素类型
	 * @param n    行长度
	 * @param keys 健名序列
	 * @return 矩阵归集器
	 */
	public static <T> Collector<T, ?, DataMatrix<T>> dmxclc(final int n, String[] keys) {
		return dmxclc(n, Arrays.asList(keys));
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <T>  元素类型
	 * @param n    行长度
	 * @param keys 键名列表
	 * @return 矩阵归集器
	 */
	public static <T> Collector<T, ?, DataMatrix<T>> dmxclc(final int n, final Iterable<String> keys) {
		return Collector.of(() -> new ArrayList<T>(), (aa, a) -> aa.add(a), (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, tt -> {
			return tt.stream().collect(DataMatrix.linesclc(n)).collect(dmxclc(keys));
		});
	}

	/**
	 * 矩阵归集器
	 *
	 * @param <T> 元素类型
	 * @param n   行长度
	 * @return 矩阵归集器
	 */
	public static <T> Collector<T, ?, DataMatrix<T>> dmxclc(final int n) {
		return dmxclc(n, (List<String>) null);
	}

	/**
	 * 固定长度切割
	 *
	 * @param <T> 元素类型
	 * @param n   切割的长度
	 * @return 切割归集器
	 */
	public static <T> Collector<T, ?, Stream<List<T>>> linesclc(final int n) {
		return linesclc((i, t) -> i > 0 && i % n == 0);
	}

	/**
	 * 指定条件切割
	 *
	 * @param <T>            元素类型
	 * @param line_predicate (i:元素索引编号从0开始,(line,t)：行数据缓存,当前字符)-> boolean,
	 *                       行分割函数，对于流中第一个元素 执行以下测试函数： predicate.test(0,(null,t))
	 * @return 切割归集器
	 */
	public static <T> Collector<T, ?, Stream<List<T>>> linesclc(
			final BiPredicate<Integer, Tuple2<LinkedList<T>, T>> line_predicate) {
		return Collector.of(() -> new ArrayList<T>(), (aa, a) -> aa.add(a), (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, (tt) -> {
			final List<List<T>> lines = new ArrayList<List<T>>(); // 系列行函数
			LinkedList<T> line = null;
			for (int i = 0; i < tt.size(); i++) {
				final T t = tt.get(i);
				final Tuple2<LinkedList<T>, T> tup = Tuple2.of(line, t);
				final boolean flag = line_predicate.test(i, tup);
				if (i == 0 || flag) {
					line = new LinkedList<T>();
					lines.add(line);
				}
				line.add(t);
			} // for
			return lines.stream();
		}); // of
	}

	/**
	 * 生成函数(转置）向量
	 * <p>
	 * names 表头表头默认为 EXCEL NAME 的命名
	 *
	 * @param datas 数据数组,行顺序:行分隔符';'行内分隔符','
	 * @return 数据矩阵
	 */
	public static DataMatrix<Double> V(final String datas) {
		return DataMatrix.dblof(datas).transpose();
	}

	/**
	 * 元素素组
	 *
	 * @param <T> 元素类型
	 * @param ts  元素序列
	 * @return 元素数组
	 */
	@SafeVarargs
	public static <T> T[] A(final T... ts) {
		return ts;
	}

	private T[][] cells; // 单元格数据
	private Map<String, Integer> keymetas = new HashMap<>();// 表头名-->列id索引 的 Map,列id索引从0开始

}
