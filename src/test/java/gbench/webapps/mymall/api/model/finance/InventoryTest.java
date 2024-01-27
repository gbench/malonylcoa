package gbench.webapps.mymall.api.model.finance;

import static gbench.util.io.Output.println;
import org.junit.jupiter.api.Test;

import gbench.webapps.mymall.api.model.finance.acct.elems.invt.InventoryBuilder;

/**
 * 仓库的测试
 */
public class InventoryTest {

	@Test
	public void foo() {
		final var inventory = InventoryBuilder.build("entityId", 1);
		println(inventory.fifo());
	}
}
