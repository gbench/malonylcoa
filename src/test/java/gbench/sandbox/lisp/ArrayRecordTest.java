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
		final var p = ArrayRecord.REC("name", "zhangsan", "height", 175);
		println(p);
		println(p.i4("height"), p.i4(1));
	}

	@Test
	public void bar() {
		final var pcts = H2db.shtmx("t_product");
		final var ra = ArrayRecord.of(pcts.keys(), null); // 模板
		final var rb = ra.clone();
		println("get(1)", ra.get(1));
		println("ra", ra);
		final var dfm = pcts.data(INdarray::of).sorted((a, b) -> ra.attach(b).compareTo(rb.attach(a)))
				.map(ra.clone()::attach).collect(DFrame.dfmclc);
		println(dfm);
		println();
		println(pcts);
	}

}
