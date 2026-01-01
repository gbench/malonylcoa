package gbench.webapps.myfuture.broker.model.market;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ignite.table.*;
import org.apache.ignite.client.IgniteClient;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

/**
 * 深度市场数据模型
 */
public class DeepMarketDataModel {

	/**
	 * 
	 * @param ctp_topic
	 * @param kafka_bootstrap_servers
	 * @param kafa_consumer_group_id
	 * @param ignite_address
	 * @param prefix_tk
	 * @param prefix_kl
	 * @param tname
	 */
	public DeepMarketDataModel(final String ctp_topic, final String kafka_bootstrap_servers,
			final String kafa_consumer_group_id, final String kafka_auto_offset_reset_config,
			final String ignite_address, final String prefix_tk, final String prefix_kl, final String tname) {
		this.CTP_TOPIC = ctp_topic;
		this.KAFKA_BOOTSTRAP_SERVERS = kafka_bootstrap_servers;
		this.KAFKA_CONSUMER_GROUP_ID = kafa_consumer_group_id;
		this.KAFKA_AUTO_OFFSET_RESET_CONFIG = kafka_auto_offset_reset_config;
		this.IGNITE_ADDRESS = ignite_address;
		this.PREFIX_KL = prefix_kl;
		this.TNAME = tname;
		this.stopflag = new AtomicBoolean(false);
		this.es = Executors.newFixedThreadPool(1);
		this.ignite_client = IgniteClient.builder().addresses(IGNITE_ADDRESS).build();
	}

	@Override
	public String toString() {
		return "DeepMarketDataModel [CTP_TOPIC=" + CTP_TOPIC + ", KAFKA_BOOTSTRAP_SERVERS=" + KAFKA_BOOTSTRAP_SERVERS
				+ ", KAFKA_CONSUMER_GROUP_ID=" + KAFKA_CONSUMER_GROUP_ID + ", KAFKA_AUTO_OFFSET_RESET_CONFIG="
				+ KAFKA_AUTO_OFFSET_RESET_CONFIG + ", IGNITE_ADDRESS=" + IGNITE_ADDRESS + ", PREFIX_KL=" + PREFIX_KL
				+ ", TNAME=" + TNAME + "]";
	}

	/**
	 * 1分钟K线
	 * 
	 * @param tsgen
	 * @throws Exception
	 */
	public void kline1m() {
		this.kline(zdt -> zdt.format(dtf_ymdhm));
	}

	/**
	 * 动态K线生成
	 * 
	 * @param tsgen
	 */
	public void kline(final Function<ZonedDateTime, String> tsgen) {
		final var kcache = new ConcurrentHashMap<String, IRecord>(); // 本地计算kline的缓存cache:key为{instrument}_{yyyyMMddHHmm}
		final var dtf = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
		final var krb = IRecord.rb("TS,OPEN,HIGH,LOW,CLOSE,VOLUME,VOL0,VOL1,IDX,TIMES,UPTIME"); // K线数据格式, 累计成交量
		final var dtf_ymdhmsS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		final var shzd = ZoneId.of("Asia/Shanghai");
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
			final var ts = tsgen.apply(zdt); // K线的主键(合约表的
			final var uptime = zdt.format(dtf_ymdhmsS); // 更新时间
			final var idx = tick.lng("ID"); // 消息在队列内的偏移位置（代表消费进度）
			final var key_ts = "%s_%s".formatted(iid, ts); // K线的分钟K归集主键
			final var px = tick.dbl("LastPrice"); // 成交价格
			final var vol = tick.i4("Volume"); // tick投递的Volume是当日的累计成交量:vol0,起点量vol1终点量,volume:期间流量
			final var value = krb.get(ts, px, px, px, px, 0, vol, vol, idx, 1, uptime); // 成交量初始为0
			kcache.merge(key_ts, value, (o, _) -> // 依据合约时间分组key进行K线聚合
			o.add(REC("HIGH", Math.max(o.dbl("HIGH"), px), "LOW", Math.min(o.dbl("LOW"), px), "CLOSE", px, "VOLUME",
					vol - o.i4("VOL0"), "VOL1", vol, "IDX", idx, "TIMES", o.i4("TIMES") + 1, "UPTIME", uptime))); // 根据key进行K线聚合
			final var kline = kcache.get(key_ts); // 提取
			final var tname = "%s_%s".formatted(PREFIX_KL, iid); // 表名

			queue.offer(kline.add(TNAME, tname)); // 写入队列消息
			println("%s:%s".formatted(key_ts, kline));

			return kline;
		};

		// 连接进入交易消息队列进行tickdata的处理
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, KAFKA_AUTO_OFFSET_RESET_CONFIG,
				tickdata_handler).sleepInterval(-1).initialize().start(); // 没间隔100毫秒批量拉去一次数据
		kline_writer.setName("IGNITE-KLINE-WRITER"); // IGNITE-KLINE-WRITER
		kline_writer.start(); // 启动igniteKLineWriter

	}

	/**
	 * 
	 */
	public void stop() {
		// 退出处理
		stopflag.set(true);
		sleep(1000); // 等待1mS让igniteWriter自动关闭(超时强制关闭）
		ignite_client.close();
		es.shutdown();
		es.close();
	}

	/**
	 * 
	 * @param ms
	 */
	public void sleep(final long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// 配置参数（对应你给出的参数）
	private final String CTP_TOPIC;
	private final String KAFKA_BOOTSTRAP_SERVERS;
	private final String KAFKA_CONSUMER_GROUP_ID;
	private final String KAFKA_AUTO_OFFSET_RESET_CONFIG;
	private final String IGNITE_ADDRESS;
	private final String PREFIX_KL;
	private final String TNAME;
	private final ExecutorService es;
	private final AtomicBoolean stopflag;
	private final IgniteClient ignite_client;
	private final DateTimeFormatter dtf_ymdhm = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

}
