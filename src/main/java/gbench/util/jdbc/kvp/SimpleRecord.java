package gbench.util.jdbc.kvp;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * 简单的记录类型,特点就是 键名可以重复
 * 
 * @author gbench
 *
 */
public class SimpleRecord implements IRecord {

	/**
	 * 复制克隆
	 * 
	 * @return IRecord 的克隆
	 */
	public IRecord duplicate() {
		SimpleRecord rec = new SimpleRecord();
		this.kvs().forEach(kv -> rec.set(kv.key(), kv.value()));
		return rec;
	}

	/**
	 * 添加一个键值，如果存在则添加一个重复的键
	 * 
	 * @return IRecord 对象本身
	 */
	@Override
	public IRecord add(final Object key, final Object value) {
		kvs.add(new KVPair<>(key.toString(), value));
		return this;
	}

	/**
	 * 设置键值key的值,如果键值已经存在则修改，否则添加
	 * 
	 * @return IRecord 对象本身
	 */
	@Override
	public IRecord set(final String key, final Object value) {
		final var kp = this.kvs().stream().filter(kv -> kv.key().equals(key)).findFirst();
		if (kp.isPresent())
			kp.get().value(value);
		else
			kvs.add(new KVPair<>(key, value));

		return this;
	}

	/**
	 * 忽略key的大小写，相同key仅寻找第一项
	 * 
	 * @param key 键值表
	 * @return key 所对应的值元素
	 */
	@Override
	public Object get(final String key) {
		final Optional<KVPair<String, Object>> opt = this.kvs.stream().filter(e -> e.key().equalsIgnoreCase(key))
				.findFirst();
		if (opt.isPresent()) {
			return opt.get().value();
		} else {
			return null;
		}
	}

	/**
	 * 忽略key的大小写，相同key仅寻找第一项
	 * 
	 * @param key 键值表
	 * @return 对象的key所对应的说有值
	 */
	@Override
	public List<Object> gets(final String key) {
		final List<Object> ll = this.kvs.stream().filter(e -> e.key().equalsIgnoreCase(key)).map(KVPair::value)
				.collect(Collectors.toList());
		return ll;
	}

	/**
	 * 键值对的列表
	 * 
	 * @return 键值对的列表
	 */
	@Override
	public List<KVPair<String, Object>> kvs() {
		return kvs;
	}

	/**
	 * 键值对儿的流
	 * 
	 * @return Stream&lt;KVPair&lt;String,Object&gt;&gt; 的流
	 */
	@Override
	public Stream<KVPair<String, Object>> tupleS() {
		return kvs.stream();
	}

	/**
	 * hashCode
	 * 
	 * @return hashCode
	 */
	@Override
	public int hashCode() {
		return this.hashint();
	}

	/**
	 * 等价判断算法
	 * 
	 * @param obj 另一个比较对象
	 * @return 等价判断算法
	 */
	public boolean equals(final Object obj) {
		return this.toString().equals(obj.toString());
	}

