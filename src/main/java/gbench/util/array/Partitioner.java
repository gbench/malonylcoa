package gbench.util.array;

import java.util.Stack;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Collector;
import java.util.stream.Stream;

import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiFunction;

import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.tree.TrieNode;

/**
 * 分区器，用户对一个连续区域进行索引化分区 <br>
 * <br>
 * 树形结构 (1,(2,3)) 的划分, 采用指令：P(1,P(2,3)) 构造如下划分结构 <br>
 * <p>
 * (null,[]/(partitioner:{0=1, 1=(partitioner:{0=2, 1=3})})[false] <br>
 * start:0,end:6,length:6) <br>
 * -|-(0,[0]/1[true] start:0,end:1,length:1) <br>
 * -|-(1,[1]/(partitioner:{0=2, 1=3})[false] start:1,end:6,length:5) <br>
 * -|--|-(0,[1, 0]/2[true] start:1,end:3,length:2) <br>
 * -|--|-(1,[1, 1]/3[true] start:3,end:6,length:3) <br>
 *
 * @author gbench
 */
public class Partitioner extends LinkedHashMap<String, Object> {

	/**
	 * 根节点
	 *
	 * @return 根节点元素
	 */
	public Elem root() {
		if (this.root == null) {
			this.root = this.initialize();
		}
		return this.root;
	}

	/**
	 * 每次进行初始化都会生成一个新的元素根节点Elem
	 * 
	 * @return 元素根节点Elem
	 */
	public Elem initialize() {
		final AtomicReference<Elem> ar = new AtomicReference<>();
		final AtomicInteger ai = new AtomicInteger();
		this.getLeafS().forEach(e -> {
			e.addVolume(e.volume());
			e.start = ai.getAndAdd(e.volume());
			Elem p = e;
			do {
				if (p.parent != null) {
					p.parent.addVolume(e.length());
					if (p.parent.start == null) {
						p.parent.start = p.start;
					}
				} else if (ar.get() == null) {
					ar.set(p);
				} // if
			} while ((p = p.parent) != null);
		}); // forEach

		final Elem root = ar.get();

		return root;
	}

	/**
	 * 是否是叶子节点
	 *
	 * @return 是否是叶子节点
	 */
	public boolean isLeaf() {
		return this.size() == 0;
	}

	/**
	 * 提取叶子节点
	 *
	 * @return 叶子节点的数据流
	 */
	public Stream<Elem> getLeafS() {
		return this.buildTreeS().filter(Elem::isLeaf);
	}

	/**
	 * 提取值列表
	 *
	 * @param reverse_flag 归集标志,true 调转, false 正序
	 * @return 值流
	 */
	public Stream<Map.Entry<String, Object>> entrySetS(final boolean reverse_flag) {
		return this.entrySet().stream().collect(Lisp.llclc(reverse_flag)).stream();
	}

	/**
	 * 获取子元素
	 *
	 * @return 子元素流
	 */
	public Stream<Tuple2<String, Object>> getChildrenS() {
		return this.entrySet().stream().map(e -> Tuple2.of(e.getKey(), e.getValue()));
	}

	/**
	 * 数据拾取
	 *
	 * @param <U>    结果元素类型
	 * @param mapper 元素映射
	 * @return U类型的数据流
	 */
	public <U> Stream<U> stream(final Function<Tuple2<List<Integer>, Tuple2<Integer, Integer>>, U> mapper) {
		return this.strides().entrySet().stream() //
				.map(e -> Tuple2.of(e.getKey(), e.getValue())).map(mapper);
	}

	/**
	 * 数据拾取
	 *
	 * @param <U>    结果元素类型
	 * @param mapper 元素映射
	 * @param offset 数据偏移
	 * @return U类型的数据流
	 */
	public <U> Stream<U> stream(final Function<Tuple2<List<Integer>, Tuple2<Integer, Integer>>, U> mapper,
			final int offset) {
		return this.strides().entrySet().stream().map(e -> Tuple2.of(e.getKey(), //
				Tuple2.of(e.getValue()._1 + offset, e.getValue()._2 + offset))) //
				.map(mapper);
	}

