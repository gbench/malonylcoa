package gbench.util.tree;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.*;

import gbench.util.lisp.IRecord;
import gbench.util.lisp.Tuple2;

/**
 * 特殊的 键值对儿(key 特化为列表) <br>
 * 
 * 前缀树节点:([k],v) <br>
 * 
 * 一棵TrieTree 的 本质就是 一个 多维键名单值列表 [([int],ndarray)] <br>
 * TrieTree 的 节点单元为 trienode, trienode 的路径path代表 多维键名，trienode 的属性 attrs 代表单值。
 * <br>
 * 这是一个键名为列表,键值为V (node节点属性 attrs)的多维键名的 Key Value Pairs
 * 
 * @author gbench
 *
 * @param <T> 节点名称的类型。
 */
public class TrieNode<T> implements INodeWriter<TrieNode<T>> {

	/**
	 * 前缀树的节点
	 * 
	 */
	public TrieNode() {
		this(null, null, null);
	}

	/**
	 * 前缀树的节点
	 * 
	 * @param name 节点字符
	 */
	public TrieNode(final T name) {
		this(name, null, null);
	}

	/**
	 * 前缀树的节点
	 * 
	 * @param name          节点字符
	 * @param children_ctor 子节点构建器 ()->map
	 */
	public TrieNode(final T name, final Supplier<Map<T, TrieNode<T>>> children_ctor) {
		this(name, null, children_ctor);
	}

	/**
	 * 节点
	 * 
	 * @param name   节点的值
	 * @param parent 父节点
	 */
	public TrieNode(final T name, final TrieNode<T> parent) {
		this(name, parent, null);
	}

	/**
	 * 节点
	 * 
	 * @param name          节点的值
	 * @param parent        父节点
	 * @param children_ctor 子节点构建器 ()->map
	 */
	public TrieNode(final T name, final TrieNode<T> parent, final Supplier<Map<T, TrieNode<T>>> children_ctor) {
		this.value = name;
		this.parent = parent;
		this.children_ctor = children_ctor;
		this.children = children_ctor == null ? new LinkedHashMap<>() : children_ctor.get();// 子节点
	}

	/**
	 * 节点流 childrenS 的 别名
	 * 
	 * @return 节点流 [trienode]
	 */
	public Stream<TrieNode<T>> childrenNodes() {
		return this.childrenS();
	}

	/**
	 * 当前节点是否拥有一个 叫做name的子节点
	 * 
	 * @param name 节点名称。
	 * @return 是否有子节点。
	 */
	public boolean hasChild(final T name) {
		return this.children.containsKey(name);
	}

	/**
	 * 为当前节点增加一个 叫做name的子节点。如果已经添加 则 直接返回 该name的子节点。
	 * 
	 * @param name 节点名称
	 * @return 名称为 name的TrieNode
	 */
	public TrieNode<T> addPart(final T name) {
		synchronized (children) {
			return children.compute(name, (k, v) -> v == null ? new TrieNode<>(k, this, this.children_ctor) : v);
		}
	}

	/**
	 * 添加单词
	 * 
	 * @param parts 单词字符序列
	 * @return 添加单词
	 */
	public TrieNode<T> addParts(final Iterable<T> parts) {
		return this.addParts(StreamSupport.stream(parts.spliterator(), false));
	}

	/**
	 * 添加单词
	 * 
	 * @param parts 单词字符序列
	 * @return 添加单词
	 */
	public TrieNode<T> addParts(final Stream<T> parts) {
		return this.addParts(parts.collect(Collectors.toList()));
	}

	/**
	 * 添加单词
	 * 
	 * @param parts 单词字符序列
	 * @return 添加单词
	 */
	public TrieNode<T> addParts(final T[] parts) {
		return this.addParts(Arrays.asList(parts));
	}

	/**
	 * 添加单词
	 * 
	 * @param parts 单词字符序列
	 * @return 添加单词
	 */
	public TrieNode<T> addParts(final List<T> parts) {
		return this.addParts(parts, true);
	}

	/**
	 * 添加单词
	 * 
	 * @param parts 单词字符序列
	 * @param b     算法模式,true 非递归模式, false 递归模式
	 * @return 添加单词
	 */
	public TrieNode<T> addParts(final List<T> parts, final boolean b) {
		if (b) {// 采用非递归方式实现。好处是 可以效率高。
			TrieNode<T> node = this;
			for (final T part : parts) {
				node = node.addPart(part);
			}
			node.attributes.computeIfAbsent("flag", k -> true);// 设置单词标记
			return node;
		} else { // 采用递归方法来实现,好处是 没有引入本地变量，方便并发
			if (parts.size() < 1) {// 标记档次
				this.attributes.computeIfAbsent("flag", k -> true);// 设置单词标记
				return this;
			} else {
				return this.addPart(parts.get(0)) // 添加子节点
						.addParts(parts.subList(1, parts.size())); // 子节点再添加后续节点
			} // if
		} // if
	}

