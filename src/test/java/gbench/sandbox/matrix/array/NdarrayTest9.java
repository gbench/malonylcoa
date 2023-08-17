package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.ArrayRecord.ra;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.lisp.IRecord.RPTA;
import static gbench.util.lisp.Lisp.cph;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;

/**
 * 
 */
public class NdarrayTest9 {

	@Test
	public void foo() {
		println("NdarrayTest9.foo");
		final var nd = cph(RPTA(A(1, 2, 3), 3)).map(e -> nd(e).dupdata()).collect(INdarray.ndclc());
		println("nd", nd);
		final var ra = ra("a,b,c");
		ra.attach(A(1, 2, 3));
		ra.set(2, 2000);
		println("ra.dresscln", ra.dresscln("a1,b1,c1".split(",")));

		println("groupBy:=======================");
		final var groups = nd.groupBy(e -> e.get(1));
		groups.forEach((k, v) -> {
			println("------------------------");
			println(k, v);
		});
		println("------------------------");
		println(nd.nx(1));
		println("------------------------");
		final var classifiers = ra("a,b,c").generateS("c,b",
				(BiFunction<INdarray<Integer>, Integer, Integer>) INdarray::get);
		final var pvt = nd.pivotTable(e -> e, classifiers); // 倒序显示。
		println("pivotTable:=======================");
		println(pvt);
		println("------------------------");
		println(nd.nx(1));
	}

}
