package gbench.util.math.algebra.op;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;

/**
 * 
 * @author gbench
 *
 * @param <T> 一元函数的参数类型
 */
public class UnaryOp<T> extends BinaryOp<T, Object> {

	/**
	 * 
	 * @param _1
	 * @param t
	 */
	public UnaryOp(final Object _1, final T t) {
		super(_1, P(t, null));
	}

	/**
	 * 
	 */
	@Override
	public int getAry() {
		return 1;
	}

	/**
	 * 构造一个复制品
	 * 
	 * @return UnaryOp 浅层拷贝
	 */
	@Override
	public UnaryOp<T> duplicate() {
		return new UnaryOp<>(this._1, this._2._1());
	}

	/**
	 * 1#参数位置组合 <br>
	 * 
	 * 一元函数继承的时候 需要 实现 自己的compose1, 另外compose1是设计为二元函数使用的函数，<br>
	 * 一元函数请使用自己的一元compose <br>
	 * 当然 一元函数可以自己先实现compose1，<br>
	 * 然后用自己compose调用自己先实现compose1
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<X, Object> compose1(final X x) {
		return new UnaryOp<>(this._1, x);
	}

	/**
	 * 2#参数位置组合
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式, UnaryOp 无法做 compose2(X) 组合
	 */
	public <X> BinaryOp<T, X> compose2(final X x) {

		try {
			throw new Exception("UnaryOp 无法做 compose2(X) 组合");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 
	 * 1#,2#位置的参数组合
	 * 
	 * @param <X> 1#参数的类型
	 * @param <Y> 2#参数的类型
	 * @param x   1#参数
	 * @param y   2#参数
	 * @return null , UnaryOp 无法做 compose(X,Y) 组合
	 */
	public <X, Y> BinaryOp<X, Y> compose(final X x, final Y y) {

		try {
			throw new Exception("UnaryOp 无法做 compose(X,Y) 组合");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 1#参数位置组合
	 * 
	 * 一元函数继承的时候 需要 实现 compose
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<X, Object> compose(final X x) {
		return this.compose1(x);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { UnaryOp.class, this._1, this._2._1 });
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof UnaryOp<?> uop) {
			return Objects.equals(this._1, uop._1) && Objects.equals(this._2._1, uop._2._1);
		}

		return false;
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		String s1 = this._2._1() == null ? "null" : this._2._1().toString();
		return MessageFormat.format("({0},{1})", this._1, s1);
	}

	/**
	 * 
	 * @param <T>
	 * @param name
	 * @param t
	 * @return
	 */
	public static <T> UnaryOp<T> of(String name, T t) {
		return new UnaryOp<>(name, t);
	}

}