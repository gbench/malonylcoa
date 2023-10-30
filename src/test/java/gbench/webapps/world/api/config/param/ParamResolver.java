package gbench.webapps.world.api.config.param;

import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.annotation.AbstractMessageReaderArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class ParamResolver extends AbstractMessageReaderArgumentResolver {

	/**
	 * 
	 * @param readers
	 */
	public ParamResolver(final List<HttpMessageReader<?>> readers) {
		super(readers);
	}

	/**
	 * 
	 * @param messageReaders
	 * @param adapterRegistry
	 */
	protected ParamResolver(final List<HttpMessageReader<?>> messageReaders,
			final ReactiveAdapterRegistry adapterRegistry) {
		super(messageReaders, adapterRegistry);
	}

	/**
	 * 判断是否需要解析参数
	 * 
	 * @param methodParameter
	 * @return
	 */
	@Override
	public boolean supportsParameter(MethodParameter methodParameter) {
		if (!methodParameter.hasParameterAnnotation(Param.class)) {
			return false;
		} else {
			final var parameterAnnotation = methodParameter.getParameterAnnotation(Param.class);
			return parameterAnnotation != null;
		}
	}

	@Override
	public Mono<Object> resolveArgument(final MethodParameter methodParameter, final BindingContext bindingContext,
			final ServerWebExchange serverWebExchange) {
		final var param = methodParameter.getParameter();
		final var parameterAnnotation = methodParameter.getParameterAnnotation(Param.class);
		final var name = Optional.ofNullable(parameterAnnotation.name()) //
				.map(e -> e.matches("^\\s*$") ? null : e).orElse(param.getName());
		@SuppressWarnings("unused")
		final var type = parameterAnnotation.type();
		final var qps = serverWebExchange.getRequest().getQueryParams();
		return serverWebExchange.getFormData().map(data -> {
			final var ll = Optional.ofNullable(data.get(name)).orElse(qps.get(name));
			return ll != null && ll.size() > 0 ? ll.get(0) : null;
		});

	}
}