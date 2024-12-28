package gbench.util.jdbc.function;

import java.sql.SQLException;

/**
 * 带有抛出异常的函数
 *
 * @param <T> 参数类型
 * @param <U> 返回类型
 * @author xuqinghua
 */
public interface SQLExceptionalFunction<T, U> {
	U apply(final T t) throws SQLException;
}