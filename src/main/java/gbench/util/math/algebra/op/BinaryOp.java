package gbench.util.math.algebra.op;

import static gbench.util.math.algebra.op.Comma.COMMA_TEST;
import static gbench.util.math.algebra.op.Ops.*;
import static gbench.util.math.algebra.symbol.Node.*;
import static gbench.util.math.algebra.tuple.MyRecord.REC;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import gbench.util.math.algebra.symbol.ISymboLab;
import gbench.util.math.algebra.symbol.Node;
import gbench.util.math.algebra.tuple.IRecord;
import gbench.util.math.algebra.tuple.Tuple2;

/**
 * 二元算符 <br>
 * 
 * 需要 注意 结构 不是 功能, 结构（或者说形式） 是一种组织方式，<br>
 * 其内容（这里是说它的组织办法）是 并不代表 什么意义，<br>
 * 意义 是一种 映射关系 a-&gt;b, 它 是 把一个 对象a 与 另一个 对象b 之间 进行的 关联绑定（约定， 或是约定俗成的习惯，或是
 * 理解认识，也就是 意识&amp;文化观念上 对这 形式/结构 的约束和限制，即: XX形式 代表了 YY意义）, 换句话说：意义 的 常见解释 就是 把
 * 结构 -&gt; 实现出 特定目的 的 功能用途，这里的 ‘-&gt;’ 读作 映射到 . <br>
 * 所以 意义 是一种 在 什么情况下 使用 某种（些）结构 的 习惯约定。 它由设计 决定。 <br>
 * 就像 房子 可以 使用 砖木结构 也可以 钢筋结构 两种不同的 结构 却常常用来完成 同样的 功能 <br>
 * 对应相同的意义。同样 的 钢筋结构(这里强调类别忽略具体细节) 不仅可以 建房子 还可以 造大船, 也就是 同样的 结构 可以用于 实现不同的 功能 即
 * 结构 和 功能 是一种 多对多 的 关系。 <br>
 * 意义 强调 某种 价值、思想、意识 上的 认识&amp;评估，而 结构 则 强调 某种类 的 资源，材料，素材，物质层面 的表达。<br>
 * 即 用结构（也就是形式，物质）表达意义（意识），也可以说 物质决定意识，意识反作用于物质，这里的反作用是指
 * 加入主观意识即人为上的故意和专门化的努力,比如：优化和设计，在 意义的特定维度，也就是主观期望 或是 业务方面的需求诉求上对物质形式进行 符合当前实际的
 * A&amp;A(实事) 和 E&amp;E（求是，效率效益）的 替换和选择. <br>
 * 注：A&amp;A:Affordable&amp;Attainable,E&amp;E:Effectiveness&amp;Efficiency) 。
 * 
 * BinaryOp 不仅可以 用来 当做 运算对象 来使用 还可以 当做 存储结构 即 数据节点(二叉树的节点来使用) <br>
 * 比如：求导 操作 的时候 就是把 BinaryOp 当作 数据节点来 用的。<br>
 * 还有 Token 就是 把 BinaryOp 当作 一个 单值 容器来使用的 <br>
 * 结构 与 意义 之间的 关系 是 非常灵活的 要在 实际 使用 中 灵活把握 同时 需要 区分 结构概念 和 意义概念（观念） <br>
 * 比如 运算Op结构 和 运算Op意义 虽然都叫 运算Op 但还是不一样的 <br>
 * 1) 运算Op结构: 强调 运算Op 是如何 构造的，即 由那些 基本的 小构件，<br>
 * 用 什么 方式 联系到一起，哪些是 动态可变&amp;可调的,哪些是 静态固定&amp;保持不变的<br>
 * 2) 运算Op意义：强调 输入Input 经过 一系列的 黑盒 运算 之后 变成了 怎样的 输出Ouput, 具体是如何计算的<br>
 * 意义是不关心的，这也是说明它是黑盒的原因,相反 意义 对 Input 和 Output 到是 极为 有兴趣， <br>
 * 即 什么样 Input 可以 得到 Output, Output 出现的时机，形态，方式，和 性质有哪些 ， 这就像 一位 美食家 <br>
 * 他对食材和菜肴极为注重 但是 对 烹饪 却 不慎 关心（比如如何颠大勺,切黄瓜之类 的 厨艺功夫他是不理会的) 。<br>
 * 所以 意义 是一种 强调 结果 的 概念 而 结构 是一种强调过程 或者说 机制 的概念。
 * 
 * 代数(符号计算)的 基本算符 <br>
 * 灵感来源于 LISP 的 CONS,CAR,CDR 的设计 <br>
 * <br>
 * LISP 的基本 逻辑就是 世界的 一切都是 列表(LIST) 结构,<br>
 * 列表以 CONS 为基本单元, 如果 一个不行，那就 多来几个，特别是 递归一下 的 多来几个<br>
 * (LIST是一个 以CONS:(CAR,CDR) 为基本单元的结构,CONS通过CDR进行彼此串联，<br>
 * 数据存放在CAR中或是由CAR指向)<br>
 * 
 * <br>
 * 所谓 CONS 是一个列表元素结构, <br>
 * 简单说就是一个二元组:第一个元素是 CAR,第二个元素是 CDR. <br>
 * 其中 CAR,CDR 可以任何类型。并且允许递归嵌套。<br>
 * CONS = (CAR,CDR) ，特别留意一下 ，CAR,CDR 不元素而是指针,<br>
 * CAR 指向一个元素 ,CDR 执行一个尾列表,例如： <br>
 * (a,(b,(c,d))) = (CAR:a,CDR:(b,(c,d))) <br>
 * 我这里采用 Tuple2 来表 代表 CONS, _1 代表 CAR,_2 代表 CDR <br>
 * <br>
 * 多元函数可以被视为 一个 参数类型是 多元列表的 UninaryOp。 <br>
 * UninaryOp 可以被视为 第二 参数是 null 的 BinaryOp <br>
 * 所以 BinaryOp 是最为基本的类型,这就是为何 把 BinaryOp 作为第一个类的原因。 <br>
 * 多元列表：采用 逗号 表达式用来构造。 <br>
 * 所以 a + b , 可以 `+` ((a,b),null) 来表示 <br>
 * 
 * <br>
 * 
 * BinaryOp 结构性 合理理解 是 一种 类似于 抗体 的 Y型结构 (Fab,(a,b)) 而不是 把作为一种二元函数结构，因为 有时候 我们 可以
 * 仅仅使用 Fab 的值得功能，比如 把 BinaryOp <br>
 * 作为一种常量容器，即 常量函数来使用。或者 运算的 结果 就是保持 Fab 不变 <br>
 * 
 * @author gbench
 *
 * @param <T> 算符 的 第一参数类型 CAR
 * @param <U> 算符 的 第二参数类型 CDR
 */
