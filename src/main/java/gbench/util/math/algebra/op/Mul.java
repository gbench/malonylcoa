package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Mul<T, U> extends BinaryOp<T, U> {

    public Mul(T t, U u) {
        super("*", P(t, u));
    }

}