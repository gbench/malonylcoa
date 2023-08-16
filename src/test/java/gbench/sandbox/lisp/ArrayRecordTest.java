package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import gbench.util.lisp.ArrayRecord;

import static gbench.util.io.Output.println;

public class ArrayRecordTest {
	
	@Test
	final void foo() {
		println(ArrayRecord.REC("name","zhangsan","height",175));
	}

}
