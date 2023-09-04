package gbench.util.math.algebra;

import static gbench.util.math.algebra.op.Comma.COMMA_TEST;
import static gbench.util.math.algebra.op.Ops.TOKEN;
import static gbench.util.math.algebra.op.Ops.kvp_int;
import static gbench.util.math.algebra.op.Ops.println;
import static gbench.util.math.algebra.symbol.Node.PACK;
import static gbench.util.math.algebra.symbol.Node.UNPACK;
import static gbench.util.math.algebra.tuple.MyRecord.REC;
import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.op.Ops;
import gbench.util.math.algebra.op.UnaryOp;
import gbench.util.math.algebra.symbol.Node;
import gbench.util.math.algebra.tuple.DFrame;
import gbench.util.math.algebra.tuple.IRecord;
import gbench.util.math.algebra.tuple.Tuple2;

/***
 * 代数引擎 <br>
 * 符号计算 的 简单实现
 * 
 * @author gbench
 *
 */
public class AlgebraEngine {

	/**
	 * 构造函数
	 */
	public AlgebraEngine() {

		this.initialize();
	}

	/**
	 * 数据初始化
	 */
	public AlgebraEngine initialize() {

		Ops.priorities.forEach(tup -> { // 合并优先级
			this.localPriorities.compute(tup._1, (k, v) -> {
				return v == null ? tup._2 : v;
			}); // compute
		}); // forEach

		Stream.of( // 添加括号符号
				P(LEFT_PARENT, RIGHT_PARENT) // 圆括号
//				, P(PACK(TOKEN("[")), PACK(PACK(TOKEN("]")))) // 方括号
//				, P(PACK(TOKEN("{")), PACK(PACK(TOKEN("}")))) // 花括号
		).forEach(brace -> { // 括号的匹配
			this.addBrace(brace);
		}); // forEach

		return this;
	}

	/**
	 * 分词器
	 * 
	 * @param line 表达式行
	 * @return 分词
	 */
	public List<IRecord> tokenize(final String line) {

		println(line);
		println();

		final var letters = "_\\.a-z0-9\\u4e00-\\u9fa5"; // 基础字母+中文
		final var digits = Pattern.compile("[\\.0-9]+", Pattern.CASE_INSENSITIVE);
		final var pattern = Pattern.compile(String.format("[%s]+", letters), Pattern.CASE_INSENSITIVE);
		final var pattern1 = Pattern.compile(String.format("^[%s]+.*", letters), Pattern.CASE_INSENSITIVE);
		final var pattern2 = Pattern.compile(String.format(".*[%s]$", letters), Pattern.CASE_INSENSITIVE);
		final var pattern3 = Pattern.compile(String.format("[^%s]+", letters), Pattern.CASE_INSENSITIVE);
		final var buffer = new StringBuffer();
		final var rb = IRecord.rb("type,value");
		final Function<String, Stream<IRecord>> keyword_handler = keyword -> {
			final var _keyword = keyword.strip(); // 去除多余的空格
			final var rec = digits.matcher(_keyword).matches() // 检测是否满足数字类型
					? rb.get("number", _keyword) // 数字类型
					: rb.get("word", _keyword); // 关键词类型
			return Stream.of(rec);
		}; // keyword_handler

		final Function<String, Stream<IRecord>> op_handler = op -> {
			return op.chars().mapToObj(c -> String.valueOf((char) c)) //
					.filter(e -> !e.matches("s*")).map(e -> rb.get("op", e));
		};

		final var tokens = new LinkedList<IRecord>();
		final var wordS = line.strip().chars().mapToObj(c -> String.valueOf((char) c)).flatMap(c -> {
			final var word = buffer.append(c).toString();
			final var matcher = pattern.matcher(buffer);
			final var n = word.length();
			if (matcher.matches()) { // 关键词
				// do nothing
			} else {
				if (pattern1.matcher(word).matches()) {// 关键字+运算符号
					final var keyword = buffer.substring(0, n - 1);
					buffer.delete(0, n - 1);
					return keyword_handler.apply(keyword);
				} else if (pattern2.matcher(word).matches()) { // 运算符号+关键词
					final var op = buffer.substring(0, n - 1);
					buffer.delete(0, n - 1);
					return op_handler.apply(op);
				} else if (pattern3.matcher(word).matches()) { // 没有关键字
					//
				} else {
					//
				}
			} // if

			return null;
		}).filter(Objects::nonNull).filter(e -> !e.str("value").matches("\\s*")) // 过滤空白值
				.onClose(() -> {
					final var _line = buffer.toString();
					final var tkns = ((pattern.matcher(_line).matches()) ? keyword_handler : op_handler).apply(_line)
							.filter(e -> !e.str("value").matches("\\s*")); // 过滤空白值
					tokens.addAll(tkns.collect(Collectors.toList()));
				});

		wordS.forEach(tokens::add);
		wordS.close();

		final var stateLevel = new AtomicInteger(0);
		tokens.forEach(e -> {
			if (e.get("value").equals("(")) { // 层次开始
				e.add("level", stateLevel.getAndUpdate(x -> x + 1)); // ( 不增加层次
			} else if (e.get("value").equals(")")) { // 层次结束
				e.add("level", stateLevel.updateAndGet(x -> x - 1)); // ) 需要减少层次
			} else { // 普通节点
				e.add("level", stateLevel.get()); // 直接读取层次
			}
		});

		return tokens;
	}

