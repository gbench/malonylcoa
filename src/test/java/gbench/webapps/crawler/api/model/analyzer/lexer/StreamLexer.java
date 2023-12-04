package gbench.webapps.crawler.api.model.analyzer.lexer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 流式中文阅读器 <br>
 * 
 * 我想我是有诗人的气质的：不是婉约的而是豪放派的那种，给点阳光就要灿烂，给竹筐就要下蛋的 灵感来了，挡也挡不住的！就要写点 疯狂与躁动， <br>
 * 用流水 掀起 狂澜。 这是 诗人的 冷静与逻辑。理性 就是用来为 自信（抱负与气魄 )提供 依据与手段的。否则 充其量 理性只会让 一个人成为
 * 忠实的奴隶，而这个理性不要也罢！ <br>
 * 
 * 我想把程序写的简洁一些 就像 每个词在文章中的 出现就像流水一样的自然顺畅。这也许就是我想写一个 StreamLexer 的最开始的动机(注意不是原始,
 * 原始强目标情怀,是一种价值追求，往往后续还会在其基础之上进行演化，派生。而 开始则简单得多,只是一个事件触发而已,没有太大的价值牵引只是一种兴趣 <br>
 * 或是不得已而为之 ,没有什么伟大的宏伟目标,或是说原因不是特别 纯良的 又或是 清楚明白的,饭真的就是 这么的开始了，然后就这么的现在了。。。)。
 * <br>
 * 单词就像 地下的泉水一样 咕噜咕噜的 往外冒。一串一串 的出来 ，口若悬河一样的 就像水一样 上善若水，这是不是说 道德，而是谈 艺术与 价值追求 所以
 * 水 是一种美好的评价标准 。它就是源源不断的出来。 水是一种艺术 Stream 是一种技术 一种最像水的技术 最善的技术。 <br>
 * 
 * Stream是一种最像水的技术，是最水的技术。我就是要用最水的技术来 写 最水的程序<br>
 * 于是 就有了 最水的分词器：StreamLexer
 * 
 * 水是没味道的 , 是没滋味的, 所以 水的 东西大家都看不上 不是不重要,而是 不容易被察觉。这就是 水的意义。所以水 的程序也是有意义的 ,
 * 只不过这个意义要 用水的人才知道,水是不知道的，这就是水的程序的意义。<br>
 * 
 * @author XUQINGHUA
 *
 */
public class StreamLexer extends AbstractLexer {

	/**
	 * 流式中文阅读器
	 * 
	 * @param reader
	 */
	public StreamLexer(final Reader reader) {
		this.streamReader = reader;
	}

	/**
	 * 流式中文阅读器-默认构造函数
	 * 
	 * @param streamReader
	 */
	public StreamLexer() {
		this.streamReader = null;
	}

	/**
	 * 获取 reader
	 * 
	 * @return reader
	 */
	public Reader getStreamReader() {
		return streamReader;
	}

	/**
	 * 设置 reader
	 * 
	 * @param reader
	 */
	public void setStreamReader(final Reader reader) {
		this.streamReader = reader;
	}

	/**
	 * 判断line是否像一个词
	 * 
	 * @param line 待检测的字符串
	 * @return 是否需要继续移入字符
	 */
	synchronized boolean moveOn(final String line) {
		final var ar = new AtomicReference<Boolean>(false);
		return this.processors.entrySet().stream().parallel().map(Map.Entry::getValue).map(processor -> {
			if (ar.get()) {
				return true;
			} else {
				boolean _b_ = processor.isPrefix(line);// 是否可以停下啦
				if (_b_) {
					ar.set(true);
				} // if
				return _b_;
			} // if
		}).filter(e -> e == true).findFirst().isPresent();
	}

	/**
	 * 对于一个词进行解释
	 * 
	 * @param symbol 符号
	 * @return TokenProfile
	 */
	public synchronized TokenProfile evaluate(final String symbol) {
		if (null == symbol) {
			return null;
		}

		// 依次从各个模块中获取文字解释
		final Optional<TokenProfile> opt = this.processors.keySet().parallelStream() // 多个处理器并发读取
				.map(key -> this.processors.get(key).evaluate(symbol)).filter(e -> e != null)
				.map(lexeme -> new TokenProfile(lexeme.getSymbol(), lexeme.getCategory(), lexeme.getMeaning())
						.addTags(lexeme.getTags().toArray(String[]::new)) // 添加标签
						.addAttributes(lexeme.getAttributes())) // 补充词素的属性
				.findFirst(); // 以最先识别的出来的单词为主

		// 内容输出
		return opt.orElse(null);
	}

	/**
	 * 把一个字符串给予分解成词单元
	 */
	public synchronized TokenProfile nextToken() {
		return this.nextToken(this.streamReader);
	}

