package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.jdbc.Jdbcs;

/**
 * DFrame 数据框，(key,[v0,v1,...]) 结构的键值对集合 kvs 即值元素为集合类型的KVPs.<br>
 * 术语来源于R，<br>
 * 
 * @author gbench
 *
 */
public class DFrame extends LinkedRecord {

	/**
	 * 构造函数
	 */
	public DFrame() {
		// do nothing
	}

	/**
	 * 构造函数
	 * 
	 * @param data 数据列表
	 */
	public DFrame(final Map<?, ?> data) {

		data.forEach(this::add);
	}

	/**
	 * 重写基础版本的数据行
	 */
	@Override
	public List<IRecord> rows() {

		return this.rows(false);
	}

	/**
	 * 带有缓存版本的 数据流
	 * 
	 * @param clearflag 是否清空缓存,true:清空,false:使用缓存,没有缓则则会自动创建缓存
	 * @return 数据项目
	 */
	public List<IRecord> rows(final boolean clearflag) {

		if (clearflag) {
			this.rowsCache = null;
		}
		return Optional.ofNullable(this.rowsCache).orElseGet(() -> this.rowsCache = super.rows());
	}

	/**
	 * 第rowid 所在的行记录
	 * 
	 * @param rowid 行号索引：从0开始
	 * @return rowid所标记行记录
	 */
	@Override
	public IRecord row(final int rowid) {

		return this.rowOpt(rowid).orElse(null);
	}

	/**
	 * 第rowid 所在的行记录
	 * 
	 * @param rowid 行号索引：从0开始
	 * @return rowid所标记行记录
	 */
	public Optional<IRecord> rowOpt(final int rowid) {

		final var rows = this.rows();
		return Optional.ofNullable(rows.size() > rowid ? rows.get(rowid) : null);
	}

	/**
	 * lhs.many2one(rhsId) <br>
	 * 按照行进行查找: many(lhs) to one(rhs) 关系的多方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param key   外键字段名
	 * @param rhsId 右侧方(一方)的Id
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public DFrame many2one(final String key, final Object rhsId) {

		return this.filterBy(key, rhsId);
	}

	/**
	 * 这是按照keys 所指定的键名进行字段过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param keys 提取的字段的键值名称数据,keys为null 表示不进行过滤。
	 * @return DFrame。
	 */
	public DFrame cols(final String[] keys) {

		return Optional.ofNullable(keys).map(kk -> new DFrame(this.filter(kk).toMap())).orElse(this);
	}

	/**
	 * 这是按照keys 所指定的键名进行字段过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param keys 否定提取的字段的键值名称数据,keys为null 表示不进行过滤。
	 * @return DFrame
	 */
	public DFrame cols(final String keys) {

		return Optional.ofNullable(this.cols(keys.split("[,]+"))).orElse(this);
	}

	/**
	 * 第colid 所在的列号数据
	 * 
	 * @param colid 列号索引：从0开始
	 * @return colid所标识列号数据
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> col(final int colid) {

		return (List<T>) this.col(colid, e -> e);
	}

	/**
	 * 第colid 所在的列号数据
	 * 
	 * @param key 列号名称
	 * @return key所标识列号数据
	 */
	public <T> List<T> col(final String key) {

		return this.col(this.indexOfKey(key));
	}

	/**
	 * 这是按照keys 所指定的键名进行字段否定过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param keys 否定提取的字段集合用逗号分割,keys为null表示不进行过滤。注意分隔符号之间不能留有空格
	 * @return DFrame。
	 */
	public DFrame colsNot(final String[] keys) {

		return Optional.ofNullable(keys).map(Arrays::asList)
				.map(ks -> this.keyS().filter(k -> ks.indexOf(k) > 0).toArray(String[]::new))
				.map(kk -> new DFrame(this.filter(kk).toMap())).orElse(this);
	}

	/**
	 * 这是按照keys 所指定的键名进行字段否定过滤。默认过滤空值字段（该字段的值value为null) <br>
	 * 
	 * @param keys 否定键值名称数据,keys为null 表示不进行过滤。
	 * @return DFrame。
	 */
	public DFrame colsNot(final String keys) {

		return Optional.ofNullable(keys).map(ks -> ks.split("[,]+")).map(Arrays::asList)
				.map(ks -> this.keyS().filter(k -> ks.indexOf(k) < 0).toArray(String[]::new))
				.map(kk -> new DFrame(this.filter(kk).toMap())).orElse(this);
	}

