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
		final var nd = Lisp.cph(RPTA(A(1, 2, 3), 3)).map(e -> INdarray.nd(e).dup()).collect(INdarray.ndclc());
		println(nd);
		final var ra = ArrayRecord.of("a,b,c");
		ra.attach(A(1, 2, 3));
		ra.set(2, 2000);
		println(ra.dressupClone("a1,b1,c1".split(",")));
		final var groups = nd.groupBy(e -> e.get(1));
		groups.forEach((k, v) -> {
			println("------------------------");
			println(k);
		});
		println("------------------------");
		println(nd.nx(1));
		println("------------------------");
		@SuppressWarnings("unchecked")
		final var pvt = nd.pivotTable(e -> e, e -> -e.get(0), e -> -e.get(1));
		println(pvt);
		println(nd.nx(1));

	}

}
