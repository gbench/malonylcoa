package gbench.sandbox.math;

import static gbench.util.array.INdarray.nd;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.data.DataApp.DFrame;
import static gbench.util.function.Functions.identity;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.math.Maths.fact;
import static java.lang.Math.pow;

import java.util.LinkedList;
import java.util.function.Function;

import static gbench.util.array.INdarray.nats;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.data.DataApp.IRecord;
import gbench.util.function.Functions;
import gbench.util.lisp.Tuple2;

/**
 * 
 */
public class MathTest {

	@Test
	public void foo() {
		try {
			for (final var n : nd(5, 5.0, 5.1, 0, -5)) {
				println(fact(n));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void bar() {
		final var sin = Functions.identity(Double.class).andThen(
				x -> nats(13).fmap(n -> (n % 2 == 0 ? 1 : -1) * 1d / fact(2 * n + 1) * pow(x, 2 * n + 1)).sum());
		for (final var n : nats(10).add(1)) {
			final var x = 3.14 / n;
			println(sin.apply(x));
			println(Math.sin(x));
			println("---------------------------");
		}
		println(nats(10).add(1).fmap(n -> 3.1415926 / n),
				nats(10).add(1).fmap(n -> 3.14 / n).fmap(x -> Tuple2.of(sin.apply(x), Math.sin(x))));
	}

	/**
	 * 计算债券到期收益率
	 */
	@Test
	public void qux() {
		final var pmts = INdarray.nd(59, 59, 59, 59, 59 + 1250).dbls(); // 现金流
		final var ratef = identity(0d).andThen(rate -> pmts.fmap((i, pmt) -> pmt * pow(1 + rate, -(i + 1))).sum())
				.andThen(x -> x - 1000); // 实际利率
		final var rate = bisect(ratef, 0d, 1, 0.00005);
		println("rate", rate);
	}

	/**
	 * 数值分析示例
	 */
	@Test
	public void quz() {
		final var rate = bisect(x -> pow(x, 3) + x - 1, 0d, 1, 0.00005);
		println("rate", rate);
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
		var i = 0;
		final var dataf = new LinkedList<IRecord>();
		while ((_b - _a) / 2 > tol) {
			var c = (_a + _b) / 2;
			var fc = f.apply(c);
			dataf.add(REC("i,a0,fai,ci,fci,bi,fbi".split(","), A(i++, _a, fa, c, fc, _b, fb)));
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

		println(DFrame.of(dataf));

		return (_a + _b) / 2;
	}
}
