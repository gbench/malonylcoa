package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.io.Output.println;
import static gbench.util.json.MyJson.toJson;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import gbench.util.data.DataApp;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.data.MyDataApp;

/**
 * 
 */
public class H2Test {

	@Test
	public void foo() {
		new MyDataApp(h2_rec).withTransaction(sess -> {
			final String table = "t_company";
			final var linedfm = shtmx(table).collect(DataApp.DFrame.dfmclc2);
			sess.sqlexecute(createsql(table, linedfm.get(0)));
			for (final var line : linedfm)
				sess.sql2execute(insql(table, line));
			final var dfm = sess.sql2x(String.format("select * from %s", table));
			println(dfm);
		});
	}

	@Test
	public void bar() {
		new MyDataApp(h2_rec).withTransaction(sess -> {
			final var line = REC("id", 1, "name", "zhangsan", "address",
					REC("city", "shanghai", "district", "changning", "street", "fahuazhen"));
			final var table = "t_individual";
			sess.sql2execute(createsql(table, line));
			sess.sql2execute(insql(table, line));
			final var p = sess.sql2x(String.format("select * from %s", table));
			p.rowS().forEach(r -> r.compute("ADDRESS", H2Test::json)); // 地址类型转换
			println(p);
		});
	}

	/**
	 * 创建表
	 * 
	 * @param line
	 * @return
	 */
	public static String createsql(final String table, final IRecord line) {
		final Function<Object, String> typeof = v -> {
			if (v instanceof Map || v instanceof IRecord) {
				return "JSON";
			} else {
				return "VARCHAR(256)";
			}
		};
		final var sql = String.format("create table %s ( %s )", table, line.tupleS()
				.map(e -> String.format("%s %s", e._1, typeof.apply(e._2))).collect(Collectors.joining(",")));
		return sql;
	}

	/**
	 * 插入数据
	 * 
	 * @param line
	 * @return
	 */
	public static String insql(final String table, final IRecord line) {
		final Function<Object, String> v2s = v -> {
			if (v instanceof Map || v instanceof IRecord) {
				return toJson(v);
			} else {
				return (v + "").replace("'", "''");
			}
		};
		return String.format("insert into %s ( %s ) values ( %s )", table,
				line.tupleS().map(e -> String.format("%s", e._1)).collect(Collectors.joining(", ")),
				line.tupleS().map(e -> String.format("'%s'", v2s.apply(e._2))).collect(Collectors.joining(", ")));
	}

	/**
	 * 表单数据
	 * 
	 * @param name
	 * @return
	 */
	@Test
	public static StrMatrix shtmx(final String name) {
		final var datafile = "f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";
		StrMatrix mx = null;
		try (final var excel = SimpleExcel.of(datafile)) {
			mx = excel.autoDetect(name);
		}
		return mx;
	}

	/**
	 * 
	 * @param bb
	 * @return
	 */
	public static IRecord json(final byte[] bb) {
		final var d = (new String(bb)).replace("\\\"", "\"");
		return REC(d.substring(1, d.length() - 1));
	}

	/**
	 * 数据库配置
	 */
	final IRecord h2_rec = IRecord.REC( //
			"url", "jdbc:h2:mem:malonylcoadb;MODE=MYSQL;DB_CLOSE_DELAY=-1;", //
			"driver", "org.h2.Driver", //
			"user", "root", "password", "123456");
}