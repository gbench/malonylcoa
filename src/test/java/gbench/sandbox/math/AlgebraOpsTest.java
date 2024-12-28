package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.op.Ops.*;
import static gbench.util.math.algebra.symbol.Node.PACK;
import static gbench.util.math.algebra.tuple.IRecord.REC;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.math.algebra.AlgebraEngine;
import gbench.util.math.algebra.op.*;
import gbench.util.math.algebra.symbol.Node;
import gbench.util.math.algebra.tuple.IRecord;

/**
 * 自定义算符
 * 
 * @author gbench
 *
 */
public class AlgebraOpsTest {

	/**
	 * 自定义 二元 算符：连接 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 *
	 * @param <T>
	 * @param <U>
	 */
	static class Join<T, U> extends BinaryOp<T, U> {

		public Join(final T t, final U u) {
			super(";", P(t, u));
		}

		@Override
		public Join<T, U> duplicate() {
			return new Join<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var oo = this.argsEval(bindings); // 计算参数
			return Stream.of(oo).flatMap(flat_mapper).collect(Collectors.toList()); // 把参数扁平化形成列表对象
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Join<X, Y> compose(final X x, final Y y) {
			return new Join<>(x, y);
		}

	}

	/**
	 * 自定义 一元算符：求和 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Sum<T> extends UnaryOp<T> {

		public Sum(final T t) {
			super("sum", t);
		}

		@Override
		public Sum<T> duplicate() {
			return new Sum<>(this._2._1);
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var oo = this.argsEval(bindings); // 计算参数
			final var dataS = ((Collection<?>) oo[0]).stream().map(IRecord.obj2dbl()); // 提取参数的元素流并给予数值类型解析

			return dataS.collect(Collectors.summarizingDouble(e -> e)).getSum(); // 数据求和
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数<br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X> Sum<X> compose(final X x) {
			return new Sum<>(x);
		}

	}

	/**
	 * 自定义 二元算符：取余算符 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Mod<T, U> extends BinaryOp<T, U> {

		public Mod(final T t, final U u) {
			super("%", P(t, u));
		}

		@Override
		public Mod<T, U> duplicate() {
			return new Mod<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var oo = this.argsEval(bindings); // 计算参数
			final var vv = Stream.of(oo).flatMap(flat_mapper).map(e -> PACK(e).evaluate(bindings))
					.map(IRecord.obj2dbl()).toArray(Double[]::new);
			return vv[0].intValue() % vv[1].intValue();
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Mod<X, Y> compose(final X x, final Y y) {
			return new Mod<>(x, y);
		}

	}

	/**
	 * 自定义 二元算符：大于 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Gt<T, U> extends BinaryOp<T, U> {

		public Gt(final T t, final U u) {
			super(">", P(t, u));
		}

		@Override
		public Gt<T, U> duplicate() {
			return new Gt<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var oo = this.argsEval(bindings); // 计算参数
			final var vv = Stream.of(oo).flatMap(flat_mapper).map(e -> PACK(e).evaluate(bindings))
					.map(IRecord.obj2dbl()).toArray(Double[]::new);
			return vv[0] > vv[1];
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Gt<X, Y> compose(final X x, final Y y) {
			return new Gt<>(x, y);
		}

	}

	/**
	 * 自定义 二元算符：小于 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Lt<T, U> extends BinaryOp<T, U> {

		public Lt(final T t, final U u) {
			super("<", P(t, u));
		}

		@Override
		public Lt<T, U> duplicate() {
			return new Lt<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var oo = this.argsEval(bindings); // 计算参数
			final var vv = Stream.of(oo).flatMap(flat_mapper).map(e -> PACK(e).evaluate(bindings))
					.map(IRecord.obj2dbl()).toArray(Double[]::new);
			return vv[0] < vv[1];
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Lt<X, Y> compose(final X x, final Y y) {
			return new Lt<>(x, y);
		}

	}

	/**
	 * 自定义 二元算符：等于 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Eq<T, U> extends BinaryOp<T, U> {

		public Eq(final T t, final U u) {
			super("=", P(t, u));
		}

		@Override
		public Eq<T, U> duplicate() {
			return new Eq<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var oo = this.argsEval(bindings); // 计算参数
			final var vv = Stream.of(oo).flatMap(flat_mapper).map(e -> PACK(e).evaluate(bindings))
					.map(IRecord.obj2dbl()).toArray(Double[]::new);
			return vv[0].equals(vv[1]);
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Eq<X, Y> compose(final X x, final Y y) {
			return new Eq<>(x, y);
		}

	}

	/**
	 * 自定义 二元算符： 赋值运算符 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Assign<T, U> extends BinaryOp<T, U> {

		public Assign(final T t, final U u) {
			super(":", P(t, u));
		}

		@Override
		public Assign<T, U> duplicate() {
			return new Assign<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {

			final var key = this._2._1.toString();
			final var value = PACK(this._2._2).evaluate(bindings);
			bindings.put(key, value);
			return value;
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Assign<X, Y> compose(final X x, final Y y) {
			return new Assign<>(x, y);
		}

		/**
		 * 逗号的优先级是0,让Assign的优先级比逗号稍微大一点,然后比其他的优先级又都小一点
		 */
		@Override
		public Number getPriority() {
			return 0.5;
		}

	}

