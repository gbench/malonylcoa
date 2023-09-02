package gbench.util.math.algebra.op;

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
public class Identity<T> extends UnaryOp<T> {

    public Identity(T t) {
        super("identity", t);
    }

}