	/**
	 * 为节点序列 设计 算符的 比较分组
	 * 
	 * @param nodes               节点序列
	 * @param predicate_data_node 数据节点的检测函数
	 * @return (1:目标比较分组, 2:比较分组集合[(1:比较分组结果,2:比较分组元素集合[(1:元素索引位置,2:第二比较元素)]] ) <br>
	 *         比较分组元素集合[左侧比较元素 a,比较元素 b] 即 代表 a &lt;比较&gt; b 的 结构形式
	 */
	public Tuple2<Integer, List<Tuple2<Integer, List<Tuple2<Integer, Node>>>>> compareGroups(final List<Node> nodes,
			final Predicate<Node> predicate_data_node) {

		final var opnodes = new ArrayList<Tuple2<Integer, Node>>(); // 算符 的 节点序列

		nodes.stream().map(kvp_int()) // 添加节点的位置索引序号
				.forEach(tup -> {
					final var node = tup._2(); // 提取node结构
					if (!predicate_data_node.test(node)) { // 仅 数据类型的节点 才 给予 进行 算符归集
						final var opnode = this.getOpByName(tup._2().getName()).get(); // 算符原型的对象
						opnodes.add(P(tup._1(), PACK(opnode))); // 转换成算符队形给予保存
					} // if
				});

		println("\nnodes的当前状态:", nodes);
		final var node_rb = IRecord.rb("no,name,是否是算符");
		final var dfm = nodes.stream().map(kvp_int()).map(kvp -> {
			return node_rb.get(kvp._1(), kvp._2().getName(), !predicate_data_node.test(kvp._2()));
		}).collect(DFrame.dfmclc);
		println(dfm);
		println("运算符序列", opnodes);

		// 逗号表达式直接返回，这是图省事，省心,鸵鸟的办法，眼不见心不烦，物质决定意识，意识 可以 选择 见不见物质，哈哈
		if (opnodes.stream().allMatch(e -> COMMA_TEST(e._2().getName()))) {
			return null;
		} // if

		// [(比较结果,[(op1_index,op1_node),(op2_index,op2_node)])]
		final var compare_groups = IRecord.slidingS(opnodes, 2, 1, true).map(opnode_pair -> {
			final var opnode1 = opnode_pair.get(0)._2(); // 运算符的 Node结构
			final var opnode2 = opnode_pair.get(1)._2(); // 运算符的 Node结构
			final var result = this.priorityCompare(opnode1, opnode2); // 运算符的比较等级
			return P(result, opnode_pair);
		}).collect(Collectors.toList()); // compare_groups

		// 结果排序后查看
		compare_groups.forEach(e -> { // 排序分组遍历
			final var result = e._1;
			final var opnode_pair = e._2;
			final var opnode1 = opnode_pair.get(0)._2(); // 运算符的 Node结构
			final var opnode2 = opnode_pair.get(1)._2(); // 运算符的 Node结构
			println("算符优先级比较记录:compare", P(opnode_pair.get(0)._1(), opnode1.getName()),
					P(opnode_pair.get(1)._1(), opnode2.getName()), "--->", result);
		}); // while

		/**
		 * 关于优先级传递问题的解决： 所谓优先级传递，看一个例子: (1,2+3*5) , <br>
		 * 依据分组规则:比较分组为 [ ( -1, (, +) ), ( -1, (+ ,*) ) ], <br>
		 * 注意到这里 出现 '+' 运算符的 优先级 传到 '*' , 也就是 优先级 不能够 读取 算符比较的 最小值, <br>
		 * 而是 要 顺延 比较组 的间 优先级 的递增顺序 给予 传导, 直到 传导链条 链条 终止。 <br>
		 * 也就是 本次使用优先级
		 */
		int i = compare_groups.stream().map(kvp_int()).filter(e -> e._2._1 < 0) // 找出第一个出现 优先级差异的
				.findAny().map(e -> e._1).orElse(0); // 目标算符 的 优先级比较分组的下标索引
		final int n = compare_groups.size(); // 比较组的列表长度

		// 比较结果<0即后续运算的优先级高度前面元素的优先级的时候才给与进行
		if (n > 0 && compare_groups.get(i)._1 < 0) {
			// 邻近的比较群的 关系处理
			while (i < n - 1) { // group 比较群 , tuple 比较对儿, 一个 比较群 中 包含 两个连续的 比较对二组
				final var group_1 = compare_groups.get(i); // 当前比较群，1#比较群
				final var group_2 = compare_groups.get(i + 1); // 当前比较群的 后继 比较群， 2#比较群
				final var tuples_1 = group_1._2(); // 当前 即 1# 比较群的 比较对儿组
				final var tuples_2 = group_2._2(); // 后继 即 2# 比较群的 比较对儿组
				final int tup1_index2 = tuples_1.get(1)._1; // 第一比较组的成员表 的 第二个元素 的 下标索引
				final int tup2_index1 = tuples_2.get(0)._1; // 第二比较组的成员表 的 第一个元素 的 下标索引
				final int tup2_index2 = tuples_2.get(1)._1; // 第二比较组的成员表 的 第二个元素 的 下标索引

				// 紧挨着的两个比较组出现了优先级的递增梯度 , 出现了 3 * sin cos x 这样的情况 '*'<'sin'='cos',并且sin的右侧紧挨着cos
				final var transit = (group_1._1() < 0 && group_2._1() < 0); // 优先级在算符从前向后依次递增的状态
				final boolean adjacent = tup1_index2 == tup2_index1; // 算符是否比邻: 1#比较对儿的2#位符号 与 2#比较对儿的1#位符号 相同
				final boolean composed = tup2_index1 + 1 == tup2_index2; // 出现sin cos tan ... 这样的循环嵌套的情况，即 符号一个接一个的出现
				if (transit && adjacent) { // 优先级 传递 以及 高级别算符 出现了 比邻显现 3 + sin cos x 需要把优先级 从 sin 传递到 cos
					i++; //
				} else if (composed) { // 参数被复合， 出现sin cos tan ... 这样的循环嵌套的情况，即 符号一个接一个的出现
					i++;
				} else { // 不做优先级传递
					break;
				} // if

			} // while

			println("目标算符优级分组 传转 到 索引位置:", i, "最优比较组 为:", compare_groups.get(i));
		} // if

		return P(i, compare_groups);
	}

