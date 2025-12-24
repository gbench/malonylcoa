package gbench.webapps.myfuture.broker.model.market;

import static gbench.util.io.Output.println;
import static gbench.webapps.myfuture.broker.model.market.CtpIgniteData.*;

import org.junit.jupiter.api.Test;
import gbench.util.io.Output;

public class DeepMarketDataModel {

	@Test
	public void foo() throws InterruptedException {
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, //
				rec -> println(rec)).initialize().start(); //
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void bar() {
		final var ignite = new CtpIgniteData(IGNITE_ADDRESS);
		ignite.withTransaction(client -> {
			final var tbl = client.tables().table("t_mtcars");
			println(tbl);
			final var rs = client.sql().execute(null, "select * from person");
			println(rs2df(rs));
			return null;
		});

	}

	// 配置参数（对应你给出的参数）
	private static final String CTP_TOPIC = "test_cxx_ctp_topic";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.1.41:9092";
	private static final String KAFKA_CONSUMER_GROUP_ID = "ctp_cxx_ctp_topic_group_ignite-3.10";
	private static final String IGNITE_ADDRESS = "192.168.1.41:10800";

}
