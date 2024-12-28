package gbench.util.math.algebra.symbol;

import static gbench.util.math.algebra.op.BinaryOp.*;
import static gbench.util.math.algebra.op.Comma.COMMA_TEST;
import static gbench.util.math.algebra.op.Ops.*;
import static gbench.util.math.algebra.tuple.MyRecord.REC;
import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.op.Ops;
import gbench.util.math.algebra.op.Token;
import gbench.util.math.algebra.tuple.IRecord;
import gbench.util.math.algebra.tuple.Tuple2;

/**
 * 数据糖衣结构 (语法节点)
 * 
 * 语法树的节点, 即 数据糖衣 是 一种 包装 <br>
 * 包装 就像 刷 为 药物 刷一层糖衣 一样,Node 以后的 target 会 更加方便 操作。<br>
 * 因为 Node 提供一系列 对 被包裹值 的 的 便捷 访问与操作函数 <br>
 * 
 * Node 的 本质就是 一个 value 的 wrapper, <br>
 * 为 value 提供一个 方法壳, 以方便对 对value的 处理 <br>
 * 对于 语法树 它的内容 主要分为： <br>
 * 1) op 运算节点 <br>
 * 2) token 词素节点 <br>
 * 
 * @author gbench
 *
 */
public class Node {

	/**
	 * 数据糖衣
	 * 
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public Node(final BinaryOp<?, ?> value) {
		if (debug) {
			if (value == null) {
				try {
					throw new Exception(println("尝试 Node 包装 一个 空值"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} // if

		this.value = (BinaryOp<Object, Object>) value;
	}

	/**
	 * 是否为空值结构
	 * 
	 * @return
	 */
	public boolean isNull() {
		return this.value == null;
	}

	/**
	 * 是否是运算节点,<br>
	 * 此时的 Op 是一种 运算意义 而 不是结构， 也就是 <br>
	 * 尽管 value 是 一种 BinaryOp 但是 由于 它没有 运算功能 <br>
	 * 比如 isConstant的 常量的 情况, 它就 不是 运算意义下得Op,它 也是 要返回 false 的
	 * 
	 * @return 是否是一种运算意义上的Op
	 */
	public boolean isOp() {
		return this.isNull() ? false : !this.value.isConstant();
	}

	/**
	 * 是否词素节点 ( null 被视为 一种 token )
	 * 
	 * @return 不是Op就是Token
	 */
	public boolean isToken() {
		return !this.isOp();
	}

	/**
	 * 获取Node的名称 对于 op 类型的 Node name 就是 op 算符名 <br>
	 * 对于 token 类型的 name, name 就是 其Record的结构的 value字段 的字符串表示。 <br>
	 * 
	 * @return Node 的名字
	 */
	public String getName() {
		return this.value.getName();
	}

