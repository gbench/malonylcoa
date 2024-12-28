package gbench.webapps.crawler.api.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

import gbench.util.chn.PinyinUtil;
import gbench.util.data.DataApp.IRecord;
import gbench.util.io.FileSystem;
import gbench.util.lisp.Tuple2;
import gbench.webapps.crawler.api.model.decomposer.DecomposeUtils;
import gbench.webapps.crawler.api.model.decomposer.IDecomposer;
import gbench.webapps.crawler.api.model.decomposer.ImageDecomposer;
import gbench.webapps.crawler.api.model.decomposer.RulesDecomposer;
import gbench.webapps.crawler.api.model.decomposer.TextDecomposer;
import gbench.webapps.crawler.api.model.srch.SrchEngineAdapter;
import gbench.webapps.crawler.api.model.srch.AbstractJdbcSrchEngine.PageQuery;

import static java.text.MessageFormat.format;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.webapps.crawler.api.model.srch.SrchUtils.*;

/**
 * 检索模块（检索业务逻辑额实现，将检索功能与索引共嗯适配到SrchEngine） <br>
 * 1) getKeywords 获取关键词语料库 <br>
 * 2) lookup 关键词检索 <br>
 * 3) indexFiles 建立关键词索引文件 <br>
 * 4) refresh 刷新关键词语料库(keywords同步)
 */
public class SrchModel extends SrchEngineAdapter {
	/**
	 * 构造函数
	 * 
	 * @param indexHome  索引的保存位置路径
	 * @param corpusHome 语料库位置 路径
	 * @param snapHome   快照的保存位置路径
	 */
	public SrchModel(String indexHome, String corpusHome, String snapHome) {
		super(indexHome, corpusHome, snapHome);
	}

	/**
	 * 内置一个文件资料索引擎
	 * 
	 * @author gbench
	 *
	 */
	public class FileSrchEngine extends EmbededSrchEngine {
		/**
		 * 构造函数
		 */
		public FileSrchEngine() {
			super();
		}

		/**
		 * 构造函数
		 * 
		 * @param indexName 索引文件位置名称或者目录
		 */
		public FileSrchEngine(String indexName) {
			super(indexName);
		}

		/**
		 * 把一条分词记录转传承 索引记录
		 * 
		 * @param token 分词分词记录: 一条token是 信息（索引）的基本单元。
		 * @return 索引文档
		 */
		public Document token2doc(final IRecord token) {
			final var symbol = token.get("symbol");// 检索词
			final var statement = token.get("statement");// 语句上下文
			final var file = token.get("file");// 文件名称
			final var id = md5(token);// md5 的去重标记
			final var position = token.strOpt("position").orElse("-"); // 提关键字的位置记录
			final var snapfile = token.strOpt("snapfile").orElse("-"); // 快照文件位置
			final var tags = token.get("tags"); // 词汇分类标签
			final var rest = token.filterNot("id,symbol,statement,file,position,snapfile,type,tags"); // 剩余的自定义字段

			final var doc = rec2doc(REC(// 定义文档结构
					"id?", id, // 临时字段用户同一批次的数据的去重, 后缀?表示临时字段
					"symbol", symbol, // 文法符号
					SEARCH_FIELD, format("{0},{1},{2}", symbol, rest.vals(), tags), // 检索符号
					"text", statement, // 上下文本
					"file", file, // 对文件名称路径
					"position", position, // 关键词
					"snapfile", snapfile, // 快照文件路径
					"tags", tags // 词汇分类标签
			).add(rest));// doc

			return doc;
		}

		/**
		 * 书写索引文件<br>
		 * 重要且关键的函数：把解构的结果 tokens 编写成索引记录 文档。<br>
		 *
		 * @param tokens 文件记录集合流：每个 token 包含有 symbol 关键词，statement 上下文语句, file:文件名称 等字段。
		 */
		public synchronized void indexTokens(final Stream<IRecord> tokens) {
			super.writeIndexes(writer -> {// 使用index writer 来写索引文件
				final var counter = new AtomicLong(1l);// 计数器
				final Function<? super IRecord, ? extends IRecord> mapper = token -> { // 增加pinyin字段
					PinyinUtil.getPinyinS(token.str("symbol")).map(Tuple2.snb(0)).forEach(e -> {
						token.add("py" + e._1, e._2);
					});
					return token;
				};
				tokens.parallel().map(mapper).forEach(token -> {// 并行处理
					try {
						final var doc = token2doc(token);// 生成索引文档
						final var id = strfld(doc, "id");// 去重标签
						writer.updateDocument(T("id", id), doc);// 文档的去重保存
						// 中间按日志文本输出
						if (debug)
							System.out.println(format( // 文本格式化
									"[{0,number,#}\t{1}] ---- {2}", // 格式化模板
									counter.getAndIncrement(),
									Thread.currentThread().threadId() + "#" + Thread.currentThread().getName(),
									doc2rec(doc).filter("id,symbol,text")) // format
							);// 日志文本
					} catch (IOException e) {
						e.printStackTrace();
					} // try
				});// parallelStream 分词处理
			}, writer -> {
				trycatch(() -> writer.forceMerge(1));// 生成合并块文件
			});// indexTokens
		}

	}

	/**
	 * 表示一个可以抛出异常的函数操作的函数接口 <br>
	 * 
	 * @author gbench
	 *
	 */
	@FunctionalInterface
	interface MaybeThrowable {
		public void run() throws Throwable;
	}

