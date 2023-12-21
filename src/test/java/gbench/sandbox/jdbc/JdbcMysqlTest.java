package gbench.sandbox.jdbc;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.IJdbcApp.nspp;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.sql;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.annotation.JdbcConfig;
import gbench.util.jdbc.annotation.JdbcExecute;
import gbench.util.jdbc.annotation.JdbcQuery;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

/**
 * 使用 JDK 代理的方式,根据接口的注解@JdbcQuery,@JdbcExecute自动填充对象的SQL语句实现数据库操作
 */
public class JdbcMysqlTest {

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
	@JdbcConfig(url = "jdbc:h2:mem:erp;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", user = "root", password = "123456")
	interface JdbcApp extends IMySQL { // 数据接口

		/**
		 * 使用注解 引用 sql文件中 SQL语句
		 * 
		 * @param cnt 返回的记录数量
		 * @return 返回值
		 */
		@JdbcQuery
		List<IRecord> getUsers(final int cnt);

		/**
		 * 更新指定用户额名称
		 * 
		 * @param name 用户名称
		 * @param id   用户id
		 */
		@JdbcExecute
		void updateUserById(final String name, final int id);

		/**
		 * 更新指定用户额名称
		 * 
		 * @param name 用户名称
		 * @param id   用户id
		 */
		@JdbcExecute
		void removeUserById(final int id);

		/**
		 * 默认函数处理,直接使用jdbc处理sql
		 * 
		 * @param jdbc
		 * @return DFrame
		 */
		default DFrame getUsers() {
			return jdbc().sql2recordS("select * from t_user").collect(DFrame.dfmclc);
		}

	}

	/**
	 * 数据库操作示例
	 */
	@Test
	public void foo() {
		// 创造一个IJdbcApp接口应用
		final var sqlfile = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql";
		final var jdbcApp = IJdbcApp.newDBInstance(() -> nspp(sqlfile), JdbcApp.class);
		println("db", jdbcApp.getDbName()); // 检索数据库名

		jdbcApp.withTransaction(sess -> { // 准备数据
			final var proto = REC("id", 1, "name", "zhangsan", "password", 123456, "phone", "18601690611", "sex", 1,
					"address", "shanghai");
			sess.sql2execute(sql("t_user", proto).ctsqls().get(2)); // 创建数据表
			for (int i = 0; i < 10; i++) {
				final var cities = "北京,天津,上海,重庆,广州".split(",");
				final var dataline = proto.derive("id", i, //
						"name", String.format("%s%d", proto.str("name"), i), //
						"sex", i % 2, //
						"address", cities[new Random().nextInt(cities.length)] //
				); // 数据行
				sess.sql2execute(sql("t_user", dataline).insql()); // 插入数据
			} // for
			println("#getUsers 3", sess.sql2dframe("#getUsers", rb("cnt").get(3))); // 使用语句标号提取语句并执行
			println("#updateUserById 3", sess.sql2execute("#updateUserById", rb("id,name").get(3, "张三"))); // 使用语句标号提取语句并执行
		}); // withTransaction
		println("------------------------------------------------------");
		final var dfm = jdbcApp.sqldframe("select * from t_user limit ##cnt", REC("cnt", 5));
		println(dfm);
		println("------------------------------------------------------");
		println("getUsers(5)", jdbcApp.getUsers(5));
		println("------------------------------------------------------");
		jdbcApp.updateUserById("zhangsan100", 1); // 修改数据
		jdbcApp.removeUserById(8); // 删除数据
		println("getUsers()", jdbcApp.getUsers());
	}

}
