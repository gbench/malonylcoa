package gbench.sandbox.matrix;

import org.junit.jupiter.api.Test;

import gbench.util.lisp.IRecord;
import gbench.util.matrix.Matrix;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gbench.util.io.Output.println;
import static gbench.util.matrix.Matrix.M;
import static gbench.util.matrix.Matrix.V;

/**
 * 
 * @author gbench
 *
 */
public class Matrix2Test {

	@Test
	public void foo() {
		final var mm = IRecord.cph(M((i, j) -> j, 2, 10).cells()).map(Matrix::of).map(e -> e.transpose())
				.collect(Collectors.groupingBy(e -> e.sum().intValue()));
		mm.forEach((k, v) -> {
			println(k, v.size(), v);
		});
		println("--------------------------------------");
		final var dd = mm.entrySet().stream().map(e -> IRecord.L(e.getKey(), e.getValue().size()))
				.collect(Matrix.rmxclc2());
		final var tt = dd.dblx() //
				.mutate((Matrix<Double> m) -> m.cbind(m.eval("ratio(C1)"))) // 添加频率列
				.mutate((Matrix<Double> m) -> m.cbind(m.eval("C0*C2"))) //
				.mutate((Matrix<Double> m) -> m.rbind(m.vrow(0).fmap(e -> 0d))); // 增加全零行

		println(tt);
		println("--------------------------------------");
		println(tt.vcol(3).sum());
		println("--------------------------------------");
		println(mm.entrySet().stream().map(e -> e.getKey() * e.getValue().size()).collect(Matrix.mxclc()));
		println("--------------------------------------");
		println(tt.eval(" R1 / R2 "));
		println(tt.eval("freq(c1)"));
		println(tt.eval("count(c1)"));
	}

	@Test
	public void bar() {
		final var mm = IRecord.cph(M((i, j) -> j, 2, 5).cells()).map(e -> Stream.of(e).collect(Collectors.toList()))
				.collect(Matrix.rmxclc2()).dblx();
		println(mm);
		println("==================##count");
		println(mm.vcol(1).eval("count(c0)"));
		println("==================##freq");
		println(mm.eval("freq(c1)"));
		println("==================##freq-sorted");
		println(mm.eval("freq(c1)").sorted());
		println("-----------------");
		println(V(10).ratio().scanl(e -> e.transpose().sum()).transpose());
	}

	@Test
	public void quz() {
		println(V(10).scanl(e -> e.transpose()));
	}
}
