package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.Algebras.analyze;
import static gbench.util.math.algebra.tuple.IRecord.REC;

import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.math.algebra.AlgebraEngine;
import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.op.Ops;
import gbench.util.math.algebra.symbol.Node;

/**
 * 
 * @author gbench
 *
 */
public class Algebra2Test {

	@Test
	public void foo() {
		final var engine = new AlgebraEngine();

		Stream.of( // 泰勒计算的一般项表达式
				"1/!(2*n+1)*pow(x,2*n+1)*pow(neg 1,n)", // sin 通项
				"1/!(2*n)*x^(2*n)*(neg 1)^(n)", // cos 通项
				"1/!(n)*x^(n)" // exp 通项
		).forEach(line -> {
			final var general_node = engine.analyze(line); // 分析一般项的结构

			println("\n-------------------------------------------------------------");
			println("一般项", line);
			println("一般项的前缀表达式", general_node);
			println("一般项的中缀表达式", general_node.map(BinaryOp::infix));
			println("-------------------------------------------------------------");

			final var result_node = Stream.iterate(0, i -> i + 1).limit(10) // 提取项目数量
					.map(n -> general_node.evaluate("n", n)) // 通用项
					.reduce(Ops::ADD) // 通用项的组织
					.map(Node::PACK) // 结果包装
					.get(); // 提取结果值

			println("结果项的结构\n", result_node.dumpAST());
			Stream.of(Math.PI / 6, 1).forEach(x -> { // 结果值验证
				println("取值于" + x + "的结果项值", result_node.evaluate("x", x));
			});
		}); // forEach
	}

	/**
	 * 求导示例
	 */
	@Test
	public void bar() {
		final var engine = new AlgebraEngine();

		Stream.of( // 基本函数求导
				"a", //
				"a+x", //
				"a-x", //
				"a*x", //
				"a/x", //
				"1+a*x-n", //
				"pow(a*x,n)", //
				"expa(a,a*x)", //
				"ln(a*x)", //
				"log(a,a*x)", //
				"sin(a*x)", //
				"cos(a*x)", //
				"tan(a*x)", //
				"cot(a*x)", //
				"sec(a*x)", //
				"csc(a*x)", //
				"arcsin(a*x)", //
				"arccos(a*x)", //
				"arctan(a*x)", //
				"arccot(a*x)", //
				"exp(sin(x))+ln(cos(x))", //
				"pow(cos(x),2)", //
				"expa(5,cos(x))" //
		).forEach(line -> { // 表达式处理
			final var node = engine.analyze(line); // 表达式分析
			final var d_node = node.derivate().fmap(e -> e.simplify()); // 表达式求导
			final var args = REC("a", 1d, "n", 2d, "x", 0.5); // 数据参数

			println("表达式:", line);
			println("参数值:", args);
			println("表达式中缀:", node.map(BinaryOp::infix));
			println("导数结构中缀:", d_node.map(BinaryOp::infix));
			println("导数结构:\n", d_node.dumpAST());
			println("函数值:", node.eval(args));
			println("导数值:", d_node.eval(args));
			println("----------------------------------------------");
		}); // forEach
	}

	@Test
	public void quz() {
		final var e1 = Stream.iterate(analyze("cos x"), e -> e.derivate().simplify()).limit(10)
				.collect(INdarray.ndclc()); // cos 高阶导数
		println(e1);
		final var e2 = Stream.iterate(analyze("cos x"), e -> e.derivate().simplify()).map(e -> e.simplify())
				.map(e -> e.eval("x", 0)).limit(10).collect(INdarray.ndclc()); // cos 高阶导数
		println(e2);
		println(analyze("neg neg x").simplify());
	}

}