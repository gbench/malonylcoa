package gbench.sandbox.jdbc.finance;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;

import org.junit.jupiter.api.Test;

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
public class MyAcct2Test extends AbstractAcct<MyAcct2Test> {

	static { // 设置数据文件名
		AbstractAcct.db = "myaccts2"; // 更改数据库名，否则会因为与MyAcctTest共享数据库而出错
		AbstractAcct.file = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/finance/acct/acct2_data.xlsx";
		AbstractAcct.sqlfile = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/acct2_test.sql";
	}

	@Test
	public void foo() {
		final var fa = new FinAcct("policy0001").intialize(); // 初始化财务会计
		jdbcApp.withTransaction(sess -> {
			println(sess.sql2dframe("show tables"));
			final var company_id = 2; // 公司id
			final var cpdfm = sess.sql2dframe("select * from t_company_product") //
					.forEachBy(h2_json_processor("attrs")).rowS(e -> e.rec("attrs")) //
					.collect(DFrame.dfmclc);
			final var orderdfm = sess.sql2dframe("#getOrders", "company_id", company_id)
					.forEachBy(h2_json_processor("details"));
			final var whdfm = sess.sql2dframe("select * from t_warehouse");
			final var cydfm = sess.sql2dframe("select * from t_company");
			println(cpdfm);
			println(whdfm);
			println(cydfm);
			final var linedfm = orderdfm.rowS().flatMap(e -> e.dfm("details/items") //
					.rowS(e.alias(k -> switch (k) {
					case "id" -> "product_id";
					default -> k;
					}).filter("id,bill_type,position,warehouse_id")::derive)).collect(DFrame.dfmclc); // 数据行
			final var ledger = fa.getLedger(String.format("公司账【%s】", //
					cydfm.one2one("id", company_id, "cy").str("name"))); // 会计账簿
			println(linedfm);
			linedfm.rowS().forEach(line -> {
				final var bill_type = line.str("bill_type");
				final var position = line.str("position");
				final var product_id = line.str("product_id");
				final var warehouse_id = line.str("warehouse_id");
				final var amount = line.dbl("price") * line.dbl("quantity");
				final var path = String.format("%s/%s", bill_type, position);
				final var mykeys = "bill_type,product_id,warehouse_id";
				final var vars = REC("bill_type", bill_type, "product_id", product_id, //
						"warehouse_id", warehouse_id, "mykeys", mykeys);
				ledger.handle(path, amount, vars);
			}); // forEach
			println(fa.dump(fa.trialBalance()));
			println(fa.getEntrieS().collect(DFrame.dfmclc));
		});
	}
}
