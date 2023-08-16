package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.data.DataApp.IRecord.FT;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.data.DataApp.IRecord.rb;
import static gbench.util.io.Output.println;
import static gbench.sandbox.data.h2.H2db.*;
import static java.util.stream.Collectors.summarizingDouble;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.sandbox.data.h2.H2db;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.JSON;
import gbench.util.type.Types;
import gbench.util.data.MyDataApp;

/**
 * H2数据库操作
 */
public class H2Test {

	@Test
	public void foo() {
		println(JSON.isJson("1243.4"));
		final var rec = rb("id,name,sex").get("1,zhangsan,man".split(",")).add("address",
				REC("city", "shanghai", "street", "huahuazhen", "building", "101nong11#"));
		final var recjson = rec.json();
		println(recjson);
		println("recjson", JSON.isJson(recjson));
		final var dfmjson = shtmx("t_customer").collect(DFrame.dfmclc2).json();
		println(dfmjson);
		println("dfmjson", JSON.isJson(dfmjson));
	}

	/**
	 * 数据操作演示
	 */
	@Test
	public void bar() {
		new MyDataApp(h2_rec).withTransaction(sess -> {
			final var line = REC("id", 1, "name", "zhangsan", "password", 123456, "phone", "18601690610", "address",
					REC("city", "shanghai", "district", "changning", "street", "fahuazhen", "nong", 101, "building",
							REC("unit", 11, "room", 201)));
			final var table = "t_individual";
			sess.sql2execute(ctsql(table, line));
			sess.sql2execute(insql(table, line));
			final var p = sess.sql2x(String.format("select * from %s", table));
			p.forEach(r -> r.compute("address", H2db::json)); // 地址类型转换
			println(p);
			println("unit", p.get(0).pathi4("address/building/unit"));
		});
	}

	/**
	 * 订单演示
	 */
	@Test
	public void quz() {
		final var now = LocalDateTime.now();
		final var rnd = new Random();

		new MyDataApp(h2_rec).withTransaction(sess -> { // 准备数据
			imports("t_company,t_product,t_company_product".split(",")).accept(sess);
			final var companies = shuffle(sess.sql2x("select * from t_company").collect(mapby("id")), 10);
			final var products = shuffle(sess.sql2x("select * from t_product").collect(mapby("id")), 10);
			final var cps = new HashMap<Integer, IRecord>();
			for (final var cent : companies.entrySet()) {
				final var c = cent.getValue();
				for (final var pent : products.entrySet()) {
					final var p = pent.getValue();
					final var line = REC("company_id", c.i4("id"), "product_id", p.i4("id"), //
							"attrs", p.filter("id,name,price").add(REC("quantity", 1 + rnd.nextInt(10))), //
							"create_time", now, "update_time", now); // 产品数据
					sess.sql2executeS(insql("t_company_product", line)).findFirst().map(e -> e.i4(0))
							.ifPresent(id -> cps.put(id, REC("id", id).derive(line)));
				} // for
			} // for

			final var cpdfms = cps.values().stream().collect(groupby("company_id", DFrame::new)); // 公司产品
			final var t_order = "t_order"; // 订单表名
			final var orderb = rb("parta,partb,lines,create_time"); // 订单结构
			sess.sql2execute(ctsql(t_order, orderb.prepend("id").get(0, 0, 0, REC(), now))); // 创建订单表
			for (final var partaent : shuffle(companies).entrySet()) {
				final var parta = partaent.getValue();
				for (final var partbent : shuffle(companies).entrySet()) {
					final var partb = partbent.getValue();
					final var lines = cpdfms.get(partb.i4("id")).stream().sorted((a, b) -> Math.random() > 0.5 ? 1 : -1) // 打乱次序
							.map(e -> e.filter("id,company_id").add(
									e.rec("attrs").alias("id,product_id,name,title,price,price,quantity,quantity")))
							.collect(DFrame.dfmclc).head(5); // 订单行：公司产品
					final var orderdata = orderb.get(parta.get("id"), partb.get("id"), lines, now);
					sess.sql2execute(insql(t_order, orderdata));
				} // for
			} // for

			final Function<List<IRecord>, Object> stats_evaluator = e -> e.stream()
					.collect(summarizingDouble(r -> r.dbl("price") * r.dbl("quantity"))).getSum(); // 数据统计
			final var entity_sql = String.format("select distinct k from ( %s ) t", Stream.of("parta,partb".split(","))
					.map(k -> String.format("select %s k from %s", k, t_order)).collect(Collectors.joining(" union ")));
			for (final var entity_id : sess.sql2recordS(entity_sql).map(e -> e.get(0)).toList()) { // 会计主体
				final var ordersql = IRecord.FT("select * from $0 where parta = $1 or partb = $1", t_order, entity_id); // 订单sql
				final var rootNode = sess.sql2recordS(ordersql).map(jscompute("lines"))
						.flatMap(e -> e.getS("lines", IRecord::REC).map(e::derive))
						.map(e -> REC("entity_id", entity_id, "drcr", e.i4("parta").equals(entity_id) ? 1 : -1)
								.add(e.filter("company_id,product_id,title,price,quantity,parta,partb"))
								.add(e.alias("id,order_id")))
						.collect(pvtreeclc(stats_evaluator, "partb,product_id,drcr")); // 数据透视分阶层统计

				// 结果打印
				println(String.format("[%s]", companies.getOrDefault(entity_id, IRecord.REC("entity_id", entity_id))));
				rootNode.forEach(node -> {
					println(String.format("%s%s\t%s\t%.2f", " | ".repeat(node.getLevel()), node.getName(),
							node.getPath(), node.attrval(Types.cast(Double.class))));
				}); // forEach(node
				println("json ", rootNode.json((sb, e) -> FT("{\"name\":\"$0\"$1$2", e, //
						e.attrval(ifnull(v -> FT(", \"value\":$0", v), "")), e.isLeaf() ? "" : ", \"children\":["),
						(sb, e) -> e.isLeaf() ? "}" : "]}"));
			} // forEach(entity_id
		}); // withTransaction
	}

	/**
	 * 数据库配置
	 */
	final IRecord h2_rec = IRecord.REC( //
			"url", "jdbc:h2:mem:malonylcoadb;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", //
			"driver", "org.h2.Driver", //
			"user", "root", "password", "123456");
}
