package gbench.webapps.myfuture.broker.model.market;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import gbench.util.jdbc.kvp.IRecord;

/**
 * Kafka 消费者示例：读取 CTP 主题消息，解析合约编码和交易日期
 */
public class CtpTickDataMQ {

	/**
	 * 消息处理程序
	 * 
	 * @param handler
	 */
	public CtpTickDataMQ(final String CTP_TOPIC, final String KAFKA_BOOTSTRAP_SERVERS,
			final String KAFKA_CONSUMER_GROUP_ID, final String KAFKA_AUTO_OFFSET_RESET_CONFIG,
			final Function<IRecord, Object> tickdata_handler) {
		this.CTP_TOPIC = CTP_TOPIC;
		this.KAFKA_BOOTSTRAP_SERVERS = KAFKA_BOOTSTRAP_SERVERS;
		this.KAFKA_CONSUMER_GROUP_ID = KAFKA_CONSUMER_GROUP_ID;
		this.KAFKA_AUTO_OFFSET_RESET_CONFIG = KAFKA_AUTO_OFFSET_RESET_CONFIG;
		this.tickdata_handler = tickdata_handler;
	}

	/**
	 * 
	 */
	public CtpTickDataMQ initialize() {
		// 1. 配置消费者属性
		Properties props = new Properties();
		// Kafka 服务地址
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
		// 消费者组 ID
		props.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_CONSUMER_GROUP_ID);
		// 反序列化器（消息 key 和 value 都用字符串解析）
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		// 首次消费策略：earliest = 从最早的消息开始消费，latest = 从最新的开始
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KAFKA_AUTO_OFFSET_RESET_CONFIG);
		// 自动提交 offset（新手推荐，生产环境可改为手动提交）
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

		// 2. 创建消费者实例
		this.consumer = new KafkaConsumer<>(props);

		// 3. 订阅指定主题
		consumer.subscribe(Collections.singletonList(CTP_TOPIC));
		System.out.println("开始消费 Kafka 主题：" + CTP_TOPIC);
		System.out.println("Kafka 服务地址：" + KAFKA_BOOTSTRAP_SERVERS);

		return this;
	}

	/**
	 * 启动Kafka消息队列
	 */
	public boolean start() {

		if (tickdata_handler != null) {
			consumerThread.setName("CTP_MQ");
			consumerThread.start();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean stop() {
		this.stopflag.set(true);
		return true;
	}

	/**
	 * 睡眠时常
	 * 
	 * @return
	 */
	public CtpTickDataMQ sleepInterval(long ms) {
		this.sleepInterval.set(ms);
		return this;
	}

	private final String CTP_TOPIC;
	private final String KAFKA_BOOTSTRAP_SERVERS;
	private final String KAFKA_CONSUMER_GROUP_ID;
	private final String KAFKA_AUTO_OFFSET_RESET_CONFIG;
	private final AtomicLong sleepInterval = new AtomicLong(1000);
	private Function<IRecord, Object> tickdata_handler = null;
	private KafkaConsumer<String, String> consumer = null;
	private AtomicReference<Boolean> stopflag = new AtomicReference<>(false);
	private final Thread consumerThread = new Thread(() -> {
		/* 0. 只建一次线程池 */
		final var ai = new AtomicInteger();
		final var pool = new ThreadPoolExecutor(4, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000), r -> {
			final var t = new Thread(r, "instrument-worker-" + ai.getAndIncrement());
			t.setDaemon(true);
			return t;
		}, new ThreadPoolExecutor.CallerRunsPolicy());

		try {
			while (!stopflag.get()) {
				final var records = consumer.poll(Duration.ofSeconds(1));
				if (records.count() == 0) { // 快速短路
					if (sleepInterval.get() > 0)
						Thread.sleep(sleepInterval.get());
					continue;
				}

				/* 1. 分组 -> 2. 直接 forEach 提交任务 */
				StreamSupport.stream(records.spliterator(), false)
						.map(rec -> REC("ID", rec.offset()).derive(REC(rec.value())))
						.collect(Collectors.groupingBy(r -> r.str("InstrumentID")))
						.forEach((_, es) -> pool.execute(() -> es.forEach(tickdata_handler::apply)));

				consumer.commitAsync(); // 批次末尾一次性提交
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt(); // 保留中断态
		} catch (Exception e) {
			println("消费消息异常：" + e.getMessage());
			e.printStackTrace();
		} finally {
			pool.shutdown(); // 先停业务线程
			pool.close();
			consumer.close(); // 再关客户端
			println("消费者已关闭");
		}
	});
}