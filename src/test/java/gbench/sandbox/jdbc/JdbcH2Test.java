package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.Jdbcs;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.kvp.Tuple2.P;
import static gbench.util.jdbc.sql.SQL.ctsql;
import static gbench.util.jdbc.sql.SQL.insql;
import static gbench.util.jdbc.sql.SQL.proto_of;
import static gbench.util.jdbc.Jdbcs.sample;
import static gbench.util.jdbc.Jdbcs.partitions;
import static gbench.util.jdbc.Jdbcs.batch_handlers;
import static gbench.util.jdbc.Jdbcs.imports;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Stream.iterate;

/**
 * H2数据库示例
 */
public class JdbcH2Test {

	/**
	 * 创建订单
	 * 
	 * @param cs         公司
	 * @param cps        产品
	 * @param stores     收获地址（仓库）
	 * @param createTime 创建时间
	 * @return 订单 IRecord
	 */
	final static IRecord buildOrder(final DFrame cs, final DFrame cps, final String[] stores,
			final LocalDateTime createTime) {

		final var rnd = new Random(); // 随机值
		final var parts = sample(cs, 2); // 订单各方
		final var part_a = parts.get(0); // 甲方 收货方 receiver
		final var part_b = parts.get(1); // 乙方 发货方 shipper
		final var parta_id = part_a.get("id"); // 甲方id
		final var partb_id = part_b.get("id"); // 乙方id, 产品是由partb转移到parta的
		final var products = cps.many2one("company_id", partb_id).shuffle().head(5); // 选择5个产品
		println(String.format("company product ---- %s[%s] ----- %s", part_b.str("name"), partb_id, products));

		final var receive_address = stores[rnd.nextInt(stores.length)]; // 接受地址
		final var items = products.rowS() // 产品记录
				.map(e -> { // 订单行项目
					final var id = e.get("id"); // 公司产品id
					final var name = e.get("name"); // 公司产品名称
					final var company_id = e.get("company_id"); // 公司id
					final var product_id = e.get("product_id"); // 产品id
					final var price = e.dbl("price"); // 价格
					final var quantity = 1 + rnd.nextInt(100); // 数量
					final var amount = price * quantity; // 金额总计
					final var item_rb = rb("id,name,company_id,product_id,price,quantity,amount");

					return item_rb.get(id, name, company_id, product_id, price, quantity, amount); // 订单行项目
				}).toList(); // 订单行项目
		final var name = String.format("%s ...", products.head().get("name")); // 用产品的第一个名称代表订单名称
		final var amount = items.stream().collect(summarizingDouble(e -> e.dbl("price") * e.dbl("quantity"))).getSum(); // 订单金额
		final var order_rb = rb("name,shipper,receiver,receive_address,amount,details,create_time"); // 订单结构
		final var details = rb("flag,amount,items").get(false, amount, items);// 订单详情
		final var t_order = order_rb.get(name, partb_id, parta_id, receive_address, amount, details, createTime); // 订单记录

		return t_order;
	}

	/**
	 * 准备公司产品
	 * 
	 * @param cs 公司数据
	 * @param ps 产品数据
	 * @return 公司产品
	 */
	public static List<IRecord> buildCPs(final DFrame cs, final DFrame ps) {

		final var cps = new LinkedList<IRecord>();
		for (final var c : cs.rows()) {
			for (final var p : ps.rows()) {
				final var cid = c.get("id"); // 公司id
				final var pid = p.get("id"); // 产品id
				final var cp_rb = rb("company_id,product_id,attrs,create_time,update_time");
				final var attrs = REC("url", "C:/Users/Administrator/Pictures/fumarate/sale.jpg", //
						"name", p.get("name"), "price", p.get("price"), "price_striked", p.get("price_striked"));
				final var cp = cp_rb.get(cid, pid, attrs, now(), now());
				cps.add(cp);
			}
		}

		return cps;
	}

	/**
	 * 数据处理器
	 * 
	 * @param key 鍵名
	 * @return 处理函数
	 */
	final static Consumer<? super IRecord> processor(final String key) {

		return e -> e.compute(key, Jdbcs::asMap); // 属性处理
	}

	/**
	 * H2 数据库操作演示, 商城数据示例：
	 */
	@Test
	public void foo() {

		// 创造一个IJdbcApp接口应用
		final var datafile = xls("f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx");
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
		final var top10 = "select * from ##tbl limit 10";
		final var size = 1000; // 数据量
		final var batch_size = 10; // 批次大小

		// 数据导入
		jdbcApp.withTransaction(imports(e -> datafile.autoDetect(e).collect(DFrame.dfmclc2), tables));
		// 数据操作
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_product", sess.sql2dframe("select * from t_product limit ##cnt", "cnt", 2));
			println("t_company_product", sess.sql2dframe(cp_sql, "cid", 1).forEachBy(processor("attrs")));
			println(sess.sql2dframe("#trialBalanceForH2", "bksys_id", 1)); // 试算平衡表

			final var cs = sess.sql2dframe("select * from t_company"); // 公司信息
			final var ps = sess.sql2dframe("select id,name,price,price_striked from t_product cp"); // 产品信息
			final var cp_name = "t_company_product"; // 公司产品表名
			final var or_name = "t_order"; // 订单名称
			final var cp_partitions = partitions(buildCPs(cs, ps), batch_size); // 公司产品数据

			// 公司产品数据
			sess.sqlexecute("drop table t_company_product"); // 移除公司产品数据
			sess.sql2execute(println(ctsql(cp_name, proto_of(cp_partitions)))); // 创建数据表
			batch_handlers(cp_partitions, partition -> { // 公司产品
				println("cp ids", sess.sql2execute(println(insql(cp_name, partition))));// 插入数据
			}); // 重新设置公司产品

			// 订单数据处理
			final var cps = sess.sql2dframe("select * from ##tbl", "tbl", cp_name).forEachBy(processor("attrs"))
					.fmapBy(e -> e.rec("attrs").add(e.filter("id,company_id,product_id"))); // 公司产品
			final Supplier<IRecord> os = () -> buildOrder(cs, cps, stores, now()); // 订单生成函数,order supplier
			final var o_partitions = partitions(iterate(os.get(), i -> os.get()).limit(size), batch_size); // 公司产能品数据

			sess.sql2execute(println(ctsql(or_name, proto_of(o_partitions)))); // 创建数据表
			batch_handlers(o_partitions, partition -> { // 订单数据
				println("order ids", sess.sql2execute(println(insql(or_name, partition))));// 插入数据
			}); // 重新设置公司产品

			// 数据查看
			println("------------------------------------ 数据查看 ------------------------------------");
			for (final var p : Arrays.asList(P(or_name, "details"), P(cp_name, "attrs"))) { // 订单数据与公司产品数据
				println(p._1(), sess.sql2dframe(top10, "tbl", p._1()).forEachBy(processor(p._2())));
			}

			// 试算平衡2
			println(sess.sql2dframe("#trialBalanceForH2", "bksys_id", 1)); // 试算平衡表
		});
	}

}
