package gbench.util.io;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 输出工具类
 * 
 * @author xuqinghua
 *
 */
public class Output {

	/**
	 * 行数据输出
	 * 
	 * @param <T> 参数类型
	 * @param objects 输出的
	 */
	@SafeVarargs
	public static <T>  String println(final T... objects) {

		if (null != objects) {
			final String line = Arrays.asList(objects).stream().map(e -> e + "").collect(Collectors.joining("\t"));
			System.out.println(line);
			return line;
		} else {
			System.out.println();
			return null;
		}
	}

}
