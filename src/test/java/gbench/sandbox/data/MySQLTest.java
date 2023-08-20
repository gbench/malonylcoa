package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.data.MyDataApp.ds;
import static gbench.util.io.Output.println;

import gbench.util.array.INdarray;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.MyDataApp;

/**
 * 
 */
public class MySQLTest {

	@Test
	public void foo() {
		new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
			final var dfm = sess.sql2x("show tables");
			println(dfm);
		});
	}

	@Test
	public void bar() {
		MyDataApp.debug = System.out::println;
		new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
			final var tableS = sess.sql2recordS("show tables");
			final var opt = tableS.findAny();
			println(opt); // 此时statement,resultSet并没有关闭
			tableS.close(); // 此时resultSet才给予关闭
		});
		// 最后关闭数据库连接
	}

	@Test
	public void quz() {
		MyDataApp.debug = System.out::println;
		new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
			final var pd = sess.sql2dataS("select 11 a, 12 b union select 21 a, 22 b");
			final var nd = INdarray.of(pd._2.toArray()).cuts(pd._1.length).collect(INdarray.ndclc());
			println(nd.nx(1));
		});
		// 最后关闭数据库连接
	}

	/**
	 * 
	 */
	final IRecord mysql_rec = IRecord.REC( //
			"url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", //
			"driver", "com.mysql.cj.jdbc.Driver", //
			"user", "root", "password", "123456");
}