package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Minus<T, U> extends BinaryOp<T, U> {

    public Minus(T t, U u) {
        super("-", P(t, u));
    }

}