package gbench.util.jdbc.function;

import java.util.function.*;

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
	 * @param f
	 * @return
	 */
	default <V> ExceptionalFunction<V, U> compose(final ExceptionalFunction<V, T> f) {
		return v -> this.apply(f.apply(v));
	}

	/**
	 * 
	 * @param <V>
	 * @param f
	 * @return
	 */
	default <V> ExceptionalFunction<T, V> andThen(final ExceptionalFunction<U, V> f) {
		return t -> f.apply((this.apply(t)));
	}

	/**
	 * 返回之前执行函数
	 * 
	 * @param <V>
	 * @param callback
	 * @return
	 */
	default <V> ExceptionalFunction<T, U> onReturn(final ExceptionalConsumer<U> callback) {

		return t -> {
			final var u = this.apply(t);
			callback.accept(u);
			return u;
		};
	}

	/**
	 * 返回之前执行函数
	 * 
	 * @param <V>
	 * @param callback
	 * @return
	 */
	default <V> ExceptionalFunction<T, U> onReturn(final ExceptionalBiConsumer<T, U> callback) {

		return t -> {
			final var u = this.apply(t);
			callback.accept(t, u);
			return u;
		};
	}

	/**
	 * 
	 * @param <T>
	 * @param <U>
	 * @param efn
	 * @return
	 */
	default Function<T, U> noexcept() {
		return t -> {
			U u = null;
			try {
				u = this.apply(t);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return u;
		};
	}

	/**
	 * 转成Consumer
	 * 
	 * @param <T>
	 * @param <U>
	 * @param efn
	 * @return
	 */
	default Consumer<T> noexcept2cs() {
		return t -> {
			try {
				this.apply(t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}

	/**
	 * 
	 * @return
	 */
	default ExceptionalConsumer<T> except2cs() {
		return t -> {
			try {
				this.apply(t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}
}