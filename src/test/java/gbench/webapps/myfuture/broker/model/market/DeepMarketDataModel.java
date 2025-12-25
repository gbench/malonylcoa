package gbench.webapps.myfuture.broker.model.market;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
	public void quz_tk() throws InterruptedException {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final Function<IRecord, Object> tickdata_handler = tick -> {
			igniteDB.put(tick.add(TNAME, "%s_%s".formatted(PREFIX_TK, tick.str("InstrumentID"))), TNAME,
					e -> println("row:%s".formatted(e))); // 数据写入
			sleep(1000);
			return null;
		};
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, tickdata_handler).initialize()
				.start();
		Thread.sleep(1000000); // 等待
	}

	/**
	 * 动态K线生成
	 * 
	 * @throws Exception
	 */
	@Test
	void quz_kline1m_final() throws Exception {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final Map<String, IRecord> kcache = new ConcurrentHashMap<String, IRecord>(); // 本地计算kline的缓存cache:key为{instrument}_{yyyyMMddHHmm}
		final var dtf = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
		final var krb = IRecord.rb("TS,OPEN,HIGH,LOW,CLOSE,VOLUME,VOL0,VOL1,TIMES,UPTIME"); // K线数据格式, 累计成交量
		final var dtf_ymdhm = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
		final var dtf_ymdhmsS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss.SSS");
		final var CLEAN_INTERVAL = 5 * 60;
		final var kcache_cleaner = ((BiFunction<Integer, Integer, Consumer<Map<String, IRecord>>>) (expired,
				maxsize) -> cache -> { // 对缓存进行清理，当cache的size大于maxsize时。把cache中距离当前系统时间大于expired时长的kline项目给予清楚
					if (cache.size() > maxsize) {
						final var now = Instant.now();
						cache.entrySet()
								.removeIf(e -> now.toEpochMilli() - LocalDateTime
										.parse(e.getKey().split("_")[1], dtf_ymdhm).atZone(ZoneId.of("Asia/Shanghai"))
										.toInstant().toEpochMilli() > expired);
					} // if
				}).apply(CLEAN_INTERVAL * 1_000, 100_00);

		final Function<IRecord, Object> tickdata_handler = tick -> {
			final var iid = tick.str("InstrumentID");
			final var zdt0 = LocalDateTime.parse("%s %s".formatted(tick.str("ActionDay"), tick.str("UpdateTime")), dtf)
					.plusNanos(tick.i4("UpdateMillisec") * 1_000_000).atZone(ZoneId.of("Asia/Shanghai"));
			final var epoch = tick.lngopt("Epoch").orElseGet(() -> zdt0.toInstant().toEpochMilli());
			final var zdt = Instant.ofEpochMilli(epoch).atZone(ZoneId.of("Asia/Shanghai"));
			final var ymdhm = zdt.format(dtf_ymdhm); // K线的主键(合约表的
			final var uptime = zdt.format(dtf_ymdhmsS); // 更新时间
			final var key = "%s_%s".formatted(iid, ymdhm); // K线的分钟K归集主键
			final var px = tick.dbl("LastPrice"); // 成交价格
			final var vol = tick.i4("Volume"); // tick投递的Volume是当日的累计成交量:vol0,起点量vol1终点量,volume:期间流量
			final var value = krb.get(ymdhm, px, px, px, px, vol, vol, vol, 1, uptime);
			kcache.merge(key, value, (o, _) -> // 依据合约时间分组key进行K线聚合
			o.add(REC("HIGH", Math.max(o.dbl("HIGH"), px), "LOW", Math.min(o.dbl("LOW"), px), "CLOSE", px, "VOLUME",
					vol - o.i4("VOL0"), "VOL1", vol, "TIMES", o.i4("TIMES") + 1, "UPTIME", uptime))); // 根据key进行K线聚合
			final var kline = kcache.get(key); // 提取
			final Consumer<Tuple> callback = e -> {
				println("upate %s:%s".formatted(kline.str(TNAME), e));
				if (System.currentTimeMillis() / 1_000 % CLEAN_INTERVAL == 0) { // 2分钟运行一次检测
					kcache_cleaner.accept(kcache); // 缓存清理
				}
			};
			println("%s:%s".formatted(key, kline));
			// 把kline数据写入TNAME标记的内存表(如KL_RB2605),表内主键为TS;
			igniteDB.put(kline.add(TNAME, "%s_%s".formatted(PREFIX_KL, iid)), TNAME, callback, "TS");

			return null;
		};

		// 连接进入交易消息队列进行tickdata的处理
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, tickdata_handler) //
				.sleepInterval(100).initialize().start(); // 没间隔100毫秒批量拉去一次数据

		Thread.sleep(1_000_000);
	}

	@Test
	public void dropTables() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final var dfm = igniteDB.sqldframe("SELECT TABLE_NAME name from SYSTEM.TABLES");
		final var patterns = REC( //
				"tk", "^%s_[A-Z]+\\d{3,}([A-Z]+\\d{4,})?".formatted(PREFIX_TK), // TICKDATA
				"kl", "^%s_.*".formatted(PREFIX_KL), // KLINE线
				"tbl", "^TBL_.*" // TICKDATA
		);
		final var pk = "kl";
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
		final var tk_symbols = dfm.filterBy(rec -> rec.str("name").matches("TK_[A-Z]+\\d{4}([A-Z]+\\d{4,})?"));
		tk_symbols.rowS().forEach(e -> {
			final var tbl = e.str(0);
			final var sql = "SELECT ID, UPDATETIME, LASTPRICE, VOLUME FROM %s ORDER BY ID limit 10".formatted(tbl);
			println("%s:\n%s\n".formatted(tbl, igniteDB.sqldframe(sql)));
		});

		final var kl_symbols = dfm.filterBy(rec -> rec.str("name").matches("KL_[A-Z]+\\d{4}([A-Z]+\\d{4,})?"));
		kl_symbols.rowS().forEach(e -> {
			final var tbl = e.str(0);
			final var sql = "SELECT * FROM %s ORDER BY TS limit 10".formatted(tbl);
			println("%s:\n%s\n".formatted(tbl, igniteDB.sqldframe(sql)));
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
	private static final String PREFIX_TK = "TK";
	private static final String PREFIX_KL = "KL";
	private static final String TNAME = "TBL";

}
