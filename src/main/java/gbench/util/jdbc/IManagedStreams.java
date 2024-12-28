package gbench.util.jdbc;

import java.util.Set;
import java.util.stream.Stream;

/**
 * 托管的流
 * 
 * @author gbench
 *
 */
public interface IManagedStreams {

	/**
	 * 获取托管数据流的集合,返回 活动流的 引用。
	 * 
	 * @return 托管数据流的集合
	 */
	Set<Stream<?>> getActiveStreams();

	/**
	 * 把所有的活动的流都给予清空
	 */
	default void clear() {
		this.getActiveStreams().forEach(Stream::close); // 关闭活动流
		this.getActiveStreams().clear();// 活动流的库存清空
	}

	/**
	 * 把所有的活动的流都给予清空 清空指定的 流对象
	 * 
	 * @param stream 指定的流对象
	 * @return jdbc 本身 便于 实现链式编程
	 */
	default IManagedStreams clear(final Stream<?> stream) {
		if (this.getActiveStreams().contains(stream)) {
			this.getActiveStreams().remove(stream);
		}
		stream.close();
		return this;
	}

	/**
	 * 托管一个流对象
	 * 
	 * @param stream 托管的流对象
	 * @return jdbc 本身 便于 实现链式编程
	 */
	default IManagedStreams add(final Stream<?> stream) {
		this.getActiveStreams().add(stream);
		return this;
	}

	/**
	 * 把所有的活动的流都给予清空
	 */
	default void dump() {
		this.getActiveStreams().forEach(e -> {
			System.out.println(e);
		});
	}
}