package gbench.util.jdbc;

import java.sql.SQLException;

/**
 * 一个DataManipulation代表一个数据操作计划：需要有媒介载体：即Session来给予表达，即DataManipulation <br>
 * 中的符号概念，具体对应在什么物理物理位置．这些信息通常就是一个数据库连接来给予间接表达．<br>
 * <p>
 * DataManipulation 是专门为了数据库的事务性操作来而创建的接口。<br>
 * 可以发出执行异常的 SQL语句,这是专门为了实现SQL事务而构造的结构。<br>
 * DataManipulation 其实就是对Consumer函数的一个模拟，只是由于Consumer的accept不能够抛出异常 <br>
 * DataManipulation 加入了一个可以抛出异常的invoke <br>
 *
 * @param <T> 会话对象 每个数据操作都会存在一个特有的会话对象，以保留数据操作的上下文信息。<br>
 *            则个会话对象需要由事务的创建者来给予提供。
 * @author gbench
 */
public interface IDataManipulation<T> {
	/**
	 * 这个函数主要是通过异常 throw Exception 来表示执行失败
	 *
	 * @param session 数据操作的媒介载体：即交易会话
	 * @throws SQLException
	 * @throws IndexOutOfBoundsException
	 * @throws Exception
	 * @throws Throwable
	 */
	void invoke(final T session) throws SQLException, IndexOutOfBoundsException, Exception, Throwable;
}