package gbench.util.tree;

import gbench.util.lisp.IRecord;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 树结构的节点。<br>
 * Node 节点为 record的值类型 做了特殊设计(根据record的键值对儿来获取节点名称,默认按顺序提取
 * name,path,Activity字段值作为getName的返回这)，不过也可以用于非IRecord的值类型 <br>
 * 注意:<br>
 * 由于Node考虑到通用性 在实现 equals做了 路径的遍历处理。这是一种效率很低的 方法。
 * 在具体使用情形中可对Node进行继承而特化出一个简单的版本，来提高效率。<br>
 * 例如:<br>
 * &#47;&#42;&#42;<br>
 * &#42; 默认的Node对equals 以及 hashcode 的实现太过繁琐 因此这里给予 实现一个简单版本。 <br>
 * &#42; 去除了很多通用条件 仅仅用于一个 临时基于Elem&lt;String&gt;本地化的实现。用特化来提高<br>
 * &#42; 特化：子类化一个Node用于对实现Node的equals比较 <br>
 * &#42;&#47;<br>
 * final var quicknode_counter = new AtomicInteger(0);// quicknode_counter
 * 的唯一标识生成器 <br>
 * final class QuickNode extends Node&lt;Elem&lt;String&gt;&gt; { <br>
 * //constructor <br>
 * public QuickNode(final Elem &lt;String &gt; value) { <br>
 * super(value); <br>
 * } <br>
 * <br>
 * &#64;Override <br>
 * public int hashCode() { <br>
 * return id; <br>
 * } <br>
 * <br>
 * &#64;Override <br>
 * public boolean equals(final Object obj) { <br>
 * if(obj==null)return false; <br>
 * return obj.getClass()== QuickNode.class?
 * ((QuickNode)obj).id==(this.id):false; <br>
 * } <br>
 * <br>
 * &#64;Override <br>
 * public String getName() { // c重写名称获取函数 <br>
 * return this.getValue()._1(); <br>
 * } <br>
 * <br>
 * private final int id = quicknode_counter.getAndIncrement();// 本地节点标识 <br>
 * }// QuickNode <br>
 *
 * @param <T> 节点中的元素的类型
 * @author gbench
 */
public class Node<T> implements INodeWriter<Node<T>> {
	/**
	 * 拷贝钩爪函数
	 *
	 * @param node 节点对象
	 */
	public Node(final Node<T> node) {
		this.value = node.value;
		this.attributes = IRecord.REC(node.attributes);
		this.children = node.children;
		this.lambda_name = node.lambda_name;
		this.parent = node.parent;
	}

	/**
	 * 节点的值
	 *
	 * @param value 节点的值
	 */
	public Node(final T value) {
		this.value = value;
	}

	/**
	 * 构造函数 含有名称解析器
	 *
	 * @param value       节点的值
	 * @param lambda_name 节点的名称获取函数: node->String
	 */
	public Node(final T value, final Function<Node<T>, String> lambda_name) {
		this.value = value;
		this.lambda_name = lambda_name;
	}

