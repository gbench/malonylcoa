package gbench.webapps.world.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.time.LocalDateTime.now;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.IRecord;
import reactor.core.publisher.Mono;

//@CrossOrigin // 通过gateway 来设置,这里就可以省略了。
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
	public Mono<IRecord> sqlquery(final String sql, final ServerWebExchange exchange) {
		final var ret = IRecord.REC("code", 0);
		if (sql == null) {
			
			System.out.println(exchange.getRequest().getMethod());
//			final var formData = exchange.getFormData();
//			formData.subscribe(e -> {
//				System.err.println(e);
//				System.err.println("attrs:--->" + exchange.getAttributes());
//			});
			exchange.getRequest().getBody().subscribe(buffer->{
				final var bb = new byte[buffer.readableByteCount()];
				buffer.read(bb);
				DataBufferUtils.release(buffer);
				try {
					System.err.println("api:"+new String(bb,"utf8"));
				}catch(Exception e) {
					
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
