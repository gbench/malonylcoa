package gbench.util.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这是一个事务级别的SQL语句执行级别． 执行一个jdbc语句，value 中提供的是一个 sql 语句模板：比如 ： "update t_user set
 * name=''{1}'' where id={0}" 语句模板采用 MessageFormat 给予格式化处理，占位符用
 * {index},形式给予提供，index从0开始，<br>
 * 代表装饰函数的第一个参数，第二个参数，以此类推
 * 
 * @author gbench
 *
 */
@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface JdbcPreparedExecute {
	String[] value() default {};// sql语句集合．
}