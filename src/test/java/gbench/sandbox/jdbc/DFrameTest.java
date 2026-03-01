package gbench.sandbox.jdbc;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.ctsql;
import static gbench.util.jdbc.sql.SQL.insql;
import static gbench.util.lisp.Lisp.A;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.DFrames;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.lisp.Lisp;

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
		final var h2_rec = IRecord.REC("driver", "org.h2.Driver", "user", "root", "password", "123456", "url",
				String.format("jdbc:h2:mem:%s2;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", "malonylcoa"));
		final var jdbcApp = IJdbcApp.newNsppDBInstance(null, IMySQL.class, h2_rec); // 数据库应用客户端

		final var xs = A(1, 2, 3);
		final var rb = IRecord.rb("a,b,c");
		final var dfm = Lisp.cph(xs, xs, xs).map(rb::get).collect(DFrame.dfmclc);
		final var proto = dfm.head();

		jdbcApp.withTransaction(sess -> {
			final var tbl = "t_cph";
			for (final var sql : Arrays.asList(ctsql(tbl, proto), insql(tbl, dfm.rows()))) {
				println(sql);
				sess.sqlexecute(sql);
			}
			final var sqldframe = DFrames.sqldframeGen2.apply(sess);
			final var shmfile = "a/b/mpg";
			sqldframe.andThen(DFrames.df2shmGen.apply(shmfile)) //
					.andThen(chanbuf -> {
						println(chanbuf.getName());
						chanbuf.close();
						return null;
					}).apply("select * from %s".formatted(tbl));
		});
	}

}
