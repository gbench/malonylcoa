package gbench.webapps.crawler.api.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 
 * @author xuqinghua
 * 
 *         启动参数: --server.port=6010
 */
@ComponentScan(basePackages = { "gbench.webapps.crawler.api" })
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