package gbench.sandbox.data;

import static gbench.util.data.MyDataApp.ds;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.util.data.MyDataApp;

public class DataAppTest {

	@Test
	public void foo() {
		this.timeit(() -> {
			for (int i = 0; i < MAX_TIMES; i++) {
				final var j = i;
				new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
					final var dfm = sess.sql2x("show tables");
					println(j, dfm.size());
				});
			}
			return "foo";
		});
	}

	@Test
	public void bar() {
		this.timeit(() -> {
			for (int i = 0; i < MAX_TIMES; i++) {
				println(i, new MyDataApp(ds(mysql_rec)).sqldframe("show tables").size());
			}
			return "bar";
		});
	}

	@Test
	public void quz() throws InterruptedException {
		this.timeit(() -> {
			new MyDataApp(ds(mysql_rec)).withTransaction(sess -> {
				for (int i = 0; i < MAX_TIMES; i++) {
					final var j = i;
					final var dfm = sess.sql2x("show tables");
					println(j, dfm.size());
				}
			});
			println("quz end!");
			return "quz";
		});

	}

	/**
	 * 记录时函数
	 * 
	 * @param cons
	 */
	public void timeit(final Supplier<String> cons) {
		final var startime = LocalDateTime.now();
		System.out.println(String.format("startime:%s", startime));
		final String fname = cons.get();
		final var endtime = LocalDateTime.now();
		System.out.println(String.format("endtime:%s", endtime));
		final var du = Duration.between(startime, endtime);
		System.out.println(String.format("%s last for: %s\n", fname, du));
	}

	/**
	 * 行数据输出
	 * 
	 * @param objects 输出的
	 */
	public String println(final Object... objects) {
		if (null != objects) {
			final String line = Arrays.asList(objects).stream().map(e -> e + "").collect(Collectors.joining("\t"));
			if (debug) {
				System.out.println(line);
			}
			return line;
		} else {
			System.out.println();
			return null;
		}
	}

	/**
	 * 
	 */
	final IRecord mysql_rec = IRecord.REC( //
			"url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", //
			"driver", "com.mysql.cj.jdbc.Driver", //
			"user", "root", "password", "123456");

	final int MAX_TIMES = 1000;
	final boolean debug = true;

}
