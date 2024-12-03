package gbench.util.jdbc;

import static gbench.util.jdbc.sql.SQL.ctsql;
import static gbench.util.jdbc.sql.SQL.insql;
import static gbench.util.jdbc.sql.SQL.proto_of;
import static java.util.stream.Collectors.groupingBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.Random;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.function.ThrowableBiConsumer;
import gbench.util.jdbc.function.ExceptionalConsumer;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.jdbc.function.ExceptionalPredicate;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Json;
import gbench.util.jdbc.kvp.KVPair;
import gbench.util.jdbc.kvp.SimpleRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.tree.Node;
import gbench.util.tree.TrieNode;

import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Jdbcs基础工具类库 <br>
 * 这是一个用于数据分析的工具箱<br>
 * <p>
 * IRecord 是 对数据记录的抽象 SQL 是对 数据操作语言的抽象 Node 是对树结构的节点的抽象
 *
 * @author gbench
 */
public class Jdbcs {

	/**
	 * 这是对一下的数据结构进行实现。<br>
	 * LittleTree.buildTree(categories,0,REC2("id","id","pid","pid"));<br>
	 * 从List&lt;IRecord&gt;结构生成节点树 <br>
	 * 根节点id 默认为 "0"; <br>
	 * 
	 * @param recs 节点集合，节点需要包含由id,pid两个字段
	 * @return Node &lt;IRecord&gt; 类型的根节点
	 */
	public static Node<IRecord> buildTree(final List<IRecord> recs) {
		return buildTree(recs, "0");
	}

	/**
	 * 这是对一下的数据结构进行实现。<br>
	 * LittleTree.buildTree(categories,0,REC2("id","id","pid","pid")); <br>
	 * 从List&lt;IRecord&gt; 结构生成节点树 <br>
	 * 根节点id 默认为 "0"; <br>
	 * 
	 * @param <T>  节点元素的值类型
	 * @param <U>  返回结果类型
	 * @param recs 节点集合，节点需要包含由id,pid两个字段
	 * @return Node &lt;IRecord&gt; 类型的根节点
	 */
	public static <T, U extends Node<T>> U buildTree2(final List<IRecord> recs,
			final Function<Node<IRecord>, U> node_mapper) {
		return buildTree(recs, "0", node_mapper);
	}

	/**
	 * 构建一棵树
	 * 
	 * @param <T>          节点元素的值类型
	 * @param sup_root     根节点函数 <br>
	 * @param get_children 子节点函数 <br>
	 * @return 树形结构的根节点： Node &lt;T&gt; 类型的根节点
	 */
	public static <T> Node<T> buildTree(final Supplier<Node<T>> sup_root,
			final Function<Node<T>, List<Node<T>>> get_children) {

		final Node<T> root = sup_root.get();

		final Stack<Node<T>> stack = new Stack<>();
		stack.push(root);// 根节点入栈，随后开始进行子节点遍历

		while (!stack.empty()) {
			Node<T> node = stack.pop();
			List<Node<T>> cc = get_children.apply(node);
			if (cc.size() > 0) {
				stack.addAll(cc);
				node.addChildren(cc);
				// System.out.println(node+"：子节点为："+cc);
			} else {
				// System.out.println(node+"：没有子节点");
			}
		} // while

		return root;
	}

	/**
	 * 这个应该是 最常用的生成结构 <br>
	 *
	 * 从List&lt;IRecord&gt;结构生成节点树 <br>
	 *
	 * @param recs   节点集合，节点需要包含由id,pid两个字段
	 * @param rootId 根节点id， 根节点是通过字符串即对象的toString然后trim 的形式进行比较的。
	 * @return 树形结构的根节点： Node &lt;IRecord&gt; 类型的根节点
	 */
	public static Node<IRecord> buildTree(List<IRecord> recs, Object rootId) {
		return buildTree(recs, rootId, SimpleRecord.REC2("id", "id", "pid", "pid"));
	}

	/**
	 * 这个应该是 最常用的生成结构 <br>
	 *
	 * 从List&lt;IRecord&gt;结构生成节点树 <br>
	 *
	 * @param <T>         节点元素的值类型
	 * @param <U>         返回结果类型
	 * @param recs        节点集合，节点需要包含由id,pid两个字段
	 * @param rootId      根节点id， 根节点是通过字符串即对象的toString然后trim 的形式进行比较的。
	 * @param node_mapper 节点构建器
	 * @return 树形结构的根节点： U 类型的根节点
	 */
	public static <T, U extends Node<T>> U buildTree(List<IRecord> recs, Object rootId,
			final Function<Node<IRecord>, U> node_mapper) {
		return buildTree(recs, rootId, SimpleRecord.REC2("id", "id", "pid", "pid"), node_mapper);
	}

	/**
	 * 这个应该是 最常用的生成结构：自动补充根节点 <br>
	 *
	 * 从List&lt;IRecord&gt;结构生成节点树 <br>
	 * 当rootId 在List&lt;IRecord&gt; 中不存在的时候,在地宫创建一个根节点 <br>
	 * REC("id",rootId,"name","根节点","pid",null) 添加到recs之中 <br>
	 *
	 * @param recs   节点集合，节点需要包含由id,pid两个字段
	 * @param rootId 根节点id， 根节点是通过字符串即对象的toString然后trim 的形式进行比较的。
	 * @return 树形结构的根节点： Node &lt;IRecord&gt; 类型的根节点
	 */
	public static Node<IRecord> buildTree2(final List<IRecord> recs, final Object rootId) {
		final var b = recs.stream().anyMatch(e -> e.get("id").equals(rootId));
		if (!b)
			recs.add(IRecord.REC("id", rootId, "name", "根节点", "pid", null));
		return buildTree(recs, rootId, SimpleRecord.REC2("id", "id", "pid", "pid"));
	}

	/**
	 * 从List&lt;IRecord&gt;结构生成节点树 <br>
	 * 使用示例: <br>
	 * LittleTree.buildTree(categories,0,REC2("id","id","pid","p_id")); <br>
	 *
	 * @param recs     节点集合，节点需要包含由id,pid两个字段
	 * @param rootId   根节点id， 根节点是通过字符串即对象的toString然后trim 的形式进行比较的。
	 * @param mappings 节点id,父节点id 与IRecord集合recs中的属性字段的对应关系
	 * @return 树形结构的根节点： Node &lt;IRecord&gt; 类型的根节点
	 */
	public static Node<IRecord> buildTree(List<IRecord> recs, Object rootId, IRecord mappings) {
		return buildTree(recs, rootId, mappings.toMap());
	}

	/**
	 * 从List&lt;IRecord&gt;结构生成节点树,生成一个U结构的树 <br>
	 * 使用示例: LittleTree.buildTree(categories,0,REC2("id","id","pid","p_id")); <br>
	 * 
	 * @param <T>         节点元素的值类型
	 * @param <U>         付汇结果的节点类型
	 * @param recs        节点集合，节点需要包含由id,pid两个字段
	 * @param rootId      根节点id， 根节点是通过字符串即对象的toString然后trim 的形式进行比较的。
	 * @param mappings    节点id,父节点id 与IRecord集合recs中的属性字段的对应关系
	 * @param node_mapper 节点构建器
	 * @return 树形结构的根节点：U 类型的根节点
	 */
	public static <T, U extends Node<T>> U buildTree(final List<IRecord> recs, final Object rootId,
			final IRecord mappings, final Function<Node<IRecord>, U> node_mapper) {

		if (node_mapper == null || recs == null || mappings == null || rootId == null)
			return null;
		final Node<IRecord> root = buildTree(recs, rootId, mappings);
		final Map<String, U> nodeCache = new HashMap<>();
		final U uroot = node_mapper.apply(root);
		nodeCache.put(root.getPath(), uroot);
		root.forEach(e -> {
			if (e.getParent() == null)
				return;
			final U pcategory = nodeCache.get(e.getParentPath());// 获得父节点
			final U catnode = node_mapper.apply(e);
			pcategory.addChild(catnode);
			nodeCache.put(e.getPath(), catnode);
		});
		return uroot;
	}

	/**
	 * 从List&lt;IRecord&gt;结构生成节点树 <br>
	 *
	 * @param recs   字段书加粗
	 * @param rootId 根节点id， 根节点是通过字符串即对象的toString然后trim 的形式进行比较的。
	 * @return 树形结构的根节点： Node &lt;IRecordT&gt; 类型的根节点
	 */
	public static Node<IRecord> buildTree(final List<IRecord> recs, final Object rootId,
			final Map<String, ?> mappings) {
		// 获得根节点
		final String id = (mappings.get("id") + "").trim();
		final String pid = (mappings.get("pid") + "").trim();
		Optional<IRecord> optRoot = SimpleRecord.fetchOne(recs, id, rootId);

		if (!optRoot.isPresent())
			optRoot = SimpleRecord.fetchOne(
					recs.stream().map(e -> e.duplicate().set("id", e.get("id") + "")).collect(Collectors.toList()), id,
					rootId + "");// 转换成字符串类型进行比较

		if (!optRoot.isPresent()) {
			System.out.println("无法确认根节点(不存在根节点：id/" + rootId + ")");
			return null;
		} // if

		final IRecord final_root = optRoot.get();
		// 获取根节点
		final Supplier<Node<IRecord>> sp_root = () -> new Node<>(final_root);
		// 获得子节点
		final Function<Node<IRecord>, List<Node<IRecord>>> get_children = (node) -> {
			final var nid = node.get(id);
			return null == nid ? new ArrayList<Node<IRecord>>() : recs.stream().filter(e -> { // 父子节点关系
				final var b = nid.equals(e.get(pid)) || (nid.toString()).equals(e.str(pid));
				return b;
			}).map(Node::new) // 构造节点
					.collect(Collectors.toList());
		}; // get_children
			// 创建树结构
		final Node<IRecord> tree = Jdbcs.buildTree(sp_root, get_children);
		return tree;
	}

	/**
	 * 创建属性结构
	 * 
	 * @param paths,       每条路径采用一维数组进行表示， 路径列表 [a,b,c1],[a,b,c2],....
	 * @param node_creator 节点创建函数 （节点名称->节点数据)
	 * @return 树形结构的根节点： Node &lt;T&gt; 类型的根节点
	 */
	public static <T> Node<T> buildTree(final List<String[]> paths, final Function<String, Node<T>> node_creator) {

		final Map<String, Node<T>> map = new LinkedHashMap<>();
		final Function<List<String>, String> buildpath = list -> String.join("/", list);
		paths.forEach(ss -> {
			final List<String> list = new LinkedList<>();
			for (String s : ss) {
				final String parent = buildpath.apply(list);
				list.add(s);
				final String cur = buildpath.apply(list);
				if (map.get(cur) == null)
					map.put(cur, node_creator.apply(s));

				final Node<T> pnode = map.get(parent);
				final Node<T> node = map.get(cur);
				if (pnode != null && !pnode.hasChild(node))
					pnode.addChild(node);
			} // for
		});

		final Node<T> root = map.values().iterator().next();
		return root;
	}

	/**
	 * 遍历树形结构
	 * 
	 * @param root  根节点
	 * @param cs    回调函数
	 * @param level 阶层数
	 */
	protected static <T> void traverse(final Node<T> root, final BiConsumer<Node<T>, Integer> cs, final int level) {

		if (root != null) {
			cs.accept(root, level);
			root.getChildren().forEach(e -> traverse(e, cs, level + 1));
		} // if
	}

	/**
	 * 遍历树形结构
	 * 
	 * @param root  根节点
	 * @param cs    回调函数
	 * @param level 阶层数
	 */
	protected static <T> void traverse_throws(final Node<T> root, final ThrowableBiConsumer<Node<T>, Integer> cs,
			final int level) throws Exception {
		if (root != null) {
			if (cs.accept(root, level))
				for (Node<T> e : root.getChildren())
					traverse_throws(e, cs, level + 1);
		} // if
	}

	/**
	 * 遍历树形结构
	 * 
	 * @param root 根节点
	 * @param cs   回调函数 返回 false 终止遍历.
	 * @throws Exception
	 */
	public static <T, U extends Node<T>> void traverse_throws2(final U root, final ThrowableBiConsumer<U, Integer> cs)
			throws Exception {
		traverse_throws2(root, cs, 0);
	}

