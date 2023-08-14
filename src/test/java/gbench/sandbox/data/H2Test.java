package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.io.Output.println;
import static gbench.util.json.MyJson.toJson;
import static gbench.sandbox.data.H2db.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import gbench.util.data.DataApp;
import gbench.util.data.DataApp.ExceptionalConsumer;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.json.MyJson;
import gbench.util.data.MyDataApp;

/**
 * 
 */
public class H2Test {

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

	@Test
	public void quz() {
		new MyDataApp(h2_rec).withTransaction(sess -> { // 准备数据
			imports("t_company,t_product,t_company_product".split(",")).accept(sess);
			final var companies = shuffle(sess.sql2x("select * from t_company").collect(mapby("id")), 10);
			final var products = shuffle(sess.sql2x("select * from t_product").collect(mapby("id")), 10);
			final var now = LocalDateTime.now();
			final var cps = new HashMap<Integer, IRecord>();
			for (final var ce : companies.entrySet()) { // 随机生成数据公司数据
				final var c = ce.getValue();
				for (final var pe : products.entrySet()) {
					final var p = pe.getValue();
					final var line = REC("company_id", c.i4("id"), "product_id", p.i4("id"), "attrs",
							p.filter("id,name,price"), "create_time", now, "update_time", now); // 产品数据
					sess.sqlexecuteS(insql("t_company_product", line)).findFirst().map(e -> e.i4(0))
							.ifPresent(id -> cps.put(id, line.add("id", id)));
				} // for
			} // for

			final var cpds = cps.values().stream().collect(groupby("company_id", DataApp.DFrame::new));
			var id = 1;
			final var t_order = "t_order";
			for (final var partae : shuffle(companies).entrySet()) {
				final var parta = partae.getValue();
				for (final var partbe : shuffle(companies).entrySet()) {
					final var partb = partbe.getValue();
					final var lines = cpds.get(partb.i4("id")).stream().sorted((a, b) -> Math.random() > 0.5 ? 1 : -1)
							.collect(DataApp.DFrame.dfmclc).head(5);
					final var order = REC("id", id++, "parta", parta.get("id"), "partb", partb.get("id"), "lines",
							lines);
					if (id <= 2) {
						sess.sql2execute(ctsql(t_order, order));
					} // if
					sess.sql2execute(insql(t_order, order));
				} // for
			} // for
			println(sess.sql2x(String.format("select * from %s", t_order)).fmap(jscompute("lines")));
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

class H2db {
	/**
	 * 创建表
	 * 
	 * @param table 表名
	 * @param line  数据行
	 * @return create table sql
	 */
	public static String ctsql(final String table, final IRecord line) {
		final var intpattern = "^\\d+$";
		final var datepattern = "^[1-9]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])\\s+(20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d$";
		final BiFunction<String, Object, String> typeof = (k, v) -> {
			final String inttype = String.format("%s %s", "INT", "id".equalsIgnoreCase(k) ? "AUTO_INCREMENT" : "");
			if (v instanceof Map || v instanceof IRecord || v instanceof Iterable) {
				return "JSON";
			} else if (v instanceof Integer) {
				return inttype;
			} else if (v instanceof Long) {
				return "BIGINT";
			} else if (v instanceof Double) {
				return "DEC";
			} else if (v instanceof String s && s.matches(intpattern)) {
				final var lng = Long.parseLong(s);
				if (lng < Integer.MAX_VALUE)
					return inttype;
				else
					return "LONG";
			} else if (v instanceof String && ((String) v).matches(datepattern)) {
				return "TIMESTAMP";
			} else if (v instanceof String s && MyJson.isJson(s)) {
				return "JSON";
			} else if (v instanceof LocalDateTime) {
				return "TIMESTAMP";
			} else {
				final var size = (int) ((v + "").length() * 1.2);
				return String.format("VARCHAR( %s )", size % 2 == 0 ? size : size + 1);
			}
		};
		final var sql = String.format("create table %s ( %s )", table, line.tupleS()
				.map(e -> String.format("%s %s", e._1, typeof.apply(e._1, e._2))).collect(Collectors.joining(", ")));
		return sql;
	}

	/**
	 * 插入数据
	 * 
	 * @param table 表名
	 * @param line  数据行
	 * @return insert sql
	 */
	public static String insql(final String table, final IRecord line) {
		final Function<Object, String> v2s = v -> {
			if (v instanceof Map || v instanceof IRecord || v instanceof Iterable) {
				return toJson(v);
			} else if (v instanceof LocalDateTime ldt) {
				return ldt.format(dtf);
			} else if (v instanceof DataApp.DFrame dfm) {
				return MyJson.toJson(dfm);
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
	 * 导入数据表格
	 * 
	 * @param tables
	 * @return 数据表
	 */
	public static ExceptionalConsumer<DataApp.IJdbcSession<Object, DataApp.DFrame>> imports(final String... tables) {
		return sess -> {
			for (final String table : tables) { // 遍历数据表
				final var data = shtmx(table).collect(DataApp.DFrame.dfmclc2);
				final var line = REC();
				data.cols().forEach((k, v) -> { // 提取最长行
					v.stream().sorted((a, b) -> (b + "").length() - (a + "").length()).findFirst() //
							.ifPresent(v1 -> {
								line.add(k, v1);
							});
				});
				final var ctsql = ctsql(table, line);
				sess.sql2execute(ctsql);
				for (var ln : data) {
					sess.sql2execute(insql(table, ln));
				}
			} // for
		};
	}

	/**
	 * jsncompute
	 * 
	 * @param keys 键名序列
	 * @return
	 */
	public static Function<IRecord, IRecord> jcompute(final String... keys) {
		return rec -> {
			for (final var key : keys) {
				rec.compute(key, H2db::json);
			}
			return rec;
		};
	}

	/**
	 * jsncompute
	 * 
	 * @param keys 键名序列
	 * @return
	 */
	public static Function<IRecord, IRecord> jscompute(final String... keys) {
		return rec -> {
			for (final var key : keys) {
				rec.compute(key, H2db::jsons);
			}
			return rec;
		};
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
	 * 
	 * @param bb
	 * @return
	 */
	public static Object jsons(final byte[] bb) {
		final var d = (new String(bb)).replace("\\\"", "\"");
		return DataApp.JSON.parse(d.substring(1, d.length() - 1));
	}

	/**
	 * 
	 * @param key 键名
	 * @return
	 */
	public static Collector<? super IRecord, ?, Map<Integer, IRecord>> mapby(final String key) {
		return IRecord.mapclc2(e -> DataApp.Tuple2.of(e.i4(key), e));
	}

	/**
	 * 
	 * @param <V>    结果类型
	 * @param key    键名
	 * @param mapper 值变换类型
	 * @return
	 */
	public static <V> Collector<? super IRecord, ?, Map<Integer, V>> groupby(final String key,
			final Function<List<IRecord>, V> mapper) {
		return Collectors.collectingAndThen(IRecord.mapclc(e -> DataApp.Tuple2.of(e.i4(key), e)), lhm -> {
			final Map<Integer, V> m = new HashMap<Integer, V>();
			lhm.forEach((k, ll) -> m.put(k, mapper.apply(ll)));
			return m;
		});
	}

	/**
	 * 洗牌排序
	 * 
	 * @param lines 源数据
	 * @return 洗牌排序
	 */
	public static LinkedHashMap<Integer, IRecord> shuffle(final Map<Integer, IRecord> lines) {
		return shuffle(lines, null);
	}

	/**
	 * 洗牌排序
	 * 
	 * @param lines   源数据
	 * @param maxsize 最大长度
	 * @return 洗牌排序
	 */
	public static LinkedHashMap<Integer, IRecord> shuffle(final Map<Integer, IRecord> lines, final Integer maxsize) {
		final Map<Integer, Integer> kk = new HashMap<Integer, Integer>();
		final LinkedHashMap<Integer, IRecord> lhm = new LinkedHashMap<Integer, IRecord>();
		final var rnd = new java.util.Random();
		lines.entrySet().stream().sorted((a, b) -> kk.computeIfAbsent(a.getKey(), e -> rnd.nextInt())
				.compareTo(kk.computeIfAbsent(b.getKey(), e -> rnd.nextInt()))).forEach(e -> {
					if (maxsize != null && lhm.size() > maxsize) {
						return;
					}
					lhm.put(e.getKey(), e.getValue());
				});
		return lhm;
	}

	public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}