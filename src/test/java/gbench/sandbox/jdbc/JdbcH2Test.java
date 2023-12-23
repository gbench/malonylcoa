package gbench.sandbox.jdbc;

import static gbench.sandbox.data.h2.H2db.imports;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

import org.junit.jupiter.api.Test;

import gbench.sandbox.data.h2.H2db;
import gbench.util.data.MyDataApp;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;

/**
 * 
 */
public class JdbcH2Test {

	@Test
	public void qux() {
		// 创造一个IJdbcApp接口应用
		final var sqlfile = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql";
		final var dbname = "mymall"; // 更换一个数据库
		final var url = String.format("jdbc:h2:mem:%s1;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", dbname);
		final var driver = "org.h2.Driver";
		final var h2_rec = REC("url", url, "driver", driver, "user", "root", "password", "123456");
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec);
		final var tables = "t_company,t_product,t_company_product,t_customer,t_coa,t_accts,t_journal,t_bksys"
				.split("[,]+"); // 基础数据表
		final var cp_sql = "select * from t_company_product where company_id=##cid"; // 公司产品
		// 数据初始导入
		new MyDataApp(h2_rec.toMap()).withTransaction(imports(tables));
		// 数据操作
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_product", sess.sql2dframe("select * from t_product limit ##cnt", "cnt", 2));
			println("t_company_product", sess.sql2dframe(cp_sql, "cid", 1).fmap(e->{
				e.compute("attrs", H2db::asMap);
				return e;
			}));
		});
	}

}
