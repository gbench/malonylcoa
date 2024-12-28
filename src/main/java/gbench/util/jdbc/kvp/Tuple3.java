package gbench.util.jdbc.kvp;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * 三元组
 *
 * @param <T> 第一元素类型
 * @param <U> 第二元素类型
 * @param <V> 第三元素类型
 * @author gbench
 */
public class Tuple3<T, U, V> {

	public Tuple3() {
	}

	public Tuple3(T t, U u, V v) {
		this._1 = t;
		this._2 = u;
		this._3 = v;
	}

	public T _1() {
		return _1;
	}

	public T _1(T t) {
		return _1 = t;
	}

	public U _2() {
		return _2;
	}

	public U _2(U u) {
		return _2 = u;
	}

	public V _3() {
		return _3;
	}

	public V _3(V v) {
		return _3 = v;
	}

	public Optional<V> __3() {
		return Optional.ofNullable(_3);
	}

	public int hashCode() {
		return _1 == null ? Integer.valueOf(0).hashCode() : this._1().hashCode();
	}

	public boolean equals(Object obj) {
		return _1 != null && _1().equals(obj);
	}

	public String toString() {
		return _1() + " --> " + _2() + " --> " + _3();
	}

	public static <T1, T2, T3> Tuple3<T1, T2, T3> TUP3(T1 _1, T2 _2, T3 _3) {
		return new Tuple3<>(_1, _2, _3);
	}

	/**
	 * tup collector
	 *
	 * @param <K>    键名
	 * @param <V>    值
	 * @param <U>    结果类型
	 * @param mapper 映射对象
	 * @return U类型的结果
	 */
	public static <K, V, U, O> Collector<Tuple3<K, V, U>, List<Tuple3<K, V, U>>, O> tupclc(
			Function<List<Tuple3<K, V, U>>, O> mapper) {
		return Collector.of(LinkedList::new, List::add, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, mapper);
	}

	private T _1;
	private U _2;
	private V _3;
}