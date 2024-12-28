package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Expa<T, U> extends BinaryOp<T, U> {

    public Expa(T t, U u) {
        super("expa", P(t, u));
    }

}