public class BinaryOp<T, U> extends Tuple2<Object, Tuple2<T, U>> {

	/**
	 * 二元算符的结构
	 * 
	 * @param name 操作符的名称
	 * @param args 操作符的参数 (left, right)
	 */
	public BinaryOp(final Object name, final Tuple2<T, U> args) {
		super(name, args);
	}

	/**
	 * 构造一个复制品
	 * 
	 * @return BinaryOp 浅层拷贝
	 */
	public BinaryOp<T, U> duplicate() {
		return this._2 == null ? BinaryOp.of(_1, null) : BinaryOp.of(_1, this._2._1(), this._2._2());
	}

	/**
	 * 操作的名称
	 * 
	 * @return 操作名称
	 */
	public String getName() {
		return this._1 + "";
	}

	/**
	 * 默认返回null,表示采用计算引擎提供的算符优先级
	 * 
	 * @return 算符优先级,算符优先级可以为小数，使用小数的原因就是为了方便进行新的算符的插入
	 */
	public Number getPriority() {
		return null;
	}

	/**
	 * 算符的参数
	 * 
	 * @return 算符参数
	 */
	public Tuple2<T, U> getArgs() {
		return this._2;
	}

	/**
	 * 把参数组装成流式结构
	 * 
	 * @return [_1:第一个参数,_2:第二个参数]
	 */
	public Stream<Object> getArgS() {
		if (this._2 == null) {
			return Stream.of();
		} else if (this.getAry() == 1) { // 一元函数
			return Stream.of(this._2._1);
		} else if (this.getAry() == 2) { // 二元函数
			return Stream.of(this._2._1, this._2._2);
		} else { // 其他 情形 返回空流
			return Stream.of();
		} // if
	}

