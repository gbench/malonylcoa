package gbench.webapps.crawler.api.model.analyzer.junit;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.DFrame;
import gbench.util.data.xls.SimpleExcel;
import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.ILexProcessor;
import gbench.webapps.crawler.api.model.analyzer.lexer.Lexeme;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;

/**
 * 
 */
public class JunitAnalyzer2 {

	/**
	 * corpusPath 语料库路径
	 *
	 * @param corpusDir
	 * @return YuhuanAnalyzer
	 */
	public YuhuanAnalyzer getYunHuan(final DFrame dfm) {
		final var trie = new Trie<String>();// 语料库的根节点树
		dfm.rowS().forEach(line -> {
			final var word = line.str("WORD");
			final var points = word.split("");
			final var meaning = word;
			Trie.addPoints2Trie(trie, points) // 增加节点
					.addAttribute("category", "word") // 绑定词法类型信息
					.addAttribute("meaning", meaning); // 词义
		});

		// 打印语料库
		Trie.traverse(trie, e -> {
			final var line = "\t".repeat(e.getLevel()) + e.getValue() + //
					"\ttype:" + e.getAttribute("type");
			System.out.println(line);
		}); // traverse

		@SuppressWarnings("resource")
		final var yuhuan = new YuhuanAnalyzer().addProcessor(new ILexProcessor() { // 创建一个 分词器
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
						.anyMatch(line::matches)) {
					return true;
				}

				return trie.isPrefix(line.split(""));
			}// isPrefix
		});// addProcessor

		return yuhuan;
	}

	@Test
	public void foo() {
		final var file = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/crawler/api/model/data/datafile.xlsx";
		final var excel = SimpleExcel.of(file);
		final var dfm = excel.autoDetect(1).collect(DFrame.dfmclc2);
		final var analyzer = this.getYunHuan(dfm);

		analyzer.analyze("何不按兵束甲，北面而事之？").forEach(world -> {
			System.out.println(world);
		});

	}

}
