package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.lisp.Lisp.CONS;
import static java.util.Arrays.asList;

import java.util.Arrays;

public class LispTest {

	@Test
	public void cons() {
		println("CONS(1,null)", asList(CONS(1, null)));
		println("CONS(null, null)", asList(CONS(null, null)));
		println("CONS(1, A(2, 3))", asList(CONS(1, A(2, 3))));
		println("CONS(null, A(2, 3))", asList(CONS(null, A(2, 3))));
	}

	/**
	 * 时间排序
	 */
	@Test
	public void bar() {
		final var rb = IRecord.rb("price,create_time");
		final var data = DFrame.of(Arrays.asList( //
				rb.get(100, "2024-05-18 09:01:01"), //
				rb.get(102, "2024-05-18 09:01:02"), //
				rb.get(102, "2024-05-17 09:01:01")));
		final var data1 = data.sorted(IRecord.cmp("price,create_time", false, true)); // 价格倒序, 时间正序
		final var data2 = data.sorted(IRecord.cmp("price,create_time", false, false)); // 价格倒序, 时间倒序
		final var data3 = data.sorted(IRecord.cmp("price,create_time", true, true)); // 价格正序, 时间正序
		final var data4 = data.sorted(IRecord.cmp("price,create_time", true, false)); // 价格正序, 时间倒序
		println("价格倒序, 时间正序", data1);
		println("价格倒序, 时间倒序", data2);
		println("价格正序, 时间正序", data3);
		println("价格正序, 时间倒序", data4);
	}

}
