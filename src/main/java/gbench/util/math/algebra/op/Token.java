package gbench.util.math.algebra.op;

import gbench.util.math.algebra.tuple.IRecord;

/**
 * 
 * @author gbench
 *
 */
public class Token extends ConstantOp {

	public Token(final Object name) {
		super(name);
	}

	@Override
	public Token duplicate() {
		return new Token(this._1);
	}

	/**
	 * token 的字面值
	 * 
	 * @return
	 */
	public Object value() {
		return this._1 == null ? null : this._1;
	}

	public boolean equals(final Object obj) {
		if (obj instanceof Token) {
			final var another = (Token) obj;
			return this.text().equals(another.text());
		} else {
			return false;
		}
	}

	/**
	 * 文本量
	 * 
	 * @return
	 */
	public String text() {
		return this._1 == null ? null : this._1.toString();
	}

	/**
	 * 提取浮点数的值
	 * 
	 * @return 非法值返回 null
	 */
	public Double dbl() {
		return IRecord.obj2dbl(null).apply(this.text());
	}

}