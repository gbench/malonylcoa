package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.lisp.Lisp.RPTA;
import static gbench.util.lisp.Lisp.cph;
import static gbench.util.math.algebra.AlgebraEngine.parse;
import static gbench.util.math.algebra.op.Ops.*;

import java.util.concurrent.atomic.AtomicReference;

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
		println(parse("(sin x)* sin x").dx().simplify().dumpAST());
		println(parse("x+x+x").simplify());
		println(parse("x-x").simplify());
		println(parse("x/x").simplify());
		println(parse("(sin cos (x+x)) * (sin cos (x+x))").simplify());
		println(SIN("x").equals(SIN("x")));
		println(parse("x").isToken());
	}

	@Test
	public void bar() {
		println("", parse("4").map(e -> e.factorOpt()));
		println("", parse("4*sin x").map(e -> e.termOpt()));
		println("", parse("sin x * 4 ").map(e -> e.termOpt()));
	}

	/**
	 * 因子式化简
	 */
	@Test
	public void quz() {
		println("", parse("2*x").map(e -> e.termOpt()));
		println(parse("x+x+x+x").simplify());
		println(parse("sin x + sin x + sin x ").simplify());
		println(parse("x+x+x+sin (x+x+x)").simplify());
		println(parse("x+2*x").simplify());
	}

}