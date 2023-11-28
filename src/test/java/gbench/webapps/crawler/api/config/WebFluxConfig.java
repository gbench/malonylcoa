package gbench.webapps.crawler.api.config;

import java.util.ArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import gbench.util.json.MyJson;
import gbench.webapps.crawler.api.config.param.ParamResolver;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

	/**
	 * 加入自定义方法参数解析器
	 * 
	 * @param configurer
	 */
	@Override
	public void configureArgumentResolvers(final ArgumentResolverConfigurer configurer) {
		final var readers = new ArrayList<HttpMessageReader<?>>();
		// 添加Http消息编解码器
		readers.add(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
		// 消息编解码器与Resolver绑定
		configurer.addCustomResolver(new ParamResolver(readers));
	}

	/**
	 * 
	 * @return
	 */
	@Bean
	@Order(0)
	@Primary
	public ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = MyJson.recM();
		return objectMapper;
	}
}
