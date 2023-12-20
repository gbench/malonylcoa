package gbench.util.jdbc.function;

/**
 * 带有抛出异常的函数
 *
 * @param <T> 参数类型
 * @param <U> 返回类型
 * @author xuqinghua
 */
public interface ThrowableFunction<T, U> {
	U apply(final T t) throws Throwable;
}