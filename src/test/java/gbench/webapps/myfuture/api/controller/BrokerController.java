package gbench.webapps.myfuture.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.text.MessageFormat.format;
import static java.time.LocalDateTime.now;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.IRecord;
import gbench.webapps.myfuture.api.config.param.Param;
import gbench.webapps.myfuture.api.msclient.BaseApiClient;
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
	public Mono<IRecord> createTraderAccount(final @Param IRecord req) {
		return baseApiClient.postJson("api/ccp/createTraderAccount", req);
	}

	@Autowired
	private MyDataApp dataApp;
	@Autowired
	private BaseApiClient baseApiClient;

	@Value("${server.port:6010}")
	private String port;
	@Value("${spring.application.name:world-api}")
	private String appname;

}
