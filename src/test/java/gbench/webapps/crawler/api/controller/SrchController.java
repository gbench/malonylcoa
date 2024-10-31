package gbench.webapps.crawler.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static java.text.MessageFormat.*;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.data.DataApp.IRecord.rb;
import static gbench.util.data.DataApp.Tuple2.P;

import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.data.DataApp.IRecord;
import gbench.webapps.crawler.api.model.SrchModel;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;
import gbench.webapps.crawler.api.model.srch.*;
import gbench.webapps.crawler.api.model.srch.AbstractJdbcSrchEngine.PageQuery;
import gbench.webapps.crawler.api.config.param.Param;

/**
 * 检索模型
 */
@RequestMapping("/api/srch")
@RefreshScope
@RestController
public class SrchController extends AbstractState<SrchController> {

	/**
	 * started callback
	 * 
	 * @return ApplicationRunner
	 */
	@Bean
	ApplicationRunner onStarted() {
		return args -> {
			this.initialize(indexHome, corpusDir, snapHome);
		};
	}

	/**
	 * 初始化
	 * 
	 * @param indexHome 索引文件路径
	 * @param corpusDir 语料库路路径
	 * @param snapHome  快照文件路径
	 */
	public SrchController initialize(final String indexHome, final String corpusDir, final String snapHome) {
		// 成员属性创建与初始化
		this.srchModel = new SrchModel(indexHome, corpusDir, snapHome).initialize();
		this.state.set(REC());// 清空state
		Trie.USE_RECURSIVE_GET_TRIE = false;// 使用LOOP模式的分词方式
		return this;
	}

	/**
	 * 系统参数配置 <br>
	 * 
	 * http://localhost:6010/api/srch/config <br>
	 * <br>
	 * curl -X POST
	 * "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=crawler-api.properties&group=DEFAULT_GROUP&content=malonylcoa.crawler.srch.fileHome=$0/java/gbench/webapps/crawler/api/model/data/docs"
	 * 
	 * @param appName 服务名称
	 * @return 配置信息
	 */
	@RequestMapping("/config")
	public IRecord config(@Value("${spring.application.name}") String appName) {
		return REC("code", 0, "name", appName, "time", LocalDateTime.now(), //
				"params", REC("indexHome", indexHome, "corpusDir", corpusDir, //
						"snapHome", snapHome, "fileHome", fileHome));
	}

	/**
	 * 请求配置信息:转发配置信息 <br>
	 * 
	 * http://localhost:6010/api/srch/config_nacos <br>
	 * 
	 * @param appName 服务名称
	 * @return 配置信息
	 */
	@RequestMapping("/config_nacos")
	public Mono<IRecord> config_nacos(@Value("${spring.application.name}") String appName) {
		final var url = format("http://{0}/api/srch/config", appName); // api 接口
		return this.webclient().post().uri(url).retrieve().bodyToMono(IRecord.class);
	}

	/**
	 * 关键词以前缀来进行检索 <br>
	 * 
	 * 请求示例 http://localhost:6010/api/srch/keywords
	 * 
	 * @param prefix 检索关键字
	 * @param size   最大长度
	 * @return 检索关键字
	 */
	@RequestMapping("keywords")
	public IRecord keywords(final @Param String prefix, final @Param Integer size) {
		synchronized (this.srchModel) {
			if (!this.srchModel.readyFlag()) {
				this.srchModel.refresh(null); // 提取分词器中的词典数据(以便于autocomplete之类的功能使用)。
			} // if
		} // synchronized

		// 直接返回结果
		return REC("code", 0, // 错误代码
				"result", this.srchModel.getKeywords().stream() // 数据结果
						.filter(e -> e != null && e.startsWith((prefix == null ? "" : prefix).strip()))
						.limit(size == null ? 10 : size)// 默认长度
						.sorted((a, b) -> a.length() - b.length()).map(e -> REC(// 返回结果字段
								"code", e, "label", e, "value", e))
						.toList()// getKeywords
		);// REC
	}

	/**
	 * 全文检索 <br>
	 * 
	 * 请求示例 http://localhost:6010/api/srch/lookup?keyword=存货
	 * 
	 * @param keyword 检索关键字
	 * @return 检索关键字
	 */
	@RequestMapping("lookup")
	public IRecord lookup(final @Param String keyword) {
		return REC("code", 0, // 错误代码
				"result", this.srchModel.lookup(keyword == null ? "" : keyword, 50) // 数据结果
		);// REC
	}

