package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.vls;
import static gbench.util.io.Output.println;
import static gbench.util.array.INdarray.ufs;
import static gbench.util.array.INdarray.bfs;
import static gbench.util.array.INdarray.tfs;
import static gbench.util.array.INdarray.pts;
import static gbench.util.array.INdarray.nds;
import static gbench.util.array.INdarray.ets;
import static gbench.util.array.INdarray.nats;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.io.Output;

public class Ndarray7Test {

	@Test
	public void foo() {
		final var data = nats(100);
		data.rows(10).get(0).assign(ufs((t) -> 10 * t));
		data.rows(10).get(1).assign(bfs((i, t) -> 100 * t));
		data.rows(10).get(2).assign(tfs((nd, i, t) -> 1000 * t));
		data.rows(10).get(3).assign(vls(10000));
		data.rows(10).get(4).assign(vls(nats(10)));
		Output.println(data.toString(10));
	}

	@Test
	public void bar() {
		final var data = nats(100);
		data.cols(10).get(0).assign(pts((t) -> 10 * t));
		data.cols(10).get(1).assign(ets((i, t) -> 100 * t));
		data.cols(10).get(2).assign(nds(nats(1000)));
		data.cols(10).get(3).assign(pts(10000));
		Output.println(data.toString(10));
	}

	@Test
	public void quz() {
		final var m = INdarray.nats(100);
		println(m.toString(10));
		println("-----------------行交换------------------");
		final var t = m.row(10, 0).dupdata();
		m.row(10, 0).assign(vls(m.row(10, 1)));
		m.row(10, 1).assign(vls(t));
		println("----------------列交换-------------------");
		final INdarray<Integer> c = m.col(10, 8).fflat();
		m.col(10, 8).assign(nds(m.col(10, 9).fflat()));
		m.col(10, 9).assign(nds(c));
		println("-----------------------------------");
		println(c);
		println(m.toString(10));
	}

	/**
	 * INdarray的行列交换
	 */
	@Test
	public void qua() {
		final var m = INdarray.nats(100).nx(10);
		m.swap2(5, 1);
		m.swap(0, 7);
		println(m);
	}

}
