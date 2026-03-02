package gbench.util.jdbc.function;

import java.util.function.Function;

/**
 * 带有抛出异常的函数
 *
 * @param <T> 参数类型
 * @param <U> 返回类型
 * @author xuqinghua
 */
public interface ThrowableFunction<T, U> {
	U apply(final T t) throws Throwable;

	/**
	 * 没有异常版本
	 * 
	 * @return
	 */
	default Function<T, U> noexcept() {
		return t -> {
			U u = null;
			try {
				u = this.apply(t);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return u;
		};
	}
}