package gbench.sandbox.matrix.linalg;

import org.junit.jupiter.api.Test;

import gbench.util.matrix.Matrices;
import gbench.util.matrix.Matrix;

import static gbench.util.array.INdarray.nds;
import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.nd;
import static gbench.util.io.Output.println;
import static gbench.util.type.Streams.intS;
import static gbench.util.matrix.Matrices.dblx;

import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

/**
 * 
 * @author gbench
 *
 */
public class QrTest {

	@Test
	public void foo() {
		final var m = 10;
		final var n = m;
		final var mod = n;
		final var data = // nd(1, 1, 1, -1, 0, -1, 0, 1, 1).dbls();
				nats(m * n).fflat().fmap(e -> new Random().nextInt(10)).dbls();
		final var q = nd(i -> 0, m * n).dbls(); // 表示正交方向:最多n个由data列向量定义上限个数
		final var r = nd(i -> 0, n * n).dbls(); // r 表示data列项量(n个)在q方向上(最多n个)的投影,因此是一个 nxn 矩阵
		println("--------------------------------------------data");
		println(data.toString(n));
		final var qq = q.cols(mod);
		final var rr = r.cols(mod);
		final var aa = data.cols(mod);
		qq.get(0).assign(nds(aa.get(0).normalize())); // 使用第一条向量初始化
		rr.get(0).get(0).set(aa.get(0).norm()); // 初始化r0,0的一个元素
		println("-------------------------------------------- q,r generating");
		intS(1, n).forEach(i -> { // 当前执行位置:i表示正在计算的向量
			final var ai = aa.get(i).dbls();
			final var ri = rr.get(i);
			final var js = nats(i); // 已经完成的向量索引j的集合[0,i)
			js.forEach(j -> ri.get(j).set(ai.proj(qq.get(j)))); // j表示已经计算完成的向量。
			final var _qi = ai.subacc(qq.lis((j, _q) -> _q.mul(ri.get(j)), js)); // qi剔除投影向量后剩余的部分
			ri.get(i).set(_qi.norm()); // 记录ri在_qi上的投影,r元素生成
			final var qi = qq.get(i).assign(nds(_qi.normalize())); // 归一化向量:q向量生成
			println("--orthogonal");
			intS(0, i + 1).forEach(_j -> { // 正交性检测
				println(i, _j, String.format("%.2f", qi.dot(qq.get(_j))), qi, qq.get(_j));
			});
		}); // forEach
		println("----------------------------------------------r");
		println(r.toString(n));
		println("----------------------------------------------q");
		println(q.toString(n));
		println(qq.lis(e -> e.norm(), nats(mod)));
		println("----------------------------------------------");
		final var qm = Matrix.of(q, m, n);
		final var rm = Matrix.of(r, m, n);
		println(qm.nd().toString(mod));
		println("--");
		println(rm.nd().toString(mod));
		println("--");
		println(Matrices.mmult(qm, rm).toMatrix().nd().toString(mod));
		println("----------------------------------------------");
	}

	@Test
	public void bar() {
		final var rnd = new Random();
		final var m = 10;
		final var n = 10;
		final var data = Matrix.of(nats(m * n).fmap(e -> rnd.nextInt(100)).dbls(), m, n);
		final var mod = data.ncol();
		println(data);
		final var qr = Matrices.qr(data);
		final var q = qr.get(dblx("Q"));
		final var r = qr.get(dblx("R"));
		println("-----------------------------");
		println("--q");
		println(q);
		println("t(q)*q:列向量正交");
		println(q.transpose().mmult(q).nd().toString(mod));
		println("--r");
		println(r);
		println("-----------------------------");
		final var _data = q.mmult(r);
		println("-----------------------------");
		println(_data.nd().toString(mod));
		println("diff:");
		final BinaryOperator<Matrices<Double>> subtract = Matrices.subtract();
		println(Stream.of(data.toMatrices(), _data).reduce(subtract));
	}

}