	/**
	 * 这个函数被用于简化 try catch 的程序辨析。自动铺货异常。采用如下的方法就可以简写系统异常了。<br>
	 * trycatch(()->{....}); <br>
	 * 对一个可以能抛出异常的操作进行异常捕获，并给与执行。<br>
	 * 
	 * @param maybe 可能抛出异常的操作
	 */
	public static void trycatch(final MaybeThrowable maybe) {
		try {
			maybe.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 初始化函数 <br>
	 * initialize
	 */
	public SrchModel initialize() {
		// 成员属性创建与初始化
		this.fileEngine = new FileSrchEngine();
		this.fileEngine.initialize();
		return this;
	}

	/**
	 * 是否初始化完毕
	 *
	 * @return 是否初始化完毕 true:初始化,false:未初始化
	 */
	public boolean readyFlag() {
		return this.fileEngine.getKeywords().size() > 0;
	}

	/**
	 * 获取关键词集合(使用关键词进行索引编制)
	 *
	 * @return 关键词集合
	 */
	public Set<String> getKeywords() {
		return this.fileEngine.getKeywords();
	}

	/**
	 * 关键字检索
	 *
	 * @param keyword     keyword 关键词检索
	 * @param hitsPerPage 放回列表的最大长度
	 * @return 与关键字匹配的产品列表, 如果没有匹配返回null
	 */
	public List<IRecord> lookup(final String keyword, final int hitsPerPage) {
		return this.fileEngine.lookup(keyword, hitsPerPage);
	}

	/**
	 * 索引文件：建立关键词索引文件
	 * 
	 * @param fileHome 待索引的文件或文件目录
	 * @param cs       索引成功的回调函数,Consumer类型参数为IRecord:
	 *                 {code:错误代码,tokens:分词列表,files:文件列表,decompose_time:分词时间}->{}
	 */
	public void indexFiles(final String fileHome, final Consumer<IRecord> cs) {
		final var srchEngine = new FileSrchEngine(this.indexHome);
		final var yuhuan = FileSrchEngine.buildYuhuanAnalyzer(corpusHome);
		srchEngine.setAnalyzer(yuhuan);
		final var tokens = new ConcurrentLinkedQueue<IRecord>(); // 生成一个并发队列，用于接收各个 文档的处理信息。
		final var files = new ConcurrentLinkedQueue<File>(); // 生成一个并发队列，用于接收各个 文档的处理信息。
		final var dftdcp = (IDecomposer) decomposers.get("default");// 默认解构器

		// 分词开始时间
		final long begTime = System.currentTimeMillis();// 开始时间
		// 检索每个文件并生成索引
		traverse(new File(fileHome), file -> {
			final var extension = FileSystem.extensionpicker(file.getAbsolutePath()).toLowerCase();// 提取文件扩展名
			final var dcp = (IDecomposer) decomposers.opt(extension).orElse(dftdcp);// 提取对应分词器
			dcp.decompose(REC("file", file, // 待处理的文件对象
					"analyzer", yuhuan, // 分词器
					"tokens", tokens, // 分词符号
					"files", files, // 文件集合
					"snapHome", this.snapHome, // 快照根目录
					"executors", this.executors // 线程池
			));// decompose
		});
		final long decomposeTime = System.currentTimeMillis() - begTime; // 分词终止时间

		// 创建索引文件
		srchEngine.prepareIndexWriter(iwc -> {
			iwc.setMaxBufferedDocs(10000);
		});
		srchEngine.indexTokens(tokens.stream());

		// file
		cs.accept(REC("code", 0, "tokens", tokens.stream(), "files", files.stream(), "decompose_time", decomposeTime));
	}

	/**
	 * keywords同步&更新语料库路径位置 <br>
	 * 刷新关键词列表 & 提取分词器中的词典数据(以便于autocomplete之类的功能使用)。<br>
	 * 
	 * @param corpusHome 语料库目录
	 */
	public void refresh(final String corpusHome) {
		this.fileEngine.refresh(Optional.ofNullable(corpusHome) //
				.orElse(this.corpusHome));
	}

	/**
	 * 生成一个按页查询的项目对象。 PageQuery 需要经过初始化之后才能够使用
	 *
	 * @param query 查询对象
	 * @return PageQuery
	 */
	public PageQuery getPageQuery(final Query query) {
		return this.fileEngine.getPageQuery(query);
	}

	/**
	 * 生成一个按页查询的项目对象。 PageQuery 需要经过初始化之后才能够使用
	 *
	 * @param queryrec boolean查询的结构描述，eg.<br>
	 *                 REC("must", REC( // MUST 必须项目 <br>
	 *                 "should", rb("symbol*,py0*,py1*").get(rec.str("keyword")), //
	 *                 单词,全拼,简拼字段的SHOULD模糊项匹配 <br>
	 *                 "file*", rec.str("file") // 文件字段模糊项目 <br>
	 *                 )) <br>
	 * @return PageQuery
	 */
	public PageQuery getPageQuery(final IRecord queryrec) {
		return this.fileEngine.getPageQuery(parseDsl(queryrec));
	}

	/**
	 * 搜索引擎
	 * 
	 */
	private FileSrchEngine fileEngine; // 搜索引擎

	/**
	 * 结构分解器
	 */
	private IRecord decomposers = REC( // 预先加载的分词器
			"txt", new TextDecomposer(), // 文本分词器
			"jpg", new ImageDecomposer(), // 图片解构
			"jpeg", new ImageDecomposer(), // 图片解构
			"bmp", new ImageDecomposer(), // 图片解构
			"gif", new ImageDecomposer(), // 图片解构
			"png", new ImageDecomposer(), // 图片解构
			"rules", new RulesDecomposer(), // 会计准则解构
			"default", (IDecomposer) DecomposeUtils::dftdecompse // 默认分词器
	);
}