	/**
	 * 把把参数扁平化之后的 流
	 * 
	 * 比如 (a,(b,(c,d)),(e,(f,g))) 扁平化 之后 返回 [b,c,d,e,f,g]
	 * 
	 * @return 把把参数扁平化之后的 流
	 */
	public Stream<Object> flatArgS() {
		return flat(this.getArgs()).stream();
	}

	/**
	 * 把把参数扁平化之后的 流
	 * 
	 * 比如 (a,(b,(c,d)),(e,(f,g))) 扁平化 之后 返回 [b,c,d,e,f,g]
	 * 
	 * @return 把把参数扁平化之后的 流
	 */
	public List<BinaryOp<?, ?>> flatArgs2() {
		return this.flatArgS2().toList();
	}

	/**
	 * 把把参数扁平化之后的 流
	 * 
	 * 比如 (a,(b,(c,d)),(e,(f,g))) 扁平化 之后 返回 [b,c,d,e,f,g]
	 * 
	 * @return 把把参数扁平化之后的 流
	 */
	public Stream<BinaryOp<?, ?>> flatArgS2() {
		return this.flatArgS().map(BinaryOp::wrap);
	}

	/**
	 * 把把参数扁平化之后的 流
	 * 
	 * 比如 (a,(b,(c,d)),(e,(f,g))) 扁平化 之后 返回 [b,c,d,e,f,g]
	 * 
	 * @return 把把参数扁平化之后的 流
	 */
	public List<Object> flatArgs() {
		return this.flatArgS().toList();
	}

	/**
	 * 扁平化
	 * 
	 * @param <E>
	 * 
	 * @return 扁平化
	 */
	@SuppressWarnings("unchecked")
	public <E extends BinaryOp<?, ?>> List<E> flats() {
		return (List<E>) this.flatS().toList();
	}

	/**
	 * 扁平化
	 * 
	 * @param <E>
	 * @return 扁平化
	 */
	@SuppressWarnings("unchecked")
	public <E extends BinaryOp<?, ?>> Stream<E> flatS() {
		final Stack<Object> stack = new Stack<>();
		final var ai = new AtomicInteger(); // 终止flag
		stack.push(this);
		return Stream.iterate((E) null, e -> ai.get() < 1, e -> {
			if (!stack.isEmpty()) {
				final var p = stack.pop();
				if (p instanceof BinaryOp<?, ?> bop) {
					if (bop._2 != null) { // 尝试压入参数
						if (bop._2._2 != null)
							stack.push(bop._2._2);
						if (bop._2._1 != null)
							stack.push(bop._2._1);
					} // if
				} // if
				return p == null ? null : (E) wrap(p);
			} else { // 加入结尾标记
				ai.getAndIncrement();
				return null;
			} //
		}).skip(1);
	}

	/**
	 * 运算的元数<br>
	 * 一个参数返回为一元运算,比如 正弦函数 sin(x)<br>
	 * 两个参数为二元运算,比如幂函数pow(x,n)
	 * 
	 * @return 运算的元数
	 */
	public int getAry() {
		return 2;
	}

	/**
	 * name equals <br>
	 * 判断当前运算符号是否名称为name
	 * 
	 * @param name 待检测的名称
	 * @return 判断当前运算符号是否名称为name
	 */
	public boolean namEq(final Object name) {
		return Objects.equals(name, this._1);
	}

	/**
	 * 是否是一个常量, 常量 没有参数 只有 一个 _1 位置的值。
	 * 
	 * @return true 是常量,false 不是常量
	 */
	public boolean isConstant() {
		return this._2 == null;
	}

