package gbench.sandbox.matrix.array;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;

import gbench.util.matrix.Matrices;

public class Ndarray8Test {

	@Test
	public void foo() {
		int m = 5;
		int mod = 10;
		int n = 5;
		final var ts = nd(i -> i, m * mod);
		final var us = nd(i -> i, mod * n);
		final var vs = ts.mmult(mod, us);
		println(vs.length());

		println(vs.toString(n));
		println("---------------------------");
		println(Matrices.of(ts.data(), m, mod).mmult(Matrices.of(us.data(), mod, n)));
	}

}
