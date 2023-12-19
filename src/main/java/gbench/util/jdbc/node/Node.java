package gbench.util.jdbc.node;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import gbench.util.jdbc.kvp.IRecord;

import java.util.stream.Collectors;

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
 * 的唯一标识申城器 <br>
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
 * @author gbench
 *
 * @param <T> 节点中的元素的类型
 */
public class Node<T> {
	/**
	 * 拷贝钩爪函数
	 * 
	 * @param node
	 */
	public Node(final Node<T> node) {
		this.value = node.value;
		this.properties = node.properties;
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
	 * 克隆函数
	 */
	@Override
	public Node<T> clone() {
		try {
			@SuppressWarnings({ "unchecked", "unused" })
			Node<T> tNode = (Node<T>) super.clone(); // 之所以写这一行 是为了 去除idea的编译警告
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
	 * 设置lambda_name的选项
	 * 
	 * @param lambda_name 名称函数的生成表达式。
	 * @return 节点对象本身 用于实现链式编程
	 */
	public Node<T> setLambdaName(final Function<Node<T>, String> lambda_name) {
		this.lambda_name = lambda_name;
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
			if (value instanceof IRecord) {
				final IRecord rec = ((IRecord) value);
				Object name = null;// name 字段的值
				if (nameflds != null)
					for (final var fld : nameflds) {// 尝试提取默认的名字字段，
						if ((name = rec.get(fld)) != null)
							break;
					} // for

				if (name != null)
					return name + "";
				else
					return value + "";
			} else {
				return value + "";
			} // if value
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
		final var nameflds = new String[] { "name", "path", "Activity" };
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
		return this.getPath(this, name_mapper);
	}

	/**
	 * 获取节点路径
	 * 
	 * @return 节点路径
	 */
	public Stream<Node<T>> getPathNodes() {
		return Node.getPathNodes(this);
	}

	/**
	 * 获取节点路径的编码
	 * 
	 * @param start 开始位置
	 * @return 节点路径的编号
	 */
	public String getPathCode(int start) {
		return this.getPathNodes().map(e -> String.valueOf(e.getIndex() + start)).collect(Collectors.joining("."));
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
	public Stream<Node<T>> getParentPathNodes() {
		return Node.getPathNodes(this.getParent());
	}

	/**
	 * 获取节点路径
	 * 
	 * @param node 节点
	 * @return 节点路径
	 */
	public String getPath(final Node<T> node) {
		return this.getPath(node, null);
	}

	/**
	 * 获取节点路径
	 * 
	 * @param node        节点
	 * @param name_mapper 节点名称变换器 : node->String
	 * @return 节点路径
	 */
	public String getPath(final Node<T> node, final Function<Node<T>, String> name_mapper) {
		if (node == null)
			return "";
		final Function<Node<T>, String> final_name_mapper = name_mapper == null ? Node::getName : name_mapper;
		String ppath = this.getPath(node.getParent(), final_name_mapper);
		if (!ppath.matches("[\\s]*"))
			ppath += "/";
		return ppath + final_name_mapper.apply(node);
	}

	/**
	 * 获取节点路径
	 * 
	 * @param <X>节点的值的类型
	 * @param node       节点对象
	 * @return 节点路径
	 */
	public static <X> Stream<Node<X>> getPathNodes(final Node<X> node) {
		if (node == null)
			return Stream.of();
		else
			return Stream.concat(Node.getPathNodes(node.getParent()), Stream.of(node));
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public Object recget(final String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.get(key);
		}
		return null;
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public String recstr(final String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.str(key);
		}
		return null;
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public Number recnum(final String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.num(key);
		}
		return null;
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public Integer reci4(final String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.i4(key);
		}
		return null;
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public Double recdbl(String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.dbl(key);
		}
		return null;
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public Timestamp rectimestamp(final String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.timestamp(key);
		}
		return null;
	}

	/**
	 * 这是专门为IRecord 设计的一个提取键值的简单的办法 String key record 的键值名
	 */
	public Date recdate(final String key) {
		if (this.value != null && this.value instanceof IRecord) {
			IRecord rec = (IRecord) this.value;
			return rec.date(key);
		}
		return null;
	}

	/**
	 * 获取节点相对于父节点的子节点索引号<br>
	 * 索引序号从0开始,根节点的Index 序号为0
	 * 
	 * @return 节点的索引号
	 */
	public int getIndex() {
		if (this.isRoot())
			return 0;
		final var parent = this.getParent();
		return parent.getChildIndex(this);
	}

	/**
	 * 获取所有的子节点信息记录
	 * 
	 * @return 所有子节点包括空节点
	 */
	public List<Node<T>> getChildren() {
		return children;
	}

	/**
	 * 提取满足条件的节点
	 * 
	 * @param predicate 节点条件过滤器
	 * @return 满足predicate 过滤条件的节点
	 */
	public List<Node<T>> getChildren(final Predicate<Node<T>> predicate) {
		return children.stream().filter(predicate).collect(Collectors.toList());
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
	 * 添加子节点,自动设置子节点的父节点,做检测c的存在性检测
	 * 
	 * @param cc 子节点,null 节点不添加
	 */
	public void addChildren(final Iterable<Node<T>> cc) {
		for (Node<T> c : cc)
			this.addChild(c);
	}

	/**
	 * 添加子节点,自动设置子节点的父节点,做检测c的存在性检测
	 * 
	 * @param c 子节点,null 节点不添加
	 */
	public void addChild(final Node<T> c) {
		this.addChild(c, false, true);
	}

	/**
	 * 添加子节点,自动设置子节点的父节点
	 * 
	 * @param c           子节点
	 * @param b           是否添加空节点,true 添加空节点,false 空节点不添加
	 * @param checkExists 是否做子节点的存在性检测 true 检测(同一节点可以添加多次),false 不检测
	 */
	public void addChild(final Node<T> c, final boolean b, final boolean checkExists) {
		if (c != null) {
			if (checkExists && this.hasChild(c)) {// 存在性检测
				// System.out.println("节点"+c+",已存在");
				return;
			} // if 存在性检测
			this.children.add(c);
			c.setParent(this);
		} else {
			if (b)
				this.children.add(null);
		}
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
	 * @param cc 孩子节点列表
	 * @return
	 */
	public boolean hasChildren(final List<Node<T>> cc) {
		return (Integer) cc.stream().mapToInt(e -> this.hasChild(e) ? 1 : 0).sum() == cc.size();
	}

	/**
	 * 添加子节点
	 * 
	 * @param cc 子节点集合
	 */
	void addChildren(final List<Node<T>> cc) {
		this.children.addAll(cc);
		cc.forEach(c -> c.setParent(this));// 设置父节点
	}

	/**
	 * 注意这里是通过addChildren 完成父子关系的构建。设置父节点 并不保证父节点的子节点中包含有 本节点(this)。所以可以
	 * setParent(null) 这样就在逻辑上构建了一个独立的树（根节点没有父节点，每颗树只有一个父节点）
	 *
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
		var n = this.size();
		if (i < n) {
			this.children.set(i, p);// 设置孩子节点
			if (p != null)
				p.setParent(this);// 设置父亲节点
		} // if
	};

	/**
	 * 没有子节点或是子节点为null的节点的高度为1<br>
	 * 返回节点的高度 ，没有孩子的节点高度为1,否个节点的高度为最高的孩子节点的高度+1
	 * 
	 * @return 节点的高度，从1开始。
	 */
	public int height() {
		final var children = this.getChildren(Objects::nonNull);
		if (children.size() == 0)
			return 1;
		return children.parallelStream().map(Node::height).collect(Collectors.summarizingInt(e -> e)).getMax() + 1;
	}

	/**
	 * 判断当前节点是否为根节点
	 *
	 * @return
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
	 * @return 节点的层级,根节点为1层,根节点的子节点层级为2,以此类推, 即 子节点的层级为父节点的层级+1
	 */
	public Integer getLevel() {
		return this.getLevel(this);
	}

	/**
	 * 获取指定节点所在的层级(空节点层级为0) 节点的层级,根节点为1层,根节点的子节点层级为2,以此类推, 即 子节点的层级为父节点的层级+1
	 * 
	 * @param node 节点数据
	 * @return 节点的层级,根节点为1层,根节点的子节点层级为2,以此类推, 即 子节点的层级为父节点的层级+1
	 */
	public Integer getLevel(final Node<T> node) {
		if (node == null)
			return 0;
		return this.getLevel(node.getParent()) + 1;
	}

	/**
	 * 获取属性值
	 * 
	 * @param name 属性名称
	 * @return 属性值
	 */
	public Object prop(final String name) {
		return properties.get(name);
	}

	/**
	 * 获取属性值
	 * 
	 * @param <U>    旧属性值类型
	 * @param <V>    新属性值类型
	 * @param name   属性名称
	 * @param mapper 属性变换函数 u:旧值->v:新值 把name 视为 属性名称 对其进行变换。
	 * @return V 类型的值。
	 */
	@SuppressWarnings("unchecked")
	public <U, V> V prop(final String name, final Function<U, V> mapper) {
		return mapper.apply((U) properties.get(name));
	}

	/**
	 * 提取属性集合，只读 设置为无效。
	 * 
	 * @return IRecord 属性集合
	 */
	public IRecord attributes() {
		return IRecord.REC(this.properties);
	}

	/**
	 * 提取属性值并制定属性值的类型，只读 设置为无效。
	 * 
	 * @param <U>         返回的结果类型
	 * @param name        属性名称
	 * @param targetClass 返回结果的类型类
	 * @return U 类型的属性值
	 */
	@SuppressWarnings("unchecked")
	public <U> U attr(final String name, final Class<U> targetClass) {
		return this.attributes().get(name, targetClass == null ? (Class<U>) Object.class : targetClass);
	}

	/**
	 * 设置属性值
	 * 
	 * @param name  属性名称
	 * @param value 属性值
	 * @return
	 */
	public Object prop(final String name, final Object value) {
		return properties.put(name, value);
	}

	/**
	 * 获取属性集合
	 * 
	 * @return
	 */
	public Map<String, Object> props() {
		return properties;
	}

	/**
	 * 字符串属性
	 * 
	 * @param name 属性名
	 * @return
	 */
	public String strProp(final String name) {
		Object obj = this.prop(name);
		return obj == null ? null : obj + "";
	}

	/**
	 * 数字属性属性
	 * 
	 * @param name 属性名
	 * @return
	 */
	public Number numProp(final String name) {
		Object obj = this.prop(name);
		if (obj == null)
			return null;
		if (obj instanceof Number)
			return (Number) obj;
		try {
			obj = Double.parseDouble(obj + "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
		return (Number) obj;
	}

	/**
	 * 数字属性属性
	 * 
	 * @param name 属性名
	 * @return
	 */
	public int intProp(final String name) {
		return numProp(name).intValue();
	}

	/**
	 * 数字属性属性
	 * 
	 * @param name 属性名
	 * @return
	 */
	public double dblProp(final String name) {
		return numProp(name).doubleValue();
	}

	public String toString() {
		return value + "";
	}

	/**
	 * 使用产品的名字进行节点散列：采用名称name 进行散开列。
	 * 
	 * @return hashCode
	 */
	public int hashCode() {
		// 之所以不采用路径的hashcode 的是为了提高效率，hashCode 的效率。
		return this.getName().hashCode();// 根据名称
	}

	/**
	 * 依据名称来进行判断对象属性
	 * 
	 * @param obj 比较对象
	 */
	public boolean equals(final Object obj) {
		if (this.hashCode() != obj.hashCode())
			return false;
		if (obj instanceof Node) {// 类型相互一致才进行比较
			@SuppressWarnings("unchecked")
			final Node<T> node = (Node<T>) obj;
			final var p0 = this.getParent();
			final var p1 = node.getParent();
			if (p0 == null && p1 != null)
				return false;
			else if (p0 == p1 || p0.equals(p1)) {// 先对父级路径进行比较
				final var v0 = this.getValue();
				final var v1 = node.getValue();
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

	/**
	 * 根据路径获取子节点,路径采用"/"进行分割 对于路径 A/B/C, 以节点 A为例 nodeA.path("B/C") 返回节点C
	 * 
	 * @param path 元素路径
	 * @return Node 结构
	 */
	public Node<T> get(final Node<T> node, final String path) {
		if (node == null)
			return null;
		int p = path.indexOf("/");// 产品路径 backslash 位置 pos
		final String name = p < 0 ? path : path.substring(0, p);// 获取一级目录项目
		final String rest = p < path.length() ? path.substring(p + 1) : null;
		if (name.equals(node.getName()))
			return node;
		else {
			Optional<Node<T>> c = node.children.stream().filter(e -> e.getName().equals(name)).findFirst();
			if (c.isPresent() && rest != null)
				return get(c.get(), rest);// 从子节点重继续寻找
		}

		return null;
	}

	/**
	 * 路径节点
	 */
	public Node<T> get(final String path) {
		return get(this, path);
	}

	/**
	 * 节点遍历
	 * 
	 * @param cs   节点长处理函数
	 * @param node 遍历的节点起始节点
	 */
	public static <U> void forEach(final Consumer<Node<U>> cs, Node<U> node) {
		if (node == null || cs == null)
			return;
		cs.accept(node);
		for (Node<U> u : node.getChildren())
			forEach(cs, u);
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
	 * @return 叶子节点
	 */
	public List<Node<T>> getAllLeaves() {
		return this.flatMap().stream().filter(Node::isLeaf).collect(Collectors.toList());
	}

	/**
	 * 树形结构的节点。扁平化成序列列表 以该节点为起始节点，进行树形结构遍历
	 *
	 * 示例： MAP(root.flatMap(),f->f.val(g->g.str("name")));// 生成一个树形结构的额各个节点名称序列。
	 * 这里是一个Node<IRecord> 结构，并且IRecord中包含了name字段
	 */
	public synchronized List<Node<T>> flatMap() {
		return this.flatMap(e -> e);
	}

	/**
	 * 以该节点为起始节点，进行树形结构遍历
	 * 
	 * @param mapper 节点的处理函数 把 <T> 类型转换成 <U> 类型。
	 */
	public synchronized <U> List<U> flatMap(final Function<Node<T>, U> mapper) {
		if (mapper == null)
			return null;
		List<U> list = new LinkedList<>();
		this.forEach(e -> list.add(mapper.apply(e)));
		return list;
	}

	/**
	 * 树形结构的节点。扁平化成序列列表 以该节点为起始节点，进行树形结构遍历
	 *
	 * 示例： MAP(root.flatMap(),f->f.val(g->g.str("name")));// 生成一个树形结构的额各个节点名称序列。
	 * 这里是一个Node<IRecord> 结构，并且IRecord中包含了name字段
	 */
	public synchronized Stream<Node<T>> flatStream() {
		return this.flatStream(e -> e);
	}

	/**
	 * 以该节点为起始节点，进行树形结构遍历
	 * 
	 * @param mapper 节点的处理函数 把 <T> 类型转换成 <U> 类型。
	 */
	public synchronized <U> Stream<U> flatStream(final Function<Node<T>, U> mapper) {
		return this.flatMap(mapper).stream();
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

	private T value;// 节点的信息值
	private Node<T> parent = null;// 父节点，初始化为null,表示他是一个根节点，没有父节点
	private Function<Node<T>, String> lambda_name = null;// 获得节点的名称
	private List<Node<T>> children = new LinkedList<>();// 子节点：集合

	private Map<String, Object> properties = new LinkedHashMap<>();// 属性记录的存储空间
}

