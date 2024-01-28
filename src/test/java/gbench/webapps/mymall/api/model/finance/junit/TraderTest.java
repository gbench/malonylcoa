package gbench.webapps.mymall.api.model.finance.junit;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.M;
import static gbench.webapps.mymall.api.model.finance.Finances.*;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.lisp.Lisp;
import gbench.webapps.mymall.api.model.finance.trading.FinTraderBuilder;

/**
 * 产品出入库的模拟
 */
public class TraderTest {

	/**
	 * foo
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void foo() throws InterruptedException {
		final var trader = FinTraderBuilder.build("id", 1);
		final var invt = trader.getInventory();
		final var times = 10; // 出入库次数
		for (int i = 0; i < times; i++) { // 模拟数据
			final var flag = Math.random() > 0.5;// 随机生成出入库单
			final var bill = flag //
					? trader.saleOrder(2, null) // 卖出订单
					: trader.purchaseOrder(2, null); // 买入订单
			println(bill);
			Thread.sleep(1000);
			if (flag) {
				trader.invoice(bill.dfm("details/items"), 1, bill.lng("id"));
			} else {
				trader.receipt(bill.dfm("details/items"), 1, bill.lng("id"));
			}
			Thread.sleep(1000);
		} // for
		println("bldfm", invt.bills().forEachBy(h2_json_processor("details")));
		println("fifo", invt.fifo()); // 出入库信息
		final var root = invt.fifo().pivotTable("id,drcr,bill_id", ss -> ss.collect(Lisp.aaclc(aa -> M(//
				"bill_id", aa.get(0).lng("bill_id"), // 数量
				"qty", dqsum(aa), // 数量
				"price", aa.get(0).ldt("price"), // 价格
				"time", aa.get(0).ldt("time") // 时间
		))));
		println("tree");
		root.treeNode().forEach(e -> {
			final var ident = " | ".repeat(e.getLevel() - 1);
			final var name = e.getName();
			final var value = e.attrval(IRecord::REC);
			println(String.format("%s%s --> %s", ident, name, value));
		});
	}
}
