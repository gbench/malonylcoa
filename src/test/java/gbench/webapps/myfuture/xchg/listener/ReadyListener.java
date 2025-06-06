package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import gbench.webapps.myfuture.xchg.model.match.IMatchModel;
import gbench.webapps.myfuture.xchg.model.match.OrderMatchEngine;
import gbench.webapps.myfuture.xchg.model.match.SimpleMatchEngine;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;

import jakarta.annotation.PreDestroy;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	/**
	 * 
	 * @param interval
	 * @param engine:    simple,disruptor,disruptor 目前有问题：会重复处理
	 * @param dataClient
	 */
	public ReadyListener(@Value("${xchg.matchorder.interval:5000}") Integer interval,
			@Value("${xchg.matchorder.engine:simple}") String engine, final DataApiClient dataClient) {
		this.matchEngine = this.buildMatchEngine(interval, engine, dataClient);
	}

	/**
	 * 
	 */
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		matchEngine.start();
	}

	/**
	 * 
	 * @param interval
	 * @param engine
	 * @param dataClient
	 * @return
	 */
	public IMatchModel buildMatchEngine(final Integer interval, final String engine, final DataApiClient dataClient) {
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
		matchEngine.destroy();
	}

	private IMatchModel matchEngine;
}
