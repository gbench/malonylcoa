package gbench.sandbox.jdbc;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.ctsql;
import static gbench.util.jdbc.sql.SQL.insql;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.jdbc.kvp.DFrames.STR2BOOL_EFN;
import static gbench.util.jdbc.kvp.DFrames.DFM2DFM_EFN;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import gbench.global.Globals;
import gbench.util.array.SharedMem;
import gbench.util.array.SharedMem.Schema.ChanBuff;
import gbench.util.io.Output;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IJdbcSession;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.function.ExceptionalConsumer;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.DFrames;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.lisp.Lisp;
import gbench.util.type.Times;

/**
 * 数据框测试
 */
public class DFrameTest {

	@Test
	public void foo() {

		final var abc_rb = rb("a,b,c");
		final var dfm = nats(9).cuts(3).collect(DFrame.dfmclc(abc_rb::get));
		println(dfm);
		println(dfm.datas());
		println(dfm.dataS(1).toArray());
		println(dfm.summary(1));
		println(dfm.summary(2));
		println(dfm.summary());
		println(dfm.min("b"), dfm.max("b"), dfm.count("b"), dfm.avg("b"));
		println(dfm.min(1), dfm.max(1), dfm.count(1), dfm.avg(1));

	}

	@Test
	public void bar() {

		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
		final var xs = A(1, 2, 3);
		final var rb = IRecord.rb("a,b,c");
		final var dfm = Lisp.cph(xs, xs, xs).map(rb::get).collect(DFrame.dfmclc); // 生成向量笛卡尔集生成数据集
		final var proto = dfm.head(); // 模板数据

		jdbcApp.withTransaction(sess -> {
			final var tbl = "t_cph";
			for (final var sql : Arrays.asList(ctsql(tbl, proto), insql(tbl, dfm.rows()))) { // 生成DML SQL语句
				sess.sqlexecute(println(sql));
			}

			final String shmfile = null; // 临时文件
			final var cbs = new ChanBuff[1]; // 单值容器！
			final var sqlpipeline = DFrames.sqldframeGen2.apply(sess) //
					.andThen(DFrames.df2shmGen.apply(shmfile)) // dfm写入共享内存
					.andThen(chanbuf -> {
						cbs[0] = chanbuf;
						println("pathname:%s".formatted(chanbuf));
						return SharedMem.read(chanbuf); // 读取数据文件
					});
			final var cphdfm = sqlpipeline.apply("select * from %s".formatted(tbl));

			println("cph", cphdfm);
			println("cph json", cphdfm.json());
			cbs[0].close(); // 缓存关闭
		});

	}

	@Test
	public void qux() {

		final var jdbcMy = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, mysql_rec); // MySQL 数据库应用客户端
		final var jdbcH2 = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // H2 数据库应用客户端

		final var showtbls = "show tables";
		final var sqlexecuteGen = DFrames.sqlfunGen(conn -> (STR2BOOL_EFN) sql -> conn.createStatement().execute(sql))
				.compose((IJdbcSession<?, ?> js) -> js.getConnection());
		final ExceptionalFunction<Integer, DFM2DFM_EFN> head = n -> df -> df.head(n); // 提取前5行
		final var dfm_h10_pipeline = DFrames.sqldframeGen2.andThen(sqldframe -> sqldframe.andThen(head.apply(10))
				.andThen(df -> df.strcolS(0).map(Lisp.rpta(2)).map("select '%s' name , count(*) n from %s"::formatted) //
						.collect(Collectors.joining("\nunion\n"))) // 生成SQL语句
				.andThen(sqldframe));
		final ExceptionalFunction<String, ExceptionalFunction<IJdbcSession<UUID, Object>, String>> print10ln_pipepline = sql -> js -> dfm_h10_pipeline
				.apply(js).andThen(Output::println).apply(sql);

