package gbench.util.math.algebra;

import java.util.Map;

import gbench.util.math.algebra.symbol.Node;

/**
 * 
 */
public class Algebras {

	/**
	 * 
	 */
	private Algebras() {

	}

	/**
	 * 结果运算
	 * 
	 * @param <T>      结果类型
	 * @param line     计算表达式
	 * @param bindings 变量参数的数据绑定,参数名，参数值 序列，即:key1,value,key2,value2,...
	 * @return T类型的运算结果
	 */
	@SuppressWarnings("unchecked")
	public static <T> T evaluate(final String line, final Object... bindings) {
		return (T) new AlgebraEngine().analyze(line).evaluate(bindings);
	}

	/**
	 * 结果运算
	 * 
	 * @param <T>      结果类型
	 * @param line     计算表达式
	 * @param bindings 变量参数的数据绑定.
	 * @return T类型的运算结果
	 */
	@SuppressWarnings("unchecked")
	public static <T> T evaluate(final String line, final Map<String, Object> bindings) {
		return (T) new AlgebraEngine().analyze(line).evaluate(bindings);
	}

	/**
	 * 分析一个 词法序列 生成 对应表达式 <br>
	 * (2*pow(x+1,2)+x) 对于 pow 这样的二元 函数 采用 前缀式 写法 , 即 逗号表达式的元组 来 提供参数的 情况, <br>
	 * 采用分组 提交的方式 会 逃避掉 buildTree 的 优先级的 方法调整，会造成 语法 分析失败， 所以 请去掉 分组 符号 <br>
	 * analyze("2*pow(x+1,2)+x") 就可以了。 <br>
	 * analyze("(2*pow(x+1,2)+x)") 会造成 分组混乱
	 *
	 * @param line 表达式的字符串表达
	 * @return Node
	 */
	public static Node analyze(final String line) {
		return new AlgebraEngine().analyze(line);
	}

}
