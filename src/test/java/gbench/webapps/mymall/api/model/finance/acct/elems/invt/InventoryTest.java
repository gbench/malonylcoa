package gbench.webapps.mymall.api.model.finance.acct.elems.invt;

import static gbench.util.io.Output.println;
import org.junit.jupiter.api.Test;

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
