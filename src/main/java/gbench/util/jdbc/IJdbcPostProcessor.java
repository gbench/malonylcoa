package gbench.util.jdbc;

import java.lang.reflect.Method;

import gbench.util.jdbc.kvp.IRecord;

/**
 * jdbc 的事后处理。一般做一些收尾的拦截与
 *
 * @param <T> 返回结果的类型
 * @author xuqinghua
 */
@FunctionalInterface
public interface IJdbcPostProcessor<T> {
	/**
	 * 后续处理的方法
	 *
	 * @param result jdbc 的处理结果
	 * @return 先前的Jdbc的处理结果。这个类型是根据 接口返回类型而侦测出来的，一般不需要给予更改。
	 */
	Object process(final Method method, final IRecord params, final Jdbc jdbc, final T result);
}