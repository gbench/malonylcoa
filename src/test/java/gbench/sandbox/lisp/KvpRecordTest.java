package gbench.sandbox.lisp;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import gbench.util.io.Output;
import gbench.util.jdbc.kvp.IRecord;

public class KvpRecordTest {

	/**
	 * IRecord.Builder 的 prepend 与 append 的 使用 <br>
	 * id:1 name:gbench age:34 creator:gbench
	 * create_time:2025-11-06T21:40:18.499439600 <br>
	 * id:1 name:kelinw age:23 creator:gbench
	 * create_time:2025-11-06T21:41:24.719690100
	 */
	@Test
	public void foo() {
		final var prefix = REC("id", 1);
		final var suffix = REC("creator", "gbench", "create_time", LocalDateTime.now());
		final var rb = IRecord.rb("name,age").prepend(prefix).append(suffix);
		println(rb.duplicate().get("gbench", 34));
		println(rb.duplicate().get("kelinw", 23));
	}

	/**
	 * IRecord.Builder的复制，与 带有前缀后缀构建器的生成！
	 */
	@Test
	public void bar() {
		final var prefix = REC("id", 1);
		final var suffix = REC("creator", "gbench", "create_time", LocalDateTime.now());
		final var rb = IRecord.rb("name,age");
		final var rb_ = rb.duplicate().append(suffix); // 带有后缀的构建暗器
		final var _rb = rb.duplicate().prepend(prefix); // 带有前缀的构建暗器
		final var _rb_ = rb.duplicate().prepend(prefix).append(suffix); // 带有前缀与后缀的构建暗器
		println(rb.get("gbench", 34));
		println(rb_.get("gbench", 34));
		println(_rb.get("gbench", 34));
		println(_rb_.get("gbench", 34));
	}

}
