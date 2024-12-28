package gbench.util.jdbc.function;

/**
 * 可以抛出异常的谓词判断函数
 * 
 * @author gbench
 *
 * @param <T> 源数据类型
 */
@FunctionalInterface
public interface ExceptionalPredicate<T> {
	/**
	 * 谓词判断函数
	 * 
	 * @param t 函数参数
	 * @return 布尔类型的结果
	 * @throws Exception 异常
	 */
	Boolean test(final T t) throws Exception;
}