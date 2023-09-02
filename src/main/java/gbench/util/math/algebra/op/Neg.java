package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Neg<T> extends UnaryOp<T> {

    public Neg(T t) {
        super("neg", t);
    }

}