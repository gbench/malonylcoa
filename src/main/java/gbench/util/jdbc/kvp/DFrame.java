package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collector;

import gbench.util.jdbc.Jdbcs;

/**
 * DataFrame 数据框，(key,[v0,v1,...]) 结构的键值对集合 kvs 即值元素为集合类型的KVPs.<br>
 * 术语来源于R，<r>
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
	 * @return dframe 归集器
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
