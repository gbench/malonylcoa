package gbench.sandbox.jdbc;

import static gbench.sandbox.data.h2.H2db.imports;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.sql;

import java.util.Random;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.summarizingDouble;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.DFrame;
import gbench.sandbox.data.h2.H2db;
import gbench.util.data.MyDataApp;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;

/**
 * H2数据库示例
 */
public class JdbcH2Test {

	/**
	 * 数据处理器
	 * 
	 * @param key 鍵名
	 * @return 处理函数
	 */
	final static Consumer<? super IRecord> processor(final String key) {
		return e -> e.compute(key, H2db::asMap); // 属性处理
	}

	/**
	 * 抽样
	 * 
	 * @param dfm 元数据
	 * @param n   抽样
	 * @return 抽样
	 */
	final static List<IRecord> sample(final DFrame dfm, final Integer n) {
		return dfm.rowS().map(IRecord::REC).map(e -> Tuple2.of(Math.random(), e))
				.sorted((a, b) -> a._1().compareTo(b._1())).map(e -> e._2()).limit(n).toList(); // 随机生成
	}

	/**
	 * 创建订单
	 * 
	 * @param companydfm 公司
	 * @param productdfm 产品
	 * @param stores     收获地址（仓库）
	 * @return IRecord
	 */
	final static IRecord buildOrder(final DFrame companydfm, final DFrame productdfm, final String[] stores) {

		final var rnd = new Random(); // 随机值
		final var parts = sample(companydfm, 2); // 订单各方
		final var products = sample(productdfm, 2); // 选择产品
		final var shipper = parts.get(0); // 发货放
		final var receiver = parts.get(1); // 收货方
		final var receive_address = stores[rnd.nextInt(stores.length)]; // 接受地址
		final var items = products.stream() // 产品记录
				.map(e -> e.add("price", e.dbl("price") * rnd.nextDouble(1d), "quantity", 1 + rnd.nextInt(100))) // 价格调整
				.map(e -> e.add("amount", e.dbl("price") * e.dbl("quantity"))).toList(); // 订单行项目
		final var amount = items.stream().collect(summarizingDouble(e -> e.dbl("price") * e.dbl("quantity"))).getSum(); // 订单金额
		final var rb = rb("#id,name,shipper,receiver,receive_address,amount,details,create_time"); // 订单结构
		final var details = REC("flag", false, "amount", amount, "items", items); // 订单详情
		final var t_order = rb.get(1, products.get(0).get("name"), shipper.get("id"), receiver.get("id"),
				receive_address, amount, details, now()); // 订单记录

		return t_order;
	}

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
		final var stores = "北京,天津,重庆,上海,广州,深圳".split("[,]+"); // 仓库地址

		// 数据初始导入
		new MyDataApp(h2_rec.toMap()).withTransaction(imports(tables)); // 导入订单数据
		// 数据操作
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_product", sess.sql2dframe("select * from t_product limit ##cnt", "cnt", 2));
			println("t_company_product", sess.sql2dframe(cp_sql, "cid", 1).forEachByRow(processor("attrs")));
			println(sess.sql2dframe("#trialBalanceForH2", "bksys_id", 1)); // 试算平衡表

			final var companydfm = sess.sql2dframe("select * from t_company"); // 公司信息
			final var productdfm = sess.sql2dframe("select id,name,price,price_striked from t_product cp"); // 产品信息
			final var order_proto = buildOrder(companydfm, productdfm, stores); // 数据原型 )
			sess.sqlexecute("drop table t_company_product");

			for (final var c : companydfm.rows()) {
				for (final var p : productdfm.rows()) {
					final var cid = c.get("id");
					final var pid = p.get("id");
					final var cprb = rb("#id,company_id,product_id,attrs,create_time,update_time");
					final var attrs = REC("url", "C:/Users/Administrator/Pictures/fumarate/sale.jpg", //
							"price", p.get("price"), "price_striked", p.get("price_striked"));
					final var cp = cprb.get(1, cid, pid, attrs, now(), now());
					final var tname = "t_company_product"; // 公司产品

					if (!sess.isTablePresent(tname)) { // 创建数据表
						sess.sql2execute(println(sql(tname, cp).ctsqls(true).get(2))); // 创建数据表
					}
					final var s = sql(tname, cp.remove("#id")).insql();
					sess.sql2execute(s); // 插入数据
				}
			}
			if (!sess.isTablePresent("t_order")) { // 表不存在则创建
				sess.sql2execute(println(sql("t_order", order_proto).ctsqls(true).get(2))); // 创建数据表
			}
			for (int i = 0; i < 10; i++) { // 创建数据表
				sess.sql2execute("#addOrder", buildOrder(companydfm, productdfm, stores)); // 创建订单
			} // for
			println("t_order", sess.sql2dframe("select * from t_order") //
					.forEachByRow(processor("details"))); // 查看订单记录
			println("t_company_product", sess.sql2dframe("select * from t_company_product limit 20,25") //
					.forEachByRow(processor("attrs"))); // 查看订单记录
		});
	}

}
