package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;
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
	 * 按照行进行查找: many to one 关系的多方数据提取 <br>
	 * 根据一方数据的id从当前对象(多方)种提取指定对于一方的数据集合。
	 * 
	 * @param key   外键字段名
	 * @param oneId 一方Id
	 * @return 指定键值的数据行(多方数据集合)
	 */
	public DFrame many2one(final String key, final Object oneId) {

		return this.filterBy(key, oneId);
	}

	/**
	 * 洗牌式乱序
	 * 
	 * @return 洗牌式乱序
	 */
	public DFrame shuffle() {

		return this.rowS().map(e -> Tuple2.of(Math.random(), e)) // 加入排序字段
				.sorted((a, b) -> Objects.compare(a._1(), b._1(), null)).map(e -> e._2) //
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
			action.accept(e);
			return e;
		}).collect(DFrame.dfmclc);
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
	 * @param kvps 键值序列 key0,value0,key1,value1
	 * @return DataFrame 对象
	 */
	public static DFrame DFM(final Object... kvps) {

		final var n = kvps.length;
		final var rec = new DFrame();
		for (int i = 0; i < n - 1; i += 2)
			rec.add(kvps[i].toString(), kvps[i + 1]);
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
		return Collector.of( //
				(Supplier<List<IRecord>>) () -> new LinkedList<IRecord>(), //
				(a, b) -> a.add(mapper.apply(b)), (a, b) -> { //
					a.addAll(b);
					return a;
				}, (aa) -> { //
					final var data = new LinkedHashMap<String, List<Object>>();
					for (final var a : aa) {
						for (final var key : a.keys()) {
							final var col = data.computeIfAbsent(key, k -> new LinkedList<>());
							col.add(a.get(key));
						} // for
					} // for
					return new DFrame(data);
				});
	}

	/**
	 * 重写基础版本的数据行
	 */
	@Override
	public List<IRecord> rows() {
		return this.rows(true);
	}

	/**
	 * 带有缓存版本的 数据流
	 * 
	 * @param flag 是否清空缓存
	 * @return 数据项目
	 */
	public List<IRecord> rows(final boolean flag) {
		if (flag) {
			this.rowsCache = null;
		}
		return Optional.ofNullable(this.rowsCache).orElseGet(() -> this.rowsCache = super.rows());
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
	private List<IRecord> rowsCache = null;

} // class DataFrame
