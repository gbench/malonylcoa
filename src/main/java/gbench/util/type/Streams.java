package gbench.util.type;

import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Streams {

	/**
	 * 
	 * @param start
	 * @param f
	 * @return
	 */
	public static Stream<Integer> intS(final int start, final UnaryOperator<Integer> f) {
		return Stream.iterate(start, f);
	}

	/**
	 * 整数流
	 * 
	 * @param n 元素数量
	 * @return IntStream
	 */
	public static IntStream intS(final int n) {
		return IntStream.iterate(0, i -> i + 1).limit(n);
	}

	/**
	 * 整数流
	 * 
	 * @param from 开始位置
	 * @param to   结束位置
	 * @return IntStream
	 */
	public static IntStream intS(final int from, final int to) {
		return intS(to - from).map(i -> from + i);
	}
}