	/**
	 * 分区索引跨度
	 * <p>
	 * 调用示例： <br>
	 * final var p2 = P2("A", 2, "B", 2, "C", 2,"D",P(1,2)); <br>
	 * p2.strides(true); <br>
	 * <br>
	 * P2("A", 2, "B", 2, "C", 2) <br>
	 * (null,[]/(partitioner:{A=2, B=2, C=2, D=(partitioner:{0=1, 1=2})})[false]
	 * start:0,end:9,length:9) <br>
	 * <p>
	 * -|-(0,[A]/2[true] start:0,end:2,length:2) <br>
	 * -|-(1,[B]/2[true] start:2,end:4,length:2) <br>
	 * -|-(2,[C]/2[true] start:4,end:6,length:2) <br>
	 * -|-(3,[D]/(partitioner:{0=1, 1=2})[false] start:6,end:9,length:3) <br>
	 * -|--|-(0,[D, 0]/1[true] start:6,end:7,length:1) <br>
	 * -|--|-(1,[D, 1]/2[true] start:7,end:9,length:2) <br>
	 * {[]=(0,9), [0]=(0,2), [1]=(2,4), [2]=(4,6), [3]=(6,9), [3, 0]=(6,7), [3,
	 * 1]=(7,9)} <br>
	 *
	 * @return 分区索引跨度
	 */
	public LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides() {
		final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides = new LinkedHashMap<>();
		this.strides(false).forEach((k, v) -> {
			strides.put(Arrays.asList(k), v);
		});
		return strides;
	}

	/**
	 * 分区索引跨度
	 * <p>
	 * 调用示例： <br>
	 * final var p2 = P2("A", 2, "B", 2, "C", 2,"D",P(1,2)); <br>
	 * p2.strides(true); <br>
	 * <br>
	 * P2("A", 2, "B", 2, "C", 2) <br>
	 * (null,[]/(partitioner:{A=2, B=2, C=2, D=(partitioner:{0=1, 1=2})})[false]
	 * start:0,end:9,length:9) <br>
	 * <p>
	 * -|-(0,[A]/2[true] start:0,end:2,length:2) <br>
	 * -|-(1,[B]/2[true] start:2,end:4,length:2) <br>
	 * -|-(2,[C]/2[true] start:4,end:6,length:2) <br>
	 * -|-(3,[D]/(partitioner:{0=1, 1=2})[false] start:6,end:9,length:3) <br>
	 * -|--|-(0,[D, 0]/1[true] start:6,end:7,length:1) <br>
	 * -|--|-(1,[D, 1]/2[true] start:7,end:9,length:2) <br>
	 * {[]=(0,9), [0]=(0,2), [1]=(2,4), [2]=(4,6), [3]=(6,9), [3, 0]=(6,7), [3,
	 * 1]=(7,9)} <br>
	 *
	 * @param flag 是否显示调试信息
	 * @return 分区索引跨度
	 */
	public LinkedHashMap<Integer[], Tuple2<Integer, Integer>> strides(final boolean flag) {
		return this.strides(e -> {
			if (flag) {
				final String indent = Stream.iterate(0, i -> i + 1).limit(e._2().paths().size()).map(i -> "-|-")
						.collect(Collectors.joining(""));
				final String line = String.format("%s%s", indent, e);
				System.err.println(line);
			}
		});
	}

