package gbench.util.chn;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.*;
import java.net.URISyntaxException;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 汉字转换成拼音方法
 * 
 * ref:https://blog.csdn.net/lkx94/article/details/53860253
 * 
 * @author gbench
 *
 */
public class PinyinUtil {

	/**
	 * 生成当前类的默认数据存放路径
	 * 
	 * @param file
	 * @param clazz 与file文件同级的目录的class文件
	 * @return
	 */
	public static String path(final String file, final Class<?> clazz) {
		String path = null;
		if (clazz == null)
			return null;
		final Class<?> cls = clazz == null ? PinyinUtil.class : clazz;
		try {
			path = cls.getClassLoader().getResource("").toURI().getPath();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		} // 获取当前路径

		path += cls.getPackage().getName().replace(".", "/") + "/" + file;
		// System.out.println(path);
		return path;
	}

	/**
	 * 将某个字符串的首字母 大写
	 * 
	 * @param str
	 * @return
	 */
	public static String convertInitialToUpperCase(final String str) {
		if (str == null)
			return null;

		final StringBuffer sb = new StringBuffer();
		final char[] arr = str.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			final char ch = arr[i];
			if (i == 0) {
				sb.append(String.valueOf(ch).toUpperCase());
			} else {
				sb.append(ch);
			} // if
		} // for

