package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.sandbox.data.h2.H2db;
import gbench.util.array.INdarray;
import gbench.util.lisp.ArrayRecord;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.util.lisp.Lisp;

import static gbench.util.io.Output.println;

/**
 * 
 * @author Administrator
 *
 */
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

		final var nx = pcts.dataOf(INdarray::of) //
				.sorted((a, b) -> ra.attach(b).i4(0).compareTo(rb.attach(a).i4(0))) //
				.fmap(e -> ra.wrap(e).clone()).nx(1);
		final var dfm = nx.dataOf(DFrame::new);
		final var dfm2 = nx.arrayOf(DFrame::new);

		// 修改
		nx.head().set("id", "101");

		// 显示数据
		println("nx");
		println(nx);
		println("dfm");
		println(dfm);
		println("dfm2");
		println(dfm2);
		println("strmx");
		println(pcts);
	}

	@Test
	public void quz() {
		final var ra = ArrayRecord.of("a,b,c".split(","));
		final var rb = ra.clone();
		final var dfm = Lisp.cph(IRecord.RPTA(IRecord.A(1, 2, 3), 3)) //
				.map(e -> ra.attach(e).duplicate()).collect(Lisp.aaclc(27, DFrame::new));
		println(dfm);
		println(dfm.shape());
		final var nd = dfm.dataOf(INdarray::of) //
				.sorted((a, b) -> ra.attach(b).i4(0).compareTo(rb.attach(b).i4(0)));
		println(dfm);
		println(nd.data() == dfm.dataOf(e->e));
		
	}

}
