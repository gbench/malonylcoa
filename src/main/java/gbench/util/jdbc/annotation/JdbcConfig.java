package gbench.util.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jdbc上下文:提供一个JDBC的基本操作参数 <br>
 * 注意serverTimezone一定需要加入：否则结果返回的时间会有问题：MYSQL采用格林乔治时间．<br>
 * driver 驱动程序：默认 "com.mysql.cj.jdbc.Driver"; <br>
 * url:数据据连接字符串，默认 "jdbc:mysql://localhost:3306/hello?serverTimezone=GMT%2B8";
 * <br>
 * user：用户名，默认 "root" <br>
 * password：密码，默认 "123456" <br>
 * 
 * @author gbench
 *
 */
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface JdbcConfig {
	String driver() default "com.mysql.cj.jdbc.Driver";

	String url() default "jdbc:mysql://localhost:3306/hello?serverTimezone=GMT%2B8";

	String user() default "root";

	String password() default "123456";
}