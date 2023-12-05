package gbench.webapps.crawler.api.model.analyzer.lexer;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * 
 * 自定义属性
 * 
 * @author gbench
 *
 */
public class LexemeAttributeImpl extends AttributeImpl implements LexemeAttribute {

	@Override
	public void clear() {
		this.lexeme = null;
	}

	@Override
	public void reflectWith(final AttributeReflector reflector) {
		reflector.reflect(LexemeAttribute.class, "lexeme", this.getLexeme());
	}

	@Override
	public void copyTo(final AttributeImpl target) {
		if (target == null)
			return;
		if (target instanceof LexemeAttributeImpl la) {// 词素属性设置
			la.setLexeme(lexeme);
		} else {
			// do nothing
		} // 词素属性
	}

	@Override
	public Lexeme getLexeme() {
		return this.lexeme;
	}

	@Override
	public void setLexeme(final Lexeme lexeme) {
		this.lexeme = lexeme;
	}

	private Lexeme lexeme;// 词素属性

}
