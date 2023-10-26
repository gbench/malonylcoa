package gbench.webapps.world.api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import gbench.util.json.MyJson;

/**
 * Jackson 配置，增加 DFrame,IRecord,DataMatrix 系列的数据处理
 *
 * @author xuqinghua
 * @EnableWebMvc 用于表示对 SpringMvc 进行设置
 */
@Configuration
@EnableWebMvc
public class SpringMvcConfig implements WebMvcConfigurer {

    /**
     * configureMessageConverters
     */
    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
        final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        final ObjectMapper objectMapper = MyJson.recM();

        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        converters.add(jackson2HttpMessageConverter);
        // 二进制格式
        converters.add(new ByteArrayHttpMessageConverter());
    }

    @Bean
    ObjectMapper objectMapper() {
        return MyJson.recM();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            //重写父类提供的跨域请求处理的接口
            public void addCorsMappings(CorsRegistry registry) {
                //添加映射路径
                registry.addMapping("/**")
                        //放行哪些原始域
                        .allowedOriginPatterns("*")
                        //是否发送Cookie信息
                        .allowCredentials(true)
                        //放行哪些原始域(请求方式)
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        //放行哪些原始域(头部信息)
                        .allowedHeaders("*")
                        //暴露哪些头部信息（因为跨域访问默认不能获取全部头部信息）
                        .exposedHeaders("Header1", "Header2");
            } // addCorsMappings
        };
    } // corsConfigurer
}