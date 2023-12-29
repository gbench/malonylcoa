package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import gbench.util.jdbc.Jdbcs;

/**
 * DataFrame 数据框，(key,[v0,v1,...]) 结构的键值对集合 kvs 即值元素为集合类型的KVPs.<br>
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
	 * lhs.many2one(oneId) <br>
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

		return this.rowS().findAny().orElse(null);
	}

	/**
	 * 尾部元素(除掉第一个元素后的剩余)
	 * 
	 * @return 尾部元素
	 */
	public DFrame tail() {

		return this.rowS().skip(1).collect(DFrame.dfmclc);
	}

	/**
	 * 尾部元素
	 * 
	 * @param n 提取n个尾部元素,当n大于整个长度的时候，返回整个元素
	 * @return 尾部元素
	 */
	public DFrame tail(final int n) {

		final var rows = this.rows(); // 提取数据行
		final var len = rows.size();
		final var startIndex = len - n; // 起始索引
		final var sublist = rows.subList(startIndex < 0 ? 0 : startIndex, len);

		return sublist.stream().collect(DFrame.dfmclc);
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
	 * @param mapper 变换函数
	 * @return DFrame 新创建的DFrame
	 */
	public DFrame fmapBy(final Function<? super IRecord, IRecord> mapper) {

		return this.rowS().map(mapper).collect(DFrame.dfmclc);
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
	 * 数据格式化
	 */
	@Override
	public String toString() {

		return this.toString2(Jdbcs.frt(2));
	}

	/**
	 * DataFrame 构造一个数据框对象
	 * 
	 * @param kvps 键值序列,每个value都代表一个值列表 key0,value0,key1,value1
	 * @return DataFrame 对象
	 */
	public static DFrame DFM(final Object... kvps) {

		final var n = kvps.length;
		final var rec = new DFrame();
		for (int i = 0; i < n - 1; i += 2) {
			rec.add(kvps[i].toString(), kvps[i + 1]);
		}
		return rec;
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
				}, (rows) -> { //
					final var data = new LinkedHashMap<String, List<Object>>(); // 列式结构的数据框数据。
					for (final var row : rows) { // 依据行进行遍历
						for (final var key : row.keys()) { // 按照列进行逐个元素增长
							final var col = data.computeIfAbsent(key, k -> new LinkedList<>());
							col.add(row.get(key));
						} // for key 列名
					} // for rows
					final var dfm = new DFrame(data);
					dfm.rowsCache = rows; // 更新行缓存
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

} // class DataFrame