	/**
	 * 分区索引跨度
	 * <p>
	 * 调用示例： <br>
	 * final var p2 = P2("A", 2, "B", 2, "C", 2,"D",P(1,2)); <br>
	 * p2.strides(true); <br>
	 * <br>
	 * P2("A", 2, "B", 2, "C", 2) <br>
	 * (null,[]/(partitioner:{A=2, B=2, C=2, D=(partitioner:{0=1, 1=2})})[false]
	 * start:0,end:9,length:9) <br>
	 * <p>
	 * -|-(0,[A]/2[true] start:0,end:2,length:2) <br>
	 * -|-(1,[B]/2[true] start:2,end:4,length:2) <br>
	 * -|-(2,[C]/2[true] start:4,end:6,length:2) <br>
	 * -|-(3,[D]/(partitioner:{0=1, 1=2})[false] start:6,end:9,length:3) <br>
	 * -|--|-(0,[D, 0]/1[true] start:6,end:7,length:1) <br>
	 * -|--|-(1,[D, 1]/2[true] start:7,end:9,length:2) <br>
	 * {[]=(0,9), [0]=(0,2), [1]=(2,4), [2]=(4,6), [3]=(6,9), [3, 0]=(6,7), [3,
	 * 1]=(7,9)} <br>
	 *
	 * @param cons 回调函数 (index:元素索引,elem:元素对象)->{}
	 * @return 分区索引跨度
	 */
	public LinkedHashMap<Integer[], Tuple2<Integer, Integer>> strides(Consumer<Tuple2<Integer, Elem>> cons) {

		if (this.strides == null) {
			this.strides = new LinkedHashMap<>();
			this.root().forEach(e -> {
				this.strides.put(e._2.pathS2().toArray(Integer[]::new), Tuple2.of(e._2.start(), e._2.end()));
				if (cons != null) {
					cons.accept(e);
				}
			});
		}
		return this.strides;
	}

	/**
	 * 生成树形结构，节点元素为Elem
	 *
	 * @return 由Elem 构成的数据流 需要竞购 消费才能有效。
	 */
	public Stream<Elem> buildTreeS() {
		final Stack<Elem> stack = new Stack<>();
		final Elem root = E(null, this, null, null);
		stack.push(root);

		return Stream.iterate(root, q -> !stack.empty(), q -> {
			if (stack.empty())
				return null;
			else {
				final Elem elem = stack.pop();
				if (elem != null) { // 空值作为结束标记
					final Partitioner partitioner = elem.partitioner();
					if (partitioner != null) {
						final AtomicInteger ai = new AtomicInteger(partitioner.size() - 1);
						partitioner.entrySetS(true).forEach(entry -> {
							final Object v = entry.getValue();
							final String key = entry.getKey();
							final int index = ai.getAndDecrement();

							if (v instanceof Partitioner) {
								final Partitioner _x = (Partitioner) v;
								stack.push(elem.create(_x, key, index));
							} else if (v instanceof Integer) {
								final Integer _x = (Integer) v;
								stack.push(elem.create(_x, key, index));
							} else { // 其余的给予省略
								// do nothing
								throw new RuntimeException(String.format("非法类型%s", //
										Optional.ofNullable(v).map(Object::getClass) //
												.map(Object::toString).orElse("null")));
							} // if
						}); //
					} // if
					if (stack.empty()) { // 增加一个 空节点以保证 最后一个节点可以 顺利输出。
						stack.push(null); // 空值的占位元素。这个 空值占位是 专为 hasNext而设计的。
					} // if
				} // if
				return elem;
			}
		}).skip(1);
	}

	/**
	 * 节点元素流
	 * 
	 * @param <T>    元素类型
	 * @param mapper 元素变化函数 elem -> T
	 * @return T类型的元素流
	 */
	public <T> Stream<T> elemS(final Function<Elem, T> mapper) {
		return this.root().stream().map(e -> mapper.apply(e._2));
	}

	/**
	 * 节点元素 的 流
	 * 
	 * @return 节点元素流
	 */
	public Stream<Elem> elemS() {
		return this.root().stream().map(e -> e._2);
	}

	/**
	 * 开始索引 从0开始
	 *
	 * @return 开始索引 从0开始 inclusive
	 */
	public int start() {
		return this.root().start();
	}

	/**
	 * 结束索引 从0开始
	 *
	 * @return 结束索引 exclusive
	 */
	public int end() {
		return this.root().end();
	}

	/**
	 * 拾取区间长度
	 * 
	 * @return 分区长度
	 */
	public int length() {
		return this.end() - this.start();
	}

