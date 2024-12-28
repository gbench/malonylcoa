package gbench.webapps.crawler.api.model.decomposer;

import gbench.util.data.DataApp.IRecord;
import gbench.webapps.crawler.api.model.SrchModel;
import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;

import static gbench.util.data.DataApp.IRecord.REC;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 图片的处理与处理
 */
public class ImageDecomposer implements IDecomposer {

	/**
	 * 图片 分词：解构
	 * 
	 * @param entry 记录需要包含一下字段 <br>
	 *              - File file 待分词的文件 <br>
	 *              - YuhuanAnalyzer analyzer 分词器 <br>
	 *              - ConcurrentLinkedQueue&lt;IRecord&gt;tokens [out] 类型参数
	 *              ,tokens的元素是一个IRecord对象需要包括:<br>
	 *              symbol:关键词,<br>
	 *              statement:上下文语句摘要, <br>
	 *              file:文件名, 以及 type:媒体类型 四个字段 特殊补充字段 <br>
	 *              position 关键词的索引位置:{rownum,sart,end}的IRecord<br>
	 *              snapfile 快照文件的位置:{rownum,sart,end}的IRecord<br>
	 *              - ConcurrentLinkedQueue&lt;IRecord&gt;files[out] 类型参数处理的文件集合
	 *              <br>
	 */
	@SuppressWarnings("unchecked")
	public void decompose(IRecord entry) {
		final File file = (File) entry.get("file");
		final YuhuanAnalyzer yuhuan = (YuhuanAnalyzer) entry.get("analyzer");
		final ConcurrentLinkedQueue<IRecord> tokens = (ConcurrentLinkedQueue<IRecord>) entry.get("tokens");
		final ConcurrentLinkedQueue<File> files = (ConcurrentLinkedQueue<File>) entry.get("files");

		if (file == null || !file.exists()) {// file 类型检测
			System.err.println(MessageFormat.format("{0} 为空 或 不存在, 不予处理!", file));
			return;
		} // if

		if (SrchModel.enableImgNameAsKeyword) {// 开始对图片名称关键词识别功能
			final Trie<String> corpus = yuhuan.findOne(Trie.class);
			final var name = file.getName().split("\\.")[0];// 提取文件名

			Stream.concat(// 路径分词
					Stream.of(file.getParentFile()).filter(e -> e != null)// 提取路径信息
							.flatMap(f -> Arrays.stream(f.getPath().split("[:/\\\\]+"))), // 分解路径层级
					Stream.of(name)).forEach(keyword -> {
						// 添加关键词
						Trie.addPoints2Trie(corpus, keyword.split("")).addAttribute("category", "word")
								.addAttribute("meaning", keyword);// 记录分词
					});
		} // 开启 enableImgNameAsKeyword

		// 加入处理列表
		files.add(file);// 记录索引文件
		yuhuan.analyzeS(file.getAbsolutePath()) // 对文件目录进行分词
				.filter(e -> "word".equals(e.strOpt("category").orElse("").toLowerCase())) //
				.forEach(token -> {
					final var symbol = token.str("symbol");
					if (symbol == null) {
						return;
					} // if
					final var tags = token.getS("tags", Object::toString).collect(Collectors.toList()); // 词汇分类标签
					final var statement = file.getAbsolutePath(); // 文件语句
					final var filename = file.getName(); // 文件名称
					final var start = token.i4("start"); // 开始位置
					final var end = token.i4("end"); // 结束位置
					final var position = REC("rownum", 1, "start", start, "end", end); // 关键字的位置记录

					tags.add("图片"); // 增加图片标签
					tokens.add(REC(// 加入字段分析结果
							"symbol", symbol, // 关键词
							"statement", statement, // 文件语句
							"file", filename, // 文件名称
							"type", "image", // 类型
							"position", position, // 关键字的位置
							"snapfile", "-", // 快照文件位置路径
							"tags", tags // 词汇分类标签
					));// add

					System.out.println(MessageFormat.format("\n token\t{0}:\n eg:{1}\n file:{2}\n position:{3}", token, // 词法记录
							statement, // 上下文语句
							file.getName(), // 文件名称
							position // 关键词的位置记录
					));// println
				});// forEach
	}

}
