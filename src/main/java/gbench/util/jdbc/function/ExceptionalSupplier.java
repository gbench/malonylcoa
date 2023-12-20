package gbench.util.jdbc.function;

/**
 * 供应函数
 * 
 * @author gbench
 *
 * @param <T> 供应类型
 */
@FunctionalInterface
public interface ExceptionalSupplier<T> {

	/**
	 * 供应产出
	 * 
	 * @return T类型的结果
	 * @throws Exception 异常
	 */
	T get() throws Exception;
}