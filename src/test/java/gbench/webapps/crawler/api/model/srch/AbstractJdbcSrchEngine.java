package gbench.webapps.crawler.api.model.srch;

import java.util.*;
import java.util.function.Consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollectorManager;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.ILexProcessor;
import gbench.webapps.crawler.api.model.analyzer.lexer.Lexeme;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;
import gbench.webapps.crawler.api.model.srch.PageData.Doc;
import gbench.util.chn.PinyinUtil;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.io.FileSystem;

import static java.text.MessageFormat.format;
import static gbench.webapps.crawler.api.model.srch.SrchUtils.*;
import static gbench.util.data.DataApp.Tuple2.*;
import static gbench.util.data.DataApp.IRecord.*;

/**
 * 简单的Jdbc式的数据访问方式的搜索引擎<br>
 * <p>
 * 称为 JdbcSeachEngine 是为了表示它提供了一个类似于 <br>
 * 关系型数据库的Jdbc式样的数据存取访问的接口。拥有一个 <br>
 * IndexJdbcSession 这样的会话访问对象。<br>
 *
 * @author gbench
 */
public abstract class AbstractJdbcSrchEngine {

	/**
	 * 搜索引擎初始化
	 */
	public AbstractJdbcSrchEngine initialize() {
		analyzer = buildYuhuanAnalyzer(this.corpusHome);
		return this;
	}

