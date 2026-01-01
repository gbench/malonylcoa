package gbench.webapps.myfuture.broker.config;

import java.util.ArrayList;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;

import gbench.util.json.MyJson;
import gbench.webapps.myfuture.broker.config.param.ParamResolver;

/**
 * 系统配置信息
 */
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
	 * 带有IRecord类型解析的 ObjectMapper
	 * 
	 * @return ObjectMapper
	 */
	@Bean
	@Order(0)
	@Primary
	public ObjectMapper objectMapper() {
		final ObjectMapper objM = MyJson.recM();
		return objM;
	}

	/**
	 * webflux 由于不能block所以不能用,写在这里作为示例 <br>
	 * 增加 objectMapper IRecord的解析能力,示例 <br>
	 * restTemplate.getForObject("http://world-api/api/srch/config", IRecord.class);
	 * 
	 * @return restTemplate
	 */
	@Bean
	@Scope("prototype")
	@LoadBalanced
	public RestTemplate restTemplate(final ObjectMapper objM) {
		final var builder = new RestTemplateBuilder();
		final var converter = new MappingJackson2HttpMessageConverter(objM);
		final var rt = builder.additionalMessageConverters(converter).build();
		return rt;
	}

	/**
	 * webflux 专用款式
	 * 
	 * @return WebClient
	 */
	@Bean
	@Scope("prototype")
	@LoadBalanced
	public WebClient.Builder webClientBuilder(final ObjectMapper objM) {
		return WebClient.builder().codecs(configurer -> {
			final var codecs = configurer.defaultCodecs();
			codecs.jackson2JsonDecoder(new Jackson2JsonDecoder(objM));
			codecs.jackson2JsonEncoder(new Jackson2JsonEncoder(objM));
		});
	}

}
