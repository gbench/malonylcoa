package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.array.INdarrayX.minorNxFn;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.RPTA;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Streams;
import gbench.util.array.INdarray;

public class NdarrayX2Test {

	/**
	 * 余子式
	 */
	@Test
	public void foo() {
		final var nx = nats(16).nx(4);
		println(nx);
		println("------------------------");
		final var mij = minorNxFn(nx);
		Stream.iterate(0, i -> i + 1).limit(4).map(i -> mij.apply(i, 0)).forEach(e -> {
			println(e);
			println("========");
		});
		final var nx1 = nats(1).nx(1);
		final var nx1_minor = minorNxFn(nx1).apply(0, 0);
		println("", nx1_minor.length());

	}

	/**
	 * 矩阵行列式
	 */
	@Test
	public void bar() {
		final var n = 10;
		final var nx = nats(n * n).fmap(i -> new Random().nextInt(10)).nx(n);
		println(String.format("np.linalg.det(np.array(%s).reshape(%s,%s))", nx.head(nx.length()), n, n));
		println(nx.nx(n).det());
	}

	/**
	 * 矩阵行列式
	 */
	@Test
	public void bar2() {
		final var n = 5;
		final var nx = nats(n * n).fmap(i -> new Random().nextInt(10)).nx(n);
		final var tt = nx.qr();
		println("r", tt._2());
		println("----------------------------------");
		println("t.det", tt._2.diagonal().product());
		println("q.det", tt._1.det());
		println("nx.det", nx.det());
		println(String.format("np.linalg.det(np.array(%s).reshape(%s,%s))", nx.head(nx.length()), n, n));
	}

	/**
	 * 矩阵行列式
	 */
	@Test
	public void quz() {
		final var n = 7;
		final var nx = nats(n * n).fmap(i -> new Random().nextInt(10)).nx(n);
		println(nx.mmult(nx.inv()));
	}

	@Test
	public void qua() {
		final var n = 4;
		final var a = nats(n).data();
		Lisp.cph(RPTA(a, n)).forEach(e -> {
			println(e);
		});
		println(nats(10).fmap(i -> i + 1).product());
	}

	@Test
	public void qub() {
		final var n = 5;
		final var a = nats(n).data();
		final var ar = new AtomicReference<INdarray<Integer>>();
		// 全排列的实现
		Lisp.cph(RPTA(a, n)).map(e -> {
			if (ar.get() == null) {
				ar.set(INdarray.nd(e));
			}
			return ar.get();
		}).filter(e -> {
			final var set = new HashSet<Integer>();
			for (int i = 0; i < e.length(); i++) {
				set.add(e.get(i));
				if (set.size() < i + 1) { // 发现重复值
					return false;
				}
			}
			return true;
		}).forEach(e -> {
			println(e);
		});
	}

	/**
	 * 行列式求助
	 */
	@Test
	public void quc() {
		final var n = 4;
		final var rand = new Random();
		final var nx = nats(n * n).fmap(e -> rand.nextInt(100)).nx(n);
		println(nx);
		println("------------------------------");
		final var det0 = Lisp.permute(nats(n).data()).map(e -> {
			final var t = Streams.intS(e.length).boxed().map(i -> nx.row(i).get(e[i])).collect(INdarray.ndclc());
			final var p = Tuple2.of(nd(e).tau(), t);
			println(p);
			return p;
		}).map(e -> (e._1 % 2 == 0 ? 1 : -1) * e._2.product()).reduce(0, Integer::sum);
		final var det1 = nx.det().intValue();
		println("----------------------------");
		println("det0", det0, "det1", det1, "flag", Objects.equals(det0, det1));

	}

}
