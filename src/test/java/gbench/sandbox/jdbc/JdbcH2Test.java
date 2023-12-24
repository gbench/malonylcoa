package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.DFrame;
import gbench.sandbox.data.h2.H2db;
import gbench.util.data.MyDataApp;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.function.ExceptionalConsumer;

import static gbench.sandbox.data.h2.H2db.imports;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.kvp.Tuple2.P;
import static gbench.util.jdbc.sql.SQL.sql;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Stream.iterate;

import java.time.LocalDateTime;

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
	final static List<IRecord> sample(final Stream<IRecord> ss, final Integer n) {
		return ss.map(IRecord::REC).map(e -> Tuple2.of(Math.random(), e)).sorted((a, b) -> a._1().compareTo(b._1()))
				.map(e -> e._2()).limit(n).toList(); // 随机生成
	}

	/**
	 * 创建订单
	 * 
	 * @param companydfm 公司
	 * @param productdfm 产品
	 * @param stores     收获地址（仓库）
	 * @return IRecord
	 */
	final static IRecord buildOrder(final DFrame companydfm, final DFrame cpdfm, final String[] stores,
			final LocalDateTime ldt) {

		final var rnd = new Random(); // 随机值
		final var parts = sample(companydfm.rowS(), 2); // 订单各方
		final var part_a = parts.getFirst();
		final var parta_id = part_a.get("id");
		final var products = sample(cpdfm.rowS().filter(e -> Objects.equals(e.get("company_id"), parta_id)), 5); // 选择产品
		println(String.format("company product ---- %s[%s] ----- %s", part_a.str("name"), parta_id, products));
		final var shipper = parts.get(0); // 发货放
		final var receiver = parts.get(1); // 收货方
		final var receive_address = stores[rnd.nextInt(stores.length)]; // 接受地址
		final var items = products.stream() // 产品记录
				.map(e -> e.add("price", e.dbl("price") * rnd.nextDouble(1d), "quantity", 1 + rnd.nextInt(100))) // 价格调整
				.map(e -> e.add("amount", e.dbl("price") * e.dbl("quantity"))).toList(); // 订单行项目
		final var amount = items.stream().collect(summarizingDouble(e -> e.dbl("price") * e.dbl("quantity"))).getSum(); // 订单金额
		final var rb = rb("name,shipper,receiver,receive_address,amount,details,create_time"); // 订单结构
		final var details = REC("flag", false, "amount", amount, "items", items); // 订单详情
		final var t_order = rb.get(products.get(0).get("name"), shipper.get("id"), receiver.get("id"), receive_address,
				amount, details, ldt); // 订单记录

		return t_order;
	}

	/**
	 * 准备公司产品
	 * 
	 * @param cs 公司数据
	 * @param ps 产品数据
	 * @return 公司产品
	 */
	public List<IRecord> buildCPs(final DFrame cs, final DFrame ps) {
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
	 * 数据分组
	 * 
	 * @param data 数据列表
	 * @param size 分组长度
	 * @return 数据分组
	 */
	public static Map<Integer, List<IRecord>> partitions(final List<IRecord> data, final int size) {
		return partitions(data.stream(), size);
	}

	/**
	 * 数据分组
	 * 
	 * @param dataS 数据列表
	 * @param size  分组长度
	 * @return 数据分组
	 */
	public static Map<Integer, List<IRecord>> partitions(final Stream<IRecord> dataS, final int size) {
		final var ar = new AtomicInteger();
		final var parts = dataS.collect(groupingBy(e -> ar.getAndIncrement() / size));
		return parts;
	}

	/**
	 * create table sql
	 * 
	 * @param name  表名
	 * @param proto 数据原型
	 * @return create table sql
	 */
	public static String ctsql(final String name, final IRecord proto) {
		return sql(name, REC("#id", 1).add(proto)).ctsqls(true).get(2);
	}

	/**
	 * 数据插入
	 * 
	 * @param name  数据表
	 * @param proto 数据原型
	 * @return 数据插入
	 */
	public static String insql(final String name, final List<IRecord> data) {
		return sql(name, data).insql();
	}

	/**
	 * 批量处理
	 * 
	 * @param <K>        分组名
	 * @param <V>        分组
	 * @param partitions 分组数据
	 * @param handler    分组处理器
	 * @throws Exception
	 */
	public static <K, V> void batch_handlers(final Map<K, V> partitions, final ExceptionalConsumer<V> handler)
			throws Exception {
		for (final var partition : partitions.entrySet()) { // 重新设置公司产品
			handler.accept(partition.getValue());
		}
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
		final var top10 = "select * from ##tbl limit 10";
		final var size = 1000; // 数据量
		final var batch_size = 10; // 批次大小

		// 数据初始导入
		new MyDataApp(h2_rec.toMap()).withTransaction(imports(tables)); // 导入订单数据
		// 数据操作
		jdbcApp.withTransaction(sess -> {
			println("all tables", sess.sql2dframe("#getAllTables"));
			println("t_product", sess.sql2dframe("select * from t_product limit ##cnt", "cnt", 2));
			println("t_company_product", sess.sql2dframe(cp_sql, "cid", 1).forEachByRow(processor("attrs")));
			println(sess.sql2dframe("#trialBalanceForH2", "bksys_id", 1)); // 试算平衡表

			final var cs = sess.sql2dframe("select * from t_company"); // 公司信息
			final var ps = sess.sql2dframe("select id,name,price,price_striked from t_product cp"); // 产品信息
			final var cp_name = "t_company_product"; // 公司产品表名
			final var or_name = "t_order"; // 订单名称
			final var cp_partitions = partitions(this.buildCPs(cs, ps), batch_size); // 公司产品数据
			final var cp_proto = cp_partitions.entrySet().iterator().next().getValue().getFirst();

			// 公司产品数据
			sess.sqlexecute("drop table t_company_product"); // 移除公司产品数据
			sess.sql2execute(println(ctsql(cp_name, cp_proto))); // 创建数据表
			batch_handlers(cp_partitions, partition -> { // 公司产品
				println("cp ids", sess.sql2execute(println(insql(cp_name, partition))));// 插入数据
			}); // 重新设置公司产品

			// 订单数据处理
			final var cps = sess.sql2dframe("select * from ##tbl", "tbl", cp_name).forEachByRow(processor("attrs"))
					.fmapByRow(e -> e.path2rec("attrs").add(e.filter("id,company_id,product_id"))); // 公司产品
			final Supplier<IRecord> os = () -> buildOrder(cs, cps, stores, now()); // 订单生成函数,order supplier
			final var o_proto = os.get(); // 数据原型
			final var o_partitions = partitions(iterate(os.get(), i -> os.get()).limit(size), batch_size); // 公司产能品数据
			sess.sql2execute(println(ctsql(or_name, o_proto))); // 创建数据表
			batch_handlers(o_partitions, partition -> { // 订单数据
				println("order ids", sess.sql2execute(println(insql(or_name, partition))));// 插入数据
			}); // 重新设置公司产品

			// 数据查看
			println("------------------------------------ 数据查看 ------------------------------------");
			for (final var p : Arrays.asList(P(or_name, "details"), P(cp_name, "attrs"))) { // 订单数据与公司产品数据
				println(p._1(), sess.sql2dframe(top10, "tbl", p._1()).forEachByRow(processor(p._2())));
			}
		});
	}

}