	/**
	 * 数据预处理 <br>
	 * 
	 * 算符的优先级处理 比如: <br>
	 * <br>
	 * 对于 节点序列 1+2*3 即 节点序列 [1,+,2,*,3 ], <br>
	 * 由于 * 比 + 优先级高，所以 * 会优先 进行 参数实装, 也就是 生成一个 运算节点 (*2,3) <br>
	 * 会 返回 [1,+,(*,2,3)] 这样的 三个 节点的 节点序列,<br>
	 * 注意 (*,2,3)是一个复合节点 或是 算符“*”的实装节点 .<br>
	 * 也就是 在 数据被规约前 事先将 优先级高的 算符 给予 提前 实装出来。<br>
	 * <br>
	 * 算符的优先处理 会 采用 递归的方式 进行处理 , 每次 只规约 当前 优先级 差别最高的算符 <br>
	 * 例如 对于逗号表达式: 2,3+5*6 由于 `,`&lt;+&lt;* 所以,<br>
	 * 这里 会 优先 规约"*"算符 实装成 (*,5,6) 得到 [2, "," , 3, +, (*,5,6) ] <br>
	 * 然后规约 “+” 得到 [2, "," , (+,3,(*,5,6)) ] <br>
	 * 这样就 只剩下了 三个元素的 节点序列 为止， 其实 这就是 一个 最简答的 逗号表达式了 <br>
	 * <br>
	 * 对于 没有优先级别差异的 节点序列 比如 1+2-3 或是 1/2*3 这样的节点序列，将不予处理，给予 直接返回
	 * 
	 * @param nodes 节点序列
	 * @return 优先为高级别的算符匹配相应的参数
	 */
	public List<Node> preProcess(final List<Node> nodes) {

		final Predicate<Node> predicate_data_node = node -> { // 节点分两类:算符节点,数据节点 即 一个 node 不是 算符 就是 数据
			if (this.getOpByName(node.getName()).isPresent()) { // 是否是一个算符关键词, 是 算符关键词 需要 继续判断 是否一个 计算表达式
				if (node.isOp()) { // 合法算符的情况
					return node.getOp().getArgs()._1() != null; // 实装了参数的算符 就变成了数据 否则依旧是算符
				} else { // 没有计算能力，只是 长得像算符
					return false;
				} // if
			} else { // 不是算符关键词 则 一定是 数据node
				return true;
			} // if
		};

		/**
		 * 生成比较列表
		 */
		final var compare_result = this.compareGroups(nodes, predicate_data_node);
		if (compare_result == null) { //
			println("优先级顺滑无需处理");
			return nodes;
		}

		final var groups_data = compare_result._2; // 比较分组数据列表
		final var target_grp_index = compare_result._1; // 目标 算符比较分组 所在 索引序号, 目标算符 是 目标 分组的 第二个 元素。
		final var min = groups_data.stream().collect(Collectors.summarizingDouble(e -> e._1)).getMin(); // 确定最小值
		if (groups_data.size() < 1 || min >= 0) { // 数据优先级正序不需要处理
			println("数据优先级正序不需要处理", nodes);
			return nodes; // 返回结果
		} else { // 算符之间存在优先级差别，需要给予计算
			// 邻近的算符优先级的比较
			final var targetGroup = groups_data.get(target_grp_index); // 目标比较组
			println("\n优先级差异最大的算符对儿为", targetGroup);
			println("尝试为:" + targetGroup._2().get(1) + "确定计算参数,即 创建算符的规约结构");

			final var op_tuple = targetGroup._2().get(1);// 目标算符
			final var index = op_tuple._1(); // 目标算符的语句小标索引
			final var op = op_tuple._2().getOp(); // 提取算符原型 并 尝试 生成 算符的 运算结构
			final var tobe_removed = new ArrayList<Integer>(); // 将要移除的节点的索引位置
			final var params_installed = new AtomicInteger(2); // 实装的参数数量 默认为2 即 二元参数
			final var handle = Optional.of(op.getAry()).map(nary -> { // 算符的元数
				final Function<Node, Object> unpack = node -> { // 解包数据，元素类型
					return node.isToken() ? node.getToken() : node.unpack();
				};
				final Supplier<Node> unitary_routine = () -> { // 一元算符
					params_installed.set(1); // 实装参数数量标记为1
					final var arg = nodes.get(index + 1);
					tobe_removed.add(index + 1); // 记录参数索引
					if (COMMA_TEST(arg.getName()) && op.getAry() == 2) { // 逗号表达式
						final var oo = arg.getOp().getArgS().map(Node::UNPACK).toArray(Object[]::new);
						final var left = oo.length > 0 ? oo[0] : null; // 左参数
						final var right = oo.length > 1 ? oo[1] : null; // 右参数

						println("实装" + op_tuple + "\n\t第一参数" + left + "\n\t");
						println("实装" + op_tuple + "\n\t第二参数" + right + "\n\t");
						return PACK(op.compose(oo[0], oo[1]));
					} else {
						println("实装" + op_tuple + "\n\t第一参数" + arg + "\n\t");
						return op instanceof UnaryOp ? PACK(((UnaryOp<?>) op).compose(unpack.apply(arg)))
								: PACK((op).compose1(unpack.apply(arg)));
					} // if
				}; // unary_routine

				tobe_removed.add(index); // 记录算符的索引位置
				if (nary == 1) { // 一元函数参数在右边
					return unitary_routine.get();
				} else if (nary == 2) { // 二元运算符
					println("-----------------------------------");
					println("组装算符 op:", op);
					println("节点序列 nodes:", nodes);
					println("-----------------------------------");

					final var left = index < 1 ? null : nodes.get(index - 1); // 第一参数
					final var right = nodes.get(index + 1); // 第二参数
					final var flag = left == null || (!predicate_data_node.test(left)) // op 左侧不是一个数据而是算符
							&& COMMA_TEST(right.getName()); // 参数1是一个算符的情况是不能做方法参数的

					if (!flag) { // 有效的二元函数
						Stream.of(index - 1, index + 1).forEach(tobe_removed::add);
						println("\n实装" + op_tuple + "\n\t第一参数:" + left + "\n\t 第二参数:" + right + "\n");
						return PACK(op.compose(unpack.apply(left), unpack.apply(right)));
					} else { // 视作一元函数
						println("由于算符" + op_tuple + "的:" + "右侧参数 " + right + " 是一个逗号表达式的元组,\n\t" + "左侧参数 " + left
								+ " 是一个算符,\n\t" + "故判断此为二元函数的前缀形式,使用右侧元组参数来实装该算符.");
						return unitary_routine.get();
					} // if
				} else { // 非法的 函数 元数
					System.err.println("函数的元数非法" + nary);
					return null;
				} // if
			}).orElse(null); // handle 规约句柄

			final var pos = params_installed.get() == 2 // handle回填到节点序列的位置,即 参数的实装状态决定
					? index - 1 // 实装了二个参数,插入位置为左侧参数位置
					: index; // 没有实装二个参数,插入位置为算符位置
			final var buffer = new LinkedList<Node>(); // 工作缓存

			println(op_tuple + "实装为:\n\t", handle);
			println("--------------------------");
			nodes.stream().map(kvp_int()) // 加入位置索引序号
					.filter(kvp -> !tobe_removed.contains(kvp._1())).map(e -> e._2()) // 提取数据项
					.forEach(buffer::add);
			buffer.add(pos, handle);

			// 一次调整之后 数据
			println("整理之后的新的节点序列状态:", buffer);
			return this.preProcess(buffer); // 递归进行数据处理
		} // 算符优先级比较
	}

