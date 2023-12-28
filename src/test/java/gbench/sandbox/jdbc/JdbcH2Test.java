package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
import static gbench.util.jdbc.sql.SQL.sql;
import static gbench.util.jdbc.Jdbcs.sample;
import static gbench.util.jdbc.Jdbcs.partitions;
import static gbench.util.jdbc.Jdbcs.batch_handlers;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.Jdbcs.imports;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Stream.iterate;

/**
 * H2数据库示例
 */
public class JdbcH2Test {

	final static Map<Object, DFrame> company_cache = new HashMap<>();
	final static Map<Object, IRecord> coa_cache = new HashMap<>();

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
		final var products = cps.many2one("company_id", partb_id, company_cache).shuffle().head(5); // 选择5个产品
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
	 * 创建账套
	 * 
	 * @param company 公司信息
	 * @param code    账套名称
	 * @return 账簿名称
	 */
	public static IRecord buildBks(final IRecord company, final int code) {

		final var title = String.format("%s-%03d", company.get("name"), code);
		return REC("name", String.format("BKS-%03d", code), "title",
				String.format("%s-%03d", company.get("name"), code), "company_id", company.get("id"), //
				"create_time", now(), "description", title);
	}

	/**
	 * H2 数据库操作演示, 商城数据示例：
	 */
	@Test
	public void foo() {

		// 创造一个IJdbcApp接口应用
		final var datafile = xls("f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx"); // 数据-源文件
		final var sqlfile = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
		final var db = "mymall"; // 数据库名
		final var url = String.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", db); // h2连接字符串
		final var h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456"); // h2数据库
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
		final var tables = "t_company,t_product,t_company_product,t_coa,t_accts,t_journal,t_bksys".split("[,]+"); // 基础数据表
		final var cp_sql = "select * from t_company_product where company_id=##cid"; // 公司产品
		final var stores = "北京,天津,重庆,上海,广州,深圳".split("[,]+"); // 仓库地址
		final var top10 = "select * from ##tbl limit 10"; // 头前10行数据
		final var size = 1000; // 模拟生成的数据量,t_order的数据规模
		final var batch_size = 10; // 插入数据时候的批次大小
		final var acct_policies = REC( // 会计记账策略
				"a", REC("dr", 1402, "cr", 1002), // dr:在途物资,cr:银行存款
				"b", REC("dr", 1407, "cr", 1406) // dr:发出商品,cr:库存商品
		);

		// 数据导入
		jdbcApp.withTransaction(imports(e -> datafile.autoDetect(e).collect(DFrame.dfmclc2), tables));
		// 数据操作
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_product", sess.sql2dframe("select * from t_product limit ##cnt", "cnt", 2));
			println("t_company_product", sess.sql2dframe(cp_sql, "cid", 1).forEachBy(h2_json_processor("attrs")));
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
			final var cps = sess.sql2dframe("select * from ##tbl", "tbl", cp_name).forEachBy(h2_json_processor("attrs"))
					.fmapBy(e -> e.filter("id,company_id,product_id").add(e.rec("attrs"))); // 公司产品
			final Supplier<IRecord> os = () -> buildOrder(cs, cps, stores, now()); // 订单生成函数,order shipper
			final var o_partitions = partitions(iterate(os.get(), i -> os.get()).limit(size), batch_size); // 公司产能品数据

			sess.sql2execute(println(ctsql(or_name, proto_of(o_partitions)))); // 创建数据表
			batch_handlers(o_partitions, partition -> { // 订单数据
				println("order ids", sess.sql2execute(println(insql(or_name, partition))));// 插入数据
			}); // 重新设置公司产品

			// 数据查看
			println("------------------------------------ 数据查看 ------------------------------------");
			for (final var p : Arrays.asList(P(or_name, "details"), P(cp_name, "attrs"))) { // 订单数据与公司产品数据
				println(p._1(), sess.sql2dframe(top10, "tbl", p._1()).forEachBy(h2_json_processor(p._2())));
			}

			// bksys
			final int bksys_id; // 账簿id
			final var entity_id = 1; // 主体id
			final var order_sql_1 = Jdbcs.format("select * from t_order where receiver={0} or shipper={0}", entity_id); // 提取公司1的订单信息
			final var coas = sess.sql2dframe("select * from t_coa") // t_coa 科目表
					.forEachBy(e -> e.compute("acctnum", (String k, Double v) -> v.intValue())); // 科目表
			final Function<Integer, String> coa_account = acctnum -> coas.one2one("acctnum", acctnum, coa_cache)
					.str("account"); // 解析编码为名称
			println(bksys_id = sess.sql2execute2int(sql("t_bksys", buildBks(cs.row(0), 2)).insql())); // 账册id

			// 分类账写入
			for (final var torder : sess.sql2dframe(order_sql_1).forEachBy(h2_json_processor("details")).rows()) { // 提取会计主体的订单
				final var parta = torder.i4("receiver"); // 日记账摘要-甲方
				final var partb = torder.i4("shipper"); // 日记账摘要-乙方
				final var title = String.format("%s-%s", parta, partb); // 日记账摘要标题
				final var tjournal = REC("bksys_id", bksys_id, "title", title, "objects", Arrays.asList(entity_id), // 会计主体
						"create_time", now(), "description", "-"); // 订单的日记账摘要
				final var journal_id = sess.sql2execute2int(sql("t_journal", tjournal).insql()); // 日记账摘要id

				// 一次处理相关的每个订单行项目
				for (final var item : torder.path2lls("details/items", IRecord::REC)) { // 订单记账
					final var due_date = now(); // 到期日
					final var id = item.get("id"); // 公司产品id
					final var name = item.get("name"); // 公司产品名称
					final var amount = item.dbl("amount"); // amount
					final var proto = REC("journal_id", journal_id, "due_date", due_date, "amount", amount); // 数据原型
					final var postion = Objects.equals(entity_id, parta) ? "a" : "b"; // 会计主体是否在甲方
					final var dr_acct = acct_policies.path2int(String.format("%s/dr", postion)); // 借方账户
					final var cr_acct = acct_policies.path2int(String.format("%s/cr", postion)); // 贷方账户
					final var dr_title = String.format("%s-%s", coa_account.apply(dr_acct), name); // 借方标题
					final var cr_title = String.format("%s-%s", coa_account.apply(cr_acct), name); // 贷方标题
					final var fmt = "%s"; // 账户编码
					// 分录誊写
					final var ids = sess.sql2execute(println(sql("t_accts").insql(// 借贷分录
							proto.derive("drcr", 1, "acctnum", String.format(fmt, dr_acct, id), "title", dr_title), // 借方
							proto.derive("drcr", -1, "acctnum", String.format(fmt, cr_acct, id), "title", cr_title)))); // 贷方
					println("借贷分录", parta, parta, ids);
				} // 订单记账
			} // 提取会计主体的订单

			// 记账信息查看
			println(sess.sql2dframe(top10, "tbl", "t_bksys")); // 账册数据
			println(sess.sql2dframe(top10, "tbl", "t_journal")); // 日记账摘要
			println(sess.sql2dframe(top10, "tbl", "t_accts")); // 会计分录

			// 试算平衡2
			println(sess.sql2dframe("#trialBalanceForH2", "bksys_id", bksys_id)); // 试算平衡表
		});
	}

}