	/**
	 * 结果值计算
	 * 
	 * @param <V>       结果值
	 * @param path      键名路径 路径采用[,/.]进行作为分隔符。a/b/c 当前节点为a
	 *                  路径a/bc/指向c节点。当当前elem为根节点时候,会自动根据需要补充null作为path开头。
	 * @param evaluator 计算器根据分区索引跨度来计算相应数据.(start,end)->v
	 * @return V类型结果
	 */
	public <V> Optional<V> evaluate(final String path, final BiFunction<Integer, Integer, V> evaluator) {
		return Optional.ofNullable(this.root().getElem(path)).map(e -> e.evaluate(evaluator));
	}

	/**
	 * 结果值计算
	 * 
	 * @param <V>       结果值
	 * @param path      键名路径 路径采用[,/.]进行作为分隔符。a/b/c 当前节点为a
	 *                  路径a/bc/指向c节点。当当前elem为根节点时候,会自动根据需要补充null作为path开头。
	 * @param evaluator 计算器根据分区索引跨度来计算相应数据.(start,end)->v
	 * @return V类型结果
	 */
	public <V> Optional<V> evaluate(final String[] path, final BiFunction<Integer, Integer, V> evaluator) {
		return Optional.ofNullable(this.root.getElem(path)).map(e -> e.evaluate(evaluator));
	}

	/**
	 * 结果值计算
	 * 
	 * @param <V>       结果值
	 * @param index     索引值：索引值从0开始。<br>
	 *                  []或者null返回根节点,<br>
	 *                  [0]返回第一个第一基层元素。<br>
	 *                  [0,0]第一个第二阶层的第一个元素<br>
	 *                  [0,1]第一个第二基层的第二个元素 <br>
	 *                  [1,0]第二个第二阶层的第一个元素<br>
	 *                  [1,1]第二个第二基层的第二个元素 <br>
	 * @param evaluator 计算器根据分区索引跨度来计算相应数据.(start,end)->v
	 * @return V类型结果
	 */
	public <V> Optional<V> evaluate(final Integer[] index, final BiFunction<Integer, Integer, V> evaluator) {
		return Optional.ofNullable(this.root.getElem(index)).map(e -> e.evaluate(evaluator));
	}