	/**
	 * 层次化处理 <br>
	 * 
	 * 把一个带有层级标识（左，右括号）的词法节点序列data处理成对应的层级结构即AST语法树。<br>
	 * 把 一个 节点 序列 根据 层次化 指令节点 LEFT_PARENT "(" , RIGHT_PARENT ")" 的 指导 进行 结构规约 即 层级构建
	 * 
	 * @param data         词法节点序列
	 * @param stack        运行堆栈，当为 null 的时候进行自我规约
	 * @param reduceAction 规约算法，是一种 小分组规约 契机, 在 reduceAction 中 算符 按照 同一优先级 从左到右 依次规约
	 * @return AST 语法树
	 */
	public Node handle(final List<Node> data, final Stack<Node> stack, final Function<List<Node>, Node> reduceAction) {

		final var _stack = stack == null ? new Stack<Node>() : stack;
		final var batchTerms = new LinkedList<Node>(); // 处理批次的表达式项目,处于同一优先级处理级别的数据项，规约批次项集缓存
		final var itr = data.iterator(); // 数据迭代器

		println("handle : ", data);
		while (itr.hasNext()) { // 优先级调整之后的 语法树构建
			final var currentNode = itr.next(); // 提取当前的算符
			final var leftPart = this.getLeftPart(currentNode); // 当前符号currentNode的左半边

			// 读取到了终点 或 读取到了分组结束（左半边存在）,需要判断 间或 条件以保证 末尾元素恶意得到处理
			if (!itr.hasNext() || leftPart.isPresent()) { // 触发规约条件，尝试向前回溯，准备规约

				// 已经清空读取堆栈 或是 读回了 分组开始
				Node node = null; // 栈顶元素
				while (!_stack.empty() && !name_eq.test(node = _stack.pop(), leftPart.orElse(null))) {
					batchTerms.addFirst(node);
					println(batchTerms, " <- 出栈 ", _stack);
				} // while

				// 若是由读取到末尾的条件触发的规约 ，尝试 把末尾节点加入到 规约批次缓存batchTerms 之中来
				if (!itr.hasNext() && leftPart.isEmpty()) { // 末尾触发 而 非分组触发（左半边为空）
					if (this.getLeftPart(currentNode).isEmpty()) { // 尾部的currentNode 不属于括号类别的字符
						// 注意这里是 add 而不是 addFirst, addFirst是堆栈符号，add 是对栈外符号
						batchTerms.add(currentNode); // 规约批次缓存
					} else { // 分组触发（左半边非空） 即 currentNode 是一个右括号
						// 右括号类别的尾部字符currentNode 给予省略
					} // if
				} // if

				// 分组触发，需要 根据 left 的条件 判断是 需要保留 left算符，即 如果 left 被定义为 计算算符则需要 保留，留置于堆栈内。
				leftPart.ifPresent(left -> { // 右括号的触发 即 存在左半边的符号 触发规约
					this.getOpByName(left.getName()).ifPresent(op -> { // 左半边是一个算符则给予保留，留置于堆栈内
						stack.push(left); // left算符放入堆栈 给予 以便 做进一步的规约,比如 定义了意义的 "[" 算符。
					}); // ifPresent op
				});// leftPart.ifPresent

				println("尝试规约:", batchTerms);
				_stack.push(reduceAction.apply(batchTerms)); // 规约节点压栈
				println("\n 规约后 stack 状态:");
				_stack.stream().map(kvp_int()).sorted((a, b) -> b._1() - a._1()).forEach(e -> {
					println(e._1(), e._2());
				});
				batchTerms.clear();
				println("----------------------\n");
			} else { // 数据暂存继续向前读取
				_stack.push(currentNode);
				println(currentNode, " 压栈 -> ", _stack);
			}
		}

		final var ret = _stack.empty() ? null : _stack.peek(); // 提取栈顶元素作为返回值
		println("规约值", ret, "\n堆栈状况:", _stack);
		if (ret == null) {
			println("规约成了一个空值，出现了数据异常");
		}

		return ret;
	}

