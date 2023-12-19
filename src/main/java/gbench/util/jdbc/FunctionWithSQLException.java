package gbench.util.jdbc;

import java.sql.SQLException;

/**
 * 带有抛出异常的函数
 *
 * @param <T> 参数类型
 * @param <U> 返回类型
 * @author xuqinghua
 */
public interface FunctionWithSQLException<T, U> {
	U apply(T t) throws SQLException;
}