	/**
	 * 1#参数位置组合 <br>
	 * 
	 * 一元函数继承的时候 需要 实现 自己的compose1, 另外compose1是设计为二元函数使用的函数，<br>
	 * 一元函数请使用自己的一元compose <br>
	 * 当然 一元函数可以自己先实现compose1，<br>
	 * 然后用自己compose调用自己先实现compose1
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<X, U> compose1(final X x) {
		return this._2 == null ? null : new BinaryOp<>(this._1, P(x, this._2._2()));
	}

	/**
	 * 2#参数位置组合 <br>
	 * 
	 * 二元函数继承的时候 需要 实现 compose
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<T, X> compose2(final X x) {
		return this._2 == null ? null : new BinaryOp<>(this._1, P(this._2._1(), x));
	}

	/**
	 * 1#,2#位置的参数组合 <br>
	 * 
	 * 二元函数继承的时候 需要 实现 compose
	 * 
	 * @param <X> 1#参数的类型
	 * @param <Y> 2#参数的类型
	 * @param x   1#参数
	 * @param y   2#参数
	 * @return 新生成的组合形式
	 */
	public <X, Y> BinaryOp<X, Y> compose(final X x, final Y y) {
		return this._2 == null ? null : new BinaryOp<>(this._1, P(x, y));
	}

	/**
	 * 二元函数的求值
	 * 
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate() {
		return this.evaluate(new HashMap<>());
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param bindings 变量参数的数据绑定, 键,值序列:key0,value0,key1,value1,....
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate(final Object... bindings) {
		return this.evaluate(REC(bindings));
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate(final IRecord bindings) {
		return this.evaluate(bindings.toMap());
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate(final Map<String, Object> bindings) {
		return ISymboLab.eval(this, bindings);
	}

	/**
	 * 参数计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 二元数组[left,right]:二元函数BinaryOp情况;<br>
	 *         一元数组[arg]:一元函数UnaryOp情况;<br>
	 *         空数组[],长度为0:常量函数ConstantOp情况
	 */
	public Object[] argsEval(final Map<String, ?> bindings) {
		@SuppressWarnings("unchecked")
		final var args = this.getArgS().map(Node::PACK) // 参数包装
				// 一定要强转否则会出现对应到Node.evaluate(Object ... oo)
				.map(node -> node.evaluate((Map<String, Object>) bindings)) // 数值计算,
				.toArray(Object[]::new);
		return args;
	}

	/**
	 * 提取所有的叶端元素
	 * 
	 * @param <X> 结果类型
	 * @param sup 结果容器的构建函数
	 * @return X 结果容器
	 */
	@SuppressWarnings("unchecked")
	public <X extends Collection<Object>> X getLeaves(final Supplier<X> sup) {
		final X leafs = sup.get();
		final var stack = new Stack<Object>();

		this.getArgS().forEach(stack::push);
		while (!stack.empty()) {
			final var o = Node.UNPACK(stack.pop());
			if (o instanceof BinaryOp) {
				final var op = ((BinaryOp<Object, Object>) o);
				if (!op.isConstant()) {
					op.getLeaves(sup).forEach(leafs::add);
				} else {
					final var name = ((ConstantOp) op).getName();
					leafs.add(name);
				} // if
			} else if (Objects.nonNull(o)) {
				leafs.add(o);
			} // if
		} // while

		return leafs;
	}

	/**
	 * 强制转换成系数
	 * 
	 * @return ConstantOp
	 */
	public Optional<ConstantOp> factorOpt() {
		return BinaryOp.factorOpt(this);
	}

	/**
	 * termOpt
	 * 
	 * @return termOpt
	 */
	public Optional<BinaryOp<Object, Object>> termOpt() {
		return BinaryOp.termOpt(this);
	}

	/**
	 * 结构化简
	 * 
	 * @param flag 是否哟先运行evaluate后化简,false:不运行,true:运行,<br>
	 *             运行evaluate后化简会将数学常数比如pi,e等转换为系统默认的浮点数。比如: <br>
	 *             1+pi,会被替换成 4.141592653589793 而不在保留 pi常数
	 * @return 结构化简
	 */
	public BinaryOp<?, ?> simplify(final boolean flag) {
		final var simbol = flag ? this.evaluate() : this; // 精简符号
		return simbol instanceof BinaryOp<?, ?> bop ? ISymboLab.spf(bop) : BinaryOp.wrap(simbol);
	}

	/**
	 * 结构化简
	 * 
	 * @return 结构化简
	 */
	public BinaryOp<?, ?> simplify() {
		return this.simplify(false);
	}

