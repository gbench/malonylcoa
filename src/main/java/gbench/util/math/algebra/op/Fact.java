package gbench.util.math.algebra.op;

/**
 * Factorial 阶乘函数
 * 
 * @author gbench
 *
 * @param <T>
 */
public class Fact<T> extends UnaryOp<T> {

    public Fact(T t) {
        super("fact", t);
    }

}