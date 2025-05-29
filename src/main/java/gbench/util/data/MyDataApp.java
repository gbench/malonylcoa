package gbench.util.data;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import gbench.util.json.MyJson;

/**
 * MyDataApp
 *
 * @author xuqinghua
 */
public class MyDataApp extends DataApp {

	/**
	 * @param ds 数据源
	 */
	public MyDataApp(final DataSource ds) {
		super(ds);
	}

	/**
	 * @param dsrec 数据源配置
	 */
	public MyDataApp(final IRecord dsrec) {
		super(ds(dsrec));
	}

	/**
	 * @param dsrec 数据源配置
	 */
	public MyDataApp(final Map<?, ?> dsrec) {
		super(ds(dsrec));
	}

	/**
	 * 自定义数据源
	 *
	 * @param rec {user,password,url,driver}
	 * @return DataSource
	 */
	public static DataSource ds(final Map<?, ?> rec) {
		return ds(IRecord.REC(rec));
	}

	/**
	 * 自定义数据源
	 *
	 * @param kvs {user,password,url,driver}
	 * @return DataSource
	 */
	public static DataSource ds(Object... kvs) {
		return ds(IRecord.REC(kvs));
	}

	/**
	 * 自定义数据源
	 *
	 * @param rec {user,password,url,driver}
	 * @return DataSource
	 */
	public static DataSource ds(final IRecord rec) {
		final String user = Optional.ofNullable(rec.str("user")).orElse("root");
		final String password = Optional.ofNullable(rec.str("password")).orElse("123456");
		final String url = Optional.ofNullable(rec.str("url")).orElse(
				"jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&useOldAliasMetadataBehavior=true");
		final String driver = Optional.ofNullable(rec.str("driver")).orElse("com.mysql.cj.jdbc.Driver");

		try {
			Class.forName(driver);
		} catch (Exception e) {
			e.printStackTrace();
		}

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
				return this.getConnection(user, password);
			}

			@Override
			public Connection getConnection(final String username, final String password) throws SQLException {
				return DriverManager.getConnection(url, username, password);
			}

			@Override
			public PrintWriter getLogWriter() throws SQLException {
				return null;
			}

			@Override
			public void setLogWriter(final PrintWriter out) throws SQLException {

			}

