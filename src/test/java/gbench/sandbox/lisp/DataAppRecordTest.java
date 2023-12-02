package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.util.json.MyJson;

import static gbench.util.array.INdarray.nats;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.io.Output.println;

import java.time.LocalDate;

/**
 * 
 */
public class DataAppRecordTest {

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

	/**
	 * bar
	 */
	@Test
	public void bar() {
		final var line = REC("name", "zhangsan", "borth", LocalDate.now().minusYears(20));
		try {
			final var objM = MyJson.recM();
			final var json = MyJson.toJson(line);
			final var rec = objM.readValue(json, IRecord.class);
			println("1", json, rec.getClass());
			println("-", rec.json());
			final var rec2 = REC(json);
			println("2", rec2, rec2.getClass());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
