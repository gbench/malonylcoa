package gbench.webapps.mymall.api.config.param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.annotation.AbstractMessageReaderArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import gbench.util.json.MyJson;
import gbench.util.type.Types;
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
	public boolean supportsParameter(final MethodParameter methodParameter) {
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
		final var type0 = parameterAnnotation.type();
		final var type1 = methodParameter.getParameterType();
		final var type = type0 == Object.class ? type1 : type0;
		final var qps = serverWebExchange.getRequest().getQueryParams();
		final BiFunction<String, Class<?>, Object> read_json = (value, cls) -> { // json 类型读取函数
			Object obj = null;
			try {
				obj = MyJson.recM().readValue(value, cls);
			} catch (Exception e) {
				try {
					if (type.isArray()
							|| Iterable.class.isAssignableFrom(type) && !value.matches("\\s*\\[\\.+\\]\\s*")) {
						obj = MyJson.recM().readValue(String.format("[%s]", value), cls);
					} else if (Map.class.isAssignableFrom(type) && !value.matches("\\s*\\{\\.+\\}\\s*")) {
						obj = MyJson.recM().readValue(String.format("[%s]", value), cls);
					} else {
						e.printStackTrace();
					} // if
				} catch (Exception e1) {
					e1.printStackTrace();
				} // try
			} // try
			return obj;
		}; // read_json

		return serverWebExchange.getFormData().map(data -> {
			final var ll = Optional.ofNullable(data.get(name)).orElse(qps.get(name));
			final Optional<Object> opt = Optional.ofNullable(ll != null && ll.size() > 0 ? ll.get(0) : null)
					.map(value -> {
						if (type.isArray() || Iterable.class.isAssignableFrom(type)
								|| Map.class.isAssignableFrom(type)) { // 数组,集合,Map类型
							return read_json.apply(value, type);
						} else { // 其他类型
							return switch (type.getName()) { // 根据类型名进行类型转换
							case "gbench.util.lisp.IRecord" -> gbench.util.lisp.IRecord.REC(value);
							case "gbench.util.jdbc.kvp.IRecord" -> gbench.util.jdbc.kvp.IRecord.REC(value);
							case "gbench.util.data.DataApp.IRecord" -> gbench.util.data.DataApp.IRecord.REC(value);
							case "gbench.util.math.algebra.tuple.IRecord" ->
								gbench.util.math.algebra.tuple.IRecord.REC(value);
							default -> (Object) Types.corece(value, type);
							};
						} // if
					});
			return opt.orElse(Types.defaultValue(type));
		});
	}
}