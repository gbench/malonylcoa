package gbench.util.jdbc.node;

import java.text.MessageFormat;
import java.util.Map;
import java.util.List;
import java.util.Stack;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import gbench.util.jdbc.kvp.IRecord;

/**
 * 前缀树节点
 * 
 * @author gbench
 *
 * @param <T> 节点名称的类型。
 */
public class TrieNode<T> {

	/**
	 * 前缀树的节点
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
		return children.compute(name, (k, v) -> v == null ? new TrieNode<>(k, this, this.children_ctor) : v);
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
			var node = this;
			for (final var part : parts) {
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
		final var s = Stream.of(this.value);
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
	public <U> U collect(final Collector<? super T, ?, U> collector) {
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
	 * 
	 * 路径分隔符 为 /
	 * 
	 * 
	 * @return 节点全路径
	 */
	public String path() {
		return this.partS().map(e -> e + "").collect(Collectors.joining("/"));
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
	 * @param path 路径信息 包含有当前节点（根节点名称) 字符的序列 比如：<br>
	 *             当前节点 this 是 根节点 "/": this.pathOf("/a/b/c") 返回 c 节点 <br>
	 *             当前节点 this 是 根节点 "a" :this.pathOf("a/b/c") 返回 c 节点
	 * @return trie节点
	 */
	public TrieNode<T> pathOf(final List<T> path) {
		if (path.size() == 1 && this.getName().equals(path.get(0))) {
			return this;
		} else {
			final var _path = path.subList(1, path.size());
			final var c = this.children.get(_path.get(0));
			return c == null ? null : c.pathOf(_path);
		} // if
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
		final var path = new ArrayList<T>();
		// path.add(this.value); // 加入根节点字符
		path.addAll(this.parts());
		for (final var t : tt) {
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
			final var node = this.childAt(indices[0]);
			if (node == null)
				return null;
			final var cdr = Arrays.copyOfRange(indices, 1, n);
			return node.childAt(cdr);
		} else {
			return null;
		}
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
	 * @return [trienode]
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
			final var cc = this.parent.children;
			if (cc == null) {
				return -1;
			}

			int i = 0;
			final var itr = cc.values().iterator();
			var flag = false;
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
		this.attributes.set("flag", b);
		return this;
	}

	/**
	 * 判断 tt 是否构成一个有效前缀。
	 * 
	 * @param tt 单词序列
	 * @return 单圈是否前缀
	 */
	public boolean isPrefix(final List<T> tt) {
		final var t = this.pathOf(tt);
		return t != null;
	}

	/**
	 * 判断是否单词序列
	 * 
	 * @param word
	 * @return 判断是否单词序列
	 */
	public boolean isWord(final List<T> word) {
		final var t = this.pathOf(word);
		return t != null && t.flag();
	}

	/**
	 * 是否是叶子节点，即 没有根节点 的节点
	 * 
	 * @return true 叶子节点,false 非叶子节点
	 */
	public boolean isLeaf() {
		return this.size() < 1;
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>    源数据类型
	 * @param <U>    目标数据类型
	 * @param path   属性路径
	 * @param mapper 数值值映射 x->u
	 * @return U 结果 类型
	 */
	public <X, U> U pattr(final String path, final Function<X, U> mapper) {
		return this.attributes.path2target(path, mapper);
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return Double 结果 类型
	 */
	public <X> Double pattr2dbl(final String path) {
		return this.attributes.path2target(path, IRecord.obj2dbl());
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return Number 结果 类型
	 */
	public <X> Number pattr2num(final String path) {
		return this.attributes.path2target(path, IRecord.obj2num);
	}

	/**
	 * 节点属性
	 * 
	 * @param <X>  源数据类型
	 * @param path 属性路径
	 * @return String 结果 类型
	 */
	public <X> String pattr2str(final String path) {
		return this.attributes.path2target(path, e -> e + "");
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
	public Object attr(final String name) {
		return this.attributes.get(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param <U>    属性值类型
	 * @param name   属性名
	 * @param tclass 属性类型
	 * @return 属性值
	 */
	public <U> U attr(final String name, final Class<U> tclass) {
		return this.attributes.get(name, tclass);
	}

	/**
	 * 获取属性值
	 * 
	 * @param <U>  属性值类型
	 * @param name 属性名
	 * @param u    属性类型占位符
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	public <U> U attr(final String name, final U u) {
		return (U) this.attributes.get(name);
	}

	/**
	 * 节点的流化处理
	 * 
	 * @param <U>    属性值类型
	 * @param mapper trienode->u 的 映射函数
	 * @return U类型的 的流
	 */
	public <U> Stream<U> stream(final Function<? super TrieNode<T>, U> mapper) {
		final var stack = new Stack<TrieNode<T>>(); // 使用堆栈 来 分小批次 的进行 中间数据的 暂存
		stack.push(this); // 根节点压入堆栈
		return Stream.iterate(this, // initial 初始值
				// hasNext 是根据前一个元素的 特征来 确定是否抵达 流的末端的。我们这里 却采用 stack 的空状态 来判断流结束标志
				prev -> !stack.empty(), // 判断流是否抵达终点。
				prev -> { // next 行数, 注意 next 是 优先于 hasNext 执行的。
					final var node = stack.pop(); // 提取贮存 数据
					if (null != node) { // 空值判断
						final var childrenNodes = node.childrenL(); // 提取子节点列表
						Collections.reverse(childrenNodes); // 倒排序
						childrenNodes.forEach(stack::push); // 依次逐个添加子节点。
						if (stack.empty()) { // 增加一个 空节点以保证 最后一个节点可以 顺利输出。
							stack.push(null); // 空值的占位元素。这个 空值占位是 专为 hasNext而设计的。
						} // if
					} // node !=null
					return node;
				}).map(mapper);
	}

	/**
	 * 节点的流化处理
	 * 
	 * @return TrieNode 的流
	 */
	public Stream<TrieNode<T>> stream() {
		return this.stream(e -> e).skip(1);
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
	 * 字符串格式化
	 */
	public String toString() {
		return MessageFormat.format("{0}", this.value);
	}

	/**
	 * 构造函数
	 * 
	 * @param <T> 元素值类型
	 * @param t   元素的值
	 * @return TrieNode<T> 类型的节点
	 */
	public static <T> TrieNode<T> of(final T t) {
		return new TrieNode<T>(t);
	}

	private final T value; // 节点字符
	private final TrieNode<T> parent; // 父节点
	private final IRecord attributes = IRecord.REC(); // 节点属性
	private final Map<T, TrieNode<T>> children;// 子节点
	private final Supplier<Map<T, TrieNode<T>>> children_ctor;
}// TrieNode
