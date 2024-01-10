package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.jdbc.Jdbcs.bytes2obj;

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
		return Mono.just(IRecord.REC("code", 0, "data", dfm.rows()));
	}

	@Autowired
	private IMySQL jdbcApp;
}
