package gbench.webapps.crawler.api.model.analyzer.lexer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 每个词素需要是一个 长度为1的字符串
 * 
 * @author gbench
 *
 */
public class TrieCorpus extends Trie<String> {

	/**
	 * 
	 * @param value
	 */
	public TrieCorpus(final String value) {
		super(value);
	}

	/**
	 * 
	 * @param trie
	 */
	public TrieCorpus(final Trie<String> trie) {
		super(trie.value);
		this.children = trie.children;
		this.level = trie.level;
		this.parent = trie.parent;
		this.value = trie.value;
	}

	/**
	 * 为语料库添加关键词
	 * 
	 * @param keyword 关键词
	 * @return 语料库
	 */
	public TrieCorpus addPoints2Trie(final String keyword) {
		addPoints2Trie(this, Arrays.asList(keyword.split("")));
		return this;
	}

	/**
	 * 为语料库添加关键词
	 * 
	 * @param keywords 关键词集合
	 * @return 语料库
	 */
	public TrieCorpus addPoints2Trie(final Stream<String> keywords) {
		keywords.forEach(keyword -> {
			addPoints2Trie(this, Arrays.asList(keyword.split("")));
		});
		return this;
	}

	/**
	 * 前缀检测
	 * 
	 * @param points
	 * @return
	 */
	public boolean isPrefix(final String points) {
		return this.isPrefix(Arrays.asList(points.split("")));
	}

	/**
	 * 创建语料库
	 * 
	 * @param keywords 关键词集合
	 * @return 语料库对象
	 */
	public static TrieCorpus getCorpus(final Stream<String> stream) {
		final var KEYWORDS = stream.map(e -> Arrays.asList(e.split(""))).collect(Collectors.toList());
		final var TRIE_KEYWORDS = Trie.create(KEYWORDS);// 创建前缀树结构
		return new TrieCorpus(TRIE_KEYWORDS);
	}

	/**
	 * 创建语料库
	 * 
	 * @param keywords 关键词集合
	 * @return 语料库对象
	 */
	public static TrieCorpus getCorpus(final List<String> keywords) {
		return getCorpus(keywords.stream());
	}

	/**
	 * 创建语料库
	 * 
	 * @param keywords 关键词集合
	 * @return 语料库对象
	 */
	public static TrieCorpus getCorpus(String[] keywords) {
		final var KEYWORDS = Arrays.stream(keywords).map(e -> Arrays.asList(e.split(""))).collect(Collectors.toList());
		final var TRIE_KEYWORDS = Trie.create(KEYWORDS);// 创建前缀树结构
		return new TrieCorpus(TRIE_KEYWORDS);
	}

}
