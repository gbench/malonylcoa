package gbench.util.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.sql.DataSource;

import gbench.util.jdbc.function.ExceptionalConsumer;
import gbench.util.jdbc.annotation.JdbcExecute;
import gbench.util.jdbc.annotation.JdbcQuery;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

/**
 * SQL数据库
 * 
 * @author gbench
 *
 */
public interface IMySQL {
	/**
	 * 获取数据库名称
	 */
	@JdbcQuery("select database() db")
	String getDbName();

	/**
	 * 创建一个数据库
	 * 
	 * @param dbName 数据库名称
	 */
	@JdbcExecute("create database {0} default character set utf8mb4")
	void createDatabase(String dbName);

	/**
	 * 删除一张表
	 * 
	 * @param tableName
	 */
	@JdbcExecute("drop table if exists {0}")
	void dropTable(String tableName);

	/**
	 * 查询用户表记录
	 * 
	 * @param tableName 表名
	 * @return 数据表行
	 */
	@JdbcQuery("select * from {0}")
	List<IRecord> getLines(String tableName);

	/**
	 * 查询用户表记录
	 * 
	 * @param tableName 表名
	 * @param maxSize   最大行数
	 * @return 数据表行
	 */
	@JdbcQuery("select * from {0} limit {1}")
	List<IRecord> getLines(String tableName, int maxSize);

	/**
	 * 返回一个获取代理对象 Jdbc 的代理对象：可以通过 IRecord.findOne(Jdbc.class) 获取jdbc对象。
	 * 
	 * @return Proxy
	 */
	IRecord getProxy();

	/**
	 * 返回数据表是否存在标记
	 * 
	 * @param tableName 表名
	 * @return 数据表是否存在。
	 */
	default boolean exists(final String tableName) {
		return this.jdbc().tblExists(tableName);
	}

