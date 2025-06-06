package gbench.webapps.myfuture.xchg.model.match;

import static gbench.util.io.Output.println;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.data.MyDataApp;
import gbench.util.io.Output;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.util.lisp.Tuple2;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 */
public abstract class AbstractMatchModel implements IMatchModel {

	/**
	 * 
	 * @param interval
	 * @param dataClient
	 */
	public AbstractMatchModel(final Integer interval, final DataApiClient dataClient) {
		this.interval = interval;
		this.dataClient = dataClient;
	}

	/**
	 * Poll and match orders using Disruptor
	 */
	public void handle() {
		this.pollOrders().subscribe(this::handleOrders);
	}

	/**
	 * 
	 */
	@Override
	public void start() {
		new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(interval); // 等待前端下达足够的交易单
					this.handle(); // 拉取交易订单并进行撮合
				} catch (InterruptedException e) {
					e.printStackTrace();
					println("撮合线程中断");
					this.handle(); // 重新拉取交易订单并进行撮合
				} catch (Exception e) {
					e.printStackTrace();
					println("撮合线程异常");
				} // try
			} // while
		}, "order-matcher").start();
	}

	/**
	 * 拉取交易订单并进行撮合
	 * 
	 * @return
	 */
	public Flux<DFrame> pollOrders() {
		return dataClient.sqldframe(SECURITY_SQL)
				.map(dfm -> dfm.colOpt(0).map(List::stream).orElse(Stream.empty()).map(IRecord.obj2int()))
				.flatMapMany(Flux::fromStream)
				.flatMap(securityid -> dataClient.sqldframe(IRecord.FT(UNMATCHED_ORDER_SQL, securityid)));
	}

	/**
	 * 
	 * @param orders
	 * @return
	 */
	public Tuple2<DFrame, DFrame> split(final DFrame orders) {
		final var groups = orders.groupBy(e -> e.i4("POSITION")); // 依据订单的头寸（多头:1,空头:-1)
		final var longs = DFrame.of(groups.getOrDefault(LONG_POSITION, EMPTY))
				.sorted(IRecord.cmp("PRICE,CREATE_TIME", false, true)); // 价格倒序，时间正序列
		final var shorts = DFrame.of(groups.getOrDefault(SHORT_POSITION, EMPTY))
				.sorted(IRecord.cmp("PRICE,CREATE_TIME", true, true)); // 价格正，时间正序列
		println("-- LONGS(%d):%s".formatted(longs.nrows(), longs));
		println("-- SHORTS(%d):%s".formatted(shorts.nrows(), shorts));
		return Tuple2.of(longs, shorts);
	}

	/**
	 * 
	 * @param orders
	 */
	public void process(final DFrame orders) {
		final var securityid = orders.headOpt().map(e -> e.i4("SECURITY_ID")).orElse(-1);

		try {
			println("-------------------------------------------");
			println("-- securityid:%s".formatted(securityid));
			println("-- time:%s".formatted(LocalTime.now()));

			final var ps = this.split(orders);

			if (ps._1.nrows() > 0 && ps._2.nrows() > 0) {
				matchOrders(ps._1, ps._2);
			}
		} catch (Exception e) {
			e.printStackTrace();
			println("-- ERROR match for securityid:%s".formatted(securityid));
		}
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

		// 批量插入&更新
		batchInsertMatchOrders(matches).map(r -> println("Match %s records inserted:\n%s".formatted(r.nrows(), r)))
				.flatMap(e -> batchUpdateDirtyOrders(dirties, longs, shorts))
				.subscribe(r -> println("Updated %s orders:\n%s".formatted(r.nrows(), r)));
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
	 * 批量插入
	 * 
	 * @param matches 撮合单
	 */
	private Mono<DFrame> batchInsertMatchOrders(final List<IRecord> matches) {
		if (matches.isEmpty())
			return Mono.empty();

		final var recs = matches.stream().map(IRecord::toMap).map(MyDataApp.IRecord::REC).toList();

		return Optional.ofNullable(MyDataApp.insert_sql("t_match_order", recs)).map(Output::println)
				.map(dataClient::sqlexecute).orElse(Mono.empty());
	}

	/**
	 * 批量更新
	 * 
	 * @param dirties 有变动的订单
	 * @param frames  订单队列
	 */
	private Mono<DFrame> batchUpdateDirtyOrders(final Set<Integer> dirties, final DFrame... frames) {
		if (dirties.isEmpty())
			return Mono.empty();

		final var updates = Stream.of(frames).flatMap(DFrame::rowS).filter(r -> dirties.contains(r.i4("ID")))
				.collect(Collectors.toMap(r -> r.i4("ID"), r -> r.i4("UNMATCHED")));
		final var ids = updates.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
		final var ps = updates.entrySet().stream().map(e -> "WHEN %s THEN %s ".formatted(e.getKey(), e.getValue()))
				.collect(Collectors.joining(" "));
		final var now = LocalDateTime.now();
		final var sql = "UPDATE t_order SET UNMATCHED = CASE ID %s END, REVISION = REVISION + 1, UPDATE_TIME = '%s' WHERE ID IN ( %s )"
				.formatted(ps, now, ids);

		return Optional.ofNullable(sql).map(Output::println).map(dataClient::sqlexecute).orElse(Mono.empty());
	}

	final static Integer LONG_POSITION = 1;
	final static Integer SHORT_POSITION = -1;
	final static List<IRecord> EMPTY = Arrays.asList();
	final static String MATCHORDER_KEYS = "LONG_ORDER_ID,SHORT_ORDER_ID,SECURITY_ID,PRICE,QUANTITY,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
	final static String SECURITY_SQL = "select distinct SECURITY_ID from t_order";
	final static String UNMATCHED_ORDER_SQL = "select * from t_order where SECURITY_ID=$0 and UNMATCHED!=0";

	protected final Integer interval;
	protected final DataApiClient dataClient;
}
