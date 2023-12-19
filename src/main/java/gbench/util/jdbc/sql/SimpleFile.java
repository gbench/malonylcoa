package gbench.util.jdbc.sql;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 简单的文本读取类：小型文件，可以把文件内容全部读入内存的文件
 * 
 * @author gbench
 *
 */
public class SimpleFile {

	/**
	 * 构造函数
	 * 
	 * @param path 文件绝对路径
	 */
	public SimpleFile(final String path) {
		try {
			is = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 文件的输入流
	 * 
	 * @param is 输入流
	 */
	public SimpleFile(final InputStream is) {
		this.is = is;
	}

	/**
	 * 读取数据行 读取行数据
	 * 
	 * @return 行数据流
	 */
	public List<String> readlines() {
		return readlines(null);
	}

	/**
	 * 读取数据行
	 *
	 * @param filter 行数据过滤器
	 * @return 行数据流
	 */
	public List<String> readlines(final Predicate<String> filter) {
		BufferedReader br = null;
		List<String> ll = null;
		final Predicate<String> flt = filter == null ? (s -> true) : filter;
		try {
			br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			ll = br.lines().filter(flt).collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return ll;
	}

	private InputStream is; // 输入流
}