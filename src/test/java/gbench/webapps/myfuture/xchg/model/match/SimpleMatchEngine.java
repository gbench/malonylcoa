package gbench.webapps.myfuture.xchg.model.match;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gbench.util.lisp.DFrame;

import gbench.webapps.myfuture.xchg.msclient.DataApiClient;

/**
 * 
 */
public class SimpleMatchEngine extends AbstractMatchModel {

	/**
	 * 
	 * @param interval
	 * @param dataClient
	 */
	public SimpleMatchEngine(final Integer interval, final DataApiClient dataClient) {
		super(interval, dataClient);
	}

	/**
	 * 订单处理
	 * 
	 * @param orders
	 */
	@Override
	public void handleOrders(final DFrame orders) {
		if (orders.nrows() < 1)
			return;

		es.execute(() -> {
			final var securityid = orders.headOpt().map(e -> e.i4("SECURITY_ID")).orElse(-1); // 获取证券ID

			// 使用ConcurrentHashMap管理证券ID对应的锁，确保同一个证券的撮合任务串行执行，避免并发冲突
			synchronized (securityLocks.computeIfAbsent(securityid, k -> new Object())) {
				this.process(orders);
			} // synchronized

		}); // 撮合订单
	}

	public void shutdown() {
		es.shutdownNow();
	}

	@Override
	public void destroy() {
		this.shutdown();
	}

	private final ConcurrentHashMap<Integer, Object> securityLocks = new ConcurrentHashMap<>();
	private ExecutorService es = Executors.newFixedThreadPool(10);
}
