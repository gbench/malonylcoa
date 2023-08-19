package gbench.util.function;

import java.util.function.*;

public class Functions {

	/**
	 * argument-insensitive
	 * 
	 * @param <T> 参数类型
	 * @return 返回值类型
	 */
	public static <T> Function<T, T> identity() {
		return new Function<T, T>() {

			@Override
			public T apply(final T o) {
				return o;
			}

			@Override
			public String toString() {
				return "Functions.identity()";
			}
		};
	}

	/**
	 * argument-insensitive <br>
	 * 语法糖书写工具,辅助编译器推导入口参数类型,用于定义级联函数,比如 <br>
	 * identity((Integer[]) null).andThen(INdarray::nd).andThen(INdarray::dupdata)
	 * <br>
	 * 就定义了一个函数类型:Function&ltInteger[],INdarray&ltInteger$gt&gt;函数
	 * 
	 * @param <T>         参数类型
	 * @param placeholder 占位符 用于提示编译器标识数据类型
	 * @return 返回值类型
	 */
	public static <T> Function<T, T> identity(final T placeholder) {
		return identity();
	}

	/**
	 * argument-insensitive <br>
	 * 语法糖书写工具,辅助编译器推导入口参数类型,用于定义级联函数,比如 <br>
	 * identity((Integer[]) null).andThen(INdarray::nd).andThen(INdarray::dupdata)
	 * <br>
	 * 就定义了一个函数类型:Function&ltInteger[],INdarray&ltInteger$gt&gt;函数
	 * 
	 * @param <T>              参数类型
	 * @param placeholderClass 占位符 用于提示编译器标识数据类型
	 * @return 返回值类型
	 */
	public static <T> Function<T, T> identity(final Class<T> placeholderClass) {
		return identity();
	}
}
