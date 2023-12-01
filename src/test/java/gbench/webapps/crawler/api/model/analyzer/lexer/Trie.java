package gbench.webapps.crawler.api.model.analyzer.lexer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 
 * Trie字典树/前缀树的节点
 * 
 * @author gbench
 *
 * @param <T> 节点的值类型
 */
public class Trie<T> {

	/**
	 * 构造函数
	 */
	public Trie() {
		this.value = null;
		this.level = 0;
		this.parent = null;
	}

	/**
	 * 构造函数
	 * 
	 * @param value 元素值
	 */
	public Trie(final T value) {
		this.value = value;
		this.level = 0;
		this.parent = null;
	}

	/**
	 * 构造函数
	 * 
	 * @param value 元素值
	 * @param level 元素层级
	 */
	public Trie(final T value, final int level) {
		this.value = value;
		this.level = level;
	}

	/**
	 * 获取节点的子元素
	 * 
	 * @return
	 */
	public Collection<Trie<T>> getChildren() {
		return this.children.values();
	}

	/**
	 * 获取节点的值
	 * 
	 * @return
	 */
	public T getValue() {
		return this.value;
	}

	/**
	 * 父节点
	 * 
	 * @return
	 */
	public Trie<T> getParent() {
		return this.parent;
	}

	/**
	 * 设置父节点
	 * 
	 * @param parent
	 * @return
	 */
	public Trie<T> setParent(final Trie<T> parent) {
		this.parent = parent;
		return this;
	}

	/**
	 * 节点所在层次 根节点层次为0,子节点的为父节点的层增加1 获取层级信息
	 * 
	 * @return
	 */
	public int getLevel() {
		return this.level;
	}

	/**
	 * 在当前节点中增加一个 点元素 point,注意这个是只添加一个点的操作 <br>
	 * 对于添加多个字符的情况，请示使用 Trie.addPoints2Trie 函数来完成。通常在使用中， <br>
	 * 不推荐直接调用addPoint函数，请使用 Trie.addPoints2Trie 系列函数。 <br>
	 * 
	 * @param point 点值
	 * @return 添加后的 节点
	 */
	public Trie<T> addPoint(final T point) {
		var trie = this.children.get(point);// 检索point值所代表的节点
		if (trie != null) {
			return trie;
		} else {
			trie = new Trie<T>(point, this.level + 1);
			trie.setParent(this);// 设置父节点
			this.children.put(point, trie);// 设置子节点
			return trie;
		} // if
	}

	/**
	 * 是否为叶子节点（没有孩子节点)
	 * 
	 * @return 是否为叶子节点
	 */
	public boolean isLeaf() {
		return this.children.size() < 1;
	}

	/**
	 * 前缀检测
	 * 
	 * @param points
	 * @return
	 */
	public boolean isPrefix(final T[] points) {
		return this.isPrefix(Arrays.asList(points));
	}

	/**
	 * 获取points 路径所在的Trie
	 * 
	 * @param points 前缀的元素列表
	 * @return points 是否是一条词语前缀
	 */
	public Trie<T> getTrie(final T[] points) {
		return this.getTrie(Arrays.asList(points));
	}

	/**
	 * 获取points 路径所在的Trie
	 * 
	 * @param points 前缀的元素列表
	 * @return points 是否是一条词语前缀
	 */
	@SafeVarargs
	public final Optional<Trie<T>> opt(final T... points) {
		return Optional.ofNullable(this.getTrie(points));
	}

	/**
	 * 删除指定前缀的节点<br>
	 * 对于含有子节点的节点给与不予强制删除
	 * 
	 * @param points 前缀的元素列表
	 * @return points 是否是一条词语前缀
	 */
	public Trie<T> removeTrie(final List<T> points) {
		return removeTrie(points, false);
	}

	/**
	 * 删除指定前缀的节点<br>
	 * 对于含有子节点的节点给与不予强制删除
	 * 
	 * @param points 前缀的元素列表
	 * @return points 是否是一条词语前缀
	 */
	public Trie<T> removeTrie(final T[] points) {
		return removeTrie(Arrays.asList(points), false);
	}

	/**
	 * 删除指定前缀的节点
	 * 
	 * @param points 前缀的元素列表
	 * @param force  对于含有子节点的节点给与强制删除
	 * @return points 是否是一条词语前缀
	 */
	public Trie<T> removeTrie(final T[] points, final boolean force) {
		return removeTrie(Arrays.asList(points), force);
	}

