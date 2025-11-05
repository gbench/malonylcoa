package gbench.sandbox.jdbc.finance;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import gbench.global.Globals;
import gbench.sandbox.jdbc.finance.acct.AbstractAcct;
import gbench.sandbox.jdbc.finance.acct.FinAcct;
import gbench.util.jdbc.kvp.DFrame;

/**
 * 简单的会计记账法(模拟一个平台下的商户间的收发货交易),示例<br>
 * MyAcct2Test <br>
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
		AbstractAcct.file = Globals.WS_HOME
				+ "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/finance/acct/acct2_data.xlsx";
		AbstractAcct.sqlfile = Globals.WS_HOME
				+ "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/acct2_test.sql";
	}

	/**
	 * 模拟公司运行
	 * 
	 * @param company_id 公司id
	 * @param fa         财务会计对象
	 */
	public Consumer<Integer> executor(final FinAcct fa) {

		return company_id -> jdbcApp.withTransaction(sess -> {

			println("平台数据表", sess.sql2dframe("show tables")); // 数据表
			final var cpdfm = sess.sql2dframe("select * from t_company_product") // 公司产品表
					.forEachBy(h2_json_processor("attrs")).rowS(e -> e.rec("attrs").derive(e)) // 解析属性字段
					.collect(DFrame.dfmclc);
			final var bldfm = sess.sql2dframe("#GJVs2", "company_id", company_id) // 读取指定的记账单据凭证信息
					.forEachBy(h2_json_processor("details"));
			final var whdfm = sess.sql2dframe("select * from t_warehouse"); // 仓库信息
			final var cydfm = sess.sql2dframe("select * from t_company"); // 公司信息

			println("--------------------------------------------------------------------------");
			println("模拟公司", cydfm.one2one("id", company_id, "cy"));
			println("--------------------------------------------------------------------------");
			println("公司cydfm", cydfm.head(5));
			println("公司产品cpdfm", cpdfm.head(5));
			println("仓库whdfm", whdfm.head(5));

			final var linedfm = bldfm.rowS().flatMap(e -> e.dfm("details/items") // 记账单据凭证行项目:产品/资产明细
					.rowS(item -> item.alias(k -> switch (k) { // 字段改名
					case "id" -> "product_id"; // 公司产品id改为产品id
					default -> k; // 其他保持默认不变
					}).derive(e.filter("id,bill_type,position,warehouse_id")))).collect(DFrame.dfmclc); // 数据行
			final var ledger = fa.getLedger(String.format("公司账【%s】", //
					cydfm.one2one("id", company_id, "cy").str("name"))); // 会计账簿

			println("单据凭证行项目linedfm", linedfm);

			// 单据凭证行项目日记账
			linedfm.rowS().forEach(line -> { // 依次处理各个单据凭证行项目
				final var channel = this.getClass().getSimpleName(); // 业务渠道 
				final var bill_type = line.str("bill_type"); // 订单类型
				final var position = line.str("position"); // 交易的产品头寸
				final var product_id = line.i4("product_id"); // 产品id
				final var warehouse_id = line.i4("warehouse_id"); // 仓库id
				final var amount = line.dbl("price") * line.dbl("quantity"); // 交易金额
				final var path = String.format("%s/%s", bill_type, position); // 会计测录路径
				final var mykeys = "bill_type,product_id,warehouse_id,channel"; // 自定义属性的键名序列
				final var vars = REC("bill_type", bill_type, "product_id", product_id, //
						"warehouse_id", warehouse_id, "channel", channel, "mykeys", mykeys);
				// 账目誊写
				ledger.handle(path, amount, vars); // 写入分类账
			}); // forEach

			fa.getEntrieS().forEach(entry -> { // 增加id转名字
				final var product_id = entry.i4("product_id");
				final var warehouse_id = entry.i4("warehouse_id");
				final var product = cpdfm.one2opt("id", product_id, "cp").map(e -> e.str("name")).orElse("-");
				final var warehouse = whdfm.one2opt("id", warehouse_id, "wh").map(e -> e.str("name")).orElse("总库");
				entry.add("product", product, "warehouse", warehouse);
			}); // forEach

			println(fa.dump(fa.trialBalance("ledger_id,channel,acctnum,warehouse,product,drcr".split(","))));
			println(fa.getEntrieS().collect(DFrame.dfmclc));
		});
	}

	/**
	 * 模拟一个平台下的商户间的收发货交易
	 */
	@Test
	public void foo() {

		// 财务会计
		final var fa = new FinAcct("policy0001").intialize(); // 初始化财务会计

		// 模拟各个公司的运行
		Stream.of(1, 2).forEach(executor(fa));
	}
}
