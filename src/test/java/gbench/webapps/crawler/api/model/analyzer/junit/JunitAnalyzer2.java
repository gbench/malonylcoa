package gbench.webapps.crawler.api.model.analyzer.junit;

import static gbench.util.io.Output.println;
import static java.text.MessageFormat.format;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.DFrame;
import gbench.util.data.DataApp.IRecord;
import gbench.util.data.xls.SimpleExcel;
import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.ILexProcessor;
import gbench.webapps.crawler.api.model.analyzer.lexer.Lexeme;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;

/**
 * 分词器
 */
public class JunitAnalyzer2 {

	/**
	 * corpusPath 语料库路径
	 *
	 * @param dfm 基础词库
	 * @return YuhuanAnalyzer
	 */
	public YuhuanAnalyzer buildYuhuanAnalyzer(final DFrame dfm) {
		final var corpus = new Trie<String>();// 语料库的根节点树

		dfm.rowS().forEach(line -> {
			final var word = line.str("WORD");
			final var meaning = word;
			Trie.addPoints2Trie(corpus, word.split("")) // 增加节点
					.addAttribute("category", "word") // 绑定词法类型信息
					.addAttribute("meaning", meaning); // 词义
		}); // forEach

		// 打印语料库
		Trie.traverse(corpus, e -> {
			final var line = "\t".repeat(e.getLevel()) + e.getValue() + //
					"\tcategory:" + e.getAttribute("category");
			System.out.println(line);
		}); // traverse

		@SuppressWarnings("resource")
		final var yuhuan = new YuhuanAnalyzer().addProcessor(new ILexProcessor() { // 创建一个 分词器
			/**
			 * 符号计算:一个成功的分词需要evaluate返回非空,否则分词器会返回最基础的单词符号
			 */
			@Override
			public Lexeme evaluate(final String symbol) { // 符号计算
				return corpus.opt(symbol.split("")).map(token -> { // 数据符号
					final var category = token.strAttr("category"); // 提取corpus词典分类属性
					final var meaning = token.strAttr("meaning"); // 提取corpus词典意义属性
					return new Lexeme(symbol, category, meaning); // 返回词素
				}).orElseGet(() -> { // 模式识别
					return predefs.tupleS() // 预定模式检测
							.filter(p -> symbol.matches(p._2.toString())) // 检索数据
							.findFirst().map(p -> new Lexeme(symbol, p._1, symbol) // 生成词素
									.addAttributes("class", "mode") // 补充属性
					).orElse(null); // 获取词意失败
				}); // opt
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

		});// addProcessor

		return yuhuan;
	}

	@Test
	public void foo() {
		final var home = "F:/slicef/ws/gitws/malonylcoa/src/test/";
		final var file = format("{0}/java/gbench/webapps/crawler/api/model/data/datafile.xlsx", home);
		final var excel = SimpleExcel.of(file);
		final var dfm = excel.autoDetect("t_wenyan").collect(DFrame.dfmclc2);
		final var yuhuan = this.buildYuhuanAnalyzer(dfm);

		yuhuan.analyzeS("何不按兵束甲，北面而事之？this is a 23,二零二三年,圣人之于天下也", e -> e).forEach(word -> {
			println(word, word.attrs(IRecord::REC));
		});

	}

}
