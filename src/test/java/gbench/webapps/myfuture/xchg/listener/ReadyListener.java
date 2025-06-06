package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import gbench.webapps.myfuture.xchg.model.match.IMatchModel;
import gbench.webapps.myfuture.xchg.model.match.OrderMatchEngine;
import gbench.webapps.myfuture.xchg.model.match.SimpleMatchEngine;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;
import jakarta.annotation.PreDestroy;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	ReadyListener(@Value("${xchg.matchorder.interval:5000}") Integer interval, final DataApiClient dataClient) {
		this.matchModel = new SimpleMatchEngine(interval, dataClient);
	}

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		matchModel.start();
	}

	@Bean
	public IMatchModel matchModel(@Value("${xchg.matchorder.interval:5000}") Integer interval,
			@Value("${xchg.matchorder.engine:disruptor}") String engine, final DataApiClient dataClient) {
		return switch (String.valueOf(engine).toLowerCase()) {
		case "disruptor" -> {
			println("OrderMatchEngine:%s".formatted(engine));
			yield new OrderMatchEngine(interval, dataClient);
		}
		case "simple" -> {
			println("SimpleMatchEngine:%s".formatted(engine));
			yield new SimpleMatchEngine(interval, dataClient);
		}
		default -> {
			println("OrderMatchEngine:%s".formatted(engine));
			yield new OrderMatchEngine(interval, dataClient);
		}
		};
	}

	@PreDestroy
	public void destroy() {
		matchModel.destroy();
	}

	private IMatchModel matchModel;
}
