package gbench.webapps.crawler.api.model.analyzer.lexer;

import org.apache.lucene.util.Attribute;

/**
 * 词素就是 一个词法结构：一个字符序号
 * 
 * @author gbench
 *
 */
public interface LexemeAttribute extends Attribute {

	/**
	 * 获取词素属性
	 * 
	 * @return 词素：最小的意义单位
	 */
	public Lexeme getLexeme();

	/**
	 * 设置词素属性
	 * 
	 * @param lexeme 词素：最小的意义单位
	 */
	public void setLexeme(final Lexeme lexeme);

}
