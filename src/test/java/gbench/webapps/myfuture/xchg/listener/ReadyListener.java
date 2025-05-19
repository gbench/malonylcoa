package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;

import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		final var thread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				final var ordsql = """
							select * from t_order where SECURITY_ID=$0 and ID not in (
								(select SHORT_ORDER_ID from t_match_order where SECURITY_ID=$0) union
								(select LONG_ORDER_ID from t_match_order where SECURITY_ID=$0)
							)
						""";
				this.getSecurities().map(dfm -> dfm.colS(0, IRecord.obj2int())).flatMapMany(Flux::fromStream)
						.subscribe(securityid -> {
							println("-------------------------------------------");
							println("securityid:%s".formatted(securityid));
							println("-------------------------------------------");
							final var sql = IRecord.FT(ordsql, securityid);
							println(sql);
							this.sqlqueryPost(sql).subscribe(ordfrm -> {
								println(ordfrm);
							});
							println("-------------------------------------------\n");
						});
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	Mono<DFrame> getSecurities() {
		final var sql = """
					select distinct SECURITY_ID from t_order
				""";
		return sqlqueryPost(sql);
	}

	@SuppressWarnings("unchecked")
	Mono<DFrame> sqlqueryPost(final String sql) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("sql", sql);
		final var mono = wb.baseUrl("http://myfuture-api").build().post()
				.uri(uriBuilder -> uriBuilder.path("/api/sqlquery").build())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(formData))
				.retrieve().bodyToMono(IRecord.class)
				.map(e -> e.llS("data", t -> ((List<IRecord>) t).stream()).collect(DFrame.dfmclc));
		return mono;
	}

	@SuppressWarnings("unchecked")
	Mono<DFrame> sqlqueryGet(final String sql) {
		final var mono = wb.baseUrl("http://myfuture-api").build().get()
				.uri(uriBuilder -> uriBuilder.path("/api/sqlquery").queryParam("sql", sql).build()).retrieve()
				.bodyToMono(IRecord.class)
				.map(e -> e.llS("data", t -> ((List<IRecord>) t).stream()).collect(DFrame.dfmclc));
		return mono;
	}

	@Autowired
	private WebClient.Builder wb;

}
