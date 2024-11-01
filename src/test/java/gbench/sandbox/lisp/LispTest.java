package gbench.sandbox.lisp;

import org.junit.jupiter.api.Test;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.A;
import static gbench.util.lisp.Lisp.CONS;
import static java.util.Arrays.asList;

public class LispTest {

	@Test
	public void cons() {
		println("CONS(1,null)", asList(CONS(1, null)));
		println("CONS(null, null)", asList(CONS(null, null)));
		println("CONS(1, A(2, 3))", asList(CONS(1, A(2, 3))));
		println("CONS(null, A(2, 3))", asList(CONS(null, A(2, 3))));
	}

}
