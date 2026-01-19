package gbench.sandbox.jdbc;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gbench.global.Globals;
import gbench.util.jdbc.IJdbcSession;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.DFrame;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

/**
 * H2数据库示例
 */
public class JdbcIgniteTest {

	@FunctionalInterface
	public interface SupplierEx<T> {
		T get() throws Exception;
	}

	public static <T> T uncheck(final SupplierEx<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 外部启动并初始化(cluster init --name ctp) 数据库（然后增肌数据mtcars)
	 */
	@Test
	public void foo() {
		final var sqlfile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
		final var db = "ctp"; // 数据库名
		final var url = String.format("jdbc:ignite:thin://192.168.1.41:10800", db); // h2连接字符串
		final var ignite_rec = REC("url", url, "driver", "org.apache.ignite.jdbc.IgniteJdbcDriver", "user", "root",
				"password", "123456"); // h2数据库
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, ignite_rec); // 数据库应用客户端
		final ExceptionalFunction<IJdbcSession<UUID, Object>, ExceptionalFunction<String, DFrame>> fngen = sess -> sql -> {
			final var conn = sess.getConnection();
			final var ar = new AtomicReference<DFrame>();
			try (final var stmt = conn.createStatement(); final var rs = stmt.executeQuery(sql)) {
				final var rsm = rs.getMetaData();
				final var n = rsm.getColumnCount();
				final var rb = IRecord.rb(IntStream.rangeClosed(1, n)
						.mapToObj(i -> uncheck(() -> rsm.getColumnLabel(i))).collect(Collectors.joining(",")));
				final var lines = new ArrayList<IRecord>();
				while (rs.next()) {
					final var line = IntStream.range(1, n).mapToObj(i -> uncheck(() -> rs.getObject(i))).toArray();
					lines.add(rb.get(line));
				}
				final var dfm = DFrame.of(lines);
				ar.set(dfm);
			}
			return ar.get();
		};

		jdbcApp.withTransaction(sess -> {
			final var sqldframe = fngen.apply(sess);
			final Function<String, LocalDateTime> toldt = s -> LocalDateTime.parse(s,
					DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
			println("KL_RB2605", sqldframe.apply("select * from KL_RB2605 limit 10"));
			println("KL_MA605", sqldframe.apply("select * from KL_MA605 limit 10").rowS().map(e -> e.set("TS", toldt))
					.collect(DFrame.dfmclc));
			final var ts = sqldframe.apply("select * from KL_IF2603 limit 10").col("TS",
					(List<String> xs) -> xs.stream().map(toldt).toList());
			println("TS", ts);
		});

	}

}
