package gbench.webapps.crawler.api.model.decomposer;

import gbench.util.data.DataApp.IRecord;
import gbench.util.io.FileSystem;
import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static gbench.util.data.DataApp.IRecord.REC;
import static java.text.MessageFormat.*;

/**
 * 文本内容的分析与处理
 */
public class TextDecomposer implements IDecomposer {

	/**
	 * 切分文本信息:line成上下文 句子
	 * 
	 * @param start:开始位置
	 * @param end：结束位置
	 * @param line       文本信息
	 * @return 包含有 start 的一段文本语句
	 */
	public static String snippet(final int start, final int end, final String line) {
		final var range = new Integer[] { 0, line.length() };
		final String pattern = "。!;“”：，";// 分句的标志性标点

		Stream.of(-start, end).forEach(pos -> {// 检索最近的
			for (final var c : pattern.toCharArray()) {// 提取语句段落
				boolean flag = false;// 是否切分语句完成
				for (int i = (pos < 0 ? start : end); (pos <= 0 ? i >= 0 : i < line.length()); i += pos <= 0 ? -1 : 1) {
					if (line.charAt(i) == c) {
						range[pos > 0 ? 1 : 0] = i;
						flag = true;
						break;
					} // if
				} // for
				if (flag)
					break;
			} // for
		});

		return line.substring(range[0], range[1] + 1 < line.length() ? range[1] + 1 : range[1]);// 文本截取
	}

	/**
	 * 文本 分词：解构
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
	public void decompose(final IRecord entry) {
		final File file = (File) entry.get("file");
		final YuhuanAnalyzer yuhuan = (YuhuanAnalyzer) entry.get("analyzer");
		final ConcurrentLinkedQueue<IRecord> tokens = (ConcurrentLinkedQueue<IRecord>) entry.get("tokens");
		final ConcurrentLinkedQueue<File> files = (ConcurrentLinkedQueue<File>) entry.get("files");
		final String snapHome = entry.str("snapHome");// 提取快照文件的存放路径

		// if(!file.getName().endsWith("txt")) return;// 仅对文本文件进行处理
		files.add(file);// 记录索引文件
		final var counter = new AtomicLong(0);
		FileInputStream is = null;// 文件流

		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} // try
		FileSystem.bufferedRead(is, "utf8", br -> {// 以utf8 方式读取文件内容
			br.lines().filter(line -> {// 过滤掉空行
				counter.getAndIncrement(); // 生成行号
				return !line.matches("^[\\s]*$");// 仅处理非空行
			}).map(line -> {// 把line行数据 转换成 文件数据结构
				final var rownum = counter.get(); // 生成行号
				final var fname = file.getName();// 文件名
				final var snapfile = format("{0}/{1}/snap_{2,number,#}.txt", snapHome,
						fname.substring(0, fname.indexOf(".")), rownum);// 快照文件路径
				final var executors = entry.findOne(ExecutorService.class);
				if (snapHome != null && executors != null) { // 写入快照文件
					executors.execute(() -> FileSystem.utf8write(snapfile, () -> line)); // 写入 快照文件,仅当提供了snapHome才给与写入
																							// snapfile
				} else {// 没有配置
					System.err.println(format("尚未配置线程池(Executors)或快照缓存位置(snapHome),快照文件:{0} 不予进行存储", snapfile));
				} // if
				return REC("line", line, "rownum", rownum, "fname", fname, "snapfile", snapfile);
			}).forEach(item -> { // 不能 parallel否则会乱序
				final var line = item.str("line");// 数据段落
				final var fname = item.get("fname"); // 文件名称
				final var snapfile = item.get("snapfile"); // 快照文件名称
				final var rownum = item.get("rownum");// 源文件中的行号

				// 对line行数据进行分词
				yuhuan.analyzeS(line) //
						.filter(e -> "word".equals(e.strOpt("category").orElse("").toLowerCase())) //
						.forEach(token -> {
							final var symbol = token.get("symbol");// 关键词
							final var start = token.i4("start"); // 开始位置
							final var end = token.i4("end"); // 结束位置
							final var statement = snippet(start, end, line);// 文本片段
							final var position = REC("rownum", rownum, "start", start, "end", end); // 关键字的位置记录
							final var tags = token.get("tags"); // 词汇分类标签

							// 语句
							tokens.add(REC(// 加入字段分析结果
									"symbol", symbol, // 关键词
									"statement", statement, // 关键词所在的上下文语句
									"file", fname, // 文件名称
									"type", "text", // 文本类型
									"position", position, // 关键字的位置
									"snapfile", snapfile, // 快照文件位置路径
									"tags", tags // 词汇分类标签
							));// add

							System.out.println(
									MessageFormat.format("\n token\t{0}:\n eg:{1}\n file:{2}\n position:{3}", token, // 词法记录
											statement, // 上下文语句
											file.getName(), // 文件名称
											position // 关键词的位置记录
							));// println
						});// forEach : tokens
			}); // forEach : item
		});// bufferedRead
	} // decompose
}