	/**
	 * 是否包含有 variable 名称的叶子节点
	 * 
	 * @param variable 变量名称
	 * @return true:包含,false:包含
	 */
	@SuppressWarnings("unchecked")
	public boolean hasLeaf(final Object variable) {
		final var stack = new Stack<Object>();
		this.getArgS().forEach(stack::push);
		while (!stack.empty()) {
			final var o = Node.UNPACK(stack.pop());
			if (o instanceof BinaryOp) {
				final var binaryOp = ((BinaryOp<Object, Object>) o);
				if (binaryOp.isConstant()) {
					final var constantOp = (ConstantOp) binaryOp;
					if (constantOp.getName().equals(variable))
						return true;
				} else if (binaryOp.hasLeaf(variable)) {
					return true;
				} // if
			} else if (Objects.nonNull(o)) {
				if (o.equals(variable))
					return true;
			} // if
		} // while

		return false;
	}

	/**
	 * 强转为浮点数类型
	 * 
	 * @return Double
	 */
	public Double dbl() {
		return this.isConstant() ? dbl(this._1) : null;
	}

	/**
	 * 转换成中缀表达式
	 * 
	 * @return 中缀表达式
	 */
	@SuppressWarnings("unchecked")
	public String infix() {
		final var buffer = new StringBuffer();
		final var stack = new Stack<Object>();
		final var LEFT_PARENT = "("; // 左括号
		final var RIGHT_PARENT = ")"; // 右括号
		final var SPACE = " "; // 空白

		stack.push(this);
		while (!stack.empty()) {
			final var p = stack.pop();
			if (p instanceof BinaryOp) {
				final var binaryOp = (BinaryOp<Object, Object>) p;
				final var nary = binaryOp.getAry(); // 算符的参数个数
				final var opname = binaryOp.getName(); // 算符名称
				final Function<Object, Stream<Object>> arg_formatter = obj -> {
					return obj instanceof BinaryOp && ((BinaryOp<Object, Object>) obj).getAry() > 0
							? Stream.of(RIGHT_PARENT, obj, LEFT_PARENT)
							: Stream.of(obj);
				};
				switch (nary) {
				case 1: {
					final var arg = binaryOp._2._1; // 唯一参数
					Stream.of(arg, SPACE, opname).flatMap(arg_formatter).forEach(stack::push);
					break;
				}
				case 2: {
					final var left_arg = binaryOp._2._1; // 1#参数
					final var right_arg = binaryOp._2._2; // 2#参数
					Stream.of(right_arg, SPACE, opname, SPACE, left_arg).flatMap(arg_formatter).forEach(stack::push);
					break;
				}
				default:
					buffer.append(p);
				} // switch
			} else {
				buffer.append(p);
			} // if
		} // while

		return buffer.toString();
	}

	/**
	 * 运算节点的遍历<br>
	 * 需要注意：forEach 方法 只遍历 算符结构 对于 非 算符结构，即 算符的参数数据 并不给予 遍历 <br>
	 * 如果需要 遍历 算符的参数 数据，需要 利用 binaryop 的实际情况 来 进行处理 <br>
	 * 
	 * @param cons 回调函数 (level:层级从0开始, binaryop:运算节点)->{}
	 */
	@SuppressWarnings("unchecked")
	public void forEach(final BiConsumer<Integer, BinaryOp<?, ?>> cons) {
		final var stack = new Stack<BinaryOp<?, ?>>();
		final Map<Object, Integer> stateLevel = new HashMap<>();

		stateLevel.put(this, 0); // 层级初始化
		stack.push(this); // 根节点如栈

		while (!stack.empty()) {
			final var binaryop = stack.pop();
			final var level = stateLevel.get(binaryop);

			cons.accept(level, binaryop); // 方法回调

			binaryop.getArgS() // 一次处理 参数节点
					.map(Node::UNPACK) // Node 类型去包装
					.filter(e -> e instanceof BinaryOp) // 仅提取 BinaryOp 类型的数据
					.filter(Objects::nonNull) // 过滤掉空值
					.map(e -> (BinaryOp<Object, Object>) e) // 统一声明为 BinaryOp
					.forEach(e -> { // 计算层级加入 stack
						stateLevel.put(e, level + 1);
						stack.push(e);
					}); // forEach
		} // while
	}

