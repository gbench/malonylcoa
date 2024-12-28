package gbench.util.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 执行一个jdbc语句<br>
 * value 中提供的是一个 sql 语句模板：比如 ："select * from t_user where id={0}" <br>
 * 语句模板采用 MessageFormat 给予格式化处理，占位符用 {index},形式给予提供， <br>
 * index从0开始，代表装饰函数的第一个参数，第二个参数，以此类推 <br>
 * 修饰的模仿默认返回未 List&gt;IRecord&gt; <br>
 * 也可是一个用户自定义的 POJO 此时会采用 IRecord.OBJINIT 来吧IRecord转换成 POJO. <br>
 * 对于是接口中的default 方法，如果该方法的最后一个参数为空数值，则会把SQL语句的返回数值传入到该该参数中，并调用该默认方法． <br>
 * 
 * @author gbench
 *
 */
@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface JdbcQuery {
	String value() default "";
}