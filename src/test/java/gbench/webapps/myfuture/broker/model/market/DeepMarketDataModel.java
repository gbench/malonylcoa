package gbench.webapps.myfuture.broker.model.market;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.webapps.myfuture.broker.model.market.CtpIgniteData.*;
import static org.apache.ignite.catalog.definitions.ColumnDefinition.column;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ignite.table.*;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.junit.jupiter.api.Test;
import gbench.util.io.Output;
import gbench.util.jdbc.kvp.IRecord;

public class DeepMarketDataModel {

	@Test
	public void foo() throws InterruptedException {
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, //
				rec -> println(rec)).initialize().start(); //
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void bar() {
		final var igniteData = new CtpIgniteData(IGNITE_ADDRESS);
		igniteData.withTransaction(client -> {
			final var tbl = client.tables().table("AO2601");
			final var rs = client.sql().execute(null, "select * from system.tables");
			println(tbl);
			println(rs2df(rs));
			println(igniteData.tableExists("t_mtcars"));
			return null;
		});

	}

	@Test
	public void quz() throws InterruptedException {
		final var igniteData = new CtpIgniteData(IGNITE_ADDRESS);
		final var tblFlags = new HashMap<String, Boolean>();
		final var ai = new AtomicInteger();
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, //
				rec -> {
					println(rec);
					final var inst = rec.str("InstrumentID");
					final var flag = tblFlags.computeIfAbsent(inst, e -> igniteData.tableExists(inst));

					if (!flag) {
						igniteData.createTable(inst, rec);
					}

					igniteData.withTransaction(client -> {
						final var tblApi = client.tables();
						final var view = tblApi.table(inst).recordView(Tuple.class);
						final var options = DataStreamerOptions.builder().pageSize(1000)
								.perPartitionParallelOperations(1).autoFlushInterval(1000).retryLimit(16).build();
						CompletableFuture<Void> streamerFut;
						try (final var publisher = new SubmissionPublisher<DataStreamerItem<Tuple>>()) {
							streamerFut = view.streamData(publisher, options);
							final var tup = rec.tupleS().reduce(Tuple.create(), (acc, a) -> acc.set(a._1(), a._2()),
									(a, b) -> a);
							publisher.submit(DataStreamerItem.of(tup));
							streamerFut.join();
						}

						return null;
					});

					try {
						Thread.sleep(1000000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} // 等待
					return null;
				}).initialize().start(); //
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void foo1() {
		final var igniteData = new CtpIgniteData(IGNITE_ADDRESS);
		igniteData.withTransaction(client -> {
			IgniteCatalog catalog = client.catalog();
			catalog.createTable(TableDefinition.builder("sampleTable3").primaryKey("myKey")
					.columns(column("myKey", ColumnType.INT32), column("myValue", ColumnType.VARCHAR)).build());

			final var myTable = client.tables().table("sampleTable3");
			myTable.keyValueView().put(null, Tuple.create().set("myKey", 1), Tuple.create().set("myValue", "John"));
			Tuple value = myTable.keyValueView().get(null, Tuple.create().set("myKey", 1));
			System.out.println("\nRetrieved value:\n" + value.stringValue("myValue"));
			return null;
		});
	}

	@Test
	public void foo2() {
		final var json = """
						{"ID":536908,"ActionDay":"20251224","AskPrice1":2566.0,"AskPrice2":0.0,"AskPrice3":0.0,"AskPrice4":0.0,"AskPrice5":0.0,"AskVolume1":24,"AskVolume2":0,"AskVolume3":0,"AskVolume4":0,"AskVolume5":0,"AveragePrice":50937.99374408962,"BidPrice1":2565.0,"BidPrice2":0.0,"BidPrice3":0.0,"BidPrice4":0.0,"BidPrice5":0.0,"BidVolume1":58,"BidVolume2":0,"BidVolume3":0,"BidVolume4":0,"BidVolume5":0,"ClosePrice":0.0,"CurrDelta":0.0,"CxxCtpCreateTime":"2025-12-24 13:31:30","ExchangeID":"","ExchangeInstID":"","HighestPrice":2581.0,"InstrumentID":"ao2601","LastPrice":2565.0,"LowerLimitPrice":2343.0,"LowestPrice":2520.0,"OpenInterest":113314.0,"OpenPrice":2520.0,"PreClosePrice":2520.0,"PreDelta":0.0,"PreOpenInterest":135956.0,"PreSettlementPrice":2520.0,"SettlementPrice":0.0,"TradingDay":"20251224","Turnover":7.7026906E9,"UpdateMillisec":500,"UpdateTime":"13:31:29","UpperLimitPrice":2696.0,"Volume":151217}
				""";
		final var proto = REC(json);

		final var igniteData = new CtpIgniteData(IGNITE_ADDRESS);
		igniteData.withTransaction(client -> {
			final var tdb = proto2tdb(proto.toMap(), proto.str("InstrumentID"));
			final var createFut = client.catalog().createTableAsync(tdb.build());
			createFut.thenCompose(tbl -> {
				final var tup = proto.tupleS().reduce(Tuple.create(), (acc, a) -> acc.set(a._1(), a._2()), (a, b) -> a);
				// 创建 key tuple（使用 id 作为主键）
				final var keyTuple = Tuple.create().set("ID", proto.get("ID"));
				final var kvv = tbl.keyValueView();
				// 执行插入操作
				kvv.put(null, keyTuple, tup);
				final var retrievedTuple = kvv.get(null, keyTuple);
				println("--------------%s".formatted(retrievedTuple));
				return null;
			});

			return null;
		});

	}

	// 配置参数（对应你给出的参数）
	private static final String CTP_TOPIC = "test_cxx_ctp_topic";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.1.41:9092";
	private static final String KAFKA_CONSUMER_GROUP_ID = "ctp_cxx_ctp_topic_group_ignite-3.10";
	private static final String IGNITE_ADDRESS = "192.168.1.41:10800";

}
