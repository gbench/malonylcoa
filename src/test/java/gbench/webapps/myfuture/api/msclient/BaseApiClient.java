package gbench.webapps.myfuture.api.msclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import gbench.util.lisp.IRecord;
import reactor.core.publisher.Mono;

/**
 * 微服务客户端
 */
@Component
public class BaseApiClient {

	/**
	 * 
	 * @param path
	 * @param params
	 * @return
	 */
	public Mono<IRecord> postJson(final String path, final IRecord params) {
		final var mono = wb.baseUrl(MYFUTURE_API_MSVC).build().post().uri(uriBuilder -> uriBuilder.path(path).build())
				.contentType(MediaType.APPLICATION_JSON).bodyValue(params).retrieve().bodyToMono(IRecord.class);
		return mono;
	}

	/**
	 * 
	 * @param path
	 * @param params
	 * @return
	 */
	public Mono<IRecord> postForm(final String path, final IRecord params) {
		final var formData = new LinkedMultiValueMap<String, String>();
		params.forEach((k, v) -> {
			formData.add(k, String.valueOf(v));
		});

		final var mono = wb.baseUrl(MYFUTURE_API_MSVC).build().post().uri(uriBuilder -> uriBuilder.path(path).build())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(formData))
				.retrieve().bodyToMono(IRecord.class);
		return mono;
	}

	@Autowired
	private WebClient.Builder wb;

	final static String MYFUTURE_API_MSVC = "http://myfuture-api";

}