	/**
	 * 没有 优先级识别能力的 节点项序列 算法 <br>
	 * 
	 * 扁平化的 词法节点序列 的 语法树构建 <br>
	 * reduce 没有 算符识别能力 只是 单一的 按照 算符的 出现 顺序 从左到右逐一的 进行 节点序列的 规约， <br>
	 * 如果 需要 使用 带有 算符 优先级 识别 能力的 节点规约 请使用 buildTree 算法。 <br>
	 * 
	 * @param terms 词法节点项序列
	 * @return 语法树的根节点
	 */
	public Node reduce(final Collection<Node> terms) {

		final var seq = terms.stream().map(e -> e.isToken() ? e.getName() : e.toString()) // 提取项目名称
				.map(e -> COMMA_TEST(e) ? "','" : e) // 对 "," 进行转义
				.collect(Collectors.toList());

		println("\n----------------------------");
		println("terms 项目的计算:", seq);
		println("----------------------------");

		final Function<Node, Object> node2arg = node -> {
			if (node == null)
				return null;
			return node.isOp() ? node.unpack() : node.getToken(); // 蜕掉糖衣转化为参数数据
		};
		final var elemStack = new Stack<Node>(); // 数据堆栈，数据项堆栈
		final var opStack = new Stack<BinaryOp<Object, Object>>(); // 运算堆栈，算符堆栈
		final var ai = new AtomicInteger(0);

		terms.forEach(term -> { // 逐项目处理
			final var name = term.getName(); // term 项目的名称
			final var i = ai.getAndIncrement(); // 下标索引
			final Optional<BinaryOp<Object, Object>> opOptional = // 尝试从 term 中 提取 算符，算符 只能只能 从 token 里提取，handle 不可以。
					term.isToken() // term.isToken()用来保证规约掉的handle(handle可以视为一种复合数据term)不会被重复计算，即只能从纯token中提取算符。
							? this.getOpByName(name) // 尝试根据term的name提取 Ops 对象
							: Optional.empty(); // 无效的算符

			println(i, name, opOptional); // 打印term 摘要

			// 语法树 的 构建 是 通过 数据 term 来 触发 算符规约 来进行 的。要知道 数据项 elem 是条件，是动力！！！
			// 这就是为何 语法树的 句柄 handle 的 构建 是位于 数据项分支 之中的 原因。
			// 数据项term又叫elem, 使用elemStack保存
			// 算符保存在opStack

			if (opOptional.isPresent()) { // 算符项分支:term词素所代表的名称是一个有效的运算操作, 读取到 算符，记录到算符栈, 等待 被数据项目term唤醒。
				opStack.push(opOptional.get()); // 加入 运算堆栈
			} else {// 数据项分支:读取到了数据元素,计算规约的关键是通过数据项目term来触发算符栈中的算符来进行的,数据是动力,算符是本质。尝试唤醒算符
				if (!opStack.isEmpty()) { // 算符栈 含有 算符 表明 该term项目可能触发出 计算规约 即 生成 计算句柄 handle 的 可能。
					final BinaryOp<Object, Object> binaryOp = opStack.peek(); // 查看栈定元素
					final var nary = binaryOp.getAry();
					if (nary == 1) { // 一元运算符
						final var arg = UNPACK(node2arg.apply(term));
						final var handle = ((UnaryOp<Object>) opStack.pop()).compose(arg); // 一元算符 规约句柄
						println("规约成 一元运算符:", handle);
						elemStack.push(PACK(handle));
					} else if (nary == 2) { // 二元运算符
						final var comma_term_flag = term.isOp() && COMMA_TEST(term.getName()); // COMMA 的条件检测
						if (!elemStack.isEmpty() || comma_term_flag) { // 二元运算 对于 除了 COMMA-逗号term以外,要求 elemStack 至少有一个元素
							final var oo = (elemStack.isEmpty() // 根据堆栈数据状态 确定算符的参数
									? term.getOp().getArgS() // comma_term_flag,逗号表达式, 拆分逗号表达式的参数 来构造左右参数
									: Stream.of(elemStack.pop(), term).map(node2arg)).map(Node::UNPACK) //
									.toArray(); // 堆栈不为空,从数据栈中提取一个数据合并当前项目term作为方法参数
							final var handle = (opStack.pop()).compose(oo[0], oo[1]); // 二元算符 规约句柄
							println("规约成 二元运算符:", handle);
							elemStack.push(PACK(handle));
						} else { // elemStack 为空
							elemStack.push(term);
						} // if elemStack
					} else {
						System.err.println("不支持[" + nary + "]元的数据运算");
					} // if ops
				} else { // opStack 中不包含算符 无法触发 计算程序：即规约节点, 将数据 保存到 elemStack 等待后续被唤醒
					elemStack.push(term);
				} // if opsStack
			} // if term
		}); // terms.forEach

		println("elemStack:", elemStack);
		println("opsStack:", opStack);

		// 计算符号栈中的尚未计算完全的运算, 注入 嵌套类 函数 需要如此 规约 比如 sin cos (tan x) 这样函数
		while (!opStack.isEmpty()) {
			final var op = opStack.pop(); // 操作符堆栈
			final BinaryOp<Object, Object> handle = Optional.of(op.getAry()) // 尝试生成规约句柄
					.map(nary -> { // 根据 参数元数 生成规约句柄
						if (nary == 2) { // 二元运算
							final var oo = Stream.iterate(0, i -> i + 1).limit(2)
									.map(i -> elemStack.empty() ? null : elemStack.pop()) // 依次从数据项堆栈弹出数据
									.map(node2arg).map(Node::UNPACK).toArray(); // oo[0] 右边的参数, oo[1] 左边
							// 对于 采用
							final var flag = (oo[1] == null && oo[0] != null); // 是否交换1,参数的位置
							final var left = flag ? oo[0] : oo[1];
							final var right = flag ? oo[1] : oo[0];

							return op.compose(left, right);
						} else if (nary == 1) {
							final var arg = elemStack.pop().unpack(); // 提取第一参数并给予 解包

							// 注意一定要用 一元函数的 compose, 以保证 调用 一个元算符 重载的 compose 否则就会更改掉 算符的 元数
							// 若是 调用 op.compose(arg,null) 就是调用二元函数的compose 生成一个 BinaryOp 类型的结构 而 非 Op的真实
							// 类型，即 op 的真实类型丢失了
							return ((UnaryOp<?>) op).compose(arg);
						} else {
							System.err.println("非法操作符" + op);
							return null;
						}
					}).orElse(null); // 规约句柄

			elemStack.push(PACK(handle));
		} // while

		final var resultNode = elemStack.pop();
		println("AST:", resultNode);

		return resultNode;
	}

