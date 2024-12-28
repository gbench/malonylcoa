package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.ndbop;
import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.ndclc;
import static gbench.util.array.INdarray.pts;
import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.io.Output;

/**
 * INdarray 的矩阵操作
 * 
 * @author gbench
 *
 */
public class Ndarray5Test {

	@Test
	public void foo() {
		final var data = nats(20);
		final var cols = data.cols2(5);
		println("----------------------------");
		println(data.toString(5));
		println("----------------------------");

		// 按照列统计累加和,多余的列被返回null
		println(cols.map(INdarray::sum, nats(8)).collect(ndclc()));
		println(cols.reduce(ndbop((a, b) -> a + b), nats(5)));

		data.col(5, 2).assign(pts(t -> -t));
		println("----------------------------");
		println(data.toString(5));
	}

	@Test
	public void bar() {
		final var data = INdarray.nats(100);
		data.rowS(10).forEach(Output::println);
		println("---------------------------");
		data.colS(10).forEach(Output::println);
	}

	@Test
	public void quz() {
		final var data_ints = INdarray.nats(10);
		println(data_ints == data_ints.ints());
		final var data_nums = data_ints.nums(1);
		println(data_ints == data_nums);
		final var data_dbls = data_ints.dbls();
		println(data_ints.nums(1d) == data_dbls, data_ints.nums(1d).equals(data_dbls));
		println(",nats(10).equals(nats(10))", nats(10).equals(nats(10)));
		println("nats(10)==nats(10)", nats(10) == nats(10));
	}

}