	/**
	 * 添加单词
	 * 
	 * @param word 单词字符序列
	 * @return 添加单词
	 */
	public TrieNode<T> add(final Stream<T> word) {
		return this.addParts(word);
	}

	/**
	 * 添加单词
	 * 
	 * @param tt 字符序列
	 * @return 添加单词
	 */
	public TrieNode<T> add(final Iterable<T> tt) {
		return this.addParts(tt);
	}

	/**
	 * 添加单词 <br>
	 * addWord 的别名
	 * 
	 * @param tt 字符序列
	 * @return 添加单词
	 */
	@SafeVarargs
	public final TrieNode<T> add(final T... tt) {
		return this.addParts(Arrays.asList(tt));
	}

	/**
	 * 去除根节点之后的字符拼接成一条字符路径（字符序列）
	 * 
	 * @return 字符序列路径。
	 */
	public String token() {
		return this.token("");
	}

	/**
	 * 去除根节点之后的字符拼接成一条字符路径（字符序列）
	 * 
	 * @param delim 分隔符
	 * @return 字符序列路径。
	 */
	public String token(final String delim) {
		return this.parts().stream() // 提取 路径序列
				.skip(1) // 去除根节点 字符
				.map(e -> e + "") // 字符格式化
				.collect(Collectors.joining(delim));
	}

	/**
	 * 获取路径
	 * 
	 * @return 添加路径
	 */
	public Stream<T> partS() {
		final Stream<T> s = Stream.of(this.value);
		if (this.parent == null)
			return s;
		else {
			return Stream.concat(this.parent.partS(), s);
		}
	}

	/**
	 * 归并链路节点 相当于 this.partS().collect(collector);
	 * 
	 * @param <U>       归并结果
	 * @param collector 归并器 [t]->u
	 * @return U 类型的 结果
	 */
	public <U> U collect(Collector<? super T, ?, U> collector) {
		return this.partS().collect(collector);
	}

	/**
	 * 获取路径
	 * 
	 * @return 添加路径
	 */
	public List<T> parts() {
		return this.partS().collect(Collectors.toList());
	}

	/**
	 * 节点全路径 <br>
	 * 路径分隔符 为 /
	 * 
	 * @return 节点全路径
	 */
	public String path() {
		return this.partS().map(e -> ("/".equals(e) ? "" : "" + e).trim()).collect(Collectors.joining("/"));
	}

	/**
	 * 提取路径中的 trie节点
	 * 
	 * 比如 对于一个 root 结构 (root 指向“/”) : "/a/b/c" <br>
	 * root.getNode("/a/b") 获得 b 节点 <br>
	 * root.getNode("a/b") 则返回 null, 因为路径 tt 没有包含 根节点名称 <br>
	 * 
	 * @param path 路径信息 包含有当前节点（根节点名称) 字符的序列 比如：<br>
	 *             当前节点 this 是 根节点 "/": this.pathOf("/a/b/c") 返回 c 节点 <br>
	 *             当前节点 this 是 根节点 "a" :this.pathOf("a/b/c") 返回 c 节点
	 * @return trie节点
	 */
	@SafeVarargs
	final public TrieNode<T> pathOf(final T... path) {
		return this.pathOf(Arrays.asList(path));
	}

