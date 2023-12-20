package gbench.sandbox.jdbc;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.IJdbcApp.nspeb;
import static gbench.util.jdbc.kvp.DFrame.dfmclc;
import static gbench.util.jdbc.kvp.IRecord.REC;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.Jdbc;
import gbench.util.jdbc.annotation.JdbcConfig;

/**
 * 
 */
public class JdbcTest {

	/**
	 * mysql 数据库操作接口<br>
	 * MysqlDatabase <br>
	 * <br>
	 * mysql> show variables like '%time_zone%' <br>
	 * system_time_zone | China Standard Time <br>
	 * <br>
	 * DST: Daylight Saving Time中文名叫“夏令时”，一般在天亮早的夏季人为将时间调快一小时，<br>
	 * 可以使人早起早睡，减少照明量，以充分利用光照资源，从而节约照明用电。中国1986-1991年实行夏令时，1992年废除。 <br>
	 * GMT-8是东八区，北京时间和东八区一致。 <br>
	 * CST: China Standard Time（老外认为有其他含义，中国就这个缩写），中国标准时。
	 * 中国1986-1991年实行夏令时，1992年废除。<br>
	 * Asia/Shanghai是已地区命名的地区标准时，在中国叫CST。<br>
	 * GMT：Greenwich Mean Time，格林威治标准时，地球每15°经度 被分为一个时区，共分为24个时区，相邻时区相差一小时；例:
	 * 中国北京位于东八区。 <br>
	 * 1992年以后，在中国，GMT+8和Asia/Shanghai是一样的时间，1986-1991之间，夏天会有一小时时差。 <br>
	 * 这就是 为何这里把时区写成Asia/Shanghai而不写成 GMT+8 (GMT%2B8) 的原因，因为 中国在 1986-1991
	 * 使用夏令时。<br>
	 * 时间 Asia/Shanghai '1989-05-02 00:18:00' 在 GMT+8 中 为 '1989-05-02 01:18:00'
	 * 
	 * @author gbench
	 *
	 */
	@JdbcConfig(url = "jdbc:mysql://localhost:3309/erp?serverTimezone=Asia/Shanghai", user = "root", password = "123456")
	interface MySQL extends IMySQL { // 数据接口
	}

	@Test
	public void foo() {
		final var mysql = IJdbcApp.newDBInstance(() -> nspeb("sqls/test.sql", this.getClass()), MySQL.class);
		println("db", mysql.getDbName());
		final var jdbc = mysql.getProxy().findOne(Jdbc.class);
		jdbc.withTransaction(sess -> {
			println(sess.sql2u("show tables", dfmclc));
		});
		println("------------------------------------------------------");
		final var dfm = mysql.sqlqueryS("select * from t_contract limit ##cnt", REC("cnt", 5)).collect(dfmclc);
		println(dfm);
	}

}
