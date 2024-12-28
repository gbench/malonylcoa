package gbench.sandbox.lisp;

import static gbench.util.io.Output.println;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import gbench.util.lisp.IRecord;

public class LispRecordTest {

	/**
	 * 别名测试
	 */
	@Test
	public void alias() {
		final var rec = IRecord.rb("a,b,c,d").get(1, 2, 3, 4);
		println("子集别名1 alias", rec.alias("a,A,c,C"), "{A=1, C=3}");
		println("子集别名2 alias", rec.alias("a,A,c,C,e,E"), "{A=1, C=3}");
		println("全集别名-kvp键名顺序1 alias2", rec.alias2("a,A,c,C"), "{A=1, C=3, b=2, d=4}");
		println("全集别名-kvp键名顺序2 alias2", rec.alias3("a,A,c,C,e,E"), "{A=1, C=3, b=2, d=4}");
		println("全集别名-源键名顺序1 alias3", rec.alias3("a,A,c,C"), "{A=1, b=2, C=3, d=4}");
		println("全集别名-源键名顺序2 alias3", rec.alias3("a,A,c,C,e,E"), "{A=1, b=2, C=3, d=4}");
	}

	/**
	 * 代数运算
	 */
	@Test
	public void op() {
		final var rb = IRecord.rb("a,b,c");
		final var a = rb.get(1, 2, 3);
		final var b = rb.get(5, 9, 4);
		final var c = rb.get(8, 0, 7);

		println("a", a);
		println("a+1", a.plus(1));
		println("a-1", a.subtract(1));
		println("a*2*-1", a.multiply(2, -1));
		println("a/2/-1", a.divide(2));
		println("a+b", a.plus(b));
		println("a+b+c", a.plus(b, c));
		println("a-b", a.subtract(b));
		println("a-b-c", a.subtract(b, c));
		println("a*b", a.multiply(b));
		println("a*b*c", a.multiply(b, c));
		println("a/b", a.divide(b));
		println("a/b/c", a.divide(b, c));
		println("a.max(c)", a.max(Integer.class::cast, c));
		println("a.max(2)", a.max(Integer.class::cast, 2));
		println("a.max(2)", a.max(2));
		println("a.min(c)", a.min(Integer.class::cast, c));
		println("a.min(2)", a.min(Integer.class::cast, 2));
		println("a.min(2)", a.min(2));
		println(a.reduce(null, b, c));
		println(a.reduce(IRecord.combine("%s/%s"::formatted), b, c));
		println(a.reduce(IRecord.combine(Arrays::asList), b, c));
	}

}
