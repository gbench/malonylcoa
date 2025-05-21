package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import gbench.util.data.MyDataApp;
import gbench.util.io.Output;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;
import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Flux;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(INTERVAL); // 等待前端下达足够的交易单
					pollAndMatchOrders(); // 拉取交易订单并进行撮合
				} catch (InterruptedException e) {
					e.printStackTrace();
					println("撮合线程中断");
					pollAndMatchOrders(); // 重新拉取交易订单并进行撮合
				} catch (Exception e) {
					e.printStackTrace();
					println("撮合线程异常");
				} // try
			} // while
		}, "order-matcher").start();
	}

	/**
	 * 拉取交易订单并进行撮合
	 */
	public void pollAndMatchOrders() {
		dataClient.sqldframe(SECURITY_SQL)
				.map(dfm -> dfm.colOpt(0).map(List::stream).orElse(Stream.empty()).map(IRecord.obj2int()))
				.flatMapMany(Flux::fromStream)
				.flatMap(securityid -> dataClient.sqldframe(IRecord.FT(UNMATCHED_ORDER_SQL, securityid)))
				.subscribe(this::handleOrders); // subscribe
	}

	/**
	 * 订单处理
	 * 
	 * @param ordfrm
	 */
	public void handleOrders(final DFrame ordfrm) {
		if (ordfrm.nrows() < 1)
			return;

		es.execute(() -> {
			final var securityid = ordfrm.headOpt().map(e -> e.i4("SECURITY_ID")).orElse(-1); // 获取证券ID
			try {
				// 使用ConcurrentHashMap管理证券ID对应的锁，确保同一个证券的撮合任务串行执行，避免并发冲突
				synchronized (securityLocks.computeIfAbsent(securityid, k -> new Object())) {
					println("-------------------------------------------");
					println("-- securityid:%s".formatted(securityid));
					println("-- time:%s".formatted(LocalTime.now()));

					final var groups = ordfrm.groupBy(e -> e.i4("POSITION")); // 依据订单的头寸（多头:1,空头:-1)
					final var longs = DFrame.of(groups.getOrDefault(LONG_POSITION, EMPTY))
							.sorted(IRecord.cmp("PRICE,CREATE_TIME", false, true)); // 价格倒序，时间正序列
					final var shorts = DFrame.of(groups.getOrDefault(SHORT_POSITION, EMPTY))
							.sorted(IRecord.cmp("PRICE,CREATE_TIME", true, true)); // 价格正，时间正序列
					println("-- LONGS(%d):%s".formatted(longs.nrows(), longs));
					println("-- SHORTS(%d):%s".formatted(shorts.nrows(), shorts));

					if (longs.nrows() > 0 && shorts.nrows() > 0) {
						this.matchOrders(longs, shorts); // 撮合订单
					} // if
				} // synchronized
			} catch (Exception e) {
				e.printStackTrace();
				println("-- ERROR match for securityid:%s".formatted(securityid));
			} // try
		}); // 撮合订单
	}

	/**
	 * 撮合订单
	 * 
	 * @param longs  多头头寸
	 * @param shorts 空头头寸
	 */
	public void matchOrders(final DFrame longs, final DFrame shorts) {
		final var dirties = new HashSet<Integer>(); // 变更单的ID集合
		final var matches = new ArrayList<IRecord>(); // 撮合单集合
		final var sn = shorts.nrows(); // 空单数量
		boolean shouldTerminate = false; // 是否需要提前终止
		int i = 0; // 空单索引

		for (final var lo : longs) {
			if (shouldTerminate)
				break;

			int lo_quantity = lo.i4("UNMATCHED");
			if (lo_quantity < 1)
				continue;

			while (i < sn && lo_quantity > 0) {
				final var so = shorts.row(i);
				final int so_quantity = so.i4("UNMATCHED");
				if (so_quantity < 1) {
					i++;
					continue;
				}

				if (lo.i4("PRICE") < so.i4("PRICE")) {
					shouldTerminate = true; // 后续多单价格更低，无需处理
					break;
				}

				final var quantity = Math.min(lo_quantity, so_quantity); // 成交数量
				final var price = Math.min(lo.i4("PRICE"), so.i4("PRICE")); // 成交价格

				matches.add(createMatchRecord(lo, so, price, quantity));
				updateQuantities(lo, so, quantity, dirties);
				lo_quantity -= quantity;
			} // while
		} // for

		batchInsertMatchOrders(matches);
		batchUpdateDirtyOrders(dirties, longs, shorts);
	}

	/**
	 * 创建撮合订单
	 * 
	 * @param lo  多头单
	 * @param so  空头单
	 * @param qty 成交数量
	 */
	private IRecord createMatchRecord(final IRecord lo, final IRecord so, final Number price, final int qty) {
		final var rec = IRecord.rb(MATCHORDER_KEYS).get(lo.str("ID"), so.str("ID"), lo.get("SECURITY_ID"), price, qty,
				LocalDateTime.now(), LocalDateTime.now(), "撮合交易单");
		return rec;
	}

	/**
	 * 
	 * @param lo      多头单
	 * @param so      空头单
	 * @param qty     成交数量
	 * @param dirties 变动单的ID集合
	 */
	private void updateQuantities(final IRecord lo, final IRecord so, final int qty, final Set<Integer> dirties) {
		lo.set("UNMATCHED", lo.i4("UNMATCHED") - qty);
		so.set("UNMATCHED", so.i4("UNMATCHED") - qty);
		dirties.add(lo.i4("ID"));
		dirties.add(so.i4("ID"));
	}

	/**
	 * 
	 * @param matches
	 */
	private void batchInsertMatchOrders(final List<IRecord> matches) {
		if (matches.isEmpty())
			return;

		final var recs = matches.stream().map(IRecord::toMap).map(MyDataApp.IRecord::REC).toList();

		Optional.ofNullable(MyDataApp.insert_sql("t_match_order", recs)).map(Output::println)
				.map(dataClient::sqlexecute).ifPresent(
						mono -> mono.subscribe(r -> println("Match %s records inserted:\n%s".formatted(r.nrows(), r))));
	}

	/**
	 * 
	 * @param dirties
	 * @param frames
	 */
	private void batchUpdateDirtyOrders(final Set<Integer> dirties, final DFrame... frames) {
		if (dirties.isEmpty())
			return;

		final var updates = Stream.of(frames).flatMap(DFrame::rowS).filter(r -> dirties.contains(r.i4("ID")))
				.collect(Collectors.toMap(r -> r.i4("ID"), r -> r.i4("UNMATCHED")));
		final var ids = updates.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
		final var ps = updates.entrySet().stream().map(e -> "WHEN %s THEN %s ".formatted(e.getKey(), e.getValue()))
				.collect(Collectors.joining(" "));
		final var sql = "UPDATE t_order SET UNMATCHED = CASE ID %s END WHERE ID IN ( %s )".formatted(ps, ids);

		Optional.ofNullable(sql).map(Output::println).map(dataClient::sqlexecute)
				.ifPresent(mono -> mono.subscribe(r -> println("Updated %s orders:\n%s".formatted(updates.size(), r))));
	}

	@PreDestroy
	public void shutdown() {
		es.shutdownNow();
	}

	@Value("${xchg.matchorder.interval:5000}")
	private Integer INTERVAL;
	@Autowired
	private DataApiClient dataClient;
	private final ConcurrentHashMap<Integer, Object> securityLocks = new ConcurrentHashMap<>();
	private ExecutorService es = Executors.newFixedThreadPool(10);

	final static List<IRecord> EMPTY = Arrays.asList();
	final static Integer LONG_POSITION = 1; // 多头头寸
	final static Integer SHORT_POSITION = -1; // 空头头寸
	final static String MATCHORDER_KEYS = "LONG_ORDER_ID,SHORT_ORDER_ID,SECURITY_ID,PRICE,QUANTITY,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
	final static String SECURITY_SQL = "select distinct SECURITY_ID from t_order";
	final static String UNMATCHED_ORDER_SQL = "select * from t_order where SECURITY_ID=$0 and UNMATCHED!=0";
	final static String DIRTY_ORDER_SQL = "update t_order set UNMATCHED=%s where ID=%s";
}
