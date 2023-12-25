package gbench.sandbox.jdbc.finance;

import static gbench.sandbox.jdbc.finance.BondTest.Bond.bond;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.rb;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.math.FinanceMaths;

/**
 * 
 */
public class BondTest {

	/**
	 * 
	 */
	public static class Bond {
		/**
		 * Bond 债券
		 * 
		 * @param nper 期限
		 * @param pmt  分期支付
		 * @param pv   初始金额
		 * @param fv   到日期金额
		 */
		public Bond(final Number nper, final Number pmt, final Number pv, final Number fv) {
			this.nper = nper.intValue();
			this.pmt = pmt.doubleValue();
			this.pv = pv.doubleValue();
			this.fv = fv.doubleValue();
		}

		/**
		 * rate
		 * 
		 * @return
		 */
		public double rate() {
			return FinanceMaths.rate(nper, pmt, pv, fv);
		}

		/**
		 * 现金流
		 * 
		 * @return 现金流列表
		 */
		public DFrame cashflows() {
			final var rb = rb("period,value,date,description");
			final var start = LocalDate.now();
			return Stream.iterate(0, i -> i + 1).limit(nper).map(i -> {
				final var date = start.plusMonths(i);
				if (i == 0) { // 期初/初始
					return rb.get(i, date, pv, String.format("第%2d年:", i + 1));
				} else if (i < nper - 1) { // 期中
					return rb.get(i, date, pmt, String.format("第%2d年:", i + 1));
				} else { // 期末/到期
					final var d = pmt + fv;
					return rb.get(i, date, d, String.format("第%2d年(%,.2f + %,.2f)", i + 1, pmt, fv));
				} // if
			}).collect(DFrame.dfmclc);
		}

		/**
		 * 格式化：
		 */
		public String toString() {
			return String.format("Bond[NPER:%d, PMT:%,.2f, PV:%,.2f, FV:%,.2f, RATE:]", nper, pmt, pv, fv, this.rate());
		}

		static Bond bond(final Number nper, final Number pmt, final Number pv, final Number fv) {
			return new Bond(nper, pmt, pv, fv);
		}

		final int nper;
		final double pmt;
		final double pv;
		final double fv;
	}

	/**
	 * 打印债券信息
	 */
	@Test
	public void foo() {
		final var b = bond(5, 59, -1000, 1250);
		println(b);
		println(b.cashflows());
	}

}
