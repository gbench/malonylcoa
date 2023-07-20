package gbench.sandbox.matrix.linalg;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;

public class MmultTest {

	@Test
	public void foo() {
		final var n = 100;
		final var nx = INdarray.nd(i -> i * 1d, n * n).nx(n);
		println(nx.mmult(nx));

	}

}
