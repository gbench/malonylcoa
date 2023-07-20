package gbench.sandbox.matrix.linalg;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;

public class MmultTest {

	@Test
	public void foo() {
		for (int k = 100; k <= 1000; k += 100) {
			long begtime = System.currentTimeMillis();
			final var n = k;
			final var nx = INdarray.nd(i -> i * 1d, n * n).nx(n);
			double d = nx.mmult(nx).get();
			long endtime = System.currentTimeMillis();
			println(k, "lasts", endtime - begtime, "last result row", d);
		}
	}

	@Test
	public void bar() {
		final var m = 5;
		final var n = 2;
		final var nx = INdarray.nd(i -> i * 1d, m * n).nx(n);
		println(nx.mmult(nx.transpose()));

	}

}
