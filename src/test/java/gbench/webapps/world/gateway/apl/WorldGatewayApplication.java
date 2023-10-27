package gbench.webapps.world.gateway.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author xuqinghua
 * 
 *         启动参数: --server.port=8010
 */
@ComponentScan(basePackages = { "gbench.webapps.world.gateway" })
@SpringBootConfiguration
@EnableAutoConfiguration
public class WorldGatewayApplication {

	/**
	 * Gateway 启动入口
	 * 
	 * @param args 入口参数
	 */
	public static void main(final String[] args) {
		SpringApplication.run(WorldGatewayApplication.class, args);
	}
}