	/**
	 * 多元算符 采用一元算符来实现
	 * 
	 * 自定义 一元算符：条件判断 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class If<T> extends UnaryOp<T> {

		public If(final T t) {
			super("if", t);
		}

		public If<T> duplicate() {
			return new If<>(this._2._1);
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			/*
			 * if 是3元函数 是大于 二元的函数，根据LISP的设计原则 将被视为 1元函数进行计算。<br> if 的参数类型 是一个 逗号表达式， 逗号表达式
			 * 的计算结果是 一个 复合的tuple , <br> 即((condition,true_branch),false_branch) 的二叉树结构。<br>
			 * 注意:[condition,true_branch,false_branch] 这是这个二叉树的深度遍历结果。所以<br>
			 * 获取原始参数列表需要对参数结构二叉树做深度遍历。
			 */
			final var root = PACK(this._2._1); // 根节点，参数二叉树的根节点。
			final var nodes = Comma.flatten(root); // 深度有优先遍历扁平化

			if (nodes.size() < 3) { // 参数数量检测
				try {
					throw new Exception("if算符的参数结构异常:\n" + Node.PACK(this).dumpAST());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			} else { // 合法进行分支选择
				final var flag = (Boolean) nodes.get(0).evaluate(bindings); // 条件项目节点
				return nodes.get(flag ? 1 : 2).evaluate(bindings); // 根据条件项目分别计算各自的分支节点
			} // if
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数<br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X> If<X> compose(final X x) {
			return new If<>(x);
		}

	}

	/**
	 * 多元算符 采用一元算符来实现
	 * 
	 * 自定义 一元算符：for 循环 四元运算符 <br>
	 * for(init,condition,increment,body) <br>
	 * for 循环 返回 被body处理后的 bindings, 是一个 Map结构 可以通过 Get 提取响应的 key 的值。如:<br>
	 * for(init,condition,increment,body) $ key 返回 bindings中的key的值 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class For<T> extends UnaryOp<T> {

		public For(final T t) {
			super("for", t);
		}

		public For<T> duplicate() {
			return new For<>(this._2._1);
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			final var root = PACK(this._2._1); // 根节点，参数二叉树的根节点。
			final var nodes = Comma.flatten(root); // for 是四元运算符

			if (nodes.size() < 4) { // 参数数量检测
				try {
					throw new Exception("for算符的参数结构异常:\n" + Node.PACK(this).dumpAST());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			} else { // 合法进行分支选择
				final var loop_init = nodes.get(0); // 循环初始化
				final var loop_condition = nodes.get(1); // 循环条件检测
				final var loop_increment = nodes.get(2); // 循环递进
				final var loop_body = nodes.get(3); // 循环体

				loop_init.evaluate(bindings);// 数据初始化
				while ((Boolean) loop_condition.evaluate(bindings)) {
					println("for loop:", bindings);
					loop_body.evaluate(bindings);
					loop_increment.evaluate(bindings);
				} // while
				println("for loop result:", bindings);

				return bindings; // 返回绑定记录
			} // if
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数<br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X> For<X> compose(final X x) {
			return new For<>(x);
		}

	}

	/**
	 * 自定义 二元算符：提取算符，模仿R语言的$提取算符 <br>
	 * 
	 * $(rec,key) 或者 rec $ key 这样的参数模式，提取rec中的key属性 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * 
	 * @author gbench
	 * 
	 * @param <T>
	 */
	static class Get<T, U> extends BinaryOp<T, U> {

		public Get(final T t, final U u) {
			super("$", P(t, u));
		}

		@Override
		public Get<T, U> duplicate() {
			return new Get<>(this._2._1, this._2._2());
		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 根据 argsEval 计算的参数 进行 算符求值
		 */
		@Override
		public Object evaluate(final Map<String, Object> bindings) {
			var rec = PACK(this._2._1).evaluate(bindings); // 结算数据主体
			final var key = PACK(this._2._2).getName();
			if (rec instanceof Map) {
				rec = REC(rec);
			} // if

			if (rec instanceof IRecord) {
				return ((IRecord) rec).get(key);
			} else {
				return null;
			} // if

		}

		/**
		 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数 <br>
		 * 固定返回 自定义函数对象本身 <br>
		 */
		@Override
		public <X, Y> Get<X, Y> compose(final X x, final Y y) {
			return new Get<>(x, y);
		}

	}

	/**
	 * 连接算符
	 * 
	 * @param <X>
	 * @param <Y>
	 * @param x
	 * @param y
	 * @return
	 */
	public <X, Y> Join<X, Y> JOIN(X x, Y y) {
		return new Join<>(x, y);
	}

	/**
	 * 取余算符
	 * 
	 * @param <T>
	 * @param <U>
	 * @param t
	 * @param u
	 * @return
	 */
	public <T, U> Mod<T, U> MOD(T t, U u) {
		return new Mod<>(t, u);
	}

