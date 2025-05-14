package gbench.webapps.myfuture.gateway.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * WorldGatewayApplication <br>
 * 
 * 启动参数: <br>
 * --server.port=9010 或 <br>
 * VM argument: <br>
 * -Dspring.config.location=classpath:/config/webapps/myfuture/gateway/application.yml
 * <br>
 * 启动nacos脚本(windows) <br>
 * set nacos_home=D:\sliced\develop\nacos\nacos-server-2.3.0 <br>
 * %nacos_home%\bin\startup.cmd -m standalone <br>
 * <br>
 * 控制台 Console: http://127.0.0.1:8848/nacos/index.html
 * 
 * @author xuqinghua
 */
@ComponentScan(basePackages = { "gbench.webapps.myfuture.gateway" })
@SpringBootConfiguration
@EnableAutoConfiguration
public class MyFutureGatewayApplication {

	/**
	 * Gateway 启动入口
	 * 
	 * @param args 入口参数
	 */
	public static void main(final String[] args) {
		SpringApplication.run(MyFutureGatewayApplication.class, args);
	}
}
