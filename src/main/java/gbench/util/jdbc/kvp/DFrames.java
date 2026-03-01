package gbench.util.jdbc.kvp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class DFrames {

	public static DFrame dfm(final ResultSet rs) {
		DFrame df = null;
		try {
			df = dfm(rs, true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return df;
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
