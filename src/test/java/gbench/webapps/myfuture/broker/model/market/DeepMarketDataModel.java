package gbench.webapps.myfuture.broker.model.market;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.ignite.Ignite;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.DataStreamerItem;
import org.apache.ignite.table.DataStreamerOptions;
import org.apache.ignite.table.DataStreamerReceiver;
import org.apache.ignite.table.DataStreamerReceiverContext;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gbench.util.io.Output;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.DFrame;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.webapps.myfuture.broker.model.market.DeepMarketDataModel.CtpIgniteData.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Spliterators;

import jakarta.annotation.Nullable;

public class DeepMarketDataModel {

	public static class Account {
		Account(int id) {
			this(id, null, null, null);
		}

		Account(int id, String name, Long value, Boolean flag) {
			this.id = id;
			this.name = name;
			this.value = value;
			this.flag = flag;
		}

		@Override
		public String toString() {
			return "Account [id=" + id + ", name=" + name + ", value=" + value + ", flag=" + flag + "]";
		}

		private Integer id;
		private String name;
		private Long value;
		private boolean flag;
	}

	public static class RecordViewPojoDataStreamerExample {
		private static final int ACCOUNTS_COUNT = 1000;

		public static void main(String[] args) throws Exception {
			/**
			 * Assuming the 'accounts' table already exists.
			 */
			try (IgniteClient client = IgniteClient.builder().addresses(IGNITE_ADDRESS).build()) {
				RecordView<Account> view = client.tables().table("accounts").recordView(Account.class);

				streamAccountDataPut(view);
				streamAccountDataRemove(view);
			}
		}

		/**
		 * Streaming data using DataStreamerOperationType#PUT operation type.
		 */
		private static void streamAccountDataPut(RecordView<Account> view) {
			DataStreamerOptions options = DataStreamerOptions.builder().pageSize(1000).perPartitionParallelOperations(1)
					.autoFlushInterval(1000).retryLimit(16).build();

			CompletableFuture<Void> streamerFut;
			try (var publisher = new SubmissionPublisher<DataStreamerItem<Account>>()) {
				streamerFut = view.streamData(publisher, options);
				ThreadLocalRandom rnd = ThreadLocalRandom.current();
				for (int i = 0; i < ACCOUNTS_COUNT; i++) {
					Account entry = new Account(i, "name" + i, rnd.nextLong(100_000), rnd.nextBoolean());
					publisher.submit(DataStreamerItem.of(entry));
				}
			}
			streamerFut.join();
		}

		/**
		 * Streaming data using DataStreamerOperationType#REMOVE operation type
		 */
		private static void streamAccountDataRemove(RecordView<Account> view) {
			DataStreamerOptions options = DataStreamerOptions.builder().pageSize(1000).perPartitionParallelOperations(1)
					.autoFlushInterval(1000).retryLimit(16).build();

			CompletableFuture<Void> streamerFut;
			try (var publisher = new SubmissionPublisher<DataStreamerItem<Account>>()) {
				streamerFut = view.streamData(publisher, options);
				for (int i = 0; i < ACCOUNTS_COUNT; i++) {
					Account entry = new Account(i);
					publisher.submit(DataStreamerItem.removed(entry));
				}
			}
			streamerFut.join();
		}
	}

	public static class TwoTableReceiver implements DataStreamerReceiver<Tuple, Void, Void> {
		@Override
		public @Nullable CompletableFuture<List<Void>> receive(List<Tuple> page, DataStreamerReceiverContext ctx,
				@Nullable Void arg) {
			// List<Tuple> is the source data. Those tuples do not conform to any table and
			// can have arbitrary data.

			RecordView<Tuple> customersTable = ctx.ignite().tables().table("customers").recordView();
			RecordView<Tuple> addressesTable = ctx.ignite().tables().table("addresses").recordView();

			for (Tuple sourceItem : page) {
				// For each source item, receiver extracts customer and address data and upserts
				// it into respective tables.
				Tuple customer = Tuple.create().set("id", sourceItem.intValue("customerId"))
						.set("name", sourceItem.stringValue("customerName"))
						.set("addressId", sourceItem.intValue("addressId"));

				Tuple address = Tuple.create().set("id", sourceItem.intValue("addressId"))
						.set("street", sourceItem.stringValue("street")).set("city", sourceItem.stringValue("city"));

				customersTable.upsert(null, customer);
				addressesTable.upsert(null, address);
			}

			return null;
		}
	}

	/**
	 * Kafka 消费者示例：读取 CTP 主题消息，解析合约编码和交易日期
	 */
	public static class CtpTickDataMQ {

		/**
		 * 消息处理程序
		 * 
		 * @param handler
		 */
		public CtpTickDataMQ(Function<IRecord, Object> tickdata_handler) {
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

	/**
	 * 
	 */
	public static class CtpIgniteData {

		/**
		 * 
		 * @param addresses
		 */
		public CtpIgniteData(final String... addresses) {
			this.addresses = addresses;
		}

		/**
		 * 
		 */
		public static void initialized() {
			// do nothing
		}

		/**
		 * 
		 * @param handler
		 * @return
		 */
		public CtpIgniteData withTransaction(Function<IgniteClient, Object> handler) {
			try (IgniteClient client = IgniteClient.builder().addresses(addresses).build()) {
				handler.apply(client);
			}
			return this;
		}

		// SqlRow 转 Map 核心方法（极简版）
		public static Map<String, Object> asMap(SqlRow row) {
			final var map = new LinkedHashMap<String, Object>();
			if (null != row) {
				println(row);
				// 遍历所有列名，填充键值对
				for (int i = 0; i < row.columnCount(); i++) {
					String colName = row.columnName(i);
					map.put(colName, row.value(i));
				}
			}
			return map;
		}

		public static <T> Stream<T> rs2S(org.apache.ignite.sql.ResultSet<T> rs) {
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rs, 0), false);
		}

		public static <T> DFrame rs2df(org.apache.ignite.sql.ResultSet<T> rs) {
			return rs2S(rs).map(e -> asMap((SqlRow) e)).collect(DFrame.dfmclc2);
		}

		private final String[] addresses;
	}

	/**
	 * CtpEngineModel
	 */
	public static class CtpEngineModel {

		@Test
		void foo() throws InterruptedException {
			new CtpTickDataMQ(rec -> println(rec)).initialize().start(); //
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
	}

	// 配置参数（对应你给出的参数）
	private static final String CTP_TOPIC = "test_cxx_ctp_topic";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.1.41:9092";
	private static final String KAFKA_CONSUMER_GROUP_ID = "ctp_cxx_ctp_topic_group_ignite-3.10";
	public static final String IGNITE_ADDRESS = "192.168.1.41:10800";

}
