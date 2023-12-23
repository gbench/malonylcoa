package gbench.util.math;

import java.util.Optional;
import java.util.function.Function;

import gbench.util.array.INdarray;

import static gbench.util.array.INdarray.nd;
import static gbench.util.function.Functions.*;
import static gbench.util.math.Maths.*;
import static java.lang.Math.*;

/**
 * 金融数学
 */
public class FinanceMaths {

	/**
	 * 构造哈桑怒
	 */
	private FinanceMaths() {

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
	 * @return Double 非收敛返回null,type 默认为0:期末产生收入
	 */
	public static Double rate(final Number nper, final Number pmt, final Number pv, final Number fv) {
		return rate(nper, pmt, pv, fv, 0, null);
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
	 *             1) 0 or omitted(null) : At the end of the period <br>
	 *             2) 1: At the beginning of the period <br>
	 * @return Double 非收敛返回null
	 */
	public static Double rate(final Number nper, final Number pmt, final Number pv, final Number fv,
			final Number type) {
		return rate(nper, pmt, pv, fv, type, null);
	}

	/**
	 * 实际利率寒素参见excelRATE函数<br>
	 * 
	 * @param nper  Required. The total number of payment periods in an annuity.
	 * @param pmt   Required. The payment made each period and cannot change over
	 *              the life of the annuity. Typically, pmt includes principal and
	 *              interest but no other fees or taxes. If pmt is omitted, you must
	 *              include the fv argument.
	 * @param pv    Required. The present value — the total amount that a series of
	 *              future payments is worth now.
	 * @param fv    Optional. The future value, or a cash balance you want to attain
	 *              after the last payment is made. If fv is omitted, it is assumed
	 *              to be 0 (the future value of a loan, for example, is 0). If fv
	 *              is omitted, you must include the pmt argument.
	 * @param type  Optional. The number 0 or 1 and indicates when payments are due.
	 *              1) 0 or omitted(null) : At the end of the period <br>
	 *              2) 1: At the beginning of the period <br>
	 * @param guess Guess Optional. Your guess for what the rate will be. <br>
	 *              If you omit guess, it is assumed to be 10 percent. <br>
	 *              If RATE does not converge, try different values for guess. RATE
	 *              <br>
	 *              usually converges if guess is between 0 and 1 <br>
	 * 
	 * @return Double 非收敛返回null
	 */
	public static Double rate(final Number nper, final Number pmt, final Number pv, final Number fv, final Number type,
			final Number guess) {
		final int _nper = nper.intValue();
		final double _pmt = pmt.doubleValue();
		final double _pv = pv.doubleValue();
		final double _fv = Optional.ofNullable(fv).map(Number::doubleValue).orElse(0d);
		final int _type = Optional.ofNullable(type).map(Number::intValue).orElse(0); // 默认起初
		final INdarray<Double> cashflows = nd(i -> (Double) _pmt, _nper);
		final Function<Double, Double> formula = identity(0d).andThen(rate -> { // 实际利率
			final boolean flag = _type == 1; // 是否是期末支付
			final double p = _pv; // 现价 price
			final double p_adjust = _fv * pow(1 + rate, -_nper); // 收入与支出的平衡调节
			final double _p = cashflows.fmap((i, cf) -> cf * pow(1 + rate, -(flag ? i : 1 + i))).sum(); // 折现现价
			return _p + p + p_adjust;
		}); // 实际利率
		final double _guess = Optional.ofNullable(guess).map(Number::doubleValue).orElse(.1);
		Double ret = null;
		try {
			final double eff_rate = bisect(formula, 0d, _guess, EPSILON);
			ret = eff_rate;
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return ret;
	}
}