	/**
	 * lhs.many2one(rhsId) <br>
	 * 按照行进行查找: many(lhs) to one(rhs) 关系的多方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param <K>   缓存的键名类型
	 * @param key   外键字段名
	 * @param rhsId 右侧方(一方)的Id
	 * @param cache 检索缓存
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public <K> DFrame many2one(final String key, final K rhsId, final Map<K, DFrame> cache) {

		return cache.computeIfAbsent(rhsId, k -> this.filterBy(key, rhsId));
	}

	/**
	 * lhs.many2one(rhsId) <br>
	 * 按照行进行查找: many(lhs) to one(rhs) 关系的多方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param <K>      缓存的键名类型
	 * @param key      外键字段名
	 * @param rhsId    右侧方(一方)的Id
	 * @param cacheKey 缓存键，会根据需要自动创建对应名称的缓存
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public <K> DFrame many2one(final String key, final K rhsId, final String cacheKey) {

		return this.many2one(key, rhsId, this.cache(cacheKey));
	}

	/**
	 * lhs.many2one(oneId) <br>
	 * 按照行进行查找: one(lhs) to one(rhs) 关系的lhs一方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param key   外键字段名
	 * @param rhsId 右侧方(一方)的Id
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public IRecord one2one(final String key, final Object rhsId) {

		return this.filterBy(key, rhsId).rowS().findFirst().orElse(null);
	}

	/**
	 * lhs.many2one(oneId) <br>
	 * 按照行进行查找: one(lhs) to one(rhs) 关系的lhs一方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param <K>   缓存的键名类型
	 * @param key   外键字段名
	 * @param rhsId 右侧方(一方)的Id
	 * @param cache 检索缓存
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public <K> IRecord one2one(final String key, final K rhsId, final Map<K, IRecord> cache) {

		return cache.computeIfAbsent(rhsId, k -> this.filterBy(key, rhsId).rowS().findFirst().orElse(null));
	}

	/**
	 * lhs.one2one(oneId) <br>
	 * 按照行进行查找: one(lhs) to one(rhs) 关系的lhs一方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param <K>      缓存的键名类型
	 * @param key      外键字段名
	 * @param rhsId    右侧方(一方)的Id
	 * @param cacheKey 缓存键，会根据需要自动创建对应名称的缓存
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public <K> IRecord one2one(final String key, final K rhsId, final String cacheKey) {

		return this.one2one(key, rhsId, this.cache(cacheKey));
	}

	/**
	 * lhs.many2one(oneId) <br>
	 * 按照行进行查找: one(lhs) to one(rhs) 关系的lhs一方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param <K>   缓存的键名类型
	 * @param key   外键字段名
	 * @param rhsId 右侧方(一方)的Id
	 * @param cache 检索缓存
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public <K> Optional<IRecord> one2opt(final String key, final K rhsId, final Map<K, Optional<IRecord>> cache) {

		return cache.computeIfAbsent(rhsId, k -> this.filterBy(key, rhsId).rowS().findFirst());
	}

	/**
	 * lhs.one2one(oneId) <br>
	 * 按照行进行查找: one(lhs) to one(rhs) 关系的lhs一方数据提取 <br>
	 * 根据提供的oneId从当前对象(多方)中提取隶属于/对应于一方的数据集合。
	 * 
	 * @param <K>      缓存的键名类型
	 * @param key      外键字段名
	 * @param rhsId    右侧方(一方)的Id
	 * @param cacheKey 缓存键，会根据需要自动创建对应名称的缓存
	 * @return Optional 指定键值的数据行(多方数据集合)
	 */
	public <K> Optional<IRecord> one2opt(final String key, final K rhsId, final String cacheKey) {

		return this.one2opt(key, rhsId, this.cache(cacheKey));
	}

	/**
	 * 洗牌式乱序
	 * 
	 * @return 洗牌式乱序
	 */
	public DFrame shuffle() {

		return this.rowS().map(e -> Tuple2.of(Math.random(), e)) // 加入排序字段
				.sorted((a, b) -> Objects.compare(a._1(), b._1(), Double::compareTo)).map(e -> e._2) //
				.collect(DFrame.dfmclc);
	}

