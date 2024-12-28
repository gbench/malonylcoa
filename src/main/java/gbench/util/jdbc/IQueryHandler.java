package gbench.util.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 對查詢結果進行操作
 * 
 * @author gbench
 *
 * @param <T> 返回值类型
 */
public interface IQueryHandler<T> {
	/**
	 * 對查詢結果進行處理（結果已經被生成道了rs之中）
	 * 
	 * @param conn 數據庫練級 通常不需要操作
	 * @param stmt 查詢語句 通常不需要操作
	 * @param rs   返回結果
	 * @param rsm  結果信息結構
	 * @param n    結果列數
	 * @return 處理結果
	 */
	T handle(final Connection conn, final Statement stmt, final ResultSet rs, final ResultSetMetaData rsm,
			final int n) throws SQLException;
}