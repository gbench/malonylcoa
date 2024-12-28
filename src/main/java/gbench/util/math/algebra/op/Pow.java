package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Pow<T, U> extends BinaryOp<T, U> {

    public Pow(T t, U u) {
        super("pow", P(t, u));
    }

}