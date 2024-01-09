package gbench.util.jdbc;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.jdbc.kvp.IRecord;

/**
 * IJdbcSession 专门定义了一套jdbc的操作函数。这些函数的操作不会关闭数据库连接。<br>
 * 这里提供一个半成品的IJdbcSession的实现，以方便具体IJdbcSession的具体实现，没必要每次都从零开始 <br>
 * 可以从AbstractJdbcSession开始，<br>
 *
 * @param <T> 交易
 * @param <D> 数据类型
 * @author gbench
 */
public abstract class AbstractJdbcSession<T, D> implements IJdbcSession<T, D> {

	@Override
	public IManagedStreams clear(final Stream<?> stream) {
		return this;
	}

	/**
	 * 转义字符的转回
	 * 
	 * @param line 字符串
	 * @return 转义字符
	 */
	public String unescape(final String line) {
		final var _sql = line != null //
				&& (line.indexOf("'{'") >= 0 || line.indexOf("''") >= 0) // 对于含有 brace 转义的语句 给予转回
						? MessageFormat.format(line, (Object[]) null)
						: line;
		return _sql;
	}

	/**
	 * 查询结果集合 <br>
	 * 
	 * AbstractJdbcSession 的 psql2recordS 的实现 <br>
	 * precords_throws 比 psql2records_throws 效率要高一点 <br>
	 * 
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * 
	 * @param sql    含有位置占位符的sql语句
	 * @param params 位置参数,{(pos0,value0),(pos1,value1),...}
	 * @return IRecord 集合
	 * @throws SQLException
	 */
	public Stream<IRecord> psql2recordS(final String sql, final Map<Integer, ?> params) throws SQLException {
		final var stream = IJdbcSession.psql2recordS(this.getConnection(), sql, params, false, false); // 采用流式实现方法
		this.add(stream);

		return stream;
	}//

	/**
	 * 更新数据:execute 与update 同属于与 update系列
	 * 
	 * @param sql    语句
	 * @param params 未知参数
	 * @return
	 * @throws SQLException
	 */
	public List<IRecord> psql2update(final String sql, final Map<Integer, Object> params) throws SQLException {
		return IJdbcSession.psql2recordS(this.getConnection(), sql, params, true, false).collect(Collectors.toList());
	}//

} // AbstractJdbcSession