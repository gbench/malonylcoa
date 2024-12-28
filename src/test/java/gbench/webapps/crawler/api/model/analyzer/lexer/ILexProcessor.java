package gbench.webapps.crawler.api.model.analyzer.lexer;

/**
 * 
 * 语料库分词器: <br>
 * 通过符号获得词库中的单词信息，<br>
 * 是判断一个端字符是否构成也能够一个词库中某个词的前缀 <br>
 * 
 * ILexProcessor是计算一个符号序列的词义与分类信息的过程方法的核心与规范。<br>
 * 1）evaluate 计算词汇的符号序列的词义（为symbol关联词义与分类(tags)的逻辑） <br>
 * 2）isPrefix 判断一个符号序列是否一个有意义的词汇前缀。
 * 
 * @author XUQINGHUA
 *
 */
public interface ILexProcessor {

	/**
	 * 处理器名称
	 * 
	 * @return 处理器名称
	 */
	public default String getName() {
		final var clazz = this.getClass();
		return clazz.getName();
	}

	/**
	 * 处理器名称
	 * 
	 * @param name 处理器名称
	 */
	public default void setName(final String name) {
		// do nothing
	}

	/**
	 * 符号关系词义 即 把符号转换成词素 <br>
	 * <br>
	 * evaluate 计算词汇的符号序列的词义（为symbol关联词义与分类(tags)的逻辑） <br>
	 * 
	 * @param symbol 符号即字符序列
	 * @return 词素
	 */
	public default Lexeme evaluate(final String symbol) {
		return new Lexeme(symbol, "unknown", "unknown") //
				.addTags("default"); // 表示接口默认实现
	}

	/**
	 * 判断一个字符串line是否构成一个词库中的某个词的前缀
	 * 
	 * isPrefix 判断一个符号序列是否一个有意义的词汇前缀。
	 * 
	 * @param line 前缀,待检测的字符串
	 * @return true 是一个词汇的前缀,false不是一个词汇的前缀
	 */
	public default boolean isPrefix(final String line) {
		return false;
	}

	/**
	 * 初始化过程
	 */
	public default void initialize() {
		// do nothing
	}

	/**
	 * 终止连接
	 */
	public default void uninitialize() {
		// do nothing
	}
}