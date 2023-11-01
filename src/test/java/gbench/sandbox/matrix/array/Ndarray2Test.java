package gbench.sandbox.matrix.array;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;

import java.util.Arrays;

import gbench.util.array.INdarray;
import gbench.util.array.StandardNdarray;
import gbench.util.matrix.Matrix;

public class Ndarray2Test {

	@Test
	public void foo() {
		final var nd = Matrix.of((i, j) -> i * j, 10, 10);
		println(nd.column(1));
	}

	@Test
	public void bar() {
		final var nd = INdarray.of(i -> i, 10);
		if (nd instanceof StandardNdarray) {
			((StandardNdarray<Integer>) nd).add_method_interceptor("set", //
					(target, args) -> { // interceptor
						println(target, Arrays.asList(args));
					}); // (target, args)
		} // if
		nd.reverse();
	}

	/**
	 * 通过ndarray 获得行列数据，可以修改矩阵的回调函数。
	 */
	@Test
	public void quz() {
		final var mx = Matrix.of((i, j) -> i * j, 5, 5).toMatrices();
		final var row_3 = mx.ndrow(3);
		final var col_1 = mx.ndcol(1);
		println(mx);
		println("-----------------------------------");
		println(row_3.assign(A(10, 20, 40)).reverse());
		println(col_1.assign(A(-1, -2, -3)).sorted((a, b) -> b - a));
		println("-----------------------------------");
		println(mx);
		//
		final var indices = row_3.map((i, e) -> i).mapToInt(i -> i).toArray();
		println(row_3.splits(indices));
		println("----------------", row_3.partition(2));

	}

	@Test
	public void qua() {
		final var row = INdarray.of(i -> i, 10);
		final var idx = row.map((i, e) -> i).mapToInt(i -> i).toArray();
		println(row.splits(idx));
	}

	/**
	 * 切分长度
	 */
	@Test
	public void qub() {
		println("nats(10).cuts(2, 3, 4)", nats(10).cuts(2, 3, 4));
		println("nats(10).splits(0, 2, 5, 9)", nats(10).splits(0, 2, 5, 9));
	}

	/**
	 * 切分长度
	 */
	@Test
	public void quc() {
		println("nats(10).prepend(-1)", nats(10).prepend(-1));
		println("nats(10).append(11)", nats(10).append(11));
	}

}
