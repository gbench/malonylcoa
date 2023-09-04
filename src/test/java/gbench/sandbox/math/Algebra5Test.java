package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.op.ConstantOp;
import gbench.util.math.algebra.op.UnaryOp;

import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.Algebras.analyze;
import static gbench.util.math.algebra.op.Ops.*;

import java.util.Arrays;
import java.util.Objects;

/**
 * 
 * @author gbench
 *
 */
public class Algebra5Test {

	/**
	 * 因子式化简
	 */
	@Test
	public void quc() {
		println(analyze("(sin x)^2 + (cos x)^2").dx().simplify());
		println(analyze("cos x").dx().simplify());
		println(analyze("sin x * cos x + cos x * sin x").simplify());
	}

	/**
	 * 
	 */
	@Test
	public void foo() {
		final var a = MUL("x", MUL("y", MUL("z", 2)));
		println(a.simplify());
		println(a.flatArgs2());
		a.flatS().forEach(e -> {
			println(e.isConstant(), e, e.dbl());
		});
	}

	@Test
	public void bar() {
		println(adjust(MUL("x", MUL("x", 2))));
		println(adjust(analyze("cos x ^ 2").dx().getOp().simplify()));
		println(analyze("sin x ^ 2 + cos x ^ 2").dx().simplify());
	}

	/**
	 * 节点调整：将节点系数向前调整
	 * 
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @param bop 符号类型
	 * @return 调整节点
	 */
	public static <T, U> BinaryOp<?, ?> adjust(final BinaryOp<T, U> bop) {
		if (bop instanceof ConstantOp cop) {
			return cop.duplicate();
		} else if (bop instanceof UnaryOp uop) {
			return uop.duplicate();
		} else {
			if (bop.namEq("*") || bop.namEq("+")) {
				final BinaryOp<?, ?>[] bb = bop.getArgS() //
						.map(e -> adjust(BinaryOp.bop(e))).toArray(BinaryOp[]::new);
				if (Arrays.stream(bb).allMatch(e -> e.isConstant()) // 简单节点
						&& bb[1].dbl() != null && bb[0].dbl() == null) {
					return bop.compose(bb[1], bb[0]);
				} else { // 复合节点
					final BinaryOp<?, ?>[] bb1 = Arrays.stream(bb).map(e -> {
						return e.isConstant() ? e : e.flatArgS2().findFirst().orElse(null);
					}).filter(Objects::nonNull).toArray(BinaryOp[]::new);
					if (bb1[1].dbl() != null && bb1[0].dbl() == null) {
						return bop.compose(bb[1], bb[0]);
					} else {// if
						return bop.compose(bb[0], bb[1]);
					}
				} // if
			} // if
		}

		return bop.duplicate();
	}

}