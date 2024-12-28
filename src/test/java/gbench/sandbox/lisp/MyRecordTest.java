package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.io.Output.println;

/**
 * 
 */
public class MyRecordTest {

	@Test
	public void foo() {
		// REC的双值展开
		println(REC("nums", nats(4)));
		println(REC("nums", nats(4).data()));
		println(REC("nums", nats(4)));
		println(REC(nats(4), nats(4)));
		println(REC("nums", nats(4), "name", "zhangsan"));
		println(REC("nums", nats(4), "name"));
	}

}
