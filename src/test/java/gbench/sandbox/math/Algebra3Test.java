package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.Algebras.analyze;

import java.util.stream.Stream;

import gbench.util.math.algebra.AlgebraEngine;
import gbench.util.math.algebra.Algebras;

/**
 * 
 * @author gbench
 *
 */
public class Algebra3Test {

	/**
	 * 语法树的生成与计算
	 */
	@Test
	void foo() {
		final var engine = new AlgebraEngine();
		Stream.of("5+pow(2,5)*tan(45.6/255-cos(4/6))", //
				"(log(10,100)+ln(2.71828)+cot(45/89))", //
				"(1+4/sin(5))", //
				"1+(2+4*7-5*3+4*sin(x)+cos(x)+exp(45)/ln(x))-pow(6/9,2+sin(pow(3.14,2+1*5)))", //
				"(1+log(2,3))", //
				"(exp(x)/cos(x))", //
				"5+pow(2,5)*tan(x)", //
				"5+pow(2,5)*tan(x)+sin(x+x*3/tan(x)+5)", //
				"pow(1+2*sin(x),3+6/5*(4+2))", //
				"1+pow(1+2*sin(x),3)", //
				"4,3+3*5", "5+pow(2,3)*10", //
				"(+(1,(2,(3,4+5))))", //
				"(pow(2,5,4))", //
				"1+4*7-6/9" //
		).forEach(line -> {
			final var root = engine.analyze(line);

			println("\n----------------------------");
			println("表达式的计算");
			println("----------------------------");
			println("语法树", root);
			println("语法树结算", root.evaluate("x", 10));

			println("============================\n");
			println(line, "的语法树结构\n");
			println(root.dumpAST());
		}); // forEach
	}

	@Test
	public void bar() {
		double d = Algebras.evaluate("1+2*3");
		println(d);
	}

	@Test
	public void quz() {
		for (var i = 0; i < 10; i++) {
			final var dx = nats(i).reduce(analyze("sin x"), (a, b) -> a.derivate());
			println(i, dx);
		}
		println("reduce", nats(10).reduce(10, (a, b) -> a + b));
	}

}