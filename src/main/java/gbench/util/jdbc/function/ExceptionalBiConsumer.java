package gbench.util.jdbc.function;

import java.util.function.BiConsumer;

/**
 * 可以抛出异常的二元消费函数
 * 
 * @author gbench
 *
 * @param <T> 源数据类型
 * @param <U> 源数据类型
 */
@FunctionalInterface
public interface ExceptionalBiConsumer<T, U> {
	/**
	 * 数据消费函数
	 * 
	 * @param t 函数参数
	 * @throws Exception 异常
	 */
	void accept(final T t, final U u) throws Exception;

	/**
	 * 没有异常版本
	 * 
	 * @return
	 */
	default BiConsumer<T, U> noexcept() {
		return (t, u) -> {
			try {
				this.accept(t, u);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}
}