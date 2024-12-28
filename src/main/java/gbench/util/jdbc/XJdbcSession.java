package gbench.util.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Stream;

import gbench.util.jdbc.function.ExceptionalConsumer;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.sql.SQL;

/**
 * Session 的实现
 * 
 * @author gbench
 *
 * @param <T> 交易代码类型
 * @param <D> 状态数据类型
 * @param <X> 归集器结果类型
 */
public class XJdbcSession<T, D, X> implements IJdbcSession<T, D> {

	/**
	 * 构造函数
	 * 
	 * @param sess      数据会话
	 * @param collector 数据搜集器 [record]->x
	 */
	public XJdbcSession(final IJdbcSession<T, D> sess, final Collector<IRecord, ?, X> collector) {

		this.sess = sess;
		this.collector = collector;
	}

	/**
	 * 数据操作 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 * 
	 * @param sql 数据操作语句
	 * @return X对象
	 * @throws SQLException
	 */
	public X sql2u(final String sql) throws SQLException {

		return this.sql2u(sql, collector);
	}

	/**
	 * 数据操作
	 * 
	 * @param sql 数据操作语句
	 * @return X对象
	 * @throws SQLException
	 */
	public X sql2u(final SQL sql) throws SQLException {

		return this.sql2u(sql, collector);
	}

	/**
	 * 数据操作
	 * 
	 * @param sql    prepared sql 数据操作语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return X对象
	 * @throws SQLException
	 */
	public X psql2u(final SQL sql, final IRecord params) throws SQLException {

		return this.psql2u(sql, params, collector);
	}

	/**
	 * 数据操作
	 * 
	 * @param sql    prepared sql 数据操作语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return X对象
	 * @throws SQLException
	 */
	public X psql2u(final String sql, final IRecord params) throws SQLException {

		return this.sql2u(sql, params, collector);
	}

	/**
	 * 数据操作
	 * 
	 * @param sql    prepared sql 数据操作语句
	 * @param params prepared sql 语句中占位符参数的值数组
	 * @return X对象
	 * @throws SQLException
	 */
	public X psql2u(final String sql, final Object... params) throws SQLException {

		return this.psql2u(sql, params, collector);
	}

	/**
	 * 数据操作
	 * 
	 * @param sql    数据操作语句
	 * @param params 语句中占位符参数的值集合，位置从1开始
	 * @return X对象
	 * @throws SQLException
	 */
	public X psql2u(final String sql, final Map<Integer, ?> params) throws SQLException {

		return this.psql2u(sql, params, collector);
	}

	@Override
	public Stream<IRecord> psql2recordS(final String sql, final Map<Integer, ?> params) throws SQLException {

		return sess.psql2recordS(sql, params);
	}

	@Override
	public List<IRecord> psql2update(final String sql, final Map<Integer, Object> params) throws SQLException {

		return sess.psql2update(sql, params);
	}

	@Override
	public Set<Stream<?>> getActiveStreams() {

		return this.sess.getActiveStreams();
	}

	@Override
	public Connection getConnection() {

		return sess.getConnection();
	}

	@Override
	public Map<Object, Object> getAttributes() {

		return sess.getAttributes();
	}

	@Override
	public T transCode() {

		return sess.transCode();
	}

	@Override
	public D getData() {

		return sess.getData();
	}

	@Override
	public D setData(D data) {

		return sess.setData(data);
	}

	/**
	 * 交易会话
	 * 
	 * @param <T>       交易代码类型
	 * @param <D>       会话状态类型
	 * @param <X>       归集类型 的结果类型
	 * @param sess      对话对象
	 * @param collector 数据归集器 [rec] -> x
	 * @return XJdbcSession 数据操作会话
	 */
	public static <T, D, X> XJdbcSession<T, D, X> of(final IJdbcSession<T, D> sess,
			final Collector<IRecord, ?, X> collector) {

		return new XJdbcSession<>(sess, collector);
	}

	/**
	 * 创建并打开一个数据操作会话
	 * 
	 * @param <T>       交易代码类型
	 * @param <D>       状态数据类型
	 * @param <X>       归集器结果类型
	 * @param collector 数据归集器 [rec] -> x
	 * @param cs        session 会话 session -> {}
	 * @return DataManipulation 数据操作的会话
	 */
	public static <T, D, X> IDataManipulation<IJdbcSession<T, D>> OPEN(final Collector<IRecord, ?, X> collector,
			final ExceptionalConsumer<? super XJdbcSession<T, D, X>> cs) {

		return session -> { // 简单的会话封装 IJdbcSession -> XJdbcSession
			cs.accept(XJdbcSession.of(session, collector)); // 包装生成一个 XJdbcSession 类型的参数给予运行
		}; // 生成 DataManipulation 对象
	}

	private final IJdbcSession<T, D> sess; // 会话对象
	private final Collector<IRecord, ?, X> collector; // 归集器对象
} // XJdbcSessions