package gbench.webapps.myfuture.broker.model.market;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.SqlRow;
import gbench.util.jdbc.kvp.DFrame;

import static gbench.util.io.Output.println;

/**
 * 
 */
public class CtpIgniteData {

	/**
	 * 
	 * @param addresses
	 */
	public CtpIgniteData(final String... addresses) {
		this.addresses = addresses;
	}

	/**
	 * 
	 */
	public static void initialized() {
		// do nothing
	}

	/**
	 * 
	 * @param handler
	 * @return
	 */
	public CtpIgniteData withTransaction(Function<IgniteClient, Object> handler) {
		try (IgniteClient client = IgniteClient.builder().addresses(addresses).build()) {
			handler.apply(client);
		}
		return this;
	}

	// SqlRow 转 Map 核心方法（极简版）
	public static Map<String, Object> asMap(SqlRow row) {
		final var map = new LinkedHashMap<String, Object>();
		if (null != row) {
			println(row);
			// 遍历所有列名，填充键值对
			for (int i = 0; i < row.columnCount(); i++) {
				String colName = row.columnName(i);
				map.put(colName, row.value(i));
			}
		}
		return map;
	}

	public static <T> Stream<T> rs2S(org.apache.ignite.sql.ResultSet<T> rs) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rs, 0), false);
	}

	public static <T> DFrame rs2df(org.apache.ignite.sql.ResultSet<T> rs) {
		return rs2S(rs).map(e -> asMap((SqlRow) e)).collect(DFrame.dfmclc2);
	}

	private final String[] addresses;
}
