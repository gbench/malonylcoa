package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.sandbox.data.h2.H2db.hand_close;
import static gbench.util.array.INdarray.ndclc;
import static gbench.util.data.MyDataApp.ds;
import static gbench.util.io.Output.println;

import gbench.util.data.DataApp.IRecord;
import gbench.util.data.MyDataApp;

/**
 * MySQL 的数据库使用示例
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

	/**
	 * 自动触发
	 */
	@Test
	public void quz() {
		MyDataApp.debug = System.out::println;
		new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
			final var pd = sess.sql2pdS("select 11 a, 12 b union select 21 a, 22 b");
			final var nd = pd._2.collect(ndclc()).cuts(pd._1.length).collect(ndclc());// 二维化
			println(pd._1);
			println(nd.nx(1));
		});
	}

	/**
	 * 手动触发
	 */
	@Test
	public void qux() {
		MyDataApp.debug = System.out::println;
		new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
			final var pd = sess.sql2pdS("select 11 a, 12 b union select 21 a, 22 b", hand_close); // 手动触发
			final var nd = pd._2.limit(1 * 2).collect(ndclc()).cuts(pd._1.length).collect(ndclc());// 提取一行,构造短路流。
			pd._2.close(); // 对于短路流需要使用手动触发close回调.不然结果集rs和语句stmt会一直处于打开状态直到会话被关闭。
			println(pd._1);
			println(nd.nx(1));
		});
	};

	/**
	 * 
	 */
	final IRecord mysql_rec = IRecord.REC( //
			"url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", //
			"driver", "com.mysql.cj.jdbc.Driver", //
			"user", "root", "password", "123456");
}