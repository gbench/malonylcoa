package gbench.sandbox.jdbc;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.ctsql;
import static gbench.util.jdbc.sql.SQL.insql;
import static gbench.util.lisp.Lisp.A;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import gbench.util.array.SharedMem;
import gbench.util.array.SharedMem.Schema.ChanBuff;
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
		final var dfm = Lisp.cph(xs, xs, xs).map(rb::get).collect(DFrame.dfmclc); // 生成向量笛卡尔集生成数据集
		final var proto = dfm.head(); // 模板数据

		jdbcApp.withTransaction(sess -> {
			final var tbl = "t_cph";
			for (final var sql : Arrays.asList(ctsql(tbl, proto), insql(tbl, dfm.rows()))) { // 生成DML SQL语句
				sess.sqlexecute(println(sql));
			}

			final String shmfile = null; // 临时文件
			final var cbs = new ChanBuff[1]; // 单值容器！
			final var sqldframe = DFrames.sqldframeGen2.apply(sess);
			final var dfm_pipeline = sqldframe.andThen(DFrames.df2shmGen.apply(shmfile)) // dfm写入共享内存
					.andThen(chanbuf -> {
						cbs[0] = chanbuf;
						println("pathname:%s".formatted(chanbuf));
						return SharedMem.read(chanbuf); // 读取数据文件
					});
			final var cphdfm = dfm_pipeline.apply("select * from %s".formatted(tbl));

			println("cph", cphdfm);
			println("cph json", cphdfm.json());
			cbs[0].close(); // 缓存关闭
		});
	}

}
