package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Add<T, U> extends BinaryOp<T, U> {

    public Add(T t, U u) {
        super("+", P(t, u));
    }

}