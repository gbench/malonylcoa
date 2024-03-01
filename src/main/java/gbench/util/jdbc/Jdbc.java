package gbench.util.jdbc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import gbench.util.jdbc.annotation.JdbcConfig;
import gbench.util.jdbc.annotation.JdbcExecute;
import gbench.util.jdbc.annotation.JdbcPreparedExecute;
import gbench.util.jdbc.annotation.JdbcPreparedQuery;
import gbench.util.jdbc.annotation.JdbcQuery;
import gbench.util.jdbc.function.SQLExceptionalFunction;
import gbench.util.jdbc.function.ThrowableFunction;
import gbench.util.type.Times;
import gbench.util.jdbc.kvp.Column;
import gbench.util.jdbc.kvp.IColumn;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Json;
import gbench.util.jdbc.kvp.KVPair;
import gbench.util.jdbc.kvp.LinkedRecord;
import gbench.util.jdbc.kvp.SimpleRecord;
import gbench.util.jdbc.sql.Term;

/**
 * JDBC访问数据库 <br>
 * 需要注意 当使用流的 时候 对于 ShortCircut 类型的流 在使用完后，请注意：<br>
 * 调用 stream.close() 方法来给予关闭 清空数据库连接 ，<br>
 * 或是 执行关闭 jdbc.clear() 方法来给与清空 <br>
 * 
 * @author gbench
 *
 */
public class Jdbc implements IManagedStreams {

	/**
	 * 创建jdbc连接对象
	 * 
	 * @param supplierConn 链接贩卖商，也就是不适用传统的DriverManager,而是第三方来提供。
	 */
	public Jdbc(final Supplier<Connection> supplierConn) {

		this.supplierConn = supplierConn;// 初始化连接贩卖商
	}

