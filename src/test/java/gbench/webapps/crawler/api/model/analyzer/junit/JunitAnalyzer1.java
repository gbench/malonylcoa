package gbench.webapps.crawler.api.model.analyzer.junit;

import org.junit.jupiter.api.Test;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.ILexProcessor;
import gbench.webapps.crawler.api.model.analyzer.lexer.Lexeme;
import gbench.webapps.crawler.api.model.analyzer.lexer.LexemeAttribute;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.data.DataApp.IRecord;
import gbench.util.io.FileSystem;

import static gbench.util.data.DataApp.IRecord.*;
import static gbench.util.io.FileSystem.readLineS;

/**
 * @author gbench
 */
public class JunitAnalyzer1 {

	/**
	 * @param analyzer
	 * @param fieldName
	 * @param text
	 * @throws Exception
	 */
	public List<IRecord> splits(Analyzer analyzer, String fieldName, String text) {
		final var ll = new LinkedList<IRecord>();
		try {
			final var tokenStream = analyzer.tokenStream(fieldName, text); // 获得tokenStream对象
			final var charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class); // 添加一个引用，可以获得每个关键词
			final var offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class); // 添加一个偏移量的引用，记录了关键词的开始位置以及结束位置
			final var lexemeAttribute = tokenStream.addAttribute(LexemeAttribute.class); // 词素信息

			// 将指针调整到列表的头部
			tokenStream.reset();

			// 遍历关键词列表，通过incrementToken方法判断列表是否结束
			while (tokenStream.incrementToken()) {
				final var category = lexemeAttribute.getLexeme().getCategory();
				final var value = charTermAttribute.toString();
				final var rec = REC(// 词法结构信息
						"start", offsetAttribute.startOffset(), "end", offsetAttribute.endOffset(), "value", value,
						"category",
						category == null
								? value.matches("[~!@#$%^&*\\(\\)\\s\\.。，、:;“”； …\\+《》： ？]+") ? "symbol" : "word"
								: category);// rec
				ll.add(rec);
			} // while

			tokenStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		analyzer.close();
		return ll;
	}

	/**
	 * corpusPath 语料库路径
	 *
	 * @param corpusDir
	 * @return YuhuanAnalyzer
	 */
	public YuhuanAnalyzer getYunHuan(final String corpusDir) {
		final var trie = new Trie<String>();// 语料库的根节点树
		final var corpusHome = new File(corpusDir); // 语料库路径

		// 构建语料库
		(corpusHome.isDirectory() //
				? Stream.of(corpusHome.listFiles())
				: Stream.of(new File(FileSystem.path(corpusDir, this.getClass()))) //
		).filter(file -> file.getAbsolutePath().endsWith(".txt")).forEach(file -> {// 创建语料文件
			readLineS(file).filter(line -> !line.matches("^\\s*$")) // 去除空行
					.forEach(e -> {// 提取语料词汇
						Stream.of(e.strip().split("[\n]+")).map(p -> p.split("")).forEach(points -> {// 设置词素的节点属性信息
							final var meaning = Arrays.stream(points).collect(Collectors.joining());
							Trie.addPoints2Trie(trie, points) // 增加节点
									.addAttribute("category", "word") // 绑定词法类型信息
									.addAttribute("meaning", meaning); // 词义
						}); // forEach
					});// forEach
		});

		// 打印语料库
		Trie.traverse(trie, e -> {
			final var line = "\t".repeat(e.getLevel()) + e.getValue() + //
					"\ttype:" + e.getAttribute("type");
			System.out.println(line);
		}); // traverse

		@SuppressWarnings("resource")
		final var yuhuan = new YuhuanAnalyzer().addProcessor(new ILexProcessor() { // 创建一个 分词器

			/**
			 * 把符号转换成 词素
			 *
			 * @param symbol 符号
			 * @return 词素
			 */
			@Override
			public Lexeme evaluate(final String symbol) {
				final var cur_trie = trie.getTrie(symbol.split(""));// 提取节点词素
				if (cur_trie == null)
					return null;
				final var category = cur_trie.getAttribute("category") + "";
				return new Lexeme(symbol, category, "unknown");
			}

			@Override
			public boolean isPrefix(final String line) {
				if (Stream.of(// pattern 列表
						"[a-zA-Z]+", // 英文单词
						"[a-zA-Z_-]+", // 英文标识符号
						"[\\d\\.]+", // 阿拉伯数据
						"[一二三四五六七八九十零百千万亿兆]+", // 中文数字
						"[一二三四五六七八九十零百千万亿兆\\d]+年([一二三四五六七八九十零百千万亿兆\\d+](月([一二三四五六七八九十零百千万亿兆\\d+]日?)?)?)?") // 日期前缀的判断
						.anyMatch(line::matches))
					return true;

				return trie.isPrefix(line.split(""));
			}// isPrefix
		});// addProcessor

		return yuhuan;
	}

	/**
	 * 语料库的 Trie 树演示
	 */
	@Test
	public void foo() {
		final var prefix = "F:/slicef/ws/gitws/malonylcoa/src/test/";
		final var fileHome = FT("$0/java/gbench/webapps/crawler/api/model/data/docs/zhuzi", prefix);
		final var corpusDir = FT("$0/java/gbench/webapps/crawler/api/model/data/corpus", prefix);

		Stream.of(new File(fileHome).listFiles()).filter(e -> e.getName().endsWith(".txt")).forEach(file -> {
			System.out.println(file.getName());
			final var line = readLineS(file).collect(Collectors.joining("\n"));
			Stream.of(// analyzer 序列
					getYunHuan(corpusDir) // getYunHuan
			).forEach(analyzer -> { // 分词器演示
				System.out.println("\n -- " + analyzer.getClass().getSimpleName());
				splits(analyzer, "test", line).stream().filter(e -> e.str("category").equals("word")) //
						.forEach(rec -> {
							System.out.println(rec);
						});
			});
		});
	}

}
