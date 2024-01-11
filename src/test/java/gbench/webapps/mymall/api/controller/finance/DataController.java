package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.jdbc.Jdbcs.bytes2obj;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.sql.SQL.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.config.param.Param;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("finance/data")
public class DataController {

	/**
	 * 数据演示
	 * <p>
	 * http://localhost:6010/finance/data/sqlquery?sql=show tables
	 * 
	 * @return IRecord
	 */
	@RequestMapping("sqlquery")
	public Mono<IRecord> sqlquery(final @Param String sql) {
		final var dfm = jdbcApp.sqldframe(sql).fmap(e -> e.aov2rec(v -> v instanceof byte[] bb ? bytes2obj(bb) : v));
		return Mono.just(REC("code", 0, "data", dfm.rows()));
	}

	/**
	 * 数据演示
	 * <p>
	 * http://localhost:6010/finance/data/insert?sql=show tables json :
	 * 
	 * @param json JSON 数据{name,lines}
	 * @return IRecord
	 */
	@RequestMapping("insert")
	public Mono<IRecord> insert(final @Param IRecord json) {
		final var name = json.str("name"); // 表名
		final var lines = json.lls("lines", IRecord::REC); // 行数据
		final var sql = sql(name, lines); // SQL对象
		final var data = REC(); // 返回结果数据

		jdbcApp.withTransaction(sess -> {
			if (!sess.isTablePresent(name)) { // 数据表不存在
				final var ctsql = sql.ctsql(true, 2);
				sess.sql2execute(ctsql);
			}
			final var insql = sql.insql(); // 插入数据
			data.add("ids", sess.sql2execute(insql));
		});

		return Mono.just(REC("code", 0, "data", data));
	}

	@Autowired
	private IMySQL jdbcApp;
}
