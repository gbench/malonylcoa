package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import gbench.global.Globals;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.ISqlPatternPreprocessor;
import gbench.util.jdbc.Neo4jApp;
import gbench.util.jdbc.annotation.JdbcConfig;
import gbench.util.jdbc.kvp.IRecord;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.*;
import static gbench.util.jdbc.kvp.PVec.PVEC;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;

import java.util.Random;
import java.util.stream.Collectors;

/**
 * Neo4jTest
 */
public class Neo4jTest {

	/**
	 * 测试数据库系统
	 * 
	 * @author gbench
	 *
	 */
	@JdbcConfig(url = "jdbc:neo4j:bolt://localhost/mydb?nossl", driver = "org.neo4j.jdbc.Neo4jDriver", user = "neo4j", password = "malonylcoa123456")
	public interface MyNeo4j extends IMySQL {
	}

	@Test
	public void foo() {
		final var app = new Neo4jApp(true);
		final var pvec = PVEC( //
				"张三/李四", R("rel", "喜欢", "张三.address", "上海", "张三.father", "lisi", //
						"张三.birth", now(), "李四.phone", "1812074620"),
				"李四/王五/赵六", R("rel", "喜欢", "2#address", "北京", "1#age", 25, "2#height", 1.98), //
				"赵六/朱八", R("rel", "喜欢", "2#address", "北京"), //
				"陈七/朱八", R("rel", "喜欢", "$address", "中国") //
		).fmap(IRecord::STRING2REC, o -> (IRecord) o);
		final var g = app.graph(pvec);
		println(g);

		// 数据库主要构件的创建。
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/neo4j_test.sql";
		final var database = IJdbcApp.newNsppDBInstance(sqlfile, MyNeo4j.class);
		// 根据SQL语句模板文件neo4j.sql 生成代理数据库
		final var proxy = database.getProxy();// 提取代理对象
		final var spp = proxy.findOne(ISqlPatternPreprocessor.class);// 提取SQLPattern 处理器
		final var line = spp.handle(null, null, "#createLine", null); // 提取数sql语句定义
		println("语句定义 line", line);
		final var line1 = spp.handle(null, //
				REC("a", "张三", "b", "李四", "transcode", format("TX%05d", new Random().nextInt(10000))), //
				"#createLine", null); // 替换参数
		println("替换参数,line1", line1);
	} // foo0

	/**
	 * 测试 PVec
	 */
	@Test
	public void bar() {
		final var pvec = PVEC(1, 2, 3, 4);
		println("unzip", pvec.unzip());
		println("hashCode", pvec.hashCode());
		println("clone hashCode", pvec.clone().hashCode());
		println("equals", pvec.clone().equals(pvec));
		println("=", pvec.clone() == pvec);
		println("groupBy", pvec.groupBy(Collectors.toList()));
		println("mutate", pvec.mutate(IRecord::REC));
	}

}
