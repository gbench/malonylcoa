package gbench.webapps.myfuture.api.controller;

import static gbench.util.data.MyDataApp.insert_sql;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.REC;
import static java.text.MessageFormat.format;
import static java.time.LocalDateTime.now;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.data.MyDataApp;
import gbench.util.io.Output;
import gbench.util.lisp.IRecord;
import gbench.webapps.myfuture.api.config.param.Param;
import reactor.core.publisher.Mono;

/**
 * 注意参数必须加入 @Param 标记进行注释，否则 post方法会获得不到参数。
 */
@RestController
@RequestMapping("api/broker")
@CrossOrigin(origins = "*") // 允许所有来源
public class BrokerController {

	/**
	 * 模块信息 <br>
	 * <p>
	 * http://localhost:7010/api/info
	 *
	 * @return IRecord
	 */
	@RequestMapping("info")
	public Mono<IRecord> info() {
		return Mono.just(REC("code", "0", "data", //
				REC("service", format("MYFUTURE-API-{0}:{1}", "MicroService", port), //
						"time", now())));
	}

	/**
	 * 提取组件模块 <br>
	 * <p>
	 * http://localhost:7010/msvc/component
	 *
	 * @return IRecord
	 */
	@RequestMapping("component")
	public Mono<IRecord> component(final @Param String name) {
		return Mono.just(REC("code", "0", "data", //
				REC("name",
						Optional.ofNullable(name).map(e -> e.matches("^\\s*$") ? null : e)
								.orElse("UNNAMED-" + UUID.randomUUID()), //
						"service", format("{0}:{1}", appname, port), //
						"time", now())));
	}

	/**
	 * sql语句查询 <br>
	 * http://localhost:7010/msvc/sqlquery?sql=show tables
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

	/**
	 * 请求示例 <br>
	 * $.ajax({ <br>
	 * url:"http://localhost:7010/api/broker/createTraderAccount", <br>
	 * data:{req:JSON.stringify({name:"zhangsan1", idcard:"210222198206238734",
	 * bankcard:"6250056654287"})}, <br>
	 * method:"post", <br>
	 * success:e=>{ <br>
	 * console.log("7010",JSON.stringify(e)); <br>
	 * } <br>
	 * }); <br>
	 * 
	 * @param req
	 * @return
	 */
	@RequestMapping("createTraderAccount")
	public Mono<IRecord> openAccount(final @Param IRecord req) {
		final var ret = IRecord.REC("code", 0);
		return Mono.just(ret.add("data", bh.createTraderAccount(req)));
	}

	private class BrokerHelper {
		/**
		 * 创建交易者账户
		 * 
		 * @param req 数据申请记录
		 * @return
		 */
		public IRecord createTraderAccount(final IRecord req) {
			Output.println(req);
			final var now = LocalDateTime.now();
			final var no = ai.getAndIncrement();
			final var rec = REC();
			final var reqrec = IRecord
					.rb("CODE,ABBRE,NAME,ID_CARD,BANK_ACCOUNT,MARGIN_ACCOUNT,CREATE_TIME,UPDATE_TIME,DESCRIPTION")
					.get(String.format("TRADER%03d", no), req.get("name"), req.get("idcard"), req.get("bankcard"),
							String.format("MA%03d", no), now, now, "-");
			dataApp.withTransaction(sess -> {
				final var insql = insert_sql("t_trader", reqrec.toMap());
				final var rs = sess.sql2execute(insql);
				if (rs != null && rs.size() > 0) {
					final var id = rs.get(0).get(0);
					final var dfm = sess.sql2x("select * from t_trader order by ID desc");
					println(dfm);
					dfm.rowS().filter(e -> Objects.equals(id, e.get(0))).findFirst().ifPresent(e -> {
						rec.add(e);
					});
				} else {
					rec.add("error", "创建失败", "reqrec", reqrec, "insql", insql);
				} // if
			});

			return rec;
		}
	}

	@Autowired
	private MyDataApp dataApp;

	private BrokerHelper bh = this.new BrokerHelper();

	@Value("${server.port:6010}")
	private String port;
	@Value("${spring.application.name:world-api}")
	private String appname;
	private AtomicInteger ai = new AtomicInteger(10);

}
