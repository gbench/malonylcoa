package gbench.webapps.myfuture.broker.model.market.junit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ignite.table.*;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.client.IgniteClient;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.myfuture.broker.model.market.CtpIgniteDB;
import gbench.webapps.myfuture.broker.model.market.CtpTickDataMQ;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.webapps.myfuture.broker.model.market.CtpIgniteDB.*;
import static org.apache.ignite.catalog.definitions.ColumnDefinition.column;

public class DMDMTest {

	@Test
	public void foo() throws InterruptedException {
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, "latest", //
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
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, "latest", tickdata_handler)
				.initialize().start();
		Thread.sleep(1000000); // 等待
	}

	/**
	 * 动态K线生成
	 * 
	 * @throws Exception
	 */
	@Test
	public void quz_kline1m_final() throws Exception {
		final Map<String, IRecord> kcache = new ConcurrentHashMap<String, IRecord>(); // 本地计算kline的缓存cache:key为{instrument}_{yyyyMMddHHmm}
		final var dtf = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
		final var krb = IRecord.rb("TS,OPEN,HIGH,LOW,CLOSE,VOLUME,VOL0,VOL1,IDX,TIMES,UPTIME"); // K线数据格式, 累计成交量
		final var dtf_ymdhm = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
		final var dtf_ymdhmsS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		final var shzd = ZoneId.of("Asia/Shanghai");
		final var es = Executors.newFixedThreadPool(1); // 清理工作线程池, 只使用一个清洁工！
		final var uptm = new AtomicReference<>(LocalDateTime.now()); // 上次更新时间按 update time ar
		final var kcache_gardener = ((BiFunction<Integer, Integer, Consumer<Map<String, IRecord>>>) // K线缓存清理器
		(expired, maxsize) -> cache -> { // cache结构为<symbol_yyyymmddhhmm,entries>的ConcurrentHashMap结构,
			if (cache.size() < maxsize && Duration.between(uptm.get(), LocalDateTime.now()).getSeconds() < expired) {
				final var dfm = cache.entrySet().stream().map(e -> e.getValue()).collect(DFrame.dfmclc);
				println("KCACHE DUMP\n:%s\n".formatted(dfm));
				return;
			}
			final var currents = cache.keySet(); // 提取所有合约K线时间戳
			final var latests = currents.stream().collect(Collectors.groupingBy(k -> k.split("_")[0])).entrySet()
					.stream().map(e -> e.getValue().stream().collect(Collectors.maxBy(String::compareTo)))
					.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet()); // 获取最新时间戳
			final var outdates = currents.stream().filter(k -> !latests.contains(k)).collect(Collectors.toSet()); // 陈旧时间戳
			currents.removeAll(outdates); // 批量删除陈旧时间戳
			uptm.set(LocalDateTime.now()); // 更新上次处理时间
		}).apply(1 * 60, 20);
		final Queue<IRecord> queue = new LinkedBlockingQueue<IRecord>();
		final var stopflag = new AtomicBoolean(false); // igniteKLineWriter 是否需要停止运行！false:运行,true:停止！
		final Function<String, Function<LocalDateTime, Consumer<Tuple>>> cbgen = key -> st -> e -> {
			final var n = kcache.size();
			final var ed = LocalDateTime.now();
			final var duration = Duration.between(st, ed).toMillis();
			println("UPDATE %s@[st:%s, ed:%s: du:%d]#%s === %s".formatted(key, st, ed, duration, n, e));
			if (n % 100 == 0) // 缓存数量超过限度通知kcache_cleaner打扫房间
				es.execute(() -> kcache_gardener.accept(kcache));
		}; // cbgen
		final var ignite_client = IgniteClient.builder().addresses(IGNITE_ADDRESS).build(); // ignite客户端
		final var kline_writer = new Thread(() -> { // igniteKLineWriter读写器具
			while (!stopflag.get()) {
				final var kline = queue.poll(); // 读取K线信息
				if (kline == null)
					continue;
				final var key = kline.str(TNAME).substring(PREFIX_KL.length() + 1); // 剔除表名前缀获取合约编码
				// 把kline数据写入TNAME标记的内存表(如KL_RB2605),表内主键为TS;
				CtpIgniteDB.put_s(ignite_client, kline, TNAME, cbgen.apply(key).apply(LocalDateTime.now()), "TS"); // 写入实时K线到Ignite
			} // while
		}); // igniteKLineWriter
		final Function<IRecord, Object> tickdata_handler = tick -> {
			final var iid = tick.str("InstrumentID");
			final var zdt0 = LocalDateTime.parse("%s %s".formatted(tick.str("ActionDay"), tick.str("UpdateTime")), dtf)
					.plusNanos(tick.i4("UpdateMillisec") * 1_000_000).atZone(shzd);
			final var epoch = tick.lngopt("Epoch").orElseGet(() -> zdt0.toInstant().toEpochMilli());
			final var zdt = Instant.ofEpochMilli(epoch).atZone(shzd);
			final var kymdhm = zdt.format(dtf_ymdhm); // K线的主键(合约表的
			final var uptime = zdt.format(dtf_ymdhmsS); // 更新时间
			final var idx = tick.lng("ID"); // 消息在队列内的偏移位置（代表消费进度）
			final var key = "%s_%s".formatted(iid, kymdhm); // K线的分钟K归集主键
			final var px = tick.dbl("LastPrice"); // 成交价格
			final var vol = tick.i4("Volume"); // tick投递的Volume是当日的累计成交量:vol0,起点量vol1终点量,volume:期间流量
			final var value = krb.get(kymdhm, px, px, px, px, 0, vol, vol, idx, 1, uptime); // 成交量初始为0
			kcache.merge(key, value, (o, _) -> // 依据合约时间分组key进行K线聚合
			o.add(REC("HIGH", Math.max(o.dbl("HIGH"), px), "LOW", Math.min(o.dbl("LOW"), px), "CLOSE", px, "VOLUME",
					vol - o.i4("VOL0"), "VOL1", vol, "IDX", idx, "TIMES", o.i4("TIMES") + 1, "UPTIME", uptime))); // 根据key进行K线聚合
			final var kline = kcache.get(key); // 提取
			final var tname = "%s_%s".formatted(PREFIX_KL, iid); // 表名

			queue.offer(kline.add(TNAME, tname)); // 写入队列消息
			println("%s:%s".formatted(key, kline));

			return kline;
		};

		// 连接进入交易消息队列进行tickdata的处理
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, "latest", tickdata_handler) //
				.sleepInterval(-1).initialize().start(); // 没间隔100毫秒批量拉去一次数据
		kline_writer.setName("IGNITE-KLINE-WRITER"); // IGNITE-KLINE-WRITER
		kline_writer.start(); // 启动igniteKLineWriter

		Thread.sleep(1_000_000_000);

		// 退出处理
		stopflag.set(true);
		Thread.sleep(1000); // 等待1mS让igniteWriter自动关闭(超时强制关闭）
		ignite_client.close();
		es.shutdown();
		es.close();
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
	private static final String CTP_TOPIC = "my-ctp-topic";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
	private static final String KAFKA_CONSUMER_GROUP_ID = "my-ctp-topic_group_ignite-3.10-1";
	private static final String IGNITE_ADDRESS = "localhost:10800";
	private static final String PREFIX_TK = "TK";
	private static final String PREFIX_KL = "KL";
	private static final String TNAME = "TBL";

}