	/**
	 * 提取头前n个元素
	 * 
	 * @param n 头前元素的数量
	 * @return 提取头前n个元素
	 */
	public DFrame head(final int n) {

		return this.rowS().limit(n).collect(DFrame.dfmclc);
	}

	/**
	 * 提取头前n个元素
	 * 
	 * @return 提取头前n个元素
	 */
	public IRecord head() {

		return this.headOpt().orElse(null);
	}

	/**
	 * 提取头前n个元素
	 * 
	 * @return 提取头前n个元素
	 */
	public Optional<IRecord> headOpt() {

		return this.rowS().findAny();
	}

	/**
	 * 尾部元素(除掉第一个元素后的剩余)
	 * 
	 * @return 尾部元素DFrame
	 */
	public DFrame tail() {

		return this.rowS().skip(1).collect(DFrame.dfmclc);
	}

	/**
	 * 尾部元素
	 * 
	 * @param n 提取n个尾部元素,当n大于整个长度的时候，返回整个元素
	 * @return 尾部元素DFrame
	 */
	public DFrame tail(final int n) {

		final var rows = this.rows(); // 提取数据行
		final var len = rows.size();
		final var startIndex = len - n; // 起始索引
		final var sublist = rows.subList(startIndex < 0 ? 0 : startIndex, len);

		return sublist.stream().collect(DFrame.dfmclc);
	}

	/**
	 * 尾部元素(除掉第一个元素后的剩余)
	 * 
	 * @return 尾部元素列表
	 */
	public List<IRecord> tails() {

		return this.tail().rows();
	}

	/**
	 * 拆分头前n个元素和所有后续元素
	 * 
	 * @return (head,tail)
	 */
	public Tuple2<IRecord, DFrame> carcdr() {

		return Tuple2.of(this.rowS().limit(1).findAny().orElse(null), this.rowS().skip(1).collect(DFrame.dfmclc));
	}

	/**
	 * 提取最后一个元素
	 * 
	 * @return 提取最后一个元素
	 */
	public IRecord last() {
		return this.lastOpt().orElse(null);
	}

	/**
	 * 提取最后一个元素
	 * 
	 * @return 提取最后一个元素
	 */
	public Optional<IRecord> lastOpt() {

		final var rows = this.rows();
		return Optional.ofNullable(rows.size() > 0 ? rows.get(rows.size() - 1) : null);
	}

	/**
	 * 拆分头前n个元素和所有后续元素
	 * 
	 * @param n 头前元素的数量
	 * @return (头前n个元素,剩余元素数量)
	 */
	public Tuple2<DFrame, DFrame> carcdr(final int n) {

		return Tuple2.of(this.rowS().limit(n).collect(DFrame.dfmclc), this.rowS().skip(n).collect(DFrame.dfmclc));
	}

