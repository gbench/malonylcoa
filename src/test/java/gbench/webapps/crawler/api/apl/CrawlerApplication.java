package gbench.webapps.crawler.api.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 
 * @author xuqinghua
 * 
 *         启动参数: <br>
 *         --server.port=6010 或 <br>
 *         VM argument: <br>
 *         -Dspring.config.location=classpath:/config/webapps/crawler/api/application.yml
 */
@ComponentScan(basePackages = { "gbench.webapps.crawler.api" })
@EnableDiscoveryClient
@SpringBootApplication
public class CrawlerApplication {

	/**
	 * API启动入口
	 * 
	 * @param args 启动参数
	 */
	public static void main(final String args[]) {
		SpringApplication.run(CrawlerApplication.class, args);
	}

}