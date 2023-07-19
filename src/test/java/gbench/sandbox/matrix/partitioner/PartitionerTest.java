package gbench.sandbox.matrix.partitioner;

import static gbench.util.array.INdarray.ndint;
import static gbench.util.array.Partitioner.P;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.tree.TrieNode.attr_of;

import org.junit.jupiter.api.Test;

import gbench.util.array.INdarray;
import gbench.util.matrix.Matrices;

/**
 * 
 * @author gbench
 *
 */
public class PartitionerTest {
	@Test
	public void bar() {
		// 划分会把多余的数据给过滤掉
		println("划分会把多余的数据给过滤掉,12个就够了", INdarray.of(i -> i, 100).trieTree(5, P(3, 4)).mutate(root -> {
			root.forEach(e -> {
				println(e.path(), e.attrval());
			});
			root.stream(e -> e.partS().skip(1).toArray(Integer[]::new)).forEach(path -> {
				println(INdarray.of(path), root.get(attr_of(ndint), path));
			});
			return root.get(attr_of(ndint), 2, 1);
		}));
	}

	@Test
	public void foo() {
		final var x = Matrices.of(A(1, 2, 3, 4)).toMatrix().nd();
		x.scanlS(e -> e).forEach(e -> {
			println(e, e.sum());
		});
		println("-----------------------------");
		x.scanrS(e -> e).forEach(e -> {
			println(e, e.sum(), e.product());
		});
		println();
		println(x.floats().sum());
		println(INdarray.of(i -> i, 10));
	}
}
