package gbench.webapps.world.gateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

@Configuration
public class FilterConfig {

	@Bean
	public GlobalFilter postGlobalFilter() {
		return (exchange, chain) -> {
			final var req = exchange.getRequest();
			if (req.getMethod().compareTo(HttpMethod.POST) == 0) {
//				req.getBody().subscribe(buffer->{
//					final var bb = new byte[buffer.readableByteCount()];
//					buffer.read(bb);
//					DataBufferUtils.release(buffer);
//					try {
//						System.err.println(new String(bb,"utf8"));
//					}catch(Exception e) {
//						
//					}
//				});
				System.out.println("POST METHOD");
			}
			return chain.filter(exchange).then(Mono.fromRunnable(() -> {
				// System.out.println("Global Post Filter executed");
			}));
		};
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		System.out.println("CORS限制打开");
		final var config = new CorsConfiguration();
		// 仅在开发环境设置为*
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.setAllowCredentials(false);
		final var configSource = new UrlBasedCorsConfigurationSource();
		configSource.registerCorsConfiguration("/**", config);
		return new CorsWebFilter(configSource);
	}

}
