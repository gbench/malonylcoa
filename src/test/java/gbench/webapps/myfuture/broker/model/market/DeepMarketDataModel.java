package gbench.webapps.myfuture.broker.model.market;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.ignite.table.*;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;

import gbench.util.jdbc.kvp.IRecord;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.webapps.myfuture.broker.model.market.CtpIgniteDB.*;
import static org.apache.ignite.catalog.definitions.ColumnDefinition.column;

public class DeepMarketDataModel {

	@Test
	public void foo() throws InterruptedException {
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, //
				rec -> println(rec)).initialize().start(); //
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void bar() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		igniteDB.withTransaction(client -> {
			final var tbl = client.tables().table("AO2601");
			final var rs = client.sql().execute(null, "select * from system.tables");
			println(tbl);
			println(rs2df(rs));
			println(igniteDB.tableExists("t_mtcars"));
			return null;
		});

	}

	@Test
	public void quz() throws InterruptedException {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final Function<IRecord, Object> tickdata_handler = rec -> {
			igniteDB.put(rec, "InstrumentID", row -> println("row:%s".formatted(row))); // 数据写入
			sleep(1000);
			return null;
		};
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, tickdata_handler).initialize()
				.start();
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void foo1() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		igniteDB.withTransaction(client -> {
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
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		igniteDB.put(proto, "InstrumentID", row -> println("row:%s".formatted(row)));
	}

	@Test
	void quz_kline1m_final() throws Exception {
		var db = new CtpIgniteDB(IGNITE_ADDRESS);
		var bar = new ConcurrentHashMap<String, IRecord>();
		final Function<IRecord, Object> tick_handler = tick -> {
			final var epoch = tick.lngopt("Epoch")
					.orElseGet(() -> LocalDateTime
							.parse(tick.str("TradingDay") + ' ' + tick.str("UpdateTime"),
									DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"))
							.plusNanos(tick.i4("UpdateMillisec") * 1_000_000).atZone(ZoneId.of("Asia/Shanghai"))
							.toInstant().toEpochMilli());

			final var iid = tick.str("InstrumentID");
			final var ymdhm = Instant.ofEpochMilli(epoch).atZone(ZoneId.of("Asia/Shanghai"))
					.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
			final var key = iid + '_' + ymdhm;
			final var px = tick.dbl("LastPrice");
			final var vol = tick.i4("Volume");

			/* 聚合：不含 InstrumentID */
			bar.merge(key, IRecord.REC("TS", ymdhm, "OPEN", px, "HIGH", px, "LOW", px, "CLOSE", px, "VOLUME", vol),
					(o, _) -> o.add(IRecord.REC("HIGH", Math.max(o.dbl("HIGH"), px), "LOW", Math.min(o.dbl("LOW"), px),
							"CLOSE", px, "VOLUME", o.i4("VOLUME") + vol)));

			/* 分钟结束：纯净 K 线（无 InstrumentID）直接写入 */
			if (System.currentTimeMillis() / 60_000 > Long.parseLong(ymdhm.substring(8))) {
				final var kline = IRecord.REC("TBL", "KLINE_%s".formatted(iid), "TS", bar.get(key).str("TS"), "OPEN",
						bar.get(key).dbl("OPEN"), "HIGH", bar.get(key).dbl("HIGH"), "LOW", bar.get(key).dbl("LOW"),
						"CLOSE", bar.get(key).dbl("CLOSE"), "VOLUME", bar.get(key).i4("VOLUME"));
				println("%s:%s".formatted(key, kline));
				db.put(kline, "TBL", e -> {
					println(e);
				}, "TS"); // 表名靠 kline 里的 TS 去拼？不对！
			}
			return null;
		};

		// 连接进入交易消息队列进行tickdata的处理
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, tick_handler).initialize()
				.start();

		Thread.sleep(1_000_000);
	}

	@Test
	public void dropTables() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final var dfm = igniteDB.sqldframe("SELECT TABLE_NAME name from SYSTEM.TABLES");
		final var patterns = REC( //
				"instrument", "[A-Z]+\\d{3,}([A-Z]+\\d{4,})?", // 期货合约
				"kline", "^KLINE_.*", // K线
				"tbl", "^TBL_.*");
		final var pk = "kline";
		final var symbols = dfm.filterBy(rec -> rec.str("name").matches(patterns.str(pk)));
		println(symbols);
		symbols.rowS().forEach(e -> {
			final var tbl = e.str(0);
			final var sql = "DROP TABLE %s".formatted(tbl);
			println("%s:%s".formatted(tbl, igniteDB.sqldframe(sql)));
		});
	}

	@Test
	public void selectTables() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final var dfm = igniteDB.sqldframe("SELECT TABLE_NAME name from SYSTEM.TABLES");
		final var symbols = dfm.filterBy(rec -> rec.str("name").matches("[A-Z]+\\d{4}([A-Z]+\\d{4,})?"));
		symbols.rowS().forEach(e -> {
			final var tbl = e.str(0);
			// final var sql = "SELECT ID, UPDATETIME, LASTPRICE, VOLUME FROM %s ORDER BY ID
			// limit 10".formatted(tbl);
			final var sql = "SELECT * FROM %s limit 10".formatted(tbl);
			println("%s\n:%s\n".formatted(tbl, igniteDB.sqldframe(sql)));
		});
	}

	/**
	 * 
	 * @param millis
	 */
	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // 等待
	}

	// 配置参数（对应你给出的参数）
	private static final String CTP_TOPIC = "test_cxx_ctp_topic";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.1.41:9092";
	private static final String KAFKA_CONSUMER_GROUP_ID = "ctp_cxx_ctp_topic_group_ignite-3.10";
	private static final String IGNITE_ADDRESS = "192.168.1.41:10800";

}