	/**
	 * hashCode
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { BinaryOp.class, this._1, this._2() });
	}

	/**
	 * equals
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof BinaryOp<?, ?> bop //
				? Tuple2.defaultComparator().compare( //
						(Tuple2<Object, Object>) (Object) bop, //
						(Tuple2<Object, Object>) (Object) this) == 0 //
				: false;
	}

	/**
	 * 数据格式化
	 */
	@Override
	public String toString() {
		if (this._2 != null) { // 函数类
			final String s1 = this._2._1() == null ? "null" : this._2._1().toString();
			final String s2 = this._2._2() == null ? "null" : this._2._2().toString();
			final var op = COMMA_TEST(this._1) ? "'" + this._1 + "'" : this._1;

			return this.getAry() == 2 ? MessageFormat.format("({0},{1},{2})", op, s1, s2)
					: MessageFormat.format("({0},{1})", op, s1);
		} else { // 常量类
			return this.getName();
		}
	}

	/**
	 * 考虑到 BinaryOp 会覆盖掉name类派生类的特性，比如 一元函数 sin , 转换成 二元 函数 的情况 <br>
	 * 因此 建议 实际使用中 尽量不用 BinaryOp.of ， 可以的化 使用 BinaryOp.duplicate().compose 系列函数<br>
	 * 
	 * @param <T>   参数区第一元素类型
	 * @param <U>   参数区第二元素类型
	 * @param name  名称
	 * @param left  参数区第一元素类型 left参数
	 * @param right 参数区第二元素类型 right参数
	 * @return BinaryOp
	 */
	public static <T, U> BinaryOp<T, U> of(final Object name, final T left, final U right) {
		return of(name, P(left, right));
	}

	/**
	 * 考虑到 BinaryOp 会覆盖掉name类派生类的特性，比如 一元函数 sin , 转换成 二元 函数 的情况 <br>
	 * 因此 建议 实际使用中 尽量不用 BinaryOp.of ， 可以的化 使用 BinaryOp.duplicate().compose 系列函数<br>
	 * 
	 * @param <T>  参数区第一元素类型
	 * @param <U>  参数区第二元素类型
	 * @param name 算符名称
	 * @param args 算符的参数
	 * @return BinaryOp
	 */
	public static <T, U> BinaryOp<T, U> of(final Object name, Tuple2<T, U> args) {
		return new BinaryOp<T, U>(name, args);
	}

	/**
	 * 强制转换成系数
	 * 
	 * @param object 参数对象
	 * @return factorOpt
	 */
	public static final Optional<ConstantOp> factorOpt(final Object object) {
		final var _obj = UNPACK(object);
		if (_obj instanceof BinaryOp<?, ?> bop && bop.isConstant()) {
			return dblOpt(bop._1).map(Ops::TOKEN);
		} else if (_obj instanceof Number num) {
			return Optional.ofNullable(TOKEN(num));
		} else {
			return dblOpt(dbl(_obj)).map(Ops::TOKEN);
		} //
	}

	/**
	 * termOpt 是否是一个乘法项目(*,t1,t2)
	 * 
	 * @param object
	 * @return termOpt
	 */
	public static Optional<BinaryOp<Object, Object>> termOpt(final Object object) {

		if (UNPACK(object) instanceof BinaryOp<?, ?> bop) {
			if (bop._2 != null) {
				final var left = bop._2._1;
				final var right = bop._2._2;
				if (Objects.equals("*", bop._1)) { // 乘项
					final var leftopt = factorOpt(left);
					return leftopt.isEmpty() //
							? factorOpt(right).map(v -> MUL(v, left)) //
							: leftopt.map(v -> MUL(v, right));
				}
			}
			return Optional.empty();
		} else {
			return Optional.empty();
		}
	}

	/**
	 * 强转为浮点数
	 * 
	 * @param obj 目标对象
	 * @return 浮点数Opt
	 */
	public static Optional<Double> dblOpt(final Object obj) {
		return Optional.ofNullable(dbl(obj));
	}

