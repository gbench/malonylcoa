package gbench.webapps.crawler.api.model.srch;

import java.io.File;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import gbench.util.data.DataApp;
import gbench.util.io.FileSystem;
import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;
import org.apache.lucene.analysis.Analyzer;

import static java.text.MessageFormat.format;

/**
 *
 */
public class SrchApp {
	/**
	 * 构造函数
	 *
	 * @param indexHome  索引库目录
	 * @param corpusHome 语料库目录
	 * @param snapHome   快照库目录
	 */
	public SrchApp(final String indexHome, final String corpusHome, final String snapHome) {
		this.indexHome = indexHome;
		this.corpusHome = corpusHome;
		this.snapHome = FileSystem.unixpath(snapHome);
	}

	/**
	 * 内置一个文件索索引擎
	 *
	 * @author gbench
	 */
	public class JdbcSrchEngine extends AbstractJdbcSrchEngine {

		/**
		 * 构造函数
		 */
		public JdbcSrchEngine() {
			this(SrchApp.this.indexHome);
			this.corpusHome = SrchApp.this.corpusHome;
		}

		/**
		 * 构造函数
		 *
		 * @param indexName 索引文件位置名称或者目录
		 */
		public JdbcSrchEngine(final String indexName) {
			this.indexHome = indexName;
			this.corpusHome = SrchApp.this.corpusHome;
		}

		/**
		 * 搜索引擎初始化
		 */
		public JdbcSrchEngine initialize() {
			analyzer = this.getYuhuanAnalyzer(SrchApp.this.corpusHome);
			return this;
		}

		/**
		 * @param analyzer 分析器
		 */
		public JdbcSrchEngine setAnalyzer(final Analyzer analyzer) {
			this.analyzer = analyzer;
			return this;
		}

		/**
		 * 获取关键词集合
		 *
		 * @return 关键词集合
		 */
		public Set<String> getKeywords() {
			return keywords;
		}

		/**
		 * 暴露分词器词典 <br>
		 * 刷新关键词列表 & 提取分词器中的词典数据(以便于autocomplete之类的功能使用)。<br>
		 * 
		 * @param forced 是否强制刷新
		 */
		@SuppressWarnings("unchecked")
		public void refresh(boolean forced) {
			final var begTime = System.currentTimeMillis(); // 记录开始运行时间

			synchronized (keywords) {// 关键词刷新
				keywords.clear(); // 清空关键词列表

				if (this.analyzer != null) {// 分词器有效
					if ((corpus == null && this.analyzer instanceof YuhuanAnalyzer) || forced) {
						if (forced) { // 强制刷新标志
							System.out.println(format("强制刷新语料库:{0} @ {1}", forced, LocalDateTime.now()));
						} // if
						SrchApp.this.corpus = ((YuhuanAnalyzer) this.analyzer).findOne(Trie.class);
					} else {//
						System.err.println("分词器为 " + this.analyzer.getClass() + " 类型, 不予提供 关键词刷新操作");
					} // if this.analyzer
				} else {// 分词器无效
					System.out.println("分词器为null,请运行initialize初始化或是添加适当的分词器");
				} // if this.analyzer

				if (null != SrchApp.this.corpus)
					SrchApp.this.corpus.traverse(e -> {
						if ("word".equals(e.getAttribute("category"))) {
							keywords.add(e.getPath(cc -> cc.collect(Collectors.joining())));
						} // if
					});// traverse
			} // synchronized

			final var duration = System.currentTimeMillis() - begTime; // 持续的时间
			System.out.println(MessageFormat.format( // 词汇量的分解
					"FileSrchEngine refresh last for:{0} s, 累积词汇量:{1,number,#} 个", duration / 1000.0, keywords.size())); // 显示刷新过程的时间消耗。
		}

		/**
		 * 刷新关键词列表 & 提取分词器中的词典数据(以便于autocomplete之类的功能使用)。<br>
		 * 
		 * @param corpusHome 语料库目录,党分词器目录与当前分词器目录不同时候,进行分词器更换
		 */
		public void refresh(final String corpusHome) {
			if (!Objects.equals(corpusHome, this.corpusHome) && new File(corpusHome).exists()) {
				this.corpusHome = corpusHome;
				JdbcSrchEngine.this.setAnalyzer(this.getYuhuanAnalyzer(corpusHome));
				System.out.println(format("更换语料库目录为：{0}", this.corpusHome));
			} // if
			this.refresh(true); // 强制刷新
		}

		private final Set<String> keywords = new HashSet<String>();// 关键词集合
	}

	/**
	 * 检索文件：
	 *
	 * @param keyword 检索关键词
	 * @return 文件记录集合。
	 */
	public List<DataApp.IRecord> searchFiles(final String keyword) {
		final var srchEngine = new JdbcSrchEngine(this.indexHome) {
		};
		srchEngine.initialize();// 搜索引擎初始化
		return srchEngine.lookup(keyword, 50);
	}

	/**
	 * 文件遍历：对homeFile进行递归遍历:并行
	 *
	 * @param homeFile 起点文件位置
	 * @param cs       文件处理函数
	 */
	public static void traverse(final File homeFile, final Consumer<File> cs) {
		if (homeFile == null || !homeFile.exists()) {
			return;
		}

		if (homeFile.isFile()) {
			cs.accept(homeFile);
		} else if (homeFile.isDirectory()) {
			Arrays.stream(homeFile.listFiles()).parallel().forEach(f -> traverse(f, cs));
		}
	}

	/**
	 * 文件遍历：对homeFile进行递归遍历:非并行
	 *
	 * @param homeFile 起点文件位置
	 * @param cs       文件处理函数
	 */
	public static void traverse2(final File homeFile, final Consumer<File> cs) {
		if (homeFile == null || !homeFile.exists()) {
			return;
		}

		if (homeFile.isFile()) {
			cs.accept(homeFile);
		} else if (homeFile.isDirectory()) {
			Arrays.stream(homeFile.listFiles()).forEach(f -> traverse2(f, cs));
		}
	}

	/**
	 * 文件遍历：对homeFile进行递归遍历
	 *
	 * @param homeFile 起点文件位置
	 * @param cs       文件处理函数
	 */
	public static void traverse(final String homeFile, final Consumer<File> cs) {
		traverse(new File(homeFile), cs);
	}

	protected String indexHome;// 索引文件的的保存位置路径
	protected String corpusHome;// 语料库的词汇目录
	protected String snapHome;// 快照文件根目录
	protected Trie<String> corpus; // 语料库
	protected final ExecutorService executors = Executors.newFixedThreadPool(5);// 线程池
	public static boolean enableImgNameAsKeyword = true;// 是否把图片名称视为关键字,默认为true
	public static boolean debug = false;// 调试状态标记
}
