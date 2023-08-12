package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.data.MyDataApp.ds;
import static gbench.util.io.Output.println;

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

	/**
	 * 
	 */
	final IRecord mysql_rec = IRecord.REC( //
			"url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", //
			"driver", "com.mysql.cj.jdbc.Driver", //
			"user", "root", "password", "123456");
}