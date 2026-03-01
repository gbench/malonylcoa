package gbench.util.jdbc.kvp;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import gbench.util.array.SharedMem;
import gbench.util.array.SharedMem.Schema.ChanBuff;
import gbench.util.jdbc.IJdbcSession;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.json.MyJson;

/**
 * DFrame 的 工具函数
 */
public class DFrames {

	/**
	 * 
	 * @param <T>
	 * @param xs
	 * @return
	 */
	public static <T> T maxof_textlen(final List<T> xs) {
		return maxof(xs, e -> String.valueOf(e).length());
	}

	/**
	 * 
	 * @param <T>
	 * @param xs
	 * @param k
	 * @return
	 */
	public static <T> T maxof(final List<T> xs, final Function<T, Integer> k) {
		return xs.stream().collect(Collectors.maxBy(Comparator.comparing(k))).orElse(null);
	}

	/**
	 * sqldframe 生成函数
	 */
	public static ExceptionalFunction<Connection, ExceptionalFunction<String, DFrame>> sqldframeGen = conn -> sql -> {
		final var stmt = conn.createStatement();
		final var rs = stmt.executeQuery(sql);
		final var dfm = dfm(rs);
		stmt.close();
		return dfm;
	};

	/**
	 * sqldframe 生成函数
	 */
	public static ExceptionalFunction<IJdbcSession<?, ?>, ExceptionalFunction<String, DFrame>> sqldframeGen2 = sqldframeGen
			.compose((ExceptionalFunction<IJdbcSession<?, ?>, Connection>) IJdbcSession::getConnection);

	/**
	 * 把 ResultSet 转换成 MappedByteBuffer
	 */
	public static ExceptionalFunction<String, ExceptionalFunction<ResultSet, ChanBuff>> rs2shmGen = //
			path -> rs -> shm(path, rs);
	/**
	 * 把 ResultSet 转换成 MappedByteBuffer
	 */
	public static ExceptionalFunction<String, ExceptionalFunction<DFrame, ChanBuff>> df2shmGen = //
			path -> dfm -> shm(path, dfm);

	/**
	 * 创建共享内存文件
	 * 
	 * @param pathname 共享内存文件路径
	 * @param dfm      共享内存文件数据
	 * @return
	 * @throws Exception
	 */
	public static ChanBuff shm(final String pathname, final DFrame dfm) throws Exception {
		final var slots = SharedMem.Schema.slots(dfm);
		final var buffer = SharedMem.Schema.rafbuf(pathname, SharedMem.Schema.sizeof(slots));

		SharedMem.write(buffer, dfm);
		return buffer;
	}

	/**
	 * 创建共享内存文件
	 * 
	 * @param path 共享内存文件路径
	 * @param rs   共享内存文件数据
	 * @return
	 * @throws Exception
	 */
	public static ChanBuff shm(final String path, final ResultSet rs) throws Exception {
		return shm(path, dfm(rs));
	}

	/**
	 * 
	 * @param json
	 * @return
	 */
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

	/**
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static DFrame dfm(final ResultSet rs) throws SQLException {
		return dfm(rs, true);
	}

	/**
	 * 
	 * @param rs
	 * @param close
	 * @return
	 * @throws SQLException
	 */
	public static DFrame dfm(final ResultSet rs, final boolean close) throws SQLException {
		return dfm(rs, null, close);
	}

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	public static DFrame dfm(final ResultSet rs, final Integer max, final boolean close) throws SQLException {

		final var kvps = new LinkedHashMap<Integer, List<Object>>();
		final var rsm = rs.getMetaData();
		final var ncol = rsm.getColumnCount();
		final Function<Integer, String> keyOf = i -> {
			String key = null;
			try {
				key = rsm.getColumnLabel(i + 1);
			} catch (Exception e) {
			}
			return key;
		};
		final Predicate<Integer> break_pred = (null == max) ? j -> false : j -> j >= max; // max 为空时永远不主动退出！

		var j = 0;
		while (rs.next()) {
			if (break_pred.test(j++)) {
				break;
			} else {
				for (int i = 0; i < ncol; i++) {
					kvps.computeIfAbsent(i, k -> new ArrayList<Object>()).add(rs.getObject(i + 1));
				}
			}
		}

		final var ps = kvps.entrySet().stream().flatMap(p -> //
		Tuple2.of(keyOf.apply(p.getKey()), p.getValue()).stream()).toArray();

		if (close) {
			rs.close();
		}

		return DFrame.dfm(ps);
	}

}
