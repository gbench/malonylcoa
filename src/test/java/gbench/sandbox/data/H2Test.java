package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.array.INdarray.ndclc;
import static gbench.util.data.DataApp.DFrame.dfmclc;
import static gbench.util.data.DataApp.IRecord.FT;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.data.DataApp.IRecord.rb;
import static gbench.util.data.DataApp.Tuple2.P;
import static gbench.util.function.Functions.identity;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.RPTA;
import static gbench.util.lisp.Lisp.cph;
import static gbench.sandbox.data.h2.H2db.*;
import static java.lang.Math.pow;
import static java.util.stream.Collectors.summarizingDouble;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.sandbox.data.h2.H2db;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.JSON;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.data.xls.DataMatrix;
import gbench.util.lisp.Lisp;
import gbench.util.tree.TrieNode;
import gbench.util.type.Types;
import gbench.util.array.INdarray;
import gbench.util.data.MyDataApp;

/**
 * H2数据库操作
 */
public class H2Test {

	@Test
	public void sqlgen() {
		final var proto = rb("id,name,password,sex,phone,address") //
				.get(1, "zhangsan", 123456, "男", 18601690610l,
						REC("city", "shanghai", "district", "changing", "street", "fahuazhenlu"));
		println("ctsql t_user", ctsql("t_user", proto));
		println("insql t_user", insql("t_user", proto));
		println("insql t_tbl ", ctsql("t_tbl", P("abc".split(""), Lisp.A(1, 2, 3, 4, 5, 6))));
		println("insql t_tbl ", insql("t_tbl", P("abc".split(""), Lisp.A(1, 2, 3, 4, 5, 6))));
		// REC的双值展开
		println(REC("nums", nats(4)));
		println(REC("nums", nats(4).data()));
		println(REC("nums", nats(4)));
		println(REC(nats(4), nats(4)));
	}

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
		new MyDataApp(h2_rec_1).withTransaction(sess -> {
			final var line = REC("id", 1, "name", "zhangsan", "password", 123456, "phone", "18601690610", "address",
					REC("city", "shanghai", "district", "changning", "street", "fahuazhen", "nong", 101, "building",
							REC("unit", 11, "room", 201)));
			final var tblname = "t_individual";
			sess.sql2execute(ctsql(tblname, line)); // 创建表
			sess.sql2execute(insql(tblname, line)); // 插入数据1
			sess.sql2execute(insql(tblname, line.derive("id", 2, "name", "zhangsan2", "phone", "18601690611")
					.pset("address/building/room", 202))); // 插入数据2
			sess.sql2execute(String.format("update %s set name='%s' where id=%d", tblname, "zhangsan1", 1)); // 修改数据
			final var dfm = sess.sql2x(String.format("select * from %s", tblname)) // 查询数据
					.foreach(r -> r.compute("address", H2db::json)); // 地址类型转换
			println(dfm);
			println("unit:", dfm.get(0).pathi4("address/building/unit"));
			println("delete:", sess.sql2execute(String.format("delete from %s where id=%d", tblname, 2))); // 删除数据
			println("data:", sess.sql2x(String.format("select * from %s", tblname)) // 查询数据
					.foreach(r -> r.compute("address", H2db::json))); // 地址类型转换
		}); // withTransaction
	}

	/**
	 * 订单演示
	 */
	@Test
	public void quz() {
		final var now = LocalDateTime.now();
		final var rnd = new Random();

		new MyDataApp(h2_rec_1).withTransaction(sess -> { // 准备数据
			imports("t_company,t_product,t_company_product".split(",")).accept(sess);
			final var companies = shuffle(sess.sql2x("select * from t_company").collect(mapby("id")), 10);
			final var products = shuffle(sess.sql2x("select * from t_product").collect(mapby("id")), 10);
			final var cps = new HashMap<Integer, IRecord>(); // 公司产品字典
			for (final var cent : companies.entrySet()) { // cent 公司项
				final var c = cent.getValue();
				for (final var pent : products.entrySet()) { // pent 产品项
					final var p = pent.getValue();
					final var line = REC("company_id", c.i4("id"), "product_id", p.i4("id"), //
							"attrs", p.filter("id,name,price").add(REC("quantity", 1 + rnd.nextInt(10))), //
							"create_time", now, "update_time", now); // 产品数据
					sess.sql2maybe2(insql("t_company_product", line)).map(e -> e.i4(0)) // 单个添加公司产品
							.ifPresent(id -> cps.put(id, REC("id", id).derive(line))); // 使用回写后的公司产品id生成cps公司产品字典
				} // for pent 产品项
			} // for cent 公司项

			final var cpdfms = cps.values().stream().collect(groupby("company_id", DFrame::new)); // 公司产品
			final var t_order = "t_order"; // 订单表名
			final var orderb = rb("parta,partb,lines,create_time"); // 订单结构
			final var orderdatas = new LinkedList<IRecord>(); // 订单数据
			sess.sql2execute(ctsql(t_order, orderb.prepend("id").get(0, 0, 0, REC(), now))); // 创建订单表
			for (final var partaent : shuffle(companies).entrySet()) { // partaent 甲方项
				final var parta = partaent.getValue();
				for (final var partbent : shuffle(companies).entrySet()) { // partbent 甲方项
					final var partb = partbent.getValue();
					final var lines = cpdfms.get(partb.i4("id")).stream().sorted((a, b) -> Math.random() > 0.5 ? 1 : -1) // 打乱次序
							.map(e -> e.filter("id,company_id").add(
									e.rec("attrs").alias("id,product_id,name,title,price,price,quantity,quantity")))
							.collect(DFrame.dfmclc).head(5); // 订单行：公司产品
					final var orderdata = orderb.get(parta.get("id"), partb.get("id"), lines, now);
					orderdatas.add(orderdata); // 生成订单数据
				} // for partbent 乙方项
			} // for partaent 甲方项
			final var orderids = sess.sql2executeS(insql(t_order, orderdatas)).collect(DFrame.dfmclc); // 插入订单数据
			println("插入的订单ids:", orderids);

			final Function<List<IRecord>, Object> stats_evaluator = e -> e.stream() // 订单分类统计
					.collect(summarizingDouble(r -> r.dbl("price") * r.dbl("quantity"))).getSum(); // 订单金额统计
			final var entity_sql = String.format("select distinct k from ( %s ) t", Stream.of("parta,partb".split(","))
					.map(k -> String.format("select %s k from %s", k, t_order)).collect(Collectors.joining(" union ")));
			for (final var entity_id : sess.sql2recordS(entity_sql).map(e -> e.get(0)).toList()) { // 会计主体
				final var ordersql = FT("select * from $0 where parta = $1 or partb = $1", t_order, entity_id); // 订单sql
				final var rootNode = sess.sql2recordS(ordersql).map(jscompute("lines"))
						.flatMap(e -> e.getS("lines", IRecord::REC).map(e::derive))
						.map(e -> REC("entity_id", entity_id, "drcr", e.i4("parta").equals(entity_id) ? 1 : -1)
								.add(e.filter("company_id,product_id,title,price,quantity,parta,partb"))
								.add(e.alias("id,order_id")))
						.collect(pvtreeclc2(stats_evaluator, "partb,product_id,drcr")); // 数据透视分阶层统计

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
	 * 分库&分表数据访问示例
	 */
	@Test
	public void qux() {
		final var n = 4; // 数据行长度(数据集/矩阵的列宽度)
		final var t_prefix = "t_data"; // 数据表名前缀
		final var ndatas = cph(RPTA(nats(n).data(), n)).map(e -> nd(e).dupdata()).collect(ndclc((int) pow(n, n))); // dataset,构造全排列的模拟数据集
		final var xrb = rb(nats(n).fmap(DataMatrix::index_to_excel_name)); // 采用excel列命名模式的IRecord构建器
		final var proto = xrb.get(ndatas.head().data()); // 基础结构：数据原型
		final var identity = identity(ndatas.head()); // 数据行恒等函数，用于标识类型，这里指向ndatas的数据项类型
		final var nd2rec = identity.andThen(INdarray::data).andThen(xrb::get); // 数据行IRecord构建器
		final var classifiers = nats(n).tail().reverse().fmap(i -> identity.andThen(nd -> nd.get(i))); // 枢轴脸谱函数
		final var ndapps = INdarray.from(h2_rec_1, h2_rec_2).fmap(MyDataApp::new); // 数据应用客户端
		final Function<INdarray<Integer>, Integer> dbid_f = path -> path.head() % ndapps.length(); // 数据库索引生成函数
		final Function<Integer, MyDataApp> db_f = ndapps::at; // 根据数据库索引获取数据库客户端DataApp
		final Function<INdarray<Integer>, String> tblname_f = path -> path.build1(1, 1).join(t_prefix, ""); // 表名生成函数
		final Function<INdarray<INdarray<Integer>>, Object> evaluator = nds -> { // 分库分表的并行计算,以枢轴的分类序列path做为数据分片/分组的key,进而实现分表或分库
			final var pvtpath = nds.head().pivotPath(classifiers); // 枢轴脸谱即枢轴的分类序列是由classifiers计算的分类key数组结构
			final var dbid = dbid_f.apply(pvtpath); // 数据库索引:依据枢轴脸谱确定数据库
			final var tblname = tblname_f.apply(pvtpath); // 数据库表名:依据枢轴脸谱确定表名
			final var dataApp = db_f.apply(dbid); // 数据库客户端dataApp：提取指定数据库索引所标记的数据库
			return dataApp.withTransaction(sess -> { // 分库分表的指标计算: (dbid:数据库索引,tblname:表名,rowids:行记录索引)
				if (!sess.isTablePresent(tblname)) // 数据表不存在则创建表
					sess.sqlexecute(ctsql(tblname, proto.prepend("ID", 0))); // 增加一个自增长列ID
				final var rowids = sess.sql2executeS(insql(tblname, nds.fmap(nd2rec))).collect(dfmclc).col(0); // 匹量插入&获得行记录索引rowids
				P(dbid, P(tblname, rowids)).mutate(sess::setData); // (dbid:索引,tblname:表名,rowids:行记录索引)
			}); // withTransaction
		}; // 指标计算器
		final var pivotLines = ndatas.pivotTable(evaluator, classifiers); // 使用透视表作为分库分表的并行计算的框架
		final var rootNode = REC(pivotLines).tupleS().parallel().reduce(TrieNode.of("root"), // 以REC形式分解成阶层元素(K,V)流,而后reduce成树形结构
				ndaccum((leaf, p) -> leaf.attrSet("value", p._2), TrieNode::addPart), TrieNode::merge); // 数据透视分阶层统计
		final var coordinates_rb = rb("DBID,TBL"); // 位置标志rb : coordinate record builder
		final var dfdata = rootNode.getAllLeaveS() // 提取叶子节点,属性值value的结构为:(数据库索引,表名)
				.map(e -> e.attrval((Tuple2<Integer, Tuple2<String, List<Integer>>> p) -> P(p._1, p._2._1))) // (数据库索引,表名)
				.distinct().map(p -> db_f.apply(p._1) // 根据数据坐标信息:(数据库索引,表名) 从p中提取数据应用dataApp对象
						.sqldframe(FT("select * from $0", p._2)).fmap(e -> coordinates_rb.get(p._1, p._2).add(e))) // 引入位置坐标:dbid,tbl
				.reduce(DFrame::rbind).map(e -> e.sorted(IRecord.cmp(coordinates_rb.keys()))) // 归集并排序
				.orElseGet(DFrame::new); // 提取归并结构

		println("数据透视表:\n", pivotLines);
		rootNode.forEach(e -> { // 显示分组计算结果
			println(FT("$0 $1 \t\t $2", " | ".repeat(e.getLevel() - 1), e.getName(), e.attrvalOpt().orElse(""))); // 树形结构显示
		}); // forEach
		println("tbls:\n", dfdata);
		println("size:\n", dfdata.size());
		println("原始数据:\n", ndatas.nx(1));
		println("nx.length:", ndatas.nx(1).length());
	}

	final String db_prefix = "malonylcoadb";

	/**
	 * 数据库配置
	 */
	final IRecord h2_rec_1 = IRecord.REC( //
			"url", String.format("jdbc:h2:mem:%s1;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", db_prefix), //
			"driver", "org.h2.Driver", //
			"user", "root", "password", "123456");

	/**
	 * 数据库配置
	 */
	final IRecord h2_rec_2 = IRecord.REC( //
			"url", String.format("jdbc:h2:mem:%s2;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", db_prefix), //
			"driver", "org.h2.Driver", //
			"user", "root", "password", "123456");
}
