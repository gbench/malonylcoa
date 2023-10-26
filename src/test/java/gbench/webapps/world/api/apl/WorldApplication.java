package gbench.webapps.world.api.apl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 
 * @author xuqinghua
 * 
 * 启动参数:
 * --server.port=8010 --server.servlet.context-path=/world
 */
@ComponentScan(basePackages = { "gbench.webapps.world.api" })
@SpringBootApplication
public class WorldApplication {

	public static void main(final String args[]) {
		SpringApplication.run(WorldApplication.class, args);
	}

}