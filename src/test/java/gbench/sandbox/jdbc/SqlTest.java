package gbench.sandbox.jdbc;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.nsql;
import static gbench.util.jdbc.sql.SQL.proto_of;
import static gbench.util.jdbc.sql.SQL.sql;
import static gbench.util.jdbc.sql.SQL.OpMode.*;
import static java.time.LocalDateTime.now;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.IRecord;

/**
 * SQL 语句使用示例
 */
public class SqlTest {

	/**
	 * 生成数据原型示例:提取每一列种数据最长的数值
	 */
	@Test
	public void foo() {
		final var rb = rb("#id,name,sex,attrs,birth,description"); // #用于标识主键
		final var proto = proto_of( // 原型数据
				rb.get(1, "zhangsan", "male", REC("city", "zhongguo shanghai"), LocalDate.now(), "民族英雄"), //
				rb.get(100, "lisi", "female", REC("city", "beijing"), LocalDateTime.now(), "盖世英豪,文化名人") //
		);
		println(proto);
		println(sql("t_user", proto).ctsqls(true).get(2));
	}

	/**
	 * SQL 模板使用示例,Foreach 结构
	 */
	@Test
	public void bar() {
		final var address = REC("city", "shanghai", "district", "changning", "street", "fahuazhen road", "nong", 101,
				"place", REC("building", 1, "unit", 11, "room", 201, "name", "zhangshan'home"));
		final var t_user = REC("name", "zhangsan", "mobile", 18601690611l, //
				"password", "123456", "weight", 30, "address", address, "birth", now(), "description", "无法无天的张三");
		final var kvs2 = t_user.kvs2(); // 键值列表2
		final var kvs3 = t_user.kvs3(); // 键值列表3

		println("t_user", t_user);
		println("kvs2", kvs2);
		println("kvs3", kvs3);

		// 创建语句
		final var ctsql1 = sql("t_user", REC("#id", 1).derive(t_user)).ctsql(true, 2); // t_user
		final var ctsql2 = sql("t_test", REC("#id", 1).derive(rb("a,b,c,d").get(1))).ctsql(true, 2); // t_test

		// 插入语句,k$ 标识无需添加引号,{ 可用 用${代替
		final var insql1 = nsql("insert into ##tbl({foreach k$ in keys k$}) values ({foreach v in values v})",
				REC("tbl", "t_user").derive(kvs3)).format(); // t_user的插入语句
		final var insql2 = sql("t_test", //
				rb("a,b,c").get(1, 2, 3), //
				rb("a,c").get(1, 3), //
				rb("d").get(3) //
		).insql();

		// 更新语句, %p.key表示不用添加单引号
		final var upsql1 = nsql("update ##tbl set {foreach p in kvs %p.key=p.value} where id=##id",
				rb("tbl,id,kvs").get("t_user", 1, kvs2)).format(); // t_user的字段更改
		final var upsql2 = sql("t_user").upsql(rb("*id,*weight,mobile").get(1, 30, "18601690612", 1), OR);
		final var upsql3 = sql("t_user").upsql(rb("*id,*weight,mobile").get(1, 30, "18601690613", 1), AND);
		final var upsql4 = sql("t_user").upsql(rb("*id,*weight,mobile").get(1, 30, "18601690614", 1));

		// sql 生成的新建、增加、与修改
		println("ctsql 1", ctsql1);
		println("ctsql 2", ctsql2);
		println("insql 1", insql1);
		println("insql 2", insql2);
		println("upsql 1", upsql1);
		println("upsql 2", upsql2);
		println("upsql 3", upsql3);
		println("upsql 4", upsql4);

		// 数据联系
		jdbcApp.withTransaction(sess -> {
			final var tables = Arrays.asList("t_user,t_test".split(","));
			for (var table : tables) { // 清除数据表
				sess.sql2execute(String.format("drop table if exists %s", table));
			}
			for (var sql : Arrays.asList(ctsql1, ctsql2, insql1, insql2, upsql1, upsql2, upsql3, upsql4)) {
				sess.sqlexecute(sql);
			} // for

			for (var tbl : tables) {
				final var hjp = switch (tbl) { // json 字段处理器
				case "t_user" -> h2_json_processor("address");
				default -> null;
				}; // hjp
				println(tbl, sess.sql2dframe("select * from ##tbl", "tbl", tbl).forEachBy(hjp));
			} // for
		}); // withTransaction
	}

	final String dbname = "sqltest"; // 更换一个数据库
	final String url = String.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", dbname);
	final IRecord h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456");
	final IMySQL jdbcApp = IJdbcApp.newNsppDBInstance(null, IMySQL.class, h2_rec); // 数据应用

}
