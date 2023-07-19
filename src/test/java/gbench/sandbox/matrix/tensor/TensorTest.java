package gbench.sandbox.matrix.tensor;

import static gbench.util.io.Output.println;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import gbench.util.array.Tensor;
import static gbench.util.array.Partitioner.P;
import static gbench.util.array.Partitioner.P2;

public class TensorTest {

	@Test
	public void foo() {
		final var p = P(2, P(8, P(4, 5)), 8); // 定于区间划分范围
		final var strides = p.strides(true);
		println("---------------------------------");
		println("partitioner");
		strides.forEach((k, v) -> {
			println(Arrays.asList(k), v);
		});
		println("---------------------------------");
		final var nd = Tensor.nd(i -> i, p);
		println("ndarray", nd);
		final var np = nd.create(p);
		final var nd110 = np.getnd(1, 1, 0);
		nd110.set(3, 130);
		println("nd110", nd110);
		println("ndarray", nd);
		println("nd110.get(1,1,0,3)", nd110.get(1, 1, 0, 3));
		println("---------------------------------");
		np.partitions().forEach((index, n) -> {
			println(index, n);
		});
		println("---------------------------------");
		Tensor.of(i -> i, p.end()).partitions(p).forEach((index, n) -> {
			println(index, n);
		});
	}

	@Test
	public void quy() {
		final var dv = Tensor.of(i -> i, 8).nd();
		println(dv.empty());
		println("head", dv.head());
		println("tail", dv.tail());
		println("last", dv.last());
		println("initial", dv.initial());
		println("car,cdr", dv.carcdr());
		println("--------------------");
		dv.headS().forEach(e -> {
			println(e);
		});
		println("--------------------");
		dv.tailS().forEach(e -> {
			println(e);
		});
		println("--------------------");
		dv.forEach(e -> {
			println("----", e);
		});
		final var d1 = dv;
		final var d2 = Tensor.of(i -> i, 8 * 2).nd();
		println("d1.compareTo(d2)", d1.compareTo(d2));
		println("d2.compareTo(d1)", d2.compareTo(d1));
		println("d1.compareTo(d2.head(8))", d1.compareTo(d2.head(8)));
		println("d1.compareTo(d1)", d1.compareTo(d1));
	}

	@Test
	public void qux() {
		println((new int[] { 1, 2 }).equals(new int[] { 1, 2 }));
		println(Arrays.asList(1, 2).equals(Arrays.asList(1, 2)));
	}

	/**
	 * 
	 */
	@Test
	public void quz() {
		final var p = P(1, P(2, 3));
		p.strides(true);
		println("--------------------------");
		final var p2 = P2("0", 1, "1", P2("0", 2, "gbench", 3));
		p2.strides(true);
		p2.elemS().forEach(e -> {
			println(e.paths(), e);
		});

	}
}