	/**
	 * 带有状态的关键字检索 <br>
	 * 全文检索:待有检索条件的项目检索 <br>
	 * 请求示例,该请求会记录sessId和agentId组成的请求key,依次遍历请求数据。
	 * http://localhost:6010/api/srch/lookup2?line=%E6%8D%AD%E9%98%96;guigu&sessId=1&agentId=1&size=1
	 * <br>
	 * http://localhost:6010/api/srch/lookup2?line=捭阖;guigu
	 * 
	 * @param line    检索关键字,symbol;file的数据格式进行指定范围的数据检索
	 * @param sessId  会话id
	 * @param agentId 用户id
	 * @param size    每页数据大小
	 * @return 检索内容
	 */
	@RequestMapping("lookup2")
	public IRecord lookup2(final @Param String line, final @Param String sessId, final @Param String agentId,
			final @Param Integer size) {
		final var pageSize = size == null || size == 0 ? Integer.MAX_VALUE : size;// 页面大小
		final var ss = Arrays.stream((line == null ? " " : line).split("[;\s]+")).map(e -> e.strip())
				.toArray(String[]::new);
		final var rec = REC( // 输入信息预处理
				"keyword", format("*{0}*", ss.length > 0 ? ss[0] : ""), // 关键词
				"file", format("*{0}*", ss.length > 1 ? ss[1] : "") // 检索文件
		); // 输入结果分析
		final var queryrec = REC("must", REC( // MUST 必须项目
				"should", rb("symbol*,py0*,py1*").get(rec.str("keyword")), // 单词,全拼,简拼字段的SHOULD模糊项匹配
				"file*", rec.str("file") // 文件字段模糊项目
		)); // 解析成Query对象

		System.out.println(format("\n#lookup2:\nline:【{0}】\nrec:【{1}】,sessionId:{2},agentId:{3},size:{4}", //
				line, rec, sessId, agentId, size));

		// 设置各种key
		final var keys = this.accessorKeys(line, sessId, agentId, size);
		final var pageQueryKey = keys.str("pageQueryKey");
		final var lineKey = keys.str("lineKey");
		final var pageNumKey = keys.str("pageNumKey");
		final var pageTotalKey = keys.str("pageTotalKey");

		System.out.println(format("pageQueryKey:{0},lineKey:{1},pageNumKey:{2},pageTotalKey:{3}", //
				pageQueryKey, lineKey, pageNumKey, pageTotalKey));

		final PageQuery pageQuery; // 按页查询
		final Optional<PageData> optional; // 页面数据

		if (!line.equals(this.stateOfT(lineKey, Object.class))) {// 初次访问
			optional = (pageQuery = this.srchModel.getPageQuery(queryrec).initialize(pageSize)).getData2();
			// 程序状态
			this.setState(line, lineKey);// 记录当前检索的请求
			this.setState(pageQuery, pageQueryKey);
			this.setState(1l, pageNumKey);// 记录行号
			this.setState((long) Math.ceil(pageQuery.getTotalHits().value() / pageSize) + 1, pageTotalKey);
		} else { // 非初次访问
			optional = (pageQuery = this.stateOfT(pageQueryKey, PageQuery.class)).nextPageNoThrow();
			// 获取页面状态
			final var pagenum = this.getState(1l, pageNumKey);
			final var pageTotal = this.getState(1l, pageTotalKey);
			// 更新程序状态
			this.setState(optional.isPresent() ? pagenum + 1 : pageTotal, pageNumKey);// 设置当前页
		} // if

		return REC("code", 0, // 错误代码
				"pagenum", this.stateOfInteger(pageNumKey), // 页号
				"pagetotal", this.stateOfInteger(pageTotalKey), // 总页数
				"pagesize", pageSize, // 页面尺寸
				"size", optional.map(e -> e.getTotalHits().value()).orElse(0l), // 中数量
				"result", optional.map(e -> e.hitsStream()).orElse(Stream.of()).toList() // 数据结果
		);// REC
	}

	/**
	 * 清除 lookup2 中的各种状态空值键(需要注意参数需要与lookup2保持一样)
	 * 
	 * @param line
	 * @param sessId
	 * @param agentId
	 * @param size
	 * @return 清除的检索键信息
	 */
	@RequestMapping("clear_lookup2")
	public IRecord clear_lookup2(final @Param String line, final @Param String sessId, final @Param String agentId,
			final Integer size) {
		final var keys = this.accessorKeys(line, sessId, agentId, size);
		keys.valueS().map(Object::toString).forEach(this::remove);

		return REC("code", 0, "keys_removed", keys, "time", LocalDateTime.now());
	}

