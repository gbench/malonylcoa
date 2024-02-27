package gbench.sandbox.data.pg;

import static gbench.util.io.Output.println;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import gbench.global.Globals;
import gbench.util.array.INdarray;
import gbench.util.data.DataApp;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.ExceptionalConsumer;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.JSON;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;

/**
 * 
 */
public class Pgdb {

	/**
	 * 创建表sql
	 * 
	 * @param tblname 表名
	 * @param line    数据行
	 * @return create table sql
	 */
	public static String ctsql(final String tblname, final IRecord line) {
		final var intpattern = "^\\d+$";
		final var datepattern = "^[1-9]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])\\s+(20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d$";
		final BiFunction<String, Object, String> typeof = (k, v) -> {
			final String inttype = String.format("%s", "id".equalsIgnoreCase(k) ? "serial" : "INT");
			if (v instanceof Map || v instanceof IRecord || v instanceof Iterable || v == Map.class
					|| v == IRecord.class
					|| ((v instanceof Class<?>) && Iterable.class.isAssignableFrom(((Class<?>) v)))) {
				return "JSON";
			} else if (v instanceof Integer || v == Integer.class) {
				return inttype;
			} else if (v instanceof Long || v == Long.class) {
				return "BIGINT";
			} else if (v instanceof Double || v == Double.class || v == Float.class) {
				return "DEC";
			} else if (v instanceof String s && s.matches(intpattern)) {
				final var lng = Long.parseLong(s);
				if (lng < Integer.MAX_VALUE)
					return inttype;
				else
					return "LONG";
			} else if (v instanceof String && ((String) v).matches(datepattern)) {
				return "TIMESTAMP";
			} else if (v instanceof String s && JSON.isJson(s)) {
				return "JSON";
			} else if (v instanceof LocalDateTime) {
				return "TIMESTAMP";
			} else {
				final var size = (int) ((v + "").length() * 1.2);
				return String.format("VARCHAR( %s )", size % 2 == 0 ? size : size + 1);
			}
		};
		final var sql = String.format("create table %s ( %s )", tblname, line.tupleS()
				.map(e -> String.format("%s %s", e._1, typeof.apply(e._1, e._2))).collect(Collectors.joining(", ")));
		return sql;
	}

	/**
	 * 插入数据sql
	 * 
	 * @param tblname 表名
	 * @param line    数据行
	 * @return insert sql
	 */
	public static String insql(final String tblname, final IRecord line) {
		return insql(tblname, Arrays.asList(line));
	}

	/**
	 * 插入数据sql
	 * 
	 * @param tblname 表名
	 * @param line    数据行
	 * @return insert sql
	 */
	public static String insql(final String tblname, final Iterable<IRecord> lines) {
		final Function<IRecord, String> value_part = line -> line.tupleS()
				.map(e -> String.format("'%s'", v2s.apply(e._2))).collect(Collectors.joining(", "));
		final StringBuilder sb = new StringBuilder(); // sql写入缓存

		for (final var line : lines) {
			if (sb.length() < 1) {// 第一行,开头部分
				sb.append(String.format("insert into %s ( %s ) values ( %s )", tblname,
						line.tupleS().map(e -> String.format("%s", e._1)).collect(Collectors.joining(", ")),
						value_part.apply(line)));
			} else { // 剩余行,追加value部分
				sb.append(String.format(", ( %s )", value_part.apply(line)));
			} // if
		} // for

		return sb.toString();
	}

	/**
	 * 插入数据sql
	 * 
	 * @param tblname 表名
	 * @param lines   数据行(keys:[k],values:[v])
	 * @return insert sql
	 */
	public static String insql(final String tblname, Tuple2<? extends String[], ? extends Object[]> lines) {
		final var keys = Arrays.stream(lines._1).collect(Collectors.joining(","));
		final var rows = INdarray.of(lines._2).cuts(lines._1.length, true);
		final Function<INdarray<?>, String> value_part = line -> line.map(e -> String.format("'%s'", v2s.apply(e)))
				.collect(Collectors.joining(", "));
		final StringBuilder sb = new StringBuilder(); // sql写入缓存
		for (final var line : rows) {
			if (sb.length() < 1) {// 第一行,开头部分
				sb.append(String.format("insert into %s ( %s ) values ( %s )", tblname, keys, value_part.apply(line)));
			} else { // 剩余行,追加value部分
				sb.append(String.format(", ( %s )", value_part.apply(line)));
			} // if
		} // for

		return sb.toString();
	}

	/**
	 * 导入数据表格
	 * 
	 * @param tblnames 表名列表
	 * @return 数据表
	 */
	public static ExceptionalConsumer<Tuple2<String, DataApp.IJdbcSession<Object, DFrame>>> imports(
			final String... tblnames) {
		return tup -> {
			final var datafile = tup._1;
			final var sess = tup._2;
			for (final var tblname : tblnames) {
				final var dfm = shtmx(datafile, tblname).collect(DFrame.dfmclc2);
				final var proto = dfm.maxBy2(compare(false, true, (String _a, String _b) -> _a.length() > _b.length()));
				final var ctsql = ctsql(tblname, proto);
				if (sess.isTablePresent(tblname)) {
					sess.sql2execute(String.format("drop table %s", tblname));
				}
				sess.sql2execute(ctsql);
				sess.sql2execute(insql(tblname, dfm));
				println(sess.sql2x(String.format("select * from %s", tblname)));
			}
		};
	};

	/**
	 * 表单数据
	 * 
	 * @param name 表单名称
	 * @return StrMatrix
	 */
	public static StrMatrix shtmx(final String name) {
		final var datafile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";
		return shtmx(datafile, name);
	}

	/**
	 * 表单数据
	 * 
	 * @param name 表单名称
	 * @return StrMatrix
	 */
	public static StrMatrix shtmx(final String datafile, final String name) {
		StrMatrix mx = null;
		try (final var excel = SimpleExcel.of(datafile)) {
			mx = excel.autoDetect(name);
		}
		return mx;
	}

	/**
	 * 二元函数
	 * 
	 * @param <V>    值类型
	 * @param t      第一元素类型
	 * @param u      第二元素类型
	 * @param vtnull T 为空值时的默认值
	 * @param vunull U 为空值时默认值
	 * @param mapper 非空变换函数 (t,u)->v
	 * @return 二元函数类型 (t,u)->bool
	 */
	public static <T, U, V> BiPredicate<T, U> compare(final Boolean vtnull, final Boolean vunull,
			final BiPredicate<T, U> mapper) {
		return (t, u) -> Optional.ofNullable(t)
				.map(_t -> Optional.ofNullable(u).map(_u -> mapper.test(_t, u)).orElse(vunull)).orElse(vtnull);
	}

	/**
	 * 时间日期格式化器
	 */
	public static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 值变换函数
	 */
	private final static Function<Object, String> v2s = v -> {
		if (v instanceof Map || v instanceof IRecord || v instanceof Iterable) {
			return JSON.toJson(v);
		} else if (v instanceof LocalDateTime ldt) {
			return ldt.format(dtf);
		} else if (v instanceof DFrame dfm) {
			return JSON.toJson(dfm);
		} else {
			return (v + "").replace("'", "''");
		}
	}; // 值书写器

}
