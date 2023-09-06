package gbench.util.math.algebra.op;

import static gbench.util.math.algebra.tuple.IRecord.REC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * 
 * 词素运算类型<br>
 * 
 * 词素运算类型 就是 没有 BinaryOp
 * 
 * @author gbench
 *
 */
public class ConstantOp extends BinaryOp<Object, Object> {

	/**
	 * 
	 * @param name 词素名称
	 */
	public ConstantOp(final Object name) {
		super(name, null);
	}

	/**
	 * 
	 */
	@Override
	public int getAry() {
		return 0;
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

		try {
			throw new Exception("ConstantOp 无法做 compose1(X) 组合");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 2#参数位置组合
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式, ConstantOp 无法做 compose2(X) 组合
	 */
	public <X> BinaryOp<Object, X> compose2(final X x) {

		try {
			throw new Exception("ConstantOp 无法做 compose2(X) 组合");
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
	 * @return null , ConstantOp 无法做 compose(X,Y) 组合
	 */
	public <X, Y> BinaryOp<X, Y> compose(final X x, final Y y) {

		try {
			throw new Exception("ConstantOp 无法做 compose(X,Y) 组合");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 构造一个复制品
	 * 
	 * @return UnaryOp 浅层拷贝
	 */
	@Override
	public ConstantOp duplicate() {
		return new ConstantOp(this._1);
	}

	/*
	 * 二元函数的求值
	 * 
	 * @return 二元函数计算的结果
	 */
	public Object evaluate(final Map<String, Object> bindings) {
		return bindings.getOrDefault(this._1, consts.getOrDefault(this._1, this._1));
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { ConstantOp.class, this._1 });
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ConstantOp cop) {
			return Objects.equals(this._1, cop._1);
		} else {
			return false;
		}
	}

	/**
	 * 数学常量
	 */
	static Map<String, Object> consts = REC( //
			"PI", Math.PI //
			, "E", Math.E //
			, "TAU", Math.TAU //
	).toMap(); // 常量

	// 大写转小写
	static { // 常量初始化
		new ArrayList<>(consts.keySet()) // 复制键名列表以保证MAP不会并发修改
				.forEach(k -> { // 键名转换
					consts.put(k.toLowerCase(), consts.get(k));
				});
	} // static

}