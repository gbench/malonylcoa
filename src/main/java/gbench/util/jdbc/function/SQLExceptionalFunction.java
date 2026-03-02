package gbench.util.jdbc.function;

import java.sql.SQLException;
import java.util.function.Function;

/**
 * 带有抛出异常的函数
 *
 * @param <T> 参数类型
 * @param <U> 返回类型
 * @author xuqinghua
 */
public interface SQLExceptionalFunction<T, U> {
	U apply(final T t) throws SQLException;

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
}