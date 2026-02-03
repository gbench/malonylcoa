package gbench.webapps.mymall.api.model.finance.bkp;

import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.ctsql;
import static gbench.util.jdbc.sql.SQL.insql;
import static gbench.util.lisp.Lisp.aaclc;
import static java.lang.String.format;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

import gbench.util.data.xls.SimpleExcel;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IJdbcSession;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

/**
 * AbstractBkp
 */
public class AbstractBkp {

	/**
	 * AbstractBkp
	 */
	public AbstractBkp() {
		this(format("%s/acct_data.xlsx", datahome), format("%s/bookeeping.sql", datahome), true);
	}

	/**
	 * AbstractBkp
	 * 
	 * @param sqlfile  sqlfile
	 * @param datafile datafile
	 */
	public AbstractBkp(final String sqlfile, final String datafile, final boolean h2_flag) {
		this.sqlfile = Optional.ofNullable(datafile).orElse(format("%s/bookeeping.sql", datahome));
		this.datafile = Optional.ofNullable(sqlfile).orElse(format("%s/acct_data.xlsx", datahome));
		this.excel = SimpleExcel.of(this.datafile);
		this.h2_flag = h2_flag;
		this.initialize(this.sqlfile, this.datafile, this.datafile);
	}

	/**
	 * 字段初始化
	 * 
	 * @param sqlfile  sqlfile
	 * @param datafile datafile
	 * @param h2_flag  h2数据库标记
	 */
	public void initialize(final String sqlfile, final String datafile, final Object h2_flag) {
		final IRecord dbconfig;
		if (h2_flag instanceof Boolean flag) {
			dbconfig = flag ? h2_rec : mysql_rec;
		} else if (h2_flag instanceof IRecord rec) { // 直接指定 driver
			this.h2_flag = Objects.equals(rec.str("driver"), "org.h2.Driver")
					|| rec.stropt("url").map(e -> e.toLowerCase().startsWith("jdbc:h2:")).orElse(false);
			dbconfig = rec;
		} else {
			this.h2_flag = true;
			dbconfig = h2_rec;
		}
		this.jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, dbconfig); // 数据库应用客户端
	}

	/**
	 * 字段初始化
	 * 
	 * @param h2_flag 是否启动h2数据库
	 */
	public void initialize(final Object h2_flag) {
		this.initialize(sqlfile, datafile, h2_flag);
	}

	/**
	 * 分录誊写
	 * 
	 * @param lines 交易记录
	 */
	public void writeTx(final Iterable<IRecord> lines) {
		final var linedfm = DFrame.of(lines);
		final var journal_line = linedfm.head(); // journal 数据
		final var entries = linedfm.tail(); // entries 分录数据

		jdbcApp.withTransaction(sess -> { // 数据写入
			// 表数据
			final var journal_proto = alias_f.apply(journal_line) //
					.apply("path/account,path;comment;date/amount,date"); // 日记账
			final var journal_id = sess.sql2execute2int(insql(jtbl, journal_proto));
			final var date = journal_proto.ldt("date");
			for (final var entry : entries.rows()) { // 科目行数据
				final var entry_proto = alias_f.apply(entry.derive("journal_id", journal_id, "date", date, "drcr",
						// drcr的字符安处理
						switch (entry.stropt("drcr").map(String::toLowerCase).map(String::strip).orElse("-")) {
						case "dr", "1" -> 1;
						case "cr", "-1" -> -1;
						default -> 0;
						})).apply("journal_id;drcr;path/account,account;date/amount,amount;date");
				sess.sql2execute2int(insql(etbl, entry_proto)); // 数据分录
			} // for
		}); // withTransaction
	}

	/**
	 * 日记账的处理
	 * 
	 * @param journaldfm
	 */
	public void handle(final DFrame journaldfm) {
		this.handle(journaldfm.rows(), this::writeTx);
	}

	/**
	 * 日记账的处理
	 * 
	 * @param journaldfm
	 */
	public <RECS extends Iterable<IRecord>> void handle(final RECS journaldfm,
			final Consumer<? super Iterable<IRecord>> action) {
		final var lines = new LinkedList<IRecord>(); // 数据航

		for (final var line : journaldfm) {
			if (lines.size() > 0 && line.stropt(0).map("-"::equals).get()) {
				action.accept(lines);
				lines.clear();
			} // if

			lines.add(line);
		} // forEach

		action.accept(lines);
	}

	/**
	 * 创建数据表
	 * 
	 * @param sess   会话对象
	 * @param keys   键名列表
	 * @param params 函数列表
	 * @throws SQLException
	 */
	public static void createtbl(final IJdbcSession<?, ?> sess, final String tbl, final String keys,
			final Integer... params) throws SQLException {
		final var catalog = sess.sql2maybe("select database()").map(e -> e.str(0)).get();
		final var ctsql = ctsql(tbl, rb(keys).get(Stream.of(params) //
				.map(i -> switch (i) {
				case INT -> 1; // 整数标准值
				case DBL -> 1d; // 浮点数标准值
				case DTM -> LocalDateTime.now(); // 事件标准值
				default -> "-".repeat(i); // 字符串类型
				}).toArray(Object[]::new)));
		// println(ctsql);
		if (!sess.isTablePresent(tbl, null, catalog)) { // 数据表
			// do nothing
		} else { // 删除表
			sess.sqlexecute(format("drop table %s", tbl));
		} // if
		sess.sqlexecute(ctsql);
	}

	/**
	 * IRecord的别名函数 <br>
	 * 注意 name2,name2;name3,name3可以简写为name2;name3 <br>
	 * # rec: 数据对象 <br>
	 * # keys: oldname0,newname0;oldname1,oldname1;name2;name3
	 */
	final static Function<IRecord, Function<String, IRecord>> alias_f = rec -> keys -> { // 别名函数
		return Stream.of(keys.split(";")).flatMap(s -> Optional.ofNullable(s.split(",")) // 别名定义
				.map(as -> Stream.of(0, 1).map(i -> as[i % as.length])).orElse(null)).filter(Objects::nonNull)
				.collect(aaclc(as -> REC(as.toArray()))) // 生成数据对象
				.mutate(r -> rb(r.values(String.class)).get(rec.filter(r.keys().toArray(String[]::new)).values())); // mutate
	}; // 别名函数

	// 数据常量
	public final static int INT = -1;
	public final static int DBL = -2;
	public final static int DTM = -3;

	// 数据表
	public final static String jtbl = "t_journal"; // 记记账
	public final static String etbl = "t_entry"; // 记账分录
	public final static String catalog = "myacct"; // 数据库
	// 数据文件
	public final static String datahome = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/data";

	// 数据库配置
	public final static IRecord h2_rec = REC("url",
			format("jdbc:h2:mem:%s;mode=mysql;db_close_delay=-1;database_to_upper=false", catalog), //
			"driver", "org.h2.Driver", "user", "root", "password", "123456"); // h2数据库
	public final static IRecord mysql_rec = REC("url",
			format("jdbc:mysql://127.0.0.1:3309/%s?serverTimezone=UTC", catalog), //
			"driver", "com.mysql.cj.jdbc.Driver", "user", "root", "password", "123456"); // h2数据库

	// 数据库连接
	protected final String datafile;
	protected final String sqlfile;
	protected final SimpleExcel excel;
	protected boolean h2_flag = false; // 是否是h2数据库
	protected IMySQL jdbcApp;
}