	/**
	 * 清除所有状态
	 * 
	 * 请求示例 http://localhost:6010/api/srch/clear
	 * 
	 * @return 清除的检索键信息
	 */
	@RequestMapping("clear")
	public IRecord clear() {
		final var state = this.state.get().duplicate();
		state.keys().forEach(this.state.get()::remove);

		return REC("code", 0, "keys_removed", state.keys(), "time", LocalDateTime.now());
	}

	/**
	 * 刷新关键词列表 <br>
	 * 请求示例 http://localhost:6010/api/srch/refresh
	 * 
	 * @param corpusHome 语料库位置
	 * @return 刷新列表
	 */
	@RequestMapping("refresh")
	public IRecord refresh(final @Param String corpusHome) {
		es.execute(() -> this.srchModel.refresh(corpusHome));// 执行刷新请求
		return REC("code", 0, // 错误代码
				"result", "刷新请求已经收到" // 数据结果
		);// REC
	}

	/**
	 * 全文检索
	 * 
	 * 请求示例 <br>
	 * http://localhost:6010/api/srch/indexfiles?fileHome=C:/Users/xuqinghua/Desktop/史记.txt
	 * 
	 * @param fileHome 原始资料数据文件位置
	 * @return 检索关键字
	 */
	@RequestMapping("indexfiles")
	public IRecord indexFiles(final @Param String fileHome) {
		if (fileHome == null) {
			return REC("code", 2, // 错误代码
					"result", format("fileHome 为空,不予执行索引", ""));// REC
		} // if

		// 索引线程
		final Runnable runnable = () -> {// 创建线程 索引文件
			this.srchModel.indexFiles(fileHome, rec -> { // 索引文件
				final var tokens = rec.pathgetS("tokens", IRecord.class);
				final var files = rec.pathgetS("files", File.class);
				final var decomposeTime = rec.pathlng("decompose_time");
				final var start = this.stateOfLong("indexfiles.start.time");
				final var pattern = "\n## {0} 索引完毕!\n 索引词条:{1} 条,索引文件:{2} 个, 分词历时:{3} s, 索引总历时:{4} s";

				System.out.println(format(pattern, this.stateOfString("indexfiles.file.to.be.indexed"), tokens.count(),
						files.count(), decomposeTime / 1000d, /* 分解结构 */
						(System.currentTimeMillis() - start) / 1000d /* 历时时间 */
				));// println

				this.state("indexfiles.running", false); // 清除运行状态
				this.state("indexfiles.file.to.be.indexed", null); // 去除索引文件
				this.state("indexfiles.start.time", null); // 去除开始时间
			});// 索引文件
		};

		// 仅当没有文件被索引的时候，开启文件索引功能
		if (!this.stateOfBoolean("indexfiles.running")) {// 读取indexfiles的执行状态
			this.state("indexfiles.running", true);// true 表示开始执行
			this.state("indexfiles.file.to.be.indexed", fileHome);// true 表示开始执行
			this.state("indexfiles.start.time", System.currentTimeMillis());// 记录开始时间

			es.execute(runnable);// 开始执行文件索引

			// 返回结果
			return REC("code", 0, // 错误代码
					"result", format("开始索引:{0}！", fileHome));// REC
		} else {
			// 返回结果
			return REC("code", 1, // 错误代码
					"result", format("{0}正在被索引， 请稍后再予以申请！", //
							this.stateOfString("indexfiles.file.to.be.indexed")));// REC
		} // 线程文件
	}

	/**
	 * webclient
	 * 
	 * @return webclient
	 */
	public WebClient webclient() {
		return wb().build();
	}

	/**
	 * web客户端构建器 <br>
	 * ### 不要直接使用wbProto,而是要使用自定义版本wb()以保护原始的wbProto ### <br>
	 * <br>
	 * 这是一个保护原始wbProto的方法，以保证wbProto不会被污染。<br>
	 * 构造一个自定义的WebClient.Builder <br>
	 * 
	 * @return 自定义的WebClient.Builder
	 */
	public WebClient.Builder wb() {
		return wbProto.clone();
	}

