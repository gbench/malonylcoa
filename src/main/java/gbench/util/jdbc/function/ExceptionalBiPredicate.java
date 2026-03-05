package gbench.util.jdbc.function;

import java.util.function.BiPredicate;

/**
 * 可以抛出异常的谓词判断函数
 * 
 * @author gbench
 *
 * @param <T> 源数据类型
 * @param <U> 源数据类型
 */
@FunctionalInterface
public interface ExceptionalBiPredicate<T, U> {
	/**
	 * 谓词判断函数
	 * 
	 * @param t 函数参数
	 * @param u 函数参数
	 * @return 布尔类型的结果
	 * @throws Exception 异常
	 */
	Boolean test(final T t, final U u) throws Exception;

	/**
	 * 没有异常版本
	 * @return
	 */
	default BiPredicate<T, U> noexcept() {
		return (t, u) -> {
			Boolean ret = false;
			try {
				ret = this.test(t, u);
			} catch (Exception e) {
			}
			return ret;
		};
	}
}