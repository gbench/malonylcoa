package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

public class Ndarray4Test {

	@Test
	public void foo() {
		final var data = nd(i -> i, 5);
		println("build1", nd(i -> data.build1(i, 3), data.length()));
		println("build2", nd(i -> data.build2(i, 3), data.length()));
	}

	@Test
	public void bar() {
		final var data = nd(i -> i, 5);
		println("data", data);
		println("build");
		println("data.build1(3, 2).shift(-1)", data.build1(3, 2).shift(-1));
		println("data.build1(3, 2).shift(-1).resize(3)", data.build1(3, 2).shift(-1).resize(3));
		println("data.build1(3, 2).shift(1)", data.build1(3, 2).shift(1));
		println("data.build1(3, 2).shift(-3)", data.build1(3, 2).shift(-3));
		println("data.build1(3, 2).shift(-4)", data.build1(3, 2).shift(-4));
		println("resize");
		println(data.build1(3, 1));
		println("data.build1(3,1).resize(2)", data.build1(3, 1).resize(2));
		println("data.build1(3,1).resize(3)", data.build1(3, 1).resize(3));
		println("data.build1(3,1).resize(-2)", data.build1(3, 1).resize(-2));
		println("data.build1(3,1).resize(-3)", data.build1(3, 1).resize(-3));
		println("data.build1(3,1).resize(-4)", data.build1(3, 1).resize(-4));
		println("data.build1(3,1).resize(-5)", data.build1(3, 1).resize(-5));
	}

	@Test
	public void qux() {
		final var data = nd(i -> i * 1.0, 25).dupdata();
		println(data);
		println(data.row(5, 2));
		println(data.col(5, 1));
		println(data.row(5, 2).dot(data.col2(5, 1)));

	}

	@Test
	public void quz() {
		final var data = nd(i -> i, 50);
		data.traverseS(e -> e.length() <= 1 ? null : e.splits(e.length() / 2)).forEach(e -> {
			println(e);
		});
		println();
		println(data.build2(10, 5).concat(1, 10, 0, 0));
	}

	@Test
	public void d1() {
		final var aa = new Object[] { 1, 2d, "dd" };
		println(aa);
		println(nd(aa).product());
	}

}
