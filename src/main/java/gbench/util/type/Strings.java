package gbench.util.type;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Strings {
	
	/**
	 * 重复元s直至n次
	 * 
	 * @param s 重复元
	 * @param n 重复次数
	 * @return 重复元s直至n次
	 */
	public static String repeat(final String s, final int n) {
		return Stream.iterate(0, i -> i + 1).limit(n).map(i -> s).collect(Collectors.joining(""));
	}

}
