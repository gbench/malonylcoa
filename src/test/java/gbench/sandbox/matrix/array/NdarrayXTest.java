package gbench.sandbox.matrix.array;

import static gbench.util.array.INdarray.nd;
import static gbench.util.array.INdarrayX.diagx;
import static gbench.util.array.INdarrayX.eyex;
import static gbench.util.io.Output.println;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * 矩阵操作接口
 * 
 * @author Administrator
 *
 */
public class NdarrayXTest {

	/**
	 * 矩阵属性
	 */
	@Test
	public void foo() {
		final var data = nd(i -> i, 10);
		final var nx10 = data.withNX(nx -> {
			final var _nx = nx.mmult(data).nx().mod(10);
			println("mx.cols()", _nx.cols().get(3));
			println("mx.rows()", _nx.rows().get(3));
			return _nx;
		});
		println(nx10);
	}

	/**
	 * 矩阵乘法
	 */
	@Test
	public void bar() {
		final var nx = nd(i -> i, 10).nx();
		println(nx);
		println("----------------------");
		println(nx.mmult(nx));
	}

	/**
	 * 矩阵QR分解
	 */
	@Test
	public void quz() {
		final int m = 10;
		final int n = 5;
		final var nx = nd(i -> new Random().nextInt(100), m * n).nx().mod(n);
		final var qr = nx.qr();
		println("-----------------------:nx");
		println(nx);
		println("-----------------------:q");
		println(qr._1);
		println("-----------------------:qt*q");
		println(qr._1.transpose().mmult(qr._1));
		println("-----------------------:r");
		println(qr._2);
		println("-----------------------:qxr");
		println(qr._1.mmult(qr._2).nx().mod(n));
		println("-----------------------:error");
		println(String.format("error:%.2f", qr._1.mmult(qr._2).nx().mod(n).sub(nx).sum()));
	}

	/**
	 * 矩阵转置 & 设置
	 */
	@Test
	public void qux() {
		final var nx = nd(i -> i, 5 * 2).nx().mod(2);
		println(nx);
		println("------------------");
		nx.set(3, 1, 31);
		println(nx);
		println("------------------");
		final var t_nx = nx.transpose();
		println(t_nx);
		t_nx.get(1, 1).set(100);
		println("------------------");
		println(nx);
	}

	@Test
	public void qua() {
		final var d = diagx(1, 2, 3);
		println(d);
		println("-------------------------:eye");
		println(eyex(10));
		println("-------------------------swap:row");
		println(eyex(10).swap(1, 0));
		println("-------------------------swap2:col");
		println(eyex(10).swap2(5, 6));
	}

	@Test
	public void qub() {
		final var a = nd(2, 1, 5, 4, 4, -4, 1, 3, 1).nx(3);
		final var b = nd(5, 0, 6).nx();
		println("--a");
		println(a);
		println("--");
		println(b);
		println("--");
		final var palu = a.dupdata().lu();
		final var p = palu._1;
		final var l = palu._2._1;
		final var u = palu._2._2;
		println("--p");
		println(p);
		println("--l");
		println(l);
		println("--u");
		println(u);
		println("--求解方程");
		println("root", a.dupdata().solve(b).transpose());
	}

}
