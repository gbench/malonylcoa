package gbench.sandbox.jdbc;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.sql.SQL.nsql;
import static gbench.util.jdbc.sql.SQL.proto_of;
import static gbench.util.jdbc.sql.SQL.sql;
import static java.time.LocalDateTime.now;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * SQL 语句使用示例
 */
public class SqlTest {

	/**
	 * 生成数据原型示例:提取每一列种数据最长的数值
	 */
	@Test
	public void foo() {

		final var rb = rb("#id,name,sex,attrs,borth,description"); // #用于标识主键
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
		final var address = REC("city", "shanghai", "district", "changning", "street", "fahuazhen road");
		final var line = REC("name", "zhangsan", "mobile", 18601690611l, //
				"password", "123456", "weight", 30, "address", address, "borth", now(), "description", "无法五天的张三");
		final var kvs2 = line.kvs2(); // 键值列表2
		final var kvs3 = line.kvs3(); // 键值列表3

		println("line", line);
		println("kvs2", kvs2);
		println("kvs3", kvs3);

		// 创建语句
		println("ctsql", sql("t_user", REC("#id", 1).derive(line)).ctsql(true, 2));

		// 插入语句,k$ 标识无需添加引号,{ 可用 用${代替
		println("insql", nsql("insert into ##tbl({foreach k$ in keys k$}) values ({foreach v in values v})",
				REC("tbl", "t_user").derive(kvs3)).format());

		// 更新语句
		final var upsql = nsql("update ##tbl set {foreach p in kvs %p.key=p.value} where id=##id",
				rb("tbl,id,kvs").get("t_user", 1, kvs2)).format();
		println("upsql", upsql);
	}

}
