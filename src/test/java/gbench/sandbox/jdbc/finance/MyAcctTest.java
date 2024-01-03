package gbench.sandbox.jdbc.finance;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.getter;

import org.junit.jupiter.api.Test;

import gbench.sandbox.jdbc.finance.acct.AbstractAcct;
import gbench.sandbox.jdbc.finance.acct.FinAcct;
import gbench.util.jdbc.kvp.IRecord;

/**
 * MyAcctTest
 */
public class MyAcctTest extends AbstractAcct {

	/**
	 * CL: CLEAR,结算掉对应科目，即查询对应科目的余额方向，而后做出与余额方向相反的分录。<br>
	 * BL: BALANCE，倒轧，根据其他分录的的余额方向 <br>
	 * DR: 借方余额 <br>
	 * CR: 贷方余额 <br>
	 * 
	 * @param recs
	 * @return
	 */
	public IRecord name(final IRecord recs) {
		return null;
	}

	@Test
	public void foo() {
		jdbcApp.withTransaction(sess -> {
			final var coa = sess.sql2dframe("select * from t_coa");
			final var policies = sess.sql2dframe("select * from t_acct_policy")
					.pivotTable1("name,order_type,position,drcr", getter("acctnum"));
			final var ledger = new FinAcct(coa, policies.rec("POLICY1000")) //
					.getLedger("交易性金融资产001"); // 创建一个分类账

			ledger.handle("交易性金融资产-初始确认/LONG", 1000, //
					"交易性金融资产--成本", 1000_000, //
					"应收股利", 60_000, //
					"投资收益", 1_000, // 交易费用
					"银行存款", 1_061_000); // 购入股票
			ledger.handle("交易性金融资产-应收股利/LONG", 60_000); // 收到现金股利
			ledger.handle("交易性金融资产-公允价值变动/LONG", 300_000); // 确认股票价格变动
			ledger.handle("交易性金融资产-终止计量/LONG", 1_500_000); // 公司股票全部售出
			println("-------------------------------------------");
			println(ledger.getEntries());
		});
	}
}