	/**
	 * 仅仅返回 SqlPattern 并不给予对参数进行替换。
	 * 
	 * @param sqlpattern 一个#开头的变量名。
	 * @return SqlPatternPreprocessor 处理后的SQL语句。
	 */
	default String getSqlPattern(final String sqlpattern) {
		return this.getSql(sqlpattern, (IRecord) null);
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。
	 * @param params     sharp变量的占位符参数,键,值序列
	 * @return SqlPatternPreprocessor 处理后的SQL语句。
	 */
	default String getSql(final String sqlpattern, final Object... params) {
		return this.getSql(sqlpattern, IRecord.REC(params));
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。
	 * @param params     sharp变量的占位符参数
	 * @return SqlPatternPreprocessor 处理后的SQL语句。
	 */
	default String getSql(final String sqlpattern, final IRecord params) {
		final var proxy = this.getProxy();
		final var spp = proxy.findOne(ISqlPatternPreprocessor.class);
		final var jdbc = proxy.findOne(Jdbc.class);
		final var sql = spp.handle(null, params, sqlpattern, jdbc);

		return sql;
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。 <br>
	 *                   #标记的参数会被添加引号: select * from user where name=#name <br>
	 *                   ##标记的参数不会添加引号: select * from user limit ##cnt <br>
	 * @param params     sharp变量的占位符参数
	 * @return IRecord 流
	 */
	default Stream<IRecord> sqlqueryS(final String sqlpattern, final IRecord params) {
		final var proxy = this.getProxy();
		final var spp = proxy.findOne(ISqlPatternPreprocessor.class);
		final var jdbc = proxy.findOne(Jdbc.class);
		final var sql = spp.handle(null, params, sqlpattern, jdbc);

		return jdbc.sql2recordS(sql);
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。
	 * @return IRecord 流
	 */
	default Stream<IRecord> sqlqueryS(final String sqlpattern) {
		return this.sqlqueryS(sqlpattern, null);
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。 <br>
	 *                   #标记的参数会被添加引号: select * from user where name=#name <br>
	 *                   ##标记的参数不会添加引号: select * from user limit ##cnt <br>
	 * @param params     sharp变量的占位符参数,语句参数,键,值间隔序列
	 * @return DFrame
	 */
	default DFrame sqldframe(final String sqlpattern, final Object... params) {
		return this.sqldframe(sqlpattern, IRecord.REC(params));
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。 <br>
	 *                   #标记的参数会被添加引号: select * from user where name=#name <br>
	 *                   ##标记的参数不会添加引号: select * from user limit ##cnt <br>
	 * @param params     sharp变量的占位符参数
	 * @return DFrame
	 */
	default DFrame sqldframe(final String sqlpattern, final IRecord params) {
		final var proxy = this.getProxy();
		final var spp = proxy.findOne(ISqlPatternPreprocessor.class);
		final var jdbc = proxy.findOne(Jdbc.class);
		final var sql = spp.handle(null, params, sqlpattern, jdbc);

		return jdbc.sql2recordS(sql).collect(DFrame.dfmclc);
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。 <br>
	 *                   #标记的参数会被添加引号: select * from user where name=#name <br>
	 *                   ##标记的参数不会添加引号: select * from user limit ##cnt <br>
	 * @return DFrame
	 */
	default DFrame sqldframe(final String sqlpattern) {
		return this.sqldframe(sqlpattern, (IRecord) null);
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。
	 * @param params     sharp变量的占位符参数
	 * @return Optional&lt;IRecord&gt;
	 */
	default Optional<IRecord> sql2maybe(final String sqlpattern, final IRecord params) {
		return this.sqlqueryS(sqlpattern, params).findAny();
	}

	/**
	 * 调用SqlPatternPreprocessor sqlpattern SqlPatternPreprocessor &amp;
	 * sqlpattern的说明 SqlPatternPreprocessor 会自动对sqlpattern中的命名参数进行替换： <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * @param sqlpattern 一个#开头的SQL语句模板语句变量。或是含有#变量的sql语句模板。
	 * @param params     sharp变量的占位符参数
	 * @return Optional&lt;IRecord&gt;
	 */
	default Optional<IRecord> sql2maybe(final String sqlpattern) {
		return this.sql2maybe(sqlpattern, null);
	}

	/**
	 * 依据主键查询
	 * 
	 * @param table 表名
	 * @param id    主键值
	 * @return 主键对应的表字段
	 */
	default Optional<IRecord> getById(final String table, final Object id) {
		return this.sqlqueryS(String.format("select * from %s where id=%s", table, id)).findFirst();
	}

	/**
	 * 获取jdbc 对象
	 * 
	 * @param jdbc jdbc 对象 由代理进行传入
	 * @return jdbc 对象
	 */
	default Jdbc getJdbc(final Jdbc jdbc) {
		return jdbc;
	}

	/**
	 * 获取jdbc对象
	 * 
	 * @return jdbc
	 */
	default Jdbc jdbc() {
		return this.getJdbc(null);
	}

	/**
	 * 指定session 执行DataManipulation．:Session 是Monad对象。因此可以进行函数式的状态编程<br>
	 * <p>
	 * 发起创建一个IJdbcSession对象，并通过IJdbcSession急性数据库操作<br>
	 * 事务处理,每一个事务，系统会动态的创建出一个 session 对象（IJdbcSession），这个Session
	 * 对象拥有一个UUID类型的对象标识。<br>
	 * 在一次事务性的会话IJdbcSession中：共享一个数据库连接，并且出现操作失败（sql操作)，将给予先前的动作回滚．<br>
	 * 事务只能对DML语句进行操作，对数据定义类语句DDL无法操作，例如建表、建立索引、建立分区等。<br>
	 * 一般采用如下方式调用此函数：<br>
	 * jdbc.withTransaction(sess->{session.sql2records("show databases");});<br>
	 *
	 * @param action 接收DataManipulation对象的处理函数，<br>
	 *               数据操作的具体过程 dm 的数据如果需要会馆请使用dm所提供的session 来操作数据,通常采用lamba表达式来给予
	 *               创建操作过程：sess->{写入你的操作代码}. 需要注意对于withTransaction创建的会话IJdbcSession
	 *               是以monad 容器。其初始数据为Object类型 值为null.
	 * @return {ret:返回值boolean值, exception:异常类型, throwable:异常类型,用于动态代理的默认函数,
	 *         result:sess的结果属性},参见Jdbc.newInstance
	 */
	default IRecord withTransaction(final ExceptionalConsumer<IJdbcSession<UUID, Object>> action) {
		final var attrs = new HashMap<Object, Object>();
		final var spp = this.getProxy().findOne(ISqlPatternPreprocessor.class);
		final var jdbc = this.jdbc();

		attrs.put(ISqlPatternPreprocessor.class, spp); // 增加值类型
		attrs.put(Collector.class, DFrame.dfmclc); // 增加值类型

		return jdbc.withTransaction(sess -> action.accept(sess), attrs);
	}

	/**
	 * 返回IMySQL的数据数据源
	 * 
	 * @return DataSource
	 */
	default DataSource ds() {
		final var jdbc = this.jdbc(); // 提取jdbc对象

		/**
		 * 构造匿名数据源
		 */
		return new DataSource() {
			@Override
			public <T> T unwrap(final Class<T> iface) throws SQLException {
				return null;
			}

			@Override
			public boolean isWrapperFor(final Class<?> iface) throws SQLException {
				return false;
			}

			@Override
			public Connection getConnection() throws SQLException {
				return jdbc.getConnection();
			}

			@Override
			public Connection getConnection(final String username, final String password) throws SQLException {
				return jdbc.getConnection();
			}

			@Override
			public PrintWriter getLogWriter() throws SQLException {
				return null;
			}

			@Override
			public void setLogWriter(final PrintWriter out) throws SQLException {
				// do nothing
			}

			@Override
			public void setLoginTimeout(final int seconds) throws SQLException {
				// do nothing
			}

			@Override
			public int getLoginTimeout() throws SQLException {
				return 0;
			}

			@Override
			public Logger getParentLogger() throws SQLFeatureNotSupportedException {
				return null;
			}
		};
	}
}
