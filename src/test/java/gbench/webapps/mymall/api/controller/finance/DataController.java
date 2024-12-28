package gbench.webapps.mymall.api.controller.finance;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.jdbc.Jdbcs.bytes2obj;
import static gbench.util.jdbc.Jdbcs.imports;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.sql.SQL.sql;
import static java.time.LocalDateTime.now;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.config.param.Param;
import reactor.core.publisher.Mono;

/**
 * 财务数据的接口控制器
 */
@RestController
@RequestMapping("finance/data")
public class DataController {

	/**
	 * 查询数据
	 * <p>
	 * http://localhost:6010/finance/data/sqlquery?sql=show tables
	 * 
	 * @param sql 语句
	 * @return IRecord
	 */
	@RequestMapping("sqlquery")
	public Mono<IRecord> sqlquery(final @Param String sql) {
		final var dfm = jdbcApp.sqldframe(sql).fmap(e -> e.aov2rec(v -> v instanceof byte[] bb ? bytes2obj(bb) : v));
		return Mono.just(REC("code", 0, "data", dfm.rows()));
	}

	/**
	 * 清空数据
	 * <p>
	 * http://localhost:6010/finance/data/sqlexecute?sql=update t_order set
	 * creator_id=2 where id=1
	 * 
	 * @param sql SQL语句
	 * @return IRecord
	 */
	@RequestMapping("sqlexecute")
	public Mono<IRecord> sqlexecute(final @Param String sql) {
		final var data = new ArrayList<IRecord>();
		jdbcApp.withTransaction(sess -> {
			data.addAll(sess.sql2execute(sql));
		});
		return Mono.just(REC("code", 0, "data", data));
	}

	/**
	 * 插入数据
	 * <p>
	 * http://localhost:6010/finance/data/insert
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

	/**
	 * 清空数据
	 * <p>
	 * http://localhost:6010/finance/data/clear?json="name":"t_order"
	 * 
	 * @param json JSON 数据{name}
	 * @return IRecord
	 */
	@RequestMapping("clear")
	public Mono<IRecord> clear(final @Param Map<String, Object> json) {
		jdbcApp.withTransaction(sess -> {
			final var tbl = REC(json).str("name");
			final var sql = String.format("truncate table %s", tbl);
			System.out.println(String.format("sql:[%s],\tjson:%s", sql, json));
			sess.sql2execute(sql);
		});
		return Mono.just(REC("code", 0, "message", "OK", "time", now()));
	}

	/**
	 * 清空数据
	 * <p>
	 * <a href=
	 * "http://localhost:6010/finance/data/reset?datafile=F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/data/acct_data.xlsx"
	 * >reset</a>
	 * 
	 * @param json JSON 数据{name}
	 * @return IRecord
	 */
	@RequestMapping("reset")
	public Mono<IRecord> reset(final @Param String datafile) {
		final var datafileXls = xls(datafile); // 数据-源文件
		final var tables = datafileXls.sheetS().map(e -> e.getSheetName()).toArray(String[]::new); // 基础数据表
		jdbcApp.withTransaction(
				imports(e -> datafileXls.autoDetect(e).collect(gbench.util.jdbc.kvp.DFrame.dfmclc2), tables));

		return Mono.just(REC("code", 0, "message", "OK", "time", now()));
	}

	@Autowired
	private IMySQL jdbcApp;
}
