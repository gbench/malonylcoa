package gbench.webapps.myfuture.api.controller;

import static gbench.util.data.DataApp.checkerr;
import static gbench.util.data.MyDataApp.insert_sql;
import static gbench.util.lisp.IRecord.REC;
import static java.text.MessageFormat.format;
import static java.time.LocalDateTime.now;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.chn.PinyinUtil;
import gbench.util.data.DataApp.DFrame;
import gbench.util.data.MyDataApp;
import gbench.util.io.Output;
import gbench.util.lisp.IRecord;
import gbench.webapps.myfuture.api.config.param.Param;
import reactor.core.publisher.Mono;

/**
 * 注意参数必须加入 @Param 标记进行注释，否则 post方法会获得不到参数。
 */
@RestController
@RequestMapping("api/ccp")
public class CCPController {

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
	 * http://localhost:7010/api/cp/component
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
	 * 请求示例 <br>
	 * $.ajax({ <br>
	 * url:"http://localhost:7010/api/cp/createTraderAccount", <br>
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
	public Mono<IRecord> createTraderAccount(final @Param IRecord req) {
		final var ret = IRecord.REC("code", 0);
		final var rec = bh.createTraderAccount(req);
		if (rec.has("$code")) { // 提取错误码
			ret.set("code", rec.get("$code"));
		}
		return Mono.just(ret.add("data", rec.filter((k, v) -> !k.startsWith("$")))); // 将内部状态字段($开头)剔除后返回
	}

	/**
	 * 请求示例 <br>
	 * $.ajax({ <br>
	 * url:"http://localhost:7010/api/cp/createTraderAccount", <br>
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
	@RequestMapping("createSecurity")
	public Mono<IRecord> createSecurity(final @Param IRecord req) {
		final var ret = IRecord.REC("code", 0);
		final var rec = sh.createSecurity(req);
		if (rec.has("$code")) { // 提取错误码
			ret.set("code", rec.get("$code"));
		}
		return Mono.just(ret.add("data", rec.filter((k, v) -> !k.startsWith("$")))); // 将内部状态字段($开头)剔除后返回
	}

	/**
	 * 请求示例 <br>
	 * $.ajax({ <br>
	 * url:"http://localhost:7010/api/cp/createTraderAccount", <br>
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
	@RequestMapping("createOrder")
	public Mono<IRecord> createOrder(final @Param IRecord req) {
		final var ret = IRecord.REC("code", 0);
		final var rec = oh.createOrder(req);
		if (rec.has("$code")) { // 提取错误码
			ret.set("code", rec.get("$code"));
		}
		return Mono.just(ret.add("data", rec.filter((k, v) -> !k.startsWith("$")))); // 将内部状态字段($开头)剔除后返回
	}

	/**
	 * 请求示例 <br>
	 * $.ajax({ <br>
	 * url:"http://localhost:7010/api/cp/createTraderAccount", <br>
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
	@RequestMapping("matchOrder")
	public Mono<IRecord> matchOrder(final @Param IRecord req) {
		final var ret = IRecord.REC("code", 0);
		final var rec = mh.matchOrder(req);
		if (rec.has("$code")) { // 提取错误码
			ret.set("code", rec.get("$code"));
		}
		return Mono.just(ret.add("data", rec.filter((k, v) -> !k.startsWith("$")))); // 将内部状态字段($开头)剔除后返回
	}

	/**
	 * sql语句查询 <br>
	 * http://localhost:7010/api/cp/sqlquery?sql=show tables
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
	 * sql语句查询 <br>
	 * http://localhost:7010/api/cp/sqlexecute?sql=xxx
	 * 
	 * @param sql SQL 语句
	 * @return IRecord
	 */
	@RequestMapping("sqlexecute")
	public Mono<IRecord> sqlexecute(final @Param String sql) {
		final var ret = IRecord.REC("code", 0);
		ret.add("data", this.dataApp.sqlexecuteopt(sql).orElse(DFrame.of()));
		return Mono.just(ret);
	}

	/**
	 * 券商
	 */
	private class BrokerHelper extends DBPesist {
		/**
		 * 创建交易者账户
		 * 
		 * @param req 数据申请记录
		 * @return
		 */
		public IRecord createTraderAccount(final IRecord req) {
			Output.println("createTraderAccount: req", req);
			final var now = LocalDateTime.now();
			final var serialnum = ai.getAndIncrement(); // 流水号
			final var flds = "CODE,ABBRE,NAME,PASSWORD,ID_CARD,BANK_ACCOUNT,MARGIN_ACCOUNT,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
			final var datarec = IRecord.rb(flds).get(String.format("TRADER%03d", serialnum),
					PinyinUtil.getPinyinShort(req.get("name")).toUpperCase(), req.get("name"), req.get("password"),
					req.get("idcard"), req.get("bankcard"), String.format("MACCT%03d", serialnum), now, now,
					req.opt("description").orElse("普通交易者"));
			return this.insert("t_trader", datarec);
		}
	}

	/**
	 * 证券
	 */
	private class SecurityHelper extends DBPesist {
		/**
		 * 创建交易者账户
		 * 
		 * @param req 数据申请记录
		 * @return
		 */
		public IRecord createSecurity(final IRecord req) {
			Output.println("createSecurity: req", req);
			final var now = LocalDateTime.now();
			final var flds = "TYPE,XCHG,CODE,ABBRE,NAME,OPEN_DATE,CLOSE_DATE,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
			final var datarec = IRecord.rb(flds).get(req.str(("type")), req.str(("xchg")), req.str(("code")),
					PinyinUtil.getPinyinShort(req.get("name")).toUpperCase()
							+ req.strOpt("name").map(e -> e.replaceAll("^[^\\d]*", "")).orElse(""),
					req.get("name"), req.get("open"), req.get("close"), now, now,
					req.opt("description").orElse("金融证券"));
			return this.insert("t_security", datarec);
		}
	}

	/**
	 * 证券
	 */
	private class OrderHelper extends DBPesist {
		/**
		 * 创建交易者账户
		 * 
		 * @param req 数据申请记录
		 * @return
		 */
		public IRecord createOrder(final IRecord req) {
			Output.println("createOrder: req", req);
			final var now = LocalDateTime.now();
			final var flds = "TRADER_ID,SECURITY_ID,POSITION,PRICE,QUANTITY,UNMATCHED,REVISION,UPDATE_TIME,CREATE_TIME,DESCRIPTION";
			final var datarec = IRecord.rb(flds).get(req.str(("traderid")), req.str(("securityid")),
					Objects.equals("LONG", req.str("position")) ? 1 : -1, req.str("price"), req.str("quantity"),
					req.str("quantity"), 1, now, now, req.opt("description").orElse("金融证券"));
			return this.insert("t_order", datarec);
		}
	}

	/**
	 * 证券
	 */
	private class MatchHelper extends DBPesist {
		/**
		 * 创建交易者账户
		 * 
		 * @param req 数据申请记录
		 * @return
		 */
		public IRecord matchOrder(final IRecord req) {
			Output.println("createOrder: req", req);
			final var now = LocalDateTime.now();
			final var flds = "LONG_ORDER_ID,SHORT_ORDER_ID,SECURITY_ID,PRICE,QUANTITY,CREATE_TIME,UPDATE_TIME,DESCRIPTION";
			final var datarec = IRecord.rb(flds).get(req.str(("loid")), req.str(("soid")), req.get("securityid"),
					req.get("price"), req.get("quantity"), now, now, req.opt("description").orElse("撮合交易单"));
			return this.insert("t_match_order", datarec);
		}
	}

	/**
	 * 数据持久化
	 */
	public class DBPesist {

		/**
		 * 数据插入
		 */
		public IRecord insert(final String tbl, final IRecord datarec) {
			final var insql = insert_sql(tbl, datarec.toMap());
			final var local = new AtomicReference<IRecord>(REC()); // 本地会话变量
			return dataApp.sqlexecuteopt(insql) //
					.flatMap(rs -> checkerr(rs, errinfo -> local.get().set("$error", errinfo.str("$error"))
							.set("$exception", errinfo.str("$exception"))).map(linedfm -> linedfm.head().get(0)) // 提出插入数据的主键
					).flatMap(id -> { // 数据主键
						final var dfm = dataApp.sqldframe("select * from %s order by ID desc".formatted(tbl));
						final var ret = dfm.rowS().filter(e -> Objects.equals(id, e.get(0))).findFirst()
								.map(e -> REC(e.toMap()));
						return ret;
					}).orElseGet(() -> REC("$code", 1, "error", local.get().opt("$error").orElse("创建失败"), "exception",
							local.get().opt("$exception").orElse("-"), "reqrec", datarec, "insql", insql));
		}
	}

	@Autowired
	private MyDataApp dataApp;

	@Value("${server.port:6010}")
	private String port;
	@Value("${spring.application.name:world-api}")
	private String appname;
	private AtomicInteger ai = new AtomicInteger(10);

	private BrokerHelper bh = this.new BrokerHelper();
	private SecurityHelper sh = this.new SecurityHelper();
	private OrderHelper oh = this.new OrderHelper();
	private MatchHelper mh = this.new MatchHelper();

}
