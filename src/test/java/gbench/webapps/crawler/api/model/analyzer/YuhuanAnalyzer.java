package gbench.webapps.crawler.api.model.analyzer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import gbench.webapps.crawler.api.model.analyzer.lexer.ILexProcessor;
import gbench.webapps.crawler.api.model.analyzer.lexer.LexemeAttribute;
import gbench.webapps.crawler.api.model.analyzer.lexer.StreamLexer;
import gbench.webapps.crawler.api.model.analyzer.lexer.AbstractLexer.TokenProfile;
import gbench.util.data.DataApp.IRecord;

import static gbench.util.data.DataApp.IRecord.*;

/**
 * 玉环分词器
 *
 * @author XUQINGHUA
 */
public class YuhuanAnalyzer extends Analyzer {

	/**
	 * 构造函数
	 */
	public YuhuanAnalyzer() {
		this.initialize();
	}

	/**
	 * 初始化
	 * 
	 * @return YuhuanAnalyzer 本身
	 */
	public YuhuanAnalyzer initialize() {
		yuhuanLexer = new StreamLexer();
		yuhuanLexer.initialize();
		return this;
	}

	/**
	 * 注销自己
	 * 
	 * @return YuhuanAnalyzer 本身
	 */
	public YuhuanAnalyzer uinitialize() {
		yuhuanLexer.uninitialize();
		return this;
	}

	/**
	 * 分词组件
	 *
	 * @param fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 */
	@SuppressWarnings("resource")
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {

		/***
		 * 注意Tokenizer 的生成是存在于对空间的，所以它的作用域不是存在于 createComponents
		 * 函数，也就是函数运行完之后对象依旧好好的存在的。
		 * <p>
		 * tokenizer 一个匿名内部类，这个写法对于临时雇佣一个无名的奴隶 对象太有效了,起个有意的名字还是停费脑子，不过这个对象还是很有用的，
		 * 就像楚乔似的。这个也是很有意思,层层包裹的方法
		 */
		final var tokenizer = new Tokenizer() {// 分的词是存放在tokenizer的属性里面的
			private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
			private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
			private final LexemeAttribute lexemeAttr = addAttribute(LexemeAttribute.class);
			private StreamLexer tokenLexer = null;// 流式分词器

			/**
			 * 这个incrementToken 很有特色的，主要原因就是它不是采用返回值 返回参数而是采用用自身的属性状态来返回参数，是一种很有趣的
			 * 技术。主要用在包装Wrapper方式进行结构组合。
			 */
			@Override
			public boolean incrementToken() throws IOException {
				if (this.tokenLexer == null) {
					System.err.println("analyzer 尚未设置,清 执行reset进行tokenizer初始化！");
					return false;
				}
				this.clearAttributes();
				TokenProfile word = null;// 词法单元结构。
				if ((word = this.tokenLexer.nextToken()) != null) {// 查询出有效的词法单元。
					termAttr.append(word.symbol);// 设置词法单元属性
					offsetAttr.setOffset(word.offset_start, word.offset_end);// 设置词法单元 空间位置。
					lexemeAttr.setLexeme(word.getLexeme());
					return true;// 成功获得词法单元信息。
				} else {
					this.tokenLexer.uninitialize();// 把模块分词器给注销掉-释放资源
					return false;
				} // if
			}//

			@Override
			public void reset() throws IOException {
				super.reset();
				// 初始化流式分词器。所以说玉环的方法要比昭君高明一点。
				tokenLexer = new StreamLexer(this.input);// 构造流式词法分词器。
				if (null != yuhuanLexer) {// 复制出 lexer: 把yuhuanLexer的各个处理器迁移到tokenLexer之中
					yuhuanLexer.getProcessors().forEach((k, v) -> tokenLexer.addProcessor(v));
				} // if
				tokenLexer.initialize();
			}//
		};// tokenizer 分词器

		// TokenStream 是从一个tokenizer 先切分出 粗产品：词法单元，然后 词法单元 再各个 filter的的的处理流 序列下 进行精细加工而后
		// 构成 token
		TokenStream tokenStream = new LowerCaseFilter(tokenizer);// 使用tokenizer把一个分词转换成小写字母
		final var stopWords = new CharArraySet(new LinkedList<String>(), false);// 不带有任何过滤的词
		// 层层包裹 tokenStream
		tokenStream = new StopFilter(tokenStream, stopWords);// 去除STOP WORDS 无意义的词儿。
		// tokenStream = new PorterStemFilter(tokenStream);// 获取词根

		// 生成一个TokenStream的组件结构：这个就像TokenStreamComponents 就像一个乐高的积木包,Analyzer 收到之后，可以自己
		// 拼装成 TokenStream
		return new TokenStreamComponents(tokenizer::setReader, tokenStream);
	}