	/**
	 * 遍历树形结构
	 * 
	 * @param root  根节点
	 * @param cs    回调函数
	 * @param level 阶层数
	 */
	@SuppressWarnings("unchecked")
	protected static <T, U extends Node<T>> void traverse_throws2(final U root,
			final ThrowableBiConsumer<? super U, Integer> cs, int level) throws Exception {

		if (root != null) {
			ThrowableBiConsumer<U, Integer> cs1 = (ThrowableBiConsumer<U, Integer>) cs;
			if (cs.accept(root, level))
				for (Node<T> e : root.getChildren())
					traverse_throws2((U) e, cs1, level + 1);
		} // if
	}

	/**
	 * 遍历树形结构
	 * 
	 * @param <T>  值类型
	 * @param <U>  遍历节点类型 Node &lt; T &gt;
	 * @param root 根节点
	 * @param cs   回调函数 （node,level)->{} node 节点数据,level
	 *             层级高度,0开始,即根节点所在层为0，其子节点层号为1,按照子节点是父节点层号+1的规则依次递推。
	 */
	@SuppressWarnings("unchecked")
	public static <T, U extends Node<T>> void traverse(final U root, final BiConsumer<U, Integer> cs) {

		traverse((Node<T>) root, (BiConsumer<Node<T>, Integer>) (Object) cs, 0);
	}

	/**
	 * 遍历树形结构:从叶子节点向根节点进行遍历
	 * 
	 * @param <T>  值类型
	 * @param <U>  遍历节点类型 Node &lt; T &gt;
	 * @param node 根节点
	 * @param cs   回调函数 （node,level)->{} node 节点数据,level
	 *             层级高度,0开始,即根节点所在层为0，其子节点层号为1,按照子节点是父节点层号+1的规则依次递推。
	 */
	@SuppressWarnings("unchecked")
	public static <T, U extends Node<T>> void traverse2(final U node, final BiConsumer<U, Integer> cs) {

		if (node.isLeaf()) {
			cs.accept((U) node, node.getLevel());
		} else {
			node.getChildren().forEach(child -> traverse2((U) child, cs));
			cs.accept((U) node, node.getLevel());
		} // if
	}

	/**
	 * 遍历树形结构
	 * 
	 * @param root 根节点
	 * @param cs   回调函数 返回 false 终止遍历.
	 * @throws Exception
	 */
	public static <T> void traverse_throws(final Node<T> root, final ThrowableBiConsumer<Node<T>, Integer> cs)
			throws Exception {

		traverse_throws(root, cs, 0);
	}

	/**
	 * 缩进尺度
	 * 
	 * @param n 缩进记录
	 * @return 缩进字符串
	 */
	public static <T> String ident(final int n) {

		return String.join("", rep("\t", n));
	}

	/**
	 * 位数精度:fraction的别名
	 * 
	 * @param n 位数的长度
	 * @return 小数位置的精度
	 */
	public static Function<Object, String> frt(final int n) {

		return fraction(n);
	}

	/**
	 * 位数精度
	 * 
	 * @param n 位数的长度
	 * @return 小数位置的精度
	 */
	public static Function<Object, String> fraction(final int n) {

		return v -> {
			if (v == null)
				return "(null)";
			var line = "{0}";// 数据格式化
			if (v instanceof Date) {
				line = "{0,Date,yyyy-MM-dd HH:mm:ss}"; // 时间格式化
			} else if (v instanceof Number) {
				line = "{0,number," + ("#." + "#".repeat(n)) + "}"; // 数字格式化
			} // if
			return MessageFormat.format(line, v);
		};// cell_formatter
	}

	/**
	 * 重n个对象放在列表
	 * 
	 * @param <T> 元素类型
	 * @param obj 对象
	 * @param n   必须大于0
	 * @return T 重复n 次后的序列
	 */
	public static <T> List<T> rep(final T obj, final int n) {

		return Stream.iterate(0, i -> i + 1).limit(Math.max(n, 0)).map(e -> obj).collect(Collectors.toList());
	}

	/**
	 * 缩进尺度
	 * 
	 * @param ws 空白:whitespace
	 * @param n  缩进记录
	 * @return 缩进字符串
	 */
	public static String ident(final String ws, final int n) {

		return String.join("", rep(ws, n));
	}

	/**
	 * 生成Json的树形解构
	 * 
	 * @param node
	 * @return json
	 */
	public static String json(final Node<IRecord> node) {

		final StringBuffer buffer = new StringBuffer();
		traverse4json(node, 0, buffer);
		return buffer.toString();
	}

	/**
	 * 生成一个json的树形结构
	 * 
	 * @param node 节点
	 */
	public static String traverse4json(final Node<IRecord> node) {

		final StringBuffer buffer = new StringBuffer();// 构建一个局部变量用户递归遍历
		traverse4json(node, 0, buffer);
		return buffer.toString();
	}

	/**
	 * 为了json而缓存
	 * 
	 * @param node   节点
	 * @param level  层级
	 * @param buffer 遍历缓存
	 */
	private static StringBuffer traverse4json(final Node<IRecord> node, final int level, final StringBuffer buffer) {

		Function<IRecord, String> rec2jsn = (rec) -> rec.tupleS().map(e -> "\"" + e.key() + "\":\"" + e.value() + "\"")
				.collect(Collectors.joining(","));

		// 打印jsn
		buffer.append(ident(level)).append("{").append(rec2jsn.apply(node.getValue()));

		// 非叶子节点
		if (!node.isLeaf()) {
			buffer.append(",children:[\n");
			node.getChildren().forEach(e -> traverse4json(e, level + 1, buffer));
		}

		boolean b = false;// 是否是子节点中的最后一个
		if (node.getParent() != null) {
			List<Node<IRecord>> cc = node.getParent().getChildren();
			if (cc.size() > 0) {
				if (node.equals(cc.get(cc.size() - 1)))
					b = true;
			}
		}

		// 收尾节点
		buffer.append(ident(level)).append(node.isLeaf() ? "" : "]").append("}")
				.append(node.isRoot() ? "" : (b ? "" : ",")).append("\n");

		return buffer;
	}

	/**
	 * 强制类型转换 Force Cast
	 * 
	 * @param <T> 元数据类型
	 * @param <U> 目标类型
	 * @return 一个强制类型转换的函数。T -> U 类型的函数
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Function<T, U> FC(final Class<U> u) {
		return o -> (U) o;
	}

	/**
	 * 字符串解析: "1:10",解析成 1,2,3,4,5,6,7,8,9 1 inclusive, 10 exclusive
	 * 
	 * @param line 字符串描述
	 * @return 数组序列。
	 */
	public static Integer[] NN(final String line) {

		return NATS(line).toArray(new Integer[0]);
	}

	/**
	 * 字符串序列解析(函数): "1:10",解析成 1,2,3,4,5,6,7,8,9 1 inclusive, 10 exclusive
	 * 
	 */
	public static Function<String, Integer[]> series = Jdbcs::NN;

