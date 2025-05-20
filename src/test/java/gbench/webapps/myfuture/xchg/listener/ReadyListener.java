package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;
import static java.util.Arrays.asList;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
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
		final var ln = longs.nrows(); // 多单数量
		final var sn = shorts.nrows(); // 空单数量
		if (ln < 1 || sn < 1) {
			return;
		}

		final var dirties = new HashSet<Integer>(); // 修改国的订单
		final var rb = IRecord.rb(MATCHORDER_KEYS);
		final var securityid = longs.head().get("SECURITY_ID");
		var i = 0; // so 空单 遍历索引
		var flag = false; // 终止订单匹配：已经把匹配的订单都匹配了

		for (final var lo : longs) { // lo:当前多单
			if (flag) {
				break;
			}

			final var lo_id = lo.str("ID");
			final var lo_price = lo.i4("PRICE");

			while (i < sn) { // 空头
				final var so = shorts.row(i); // 当前空单
				final var so_id = so.str("ID");
				final var so_price = so.i4("PRICE");
				final var so_quantity = so.i4("UNMATCHED"); // 空头数量
				final var lo_quantity = lo.i4("UNMATCHED"); // 多头数量

				if (lo_quantity < 1 || so_quantity < 1) {
					if (so_quantity < 1) // 空头数量已经匹配完成
						i++; // 使用下一个空单
					else // 多单已经配置完成
						break; // 继续下一个多单
				} else {
					if (lo_price < so_price) { // 买价低于卖价：无法进行撮合
						flag = true; // 当前多单价格（最高价）小于当前空单价格（最低价），后面的就没有必要基础处理了，标记退出标志
						break;
					} else { // 买价大于等于卖价
						final var now = LocalDateTime.now();
						final var price = Math.min(lo_price, so_price); // 撮合价格
						final var quantity = Math.min(lo_quantity, so_quantity); // 撮合数量
						final var datarec = rb.get(lo_id, so_id, securityid, price, quantity, now, now, "撮合交易单"); // 撮合单数据
						final var insql = MyDataApp.insert_sql("t_match_order", datarec.toMap()); // 数据插入语句

						dataClient.sqlexecute(insql).subscribe(Output::println); // 写入数据库

						// 更新数量
						final var so_left = so_quantity - quantity;
						final var lo_left = lo_quantity - quantity;

						for (final var rec : asList(so.set("UNMATCHED", so_left), lo.set("UNMATCHED", lo_left))) { // 更新未匹配数量
							dirties.add(rec.i4("ID")); // 标记已经修改过的订单ID
						} // for UNMATCHED
					} // if lo_price < so_price
				} // if lo_quantity
			} // for so 空单
		} // for lo 多单

		// 更新 DIRTY_ORDER
		Stream.concat(longs.rowS(), shorts.rowS()).filter(rec -> dirties.contains(rec.get("ID")))
				.map(rec -> DIRTY_ORDER_SQL.formatted(rec.get("UNMATCHED"), rec.get("ID"))).map(dataClient::sqlexecute)
				.forEach(mono -> mono.subscribe(Output::println));

	}

	@Autowired
	private DataApiClient dataClient;

	final static String MATCHORDER_KEYS = "LONG_ORDER_ID,SHORT_ORDER_ID,SECURITY_ID,PRICE,QUANTITY,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
	final static String SECURITY_SQL = "select distinct SECURITY_ID from t_order";
	final static String UNMATCHED_ORDER_SQL = "select * from t_order where SECURITY_ID=$0 and UNMATCHED!=0";
	final static String DIRTY_ORDER_SQL = "update t_order set UNMATCHED=%s where ID=%s";
}
