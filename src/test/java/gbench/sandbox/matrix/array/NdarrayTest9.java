package gbench.sandbox.matrix.array;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.lisp.IRecord.RPTA;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.lisp.ArrayRecord;
import gbench.util.lisp.Lisp;

public class NdarrayTest9 {

	@Test
	public void foo() {
		println("NdarrayTest9.foo");
		final var nd = Lisp.cph(RPTA(A(1, 2, 3), 3)).map(e -> INdarray.nd(e).dup()).collect(INdarray.ndclc());
		println("nd", nd);
		final var ra = ArrayRecord.of("a,b,c");
		ra.attach(A(1, 2, 3));
		ra.set(2, 2000);
		println("ra.dressupClone", ra.dressupClone("a1,b1,c1".split(",")));

		println("groupBy:=======================");
		final var groups = nd.groupBy(e -> e.get(1));
		groups.forEach((k, v) -> {
			println("------------------------");
			println(k, v);
		});
		println("------------------------");
		println(nd.nx(1));
		println("------------------------");
		@SuppressWarnings("unchecked")
		final var pvt = nd.pivotTable(e -> e, e -> -e.get(2), e -> -e.get(0)); // 倒序显示。
		println("pivotTable:=======================");
		println(pvt);
		println("------------------------");
		println(nd.nx(1));
	}

}