	/**
	 * 字符串解析: "1:10",解析成 1,2,3,4,5,6,7,8,9 1 inclusive, 10 exclusive
	 * 
	 * @param line
	 * @return 数组序列。
	 */
	public static List<Integer> NATS(final String line) {

		final var ll = new LinkedList<Integer>();
		final var matcher = Pattern.compile("\\s*([0-9-]+)\\s*:\\s*([0-9-]+)\\s*(:\\s*([0-9-]+))?\\s*").matcher(line);

		if (matcher.matches()) {
			try {
				final var n = matcher.groupCount();
				final var start = Integer.parseInt(matcher.group(1));
				final var end = Integer.parseInt(matcher.group(2));
				final var sign = end >= start ? 1 : -1;
				var step = sign;// 步长
				final var s_step = matcher.group(4);
				if (n > 2 && s_step != null)
					step = sign * Math.abs(Integer.parseInt(s_step));
				for (var i = start; (sign > 0 ? i < end : i > end); i += step)
					ll.add(i);
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		} // if

		return ll;
	}

	/**
	 * 自然数子集合．0,1,2,3,.... 生成一个特定长度的字数字流,从０开始
	 * 
	 * @param size 流的长度,n 为null 或者0,负数返回无限流.
	 * @return 序列长度
	 */
	public static Stream<Long> NATS(final Integer size) {

		if (size == null || size <= 0)
			NATS();
		return Stream.iterate(0L, (Long i) -> i + 1L).limit(size);
	}

	/**
	 * 自然数子集合．0,1,2,3,.... 生成一个无限长度的数字流.0,1,2,3,....
	 * 
	 * @return 数字序列
	 */
	public static Stream<Long> NATS() {

		return Stream.iterate(0L, (Long i) -> i + 1L);
	}

	/**
	 * double 随机数 : DouBLs
	 * 
	 * @param n 随机数的superior 上限
	 * @return 浮点数的数字
	 */
	public static Double[] DBLS(final int n) {

		return NATS(n).map(e -> new Random().nextDouble()).toArray(Double[]::new);
	}

	/**
	 * double 随机数 : DouBLs
	 * 
	 * @param n 随机数的superior 上限
	 * @return 浮点数的数字
	 */
	public static double[] dbls(final int n) {

		return NATS(n).map(e -> new Random().nextDouble()).mapToDouble(e -> e).toArray();
	}

	/**
	 * double 随机数 :DouBLs Stream
	 * 
	 * @param n 随机数的superior 上限
	 * @return 浮点数的数字的流
	 */
	public static Stream<Double> DBLSTREAM(final int n) {

		return NATS(n).map(e -> new Random().nextDouble());
	}

	/**
	 * double 随机数
	 * 
	 * @param n          随机数的superior
	 * @param nfractions 小数的位数
	 * @return
	 */
	public static Double RNDDBL(final int n, final int nfractions) {

		final Number d = Math.random() * n;
		StringBuilder f = new StringBuilder("#.");
		for (var i = 0; i < nfractions; i++)
			f.append("0");
		var df = new DecimalFormat(f.toString());
		String s = df.format(d);
		// System.out.println(s);
		return Double.parseDouble(s);
	}

	/**
	 * int 随机数
	 * 
	 * @param n 随机数的superior
	 * @return 随机整数
	 */
	public static int RNDINT(final int n) {

		return new Random().nextInt(n);
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 <br>
	 * LIST的医用用途就是去克隆列表。比如： <br>
	 * ------------------------------- <br>
	 * var a = asList(0,1,2,3,4,5,6,7); <br>
	 * var b = LIST(a.subList(1, 4)); <br>
	 * b.addAll(asList(9,10,11)); <br>
	 * 
	 * a:[0, 1, 2, 3, 4, 5, 6, 7] <br>
	 * b:[1, 2, 3, 9, 10, 11] <br>
	 * -------------------------------- <br>
	 * 对比： <br>
	 * var a = new LinkedList&lt;Integer&gt;();// 使用链表方便进行拆解。 <br>
	 * a.addAll(asList(0,1,2,3,4,5,6,7)); <br>
	 * var b = a.subList(1, 4);// 返回的是一个视图 <br>
	 * b.addAll(asList(9,10,11)); <br>
	 * 
	 * a:[0, 1, 2, 3, 9, 10, 11, 4, 5, 6, 7] <br>
	 * b:[1, 2, 3, 9, 10, 11] <br>
	 * 
	 * @param <T>  元素类型
	 * @param coll 流对象
	 * @return coll元素组成的列表，注意这是一个coll对象浅层克隆的版本。
	 * @return T类型的列表
	 */
	public static <T> List<T> LIST(final Collection<T> coll) {

		if (coll == null)
			return null;
		return LIST(coll.stream());
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T> 元素类型
	 * @param tt  流对象
	 * @return T类型的列表
	 */
	public static <T> List<T> LIST(final T[] tt) {

		if (tt == null)
			return null;
		return LIST(Arrays.stream(tt));
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>    元素类型
	 * @param stream 流对象
	 * @return T类型的列表
	 */
	public static <T> List<T> LIST(final Stream<T> stream) {

		if (stream == null)
			return null;
		return stream.collect(Collectors.toList());
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>    mapper 定义域
	 * @param <U>    mapper 值域
	 * @param stream 流对象
	 * @param mapper 转换器
	 * @return T类型的列表
	 */
	public static <T, U> List<U> MAP(final Stream<T> stream, final Function<T, U> mapper) {

		if (stream == null)
			return null;
		return stream.map(mapper).collect(Collectors.toList());
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些<br>
	 * 把一个T类型的List转换成一个U类型的 List<br>
	 * 
	 * @param <T>    mapper 定义域
	 * @param <U>    mapper 值域
	 * @param coll   T类型的集合对象
	 * @param mapper 转换器
	 * @return U类型的列表
	 */
	public static <T, U> List<U> MAP(final Collection<T> coll, final Function<T, U> mapper) {

		if (coll == null)
			return null;
		return MAP(coll.stream(), mapper);
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 把一个T类型的List转换成一个R类型对象
	 * 
	 * @param <T>       collector 元数据类型
	 * @param <A>       collector 累加器类型
	 * @param <R>       collector 规约值类型
	 * @param coll      集合对象
	 * @param collector 转换器
	 * @return R的规约值类型
	 */
	public static <T, A, R> R COLLECT(final Collection<T> coll, final Collector<? super T, A, R> collector) {

		if (coll == null)
			return null;
		return coll.stream().collect(collector);
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 <br>
	 * 把一个T类型的List转换成一个R类型对象 <br>
	 * 
	 * @param <T>    collector 元数据类型
	 * @param <A>    collector 累加器类型
	 * @param <R>    collector 规约值类型
	 * @param stream T类型的流对象
	 * @return R的规约值类型
	 */
	public static <T, A, R> R COLLECT(final Stream<T> stream, final Collector<? super T, A, R> collector) {

		if (stream == null)
			return null;
		return stream.collect(collector);
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 把一个T类型的List转换成一个 用逗号进行
	 * 
	 * @param coll 流对象
	 * @return 分类征集
	 */
	public static String COMMASTR(final Collection<?> coll) {

		if (coll == null)
			return null;
		return SEQSTR(coll.stream(), ",");
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 把一个T类型的List转换成一个 用逗号进行
	 * 
	 * @param stream 流对象
	 * @return 采用逗号“,”进行分割后的字符串对象
	 */
	public static String COMMASTR(final Stream<?> stream) {

		if (stream == null)
			return null;
		return SEQSTR(stream, ",");
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 分隔符字符串序列
	 * 
	 * @param coll      流对象
	 * @param separator 转换器
	 * @return 采用separator进行分割后的字符串对象
	 */
	public static String SEQSTR(final Collection<?> coll, final String separator) {

		if (coll == null)
			return null;
		return SEQSTR(coll.stream(), separator);
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 分隔符字符串序列
	 * 
	 * @param stream    流对象
	 * @param separator 转换器
	 * @return 采用separator进行分割后的字符串对象
	 */
	public static String SEQSTR(final Stream<?> stream, final String separator) {

		if (stream == null)
			return null;
		return stream.map(Object::toString).collect(Collectors.joining(separator));
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>     流的元素类型
	 * @param stream  T类型流对象
	 * @param pfilter 流对象过滤器
	 * @return T 类型的列表
	 */
	public static <T> List<T> FILTER(final Stream<T> stream, final Predicate<T> pfilter) {

		if (stream == null)
			return null;
		if (pfilter == null)
			return LIST(stream);
		return stream.filter(pfilter).collect(Collectors.toList());
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>     流的元素类型
	 * @param <U>     目标流的元素类型
	 * @param stream  流对象
	 * @param pfilter 流对象过滤器
	 * @return U 类型的列表
	 */
	public static <T, U> List<U> FILTER(final Stream<T> stream, final Predicate<T> pfilter,
			final Function<T, U> mapper) {

		if (stream == null)
			return null;
		if (pfilter == null)
			return MAP(stream, mapper);
		return stream.filter(pfilter).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>     流的元素类型
	 * @param coll    集合类
	 * @param pfilter 流对象过滤器
	 * @return T 类型的列表
	 */
	public static <T> List<T> FILTER(final Collection<T> coll, final Predicate<T> pfilter) {

		if (coll == null)
			return null;
		if (pfilter == null)
			return LIST(coll.stream());
		return coll.stream().filter(pfilter).collect(Collectors.toList());
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>     流的元素类型
	 * @param <U>     目标流的元素类型
	 * @param coll    集合类
	 * @param pfilter 流对象过滤器
	 * @return U类型列表
	 */
	public static <T, U> List<U> FILTER(final Collection<T> coll, final Predicate<T> pfilter,
			final Function<T, U> mapper) {

		if (coll == null)
			return null;
		if (pfilter == null)
			return MAP(coll.stream(), mapper);
		return coll.stream().filter(pfilter).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param coll       T类型的数据集合
	 * @param classifier 分类器
	 * @return 以K为键类型 T的列表为值得 Map结构
	 */
	public static <T, K> Map<K, List<T>> GROUPBY(final Collection<T> coll, final Function<T, K> classifier) {

		if (coll == null)
			return new HashMap<>();
		return GROUPBY(coll.stream(), classifier);
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param stream     T类型的数据流
	 * @param classifier 分类器
	 * @return 以K为键类型 T的列表为值得 Map结构
	 */
	public static <T, K> Map<K, List<T>> GROUPBY(final Stream<T> stream, final Function<T, K> classifier) {

		if (stream == null)
			return new HashMap<>();
		return stream.collect(Collectors.groupingBy(classifier));
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param coll       集合数据
	 * @param classifier 分类器
	 * @return 以K为键类型 T的列表为值得 Map结构
	 */
	public static <T, K> Map<K, T> GROUP2MAP(final Collection<T> coll, final Function<T, K> classifier) {

		return GROUP2MAP(coll.stream(), classifier);
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param stream     集合数据
	 * @param classifier 分类器
	 * @return 以K为键类型 T的列表为值得 Map结构
	 */
	public static <T, K> Map<K, T> GROUP2MAP(final Stream<T> stream, final Function<T, K> classifier) {

		if (stream == null)
			return new HashMap<>();
		return (Map<K, T>) stream.collect(Collectors.groupingBy(classifier,
				Collector.of((Supplier<LinkedList<T>>) LinkedList::new, LinkedList::add, (aa, bb) -> {
					aa.addAll(bb);
					return aa;
				}, LinkedList::getFirst)));
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param coll       集合数据
	 * @param classifier 分类器
	 * @return 以K为键类型 T的列表为值得 Map结构
	 */
	public static <T, K> Map<K, Collection<T>> GROUP(final Collection<T> coll, final Function<T, K> classifier) {

		return GROUP(coll.stream(), classifier, e -> e);
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param stream     集合数据
	 * @param classifier 分类器
	 * @return 以K为键类型 T的列表为值得 Map结构
	 */
	public static <T, K> Map<K, Collection<T>> GROUP(final Stream<T> stream, final Function<T, K> classifier) {

		return GROUP(stream, classifier, e -> e);
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param <U>        键值列表的元素类型
	 * @param coll       集合数据
	 * @param classifier 分类器
	 * @param finalizer  归并函数
	 * @return 以K为键类型 U的列表为值得 Map结构
	 */
	public static <T, K, U> Map<K, U> GROUP(final Collection<T> coll, final Function<T, K> classifier,
			final Function<Collection<T>, U> finalizer) {

		return GROUP(coll.stream(), classifier, finalizer);
	}

	/**
	 * 对集合数据进行分组
	 * 
	 * @param <T>        流的元素类型
	 * @param <K>        键名类型
	 * @param <U>        键值列表的元素类型
	 * @param stream     T类型的源数据流
	 * @param classifier 分类器
	 * @return 以K为键类型 U的列表为值得 Map结构
	 */
	public static <T, K, U> Map<K, U> GROUP(final Stream<T> stream, final Function<T, K> classifier,
			final Function<Collection<T>, U> finalizer) {

		if (stream == null)
			return new HashMap<>();
		return (Map<K, U>) stream
				.collect(Collectors.groupingBy(classifier, Collector.of(LinkedList::new, Collection::add, (aa, bb) -> {
					aa.addAll(bb);
					return aa;
				}, finalizer)));
	}

	/**
	 * 检查集合中是否窜在一个元素满足指定条件
	 * 
	 * @param predicator 检查㢝元素是否满足指定的条件判断函数。
	 * @return 是否存在的满足predicator 条件的元素
	 */
	public static <T> Boolean EXISTS(final Stream<T> stream, final Predicate<T> predicator) {

		if (stream == null)
			return false;
		return stream.anyMatch(predicator);
	}

	/**
	 * 检查集合中是否窜在一个元素满足指定条件
	 * 
	 * @param predicator 检查㢝元素是否满足指定的条件判断函数。
	 * @return
	 */
	public static <T, K> Boolean EXISTS(final Collection<T> coll, final Predicate<T> predicator) {

		if (coll == null)
			return false;
		return coll.stream().anyMatch(predicator);
	}

	/**
	 * 检查集合中是否窜在一个元素满足指定条件
	 * 
	 * @param e 集合中的元素
	 * @return
	 */
	public static <T> Boolean EXISTS2(final Collection<T> coll, T e) {

		if (coll == null)
			return false;
		return coll.stream().anyMatch(g -> g.equals(e));
	}

	/**
	 * 对集合数据进行 进行 首尾连接
	 * 
	 * @param coll      集合数据
	 * @param mapper    分类器
	 * @param delimiter 分隔符
	 * @return 字符串
	 */
	public static <T> String JOIN(final Collection<T> coll, final Function<T, String> mapper, final String delimiter) {

		return JOIN(coll.stream(), mapper, delimiter);
	}

	/**
	 * 对集合数据 进行 首尾连接
	 * 
	 * @param tt        集合数据
	 * @param delimiter 分类器
	 * @return 字符串
	 */
	public static <T> String JOIN(final T[] tt, final String delimiter) {

		return JOIN(Arrays.stream(tt), e -> e + "", delimiter);
	}

	/**
	 * 对集合数据 进行 首尾连接
	 * 
	 * @param tt        集合数据
	 * @param delimiter 分类器
	 * @return 字符串
	 */
	public static <T> String JOIN(final T[] tt, final Function<T, String> mapper, final String delimiter) {

		return JOIN(Arrays.stream(tt), mapper, delimiter);
	}

	/**
	 * 对集合数据 进行 首尾连接
	 * 
	 * @param coll 集合数据<br>
	 *             delimiter 分割默认为 "/"
	 * @return 字符串
	 */
	public static <T> String JOIN(final Collection<T> coll) {

		return JOIN(coll.stream(), e -> e + "", "/");
	}

	/**
	 * 对集合数据 进行 首尾连接
	 * 
	 * @param coll      集合数据
	 * @param delimiter 分割符号
	 * @return 字符串
	 */
	public static <T> String JOIN(final Collection<T> coll, final String delimiter) {

		return JOIN(coll.stream(), e -> e + "", delimiter);
	}

	/**
	 * 对集合数据 进行 首尾连接
	 * 
	 * @param stream    集合数据
	 * @param delimiter 分隔符
	 * @return 字符串
	 */
	public static <T> String JOIN(final Stream<T> stream, final String delimiter) {

		return JOIN(stream, e -> e + "", delimiter);
	}

	/**
	 * 对集合数据进行 首尾连接
	 * 
	 * @param stream    集合数据
	 * @param mapper    转换器
	 * @param delimiter 分隔符
	 * @return 字符串
	 */
	public static <T> String JOIN(final Stream<T> stream, final Function<T, String> mapper, final String delimiter) {

		if (stream == null)
			return "";
		return stream.map(mapper).collect(Collectors.joining(delimiter));
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 <br>
	 * 求两个集合之间的差. 把 coll2在coll1中存在的元素给予删除 <br>
	 * coll1-coll2 <br>
	 * 
	 * @param <T>   数据元素的类型
	 * @param coll1 集合类1
	 * @param coll2 集合类2
	 * @return T类型的列表
	 */
	public static <T> List<T> DIFF(final Collection<T> coll1, final Collection<T> coll2) {

		return FILTER(coll1, e -> !coll2.contains(e));
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些 求两个集合之间的交集
	 * 
	 * @param <T>   数据元素的类型
	 * @param coll1 集合类2
	 * @param coll2 集合类2
	 * @return T类型的列表
	 */
	public static <T> List<T> INTERSECT(final Collection<T> coll1, final Collection<T> coll2) {

		return FILTER(coll1, coll2::contains);
	}

	/**
	 * 之所以写这个方法就是为了把代码写的精减一些
	 * 
	 * @param <T>   数据元素的类型
	 * @param coll1 集合类1
	 * @param coll2 集合类2
	 * @return T类型的列表
	 */
	public static <T> List<T> UNION(final Collection<T> coll1, final Collection<T> coll2) {

		List<T> list = LIST(coll1);
		list.addAll(coll2.stream().filter(e -> !coll1.contains(e)).collect(Collectors.toList()));
		return list;
	}

	/**
	 * 判断一个集合对象是否为空
	 *
	 * @param <T>  数据元素的类型
	 * @param coll 集合对象，coll 为null 返回为null
	 * @return 判断coll 是否为空,如果 coll 为null 返回null
	 */
	public static <T> Boolean EMPTY(final Collection<T> coll) {

		if (coll == null)
			return null;
		return coll.isEmpty();
	}

	/**
	 * 左部填充
	 * 
	 * @param coll 集合 not Set
	 * @param n    长度
	 * @return 左部填充了 指定字符的列表
	 */
	public static <T> List<T> LFILL(final Collection<T> coll, final int n) {

		return LFILL(coll, n, null);
	}

	/**
	 * 左部填充,填充到n位字长
	 * 
	 * @param coll 集合 not Set
	 * @param n    长度
	 * @param t    填充数据
	 * @return 左部填充了 指定字符的列表
	 */
	public static <T> List<T> LFILL(final Collection<T> coll, final int n, final T t) {

		if (coll == null)
			return null;
		int size = coll.size();
		List<T> list = new LinkedList<>();
		for (int i = 0; i < n - size; i++)
			list.add(t);
		list.addAll(coll);
		return list;
	}

	/**
	 * 右部填充
	 * 
	 * @param coll 集合 not Set
	 * @param n    长度
	 * @return 右部填充了 指定字符的列表
	 */
	public static <T> List<T> RFILL(final Collection<T> coll, final int n) {

		return RFILL(coll, n, null);
	}

	/**
	 * 右部填充
	 * 
	 * @param coll 集合 not Set
	 * @param n    长度
	 * @param t    填充数据
	 * @return 右部填充了 指定字符的列表
	 */
	public static <T> List<T> RFILL(final Collection<T> coll, final int n, final T t) {

		if (coll == null)
			return null;
		final int size = coll.size();
		final List<T> list = new LinkedList<>(coll);
		for (int i = 0; i < n - size; i++)
			list.add(t);

		return list;
	}

	/**
	 * 拉链函数
	 * 
	 * @param names 键名列表,用逗号,'-'分隔,为objs中的元素的进行命名，如果为 null 默认采用数组命名，编号从0开始,1,2,3,递增
	 * @param objs  数组对象,把多个数组对象按照 names 给出的字段名顺序拼装成一个字段列表
	 * @return 一个合并成功的Record列表
	 */
	public static List<IRecord> ZIP(final String names, final Object[]... objs) {

		final Collection<?>[] oo = (Collection<?>[]) Array.newInstance(Collection.class, objs.length);
		for (int i = 0; i < objs.length; i++)
			oo[i] = Arrays.asList(objs[i]);
		return ZIP(names, oo);
	}

	/**
	 * 拉链函数
	 * 
	 * @param names 键名列表,用逗号,'-'分隔,为objs中的元素的进行命名，如果为 null 默认采用数组命名，编号从0开始,1,2,3,递增
	 * @param objs  数组对象,把多个数组对象按照 names 给出的字段名顺序拼装成一个字段列表
	 * @return 一个合并成功的Record列表
	 */
	public static List<IRecord> ZIP(final String names, final Collection<?>... objs) {

		if (names == null)
			return ZIP((String[]) null, objs);
		return ZIP(names.split("[,-]+"), objs);
	}

	/**
	 * 拉链函数
	 * 
	 * @param names 键名列表,为objs中的元素的进行命名，如果为 null 默认采用数组命名，编号从0开始,1,2,3,递增
	 * @param objs  数组对象,把多个数组对象按照 names 给出的字段名顺序拼装成一个字段列表
	 * @return 一个合并成功的Record列表
	 */
	public static List<IRecord> ZIP(final String[] names, final Collection<?>... objs) {

		final Function<Collection<?>, Function<Integer, ?>> getter = (coll) -> {
			if (coll == null || coll.size() <= 0)
				return x -> null;// 空值返回一个空值函数
			Object[] oo = coll.toArray();
			int n = oo.length;
			return x -> n >= 0 ? oo[x % n] : oo[0];
		};
		final var final_names = (names == null)
				? Stream.iterate(0, e -> e + 1).limit(objs.length).map(e -> e + "").toArray(String[]::new)
				: names;
		final List<Function<Integer, ?>> gg = Stream.of(objs).map(getter).collect(Collectors.toList());
		final Function<Integer, ?> hh = getter.apply(Arrays.asList(final_names));

		List<IRecord> ll = new LinkedList<>();
		int n = Arrays.stream(objs).collect(Collectors.summarizingInt(Collection::size)).getMax();
		for (int i = 0; i < n; i++) {
			IRecord rec = new SimpleRecord();
			for (int j = 0; j < objs.length; j++)
				rec.add(hh.apply(j) + "", gg.get(j).apply(i));
			ll.add(rec);
		} // for

		return ll;
	}

	/**
	 * 对obj的字符串形式进行转义：
	 * 
	 * @param obj 待转义的数据对象
	 * @return obj为null 返回null
	 */
	public static String ESCAPE2STR(final Object obj) {

		if (obj == null)
			return null;
		return (obj + "").replaceAll("'", "").replaceAll("\"", "");
	}

	/**
	 * 把一个record 改装成一个 只有 name->value两个字段值的 record列表
	 * 
	 * @param rec 字段值
	 * @return IRecord 列表
	 */
	public static List<IRecord> REC2KVS(final IRecord rec) {

		return MAP(rec.kvs(), kvp -> SimpleRecord.REC2("key", kvp.key(), "value", kvp.value()));
	}

	/**
	 * 把一个Map结构转换成一个 KVP的 键值对儿 序列。
	 * 
	 * @param map 字段值
	 * @return IRecord 结构的 List
	 */
	public static List<IRecord> MAP2KVS(final Map<String, ?> map) {

		return MAP(map.entrySet(), entry -> SimpleRecord.REC2("key", entry.getKey(), "value", entry.getValue()));
	}

	/**
	 * 过滤掉MAP指定的值
	 * 
	 * @param <K>    Map的键名类型
	 * @param <V>    Map的键值类型
	 * @param map    源数据。
	 * @param filter 过滤器漆
	 * @return {K,V} 结构的Map
	 */
	public static <K, V> Map<K, V> FILTER(final Map<K, V> map, final BiPredicate<K, V> filter) {

		return FILTER(map, filter, e -> e);
	}

	/**
	 * 过滤掉MAP指定的值
	 * 
	 * @param <K>    Map的键名类型
	 * @param <V>    Map的键值类型
	 * @param <U>    mapper的值类型
	 * @param map    映射对象 源数据
	 * @param filter 过滤函数 (k,v)->true/false
	 * @param mapper 值变换函数 :v->u
	 * @return {K,U} 结构的Map
	 */
	public static <K, V, U> Map<K, U> FILTER(final Map<K, V> map, final BiPredicate<K, V> filter,
			Function<V, U> mapper) {

		final Map<K, U> newMap = new HashMap<>();
		map.forEach((k, v) -> {
			if (filter.test(k, v))
				newMap.put(k, mapper.apply(v));
		});
		return newMap;
	}

	/**
	 * 对于复杂对象stream.distinct 无效，这个我一直灭有找到原因，修改了 equals 和hashcode 也是无效。<br>
	 * 为对象创建一个 根据键值进行去重的方法。<br>
	 * 使用示例： tt 是一个 含有“名称”项目的记录流 <br>
	 * LIST(tt.stream().filter(distinctByKey(e->e.str("名称")))); <br>
	 * 
	 * @param <T>          Predicate的参数类型
	 * @param keyExtractor 提取对象键值
	 * @return T类型谓词判断函数
	 */
	public static <T> Predicate<T> DISTINCT_BY_KEY(final Function<? super T, Object> keyExtractor) {

		final Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	/**
	 * 计时函数
	 * 
	 * @param supplier 被计时函数
	 * @return 毫秒数
	 */
	public static double timeit(final Supplier<?> supplier) {

		final long begTime = System.nanoTime();
		supplier.get();
		final long endTime = System.nanoTime();
		final double duration = (endTime - begTime) / 1000000d;
		System.out.println("last for:" + duration + "ms");
		return duration;
	}

	/**
	 * 计时函数
	 * 
	 * @param command 待执行的命令
	 * @return command 执行的运行时间
	 */
	public static double timeit(final Runnable command) {

		final long begTime = System.nanoTime();
		command.run();
		final long endTime = System.nanoTime();
		final double duration = (endTime - begTime) / 1000000d;
		System.out.println("last for:" + duration + "ms");
		return duration;
	}

	/**
	 * 测试函数的执行时间： 例如： timeit(p-&gt;{ final List&lt;List&lt;?&gt;7&gt; directions =
	 * LIST(iterate(0,i-&gt;i&lt;10,i->i+1).map(e-&gt;asList(1,0,-1)));// 价格的变动方向
	 * final var ff = cph(directions);// 生成涨跌列表
	 * ff.forEach(e-&gt;e.compute("total",o-&gt;e.reduce(kv2int,0, (a,b)-&gt;a+b)));
	 * System.out.println(FMT2(ff)); //结果输出 },args,true);
	 * 
	 * @param <T>    cons的参数的类型
	 * @param cs     被统计函数
	 * @param args   被统计函数 参数
	 * @param isnano 是否采用纳秒进行时间统计 true 纳秒 false 毫秒时间单位
	 * @return 历时时长 纳秒
	 * @return 执行结果
	 */
	public static <T> long timeit(final Consumer<T> cs, final T args, final boolean isnano) {

		final var ai = new AtomicInteger(0);
		final var th = new Thread(() -> {
			while (ai.get() >= 0) {
				System.out.println(format(". {0} S passed", ai.getAndIncrement()));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} // while
		});
		th.start();
		final var start = isnano ? System.nanoTime() : System.currentTimeMillis();
		cs.accept(args);
		final var end = isnano ? System.nanoTime() : System.currentTimeMillis();
		final var last = end - start;
		ai.set(-1);// 停止计时器
		System.out.println(format("\nlast for:{0} {1}", last, isnano ? "ns" : "ms"));
		return last;
	};

	/**
	 * 毫秒的计时统计
	 * 
	 * @param cs   被统计函数
	 * @param args 被统计函数 参数
	 * @return 历时时长 毫秒
	 */
	public static <T> long ms_timeit(final Consumer<T> cs, final T args) {

		return timeit(cs, args, false);
	}

	/**
	 * 把 一个数字 n转换成一个字母表中的数值(术语）<br>
	 * 
	 * 在alphabetics中:ABCDEFGHIJKLMNOPQRSTUVWXYZ <br>
	 * 比如:0->A,1-B,25-Z,26-AA 等等
	 * 
	 * @param n      数字 从0开始
	 * @param alphas 字母表
	 * @return 生成exel式样的名称
	 */
	public static String nomenclature(final Integer n, final String[] alphas) {

		final int model = alphas.length;// 字母表尺寸
		final List<Integer> dd = new LinkedList<>();
		Integer num = n;

		do {
			dd.add(num % model);
			num /= model;// 进入下回一个轮回
		} while (num-- > 0); // num-- 使得每次都是从A开始，即Z的下一个是AA而不是BA

		// 就是这个简单算法我想了一夜,我就是不知道如何让10位每次都从0开始。
		Collections.reverse(dd);

		return dd.stream().map(e -> alphas[e]).collect(Collectors.joining(""));
	}

	/**
	 * 列名称： 从0开始 <br>
	 * 0->A,1->B;2->C;....,25->Z,26->AA
	 * 
	 * @param n 数字 从0开始
	 * @return 类似于EXCEL的列名称
	 */
	public static String excelname(final int n) {

		// 字母表
		String alphabetics[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
		return nomenclature(n, alphabetics);
	}

	/**
	 * 纳秒的计时统计
	 * 
	 * @param <T>  cs 的参数类型
	 * @param cs   被统计函数
	 * @param args 被统计函数 参数
	 * @return 历时时长 纳秒
	 */
	public static <T> long ns_timeit(final Consumer<T> cs, T args) {

		return timeit(cs, args, true);
	}

	/**
	 * 用法示例:// 修改Database上的注解JdbcConfig的url属性
	 * SET_FIELD_OF_ANNOTATION(ANNO(Database.class, JdbcConfig.class),
	 * "url","jdbc:neo4j:bolt://localhost/mydb?nossl");
	 * 
	 * 获得指定对象obj的对应class的注解ana，当对obj为class则直接获取其中的注解ana
	 * 注意：ANNO无法获取代理对象上的注解．因为在代理对象的类可能是cglib给予组装出来的类
	 * 
	 * @param <T>             注解的类的类型
	 * @param target          目标对象实例，指定对象obj的对应class的注解annotation，当对obj为class则直接获取其中的注解annotation
	 * @param annotationClass 目标注解类型
	 * @return ana注解的内容
	 */
	public static <T extends Annotation> T ANNO(final Object target, final Class<T> annotationClass) {

		Class<?> cls = target instanceof Class<?> ? (Class<?>) target : target.getClass();
		return cls.getAnnotation(annotationClass);
	}

	/**
	 * 用法示例:// 修改Database上的注解JdbcConfig的url属性
	 * SET_FIELD_OF_ANNOTATION(ANNO(Database.class, JdbcConfig.class),
	 * "url","jdbc:neo4j:bolt://localhost/mydb?nossl"); 修改注解中的字段内容
	 * 
	 * @param <T>        注解类型
	 * @param annotation 注解对象实例
	 * @param annofld    注解中的字段名称
	 * @param annoValue  修正的内容．
	 */
	public static <T extends Annotation> void SET_FIELD_OF_ANNOTATION(final T annotation, final String annofld,
			final Object annoValue) {

		// 注解修改工具,只有value非空才给予修改
		BiConsumer<String, Object> modifer = (fldName, value) -> {// 修改计划函数
			try {
				if (annotation == null || value == null)
					return;// 只有value非空才给予修改
				InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
				// var flds = invocationHandler.getClass().getDeclaredFields();
				// for(var fld:flds) System.out.println(fld.getName()+"/"+fld.getType());//
				// 产看注解类的成员结构

				// 获取注解中数据
				final String annotationFieldsName = "memberValues"; // 注解中配置的字段存在于memberValues
				Field fld = invocationHandler.getClass().getDeclaredField(annotationFieldsName);
				fld.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<String, Object> memberValues = (Map<String, Object>) fld.get(invocationHandler);
				if (value != null)
					memberValues.put(fldName, value);
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		};// modifer

		// 执行修改计划
		modifer.accept(annofld, annoValue);
	}

	/**
	 * 随机算子
	 * 
	 * @param <T> 数据元素
	 * @param tt  数据集合
	 * @return 从 tt随机选择一个元素
	 */
	public static <T> T rnd(final T[] tt) {

		return tt[RNDINT(tt.length)];
	}

	/**
	 * 随机算子
	 * 
	 * @param <T> 数据元素
	 * @param tt  数据集合
	 * @return 从 tt随机选择一个元素
	 */
	public static <T> T rnd(final List<T> tt) {

		return tt.get(RNDINT(tt.size()));
	}

	/**
	 * 生成一个对 T类型数字 按照 mode 大小 给予取模 的函数，模值为 T类型
	 * 
	 * @param <T>  模大小 mode 的 类型
	 * @param mode 模的大小
	 * @return 生成一个对 T类型数字 按照 mode 大小 给予取模 的函数，模值为 T类型
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> Function<T, T> mod(final T mode) {

		return n -> (T) (Object) (n.longValue() % mode.longValue());
	}

	/**
	 * 生成一个对 T类型数字 按照 mode 大小 给予取模 的函数，模值为 Integer类型
	 * 
	 * @param <T>  模大小 mode 的 类型
	 * @param mode 模的大小
	 * @return 生成一个对 T类型数字 按照 mode 大小 给予取模 的函数，模值为 Integer类型
	 */
	public static <T extends Number> Function<T, Integer> intmod(final T mode) {

		return n -> ((Number) (n.longValue() % mode.longValue())).intValue();
	}

	/**
	 * 生成一个对 T类型数字 按照 mode 大小 给予取模 的函数，模值为 Long类型
	 * 
	 * @param <T>  模大小 mode 的 类型
	 * @param mode 模的大小
	 * @return 生成一个对 T类型数字 按照 mode 大小 给予取模 的函数，模值为 Long类型
	 */
	public static <T extends Number> Function<T, Long> lngmod(final T mode) {

		return n -> ((Number) (n.longValue() % mode.longValue())).longValue();
	}

	/**
	 * Message FormaT 数据格式化
	 * 
	 * @param pattern   number,date
	 * @param arguments 参数列表
	 * @return 数据格式化
	 */
	public static String format(final String pattern, final Object... arguments) {

		return MessageFormat.format(pattern, arguments);
	}

	/**
	 * 对MFT的模板字符进行转义
	 * 
	 * @param line 源字符串
	 * @return 转义后的字符串
	 */
	public static String format_escape(final String line) {

		final var l1 = line.replace("'", "''"); // 转义单引号
		final var l2 = l1.replace("{", "'{'"); // 转义左括号
		return l2;
	}

	/**
	 * 复制 t n 次
	 * 
	 * @param <T> 元素类型
	 * @param n   重复的次数
	 * @param t   重复的元素
	 * @return T类型的列表
	 */
	public static <T> List<T> REPEAT(int n, T t) {

		return Stream.iterate(0, i -> i + 1).limit(n).map(i -> t).collect(Collectors.toList());
	}

	/**
	 * REPEAT 的别名的列表类型的返回值
	 * 
	 * @param <T> 元素类型
	 * @param n   重复的次数
	 * @param t   重复的元素
	 * @return T类型的列表
	 */
	public static <T> List<T> RPT(int n, T t) {

		return REPEAT(n, t);
	}

	/**
	 * REPEAT 的别名的列表类型的返回值
	 * 
	 * @param <T> 元素类型
	 * @param n   重复的次数
	 * @param t   重复的元素
	 * @return T类型的列表
	 */
	public static <T> List<T> RPTL(int n, T t) {

		return REPEAT(n, t);
	}

	/**
	 * REPEAT 的别名 的流类型的返回值
	 * 
	 * @param <T> 元素类型
	 * @param n   重复的次数
	 * @param t   重复的元素
	 * @return T类型的数据流
	 */
	public static <T> Stream<T> RPTS(int n, T t) {

		return REPEAT(n, t).stream();
	}

	/**
	 * REPEAT 的别名 的数组类型的返回值<br>
	 * 这里有个小技巧：就是RPTA可以一个数组类型转换成二维数组类型：比如<br>
	 * var aa = new Integer[] {1,2,3,4};<br>
	 * var aaa = RPTA(1,aa);<br>
	 * aaa 就是一个以aa为行向量的二维数组。<br>
	 * 
	 * @param <T> 待复制的元素类型
	 * @param n   重复的次数
	 * @param t   重复的元素
	 * @return T[] T类型的数组
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] RPTA(final int n, final T t) {

		final Class<T> tclass = t == null ? (Class<T>) Object.class : (Class<T>) t.getClass();
		return REPEAT(n, t).toArray(a -> (T[]) Array.newInstance(tclass, a));
	}

	/**
	 * 生成一个编号元组流, 编号从0开始
	 * 
	 * @param <T> 元组类型
	 * @param tt  源数据数组
	 * @return 编号元组流
	 */
	public static <T> Stream<Tuple2<Long, T>> TUPLES(final List<T> tt) {

		return TUPLES(tt.stream());
	}

	/**
	 * 生成一个编号元组流，编号从0开始
	 * 
	 * @param <T> 元组类型
	 * @param tt  源数据数组
	 * @return 编号元组流
	 */
	@SafeVarargs
	public static <T> Stream<Tuple2<Long, T>> TUPLES(final T... tt) {

		return TUPLES(Arrays.stream(tt));
	}

	/**
	 * 生成一个编号元组流,编号从0开始
	 * 
	 * @param <T> 元组类型
	 * @param tt  源数据流
	 * @return 编号元组流
	 */
	public static <T> Stream<Tuple2<Long, T>> TUPLES(final Stream<T> tt) {

		return TUPLES(tt, null);
	}

	/**
	 * 生成一个编号元组流
	 * 
	 * @param <T>   元组类型
	 * @param tt    源数据流
	 * @param start 开始编号,空值 表示从0开始
	 * @return 编号元组流
	 */
	public static <T> Stream<Tuple2<Long, T>> TUPLES(final Stream<T> tt, final Number start) {

		final var al = new AtomicLong(start == null ? 0L : start.longValue());
		return tt.map(t -> new Tuple2<>(al.getAndIncrement(), t));
	}

	/**
	 * 把以数组额元素进行倒转:即尾为前,前转尾
	 * 
	 * @param <T> 数组元素的类型
	 * @param tt  数组元素
	 * @return T[] 倒转后的数组。
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] reverse(final T[] tt) {

		final var ll = Arrays.asList(tt);
		var tclazz = ll.stream().filter(Objects::nonNull).map(e -> (Class<T>) e.getClass()).findAny().orElse(null);
		if (tclazz == null)
			return tt;
		Collections.reverse(ll);
		return ll.toArray(n -> (T[]) Array.newInstance(tclazz, n));
	}

	/**
	 * 包装一个计数器:计数变量从init开始,每次增加 step 这就是模拟for(long i = start;i&lt;end;i+=step) 的语句。
	 * 用于对一个stream进行遍历和截取。
	 * 
	 * @param start  开始数值
	 * @param step   步长
	 * @param tester 测试函数:(i,t)-&gt; true|false, i 索引从start开始，t 当前的数值。
	 * @return
	 */
	public static <T> Predicate<T> i_for(long start, long step, final BiPredicate<Long, T> tester) {

		final AtomicLong atom = new AtomicLong(start);
		return t -> tester.test(atom.getAndAdd(step), t);
	}

	/**
	 * 包装一个计数器:计数变量从init开始,每次增加 step
	 * 
	 * Stream.iterate(0,
	 * i-&lt;i+1).takeWhile(i_for(6)).forEach(System.out::println);
	 * Stream.iterate(0,t-&gt;t&lt;6, i-&gt;i+1).forEach(System.out::println);
	 * 
	 * @param n 序列的长度
	 * @return Predicate 的谓词判断
	 */
	public static <T> Predicate<T> i_for(final long n) {

		return i_for(0L, 1L, (i, t) -> i < n);
	}

	/**
	 * 把一个可遍历对象转换成列表 对应的 列表 结构
	 * 
	 * @param <T>      元素类型
	 * @param iterable 可遍历对象 [t], iterable 为 null 值的时候 返回 0 长度的 列表
	 * @return T类型的 列表
	 */
	public static <T> List<T> iterable2list(final Iterable<T> iterable) {

		final var list = new LinkedList<T>(); // 结果列表

		if (iterable != null) {
			for (var t : iterable) {
				list.add(t);
			} // for
		} // if

		return list;
	}

	/**
	 * 为一个类型为T的流中的每个元素 <br>
	 * 分配一个索引号，索引号从0开始 Key-&gt;Value:<br>
	 * 0-&lt;t1,1-&gt;t1,2-&gt;t2,...,n-1-&gt;tn-1,... <br>
	 * NATS(10).map(kvp()).forEach(System.out::println);<br>
	 * <br>
	 * 索引号 从0开始
	 * 
	 * @param <T> KVPair 的值类型，这其实就是为一个值 T,分配一个索引号。主要用于对流中的数据进行索引。
	 * @return 键值对儿 (long,t) 的生成函数，自动为值匹配一个长整形的编号
	 */
	public static <T> Function<T, KVPair<Long, T>> kvp() {

		return kvp(0L);
	}

	/**
	 * 为一个类型为T的流中的每个元素 <br>
	 * 分配一个索引号，索引号从start开始 Key-&lt;Value:<br>
	 * start-&lt;t0,start+1-&lt;t1,start+2-&lt;t2,...,start+n-1-&lt;tn-1,... <br>
	 * NATS(10).map(kvp(0)).forEach(System.out::println);<br>
	 * 
	 * @param <T>   KVPair 的值类型，这其实就是为一个值 T,分配一个索引号。主要用于对流中的数据进行索引。
	 * @param start 键名索引 的开始位置。
	 * @return 键值对儿 (long,t) 的生成函数，自动为值匹配一个长整形的编号
	 */
	public static <T> Function<T, KVPair<Long, T>> kvp(final Number start) {

		final var atom = new AtomicLong(start.longValue());// 状态缓存：用于生成序号
		return t -> new KVPair<>(atom.getAndIncrement(), t);
	}

	/**
	 * 为一个类型为T的流中的每个元素 分配一个索引号，索引号从0开始 Key-&lt;Value:<br>
	 * 0-&lt;t1,1-&lt;t1,2-&lt;t2,...,n-1-&lt;tn-1,... <br>
	 * NATS(10).map(kvp_int()).forEach(System.out::println);<br>
	 * <br>
	 * 索引号 从0开始
	 * 
	 * @param <T> KVPair 的值类型，这其实就是为一个值 T,分配一个索引号。主要用于对流中的数据进行索引。
	 * @return 键值对儿 (int,t) 的生成函数，自动为值匹配一个数型的编号
	 */
	public static <T> Function<T, KVPair<Integer, T>> kvp_int() {

		return kvp_int(0);
	}

	/**
	 * 为一个类型为T的流中的每个元素 分配一个索引号，索引号从start开始 Key-&lt;Value:<br>
	 * start-&lt;t0,start+1-&lt;t1,start+2-&lt;t2,...,start+n-1-&lt;tn-1,... <br>
	 * NATS(10).map(kvp_int(0)).forEach(System.out::println);<br>
	 * 
	 * @param <T>   KVPair 的值类型，这其实就是为一个值 T,分配一个索引号。主要用于对流中的数据进行索引。
	 * @param start 键名索引 的开始位置。
	 * @return 键值对儿 (int,t) 的生成函数，自动为值匹配一个整形的编号
	 */
	public static <T> Function<T, KVPair<Integer, T>> kvp_int(final Number start) {

		return kvp_int(start, e -> e);
	}

	/**
	 * 为一个类型为T的流中的每个元素 分配一个索引号，索引号从start开始 Key-&lt;Value:<br>
	 * start-&lt;t0,start+1-&lt;t1,start+2-&lt;t2,...,start+n-1-&lt;tn-1,... <br>
	 * NATS(10).map(kvp_int(0)).forEach(System.out::println);<br>
	 * 
	 * @param <T>           KVPair 的值类型，这其实就是为一个值 T,分配一个索引号。主要用于对流中的数据进行索引。
	 * @param <K>           键类型
	 * @param key_generator 键名索引的映射器，把数值类型的序号索引转变成其他的数据类型，<br>
	 *                      这个一般用于按照某种key_generator的函数规则来规律的生成K类型。比如 按照列序进行矩阵生成的功能。
	 *                      例如：
	 *                      nvec.stream(kvp_int(NVec.modeOf(10,1))).sorted((a,b)-&lt;a._1().compareTo(b._1()))<br>
	 *                      .map(e -&lt; e._2()).collect(aaclc(NVec::new));<br>
	 *                      详见 matlib的NVec <br>
	 * @return 键值对儿 (int,t) 的生成函数，为值 按照 key_generator 编制一个 K类型 编号
	 */
	public static <T, K> Function<T, KVPair<K, T>> kvp_int(final Function<Integer, K> key_generator) {

		return kvp_int(0, key_generator);
	}

	/**
	 * 为一个类型为T的流中的每个元素 分配一个索引号，索引号从start开始 Key->Value:<br>
	 * start-&lt;t0,start+1-&lt;t1,start+2-&lt;t2,...,start+n-1-&lt;tn-1,... <br>
	 * NATS(10).map(kvp_int(0)).forEach(System.out::println);<br>
	 * 
	 * @param <T>           KVPair 的值类型，这其实就是为一个值 T,分配一个索引号。主要用于对流中的数据进行索引。
	 * @param <K>           键类型
	 * @param start         键名索引 的开始位置。
	 * @param key_generator 键名索引的映射器，把数值类型的序号索引转变成其他的数据类型，<br>
	 *                      这个一般用于按照某种key_generator的函数规则来规律的生成K类型。比如
	 *                      按照列序进行矩阵生成的功能。<br>
	 *                      例如：
	 *                      nvec.stream(kvp_int(NVec.modeOf(10,1))).sorted((a,b)-&lt;a._1().compareTo(b._1()))<br>
	 *                      .map(e -&lt; e._2()).collect(aaclc(NVec::new)); <br>
	 *                      详见 matlib的NVec <br>
	 * @return 键值对儿 (int,t) 的生成函数，为值 按照 key_generator 编制一个 K类型 编号
	 */
	public static <T, K> Function<T, KVPair<K, T>> kvp_int(final Number start,
			final Function<Integer, K> key_generator) {

		final var atom = new AtomicInteger(start.intValue());// 状态缓存：用于生成序号
		return t -> new KVPair<>(key_generator.apply(atom.getAndIncrement()), t);
	}

	/**
	 * key_generator 根据key 生成 相应的主键。
	 * NATS(10).map(kvp(t-&lt;t%2)).forEach(System.out::println);
	 * 
	 * @param <K>           键值名类型
	 * @param <T>           键值类型
	 * @param key_generator 键变换函数
	 * @return 键值对儿 (k,t)
	 */
	public static <T, K> Function<T, KVPair<K, T>> kvp(final Function<T, K> key_generator) {

		return t -> new KVPair<>(key_generator.apply(t), t);
	}

	/**
	 * 列表延展：即构造 全排列的组合。比如：comprehensive([1,2],[a,b]) 会产生：如下结构的数据 <br>
	 * 0 1 表头 <br>
	 * -------- <br>
	 * 1 a 数据项目 <br>
	 * 1 b 数据项目 <br>
	 * 2 a 数据项目 <br>
	 * 2 b 数据项目 <br>
	 * 
	 * @param ccc      待延展的列表集合。位置向量集合 [cc1,cc2,...] ccc 表示集合的集合即 列表的元素依然是列表
	 * @param position 当前工作的位置索引（ccc的元素位置索引），从0开始
	 * @param rr       返回结果集合。需要递归累加, r 表示result,rr 象征着 r 的集合。 r 表示record记录的意思。
	 */
	public static void _comprehensive(final List<List<?>> ccc, final int position, final List<IRecord> rr) {

		if (ccc == null || ccc.size() < 0 || position >= ccc.size())
			return;// 保证 参数合法有效。

		if (position >= 0) {// 位置索引从0开始
			final var cc = ccc.get(position);// 提取当前位置的列表数组
			if (rr.size() == 0) {// 第一次运行时,rr是一个0长度的List
				cc.forEach(c -> rr.add(IRecord.REC(position, c)));// 初始化构建一个 rr中的每个元素r：REC(position,c),后续将在这个记录结构r上进行扩展
			} else {// 非第一次运行需要对rr中的每个元素进行扩展

				// 这是一个写起来有效率，但运行起来没有效率的方法
				// final var aa =
				// LIST(cc.stream().flatMap(c->rr.stream().map(r->REC(position,c).union(r))));//
				// 列表向前展开一层。
				// 使用aa内容展开一层后代的结果，替换上一次计算的结果：即模拟:rr = aa的结果，把啊啊赋值与rr
				// rr.clear(); rr.addAll(aa);

				// 这是一种运行起来很有效率高：但写起来没有效率的方法,考虑到运行，就采用这种写起来没有效率的算法了。haha
				final var r_litr = rr.listIterator();// listIterator 带有插入功能,listIterator add
														// 会自动移动itr，保证插入元素后next的指向保持不变。
				while (r_litr.hasNext()) { // r_litr 的迭代器
					final var r_current = r_litr.next();// 待给予展开的元素节点：r_表示元素节点为一个record记录。
					final var pos = String.valueOf(position);// 当前位置的字符串表示。
					final var c_itr = cc.iterator();// 当前位置向量（向量就是数组）的元素迭代器
					r_current.add(pos, c_itr.next());// 使用cc的第一个元素拓展r_current,拓展之后的r_current将成为一个模版,来其余的cc元素（第二个开始）的拓展基础。
					while (c_itr.hasNext()) {// 用 位置向量cc 中的每个元素(c_itr.next())给予拓展：从第二个元素开始，第一个元素位于循环外侧
						final var c = c_itr.next();// 待追加到r_current的复制品的 位置向量cc 的元素数据。
						r_litr.add(r_current.duplicate().set(pos, c));// 用r_current的复制结构duplicate来实现对rr中的元素进行拓展
					} // while c_itr
				} // while l_itr

			} // if if(rr.size()==0)

			// 继续向前进行延展
			_comprehensive(ccc, position - 1, rr);
		} // 仅当尚未结束。 position>=0
	}

	/**
	 * 数组的comprehensive
	 * 
	 * @param ccc 待延展的列表集合。ccc 表示集合的集合即 列表的元素依然是列表
	 * @return 返回结果集合。需要递归累加
	 */
	public static List<IRecord> comprehensive(final List<List<?>> ccc) {

		if (ccc == null || ccc.size() < 0)
			return new LinkedList<>();
		final List<IRecord> rr = new LinkedList<>(); // 构造返回结果集合
		_comprehensive(ccc, ccc.size() - 1, rr);
		return rr;
	}

	/**
	 * comprehensive 的别名 数组的comprehensive
	 * 
	 * @param ccc 待延展的列表集合。ccc 表示集合的集合即 列表的元素依然是列表
	 * @return 返回结果集合。需要递归累加
	 */
	public static List<IRecord> cph(final List<List<?>> ccc) {

		return comprehensive(ccc);
	}

	/**
	 * comprehensive 的别名:返回Stream结果 数组的comprehensive
	 * 
	 * @param ccc 待延展的列表集合。ccc 表示集合的集合即 列表的元素依然是列表
	 * @return 返回结果集合。需要递归累加
	 */
	public static Stream<IRecord> cph2(final List<List<?>> ccc) {

		return comprehensive(ccc).stream();
	}

	/**
	 * comprehensive 的别名 数组的comprehensive
	 * 
	 * @param ccc 待延展的列表集合。ccc 表示集合的集合即 列表的元素依然是列表
	 * @return 返回结果集合。需要递归累加
	 */
	public static List<IRecord> cph(final Stream<List<?>> ccc) {

		return comprehensive(LIST(ccc));
	}

	/**
	 * 数组的comprehensive
	 * 
	 * @param ccc 待延展的列表集合。
	 * @return 返回结果集合。需要递归累加
	 */
	public static List<IRecord> comprehensive(final List<?>... ccc) {

		if (ccc == null || ccc.length < 0)
			return new LinkedList<>();
		final List<IRecord> rr = new LinkedList<>(); // 构造返回结果集合
		_comprehensive(Arrays.asList(ccc), ccc.length - 1, rr);
		return rr;
	}

	/**
	 * comprehensive 的别名
	 * 
	 * @param ccc 待延展的列表集合。
	 * @return 返回结果集合。需要递归累加
	 */
	public static List<IRecord> cph(final List<?>... ccc) {

		return comprehensive((List<?>[]) ccc);
	}

	/**
	 * comprehensive(LittleTree::NN,"1:10:2") 示例程序 数组的comprehensive 用法示例
	 * comprehensive(s-&gt;s.split(","),"1,2,3,4","a,b,c,d").forEach(System.out::println);
	 * 
	 * @param <S>    源数据类型
	 * @param <T>    目标的数据元素类型
	 * @param parser S对象解析函数
	 * @param ss     数据源集合。
	 * @return Stream&lt;IRecord&gt; 结构的IRecord流 ，每个IRecord元素是倒序的。
	 */
	@SafeVarargs
	public static <S, T> List<IRecord> comprehensive(final Function<S, T[]> parser, final S... ss) {

		final List<List<?>> ccc = LIST(Arrays.stream(ss).map(parser).map(Arrays::asList));
		return comprehensive(ccc);
	}

	/**
	 * comprehensive 的别名 comprehensive(LittleTree::NN,"1:10:2") 示例程序
	 * 数组的comprehensive 用法示例
	 * comprehensive(s-&gt;s.split(","),"1,2,3,4","a,b,c,d").forEach(System.out::println);
	 * 
	 * @param <S>    源数据类型
	 * @param <T>    目标的数据元素类型
	 * @param parser S对象解析函数
	 * @param ss     数据源集合。
	 * @return Stream&lt;IRecord&gt; 结构的IRecord流 ，每个IRecord元素是倒序的。
	 */
	@SafeVarargs
	public static <S, T> List<IRecord> cph(final Function<S, T[]> parser, final S... ss) {

		return comprehensive(parser, (S[]) ss);
	}

	/**
	 * comprehensive 的别名 <br>
	 * comprehensive(LittleTree::NN,"1:10:2") 示例程序 <br>
	 * 数组的comprehensive <br>
	 * 用法示例 <br>
	 * comprehensive(s-&gt;s.split(","),"1,2,3,4","a,b,c,d").forEach(System.out::println);
	 * <br>
	 * 
	 * @param <S>    源数据类型
	 * @param <T>    目标的数据元素类型
	 * @param parser S对象解析函数
	 * @param ss     数据源集合。
	 * @return Stream&lt;IRecord&gt; 结构的IRecord流 ，每个IRecord元素是倒序的。
	 */
	@SafeVarargs
	public static <S, T> Stream<IRecord> cph2(final Function<S, T[]> parser, final S... ss) {

		return comprehensive(parser, (S[]) ss).stream().map(IRecord::reverse);
	}

	/**
	 * 数组的comprehensive
	 * 
	 * @param ccc 待延展的列表集合。
	 * @return 返回全排列的结果 IRecord集合的流。
	 */
	public static List<IRecord> comprehensive(final Object[]... ccc) {

		if (ccc == null || ccc.length < 0)
			return new LinkedList<>();
		final List<IRecord> rr = new LinkedList<>();
		final List<List<?>> lll = new LinkedList<>();
		for (var cc : ccc)
			lll.add(Arrays.asList(cc));
		_comprehensive(lll, ccc.length - 1, rr);
		return rr;
	}

	/**
	 * comprehensive 的别名，对ccc 中的集合元素进行全排列。
	 * 
	 * @param ccc 待延展的列表集合。
	 * @return 返回全排列的结果 IRecord集合的流。
	 */
	public static List<IRecord> cph(final Object[]... ccc) {

		return comprehensive((Object[][]) ccc);
	}

	/**
	 * comprehensive 的别名,对ccc 中的集合元素进行全排列。<br>
	 * 对排列结果，采用reverse以维持原有谁能够。<br>
	 * 
	 * @param <T> 参数的对象类型 使用泛型以保证对 任意类型的接收
	 * @param ccc 待延展的列表集合。需要注意ccc 需要是一个T[][]。写成 T[] ... ccc 是为了保证不定参数。
	 * @return 返回全排列的结果 IRecord集合的流。
	 */
	@SafeVarargs
	public static <T> Stream<IRecord> cph2(final T[]... ccc) {

		return comprehensive((Object[][]) ccc).stream().map(IRecord::reverse);
	}

	/**
	 * comprehensive 的别名,对ccc 中的集合元素进行全排列。<br>
	 * 对排列结果，采用reverse以维持原有谁能够。<br>
	 * 
	 * @param <T> 参数的对象类型 使用泛型以保证对 任意类型的接收
	 * @param ccc 待延展的列表集合。需要注意ccc 需要是一个T[][]。写成 T[] ... ccc 是为了保证不定参数。
	 * @return 返回全排列的结果 IRecord集合的流。
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T> Stream<IRecord> cph2(final List<T>... ccc) {

		return comprehensive((List<List<?>>) (Object) Arrays.asList(ccc)).stream();// 注意一定需要用List<List<?>>来完成类型转换，否则会接入到错误类型。
	}

	/**
	 * 构造出一个可以捕获抛出异常的一元函数 <br>
	 * 
	 * 用法示例：<br>
	 * Stream.of("trycach用法示例").forEach(line-&gt;{ // 书写一行数据 <br>
	 * final var constructor =
	 * compose_ef(BufferedWriter::new,(ExceptionalFunction&lt;String,FileWriter&gt;)FileWriter::new);//
	 * 构造函数 <br>
	 * final var bw =
	 * Optional.of("d:/sliced/tmp/a.txt").map(trycatch(constructor)).get();// 创建书写器
	 * <br>
	 * Stream.of(bw).forEach(trycatch2(writer-&gt;writer.append(line)));// 书写内容 <br>
	 * Stream.of(bw).forEach(trycatch3(Writer::close)); // 关闭书写器 <br>
	 * }); <br>
	 * 
	 * @param <T>               源数据类型
	 * @param <U>               解雇数据类型
	 * @param exceptionFunction 一个可以抛出异常的函数 t-&gt;u
	 * @return 剥夺掉异常的函数 ： 一元函数 t-&gt;u
	 */
	public static <T, U> Function<T, U> trycatch(final ExceptionalFunction<T, U> exceptionFunction) {

		return t -> {
			try {
				return exceptionFunction.apply(t);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			} // try// try

			return null;
		};// 返回一个Function
	}

	/**
	 * 构造出一个可以捕获抛出异常的一元消费函数 <br>
	 * 用法示例：<br>
	 * Stream.of("trycach用法示例").forEach(line-&gt;{ // 书写一行数据 <br>
	 * final var constructor =
	 * compose_ef(BufferedWriter::new,(ExceptionalFunction&gt;String,FileWriter&gt;)FileWriter::new);//
	 * 构造函数 <br>
	 * final var bw =
	 * Optional.of("d:/sliced/tmp/a.txt").map(trycatch(constructor)).get();// 创建书写器
	 * <br>
	 * Stream.of(bw).forEach(trycatch2(writer-&gt;writer.append(line)));// 书写内容 <br>
	 * Stream.of(bw).forEach(trycatch3(Writer::close)); // 关闭书写器 <br>
	 * }); <br>
	 * 
	 * @param <T>               源数据类型
	 * @param exceptionFunction 一个可以抛出异常的函数 t-&gt;?
	 * @return 剥夺掉异常的函数 ： 消费函数 t-&gt;{}
	 */
	public static <T> Consumer<T> trycatch2(final ExceptionalFunction<T, ?> exceptionFunction) {

		return fun2cs(trycatch(exceptionFunction));
	}

	/**
	 * 构造出一个可以捕获抛出异常的一元消费函数 <br>
	 * 用法示例：<br>
	 * Stream.of("trycach用法示例").forEach(line-&gt;{ // 书写一行数据 <br>
	 * final var constructor =
	 * compose_ef(BufferedWriter::new,(ExceptionalFunction&lt;String,FileWriter&gt;)FileWriter::new);//
	 * 构造函数 <br>
	 * final var bw =
	 * Optional.of("d:/sliced/tmp/a.txt").map(trycatch(constructor)).get();// 创建书写器
	 * <br>
	 * Stream.of(bw).forEach(trycatch2(writer-&gt;writer.append(line)));// 书写内容 <br>
	 * Stream.of(bw).forEach(trycatch3(Writer::close)); // 关闭书写器 <br>
	 * }); <br>
	 * 
	 * @param <T>         源数据类型
	 * @param exceptioncs 一个可以抛出异常的 消费函数 t-&gt;{}
	 * @return 剥夺掉异常的函数 ： 消费函数 t-&gt;{}
	 */
	public static <T> Consumer<T> trycatch3(final ExceptionalConsumer<T> exceptioncs) {

		return t -> {
			try {
				exceptioncs.accept(t);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		};
	}

	/**
	 * 构造出一个可以捕获抛出异常的一元函数 <br>
	 * 
	 * 用法示例：<br>
	 * Stream.of("trycach用法示例").forEach(line-&gt;{ // 书写一行数据 <br>
	 * final var constructor =
	 * compose_ef(BufferedWriter::new,(ExceptionalFunction&lt;String,FileWriter&gt;)FileWriter::new);//
	 * 构造函数 <br>
	 * final var bw =
	 * Optional.of("d:/sliced/tmp/a.txt").map(trycatch(constructor)).get();// 创建书写器
	 * <br>
	 * Stream.of(bw).forEach(trycatch2(writer-&gt;writer.append(line)));// 书写内容 <br>
	 * Stream.of(bw).forEach(trycatch3(Writer::close)); // 关闭书写器 <br>
	 * }); <br>
	 * 
	 * @param <T>                源数据类型
	 * @param <U>                解雇数据类型
	 * @param exceptionPredicate 一个可以抛出异常的函数 t-&gt;bool
	 * @return 剥夺掉异常的函数: 谓词判断函数 t-&gt;bool
	 */
	public static <T, U> Predicate<T> trycatch4(final ExceptionalPredicate<T> exceptionPredicate) {

		return t -> {
			try {
				return exceptionPredicate.test(t);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			} // try// try

			return false;
		};// 返回一个Function
	}

	/**
	 * 把一个函数转换成消费函数
	 * 
	 * @param <T>  源数据类型
	 * @param func 一个T为参数的一元函数 t-&gt;?
	 * @return 消费函数 cosnumer&lt;T&gt;
	 */
	public static <T> Consumer<T> fun2cs(Function<T, ?> func) {

		return func::apply;
	}

	/**
	 * 组合两个函数 <br>
	 * 
	 * @param <T> 第一个参数类型
	 * @param <U> 第二个参数类型
	 * @param <V> 第三个参数类型
	 * @param ftu t-&gt;u
	 * @param fuv u-&gt;v
	 * @return t-&gt;v
	 */
	public static <T, U, V> Function<T, V> compose_f(final Function<U, V> fuv, final Function<T, U> ftu) {

		return fuv.compose(ftu);
	}

	/**
	 * 组合三个函数 <br>
	 * 
	 * @param <T> 第一个参数类型
	 * @param <U> 第二个参数类型
	 * @param <V> 第三个参数类型
	 * @param <X> 第四个参数类型
	 * @param ftu t-&gt;u
	 * @param fuv u-&gt;v
	 * @param fvx v-&gt;x
	 * @return t-&gt;x
	 */
	public static <T, U, V, X> Function<T, X> compose_f(final Function<V, X> fvx, final Function<U, V> fuv,
			final Function<T, U> ftu) {

		return fvx.compose(fuv.compose(ftu));
	}

	/**
	 * 组合四个函数 <br>
	 * 
	 * @param <T> 第一个参数类型
	 * @param <U> 第二个参数类型
	 * @param <V> 第三个参数类型
	 * @param <X> 第四个参数类型
	 * @param <Y> 第五个参数类型
	 * @param ftu t-&gt;u
	 * @param fuv u-&gt;v
	 * @param fvx v-&gt;x
	 * @param fxy x-&gt;y
	 * @return t-&gt;y
	 */
	public static <T, U, V, X, Y> Function<T, Y> compose_f(final Function<X, Y> fxy, final Function<V, X> fvx,
			final Function<U, V> fuv, final Function<T, U> ftu) {

		return fxy.compose(fvx.compose(fuv.compose(ftu)));
	}

	/**
	 * 组合五个个函数 <br>
	 * 
	 * @param <T> 第一个参数类型
	 * @param <U> 第二个参数类型
	 * @param <V> 第三个参数类型
	 * @param <X> 第四个参数类型
	 * @param <Y> 第五个参数类型
	 * @param <Y> 第六个参数类型
	 * @param <Z> 第七个参数类型
	 * @param ftu t-&gt;u
	 * @param fuv u-&gt;v
	 * @param fvx v-&gt;x
	 * @param fxy x-&gt;y
	 * @param fyz y-&gt;z
	 * @return t-&gt;z
	 */
	public static <T, U, V, X, Y, Z> Function<T, Z> compose_f(final Function<Y, Z> fyz, final Function<X, Y> fxy,
			final Function<V, X> fvx, final Function<U, V> fuv, final Function<T, U> ftu) {

		return fyz.compose(fxy.compose(fvx.compose(fuv.compose(ftu))));
	}

	/**
	 * 组合成一个消费函数
	 * 
	 * @param <T> 第一个参数类型
	 * @param <U> 第二个参数类型
	 * @param ftu t-&gt;u
	 * @param cs  u-&gt;{}
	 * @return t-&gt;{}
	 */
	public static <T, U> Consumer<T> compose_cs(final Consumer<U> cs, final Function<T, U> ftu) {

		return t -> cs.accept(ftu.apply(t));
	}

	/**
	 * 组合两个函数 <br>
	 * 
	 * 用法示例：<br>
	 * Stream.of("trycach用法示例").forEach(line-&gt;{ // 书写一行数据 <br>
	 * final var constructor =
	 * compose_ef(BufferedWriter::new,(ExceptionalFunction&lt;String,FileWriter&gt;)FileWriter::new);//
	 * 构造函数 <br>
	 * final var bw =
	 * Optional.of("d:/sliced/tmp/a.txt").map(trycatch(constructor)).get();// 创建书写器
	 * <br>
	 * Stream.of(bw).forEach(trycatch2(writer-&gt;writer.append(line)));// 书写内容 <br>
	 * Stream.of(bw).forEach(trycatch3(Writer::close)); // 关闭书写器 <br>
	 * }); <br>
	 * 
	 * @param <T> 第一个参数类型
	 * @param <U> 第二个参数类型
	 * @param <V> 第三个参数类型
	 * @param ftu t-&gt;u
	 * @param fuv u-&gt;v
	 * @return t-&gt;v
	 */
	public static <T, U, V> ExceptionalFunction<T, V> compose_ef(final ExceptionalFunction<U, V> fuv,
			final ExceptionalFunction<T, U> ftu) {

		return t -> fuv.apply(ftu.apply(t));
	}

	/**
	 * 对 一个 line 进行分词
	 * 
	 * @param line     数据字符串
	 * @param keywords 关键词列表
	 * @return 分词结果 {name:分词符号,flag:分词标记是 keyword为true否自false,offset:偏移量}
	 */
	public static List<IRecord> tokenize(final String line, final List<String> keywords) {

		final Function<String, List<Character>> lc = strline -> strline.chars().mapToObj(c -> (char) c)
				.collect(Collectors.toList());// 把一个字符串转换成一个字符列表
		final var keywordsTrie = new TrieNode<>('/');// 前缀树
		keywords.stream().map(lc).forEach(keywordsTrie::addParts);// 构建前赘树
		return tokenize(line, keywordsTrie);
	} // tokenize

	/**
	 * 对 一个 源数据字符串line 进行分词,分词列表 是 IRecord 列表[{name:分词符号,flag:分词标记是
	 * keyword为true否自false,offset:偏移量}]
	 * 
	 * @param line         源数据数据字符串
	 * @param keywordsTrie 以 '/' 为根节点的 keywords前缀树
	 * @return 分词结果 {name:分词符号,flag:分词标记是 keyword为true否自false,offset:偏移量}
	 */
	public static List<IRecord> tokenize(final String line, final TrieNode<Character> keywordsTrie) {

		final Function<String, List<Character>> lc = strline -> strline.chars().mapToObj(c -> (char) c)
				.collect(Collectors.toList());// 把一个字符串转换成一个字符列表
		final var len = line.length();// 模版字符串的长度。
		final var stack = new Stack<String>();// 候选集合：即当前业已发现匹配的 候选keyword集合。 最长的候选keyword位于栈顶最短的位于栈底。
		int i = 0;// 当前阶段/周期 读取的模版字符位置。即开始位置。
		final var tokens = new LinkedList<IRecord>(); // 分词结果的集合。

		while (i < len) {// 逐字读取
			stack.clear();// 清空候选集合
			int j = i;// 读取串的结束位置(不包含)。阶段/周期 开始位置 开始。读取串由[i,j)来进行定义。
			var prefixNode = keywordsTrie.pathOf(Arrays.asList('/', line.charAt(i)));// 前缀节点:i周期初始前缀节点。

			// 在i周期初始前缀节点的基础之上,使用 while循环逐增加字符使之成为keyword(最长)以提取keyword.
			while (prefixNode != null) {// 尽力向前读取字符直到不是前缀为止:尝试在从当前位置开始向后读取占位符，尽可能长的读取字符。直到不能够拼成占位付的前缀为止。
				if (prefixNode.flag())
					stack.push(prefixNode.token());
				if (j == len)
					break;
				prefixNode = keywordsTrie.pathOf(lc.apply("/" + line.substring(i, ++j)));// 移动读取位置的右侧索引。
			} // while prefixNode!=null

			if (!stack.empty()) { // 已经发现了keyword
				final var keyword = stack.pop();// 提取最长的即栈顶元素作为keyword。
				tokens.add(IRecord.REC("name", keyword, "flag", true, "offset", i)); // 为防止
																						// 两个连续keyword,不能把keyword后面的读取的字符视为普通word即flag为false的字符串。
				i += keyword.length();// 步长移动keyword长度。
			} else { // word template的子串[i,j)表示模版内容则给予拷贝buffer缓存,即比keywords的前缀多一个非前缀字符的字符串。
				final var word = line.substring(i, j == i ? i + 1 : j);// 读取的单词,因为在 while( prefixNode!=null ) 跳出循环前做过
																		// ++j,座椅这里只用使用j
				tokens.add(IRecord.REC("name", word, "flag", false, "offset", i));// 提取业已读取的(j位置的字符已经读取）字符串长度。
				i += word.length();// 步长移动到word长度
			} // !stack.empty()

		} // while i<len

		return tokens; // 返回结果
	}// tokenize

	/**
	 * 数据分组
	 * 
	 * @param <T>   元素类型
	 * @param datas 数据列表
	 * @param size  分组长度
	 * @return 数据分组
	 */
	public static <T> Map<Integer, List<T>> partitions(final List<T> datas, final int size) {

		return partitions(datas.stream(), size);
	}

	/**
	 * 数据分组
	 * 
	 * @param <T>   元素类型
	 * @param dataS 数据列表
	 * @param size  分组长度
	 * @return 数据分组
	 */
	public static <T> Map<Integer, List<T>> partitions(final Stream<T> dataS, final int size) {

		final var ar = new AtomicInteger();
		final var parts = dataS.collect(groupingBy(e -> ar.getAndIncrement() / size));
		return parts;
	}

	/**
	 * 抽取指定尺寸大小的抽象
	 * 
	 * @param <T>   元素类型
	 * @param dataS 数据源
	 * @param size  抽样大小
	 * @return 抽取指定尺寸大小的抽象
	 */
	public static <T> List<T> sample(final Stream<T> dataS, final Integer size) {

		return dataS.map(e -> Tuple2.of(Math.random(), e)).sorted((a, b) -> a._1().compareTo(b._1())).map(e -> e._2())
				.limit(size).toList(); // 随机生成
	}

	/**
	 * 抽取指定尺寸大小的抽象
	 * 
	 * @param dfm  数据源
	 * @param size 抽样大小
	 * @return 抽取指定尺寸大小的抽象
	 */
	public static List<IRecord> sample(final DFrame dfm, final Integer size) {

		return sample(dfm.rowS(), size);
	}

	/**
	 * 批量处理
	 * 
	 * @param <K>        分组名
	 * @param <V>        分组数据
	 * @param partitions 分组数据源
	 * @param handler    分组处理器 partition-&gt;{}
	 * @throws Exception
	 */
	public static <K, V> void batch_handlers(final Map<K, V> partitions, final ExceptionalConsumer<V> handler)
			throws Exception {

		for (final var partition : partitions.entrySet()) { // 重新设置公司产品
			handler.accept(partition.getValue());
		}
	}

	/**
	 * 导入数据表格
	 * 
	 * @param shtmx    表单读取器
	 * @param tblnames 表名列表
	 * @return 数据表
	 */
	public static ExceptionalConsumer<IJdbcSession<UUID, Object>> imports(final Function<String, DFrame> shtmx,
			final String... tblnames) {

		return imports2(shtmx, false, tblnames);
	}

	/**
	 * 导入数据表格
	 * 
	 * @param shtmx            表单读取器
	 * @param remove_insert_id insert语句是否把id字段去除以使用自增长字段
	 * @param tblnames         表名列表
	 * @return 数据表
	 */
	public static ExceptionalConsumer<IJdbcSession<UUID, Object>> imports2(final Function<String, DFrame> shtmx,
			final boolean remove_insert_id, final String... tblnames) {

		return (sess) -> {
			for (final String tblname : tblnames) { // 遍历数据表
				final var data = shtmx.apply(tblname);
				final var line = proto_of(data.rows()).aoks2rec(String::toLowerCase);
				if (line.opt("id").isPresent()) {
					final var id = line.i4("id");
					if (id != null) {
						line.add(line.remove("id")).add("id", id);
					} // if
				} // if
				final var ctsql = ctsql(tblname, line);
				sess.sql2execute(String.format("drop table if exists %s", tblname)); // 删除数据表
				sess.sql2execute(ctsql); // 创建数据表

				final boolean flag;
				if (remove_insert_id) { // 移除首项id列
					flag = data.headOpt().map(e -> e.keyOfIndex(0)).map("id"::equalsIgnoreCase).orElse(false);
				} else {
					flag = true;
				} // if

				for (final var row : data.rows()) {
					final var r = flag //
							? row.remove(0) // 去除首项id,因为这是自增长数据表
							: row;
					sess.sql2execute(insql(tblname, r));
				}
			} // for
		};
	}

	/**
	 * 解析json成IRecord
	 * 
	 * @param bb 字符串字节
	 * @return Map&lt;String,Object&gt;
	 */
	public static Map<String, Object> bytes2map(final byte[] bb) {

		final var rec = h2_opt_processor(bb).map(IRecord::REC);

		return rec.map(IRecord::toMap).orElse(null);
	}

	/**
	 * 解析json成IRecord
	 * 
	 * @param bb 字符串字节
	 * @return Object 对象
	 */
	public static Object bytes2obj(final byte[] bb) {

		final var opt = h2_opt_processor(bb).map(e -> Json.json2obj(e, Object.class));

		return opt.orElse(null);
	}

	/**
	 * h2数据库的json字段的数据处理器
	 * 
	 * @param key 鍵名
	 * @return 处理函数
	 */
	public static Consumer<? super IRecord> h2_json_processor(final String key) {

		return e -> e.compute(key, Jdbcs::bytes2obj); // 属性处理
	}

	/**
	 * 解析 字节码 成为 Optional
	 * 
	 * @param bb 字节码
	 * @return Optional
	 */
	public static Optional<String> h2_opt_processor(final byte[] bb) {

		return Optional.ofNullable(bb == null ? null : new String(bb))
				.map(s -> s = s.startsWith("\"") && s.endsWith("\"")
						? s.substring(1, s.length() - 1).replace("\\\"", "\"")
						: s);
	}

	/**
	 * 解析 字节码 成为 String
	 * 
	 * @param bb 字节码
	 * @return String
	 */
	public static String h2_str_processor(final byte[] bb) {

		return h2_opt_processor(bb).orElse(null);
	}

	public static final boolean debug = false;// 调试信息开启标记

}
