package gbench.webapps.crawler.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.text.MessageFormat.format;
import static java.time.LocalDateTime.now;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
import gbench.webapps.crawler.api.config.param.Param;
import gbench.webapps.crawler.api.model.MediaModel;
import reactor.core.publisher.Mono;

/**
 * 注意参数必须加入 @Param 标记进行注释，否则 post方法会获得不到参数。
 */
@RestController
@RequestMapping("api")
public class ApiController {

	/**
	 * 提取组件模块 <br>
	 * 
	 * http://localhost:6010/api/component
	 * 
	 * @return IRecord
	 */
	@RequestMapping("component")
	public Mono<IRecord> component(final @Param String name) {
		return Mono.just(REC("code", "0", "data", //
				REC("name", Optional.ofNullable(name).map(e -> e.matches("^\\s*$") ? null : e)
						.orElse("UNNAMED-" + UUID.randomUUID()), "time", now())));
	}

	/**
	 * sql语句查询 <br>
	 * http://localhost:6010/api/sqlquery?sql=select*from%20t_maozedong%20limit%2020
	 * 
	 * @param sql SQL 语句
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
	 * handleFileUpload
	 * 
	 * @param file
	 * @return IRecord
	 */
	@RequestMapping(value = { "upload" })
	public Mono<IRecord> upload(@RequestPart("file") final Mono<FilePart> fileMono) {
		return (Mono<IRecord>) fileMono.flatMap(file -> {
			return DataBufferUtils.join(file.content()).map(d -> {
				try (final var inputStream = d.asInputStream()) {
					mediaModel.store(inputStream, file.filename().replaceFirst("\\.([^.]+)$", // 时间戳
							format("_{0}.$1", UUID.randomUUID())));
				} catch (Exception e) {
					e.printStackTrace();
				} // try
				return REC("code", 0, "message", format("You successfully uploaded {0}!", file.filename()));
			}); // DataBufferUtils
		});
	}

	@Autowired
	private MyDataApp dataApp;
	@Autowired
	private MediaModel mediaModel;

}
