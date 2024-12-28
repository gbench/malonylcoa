package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.ArrayRecord.ra;
import static gbench.util.lisp.IRecord.RPTA;
import static gbench.util.lisp.Lisp.cph;

import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.data.xls.DataMatrix;

/**
 * 
 */
public class Ndarray9Test {

	@Test
	public void foo() {
		final var n = 4;
		final var npk = nats(n).fmap(DataMatrix::index_to_excel_name);
		final var keys = npk.collect(Collectors.joining(","));
		println("NdarrayTest9.foo");
		final var nd = cph(RPTA(nats(n).data(), n)).map(e -> nd(e).dupdata()).collect(INdarray.ndclc());
		println("nd", nd);
		final var ra = ra(keys);
		ra.attach(nats(n).data());
		ra.set(2, 2000);
		println("ra.dresscln", ra.dresscln(npk.map(i -> String.format("%s1", i)).toArray(String[]::new)));

		println("groupBy:=======================");
		final var groups = nd.groupBy(e -> e.get(1));
		groups.forEach((k, v) -> {
			println("------------------------");
			println(k, v);
		});
		println("------------------------");
		println(nd.nx(1));
		println("------------------------");
		final var classifiers = ra(keys).generateS("D,C,B",
				(BiFunction<INdarray<Integer>, Integer, Integer>) INdarray::get);
		final var pvt = nd.pivotTable(e -> e, classifiers); // 倒序显示。
		println("pivotTable:=======================");
		println(pvt);
		println("------------------------");
		println(nd.nx(1));
	}

}