	/**
	 * 
	 * 构建语法树 <br>
	 * 
	 * 非扁平化 词法节点序列 的语法树构建 即 <br>
	 * 带有优先级的识别功能的 语法树构建，为不同优先级的算符构建其对应的层级结构。 <br>
	 * 需要数据中不能包含 分组 符号:LEFT_PARENT,RIGHT_PARENT <br>
	 * 
	 * @param nodes 词法节点序列,不能含有 LEFT_PARENT,RIGHT_PARENT 两个字符
	 * @return AST 语法树
	 */
	public Node buildTree(final List<Node> nodes) {

		final var opt = nodes.stream() // 检测非法字符
				.filter(e -> name_eq.test(e, LEFT_PARENT) || name_eq.test(e, RIGHT_PARENT)).findFirst();

		if (opt.isPresent()) {
			System.err.println("buildTree 数据中不能包含分组符号:" + Arrays.asList(LEFT_PARENT, RIGHT_PARENT));
			return null;
		}

		return this.handle(this.preProcess(nodes), null, this::reduce);
	}

	/**
	 * 词素(词法符号 token) 的 结构 化分析 即 语法树的生辰 <br>
	 * 
	 * analyze 词素结构化 分成了 两个层次 <br>
	 * 1) buildTree 过程 : 把 有优先级差异 的 节点序列 分组成 一 系列的 无差异 分组，<br>
	 * 1.1) 先 深一个 阶层 做 无差异 节点 规约，<br>
	 * 1.2) 而后 再退回 来 继续 做 无优先级差异 节点。 <br>
	 * 简单的说就是 把 一个 优先级有差异的节点 序列 转换 成 无差异序列<br>
	 * 即 无差异节点序列 的 树形结构（层级）的 过程。也就是 节点层次化。 <br>
	 * 对应算法 buildTree <br>
	 * 2) reduce 过程 : 无优先级差异的 节点规约 对应 算法 reduce <br>
	 * 
	 * @param tokens 符号流
	 * @return 语法树结构 AST, 当 tokens 长度为0 返回null
	 */
	public Node analyze(final List<IRecord> tokens) {

		final var data = tokens.stream().map(e -> PACK(TOKEN(e.str("value")))).collect(Collectors.toList()); // 生成数据节点序列
		final var stack = new Stack<Node>(); // 节点工作栈

		/**
		 * buildTree 是一种 带有 算符 优先级 识别 能力的 规约 算法，所以
		 */
		this.handle(data, stack, this::buildTree); // 处理节点工作栈的

		if (stack.size() > 1) {
			var state = new Stack<Node>(); // 状态检测数据栈
			final var ret = this.handle(enclose(stack), state, this::buildTree);
			if (state.size() > 1) { // 正常情况 数据栈中 只有一个一个元素 超过一个 就是存在 语法错误
				try {
					throw new Exception(println("\n stack:", stack, "\n tokens:", tokens, "\n存在法语法错误"));
				} catch (Exception e) {
					e.printStackTrace();
				} // try
			} // if
			return ret;
		} else {
			return stack.empty() ? null : stack.peek();
		}
	}

