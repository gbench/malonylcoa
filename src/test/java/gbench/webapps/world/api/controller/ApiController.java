package gbench.webapps.world.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.time.LocalDateTime.now;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.IRecord;
import gbench.webapps.world.api.config.Param;
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
	public Mono<IRecord> hello() {
		return Mono.just(REC("code", "0", "message", "hello", "time", now()));
	}

	/**
	 * sql语句查询 <br>
	 * http://localhost:6010/api/sqlquery?sql=select*from%20t_maozedong%20limit%2020
	 * 
	 * @param sql      SQL 语句
	 * @param exchange post 函数
	 * @return IRecord
	 */
	@RequestMapping("sqlquery")
	public Mono<IRecord> sqlquery(final @Param String sql) {
		final var ret = IRecord.REC("code", 0);
		ret.add("data", this.dataApp.sqldframe(sql));
		return Mono.just(ret);
	}

	@Autowired
	private MyDataApp dataApp;

}
