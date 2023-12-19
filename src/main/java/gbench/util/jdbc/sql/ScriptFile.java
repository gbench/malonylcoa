package gbench.util.jdbc.sql;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 脚本文件:存放着查询语句的模板
 * 
 * @author gbench
 *
 */
public class ScriptFile extends SimpleFile {

	/**
	 * 创建一个脚本文件对象
	 * 
	 * @param path 脚本文件路径
	 */
	public ScriptFile(final String path) {
		super(path);
		this.initialize();
	}

	/**
	 * 创建一个脚本文件路径
	 * 
	 * @param is 输入流
	 */
	public ScriptFile(final InputStream is) {
		super(is);
		this.initialize();
	}

	/**
	 * 数据初始化
	 */
	public void initialize() {
		final StringBuilder buffer = new StringBuilder();
		final var titlePattern = Pattern.compile("\\s*--\\s*#+\\s*(([^\\s])(.*[^\\s])?)\\s*"); // sql标题开始行
		String title = null;
		for (var line : this.readlines(s -> !s.matches("[\\s*]"))) {
			var mat = titlePattern.matcher(line);
			if (mat.matches()) {
				if (title != null && buffer.length() > 0) {// 记录标题
					stmts.put(title, new SQL(title, buffer.toString()));
				} // if
				title = mat.group(1);// 标题
				buffer.delete(0, buffer.length());// buffer 清空
			} else {
				if (!line.matches("\\s*--\\s*.*")) {// 去掉注释行
					if (buffer.length() > 0)
						buffer.append("\n");
					line = line.replaceAll("\\s*((--)|(//)).*$", "");// 去除行尾部的注释 -- 和 --都是注释符号
					buffer.append(line);
				} // if
			} // if
		}
		;// for
		stmts.put(title, new SQL(title, buffer.toString()));

		// stmts.forEach((k,v)->{System.out.println("name"+k+"\nstmts\n"+v);});
	}

	/**
	 *
	 * @param name 语句名称
	 * @return SQL 对象
	 */
	public SQL get(final String name) {
		return stmts.get(name);
	}

	public Map<String, SQL> getStmts() {
		return stmts;
	}

	private final Map<String, SQL> stmts = new LinkedHashMap<>();// 语句集合

}