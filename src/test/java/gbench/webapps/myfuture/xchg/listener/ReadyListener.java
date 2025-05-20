package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;
import reactor.core.publisher.Flux;

@Component
public class ReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		final var thread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(5000); // 休息5秒钟
				} catch (Exception e) {
					e.printStackTrace();
				}

				dataClient.sqldframe(SECURITY_SQL)
						.map(dfm -> dfm.colOpt(0).map(List::stream).orElse(Stream.empty()).map(IRecord.obj2int()))
						.flatMapMany(Flux::fromStream)
						.flatMap(securityid -> dataClient.sqldframe(IRecord.FT(UNMATCHED_ORDER_SQL, securityid)))
						.subscribe(ordfrm -> {
							final var securityid = ordfrm.headOpt().map(e -> e.get("SECURITY_ID")).orElse("-"); // 获取证券ID
							println("-------------------------------------------");
							println("-- securityid:%s".formatted(securityid));
							final var groups = ordfrm.groupBy(e -> e.i4("POSITION"));
							final var longs = DFrame.of(groups.getOrDefault(1, Arrays.asList()))
									.sorted(IRecord.cmp("PRICE,CREATE_TIME", false, true)); // 价格倒序，时间正序列
							final var shorts = DFrame.of(groups.getOrDefault(-1, Arrays.asList()))
									.sorted(IRecord.cmp("PRICE,CREATE_TIME", true, true)); // 价格正，时间正序列
							println("-- LONGS:%s".formatted(longs));
							println("-- SHORTS:%s".formatted(shorts));
							this.matchOrders(longs, shorts); // 撮合订单
						});
			} // while
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 撮合订单
	 * 
	 * @param longs  多头头寸
	 * @param shorts 空头头寸
	 */
	public void matchOrders(final DFrame longs, final DFrame shorts) {
		final var dirties = new HashSet<Integer>();
		final var sn = shorts.nrows();
		int i = 0; // 空单索引
		boolean shouldTerminate = false;

		for (final var lo : longs) {
			if (shouldTerminate)
				break;

			int lo_quantity = lo.i4("UNMATCHED");
			if (lo_quantity <= 0)
				continue;

			while (i < sn && lo_quantity > 0) {
				final var so = shorts.row(i);
				final int so_quantity = so.i4("UNMATCHED");
				if (so_quantity <= 0) {
					i++;
					continue;
				}

				if (lo.i4("PRICE") < so.i4("PRICE")) {
					shouldTerminate = true; // 后续多单价格更低，无需处理
					break;
				}

				final int quantity = Math.min(lo_quantity, so_quantity);
				createMatchRecord(lo, so, quantity);
				updateQuantities(lo, so, quantity, dirties);
				lo_quantity -= quantity;
			} // while
		} // for

		batchUpdateDirtyOrders(dirties, longs, shorts);
	}

	/**
	 * 
	 * @param lo
	 * @param so
	 * @param qty
	 */
	private void createMatchRecord(final IRecord lo, final IRecord so, final int qty) {
		final var rec = IRecord.rb(MATCHORDER_KEYS).get(lo.str("ID"), so.str("ID"), lo.get("SECURITY_ID"),
				Math.min(lo.i4("PRICE"), so.i4("PRICE")), qty, LocalDateTime.now(), LocalDateTime.now(), "撮合交易单");
		dataClient.sqlexecute(MyDataApp.insert_sql("t_match_order", rec.toMap()))
				.subscribe(r -> println("Match record inserted:\n%s".formatted(r)));
	}

	/**
	 * 
	 * @param lo
	 * @param so
	 * @param qty
	 * @param dirties
	 */
	private void updateQuantities(final IRecord lo, final IRecord so, final int qty, final Set<Integer> dirties) {
		lo.set("UNMATCHED", lo.i4("UNMATCHED") - qty);
		so.set("UNMATCHED", so.i4("UNMATCHED") - qty);
		dirties.add(lo.i4("ID"));
		dirties.add(so.i4("ID"));
	}

	/**
	 * 
	 * @param dirties
	 * @param frames
	 */
	private void batchUpdateDirtyOrders(final Set<Integer> dirties, final DFrame... frames) {
		if (dirties.isEmpty())
			return;

		final var buffer = new StringBuffer();
		buffer.append("UPDATE t_order SET UNMATCHED = CASE ID ");
		final var updates = Stream.of(frames).flatMap(DFrame::rowS).filter(r -> dirties.contains(r.i4("ID")))
				.collect(Collectors.toMap(r -> r.i4("ID"), r -> r.i4("UNMATCHED")));
		final var ids = updates.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
		updates.forEach((id, qty) -> buffer.append(" WHEN %s THEN %s ".formatted(id, qty)));
		buffer.append("END WHERE ID IN ( %s )".formatted(ids));
		final var sql = buffer.toString();

		dataClient.sqlexecute(sql).subscribe(r -> println("Updated %s orders:\n%s".formatted(updates.size(), r)));
	}

	@Autowired
	private DataApiClient dataClient;

	final static String MATCHORDER_KEYS = "LONG_ORDER_ID,SHORT_ORDER_ID,SECURITY_ID,PRICE,QUANTITY,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
	final static String SECURITY_SQL = "select distinct SECURITY_ID from t_order";
	final static String UNMATCHED_ORDER_SQL = "select * from t_order where SECURITY_ID=$0 and UNMATCHED!=0";
	final static String DIRTY_ORDER_SQL = "update t_order set UNMATCHED=%s where ID=%s";
}
