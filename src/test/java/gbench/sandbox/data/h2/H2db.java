package gbench.sandbox.data.h2;

import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.io.Output.println;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import gbench.global.Globals;
import gbench.util.array.INdarray;
import gbench.util.data.DataApp;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.ExceptionalConsumer;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.JSON;
import gbench.util.data.DataApp.SQLExceptionalBiConsumer;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.tree.Node;
import gbench.util.tree.TrieNode;

/**
 * H2数据库基本操作
 */
public class H2db {

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
	 * @param lines   数据行
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
	 * 表单数据
	 * 
	 * @param name
	 * @return StrMatrix
	 */
	public static StrMatrix shtmx(final String name) {
		final var datafile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";
		StrMatrix mx = null;
		try (final var excel = SimpleExcel.of(datafile)) {
			mx = excel.autoDetect(name);
		}
		return mx;
	}

	/**
	 * 导入数据表格
	 * 
	 * @param tblnames 表名列表
	 * @return 数据表
	 */
	public static ExceptionalConsumer<DataApp.IJdbcSession<Object, DFrame>> imports(final String... tblnames) {
		return sess -> {
			for (final String tblname : tblnames) { // 遍历数据表
				final var data = shtmx(tblname).collect(DFrame.dfmclc2);
				final var line = REC();
				data.cols().forEach((k, v) -> { // 提取最长行
					v.stream().sorted((a, b) -> (b + "").length() - (a + "").length()).findFirst() //
							.ifPresent(v1 -> {
								line.add(k, v1);
							});
				});
				final var ctsql = ctsql(tblname, line);
				// System.err.println(ctsql);
				sess.sql2execute(ctsql);
				for (var ln : data) {
					sess.sql2execute(insql(tblname, ln));
				}
			} // for
		};
	}

	/**
	 * jsncompute
	 * 
	 * @param keys 键名序列
	 * @return rec->&gt;rec
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
	 * @return rec-&gt;rec
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
	 * 解析json成IRecord
	 * 
	 * @param bb 字符串字节
	 * @return Map&lt;String,Object&gt;
	 */
	public static Map<String, Object> asMap(final byte[] bb) {
		return Optional.ofNullable(json(bb)).map(IRecord::toMap).orElse(null);
	}

	/**
	 * 解析json成IRecord
	 * 
	 * @param bb 字符串字节
	 * @return IRecord
	 */
	public static IRecord json(final byte[] bb) {
		return Optional.ofNullable(bb == null ? null : new String(bb))
				.map(s -> s = s.startsWith("\"") && s.endsWith("\"")
						? s.substring(1, s.length() - 1).replace("\\\"", "\"")
						: s)
				.map(IRecord::REC).orElse(null);
	}

	/**
	 * 解析json
	 * 
	 * @param bb 字符串字节
	 * @return Json 对象
	 */
	public static Object jsons(final byte[] bb) {
		final var d = (new String(bb)).replace("\\\"", "\"");
		return JSON.parse(d.substring(1, d.length() - 1));
	}

	/**
	 * 分组归集器
	 * 
	 * @param key 键名
	 * @return 键名序号归归集器
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
	 * 树形结构的递归归集 <br>
	 * 
	 * final var rootNode = ss.reduce(Node.of("root"),ndaccum((leaf, p) ->
	 * leaf.attrSet("value", p._2), Node::of), Node::merge)
	 * 
	 * @param <K>          元组键名类型
	 * @param <V>          元组值类型
	 * @param <U>          核算器结果类型
	 * @param <P>          键值对儿类型
	 * @param <N>          节点类型
	 * @param leaf_handler (node,p)-&gt;{} 叶子节点处理函数,注意：这里的叶子指的是 成长中的叶子 而不是 最终生成树的叶子。
	 *                     所以leaf_handler其实是在每次添加一个子节点即新的叶子节点leaf的时候都会被调用，<br>
	 *                     然后这个叶子节点leaf可以再次添加更新的子节点直到没有可以添加的子节点为止，即成为了最终生成树的的叶子节点真正的叶子节点
	 * @param nodegen      节点生成器 (parent,p)->node
	 * @return 累加器 (acc,a)-&gt;acc
	 */
	public static <K, V, U, P extends Tuple2<K, V>, N> BiFunction<N, P, N> ndaccum(final BiConsumer<N, P> leaf_handler,
			final BiFunction<N, K, N> nodegen) {
		return (acc, a) -> { // 规约处理
			(new BiConsumer<N, P>() { // 使用匿名类的this对象实现FunctionalInterace递归
				@SuppressWarnings("unchecked")
				public void accept(final N parent, final P tp) { // 递归方法
					final var node = nodegen.apply(parent, tp._1);

					if ((tp._2 instanceof Map ? REC(tp._2) : tp._2) instanceof Iterable<?> ps) { // 可遍历类型,注意这里的技巧ps通过模式匹配实现了左值接收
						final var ll = ps instanceof Collection<?> cc ? cc
								: StreamSupport.stream(ps.spliterator(), true).toList();
						final var itr = ll.iterator();
						if (itr.hasNext() && itr.next() instanceof Tuple2) { // 首位元素为tuple2
							ll.forEach(_tp -> accept(node, (P) _tp)); // accept递归循环
							return; // 直接返回
						} // // 首位元素为tuple2
					} // ps
					leaf_handler.accept(node, tp); // 叶子节点的处理
				} // accept
			}).accept(acc, a);// mountf
			return acc;
		};
	}

