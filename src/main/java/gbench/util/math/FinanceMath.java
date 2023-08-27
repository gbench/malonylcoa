package gbench.util.math;

import java.util.Optional;
import java.util.function.Function;

import gbench.util.array.INdarray;

import static gbench.util.function.Functions.*;
import static gbench.util.math.Maths.*;
import static java.lang.Math.*;

/**
 * 金融数学
 */
public class FinanceMath {

	/**
	 * 构造哈桑怒
	 */
	private FinanceMath() {

	}

	/**
	 * 实际利率寒素参见excelRATE函数<br>
	 * 
	 * @param nper Required. The total number of payment periods in an annuity.
	 * @param pmt  Required. The payment made each period and cannot change over the
	 *             life of the annuity. Typically, pmt includes principal and
	 *             interest but no other fees or taxes. If pmt is omitted, you must
	 *             include the fv argument.
	 * @param pv   Required. The present value — the total amount that a series of
	 *             future payments is worth now.
	 * @param fv   Optional. The future value, or a cash balance you want to attain
	 *             after the last payment is made. If fv is omitted, it is assumed
	 *             to be 0 (the future value of a loan, for example, is 0). If fv is
	 *             omitted, you must include the pmt argument.
	 * @param type Optional. The number 0 or 1 and indicates when payments are due.
	 * @return
	 */
	public static double rate(final Number nper, final Number pmt, final Number pv, final Number fv,
			final Number type) {
		final int _nper = nper.intValue();
		final double _pmt = pmt.doubleValue();
		final double _pv = pv.doubleValue();
		final double _fv = Optional.ofNullable(fv).map(Number::doubleValue).orElse(0d);
		final int _type = Optional.ofNullable(type).map(Number::intValue).orElse(0); // 默认起初
		final INdarray<Double> cashflows = INdarray.nd(i -> (Double) _pmt, _nper);
		final Function<Double, Double> formula = identity(0d).andThen(rate -> { // 实际利率
			final double p = _pv;
			final double _p = cashflows.fmap((i, cf) -> cf * pow(1 + rate, _type == 1 ? -i : -(1 + i))).sum();
			return _p + p + _fv * pow(1 + rate, -_nper);
		}); // 实际利率
		final double eff_rate = bisect(formula, 0d, 100, EPSILON);

		return eff_rate;
	}
}