	/**
	 * 删除指定前缀的节点
	 * 
	 * @param points 前缀的元素列表
	 * @param force  对于含有子节点的节点给与强制删除
	 * @return points 是否是一条词语前缀
	 */
	public Trie<T> removeTrie(final List<T> points, final boolean force) {
		final var trie = this.getTrie(points);
		if (trie == null) {
			return null;
		}

		final var parent = trie.getParent();
		if (parent != null) {// 含有父节点
			final var child = parent.children.get(points.get(points.size() - 1));
			if (child != null) {
				if (!child.isLeaf() && force) {
					// System.err.println("删除了一个拥有:"+child.getChildren().size()+"个孩子的节点");
				} else if (!force) {
					System.err.println("由于存在" + child.getChildren().size() + "个孩子,不予删除");
					return trie;
				} // if
				parent.children.remove(points.get(points.size() - 1));
			} else {// 不可能出现的情况
				System.err.println("这个情况不符合逻辑程序设计出现错误");
			} // if
		} // if

		return trie;
	}

	/**
	 * 获取points 路径所在的Trie
	 * 
	 * @param points 前缀的元素列表
	 * @return points 是否是一条词语前缀
	 */
	public Trie<T> recursive_getTrie(final List<T> points) {
		if (points == null || points.size() < 1)
			return null;
		if (this.getValue() == null) {// 根节点
			final var child = this.children.get(points.get(0));
			if (child == null) {
				return null;
			} else {
				return child.recursive_getTrie(points);
			}
		} else {// 非根节点
			if (this.getValue().equals(points.get(0))) {// 根节点比较
				if (points.size() < 2) {// points 集合中只有一个 点
					return this;
				} else {// points 中至少有2个点
					final var child = this.children.get(points.get(1));// 获取子节点
					if (child == null) {
						return null;
					} else {
						return child.recursive_getTrie(points.subList(1, points.size()));// 步进到 子节点比较，移除一个元素之后继续比较
					}
				} // if
			} else {// 前缀不匹配
				return null;
			} // 首字符检查
		} // 根节点&非根节点
	}

	/**
	 * 
	 * @param points
	 * @return
	 */
	public Trie<T> loop_getTrie(final List<T> points) {
		if (points == null || points.size() < 1) {
			return null;
		}
		var trie = this.getValue() == null ? this.getChild(points.get(0)) : this;
		if (trie == null) {
			return null;
		}

		var pitr = points.iterator();
		var tval = trie.getValue();// trie 节点值
		var pval = pitr.next();// 提取第一个节点

		while (pitr.hasNext()) {
			if (!tval.equals(pval))
				return null;
			pval = pitr.next();// 提取point值
			trie = trie.getChild(pval);
			if (trie == null)
				return null;
			tval = trie.getValue();// 提取节点值
		} // while

		return !tval.equals(pval) ? null : trie;
	}

	/**
	 * 
	 * @param points
	 * @return
	 */
	public Trie<T> getTrie(final List<T> points) {
		return USE_RECURSIVE_GET_TRIE ? recursive_getTrie(points) : loop_getTrie(points);
	}

	/**
	 * 判断是否是前缀
	 * 
	 * @param points 前缀的元素列表
	 * @return points 是否是一条词语前缀
	 */
	public boolean isPrefix(final List<T> points) {
		return this.getTrie(points) != null;
	}

	/**
	 * 获取节点路径
	 * 
	 * @param trie
	 * @return 获取节点上的路径数据
	 */
	public static <T> Stream<T> getPath(final Trie<T> trie) {
		if (trie == null || trie.getParent() == null)
			return Stream.empty();
		else
			return Stream.concat(getPath(trie.getParent()), Stream.of(trie.getValue()));
	}

	/**
	 * 获取节点路径
	 * 
	 * @param trie
	 * @return 获取节点上的路径数据
	 */
	public Stream<T> getPath() {
		if (this.getParent() == null)
			return Stream.empty();
		else
			return Stream.concat(getPath(this.getParent()), Stream.of(this.getValue()));
	}

	/**
	 * 获取子节点
	 * 
	 * @param point 点对象
	 * @return Trie节点
	 */
	public Trie<T> getChild(final T point) {
		return this.children.get(point);
	}

	/**
	 * 获取节点路径
	 * 
	 * @param trie
	 * @return 获取节点上的路径数据
	 */
	public <U> U getPath(final Function<Stream<T>, U> mapper) {
		return mapper.apply(this.getPath());
	}

	/**
	 * 格式化成字符串形式
	 */
	public String toString() {
		return this.value + "@" + this.level;
	}