	/**
	 * node tree collector
	 * 
	 * @param <V>          元组值类型
	 * @param <U>          核算器结果类型
	 * @param <P>          键值对儿类型
	 * @param rootgen      根节点生成器
	 * @param leaf_handler 分组核算器 (node,p) -&gt; {}
	 * @return 归集器 [p]-&gt;node
	 */
	public static <V, U, P extends Tuple2<String, V>> Collector<P, ?, Node<String>> ndtreeclc(
			final Supplier<Node<String>> rootgen, BiConsumer<Node<String>, P> leaf_handler) {
		return Collector.of(rootgen, (acc, a) -> ndaccum(leaf_handler, Node::of).apply(acc, a), Node::merge);
	}

	/**
	 * node tree collector
	 * 
	 * @param <V>      元组值类型
	 * @param <U>      核算器结果类型
	 * @param <P>      键值对儿类型
	 * @param rootgen  根节点生成器
	 * @param finisher 分组核算器 [rec] -&gt; u
	 * @return 归集器 [p]-&gt;node
	 */
	public static <V, U, P extends Tuple2<String, V>> Collector<P, ?, Node<String>> ndtreeclc(
			final Supplier<Node<String>> rootgen) {
		return ndtreeclc(rootgen, (leaf, p) -> leaf.attrSet("value", p._2));
	}

	/**
	 * pvtreeclc 数据透视表规约: node 根节点
	 * 
	 * @param <U>       核算结果类型
	 * @param evaluator 分组核算器 [rec] -&gt; u
	 * @param keys      键名列表
	 * @return 归集器 [rec]-&gt;node
	 */
	public static <U> Collector<IRecord, ?, Node<String>> pvtreeclc(final Function<List<IRecord>, U> evaluator,
			final String keys) {
		return pvtreeclc(null, evaluator, keys);
	}

	/**
	 * pvtreeclc 数据透视表规约: node 根节点
	 * 
	 * @param <U>       核算结果类型
	 * @param rootNode  根节点
	 * @param evaluator 分组核算器 [rec] -&gt; u
	 * @param keys      键名列表
	 * @return 归集器 [rec]-&gt;node
	 */
	public static <U> Collector<IRecord, ?, Node<String>> pvtreeclc(final Node<String> rootNode,
			final Function<List<IRecord>, U> evaluator, final String keys) {
		return Collectors.collectingAndThen(IRecord.pvtclc(evaluator, keys),
				rec -> rec.tupleS().parallel().reduce(Optional.ofNullable(rootNode).orElse(Node.of("root")),
						ndaccum((leaf, p) -> leaf.attrSet("value", p._2), Node::of), Node::merge));
	}

	/**
	 * pvtreeclc2 数据透视表规约: trienode 根节点
	 * 
	 * @param <U>       核算结果类型
	 * @param evaluator 分组核算器 [rec] -&gt; u
	 * @param keys      键名列表
	 * @return 归集器 [rec]-&gt;trienode
	 */
	public static <U> Collector<IRecord, ?, TrieNode<String>> pvtreeclc2(final Function<List<IRecord>, U> evaluator,
			final String keys) {
		return pvtreeclc2(null, evaluator, keys);
	}

	/**
	 * pvtreeclc2 数据透视表规约: trienode 根节点
	 * 
	 * @param <U>       核算结果类型
	 * @param rootNode  根节点
	 * @param evaluator 分组核算器 [rec] -&gt; u
	 * @param keys      键名列表
	 * @return 归集器 [rec]-&gt;trienode
	 */
	public static <U> Collector<IRecord, ?, TrieNode<String>> pvtreeclc2(final TrieNode<String> rootNode,
			final Function<List<IRecord>, U> evaluator, final String keys) {
		return Collectors.collectingAndThen(IRecord.pvtclc(evaluator, keys),
				rec -> rec.tupleS().parallel().reduce(Optional.ofNullable(rootNode).orElse(TrieNode.of("root")),
						ndaccum((leaf, p) -> leaf.attrSet("value", p._2), TrieNode::addPart), TrieNode::merge));
	}

	/**
	 * ifnull 函数,仅当 u 为非null的时候执行notnull_branch,否则返回defaultvalue
	 * 
	 * @param <U>            参数类型
	 * @param <V>            结果类型
	 * @param notnull_branch u-&gt;v
	 * @param v              默认值
	 * @return u-&gt;v
	 */
	public static <U, V> Function<U, V> ifnull(final Function<U, V> notnull_branch, final V v) {
		return ifnull(notnull_branch, (Supplier<V>) () -> v);
	}

	/**
	 * ifnull 函数,仅当 u 为非null的时候执行notnull_branch,否则返回defaultvalue
	 * 
	 * @param <U>            参数类型
	 * @param <V>            结果类型
	 * @param notnull_branch u-&gt;v
	 * @param nullbranch     默认值
	 * @return u-&gt;v
	 */
	public static <U, V> Function<U, V> ifnull(final Function<U, V> notnull_branch, final Supplier<V> nullbranch) {
		return u -> u != null ? notnull_branch.apply(u) : nullbranch.get();
	}

	/**
	 * 手动触发关 语句和结果集
	 */
	public static final SQLExceptionalBiConsumer<Statement, ResultSet> hand_close = (stmt, rs) -> {
		println("hand_close", REC("stmt", stmt, "rs", rs, "time", LocalDateTime.now()));
		if (null != rs && !rs.isClosed()) {
			rs.close();
		}

		if (null != stmt && !stmt.isClosed()) {
			stmt.close();
		}
	};

	/**
	 * 时间日期格式化器
	 */
	public final static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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