	/**
	 * 提取路径中的 trie节点
	 * 
	 * 比如 对于一个 root 结构 (root 指向“/”) : "/a/b/c" <br>
	 * root.getNode("/a/b") 获得 b 节点 <br>
	 * root.getNode("a/b") 则返回 null, 因为路径 tt 没有包含 根节点名称 <br>
	 * 
	 * @param path 路径信息 包含有当前节点(根节点名称) 字符的序列 比如：<br>
	 *             当前节点 this 是 根节点 "/": this.pathOf("/a/b/c") 返回 c 节点 <br>
	 *             当前节点 this 是 根节点 "a" :this.pathOf("a/b/c") 返回 c 节点
	 * @return trie节点
	 */
	@SuppressWarnings("unchecked")
	public TrieNode<T> pathOf(final List<T> path) {
		final List<T> path_adjusted = Optional.ofNullable(path).map(e -> {
			if (this.isRoot() && this.getValue() == null && // 根节点 空值验证
					path != null && path.size() >= 1 // 路径大于1 表明这是子节点查询
					&& (path.get(0) != null && !"null".equals(path.get(0)) // 空值检测
			)) { // 补充根节点的null
				final LinkedList<T> p = new LinkedList<>(path);
				p.add(0, null); // 自动填充首位null值节点
				return p;
			} else {
				return e;
			} // if
		}).orElse(path);

		if (path_adjusted.size() == 1 // 路径长度键值
				&& (Optional.ofNullable(path_adjusted.get(0)).map(s -> // 路径与名字或名字检查
				(Tuple2<Object, Object>) //
				(s instanceof String // 路径中的一个元素是否为 字符串类型
						? Tuple2.of(s, this.getName()) // 字符串类型采用 name 比较
						: Tuple2.of(s, this.getValue())) // 非字符穿类型采用 value 比较
				).map(Tuple2.bifun(Objects::equals)) //
						.orElse(null == this.getValue()) // 当path的第1项为null判断value是否为null
				)) { // 判断路径是否有与当前元素位置相符合
			return this;
		} else if (path_adjusted.size() > 0) { // 长路径有效
			final List<T> _path = path_adjusted.subList(1, path_adjusted.size());
			if (_path.size() < 1) {
				return null;
			} else {
				final T key = _path.get(0);
				final TrieNode<T> c = Optional.ofNullable(this.children.get(key)) // 尝试对值进行修复
						.orElseGet(() -> { // 尝试对路径与键值进行转换
							if (key == null) { // null 值已经在外面检索过了
								return null;
							} // if
							try { // 确保异常不中断执行
								final Class<?> kclass = this.children.keySet().stream() //
										.filter(Objects::nonNull).findFirst().map(e -> (Class<Object>) e.getClass())
										.orElse(Object.class);
								if (kclass == Integer.class && key.getClass().equals(String.class)) { // 键名
									final Object _key = key == null || "null".equals(key) ? null
											: Integer.parseInt(key + "");
									return this.children.get(_key);
								} else if ((kclass == Double.class || kclass == Float.class)
										&& key.getClass().equals(String.class)) { // 键名
									final Object _key = key == null || "null".equals(key) ? null
											: Double.parseDouble(key + "");
									return this.children.get(_key);
								} else if (kclass == String.class) {
									return this.children.get((T) (key + ""));
								} else {
									return null;
								} // if
							} catch (Exception e) {
								// do nothing
								e.printStackTrace();
							}
							return null;
						}); // orElseGet

				return c == null ? null : c.pathOf(_path);
			}
		} else { // path 长度为0
			assert path_adjusted.size() == 0 : "[]路径返回节点本身";
			return this;
		}
	}

	/**
	 * 获取子节点
	 * 
	 * @param t 字符名称。
	 * @return 获取节点
	 */
	public TrieNode<T> childOf(final T t) {
		return this.childOf(Collections.singletonList(t));
	}

	/**
	 * 获取子节点
	 * 
	 * 比如 对于一个 root 结构 :"/a/b/c" <br>
	 * root.getNode("/a/b") 获得 null 节点 <br>
	 * root.getNode("a/b") 则返回 b <br>
	 * 
	 * @param tt 节点字符序列 不包括当前节点字符
	 * @return 获取节点
	 */
	@SafeVarargs
	final public TrieNode<T> childOf(final T... tt) {
		return childOf(Arrays.asList(tt));
	}

	/**
	 * 获取子节点
	 * 
	 * 比如 对于一个 root 结构 :"/a/b/c" <br>
	 * root.getNode("/a/b") 获得 null 节点 <br>
	 * root.getNode("a/b") 则返回 b <br>
	 * 
	 * @param tt 节点字符序列 不包括当前节点字符
	 * @return 获取节点
	 */
	public TrieNode<T> childOf(final List<T> tt) {
		final ArrayList<T> path = new ArrayList<T>();
		// path.add(this.value); // 加入根节点字符
		path.addAll(this.parts());
		for (final T t : tt) {
			path.add(t);
		}

		return this.pathOf(path);
	}

	/**
	 * 获取子节点
	 * 
	 * @param indices 子节点的偏移序列,偏移从0开始
	 * @return 获取节点
	 */
	public TrieNode<T> childAt(final int... indices) {
		int n = indices.length;
		if (n == 1) {
			if (indices[0] >= this.size())
				return null;
			return this.childrenL().get(indices[0]);
		} else if (n > 1) {
			final TrieNode<T> node = this.childAt(indices[0]);
			if (node == null)
				return null;
			final int[] cdr = Arrays.copyOfRange(indices, 1, n);
			return node.childAt(cdr);
		} else {
			return null;
		}
	}

	/**
	 * 父节点 <br>
	 * 
	 * @return 父节点
	 */
	public TrieNode<T> getParent() {
		return this.getParentOpt().orElse(null);
	}

