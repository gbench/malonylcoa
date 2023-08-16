package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.sandbox.data.h2.H2db;
import gbench.util.array.INdarray;
import gbench.util.lisp.ArrayRecord;
import gbench.util.lisp.DFrame;

import static gbench.util.io.Output.println;

public class ArrayRecordTest {

	@Test
	final void foo() {
		final var p = ArrayRecord.REC("name", "zhangsan", "height", 175, "phone", 13120751773l);
		println(p);
		println("height", p.i4("height"), "index:1", p.i4(1));
		println("remove height", p.remove("height"));
		println("p", p);
	}

	@Test
	public void bar() {
		final var pcts = H2db.shtmx("t_product");
		final var ra = ArrayRecord.of(pcts.keys()); // 模板
		final var rb = ra.clone();
		println("get(1)", ra.get(1));
		println("ra", ra);

		final var dfm = pcts.data(INdarray::of) //
				.sorted((a, b) -> ra.attach(b).i4(0).compareTo(rb.attach(a).i4(0))) //
				.fmap(e->ra.wrap(e).clone())
				.nx(1);
		println("dfm");
		println(dfm);
		println("strmx");
		println(pcts);
	}

}
