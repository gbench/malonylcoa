package gbench.util.function;

/**
 * TriConsumer 三元消费函数
 *
 * @param <X> 第一参数类型
 * @param <Y> 第二参数类型
 * @param <Z> 第三参数类型
 */
@FunctionalInterface
public interface TriConsumer<X, Y, Z> {
	void accept(final X x, final Y y, final Z z);
}