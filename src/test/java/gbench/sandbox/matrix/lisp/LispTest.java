package gbench.sandbox.matrix.lisp;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;

public class LispTest {

	@Test
	public void aaclc() {

		for (int x = 0; x < n; x++) {
			final var s = Stream.iterate(0, i -> i + 1).limit(1000000) //
					.map(i -> i < 1000 ? null : i).map(e -> {
						return Tuple2.of(Thread.currentThread().getName(), e);
					}).collect(Lisp.aaclc(1000000, null, INdarray::of));
			println("AACLC", s.length());
		}
	}

	@Test
	public void ndclc() {

		for (int x = 0; x < n; x++) {
			final var s = Stream.iterate(0, i -> i + 1).limit(1000000) //
					.map(i -> i < 1000 ? null : i).map(e -> {
						return Tuple2.of(Thread.currentThread().getName(), e);
					}).collect(INdarray.ndclc());
			println("NDCLC", s.length());

		}
	}

	@Test
	public void aaclc_parallel() {

		for (int x = 0; x < n; x++) {
			final var s = Stream.iterate(0, i -> i + 1).limit(1000000) //
					.map(i -> i < 1000 ? null : i).parallel().map(e -> {
						return Tuple2.of(Thread.currentThread().getName(), e);
					}).collect(Lisp.aaclc(100000, null, INdarray::of));
			println("AACLC_PARALLEL", s.length());
		}
	}

	@Test
	public void ndclc_parallel() {

		for (int x = 0; x < n; x++) {
			final var s = Stream.iterate(0, i -> i + 1).limit(1000000) //
					.map(i -> i < 1000 ? null : i).map(e -> {
						return Tuple2.of(Thread.currentThread().getName(), e);
					}).collect(INdarray.ndclc());
			println("NDCLC_PARALLEL", s.length());

		}
	}

	/**
	 * 不同批次大小的比较
	 */
	@Test
	public void aaclc_parallel2() {
		for (var batch_size : INdarray.nd(10, 100, 1000, 10000, 100000, 1000000)) {
			long total = 0;
			for (int k = 0; k < n; k++) {
				final var begtime = System.currentTimeMillis();
				final var s = Stream.iterate(0, i -> i + 1).limit(1000000) //
						.map(i -> i < 1000 ? null : i).parallel().map(e -> {
							return Tuple2.of(Thread.currentThread().getName(), e);
						}).collect(Lisp.aaclc(batch_size, null, INdarray::of));
				final var endtime = System.currentTimeMillis();
				final var lasts = endtime - begtime;

				println("AACLC_PARALLEL ", "batch_size", batch_size, "len", s.length(), "lasts", lasts, "total",
						total += lasts, "average", total * 1d / (k + 1));
			}
		}
	}

	@Test
	public void aaclc_parallel_null() {

		for (int x = 0; x < n; x++) {
			final var s = Stream.iterate(0, i -> i + 1).limit(1000000) //
					.map(i -> null).parallel().collect(Lisp.aaclc(10000, null, INdarray::of));
			println("AACLC_PARALLEL_null", s.length());
		}
	}

	@Test
	public void foo() {
		final var nd = Stream.iterate(0, i -> i + 1).limit(10) //
				.map(i -> i < 5 ? null : i).collect(Lisp.aaclc(4, null, INdarray::of));
		println(nd);

		println("all null", Stream.iterate(0, i -> i + 1).limit(10) //
				.map(i -> null).collect(Lisp.aaclc(4, null, INdarray::of)));
	}

	@Test
	public void foo1() {
		println("------------------");
		for (int k = 0; k < 1; k++) {
			final var nd100 = Stream.iterate(0, i -> i + 1).limit(100).parallel() //
					.map(i -> Math.random() < 0.5 ? null : i).map(e -> {
						return Tuple2.of(Thread.currentThread().getName(), e);
					}).peek(e -> {
						assert e != null : "error";
					}).collect(Lisp.aaclc(10, null, INdarray::of));
			println("AACLC", nd100, "len", nd100.length());
			nd100.collect(Collectors.groupingBy(e -> e._1)).forEach((key, v) -> {
				final var vv = v.stream().map(e -> e._2).toList();
				println(key, vv.size(), vv);
			});
		}
	}

	int n = 10;

}
