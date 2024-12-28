package gbench.sandbox.matrix;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import gbench.global.Globals;
import gbench.util.array.INdarray;
import gbench.util.array.Tensor;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.matrix.Matrices;
import gbench.util.matrix.Matrix;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.lisp.IRecord.RPTA;
import static gbench.util.matrix.Matrices.*;
import java.util.stream.Stream;

public class MatrixTest {

	@Test
	public void foo() {
		final var t = Stream.of(V(10), V(20)).reduce(Matrices.add());
		println(t);
	}

	@Test
	public void bar() {
		final var t = Stream.of(VT(3), diag(1, 2, 3), V(3)).reduce(Matrices.add()).get();
		println(t);
		println("-------------------");
		final var s = Stream.of(VT(1d, 2d, 3d), diag(1d, 2d, 3d), V(1d, 2d, 9d)) //
				.reduce(Matrices.mmult()).get();
		println(s);
		println("-------------------");
		println(VT(null, 1, 2).entrieS().map(e -> e._2).collect(Matrix.plus_clc()));
		println("-------------------");
		println(VT(3));
		println("-------------------");
		println(V(3));
		println("-------------------");
		println(VT(3).mmult(V(3)));
	}

	@Test
	public void quz() {
		final var ts = Tensor.of(V(32).data(), 2, 2, 2, 2, 2);
		println(ts.at(0, 0, 0, 0, 0), ts.at(1, 1, 0, 0, 0), ts.at(1, 1, 1, 1, 1));
		println(ts);
		println("-------------------");
		// 遍历tensor结构
		Tensor.cph(RPTA(A(0, 1), 5)).forEach(index -> {
			println(VT(index), "----------->", ts.at(index));
		});
		println("-------------------");
		final var tensor = Tensor.of(V(32).data(), 2, 2, 4, 2);
		println(tensor);
		println("-------------------");
		println("(0,1)", tensor.get(0, 1));
		println("-------------------");
		println("(1,1)", tensor.get(1, 1));
		println("-------------------");
		println("tensor.get(1, 1).get(2)", tensor.get(1, 1).get(2));
		println("tensor.get(1, 1).get(2,1) reach element level return null", tensor.get(1, 1).get(2, 1)); //
		final var path = new ArrayList<Tensor<Integer>>();
		println("tensor.get(1, 1).get(2,1) reach element level return null 2", tensor.getItem(path, 1, 1, 2, 1)); //
		println("tensor.get(1, 1).get(2).at(1)", tensor.get(1, 1).get(2).at(1));
		tensor.writer(1, 1, 2, 1).accept(100);
		println("tensor.get(1, 1).get(2).at(1)", tensor.get(1, 1).get(2).at(1));
		println("-------------------");
		println("tensor after set", tensor);
		println("=============================================");
		path.forEach(e -> {
			println("--------------");
			println(e);
		});
	}

	/**
	 * 
	 */
	@Test
	public void qux() {
		final var tx = Tensor.of(i -> i, 10).reshape(5, 2);
		final var ty = Tensor.of(i -> i * 2, 10).reshape(5, 2);
		final var z = tx.op(ty, (a, b) -> a + b);
		println(z);
		println(tx.equals(ty));
		println(tx.compareTo(ty));
		Tensor.of(i -> i, 32).reshape(2, 2, 2, 2, 2).tupleS(Tensor::of).forEach(e -> {
			println(e);
		});
		println("-------------------------");
		Tensor.of(i -> i, 32).reshape(2, 2, 2, 2, 2).itemS(1).forEach(e -> {
			println("------------");
			println(e);
		});

		println("-------------------------");
		Tensor.of(i -> i, 8).reshape(2, 2, 2).flatMapS().forEach(e -> {
			println("===", e.isLeaf(), e.length());
			println(e);
		});
		println("-------------------------");
		Tensor.of(i -> i, 8).reshape(2, 2, 2).flatMapS().filter(e -> e.isLeaf()).forEach(e -> {
			println("===", e.isLeaf(), e.length());
			println(e);
		});

	}

	@Test
	public void quy() {
		final var tensor = Tensor.of(i -> i, 8).reshape(2, 2, 2);
		println(tensor);
		println("----------------------");
		final var arrays = tensor.partitions(9, 5, 3); // 9 号无效, 与 toDataViews(3,5) 等价
		println(arrays);
		println(tensor.partitions(3, 5));
		println("----------------------");
		arrays.get(0).set(2, 20);
		println(tensor);
		println("----------------------");
		println("---", arrays.get(1).arrayOf(Tensor::of));
		arrays.get(1).forEach(e -> {
			println(e);
		});
	}

	@Test
	public void quc() {
		final var x = Matrix.of((i, j) -> i + j, 5, 5).mutate((Matrix<Integer> e) -> e.nd());
		println(x.arrayOf(e -> e));
	}

	@Test
	public void matrix() {
		try (final var excel = SimpleExcel
				.of(Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/matrix/data.xlsx")) {
			final var x = excel.autoDetect(0).row2S(INdarray::of).flatMap(e -> e.stream()).collect(INdarray.nxclc(6));
			println(x);
			println(x.colS(6).map(e -> e.fflat().sum()).collect(INdarray.ndclc()));
		}
	}
}
