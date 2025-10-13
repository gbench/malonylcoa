package gbench.webapps.myfuture.xchg.model.match;

import static gbench.util.io.Output.println;

import java.util.concurrent.Executors;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import gbench.util.lisp.DFrame;
import gbench.webapps.myfuture.xchg.msclient.DataApiClient;

/**
 * High-performance order matching engine using LMAX Disruptor
 */
public class OrderMatchEngine extends AbstractMatchModel implements IMatchModel {
	private final Disruptor<OrderEvent> disruptor;
	private final RingBuffer<OrderEvent> ringBuffer;
	private final int numPartitions;
	private final EventHandler<OrderEvent>[] handlers;

	/**
	 * 
	 * @param interval
	 * @param dataClient
	 */
	public OrderMatchEngine(final Integer interval, final DataApiClient dataClient) {
		this(interval, dataClient, 1024 * 4, 10);
	}

	@SuppressWarnings("unchecked")
	public OrderMatchEngine(final Integer interval, final DataApiClient dataClient, final int bufferSize,
			final int numPartitions) {
		super(interval, dataClient);
		this.numPartitions = numPartitions;

		// Initialize Disruptor with multi-producer support
		this.disruptor = new Disruptor<>(OrderEvent::new, bufferSize, Executors.defaultThreadFactory(),
				ProducerType.MULTI, new BlockingWaitStrategy());

		// Create partition-based handlers
		this.handlers = new EventHandler[numPartitions];
		for (int i = 0; i < numPartitions; i++) {
			handlers[i] = new OrderEventHandler();
		}

		// Set up event handlers with partitioning
		disruptor.handleEventsWith(handlers);

		// Start Disruptor
		this.ringBuffer = disruptor.start();
	}

	/**
	 * Publish order data to Disruptor ring buffer with partition-based routing
	 */
	@Override
	public void handleOrders(final DFrame orders) {
		if (orders.nrows() < 1) {
			return;
		}

		final int securityId = orders.headOpt().map(e -> e.i4("SECURITY_ID")).orElse(-1);

		// Determine partition based on security ID
		final int partition = Math.abs(securityId) % numPartitions;

		long sequence = ringBuffer.next();
		try {
			OrderEvent event = ringBuffer.get(sequence);
			event.setOrders(orders);
			event.setPartition(partition);
		} finally {
			ringBuffer.publish(sequence);
		}
	}

	/**
	 * Order event class for Disruptor
	 */
	public static class OrderEvent {
		private DFrame orders;
		private int partition;

		public void setOrders(DFrame orders) {
			this.orders = orders;
		}

		public DFrame getOrders() {
			return orders;
		}

		public void setPartition(int partition) {
			this.partition = partition;
		}

		public int getPartition() {
			return partition;
		}
	}

	/**
	 * Order event handler - each instance handles a specific partition
	 */
	public class OrderEventHandler implements EventHandler<OrderEvent> {
		@Override
		public void onEvent(final OrderEvent event, final long sequence, final boolean endOfBatch) {
			final DFrame orders = event.getOrders();
			println("** sequence:%s".formatted(sequence));
			println("** endOfBatch:%s".formatted(endOfBatch));
			OrderMatchEngine.this.process(orders);
		}
	}

	/**
	 * 
	 */
	public void shutdown() {
		disruptor.shutdown();
	}

	@Override
	public void destroy() {
		shutdown();
	}
}