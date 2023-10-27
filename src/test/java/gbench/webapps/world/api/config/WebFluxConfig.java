package gbench.webapps.world.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.databind.ObjectMapper;

import gbench.util.json.MyJson;

@Configuration
public class WebFluxConfig {
	
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