	/**
	 * 分析一个 词法序列 生成 对应表达式 <br>
	 * (2*pow(x+1,2)+x) 对于 pow 这样的二元 函数 采用 前缀式 写法 , 即 逗号表达式的元组 来 提供参数的 情况, <br>
	 * 采用分组 提交的方式 会 逃避掉 buildTree 的 优先级的 方法调整，会造成 语法 分析失败， 所以 请去掉 分组 符号 <br>
	 * analyze("2*pow(x+1,2)+x") 就可以了。 <br>
	 * analyze("(2*pow(x+1,2)+x)") 会造成 分组混乱
	 *
	 * @param line 表达式的字符串表达
	 * @return Node
	 */
	public Node analyze(final String line) {

		final var tokens = tokenize(line); // 提取词法序列
		return this.analyze(tokens);
	}

	/**
	 * 根据运算名称查找运算对象
	 * 
	 * @param opName 运算对象的名称
	 * @return optional 的 运算对象
	 */
	@SuppressWarnings("unchecked")
	public Optional<BinaryOp<Object, Object>> getOpByName(final String opName) {

		final var op = (BinaryOp<Object, Object>) localOps.get(opName);
		return Optional.ofNullable(op).or(() -> Ops.lookup(opName));
	}

	/**
	 * 增加 运算符
	 * 
	 * @param bop 算符对象
	 * @return AlgebraEngine 对象本身
	 */
	public AlgebraEngine add(final BinaryOp<?, ?> bop) {

		return this.add(bop, null);
	}

	/**
	 * 增加 运算符
	 * 
	 * @param bop      算符对象
	 * @param priority 算符优先级,priority 为 null 表示采用算符优先级
	 * @return AlgebraEngine 对象本身
	 */
	public AlgebraEngine add(final BinaryOp<?, ?> bop, final Number priority) {

		this.localOps.put(bop.getName(), bop); // 注册算符

		final var _priority = Optional.ofNullable(priority).orElse(bop.getPriority()); // 提取算符优先级
		if (null != _priority) { // 注册优先级
			this.localPriorities.add(bop.getName(), _priority);
		} // if

		return this;
	}

	/**
	 * 添加分组括号（分组标记）<br>
	 * 
	 * 圆括号 是 天然分组 不需要添加
	 * 
	 * @param brace 括号对儿比如：("(",")"), ("[","]"), ("{","}"), ("‘","’"),
	 *              ("&lt;","&gt;"), ("《","》") 等
	 * @return AlgebraEngine 对象本身 以便实现链式编程。
	 */
	public AlgebraEngine addBrace(final Tuple2<Node, Node> brace) {

		final var rightPart = brace._2;
		this.parentheses.put(rightPart.getName(), brace);

		return this;
	}

