package gbench.util.math;

import java.util.function.Function;

/**
 * Maths
 */
public class Maths {

	/**
	 * 默认构造函数
	 */
	private Maths() {

	}

	/**
	 * 阶乘
	 * 
	 * @param n 大于等于0的整数
	 * @return 阶乘
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

	/**
	 * Returns the signum function of the argument; zero if the argumentis zero, 1.0
	 * if the argument is greater than zero, -1.0 if theargument is less than zero.
	 * 
	 * @param d the floating-point value whose signum is to be returned
	 * @return the signum function of the argument
	 */
	public static double sign(double d) {
		return Math.signum(d);
	}

	/**
	 * 
	 * Bisection Method <br>
	 * Computes approximate solution of f(x) = 0 <br>
	 * 
	 * 
	 * @param f   inline function f
	 * @param a   low boundary
	 * @param b   high boundary
	 * @param tol tolerance
	 * @return Approximate solution xc
	 */
	public static double bisect(final Function<Double, Double> f, double a, double b, double tol) {
		var _a = Math.min(a, b);
		var _b = Math.max(a, b);
		var fa = f.apply(_a);
		var fb = f.apply(_b);
		if (sign(fa) * sign(fb) >= 0) {
			// cease execution
			throw new RuntimeException(String.format("f(a)[%s]*f(b)[%s] not satisfied! (%s,%s)", fa, fb, _a, _b));
		}
		while ((_b - _a) / 2 > tol) {
			var c = (_a + _b) / 2;
			var fc = f.apply(c);
			if (fc == 0)
				break;
			if (sign(fc) * sign(fa) < 0) {
				_b = c;
				fb = fc;
			} else {
				_a = c;
				fa = fc;
			}
		} // while

		return (_a + _b) / 2;
	}

}