	/**
	 * 父节点 <br>
	 * 
	 * @return 父节点
	 */
	public Optional<TrieNode<T>> getParentOpt() {
		return Optional.ofNullable(this.parent);
	}

	/**
	 * 节点全路径 <br>
	 * 路径分隔符 为 /
	 * 
	 * @return 节点全路径
	 */
	public String getPath() {
		return this.path();
	}

	/**
	 * 获取节点字符(名称)
	 * 
	 * @return 节点字符
	 */
	public String getName() {
		return this.value + "";
	}

	/**
	 * 获取节点字符(名称) <br>
	 * value() 别名
	 * 
	 * @return 节点字符
	 */
	public T getValue() {
		return this.value();
	}

	/***
	 * 根据路径检索节点
	 * 
	 * 提取路径中的 trie节点比如 对于一个 root 结构 (root 指向“/”) : "/a/b/c" root.getNode("/a/b") 获得
	 * b 节点 root.getNode("a/b") 则返回 null, 因为路径 tt 没有包含 根节点名称
	 * 
	 * @param path 路径采用 [,/]进行分割. 路径信息 包含有当前节点（根节点名称) 字符的序列 比如： 当前节点 this 是 根节点 "/":
	 *             this.pathOf("/a/b/c") 返回 c 节点 当前节点 this 是 根节点 "a"
	 *             :this.pathOf("a/b/c") 返回 c 节点
	 * @return TrieNode
	 */
	@SafeVarargs
	public final TrieNode<T> getNode(final T... path) {
		return this.pathOf(path);
	}

	/***
	 * 根据路径检索节点 getNode的别名
	 * 
	 * 提取路径中的 trie节点比如 对于一个 root 结构 (root 指向“/”) : "/a/b/c" root.getNode("/a/b") 获得
	 * b 节点 root.getNode("a/b") 则返回 null, 因为路径 tt 没有包含 根节点名称
	 * 
	 * @param path 路径采用 [,/]进行分割. 路径信息 包含有当前节点（根节点名称) 字符的序列 比如： 当前节点 this 是 根节点 "/":
	 *             this.pathOf("/a/b/c") 返回 c 节点 当前节点 this 是 根节点 "a" <br>
	 *             :this.pathOf("a/b/c") 返回 c 节点 <br>
	 *             特别是当 根节点的为null,path长度大于1的时候,path的首字符可以省略.即 null,a,b 可以省略为 a,b
	 * @return TrieNode
	 */
	@SafeVarargs
	public final TrieNode<T> get(final T... path) {
		return this.pathOf(path);
	}

	/***
	 * 根据路径检索节点 getNode的别名
	 * 
	 * 提取路径中的 trie节点比如 对于一个 root 结构 (root 指向“/”) : "/a/b/c" root.getNode("/a/b") 获得
	 * b 节点 root.getNode("a/b") 则返回 null, 因为路径 tt 没有包含 根节点名称
	 * 
	 * @param path 路径采用 [,/]进行分割. 路径信息 包含有当前节点（根节点名称) 字符的序列 比如： 当前节点 this 是 根节点 "/":
	 *             this.pathOf("/a/b/c") 返回 c 节点 当前节点 this 是 根节点 "a" <br>
	 *             :this.pathOf("a/b/c") 返回 c 节点 <br>
	 *             特别是当 根节点的为null,path长度大于1的时候,path的首字符可以省略.即 null,a,b 可以省略为 a,b
	 * @return TrieNode
	 */
	@SafeVarargs
	public final <X, U> U get(final Function<TrieNode<T>, U> mapper, final T... path) {
		return Optional.ofNullable(this.get(path)).map(mapper).orElse(null);
	}

	/***
	 * 根据路径检索节点
	 * 
	 * 提取路径中的 trie节点比如 对于一个 root 结构 (root 指向“/”) : "/a/b/c" root.getNode("/a/b") 获得
	 * b 节点 root.getNode("a/b") 则返回 null, 因为路径 tt 没有包含 根节点名称
	 * 
	 * @param path 路径采用 [,/]进行分割. 路径信息 包含有当前节点（根节点名称) 字符的序列 比如： 当前节点 this 是 根节点 "/":
	 *             this.pathOf("/a/b/c") 返回 c 节点 当前节点 this 是 根节点 "a"
	 *             :this.pathOf("a/b/c") 返回 c 节点
	 * @return TrieNode
	 */
	@SuppressWarnings("unchecked")
	public TrieNode<T> getNode(final String path) {
		String[] _path = Stream.of(path.split("[,/]+")).map(String::trim) // 去除路径里面的空格
				.toArray(String[]::new);
		return this.pathOf((T[]) _path);
	}

	/**
	 * 获取节点字符(名称)
	 * 
	 * @return 节点字符
	 */
	public T value() {
		return this.value;
	}

