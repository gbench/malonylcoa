package gbench.util.jdbc;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 托管流的数据的处理
 * 
 * @author gbench
 *
 */
public class ManagedStreams implements IManagedStreams {

	@Override
	public Set<Stream<?>> getActiveStreams() {
		return streams;
	}

	protected Set<Stream<?>> streams = new HashSet<>(); // 活动的流
}