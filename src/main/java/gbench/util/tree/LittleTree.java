package gbench.util.tree;

import java.util.List;
import java.util.Map;

import static gbench.util.data.DataApp.IRecord.REC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.lisp.Tuple2;
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
	 * @param root         根节点数据 生成函数 [a]-&gt;a
	 * @param get_children ([a],a)-&gt;[a]
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
	 * Map&lt;K,V&gt; 的 结构 (id:节点id,name:节点名称,pid:父节点id)
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
	 * @return [a] -&gt; rootnode
	 */
	public static Collector<gbench.util.data.DataApp.IRecord, ?, Node<gbench.util.data.DataApp.IRecord>> dtreeclc(
			final Object rootid) {
		return LittleTree.treeclc(Node.get_one(p -> p.get("id").equals(rootid)),
				Node.get_children((p, e) -> p.get("id").equals(e.get("pid"))));
	}

}
