package gbench.sandbox.math;

import static gbench.util.io.Output.println;
import static gbench.util.math.FinanceMaths.rate;

import org.junit.jupiter.api.Test;

/**
 * 
 */
public class FinanceMathTest {

	/**
	 * 实际利率的计算
	 */
	@Test
	public void foo() {
		println(0, rate(10, 100, -1000, 1000, null));
		println(0, rate(10, 100, -1000, 1000, 0));
		// 非收敛，返回null
		println("非收敛", 1, rate(10, 100, -1000, 1000, 1));
		println("调整收敛", 1, rate(10, 100, -1000, 1000, 1, 1.5));
	}

}
