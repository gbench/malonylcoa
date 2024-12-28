package gbench.sandbox.matrix.linalg;

import org.junit.jupiter.api.Test;

import java.util.stream.*;
import gbench.util.array.INdarray;
import gbench.util.matrix.Matrices;

import static gbench.util.array.INdarray.dot;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.matrix.Matrices.*;

/**
 * 解方程
 * 
 * @author gbench
 *
 */
public class LinalgTest {

	/**
	 * Ax=b的PA=LU方法求解
	 * 
	 * @param A 系数矩阵
	 * @param b 值向量
	 * @return
	 */
	public static INdarray<Double> solve(final Matrices<? extends Number> A, final Matrices<? extends Number> b) {
		final var palu = Matrices.lu(A);
		final var p = palu.get(dblx("P"));
		final var l = palu.get(dblx(0));
		final var u = palu.get(dblx("U"));
		final var pb = MMULT(p, b); // Pb 相乘
		final var c = pb.duplicatex().nd();
		final var n = c.length(); // 结果向量维度

		// 一次规约 Lc=Pb 获得 c 向量
		Stream.iterate(1, i -> i < n, i -> i + 1).reduce(c, (acc, i) -> { // lc = pb
			final var h1 = c.head(i);
			final var h2 = l.ndrow(i).head(i);
			final var dot = dot(h1, h2);
			c.set(i, c.get(i) - dot);
			return acc;
		}, (a1, a2) -> a1);

		// 二次规约 Ux = c
		final var x = Stream.iterate(0, i -> i < n, i -> i + 1).reduce(c, (acc, i) -> { // ux = c
			final var j = (n - 1) - i; // 与最后一行之间的距离,与对角线上元素之间的距离。即 已经计算完成的x值的个数。
			final var r = u.ndrow(j); // 行号
			final var dot = dot(r.tail(i), c.tail(i));
			c.set(j, (c.get(j) - dot) / r.get(j));
			return acc;
		}, (a1, a2) -> a1);
		return x;
	}

	/**
	 * Ax=b的PA=LU方法求解
	 * 
	 * @param A 系数矩阵
	 * @param b 值向量
	 * @return
	 */
	public static INdarray<Double> solve2(final Matrices<? extends Number> A, final Matrices<? extends Number> b) {
		final var palu = Matrices.lu(A);
		final var p = palu.get(dblx("P"));
		final var l = palu.get(dblx(0));
		final var u = palu.get(dblx("U"));
		final var pb = MMULT(p, b); // Pb 相乘
		final var c = pb.duplicatex().nd();
		final var n = c.length(); // 结果向量维度

		// 一次规约 Lc=Pb 获得 c 向量
		Stream.iterate(1, i -> i < n, i -> i + 1).forEach(i -> { // 从1开始
			c.set(i, c.get(i) - dot(l.ndrow(i).head(i), c.head(i)));
		});
		// 二次规约 Ux = c
		Stream.iterate(n - 1, i -> i >= 0, i -> i - 1).forEach(i -> { // 从n-1开始，最后一行
			final int j = (n - 1) - i; // 与最后一行之间的距离,与对角线上元素之间的距离。即 已经计算完成的x值的个数。
			final var irow = u.ndrow(i);
			final var coef = irow.get(i); // 对角线上的元素
			c.set(i, (c.get(i) - dot(irow.tail(j), c.tail(j))) / coef);
		});
		return c;
	}

	/**
	 * 使用palu求解线性方程组
	 */
	@Test
	public void palu() {
		final var A = Matrices.of(A(2, 1, 5, 4, 4, -4, 1, 3, 1), 3, 3);
		final var b = Matrices.of(A(5, 0, 6));
		println(A);
		println();
		println(b);
		println();
		println("root", solve2(A, b));
	}

}
