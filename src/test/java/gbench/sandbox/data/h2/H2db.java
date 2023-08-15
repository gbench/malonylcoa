package gbench.sandbox.data.h2;

import static gbench.util.data.DataApp.IRecord.REC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import gbench.util.data.DataApp;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.ExceptionalConsumer;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.JSON;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.tree.Node;

public class H2db {
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
			} else if (v instanceof String s && JSON.isJson(s)) {
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
				return JSON.toJson(v);
			} else if (v instanceof LocalDateTime ldt) {
				return ldt.format(dtf);
			} else if (v instanceof DFrame dfm) {
				return JSON.toJson(dfm);
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
	public static ExceptionalConsumer<DataApp.IJdbcSession<Object, DFrame>> imports(final String... tables) {
		return sess -> {
			for (final String table : tables) { // 遍历数据表
				final var data = shtmx(table).collect(DFrame.dfmclc2);
				final var line = REC();
				data.cols().forEach((k, v) -> { // 提取最长行
					v.stream().sorted((a, b) -> (b + "").length() - (a + "").length()).findFirst() //
							.ifPresent(v1 -> {
								line.add(k, v1);
							});
				});
				final var ctsql = ctsql(table, line);
				// System.err.println(ctsql);
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
	 * jscompute
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
		return JSON.parse(d.substring(1, d.length() - 1));
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
					} else {
						lhm.put(e.getKey(), e.getValue());
					}
				});
		return lhm;
	}

	/**
	 * 树形结构的递归归集
	 * 
	 * final var rootNode = ss.reduce(Node.of("root"), node_accum(e -> e),
	 * Node::merge);
	 * 
	 * @param <T>       元素值的参数类型
	 * @param evaluator t->u
	 * @return (acc,a)->acc
	 */
	public static <T, U> BiFunction<Node<String>, ? super Tuple2<String, Object>, Node<String>> node_accum(
			final Function<T, U> evaluator) {
		return (acc, a) -> { // 规约处理
			(new BiConsumer<Node<String>, Tuple2<String, Object>>() { // 使用匿名类的this对象实现FunctionalInterace递归
				@SuppressWarnings("unchecked")
				public void accept(final Node<String> parent, final Tuple2<String, Object> tp) { // 递归方法
					final var node = Node.of(parent, tp._1);
					if (tp._2 instanceof IRecord rec) {
						rec.tupleS().parallel().forEach(_tp -> this.accept(node, _tp)); // 递归
					} else { // 值计算
						node.attrSet("value", evaluator.apply((T) tp._2));
					} // if
				} // accept
			}).accept(acc, a);// mountf
			return acc;
		};
	}

	public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}