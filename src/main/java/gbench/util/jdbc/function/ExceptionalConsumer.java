package gbench.util.jdbc.function;

/**
 * 可以抛出异常的消费函数
 * 
 * @author gbench
 *
 * @param <T> 源数据类型
 */
@FunctionalInterface
public interface ExceptionalConsumer<T> {
	/**
	 * 数据消费函数
	 * 
	 * @param t 函数参数
	 * @throws Exception 异常
	 */
	void accept(final T t) throws Exception;
}