	/**
	 * 把一个字符串给予分解成词单元
	 * 
	 * @param input 输入流
	 * @return 分词单词信息 TokenProfile 可能为 null
	 */
	public synchronized TokenProfile nextToken(final Reader input) {
		TokenProfile word = null;// 返回值
		if (input == null) {
			return null;
		}

		// 单字符读取函数-扫描器
		final Supplier<Integer> spfunc_scanner = () -> {
			int c = -1;// 默认是无效字符
			try {
				// 优先读取堆栈里面的内容
				// isEmpty():如果变量未初始化或显式地设置为 Empty，则函数 IsEmpty
				// 返回 True；否则函数返回 False 是 Vector 方法
				// empty 判断stack 是否为空 是stack 方法
				c = (this.charsStack.isEmpty() || this.charsStack.empty()) ? input.read() : charsStack.pop();
			} catch (Exception e) {
				System.out.println("stack状态：" + this.charsStack);
				e.printStackTrace();
			} // try
			return c;
		};// 读取字符的函数

		try {// 取词开始
			int c = spfunc_scanner.get();// 扫描一个字符
			while (c != -1) {
				var wordBuffer = new String((char) c + "");// 单词前缀的字符序列 。用String做数据结构是 比较下来还是String的结果比较快。
				// 如果s是一个 单词前缀 就一直读下去， 直到 最近读到一个字符（c)让其（wordBuffer）不在是一个单词前缀。
				// 增长单词前缀长度，直到不再是单词前缀为止（以获得wordBuffer在这一轮读取操作中是中包含有最长的按此前缀）即去掉 末尾的一个字符 就可以得到这个
				// 最长前缀。
				while (c != -1 && this.moveOn(wordBuffer)) {
					wordBuffer += ((char) (c = spfunc_scanner.get())); // 增长单词前缀长度
				}

				// 已经读到了最后一个字符,注意这里不能使用wordBuffer.indexOf(0),
				// 因为对于unicode会会返回第一字节的内容(-1),unicode的一个字符不是一个字节长的。而不是第一个字符。
				if (wordBuffer.length() == 1 && wordBuffer.equals(((char) -1) + "")) {
					break;// 数据读完了。因读到 -1文件结束字符了。
				}

				/**
				 * 尝试去进行精确匹配的取词：获取 characters（有效单词前缀）的意义。即 this.evaluate 若是最近的取得那个字符(c)无法与先前取到
				 * 字符序列(wordBuffer)构成有效词的前缀，就把这个字符c 从characters中给与去除。 这样就确保了 送交到
				 * this.evaluate做意义检测 的 characters必定是一个 在当前语料单词卡库下 匹配得到的的 最长的单词前缀。
				 * 
				 * 而对于一个单词而言 最长的单词前缀 就是这个单词本身。这 就是nextToken的分词原理。基于最长单词前缀 分析法。 所谓单词 要同时符合两个条件:
				 * 1) 必须是一个单词前缀 ： moveOn 保证了wordBuffer 是一个逐渐增长的 单词前缀。 2) 而且 还拥有 意义 meaning
				 */
				String characters = null; // 准备做单词意义检测的字符串。
				if (wordBuffer.length() > 1) { // 已经读到了至少两个字符,读到的字符存入 characters 以之中 准备做 单词意义检测。
					characters = wordBuffer.substring(0, wordBuffer.length() - 1);// 回退一个字符：获取单词前缀。
				} else { // 长度为1的wordBuffer: 因为 spfunc_scanner保证了 wordBuffer至少包含一个字符。
					// 由于 此时wordBuffer中只有一个字符,所以 就不能再去除尾部字符了（那样characters就为空了） ,只有一个字符
					// 的wordBuffer将以整体形式进行evaluate.
					characters = wordBuffer.toString();
				}

				// 已经取得一个类似词的对象(准单词characters),即 characters 是我们找到的一个不是任何
				// 其他单词前缀的对象（即最长单词前缀,我们用最长保证 不是任何其他），
				// evaluate 的目的就是要确定出这个 单词前缀 (characters)是否拥有意义。如有 具有意义 那 characters 就是我们 要找出的词。
				// 否则 就不是词。（也有可能是只
				// 是一些标点符号或是其他的东西，不过 这个就不重要了，这就不是分词的目的了。）总之 evaluate 要保证我们可以
				// 提供对我们提交的任何 字符串 给出有效 意义的解释。
				// 1) 有意义 : 返回非空 对象
				// 2) 无意义 : 返回 空null
				word = this.evaluate(characters); // 获取characters的符号意义。
				if (word != null) {// characters 验证无误这是一个词 这是一次成功的操作
					if (wordBuffer.length() > 1) { // 确保是在此读到了一个新的字符。
						c = wordBuffer.charAt(wordBuffer.length() - 1);
					} else {// characters 为只有一个字符长度的 单词(这一个单字符的单词没有保存在单词库中,因为moveOn返回false)，所以
							// 不用再去退自负了,继续读下一个字符了。即不退回的读
						c = spfunc_scanner.get();// 读取一个字符
					} // if
				} else { // characters 不是一个单词，但要注意 characters 这个这个字符串的 本身 可能还包含有 一个 是
							// 一个稍微短一点的单词。我们要把他们给尽可能的找出来。
					/**
					 * 比如 对于 "人民代表大" 它是 "人民代表大会"这个词 的前缀 但是由于 文本中没有"会"这个词（比如：写错了,落下
					 * 或是其他原因，总之这不是分词要考虑的）， 所以 "人民代表大" 就不是一个词了 但是 "人民代表" 是一个词，所以
					 * 我们这里的操作就是要从后向前的继续去剔除多余的字符，把 "人民代表"给找出来。一个字符的一个字符的精雕细刻的把这个单词("人民代表")给找出来。
					 * 
					 * 取词失败，这时候的wordBuffer/characters只是一个前缀。需要进行回溯,并探测是否存在一个合法标识符，回溯退回的
					 * 字符给予压入堆栈，注意这里采用倒序退回，保证了在以后的 再次读取的时候，通过堆栈的pop可以得到正常顺序。
					 */
					for (int i = wordBuffer.length() - 1; i > 0; i--) {
						final char i_char = wordBuffer.charAt(i);
						this.charsStack.push((int) i_char); // 退掉一个字符
						characters = wordBuffer.substring(0, i);// 退回后的残基
						// 对退回后的字符串残基检查
						if ((word = this.evaluate(characters)) != null) {
							break;// 一旦发现残基characters是一个合法的标识就立即给予返回
						} // if
					} // 依次压入栈

					c = spfunc_scanner.get();// 读取一个字符,作为下一次测验的开始
				} // 验证无误这是一个词

				if (word != null) {// 确定并核实了这就是一个有完整意义的词。
					// do nothing;
				} else { // 对于 经过上述努力（characters的整体检测,characters 部分检测,注意此时characters
							// 已经退回到了只有一个字符的长度的词了）我们 依旧无法确定 这个单词的意义，于是干脆 就自定义 一种解释出来。
					// 这就是 未知 单词类型的 意义所在。保证我们可以对任何字符序列进行分词。这就是 语言逻辑的 魅力，没有意义 我们就构造一种意义。这是
					// 毛主席：没有条件创造条件 思想 的 编程应用。
					word = new TokenProfile(characters, "未知", "标点符号，英文字母或者无法获得意义的符号");
					// System.out.println("---null");// 对于非词性串给予忽视
				} // if
				charsStack.push(c);// 保留下一次进入需要读取的字符。
				break;// 中断操作给予返回
			} //
				// 除掉最后一个字符以外就是一个完整的词。
		} catch (Exception e) {
			e.printStackTrace();
		} // try 取词结束

		// 单词返回
		if (word != null) {// null 就说明已经处理到了文件末尾
			word.offset_start = this.wordOffset;// 开始字符索引位置
			this.wordOffset = this.wordOffset + word.symbol.length();// 修正wordOffset
			word.offset_end = this.wordOffset - 1;// 结束字符索引位置。
		} else {// 我有一个故事：有一个人它一出生就死了，我不是在搞笑 的 , 相反由于
				// 这是个严肃的事情，所以我要在这里正式的写一下，让大家知道有这样的故事。有这样的事情发生。
			// 注释掉了不是 不重要，而是不想让 那些无关紧要的人员知道，免得他们 生疑，这就是为何 并不是所有 事实都要说出来，不是不知道，而是不想让
			// 大家知道。因为没有必要
			// System.err.println("开始就是结束,也就是没有读出字符或是一开始就读到了结束的字符,小概率也是机会,是机会就一定会发生 这是
			// 应对风险的逻辑 与 谨慎思考的态度。!'");
		} // if

		return word; // 返回单词。
	}

	/**
	 * 重置系统
	 */
	public synchronized void reset() {
		this.charsStack.clear();
		this.wordOffset = 0;// 设置wordOffset。开始位置。
	}

	/**
	 * 把一个字符串给予分解成词单元
	 */
	public synchronized List<TokenProfile> analyze(final InputStreamReader isr, final boolean close) {
		final List<TokenProfile> list = new LinkedList<TokenProfile>(); // 返回值
		this.reset();// 清空堆栈数据
		TokenProfile word = this.nextToken(isr);
		while (word != null) {
			list.add(word);
			word = this.nextToken(isr);
		} // while

		try {
			isr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * 把一个字符串给予分解成词单元
	 */
	public synchronized List<TokenProfile> analyze(final String line) {
		final InputStream byteStream = new ByteArrayInputStream(line.getBytes()); // 把line 打散成粒子态(字节码）
		final InputStreamReader isr = new InputStreamReader(byteStream); // 再把字节码整理成字符。
		return this.analyze(isr, true);
	}

	// 流式阅读器
	private Reader streamReader = null;// 阅读器
	private final Stack<Integer> charsStack = new Stack<Integer>(); // 回退词的存储空间, 阅读状态的暂存空间
	private int wordOffset = 0;// 单词的偏移位置。

}