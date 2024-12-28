package gbench.webapps.mymall.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.text.MessageFormat.format;
import static java.time.LocalDateTime.now;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.IRecord;
import gbench.webapps.mymall.api.model.MediaModel;
import gbench.webapps.mymall.api.config.param.Param;
import reactor.core.publisher.Mono;

/**
 * 注意参数必须加入 @Param 标记进行注释，否则 post方法会获得不到参数。
 */
@RestController
@RequestMapping("api")
public class ApiController {

	/**
	 * 模块信息 <br>
	 * <p>
	 * http://localhost:6010/api/info
	 *
	 * @return IRecord
	 */
	@RequestMapping("info")
	public Mono<IRecord> info() {
		return Mono.just(REC("code", "0", "data", //
				REC("service", format("{0}:{1}", appname, port), //
						"time", now())));
	}

	/**
	 * 提取组件模块 <br>
	 * <p>
	 * http://localhost:6010/api/component
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
	
	/**
	 * 读取文件 <br>
	 * <p>
	 * http://localhost:6010/api/readfile?file=C:/Users/Administrator/Pictures/foods/火锅/火锅1.jpg
	 *
	 * @param file     文件绝对路径
	 * @param response response
	 * @return 读取文件
	 * @throws IOException
	 */
	@RequestMapping(value = { "readfile" })
	public Mono<Void> readfile(final String file, final ServerHttpResponse response) throws IOException {
		try {
			final var tup = mediaModel.readFile2(file);
			final var bufferX = DataBufferUtils.readByteChannel(tup._2::getChannel, new DefaultDataBufferFactory(),
					4096);
			final var resp = response;
			final var header = resp.getHeaders();
			final var ss = tup._1.split("/");

			if (ss.length > 0) {
				header.setContentType(new MediaType(ss[0], ss[1]));
			} // if

			return resp.writeWith(bufferX);
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return Mono.empty();
	}

	/**
	 * upload 文件上传
	 *
	 * @param fileMono 文件对象
	 * @return IRecord
	 */
	@RequestMapping(value = { "upload" })
	public Mono<IRecord> upload(@RequestPart("file") final Mono<FilePart> fileMono) {
		return fileMono.flatMap(file -> {
			return DataBufferUtils.join(file.content()).map(d -> {
				final var rec = REC("code", 0);
				try (final var inputStream = d.asInputStream()) {
					final var stamp = UUID.randomUUID(); // 印戳标记
					final var filename = file.filename().replaceFirst("\\.([^.]+)$", format("_{0}.$1", stamp));
					final var path = mediaModel.store(inputStream, filename);
					rec.add("message", format("You successfully uploaded {0}!", filename), "path", path);
				} catch (Exception e) {
					rec.add("code", 1);
					e.printStackTrace();
				} // try
				return rec;
			}); // DataBufferUtils
		});
	}

	@Autowired
	private MyDataApp dataApp;
	
	@Autowired
	private MediaModel mediaModel;

	@Value("${server.port:6010}")
	private String port;
	@Value("${spring.application.name:mymall-api}")
	private String appname;

}