	/**
	 * 添加分组括号（分组标记） <br>
	 * 
	 * 圆括号 是 天然分组 不需要添加
	 * 
	 * @param brace 括号对儿比如：("(",")"), ("[","]"), ("{","}"), ("‘","’"),
	 *              ("&lt;","&gt;"), ("《","》") 等
	 * @return AlgebraEngine 对象本身 以便实现链式编程。
	 */
	public AlgebraEngine addBrace(final String brace[]) {

		if (brace.length < 2) {
			return null;
		}

		return this.addBrace(P(PACK(brace[0]), PACK(brace[1])));
	}

	/**
	 * 算符优先级比较
	 * 
	 * @param op1 1#位置值的算符的Node结构
	 * @param op2 1#位置值的算符的Node结构
	 * @return 优先级比较结果 <br>
	 *         the value 0 if d1 isnumerically equal to d2; a value less than 0 if
	 *         d1 is numerically less than d2; and a value greater than 0if d1 is
	 *         numerically greater than d2.
	 */
	public int priorityCompare(final Node op1, final Node op2) {

		return priorityCompare(op1.getName(), op2.getName());
	}

	/**
	 * 算符优先级的比较
	 * 
	 * @param op1 1#位置值的算符
	 * @param op2 1#位置值的算符
	 * @return 优先级比较结果 <br>
	 *         the value 0 if d1 isnumerically equal to d2; a value less than 0 if
	 *         d1 is numerically less than d2; and a value greater than 0if d1 is
	 *         numerically greater than d2.
	 */
	public int priorityCompare(final String op1, final String op2) {

		final var n = localPriorities.keys().stream().map(localPriorities::get) // 提取最大的优先级数
				.collect(Collectors.summarizingDouble(e -> IRecord.obj2dbl().apply(e))).getMax() + 1;

		return Double.compare(priorityOf(op1, n), priorityOf(op2, n));
	}

	/**
	 * 提取算符优先级
	 * 
	 * @param name         算符名称
	 * @param defaultValue 优先级的默认值
	 * @return 算符优先级
	 */
	public Double priorityOf(final String name, final Number defaultValue) {

		return localPriorities.dbl(name, defaultValue.doubleValue());
	}

	/**
	 * 提取（分组括号的）左半边
	 * 
	 * @param rightPart 节点符号（右半边）
	 * @return 与右边符号对应的左半边符号，如果分组括号不存在返回 Optional.empty
	 */
	public Optional<Node> getLeftPart(final Node rightPart) {

		return Optional.ofNullable(this.parentheses.get(rightPart.getName())).map(e -> e._1);
	}

	/**
	 * 数据 统一 结构化 处理 即 统一使用 ArrayList 来管理数据
	 * 
	 * @param collection 数据结合
	 * @return 列表结构的数据集合
	 */
	final static List<Node> enclose(final Collection<Node> collection) {

		return new ArrayList<Node>(collection);
	}

	/**
	 * 判断两个node的名字是否相等
	 */
	final static BiPredicate<Node, Node> name_eq = (a, b) -> a != null && b != null && a.getName().equals(b.getName()); // 名字相等
	final static Node LEFT_PARENT = PACK(TOKEN("(")); // 左括号 分组开始标记
	final static Node RIGHT_PARENT = PACK(TOKEN(")")); // 有括号 分局结束标记

	/**
	 * 本地算符注册表
	 */
	protected Map<String, BinaryOp<?, ?>> localOps = new HashMap<>();

	/**
	 * 本地算符优先级注册表 {（String,Number) }
	 */
	protected IRecord localPriorities = REC();

	/**
	 * 层级标识的注册 <br>
	 * 
	 * 分组算符（括号算符）的注册，以右半边儿的名称为key<br>
	 * 分组算符 是 触发 规约的重要条件，是 构造层级结构的 基本语言设施 ，默认使用 圆括号"(",")" 作为分组 算符。<br>
	 * <br>
	 * 可以根据 设计需要 注册 自定义的的 分组算符，比如 ("[","]"),("{","}")。<br>
	 * 分组只是一种 规约标记结构,一般仅用于 提示 语法分析器(handle), 何时进行规约(reduce) .<br>
	 * 即 分组符号 一般不会 出现在 语法树之中，<br>
	 * 但是 当 leftPart 存在明确定义的触发,即 把leftPart 作为算符 注册到了 算符注册表缓存，<br>
	 * 比如：localOps 或 本身就是位于 Ops.registry 中的算符 则leftPart 将会给予保留，但是rightPart依旧给予过滤<br>
	 * 比如：对于 "rec[x]" 这样语句 ，在 注册了 ("[","]") 作为分组算符， 并且 到了分量提取二元算符 "[" 的情况下, <br>
	 * 解析出的语法树AST（Abstract Syntax Tree，AST） 是这样的前缀式结构: ('[',rec,x), 即 "]" 被过滤掉了 <br>
	 * <br>
	 * 分组算符 是一个 二元组组 (leftPart,rightPart):<br>
	 * 1) rightPart 是 触发分组回溯的 条件 <br>
	 * 2) 回溯 将以 leftPart 为终止条件。 <br>
	 * 借此 将 leftPart, 与 rightPart 之间的内容 : 规约批次项集缓存 batchTerms<br>
	 * 给予 规约处理，这就是 分组处理的 机制。
	 */
	protected Map<String, Tuple2<Node, Node>> parentheses = new HashMap<>(); // 括号结构
}