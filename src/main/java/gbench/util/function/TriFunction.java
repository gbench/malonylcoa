package gbench.util.function;

/**
 * TriConsumer 三元函数
 *
 * @param <X> 第一参数类型
 * @param <Y> 第二参数类型
 * @param <Z> 第三叔类型
 * @param <T> 结果类型
 */
@FunctionalInterface
public interface TriFunction<X, Y, Z, T> {
    T apply(final X x, final Y y, final Z z);
}