	/**
	 * evaluate the value
	 * 
	 * @param <U>    the result type
	 * @param mapper the value evaluator t->u
	 * @return U type result
	 */
	public <U> U value(final Function<T, U> mapper) {
		return mapper.apply(value);
	}

	/**
	 * mutate the current node
	 * 
	 * @param <U>     the result type
	 * @param mutator the mutator trienode->u
	 * @return U type result
	 */
	public <U> U mutate(final Function<TrieNode<T>, U> mutator) {
		return mutator.apply(this);
	}

	/**
	 * 获取当前节点的阶层号
	 * 
	 * @return 当前的节点的阶层号从1开始。
	 */
	public int getLevel() {
		return this.parts().size();
	}

	/**
	 * 获取 TrieNode 节点 Map
	 */
	public Map<T, TrieNode<T>> getChildren() {
		return this.children;
	}

	/**
	 * 子节点
	 * 
	 * @return [trienode]
	 */
	public Collection<TrieNode<T>> childrenC() {
		return this.children.values();
	}

	/**
	 * 子节点
	 * 
	 * @return 子节点 列表 [trienode]
	 */
	public List<TrieNode<T>> childrenL() {
		return this.children.values().stream().collect(Collectors.toList());
	}

	/**
	 * 子节点
	 * 
	 * @return [trienode]
	 */
	public Stream<TrieNode<T>> childrenS() {
		return this.children.values().stream();
	}

	/**
	 * 把另一个node的子节点合并入自己的子节点
	 * 
	 * @param node 另一个节点,当node为this或是null时候，直接返回this
	 * @return 当前对象本身
	 */
	public TrieNode<T> merge(final TrieNode<T> node) {
		if (null == node || this == node) {
			// do nothing
		} else {
			this.children.putAll(node.children);
		}
		return this;
	}

	/**
	 * 兄弟排行
	 * 
	 * 第一个节点0,第二个节点1,第三个节点2,依次类图
	 * 
	 * @return
	 */
	public Integer offset() {
		if (this.parent == null) {
			return 0;
		} else {
			final Map<T, TrieNode<T>> cc = this.parent.children;
			if (cc == null) {
				return -1;
			}

			int i = 0;
			final Iterator<TrieNode<T>> itr = cc.values().iterator();
			boolean flag = false;
			while (itr.hasNext()) {
				if (itr.next() == this) {
					flag = true;
					break;
				} // if
				i++;
			}

			return flag ? i : -1;
		}
	}

	/**
	 * 模拟数字编号
	 * 
	 * @return 路径数字编号
	 */
	public String getPathCode() {
		return this.getPathCode(1);
	}

	/**
	 * 模拟数字编号
	 * 
	 * @param rootcode 根节点的编号
	 * @return 路径数字编号
	 */
	public String getPathCode(final int rootcode) {
		return this.parent == null ? rootcode + "" // 根节点编号
				: MessageFormat.format("{0}.{1}", this.parent.getPathCode(rootcode), this.offset() + 1);
	}

	/**
	 * 是否拥有一个单词标记。
	 * 
	 * @return 是否是一个单词的尾部节点。
	 */
	public boolean flag() {
		return this.attributes.bool("flag");
	}

	/**
	 * 设置一个flag
	 * 
	 * @param b 设置flag的标志, true 表示 设置,flase 取消设置
	 * @return TrieNode 对象本身已便于实现 链式编程
	 */
	public TrieNode<T> flag(final boolean b) {
		this.attributes.add("flag", b);
		return this;
	}

	/**
	 * 判断 tt 是否构成一个有效前缀。
	 * 
	 * @param tt 单词序列
	 * @return 单圈是否前缀
	 */
	public boolean isPrefix(final List<T> tt) {
		final TrieNode<T> t = this.pathOf(tt);
		return t != null;
	}

	/**
	 * 判断是否单词序列
	 * 
	 * @param word
	 * @return 判断是否单词序列
	 */
	public boolean isWord(final List<T> word) {
		final TrieNode<T> t = this.pathOf(word);
		return t != null && t.flag();
	}

	/**
	 * 是否是叶子节点，即 没有子节点的节点，注意根节点可以同时是叶子节点，此时的树只有一个节点
	 * 
	 * @return true 叶子节点,false 非叶子节点
	 */
	public boolean isLeaf() {
		return this.size() < 1;
	}

	/**
	 * 是否是根节点，即 没有子节点的节点，注意根节点可以同时是叶子节点，此时的树只有一个节点
	 * 
	 * @return true 根节点,false 非叶根节点
	 */
	public boolean isRoot() {
		return this.parent == null;
	}

