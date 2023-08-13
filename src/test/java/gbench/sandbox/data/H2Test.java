package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;

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
			final var linedfm = shtmx("t_company").collect(DataApp.DFrame.dfmclc2);
			sess.sqlexecute(createsql(linedfm.get(0)));
			for (final var line : linedfm)
				sess.sql2execute(insql(line));
			final var dfm = sess.sql2x("select * from t_company");
			println(dfm);
		});
	}

	/**
	 * 创建表
	 * 
	 * @param line
	 * @return
	 */
	public static String createsql(final IRecord line) {
		final Function<Object, String> typeof = v -> "VARCHAR(256)";
		final var sql = String.format("create table %s ( %s )", "t_company", line.tupleS()
				.map(e -> String.format("%s %s", e._1, typeof.apply(e._2))).collect(Collectors.joining(",")));
		return sql;
	}

	/**
	 * 插入数据
	 * 
	 * @param line
	 * @return
	 */
	public static String insql(final IRecord line) {
		return String.format("insert into %s ( %s ) values ( %s )", "t_company",
				line.tupleS().map(e -> String.format("%s", e._1)).collect(Collectors.joining(", ")),
				line.tupleS().map(e -> String.format("'%s'", (e._2 + "").replace("'", "''")))
						.collect(Collectors.joining(", ")));
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
	 * 数据库配置
	 */
	final IRecord h2_rec = IRecord.REC( //
			"url", "jdbc:h2:mem:malonylcoadb;MODE=MYSQL;DB_CLOSE_DELAY=-1;", //
			"driver", "org.h2.Driver", //
			"user", "root", "password", "123456");
}