	/**
	 * 转换成{(String,Object)} 类型的Map
	 * 
	 * @return {(String,Object)} 类型的Map
	 */
	@Override
	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		this.kvs().forEach(kv -> map.put(kv.key(), kv.value()));
		return map;
	}

	/**
	 * 字符串格式化 <br>
	 * 例如： final var r = REC(); <br>
	 * r.set("a", r); // 构造一个递归引用的数据对象 println(r); <br>
	 * 返回a:a:a:a:a:a:a:a:a:a:a:a:a:a:a:a:[a] <br>
	 * 其中 a:a:a:就是进行递归的层级痕迹,最后的 [a] 是 keys的字符串
	 */
	@Override
	public String toString() {

		if (Thread.currentThread().getStackTrace().length > MAX_STACK_TRACE_SIZE) {
			System.err.println("数据太大或是出现了递归的成员引用,仅返回keys的引用");
			return this.keys().toString();
		}

		return this.kvs().stream().map(e -> e.key() + ":" + e.value()).collect(Collectors.joining(","));
	}

	/**
	 * 获取指定键值中的数据的第一条
	 * 
	 * @param recs  记录集合：查找范围
	 * @param key   键名
	 * @param value 键值
	 * @return 记录信息
	 */
	public static Optional<IRecord> fetchOne(final List<IRecord> recs, final String key, Object value) {
		return recs.stream().filter(e -> e.get(key).equals(value)).findFirst();
	}

	/**
	 * 获取指定键值中的数据的第一条
	 * 
	 * @param recs  记录集合：查找范围
	 * @param key   键名
	 * @param value 键值
	 * @return 记录信息
	 */
	public static List<IRecord> fetchAll(final List<IRecord> recs, final String key, final Object value) {

		return recs.stream().filter(e -> e.get(key).equals(value)).collect(Collectors.toList());
	}

	/**
	 * 使用recB 来覆盖recA merged recA 和 recB 的各个各个属性的集合。
	 * 
	 * @param recA 源IRecord，基础IRecord
	 * @param recB 将要覆盖中的属性的数据,会利用recB中空值覆盖a中的空值
	 * @return merged recA 和 recB 的各个各个属性的集合。这是一个新生成的数据对象，该操作不会对recA 和 recB 有任何涌向
	 */
	public static IRecord extend(final IRecord recA, final IRecord recB) {
		IRecord rec = new SimpleRecord();//
		if (recA != null) {
			recA.kvs().forEach(kv -> rec.add(kv.key(), kv.value()));
		}
		if (recB != null) {
			recB.kvs().forEach(kv -> rec.set(kv.key(), kv.value()));
		}
		return rec;
	}

	/**
	 * 创造一个Record对象,参数分别是：键名1,键值1,键名2,键值2,。。。的序列。
	 * 
	 * @param values 键值序列
	 * @return SimpleRecord 的序列结构可以含有重复的键名
	 */
	public static SimpleRecord REC2(final List<?> values) {
		return _REC2(values.toArray());
	}

	/**
	 * 创造一个Record对象,参数分别是：键名1,键值1,键名2,键值2,。。。的序列。
	 * 
	 * @param values 键值序列
	 * @return SimpleRecord 的序列结构可以含有重复的键名
	 */
	public static SimpleRecord REC2(final Stream<?> values) {
		return _REC2(values.toArray());
	}

	/**
	 * 创造一个Record对象,参数分别是：键名1,键值1,键名2,键值2,。。。的序列。
	 * 
	 * @param values 键值序列
	 * @return SimpleRecord 的序列结构可以含有重复的键名
	 */
	public static SimpleRecord REC2(final Object... values) {
		return _REC2(values);
	}

	/**
	 * 解释字符串生成SimpleRecord
	 * 
	 * @param jsonstr json格式的对象描述
	 * @return IRecord 对象
	 */
	@SuppressWarnings("unchecked")
	public static SimpleRecord REC2(final String jsonstr) {
		final ObjectMapper mapper = new ObjectMapper();
		final SimpleRecord rec = new SimpleRecord();
		if (jsonstr == null || jsonstr.matches("\\s*"))
			return rec;
		var json = jsonstr.trim();
		if (!json.startsWith("{"))
			json = "{" + json;// 补充开头的"{"
		if (!json.endsWith("}"))
			json = json + "}";// 补充结尾的"{"
		try {
			mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			mapper.readValue(json, Map.class).forEach((k, v) -> rec.add(k + "", v + ""));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rec;
	}

	/**
	 * 创造一个Record对象,参数分别是：键名1,键值1,键名2,键值2,。。。的序列。
	 * 
	 * @param values 键值序列
	 * @return SimpleRecord 的序列结构可以含有重复的键名
	 */
	public static SimpleRecord _REC2(final Object[] values) {
		final SimpleRecord rec = new SimpleRecord();
		if (values == null)
			return rec;
		for (int i = 0; i < values.length; i += 2) {
			final String key = values[i] + "";// 键值名
			final Object value = (i + 1) < values.length ? values[i + 1] : null;// 如果最后一位是key则它的value为null
			rec.add(key, value);
		} // for
		return rec;
	}

	/**
	 * 创造一个Record对象,参数分别是：kvp0,kvp1,kvp2,。。。的序列流。
	 * 
	 * @param kvps 键值序列流
	 * @return SimpleRecord 的序列结构可以含有重复的键名
	 */
	public static SimpleRecord KVS2REC(final Stream<? extends KVPair<String, ?>> kvps) {
		final var rec = new SimpleRecord();
		kvps.forEach(kvp -> rec.add(kvp.key(), kvp.value()));
		return rec;
	}

	/**
	 * 创造一个Record对象,参数分别是：kvp0,kvp1,kvp2,。。。的序列。
	 * 
	 * @param kvps 键值序列
	 * @return SimpleRecord 的序列结构可以含有重复的键名
	 */
	@SuppressWarnings("unchecked")
	public static SimpleRecord KVS2REC(final Collection<? extends KVPair<String, ?>> kvps) {
		final var rec = new SimpleRecord();
		rec.kvs.addAll((Collection<KVPair<String, Object>>) (Object) kvps);
		return rec;
	}

	private static final long serialVersionUID = 1L;
	private final List<KVPair<String, Object>> kvs = new LinkedList<>();
} // SimpleRecord
