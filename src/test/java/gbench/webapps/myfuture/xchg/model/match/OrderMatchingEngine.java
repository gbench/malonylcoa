package gbench.webapps.myfuture.xchg.model.match;

import static gbench.util.io.Output.println;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;

/**
 * 
 */
public class OrderMatchingEngine extends AbstractMatchModel implements IMatchModel {
	private final ConcurrentHashMap<Integer, Object> securityLocks = new ConcurrentHashMap<>();
	private Disruptor<OrderEvent> disruptor;
	private RingBuffer<OrderEvent> ringBuffer;

	public OrderMatchingEngine(final Integer interval, DataApiClient dataClient, int bufferSize) {
		super(interval, dataClient);

		// Initialize Disruptor
		this.disruptor = new Disruptor<>(OrderEvent::new, bufferSize, Executors.defaultThreadFactory(),
				ProducerType.MULTI, // Multiple producers
				new BlockingWaitStrategy() // Balance between performance and CPU usage
		);

		// Set up event handlers
		disruptor.handleEventsWith(new OrderEventHandler());

		// Start Disruptor
		this.ringBuffer = disruptor.start();
	}

	/**
	 * Publish order data to Disruptor ring buffer
	 */
	public void handleOrders(final DFrame orderFrame) {
		if (orderFrame.nrows() < 1)
			return;

		long sequence = ringBuffer.next();
		try {
			OrderEvent event = ringBuffer.get(sequence);
			event.setOrderFrame(orderFrame);
		} finally {
			ringBuffer.publish(sequence);
		}
	}

	/**
	 * Order event class for Disruptor
	 */
	public static class OrderEvent {
		private DFrame orderFrame;

		public void setOrderFrame(DFrame orderFrame) {
			this.orderFrame = orderFrame;
		}

		public DFrame getOrderFrame() {
			return orderFrame;
		}
	}

	/**
	 * Order event handler
	 */
	public class OrderEventHandler implements EventHandler<OrderEvent> {
		@Override
		public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
			DFrame orderFrame = event.getOrderFrame();
			final var securityid = orderFrame.headOpt().map(e -> e.i4("SECURITY_ID")).orElse(-1);

			try {
				// Still synchronize by security ID to prevent concurrent modifications
				synchronized (securityLocks.computeIfAbsent(securityid, k -> new Object())) {
					println("-------------------------------------------");
					println("-- securityid:%s".formatted(securityid));
					println("-- time:%s".formatted(LocalTime.now()));

					final var groups = orderFrame.groupBy(e -> e.i4("POSITION"));
					final var longs = DFrame.of(groups.getOrDefault(LONG_POSITION, EMPTY))
							.sorted(IRecord.cmp("PRICE,CREATE_TIME", false, true));
					final var shorts = DFrame.of(groups.getOrDefault(SHORT_POSITION, EMPTY))
							.sorted(IRecord.cmp("PRICE,CREATE_TIME", true, true));
					println("-- LONGS(%d):%s".formatted(longs.nrows(), longs));
					println("-- SHORTS(%d):%s".formatted(shorts.nrows(), shorts));

					if (longs.nrows() > 0 && shorts.nrows() > 0) {
						matchOrders(longs, shorts);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				println("-- ERROR match for securityid:%s".formatted(securityid));
			}
		}
	}

	public void shutdown() {
		disruptor.shutdown();
	}

	@Override
	public void destroy() {
		this.shutdown();

	}
}