	/**
	 * 强转为浮点数
	 * 
	 * @param obj 目标对象
	 * @return 浮点数Opt
	 */
	public static Double dbl(final Object obj) {

		return obj instanceof Token token ? token.dbl() : IRecord.obj2dbl(null, false).apply(obj);
	}

	/**
	 * 使用 BinaryOp 封装对象：算符化 <br>
	 * 1) 算符类型 返回 原来的算符类型 <br>
	 * 2) 非算符类型 视为视为 TOKEN
	 * 
	 * @param obj 目标对象
	 * @return 如果是BinaryOp直接返回,否则返回TOKEN
	 */
	public static BinaryOp<?, ?> wrap(final Object obj) {
		return obj instanceof BinaryOp<?, ?> bop // 是否本身就是算符
				? bop
				: obj instanceof Number num // 是否是数值类型
						? TOKEN(num) // 数值类型
						: TOKEN(String.valueOf(obj)); // 非数值类型
	}

	/**
	 * 判断t 是否是运算符
	 */
	@SuppressWarnings("unchecked")
	final static Predicate<Object> predicate_op = t -> {
		if ((t instanceof BinaryOp)) {
			return !((BinaryOp<Object, Object>) t).isConstant();
		} else if (t instanceof Node) {
			return ((Node) t).isOp();
		} else {
			return false;
		}
	};

	/**
	 * 当 t 为 null 的时候 返回 false 判断t 是否是词素 是 predicate_op 的 取反
	 */
	public final static Predicate<Object> predicate_token = t -> {
		return t == null ? false : !predicate_op.test(t);
	};

	/**
	 * 判断是否是变量
	 */
	public final static Predicate<Object> predicate_var = t -> {
		Object _t = Node.UNPACK(t);
		if (_t instanceof BinaryOp) {
			@SuppressWarnings("unchecked")
			final var op = ((BinaryOp<Object, Object>) t);
			if (op.isConstant()) {
				final var name = op.getName();
				return IRecord.obj2dbl().apply(name) == null;
			}
			return false;
		} else if (_t instanceof String) {
			return IRecord.obj2dbl().apply(_t) == null;
		} else
			return false;
	};

	/**
	 * 判断t是否是常量
	 */
	@SuppressWarnings("unchecked")
	public final static Predicate<Object> predicate_const = t -> {
		Object _t = Node.UNPACK(t);
		if (t instanceof BinaryOp) {
			final var op = ((BinaryOp<Object, Object>) t);
			if (!((BinaryOp<Object, Object>) t).isConstant()) {
				return false;
			} else {
				_t = op.getName();
			}
		}
		return IRecord.obj2dbl().apply(_t) != null;
	};

	/**
	 * 获取 指定节点 op 的 在parent中的家庭排行,即 是 parent 的 第几个子节点。 <br>
	 * 当 parent 为 null 的 时候 返回 0 <br>
	 * 需要注意:对于 op 即是 parent的第一个参数也是第二个参数的时候, 即 parent(op,op) <br>
	 * rank_eval 是无法区分的具体排行的，同一返回最小的排行。<br>
	 * 此时 rank_eval 排行计算就会出现混乱，即 op 即是老大也是老二, <br>
	 * 会造成 相关方法比如: Node.dumpAST方法计算换乱, 为了避难 此种情况发生，<br>
	 * 请在构造节点的时候 对op 做一个浅拷贝。比如:parent(op,op.duplicate) <br>
	 * 参见 Node.derivate 方法的 微分的除法法则部分。 <br>
	 * MUL( ynode, ynode.duplicate() ) <br>
	 * <br>
	 * 
	 * return 排行序号从0开始,非子节点返回-1 <br>
	 */
	public final static BiFunction<BinaryOp<?, ?>, BinaryOp<?, ?>, Integer> rank_eval = (parent, op) -> {
		if (parent == null) {
			return 0;
		} else {
			final var optional = parent.getArgS().map(kvp_int()).map(sibling -> { // 添加序号并脱壳
				final var o = Node.UNPACK(sibling._2()); // 对象脱壳
				return P(sibling._1(), o instanceof BinaryOp ? o : null);
			}).filter(e -> e._2() == op).findAny(); // 尝试从脱壳之后的args中查询结果项为op的节点

			return optional.map(e -> e._1()).orElse(-1);
		} // if
	};

}