	/**
	 * 获取内容物 即 被包裹的值
	 * 
	 * @return Object
	 */
	public BinaryOp<Object, Object> getValue() {
		return this.value;
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变 <br>
	 * 尝试接续解包的次数为100
	 * 
	 * @return 脱壳后的对象
	 */
	public Object unpack() {
		return UNPACK(this);
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变
	 * 
	 * @param n 尝试继续解包的次数
	 * @return 脱壳后的对象
	 */
	public Object unpack(final int n) {
		return UNPACK(this, n);
	}

	/**
	 * 提取运算符
	 * 
	 * @return BinaryOp
	 */
	public BinaryOp<Object, Object> getOp() {
		return this.isOp() ? (BinaryOp<Object, Object>) value : null;
	}

	/**
	 * 生成词素对象
	 * 
	 * @return Token
	 */
	public Token getToken() {
		return this.isToken() ? (Token) this.value : null;
	}

	/**
	 * 值的变换
	 * 
	 * @param mapper 数值的变换 op0 -&gt; op1
	 * 
	 * @return 变换后的 Node
	 */
	public Node fmap(final Function<BinaryOp<?, ?>, BinaryOp<?, ?>> mapper) {
		return new Node(mapper.apply(this.value));
	}

	/**
	 * 结构化简
	 * 
	 * @return Node
	 */
	public Node simplify() {
		return this.simplify(false);
	}

	/**
	 * 结构化简
	 * 
	 * @param flag 是否哟先运行evaluate后化简,false:不运行,true:运行,<br>
	 *             运行evaluate后化简会将数学常数比如pi,e等转换为系统默认的浮点数。比如: <br>
	 *             1+pi,会被替换成 4.141592653589793 而不在保留 pi常数
	 * @return Node
	 */
	public Node simplify(final boolean flag) {
		final var o = this.isOp() ? this.getOp().simplify(flag) : this.getToken(); // 只对op结构进行化简
		return new Node(o);
	}

	/**
	 * 值的变换
	 * 
	 * @param mapper 数值的变换 op0 -&gt; op1
	 * 
	 * @return 变换后的 Node
	 */
	public Node fmap2(final Function<Optional<BinaryOp<?, ?>>, BinaryOp<?, ?>> mapper) {
		return new Node(mapper.apply(Optional.ofNullable(this.value)));
	}

	/**
	 * 值变换
	 * 
	 * @param <T>    结果类型
	 * @param mapper 变换函数 bop -> t
	 * @return T 类型的结果
	 */
	public <T> T map(final Function<BinaryOp<?, ?>, T> mapper) {
		return mapper.apply(this.value);
	}

	/**
	 * 节点结算
	 * 
	 * @return 节点计算的结果，数值 或者 BinaryOp的 表达式
	 */
	public Object evaluate() {
		return this.evaluate(REC());
	}

	/**
	 * 节点计算
	 * 
	 * @param bindings 变量参数的数据绑定, 键,值序列:key0,value0,key1,value1,....
	 * @return 节点计算的结果，数值 或者 BinaryOp的 表达式
	 */
	public Object evaluate(final Object... bindings) {
		return this.evaluate(REC(bindings));
	}

	/**
	 * 节点计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 节点计算的结果，数值 或者 BinaryOp的 表达式
	 */
	public Object evaluate(final IRecord bindings) {
		return this.evaluate(bindings.toMap());
	}

	/**
	 * 节点计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @param tclass   占位符类型
	 * @return 节点计算的结果，数值 或者 BinaryOp的 表达式
	 */
	@SuppressWarnings("unchecked")
	public <X> X evaluate(final IRecord bindings, final Class<X> tclass) {
		return (X) this.evaluate(bindings.toMap());
	}

	/**
	 * 节点计算
	 * 
	 * @param <X>      结果类型
	 * @param bindings 变量参数的数据绑定
	 * @param x        默认值
	 * @return x 类型的结果
	 */
	@SuppressWarnings("unchecked")
	public <X> X evaluate(final IRecord bindings, final X x) {
		X _x = x;
		try {
			_x = (X) this.evaluate(bindings.toMap());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return _x;
	}

	/**
	 * 节点计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 节点计算后的结果，数值 或者 BinaryOp的 表达式
	 */
	public Object evaluate(final Map<String, Object> bindings) {
		return this.getValue().evaluate(bindings);
	}

	/**
	 * 数值计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 数值数据
	 */
	public Double eval(final IRecord bindings) {
		return this.eval(bindings.toMap());
	}

	/**
	 * 数值计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 数值数据
	 */
	public Double eval(final Map<String, Object> bindings) {
		return IRecord.obj2dbl(0d).apply(this.evaluate(bindings));
	}

	/**
	 * 数值计算
	 * 
	 * @param bindings 变量参数的数据绑的 kvs 键,值序列, key0,value0,key1,value1,....
	 * @return 数值数据
	 */
	public Double eval(final Object... bindings) {
		return this.eval(REC(bindings));
	}

	/**
	 * 按照x变量进行方法求导
	 * 
	 * @return 求导之后的语法树结构
	 */
	public Node derivate() {
		return this.derivate("x");
	}

	/**
	 * 按照x变量进行方法求导(derivate()的别名)
	 * 
	 * @return 求导之后的语法树结构
	 */
	public Node dx() {
		return this.derivate("x");
	}

	/**
	 * 绑定值函数: fb 是 function Boolean 的缩写 <br>
	 * fb 是 function Boolean 的缩写 代表 结果是返回一个 Predicate&lt;IRecord&gt; <br>
	 * 相当于 bindings-&gt;predicate.apply(this.evaluate(bindings)); <br>
	 * 
	 * @param <T>       predicate 的 参数类型
	 * @param predicate 值判断谓词 t -&gt; boolean 值类型判断
	 * @return bindings:变量参数的数据绑定 -&gt; boolean
	 */
	@SuppressWarnings("unchecked")
	public <T> Predicate<IRecord> fb(final Predicate<T> predicate) {
		return bindings -> predicate.test((T) this.evaluate(bindings));
	}

	/**
	 * 绑定值函数:Fx 是 Function X vlaue 的缩写<br>
	 * fx 是 function X value 的缩写 代表 结果是返回一个 Function&lt;T,U&gt; <br>
	 * 相当于 bindings-&gt;fx.apply(this.evaluate(bindings)); <br>
	 * 
	 * @param <T> fx 的 参数类型
	 * @param <U> fx 的 结果类型
	 * @param fx  计算规则函数 t -&gt; u 值类型判断
	 * @return bindings:变量参数的数据绑定 -&gt; t
	 */
	@SuppressWarnings("unchecked")
	public <T, U> Function<IRecord, U> fx(final Function<T, U> fx) {
		return bindings -> fx.apply((T) this.evaluate(bindings));
	}

	/**
	 * 绑定值函数: fb 是 function Boolean 的缩写 <br>
	 * fb 是 function Boolean 的缩写 代表 结果是返回一个 Predicate&lt;IRecord&gt; <br>
	 * 相当于 bindings->predicate.apply(this.evaluate(bindings)); <br>
	 * 
	 * @param <T>       predicate 的 参数类型
	 * @param predicate 值判断谓词 t -> boolean 值类型判断
	 * @return bindings:变量参数的数据绑定 -> boolean
	 */
	@SuppressWarnings("unchecked")
	public <T> Predicate<Map<String, Object>> fb2(final Predicate<T> predicate) {
		return bindings -> predicate.test((T) this.evaluate(bindings));
	}

	/**
	 * 绑定值函数:Fx 是 Function X vlaue 的缩写<br>
	 * fx 是 function X value 的缩写 代表 结果是返回一个 Function&lt;T,U&gt; <br>
	 * 相当于 bindings-&gt;fx.apply(this.evaluate(bindings)); <br>
	 * 
	 * @param <T> fx 的 参数类型
	 * @param <U> fx 的 结果类型
	 * @param fx  计算规则函数 t -&gt; u 值类型判断
	 * @return bindings:变量参数的数据绑定 -&gt; t
	 */
	@SuppressWarnings("unchecked")
	public <T, U> Function<Map<String, Object>, U> fx2(final Function<T, U> fx) {
		return bindings -> fx.apply((T) this.evaluate(bindings));
	}

	/**
	 * 导数是量的变化的结构。(常量的变化结构为零,即TOKEN(0)，表示永恒的不变) <br>
	 * 
	 * 结构求导 (尚未把求导规则补充完成) <br>
	 * 需要注意: <br>
	 * 1) 求导是一种结构运算,是一种结构变换，从一种结构 测定出 它的 变化能力， <br>
	 * 2）描述变化(极限式变化率)能力方式就是用另一种结构来表达（也就是解析式） <br>
	 * 3）于是 对于常量结构 比如:"0" 的导数就是TOKEN(0)。这用"0"表示一种结构描述而不是数值0 <br>
	 * 4）TOKEN(0) 是 "0" 这种常量结构的变化率结构 也就是0 <br>
	 * 
	 * @param variable 求导变量
	 * @return 求导之后的计算结构
	 */
	public Node derivate(final String variable) {
		if (this.isToken()) { // 词素类型节点
			if (predicate_var.test(this.value)) { // 变量检测
				return variable.equals(this.getName()) ? PACK(1) : PACK(0);
			} else if (predicate_const.test(this.value)) { // 常量检测
				return PACK(0);
			} else {
				return null;
			}
		} else { // 运算类型节点
			final var theOp = this.getOp(); // 当前的操作符号
			if (!theOp.hasLeaf(variable)) { // 算符的叶端节点不包含微分变量则返回0,即不会改变
				return PACK(Ops.TOKEN(0));
			} // if

			final var name = this.getName(); // 获取运算名称
			final var nodes = theOp.getArgS().limit(theOp.getAry()).map(Node::PACK).toArray(Node[]::new); // 提取方法参数并Node糖衣化
			final var xnode = nodes.length < 1 ? null : nodes[0]; // 第一个参数的node
			final var ynode = nodes.length < 2 ? null : nodes[1]; // 第二个参数的node
			final var x = xnode == null ? null : xnode.unpack(); // 第一个参数
			final var y = ynode == null ? null : ynode.unpack(); // 第二个参数
			final Supplier<IRecord> comma_args_adjust = () -> { // 逗号表达式的情况
				final var rb = IRecord.rb("xnode,ynode,x,y"); // 基本数据的命名结构
				if (y == null && COMMA_TEST(xnode.getOp().getName())) { // xnode 是一个逗号表达式
					final var comma_args = xnode.getOp().flatArgS().map(Node::UNPACK).toArray(Object[]::new); // 提取逗号表达式的参数列表
					final var _x = comma_args[0]; // comma 的一号位置元素
					final var _y = comma_args[1]; // comma 的二号位置元素
					final var _xnode = PACK(_x);
					final var _ynode = PACK(_y);
					return rb.get(_xnode, _ynode, _x, _y);
				} else { // 非逗号表达式 不予 调整参数 原样返回
					return rb.get(xnode, ynode, x, y);
				} // if
			}; // comma_args_adjust
			final Function<Node, Node> derivate_rule = prototype -> { // 求导规则
				if (xnode != null && xnode.isOp()) { // 复合函数情况
					return PACK(MUL(prototype.unpack(), xnode.derivate(variable).unpack()));
				} else { // 基本函数情况
					return prototype;
				}
			};

			switch (name) {
			case "+":
			case "-": { // 加减法规则 +(x,y) 或 -(x,y) 就 x+y 或 x-y
				final var node = Stream.of(nodes).filter(Objects::nonNull) // 去除空值节点
						.map(e -> {
							final var p = e.derivate(variable);
							return p;
						}).reduce((node1, node2) -> {
							return PACK(theOp.duplicate().compose(node1.unpack(), node2.unpack()));
						}).orElse(null);
				return node;
			}
			case "*": { // 乘法运算规则 *(x,y) 即 x*y
				final var op = ADD(MUL(xnode.derivate(variable).unpack(), y), // x'*y
						MUL(x, ynode.derivate(variable).unpack())); // x*y'
				return PACK(op);
			}
			case "/": { // 除法运算规则 /(x,y) 即 x/y
				final var op = DIV(
						MINUS(MUL(xnode.derivate(variable).unpack(), ynode.unpack()),
								MUL(xnode.unpack(), ynode.derivate(variable).unpack())),
						// 为了避免 dumpAST出现混乱[会出现兄弟排行错误,即一个元素即是老大也是老二]，
						// 兄弟节点的元素不要使用相同的对象结构。所以 这里第二个ynode给予复制
						MUL(ynode.unpack(), ynode.duplicate().unpack()));
				return PACK(op);
			}
			case "identity": { // 常量函数
				return xnode.derivate(variable);
			}
			case "neg": { // 取反函数 neg(x)
				return PACK(MINUS("0", xnode.derivate(variable).unpack()));
			}
			case "sin": { // 正弦函数 sin(x)
				return derivate_rule.apply(PACK(COS(x)));
			}
			case "cos": { // 余弦函数 cos(x)
				return derivate_rule.apply(PACK(NEG(SIN(x))));
			}
			case "tan": { // 正切函数 tan(x)
				return derivate_rule.apply(PACK(SQUARE(SEC(x))));
			}
			case "cot": { // 余切函数 cot(x)
				return derivate_rule.apply(PACK(NEG(SQUARE(CSC(x)))));
			}
			case "sec": { // 正割函数 sec(x)
				return derivate_rule.apply(PACK(MUL(SEC(x), TAN(x))));
			}
			case "csc": { // 余割函数 csc(x)
				return derivate_rule.apply(PACK(NEG(MUL(CSC(x), COT(x)))));
			}
			case "arcsin": { // 反正弦函数 arcsin(x)
				return derivate_rule.apply(PACK(DIV(1, SQRT(MINUS(1, POW(x, 2))))));
			}
			case "arccos": { // 反余弦函数 arccos(x)
				return derivate_rule.apply(PACK(NEG(DIV(1, SQRT(MINUS(1, POW(x, 2)))))));
			}
			case "arctan": { // 反正切函数 arctan(x)
				return derivate_rule.apply(PACK(DIV(1, ADD(1, POW(x, 2)))));
			}
			case "arccot": { // 反余切函数 arccot(x)
				return derivate_rule.apply(PACK(NEG(DIV(1, ADD(1, POW(x, 2))))));
			}
			case "exp": { // 指数函数 exp(x)
				return derivate_rule.apply(PACK(EXP(x)));
			}
			case "expa": { // 指数函数 exp(x,y), x^y
				final var _args = comma_args_adjust.get(); // 参数调整 a^x
				final var _a = _args.get("x"); // 底数 命名为 数学表达习惯
				final var _x = _args.get("y"); // 底数 命名为 数学表达习惯
				final var _xnode = _args.get("ynode", Node.class); // 微分变量 命名为 数学表达习惯
				if (_xnode != null && _xnode.isOp()) { // 复合函数情况
					return PACK(MUL(MUL(LN(_a), EXP(_x)), _xnode.derivate(variable).unpack()));
				} else {
					return PACK(MUL(LN(_a), EXP(_x)));
				}
			}
			case "ln": { // 自然对数函数 ln(x)
				return derivate_rule.apply(PACK(DIV(1, x)));
			}
			case "log": { // 对数函数 log(x,y) 其中 x 常数底数, y 是变量
				final var _args = comma_args_adjust.get(); // 参数调整 log(a,x)
				final var _a = _args.get("x"); // 底数 命名为 数学表达习惯
				final var _x = _args.get("y"); // 微分变量 命名为 数学表达习惯
				final var _xnode = _args.get("ynode", Node.class); // 底数

				if (_xnode != null && _xnode.isOp()) { // 复合函数情况
					return PACK(MUL(DIV(1, MUL(LN(_a), _x)), _xnode.derivate(variable).unpack()));
				} else { // 基本函数情况
					return PACK(DIV(1, MUL(LN(_a), _x)));
				} // if
			}
			case "sqrt": { // 求平方根函数 sqrt(x)
				return derivate_rule.apply(PACK(MUL(1 / 2d, POW(x, -1 / 2d))));
			}
			case "square": { // 平方根函数 square(x)
				return derivate_rule.apply(PACK(MUL(2d, x)));
			}
			case "^":
			case "pow": { // 指数函数 pow (x,y), 由于 pow 函数 是采用逗号表达式来提供参数操作数的，所以这里需要对操作数进行调整
				final var _args = comma_args_adjust.get();
				final var _x = UNPACK(_args.get("x"));
				final var _y = UNPACK(_args.get("y"));
				final var _xnode = _args.get("xnode", Node.class);

				if (_xnode != null && _xnode.isOp()) { // 复合函数情况
					return PACK(MUL(_y, MUL(POW(_x, MINUS(_y, 1)), _xnode.derivate(variable).unpack())));
				} else { // 基本函数情况
					return PACK(MUL(_y, POW(_x, MINUS(_y, 1))));
				}
			}
			default: { // 其他函数不予处理
				return null;
			}
			} // switch
		}
	}

	/**
	 * 语法树dump <br>
	 * 
	 * 所谓语法树就是一个计算结构：<br>
	 * 类似于如下结构： <br>
	 * (5*pow(2,5)*tan(45.6/255-cos(4/6))) <br>
	 * 
	 * * ----&gt;
	 * (*,5,(*,(pow,(',',2,5),null),(tan,(-,(/,45.6,255),(cos,(/,4,6)))))) <br>
	 * | 5 <br>
	 * | * ----&gt; (*,(pow,(',',2,5),null),(tan,(-,(/,45.6,255),(cos,(/,4,6)))))
	 * <br>
	 * | | pow ----&gt; (pow,(',',2,5),null) <br>
	 * | | | , ----&gt; (',',2,5) <br>
	 * | | | | 2 <br>
	 * | | | | 5 <br>
	 * | | | null <br>
	 * | | tan ----&gt; (tan,(-,(/,45.6,255),(cos,(/,4,6)))) <br>
	 * | | | - ----&gt; (-,(/,45.6,255),(cos,(/,4,6))) <br>
	 * | | | | / ----&gt; (/,45.6,255) <br>
	 * | | | | | 45.6 <br>
	 * | | | | | 255 <br>
	 * | | | | cos ----&gt; (cos,(/,4,6)) <br>
	 * | | | | | / ----&gt; (/,4,6) <br>
	 * | | | | | | 4 <br>
	 * | | | | | | 6 <br>
	 */
	public String dumpAST() {

		/**
		 * 由于 采用深度遍历，所以 一个 阶层为 i 的节点 路径为 [0,1,2,...,i-1,i]中个对应的 值序列 <br>
		 * 于是 父节点 利索应当为 i-1 所对应键值
		 */
		final var level2ops = new HashMap<Integer, BinaryOp<?, ?>>(); // 阶层对节点对象,阶层从0开始
		final var buffer = new StringBuffer(); // 数据行缓存由writeln写入
		final Consumer<String> writeln = line -> { // 数据行的写入到缓存buffer
			buffer.append(line);
			buffer.append("\n");
		};

		/**
		 * Tuple2<List<Integer>>是 处理例程routine 的数据结构 <br>
		 * routine处理例程 (rank:家族排行,task:处理过程Runnable), 根据 家族排行rank 进行依次排行,<br>
		 * 执行时,按照这样的阶层逐层递进的 方式 进行 深度遍历 (展开) : 0 > 0.1 > 0.2 > 1 > 1.1 > 1.1.1 > 1.2 >
		 * ... routines_executor 把处于同一层级的 例程 统一到一起 并 根据 其 例程的 家族序号 即 rank 按照顺序的 依次执行
		 * <br>
		 * 例子程序,用 Runnable 结构来表示.
		 */
		final Consumer<List<Tuple2<List<Integer>, Runnable>>> routines_executor = routines -> { // routine 例程
			routines.stream().sorted((a, b) -> {
				final var key1 = a._1(); // 第一元素的排序key
				final var key2 = b._1(); // 第二元素的排序key
				final var opt = Stream.iterate(0, i -> i + 1) // 生一个从0开始的自然数序列
						.limit(Math.min(key1.size(), key2.size())) // 提取公共长度
						.map(i -> key1.get(i).compareTo(key2.get(i))) // 共同长度时候按照对于位置的元素大小进行比较
						.filter(e -> e != 0) // 判断是否出现大小差异
						.findFirst(); // 获取比较结果
				if (opt.isEmpty()) { // 公共长度部分完全相同时按照数据长度进行比较，公共长度比较无效
					return key1.size() - key2.size();
				} else { // 公共长度比较有效
					return opt.get();
				}
			}).map(e -> e._2()) // 提取执行routine:Runnalbe
					.forEach(Runnable::run); // 依次运行routine

			routines.clear(); // 回调数据清空
		}; // subroutines_executor
		final var routines = new ArrayList<Tuple2<List<Integer>, Runnable>>(); // routine:执行例程的缓存
		final Function<Integer, List<Integer>> pathcode_eval = (level) -> { // 计算level阶层节点的pathcode
			final var ancestors = Stream.iterate(0, i -> i <= level, i -> i + 1).map(level2ops::get)
					.collect(Collectors.toList());// 提取祖先列表
			final var width = 2; // 滑动窗口的宽度
			final var ranks = Stream.iterate(0, i -> i <= ancestors.size() - width, i -> i + 1) // 构造以width为窗口宽度的sliding
					.map(i -> ancestors.subList(i, i + width)) // 提取数据窗口
					.map(e -> rank_eval.apply(e.get(0), e.get(1))) // 提取家庭排行
					.collect(Collectors.toList());// 按照家庭排行生成族内排行，使用族内排行代表当前level节点的pathcode
			ranks.add(0, -1); // 用-1 代表根节点的家族排行,这样的做额原因就是避免根节点的ranks为空列表[]
			return ranks;
		};

		if (this.isToken()) { // 词素
			return this.getName();
		} else { // 算符
			/**
			 * 由于 BinaryOp的forEach 只提供 非叶子结点 Op结点 的方式遍历，无法 提供 叶子结点即数据结点的参数 信息。<br>
			 * 为了 在构造语法树 AST的时候 提供 叶子结点即数据结点的 信息，需要 在 Op结点 之间插入 叶子结点 即 数据结点 的数据。<br>
			 * 单考 虑度 数据结点 与 Op结点 在 BinaryOp的 参数结点 即 子节点中的组织顺序 并非固定，若是 在 BinaryOp的forEach遍历同时
			 * <br>
			 * 给予一次性构建AST势必造成数据显示混乱，由此 这里的 AST 构建采用 构建遍历例程 带有家族排行的 routine 历程的方式进行 静态组织。<br>
			 * 家族排行 rank(如: 1, 1.1 , 1.1.1, 1.1.2 , 1.2, 1.2.1, 1.2.2 ) 代表了 routine
			 * 执行时候的时序关系, <br>
			 * 这是一种深度优先DFS的范式
			 * 
			 * 语法树结构遍历方式采用静态结构进行组织,分为2个步骤: <br>
			 * 1) 生成&归集 处理历程 routine 到 例程容器 routines, 但不予执行. <br>
			 * 2) 对 例程容器 routines 中的 历程 进行家族排序，按照家族排行键rank,按照 深度遍历的次序 给予依次执行。<br>
			 */
			this.getOp().forEach((level, op) -> {
				final var IDENT = " | "; // 缩进字符串
				if (op.isConstant())
					return; // 对于叶子节点 使用 子程序进行 访问
				level2ops.put(level, op); // 记录节点阶层
				final var pathcode = pathcode_eval.apply(level);
				routines.add(P(pathcode, (Runnable) () -> { // 算符节点 的 数据 dump : 创建一个执行例程 用于在当前level打印op的信息
					final var line = IDENT.repeat(level) + op.getName() + " \t ----> " + op; // 算符节点的 信息条目
					writeln.accept(line);
				}));

				op.getArgS() // 子节点的处理
						.limit(op.getAry()) // 提取有效参数
						.map(kvp_int()) // 增加节点的兄弟排行
						.filter(kvp -> predicate_token.test(kvp._2())) // 提取叶子节点
						.map(kvp -> { // 生成必要的叶子节点的处理例程:subroutine
							final List<Integer> child_pathcode = new ArrayList<Integer>(pathcode);
							child_pathcode.add(kvp._1()); // 补充上当前节点的家族排行
							// 数据节点 的 数据 dump : 创建一个执行例程 用于在下一个level打印叶子节点信息
							return P(child_pathcode, (Runnable) () -> {
								final var line = IDENT.repeat(level + 1) + kvp._2(); // 叶子节点的 信息条目
								writeln.accept(line);
							});
						}).forEach(routines::add); // 添加处理例程
			}); // forEach

			// 把routines根据家族排行的次序给予依次执行
			routines_executor.accept(routines);
		} // if

		return buffer.toString();
	}

	/**
	 * 构造一个复制品 <br>
	 * 调用 值的 duplicate 方法来完成值复制并重新创建Node,注意:值的复制办法是采用的是浅层复制
	 * 
	 * @return 复制品,
	 */
	public Node duplicate() {
		final var _value = value.duplicate();
		return PACK(_value);
	}

	/**
	 * hashCode
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { Node.class, this.getValue() });
	}

	/**
	 * equals
	 */
	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof Node another) {
			return Objects.equals(this.getValue(), another.getValue());
		} else {
			return false;
		}

	}

	/**
	 * 数据格式化
	 */
	@Override
	public String toString() {
		if (this.value == null)
			return "Null";
		return this.isOp() ? this.value.toString() : this.getName();
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变 <br>
	 * 尝试接续解包的次数为100
	 * 
	 * @param target 目标对象
	 * @return 脱壳后的对象
	 */
	public static Object UNPACK(final Object target) {
		return UNPACK(target, 100);
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变
	 * 
	 * @param target 目标对象
	 * @param n      尝试继续解包的次数
	 * @return 脱壳后的对象
	 */
	public static Object UNPACK(final Object target, final int n) {
		final var _t = target instanceof Node ? ((Node) target).value : target; // 脱壳
		return (_t instanceof Node && n > 0) ? UNPACK(_t, n - 1) : _t;
	}

	/**
	 * 视图包装 <br>
	 * 如果已经包装之后则不予包装，而是 直接返回, 否则 Node 包装 target
	 * 
	 * @param target 被包装对象
	 * @return Node 包装后的 target
	 */
	public static Node PACK(final Object target) {
		if (target == null) {

			if (debug) {
				try {
					throw new Exception(println("尝试 pack 一个空值"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} // if

			return NODE(null);
		} else if (target instanceof BinaryOp<?, ?> _target) {
			return NODE(_target);
		} else if (target instanceof Node) {
			return (Node) target;
		} else { // 其他类型的值 一律 视为 Token
			final var token = Optional.ofNullable(target) //
					.map(BinaryOp::dbl).map(Ops::TOKEN).orElse(TOKEN(String.valueOf(target)));
			return NODE(token);
		} // if
	}

	/**
	 * 把 value 值 包装一层Node <br>
	 * 包装 就像 刷 为 药物 刷一层糖衣 一样,Node 以后的 target 会 更加方便 操作。<br>
	 * 因为 Node 提供一系列 对 被包裹值 的 的 便捷 访问与操作函数 <br>
	 * 
	 * @param target 被包装的对象
	 * @return Node 包装后的 target
	 */
	public static Node NODE(final BinaryOp<?, ?> target) {
		return new Node(target);
	}

	/**
	 * 把 value 值 包装一层Node <br>
	 * 包装 就像 刷 为 药物 刷一层糖衣 一样,Node 以后的 target 会 更加方便 操作。<br>
	 * 因为 Node 提供一系列 对 被包裹值 的 的 便捷 访问与操作函数 <br>
	 * 
	 * @param target 被包装的对象
	 * @return Node 包装后的 target
	 */
	public static Node of(final BinaryOp<?, ?> target) {
		return NODE(target);
	}

	private BinaryOp<Object, Object> value; // 被包裹的值
}