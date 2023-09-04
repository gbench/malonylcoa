package gbench.util.math.algebra.symbol;

import java.util.Map;

import gbench.util.math.algebra.op.BinaryOp;

/**
 * 符号运算
 * 
 * @author Administrator
 *
 */
public interface ISymboLab {

	/**
	 * 符号计算
	 * 
	 * @param <T>
	 * @param <U>
	 * @param symbol 运算符号
	 * @return 计算结果
	 */
	<T, U> BinaryOp<Object, Object> simplify(final BinaryOp<T, U> symbol);

	/**
	 * 二元函数的求值
	 * 
	 * @param <T>
	 * @param <U>
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	<T, U> Object evaluate(final BinaryOp<T, U> symbol, final Map<String, Object> bindings);

	/**
	 * 符号计算
	 * 
	 * @param <T>
	 * @param <U>
	 * @param symbol 运算符号
	 * @return 计算结果
	 */
	public static <T, U> BinaryOp<Object, Object> spf(final BinaryOp<T, U> symbol) {
		return new SymboLab().simplify(symbol);
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param <T>
	 * @param <U>
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public static <T, U> Object eval(final BinaryOp<T, U> symbol, final Map<String, Object> bindings) {
		return new SymboLab().evaluate(symbol, bindings);
	}
}
