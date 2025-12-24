package gbench.webapps.myfuture.broker.model.market;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.ignite.catalog.*;
import org.apache.ignite.table.*;
import org.apache.ignite.catalog.definitions.*;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Table;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

import static gbench.util.io.Output.println;
import static org.apache.ignite.catalog.definitions.ColumnDefinition.column;

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
	public <T> T withTransaction(Function<IgniteClient, T> handler) {
		final var ar = new AtomicReference<T>();
		try (IgniteClient client = IgniteClient.builder().addresses(addresses).build()) {
			ar.set(handler.apply(client));
		}
		return ar.get();
	}

	/**
	 * 
	 * @param schema
	 * @param table
	 * @return
	 */
	public boolean tableExists(final String table) {
		return this.tableExists(this.defaultSchema, table);
	}

	public Table createTable(final String tbl, final IRecord proto) {
		return this.createTble(tbl, proto.toMap());
	}

	public Table createTble(final String tbl, final Map<?, ?> proto) {
		return this.withTransaction(client -> {
			return client.catalog().createTable(proto2tdb(proto, tbl).build());
		});
	}

	/**
	 * 
	 * @param schema
	 * @param table
	 * @return
	 */
	public boolean tableExists(final String schema, final String table) {
		return this.withTransaction(client -> {
			final var sql = "SELECT 1 FROM SYSTEM.TABLES WHERE SCHEMA_NAME = ? AND TABLE_NAME = ?";
			try (var rs = client.sql().execute(null, sql, schema.toUpperCase(), table.toUpperCase())) {
				return rs.hasNext(); // 有行就说明存在
			}
		});
	}

	public String getDefaultSchema() {
		return defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public static void dumpBuilder(TableDefinition.Builder b) {
		try {
			Field f = b.getClass().getDeclaredField("columns"); // private List<ColumnDefinition>
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<ColumnDefinition> cols = (List<ColumnDefinition>) f.get(b);

			System.out.println("Builder 当前列信息:");
			for (ColumnDefinition c : cols) {
				System.out.printf("  %s : %s%n", c.name(), c.type());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 把样本 Map 转成 Ignite 3.x 可用的 DDL 字符串
	 * 
	 * @param proto 一条样本数据，key=列名，value=任意对象
	 * @param tbl   表名，为空时自动生成
	 */
	public static <X, Y> TableDefinition.Builder proto2tdb(final Map<X, Y> proto, String tbl) {
		String tableName = (tbl == null || tbl.isBlank()) ? "tbl_" + Math.abs(proto.hashCode()) : tbl;

		final var builder = TableDefinition.builder(tableName);

		/* 1. 先推断所有列类型 */
		Map<String, ColumnType> colTypes = new LinkedHashMap<>();
		proto.forEach((k, v) -> {
			String col = String.valueOf(k);
			colTypes.put(col, inferColumnType(v));
		});

		/* 2. 主键推断规则：列名 id 且是 INT32 → 主键 */
		String pk = colTypes.entrySet().stream()
				.filter(e -> "ID".equalsIgnoreCase(e.getKey()) && e.getValue() == ColumnType.INT32)
				.map(Map.Entry::getKey).findFirst().orElse(colTypes.keySet().iterator().next()); // 否则取第一列

		final var cols = colTypes.entrySet().stream().map(e -> column(e.getKey(), e.getValue()))
				.toArray(ColumnDefinition[]::new);

		builder.columns(cols);
		/* 4. 设置主键 */
		builder.primaryKey(pk);

		println("proto(%s):%s".formatted(proto.size(), proto));
		dumpBuilder(builder);

		return builder;

	}

	/**
	 * 把样本 Map 转成 Ignite 3.x 可用的 DDL 字符串
	 * 
	 * @param sample 一条样本数据，key=列名，value=任意对象
	 * @param tbl    表名，为空时自动生成
	 */
	public static String proto2ddl(final Map<String, Object> sample, String tbl) {
		String tableName = (tbl == null || tbl.isBlank()) ? "tbl_" + Math.abs(sample.hashCode()) : tbl;

		List<String> cols = new ArrayList<>();
		sample.forEach((col, val) -> {
			String type = inferSqlType(val);
			// 与 R 函数一样：列名 id 且是整数 → 主键自增
			if ("id".equalsIgnoreCase(col) && "INTEGER".equals(type)) {
				cols.add(col + " INTEGER PRIMARY KEY AUTO_INCREMENT");
			} else {
				cols.add(col + " " + type);
			}
		});

		return "CREATE TABLE " + tableName + " (\n  " + String.join(",\n  ", cols) + "\n);";
	}

	/** 根据 Java 类型推断 SQL 类型字符串 */
	private static String inferSqlType(Object v) {
		if (v == null)
			return "VARCHAR(255)";

		Class<?> c = v.getClass();

		if (c == Integer.class || c == int.class)
			return "INTEGER";
		if (c == Long.class || c == long.class)
			return "BIGINT";
		if (c == Short.class || c == short.class)
			return "SMALLINT";
		if (c == Byte.class || c == byte.class)
			return "TINYINT";

		if (c == Double.class || c == double.class)
			return "DOUBLE";
		if (c == Float.class || c == float.class)
			return "REAL";

		if (c == Boolean.class || c == boolean.class)
			return "BOOLEAN";

		if (v instanceof LocalDate)
			return "DATE";
		if (v instanceof LocalDateTime)
			return "TIMESTAMP";
		if (v instanceof Instant || v instanceof ZonedDateTime || v instanceof OffsetDateTime)
			return "TIMESTAMP WITH TIME ZONE";

		if (v instanceof byte[])
			return "BLOB";

		// 集合/数组 → JSON 字符串
		if (v instanceof Collection || c.isArray())
			return "VARCHAR(4096)";

		// 字符串：按样本长度*1.5 取整，与 R 函数一致
		int len = Math.max(255, v.toString().length() * 3 / 2);
		return "VARCHAR(" + len + ")";
	}

	/** 根据 Java 对象推断 Ignite ColumnType */
	private static ColumnType inferColumnType(Object v) {
		if (v == null)
			return ColumnType.varchar(256); // 默认 VARCHAR

		Class<?> c = v.getClass();

		// 整数
		if (c == Integer.class || c == int.class)
			return ColumnType.INT32;
		if (c == Long.class || c == long.class)
			return ColumnType.INT64;
		if (c == Short.class || c == short.class)
			return ColumnType.INT16;
		if (c == Byte.class || c == byte.class)
			return ColumnType.INT8;

		// 浮点
		if (c == Double.class || c == double.class)
			return ColumnType.DOUBLE;
		if (c == Float.class || c == float.class)
			return ColumnType.FLOAT;
		if (c == BigDecimal.class)
			return ColumnType.decimal(((BigDecimal) v).precision(), ((BigDecimal) v).scale());

		// 布尔
		if (c == Boolean.class || c == boolean.class)
			return ColumnType.BOOLEAN;

		// 时间
		if (v instanceof LocalDate)
			return ColumnType.DATE;
		if (v instanceof LocalTime)
			return ColumnType.TIME;
		if (v instanceof Instant || v instanceof ZonedDateTime || v instanceof OffsetDateTime)
			return ColumnType.TIMESTAMP;

		// 二进制
		if (v instanceof byte[])
			return ColumnType.varbinary(((byte[]) v).length);

		// UUID
		if (v instanceof UUID)
			return ColumnType.UUID;

		// 字符串（含集合/数组转 JSON）
		if (v instanceof CharSequence)
			return ColumnType.varchar(Math.max(255, v.toString().length() * 3 / 2));

		// 集合/数组 → 变长字符串(JSON)
		if (v instanceof Collection || c.isArray())
			return ColumnType.varchar(4096);

		// 兜底
		return ColumnType.varchar(256);
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

	private String defaultSchema = "PUBLIC";
	private final String[] addresses;
}