	/**
	 * 返回所有的 叶子节点
	 *
	 * @return 叶子节点列表
	 */
	public List<TrieNode<T>> getAllLeaves() {
		return this.getAllLeaveS().toList();
	}

	/**
	 * 返回所有的 叶子节点
	 *
	 * @return 叶子节点流
	 */
	public Stream<TrieNode<T>> getAllLeaveS() {
		return this.stream().filter(TrieNode::isLeaf);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public Boolean attrBool(final String name) {
		return this.attributes.bool(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public Integer attrI4(final String name) {
		return this.attributes.i4(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public Long attrLng(final String name) {
		return this.attributes.lng(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public LocalDateTime attrLdt(final String name) {
		return this.attributes.ldt(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public LocalDate attrLd(final String name) {
		return this.attributes.ld(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public LocalTime attrLt(final String name) {
		return this.attributes.lt(name);
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>    源数据类型
	 * @param <U>    目标数据类型
	 * @param path   属性路径
	 * @param mapper 数值值映射 x-&lt;u
	 * @return U 结果 类型
	 */
	public <X, U> U attrPathget(final String path, final Function<X, U> mapper) {
		return this.attributes.pathget(path, mapper);
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return Double 结果 类型
	 */
	public <X> Double attrPathget2dbl(final String path) {
		return this.attributes.pathget(path, IRecord.obj2dbl());
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return Double 结果 类型
	 */
	public <X> Integer attrPathget2int(final String path) {
		return this.attributes.pathget(path, IRecord.obj2dbl()).intValue();
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return Number 结果 类型
	 */
	public <X> Number attrPathget2num(final String path) {
		return this.attributes.pathget(path, IRecord.obj2dbl());
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return String 结果 类型
	 */
	public <X> String attrPathget2str(final String path) {
		return this.attributes.pathget(path, e -> e + "");
	}

	/**
	 * 设置节点属性
	 * 
	 * @param name  属性名
	 * @param value 属性值
	 * @return 节点本身 以实现链式编程
	 */
	public TrieNode<T> attrSet(final String name, final Object value) {
		return this.setAttribute(name, value);
	}

	/**
	 * this.attrvalOpt().map(v -> mapper.apply((U) v)) 的简写
	 * 
	 * @param <U>    mapper 参数类型
	 * @param <V>    mapper 结果类型
	 * @param mapper u->v 值变换函数 对结果进行 mapper 变换
	 * @return Optional的value属性, 是attributes属性key为value的值，注意不是 节点value
	 */
	@SuppressWarnings("unchecked")
	public <U, V> Optional<V> attrvalOpt(final Function<U, V> mapper) {
		return this.attrvalOpt().map(v -> mapper.apply((U) v));
	}

	/**
	 * Optional.ofNullable(this.attributes.get("value")) 的简写
	 * 
	 * @return Optional的value属性, 是attributes属性key为value的值，注意不是 节点value
	 */
	public Optional<Object> attrvalOpt() {
		return Optional.ofNullable(this.getAttribute("value"));
	}

	/**
	 * this.getAttribute("value") 的简写
	 * 
	 * @return 是attributes属性key为value的值，注意不是 节点value
	 */
	public Object attrval() {
		return this.getAttribute("value");
	}

	/**
	 * this.attrSet("value", value) 的简写
	 * 
	 * @param value 值对象
	 * @return TrieNode 对象本身
	 */
	public TrieNode<T> attrval(final Object value) {
		this.attrSet("value", value);
		return this;
	}

	/**
	 * this.getAttribute("value")的结果变换
	 * 
	 * @param <U>    mapper 参数类型
	 * @param <V>    mapper 结果类型
	 * @param mapper u->v 值变换函数 对结果进行 mapper 变换
	 * @return V 类型的结果
	 */
	public <U, V> V attrval(final Function<U, V> mapper) {
		return this.attrval("value", mapper);
	}

	/**
	 * this.getAttribute("value")的结果变换
	 * 
	 * @param <U>    mapper 参数类型
	 * @param <V>    mapper 结果类型
	 * @param mapper u->v 值变换函数 对结果进行 mapper 变换
	 * @return V 类型的结果
	 */
	@SuppressWarnings("unchecked")
	public <U, V> V attrval(final String key, final Function<U, V> mapper) {
		return mapper.apply((U) this.getAttribute(key));
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public Object attr(final String name) {
		return this.getAttribute(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public Object attrGet(final String name) {
		return this.getAttribute(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param <U>    属性值类型
	 * @param name   属性名
	 * @param tclass 属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	public <U> U attrGet(final String name, final Class<U> tclass) {
		return (U) this.attributes.get(name);
	}

	/**
	 * 设置节点属性
	 * 
	 * @param name  属性名
	 * @param value 属性值
	 * @return 节点本身 以实现链式编程
	 */
	public TrieNode<T> attr(final String name, final Object value) {
		return this.setAttribute(name, value);
	}

	/**
	 * 设置节点属性
	 * 
	 * @param name  属性名
	 * @param value 属性值
	 * @return 节点本身 以实现链式编程
	 */
	public TrieNode<T> setAttribute(final String name, final Object value) {
		this.attributes.set(name, value);
		return this;
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名
	 * @return 属性值
	 */
	public Object getAttribute(final String name) {
		return this.attributes.get(name);
	}

	/**
	 * 获取节点名称
	 * 
	 * @return 节点属性集合
	 */
	public IRecord getAttributes() {
		return this.attributes;
	}

	/**
	 * 获取节点名称
	 * 
	 * @return 节点属性集合
	 */
	public IRecord attrs() {
		return this.getAttributes();
	}

	/**
	 * 节点的流化处理
	 * 
	 * @param <U>    mapper的结果类型
	 * @param mapper trienode->u 的 映射函数
	 * @return U类型的元素流
	 */
	public <U> Stream<U> stream(final Function<? super TrieNode<T>, U> mapper) {
		final Stack<TrieNode<T>> stack = new Stack<TrieNode<T>>(); // 使用堆栈 来 分小批次 的进行 中间数据的 暂存
		stack.push(this); // 根节点压入堆栈
		return Stream.iterate(this, // initial 初始值
				// hasNext 是根据前一个元素的 特征来 确定是否抵达 流的末端的。我们这里 却采用 stack 的空状态 来判断流结束标志
				prev -> !stack.empty(), // 判断流是否抵达终点。
				prev -> { // next 行数, 注意 next 是 优先于 hasNext 执行的。
					final TrieNode<T> node = stack.pop(); // 提取贮存 数据
					if (null != node) { // 空值判断
						final List<TrieNode<T>> childrenNodes = node.childrenL(); // 提取子节点列表
						Collections.reverse(childrenNodes); // 倒排序
						childrenNodes.forEach(stack::push); // 依次逐个添加子节点。
						if (stack.empty()) { // 增加一个 空节点以保证 最后一个节点可以 顺利输出。
							stack.push(null); // 空值的占位元素。这个 空值占位是 专为 hasNext而设计的。
						} // if
					} // node !=null
					return node;
				}).map(mapper) //
				.skip(1); // 剔除initial
	}

	/**
	 * 节点的流化处理
	 * 
	 * @return TrieNode的流
	 */
	public Stream<TrieNode<T>> stream() {
		return this.stream(e -> e);
	}

	/**
	 * 节点的流化处理,stream的别名。 (递归遍历)
	 * 
	 * @param <U>    mapper的结果类型
	 * @param mapper trienode->u 的 映射函数
	 * @return U 类型的元素流
	 */
	public <U> Stream<U> nodeS(final Function<? super TrieNode<T>, U> mapper) {
		return this.stream(mapper);
	}

	/**
	 * 节点的流化处理
	 * 
	 * @return TrieNode的流
	 */
	public Stream<TrieNode<T>> nodeS() {
		return this.stream();
	}

	/**
	 * 节点的流化处理
	 * 
	 * @param <U>    元素内容
	 * @param mapper a non-interfering, statelessfunction to apply to each element
	 * @return TrieNode 的流
	 */
	public <U> Stream<U> leafS(final Function<? super TrieNode<T>, U> mapper) {
		return this.stream(e -> e).filter(e -> e.isLeaf()).map(mapper);
	}

	/**
	 * 节点的流化处理
	 * 
	 * @param <U>    元素内容
	 * @param mapper a non-interfering, statelessfunction to apply to each element
	 * @return TrieNode 的流
	 */
	public <U> List<U> leafs(final Function<? super TrieNode<T>, U> mapper) {
		return this.stream(e -> e).filter(e -> e.isLeaf()).map(mapper) //
				.collect(Collectors.toList());
	}

	/**
	 * 节点的流化处理
	 * 
	 * @param predicate 过滤函数 trienode->boolean
	 * @return TrieNode 的流
	 */
	public Stream<TrieNode<T>> filter(final Predicate<TrieNode<T>> predicate) {
		return this.stream().filter(predicate);
	}

	/**
	 * 子节点个数
	 * 
	 * @return 子节点个数
	 */
	public int size() {
		return this.children.size();
	}

	/**
	 * 遍历访问
	 * 
	 * @param cs 单词回调函数
	 */
	public void forEach(final Consumer<TrieNode<T>> cs) {
		cs.accept(this);
		this.children.forEach((t, e) -> e.forEach(cs));
	}

	/**
	 * 
	 */
	@Override
	public String json(final BiFunction<StringBuilder, TrieNode<T>, String> pre_processor,
			final BiFunction<StringBuilder, TrieNode<T>, String> post_processor) {
		return INodeWriter.writeJson(this, p -> p.childrenL(), pre_processor, post_processor);
	}

	/**
	 * 字符串格式化
	 */
	@Override
	public String toString() {
		return MessageFormat.format("{0}", this.value);
	}

	/**
	 * 构造函数
	 * 
	 * <pre>
	 * {@code
	 * final var rootNode = REC(pivotLines).tupleS().parallel().reduce(TrieNode.of("root"), // 以REC形式分解成阶层元素(K,V)流,而后reduce成树形结构
	 * 		ndaccum((leaf, p) -> leaf.attrSet("value", p._2), TrieNode::addPart), TrieNode::merge); // 数据透视分阶层统计
	 * }</pre>
	 * 
	 * @param <T> 元素值类型
	 * @param t   元素的值
	 * @return TrieNode&lt;T&gt; 类型的节点
	 */
	public static <T> TrieNode<T> of(final T t) {
		return new TrieNode<>(t);
	}

	/**
	 * TrieNode 归集器
	 * 
	 * @param <K>  键名列表类型
	 * @param <T>  键名元素类型
	 * @param <V>  键值元素类型
	 * @param root 根节点名
	 * @return 根节点的TrieNode [([t],v)] -> trie
	 */
	public static <K extends Iterable<T>, T, V> Collector<Tuple2<K, V>, ?, TrieNode<T>> trieclc(final T root) {
		return TrieNode.trieclc(root, "value");
	}

	/**
	 * TrieNode 归集器
	 * 
	 * @param <K>      键名列表类型
	 * @param <T>      键名元素类型
	 * @param <V>      键值元素类型
	 * @param root     根节点名
	 * @param valueKey 值属性的key,valueKey 若为null则，使用默认值 "valueKey" 替代。
	 * @return 根节点的TrieNode [([t],v)] -> trie
	 */
	public static <K extends Iterable<T>, T, V> Collector<Tuple2<K, V>, ?, TrieNode<T>> trieclc(final T root,
			final String valueKey) {
		return TrieNode.trieclc(root, (node, v) -> {
			final String key = Optional.ofNullable(valueKey).orElse("valueKey");
			node.attrSet(key, v);
		});
	}

	/**
	 * TrieNode 归集器
	 * 
	 * @param <K>  键名列表类型
	 * @param <T>  键名元素类型
	 * @param <V>  键值元素类型
	 * @param root 根节点名
	 * @param cons 结果回调函数 (trienode:生成节点,v:节点所标记的值value)->{},可以在 cons 中设置
	 *             trienode的属性，比如指定一个value属性: trienode.attrSet("value",v);
	 * @return 根节点的TrieNode [([t],v)] -> trie
	 */
	public static <K extends Iterable<T>, T, V> Collector<Tuple2<K, V>, ?, TrieNode<T>> trieclc(final T root,
			BiConsumer<TrieNode<T>, V> cons) {
		return Collector.of(() -> TrieNode.of(root), (aa, a) -> {
			cons.accept(aa.addParts(a._1), a._2);
		}, (aa, bb) -> aa);
	}

	/**
	 * 属性变换:valueof, 与 get 方法合在一起使用 node.get(attr_of(ndint), 2, 0)
	 * 
	 * @param <T>    TrieNode的元素值类型
	 * @param <A>    属性值类型
	 * @param <U>    变换函数类型
	 * @param name   属性名称
	 * @param mapper a->v 值变换函数 对结果进行 mapper 变换
	 * @return node->U
	 */
	public static <T, A, U> Function<TrieNode<T>, U> attr_of(final String name, final Function<A, U> mapper) {
		return node -> node.attrval(name, mapper);
	}

	/**
	 * 属性变换:valueof, 与 get 方法合在一起使用 node.get(attr_of(ndint), 2, 0)
	 * 
	 * @param <T>    TrieNode的元素值类型
	 * @param <A>    属性值类型
	 * @param <U>    变换函数类型
	 * @param mapper a-&lt;v 值变换函数 对结果进行 mapper 变换
	 * @return node-&lt;U
	 */
	public static <T, A, U> Function<TrieNode<T>, U> attr_of(final Function<A, U> mapper) {
		return node -> node.attrval(mapper);
	}

	private final T value; // 节点字符
	private final TrieNode<T> parent; // 父节点
	private final IRecord attributes = IRecord.REC(); // 节点属性
	private final Map<T, TrieNode<T>> children;// 子节点
	private final Supplier<Map<T, TrieNode<T>>> children_ctor;

}// TrieNode
