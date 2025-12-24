package gbench.webapps.myfuture.broker.model.market;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
	public CtpTickDataMQ(String CTP_TOPIC, String KAFKA_BOOTSTRAP_SERVERS, String KAFKA_CONSUMER_GROUP_ID,
			Function<IRecord, Object> tickdata_handler) {
		this.CTP_TOPIC = CTP_TOPIC;
		this.KAFKA_BOOTSTRAP_SERVERS = KAFKA_BOOTSTRAP_SERVERS;
		this.KAFKA_CONSUMER_GROUP_ID = KAFKA_CONSUMER_GROUP_ID;
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
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
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
		this.flag.set(true);
		return true;
	}

	final String CTP_TOPIC;
	final String KAFKA_BOOTSTRAP_SERVERS;
	final String KAFKA_CONSUMER_GROUP_ID;
	private Function<IRecord, Object> tickdata_handler = null;
	private KafkaConsumer<String, String> consumer = null;
	private AtomicReference<Boolean> flag = new AtomicReference<>(false);
	private final Thread consumerThread = new Thread(() -> {
		try {
			// 4. 循环消费消息
			while (!this.flag.get()) {
				final var records = consumer.poll(Duration.ofSeconds(1)); // 拉取消息，超时时间 1 秒

				for (final var record : records) {
					// 打印消息基础信息
					println("\n===== 收到新消息 =====");
					println("分区：" + record.partition() + "，偏移量：" + record.offset());
					println("消息 Key：" + record.key());
					println("消息 Value：" + record.value());

					try {
						tickdata_handler.apply(REC(record.value()));
					} catch (Exception e) {
						println("解析消息 JSON 失败：" + e.getMessage());
						println("失败的消息内容：" + record.value());
					}
				} // for

				Thread.sleep(1000);
			} // while
		} catch (Exception e) {
			println("消费消息异常：" + e.getMessage());
			e.printStackTrace();
		} finally {
			// 6. 关闭消费者，释放资源
			if (this.flag.get()) {
				consumer.close();
				System.out.println("消费者已关闭");
			}
		}
	});
}