package gbench.util.jdbc.function;

import java.util.function.BiFunction;

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

	/**
	 * 不抛异常版本
	 * 
	 * @param <T>
	 * @param <U>
	 * @param efn
	 * @return
	 */
	public default BiFunction<T, U, V> noexcept() {
		return (t, u) -> {
			V v = null;
			try {
				v = this.apply(t, u);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return v;
		};
	}

	/**
	 * 返回之前执行函数
	 * 
	 * @param <V>
	 * @param callback
	 * @return
	 */
	default ExceptionalBiFunction<T, U, V> onReturn(final ExceptionalBiConsumer<T, U> callback) {

		return (t, u) -> {
			final var v = this.apply(t, u);
			callback.accept(t, u);
			return v;
		};
	}
}