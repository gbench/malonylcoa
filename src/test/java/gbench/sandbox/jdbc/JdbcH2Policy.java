package gbench.sandbox.jdbc;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.jdbc.kvp.IRecord.REC;

import org.junit.jupiter.api.Test;

import gbench.util.io.Output;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.Jdbcs;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

/**
 * 交易策略
 */
public class JdbcH2Policy {

	@Test
	public void foo() {
		final var datafile = xls("f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx"); // 数据-源文件
		final var sqlfile = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
		final var db = "mymall"; // 数据库名
		final var url = String.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", db); // h2连接字符串
		final var h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456"); // h2数据库
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
		final var tables = "t_company,t_product,t_company_product,t_coa,t_accts,t_journal,t_bksys,t_policy"
				.split("[,]+"); // 基础数据表
		jdbcApp.withTransaction(sess -> {
			Jdbcs.imports(e -> datafile.autoDetect(e).collect(DFrame.dfmclc2), tables).accept(sess);
			final var policies = sess.sql2dframe("select * from t_policy").rowS()
					.collect(IRecord.pvtclc("name,order_type,position,drcr"));
			final var name = "PLIOCY0001"; // 策略名称
			final var order_type = "ORDER0001"; // 订单类型
			final var position = "LONG"; // 头寸
			final var drcr = "DR"; // 借贷方向
			final var path = String.format("%s/%s/%s/%s", name, order_type, position, drcr);
			final var pp = policies.path2opt(path, e -> (IRecord) e);
			Output.println(path, pp);
		});
	}
}
