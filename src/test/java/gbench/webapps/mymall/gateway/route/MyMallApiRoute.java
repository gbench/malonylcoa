package gbench.webapps.mymall.gateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * MyMallApiRoute
 */
@Component
public class MyMallApiRoute {

	/**
	 * apiRouteLocator
	 * 
	 * @param builder 路由定位器构建器
	 * @return RouteLocator
	 */
	@Bean
	public RouteLocator apiRouteLocator(final RouteLocatorBuilder builder) {
		return builder.routes() //
				.route(r -> r.path("/mymall/**").filters(f -> f.stripPrefix(1)).uri(mymall_api)) //
				.build();
	}

	private String mymall_api = "lb://mymall-api"; // 接口API,使用lb表示利用服务名进行负载均衡

}
