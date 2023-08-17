package gbench.sandbox.matrix.array;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarrayX.ohEncode;
import static gbench.util.array.INdarrayX.ohDecode;
import static gbench.util.io.Output.println;

/**
 * 独热编码
 * 
 * @author gbench
 *
 */
public class NdarrayX3Test {

	@Test
	public void foo() {
		final var data = nats(100);
		println(data.tau());
		println(data.reverse().tau());
		println(((Integer) 1).compareTo(2));
	}

	@Test
	public void onehot() {
		final var origins = nats(10);
		final var onehots = ohEncode(origins);
		println(onehots);
		println("------------------------------");
		onehots.rowS().forEach(e -> {
			println(ohDecode(e), origins);
		});
	}

}