	/**
	 * 注销关键组件
	 */
	public AbstractJdbcSrchEngine uninitialize() {
		try {
			if (this.indexReader != null) {
				this.indexReader.close();
			} // if
			if (this.analyzer != null) {
				this.analyzer.close();
			} // if
			this.indexReader = null;
			this.analyzer = null;
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return this;
	}

	/**
	 * 关键字检索
	 *
	 * @param keyword     keyword 关键词检索
	 * @param hitsPerPage 放回列表的最大长度
	 * @return 与关键字匹配的产品列表, 如果没有匹配返回null
	 */
	public List<IRecord> lookup(final String keyword, final int hitsPerPage) {
		final Term term = new Term(SEARCH_FIELD, format("*{0}*", keyword));// 通配符查询
		final Query query = new WildcardQuery(term);

		return this.lookup(query, hitsPerPage);
	}

	/**
	 * 关键字检索
	 *
	 * @param query 关键词检索
	 * @return 与关键字匹配的产品列表, 如果没有匹配返回null
	 */
	public List<IRecord> lookup(final Query query) {
		return lookup(query, Integer.MAX_VALUE);
	}

	/**
	 * 关键字检索
	 *
	 * @param query       关键词检索
	 * @param hitsPerPage 放回列表的最大长度
	 * @return 与关键字匹配的产品列表, 如果没有匹配返回null
	 */
	public List<IRecord> lookup(final Query query, final int hitsPerPage) {
		List<IRecord> recs = new LinkedList<IRecord>();// 返回结构
		if (query != null) {
			try {
				final var home = getIndexHome();
				if (home.listAll().length <= 0) {
					System.out.println(String.format("%s索引文件目录内容为空,请先建立然后再检索", home.getDirectory()));
					return recs;
				} // 索引目录

				final var reader = this.getIndexReader();
				final var searcher = new IndexSearcher(reader);
				recs = doPagingSearch(searcher, query, hitsPerPage);
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		} else {
			System.err.println("query 为 null 不予查询");
		}

		return recs;
	}

	/**
	 * 关键字检索
	 *
	 * @param start       开始位置
	 * @param query       关键词检索
	 * @param hitsPerPage 放回列表的最大长度
	 * @return 与关键字匹配的产品列表, 如果没有匹配返回null
	 */
	public List<IRecord> lookup(int start, final Query query, final int hitsPerPage) {
		final List<IRecord> recs = new LinkedList<IRecord>();// 返回结构

		if (query != null) {
			try {
				final var home = getIndexHome();
				if (home.listAll().length <= 0) { // 索引目录
					System.out.println(String.format("%s索引文件目录内容为空,请先建立然后再检索", home.getDirectory()));
					return recs;
				} else { // 索引目录 有效
					final var reader = this.getIndexReader();
					final var searcher = new IndexSearcher(reader);
					recs.addAll(doPagingSearch(searcher, query, hitsPerPage));
				} //
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		} else {
			System.err.println("query 为 null 不予查询");
		} // if

		return recs;
	}

	/**
	 * 获取最后索引陌路的的更改时间
	 *
	 * @return 更改时间
	 */
	public long getLastModified() {
		long modified = 0;
		try {
			modified = getIndexHome().getDirectory().toFile().lastModified();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return modified;
	}

	/**
	 * 遍历一个 索引库 中的所有文档
	 *
	 * @return 文档流
	 */
	public Stream<Document> docStream() {
		return docStream(this.getIndexReader());
	}

	/**
	 * 遍历一个 索引库 中的所有文档
	 *
	 * @return 文档流
	 */
	public Stream<IRecord> docStream2() {
		return docStream(this.getIndexReader()).map(SrchUtils::doc2rec);
	}

	/**
	 * 设置存放索引文件位置
	 *
	 * @throws IOException
	 */
	public void setIndexHome(final String indexHome) {
		this.indexHome = indexHome;
	}

	/**
	 * 获得索引位置
	 *
	 * @return 索引存放位置
	 * @throws IOException
	 */
	public FSDirectory getIndexHome() {
		return getIndexHome(null);
	}

	/**
	 * 准备IndexWriterConfig的配置
	 *
	 * @param indexWriterConfigInitiator IndexWriterConfig 的准备调整回调
	 */
	public void prepareIndexWriter(Consumer<IndexWriterConfig> indexWriterConfigInitiator) {
		this.indexWriterConfigInitiator = indexWriterConfigInitiator;
	}

	/**
	 * 索引文件(indexedFile)的保存目录 根据 index_name 获取生成IndexHome, 如果index_name为空 则采用
	 * this.indexName
	 *
	 * @param indexPosition 索引文件位置：名称,也可以是一个绝对索引文件存储位置的绝对路径
	 * @return 索引文件的home 位置
	 * @throws IOException
	 */
	public FSDirectory getIndexHome(final String indexPosition) {
		final var indexName = indexPosition == null ? this.indexHome : indexPosition;// 索引位置名称
		FSDirectory home = null;
		try {
			final var path = FileSystem.path(indexName, this.getClass());
			home = // FSDirectory.open(Paths.get(path));
					MMapDirectory.open(Paths.get(path));
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return home;
	}

	/**
	 * 书写索引文件<br>
	 *
	 * @param cs 提供一个IndexWriter的回调函数,IndexWriter 不需要close 使用完由上级来给与关闭。
	 */
	public synchronized void writeIndexes(final Consumer<IndexWriter> cs) {
		this.writeIndexes(cs, writer -> {
			try {
				writer.forceMerge(1);// 默认合并成一个段
			} catch (IOException e) {
				e.printStackTrace();
			} // try
		});
	}

	/**
	 * 书写索引文件<br>
	 *
	 * @param primecs 提供一个IndexWriter的回调函数,IndexWriter 不需要close 使用完由上级来给与关闭。
	 * @param postcs  执行关闭前的收尾操作，比如 设置 writer.forceMerge(1) 之类的操作
	 */
	public synchronized void writeIndexes(final Consumer<IndexWriter> primecs, final Consumer<IndexWriter> postcs) {
		// 索引书写器的配置
		final var iwc = new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE_OR_APPEND);
		if (this.indexWriterConfigInitiator != null) {
			indexWriterConfigInitiator.accept(iwc);// 配置IndexWriterConfig
		} // if

		// 使用try 块自动关闭 writer
		try (final var writer = new IndexWriter(this.getIndexHome(), iwc);) {
			primecs.accept(writer);//
			postcs.accept(writer);
			@SuppressWarnings("unused")
			final var seqnum = writer.commit();
			// System.err.println(MessageFormat.format("commit seqnum:{0}", seqnum));
		} catch (Exception e) {
			e.printStackTrace();
		} // try
	}

	/**
	 * 书写索引文件<br>
	 *
	 * @param cs 提供一个IndexWriter的回调函数,IndexWriter 不需要close 使用完由上级来给与关闭。
	 */
	public synchronized void readIndexes(final Consumer<IndexReader> cs) {
		// 使用try 块自动关闭 writer
		try {
			cs.accept(this.getIndexReader());
		} catch (Exception e) {
			e.printStackTrace();
		} // try
	}

	/**
	 * 构造一个索引文件的编写会话：其实这个接口不是事物性质的，之所以叫做 withTransaction 是为了与 Jdbc的命名风格像一致<br>
	 *
	 * @param cs 构造了一个类SQL 的Jdbc操作的 API接口 会话
	 */
	public synchronized void withTransaction(final Consumer<IndexJdbcSession> cs) {
		// 写入索引信息
		this.writeIndexes(writer -> cs.accept(new IndexJdbcSession(writer)));
	}

	/**
	 * 对line 进行分词
	 *
	 * @param line 待分词的字符串
	 * @return 分词结果 [{symbol:单词符号,category:单词类型,meaning：单词解释,...}]
	 */
	public List<IRecord> analyze(final String line) {
		if (this.analyzer == null) {
			System.err.println("analyzer 为 null, 请执行 intialize 初始化 而后在在运行 分词");
			return null;
		} // if

		if (analyzer instanceof YuhuanAnalyzer yuhuan) {
			return yuhuan.analyze(line); // 使用玉环
		} else {
			final var recs = YuhuanAnalyzer.splits(analyzer, UUID.randomUUID().toString(), line, false);
			return recs.stream().map(
					rec -> rec.derive(REC("symbol", rec.get("value"), "category", rec.get("type"), "meaning", "-"))) // 复制属性
					.collect(Collectors.toList());
		} // if
	}

	/**
	 * 获取索引读取器
	 *
	 * @return IndexReader
	 */
	public <T extends IndexReader> T getIndexReader(final Class<T> clazz) {
		return corece(this.getIndexReader(), clazz);
	}

	/**
	 * 获取索引读取器
	 *
	 * @return IndexReader
	 */
	public IndexReader getIndexReader() {
		try {
			if (this.indexReader == null) {
				this.indexReader = DirectoryReader.open(this.getIndexHome());
			} else {
				// 如果 IndexReader 不为空，就使用 DirectoryReader 打开一个索引变更过的 IndexReader 类
				// 此时要记得把旧的索引对象关闭
				final IndexReader new_reader = DirectoryReader.openIfChanged((DirectoryReader) this.indexReader);
				if (new_reader != null) {
					this.indexReader.close();
					this.indexReader = new_reader;
				} // if
			} // if
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(MessageFormat.format("请仔细核对目录{0}是否为一个有效的索引文件路径:必须含有索引文件!", this.getIndexHome()));
		} // try

		return this.indexReader;
	}

	/**
	 * 生成一个按页查询的项目对象。 PageQuery 需要经过初始化之后才能够使用
	 *
	 * @param query 查询对象
	 * @return PageQuery
	 */
	public PageQuery getPageQuery(final Query query) {
		return this.getPageQuery(query, true);
	}

	/**
	 * 生成一个分页查询对象 PageQuery 需要经过初始化之后才能够使用
	 *
	 * @param query 查询对象
	 * @param binit 是否初始化
	 * @return PageQuery
	 */
	public PageQuery getPageQuery(final Query query, final boolean binit) {
		final var sbp = this.new PageQuery(this.getIndexReader(), query);
		if (binit) {
			sbp.initialize();
		} // if

		return sbp;
	}

	/**
	 * 返回一个分词器
	 *
	 * @return
	 */
	public Analyzer getAnalyzer() {
		return analyzer;
	}

	/**
	 * 把rec记录的指定col展开成多记录对象,增加拼音字段
	 *
	 * @param rec 记录对象
	 * @param col 展开的字段
	 * @return 文档集合流
	 */
	public Stream<Document> expand2docs(final IRecord rec, final String col) {
		final String name = rec.str(col);// 获取名称项目
		final List<String> pinyins = PinyinUtil.getPinyins(name);
		final List<String> names = new LinkedList<>();
		names.add(name);
		names.addAll(pinyins);
		return names.stream().map(e -> {
			IRecord r = rec.duplicate();
			r = r.add(SEARCH_FIELD, e);
			// System.out.println(r);
			return r;
		}).map(SrchUtils::rec2doc);
	}

	/**
	 * 索引会话
	 *
	 * @author gbench
	 */
	public class IndexJdbcSession {

		/**
		 * 构造函数
		 *
		 * @param writer 索引书写器
		 */
		public IndexJdbcSession(final IndexWriter writer) {
			this.writer = writer;
		}

		/**
		 * 添加文档<br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param docs 文档集合
		 */
		public void add(final Document[] docs, final boolean bcommit) {
			for (final var doc : docs) {
				try {
					this.writer.addDocument(doc);
				} catch (Exception e) {
					e.printStackTrace();
				} // try
			} // for
		}

		/**
		 * 添加文档<br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param recs    IRecord对象集合
		 * @param bcommit 石佛立即提交更新
		 */
		public void sqladd(final IRecord[] recs, final boolean bcommit) {
			final var docs = Stream.of(recs).map(SrchUtils::rec2doc).toArray(Document[]::new);
			this.add(docs, bcommit);
		}

		/**
		 * 添加文档<br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请手动调用 commit函数给予更新
		 *
		 * @param recs IRecord对象集合
		 */
		public void sqladd(final IRecord[] recs) {
			final var docs = Stream.of(recs).map(SrchUtils::rec2doc).toArray(Document[]::new);
			this.add(docs, false);
		}

		/**
		 * 添加文档<br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请手动调用 commit函数给予更新
		 *
		 * @param rec IRecord对象
		 */
		public void sqladd(final IRecord rec) {
			this.sqladd(new IRecord[] { rec });
		}

		/**
		 * 更新文档<br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param term    检索过滤条件
		 * @param doc     文档对象
		 * @param bcommit 是否手动提交
		 */
		public void update(final Term term, final Document doc, final boolean bcommit) {
			try {
				@SuppressWarnings("unused")
				long status = this.writer.updateDocument(term, doc);
				// System.out.println(MessageFormat.format("update status:{0}", status));
				if (bcommit) {
					this.commit();
				} // if
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		}

		/**
		 * 更新文档字段 <br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param term    检索过滤条件
		 * @param updates 变更字段
		 * @param bcommit 是否给与立即提交
		 */
		public void update2(final Term term, final Field[] updates, final boolean bcommit) {
			try {
				@SuppressWarnings("unused")
				long status = this.writer.updateDocValues(term, updates);
				// System.err.println(MessageFormat.format("update status:{0}", status));
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		}

		/**
		 * 更新文档 <br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param term    检索过滤条件
		 * @param rec     IRecord对象
		 * @param bcommit 是否给与立即提交
		 */
		public void sqlupdate(final Term term, final IRecord rec, boolean bcommit) {
			this.update(term, rec2doc(rec), bcommit);
		}

		/**
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请手动调用 commit函数给予更新
		 *
		 * @param term 检索过滤条件
		 * @param rec  IRecord对象
		 */
		public void sqlupdate(final Term term, final IRecord rec) {
			this.sqlupdate(term, rec, false);
		}

		/**
		 * 删除文档 <br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param terms   查找条件项目
		 * @param bcommit 是否给与立即提交
		 */
		public void remove(final Term[] terms, final boolean bcommit) {
			try {
				this.writer.deleteDocuments(terms);
				if (bcommit) {
					this.commit();
				} // if
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		}

		/**
		 * 删除文档 <br>
		 * <p>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请设置bcommit 为true
		 *
		 * @param rec     term 的集合。
		 * @param bcommit 是否给与立即提交
		 */
		public void sqlremove(final IRecord rec, boolean bcommit) {
			this.remove(rec2terms(rec).toArray(Term[]::new), bcommit);
		}

		/**
		 * 删除文档 <br>
		 * 对于在同一个 IndexJdbcSession中的跟随于 本操作后 之后的数据操作,比如(query,update,remove)<br>
		 * 如果需要查看到最新做出的数据变化, 请手动调用 commit函数给予更新
		 *
		 * @param rec term的集合。
		 */
		public void sqlremove(final IRecord rec) {
			this.remove(rec2terms(rec).toArray(Term[]::new), false);
		}

		/**
		 * 提交变更：错误序列号为-1
		 *
		 * @return 提交的操作序列号
		 */
		public long commit() {
			try {
				this.writer.flush();
				return this.writer.commit();
			} catch (IOException e) {
				e.printStackTrace();
			} // if

			return -1;
		}

		/**
		 * 数据查询
		 *
		 * @param rec  查询定义
		 * @param size 返回最大长度
		 */
		public List<IRecord> boolean_query(final IRecord rec, final int size) {
			return AbstractJdbcSrchEngine.this.lookup(parseDsl(rec), size);
		}

		/**
		 * 数据查询
		 *
		 * @param query 长训对象
		 * @param size  size 返回最大长度
		 */
		public List<IRecord> get(final Query query, final int size) {
			return AbstractJdbcSrchEngine.this.lookup(query, size);

		}

		/**
		 * 数据查询 查询多条数据
		 *
		 * @param rec 查询定义
		 */
		public List<IRecord> sqlgets(final IRecord rec) {
			return this.boolean_query(rec, Integer.MAX_VALUE);
		}

		/**
		 * 数据查询：只获取一条数据记录
		 *
		 * @param rec 查询定义
		 */
		public IRecord sqlget1(final IRecord rec) {
			final var r = AbstractJdbcSrchEngine.this.lookup(parseDsl(rec), 1);
			return r == null || r.size() < 1 ? null : r.get(0);
		}

		/**
		 * 正价属性状态
		 *
		 * @param name  属性状态
		 * @param value 属性状态值
		 */
		public void addAttribute(final String name, final Object value) {
			this.attributes.add(name, value);
		}

		/**
		 * 获取属性状态
		 *
		 * @param name 属性名
		 */
		public Object getAttribute(final String name) {
			return this.attributes.get(name);
		}

		/**
		 * 获取属性状态
		 *
		 * @return 属性状态
		 */
		public IRecord getAttributes() {
			return this.attributes;
		}

		/**
		 * 返回索引写入器
		 *
		 * @return indexWriter
		 */
		public IndexWriter getWriter() {
			return this.writer;
		}

		/**
		 * 需要注意 reader 共享SrchEngine的IndexReader。 使用完后是否关闭要看具体事情情况
		 * AbstractJdbcSrchEngine来决定。一般推荐由 outer 即 AbstractJdbcSrchEngine.this 来处理<br>
		 * withTransaction 中不可以不予理会。
		 *
		 * @return indexReader
		 */
		public IndexReader getReader() {
			return AbstractJdbcSrchEngine.this.getIndexReader();
		}

		private final IndexWriter writer;
		private final IRecord attributes = REC();// 会话中的状态数据

	}// SimpleIndexWriter

	/**
	 * 按照页进行搜索:<br>
	 * 从
	 * https://github.com/DmitryKey/luke/blob/master/src/main/java/org/apache/lucene/luke/models/search/SearchImpl.java
	 * <br>
	 * 抄写了部分代码。 但是做了 大量的 更改 <br>
	 * <p>
	 * 页面查询
	 *
	 * @author gbench
	 */
	public class PageQuery {

		/**
		 * 构造函数
		 *
		 * @param reader
		 */
		public PageQuery(final IndexReader reader, final Query query) {
			this.reader = reader;
			this.searcher = new IndexSearcher(reader);
			this.query = Objects.requireNonNull(query);
		}

		/**
		 * 初始化准备
		 *
		 * @param pageSize       页面中的数据条目数
		 * @param sort           排序方式
		 * @param fieldsToLoad   文档的字段集合
		 * @param exactHitsCount 是否返回所有符合条件的数据数量。
		 */
		public PageQuery initialize(final int pageSize, final Sort sort, final Collection<String> fieldsToLoad,
				final boolean exactHitsCount) {
			if (pageSize < 0) {
				throw new PageQueryException(
						new IllegalArgumentException("Negative integer is not acceptable for page size."));
			} // if

			// reset internal status to prepare for a new search session
			this.docs = new ScoreDoc[0];
			this.currentPage = 0;
			this.pageSize = pageSize;
			this.exactHitsCount = exactHitsCount;
			this.sort = sort == null ? Sort.INDEXORDER : sort;
			this.fieldsToLoad = fieldsToLoad == null ? null : Collections.unmodifiableSet(new HashSet<>(fieldsToLoad));

			return this;
		}

		/**
		 * 指定 fieldsToLoad 进行初始化
		 *
		 * @param pageSize     页面尺寸
		 * @param fieldsToLoad 文档的字段集合
		 */
		public PageQuery initialize(final Integer pageSize, final Collection<String> fieldsToLoad) {
			return this.initialize(pageSize == null ? PAGEQUEY_DEFAULT_PAGE_SIZE : pageSize, Sort.INDEXORDER,
					fieldsToLoad, true);
		}

		/**
		 * 默认初始
		 *
		 * @param pageSize 页面尺寸
		 */
		public PageQuery initialize(final Integer pageSize) {
			return this.initialize(pageSize, null);
		}

		/**
		 * 默认初始
		 *
		 * @param fieldsToLoad 文档中的显示字段集合
		 */
		public PageQuery initialize(final Collection<String> fieldsToLoad) {
			return this.initialize(null, fieldsToLoad);
		}

		/**
		 * 默认初始
		 */
		public PageQuery initialize() {
			return this.initialize(null, null);
		}

		/**
		 * 数据查询<br>
		 * 每执行一次数据查询请求:每调用一次都会发送一次 pageSize 大小的数据请求，直到请求到最后一条匹配数据
		 *
		 * @return 查询的返回数据
		 * @throws IOException
		 */
		public Optional<PageData> getData() throws IOException {
			// execute search
			final ScoreDoc after = docs.length == 0 ? null : docs[docs.length - 1];
			final TopDocs topDocs;

			if (sort != null) {
				topDocs = searcher.searchAfter(after, query, pageSize, sort);
			} else {
				final int hitsThreshold = exactHitsCount ? Integer.MAX_VALUE : PAGEQUEY_DEFAULT_TOTAL_HITS_THRESHOLD; // 是否返回精准的匹配数量
				final TopScoreDocCollectorManager tsdcm = new TopScoreDocCollectorManager(pageSize, after,
						hitsThreshold);
				topDocs = searcher.search(query, tsdcm);
			}

			// reset total hits for the current query
			this.totalHits = topDocs.totalHits;

			// cache search results for later use
			final ScoreDoc[] newDocs = new ScoreDoc[docs.length + topDocs.scoreDocs.length];
			System.arraycopy(docs, 0, newDocs, 0, docs.length);
			System.arraycopy(topDocs.scoreDocs, 0, newDocs, docs.length, topDocs.scoreDocs.length);
			this.docs = newDocs;

			return Optional.ofNullable(
					PageData.of(topDocs.totalHits, topDocs.scoreDocs, currentPage * pageSize, searcher, fieldsToLoad));
		}

		/**
		 * 不抛出异常版本的getData<br>
		 * 每执行一次数据查询请求:每调用一次都会发送一次 pageSize 大小的数据请求，直到请求到最后一条匹配数据<br>
		 *
		 * @return Optional HitsData
		 */
		public Optional<PageData> getData2() {
			Optional<PageData> opt = Optional.empty();
			try {
				opt = this.getData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return opt;
		}

		/**
		 * 下一页
		 *
		 * @return Optional PageData
		 */
		public Optional<PageData> nextPage() throws Exception {
			if ((this.docs.length == 0) && (currentPage < 0 || query == null)) {
				throw new PageQueryException(new IllegalStateException("Search session not started."));
			}

			// proceed to next page
			currentPage += 1;

			if (totalHits.value() == 0 || (totalHits.relation() == TotalHits.Relation.EQUAL_TO
					&& currentPage * pageSize >= totalHits.value())) {
				log("No more next search results are available.");
				return Optional.empty();
			}

			try {
				if (currentPage * pageSize < docs.length) {
					// if cached results exist, return that.
					final int from = currentPage * pageSize;
					final int to = Math.min(from + pageSize, docs.length);
					final ScoreDoc[] part = Arrays.copyOfRange(docs, from, to);
					return Optional.of(PageData.of(totalHits, part, from, searcher, fieldsToLoad));
				} else {
					return getData();
				} // if
			} catch (IOException e) {
				throw new PageQueryException("Search Failed.", e);
			}
		}

		/**
		 * 不抛出异常版本的nextPageNoThrow
		 *
		 * @return Optional HitsData
		 */
		public Optional<PageData> nextPageNoThrow() {
			Optional<PageData> opt = Optional.empty();
			try {
				opt = this.nextPage();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return opt;
		}

		/**
		 * 向前滚动一页<br>
		 * 注意 一直向前当到达第一个页时，再执行prevPage会把 currentPage 减成-1, 所以如果此时 需要后项操作，请
		 * resetCurrentPage 来设置到第0页位置
		 *
		 * @return Optional HitsData
		 * @throws Exception
		 */
		public Optional<PageData> prevPage() throws Exception {
			if (currentPage < 0 || query == null) {
				throw new Exception(new IllegalStateException("Search session not started."));
			} // if

			// return to previous page
			currentPage -= 1;

			if (currentPage < 0) {
				System.err.println("No more previous search results are available.");
				return Optional.empty();
			} // if

			try {
				// there should be cached results for this page
				final int from = currentPage * pageSize;
				final int to = Math.min(from + pageSize, docs.length);
				final ScoreDoc[] part = Arrays.copyOfRange(docs, from, to);
				return Optional.of(PageData.of(totalHits, part, from, searcher, fieldsToLoad));
			} catch (IOException e) {
				throw new PageQueryException("Search Failed.", e);
			} // try
		}

		/**
		 * 不抛出异常版本的prevPageNoThrow 注意 一直向前当到达第一个页时，再执行prevPage会把 currentPage 减成-1,
		 * 所以如果此时 需要后项操作，请 resetCurrentPage 来设置到第0页位置
		 *
		 * @return Optional PageData
		 */
		public Optional<PageData> prevPageNoThrow() {
			Optional<PageData> opt = Optional.empty();

			try {
				opt = this.prevPage();
			} catch (Exception e) {
				e.printStackTrace();
			} // try

			return opt;
		}

		/**
		 * 后项目流
		 *
		 * @return IRecord流
		 */
		public Stream<IRecord> hitsStream() {
			return Stream
					.iterate(this.docs.length == 0 ? this.getData2() : this.nextPageNoThrow(),
							i -> this.nextPageNoThrow())
					.takeWhile(opt -> !opt.isEmpty()).flatMap(opt -> opt.map(hd -> hd.hitsStream()).get());
		} // hitsStream

		/**
		 * 前项目流
		 *
		 * @return IRecord 流
		 */
		public Stream<IRecord> hitsStream2() {
			return Stream.iterate(this.docs.length == 0 ? this.getData2() : this.prevPageNoThrow(),
					i -> this.prevPageNoThrow()).takeWhile(opt -> !opt.isEmpty()).flatMap(opt -> opt.map(hd -> {// 把hd中的文档给予倒序过来。
						final Collector<Doc, LinkedList<Doc>, Stream<IRecord>> clc = Collector
								.of(() -> new LinkedList<Doc>(), (aa, a) -> aa.addFirst(a), (aa, bb) -> {
									aa.addAll(bb);
									return aa;
								}, aa -> aa.stream().map(Doc::record)); // clc
						return hd.getHits().stream().collect(clc);// 返回 Stream<IRecord>
					}).get());
		}// hitsStream2

		/**
		 * Wrapper exception class to convert checked exceptions to runtime exceptions.
		 *
		 * @author gbench
		 */
		public class PageQueryException extends RuntimeException {

			public PageQueryException(String message, Throwable cause) {
				super(message, cause);
			}

			public PageQueryException(Throwable cause) {
				super(cause);
			}

			public PageQueryException(String message) {
				super(message);
			}

			private static final long serialVersionUID = 1L;

		} // class PageQueryException

		public int getPageSize() {
			return pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public Set<String> getFieldsToLoad() {
			return fieldsToLoad;
		}

		public void setFieldsToLoad(Set<String> fieldsToLoad) {
			this.fieldsToLoad = fieldsToLoad;
		}

		public int getCurrentPage() {
			return currentPage;
		}

		public ScoreDoc[] getDocs() {
			return docs;
		}

		public TotalHits getTotalHits() {
			return totalHits;
		}

		public boolean isExactHitsCount() {
			return exactHitsCount;
		}

		public Sort getSort() {
			return sort;
		}

		/**
		 * @return IndexSearcher
		 */
		public IndexSearcher getSearcher() {
			return searcher;
		}

		/**
		 * @return IndexReader
		 */
		public IndexReader getReader() {
			return reader;
		}

		public String toString() {
			return format("query:[ {0} ],size:{1},currentPage:{2}", this.query, pageSize, currentPage);
		}

		private int pageSize = PAGEQUEY_DEFAULT_PAGE_SIZE; // 每一页中的数据条目数
		private int currentPage = -1; // 当前页号 从0开始。 0 表示无效页号
		private ScoreDoc[] docs = new ScoreDoc[0];
		private TotalHits totalHits; // 匹配数据的数量
		private boolean exactHitsCount; // 是否返回精准的匹配数量， true 返回所有的符合条件的 记录数量，否则 返回 DEFAULT_TOTAL_HITS_THRESHOLD 所定义的数量
		private final Query query; // 数据请求
		private Sort sort;// 排序方式
		private Set<String> fieldsToLoad; // 返回文档中的 字段集合。
		private final IndexSearcher searcher; // 查询器
		private final IndexReader reader;// 索引访问器
	}

	/**
	 * 字符数组工具类
	 *
	 * @author gbench
	 */
	public static class ByteArrayUtils {

		/**
		 * 把一个对象转换成 bytes 数据
		 *
		 * @param <T>
		 * @param obj 待转换的对象
		 * @return Optional 的 byte[] 结构
		 */
		public static <T> Optional<byte[]> objectToBytes(final T obj) {
			byte[] bytes = null;

			try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
					final ObjectOutputStream oos = new ObjectOutputStream(out);) {
				oos.writeObject(obj);
				oos.flush();
				bytes = out.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			} // try

			return Optional.ofNullable(bytes);
		}

		/**
		 * @param <T>
		 * @param bytes
		 * @return Optional T 结构
		 */
		@SuppressWarnings("unchecked")
		public static <T> Optional<T> bytesToObject(final byte[] bytes) {
			T t = null;

			try (final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
					final ObjectInputStream ois = new ObjectInputStream(in)) {
				t = (T) ois.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			} // try

			return Optional.ofNullable(t);
		}// bytesToObject

	} // class ByteArrayUtils

	/**
	 * 类型装换
	 *
	 * @param <T>
	 * @param obj   被转换的对象
	 * @param clazz 目标类型
	 * @return T类型的结果
	 */
	@SuppressWarnings("unchecked")
	public static <T> T corece(final Object obj, final Class<T> clazz) {
		if (clazz.isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			return null;
		} // if
	}

	/**
	 * 当仅使用YuhuanAnalyzer的 特有分词函数的时候，可以不对 YuhuanAnalyzer进行关闭。
	 *
	 * @param corpusDir 语料库路径
	 * @return YuhuanAnalyzer 返回的YuhuanAnalyzer中包含一个属性corpus 用于保存 分词器所使用的语料信息(词汇表)
	 */
	public static YuhuanAnalyzer buildYuhuanAnalyzer(final String corpusDir) {
		final var corpus = new Trie<String>();// 语料库的根节点树
		final var processorTrieTuples = new LinkedList<Tuple2<String, Trie<String>>>(); // Trie 的资料集合

		if (corpusDir != null) {// 遍历 corpusDir 获取 fileTries
			final var corpusHome = new File(corpusDir);// 语料库的路径
			final var processorCorpuses = corpusHome.isDirectory() // 构建语料库
					? Stream.of(corpusHome.listFiles())
					: Stream.of(new File(FileSystem.path(corpusDir, null)));

			processorCorpuses // 文件分词
					.filter(file -> file.getAbsolutePath().endsWith(".txt")).forEach(file -> { // 创建语料文件
						final var processorTrie = new Trie<String>();// 语料库的根节点树
						final var name = file.getName();
						final var idx = name.indexOf(".");
						final var endIndex = idx == -1 ? idx : file.getName().length();
						final var fname = name.substring(0, endIndex); // 提取文件名

						FileSystem.readLineS(file).filter(line -> !line.matches("^\\s*$")) // 去除空行
								.forEach(line -> {// 提取语料词汇
									Stream.of(line.strip().split("[\n]+")) // 按行进行切分
											.forEach(keyword -> {// 设置词素 的节点属性信息
												final var points = keyword.split("");// 把keyword 切分乘字符点

												Stream.of(processorTrie, corpus).forEach(trie -> {// 为trie 增加
													Trie.addPoints2Trie(trie, points).addAttribute("category", "word")// 绑定词法类型信息
															.addAttribute("meaning",
																	Arrays.stream(points).collect(Collectors.joining()))
															.addAttribute("tag", fname);
												});// Stream.of
											}); // forEach : keyword
								}); // forEach : line
						processorTrieTuples.add(TUP2(fname, processorTrie)); // fileTries
					}); // forEach : file
		} // if corpusDir

		if (debug) { // 打印语料库
			Trie.traverse(corpus, e -> System.out.println("\t".repeat(e.getLevel()) + e.getValue() + //
					"\ttype:" + e.getAttribute("type")));
		} // if

		final var yuhuan = new YuhuanAnalyzer(); // 生成玉环的分词器
		final var symbolProcessor = new ILexProcessor() { // 创建一个符号处理器

			/**
			 * 分词词库的名称
			 *
			 * @return 分词词库的名称
			 */
			@Override
			public String getName() {
				return "符号与数字";
			}

			/**
			 * 符号计算:一个成功的分词需要evaluate返回非空,否则分词器会返回最基础的单词符号
			 */
			@Override
			public Lexeme evaluate(final String symbol) { // 符号计算
				return predefs.tupleS() // 预定模式检测
						.filter(p -> symbol.matches(String.valueOf(p._2))) // 检索数据
						.findFirst().map(p -> new Lexeme(symbol, p._1, symbol) // 生成词素
								.addTags(this.getName(), p._1) // 增加词素标签,由于标记语料库来源
								.addAttributes("mode", "pattern") // 补充属性表示这是通过模式匹配尽心识别的
				).orElse(null); // 获取词意失败,表示该symbolProcessor识别不了，将交由其他的分词器其进行处理
			}

			@Override
			public boolean isPrefix(final String line) {
				if (predefs.valueS().map(Object::toString).anyMatch(line::matches)) {
					return true;
				} else {
					return corpus.isPrefix(line.split(""));
				} // if
			} // isPrefix

			/**
			 * 预定义模式
			 */
			private final IRecord predefs = IRecord.REC( // 预定义符号
					"WORD", "[a-zA-Z]+" // 英文单词
					, "INDENTIFIER", "[a-zA-Z_-]+" // 英文标识符号
					, "NUMBER", "[\\d\\.]+" // 阿拉伯数据
					, "CN_NUMBER", "[一二三四五六七八九十零百千万亿兆]+" // 中文数字
					, "CN_DATETIME", "[一二三四五六七八九十零百千万亿兆\\d]+年([一二三四五六七八九十零百千万亿兆\\d+](月([一二三四五六七八九十零百千万亿兆\\d+]日?)?)?)?" // 日期时间
					, "PUNCT", "[-,+*/，。、]" // 标点符号
			); // 预定义模式
		};// 符号处理器

		// 符号与关键词的处理器
		yuhuan.addProcessor(symbolProcessor);
		// 为yuhuan 添加分词processor
		processorTrieTuples.forEach(processorTrieTuple -> {// 每个分词器带有一个 processorCorpus 是一个特异的 专业词库

			// 构造分词器的 处理器
			final var processor = new ILexProcessor() { // 创建一个 分词器
				final Trie<String> processorCorpus = processorTrieTuple._2;// 私有处理的词库

				/**
				 * 分词词库的名称
				 *
				 * @return 分词词库的名称
				 */
				@Override
				public String getName() {
					final var line = processorTrieTuple._1;
					final var idx = line.indexOf(".");
					final var endIndex = idx < 0 ? line.length() : idx;
					return line.substring(0, endIndex);// 去除扩展名
				}

				/**
				 * 把符号转换成 词素
				 *
				 * @param symbol 符号
				 * @return 词素
				 */
				@Override
				public Lexeme evaluate(final String symbol) {
					final var trieNode = processorCorpus.getTrie(symbol.split(""));// 提取节点词素
					if (trieNode == null) {
						return null;
					} // if
					if (!Objects.equals("word", trieNode.getAttribute("category"))) {
						return null;
					} // if

					final var category = trieNode.strAttr("category");// 提取分类属性
					final var meaning = trieNode.strAttr("meaning");// 提取意义属性

					return new Lexeme(symbol, category, meaning) //
							.addTags(this.getName()); // 增加词素标签,由于标记语料库来源
				}

				/**
				 * 符号检测
				 *
				 * @param line 符号字符串
				 * @return 是否是一个符号前缀
				 */
				@Override
				public boolean isPrefix(final String line) {
					return processorCorpus.isPrefix(line.split(""));
				}// isPrefix
			};

			yuhuan.addProcessor(processor);// addProcessor
		});

		// 设置玉环的 分此次词库
		yuhuan.setAttribute("corpus", corpus);// 设置词库竖向

		return yuhuan;
	}

	/**
	 * 分页检索
	 *
	 * @param searcher    搜索器
	 * @param query       查询对象
	 * @param hitsPerPage 每页的
	 * @throws IOException
	 */
	public static List<IRecord> doPagingSearch(final IndexSearcher searcher, final Query query, final int hitsPerPage)
			throws IOException {
		return doPagingSearch(null, searcher, query, hitsPerPage, Sort.INDEXORDER);
	}

	/**
	 * 分页检索
	 * 
	 * @param after       前一文档
	 * @param searcher    搜索器
	 * @param query       查询对象
	 * @param hitsPerPage 每页的数量
	 * @param sort        排序
	 * @return 文档结构
	 * @throws IOException
	 */
	public static List<IRecord> doPagingSearch(final ScoreDoc after, final IndexSearcher searcher, final Query query,
			final int hitsPerPage, final Sort sort) throws IOException {
		return Arrays.stream(AbstractJdbcSrchEngine.search(after, searcher, query, hitsPerPage, sort)).map(h -> {
			try {
				return searcher.storedFields().document(h.doc);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return null;
		}).map(e -> doc2rec(e)).collect(Collectors.toList());
	}

	/**
	 * 普通查询
	 *
	 * @param after       前一文档
	 * @param searcher    搜索器
	 * @param query       查询对象
	 * @param hitsPerPage 每页的数量
	 * @param sort        排序
	 * @throws IOException
	 */
	public static ScoreDoc[] search(final ScoreDoc after, final IndexSearcher searcher, final Query query,
			final int hitsPerPage, final Sort sort) throws IOException {
		final TopDocs results = after == null //
				? searcher.search(query, hitsPerPage, sort)
				: searcher.searchAfter(after, query, hitsPerPage, sort);
		final ScoreDoc[] hits = results.scoreDocs;
		return hits;
	}

	/**
	 * 遍历一个 索引库 中的所有文档
	 *
	 * @param indexReader 索引库的 访问接口
	 * @return 文档流
	 */
	public static Stream<Document> docStream(final IndexReader indexReader) {
		return Stream.iterate(0, i -> i + 1).limit(indexReader.maxDoc()).map(docID -> {
			Document doc = null;
			try {
				doc = indexReader.storedFields().document(docID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return doc;
		}).filter(e -> e != null);
	}

	/**
	 * 遍历一个 索引库 中的所有文档
	 *
	 * @param indexReader 索引库的 访问接口
	 * @return 文档流
	 */
	public static Stream<IRecord> docStream2(final IndexReader indexReader) {
		return docStream(indexReader).map(SrchUtils::doc2rec);
	}

	/**
	 * Md5 加密
	 *
	 * @param object 待加密的数据对象
	 * @return Md5 加密 串
	 */
	public static String md5(final Object object) {
		byte[] secretBytes = null;
		try {
			final var bb = object instanceof String s ? s.getBytes() : ByteArrayUtils.objectToBytes(object).get();
			secretBytes = MessageDigest.getInstance("md5").digest(bb);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("没有这个md5算法！");
		}

		String md5code = new BigInteger(1, secretBytes).toString(16);
		for (int i = 0; i < 32 - md5code.length(); i++) {
			md5code = "0" + md5code;
		} // for

		return md5code;
	}

	/**
	 * 提取一个文档的fld的字段的字符串的值
	 *
	 * @param doc 索引记录文档
	 * @param fld 字段名称
	 * @return fld的字段的二进制形式的值
	 */
	public static String strfld(final Document doc, final String fld) {
		return doc.getField(fld).stringValue();
	}

	/**
	 * 提取一个文档的fld的字段的二进制形式的值
	 *
	 * @param doc 索引记录文档
	 * @param fld 字段名称
	 * @return fld的字段的二进制形式的值
	 */
	public static BytesRef binfld(final Document doc, final String fld) {
		return doc.getField(fld).binaryValue();
	}

	/**
	 * 提取一个文档的fld的字段的记录类型
	 *
	 * @param doc 索引记录文档
	 * @param fld 字段名称
	 * @return fld的字段的记录类型
	 */
	public static IndexableFieldType typefld(final Document doc, final String fld) {
		return doc.getField(fld).fieldType();
	}

	/**
	 * 生成一个 字段项目
	 *
	 * @param name  字段项目的名称
	 * @param value 字段项目的值
	 * @return 字段项目
	 */
	public static Term T(final String name, final Object value) {
		return new Term(name, value + "");
	}

	/**
	 * 生成一个索引记录文档对象
	 *
	 * @param fields 字段,值序列:field0,value0,field1,value1,....
	 * @return 索引记录文档对象
	 */
	public static Document D(Object... fields) {
		return rec2doc(REC(fields));
	}

	/**
	 * 日志输出
	 *
	 * @param message
	 */
	public static void log(Object message) {
		System.err.print(message);
	}

	public static boolean debug = false; // 调试标记

	private IndexReader indexReader = null; // 索引库的访问接口

	protected final String SEARCH_FIELD = "search_field";// 检索的字段项目
	protected Analyzer analyzer = null;// 分析器
	protected String indexHome = "indexes";// 索引文件位置名称或者目录
	protected String corpusHome;// 语料库的词汇目录,一般由子类给赋值 AbstractJdbcSrchEngine 仅提供一个占用/引用位置。这是一种叫做模板方法 的模式在
	// 属性成员中的应用.
	protected Consumer<IndexWriterConfig> indexWriterConfigInitiator = iwc -> {
	};

	// 默认分页尺寸大小 写成大写式表示 这是作为 PageQuery的类的默认值
	protected final Integer PAGEQUEY_DEFAULT_PAGE_SIZE = 10;
	// 默认的最大查询结果数量,超过这个数量值 就不再继续向后查询了。写成大写式表示这是作为PageQuery的类的默认值
	protected final Integer PAGEQUEY_DEFAULT_TOTAL_HITS_THRESHOLD = 10000;
}