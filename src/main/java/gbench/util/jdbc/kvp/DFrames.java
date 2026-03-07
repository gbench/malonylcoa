package gbench.util.jdbc.kvp;

import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.array.SharedMem;
import gbench.util.array.SharedMem.DataType;
import gbench.util.array.SharedMem.Schema.ChanBuff;
import gbench.util.jdbc.IJdbcSession;
import gbench.util.jdbc.function.ExceptionalConsumer;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.json.MyJson;

/**
 * DFrame 的 工具函数
 */
public class DFrames {

	/**
	 * 
	 */
	public static interface STR2INT_EFN extends ExceptionalFunction<String, Integer> {

	}

	/**
	 * 
	 */
	public static interface STR2BOOL_EFN extends ExceptionalFunction<String, Boolean> {

	}

	/**
	 * 
	 */
	public static interface STR2DFM_EFN extends ExceptionalFunction<String, DFrame> {

	}

	/**
	 * 
	 */
	public static interface DFM2DFM_EFN extends ExceptionalFunction<DFrame, DFrame> {

	}

	/**
	 * 
	 */
	public static interface REC2DFM_EFN extends ExceptionalFunction<IRecord, DFrame> {

	}

	/**
	 * 
	 */
	public static interface DFM2REC_FN extends ExceptionalFunction<DFrame, IRecord> {

	}

	/**
	 * 
	 */
	public static interface STR_ECS extends ExceptionalConsumer<IRecord> {

	}

	/**
	 * 
	 */
	public static interface REC_ECS extends ExceptionalConsumer<IRecord> {

	}

	/**
	 * 
	 */
	public static interface DFM_ECS extends ExceptionalConsumer<DFrame> {

	}

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

	// 直接写入 Buffer（无中间对象）
	private static Function<MappedByteBuffer, BiFunction<DataType, Object, MappedByteBuffer>> mbb_writer = buf -> (type,
			value) -> {

		if (value == null) { // 写入零值
			for (int i = 0; i < type.elementSize; i++)
				buf.put((byte) 0);
			return buf;
		} else {
			switch (type) {
			case INT32 -> buf.putInt(((Number) value).intValue());
			case INT64 -> buf.putLong(((Number) value).longValue());
			case FLOAT64 -> buf.putDouble(((Number) value).doubleValue());
			case FLOAT32 -> buf.putFloat(((Number) value).floatValue());
			case DATETIME -> {
				final var ldt = (LocalDateTime) value;
				buf.putLong(ldt.toEpochSecond(ZoneOffset.UTC));
				buf.putInt(ldt.getNano());
			}
			case DATE -> buf.putLong(((java.time.LocalDate) value).toEpochDay());
			case STRING16, STRING32, STRING64, STRING128, STRING256, STRING512, STRING1024, STRING2048 -> {
				final var str = value.toString();
				final var bytes = str.getBytes(StandardCharsets.UTF_16LE);
				final var fixed = Arrays.copyOf(bytes, type.elementSize);
				buf.put(fixed);
			}
			default -> throw new IllegalArgumentException("Unsupported type: " + type);
			}
			return buf;
		} // if
	};

