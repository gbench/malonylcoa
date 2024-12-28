package gbench.sandbox.matrix;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.type.Types.cast;

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.matrix.Matrices;
import gbench.util.matrix.Matrix;

public class Matrix4Test {

	@Test
	public void foo() {
		println(Lisp.head(1, 2, 3));
		println(Lisp.tail(1, 2, 3));
		println(Lisp.headTail(1, 2, 3).fmap2(Arrays::asList));
		println(Lisp.initalLast(1, 2, 3).fmap1(Arrays::asList));
		println(Lisp.initial(1, 2, 3));
		println(Lisp.last(1, 2, 3));
		println("head", Lisp.head());
		println("tail", Lisp.tail());
		println("head", Lisp.head(1));
		println("tail", Arrays.asList(Lisp.tail(1)));
	}

	@Test
	public void bar() {
		final var A = Matrix.of(A(3.3, 1, 2, 6, 3, 4, 3, 1, 5), 3);
		final var palu = Matrices.lu(A).toArray(cast((Matrices<Double>) null));
		final var l = palu[0];
//		final var u = palu[1];
//		final var p = palu[2];

		println(l);
		println();
		final int n = l.shape()[1];
		final var lij = Matrices.Lij(n);
		final var l1 = l.entrieS().filter(e -> e._1._1 > e._1._2).map(e -> lij.apply(e._1._1, e._1._2, e._2))
				.collect(Lisp.llclc());
		final var m1 = l1.stream().reduce(Matrices::mmult).get();
		println(m1);
		println();
		final var m2 = l.entrieS().filter(e -> e._1._1 > e._1._2).map(e -> lij.apply(e._1._1, e._1._2, -e._2))
				.collect(Lisp.llclcS(true, e -> e.reduce(Matrices::mmult).get())); // 调转后
		println(m2);
		println();
		println(m1.mmult(m2));
	}

	@Test
	public void qux() {
		final var ai = new AtomicInteger();
		final var m = Matrix.of((i, j) -> ai.getAndIncrement(), 5, 5).toMatrices();
		println(m);
		println();
		final var t = m.colS(Matrix::V).map(Tuple2.zipper2(m.rowS(Matrix::VT))) //
				.map(Tuple2.bifun(Matrices::mmult)) //
				.reduce(Matrices.add()).get(); // 谱分析
		println();
		println(t);
		println();
		println(m.mmult(m));

		println(Matrix.V(10));
	}

}
