package gbench.webapps.myfuture.xchg.msclient;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import reactor.core.publisher.Mono;

/**
 * 微服务客户端
 */
@Component
public class DataApiClient {

	/**
	 * 
	 * @param sql
	 * @return
	 */
	public Mono<DFrame> sqldframe(final String sql) {
		return this.sqlqueryPost(sql);
	}

	/**
	 * 
	 * @param sql
	 * @return
	 */
	public Mono<DFrame> sqlexecute(final String sql) {
		return this.sqlexecutePost(sql);
	}

	/**
	 * 
	 * @param sql
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Mono<DFrame> sqlexecutePost(final String sql) {
		final var formData = new LinkedMultiValueMap<String, String>();
		formData.add("sql", sql);
		final var mono = wb.baseUrl(MYFUTURE_API_MSVC).build().post()
				.uri(uriBuilder -> uriBuilder.path("/api/sqlexecute").build())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(formData))
				.retrieve().bodyToMono(IRecord.class)
				.map(e -> e.llS("data", t -> ((List<IRecord>) t).stream()).collect(DFrame.dfmclc));
		return mono;
	}

	/**
	 * 
	 * @param sql
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Mono<DFrame> sqlqueryPost(final String sql) {
		final var formData = new LinkedMultiValueMap<String, String>();
		formData.add("sql", sql);
		final var mono = wb.baseUrl(MYFUTURE_API_MSVC).build().post()
				.uri(uriBuilder -> uriBuilder.path("/api/sqlquery").build())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(formData))
				.retrieve().bodyToMono(IRecord.class)
				.map(e -> e.llS("data", t -> ((List<IRecord>) t).stream()).collect(DFrame.dfmclc));
		return mono;
	}

	/**
	 * 
	 * @param sql
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Mono<DFrame> sqlqueryGet(final String sql) {
		final var mono = wb.baseUrl(MYFUTURE_API_MSVC).build().get()
				.uri(uriBuilder -> uriBuilder.path("/api/sqlquery").queryParam("sql", sql).build()).retrieve()
				.bodyToMono(IRecord.class)
				.map(e -> e.llS("data", t -> ((List<IRecord>) t).stream()).collect(DFrame.dfmclc));
		return mono;
	}

	@Autowired
	private WebClient.Builder wb;

	final static String MYFUTURE_API_MSVC = "http://myfuture-api";

}
