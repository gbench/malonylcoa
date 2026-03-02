package gbench.util.jdbc.function;

import java.util.function.Predicate;

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

	/**
	 * 没有异常版本
	 * 
	 * @return
	 */
	default Predicate<T> noexcept() {
		return (t) -> {
			Boolean ret = false;
			try {
				ret = this.test(t);
			} catch (Exception e) {
			}
			return ret;
		};
	}
}