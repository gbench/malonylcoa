package gbench.webapps.myfuture.xchg.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import gbench.webapps.myfuture.xchg.model.match.IMatchModel;
import gbench.webapps.myfuture.xchg.model.match.SimpleMatchModel;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;
import jakarta.annotation.PreDestroy;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	ReadyListener(@Value("${xchg.matchorder.interval:5000}") Integer interval, final DataApiClient dataClient) {
		this.matchModel = new SimpleMatchModel(interval, dataClient);
	}

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		matchModel.start();
	}

	@Bean
	public IMatchModel matchModel(@Value("${xchg.matchorder.interval:5000}") Integer interval,
			final DataApiClient dataClient) {
		return new SimpleMatchModel(interval, dataClient);
	}

	@PreDestroy
	public void destroy() {
		matchModel.destroy();
	}

	private IMatchModel matchModel;
}
