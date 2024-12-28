package gbench.webapps.crawler.gateway.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * CrawlerGatewayApplication <br>
 * 
 * 启动参数: <br>
 * --server.port=8010 或 <br>
 * VM argument: <br>
 * -Dspring.config.location=classpath:/config/webapps/crawler/gateway/application.yml
 * <br>
 * 启动nacos脚本(windows) <br>
 * set nacos_home=D:\sliced\develop\nacos\nacos-server-2.3.0
 * %nacos_home%\bin\startup.cmd -m standalone <br>
 * <br>
 * 控制台 Console: http://127.0.0.1:8848/nacos/index.html
 * 
 * @author xuqinghua
 */
@ComponentScan(basePackages = { "gbench.webapps.crawler.gateway" })
@SpringBootConfiguration
@EnableAutoConfiguration
public class CrawlerGatewayApplication {

	/**
	 * Gateway 启动入口
	 * 
	 * @param args 入口参数
	 */
	public static void main(final String[] args) {
		SpringApplication.run(CrawlerGatewayApplication.class, args);
	}
}
