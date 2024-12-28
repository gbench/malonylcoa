package gbench.util.jdbc.kvp;

import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Comparator;

/**
 * TreeRecord 区分对key的名称区分大小写
 * 
 * @author gbench
 *
 */
public class TreeRecord extends AbstractMapRecord implements IRecord {

	/**
	 * 字段构造函数，只能put存放在keys中的字段
	 * 
	 * @param keys 键使用,分割
	 */
	public TreeRecord(String keys) {
		this.intialize(Arrays.asList(keys.split("[,]+")));
	}

	/**
	 * 字段构造函数，只能put存放在keys中的字段
	 * 
	 * @param keys 键使用,分割
	 */
	public TreeRecord(List<String> keys) {
		this.intialize(keys);
	}

	public TreeRecord(Comparator<String> comparator) {
		data = new TreeMap<>(comparator);
		this.comparator = comparator;
	}

	public TreeRecord(String[] keys) {
		this.intialize(Arrays.asList(keys));
	}

	/**
	 * 按照字键名出现次序生成比较器
	 * 
	 * @param keys 键名序列
	 */
	public void intialize(List<String> keys) {
		// 比较器
		Comparator<String> comparator = (a, b) -> {
			int ret = 0;// 比较结果
			int i = 0;// 键名编号
			Map<String, Integer> map = new HashMap<>();
			for (String s : keys)
				map.put(s, i++);
			try {
				ret = map.get(a) - map.get(b);
			} catch (Exception e) {
				System.out.println("a:" + a + "\nb:" + b + ",没有出现在 键名列表中：" + map + "\n 无法进行数据判断");
				e.printStackTrace();
			} // try
			return ret;
		};

		data = new TreeMap<>(comparator);
		this.comparator = comparator;
	}

	/**
	 * 复制克隆
	 * 
	 * @return
	 */
	public IRecord duplicate() {
		final TreeRecord rec = this.imitate();
		this.kvs().forEach(kv -> rec.set(kv.key(), kv.value()));
		return rec;
	}

	public Comparator<String> getComparator() {
		return this.comparator;
	}

	/**
	 * 仿制就是为了重用原来的比较器
	 * 
	 * @return
	 */
	public TreeRecord imitate() {
		return new TreeRecord(comparator);
	}

	private static final long serialVersionUID = 5297462049239467986L;
	private Comparator<String> comparator = null; // 比较器
} // TreeRecord
