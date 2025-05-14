package gbench.webapps.myfuture.gateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * WorldApiRoute
 */
@Component
public class MyFutureApiRoute {

	/**
	 * apiRouteLocator
	 * 
	 * @param builder 路由定位器构建器
	 * @return RouteLocator
	 */
	@Bean
	public RouteLocator apiRouteLocator(final RouteLocatorBuilder builder) {
		return builder.routes() //
				.route(r -> r.path("/myfuture/**").filters(f -> f.stripPrefix(1)).uri(myfuture_api)) //
				.build();
	}

	private String myfuture_api = "lb://myfuture-api"; // 接口API,使用lb表示利用服务名进行负载均衡

}
