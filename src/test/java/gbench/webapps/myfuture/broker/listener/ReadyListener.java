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
	 * @param kafka_auto_offset_reset_config
	 * @param ignite_address
	 * @param prefix_tk
	 * @param prefix_kl
	 * @param tname
	 */
	public ReadyListener(@Value("${broker.dmdm.ctp_topic:my-ctp-topic}") final String ctp_topic,
			@Value("${broker.dmdm.kafka_bootstrap_servers:localhost:9092}") final String kafka_bootstrap_servers,
			@Value("${broker.dmdm.kafka_consumer_group_id:my-ctp-topic_group_ignite-3.10-1}") final String kafa_consumer_group_id,
			@Value("${broker.dmdm.kafka_auto_offset_reset_config:latest}") final String kafka_auto_offset_reset_config,
			@Value("${broker.dmdm.ignite_address:localhost:10800}") final String ignite_address,
			@Value("${broker.dmdm.prefix_tk:TK}") final String prefix_tk,
			@Value("${broker.dmdm.prefix_kl:KL}") final String prefix_kl,
			@Value("${broker.dmdm.tname:TBL}") final String tname) {

		this.dmdm = this.dmdm(ctp_topic, kafka_bootstrap_servers, kafa_consumer_group_id,
				kafka_auto_offset_reset_config, ignite_address, prefix_tk, prefix_kl, tname);
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
	 * @param kafka_auto_offset_reset_config
	 * @param ignite_address
	 * @param prefix_tk
	 * @param prefix_kl
	 * @param tname
	 * @return
	 */
	public DeepMarketDataModel dmdm(final String ctp_topic, final String kafka_bootstrap_servers,
			final String kafa_consumer_group_id, final String kafka_auto_offset_reset_config,
			final String ignite_address, final String prefix_tk, final String prefix_kl, final String tname) {

		final var dmdm = new DeepMarketDataModel(ctp_topic, kafka_bootstrap_servers, kafa_consumer_group_id,
				kafka_auto_offset_reset_config, ignite_address, prefix_tk, prefix_kl, tname);

		System.out.println("------------------\n# %s\n------------------\n".formatted(dmdm.toString()));

		return dmdm;
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
