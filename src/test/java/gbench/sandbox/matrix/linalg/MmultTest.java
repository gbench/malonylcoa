package gbench.sandbox.matrix.linalg;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;

public class MmultTest {

	@Test
	public void foo() {
		final var nx = INdarray.nd(i -> i * 1d, 16).nx(4);
		println(nx.mmult(nx));

	}

}