		jdbcMy.withTransaction(sess -> {
			final var print10tbl = print10ln_pipepline.apply(showtbls); // 最多显示10行数据
			final var sqldframe = DFrames.sqldframeGen2.apply(sess);
			sqldframe.andThen(head.apply(5)) // 把数据拷贝到H2数据库（5张表）
					.andThen(dfm -> dfm.strcolS(0).map(e -> Tuple2.of(e, "select * from %s limit 100".formatted(e)))) // 100条数据
					.andThen(ps -> ps.flatMap(p -> {
						final var dfm = sqldframe.noexcept().apply(p._2());
						return Stream.of(ctsql(p._1(), dfm.proto()), insql(p._1(), dfm.rows())); // 生成DML SQL语句
					})).andThen(sqls -> jdbcH2.withTransaction(js -> sqls.map(Output::println) //
							.forEach(sqlexecuteGen.apply(js).noexcept2cs())))
					.apply(showtbls);

			println("\nmysql:");
			print10tbl.apply(sess); // 打印输出
			println("\nh2:");
			jdbcH2.withTransaction(print10tbl.except2cs()); // 打印输出
		});

	}

	@Test
	public void quz() {

		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, mysql_rec); // MySQL 数据库应用客户端
		final var sql = "select '%s' Tbl, concat(REGEXP_REPLACE(ActionDay, '(\\\\d{4})(\\\\d{2})(\\\\d{2})', '$1-$2-$3'), ' ', UpdateTime) TickTime, t.* from %s t limit 10";
		final var flds = "Id,Tbl,LastPrice,TradingDay,TickTime,UpdateMillisec,CxxCtpCreateTime";
		final ExceptionalConsumer<ChanBuff> cbclose = ChanBuff::close; // ChanBuff 关闭回收！
		final Function<List<ChanBuff>, ExceptionalConsumer<DFrame>> shmwriterGen = chanbuffs -> datadfm -> { // 共享缓存读写
			final var dfm = datadfm.addcol( // 时间修正
					"TickTime", datadfm.ldtcol("TickTime"), //
					"CxxCtpCreateTime", datadfm.ldtcol("CxxCtpCreateTime"), //
					"TradingDay", datadfm.column("TradingDay", Times::asLocalDate));
			final var shmname = this.getClass().getName().replace(".", "/").toLowerCase();
			final var shmfile = "E:/slicee/temp/malonylcoa/test/%s/%s".formatted(shmname, dfm.head().str("Tbl"));

			DFrames.df2shmGen.apply(shmfile).andThen(chanbuf -> { // 使用DFrame读写共享缓存:ChanBuffer封装了MappedByteBuffer与FileChannel
				chanbuffs.add(chanbuf); // 登记chanbuf已便结束时可以close关闭回收！
				println("pathname:%s".formatted(chanbuf)); // 打印内存映射文件路径(ChanBuff.toString默认显示pathname即数据文件的绝对路径)
				println("read.shm('%s') |> as_tibble() |> select(%s)".formatted(chanbuf, flds).replace("\\", "/"));
				return SharedMem.read(chanbuf).filtercol(flds); // 读取数据文件
			}).andThen(Output::println).apply(dfm);
		}; // shm_writer

		jdbcApp.withTransaction(sess -> {
			final var chanbuffs = new LinkedList<ChanBuff>();
			final var sqldframe = DFrames.sqldframeGen2.apply(sess);
			final var shmwriter = shmwriterGen.apply(chanbuffs).noexcept();
			sqldframe.andThen(dfm -> dfm.head(10).strcolS(0).map(Lisp.rpta(2)).map(sql::formatted).map(Output::println)
					.map(sqldframe.noexcept())).apply("show tables").forEach(shmwriter);
			chanbuffs.forEach(cbclose.noexcept()); // chanbuffs 批量回收！
		}); // withTransaction

	}

	final String sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql";
	final IRecord h2_rec = REC("driver", "org.h2.Driver", "user", "root", "password", "123456", "url",
			String.format("jdbc:h2:mem:%s2;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", "malonylcoa"));
	final IRecord mysql_rec = REC("url", "jdbc:mysql://127.0.0.1:3371/ctp?serverTimezone=UTC", "driver",
			"com.mysql.cj.jdbc.Driver", "user", "root", "password", "123456");

}
