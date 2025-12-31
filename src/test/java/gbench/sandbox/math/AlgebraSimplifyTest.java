package gbench.sandbox.math;

import org.junit.jupiter.api.Test;

import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.op.ConstantOp;
import gbench.util.math.algebra.op.UnaryOp;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.math.algebra.Algebras.analyze;
import static gbench.util.math.algebra.Algebras.evaluate;
import static gbench.util.math.algebra.op.Ops.*;
import static java.lang.Math.PI;

import java.util.Arrays;
import java.util.Objects;

/**
 * 化简
 * 
 * @author gbench
 *
 */
public class AlgebraSimplifyTest {

	/**
	 * 因子式化简
	 */
	@Test
	public void quc() {
		println(analyze("(sin x)^2 + (cos x)^2").dx().simplify());
		println(analyze("cos x").dx().simplify());
		println(analyze("sin x * cos x + cos x * sin x").simplify());
		println(analyze("x+x+x+x+x").dx().simplify());
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
		println(analyze("cos x ^ 2").dx().simplify());
		println(analyze("(5*sin x) * (5*sin x)").simplify());
	}

	@Test
	public void qux() {
		nats(8).fmap(n -> evaluate("sin x", "x", PI * 2 / 8 * n));
		nats(8).fmap(n -> evaluate("sin x ^ 2 + cos x ^ 2", "x", PI * 2 / 8 * n));
		analyze("x+(x+2*x+x)+x*sin(x+(x+2*x+x))").simplify(); // 符号计算化简
		println(analyze("sin x ^ 2 + cos x ^ 2").dumpAST()); // 解析成语法树
		analyze("sin x ^ 2 + cos x ^ 2").dx(); // 微分结构
	}

	@Test
	public void quy() {
		println(analyze("x*x*x").simplify());
		println(analyze("sin x * sin x * sin x").simplify());
		println(analyze("x*5*x*6*x*x").simplify());
		println(analyze("(* (* 30.0 (pow x 2)) x)").simplify());
	}

	@Test
	public void qua() {
		println(analyze("2*x*3*x").simplify());
		println(analyze("2*x*x*3").simplify());
		println(analyze("x*2*x*3").simplify());
		println(analyze("x*2*3*x").simplify());
		println(analyze("x*2*neg x").simplify());
	}

	@Test
	public void qub() {
		// println(analyze("cos x ^ 2").dx().simplify());
		println(analyze("1*sin x * neg 1 * cos x").simplify());
		// println(analyze("1 * 1").simplify());
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
		} else if (bop instanceof UnaryOp<?> uop) {
			return uop.duplicate();
		} else {
			if (bop.namEq("*") || bop.namEq("+")) {
				final BinaryOp<?, ?>[] bb = bop.getArgS() //
						.map(e -> (BinaryOp<Object, Object>) adjust(BinaryOp.wrap(e))).toArray(BinaryOp[]::new);
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