		return sb.toString();
	}

	/**
	 * 汉字转拼音 最大匹配优先
	 * 
	 * @param chinese
	 * @return
	 */
	public static String convertChineseToPinyin(final String chinese) {
		if (null == chinese)
			return null;

		final StringBuffer pinyin = new StringBuffer();
		final HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
		defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		final char[] arr = chinese.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			final char ch = arr[i];
			if (ch > 128) { // 非ASCII码
				// 取得当前汉字的所有全拼
				try {
					final String[] results = PinyinHelper.toHanyuPinyinStringArray(ch, defaultFormat);
					if (results == null || results.length < 1) { // 非中文
						return "";
					} else {
						final int len = results.length;
						if (len == 1) { // 不是多音字
							// pinyin.append(results[0]);
							String py = results[0];
							if (py.contains("u:")) { // 过滤 u:
								py = py.replace("u:", "v");
								// System.out.println("filter u:" + py);
							} // if
							pinyin.append(convertInitialToUpperCase(py));
						} else if (results[0].equals(results[1])) { // 非多音字 有多个音，取第一个
							// pinyin.append(results[0]);
							pinyin.append(convertInitialToUpperCase(results[0]));
						} else { // 多音字
							// System.out.println("多音字：" + ch);
							final int length = chinese.length();
							boolean flag = false;
							String s = null;
							List<String> keyList = null;
							for (int x = 0; x < len; x++) {
								String py = results[x];
								if (py.contains("u:")) { // 过滤 u:
									py = py.replace("u:", "v");
									// System.out.println("filter u:" + py);
								}
								keyList = pinyinMap.get(py);
								if (i + 3 <= length) { // 后向匹配2个汉字 大西洋
									s = chinese.substring(i, i + 3);
									if (keyList != null && (keyList.contains(s))) {
										// System.out.println("last 2 > " + py);
										// pinyin.append(results[x]);
										pinyin.append(convertInitialToUpperCase(py));
										flag = true;
										break;
									}
								}

								if (i + 2 <= length) { // 后向匹配 1个汉字 大西
									s = chinese.substring(i, i + 2);
									if (keyList != null && (keyList.contains(s))) {
										// System.out.println("last 1 > " + py);
										// pinyin.append(results[x]);
										pinyin.append(convertInitialToUpperCase(py));
										flag = true;
										break;
									}
								}

								if ((i - 2 >= 0) && (i + 1 <= length)) { // 前向匹配2个汉字 龙固大
									s = chinese.substring(i - 2, i + 1);
									if (keyList != null && (keyList.contains(s))) {
										System.out.println("before 2 < " + py);
										// pinyin.append(results[x]);
										pinyin.append(convertInitialToUpperCase(py));
										flag = true;
										break;
									}
								}

								if ((i - 1 >= 0) && (i + 1 <= length)) { // 前向匹配1个汉字 固大
									s = chinese.substring(i - 1, i + 1);
									if (keyList != null && (keyList.contains(s))) {
										// System.out.println("before 1 < " + py);
										pinyin.append(results[x]);
										pinyin.append(convertInitialToUpperCase(py));
										flag = true;
										break;
									}
								}

								if ((i - 1 >= 0) && (i + 2 <= length)) { // 前向1个，后向1个 固大西
									s = chinese.substring(i - 1, i + 2);
									if (keyList != null && (keyList.contains(s))) {
										// System.out.println("before last 1 <> " + py);
										// pinyin.append(results[x]);
										pinyin.append(convertInitialToUpperCase(py));
										flag = true;
										break;
									}
								}
							}

							if (!flag) { // 都没有找到，匹配默认的 读音 大
								s = String.valueOf(ch);
								for (int x = 0; x < len; x++) {
									String py = results[x];
									if (py.contains("u:")) { // 过滤 u:
										py = py.replace("u:", "v");
										// System.out.println("filter u:");
									}

									keyList = pinyinMap.get(py);
									if (keyList != null && (keyList.contains(s))) {
										// System.out.println("default = " + py);
										// pinyin.append(results[x]); //如果不需要拼音首字母大写 ，直接返回即可
										pinyin.append(convertInitialToUpperCase(py));// 拼音首字母 大写
										break;
									}
								}
							}
						}
					}
				} catch (BadHanyuPinyinOutputFormatCombination e) {
					e.printStackTrace();
				}
			} else {
				pinyin.append(arr[i]);
			}
		}
		return pinyin.toString();
	}

	/**
	 * 初始化 所有的多音字词组
	 * 
	 * @param fileName
	 */
	public static void initPinyin(String fileName) {
		// 读取多音字的全部拼音表;
		fileName = path(fileName, PinyinUtil.class);
		InputStream is = null;
		try {
			is = new FileInputStream(fileName);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String s = null;
		try {
			while ((s = br.readLine()) != null) {
				if (s != null) {
					final String[] arr = s.split("#");
					final String pinyin = arr[0];
					final String chinese = arr[1];
					if (chinese != null) {
						final String[] strs = chinese.split(" ");
						final List<String> list = Arrays.asList(strs);
						pinyinMap.put(pinyin, list);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 转换成拼音数组
	 * 
	 * @param s
	 * @return
	 */
	public static String[] getPinyin(final String s) {
		if (s == null)
			return null;
		final Pattern p = Pattern.compile("([A-Z])");
		final String line = convertChineseToPinyin(s);// 转换成拼音文字
		String t = p.matcher(line).replaceAll("_$1");// 添加下划线进行分割
		t = t.toLowerCase().replaceAll("^[_]*", "");// 去除开头的_
		return t.split("_");
	}

	/**
	 * 转换成拼音数组
	 * 
	 * @param s
	 * @return
	 */
	public static String getPinyinShort(final String s) {
		return Arrays.stream(getPinyin(s)).filter(e -> e.length() > 0).map(e -> e.charAt(0) + "")
				.collect(Collectors.joining(""));
	}

	/**
	 * 转换成拼音数组
	 * 
	 * @param s
	 * @return
	 */
	public static String getPinyinLong(final String s) {
		return convertChineseToPinyin(s).toLowerCase();
	}

	/**
	 * 获得双拼音编码 转换成缩略和全拼的两种拼音格式
	 * 
	 * @param s 中文字符串
	 * @return 数组第一项是缩略拼音取每个字的拼音的首字母，第二项是全拼
	 */
	public static List<String> getPinyins(final String s) {
		final List<String> ss = new ArrayList<>();
		if (s == null)
			return ss;// 不会返回null
		final String pp[] = getPinyin(s);
		if (pp == null || pp.length < 1)
			return ss;
		final StringBuffer s1 = new StringBuffer();// 缩略
		final StringBuffer s2 = new StringBuffer();// 全拼
		Arrays.stream(pp).filter(e -> e != null && e.length() > 0).forEach(e -> {
			s1.append(e.charAt(0));// s1 是缩略拼音
			s2.append(e);// s2 是全拼
		});
		ss.add(s1.toString());// 缩略
		ss.add(s2.toString());// 全拼
		return ss;
	}

	/**
	 * 获得双拼音编码 转换成缩略和全拼的两种拼音格式
	 * 
	 * @param s 中文字符串
	 * @return 数组第一项是缩略拼音取每个字的拼音的首字母，第二项是全拼
	 */
	public static Stream<String> getPinyinS(final String s) {
		return getPinyins(s).stream();
	}

	private static final Map<String, List<String>> pinyinMap = new HashMap<String, List<String>>();
}
