package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import gbench.global.Globals;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.sql.SQL;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.imports;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.sql;
import static gbench.util.jdbc.sql.SQL.OpMode.BLANK;

/**
 * MallTest
 * 
 * @author: gbench@sina.com 2022-12-26 12:52
 */
public class Sql2Test {

	/**
	 * test
	 */
	@Test
	public void foo() {
		final var proto = rb("name,sex,address") //
				.get("zhangsan,man,shanghai".split(","));
		final var datafile = xls(Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx"); // 数据-源文件
		SQL.debug = true; // 开启调试标记

		jdbcApp.withTransaction(sess -> {
			imports(name -> datafile.autoDetect(name).collect(DFrame.dfmclc2), "t_product").accept(sess);
			println(sess.sql2dframe("select * from t_product").shuffle().head(5));
			final var users = Stream.iterate(0, i -> i + 1).limit(10) //
					.map(i -> proto.derive("name", String.format("zhang%s", i))).toList();
			for (final String sql : Arrays.asList( // 创建用户数据
					sql("t_user").ctsql(users, true, true, 2) // 用户表
					, sql("t_user").insql(users) //
					, sql("t_user").upsql(rb("id,name,sex").get(10, "zhangsan_10", 1)) // 默认是等于
					, sql("t_user").upsql(rb("*id,address,*sex").get("between 1 and 5", "beijing", "man")) // between 测试
					, sql("t_user").upsql(rb("id,name,sex").get("in (2,3,4)", "zhangsan_1", 1)) // in 测试
					, sql("t_user").upsql(rb("id,name,sex").get("not in (2,3,4)", "zhangsan_not", 1)) // not in 测试
					, sql("t_user").upsql(rb("id,name,sex").get("is not null", "zs_notnull", 1)) // is 测试
					, sql("t_user").upsql(rb("*id,*name,sex").get("=1 and", "='lisi' or sex='3'", 1), BLANK) // is 测试
			)) { // 用户数据
				println(sql, sess.sql2execute(sql));
			} // for
			println(sess.sql2dframe("select * from t_user"));
		}); // withTransaction

		// datafile.close();
	}

	final String dbname = "sqltest"; // 更换一个数据库
	final String url = String.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", dbname);
	final IRecord h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456");
	final IMySQL jdbcApp = IJdbcApp.newNsppDBInstance(null, IMySQL.class, h2_rec); // 数据应用

}
