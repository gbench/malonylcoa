package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

/**
 * LinkedRecord 区分对key的名称区分大小写 <br>
 * 注意:LinkedRecord 采用默认的 hashcode(地址哈希)以及地址比较的equals,这样做的目的就是:使得<br>
 * Map&lt;IRecord,?&gt; 即以 LinkedRecord 为键类型的 HashMap可以拥有重复的键值,而这对于<br>
 * 同名键比如:Neo4jApp的graph系函数来说<br>
 * 即同名边的属性信息集的生成是很有必要的，特此给予提醒<br>
 * 判断两个LinkedRecord可以采用 comparedTo 是否等于0来进行比较。
 * 
 * @author gbench
 *
 */
public class LinkedRecord extends AbstractMapRecord2<LinkedRecord> implements IRecord {

	/**
	 * 序列构造函数
	 * 
	 * @param oo 键值序列 :k0,v0,k1,v1,...
	 */
	public LinkedRecord(final Object... oo) {
		if (data == null) {
			data = new LinkedHashMap<>();
		}

		for (int i = 0; i < oo.length; i += 2) {
			data.put(oo[i] + "", i < oo.length - 1 ? oo[i + 1] : null);
		}
	}// 键值序列构造函数

	/**
	 * 默认构造函数
	 */
	public LinkedRecord() {
		data = new LinkedHashMap<>();
	}

	/**
	 * 使用map来初始化记录对象：Record 存储一个对initData对象的拷贝。
	 * 
	 * @param initData 记录对象初始化:
	 */
	public LinkedRecord(final Map<String, ?> initData) {
		Map<String, Object> oo = new LinkedHashMap<>();
		if (initData != null)
			oo.putAll(initData);
		data = initData != null ? oo : new LinkedHashMap<>();
	}

	/**
	 * 建立以keys为字段的记录对象，并且每个key的值采用initData中对应键值的值进行初始化 <br>
	 * 当map中出现不再keys中出现的记录数据，给予舍弃，即LinkedRecord是严格 按照keys的结构进行构造 <br>
	 * 
	 * @param keys     字段序列
	 * @param initData 默认值集合。超出keys中的数据将给予舍弃
	 */
	public LinkedRecord(final List<String> keys, final Map<String, ?> initData) {
		this.intialize(keys, initData);
	}

	/**
	 * 建立以keys为字段的记录对象，并且每个key的值采用initData中对应键值的值进行初始化 <br>
	 * 当map中出现不再keys中出现的记录数据，给予舍弃，即LinkedRecord是严格 按照keys的结构进行构造 <br>
	 * 
	 * @param keys     字段序列
	 * @param initData 默认值集合。超出keys中的数据将给予舍弃
	 */
	public LinkedRecord(final String keys[], final Map<String, ?> initData) {
		this.intialize(Arrays.asList(keys), initData);
	}

	/**
	 * 建立以keys为字段的记录对象，并且每个key的值采用initData中对应键值的值进行初始化 <br>
	 * 当map中出现不再keys中出现的记录数据，给予舍弃，即LinkedRecord是严格 按照keys的结构进行构造 <br>
	 * 
	 * @param keys     字段序列,使用逗号进行分隔
	 * @param initData 默认值集合。超出keys中的数据将给予舍弃
	 */
	public LinkedRecord(final String keys, final Map<String, ?> initData) {
		if (keys == null) {
			this.data = new LinkedHashMap<>();
		} else {
			this.intialize(Arrays.asList(keys.split("[,]+")), initData);
		}
	}

	/**
	 * 建立以keys为字段的记录对象，并且每个key额值采用map中对应键值的值进行初始化 <br>
	 * 当map中出现不再keys中出现的记录数据，给予舍弃，即LinkedRecord是严格 按照keys的结构进行构造 <br>
	 * 
	 * @param keys 字段序列
	 * @param map  默认值集合。超出keys中的数据将给予舍弃
	 */
	public void intialize(final List<String> keys, final Map<String, ?> map) {
		@SuppressWarnings("unchecked")
		Map<String, Object> initData = (Map<String, Object>) (map != null ? map : new LinkedHashMap<>());// 初始数据
		data = new LinkedHashMap<>();
		if (keys == null || keys.size() <= 0) {
			// do nothing
		} else {// 仅当keys 有效是才给予字段初始化
			keys.stream().filter(Objects::nonNull)
					.forEach(key -> data.computeIfAbsent(key, k -> initData.getOrDefault(k, "-")));
		} // if
	}

	/**
	 * 复制一个 Record 对象
	 */
	@Override
	public LinkedRecord duplicate2() {
		return new LinkedRecord(this.data);
	}

	/**
	 * 数据便历
	 * 
	 * @param bics 二元接收函数 (k,v)->{}
	 */
	public void forEach2(final BiConsumer<? super String, ? super Object> bics) {
		this.data.forEach(bics);
	}

	/**
	 * 使用map来初始化记录对象,注意这里是直接使用把 initData来给予包装的。：这是一种快速构造方法。<br>
	 * 但会造成实际结构不是LinkedMap,这个适合于属性名称不重要的情况。比如以集合的形式来统一传递参数。 <br>
	 * 
	 * @param initData 记录对象初始化
	 */
	@SuppressWarnings("unchecked")
	public static LinkedRecord of(final Map<?, ?> initData) {
		final var rec = new LinkedRecord();
		rec.data = (Map<String, Object>) initData;
		return rec;
	}

	/**
	 * 使用recB 来覆盖recA merged recA 和 recB 的各个各个属性的集合。
	 * 
	 * @param recA 第一个IRecord
	 * @param recB 将要覆盖中的属性的数据,会利用recB中空值覆盖a中的空值
	 * @return merged recA 和 recB 的各个各个属性的集合。这是一个新生成的数据对象，该操作不会对recA 和 recB 有任何涌向
	 */
	public static IRecord extend(final IRecord recA, final IRecord recB) {
		final IRecord rec = new LinkedRecord();//
		if (recA != null) {
			recA.kvs().forEach(kv -> rec.add(kv.key(), kv.value()));
		}
		if (recB != null) {
			recB.kvs().forEach(kv -> rec.set(kv.key(), kv.value()));
		}
		return rec;
	}

	private static final long serialVersionUID = 1060363673536668645L;
} // LinkedRecord