	/**
	 * lookup2 的访问key (需要注意参数需要与lookup2保持一样)
	 * 
	 * @param line
	 * @param sessId
	 * @param agentId
	 * @param size
	 * @return IRecord
	 *         键名映射(keyPrefix;_,pageQueryKey:_,lineKey:_,pageNumKey:_,pageTotalKey:_)
	 */
	private IRecord accessorKeys(final String line, final String sessId, final String agentId, final Integer size) {
		final var keyPrefix = format("{0}@{1}", agentId, sessId); // key前缀
		final var pageQueryKey = format("${0}.${1}.${2}", keyPrefix, "lookup2", "pageQuery");
		final var lineKey = format("${0}.${1}.${2}", keyPrefix, "lookup2", "line");
		final var pageNumKey = format("${0}.${1}.${2}", keyPrefix, "lookup2", "pagenum");
		final var pageTotalKey = format("${0}.${1}.${2}", keyPrefix, "lookup2", "pageTotal");
		return IRecord.rb("keyPrefix,pageQueryKey,lineKey,pageNumKey,pageTotalKey") //
				.get(keyPrefix, pageQueryKey, lineKey, pageNumKey, pageTotalKey);
	}

	private SrchModel srchModel; // 搜索APP
	private ExecutorService es = Executors.newFixedThreadPool(1);// 创建一个线程队列
	@Autowired
	private WebClient.Builder wbProto; // web客户端构建器原型,不要直接使用web,而是要使用自定义版本。mywb()以保护原始的wb
	@Value("${malonylcoa.crawler.srch.corpusDir:F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/crawler/api/model/data/corpus/}")
	private String corpusDir;
	@Value("${malonylcoa.crawler.srch.indexHome:D:/sliced/tmp/crawler/index}")
	private String indexHome;
	@Value("${malonylcoa.crawler.srch.snapHome:D:/sliced/tmp/crawler/snap}")
	private String snapHome;
	@Value("${malonylcoa.crawler.srch.fileHome:F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/crawler/api/model/data/docs}")
	private String fileHome;

}

/**
 * 
 * @author gbench
 *
 * @param <SELF> 子类的实际类型
 */
class AbstractState<SELF> {

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return boolean
	 */
	public boolean stateOfBoolean(String name) {
		return this.state.get().bool(name);
	}

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return int
	 */
	public int stateOfInteger(String name) {
		return this.state.get().i4(name);
	}

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return double
	 */
	public double stateOfDouble(String name) {
		return this.state.get().dbl(name);
	}

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return long
	 */
	public long stateOfLong(String name) {
		return this.state.get().pathlng(name);
	}

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public <T> T stateOfT(String name, Class<T> clazz) {
		return (T) this.state.get().get(name);
	}

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return String
	 */
	public String stateOfString(String name) {
		return this.state.get().str(name);
	}

	/**
	 * 
	 * @param <T>          返回值类型
	 * @param defaultValue 默认值 或者 值类型
	 * @param fields       键名序列
	 * @return 属性的值
	 */
	@SuppressWarnings("unchecked")
	public <T> T getState(final T defaultValue, final Object... fields) {
		final String key = Arrays.stream(fields).map(e -> e + "").collect(Collectors.joining("."));
		final Class<T> tclass = defaultValue instanceof Class ? (Class<T>) defaultValue
				: defaultValue == null ? (Class<T>) Object.class : (Class<T>) defaultValue.getClass();
		final T t = this.stateOfT(key, tclass);
		if (t != null)
			return t;
		else
			return defaultValue instanceof Class ? null : defaultValue;
	}

	/**
	 * 设置状态
	 * 
	 * @param <T>
	 * @param value
	 * @param strings
	 * @return SELF
	 */
	public <T> SELF setState(T value, String... strings) {
		final String key = Arrays.stream(strings).collect(Collectors.joining("."));
		return this.state(key, value);
	}

	/**
	 * bool 类型的状态
	 * 
	 * @param name
	 * @return SELF
	 */
	@SuppressWarnings("unchecked")
	public SELF state(final String name, final Object value) {
		// 重新设置新的状态信息
		this.state.set(this.state.get().derive(Arrays.asList(P(name, value))));
		return (SELF) this;
	}

	/**
	 * 删除指定键名属性
	 * 
	 * @param name 键名
	 * @return SELF
	 */
	@SuppressWarnings("unchecked")
	public SELF remove(final String name) {
		this.state.get().remove(name);
		return (SELF) this;
	}

	protected AtomicReference<IRecord> state = new AtomicReference<IRecord>(); // 系统状态

}
