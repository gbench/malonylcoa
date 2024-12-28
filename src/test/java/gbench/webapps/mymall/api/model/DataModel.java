package gbench.webapps.mymall.api.model;

import static gbench.util.data.DataApp.IRecord.REC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import gbench.util.array.INdarray;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.JSON;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;

/**
 * 
 */
public class DataModel {
	
	/**
	 * 
	 * @param datafile
	 */
	public DataModel(final String datafile){
		this.datafile = datafile;
	}
	
	/**
	 * 表单数据
	 * 
	 * @param name
	 * @return
	 */
	public StrMatrix shtmx(final String name) {
		
		StrMatrix mx = null;
		try (final var excel = SimpleExcel.of(datafile)) {
			mx = excel.autoDetect(name);
		}
		return mx;
	}

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
			final String inttype = String.format("%s %s", "INT", "id".equalsIgnoreCase(k) ? "AUTO_INCREMENT" : "");
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
	 * 创建表sql
	 * 
	 * @param tblname 表名
	 * @param lines   数据行(keys:[k],values:[v])
	 * @return create table sql
	 */
	public static String ctsql(final String tblname, final Tuple2<? extends String[], ? extends Object[]> lines) {
		return ctsql(tblname, REC(lines._1, lines._2));
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
	
	final String datafile;
}
