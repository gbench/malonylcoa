package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import gbench.global.Globals;
import gbench.util.data.xls.DataMatrix;
import gbench.util.io.Output;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.DFrame;

import static gbench.util.array.INdarray.nats;
import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.jdbc.kvp.IRecord.REC;

/**
 * H2数据库示例
 */
public class JdbcIgniteTest {

	/**
	 *  外部启动并初始化(cluster init --name ctp) 数据库（然后增肌数据mtcars)
	 */
	@Test
	public void foo() {
		final var datafile = xls(Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx"); // 数据-源文件
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
		final var db = "ctp"; // 数据库名
		final var url = String.format("jdbc:ignite:thin://localhost:10800", db); // h2连接字符串
		final var h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456"); // h2数据库
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
		jdbcApp.withTransaction(sess -> {
			final var conn = sess.getConnection();
			final var stmt = conn.createStatement();
			final var rs = stmt.executeQuery("select * from mtcars");
			final var rsm = rs.getMetaData();
			final var n = rsm.getColumnCount();
			final var lines = new ArrayList<IRecord>();
			final var rb = IRecord.rb(nats(n).fmap(DataMatrix::xlsn).data());
			while (rs.next()) {
				final var line = new ArrayList<String>(n);
				for (int i = 1; i < n; i++) {
					line.add(rs.getString(i));
				}
				lines.add(rb.get(line));
			}
			final var dfm = DFrame.of(lines);
			stmt.close();
			rs.close();
			Output.println(dfm);
		});

	}

}