	/**
	 * 拾取变换
	 *
	 * @param <U>    数据元素
	 * @param mapper 数据变化函数
	 * @return U类型结果
	 */
	public <U> U mutate(final Function<Partitioner, U> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 生成 TrieTree 即 多维键名单值列表 [([int],ndarray)],根节点为null
	 *
	 * @param <V>    键值类型
	 * @param mapper 区间变换函数 (start,end) -> v
	 * @return 生成TrieTree,根节点为null, 多维键名单值列表的 kvps 列表 [([int],ndarray]) 即
	 *         TrieNode&lt;Integer&gt;
	 */
	public <V> TrieNode<Integer> trieTree(final BiFunction<Integer, Integer, V> mapper) {
		final Collector<? super Tuple2<List<Integer>, V>, ?, TrieNode<Integer>> collector = Partitioner.trieclc();
		return this.stream(e -> e.fmap2(Tuple2.bifun(mapper))) //
				.collect(collector);
	}

	@Override
	public String toString() {
		return String.format("(partitioner:%s)", super.toString());
	}

	/**
	 * 节点元素
	 *
	 * @author gbench
	 */
	public static class Elem {

		/**
		 * 创建元素
		 *
		 * @param parent 父节点
		 * @param value  值对象
		 * @param key    键名
		 * @param index  键名索引
		 */
		public Elem(final Elem parent, final Tuple2<Partitioner, Integer> value, final String key,
				final Integer index) {
			this.parent = parent;
			this.value = value;
			this.key = key;
			this.index = index;
			if (parent != null) {
				this.parent.addChildren(this.parent.children.size(), this);
			}
		}

		/**
		 * 创建元素
		 *
		 * @param value 值对象
		 * @param key   键名
		 * @param index 键名索引
		 */
		public Elem create(final Tuple2<Partitioner, Integer> value, final String key, final Integer index) {
			return new Elem(this, value, key, index);
		}

		/**
		 * 创建元素
		 *
		 * @param p     分区器
		 * @param key   键名
		 * @param index 键名索引
		 */
		public Elem create(final Partitioner p, final String key, final Integer index) {
			return new Elem(this, Tuple2.of(p, null), key, index);
		}

		/**
		 * 创建元素
		 *
		 * @param volume 容量
		 * @param key    键名
		 * @param index  键名索引
		 */
		public Elem create(final Integer volume, final String key, final Integer index) {
			return new Elem(this, Tuple2.of(null, volume), key, index);
		}

		/**
		 * 键名索引：分区定义的键名索引，P 或 P2 定义中的 键值的顺序
		 *
		 * @return 分区定义的键名索引，从0开始
		 */
		public Integer index() {
			return this.index;
		}

		/**
		 * 键名:分区定义的键名，P 或 P2 定义中的 所指定的键名
		 *
		 * @return 分区定义的键名
		 */
		public String key() {
			return key;
		}

		/**
		 * 分区的开始索引
		 * 
		 * @return 分区的开始索引,大于等于0的整数,inclusive
		 */
		public int start() {
			return this.start;
		}

		/**
		 * 分区的结束索引
		 * 
		 * @return 分区的结束索引,大于0的整数,exclusive
		 */
		public int end() {
			return this.start + this.length;
		}

		/**
		 * 子分区器
		 *
		 * @return 子分区器
		 */
		public Partitioner partitioner() {
			return this.value._1;
		}

		/**
		 * 节点容量，分局的跨度范围
		 *
		 * @return 节点容量
		 */
		public Integer volume() {
			return this.value._2;
		}

		/**
		 * 分区容量
		 *
		 * @return
		 */
		public Integer length() {
			return this.length;
		}

		/**
		 * 值对象 的 Optional 结构
		 *
		 * @return Optional 对象
		 */
		public Optional<Object> valOpt() {
			return Optional.ofNullable(this.isLeaf() ? this.value._2 : this.value._1);
		}

		/**
		 * 路径:元素流 <br>
		 * 以key键名为元素 <br>
		 *
		 * @return 路径元素流
		 */
		public Stream<String> pathS() {
			return Optional.ofNullable(this.parent).map(p -> Stream.concat(p.pathS(), Stream.of(this.key())))
					.orElse(Stream.empty());
		}

		/**
		 * 路径:元素流 <br>
		 * 以index索引为元素 <br>
		 *
		 * @return 路径元素流
		 */
		public Stream<Integer> pathS2() {
			return Optional.ofNullable(this.parent).map(p -> Stream.concat(p.pathS2(), Stream.of(this.index())))
					.orElse(Stream.empty());
		}

		/**
		 * 路径 <br>
		 * 以key键名为元素 <br>
		 *
		 * @return 路径
		 */
		public List<String> paths() {
			return this.pathS().collect(Collectors.toList());
		}

		/**
		 * 路径 <br>
		 * 以index索引为元素 <br>
		 *
		 * @return 路径
		 */
		public List<String> paths2() {
			return this.pathS().collect(Collectors.toList());
		}

		/**
		 * 是否是根节点
		 *
		 * @return 是否是根节点
		 */
		public boolean isLeaf() {
			return this.value._2 != null;
		}

		/**
		 * 是否是根节点
		 * 
		 * @return 是否是根节点
		 */
		public boolean isRoot() {
			return this.parent == null;
		}

		/**
		 * 修改容量
		 *
		 * @param volume 容量
		 */
		public void addVolume(final int volume) {
			this.length += volume;
		}

		/**
		 * 子节点
		 *
		 * @return 子节点序列
		 */
		public Stream<Tuple2<Integer, Elem>> getChildrenS() {
			return this.children.entrySet().stream().map(e -> Tuple2.of(e.getKey(), e.getValue()));
		}

		/**
		 * 根据路径读取 节点元素
		 * 
		 * @param path 键名路径 路径采用[,/.]进行作为分隔符。a/b/c 当前节点为a
		 *             路径a/bc/指向c节点。当当前elem为根节点时候,会自动根据需要补充null作为path开头。
		 * @return Elem 节点元素
		 */
		public Elem getElem(final String path) {
			return this.getElem(path.split("[.,/]+"));
		}

		/**
		 * 根据路径读取 节点元素
		 * 
		 * @param path 键名路径. a/b/c 当前节点为a
		 *             路径a/bc/指向c节点。当当前elem为根节点时候,会自动根据需要补充null作为path开头。
		 * @return Elem 节点元素
		 */
		public Elem getElem(final String[] path) {
			return this.getElem(Arrays.asList(path));
		}

		/**
		 * 根据路径读取 节点元素
		 * 
		 * @param index 索引值：索引值从0开始。<br>
		 *              []或者null返回根节点,<br>
		 *              [0]返回第一个第一基层元素。<br>
		 *              [0,0]第一个第二阶层的第一个元素<br>
		 *              [0,1]第一个第二基层的第二个元素 <br>
		 *              [1,0]第二个第二阶层的第一个元素<br>
		 *              [1,1]第二个第二基层的第二个元素 <br>
		 * @return Elem 节点元素
		 */
		public Elem getElem(final Integer[] index) {
			return this.getElem2(Arrays.asList(index));
		}

		/**
		 * 根据路径读取 节点元素
		 * 
		 * @param index 索引值：索引值从0开始。<br>
		 *              []或者null返回根节点,<br>
		 *              [0]返回第一个第一基层元素。<br>
		 *              [0,0]第一个第二阶层的第一个元素<br>
		 *              [0,1]第一个第二基层的第二个元素 <br>
		 *              [1,0]第二个第二阶层的第一个元素<br>
		 *              [1,1]第二个第二基层的第二个元素 <br>
		 * @return Elem 节点元素
		 */
		public Elem getElem2(final List<Integer> index) {
			return this.getElem(index, e -> e.index, Integer.class);
		}

		/**
		 * 根据路径读取 节点元素
		 * 
		 * @param path 键名路径. a/b/c 当前节点为a 路径a/bc/指向c节点。
		 *             当当前elem为根节点时候,会自动根据需要补充null作为path开头。
		 * @return Elem 节点元素
		 */
		public Elem getElem(final List<String> path) {
			return this.getElem(path, e -> e.key, String.class);
		}

		/**
		 * 根据路径读取 节点元素
		 * 
		 * @param path             键名路径. a/b/c 当前节点为a 路径a/bc/指向c节点。
		 *                         当当前elem为根节点时候,会自动根据需要补充null作为path开头。
		 * @param firstelem_getter 路径首元素
		 * @param placeholder      类型占位符，用于指导编译器做多态实现选择，Integer.class标示索引index，String.class
		 *                         标示键名key
		 * @return Elem 节点元素
		 */
		public <T> Elem getElem(final List<T> path, final Function<Elem, T> firstelem_getter,
				final Class<T> placeholder) {
			if (this.isRoot() && (path == null || path.size() == 0)) { // 根节点path无效，直接返回根节点
				return this;
			} else { // path 有效
				final List<T> _path = (this.isRoot() && path != null && path.size() > 0 && path.get(0) != null)
						? Optional.of(path).map(LinkedList::new).map(lpath -> { // 根据path信息补充null作为路径开始
							lpath.add(0, null); // 为根节点补充null标记
							return lpath;
						}).get()
						: path;

				if (_path.size() < 1) { // 路径长度无效
					return null;
				} else {
					final T key = _path.get(0);
					final boolean flag = Objects.equals(key, firstelem_getter.apply(this)); // 键名匹配
					if (flag) { // 键名首位匹配
						if (_path.size() < 2) { // 路径也业已抵达末端
							return this;
						} else { // 路径包含子路径则进行递归查询。
							final List<T> __path = _path.subList(1, _path.size()); // 提取子路径
							return this.children.values().stream()
									.map(elem -> elem.getElem(__path, firstelem_getter, placeholder))
									.filter(Objects::nonNull).findFirst().orElse(null);
						} // if
					} else { // 首位不匹配则退出
						return null;
					} // if flag
				} // if _path
			} // if isRoot
		} // getElem

		/**
		 * 添加子节点
		 *
		 * @param key  键名
		 * @param elem 元素
		 */
		public void addChildren(final Integer key, final Elem elem) {
			this.children.put(key, elem);
		}

		/**
		 * 元素流化
		 *
		 * @return Elem 元素流,根节点元素为(null,elem)
		 */
		public Stream<Tuple2<Integer, Elem>> stream() {
			final Stack<Tuple2<Integer, Elem>> stack = new Stack<>();
			final Tuple2<Integer, Elem> root = Tuple2.of(null, this);
			stack.push(root);
			return Stream.iterate(root, e -> !stack.empty(), e -> {
				final Tuple2<Integer, Elem> p = stack.pop();
				if (p == null) { // 已经抵达了最有一个元素之后,此时 stack 已经是empty了。
					assert stack.empty() : String.format("堆栈未抵达栈底，出现了空值！%s", stack);
					return null;
				} //
				stack.addAll(p._2().getChildrenS().collect(Collectors.toList())); // 加入所有元素
				if (stack.empty()) { // 抵达最有一个元素，添加一个 null由于标记迭代技术。
					stack.push(null);
				} // if
				return p;
			}).skip(1); // 去除首位元素，避免重复
		}

		/**
		 * 遍历函数
		 *
		 * @param cons 回调函数 (index:元素索引,elem:元素对象)->{}
		 */
		public void forEach(final Consumer<Tuple2<Integer, Elem>> cons) {
			cons.accept(Tuple2.of(this.index(), this));
			this.getChildrenS().collect(Lisp.llclc(true)).forEach(e -> {
				e._2.forEach(cons);
			}); // forEach
		}

		/**
		 * 分区计算
		 * 
		 * @param <V>       结果类型
		 * @param evaluator 分区计算函数,(start,end)->v
		 * @return V类型结果
		 */
		public <V> V evaluate(final BiFunction<Integer, Integer, V> evaluator) {
			return evaluator.apply(this.start(), this.end());
		}

		/**
		 * 格式化
		 */
		public String toString() {
			return String.format("%s/%s[%s] start:%s,end:%s,length:%s", this.paths(), //
					this.isLeaf() ? this.value._2 : this.value._1, this.isLeaf(), //
					this.start, start + this.length, this.length);
		}

		// 变量区域 加速访问
		private Integer length = 0;
		private Integer start = null;
		// 常量区域
		private final Elem parent;
		private final Integer index;
		private final String key;
		private final Tuple2<Partitioner, Integer> value;
		private final LinkedHashMap<Integer, Elem> children = new LinkedHashMap<>();

	}

	/**
	 * 节点元素
	 * 
	 * @param parent 父节点
	 * @param volume 节点容量
	 * @param key    键名
	 * @param index  键名索引从0开始
	 * @return Elem
	 */
	public Elem E(final Elem parent, final Integer volume, final String key, final Integer index) {
		return new Elem(parent, Tuple2.of(null, volume), key, index);
	}

	/**
	 * 节点元素
	 * 
	 * @param parent      夫节
	 * @param partitioner 分区器
	 * @param key         键名
	 * @param index       键名索引从0开始
	 * @return 节点元素
	 */
	public Elem E(final Elem parent, final Partitioner partitioner, final String key, final Integer index) {
		return new Elem(parent, Tuple2.of(partitioner, null), key, index);
	}

	/**
	 * 分区器构造 <br>
	 * <p>
	 * P(1,P(2,3)) 构造如下划分结构 <br>
	 * (null,[]/(partitioner:{0=1, 1=(partitioner:{0=2, 1=3})})[false]
	 * start:0,end:6,length:6) <br>
	 * -|-(0,[0]/1[true] start:0,end:1,length:1) <br>
	 * -|-(1,[1]/(partitioner:{0=2, 1=3})[false] start:1,end:6,length:5) <br>
	 * -|--|-(0,[1, 0]/2[true] start:1,end:3,length:2) <br>
	 * -|--|-(1,[1, 1]/3[true] start:3,end:6,length:3) <br>
	 *
	 * @param <V> 元素类型
	 * @param vs  分区序列,1,P(2,3),...
	 * @return Partitioner
	 */
	@SafeVarargs
	public static <V> Partitioner P(final V... vs) {
		final Partitioner q = new Partitioner();
		for (int i = 0; i < vs.length; i++) {
			q.put(String.valueOf(i), vs[i]);
		}
		return q;
	}

	/**
	 * 分区器构造
	 * <p>
	 * (null,[]/(partitioner:{0=1, 1=(partitioner:{0=2, gbench=3})})[false]
	 * start:0,end:6,length:6) <br>
	 * --------------------------------------------------------------------------
	 * <br>
	 * -|-(0,[0]/1[true] start:0,end:1,length:1) <br>
	 * -|-(1,[1]/(partitioner:{0=2, gbench=3})[false] start:1,end:6,length:5) <br>
	 * -|--|-(0,[1, 0]/2[true] start:1,end:3,length:2) <br>
	 * -|--|-(1,[1, gbench]/3[true] start:3,end:6,length:3) <br>
	 * --------------------------------------------------------------------------
	 * <br>
	 * [] []/(partitioner:{0=1, 1=(partitioner:{0=2, gbench=3})})[false]
	 * start:0,end:6,length:6 <br>
	 * [0] [0]/1[true] start:0,end:1,length:1 <br>
	 * [1] [1]/(partitioner:{0=2, gbench=3})[false] start:1,end:6,length:5 <br>
	 * [1, 0] [1, 0]/2[true] start:1,end:3,length:2 <br>
	 * [1, gbench] [1, gbench]/3[true] start:3,end:6,length:3 <br>
	 *
	 * @param <V> 元素类型
	 * @param vs  分区定义，key1,value1,key2,value2,...
	 * @return Partitioner
	 */
	@SafeVarargs
	public static <V> Partitioner P2(final V... vs) {
		final Partitioner p = new Partitioner();
		for (int i = 0; i < vs.length - 1; i += 2) {
			p.put(String.valueOf(vs[i]), vs[i + 1]);
		}
		return p;
	}

	/**
	 * 整数分析函数
	 */
	public static Function<String, Integer> parseint = s -> {
		Integer i = Integer.MAX_VALUE;
		try {
			i = Integer.parseInt(s);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return i;
	};

	/**
	 * 多维键名单值-键值对-归集器 <br>
	 * 多维键为 List<Integer> : 分区索引 <br>
	 * 单值为 V : 分区结构,构建器 (start,end)->v 构建的 V类型对象 <br>
	 * V 作为 TrieNode 的 属性值:value 给予 存储。
	 * 
	 * @param <V> 单值类型
	 * @return 多维键名单值-键值对-归集器 [([k],v)]->trienode
	 */
	static <V> Collector<? super Tuple2<List<Integer>, V>, ?, TrieNode<Integer>> trieclc() {
		return TrieNode.trieclc((Integer) null, (trienode, v) -> {
			final Integer[] index = trienode.partS().skip(1).toArray(Integer[]::new);
			final TrieNode<Integer> parent = trienode.getParent();

			if (parent != null && Objects.equals(true, parent.attrGet("isleaf"))) {
				parent.attrSet("isleaf", false);
			} // if

			trienode.getAttributes().add("value", v, "index", index, //
					"level", trienode.getLevel() - 1, // 节点所在层级
					// 叶子属性默认为true,待子节点追加的时候给予更新
					"isleaf", true);
		});
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 17547960642734348L;
	private Elem root = null;
	private LinkedHashMap<Integer[], Tuple2<Integer, Integer>> strides = null;

}
