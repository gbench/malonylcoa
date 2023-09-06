package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.lisp.Lisp.RPTA;
import static gbench.util.lisp.Lisp.cph;
import static gbench.util.math.algebra.Algebras.analyze;
import static gbench.util.math.algebra.Algebras.evaluate;
import static gbench.util.math.algebra.op.Ops.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import gbench.util.math.algebra.AlgebraEngine;
import gbench.util.math.algebra.tuple.IRecord;

/**
 * 
 * @author gbench
 *
 */
public class AlgebraTest {

	/**
	 * 把 node 视作函数结构
	 */
	@Test
	public void qux() {
		final var engine = new AlgebraEngine();
		final var node = engine.analyze("x^2+y^2+z^2"); // 球形结构
		final var rb = IRecord.rb("x,y,z"); // 球形结构的参数构建
		println("----------------------------------------------");
		cph(RPTA(nats(10).data(), 3)).map(rb::get).filter(node.fb((Double d) -> d < 10)) // 提取指定的数据
				.forEach(e -> {
					println(e);
				});
		println("----------------------------------------------");
		final var ar = new AtomicReference<IRecord>();
		cph(RPTA(A(0, 1), 3)).map(rb::get).peek(ar::set).map(node.fx(e -> e)) // 变换指定数据
				.forEach(e -> {
					println(ar.get(), e);
				});
	}

	@Test
	public void foo() {
		println(analyze("(sin x)* sin x").dx().simplify().dumpAST());
		println(analyze("x+x+x").simplify());
		println(analyze("x-x").simplify());
		println(analyze("x/x").simplify());
		println(analyze("(sin cos (x+x)) * (sin cos (x+x))").simplify());
		println(SIN("x").equals(SIN("x")));
		println(analyze("x").isToken());
	}

	@Test
	public void bar() {
		println("", analyze("4").map(e -> e.factorOpt()));
		println("", analyze("4*sin x").map(e -> e.termOpt()));
		println("", analyze("sin x * 4 ").map(e -> e.termOpt()));
	}

	/**
	 * 因子式化简
	 */
	@Test
	public void quz() {
		println("", analyze("2*x").map(e -> e.termOpt()));
		println(analyze("x+x+x+x").simplify());
		println(analyze("sin x + sin x + sin x ").simplify());
		println(analyze("x+(x+2*x+x)+x*sin(x+(x+2*x+x))").simplify());
		println(analyze("x+2*x").simplify());
	}

	/**
	 * 因子式化简
	 */
	@Test
	public void qua() {
		println(analyze("a*sin x + b*sin x").simplify().dumpAST());
		println("-------------------------------------------------------------------");
		println(analyze("sin x * tan x + sin x * (cos x + sin x)").simplify().dumpAST());
		println("-------------------------------------------------------------------");
		println(analyze("x+(x+2*x+x)+x*sin(x+(x+2*x+x))").simplify().dumpAST());
	}

	/**
	 * 因子式化简
	 */
	@Test
	public void qub() {
		println(analyze("x*a + b*x").simplify());
		println(analyze("a*x + x*b").simplify());
		println(analyze("a*b + b*a").simplify());
	}

	/**
	 * 因子式化简
	 */
	@Test
	public void quc() {
		println(analyze("(sin x)^2 + (cos x)^2").dx().simplify());
		println(analyze("cos x").dx().simplify());
		println(analyze("sin x * cos x + cos x * sin x").simplify());
	}

	/**
	 * 因子式化简
	 */
	@Test
	public void qud() {
		println(analyze("(5*a)*b").simplify());
		println(analyze("square(sin x)").dx().simplify());
		println(analyze("1/(sigma*sqrt(2*pi))*exp(neg(square((x-mu)/sigma)/2))").dumpAST());
		println(analyze("1/(sigma*sqrt(2*pi))*exp(neg(square((x-mu)/sigma)/2))").dx().simplify());
	}

	/**
	 * 数学常数
	 */
	@Test
	public void consts() {
		Stream.of("pi,e,tau".split(",")).forEach(k -> {
			println(k, "" + evaluate(k));
		});
		println(analyze("1+pi").simplify());
		println(analyze("1+pi").simplify(true));
	}

}