package gbench.sandbox.jdbc;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.IJdbcApp.nspp;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.sql;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import gbench.global.Globals;
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
	 * 数据库配置
	 */
	@JdbcConfig(url = "jdbc:h2:mem:malonylcoa;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", user = "root", password = "123456")
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
	 * 执行 JdbcApp 数据处理
	 * 
	 * @param jdbcApp
	 */
	public void run(final JdbcApp jdbcApp) {
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
		final var sql = jdbcApp.getSql("select * from t_user limit ##cnt", "cnt", 5);
		println("sql", sql);
		final var sql2 = jdbcApp.getSql("#updateUserById", rb("id,name").get(3, "张三"));
		println("sql2", sql2);
		println("------------------------------------------------------");
		final var dfm = jdbcApp.sqldframe("select * from t_user limit ##cnt", "cnt", 5);
		println("dfm", dfm);
		final var dfm2 = jdbcApp.sqldframe("#getUsers", "cnt", 5);
		println("dfm2", dfm2);
		println("------------------------------------------------------");
		println("getUsers(5)", jdbcApp.getUsers(5));
		println("------------------------------------------------------");
		jdbcApp.updateUserById("zhangsan100", 1); // 修改数据
		jdbcApp.removeUserById(8); // 删除数据
		println("getUsers()", jdbcApp.getUsers());
	}

	/**
	 * 数据库操作示例
	 */
	@Test
	public void foo() {
		// 创造一个IJdbcApp接口应用
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql";
		final var jdbcApp = IJdbcApp.newDBInstance(() -> nspp(sqlfile), JdbcApp.class);

		this.run(jdbcApp);
	}

	/**
	 * 使用指定配置来创建
	 */
	@Test
	public void bar() {
		// 创造一个IJdbcApp接口应用
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql";
		final var dbname = String.format("malonylcoa_%s", "bar"); // 更换一个数据库
		final var url = String.format("jdbc:h2:mem:%s1;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", dbname);
		final var driver = "org.h2.Driver";
		final var h2_rec_1 = REC("url", url, "driver", driver, "user", "root", "password", "123456");
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, JdbcApp.class, h2_rec_1);

		this.run(jdbcApp);
	}

	/**
	 * 使用指定配置来创建
	 * <p>
	 * 数据库界面参见应用: <br>
	 * https://gitee.com/gbench/gcloud/tree/develop/projs/fumarate/src/test/resources/mall
	 * <p>
	 * 数据表参见应用:
	 * https://gitee.com/gbench/gcloud/blob/develop/projs/fumarate/src/test/resources/mall/src/comps/ecomp/EComp.js
	 */
	@Test
	public void quz() {
		// 创造一个IJdbcApp接口应用
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql";
		final var dbname = "hitler"; // 更换一个数据库
		final var url = String.format("jdbc:mysql://127.0.0.1:3309/%s?serverTimezone=UTC", dbname);
		final var driver = "com.mysql.cj.jdbc.Driver";
		final var mysql_rec = REC("url", url, "driver", driver, "user", "root", "password", "123456");
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, JdbcApp.class, mysql_rec);

		println("db", jdbcApp.getDbName()); // 检索数据库名
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_coa", sess.sql2dframe("select * from t_coa limit ##cnt", "cnt", 3));
			println("trialBalance", sess.sql2dframe("#trialBalance", "bksys_id", 1));
			// 订单处理
			final var orders = sess.sql2dframe("#getOrdersByCID", "bksys_id", 1, "company_id", 1);
			println("orders", orders);
			println("按照列顺序进行遍历");
			orders.foreach((String name, List<String> col) -> {
				println(name, col);
			});
		});
	}

}
