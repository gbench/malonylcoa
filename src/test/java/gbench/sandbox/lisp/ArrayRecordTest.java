package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.sandbox.data.h2.H2db;
import gbench.util.array.INdarray;
import gbench.util.data.xls.DataMatrix;
import gbench.util.lisp.ArrayRecord;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.Lisp;
import gbench.util.lisp.MyRecord;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.ArrayRecord.ra;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.lisp.IRecord.RPTA;

import java.util.stream.Collectors;

/**
 * 
 * @author Administrator
 *
 */
public class ArrayRecordTest {

	@Test
	final void foo() {
		final var ra = ArrayRecord.REC("name", "zhangsan", "height", 175, "phone", 13120751773l);
		println(ra);
		println("ra.i4(\"height\")", ra.i4("height"), "ra.i4(1)", ra.i4(1));
		println("ra.remove(\"height\")", ra.remove("height"));
		println("ra", ra);
		println("ra.filter(\"phone,name,height\")", ra.filter("phone,name,height"));
		println("ra.set(100, 2323)", ra.set(100, 2323));
		final var rm = MyRecord.REC(ra);
		println("ra==ra.set(\"name\", \"zhanger\")", ra == ra.set("name", "zhanger"));
		println("ra==ra.set(\"name1\", \"zhanger\")", ra == ra.set("name1", "zhanger"));
		println("rm==rm.set(\"name\", \"zhanger\")", rm == rm.set("name", "zhanger"));
		println("rm==rm.set(\"name1\", \"zhanger\")", rm == rm.set("name1", "zhanger"));
	}

	@Test
	public void bar() {
		final var pcts = H2db.shtmx("t_product");
		final var ra = ArrayRecord.of(pcts.keys()); // 模板
		final var rb = ra.clone();
		println("get(1)", ra.get(1));
		println("ra", ra);

		final var nx = pcts.dataOf(INdarray::of) //
				.sorted((a, b) -> ra.attachcln(b).i4(0).compareTo(rb.attachcln(a).i4(0))) //
				.fmap(e -> ra.attachcln(e).clone()).nx(1);
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
		final var n = 3;
		final var keys = nats(n).map(DataMatrix::index_to_excel_name).collect(Collectors.joining(","));
		final var ra = ra(keys);
		final var dfm = Lisp.cph(RPTA(nats(n).data(), n)) //
				.map(e -> ra.attachdup(e)).collect(Lisp.aaclc(27, DFrame::new));
		println(dfm);
		println(dfm.shape());
		println("-----------------------------------------------");
		final var nd = dfm.dataOf(INdarray::of) //
				.fmap(e -> ra.attachcln(e)).sortBy(e -> e.filter("B,C"), false);
		println(nd.nx(1));
	}

	@Test
	public void qux() {
		final var ra = ArrayRecord.of("a,b,c");
		ra.attach(A(1, 2, 3));
		ra.set(2, 2000);
		println(ra.dresscln("a1,b1,c1"));
	}

}
