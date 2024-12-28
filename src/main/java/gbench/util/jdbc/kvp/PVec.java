package gbench.util.jdbc.kvp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

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
		pairs.forEach(this::add);
	}

	/**
	 * 构造函数
	 *
	 * @param pairs 键值对儿集合
	 */
	public <U extends Tuple2<K, V>> PVec(final Map<? extends K, ? extends V> pairs) {
		pairs.forEach((k, v) -> {
			this.add(new Tuple2<>(k, v));
		});
	}

	/**
	 * PVec 构造函数
	 * 
	 * @param <KK> 第一列表类型
	 * @param <VV> 第二列表类型
	 * @param kk   第一列表
	 * @param vv   第二列表
	 */
	public <KK extends Iterable<K>, VV extends Iterable<V>> PVec(final KK kk, final VV vv) {
		super();
		final var kitr = kk.iterator();
		final var vitr = vv.iterator();

		while (kitr.hasNext() || vitr.hasNext()) {
			final var k = kitr.hasNext() ? kitr.next() : null;
			final var v = vitr.hasNext() ? vitr.next() : null;
			this.add(Tuple2.of(k, v));
		}
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
	 * 流化处理
	 * 
	 * @param <U>    结果元素类型
	 * @param mapper 元素变化类型
	 * @return U 类型的流
	 */
	public <U> Stream<U> map(final BiFunction<K, V, U> mapper) {
		return this.stream().map(p -> mapper.apply(p._1, p._2));
	}

	/**
	 * 转换成Map
	 * 
	 * @return 转换成Map
	 */
	public Map<K, V> toMap() {
		final var lhm = new LinkedHashMap<K, V>();
		this.forEach(lhm::put);
		return lhm;
	}

	/**
	 * 变换成其他对象
	 * 
	 * @param <U>    其他对象
	 * @param mapper pvec-&gt;u
	 * @return U 类型的对象
	 */
	public <U> U mutate(final Function<PVec<K, V>, U> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 变换成其他对象
	 * 
	 * @param <U>    其他对象
	 * @param mapper m-&gt;u
	 * @return U 类型的对象
	 */
	public <U> U mutate2(final Function<Map<K, V>, U> mapper) {
		return mapper.apply(this.toMap());
	}

	/**
	 * 分组归集
	 * 
	 * @param <U>  归集值类型
	 * @param uclc V的归集器
	 * @return PVec
	 */
	public <U> PVec<K, U> groupBy(final Collector<? super V, ?, U> uclc) {
		return this.groupBy(vv -> vv.stream().collect(uclc));
	}

	/**
	 * 分组归集
	 * 
	 * @param <U>     归集值类型
	 * @param vmapper V的归集器
	 * @return PVec
	 */
	public <U> PVec<K, U> groupBy(final Function<List<V>, U> vmapper) {
		final var m = new LinkedHashMap<K, List<V>>();
		this.forEach((k, v) -> {
			m.computeIfAbsent(k, _k -> new LinkedList<>()).add(v);
		});
		return m.entrySet().stream().map(e -> Tuple2.of(e.getKey(), vmapper.apply(e.getValue())))
				.collect(PVec.pveclc());
	}

	/**
	 * 流化处理
	 * 
	 * @param <U>    结果元素类型
	 * @param mapper 元素变化类型
	 * @return U 类型的流
	 */
	/**
	 * 
	 * @param <A>    原类型
	 * @param <B>    目标类型
	 * @param <T>    目标键类型
	 * @param <U>    目标值类型
	 * @param mapper 变换函数 (k,v)->&gt;(t,u)
	 * @return pvec
	 */
	@SuppressWarnings("unchecked")
	public <A extends Tuple2<K, V>, B extends Tuple2<T, U>, T, U> PVec<T, U> fmap(final Function<A, B> mapper) {
		return this.stream().collect((Collector<? super Tuple2<K, V>, ?, PVec<T, U>>) PVec.pveclc(mapper));
	}

	/**
	 * 流化处理
	 *
	 * @param <T>     目标键类型
	 * @param <U>     目标值类型
	 * @param kmapper k-&gt;t
	 * @param vmapper v-&gt;u
	 * @return
	 */
	public <T, U> PVec<T, U> fmap(final Function<K, T> kmapper, final Function<V, U> vmapper) {
		return this.stream().collect(PVec.pveclc(e -> new Tuple2<>(kmapper.apply(e._1), vmapper.apply(e._2))));
	}

	/**
	 * 分拆成两个独立列表
	 * 
	 * @return ([k],[v])
	 */
	public Tuple2<List<K>, List<V>> unzip() {
		final var kk = new ArrayList<K>(this.size());
		final var vv = new ArrayList<V>(this.size());
		this.forEach(p -> {
			kk.add(p._1);
			vv.add(p._2);
		});
		return Tuple2.of(kk, vv);
	}

	/**
	 * 
	 * @param bics
	 */
	public void forEach(final BiConsumer<K, V> bics) {
		this.forEach(p -> bics.accept(p._1(), p._2()));
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof PVec<?, ?> p && this.size() == ((PVec<?, ?>) obj).size()) {
			final var iitr = this.iterator();
			final var jitr = p.iterator();
			while (iitr.hasNext()) {
				if (!Objects.equals(iitr.next(), jitr.next())) {
					break;
				}
			}
			return iitr.hasNext() ? false : true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.toArray());
	}

	@Override
	public String toString() {
		return this.toMap().toString();
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
	public static <T, K, V> Collector<T, ?, PVec<K, V>> pveclc(final Function<T, ? extends Tuple2<K, V>> mapper) {
		return Collector.of(ArrayList::new, (List<Tuple2<K, V>> aa, T t) -> aa.add(mapper.apply(t)), (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, PVec::new);
	}

	/**
	 * 单值 归集,pvec归集器,pvec3 表示这是一个 对T类型(Three的开头字母,也是pvecXclc系列的发生序号)的元素 归集。
	 *
	 * @param <T> 源数据元素类型
	 * @param <K> 键名类型
	 * @param <V> 键值类型
	 * @return 把T类型的流归结成PVec&lt;K,U&gt; 类型的归集器
	 */
	public static <T, K, V> Collector<? super Tuple2<K, V>, ?, PVec<K, V>> pveclc() {
		return pveclc(e -> e);
	}

	/**
	 * 构造要给键值对儿序列
	 * 
	 * @param kvs 键,值序列:key0,value0,key1,value1,....
	 * @return PVec
	 */
	public static PVec<Object, Object> PVEC(final Object... kvs) {
		final var data = new ArrayList<Tuple2<Object, Object>>(kvs.length / 2);
		for (int i = 0; i < kvs.length - 1; i += 2) {
			data.add(new Tuple2<>(kvs[i], kvs[i + 1]));
		}
		return new PVec<>(data);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 9177955682722683607L;

}
