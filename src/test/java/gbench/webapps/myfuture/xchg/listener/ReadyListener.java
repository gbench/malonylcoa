package gbench.webapps.myfuture.xchg.listener;

import static gbench.util.io.Output.println;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

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

				dataClient.sqldframe(SECURITY_SQL).map(dfm -> dfm.colS(0, IRecord.obj2int()))
						.flatMapMany(Flux::fromStream)
						.flatMap(securityid -> dataClient.sqldframe(IRecord.FT(UNMATCHED_ORDER_SQL, securityid)))
						.subscribe(ordfrm -> {
							final var securityid = ordfrm.headOpt().map(e -> e.get("SECURITY_ID")).orElse("-");
							println("-------------------------------------------");
							println("-- securityid:%s".formatted(securityid));
							final var groups = ordfrm.groupBy(e -> e.i4("POSITION"));
							final var lngdfm = DFrame.of(groups.getOrDefault(1, Arrays.asList()))
									.sorted(IRecord.cmp("PRICE,CREATE_TIME", false, true)); // 价格倒序，时间正序列
							final var shtdfm = DFrame.of(groups.getOrDefault(-1, Arrays.asList()))
									.sorted(IRecord.cmp("PRICE,CREATE_TIME", true, true)); // 价格正，时间正序列
							println("-- LONGS:%s".formatted(lngdfm));
							println("-- SHORTS:%s".formatted(shtdfm));
						});
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	@Autowired
	DataApiClient dataClient;

	final static String SECURITY_SQL = "select distinct SECURITY_ID from t_order";
	final static String UNMATCHED_ORDER_SQL = """
				select * from t_order where SECURITY_ID=$0 and ID not in (
					(select SHORT_ORDER_ID from t_match_order where SECURITY_ID=$0) union
					(select LONG_ORDER_ID from t_match_order where SECURITY_ID=$0)
				)
			""".strip();
}
