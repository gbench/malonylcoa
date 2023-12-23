package gbench.sandbox.jdbc;

import static gbench.sandbox.data.h2.H2db.imports;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;

import java.util.Random;

import static java.util.stream.Collectors.summarizingDouble;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.sql.SQL;
import gbench.util.jdbc.kvp.DFrame;
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
		final Consumer<? super IRecord> attrs_f = e -> e.compute("attrs", H2db::asMap); // 属性处理
		final BiFunction<DFrame, Integer, List<IRecord>> random_of = (dfm, n) -> dfm.rowS().map(IRecord::REC)
				.map(e -> Tuple2.of(Math.random(), e)).sorted((a, b) -> a._1().compareTo(b._1())).map(e -> e._2())
				.limit(n).toList(); // 随机生成
		final var stores = "北京,天津,重庆,上海,广州,深圳".split("[,]+");
		final var rnd = new Random();

		// 数据初始导入
		new MyDataApp(h2_rec.toMap()).withTransaction(imports(tables));
		// 数据操作
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_product", sess.sql2dframe("select * from t_product limit ##cnt", "cnt", 2));
			println("t_company_product", sess.sql2dframe(cp_sql, "cid", 1).forEachByRow(attrs_f));
			println(sess.sql2dframe("#trialBalanceForH2", "bksys_id", 1));
			
			final var companydfm = sess.sql2dframe("select * from t_company");
			final var productdfm = sess.sql2dframe("select id,name,price from t_product");
			final var parts = random_of.apply(companydfm, 2); // 订单各方
			final var products = random_of.apply(productdfm, 2); // 选择产品
			final var shipper = parts.get(0);
			final var receiver = parts.get(1);
			final var receive_address = stores[rnd.nextInt(stores.length)];
			final var items = products.stream().map(e -> e.add(//
					"price", e.dbl("price") * rnd.nextDouble(1d), //
					"quantity", rnd.nextInt(100))) //
					.map(e -> e.add("", e.dbl("price") * e.dbl("quantity"))).toList();
			final var amount = items.stream().collect(summarizingDouble(e -> e.dbl("price")*e.dbl("quantity"))).getSum();
			final var rb = rb("id,name,shipper,receiver,receive_address,amount,details,create_time");
			final var details = REC("flag", false, "amount", amount, "items", items);
			final var order = rb.get(1,products.get(0).get("name"),shipper.get("id"), //
					receiver.get("id"), receive_address, amount, details,LocalDateTime.now());
			final var sql = SQL.sql("t_order", order);
			final var ctsql = sql.ctsqls().get(2);
			println(ctsql);
			sess.sql2execute(ctsql);
			sess.sql2execute("#addOrder", order.set("details", details.json()));
			println(sess.sql2dframe("select * from t_order")
					.forEachByRow(e -> e.compute("details", H2db::asMap)));

		});
	}

}
