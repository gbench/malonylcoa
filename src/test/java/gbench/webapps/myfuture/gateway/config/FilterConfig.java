package gbench.webapps.myfuture.gateway.config;

import static gbench.util.io.Output.println;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

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

	/**
	 * 全局过滤器
	 * 
	 * @return GlobalFilter
	 */
	@Order(0)
	@Bean
	public GlobalFilter globalFilter() {

		return (exchange, chain) -> {
			final var req = exchange.getRequest();
			final var method = req.getMethod();
			final var contentType = req.getHeaders().getContentType();
			println(contentType, LocalDateTime.now());

			if (HttpMethod.POST.compareTo(method) == 0 && //
					Objects.equals((Object) "application/x-www-form-urlencoded", contentType)) { // POST
				println("GATEWAY", HttpMethod.POST, LocalDateTime.now());

				return DataBufferUtils.join(exchange.getRequest().getBody()) //
						.flatMap(dataBuffer -> {
							final var bytes = new byte[dataBuffer.readableByteCount()];
							dataBuffer.read(bytes);
							final var body = new String(bytes, StandardCharsets.UTF_8);
							System.err.println("GATEWAY:" + body);
							exchange.getAttributes().put("POST_BODY", body);
							DataBufferUtils.release(dataBuffer);

							return chain.filter(exchange.mutate() //
									.request(new ServerHttpRequestDecorator(exchange.getRequest()) {
										@Override
										public Flux<DataBuffer> getBody() {
											return Flux.defer(() -> Mono
													.just(exchange.getResponse().bufferFactory().wrap(bytes)));
										} // getBody
									}).build());
						}); // flatMap
			} else { // 其他方法
				return chain.filter(exchange).then(Mono.fromRunnable(() -> {
					println("GATEWAY", method, LocalDateTime.now());
				}));
			} // if
		};
	}

	/**
	 * CorsWebFilter
	 * 
	 * @return CorsWebFilter
	 */
	@Bean
	public CorsWebFilter corsWebFilter() {
		System.out.println("corsWebFilter");
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
