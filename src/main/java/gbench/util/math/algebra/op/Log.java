package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Log<T, U> extends BinaryOp<T, U> {

	public Log(T t, U u) {
		super("log", P(t, u));
	}

}