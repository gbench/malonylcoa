package gbench.util.jdbc.kvp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Paired Vector
 * 
 * @param <K> 键名类型
 * @param <V> 键值类型
 */
public class PVec<K, V> extends ArrayList<Tuple2<K, V>> {

	/**
	 * 构造函数
	 *
	 * @param pairs 键值对儿集合
	 */
	public <U extends Tuple2<K, V>> PVec(final Iterable<U> pairs) {
		super();
		pairs.forEach(this::add);
	}

	/**
	 * 构造函数
	 *
	 * @param pairs 键值对儿集合
	 */
	@SafeVarargs
	public <U extends Tuple2<K, V>> PVec(final U... pairs) {
		super();
		this.addAll(Arrays.asList(pairs));
	}

	/**
	 * 
	 * @param bics
	 */
	public void forEach(final BiConsumer<K, V> bics) {
		this.forEach(p -> bics.accept(p._1(), p._2()));
	}

	/**
	 * 单值 归集,pvec归集器,pvec3 表示这是一个 对T类型(Three的开头字母,也是pvecXclc系列的发生序号)的元素 归集。
	 *
	 * @param <T>    源数据元素类型
	 * @param <K>    键名类型
	 * @param <V>    键值类型
	 * @param mapper 元组转换器 t-&gt;(k,v)
	 * @return 把T类型的流归结成PVec&lt;K,U&gt; 类型的归集器
	 */
	public static <T, K, V> Collector<T, ?, PVec<K, V>> pveclc(final Function<T, Tuple2<K, V>> mapper) {
		return Collector.of(ArrayList::new, (List<Tuple2<K, V>> aa, T t) -> aa.add(mapper.apply(t)), (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, PVec::new);
	}

	/**
	 * 构造要给键值对儿序列
	 * 
	 * @param kvs 键,值序列:key0,value0,key1,value1,....
	 * @return PVec
	 */
	public static PVec<String, Object> PVEC(final Object... kvs) {
		final var data = new ArrayList<Tuple2<String, Object>>(kvs.length / 2);
		for (int i = 0; i < kvs.length - 1; i += 2) {
			data.add(new Tuple2<>(String.valueOf(kvs[i]), kvs[i + 1]));
		}
		return new PVec<>(data);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 9177955682722683607L;

}
