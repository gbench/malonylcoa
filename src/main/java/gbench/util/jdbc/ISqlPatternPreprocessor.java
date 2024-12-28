package gbench.util.jdbc;

import java.lang.reflect.Method;

import gbench.util.jdbc.kvp.IRecord;

/**
 * SQL 语句模板的预处理器 <br>
 * 函数接口 String handle(Method method,IRecord params,String sqlpattern,Jdbc jdbc)
 * 对 <br>
 * 请求的 sqlpattern 进行预处理。比如读写分离，分库分表的处理等。 <br>
 * -- method 方法对象, <br>
 * -- params 参数列表:name->value, <br>
 * -- sqlpattern sql模板 ，sql 模板通称会携带模板参数，以完成分数据库，分表的能力, <br>
 * -- jdbc 当前连接的数据库对象． <br>
 * 
 * @author xuqinghua
 *
 */
@FunctionalInterface
public interface ISqlPatternPreprocessor {
	/**
	 * sql 模板的参数的预处理函数。 <br>
	 * 比如可以有这样一种预处理的实现SQL变换的预处理实现，当然也可以采用 另外的自定义实现 等。 <br>
	 * 可以有某个SqlPatternPreprocessor 的实现自动对读sqlpattern中的命名参数进行替换： <br>
	 * method:public User getUserByName(String name); <br>
	 * sqlpattern: select * from user where name=#name <br>
	 * params:REC("name","张三") <br>
	 * 返回值:select * from user where name="张三" <br>
	 * 
	 * 需要注意对于sql语句模板不能以#开头。
	 * 
	 * @param method     方法对象
	 * @param params     形参与实参之间的对应关系列表:{name/形式参数的名称->value/实际参数的数值}
	 * @param sqlpattern sql模板 ，sql 模板通称会携带模板参数，以完成分数据库，分表的能力．
	 * @param jdbc       当前连接的数据库对象．
	 * @return 处理后的sql模板
	 */
	String handle(final Method method, final IRecord params, final String sqlpattern, final Jdbc jdbc);

	/**
	 * SqlPatternPreprocessor 的名称
	 * 
	 * @return SqlPatternPreprocessor 的名称,默认名称为 none,表示没有名称
	 */
	default String name() {
		return "none";
	}

	/**
	 * SqlPatternPreprocessor 的执行状态数据
	 * 
	 * @return SqlPatternPreprocessor 的执行状态数据
	 */
	default IRecord status() {
		return null;
	}
}