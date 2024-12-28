package gbench.webapps.crawler.api.model.analyzer.lexer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 语料词素（关联了语意义的符号） <br>
 * 符号 --> 意义 <br>
 * 
 * @author XUQINGHUA
 *
 */
public class Lexeme {
	/**
	 * 构造函数
	 * 
	 * @param word     单词
	 * @param category 类别
	 * @param meaning  词义解释
	 */
	public Lexeme(final String word, final String category, final String meaning) {
		this.symbol = word;
		this.category = category;
		this.meaning = meaning;
	}//

	/**
	 * 符号的字符序列
	 * 
	 * @return 符号的字符序列
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * 词汇语义
	 * 
	 * @return 词汇语义
	 */
	public String getMeaning() {
		return meaning;
	}

	/**
	 * 词义分类
	 * 
	 * @return 词义分类
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * 获取标签集合 <br>
	 * 
	 * 一般由ILexProcessor.evaluate 计算词汇的符号序列语义时候进行生成，<br>
	 * 当然也可以由业务逻辑自行给予标记 <br>
	 * 
	 * @return 标签集合
	 */
	public Set<String> getTags() {
		return this.tags;
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
	 * 获取节点属性
	 * 
	 * @return 节点属性集合
	 */
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * 添加词汇标签 <br>
	 * 通常在ILexProcessor的evaluate计算单词意义的时候，进行标记，以满足后续对该词的类型标记。
	 * 
	 * @param tags 标签集合
	 * @return tags 标签集合
	 */
	public Lexeme addTags(final String... tags) {
		this.tags.addAll(Arrays.asList(tags));
		return this;
	}

	/**
	 * 移除词汇标签
	 * 
	 * @param tag 标签名称
	 * @return 本对象以提供链式编程
	 */
	public Lexeme removeTag(final String tag) {
		tags.remove(tag);
		return this;
	}

	/**
	 * 清除所有词汇标签
	 * 
	 * @param tag 标签名称
	 * @return 本对象以提供链式编程
	 */
	public Lexeme clearTag(final String tag) {
		tags.clear();
		return this;
	}

	/**
	 * 添加节点属性
	 * 
	 * @param objs 节点的属性集合
	 * @reurn Lexeme 元素本身，以方便实现链式编程
	 */
	public Lexeme addAttributes(final Object... objs) {
		int n = objs.length;
		for (int i = 0; i < n - 1; i += 2) {
			this.attributes.put(objs[i] + "", objs[i + 1]);
		}
		return this;
	}

	/**
	 * 添加节点属性
	 * 
	 * @param attributes 属性集合
	 * @reurn Lexeme 元素本身，以方便实现链式编程
	 */
	public Lexeme addAttributes(final Map<?, ?> attributes) {
		attributes.forEach((k, v) -> this.addAttributes(k, v));
		return this;
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
	 * 重写 hashCode 方法一定要和equals 相匹配 以便可以将该对象用作Map的键值
	 */
	public int hashCode() {
		return Objects.hash(this.symbol, this.meaning, this.category);
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
	 * 用于字符串的连接是的格式转换
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append(this.symbol + ":" + this.meaning);
		return buffer.toString();
	}

	/**
	 * 词汇的的词义分类标签<br>
	 * 通常在ILexProcessor的evaluate计算单词意义的时候，进行标记，以满足后续对该词的类型标记。
	 */
	private final Set<String> tags = new HashSet<>(); // 预料词汇标签，
	private final Map<String, Object> attributes = new HashMap<>();

	private final String symbol;// 拼写形式
	private final String meaning;// 单词意义
	private final String category;// 类别
}
