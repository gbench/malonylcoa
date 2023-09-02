package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Div<T, U> extends BinaryOp<T, U> {

    public Div(T t, U u) {
        super("/", P(t, u));
    }

}