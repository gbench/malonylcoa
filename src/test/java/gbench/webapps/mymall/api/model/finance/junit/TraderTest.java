package gbench.webapps.mymall.api.model.finance.junit;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.DFrame.dfmclc;
import static gbench.util.jdbc.kvp.IRecord.M;
import static gbench.webapps.mymall.api.model.finance.Finances.*;
import static gbench.webapps.mymall.api.model.finance.acct.invt.Inventory.correspondfm;

import java.util.Collections;
import java.util.stream.Stream;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.tree.Node;
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
		final var times = 50; // 出入库次数
		for (int i = 0; i < times; i++) { // 模拟数据
			final var is_invoice = i >= times / 2; // 随机生成出入库单
			final var bill = is_invoice //
					? trader.saleOrder(2, null) // 卖出订单
					: trader.purchaseOrder(2, null); // 买入订单
			println(bill);
			// Thread.sleep(1);
			if (is_invoice) {
				trader.invoice(bill.dfm("details/items"), 1, bill.lng("id"));
			} else {
				trader.receipt(bill.dfm("details/items"), 1, bill.lng("id"));
			}
			// Thread.sleep(1000);
		} // for
		println("bldfm", invt.bills().forEachBy(h2_json_processor("details")));
		println("fifo", invt.fifo()); // 出入库信息
		final var root = invt.fifo().pivotTable0("id,drcr,bill_id", aa -> M( //
				"bill_id", aa.get(0).i4("bill_id"), //
				"qty", dqsum(aa), // 数量
				"price", aa.get(0).dbl("price"), // 价格
				"time", aa.get(0).ldt("time") // 时间
		));
		println("tree");
		root.treeNode().forEach(e -> {
			final var level = e.getLevel();
			if (level.equals(3)) {
				Collections.sort(e.getChildren(), (a, b) -> { // 低于drcr层按照bill_id进行排序
					final var aa = Stream.of(a, b) //
							.map(Node::getName).map(IRecord.obj2dbl()) //
							.toArray(Double[]::new);
					return aa[0].compareTo(aa[1]);
				});
				final var dfm = e.getChildren().stream().collect(dfmclc(n -> n.attrval(IRecord::REC)));
				println();
				println(dfm);
			} // if
			final var ident = " | ".repeat(e.getLevel() - 1);
			final var name = e.getName();
			final var value = e.attrval(IRecord::REC);
			println(String.format("%s%s --> %s", ident, name, value));
		});
	}

	/**
	 * 收发货匹配
	 */
	@Test
	public void bar() {
		// 空库存，空出库单
		println("空库存，空出库单 correspondfm", correspondfm(new double[] {}, //
				new double[] {}));
		// 空库存
		println("空库存 correspondfm", correspondfm(new double[] {}, //
				new double[] { 2, 4, 8 }));
		// 货源充足
		println("货源充足 correspondfm", correspondfm(new double[] { 1000 }, //
				new double[] { 2, 4, 8 }));
		// 首次发货：精确匹配
		println("首次发货：精确匹配 correspondfm", correspondfm(new double[] { 1, 5, 1, 3, 5 }, //
				new double[] { 2, 4, 9 }));
		// 不经精确匹配
		println("不经精确匹配correspondfm", correspondfm(new double[] { 1, 5, 1, 3, 5, 1, 3, 1 }, //
				new double[] { 2, 4, 8, 8, 9 }));
	}
}