	/**
	 * 求和算符
	 * 
	 * @param <X>
	 * @param x
	 * @return
	 */
	public <X> Sum<X> SUM(X x) {
		return new Sum<>(x);
	}

	/**
	 * 大于算符
	 * 
	 * @param <T>
	 * @param <U>
	 * @param t
	 * @param u
	 * @return
	 */
	public <T, U> Gt<T, U> GT(T t, U u) {
		return new Gt<>(t, u);
	}

	/**
	 * 小于算符
	 * 
	 * @param <T>
	 * @param <U>
	 * @param t
	 * @param u
	 * @return
	 */
	public <T, U> Lt<T, U> LT(T t, U u) {
		return new Lt<>(t, u);
	}

	/**
	 * 等于算符
	 * 
	 * @param <T>
	 * @param <U>
	 * @param t
	 * @param u
	 * @return
	 */
	public <T, U> Eq<T, U> EQ(T t, U u) {
		return new Eq<>(t, u);
	}

	/**
	 * 赋值算符
	 * 
	 * @param <T>
	 * @param <U>
	 * @param t
	 * @param u
	 * @return
	 */
	public <T, U> Assign<T, U> ASSIGN(T t, U u) {
		return new Assign<>(t, u);
	}

	/**
	 * 条件算符
	 * 
	 * @param <X>
	 * @param x
	 * @return
	 */
	public <X> If<X> IF(X x) {
		return new If<>(x);
	}

	/**
	 * 循环算符
	 * 
	 * @param <X>
	 * @param x
	 * @return
	 */
	public <X> For<X> FOR(X x) {
		return new For<>(x);
	}

	/**
	 * 提取算符
	 * 
	 * @param <X>
	 * @param <Y>
	 * @param x
	 * @param y
	 * @return
	 */
	public <X, Y> Get<X, Y> GET(X x, Y y) {
		return new Get<>(x, y);
	}

	/**
	 * 自定义算符
	 */
	@Test
	public void foo() {

		final var engine = new AlgebraEngine();

		Stream.of(JOIN(null, null), SUM(null), MOD(null, null), GT(null, null), LT(null, null), EQ(null, null),
				IF(null), FOR(null), ASSIGN(null, null), GET(null, null)).forEach(engine::add);

		Stream.of("sum ( 1; 2; 3; 4; 5 )", // 连接 join 结构
				"5 * if ( x > y , if ( x % 2 = 0, x, x * 2 ), y * 3 )", // if 表达式
				"a : (5*3)", // 赋值表达式, 由于 : 的优先级与 * 相同 所以 需要为 5*3加括号

				// for循环的使用演示,':','sin' 使用了优先级传到导的功能，所以省略了括号
				"5 * for(i:0, i<10, i:i+1, x:sin(x+(y:sin(y)))) $ x", // 返回 bindings中的x的参数值
				"x:10,5 * for(i:0, i<10, i:i+1, x:(x+i+1)) $ x", // 返回 bindings中的x的参数值
				"5 * for(i:0, i<10, i:i+1, x:(x+i+1)) $ x" // 返回 bindings中的x的参数值
		).forEach(line -> {
			final var node = engine.analyze(line); // 表达式的解析

			println("中缀表达式", node.map(BinaryOp::infix));
			println("节点结构：\n", node.dumpAST());
			println("结点计算值 x:5,y:2", node.evaluate("x", 5, "y", 2));
			println("结点计算值 x:4,y:2", node.evaluate("x", 4, "y", 2));
			println("结点计算值 x:1,y:2", node.evaluate("x", 1, "y", 2));
		});
	}

	/**
	 * for 循环函数的示例: <br>
	 * 累计求和 <br>
	 * for loop: {x=0, i=0} <br>
	 * for loop: {x=1.0, i=1.0} <br>
	 * for loop: {x=3.0, i=2.0} <br>
	 * for loop: {x=6.0, i=3.0} <br>
	 * for loop: {x=10.0, i=4.0} <br>
	 * for loop: {x=15.0, i=5.0} <br>
	 * for loop: {x=21.0, i=6.0} <br>
	 * for loop: {x=28.0, i=7.0} <br>
	 * for loop: {x=36.0, i=8.0} <br>
	 * for loop: {x=45.0, i=9.0} <br>
	 * for loop result: {x=55.0, i=10.0} <br>
	 * result: 275.0
	 * 
	 */
	@Test
	public void bar() {

		final var engine = new AlgebraEngine();

		Stream.of(JOIN(null, null), SUM(null), MOD(null, null), GT(null, null), LT(null, null), EQ(null, null),
				IF(null), FOR(null), ASSIGN(null, null), GET(null, null)).forEach(engine::add);
		// 返回 bindings中的x的参数值
		Stream.of("5 * for(i:0, i<10, i:i+1, x:(x+i+1)) $ x").forEach(line -> {
			final var node = engine.analyze(line); // 表达式的解析

			println("infix", node.map(BinaryOp::infix));
			println("ast:\n", node.dumpAST());
			println("result:", node.evaluate("x", 0)); // 从外界注入x
		});
	}
}