	/**
	 * 创建jdbc连接对象
	 * 
	 * @param ds 数据源
	 */
	public Jdbc(final DataSource ds) {

		this.supplierConn = () -> {
			Connection conn = null;
			try {
				conn = ds.getConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return conn;
		};// 初始化连接贩卖商
	}

	/**
	 * 创建jdbc连接对象
	 * 
	 * @param driver   驱动程序
	 * @param url      连接url
	 * @param user     数据库用户
	 * @param password 数据库密码
	 */
	public Jdbc(final String driver, final String url, final String user, final String password) {

		this.init(driver, url, user, password);
	}

	/**
	 * 创建jdbc连接对象
	 * 
	 * @param rec: 需要包含key:driver,url, user, password
	 */
	public Jdbc(final IRecord rec) {

		final var props = rec.toProps();
		this.init(props.getProperty("driver"), props.getProperty("url"), props.getProperty("user"),
				props.getProperty("password"));
	}

	/**
	 * 创建jdbc连接对象
	 * 
	 * @param map: 需要包含key:driver,url, user, password
	 */
	public Jdbc(final Map<?, ?> map) {

		final var props = IRecord.REC(map).toProps();
		this.init(props.getProperty("driver"), props.getProperty("url"), props.getProperty("user"),
				props.getProperty("password"));
	}

	/**
	 * 创建jdbc连接对象
	 * 
	 * @param props: 需要包含key:driver,url, user, password
	 */
	public Jdbc(final Properties props) {

		this.init(props.getProperty("driver"), props.getProperty("url"), props.getProperty("user"),
				props.getProperty("password"));
	}

	/**
	 * 带有连接的数据查询
	 * 
	 * @param conn  数据库连接
	 * @param query 查询语句
	 * @return 查询结果
	 * @throws SQLException
	 */
	public static List<IRecord> queryWithConnection(final Connection conn, final String query) throws SQLException {

		final var pstmt = conn.prepareStatement(query);// 生成SQL语句
		return preparedQuery(() -> pstmt, null);
	}

	/**
	 * 带有连接的数据查询
	 * 
	 * @param conn    数据库连接
	 * @param query   查询语句
	 * @param prepare PreparedStatement 的查询前的数据处理。比如设置 PreparedStatement 的各个参数。
	 * @return 查询结果
	 * @throws SQLException
	 */
	public static List<IRecord> queryWithConnection(final Connection conn, final String query,
			final Consumer<PreparedStatement> prepare) throws SQLException {

		final var pstmt = conn.prepareStatement(query);// 生成SQL语句
		return preparedQuery(() -> pstmt, prepare);
	}

	/**
	 * preparedStatement 结果处理。
	 * 
	 * @param pstmt parepared statement 生成器
	 * @return 查询的结果集合
	 * @throws SQLException
	 */
	public static List<IRecord> preparedQuery(final Supplier<PreparedStatement> pstmt) throws SQLException {

		return preparedQuery(pstmt, (Consumer<PreparedStatement>) null);
	}

	/**
	 * preparedStatement 结果处理。
	 * 
	 * @param pstmt   parepared statement 生成器
	 * @param prepare parepared statement的参数准备
	 * @return 查询的结果集合
	 * @throws SQLException
	 */
	public static List<IRecord> preparedQuery(final Supplier<PreparedStatement> pstmt,
			final Consumer<PreparedStatement> prepare) throws SQLException {

		final List<IRecord> ll = new LinkedList<>();
		try (final var stmt = pstmt.get()) {
			if (prepare != null)
				prepare.accept(stmt);
			try (final var rs = stmt.executeQuery()) {
				while (rs.next())
					ll.add(readline(rs));
			} // try rs
		} // try stmt

		return ll;
	}

	/**
	 * 执行prepared statment
	 * 
	 * @param conn 数据库连接
	 * @throws SQLException
	 */
	public static void executeWithConnection(final Connection conn, final String sql) throws SQLException {

		executeWithConnection(conn, sql, (Consumer<PreparedStatement>) null);
	}

	/**
	 * 执行prepared statment
	 * 
	 * @param conn 数据库连接
	 * @param sql  prepare sql语句的处理
	 * @throws SQLException
	 */
	public static void executeWithConnection(final Connection conn, final String sql,
			final Consumer<PreparedStatement> prepare) throws SQLException {

		final var pstmt = conn.prepareStatement(sql);
		preparedExecute(() -> pstmt, prepare);
	}

	/**
	 * 执行prepared statment
	 * 
	 * @param pstmt parepared statement 生成器
	 * @throws SQLException
	 */
	public static void preparedExecute(final Supplier<PreparedStatement> pstmt) throws SQLException {

		preparedExecute(pstmt, (Consumer<PreparedStatement>) null, (BiFunction<Boolean, PreparedStatement, ?>) null);
	}

	/**
	 * 执行prepared statment
	 * 
	 * @param pstmt   parepared statement 生成器
	 * @param prepare parepared statement的参数准备
	 * @throws SQLException
	 */
	public static void preparedExecute(final Supplier<PreparedStatement> pstmt,
			final Consumer<PreparedStatement> prepare) throws SQLException {

		preparedExecute(pstmt, prepare, (BiFunction<Boolean, PreparedStatement, ?>) null);
	}

	/**
	 * 执行prepared statment
	 * 
	 * @param pstmt    parepared statement 生成器
	 * @param callback 返回结果的处理
	 * @return callback 处理的结果
	 * @throws SQLException
	 */
	public static <T> T preparedExecute(final Supplier<PreparedStatement> pstmt,
			final BiFunction<Boolean, PreparedStatement, T> callback) throws SQLException {

		return preparedExecute(pstmt, (Consumer<PreparedStatement>) null, callback);
	}

	/**
	 * 执行prepared statment
	 * 
	 * @param pstmt    parepared statement 生成器
	 * @param prepare  parepared statement的参数准备
	 * @param callback 返回结果的处理
	 * @return callback 处理的结果
	 * @throws SQLException
	 */
	public static <T> T preparedExecute(final Supplier<PreparedStatement> pstmt,
			final Consumer<PreparedStatement> prepare, final BiFunction<Boolean, PreparedStatement, T> callback)
			throws SQLException {

		T t = null;// 返回结果
		try (final var stmt = pstmt.get()) {
			if (prepare != null) {
				prepare.accept(stmt);
			}

			if (callback != null) {
				t = callback.apply(stmt.execute(), stmt);
			}
		} // try stmt

		return t;// 返回结果
	}

	/**
	 * 提取列名序列定义 IRecord.Builder
	 * 
	 * @param rs 结果集合
	 * @return 以 列名列表为 键名的 IRecorder.Builder
	 */
	public static IRecord.Builder linebuilder(final ResultSet rs) {

		return IRecord.rb(Jdbc.colnames(rs));
	}

	/**
	 * 提取列名序列定义 IRecord.Builder
	 * 
	 * @param rs      结果集合
	 * @param indices 列号索引序列,从1开始
	 * @return 以 列名列表为 键名的 IRecorder.Builder
	 */
	public static IRecord.Builder linebuilder(final ResultSet rs, final int[] indices) {

		return IRecord.rb(Jdbc.colnames(rs, indices));
	}

	/**
	 * 提取列名序列数组
	 * 
	 * @param rs 结果集合
	 * @return 当前行的对象数组,出现异常则返回null
	 */
	public static String[] colnames(final ResultSet rs) {

		ResultSetMetaData rsm;
		String[] ret = null;
		try {
			rsm = rs.getMetaData();
			final var n = rsm.getColumnCount();
			int[] indices = new int[n];
			for (int i = 1; i <= n; i++) {
				indices[i - 1] = i;
			} // for
			ret = Jdbc.colnames(rs, indices);
		} catch (SQLException e) {
			e.printStackTrace();
		} // try

		return ret;
	}

	/**
	 * 提取列名序列数组
	 * 
	 * @param rs      结果集合
	 * @param indices 列号索引序列,从1开始
	 * @return 当前行的对象数组,出现异常则返回null
	 */
	public static String[] colnames(final ResultSet rs, final int[] indices) {

		final String[] ret = new String[indices.length];// 默认的非法结果
		try {
			final var rsm = rs.getMetaData();
			for (int i = 0; i < indices.length; i++) {
				final var idx = indices[i];
				final var value = rsm.getColumnName(idx); // 提取键值
				ret[i] = value;
			} // for
		} catch (Exception e) {
			e.printStackTrace();
		} // for

		return ret;
	}

	/**
	 * 从ResultSet 当前游标位置 中读取一条数据。<br>
	 * 不对结果集合ResultSet做任何改变,游标位置需要事先设置好<br>
	 * 即不会移动ResultSet的cursor。所以第一条数据的时候需要在外部进行rs.next()<br>
	 * 
	 * @param rs 结果集合
	 * @return 当前行的对象数组,出现异常则返回null
	 */
	public static Object[] readlineA(final ResultSet rs) {

		ResultSetMetaData rsm;
		Object[] ret = null;
		try {
			rsm = rs.getMetaData();
			final var n = rsm.getColumnCount();
			final int[] indices = new int[n];
			for (int i = 0; i < n; i++) {
				indices[i] = i + 1;
			} // for
			ret = Jdbc.readlineA(rs, indices);
		} catch (SQLException e) {
			e.printStackTrace();
		} // try

		return ret;
	}

	/**
	 * 从ResultSet 当前游标位置 中读取一条数据。<br>
	 * 不对结果集合ResultSet做任何改变,游标位置需要事先设置好<br>
	 * 即不会移动ResultSet的cursor。所以第一条数据的时候需要在外部进行rs.next()<br>
	 * 
	 * @param rs      结果集合
	 * @param indices 列号索引序列,从1开始
	 * @return 当前行的对象数组
	 */
	public static Object[] readlineA(final ResultSet rs, final int[] indices) {

		final Object[] ret = new Object[indices.length];// 默认的非法结果
		try {
			for (int i = 0; i < indices.length; i++) {
				final var idx = indices[i];
				final var value = rs.getObject(idx); // 提取键值
				ret[i] = value;
			} // for
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return ret;
	}

	/**
	 * 从ResultSet 当前游标位置 中读取一条数据。<br>
	 * 不对结果集合ResultSet做任何改变,游标位置需要事先设置好<br>
	 * 即不会移动ResultSet的cursor。所以读取第一条数据的时候需要在外部进行rs.next()<br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 
	 * @param rs 结果集合
	 * @return 结果集数据的record的表示,出现异常则返回null
	 */
	public static IRecord readline(final ResultSet rs) {

		final var rec = IRecord.REC();
		try {
			return Jdbc.readline(rs, Jdbc.labels2(rs));
		} catch (Exception e) {
			return rec;
		}
	}

	/**
	 * 从ResultSet 当前游标位置 中读取一条数据。<br>
	 * 不对结果集合ResultSet做任何改变,游标位置需要事先设置好<br>
	 * 即不会移动ResultSet的cursor。所以第一条数据的时候需要在外部进行rs.next()<br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 
	 * @param rs   结果集合
	 * @param lbls 键名集合
	 * @return 结果集数据的record的表示,出现异常则返回null
	 */
	public static IRecord readline(final ResultSet rs, final String[] lbls) {

		IRecord rec = null;// 默认的非法结果
		try {
			rec = IRecord.REC();
			final var n = lbls.length;
			for (int i = 0; i < n; i++) {// 读取当前行的各个字段信息。
				final var name = lbls[i]; // 提取键名
				final var value = rs.getObject(i + 1); // 提取键值
				rec.add(name, value);
			} // for
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return rec;
	}

	/**
	 * 对 lbls 带有内容调整的行数据读取 <br>
	 * 从ResultSet 中读取一条数据。<br>
	 * 不对结果集合ResultSet做任何改变,所以需要自动调整rs的游标位置<br>
	 * 即不会移动ResultSet的cursor。所以第一条数据的时候需要在外部进行rs.next()<br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 
	 * @param rs   结果集合
	 * @param lbls 键名集合
	 * @return 结果集数据的record的表示,出现异常则返回null
	 */
	public static IRecord readline2(final ResultSet rs, final String[] lbls) {

		try {
			final var n = rs.getMetaData().getColumnCount();
			return Jdbc.readline(rs, Jdbc.adjustLabels(n, lbls));
		} catch (Exception e) {
			return IRecord.REC();
		}
	}

	/**
	 * 从当前游标开始（包含)依次向后读取数据<br>
	 * 若当前游标位于第一行以前则先移动游标到第一行而后读取 <br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 
	 * @param rs    结果集合
	 * @param close 读完是否关闭rs,true 表示读完后关闭rs
	 * @return 结果集数据的records 集合
	 * @throws SQLException
	 */
	public static List<IRecord> readlines(final ResultSet rs, final boolean close) throws SQLException {

		return Jdbc.readlineS(rs, close).collect(Collectors.toList());
	}

	/**
	 * 从当前游标（不包含)开始依次向后读取数据<br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 读取完成之后 不会关闭结果集, 是 Jdbc.readlineS(rs, false) 的 简写 <br>
	 * 
	 * @param rs 结果集合
	 * @return 结果集数据的record的流
	 * @throws SQLException
	 */
	public static Stream<IRecord> readlineS(final ResultSet rs) throws SQLException {

		return Jdbc.readlineS(rs, false);
	}

	/**
	 * 从当前游标开始（不包含)依次向后读取数据<br>
	 * <br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 
	 * @param rs    结果集合
	 * @param close 读完是否关闭rs,true 表示读完后关闭rs,<br>
	 *              注意关闭事件的触发是根据 是否读到rs结果集合的末尾,<br>
	 *              即仅当 当读取数据末端 并且 明确指了close标记的时候才给予关闭结果集合，其他情形则不给予关闭
	 * @return 结果集数据的record的流
	 * @throws SQLException
	 */
	public static Stream<IRecord> readlineS(final ResultSet rs, final boolean close) throws SQLException {

		return Jdbc.readlineS(rs, () -> {
			if (close) { // 关闭操作
				Jdbcs.trycatch3(ResultSet::close).accept(rs);
			}
		});

	}

	/**
	 * 从当前游标开始 (不包含)依次向后读取数据 <br>
	 * <br>
	 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
	 * 
	 * @param rs       结果集合
	 * @param callback 执行结束的回调函数，比如 关闭 数据集、语句、连接 之类的 收尾操作。
	 * @return 结果集数据的record的流 [rec]
	 * @throws SQLException
	 */
	public static Stream<IRecord> readlineS(final ResultSet rs, final Runnable callback) throws SQLException {

		final var lbls = Jdbc.labels2(rs);
		final var stopflag = new AtomicBoolean(false); // 是否达到末端
		final Stream<IRecord> stream = !rs.next() // 检查是否存在有后继
				? Stream.of() // 空列表
				: Stream.iterate( // 生成流对象
						readline(rs, lbls), // 初始值
						previous -> !stopflag.get(), // 是否到达末端
						previous -> { // next
							return Jdbcs.trycatch((ResultSet r) -> { // 读取 resultset
								if (r.next()) { // 先移动然后读取
									return readline(r, lbls); // 读取结果集
								} else { // 已经读取到了最后一条数据,返回null
									callback.run(); // 执行回调函数
									stopflag.set(true); // 设置结束标志
									return null; // 返回空值
								} // if
							}).apply(rs); // trycatch
						}); // iterate

		return stream;
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。 <br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。<br>
	 * 
	 * @param <T> 访问接口 类的类型
	 * @param itf 访问接口
	 * @return 数据库访问的代理对象
	 */
	public static <T> T newInstance(final Class<T> itf) {

		return newInstance(itf, (ISqlPatternPreprocessor) null);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param itf                     访问接口
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．<br>
	 *                                是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @return 书苦苦访问的代理对象
	 */
	public static <T> T newInstance(final Class<T> itf, final ISqlPatternPreprocessor sqlpattern_preprocessor) {

		return newInstance(itf, (Map<String, String>) null, sqlpattern_preprocessor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param itf                     访问接口
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．<br>
	 *                                是一个(m,a,p,j)-&gt;p 的形式 <br>
	 *                                method(m) 方法对象, <br>
	 *                                params(a args) 参数列表:name-&gt;value, <br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param jdbc_postprocessor      事后处理
	 * @return 书苦苦访问的代理对象
	 */
	public static <T> T newInstance(final Class<T> itf, final ISqlPatternPreprocessor sqlpattern_preprocessor,
			final IJdbcPostProcessor<?> jdbc_postprocessor) {

		return newInstance(itf, (Map<?, ?>) null, sqlpattern_preprocessor, jdbc_postprocessor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 * 需要有一个Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>        访问接口 类的类型
	 * @param itf        访问接口
	 * @param jdbcConfig jdbc的配置：driver,url,user,password<br>
	 *                   也可以直接加入一个 jdbcClass键
	 *                   jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                   需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                   jdbcClass键还可以直接是一个类对象：<br>
	 *                   jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 *                   需要有一个Map&lt;?,?&gt;的构造函数 <br>
	 *                   或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 *                   MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，<br>
	 *                   选取第一个。<br>
	 * @return 数据库访问的代理对象
	 */
	public static <T> T newInstance(final Class<T> itf, final IRecord jdbcConfig) {

		return newInstance(itf, jdbcConfig, (ISqlPatternPreprocessor) null);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param itf                     访问接口
	 * @param jdbcConfig              jdbc的配置：driver,url,user,password<br>
	 *                                也可以直接加入一个 jdbcClass键
	 *                                jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                jdbcClass键还可以直接是一个类对象：
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 *                                MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 *
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 *                                <br>
	 *                                是一个(m,a,p,j)-&gt;p 的形式 <br>
	 *                                method(m) 方法对象, <br>
	 *                                params(a args) 参数列表:name-&gt;value, <br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力, <br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @return 书苦苦访问的代理对象
	 */
	public static <T> T newInstance(final Class<T> itf, final IRecord jdbcConfig,
			final ISqlPatternPreprocessor sqlpattern_preprocessor) {

		return newInstance(itf, jdbcConfig.toMap(), sqlpattern_preprocessor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>        访问接口 类的类型
	 * @param itf        访问接口
	 * @param jdbcConfig jdbc的配置：driver,url,user,password<br>
	 *                   也可以直接加入一个 jdbcClass键
	 *                   jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                   需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                   jdbcClass键还可以直接是一个类对象：
	 *                   jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass 需要有一个
	 *                   Map&lt;?,?&gt;的构造函数 <br>
	 *                   或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                   MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T> T newInstance(final Class<T> itf, final Map<?, ?> jdbcConfig) {

		return newInstance(itf, jdbcConfig, (ISqlPatternPreprocessor) null);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.add("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param itf                     访问接口
	 * @param jdbcConfig              jdbc的配置：driver,url,user,password<br>
	 *                                也可以直接加入一个 jdbcClass键
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                jdbcClass键还可以直接是一个类对象：
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                                MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 *                                是一个(m,a,p,j)-&gt;p 的形式 <br>
	 *                                method(m) 方法对象, <br>
	 *                                params(a args) 参数列表:name-&gt;value, <br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力, <br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T> T newInstance(final Class<T> itf, final Map<?, ?> jdbcConfig,
			final ISqlPatternPreprocessor sqlpattern_preprocessor) {

		return newInstance(itf, jdbcConfig, sqlpattern_preprocessor, (ISqlInterceptor<List<IRecord>>) null);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&gt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>         访问接口 类的类型
	 * @param itf         访问接口
	 * @param interceptor SqlInterceptor&lt;List&lt;IRecord&gt;&gt;:方法执行的接获函数,返回一个
	 *                    List&lt;IRecord&gt;,如果非空，表示完成接获<br>
	 *                    代理对象的执行结果就是该interceptor所接获的结果，反之就是 就会继续执行。后续的操作。<br>
	 *                    是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                    method(m) 方法对象,<br>
	 *                    params(a args) 参数列表:name-&gt;value,<br>
	 *                    sqlpattern(p pattern) sql模板 ，sql
	 *                    模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                    jdbc(j) 当前连接的数据库对象．<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T> T newInstance(final Class<T> itf, final ISqlInterceptor<List<IRecord>> interceptor) {

		return newInstance(itf, (Map<String, String>) null, (ISqlPatternPreprocessor) null,
				(ISqlInterceptor<List<IRecord>>) interceptor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.add("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.add("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>         访问接口 类的类型
	 * @param itf         访问接口
	 * @param jdbcConfig  jdbc的配置：driver,url,user,password<br>
	 *                    也可以直接加入一个 jdbcClass键
	 *                    jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                    需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                    jdbcClass键还可以直接是一个类对象：
	 *                    jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass 需要有一个
	 *                    Map&lt;?,?&gt;的构造函数 <br>
	 *                    或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                    MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 *
	 * @param interceptor SqlInterceptor&lt;List&lt;IRecord&gt;&gt;:方法执行的接获函数,返回一个
	 *                    List&lt;IRecord&gt;,如果非空，表示完成接获
	 *                    代理对象的执行结果就是该interceptor所接获的结果，反之就是 就会继续执行。后续的操作。<br>
	 *                    是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                    method(m) 方法对象,<br>
	 *                    params(a args) 参数列表:name-&gt;value,<br>
	 *                    sqlpattern(p pattern) sql模板 ，sql
	 *                    模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                    jdbc(j) 当前连接的数据库对象．<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T> T newInstance(final Class<T> itf, final IRecord jdbcConfig,
			final ISqlInterceptor<List<IRecord>> interceptor) {

		final Map<?, ?> cfg = jdbcConfig == null ? null : jdbcConfig.toMap();
		return newInstance(itf, cfg, (ISqlPatternPreprocessor) null, (ISqlInterceptor<List<IRecord>>) interceptor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。<br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数<br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>         访问接口 类的类型
	 * @param itf         访问接口
	 * @param jdbcConfig  jdbc的配置：driver,url,user,password<br>
	 *                    也可以直接加入一个 jdbcClass键
	 *                    jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                    需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                    jdbcClass键还可以直接是一个类对象：
	 *                    jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass 需要有一个
	 *                    Map&lt;?,?&gt;的构造函数 <br>
	 *                    或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                    MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 *
	 * @param interceptor SqlInterceptor&lt;List&lt;IRecord&gt;&gt;:方法执行的接获函数,返回一个
	 *                    List&lt;IRecord&gt;,如果非空，表示完成接获<br>
	 *                    代理对象的执行结果就是该interceptor所接获的结果，反之就是 就会继续执行。后续的操作。<br>
	 *                    是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                    method(m) 方法对象,<br>
	 *                    params(a args) 参数列表:name-&gt;value,<br>
	 *                    sqlpattern(p pattern) sql模板 ，sql
	 *                    模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                    jdbc(j) 当前连接的数据库对象．<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T> T newInstance(final Class<T> itf, final Map<?, ?> jdbcConfig,
			final ISqlInterceptor<List<IRecord>> interceptor) {

		return newInstance(itf, jdbcConfig, (ISqlPatternPreprocessor) null,
				(ISqlInterceptor<List<IRecord>>) interceptor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password<br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数 jdbcClass键还可以直接是一个类对象：
	 * jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass 需要有一个 Map&lt;?,?&gt;的构造函数
	 * <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param itf                     访问接口
	 * @param jdbcConfig              jdbc的配置：driver,url,user,password<br>
	 *                                也可以直接加入一个 jdbcClass键
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                jdbcClass键还可以直接是一个类对象：
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                                MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 *
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 *                                是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param jdbc_postprocessor      结果的后置处理处理器：对一般标准的处理结果进行后续处理。
	 * @return 数据库访问的代理接口
	 */
	public static <T, U> T newInstance(final Class<T> itf, final Map<?, ?> jdbcConfig,
			final ISqlPatternPreprocessor sqlpattern_preprocessor, final IJdbcPostProcessor<U> jdbc_postprocessor) {

		return newInstance(itf, jdbcConfig, sqlpattern_preprocessor, (ISqlInterceptor<List<IRecord>>) null,
				jdbc_postprocessor);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。 <br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password <br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数 jdbcClass键还可以直接是一个类对象：
	 * jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass 需要有一个 Map&lt;?,?&gt;的构造函数
	 * <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param <U>                     JdbcPostProcessor 的返回结果类型：这是一个类型占位符
	 * @param itf                     访问接口 <br>
	 * @param jdbcConfig              jdbc的配置：driver,url,user,password<br>
	 *                                也可以直接加入一个 jdbcClass键
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                jdbcClass键还可以直接是一个类对象：
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                                MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．<br>
	 *                                是一个(m,a,p,j)->p 的形式 <br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．
	 * 
	 * @param sqlinterceptor          SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                                sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                                也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．
	 * @return 数据库访问的代理接口
	 */
	public static <T, U> T newInstance(final Class<T> itf, final Map<?, ?> jdbcConfig,
			final ISqlPatternPreprocessor sqlpattern_preprocessor,
			final ISqlInterceptor<List<IRecord>> sqlinterceptor) {

		return newInstance(itf, jdbcConfig, sqlpattern_preprocessor, sqlinterceptor, (IJdbcPostProcessor<U>) null);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password<br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数 <br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param <U>                     jdbcPostProcessor的返回结果的类型
	 * @param itf                     访问接口
	 * @param jdbcConfig              jdbc的配置：driver,url,user,password<br>
	 *                                也可以直接加入一个 jdbcClass键：
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                jdbcClass键还可以直接是一个类对象：
	 *                                jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 *                                需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 *                                或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 *                                MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 *                                是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * 
	 * @param sqlinterceptor          SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                                sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                                也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param jdbcPostProcessor       结果的后置处理处理器：对一般标准的处理结果进行后续处理。<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T, U> T newInstance(final Class<T> itf, final Map<?, ?> jdbcConfig,
			final ISqlPatternPreprocessor sqlpattern_preprocessor, final ISqlInterceptor<List<IRecord>> sqlinterceptor,
			final IJdbcPostProcessor<U> jdbcPostProcessor) {

		final var jc = itf.getAnnotation(JdbcConfig.class);// 获取jdbc的配置
		final var jcfg = IRecord.REC(null == jdbcConfig ? new HashMap<String, String>() : jdbcConfig);// 提供默认数据库配置
		T objT = null;// 接口实例对象
		try {
			Jdbc _jdbc = jcfg.findOne(Jdbc.class);// 尝试直接在配置类中获取Jdbc对象,如果存在jdbc对象，则直接使用。
			if (_jdbc == null && jdbcConfig != null) {
				try {
					final var jdbcClass = jdbcConfig.get("jdbcClass");// 提取jdbc Class
					if (jdbcClass != null) {
						@SuppressWarnings("unchecked")
						final Class<Jdbc> jdbcClazz = jdbcClass instanceof Class<?> //
								? (Class<Jdbc>) jdbcClass // 直接转换成Jdbc 类对象
								: (Class<Jdbc>) Class.forName(jdbcClass.toString()); // 通过类名加载类对象。
						if (jdbcClazz != null) { // jdbcClazz 可非空判断
							final var ctor = jdbcClazz.getConstructor(Map.class);
							ctor.setAccessible(true);
							if (ctor != null) {
								_jdbc = ctor.newInstance(jdbcConfig);
							}
						} // if jdbcClazz 可非空判断
					} // if
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} // _jdbc 对象

			final var jdbc = _jdbc != null // _jdbc 的构造优先。
					? _jdbc
					: (jdbcConfig == null || jdbcConfig.size() < 1) && (jc == null) //
							? null // 没有默认配置
							: new Jdbc( // 使用Jdbc创建对象
									jcfg.computeIfAbsent("driver", k -> jc == null ? null : jc.driver()),
									jcfg.computeIfAbsent("url", k -> jc == null ? null : jc.url()),
									jcfg.computeIfAbsent("user", k -> jc == null ? null : jc.user()),
									jcfg.computeIfAbsent("password", k -> jc == null ? null : jc.password()));

			if (jdbc == null) {
				System.err.println(Jdbcs.format("尚未配置Jdbc实例,无法处理 数据库连接操作,因为jdbc的配置:\njdbc:{0},\n@JdbcConfig:{1}！",
						jdbcConfig, jc));
			} // if jdbc==null

			objT = newInstance(itf, jdbc, sqlpattern_preprocessor, sqlinterceptor, jdbcPostProcessor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return objT;// 返回接口实例
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。 <br>
	 * 请确保jdbcConfig 含有有效的driver,url,user,password<br>
	 * 也可以直接加入一个 jdbcClass键
	 * jdbcConfig.put("jdbcClass",MyJdbc.class.getName()),jdbcClass 需要有一个
	 * Map&lt;?,?&gt;的构造函数 <br>
	 * jdbcClass键还可以直接是一个类对象： jdbcConfig.put("jdbcClass",MyJdbc.class),jdbcClass
	 * 需要有一个 Map&lt;?,?&gt;的构造函数 <br>
	 * 或者是直接传入一个非空的Jdbc实例。jdbcConfig.put("_",new
	 * MyJdbc),键名可以任意，当存在多个Jdbc实例的时候，选取第一个。<br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param <U>                     jdbcPostProcessor的返回结果的类型
	 * @param itf                     访问接口
	 * @param ds                      数据源
	 * 
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 *                                是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * 
	 * @param sqlinterceptor          SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                                sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                                也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param jdbcPostProcessor       结果的后置处理处理器：对一般标准的处理结果进行后续处理。<br>
	 * @return 数据库访问的代理接口
	 */
	public static <T, U> T newInstance(final Class<T> itf, final DataSource ds,
			final ISqlPatternPreprocessor sqlpattern_preprocessor, final ISqlInterceptor<List<IRecord>> sqlinterceptor,
			final IJdbcPostProcessor<U> jdbcPostProcessor) {
		T objT = null;// 接口实例对象
		try {
			objT = newInstance(itf, new Jdbc(ds), sqlpattern_preprocessor, sqlinterceptor, jdbcPostProcessor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return objT;
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。 <br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。
	 * 
	 * @param <T>  访问接口 类的类型
	 * @param itf  访问接口
	 * @param jdbc 数据库访问接口
	 * @return 数据库访问的代理接口
	 */
	public static synchronized <T> T newInstance(final Class<T> itf, final Jdbc jdbc) {

		return newInstance(itf, jdbc, null, null);
	}

	/**
	 * 命名SQL文件（namedsql file）是一个 sql 脚本文件 只是 这个 sql脚本 包含有特别的 用于说明 语句结构的 文法注释。<br>
	 * 一个 namedsql file 是如下的结构： <br>
	 * 其中 语句名称用 # 空格 ‘语句名称’ 的形式。 而不带#与 名称之间不带有空格的 将被视作一般注释。给予忽略。<br>
	 * 即他会按照如下进行解释：<br>
	 * <br>
	 * 命名SQL文件的解析，类似于如下的格式。需要注意的#后面必须带有空格，才可以作为 sql语句的名称<br>
	 * 需要注意：‘# count1’ 才是sql语句的名称，而‘#name: 用户名称’ 则是参数的说明。区别在于<br>
	 * count1与# 之间有一个 空格。而name 与#之间没有 <br>
	 * 
	 * -- -------------------------- <br>
	 * -- # count1 <br>
	 * -- name: 用户名称 <br>
	 * -- --------------------------<br>
	 * select count(*) from t_user <br>
	 * where name = #name <br>
	 *
	 * -- --------------------------<br>
	 * -- # count2 <br>
	 * -- --------------------------<br>
	 * select count(*) from t_user;<br>
	 *
	 * -- --------------------------<br>
	 * -- # insertUser <br>
	 * -- -------------------------- <br>
	 * insert into ##tableName (name,password,sex,address,birth,phonenumber,email)
	 * <br>
	 * values (#name,#password,#sex,#address,#birth,#phonenumber,#email) <br>
	 * 
	 * @param lines 行文本序列
	 * @return 命名sql 集合。
	 */
	public static Map<String, String> parse2namedsqls(final Stream<String> lines) {

		return parse2namedsqls(lines, true);
	}

	/**
	 * 命名SQL文件的解析，类似于如下的格式。需要注意的#后面必须带有空格，才可以作为 sql语句的名称 <br>
	 * 需要注意：‘# count1’ 才是sql语句的名称，而‘#name: 用户名称’ 则是参数的说明。区别在于 <br>
	 * count1与# 之间有一个 空格。而name 与#之间没有 <br>
	 * 
	 * -- -------------------------- <br>
	 * -- # count1 <br>
	 * -- #name: 用户名称 <br>
	 * -- -------------------------- <br>
	 * select count(*) from t_user <br>
	 * where name = #name <br>
	 *
	 * -- -------------------------- <br>
	 * -- # count2 <br>
	 * -- -------------------------- <br>
	 * select count(*) from t_user; <br>
	 *
	 * -- -------------------------- <br>
	 * -- # insertUser <br>
	 * -- -------------------------- <br>
	 * insert into ##tableName (name,password,sex,address,birth,phonenumber,email)
	 * <br>
	 * values (#name,#password,#sex,#address,#birth,#phonenumber,#email) <br>
	 * 
	 * @param lines                  行文本序列
	 * @param remove_inline_comments 是否移除行内注释。 true:移除,否则不移除
	 * @return 命名sql 集合。
	 */
	public static Map<String, String> parse2namedsqls(final Stream<String> lines,
			final boolean remove_inline_comments) {

		if (null == lines) { // 空值返回
			return new HashMap<>();
		} // if

		final var namedsqls = new HashMap<String, String>();
		final var p = Pattern.compile("^([\\s-])*#\\s+([^\\s#]+)\\s*$");// key 标记
		final var p_comment = Pattern.compile("^([\\s]*)--.*$");// key 标记
		final var keys = new LinkedList<String>();
		final var values = new LinkedList<String>();

		// 添加key values
		final Supplier<Map<String, String>> add2namedsqls = () -> {
			if (keys.size() > 0 && values.size() > 0) {
				final var k = keys.getLast();
				namedsqls.put(k, String.join("\n", values));
				keys.clear();
				values.clear();
			} // keys 和 values 缓存框都给清空

			return namedsqls;// 命名SQL
		};

		// 逐行遍历
		lines.forEach(line -> {
			if (line.matches("^\\s*$"))
				return;// 空白行过滤
			final var key_matcher = p.matcher(line);
			if (key_matcher.matches()) {
				final var key = key_matcher.group(2);
				add2namedsqls.get();
				keys.add(key);
			} else {
				if (p_comment.matcher(line).matches()) {
					return;
				}

				final var _line = remove_inline_comments // 是否移除行内注释
						? line.replaceAll("--\\s+.*$", "") // 去除行内的尾部注释 ,由 --空格 引导的内容被省略掉。
						: line;// 不移除行内注释

				values.add(_line);// 数据追加
			} // if (key_matcher.matches())
		});// lines.forEach

		// 添加剩余的SQL语句。
		add2namedsqls.get();

		// 返回结果集合
		return namedsqls;
	}

	/**
	 * 提取参数名列表:
	 * 
	 * @param line 类似于 这样的sql 模板insert into ##tableName
	 *             (name,password,sex,address,birth,phonenumber,email) <br>
	 *             values
	 *             (#name,#password,#sex,#address,#birth,#phonenumber,#email),把 <br>
	 *             [#name,#password,#sex,#address,#birth,#phonenumber,#email],提取出来,注意提取后
	 *             井号被去除了。 <br>
	 * 
	 * @return 从模板提取出来的参数集合。
	 */
	public static LinkedHashSet<String> retrieve_params(final String line) {

		final var placeholder = Pattern.compile("#+([a-z_][a-z0-9_]+)", Pattern.CASE_INSENSITIVE).matcher(line);
		final var params = new LinkedHashSet<String>();

		// 提取所有位置参数
		while (placeholder.find()) {
			params.add(placeholder.group(1));
		}

		return params;
	}

	/**
	 * namedsql_processor 对namedsqls中的‘{’进行转义：防止Jdbcs.formater把他误认为参数。<br>
	 * 注意:<br>
	 * namedsql_processor的 preprocessor 只会对 namedsqls 中的所包含的语句进行处理,<br>
	 * 并不会对 没有在namedsqls中存贮的sql语句调用preprocessor做预处理。<br>
	 * 对于其他的传入的(比如直接传递,而不是采用方法反射,比如IJdbcSession.sql2records(sql)系列函数) <br>
	 * 参见方法:IJdbcSession.sql2records(final String sqlpattern, final IRecord params)
	 * <br>
	 * preprocessor 是不会对sql进行处理的,所以 如果需要调用 namedsql_processor 做参数
	 * 填充,需要先手动的把sql进行进行预处理 <br>
	 * 
	 * @param namedsqls    sql语句 {(name,sql)}
	 * @param preprocessor sql语句 的预处理器：例如对 neo4j的转义。
	 * @return SqlPatternPreprocessor
	 */
	public static ISqlPatternPreprocessor namedsql_processor(final Map<String, String> namedsqls,
			final Function<String, String> preprocessor) {

		if (namedsqls != null)
			namedsqls.forEach((k, v) -> {// 依次处理每个SQL
				namedsqls.compute(k, (key, value) -> preprocessor.apply(value));
			});// 依次处理每个SQL

		return namedsql_processor(namedsqls);
	}

	/**
	 * 其实就是提取在方法Method上的标记比如annotation:JdbcQuery,JdbcExecute中的value 值。 根据方法签名提取 对含有
	 * 井号#的pattern 进行侦测，如果侦测出来就用sharppattern_todetect所表标的内容，作为键值在
	 * 在sharppattern_defs进行检索，并对method上的标记
	 * 
	 * @param method            方法方法对象，当sharppattern为null时，用于作为生成默认的sharppattern 名。
	 * @param sharppattern      待检测的位于JdbcQuery中的以井号#开头的字符串 ,null 或是空白 表示 是#
	 *                          "#"+方法名称。 例如对于方法 getName,sharppattern
	 *                          为null,sharppattern则被视作#getName
	 * @param sharppattern_defs 命名sharp pattern 的定义
	 * @return 识别出来的 sharp pattern
	 */
	public static String parseJdbcMethodSharpPattern(final Method method, final String sharppattern,
			final Map<String, String> sharppattern_defs) {

		final var sqlpattern = (sharppattern == null || sharppattern.matches("\\s*")) //
				? "#" + method.getName()
				: sharppattern;// 默认的sqlpattern为方法名
		// namedsql 是一个用＃号作为前缀的名称
		final var sharp_matcher = Pattern.compile("#+([a-z_][a-z0-9_]+)", Pattern.CASE_INSENSITIVE).matcher(sqlpattern);

		if (!sharp_matcher.matches()) {
			return sqlpattern;// 非namedsql
		}

		final var namedSqlpattern = sharppattern_defs.get(sharp_matcher.group(1));// 提取sqlpattern
		if (namedSqlpattern == null) {// namedsqls 中无法对应
			System.out.println(
					Jdbcs.format("in {0} 无法对应到:{1}", method == null ? "\"方法缺失\"" : method.getName(), sqlpattern));
			return sqlpattern;//
		} // if

		return namedSqlpattern;
	}

	/**
	 * 生成一个命名sql(namedsql)的预处理器。 <br>
	 * 对与双#号的参数不予进行替换 类型转换。 <br>
	 * 
	 * 一个典型使用 namedsql进行创建数据库接机口的案例是如下情形：<br>
	 * 定义接口 <br>
	 * interface UserDatabase { <br>
	 * &#064;JdbcExecute("#createTable") <br>
	 * public void createTable(String tableName); <br>
	 * }<br>
	 * //提取命名SQL的集合。 <br>
	 * var namedsqls = parse2namedsqls(utf8lines(new
	 * File(path("user.sql",UserModel.class)))); <br>
	 * //生成接口的代理实例，并注入SqlPatternPreprocessor: 即namedsql_processor(namedsqls)的函数调用。
	 * <br>
	 * var database =
	 * Jdbc.newInstance(UserDatabase.class,jdbcConfig,namedsql_processor(namedsqls));<br>
	 * //使用代理实例进行数据库操作。<br>
	 * database.createTable()// <br>
	 * 
	 * #createTable 是 user.sql 中的一条创建表的SQL语句。 <br>
	 * 
	 * 把如下的 命名sql 通常是来自于一个SQL文件。 <br>
	 * -- -------------------------- <br>
	 * -- # insertUser <br>
	 * -- -------------------------- <br>
	 * insert into ##tableName (name,password,sex,address,birth,phonenumber,email)
	 * <br>
	 * values (#name,#password,#sex,#address,#birth,#phonenumber,#email) <br>
	 *
	 * 根据接口 <br>
	 * &#064;JdbcExecute({"#insertUser"}) <br>
	 * public void insertUser(String tableName,String name,String password,String
	 * sex,String address, <br>
	 * Date birth,String phonenumber,String email); <br>
	 *
	 * 给替换成 如下的式样 <br>
	 * insert into {0} (name,password,sex,address,birth,phonenumber,email) <br>
	 * values (''{1}'',''{2}'',''{3}'',''{4}'',''{5,date,yyyy-MM-dd
	 * HH:mm:ss}'',''{6}'',''{7}'') <br>
	 * 
	 * @param namedsqls 命名sql集合：{#key1-&gt;sql1,#key1-&gt;sql2,...},
	 * @return 变换后的sqlapttern 可以被 Jdbcs.format处理的SQL语句。
	 */
	public static ISqlPatternPreprocessor namedsql_processor(final Map<String, String> namedsqls) {

		return (final Method method, final IRecord params, final String sqlpattern, final Jdbc jdbc) -> {
			// 自动侦测SQL Pattern 或者 mehtod sharp pattern
			final var namedSqlpattern = parseJdbcMethodSharpPattern(method, sqlpattern, namedsqls);
			var line = namedSqlpattern;// 对namedSqlpattern 进行数据处理。

			if (method == null) { // 方法名为空
				return params != null // 根据参数数据的不同进行数据变换
						? Jdbc.quote_substitute(line, "#+(\\w+)", params) // 采用参数sharpPattern进行数据替换
						: line; // 不予进行方法参数回填。
			} else { // 方法名不为空
				final var ai = new AtomicInteger(0);// 计数变量
				final var pp = method.getParameters();// 方法的参数集合
				for (final String key : params.keys()) {// 遍历方法参数。
					final var i = ai.getAndIncrement();// 获取当前的参数位置
					final var rawfmt = "{" + i + "}"; // 原始类型
					final var type = pp[i].getType();// 获取参数类型
					final var numfmt = "{" + i + ",number,#}"; // 数值格式
					final var datefmt = "''{" + i + ",date,yyyy-MM-dd HH:mm:ss}''"; // 日期类型。
					final var defaultfmt = "''{" + i + "}''"; // 默认格式
					var isnumber = false; // 是否是数字格式
					var isdate = false; // 是否是日期类型
					var placeholder = defaultfmt;// 默认的占位符的式样，加上单引号

					if (Number.class.isAssignableFrom(type) || type == short.class || type == int.class
							|| type == long.class || type == float.class || type == double.class) { // 数值格式
						placeholder = numfmt;
						isnumber = true;
					} else if (Date.class.equals(type)) { // 时间格式
						isdate = true;
						placeholder = datefmt;
					} else { // 默认处理
						// placeholder = defaultfmt;// 默认格式
					}

					// 更新 sql模板,把参数名更换成位置序号
					line = line.replace("##" + key, isnumber ? numfmt : isdate ? datefmt : rawfmt) // 数值格式
							.replace("#" + key, placeholder); // 默认格式
				} // for

				// System.out.println(line);
				return line;// 返回处理后的sql 语句模板。
			}
		}; // (Method method,IRecord params,String sqlpattern,Jdbc jdbc)->
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。 创建一个接口itf的实现bean 根据注解信息来给予实现。
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param itf                     访问接口
	 * @param jdbc                    数据库访问接口
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 *                                是一个(m,a,p,j)-&gt;p 的形式<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param sqlinterceptor          SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                                sqlpattern_preprocessor 处理之后才给予调用的。
	 *                                也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @return 数据库访问的代理接口
	 */
	public static synchronized <T> T newInstance(final Class<T> itf, final Jdbc jdbc,
			final ISqlPatternPreprocessor sqlpattern_preprocessor,
			final ISqlInterceptor<List<IRecord>> sqlinterceptor) {

		return newInstance(itf, jdbc, sqlpattern_preprocessor, sqlinterceptor, (IJdbcPostProcessor<?>) null);
	}

	/**
	 * 根据JDBC的上下文构建一个JDBC的执行环境。<br>
	 * 创建一个接口itf的实现bean 根据注解信息来给予实现。<br>
	 * 
	 * @param <T>                     访问接口 类的类型
	 * @param <U>                     jdbcPostProcessor的返回结果的类型
	 * @param itf                     访问接口 <br>
	 * @param jdbc                    数据库访问接口 <br>
	 * @param sqlpattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．<br>
	 *                                是一个(m,a,p,j)->p 的形式 <br>
	 *                                method(m) 方法对象, <br>
	 *                                params(a args) 参数列表:name-&gt;value, <br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param sqlinterceptor          SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                                sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                                也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                                method(m) 方法对象,<br>
	 *                                params(a args) 参数列表:name-&gt;value,<br>
	 *                                sqlpattern(p pattern) sql模板 ，sql
	 *                                模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                                jdbc(j) 当前连接的数据库对象．<br>
	 * @param jdbc_postprocessor      结果的后置处理处理器：对一般标准的处理结果进行后续处理。<br>
	 * @return 数据库访问的代理接口
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T, U> T newInstance(final Class<T> itf, final Jdbc jdbc,
			final ISqlPatternPreprocessor sqlpattern_preprocessor, // sql 语句模板的处理
			final ISqlInterceptor<List<IRecord>> sqlinterceptor, // jdbc的sql 执行拦截
			final IJdbcPostProcessor<U> jdbc_postprocessor) { // jdbc_postprocessor 的jdbc处理结果最终递交。

		// 定义代码拦截器
		final ISqlInterceptor<List<IRecord>> interceptor = // 方法的前置拦截器
				sqlinterceptor == null ? (m, a, s, j) -> null : sqlinterceptor;// 定义拦截器
		final IJdbcPostProcessor<U> postprocessor = // jdbc 处理结果的最终 称帝的处理包装结果。
				jdbc_postprocessor == null ? (m, a, j, r) -> r : jdbc_postprocessor;// 定义后置处理器:默认不做处理e->e
		// 创建数据库操作的代理对象。
		final var t = (T) Proxy.newProxyInstance(itf.getClassLoader(), new Class<?>[] { itf },
				(proxy, method, args) -> {
					/*
					 * 对这里的程序设计做一点说明：考虑到各种语句Annotation的处理逻辑相似，这里我没有把他们分装成函数而是 统一的卸载一个函数里了
					 * 好处就是变量的共享非常方便：坏处就是 出现了多出口（postprocessor 出现的地方）。造成程序的理解混乱，但是由于注解不多，这种缺点还是
					 * 可以忍受的。所以就采用了这种混编程。
					 */

					///////////////////////////////////////////////////////////////////////
					// 数据注入
					// 注入jdbc对象等环境对象．就像SpringMVC的请求参数的注入
					///////////////////////////////////////////////////////////////////////
					final BiConsumer<Object[], Map<Class<?>, Object>> inject_default_values = (oo, mm) -> {
						if (oo != null && oo.length > 0) {
							final var tt = method.getParameterTypes();// 提取方法的参数列表
							mm.forEach((cls, v) -> {
								for (int i = 0; i < tt.length; i++)
									if ((tt[i] == cls) && oo[i] == null)
										oo[i] = v;
							}// 根据参数类型进行注入。该位置的参数为null的时候给予注入。
							);// foreach
						} // oo!=null&&oo.length>0
					};// inject
					final var default_values = new HashMap<Class<?>, Object>();// Jdbc代理提供的默认参数列表。
					default_values.put(Jdbc.class, jdbc);// Jdbc的默认值

					///////////////////////////////////////////////////////////////////////
					// 方法注释解析：JdbcQuery
					///////////////////////////////////////////////////////////////////////
					// sql查询的执行
					final JdbcQuery[] jcqs = method.getAnnotationsByType(JdbcQuery.class);
					final ISqlPatternPreprocessor pattern_preprocessor = sqlpattern_preprocessor == null
							? (m, a, p, j) -> p
							: sqlpattern_preprocessor;// sql pattern 预处理。
					if (jcqs != null && jcqs.length > 0) {// 查询优先
						final var o = handleJdbcQuery(jdbc, jcqs, method, args, pattern_preprocessor, interceptor);// 查询优先
						// 默认方法的处理
						if (isDefaultMethod(method) && args.length > 0 && args[args.length - 1] == null) {
							final Object oo[] = args.clone();
							oo[oo.length - 1] = o; // 把查询结果作为参数列表的最后一位给予传递
							inject_default_values.accept(oo, default_values);// 注入默认值
							return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
							invokeDefaultMethod(proxy, method, oo));
						} // if isDefaultMethod
						return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
						o);
					} // if jcqs!=null

					///////////////////////////////////////////////////////////////////////
					// 方法注释解析：JdbcPreparedQuery
					///////////////////////////////////////////////////////////////////////
					// sql查询的执行:第二种jcq 这也是为何有个2的原因。s结尾表示这里复数的意思。
					final JdbcPreparedQuery[] jcq2s = method.getAnnotationsByType(JdbcPreparedQuery.class);
					// 这一段代码是一种示例：用于演示每个annotaion 都可以自定义
					// SqlPatternPreprocessor,其实与采用pattern_preprocessor效果一样
					final ISqlPatternPreprocessor pattern_preprocessor2 = sqlpattern_preprocessor == null
							? (m, a, p, j) -> p
							: sqlpattern_preprocessor;// 演示使用一个专用的 pattern_preprocessor2
					if (jcq2s != null && jcq2s.length > 0) {// 查询优先
						final var o = handleJdbcPreparedQuery(jdbc, jcq2s, method, args, pattern_preprocessor2,
								interceptor);// 查询优先
						// 默认方法的处理
						if (isDefaultMethod(method) && args.length > 0 && args[args.length - 1] == null) {
							final Object oo[] = args.clone();
							oo[oo.length - 1] = o; // 把查询结果作为参数列表的最后一位给予传递
							inject_default_values.accept(oo, default_values);// 注入默认值
							return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
							invokeDefaultMethod(proxy, method, oo));
						} // if isDefaultMethod
						return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
						o);
					} // if jcqs!=null

					///////////////////////////////////////////////////////////////////////
					// 方法注释解析：JdbcExecute
					///////////////////////////////////////////////////////////////////////
					// SQL语句的执行:JdbcExecute
					final JdbcExecute[] jces = method.getAnnotationsByType(JdbcExecute.class);
					synchronized (jdbc) {// 保持执行操作同步运行．
						if (jces != null && jces.length > 0) {
							return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
							handleJdbcExecute(jdbc, jces, method, args, pattern_preprocessor, interceptor));
						} // if
					} // synchronized (jdbc)

					///////////////////////////////////////////////////////////////////////
					// 方法注释解析：PreparedExecute
					///////////////////////////////////////////////////////////////////////
					// SQL语句的执行:PreparedExecute
					final JdbcPreparedExecute[] jce2s = method.getAnnotationsByType(JdbcPreparedExecute.class);
					synchronized (jdbc) {// 保持执行操作同步运行．
						if (jce2s != null && jce2s.length > 0) {
							return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
							handlePreparedExecute(jdbc, jce2s, method, args, pattern_preprocessor, interceptor));
						} // if
					} // synchronized (jdbc)

					///////////////////////////////////////////////////////////////////////
					// 方法注释解析：默认方法 -没有注释但是默认方法
					///////////////////////////////////////////////////////////////////////
					// 如果以上的注解都没有处理成功，尝试执行函数的默认方法：默认函数的处理
					if (isDefaultMethod(method)) {
						return postprocessor.process(method, params(method, args), jdbc, (U) // 结果呈递
						Jdbc.handleDefaultMethod(proxy, method, args, jdbc));
					}

					///////////////////////////////////////////////////////////////////////
					// 方法注释解析：Jdbc所不能理解的方法
					///////////////////////////////////////////////////////////////////////
					final var params = params(method, args);
					final var message = Jdbcs.format(
							"方法：{0},参数:{1}，超出了Jdbc所能理解的范围，代理失败。但是这个方法会把代理的对象的结构信息也给返回出去，\n"
									+ "可以作为访问:sqlinterceptor，sqlpattern_preprocessor，jdbc_postprocessorjdbc等对象的一个入口。",
							method.getName(), params);

					U u = null;// 默认的返回值
					// 仅当方法的返回值类型为IRecord 系列的时候才提供全面代理结构的信息作为返回值。
					if (method.getReturnType().isAssignableFrom(IRecord.class)) {
						try {
							u = (U) (Object) IRecord.REC(// 返回结果结构
									"success", false, // 标记执行失败
									"message", message, // 提示消息内容
									"params", params, // 调用的参数
									"method", method, // 执行失败的额方法
									"sqlinterceptor", sqlinterceptor, "sqlpattern_preprocessor",
									sqlpattern_preprocessor, "jdbc_postprocessor", jdbc_postprocessor, "jdbc", jdbc // jdbc
																													// 的连接对象
							);// 返回当前的执行环境。;
						} catch (Exception e) {
							e.printStackTrace();
						} // try
					} else {// if(method.getReturnType().isAssignableFrom(IRecord.class))
						System.err.println("返回类型非:IRecord，不予提供 代理详情结构信息：\n" + message);
					} // if(method.getReturnType().isAssignableFrom(IRecord.class))

					// 以上均处理不了，折返回null 作为处理失败的结果。
					// 方法的计算结果，与 下面的 return t 是不一样的，return t 是代理对象，而这里是方法的运算结果。
					return postprocessor.process(method, params, jdbc, (U) u);

				});// newProxyInstance

		return t;// 返回T类型的代理对象。
	}

	/**
	 * 处理默认的构造方法<br>
	 * 注意invoke 与 handleDefaultMethod的区别是 handleDefaultMethod会 左接口的参数方法注入。<br>
	 * 而 invokeDefaultMethod 则不会。 所 handleDefaultMethod 需要传递Jdbc 参数<br>
	 *
	 * @param proxy  代理对象
	 * @param method 方法对象
	 * @param args   方法参数对象数组
	 * @param jdbc   jdbc对象
	 * @return 默认方法的执行结果
	 */
	public static Object handleDefaultMethod(final Object proxy, final Method method, final Object[] args,
			final Jdbc jdbc) {

		if (jdbc == null)
			return null;

		// 数据注入,注入jdbc对象等环境对象．
		final BiConsumer<Object[], Map<Class<?>, Object>> inject = (oo, mm) -> {
			if (oo != null && oo.length > 0) {
				var tt = method.getParameterTypes();
				mm.forEach((cls, v) -> {
					for (int i = 0; i < tt.length; i++) {
						if ((tt[i] == cls) && oo[i] == null) {
							oo[i] = v;
						} // if
					} // for
				}); // foreach
			} // if
		};// inject

		final var mm = new HashMap<Class<?>, Object>();
		mm.put(Jdbc.class, jdbc);
		inject.accept(args, mm);
		final boolean isTransaction = Arrays.stream(method.getParameterTypes()).anyMatch(e -> e == IJdbcSession.class);// IJdbcSession对象作为是否为事务操作的标记。
		// 封装trycatch,trycach 的编码真的很占地方：哈哈啥
		final Supplier<Object> sp = () -> {
			Object obj = null;// 返回值
			try {
				obj = invokeDefaultMethod(proxy, method, args);
			} // 调用默认处理
			catch (Throwable e) {
				e.printStackTrace();
			}
			return obj;// 返回默认值的处理结果
		};// sp 结果生成器

		// 返回结果
		return isTransaction // 是否存在事务处理
				? jdbc.withTransaction(sess -> { // 执行事务
					mm.put(IJdbcSession.class, sess);// 准备sess会话对象
					inject.accept(args, mm);// 把sess事务对象注入到参数上下文中
					// System.out.println(Arrays.asList(args));
					invokeDefaultMethod(proxy, method, args);
				}) // withTransaction
				: sp.get(); // 非事务操作
	}

	/**
	 * 调用接口默认方法：<br>
	 * 注意invoke 与 handleDefaultMethod的区别是 handleDefaultMethod会 左接口的参数方法注入。<br>
	 * 而 invokeDefaultMethod 则不会。 <br>
	 *
	 * @param proxy  代理对象
	 * @param method 方法名
	 * @param args   参数名
	 * @return 调用默认方法．
	 * @throws Throwable
	 */
	private static Object invokeDefaultMethod(final Object proxy, final Method method, final Object[] args)
			throws Throwable {

		final var declaringClass = method.getDeclaringClass();
		final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());// 确定似有方法寻找工具
		return lookup.unreflectSpecial(method, declaringClass).bindTo(proxy)// 防止对proxy的第归调用．
				.invokeWithArguments(args);
	}

	/**
	 * 判断是否是默认方法<br>
	 *
	 * @param method 方法对象
	 * @return true default method 否则 非默认函数
	 */
	private static boolean isDefaultMethod(final Method method) {

		return ((method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)
				&& method.getDeclaringClass().isInterface();
	}

	/**
	 * 装配方法参数:形参数->实参数<br>
	 *
	 * @param method 方法对象
	 * @param args   实际参数对象集合
	 * @return 参数的key-value 集合
	 */
	private static IRecord params(final Method method, final Object[] args) {

		final var pp = method.getParameters();
		final var map = new LinkedHashMap<String, Object>();// 命名参数:需要注意这里采用LinkedHashMap以保持原来参数的顺序。

		for (int i = 0; i < pp.length; i++) {
			map.put(pp[i].getName(), args[i]);
		} // for

		return new LinkedRecord(map);
	}

	/**
	 * 执行的处理：
	 * <p>
	 * 对于一个换行位含有;sql会自动尽心拆分即：<br>
	 * 对于一个 这样的sqlpattern：<br>
	 * drop table if exists t_aaa;<br>
	 * create table t_aaa(id int)<br>
	 * 会视作两条sql 语句<br>
	 *
	 * @param jdbc                 jdbc对象
	 * @param jces                 jdbc的执行语句集合
	 * @param method               调用方法
	 * @param args                 调用参数
	 * @param pattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．<br>
	 * @param sqlinterceptor       SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                             sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                             也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                             method(m) 方法对象,<br>
	 *                             params(a args) 参数列表:name-&gt;value,<br>
	 *                             sqlpattern(p pattern) sql模板 ，sql
	 *                             模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                             jdbc(j) 当前连接的数据库对象．
	 * @return 执行结果
	 */
	private static Object handleJdbcExecute(final Jdbc jdbc, final JdbcExecute[] jces, final Method method,
			final Object[] args, final ISqlPatternPreprocessor pattern_preprocessor,
			final ISqlInterceptor<List<IRecord>> sqlinterceptor) {

		// 非法的执行SQL语句则直接返回
		if (jces == null || jces.length < 1) {
			return null;
		}

		final var _patterns = jces[0].value();// sql语句模板数组
		// 没有传入sqlpattern 代表一个采用默认 pattern 需要用pattern_preprocessor// 来解析。
		final String[] patterns = (_patterns == null || _patterns.length < 1) ? new String[] { null } : _patterns;
		final var pargs = params(method, args);// 构造参数对象
		return jdbc.withTransaction(sess -> {// 开启事务管理
			for (final var pattern : patterns) { // sql语句模板 :当pattern 为null的时候，pattern_preprocessor
				// 会为其左方法签名的解释。不过需要namedsql_processor配置。
				final var dd = sqlinterceptor.intercept(method, pargs, pattern, jdbc);
				if (dd != null) {
					continue;// 非空表示拦截
				} // if
					// 提取SQL语句模板,会自动为 null的pattern 提供方法签名的解释
				final var sqlPattern = pattern_preprocessor.handle(method, pargs, pattern, jdbc);
				final var sqlLines = Jdbcs.format(sqlPattern, args);// 提取SQL语句,并添加参数
				if (sqlLines == null) {// 方法的SQL语句模板解释失败
					throw new Exception(Jdbcs.format("无法为方法{0}解析出正确的SQL语句，请确保为Jdbc对象安装了正确的pattern_preprocessor!",
							method.getName()));
				} // lines
				final String[] sqls = sqlLines.split(";\\s*\n+");// 尝试对sqls 进行多语句解析。位于行末的分号给予分解
				for (final var sql : sqls) {// 依次执行SQL语句
					if (sql.matches("\\s*")) {
						continue;
					} // if
					if (debug) {
						System.out.println("jdbc:handleJdbcQuery:" + sql);
					} // if
					sess.sql2execute2int(sql);
				} // for sql:sqls
			} // for var pattern:patterns
		});// withTransaction

	} // handleJdbcExecute

	/**
	 * 对于一个换行位含有;sql会自动尽心拆分即：<br>
	 * 对于一个 这样的sqlpattern：<br>
	 * drop table if exists t_aaa; <br>
	 * create table t_aaa(id int) <br>
	 * 会视作两条sql 语句 <br>
	 * <p>
	 * sqlpattern 和sqltpl 的区别就是<br>
	 * sqlpattern 是{0},{1},等 Jdbcs.format 格式的模板，用于匹配 参数 <br>
	 * sqltpl 是sqlpattern被解析后的结果，其中的参数是 ?的占位符，用于对PreparedStatement的处理。 <br>
	 * 执行的处理<br>
	 *
	 * @param jdbc                 jdbc对象
	 * @param jces                 jdbc的执行
	 * @param method               调用方法
	 * @param args                 调用参数
	 * @param pattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 * @param sqlinterceptor       SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                             sqlpattern_preprocessor 处理之后才给予调用的。
	 *                             也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;
	 *                             method(m) 方法对象, params(a args)
	 *                             参数列表:name-&gt;value, sqlpattern(p pattern) sql模板
	 *                             ，sql 模板通称会携带模板参数，以完成分数据库，分表的能力, jdbc(j)
	 *                             当前连接的数据库对象．
	 * @return 执行结果
	 */
	private static Object handlePreparedExecute(final Jdbc jdbc, final JdbcPreparedExecute[] jces, final Method method,
			final Object[] args, final ISqlPatternPreprocessor pattern_preprocessor,
			final ISqlInterceptor<List<IRecord>> sqlinterceptor) {

		if (jces == null || jces.length < 1) {
			return null;
		} // if

		final var _patterns = jces[0].value();// sql语句模板数组
		// 没有传入sqlpattern 代表一个采用默认 pattern 需要用pattern_preprocessor 来解析。
		final String[] patterns = (_patterns == null || _patterns.length < 1) ? new String[] { null } : _patterns;
		final var pargs = params(method, args);// 构造参数对象

		return jdbc.withConnection(conn -> {// 开启事务管理
			for (final var pattern : patterns) {// sql语句模板 :当pattern 为null的时候，pattern_preprocessor
				// 会为其左方法签名的解释。不过需要namedsql_processor配置。
				final var dd = sqlinterceptor.intercept(method, pargs, pattern, jdbc);
				if (dd != null) {
					continue;// 非空表示拦截
				} // if
					// 提取SQL语句模板,会自动为 null的pattern 提供方法签名的解释
				final var patternLines = pattern_preprocessor.handle(method, pargs, pattern, jdbc);
				if (patternLines == null) {// 方法的SQL语句模板解释失败
					throw new Exception(Jdbcs.format("无法为方法{0}解析出正确的SQL语句，请确保为Jdbc对象安装了正确的pattern_preprocessor!",
							method.getName()));
				} // lines
				final String[] tpl_patterns = patternLines.split(";\\s*\n+");// 尝试对sqls 进行多语句解析，位于行末的分号给予分解
				for (final var tpl_pattern : tpl_patterns) {// 依次执行SQL语句
					if (tpl_pattern.matches("\\s*")) {
						continue;
					} // if
					final var sqltpl = Jdbcs.format(tpl_pattern, args);// 提取SQL语句
					if (debug) {
						System.out.println("jdbc:handleJdbcQuery:" + sqltpl);
					} // if
					final var pstmt = pudt_stmt(conn, sqltpl);
					jdbc.pstmt_execute_throws(pstmt, sqltpl.contains("?") ? args : null, true);
				} // for sql:sqls
			} // for var pattern:patterns

			return true;
		});// withTransaction
	}

	/**
	 * 查询的处理：通用查询，由具体俄查询分析工
	 *
	 * @param jdbc    jdbc对象
	 * @param queryer 具体的查询器：handleJdbcQuery，handleJdbcPreparedQuery
	 * @param method  调用方法
	 * @param args    调用参数
	 * @return SQL 查询结果
	 */
	private static Object handleGenericQuery(final Jdbc jdbc, final Supplier<List<IRecord>> queryer,
			final Method method, final Object[] args) {

		final Class<?> retCls = method.getReturnType();// 返回类型
		// System.out.println(retCls.getName());
		final List<IRecord> recs = queryer.get();// 数据查询

		// 集合类型数据处理：判断结果的依旧就返回值是Collection 则为集合，否则就是单值
		if (Collection.class.isAssignableFrom(retCls)) {// 集合类型数据处理：是否单值
			return recs;
		} else {// 单个值：则尝试进行左类型转换。
			final IRecord rec = recs != null && recs.size() > 0 ? recs.get(0) : null;// 不存在则返回null
			return IRecord.rec2obj(rec, retCls);// 把单个record 转传承目标类型。
		} // if
	}

	/**
	 * 查询的处理
	 *
	 * @param jdbc                 jdbc对象
	 * @param jcqs                 jdbc的查询
	 * @param method               调用方法
	 * @param args                 调用参数
	 * @param pattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．
	 * @param sqlinterceptor       SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                             sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                             也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                             method(m) 方法对象,<br>
	 *                             params(a args) 参数列表:name-&gt;value,<br>
	 *                             sqlpattern(p pattern) sql模板 ，sql
	 *                             模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                             jdbc(j) 当前连接的数据库对象．<br>
	 * @return SQL 查询结果
	 */
	private static Object handleJdbcQuery(final Jdbc jdbc, final JdbcQuery[] jcqs, final Method method,
			final Object[] args, final ISqlPatternPreprocessor pattern_preprocessor,
			final ISqlInterceptor<List<IRecord>> sqlinterceptor) {

		// 非法参数直接返回
		if (jcqs == null || jcqs.length < 1) {
			return null;
		} else {
			// 通用jdbc 查询
			return Jdbc.handleGenericQuery(jdbc, () -> {
				final var params = params(method, args);
				List<IRecord> recs = sqlinterceptor.intercept(method, params, jcqs[0].value(), jdbc);

				if (recs != null) {
					return recs;// 方法截取
				} else {
					final var _params = params(method, args);
					final var pattern = pattern_preprocessor.handle(method, _params, jcqs[0].value(), jdbc);// 模式处理
					final var sql = Jdbcs.format(pattern, args);// SQL语句组装

					if (debug) {
						System.out.println("jdbc:handleJdbcQuery:" + sql);// 调试信息
					} // debug

					try {
						recs = jdbc.sql2records_throws(sql);// 执行SQL查询
					} catch (Exception e) {
						final var new_param = params
								.aoks2rec(k -> params.get(k) instanceof Number ? "##" + k : "#" + k);
						final var new_sql = Term.FT(sql, new_param);
						recs = jdbc.sql2records(new_sql);// 执行SQL查询
					} // try

					return recs; // 结果返回
				} // if
			}, method, args);// handleGenericQuery
		} // if
	}

	/**
	 * 查询的处理<br>
	 *
	 * @param jdbc                 jdbc对象
	 * @param jcq2s                jdbc的查询
	 * @param method               调用方法
	 * @param args                 调用参数
	 * @param pattern_preprocessor sql语句pattern预处理：比如对于特征占位符个与替换，特别是datasharding的分数据分表．<br>
	 * @param sqlinterceptor       SQL方法执行拦截器：需要注意 sqlinterceptor 是在
	 *                             sqlpattern_preprocessor 处理之后才给予调用的。<br>
	 *                             也就是说先调用sqlpattern_preprocessor，然后在调用sqlinterceptor;<br>
	 *                             method(m) 方法对象,<br>
	 *                             params(a args) 参数列表:name-&gt;value,<br>
	 *                             sqlpattern(p pattern) sql模板 ，sql
	 *                             模板通称会携带模板参数，以完成分数据库，分表的能力,<br>
	 *                             jdbc(j) 当前连接的数据库对象．<br>
	 * @return SQL 查询结果
	 */
	private static Object handleJdbcPreparedQuery(final Jdbc jdbc, final JdbcPreparedQuery[] jcq2s, final Method method,
			final Object[] args, final ISqlPatternPreprocessor pattern_preprocessor,
			final ISqlInterceptor<List<IRecord>> sqlinterceptor) {

		// 条件检测
		if (jcq2s == null || jcq2s.length < 1) {
			return null;
		} else { // 通用查询
			return Jdbc.handleGenericQuery(jdbc, () -> {
				final var params = params(method, args);
				List<IRecord> recs = sqlinterceptor.intercept(method, params, jcq2s[0].value(), jdbc);
				if (recs != null) {
					return recs;// 方法截取
				} else {
					final var preparedSqlpattern = pattern_preprocessor.handle(method, params, jcq2s[0].value(), jdbc);
					final var preparedSql = Jdbcs.format(preparedSqlpattern, args);// SQL语句组装
					if (debug) {
						System.out.println("jdbc:handleJdbcQuery2:preparedsql" + preparedSql);
					} // if
					recs = jdbc.precords(preparedSql, args);// prepared SQL 执行查询

					return recs; // 检索结果返回
				} // if
			}, method, args);// handleGenericQuery
		} // if
	}

	/**
	 * 添加引号
	 * 
	 * @param mapper 值变换函数
	 * @return 添加引号函数
	 */
	public static BiFunction<String, Object, String> quote(final Function<Object, Object> mapper) {

		return (pat, e) -> {
			return Jdbcs.format(!pat.startsWith("##") ? "''{0}''" : "{0}", Jdbc.asString(mapper.apply(e)));
		};
	}

	/**
	 * 值替换的时候 自动为值添加商英文双引号“"”，但是对于以double sharp 开头的pat 不予添加引号。<br>
	 * 用params中的值替换line中的各种pattern的内容。<br>
	 * 
	 * 示例：quote_substitute("添加引号:#name,不添加引号 :##name",
	 * "#+(\\w+)",REC("name","肥大细胞"))返回 <br>
	 * 添加引号:"肥大细胞",不添加引号 :肥大细胞 <br>
	 * 
	 * @param line    待解析的行数据
	 * @param pattern 变量名的pattern
	 * @param params  变量的定义：{name-&gt;value}
	 * @return 解析后的结果
	 */
	public static String quote_substitute(final String line, final String pattern, final IRecord params) {

		final Function<Object, Object> mapper = (o) -> {
			final var clazz = o == null ? Objects.class : o.getClass();
			if (o instanceof Date) {
				return Times.sdf.format((Date) o);
			} else if (o instanceof LocalTime) {
				return Times.sdf.format(Times.lt2dt((LocalTime) o));
			} else if (o instanceof LocalDate) {
				return Times.sdf.format(Times.ld2dt((LocalDate) o));
			} else if (o instanceof LocalDateTime) {
				return Times.sdf.format(Times.ldt2dt((LocalDateTime) o));
			} else if (Number.class.isAssignableFrom(clazz) || clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class || clazz == double.class) {
				return String.valueOf(o);
			} else {
				return o;
			} // if
		};// 变换函数

		return substitute(line, Pattern.compile(pattern), params, quote(mapper));
	}

	/**
	 * 把一個值對象轉換成字符串
	 * 
	 * @param obj 值對象
	 * @return 字符串
	 */
	public static String asString(final Object obj) {

		String line = null;
		final var _obj = obj instanceof Stream<?> s ? s.toList() : obj;

		if (_obj instanceof Map || _obj instanceof Iterable || _obj instanceof IRecord) {
			line = Json.obj2json(_obj);
		} else {
			line = String.valueOf(_obj);
		} // if

		return line;
	}

	/**
	 * 值替换的时候 自动为值添加商英文双引号“"”，但是对于以double sharp 开头的pat 不予添加引号。
	 * 用params中的值替换line中的各种pattern的内容。 但是对于以double sharp 开头的pat 不予添加引号。
	 * 
	 * 示例：quote_substitute("添加引号:#name,不添加引号 :##name",
	 * "#+(\\w+)",REC("name","肥大细胞"))返回 添加引号:"肥大细胞",不添加引号 :肥大细胞
	 * 
	 * @param line    待解析的行数据
	 * @param pattern 变量名的pattern
	 * @param params  变量的定义：{name-&gt;value}
	 * @return 解析后的结果
	 */
	public static String quote_substitute(final String line, final Pattern pattern, final IRecord params) {

		return substitute(line, pattern, params, quote(Jdbc::asString));
	}

	/**
	 * 简单替换 <br>
	 * 用params中的值替换line中的各种pattern的内容。不会添加任何 辅助标记，比如引号。<br>
	 * 
	 * @param line    待解析的行数据
	 * @param pattern 变量名的pattern
	 * @param params  变量的定义：{name->value}
	 * @return 解析后的结果
	 */
	public static String substitute(final String line, final String pattern, final IRecord params) {

		return substitute(line, Pattern.compile(pattern), params, (pat, e) -> Jdbc.asString(e));
	}

	/**
	 * 简单替换 <br>
	 * 用params中的值替换line中的各种pattern的内容。不会添加任何 辅助标记，比如引号。<br>
	 *
	 * @param line    待解析的行数据
	 * @param pattern 变量名的pattern
	 * @param params  变量的定义：{name-&gt;value}
	 * @return 解析后的结果
	 */
	public static String substitute(final String line, final Pattern pattern, final IRecord params) {

		return substitute(line, pattern, params, (pat, e) -> Jdbc.asString(e));
	}

	/**
	 * 模式替换：带有回调函数，可以进行替换值的自定义。<br>
	 * <p>
	 * 示例：<br>
	 * var t = Jdbc.substitute("hello #your_name i am #my_name","#(\\w+)",<br>
	 * REC("your_name","张三","my_name","李四"),(e)->e+"!");<br>
	 * 返回:hello 张三! i am 李四!<br>
	 * <p>
	 * 用params中的值替换line中的各种pattern的内容。<br>
	 *
	 * @param line     待解析的行数据
	 * @param pattern  变量名的pattern
	 * @param params   变量的定义：{name-&gt;value}
	 * @param replacer 二元函数：使用params中的值替换pattern结构的变量的时候，对值的操作函数。把对象转换成字符串的方法。<br>
	 *                 (pat,value)-&gt;String, pat 是匹配的模式的字符串,value 是在params 与 pat
	 *                 中的key 为 pat的 group(1)的数据值。
	 * @return 解析后的结果
	 */
	public static <T> String substitute(final String line, final String pattern, final IRecord params,
			final BiFunction<String, T, String> replacer) {

		return Jdbc.substitute(line, Pattern.compile(pattern), params, replacer);
	}

	/**
	 * 模式替换：带有回调函数，可以进行替换值的自定义。<br>
	 * <p>
	 * 用params中的值替换line中的各种pattern的内容。<br>
	 * <p>
	 * 例如： 一段 line: hello #your_name i am #my_name 的一段文字。<br>
	 * 使用 pattern: "#(\\w+)" 在 REC("your_name","张三","my_name","李四")进行替换, <br>
	 * callback :e->e+"!"<br>
	 * 就会返回：<br>
	 * hello 张三！i am 李四！<br>
	 *
	 * @param line     待解析的行数据
	 * @param pattern  变量名的pattern,必须在pattern中使用分组标识出：pattern 标识的变量名称！ 否则就会之际返回
	 * @param params   变量的定义：{name-&gt;value},
	 *                 对于满足line中的pattern,但是尚未再params找到对应值的数据，将其给予再line中删除，即替换为空白。
	 * @param replacer 二元函数：使用params中的值替换pattern结构的变量的时候，对值的操作函数。把对象转换成字符串的方法。<br>
	 *                 (pat,value)-&gt;String, pat 是匹配的模式的字符串,value 是在params 与 pat
	 *                 中的key 为 pat的 <br>
	 *                 group(1)的数据值。
	 * @return 解析后的结果，对于满足line中的pattern,但是尚未再params找到对应值的数据，将其给予再line中删除，即替换为空白。
	 */
	@SuppressWarnings("unchecked")
	public static <T> String substitute(final String line, final Pattern pattern, final IRecord params,
			final BiFunction<String, T, String> replacer) {

		final var matcher = pattern.matcher(line);
		final var _line = matcher.replaceAll(r -> {
			final var key = r.group(1); // 提取pattern所标识的变量
			final var value = params.get(key); // 提取该pattern所标识的变量的值。
			final var _value = replacer.apply(r.group(), (T) value); // 获取替换值的字符串标识。
			return _value;
		}); // 变换后的结果

		return _line;
	}

	/**
	 * 从clazz 对象中寻找一个名字叫做 name的方法
	 *
	 * @param clazz 类对象
	 * @param name  方法名称的字符串表示
	 * @return 方法对象
	 */
	public static Method methodOf(final Class<?> clazz, final String name) {

		return Arrays.stream(clazz.getDeclaredMethods()).filter(e -> e.getName().equals(name)).findFirst().get();
	}

	/**
	 * 快速构造一个Map&lt;Object,Object&gt;的对象:快送构造Map的方法
	 *
	 * @param oo key1,value1,key2,value2的序列
	 * @return Map&lt;Object,Object&gt;的对象
	 */
	public static Map<Object, Object> M(final Object... oo) {

		final var map = new LinkedHashMap<>();
		if (oo != null && oo.length > 0) {
			for (int i = 0; i + 1 < oo.length; i += 2) {
				map.put(oo[i], oo[i + 1]);
			} // for
		} // if
		return map;
	}

	/**
	 * 创建jdbc连接对象
	 */
	public void init(final String driver, final String url, final String user, final String password) {

		this.supplierConn = () -> {// 自己提供数据库连接
			Connection conn = null;// 数据库连接
			try {
				final Class<?> cls = (null != driver) ? Class.forName(driver) : null;
				if (cls == null) {
					System.err.println("数据库驱动:\"" + driver + "\"加载失败!");
					return null;
				}
				conn = DriverManager.getConnection(url, user, password);
			} catch (Exception e) {
				System.err.println(Jdbcs.format("jdbc connection error for,driver:{0},url:{1},user:{2},password:{3}",
						driver, url, user, password));
				e.printStackTrace();
			} // try
			return conn;
		};// 连接构造其
	}

	/**
	 * 目前只支持mysql，为了防止意外发生，不提供表删除操作
	 *
	 * @param tableName 表名,不能带有数据库名：即不能出现dbname.tblename这样的全路径名，而只能为tblename的相对路径名。
	 * @return 表格存在 返回 true,表格不存在返回false
	 */
	public boolean tblExists(final String tableName) {

		return this.sql2records("show tables like '" + tableName + "'").size() > 0;
	}

	/**
	 * 目前只支持mysql
	 *
	 * @param database 数据库名
	 * @return 数据库存在 返回 true,数据库不存在返回false
	 */
	public boolean dbExists(final String database) {

		return this.sql2records("show databases like '" + database + "'").size() > 0;
	}

	/**
	 * 目前只支持mysql
	 *
	 * @param tableName 表名
	 * @param defs      表定义
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean createTable(final String tableName, final String defs) {

		final String sql = Jdbcs.format("create table {0} ( {1} ) ", defs);
		return this.sqlexecute(sql);
	}

	/**
	 * 目前只支持mysql
	 *
	 * @param tableName 表名
	 * @param defs      表定义的各个字段定义[如 name varchar(32)]序列 会采用逗号进行字段连接
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean createTable(final String tableName, final String... defs) {

		final String sql = Jdbcs.format("create table {0} ( {1} ) ", tableName, String.join(",", Arrays.asList(defs)));
		return this.sqlexecute(sql);
	}

	/**
	 * 目前只支持mysql
	 *
	 * @param tableName 表名
	 * @param defs      表定义的各个字段定义[如 name varchar(32)]序列 会采用逗号进行字段连接
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean createTableIfNotExists(final String tableName, final String... defs) {

		if (this.tblExists(tableName)) {
			return false;
		}

		final String sql = Jdbcs.format("create table {0} ( {1} ) ", tableName, String.join(",", Arrays.asList(defs)));
		if (debug) {
			System.out.println("jdbc:createTableIfNotExists:" + sql);
		}

		return this.sqlexecute(sql);
	}

	/**
	 * 获得数据库连接:每次都是重新创建一个数据库连接．
	 *
	 * @return 数据库连接
	 */
	public Connection getConnection() {

		final Connection conn = supplierConn.get();
		return conn;
	}

	/**
	 * 執行sql
	 *
	 * @param sql 更新操作的sql语句：update,delete,drop 等
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean sqlexecute(final String sql) {

		return psqlexecute(sql, (Map<Integer, Object>) null);
	}

	/**
	 * 執行sql
	 *
	 * @param sql 更新操作的sql语句：update,delete,drop 等
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean psqlexecute(final String sql, final Map<Integer, Object> params) {

		return psqlexecute(sql, params, this.getConnection(), true);
	}

	/**
	 * 执行SQL语句不考虑对原有的数据的做的变化：
	 *
	 * @param sql 更新操作的sql语句：update,delete,drop 等
	 * @param oo  参数数组
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean psqlexecute(final String sql, final Object[] oo) {

		final Map<Integer, Object> params = new HashMap<>();
		if (oo != null) {
			for (int i = 0; i < oo.length; i++) {
				params.put(i + 1, oo[i]);
			}
		}

		return psqlexecute(sql, params, this.getConnection(), true);
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
	public synchronized IRecord withTransaction(final IDataManipulation<IJdbcSession<UUID, Object>> dm) {

		return this.withTransaction(dm, (IJdbcSession<UUID, Object>) null, (Map<Object, Object>) null);
	}

	/**
	 * 指定session 执行DataManipulation．:Session 是Monad对象。因此可以进行函数式的状态编程<br>
	 * <p>
	 * 发起创建一个IJdbcSession对象，并通过IJdbcSession急性数据库操作<br>
	 * 事务处理,每一个事务，系统会动态的创建出一个 session 对象（IJdbcSession），这个Session
	 * 对象拥有一个UUID类型的对象标识。<br>
	 * 在一次事务性的会话IJdbcSession中：共享一个数据库连接，并且出现操作失败（sql操作)，将给予先前的动作回滚．<br>
	 * 事务只能对DML语句进行操作，对数据定义类语句DDL无法操作，例如建表、建立索引、建立分区等。<br>
	 * 一般采用如下方式调用此函数(使用Jdbc.M设置 session属性)：<br>
	 * jdbc.withTransaction(sess->{session.sql2records("show databases");},<br>
	 * Jdbc.M(SqlPatternPreprocessor.class,spp));<br>
	 *
	 * @param dm             DataManipulation 代表，数据操作的具体过程 dm
	 *                       的数据如果需要会馆请使用dm所提供的session 来操作数据,通常采用lamba表达式来给予
	 *                       创建操作过程：sess->{写入你的操作代码}.
	 *                       需要注意对于withTransaction创建的会话IJdbcSession 是以monad
	 *                       容器。其初始数据为Object类型 值为null.
	 * @param sessAttributes 附加到sess上的属性信息。注入到 sess 上下文中的属性数据{(key,value)}
	 * @return {ret:返回值boolean值, exception:异常类型, throwable:异常类型,用于动态代理的默认函数,
	 *         result:sess的结果属性},参见Jdbc.newInstance
	 */
	public synchronized IRecord withTransaction(final IDataManipulation<IJdbcSession<UUID, Object>> dm,
			final Map<Object, Object> sessAttributes) {

		return this.withTransaction(dm, (IJdbcSession<UUID, Object>) null, sessAttributes);
	}

	/**
	 * 指定session 执行DataManipulation．:Session 是Monad对象。因此可以进行函数式的状态编程<br>
	 * 发起创建一个IJdbcSession对象，并通过IJdbcSession急性数据库操作<br>
	 * 事务处理,每一个事务，系统会动态的创建出一个 session 对象，这个Session 对象拥有一个UUID类型的对象标识。<br>
	 * 在一次事务性的会话IJdbcSession中：共享一个数据库连接，并且出现操作失败（sql操作)，将给予先前的动作回滚．<br>
	 * 事务只能对DML语句进行操作，对数据定义类语句DDL无法操作，例如建表、建立索引、建立分区等。<br>
	 *
	 * @param dm             数据操作的具体过程 dm 的数据如果需要会馆请使用dm所提供的session 来操作数据
	 * @param sess           数据操作所在的会话会话对象,其实就是对一个Connection的包装.需要注意对于withTransaction创建的会话IJdbcSession
	 *                       是以monad 容器。其初始数据为Object类型 值为null.
	 * @param sessAttributes 附加到sess上的属性信息。注入到 sess 上下文中的属性数据{(key,value)}
	 * @return {ret:返回值boolean值, exception:异常类型, throwable:异常类型,用于动态代理的默认函数,
	 *         result:sess的结果属性}, 参见Jdbc.newInstance
	 */
	public synchronized IRecord withTransaction(final IDataManipulation<IJdbcSession<UUID, Object>> dm,
			final IJdbcSession<UUID, Object> sess, final Map<Object, Object> sessAttributes) {

		boolean success = true;// 执行状态，不是成功就是失败，判断标志就是 completed是否爆出异常
		// IJdbcSession 专门定义了一套jdbc的操作函数。这些函数的操作不会关闭数据库连接。
		final Connection conn = this.supplierConn.get();// 自己创建一个数据库连接，这个数据库连接将在整个transaction 进行共享。
		boolean conn_closed = false; // 数据库连接关闭标记
		Exception exception = null;// 异常结构
		Throwable throwable = null;// 可抛出性异常的结构
		Object result = null;
		try {
			if (conn.getAutoCommit())
				conn.setAutoCommit(false);// 取消自动提交。
			try {// DM数据操作
					// 给出一个自定义实现
				final IJdbcSession<UUID, Object> session = sess != null // 判断参数提供的session 是否有效。
						? sess // 非空则采用 参数提供的Session
						: new AbstractJdbcSession<UUID, Object>() { // 空值则提供一个默认的实现 会话的实现。

							@Override
							public Set<Stream<?>> getActiveStreams() {
								return managedStream.getActiveStreams();
							}

							@Override
							public Connection getConnection() {
								return conn;
							}// 创建session 会话

							@Override
							public UUID transCode() {
								return uuid;
							}// 交易编码的实现

							@Override
							public Object getData() {
								return data;
							} // 会话中的数据

							@Override
							public Object setData(Object _data) {
								return this.data = _data;
							} // 会话中的数据

							@Override
							public Map<Object, Object> getAttributes() {
								return this.attributes;
							} // 返回会话属性

							private final UUID uuid = UUID.randomUUID();
							private Object data = null;// 初始创建Monad中data 初始化为null
							private final Map<Object, Object> attributes = new HashMap<>();// 会话属性
							private final IManagedStreams managedStream = new ManagedStreams(); // 托管流对象

						};// IJdbcSession

				if (sessAttributes != null && sessAttributes.size() > 0) { // 设置session 的会话属性
					session.setAttributes(sessAttributes);// 设置属性
				} // if 设置session的属性

				if (dm != null) { // 执行方法 调用 进行 数据操作
					dm.invoke(session); // 方法属性调用
				}

				result = session.getResult(); // 提取会话属性中的结果内容
				session.clear(); // sess 对象清空
			} catch (Throwable e) { // 出现异常则进行回滚
				success = false;
				conn.rollback();
				throw e;// 把异常抛向外层
			} finally { // try 异常出现则回滚，标记执行失败
				if (success)
					conn.commit();// 未出现异常则提交结果
				if (conn != null && !conn.isClosed()) {
					conn.close();// 关闭连接
					conn_closed = true;
				} else {
					conn_closed = false;
				}
			} // try 异常处理
		} catch (Exception e) {
			exception = e;// 记录异常场景
			e.printStackTrace();
		} catch (Throwable e) {
			throwable = e;// 记录异常场景
			e.printStackTrace();
		} finally {
			try {
				if (!conn_closed && conn != null && !conn.isClosed()) {
					conn.close();
				} // if
			} catch (Exception e) {
				e.printStackTrace();
			} // try inner
		} // try outter

		final var rec = SimpleRecord.REC2("ret", success, "exception", exception, "throwable", throwable, "result",
				result);

		this.clear(); // jdbc 流数据清空

		return rec;
	};

	/**
	 * 執行sql
	 *
	 * @param <T>     返回结果类型
	 * @param handler 數據庫連接处理子
	 * @return 返回结果类型T
	 */
	public <T> T sqlexecute(final Function<Connection, T> handler) {

		return handler.apply(this.getConnection());
	}

	/**
	 * 執行sql
	 *
	 * @param sql   更新操作的sql语句：update,delete,drop 等
	 * @param conn  數據庫連接
	 * @param close 是否關閉連接
	 * @return 执行状态标记 true if the first result is a ResultSet object; false if it is
	 *         an update count or there are no results
	 */
	public boolean psqlexecute(final String sql, final Map<Integer, Object> params, final Connection conn,
			final boolean close) {

		PreparedStatement pstmt = null;
		boolean ret = false;
		if (conn == null) {
			System.out.println("数据库连接为null,不予执行语句！");
			return false;
		}
		try {
			pstmt = conn.prepareStatement(sql);
			if (params != null)
				for (var key : params.keySet())
					pstmt.setObject(key, params.get(key));
			ret = pstmt.execute();
			// System.out.println(ret+"------");
		} catch (Exception e) {
			System.out.println("error sql:\n\"" + sql + "\"");
			e.printStackTrace();
		} finally {
			try {
				if (pstmt != null)
					pstmt.close();
				if (close)
					conn.close();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * 批量執行sql语句集合
	 *
	 * @param sqls sql 语句集合
	 */
	public List<Boolean> sqlbatch(final List<String> sqls) {

		if (sqls == null) {
			return null;
		}
		final Connection conn = this.getConnection();
		final List<Boolean> bb = sqls.stream().filter(e -> e != null && e.length() > 0 && !e.matches("^[\\s]*$"))// 滤除空语句
				.map(sql -> psqlexecute(sql, null, conn, false)).collect(Collectors.toList());
		try {
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bb;
	}

	/**
	 * 批量執行sql语句集合
	 *
	 * @param sqls sql 语句集合
	 */
	public List<Boolean> sqlbatch(final String[] sqls) {

		if (sqls == null) {
			return null;
		}

		return this.sqlbatch(Arrays.asList(sqls));
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @param qh  結果的處理
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2apply(final String sql, final Map<Integer, Object> params, final Jdbc.SQL_MODE mode,
			final IQueryHandler<T> qh) {

		return psql2apply(sql, params, this.getConnection(), true, mode, qh);
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql  查询语句
	 * @param conn 數據庫連接
	 * @param mode 请求的模式 :查询还是更新
	 * @param qh   結果的處理
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2apply(final String sql, final Map<Integer, Object> params, final Connection conn,
			final Jdbc.SQL_MODE mode, final IQueryHandler<T> qh) {

		return psql2apply(sql, params, conn, true, mode, qh);
	}

	/**
	 * 这个函数是专门为了DataMatrix::new 来进行设计的． var mx =
	 * jdbc.psql2apply(sql,params,DataMatrix::new); 生成结构表对象
	 *
	 * @param sql      sql 语句
	 * @param producer 对象构造行数 (oo,hh)->T , oo数据对象矩阵,hh 表头
	 * @return T 类型的结果
	 */
	public <T> T sql2apply(final String sql, final BiFunction<Object[][], List<String>, T> producer) {

		return this.psql2apply(sql, null, producer);
	}

	/**
	 * 这个函数是专门为了DataMatrix::new 来进行设计的．<br>
	 * var mx = jdbc.psql2apply(sql,params,DataMatrix::new); <br>
	 * 生成结构表对象
	 *
	 * @param sql      sql 语句
	 * @param producer 对象构造行数 (oo,hh)->T , oo数据对象矩阵,hh 表头
	 * @return T 类型的对象
	 */
	public <T> T psql2apply(final String sql, final Map<Integer, Object> params,
			final BiFunction<Object[][], List<String>, T> producer) {

		T t = null;
		try {
			t = psql2apply_throws(sql, params, producer);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return t;
	}

	/**
	 * 返回的结果集：就是一个key值为列名：ColumnLabel <br>
	 * 对于 SQL_UPDATE
	 * 模式结果会返回会包括，GENERATED_KEY，生成的主键，COLS_CNT，列数，UPDATED_CNT，更新数据行数。<br>
	 *
	 * @param sql   查询语句
	 * @param conn  數據庫連接
	 * @param close 是否關閉連接
	 * @param qh    結果的處理
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2apply(final String sql, final Map<Integer, Object> params, final Connection conn,
			final boolean close, final Jdbc.SQL_MODE mode, final IQueryHandler<T> qh) {

		T ret = null;
		try {
			ret = this.psql2apply_throws(sql, params, conn, close, mode, qh);
		} catch (SQLException e) {
			e.printStackTrace();
		} // try
		return ret;
	}

	/**
	 * 生成结构表对象
	 *
	 * @param <T>      返回结果的类型
	 * @param sql      sql 语句
	 * @param producer 对象构造行数 (oo,hh)->T , oo数据对象矩阵,hh 表头
	 * @return T类型数据对象 由 producer 所生成。
	 */
	public <T> T psql2apply_throws(final String sql, final Map<Integer, Object> params,
			final BiFunction<Object[][], List<String>, T> producer) throws SQLException {

		return psql2apply_throws(sql, params, (conn, stmt, rs, rsm, n) -> {
			rs.last();
			final Object[][] oo = new Object[rs.getRow()][n];
			rs.beforeFirst();
			while (rs.next()) {
				for (int j = 1; j <= n; j++) {
					oo[rs.getRow() - 1][j - 1] = rs.getObject(j);
				} // for
			} // while

			final Function<Integer, String> foo = i -> {
				String s = null;
				try {
					s = rsm.getColumnLabel(i);
				} catch (Exception ignored) {
				}
				return s;
			};
			final List<String> hh = Stream.iterate(1, i -> i + 1).map(foo).limit(n).collect(Collectors.toList());

			return producer.apply(oo, hh);
		});
	}

	/**
	 * sql2apply_throws 是对SQL 语句执行的一种抽象 <br>
	 * sql2apply_throws 认为对于任何的一次SQL请求包括：<br>
	 * sql
	 * 语句,sql语句的处理模式(mode),数据库连接(conn)，连接的使用后处理情况(close)，以及对返回结果的后续加工处理(QueryHandler)。<br>
	 * <p>
	 * 带有异常抛出的 sql2apply <br>
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * 对于 SQL_UPDATE
	 * 模式结果会返回会包括，GENERATED_KEY，生成的主键，COLS_CNT，列数，UPDATED_CNT，更新数据行数。<br>
	 *
	 * @param sql    查询语句
	 * @param qh     結果的處理 (conn, stmt, rs, rsm, cols_cnt)->T
	 * @param params sql 中的占位符参数
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2apply_throws(final String sql, final Map<Integer, Object> params, final IQueryHandler<T> qh)
			throws SQLException {

		return psql2apply_throws(sql, params, this.getConnection(), true, SQL_MODE.QUERY_SCROLL, qh);
	}

	/**
	 * psql:prepared SQＬ的别名．<br>
	 * psql2apply_throws 是对SQL 语句执行的一种抽象 <br>
	 * psql2apply_throws 认为对于任何的一次SQL请求包括：<br>
	 * sql
	 * 语句,sql语句的处理模式(mode),数据库连接(conn)，连接的使用后处理情况(close)，以及对返回结果的后续加工处理(QueryHandler)。<br>
	 * <p>
	 * 带有异常抛出的 sql2apply <br>
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * 对于 SQL_UPDATE
	 * 模式结果会返回会包括，GENERATED_KEY，生成的主键，COLS_CNT，列数，UPDATED_CNT，更新数据行数。<br>
	 *
	 * @param sql    查询语句
	 * @param params 语句参数
	 * @param conn   數據庫連接
	 * @param close  是否關閉連接
	 * @param qh     結果的處理 (conn, stmt, rs, rsm, cols_cnt)->T
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2apply_throws(final String sql, final Map<Integer, Object> params, final Connection conn,
			final boolean close, final Jdbc.SQL_MODE mode, final IQueryHandler<T> qh) throws SQLException {

		PreparedStatement stmt = null;// 查询语句
		ResultSet rs = null;// 结果集对象
		T ret = null;
		final long begTime = System.currentTimeMillis();
		if (conn == null) {
			System.out.println("數據連接為空(null),不予進行數據查詢");
			return null;
		}

		try {
			int cols_cnt = 0;// 结果集列数量
			int updated_cnt = 0; // 更新的行数量
			stmt = pstmt(conn, mode, sql, params); // 创建语句
			if (mode == SQL_MODE.UPDATE) {// SQL update 模式
				updated_cnt = stmt.executeUpdate();
				rs = stmt.getGeneratedKeys();// 获取生成主键信息，字段名称为：GENERATED_KEY
			} else if (mode == SQL_MODE.QUERY_SCROLL) {
				rs = stmt.executeQuery(); // 执行语句
			} else {// 默认为SQL query 模式
				rs = stmt.executeQuery(); // 执行语句
			} // if

			final ResultSetMetaData rsm = rs.getMetaData();
			cols_cnt = rsm.getColumnCount();
			ret = qh.handle(conn, stmt, rs, rsm, cols_cnt);
			if (mode == SQL_MODE.UPDATE && ret instanceof List && ((List<?>) ret).size() == 1
					&& ((List<?>) ret).get(0) instanceof IRecord) {
				((IRecord) ((List<?>) ret).get(0)).add("COLS_CNT", cols_cnt).add("UPDATED_CNT", updated_cnt);
			} // if 返回结果
		} catch (Exception e) {
			System.out.println("\n-------------gbench提醒：出错sql-------------");
			System.out.println(sql);
			System.out.println("--------------------------------------------\n");
			e.printStackTrace();
			throw e;// 把捕获的异常继续跑出去
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
				if (close && conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw e; // 把捕获的异常继续跑出去
			} // try 关闭结果集合
		} // try

		final long endTime = System.currentTimeMillis();
		if (debug) {
			System.out.println("last for:" + (endTime - begTime) + "ms");
		}

		return ret;
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。
	 *
	 * @param pstmt  prepared 语句
	 * @param params 语句参数 pos->value, pos 从1开始
	 * @param close  执行结束后是否需要关闭 prepared 语句
	 * @return true if the first result is a ResultSet object; false if the first
	 *         result is an update count or there is no result
	 * @throws SQLException
	 */
	public boolean pstmt_execute_throws(final PreparedStatement pstmt, final Map<Integer, Object> params,
			final boolean close) throws SQLException {

		if (params != null) {
			for (final var i : params.keySet()) {
				pstmt.setObject(i, params.get(i));
			} // for
		} // if
		final boolean b = pstmt.execute();
		if (close) {
			pstmt.close();
		} // if

		return b;
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。 preapared 批处理 <br>
	 * 
	 * @param pstmt  prepared 语句
	 * @param params 语句参数
	 * @param close  执行结束后是否需要关闭 prepared 语句
	 * @return true if the first result is a ResultSet object; false if the first
	 *         result is an update count or there is no result
	 * @throws SQLException
	 */
	public boolean pstmt_execute_throws(final PreparedStatement pstmt, final Object[] params, final boolean close)
			throws SQLException {

		if (params != null)
			for (int i = 0; i < params.length; i++) {
				pstmt.setObject(i + 1, params[i]);
			}
		final var b = pstmt.execute();
		if (close) {
			pstmt.close();
		}

		return b;
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。
	 *
	 * @param conn   数据据库连接
	 * @param sql    SQL语句
	 * @param params parepared 的位置参数{pos->value} pos 从1开始
	 * @return true if the first result is a ResultSet object; false if the first
	 *         result is an update count or there is no result
	 * @throws SQLException
	 */
	public boolean pconn_execute_throws(final Connection conn, final String sql, final Map<Integer, Object> params)
			throws SQLException {

		return pstmt_execute_throws(conn.prepareStatement(sql), params, true);
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。
	 *
	 * @param conn 数据据库连接
	 * @param sql  SQL语句
	 * @param pp   parepared 的位置参数{pos->value}
	 * @return true if the first result is a ResultSet object; false if the first
	 *         result is an update count or there is no result
	 * @throws SQLException
	 */
	public boolean pconn_execute_throws(final Connection conn, final String sql, final Object... pp)
			throws SQLException {

		if (pp == null) {
			return false;
		}
		final Map<Integer, Object> params = new HashMap<>();
		for (int i = 1; i <= pp.length; i++) {
			params.put(i, pp[i]);
		}

		return pstmt_execute_throws(conn.prepareStatement(sql), params, true);
	}

	/**
	 * 执行psql请求 <br>
	 * p开头的函数：表示prepared sql相关测操作
	 *
	 * @param <T>         目标结果类型
	 * @param conn        外界提供的数据库连接。
	 * @param transformer 把resultset 转变成目标结果类型。
	 * @param sql         sql语句模板
	 * @param params      模板参数数组
	 * @return 目标类型的T对象
	 * @throws Throwable 异常原因。使用Throwable的好处是可以很方便的进行二次异常封装（兼容多种异常类型）这样就可以建立起与JDBC
	 *                   SQL异常的最完整偶联机制。
	 */
	public <T> T pconn_query_throws(final Connection conn, final ThrowableFunction<ResultSet, T> transformer,
			final String sql, final Object[] params) throws Throwable {

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		T t = null;// 目标结果
		try {
			pstmt = conn.prepareStatement(sql);// 制作Prepare 语句。
			var pcnt = -1;// 默认参数非法
			try {// 检查是否可以使用getParameterCount
				pcnt = pstmt.getParameterMetaData().getParameterCount();// SQL语句可以接收参数的个数。
			} catch (UnsupportedOperationException ignored) {// 操作不支持异常,说明驱动程序尚未实现该功能
				// 忽略掉异常堆栈打印
				// ignored.printStackTrace();
			} // try 方法检测

			// 在SQL模板sql填如实际参数params，生成SQL语句
			if (pcnt > 0 && params != null) {

				for (int i = 0; i < params.length; i++) { // 模板参数遍历。
					if (i > pcnt) {// 参数设置超过了 PreparedStatement 所能容纳的参数个数。
						System.err.println(Jdbcs.format(
								"参数#{2}设置超过了 PreparedStatement 所能容纳的参数个数{3}。参数设置提前终止！\n"
										+ "set-warnnings:pconn_query_throws-sql:{0},params:{1}",
								sql, Arrays.asList(params), i, pcnt));
						break;
					} // i>pcnt

					if (params[i] != null) {
						try { // 只有当参数param[i]不为空才给予进行模板参数填充。
							pstmt.setObject(i + 1, params[i]);// 模板参数填充，注意模板参数需要从 1开始。
						} catch (Exception x) {
							System.err.println(Jdbcs.format("set-error:pconn_query_throws-sql:{0},params:{1}", sql,
									Arrays.asList(params)));
							throw x;// 一旦出现设置失败，立即异常抛出。因为参数设置师表就表名，该SQL语句执行不了，尽早告知用户程序，给予处理解决，别浪费时间做无用功。
						} // if params[i]!=null try
					} // if
				} // for 模板参数遍历。

			} else if (pcnt < 0) {// 非法数值表明getParameterCount无法获得参数数量
				for (int i = 0; i < params.length; i++) { // 模板参数遍历。
					pstmt.setObject(i + 1, params[i]);// 模板参数填充，注意模板参数需要从 1开始。
				} // for 模板参数遍历。

			} // if pcnt>0 && params!=null

			rs = pstmt.executeQuery();// 执行结果查询
			t = transformer.apply(rs);// 进行目标类型变换。
		} catch (SQLException e) {
			System.out.println("pconn_query_throws,error sql:" + sql);
			throw e;// 抛出异常 原因。
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
			if (rs != null) {
				rs.close();
			}
		} // try

		// 返回目标结果。
		return t;
	}

	/**
	 * 提供一个数据库连接，来执行callbacks函数 <br>
	 *
	 * @param connection 数据库连接，如果为null,表示自动创建数据库连接。
	 * @param callbacks  conn->t 数据库连接 的操作函数，通过数据库连接生产出目标结果对象 T。
	 * @return 执行结果集合的 IRecord 列表
	 * @throws SQLException
	 */
	public <T> T withConnection_throws(final ThrowableFunction<Connection, T> callbacks, final Connection connection)
			throws SQLException {

		final var ar = new AtomicReference<T>(); // 单值容器
		final var need_close = connection == null;// 外部没有提供数据库连接,则使用自创建的数据库连接。此时在使用完毕后，需要给予关闭。否则连接由外部给予关闭。
		final var _conn = connection == null ? this.getConnection() : connection; // 获取数据库连接

		try {
			ar.set(callbacks.apply(_conn));
		} catch (Throwable e) {
			throw new SQLException(e);
		} finally {
			if (need_close)
				_conn.close();
		}

		return ar.get(); // 从容器中提取运算结果。
	}

	/**
	 * 提供一个数据库连接，来执行callbacks函数
	 *
	 * @param callbacks conn->t 的回调函数
	 * @param <T>       回调函数的返回值类型
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T withConnection(final ThrowableFunction<Connection, T> callbacks) {

		T res = null;
		try {
			res = withConnection_throws(callbacks, null);
		} catch (SQLException e) {
			e.printStackTrace();
		} // 自动创建数据库连接

		return res;
	}

	/**
	 * prepared Statement 的查询操纵
	 *
	 * @param sql prepared Statement
	 * @param pp  pp 位置参数集合
	 * @return 查询结果结合
	 */
	public List<IRecord> precords(final String sql, final Object... pp) {
		List<IRecord> ll = null;
		try {
			ll = this.precords_throws(null, sql, pp);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ll;
	}

	/**
	 * prepared Statement 的查询操纵
	 *
	 * @param sql prepared Statement
	 * @param pp  位置参数集合
	 * @return 查询结果结合
	 */
	public List<IRecord> precords(final Connection connection, final String sql, final Object... pp) {

		List<IRecord> ll = null;
		try {
			ll = this.precords_throws(null, sql, pp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ll;
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。
	 *
	 * @param connection 提供的数据库连接，如果为null 则自动创建
	 * @param sql        sql语句模板
	 * @param params     sql语句参数 数组 ,可以为空表示没有参数，但此时需要sql中没有?问号占位符，即不做参数设置
	 * @return 结果集合
	 * @throws SQLException
	 */
	public List<IRecord> precords_throws(final Connection connection, final String sql, Map<Integer, Object> params)
			throws SQLException {

		final Object[] _params = params == null ? new Object[] {} : params.values().toArray();// preparedSQL的模板参数列表。
		if (params != null) {
			params.forEach((i, d) -> _params[i - 1] = params.get(i));// 矫正 参数位置
		}

		return precords_throws(connection, sql, _params);
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。
	 *
	 * @param connection 提供的数据库连接，如果为null 则自动创建
	 * @param sql        sql语句模板
	 * @param params     sql语句参数 数组 ,可以为空表示没有参数，但此时需要sql中没有?问号占位符，即不做参数设置
	 * @return 结果集合
	 * @throws SQLException
	 */
	public List<IRecord> precords_throws(final Connection connection, final String sql, final Object[] params)
			throws SQLException {

		return this.withConnection_throws(conn -> this.pconn_query_throws(conn, rs -> {
			final var recs = new LinkedList<IRecord>();
			final var lbls = labels(rs);
			final int n = lbls.size();// 结果集的列标签(列名）

			while (rs.next()) {
				final var rec = new LinkedRecord();
				for (int i = 1; i <= n; i++) {
					rec.set(lbls.get(i), rs.getObject(i));
				}
				recs.add(rec);
			} // while

			return recs;
		}, sql, params), connection);// withConnection
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。 专门为了DataMatrix::new 来进行设计的接口
	 *
	 * @param mxbuilder 数据矩阵的构建器
	 * @param sql       SQL语句
	 * @param pp        sql 语句的位置参数：占位符对应的实际数值
	 * @param <T>       返回结果的类型即数据矩阵的类型 mxbuilder 所生成的数据对象
	 * @return T 类型的结果
	 */
	public <T> T pmatrix(final BiFunction<Object[][], String[], T> mxbuilder, final String sql, final Object... pp) {

		T t = null;
		try {
			t = this.pmatrix_throws(mxbuilder, sql, pp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return t;
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。 专门为了DataMatrix::new 来进行设计的接口
	 *
	 * @param <T>       返回结果的类型即数据矩阵的类型 mxbuilder 所生成的数据对象的类型
	 * @param mxbuilder 数据矩阵的构建器
	 * @param sql       语句模板
	 * @param pp        sql语句的位置参数：占位符对应的实际数值
	 * @return 返回结果的类型即数据矩阵的类型 mxbuilder 所生成的数据对象
	 */
	public <T> T pmatrix2(final BiFunction<String[][], String[], T> mxbuilder, final String sql, final Object... pp) {

		T t = null;

		final BiFunction<Object[][], String[], T> _mxbuilder = (ooo, hh) -> mxbuilder.apply(casts(ooo), hh);
		try {
			t = this.pmatrix_throws(_mxbuilder, sql, pp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return t;
	}

	/**
	 * p开头的函数：表示prepared sql相关测操作。 专门为了DataMatrix::new 来进行设计的接口
	 *
	 * @param <T>       返回结果的类型即数据矩阵的类型 mxbuilder 所生成的数据对象的类型
	 * @param mxbuilder 数据矩阵的构建器
	 * @param sql       语句模板
	 * @param pp        sql 参数 sql 语句的位置参数：占位符对应的实际数值
	 * @return T 类型的结果
	 * @throws SQLException
	 */
	public <T> T pmatrix_throws(final BiFunction<Object[][], String[], T> mxbuilder, final String sql,
			final Object... pp) throws SQLException {

		return this.withConnection(conn -> this.pconn_query_throws(conn, rs -> {
			var recs = new ArrayList<Object[]>();
			var lbls = labels2(rs);
			int n = lbls.length;

			while (rs.next()) {
				var oo = new Object[n];
				for (int i = 1; i <= n; i++) {
					oo[i - 1] = rs.getObject(i);
				}
				recs.add(oo);
			}
			return mxbuilder.apply(recs.toArray(Object[][]::new), lbls);
		}, sql, pp));// withConnection
	}

	/**
	 * 大数据检索, 单次获取数据的数据量 30000
	 *
	 * @param largesql 大数据sql
	 * @return record 数据集合
	 */
	public List<IRecord> bigdataQuery(final String largesql) {

		return blockquery(largesql, 30000);
	}

	/**
	 * 分块数据检索
	 *
	 * @param sql       大数据的sql语句
	 * @param fecthsize 单次获取数据的数据量大小
	 * @return record 数据集合
	 */
	@SuppressWarnings("unchecked")
	public List<IRecord> blockquery(final String sql, final int fecthsize) {

		final Supplier<String> alias = () -> "table" + UUID.randomUUID().toString().replace("-", "");// 表格别名
		final String big_query = "select count(*) cnt from (" + sql + ") " + alias.get();
		final Optional<IRecord> opt = this.sql2maybe(big_query);
		if (!opt.isPresent()) {
			return new LinkedList<>();
		}
		final int cnt = opt.get().i4("cnt");// 首先读取记录条目数
		final int BATCH_SIZE = Math.abs(fecthsize) > 0 ? Math.abs(fecthsize) : 30000;// 默认块大小为30000
		final List<String> sub_sqls = new ArrayList<>(10);
		if (cnt > BATCH_SIZE) {// 只有超过BATCH_SIZE才给予分割
			for (int i = 0; i < cnt; i += BATCH_SIZE) {
				final int start = i;
				int end = (i + BATCH_SIZE) - 1;// 结束标记
				if (end > cnt) {
					end = cnt;
				}
				sub_sqls.add("select * from (" + sql + ") " + alias.get() + " limit " + start + "," + BATCH_SIZE);// 定义子任务处理范围批量大小
			} // for
		} else {// 没有超过batchsize不予分割
			sub_sqls.add(sql);
		} // if

		if (System.currentTimeMillis() < 0) {// 代码块开启标志
			final List<IRecord> ll = new LinkedList<>();// 结果集列表
			final int n = sub_sqls.size();// 任务数
			final Object aa[] = new Object[n];// 结果集列表
			final Semaphore semaphore = new Semaphore(1 - n);
			final AtomicInteger counter = new AtomicInteger();// 计数器

			sub_sqls.forEach(e -> new Thread(() -> {
				final int i = counter.getAndIncrement();// 线程编号,领取一个任务号,然后执行
				final String stmt = sub_sqls.get(i);// 获得待执行的sql语句
				Thread.currentThread().setName("#bigquery-" + i);
				final long begTime = System.currentTimeMillis();
				System.out.println("sql 语句验证:[" + i + "]" + e.equals(stmt) + "\ne:" + e + "\nstmt:" + stmt + "\n");
				final List<IRecord> recs = this.sql2records(stmt);
				if (System.currentTimeMillis() < 0) { // 关闭该条 选择支路
					synchronized (ll) {
						ll.addAll(recs);
					}
				} else {
					aa[i] = recs;
				}

				long endTime = System.currentTimeMillis();
				System.out.println("" + Thread.currentThread().getName() + " last for " + (endTime - begTime) + "ms");
				semaphore.release();
			}).start());

			try {
				semaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return ll.size() > 0 //
					? ll
					: Stream.of(aa).map(e -> (List<IRecord>) e).flatMap(Collection::stream)
							.collect(Collectors.toList());
		} else {
			return sub_sqls.parallelStream()// 并行各个子任务
					.flatMap(e -> this.sql2records(e).stream())// 分组获取数据集
					.collect(Collectors.toList());// 采用jdk的流式实现
		} // if
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IColumn<?>> sql2ll(final String sql) {

		return Arrays.asList(sql2cols(sql, this.getConnection(), true));
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @return 执行结果集合的 IRecord 列表
	 */
	public IColumn<?>[] sql2cols(final String sql) {

		return sql2cols(sql, this.getConnection(), true);
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql   查询语句
	 * @param con   數據庫連接
	 * @param close 是否關閉連接
	 * @return 执行结果集合的 IRecord 列表
	 */
	public IColumn<?>[] sql2cols(final String sql, final Connection con, final boolean close) {

		return psql2apply(sql, null, con, close, SQL_MODE.QUERY, (conn, stmt, rs, rsm, n) -> {
			final Column<?>[] finalcc = new Column<?>[n];// 初始化空間
			try {
				// 获得列名
				final Function<Integer, KVPair<String, Integer>> colname = (i) -> {
					String name = null;
					try {
						name = rsm.getColumnLabel(i);
					} catch (Exception ignored) {
					}
					return new KVPair<>(name, i);
				};

				// 构造表头
				Stream.iterate(1, i -> i + 1).limit(n).map(colname).forEach((kv) -> {
					final Column<?> c = new Column<>(kv.key());
					finalcc[kv.value() - 1] = c;
				});

				// 遍歷數據
				while (rs.next()) {
					for (int i = 0; i < n; i++) {
						final Object obj = rs.getObject(i + 1);
						if (finalcc[i].getType() == null && obj != null) {
							finalcc[i].setType(obj.getClass());
						}
						finalcc[i].addObject(obj);
					} // for
				} // while

			} catch (Exception e) {
				e.printStackTrace();
			}
			return finalcc;
		});
	}

	/**
	 * 查询结果集合 <br>
	 * 连接使用完后自动关闭
	 *
	 * @param sql 查询语句
	 * @return 单条数据的结果集
	 */
	public Optional<IRecord> sql2maybe(final String sql) {

		final List<IRecord> recs = this.sql2records(sql);

		if (recs.size() > 1) {
			System.out.println(sql + "\n返回多条(" + recs.size() + ")数据，仅截取第一条返回！");
		}

		return recs.stream().findFirst();
	}

	/**
	 * 查询结果集合 连接使用完后自动关闭
	 *
	 * @param sql 查询语句
	 * @return 单条数据的结果集
	 */
	public <T> Optional<T> sql2maybe(final String sql, final Class<T> tclazz) {

		return sql2maybe(sql).map(e -> IRecord.rec2obj(e, tclazz));
	}

	/**
	 * 查询结果集合 连接使用完后自动关闭
	 *
	 * @param sql    查询语句
	 * @param tclazz 目标结果类型
	 * @return 单条数据的结果集
	 */
	public <T> T sql2get(final String sql, final Class<T> tclazz) {

		return sql2maybe(sql).map(e -> IRecord.rec2obj(e, tclazz)).get();
	}

	/**
	 * 查询结果集合 连接使用完后自动关闭
	 *
	 * @param sql  查询语句
	 * @param mode sql 模式查询还是更新
	 * @return 单条数据的结果集
	 */
	public Optional<IRecord> sql2maybe(final String sql, final Jdbc.SQL_MODE mode) {

		final List<IRecord> recs = this.sql2records(sql, mode);
		if (recs.size() > 1) {
			System.out.println(sql + "\n返回多条(" + recs.size() + ")数据，仅截取第一条返回！");
		}

		return recs.stream().findFirst();
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * 连接使用完后自动关闭
	 *
	 * @param sql 查询语句
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql) {

		return this.psql2records(sql, null, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * 对于不短路的流，在数据库连接使用完后会自动关闭。 对于短路使用的流，数据库连接使用完毕后并不会自动关闭。
	 *
	 * @param sql 查询语句
	 * @return 执行结果集合的 IRecord 流
	 */
	public Stream<IRecord> sql2recordS(final String sql) {

		return this.psql2recordS(sql, (Map<Integer, Object>) null);
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql  查询语句
	 * @param mode 结果获取模式:QUERY,UPDATE
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql, final Jdbc.SQL_MODE mode) {

		return this.psql2records(sql, null, this.getConnection(), true, mode, null, Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel,连接不予关闭
	 *
	 * @param sql  查询语句
	 * @param mode 结果获取模式，QUERY，UPDATE
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql, final Connection conn, final Jdbc.SQL_MODE mode) {

		if (conn == null) {
			return new LinkedList<>();
		}

		return this.psql2records(sql, null, conn, false, mode, null, Optional.empty());
	}

	/**
	 * 带有异常抛出 的 结果集查询<br>
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * 连接使用完后自动关闭 <br>
	 *
	 * @param sql 查询语句
	 * @return 执行结果集合的 IRecord 列表
	 * @throws SQLException
	 */
	public List<IRecord> sql2records_throws(final String sql) throws SQLException {

		return this.psql2records_throws(sql, null, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回经过变换之后的数据记录
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql    查询语句
	 * @param mapper 结果变换函数 IRecord-&gt;T
	 * @return 执行结果集合的List&lt;T&gt;
	 */
	public <T> List<T> sqlmutate(final String sql, final Function<IRecord, T> mapper) {

		return this.sql2recordS(sql).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql      查询语句
	 * @param jsn2keys json keys
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql, final Map<String, String[]> jsn2keys) {

		return this.psql2records(sql, null, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.of(jsn2keys));
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql         查询语句
	 * @param jsn2keysrec json keys 的键值集合
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql, final IRecord jsn2keysrec) {

		return this.sql2records(sql, null, jsn2keysrec);
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql         查询语句
	 * @param jsn2keysrec json keys 的键值集合
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql, final Map<Integer, Object> params, final IRecord jsn2keysrec) {

		final Map<String, String[]> jsn2keys = new HashMap<>();

		jsn2keysrec.tupleS().forEach(kvp -> {
			Object obj = kvp._2();
			String[] oo = null;
			if (obj instanceof String) {
				oo = (obj + "").split("[,]+");
			} else if (obj.getClass().isArray()) {
				try {
					oo = (String[]) obj;
				} catch (Exception e) {
					return;
				} // 转换异常忽略该项目
			} else {
				return;// 忽略该项目
			}
			jsn2keys.put(kvp.key(), oo);
		});

		return this.psql2records(sql, params, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.of(jsn2keys));
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql   查询语句
	 * @param close 是否關閉連接
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> sql2records(final String sql, final boolean close) {

		return this.psql2records(sql, null, this.getConnection(), close, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @param rec prepared statement 的位置参数 integer-&gt;value ,integer 从1开始
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final IRecord rec) {

		final Map<Integer, Object> params = new HashMap<>();

		rec.foreach((k, v) -> {// 位置解析
			Integer key = Integer.parseInt(k);
			params.put(key, v);
		});// 数据转换

		return this.psql2records(sql, params, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql 查询语句
	 * @param rec prepared statement 的位置参数 integer-&gt;value ,integer 从1开始
	 * @return 执行结果集合的 IRecord 流
	 */
	public Stream<IRecord> psql2recordS(final String sql, final IRecord rec) {

		final Map<Integer, Object> params = new HashMap<>();
		rec.foreach((k, v) -> {// 位置解析
			Integer key = Integer.parseInt(k);
			params.put(key, v);
		});// 数据转换

		return this.psql2recordS(sql, params);
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @param oo  prepared statement 的位置参数 序列
	 * @return 执行结果集合的 IRecord 列表
	 */
	public Optional<IRecord> psql2maybe(final String sql, final Object[] oo) {

		final var stream = this.psql2recordS(sql, oo);
		final var ret = stream.findFirst();
		this.clear(stream);

		return ret;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql    查询语句
	 * @param params prepared statement 的位置参数 integer-&gt;value ,integer 从1开始
	 *               参数序号从1开始。1-&lt;xxx,2-&lt;yyy.
	 * @return 执行结果集合的 IRecord 列表
	 */
	public Optional<IRecord> psql2maybe(final String sql, final IRecord params) {

		final var stream = this.psql2recordS(sql, params);
		final var ret = stream.findFirst();
		this.clear(stream);

		return ret;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql    查询语句
	 * @param params prepared statement 的位置参数 integer-&gt;value ,integer 从1开始
	 * @return 执行结果集合的 IRecord 列表
	 */
	public Optional<IRecord> psql2maybe(final String sql, final Map<String, Object> params) {

		final var stream = this.psql2recordS(sql, IRecord.REC(params));
		final var ret = stream.findFirst();
		this.clear(stream);

		return ret;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 * 
	 * @param sql         查询语句
	 * @param params      prepared statement 的位置参数 integer-&gt;value ,integer 从1开始
	 * @param targetClass 期待的结果类型的class
	 * @param <T>         期待的结果类型的
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> Optional<T> psql2maybe(final String sql, final Map<String, Object> params, final Class<T> targetClass) {

		final var stream = this.psql2recordS(sql, IRecord.REC(params));
		final var opt = stream.findFirst().map(e -> IRecord.rec2obj(e, targetClass));
		this.clear(stream);

		return opt;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 * 
	 * @param sql         查询语句
	 * @param oo          prepared statement 的位置参数数组
	 * @param targetClass 期待的结果类型的class
	 * @param <T>         期待的结果类型的
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> Optional<T> psql2maybe(final String sql, final Object[] oo, final Class<T> targetClass) {

		final var stream = this.psql2recordS(sql, oo);
		final var opt = stream.findFirst().map(e -> IRecord.rec2obj(e, targetClass));
		this.clear(stream);

		return opt;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 * 
	 * @param sql 查询语句
	 * @param oo  prepared statement 的位置参数 序列
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2get(final String sql, final Object[] oo, final Class<T> targetClass) {

		final var stream = psql2recordS(sql, oo);
		final var ret = stream.findFirst().map(e -> IRecord.rec2obj(e, targetClass)).get();
		this.clear(stream);

		return ret;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql    查询语句
	 * @param params prepared statement的位置参数 序列: 参数序号从1开始。1-&lt;xxx,2-&lt;yyy.
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> T psql2get(String sql, IRecord params, Class<T> targetClass) {

		final var stream = psql2recordS(sql, params);
		final var ret = stream.findFirst().map(e -> IRecord.rec2obj(e, targetClass)).get();
		this.clear(stream);

		return ret;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @param oo  参数列表
	 * @return 执行结果集合的 IRecord 列表
	 */
	public <T> List<IRecord> psql2records(String sql, List<T> oo) {

		return psql2records(sql, oo.toArray());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql 查询语句
	 * @param oo  参数数组
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Object[] oo) {

		final Map<Integer, Object> params = new HashMap<>();

		if (oo != null) {
			for (int i = 0; i < oo.length; i++) {
				params.put(i + 1, oo[i]);
			} // for
		}

		return this.psql2records(sql, params, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql 查询语句
	 * @param oo  参数数组，从前想后依次类推的对应于 sql 中的 ?
	 * @return 执行结果集合的 IRecord 流
	 */
	public Stream<IRecord> psql2recordS(final String sql, final Object[] oo) {

		final Map<Integer, Object> params = new HashMap<>();

		if (oo != null) {
			for (int i = 0; i < oo.length; i++) {
				params.put(i + 1, oo[i]);
			} // for
		} // if

		return this.psql2recordS(sql, params);
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql    查询语句
	 * @param params sql中的占位符多对应的实际数据，即占位符参数
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params) {

		return this.psql2records(sql, params, this.getConnection(), true, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * 
	 * @param sql    查询语句
	 * @param params sql中的占位符多对应的实际数据，即占位符参数
	 * @return 执行结果集合的 IRecord 流
	 */
	public Stream<IRecord> psql2recordS(final String sql, final Map<Integer, Object> params) {

		Stream<IRecord> stream = null;

		try {
			stream = this.psql2recordS_throws(sql, params);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return stream;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel <br>
	 * Jdbc 的 psql2recordS_throws 的实现 <br>
	 * 
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * 
	 * @param sql    查询语句
	 * @param params sql中的占位符多对应的实际数据，即占位符参数
	 * @return 执行结果集合的 IRecord 流
	 * @throws SQLException
	 */
	public Stream<IRecord> psql2recordS_throws(final String sql, final Map<Integer, Object> params)
			throws SQLException {

		// 需要注意这里的 close_conn 为 true
		final Stream<IRecord> stream = IJdbcSession.psql2recordS(this.getConnection(), sql, params, false, true); // 建议在关闭连接
		this.add(stream);// 托管流

		return stream;
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql         查询语句
	 * @param recSupplier 行记录的生成器 即提供一种自定义的结果集的行实现
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params,
			final Supplier<IRecord> recSupplier) {

		return this.psql2records(sql, params, this.getConnection(), true, SQL_MODE.QUERY, recSupplier,
				Optional.empty());
	}

	/**
	 * 返回的结果集：就是一个中以key值为列名：ColumnLabel
	 *
	 * @param sql         查询语句
	 * @param close       是否關閉連接
	 * @param recSupplier 行记录的生成器 即提供一种自定义的结果集的行实现
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params, final boolean close,
			final Supplier<IRecord> recSupplier) {

		return this.psql2records(sql, params, this.getConnection(), close, SQL_MODE.QUERY, recSupplier,
				Optional.empty());
	}

	/**
	 * 返回的结果集(不会返回空值,null,失败也返回长度为0的list）
	 *
	 * @param sql   查询语句
	 * @param conn  數據庫連接
	 * @param close 是否關閉連接
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params, final Connection conn,
			final boolean close) {

		return this.psql2records(sql, params, conn, close, SQL_MODE.QUERY, null, Optional.empty());
	}

	/**
	 * 返回的结果集(不会返回空值,null,失败也返回长度为0的list）
	 *
	 * @param sql   查询语句
	 * @param con   數據庫連接
	 * @param close 是否關閉連接
	 * @param mode  结果获取模式 QUERY or UDATE
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params, Connection con,
			boolean close, Jdbc.SQL_MODE mode) {

		return this.psql2records(sql, params, con, close, mode, null, Optional.empty());
	}

	/**
	 * 返回的结果集(不会返回空值,null,失败也返回长度为0的list）
	 *
	 * @param sql         查询语句
	 * @param close       是否關閉連接
	 * @param recSupplier 行记录的数据格式
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params, final Connection con,
			final boolean close, final Supplier<IRecord> recSupplier) {

		return this.psql2records(sql, params, con, close, SQL_MODE.QUERY, recSupplier, Optional.empty());
	}

	@Override
	public Set<Stream<?>> getActiveStreams() {

		return this.activeStreams.getActiveStreams();
	}

	/**
	 * 把一个对象数组转换成一个字符串数组。
	 *
	 * @param objs 对象二维数据
	 * @return 字符串二维数组
	 */
	public static String[][] casts(final Object[][] objs) {

		final int height = objs.length;
		final int width = objs[0].length;
		final String[][] strs = new String[height][];

		for (int i = 0; i < height; i++) {
			strs[i] = new String[width];
			for (int j = 0; j < width; j++) {
				strs[i][j] = String.valueOf(objs[i][j]);
			} // for
		} // for

		return strs;
	}

	/**
	 * 把标签数组调整成长度为n
	 * 
	 * @param n    目标长度
	 * @param lbls 标签数组
	 * @return 把标签数组调整成长度为n
	 */
	public static String[] adjustLabels(final int n, final String[] lbls) throws SQLException {

		var labels = lbls == null ? new String[] { "" } : lbls;
		final var sz = labels == null ? 0 : labels.length; // labels的长度
		if (sz < n) {
			labels = new String[n];
			for (int i = 0; i < n; i++) {
				final var j = i / sz; // 循环次数:即周期号
				final var k = i % sz; // 周期内偏移量
				labels[i] = j > 0 // 判断周期号是否大于等于1,超过1则自动追加一个周期号的后缀
						? lbls[k] + "_" + j // 生成重复键名的后缀
						: lbls[k]; // 提取键名
			} // for
			return labels;
		} else {
			return labels;
		}
	}

	/**
	 * 提取列标签:数组类型的返回结果
	 *
	 * @param rs 结果集合
	 * @return 标签数组
	 * @throws SQLException
	 */
	public static String[] labels2(final ResultSet rs) throws SQLException {

		String[] aa = null;
		if (rs == null) {
			return null;
		}

		final var rsm = rs.getMetaData();
		final var n = rsm.getColumnCount();
		aa = new String[n];
		for (int i = 1; i <= n; i++) {
			aa[i - 1] = rsm.getColumnLabel(i);
		}

		return aa;
	}

	/**
	 * 提取列标签:key 从1开始
	 *
	 * @param rs 结果集合
	 * @return 标签的值信息(位置从1开始 ） { 位置 ： Integer - > 标签名 ： String }
	 */
	public static Map<Integer, String> labels(final ResultSet rs) {

		final var mm = new HashMap<Integer, String>();
		try {
			if (rs == null) {
				return mm;
			}

			final var rsm = rs.getMetaData();
			final var n = rsm.getColumnCount();
			for (int i = 1; i <= n; i++) {
				mm.put(i, rsm.getColumnLabel(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mm;
	}

	/**
	 * 生成sql语句．
	 *
	 * @param conn   数据库连接
	 * @param sql    语句
	 * @param params sql 中的占位符参数
	 * @return PreparedStatement
	 * @throws SQLException
	 */
	public static PreparedStatement pudt_stmt(final Connection conn, final String sql,
			final Map<Integer, Object> params) throws SQLException {

		return pstmt(conn, SQL_MODE.UPDATE, sql, params);
	}

	/**
	 * 生成sql语句．
	 *
	 * @param conn 数据库连接
	 * @param sql  语句
	 * @return PreparedStatement
	 * @throws SQLException
	 */
	public static PreparedStatement pudt_stmt(final Connection conn, final String sql) throws SQLException {

		return pstmt(conn, SQL_MODE.UPDATE, sql, null);
	}

	/**
	 * 生成sql语句．
	 *
	 * @param conn   数据库连接
	 * @param mode   语句的类型 UPDATE ,QUERY_SCROLL
	 * @param sql    语句：含有占位符
	 * @param params sql 参数: {Key:Integer-Value:Object},Key 从1开始。 params
	 *               为null时候不予进行sql语句填充
	 * @return PreparedStatement
	 * @throws SQLException
	 */
	public static PreparedStatement pstmt(final Connection conn, final Jdbc.SQL_MODE mode, final String sql,
			final Map<Integer, ?> params) throws SQLException {

		PreparedStatement ps = null;
		if (mode == SQL_MODE.UPDATE) {
			ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);// 生成数据主键
		} else if (mode == SQL_MODE.QUERY_SCROLL) {
			ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		} else {
			ps = conn.prepareStatement(sql);
		}

		final var pm = ps.getParameterMetaData();// 参数元数据
		try {
			final var pcnt = pm.getParameterCount(); // paramcnt;
			if (pcnt > 0 && params != null && params.size() > 0) {
				for (final var paramIndex : params.keySet()) {
					if (pcnt < paramIndex) {
						continue;// 对于超出sql的参数位置范围的 参数给予舍弃
					} // if
					final var value = params.get(paramIndex);
					ps.setObject(paramIndex, value);// 设置参数
				} // for
			} // if
		} catch (UnsupportedOperationException e) {
			if (params != null && params.size() > 0) {
				for (final var paramIndex : params.keySet()) {
					final var value = params.get(paramIndex);
					ps.setObject(paramIndex, value);// 设置参数
				} // for
			} // if
		} // try

		return ps;
	}// pstmt

	/**
	 * 返回的结果集(不会返回空值,null,失败也返回长度为0的list）
	 *
	 * @param sql         查询语句
	 * @param params      sql语句中的位置参数
	 * @param con         數據庫連接
	 * @param close       是否關閉連接
	 * @param mode        sql语句的执行模式查询还是更新
	 * @param recSupplier 行记录的数据格式
	 * @param jsncol2keys 把json列按照指定的序列结构给予展开，即一jsn列表换成多列(keys) jsncol2keys是一个
	 *                    {json列名->展开序列keys}的结构的Map
	 * @return 执行结果集合的 IRecord 列表
	 */
	public List<IRecord> psql2records(final String sql, final Map<Integer, Object> params, final Connection con,
			boolean close, Jdbc.SQL_MODE mode, final Supplier<IRecord> recSupplier,
			Optional<Map<String, String[]>> jsncol2keys) {

		List<IRecord> ll = null;
		try {
			ll = this.psql2records_throws(sql, params, con, close, mode, recSupplier, jsncol2keys);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ll;
	}

	/**
	 * 返回的结果集(不会返回空值,null,失败也返回长度为0的list） 该方法依赖于：psql2apply_throws
	 * 
	 * @param sql         查询语句
	 * @param params      语句参数
	 * @param con         數據庫連接
	 * @param close       是否關閉連接
	 * @param mode        sql语句的执行模式查询还是更新
	 * @param recSupplier 行记录的数据格式
	 * @param jsncol2keys 把json列按照指定的序列结构给予展开，即一jsn列表换成多列(keys) _jsncol2keys是一个
	 *                    {json列名->展开序列keys}的结构的Map :
	 *                    比如｛product->[id,name,description]｝
	 *
	 * @return 执行结果集合的 IRecord 列表,返回结果不为null,可以是一个没有数据的List
	 */
	public List<IRecord> psql2records_throws(final String sql, final Map<Integer, Object> params, final Connection con,
			final boolean close, final Jdbc.SQL_MODE mode, final Supplier<IRecord> recSupplier,
			final Optional<Map<String, String[]>> jsncol2keys) throws SQLException {

		// System.out.println(sql);
		final Optional<Map<String, String[]>> _jsncol2keys = (!jsncol2keys.isPresent()) //
				? Optional.empty()
				: jsncol2keys;// 保证jsncol2keys结构非空
		final Optional<Map<String, String[]>> finaljsncol2keys = _jsncol2keys == null //
				? Optional.empty() //
				: _jsncol2keys;
		final IRecord proto = new LinkedRecord(); // 采用原型法进行记录创建
		final IRecord finalProto = proto;// 转换成 final 类型，java的lambda的要求，这个有点恶心，哈哈！
		final Supplier<IRecord> recsup = (null == recSupplier) ? finalProto::duplicate : recSupplier;// 构造默认的记录结构生成器

		// 烹饪 数据的方法，老子道德经曰：治大国，若烹小鲜, Json值字段的记录生成器
		final IRecordCooker jsnCooker = new IRecordCooker() {
			int n;
			ResultSet rs;
			ResultSetMetaData rsm; // n:结果集合的列数量;rs:查询的结果集,rsm:结果集合的字段描述
			ArrayList<String> labels; // 列的显示名(label),name 是实际名
			ArrayList<String[]> jks; // json 值的列名集合:展开字段序列，索引从0开始，0,1,2 依次对应 第一，第二，第三等．

			// 把rec(含有复合字段：jsn的多key字段）cook(转换)成 扁平的结构
			SQLExceptionalFunction<IRecord, IRecord> lambda_cook = rec -> {// 默认为需要 展开 含有 展开列名 即jks不是全部为null,含有json值列名集合
				for (int i = 0; i < n; i++) {// 逐列添加数据
					final var label = labels.get(i);// 列名
					final String[] seqkeys = jks.get(i);// 对应于第i列（从０开始）的json keys,seqkeys表述key的序列，受到scala影响的命名．
					Object value = rs.getObject(i + 1);// 列值
					if (seqkeys != null) {// 需要对jsn字段给予展开,剪开的键名序列不为空
						if (value == null) {
							value = "{}";// 空对象，保证value有效
						}

						@SuppressWarnings("unchecked")
						Map<String, Object> jsnmap = Json.json2obj(value, Map.class);// json 展开成关联数组 Map
						if (jsnmap == null) {// 默认值的字段填充
							jsnmap = new HashMap<>();
							for (var key : seqkeys) {
								rec.add(key, null);// 为了保证key名称存在，不过对于Map结构的rec，这是无效的．
							} // for
						} // if jsnmap==null

						// 提取jsnmap中的seqkeys中的键名数据，并把他们置如结果记录里面去．
						new LinkedRecord(seqkeys, jsnmap).forEach2(rec::add);
					} else {// 不需要对jsn列进行展开
						rec.add(label, value);
					} // if 逐列添加数据
				} /* for */

				return rec;
			}; // lambda_cook

			// n:结果集合的列数量;rs:查询的结果集,rsm:结果集合的字段描述
			public void initialize(int n, ResultSet rs, ResultSetMetaData rsm) {
				this.n = n;
				this.rs = rs;
				this.rsm = rsm;
				labels = new ArrayList<>(n);// 便签集合
				jks = new ArrayList<>(n);// json key 集合
				for (int i = 0; i < n; i++) {
					String label = null;
					try {
						label = this.rsm.getColumnLabel(i + 1);
					} catch (Exception ignored) {
					}
					labels.add(label);
					String[] cc = finaljsncol2keys.isPresent() ? finaljsncol2keys.get().get(label) : null;// 展开json中的复合列
					jks.add((cc != null && cc.length > 0) ? cc : null); // 展开键值序列：cc就是展开之后扁平列名集合
				} // for i

				// 根据是否含有json字段判断是否开启
				if (finaljsncol2keys.isEmpty()) {
					lambda_cook = rec -> {
						for (int i = 0; i < n; i++) {
							rec.add(labels.get(i), rs.getObject(i + 1));
						} // for
						return rec;
					};
				} // if
			}// initialize

			// 把rec(含有复合字段：jsn的多key字段）cook(转换成)扁平的结构
			public IRecord cook(IRecord rec) throws SQLException {// 数据烹饪方法：把一个空白记录，赋予内容
				return lambda_cook.apply(rec);
			};// cooklambda_cook
		};// jsnCooker

		// 简单记录生成器:这个记录生成器已经随着JsonCooker lamba_cook变得意义不大了．但我还是用他，因为简单．
		final IRecordCooker simpleCooker = new IRecordCooker() {
			int n;
			ResultSet rs;
			ResultSetMetaData rsm;

			public void initialize(int n, ResultSet rs, ResultSetMetaData rsm) {// cooker 的基本准备
				this.n = n;
				this.rs = rs;
				this.rsm = rsm;
			}

			public IRecord cook(IRecord rec) throws SQLException {// 数据烹饪方法：把一个空白记录，赋予内容
				for (int i = 0; i < n; i++) {
					rec.add(rsm.getColumnLabel(i + 1), rs.getObject(i + 1));
				} // for
				return rec;
			}// cook
		};// simpleCooker

		final IRecordCooker cooker = _jsncol2keys.isPresent() ? jsnCooker : simpleCooker;// 选择一个合适的厨师
		// final RecordCooker cooker = jsnCooker;

		// 代码在这里才是真正的开始，以上都是准备工作．准备活动要有条不紊，可以慢（此处的慢是缜密与完备）但不能乱．行动之时要快如闪电．
		final List<IRecord> ll = psql2apply_throws(sql, params, con, close, mode, (conn, stmt, rs, rsm, n) -> { // 结果集合的生成．
			final var recs = new LinkedList<IRecord>();
			cooker.initialize(n, rs, rsm);// 在烹饪之前准备一下，cooker 准备。。。 Go! Go!! Go!!!
			while (rs.next()) {
				recs.add(cooker.cook(recsup.get()));// 數據遍历,把rec煮熟一下再放入结果集recs中
			} // while
			return recs;
		});// sqlquery 查詢結果集

		return ll == null ? new LinkedList<>() : ll;// 返回结果不为null
	}

	/**
	 * 请求模式
	 */
	public enum SQL_MODE {
		QUERY, QUERY_SCROLL, UPDATE
	}

	protected IManagedStreams activeStreams = new ManagedStreams(); // 活动的流
	protected Supplier<Connection> supplierConn = null;// 数据连接贩卖商
	public static boolean debug = false;// 调试标记
}