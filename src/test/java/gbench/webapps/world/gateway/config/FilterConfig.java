package gbench.webapps.world.gateway.config;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class FilterConfig {

	@Order(1)
	@Bean
	public GlobalFilter globalFilter() {
		return (exchange, chain) -> {
			final var req = exchange.getRequest();
			if (req.getMethod().compareTo(HttpMethod.POST) == 0) {
				System.out.println("GATEWAY: POST METHOD");
				return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
					final var bytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(bytes);
					final var body = new String(bytes, StandardCharsets.UTF_8);
					System.err.println("GATEWAY:" + body);
					exchange.getAttributes().put("POST_BODY", body);
					DataBufferUtils.release(dataBuffer);
					final var cachedFlux = Flux.defer(() -> {
						final var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
						return Mono.just(buffer);
					});
					final var mutateRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
						@Override
						public Flux<DataBuffer> getBody() {
							return cachedFlux;
						}
					};
					return chain.filter(exchange.mutate().request(mutateRequest).build());
				});
			} else {
				return chain.filter(exchange).then(Mono.fromRunnable(() -> {
					// System.out.println("Global Filter executed");
				}));
			}
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