			@Override
			public void setLoginTimeout(final int seconds) throws SQLException {

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

	/**
	 * 生成表定义
	 *
	 * @param rec 表定义记录: 列名1, 列类型1, 列名2, 列类型2, ...
	 * @return
	 */
	public static List<IRecord> parse_tbldef(final IRecord rec) {
		return Stream.of(rec).flatMap(e -> Stream.of(e, e.fmap1((k, v) -> "COMMENT '" + k + "'")))
				.collect(Collectors.toList());
	}

	/**
	 * 生成表定义
	 *
	 * @param kvs 表定义: 列名1, 列类型1, 列名2, 列类型2, ...
	 * @return
	 */
	@SafeVarargs
	public static <T> List<IRecord> parse_tbldef(final T... kvs) {
		return parse_tbldef(IRecord.REC(kvs));
	}

	/**
	 * ddl 创建sql
	 * <p>
	 * 数据示例: <br>
	 * final var defs = Stream.of(MyDataApp.IRecord.REC("id", "INT",
	 * "name","VARCHAR(64)", "password", "VARCHAR(16)")) .flatMap(e -> Stream.of(e,
	 * e.fmap1((k,v) -> "COMMENT '"+k+"'"))).collect(Collectors.toList());
	 *
	 * @param name    数据表名
	 * @param dfmdefs 数据字段表: [[字段类型],[字段约束],...] 最少两行,第一行 字段类型,第二行字段约束
	 * @return ddl 创建sql
	 */
	public static List<String> createtbl_sql(final String name, final Iterable<IRecord> dfmdefs) {
		final Iterator<IRecord> itr = dfmdefs.iterator();
		final IRecord types = itr.next();
		if (!itr.hasNext())
			return null;
		final IRecord suffix = itr.next();
		final Stream<Tuple2<Tuple2<String, Object>, Object>> triples = types.valueS().map(Tuple2.zipper1(types.keys()))
				.map(Tuple2.zipper2(suffix.valueS().map(e -> (e + "").trim()).map(e -> "-".equals(e) ? "" : e)));
		final String defs = triples.map(e -> e.flatS(Object::toString).collect(Collectors.joining(" ")))
				.collect(Collectors.joining(",\n\t"));

		final String sql0 = IRecord.FT("drop table if exists $0;", name);
		final String sql1 = IRecord.FT("create table $0 (\n\t$1\n) COMMENT '$0';", name, defs);

		return Arrays.asList(sql0, sql1);
	}

	/**
	 * sql 语句流 <br>
	 * 示例:<br>
	 * MyDataApp.create_table("t_user", MyDataApp.IRecord.REC("id", "INT", "name",
	 * "VARCHAR(64)", "password", "VARCHAR(16)"))
	 *
	 * @param name   表名
	 * @param tbldef 表定义元数据: 列1名称,列1的类型,列2名称,列2的类型, ...
	 * @return sql 语句流
	 */
	public static List<String> createtbl_sql(final String name, final IRecord tbldef) {
		return createtbl_sql(name, parse_tbldef(tbldef));
	}

	/**
	 * sql 语句流 <br>
	 * 示例:<br>
	 * MyDataApp.create_table("t_user", "id", "INT", "name", "VARCHAR(64)",
	 * "password", "VARCHAR(16)")
	 *
	 * @param <T>  键值列表元素类型
	 * @param name 表名
	 * @param kvs  表定义元数据: 列1名称,列1的类型,列2名称,列2的类型, ...
	 * @return sql 语句流
	 */
	@SafeVarargs
	public static <T> List<String> createtbl_sql(final String name, final T... kvs) {
		return createtbl_sql(name, parse_tbldef(kvs));
	}

	/**
	 * sql 语句流
	 *
	 * @param name 表名
	 * @param dfm  数据定义表
	 * @return sql 语句流
	 */
	public static Stream<String> createtbl_sqlS(final String name, final DFrame dfm) {
		return createtbl_sql(name, dfm).stream();
	}

	/**
	 * sql 语句流
	 * <p>
	 * 示例:<br>
	 * MyDataApp.create_table("t_user", MyDataApp.IRecord.REC("id", "INT", "name",
	 * "VARCHAR(64)", "password", "VARCHAR(16)"))
	 *
	 * @param name   表名
	 * @param tbldef 表定义元数据: 列1名称,列1的类型,列2名称,列2的类型, ...
	 * @return sql 语句流
	 */
	public static Stream<String> createtbl_sqlS(final String name, final IRecord tbldef) {
		return createtbl_sql(name, tbldef).stream();
	}

	/**
	 * insert SQL 语句
	 *
	 * @param name 表名
	 * @param rec  数据元素
	 * @param flag 是否单引号'进行转义, true 转义,false 不转义
	 * @return insert SQL 语句 流
	 */
	public static String insert_sql(final String name, final Iterable<IRecord> recs, final boolean flag) {
		final List<IRecord> rs = recs instanceof List ? (List<IRecord>) recs
				: StreamSupport.stream(recs.spliterator(), false).toList();

		if (rs.size() > 0) {
			final var rec = rs.get(0);
			final var join = Collectors.joining(", ");
			final String keys = rec.keyS().collect(join);
			final String values = rs.stream()
					.map(r -> r.valueS()
							.map(e -> e == null ? null
									: "'%s'".formatted(!flag ? e
											: (e instanceof Map ? MyJson.toJson(e) : e.toString()).replace("'", "\\'")))
							.collect(join))
					.map("(%s)"::formatted).collect(join);
			return IRecord.FT("insert into $0 ($1) values $2", name, keys, values);
		} else {
			return null;
		}
	}

	/**
	 * insert SQL 语句
	 *
	 * @param name 表名
	 * @param rec  数据元素
	 * @param flag 是否单引号'进行转义, true 转义,false 不转义
	 * @return insert SQL 语句 流
	 */
	public static String insert_sql(final String name, final Iterable<IRecord> recs) {
		return insert_sql(name, recs, true);
	}

	/**
	 * insert SQL 语句( 对单引号'进行转义)
	 *
	 * @param name 表名
	 * @param rec  数据元素
	 * @return insert SQL 语句 流
	 */
	public static String insert_sql(final String name, final IRecord... recs) {
		return insert_sql(name, Arrays.asList(recs));
	}

	/**
	 * insert SQL 语句( 对单引号'进行转义)
	 *
	 * @param <T>  键值列表元素类型
	 * @param name 表名
	 * @param kvs  表定义元数据: 列1名称,列1的类型,列2名称,列2的类型, ...
	 * @return insert SQL 语句 流
	 */
	public static <T> String insert_sql(final String name, final Map<?, ?> kvs) {
		return insert_sql(name, Arrays.asList(IRecord.REC(kvs)), true);
	}

	/**
	 * insert SQL 语句
	 *
	 * @param name 表名
	 * @param recs 数据列表
	 * @return insert SQL 语句
	 */
	public static Stream<String> insert_sqlS(final String name, final Iterable<IRecord> recs) {
		return StreamSupport.stream(recs.spliterator(), false).map(e -> insert_sql(name, e));
	}

	/**
	 * update SQL 语句
	 *
	 * @param name        表名
	 * @param rec         数据元素
	 * @param whereClause where 条件, 键名之间 使用 and 进行连接, (key1=value1 and key2=value2
	 *                    and ...)
	 * @param flag        是否单引号'进行转义, true 转义,false 不转义
	 * @return insert SQL 语句 流
	 */
	public static String update_sql(final String name, final IRecord rec, final IRecord whereClause,
			final boolean flag) {

		final String where_clause = whereClause
				.tupleS().map(
						tup -> IRecord.FT("$0$1", tup._1,
								Optional.ofNullable(tup._2).map(Object::toString).map(e -> e.replace("'", "\\'"))
										.map(e -> "='" + e + "'").orElse(" IS NULL")))
				.collect(Collectors.joining(" and "));

		return update_sql(name, rec, where_clause, flag);
	}

	/**
	 * update SQL 语句 ( 对单引号'进行转义)
	 *
	 * @param name        表名
	 * @param rec         数据元素
	 * @param whereClause where 条件, 键名之间 使用 and 进行连接, (key1=value1 and key2=value2
	 *                    and ...)
	 * @return insert SQL 语句 流
	 */
	public static String update_sql(final String name, final IRecord rec, final IRecord whereClause) {

		final String where_clause = whereClause
				.tupleS().map(
						tup -> IRecord.FT("$0$1", tup._1,
								Optional.ofNullable(tup._2).map(Object::toString).map(e -> e.replace("'", "\\'"))
										.map(e -> "='" + e + "'").orElse(" IS NULL")))
				.collect(Collectors.joining(" and "));

		return update_sql(name, rec, where_clause, true);
	}

	/**
	 * update SQL 语句
	 *
	 * @param name         表名
	 * @param rec          数据元素
	 * @param where_clause where 条件, 键名之间 使用 and 进行连接, (key1=value1 and key2=value2
	 *                     and ...)
	 * @param flag         是否单引号'进行转义, true 转义,false 不转义
	 * @return insert SQL 语句 流
	 */
	public static String update_sql(final String name, final IRecord rec, final String where_clause,
			final boolean flag) {

		final String set_flds = rec.tupleS().map(tup -> IRecord.FT("$0=$1", tup._1, !flag //
				? Optional.ofNullable(tup._2).map(e -> "'" + e + "'").orElse("NULL") //
				: Optional.ofNullable(tup._2).map(Object::toString).map(e -> e.replace("'", "\\'"))
						.map(e -> "'" + e + "'").orElse("NULL") //
		)).collect(Collectors.joining(" , "));

		return IRecord.FT("update $0 set $1 where $2;", name, set_flds, where_clause);
	}

	/**
	 * update SQL 语句
	 *
	 * @param name         表名
	 * @param rec          数据元素
	 * @param where_clause where 条件, 键名之间 使用 and 进行连接, (key1=value1 and key2=value2
	 *                     and ...)
	 * @return insert SQL 语句 流
	 */
	public static String update_sql(final String name, final IRecord rec, final String where_clause) {
		return update_sql(name, rec, where_clause, true);
	}

}
