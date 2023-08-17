package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static java.util.Arrays.asList;

import org.junit.jupiter.api.Test;

/**
 * 算数运算
 * 
 * @author gbench
 *
 */
public class Ndarray6Test {

	@Test
	public void foo() {
		println("nd(1,2,3).add(1)", nd(1, 2, 3).add(1));
		println("nd(1,2,3).sub(1)", nd(1, 2, 3).sub(1));
		println("nd(1,2,3).mul(2)", nd(1, 2, 3).mul(2));
		println("nd(1,2,3).div(2)", nd(1, 2, 3).div(2));
		final var nd = nd(1, 2, 3);
		println("accum", nd.accum((a, b) -> a + b, asList(nd, nd)));
		println("nd", nd);
	}

	@Test
	public void bar() {
		final var data = nats(32).lngs();
		println(data.dot(data));
		println(data.toString(8));
		println(nd(2, 5).norm());
		println(nd(23, 78).proj(nd(1, 0)));
	}

}
