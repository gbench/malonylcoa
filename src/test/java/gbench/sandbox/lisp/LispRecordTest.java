package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.util.io.Output;
import gbench.util.lisp.IRecord;

public class LispRecordTest {

	/**
	 * 别名测试
	 */
	@Test
	public void alias() {
		final var rec = IRecord.rb("a,b,c,d").get(1, 2, 3, 4);
		Output.println("子集别名 alias", rec.alias("a,A,c,C"), "{A=1, C=3}");
		Output.println("全集别名-kvp键名顺序 alias2", rec.alias2("a,A,c,C"), "{A=1, C=3, b=2, d=4}");
		Output.println("全集别名-源键名顺序 alias3", rec.alias3("a,A,c,C"), "{A=1, b=2, C=3, d=4}");
	}

}
