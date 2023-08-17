package gbench.sandbox.matrix.linalg;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.array.INdarrayX;
import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Streams;

public class DeterminantTest {
	/**
	 * 行列式求解
	 */
	@Test
	public void foo() {
		final var n = 5;
		final var rand = new Random();
		final var nx = nats(n * n).fmap(e -> rand.nextInt(100)).nx(n).dblx();
		println(nx);
		println("------------------------------");
		final var ai = new AtomicInteger();
		final var det0 = Lisp.permute(nats(n).data()).map(e -> {
			final var t = Streams.intS(e.length).boxed().map(i -> nx.row(i).get(e[i])).collect(INdarray.ndclc());
			final var p = Tuple2.of(nd(e).tau(), t);
			println(ai.getAndIncrement(), p);
			return p;
		}).map(e -> (e._1 % 2 == 0 ? 1d : -1d) * e._2.product()).reduce(0d, Double::sum);
		final var det1 = nx.det();
		println("----------------------------");
		println("det0", det0, "det1", det1, "flag", Objects.equals(det0, det1), "error", det0 - det1);
	}

	/**
	 * 行列式求解
	 */
	@Test
	public void foo1() {
		final var n = 6;
		final var rand = new Random();
		final var nx = nats(n * n).fmap(e -> rand.nextInt(100)).nx(n).dblx();
		println(nx);
		println("------------------------------");
		final var det0 = INdarrayX.det1(nx);
		final var det1 = nx.det();
		println("----------------------------");
		println("det0", det0, "det1", det1, "flag", Objects.equals(det0, det1), "error", det0 - det1);
	}

	/**
	 * 行列式求解:palu方法
	 */
	@Test
	public void foo2() {
		final var n = 5;
		final var rand = new Random();
		final var nx = nats(n * n).fmap(e -> rand.nextInt(100)).nx(n).dblx();
		println(nx);
		println(String.format("np.linalg.det(np.array(%s).reshape(%s,%s))", nx.head(nx.length()), n, n));
		println("------------------------------");
		final var palu = nx.dupdata().lu(); // lu 会修改 源矩阵，这里复制一下求行列式
		final var p = palu._1;
		final var u = palu._2._2;
		println(p);
		println("------------------------------l");
		println(palu._2._1);
		println(palu._2._1.det());
		println("------------------------------");
		final var _p = p.rowS().map(INdarrayX::ohDecode).collect(INdarray.ndclc());
		println("_p", _p, "tau", _p.tau());
		println(u);
		println("------------------------------");
		final var det0 = INdarrayX.det0(nx);
		final var det1 = Math.pow(-1, _p.tau()) * u.diagonal2().product();
		println("u.det", u.det());
		println("p.det", p.det());
		println("----------------------------");
		println("det0", det0, "det1", det1, "flag", Objects.equals(det0, det1), "error", det0 - det1);
	}

	/**
	 * 行列式求解:palu方法
	 */
	@Test
	public void foo3() {
		for (int i = 0; i < 40; i++) { //
			println(String.format("-------------------[%s]---------------------", i));
			final var n = i;
			final var begtime = System.currentTimeMillis();
			final var rand = new Random();
			final var nx = nats(n * n).fmap(e -> rand.nextInt(100)).nx(n).dblx();
			println(String.format("np.linalg.det(np.array(%s).reshape(%s,%s))", nx.head(nx.length()), n, n));
			println(INdarrayX.det2(nx));
			println(String.format("last for: %d ms", (System.currentTimeMillis() - begtime)));
		}
	}

}
