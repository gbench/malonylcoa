package gbench.util.jdbc;

import java.lang.reflect.Method;

import gbench.util.jdbc.kvp.IRecord;

/**
 * SQL 的拦截器 函数接口 String handle(Method method,IRecord params,String
 * sqlpattern,Jdbc jdbc) 对 请求的 sqlpattern 进行预处理。比如读写分离，分库分表的处理等。 -- method 方法对象,
 * -- params 参数列表:name->value, -- sqlpattern sql模板 ，sql
 * 模板通称会携带模板参数，以完成分数据库，分表的能力, -- jdbc 当前连接的数据库对象．
 * 
 * @author xuqinghua
 *
 * @param <T> 返回结果的类型
 */
@FunctionalInterface
public interface ISqlInterceptor<T> {
	/**
	 * 拦截器：如果结果返回null则给予放行,否则方法给予放行
	 * 
	 * @param method     方法对象
	 * @param params     参数列表:name->value
	 * @param sqlpattern sql模板 ，sql 模板通称会携带模板参数，以完成分数据库，分表的能力．
	 * @param jdbc       当前连接的数据库对象．
	 * @return 处理后的sql模板
	 */
	T intercept(final Method method, final IRecord params, final String sqlpattern, final Jdbc jdbc);
}