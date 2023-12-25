package gbench.util.jdbc;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

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
	 * 创建一个数据库
	 * 
	 * @param dbName 数据库名称
	 */
	@JdbcExecute("create database {0} default character set utf8mb4")
	void createDatabase(String dbName);

	/**
	 * 获取数据库名称
	 */
	@JdbcQuery("select database() db")
	String getDbName();

	/**
	 * 可以返回一些简单的类型：非空则认为true
	 * 
	 * @param tableName
	 * @return 表格是否存在。
	 */
	@JdbcQuery("show tables like ''{0}''")
	boolean exists(String tableName);

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
	 * @param tableName
	 * @return
	 */
	@JdbcQuery("select * from {0}")
	List<IRecord> getAll(String tableName);

	/**
	 * 查询用户表记录
	 * 
	 * @param tableName
	 * @return
	 */
	@JdbcQuery("select * from {0} limit {1}")
	List<IRecord> getAll(String tableName, int maxSize);

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
	 * @return IRecord 流
	 */
	default Stream<IRecord> sqlqueryS(final String sqlpattern) {
		return this.sqlqueryS(sqlpattern, null);
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
	 * @param dm DataManipulation 代表，数据操作的具体过程 dm 的数据如果需要会馆请使用dm所提供的session
	 *           来操作数据,通常采用lamba表达式来给予 创建操作过程：sess->{写入你的操作代码}.
	 *           需要注意对于withTransaction创建的会话IJdbcSession 是以monad 容器。其初始数据为Object类型
	 *           值为null.
	 * @return {ret:返回值boolean值, exception:异常类型, throwable:异常类型,用于动态代理的默认函数,
	 *         result:sess的结果属性},参见Jdbc.newInstance
	 */
	default IRecord withTransaction(final ExceptionalConsumer<IJdbcSession<?, ?>> cs) {
		final var attrs = new HashMap<Object, Object>();
		final var spp = this.getProxy().findOne(ISqlPatternPreprocessor.class);
		final var jdbc = this.jdbc();
		attrs.put(ISqlPatternPreprocessor.class, spp); // 增加值类型
		attrs.put(Collector.class, DFrame.dfmclc); // 增加值类型
		return jdbc.withTransaction(sess -> cs.accept(sess), attrs);
	}

	/**
	 * 返回一个获取代理对象 Jdbc 的代理对象：可以通过 IRecord.findOne(Jdbc.class) 获取jdbc对象。
	 * 
	 * @return Proxy
	 */
	IRecord getProxy();
}
