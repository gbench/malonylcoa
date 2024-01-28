package gbench.webapps.mymall.api.model.finance.junit;

import static gbench.util.io.Output.println;

import java.util.Random;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.kvp.DFrame;
import gbench.webapps.mymall.api.model.finance.acct.invt.InventoryBuilder;
import gbench.webapps.mymall.api.model.finance.trading.FinTraderBuilder;

/**
 * 仓库的测试
 */
public class InventoryTest {

	/**
	 * 
	 * @param dfm
	 * @param size
	 * @return
	 */
	public DFrame rnd(final DFrame dfm, final int size) {
		return dfm.shuffle().head(new Random().nextInt(size));
	}

	/**
	 * foo
	 */
	@Test
	public void foo() {
		final var trader = FinTraderBuilder.build("id", 2);
		final var inventory = InventoryBuilder.build("entityId", 1);
		final var pcts = rnd(trader.getProvidables(), 5); // 可供出售的商品
		println(trader.getName(), pcts);
		println(inventory.fifo());
	}
}
