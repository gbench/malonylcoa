package gbench.util.jdbc.function;

/**
 * 可以抛出异常的函数
 * 
 * @author gbench
 *
 * @param <T> 源数据类型
 * @param <U> 源数据类型
 * @param <V> 结果数据类型
 */
@FunctionalInterface
public interface ExceptionalBiFunction<T, U, V> {
	/**
	 * 函数调用函数
	 * 
	 * @param t 函数参数
	 * @param u 函数参数
	 * @return V类型的函数
	 * @throws Exception 异常
	 */
	V apply(final T t, final U u) throws Exception;
}