	/**
	 * 构造函数 带有父节点
	 * 
	 * @param parent 父节点
	 * @param value  节点的值
	 */
	public Node(final Node<T> parent, final T value) {
		this.value = value;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	/**
	 * 克隆函数
	 */
	@Override
	public Node<T> clone() {
		try {
			// @SuppressWarnings({ "unchecked", "unused" })
			// Node<T> tNode = (Node<T>) super.clone(); // 之所以写这一行 是为了 去除idea的编译警告
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Node<>(this);
	}

	/**
	 * 获取节点的值信息
	 *
	 * @return 节点的值对象
	 */
	public T getValue() {
		return value;
	}

	/**
	 * 获取节点的值信息
	 *
	 * @param <U>    结果类型
	 * @param mapper 值的结果变换
	 * @return U 类型的结果
	 */
	public <U> U getValue(final Function<T, U> mapper) {
		return mapper.apply(value);
	}

	/**
	 * 返回节点自身，以便实现链式编程
	 *
	 * @param value 待设置的节点的值
	 * @return 返回节点自身，以便实现链式编程
	 */
	public Node<T> setValue(final T value) {
		this.value = value;
		return this;
	}

	/**
	 * 简单的额取值写法 getValue 的别名
	 *
	 * @return 节点的值对象
	 */
	public T val() {
		return getValue();
	}

	/**
	 * 简单的额设置值的写法 setValue 的别名
	 *
	 * @return 返回节点自身，以便实现链式编程
	 */
	public Node<T> val(final T value) {
		return this.setValue(value);
	}

	/**
	 * 简单的额设置值的写法
	 *
	 * @param fld_picker 值字段的提取器
	 * @return 返回节点自身，以便实现链式编程
	 */
	public <U> U val(final Function<T, U> fld_picker) {
		return fld_picker.apply(this.getValue());
	}

	/**
	 * 设置lambda_name的选项
	 *
	 * @param lambda_name 名称函数的生成表达式。
	 * @return 节点对象本身 用于实现链式编程
	 */
	public Node<T> setName(final Function<Node<T>, String> lambda_name) {
		this.lambda_name = lambda_name;
		return this;
	}

	/**
	 * 获得节点名称，节点名是通过信息值进行访问的。 nameit 根据节点生成对应的名字
	 *
	 * @return 获取产品名称
	 */
	public String getName(final Function<Node<T>, String> nameit) {
		return nameit.apply(this);
	}

	/**
	 * 获得节点名称，节点名是通过信息值进行访问的。
	 *
	 * @param nameflds 可能表示名字的字段名:比如 【“name”,“path”,“Activity”】 的字符串数组
	 * @return 节点的名称 如果是视IRecord类型的值对象，首先尝试获取IRecord的path属性，<br>
	 *         如果path不存在尝试获取 name属性，如果 仍旧不存在在，就按照一般值对象进行处理器，即把节点值得字符串给予返回。
	 */
	public String getName(final String[] nameflds) {
		if (lambda_name != null) {
			return lambda_name.apply(this);
		} else {
			final IRecord _value = IRecord.REC("name", "-"); // 默认名称

			if (value instanceof IRecord) {
				_value.add(value);
			} else if (value instanceof Map) {
				_value.add(value);
			} else {
				_value.add("name", value + "");
			} // if

			Object name = null;// name 字段的值
			if (nameflds != null) {
				for (final String fld : nameflds) {// 尝试提取默认的名字字段，
					if ((name = _value.get(fld)) != null) {
						break;
					} // if
				} // for
			} // if

			if (name != null) {
				return name + "";
			} else {
				return value + "";
			} // if

		} // lambda_name
	}

	/**
	 * 获得节点名称，节点名是通过信息值进行访问的。
	 *
	 * @return 节点的名称 如果是视IRecord类型的值对象，首先尝试获取IRecord的path属性，默认把 name,path,Activity
	 *         顺序提取 名称<br>
	 *         如果path不存在尝试获取 name属性，如果 仍旧不存在在，就按照一般值对象进行处理器，即把节点值得字符串给予返回。
	 */
	public String getName() {
		final String[] nameflds = new String[] { "name", "path", "Activity" };
		return this.getName(nameflds);// 默认可能的用于名字的字段)
	}

	/**
	 * 获取节点的父节点，根节的父节点为null
	 *
	 * @return 返回父节点
	 */
	public Node<T> getParent() {
		return this.parent;
	}

	/**
	 * 获得节点路径
	 *
	 * @return 从根节点到当前节点的路径
	 */
	public String getPath() {
		return this.getPath(this);
	}

	/**
	 * 获得节点路径
	 *
	 * @param name_mapper 节点名称变换器 : node->String
	 * @return 从根节点到当前节点的路径
	 */
	public String getPath(final Function<Node<T>, String> name_mapper) {
		return Node.getPath(this, name_mapper);
	}

	/**
	 * 获取节点路径
	 *
	 * @return 节点路径
	 */
	public Stream<Node<T>> getPathNodeS() {
		return Node.getPathNodeS(this);
	}

	/**
	 * 获取节点路径
	 *
	 * @return 节点路径
	 */
	public List<Node<T>> getPathNodes() {
		return Node.getPathNodeS(this).collect(Collectors.toList());
	}

	/**
	 * 获取节点路径的编码
	 *
	 * @param start 开始位置
	 * @return 节点路径的编号
	 */
	public String getPathCode(int start) {
		return this.getPathNodeS().map(e -> String.valueOf(e.getIndex() + start)).collect(Collectors.joining("."));
	}

	/**
	 * 获取节点路径的编码：路径编码从1开始
	 *
	 * @return 节点路径的编号
	 */
	public String getPathCode() {
		return getPathCode(1);
	}

	/**
	 * 获得父节点路径 如果父节点为null返回空
	 *
	 * @return 获取父节点的路径
	 */
	public String getParentPath() {
		if (this.getParent() == null)
			return null;
		return this.getParent().getPath();
	}

	/**
	 * 获取父节点 节点路径
	 *
	 * @return 节点路径
	 */
	public List<Node<T>> getParentPathNodes() {
		return Node.getPathNodes(this.getParent());
	}

	/**
	 * 获取父节点 节点路径
	 *
	 * @return 节点路径
	 */
	public Stream<Node<T>> getParentPathNodeS() {
		return Node.getPathNodeS(this.getParent());
	}

	/**
	 * 获取节点路径
	 *
	 * @param node 节点
	 * @return 节点路径
	 */
	public String getPath(final Node<T> node) {
		return Node.getPath(node, null);
	}

	/**
	 * 根据节点路径查询节点
	 * 
	 * 节点结构 a/b/c <br>
	 * node_a.getNode([a,b,c]) 返回 node_c <br>
	 * 
	 * @param path 节点路径包括当前节点名称 node_a.getNode([a,b,c]) 返回node_c
	 * @return 根据节点路径查询节点信息
	 */
	public Optional<Node<T>> getNodeOpt(final String path) {
		return Optional.ofNullable(this.getNode(path));
	}

	/**
	 * 根据节点路径查询节点
	 * 
	 * 节点结构 a/b/c <br>
	 * node_a.getNode([a,b,c]) 返回 node_c <br>
	 * 
	 * @param path 节点路径包括当前节点名称 node_a.getNode([a,b,c]) 返回node_c
	 * @return 根据节点路径查询节点信息
	 */
	public Node<T> getNode(final String path) {
		return Node.getNode(this, path);
	}

	/**
	 * 根据节点路径查询节点
	 * 
	 * 节点结构 a/b/c <br>
	 * node_a.getNode([a,b,c]) 返回 node_c <br>
	 * 
	 * @param path 节点路径包括当前节点名称 node_a.getNode([a,b,c]) 返回node_c
	 * @return 根据节点路径查询节点信息
	 */
	public Node<T> getNode2(final String path) {
		return this.getNode2(path, e -> e.getName());
	}

	/**
	 * 根据节点路径查询节点
	 * 
	 * 节点结构 a/b/c <br>
	 * node_a.getNode([a,b,c]) 返回 node_c <br>
	 * 
	 * @param path   节点路径包括当前节点名称 node_a.getNode([a,b,c]) 返回node_c
	 * @param nameof 节点命名函数
	 * @return 根据节点路径查询节点信息
	 */
	public Node<T> getNode2(final String path, final Function<Node<T>, String> nameof) {
		return this.getNode2(path.split("[/;,]+"), nameof);
	}

	/**
	 * 根据节点路径查询节点
	 * 
	 * 节点结构 a/b/c <br>
	 * node_a.getNode([a,b,c]) 返回 node_c <br>
	 * 
	 * @param path   节点路径包括当前节点名称 node_a.getNode([a,b,c]) 返回node_c
	 * @param nameof 节点命名函数 node->name
	 * @return 根据节点路径查询节点信息
	 */
	public Node<T> getNode2(final String[] path, final Function<Node<T>, String> nameof) {
		final String name = path[0];
		final boolean b = Optional.of(nameof.apply(this)).map(name::equals).orElse(false);
		if (b) {
			if (path.length > 1) {
				final Node<T> node = this.getChild(p -> nameof.apply(p).equals(path[1]));
				if (node != null) {
					final String[] _path = Arrays.copyOfRange(path, 1, path.length);
					return node.getNode2(_path, nameof);
				} else {
					return null;
				}
			} else {
				return this;
			}
		} else {
			return null;
		}
	}

	/**
	 * 提取节点集合 flatNodes的别名 (包含当前节点即第一个节点）
	 * 
	 * @param path 节点路径
	 * @return 节点集合
	 */
	public Stream<Node<T>> getNodeS(final String path) {
		return Node.getNodeS(Stream.of(this), path);
	}

	/**
	 * 提取节点集合 flatNodes的别名 (包含当前节点即第一个节点）
	 * 
	 * @return 节点集合
	 */
	public Stream<Node<T>> getNodeS() {
		return this.flatNodeS();
	}

	/**
	 * 提取节点集合 flatNodes的别名(包含当前节点即第一个节点）
	 * 
	 * @return 节点集合
	 */
	public List<Node<T>> getNodes() {
		return this.getNodeS().collect(Collectors.toList());
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法
	 */
	public Optional<IRecord> ropt() {
		return Optional.ofNullable(this.value instanceof IRecord ? (IRecord) this.value : IRecord.REC(this.value));
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 Object key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public Object rget(final String key) {
		return this.ropt().map(e -> e.get(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public String rstr(final String key) {
		return this.ropt().map(e -> e.str(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 Number key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public Number rnum(final String key) {
		return this.ropt().map(e -> e.dbl(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 Integer key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public Integer ri4(final String key) {
		return this.ropt().map(e -> e.i4(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 Double key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public Double rdbl(String key) {
		return this.ropt().map(e -> e.dbl(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 Timestamp key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public Timestamp rtimestamp(final String key) {
		return this.ropt().map(e -> e.timestamp(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 Date key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public Date rdate(final String key) {
		return this.ropt().map(e -> e.date(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 LocalDate key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public LocalDate rld(final String key) {
		return this.ropt().map(e -> e.ld(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public LocalTime rlt(final String key) {
		return this.ropt().map(e -> e.lt(key)).orElse(null);
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 * 
	 * @param key 健名
	 * @return 健名值
	 */
	public LocalDateTime rldt(final String key) {
		return this.ropt().map(e -> e.ldt(key)).orElse(null);
	}

	/**
	 * 获取节点相对于父节点的子节点索引号<br>
	 * 索引序号从0开始,根节点的Index 序号为0
	 *
	 * @return 节点的索引号
	 */
	public int getIndex() {
		if (this.isRoot()) {
			return 0;
		}
		final Node<T> parent = this.getParent();
		return parent.getChildIndex(this);
	}

	/**
	 * getChildren别名 <br>
	 * 获取所有的子节点信息记录
	 *
	 * @return 所有子节点包括空节点
	 */
	public LinkedList<Node<T>> childrenL() {
		return this.getChildren();
	}

	/**
	 * getChildren别名 <br>
	 * 获取所有的子节点信息记录
	 *
	 * @return 所有子节点包括空节点
	 */
	public Stream<Node<T>> childrenS() {
		return this.getChildren().stream();
	}

	/**
	 * 获取所有的子节点信息记录
	 *
	 * @return 所有子节点包括空节点
	 */
	public LinkedList<Node<T>> getChildren() {
		return children;
	}

	/**
	 * 提取满足条件的节点
	 *
	 * @param predicate 节点条件过滤器
	 * @return 满足predicate 过滤条件的节点
	 */
	public LinkedList<Node<T>> getChildren(final Predicate<Node<T>> predicate) {
		return children.stream().filter(predicate).collect(Collector.of(() -> new LinkedList<Node<T>>(), (aa, a) -> {
			aa.add(a);
		}, (aa, bb) -> {
			aa.addAll(bb);
			return aa;
		}, e -> e));
	}

	/**
	 * 获取子节点的索引序号
	 *
	 * @param cmp 节点的比较器
	 * @return 获取子节点的索引序号 从开始,查询不到返回 -1
	 */
	public Integer getChildIndex(final Predicate<Node<T>> cmp) {
		for (int i = 0; i < this.size(); i++) {
			if (cmp.test(this.getChild(i)))
				return i;
		}
		return -1;
	}

	/**
	 * 获取子节点的索引序号
	 *
	 * @param node 子节点 对象
	 * @return 获取子节点的索引序号 从0开始
	 */
	public Integer getChildIndex(final Node<T> node) {
		return this.getChildIndex(n -> n == node);
	}

	/**
	 * 提取第i个索引位置的节点
	 *
	 * @param i 子节点索引 从0开始,当节点索引i超过节点数量（包含）时候，返回null
	 * @return 第i个子节点
	 */
	public Node<T> getChild(final int i) {
		return children.size() > i ? children.get(i) : null;
	}

	/**
	 * 提取第i个索引位置的节点
	 *
	 * @param i 子节点索引 从0开始,当节点索引i超过节点数量（包含）时候，返回null
	 * @return 第i个子节点
	 */
	public Optional<Node<T>> getChildOpt(final int i) {
		return Optional.ofNullable(this.getChild(i));
	}

	/**
	 * 提取子节点
	 *
	 * @param c   孩子节点
	 * @param cmp T 类型的比较器
	 * @return 是否含有指定的节点数据
	 */
	public Optional<Node<T>> getChildOpt(final T t, final Comparator<T> cmp) {
		return this.childrenS().filter(e -> cmp.compare(e.getValue(), t) == 0).findFirst();
	}

	/**
	 * 提取 T t
	 *
	 * @param t 子节点参考值
	 * @return 是否含有指定的节点数据
	 */
	public Optional<Node<T>> getChildOpt(final T t) {
		@SuppressWarnings("unchecked")
		final Predicate<T> test = p -> {
			var flag = false;
			try { // 强制转换成可比较类型
				flag = Comparator.comparing(e -> (Comparable<Object>) e).compare(p, t) == 0;
			} catch (Exception e) { // 类型转换失败
				flag = (p == t); // 进行引用比较
				// do nothing
			}
			return flag;
		};
		return this.childrenS().filter(e -> test.test(e.getValue())).findFirst();
	}

	/**
	 * 提取第i个索引位置的节点
	 *
	 * @param p 节点判定
	 * @return 第i个子节点
	 */
	public Node<T> getChild(final Predicate<Node<T>> p) {
		return this.children.stream().filter(p).findAny().orElse(null);
	}

	/**
	 * 提取 t 值子节点
	 *
	 * @param t 子节点参考值
	 * @return 是否含有指定的节点数据
	 */
	public Optional<Node<T>> childOpt(final T t) {
		return this.getChildOpt(t);
	}

	/**
	 * 提取 t 值子节点
	 *
	 * @param t 子节点参考值
	 * @return 是否含有指定的节点数据
	 */
	public Node<T> child(final T t) {
		return this.getChildOpt(t).orElse(null);
	}

	/**
	 * 添加子节点,自动设置子节点的父节点,做检测c的存在性检测
	 *
	 * @param cc 子节点,null 节点不添加
	 */
	public Node<T> addChildren(final Iterable<Node<T>> cc) {
		synchronized (cc) {
			for (final Node<T> c : cc) {
				this.addChild(c);
			}
		}
		return this;
	}

	/**
	 * 添加子节点,自动设置子节点的父节点,做检测c的存在性检测
	 *
	 * @param c 子节点,null 节点不添加
	 */
	public Node<T> addChild(final Node<T> c) {
		return this.addChild(c, false, true);
	}

	/**
	 * 把另一个node的子节点合并入自己的子节点
	 *
	 * @param node 另一个节点,当node为this或是null时候，直接返回this
	 * @return 当前对象本身
	 */
	public Node<T> merge(final Node<T> node) {
		if (node == null || node == this) {
			return this;
		}
		return this.addChildren(node.getChildren());
	}

	/**
	 * 添加子节点,自动设置子节点的父节点
	 *
	 * @param c           子节点
	 * @param b           是否添加空节点,true 添加空节点,false 空节点不添加
	 * @param checkExists 是否做子节点的存在性检测 true 检测(同一节点可以添加多次),false 不检测
	 */
	public Node<T> addChild(final Node<T> c, final boolean b, final boolean checkExists) {
		synchronized (this.children) {
			if (c != null) {
				if (checkExists && this.hasChild(c)) {// 存在性检测
					// System.out.println("节点"+c+",已存在");
					return this;
				} // if 存在性检测
				this.children.add(c);
				c.setParent(this);
			} else {
				if (b) {
					this.children.add(null);
				}
			} // if
		}
		return this;
	}

	/**
	 * 添加子节点,自动设置子节点的父节点,做检测c的存在性检测
	 *
	 * @param i index at which the specified element is to be inserted 从0开始
	 * @param c 子节点,null 节点不添加
	 */
	public Node<T> addChild(final int i, final Node<T> c) {
		return this.addChild(i, c, false, true);
	}

	/**
	 * 添加子节点,自动设置子节点的父节点
	 *
	 * @param i           index at which the specified element is to be inserted
	 *                    从0开始
	 * @param c           子节点
	 * @param b           是否添加空节点,true 添加空节点,false 空节点不添加
	 * @param checkExists 是否做子节点的存在性检测 true 检测(同一节点可以添加多次),false 不检测
	 */
	public Node<T> addChild(final int i, final Node<T> c, final boolean b, final boolean checkExists) {
		if (c != null) {
			if (checkExists && this.hasChild(c)) {// 存在性检测
				// System.out.println("节点"+c+",已存在");
				return this;
			} // if 存在性检测
			this.children.add(i, c);
			c.setParent(this);
		} else {
			if (b)
				this.children.add(i, null);
		}
		return this;
	}

	/**
	 * 删除子节点
	 * 
	 * @param child 被移除的子节点
	 * 
	 * @return 节点对象this本身，以便于链式编程
	 */
	public Node<T> removeChild(final Node<T> child) {
		this.children.remove(child);
		return this;
	}

	/**
	 * 删除子节点
	 * 
	 * @param index the index of the element to be removed
	 * @return 节点对象this本身，以便于链式编程
	 */
	public Node<T> removeChild(final int index) {
		this.children.remove(index);
		return this;
	}

	/**
	 * 是否包含由孩子节点
	 *
	 * @param c 孩子节点
	 * @return 是否含有指定的节点数据
	 */
	public boolean hasChild(final Node<T> c) {
		return this.getChildren().contains(c);
	}

	/**
	 * 是否包含由孩子节点
	 *
	 * @param c   孩子节点
	 * @param cmp T 类型的比较器
	 * @return 是否含有指定的节点数据
	 */
	public boolean hasChild(final T t, final Comparator<T> cmp) {
		return this.childrenS().filter(e -> cmp.compare(e.getValue(), t) == 0).findFirst().isPresent();
	}

	/**
	 * 是否包含由孩子节点
	 *
	 * @param c 孩子节点
	 * @return 是否含有指定的节点数据
	 */
	public boolean hasChild(final String name) {
		return this.getNodeOpt(name).isPresent();
	}

	/**
	 * 是否包含由孩子节点
	 *
	 * @param cc 孩子节点列表
	 * @return true 含有子节点
	 */
	public boolean hasChildren(final List<Node<T>> cc) {
		return cc.stream().mapToInt(e -> this.hasChild(e) ? 1 : 0).sum() == cc.size();
	}

	/**
	 * 添加子节点
	 *
	 * @param cc 子节点集合，叫注意cc中包含有this父节点或是this子节点，addChildren仅仅添加不做树形结构完整性检测：有意为之除外。
	 * @return 当前对象本身
	 */
	public Node<T> addChildren(final List<Node<T>> cc) {
		synchronized (this.children) {
			this.children.addAll(cc);
			cc.forEach(c -> c.setParent(this));// 设置父节点
		}
		return this;
	}

	/**
	 * 注意这里是通过addChildren 完成父子关系的构建。设置父节点 并不保证父节点的子节点中包含有 本节点(this)。所以可以
	 * setParent(null) 这样就在逻辑上构建了一个独立的树（根节点没有父节点，每颗树只有一个父节点）
	 * <p>
	 * 设置父节点:不会为parent添加this作为子节点
	 *
	 * @param parent 父节点对象，可以为null,这样就是表明他是一颗树的根
	 */
	public void setParent(final Node<T> parent) {
		this.parent = parent;
	}

	/**
	 * 把 设置i个孩子节点为p节点，如果p为非空会自动将 p的父节点设置为 this
	 *
	 * @param i 子节点索引号,从0开始,i 索引号需要小于 子节点的长度。
	 * @param p 待设置的节点
	 */
	public void setChild(final int i, final Node<T> p) {
		final int n = this.size();
		if (i < n) {
			this.children.set(i, p);// 设置孩子节点
			if (p != null)
				p.setParent(this);// 设置父亲节点
		} // if
	}

	/**
	 * 没有子节点或是子节点为null的节点的高度为1<br>
	 * 返回节点的高度 ，没有孩子的节点高度为1,否个节点的高度为最高的孩子节点的高度+1
	 *
	 * @return 节点的高度，从1开始。
	 */
	public int height() {
		final List<Node<T>> children = this.getChildren(Objects::nonNull);
		if (children.size() == 0)
			return 1;
		return children.parallelStream().map(Node::height).collect(Collectors.summarizingInt(e -> e)).getMax() + 1;
	}

	/**
	 * 判断当前节点是否为根节点
	 *
	 * @return true 根节点，false 非根节点
	 */
	public boolean isRoot() {
		return this.getParent() == null;
	}

	/**
	 * 是否是叶子节点
	 *
	 * @return 是否为叶子节点
	 */
	public boolean isLeaf() {
		return this.children.size() <= 0;
	}

	/**
	 * 获取节点所在的层级(空节点层级为0)
	 *
	 * @return 节点的层级, 根节点为1层, 根节点的子节点层级为2, 以此类推, 即 子节点的层级为父节点的层级+1
	 */
	public Integer getLevel() {
		return this.getLevel(this);
	}

	/**
	 * getLevel()-1 的 简化版本<br>
	 * 获取节点所在的层级(空节点层级为0) <br>
	 *
	 * @return 节点的层级, 根节点为0层, 根节点的子节点层级为1, 以此类推, 即 子节点的层级为父节点的层级+1
	 */
	public Integer level() {
		return this.getLevel() - 1;
	}

	/**
	 * 获取指定节点所在的层级(空节点层级为0) 节点的层级,根节点为1层,根节点的子节点层级为2,以此类推, 即 子节点的层级为父节点的层级+1
	 *
	 * @param node 节点数据
	 * @return 节点的层级, 根节点为1层, 根节点的子节点层级为2, 以此类推, 即 子节点的层级为父节点的层级+1
	 */
	public Integer getLevel(final Node<T> node) {
		if (node == null)
			return 0;
		return this.getLevel(node.getParent()) + 1;
	}

	/**
	 * 提取属性集合，只读 设置为无效。
	 *
	 * @return IRecord 属性集合
	 */
	public IRecord attributes() {
		return this.attributes;
	}

	/**
	 * 提取属性值并制定属性值的类型，只读 设置为无效。
	 *
	 * @param name 属性名称
	 * @return U 类型的属性值
	 */
	@SuppressWarnings("unchecked")
	public <U> U attr(final String name) {
		return (U) this.attributes().get(name);
	}

	/**
	 * 提取属性值并制定属性值的类型，只读 设置为无效。
	 *
	 * @param <U>          返回的结果类型
	 * @param name         属性名称
	 * @param defaultValue 返回结果的类型类
	 * @return U 类型的属性值
	 */
	@SuppressWarnings("unchecked")
	public <U> U attr(final String name, final U defaultValue) {
		return (U) this.attrOpt(name).orElse(defaultValue);
	}

	/**
	 * 提取属性值并制定属性值的类型，只读 设置为无效。
	 *
	 * @param <U>  返回的结果类型
	 * @param name 属性名称
	 * @return Optional U 类型的属性值
	 */
	@SuppressWarnings("unchecked")
	public <U> Optional<U> attrOpt(final String name) {
		return this.attributes().opt(name).map(e -> (U) e);
	}

	/**
	 * 设置对象属性
	 *
	 * @param name  属性名称
	 * @param value 数据值
	 * @return 节点对象本身,用于实现链式编程
	 */
	public Node<T> setAttr(final String name, Object value) {
		this.attributes().set(name, value);
		return this;
	}

	/**
	 * 设置对象属性
	 *
	 * @param attributes 属性名,数值型的 序列[{key,value}]
	 * @return 节点对象本身,用于实现链式编程
	 */
	public Node<T> setAttrs(final IRecord attributes) {
		this.attributes.add(attributes);
		return this;
	}

	/**
	 * 设置节点属性
	 * 
	 * @param name  属性名
	 * @param value 属性值
	 * @return 节点本身 以实现链式编程
	 */
	public Node<T> attrSet(final String name, final Object value) {
		return this.setAttr(name, value);
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
		return Optional.ofNullable(this.attributes.get("value"));
	}

	/**
	 * this.getAttribute("value") 的简写
	 * 
	 * @return 属性key为value的值，注意不是 节点value
	 */
	public Object attrval() {
		return this.attributes.get("value");
	}

	/**
	 * this.attrSet("value", value) 的简写
	 * 
	 * @param value 值对象
	 * @return Node 对象本身
	 */
	public Node<T> attrval(final Object value) {
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
		return mapper.apply((U) this.attributes.get(key));
	}

	/**
	 * 路径节点 <br>
	 * 根据路径获取子节点,路径采用"/"进行分割 对于路径 A/B/C, 以节点 A为例 nodeA.path("B/C") 返回节点C <br>
	 * 
	 * @param path 相对于当前节点的路径，
	 * 
	 * @return 根节点
	 */
	public Node<T> get(final String path) {
		return Node.getNode(this, path);
	}

	/**
	 * 以该节点为起始节点，进行树形结构遍历
	 *
	 * @param cs 节点长处理函数
	 */
	public void forEach(final Consumer<Node<T>> cs) {
		forEach(cs, this);
	}

	/**
	 * 返回所有的 叶子节点
	 *
	 * @return 叶子节点列表
	 */
	public List<Node<T>> getAllLeaves() {
		return this.getAllLeaveS().collect(Collectors.toList());
	}

	/**
	 * 返回所有的 叶子节点
	 *
	 * @return 叶子节点流
	 */
	public Stream<Node<T>> getAllLeaveS() {
		return this.flatNodeS().filter(Node::isLeaf);
	}

	/**
	 * 树形结构的节点。扁平化成序列列表 以该节点为起始节点，进行树形结构遍历
	 * <p>
	 * 示例： MAP(root.flatMap(),f-&gt;f.val(g-&gt;g.str("name")));//
	 * 生成一个树形结构的额各个节点名称序列。 这里是一个Node&lt;IRecord&gt; 结构，并且IRecord中包含了name字段
	 * 
	 * @return 节点流程
	 */
	public synchronized List<Node<T>> flatNodes() {
		return this.flatMaps(e -> e);
	}

	/**
	 * 树形结构的节点。扁平化成序列列表 以该节点为起始节点，进行树形结构遍历
	 * <p>
	 * 示例： MAP(root.flatMap(),f-&gt;f.val(g-&gt;g.str("name")));//
	 * 生成一个树形结构的额各个节点名称序列。 这里是一个Node&lt;IRecord&gt; 结构，并且IRecord中包含了name字段
	 * 
	 * @return 节点流程
	 */
	public synchronized Stream<Node<T>> flatNodeS() {
		return Node.flatMapS(this);
	}

	/**
	 * 以该节点为起始节点，进行树形结构遍历
	 * 
	 * @param <U>    列表元素类型
	 * @param mapper 节点的处理函数 把 &lt;T&gt; 类型转换成 &lt;U&gt; 类型。
	 */
	public synchronized <U> List<U> flatMaps(final Function<Node<T>, U> mapper) {
		if (mapper == null) {
			return null;
		}

		return this.flatNodeS().map(mapper).collect(Collectors.toList());
	}

	/**
	 * 以该节点为起始节点，进行树形结构遍历
	 * 
	 * @param <U>    列表元素类型
	 * @param mapper 节点的处理函数 把 &lt;T&gt; 类型转换成 &lt;U&gt; 类型。
	 */
	public synchronized <U> Stream<U> flatMap(final Function<Node<T>, U> mapper) {
		if (mapper == null) {
			return null;
		}

		return this.flatNodeS().map(mapper);
	}

	/**
	 * 树形结构的节点。扁平化成序列列表 以该节点为起始节点，进行树形结构遍历
	 * <p>
	 * 示例： MAP(root.flatMap(),f-&gt;f.val(g-&gt;g.str("name")));//
	 * 生成一个树形结构的额各个节点名称序列。 这里是一个Node&lt;IRecord&gt; 结构，并且IRecord中包含了name字段
	 */
	public synchronized Stream<Node<T>> flatStream() {
		return this.flatStream(e -> e);
	}

	/**
	 * 以该节点为起始节点，进行树形结构遍历
	 * 
	 * @param <U>    流元素类型
	 * @param mapper 节点的处理函数 把T类型转换成U类型。
	 */
	public synchronized <U> Stream<U> flatStream(final Function<Node<T>, U> mapper) {
		return this.flatNodeS().map(mapper);
	}

	/**
	 * 树形结构的节点。扁平化成序列列表 以该节点为起始节点，进行树形结构遍历
	 * <p>
	 * 示例： MAP(root.flatMap(),f-&gt;f.val(g-&gt;g.str("name")));//
	 * 生成一个树形结构的额各个节点名称序列。 这里是一个Node&lt;IRecord&gt; 结构，并且IRecord中包含了name字段
	 */
	public synchronized Stream<Node<T>> flatS() {
		return this.flatStream();
	}

	/**
	 * 遍历 node
	 * 
	 * @param preprocess  前处理
	 * @param process     处理
	 * @param postprocess 后处理
	 */
	public void traverse(final Consumer<Node<T>> preprocess, final Consumer<Node<T>> process,
			final Consumer<Node<T>> postprocess) {
		Node.traverse(this, preprocess, process, postprocess);
	}

	/**
	 * 直接子节点数目:非空节点的数目
	 *
	 * @return 返回直接子节点数目
	 */
	public int size() {
		return this.size(true);
	}

	/**
	 * 直接子节点数目
	 *
	 * @param b 是否包含空节点:true 包含,false 不包含
	 * @return 返回直接子节点数目
	 */
	public int size(final boolean b) {
		return b ? this.getChildren().size() : this.getChildren(Objects::nonNull).size();
	}

	/**
	 * 
	 */
	@Override
	public String json(final BiFunction<StringBuilder, Node<T>, String> pre_processor,
			final BiFunction<StringBuilder, Node<T>, String> post_processor) {
		return INodeWriter.writeJson(this, p -> p.childrenL(), pre_processor, post_processor);
	}

	/**
	 * 使用产品的名字进行节点散列：采用名称name 进行散开列。
	 *
	 * @return hashCode
	 */
	@Override
	public int hashCode() {
		// 之所以不采用路径的hashcode 的是为了提高效率，hashCode 的效率。
		return this.getName().hashCode();// 根据名称
	}

	/**
	 * 依据名称来进行判断对象属性
	 *
	 * @param obj 比较对象
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this.hashCode() != obj.hashCode())
			return false;
		if (obj instanceof Node) {// 类型相互一致才进行比较
			@SuppressWarnings("unchecked")
			final Node<T> node = (Node<T>) obj;
			final Node<T> p0 = this.getParent();
			final Node<T> p1 = node.getParent();
			if (p0 == null && p1 != null)
				return false;
			else if (p0 == p1 || p0.equals(p1)) {// 先对父级路径进行比较
				final T v0 = this.getValue();
				final T v1 = node.getValue();
				if (v0 == v1)
					return true;
				else
					return v0 != null && v0.equals(v1);
			} else {
				return false;
			} // if
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return value + "";
	}

	/**
	 * 获取节点路径(过滤掉空白节点名）
	 *
	 * @param <T>         节点中的元素的类型
	 * @param node        节点
	 * @param name_mapper 节点名称变换器 : node->String
	 * @return 节点路径
	 */
	public static <T> String getPath(final Node<T> node, final Function<Node<T>, String> name_mapper) {
		if (node == null)
			return "";
		final Function<Node<T>, String> final_name_mapper = name_mapper == null ? Node::getName : name_mapper;
		String ppath = Node.getPath(node.getParent(), final_name_mapper);
		if (!ppath.matches("[\\s]*")) {
			ppath += "/";
		}
		return ppath + final_name_mapper.apply(node);
	}

	/**
	 * 获取节点路径
	 *
	 * @param <X>节点的值的类型
	 * @param node       节点对象
	 * @return 节点路径
	 */
	public static <X> Stream<Node<X>> getPathNodeS(final Node<X> node) {
		if (node == null) {
			return Stream.of();
		} else {
			return Stream.concat(Node.getPathNodeS(node.getParent()), Stream.of(node));
		}
	}

	/**
	 * 获取节点路径
	 *
	 * @param <X>节点的值的类型
	 * @param node       节点对象
	 * @return 节点路径
	 */
	public static <X> List<Node<X>> getPathNodes(final Node<X> node) {
		return getPathNodeS(node).collect(Collectors.toList());
	}

	/**
	 * 节点遍历
	 *
	 * @param <U>  节点中的元素的类型
	 * @param cs   节点长处理函数
	 * @param node 遍历的节点起始节点
	 */
	public static <U> void forEach(final Consumer<Node<U>> cs, Node<U> node) {
		if (node == null || cs == null)
			return;
		cs.accept(node);
		for (final Node<U> u : reverse(node.getChildren()))
			forEach(cs, u);
	}

	/**
	 * 节点遍历
	 *
	 * @param <U>  节点中的元素的类型
	 * @param node 遍历的节点起始节点
	 */
	public static <U> Stream<Node<U>> flatMapS(final Node<U> node) {
		if (node == null) {
			return Stream.of();
		}

		final Stack<Node<U>> stack = new Stack<>();
		final AtomicInteger ar = new AtomicInteger(0);
		stack.push(node);
		return Stream.iterate(node, e -> ar.get() > -1, e -> { //
			if (!stack.isEmpty()) {
				final Node<U> p = stack.pop();
				final ListIterator<Node<U>> itr = p.getChildren().listIterator(p.size());
				while (itr.hasPrevious()) {
					final Node<U> n = itr.previous();
					stack.push(n);
				} // while
				return p;
			} else { // 保持最后元素得以输出
				ar.decrementAndGet();
				return e;
			} // if
		}).skip(1);
	}

	/**
	 * 寻找一个
	 * 
	 * @param <T> 元素类型
	 * @param itr 可遍历数据
	 * @param p   过滤条件
	 * @return 可选值
	 */
	public static <T> T findOne(final Iterable<T> itr, final Predicate<T> p) {
		return Node.findOneOpt(itr, p).orElse(null);
	}

	/**
	 * 寻找一个
	 * 
	 * @param <T> 元素类型
	 * @param itr 可遍历数据
	 * @param p   过滤条件
	 * @return 可选值
	 */
	public static <T> Optional<T> findOneOpt(final Iterable<T> itr, final Predicate<T> p) {
		return StreamSupport.stream(itr.spliterator(), false).filter(p).findFirst();
	}

	/**
	 * 提取所有
	 * 
	 * @param <T> 元素类型
	 * @param itr 可遍历数据
	 * @param p   过滤条件
	 * @return 列表数据
	 */
	public static <T> List<T> findAll(final Iterable<T> itr, final Predicate<T> p) {
		return Node.findAllS(itr, p).collect(Collectors.toList());
	}

	/**
	 * 提取所有
	 * 
	 * @param <T> 元素类型
	 * @param itr 可遍历数据
	 * @param p   过滤条件
	 * @return 列表数据
	 */
	public static <T> Stream<T> findAllS(final Iterable<T> itr, final Predicate<T> p) {
		return StreamSupport.stream(itr.spliterator(), false).filter(p);
	}

	/**
	 * 寻找一个
	 * 
	 * @param <T>       元素类型
	 * @param predicate 过滤条件
	 * @return 是否
	 */
	public static <T> Function<List<T>, T> get_one(final Predicate<T> predicate) {
		return nodes -> Node.findOneOpt(nodes, predicate).orElse(null);
	}

	/**
	 * 
	 * @param <T>       元素类型
	 * @param predicate (parent,e)->boolean
	 * @return 是否具有父子关系
	 */
	public static <T> BiFunction<List<T>, T, List<T>> get_children(final BiPredicate<T, T> predicate) {
		return (ll, parent) -> ll.stream().filter(node -> predicate.test(parent, node)).collect(Collectors.toList());
	}

	/**
	 * 根据路径获取子节点,路径采用"/"进行分割 对于路径 A/B/C, 以节点 A为例 nodeA.path("B/C") 返回节点C
	 *
	 * @param <T>  节点值的类型
	 * @param node 相对于当前节点的路径，
	 * @param path 元素路径
	 * @return Node 结构
	 */
	public static <T> Node<T> getNode(final Node<T> node, final String path) {
		if (node == null) {
			return null;
		}

		final int p = path.indexOf("/");// 产品路径 backslash 位置 pos
		final String name = p < 0 ? path : path.substring(0, p);// 获取一级目录项目
		final String rest = p < path.length() ? path.substring(p + 1) : null;
		if (name.equals(node.getName())) {
			return node;
		} else {
			final Optional<Node<T>> c = node.children.stream() //
					.filter(e -> Objects.equals(e.getName(), name)).findFirst();
			if (c.isPresent() && rest != null)
				return Node.getNode(c.get(), rest);// 从子节点重继续寻找
		}

		return null;
	}

	/**
	 * 根据路径获取子节点,路径采用"/"进行分割 对于路径 A/B/C, 以节点 A为例 nodeA.path("B/C") 返回节点C
	 *
	 * @param <T>  节点中的元素的类型
	 * @param path 元素路径
	 * @return Node 结构
	 */
	public static <T> Stream<Node<T>> getNodeS(final Stream<Node<T>> nodes, final String path) {

		final Function<Node<T>, Stream<Node<T>>> mapper = node -> {
			if (node == null) {
				return null;
			}

			final int p = path.indexOf("/");// 产品路径 backslash 位置 pos
			final String name = p < 0 ? path : path.substring(0, p);// 获取一级目录项目
			final String rest = p < path.length() ? path.substring(p + 1) : null;
			if (name.equals(node.getName())) { // 节点直接匹配
				return Stream.of(node);
			} else { // 尝试进行子节点匹配
				final Stream<Node<T>> _nodeS = node.children.stream().filter(e -> e.getName().equals(name));
				if (rest != null)
					return Node.getNodeS(_nodeS, rest);// 从子节点重继续寻找
				else
					return Stream.of();
			} // if
		};

		return nodes.flatMap(mapper);
	}

	/**
	 * 遍历 node
	 * 
	 * @param <T>         节点中的元素的类型
	 * @param node        节点
	 * @param preprocess  前处理
	 * @param process     处理
	 * @param postprocess 后处理
	 */
	public static <T> void traverse(final Node<T> node, final Consumer<Node<T>> preprocess,
			final Consumer<Node<T>> process, final Consumer<Node<T>> postprocess) {
		preprocess.accept(node);
		process.accept(node);
		reverse(node.getChildren()).forEach(e -> traverse(e, preprocess, process, postprocess));
		postprocess.accept(node);
	}

	/**
	 * 
	 * @param rec Map结构的树形结构 ,嵌套型树形结构,{key:{key2:{key3:value3}}}
	 * @return rootNode 根节点
	 */
	public static Node<Object> buildTree(final Map<?, ?> rec) {
		return buildTree(rec, null);
	}

	/**
	 * 构建树形结构
	 * 
	 * @param rec      Map结构的树形结构 ,嵌套型树形结构,{key:{key2:{key3:value3}}}
	 * @param rootNode 根节点
	 * @return rootNode 根节点
	 */
	public static Node<Object> buildTree(final Map<?, ?> rec, final Node<Object> rootNode) {
		final Node<Object> _rootNode = rootNode != null ? rootNode : Node.of(null); // 默认根节点
		rec.forEach((key, value) -> {
			Node<Object> node = null;
			if (value instanceof Map) {
				node = Node.of(key);
				buildTree((Map<?, ?>) value, node);
			} else {
				node = new Node<Object>(value, e -> key + "");
			} // if
			_rootNode.addChild(node);
		}); // forEach
		return _rootNode;
	}

	/**
	 * Reverses the order of the elements in the specified list.
	 * 
	 * @param <T>  元素类型
	 * @param list 列表数据
	 * @return 首位调转后的列表
	 */
	public static <T> List<T> reverse(final List<T> list) {
		final var _list = new ArrayList<>(list); // 构造一个复制品
		Collections.reverse(list);
		return _list;
	}

	/**
	 * 节点对象生成器
	 * 
	 * @param <T> 元素类型
	 * @param t   元素
	 * @return 元素节点
	 */
	public static <T> Node<T> of(final T t) {
		return new Node<T>(t);
	}

	/**
	 * 节点对象生成器
	 * 
	 * 可以用于树形结构的对象的根节点生成:
	 * 
	 * <pre>
	 * {@code
	 * final var rootNode = REC(pivotLines).tupleS().parallel().reduce(Node.of("root"), // 以REC形式分解成阶层元素(K,V)流,而后reduce成树形结构
	 * 		ndaccum((leaf, p) -> leaf.attrSet("value", p._2), Node::of), Node::merge); // 数据透视分阶层统计
	 * }</pre>
	 * 
	 * @param <T>    元素类型
	 * @param parent 父节点
	 * @param t      元素
	 * @return 元素节点
	 */
	public static <T> Node<T> of(final Node<T> parent, final T t) {
		return new Node<>(parent, t);
	}

	private T value;// 节点的信息值
	private Node<T> parent = null;// 父节点，初始化为null,表示他是一个根节点，没有父节点
	private Function<Node<T>, String> lambda_name = null;// 获得节点的名称
	private LinkedList<Node<T>> children = new LinkedList<>();// 子节点：集合
	private IRecord attributes = IRecord.REC();// 属性记录的存储空间

}
