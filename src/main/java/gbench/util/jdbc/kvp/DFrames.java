package gbench.util.jdbc.kvp;

import java.nio.MappedByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.array.SharedMem;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.json.MyJson;

public class DFrames {

	public static <T> T maxof_textlen(final List<T> xs) {
		return maxof(xs, e -> String.valueOf(e).length());
	}

	public static <T> T maxof(final List<T> xs, final Function<T, Integer> k) {
		return xs.stream().collect(Collectors.maxBy(Comparator.comparing(k))).orElse(null);
	}

	/**
	 * 把 ResultSet 转换成 MappedByteBuffer
	 */
	public static Function<String, ExceptionalFunction<ResultSet, MappedByteBuffer>> shmfn = //
			path -> rs -> shm(path, rs);

	public static MappedByteBuffer shm(final String path, final ResultSet rs) throws Exception {
		final var dfm = dfm(rs);
		final var slots = SharedMem.Schema.slots(dfm);
		final var buffer = SharedMem.Schema.rafbuf(path, SharedMem.Schema.sizeof(slots));
		SharedMem.write(buffer, dfm);
		return buffer;
	}

	@SuppressWarnings("unchecked")
	public static DFrame dfm(final String json) {
		DFrame dfm = null;
		try {
			final List<Object> es = MyJson.recM().readValue(json, List.class);
			dfm = es.stream().map(IRecord::REC).collect(DFrame.dfmclc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dfm;
	}

	public static DFrame dfm(final ResultSet rs) throws SQLException {
		return dfm(rs, true);
	}

	public static DFrame dfm(final ResultSet rs, final boolean close) throws SQLException {
		return dfm(rs, close);
	}

	@SuppressWarnings("unused")
	public static DFrame dfm(final ResultSet rs, final Integer max, final boolean close) throws SQLException {
		final var kvps = new LinkedHashMap<Integer, List<Object>>();
		final var rsm = rs.getMetaData();
		final var ncol = rsm.getColumnCount();
		final var MAX = Optional.of(max).orElse(Integer.MAX_VALUE);
		final Function<Integer, String> keyOf = i -> {
			String key = null;
			try {
				key = rsm.getColumnLabel(i + 1);
			} catch (Exception e) {
			}
			return key;
		};

		var j = 0;
		while (rs.next()) {
			if (j++ >= MAX) {
				break;
			} else {
				for (int i = 0; i < ncol; i++) {
					kvps.computeIfAbsent(i, k -> new ArrayList<Object>()).add(rs.getObject(i + 1));
				}
			}
		}

		if (close) {
			rs.close();
		}

		final var ps = kvps.entrySet().stream().flatMap(e -> //
		(Stream<Object>) Stream.of(keyOf.apply(e.getKey() + 1), e.getValue())).toArray();

		return DFrame.dfm(ps);
	}

}
