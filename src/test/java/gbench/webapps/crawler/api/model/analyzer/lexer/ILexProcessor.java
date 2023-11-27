package gbench.webapps.crawler.api.model.analyzer.lexer;

/**
 * 
 * 语料库分词器: <br>
 * 通过符号获得词库中的单词信息，<br>
 * 是判断一个端字符是否构成也能够一个词库中某个词的前缀 <br>
 * 
 * @author XUQINGHUA
 *
 */
public interface ILexProcessor {

	/**
	 * 模块名称
	 * 
	 * @return
	 */
	public default String getName() {
		final var clazz = this.getClass();
		return clazz.getName();
	}

	/**
	 * 
	 * @param name
	 */
	public default void setName(String name) {
		// do nothing
	}

	/**
	 * 把符号转换成 词素
	 * 
	 * @param symbol 符号
	 * @return 词素
	 */
	public default Lexeme evaluate(String symbol) {
		return new Lexeme(symbol, "unknown", "unknown");
	}

	/**
	 * 判断一个字符串line是否构成一个词库中的某个词的前缀
	 * 
	 * @param line 前缀,待检测的字符串
	 * @return
	 */
	public default boolean isPrefix(String line) {
		return false;
	}

	/**
	 * 终止连接
	 */
	public default void uninitialize() {
		// do nothing
	}

	/**
	 * 初始化过程
	 */
	public default void initialize() {
		// do nothing
	}
}