	/**
	 * 分词
	 *
	 * @param line 数据行
	 * @return 分词明细 symbol,category,meaning,含有tags 集合标签
	 */
	public List<IRecord> analyze(final String line) {
		return this.analyzeS(line, YuhuanAnalyzer::token2rec).collect(Collectors.toList());
	}

	/**
	 * 分词
	 *
	 * @param <U>    结果类型
	 * @param line   数据行
	 * @param mapper TokenProfile -&gt;u 元素变换函数
	 * @return 分词明细 symbol,category,meaning,含有tags 集合标签
	 */
	public <U> Stream<U> analyzeS(final String line, final Function<TokenProfile, U> mapper) {
		if (yuhuanLexer == null) {
			yuhuanLexer = new StreamLexer();
		}
		final List<TokenProfile> words = yuhuanLexer.analyze(line);

		return words.stream().map(mapper);
	}

	/**
	 * 分词
	 *
	 * @param line 数据行
	 * @return 分词明细 symbol,category,meaning,含有tags 集合标签
	 */
	public Stream<IRecord> analyzeS(final String line) {
		return this.analyzeS(line, YuhuanAnalyzer::token2rec);
	}

	/**
	 * 自定义单词处理器
	 *
	 * @param processor 每个处理器对应一个特定的语料词库
	 */
	public YuhuanAnalyzer addProcessor(final ILexProcessor processor) {
		this.yuhuanLexer.addProcessor(processor);
		return this;
	}

	/**
	 * 这个属性对不是分词器来说不是必须，不过就像设计衣服的时候为 服装缝制个口袋，完全是为了便于 携带数据而做的设计而已。<br>
	 * 比如把 yuhuan作为参数传递时还需要附加一些关联数据的时候，可以省略一个调用导函数的参数位置。<br>
	 * 把这个数据写入到 attributes 中就可以了。<br>
	 * 这就是袋袋裤的设计思想的程序体现。<br>
	 * <p>
	 * 根据类型检索属性对象
	 *
	 * @param <T>   属性对象
	 * @param clazz 属性类型
	 * @return 属性值 或 null
	 */
	public <T> T findOne(final Class<T> clazz) {
		return (T) attributes.findOne(clazz);
	}

	/**
	 * 这个属性对不是分词器来说不是必须，不过就像设计衣服的时候为 服装缝制个口袋，完全是为了便于 携带数据而做的设计而已。<br>
	 * 比如把 yuhuan作为参数传递时还需要附加一些关联数据的时候，可以省略一个调用导函数的参数位置。<br>
	 * 把这个数据写入到 attributes 中就可以了。<br>
	 * 这就是袋袋裤的设计思想的程序体现。<br>
	 * <p>
	 * 获取对象属性
	 *
	 * @param name 属性名
	 * @return 属性值 或 null
	 */
	public Object getAttribute(final String name) {
		return attributes.get(name);
	}

	/**
	 * 设置属性,同名属性如果已经存在则给与覆盖
	 *
	 * @param name  属性名
	 * @param value 属性值
	 * @return YuhuanAnalyzer 对象本身，以实现链式编程
	 */
	public YuhuanAnalyzer setAttribute(final String name, final Object value) {
		this.attributes.set(name, value);
		return this;
	}

