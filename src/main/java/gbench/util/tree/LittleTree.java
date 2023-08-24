package gbench.util.tree;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.lisp.IRecord;

/**
 * 树形结构
 */
public class LittleTree {
	
	/**
	 * 默认构造函数
	 */
	private LittleTree() {
		
	}

	/**
	 * 重复 ws(words) n次
	 *
	 * @param ws words 单词序列
	 * @param n  重复次数
	 * @return 重复 ws(words) n次
	 */
	public static String repeat(final String ws, final int n) {
		return Stream.iterate(0, i -> i + 1).limit(n).map(e -> ws).collect(Collectors.joining(""));
	}

	/**
	 * 构建一棵树
	 *
	 * @param <T>          节点元素的值类型
	 * @param root         根节点
	 * @param get_children 子节点函数 <br>
	 * @return 树形结构的根节点： Node &lt;T&gt; 类型的根节点
	 */
	public static <T> Node<T> buildTree(final T root, final Function<T, List<T>> get_children) {
		final Function<Node<T>, List<Node<T>>> _get_children = p -> get_children.apply(p.getValue()).stream()
				.map(Node::new).collect(Collectors.toList());
		return LittleTree.buildTree((Supplier<Node<T>>) () -> Node.of(root), _get_children);

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
			final Node<T> node = stack.pop();
			final List<Node<T>> cc = get_children.apply(node);
			if (cc.size() > 0) {
				stack.addAll(cc);
				node.addChildren(cc);
				// System.out.println(node+"：子节点为："+cc);
			} else {
				// System.out.println(node+"：没有子节点");
			} // if
		} // while

		return root;
	}

	/**
	 * 树形结构归结器
	 * 
	 * @param <T>          元素类型
	 * @param root         根节点数据 生成函数 [a]->a
	 * @param get_children ([a],a)->[a]
	 * @return [a] -> rootnode
	 */
	public static <T> Collector<T, ?, Node<T>> treeclc(final Function<List<T>, T> root,
			final BiFunction<List<T>, T, List<T>> get_children) {
		return Collector.of(() -> new ArrayList<T>(), (aa, a) -> aa.add(a), (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, (a) -> {
			return LittleTree.buildTree(root.apply(a), e -> get_children.apply(a, e));
		});
	}

	/**
	 * 树形结构归结器(Map类型) <br>
	 * Map<K,V> 的 结构 (id:节点id,name:节点名称,pid:父节点id)
	 * 
	 * @param <K>    健名类型
	 * @param <V>    键值类型
	 * @param rootid 根节点ID
	 * @return [a] -&gt; rootnode
	 */
	public static <K, V> Collector<Map<K, V>, ?, Node<Map<K, V>>> mtreeclc(final Object rootid) {
		return LittleTree.treeclc(Node.get_one(p -> p.get("id").equals(rootid)),
				Node.get_children((p, e) -> p.get("id").equals(e.get("pid"))));
	}

	/**
	 * 树形结构归结器(lisp.IRecord) <br>
	 * IRecord 的 结构 (id:节点id,name:节点名称,pid:父节点id)
	 * 
	 * @param rootid 根节点ID
	 * @return [a] -> rootnode
	 */
	public static Collector<IRecord, ?, Node<IRecord>> rtreeclc(final Object rootid) {
		return LittleTree.treeclc(Node.get_one(p -> p.get("id").equals(rootid)),
				Node.get_children((p, e) -> p.get("id").equals(e.get("pid"))));
	}

	/**
	 * 树形结构归结器(DataApp.IRecord) <br>
	 * IRecord 的 结构 (id:节点id,name:节点名称,pid:父节点id)
	 * 
	 * @param rootid 根节点ID
	 * @return [a] -> rootnode
	 */
	public static Collector<gbench.util.data.DataApp.IRecord, ?, Node<gbench.util.data.DataApp.IRecord>> dtreeclc(
			final Object rootid) {
		return LittleTree.treeclc(Node.get_one(p -> p.get("id").equals(rootid)),
				Node.get_children((p, e) -> p.get("id").equals(e.get("pid"))));
	}

}