	/**
	 * sql2chanbufGen
	 */
	public static ExceptionalFunction<Connection, ExceptionalFunction<String, ExceptionalFunction<String, ChanBuff>>> sql2chanbufGen = conn -> sql -> shmfile -> {

		// 1. 获取列元数据
		final var metasql = "SELECT * FROM (%s) t WHERE 1=0".formatted(sql);
		final List<IRecord> columns;
		try (final var stmt = conn.createStatement(); final var meta_rs = stmt.executeQuery(metasql)) {
			final var rsm = meta_rs.getMetaData();
			final ExceptionalFunction<Integer, IRecord> idx2rec = i -> IRecord.REC("name", rsm.getColumnLabel(i + 1),
					"sqltype", rsm.getColumnType(i + 1), "precision", rsm.getPrecision(i + 1));
			columns = Stream.iterate(0, i -> i + 1).limit(rsm.getColumnCount()).map(idx2rec.noexcept()).toList();
		}

		// 2. 获取统计信息（行数 + 字符串最大长度）
		final var stats_sql = statssql_of(sql, columns);
		final IRecord stats;
		try (final var stmt = conn.createStatement(); final var stats_rs = stmt.executeQuery(stats_sql)) {
			stats_rs.next();
			stats = IRecord.REC("nrows", stats_rs.getInt(1));
			for (int i = 0; i < columns.size(); i++) {
				final var col = columns.get(i);
				if (is_string(col.i4("sqltype"))) {
					stats.add("max_" + col.str("name"), stats_rs.getInt(i + 2));
				}
			}
		}

		final int nrows = stats.i4("nrows");

		// 3. 构建 Schema 并计算精确大小
		final var slots = new ArrayList<IRecord>();
		final var rb = IRecord.rb("path,name,type,start,end,length,count");
		final List<DataType> types = new ArrayList<>();
		var datasize = 0;

		for (final var col : columns) {
			final var name = col.str("name");
			final var sqltype = col.i4("sqltype");
			final var maxlen = stats.i4opt("max_" + name).orElse(0);
			final var type = resolve_type(sqltype, maxlen);
			final var colsize = type.elementSize * nrows;

			types.add(type);
			slots.add(rb.get("root.%s.%s".formatted(type.name(), name), name, type.name(), datasize, datasize + colsize,
					type.elementSize, nrows));
			datasize += colsize;
		}

		// 4. 精确分配 ChanBuff
		final var metajson = Json.obj2json(Map.of("slots", slots.stream() //
				.map(s -> Map.of("x", s.str("name"), "t", s.str("type"), "n", s.i4("count"), "s", s.i4("start")))
				.toList()));
		final var metabytes = metajson.getBytes(StandardCharsets.UTF_8);
		final var totalsize = 4 + metabytes.length + datasize;
		final var chanbuff = SharedMem.Schema.rafbuf(shmfile, totalsize);
		final var buf = chanbuff.buff;

		// 5. 写入元数据头
		buf.putInt(metabytes.length);
		buf.put(metabytes);

		// 6. 执行正式查询 → 直接写入 Buffer
		try (final var stmt = conn.createStatement(); final var rs = stmt.executeQuery(sql)) {
			stmt.setFetchSize(nrows); // 避免一次性加载所有结果到内存
			final var ncol = columns.size();

			// 按列维护写入位置
			final var offsets = slots.stream().mapToInt(s -> 4 + metabytes.length + s.i4("start")).toArray();
			final var writer = mbb_writer.apply(buf);
			while (rs.next()) {
				for (int i = 0; i < ncol; i++) {
					buf.position(offsets[i]);
					writer.apply(types.get(i), rs.getObject(i + 1));
					offsets[i] += types.get(i).elementSize;
				}
			}

			// buf.force(); // 7. 强制刷新并关闭
		}

		return chanbuff;
	};

	// 辅助方法
	private static String statssql_of(final String sql, final List<IRecord> columns) {
		final var aggs = columns.stream().filter(c -> is_string(c.i4("sqltype")))
				.map(c -> "MAX(LENGTH(%s))".formatted(c.str("name"))).collect(Collectors.joining(", "));
		return aggs.isEmpty() ? "SELECT COUNT(*) FROM (%s) t".formatted(sql)
				: "SELECT COUNT(*), %s FROM (%s) t".formatted(aggs, sql);
	}

	/**
	 * 
	 * @param sqltype
	 * @return
	 */
	private static boolean is_string(final int sqltype) {
		return sqltype == Types.VARCHAR || sqltype == Types.CHAR || sqltype == Types.LONGVARCHAR;
	}

	/**
	 * 
	 * @param sqltype
	 * @param maxlen
	 * @return
	 */
	private static DataType resolve_type(final int sqltype, final int maxlen) {
		final Function<Integer, DataType> choose_strtype = n -> {
			if (n <= 8)
				return DataType.STRING16;
			if (n <= 16)
				return DataType.STRING32;
			if (n <= 32)
				return DataType.STRING64;
			if (n <= 64)
				return DataType.STRING128;
			if (n <= 128)
				return DataType.STRING256;
			if (n <= 256)
				return DataType.STRING512;
			if (n <= 512)
				return DataType.STRING1024;
			return DataType.STRING2048;
		};

		return switch (sqltype) {
		case Types.INTEGER -> DataType.INT32;
		case Types.BIGINT -> DataType.INT64;
		case Types.DOUBLE, Types.FLOAT -> DataType.FLOAT64;
		case Types.DECIMAL, Types.NUMERIC -> DataType.FLOAT64;
		case Types.TIMESTAMP -> DataType.DATETIME;
		case Types.DATE -> DataType.DATE;
		case Types.VARCHAR, Types.CHAR -> choose_strtype.apply(maxlen);
		default -> DataType.STRING256;
		};
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
	 * sqlexecute 生成函数
	 */
	public static ExceptionalFunction<Connection, ExceptionalFunction<String, Boolean>> sqlexecuteGen = conn -> sql -> {
		return conn.createStatement().execute(sql);
	};

	/**
	 * sqlexecute 生成函数
	 */
	public static ExceptionalFunction<IJdbcSession<?, ?>, ExceptionalFunction<String, Boolean>> sqlexecuteGen2 = sqlexecuteGen
			.compose((ExceptionalFunction<IJdbcSession<?, ?>, Connection>) IJdbcSession::getConnection);

	/**
	 * 
	 * @param <T>
	 * @param fun
	 * @return
	 */
	public static <T> ExceptionalFunction<Connection, T> sqlfunGen(final ExceptionalFunction<Connection, T> fun) {
		return con -> fun.apply(con);
	}

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