	/**
	 * TokenProfile 转换成IRecord 对象
	 *
	 * @param profile TokenProfile 符号内容
	 * @return {symbol:符号名,category:分类,meaning:意义,start:开始位置包含,end:结束位置包含,tags:标签}
	 */
	public static IRecord token2rec(final TokenProfile profile) {
		return REC("symbol", profile.symbol, "category", profile.category, "meaning", profile.meaning, "start",
				profile.offset_start, "end", profile.offset_end, "tags", profile.getTags());
	}

	/**
	 * 使用分词器进行分词 <br>
	 * 使用完后自动关闭 <br>
	 *
	 * @param analyzer  分词器
	 * @param fieldName 字段名 fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 * @param text      字段名 fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 */
	public static List<IRecord> splits(final Analyzer analyzer, final String fieldName, final String text) {
		return splits(analyzer, fieldName, text, true);
	}

	/**
	 * 使用分词器进行分词
	 *
	 * @param analyzer  分词器
	 * @param fieldName 字段名 fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 * @param text      待分词的字符串
	 * @param bclose    是否使用完后 给与关闭
	 */
	public static List<IRecord> splits(final Analyzer analyzer, final String fieldName, final String text,
			final boolean bclose) {
		final var ll = new LinkedList<IRecord>(); // 分词结果

		try {
			// 获得tokenStream对象
			// 第一个参数：域名，可以随便给一个
			// 第二个参数：要分析的文本内容
			final var tokenStream = analyzer.tokenStream(fieldName, text);
			// 添加一个引用，可以获得每个关键词
			final var charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			// 添加一个偏移量的引用，记录了关键词的开始位置以及结束位置
			final var offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
			// 将指针调整到列表的头部
			tokenStream.reset();

			// 遍历关键词列表，通过incrementToken方法判断列表是否结束
			while (tokenStream.incrementToken()) {
				final var rec = REC("start", offsetAttribute.startOffset(), "end", offsetAttribute.endOffset(), "value",
						charTermAttribute.toString());
				rec.add("type", rec.str("value").matches("[~!@#$%^&*\\(\\)\\s\\.。:;]+") ? "symbol" : "word");// 加入符号类型的
				ll.add(rec);
			}

			tokenStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		if (bclose)
			analyzer.close();

		return ll;
	}

	/**
	 * 使用分词器进行分词 <br>
	 * 使用完后自动关闭 <br>
	 *
	 * @param analyzer  分词器
	 * @param fieldName 字段名 fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 * @param text      字段名 fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 */
	public static Stream<IRecord> splitS(final Analyzer analyzer, final String fieldName, final String text) {
		return splitS(analyzer, fieldName, text, true);
	}

	/**
	 * 使用分词器进行分词
	 *
	 * @param analyzer  分词器
	 * @param fieldName 字段名 fieldName 组件对于使用同一个分词器进行分词，即 重用策略ReuseStrategy 具有标记意义。
	 * @param text      待分词的字符串
	 * @param bclose    是否使用完后 给与关闭
	 */
	public static Stream<IRecord> splitS(final Analyzer analyzer, final String fieldName, final String text,
			final boolean bclose) {
		return YuhuanAnalyzer.splits(analyzer, fieldName, text, bclose).stream();
	}

	private StreamLexer yuhuanLexer = null; // 静态服务的分析

	/**
	 * 这个属性对不是分词器来说不是必须，不过就像设计衣服的时候为 服装缝制个口袋，完全是为了便于 携带数据而做的设计而已。<br>
	 * 比如把 yuhuan作为参数传递时还需要附加一些关联数据的时候，可以省略一个调用导函数的参数位置。<br>
	 * 把这个数据写入到 attributes 中就可以了。<br>
	 * 这就是袋袋裤的设计思想的程序体现。<br>
	 */
	private final IRecord attributes = REC();// 分词器的属性状态，
}