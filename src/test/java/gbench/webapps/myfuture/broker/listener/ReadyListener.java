package gbench.webapps.myfuture.broker.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import gbench.webapps.myfuture.broker.model.market.DeepMarketDataModel;
import jakarta.annotation.PreDestroy;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	/**
	 * 
	 * @param ctp_topic
	 * @param kafka_bootstrap_servers
	 * @param kafa_consumer_group_id
	 * @param ignite_address
	 * @param prefix_tk
	 * @param prefix_tl
	 * @param tname
	 */
	public ReadyListener(@Value("${broker.dmdm.ctp_topic:my-ctp-topic}") final String ctp_topic,
			@Value("${broker.dmdm.kafka_bootstrap_servers:localhost:9092}") final String kafka_bootstrap_servers,
			@Value("${broker.dmdm.kafa_consumer_group_id:my-ctp-topic_group_ignite-3.10-1}") final String kafa_consumer_group_id,
			@Value("${broker.dmdm.ignite_address:localhost:10800}") final String ignite_address,
			@Value("${broker.dmdm.prefix_tk:TK}") final String prefix_tk,
			@Value("${broker.dmdm.prefix_tl:TL}") final String prefix_tl,
			@Value("${broker.dmdm.tname:TBL}") final String tname) {

		this.dmdm = this.mmc(ctp_topic, kafka_bootstrap_servers, kafa_consumer_group_id, ignite_address, prefix_tk,
				prefix_tl, tname);
	}

	/**
	 * 
	 */
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		// 启动1分钟K线的绘制
		this.dmdm.kline1m();
	}

	/**
	 * 
	 * @param ctp_topic
	 * @param kafka_bootstrap_servers
	 * @param kafa_consumer_group_id
	 * @param ignite_address
	 * @param prefix_TK
	 * @param prefix_tl
	 * @param tname
	 * @return
	 */
	public DeepMarketDataModel mmc(final String ctp_topic, final String kafka_bootstrap_servers,
			final String kafa_consumer_group_id, final String ignite_address, final String prefix_TK,
			final String prefix_tl, final String tname) {

		return new DeepMarketDataModel(ctp_topic, kafka_bootstrap_servers, kafa_consumer_group_id, ignite_address,
				prefix_TK, prefix_tl, tname);
	}

	/**
	 * 
	 */
	@PreDestroy
	public void destroy() {

		dmdm.stop();
	}

	private DeepMarketDataModel dmdm;
}