	/**
	 * 根据属性名获取属性值
	 * 
	 * @param key 属性名
	 * @return 属性值
	 */
	public Object getAttribute(final Object key) {
		return this.attributes.get(key);
	}

	/**
	 * 根据属性名获取属性值:字符串类型的属性值
	 * 
	 * @param key 属性名
	 * @return 属性值
	 */
	public String strAttr(final Object key) {
		final var obj = this.attributes.get(key);
		return obj == null ? null : obj.toString();
	}

	/**
	 * 获取属性集合
	 * 
	 * @return 属性集合
	 */
	public Map<Object, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * 添加节点 属性 同名 key 将给与覆盖
	 * 
	 * @param key   属性名
	 * @param value 属性值
	 * @return Trie 对象本身用于实现链式编程
	 */
	public Trie<T> addAttribute(final Object key, final Object value) {

		this.attributes.put(key, value);
		return this;
	}

	/**
	 * 节点遍历
	 * 
	 * @param <T>
	 * @param trie 节点对象
	 * @param cs   回调函数
	 */
	public void traverse(final Consumer<Trie<T>> cs) {
		Trie.traverse(this, cs);
	}

	/**
	 * 节点遍历
	 * 
	 * @param <T>
	 * @param trie 节点对象
	 * @param cs   回调函数
	 */
	public static <T> void traverse(final Trie<T> trie, final Consumer<Trie<T>> cs) {
		cs.accept(trie);
		trie.children.forEach((k, v) -> traverse(v, cs));
	}

	/**
	 * 生成一个 Trie树结构
	 * 
	 * @param <T>   元素类型
	 * @param lines 关键词列表
	 * @return Trie 树结构
	 */
	public static <T> Trie<T> create(final List<List<T>> lines) {
		final var trie = new Trie<T>(null);
		for (var line : lines)
			addPoints2Trie(trie, line);
		return trie;
	}

	/**
	 * 把 元素点集合路径points 附加到 trie 节点
	 * 
	 * @param <T>    元素类型
	 * @param points 即 line 关键词列表
	 * @return points 所在的末梢节点 Trie
	 */
	public static <T> Trie<T> addPoints2Trie(final Trie<T> trie, final List<T> points) {
		return addPoints2Trie(trie, Stream.of(points));
	}

	/**
	 * 把 元素点集合路径points 附加到 trie 节点
	 * 
	 * @param <T>    元素类型
	 * @param trie   附加节点
	 * @param points 即 line 关键词列表, 点元素路径
	 * @return points 所在的末梢节点 Trie
	 */
	@SafeVarargs
	public static <T> Trie<T> addPoints2Trie(final Trie<T> trie, final T... points) {
		return addPoints2Trie(trie, Arrays.asList(points));
	}

	/**
	 * 把 元素点集合路径points 附加到 trie 节点
	 * 
	 * @param <T>    元素类型
	 * @param trie   附加节点
	 * @param points 即 line 关键词列表, 点元素路径
	 * @return Trie 树结构: 设置的最后一个节点的 trie对象
	 */
	public static <T> Trie<T> addPoints2Trie(final Trie<T> trie, final T[][] lines) {
		return addPoints2Trie(trie, Stream.of(lines).map(line -> Arrays.asList(line)));
	}

	/**
	 * 把 元素点集合路径points 附加到 trie 节点
	 * 
	 * @param <T>   元素类型
	 * @param trie  附加节点
	 * @param lines 关键词列表 多语料库:每个line都是一个List<T> 的 points 点集
	 * @return Trie 树结构: 设置的最后一个节点的 trie对象
	 */
	public static <T> Trie<T> addPoints2Trie(final Trie<T> trie, final Stream<List<T>> lines) {
		final var ar = new AtomicReference<Trie<T>>();
		lines.forEach(line -> {// 依次加入数据点集行
			var cur_trie = trie;
			for (var piont : line)
				cur_trie = cur_trie.addPoint(piont);
			ar.set(cur_trie);
		});

		return ar.get();
	}

	protected int level;// 节点所在层次 根节点层次为0,子节点的为父节点的层增加1
	protected T value;// 节点的值
	protected Trie<T> parent;// 父节点
	protected Map<T, Trie<T>> children = new HashMap<T, Trie<T>>();// 子节点集合
	protected final Map<Object, Object> attributes = new HashMap<Object, Object>();// 属性集合

	public static boolean USE_RECURSIVE_GET_TRIE = true; // 是否使用 recursive 版本的 getTrie

}
