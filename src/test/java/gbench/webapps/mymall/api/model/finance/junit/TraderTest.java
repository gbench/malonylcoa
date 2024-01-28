package gbench.webapps.mymall.api.model.finance.junit;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.kvp.DFrame;
import gbench.webapps.mymall.api.model.finance.trading.FinTraderBuilder;

/**
 * 仓库的测试
 */
public class TraderTest {

	/**
	 * foo
	 */
	@Test
	public void foo() {
		final var trader = FinTraderBuilder.build("id", 1);
		final var inventory = trader.getInventory();
		final var so = trader.saleOrder(2, null); // 卖出订单
		final var po = trader.purchaseOrder(2, null); // 买入订单
		println(trader.getName(), "销售与采购", DFrame.of(so, po));
		trader.invoice(so.dfm("details/items"), so.lng("id"), -1);
		trader.receipt(po.dfm("details/items"), po.lng("id"), -1);
		println(inventory.fifo());
	}
}
