package gbench.webapps.world.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.time.LocalDateTime.now;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.IRecord;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api")
public class ApiController {

	/**
	 * http://localhost:8010/world/api/hello
	 * 
	 * @return
	 */
	@RequestMapping("hello")
	public IRecord hello() {
		return REC("code", "0", "message", "hello", "time", now());
	}

	/**
	 * sql语句查询
	 * 
	 * @param sql SQL 语句
	 * @return IRecord
	 */
	@RequestMapping("sqlquery")
	public Mono<IRecord> sqlquery(final String sql, final ServerHttpRequest req) {
		final var ret = IRecord.REC("code", 0);
		if (sql == null) {
			req.getBody().subscribe(buffer -> {
				final var bb = new byte[buffer.readableByteCount()];
				buffer.read(bb);
				DataBufferUtils.release(buffer);
				try {
					final var body = new String(bb, "utf8");
					System.err.println(body);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			ret.add("data", this.dataApp.sqldframe("select * from t_maozedong limit 1"));
		} else {
			ret.add("data", this.dataApp.sqldframe(sql));
		}

		return Mono.fromSupplier(() -> ret);
	}

	@Autowired
	private MyDataApp dataApp;

}
