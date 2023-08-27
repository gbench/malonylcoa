package gbench.sandbox.math;

import static gbench.util.io.Output.println;
import static gbench.util.math.FinanceMath.rate;

import org.junit.jupiter.api.Test;

/**
 * 
 */
public class FinanceMathTest {

	@Test
	public void foo() {
		 println(0, rate(10, 100, -1000, 1000, null));
		 println(0, rate(10, 100, -1000, 1000, 0));
		println(1, rate(2, 100, -1000, 1000, 1));
	}

}
