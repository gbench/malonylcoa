package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.Objects;
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

		return this.filterRows(key, oneId);
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param predicate 筛选条件
	 * @return 指定条件
	 */
	public DFrame filterRows(final Predicate<IRecord> predicate) {

		return this.rowS().filter(predicate).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param key 键名
	 * @param obj 键值
	 * @return 指定键值的数据行
	 */
	public DFrame filterRows(final String key, final Object obj) {

		return this.filterRows(e -> Objects.equals(e.get(key), obj));
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param predicate 筛选条件
	 * @return 指定条件
	 */
	public DFrame filterRowsNot(final Predicate<IRecord> predicate) {

		return this.rowS().filter(predicate).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行查找
	 * 
	 * @param key 键名
	 * @param obj 键值
	 * @return 指定键值的数据行
	 */
	public DFrame filterRowsNot(final String key, final Object obj) {

		return this.filterRows(e -> !Objects.equals(e.get(key), obj));
	}

	/**
	 * 按照行进行遍历变换函数(带有返回值)
	 * 
	 * @param mapper 变换函数
	 * @return DFrame 新创建的DFrame
	 */
	public DFrame fmapByRow(final Function<? super IRecord, IRecord> mapper) {

		return this.rows().stream().map(mapper).collect(DFrame.dfmclc);
	}

	/**
	 * 按照行进行遍历变换函数(带有返回值)
	 * 
	 * @param action 变换函数
	 * @return DFrame 新创建的DFrame
	 */
	public DFrame forEachByRow(final Consumer<? super IRecord> action) {

		return this.rows().stream().map(e -> {
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
	 */
	public static Collector<IRecord, ?, DFrame> dfmclc = Collector.of( //
			(Supplier<List<IRecord>>) () -> new LinkedList<IRecord>(), //
			List::add, (a, b) -> { //
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

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 7525552043906704226L;

} // class DataFrame
