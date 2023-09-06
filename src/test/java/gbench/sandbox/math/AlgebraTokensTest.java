package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import gbench.util.math.algebra.AlgebraEngine;
import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.tuple.IRecord;

import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.symbol.Node.PACK;
import static gbench.util.math.algebra.tuple.IRecord.REC;
import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.Arrays;
import java.util.Map;

/**
 * 分词<br>
 * 
 * @author gbench
 *
 */
public class AlgebraTokensTest {

	/**
	 * 自定义分词
	 */
	@Test
	public void foo() {
		final var line = "a := ( b := 1 + 0.5 )";
		final var engine = new AlgebraEngine();
		final var aa = line.split("[\\s]+"); // 自定义分词
		final var rb = IRecord.rb("type,value"); // 符号类型
		final var tokens = Arrays.stream(aa).map(e -> rb.get("word", e)).toList();
		println("tokens", tokens);
		engine.add(new Assign<>(null, null));
		final var node = engine.analyze(tokens); // 分析类型
		println("ast", node.dumpAST());
		println("value", node.evaluate());
	}

	/**
	 * 自定义分词
	 */
	@Test
	public void bar() {
		final var line = "a := b";
		final var engine = new AlgebraEngine();
		final var tokens = Arrays.stream(line.split("\\s+")).map(e -> REC("type", "word", "value", e)).toList();
		println("tokens", tokens);
		engine.add(BinaryOp.of(":=", P(null, null)));
		final var node = engine.analyze(tokens);
		println("ast", node.dumpAST());

	}

	/**
	 * 自定义 二元算符： 赋值运算符 <br>
	 * 
	 * compose 和 evaluate 连个函数是 进行 自定义算符 必须实现的 函数
	 * 
	 * @author gbench
	 * 
	 * @param <T> 第一参数
	 * @param <U> 第二参数
	 */
	public static class Assign<T, U> extends BinaryOp<T, U> {

		/**
		 * Assign 构造函数
		 * 
		 * @param t 第一参数
		 * @param u 第二参数
		 */
		public Assign(final T t, final U u) {
			super(":=", P(t, u));
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

	} // Assign

}