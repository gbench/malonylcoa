package gbench.sandbox.math;

import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static gbench.util.math.Maths.fact;
import static java.lang.Math.pow;
import static gbench.util.array.INdarray.nats;

import org.junit.jupiter.api.Test;

import gbench.util.function.Functions;
import gbench.util.lisp.Tuple2;

/**
 * 
 */
public class MathTest {

	@Test
	public void foo() {
		try {
			for (final var n : nd(5, 5.0, 5.1, 0, -5)) {
				println(fact(n));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void bar() {
		final var sin = Functions.identity(Double.class).andThen(
				x -> nats(13).fmap(n -> (n % 2 == 0 ? 1 : -1) * 1d / fact(2 * n + 1) * pow(x, 2 * n + 1)).sum());
		for (final var n : nats(10).add(1)) {
			final var x = 3.14 / n;
			println(sin.apply(x));
			println(Math.sin(x));
			println("---------------------------");
		}
		println(nats(10).add(1).fmap(n->3.1415926/n),nats(10).add(1).fmap(n->3.14/n).fmap(x->Tuple2.of(sin.apply(x), Math.sin(x))));
	}
}
