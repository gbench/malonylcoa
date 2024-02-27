package gbench.sandbox.jdbc.finance;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.global.Globals;
import gbench.sandbox.jdbc.finance.acct.AbstractAcct;
import gbench.sandbox.jdbc.finance.acct.FinAcct;
import gbench.util.jdbc.kvp.DFrame;

/**
 * 简单的会计记账法,示例<br>
 * MyAcctTest <br>
 * 
 * 根据会计策略中的分录科目指令,建造分类账对象Ledger,并为Ledger分类账,自动生成会计分录，并制作相应分类账的试算平衡表 <br>
 * 分录科目指令， <br>
 * CL: CLEAR,结算掉对应科目，即查询对应科目的余额方向，而后做出与余额方向相反的分录。<br>
 * BL: BALANCE，倒轧，根据其他分录的的余额方向 <br>
 * DR: 借方余额 <br>
 * CR: 贷方余额 <br>
 */
public class MyAcctTest {

	static { // 设置数据文件名
		AbstractAcct.db = "myaccts"; // 数据库名
		AbstractAcct.file = Globals.WS_HOME
				+ "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/finance/acct/acct_data.xlsx"; // 数据文件
		AbstractAcct.sqlfile = Globals.WS_HOME
				+ "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
	}

	@Test
	public void foo() {
		final var fa = new FinAcct("POLICY1000").intialize(); // 初始化财务会计

		for (final var ledgerId : "交易性金融资产001,交易性金融资产002".split(",")) { // 会计记账
			final var ledger = fa.getLedger(ledgerId); // 创建一个分类账
			// 期初：初始确认/计量
			ledger.handle("交易性金融资产-初始确认/LONG", 1000_000, // amount 是默认金额
					// "交易性金融资产--成本", 1000_000, // 交易性金融资产--成本 采用默认的amount金额,故可以省略
					"应收股利", 60_000, // 应收股利
					6111, 1_000, // 交易费用, 键名可以用账户编号代替,即:"投资收益", 1_000,
					"银行存款", 1_061_000); // 购入股票
			// 期中: 后续计量&资产负债表日的变动跟踪&处理
			ledger.handle("交易性金融资产-应收股利/LONG", 60_000); // 收到现金股利
			ledger.handle("交易性金融资产-公允价值变动/LONG", 300_000); // 确认股票价格变动
			// 期末: 终止确认&资产处置
			ledger.handle("交易性金融资产-终止计量/LONG", 1_500_000); // 公司股票全部售出
			println(ledger.getEntries());
			println(fa.dump(fa.trialBalance(ledgerId)));
		}

		println(fa.getEntrieS().collect(DFrame.dfmclc));
		println("-------------------------------------------");
		println(fa.dump(fa.trialBalance()));
		println("-------------------------------------------");
		println(fa.dump(fa.trialBalance(null, "acctnum,drcr")));

	}
}
