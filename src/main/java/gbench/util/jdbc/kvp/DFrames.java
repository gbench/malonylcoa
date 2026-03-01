package gbench.util.jdbc.kvp;

import java.nio.MappedByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	public static ExceptionalFunction<Connection, ExceptionalFunction<String, DFrame>> sqldframeGen = conn -> sql -> {
		final var stmt = conn.createStatement();
		final var rs = stmt.executeQuery(sql);
		final var dfm = dfm(rs);
		stmt.close();
		return dfm;
	};

	/**
	 * 把 ResultSet 转换成 MappedByteBuffer
	 */
	public static ExceptionalFunction<String, ExceptionalFunction<ResultSet, MappedByteBuffer>> rs2shmGen = //
			path -> rs -> shm(path, rs);
	/**
	 * 把 ResultSet 转换成 MappedByteBuffer
	 */
	public static ExceptionalFunction<String, ExceptionalFunction<DFrame, MappedByteBuffer>> df2shmGen = //
			path -> dfm -> shm(path, dfm);

	/**
	 * 
	 * @param path
	 * @param dfm
	 * @return
	 * @throws Exception
	 */
	public static MappedByteBuffer shm(final String path, final DFrame dfm) throws Exception {
		final var slots = SharedMem.Schema.slots(dfm);
		final var buffer = SharedMem.Schema.rafbuf(path, SharedMem.Schema.sizeof(slots));
		SharedMem.write(buffer, dfm);
		return buffer;
	}

	/**
	 * 
	 * @param path
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public static MappedByteBuffer shm(final String path, final ResultSet rs) throws Exception {
		return shm(path, dfm(rs));
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
		return dfm(rs, null, close);
	}

	@SuppressWarnings("unused")
	public static DFrame dfm(final ResultSet rs, final Integer max, final boolean close) throws SQLException {
		final var kvps = new LinkedHashMap<Integer, List<Object>>();
		final var rsm = rs.getMetaData();
		final var ncol = rsm.getColumnCount();
		final var MAX = Optional.ofNullable(max).orElse(Integer.MAX_VALUE);
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

		final var ps = kvps.entrySet().stream().flatMap(p -> //
		Tuple2.of(keyOf.apply(p.getKey() + 1), p.getValue()).stream()).toArray();

		if (close) {
			rs.close();
		}

		return DFrame.dfm(ps);
	}

}
