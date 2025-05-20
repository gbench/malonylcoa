package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import gbench.util.data.MyDataApp;
import gbench.util.io.Output;
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
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				dataClient.sqldframe(SECURITY_SQL)
						.map(dfm -> dfm.colOpt(0).map(List::stream).orElse(Stream.empty()).map(IRecord.obj2int()))
						.flatMapMany(Flux::fromStream)
						.flatMap(securityid -> dataClient.sqldframe(IRecord.FT(UNMATCHED_ORDER_SQL, securityid)))
						.subscribe(ordfrm -> {
							final var securityid = ordfrm.headOpt().map(e -> e.get("SECURITY_ID")).orElse("-");
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
		if (longs.nrows() < 1 || shorts.nrows() < 1) {
			return;
		}

		final var rb = IRecord.rb(MATCHORDER_KEYS);
		final var securityid = longs.head().get("SECURITY_ID");

		for (final var lo : longs) { // 多头
			final var lo_id = lo.str("ID");
			final var lo_price = lo.i4("PRICE");
			final var lo_quantiy = lo.i4("QUANTITY");

			for (final var so : shorts) { // 空头
				final var so_id = so.str("ID");
				final var so_price = so.i4("PRICE");
				final var so_quantity = so.i4("QUANTITY");

				if (lo_price < so_price) { // 买价低于卖价：无法进行撮合
					break;
				} else { // 买价大于等于卖价
					final var now = LocalDateTime.now();
					final var price = Math.min(lo_price, so_price); // 撮合价格
					final var quantity = Math.min(lo_quantiy, so_quantity); // 撮合数量
					final var datarec = rb.get(lo_id, so_id, securityid, price, quantity, now, now, "撮合交易单"); // 撮合单数据
					final var insql = MyDataApp.insert_sql("t_match_order", datarec.toMap()); // 数据插入语句

					dataClient.sqlexecute(insql).subscribe(Output::println);
				} // if
			} // for so
		} // for lo
	}

	@Autowired
	DataApiClient dataClient;

	final static String MATCHORDER_KEYS = "LONG_ORDER_ID,SHORT_ORDER_ID,SECURITY_ID,PRICE,QUANTITY,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
	final static String SECURITY_SQL = "select distinct SECURITY_ID from t_order";
	final static String UNMATCHED_ORDER_SQL = """
				select * from t_order where SECURITY_ID=$0 and ID not in (
					(select SHORT_ORDER_ID from t_match_order where SECURITY_ID=$0) union
					(select LONG_ORDER_ID from t_match_order where SECURITY_ID=$0)
				)
			""".strip();
}
