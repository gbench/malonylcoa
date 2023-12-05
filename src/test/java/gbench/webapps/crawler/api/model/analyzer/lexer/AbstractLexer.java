package gbench.webapps.crawler.api.model.analyzer.lexer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 简单的汉语分词器
 * 
 * @author XUQINGHUA
 *
 */
public abstract class AbstractLexer {
	/**
	 * 逐行处理文字
	 * 
	 * @param file         文件名称
	 * @param encoding     编码
	 * @param line_handler 行处理器
	 */
	public void handle(final String file, final String encoding, final Function<String, String> line_handler) {
		try {
			final FileInputStream fis = new FileInputStream(file);
			final InputStreamReader isr = new InputStreamReader(fis, encoding);
			final BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			while (line != null) {
				line_handler.apply(line);
				line = br.readLine();// 处理下一行
			} // while
			br.close();// 关闭流
			isr.close();
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		} // try
	}

	/**
	 * 词 项目，拼写方法，以及词的意义。
	 * 
	 * @author XUQINGHUA
	 */
	public class TokenProfile {
		/**
		 * 构造函数
		 * 
		 * @param word    单词
		 * @param meaning 词义解释
		 */
		public TokenProfile(final String word, final String category, final String meaning) {
			this.symbol = word;
			this.category = category;
			this.meaning = meaning;
			this.offset_start = -1;
			this.offset_end = -1;
		}//

		/**
		 * 重写 hashCode 方法一定要和equals 相匹配 以便可以将该对象用作Map的键值
		 */
		public int hashCode() {
			return this.symbol.hashCode();
		}

		/**
		 * 重写 hashCode 方法一定要和equals 相匹配 以便可以将该对象用作Map的键值
		 */
		public boolean equals(final Object obj) {
			if (this.hashCode() == obj.hashCode()) {
				return true;
			}
			return false;
		}

		/**
		 * 返回词素
		 * 
		 * @return
		 */
		public Lexeme getLexeme() {
			return new Lexeme(this.symbol, this.category, this.meaning);
		}

		/**
		 * 用于字符串的连接是的格式转换
		 */
		public String toString() {
			final StringBuffer buffer = new StringBuffer();
			buffer.append(this.symbol + "[" + this.offset_start + "," + this.offset_end + "]:" + this.category + ":"
					+ this.meaning);
			return buffer.toString();
		}

		/**
		 * 添加词汇标签
		 * 
		 * @param tag 标签
		 * @return TokenProfile
		 */
		public TokenProfile addTags(final String... tags) {
			Arrays.stream(tags).forEach(tag -> {
				this.tags.add(tag);
			});
			return this;
		}

		/**
		 * 移除词汇标签
		 * 
		 * @param tag 标签
		 * @return TokenProfile
		 */
		public TokenProfile removeTag(final String tag) {
			tags.remove(tag);
			return this;
		}

		/**
		 * 清除所有词汇标签
		 * 
		 * @param tag 标签
		 * @return TokenProfile
		 */
		public TokenProfile clearTag(final String tag) {
			tags.clear();
			return this;
		}

		/**
		 * 
		 * @return tags
		 */
		public Set<String> getTags() {
			return this.tags;
		}

		/**
		 * 添加节点属性
		 * 
		 * @param objs 节点的属性集合
		 * @reurn TokenProfile 元素本身，以方便实现链式编程
		 */
		public TokenProfile addAttributes(final Object... objs) {
			final int n = objs.length;
			for (int i = 0; i < n - 1; i += 2) {
				this.attributes.put(objs[i] + "", objs[i + 1]);
			}
			return this;
		}

		/**
		 * 添加节点属性
		 * 
		 * @param attributes 属性集合
		 * @reurn TokenProfile 元素本身，以方便实现链式编程
		 */
		public TokenProfile addAttributes(final Map<String, Object> attributes) {
			this.attributes.putAll(attributes);
			return this;
		}

		/**
		 * 获取属性值
		 * 
		 * @param name 属性名
		 * @return 获取属性值
		 */
		public Object getAttribute(final String name) {
			return this.attributes.get(name);
		}

		/**
		 * 移除属性值
		 * 
		 * @param name 属性名
		 * @return 移除的属性的值
		 */
		public Object removeAttribute(final String name) {
			return this.attributes.remove(name);
		}

		/**
		 * 获取节点属性
		 * 
		 * @return 节点属性集合
		 */
		public Map<String, Object> getAttributes() {
			return this.attributes;
		}

		/**
		 * 结果类型
		 * 
		 * @param <T>    返回结果类型
		 * @param getter 树形映射函数
		 * @return 树形信息
		 */
		public <T> T attrs(final Function<Map<String, Object>, T> getter) {
			return getter.apply(this.getAttributes());
		}

		private final Map<String, Object> attributes = new HashMap<>();
		public final Set<String> tags = new HashSet<String>();

		public final String symbol;// 拼写形式
		public final String meaning;// 单词意义
		public final String category;// 单词意义
		/*
		 * *** *** [x]xxxxxxxxxxxxxx[x] *** **** **** ↑ ↑ offset_start offset_end
		 */
		public int offset_start = 0;// 偏移起始
		public int offset_end = 0;// 偏移终止
	};

	/**
	 * 项目初始化
	 */
	public void initialize() {
		this.uninitialize();
		this.processors.forEach((key, processor) -> {
			if (processor != null)
				processor.initialize();
		});// 初始化各个加载的模块
	}

	/**
	 * 添加词库模块
	 * 
	 * @param processor 处理器名称
	 */
	public AbstractLexer addProcessor(final ILexProcessor processor) {
		final var p = this.processors.get(processor.getName());
		if (p != null) {
			p.uninitialize();
			this.processors.remove(p.getName());
		} // if

		final var processor_name = processor.getName();
		this.processors.put(processor_name, processor);
		return this;
	}

	/**
	 * 终止连接
	 */
	public void uninitialize() {
		this.processors.forEach((key, model) -> {
			if (model != null) {
				model.uninitialize();
			} // if
		}); // forEach
	}

	public Map<String, ILexProcessor> getProcessors() {
		return processors;
	}

	// 项目初始的内容
	protected Map<String, ILexProcessor> processors = new HashMap<String, ILexProcessor>();
}