	/**
	 * 按照行进行排序 <br>
	 * 例如:dfm.sortBy(Collections::reverse)
	 * 
	 * @param cs 排序函数
	 * @return DFrame
	 */
	public DFrame sortBy(final Consumer<List<IRecord>> cs) {
		final var rows = this.rows();
		cs.accept(rows);
		return rows.stream().collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行排序
	 * 
	 * @param comparator 行元比较器
	 * @return DFrame
	 */
	public DFrame sortBy(final Comparator<? super IRecord> comparator) {

		return this.rowS().sorted(comparator).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行排序
	 * 
	 * @param keys 键名比较器
	 * @return DFrame
	 */
	public DFrame sortBy(final String... keys) {

		return this.rowS().sorted((a, b) -> a.filter(keys).compareTo(b.filter(keys))) //
				.collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param predicate 筛选条件
	 * @return 指定条件
	 */
	public DFrame filterBy(final Predicate<IRecord> predicate) {

		return this.rowS().filter(predicate).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param key 键名
	 * @param obj 键值
	 * @return 指定键值的数据行
	 */
	public DFrame filterBy(final String key, final Object obj) {

		return this.filterBy(e -> Objects.equals(e.get(key), obj));
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param predicate 筛选条件
	 * @return 指定条件
	 */
	public DFrame filterByNot(final Predicate<IRecord> predicate) {

		return this.rowS().filter(predicate).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param key 键名
	 * @param obj 键值
	 * @return 指定键值的数据行
	 */
	public DFrame filterByNot(final String key, final Object obj) {

		return this.filterBy(e -> !Objects.equals(e.get(key), obj));
	}

	/**
	 * 按照行进行遍历变换函数(带有返回值)
	 * 
	 * @param action 变换函数
	 * @return DFrame 新创建的DFrame
	 */
	public DFrame forEachBy(final Consumer<? super IRecord> action) {

		return this.rowS().map(e -> {
			if (null != action) {
				action.accept(e);
			}
			return e;
		}).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行遍历变换函数(带有返回值)
	 * 
	 * @param mapper 变换函数
	 * @return DFrame 新创建的DFrame
	 */
	public DFrame fmap(final Function<? super IRecord, IRecord> mapper) {

		return this.rowS().map(mapper).collect(DFrame.dfmclc);
	}

	/**
	 * 数据透视表（指标变换批量mapper）
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 列指标：分类结果的计算器
	 * @return 按照keys标识的pivotPath所组织的阶梯式指标数据格式
	 */
	public <U> IRecord pivotTable(final String keys, final Function<Stream<IRecord>, U> mapper) {

		return this.rowS().collect(IRecord.pvtclc(keys, mapper));
	}

	/**
	 * 数据透视表（指标变换批量mapper）
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 列指标：分类结果的计算器
	 * @return 按照keys标识的pivotPath所组织的阶梯式指标数据格式
	 */
	public <U> IRecord pivotTable(final String keys[], final Function<Stream<IRecord>, U> mapper) {

		return this.rowS().collect(IRecord.pvtclc(keys, mapper));
	}

	/**
	 * 数据透视表（指标变换批量mapper）
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 列指标：分类结果的计算器
	 * @return 按照keys标识的pivotPath所组织的阶梯式指标数据格式
	 */
	public <U> IRecord pivotTable0(final String keys, final Function<List<IRecord>, U> mapper) {

		return this.rowS().collect(IRecord.pvtclc(keys, ss -> mapper.apply(ss.toList())));
	}

	/**
	 * 数据透视表（指标变换逐行mapper）
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 列指标：分类结果的计算器
	 * @return 按照keys标识的pivotPath所组织的阶梯式指标数据格式
	 */
	public <U> IRecord pivotTable1(final String keys, final Function<IRecord, U> mapper) {

		return this.rowS().collect(IRecord.pvtclc1(keys, mapper));
	}

	/**
	 * 数据透视表（指标变换逐行mapper）
	 * 
	 * @param <U>    结果类型
	 * @param keys   键名序列,用逗号分隔
	 * @param mapper 列指标：分类结果的计算器
	 * @return 按照keys标识的pivotPath所组织的阶梯式指标数据格式
	 */
	public <U> IRecord pivotTable1(final String keys[], final Function<IRecord, U> mapper) {

		return this.rowS().collect(IRecord.pvtclc1(keys, mapper));
	}

	/**
	 * 边际统计
	 * 
	 * @return 返回行统计
	 */
	public DFrame summary() {

		return this.summary(2);
	}

	/**
	 * 边际统计
	 * 
	 * @param margin 统计边际 1:按照行统计,2或其他,按照列进行统计
	 * @return 返回行统计
	 */
	public DFrame summary(final int margin) {

		final var numS = margin == 1 // 数值流
				? this.rowS().map(e -> DFrame.dblS(e.values())) // 行数据流
				: this.colS(DFrame::dblS); // 列数据流

		return numS.map(ds -> ds.summaryStatistics()).map(e -> {
			return IRecord.REC("min", e.getMin(), "max", e.getMax(), //
					"avg", e.getAverage(), "sum", e.getSum(), //
					"n", e.getCount());
		}).collect(DFrame.dfmclc);

	}

	/**
	 * 将key所指定的数据数值化
	 * 
	 * @param key 键名
	 * @return key 所指定的列的数值的数值序列
	 */
	public DoubleStream dblS(final String key) {

		return DFrame.dblS(this.lls(key));
	}

	/**
	 * 将key所指定的数据数值化
	 * 
	 * @param idx 键名索引从0开始
	 * @return key 所指定的列的数值的数值序列
	 */
	public DoubleStream dblS(final Integer idx) {

		return this.dblS(this.keyOfIndex(idx));
	}

	/**
	 * 列的平均值
	 * 
	 * @return 列的平均值
	 */
	public long count() {

		return this.dblS(0).summaryStatistics().getCount();
	}

	/**
	 * 列的平均值
	 * 
	 * @param key 键名
	 * @return 列的平均值
	 */
	public long count(final String key) {

		return this.dblS(key).summaryStatistics().getCount();
	}

	/**
	 * 列的平均值
	 * 
	 * @param idx 键名索引从0开始
	 * @return 列的平均值
	 */
	public long count(final Integer idx) {

		return this.count(this.keyOfIndex(idx));
	}

	/**
	 * 列的平均值
	 * 
	 * @param key 键名
	 * @return 列的平均值
	 */
	public double sum(final String key) {

		return this.dblS(key).summaryStatistics().getSum();
	}

	/**
	 * 列的平均值
	 * 
	 * @param idx 键名索引从0开始
	 * @return 列的平均值
	 */
	public double sum(final Integer idx) {

		return this.sum(this.keyOfIndex(idx));
	}

	/**
	 * 列的平均值
	 * 
	 * @param key 键名
	 * @return 列的平均值
	 */
	public double avg(final String key) {

		return this.dblS(key).summaryStatistics().getAverage();
	}

	/**
	 * 列的平均值
	 * 
	 * @param idx 键名索引从0开始
	 * @return 列的平均值
	 */
	public double avg(final Integer idx) {

		return this.avg(this.keyOfIndex(idx));
	}

	/**
	 * 列的最大值
	 * 
	 * @param key 键名
	 * @return 列的最大值
	 */
	public double min(final String key) {

		return this.dblS(key).summaryStatistics().getMin();
	}

	/**
	 * 列的最大值
	 * 
	 * @param idx 键名索引从0开始
	 * @return 列的最大值
	 */
	public double min(final Integer idx) {

		return this.min(this.keyOfIndex(idx));
	}

	/**
	 * 列的最大值
	 * 
	 * @param key 键名
	 * @return 列的最大值
	 */
	public double max(final String key) {

		return this.dblS(key).summaryStatistics().getMax();
	}

	/**
	 * 列的最大值
	 * 
	 * @param idx 键名索引从0开始
	 * @return 列的最大值
	 */
	public double max(final Integer idx) {

		return this.max(this.keyOfIndex(idx));
	}

	/**
	 * 生成统一的序列化对象序列,列顺序
	 * 
	 * @return 数据流
	 */
	public Object data() {

		return this.dataS().toArray();
	}

	/**
	 * 生成统一的序列化对象序列,列顺序
	 * 
	 * @return 数据流
	 */
	public Stream<Object> dataS() {

		return this.dataS(2);
	}

	/**
	 * 生成统一的序列化对象序列,列顺序
	 * 
	 * @return 数据流
	 */
	public List<Object> datas() {

		return this.dataS().toList();
	}

	/**
	 * 生成统一的序列化对象序列,列顺序
	 * 
	 * @param <A>       元素类型
	 * @param generator a function which produces a new array of the desiredtype and
	 *                  the provided length
	 * @return 数据数组
	 */
	public <A> A[] data(final IntFunction<A[]> generator) {

		return this.dataS().toArray(generator);
	}

	/**
	 * 生成统一的序列化对象序列
	 * 
	 * @param margin 统计边际 1:按照行统计,2或其他,按照列进行统计
	 * @return 数据流
	 */
	public Stream<Object> dataS(final int margin) {

		final Stream<Stream<Object>> dataS = margin == 1 //
				? this.rowS(e -> e.values().stream()) //
				: this.colS(e -> e.stream());
		return dataS.flatMap(e -> e);
	}

	/**
	 * 生成统一的序列化对象序列
	 * 
	 * @param <T>    元素类型
	 * @param <U>    值类型
	 * @param margin 统计边际 1:按照行统计,2或其他,按照列进行统计
	 * @return 数据流
	 */
	@SuppressWarnings("unchecked")
	public <T, U> Stream<U> dataS(final int margin, final Function<T, U> mapper) {

		return this.dataS(margin).map(e -> mapper.apply((T) e));
	}

	/**
	 * 提取指定名称额缓存
	 * 
	 * @param <K> 键名类型
	 * @param <V> 值类型
	 * @param key 缓存
	 * @return 用key标识的缓存
	 */
	@SuppressWarnings("unchecked")
	public synchronized <K, V> Map<K, V> cache(final String key) {

		if (caches == null) {
			caches = new ConcurrentHashMap<>();
		}
		return (Map<K, V>) this.caches.computeIfAbsent(key, k -> new HashMap<>());
	}

	/**
	 * 缓存清空
	 */
	public void clear() {

		if (this.caches != null) {
			this.caches.clear();
		} // if
		this.caches = null;
		if (this.rowsCache != null) {
			this.rowsCache.clear();
		} // if
		this.rowsCache = null;
	}

	/**
	 * 行数量(length别名)
	 * 
	 * @return 行数量
	 */
	public int height() {
		return this.length();
	}

	/**
	 * 行数量
	 * 
	 * @return 行数量
	 */
	public int length() {
		return this.rows().size();
	}

	/**
	 * 列数量：等同于 size 方法
	 * 
	 * @return 列数量
	 */
	public int width() {
		return this.size();
	}

	/**
	 * 数据格式化
	 */
	@Override
	public String toString() {

		return this.toString2(Jdbcs.frt(2));
	}

	/**
	 * DFrame 构造一个数据框对象
	 * 
	 * @param kvps 键值序列,每个value都代表一个值列表 key0,value0,key1,value1
	 * @return DFrame 对象
	 */
	public static DFrame dfm(final Object... kvps) {

		return new DFrame(IRecord.REC(kvps).toMap());
	}

	/**
	 * 构建DFrame
	 * 
	 * @param rows 行数据
	 * @return DFrame
	 */
	public static DFrame of(final IRecord... rows) {

		final var dfm = Stream.of(rows).collect(dfmclc);
		return dfm;
	}

	/**
	 * 构建DFrame
	 * 
	 * @param rows 行数据
	 * @return DFrame
	 */
	public static DFrame of(final Iterable<IRecord> rows) {

		return StreamSupport.stream(rows.spliterator(), false).collect(dfmclc);
	}

	/**
	 * 浮点数的流
	 * 
	 * @param objs 浮点数对象
	 * @return DoubleStream
	 */
	public static DoubleStream dblS(final Iterable<?> objs) {

		return DFrame.dblS(objs, -0d);
	}

	/**
	 * 浮点数的流
	 * 
	 * @param objs         浮点数对象
	 * @param defaultValue 默认值
	 * @return DoubleStream
	 */
	public static DoubleStream dblS(final Iterable<?> objs, final double defaultValue) {

		final ToDoubleFunction<? super Object> mapper = e -> {
			if (e instanceof Double d) { //
				return d;
			} else if (e instanceof Number num) { //
				return num.doubleValue();
			} else if (e instanceof String s) { // 字符串类型
				var _d = 0d;
				try {
					_d = Double.parseDouble(s);
				} catch (Exception ex) {
					// do nothing
				}
				return _d;
			} else { // 非法值
				return defaultValue;
			}
		};
		return StreamSupport.stream(objs.spliterator(), false).mapToDouble(mapper);
	}

	/**
	 * 数据框归集器
	 * 
	 * @param <T>    归集元素类型
	 * @param mapper 数据变换
	 * @return 数据框归集器
	 */
	public static <T> Collector<T, ?, DFrame> dfmclc(final Function<T, IRecord> mapper) {

		return Collector.of((Supplier<List<IRecord>>) LinkedList::new, //
				(rows, t) -> rows.add(mapper.apply(t)), (a, b) -> { //
					a.addAll(b);
					return a;
				}, (rows) -> { // 行项目
					final var data = new LinkedHashMap<String, List<Object>>(); // 列式结构的数据框数据。
					for (final var row : rows) { // 依据行进行遍历
						for (final var key : row.keys()) { // 按照列进行逐个元素增长
							final var col = data.computeIfAbsent(key, k -> new LinkedList<>());
							col.add(row.get(key));
						} // for key 列名
					} // for rows
					final var dfm = new DFrame(data);
					dfm.rowsCache = new ArrayList<>(rows); // 更新行缓存,改用数组列表
					return dfm;
				});
	}

	/**
	 * 数据框归集器
	 */
	public static Collector<IRecord, ?, DFrame> dfmclc = dfmclc(IRecord::REC);

	/**
	 * 数据框归集器
	 */
	public static Collector<? super Map<?, ?>, ?, DFrame> dfmclc2 = dfmclc(IRecord::REC);

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 7525552043906704226L;

	/**
	 * 数据缓存
	 */
	private transient List<IRecord> rowsCache = null;
	private transient Map<String, Map<?, ?>> caches = null;

} // class DFrame
