package gbench.sandbox.jdbc;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

import org.junit.jupiter.api.Test;

import gbench.global.Globals;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.Jdbcs;
import gbench.util.jdbc.kvp.DFrame;

/**
 * 交易策略
 */
public class JdbcH2Policy {

	@Test
	public void foo() {
		final var datafile = xls(Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx"); // 数据-源文件
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
		final var db = "mymall"; // 数据库名
		final var url = String.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", db); // h2连接字符串
		final var h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456"); // h2数据库
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
		final var tables = "t_company,t_product,t_company_product,t_coa,t_accts,t_journal,t_bksys,t_acct_policy"
				.split("[,]+"); // 基础数据表

		jdbcApp.withTransaction(sess -> {
			Jdbcs.imports(e -> datafile.autoDetect(e).collect(DFrame.dfmclc2), tables).accept(sess);
			final var policies = sess.sql2dframe("select * from t_acct_policy").pivotTable(
					"name,bill_type,position,drcr", datas -> datas.findFirst().map(e -> e.get("acctnum")).orElse(null));
			final var name = "POLICY0001"; // 策略名称
			final var order_type = "BILL0001"; // 订单类型
			final var policy_path = String.format("%s/%s", name, order_type); // 策略名称
			final var policy = policies.path2rec(policy_path);

			println(policy_path, policy);
		});
	}
}
