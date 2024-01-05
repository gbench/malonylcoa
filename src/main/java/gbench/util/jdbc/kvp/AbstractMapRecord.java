package gbench.util.jdbc.kvp;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * TreeRecord 区分对key的名称区分大小写
 * 
 * @author gbench
 *
 */
public abstract class AbstractMapRecord implements IRecord {

	/**
	 * 返回自身便于实现链式编程
	 */
	@Override
	public IRecord add(Object key, Object value) {
		data.put(key.toString(), value);
		return this;
	}

	@Override
	public IRecord set(String key, Object value) {
		data.put(key, value);
		return this;
	}

	@Override
	public Object get(String key) {
		if (key == null)
			return null;
		Object o = data.get(key);
		if (o == null) {// 忽略大小写
			Optional<String> k = data.keySet().stream().filter(key::equalsIgnoreCase).findFirst();
			return k.isPresent() ? data.get(k.get()) : null;
		} // if
		return o;
	}

	@Override
	public List<Object> gets(String key) {
		return Collections.singletonList(this.get(key));
	}

	@Override
	public Stream<KVPair<String, Object>> tupleS() {
		return data.entrySet().stream().map(e -> new KVPair<>(e.getKey(), e.getValue()));
	}

	/**
	 * 字符串格式化 <br>
	 * 例如： final var r = REC(); <br>
	 * r.set("a", r); // 构造一个递归引用的数据对象 println(r); <br>
	 * 返回a:a:a:a:a:a:a:a:a:a:a:a:a:a:a:a:[a] <br>
	 * 其中 a:a:a:就是进行递归的层级痕迹,最后的 [a] 是 keys的字符串
	 */
	public String toString() {

		if (Thread.currentThread().getStackTrace().length > MAX_STACK_TRACE_SIZE) {
			System.err.println("数据太大或是出现了递归的成员引用,仅返回keys的引用");
			return this.keys().toString(); //
		}

		final var builder = new StringBuilder();
		final Function<Object, String> cell_formatter = v -> {
			if (v == null)
				return "(null)";
			var line = "{0}";// 数据格式化
			if (v instanceof Date) {
				line = "{0,Date,yyyy-MM-dd HH:mm:ss}"; // 时间格式化
			} else if (v instanceof Number) {
				line = "{0,Number,#}"; // 数字格式化
			} // if
			return MessageFormat.format(line, v);
		};// cell_formatter

		if (this.data != null) {
			this.data.forEach((k, v) -> builder.append(k).append(":").append(cell_formatter.apply(v)).append("\t"));
		}

		return builder.toString().trim();
	}

	/**
	 * 等价函数
	 */
	@Override
	public boolean equals(final Object rec) {
		if (rec == null || !(rec instanceof IRecord))
			return false;

		return ((IRecord) rec).compareTo(this) == 0;
	}

	/**
	 * 转换成 数据map
	 */
	public Map<String, Object> toMap() {
		return this.data;
	}

	/**
	 * 生成hashcode的方法
	 * 
	 * @return hashcode
	 */
	public int hashCode() {
		return this.hashint();
	}

	private static final long serialVersionUID = -6173203337428164904L;
	protected Map<String, Object> data = null;// 数据信息,使用TreeMap 就是为了保持key的顺序
} // AbstractMapRecord
