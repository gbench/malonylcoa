package gbench.util.jdbc.kvp;

import gbench.util.jdbc.Jdbcs;

/**
 * DataFrame 数据框，(key,[v0,v1,...]) 结构的键值对集合 kvs 即值元素为集合类型的KVPs.<br>
 * 术语来源于R，<r>
 * 
 * @author gbench
 *
 */
public class DataFrame extends LinkedRecord {

	/**
	 * 数据格式化
	 */
	public String toString() {
		return this.toString2(Jdbcs.frt(2));
	}

	/**
	 * DataFrame 构造一个数据框对象
	 * 
	 * @param objects 键值序列 key0,value0,key1,value1
	 * @return DataFrame 对象
	 */
	public static DataFrame DFM(final Object... objects) {
		final var n = objects.length;
		final var rec = new DataFrame();
		for (int i = 0; i < n - 1; i += 2)
			rec.add(objects[i].toString(), objects[i + 1]);
		return rec;
	}

	private static final long serialVersionUID = 1L;
}// class DataFrame
