package gbench.sandbox.matrix.array;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.nats;
import static gbench.util.array.INdarray.ndint;
import static gbench.util.array.Partitioner.P;
import static gbench.util.array.Partitioner.P2;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.array.Tensor;
import gbench.util.type.Types;

public class NdarrayTest {

	@Test
	public void foo() {
		final var p = P(P(P(P(P(P(P(4))), 2, P(P(9)))))); // 定于区间划分范围
		final var strides = p.strides(true);
		println(strides);
		final var nd = Tensor.of(i -> i, p.end()).nd(p);
		println(nd.get(0, 0, 0, 2, 0, 0));
		println(nd.getnd(0, 0, 0, 2, 0, 0));
		println("--------------------------------------");
		// 生成根节点
		final var root = nd.trieTree();
		root.forEach(e -> {
			println(String.format("%s %s -----------> name: %s attr: %s", //
					"-|-".repeat(e.getLevel() - 1), //
					e.getPath(), e.getName(), e.attrs()));
		});
		println("pathOf(null, 0, 0, 0, 1)", root.pathOf(null, 0, 0, 0, 1).attrval(Types.cast(INdarray.class)));
		println("getNode(null, 0, 0, 0, 1)", root.getNode(null, 0, 0, 0, 1).attrval());
		println("getNode(\"null, 0, 0, 0, 1\")", root.getNode("null, 0, 0, 0, 1").attrval());
		println("getNode(\"null/0/0/0/1\")", root.getNode("null/0/0/0/1").attrval());
		// 结果转换
		println("pathOf(null, 0, 0, 0,1)", root.pathOf(null, 0, 0, 0, 1).attrval(ndint).reduce(Integer::sum));
	}

	@Test
	public void bar() {

		final var nd = Tensor.nd(i -> i, P(20));
		println(nd);
		nd.trieTree().forEach(e -> {
			println(e.parts(), e.attrval(ndint));
		});
		println("----------------------------------");
		println("nd.split(10)", nd.partitions(10));
		println("nd.splits(3, 5)", nd.partitions(3, 5));
		println("----------------------------------");
		println("nd.split(10)._1.split(3)", nd.partition(10)._1.partitions(3));
		println("nd.split(10)._2.split(3)", nd.partition(10)._2.partitions(3));
		println("----------------------------------");
		println("nd.split(10)._1.splits(3, 5)", nd.partition(10)._1.partitions(3, 5));
		println("nd.split(10)._2.splits(3, 5)", nd.partition(10)._2.partitions(3, 5));
		println("----------------------------------");
		println("nd.split(0)", nd.partitions(0));
		println("nd.split(nd.length())", nd.partitions(nd.length()));
		println("----------------------------------");
		try { // 抛出异常
			println("nd.split(0)", nd.partition(0)._1.get(0));
		} catch (Exception e) {
			e.printStackTrace();
		}
		println("----------------------------------");
		println("nd.split(10)", nd.partition(10)._2.get(9));
		try { // 抛出异常
			println("nd.split(10)", nd.partition(10)._2.get(10));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void quz() {
		final var p = P(2, 2, 2);
		p.strides(true);
		println(p.strides());
		println("-------------------");
		final var p2 = P2("A", 2, "B", 2, "C", 2, "D", P(1, 2));
		println("\"A\", 2, \"B\", 2, \"C\", 2,\"D\",P(1,2)");
		p2.strides(true);
		println(p2.strides());
	}

	/**
	 * trieTree 多维键名单值列表:<br>
	 * -|- {flag=true, value=[0,1,2,3,4,5,6,7,8], index=[], level=0, isleaf=false}
	 * [0,1,2,3,4,5,6,7,8] [0,1,2,3,4,5,6,7,8] <br>
	 * -|--|- {flag=true, value=[0,1,2,3], index=[0], level=1, isleaf=false}
	 * [0,1,2,3] [0,1,2,3] <br>
	 * -|--|--|- {flag=true, value=[0,1], index=[0, 0], level=2, isleaf=true} [0,1]
	 * [0,1] <br>
	 * -|--|--|- {flag=true, value=[2,3], index=[0, 1], level=2, isleaf=true} [2,3]
	 * [2,3] <br>
	 * -|--|- {flag=true, value=[4,5,6,7,8], index=[1], level=1, isleaf=false}
	 * [4,5,6,7,8] [4,5,6,7,8] <br>
	 * -|--|--|- {flag=true, value=[4,5,6], index=[1, 0], level=2, isleaf=true}
	 * [4,5,6] [4,5,6] <br>
	 * -|--|--|- {flag=true, value=[7,8], index=[1, 1], level=2, isleaf=true} [7,8]
	 * [7,8] <br>
	 */
	@Test
	public void qux() {
		final var p = P(P(2, 2), P(3, 2));
		final var np = Tensor.nd(i -> i, p);
		println(np);
		println(np.strides());
		println("---------------------------------------------------------");
		np.trieTree(p)
				// .filter(e -> e.attrval("isleaf", bool)) //
				.forEach(e -> {
					println(String.format("%s %s %s %s", //
							"-|-".repeat(e.getLevel()), e.attrs(), e.attrval(ndint), //
							e.attrval("index", np::getndI)));
				});
		println("---------------------------------------------------------");
		println("p.getnd(0)", np.getnd(0));
		println(np.get(0, 1));
		final var t = np.arrayOf(Tensor::of).reshape(10);
		println("tensor", t);
		np.strides().forEach((k, v) -> {
			println(k, v);
		});
	}

	@Test
	public void qua() {
		final var nd = Stream.iterate(0, i -> i < 9, i -> i + 1).collect(INdarray.ndclc());
		println(nd.reverse());
		println(nd);
		println(INdarray.from(A(1, 2, 3, 4)).assign(asList(3, 4)));

		println(INdarray.from(1, 2, 3, 4, 5, 6).collect(Collectors.partitioningBy(e -> e > 3, INdarray.ndclc())));
		final var nd1 = INdarray.from(0, 1, 2, 3, 4, 5, 6);
		println(nd1.sorted((a, b) -> b - a).sorted().subarray(1, 4).assign(asList(8, 9, 10)));
		println(nd1);
		println(INdarray.from(0, 1, 2, 3, 4, 5, 6).subarray(6));
	}

	/**
	 * 拷贝克隆
	 */
	@Test
	public void dup() {
		final var nd = nats(10);
		final var p = nd.shift(3);
		final var p1 = p.dupdata();
		final var p2 = p.dupdata2();

		println(p.set(23));
		println("p1", p1, p1.datalen());
		println("p2", p2, p2.datalen());
		println("p2.create(0)", p2.create(0), p2.datalen());
		println(nd);
	}

	@Test
	public void dupq() {
		println(nats(100).entries());
		println(nats(10).heads());
		println(nats(10).tails());
	}

	@Test
	public void dup1() {
		final var nd1 = nats(100);
		final var nd2 = INdarray.nd(nd1);
		nd2.set(100);
		println(nd1);
		println("----------------------------");
		final var nums = new Integer[] { 1, 2, 3, 4, 5 };
		final var nd3 = INdarray.nd(Arrays.asList(nums));
		nums[0] = 100;
		println(nd3);
	}

	@Test
	public void cuts() {
		final var nd = INdarray.nd(1, 2, 3, 4, 5);
		println("nd.cuts(2)", nd.cuts(2));
		println("nd.cuts(2,true)", nd.cuts(2, true));
	}

}
