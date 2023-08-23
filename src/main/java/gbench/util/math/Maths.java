package gbench.util.math;

public class Maths {

	/**
	 * 
	 * @param n 大于等于0的整数
	 * @return
	 */
	public static int fact(final Number n) {
		final int _n = n.intValue();
		if (_n < 0) {
			throw new RuntimeException(String.format("负数%s不能求阶乘!", n));
		} else if (_n == 0)
			return 1;
		else {
			return _n * fact(_n - 1);
		}
	}

}
