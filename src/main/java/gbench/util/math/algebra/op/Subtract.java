package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Subtract<T, U> extends BinaryOp<T, U> {

    public Subtract(T t, U u) {
        super("-", P(t, u));
    }

}