package gbench.webapps.crawler.gateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * WorldApiRoute
 */
@Component
public class CrawlerApiRoute {

	/**
	 * apiRouteLocator
	 * 
	 * @param builder 路由定位器构建器
	 * @return RouteLocator
	 */
	@Bean
	public RouteLocator apiRouteLocator(final RouteLocatorBuilder builder) {
		return builder.routes() //
				.route(r -> r.path("/world/**").filters(f -> f.stripPrefix(1))
						.uri(world_api)) //
				.build();
	}

	private String world_api = "http://localhost:6010"; // 接口API

}
