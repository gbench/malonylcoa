package gbench.webapps.crawler.gateway.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author xuqinghua
 * 
 *         启动参数: --server.port=8010
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
