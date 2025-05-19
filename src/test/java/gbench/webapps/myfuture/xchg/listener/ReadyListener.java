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
							this.matchOrders(longs, shorts);
						});
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 
	 * @param longs
	 * @param shorts
	 */
	public void matchOrders(final DFrame longs, final DFrame shorts) {
		final var rb = IRecord.rb(MATCHORDER_KEYS);
		if (longs.nrows() < 1 || shorts.nrows() < 1) {
			return;
		}
		final var securityid = longs.head().get("SECURITY_ID");

		for (final var lo : longs) {
			final var loid = lo.str("ID");
			final var lo_price = lo.i4("PRICE");
			final var lo_quantiy = lo.i4("QUANTITY");
			for (final var so : shorts) {
				final var soid = so.str("ID");
				final var so_price = so.i4("PRICE");
				final var so_quantity = so.i4("QUANTITY");
				final var now = LocalDateTime.now();

				if (lo_price < so_price) { // 买价低于卖价
					break;
				} else { // 买价大于等于卖价
					final var price = Math.min(lo_price, so_price);
					final var quantity = Math.min(lo_quantiy, so_quantity);
					final var datarec = rb.get(loid, soid, securityid, price, quantity, now, now, "撮合交易单");
					final var insql = MyDataApp.insert_sql("t_match_order", datarec.toMap());

					dataClient.sqlexecute(insql).subscribe(e -> {
						println(insql);
						println(e);
					});
				} // if
			}
		}
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
