package gbench.util.jdbc.function;

/**
 * 可以抛出异常的函数
 * 
 * @author gbench
 *
 * @param <T> 源数据类型
 * @param <U> 结果数据类型
 */
@FunctionalInterface
public interface ExceptionalFunction<T, U> {
	/**
	 * 函数调用函数
	 * 
	 * @param t 函数参数
	 * @return U类型的函数
	 * @throws Exception 异常
	 */
	U apply(final T t) throws Exception;

	/**
	 * 
	 * @param <V>
	 * @param v2t
	 * @return
	 */
	default <V> ExceptionalFunction<V, U> compose(final ExceptionalFunction<V, T> v2t) {
		return v -> this.apply(v2t.apply(v));
	}

	/**
	 * 
	 * @param <V>
	 * @param u2v
	 * @return
	 */
	default <V> ExceptionalFunction<T, V> andThen(final ExceptionalFunction<U, V> u2v) {
		return t -> u2v.apply((this.apply(t)));
	}
}