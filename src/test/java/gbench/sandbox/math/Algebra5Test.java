package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.Algebras.analyze;
import static gbench.util.math.algebra.op.Ops.*;

/**
 * 
 * @author gbench
 *
 */
public class Algebra5Test {

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
	 * 
	 */
	@Test
	public void foo() {
		final var a = MUL("x", MUL("y", MUL("z", 2)));
		println(a.simplify());
		println(a.flatArgs2());
		a.flatS().forEach(e -> {
			println(e.isConstant(), e, e.dbl());
		});
	}

}